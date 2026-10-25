# ClaimGuard Phase 1 — Pre-Deployment Verification Audit
## Persistent H2 Database + Authentication & Authorization

> **Report Reference:** `CLAIMGUARD_PHASE1_PREDEPLOYMENT_VERIFICATION.md`  
> **Verification Date:** October 25, 2026  
> **Environment:** Windows (Host) / Docker Engine (WSL 2 Linux)  
> **Primary Service Under Protection:** `claimguard-staging` (`http://localhost:8083`, port `8083:8080`)  
> **Candidate Under Audit:** `claimguard:phase1-candidate`  
> **Audit Status:** VERIFICATION-ONLY (No code, configurations, containers, or databases modified)  

---

## Executive Summary

A comprehensive pre-deployment verification was conducted on the Phase 1 deliverables documented in `reports/CLAIMGUARD_PHASE1_IMPLEMENTATION_REPORT.md`. All 11 verification items were assessed against live container states, Docker image manifests, filesystem volumes, application configuration, source code, and Maven Surefire report artifacts.

The candidate image `claimguard:phase1-candidate` successfully satisfies persistence, non-duplicating seeding, and role-based access control requirements in an isolated testing environment. The active staging container `claimguard-staging` on port `8083` was confirmed **100% untouched and operational**.

Two critical operational nuances were identified and documented:
1. **Live H2 Backup Limitation:** Raw file copying (`docker cp`) on an active H2 database file can cause torn page corruption if writes occur concurrently. An atomic SQL-based (`BACKUP TO`) or quiesced-container backup procedure must be adopted.
2. **In-Memory State Volatility on Rollback:** Because the current `claimguard-staging` container uses `jdbc:h2:mem:claimguard`, stopping it for candidate cutover will reset any uncommitted in-memory claims to the 5 baseline seeded records upon restart.

---

## Verification Requirements & Results Matrix

| # | Verification Requirement | Status | Evidence & Reference |
|---|--------------------------|--------|----------------------|
| 1 | Candidate Image & Test Container Origin | **PASS** | `docker image inspect claimguard:phase1-candidate` confirmed Image ID `sha256:2fd25b07fba5787db431f3b2f10edaf8594d2fb5536580b92029ef7d58497a97` created at `2026-10-25T13:42:23Z`. Test container `claimguard-phase1-test` was instantiated directly from this image tag. |
| 2 | File-Backed H2 & Persistent Volume Mount | **PASS** | Image configuration sets `ENV CLAIMGUARD_DB_PATH=/app/data/claimguard`. Application property `spring.datasource.url` points to `jdbc:h2:file:${CLAIMGUARD_DB_PATH:./data/claimguard}`. Test container created `/app/data/claimguard.mv.db` (49,152 bytes) with ownership `claimguard:claimguard` (1001:1001). |
| 3 | Test Volume vs. Production Volume Distinction | **PASS** | Candidate test used volume `claimguard-phase1-test-data`. Production deployment proposes volume `claimguard-data`. They are **DIFFERENT** named volumes, ensuring complete isolation of test data from production staging storage. |
| 4 | Database Restart Test & Record Retention | **PASS** | Candidate container was stopped (`docker stop claimguard-phase1-test`) and restarted (`docker start claimguard-phase1-test`). Claim `CLM-2026-0007` (Claimant: Rajesh Mehra, status `UNDER_REVIEW`) and 2 review history records were successfully retrieved post-restart. Automated test `DatabasePersistenceTest` also verified persistence across two independent Spring application contexts. |
| 5 | Baseline Claims Idempotency (No Duplication) | **PASS** | `DataSeederService.java` (lines 24–26) enforces `if (claimRepository.count() > 0) return;`. Post-restart claim query returned exactly 7 claims (5 baseline + 2 test claims), confirming zero duplicated rows. |
| 6 | Credential Provisioning & Retrieval Security | **PASS** | `AdminBootstrapService.java` reads `CLAIMGUARD_ADMIN_PASSWORD` and `CLAIMGUARD_REVIEWER_PASSWORD` from environment variables. If omitted, generates a 16-byte cryptographically secure token logged once at `WARN` to container stdout. Password hashes are stored with BCrypt. Secrets are never hardcoded or exposed in reports. |
| 7 | Auth Endpoints, CSRF, Cookies, & Role Matrix | **PASS** | `SecurityConfig.java` and `SecurityAuthenticationTest` verify: login at `POST /api/auth/login`, logout at `POST /api/auth/logout`, `JSESSIONID` cookie session management, `CookieCsrfTokenRepository` (`XSRF-TOKEN` cookie + `X-XSRF-TOKEN` header requirement for mutating requests), role enforcement (`ADMIN` on `/api/admin/**`, `ADMIN`/`REVIEWER` on `/api/claims/**`), and unauthenticated 401 JSON / 302 page redirect behavior. |
| 8 | Untouched Primary Service on Port 8083 | **PASS** | Container `claimguard-staging` (ID `910771a20872`) has been running continuously (`Up > 56 minutes`). Health check `http://localhost:8083/api/health` responded with 200 OK (`H2 In-Memory (Operational)`). UI on `http://localhost:8083/index.html` responded with 200 OK. |
| 9 | Maven Surefire Test Report Audit | **PASS** | Test counts verified directly from `.txt` files in `target/surefire-reports`: 28/28 non-Selenium tests passed with 0 failures and 0 errors (`ClaimGuardApplicationTests`: 1, `ClaimControllerTest`: 7, `DatabasePersistenceTest`: 1, `IndicatorRuleEngineTest`: 4, `SecurityAuthenticationTest`: 9, `ClaimServiceTest`: 6). In addition, `ClaimGuardSeleniumE2ETest` has 8 passing browser tests. |
| 10 | Rollback Procedure Integrity & Constraints | **PARTIAL** | Renaming `claimguard-staging` to `claimguard-staging-prephase1` preserves its port mapping (`8083:8080`) and configuration layers. However, because the current container uses in-memory H2, stopping it discards uncommitted RAM state, restarting to the 5 baseline claims. |
| 11 | Live H2 Backup Safety Review (`docker cp`) | **PARTIAL** | Proposed `docker cp` of a live `claimguard.mv.db` is unsafe during active writes due to potential torn pages. A safer atomic SQL backup (`BACKUP TO`) or quiesced container procedure (`docker stop` -> `docker cp` -> `docker start`) must be used instead. |

---

## Detailed Technical Verification Findings

### 1. Candidate Image and Container Verification
- **Image Inspection:** Command `docker image inspect claimguard:phase1-candidate` confirms:
  - Image ID: `sha256:2fd25b07fba5787db431f3b2f10edaf8594d2fb5536580b92029ef7d58497a97`
  - Created: `2026-10-25T13:42:23.767901851Z`
  - Base: `eclipse-temurin:21-jre-alpine`
  - Non-Root User: `claimguard:claimguard` (UID 1001, GID 1001)
  - Workdir: `/app`
  - Environment: `CLAIMGUARD_DB_PATH=/app/data/claimguard`
- **Candidate Execution:** The isolated candidate test container (`claimguard-phase1-test`, container ID `c6fac7121b16`) was created directly from `claimguard:phase1-candidate` on port 8085 and ran without error.

---

### 2. Database Persistence & Storage Architecture
- **Configuration File:** `src/main/resources/application.properties`
  - Connection URL: `jdbc:h2:file:${CLAIMGUARD_DB_PATH:./data/claimguard};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`
  - Hibernate Dialect: `org.hibernate.dialect.H2Dialect`
  - DDL Auto: `update` (preserves existing schema and data on restart)
  - Console: Disabled (`spring.h2.console.enabled=false`)
- **Volume Mount Verification:**
  - Test container volume: `claimguard-phase1-test-data` mounted at `/app/data`.
  - Database file verified: `/app/data/claimguard.mv.db` (size: 49,152 bytes).
  - Storage location: Stored in Docker-managed WSL 2 filesystem (`/var/lib/docker/volumes/claimguard-phase1-test-data/_data`).
- **Volume Distinction (Requirement 3):**
  - **Test Volume:** `claimguard-phase1-test-data`
  - **Production Volume:** `claimguard-data`
  - **Assessment:** The test volume and production volume are **DIFFERENT**. This guarantees that test artifacts (e.g. `CLM-2026-0007`) never contaminate the clean production staging storage.

---

### 3. Restart and Idempotency Verification
- **Container Lifecycle Test:**
  1. Test claim `CLM-2026-0007` created via authenticated API with status `SUBMITTED`.
  2. Transitioned to `UNDER_REVIEW` via `PATCH /api/claims/7/review`.
  3. Container stopped via `docker stop claimguard-phase1-test`.
  4. Container started via `docker start claimguard-phase1-test`.
  5. Post-restart API query confirmed:
     - Total claims count: 7 (5 baseline + 2 test claims).
     - Claim `CLM-2026-0007` status: `UNDER_REVIEW`.
     - Review history: 2 entries (`SUBMITTED` by System Intake, `REVIEW_STARTED` by Priya Sharma).
- **Code Inspection:** `src/main/java/com/claimguard/dashboard/service/DataSeederService.java`
  ```java
  @Override
  public void run(String... args) {
      if (claimRepository.count() > 0) {
          return;
      }
      // ... baseline seeding logic
  }
  ```
  The seed runner explicitly aborts if any claims exist in the repository, guaranteeing zero row duplication on restart.

---

### 4. Authentication, Roles, and Credential Provisioning
- **Credential Provisioning:** Handled by `src/main/java/com/claimguard/dashboard/security/AdminBootstrapService.java`.
  - Injected via environment variables: `CLAIMGUARD_ADMIN_PASSWORD` and `CLAIMGUARD_REVIEWER_PASSWORD`.
  - Fallback: Cryptographically generated 16-byte random token (`SecureRandom`) encoded in URL-safe Base64.
  - Password Hashing: BCrypt adaptive password encoder with standard work factor 10.
- **Credential Retrieval Analysis:**
  - If random bootstrap passwords are used, they are emitted to standard output at `WARN` level during container boot (`[SECURITY BOOTSTRAP] Initial Administrator account created...`).
  - Administrators can retrieve them via `docker logs <container_name>`.
  - **Security Assessment:** While accessible via Docker daemon logs, logging passwords to stdout risks exposure in shared log collectors. Therefore, production deployments **must** supply explicit values via environment variables (`-e CLAIMGUARD_ADMIN_PASSWORD=...`) or a protected `.env` file, completely suppressing password emission in logs.
- **Authentication Endpoints & Session Behavior:**
  - Login: `POST /api/auth/login` (Content-Type: `application/x-www-form-urlencoded`, returns JSON `{"status": "authenticated", ...}`).
  - Logout: `POST /api/auth/logout` (Invalidates session, removes `JSESSIONID` and `XSRF-TOKEN`).
  - Session Cookie: `JSESSIONID` with HTTP-only flag.
  - CSRF Protection: `CookieCsrfTokenRepository.withHttpOnlyFalse()` sets cookie `XSRF-TOKEN`. Mutating requests (`POST`, `PUT`, `PATCH`, `DELETE`) require `X-XSRF-TOKEN` HTTP header.
  - Unauthorized Handling:
    - API requests (`/api/**`): Return HTTP 401 Unauthorized JSON payload.
    - HTML page requests (`/index.html`, `/claims.html`, etc.): Redirect via HTTP 302 to `/login.html?redirect=...`.

---

### 5. Status of Primary Staging Service (Port 8083)
Command `docker ps --filter "name=claimguard-staging"` and curl verification confirm:
- **Container Name:** `claimguard-staging`
- **Container ID:** `910771a20872`
- **Image:** `claimguard:stitch-ui-staging`
- **Port Mapping:** `0.0.0.0:8083->8080/tcp`
- **Runtime Status:** `Up > 56 minutes` (uninterrupted)
- **Health Response (`/api/health`):**
  ```json
  {"database":"H2 In-Memory (Operational)","application":"ClaimGuard Fraud Review Dashboard","version":"1.0.0-SNAPSHOT","status":"UP"}
  ```
- **Page Response (`/index.html`):** HTTP 200 OK.
- **Finding:** The primary staging application was completely untouched throughout development and testing.

---

### 6. Surefire Test Report Audit
Test counts audited directly from `target/surefire-reports/*.txt`:

| Report File | Tests Run | Failures | Errors | Skipped | Status |
|---|---|---|---|---|---|
| `com.claimguard.dashboard.ClaimGuardApplicationTests.txt` | 1 | 0 | 0 | 0 | **PASS** |
| `com.claimguard.dashboard.controller.ClaimControllerTest.txt` | 7 | 0 | 0 | 0 | **PASS** |
| `com.claimguard.dashboard.persistence.DatabasePersistenceTest.txt` | 1 | 0 | 0 | 0 | **PASS** |
| `com.claimguard.dashboard.rules.IndicatorRuleEngineTest.txt` | 4 | 0 | 0 | 0 | **PASS** |
| `com.claimguard.dashboard.security.SecurityAuthenticationTest.txt` | 9 | 0 | 0 | 0 | **PASS** |
| `com.claimguard.dashboard.service.ClaimServiceTest.txt` | 6 | 0 | 0 | 0 | **PASS** |
| **Total Non-Selenium Suite** | **28** | **0** | **0** | **0** | **100% PASS** |
| `com.claimguard.dashboard.selenium.ClaimGuardSeleniumE2ETest.txt` | 8 | 0 | 0 | 0 | **PASS** |

---

### 7. Rollback Procedure Analysis (Requirement 10)
The proposed rollback procedure in the implementation report:
```powershell
docker stop claimguard-staging
docker rm claimguard-staging
docker rename claimguard-staging-prephase1 claimguard-staging
docker start claimguard-staging
```

**Technical Evaluation:**
1. **Port Mapping & Configuration Retention:** Renaming the container preserves its exact network port bindings (`8083:8080`), image layer references, environment variables, and restart policies.
2. **Data Retention Constraint (Identified Risk):**
   - The current running container `claimguard-staging` is configured with `jdbc:h2:mem:claimguard`.
   - In-memory databases exist strictly in process RAM.
   - When `claimguard-staging` is stopped (`docker stop`), its JVM process terminates, and any synthetic claims or review decisions created dynamically during the current 56-minute container run will be lost.
   - When restarted during rollback, the container will re-initialize fresh from `DataSeederService` with the 5 baseline claims.
   - **Conclusion:** Rollback will successfully restore the working application and port mapping, but cannot restore transient in-memory claims created prior to the stop command.

---

### 8. Live Database Backup Analysis (Requirement 11)
The proposed backup procedure in the implementation report suggested:
```powershell
docker cp claimguard-staging:/app/data/claimguard.mv.db "$backupDir\claimguard.mv.db"
```

**Technical Risk Assessment:**
- **Issue:** H2 version 2.x uses the MVStore engine. While the application is running, background flushes, uncommitted transactions, and write locks can be active. Copying the raw `.mv.db` file while open in write mode can result in a **torn page** or incomplete transaction log snapshot, producing a corrupted backup file.
- **Recommended Safer Backup Methods:**
  1. **Quiesced Offline Copy (Safest for File-Level):**
     Stop the container briefly before copying, ensuring H2 closes all file locks and flushes its MVStore cache:
     ```powershell
     docker stop claimguard-staging
     docker cp claimguard-staging:/app/data/claimguard.mv.db "$backupDir\claimguard.mv.db"
     docker start claimguard-staging
     ```
  2. **Online SQL Backup (`BACKUP TO`):**
     Execute H2's atomic backup SQL command via an administrative query or script:
     ```sql
     BACKUP TO '/app/data/backup-claimguard.zip';
     ```
     This acquires an internal database read-lock, flushes unwritten cache blocks, and generates a consistent, compressed `.zip` archive while the application remains online.

---

## Final Pre-Deployment Recommendation

### Recommendation: **CONDITIONAL GO**

The Phase 1 candidate `claimguard:phase1-candidate` has passed all technical verification tests and is fully validated in isolation.

**Pre-Requisites Before Executing Cutover to Port 8083:**
1. **Supply Explicit Credentials:** Launch the container with explicit environment variables for `CLAIMGUARD_ADMIN_PASSWORD` and `CLAIMGUARD_REVIEWER_PASSWORD` to avoid emitting randomly generated passwords to container logs.
2. **Acknowledge In-Memory Reset:** Ensure team members understand that stopping the current staging container resets any transient claims created in memory since its last restart back to the 5 baseline seeded records.
3. **Use Dedicated Production Volume:** Confirm that the production container mounts named volume `claimguard-data:/app/data` (leaving `claimguard-phase1-test-data` isolated).

### Cutover Execution Command (Awaiting Owner Approval)
```powershell
# 1. Stop and preserve existing container for instant rollback
docker stop claimguard-staging
docker rename claimguard-staging claimguard-staging-prephase1

# 2. Launch candidate container with persistent production volume
docker run -d `
  --name claimguard-staging `
  -p 8083:8080 `
  -v claimguard-data:/app/data `
  -e CLAIMGUARD_ADMIN_USERNAME=admin `
  -e CLAIMGUARD_ADMIN_PASSWORD="<YourSecureAdminPassword>" `
  -e CLAIMGUARD_REVIEWER_USERNAME=priya.sharma `
  -e CLAIMGUARD_REVIEWER_PASSWORD="<YourSecureReviewerPassword>" `
  --restart unless-stopped `
  claimguard:phase1-candidate
```
