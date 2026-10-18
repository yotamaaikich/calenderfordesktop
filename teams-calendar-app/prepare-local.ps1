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

  $desktop = [Environment]::GetFolderPath('Desktop')
  $shortcutPath = Join-Path $desktop 'Teams Calendar.lnk'
  $shell = New-Object -ComObject WScript.Shell
  $shortcut = $shell.CreateShortcut($shortcutPath)
  $shortcut.TargetPath = (Get-Command powershell.exe).Source
  $shortcut.Arguments = "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$appRoot\launch.ps1`""
  $shortcut.WorkingDirectory = $appRoot
  $shortcut.Description = 'Open the local Teams notification calendar'
  $shortcut.Save()
  Write-Host "Setup complete. Desktop shortcut created: $shortcutPath"
  Write-Host 'Starting the app. Click 通知を許可 in the Teams Calendar window when Windows asks.'
  & '.\launch.ps1'
}
finally { Pop-Location }
