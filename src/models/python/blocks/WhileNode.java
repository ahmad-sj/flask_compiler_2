package models.python.blocks;


import models.Node;
import models.python.BlockNode;

public class WhileNode extends Node {

    private final Node condition;   // expr بعد WHILE
    private final BlockNode body;   // جسم الحلقة

    public WhileNode(int lineNumber, Node condition, BlockNode body) {
        this.nodeName = "While";
        this.lineNumber = lineNumber;
        this.condition = condition;
        this.body = body;
    }

    public Node getCondition() { return condition; }
    public BlockNode getBody() { return body; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("While (line=").append(lineNumber).append(")\n");
        if (condition != null) sb.append(condition.toString()).append("\n");
        if (body != null) sb.append(body.toString());
        return sb.toString();
    }
}
