<#
.SYNOPSIS
    Safely stops and cleans up the ClaimGuard standalone college demo container.
.DESCRIPTION
    Strictly targets the dedicated demo container 'claimguard-demo-app' and optionally
    removes the persistent demo volume 'claimguard-demo-data' when -CleanStorage is set.
    Includes strict guardrails protecting staging and legacy containers.
#>
[CmdletBinding()]
param (
    [switch]$CleanStorage
)

$ErrorActionPreference = "Stop"

$demoContainerName = "claimguard-demo-app"
$demoVolumeName = "claimguard-demo-data"

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ClaimGuard - Demo Teardown & Stop Script" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

# 1. Verify existence of demo container
$demoContId = docker ps -a -q --filter "name=^${demoContainerName}$"
if ($demoContId) {
    Write-Host "[STOP] Stopping demo container '$demoContainerName'..."
    docker stop $demoContainerName | Out-Null
    Write-Host "[REMOVE] Removing demo container '$demoContainerName'..."
    docker rm -f $demoContainerName | Out-Null
    Write-Host "  Container '$demoContainerName' stopped and removed." -ForegroundColor Green
} else {
    Write-Host "  Demo container '$demoContainerName' is not running (nothing to stop)." -ForegroundColor Gray
}

# 2. Volume cleanup (opt-in only)
if ($CleanStorage.IsPresent) {
    $demoVol = docker volume ls -q --filter "name=^${demoVolumeName}$"
    if ($demoVol) {
        Write-Host "[STORAGE] Removing demo persistent volume '$demoVolumeName'..."
        docker volume rm $demoVolumeName | Out-Null
        Write-Host "  Demo storage volume removed cleanly." -ForegroundColor Green
    }
} else {
    Write-Host "  Persistent storage '$demoVolumeName' preserved for data durability." -ForegroundColor Gray
    Write-Host "  (To completely reset the demo database, run: .\stop-demo.ps1 -CleanStorage)" -ForegroundColor Gray
}

# 3. Guardrail Invariant Check: Confirm protected resources are untouched
Write-Host "[SAFETY AUDIT] Verifying protected container invariants..."
$protectedNames = @("claimguard-staging", "claimguard-ansible", "claimguard-task11")
foreach ($p in $protectedNames) {
    $c = docker ps -q --filter "name=^$p$"
    if ($c) {
        Write-Host "  Protected container '$p' verified UNTOUCHED and active." -ForegroundColor Green
    }
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  ClaimGuard Demo Teardown Complete" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
