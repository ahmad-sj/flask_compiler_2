package models.python.blocks;

import models.Node;

public class ElifBlock extends Node {
    public Node condition;
    public Node body;

    public ElifBlock(Node condition, Node body) {
        this.condition = condition;
        this.body = body;
        this.nodeName = "elif block";
    }
}
