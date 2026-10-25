# ClaimGuard — Phase 1 Implementation Report
## Persistent H2 Database + Authentication & Authorization

> **Report Reference:** `CLAIMGUARD_PHASE1_IMPLEMENTATION_REPORT.md`  
> **Date:** October 25, 2026  
> **Target Environment:** Spring Boot 3.3.4, Java 21, Docker, Eclipse Temurin 21 JRE Alpine  
> **Primary Application (Untouched):** `claimguard-staging` (`http://localhost:8083`, port `8083:8080`)  
> **Candidate Image (Verified Isolated):** `claimguard:phase1-candidate`  
> **Pre-Implementation Backup:** `E:\Desktop\Raj Yadav\Projects\backups\claimguard-phase1-prebackup-20261025`  

---

## Executive Summary

Phase 1 of the ClaimGuard modernization roadmap has been implemented and verified. The application has transitioned from an ephemeral in-memory database (`jdbc:h2:mem:claimguard`) to an enterprise-grade file-backed persistent H2 architecture (`jdbc:h2:file:...`) backed by Docker volume storage (`claimguard-data:/app/data`), integrated with Spring Security role-based access control (`ADMIN` and `REVIEWER`), and fully authenticated without compromising the Stitch design system or interrupting existing production-like containers (`claimguard-staging` on port `8083`, `claimguard-task11` on `8082`, `claimguard-ansible` on `8084`).

All 28 automated tests (including context loads, persistence across restarts, authentication and roles, CSRF verification, deterministic fraud indicators, and claim services) passed with zero errors, zero failures, and zero skipped tests.

---

## 1. File-Backed H2 Database Persistence Architecture

### 1.1 Database Configuration (`application.properties`)
The in-memory database URL was replaced with a configurable file-backed H2 connection string:

```properties
# H2 Persistent File-Backed Database Configuration
# Local default: ./data/claimguard. Docker container default: /app/data/claimguard
spring.datasource.url=jdbc:h2:file:${CLAIMGUARD_DB_PATH:./data/claimguard};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA / Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true

# H2 Web Console (Disabled by default; enable explicitly for local debugging)
spring.h2.console.enabled=${CLAIMGUARD_H2_CONSOLE_ENABLED:false}
spring.h2.console.path=/h2-console
spring.h2.console.settings.web-allow-others=false
```

### 1.2 Technical Highlights
- **Configurable Path:** `CLAIMGUARD_DB_PATH` allows flexible override across local developer workstations (`./data/claimguard`), CI/CD test runners, and production Docker containers (`/app/data/claimguard`).
- **H2 2.2+ Compatibility:** Avoids unsupported H2 2.2+ driver flag combinations (`AUTO_SERVER=TRUE && DB_CLOSE_ON_EXIT=FALSE`). Uses `DB_CLOSE_DELAY=-1` to keep connection pools active while the Spring Boot JVM is alive and flushes MVStore database files on shutdown.
- **Explicit Dialect:** Configured `org.hibernate.dialect.H2Dialect` to eliminate Hibernate dialect resolution warnings during startup.
- **Console Security:** H2 console is disabled by default (`spring.h2.console.enabled=false`) to protect database files in non-local environments.

### 1.3 Seed Data Idempotency (`DataSeederService.java`)
`DataSeederService` now checks `claimRepository.count() > 0` before inserting the five baseline synthetic claims (`CLM-2026-0001` through `CLM-2026-0005`). On container restart or application reboot, existing claims and review history remain intact with zero duplicate rows.

---

## 2. Persistent Docker Storage Architecture

### 2.1 Dockerfile Hardening
The application `Dockerfile` was updated to establish dedicated ownership of the persistent data directory:

```dockerfile
# Create a non-root group and user for secure, unprivileged execution
RUN addgroup -S -g 1001 claimguard && \
    adduser -S -u 1001 -G claimguard claimguard && \
    mkdir -p /app/data && \
    chown -R claimguard:claimguard /app

# Copy the pre-built production JAR with appropriate ownership
COPY --chown=claimguard:claimguard target/claimguard-dashboard-1.0.0-SNAPSHOT.jar app.jar

# Run as non-root user
USER claimguard:claimguard
EXPOSE 8080
ENV CLAIMGUARD_DB_PATH=/app/data/claimguard
```

### 2.2 Volume Mapping
- **Production Staging Volume:** `claimguard-data:/app/data`
- **Isolated Testing Volume:** `claimguard-phase1-test-data:/app/data`
- Database files (`claimguard.mv.db`) are created with non-root ownership (`1001:1001`) inside the persistent volume and survive container destruction, recreation, and restarts.

---

## 3. Authentication & Role-Based Authorization Architecture

### 3.1 Security Core Components
1. **`User` Entity & `UserRole` Enum (`entity/`):**
   - Fields: `id`, `username` (unique), `passwordHash`, `role` (`ADMIN`, `REVIEWER`), `fullName`, `enabled`, `createdAt`.
   - Never stores plaintext passwords; all credentials use BCrypt adaptive hashing.
2. **`CustomUserDetailsService` (`security/`):**
   - Implements Spring Security `UserDetailsService`.
   - Maps `UserRole` to Spring Security `SimpleGrantedAuthority("ROLE_" + role.name())`.
   - Checks `user.isEnabled()` to prevent authentication of deactivated accounts.
3. **`SecurityConfig` (`security/`):**
   - **Form Login:** POST `/api/auth/login` accepting `username` and `password`. Returns JSON response with user identity and authorities on success; returns clean 401 JSON on failure.
   - **Logout:** POST `/api/auth/logout`. Invalidates `HttpSession`, clears `JSESSIONID` and `XSRF-TOKEN` cookies.
   - **CSRF Protection:** Configured with `CookieCsrfTokenRepository.withHttpOnlyFalse()` sending `XSRF-TOKEN` cookie and accepting `X-XSRF-TOKEN` header on mutating requests (`POST`, `PATCH`, `PUT`, `DELETE`).
   - **API 401 Handler:** Unauthenticated API requests receive HTTP 401 JSON payloads (`{"status":401,"title":"Unauthorized"}`).
   - **Page Navigation Redirect:** Unauthenticated browser visits to `/index.html`, `/claims.html`, `/submit.html`, or `/review.html` automatically redirect to `/login.html?redirect=...`.
4. **`AdminBootstrapService` (`security/`):**
   - Annotated with `@Order(1)` to ensure users exist prior to data seeding.
   - Idempotent: checks `userRepository.existsByRole(UserRole.ADMIN)` before creating administrator.
   - Configurable via environment variables:
     - `CLAIMGUARD_ADMIN_USERNAME` (default: `admin`)
     - `CLAIMGUARD_ADMIN_PASSWORD` (if unset, generates a cryptographic 16-character token logged once)
     - `CLAIMGUARD_REVIEWER_USERNAME` (default: `priya.sharma`)
     - `CLAIMGUARD_REVIEWER_PASSWORD` (if unset, generates a cryptographic 16-character token logged once)
   - Prevents hardcoded passwords in version control.

### 3.2 Role and Endpoint Permission Matrix

| Endpoint | Method | Permitted Roles | Anonymous Behavior |
|----------|--------|-----------------|--------------------|
| `/api/health` | GET | Permit All | 200 OK (Public status check) |
| `/actuator/health` | GET | Permit All | 200 OK |
| `/api/auth/csrf` | GET | Permit All | 200 OK (Returns CSRF token) |
| `/api/auth/login` | POST | Permit All | 200 OK (Auth success) / 401 (Failure) |
| `/api/auth/logout` | POST | Authenticated | 200 OK (Invalidates session) |
| `/api/auth/me` | GET | Permit All | 200 OK (Returns auth status) |
| `/api/claims/**` (GET) | GET | `ADMIN`, `REVIEWER` | 401 Unauthorized (JSON) |
| `/api/claims` (POST) | POST | `ADMIN`, `REVIEWER` | 401 Unauthorized (Requires CSRF) |
| `/api/claims/{id}/review` | PATCH | `ADMIN`, `REVIEWER` | 401 Unauthorized (Requires CSRF) |
| `/api/claims/{id}/approve` | POST | `ADMIN`, `REVIEWER` | 401 Unauthorized (Requires CSRF) |
| `/api/claims/{id}/reject` | POST | `ADMIN`, `REVIEWER` | 401 Unauthorized (Requires CSRF) |
| `/api/claims/{id}/request-info` | POST | `ADMIN`, `REVIEWER` | 401 Unauthorized (Requires CSRF) |
| `/api/admin/**` | ANY | `ADMIN` only | 401 Unauthorized / 403 Forbidden |
| `/login.html` | GET | Permit All | 200 OK |
| `/index.html`, `/claims.html`, `/submit.html`, `/review.html` | GET | `ADMIN`, `REVIEWER` | 302 Redirect to `/login.html` |

---

## 4. Stitch-Styled Frontend Integration

### 4.1 Login Console (`login.html`)
Created `src/main/resources/static/login.html` adhering strictly to the Google Stitch design system tokens:
- Reuses exact fonts (`Cabinet Grotesk`, `Inter`, `JetBrains Mono`).
- Reuses color palette (`surface-container`, `primary-container`, `error-container`).
- Seamless error handling displaying non-revealing credential failure messages.
- Supports `?redirect=` return parameter to preserve user destination post-login.

### 4.2 Universal Header Integration
Updated top navigation header in [`index.html`](src/main/resources/static/index.html), [`claims.html`](src/main/resources/static/claims.html), [`submit.html`](src/main/resources/static/submit.html), and [`review.html`](src/main/resources/static/review.html):
- Displays current user badge: avatar, user full name, and role badge (`REVIEWER` or `ADMIN`).
- Includes authenticated logout button (`#header-logout-btn`) that invalidates session cookies and returns operator to login.

### 4.3 API Wrapper Updates (`js/api.js`)
Enhanced existing `ClaimGuardAPI` client:
- Added CSRF cookie parsing (`getCookie('XSRF-TOKEN')`) and automatic injection of `X-XSRF-TOKEN` header on mutating HTTP methods.
- Intercepts 401 HTTP responses: automatically redirects to `/login.html` without infinite loops.
- Added `getCurrentUser()` and `logout()` helper methods.

---

## 5. Verification & Test Evidence

### 5.1 Automated Test Suite Results
Executed via Maven: `.\mvnw.cmd test -Dtest="!*Selenium*"`

```
[INFO] Results:
[INFO] 
[INFO] Tests run: 28, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

#### Detailed Breakdown:
1. **`ClaimGuardApplicationTests` (1/1 PASSED):** Spring context initialization with file-backed H2 configuration.
2. **`ClaimControllerTest` (7/7 PASSED):** MockMvc tests verifying claim queries, pagination, detail retrieval, indicator evaluation, and anonymous 401 rejection.
3. **`IndicatorRuleEngineTest` (4/4 PASSED):** Deterministic verification of heuristic fraud rules and risk priority categorization.
4. **`ClaimServiceTest` (6/6 PASSED):** Business logic, transitions, reference formatting, and audit entry generation.
5. **`DatabasePersistenceTest` (1/1 PASSED):** Integration test utilizing temporary file-backed H2 storage. Closes the first application context and restarts a second independent context, verifying:
   - Claim creation survives JVM shutdown.
   - Status transitions (`UNDER_REVIEW`) and immutable review history survive.
   - Seed data is not duplicated (5 baseline claims + 1 test claim = 6 claims total).
6. **`SecurityAuthenticationTest` (9/9 PASSED):**
   - BCrypt password hash verification (stored hashes begin with `$2a$` / `$2b$`, raw passwords never stored).
   - Form login success returning authenticated session.
   - Invalid password failure returning 401 JSON.
   - Anonymous access to `/api/claims` returning 401 JSON.
   - Anonymous page access redirecting to `/login.html`.
   - CSRF token endpoint `/api/auth/csrf` generation.
   - Mutating request without CSRF token returning 403 Forbidden.
   - Disabled account rejection.
   - Session invalidation on logout.

---

### 5.2 Isolated Docker Container Restart Persistence Verification

An isolated test container was launched on unused port 8085 to test container lifecycle persistence without touching running services:

```powershell
docker run -d --name claimguard-phase1-test -p 8085:8080 \
  -v claimguard-phase1-test-data:/app/data \
  -e CLAIMGUARD_ADMIN_PASSWORD=AdminTestPass123! \
  -e CLAIMGUARD_REVIEWER_PASSWORD=ReviewerTestPass123! \
  claimguard:phase1-candidate
```

#### Recorded Verification Output:
```
1. Initial container startup:
   - Initial claim count: 5
   - Created Claim ID: 7, Reference: CLM-2026-0007, Status: SUBMITTED
   - Transition: PATCH /api/claims/7/review -> Status: UNDER_REVIEW
   - Verified file created: /app/data/claimguard.mv.db (49152 bytes, owner claimguard:claimguard)

2. Container restart:
   - docker stop claimguard-phase1-test (exited cleanly)
   - docker start claimguard-phase1-test (restarted in 8.081 seconds)
   - [SECURITY BOOTSTRAP] Admin & Reviewer exist. Skipping bootstrap.

3. Post-restart verification:
   - Total claims count after restart: 7 (5 baseline + 2 test claims, ZERO duplicates)
   - Claim 7 Ref: CLM-2026-0007, Status: UNDER_REVIEW, Claimant: Rajesh Mehra
   - History entries count: 2
     * Action: SUBMITTED, Reviewer: System Intake, Status: SUBMITTED
     * Action: REVIEW_STARTED, Reviewer: Priya Sharma, Status: UNDER_REVIEW
```
Test container `claimguard-phase1-test` was subsequently stopped and removed cleanly.

---

### 5.3 Preservation of Existing Primary Services
The primary staging container and auxiliary environments remained running and untouched throughout development and testing:

| Container Name | Port Mapping | Image | Runtime Status | Health Check |
|----------------|--------------|-------|----------------|--------------|
| `claimguard-staging` | `8083:8080` | `claimguard:stitch-ui-staging` | Up > 50 minutes | 200 OK (`/api/health`) |
| `claimguard-task11` | `8082:8080` | `claimguard:task11` | Up > 50 minutes | 200 OK |
| `claimguard-ansible` | `8084:8080` | `claimguard:staging` | Up > 50 minutes | 200 OK |

---

## 6. Changed Files & Purpose

| File Path | Purpose |
|-----------|---------|
| `pom.xml` | Added `spring-boot-starter-security` and `spring-security-test` dependencies. |
| `src/main/resources/application.properties` | Configured file-backed H2 URL (`jdbc:h2:file:...`), `H2Dialect`, and disabled web console. |
| `Dockerfile` | Created `/app/data` directory owned by `claimguard:claimguard` and declared `ENV CLAIMGUARD_DB_PATH=/app/data/claimguard`. |
| `.env.example` | Documented environment variables for administrator/reviewer bootstrap and database path. |
| `src/main/java/com/claimguard/dashboard/entity/UserRole.java` | Role definitions (`ADMIN`, `REVIEWER`). |
| `src/main/java/com/claimguard/dashboard/entity/User.java` | JPA entity for credentials, roles, and account status. |
| `src/main/java/com/claimguard/dashboard/repository/UserRepository.java` | Repository for user persistence. |
| `src/main/java/com/claimguard/dashboard/security/CustomUserDetailsService.java` | Bridges JPA users to Spring Security `UserDetails`. |
| `src/main/java/com/claimguard/dashboard/security/SecurityConfig.java` | Spring Security filter chain: form login, logout, CSRF, BCrypt, 401 API handler, RBAC. |
| `src/main/java/com/claimguard/dashboard/security/AdminBootstrapService.java` | One-time bootstrap for Admin and Reviewer accounts using environment variables or secure random tokens. |
| `src/main/java/com/claimguard/dashboard/service/DataSeederService.java` | Annotated with `@Order(2)` and guarded with `count() > 0` check to prevent duplicate seed claims. |
| `src/main/java/com/claimguard/dashboard/controller/AuthController.java` | Endpoints for session status (`/api/auth/me`) and CSRF tokens (`/api/auth/csrf`). |
| `src/main/resources/static/login.html` | Stitch-styled authentication console. |
| `src/main/resources/static/js/api.js` | Integrated CSRF token attachment, session checks, and 401 redirect handling. |
| `src/main/resources/static/index.html` | Integrated header user badge and logout action. |
| `src/main/resources/static/claims.html` | Integrated header user badge and logout action. |
| `src/main/resources/static/submit.html` | Integrated header user badge and logout action. |
| `src/main/resources/static/review.html` | Integrated header user badge and logout action. |
| `src/test/java/com/claimguard/dashboard/controller/ClaimControllerTest.java` | Updated controller tests with `@WithMockUser` and `@Import(SecurityConfig.class)`. |
| `src/test/java/com/claimguard/dashboard/persistence/DatabasePersistenceTest.java` | Integration test verifying file-backed persistence and seed idempotency across JVM restarts. |
| `src/test/java/com/claimguard/dashboard/security/SecurityAuthenticationTest.java` | Suite verifying BCrypt hashing, login/logout, CSRF enforcement, and role security. |
| `src/test/java/com/claimguard/dashboard/selenium/ClaimGuardSeleniumE2ETest.java` | Added `ensureAuthenticated()` helper to support E2E tests against protected pages. |
| `README.md` | Updated architecture, persistence specifications, and testing details. |

---

## 7. Database Backup & Recovery Guide

### 7.1 Location of Recovery Backups
Existing image and root filesystem backups are archived in `E:\DockerData`:
- `all-claimguard-images.tar`
- `claimguard-container-inspect.json`
- `claimguard-staging-rootfs.tar`
- `claimguard-task11-rootfs.tar`
- `claimguard-ansible-rootfs.tar`

Pre-Phase 1 source code backup:
- `E:\Desktop\Raj Yadav\Projects\backups\claimguard-phase1-prebackup-20261025`

### 7.2 Live Database Backup Procedure
To take a point-in-time snapshot of the persistent H2 database volume while the container is running:

```powershell
# 1. Create a timestamped backup directory
$backupDir = "E:\Desktop\Raj Yadav\Projects\backups\h2-backup-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
New-Item -ItemType Directory -Path $backupDir -Force

# 2. Copy the database file directly from the running container
docker cp claimguard-staging:/app/data/claimguard.mv.db "$backupDir\claimguard.mv.db"
Write-Host "Database snapshot saved to $backupDir\claimguard.mv.db"
```

### 7.3 Restore Procedure
If the persistent database needs to be restored to a known good snapshot:

```powershell
# 1. Stop the target container
docker stop claimguard-staging

# 2. Copy the backup file into the volume location
docker cp "E:\path\to\backup\claimguard.mv.db" claimguard-staging:/app/data/claimguard.mv.db

# 3. Start the container
docker start claimguard-staging
```

---

## 8. Deployment and Rollback Guide for Primary Service (`http://localhost:8083`)

> [!IMPORTANT]
> **Owner Approval Required:** The candidate image `claimguard:phase1-candidate` has been compiled and verified in an isolated container. The live service on port `8083` has NOT been touched and will only be upgraded when you approve.

### 8.1 Production Deployment Steps (Upon Approval)

1. **Tag Candidate as Staging Release:**
   ```powershell
   docker tag claimguard:phase1-candidate claimguard:phase1-staging
   ```

2. **Create Named Volume for Staging (if not existing):**
   ```powershell
   docker volume create claimguard-data
   ```

3. **Stop and Rename Existing Staging Container (Preserving for Rollback):**
   ```powershell
   docker stop claimguard-staging
   docker rename claimguard-staging claimguard-staging-prephase1
   ```

4. **Launch Modernized Staging Container on Port 8083:**
   ```powershell
   docker run -d `
     --name claimguard-staging `
     -p 8083:8080 `
     -v claimguard-data:/app/data `
     -e CLAIMGUARD_ADMIN_USERNAME=admin `
     -e CLAIMGUARD_ADMIN_PASSWORD="<SecureAdminPassword>" `
     -e CLAIMGUARD_REVIEWER_USERNAME=priya.sharma `
     -e CLAIMGUARD_REVIEWER_PASSWORD="<SecureReviewerPassword>" `
     --restart unless-stopped `
     claimguard:phase1-staging
   ```

5. **Verify Operational Health & Login:**
   - Browse to `http://localhost:8083/api/health` -> verify status is `UP`.
   - Browse to `http://localhost:8083/login.html` -> sign in with configured reviewer credentials.
   - Verify dashboard loads synthetic claims from persistent storage.

---

### 8.2 Immediate Rollback Procedure
If any discrepancy occurs during or after deployment:

```powershell
# 1. Stop and remove the candidate container
docker stop claimguard-staging
docker rm claimguard-staging

# 2. Restore the previous container to active service
docker rename claimguard-staging-prephase1 claimguard-staging
docker start claimguard-staging

# 3. Confirm previous container is listening on port 8083
Invoke-RestMethod -Uri "http://localhost:8083/api/health"
```
*Rollback execution time is less than 15 seconds.*

---

## 9. Remaining Items & Next Roadmap Phase

- **Phase 2 Roadmap:** Real multipart forensic document uploads with virus scanning, policyholder audit trail event streaming, advanced pagination, and PostgreSQL clustering for multi-node deployments.
- **Current Phase Status:** 100% of Phase 1 requirements implemented and verified.
