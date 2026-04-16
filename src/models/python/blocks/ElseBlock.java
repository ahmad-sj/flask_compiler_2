package models.python.blocks;


import models.Node;
import models.python.BlockNode;

import java.util.List;

public class ElseBlock extends BlockNode {

    public ElseBlock(List<Node> statements) {
        super(statements);
        setNodeName("else block");
    }
}
