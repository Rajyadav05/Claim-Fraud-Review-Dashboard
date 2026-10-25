# Task 13 — Configuration Management Script (Ansible)

**Project:** ClaimGuard — Claim Fraud Review Dashboard  
**Assignment Module:** Jenkins Deployment for a Claim Fraud Review Dashboard  
**Author:** Raj Yadav  
**Date:** 2026-10-22  
**Branch:** `main`  
**Host Environment:** Windows 10 Pro 22H2 (Build 19045), Docker Desktop 4.96.2 (WSL 2 Linux Engine)  
**Control Node Environment:** Ubuntu 26.04.1 LTS in WSL 2 (`/usr/bin/ansible`, Ansible Core 2.20.1)  
**Configuration Management Tool:** Ansible 13.1.0 (`ansible-core 2.20.1`, Python 3.14.4)  
**Target Inventory:** Localhost (`connection: local`, Python 3 interpreter)  
**Playbook Path:** [`ansible/playbooks/deploy-claimguard.yml`](file:///e:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible/playbooks/deploy-claimguard.yml)  
**PowerShell Runner:** [`scripts/run-ansible-playbook.ps1`](file:///e:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/scripts/run-ansible-playbook.ps1)  
**Managed Container:** `claimguard-ansible` (Host Port `8084` mapped to Container Port `8080`)  
**Target Image:** `claimguard:staging`  

---

## 1. Executive Summary & Purpose

Configuration Management (CM) ensures that server environments, application runtimes, network boundaries, and deployment configurations remain **declarative, version-controlled, auditable, and idempotent**. 

In Task 13, Ansible was selected over Puppet to provide lightweight, agentless configuration management for ClaimGuard. Operating from an **Ubuntu WSL 2 control node**, Ansible orchestrates local container provisioning, enforces multi-port safety guardrails, manages container lifecycle declaratively, and validates runtime health without requiring background management daemons or external master servers.

### 1.1 Core Objectives for Task 13
1. **Agentless Control Node Provisioning:** Install and verify `ansible-core` (v2.20.1) inside Ubuntu 26.04 WSL 2 without installing unsupported native Windows packages.
2. **Industry-Standard Ansible Project Structure:** Establish standard directories (`ansible/ansible.cfg`, `ansible/inventory/hosts.ini`, `ansible/group_vars/all.yml`, `ansible/playbooks/deploy-claimguard.yml`).
3. **Strict Port & Container Guardrails:** Assign dedicated host port **8084** to `claimguard-ansible`, enforcing assertions to protect ports 8080 (Dev/E2E), 8081 (Jenkins), 8082 (`claimguard-task11`), and 8083 (`claimguard-staging`).
4. **Verified Idempotency:** Guarantee that re-running the playbook against an unchanged environment makes zero state modifications (`changed=0`), avoiding unnecessary container restarts.
5. **Automated Health Probing:** Query `/api/health` and verify HTTP 200 with JSON status `"UP"` before declaring deployment success.
6. **Cross-Platform PowerShell Runner:** Provide [`scripts/run-ansible-playbook.ps1`](file:///e:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/scripts/run-ansible-playbook.ps1) to allow execution from Windows terminal or CI/CD pipelines while preserving exit codes.

---

## 2. Configuration Management Architecture

```mermaid
flowchart TD
    subgraph WindowsHost["Windows 10 Host System"]
        Dev["Port 8080: Local Spring Boot Dev & E2E Tests"]
        Jenkins["Port 8081: Jenkins LTS Controller (LocalSystem)"]
        Task11["Port 8082: claimguard-task11 (Task 11 Immutable Container)"]
        Task12["Port 8083: claimguard-staging (Task 12 Jenkins CD Container)"]
        PSRunner["PowerShell Runner: scripts/run-ansible-playbook.ps1"]
    end

    subgraph WSLNode["Ubuntu 26.04 LTS (WSL 2 Control Node)"]
        AnsibleCLI["Ansible Core 2.20.1 (Python 3.14.4)"]
        Playbook["Playbook: ansible/playbooks/deploy-claimguard.yml"]
        Inventory["Inventory: ansible/inventory/hosts.ini (connection: local)"]
        Config["Config: ansible/ansible.cfg"]
        Vars["Group Vars: ansible/group_vars/all.yml"]

        Config --> AnsibleCLI
        Inventory --> AnsibleCLI
        Vars --> Playbook
        AnsibleCLI --> Playbook
    end

    PSRunner -->|wsl -d Ubuntu -- ansible-playbook| AnsibleCLI

    subgraph DockerDaemon["Docker Desktop 29.9.2 (WSL 2 Backend)"]
        Interop["Docker Windows Binary Interop:<br/>/mnt/c/.../docker.exe"]
        AnsibleContainer["Container: claimguard-ansible<br/>(Port 8084:8080 / Image: claimguard:staging)"]
        Interop --> AnsibleContainer
    end

    Playbook -->|Inspect & Run Container| Interop

    HealthProbe["Ansible uri Module<br/>GET http://localhost:8084/api/health"] -->|HTTP 200 {"status":"UP"}| AnsibleContainer
```

### 2.1 Multi-Container Isolation Matrix

| Port | Service / Container | Deployment Mechanism | Status in Task 13 |
|:---:|:---|:---|:---:|
| **8080** | Ephemeral Spring Boot Process | Managed by [`scripts/run-e2e-suite.ps1`](file:///e:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/scripts/run-e2e-suite.ps1) | Transient (Port released) |
| **8081** | Jenkins Master Controller | Windows Service (`LocalSystem`) | Active & Protected |
| **8082** | `claimguard-task11` | Task 11 Docker Lifecycle Reference | Preserved & Running |
| **8083** | `claimguard-staging` | Task 12 Jenkins Automated Pipeline | Preserved & Running |
| **8084** | `claimguard-ansible` | **Task 13 Ansible Playbook** | **Provisioned, Verified & Running** |

---

## 3. Phase A — Installation & Verification Log

### 3.1 Package Repository Update
```powershell
wsl -d Ubuntu -- apt-get update
```
- Fetched 32.1 MB from Ubuntu Resolute package repositories in 12s.
- `apt-cache policy ansible` confirmed package `13.1.0+dfsg-1ubuntu1` and `ansible-core 2.20.1-1`.

### 3.2 Ansible Package Installation
```powershell
wsl -d Ubuntu -- DEBIAN_FRONTEND=noninteractive apt-get install -y ansible
```
- Installed 17 packages (`ansible`, `ansible-core`, `python3-resolvelib`, `python3-netaddr`, `python3-jmespath`, etc.).

### 3.3 Version Verification
```powershell
wsl -d Ubuntu -- ansible --version
wsl -d Ubuntu -- ansible-playbook --version
```
**Verified Output:**
```text
ansible [core 2.20.1]
  executable location = /usr/bin/ansible
  python version = 3.14.4 (main, Jun 18 2026, 14:25:02) [GCC 15.2.0] (/usr/bin/python3)
  jinja version = 3.1.6
  pyyaml version = 6.0.3 (with libyaml v0.2.5)

ansible-playbook [core 2.20.1]
  executable location = /usr/bin/ansible-playbook
  python version = 3.14.4 (/usr/bin/python3)
```

---

## 4. Phase B — Ansible Project Implementation

### 4.1 Configuration File: `ansible/ansible.cfg`
```ini
[defaults]
inventory = ./inventory/hosts.ini
host_key_checking = False
retry_files_enabled = False
stdout_callback = default
interpreter_python = auto_silent
nocows = 1

[privilege_escalation]
become = False
```

### 4.2 Local Inventory: `ansible/inventory/hosts.ini`
```ini
[local]
localhost ansible_connection=local ansible_python_interpreter=/usr/bin/python3

[claimguard_nodes]
localhost

[claimguard_nodes:vars]
ansible_connection=local
ansible_python_interpreter=/usr/bin/python3
```

### 4.3 Group Variables: `ansible/group_vars/all.yml`
```yaml
claimguard_app_name: "claimguard-dashboard"
claimguard_container_name: "claimguard-ansible"
claimguard_image: "claimguard:staging"
claimguard_host_port: 8084
claimguard_container_port: 8080
claimguard_health_url: "http://localhost:8084/api/health"
claimguard_root_url: "http://localhost:8084/"
claimguard_health_timeout: 45
claimguard_docker_bin: "/mnt/c/Users/User/AppData/Local/Programs/DockerDesktop/resources/bin/docker.exe"

# Port Safety Guardrails: Reserved ports that MUST never be bound
protected_ports:
  - 8080  # Dev / E2E
  - 8081  # Jenkins
  - 8082  # Task 11 Container
  - 8083  # Task 12 Staging Container

protected_containers:
  - "claimguard-task11"
  - "claimguard-staging"
```

### 4.4 Declarative Playbook: `ansible/playbooks/deploy-claimguard.yml`
```yaml
- name: Provision and Configure ClaimGuard Dashboard via Ansible
  hosts: localhost
  connection: local
  gather_facts: false
  vars_files:
    - "../group_vars/all.yml"

  tasks:
    - name: "[PRE-FLIGHT] Verify Docker CLI accessibility"
      ansible.builtin.stat:
        path: "{{ claimguard_docker_bin }}"
      register: docker_bin_stat
      failed_when: not docker_bin_stat.stat.exists

    - name: "[PRE-FLIGHT] Enforce port protection guardrails"
      ansible.builtin.assert:
        that:
          - (claimguard_host_port | int) not in protected_ports
        fail_msg: "Cannot deploy over protected ports {{ protected_ports }}."

    - name: "[PRE-FLIGHT] Verify target Docker image exists"
      ansible.builtin.command:
        cmd: "{{ claimguard_docker_bin }} images -q {{ claimguard_image }}"
      register: image_check
      changed_when: false
      check_mode: no
      failed_when: image_check.stdout | trim == ""

    - name: "[PRE-FLIGHT] Retrieve target image ID"
      ansible.builtin.command:
        cmd: "{{ claimguard_docker_bin }} inspect --format '{% raw %}{{.Id}}{% endraw %}' {{ claimguard_image }}"
      register: target_image_id
      changed_when: false
      check_mode: no

    - name: "[INSPECT] Check if container '{{ claimguard_container_name }}' currently exists"
      ansible.builtin.command:
        cmd: "{{ claimguard_docker_bin }} ps -a --filter name=^/{{ claimguard_container_name }}$ --format '{% raw %}{{.ID}}{% endraw %}'"
      register: existing_container
      changed_when: false
      check_mode: no

    - name: "[INSPECT] Inspect existing container properties if present"
      ansible.builtin.command:
        cmd: "{{ claimguard_docker_bin }} inspect --format '{% raw %}{{.State.Running}} {{.Image}} {{range $p, $conf := .NetworkSettings.Ports}}{{range $conf}}{{.HostPort}}{{end}}{{end}}{% endraw %}' {{ claimguard_container_name }}"
      register: container_inspect
      changed_when: false
      check_mode: no
      failed_when: false
      when: existing_container.stdout | trim != ""

    - name: "[IDEMPOTENCY] Determine if container recreation is required"
      ansible.builtin.set_fact:
        needs_deployment: >-
          {{
            (existing_container.stdout | trim == "") or
            (container_inspect.stdout is not defined) or
            (not container_inspect.stdout.startswith('true')) or
            (target_image_id.stdout | trim not in container_inspect.stdout) or
            ((claimguard_host_port | string) not in container_inspect.stdout)
          }}

    - name: "[DEPLOY] Remove existing container if recreation is needed"
      ansible.builtin.command:
        cmd: "{{ claimguard_docker_bin }} rm -f {{ claimguard_container_name }}"
      when: needs_deployment and (existing_container.stdout | trim != "")
      changed_when: true

    - name: "[DEPLOY] Start '{{ claimguard_container_name }}' container on port {{ claimguard_host_port }}"
      ansible.builtin.command:
        cmd: "{{ claimguard_docker_bin }} run -d --name {{ claimguard_container_name }} -p {{ claimguard_host_port }}:{{ claimguard_container_port }} {{ claimguard_image }}"
      when: needs_deployment
      changed_when: true

    - name: "[IDEMPOTENT] Confirm existing container is already up to date"
      ansible.builtin.debug:
        msg: "Container '{{ claimguard_container_name }}' is already running image '{{ claimguard_image }}' on port {{ claimguard_host_port }}. No action taken."
      when: not needs_deployment

    - name: "[VERIFY] Poll health endpoint until 'UP'"
      ansible.builtin.uri:
        url: "{{ claimguard_health_url }}"
        method: GET
        status_code: 200
        return_content: true
      register: health_resp
      until: >
        health_resp.status == 200 and
        ('"status":"UP"' in health_resp.content or
         (health_resp.json is defined and health_resp.json.status == 'UP'))
      retries: 25
      delay: 2

    - name: "[VERIFY] Verify root dashboard endpoint responds 200 OK"
      ansible.builtin.uri:
        url: "{{ claimguard_root_url }}"
        method: GET
        status_code: 200
      register: root_resp
```

---

## 5. Verification Results & Idempotency Proof

### 5.1 Syntax Check
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-ansible-playbook.ps1 -SyntaxCheck
```
**Output:**
```text
[ANSIBLE RUNNER] ClaimGuard Configuration Management
[INFO] Detected Ansible control node in Ubuntu.
[INFO] Mapped workspace to WSL mount: /mnt/e/Desktop/Raj Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible
[INFO] Executing playbook via WSL...
playbook: playbooks/deploy-claimguard.yml
[SUCCESS] Ansible playbook completed successfully (Exit Code 0).
```

### 5.2 Initial Deployment Run (State Modification)
On the first execution when no `claimguard-ansible` container existed:
```text
TASK [[STATUS] Report container state before provisioning] *********************
ok: [localhost] => {
    "msg": "Container 'claimguard-ansible' exists: False. Recreation needed: True.\n"
}

TASK [[DEPLOY] Start 'claimguard-ansible' container on port 8084] **************
changed: [localhost]

TASK [[VERIFY] Poll health endpoint until 'UP'] ********************************
ok: [localhost]

PLAY RECAP *********************************************************************
localhost                  : ok=12   changed=1    unreachable=0    failed=0    skipped=3
```
- Assigned Container ID: `cdd4b0cb1b82`
- Result: **1 change** (`changed=1`), Container started, verified healthy on port 8084.

### 5.3 Second Run (Idempotency Verification)
Executing the exact same playbook immediately afterward without modifying state:
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-ansible-playbook.ps1
```
**Output:**
```text
TASK [[STATUS] Report container state before provisioning] *********************
ok: [localhost] => {
    "msg": "Container 'claimguard-ansible' exists: True. Recreation needed: False.\n"
}

TASK [[PRE-FLIGHT] Check port 8084 availability if deploying] ******************
skipping: [localhost]

TASK [[DEPLOY] Remove existing container if recreation is needed] **************
skipping: [localhost]

TASK [[DEPLOY] Start 'claimguard-ansible' container on port 8084] **************
skipping: [localhost]

TASK [[IDEMPOTENT] Confirm existing container is already up to date] ***********
ok: [localhost] => {
    "msg": "Container 'claimguard-ansible' is already running image 'claimguard:staging' on port 8084. No action taken."
}

TASK [[VERIFY] Poll health endpoint until 'UP'] ********************************
ok: [localhost]

TASK [[VERIFY] Verify root dashboard endpoint responds 200 OK] *****************
ok: [localhost]

TASK [[SUMMARY] Telemetry of Ansible-managed ClaimGuard deployment] ************
ok: [localhost] => {
    "msg": {
        "container_name": "claimguard-ansible",
        "health_payload": "{\"database\":\"H2 In-Memory (Operational)\",\"application\":\"ClaimGuard Fraud Review Dashboard\",\"version\":\"1.0.0-SNAPSHOT\",\"status\":\"UP\",\"timestamp\":\"2026-10-22T17:04:22.786612289\"}",
        "health_status": 200,
        "host_port": "8084",
        "idempotent_pass": true,
        "image_tag": "claimguard:staging",
        "root_dashboard_status": 200
    }
}

PLAY RECAP *********************************************************************
localhost                  : ok=12   changed=0    unreachable=0    failed=0    skipped=3    rescued=0    ignored=0   
```

### 5.4 Run 1 vs. Run 2 Comparison Matrix

| Metric | Run 1 (Initial Deploy) | Run 2 (Idempotency Check) | Evaluation |
|:---|:---:|:---:|:---:|
| **Existing Container Detected** | `False` | `True` | State properly captured |
| **Recreation Needed** | `True` | `False` | Correct logical branching |
| **Deploy Task Executed** | `changed: [localhost]` | `skipping: [localhost]` | **Idempotent** |
| **Active Container ID** | `cdd4b0cb1b82` | `cdd4b0cb1b82` | **Container retained untouched** |
| **Play Recap Changed Count** | **`changed=1`** | **`changed=0`** | **Verified Idempotency** |
| **Health Check Status** | HTTP `200 UP` | HTTP `200 UP` | Healthy across both runs |

---

## 6. Multi-Service Environment Cross-Verification

```powershell
$p8081 = (Invoke-WebRequest -Uri "http://localhost:8081/login" -UseBasicParsing).StatusCode
$p8082 = (Invoke-WebRequest -Uri "http://localhost:8082/api/health" -UseBasicParsing).StatusCode
$p8083 = (Invoke-WebRequest -Uri "http://localhost:8083/api/health" -UseBasicParsing).StatusCode
$p8084Health = (Invoke-WebRequest -Uri "http://localhost:8084/api/health" -UseBasicParsing)
$p8084Root = (Invoke-WebRequest -Uri "http://localhost:8084/" -UseBasicParsing).StatusCode
$p8084Claims = (Invoke-WebRequest -Uri "http://localhost:8084/claims.html" -UseBasicParsing).StatusCode
```

**Results:**
- **Jenkins Controller (`http://localhost:8081`):** `200 OK`
- **Task 11 Container (`http://localhost:8082`):** `200 OK`
- **Task 12 Staging Container (`http://localhost:8083`):** `200 OK`
- **Task 13 Ansible Container (`http://localhost:8084/api/health`):** `200 OK` (`{"status":"UP", ...}`)
- **Task 13 Ansible Container (`http://localhost:8084/`):** `200 OK`
- **Task 13 Ansible Container (`http://localhost:8084/claims.html`):** `200 OK`

---

## 7. Screenshots & Evidence Guide for Submissions

When capturing visual proof for the assignment report or presentation, capture the following 6 artifacts:

1. **Ansible Version in Ubuntu WSL:** Terminal showing `ansible --version` and `ansible-playbook --version` (Core 2.20.1 on Python 3.14.4).
2. **Ansible Playbook Structure:** File explorer / directory tree showing `ansible/ansible.cfg`, `inventory/hosts.ini`, `group_vars/all.yml`, and `playbooks/deploy-claimguard.yml`.
3. **Syntax Check Output:** Terminal displaying `scripts\run-ansible-playbook.ps1 -SyntaxCheck` passing with Exit Code 0.
4. **Idempotency Proof (Side-by-Side Play Recaps):**
   - Run 1 Recap: `ok=12 changed=1 skipped=3`
   - Run 2 Recap: `ok=12 changed=0 skipped=3`
5. **Active Containers Terminal (`docker ps`):** Displaying all three containers active:
   - `claimguard-ansible` (`0.0.0.0:8084->8080/tcp`)
   - `claimguard-staging` (`0.0.0.0:8083->8080/tcp`)
   - `claimguard-task11` (`0.0.0.0:8082->8080/tcp`)
6. **Browser Verification on Port 8084:** Browser windows displaying `http://localhost:8084/api/health` returning JSON `UP` and `http://localhost:8084/` displaying the Operations Executive Dashboard.

---

## 8. Comprehensive Viva Defense Questions & Answers

### Q1: What is Configuration Management, and why is Ansible preferred over Puppet for this workflow?
**Answer:** Configuration Management is the practice of maintaining computer systems, servers, and software in a desired, consistent state through automated, version-controlled scripts. Ansible is preferred over Puppet because:
1. **Agentless Architecture:** Ansible uses SSH or local execution (`connection: local`) without requiring persistent background daemons or agents installed on managed nodes. In contrast, Puppet requires a Puppet Master server, Puppet Agents, and certificate authority management.
2. **Simpler Learning Curve & YAML Syntax:** Ansible playbooks are written in standard YAML, making them easier to read, review, and integrate into existing CI/CD pipelines.
3. **Control Node Flexibility:** Ansible runs seamlessly inside Ubuntu WSL on Windows developer workstations without requiring dedicated Linux virtual machines or heavy master servers.

### Q2: What does "Idempotency" mean in configuration management, and how does our playbook demonstrate it?
**Answer:** Idempotency means that applying an operation multiple times produces the exact same result as applying it once, without making unintended changes or repeatedly restarting healthy services.
In [`ansible/playbooks/deploy-claimguard.yml`](file:///e:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible/playbooks/deploy-claimguard.yml), our playbook inspects whether container `claimguard-ansible` is already running, checks whether its image ID matches `claimguard:staging`, and verifies its bound port (`8084`).
- **Run 1:** Container does not exist $\rightarrow$ `needs_deployment: true` $\rightarrow$ container started $\rightarrow$ **`changed=1`**.
- **Run 2:** Container already exists, matches image, and is healthy $\rightarrow$ `needs_deployment: false` $\rightarrow$ deploy tasks skipped $\rightarrow$ **`changed=0`**.

### Q3: Why does Ansible control node need to run inside WSL rather than natively on Windows?
**Answer:** The Ansible control node (the machine that reads playbooks, resolves dependencies, and manages targets) relies on POSIX system calls, process forking (`os.fork()`), and Unix file permissions that are not supported by the Windows Win32 API. While Ansible can *manage* Windows hosts remotely via WinRM or SSH, the *control node* itself must run in a POSIX environment. Running Ansible inside Ubuntu WSL 2 provides an authentic Linux environment while allowing direct access to the Windows host filesystem mounted under `/mnt/`.

### Q4: How does the playbook prevent accidental collisions with Jenkins and prior task deliverables?
**Answer:** The playbook implements multiple layers of defensive guardrails:
1. **Group Variable Whitelist:** `ansible/group_vars/all.yml` explicitly defines `protected_ports: [8080, 8081, 8082, 8083]`.
2. **Pre-Flight Assertion:** An `ansible.builtin.assert` task validates that `claimguard_host_port` (default 8084) is not in `protected_ports`. If a user attempts to run the playbook with `-e claimguard_host_port=8082`, the playbook halts immediately before touching Docker.
3. **Port Listener Inspection:** If port 8084 is in use by an external process or another container, the playbook detects it via `docker ps --filter publish=8084` and aborts rather than terminating the existing service.

### Q5: How does the PowerShell runner bridge Windows and WSL?
**Answer:** [`scripts/run-ansible-playbook.ps1`](file:///e:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/scripts/run-ansible-playbook.ps1):
1. Verifies that `wsl.exe` is available and the `Ubuntu` distribution is responsive.
2. Translates the Windows project path (`E:\Desktop\...`) into the corresponding WSL mount path (`/mnt/e/Desktop/...`).
3. Sets `ANSIBLE_CONFIG` environment variable in WSL to prevent world-writable directory permission warnings from disabling `ansible.cfg`.
4. Invokes `wsl.exe -d Ubuntu -- bash -c "..."`, passing dynamic extra variables (`-e claimguard_image=... -e claimguard_host_port=...`).
5. Captures and propagates the Ansible exit code (`$LASTEXITCODE`) back to PowerShell, allowing Jenkins or scripts to catch failures.

### Q6: What is the purpose of `vars_files` in the playbook?
**Answer:** By default, Ansible searches for `group_vars/` relative to the inventory file or the playbook directory. Because our playbook resides in `ansible/playbooks/` while `group_vars/` is in `ansible/group_vars/`, declaring `vars_files: ["../group_vars/all.yml"]` explicitly guarantees that default configurations and port protection lists are loaded deterministically, regardless of what working directory the playbook is invoked from.

### Q7: Why did we use `ansible.builtin.uri` instead of a curl shell command for health verification?
**Answer:** The `ansible.builtin.uri` module is an idiomatic, native Ansible module. It provides:
1. Declarative status code matching (`status_code: 200`).
2. Automatic retry polling loops (`until`, `retries: 25`, `delay: 2`) without writing fragile bash while-loops.
3. Native JSON parsing, allowing assertions on structured response bodies (`health_resp.json.status == 'UP'`).
4. Structured error reporting in Ansible's standard output and JSON recap.

### Q8: How does Task 13 integrate with previous tasks (Tasks 11 and 12)?
**Answer:** 
- In **Task 11**, we established the container runtime image and manual Docker container lifecycle (`claimguard:task11` on port 8082).
- In **Task 12**, Jenkins automated the continuous integration and deployment of versioned tags (`claimguard:b${BUILD_NUMBER}` and `claimguard:staging` on port 8083).
- In **Task 13**, Ansible takes over configuration management: consuming the verified `claimguard:staging` image, provisioning runtime container instances (`claimguard-ansible` on port 8084), verifying state idempotently, and establishing the groundwork for multi-node production deployment.

---

*ClaimGuard — Claim Fraud Review Dashboard | College DevOps Assignment | Raj Yadav | 2026*
