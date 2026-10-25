<#
.SYNOPSIS
    Imports the offline ClaimGuard Docker image archive on the target demo laptop.
.DESCRIPTION
    Loads the tar archive using 'docker load' and validates that the resulting image ID
    matches the verified candidate hash sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043.
#>
[CmdletBinding()]
param (
    [string]$ImagePath = "claimguard-phase1-reconciled.tar",
    [string]$ExpectedImageId = "sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043"
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ClaimGuard - Offline Docker Image Importer" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Verify Archive Exists
if (-not (Test-Path $ImagePath)) {
    Write-Host "ERROR: Image archive not found at: $ImagePath" -ForegroundColor Red
    Write-Host "  Ensure 'claimguard-phase1-reconciled.tar' is located in the specified folder." -ForegroundColor Yellow
    exit 1
}

# 2. Verify SHA-256 Checksum if manifest exists
$manifestPath = "$ImagePath.manifest.json"
if (Test-Path $manifestPath) {
    Write-Host "--- Step 1/3: Verifying archive checksum against manifest ---" -ForegroundColor Yellow
    $manifestData = Get-Content $manifestPath -Raw | ConvertFrom-Json
    $calculatedHash = (Get-FileHash -Path $ImagePath -Algorithm SHA256).Hash
    if ($calculatedHash -ne $manifestData.SHA256) {
        Write-Host "ERROR: Checksum verification failed! File may be incomplete or corrupted." -ForegroundColor Red
        Write-Host "  Expected : $($manifestData.SHA256)" -ForegroundColor Red
        Write-Host "  Actual   : $calculatedHash" -ForegroundColor Red
        exit 1
    }
    Write-Host "  Checksum verified: $calculatedHash" -ForegroundColor Green
} else {
    Write-Host "--- Step 1/3: Manifest not found. Skipping archive pre-check ---" -ForegroundColor Gray
}

# 3. Load Image into Docker
Write-Host "--- Step 2/3: Loading image into local Docker Engine ---" -ForegroundColor Yellow
docker load -i $ImagePath
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: 'docker load' failed. Ensure Docker Desktop is running." -ForegroundColor Red
    exit 1
}

# 4. Verify Loaded Image Identity
Write-Host "--- Step 3/3: Inspecting loaded image ---" -ForegroundColor Yellow
$loadedId = docker image inspect claimguard:phase1-reconciled --format "{{.Id}}" 2>$null
if ($null -eq $loadedId -or $loadedId -ne $ExpectedImageId) {
    Write-Host "ERROR: Loaded image ID mismatch!" -ForegroundColor Red
    Write-Host "  Expected : $ExpectedImageId" -ForegroundColor Red
    Write-Host "  Actual   : $loadedId" -ForegroundColor Red
    exit 1
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  IMAGE LOADED SUCCESSFULLY: claimguard:phase1-reconciled" -ForegroundColor Green
Write-Host "  Image ID : $loadedId"
Write-Host "  You can now start the demo via: .\scripts\start-demo.ps1"
Write-Host "============================================================" -ForegroundColor Cyan
