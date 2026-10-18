$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$node = Join-Path $appRoot 'runtime\node.exe'
if (-not (Test-Path $node)) {
  throw 'Run prepare-local.ps1 first to set up the Windows notification permission.'
}
if (Get-NetTCPConnection -LocalPort 43187 -State Listen -ErrorAction SilentlyContinue) {
  Write-Host 'Teams Calendar is already running.'
  exit 0
}
$logs = Join-Path $appRoot 'logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$stdout = Join-Path $logs 'app.log'
$stderr = Join-Path $logs 'error.log'
Start-Process -FilePath $node -ArgumentList 'main.js' -WorkingDirectory $appRoot -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr | Out-Null
