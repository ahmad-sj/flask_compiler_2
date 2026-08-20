package models.jinja.expressions;

import models.Node;

public class DefaultExpression extends Expression {
    public Node expr;
    public Node defaultExpr;

    public DefaultExpression(Node expr, Node defaultExpr) {
        this.expr = expr;
        this.defaultExpr = defaultExpr;
    }

    @Override
    public String toString() {
        return "("
                + expr.toString()
                + " ?? "
                + defaultExpr.toString()
                + ")";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return "default expr\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "├─ main expr: " + expr.print(level + 2)
                + indent + "└─ default expr: " + expr.print(level + 2)
                ;
    }
}
