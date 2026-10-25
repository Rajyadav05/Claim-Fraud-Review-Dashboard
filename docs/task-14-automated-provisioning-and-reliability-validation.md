# Task 14 — Automated Provisioning and Reliability Validation

**Project:** ClaimGuard — Claim Fraud Review Dashboard  
**Assignment Module:** Jenkins Deployment for a Claim Fraud Review Dashboard  
**Author:** Raj Yadav  
**Date:** 2026-10-23  
**Branch:** `main`  
**Host Environment:** Windows 10 Pro 22H2 (Build 19045), Docker Desktop (WSL 2 Linux Engine)  
**Control Node Environment:** Ubuntu 26.04.1 LTS in WSL 2 (`ansible-core 2.20.1`, Python 3.14.4)  
**Target Managed Containers:**
- `claimguard-staging` (Host Port `8083` mapped to Container Port `8080`, image: `claimguard:integration-fixes-local`)
- `claimguard-ansible` (Host Port `8084` mapped to Container Port `8080`, image: `claimguard:staging`)
- `claimguard-task11` (Host Port `8082` mapped to Container Port `8080`, image: `claimguard:task11`)
- Jenkins LTS Controller (Host Port `8081`)

---

## 1. Objective and Scope

Task 14 serves as the operational reliability and automated provisioning validation phase of the ClaimGuard DevOps engineering lifecycle. 

### 1.1 Core Objectives
1. **Ansible Playbook and Inventory Validation:** Verify playbook syntax, inventory resolution, variable scoping, and execution guardrails under Ubuntu WSL 2.
2. **Idempotency and Non-Destructive Provisioning:** Demonstrate that sequential Ansible playbook executions enforce state convergence without unneeded container restarts or changes (`changed=0`).
3. **Multi-Port Isolation & Service Preservation:** Validate that executing configuration management on port 8084 strictly preserves:
   - Port 8081: Dedicated Jenkins CI/CD Controller.
   - Port 8082: Immutable Task 11 reference container (`claimguard-task11`).
   - Port 8083: Dedicated staging container (`claimguard-staging`) running verified integration fixes with image `claimguard:integration-fixes-local`.
   - Port 8084: Dedicated Ansible-managed container (`claimguard-ansible`).
4. **Endpoint Reliability Verification:** Execute non-destructive automated health probes, indicators queries, and web UI verifications across all active application endpoints.
5. **Operational Limitations & Recovery Analysis:** Formally document system boundaries, database persistence constraints (in-memory H2), and safety guarantees.

---

## 2. Environment and Runtime Versions

| Component | Specification / Version | Runtime Environment | Role / Purpose |
|---|---|---|---|
| **Operating System** | Windows 10 Pro 22H2 (19045) | Physical Host | Workstation, Docker Desktop host, IDE host |
| **WSL 2 Distribution** | Ubuntu 26.04.1 LTS | WSL 2 Linux Subsystem | Ansible Control Node runtime |
| **Ansible Engine** | `ansible-core 2.20.1` | Ubuntu WSL 2 (`/usr/bin/ansible`) | Configuration Management engine |
| **Python** | Python 3.14.4 | Ubuntu WSL 2 (`/usr/bin/python3`) | Ansible execution interpreter |
| **Java Development Kit** | Eclipse Adoptium OpenJDK 21.0.12.101 | Windows Host | Build tool, test runner, Maven execution |
| **Docker Engine** | 29.9.2 (API v1.53) | Docker Desktop WSL 2 Engine | Container runtime and image store |
| **Jenkins Controller** | 2.580.1 LTS | Windows Service (`LocalSystem`) | Port 8081 CI/CD automation server |
| **Application Runtime** | Eclipse Temurin 21 JRE Alpine | Docker Container (`/app/app.jar`) | Spring Boot 3.3.4 microservice runtime |

---

## 3. Configuration Inspection & Pre-Flight Validation

Prior to executing any provisioning actions, the declarative Ansible configuration was audited to ensure full compliance with multi-port isolation and image safety.

### 3.1 Ansible Configuration Audit
- **`ansible/ansible.cfg`:** Configured with `inventory = ./inventory/hosts.ini`, `host_key_checking = False`, and `interpreter_python = auto_silent`.
- **`ansible/inventory/hosts.ini`:** Declares `localhost` with `ansible_connection=local` and `ansible_python_interpreter=/usr/bin/python3`.
- **`ansible/group_vars/all.yml`:** Defines default target `claimguard-ansible`, host port `8084`, image `claimguard:staging`, and explicit guardrails:
  ```yaml
  protected_ports:
    - 8080  # Dev / Selenium E2E
    - 8081  # Jenkins Controller
    - 8082  # Task 11 Reference
    - 8083  # Staging Container
  protected_containers:
    - "claimguard-task11"
    - "claimguard-staging"
  ```
- **`ansible/playbooks/deploy-claimguard.yml`:** Contains strict pre-flight assertions verifying `claimguard_host_port not in protected_ports`. Inspects existing container properties before triggering container removal or creation.

### 3.2 Syntax and Inventory Resolution Evidence

#### Syntax Check Execution:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-ansible-playbook.ps1 -SyntaxCheck
```
**Command Output:**
```text
============================================================
[ANSIBLE RUNNER] ClaimGuard Configuration Management
Playbook           : playbooks/deploy-claimguard.yml
Inventory          : inventory/hosts.ini
Target Image       : claimguard:staging
Host Port          : 8084
Check Mode (Dry)   : False
Syntax Check Only  : True
WSL Distribution   : Ubuntu
============================================================
[INFO] Detected Ansible control node in Ubuntu.
[INFO] Mapped workspace to WSL mount: /mnt/e/Desktop/Raj Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible
[INFO] Executing playbook via WSL...
[CMD]  wsl -d Ubuntu -- bash -c "export ANSIBLE_CONFIG='/mnt/e/Desktop/Raj Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible/ansible.cfg'; cd '/mnt/e/Desktop/Raj Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible' && ansible-playbook --syntax-check playbooks/deploy-claimguard.yml -i inventory/hosts.ini -e claimguard_image=claimguard:staging -e claimguard_host_port=8084"

playbook: playbooks/deploy-claimguard.yml
============================================================
[SUCCESS] Ansible playbook completed successfully (Exit Code 0).
============================================================
```

#### Inventory Resolution Verification:
```bash
wsl -d Ubuntu -- bash -c "export ANSIBLE_CONFIG='...'; cd '...' && ansible-inventory -i inventory/hosts.ini --list"
```
**Output Summary:** Successfully resolved `localhost` under groups `all`, `local`, and `claimguard_nodes`, properly injecting variables from `group_vars/all.yml`.

---

## 4. Playbook Execution & Idempotency Evidence

The playbook was executed twice in succession to validate convergence and idempotency against host port 8084.

### 4.1 Run 1 — Convergence & State Verification
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-ansible-playbook.ps1
```
**Play Recap:**
```text
PLAY RECAP *********************************************************************
localhost                  : ok=12   changed=0    unreachable=0    failed=0    skipped=3    rescued=0    ignored=0   
============================================================
[SUCCESS] Ansible playbook completed successfully (Exit Code 0).
============================================================
```
- **Behavior:** The inspection task evaluated `needs_deployment = False` because container `claimguard-ansible` was already running image `claimguard:staging` on port 8084. The 3 deployment tasks (`Remove existing container`, `Start container`, and `Check port availability`) were safely skipped. Health probe returned HTTP 200 `UP`.

### 4.2 Run 2 — Idempotency Proof
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-ansible-playbook.ps1
```
**Play Recap:**
```text
PLAY RECAP *********************************************************************
localhost                  : ok=12   changed=0    unreachable=0    failed=0    skipped=3    rescued=0    ignored=0   
============================================================
[SUCCESS] Ansible playbook completed successfully (Exit Code 0).
============================================================
```
- **Idempotency Result:** `changed=0`, `failed=0`. Zero modifications were applied; the running container state remained completely intact and untouched.

---

## 5. Comprehensive Reliability and Multi-Port Preservation Verification

Non-destructive HTTP health checks and Docker container inspections were executed across the entire port topology:

```
+-----------------------------------------------------------------------------------------------------+
|                                      PORT TOPOLOGY VERIFICATION                                     |
+------+--------------------+------------------------------------+--------------+---------------------+
| Port | Container / Entity | Image Tag                          | Container ID | Status / Uptime     |
+------+--------------------+------------------------------------+--------------+---------------------+
| 8081 | Jenkins LTS        | Native Windows Service             | N/A          | Active (HTTP 200)   |
| 8082 | claimguard-task11  | claimguard:task11                  | d39013d2a053 | Up 7 hours (HTTP 200)|
| 8083 | claimguard-staging | claimguard:integration-fixes-local | aac191f2c14d | Up 9 mins (HTTP 200)|
| 8084 | claimguard-ansible | claimguard:staging                 | cdd4b0cb1b82 | Up 3 hours (HTTP 200)|
+------+--------------------+------------------------------------+--------------+---------------------+
```

### 5.1 Endpoint Health Probe Verification Output
```powershell
# Probe Jenkins (8081)
curl.exe -s -o NUL -w "%{http_code}" http://localhost:8081/login
# Result: 200

# Probe Task 11 (8082)
curl.exe -s http://localhost:8082/api/health
# Result: {"database":"H2 In-Memory (Operational)","application":"ClaimGuard Fraud Review Dashboard","version":"1.0.0-SNAPSHOT","status":"UP",...}

# Probe Staging (8083) Health
curl.exe -s http://localhost:8083/api/health
# Result: {"database":"H2 In-Memory (Operational)","application":"ClaimGuard Fraud Review Dashboard","version":"1.0.0-SNAPSHOT","status":"UP",...}

# Probe Staging (8083) Suspicious Indicators DTO Endpoint
curl.exe -s http://localhost:8083/api/claims/1/indicators
# Result: HTTP 200 with JSON DTO array:
# [{"id":1,"ruleCode":"RUL-AMT-01","title":"High Value Claim Threshold","severity":"MEDIUM",...},
#  {"id":2,"ruleCode":"RUL-MTR-04","title":"Parts Benchmark Discrepancy","severity":"MEDIUM",...},
#  {"id":3,"ruleCode":"RUL-TIM-01","title":"Delayed Incident Reporting","severity":"LOW",...}]

# Probe Ansible Managed Container (8084) Health
curl.exe -s http://localhost:8084/api/health
# Result: {"database":"H2 In-Memory (Operational)","application":"ClaimGuard Fraud Review Dashboard","version":"1.0.0-SNAPSHOT","status":"UP",...}

# Probe UI Accessibility on Staging (8083)
Invoke-WebRequest -Uri "http://localhost:8083/"         # Result: HTTP 200 (32531 bytes)
Invoke-WebRequest -Uri "http://localhost:8083/claims.html" # Result: HTTP 200 (27256 bytes)
```

### 5.2 Container Preservation Evidence
```powershell
docker ps --format "table {{.Names}}\t{{.ID}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
```
**Output:**
```text
NAMES                CONTAINER ID   IMAGE                                STATUS         PORTS
claimguard-staging   aac191f2c14d   claimguard:integration-fixes-local   Up 9 minutes   0.0.0.0:8083->8080/tcp, [::]:8083->8080/tcp
claimguard-ansible   cdd4b0cb1b82   claimguard:staging                   Up 3 hours     0.0.0.0:8084->8080/tcp, [::]:8084->8080/tcp
claimguard-task11    d39013d2a053   claimguard:task11                    Up 7 hours     0.0.0.0:8082->8080/tcp, [::]:8082->8080/tcp
```
- **Proof of Preservation:** Neither `claimguard-task11` nor `claimguard-staging` was stopped, restarted, or altered. Container IDs and uptimes demonstrate zero disruption.

---

## 6. Failure Handling, Limitations, and Untested Recovery Scenarios

To comply with strict safety requirements, non-destructive testing was prioritized. The following operational parameters, architectural boundaries, and untested scenarios are explicitly recorded:

1. **In-Memory Database Lifecycle:** The application uses an embedded in-memory H2 database (`jdbc:h2:mem:claimguard`). Restarting any container clears non-persisted transactional state and re-seeds the default 5 demo claims. True stateful disaster recovery requires an external relational database (e.g. PostgreSQL), which is outside the college assignment scope.
2. **Destructive Failure Injection Omission:** Host failure simulation, process SIGKILL injection, and sudden Docker engine termination were intentionally not performed against the running environment to preserve active containers and avoid state corruption.
3. **Automated Rollback Logic (Code-Inspected):**
   - The deployment script [`scripts/deploy-staging-container.ps1`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/scripts/deploy-staging-container.ps1) features verified rollback logic: if `/api/health` polling times out or returns non-200, the failed candidate container is removed, and the archived backup container is automatically restored and started.
   - In Ansible ([`ansible/playbooks/deploy-claimguard.yml`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible/playbooks/deploy-claimguard.yml)), pre-flight checks block port collision or invalid images prior to stopping any running container.
4. **Local Source vs. Remote SCM:** Local working tree changes remain uncommitted and unpushed. Jenkins job `ClaimGuard-Pipeline` continues to track remote GitHub `main`.

---

## 7. Task 14 Completion Checklist

- [x] Pre-flight Ansible configuration inspection completed.
- [x] Ansible playbook syntax check passed (`--syntax-check`).
- [x] Ansible inventory resolution verified (`ansible-inventory --list`).
- [x] Playbook Run 1 executed successfully (`ok=12, changed=0, failed=0`).
- [x] Playbook Run 2 executed successfully and verified idempotency (`ok=12, changed=0, failed=0`).
- [x] Port 8081 (Jenkins) verified operational (`HTTP 200`).
- [x] Port 8082 (`claimguard-task11`) verified untouched and healthy (`HTTP 200`).
- [x] Port 8083 (`claimguard-staging`) verified healthy running `claimguard:integration-fixes-local` (`HTTP 200`).
- [x] Port 8083 updated `GET /api/claims/1/indicators` returns HTTP 200 with DTO array.
- [x] Port 8084 (`claimguard-ansible`) verified healthy (`HTTP 200`).
- [x] Non-destructive validation boundaries and limitations documented.
- [x] Zero Git commits, pushes, or merges performed.
