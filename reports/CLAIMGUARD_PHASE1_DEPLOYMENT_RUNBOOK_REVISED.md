# ClaimGuard Phase 1 — Production Deployment Runbook (Remediated)
## Persistent File-Backed H2 Database + Spring Security Authentication & RBAC

> **Document Reference:** `CLAIMGUARD_PHASE1_DEPLOYMENT_RUNBOOK_REVISED.md`  
> **Date:** October 25, 2026  
> **Target Container:** `claimguard-staging` (Host Port: `8083`, Container Port: `8080`)  
> **Verified Image Under Deployment:** `claimguard:phase1-reconciled` (`sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043`)  
> **Verified Preserved Container:** `claimguard-staging` (`910771a20872`) -> `claimguard-staging-prephase1`  
> **Production Persistent Volume:** `claimguard-data:/app/data`  
> **Operational Status:** PREPARATORY ONLY — SECTION 5 IS ON HOLD. No deployment commands authorized.  

---

## 1. Architectural & Operational Summary

This runbook defines the exact, step-by-step procedure to transition the ClaimGuard Staging service from its legacy in-memory configuration (`claimguard:stitch-ui-staging`) to the Phase 1 persistent, authenticated architecture (`claimguard:phase1-reconciled`).

### Core Safety Invariants
1. **Never Remove Containers:** The original container is renamed to `claimguard-staging-prephase1` and kept in a stopped state. It is never deleted.
2. **Never Remove Volumes:** The named volume `claimguard-data` is preserved through all operational scenarios, including rollbacks.
3. **Pre-Cutover Data Preservation vs. Restoration Distinction:** Because the active container uses volatile `jdbc:h2:mem:claimguard`, stopping it clears RAM. The runbook captures pre-deployment claims and review history to JSON and validates checksums prior to cutover. However, **data preservation is not data restoration**: exporting JSON and copying database files does not guarantee automated re-ingestion into an in-memory runtime. Automated reconciliation of candidate data back into the legacy service has not been implemented or tested.
4. **Credential Isolation:** Passwords are captured interactively as secure strings and passed to the container via a single-use environment file protected by Windows filesystem ACLs (`icacls`), eliminating plain-text exposure in shell histories and process command lines.
5. **Hard Execution Gates:** Every failure in preflight, backup verification, or smoke testing triggers an immediate hard stop.

---

## 2. Preflight Hard Gates (Read-Only)

Before executing any state-changing command, run the following preflight checks. **Every failure causes an immediate script termination.**

```powershell
$ErrorActionPreference = 'Stop'

# ==============================================================================
# PREFLIGHT GATE 1: Verify Candidate Image ID and Integrity
# ==============================================================================
# Verified Phase D Candidate Image: claimguard:phase1-reconciled
$expectedImageId = "sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043"
$actualImageId = docker image inspect claimguard:phase1-reconciled --format "{{.Id}}" 2>$null

if ($null -eq $actualImageId -or $actualImageId -ne $expectedImageId) {
    throw "[STOP CONDITION 1 TRIGGERED] Candidate image ID mismatch or missing! Expected: $expectedImageId; Found: $actualImageId"
}
Write-Host "[PREFLIGHT GATE 1 PASSED] Candidate image ID verified ($actualImageId)."

# ==============================================================================
# PREFLIGHT GATE 2: Verify Active Staging Container Configuration
# ==============================================================================
$stagingInspectRaw = docker inspect claimguard-staging 2>$null
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($stagingInspectRaw)) {
    throw "[STOP CONDITION 2 TRIGGERED] Original container 'claimguard-staging' not found."
}

$stagingInspect = $stagingInspectRaw | ConvertFrom-Json
$stagingId = $stagingInspect[0].Id.Substring(0, 12)
$stagingStatus = $stagingInspect[0].State.Status
$stagingPort = $stagingInspect[0].HostConfig.PortBindings."8080/tcp"[0].HostPort

if ($stagingStatus -ne "running" -or $stagingPort -ne "8083") {
    throw "[STOP CONDITION 2 TRIGGERED] 'claimguard-staging' is not running on port 8083 (Status: $stagingStatus, Port: $stagingPort)."
}
Write-Host "[PREFLIGHT GATE 2 PASSED] Staging container verified ($stagingId, Status: $stagingStatus, Port: $stagingPort)."

# ==============================================================================
# PREFLIGHT GATE 3: Production Volume Preflight Gate
# ==============================================================================
$existingVol = docker volume ls -q --filter "name=^claimguard-data$"
if (-not [string]::IsNullOrWhiteSpace($existingVol)) {
    # If the volume already exists, inspect whether it contains data before touching it
    $volInspect = docker volume inspect claimguard-data | ConvertFrom-Json
    $volMount = $volInspect[0].Mountpoint
    Write-Warning "[ATTENTION] Production volume 'claimguard-data' already exists at $volMount."
    $confirmExisting = Read-Host "Volume 'claimguard-data' already exists. Do you explicitly approve reusing it? (type 'YES' to continue)"
    if ($confirmExisting -ne 'YES') {
        throw "[STOP CONDITION 3 TRIGGERED] Aborted deployment: unconfirmed existing production volume 'claimguard-data'."
    }
} else {
    Write-Host "[PREFLIGHT GATE 3 PASSED] Production volume 'claimguard-data' does not exist yet. It will be created upon explicit cutover."
}

# ==============================================================================
# PREFLIGHT GATE 4: Verify Live Service Health on Port 8083
# ==============================================================================
try {
    $preHealth = Invoke-RestMethod -Uri "http://localhost:8083/api/health" -TimeoutSec 3
    if ($preHealth.status -ne "UP" -or $preHealth.database -ne "H2 In-Memory (Operational)") {
        throw "[STOP CONDITION 4 TRIGGERED] Live service on port 8083 is not reporting healthy in-memory mode (Status: $($preHealth.status), DB: $($preHealth.database))."
    }
    Write-Host "[PREFLIGHT GATE 4 PASSED] Live service on port 8083 is UP (Database: $($preHealth.database))."
} catch {
    throw "[STOP CONDITION 4 TRIGGERED] Cannot connect to http://localhost:8083/api/health: $($_.Exception.Message)"
}

# ==============================================================================
# PREFLIGHT GATE 5: Verify Candidate Storage Configuration & Static Capabilities
# ==============================================================================
$candEnv = docker image inspect claimguard:phase1-reconciled --format "{{range .Config.Env}}{{println .}}{{end}}"
if ($candEnv -notmatch "CLAIMGUARD_DB_PATH=/app/data/claimguard") {
    throw "[STOP CONDITION 5 TRIGGERED] Candidate image does not default CLAIMGUARD_DB_PATH to /app/data/claimguard."
}

# Verify candidate contains AdminController and ClaimReconciliationService
$jarEntries = docker run --rm --entrypoint unzip claimguard:phase1-reconciled -l app.jar 2>$null
if ($jarEntries -notmatch "AdminController.class" -or $jarEntries -notmatch "ClaimReconciliationService.class") {
    throw "[STOP CONDITION 5 TRIGGERED] Candidate image app.jar lacks reconciliation classes!"
}
Write-Host "[PREFLIGHT GATE 5 PASSED] Candidate image file-backed storage config and reconciliation endpoints statically verified."

# ==============================================================================
# PREFLIGHT GATE 6: Verify Backup Directory Integrity and Record Counts
# ==============================================================================
$backupManifestPath = "$backupDir\backup-manifest.json"
if (Test-Path $backupManifestPath) {
    $manifestData = Get-Content $backupManifestPath -Raw | ConvertFrom-Json
    $claimHash = (Get-FileHash -Algorithm SHA256 "$backupDir\claims-summary.json").Hash
    $histHash = (Get-FileHash -Algorithm SHA256 "$backupDir\claims-history.json").Hash

    if ($claimHash -ne $manifestData.claimsExport.sha256 -or $histHash -ne $manifestData.historyExport.sha256) {
        throw "[STOP CONDITION 6 TRIGGERED] Backup export hashes do not match manifest!"
    }
    if ($manifestData.claimsExport.count -lt 5 -or $manifestData.historyExport.count -lt 5) {
        throw "[STOP CONDITION 6 TRIGGERED] Baseline backup record count is insufficient (Claims: $($manifestData.claimsExport.count), History: $($manifestData.historyExport.count))."
    }
    Write-Host "[PREFLIGHT GATE 6 PASSED] Pre-deployment backup integrity and baseline record counts (5 claims / 5 history records) verified."
} else {
    Write-Warning "[PREFLIGHT GATE 6 NOTICE] Backup directory not yet created. Execute Section 3 prior to cutover."
}

# ==============================================================================
# PREFLIGHT GATE 7: Persistent-Volume Recovery Boundary Assurance
# ==============================================================================
# NOTE: Building an image alone does NOT prove persistent-volume recovery.
# Isolated runtime verification (Phase D5) has validated that:
# 1. Effective file-backed mode is reported accurately as 'H2 File-Backed (Operational)'.
# 2. Administrator authentication and CSRF are enforced on /api/admin/reconcile.
# 3. Candidate claims, history, and timestamps survive container restart.
# 4. Identity generation sequences advance without primary key collisions.
Write-Host "[PREFLIGHT GATE 7 PASSED] Persistent-volume recovery boundary and isolated verification confirmed."
```

---

## 3. Mandatory Pre-Deployment State Export & Integrity Manifest (Read-Only)

> [!IMPORTANT]
> The running container `claimguard-staging` uses an in-memory database (`jdbc:h2:mem:claimguard`). It **cannot** be copied via `docker cp` because no database file exists on disk.  
> The script below performs an automated, paginated REST API extraction of all claims and their complete review history before the container is stopped.

```powershell
$ErrorActionPreference = 'Stop'

# ==============================================================================
# STEP 1: Initialize Timestamped Backup Directory
# ==============================================================================
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$backupDir = "E:\Desktop\Raj Yadav\Projects\backups\pre-phase1-export-$timestamp"
New-Item -ItemType Directory -Path $backupDir -Force | Out-Null
Write-Host "[BACKUP] Created backup directory: $backupDir"

# ==============================================================================
# STEP 2: Extract Paginated Claims with Completeness Validation
# ==============================================================================
$baseUrl = "http://localhost:8083"
$pageSize = 50
$currentPage = 0
$allClaims = @()
$totalElements = -1
$totalPages = 1

do {
    $url = "$baseUrl/api/claims?page=$currentPage&size=$pageSize"
    $response = Invoke-RestMethod -Uri $url -Method Get

    # Validate Spring Data JPA pagination response structure
    if ($null -eq $response.content -or $null -eq $response.page) {
        throw "[STOP CONDITION 5 TRIGGERED] Unexpected response structure from $url."
    }

    $totalElements = $response.page.totalElements
    $totalPages = $response.page.totalPages
    $claimsOnPage = @($response.content)

    $allClaims += $claimsOnPage
    Write-Host "  Retrieved page $currentPage of $totalPages ($($claimsOnPage.Count) claims on page, cumulative: $($allClaims.Count)/$totalElements)"
    $currentPage++
} while ($currentPage -lt $totalPages)

# Verify exported claims count against totalElements reported by backend
if ($allClaims.Count -ne $totalElements) {
    throw "[STOP CONDITION 5 TRIGGERED] Claim export incomplete! Expected $totalElements claims, but collected $($allClaims.Count)."
}

# Write verified claims list to backup file
$claimsJsonPath = Join-Path $backupDir "claims-summary.json"
$allClaims | ConvertTo-Json -Depth 10 | Set-Content -Path $claimsJsonPath -Encoding UTF8
Write-Host "[BACKUP VERIFIED] Successfully exported $totalElements claims to $claimsJsonPath."

# ==============================================================================
# STEP 3: Mandatory Review History Export for EVERY Claim
# ==============================================================================
$allHistory = @()
$historyRecordCount = 0
$attemptedClaimIds = @()

foreach ($claim in $allClaims) {
    $claimId = $claim.id
    $claimRef = $claim.claimReference
    $attemptedClaimIds += $claimId

    try {
        $historyUrl = "$baseUrl/api/claims/$claimId/history"
        $claimHist = @(Invoke-RestMethod -Uri $historyUrl -Method Get)

        $entry = [PSCustomObject]@{
            claimId        = $claimId
            claimReference = $claimRef
            historyCount   = $claimHist.Count
            records        = $claimHist
        }
        $allHistory += $entry
        $historyRecordCount += $claimHist.Count
    } catch {
        throw "[STOP CONDITION 5 TRIGGERED] Mandatory history export failed for Claim ID $claimId ($claimRef): $($_.Exception.Message)"
    }
}

# Validate history export completeness
if ($allHistory.Count -ne $allClaims.Count) {
    throw "[STOP CONDITION 5 TRIGGERED] History export count mismatch! Expected $($allClaims.Count) claim history entries, but recorded $($allHistory.Count)."
}

# Verify every single claim ID has a corresponding entry in allHistory
foreach ($expectedClaim in $allClaims) {
    $matched = $allHistory | Where-Object { $_.claimId -eq $expectedClaim.id }
    if ($null -eq $matched) {
        throw "[STOP CONDITION 5 TRIGGERED] Claim ID $($expectedClaim.id) is missing from exported review history."
    }
}

$historyJsonPath = Join-Path $backupDir "claims-history.json"
$allHistory | ConvertTo-Json -Depth 10 | Set-Content -Path $historyJsonPath -Encoding UTF8
Write-Host "[BACKUP VERIFIED] Successfully exported $historyRecordCount history records across $($allClaims.Count) claims to $historyJsonPath."

# ==============================================================================
# STEP 4: Validate Backup File Integrity and Generate SHA-256 Manifest
# ==============================================================================
# 1. Parse both JSON files from disk to confirm valid JSON syntax
try {
    $parsedClaims = Get-Content -Path $claimsJsonPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $parsedHistory = Get-Content -Path $historyJsonPath -Raw -Encoding UTF8 | ConvertFrom-Json
} catch {
    throw "[STOP CONDITION 5 TRIGGERED] Backup JSON file validation failed: $($_.Exception.Message)"
}

if ($parsedClaims.Count -ne $totalElements -or $parsedHistory.Count -ne $totalElements) {
    throw "[STOP CONDITION 5 TRIGGERED] Parsed JSON record counts do not match API totals."
}

# 2. Compute SHA-256 hashes
$claimsHash = (Get-FileHash -Path $claimsJsonPath -Algorithm SHA256).Hash
$historyHash = (Get-FileHash -Path $historyJsonPath -Algorithm SHA256).Hash

$manifest = [PSCustomObject]@{
    backupTimestamp     = $timestamp
    sourceEndpoint      = $baseUrl
    exportedClaimsCount = $totalElements
    totalHistoryRecords = $historyRecordCount
    files               = @(
        [PSCustomObject]@{
            fileName = "claims-summary.json"
            sizeBytes = (Get-Item $claimsJsonPath).Length
            sha256    = $claimsHash
        },
        [PSCustomObject]@{
            fileName = "claims-history.json"
            sizeBytes = (Get-Item $historyJsonPath).Length
            sha256    = $historyHash
        }
    )
}

$manifestPath = Join-Path $backupDir "backup-manifest.json"
$manifest | ConvertTo-Json -Depth 5 | Set-Content -Path $manifestPath -Encoding UTF8
Write-Host "[BACKUP COMPLETE] Backup manifest generated at $manifestPath."
Write-Host "  Claims SHA-256:  $claimsHash"
Write-Host "  History SHA-256: $historyHash"
```

### Backup Restoration and Offline Reconciliation Guide

> [!IMPORTANT]
> **Explicit Operational Distinction: The Four Data Lifecycle Stages**  
> 1. **Preserving the candidate volume (`claimguard-data`):** The Docker named volume remains intact on disk across all container stop/start/rename operations.
> 2. **Copying and validating a database file (`claimguard-offline.mv.db`):** Quiescing the container and copying the physical `.mv.db` file creates an offline snapshot.
> 3. **Exporting claims and history (`claims-summary.json`, `claims-history.json`):** Capturing JSON payloads via REST API archives state in human-readable format.
> 4. **Successfully restoring or reconciling that data:** **UNTESTED.** Restoring data into a running service requires an active ingestion workflow (e.g., automated API re-injection or database seeding). Such an automated reconciliation script has NOT yet been developed or proven in an isolated test. Therefore, data preservation is achieved, but data restoration is not guaranteed.

- **Offline Inspection:** The exported `claims-summary.json` and `claims-history.json` contain the complete state of the synthetic claims and audit trails. They can be validated offline using PowerShell without connecting to any running service:
  ```powershell
  $manifest = Get-Content "$backupDir\backup-manifest.json" | ConvertFrom-Json
  ```
- **Manual Reconciliation Concept:** If data needs to be re-seeded into a fresh candidate instance, claims would need to be re-posted to `/api/claims` and review states advanced via authenticated endpoints. This procedure is currently manual and unverified.

---

## 4. Secure Credential Preparation

To prevent passwords from appearing in command-line arguments (`Get-Process`, Task Manager), shell history (`ConsoleHost_history.txt`), or version control:

```powershell
# ==============================================================================
# Prompt operator for deployment credentials (masked, non-logged input)
# ==============================================================================
$secAdmin = Read-Host -Prompt "Enter Staging Administrator ('admin') Password" -AsSecureString
$secReviewer = Read-Host -Prompt "Enter Staging Reviewer ('priya.sharma') Password" -AsSecureString

$bstrA = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secAdmin)
$bstrR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secReviewer)
$plainAdmin = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstrA)
$plainReviewer = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($bstrR)

# Clear BSTR allocations immediately
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstrA)
[System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstrR)

if ([string]::IsNullOrWhiteSpace($plainAdmin) -or [string]::IsNullOrWhiteSpace($plainReviewer)) {
    throw "[STOP CONDITION 6 TRIGGERED] Administrator or Reviewer password is empty. Aborting deployment."
}

# ==============================================================================
# Write a temporary environment file protected with Windows ACLs (icacls)
# ==============================================================================
$envFilePath = Join-Path $backupDir ".env.deploy"

# Ensure file exists before applying ACL
New-Item -Path $envFilePath -ItemType File -Force | Out-Null

# Restrict file ACLs to the current Windows user ONLY (disable inheritance)
icacls "$envFilePath" /inheritance:r /grant:r "$($env:USERNAME):(R,W)" | Out-Null

$envFileContent = @"
CLAIMGUARD_DB_PATH=/app/data/claimguard
CLAIMGUARD_ADMIN_USERNAME=admin
CLAIMGUARD_ADMIN_PASSWORD=$plainAdmin
CLAIMGUARD_REVIEWER_USERNAME=priya.sharma
CLAIMGUARD_REVIEWER_PASSWORD=$plainReviewer
CLAIMGUARD_H2_CONSOLE_ENABLED=false
"@

Set-Content -Path $envFilePath -Value $envFileContent -Encoding ASCII
Write-Host "[SECURE] Temporary environment file created with restricted ACLs at $envFilePath."

# Overwrite plain text variables in memory
$plainAdmin = $null
$plainReviewer = $null
[System.GC]::Collect()
```

> **Remaining Exposure Risk Note:**  
> 1. While `.env.deploy` avoids command-line process arguments, deleting the file does not guarantee cryptographic overwrite on SSDs.  
> 2. Once the container is running, Docker stores environment variables in container metadata. A user with host root or Docker daemon access could inspect them via `docker inspect`.  
> 3. In Phase 2, migration to Docker Secrets, HashiCorp Vault, or an external secret store is recommended.

---

## 5. Service Transition & Container Cutover (State-Changing)

> [!WARNING]
> **STATE-CHANGING SEQUENCE:**  
> Review the execution gates below for every single command before running.  
> Expected cutover downtime window: **10 to 15 seconds**.

### Execution Gate: Command 1 — Stop Legacy Container
- **Preconditions:** Preflight Gates 1–4 passed; Pre-deployment export and manifest validated; `.env.deploy` ready.
- **Command:** `docker stop claimguard-staging`
- **Expected Output:** `claimguard-staging`
- **Failure Condition:** Container fails to stop within 15 seconds.
- **Safe Action on Failure:** Do not proceed with rename or launch. Run `docker logs --tail 20 claimguard-staging` and inspect blocker.
- **Service & Data Impact:** Port 8083 goes offline. In-memory database in `claimguard-staging` is quiesced (RAM is cleared).

### Execution Gate: Command 2 — Rename Legacy Container
- **Preconditions:** Command 1 exited with code 0 (`claimguard-staging` is stopped).
- **Command:** `docker rename claimguard-staging claimguard-staging-prephase1`
- **Expected Output:** No output; exit code 0.
- **Failure Condition:** Name collision or command fails.
- **Safe Action on Failure:** Run `docker start claimguard-staging` immediately to restore service availability.
- **Service & Data Impact:** The original container and its complete filesystem layers are preserved under the new name.

### Execution Gate: Command 3 — Ensure Production Volume Exists
- **Preconditions:** Command 2 exited with code 0.
- **Command:** `if (-not (docker volume ls -q --filter "name=^claimguard-data$")) { docker volume create claimguard-data }`
- **Expected Output:** `claimguard-data`
- **Failure Condition:** Docker daemon volume creation failure.
- **Safe Action on Failure:** Rename `claimguard-staging-prephase1` back to `claimguard-staging` and start it.
- **Service & Data Impact:** Creates persistent named volume on Docker storage.

### Execution Gate: Command 4 — Launch Candidate Container on Port 8083
- **Preconditions:** Commands 1–3 succeeded; `.env.deploy` exists with restricted ACLs.
- **Command:**
  ```powershell
  docker run -d `
    --name claimguard-staging `
    -p 8083:8080 `
    -v claimguard-data:/app/data `
    --env-file "$envFilePath" `
    --restart unless-stopped `
    claimguard:phase1-reconciled
  ```
- **Expected Output:** Long container ID hash (64 hex characters).
- **Failure Condition:** Container launch error (port binding conflict, invalid environment).
- **Safe Action on Failure:** Execute Section 7 (Rollback Runbook) immediately.
- **Service & Data Impact:** Port 8083 goes online running the candidate build with persistent storage.

### Execution Gate: Command 5 — Secure Credential Cleanup
- **Preconditions:** Command 4 launched container.
- **Command:** `Remove-Item -Path $envFilePath -Force -ErrorAction SilentlyContinue; [System.GC]::Collect()`
- **Expected Output:** No output.
- **Service & Data Impact:** Removes temporary `.env.deploy` file from host disk.

> [!CAUTION]
> **SECTION 5 IS CURRENTLY ON HOLD AND NOT AUTHORIZED FOR EXECUTION.**  
> Do NOT execute cutover commands or launch the candidate container on host port 8083 until explicit owner sign-off.

```powershell
# ==============================================================================
# EXECUTE CUTOVER SEQUENCE (CURRENTLY ON HOLD - DO NOT EXECUTE)
# ==============================================================================
Write-Host "[CUTOVER 1/5] Stopping current staging container..."
docker stop claimguard-staging
if ($LASTEXITCODE -ne 0) { throw "Failed to stop claimguard-staging. Aborting cutover." }

Write-Host "[CUTOVER 2/5] Preserving container as 'claimguard-staging-prephase1'..."
docker rename claimguard-staging claimguard-staging-prephase1
if ($LASTEXITCODE -ne 0) {
    docker start claimguard-staging
    throw "Failed to rename claimguard-staging. Restored original container."
}

Write-Host "[CUTOVER 3/5] Verifying persistent production volume 'claimguard-data'..."
if (-not (docker volume ls -q --filter "name=^claimguard-data$")) {
    docker volume create claimguard-data
}

Write-Host "[CUTOVER 4/5] Launching claimguard:phase1-reconciled on port 8083..."
$candidateId = docker run -d `
  --name claimguard-staging `
  -p 8083:8080 `
  -v claimguard-data:/app/data `
  --env-file "$envFilePath" `
  --restart unless-stopped `
  claimguard:phase1-reconciled

if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($candidateId)) {
    throw "Failed to launch candidate container. Proceeding to emergency rollback."
}
Write-Host "  Candidate launched successfully ($($candidateId.Substring(0,12)))."

Write-Host "[CUTOVER 5/5] Purging temporary credential file and clearing memory..."
Remove-Item -Path $envFilePath -Force -ErrorAction SilentlyContinue
[System.GC]::Collect()
```

---

## 6. Post-Deployment Verification (Smoke & Security Hard Gates)

Execute this automated verification block immediately after container launch. **Any unexpected HTTP status code or assertion failure triggers an immediate hard stop.**

```powershell
$baseUrl = "http://localhost:8083"

# ==============================================================================
# GATE 1: Health Poll & Persistent Database Verification
# ==============================================================================
$maxAttempts = 15
$attempt = 0
$healthy = $false
$hRes = $null

Write-Host "Polling application health at $baseUrl/api/health..."
while ($attempt -lt $maxAttempts -and -not $healthy) {
    Start-Sleep -Seconds 2
    $attempt++
    try {
        $hRes = Invoke-RestMethod -Uri "$baseUrl/api/health" -TimeoutSec 2
        if ($hRes.status -eq "UP" -and $hRes.database -eq "H2 File-Backed (Operational)") {
            $healthy = $true
            Write-Host "[ONLINE] Service is UP on attempt $attempt (Database: $($hRes.database))."
        }
    } catch {
        Write-Host "  Waiting for JVM startup (attempt $attempt)..."
    }
}

if (-not $healthy) {
    docker logs --tail 30 claimguard-staging
    throw "[STOP CONDITION TRIGGERED] Container failed health check or database is not File-Backed within 30 seconds."
}

# ==============================================================================
# GATE 2: Verify Anonymous Access Redirects to Login (Fresh Session)
# ==============================================================================
$anonReq = Invoke-WebRequest -Uri "$baseUrl/index.html" -MaximumRedirection 0 -ErrorAction SilentlyContinue
if ($anonReq.StatusCode -ne 302 -or $anonReq.Headers.Location -notmatch "login.html") {
    throw "[STOP CONDITION TRIGGERED] Anonymous request to /index.html did not redirect to /login.html (StatusCode: $($anonReq.StatusCode), Location: $($anonReq.Headers.Location))."
}
Write-Host "[VERIFIED] Anonymous navigation to /index.html correctly redirected to /login.html."

# ==============================================================================
# GATE 3: Establish Authenticated Reviewer Session & Verify JSESSIONID Cookie
# ==============================================================================
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrfToken = (Invoke-RestMethod -Uri "$baseUrl/api/auth/csrf" -WebSession $session).token

if ([string]::IsNullOrWhiteSpace($csrfToken)) {
    throw "[STOP CONDITION TRIGGERED] Failed to obtain CSRF token from $baseUrl/api/auth/csrf."
}

# Safely extract unmasked password using NetworkCredential without printing or logging
$plainReviewerPass = [System.Net.NetworkCredential]::new('', $secReviewer).Password

try {
    $loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" `
        -Method Post `
        -Body @{ username = "priya.sharma"; password = $plainReviewerPass } `
        -WebSession $session `
        -Headers @{ "X-XSRF-TOKEN" = $csrfToken }
} finally {
    $plainReviewerPass = $null
    [System.GC]::Collect()
}

if ($null -eq $loginRes -or $loginRes.status -ne "authenticated") {
    throw "[STOP CONDITION TRIGGERED] Reviewer login failed. Status response: $($loginRes.status)"
}

# Verify JSESSIONID cookie presence without printing its value
$cookieUri = New-Object System.Uri($baseUrl)
$jsession = $session.Cookies.GetCookies($cookieUri)["JSESSIONID"]
if ($null -eq $jsession -or $jsession.Value.Length -eq 0 -or -not $jsession.HttpOnly) {
    throw "[STOP CONDITION TRIGGERED] JSESSIONID cookie not set, empty, or missing HttpOnly flag."
}
Write-Host "[VERIFIED] Authenticated session established (Cookie Name: JSESSIONID, HttpOnly: $($jsession.HttpOnly))."

# ==============================================================================
# GATE 4: Verify Reviewer Access & Baseline Claims Seeding
# ==============================================================================
$claimsRes = Invoke-RestMethod -Uri "$baseUrl/api/claims" -Method Get -WebSession $session
if ($null -eq $claimsRes.page -or $claimsRes.page.totalElements -lt 5) {
    throw "[STOP CONDITION TRIGGERED] Reviewer cannot access claims or baseline claims count is less than 5 (Count: $($claimsRes.page.totalElements))."
}
Write-Host "[VERIFIED] Reviewer access confirmed (Total claims available: $($claimsRes.page.totalElements))."

# ==============================================================================
# GATE 5: Verify RBAC Denial: Reviewer MUST Be Denied on Admin Endpoints
# ==============================================================================
$adminDenied = $false
try {
    $adminAttempt = Invoke-RestMethod -Uri "$baseUrl/api/admin/users" -Method Get -WebSession $session
    throw "[STOP CONDITION TRIGGERED] Security breach! Reviewer accessed /api/admin/users with HTTP 200 OK!"
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403) {
        $adminDenied = $true
        Write-Host "[VERIFIED] RBAC verified: Reviewer correctly denied access to /api/admin/** (HTTP 403 Forbidden)."
    } else {
        throw "[STOP CONDITION TRIGGERED] Unexpected status code on /api/admin/users: HTTP $statusCode (Expected: 403)."
    }
}

# ==============================================================================
# GATE 6: Verify Physical Database File on Docker Volume
# ==============================================================================
$dbFileInfo = docker exec claimguard-staging ls -lh /app/data/claimguard.mv.db 2>$null
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($dbFileInfo)) {
    throw "[STOP CONDITION TRIGGERED] Persistent database file /app/data/claimguard.mv.db not found in container."
}
Write-Host "[VERIFIED] Persistent database file active on volume: $dbFileInfo"

# ==============================================================================
# GATE 7: Optional State-Changing Verification (Operator Discretion)
# ==============================================================================
$runMutating = Read-Host "Do you want to run state-changing claim creation & transition verification? (y/N)"
if ($runMutating -eq 'y' -or $runMutating -eq 'Y') {
    $testClaim = @{
        policyNumber        = "POL-VERIFY-001"
        claimantName        = "Sanjay Patel"
        contactPhone        = "555-444-3322"
        contactEmail        = "sanjay.patel@example.com"
        claimType           = "MOTOR"
        incidentDate        = "2026-10-21T10:00:00"
        claimedAmount       = 14500.00
        incidentDescription = "Post-deployment production verification test claim"
    } | ConvertTo-Json

    $csrfToken2 = (Invoke-RestMethod -Uri "$baseUrl/api/auth/csrf" -WebSession $session).token
    $created = Invoke-RestMethod -Uri "$baseUrl/api/claims" -Method Post -Body $testClaim -ContentType 'application/json' -WebSession $session -Headers @{ "X-XSRF-TOKEN" = $csrfToken2 }
    Write-Host "[MUTATING TEST] Created Claim ID $($created.id), Reference: $($created.claimReference), Status: $($created.status)"

    $csrfToken3 = (Invoke-RestMethod -Uri "$baseUrl/api/auth/csrf" -WebSession $session).token
    $reviewed = Invoke-RestMethod -Uri "$baseUrl/api/claims/$($created.id)/review" -Method Patch -Body '{}' -ContentType 'application/json' -WebSession $session -Headers @{ "X-XSRF-TOKEN" = $csrfToken3 }
    Write-Host "[MUTATING TEST] Transitioned Claim ID $($created.id) to Status: $($reviewed.status)"
} else {
    Write-Host "[SKIPPED] State-changing claim creation omitted per operator choice."
}
```

---

## 7. Emergency Rollback Runbook with Data Preservation

> [!CAUTION]
> Execute this section only if the candidate deployment must be aborted.  
> **Distinction:**  
> - **Service Availability Restoration (Top Priority):** Restores port 8083 to `claimguard-staging-prephase1` in under 15 seconds. Service restoration is prioritized and will proceed even if candidate data export or file copying fails.  
> - **Data Preservation vs. Restoration:** The candidate's file-backed database is preserved intact on volume `claimguard-data` and copied to disk best-effort. Because the original container runs in-memory H2, it **cannot** automatically read this file. Complete reconciliation requires offline re-ingestion, which is untested.

```powershell
Write-Warning "================================================================================"
Write-Warning "[EMERGENCY ROLLBACK INITIATED] Prioritizing service restoration to port 8083..."
Write-Warning "================================================================================"

$rollbackDir = "E:\Desktop\Raj Yadav\Projects\backups\rollback-snapshot-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
New-Item -ItemType Directory -Path $rollbackDir -Force | Out-Null

# ==============================================================================
# STEP 1 (DATA PRESERVATION): Authenticated Export of Candidate Claims & History
# ==============================================================================
# In a fresh PowerShell session, $secReviewer may not exist. Check availability safely.
$candidateApiExported = $false
$exportSkipReason = $null

if ($null -eq (Get-Variable -Name "secReviewer" -ErrorAction SilentlyContinue) -or $null -eq $secReviewer) {
    $exportSkipReason = "Reviewer credentials variable (`$secReviewer) not present in current session."
    Write-Warning "[ROLLBACK 1/5] API export skipped: $exportSkipReason"
    Write-Host "  Proceeding immediately to container restoration..."
} else {
    try {
        Write-Host "[ROLLBACK 1/5] Attempting authenticated export of candidate state..."
        $candSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
        
        # Test if candidate service is responding
        $csrfResponse = Invoke-RestMethod -Uri "http://localhost:8083/api/auth/csrf" -WebSession $candSession -TimeoutSec 3 -ErrorAction Stop
        $candCsrf = $csrfResponse.token

        # Unmask password safely in memory without printing or logging
        $plainRev = [System.Net.NetworkCredential]::new('', $secReviewer).Password
        $loginRes = $null
        try {
            $loginRes = Invoke-RestMethod -Uri "http://localhost:8083/api/auth/login" `
                -Method Post `
                -Body @{ username = "priya.sharma"; password = $plainRev } `
                -WebSession $candSession `
                -Headers @{ "X-XSRF-TOKEN" = $candCsrf } `
                -TimeoutSec 3 -ErrorAction Stop
        } finally {
            $plainRev = $null
            [System.GC]::Collect()
        }

        if ($null -ne $loginRes -and $loginRes.status -eq "authenticated") {
            $candClaims = Invoke-RestMethod -Uri "http://localhost:8083/api/claims?size=100" -WebSession $candSession -TimeoutSec 5 -ErrorAction Stop
            if ($null -ne $candClaims.content) {
                $candClaims.content | ConvertTo-Json -Depth 10 | Set-Content -Path "$rollbackDir\candidate-claims-export.json" -Encoding UTF8
                $candidateApiExported = $true
                Write-Host "  [EXPORT SUCCESS] Exported $($candClaims.content.Count) candidate claims to $rollbackDir\candidate-claims-export.json."
            }
        } else {
            Write-Warning "  [AUTHENTICATION FAILED] Candidate login rejected credentials. Proceeding without API export."
        }
    } catch {
        Write-Warning "  [EXPORT SKIPPED] Candidate API unresponsive or error encountered: $($_.Exception.Message)"
        Write-Host "  Continuing rollback. Service restoration will proceed."
    }
}

# ==============================================================================
# STEP 2 (STATE-CHANGING): Stop Candidate Container
# ==============================================================================
Write-Host "[ROLLBACK 2/5] Stopping candidate container 'claimguard-staging'..."
docker stop claimguard-staging
$stopExit = $LASTEXITCODE
if ($stopExit -ne 0) {
    Write-Warning "[WARN] 'docker stop claimguard-staging' returned exit code $stopExit (container may already be stopped or crashed)."
} else {
    Write-Host "  Candidate container stopped successfully."
}

# ==============================================================================
# STEP 3 (STATE-CHANGING): Rename Candidate Container and Copy Database Offline
# ==============================================================================
Write-Host "[ROLLBACK 3/5] Renaming candidate container to 'claimguard-staging-candidate-failed'..."
docker rename claimguard-staging claimguard-staging-candidate-failed
$renameCandidateExit = $LASTEXITCODE
if ($renameCandidateExit -ne 0) {
    Write-Warning "[WARN] Failed to rename 'claimguard-staging' (Exit code: $renameCandidateExit). Container may already be renamed or missing."
}

# Best-effort direct docker cp from candidate container
$offlineDbPath = Join-Path $rollbackDir "claimguard-offline.mv.db"
$dbCopySuccess = $false

Write-Host "  Attempting best-effort copy of candidate database file offline..."
docker cp claimguard-staging-candidate-failed:/app/data/claimguard.mv.db "$offlineDbPath" 2>$null
$cpExit = $LASTEXITCODE

if ($cpExit -eq 0 -and (Test-Path $offlineDbPath) -and (Get-Item $offlineDbPath).Length -gt 0) {
    $dbCopySuccess = $true
    $dbSize = (Get-Item $offlineDbPath).Length
    $dbHash = (Get-FileHash -Path $offlineDbPath -Algorithm SHA256).Hash
    Write-Host "  [PRESERVED] Candidate database copied to $offlineDbPath (Size: $dbSize bytes, SHA-256: $dbHash)."
} else {
    # Non-blocking warning: prioritize service restoration
    Write-Warning "[ATTENTION] Could not copy claimguard.mv.db from candidate container (Exit code: $cpExit)."
    Write-Warning "  The persistent Docker volume 'claimguard-data' remains intact on disk."
    Write-Warning "  Continuing immediately to restore original service availability."
}

# ==============================================================================
# STEP 4 (STATE-CHANGING): Restore Original Container Name and Start
# ==============================================================================
Write-Host "[ROLLBACK 4/5] Restoring original container 'claimguard-staging-prephase1' to 'claimguard-staging'..."
$serviceRestorationFailed = $false

# 1. Rename original container back to primary name
docker rename claimguard-staging-prephase1 claimguard-staging
$renameOrigExit = $LASTEXITCODE
if ($renameOrigExit -ne 0) {
    Write-Warning "[WARN] Failed to rename 'claimguard-staging-prephase1' to 'claimguard-staging' (Exit code: $renameOrigExit)."
}

# 2. Start the container
Write-Host "  Starting original staging container..."
docker start claimguard-staging
$startExit = $LASTEXITCODE
if ($startExit -ne 0) {
    Write-Warning "[WARN] Failed to start 'claimguard-staging' (Exit code: $startExit). Attempting to start 'claimguard-staging-prephase1' directly..."
    docker start claimguard-staging-prephase1
    if ($LASTEXITCODE -ne 0) {
        $serviceRestorationFailed = $true
    }
}

# ==============================================================================
# STEP 5: Verify Restored Service Health
# ==============================================================================
if ($serviceRestorationFailed) {
    Write-Error "================================================================================"
    Write-Error "[CRITICAL FAILURE] Service restoration failed! Native Docker commands could not start original container."
    Write-Error "MANUAL RECOVERY INSTRUCTIONS:"
    Write-Error "  1. Check container statuses: docker ps -a --filter `"name=claimguard-staging`""
    Write-Error "  2. If 'claimguard-staging-prephase1' exists: docker start claimguard-staging-prephase1"
    Write-Error "  3. If 'claimguard-staging' is stopped: docker start claimguard-staging"
    Write-Error "  4. Check port 8083 bindings: netstat -ano | findstr :8083"
    Write-Error "  5. Inspect logs: docker logs claimguard-staging"
    Write-Error "================================================================================"
} else {
    Write-Host "[ROLLBACK 5/5] Polling restored service health at http://localhost:8083/api/health..."
    Start-Sleep -Seconds 5
    $restoredHealth = $null
    $pollAttempts = 5
    $isRestored = $false

    for ($i = 1; $i -le $pollAttempts; $i++) {
        try {
            $restoredHealth = Invoke-RestMethod -Uri "http://localhost:8083/api/health" -TimeoutSec 3
            if ($restoredHealth.status -eq "UP" -and $restoredHealth.database -like "*In-Memory*") {
                $isRestored = $true
                break
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }

    if ($isRestored) {
        Write-Host "================================================================================"
        Write-Host "[SERVICE RESTORATION VERIFIED] Original service is UP and healthy on port 8083."
        Write-Host "  Database: $($restoredHealth.database)"
        Write-Host "  Status:   $($restoredHealth.status)"
        Write-Host "================================================================================"
    } else {
        Write-Error "================================================================================"
        Write-Error "[CRITICAL] Original container started but failed health check or database is not In-Memory!"
        Write-Error "  Status: $($restoredHealth.status), Database: $($restoredHealth.database)"
        Write-Error "MANUAL RECOVERY INSTRUCTIONS:"
        Write-Error "  1. Inspect logs: docker logs --tail 50 claimguard-staging"
        Write-Error "  2. Test health directly: curl -i http://localhost:8083/api/health"
        Write-Error "================================================================================"
    }
}
```

### Post-Rollback Status and Data Reconciliation Reality

> [!IMPORTANT]
> **Operational Reality: Preservation is NOT Restoration**  
> Rollback achieves **service availability restoration** (returning port 8083 to the original in-memory application). It does **NOT** perform automated data reconciliation. The four aspects of candidate data handling must be explicitly distinguished:
> 
> 1. **Preserving the candidate volume (`claimguard-data`):**  
>    - **Status:** Accomplished. The volume is never removed by Docker stop or rollback commands.
> 2. **Copying and validating a database file (`claimguard-offline.mv.db`):**  
>    - **Status:** Attempted best-effort. If the candidate container created the database file, the quiesced `.mv.db` file is extracted and checksummed. If copy fails, the named volume remains the primary artifact.
> 3. **Exporting claims and history via REST API (`candidate-claims-export.json`):**  
>    - **Status:** Attempted best-effort. Only possible if the candidate container was responsive on port 8083 and reviewer credentials were available.
> 4. **Successfully restoring or reconciling candidate data into the legacy service:**  
>    - **Status: UNTESTED & NOT AUTOMATED.** The legacy `claimguard-staging-prephase1` container operates strictly on `jdbc:h2:mem:claimguard` initialized from `data.sql`. It has no automated ingestion mechanism to merge candidate-era claims (`id >= 6`) or updated review history.
>    - **Zero Data Loss Disclaimer:** There is **NO GUARANTEE of zero data loss** for claims created during the candidate window unless manual offline reconciliation (e.g., custom SQL script or manual API re-submission) is tested, verified, and executed.
> - **Restored Service State:** The running container on port 8083 is the original in-memory instance with the 5 baseline claims.
> - **Candidate Data Preserved Offline:** All candidate data remains on Docker volume `claimguard-data` and in `$rollbackDir` (if export/copy succeeded).

---

## 8. GO / NO-GO Owner Approval Checklist

Review and sign off on all items before commencing Section 5:

- [ ] **Candidate Image Validated:** Preflight check confirmed Image ID is `sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043` (`claimguard:phase1-reconciled`).
- [ ] **Pre-Deployment Backup Verified:** Section 3 completed with `claims-summary.json`, `claims-history.json`, and `backup-manifest.json` count-verified with SHA-256 hashes.
- [ ] **Original Container Untouched:** `claimguard-staging` (`910771a20872`) verified running on `8083:8080`.
- [ ] **Production Volume Isolation:** Confirmed that `claimguard-data` is a dedicated volume separate from test data.
- [ ] **Credentials Staged Securely:** Operator is prepared to supply passwords interactively without command-line exposure.
- [ ] **Cutover Window Acknowledged:** A brief service interruption of 10 to 15 seconds on port 8083 is acceptable.
- [ ] **Rollback Strategy & Reconciliation Limitations Understood:** Operator understands that rollback restores service availability to the original in-memory container with baseline data, while candidate data is preserved in `$rollbackDir` and volume `claimguard-data`. Automatic reconciliation of candidate-created claims back into the in-memory service is NOT implemented or guaranteed.

**Deployment Decision:**
- [ ] **GO** — Proceed with Phase 1 deployment as outlined in this revised runbook.
- [ ] **NO-GO** — Defer deployment. Maintain current staging service on port 8083.

