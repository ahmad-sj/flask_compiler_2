package models.jinja.expressions;

import models.Node;

import java.util.ArrayList;

public class PipeExpression extends Expression {
    public Node expr;
    public ArrayList<Node> filterList;

    public PipeExpression(Node expr, ArrayList<Node> filterList) {
        this.expr = expr;
        this.filterList = filterList;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        if (filterList != null) {
            for (int i = 0; i < filterList.size(); i++) {
                stringBuilder.append(filterList.get(i));

                if (i + 1 < filterList.size())
                    stringBuilder.append(" | ");
            }
        }

        return expr.toString() + " | " + stringBuilder;
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder filters = new StringBuilder();

        if (filterList != null) {
            for (int i = 0; i < filterList.size(); i++) {
                filters.append(getIndent(level + 1));
                filters.append("├─ ");
                filters.append(filterList.get(i).print(level + 1));
            }
        }

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "├─ expr: " + expr.print(level + 1)
                + indent + "└─ filters:\n" + filters
                ;
    }
}
