# Compilers Project — Report

A compiler front-end for a combined **Python + Jinja2 + HTML + CSS** language,
implemented in Java on ANTLR 4, plus a back-end that renders the parsed input to
a working static site.

> An Arabic version of this report is available in [REPORT.ar.md](REPORT.ar.md).

| Part | Where |
| --- | --- |
| Grammar / lexer / parser | [grammars/](grammars/) → generated into [src/antlr/](src/antlr/) |
| AST classes | [src/models/](src/models/) — 98 node classes |
| Visitors | [src/visitors/](src/visitors/) |
| Symbol table | [src/symbols/](src/symbols/) |
| Tree printer | [src/app/TreePrinter.java](src/app/TreePrinter.java) + `print()` on every node |
| Demo app | [project/](project/) |

---

## 1. Grammar, Lexer & Parser

Two grammar families, kept separate because the two languages have genuinely
different lexical rules — Python is indentation-sensitive, templates are
mode-sensitive.

| Grammar | Lines | Covers |
| --- | ---: | --- |
| `pythonLexer.g4` | 101 | 52 token rules, incl. INDENT/DEDENT |
| `pythonParser.g4` | 274 | statements, functions, decorators, expressions |
| `templateLexer.g4` | 154 | 108 token rules across lexical modes |
| `templateParser.g4` | 336 | 68 rules: Jinja, HTML, CSS |
| `templateFragments.g4` | 118 | shared character fragments |

**Python.** Indentation is handled by
[`Python3LexerBase`](src/antlr/Python3LexerBase.java), named as the lexer's
`superClass` in [pythonLexer.g4:13](grammars/pythonLexer.g4#L13). It synthesises
INDENT/DEDENT tokens the parser can use as block delimiters — the standard
solution for an off-side-rule language in a context-free grammar.

**Templates — lexical modes.** A template file is really four languages
interleaved, so the lexer switches modes rather than trying to write one token
set that covers all of them:

```
DEFAULT ──'<'──▶ START_TAG_MODE ──'style'──▶ STYLE_START_TAG_MODE ──▶ CSS_BLK
   │                    │                                              │
   │                    └──'="'──▶ ATTR_VAL_QOUTED                      ├──▶ CSS_BLK_PROP
   │                                    │                              │        │
   └──'{{'──▶ EXPRESSION_MODE ◀─────────┘                              │        └──▶ CSS_PROP_VALUES
   └──'{%'──▶ J_STMNT_MODE                                    CSS_INLINE (style="...")
```

This is why `{{ product.name }}` is tokenised the same way inside body text and
inside `src="…"` — both push `EXPRESSION_MODE`.

**Jinja coverage.** `{{ }}` expressions, `{% %}` statements, filters
(`concatExpr filter*` where `filter : PIPELINE ID (LPAREN argumentList? RPAREN)?`),
and control blocks: `if` / `elif` / `else`, `for` / `else`, `set`, `extends`,
`block`.

**CSS coverage.** `cssBlock : selectorList CSS_LBRACE cssProp* BLK_RBRACE`, with
id / class / element / descendant / group / pseudo-class selectors.

Demonstrated by the `<style>` block in
[base.jinja](project/templates/base.jinja), which holds the styles for the
markup that template owns. One run over `project/` builds 7 `CssBlock` nodes and
exercises five of the six selector kinds — 9 element, 2 descendant, 1 class,
1 group and 1 pseudo-class. Only `IdSelector` is unused, because no markup in
the demo carries an `id`. `style.css` is copied verbatim and never parsed, which
is why it can keep rules the grammar does not accept, such as
`form:not([style*="display: inline"])`.

---

## 2. Abstract Syntax Tree

102 files under `src/models/`: **98 node classes**, all descending from the single
abstract base `Node`, plus three plain containers — `App`, `Template` and
`RouteInfo` — which *hold* nodes rather than being nodes themselves.

Every node stores the three required pieces of identity:

```java
public abstract class Node {
    private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

    protected final int nodeId;      // node ID (number), unique per run
    protected String nodeName;       // node name/type
    protected int lineNumber;        // source line

    protected Node() {
        this.nodeId = ID_SEQUENCE.incrementAndGet();
        this.nodeName = getClass().getSimpleName();   // sensible default
    }
    public String header() { return "#" + nodeId + " " + nodeName + " (line " + lineNumber + ")"; }
    public String print(int level) { ... }            // overridden per subclass
}
```

IDs are assigned in construction order, so they also record the order in which
the parser built the tree.

### Inheritance hierarchy

```
                              Node  (abstract)
                                │
      ┌───────────┬─────────────┼──────────────┬──────────────┬────────────┐
      │           │             │              │              │            │
   DocType    NormalText    NodeBody      python.*        jinja.*       html.* / css.*
                                │              │              │            │
                          (child list)         │              │            │
                                               │              │            ├── HtmlElement (abstract)
                    ┌──────────────────────────┤              │            │     ├── HtmlRegularElement
                    │                          │              │            │     ├── HtmlSelfClosingElement
              Func / Decorator          Statement / Value     │            │     └── HtmlStyleElement
              BlockNode                 AssignLine            │            ├── Attribute (abstract)
              blocks.IfBlock            ReturnLine            │            │     ├── QuotedAttribute
              blocks.ForNode            ExprLine              │            │     ├── BooleanAttribute
              blocks.WhileNode          import_lines.*        │            │     └── StyleAttribute
              expressions.*             literals.*            │            └── CssBlock / Selector / Property
                                                              │
                          ┌───────────────────────────────────┤
                          │                    │              │
                   JinjaExpression      JinjaBlock (abstract)  Expression (abstract)
                                              │                     │
                                     IfBlock / ElifBlock       AddExpression / MulExpression
                                     ElseBlock / ForBlock      ComparisonExpression / AndExpression
                                     ExtendsBlock              OrExpression / NotExpression
                                     InheritedBlock            PipeExpression / FilterExpression
                                     SetStatement              PrimaryExpression + trailers.*
                                                               TernaryExpression / atoms.*
```

Polymorphism carries the design: the renderer and the printer both hold
`Node` references and call `print(level)` or dispatch on node type, never
knowing the concrete class in advance.

### A concrete tree

The `{% for %}` in `index.jinja`, as actually built — node IDs below are read
straight out of `compiler_output/ast_jinja.json`:

```
#936 ForBlock (line 10)
 ├── loopVars ──▶ #846 IdType "product"
 ├── iterable ──▶ #848 PrimaryExpression
 │                 └── atom ──▶ #847 IdType "products"
 └── nodeBody ──▶ #935 NodeBody
                   ├── #916 HtmlRegularElement <div class="card">
                   │        └── … img, card-body, title, price, detail link
                   └── #934 ElseBlock (line 20)      ← the {% else %} branch
```

Two things this shows. Child IDs are lower than their parent's, because the
visitor builds children before constructing the node that owns them — the IDs
record construction order. And `{% else %}` is a **child of the for-body**, not
a sibling of the loop; the renderer relies on exactly that shape.

Where the IDs appear:

| Output | Node IDs |
| --- | --- |
| `ast_python.json` / `ast_jinja.json` | on **every** node, as `"id"` |
| `ast_python.txt` / `ast_jinja.txt` | on each top-level node's header line |

The text tree's inner lines come from each class's own `print()` method, which
renders name and line; the JSON dump is the exhaustive per-node view.

The tree is built once and **not** consumed by rendering — the renderer walks it
read-only, so it is still intact after execution and is dumped to
`compiler_output/` afterwards.

---

## 3. Visitor

ANTLR generates `templateParserBaseVisitor<T>` / `pythonParserBaseVisitor<T>`;
each visitor overrides the rule methods it cares about and returns model objects.

| Visitor | Role |
| --- | ---: |
| `AppVisitor` | Python parse tree → `App` root |
| `PythonVisitor` | Python rules → `models.python.*` (770 lines) |
| `TemplateVisitor` | template root → `Template` |
| `NodeVisitor` | Jinja + HTML + CSS rules → nodes (1272 lines) |
| `SemanticAnalyzer` | walks the Python AST for 14 checks |
| `TemplateSemanticAnalyzer` | checks each template against the context its route supplies |
| `PythonDataExtractor` | walks the AST for render data |
| `JinjaRenderer` + `ExpressionEvaluator` | walk the AST to emit HTML |

The last three matter: **every consumer is a separate walk over the same tree.**
Analysis, extraction and rendering are independent passes, so none of them can
corrupt the AST for the others.

---

## 4. Symbol Table

`SymbolTable` → `Scope` (tree, each with a parent) → `Symbol`.

| Operation | Method |
| --- | --- |
| insert | `define(name, kind, type, value)` |
| lookup | `resolve(name)` (throws) / `lookup(name)` (returns null) / `isDefined(name)` |
| update | `update(name, kind, type, value)` — searches outward from the current scope |
| scope handling | `enterScope(name)` / `exitScope()` |
| inspection | `scopeCount()` / `symbolCount()` / `render()` |

`Scope.resolve` walks the parent chain, so an inner scope sees outer names but
not vice versa. `Scope.update` does the same walk, so assigning to a name
defined in an enclosing scope updates it there rather than shadowing it.

Output from the demo app (17 scopes, 11 symbols):

```
symbol         kind          type          value       scope
-------------- ------------- ------------- ----------- ----------------
title          block name    StringType    title       title block
content        block name    StringType    content     content block
product        id            IdType        product     for block at 10:4
```

**Generation never consults the symbol table.** `CodeGenerator`, `JinjaRenderer`,
`ExpressionEvaluator` and `PythonDataExtractor` hold no reference to one; they
resolve names against the extracted context instead, so checking and output stay
independent.

There are in fact **two** tables, and the one printed above is the template table:
`NodeVisitor` populates it while *constructing* the template AST, which is where
those `{% block %}` names and the `{% for %}` loop variable come from.
`SemanticAnalyzer` builds a second, separate table for the Python side. So the
table is not exclusive to semantic analysis — it is exclusive to the front end,
and generation is what stays clear of it.

---

## 5. Tree Printer

Two halves, as the requirement describes:

1. **A print method per node type** — every class overrides `print(int level)`,
   rendering its own fields and recursing into children with `level + 1`.
   Indentation comes from `Node.getIndent(level)`.
2. **A driver that walks the whole tree** — `TreePrinter` iterates the roots,
   frames each with `node.header()` (ID, type, line), and calls `print(0)`.

```java
TreePrinter.renderPythonAst(app, "app.py");   // Python AST
TreePrinter.renderTemplateAsts(templates);    // every template's AST
TreePrinter.renderSymbolTable(symbolTable);   // the table
```

Printed to the console during execution **and** written to
`compiler_output/ast_python.txt`, `ast_jinja.txt`, `symbol_table.txt`. Machine-
readable JSON (`ast_python.json`, `ast_jinja.json`) is emitted alongside.

Sample:

```
[1/10] #8 multi import line (line 2)
----------------------------------------------------------------
multi import
+- line no: 2
+- from name: name
``````+- line no: 2
``````\- id: flask
\- imported names: Flask, render_template, request, redirect, url_for
```

> The print methods draw the tree with the Unicode box characters `├ └ ─`. When
> the console runs a legacy code page that cannot encode them, they are
> transliterated to their ASCII equivalents (`+ \ -`) as shown above, rather
> than appearing as `??`. Files are always written as UTF-8 with the original
> characters.

---

## Demo App

[project/app.py](project/app.py) — a Flask product catalogue.
`Product = { id, image, name, price, details }`.

| Flow | Route | Template |
| --- | --- | --- |
| List all products | `/` | `index.jinja` |
| View product details | `/product/<int:product_id>` | `product_detail.jinja` |
| Add a product | `/add` | `add_product.jinja` |
| Edit a product | `/product/<int:product_id>/edit` | `edit_product.jinja` |
| Delete a product *(bonus)* | `/product/<int:product_id>/delete` | — |

This app's own source is what gets parsed: `app.py` through the Python
lexer/parser, `templates/*.jinja` through the template lexer/parser.

---

## Running it

```powershell
.\build.ps1
java -cp "out\classes;dependencies\antlr-4.13.2-complete.jar" app.FlaskCompiler
```

Prints the Python AST, every template AST, and the symbol table, then generates
the site into `output/`.

Every stage of the front-end can be printed during execution:

| Flag | Console output | Lines |
| --- | --- | ---: |
| *(none)* | AST + symbol table | ~970 |
| `--print-tokens` | also the lexer token streams | ~2,800 |
| `--print-parse-tree` | also the ANTLR parse trees | ~6,100 |
| `--print-all` | all four stages | ~8,000 |
| `--quiet-ast` | none | ~60 |

Regardless of the flags, every stage is written to `compiler_output/`:
`tokens.txt`, `parse_tree.txt`, `ast_python.txt`, `ast_jinja.txt`,
`symbol_table.txt`, plus the JSON dumps.

### The four stages, and how they differ

```
source ──▶ LEXER TOKENS ──▶ PARSE TREE ──▶ AST ──▶ SYMBOL TABLE
           flat token       one node per   model    names, kinds,
           stream           grammar rule   classes  scopes
```

**Tokens** are the flat stream, showing the synthetic INDENT/DEDENT the Python
lexer inserts and the mode switches the template lexer performs:

```
98     23:4       INDENT                     '    '   <-- synthetic
 4     3:0        J_STMNT_START              '{%'
```

**Parse tree** is the concrete syntax tree: every grammar rule that matched,
including punctuation the AST discards. It shows the derivation, e.g. how the
`'/'` in `@app.route('/')` descends the whole expression-precedence chain:

```
decorator   (line 25)
  '@'
  name   (line 25)
    id   (line 25)
      'app'
    dotTrailer   (line 25)
      '.'
      'route'
  callArgs   (line 25)
    callList → callArg → ternaryExpr → orExpr → andExpr
             → equalExpr → compareExpr → addExpr → mulExpr
             → singleExpr → value → baseValue → literal → string
                                                            ''/''
```

**AST** is the model tree — the same route, with the noise gone (see §5).

Open `output/index.html` for the working UI. Add / edit / delete persist through
`localStorage` — see [README.md](README.md).

## Verification

```powershell
.\check.ps1
```

- 7 checks — output layout, asset fidelity, valid JSON, DOCTYPE, no un-rendered Jinja
- 27 fixtures — every invalid backend caught **and** blocking generation
- 3 fixtures — valid backends **not** rejected (false-positive guard, `tests/valid/`)
- 2 projects — broken templates caught **and** blocking generation (`tests/bad_templates/`)
- 5 checks — Jinja control flow (`if` / `elif` / `else`, `for` / `for-else`)
- 24 checks — add / edit / delete driven through the real pages in jsdom

The false-positive group matters as much as the rest: a checker that rejects
valid programs is worse than one that misses a fault, because it blocks a build
that should have succeeded.
