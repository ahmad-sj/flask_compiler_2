package models.jinja.atoms;

import models.Node;

import java.util.ArrayList;

public class DictType extends Atom {
    public ArrayList<Node> pairsList;

    public DictType(ArrayList<Node> pairList) {
        this.pairsList = pairList;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        if (pairsList != null) {
            for (int i = 0; i < pairsList.size(); i++) {
                stringBuilder.append(pairsList.get(i).toString());

                if (i + 1 < pairsList.size())
                    stringBuilder.append(", ");
            }
        }

        return "{" + stringBuilder + "}";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);
        StringBuilder pairs = new StringBuilder();

        if (pairsList != null) {
            for (int i = 0; i < pairsList.size(); i++) {
                if (i + 1 < pairsList.size()) {
                    pairs.append(getIndent(level + 1)).append("├─ ");
                    pairs.append(pairsList.get(i).print(level + 2));
                    pairs.append("\n");
                } else {
                    pairs.append(getIndent(level + 1)).append("└─ ");
                    pairs.append(pairsList.get(i).print(level + 2));
                }
            }
        }

        return "dict type:\n" +
                (pairsList == null
                        ? indent + "└─ line no: " + lineNumber + "\n"
                        : indent + "├─ line no: " + lineNumber + "\n"
                        + indent + "└─ pair list:\n" + pairs + "\n"
                );
    }
}
