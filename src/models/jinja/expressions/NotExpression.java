package models.jinja.expressions;

import models.Node;

public class NotExpression extends Expression {
    public Node expression;

    public NotExpression(Node expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return this.expression.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return "not expr\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "└─ expr: " + expression.print(level + 2)
                ;
    }
}
