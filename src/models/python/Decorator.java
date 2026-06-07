package models.python;

import models.Node;

import java.util.List;

public class Decorator extends Node {

    public Node name;
    public List<Node> callArgs;

    public Decorator(Node name, List<Node> callArgs) {
        this.nodeName = "decorator line";
        this.name = name;
        this.callArgs = callArgs;
    }

    @Override
    public String toString() {
        StringBuilder args = new StringBuilder();

        if (callArgs != null) {
            for (int i = 0; i < callArgs.size(); i++) {
                args.append(callArgs.get(i).toString());

                if (i + 1 < callArgs.size())
                    args.append(", ");
            }
        }

        return this.nodeName + ":"
                + "\nline no: " + lineNumber
                + "\nname: " + name.toString() +
                (callArgs == null
                        ? "\n"
                        : "\nargs: " + args + "\n"
                );
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder args = new StringBuilder();

        if (callArgs != null) {
            for (int i = 0; i < callArgs.size(); i++) {
                args.append(callArgs.get(i).toString());

                if (i + 1 < callArgs.size())
                    args.append(", ");
            }
        }

        return this.nodeName + ":\n"
                + (callArgs == null ?
                (
                        indent + "├─ line no: " + lineNumber + "\n"
                                + indent + "└─ " + name.print(level + 1)
                ) :
                (
                        indent + "├─ line no: " + lineNumber + "\n"
                                + indent + "├─ " + name.print(level + 1)
                                + indent + "└─ args: " + args.toString()
                )
        );
    }
}
