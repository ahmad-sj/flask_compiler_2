package models.jinja.expressions;

import models.Node;

public class ComparisonExpression extends Expression {
    public Node expr1;
    public Node expr2;
    public Node compOptor;

    public ComparisonExpression(Node expr1, Node expr2, Node compOptor) {
        this.expr1 = expr1;
        this.expr2 = expr2;
        this.compOptor = compOptor;
    }

    @Override
    public String toString() {
        return expr1.toString()
                + " " + compOptor.toString() + " "
                + expr2.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "├─ expr1: " + expr1.print(level + 2)
                + indent + "├─ optor: " + compOptor.print(level)
                + indent + "└─ expr2: " + expr2.print(level + 2)
                ;
    }
}
