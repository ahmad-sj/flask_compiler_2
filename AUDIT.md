# AUDIT — Flask/Jinja2/HTML/CSS → Static HTML Compiler

Independent audit performed against the source, not the README or REPORT.
Every DONE verdict is backed by a `file:line` reference or by real output captured
from a build performed during this audit (`.\build.ps1` + a full run over `project/`).

**Audit method.** Read all 5 `.g4` files and all 10,743 lines of hand-written Java
(`src/` minus `src/antlr/`); built the compiler; ran it over `project/`; ran
`check.ps1` (all 6 groups pass); and ran 40 purpose-built probe inputs against the
Python and template front-ends to establish exactly where they break.

---

# PART A — OFFICIAL REQUIREMENTS COMPLIANCE

## A1. Language definitions and grammars

Every `.g4` file in the repo:

| File | Lines | Type | Covers |
| --- | ---: | --- | --- |
| [pythonLexer.g4](grammars/pythonLexer.g4) | 97 | lexer grammar | **Python** tokens + INDENT/DEDENT |
| [pythonParser.g4](grammars/pythonParser.g4) | 260 | parser grammar | **Python** statements/expressions |
| [templateLexer.g4](grammars/templateLexer.g4) | 154 | lexer grammar | **Jinja2 + HTML + CSS** (12 lexical modes) |
| [templateParser.g4](grammars/templateParser.g4) | 336 | parser grammar | **Jinja2 + HTML + CSS** (68 rules) |
| [templateFragments.g4](grammars/templateFragments.g4) | 118 | lexer fragments | shared character classes, imported by templateLexer |

| # | Requirement | Verdict | Evidence / gap |
| --- | --- | --- | --- |
| A1.1 | **Python** lexer + parser | **DONE** | [pythonLexer.g4:1](grammars/pythonLexer.g4#L1), [pythonParser.g4:2](grammars/pythonParser.g4#L2). Genuinely separate grammar pair with its own token vocabulary. |
| A1.2 | **Jinja2** lexer + parser | **DONE** | [templateLexer.g4:9-11](grammars/templateLexer.g4#L9-L11) (`{{`/`{%`/`{#`), [templateParser.g4:11-241](grammars/templateParser.g4#L11-L241). |
| A1.3 | **HTML** lexer + parser | **DONE** | Dedicated lexer modes [templateLexer.g4:81-118](grammars/templateLexer.g4#L81-L118) (`START_TAG_MODE`, `END_TAG_MODE`, `INSIDE_START_TAG_MODE`, `ATTR_VAL`, `ATTR_VAL_QOUTED`) and parser rules [templateParser.g4:245-296](grammars/templateParser.g4#L245-L296), producing real nodes (`HtmlRegularElement`, `HtmlSelfClosingElement`, 4 `Attribute` subclasses). **HTML is not passed through as raw text.** |
| A1.4 | **CSS** lexer + parser | **DONE (grammar) / NOT DEMONSTRATED (demo input)** | Modes [templateLexer.g4:123-154](grammars/templateLexer.g4#L123-L154) (`CSS_BLK`, `CSS_INLINE`, `CSS_BLK_PROP`, `CSS_PROP_VALUES`) and rules [templateParser.g4:313-336](grammars/templateParser.g4#L313-L336), producing `CssBlock` + 6 `Selector` subclasses. **But see Finding C-2**: no template in `project/` contains a `<style>` block, so on the shipped demo the CSS-block path emits **zero tokens**; and `project/style.css` is copied byte-for-byte, never parsed. |

**Verdict on the audit brief's suspicion:** the brief asked whether "HTML is merely
passed through as raw text and CSS is only copied as a file." **That is not the case
for the grammar.** HTML and CSS both have real modes, real parser rules, and real AST
node classes. This is *not* a Critical requirement gap. The genuine A1 problem is
narrower and is captured as Finding C-2: the CSS block grammar is never exercised by
the demo project you will show the examiners.

**A1 fix (≈15 min):** add a `<style>` block to `project/templates/base.jinja` so the CSS
grammar is visibly exercised — but do **Finding C-1 first**, because `<style>` currently
renders as broken HTML.

---

## A2. AST built and printed at execution

| # | Requirement | Verdict | Evidence |
| --- | --- | --- | --- |
| A2.1 | An AST is constructed (not ANTLR's parse tree) | **DONE** | 98 hand-written node classes under [src/models/](src/models/), all constructed explicitly by the visitors, e.g. [NodeVisitor.java:76](src/visitors/NodeVisitor.java#L76) `new HtmlRegularElement(...)`. No `ParseTree`/`XContext` object survives into the AST. |
| A2.2 | OOP: proper class hierarchy | **DONE** | 8 abstract intermediate bases: `HtmlElement`, `Attribute`, `Selector`, `Atom`, `Expression`, `JinjaBlock`, `Trailer`, `Statement`. |
| A2.3 | Inheritance: common abstract base node | **DONE** | **`models.Node`** — [Node.java:16](src/models/Node.java#L16) `public abstract class Node`. 98 of 102 files under `src/models/` extend it. |
| A2.4 | Polymorphism: overridden methods via the base type | **DONE** | Overridden method **`public String print(int level)`**, declared [Node.java:63](src/models/Node.java#L63), overridden in **66** subclasses. Polymorphic dispatch sites: [TreePrinter.java:53](src/app/TreePrinter.java#L53) and [:80](src/app/TreePrinter.java#L80) (`node.print(0)` on a `Node` reference), and the recursive one at [NodeBody.java:38](src/models/NodeBody.java#L38) / [:42](src/models/NodeBody.java#L42) (`nodeList.get(i).print(level + 1)`). `toString()` is a second polymorphic axis. |
| A2.5 | **Every node stores a Line Number** | **DONE** | [Node.java:23](src/models/Node.java#L23) `protected int lineNumber`; 151 `setLineNumber(...)` call sites in the visitors. **Verified empirically:** `ast_python.json` has 558 nodes and `ast_jinja.json` 599 nodes — **1,157 nodes, zero with `"line":0`**. |
| A2.6 | **Every node stores a Node Name** | **DONE** | [Node.java:22](src/models/Node.java#L22) + [:29](src/models/Node.java#L29) — defaults to the class simple name in the constructor so a node can never lack one; `setNodeName` overrides it at 151 sites. |
| A2.7 | Node information stored correctly and completely | **PARTIAL** | Model is complete. **But 36 of 102 classes never override `print(int)`** (list under Finding I-1) — including every CSS selector, every HTML attribute, `FloatType`, `TrueValue`/`FalseValue`/`NoneValue`, `python.blocks.ForNode`/`WhileNode`. Their parents fall back to `toString()`, so those subtrees print flat, with **no ID, no node name, no line number**. |
| A2.8 | Tree persisted correctly at end of execution | **DONE** | 4 tree artifacts written every run: `ast_python.json`, `ast_jinja.json`, `ast_python.txt`, `ast_jinja.txt` — [FlaskCompiler.java:158-187](src/app/FlaskCompiler.java#L158-L187). JSON carries `id`, `node`, `type`, `line` on every node. |

**Nodes lacking a line number:** none. **Nodes lacking a node name:** none.
**Concrete node classes lacking a `print()` override (A2.7 gap):** `Property`,
`PropertyValue`, `ClassSelector`, `DescendantSelector`, `ElementSelector`,
`GroupSelector`, `IdSelector`, `PseudoClassSelector`, `AttributeValue`,
`BooleanAttribute`, `QuotedAttribute`, `StyleAttribute`, `UnquotedAttribute`,
`FloatType`, `python/blocks/ElifBlock`, `ElseBlock`, `ForNode`, `WhileNode`,
`python/expressions/MulExpression`, `FalseValue`, `NoneValue`, `TrueValue`,
`SingleReturnNode`, `TupleReturnNode`, `TestNode`. (The remaining 11 are abstract
bases or the non-Node containers `App`/`Template`/`RouteInfo`, which is fine.)

---

## A3. Visitor that populates the AST

| # | Requirement | Verdict | Evidence |
| --- | --- | --- | --- |
| A3.1 | A Visitor builds/populates the AST | **DONE** | Four, all extending ANTLR's generated `BaseVisitor<T>`, all constructing nodes directly — there is **no separate ad-hoc builder**. |

| Class | Extends | `T` | Builds |
| --- | --- | --- | --- |
| [AppVisitor.java:17](src/visitors/AppVisitor.java#L17) | `pythonParserBaseVisitor<App>` | `models.App` | Python root; delegates each statement to `PythonVisitor` |
| [PythonVisitor.java:25](src/visitors/PythonVisitor.java#L25) | `pythonParserBaseVisitor<Node>` | `models.Node` | all `models.python.*` nodes (770 lines) |
| [TemplateVisitor.java:10](src/visitors/TemplateVisitor.java#L10) | `templateParserBaseVisitor<Template>` | `models.Template` | template root; delegates to `NodeVisitor` |
| [NodeVisitor.java:31](src/visitors/NodeVisitor.java#L31) | `templateParserBaseVisitor<Node>` | `models.Node` | all Jinja + HTML + CSS nodes (1,272 lines, 72 overrides) |

No listeners and no `ParseTreeWalker` anywhere in hand-written code — verified by grep.
Consistent visitor-only design across both front-ends, which is defensible.

---

## A4. Symbol Table

| # | Requirement | Verdict | Evidence |
| --- | --- | --- | --- |
| A4.1 | A suitable symbol table data structure exists | **DONE** | [SymbolTable.java](src/symbols/SymbolTable.java) → [Scope.java](src/symbols/Scope.java) (tree with `parent` + `children`) → [Symbol.java](src/symbols/Symbol.java) (`name`, `kind`, `type`, `value`, `scope`, `attributes`). |
| A4.2 | Helper methods controlling operations | **PARTIAL** | 11 helpers exist, **6 are dead code**, and the main consumer bypasses the API. |

| Helper | Signature | Used? |
| --- | --- | --- |
| enter scope | `void enterScope(String name)` — [:15](src/symbols/SymbolTable.java#L15) | **yes** — NodeVisitor ×5, SemanticAnalyzer ×6 |
| exit scope | `void exitScope()` — [:20](src/symbols/SymbolTable.java#L20) | **yes** — 11 sites |
| insert/define | `void define(String,String,String,Node)` — [:27](src/symbols/SymbolTable.java#L27) | **once only** — [NodeVisitor.java:492](src/visitors/NodeVisitor.java#L492) |
| resolve (throwing) | `Symbol resolve(String)` — [:32](src/symbols/SymbolTable.java#L32) | **DEAD — 0 call sites** |
| lookup (null-safe) | `Symbol lookup(String)` — [:46](src/symbols/SymbolTable.java#L46) | **DEAD — 0 call sites** |
| contains | `boolean isDefined(String)` — [:51](src/symbols/SymbolTable.java#L51) | **DEAD — 0 call sites** |
| update | `boolean update(String,String,String,Node)` — [:61](src/symbols/SymbolTable.java#L61) | **DEAD — 0 external call sites** |
| count scopes | `int scopeCount()` — [:66](src/symbols/SymbolTable.java#L66) | yes — [TreePrinter.java:92](src/app/TreePrinter.java#L92) |
| count symbols | `int symbolCount()` — [:71](src/symbols/SymbolTable.java#L71) | yes — [TreePrinter.java:93](src/app/TreePrinter.java#L93) |
| print | `void print()` — [:75](src/symbols/SymbolTable.java#L75) | **DEAD — 0 call sites** |
| render | `String render()` — [:80](src/symbols/SymbolTable.java#L80) | yes — [TreePrinter.java:90](src/app/TreePrinter.java#L90) |

`Scope.print()` is also dead. `SemanticAnalyzer` reaches through the field —
`symbolTable.currentScope.resolve(...)` / `.define(...)` at
[SemanticAnalyzer.java:642, 742, 901, 904, 1100, 1170, 1171, 1228](src/visitors/SemanticAnalyzer.java#L642)
— instead of calling the table's own `resolve`/`lookup`/`define`. This is why those
helpers are dead: the API exists but is routed around. **The examiner will ask you to
walk through the helper methods; be ready for "why does nothing call `lookup`?"**

**See also Finding C-3** — the symbol table that is *printed* is not the one semantic
analysis builds.

---

## A5. AST printing

| # | Requirement | Verdict | Evidence |
| --- | --- | --- | --- |
| A5.1 | Per-node print method | **DONE** | `Node.print(int level)` [Node.java:63](src/models/Node.java#L63), 66 overrides. |
| A5.2 | Whole-tree print method calling per-node methods recursively | **DONE** | [TreePrinter.renderPythonAst:38-57](src/app/TreePrinter.java#L38-L57) and `renderTemplateAsts:60-84`; recursion via [NodeBody.print:27-47](src/models/NodeBody.java#L27-L47). |
| A5.3 | Output human-readable, showing depth | **PARTIAL** | Depth *is* shown, but the indent unit is **three backticks per level** ([Node.java:72](src/models/Node.java#L72) `indent.append("```")`), which produces `` ``````````````` `` runs that look like broken Markdown fences. |
| A5.4 | Each node prints its own information **and its children** | **PARTIAL** | Children are printed. But **no `print()` method anywhere calls `header()`** (0 hits across `src/models/`), so node ID and node name appear **only** on the top-level frames the driver writes. Nested nodes show `line no:` at best (49 of 102 classes), and the 36 classes from A2.7 show nothing. |

### Real output — first 40 lines of `compiler_output/ast_python.txt`

```
================================================================
 PYTHON AST - app.py
================================================================

[1/10] #8 multi import line (line 2)
----------------------------------------------------------------
multi import
├─ line no: 2
├─ from name: name
``````├─ line no: 2
``````└─ id: flask
└─ imported names: Flask, render_template, request, redirect, url_for

[2/10] #17 assign line (line 4)
----------------------------------------------------------------
assign line
├─ line no: 4
├─ target: app
└─ expr: value:
``````├─ line no: 4
``````├─ base value: Flask
``````└─ trailers: (__name__)

[3/10] #87 assign line (line 7)
----------------------------------------------------------------
assign line
├─ line no: 7
├─ target: products
└─ expr: value:
``````├─ line no: 7
``````└─ base value: list type:
`````````├─ line no: 7
`````````└─ expr list:
````````````├─ value:
```````````````├─ line no: 8
```````````````└─ base value: "id" : 1, "name" : "Wireless Headphones", "price" : 79.99
, "details" : "Noise-cancelling over-ear headphones with 30-hour battery life.", "image" : "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400"
````````````├─ value:
```````````````├─ line no: 12
```````````````└─ base value: "id" : 2, "name" : "Mechanical Keyboard", ...
```

**Judge for yourself:** the structure is legible, `#8`/`#17`/`#87` node IDs and
`(line N)` are present on the top-level frames, and the box-drawing characters work.
Two things will draw fire: the backtick indent, and the fact that the dict at line 8
is flattened onto one line with **no node ID, no node name, no line number for the
dict or any of its 5 entries**, because `Dict`'s children are emitted via `toString()`.
The JSON dump does not have this problem — every one of the 1,157 nodes there carries
`id`, `node`, `type`, `line`.

---

## A6. Required test programs

| # | Program | Verdict | Evidence |
| --- | --- | --- | --- |
| A6.1 | **Display products** | **DONE** | [project/templates/index.jinja](project/templates/index.jinja) + route `index` → `output/index.html`, 3 product cards rendered from real data. |
| A6.2 | **Add product** | **DONE** | [project/templates/add_product.jinja](project/templates/add_product.jinja) + route `add` → `output/add_product.html`. |
| A6.3 | **Product details** | **DONE** | [project/templates/product_detail.jinja](project/templates/product_detail.jinja) + route `detail` → `product_detail_1/2/3.html` + a `?id=` shell page. |
| A6.4 | *Bonus:* Delete product | **DONE** | Route `delete` at [app.py:69-74](project/app.py#L69-L74); no template (POST-only), so the flow is completed client-side by `script.js`. Verified working by `check.ps1` group [F]/[G]. |

**The brief's suspicion is wrong here.** `product_detail.jinja` **is** present. `edit_product.jinja`
is a *fourth, extra* program, not a substitute. **No required program is unimplemented.**

### All four product fields — traced end to end

| Field | In Python data | Referenced in template | Visible in generated HTML |
| --- | --- | --- | --- |
| **image** | [app.py:10](project/app.py#L10) `"image": "https://…"` | `{{ product.image }}` — index.jinja:12, product_detail.jinja:24 | `output/product_detail_1.html:32` `src="https://images.unsplash.com/photo-1505740420928…"` |
| **name** | [app.py:8](project/app.py#L8) `"name": "Wireless Headphones"` | index.jinja:14, product_detail.jinja:7 | `output/product_detail_1.html:28` `Wireless Headphones` |
| **price** | [app.py:8](project/app.py#L8) `"price": 79.99` | `{{ "%.2f"\|format(product.price) }}` — index.jinja:15 | `output/product_detail_1.html:40` `79.99` |
| **details** | [app.py:9](project/app.py#L9) `"details": "Noise-cancelling…"` | product_detail.jinja:38 | `output/product_detail_1.html:50` `Noise-cancelling over-ear headphones with 30-hour battery life.` |

All four present in all three layers. **A6 is fully satisfied.**

---

## A7. Report and submission package

| # | Requirement | Verdict | Evidence / gap |
| --- | --- | --- | --- |
| A7.1 | Report explains every section | **DONE** | [REPORT.md](REPORT.md) (355 lines) covers grammar/lexer/parser, AST, visitors, symbol table, tree printer, demo, running, four-stage comparison, verification. Arabic version [REPORT.ar.md](REPORT.ar.md) (340 lines). |
| A7.2 | AST structure diagram matching real classes | **DONE (minor drift)** | Diagram at [REPORT.md:90-124](REPORT.md#L90-L124). Checked class by class against `src/models/` — the hierarchy is accurate. |
| A7.3 | Group member names file | **PARTIAL** | [ROLES.md](ROLES.md) lists 5 first names (Melad, Aram, Raghad, Ahmad, Yousef). **No surnames, no student IDs**, and see Finding I-7 — the names do not reconcile with the git history. |
| A7.4 | Packaged for submission (zip) | **MISSING** | No zip, no packaging script. `bin/` (50 stale `.class` files) and `.idea/` would be included as-is. |

### Report claims that are inaccurate — fix before submission

| # | Location | Claim | Reality |
| --- | --- | --- | --- |
| R-1 | [REPORT.md:34](REPORT.md#L34) **and** [REPORT.ar.md:32](REPORT.ar.md#L32) | "Indentation is handled by `PythonIndentationLexerBase`" | The class is **`Python3LexerBase`** — [pythonLexer.g4:13](grammars/pythonLexer.g4#L13) `superClass = Python3LexerBase`, [src/antlr/Python3LexerBase.java:9](src/antlr/Python3LexerBase.java#L9). `PythonIndentationLexerBase` exists **only** as a stale compiled artifact at `bin/antlr/PythonIndentationLexerBase.class`. An examiner who greps for the name in `src/` finds nothing. **Wrong in both languages.** |
| R-2 | [REPORT.md:66](REPORT.md#L66) | "102 classes under `src/models/`, all descending from a single abstract base" | 102 *files*; **98** extend `Node`. `App`, `Template`, `RouteInfo` do not (they are containers), and `Node` is the base itself. Say "98 node classes plus 3 container classes". |
| R-3 | [REPORT.md:211](REPORT.md#L211) | "The symbol table is used **only** by semantic analysis" | **False.** The `SymbolTable` created at [FlaskCompiler.java:72](src/app/FlaskCompiler.java#L72) is handed to `TemplatesHandler` → `TemplateVisitor` → `NodeVisitor`, which is the **AST-construction** visitor ([NodeVisitor.java:387, 413, 425-426, 478, 492, 535, 565, 605](src/visitors/NodeVisitor.java#L387)). The report's own quoted output (`title`, `content`, `product`) is proof — those rows were written by `NodeVisitor`, not by `SemanticAnalyzer`. The claim is true only of **generation**, which is the narrower statement you can safely defend. |
| R-4 | [REPORT.md:88](REPORT.md#L88) | `header()` shown as always appending `" (line N)"` | Real code omits it when `lineNumber <= 0` — [Node.java:59-60](src/models/Node.java#L59-L60). Cosmetic. |
| R-5 | [README.md:186-187](README.md#L186-L187) "Known limitations" | Lists 3 limitations | Materially understates the front-ends. Missing from the list: no unary minus, no `**`, no `//`, no augmented assignment, no f-strings, no comprehensions, no classes, no `try`/`with`, no `{% include %}`/`{% macro %}`, no whitespace control, no `{`/`}`/`>` in body text, no `<script>` bodies, closed HTML tag list, void elements must self-close, no HTML escaping. See Part B. |

**Verified correct — the brief's suspicion does not apply.** The report does **not**
claim "Python → bytecode → execution". [README.md:18-20](README.md#L18-L20) correctly
states "Data extraction walks the Python AST directly (no Python interpreter is
required), and rendering walks the Jinja AST." That matches the code exactly.

---

## A8. Interview demo readiness

| # | Requirement | Verdict | Evidence / gap |
| --- | --- | --- | --- |
| A8.1 | Single documented command printing AST **and** symbol table | **DONE** | `java -cp "out\classes;dependencies\antlr-4.13.2-complete.jar" app.FlaskCompiler` — defaults to `project`/`output`/`compiler_output`, prints both trees and the symbol table. `--print-all` adds tokens + parse tree. Documented at [README.md:38](README.md#L38) and [REPORT.md:277](REPORT.md#L277). |
| A8.2 | UIs render correctly in a browser with CSS applied | **DONE** | 10 pages generated; `style.css` copied byte-identical and linked as `href="style.css"`, which resolves correctly in the flat output layout. `check.ps1` verifies DOCTYPE on every page, no leftover Jinja delimiters, and byte-identical assets; `tests/runtime-test.js` drives the real pages in jsdom (24/24 pass). |
| A8.3 | *Implicit:* a grammar change can be demonstrated live | **MISSING** | **Finding C-4** — nothing in the repo regenerates the parsers from the `.g4` files. |

**What blocks A8.3 and the exact command that should exist:** neither
[build.ps1:18](build.ps1#L18) nor [build.sh:13](build.sh#L13) invokes ANTLR — they run
`javac` over `src/` only, and `src/antlr/*.java` is committed. The command is documented
in prose at [ROLES.md](ROLES.md) but is not wired into any script:

```powershell
java -jar dependencies\antlr-4.13.2-complete.jar -Dlanguage=Java -visitor `
     -o src\antlr -package antlr grammars\pythonLexer.g4 grammars\pythonParser.g4 `
     grammars\templateLexer.g4 grammars\templateParser.g4
```

Add that as step 1 of `build.ps1`/`build.sh`. Until then, **editing a `.g4` file has no
effect on the build**, which is fatal if an examiner asks for a live grammar change.

---

# PART A — SCORECARD

| Item | Verdict |
| --- | --- |
| A1.1 Python grammar | **DONE** |
| A1.2 Jinja grammar | **DONE** |
| A1.3 HTML grammar | **DONE** |
| A1.4 CSS grammar | **DONE** (not demonstrated by the demo — C-2) |
| A2.1 AST constructed | **DONE** |
| A2.2 OOP hierarchy | **DONE** |
| A2.3 Inheritance / base class | **DONE** |
| A2.4 Polymorphism | **DONE** |
| A2.5 Line number on every node | **DONE** |
| A2.6 Node name on every node | **DONE** |
| A2.7 Info complete | **PARTIAL** (36 classes have no `print()`) |
| A2.8 Tree persisted | **DONE** |
| A3.1 AST-populating visitor | **DONE** |
| A4.1 Symbol table structure | **DONE** |
| A4.2 Helper methods | **PARTIAL** (6 of 11 dead; API bypassed) |
| A5.1 Per-node print | **DONE** |
| A5.2 Whole-tree print | **DONE** |
| A5.3 Human-readable | **PARTIAL** (backtick indent) |
| A5.4 Prints own info + children | **PARTIAL** (`header()` never called by `print()`) |
| A6.1 Display products | **DONE** |
| A6.2 Add product | **DONE** |
| A6.3 Product details | **DONE** |
| A6.4 Delete (bonus) | **DONE** |
| A6.5 All four fields end to end | **DONE** |
| A7.1 Report covers every section | **DONE** |
| A7.2 AST diagram matches code | **DONE** |
| A7.3 Members file | **PARTIAL** |
| A7.4 Submission zip | **MISSING** |
| A8.1 Single documented command | **DONE** |
| A8.2 UIs render with CSS | **DONE** |
| A8.3 Grammar regeneration | **MISSING** |

**19 DONE · 6 PARTIAL · 2 MISSING.** Nothing graded is absent outright except the
submission package and the grammar-regeneration step.

---

# VERDICT ON DEFENSE READINESS

This project is in strong shape and will defend well. The architecture the report
describes is the architecture the code implements: two genuinely separate ANTLR
front-ends, four visitors that build 98 hand-written node classes descending from one
abstract `Node`, a distinct semantic-analysis pass that provably blocks generation, and
a generator that walks the Jinja AST rather than running regexes over template text —
all of it exercised by a 60-commit history and a real test suite (27 invalid-backend
fixtures, 3 false-positive guards, 2 broken-template projects, 5 control-flow checks,
24 jsdom runtime checks) that passes end to end. Every requirement in Part A is met or
substantially met, the three mandated programs all exist and carry all four product
fields into visible HTML, and the cross-front-end check that catches a template
referencing a name its route never supplies is genuinely impressive work. The exposure
is in four places, none of them architectural: **(1)** nothing in the repo regenerates
the parsers from the `.g4` files, so a live grammar edit will appear to do nothing —
fix this first, it is the single highest-risk item in a viva; **(2)** the symbol table
you print contains eleven Jinja block names and no Python symbols at all, which
directly undercuts "walk me through your symbol table" and contradicts a claim the
report makes in writing; **(3)** the Python grammar rejects `x = -5`, `2 ** 3` and
`7 // 2`, so any examiner who types an expression at the demo will hit a parse error
within seconds; and **(4)** `<style>` blocks render as malformed HTML, which is why the
demo quietly avoids them. All four are hours of work, not days. Fix them and this is a
confident, well-evidenced defense.

---

# PART B — TECHNICAL FINDINGS

## Critical

### C-1 · `<style>` blocks render as malformed HTML — the tag is emitted twice
**`src/visitors/JinjaRenderer.java:369-393`**, **`src/models/html/elements/HtmlStyleElement.java:28-30`**

`renderHtmlElement` writes `<` + tagName + attributes + `>` at
[:371](src/visitors/JinjaRenderer.java#L371) and [:379](src/visitors/JinjaRenderer.java#L379).
An `HtmlStyleElement` is neither `HtmlSelfClosingElement` nor `HtmlRegularElement`, so
control falls to [:392](src/visitors/JinjaRenderer.java#L392) `out.append(element.toString())`
— and `HtmlStyleElement.toString()` re-emits the **complete** element including its own
`<style>` and `</style>`.

Reproduced during this audit with a template containing a four-rule `<style>` block:

```html
<style><style>
#main {
color: red;
...
</style>
```

The first `</style>` closes the first `<style>`; the browser then treats the second
`<style>` and everything after it as **text content**, so the entire stylesheet is
silently discarded and the raw CSS is not applied. This is why no template in
`project/` contains a `<style>` block — the demo works around the bug.

**Fix:** give `HtmlStyleElement` the same treatment as `HtmlRegularElement`:

```java
if (element instanceof HtmlStyleElement) {
    HtmlStyleElement style = (HtmlStyleElement) element;
    if (style.elementBody != null) {
        out.append('\n');
        renderNode(style.elementBody, context, out, overrides);
        out.append('\n');
    }
    out.append("</").append(element.tagName).append('>');
    return;
}
```

(`HtmlStyleElement.elementBody` is package-private — widen it or add a getter.)

---

### C-2 · The CSS block grammar is never exercised by the demo project
**`project/templates/*.jinja`**, **`src/app/CodeGenerator.java:328-340`**

Measured on the audited run of `project/`, from `compiler_output/tokens.txt`:

| Token | Count |
| --- | ---: |
| `STYLE_TAG_START_NAME` | **0** |
| `CSS_SEL_ID` / `CSS_SEL_CLASS` / `CSS_SEL_ELEM` | **0 / 0 / 0** |
| `CSS_LBRACE` / `BLK_PROP_NAME` | **0 / 0** |
| `CSS_INLINE_PROP_NAME` (inline `style="…"`) | 15 |

No template contains a `<style>` element, and `project/style.css` is copied
byte-for-byte at [CodeGenerator.java:337](src/app/CodeGenerator.java#L337) — it is
**never parsed**. So `CSS_BLK`, `CSS_BLK_PROP`, `cssBlock`, `selectorList` and all six
`Selector` node classes are dead on the input you will demo. When asked "show me your
CSS grammar working", you currently have nothing to point at.

I verified the grammar itself **does** work when given input (after C-1 is fixed the
output is correct): `#main`, `.card p`, `a:hover` and `h1, h2` all produce proper
`css block` / `selectors:` nodes in the AST dump.

**Fix (after C-1):** move a representative slice of `style.css` into a `<style>` block
in `base.jinja`. Roughly 20 minutes, and it turns a dead grammar section into a
demonstrable one.

---

### C-3 · The printed symbol table contains no Python symbols
**`src/app/FlaskCompiler.java:72, 79, 98`**, **`src/visitors/SemanticAnalyzer.java:112`**

There are **two** `SymbolTable` instances and the wrong one is printed:

1. Created at [FlaskCompiler.java:72](src/app/FlaskCompiler.java#L72), passed to
   `TemplatesHandler` at [:79](src/app/FlaskCompiler.java#L79) → `TemplateVisitor` →
   `NodeVisitor`. Populated during **template AST construction** with `{% block %}`
   names and `{% for %}` loop variables. **This is the one written to
   `symbol_table.txt`.**
2. Created privately at [SemanticAnalyzer.java:112](src/visitors/SemanticAnalyzer.java#L112)
   by `new SemanticAnalyzer()` at [FlaskCompiler.java:98](src/app/FlaskCompiler.java#L98).
   Holds every Python symbol — module variables, route functions, parameters, scopes.
   **Never returned, never printed, discarded when `analyze()` returns.**

The actual `compiler_output/symbol_table.txt` from the audited run, in full:

```
symbol        kind         type        value      scope
title         block name   StringType  title      title block
content       block name   StringType  content    content block
title         block name   StringType  title      title block
content       block name   StringType  content    content block
title         block name   StringType  title      title block
content       block name   StringType  content    content block
title         block name   StringType  title      title block
content       block name   StringType  content    content block
product       id           IdType      product    for block at 10:4
title         block name   StringType  title      title block
content       block name   StringType  content    content block

Scopes: 17   Symbols: 11
```

No `app`, no `products`, no `index`/`detail`/`add`/`edit`/`delete`, no function
parameters. Eleven rows, nine of which are the same two block names repeated once per
template. Two secondary defects are visible here too: the render is **flat** — scope
nesting appears only as a name in the last column, with no indentation and no per-scope
grouping ([Scope.render:86-93](src/symbols/Scope.java#L86-L93) recurses but emits no
structure) — and rows come from a `HashMap` ([Scope.java:16](src/symbols/Scope.java#L16)),
so **ordering is not deterministic between runs**.

This is also the evidence that contradicts report claim **R-3**.

**Fix:** expose the analyzer's table (`public SymbolTable getSymbolTable()` on
`SemanticAnalyzer`) and print *that* one — or print both under separate banners, which
is the more honest presentation since both are real. Switch `Scope.symbols` to a
`LinkedHashMap` for stable ordering, and indent nested scopes in `Scope.render`.

---

### C-4 · Nothing regenerates the parsers from the `.g4` files
**`build.ps1:18`**, **`build.sh:13`**

Both build scripts run `javac` over `src/` and nothing else. The ANTLR-generated
sources are **committed** under `src/antlr/` (14 `.java`, 8 `.tokens`, 8 `.interp`).
Consequence: **editing any `.g4` file changes nothing** until someone manually runs the
ANTLR jar. There is no Maven or Gradle build in this repo at all, so the audit brief's
`mvn clean package` / `<visitor>true</visitor>` question does not apply — the
equivalent evidence is that `-visitor` *was* passed at some point, since
`templateParserVisitor.java` and `pythonParserVisitor.java` exist alongside the
listeners.

This is the single most dangerous item for a live demo. "Add a `|upper` filter" is a
grammar-free change (see B11), but "add a Python operator" or "add a new AST node type
end to end" both require regeneration, and the team would be improvising the command
under pressure.

**Fix:** prepend to both build scripts (command given under A8.3 above), then verify a
clean clone builds with `.\build.ps1; .\check.ps1`.

---

### C-5 · No HTML escaping — template values are injected raw
**`src/visitors/JinjaRenderer.java:160, 430`**

`out.append(ExpressionEvaluator.str(evaluator.eval(...)))` writes the evaluated value
directly into the page, in both body text and attribute values. Real Jinja autoescapes
`.html`/`.jinja` templates by default.

Tested with a product named `<script>alert(1)</script>`. The build reported
**`Success: 1 page(s)`** and emitted:

```html
<h3>
<script>alert(1)</script>
</h3>
```

The script tag is live in the output. An `escape`/`e` filter *does* exist
([ExpressionEvaluator.java:368-369](src/visitors/ExpressionEvaluator.java#L368-L369),
implementation at [:650](src/visitors/ExpressionEvaluator.java#L650)) but is opt-in —
exactly inverted from Jinja. And `|safe` is **not** in `SUPPORTED_FILTERS`
([:57-59](src/visitors/ExpressionEvaluator.java#L57-L59)), so `{{ x|safe }}` is
reported as `Unknown filter 'safe'`.

**Fix:** escape by default in both append sites, add `safe` to `SUPPORTED_FILTERS`, and
have it mark the value so the renderer skips escaping. Small change, and it is a
question an examiner is likely to ask about a template engine.

---

## Important

### I-1 · `print()` never calls `header()`, so nested nodes lose their identity
**`src/models/*` (66 `print()` overrides, 0 references to `header()`)**

`header()` — [Node.java:58-61](src/models/Node.java#L58-L61) — is the method that
renders `#id nodeName (line N)`. It is called **only** by the driver, at
[TreePrinter.java:51](src/app/TreePrinter.java#L51) and [:78](src/app/TreePrinter.java#L78).
No `print()` override calls it. Combined with the 36 classes that have no `print()` at
all (A2.7), a large part of the printed tree shows neither node ID nor node name.

The requirement is *"each node prints its own information and its children"*, and the
information the requirement names is node name + line number. Right now the JSON dump
satisfies this fully and the text tree does not.

**Fix:** change each `print()` to open with `header()` instead of a bare literal. 66
mechanical edits; the highest-value subset is the ~15 concrete classes from A2.7 that
have no `print()` at all. `ROLES.md` already lists this as known open work.

---

### I-2 · Python operator precedence: `not` binds tighter than comparison
**`grammars/pythonParser.g4:106-107, 156-157`**

The precedence chain is `ternaryExpr → orExpr → andExpr → equalExpr → compareExpr →
addExpr → mulExpr → singleExpr → negatedExpr`. `negatedExpr : NOT singleExpr` sits at
the **tightest** binding level, below `mulExpr`. In real Python, `not` is *looser* than
comparison and arithmetic.

Verified by compiling `x = not a == b` and reading `compiler_output/ast_python.txt`:

```
└─ expr: equal expr
``````├─ expr0: negated expr        ←  not a
``````│      └─ expr: value: a
``````├─ expr1: value: b
``````├─ operator: ==
```

The AST is `(not a) == b`. Python evaluates `not (a == b)`. Same error class for
`not a * b`.

**Fix:** move `not` between `andExpr` and `equalExpr`:

```antlr
andExpr    : notExpr (AND notExpr)* ;
notExpr    : NOT notExpr | equalExpr ;
equalExpr  : compareExpr ((EQUALEQUAL | NOTEQUAL) compareExpr)* ;
mulExpr    : value (mulOperator value)* ;     // drop singleExpr/negatedExpr
```

Requires regenerating the parser (C-4) and touching `PythonVisitor.visitNegatedExpr`.

---

### I-3 · Python grammar rejects ordinary arithmetic
**`grammars/pythonLexer.g4:55-72`**, **`grammars/pythonParser.g4:147-161`**

Missing operators, each verified as a real parse error during this audit:

| Input | Result |
| --- | --- |
| `x = -5` | `extraneous input '-'` — **no unary minus anywhere in the grammar** |
| `x = 2 ** 3` | `extraneous input '*'` — no `POWER` token, no `powExpr` rule |
| `x = 7 // 2` | `extraneous input '/'` — no `FLOORDIV` token |
| `x += 2` | `extraneous input '='` — no augmented assignment |

Note the **asymmetry with the Jinja grammar**, which has all of these
([templateLexer.g4:42-44](grammars/templateLexer.g4#L42-L44) `FLOORDIV`, `POW`;
[templateParser.g4:210-217](grammars/templateParser.g4#L210-L217) `unaryExpr`, `powExpr`).
The template side is the better-engineered of the two expression grammars.

`x = -5` is the one that matters — it is the first thing a curious examiner types.

**Fix:** add `DOUBLESTAR : '**'` and `DOUBLESLASH : '//'` to the lexer **before**
`STAR`/`SLASH`, then mirror the template grammar's `unaryExpr`/`powExpr` structure.
Roughly 20 lines of grammar plus two visitor methods.

---

### I-4 · Python subset: 11 common constructs cannot be parsed
**`grammars/pythonParser.g4`**

All verified as parse errors:

| Construct | Error | Cause |
| --- | --- | --- |
| f-strings `f"hi {n}"` | `extraneous input '"hi {n}"'` | `STRING` [pythonLexer.g4:92-95](grammars/pythonLexer.g4#L92-L95) has no prefix support; `f` lexes as a separate `NAME` |
| triple-quoted strings | `mismatched input` | same rule, no `'''`/`"""` alternative |
| list comprehensions `[p for p in ps]` | `mismatched input 'for'` | `listVal` [:215-219](grammars/pythonParser.g4#L215-L219) has no `for` clause (only `parenthedGenExpr` exists) |
| `class Foo:` | `extraneous input 'class'` | `CLASS` token exists [pythonLexer.g4:31](grammars/pythonLexer.g4#L31) but **no parser rule uses it** — a shadowed, unreachable token |
| `with open(f) as fh:` | `mismatched input` | no `with` keyword or rule |
| `try`/`except` | `extraneous input ':'` | no rule |
| `def f(a, b=2)` | `mismatched input '='` | `argsNames : NAME (COMMA NAME)*` [:179-180](grammars/pythonParser.g4#L179-L180) — no defaults |
| `def f(*args, **kwargs)` | `extraneous input '*'` | same rule |
| stacked decorators | `mismatched input '@'` | `func : (decorator)? DEF …` [:183-184](grammars/pythonParser.g4#L183-L184) — **exactly one** decorator |
| `{k: 1}` (variable key) | `mismatched input 'k'` | `dictItem : literal COLON ternaryExpr` [:236-237](grammars/pythonParser.g4#L236-L237) — keys must be literals |
| tuple unpacking `a, b = 1, 2` | not supported | `assignLine : target EQUAL ternaryExpr` [:43-44](grammars/pythonParser.g4#L43-L44) |

`CLASS` being tokenised but unreachable is worth pre-empting: an examiner reading the
lexer will see `class` in the keyword list and try it.

**These are acceptable as a documented subset** — the assignment is a Flask data
extractor, not a Python implementation. The problem is that [README.md:186](README.md#L186)
does not list them. **Fix: document the subset honestly** (see R-5). Free marks for
knowing your own boundaries; lost marks for being surprised by them.

---

### I-5 · Jinja subset: constructs a Jinja user would expect
**`grammars/templateLexer.g4:23-35`**, **`grammars/templateParser.g4`**

`J_STMNT_MODE` [templateLexer.g4:23-35](grammars/templateLexer.g4#L23-L35) defines
exactly ten keywords and **no generic `ID` rule**, so any unrecognised tag name fails
at the *lexer*, with a character-level error rather than a useful message.

| Construct | Result | Note |
| --- | --- | --- |
| `{% include "x.jinja" %}` | `token recognition error at: 'in'` | not in `J_STMNT_MODE` |
| `{% macro m() %}` | `token recognition error at: 'm'` | not in `J_STMNT_MODE` |
| `{{ a if b else c }}` | `mismatched input 'if'` | **the grammar uses C-style `{{ cond ? a : b }}`** ([templateParser.g4:136-137](grammars/templateParser.g4#L136-L137)) — which is **not valid Jinja**. `ELVIS: '??'` [:57](grammars/templateLexer.g4#L57) likewise. |
| `{%- if x -%}` | `token recognition error at: '-'` | no whitespace control |
| `{# TODO issue #12 #}` | `token recognition error` | `J_COMMENT : '{#' (~[#])* '#}'` [templateLexer.g4:11](grammars/templateLexer.g4#L11) — a `#` **inside** the comment terminates the match |

Supported and working: `{{ var }}`, `{{ obj.attr }}`, `{{ list[i] }}`, `{% for %}`/
`{% endfor %}` (incl. `{% for %}…{% else %}`), `{% if %}`/`{% elif %}`/`{% else %}`/
`{% endif %}`, `{% set %}`, `{% extends %}`, `{% block %}`/`{% endblock %}`, filters
with arguments, and `loop.index`/`index0`/`revindex`/`first`/`last`/`length`
([JinjaRenderer.java:307-317](src/visitors/JinjaRenderer.java#L307-L317)).

**On the audit brief's balanced-grammar question:** block tags **are** matched by real
balanced parser rules — `forBlock : forStartStatement forBody? forEndStatement`
[templateParser.g4:78-80](grammars/templateParser.g4#L78-L80), `ifBlock`
[:31-33](grammars/templateParser.g4#L31-L33). No regex, no manual scanning. An unclosed
`{% for %}` **is** a real parse error with a line number:

```
bad.jinja:1:46  mismatched input '</' expecting {'*', '+', '-', '/', '//', … '}}'}
```

The line/column are right; the expected-set is unhelpful (it lists expression operators
rather than "unclosed `{% for %}` opened at line 1"). Minor, but worth a custom
`BaseErrorListener` message if there is time.

**On lexer modes:** the brief's Critical-if-single-mode condition does **not** apply.
The template lexer uses **12 modes** with proper `pushMode`/`popMode`, including the
double-pop `J_EXPR_STMNT_END : '%}' -> popMode, popMode`
[templateLexer.g4:75](grammars/templateLexer.g4#L75) that unwinds `{% if <expr> %}`.
`{{ }}` inside an HTML attribute value is handled by a dedicated token
`ATTR_VAL_J_EXPR_START` [:115](grammars/templateLexer.g4#L115), and outside quotes by
`INSIDE_START_TAG_J_EXPR_OPEN` [:104](grammars/templateLexer.g4#L104). This is the
strongest part of the grammar work and should be led with in the defense.

---

### I-6 · HTML lexer: closed tag list, no `<script>` body, no `{`/`}`/`>` in text
**`grammars/templateFragments.g4:95-115`**, **`grammars/templateLexer.g4:19`**

| Input | Result | Cause |
| --- | --- | --- |
| `<svg><circle/></svg>` | `token recognition error at: 'cir'` | `HTML_TAG_NAME` [templateFragments.g4:95-115](grammars/templateFragments.g4#L95-L115) is a **closed list** — no SVG, no `<slot>`, `<hgroup>`, `<search>`, no custom elements |
| `<script>function f(){ }</script>` | `token recognition error at: '{ '` | **no `SCRIPT` lexer mode.** Script bodies fall into `NORMAL_TEXT`, which excludes `{`/`}` |
| `<p>set = {1, 2}</p>` | `token recognition error at: '{1'` | `NORMAL_TEXT : ~[<>{}\t\r\n]+` [templateLexer.g4:19](grammars/templateLexer.g4#L19) |
| `<p>a > b</p>` | `token recognition error at: '>'` | same charset |
| `<img src="a.png">` | `mismatched input '<EOF>'` | **void elements must be written `/>`**; there is no void-element list |
| `<p>x</div>` | **accepted, silently rewritten** | see below |
| `<style>` with `font-family: "Segoe UI"` | `token recognition error at: '"'` | `CSS_PROP_VAL : [a-zA-Z0-9#%(),.-]+` [templateLexer.g4:152](grammars/templateLexer.g4#L152) — no quotes |
| `<style>` with `@media (...)` | `token recognition error at: '@'` | no at-rule support in `CSS_BLK` |

The `<p>x</div>` case is the one to fix. `htmlRegularElement : htmlStartTag
htmlElementBody? htmlEndTag` [templateParser.g4:250-251](grammars/templateParser.g4#L250-L251)
does not constrain `END_TAG_NAME` to match `START_TAG_NAME`, and
[NodeVisitor.java:52-81](src/visitors/NodeVisitor.java#L52-L81) reads only the *start*
tag name and discards the end tag entirely. The compiler therefore **silently repairs**
the input, emitting `<p>x</p>` — changing the author's markup without a word. Silent
rewriting is worse than either accepting or rejecting.

**Fix (30 min, no grammar change needed):** in `visitHtmlRegularElement`, compare
`ctx.htmlStartTag().START_TAG_NAME().getText()` against
`ctx.htmlEndTag().END_TAG_NAME().getText()` and report a `SemanticError` on mismatch.

Also note the malformed unused fragment at
[templateFragments.g4:118-119](grammars/templateFragments.g4#L118-L119): `HTML_GLOBAL_ATTR`
has a missing `|` (`'id' 'inert'` concatenates) and an **empty alternative**
(`'spellcheck' |  | 'style'`). Harmless today because nothing references it, but delete
it before an examiner reads the file.

---

### I-7 · Git history does not reconcile with `ROLES.md`
**`ROLES.md`**, 60 commits

| Git author | Commits |
| --- | ---: |
| ahmad.sj `<ahmad-alsarraj@hotmail.com>` | 23 |
| Ahmad khaled `<eng.ahmadkhaled21@gmail.com>` | 16 |
| Dude1o `<meladnofal146@gmail.com>` | 11 |
| mariaali2000 `<mariamariaas2002@gmail.com>` | 5 |
| Dude1o `<meladnofal91@gmail.com>` | 4 |
| ahmad-sj `<ahmad-alsarraj@hotmail.com>` | 1 |

That is **four distinct people** (two of them committing under two identities each).
`ROLES.md` names **five**: Melad, Aram, Raghad, Ahmad, Yousef. "Aram", "Raghad" and
"Yousef" appear nowhere in the history; "Maria", who has 5 commits, appears nowhere in
`ROLES.md`.

Individual marks are awarded, and examiners do check `git log`. Either the role table
uses different names for the same people — in which case **map them explicitly** — or
three listed members have no committed work, which is a much harder conversation.

**Fix:** add a members file with full names + student IDs and, next to each, the git
identity they committed under. Consolidate the duplicate `Dude1o` identities with a
`.mailmap`.

---

### I-8 · `url_for('static', filename=…)` is a hard error
**`src/visitors/TemplateSemanticAnalyzer.java:388-405`**

`checkUrlFor` resolves the first argument against `routeNames` only. Flask's built-in
`static` endpoint is not a user route, so the canonical Flask idiom is rejected and
**blocks the build**:

```
[MISSING_FLASK_VARIABLE] line 1 | 'static' - t.jinja: url_for('static') refers to no route defined in app.py
=== Result === Generation blocked by 1 semantic error(s).
```

The demo sidesteps this by hardcoding `href="style.css"`
([base.jinja:7](project/templates/base.jinja#L7)). Since the output layout is flat that
path happens to resolve correctly — but "why don't you use `url_for` for your
stylesheet?" is a very natural examiner question, and the honest answer is currently
"because it fails the build".

**Fix:** special-case `static` in `checkUrlFor`, and in
[CodeGenerator.urlFor:288](src/app/CodeGenerator.java#L288) return the bare `filename`
value (correct for the flat output layout).

---

### I-9 · Attribute-key typos warn but do not block; inconsistent with variable typos
**`src/visitors/TemplateSemanticAnalyzer.java` (no key check)**, **`src/visitors/ExpressionEvaluator.java:222`**

`TemplateSemanticAnalyzer` tracks context as a `Set<String>` of **names only**
([:124-127](src/visitors/TemplateSemanticAnalyzer.java#L124-L127)), never the *shape* of
the data. So B6 check #4 — "attribute access matches keys actually present in the
data" — is not performed at analysis time. Tested with `{{ p.imagee }}` / `{{ p.nmae }}`:

```
No semantic errors found.
No template errors found.
WARNING  2 rendering problem(s)
  Key 'imagee' not present in [id, name, price, details, image]
  Key 'nmae' not present in [id, name, price, details, image]
=== Result === Success: 1 page(s) written
```

Exit code **0**, page emitted with `<img src=""/>`. Compare with an undefined
*variable*, which is a hard error that blocks generation. Same class of author mistake,
two different outcomes — that inconsistency is exactly what the audit brief asks about
under B8, and an examiner may well probe it.

**Fix:** the extractor already knows the real dict keys
([PythonDataExtractor.java:262-274](src/visitors/PythonDataExtractor.java#L262-L274)).
Pass `getModuleVars()` (values, not just names) into `TemplateSemanticAnalyzer` and
check `MemberTrailer` names against the keys of the sample item. Half a day, and it
closes the most valuable remaining cross-front-end check.

---

### I-10 · `output/` is never cleaned, so stale pages survive
**`src/app/CodeGenerator.java:104`**

`Files.createDirectories(config.outputDir)` only. Verified: a file dropped into
`output/` before a rebuild is still there afterwards. Delete a route or rename a
template and its old `.html` remains, indistinguishable from live output.
`compiler_output/` has the same issue
([FlaskCompiler.java:70](src/app/FlaskCompiler.java#L70)).

**Fix:** delete and recreate both directories at the start of a run, or track written
filenames and sweep the rest.

---

## Minor

### M-1 · Backtick indentation in the tree printer
[Node.java:72](src/models/Node.java#L72) — `indent.append("```")` per level. At depth 5
this is 15 consecutive backticks, which reads as broken Markdown. Change to
`"  "` or `"│  "` — a one-line fix that noticeably improves how the demo looks.

### M-2 · Whitespace between sibling nodes is not recoverable
`WS : [ \t\r\n]+ -> skip` [templateLexer.g4:18](grammars/templateLexer.g4#L18) and
`NORMAL_TEXT` excluding `\r\n` mean original inter-node spacing is gone. The renderer
substitutes `\n` between siblings ([JinjaRenderer.java:202-210](src/visitors/JinjaRenderer.java#L202-L210)).
Visible consequence: `${{ …price… }}` in `index.jinja:15` becomes

```html
<p class="price">
$
79.99
</p>
```

which a browser renders as **"$ 79.99"** with a stray space. Cosmetic but on the
front page of the demo. Workaround without a grammar change: put the `$` inside the
expression — `{{ "$%.2f"|format(product.price) }}`.

### M-3 · `TemplateVisitor` calls `exitScope()` with no matching `enterScope()`
[TemplateVisitor.java:48](src/visitors/TemplateVisitor.java#L48). The matching
`enterScope` is in `NodeVisitor.visitExtendsBlock`
([:387](src/visitors/NodeVisitor.java#L387)), so it is only balanced for templates that
use `{% extends %}`. Harmless because `exitScope` guards on `parent != null`
([SymbolTable.java:21](src/symbols/SymbolTable.java#L21)), but the scope bookkeeping is
split across two classes and is fragile.

### M-4 · Symbol ordering is nondeterministic
`Scope.symbols` is a `HashMap` ([Scope.java:16](src/symbols/Scope.java#L16)), so
`symbol_table.txt` can reorder between runs. Use `LinkedHashMap`.

### M-5 · Mixed tabs and spaces parse silently
A file mixing `\t` and 8-space indentation parses without complaint; Python 3 raises
`TabError`. Inconsistent indentation *is* caught (`extraneous input '      '`), though
the message is not user-friendly. `Python3LexerBase.getIndentationCount`
([:83](src/antlr/Python3LexerBase.java#L83)) uses the classic tab=8 rule.

### M-6 · Dead code
`Node.resetIdSequence()` ([Node.java:33](src/models/Node.java#L33)) — 0 call sites.
`TestNode` ([TestNode.java](src/models/TestNode.java)) — never constructed, and has no
`print()` override, so if it ever were it would emit the
`"################## method print is not overrided"` placeholder from
[Node.java:64](src/models/Node.java#L64). Delete both.

### M-7 · `tests/*.html` are orphans
`tests/base.html`, `index.html`, `add.html`, `detail.html`, `edit.html`, `tests.html`
are referenced by neither `check.ps1` nor `runtime-test.js`. `tests/base.html` is the
**only** file in the repo containing a `<style>` block — so the one artifact that would
have exercised the CSS grammar is dead weight. Delete or wire in.

---

## Part B checklist — items verified clean

These were checked and are **correct**; they are listed so the team knows what not to
worry about, and so the defense can claim them confidently.

| Brief item | Result |
| --- | --- |
| **B1** — generator consults the symbol table? | **No.** `CodeGenerator`, `JinjaRenderer`, `ExpressionEvaluator` and `PythonDataExtractor` contain **zero** symbol-table references. The stated design rule holds. (`NodeVisitor` does use one, but that is AST construction, not generation — see C-3/R-3.) |
| **B1** — all 4 `compiler_output/` artifacts produced every run? | **Yes**, plus 5 extra (`ast_python.txt`, `ast_jinja.txt`, `symbol_table.txt`, `tokens.txt`, `parse_tree.txt`). Not stale: `compiler_output/` is gitignored and regenerated on every run. |
| **B2** — `removeErrorListeners()` + custom listener? | **Yes** — [AppHandler.java:76-84](src/app/AppHandler.java#L76-L84), [TemplatesHandler.java:135-142](src/app/TemplatesHandler.java#L135-L142), both lexer and parser, using `util.SyntaxErrors`. |
| **B2** — syntax error halts the pipeline? | **Yes.** `AppHandler.parse()` returns `null` [:94](src/app/AppHandler.java#L94); a broken template is dropped and then reported as a hard semantic error. Verified: a template with an unclosed `{% for %}` produced `Generation blocked`, exit 1, **zero** files written. |
| **B2** — keywords precede `NAME`? | **Yes** — keywords [pythonLexer.g4:30-50](grammars/pythonLexer.g4#L30-L50), `NAME` at [:89](grammars/pythonLexer.g4#L89). No shadowed rules except unreachable `CLASS` (I-4). |
| **B2** — `==`, `!=`, `>=` split into single chars? | **No** — maximal munch handles them correctly; verified in `tokens.txt`. (`//` and `**` are absent entirely — I-3.) |
| **B2** — comments/blank lines skipped? | **Yes** — `SKIP_ : (SPACES \| COMMENT \| LINE_JOINING) -> skip` [pythonLexer.g4:25](grammars/pythonLexer.g4#L25). |
| **B2** — INDENT/DEDENT mechanism | Custom lexer superclass **`Python3LexerBase`** — [pythonLexer.g4:13](grammars/pythonLexer.g4#L13), [src/antlr/Python3LexerBase.java:9](src/antlr/Python3LexerBase.java#L9). A port of the standard ANTLR Python3 solution: brace-depth tracking (`opened`), synthetic token emission, multi-DEDENT unwinding at EOF. **5-level nesting parses correctly.** |
| **B3** — lexer modes used? | **Yes — 12 of them.** See I-5. Not a single-mode hack. |
| **B4** — own AST or ANTLR's parse tree? | **Own, for both front-ends.** 98 hand-written classes; no `ParserRuleContext` reaches the AST. |
| **B4** — line/column propagated into every node? | **Line: yes**, 1,157/1,157 nodes non-zero. **Column: no** — `Node` has no column field. The requirement only names line numbers, so this is not a gap, but be ready for "why no column?" |
| **B4** — JSON serialized from your AST or the parse tree? | **From the AST.** [AstDumper.java](src/app/AstDumper.java) reflects over each `Node`'s declared fields, with a depth guard and identity-based cycle detection. Emits `id`/`node`/`type`/`line` per node. Matches the node classes exactly, because it *is* the node classes. |
| **B5** — `visitChildren()` silent fall-through? | **Checked all 96 template rules and all Python rules. No information is lost.** Rules with no override are single-child pass-throughs (`htmlElement`, `jinjaElement`, `htmlTagAttr`) or are consumed inline by their parent (`ifBody` is iterated child-by-child at [NodeVisitor.java:616-618](src/visitors/NodeVisitor.java#L616-L618); `defaultExpr`/`ternaryExt` are handled inside `visitExpression` [:644-687](src/visitors/NodeVisitor.java#L644-L687)). Every one of the 102 node classes is constructed somewhere. |
| **B5** — labeled alternatives all implemented? | **Yes.** Every `#label` in both grammars has a corresponding override. |
| **B5** — passes per tree | Python AST: 3 (`SemanticAnalyzer`, `PythonDataExtractor`, and the printer/dumper). Jinja AST: 3 (`TemplateSemanticAnalyzer`, `JinjaRenderer`, printer/dumper). All independent; none mutates the tree. |
| **B6** — semantic analysis a distinct pass? | **Yes** — Phase 3 at [FlaskCompiler.java:98-99](src/app/FlaskCompiler.java#L98-L99), entirely separate from generation (Phase 6). `AppVisitor`'s class comment records that this was deliberately split out. |
| **B6** — errors collected and reported together? | **Yes**, with type, line, symbol and message, into `semantic_report.txt`. |
| **B6** — failing analysis blocks generation? | **Yes** — [CodeGenerator.java:83-87](src/app/CodeGenerator.java#L83-L87). Verified: 27/27 invalid-backend fixtures blocked. |
| **B6** — report meaningful on a clean run? | **Yes** — explicit `RESULT: clean` with per-category zero counts, not an empty file. |
| **B7** — report claims bytecode/execution? | **No.** README correctly describes AST-walking. See A7. |
| **B8** — substitution by AST walk or regex? | **AST walk.** [JinjaRenderer.java:143-191](src/visitors/JinjaRenderer.java#L143-L191) dispatches on node type. The class comment documents that a prior regex implementation was removed because it dropped every `{% else %}`. |
| **B8** — loop expansion, scoping, shadowing | **Correct.** Fresh `LinkedHashMap` scope per iteration [:283](src/visitors/JinjaRenderer.java#L283), so the loop variable shadows an outer name for the body only. Multi-variable unpacking supported [:290-311](src/visitors/JinjaRenderer.java#L290-L311). |
| **B8** — conditional evaluation | **Correct** — `if`/`elif` chain/`else`, all six comparison operators, `and`/`or`/`not`/`in`/`is`, truthiness. Verified by `check.ps1` group [5], 5/5. |
| **B8** — undefined variable policy | Consistent: `null` → reported via `Problems` → renders as empty string. Matches the semantic check, which blocks first. |
| **B8** — raw HTML/CSS/JS pass through unmodified? | HTML and inline CSS: **yes** (`rgba(0, 0, 0, 0.1)` round-trips exactly). `<style>` blocks: **no — C-1**. `<script>` bodies: cannot be lexed at all — I-6. |
| **B8** — output well-formed, no leftover delimiters? | **Yes** — verified on all 10 pages; `check.ps1` asserts DOCTYPE + no un-rendered Jinja. |
| **B9** — `app.py`/`style.css`/`script.js` copied byte-for-byte? | **Yes** — `Files.copy` [CodeGenerator.java:337](src/app/CodeGenerator.java#L337); MD5-verified by `check.ps1`. |
| **B9** — asset paths valid in the flat output? | **Yes** — `href="style.css"` resolves correctly. The `url_for('static', …)` trap is I-8. |
| **B10** — regeneration not cached | **Verified.** Added a 4th product to `app.py`, re-ran: `index.html` gained exactly the 16-line card for `USB-C Hub`, and `product_detail_4.html`/`edit_product_4.html` appeared. Nothing short-circuited. |
| **B10** — new `.jinja` + route with no code change | **Verified.** Dropped in `about.jinja` + an `/about` route: `about.html` generated, `{{ products\|length }}` → `3`. Zero compiler changes. |
| **B11** — clean clone builds and runs | **Yes** for Java (`.\build.ps1` → `Build OK`), **no** for grammars (C-4). Requires **Java 21+** (`List.getFirst()` is used at [NodeVisitor.java:237](src/visitors/NodeVisitor.java#L237) and elsewhere) and ships **ANTLR 4.13.2** vendored at `dependencies/`. |

---

# ARCHITECTURE WALKTHROUGH

## Actual pipeline, by real class name

```
                      FlaskCompiler.main → CompilerConfig.fromArgs
                                    │
        ┌───────────────────────────┴────────────────────────────┐
        ▼ Phase 1                                        Phase 2 ▼
   AppHandler.parse()                            TemplatesHandler.parseAll()
   Source.read → pythonLexer                     Source.read → templateLexer
     (Python3LexerBase: INDENT/DEDENT)             (12 lexical modes)
        │ CommonTokenStream                            │ CommonTokenStream
        ▼                                              ▼
   pythonParser.prog()                           templateParser.template()
   + SyntaxErrors listener                       + SyntaxErrors listener
        │ ParseTree                                    │ ParseTree
        ▼                                              ▼
   AppVisitor  (BaseVisitor<App>)                TemplateVisitor (BaseVisitor<Template>)
     └─▶ PythonVisitor (BaseVisitor<Node>)         └─▶ NodeVisitor (BaseVisitor<Node>)
        │                                              │        └── writes SymbolTable #1  ⚠ C-3
        ▼                                              ▼
   models.App { ArrayList<Node> }                models.Template { ArrayList<Node> }
   ══ PYTHON AST ══                              ══ JINJA + HTML + CSS AST ══
        │                                              │
        ├─────────────▶ TreePrinter / AstDumper ◀──────┤   (ast_*.txt, ast_*.json,
        │                                              │    tokens.txt, parse_tree.txt)
        ▼ Phase 3                                      │
   SemanticAnalyzer                                    │
   └── SymbolTable #2 (private, never printed) ⚠ C-3   │
        │ List<SemanticError>                          │
        ▼ Phase 4                                      │
   PythonDataExtractor                                 │
   ├── Map<String,Object> moduleVars                   │
   └── List<RouteInfo>  (name, urlPattern, params,     │
        templateName, context, collectionName…)        │
        │                                              │
        │            Phase 5 ▼                         │
        └──────────▶ TemplateSemanticAnalyzer ◀────────┘
                     (names + routes + templates)
                            │ List<SemanticError>
                            ▼ Phase 6   ← blocked here if any error
                     CodeGenerator.generate()
                     ├── baseContext(route)  ══ CONTEXT DATA — THE MEETING POINT ══
                     ├── JinjaRenderer.render(template, context)
                     │     └── ExpressionEvaluator.eval(node, context)
                     ├── writeClientData()   → data.js
                     └── copyStaticAssets()  → app.py, style.css, script.js
                            │
                            ▼
                     output/*.html
```

## Diff against the brief's intended diagram

| Stage | Status |
| --- | --- |
| Python lexer/parser → Python AST | **Matches.** |
| Jinja lexer/parser → Jinja AST | **Matches**, and covers HTML + CSS too, which the diagram does not show. |
| Semantic analysis (symbol table, checks) | **Matches**, and is stronger than the diagram: there are **two** analyzers, and the second one checks *across* the front-ends. |
| Generator → Context Data | **Matches.** `PythonDataExtractor` is the "Generator" box; `moduleVars` + `RouteInfo.context` are "Context Data". |
| `render_template()` → substitution → HTML | **Matches.** |
| **Extra stage not in the diagram** | `writeClientData()` emits `data.js`, and `script.js` provides a localStorage runtime so add/edit/delete work in a static site. Legitimate bonus work — but it is **not** part of the assignment, and the report should present it as such. |
| **Merged/bypassed stages** | None. Every box in the diagram is a distinct class. |

**Front-end separation — the honest answer.** Separate grammars: **yes**. Separate
token vocabularies: **yes**. Separate visitors: **yes**. Separate AST *classes*: **no,
not fully.** `PythonVisitor` imports and constructs `models.jinja.atoms.*`
(`IdType`, `IntType`, `FloatType`, `StringType`, `ListType`),
`models.jinja.expressions.*` (`Argument`, `ArgumentList`) and
`models.jinja.trailers.*` (`CallTrailer`, `MemberTrailer`, `SubTrailer`) —
[PythonVisitor.java:6-9](src/visitors/PythonVisitor.java#L6-L9). So the Python AST is
built partly out of node classes that live in the Jinja package. This is pragmatic
reuse (the two languages really do share literal and access-chain shapes), and it is
what lets `PythonDataExtractor` and `ExpressionEvaluator` share atom handling — but be
ready to defend it as a *deliberate* decision rather than be caught by it. The two
front-ends meet at **`Map<String, Object>` context data**
([CodeGenerator.baseContext:262-272](src/app/CodeGenerator.java#L262-L272)), which is a
clean boundary of plain Java values with no AST types crossing it.

## AST class diagram — as actually implemented

Check this against the diagram in `REPORT.md:90-124`; they agree.

```
models.Node  (abstract)                            ← THE BASE CLASS (A2.3)
│   int    nodeId       final, AtomicInteger, construction order
│   String nodeName     defaults to getClass().getSimpleName()
│   int    lineNumber
│   String header()             → "#12 assign line (line 7)"
│   String print(int level)     ← THE OVERRIDDEN METHOD (A2.4), 66 overrides
│   String getIndent(int level)
│
├── DocType · NormalText · NodeBody(children) · TestNode(dead)
│
├── PYTHON  (models.python.*) — flat: nearly everything extends Node directly
│   ├── Func · Decorator · Name · Value · BlockNode
│   ├── Statement (abstract) ──── SingleImport            ⚠ only 1 subclass
│   ├── BlockNode ─────────────── blocks.ElseBlock        ⚠ only 1 subclass
│   ├── blocks/     ForNode · WhileNode · ElifBlock ◀── IfBlock   ⚠ IfBlock is-a ElifBlock
│   ├── expressions/ CompareExpression · EqualExpression · GenExpression
│   │                MulExpression · NegatedExpression
│   ├── literals/    Dict · DictItem · TrueValue · FalseValue · NoneValue
│   │                CompareOperator
│   └── simple_statements/ AssignLine · ExprLine · ReturnLine · Pass · TernaryExpr
│                          import_lines/{SingleImport, MultiImport}
│                          return_expr/{SingleReturnNode, TupleReturnNode}
│
├── JINJA  (models.jinja.*) — deep, well-factored
│   ├── JinjaExpression                          {{ … }}
│   ├── JinjaBlock (abstract)
│   │   └── IfBlock · ElifBlock · ElseBlock · ForBlock
│   │       ExtendsBlock · InheritedBlock · SetStatement
│   ├── Expression (abstract)                    ← 18 subclasses
│   │   └── Or/And/Not · Comparison · In · Is · Add · Mul · Power · Concat
│   │       Unary · Ternary · Default · Parenthed · Pipe · Filter
│   │       Primary · Argument · ArgumentList · Operator
│   ├── Atom (abstract)
│   │   └── IdType · IntType · FloatType · StringType · ListType · DictType · PairType
│   └── Trailer (abstract)
│       └── MemberTrailer(.x) · SubTrailer([i]) · CallTrailer((…))
│
├── HTML  (models.html.*)
│   ├── HtmlElement (abstract)  { String tagName; List<Node> attrList; }
│   │   └── HtmlRegularElement · HtmlSelfClosingElement · HtmlStyleElement
│   ├── Attribute (abstract)
│   │   └── BooleanAttribute · QuotedAttribute · UnquotedAttribute · StyleAttribute
│   └── AttributeValue
│
└── CSS  (models.css.*)
    ├── CssBlock  { selectors, properties }
    ├── Selector (abstract)
    │   └── IdSelector · ClassSelector · ElementSelector
    │       DescendantSelector · GroupSelector · PseudoClassSelector
    └── properties/ Property · PropertyValue

NOT Node subclasses (plain containers — correct, but note R-2):
    App { ArrayList<Node> nodes; List<SemanticError> }
    Template { ArrayList<Node> nodes; String templateName; boolean hasExtends }
    RouteInfo { name, urlPattern, params, templateName, context, collectionName… }
```

**Asymmetry to be ready for.** The Jinja/HTML/CSS side has 8 abstract intermediates and
a clean hierarchy. The Python side is nearly flat — `Statement` and `BlockNode` each
have exactly one subclass, and `python.blocks.IfBlock extends ElifBlock` inverts the
natural relationship (an `if` is not a kind of `elif`). If an examiner asks "show me
your inheritance", **lead with `Expression` (18 subclasses) or `HtmlElement`/`Attribute`,
not the Python package.**

---

# LIKELY EXAMINER QUESTIONS, PER PHASE

## The nine named in the brief

**1. "Where are inheritance and polymorphism in your AST? Show me the base class and an overridden method."**
Strong. `models.Node` at [Node.java:16](src/models/Node.java#L16); overridden method
`public String print(int level)` at [:63](src/models/Node.java#L63), 66 overrides;
polymorphic dispatch at [TreePrinter.java:53](src/app/TreePrinter.java#L53)
(`node.print(0)` through a `Node` reference) and recursively at
[NodeBody.java:38](src/models/NodeBody.java#L38). Show `Expression` (18 subclasses) for
depth. **Have `HtmlElement` and `Attribute` ready as a second example** and steer away
from the Python package (see the asymmetry note above).

**2. "Is your AST your own, or ANTLR's parse tree?"**
Strong. Your own — 98 classes. The killer demo is opening `compiler_output/parse_tree.txt`
(295 KB, one node per grammar rule, `'/'` descending 13 levels of the precedence chain)
next to `ast_python.txt` (12 KB, the same route as a handful of meaningful nodes).
`REPORT.md:299-334` already frames this comparison well.

**3. "Show me a node's line number and node name in the printed output."**
**Weak — practise this.** In the text tree, `#8 multi import line (line 2)` appears only
on top-level frames; nested nodes show at most `line no:`, and 36 classes show nothing
(I-1). **Answer with the JSON, not the text tree:** `ast_python.json` gives
`"id"/"node"/"type"/"line"` on all 1,157 nodes with zero at line 0. Then say "the text
printer shows identity at the frame level; the JSON dump carries it on every node."

**4. "Why two grammars instead of one? Where are the HTML and CSS grammars?"**
Strong. Two, because Python is indentation-sensitive (needs a lexer superclass emitting
synthetic INDENT/DEDENT) while templates are mode-sensitive — one token set cannot do
both. HTML and CSS are inside `templateLexer.g4` / `templateParser.g4`, as lexer modes
plus parser rules producing `HtmlElement`/`CssBlock` nodes. **Risk: if they then ask
"show me CSS being parsed", you currently have nothing in `project/` — fix C-1 and C-2
first.**

**5. "How does your lexer separate raw HTML text from Jinja tags?"**
**Your strongest answer.** Twelve lexical modes. Default mode emits `NORMAL_TEXT`;
`'{{' -> pushMode(EXPRESSION_MODE)`, `'}}' -> popMode`; `'{%' -> pushMode(J_STMNT_MODE)`,
then `if`/`for` push `EXPRESSION_MODE` *on top*, so `'%}'` needs the double-pop
`J_EXPR_STMNT_END : '%}' -> popMode, popMode` — [templateLexer.g4:75](grammars/templateLexer.g4#L75).
`{{ }}` inside an attribute is a separate token (`ATTR_VAL_J_EXPR_START`) reaching the
same mode. Show `tokens.txt` and point at the mode switches. Lead with this.

**6. "Walk me through your Symbol Table's helper methods."**
**Weakest question. Fix C-3 before the defense.** Today, printing the table shows 11
rows of Jinja block names and no Python symbols, and 6 of the 11 helpers have zero call
sites. If unfixed, be honest: "there are two tables; the analyzer's is the substantive
one and we currently print the template one — here it is via the analyzer." Better:
spend an hour on C-3 and A4.2 so the question becomes a strength.

**7. "What happens if a template uses a variable the context does not provide?"**
Strong. Live-demo it: `TemplateSemanticAnalyzer` reports
`[UNDEFINED_VARIABLE] line 1 | 'missing_name' - t.jinja: 'missing_name' is not provided
to this template`, `CodeGenerator` refuses to run, exit code 1, **zero files written**.
Then explain the design choice: the analyzer walks the `{% extends %}` chain with the
*child's* context, because that is exactly how the renderer resolves it. **Follow-up
risk: "and if a *key* is missing on a defined variable?" — that is only a warning
(I-9). Know the difference and why.**

**8. "Why doesn't the generator use the symbol table?"**
Strong on the substance — grep `CodeGenerator`/`JinjaRenderer`/`ExpressionEvaluator`/
`PythonDataExtractor` for `SymbolTable` and get zero hits. The answer: the symbol table
answers "is this name legal here?", generation answers "what is this name's value?" —
different questions, and coupling them would let a checking decision change the output.
**Do not repeat the report's blanket claim (R-3)** that the table is used *only* by
semantic analysis; `NodeVisitor` uses one during AST construction. Say "the *generator*
never touches it" and you are exactly right.

**9. "Add a 'delete product' flow right now — what would you change?"**
**Nothing — it already exists.** Route at [app.py:69-74](project/app.py#L69-L74),
verified working by `check.ps1` groups [F] and [G]. Explain the interesting part: it is
POST-only and renders no template, so `RouteInfo.rendersTemplate()` is false, no page is
generated, and `url_for('delete')` resolves to `#` with a diagnostic
([CodeGenerator.java:294-299](src/app/CodeGenerator.java#L294-L299)). The actual
deletion is a form in `product_detail.jinja:42` handled by the localStorage runtime.
**If they want a *new* flow (say "archive product"): add the route to `app.py` and an
`archive.jinja` — zero compiler changes.** I verified exactly this with a new
`about.jinja` + `/about` route: it generated with no code change at all.

## Additional questions this implementation is weak against

**"Type `x = -5` into your app.py and rebuild."** → parse error (I-3). The most likely
spontaneous test an examiner performs. **Fix I-3 or have the answer ready.**

**"Edit a `.g4` file and show me the change take effect."** → nothing happens (C-4).
**Fix this first.**

**"Put a `<style>` block in your template."** → `<style><style>` malformed output (C-1).

**"What if a product name contains HTML?"** → injected raw (C-5).

**"Why is your `<p class="price">` rendering as `$ 79.99` with a space?"** → whitespace
between siblings is not recoverable from the token stream (M-2). Honest answer:
`NORMAL_TEXT` excludes newlines and `WS` is skipped, so we re-insert `\n` between
siblings; the fix is to preserve whitespace on a hidden channel.

**"`<p>x</div>` — what does your compiler do?"** → silently emits `<p>x</p>` (I-6).
The honest answer is "the grammar doesn't constrain the end-tag name and the visitor
discards it; it should be a semantic check."

**"Use `url_for('static', filename='style.css')`."** → hard error, build blocked (I-8).

**"Show me an ANTLR listener."** → there are none; the project is visitor-only. That is
a defensible choice — visitors give explicit control over traversal order and return
typed values, which is what tree *construction* needs, whereas a listener is pushed by
the walker and returns nothing. Say it that way.

**"Why are child node IDs lower than their parents'?"** → IDs are assigned in
constructor order and the visitor builds children before the parent that holds them.
`ROLES.md` already flags this; it is a good answer that shows you understand your own
traversal.

---

# DIVISION OF KNOWLEDGE FOR THE DEFENSE

[ROLES.md](ROLES.md) already assigns five areas along clean module boundaries, and the
split matches the real code. Adopt it, with the audit-specific additions below. **The
git-history discrepancy (I-7) must be resolved before submission** — the mapping from
these names to committer identities needs to be explicit.

| # | Owner | Phase | Files to own | Demo artifact | Must be able to answer | Must fix before the defense |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | **Melad** | Grammar, lexer, parser | `grammars/*.g4`, `src/antlr/Python3LexerBase.java` | `compiler_output/tokens.txt` | Q4, Q5 · why INDENT/DEDENT · why 12 modes · double-pop `%}` | **C-4** (build regenerates grammars) · **I-2**, **I-3** (precedence + missing operators) |
| 2 | **Aram** | AST classes & printer | `src/models/**`, `TreePrinter.java`, `AstDumper.java` | `ast_python.txt`, `ast_python.json` | Q1, Q2, Q3 · base class, overridden method, dispatch site · why child IDs are lower | **I-1** (`print()` should call `header()`) · **M-1** (backtick indent) |
| 3 | **Raghad** | Visitors | `AppVisitor`, `PythonVisitor`, `TemplateVisitor`, `NodeVisitor` | `parse_tree.txt` beside `ast_*.txt` | how `BaseVisitor<T>` is specialised · why `elif`/`else` nest inside the if-body · why no listeners | **I-6** (start/end tag-name check — a visitor-level fix) |
| 4 | **Ahmad** | Symbol table & semantic analysis | `src/symbols/**`, `SemanticAnalyzer`, `TemplateSemanticAnalyzer`, `tests/**` | `symbol_table.txt`, `semantic_report.txt` | Q6, Q7, Q8 · scope chain and shadowing · why false positives are worse than misses | **C-3** (print the analyzer's table) · **A4.2** (dead helpers) · **I-8** (`url_for('static')`) · **I-9** (key checking) |
| 5 | **Yousef** | Generation & demo | `PythonDataExtractor`, `JinjaRenderer`, `ExpressionEvaluator`, `CodeGenerator`, `project/**` | `output/index.html` in a browser | Q8, Q9 · why generation never touches the symbol table · how a parameterized route discovers what to iterate | **C-1** (`<style>` double tag) · **C-5** (escaping) · **C-2** (put CSS in a template) · **I-10** (clean `output/`) |

**Everyone** should be able to run the one command and narrate their own stage:

```powershell
.\build.ps1
java -cp "out\classes;dependencies\antlr-4.13.2-complete.jar" app.FlaskCompiler --print-all
```

---

# PRIORITISED FIX LIST

| Priority | Item | Effort | Why now |
| --- | --- | --- | --- |
| 1 | **C-4** — wire ANTLR into `build.ps1`/`build.sh` | 30 min | Without it no grammar change can be demonstrated live. Highest viva risk. |
| 2 | **C-3** — print the analyzer's symbol table | 1 h | Turns the weakest question (Q6) into a strength; also fixes report claim R-3. |
| 3 | **I-3** — unary minus, `**`, `//` | 1 h | `x = -5` is the first thing an examiner types. |
| 4 | **C-1** — `<style>` double-tag bug | 30 min | Malformed HTML; blocks C-2. |
| 5 | **C-2** — put a `<style>` block in `base.jinja` | 20 min | Makes the CSS grammar demonstrable (A1.4). |
| 6 | **R-1/R-2/R-3/R-5** — report corrections | 1 h | Cheapest marks in the whole list; wrong class names get caught. |
| 7 | **I-7** + **A7.3/A7.4** — members file, `.mailmap`, submission zip | 1 h | Individual marks depend on it. |
| 8 | **I-1** — `print()` calls `header()` | 2 h | Directly addresses A2.7/A5.4 and examiner Q3. |
| 9 | **C-5** — escape by default, add `\|safe` | 2 h | Likely question about a template engine. |
| 10 | **I-2** — `not` precedence | 1 h | Demonstrable wrong answer; needs C-4 first. |
| 11 | **I-6** — start/end tag-name check | 30 min | Stops silent rewriting of the author's markup. |
| 12 | **I-8**, **I-9**, **I-10**, **M-1**, **M-2**, **M-4**, **M-6**, **M-7**, **B12 hygiene** | ½ day | Polish. `bin/` (50 `.class`), `.idea/`, `grammars/gen/`, `grammars/.antlr/` should all be gitignored and removed from the index — 88 committed build artifacts in total. |

*Audit complete. No source files were modified.*
