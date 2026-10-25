# ClaimGuard Phase D6 — Full Recovery Simulation Report

**Project:** `E:\Desktop\Raj Yadav\Projects\Claim-Fraud-Review-Dashboard`  
**Branch:** `feature/phase-d-reconciliation`  
**Candidate Image:** `claimguard:phase1-reconciled`  
**Expected Image ID:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`  
**Execution Timestamp:** 2026-10-25T19:56:40Z to 2026-10-25T19:57:04Z (Local: 2026-10-26T01:26:40+05:30)  
**Safety Status:** **LIVE STAGING CONTAINER (`910771a20872`), HOST PORT 8083, AND PRODUCTION VOLUME `claimguard-data` REMAIN 100% UNTOUCHED. RUNBOOK SECTION 5 WAS NOT EXECUTED.**

---

## Executive Summary

A full end-to-end recovery simulation was conducted in complete isolation using dedicated, disposable Docker test resources. The simulation verified the capability of candidate image `claimguard:phase1-reconciled` to start against an empty file-backed database, seed its baseline state, authenticate via an administrator account, ingest archived candidate-era claims and audit history via the supported reconciliation endpoint (`POST /api/admin/reconcile`), maintain strict idempotency across repeated submissions, persist data across container restarts, and generate subsequent claim identities without primary key collisions.

Every check in the 15-step procedure has been verified and classified as **PASS**.

---

## 1. Check Classification Summary

| # | Verification Check | Expected Target | Observed Result | Classification |
| :---: | :--- | :--- | :--- | :---: |
| **1** | Candidate Image ID Verification | `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043` | `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043` | **PASS** |
| **2** | Archived Evidence Validation | 5 valid artifacts; SHA-256 matches manifest | 5 files verified; exact SHA-256 match | **PASS** |
| **3** | Disposable Resource Allocation | Clean isolation; no port/name collision | Allocated port `8089`, container `claimguard-d6-sim-test`, volume `claimguard-d6-sim-data` | **PASS** |
| **4** | Candidate Database Mode & Health | Health UP, mode `H2 File-Backed (Operational)` | `{"status":"UP","database":"H2 File-Backed (Operational)"}` | **PASS** |
| **5** | Baseline Seeding Verification | Exactly 5 claims, 9 cumulative audit events | 5 claims (`CLM-2026-0001`..`0005`), 9 audit events | **PASS** |
| **6** | Administrator Authentication & CSRF | Role `ROLE_ADMIN`, valid session & CSRF | Admin authenticated; CSRF token acquired | **PASS** |
| **7** | Candidate Data Ingestion | Supported schema; HTTP 200; `status: "SUCCESS"` | HTTP 200; 2 inserted, 5 skipped, 3 history inserted | **PASS** |
| **8** | Post-Import Dataset Verification | Exactly 7 total claims, 12 cumulative audit events | 7 total claims; 12 cumulative audit events correctly linked | **PASS** |
| **9** | Idempotency Verification | Repeated submission: 0 inserted, 7 skipped, 0 history | Repeated submission: 0 inserted, 7 skipped, 0 history; totals unchanged | **PASS** |
| **10** | Attribute & Relationship Integrity | Core fields, amounts, statuses, `sourceEventId`s | All fields match export; source events `10`, `11`, `12` recorded | **PASS** |
| **11** | Container Restart Persistence | 7 claims, 12 events retained after container reboot | All 7 claims and 12 audit events persisted intact | **PASS** |
| **12** | Identity Generation Post-Recovery | New claim allocated ID $\ge 8$ without collision | Created `CLM-2026-0008`, allocated ID `8` | **PASS** |
| **13** | Evidence Capture & Redaction | Structured evidence saved; secrets masked | Captured `d6-simulation-evidence.json`; secrets redacted | **PASS** |
| **14** | Disposable Resource Cleanup | Test container and volume removed | Container and volume removed cleanly | **PASS** |
| **15** | Production Invariants & Staging Health | `claimguard-staging` untouched; no prod volume | Container `910771a20872` UP 7+ hrs on 8083; `claimguard-data` absent | **PASS** |

---

## 2. Test Execution Details

### Step 1 — Candidate Image Verification
- **Command:** `docker image inspect claimguard:phase1-reconciled --format "{{.Id}}"`
- **Expected ID:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`
- **Actual ID:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`
- **Result:** **PASS**

### Step 2 — Archived Evidence Manifest & Checksum Validation
The simulation resolved and validated the 5 evidence artifacts in `scratch/iso-test-evidence/`:
1. `iso-candidate-export-manifest.json`
2. `iso-candidate-claims-export.json` (7 claims)
   - Calculated SHA-256: `07B2939AD60B70F0B4CEEC95E3A0C53DBD169511D4C1637406B306521659AB6A` (Matched manifest)
3. `iso-candidate-history-export.json` (7 claim entries, 12 cumulative audit events)
   - Calculated SHA-256: `844F378741899E029EA01F1B6156D8DD22B37188FF691B572FB3D41E0AF63B90` (Matched manifest)
4. `legacy-baseline-manifest.json` (Documenting 5 baseline claims and 9 audit events)
5. `candidate-persistence-manifest.json` (Documenting candidate-era claims `CLM-2026-0006` and `CLM-2026-0007`)
- **Result:** **PASS**

### Step 3 — Disposable Test Resource Allocation
- **Container Name:** `claimguard-d6-sim-test`
- **Volume Name:** `claimguard-d6-sim-data` (dedicated persistent volume, physically separated from production)
- **Host Port:** `8089` (confirmed unused)
- **Result:** **PASS**

### Step 4 — Candidate Container Startup & Health Verification
- **Run Command:**
  ```powershell
  docker run -d --name claimguard-d6-sim-test `
      -p 8089:8080 `
      -v claimguard-d6-sim-data:/app/data `
      -e CLAIMGUARD_DB_PATH=/app/data/claimguard `
      -e CLAIMGUARD_ADMIN_PASSWORD=[REDACTED] `
      -e CLAIMGUARD_REVIEWER_PASSWORD=[REDACTED] `
      claimguard:phase1-reconciled
  ```
- **Container ID:** `1e010e5f66932ef253cdd54e4184f060829fca8dd1abe70a67eb29567909fcdc`
- **Health Response (`GET http://localhost:8089/api/health`):**
  ```json
  {
    "database": "H2 File-Backed (Operational)",
    "application": "ClaimGuard Fraud Review Dashboard",
    "version": "1.0.0-SNAPSHOT",
    "status": "UP",
    "timestamp": "2026-10-25T19:56:49.629844056"
  }
  ```
- **Result:** **PASS**

### Step 5 — Baseline State Verification (Pre-Reconciliation)
The candidate container booted and executed `DataSeederService.java` against the fresh persistent database:
- **Baseline Claims Count:** Exactly 5 claims
  - `CLM-2026-0001`: Rajesh Kumar | UNDER_REVIEW | ₹185,000.00
  - `CLM-2026-0002`: Anita Desai | SUBMITTED | ₹45,000.00
  - `CLM-2026-0003`: Vikram Mehta | MORE_INFO_REQUIRED | ₹320,000.00
  - `CLM-2026-0004`: Sunita Rao | APPROVED | ₹82,000.00
  - `CLM-2026-0005`: Rahul Verma | REJECTED | ₹215,000.00
- **Baseline Audit Events:** Exactly 9 cumulative audit events
  - `CLM-2026-0001`: 2 events (SUBMITTED, REVIEW_STARTED)
  - `CLM-2026-0002`: 1 event (SUBMITTED)
  - `CLM-2026-0003`: 2 events (SUBMITTED, INFO_REQUESTED)
  - `CLM-2026-0004`: 2 events (SUBMITTED, APPROVED)
  - `CLM-2026-0005`: 2 events (SUBMITTED, REJECTED)
- **Result:** **PASS**

---

## 3. Data Ingestion & Record Arithmetic

### Step 6 & 7 — Reconciliation Submission
The candidate claims and history from the archived export were mapped to `ReconciliationPayloadDTO` (`schemaVersion = "1.0"`).
- Candidate claims `CLM-2026-0006` and `CLM-2026-0007` carried their candidate-era audit history (3 events: Event 10, Event 11, Event 12 with source event identifiers).
- Baseline claims `CLM-2026-0001` through `CLM-2026-0005` were submitted to allow natural key deduplication against the baseline target database.
- **Request:** `POST http://localhost:8089/api/admin/reconcile` (with `X-XSRF-TOKEN` and admin cookie)
- **Response:**
  ```json
  {
    "totalReceived": 7,
    "claimsInserted": 2,
    "claimsSkipped": 5,
    "historyRecordsInserted": 3,
    "indicatorsInserted": 0,
    "documentsInserted": 0,
    "status": "SUCCESS",
    "message": "Reconciliation successfully executed.",
    "reconciledReferences": [
      "CLM-2026-0007",
      "CLM-2026-0006"
    ]
  }
  ```
- **Distinction between Total Claims vs. Newly Imported Claims:**
  - **Total Claims in Payload:** 7
  - **Newly Imported Candidate Claims:** Exactly 2 (`CLM-2026-0006` and `CLM-2026-0007`)
  - **Pre-existing Baseline Claims Skipped:** Exactly 5 (`CLM-2026-0001` through `CLM-2026-0005`)
- **Distinction between History Entries vs. Individual Audit Events:**
  - **Archived History Entries in JSON Array:** 7 (one array element per claim reference)
  - **Newly Imported Candidate Audit Events:** Exactly 3 (2 events for `CLM-2026-0006`, 1 event for `CLM-2026-0007`)
- **Result:** **PASS**

### Step 8 — Resulting Dataset & Relationship Integrity
- **Total Claims in Target Database:** Exactly 7
- **Candidate Claims Verified:**
  - `CLM-2026-0006`: Assigned ID `7`, Claimant "Anil Sharma", Status `UNDER_REVIEW`, Claimed Amount ₹12,500.00
  - `CLM-2026-0007`: Assigned ID `6`, Claimant "Meera Gupta", Status `SUBMITTED`, Claimed Amount ₹8,400.00
- **Cumulative Audit Events:** Exactly 12 cumulative audit events correctly linked across all 7 claims:
  - Claim 1 (`CLM-2026-0001`): 2 events
  - Claim 2 (`CLM-2026-0002`): 1 event
  - Claim 3 (`CLM-2026-0003`): 2 events
  - Claim 4 (`CLM-2026-0004`): 2 events
  - Claim 5 (`CLM-2026-0005`): 2 events
  - Claim 6 (`CLM-2026-0006`): 2 events (`sourceEventId`: "10", "12")
  - Claim 7 (`CLM-2026-0007`): 1 event (`sourceEventId`: "11")
  $$\text{Total Audit Events} = 9 \text{ (baseline)} + 3 \text{ (candidate-era)} = 12$$
- **Result:** **PASS**

---

## 4. Idempotency, Durability & Identity Sequences

### Step 9 — Idempotency Re-Verification
The exact same 7-claim payload was submitted a second time to `POST http://localhost:8089/api/admin/reconcile`.
- **Response:**
  ```json
  {
    "totalReceived": 7,
    "claimsInserted": 0,
    "claimsSkipped": 7,
    "historyRecordsInserted": 0,
    "indicatorsInserted": 0,
    "documentsInserted": 0,
    "status": "SUCCESS",
    "message": "Reconciliation successfully executed.",
    "reconciledReferences": []
  }
  ```
- **Post-Idempotency Database Verification:**
  - Total Claims Count: 7 (Unchanged)
  - Cumulative Audit Events: 12 (Unchanged)
  - No duplicate claims, duplicate history entries, or corrupted foreign keys were produced.
- **Result:** **PASS**

### Step 10 — Detailed Attribute & Integrity Verification
Every field of the recovered candidate records was inspected via `GET /api/claims/{id}` and `GET /api/claims/{id}/history`:
- `CLM-2026-0006`: Policy `POL-ISO-001`, Amount 12,500.00, Status `UNDER_REVIEW`, Reviewer Priya Sharma.
- `CLM-2026-0007`: Policy `POL-ISO-002`, Amount 8,400.00, Status `SUBMITTED`, Reviewer System Intake.
- Audit history foreign keys strictly point to their respective parent claims with verified `sourceEventId` mappings.
- **Result:** **PASS**

### Step 11 — Container Restart Persistence
- The test container was stopped and restarted: `docker restart claimguard-d6-sim-test`.
- Post-restart health confirmed operational status:
  `{"status":"UP","database":"H2 File-Backed (Operational)"}`
- Re-authenticated fresh session and queried all claims and history:
  - Total Claims: Exactly 7
  - Total Audit Events: Exactly 12
  - All candidate records (`CLM-2026-0006`, `CLM-2026-0007`) and baseline records survived restart with zero loss.
- **Result:** **PASS**

### Step 12 — Subsequent Identity Generation & Collision Immunity
An additional synthetic claim was created via the standard application API (`POST /api/claims`) to verify that the database identity sequence (`claims` table auto-increment) was successfully synchronized by `ClaimReconciliationService.syncIdentitySequences()`:
- **API Request:** `POST /api/claims` (Payload: Policy `POL-POST-RECON-D6`, Claimant "Pooja Verma", Amount 18,500.00)
- **API Response:**
  ```json
  {
    "id": 8,
    "claimReference": "CLM-2026-0008",
    "policyNumber": "POL-POST-RECON-D6",
    "claimantName": "Pooja Verma",
    "status": "SUBMITTED",
    "claimedAmount": 18500.00
  }
  ```
- **Collision Immunity Analysis:**
  - Recovered claims occupied IDs up to `7`.
  - The sequence generator successfully allocated ID `8` (`CLM-2026-0008`).
  - No `JdbcSQLIntegrityConstraintViolationException` occurred.
- **Result:** **PASS**

---

## 5. Teardown & Safety Verification

### Step 13 — Evidence Archival
Simulation telemetry, HTTP status codes, record counts, and verification outcomes were compiled and saved to:
`scratch/iso-test-evidence/d6-simulation-evidence.json` (SHA-256: `9B4EF7B4BC8C50C36B690CD1BD6C36109DE3F0F4EFE6F385F2C55312BD6285DF`).
All passwords, session cookies, and CSRF tokens were completely redacted.

### Step 14 — Disposable Resource Teardown
- `docker stop claimguard-d6-sim-test`
- `docker rm claimguard-d6-sim-test`
- `docker volume rm claimguard-d6-sim-data`
- Confirmed zero residual test containers or test volumes remain.
- **Result:** **PASS**

### Step 15 — Live Staging & Production Volume Isolation
- **Live Staging Container:** `claimguard-staging` (ID: `910771a20872`)
  - Status: `Up 7 hours`
  - Port: `0.0.0.0:8083->8080/tcp`
  - Health Response: `{"status":"UP","database":"H2 In-Memory (Operational)"}`
  - Confirmed 100% untouched and healthy.
- **Production Volume `claimguard-data`:**
  - Inspected Docker volumes: `docker volume ls -q --filter "name=^claimguard-data$"`
  - Output: Empty. The intended production volume has **never been created or modified**.
- **Runbook Section 5:** Confirmed **NOT EXECUTED**.
- **Result:** **PASS**

---

## 6. Unresolved Data-Loss and Compatibility Risks

1. **Baseline Seeder Timestamp Variance vs. Archived Exports:**
   - **Finding:** `DataSeederService.java` generates baseline claim timestamps dynamically at boot time using `LocalDateTime.now().minusDays(1)`.
   - **Compatibility Risk:** If a raw export containing baseline claims' history from an earlier date is submitted directly into a freshly booted container without delta filtering, `ClaimReconciliationService` cannot match the seeder timestamps ($> 1000\text{ ms}$ difference) and will duplicate the baseline audit events on existing claims.
   - **Mitigation:** The deployment runbook and recovery automation must ensure that reconciliation payloads submitted for recovery either:
     a) Contain only candidate-era claims (`id >= 6`, references `CLM-2026-0006+`) and their candidate-era history; or
     b) If submitting full exports, omit the seeder review history for pre-existing baseline claims unless candidate-era review transitions were actually performed on those baseline claims.

2. **In-Memory Legacy Rollback Reality (Re-emphasized):**
   - **Finding:** The legacy staging container (`claimguard:stitch-ui-staging`) operates exclusively in-memory (`jdbc:h2:mem:claimguard`).
   - **Data-Loss Risk:** Reconciling candidate claims into an in-memory database will succeed for the active session, but **all recovered candidate claims and history will be instantly destroyed if that container restarts or stops**.
   - **Mitigation:** As designed in Phase D, durable recovery is only guaranteed when the target container is operated with persistent file-backed storage (`CLAIMGUARD_DB_PATH=/app/data/claimguard` backed by a persistent Docker volume).

3. **Character Encoding on Review Remarks:**
   - **Finding:** In `CLM-2026-0004`, reviewer remarks in `DataSeederService` contain the Rupee symbol (`₹`). When exported and imported across PowerShell JSON pipelines without explicit UTF-8 encoding, character corruption (`â‚¹`) can cause canonical fingerprint comparison to fail.
   - **Mitigation:** All export and import payloads must enforce strict `-Encoding UTF8` and UTF-8 Content-Type headers.

---

## 7. Conclusion

Phase D6 Full Recovery Simulation is **100% COMPLETE and VERIFIED**.  
Candidate image `claimguard:phase1-reconciled` (`sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`) is fully proven to support non-destructive candidate claim recovery, audit trail preservation, idempotent re-runs, file-backed restart durability, and collision-free sequence progression.

Runbook Section 5 live cutover remains **ON HOLD** awaiting explicit user authorization.
