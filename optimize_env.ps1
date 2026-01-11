<#
.SYNOPSIS
    Jarvis Dev Environment Optimizer
#>

# Ensure Admin
if (!([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole] "Administrator")) {
    Write-Warning "Administrator privileges required."
    Break
}

Write-Host "Jarvis: Optimizing Environment..." -ForegroundColor Cyan

# 1. DEFENDER
$exclusionPaths = @("node_modules", "dist", "build", ".next", ".git", ".gradle", "app\build", "bin", "obj", ".vs")
$exclusionProcesses = @("node.exe", "npm.exe", "yarn.exe", "bun.exe", "adb.exe", "java.exe", "gradlew.bat", "qemu-system-x86_64.exe", "msbuild.exe", "cl.exe", "link.exe", "cmake.exe", "ninja.exe", "python.exe")

Write-Host "Configuring Defender..." -ForegroundColor Yellow
foreach ($path in $exclusionPaths) {
    try {
        Add-MpPreference -ExclusionPath $path -ErrorAction SilentlyContinue
        Write-Host "   [+] Path: $path" -ForegroundColor Green
    }
    catch {
        Write-Host "   [!] Failed: $path" -ForegroundColor Red
    }
}

foreach ($proc in $exclusionProcesses) {
    try {
        Add-MpPreference -ExclusionProcess $proc -ErrorAction SilentlyContinue
        Write-Host "   [+] Process: $proc" -ForegroundColor Green
    }
    catch {
        Write-Host "   [!] Failed: $proc" -ForegroundColor Red
    }
}

# 2. FIREWALL
$firewallRules = @(
    @{ Name = "Jarvis-Web-React"; Port = "3000"; Protocol = "TCP" },
    @{ Name = "Jarvis-Web-Vite"; Port = "5173"; Protocol = "TCP" },
    @{ Name = "Jarvis-Web-Webpack"; Port = "8080"; Protocol = "TCP" },
    @{ Name = "Jarvis-Android-Metro"; Port = "8081"; Protocol = "TCP" },
    @{ Name = "Jarvis-Android-ADB"; Port = "5037"; Protocol = "TCP" },
    @{ Name = "Jarvis-Android-Emu"; Port = "5554-5555"; Protocol = "TCP" }
)

Write-Host "Configuring Firewall..." -ForegroundColor Yellow
foreach ($rule in $firewallRules) {
    if (-not (Get-NetFirewallRule -DisplayName $rule.Name -ErrorAction SilentlyContinue)) {
        New-NetFirewallRule -DisplayName $rule.Name -Direction Inbound -LocalPort $rule.Port -Protocol $rule.Protocol -Action Allow -Profile Any -Description "Jarvis Dev Tool" | Out-Null
        Write-Host "   [+] Port $($rule.Port)" -ForegroundColor Green
    }
}

# 3. DEV MODE
Write-Host "Checking Developer Mode..." -ForegroundColor Yellow
try {
    $devMode = Get-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" -Name "AllowDevelopmentWithoutDevLicense" -ErrorAction SilentlyContinue
    
    if ($devMode.AllowDevelopmentWithoutDevLicense -ne 1) {
        reg add "HKEY_LOCAL_MACHINE\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" /t REG_DWORD /f /v "AllowDevelopmentWithoutDevLicense" /d "1"
        Write-Host "   [+] Developer Mode Enabled" -ForegroundColor Green
    }
    else {
        Write-Host "   [v] Enabled" -ForegroundColor Green
    }
}
catch {
    Write-Host "   [!] Check Failed" -ForegroundColor Red
}

Write-Host "Optimization Complete." -ForegroundColor Cyan
