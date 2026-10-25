<#
.SYNOPSIS
    Cross-Platform Ansible Playbook Runner for ClaimGuard Fraud Review Dashboard.
.DESCRIPTION
    Executes Ansible playbooks inside Ubuntu WSL against local ClaimGuard targets:
    1. Validates that WSL and Ubuntu distribution are available.
    2. Translates Windows workspace path to WSL Linux mount path (/mnt/e/...).
    3. Verifies that ansible-playbook is installed in Ubuntu WSL.
    4. Passes dynamic parameters (playbook, inventory, image, port, check mode).
    5. Executes ansible-playbook with ANSIBLE_CONFIG configured.
    6. Propagates the Ansible exit code to the caller.
#>
[CmdletBinding()]
param (
    [string]$Playbook = "playbooks/deploy-claimguard.yml",
    [string]$Inventory = "inventory/hosts.ini",
    [string]$ImageTag = "claimguard:staging",
    [int]$HostPort = 8084,
    [switch]$CheckMode,
    [switch]$SyntaxCheck,
    [string]$WslDistro = "Ubuntu"
)

$ErrorActionPreference = "Stop"

Write-Host "============================================================"
Write-Host "[ANSIBLE RUNNER] ClaimGuard Configuration Management"
Write-Host "Playbook           : $Playbook"
Write-Host "Inventory          : $Inventory"
Write-Host "Target Image       : $ImageTag"
Write-Host "Host Port          : $HostPort"
Write-Host "Check Mode (Dry)   : $($CheckMode.IsPresent)"
Write-Host "Syntax Check Only  : $($SyntaxCheck.IsPresent)"
Write-Host "WSL Distribution   : $WslDistro"
Write-Host "============================================================"

# 1. Verify WSL accessibility
$wslCheck = Get-Command "wsl.exe" -ErrorAction SilentlyContinue
if (-not $wslCheck) {
    Write-Error "[FATAL] wsl.exe not found on Windows PATH. Cannot invoke Ansible control node."
    exit 1
}

# 2. Verify Ubuntu distribution is accessible
wsl.exe -d $WslDistro -- true 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Error "[FATAL] WSL distribution '$WslDistro' is not installed or not accessible."
    exit 1
}

# 3. Verify ansible-playbook is installed inside Ubuntu WSL
$ansibleVersion = wsl -d $WslDistro -- ansible-playbook --version 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Error "[FATAL] ansible-playbook is not installed inside $WslDistro. Please install via: wsl -d $WslDistro -- apt-get install -y ansible"
    exit 1
}
Write-Host "[INFO] Detected Ansible control node in $WslDistro."

# 4. Resolve Windows path to WSL Linux mount path
$winPath = (Get-Item -Path $PSScriptRoot).Parent.FullName
$driveLetter = $winPath.Substring(0, 1).ToLowerInvariant()
$restOfPath = $winPath.Substring(2).Replace('\', '/')
$wslWorkspace = "/mnt/$driveLetter$restOfPath"
$wslAnsibleDir = "$wslWorkspace/ansible"
Write-Host "[INFO] Mapped workspace to WSL mount: $wslAnsibleDir"

# 5. Build Ansible command options
$modeFlag = ""
if ($SyntaxCheck.IsPresent) {
    $modeFlag = "--syntax-check"
} elseif ($CheckMode.IsPresent) {
    $modeFlag = "--check"
}

# 6. Execute Ansible Playbook inside Ubuntu WSL
$bashCmd = "export ANSIBLE_CONFIG='$wslAnsibleDir/ansible.cfg'; cd '$wslAnsibleDir' && ansible-playbook $modeFlag $Playbook -i $Inventory -e claimguard_image=$ImageTag -e claimguard_host_port=$HostPort"
Write-Host "[INFO] Executing playbook via WSL..."
Write-Host "[CMD]  wsl -d $WslDistro -- bash -c `"$bashCmd`""

wsl.exe -d $WslDistro -- bash -c $bashCmd
$playbookExitCode = $LASTEXITCODE

Write-Host "============================================================"
if ($playbookExitCode -eq 0) {
    Write-Host "[SUCCESS] Ansible playbook completed successfully (Exit Code 0)." -ForegroundColor Green
} else {
    Write-Host "[ERROR] Ansible playbook execution failed (Exit Code $playbookExitCode)." -ForegroundColor Red
}
Write-Host "============================================================"

exit $playbookExitCode
