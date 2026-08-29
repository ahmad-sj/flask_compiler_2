package symbols;

import models.Node;

import java.util.HashMap;

public class Symbol {
    public String name;
    public String kind; // "variable", "method", "class", "namespace"
    public String type; // e.g., "int", "MyClass"
    public Node value;
    public Scope scope; // The scope this symbol belongs to
    public HashMap<String, Object> attributes; // Extra info (e.g., method parameters)

    public Symbol(String name, String kind, String type, Node value, Scope scope) {
        this.name = name;
        this.kind = kind;
        this.type = type;
        this.scope = scope;
        this.value = value;
        this.attributes = new HashMap<String, Object>();
    }

    /**
     * One formatted table row.
     *
     * This used to print to stdout and return "", so the symbol table only
     * appeared by side effect and could not be captured into a file. It also
     * threw when a symbol had no value.
     */
    @Override
    public String toString() {
        return String.format("%-30s%-30s%-30s%-40s%-30s",
                text(name), text(kind), text(type), describeValue(), scope == null ? "" : text(scope.name));
    }

    /**
     * One row without the scope column, for the nested render.
     *
     * Scope.render() prints each scope under its own heading, so repeating the
     * scope name on every row inside it is redundant.
     */
    public String row() {
        return String.format("%-26s%-16s%-18s%s",
                text(name), text(kind), text(type), describeValue());
    }

    /** Column headings matching row(). */
    public static String rowHeader() {
        return String.format("%-26s%-16s%-18s%s", "symbol", "kind", "type", "value");
    }

    /**
     * Values can be large trees, so show a single trimmed line.
     *
     * A node's toString() walks the subtree, and a malformed one can throw - a
     * missing visitor override putting a null into an operator list was enough
     * to do it. Printing a diagnostic must never bring down the compiler, so a
     * failure here degrades to a marker instead of propagating.
     */
    private String describeValue() {
        if (value == null) return "-";
        String rendered;
        try {
            rendered = value.toString().replaceAll("\\s+", " ").trim();
        } catch (RuntimeException e) {
            return "<unprintable " + value.getClass().getSimpleName() + ">";
        }
        return rendered.length() > 38 ? rendered.substring(0, 35) + "..." : rendered;
    }

    private static String text(String s) {
        return s == null ? "" : s;
    }
}
