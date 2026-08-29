package models.jinja.blocks;

import models.Node;

import java.util.ArrayList;

public class ForBlock extends JinjaBlock {
    public ArrayList<Node> loopVars;
    public Node iterable;
    public Node nodeBody; // object of type NodeBody

    public ForBlock(ArrayList<Node> loopVars, Node iterable, Node nodeBody) {
        this.loopVars = loopVars;
        this.iterable = iterable;
        this.nodeBody = nodeBody;
    }

    @Override
    public String toString() {
        StringBuilder loopVars = new StringBuilder();

        for (int i = 0; i < this.loopVars.size(); i++) {
            loopVars.append(this.loopVars.get(i));

            if (i + 1 < this.loopVars.size())
                loopVars.append(", ");
        }

        return "{% for " + loopVars + " in " + iterable.toString() + " %}\n"
                + (nodeBody == null ? "" : nodeBody.toString())
                + "\n{% endfor %}";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level + 1);

        StringBuilder loopVars = new StringBuilder();

        for (int i = 0; i < this.loopVars.size(); i++) {
            loopVars.append(this.loopVars.get(i));

            if (i + 1 < this.loopVars.size())
                loopVars.append(", ");
        }

        return header() + "\n" +
                (nodeBody == null
                        ? indent + "├─ line no: " + lineNumber + "\n"
                        + indent + "└─ loop vars: " + loopVars.toString() + "\n"
                        : indent + "├─ line no: " + lineNumber + "\n"
                        + indent + "├─ loop vars: " + loopVars.toString() + "\n"
                        + indent + "└─ for body:\n" + nodeBody.print(level + 2)
                );
    }
}
