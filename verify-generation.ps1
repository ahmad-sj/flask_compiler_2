# Proves the generator and the regeneration are correct - or shows exactly where they are not.
#
# check.ps1 proves the pipeline RUNS. This proves the OUTPUT IS RIGHT, which is
# a different question. It covers the eight properties a code generator has to
# have, and the six a regenerator has to have.
#
#   .\build.ps1
#   .\verify-generation.ps1
#
# Everything destructive happens in out\verify\ on a copy of project\.
# output\ is regenerated once at the start and then only read.
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

$CP = "out\classes;dependencies\antlr-4.13.2-complete.jar"
if (-not (Test-Path "out\classes\app\FlaskCompiler.class")) {
    throw "Not built yet - run .\build.ps1 first"
}

$pass = 0; $fail = 0
function Ok($m)   { Write-Host "    ok   $m" -ForegroundColor Green;  $script:pass++ }
function Bad($m)  { Write-Host "    FAIL $m" -ForegroundColor Red;    $script:fail++ }
function Head($m) { Write-Host "`n$m" -ForegroundColor Cyan }

function Compile($inDir, $outDir, $coDir) {
    java -cp $CP app.FlaskCompiler $inDir $outDir $coDir --quiet-ast 2>&1 | Out-String
}
function Hashes($dir) { Get-ChildItem "$dir\*.html" | Get-FileHash | Select-Object Path, Hash }

# Fresh baseline in output\, so the static checks below describe the real site.
Remove-Item -Recurse -Force output, compiler_output -ErrorAction SilentlyContinue
$log = Compile "project" "output" "compiler_output"

$W = "out\verify"
Remove-Item -Recurse -Force $W -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $W | Out-Null

# ══════════════════════════════════════════════════════════════════════════
Head "GENERATION - is the output correct?"
# ══════════════════════════════════════════════════════════════════════════

# --- 1. Completeness: one page per route, per item, plus the shell page -----
$routes = ([regex]::Matches($log, '(?m)^\s{4}(\w+) ')) | ForEach-Object { $_.Groups[1].Value }
# Only literal ids in the data list. "id": next_id() inside the add route is a
# call, not a product, and counting it made this script report a phantom item.
$items  = ([regex]::Matches((Get-Content project\app.py -Raw), '"id"\s*:\s*\d')).Count
$pages  = (Get-ChildItem output\*.html).Count
# index + add + (detail: items + shell) + (edit: items + shell)
$expected = 2 + ($items + 1) * 2
if ($pages -eq $expected) { Ok "page count: $pages = 2 static + 2 parameterized routes x ($items items + 1 shell)" }
else { Bad "page count: expected $expected, found $pages" }

# --- 2. No template syntax survived into the output ------------------------
$leftover = Get-ChildItem output\*.html | Select-String -Pattern '\{\{|\{%' -List
if ($leftover) { Bad "un-rendered Jinja in: $($leftover.Filename -join ', ')" }
else { Ok "no {{ }} or {% %} left anywhere" }

# --- 3. Data fidelity: every value in the source reached a page ------------
$src = Get-Content project\app.py -Raw
$names  = [regex]::Matches($src, '"name"\s*:\s*"([^"]+)"')  | ForEach-Object { $_.Groups[1].Value }
$prices = [regex]::Matches($src, '"price"\s*:\s*([0-9.]+)') | ForEach-Object { $_.Groups[1].Value }
$index  = Get-Content output\index.html -Raw
$missingNames = $names | Where-Object { $index -notlike "*$_*" }
if ($missingNames) { Bad "names missing from index.html: $($missingNames -join ', ')" }
else { Ok "all $($names.Count) product names present in index.html" }

# Prices go through {{ "%.2f"|format(...) }}, so compare formatted.
$missingPrices = @()
foreach ($p in $prices) {
    $formatted = "{0:F2}" -f [double]$p
    if ($index -notlike "*$formatted*") { $missingPrices += "$p (as $formatted)" }
}
if ($missingPrices) { Bad "prices missing or misformatted: $($missingPrices -join ', ')" }
else { Ok "all $($prices.Count) prices rendered through the format filter correctly" }

# --- 4. Referential integrity: every local link resolves to a real file ----
# check.ps1 does not test this. A url_for that resolves to a filename which was
# never written is a broken site that still reports success.
$broken = @()
foreach ($page in Get-ChildItem output\*.html) {
    $html = Get-Content $page.FullName -Raw
    foreach ($m in [regex]::Matches($html, '(?:href|src)="([^"]+)"')) {
        $target = $m.Groups[1].Value
        if ($target -match '^(https?:|//|#|mailto:|data:|javascript:)') { continue }
        if (-not (Test-Path (Join-Path "output" $target))) {
            $broken += "$($page.Name) -> $target"
        }
    }
}
if ($broken) { Bad "dead local links:`n         $($broken -join "`n         ")" }
else { Ok "every local href/src resolves to a file that exists" }

# --- 5. Inheritance applied on every page ---------------------------------
# The marker is read out of base.jinja rather than hardcoded, so editing the
# template does not turn this into a false failure.
$banner = ([regex]::Match((Get-Content project\templates\base.jinja -Raw), '<h1>([^<]+)</h1>')).Groups[1].Value.Trim()
if (-not $banner) { Bad "could not find an <h1> in base.jinja to check inheritance against" }
else {
    $noBase = Get-ChildItem output\*.html | Where-Object {
        (Get-Content $_.FullName -Raw) -notlike "*$banner*" }
    if ($noBase) { Bad "base.jinja not applied to: $($noBase.Name -join ', ')" }
    else { Ok "base.jinja applied to all $((Get-ChildItem output\*.html).Count) pages (banner: '$banner')" }
}

# --- 6. The loop ran the right number of times ----------------------------
$cards = ([regex]::Matches($index, 'class="card"')).Count
if ($cards -eq $items) { Ok "{% for %} produced exactly $cards cards for $items products" }
else { Bad "{% for %} produced $cards cards for $items products" }

# --- 7. Static assets copied byte-identical -------------------------------
$assetOk = $true
foreach ($a in @("app.py", "style.css")) {
    if (-not (Test-Path "output\$a")) { Bad "asset not copied: $a"; $assetOk = $false; continue }
    if ((Get-FileHash "project\$a").Hash -ne (Get-FileHash "output\$a").Hash) {
        Bad "$a was modified during copy"; $assetOk = $false
    }
}
if ($assetOk) { Ok "app.py and style.css copied byte-identical" }

# --- 8. The renderer reported no problems ---------------------------------
$report = Get-Content compiler_output\semantic_report.txt -Raw
if ($report -match 'Rendering problems: 0') { Ok "zero rendering problems reported" }
else {
    $n = ([regex]::Match($report, 'Rendering problems: (\d+)')).Groups[1].Value
    Bad "$n rendering problem(s) - see compiler_output\semantic_report.txt"
}

# ══════════════════════════════════════════════════════════════════════════
Head "REGENERATION - does a second run behave?"
# ══════════════════════════════════════════════════════════════════════════

Copy-Item -Recurse project "$W\proj"
Compile "$W\proj" "$W\a" "$W\co" | Out-Null
$baseline = Hashes "$W\a"

# --- 9. Idempotent: same input, same bytes --------------------------------
Compile "$W\proj" "$W\a" "$W\co" | Out-Null
$second = Hashes "$W\a"
if ($null -eq (Compare-Object $baseline $second -Property Hash)) {
    Ok "idempotent: two runs on unchanged input are byte-identical"
} else { Bad "not idempotent: output changed with no input change" }

# --- 10. Minimal: editing one value touches only what mentions it ---------
$appPath = (Resolve-Path "$W\proj\app.py").Path
$before  = [System.IO.File]::ReadAllText($appPath)
$utf8    = New-Object System.Text.UTF8Encoding $false
[System.IO.File]::WriteAllText($appPath, $before.Replace('"price": 79.99', '"price": 61.50'), $utf8)
Compile "$W\proj" "$W\a" "$W\co" | Out-Null
$changed = (Compare-Object $baseline (Hashes "$W\a") -Property Path, Hash |
            Where-Object { $_.SideIndicator -eq '=>' }).Path | Split-Path -Leaf
$shouldChange = @("index.html", "product_detail_1.html", "edit_product_1.html")
$extra   = $changed | Where-Object { $shouldChange -notcontains $_ }
$missed  = $shouldChange | Where-Object { $changed -notcontains $_ }
if (-not $extra -and -not $missed) {
    Ok "minimal: one price change rewrote exactly the 3 pages that show it"
} else {
    if ($extra)  { Bad "unrelated pages rewritten: $($extra -join ', ')" }
    if ($missed) { Bad "pages that show the price were NOT updated: $($missed -join ', ')" }
}
[System.IO.File]::WriteAllText($appPath, $before, $utf8)
Compile "$W\proj" "$W\a" "$W\co" | Out-Null

# --- 11. ADD: a new item creates its pages --------------------------------
$added = $before.Replace(
  '     "image": "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400"},',
  "     `"image`": `"https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400`"},`r`n`r`n    {`"id`": 99, `"name`": `"VERIFY WIDGET`", `"price`": 12.50,`r`n     `"details`": `"Added by verify-generation.ps1.`",`r`n     `"image`": `"https://example.com/w.png`"},")
[System.IO.File]::WriteAllText($appPath, $added, $utf8)
Compile "$W\proj" "$W\a" "$W\co" | Out-Null
$created = (Test-Path "$W\a\product_detail_99.html") -and (Test-Path "$W\a\edit_product_99.html")
$listed  = (Get-Content "$W\a\index.html" -Raw) -like "*VERIFY WIDGET*"
if ($created -and $listed) { Ok "add: new item created its 2 pages and appears on the index" }
else { Bad "add: pages created=$created, listed on index=$listed" }

# --- 12. EDIT: changing an item rewrites its pages ------------------------
[System.IO.File]::WriteAllText($appPath, $added.Replace('"VERIFY WIDGET"', '"RENAMED WIDGET"'), $utf8)
Compile "$W\proj" "$W\a" "$W\co" | Out-Null
$detail = Get-Content "$W\a\product_detail_99.html" -Raw
if (($detail -like "*RENAMED WIDGET*") -and ($detail -notlike "*VERIFY WIDGET*")) {
    Ok "edit: rename propagated and the old value is gone"
} else { Bad "edit: product_detail_99.html still shows the old name" }

# --- 13. DELETE: removing an item must remove its pages ------------------
[System.IO.File]::WriteAllText($appPath, $before, $utf8)
Compile "$W\proj" "$W\a" "$W\co" | Out-Null
$orphans = @()
foreach ($f in @("product_detail_99.html", "edit_product_99.html")) {
    if (Test-Path "$W\a\$f") { $orphans += $f }
}
if ($orphans) {
    Bad "delete: $($orphans -join ' and ') left behind - the generator never removes pages"
    Write-Host "         (CodeGenerator has no delete step; check.ps1 hides this by" -ForegroundColor DarkGray
    Write-Host "          wiping output\ before every run. Fix belongs in generate().)" -ForegroundColor DarkGray
} else { Ok "delete: removed item's pages were deleted" }

# --- 14. Deterministic across a clean rebuild ----------------------------
Remove-Item -Recurse -Force "$W\b" -ErrorAction SilentlyContinue
Compile "$W\proj" "$W\b" "$W\co2" | Out-Null
$fromScratch = Hashes "$W\b" | ForEach-Object { $_.Hash }
$reused      = (Hashes "$W\a" | Where-Object { (Split-Path $_.Path -Leaf) -notlike "*_99.html" }) |
               ForEach-Object { $_.Hash }
if ($null -eq (Compare-Object $fromScratch $reused)) {
    Ok "deterministic: a clean build equals an incremental one (ignoring orphans)"
} else { Bad "a clean build differs from an incremental build" }

# ══════════════════════════════════════════════════════════════════════════
Write-Host ""
if ($fail -eq 0) {
    Write-Host "PASS - $pass checks, generation and regeneration are correct." -ForegroundColor Green
} else {
    Write-Host "$pass passed, $fail FAILED." -ForegroundColor Yellow
    Write-Host "A failure here is a real defect, not a flaky test - read the line above it." -ForegroundColor Yellow
}
