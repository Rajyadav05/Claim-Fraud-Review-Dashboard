# ClaimGuard — Claim Fraud Review Dashboard

> **College DevOps Project** | Jenkins Deployment for a Claim Fraud Review Dashboard

---

## Project Description

A web-based insurance **Claim Fraud Review Dashboard** that allows operators to submit insurance claims, surfaces deterministic heuristic suspicious indicators for trained reviewers, captures structured approve/reject decisions with mandatory remarks, and maintains an immutable audit trail of review actions.

The application is the subject of a college DevOps module assignment. The primary technical deliverable is an automated **Jenkins CI/CD pipeline** that compiles, tests, containerises, and deploys the application using Docker and Ansible — demonstrating the complete software delivery lifecycle from requirements through to automated deployment.

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
| **Version Control** | Git + GitHub | Feature branch: `feature/claim-review-mvp` | In Progress |
| **CI/CD** | Jenkins | Automated build, test, and package pipeline | Planned (Task 7+) |
| **Containerisation** | Docker | Multi-stage Dockerfile packaging | Planned (Task 10+) |
| **Configuration** | Ansible | Playbooks for automated provisioning | Planned (Task 12+) |
| **E2E Testing** | Selenium WebDriver | Automated critical journey testing | Planned (Task 9+) |

---

## Current Development Stage: Task 5 — Feature Development with Branching

- **Active Branch:** `feature/claim-review-mvp`
- **Initial Baseline Commit:** `02386fe252b38e2a0e1783cff04b77f756ab7fdc`
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
  - Comprehensive unit and integration test suite passing 100% of tests.

---

## Local Run Instructions

### Prerequisites
- **Java Development Kit (JDK):** OpenJDK 21 LTS installed (`JAVA_HOME` configured).
- **Maven:** Version 3.9+ or use the included Maven Wrapper (`.\mvnw`).

### 1. Build and Run Tests
From the project root:
```powershell
.\mvnw test
```

### 2. Package Executable JAR
```powershell
.\mvnw clean package
```

### 3. Run Application Locally
```powershell
.\mvnw spring-boot:run
```
Or execute the packaged JAR directly:
```powershell
java -jar target/claimguard-dashboard-1.0.0-SNAPSHOT.jar
```

The server starts by default on port `8080`.

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
| Task 05 | Feature Development with Branching | **In Progress** (`feature/claim-review-mvp`) |
| Task 06 | Code Review and Branch Merging | Planned |
| Task 07 | Jenkins Pipeline Setup (CI Build & Test) | Planned |
| Task 08 | Automated Testing Pipeline Integration | Planned |
| Task 09 | Selenium End-to-End Automated Testing | Planned |
| Task 10 | Docker Container Packaging | Planned |
| Task 11 | Jenkins + Docker Automated Pipeline | Planned |
| Task 12 | Ansible Configuration Management | Planned |
| Task 13 | Full CI/CD Pipeline Demonstration | Planned |
| Task 14 | Project Retrospective and Viva Preparation | Planned |

---

*ClaimGuard — Claim Fraud Review Dashboard | College DevOps Assignment | Raj Yadav | 2026*
