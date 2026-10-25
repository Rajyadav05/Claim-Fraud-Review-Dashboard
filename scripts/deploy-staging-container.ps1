<#
.SYNOPSIS
    Automated Docker Staging Container Deployment and Health Verification.
.DESCRIPTION
    Manages safe deployment and health verification for ClaimGuard staging container:
    1. Validates Docker CLI and Engine accessibility via explicit paths and named pipe.
    2. Inspects host port 8083 and existing staging containers.
    3. Safely backs up any running staging container before starting the new container.
    4. Starts the new container mapped to host port 8083.
    5. Polls /api/health and verifies HTTP 200 and JSON status "UP".
    6. Verifies root endpoint http://localhost:8083/ returns HTTP 200.
    7. Automatically rolls back to the previous staging container if health checks fail.
    8. Strictly preserves the Task 11 container (claimguard-task11 on port 8082).
#>
[CmdletBinding()]
param (
    [string]$ImageTag = "claimguard:staging",
    [string]$ContainerName = "claimguard-staging",
    [int]$HostPort = 8083,
    [int]$ContainerPort = 8080,
    [int]$MaxHealthWaitSeconds = 45,
    [string]$DockerBin = "C:\Users\User\AppData\Local\Programs\DockerDesktop\resources\bin",
    [string]$DockerHost = "npipe:////./pipe/docker_engine"
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================"
Write-Host "[DEPLOYER] ClaimGuard Staging Container Deployment"
Write-Host "Target Image       : $ImageTag"
Write-Host "Container Name     : $ContainerName"
Write-Host "Port Mapping       : Host ${HostPort} -> Container ${ContainerPort}"
Write-Host "Max Health Timeout : ${MaxHealthWaitSeconds}s"
Write-Host "Docker Binary Path : $DockerBin"
Write-Host "Docker Host Pipe   : $DockerHost"
Write-Host "============================================================"

# Configure environment for Docker CLI execution
if (Test-Path $DockerBin) {
    $env:Path = "$DockerBin;$env:Path"
}
$env:DOCKER_HOST = $DockerHost

# Verify docker CLI availability
$dockerExe = Get-Command "docker.exe" -ErrorAction SilentlyContinue
if (-not $dockerExe) {
    Write-Error "[FATAL] docker.exe not found on PATH ($DockerBin). Aborting deployment."
    exit 1
}

# Verify Docker engine connectivity
try {
    $dockerInfo = & docker.exe info 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Error "[FATAL] Cannot connect to Docker engine at $DockerHost : $dockerInfo"
        exit 1
    }
    Write-Host "[INFO] Docker engine connectivity confirmed."
} catch {
    Write-Error "[FATAL] Failed to query Docker engine: $_"
    exit 1
}

# Safety Check: Verify Task 11 container is running and not targeted
$task11Container = & docker.exe ps -q --filter "name=^/claimguard-task11$" 2>$null
if ($task11Container) {
    Write-Host "[SAFETY] Verified Task 11 container (claimguard-task11) is active on port 8082. Will NOT touch it."
}

# Verify Host Port doesn't collide with non-Docker process
$portOccupied = $false
try {
    $tcpConn = Get-NetTCPConnection -LocalPort $HostPort -ErrorAction SilentlyContinue
    if ($tcpConn) {
        $portOccupied = $true
        $owningPids = $tcpConn | Select-Object -ExpandProperty OwningProcess -Unique
    }
} catch {
    # On some systems Get-NetTCPConnection may require elevated rights; fallback to netstat
    $netstatOutput = netstat -ano | findstr ":$HostPort "
    if ($netstatOutput) {
        $portOccupied = $true
    }
}

# Inspect if an existing container named $ContainerName is present
$existingContainerId = & docker.exe ps -a -q --filter "name=^/${ContainerName}$" 2>$null
$backupContainerName = "${ContainerName}-backup"

# Clean up any stale backup container from previous aborted runs
$staleBackupId = & docker.exe ps -a -q --filter "name=^/${backupContainerName}$" 2>$null
if ($staleBackupId) {
    Write-Host "[INFO] Removing old backup container ($backupContainerName)..."
    & docker.exe rm -f $backupContainerName | Out-Null
}

$hasExistingBackup = $false
if ($existingContainerId) {
    Write-Host "[INFO] Found existing staging container '$ContainerName' (ID: $existingContainerId)."
    Write-Host "[INFO] Gracefully stopping and archiving existing container as backup..."
    & docker.exe stop $ContainerName | Out-Null
    & docker.exe rename $ContainerName $backupContainerName | Out-Null
    $hasExistingBackup = $true
    Write-Host "[INFO] Staged backup created: '$backupContainerName'."
} elseif ($portOccupied) {
    Write-Error "[FATAL] Host port $HostPort is in use by an external process, not '$ContainerName'. Halting to prevent collision."
    exit 1
}

# Launch the new staging container
Write-Host "[INFO] Launching new container '$ContainerName' with image '$ImageTag'..."
$newContainerId = & docker.exe run -d --name $ContainerName -p "${HostPort}:${ContainerPort}" $ImageTag
if ($LASTEXITCODE -ne 0 -or -not $newContainerId) {
    Write-Error "[FATAL] docker run failed for image '$ImageTag'."
    if ($hasExistingBackup) {
        Write-Warning "[ROLLBACK] Attempting to restore backup container '$backupContainerName'..."
        & docker.exe rename $backupContainerName $ContainerName
        & docker.exe start $ContainerName
    }
    exit 1
}
Write-Host "[INFO] Container started. Assigned Container ID: $newContainerId"

# Health check polling
$healthUrl = "http://localhost:${HostPort}/api/health"
$rootUrl = "http://localhost:${HostPort}/"
$isHealthy = $false
$healthResponse = $null
$elapsed = 0

Write-Host "[INFO] Polling health endpoint: $healthUrl (Timeout: ${MaxHealthWaitSeconds}s)..."
while ($elapsed -lt $MaxHealthWaitSeconds) {
    Start-Sleep -Seconds 2
    $elapsed += 2
    try {
        $resp = Invoke-WebRequest -Uri $healthUrl -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        if ($resp.StatusCode -eq 200) {
            $json = $resp.Content | ConvertFrom-Json
            if ($json.status -eq "UP") {
                $isHealthy = $true
                $healthResponse = $resp.Content
                Write-Host "[SUCCESS] Health check passed at attempt ${elapsed}s: $($resp.StatusCode) $($json.status)"
                break
            }
        }
    } catch {
        Write-Host "[WAIT] Waiting for container readiness ($elapsed/${MaxHealthWaitSeconds}s)..."
    }
}

if (-not $isHealthy) {
    Write-Host "============================================================" -ForegroundColor Red
    Write-Host "[ERROR] Health check failed or timed out after ${MaxHealthWaitSeconds}s!" -ForegroundColor Red
    Write-Host "[ERROR] Dumping container logs for diagnosis:" -ForegroundColor Red
    & docker.exe logs $ContainerName --tail 50
    Write-Host "============================================================" -ForegroundColor Red

    Write-Warning "[TEARDOWN] Stopping and removing faulty container '$ContainerName'..."
    & docker.exe stop $ContainerName 2>$null | Out-Null
    & docker.exe rm -f $ContainerName 2>$null | Out-Null

    if ($hasExistingBackup) {
        Write-Warning "[ROLLBACK] Restoring previous staging container from backup '$backupContainerName'..."
        & docker.exe rename $backupContainerName $ContainerName
        & docker.exe start $ContainerName
        Write-Host "[ROLLBACK] Previous staging container restored."
    }
    exit 1
}

# Verify Root Dashboard URL
try {
    $rootResp = Invoke-WebRequest -Uri $rootUrl -UseBasicParsing -TimeoutSec 5 -ErrorAction Stop
    if ($rootResp.StatusCode -eq 200) {
        Write-Host "[SUCCESS] Root dashboard check passed: Status $($rootResp.StatusCode) (Content-Length: $($rootResp.Content.Length) bytes)"
    } else {
        Write-Warning "[WARN] Root dashboard returned unexpected status: $($rootResp.StatusCode)"
    }
} catch {
    Write-Error "[ERROR] Root dashboard check failed: $_"
    exit 1
}

# Post-Deployment Success: Remove backup container if present
if ($hasExistingBackup) {
    Write-Host "[INFO] Deployment healthy. Removing decommissioned backup container '$backupContainerName'..."
    & docker.exe rm -f $backupContainerName 2>$null | Out-Null
}

Write-Host "============================================================"
Write-Host "[DEPLOYMENT SUCCESS] ClaimGuard Staging Container is Healthy!"
Write-Host "Container Name     : $ContainerName"
Write-Host "Container ID       : $newContainerId"
Write-Host "Image Tag          : $ImageTag"
Write-Host "Endpoint           : http://localhost:${HostPort}/"
Write-Host "Health Probe       : $healthUrl"
Write-Host "Health Payload     : $healthResponse"
Write-Host "============================================================"
exit 0
