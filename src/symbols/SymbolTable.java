package symbols;

import models.Node;

public class SymbolTable {
    public Scope globalScope;
    public Scope currentScope;

    public SymbolTable() {
        globalScope = new Scope("global", null);
        currentScope = globalScope;
    }

    // Enter a new scope
    public void enterScope(String name) {
        currentScope = new Scope(name, currentScope);
    }

    // Exit the current scope
    public void exitScope() {
        if (currentScope.parent != null) {
            currentScope = currentScope.parent;
        }
    }

    // Define a symbol in the current scope
    public void define(String name, String kind, String type, Node value) {
        currentScope.define(name, kind, type, value);
    }

    // Resolve a symbol starting from the current scope
    public Symbol resolve(String name) {
        var symbol = currentScope.resolve(name);
        if (symbol == null) {
            throw new RuntimeException(
                    String.format("Symbol '%s' not found in current or parent scopes.", name));
        }
        return symbol;
    }

    /**
     * Non-throwing lookup: returns null when the name is not in scope.
     * resolve() throws, which suits a hard requirement but not the many callers
     * that simply need to ask whether a name exists.
     */
    public Symbol lookup(String name) {
        return currentScope.resolve(name);
    }

    /** True when the name is visible from the current scope. */
    public boolean isDefined(String name) {
        return lookup(name) != null;
    }

    /**
     * Updates an existing symbol, searching outward from the current scope.
     * Null kind/type/value leave that attribute unchanged.
     *
     * @return true if a symbol was found and updated, false if the name is unknown
     */
    public boolean update(String name, String kind, String type, Node value) {
        return currentScope.update(name, kind, type, value);
    }

    /** Number of scopes in the table, counting the global scope. */
    public int scopeCount() {
        return globalScope.countScopes();
    }

    /** Total symbols across every scope. */
    public int symbolCount() {
        return globalScope.countSymbols();
    }

    public void print() {
        System.out.print(render());
    }

    /**
     * The table as text, so it can be written to a file as well as printed.
     *
     * Scopes are rendered as a tree: each one carries its own column headings
     * and its nested scopes are indented beneath it.
     */
    public String render() {
        StringBuilder out = new StringBuilder();
        globalScope.render(out);
        return out.toString();
    }
}
