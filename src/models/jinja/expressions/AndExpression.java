package models.jinja.expressions;

import models.Node;

import java.util.ArrayList;

public class AndExpression extends Expression {

    public ArrayList<Node> exprList;

    public AndExpression(ArrayList<Node> exprList) {
        this.exprList = exprList;
    }

    @Override
    public String toString() {
        StringBuilder andExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            andExpr.append(exprList.get(i));

            if (i + 1 < exprList.size())
                andExpr.append(" and ");
        }
        return andExpr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder andExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            if (i + 1 < exprList.size()) {
                andExpr.append(indent).append("├─ expr").append(i).append(": ");
                andExpr.append(exprList.get(i).print(level + 2));
                andExpr.append(indent).append("├─ optor: and\n");
            } else {
                andExpr.append(indent).append("└─ expr").append(i).append(": ");
                andExpr.append(exprList.get(i).print(level + 2));
            }
        }

        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + andExpr
                ;
    }
}
