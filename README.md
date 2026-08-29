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

## Working add / edit / delete

The generated site is static, so those actions have no server to post to. The
runtime in `script.js` keeps the collection in `localStorage` instead — the same
idea as stashing an auth token there. It is seeded once from `data.js` (which
the compiler emits from the build-time data) and is the source of truth from
then on.

| Page | Behaviour |
| --- | --- |
| index | list re-rendered from storage, so it shows added/edited/deleted items |
| add | form intercepted, item appended with the next free key, back to index |
| edit | form pre-filled from storage, saved back, on to the detail page |
| detail | fields hydrated from storage, so edits are visible |
| delete | confirms, removes from storage, back to index |

Items created in the browser have no pre-rendered page — the compiler never saw
them — so the generator also emits an un-suffixed shell page per parameterized
route (`product_detail.html`, `edit_product.html`). The runtime sends new items
there as `product_detail.html?product_id=4` and fills in the fields.

Pre-rendered per-item pages are still generated and still contain real content,
so the site reads correctly with JavaScript disabled.

Nothing in `script.js` names `product` or `products`. The collection, its key
field and each route's page all come from the metadata in `data.js`, so it works
for whatever model the backend defines.

Storage is reset by clearing the site's `localStorage`, or from the console:

```js
localStorage.removeItem("flask-compiler-site"); location.reload();
```

## Verify

```powershell
.\check.ps1
```

Six groups:

| Group | What it proves |
| --- | --- |
| Full project | builds to the specified layout, assets copied byte-identical, valid JSON, DOCTYPE, no un-rendered Jinja |
| Invalid backends (`tests/test_*.py`) | 27 faulty programs caught **and** blocking generation |
| Valid backends (`tests/valid/`) | legal programs are **not** rejected — a false-positive guard |
| Broken templates (`tests/bad_templates/`) | template faults caught **and** blocking generation |
| Jinja control flow | `if` / `elif` / `else` and `for` / `for-else` all render |
| Browser runtime | add / edit / delete persist across page loads |

The false-positive group is the one that catches the worst class of bug: a
checker that rejects a valid program blocks a build that should have succeeded.

The last group drives the real generated pages in jsdom. It needs `npm install`
once; without it that group is skipped and the rest still runs.

## Known limitations

Each front-end parses a deliberate **subset** of its language — enough for a Flask
data-and-routes backend and the templates that render it, not the whole language.
Everything below is a real, reproducible boundary, not a rough edge.

### Python subset

Arithmetic is complete apart from augmented assignment. Unary sign,
exponentiation and floor division all parse with Python's own precedence and
associativity:

| Supported | Parses as |
| --- | --- |
| `x = -5` | unary minus |
| `x = 2 ** 3 ** 2` | `2 ** (3 ** 2)` — right-associative |
| `x = -2 ** 2` | `-(2 ** 2)` — `**` binds tighter than the sign |
| `x = a * b // c` | `(a * b) // c` — left-associative |

Still a parse error:

| Not supported | Example |
| --- | --- |
| augmented assignment | `x += 2` |

Missing constructs:

- f-strings and triple-quoted strings (`STRING` has no prefix or `"""` form)
- list/dict comprehensions — `[p for p in ps]`. Generator expressions **in
  parentheses** *are* supported, which is what `next((p for p in products …))`
  in a route needs.
- `class` — the `CLASS` token is defined in the lexer but no parser rule uses it
- `with`, `try`/`except`, `del`, `global`, `lambda`, `yield`
- default arguments (`def f(a, b=2)`) and `*args` / `**kwargs`
- stacked decorators — exactly one decorator per function, which is all
  `@app.route(...)` needs
- dict keys must be literals, so `{k: 1}` with a variable key is rejected
- tuple unpacking (`a, b = 1, 2`)

One known precedence deviation: `not` binds **tighter** than comparison, so
`not a == b` parses as `(not a) == b` where Python means `not (a == b)`.

### Jinja subset

Supported: `{{ }}`, `{% if %}`/`{% elif %}`/`{% else %}`, `{% for %}` (with
`{% else %}`), `{% set %}`, `{% extends %}`, `{% block %}`, filters with
arguments, and `loop.index` / `index0` / `revindex` / `first` / `last` / `length`.

Not supported:

- `{% include %}` and `{% macro %}`
- whitespace control — `{%- … -%}`
- Jinja's conditional expression `{{ a if cond else b }}`. This grammar uses the
  C-style form **`{{ cond ? a : b }}`** instead, plus `??` for defaulting. Both
  are deviations from real Jinja syntax.
- a `#` inside a `{# comment #}` terminates it early

### HTML subset

- Tag names come from a **closed list** in `templateFragments.g4`. Anything
  outside it — SVG elements, custom elements — fails to lex.
- Void elements **must be self-closed**: write `<img src="a.png" />` and
  `<meta charset="UTF-8" />`, not the bare form.
- `<script>` bodies cannot contain `{`, `}`, `<` or `>`, so inline JavaScript
  will not lex. Keep JS in `script.js`, which is copied rather than parsed.
- Body text cannot contain a bare `{`, `}` or `>`; use `&gt;` and `&#123;`.
- A mismatched closing tag (`<p>x</div>`) is currently accepted and the end tag
  is discarded, so the output is silently repaired rather than reported.
- Values substituted into a page are **not HTML-escaped**. An `escape` / `e`
  filter exists but is opt-in, the reverse of Jinja's default; `|safe` is not
  recognised.

### CSS subset

- Only `<style>` blocks inside templates go through the CSS grammar.
  `style.css` is copied rather than parsed, and inline `style="…"` attributes
  use a separate, simpler path.
- No attribute selectors (`[attr*="v"]`), no functional pseudo-classes
  (`:not(...)`), no `>` / `+` / `~` combinators, no `@media` or other at-rules,
  no CSS comments.
- Property values cannot contain quotes, so `font-family: "Segoe UI"` is
  rejected. Use unquoted family names.

### Data extraction and URLs

- Data extraction is static. Values computed at request time — anything derived
  from `request.form`, a database, or arbitrary control flow — are not knowable
  at build time and resolve to null. Arithmetic, string concatenation and
  function calls in a data definition are not evaluated either; only literals,
  lists, dicts and references to other module-level names are.
- `url_for()` pointing at a route that renders no template (a POST-only endpoint
  such as `delete`) resolves to `#`, since no static page exists for it. This is
  reported in `semantic_report.txt`.
- `url_for('static', filename=…)` is **not** recognised — `static` is not a user
  route, so it is reported as an unknown route and blocks the build. Link assets
  directly (`href="style.css"`), which resolves correctly in the flat output.
