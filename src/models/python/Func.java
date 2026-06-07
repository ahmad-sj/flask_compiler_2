package models.python;

import models.Node;

import java.util.ArrayList;

public class Func extends Node {

    public Node decorator;
    public Node funcName;
    public ArrayList<Node> funcArgs;
    public Node funcBlock;

    public Func(Node decorator, Node funcName, ArrayList<Node> funcArgs, Node funcBlock) {
        this.decorator = decorator;
        this.funcName = funcName;
        this.funcArgs = funcArgs;
        this.funcBlock = funcBlock;
    }

    @Override
    public String toString() {
        return "- function" +
                "\nfunc name: " + funcName.toString() + "\n" +
                (decorator != null ? "func decorator: " + decorator.toString() : "") + "\n" +
                (funcArgs != null ? "func args: " + funcArgs : "") + "\n" +
                (funcBlock != null ? "``````` func body start ```````\n" + funcBlock.toString() + "\n``````` func body end ```````" : "")
                ;
    }

    @Override
    public String print(int level) {

        String indent = getIndent(level);

        StringBuilder sb = new StringBuilder();

        sb.append("function:")
                .append("\n").append(indent).append("├─ line no: ").append(this.lineNumber)
                .append("\n").append(indent).append("├─ name: ").append(funcName.toString())
        ;

        if (decorator != null) {
            sb.append("\n").append(indent).append("├─ ").append(decorator.print(level + 1));
        }

        if (funcArgs != null) {
            StringBuilder args = new StringBuilder();

            for (int i = 0; i < funcArgs.size(); i++) {
                args.append(funcArgs.get(i));

                if (i + 1 < funcArgs.size())
                    args.append(", ");
            }

            sb.append("\n").append(indent).append("├─ args: ").append(args);
        }

        if (funcBlock != null) {
            sb.append("\n").append(indent).append("├─ func body: ").append(funcBlock.print(level));
        }

        sb.append(indent).append("└─ ");
        return sb.toString();
    }
}
