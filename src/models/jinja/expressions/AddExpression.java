package models.jinja.expressions;

import models.Node;

import java.util.ArrayList;

public class AddExpression extends Expression {

    public ArrayList<Node> exprList;

    public AddExpression(ArrayList<Node> exprList) {
        this.exprList = exprList;
    }

    @Override
    public String toString() {
        StringBuilder addExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            addExpr.append(exprList.get(i));

            if (i + 1 < exprList.size())
                addExpr.append(" + ");
        }
        return addExpr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder addExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            if (i + 1 < exprList.size()) {
                addExpr.append(indent).append("├─ expr").append(i).append(": ");
                addExpr.append(exprList.get(i).print(level + 2));
                addExpr.append(indent).append("├─ optor: +\n");
            } else {
                addExpr.append(indent).append("└─ expr").append(i).append(": ");
                addExpr.append(exprList.get(i).print(level + 2));
            }
        }

        return "add expr\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + addExpr
                ;
    }
}
