package models.jinja.expressions;

import models.Node;

public class PowerExpression extends Expression {
    public Node baseValueExpr;
    public Node powerValueExpr;

    public PowerExpression(Node baseValueExpr, Node powerValueExpr) {
        this.baseValueExpr = baseValueExpr;
        this.powerValueExpr = powerValueExpr;
    }

    @Override
    public String toString() {
        return baseValueExpr.toString() + "**" + powerValueExpr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "├─ base expr: " + baseValueExpr.print(level + 2)
                + indent + "├─ optor: **\n"
                + indent + "└─ pow expr: " + powerValueExpr.print(level + 2)
                ;
    }
}
