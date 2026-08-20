# Integration check: proves semantic analysis and code generation work together.
#
# The contract between the two phases is:
#   valid app    -> [Semantic] finds nothing -> [CodeGen] produces HTML
#   invalid app  -> [Semantic] reports it    -> [CodeGen] refuses to run
#
# Run .\build.ps1 first, then .\check.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$CP = "out\classes;dependencies\antlr-4.13.2-complete.jar"
if (-not (Test-Path "out\classes\app\FlaskCompiler.class")) {
    throw "Not built yet - run .\build.ps1 first"
}

$failures = 0

# --- Part 1: a valid app must generate HTML -------------------------------
Write-Host "`n[1] Valid app (tests\app.py) should GENERATE:" -ForegroundColor Cyan
$out = java -cp $CP app.FlaskCompiler tests\app.py 2>&1 | Out-String

if ($out -match "\[Semantic\] No errors found") {
    Write-Host "    ok   semantic analysis passed it" -ForegroundColor Green
} else {
    Write-Host "    FAIL semantic analysis reported errors on a valid app" -ForegroundColor Red; $failures++
}

if ($out -match "\[CodeGen\] Generated (\d+) HTML files" -and [int]$Matches[1] -gt 0) {
    Write-Host "    ok   code generation produced $($Matches[1]) files" -ForegroundColor Green
} else {
    Write-Host "    FAIL code generation produced nothing" -ForegroundColor Red; $failures++
}

# Rendering really happened if no Jinja tags survive in the output.
$leftover = Get-ChildItem out\generated\*.html -ErrorAction SilentlyContinue |
            Select-String -Pattern '\{\{|\{%' -List
if ($leftover) {
    Write-Host "    FAIL un-rendered Jinja left in: $($leftover.Filename -join ', ')" -ForegroundColor Red; $failures++
} else {
    Write-Host "    ok   all Jinja fully rendered, no tags left behind" -ForegroundColor Green
}

# --- Part 2: every invalid app must be caught AND block codegen -----------
$fixtures = Get-ChildItem tests\test_*.py
Write-Host "`n[2] Invalid apps ($($fixtures.Count) fixtures) should be CAUGHT and BLOCK codegen:" -ForegroundColor Cyan

foreach ($f in $fixtures) {
    $o = java -cp $CP app.FlaskCompiler "tests\$($f.Name)" 2>&1 | Out-String
    $caught  = $o -match "\[Semantic\] \d+ error\(s\) found"
    $blocked = $o -match "Skipping code generation"

    if ($caught -and $blocked) { continue }

    if (-not $caught)  { Write-Host "    FAIL $($f.Name): error not detected" -ForegroundColor Red }
    elseif (-not $blocked) { Write-Host "    FAIL $($f.Name): error found but codegen ran anyway" -ForegroundColor Red }
    $failures++
}
if ($failures -eq 0) { Write-Host "    ok   all $($fixtures.Count) caught, all blocked codegen" -ForegroundColor Green }

# --- Verdict --------------------------------------------------------------
Write-Host ""
if ($failures -eq 0) {
    Write-Host "PASS - semantic analysis and code generation are working together." -ForegroundColor Green
    exit 0
} else {
    Write-Host "FAIL - $failures problem(s) found." -ForegroundColor Red
    exit 1
}
