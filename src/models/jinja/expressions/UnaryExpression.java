package models.jinja.expressions;

import models.Node;

public class UnaryExpression extends Expression {
    public Node unaryOperator;
    public Node expr;

    public UnaryExpression(Node unaryOperator, Node expr) {
        this.unaryOperator = unaryOperator;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return unaryOperator.toString() + expr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "├─ sign: " + unaryOperator.print(level)
                + indent + "└─ expr: " + expr.print(level + 2)
                ;
    }
}
