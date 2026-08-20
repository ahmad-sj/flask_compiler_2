package models.jinja.expressions;

import models.Node;

import java.util.ArrayList;

/**
 * A chain of additive operands: {@code a + b - c}.
 *
 * operatorList holds the operator that joins each pair, so operatorList.get(i)
 * sits between exprList.get(i) and exprList.get(i + 1) and is therefore always
 * one shorter than exprList. It used to be dropped entirely, which made every
 * subtraction print and evaluate as an addition.
 */
public class AddExpression extends Expression {

    public ArrayList<Node> exprList;
    public ArrayList<Node> operatorList;

    public AddExpression(ArrayList<Node> exprList, ArrayList<Node> operatorList) {
        this.exprList = exprList;
        this.operatorList = operatorList;
    }

    /** Returns the operator joining operand i and i + 1, defaulting to "+". */
    public String operatorAt(int i) {
        if (operatorList == null || i >= operatorList.size()) return "+";
        Node op = operatorList.get(i);
        return op instanceof Operator ? ((Operator) op).operator : op.toString();
    }

    @Override
    public String toString() {
        StringBuilder addExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            addExpr.append(exprList.get(i));

            if (i + 1 < exprList.size())
                addExpr.append(" ").append(operatorAt(i)).append(" ");
        }
        return addExpr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder sb = new StringBuilder("add expr\n");
        sb.append(indent).append("├─ line no: ").append(lineNumber).append("\n");

        for (int i = 0; i < exprList.size(); i++) {
            boolean last = i + 1 == exprList.size();
            sb.append(indent).append(last ? "└─ " : "├─ ")
              .append(last ? "operand: " : "operand (" + operatorAt(i) + " follows): ")
              .append(exprList.get(i).print(level + 2));
        }
        return sb.toString();
    }
}
