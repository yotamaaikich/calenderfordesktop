$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$node = Join-Path $appRoot 'runtime\node.exe'
if (-not (Test-Path $node)) {
  throw 'Run prepare-local.ps1 first to set up the Windows notification permission.'
}
$logs = Join-Path $appRoot 'logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$stdout = Join-Path $logs 'app.log'
$stderr = Join-Path $logs 'error.log'
$startupLog = Join-Path $logs 'startup.log'

if (-not (Get-NetTCPConnection -LocalPort 43187 -State Listen -ErrorAction SilentlyContinue)) {
  Start-Process -FilePath $node -ArgumentList 'main.js' -WorkingDirectory $appRoot -WindowStyle Hidden -RedirectStandardOutput $stdout -RedirectStandardError $stderr | Out-Null
}

# Do not ask Lively to load the wallpaper until the local page can answer.
$ready = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
  try {
    $response = Invoke-WebRequest -Uri 'http://127.0.0.1:43187/api/calendar' -TimeoutSec 2 -UseBasicParsing
    if ($response.StatusCode -eq 200) { $ready = $true; break }
  } catch { }
  Start-Sleep -Seconds 1
}
if (-not $ready) {
  Add-Content -Path $startupLog -Value "$(Get-Date -Format o) Calendar server did not become ready; wallpaper was not reloaded."
  exit 1
}

# Use a self-contained copy in Lively's library, not a path inside the source checkout.
$livelyExe = Join-Path $env:ProgramFiles 'Lively Wallpaper\Lively.exe'
$livelyWallpaper = Join-Path $env:LOCALAPPDATA 'Lively Wallpaper\Library\wallpapers\notification-calendar'
if ((Test-Path $livelyExe) -and (Test-Path (Join-Path $livelyWallpaper 'LivelyInfo.json'))) {
  try {
    & $livelyExe setwp --file $livelyWallpaper 2>&1 | Out-File -FilePath $startupLog -Append -Encoding utf8
    Add-Content -Path $startupLog -Value "$(Get-Date -Format o) Lively wallpaper applied after calendar server was ready."
  } catch {
    Add-Content -Path $startupLog -Value "$(Get-Date -Format o) Lively wallpaper could not be applied: $($_.Exception.Message)"
  }
} else {
  Add-Content -Path $startupLog -Value "$(Get-Date -Format o) Lively or its installed calendar wallpaper was not found."
}
