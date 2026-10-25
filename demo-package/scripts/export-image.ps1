<#
.SYNOPSIS
    Exports the verified ClaimGuard candidate Docker image to an offline tar archive.
.DESCRIPTION
    Validates candidate image ID against sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043,
    executes 'docker save', and generates a SHA-256 checksum manifest for offline transfer.
#>
[CmdletBinding()]
param (
    [string]$OutputFile = "claimguard-phase1-reconciled.tar",
    [string]$ImageTag = "claimguard:phase1-reconciled",
    [string]$ExpectedImageId = "sha256:3aaad642498a3f71e67b37d190e1ba1286865663f4c36dc652f9dd0601461043"
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ClaimGuard - Offline Docker Image Exporter" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Verify Image & SHA-256 ID
Write-Host "--- Step 1/3: Verifying candidate image $ImageTag ---" -ForegroundColor Yellow
$actualId = docker image inspect $ImageTag --format "{{.Id}}" 2>$null
if ($null -eq $actualId -or $actualId -ne $ExpectedImageId) {
    Write-Host "ERROR: Image ID mismatch or image not found!" -ForegroundColor Red
    Write-Host "  Expected : $ExpectedImageId" -ForegroundColor Red
    Write-Host "  Actual   : $actualId" -ForegroundColor Red
    exit 1
}
Write-Host "  Candidate image verified: $actualId" -ForegroundColor Green

# 2. Export Image to Tar Archive
Write-Host "--- Step 2/3: Exporting $ImageTag to $OutputFile ---" -ForegroundColor Yellow
docker save -o $OutputFile $ImageTag
if ($LASTEXITCODE -ne 0 -or -not (Test-Path $OutputFile)) {
    Write-Host "ERROR: 'docker save' failed to produce output archive." -ForegroundColor Red
    exit 1
}

$fileSizeMb = [math]::Round((Get-Item $OutputFile).Length / 1MB, 2)
Write-Host "  Export complete. File size: $fileSizeMb MB" -ForegroundColor Green

# 3. Compute SHA-256 Checksum
Write-Host "--- Step 3/3: Calculating SHA-256 checksum of export archive ---" -ForegroundColor Yellow
$sha256 = (Get-FileHash -Path $OutputFile -Algorithm SHA256).Hash
Write-Host "  SHA-256 Checksum: $sha256" -ForegroundColor Green

$manifest = @{
    ImageTag        = $ImageTag
    ImageId         = $actualId
    ArchiveFileName = [System.IO.Path]::GetFileName($OutputFile)
    FileSizeBytes   = (Get-Item $OutputFile).Length
    FileSizeMB      = $fileSizeMb
    SHA256          = $sha256
    ExportTimestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssK")
} | ConvertTo-Json -Depth 5

$manifestPath = "$OutputFile.manifest.json"
$manifest | Set-Content -Path $manifestPath -Encoding UTF8
Write-Host "  Manifest saved: $manifestPath" -ForegroundColor Green

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  EXPORT READY FOR OFFLINE TRANSFER" -ForegroundColor Green
Write-Host "  Copy '$OutputFile' and '$manifestPath' to your USB drive."
Write-Host "============================================================" -ForegroundColor Cyan
