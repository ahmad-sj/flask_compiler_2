package models.python.simple_statements.return_expr;

import models.Node;

import java.util.ArrayList;
import java.util.List;

public class TupleReturnNode extends Node {



    private   List<Node> exprList = new ArrayList<>();

    public TupleReturnNode(String nodeName, int lineNumber) {

        this.nodeName = nodeName;
        this.lineNumber=lineNumber;


    }
    public void setExpr(Node  expr) {
        if(expr != null) {
            this.exprList.add(expr);
        }
    }

    public List<Node> getExprList() {
        return exprList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(nodeName)
                .append(" (line ")
                .append(lineNumber)
                .append("): ");

        for (int i = 0; i < exprList.size(); i++) {
            sb.append(exprList.get(i).toString());
            if (i + 1 < exprList.size()) sb.append(", ");
        }

        return sb.toString();
    }
}
