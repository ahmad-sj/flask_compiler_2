package models.html.attributes;

import models.Node;

import java.util.ArrayList;

public class StyleAttribute extends Attribute {
    public ArrayList<Node> propList;

    public StyleAttribute(String name, ArrayList<Node> propList) {
        this.attrName = name;
        this.propList = propList;
    }

    @Override
    public String toString() {
        StringBuilder properties = new StringBuilder();

        if (this.propList != null) {

            for (int i = 0; i < this.propList.size(); i++) {
                properties.append(this.propList.get(i));

                if (i + 1 < this.propList.size())
                    properties.append(" ");
            }
        }


        return "style=" + '"' + properties + '"';
    }
}
