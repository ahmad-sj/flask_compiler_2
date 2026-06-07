package models.python.literals;

import models.Node;

public class DictItem extends Node {
    Node literal;
    Node expr;

    public DictItem(Node literal, Node expr) {
        this.literal = literal;
        this.expr = expr;
    }

    @Override
    public String toString() {
        return literal.toString() + " : " + expr.toString();
    }

    @Override
    public String print(int level) {
        return literal.toString() + " : " + expr.toString();
    }
}
