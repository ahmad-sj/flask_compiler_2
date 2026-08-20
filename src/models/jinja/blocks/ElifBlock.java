package models.jinja.blocks;

import models.Node;

public class ElifBlock extends JinjaBlock {
    public Node condition;
    public Node nodeBody; // object of type NodeBody

    public ElifBlock(Node condition, Node nodeBody) {
        this.condition = condition;
        this.nodeBody = nodeBody;
    }

    @Override
    public String toString() {
        return "{% elif " + this.condition.toString() + " %}\n"
                + (nodeBody == null ? "" : nodeBody.toString());
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return "elif block\n" +
                indent + "├─ line no: " + lineNumber + "\n" +
                (nodeBody == null
                        ? indent + "└─ condition: " + condition.toString() + "\n"
                        : indent + "├─ condition: " + condition.toString() + "\n"
                        + indent + "└─ elif body:\n" + nodeBody.print(level + 1)
                );
    }
}
