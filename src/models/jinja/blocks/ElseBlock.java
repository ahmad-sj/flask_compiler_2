package models.jinja.blocks;

import models.Node;

public class ElseBlock extends JinjaBlock {
    public Node nodeBody; // object of type NodeBody

    public ElseBlock(Node nodeBody) {
        this.nodeBody = nodeBody;
    }

    @Override
    public String toString() {
        if (this.nodeBody != null)
            return "{% else %}\n" + nodeBody.toString();
        else
            return "{% else %}\n";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return "else block\n" +
                (nodeBody == null
                        ? indent + "└─ line no " + lineNumber + "\n"
                        : indent + "├─ line no " + lineNumber + "\n"
                        + indent + "└─ else body:\n" + nodeBody.print(level + 1)
                );
    }
}
