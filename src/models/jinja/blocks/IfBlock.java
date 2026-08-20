package models.jinja.blocks;

import models.Node;

public class IfBlock extends JinjaBlock {
    public Node condition;
    public Node nodeBody; // object of type NodeBody

    public IfBlock(Node condition, Node nodeBody) {
        this.condition = condition;
        this.nodeBody = nodeBody;
    }

    @Override
    public String toString() {
        return "{% if " + this.condition.toString() + " %}\n"
                + (nodeBody == null ? "" : nodeBody.toString() + "\n")
                + "{% endif %}";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return "if block\n" +
                indent + "├─ line no: " + lineNumber + "\n" +
                (nodeBody == null
                        ? indent + "└─ condition: " + condition.toString() + "\n"
                        : indent + "├─ condition: " + condition.toString() + "\n"
                        + indent + "└─ if body:\n" + nodeBody.print(level + 1)
                );
    }
}
