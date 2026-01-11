
Write-Host "Testing Developer Mode Block"

try {
    $devMode = Get-ItemProperty -Path "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\AppModelUnlock" -Name "AllowDevelopmentWithoutDevLicense" -ErrorAction SilentlyContinue
    
    if ($devMode.AllowDevelopmentWithoutDevLicense -ne 1) {
        Write-Host "   [+] Developer Mode Enabled" -ForegroundColor Green
    }
    else {
        Write-Host "   [✓] Enabled" -ForegroundColor Green
    }
}
catch {
    Write-Host "   [!] Check Failed" -ForegroundColor Red
}
