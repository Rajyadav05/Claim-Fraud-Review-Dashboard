<#
.SYNOPSIS
    Starts the ClaimGuard Fraud Review Dashboard standalone college demo container.
.DESCRIPTION
    Performs safety pre-flight checks (Docker availability, image integrity, port conflicts),
    creates the dedicated persistent demo volume 'claimguard-demo-data', starts container
    'claimguard-demo-app' on port 8095 (or custom port), polls /api/health until UP, and
    displays login credentials.
#>
[CmdletBinding()]
param (
    [int]$Port = 8095,
    [string]$ImageTag = "claimguard:phase1-reconciled",
    [string]$ExpectedImageId = "sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043",
    [string]$AdminPassword = "DemoAdminSecret2026!",
    [string]$ReviewerPassword = "DemoReviewerSecret2026!",
    [switch]$ForceRecreate
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ClaimGuard - Standalone College Demo Launcher" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Verify Docker Availability
Write-Host "--- Pre-flight 1/5: Checking Docker engine status ---" -ForegroundColor Yellow
$dockerCmd = Get-Command "docker" -ErrorAction SilentlyContinue
if (-not $dockerCmd) {
    Write-Host "ERROR: 'docker' command not found on PATH. Please install or start Docker Desktop." -ForegroundColor Red
    exit 1
}

docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Docker engine is not running or unreachable. Please start Docker Desktop and retry." -ForegroundColor Red
    exit 1
}
Write-Host "  Docker engine is operational." -ForegroundColor Green

# 2. Check Candidate Image & SHA-256 Identity
Write-Host "--- Pre-flight 2/5: Validating image $ImageTag ---" -ForegroundColor Yellow
$actualId = docker image inspect $ImageTag --format "{{.Id}}" 2>$null
if ($null -eq $actualId -or [string]::IsNullOrWhiteSpace($actualId)) {
    Write-Host "ERROR: Docker image '$ImageTag' not found locally!" -ForegroundColor Red
    Write-Host "  To import the offline image archive, run:" -ForegroundColor Yellow
    Write-Host "    .\scripts\import-image.ps1 -ImagePath .\claimguard-phase1-reconciled.tar" -ForegroundColor Yellow
    exit 1
}

if ($actualId -ne $ExpectedImageId) {
    Write-Host "ERROR: Image ID mismatch! Target image has been modified or corrupted." -ForegroundColor Red
    Write-Host "  Expected : $ExpectedImageId" -ForegroundColor Red
    Write-Host "  Actual   : $actualId" -ForegroundColor Red
    exit 1
}
Write-Host "  Candidate image verified: $actualId" -ForegroundColor Green

# 3. Check Port Availability
Write-Host "--- Pre-flight 3/5: Verifying host port $Port availability ---" -ForegroundColor Yellow
$portCheck = Test-NetConnection -ComputerName localhost -Port $Port -WarningAction SilentlyContinue
if ($portCheck.TcpTestSucceeded) {
    $isDemoApp = docker ps --filter "name=^claimguard-demo-app$" --filter "publish=$Port" -q
    if ($isDemoApp -and -not $ForceRecreate.IsPresent) {
        Write-Host "  Container 'claimguard-demo-app' is already running and listening on port $Port." -ForegroundColor Green
        Write-Host "  Testing health endpoint..."
        $health = Invoke-RestMethod -Uri "http://localhost:$Port/api/health" -TimeoutSec 3 -ErrorAction SilentlyContinue
        if ($health.status -eq "UP") {
            Write-Host "  Application is UP and operational." -ForegroundColor Green
            Write-Host "`nAccess the demo dashboard at: http://localhost:$Port/" -ForegroundColor Cyan
            exit 0
        }
    }

    Write-Host "ERROR: Port $Port is currently occupied by another process!" -ForegroundColor Red
    Write-Host "  Specify an alternate available port via: .\start-demo.ps1 -Port 8096" -ForegroundColor Yellow
    exit 1
}
Write-Host "  Port $Port is free." -ForegroundColor Green

# 4. Manage Container Lifecycle
Write-Host "--- Pre-flight 4/5: Preparing demo container and volume ---" -ForegroundColor Yellow
$contName = "claimguard-demo-app"
$volName = "claimguard-demo-data"

$existingCont = docker ps -a -q --filter "name=^$contName$"
if ($existingCont) {
    Write-Host "  Removing previous demo container '$contName'..."
    docker rm -f $contName | Out-Null
}

$existingVol = docker volume ls -q --filter "name=^$volName$"
if (-not $existingVol) {
    Write-Host "  Creating persistent demo volume '$volName'..."
    docker volume create $volName | Out-Null
} else {
    Write-Host "  Preserving existing demo volume '$volName' for data durability." -ForegroundColor Gray
}

# 5. Launch Container
Write-Host "--- Launching $contName on port $Port ---" -ForegroundColor Yellow
$runId = docker run -d --name $contName `
    -p "${Port}:8080" `
    -v "${volName}:/app/data" `
    -e "CLAIMGUARD_DB_PATH=/app/data/claimguard" `
    -e "CLAIMGUARD_ADMIN_USERNAME=admin" `
    -e "CLAIMGUARD_ADMIN_PASSWORD=$AdminPassword" `
    -e "CLAIMGUARD_REVIEWER_USERNAME=priya.sharma" `
    -e "CLAIMGUARD_REVIEWER_PASSWORD=$ReviewerPassword" `
    $ImageTag

Write-Host "  Container launched with ID: $runId" -ForegroundColor Green

# 6. Poll Health Endpoint
Write-Host "--- Waiting for application to initialize ---" -ForegroundColor Yellow
$maxAttempts = 35
$isHealthy = $false
$sw = [System.Diagnostics.Stopwatch]::StartNew()

while ($sw.Elapsed.TotalSeconds -lt $maxAttempts) {
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:$Port/api/health" -TimeoutSec 2 -ErrorAction Stop
        if ($health.status -eq "UP") {
            $isHealthy = $true
            break
        }
    } catch {
        Start-Sleep -Milliseconds 800
    }
}

if (-not $isHealthy) {
    Write-Host "ERROR: Application failed to report healthy status within $maxAttempts seconds." -ForegroundColor Red
    Write-Host "  Container logs:" -ForegroundColor Yellow
    docker logs --tail 30 $contName
    exit 1
}

Write-Host "  Health status: $($health.status) | Mode: $($health.database)" -ForegroundColor Green

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  ClaimGuard Demo Application is Live!" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Web Dashboard       : http://localhost:$Port/"
Write-Host "  Health Probe        : http://localhost:$Port/api/health"
Write-Host "  All Claims Directory: http://localhost:$Port/claims.html"
Write-Host "  Submit Claim Intake : http://localhost:$Port/submit.html"
Write-Host "  Review Console      : http://localhost:$Port/review.html?id=1"
Write-Host "------------------------------------------------------------"
Write-Host "  DEMO CREDENTIALS:"
Write-Host "    Reviewer Account  : priya.sharma / $ReviewerPassword"
Write-Host "    Admin Account     : admin / $AdminPassword"
Write-Host "============================================================" -ForegroundColor Cyan
