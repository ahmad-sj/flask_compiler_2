# Flask → Static Site Compiler

Takes a Flask-style `app.py` plus Jinja templates and produces fully rendered
static HTML. Written in Java on top of ANTLR 4.

## Pipeline

```
app.py ──▶ Python parser ──▶ Python AST ──┐
                                          ├──▶ semantic analysis ──▶ semantic_report.txt
templates/*.jinja ──▶ Jinja parser ──▶ Jinja AST            │
                                                            ▼   (blocks generation on error)
                                            data extraction ──▶ context
                                                            ▼
                                                Jinja rendering ──▶ output/*.html
```

Each stage is an explicit phase in `FlaskCompiler`. Data extraction walks the
Python AST directly (no Python interpreter is required), and rendering walks the
Jinja AST — templates are never re-serialized to text and re-parsed.

The symbol table is used **only** by semantic analysis. Generation resolves
names against the extracted context, exactly as the spec requires.

## Build

```powershell
.\build.ps1      # Windows
```
```sh
./build.sh       # Linux / macOS / Git Bash
```

Sources are globbed at build time, so no file list needs maintaining.

## Run

```powershell
java -cp "out\classes;dependencies\antlr-4.13.2-complete.jar" app.FlaskCompiler [input] [output] [compilerOutput]
```

Defaults are `project`, `output` and `compiler_output`. `input` may be either a
project directory or a single `.py` file — the latter runs parsing and semantic
analysis only, which is how the `tests/test_*.py` fixtures are checked.

Exit code is `0` on success, `1` on a compilation problem, `2` on I/O failure.

## Input layout

```
project/
├── app.py                    backend data + routes
├── templates/
│   ├── base.jinja
│   ├── index.jinja
│   ├── add_product.jinja
│   ├── edit_product.jinja
│   └── product_detail.jinja
├── style.css                 copied verbatim
└── script.js                 copied verbatim
```

Templates are discovered by scanning the directory. `.jinja`, `.jinja2`, `.html`
and `.htm` are all recognised.

## Output layout

```
output/                       the deliverable
├── index.html
├── add_product.html
├── product_detail_1.html     one page per item for parameterized routes
├── product_detail_2.html
├── product_detail_3.html
├── edit_product_1.html
├── edit_product_2.html
├── edit_product_3.html
├── app.py                    copied unchanged
├── style.css                 copied unchanged
└── script.js                 copied unchanged

compiler_output/              intermediate analysis artifacts
├── ast_python.json
├── ast_jinja.json
├── semantic_report.txt
└── generation_log.txt
```

### Page naming

A route's page is named after its template. A route whose URL carries a
parameter, such as `/product/<int:product_id>`, produces one page per item of
the collection it selects from, suffixed with the item's key:
`product_detail_1.html`, `product_detail_2.html`, and so on.

The collection is discovered from the generator expression in the route body —

```python
product = next((p for p in products if p["id"] == product_id), None)
```

— which yields collection `products`, item variable `product` and key `id`.
Nothing is hardcoded to any particular variable name.

## Supported template features

| Feature | Notes |
| --- | --- |
| `{{ expr }}` | full expression evaluation over the AST |
| `{% if %}` / `{% elif %}` / `{% else %}` | all branches |
| `{% for %}` / `{% else %}` | `else` renders when the sequence is empty |
| `{% extends %}` / `{% block %}` | multi-level, with cycle detection |
| `{% set %}` | scoped to the page being rendered |
| `loop.*` | `index`, `index0`, `first`, `last`, `length`, `revindex` |
| Filters | `format`, `upper`, `lower`, `title`, `trim`, `length`, `int`, `float`, `round`, `default`, `join`, `escape` |
| Tests | `defined`, `undefined`, `none`, `even`, `odd`, `string`, `number` |
| `url_for()` | resolves to the generated page, including per-item pages |

## Verify

```powershell
.\check.ps1
```

Checks that the project builds to the specified layout, that all 24 semantic
fixtures are caught **and** block generation, and that Jinja control flow
renders correctly.

## Known limitations

- The CSS grammar has no attribute selectors (`[attr*="v"]`) or functional
  pseudo-classes (`:not(...)`). Keep stylesheets in `style.css`, which is copied
  rather than parsed; only `<style>` blocks inside templates go through the CSS
  grammar.
- Data extraction is static. Values computed at request time — anything derived
  from `request.form`, a database, or arbitrary control flow — are not knowable
  at build time and resolve to null.
- `url_for()` pointing at a route that renders no template (a POST-only endpoint
  such as `delete`) resolves to `#`, since no static page exists for it. This is
  reported in `semantic_report.txt`.
