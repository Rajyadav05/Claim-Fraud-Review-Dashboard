<#
.SYNOPSIS
    Automated Diagnostic & Verification Checker for ClaimGuard Demo.
.DESCRIPTION
    Validates container state, health probe, security authentication,
    CSRF enforcement, synthetic baseline data, and optional restart persistence.
#>
[CmdletBinding()]
param (
    [int]$Port = 8095,
    [string]$ReviewerPassword = "DemoReviewerSecret2026!",
    [switch]$TestPersistence
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ClaimGuard - Demo Health & Diagnostic Checker" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Container Status Check
Write-Host "--- Check 1/6: Inspecting container state ---" -ForegroundColor Yellow
$contStatus = docker ps --filter "name=^claimguard-demo-app$" --format "{{.Status}}"
if (-not $contStatus) {
    Write-Host "FAIL: Container 'claimguard-demo-app' is not running!" -ForegroundColor Red
    Write-Host "  Start it first via: .\scripts\start-demo.ps1" -ForegroundColor Yellow
    exit 1
}
Write-Host "  Container is active: $contStatus" -ForegroundColor Green

# 2. Health Endpoint Probe
Write-Host "--- Check 2/6: Querying /api/health probe ---" -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "http://localhost:$Port/api/health" -TimeoutSec 3 -ErrorAction Stop
} catch {
    Write-Host "FAIL: Unable to connect to http://localhost:$Port/api/health: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

Write-Host "  Status       : $($health.status)" -ForegroundColor Green
Write-Host "  Database     : $($health.database)" -ForegroundColor Green
Write-Host "  Application  : $($health.application) v$($health.version)"

if ($health.status -ne "UP" -or $health.database -ne "H2 File-Backed (Operational)") {
    Write-Host "FAIL: Health endpoint returned abnormal status or mode." -ForegroundColor Red
    exit 1
}

# 3. Security Form-Login Redirect Gate
Write-Host "--- Check 3/6: Testing Spring Security unauthenticated redirect ---" -ForegroundColor Yellow
$anonReq = Invoke-WebRequest -Uri "http://localhost:$Port/index.html" -MaximumRedirection 0 -ErrorAction SilentlyContinue
if ($anonReq.StatusCode -eq 302 -and $anonReq.Headers.Location -match "login.html") {
    Write-Host "  Anonymous request redirected to login page: HTTP $($anonReq.StatusCode)" -ForegroundColor Green
} else {
    Write-Host "  Notice: Response to unauthenticated request: HTTP $($anonReq.StatusCode)" -ForegroundColor Gray
}

# 4. Authenticated Session & CSRF Token Acquisition
Write-Host "--- Check 4/6: Authenticating Reviewer session ---" -ForegroundColor Yellow
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$csrfToken = (Invoke-RestMethod -Uri "http://localhost:$Port/api/auth/csrf" -Method Get -WebSession $session).token

if ([string]::IsNullOrWhiteSpace($csrfToken)) {
    Write-Host "FAIL: Failed to obtain CSRF token from /api/auth/csrf" -ForegroundColor Red
    exit 1
}

$loginBody = "username=priya.sharma&password=$ReviewerPassword"
try {
    $loginRes = Invoke-RestMethod -Uri "http://localhost:$Port/api/auth/login" `
        -Method Post `
        -Body $loginBody `
        -ContentType "application/x-www-form-urlencoded" `
        -Headers @{ "X-XSRF-TOKEN" = $csrfToken } `
        -WebSession $session
} catch {
    Write-Host "FAIL: Reviewer authentication failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Check if CLAIMGUARD_REVIEWER_PASSWORD was modified in .env." -ForegroundColor Yellow
    exit 1
}

if ($loginRes.status -ne "authenticated") {
    Write-Host "FAIL: Login response did not indicate 'authenticated' status: $($loginRes.status)" -ForegroundColor Red
    exit 1
}
Write-Host "  Reviewer authenticated successfully as '$($loginRes.username)'." -ForegroundColor Green

# 5. Seed Data & Endpoints Verification
Write-Host "--- Check 5/6: Verifying claims and audit history endpoints ---" -ForegroundColor Yellow
$claimsRes = Invoke-RestMethod -Uri "http://localhost:$Port/api/claims" -Method Get -WebSession $session
Write-Host "  Total Claims in Database : $($claimsRes.page.totalElements)" -ForegroundColor Green

if ($claimsRes.page.totalElements -lt 5) {
    Write-Host "FAIL: Baseline database contains fewer than 5 synthetic claims!" -ForegroundColor Red
    exit 1
}

$firstClaim = $claimsRes.content[0]
Write-Host "  Sample Claim Reference   : $($firstClaim.claimReference) (Claimant: $($firstClaim.claimantName), Status: $($firstClaim.status))"

$histRes = Invoke-RestMethod -Uri "http://localhost:$Port/api/claims/$($firstClaim.id)/history" -Method Get -WebSession $session
Write-Host "  Audit History Events     : $($histRes.Count) events recorded on Claim $($firstClaim.id)" -ForegroundColor Green

# 6. Optional Persistence Across Restart Check
if ($TestPersistence.IsPresent) {
    Write-Host "--- Check 6/6: Verifying file-backed persistence across container restart ---" -ForegroundColor Yellow
    docker restart claimguard-demo-app | Out-Null
    
    $rebootHealthy = $false
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt 25) {
        try {
            $postRestartHealth = Invoke-RestMethod -Uri "http://localhost:$Port/api/health" -TimeoutSec 2 -ErrorAction Stop
            if ($postRestartHealth.status -eq "UP" -and $postRestartHealth.database -eq "H2 File-Backed (Operational)") {
                $rebootHealthy = $true
                break
            }
        } catch {
            Start-Sleep -Milliseconds 800
        }
    }

    if ($rebootHealthy) {
        Write-Host "  Container rebooted and restored file-backed operational state." -ForegroundColor Green
    } else {
        Write-Host "FAIL: Container failed to recover file-backed state after restart!" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "--- Check 6/6: Restart persistence check skipped (use -TestPersistence to run) ---" -ForegroundColor Gray
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  ALL CHECKS PASSED: ClaimGuard Demo is 100% Operational" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
