package models.python.literals;

import models.Node;

import java.util.ArrayList;

public class Dict extends Node {
    public ArrayList<Node> itemList;

    public Dict(ArrayList<Node> itemList) {
        this.itemList = itemList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");

        if (itemList != null) {
            for (int i = 0; i < itemList.size(); i++) {
                sb.append(itemList.get(i));

                if (i + 1 < itemList.size())
                    sb.append(", ");
            }
        }
        sb.append("}");

        return sb.toString();
    }

    @Override
    public String print(int level) {
        StringBuilder sb = new StringBuilder();

        if (itemList != null) {
            for (int i = 0; i < itemList.size(); i++) {
                sb.append(itemList.get(i));

                if (i + 1 < itemList.size())
                    sb.append(", ");
            }
        }

        return sb.toString();
    }
}
