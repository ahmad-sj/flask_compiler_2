# End-to-end check for the compiler pipeline.
#
# Covers the three things that can independently break:
#   1. the full project builds and produces the specified output layout
#   2. semantic analysis catches bad backends AND blocks generation
#   3. Jinja control flow renders correctly (if/elif/else, for/for-else)
#
# Run .\build.ps1 first, then .\check.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$CP = "out\classes;dependencies\antlr-4.13.2-complete.jar"
if (-not (Test-Path "out\classes\app\FlaskCompiler.class")) {
    throw "Not built yet - run .\build.ps1 first"
}

$failures = 0
function Fail($msg) { Write-Host "    FAIL $msg" -ForegroundColor Red; $script:failures++ }
function Ok($msg)   { Write-Host "    ok   $msg" -ForegroundColor Green }

function Run-Compiler($inputPath, $outDir, $coDir) {
    # stderr is folded in so a stack trace shows up in the captured output.
    return (java -cp $CP app.FlaskCompiler $inputPath $outDir $coDir 2>&1 | Out-String)
}

# ── 1. Full project ────────────────────────────────────────────────────────
Write-Host "`n[1] Full project (project\ -> output\ + compiler_output\):" -ForegroundColor Cyan

Remove-Item -Recurse -Force output, compiler_output -ErrorAction SilentlyContinue
$out = Run-Compiler "project" "output" "compiler_output"

if ($out -match "No semantic errors found") { Ok "backend passed semantic analysis" }
else { Fail "semantic analysis reported errors on a valid project" }

# Every page the spec calls for, plus the per-item pages.
$expectedPages = @("index.html", "add_product.html",
                   "product_detail_1.html", "product_detail_2.html", "product_detail_3.html",
                   "edit_product_1.html", "edit_product_2.html", "edit_product_3.html")
$missingPages = $expectedPages | Where-Object { -not (Test-Path "output\$_") }
if ($missingPages) { Fail "missing generated pages: $($missingPages -join ', ')" }
else { Ok "all $($expectedPages.Count) expected pages generated" }

# Static assets must be copied through untransformed.
foreach ($asset in @("app.py", "style.css", "script.js")) {
    if (-not (Test-Path "output\$asset")) { Fail "static asset not copied: $asset"; continue }
    $src = Get-FileHash "project\$asset" -Algorithm MD5
    $dst = Get-FileHash "output\$asset" -Algorithm MD5
    if ($src.Hash -ne $dst.Hash) { Fail "$asset was modified during copy" }
}
if ($failures -eq 0) { Ok "app.py / style.css / script.js copied byte-identical" }

# compiler_output artifacts.
foreach ($artifact in @("ast_python.json", "ast_jinja.json", "semantic_report.txt", "generation_log.txt")) {
    if (-not (Test-Path "compiler_output\$artifact")) { Fail "missing artifact: $artifact" }
}
Ok "compiler_output artifacts present"

# The AST dumps must be valid JSON, not just non-empty.
foreach ($json in @("ast_python.json", "ast_jinja.json")) {
    try { Get-Content "compiler_output\$json" -Raw | ConvertFrom-Json | Out-Null }
    catch { Fail "$json is not valid JSON: $($_.Exception.Message)" }
}
Ok "AST dumps parse as valid JSON"

# Rendering completeness.
$leftover = Get-ChildItem output\*.html | Select-String -Pattern '\{\{|\{%' -List
if ($leftover) { Fail "un-rendered Jinja left in: $($leftover.Filename -join ', ')" }
else { Ok "no un-rendered Jinja tags remain" }

$noDoctype = Get-ChildItem output\*.html | Where-Object {
    -not (Select-String -Path $_.FullName -Pattern '<!DOCTYPE' -Quiet) }
if ($noDoctype) { Fail "pages missing DOCTYPE: $($noDoctype.Name -join ', ')" }
else { Ok "every page declares a DOCTYPE" }

# ── 2. Semantic fixtures ───────────────────────────────────────────────────
$fixtures = Get-ChildItem tests\test_*.py
Write-Host "`n[2] Invalid backends ($($fixtures.Count) fixtures) must be caught AND block generation:" -ForegroundColor Cyan

$before = $failures
foreach ($f in $fixtures) {
    $o = Run-Compiler "tests\$($f.Name)" "out\scratch\pages" "out\scratch\co"
    if ($o -notmatch "semantic error\(s\)")            { Fail "$($f.Name): error not detected" }
    elseif ($o -notmatch "must be fixed before generating") { Fail "$($f.Name): error found but generation ran anyway" }
}
if ($failures -eq $before) { Ok "all $($fixtures.Count) caught, all blocked generation" }

# ── 3. Jinja control flow ──────────────────────────────────────────────────
Write-Host "`n[3] Jinja control flow (if / elif / else, for / for-else):" -ForegroundColor Cyan

$appPy = "tests\render_project\app.py"
$original = Get-Content $appPy -Raw

# stock value, items literal, substring the rendered page must contain
$cases = @(
    @{ desc = "if branch";    stock = 50; items = "[]";                 expect = "plenty" },
    @{ desc = "elif branch";  stock = 5;  items = "[]";                 expect = "low" },
    @{ desc = "else branch";  stock = 0;  items = "[]";                 expect = "out of stock" },
    @{ desc = "for-else";     stock = 0;  items = "[]";                 expect = "nothing here" },
    @{ desc = "for body";     stock = 0;  items = '["alpha", "beta"]';  expect = "alpha" }
)

try {
    foreach ($c in $cases) {
        $src = $original -replace '(?m)^stock = .*$', "stock = $($c.stock)" `
                         -replace '(?m)^items = .*$', "items = $($c.items)"
        Set-Content $appPy -Value $src -Encoding utf8 -NoNewline

        Remove-Item -Recurse -Force "tests\render_project\out" -ErrorAction SilentlyContinue
        Run-Compiler "tests\render_project" "tests\render_project\out" "tests\render_project\co" | Out-Null

        $page = "tests\render_project\out\branches.html"
        if (-not (Test-Path $page)) { Fail "$($c.desc): no page generated"; continue }

        $html = (Get-Content $page -Raw) -replace '\s+', ' '
        if ($html -notmatch [regex]::Escape($c.expect)) {
            Fail "$($c.desc): expected '$($c.expect)' in output"
        } else {
            Ok "$($c.desc) -> '$($c.expect)'"
        }
    }
} finally {
    # Always restore the fixture, even if a case threw.
    Set-Content $appPy -Value $original -Encoding utf8 -NoNewline
    Remove-Item -Recurse -Force "tests\render_project\out", "tests\render_project\co" -ErrorAction SilentlyContinue
    Remove-Item -Recurse -Force "out\scratch" -ErrorAction SilentlyContinue
}

# ── 4. Browser runtime (add / edit / delete via localStorage) ──────────────
Write-Host "`n[4] Browser runtime (add / edit / delete):" -ForegroundColor Cyan

$node = Get-Command node -ErrorAction SilentlyContinue
$hasJsdom = $false
if ($node) {
    node -e "require.resolve('jsdom')" 2>$null | Out-Null
    $hasJsdom = ($LASTEXITCODE -eq 0)
}

if (-not $node) {
    Write-Host "    skip - node not on PATH" -ForegroundColor DarkGray
} elseif (-not $hasJsdom) {
    Write-Host "    skip - jsdom not installed (run: npm install jsdom)" -ForegroundColor DarkGray
} else {
    # The full project must be built first; section 1 already did that.
    $runtime = node tests\runtime-test.js "output" 2>&1 | Out-String
    Write-Host ($runtime.TrimEnd())
    if ($LASTEXITCODE -ne 0) { $failures++ }
}

# ── Verdict ────────────────────────────────────────────────────────────────
Write-Host ""
if ($failures -eq 0) {
    Write-Host "PASS - pipeline works end to end." -ForegroundColor Green
    exit 0
} else {
    Write-Host "FAIL - $failures problem(s) found." -ForegroundColor Red
    exit 1
}
