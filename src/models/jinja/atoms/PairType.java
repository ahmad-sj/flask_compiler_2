package models.jinja.atoms;

import models.Node;

public class PairType extends Atom {
    public Node expr1;
    public Node expr2;

    public PairType(Node expr1, Node expr2) {
        this.expr1 = expr1;
        this.expr2 = expr2;
    }

    @Override
    public String toString() {
        return expr1.toString() + " : " + expr2.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "├─ expr1: " + expr1.print(level + 2) + "\n"
                + indent + "└─ expr2: " + expr1.print(level + 2) + "\n"
                ;
    }
}
