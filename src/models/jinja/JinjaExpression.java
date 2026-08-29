package models.jinja;

import models.Node;

public class JinjaExpression extends Node {
    public Node expression;

    public JinjaExpression(Node expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "{{ " + this.expression.toString() + " }}";
    }

    public String evaluate(){
        return "{{ " + this.expression.toString() + " }}";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level + 1);

        return header() + "\n" + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "└─ expression: " + this.expression.print(level + 2)
                ;
    }
}
