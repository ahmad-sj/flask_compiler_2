package models.jinja.expressions;

import models.Node;

public class ParenthedExpression extends Expression {
    public Node expr;

    public ParenthedExpression(Node expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "(" + expr.toString() + ")";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "└─ expr: " + expr.print(level + 2)
                ;
    }
}
