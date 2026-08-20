package models.html.elements;

import models.Node;

import java.util.ArrayList;

public class HtmlRegularElement extends HtmlElement {
    public Node elementBody; // object of type NodeBody

    public HtmlRegularElement(String tagName, ArrayList<Node> attrList, Node elementBody) {
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
        String indent = getIndent(level);

        StringBuilder attributes = new StringBuilder();

        if (attrList != null) {
            for (int i = 0; i < attrList.size(); i++) {
                attributes.append(attrList.get(i).toString());

                if (i + 1 < attrList.size())
                    attributes.append(" ");
            }
        }

        return "html element: <" + tagName + ">\n" +
                (attributes.isEmpty() ?
                        (elementBody == null
                                // line no only
                                ? indent + "└─ line no: " + lineNumber + "\n"
                                // line no + children
                                : indent + "├─ line no: " + lineNumber + "\n"
                                + indent + "└─ children:\n" + elementBody.print(level + 1)
                        ) :
                        (elementBody == null
                                // line no + attributes
                                ? indent + "├─ line no: " + lineNumber + "\n"
                                + indent + "└─ attributes: " + attributes + "\n"
                                // line no + attributes + children
                                : indent + "├─ line no: " + lineNumber + "\n"
                                + indent + "├─ attributes: " + attributes + "\n"
                                + indent + "└─ children:\n" + elementBody.print(level + 1)
                        )
                );
    }
}
