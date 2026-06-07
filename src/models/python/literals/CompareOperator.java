package models.python.literals;

import models.Node;

public class CompareOperator extends Node {
    String value;

    public CompareOperator(String value) {
        this.value = value;
        this.nodeName = value + " operator";
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String print(int level) {
        return value;
    }
}
