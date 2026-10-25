# ClaimGuard D5 — POST/GET API Smoke Test Report

**Execution Date:** October 26, 2026 (System UTC: October 25, 2026 18:32 UTC)  
**Project:** `E:\Desktop\Raj Yadav\Projects\Claim-Fraud-Review-Dashboard`  
**Candidate Image:** `claimguard:phase1-reconciled`  
**Expected Image ID:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`  
**Actual Image ID:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043` (Verified MATCH)  
**Isolated Host Port:** `8088` (mapped to container port `8080`)  
**Isolated Disposable Volume:** `claimguard-smoke-data`  
**Target Service Status:** Pre-existing `claimguard-staging` on port 8083 was **UNTOUCHED**; Section 5 cutover was **NOT** executed; production volume `claimguard-data` was **NOT** created.

---

## 1. Executive Summary

This smoke test performed complete end-to-end isolated API validation of the freshly built candidate Docker image `claimguard:phase1-reconciled`. Testing evaluated health probes, RBAC authentication, CSRF token handling, synthetic claim submission (`POST /api/claims`), seeded baseline retrieval (`GET /api/claims/1`), created claim retrieval (`GET /api/claims/{id}`), review history retrieval (`GET /api/claims/{id}/history`), and container restart data persistence.

All tested operations completed successfully with **100% PASS** rate. All disposable test resources were cleanly destroyed following execution.

---

## 2. API Contract & Endpoint Specification

Per inspection of `ClaimController.java`, `CreateClaimRequest.java`, and `SecurityConfig.java`:

| Endpoint | HTTP Method | Auth Required | CSRF Required | Request Body / Parameters | Response Type |
|:---|:---:|:---:|:---:|:---|:---|
| `/api/health` | GET | No | No | None | `Map<String, Object>` (status, database mode) |
| `/api/auth/csrf` | GET | No | No | None | `Map<String, String>` (token, headerName, parameterName) |
| `/api/auth/login` | POST | No | Yes | `username`, `password` (form-urlencoded) | `Map<String, Object>` (status, authorities, redirectUrl) |
| `/api/claims` | POST | Yes (`ROLE_ADMIN` or `ROLE_REVIEWER`) | Yes (`X-XSRF-TOKEN`) | `CreateClaimRequest` JSON | `ClaimDetailDTO` (HTTP 201 Created) |
| `/api/claims/1` | GET | Yes (`ROLE_ADMIN` or `ROLE_REVIEWER`) | No | `id = 1` (path parameter) | `ClaimDetailDTO` (HTTP 200 OK) |
| `/api/claims/{id}` | GET | Yes (`ROLE_ADMIN` or `ROLE_REVIEWER`) | No | `id = {generatedId}` (path parameter) | `ClaimDetailDTO` (HTTP 200 OK) |
| `/api/claims/{id}/history` | GET | Yes (`ROLE_ADMIN` or `ROLE_REVIEWER`) | No | `id = {generatedId}` (path parameter) | `List<ReviewHistory>` (HTTP 200 OK) |

---

## 3. Step-by-Step Test Results

### Step 1: Candidate Image ID Verification
- **Command:** `docker image inspect claimguard:phase1-reconciled --format "{{.Id}}"`
- **Expected ID:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`
- **Actual ID:** `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`
- **Result:** **PASS**

### Step 2: Isolated Container Launch & Database Mode Check
- **Container Name:** `claimguard-smoke-test`
- **Host Port:** `8088`
- **Storage Configuration:** `-v claimguard-smoke-data:/app/data`, `-e CLAIMGUARD_DB_PATH=/app/data/claimguard`
- **Probe Endpoint:** `GET http://localhost:8088/api/health`
- **Probe Response:**
  ```json
  {
    "status": "UP",
    "application": "ClaimGuard Fraud Review Dashboard",
    "version": "1.0.0-SNAPSHOT",
    "database": "H2 File-Backed (Operational)",
    "timestamp": "2026-10-25T18:32:00.280944798"
  }
  ```
- **Result:** **PASS** (Correctly reports file-backed operational status)

### Step 3: Authentication & CSRF Token Acquisition
- **Action:** Acquired CSRF token via `GET /api/auth/csrf` (`parameterName: "_csrf"`, `headerName: "X-XSRF-TOKEN"`).
- **Login Action:** `POST /api/auth/login` with test reviewer credentials.
- **Login Response:** `{"status": "authenticated", "username": "priya.sharma", "authorities": ["ROLE_REVIEWER"], "redirectUrl": "/index.html"}`
- **Result:** **PASS** (Authenticated session established)

### Step 4: POST /api/claims (Synthetic Claim Submission)
- **HTTP Method & URI:** `POST http://localhost:8088/api/claims`
- **Request Headers:** `Content-Type: application/json`, `X-XSRF-TOKEN: [REDACTED]`
- **Request Payload:**
  ```json
  {
    "policyNumber": "POL-SMOKE-2026-99",
    "claimantName": "Aarav Mehta",
    "contactPhone": "+91 98111 22334",
    "contactEmail": "aarav.mehta@example.com",
    "claimType": "MOTOR",
    "incidentDate": "2026-02-18T11:30:00",
    "claimedAmount": 42500.00,
    "incidentDescription": "Front wing mirror and passenger door scrape against pillar in underground parking.",
    "vehicleRegNumber": "MH-01-AB-1234",
    "incidentLocation": "Nariman Point, Mumbai"
  }
  ```
- **HTTP Status Code:** `201 Created`
- **Key Response Fields:**
  - `id`: `6`
  - `claimReference`: `"CLM-2026-0006"`
  - `status`: `"SUBMITTED"`
  - `riskPriority`: `"HIGH"` (heuristic evaluation triggered indicators)
  - `claimedAmount`: `42500.00`
  - `submissionDate`: `"2026-10-25T18:32:00.869407335"`
- **Result:** **PASS** (New claim generated with ID 6 and reference CLM-2026-0006)

### Step 5: GET /api/claims/1 (Baseline Seeded Claim Verification)
- **HTTP Method & URI:** `GET http://localhost:8088/api/claims/1`
- **HTTP Status Code:** `200 OK`
- **Key Response Fields:**
  - `id`: `1`
  - `claimReference`: `"CLM-2026-0001"`
  - `policyNumber`: `"POL-MH-9482-1049"`
  - `claimantName`: `"Rajesh Kumar"`
  - `status`: `"UNDER_REVIEW"`
  - `riskPriority`: `"HIGH"`
  - `claimedAmount`: `185000.00`
- **Record Match Verification:** Stored record in the file-backed database strictly matches the known baseline seeded record.
- **Result:** **PASS**

### Step 6: GET /api/claims/6 (Newly Created Claim Verification)
- **HTTP Method & URI:** `GET http://localhost:8088/api/claims/6`
- **HTTP Status Code:** `200 OK`
- **Field Comparison with POST Response:**
  - `id`: `6` (Matched)
  - `claimReference`: `"CLM-2026-0006"` (Matched)
  - `policyNumber`: `"POL-SMOKE-2026-99"` (Matched)
  - `claimantName`: `"Aarav Mehta"` (Matched)
  - `claimedAmount`: `42500.00` (Matched)
  - `status`: `"SUBMITTED"` (Matched)
- **Result:** **PASS** (GET response exactly corroborates POST response)

### Step 7: GET /api/claims/6/history (Review History Event Verification)
- **HTTP Method & URI:** `GET http://localhost:8088/api/claims/6/history`
- **HTTP Status Code:** `200 OK`
- **Record Count:** `1`
- **Event Details:**
  - `id`: `10`
  - `actionType`: `"SUBMITTED"`
  - `previousStatus`: `null`
  - `newStatus`: `"SUBMITTED"`
  - `reviewerName`: `"System Intake"`
  - `reviewerRemarks`: `"Claim successfully ingested via API with 2 heuristic indicator(s) evaluated."`
  - `actionTimestamp`: `"2026-10-25T18:32:00.88096"`
- **Linkage Verification:** Review history event is properly associated with claim ID `6`.
- **Result:** **PASS**

### Step 8: Container Restart & File-Backed Persistence Test
- **Action:** Restarted test container `claimguard-smoke-test` via `docker restart`.
- **Post-Restart Health Probe:** `status: "UP"`, `database: "H2 File-Backed (Operational)"`.
- **Post-Restart Session:** Authenticated a new reviewer session with fresh CSRF token.
- **Post-Restart GET /api/claims/1:** Retrieved successfully; reference `CLM-2026-0001`, claimant `Rajesh Kumar`.
- **Post-Restart GET /api/claims/6:** Retrieved successfully; reference `CLM-2026-0006`, claimant `Aarav Mehta`, amount `42500.00`.
- **Post-Restart GET /api/claims/6/history:** Retrieved successfully; 1 event, action `SUBMITTED`, reviewer `System Intake`.
- **Result:** **PASS** (All data retained across process and container termination)

### Step 9: Disposable Resource Cleanup
- **Resources Cleaned:**
  - Container `claimguard-smoke-test` stopped and removed.
  - Named volume `claimguard-smoke-data` removed.
- **Cleanup Mode:** Targeted resource names only. No broad cleanup commands (`docker prune` or `docker rm $(docker ps -aq)`) were executed.
- **Result:** **PASS**

---

## 4. Safety & Invariant Verification

| Item | Requirement | Verification Result | Status |
|:---|:---|:---|:---:|
| **Live Staging Container** | Do not touch, restart, or modify `claimguard-staging` | Verified running continuously (`Up 6 hours`), ID `910771a20872` | **PASS** |
| **Host Port 8083** | Do not modify or route test traffic to port 8083 | Verified healthy (`database: "H2 In-Memory (Operational)"`) | **PASS** |
| **Production Volume** | Do not create or mount `claimguard-data` | Verified volume `claimguard-data` does NOT exist | **PASS** |
| **Runbook Section 5** | Do not execute cutover commands | Cutover sequence remains untouched on hold | **PASS** |
| **Credential Redaction** | Do not leak passwords or secrets | All tokens, cookies, and passwords redacted from report | **PASS** |

---

## 5. Failures and Non-Applicable Tests

- **Failures:** `0` failures encountered.
- **Tests Not Applicable:** None. All required POST, GET, history, and persistence validation steps were supported and fully executed.

---
*Report generated and validated for ClaimGuard D5 Smoke Testing.*
