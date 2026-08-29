package models.css;

import models.Node;

import java.util.ArrayList;

public class CssBlock extends Node {
    Node selectorList;
    ArrayList<Node> propertiesList;

    public CssBlock(Node selectorList, ArrayList<Node> propertiesList) {
        this.selectorList = selectorList;
        this.propertiesList = propertiesList;
    }

    @Override
    public String toString() {
        StringBuilder propList = new StringBuilder();

        if (propertiesList != null) {
            for (int i = 0; i < propertiesList.size(); i++) {
                propList.append(propertiesList.get(i).toString());

                if (i + 1 < propertiesList.size())
                    propList.append("\n");
            }
        }

        return selectorList.toString() + " {\n"
                + propList.toString()
                + "\n}";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level + 1);

        StringBuilder propList = new StringBuilder();

        if (propertiesList != null) {
            for (int i = 0; i < propertiesList.size(); i++) {
                propList.append(propertiesList.get(i).toString());

                if (i + 1 < propertiesList.size())
                    propList.append(" ");
            }
        }

        return header() + "\n" +
                indent + "├─ line no: " + lineNumber + "\n" +
                (propList.isEmpty()
                        ? indent + "└─ selectors: " + selectorList.toString() + "\n"
                        : indent + "├─ selectors: " + selectorList.toString() + "\n"
                        + indent + "└─ props: " + propList.toString() + "\n"
                );
    }
}
