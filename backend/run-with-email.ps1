# Starts I-ITSM with real Gmail e-mail delivery.
# The Gmail App Password is typed at startup (hidden), kept only in this window's memory, and never saved.
#
#   powershell -ExecutionPolicy Bypass -File .\run-with-email.ps1 -GmailAddress you@gmail.com
#   powershell -ExecutionPolicy Bypass -File .\run-with-email.ps1 -GmailAddress you@gmail.com -RedirectTo you@gmail.com
#
# -RedirectTo sends every notification to one inbox (the demo users have fake @itsm.local addresses).
param(
    [Parameter(Mandatory = $true)] [string] $GmailAddress,
    [string] $RedirectTo = ''
)

$secure = Read-Host "Gmail App Password for $GmailAddress" -AsSecureString
$env:ITSM_MAIL_USERNAME = $GmailAddress
$env:ITSM_MAIL_PASSWORD = ([System.Net.NetworkCredential]::new('', $secure).Password) -replace '\s', ''
$env:ITSM_MAIL_REDIRECT_TO = $RedirectTo

Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -match 'itsm-backend' } |
    ForEach-Object { Write-Host "Stopping the I-ITSM server that is already running (PID $($_.ProcessId))"; Stop-Process -Id $_.ProcessId }

Set-Location $PSScriptRoot
Write-Host "Starting I-ITSM with real e-mail from $GmailAddress ... open http://localhost:8080 when you see 'Started ItsmApplication'"
java -Xmx512m -jar target\itsm-backend-1.0.0.jar
