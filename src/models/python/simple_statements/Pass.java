package models.python.simple_statements;

import models.Node;

public class Pass extends Node {
    @Override
    public String toString() {
        return "pass\n";
    }

    @Override
    public String print(int level) {
        return "pass\n"
                + getIndent(level) + "└─ line no: " + lineNumber + "\n"
                ;
    }
}
