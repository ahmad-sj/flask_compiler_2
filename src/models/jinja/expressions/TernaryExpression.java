package models.jinja.expressions;

import models.Node;

public class TernaryExpression extends Expression {
    public Node condExpr;
    public Node trueExpr;
    public Node falseExpr;

    public TernaryExpression(Node condExpr, Node trueExpr, Node falseExpr) {
        this.condExpr = condExpr;
        this.trueExpr = trueExpr;
        this.falseExpr = falseExpr;
    }

    @Override
    public String toString() {
        return "("
                + condExpr.toString()
                + " ? "
                + trueExpr.toString()
                + " : "
                + falseExpr.toString()
                + ")";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "├─ cond expr: " + condExpr.print(level + 2)
                + indent + "├─ true expr: " + trueExpr.print(level + 2)
                + indent + "└─ false expr: " + falseExpr.print(level + 2)
                ;
    }
}
