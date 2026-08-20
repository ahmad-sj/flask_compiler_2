package models.jinja.expressions;

import models.Node;

import java.util.ArrayList;

/**
 * A chain of multiplicative operands: {@code a * b / c % d}.
 *
 * operatorList holds the operator joining each pair, so operatorList.get(i)
 * sits between exprList.get(i) and exprList.get(i + 1). It used to be dropped,
 * which made every division and modulo print and evaluate as multiplication.
 */
public class MulExpression extends Expression {

    public ArrayList<Node> exprList;
    public ArrayList<Node> operatorList;

    public MulExpression(ArrayList<Node> exprList, ArrayList<Node> operatorList) {
        this.exprList = exprList;
        this.operatorList = operatorList;
    }

    /** Returns the operator joining operand i and i + 1, defaulting to "*". */
    public String operatorAt(int i) {
        if (operatorList == null || i >= operatorList.size()) return "*";
        Node op = operatorList.get(i);
        return op instanceof Operator ? ((Operator) op).operator : op.toString();
    }

    @Override
    public String toString() {
        StringBuilder mulExpr = new StringBuilder();

        for (int i = 0; i < exprList.size(); i++) {
            mulExpr.append(exprList.get(i));

            if (i + 1 < exprList.size())
                mulExpr.append(" ").append(operatorAt(i)).append(" ");
        }
        return mulExpr.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        StringBuilder sb = new StringBuilder("mul expr\n");
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
