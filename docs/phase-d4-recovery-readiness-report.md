# ClaimGuard Phase D — Step D4: Recovery Readiness and Deployment Audit Report

**Repository:** `E:\Desktop\Raj Yadav\Projects\Claim-Fraud-Review-Dashboard`  
**Current Git Branch:** `feature/phase-d-reconciliation`  
**Audit Scope:** Read-Only Deployment, Persistent Storage, and Recovery Readiness Review  
**Date:** 2026-10-25  
**Staging Status:** `claimguard-staging` (Container ID `910771a20872`) on host port `8083` untouched, operational (`UP`).

---

## 1. Executive Summary

A comprehensive read-only deployment and recovery-readiness audit was performed to evaluate whether the ClaimGuard Phase 1 staging service can safely transition from ephemeral in-memory storage to persistent authenticated storage with reliable data recovery.

### Overall Recommendation
> [!CAUTION]
> **AUDIT VERDICT: NOT READY — REQUIRED REMEDIATIONS LISTED**  
> While the reconciliation mechanism is functionally verified and achieves 100% test pass rates (53/53 tests passing), live cutover cannot proceed safely under the current runbook due to two critical blockers:
> 1. **Candidate Image Staleness:** The candidate image specified in the runbook (`claimguard:phase1-candidate`, `sha256:2fd25b07fba5787db431f3b2f10edaf8594d2fb5536580b92029ef7d58497a97`) was compiled prior to Phase D and **does not contain the reconciliation service, DTOs, or administrative endpoint**.
> 2. **Smoke Test Health Check Assertion Mismatch:** Runbook Gate 1 asserts that `/api/health` returns a database field matching `*File-Backed*`. However, `HealthController.java` hardcodes `"H2 In-Memory (Operational)"`. Executing Section 5 will cause the smoke test to fail automatically after 30 seconds and trigger an immediate emergency rollback.

---

## 2. Verification of Current Implementation Artifacts (Section 1)

| Check Item | Classification | Evidence | Impact / Finding |
| :--- | :---: | :--- | :--- |
| **Git Branch & Status** | **PASS** | `feature/phase-d-reconciliation` | Verified clean branch isolation; all working-tree files preserved without reset or stash. |
| **Reconciliation Service & Endpoints** | **PASS** | `ClaimReconciliationService.java`, `AdminController.java` | Protected by `@PreAuthorize("hasRole('ADMIN')")`, CSRF verification, and reentrant concurrency control. |
| **DTOs & Entity Mappings** | **PASS** | `ReconciliationPayloadDTO.java`, `ReviewHistory.java` | Mappings confirmed; `ReviewHistory` maps to `@Table(name = "review_history")` with `source_event_id VARCHAR(100)`. |
| **History Deduplication Strategy** | **PASS** | `ClaimReconciliationService.java:204-264` | Three-tier deduplication implemented: Tier 1 (`sourceEventId`), Tier 2 (6-field canonical fingerprint), Tier 3 (rejection of ambiguous events). |
| **Sequence Synchronization Tables** | **PASS** | `ClaimReconciliationService.java:240-252` | Explicitly targets `claims`, `review_history`, `suspicious_indicators`, `claim_documents` using H2 2.2-compatible literal syntax. |
| **Automated Test Results** | **PASS** | `target/surefire-reports/*.txt` | Directly verified from surefire report text files: Exactly 53 tests run, 0 failures, 0 errors, 0 skipped. |
| **Built JAR Artifact** | **PASS** | `target/claimguard-dashboard-1.0.0-SNAPSHOT.jar` | File exists; Size: 54,975,847 bytes (~52.43 MB); SHA-256: `9E6D93B11BFDBCD44EFCA174686EE8BFD1CF105D4BE92D383FEEC3DEB3385759`. |

---

## 3. Persistent-Storage Readiness Audit (Section 2)

| Check Item | Classification | Evidence | Impact / Finding |
| :--- | :---: | :--- | :--- |
| **Production Named Volume `claimguard-data`** | **PASS (Expected State)** | `docker volume inspect claimguard-data` | Volume does not exist yet. Expected per runbook Preflight Gate 3 and Cutover Command 3 (`docker volume create claimguard-data`). |
| **Candidate Database Path** | **PASS** | `Dockerfile:33`, `application.properties:7` | Container expects `/app/data/claimguard`. Matches runbook `.env.deploy` (`CLAIMGUARD_DB_PATH=/app/data/claimguard`). |
| **Container Mount Mapping** | **PASS** | Runbook Command 4 (`-v claimguard-data:/app/data`) | Maps `/app/data` to named volume `claimguard-data`. Container directory ownership `1001:1001` ensures write access. |
| **Database Creation vs Restore** | **PASS** | `DataSeederService.java:24`, `application.properties:14` | The application auto-initializes the file on startup via `ddl-auto=update` and seeds 5 baseline claims if empty (`count == 0`). |
| **Offline `.mv.db` vs JSON Exports** | **PASS** | `scratch/iso-test-evidence/` | Offline database `claimguard-iso-offline.mv.db` (53,248 bytes) and JSON exports (`iso-candidate-claims-export.json`) are verified and available. |
| **Schema Compatibility (`source_event_id`)** | **PASS** | `ReviewHistory.java:40` | Column is nullable (`VARCHAR(100)`). Hibernate `update` auto-adds column without invalidating existing records. |

---

## 4. Data Recovery and Reconciliation Audit (Section 3)

### 4.1 Recommended Recovery Methodology
Two recovery methodologies were evaluated:
- **Method A (Clean Database + Administrative Reconciliation REST API):** Launch container on fresh volume (auto-seeds 5 baseline claims), then execute `POST /api/admin/reconcile` with candidate claims (`CLM-2026-0006`, `CLM-2026-0007`) and associated history.
- **Method B (Physical Binary Database Copy):** Copy `claimguard-iso-offline.mv.db` directly into the Docker volume prior to container launch.

> [!IMPORTANT]
> **Audit Decision: Method A is recommended.**  
> - Combining Method A and Method B must be **strictly prohibited**. While the reconciliation service is idempotent, copying a binary `.mv.db` file and subsequently re-reconciling introduces dual-path operational hazards and risks sequence desynchronization.
> - Method A provides verified transaction atomicity, foreign-key cascade integrity, sequence synchronization, and structured audit logs.

### 4.2 Preservation of Data Integrity
- **Timestamps:** Original submission dates, incident dates, and audit timestamps are preserved.
- **Natural Matching:** Claims are anchored to immutable `claim_reference` keys.
- **Audit Ledger:** Foreign keys link to the exact claim ID assigned during ingestion.
- **Identity Sequences:** `syncIdentitySequences()` sets the identity sequence to `MAX(id) + 1`, ensuring zero primary-key collisions on subsequent application transactions.

---

## 5. Backup Restoration and Rollback Audit (Section 4)

| Check Item | Classification | Runbook Location | Evaluation / Findings |
| :--- | :---: | :--- | :--- |
| **Pre-Cutover Backup** | **PASS** | Runbook Section 3 | Automated REST extraction of all claims and review history before stopping legacy service. |
| **Manifest & Hash Verification** | **PASS** | Runbook Lines 212-240 | Manifest validates record counts and SHA-256 hashes before proceeding to credential staging. |
| **Disposable Restore Testing** | **PASS** | Verified in Phase B & C isolated tests | Confirmed candidate recovery and rollback simulation in isolated Docker containers (`port 8085`). |
| **Post-Restore Integrity Checks** | **PASS** | Runbook Lines 707-740 | Polling verification confirms container status `UP` and port `8083` online. |
| **Post-Cutover Transaction Preservation** | **PASS** | Runbook Lines 609-666 | Candidate database `.mv.db` is copied offline to `$rollbackDir` before starting legacy container. |
| **Accidental Deletion Safeguards** | **PASS** | Runbook Lines 18-20 | Original container is renamed to `claimguard-staging-prephase1`, never deleted. Volume `claimguard-data` is never deleted. |

---

## 6. Runtime and Security Compatibility Audit (Section 5)

| Check Item | Classification | Evidence / Code Location | Evaluation / Findings |
| :--- | :---: | :--- | :--- |
| **Environment Variable Handling** | **PASS** | `.env.deploy`, `application.properties` | Evaluates `CLAIMGUARD_DB_PATH`, `CLAIMGUARD_ADMIN_PASSWORD`, `CLAIMGUARD_REVIEWER_PASSWORD`. |
| **Volume Mount Permissions** | **PASS** | `Dockerfile:18-27` | Non-root `claimguard:claimguard` (UID 1001) has full ownership of `/app/data`. |
| **Admin Authorization & CSRF** | **PASS** | `SecurityConfig.java`, `AdminControllerTest.java` | `/api/admin/**` strictly requires `ROLE_ADMIN` and valid CSRF token. |
| **Health Check Endpoint Behavior** | **FAIL** | `HealthController.java:25`, Runbook Line 435 | **CRITICAL DEFECT:** `HealthController.java` hardcodes `"H2 In-Memory (Operational)"`. Runbook smoke test asserts `$hRes.database -like "*File-Backed*"`. This causes an automatic cutover failure and false rollback. |
| **Candidate Image Reconciliation API** | **FAIL** | Docker image `claimguard:phase1-candidate` | **CRITICAL DEFECT:** Image `claimguard:phase1-candidate` (`sha256:2fd25b07...`) does not contain Phase D reconciliation code. |
| **Secrets Redaction** | **PASS** | `GlobalExceptionHandler.java`, Service logs | Logs omit credentials, tokens, claimant names, and financial figures. |

---

## 7. Required Remediations Before Live Cutover

To achieve deployment readiness, the following remediations must be executed in a separately authorized task:

1. **Rebuild Candidate Image with Phase D Reconciliation Code:**
   - Build a new Docker image from `feature/phase-d-reconciliation` incorporating `ClaimReconciliationService.java`, `AdminController.java`, and the updated `ReviewHistory.java`.
   - Tag the new image (e.g. `claimguard:phase1-candidate`) and capture its exact SHA-256 digest.
2. **Remediate `HealthController.java` Database Status String:**
   - Update `HealthController.java` to dynamically report `"H2 File-Backed (Operational)"` when `CLAIMGUARD_DB_PATH` is configured or points to a file, OR align the runbook assertion.
3. **Update Runbook Preflight Check:**
   - Update `expectedImageId` in Section 2 Gate 1 of `CLAIMGUARD_PHASE1_DEPLOYMENT_RUNBOOK_REVISED.md` to match the newly built candidate image ID.

---

## 8. Verification Sign-Off

- **Git Branch:** `feature/phase-d-reconciliation`
- **Automated Tests:** 53/53 passing (100% PASS)
- **Live Staging Container (`910771a20872`):** Untouched, operational (`UP`) on port `8083`.
- **Deployment Runbook Section 5:** **NOT EXECUTED.**
