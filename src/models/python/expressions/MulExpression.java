package models.python.expressions;


import models.Node;
import models.jinja.expressions.Operator;


import java.util.List;


public class MulExpression extends Node {

    private List<Node> exprList;
    private List<Operator> operators;

    public MulExpression(List<Node> exprList, List<Operator> operators) {
        this.exprList = exprList;
        this.operators = operators;
        this.nodeName = "mul expr";
    }

    public List<Node> getExprList() { return exprList; }
    public List<Operator> getOperators() { return operators; }

    @Override
    public String toString() {
        if (operators.isEmpty()) return exprList.get(0).toString();

        StringBuilder sb = new StringBuilder();
        sb.append(exprList.get(0).toString());

        for (int i = 0; i < operators.size(); i++) {
            sb.append(" ").append(operators.get(i).toString()).append(" ");
            sb.append(exprList.get(i + 1).toString());
        }

        return sb.toString();
    }
}
