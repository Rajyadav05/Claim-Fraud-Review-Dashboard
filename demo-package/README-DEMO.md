# ClaimGuard — Portable College Demonstration Package

> **Stand-Alone Offline Student Evaluation & Presentation Guide**  
> **Candidate Release:** `claimguard:phase1-reconciled`  
> **Verified Image Digest:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`

---

## 1. Overview & Architecture

ClaimGuard is a production-conscious insurance **Claim Fraud Review Dashboard** built as a college DevOps assignment. It features:
- **Heuristic Fraud Engine:** Evaluates 5 automated risk indicators (catalog price anomalies, suspicious submission delays, high claimed amounts, frequent submissions).
- **Forensic Adjudication Console:** Allows trained reviewers to examine claim files, inspect flagged heuristics, record structured approval/rejection decisions, and enforce immutable review history.
- **Role-Based Access Control (RBAC):** Spring Security form authentication with BCrypt hashing and session-level CSRF token enforcement.
- **File-Backed H2 Persistence:** Durable database engine (`/app/data/claimguard.mv.db`) mapped to a persistent Docker volume, surviving all container restarts.
- **DevOps Lifecycle:** Continuous integration via Jenkins, containerization with Docker, and automated configuration management using Ansible.

This demonstration package is **entirely self-contained**. It runs locally on any Windows laptop without connecting to the author's computer, external cloud registries, or remote build servers.

---

## 2. Hardware & Software Prerequisites (Target Laptop)

| Requirement | Specification / Recommendation | Purpose |
| :--- | :--- | :--- |
| **Operating System** | Windows 10 (Build 19044+) or Windows 11 (64-bit x86_64) | Presentation host |
| **Docker Engine** | Docker Desktop 4.x+ with WSL 2 Linux engine active | Container runtime |
| **Shell** | Windows PowerShell 5.1+ or PowerShell 7 | Script runner |
| **Browser** | Google Chrome, Microsoft Edge, or Firefox | Presentation UI |
| **Network** | **Zero Internet Required** (Operates completely offline) | Presentation independence |

---

## 3. Quick Start: Transfer & Setup (2 Minutes)

### Step 1: Copy Package Files
Copy the `demo-package/` folder and the exported image archive `claimguard-phase1-reconciled.tar` onto the target laptop (e.g., via USB drive).

### Step 2: Open PowerShell as Standard User
Open PowerShell in the `demo-package/` directory:
```powershell
cd demo-package
```

### Step 3: Load the Verified Docker Image
Execute the automated importer to load the offline image and verify its cryptographic SHA-256 identity:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\import-image.ps1
```
*Expected Output: Confirms image `claimguard:phase1-reconciled` with digest `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`.*

---

## 4. Running the Demonstration

### Step 1: Launch the Demo Container
Run the automated launcher:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-demo.ps1
```

The script performs automated pre-flight checks:
1. Confirms Docker Engine is active.
2. Validates candidate image identity.
3. Checks that host port `8095` is available (avoids default port conflicts on 8080/8090).
4. Creates the dedicated persistent volume `claimguard-demo-data`.
5. Starts container `claimguard-demo-app`.
6. Polls `/api/health` until the file-backed operational state is confirmed.

*(Optional: To run on an alternate port, pass `-Port <number>`, e.g., `.\scripts\start-demo.ps1 -Port 8096`)*

### Step 2: Presenter Credentials

| Role | Username | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Fraud Reviewer** | `priya.sharma` | `DemoReviewerSecret2026!` | Adjudicating claims, examining evidence, approving/rejecting |
| **Administrator** | `admin` | `DemoAdminSecret2026!` | System administration, diagnostics, data reconciliation |

---

## 5. Live Presentation Walkthrough Script

### Scene 1: Authentication & Security Guardrails
1. Open your browser to: **`http://localhost:8095/`**
2. Notice that unauthenticated requests are automatically intercepted by Spring Security and redirected to `http://localhost:8095/login.html`.
3. Log in using the Reviewer account:
   - **Username:** `priya.sharma`
   - **Password:** `DemoReviewerSecret2026!`
4. Explain that passwords use salted BCrypt hashing and all state-changing forms enforce hidden `X-XSRF-TOKEN` CSRF tokens.

### Scene 2: Operations Executive Dashboard
1. Arrive at the Executive Overview (`/index.html`).
2. Highlight the real-time operational metric cards:
   - Total Claims (5 baseline claims seeded).
   - Under Review, Approved, and Flagged High-Risk distributions.
3. Show the suspicious indicator distribution breakdown.

### Scene 3: All Claims Directory & Forensic Filtering
1. Navigate to **All Claims** (`/claims.html`).
2. Demonstrate real-time searching and filtering by claim reference, policy number, or status (`UNDER_REVIEW`, `SUBMITTED`, `APPROVED`, `REJECTED`, `MORE_INFO_REQUIRED`).
3. Select claim **`CLM-2026-0001`** (Rajesh Kumar, ₹185,000.00) and click **"Review Claim"**.

### Scene 4: Forensic Adjudication Console (`/review.html?id=1`)
1. Show the deterministic heuristic rules evaluated by `IndicatorRuleEngine.java`:
   - *HIGH_VALUE_CLAIM*: Exceeds ₹150,000 threshold.
   - *DOCUMENTATION_DELAY*: Incident date vs submission date anomaly.
2. Show the immutable review audit history:
   - Initial `SUBMITTED` event by System Intake.
   - `REVIEW_STARTED` transition to `UNDER_REVIEW` assigned to Priya Sharma.
3. Demonstrate a live decision:
   - Select **"Approve Claim"** or **"Request More Info"**.
   - Input mandatory adjudicator remarks: *"Catalog prices cross-verified with authorized repairer; authorized for settlement."*
   - Submit the review and observe the instant status change and new immutable entry added to the audit trail.

### Scene 5: Claim Intake Portal (`/submit.html`)
1. Navigate to **Submit Claim** (`/submit.html`).
2. Input a new synthetic motor claim:
   - Policy: `POL-COLLEGE-DEMO-01`
   - Claimant: `Siddharth Malhotra`
   - Amount: `₹95,000.00`
3. Submit and watch the system automatically assign the next sequential business reference (e.g., `CLM-2026-0006`).

### Scene 6: File-Backed Persistence Demonstration
1. Open a new tab to: **`http://localhost:8095/api/health`**
2. Highlight the JSON response proving file-backed operational mode:
   ```json
   {
     "status": "UP",
     "database": "H2 File-Backed (Operational)",
     "application": "ClaimGuard Fraud Review Dashboard",
     "version": "1.0.0-SNAPSHOT"
   }
   ```
3. Run the automated persistence checker in PowerShell:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\check-demo.ps1 -TestPersistence
   ```
4. This reboots the Docker container and verifies that all newly created claims and audit entries remain 100% intact on the persistent volume `claimguard-demo-data`.

---

## 6. DevOps Architecture Demonstration

### A. Docker Container Lifecycle
- **Dockerfile Inspection:** Review the multi-stage build using `eclipse-temurin:21-jre-alpine` and unprivileged execution (`claimguard` user, UID 1001).
- **Container Isolation:** Demonstrates dedicated container `claimguard-demo-app` on port 8095 isolated from all host services.

### B. Jenkins Continuous Integration & Testing
- **Production Pipeline (`Jenkinsfile`):** 7 automated stages:
  1. `Initialize`: Workspace and build metadata setup.
  2. `Verify Environment`: Validates Java 21 JDK, Docker engine, and Chrome.
  3. `Run Unit & Integration Tests`: Executes 57 automated tests via Maven.
  4. `Package Application`: Packages executable Spring Boot JAR with fingerprinting.
  5. `Run Selenium E2E Tests`: Headless browser testing.
  6. `Build Docker Image`: Packages candidate container image.
  7. `Deploy Staging Container`: Staging deployment gate.
- **Educational Demo Pipeline (`Jenkinsfile.demo`):**
  - Designed specifically for student demonstration.
  - Safely runs stages 1 through 6 (build, test, package, docker build) while **bypassing the live staging deployment stage** to protect existing environments.

### C. Ansible Configuration Management
- **Declarative Provisioning:** Inspect [`ansible/playbooks/deploy-claimguard.yml`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible/playbooks/deploy-claimguard.yml).
- **Port Protection Guardrails:** Explains how Ansible enforces assertions protecting reserved ports `8080`, `8081`, `8082`, and `8083`.
- **Syntax Check Demo:** Execute safe static validation via WSL:
  ```powershell
  powershell -ExecutionPolicy Bypass -File ..\scripts\run-ansible-playbook.ps1 -SyntaxCheck
  ```

---

## 7. Teardown & Reset Commands

### Stopping the Demo Application
To stop the demo container while preserving database state:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-demo.ps1
```

### Full Reset (Clearing Storage)
To stop the container and completely reset the database back to clean baseline:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-demo.ps1 -CleanStorage
```

---

## 8. Troubleshooting Cheatsheet

| Issue | Cause | Solution |
| :--- | :--- | :--- |
| `docker command not found` | Docker Desktop is not installed or not in PATH | Install Docker Desktop and enable WSL 2 integration |
| `Docker engine is not running` | Docker service stopped | Start Docker Desktop from Windows Start Menu |
| `Port 8095 is currently occupied` | Another process is using port 8095 | Launch with a different port: `.\scripts\start-demo.ps1 -Port 8096` |
| `Image not found locally` | Image archive not loaded yet | Run `.\scripts\import-image.ps1` |
| `Login rejected credentials` | Custom password in `.env` | Inspect `.env` or use credentials printed in `docker logs claimguard-demo-app` |
