package models.python;

import models.Node;

import java.util.List;

public class BlockNode extends Node {

    public List<Node> statements;

    public BlockNode(List<Node> statements) {
        this.statements = statements;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < statements.size(); i++) {
            sb.append(statements.get(i).toString());

            if (i + 1 < statements.size())
                sb.append("\n");
        }

        return this.nodeName + ":\n"
                + "line no: " + this.lineNumber + "\n"
                + "statements list:\n" + sb.toString()
                ;
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level + 1);

        StringBuilder sb = new StringBuilder();

        sb.append(this.nodeName).append("\n");

        for (int i = 0; i < statements.size(); i++) {

            if (i + 1 < statements.size()) {
                sb.append(indent).append("├─ ").append("stmt").append(i).append(": ");
                sb.append(statements.get(i).print(level + 2));
//                sb.append("\n");
            } else {
                sb.append(indent).append("└─ ").append("stmt").append(i).append(": ");
                sb.append(statements.get(i).print(level + 2));
            }
        }
        return sb.toString();
    }
}
