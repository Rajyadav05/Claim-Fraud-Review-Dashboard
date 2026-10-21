# Task 6 — MVP Completion and Git Collaboration

**Project:** ClaimGuard — Claim Fraud Review Dashboard
**Assignment module:** Jenkins Deployment for a Claim Fraud Review Dashboard
**Author:** Raj Yadav
**Date:** 2026-10-22
**Branch at verification:** `feature/claim-review-mvp`

---

## 1. Task Objective and Scope

Task 6 covers three interconnected activities:

1. **MVP Completion** — Verify that the Spring Boot application implemented during Task 5 satisfies the functional requirements defined in `docs/task-03-requirements-architecture.md`. Run all automated tests and the Maven package goal with the Java 21 JDK and confirm that API and static-resource endpoints respond correctly.

2. **Documentation Accuracy** — Confirm that `README.md` and Task 1–3 documents accurately represent the implemented and planned feature sets. Make only minimal, targeted corrections where documentation was inaccurate or silent on a key limitation.

3. **Git Collaboration Workflow** — Document the proposed team workflow (feature branch → pull request → review → merge → post-merge verification) without executing the remote steps, because the feature branch has not yet been authorized for push or merge into `main`.

**Out of scope for Task 6:**
- Selenium end-to-end browser testing (Task 9).
- Jenkins CI pipeline configuration (Task 7).
- Docker containerisation (Task 10).
- Ansible provisioning (Task 12).
- Any force-push, rebase, remote branch creation, or pull request creation.

---

## 2. Repository and Branch State

### Before verification (at task start)

```
Branch:   feature/claim-review-mvp
HEAD:     3c00fa8  Implement ClaimGuard claim review MVP
Parent:   02386fe  Initial commit: Task 1-4 project baseline and Stitch UI reference exports
Remote:   origin → https://github.com/Rajyadav05/Claim-Fraud-Review-Dashboard.git
main:     02386fe  [origin/main]
Working tree: clean
```

`git branch -vv` output:
```
* feature/claim-review-mvp  3c00fa8  Implement ClaimGuard claim review MVP
  main                      02386fe  [origin/main]  Initial commit: Task 1-4 project baseline ...
```

`feature/claim-review-mvp` has **no upstream tracking branch** — it exists only locally. It is 1 commit ahead of `main`.

### After verification (at task end)

Working-tree only changes (not yet committed — awaiting author authorization):
- `README.md` — notices block expanded with three additional clarifying notices.
- `docs/task-06-mvp-completion-and-git-collaboration.md` — this file (newly created).

No application source files were modified. No new commits were created.

---

## 3. MVP Functionality Checklist

| # | Requirement or Behaviour | Evidence Type | Result | Issue / Severity | Recommended Action |
|---|:---|:---|:---:|:---|:---|
| 1 | Application starts without errors using Java 21 and H2 | Automated test bootstrap log | **PASS** | None | None |
| 2 | `GET /api/health` responds with `{"status":"UP"}` | MockMvc test (`ClaimControllerTest`) | **PASS** | None | None |
| 3 | `GET /actuator/health` includes DB component detail | `application.properties` + boot log | **PASS** | None | None |
| 4 | Claim list `GET /api/claims` returns paginated results | MockMvc integration test; JPQL query inspected | **PASS** | None | None |
| 5 | Claim filtering by status, risk, and free-text search | JPQL parameters and `ClaimController` params inspected | **PASS** (code) | Not verified via live HTTP in Task 6 (no running server) | Run `.\mvnw.cmd spring-boot:run` then verify manually |
| 6 | Claim detail `GET /api/claims/{id}` | MockMvc test (`shouldReturnClaimById`) | **PASS** | None | None |
| 7 | Claim detail by reference `GET /api/claims/reference/{ref}` | `ClaimService` and `ClaimRepository` inspected | **PASS** (code) | No dedicated automated test | Low: add in Task 9 |
| 8 | Claim submission `POST /api/claims` with Jakarta validation | `CreateClaimRequest` annotations; `ClaimServiceTest.shouldCreateClaimSuccessfully` | **PASS** | None | None |
| 9 | Reference auto-generation `CLM-YYYY-NNNN` | `ClaimService.createClaim()` code + unit test | **PASS** | None | None |
| 10 | Duplicate claim detection RUL-DUP-01 | `IndicatorRuleEngine` code inspected | **PASS** (code) | No dedicated unit test for duplication path | Low: add in Task 8 |
| 11 | Start review `PATCH /api/claims/{id}/review` | `ClaimService.startReview()` guards inspected | **PASS** (code) | No dedicated automated test | Low: add in Task 8 |
| 12 | Approve `POST /api/claims/{id}/approve` | `ClaimServiceTest.shouldApproveClaimSuccessfully` PASSED | **PASS** | None | None |
| 13 | Reject `POST /api/claims/{id}/reject` | `ClaimServiceTest.shouldRejectClaimSuccessfully` PASSED | **PASS** | None | None |
| 14 | Request more info `POST /api/claims/{id}/request-info` | `ClaimService.requestMoreInfo()` and `ReviewController` inspected | **PASS** (code) | No dedicated automated test | Low |
| 15 | Invalid state transition rejected with HTTP 409 | `ClaimServiceTest.shouldThrowWhenApprovingAlreadyResolvedClaim` | **PASS** | None | None |
| 16 | Payout calculation: `approvedAmount = claimedAmount - deductible` (floor 0) | Unit test asserts Rs 1,85,000 - Rs 5,000 = Rs 1,80,000 | **PASS** | None | None |
| 17 | Audit history on every state change | All six `ReviewHistory` append paths inspected in `ClaimService` | **PASS** (code) | No lifecycle end-to-end test | Low: deferred to Task 9 |
| 18 | Document metadata `POST /api/claims/{id}/documents` | `ClaimController.addDocument()` and service inspected | **PASS** (code) | No dedicated automated test | Low |
| 19 | `MORE_INFO_REQUIRED` to `UNDER_REVIEW` transition on document upload | `ClaimService.addDocumentMetadata()` inspected | **PASS** (code) | No dedicated automated test | Low |
| 20 | RUL-AMT-01 high value flag (>= Rs 1,50,000) | `IndicatorRuleEngineTest.shouldTriggerHighValueThresholdRule` PASSED | **PASS** | None | None |
| 21 | RUL-TIM-01 delayed reporting flag (> 48 h) | `IndicatorRuleEngineTest.shouldTriggerDelayedReportingRule` PASSED | **PASS** | None | None |
| 22 | RUL-MTR-04 parts benchmark discrepancy for MOTOR | `IndicatorRuleEngine` code inspected; no unit test | **PASS** (code) | Low: add unit test in Task 8 | See Section 8 |
| 23 | RUL-DUP-01 duplicate policy + incident date | Code inspected; see row 10 | **PASS** (code) | Low | See Section 8 |
| 24 | RUL-DOC-02 missing mandatory document | `IndicatorRuleEngineTest.shouldTriggerMissingDocumentRule` PASSED | **PASS** | None | None |
| 25 | Risk priority aggregation: HIGH if any indicator is HIGH | `IndicatorRuleEngineTest.shouldCalculateHighOverallPriority` PASSED | **PASS** | None | None |
| 26 | Indicators described as heuristics, not proof of fraud | `IndicatorRuleEngine` Javadoc + `README.md` + HTML page disclaimers | **PASS** | None | None |
| 27 | Synthetic sample data clearly identified | `DataSeederService` uses fictional names/policies; guarded to not reseed | **PASS** | None | None |
| 28 | H2 in-memory data resets on restart | `jdbc:h2:mem:claimguard`; `DataSeederService` re-seeds at startup | **PASS** | Intentional — documented | None |
| 29 | H2 console accessible at `/h2-console` | `spring.h2.console.enabled=true`; confirmed in boot log | **PASS** | None | None |
| 30 | Static pages served by Spring Boot | `WelcomePageHandlerMapping` confirmed in boot log | **PASS** (routing) | Interactive browser rendering deferred to Task 9 | Run and verify manually |
| 31 | No secrets, credentials, or build artifacts tracked | `.gitignore` covers `target/`, `*.jar`, `.env`, `credentials.json`; diff check clean | **PASS** | None | None |
| 32 | Stitch reference screens unchanged | `git diff -- docs/stitch-export/` exit 0, empty output | **PASS** | None | None |

**Checklist summary (32 items total):**

| Evidence category | Count | Rows |
|:---|---:|:---|
| Automated test PASS (JUnit 5 unit or MockMvc integration, direct assertion) | 14 | 1, 2, 4, 6, 8, 9, 12, 13, 15, 16, 20, 21, 24, 25 |
| PASS — configuration, boot log, or `git diff` evidence | 7 | 3, 26, 27, 28, 29, 31, 32 |
| PASS (code review only — no automated test or live HTTP in Task 6) | 10 | 5, 7, 10, 11, 14, 17, 18, 19, 22, 23 |
| PASS (server-side routing confirmed; browser render deferred to Task 9) | 1 | 30 |
| FAIL | 0 | — |
| NOT VERIFIED | 0 | — |

All 32 items carry a PASS result at one or more verification levels. The 10 code-review-only items and the 1 routing-only item are not backed by automated tests; they are flagged with `(code)` in the Result column and carry low-severity recommendations to add test coverage in Tasks 8–9.

---

## 4. Actual Test and Build Results

### 4.1 Java Runtime Note

`JAVA_HOME` is permanently set to the Adoptium JDK 21 installation at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`. This was confirmed by running:

```powershell
$env:JAVA_HOME
# C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot
java -version
# openjdk version "21.0.12.1" 2026-08-18 LTS
javac -version
# javac 21.0.12.1
```

All Maven wrapper commands (`.\ mvnw.cmd`) resolve to Java 21 without any additional prefix. Defect D-01 is **resolved**.

### 4.2 `.\mvnw.cmd test` result (Task 6 run, 2026-10-22)

```
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  14.666 s
[INFO] Finished at: 2026-10-22T00:16:16+05:30
```

| Test Class | Tests | Failures | Errors |
|:---|---:|---:|---:|
| `ClaimGuardApplicationTests` (context load) | 1 | 0 | 0 |
| `ClaimControllerTest` (MockMvc integration) | 2 | 0 | 0 |
| `IndicatorRuleEngineTest` (unit rules) | 4 | 0 | 0 |
| `ClaimServiceTest` (unit service) | 4 | 0 | 0 |
| **Total** | **11** | **0** | **0** |

JVM warnings about dynamic agent loading are non-fatal Mockito/ByteBuddy notices and do not indicate test failures.

### 4.3 `.\mvnw.cmd package` result (Task 6 run, 2026-10-22)

```
[INFO] Building jar: target\claimguard-dashboard-1.0.0-SNAPSHOT.jar
[INFO] Replacing main artifact ... with repackaged archive (BOOT-INF/).
[INFO] BUILD SUCCESS
[INFO] Total time:  24.102 s
[INFO] Finished at: 2026-10-22T00:17:00+05:30
```

Executable Spring Boot fat JAR: `target/claimguard-dashboard-1.0.0-SNAPSHOT.jar`

### 4.4 `git diff --check`

Zero trailing whitespace errors. Only a non-fatal line-ending normalisation notice for `README.md`. Exit code: 0.

---

## 5. Manual / API / Browser Verification Evidence

### 5.1 Verification scope and methodology

| Level | Tool | Covers | Applied this task |
|:---|:---|:---|:---:|
| Automated unit test | JUnit 5 + Mockito | Business logic, rule evaluation, service layer | Yes |
| Automated integration test | MockMvc + Spring Boot Test | HTTP routing, serialisation, error codes | Yes |
| HTTP / API check | curl / Invoke-WebRequest | Live server endpoints | Task 5 only (no server started in Task 6) |
| Interactive browser test | Manual or Selenium WebDriver | Page rendering, JS execution, form interactions | **Deferred to Task 9** |

**No Selenium end-to-end tests have been run or claimed as complete.**

### 5.2 API endpoints verified during Task 5 (reproduced as evidence)

The following endpoints were verified by live HTTP requests during Task 5 with the application running on `http://localhost:8080`:

| Endpoint | Method | Status | Observed Result |
|:---|:---|:---:|:---|
| `/api/health` | GET | 200 | `{"status":"UP","database":"UP"}` |
| `/actuator/health` | GET | 200 | Full component details, H2 DB status UP |
| `/api/claims` | GET | 200 | 5 seeded claims returned, paginated |
| `/api/claims/1` | GET | 200 | Claim detail with indicators and history |
| `POST /api/claims` | POST | 201 | New claim `CLM-2026-0006` created |
| `POST /api/claims/6/approve` | POST | 200 | Approved amount Rs 2,45,000 (Rs 2,50,000 minus Rs 5,000) |
| `/` (index.html) | GET | 200 | Static page served |
| `/claims.html` | GET | 200 | Static page served |
| `/submit.html` | GET | 200 | Static page served |
| `/review.html?id=1` | GET | 200 | Static page served |
| `/h2-console` | GET | 200 | H2 login page served |

### 5.3 Verification limitation

Static page delivery (HTTP 200) was confirmed. Interactive rendering of JavaScript API calls, DOM updates, loading spinners, and form submission flows has **not been verified by any automated means**. This gap is acknowledged and deferred to Selenium testing in Task 9.

---

## 6. Stitch Reference Preservation

```powershell
git diff -- docs/stitch-export/
# Output: (empty)
# Exit code: 0

git log --oneline -- docs/stitch-export/
# 02386fe  Initial commit: Task 1-4 project baseline and Stitch UI reference exports
```

**Result: PASS.** All four original Google Stitch HTML reference screens and the export README:
- Are tracked since commit `02386fe` only.
- Were not modified in commit `3c00fa8` (Task 5).
- Have zero working-tree modifications.

Files confirmed intact:
- `docs/stitch-export/screen-01-executive-dashboard.html`
- `docs/stitch-export/screen-02-all-claims.html`
- `docs/stitch-export/screen-03-submit-claim.html`
- `docs/stitch-export/screen-04-claim-details-review.html`
- `docs/stitch-export/README.md`

---

## 7. Git Collaboration Workflow

### 7.1 Current branch state

```
main (origin/main)               ── 02386fe
                                        \
feature/claim-review-mvp (local only) ── 3c00fa8
```

The feature branch exists only locally. It is 1 commit ahead of `main`.

### 7.2 Proposed workflow (not yet executed; all steps require your authorization)

#### Step A — Push feature branch

```powershell
git push -u origin feature/claim-review-mvp
```

#### Step B — Open Pull Request (GitHub UI)

1. Go to `https://github.com/Rajyadav05/Claim-Fraud-Review-Dashboard`
2. Click **Compare & pull request** for `feature/claim-review-mvp -> main`
3. Title: `Task 5 & 6: Implement ClaimGuard claim review MVP`
4. Description: summarise features, 11/11 test results, and known limitations
5. Assign yourself as reviewer

#### Step C — Code Review

Self-review checklist:
- No unintended files in diff
- `target/` absent from diff
- No secrets or credentials visible
- `docs/stitch-export/` shows no modifications

#### Step D — Merge

Use **"Create a merge commit"** to preserve explicit branch history:
```
Merge commit message: Merge feature/claim-review-mvp: Task 5 & 6 ClaimGuard MVP
```

#### Step E — Post-merge verification

```powershell
git checkout main
git pull origin main
git log --oneline -3
.\mvnw.cmd test
git diff HEAD~1 -- docs/stitch-export/
```

All 11 tests must pass; Stitch diff must be empty.

#### Step F — Branch cleanup (after successful verification)

```powershell
git branch -d feature/claim-review-mvp
git push origin --delete feature/claim-review-mvp
```

### 7.3 Why merge commit over rebase

A merge commit preserves the explicit branching history demonstrating the feature-branch workflow required by the assignment. Rebasing would rewrite commit `3c00fa8` and change its SHA, disrupting the audit trail.

---

## 8. Known Defects, Limitations, and Remaining Work

### 8.1 Known defects

| ID | Area | Description | Severity |
|:---|:---|:---|:---|
| D-01 | Environment | ~~System PATH defaulted to Java 8; JAVA_HOME unset.~~ **RESOLVED** — `JAVA_HOME` is now permanently set to `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` (Java 21.0.12.1 LTS). Confirmed via `java -version` and `javac -version`. | ~~Medium~~ **Resolved** |
| D-02 | Test coverage | RUL-DUP-01 and RUL-MTR-04 have no dedicated unit tests. Code confirmed correct by inspection. | Low |
| D-03 | Test coverage | `startReview`, `requestMoreInfo`, and `addDocumentMetadata` have no dedicated tests. | Low |
| D-04 | Test coverage | `GET /api/claims/reference/{ref}` has no automated test. | Low |
| D-05 | Frontend | Interactive browser behaviour not covered by any automated test. | Medium — deferred to Task 9 |

### 8.2 Known limitations (design decisions)

| ID | Area | Description |
|:---|:---|:---|
| L-01 | Persistence | H2 in-memory — all data resets on process restart; claims re-seeded automatically at startup. Intentional for MVP. |
| L-02 | File storage | Physical file content not stored; only metadata (filename, type, size, SHA-256) persisted. File upload is out of MVP scope. |
| L-03 | Authentication | No auth/authorisation. All REST endpoints publicly accessible. Deferred to future tasks or out of assignment scope. |
| L-04 | Fraud determination | Heuristic engine does not determine fraud. All indicator descriptions state results require manual reviewer evaluation. |
| L-05 | Browser testing | Selenium WebDriver E2E tests not yet written or executed. Planned for Task 9. |

### 8.3 Remaining planned tasks

| Task | Title | Key prerequisite |
|:---|:---|:---|
| Task 7 | Jenkins Pipeline Setup | D-01 resolved (JAVA_HOME = Java 21); feature branch pushed to GitHub |
| Task 8 | Automated Testing Pipeline Integration | Task 7 |
| Task 9 | Selenium End-to-End Testing | Tasks 7-8 |
| Task 10 | Docker Container Packaging | Task 7 |
| Task 11 | Jenkins + Docker Automated Pipeline | Tasks 7, 10 |
| Task 12 | Ansible Configuration Management | Task 11 |
| Task 13 | Full CI/CD Pipeline Demonstration | Tasks 7-12 |
| Task 14 | Project Retrospective and Viva Preparation | All tasks |

---

## 9. Screenshots and Evidence Still to Be Captured Manually

| # | Evidence Item | How to Capture |
|---|:---|:---|
| 1 | GitHub PR opened for `feature/claim-review-mvp -> main` | Screenshot of GitHub PR page |
| 2 | GitHub PR diff confirming `docs/stitch-export/` unmodified | Screenshot of Files Changed tab |
| 3 | GitHub merge confirmation and closed PR | Screenshot |
| 4 | Post-merge `git log --oneline -5` on `main` | Terminal output |
| 5 | Browser rendering of `index.html` (Executive Dashboard) | Screenshot of `http://localhost:8080/` |
| 6 | `claims.html` with live claim data loaded from API | Screenshot |
| 7 | `submit.html` form submission flow | Screenshot of confirmation |
| 8 | `review.html` with indicators, approve/reject/request-info controls | Screenshot for `CLM-2026-0001` |

---

## 10. Task 6 Acceptance Checklist

| # | Acceptance Criterion | Status |
|---|:---|:---:|
| 1 | All 11 automated tests pass with `.\mvnw.cmd test` using Java 21 | DONE |
| 2 | `.\mvnw.cmd package` produces a runnable Spring Boot JAR | DONE |
| 3 | No secrets, credentials, database files, or build output are tracked in Git | DONE |
| 4 | `docs/stitch-export/` reference screens unchanged | DONE |
| 5 | `README.md` accurately documents implemented vs planned, H2 lifecycle, heuristics disclaimer, Selenium deferral | DONE |
| 6 | `docs/task-06-mvp-completion-and-git-collaboration.md` created with all 10 sections | DONE (this file) |
| 7 | `git diff --check` produces zero whitespace errors | DONE |
| 8 | MVP functionality checklist with PASS/FAIL/NOT VERIFIED evidence | DONE (Section 3) |
| 9 | Selenium testing identified as deferred and not claimed as complete | DONE |
| 10 | Git collaboration workflow documented without executing unauthorized remote steps | DONE (Section 7) |
| 11 | JAVA_HOME system environment variable set to Java 21 permanently | DONE — confirmed `openjdk 21.0.12.1 LTS`, `javac 21.0.12.1` |
| 12 | Feature branch pushed to GitHub | PENDING — awaiting your authorization |
| 13 | Pull request opened on GitHub | PENDING — after branch push |
| 14 | Pull request reviewed and merged into `main` | PENDING — after PR opened |
| 15 | Post-merge `.\mvnw.cmd test` confirmed passing on `main` | PENDING — after merge |
| 16 | Browser screenshots captured for all four UI pages | PENDING — manual capture |

**Overall: 11 of 16 acceptance criteria met. 5 require your authorization and manual action on GitHub.**

---

## Appendix A — How to Run Locally on Windows

### One-time prerequisite

`JAVA_HOME` is already permanently set to `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` (Java 21.0.12.1 LTS). Verify with:
```powershell
$env:JAVA_HOME
java -version
javac -version
```
If the output does not show OpenJDK 21, add `JAVA_HOME` as a Windows System Environment Variable and add `%JAVA_HOME%\bin` to the front of the system `PATH`, then restart the terminal.

### Run tests

```powershell
cd "E:\Desktop\Raj Yadav\Projects\Claim-Fraud-Review-Dashboard"
.\mvnw.cmd test
```

Expected: `Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS`

### Package JAR

```powershell
.\mvnw.cmd clean package
```

Output: `target/claimguard-dashboard-1.0.0-SNAPSHOT.jar`

### Start application

```powershell
.\mvnw.cmd spring-boot:run
# or
java -jar target\claimguard-dashboard-1.0.0-SNAPSHOT.jar
```

Starts on port `8080`. All data resets and is re-seeded on every restart.

### Health checks

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/health" | Select-Object -ExpandProperty Content
Invoke-WebRequest -Uri "http://localhost:8080/actuator/health" | Select-Object -ExpandProperty Content
```

### UI URLs

| URL | Page |
|:---|:---|
| `http://localhost:8080/` | Executive Dashboard |
| `http://localhost:8080/claims.html` | All Claims |
| `http://localhost:8080/submit.html` | Submit Claim |
| `http://localhost:8080/review.html?id=1` | Claim Details and Review |
| `http://localhost:8080/h2-console` | H2 Console (JDBC: `jdbc:h2:mem:claimguard`, User: `sa`, Password: blank) |