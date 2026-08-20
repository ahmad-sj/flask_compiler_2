package symbols;

import models.Node;

import java.util.*;

public class Scope {
    public String name; // e.g., "MyNamespace", "MyClass", "MyMethod"
    public Scope parent; // Parent scope (null for global)
    public HashMap<String, Symbol> symbols; // symbols in this scope
    public List<Scope> children; // Nested scopes (e.g., blocks inside a method)

    public Scope(String name, Scope parent) {
        this.name = name;
        this.parent = parent;
        this.symbols = new HashMap<String, Symbol>();
        this.children = new ArrayList<>();
        if (parent != null) {
            parent.children.add(this);
        }
    }

    public void define(String name, String kind, String type, Node value) {
        Symbol symbol = new Symbol(name, kind, type, value, this);

        if (symbols.containsKey(name)) {
            throw new RuntimeException(
                    String.format("Symbol '%s' already defined in scope '%s'.", name, this.name));
        }

        symbols.put(name, symbol);
    }

    public Symbol resolve(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        }

        // Walk up the scope chain
        if (parent != null) {
            return parent.resolve(name);
        }

        return null; // Not found in this scope or any parent
    }

    /**
     * Updates a symbol in this scope, or the nearest enclosing one that has it.
     * Null attributes are left unchanged.
     *
     * @return true if the symbol was found and updated
     */
    public boolean update(String name, String kind, String type, Node value) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            if (kind != null)  symbol.kind = kind;
            if (type != null)  symbol.type = type;
            if (value != null) symbol.value = value;
            return true;
        }
        return parent != null && parent.update(name, kind, type, value);
    }

    /** This scope plus all nested scopes. */
    public int countScopes() {
        int total = 1;
        for (Scope child : children) total += child.countScopes();
        return total;
    }

    /** Symbols in this scope plus all nested scopes. */
    public int countSymbols() {
        int total = symbols.size();
        for (Scope child : children) total += child.countSymbols();
        return total;
    }

    public void print() {
        StringBuilder out = new StringBuilder();
        render(out);
        System.out.print(out);
    }

    /** Appends this scope's symbols, then its children's, to out. */
    public void render(StringBuilder out) {
        for (Map.Entry<String, Symbol> entry : symbols.entrySet()) {
            out.append(entry.getValue()).append(System.lineSeparator());
        }

        for (Scope child : children) {
            child.render(out);
        }
    }
}
