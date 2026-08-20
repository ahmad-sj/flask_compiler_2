# Builds the compiler into out\classes.
# Sources are globbed at build time, so no file list needs maintaining.
#
#   .\build.ps1
#   java -cp "out\classes;dependencies\antlr-4.13.2-complete.jar" app.FlaskCompiler tests\app.py
#
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

New-Item -ItemType Directory -Force out\classes | Out-Null

$sources = Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName }
$listFile = Join-Path $PSScriptRoot "out\sources.generated.txt"

# Write UTF-8 *without* a BOM: javac cannot parse an argfile that starts with one.
[System.IO.File]::WriteAllLines($listFile, $sources, (New-Object System.Text.UTF8Encoding $false))

javac -nowarn -cp dependencies\antlr-4.13.2-complete.jar -d out\classes "@$listFile"
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

Write-Host "Build OK -> out\classes"
