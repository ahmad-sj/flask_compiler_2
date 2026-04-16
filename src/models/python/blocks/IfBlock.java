package models.python.blocks;

import models.Node;

import java.util.ArrayList;

public class IfBlock extends ElifBlock {
    ArrayList<Node> elifBlockList;
    Node elseBlock;

    public IfBlock(Node condition, Node body) {
        super(condition, body);
        this.nodeName = "if block";
        elifBlockList = null;
        elseBlock = null;
    }

    public void setElifBlockList(ArrayList<Node> elifBlockList) {
        this.elifBlockList = elifBlockList;
    }

    public void setElseBlock(Node elseBlock) {
        this.elseBlock = elseBlock;
    }
}
