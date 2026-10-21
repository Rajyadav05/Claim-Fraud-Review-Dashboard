# Task 01 — Problem Definition and Scope

**Project Title:** Jenkins Deployment for a Claim Fraud Review Dashboard<br>
**Document Version:** 1.0<br>
**Date:** 2026-10-21<br>
**Author:** Raj Yadav<br>
**Status:** Draft — Under Review

---

## Table of Contents

1. [Project Title and Introduction](#1-project-title-and-introduction)
2. [Background and Detailed Problem Statement](#2-background-and-detailed-problem-statement)
3. [Existing Problems in Manual Insurance Claim Review](#3-existing-problems-in-manual-insurance-claim-review)
4. [Target Users and Their Responsibilities](#4-target-users-and-their-responsibilities)
5. [Stakeholder List](#5-stakeholder-list)
6. [Project Objectives](#6-project-objectives)
7. [Functional Requirements](#7-functional-requirements)
8. [Non-Functional Requirements](#8-non-functional-requirements)
9. [Project Constraints and Assumptions](#9-project-constraints-and-assumptions)
10. [Project Risks and Mitigation Measures](#10-project-risks-and-mitigation-measures)
11. [Measurable Project Success Criteria](#11-measurable-project-success-criteria)
12. [Features Included in the MVP](#12-features-included-in-the-mvp)
13. [Features Excluded from the MVP](#13-features-excluded-from-the-mvp)
14. [Scope Aligned with All 14 Official Assignment Tasks](#14-scope-aligned-with-all-14-official-assignment-tasks)
15. [Conclusion](#15-conclusion)

---

## 1. Project Title and Introduction

### Title
**Jenkins Deployment for a Claim Fraud Review Dashboard**

### Introduction

Insurance fraud is a widespread challenge that results in significant financial losses for insurers and, ultimately, higher premium costs for honest policyholders. Manual claim review processes are slow, inconsistent, and difficult to audit, making it harder for organisations to detect suspicious patterns before a payout is made.

This project proposes the design and development of a **Claim Fraud Review Dashboard** — a web-based application that centralises insurance claim submission, rule-based suspicious-indicator detection, and reviewer decision-making in a single auditable interface. The application will be built using a modern Java-based backend with a rich frontend designed via Google Stitch, and will be deployed through a fully automated **Jenkins CI/CD pipeline** supported by **Docker** containers and **Ansible** configuration management.

The project is a college-level DevOps assignment. Its primary purpose is to demonstrate the end-to-end software development lifecycle — from requirement gathering and UI design through to automated testing, containerisation, and continuous delivery — using industry-standard tooling.

> **Scope Notice:** All claim data used during development and testing will be **synthetic (artificially generated)**. No real policyholder data will be used at any stage. Suspicious-indicator flags produced by the application's rules engine are decision-support signals only; they do **not** constitute proof of fraud.

---

## 2. Background and Detailed Problem Statement

### 2.1 Industry Context

The insurance sector processes millions of claims annually across health, motor, property, and life segments. A small but financially significant percentage of these claims involve misrepresentation, exaggerated losses, or outright fabrication. Industry bodies estimate that insurance fraud accounts for a meaningful proportion of total claim expenditure, creating pressure on insurers to improve detection without sacrificing legitimate claim turnaround times.

Traditional approaches to fraud detection rely on:
- Experienced human investigators reviewing claims based on intuition and experience.
- Retrospective data analysis performed after payment.
- External fraud databases that are expensive and not always current.

These approaches are reactive and do not integrate into the claim review workflow in a structured, repeatable way.

### 2.2 Problem Statement

There is no lightweight, open-source, developer-deployable tool that allows a small insurance operations team to:
1. Accept structured claim submissions with supporting document metadata.
2. Automatically surface rule-based suspicious indicators at the point of review.
3. Capture structured reviewer decisions (approve/reject with written remarks).
4. Maintain a full, immutable audit trail of status changes.
5. Be deployed and updated through a reproducible automated pipeline.

This project addresses that gap by delivering a dashboard application that integrates claim management, indicator display, and reviewer workflow into a single system, backed by a Jenkins-managed CI/CD pipeline.

### 2.3 Academic Context

This project is submitted as part of a college DevOps module. The assignment requires students to demonstrate practical competence across fourteen defined tasks, spanning requirements engineering, UI/UX design, backend development, automated testing, containerisation, orchestration, and deployment automation. The application domain (insurance fraud review) has been chosen because it provides realistic, relatable business logic that can be modelled without sensitive data.

---

## 3. Existing Problems in Manual Insurance Claim Review

The following problems have been identified through analysis of publicly available industry literature and generalised operational knowledge. No proprietary or confidential insurer data has been used.

| # | Problem | Impact |
|---|---------|--------|
| 1 | **Inconsistent review criteria** — Each reviewer applies their own heuristics, leading to different outcomes for similar claims. | Unfair decisions; legal risk. |
| 2 | **No centralised claim status tracking** — Status updates are recorded in email threads or spreadsheets, making audit difficult. | Lack of traceability; compliance risk. |
| 3 | **Delayed suspicious-indicator identification** — Indicators are often noticed only after preliminary payment approval. | Financial loss; delayed recovery. |
| 4 | **Manual document validation** — Reviewers must cross-check document metadata against claim fields manually. | Time-consuming; error-prone. |
| 5 | **No structured remarks system** — Reject decisions are communicated verbally or via free-form email rather than in a structured, queryable format. | Difficult appeals process; knowledge loss. |
| 6 | **Fragmented tooling** — Claims intake, fraud review, and decision records exist across multiple disconnected tools. | Duplication of effort; version conflicts. |
| 7 | **No automated deployment or rollback** — Updates to review tools are applied manually, risking downtime and inconsistent environments. | Service interruptions; deployment failures. |
| 8 | **Limited reporting capability** — Management has no real-time view of claim volume, review backlog, or indicator hit rates. | Poor resource planning; missed trends. |

---

## 4. Target Users and Their Responsibilities

### 4.1 Claim Submitter (Policyholder Representative or Data Entry Operator)

| Attribute | Detail |
|-----------|--------|
| **Who** | An operator who enters claim details on behalf of a policyholder, or a policyholder accessing a self-service portal. |
| **Primary task** | Submit a new insurance claim with all required fields and supporting document metadata. |
| **Secondary task** | Check the current status of a previously submitted claim. |
| **Technical level** | Low to medium — expects a simple, guided form with clear validation messages. |
| **Key concern** | Confirmation that the claim has been received and is being processed. |

### 4.2 Fraud Reviewer (Claims Analyst)

| Attribute | Detail |
|-----------|--------|
| **Who** | A trained claims analyst responsible for reviewing flagged claims. |
| **Primary task** | Open a pending claim, review the claim details and any suspicious indicators, then record an approval or rejection decision with written remarks. |
| **Secondary task** | View the review history of a claim to understand previous decisions. |
| **Technical level** | Medium — comfortable with web-based tools; not a developer. |
| **Key concern** | Clear presentation of indicators, full claim context, and a structured way to record decisions. |

### 4.3 Operations Manager / Supervisor

| Attribute | Detail |
|-----------|--------|
| **Who** | A manager overseeing the fraud review team. |
| **Primary task** | Monitor claim volumes, review backlog, and decision outcomes through the dashboard summary. |
| **Secondary task** | Identify reviewers with high rejection or approval rates to ensure consistency. |
| **Technical level** | Low to medium — expects summary cards and simple filters. |
| **Key concern** | Visibility into team productivity and claim pipeline health. |

### 4.4 DevOps Engineer / System Administrator (Academic Role)

| Attribute | Detail |
|-----------|--------|
| **Who** | The student developer (Raj Yadav) acting in the DevOps role for this project. |
| **Primary task** | Build, test, containerise, and deploy the application using Jenkins, Docker, and Ansible. |
| **Secondary task** | Configure the CI/CD pipeline, write Selenium tests, and maintain environment configuration files. |
| **Technical level** | High — works with build tools, container runtimes, and automation scripts. |
| **Key concern** | A reproducible, automated pipeline that builds and deploys the application without manual steps. |

---

## 5. Stakeholder List

| Stakeholder | Role | Interest | Influence |
|-------------|------|----------|-----------|
| **Raj Yadav (Student Developer)** | Developer, DevOps Engineer, Project Owner | Successful completion of all 14 assignment tasks; academic grade. | High |
| **College Assessor / Module Lecturer** | Evaluator | Assessment of technical competence across the full DevOps lifecycle. | High |
| **Claim Submitters** (simulated) | End user | Accurate claim submission and status visibility. | Medium |
| **Fraud Reviewers** (simulated) | End user | Efficient, structured claim review workflow. | Medium |
| **Operations Manager** (simulated) | End user | Real-time dashboard visibility and reporting. | Medium |
| **Open-Source Community** | Indirect stakeholder | If the repository is made public, other students or practitioners may reference the project. | Low |

> All end-user personas listed above are **simulated** for the purpose of this academic project. No actual insurance company staff are involved.

---

## 6. Project Objectives

The following objectives define what this project aims to achieve by the end of all fourteen assignment tasks. They are stated as **planned targets** and have not yet been achieved.

### Primary Objectives

1. **Design a functional Claim Fraud Review Dashboard** that supports claim submission, rule-based indicator display, reviewer decisions, and audit history.
2. **Implement a Jenkins CI/CD pipeline** that automatically builds, tests, and deploys the application on every code push.
3. **Containerise the application using Docker** to ensure environment consistency across development, testing, and deployment stages.
4. **Automate environment provisioning using Ansible** to eliminate manual configuration steps.
5. **Demonstrate end-to-end automated testing** using Selenium WebDriver and JUnit to validate critical user workflows.

### Secondary Objectives

6. **Design the frontend UI in Google Stitch** before implementing it, to ensure a professional, structured user interface that is reviewed and approved before coding begins.
7. **Maintain clear, version-controlled documentation** for each of the fourteen assignment tasks within the project repository.
8. **Apply DevOps principles** — automation, repeatability, traceability, and continuous improvement — throughout the project lifecycle.
9. **Use only synthetic data** to protect privacy and maintain academic integrity.
10. **Produce a reproducible project** that a reviewer can clone, build, and run using the documented pipeline without additional configuration.

---

## 7. Functional Requirements

The following functional requirements describe what the system must do. They are categorised by user-facing feature area.

### 7.1 Claim Submission

| ID | Requirement |
|----|-------------|
| FR-01 | The system shall provide a web form that allows a user to submit a new insurance claim. |
| FR-02 | The claim form shall capture: claimant name, policy number, claim type (health / motor / property), incident date, claim amount, and a brief description. |
| FR-03 | The system shall validate all mandatory fields before accepting submission; invalid submissions shall display clear inline error messages. |
| FR-04 | The system shall capture document metadata (document type, reference number, upload date) associated with each claim. |
| FR-05 | On successful submission, the system shall assign a unique Claim ID and set the claim status to `PENDING`. |
| FR-06 | The submitter shall be able to view the current status of a submitted claim by searching with the Claim ID. |

### 7.2 Suspicious Indicator Detection

| ID | Requirement |
|----|-------------|
| FR-07 | The system shall evaluate each submitted claim against a configurable set of rule-based indicators upon submission. |
| FR-08 | Indicator rules shall include (at minimum): claim amount exceeding a defined threshold, incident date falling on a weekend or public holiday, multiple claims from the same policy number within a defined period, and document reference number matching a known synthetic test pattern. |
| FR-09 | Each triggered indicator shall be displayed on the claim review screen with a plain-language description. |
| FR-10 | Indicators shall be labelled as **"Suspicious Signal"** and shall include a disclaimer stating they do not constitute proof of fraud. |
| FR-11 | The number of triggered indicators per claim shall be displayed as a count on the claim list view. |

### 7.3 Reviewer Workflow

| ID | Requirement |
|----|-------------|
| FR-12 | The system shall display a list of all claims with current status, indicator count, and submission date, sortable by each column. |
| FR-13 | A reviewer shall be able to open a claim detail page showing all claim fields, document metadata, triggered indicators, and review history. |
| FR-14 | A reviewer shall be able to record a decision of `APPROVED` or `REJECTED`, along with mandatory written remarks. |
| FR-15 | After a decision is recorded, the claim status shall be updated accordingly and the decision shall be appended to the claim's review history. |
| FR-16 | A reviewer shall not be able to submit a decision without providing remarks. |

### 7.4 Audit and History

| ID | Requirement |
|----|-------------|
| FR-17 | The system shall maintain an immutable review history for each claim, recording: reviewer name, decision, remarks, and timestamp. |
| FR-18 | The review history shall be visible on the claim detail page in chronological order. |
| FR-19 | The system shall display a dashboard summary showing: total claims, claims by status, and average indicator count. |

### 7.5 CI/CD and Deployment

| ID | Requirement |
|----|-------------|
| FR-20 | The application shall be buildable from source using a single Maven command. |
| FR-21 | The Jenkins pipeline shall execute build, unit test, and packaging stages automatically on code push. |
| FR-22 | The Jenkins pipeline shall build a Docker image and push it to a container registry (or local registry for academic purposes). |
| FR-23 | Ansible playbooks shall provision the target environment and deploy the Docker container without manual steps. |
| FR-24 | The Selenium test suite shall execute as part of the Jenkins pipeline and report pass/fail results. |

---

## 8. Non-Functional Requirements

| ID | Category | Requirement |
|----|----------|-------------|
| NFR-01 | **Performance** | The claim list page shall load within 3 seconds for a dataset of up to 500 synthetic claims on the development machine. *(Planned target — not yet measured.)* |
| NFR-02 | **Usability** | The reviewer workflow (open claim → review indicators → record decision) shall be completable in no more than 5 clicks from the claim list. *(Planned target — not yet validated.)* |
| NFR-03 | **Reliability** | The Jenkins pipeline shall complete successfully without manual intervention for a clean code push. *(Planned target — not yet tested.)* |
| NFR-04 | **Maintainability** | All application configuration values (indicator thresholds, database connection strings) shall be externalised in configuration files, not hard-coded. |
| NFR-05 | **Portability** | The application shall run inside a Docker container and shall not require any host-specific configuration beyond Docker being installed. |
| NFR-06 | **Security (Basic)** | The reviewer decision endpoint shall not be accessible without a valid session. Basic session-based authentication shall be implemented for the reviewer role. |
| NFR-07 | **Auditability** | All review decisions shall be stored with timestamps and shall not be modifiable once recorded. |
| NFR-08 | **Testability** | The application shall expose deterministic, testable UI flows that can be driven by Selenium WebDriver without flaky timing dependencies. |
| NFR-09 | **Scalability (Academic scope)** | The system is designed for a single-node deployment suitable for academic demonstration; horizontal scaling is out of scope. |
| NFR-10 | **Documentation** | Every assignment task shall have a corresponding document in the `docs/` directory committed to the repository. |

---

## 9. Project Constraints and Assumptions

### 9.1 Constraints

| # | Constraint |
|---|-----------|
| C-01 | The project must be completed within the academic semester timeline defined by the module assessment schedule. |
| C-02 | All tools used (Jenkins, Docker, Ansible, Selenium, Java, Maven) must be open-source or freely available under a licence compatible with academic use. |
| C-03 | No real policyholder data may be used at any stage; all data must be synthetic. |
| C-04 | The project must be self-contained within the `Claim-Fraud-Review-Dashboard` repository; no external paid services may be required to run it. |
| C-05 | The H2 in-memory database is the target database for development and testing; persistence across restarts is a planned enhancement, not a core requirement. |
| C-06 | The Google Stitch frontend design must be exported and inspected before a final decision is made on the integration approach (Thymeleaf server-side rendering vs. a separately served frontend). |
| C-07 | Docker must be the sole container runtime used; Kubernetes or other orchestrators are out of scope. |

### 9.2 Assumptions

| # | Assumption |
|---|-----------|
| A-01 | Java 21, Maven, Docker, Jenkins, and Ansible will be installed on the development machine before the relevant tasks begin. |
| A-02 | A GitHub repository will be used for version control and as the source for Jenkins pipeline triggers. |
| A-03 | The development machine has sufficient resources to run Jenkins, a Docker daemon, and the application simultaneously. |
| A-04 | Google Stitch will produce exportable HTML/CSS or component files that can be inspected and adapted for the chosen frontend technology. |
| A-05 | The H2 in-memory database will be pre-seeded with synthetic claim data for demonstration and testing purposes. |
| A-06 | The indicator rules will be simple, deterministic, and hard-coded in the first version; a configurable rules engine is a future enhancement. |
| A-07 | The project assessor has access to the GitHub repository and can clone and run the pipeline independently. |

---

## 10. Project Risks and Mitigation Measures

| ID | Risk | Likelihood | Impact | Mitigation |
|----|------|-----------|--------|-----------|
| R-01 | **Jenkins pipeline configuration complexity** — Pipeline syntax errors or plugin conflicts cause build failures. | Medium | High | Start with a minimal Jenkinsfile (build only), add stages incrementally, and commit each working stage separately. |
| R-02 | **Google Stitch export compatibility** — Exported frontend files may not integrate cleanly with Thymeleaf or a separate frontend framework. | Medium | Medium | Inspect exported files before committing to an integration approach; document the decision in Task 3. Fallback: implement UI directly in Thymeleaf without Stitch export. |
| R-03 | **Selenium test flakiness** — Tests pass locally but fail in the Jenkins environment due to timing or environment differences. | Medium | High | Use explicit waits (not `Thread.sleep`), isolate tests from each other, and run against the packaged JAR, not a dev server. |
| R-04 | **H2 data loss on restart** — In-memory database loses all data when the application restarts, making demo preparation difficult. | High | Low | Use H2 file mode for demo runs; document the configuration switch. |
| R-05 | **Docker image size and build time** — Large image sizes slow the Jenkins pipeline. | Low | Medium | Use a multi-stage Dockerfile; build the fat JAR first and copy only the JAR into a slim JRE base image. |
| R-06 | **Ansible connectivity issues** — Ansible playbooks cannot connect to the target host in the academic environment. | Medium | High | Test Ansible with a local target (localhost) first; document the inventory configuration clearly. |
| R-07 | **Scope creep** — Adding features beyond the MVP delays core pipeline and testing tasks. | Medium | Medium | Maintain a strict feature freeze on the MVP; log all additional ideas in a backlog section of the README. |
| R-08 | **Academic deadline pressure** — Later tasks (Ansible, full Selenium suite) require significant setup time. | High | High | Follow the task sequence strictly; do not start Task N+1 until Task N documentation and evidence are committed. |

---

## 11. Measurable Project Success Criteria

The following criteria define what "done" looks like for this project. All criteria are **planned targets**; none have been achieved at the time of writing (Task 1).

| ID | Criterion | Measurement Method | Target |
|----|----------|--------------------|--------|
| SC-01 | A claim can be submitted through the web form and assigned a Claim ID. | Manual functional test + Selenium test. | 100% pass rate on defined test cases. |
| SC-02 | At least three distinct indicator rules fire correctly on synthetic test claims designed to trigger them. | Unit tests on the rules engine. | 100% pass rate. |
| SC-03 | A reviewer can record an approve/reject decision with remarks, and the decision appears in the review history. | Manual functional test + Selenium test. | 100% pass rate. |
| SC-04 | The Jenkins pipeline completes all stages (build → test → Docker image → deploy) without manual intervention on a clean push. | Jenkins build log. | Zero manual steps required; green pipeline. |
| SC-05 | The Docker container starts and serves the application correctly on the target port. | `curl` health-check or browser test post-deploy. | Application reachable within 60 seconds of container start. |
| SC-06 | Ansible playbook provisions the environment and deploys the container from scratch on a clean target. | Ansible run log + post-deploy health check. | Playbook completes without errors. |
| SC-07 | At least five Selenium tests cover the core reviewer workflow end-to-end. | Jenkins test report. | All five tests pass in CI. |
| SC-08 | All fourteen task documents exist in the `docs/` directory and are committed to the repository. | Repository file listing. | 14 documents present at project completion. |
| SC-09 | *(Planned target — must be measured, not yet achieved.)* Target a dashboard load time of 3 seconds or less when displaying 500 synthetic claim records under a documented local test environment. | Browser developer tools timing recorded during a local load test; test environment hardware and configuration must be documented at measurement time. | Dashboard renders within 3 seconds at the p95 percentile under the documented local test environment conditions. |
| SC-10 | The project can be cloned and run by a third party using only the README instructions. | Peer review / assessor verification. | Successful independent run confirmed. |

---

## 12. Features Included in the MVP

The Minimum Viable Product (MVP) is the smallest version of the application that demonstrates all required DevOps pipeline stages and provides sufficient application functionality to run meaningful automated tests.

| # | Feature | Justification |
|---|---------|--------------|
| MVP-01 | **Claim submission form** with mandatory field validation. | Core data entry feature; required to populate the system. |
| MVP-02 | **Claim list view** showing all submitted claims with status and indicator count. | Provides the reviewer entry point. |
| MVP-03 | **Claim detail page** showing all fields, indicators, and review history. | Enables the reviewer decision workflow. |
| MVP-04 | **Rule-based suspicious indicator engine** (minimum 3 rules). | Demonstrates domain logic; provides testable assertions. |
| MVP-05 | **Approve/Reject decision form** with mandatory remarks. | Core reviewer action; required for audit history. |
| MVP-06 | **Review history display** per claim in chronological order. | Demonstrates auditability. |
| MVP-07 | **Dashboard summary** (total claims, status counts, indicator counts). | Demonstrates the "dashboard" aspect of the application. |
| MVP-08 | **H2 in-memory database** with synthetic seed data. | Lightweight; no external DB dependency; suitable for CI. |
| MVP-09 | **Basic session-based authentication** for the reviewer role. | Satisfies NFR-06; demonstrates security awareness. |
| MVP-10 | **Jenkins CI/CD pipeline** (build, test, Docker image, deploy). | Core DevOps deliverable for the assignment. |
| MVP-11 | **Dockerfile** producing a runnable application image. | Required for pipeline and Ansible deployment. |
| MVP-12 | **Ansible playbook** for environment setup and container deployment. | Required by the assignment task list. |
| MVP-13 | **Selenium WebDriver test suite** (minimum 5 tests) integrated into Jenkins. | Required by the assignment task list. |
| MVP-14 | **Google Stitch UI design** reviewed and integrated (or documented as a design reference). | Required by the assignment task list. |

---

## 13. Features Excluded from the MVP

The following features are explicitly out of scope for the MVP. They may be added in a future enhancement phase if time permits.

| # | Feature | Reason for Exclusion |
|---|---------|---------------------|
| EX-01 | **Machine learning fraud scoring** | Requires training data and ML infrastructure; beyond DevOps module scope. |
| EX-02 | **Real document upload and storage** | Introduces file storage complexity and data privacy concerns; metadata only is sufficient for MVP. |
| EX-03 | **Email or SMS notification to claimants** | Requires third-party messaging services; out of academic scope. |
| EX-04 | **Multi-node / clustered deployment** | Academic environment is single-node; Kubernetes or Swarm is out of scope. |
| EX-05 | **Configurable rules engine UI** | Rules are defined in code/config for MVP; a UI for rule management is a future enhancement. |
| EX-06 | **Role-based access control beyond reviewer/submitter** | Only two roles are needed for MVP; fine-grained RBAC is a future enhancement. |
| EX-07 | **Real-time WebSocket updates** | Polling or page refresh is sufficient for MVP; WebSockets add implementation complexity. |
| EX-08 | **Production database (PostgreSQL / MySQL)** | H2 is sufficient for academic demonstration; production DB migration is a future step. |
| EX-09 | **Kubernetes / Helm deployment** | Docker Compose or direct container deployment via Ansible is sufficient. |
| EX-10 | **Reporting and analytics exports (PDF/CSV)** | Not required by the assignment; adds UI and library complexity. |
| EX-11 | **Internationalisation (i18n)** | Single-language (English) application is sufficient for the academic context. |
| EX-12 | **Mobile-responsive design beyond basic usability** | The UI will be designed to be readable on standard desktop/laptop screens. |

---

## 14. Scope Aligned with All 14 Official Assignment Tasks

The table below maps each official assignment task to the scope and deliverables planned for this project.

| Task | Title (Planned) | Key Deliverables | Application Scope Contribution |
|------|----------------|------------------|-------------------------------|
| **Task 01** | Problem Definition and Scope | `docs/task-01-problem-definition.md`, `README.md` | Defines what the system is, who uses it, what it does, and what is in/out of scope. |
| **Task 02** | UI/UX Design (Google Stitch) | Stitch project link/export, `docs/task-02-ui-design.md` | Produces the visual design and screen layouts before implementation. Stitch export will be inspected to determine integration approach. |
| **Task 03** | Frontend Integration | Frontend source files, `docs/task-03-frontend.md` | Implements the Stitch design as Thymeleaf templates or a separate frontend; documents the integration decision. |
| **Task 04** | Backend Development | Spring Boot source code, `docs/task-04-backend.md` | Implements claim submission, indicator engine, reviewer decision API, and audit history using Java 21 and Spring Boot. |
| **Task 05** | Database Design and Seed Data | Entity classes, schema, synthetic seed SQL/H2 init, `docs/task-05-database.md` | Defines claim, indicator, decision, and history data models; provides synthetic seed data. |
| **Task 06** | Version Control and Branching Strategy | `.gitignore`, branch naming conventions, `docs/task-06-version-control.md` | Establishes the Git workflow (feature branches, pull requests, commit conventions) used throughout the project. |
| **Task 07** | Jenkins Pipeline Setup | `Jenkinsfile`, `docs/task-07-jenkins.md` | Configures the Jenkins server and defines the CI pipeline stages (build, test, package). |
| **Task 08** | Automated Testing (Unit + Integration) | JUnit test classes, test report, `docs/task-08-testing.md` | Implements unit tests for the rules engine and integration tests for the API endpoints. |
| **Task 09** | Selenium End-to-End Tests | Selenium test classes, `docs/task-09-selenium.md` | Implements at least five browser-driven tests covering claim submission and reviewer decision workflow; integrated into Jenkins. |
| **Task 10** | Docker Containerisation | `Dockerfile`, `docker-compose.yml` (if used), `docs/task-10-docker.md` | Packages the application as a Docker image; documents the image build and run process. |
| **Task 11** | Jenkins + Docker Integration | Updated `Jenkinsfile` with Docker stages, `docs/task-11-jenkins-docker.md` | Adds Docker image build and push stages to the Jenkins pipeline. |
| **Task 12** | Ansible Configuration Management | Ansible playbooks, inventory, `docs/task-12-ansible.md` | Automates environment provisioning and container deployment on the target host. |
| **Task 13** | Full Pipeline Demonstration | Pipeline run screenshots/logs, `docs/task-13-pipeline-demo.md` | Demonstrates the complete end-to-end pipeline (code push to build to test to Docker to Ansible deploy to health check). |
| **Task 14** | Project Retrospective and Lessons Learned | `docs/task-14-retrospective.md` | Reflects on what worked, what was challenging, and what would be improved in a real-world project. |

> **Note:** Task titles marked "Planned" above are working titles subject to revision based on the official assignment brief as tasks are undertaken.

---

## 15. Conclusion

This document has defined the scope, objectives, requirements, risks, and constraints for the **Jenkins Deployment for a Claim Fraud Review Dashboard** project. The application addresses a real and well-understood problem in the insurance industry — the need for a structured, auditable, rule-assisted claim fraud review process — within a tightly scoped academic context.

Key decisions established at this stage:

- **All data is synthetic.** No real policyholder information will be used.
- **Suspicious indicators are decision-support signals only.** They do not constitute proof of fraud.
- **The frontend will be designed in Google Stitch first.** The exported files will be inspected before the integration approach is finalised.
- **The technology stack** (Java 21, Spring Boot, Maven, Thymeleaf/separate frontend, H2, Git, GitHub, Jenkins, Selenium, Docker, Ansible) is confirmed as the planned stack. Deviations will be documented.
- **All success criteria are planned targets,** not yet achieved or verified.
- **Implementation, testing, deployment, and pipeline runs have not yet been performed.** This document records the intent and plan only.

The project will proceed task by task, with documentation committed to the repository at each stage. No task will be declared complete until the corresponding document exists, any required evidence (screenshots, logs, test reports) has been captured, and manual verification has been performed by the student.

---

*End of Task 01 — Problem Definition and Scope*

*Document prepared by Raj Yadav | College DevOps Project | 2026-10-21*
