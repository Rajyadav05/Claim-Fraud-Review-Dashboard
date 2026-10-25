# ClaimGuard â€” Claim Fraud Review Dashboard

> **College DevOps Project** | Jenkins Deployment for a Claim Fraud Review Dashboard

---

## Project Description

A web-based insurance **Claim Fraud Review Dashboard** that allows operators to submit insurance claims, surfaces deterministic heuristic suspicious indicators for trained reviewers, captures structured approve/reject decisions with mandatory remarks, and maintains an immutable audit trail of review actions.

The application is the subject of a college DevOps module assignment. The primary technical deliverable is an automated **Jenkins CI/CD pipeline** that compiles, tests, containerises, and deploys the application using Docker and Ansible â€” demonstrating the complete software delivery lifecycle from requirements through to automated deployment.

> **Important Notices:**<br>
> 1. All claim data used in this project is strictly **synthetic**. No real policyholder data is used at any stage.<br>
> 2. Suspicious-indicator flags are decision-support heuristics only and do not constitute proof of fraud or conclusive determination.<br>
> 3. Physical file storage is simulated at the metadata level during MVP development.<br>
> 4. **Educational Prototype:** This application is an educational prototype developed for a college DevOps curriculum and is not intended or certified for production claims operations.<br>
> 5. **Persistent File-Backed H2 Persistence:** Uses embedded file-backed H2 storage (`jdbc:h2:file:${CLAIMGUARD_DB_PATH:./data/claimguard}`). Claims, indicators, and immutable audit history persist across application restarts. In Docker, this maps to persistent volume `claimguard-data:/app/data`.<br>
> 6. **Authentication & Authorization:** Implements role-based access control with Spring Security, BCrypt password hashing, session-based authentication with CSRF protection, and non-hardcoded administrator bootstrap.

---

## Technology Stack

| Layer | Technology | Version / Configuration | Implementation Status |
|-------|-----------|-------------------------|-----------------------|
| **Language** | Java | OpenJDK 21 LTS | Implemented |
| **Framework** | Spring Boot | 3.3.4 (REST API + Static Serving + Spring Security) | Implemented |
| **Build Tool** | Apache Maven | 3.10.0 + Maven Wrapper (`mvnw`) | Implemented |
| **Persistence** | H2 Database | Persistent File-Backed (`jdbc:h2:file:...`) | Implemented (Phase 1) |
| **Security** | Spring Security | Form Login, CSRF, BCrypt, Session Management, RBAC (`ADMIN`, `REVIEWER`) | Implemented (Phase 1) |
| **Frontend UI** | HTML5 / Vanilla JS / Tailwind CDN | Google Stitch Design Reference Adaptation + Integrated Auth Console | Implemented |
| **UI Reference** | Google Stitch | Reference Screens under `docs/stitch-export/` | Preserved Ground Truth |
| **Testing** | JUnit 5 + Mockito + MockMvc | 57 Automated Tests (Persistence, Security, Rules, Services, Reconciliation) | Implemented (100% PASS) |
| **Version Control** | Git + GitHub | Branch: `feature/phase-d-reconciliation` | Implemented |
| **CI/CD** | Jenkins | Continuous Testing & Docker CD Pipeline as Code ([`Jenkinsfile`](Jenkinsfile)) | Implemented |
| **Containerisation** | Docker | Verified candidate release (`claimguard:phase1-reconciled`, SHA-256: `3aaad642...`) with named volume | Implemented (Phase 1) |
| **Configuration** | Ansible | Playbooks for automated provisioning & container lifecycle (`ansible/`) | Implemented |
| **E2E Testing** | Selenium WebDriver | Automated Browser Tests with Auth Helper | Implemented |
| **College Demo** | Standalone Package | Self-contained demo launcher on port 8095 ([`demo-package/`](demo-package/)) | Implemented (Phase D8) |

---

## Verified Release Status: Phase D — Reconciliation & College Demo Packaging

- **Active Development Branch:** `feature/phase-d-reconciliation`
- **Verified Candidate Image:** `claimguard:phase1-reconciled`
  - **Digest:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`
- **Automated Regression Suite:** **57 of 57 Automated Tests Passed** (`mvn test -Dtest="!*Selenium*"`, 0 failures, 0 errors, 0 skipped).
- **Data Recovery & Durability Proven (Phase D6):** Confirmed full data recovery, natural key deduplication (7 total claims: 5 baseline + 2 candidate-era), audit trail integrity (12 cumulative events), idempotency, file-backed restart durability, and sequence synchronization (ID >= 8 without collision).
- **Standalone Demonstration Package (Phase D8):** Located under [`demo-package/`](demo-package/) with automated startup ([`scripts/start-demo.ps1`](demo-package/scripts/start-demo.ps1)), health check ([`scripts/check-demo.ps1`](demo-package/scripts/check-demo.ps1)), and teardown ([`scripts/stop-demo.ps1`](demo-package/scripts/stop-demo.ps1)) operating on isolated host port `8095`.
- **Mandatory Safety Boundaries:** Live staging container `claimguard-staging` (host port 8083) and production volume `claimguard-data` remain 100% untouched. Runbook Section 5 remains on hold.

---

## Current Development Stage: Task 14 — Automated Provisioning and Reliability Validation

- **Active Branch:** `main`
- **Ansible Control Node Architecture:**
  - Control node installed inside **Ubuntu 26.04.1 LTS (WSL 2)** (`ansible-core 2.20.1`, Python 3.14.4).
  - Declarative configuration management structure under [`ansible/`](ansible/):
    - [`ansible/ansible.cfg`](ansible/ansible.cfg): local configuration with world-writable permission handling.
    - [`ansible/inventory/hosts.ini`](ansible/inventory/hosts.ini): local host inventory (`connection: local`).
    - [`ansible/group_vars/all.yml`](ansible/group_vars/all.yml): centralized variables, port mapping, and port protection guardrails.
    - [`ansible/playbooks/deploy-claimguard.yml`](ansible/playbooks/deploy-claimguard.yml): idempotent provisioning playbook with pre-flight checks, state inspection, container lifecycle management, and `/api/health` polling.
  - Automated PowerShell wrapper: [`scripts/run-ansible-playbook.ps1`](scripts/run-ansible-playbook.ps1) translating Windows paths, bridging to WSL, and preserving exit codes.
- **Multi-Port Isolation & Reliability Strategy:**
  - `8080`: Ephemeral Spring Boot dev & Selenium E2E runner (8/8 browser tests passed).
  - `8081`: Dedicated Jenkins CI/CD controller (`LocalSystem`, verified operational).
  - `8082`: Immutable Task 11 reference container (`claimguard-task11`, verified UP).
  - `8083`: Automated staging container (`claimguard-staging`) running verified integration fixes (`claimguard:integration-fixes-local`).
  - `8084`: Automated Task 13/14 Ansible container (`claimguard-ansible`, verified UP).
- **Verified Idempotency & Reliability:**
  - Run 1 (State Inspection & Verification): `changed=0`, `ok=12`, `skipped=3`, container `claimguard-ansible` verified healthy on port 8084.
  - Run 2 (Idempotency Check): `changed=0`, `ok=12`, `skipped=3`, zero modifications applied.
  - Endpoints verified: `/api/health`, `/api/claims/1/indicators` (DTO mapped without LazyInitializationException), and static frontend pages across all active ports.
- **Task 14 Documentation:** [`docs/task-14-automated-provisioning-and-reliability-validation.md`](docs/task-14-automated-provisioning-and-reliability-validation.md) covering Ansible syntax/inventory validation, idempotency proof, multi-endpoint reliability, and failure limitation analysis.
- **Implemented Work:**
  - Standard Spring Boot 3.3.4 project layout using Java 21 and Maven.
  - Relational entities: `Claim`, `ClaimDocument`, `SuspiciousIndicator`, `ReviewHistory`.
  - Spring Data JPA repositories and embedded H2 database configuration.
  - Deterministic heuristic rule evaluation engine (`IndicatorRuleEngine`) evaluating 5 business rules.
  - REST API controllers for Claims, Reviews, and Health monitoring.
  - Comprehensive unit and integration test suite (11 unit/MockMvc tests passing).
  - Dedicated Selenium E2E test suite (6 browser tests passing against live server).
  - Production-conscious Docker packaging and verified container lifecycle operations (Task 11).
  - Fully automated Jenkins-to-Docker continuous deployment pipeline (Task 12).
  - Agentless Ansible configuration management with verified idempotency on port 8084 (Task 13).

---

## Local Run Instructions

### Prerequisites
- **Java Development Kit (JDK):** OpenJDK 21 LTS installed (`JAVA_HOME` configured).
- **Maven:** Version 3.9+ or use the included Maven Wrapper (`.\mvnw.cmd`).
- **Browser (for E2E tests):** Google Chrome installed at standard path.
- **Docker (for container run):** Docker Desktop with WSL 2 Linux engine.

### 1. Build and Run Tests
From the project root:
```powershell
.\mvnw.cmd test
```

### 2. Package Executable JAR
```powershell
.\mvnw.cmd clean package
```

### 3. Run Application Locally
```powershell
.\mvnw.cmd spring-boot:run
```
Or execute the packaged JAR directly:
```powershell
java -jar target/claimguard-dashboard-1.0.0-SNAPSHOT.jar
```

The server starts by default on port `8080`.

### 4. Run Selenium E2E Browser Tests Manually
With the application running locally on port 8080:
```powershell
.\mvnw.cmd test -Dtest=ClaimGuardSeleniumE2ETest
```
To run in headed (visible browser) mode:
```powershell
.\mvnw.cmd test -Dtest=ClaimGuardSeleniumE2ETest -Dselenium.headless=false
```

### 5. Run Automated E2E Continuous Testing Runner
To execute the automated lifecycle runner (starts application, verifies health, runs headless Selenium tests, and stops application):
```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-e2e-suite.ps1
```

### 6. Build and Run via Docker Container (Task 11)
To package and run the application in a production-conscious Docker container:

**Build the Docker Image:**
```powershell
# Ensure the JAR is packaged first
.\mvnw.cmd clean package

# Build the runtime image
docker build -t claimguard:task11 .
```

**Run the Container on Host Port 8082:**
```powershell
docker run -d --name claimguard-task11 -p 8082:8080 claimguard:task11
```

**Verify Container Health & Logs:**
```powershell
docker ps --filter "name=claimguard-task11"
docker logs claimguard-task11
Invoke-WebRequest -Uri "http://localhost:8082/api/health" -UseBasicParsing
```

**Container Lifecycle Commands:**
```powershell
# Stop container (sends SIGTERM to PID 1, exits gracefully with code 143)
docker stop claimguard-task11

# Restart container
docker start claimguard-task11

# Remove container
docker rm -f claimguard-task11
```

### 7. Run Ansible Configuration Management Playbook (Task 13)
To provision, configure, and verify the ClaimGuard deployment declaratively using Ansible (running inside Ubuntu WSL 2):

**From Windows PowerShell (via Automated Runner):**
```powershell
# Run idempotent deployment playbook (provisions container on port 8084)
powershell -ExecutionPolicy Bypass -File scripts\run-ansible-playbook.ps1

# Dry-run syntax check mode
powershell -ExecutionPolicy Bypass -File scripts\run-ansible-playbook.ps1 -SyntaxCheck
```

**Directly inside Ubuntu WSL:**
```bash
cd "/mnt/e/Desktop/Raj Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible"
export ANSIBLE_CONFIG="./ansible.cfg"
ansible-playbook playbooks/deploy-claimguard.yml -i inventory/hosts.ini
```

---

## Application URL and Endpoints

| URL (Local Host / Task 11 / Staging CD / Ansible) | Component / Purpose |
|--------------------------------------------------|-------------------|
| `http://localhost:8080/` *(Dev)* / `:8082/` *(Task 11)* / `:8083/` *(Staging)* / `:8084/` *(Ansible)* | Operations Executive Dashboard (`index.html`) |
| `http://localhost:8080/claims.html` / `:8082/` / `:8083/` / `:8084/claims.html` | All Claims Directory & Filter View |
| `http://localhost:8080/submit.html` / `:8082/` / `:8083/` / `:8084/submit.html` | Submit New Claim Intake Portal |
| `http://localhost:8080/review.html?id=1` / `:8082/` / `:8083/` / `:8084/review.html` | Claim Details & Forensic Adjudication Console |
| `http://localhost:8080/api/health` / `:8082/` / `:8083/` / `:8084/api/health` | Application Health Probe (`{"status":"UP",...}`) |
| `http://localhost:8080/actuator/health` / `:8082/` / `:8083/` / `:8084/actuator/health` | Spring Boot Actuator Health Probe |
| `http://localhost:8080/api/claims` / `:8082/` / `:8083/` / `:8084/api/claims` | Claims List / Search REST Endpoint |
| `http://localhost:8080/api/claims/{id}` / `:8082/` / `:8083/` / `:8084/api/claims/{id}` | Claim Detail REST Endpoint |
| `http://localhost:8080/api/claims/{id}/indicators` / `:8082/` / `:8083/` / `:8084/api/claims/{id}/indicators` | Suspicious Indicators DTO REST Endpoint |
| `http://localhost:8080/h2-console` / `:8082/` / `:8083/` / `:8084/h2-console` | H2 Database Web Console (`jdbc:h2:mem:claimguard`, User: `sa`) |

---

## Assignment Task Roadmap

| Task | Title | Status |
|------|-------|--------|
| Task 01 | Problem Definition and Scope | **Completed** (`docs/task-01-problem-definition.md`) |
| Task 02 | Agile Planning and DevOps Workflow | **Completed** (`docs/task-02-agile-planning.md`) |
| Task 03 | Requirements, Architecture and Technology Setup | **Completed** (`docs/task-03-requirements-architecture.md`) |
| Task 04 | Git and GitHub Repository Initialization | **Completed** (`main` pushed to GitHub) |
| Task 05 | Feature Development with Branching | **Completed** (`feature/claim-review-mvp`) |
| Task 06 | Code Review and Branch Merging | **Completed** (`docs/task-06-mvp-completion-and-git-collaboration.md`, PR #1 merged) |
| Task 07 | Jenkins Pipeline Setup (CI Build & Test) | **Completed** (Freestyle job `ClaimGuard-CI` verified) |
| Task 08 | Pipeline as Code and Server Deployment | **Completed** ([`Jenkinsfile`](Jenkinsfile), [`docs/task-08-pipeline-as-code-and-server-deployment.md`](docs/task-08-pipeline-as-code-and-server-deployment.md)) |
| Task 09 | Selenium End-to-End Automated Testing | **Completed** ([`docs/task-09-selenium-test-design-and-local-execution.md`](docs/task-09-selenium-test-design-and-local-execution.md)) |
| Task 10 | Continuous Testing in Jenkins | **Completed** ([`docs/task-10-continuous-testing-in-jenkins.md`](docs/task-10-continuous-testing-in-jenkins.md)) |
| Task 11 | Docker Image and Container Lifecycle | **Completed** ([`docs/task-11-docker-image-and-container-lifecycle.md`](docs/task-11-docker-image-and-container-lifecycle.md)) |
| Task 12 | Jenkins + Docker Automated Pipeline | **Completed** ([`docs/task-12-jenkins-docker-continuous-deployment.md`](docs/task-12-jenkins-docker-continuous-deployment.md)) |
| Task 13 | Ansible Configuration Management | **Completed** ([`docs/task-13-ansible-configuration-management.md`](docs/task-13-ansible-configuration-management.md)) |
| Task 14 | Automated Provisioning and Reliability Validation | **Completed** ([`docs/task-14-automated-provisioning-and-reliability-validation.md`](docs/task-14-automated-provisioning-and-reliability-validation.md)) |

---

*ClaimGuard — Claim Fraud Review Dashboard | College DevOps Assignment | Raj Yadav | 2026*
