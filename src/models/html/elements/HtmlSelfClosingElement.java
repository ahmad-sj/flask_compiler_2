package models.html.elements;

import models.Node;
import models.html.attributes.Attribute;

import java.util.ArrayList;

public class HtmlSelfClosingElement extends HtmlElement {

    public HtmlSelfClosingElement(String tagName, ArrayList<Node> attrList) {
        super(tagName, attrList);
    }

    @Override
    public String toString() {
        StringBuilder attributes = new StringBuilder();

        if (attrList != null) {
            for (int i = 0; i < attrList.size(); i++) {
                attributes.append(attrList.get(i).toString());

                if (i + 1 < attrList.size())
                    attributes.append(" ");
            }
        }

        return "<" + tagName + (attributes.isEmpty() ? "" : " " + attributes) + "/>";
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level + 1);

        StringBuilder attributes = new StringBuilder();

        if (attrList != null) {
            for (int i = 0; i < attrList.size(); i++) {
                attributes.append(attrList.get(i).toString());

                if (i + 1 < attrList.size())
                    attributes.append(" ");
            }
        }

        return header() + "\n"
                + (attributes.isEmpty() ?
                indent + "└─ line no: " + lineNumber + " \n" :
                indent + "├─ line no: " + lineNumber + " \n" +
                indent + "└─ attributes: " + attributes + "\n")
                ;
    }
}
