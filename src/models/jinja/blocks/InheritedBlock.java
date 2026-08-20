package models.jinja.blocks;

import models.Node;

public class InheritedBlock extends JinjaBlock {
    public String blockName;
    public Node nodeBody; // object of type NodeBody

    public InheritedBlock(String blockName, Node nodeBody) {
        this.blockName = blockName;
        this.nodeBody = nodeBody;
    }

    @Override
    public String toString() {
        return "{% block " + blockName + " %}\n"
                + (this.nodeBody == null ? "" : nodeBody.toString())
                + "\n{% endblock %}";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level + 1);

        return "block" + "\n"
                + indent + "├─ line no: " + lineNumber + "\n" +
                (nodeBody == null
                        ? indent + "└─ block name: " + blockName + "\n"
                        : indent + "├─ block name: " + blockName + "\n"
                        + indent + "└─ children: " + "\n"
                        + this.nodeBody.print(level + 2)
                );
    }
}
