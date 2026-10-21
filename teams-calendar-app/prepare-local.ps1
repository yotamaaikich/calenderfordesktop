$ErrorActionPreference = 'Stop'
$appRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Push-Location $appRoot
try {
  if (-not (Test-Path '.\node_modules\@microsoft\winappcli')) {
    npm install
    if ($LASTEXITCODE -ne 0) { throw 'npm install failed.' }
  }
  if (-not (Test-Path '.\.winapp\bin\x64\Microsoft.WindowsAppRuntime.Bootstrap.dll')) {
    npm run restore
    if ($LASTEXITCODE -ne 0) { throw 'Windows App SDK restore failed.' }
  }

  $nodeCommand = Get-Command node.exe -ErrorAction Stop
  New-Item -ItemType Directory -Force -Path '.\runtime' | Out-Null
  $originalNode = (Resolve-Path '.\runtime').Path + '\node.original.exe'
  $identityNode = (Resolve-Path '.\runtime').Path + '\node.exe'
  if (-not (Test-Path $originalNode)) { Copy-Item $nodeCommand.Source $originalNode }
  Get-CimInstance Win32_Process | Where-Object { $_.ExecutablePath -ieq $identityNode } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }
  Start-Sleep -Milliseconds 500
  Copy-Item $originalNode $identityNode -Force

  & '.\node_modules\.bin\winapp.cmd' create-debug-identity $identityNode --manifest '.\Package.appxmanifest' --keep-identity
  if ($LASTEXITCODE -ne 0) { throw 'Package identity setup failed. Check the CLI output above, the app manifest, and Windows Developer Mode.' }

  $livelyWallpaper = Join-Path $env:LOCALAPPDATA 'Lively Wallpaper\Library\wallpapers\notification-calendar'
  if (Test-Path (Join-Path $env:ProgramFiles 'Lively Wallpaper\Lively.exe')) {
    New-Item -ItemType Directory -Force -Path $livelyWallpaper | Out-Null
    Copy-Item '.\wallpaper\index.html', '.\wallpaper\LivelyInfo.json' -Destination $livelyWallpaper -Force
    Write-Host "Installed the calendar wallpaper in the Lively library: $livelyWallpaper"
  } else {
    Write-Host 'Lively Wallpaper was not found; the calendar app will still start normally.'
  }

  $powerShellPath = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
  if (-not (Test-Path $powerShellPath)) { throw "Windows PowerShell was not found: $powerShellPath" }
  $shortcutArguments = "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$appRoot\launch.ps1`""
  $shortcutTargets = @(
    (Join-Path ([Environment]::GetFolderPath('Desktop')) 'Teams Calendar.lnk'),
    (Join-Path ([Environment]::GetFolderPath('Startup')) 'Teams Calendar.lnk')
  )
  $shell = New-Object -ComObject WScript.Shell
  foreach ($shortcutPath in $shortcutTargets) {
    $shortcut = $shell.CreateShortcut($shortcutPath)
    $shortcut.TargetPath = $powerShellPath
    $shortcut.Arguments = $shortcutArguments
    $shortcut.WorkingDirectory = $appRoot
    $shortcut.Description = 'Open the local Teams notification calendar'
    $shortcut.Save()
  }
  Write-Host "Setup complete. Desktop shortcut created: $($shortcutTargets[0])"
  Write-Host "Automatic sign-in startup shortcut created: $($shortcutTargets[1])"
  Write-Host 'Starting the app. Click 通知を許可 in the Teams Calendar window when Windows asks.'
  & '.\launch.ps1'
}
finally { Pop-Location }
