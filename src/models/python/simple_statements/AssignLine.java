package models.python.simple_statements;

import models.Node;

public class AssignLine extends Node {
    public Node target;
    public Node expr;

    public AssignLine(Node target, Node expr) {
        this.target = target;
        this.expr = expr;
    }

    @Override
    public String print(int level) {

        String indent = getIndent(level);

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "├─ target: " + target.print(level + 2)
                + indent + "└─ expr: " + expr.print(level + 2)
                ;

    }

    @Override
    public String toString() {
        return "- assign line" +
                "\nline no: " + lineNumber +
                "\ntarget: " + target.toString() +
                "\nassigned value: " + expr.toString();
    }
}
