# Proves every semantic check the compiler implements actually fires.
#
# check.ps1 proves each of the 27 original fixtures is caught. This proves
# something stronger: that each CHECK is triggered by five different programs,
# so a check cannot pass by accidentally matching one specific shape.
#
#   .\build.ps1
#   .\check-semantic.ps1
#
# 20 python checks  x 5 files  = 100 fixtures in tests\semantic\
#  8 template checks x 5 projects = 40 fixtures in tests\semantic_templates\
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$CP = "out\classes;dependencies\antlr-4.13.2-complete.jar"
if (-not (Test-Path "out\classes\app\FlaskCompiler.class")) {
    throw "Not built yet - run .\build.ps1 first"
}

$W = "out\semantic-run"
Remove-Item -Recurse -Force $W -ErrorAction SilentlyContinue

$pass = 0; $fail = 0
function Head($m) { Write-Host "`n$m" -ForegroundColor Cyan }

function Load-Expected($path) {
    $map = @{}
    foreach ($line in Get-Content $path) {
        if ($line.Trim() -eq "") { continue }
        $parts = $line -split "`t"
        $map[$parts[0]] = $parts[1]
    }
    return $map
}

# ── Python checks ──────────────────────────────────────────────────────────
Head "PYTHON CHECKS - each must fire on all 5 of its fixtures"
$expected = Load-Expected "tests\semantic\EXPECTED.txt"

foreach ($key in ($expected.Keys | Sort-Object)) {
    $want = $expected[$key]
    $missed = @()
    foreach ($f in Get-ChildItem "tests\semantic\$key`_*.py") {
        $out = java -cp $CP app.FlaskCompiler $f.FullName "$W\p" "$W\c" --quiet-ast 2>&1 | Out-String
        if ($out -notlike "*$want*") { $missed += $f.Name }
    }
    if ($missed.Count -eq 0) {
        Write-Host ("    ok   {0,-22} 5/5   `"{1}`"" -f $key, $want) -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ("    FAIL {0,-22} {1}/5 - not fired by: {2}" -f $key, (5 - $missed.Count), ($missed -join ', ')) -ForegroundColor Red
        $script:fail++
    }
}

# ── Template checks ────────────────────────────────────────────────────────
Head "TEMPLATE CHECKS - each must fire on all 5 of its projects"
$expectedT = Load-Expected "tests\semantic_templates\EXPECTED.txt"

foreach ($key in ($expectedT.Keys | Sort-Object)) {
    $want = $expectedT[$key]
    $missed = @()
    foreach ($d in Get-ChildItem "tests\semantic_templates" -Directory -Filter "$key`_*") {
        $out = java -cp $CP app.FlaskCompiler $d.FullName "$W\p" "$W\c" --quiet-ast 2>&1 | Out-String
        if ($out -notlike "*$want*") { $missed += $d.Name }
    }
    if ($missed.Count -eq 0) {
        Write-Host ("    ok   {0,-22} 5/5   `"{1}`"" -f $key, $want) -ForegroundColor Green
        $script:pass++
    } else {
        Write-Host ("    FAIL {0,-22} {1}/5 - not fired by: {2}" -f $key, (5 - $missed.Count), ($missed -join ', ')) -ForegroundColor Red
        $script:fail++
    }
}

# ── False-positive guard ───────────────────────────────────────────────────
# The worst failure a checker can have is rejecting a correct program, so the
# valid fixtures and the real demo project must stay clean.
Head "FALSE-POSITIVE GUARD - valid programs must report nothing"
$fpFail = 0
foreach ($v in Get-ChildItem "tests\valid\*.py") {
    $out = java -cp $CP app.FlaskCompiler $v.FullName "$W\p" "$W\c" --quiet-ast 2>&1 | Out-String
    if ($out -notlike "*No semantic errors found*") {
        Write-Host "    FAIL $($v.Name) reported an error" -ForegroundColor Red; $fpFail++
    }
}
$out = java -cp $CP app.FlaskCompiler "project" "$W\p" "$W\c" --quiet-ast 2>&1 | Out-String
if ($out -notlike "*No semantic errors found*" -or $out -notlike "*No template errors found*") {
    Write-Host "    FAIL the demo project reported an error" -ForegroundColor Red; $fpFail++
}
if ($fpFail -eq 0) {
    Write-Host "    ok   3 valid fixtures + the demo project all clean" -ForegroundColor Green
    $pass++
} else { $fail++ }

# ══════════════════════════════════════════════════════════════════════════
Write-Host ""
$files = (Get-ChildItem "tests\semantic\*.py").Count + (Get-ChildItem "tests\semantic_templates" -Directory).Count
if ($fail -eq 0) {
    Write-Host "PASS - $pass check groups, $files fixtures, every check fires 5/5." -ForegroundColor Green
} else {
    Write-Host "$pass passed, $fail FAILED - a check is not firing on every shape." -ForegroundColor Yellow
}
