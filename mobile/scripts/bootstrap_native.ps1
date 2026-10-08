$ErrorActionPreference = "Stop"
Set-Location (Split-Path $PSScriptRoot -Parent)
if (-not (Get-Command flutter -ErrorAction SilentlyContinue)) { throw "Flutter is not installed or is not on PATH." }
$temp = Join-Path $env:TEMP ("eone-mobile-backup-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $temp | Out-Null
Copy-Item lib $temp -Recurse
Copy-Item pubspec.yaml $temp
Copy-Item analysis_options.yaml $temp
flutter create --platforms=android,ios .
Remove-Item lib -Recurse -Force
Copy-Item (Join-Path $temp "lib") . -Recurse
Copy-Item (Join-Path $temp "pubspec.yaml") . -Force
Copy-Item (Join-Path $temp "analysis_options.yaml") . -Force
Remove-Item $temp -Recurse -Force
flutter pub get
Write-Host "Native Flutter platform files generated successfully."
