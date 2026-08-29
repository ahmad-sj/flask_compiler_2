# Interview Roles — who presents what

This is a **presentation plan**, not a record of authorship. It says who explains
which part of the compiler during the defence. The authorship record produced
from `git log` is in [CONTRIBUTIONS.md](CONTRIBUTIONS.md).

**Format:** 15 minutes, 5 people — about **2½ minutes each**, leaving ~2½
minutes of slack. You will lose time to questions, so treat 2½ as the ceiling.

Each part is built the same way: **one artifact on screen, one core idea, one
question you can survive.** Nobody has time to explain a whole subsystem.

---

## The split

| # | Part | Artifact to show | Depth of likely follow-up | Owner |
| --- | --- | --- | --- | --- |
| 1 | Grammars & lexing | `compiler_output/tokens.txt` | **Deep** | **ahmad.sj** |
| 2 | Parse tree → AST (visitors) | `parse_tree.txt` + `ast_python.txt` | Medium | |
| 3 | Symbol table & semantic analysis | `symbol_table.txt` + `semantic_report.txt` | **Deep** | **Melad** |
| 4 | Code generation & context data | `generation_log.txt` + `output/index.html` | Medium | |
| 5 | AST printing & live demo | `ast_python.txt` + browser | Shallow | |

Parts 1 and 3 are where a follow-up question stops being answerable from a
script — the lexer mode machine and the scope chain both invite "show me why".
They are assigned to the two people who worked on those areas directly.

Parts 2, 4 and 5 are for the remaining three members to divide. Each is
self-contained: no part depends on another being explained first.

---

## 1 · Grammars & lexing — ahmad.sj

**Core idea.** Two grammar families, because the two languages have genuinely
different lexical rules. Python is indentation-sensitive: `Python3LexerBase`
synthesises INDENT/DEDENT tokens so a context-free grammar can express blocks.
Templates are mode-sensitive: **12 lexer modes**, where `{{` pushes
`EXPRESSION_MODE` and `}}` pops it, `{%` pushes `J_STMNT_MODE`, and `if`/`for`
push expression mode *on top of that* — which is why `%}` is
`J_EXPR_STMNT_END : '%}' -> popMode, popMode` at
[templateLexer.g4:75](grammars/templateLexer.g4#L75).

**Show.** `compiler_output/tokens.txt` — the mode switches, and the INDENT
tokens flagged `<-- synthetic`.

**Be ready for.**

- *"Where are the HTML and CSS grammars?"* → Inside `templateLexer.g4` and
  `templateParser.g4`, as lexer modes plus parser rules producing
  `HtmlElement` and `CssBlock` nodes. Hand to part 5 for the CSS proof.
- *"How does the lexer separate raw HTML text from Jinja tags?"* → The mode
  machine above. This is the strongest answer in the whole defence; lead with it.

**Files.** `grammars/*.g4`, `src/antlr/Python3LexerBase.java`.

---

## 2 · Parse tree → AST (visitors)

**Core idea.** ANTLR generates `BaseVisitor<T>`; we specialise it. Four
visitors — `AppVisitor` and `PythonVisitor` for Python, `TemplateVisitor` and
`NodeVisitor` for templates. They **construct our own node classes**; ANTLR's
parse tree is discarded once the AST exists.

**Show.** `parse_tree.txt` (~335 KB, one node per grammar rule) beside
`ast_python.txt` (~15 KB). The size difference is the argument.

**Be ready for.**

- *"Is your AST your own, or ANTLR's parse tree?"* → Ours, 98 classes. Show the
  same route in both files.
- *"Why visitors and not listeners?"* → A visitor controls traversal order and
  returns a typed value, which is what building a tree needs. A listener is
  pushed by the walker and returns nothing.

**Study.** `AppVisitor.java` is 30 lines — read all of it. Then `visitAssignLine`
in `PythonVisitor.java` and `visitForBlock` in `NodeVisitor.java`.

---

## 3 · Symbol table & semantic analysis — Melad

**Core idea.** `SymbolTable` → `Scope` (a tree, each with a parent and children)
→ `Symbol`. `Scope.resolve` walks the parent chain, so an inner scope sees outer
names but not the reverse. There are **two** tables: `SemanticAnalyzer` builds
the Python one, `NodeVisitor` builds the template one while constructing ASTs.
**Generation consults neither** — it resolves names against extracted context
data instead.

**Show.** `symbol_table.txt` — 13 scopes and 27 symbols on the demo, with
nesting visible. Then `semantic_report.txt`.

**Be ready for.**

- *"Walk me through the symbol table's helper methods."* → `enterScope` /
  `exitScope` / `define` / `resolve`, and the parent-chain walk in
  `Scope.resolve`.
- *"What happens if a template uses a variable the context doesn't provide?"* →
  Demo it live: hard error, generation blocked, exit 1, zero files written.
- *"Why doesn't the generator use the symbol table?"* → The table answers "is
  this name legal here"; generation answers "what is this name's value".

**Files.** `src/symbols/**`, `src/visitors/SemanticAnalyzer.java`,
`src/visitors/TemplateSemanticAnalyzer.java`.

---

## 4 · Code generation & context data

**Core idea.** `PythonDataExtractor` walks the Python AST and produces two
things: the module-level data, and one `RouteInfo` per `@app.route`. That map is
the **only** input the renderer receives — it is the boundary where the two
front-ends meet. `JinjaRenderer` then walks the Jinja AST to emit HTML; it never
runs a regular expression over template text. A route with a URL parameter
generates one page per item.

**Show.** `generation_log.txt` — the five discovered routes, then ten pages
rendered.

**Be ready for.**

- *"Do you execute the Python?"* → No. No interpreter, no bytecode. The AST is
  walked and literals are extracted; anything not statically knowable resolves
  to null.
- *"How does a parameterised route know what to iterate?"* → It reads the
  generator expression in the route body. Nothing is hardcoded to `products`.

**Study.** `CodeGenerator.generate()`, `JinjaRenderer.renderFor()`,
`PythonDataExtractor.toJavaValue()`.

---

## 5 · AST printing & the live demo

**Core idea.** `models.Node` is the abstract base and carries the three required
pieces of identity: node ID, node name, source line. `print(int level)` is
declared there and overridden in **66** subclasses, dispatched polymorphically
through `Node` references. `TreePrinter` walks the roots; each node prints its
own header and recurses into its children.

**Show.** `ast_python.txt` — point at `#8 multi import line (line 2)`, then at a
*nested* child showing its own ID and line. Then open `output/index.html` in a
browser: real products, images, prices, CSS applied.

**Be ready for.**

- *"Show me inheritance and polymorphism."* → Base class `Node.java:16`,
  overridden method `print(int)` at `Node.java:63`, dispatch at
  `TreePrinter.java:56` and recursively at `NodeBody.java:38`. Lead with
  `Expression` (20 subclasses) or `HtmlElement`.
- *"Show me a node's line number and node name."* → Any line of the printed
  tree; every structural node opens with `#id name (line N)`.
- *"Show me the CSS grammar working."* → The `<style>` block in `base.jinja`
  produces 7 `CssBlock` nodes and exercises five of the six selector kinds.

**Warning.** This part looks easiest but invites *"now show me a class that
overrides `print()`"*. Open two or three of them beforehand and be able to name
them.

---

## Timing

Person 1 opens with **30 seconds** of pipeline overview — source → tokens →
parse tree → AST → semantic analysis → generation → HTML — then goes into
part 1. Everyone else goes straight into their part.

| Slot | Minutes |
| --- | --- |
| Overview + part 1 | 0:00 – 3:00 |
| Part 2 | 3:00 – 5:30 |
| Part 3 | 5:30 – 8:00 |
| Part 4 | 8:00 – 10:30 |
| Part 5 + demo | 10:30 – 13:00 |
| Questions | 13:00 – 15:00 |

---

## Before you walk in

Run once, so every artifact is fresh and open in a tab:

```powershell
.\build.ps1
java -cp "out\classes;dependencies\antlr-4.13.2-complete.jar" app.FlaskCompiler --print-all
```

`--print-all` echoes tokens, parse tree, ASTs and both symbol tables. Everything
is written to `compiler_output/` either way.

Have open: `tokens.txt`, `parse_tree.txt`, `ast_python.txt`, `symbol_table.txt`,
`semantic_report.txt`, and `output/index.html` in a browser.

## Known gaps — everyone should know these

If asked, answer plainly rather than guessing:

- Values substituted into a page are **not HTML-escaped**; `|safe` is not
  recognised.
- `url_for('static', filename=…)` is rejected as an unknown route — the
  templates link `href="style.css"` directly, which resolves in the flat output.
- A mismatched closing tag (`<div>x</p>`) is accepted and silently repaired.
- A missing *key* (`{{ p.nmae }}`) is a warning, not an error, unlike a missing
  *variable*, which blocks the build.
- `not a == b` parses as `(not a) == b`; Python means `not (a == b)`.
- `x += 2` is not supported. Unary minus, `**` and `//` are.
- `output/` is not cleaned between runs, so a removed route leaves a stale page.
