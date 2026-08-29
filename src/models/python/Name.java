package models.python;

import models.Node;

import java.util.ArrayList;

public class Name extends Node {
    public Node id; // of type IdType
    public ArrayList<Node> trailerList; // list of type IdType

    public Name(Node id, ArrayList<Node> trailerList) {
        this.id = id;
        this.trailerList = trailerList;
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder trailers = new StringBuilder();

        if (trailerList != null) {
            for (int i = 0; i < trailerList.size(); i++) {

                if (i + 1 < trailerList.size()) {
                    trailers.append(getIndent(level + 1));
                    trailers.append("├─ ").append(trailerList.get(i).print(level + 2));
                    trailers.append("\n");
                } else {
                    trailers.append(getIndent(level + 1));
                    trailers.append("└─ ").append(trailerList.get(i).print(level + 2));
                }
            }
        }

        return header() + "\n" +
                indent + "├─ line no: " + lineNumber + "\n" +
                (trailers.isEmpty()
                        ? indent + "└─ id: " + id.print(level + 2)
                        : indent + "├─ id : " + id.print(level + 2)
                        + indent + "└─ trailers:\n" + trailers
                );
    }

}
