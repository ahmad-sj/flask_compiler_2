package models.jinja.atoms;

import models.Node;

import java.util.ArrayList;

public class ListType extends Atom {
    public ArrayList<Node> itemList;

    public ListType(ArrayList<Node> itemList) {
        this.itemList = itemList;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        if (itemList != null) {
            for (int i = 0; i < itemList.size(); i++) {
                stringBuilder.append(itemList.get(i).toString());

                if (i + 1 < itemList.size())
                    stringBuilder.append(", ");
            }
        }

        return "[" + stringBuilder + "]";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder items = new StringBuilder();

        if (itemList != null) {
            for (int i = 0; i < itemList.size(); i++) {
                if (i + 1 < itemList.size()) {
                    items.append(getIndent(level + 1)).append("├─ ");
                    items.append(itemList.get(i).print(level + 2));
                    items.append("\n");
                } else {
                    items.append(getIndent(level + 1)).append("└─ ");
                    items.append(itemList.get(i).print(level + 2));
                }
            }
        }

        return header() + "\n" +
                (itemList == null
                        ? indent + "└─ line no: " + lineNumber + "\n"
                        : indent + "├─ line no: " + lineNumber + "\n"
                        + indent + "└─ expr list:\n" + items + "\n"
                );
    }
}
