<#
.SYNOPSIS
    Automated E2E Test Runner for ClaimGuard Fraud Review Dashboard.
.DESCRIPTION
    Manages the lifecycle of the Spring Boot application for automated E2E browser tests:
    1. Checks if the target port (default 8080) is already occupied; if so, fails safely
       without terminating external processes.
    2. Starts the packaged Spring Boot executable JAR in the background and tracks its PID.
    3. Polls the /api/health endpoint until the application returns {"status":"UP"}.
    4. Executes the Selenium test suite (ClaimGuardSeleniumE2ETest) via Maven Wrapper.
    5. In the finally block, reliably stops ONLY the tracked application process.
    6. Reports any remaining listeners on the port without killing them.
    7. Propagates the test exit code to the caller.
#>
[CmdletBinding()]
param (
    [int]$Port = 8080,
    [string]$JarPath = "target\claimguard-dashboard-1.0.0-SNAPSHOT.jar",
    [int]$MaxWaitSeconds = 30,
    [string]$JavaHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot",
    [bool]$Headless = $true
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================"
Write-Host "[E2E RUNNER] ClaimGuard Automated E2E Test Suite"
Write-Host "Target Port        : $Port"
Write-Host "Application JAR    : $JarPath"
Write-Host "Max Readiness Wait : $MaxWaitSeconds seconds"
Write-Host "Headless Mode      : $Headless"
Write-Host "============================================================"

# Configure Java runtime
if (-not (Test-Path $JavaHome)) {
    if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
        $JavaHome = $env:JAVA_HOME
    } else {
        Write-Warning "[WARN] Specified JavaHome not found, falling back to system PATH java"
        $JavaHome = ""
    }
}

$JavaExe = if ($JavaHome) { Join-Path $JavaHome "bin\java.exe" } else { "java.exe" }
Write-Host "[INFO] Using Java binary: $JavaExe"

# Verify JAR existence
if (-not (Test-Path $JarPath)) {
    Write-Error "[ERROR] Application JAR not found at '$JarPath'. Run 'mvnw.cmd package' first."
    exit 1
}

# Verify target port is not already occupied by an external process
$isPortOccupied = $false
$occupiedPidList = ""
try {
    $existingConns = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
    if ($existingConns) {
        $occupiedPids = $existingConns | Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -gt 0 }
        if ($occupiedPids) {
            $isPortOccupied = $true
            $occupiedPidList = $occupiedPids -join ', '
        }
    }
} catch {
    Write-Warning "[WARN] Could not inspect active TCP connections on port ${Port}: $_"
}

if ($isPortOccupied) {
    Write-Host "[ERROR] Target port $Port is already in use by process PID(s): $occupiedPidList." -ForegroundColor Red
    Write-Host "[ERROR] This script will not terminate external processes. Please inspect and stop the existing process manually, or choose a different port using -Port <number>." -ForegroundColor Red
    exit 1
}

# Ensure log directory exists
$logDir = "target"
if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir -Force | Out-Null
}
$appLogOut = Join-Path $logDir "app-e2e-out.log"
$appLogErr = Join-Path $logDir "app-e2e-err.log"

# Fallback driver search: If SE_CACHE_PATH is unset, check User cache for ChromeDriver
if (-not $env:SE_CACHE_PATH) {
    $userCache = Join-Path $env:USERPROFILE ".cache\selenium"
    if (Test-Path $userCache) {
        $env:SE_CACHE_PATH = $userCache
        Write-Host "[INFO] Configured SE_CACHE_PATH: $env:SE_CACHE_PATH"
    }
}

Write-Host "[INFO] Launching Spring Boot application in background..."
$startInfo = New-Object System.Diagnostics.ProcessStartInfo
$startInfo.FileName = $JavaExe
$startInfo.Arguments = "-Dserver.port=$Port -jar `"$JarPath`""
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.CreateNoWindow = $true

$appProcess = New-Object System.Diagnostics.Process
$appProcess.StartInfo = $startInfo

# Create stream writers for background output logging
$outStream = [System.IO.File]::CreateText($appLogOut)
$errStream = [System.IO.File]::CreateText($appLogErr)

$outHandler = {
    if ($EventArgs.Data) {
        $Event.MessageData.WriteLine($EventArgs.Data)
        $Event.MessageData.Flush()
    }
}

Register-ObjectEvent -InputObject $appProcess -EventName "OutputDataReceived" -Action $outHandler -MessageData $outStream | Out-Null
Register-ObjectEvent -InputObject $appProcess -EventName "ErrorDataReceived" -Action $outHandler -MessageData $errStream | Out-Null

$started = $appProcess.Start()
if (-not $started) {
    Write-Error "[ERROR] Failed to start application process."
    $outStream.Close()
    $errStream.Close()
    exit 1
}

$appProcess.BeginOutputReadLine()
$appProcess.BeginErrorReadLine()
$ownedPid = $appProcess.Id
Write-Host "[INFO] Application started with PID: $ownedPid"

$testExitCode = 1

try {
    Write-Host "[INFO] Polling readiness at http://localhost:$Port/api/health..."
    $healthy = $false
    $healthUri = "http://localhost:$Port/api/health"

    for ($attempt = 1; $attempt -le $MaxWaitSeconds; $attempt++) {
        Start-Sleep -Seconds 1

        if ($appProcess.HasExited) {
            Write-Error "[ERROR] Application process (PID $ownedPid) exited prematurely with code $($appProcess.ExitCode)!"
            break
        }

        try {
            $resp = Invoke-RestMethod -Uri $healthUri -TimeoutSec 2 -ErrorAction Stop
            if ($resp.status -eq "UP") {
                $healthy = $true
                Write-Host "[SUCCESS] Application is UP and healthy on attempt $attempt."
                Write-Host "[INFO] Health Response: $($resp | ConvertTo-Json -Compress)"
                break
            }
        } catch {
            # Expected during startup
        }
    }

    if (-not $healthy) {
        Write-Error "[ERROR] Application failed to report healthy status within $MaxWaitSeconds seconds."
        $testExitCode = 1
    } else {
        Write-Host "============================================================"
        Write-Host "[STAGE] Executing Selenium E2E Suite"
        Write-Host "============================================================"

        $mvnCmd = ".\mvnw.cmd"
        $mvnArgs = @(
            "test",
            "-Dtest=ClaimGuardSeleniumE2ETest",
            "-Dclaimguard.base-url=http://localhost:$Port",
            "-Dselenium.headless=$Headless"
        )

        if ($JavaHome) {
            $env:JAVA_HOME = $JavaHome
        }

        Write-Host "[INFO] Invoking: $mvnCmd $($mvnArgs -join ' ')"
        $proc = Start-Process -FilePath $mvnCmd -ArgumentList $mvnArgs -NoNewWindow -Wait -PassThru
        $testExitCode = $proc.ExitCode

        Write-Host "============================================================"
        Write-Host "[RESULT] Selenium Test Suite Exit Code: $testExitCode"
        Write-Host "============================================================"
    }

} finally {
    Write-Host "[TEARDOWN] Stopping owned application process (PID: $ownedPid)..."
    try {
        if (-not $appProcess.HasExited) {
            Stop-Process -Id $ownedPid -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 1
        }
    } catch {
        Write-Warning "[WARN] Could not stop process ${ownedPid}. Error: $_"
    }

    # Verify port state and report any remaining listeners without terminating them
    try {
        $remainingConns = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue
        if ($remainingConns) {
            $remPids = $remainingConns | Select-Object -ExpandProperty OwningProcess -Unique | Where-Object { $_ -gt 0 -and $_ -ne $ownedPid }
            if ($remPids) {
                Write-Warning "[WARN] Target port $Port is still in use by external process PID(s): $($remPids -join ', '). These processes were not started by this script and were NOT terminated."
            } else {
                Write-Host "[TEARDOWN] Port $Port confirmed released."
            }
        } else {
            Write-Host "[TEARDOWN] Port $Port confirmed released."
        }
    } catch {
        Write-Host "[TEARDOWN] Port $Port release check completed."
    }

    # Close log streams
    try {
        $outStream.Flush()
        $outStream.Close()
        $errStream.Flush()
        $errStream.Close()
    } catch {}

    Write-Host "[TEARDOWN] Teardown complete for tracked PID $ownedPid."
}

exit $testExitCode
