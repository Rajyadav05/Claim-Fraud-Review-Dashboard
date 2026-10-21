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
> 5. **H2 In-Memory Persistence:** Uses embedded H2 in-memory storage (`jdbc:h2:mem:claimguard`). All database records are transient, existing in RAM only for the lifetime of the process, and completely reset upon process stop or restart; baseline synthetic claims are automatically re-seeded on application startup.<br>
> 6. **Testing Scope:** Current automated testing covers unit, business domain, and Spring MVC integration layers (11 automated tests via JUnit 5, Mockito, and MockMvc). End-to-end interactive browser testing via Selenium WebDriver is explicitly deferred to Task 9 in accordance with the project roadmap.

---

## Technology Stack

| Layer | Technology | Version / Configuration | Implementation Status |
|-------|-----------|-------------------------|-----------------------|
| **Language** | Java | OpenJDK 21 LTS | Implemented |
| **Framework** | Spring Boot | 3.3.4 (REST API + Static Serving) | Implemented |
| **Build Tool** | Apache Maven | 3.10.0 + Maven Wrapper (`mvnw`) | Implemented |
| **Persistence** | H2 Database | In-Memory (`jdbc:h2:mem:claimguard`) | Implemented |
| **Frontend UI** | HTML5 / Vanilla JS / Tailwind CDN | Google Stitch Design Reference Adaptation | Implemented |
| **UI Reference** | Google Stitch | 4 Reference Screens under `docs/stitch-export/` | Preserved Ground Truth |
| **Testing** | JUnit 5 + Mockito + MockMvc | 11 Automated Tests | Implemented |
| **Version Control** | Git + GitHub | Branch: `main` (feature merged via PR #1) | Implemented |
| **CI/CD** | Jenkins | Continuous Testing Pipeline as Code ([`Jenkinsfile`](Jenkinsfile)) | Implemented ([Task 8](docs/task-08-pipeline-as-code-and-server-deployment.md), [Task 10](docs/task-10-continuous-testing-in-jenkins.md)) |
| **Containerisation** | Docker | Multi-stage Dockerfile packaging | Planned (Task 11+) |
| **Configuration** | Ansible | Playbooks for automated provisioning | Planned (Task 13+) |
| **E2E Testing** | Selenium WebDriver | 6 Automated Browser Tests + Continuous Runner | Implemented ([Task 9](docs/task-09-selenium-test-design-and-local-execution.md), [Task 10](docs/task-10-continuous-testing-in-jenkins.md)) |

---

## Current Development Stage: Task 10 — Continuous Testing in Jenkins

- **Active Branch:** `main`
- **Continuous Testing Pipeline:** Root [`Jenkinsfile`](Jenkinsfile) implementing Declarative Pipeline stages:
  1. `Initialize` (display metadata and workspace logging)
  2. `Verify Environment` (JDK 21, compiler, and Chrome installation detection)
  3. `Run Unit & Integration Tests` (11 unit/MockMvc tests via `mvnw.cmd clean test -Dtest="!*Selenium*"`)
  4. `Package Application` (assembly of fat JAR with `-DskipTests` to prevent duplicate test runs, artifact verification, and archiving)
  5. `Run Selenium E2E Tests` (automated background app launch, `/api/health` polling, headless Chrome execution, and guaranteed shutdown)
  6. `Server Deployment Standby` (gated deployment standby for future container/Ansible stages)
- **Automated E2E Runner:** [`scripts/run-e2e-suite.ps1`](scripts/run-e2e-suite.ps1) managing the end-to-end lifecycle: stale port cleanup, Spring Boot background execution, health probe verification, headless Selenium test invocation, and guaranteed port release in `finally`.
- **Task 10 Documentation:** [`docs/task-10-continuous-testing-in-jenkins.md`](docs/task-10-continuous-testing-in-jenkins.md) covering pipeline architecture, Windows Session 0 headless strategy, local verification evidence (17/17 tests passing), troubleshooting, and viva defense questions.
- **Implemented Work:**
  - Standard Spring Boot 3.3.4 project layout using Java 21 and Maven.
  - Relational entities: `Claim`, `ClaimDocument`, `SuspiciousIndicator`, `ReviewHistory`.
  - Spring Data JPA repositories and embedded H2 database configuration.
  - Deterministic heuristic rule evaluation engine (`IndicatorRuleEngine`) evaluating 5 business rules:
    - `RUL-AMT-01`: High Value Claim Threshold (≥ ₹1,50,000)
    - `RUL-TIM-01`: Delayed Incident Reporting (> 48 hours)
    - `RUL-MTR-04`: Parts Benchmark Discrepancy (> ₹1,00,000)
    - `RUL-DUP-01`: Duplicate Claim Submission Detection
    - `RUL-DOC-02`: Missing Mandatory Supporting Documents
  - REST API controllers for Claims, Reviews, and Health monitoring.
  - Synthetic data seeder initializing 5 realistic test claims covering diverse lifecycle states (`SUBMITTED`, `UNDER_REVIEW`, `MORE_INFO_REQUIRED`, `APPROVED`, `REJECTED`).
  - Adapted frontend static web console (`index.html`, `claims.html`, `submit.html`, `review.html`) with localized SVG branding and responsive async REST integration.
  - Comprehensive unit and integration test suite (11 unit/MockMvc tests passing).
  - Dedicated Selenium E2E test suite (6 browser tests passing against live server).

---

## Local Run Instructions

### Prerequisites
- **Java Development Kit (JDK):** OpenJDK 21 LTS installed (`JAVA_HOME` configured).
- **Maven:** Version 3.9+ or use the included Maven Wrapper (`.\mvnw.cmd`).
- **Browser (for E2E tests):** Google Chrome installed at standard path.

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

---

## Application URL and Endpoints

| URL | Component / Purpose |
|-----|-------------------|
| `http://localhost:8080/` | Operations Executive Dashboard (`index.html`) |
| `http://localhost:8080/claims.html` | All Claims Directory & Filter View |
| `http://localhost:8080/submit.html` | Submit New Claim Intake Portal |
| `http://localhost:8080/review.html?id=1` | Claim Details & Forensic Adjudication Console |
| `http://localhost:8080/api/health` | Application Health Probe (`{"status":"UP",...}`) |
| `http://localhost:8080/actuator/health` | Spring Boot Actuator Health Probe |
| `http://localhost:8080/api/claims` | Claims List / Search REST Endpoint |
| `http://localhost:8080/api/claims/{id}` | Claim Detail REST Endpoint |
| `http://localhost:8080/h2-console` | H2 In-Memory Database Web Console (JDBC: `jdbc:h2:mem:claimguard`, User: `sa`) |

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
| Task 11 | Docker Container Packaging | Planned |
| Task 12 | Jenkins + Docker Automated Pipeline | Planned |
| Task 13 | Ansible Configuration Management | Planned |
| Task 14 | Project Retrospective and Viva Preparation | Planned |

---

*ClaimGuard — Claim Fraud Review Dashboard | College DevOps Assignment | Raj Yadav | 2026*
