package models.jinja.expressions;

import models.Node;

import java.util.ArrayList;

public class OrExpression extends Expression {
    public ArrayList<Node> exprList;

    public OrExpression(ArrayList<Node> exprList) {
        this.exprList = exprList;
    }

    @Override
    public String toString() {
        StringBuilder orExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            orExpr.append(exprList.get(i));

            if (i + 1 < exprList.size())
                orExpr.append(" or ");
        }
        return orExpr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder orExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            if (i + 1 < exprList.size()) {
                orExpr.append(indent).append("├─ expr").append(i).append(": ");
                orExpr.append(exprList.get(i).print(level + 2));
                orExpr.append(indent).append("├─ optor: or\n");
            } else {
                orExpr.append(indent).append("└─ expr").append(i).append(": ");
                orExpr.append(exprList.get(i).print(level + 2));
            }
        }

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + orExpr
                ;
    }
}
