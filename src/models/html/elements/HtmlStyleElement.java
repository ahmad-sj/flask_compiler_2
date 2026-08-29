package models.html.elements;

import models.Node;

import java.util.ArrayList;

public class HtmlStyleElement extends HtmlElement {
    // public, matching HtmlRegularElement: the renderer walks this body rather
    // than falling back to toString(), which re-emitted the surrounding tags.
    public Node elementBody; // object of type HtmlElementBody

    public HtmlStyleElement(String tagName, ArrayList<Node> attrList, Node elementBody) {
        super(tagName, attrList);
        this.elementBody = elementBody;
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

        return "<" + tagName + (attributes.isEmpty() ? "" : " " + attributes) + ">"
                + (this.elementBody == null ? "" : "\n" + elementBody.toString())
                + "\n</" + tagName + ">";
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
                + indent + "├─ line no: " + lineNumber + "\n"
                + (attributes.isEmpty() ? "" : indent + "├─ attributes: " + attributes + "\n")
                + (elementBody == null ? "" : indent + "└─ children:\n" + elementBody.print(level + 2));
    }
}
