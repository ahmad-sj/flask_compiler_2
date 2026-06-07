package models.python.blocks;


import models.Node;
import models.python.BlockNode;

public class ForNode extends Node {

    private final String iterator;  // الاسم بعد FOR
    private final Node iterable;    // expr بعد IN
    private final BlockNode body;   // جسم الحلقة

    public ForNode(int lineNumber, String iterator, Node iterable, BlockNode body) {
        this.nodeName = "For";
        this.lineNumber = lineNumber;
        this.iterator = iterator;
        this.iterable = iterable;
        this.body = body;
    }

    public String getIterator() { return iterator; }
    public Node getIterable() { return iterable; }
    public BlockNode getBody() { return body; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("For (line=").append(lineNumber)
                .append(", iterator=").append(iterator).append(")\n");
        if (iterable != null) sb.append(iterable.toString()).append("\n");
        if (body != null) sb.append(body.toString());
        return sb.toString();
    }
}
