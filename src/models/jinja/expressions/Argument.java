package models.jinja.expressions;

import models.Node;

public class Argument extends Expression {
    public Node expr;
    public Node argName;

    public Argument(Node expr, Node argName) {
        this.expr = expr;
        this.argName = argName;
    }

    @Override
    public String toString() {
        return (argName == null ? "" : argName.toString() + " = ") + expr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return header() + "\n" +
                (argName == null
                        ? indent + "└─ arg expr: " + expr.print(level + 1) + "\n"
                        : indent + "├─ arg expr: " + expr.print(level + 1) + "\n"
                        + indent + "└─ argName: " + argName.print(level) + "\n"
                );
    }
}
