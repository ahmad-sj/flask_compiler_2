package models.python.expressions;


import models.Node;
import java.util.ArrayList;
import java.util.List;

public class EqualExpression extends Node {

    private List<Node> exprList = new ArrayList<>();
    private List<String> operators = new ArrayList<>();

    public EqualExpression() {}

    public void addExpr(Node expr) {
        if (expr != null) exprList.add(expr);
    }

    public void addOperator(String op) {
        if (op != null) operators.add(op);
    }

    @Override
    public String toString() {
        if (exprList.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(exprList.get(0).toString());
        for (int i = 1; i < exprList.size(); i++) {
            sb.append(" ").append(operators.get(i - 1)).append(" ").append(exprList.get(i).toString());
        }
        return sb.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);
        StringBuilder sb = new StringBuilder(indent + "equal expr\n");
        sb.append(indent).append("├─ line no: ").append(lineNumber).append("\n");

        for (int i = 0; i < exprList.size(); i++) {
            sb.append(indent).append("├─ expr").append(i).append(": ");
            sb.append(exprList.get(i).print(level + 2));
            if (i > 0) {
                sb.append(indent).append("├─ operator: ").append(operators.get(i - 1)).append("\n");
            }
        }

        return sb.toString();
    }
}

