package models.python.simple_statements;

import models.Node;

public class ReturnLine extends Node {

    public Node returnExpr;

    public ReturnLine(Node returnExpr) {
        this.returnExpr = returnExpr;
    }

    @Override
    public String toString() {
        return "- return line" +
                "\nline no: " + lineNumber +
                (returnExpr == null
                        ? ""
                        : "\nreturn expr: " + returnExpr.toString()
                );
    }

    @Override
    public String print(int level) {

        String indent = getIndent(level);

        return "return line\n" +
                (returnExpr == null
                        ? indent + "└─ line no: " + lineNumber + "\n"
                        : indent + "├─ line no: " + lineNumber + "\n"
                        + indent + "└─ return expr: " + returnExpr.print(level + 1)
                );

    }
}
