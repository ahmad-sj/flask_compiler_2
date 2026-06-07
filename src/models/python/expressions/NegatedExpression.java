package models.python.expressions;

import models.Node;

public class NegatedExpression extends Node {
    Node expr;

    public NegatedExpression(Node expr) {
        this.expr = expr;
        this.nodeName = "negated expr";
    }

    @Override
    public String toString() {
        return "not" + expr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder sb = new StringBuilder();

        sb.append(this.nodeName).append("\n")
                .append(indent).append("├─ ").append("line no: ").append(this.lineNumber).append("\n")
                .append(indent).append("└─ ").append("expr: ").append(this.expr.print(level + 1))
                ;

        return sb.toString();
    }
}
