package models.python;

import models.Node;

import java.util.ArrayList;

public class Value extends Node {
    Node baseValue;
    ArrayList<Node> trailerList;

    public Value(Node baseValue, ArrayList<Node> trailerList) {
        this.baseValue = baseValue;
        this.trailerList = trailerList;
    }

    @Override
    public String toString() {
        StringBuilder trailers = new StringBuilder();

        if (trailerList != null) {
            for (int i = 0; i < trailerList.size(); i++) {
                trailers.append(trailerList.get(i));
            }
        }

        return baseValue.toString() + trailers.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);
        StringBuilder sb = new StringBuilder();


        if (trailerList == null) {
            sb.append("value:\n")
                    .append(indent).append("├─ line no: ").append(lineNumber).append("\n")
                    .append(indent).append("└─ base value: ").append(baseValue.print(level + 1))

            ;
        }


        if (trailerList != null) {
            StringBuilder trailers = new StringBuilder();

            for (int i = 0; i < trailerList.size(); i++) {
                trailers.append(trailerList.get(i));
            }

            sb.append("value:\n");
            sb.append(indent).append("├─ line no: ").append(lineNumber).append("\n");
            sb.append(indent).append("├─ base value: ").append(baseValue.print(level + 1));
            sb.append(indent).append("└─ trailers: ").append(trailers).append("\n");
        }

        return sb.toString();
    }
}
