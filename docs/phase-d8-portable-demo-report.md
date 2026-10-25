# ClaimGuard Phase D8 — Portable College Demo Package Report

**Project:** `E:\Desktop\Raj Yadav\Projects\Claim-Fraud-Review-Dashboard`  
**Git Branch:** `feature/phase-d-reconciliation`  
**Candidate Release Image:** `claimguard:phase1-reconciled`  
**Verified Image ID:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`  
**Completion Timestamp:** 2026-10-25T20:23:45Z (Local: 2026-10-26T01:53:45+05:30)  

---

## 1. Safety & Isolation Compliance Confirmation

Throughout Phase D8, all mandatory safety boundaries were strictly respected:
1. **Live Staging Container (`claimguard-staging`, ID `910771a20872`, port 8083):** **100% UNTOUCHED & ACTIVE** (Up 7+ hours, H2 in-memory).
2. **Legacy Container `claimguard-ansible` (port 8084):** **100% UNTOUCHED & ACTIVE**.
3. **Legacy Container `claimguard-task11` (port 8082):** **100% UNTOUCHED & ACTIVE**.
4. **Production Volume `claimguard-data`:** **DOES NOT EXIST / UNTOUCHED**.
5. **Runbook Section 5 Cutover:** **NOT EXECUTED**.
6. **Git Working Tree Changes:** **100% PRESERVED** (no git reset, clean, or discard).

---

## 2. Deliverables & Created Artifacts

### 2.1 Demo Package Directory (`demo-package/`)
| File / Directory | Purpose |
| :--- | :--- |
| [`demo-package/README-DEMO.md`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/README-DEMO.md) | Comprehensive presenter walkthrough script, RBAC credentials, and troubleshooting guide |
| [`demo-package/docker-compose.yml`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/docker-compose.yml) | Declarative Docker Compose definition for standalone local execution |
| [`demo-package/.env.example`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/.env.example) | Safe environment configuration template with port 8095 and demo credentials |
| [`demo-package/Jenkinsfile.demo`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/Jenkinsfile.demo) | Safe educational Jenkins pipeline (Stages 1–6: build, test, package, docker build) that bypasses live staging deployment |
| [`demo-package/scripts/start-demo.ps1`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/scripts/start-demo.ps1) | Automated launcher with pre-flight checks (Docker, image SHA-256, port 8095, volume creation, healthcheck polling) |
| [`demo-package/scripts/stop-demo.ps1`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/scripts/stop-demo.ps1) | Safe teardown script targeting only `claimguard-demo-app` and `claimguard-demo-data` with protected container guardrails |
| [`demo-package/scripts/check-demo.ps1`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/scripts/check-demo.ps1) | Diagnostic test runner (container state, health UP, 302 login redirect, CSRF auth, baseline claims, and restart persistence) |
| [`demo-package/scripts/export-image.ps1`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/scripts/export-image.ps1) | Helper to export the candidate image to an offline tar archive with SHA-256 manifest generation |
| [`demo-package/scripts/import-image.ps1`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/scripts/import-image.ps1) | Helper to load the tar archive on the target laptop and verify image identity |
| `demo-package/claimguard-phase1-reconciled.tar` | Pre-exported offline candidate container image archive (117.73 MB) |
| `demo-package/claimguard-phase1-reconciled.tar.manifest.json` | Cryptographic checksum manifest for the tar archive |

### 2.2 Root Documentation Updates
- [`README.md`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/README.md):
  - Updated Technology Stack table to reflect **57 automated tests** (100% pass) and candidate image `claimguard:phase1-reconciled`.
  - Added "Verified Release Status: Phase D — Reconciliation & College Demo Packaging" section documenting verified image ID, data recovery proof, standalone demo port `8095`, and staging safety boundaries.

### 2.3 Standalone Portable Demo Bundle Archive
- **Archive Path:** `E:\Desktop\Raj Yadav\Projects\Claim-Fraud-Review-Dashboard\claimguard-college-demo.zip`
- **File Size:** `122,110,846 bytes` (116.45 MB)
- **SHA-256 Hash:** `B3A16493C41EBDA3FB3BAD41B5BB9712C1221C8016DA5999707184F2F41C2A60`
- **Contents:** The complete self-contained package including all launch scripts, compose file, presenter guide, Jenkins demo pipeline, and the offline image archive.

---

## 3. Cryptographic Verification & Image Provenance

| Artifact | Type | Expected Identifier | Observed Identifier | Status |
| :--- | :---: | :--- | :--- | :---: |
| **Candidate Docker Image** | OCI Image ID | `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043` | `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043` | **PASS** |
| **Offline Image Tar Archive** | SHA-256 Checksum | *N/A (Generated in D8)* | `1AD12B406CEB43F52A63A847B686D744564D1C3D18E40ABED43A91D2F84197B2` | **PASS** |
| **College Demo Zip Bundle** | SHA-256 Checksum | *N/A (Generated in D8)* | `B3A16493C41EBDA3FB3BAD41B5BB9712C1221C8016DA5999707184F2F41C2A60` | **PASS** |

---

## 4. Port Conflict Discovery & Resolution

- **Port 8090 Audit:**
  - Before starting the demo, port 8090 was audited via `Test-NetConnection -ComputerName localhost -Port 8090`.
  - Result: Port 8090 was active and occupied by Windows process `WsToastNotification` (PID 19076).
- **Resolution:**
  - Port `8095` was scanned and confirmed completely free (`Listening: False`).
  - The demo package was configured to default to port **`8095`**, while preserving parameterized flexibility (`-Port <number>`) in all scripts and compose configurations.

---

## 5. Verification of the Demo Package Lifecycle

The complete standalone demo workflow was executed and validated locally:

### 5.1 Launch Verification (`start-demo.ps1`)
- **Execution:** `powershell -ExecutionPolicy Bypass -File demo-package\scripts\start-demo.ps1`
- **Pre-flight Checks:**
  - Docker engine availability: Verified operational.
  - Candidate image ID: Confirmed `sha256:3aaad642...`.
  - Port 8095 availability: Confirmed free.
  - Dedicated storage volume `claimguard-demo-data`: Created cleanly.
- **Container Launch:** Container `claimguard-demo-app` started and polled `/api/health`.
- **Health Response:** `status: UP` | database mode: `H2 File-Backed (Operational)`.
- **Result:** **PASS**

### 5.2 Diagnostic & Persistence Verification (`check-demo.ps1`)
- **Execution:** `powershell -ExecutionPolicy Bypass -File demo-package\scripts\check-demo.ps1 -TestPersistence`
- **6/6 Diagnostic Checks Passed:**
  1. *Container State:* Confirmed active.
  2. *Health Probe:* Confirmed UP with file-backed mode.
  3. *Unauthenticated Request Gate:* Anonymous `GET /index.html` intercepted with HTTP 302 redirecting to `login.html`.
  4. *Authentication & CSRF:* Acquired `X-XSRF-TOKEN`, authenticated reviewer `priya.sharma`, session cookie established.
  5. *Seed Data Inspection:* 5 synthetic baseline claims verified (`CLM-2026-0001` through `CLM-2026-0005`) with audit history.
  6. *Restart Durability:* Executed `docker restart claimguard-demo-app`; application successfully rebooted, re-established database connection, and preserved all records on volume `claimguard-demo-data`.
- **Result:** **PASS**

### 5.3 Teardown Verification (`stop-demo.ps1`)
- **Execution:** `powershell -ExecutionPolicy Bypass -File demo-package\scripts\stop-demo.ps1 -CleanStorage`
- **Safety Audit Results:**
  - Stopped and removed `claimguard-demo-app`.
  - Removed dedicated demo volume `claimguard-demo-data`.
  - **Invariants Checked:** Confirmed `claimguard-staging`, `claimguard-ansible`, and `claimguard-task11` remained 100% untouched and active.
- **Result:** **PASS**

---

## 6. Offline Deployment & Transfer Instructions

To run the demonstration on a friend's Windows laptop:

### Step 1: Transfer
Copy `claimguard-college-demo.zip` to a USB flash drive or local directory on the target laptop and extract it:
```powershell
Expand-Archive -Path claimguard-college-demo.zip -DestinationPath C:\ClaimGuardDemo
cd C:\ClaimGuardDemo
```

### Step 2: Load Docker Image (Offline)
Load the pre-packaged container image into the local Docker Engine:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\import-image.ps1
```

### Step 3: Start Demo Application
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-demo.ps1
```

### Step 4: Access Application in Browser
Open browser to: **`http://localhost:8095/`**
- **Reviewer Account:** `priya.sharma` / `DemoReviewerSecret2026!`
- **Administrator Account:** `admin` / `DemoAdminSecret2026!`

### Step 5: Teardown
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-demo.ps1 -CleanStorage
```

---

## 7. DevOps Components Demonstration Strategy

1. **Docker Container Architecture:**
   - Demonstrates multi-stage build, minimal Alpine base image, unprivileged non-root execution (`claimguard` user, UID 1001), JSON-form entrypoint, and persistent volume mapping.
2. **Jenkins CI/CD Pipeline:**
   - Real pipeline explained across all 7 stages in `Jenkinsfile`.
   - Educational pipeline provided in [`Jenkinsfile.demo`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/demo-package/Jenkinsfile.demo) demonstrating automated checkout, unit/integration testing (57 tests), JAR packaging, and Docker image build without targeting the live staging container.
3. **Ansible Configuration Management:**
   - Explains declarative playbook [`ansible/playbooks/deploy-claimguard.yml`](file:///E:/Desktop/Raj%20Yadav/Projects/Claim-Fraud-Review-Dashboard/ansible/playbooks/deploy-claimguard.yml).
   - Demonstrates port isolation assertions and safe syntax validation via WSL:
     `powershell -ExecutionPolicy Bypass -File scripts\run-ansible-playbook.ps1 -SyntaxCheck`.

---

## 8. Environmental Dependencies & Limitations

| Feature | Offline Compatibility | Dependencies / Requirements |
| :--- | :---: | :--- |
| **ClaimGuard Web Dashboard** | **100% Offline** | Docker Desktop (WSL 2 backend) |
| **REST APIs & Health Probes** | **100% Offline** | Local container on port 8095 |
| **Fraud Heuristics & Scoring** | **100% Offline** | Embedded in container image |
| **H2 Database Persistence** | **100% Offline** | Docker named volume |
| **Jenkins Build Demonstration** | Offline (Requires local Jenkins) | Jenkins Controller with Docker pipeline plugins |
| **Ansible Syntax Validation** | Offline (Requires WSL 2 Ubuntu) | Ubuntu distribution with `ansible-core` installed |

---

## 9. Conclusion

Phase D8 has delivered a **complete, verified, self-contained college demonstration package**.  
The portable archive `claimguard-college-demo.zip` is ready for presentation on any independent Windows machine with Docker Desktop.

All live staging containers, production storage boundaries, and runbook Section 5 cutovers remain strictly untouched.
