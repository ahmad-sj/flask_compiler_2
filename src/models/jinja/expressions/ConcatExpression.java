package models.jinja.expressions;

import models.Node;

import java.util.ArrayList;

public class ConcatExpression extends Expression {
    public ArrayList<Node> exprList;

    public ConcatExpression(ArrayList<Node> exprList) {
        this.exprList = exprList;
    }

    @Override
    public String toString() {
        StringBuilder concatExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            concatExpr.append(exprList.get(i));

            if (i + 1 < exprList.size())
                concatExpr.append(" ~ ");
        }
        return concatExpr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder concatExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            if (i + 1 < exprList.size()) {
                concatExpr.append(indent).append("├─ expr").append(i).append(": ");
                concatExpr.append(exprList.get(i).print(level + 2));
                concatExpr.append(indent).append("├─ optor: ~\n");
            } else {
                concatExpr.append(indent).append("└─ expr").append(i).append(": ");
                concatExpr.append(exprList.get(i).print(level + 2));
            }
        }
        return header() + "\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + concatExpr
                ;
    }
}
