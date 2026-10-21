# Claim Fraud Review Dashboard

> **College DevOps Project** | Jenkins Deployment for a Claim Fraud Review Dashboard

---

## Project Description

A web-based insurance **Claim Fraud Review Dashboard** that allows operators to submit insurance claims, surfaces rule-based suspicious indicators for trained reviewers, captures structured approve/reject decisions with mandatory remarks, and maintains an immutable audit trail of every review action.

The application is the subject of a college DevOps module assignment. The primary technical deliverable is a fully automated **Jenkins CI/CD pipeline** that builds, tests, containerises, and deploys the application using Docker and Ansible — demonstrating the complete software delivery lifecycle from requirements through to automated deployment.

> **Note:** All claim data used in this project is **synthetic**. No real policyholder data is used at any stage.<br>
> Suspicious-indicator flags are decision-support signals only and do not constitute proof of fraud.<br>
> Implementation, testing, and deployment have **not yet been performed or verified** as of the current development stage.

---

## Project Objectives

1. Design a functional Claim Fraud Review Dashboard supporting claim submission, rule-based indicator display, reviewer decisions, and audit history.
2. Implement a Jenkins CI/CD pipeline that automatically builds, tests, and deploys the application on every code push.
3. Containerise the application using Docker to ensure environment consistency across all stages.
4. Automate environment provisioning using Ansible to eliminate manual configuration steps.
5. Demonstrate end-to-end automated testing using Selenium WebDriver and JUnit covering critical user workflows.
6. Design the frontend UI in Google Stitch before implementation and inspect exported files before choosing the final integration approach.
7. Maintain clear, version-controlled documentation for each of the fourteen assignment tasks.
8. Use only synthetic data throughout the project.

---

## Planned Technology Stack

| Layer | Technology | Version / Notes |
|-------|-----------|----------------|
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | Latest stable |
| **Build Tool** | Apache Maven | Latest stable |
| **Frontend** | Thymeleaf (server-side) or separate frontend | Decision pending Stitch export inspection |
| **UI Design** | Google Stitch | Design phase only |
| **Database** | H2 (in-memory) | Synthetic seed data; file mode for demos |
| **Version Control** | Git + GitHub | Feature branch workflow |
| **CI/CD** | Jenkins | Pipeline-as-code via `Jenkinsfile` |
| **Testing (Unit/Integration)** | JUnit 5 | Integrated into Maven build |
| **Testing (E2E)** | Selenium WebDriver | Browser-driven; integrated into Jenkins |
| **Containerisation** | Docker | Multi-stage Dockerfile |
| **Configuration Management** | Ansible | Playbooks for environment setup and deployment |

> All tools are open-source or freely available under licences compatible with academic use.

---

## Current Development Stage

**Task 1 — Problem Definition and Scope** *(In Progress)*

- [x] `docs/task-01-problem-definition.md` created — covers problem statement, stakeholders, requirements, risks, success criteria, MVP scope, and alignment with all 14 tasks.
- [x] `README.md` created — project overview and planned milestones.
- [ ] Manual verification by student pending.
- [ ] Git commit and push pending.

No application code has been written. No tests have been run. No pipeline has been configured. No software has been installed as part of this project.

---

## Planned Future Milestones

| Task | Title | Status |
|------|-------|--------|
| Task 01 | Problem Definition and Scope | **In Progress** |
| Task 02 | UI/UX Design (Google Stitch) | Not started |
| Task 03 | Frontend Integration | Not started |
| Task 04 | Backend Development (Spring Boot) | Not started |
| Task 05 | Database Design and Seed Data | Not started |
| Task 06 | Version Control and Branching Strategy | Not started |
| Task 07 | Jenkins Pipeline Setup | Not started |
| Task 08 | Automated Testing (Unit + Integration) | Not started |
| Task 09 | Selenium End-to-End Tests | Not started |
| Task 10 | Docker Containerisation | Not started |
| Task 11 | Jenkins + Docker Integration | Not started |
| Task 12 | Ansible Configuration Management | Not started |
| Task 13 | Full Pipeline Demonstration | Not started |
| Task 14 | Project Retrospective and Lessons Learned | Not started |

Each task will have a corresponding document in the `docs/` directory. No task will be marked complete until the document is committed, evidence has been captured, and manual verification has been performed.

---

## Repository Structure (Planned)

```
Claim-Fraud-Review-Dashboard/
├── README.md
├── docs/
│   ├── task-01-problem-definition.md   ← Current task
│   ├── task-02-ui-design.md
│   ├── task-03-frontend.md
│   └── ...
├── src/                                ← Java/Spring Boot source (Task 04+)
├── Jenkinsfile                         ← CI/CD pipeline definition (Task 07+)
├── Dockerfile                          ← Container definition (Task 10+)
└── ansible/                            ← Ansible playbooks (Task 12+)
```

---

## Important Notices

- **No results have been fabricated.** All success criteria in `docs/task-01-problem-definition.md` are labelled as planned targets.
- **No software has been installed, configured, or run** as part of this project at this stage.
- **Frontend integration approach is undecided.** The Google Stitch export will be inspected in Task 2 before the approach (Thymeleaf vs. separate frontend) is finalised.

---

*Claim Fraud Review Dashboard | College DevOps Assignment | Raj Yadav | 2026*
