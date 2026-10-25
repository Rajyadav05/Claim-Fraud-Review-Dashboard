param (
    [int]$Port = 8080,
    [string]$JarPath = "target\claimguard-dashboard-1.0.0-SNAPSHOT.jar",
    [string]$OutputDir = "C:\Users\User\.gemini\antigravity-ide\brain\d8af03ca-e58d-4089-8116-8fd50f69254d\screenshots"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$ChromePath = "C:\Program Files\Google\Chrome\Application\chrome.exe"
if (-not (Test-Path $ChromePath)) {
    Write-Error "Chrome not found at $ChromePath"
    exit 1
}

# 1. Start application on Port 8080
Write-Host "[INFO] Starting application on port $Port..."
$AppProcess = Start-Process -FilePath "java" -ArgumentList "-Dserver.port=$Port", "-jar", $JarPath -PassThru -NoNewWindow
$AppPid = $AppProcess.Id

# Wait for readiness
$Ready = $false
for ($i = 1; $i -le 30; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:$Port/api/health" -Method Get -TimeoutSec 2
        if ($res.status -eq "UP") {
            $Ready = $true
            Write-Host "[SUCCESS] Application is UP and healthy on port $Port."
            break
        }
    } catch {}
}

if (-not $Ready) {
    Write-Error "Application failed to become ready on port $Port."
    Stop-Process -Id $AppPid -Force
    exit 1
}

try {
    $Resolutions = @(
        @{ Width = 1920; Height = 1080; Suffix = "1920x1080" },
        @{ Width = 1440; Height = 900;  Suffix = "1440x900" },
        @{ Width = 1366; Height = 768;  Suffix = "1366x768" }
    )

    $Pages = @(
        @{ Name = "screen-01-dashboard"; Url = "http://localhost:$Port/index.html" },
        @{ Name = "screen-02-claims";    Url = "http://localhost:$Port/claims.html" },
        @{ Name = "screen-03-submit";    Url = "http://localhost:$Port/submit.html" },
        @{ Name = "screen-04-review";    Url = "http://localhost:$Port/review.html?id=1" }
    )

    foreach ($page in $Pages) {
        foreach ($res in $Resolutions) {
            $outFile = Join-Path $OutputDir "$($page.Name)-$($res.Suffix).png"
            Write-Host "[SCREENSHOT] Capturing $($page.Name) at $($res.Suffix)..."
            Start-Process -FilePath $ChromePath -ArgumentList `
                "--headless=new", `
                "--screenshot=`"$outFile`"", `
                "--window-size=$($res.Width),$($res.Height)", `
                "--hide-scrollbars", `
                "--virtual-time-budget=4000", `
                "`"$($page.Url)`"" -Wait -NoNewWindow
        }
    }

    # Also capture Stitch reference files for 1920x1080 comparison
    $RepoRoot = (Get-Location).Path
    $StitchReferences = @(
        @{ Name = "stitch-ref-01-dashboard"; Path = "$RepoRoot\docs\stitch-export\screen-01-executive-dashboard.html" },
        @{ Name = "stitch-ref-02-claims";    Path = "$RepoRoot\docs\stitch-export\screen-02-all-claims.html" },
        @{ Name = "stitch-ref-03-submit";    Path = "$RepoRoot\docs\stitch-export\screen-03-submit-claim.html" },
        @{ Name = "stitch-ref-04-review";    Path = "$RepoRoot\docs\stitch-export\screen-04-claim-details-review.html" }
    )

    foreach ($ref in $StitchReferences) {
        $outFile = Join-Path $OutputDir "$($ref.Name)-1920x1080.png"
        Write-Host "[SCREENSHOT] Capturing Stitch Reference $($ref.Name)..."
        $fileUrl = "file:///" + ($ref.Path -replace '\\', '/')
        Start-Process -FilePath $ChromePath -ArgumentList `
            "--headless=new", `
            "--screenshot=`"$outFile`"", `
            "--window-size=1920,1080", `
            "--hide-scrollbars", `
            "--virtual-time-budget=2000", `
            "`"$fileUrl`"" -Wait -NoNewWindow
    }

    Write-Host "[SUCCESS] All screenshots captured in $OutputDir"
} finally {
    Write-Host "[TEARDOWN] Stopping test server (PID $AppPid)..."
    Stop-Process -Id $AppPid -Force -ErrorAction SilentlyContinue
}
