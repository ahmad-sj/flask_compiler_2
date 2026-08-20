# Compilers Project — Flask + Jinja2 + HTML + CSS Parser

Build a custom compiler front-end (Lexer → Parser → AST → Symbol Table) for a language combining Python, Jinja2, HTML, and CSS syntax, then prove it works by running it against a real Flask demo app.

**Stack:** Python, Flask, Jinja2, HTML, CSS. Lexer/Parser can be hand-written or grammar-generator-based (e.g. ANTLR) — either is fine as long as the grammar/tokens are explicit. AST, Visitor, and Symbol Table must be custom, OOP-based.

## 1. Grammar, Lexer & Parser
- Define grammar/token rules for: Python syntax, Jinja2 syntax (`{{ }}`, `{% %}`, filters, control blocks), HTML, and CSS.
- Implement a Lexer and Parser from this grammar.

## 2. Abstract Syntax Tree (AST)
- OOP design: a base `Node` class with subclasses per node type — use inheritance and polymorphism.
- Every node must store: node name/type, node ID (number), and source line number.
- Print the tree during execution; it must remain correctly stored after execution finishes.

## 3. Visitor
- Implement the Visitor pattern to traverse the parsed input and populate the AST.

## 4. Symbol Table
- Proper data structure with helper methods: insert, lookup, update, scope handling, etc.

## 5. Tree Printer
- A print method per node type.
- A driver function that walks the full tree calling them, producing readable output — each node's info plus its children.

## Demo App (parser test input)
Small Flask + Jinja2 + HTML/CSS product-catalog app:
1. List all products
2. Add a product
3. View product details
4. *(Bonus, optional)* Delete a product

Product = `{ image, name, price, details }`

This app's own source is what gets run through the Lexer/Parser to generate and print its AST + symbol table.

## Deliverables
- Full source: lexer, parser, AST classes, visitor, symbol table, tree printer
- The demo Flask app above (3 required flows + optional delete)
- Short report explaining each part, with a diagram of the AST structure
- Must be demo-ready: working UI, AST + symbol table printed output, and a clear walkthrough of the grammar/tree design on request
