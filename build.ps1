# Builds the compiler into out\classes.
# Sources are globbed at build time, so no file list needs maintaining.
#
#   .\build.ps1
#   java -cp "out\classes;dependencies\antlr-4.13.2-complete.jar" app.FlaskCompiler tests\app.py
#
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

New-Item -ItemType Directory -Force out\classes | Out-Null

# ── Step 1: regenerate the parsers from the .g4 grammars ───────────────────
# Without this, editing a grammar has no effect: src\antlr\*.java is committed
# and javac would just recompile the stale copies.
#
# templateFragments.g4 is not listed: it is a fragment-only grammar pulled in
# by `import templateFragments;` inside templateLexer.g4, so ANTLR resolves it
# from the grammars\ directory and emits no file of its own.
#
# No -package flag: all four grammars already declare `package antlr;` in their
# @header, and -package would emit it a second time, which does not compile.
#
# Grammar paths use forward slashes on purpose, so the "Generated from" comment
# ANTLR writes into line 1 is identical whether build.ps1 or build.sh produced
# it. Backslashes here would churn that line against every Linux build.
$grammars = @(
    "grammars/pythonLexer.g4",
    "grammars/pythonParser.g4",
    "grammars/templateLexer.g4",
    "grammars/templateParser.g4"
)

java -jar dependencies\antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -o src\antlr $grammars
if ($LASTEXITCODE -ne 0) { throw "ANTLR failed with exit code $LASTEXITCODE" }

Write-Host "ANTLR OK  -> src\antlr"

$sources = Get-ChildItem -Path src -Filter *.java -Recurse | ForEach-Object { $_.FullName }
$listFile = Join-Path $PSScriptRoot "out\sources.generated.txt"

# Write UTF-8 *without* a BOM: javac cannot parse an argfile that starts with one.
[System.IO.File]::WriteAllLines($listFile, $sources, (New-Object System.Text.UTF8Encoding $false))

javac -nowarn -cp dependencies\antlr-4.13.2-complete.jar -d out\classes "@$listFile"
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

Write-Host "Build OK -> out\classes"
