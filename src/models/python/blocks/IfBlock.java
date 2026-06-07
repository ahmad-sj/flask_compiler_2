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

    @Override
    public String print(int level) {
        String indent = getIndent(level);
        StringBuilder sb = new StringBuilder();

        sb.append(this.nodeName).append("\n")
                .append(indent).append("├─ line no: ").append(this.lineNumber).append("\n")
                .append(indent).append("├─ if condition: ").append(this.condition.print(level + 1))
                .append(indent).append("├─ ").append(this.body.print(level + 1));

        if (elifBlockList != null) {
            for (int i = 0; i < elifBlockList.size(); i++) {
                sb.append(elifBlockList.get(i).print(level + 1));

//                if (i + 1 < elifBlockList.size()) {
//                    sb.append("\n");
//                }
            }
        }

        if (elseBlock != null) {
            sb.append(elseBlock.print(level + 1));
        }

        return sb.toString();
    }
}
