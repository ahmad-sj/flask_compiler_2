package models.python.simple_statements;

import models.Node;

public class TernaryExpr extends Node {
    public Node trueExpr;
    public Node condition;
    public Node falseExpr;

    public TernaryExpr(Node trueExpr, Node condition, Node falseExpr) {
        this.trueExpr = trueExpr;
        this.condition = condition;
        this.falseExpr = falseExpr;
    }

    @Override
    public String toString() {
        return trueExpr.toString() + " if " + condition.toString() + " else " + falseExpr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);
        StringBuilder sb = new StringBuilder();

        sb.append("ternaryExpr:\n")
                .append(indent).append("├─ trueExpr: ").append(trueExpr.print(level + 1))
                .append(indent).append("├─ condition: ").append(condition.print(level + 1))
                .append(indent).append("└─ falseExpr: ").append(falseExpr.print(level + 1))
        ;

        return sb.toString();
    }
}
