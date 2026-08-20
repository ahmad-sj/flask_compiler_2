# Team Roles

Five areas, split along compiler-phase boundaries so each person owns a stage
that can be explained, demoed and defended on its own.

| # | Owner | Area | Approx. lines |
| --- | --- | --- | ---: |
| 1 | **Melad** | Grammar, Lexer & Parser | ~965 grammar + generated |
| 2 | **Aram** | AST classes & Tree Printer | ~3,350 + ~400 |
| 3 | **Raghad** | Visitors (parse tree → AST) | ~2,120 |
| 4 | **Ahmad** | Symbol Table & Semantic Analysis | ~2,090 |
| 5 | **Yousef** | Code Generation & Demo App | ~2,170 + ~680 |

Shared by everyone: the build scripts, `check.ps1`, and keeping the reports
current.

---

## 1. Melad — Grammar, Lexer & Parser

Owns how source text becomes tokens and a parse tree. **Project requirement §1.**

**Files**
- `grammars/pythonLexer.g4`, `grammars/pythonParser.g4`
- `grammars/templateLexer.g4`, `grammars/templateParser.g4`, `grammars/templateFragments.g4`
- `src/antlr/**` — generated; regenerate, never hand-edit
- `src/antlr/Python3LexerBase.java` — the INDENT/DEDENT logic

**Responsibilities**
- Token and rule definitions for all four languages: Python, Jinja2, HTML, CSS
- The template lexer's mode machine — `EXPRESSION_MODE`, `J_STMNT_MODE`,
  `CSS_BLK`, `ATTR_VAL_QOUTED` and the transitions between them
- Regenerating the parser after any `.g4` change:
  ```sh
  cd grammars
  java -jar ../dependencies/antlr-4.13.2-complete.jar -Dlanguage=Java -visitor -o ../src/antlr templateLexer.g4 templateParser.g4
  ```

**Must be able to explain in the demo**
- Why Python needs synthetic INDENT/DEDENT tokens, and where they come from
- Why the template lexer uses modes instead of one flat token set
- The difference between the token stream and the parse tree
  (`compiler_output/tokens.txt` vs `parse_tree.txt`)

**Open work**
- CSS grammar has no attribute selectors (`[attr*="v"]`) or functional
  pseudo-classes (`:not(...)`) — see *Known limitations* in `README.md`

---

## 2. Aram — AST Classes & Tree Printer

Owns the shape of the tree and how it is displayed. **Requirements §2 and §5.**

**Files**
- `src/models/**` — 102 classes, all extending `Node`
- `src/models/Node.java` — node ID, node name/type, line number
- `src/app/TreePrinter.java` — the driver
- `src/app/AstDumper.java` — the JSON dumps

**Responsibilities**
- The class hierarchy and its use of inheritance and polymorphism
- Every node storing its **name/type, numeric ID and source line**
- A `print(int level)` override per node type, recursing into children
- The driver that walks the roots and calls them

**Must be able to explain in the demo**
- The hierarchy diagram in `REPORT.md` §2
- Why node IDs run lower for children than parents (the visitor builds children
  first, so IDs record construction order)
- Where node IDs appear: every node in the JSON, top-level headers in the text
- Why the AST survives execution intact — rendering only reads it

**Open work**
- The text tree shows node IDs only on top-level headers. Putting them on every
  line means touching all 66 `print()` methods.

---

## 3. Raghad — Visitors

Owns turning the parse tree into the AST. **Requirement §3.**

**Files**
- `src/visitors/AppVisitor.java` — Python parse tree → `App`
- `src/visitors/PythonVisitor.java` — Python rules → `models.python.*`
- `src/visitors/TemplateVisitor.java` — template root → `Template`
- `src/visitors/NodeVisitor.java` — Jinja + HTML + CSS rules → nodes

**Responsibilities**
- One visit method per grammar rule that produces a node
- Setting `nodeName` and `lineNumber` on everything constructed
- Keeping visitors in step with Melad's grammar: a renamed or relabelled rule
  changes the generated visitor interface

**Must be able to explain in the demo**
- How ANTLR's generated `BaseVisitor<T>` is specialised
- Why `{% elif %}` and `{% else %}` end up *inside* the if-body rather than as
  siblings — this follows the grammar, and the renderer depends on it
- Why operators must be captured, not just operands: they were dropped once,
  which made every `-` evaluate as `+`

**Interfaces**
- Consumes: Melad's generated parser
- Produces: Aram's node classes

---

## 4. Ahmad — Symbol Table & Semantic Analysis

Owns correctness checking. **Requirement §4.**

**Files**
- `src/symbols/SymbolTable.java`, `Scope.java`, `Symbol.java`, `SemanticError.java`
- `src/visitors/SemanticAnalyzer.java` — Python checks
- `src/visitors/TemplateSemanticAnalyzer.java` — template checks
- `tests/test_*.py`, `tests/valid/`, `tests/bad_templates/`

**Responsibilities**
- Symbol table operations: `define` (insert), `resolve`/`lookup`, `update`,
  `enterScope`/`exitScope`
- The Python checks: undefined variables, scope violations, redefinition,
  arity, type mismatch, unreachable code, recursion without a base case, and
  the Flask bootstrap checks
- The template checks: names not provided to a template, `url_for` naming an
  unknown route, missing `extends`/`render_template` targets, unknown filters
  and tests, loop variables used after `{% endfor %}`
- **Guarding against false positives** — `tests/valid/` holds legal programs
  that must report zero errors

**Must be able to explain in the demo**
- Why the symbol table is used *only* in analysis, never in generation
- Why module-level names are hoisted before any function body is analysed
  (without it, calling a function defined lower in the file was wrongly
  reported as undefined, and blocked builds that should have succeeded)
- Why a false positive is worse than a missed error

**Open work — not yet detected**
- Use before assignment (`total = total + 1` with `total` unassigned)
- A route that returns only on some branches, so it can return `None`
- A type mismatch nested inside a larger expression (`"text" + 5 * 2`)

Each has real false-positive risk; a check that misfires is worse than none.

---

## 5. Yousef — Code Generation & Demo App

Owns everything after analysis: producing the site.

**Files**
- `src/visitors/PythonDataExtractor.java` — AST → context data
- `src/visitors/JinjaRenderer.java` — AST walk → HTML
- `src/visitors/ExpressionEvaluator.java` — expression evaluation, filters, tests
- `src/app/CodeGenerator.java` — page planning, asset copying, `data.js`
- `src/models/RouteInfo.java` — the route model
- `project/**` — the demo app, templates, `style.css`, `script.js`
- `tests/runtime-test.js`

**Responsibilities**
- Extracting module data and route metadata statically from the Python AST
- Rendering by **walking the Jinja AST**, never by regex over template text
- Per-route page planning, including one page per item for parameterized routes
- The browser runtime: add / edit / delete through `localStorage`

**Must be able to explain in the demo**
- Why generation must not consult the symbol table
- How a parameterized route discovers what to iterate — read from the generator
  expression in the route body, not hardcoded to `products`
- Why an item added in the browser needs the `?id=` shell page
- The demo flows: list, add, view details, edit, delete

---

## Working agreement

**Interfaces are contracts.** Each stage consumes the one before it:

```
Melad ──▶ Raghad ──▶ Aram ──▶ Ahmad ──▶ Yousef
grammar    visitors   AST      checks    output
```

Changing a shared type — `Node`, `Template`, `RouteInfo`, `SemanticError` —
affects people downstream. Say so before you change one.

**Before pushing, run:**
```powershell
.\build.ps1
.\check.ps1
```

All six groups must pass. If you add a semantic check, add both a failing
fixture in `tests/` **and** a valid program in `tests/valid/` that must stay
clean.

**Branches.** Work on a branch named for your area, rebase onto `main` rather
than merging, and keep `src/antlr/**` regenerated rather than hand-edited.

**Demo readiness.** Every member should be able to run the compiler and talk
through their own stage of the output:

```powershell
java -cp "out\classes;dependencies\antlr-4.13.2-complete.jar" app.FlaskCompiler --print-all
```

| Stage | Owner | Where to look |
| --- | --- | --- |
| Tokens | Melad | `compiler_output/tokens.txt` |
| Parse tree | Melad / Raghad | `compiler_output/parse_tree.txt` |
| AST | Aram / Raghad | `compiler_output/ast_python.txt`, `ast_jinja.txt` |
| Symbol table | Ahmad | `compiler_output/symbol_table.txt` |
| Errors | Ahmad | `compiler_output/semantic_report.txt` |
| Generated site | Yousef | `output/index.html` |
