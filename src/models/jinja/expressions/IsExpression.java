package models.jinja.expressions;

import models.Node;

public class IsExpression extends Expression {
    public Node expr;
    public boolean negated;
    public Node id;

    public IsExpression(Node expr, boolean negated, Node id) {
        this.expr = expr;
        this.negated = negated;
        this.id = id;
    }

    @Override
    public String toString() {
        return expr.toString() + " is " + (negated ? "not " : "") + id.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "├─ expr: " + expr.print(level + 2)
                + indent + "├─ optor: is" + (negated ? " not" : "") + "\n"
                + indent + "└─ id: " + id.print(level)
                ;
    }
}
