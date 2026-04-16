package models.python.simple_statements.return_expr;

import models.Node;

public class SingleReturnNode extends Node {



    private  Node expr;

    public SingleReturnNode(String nodeName, int lineNumber) {

        this.nodeName=nodeName;
        this    .lineNumber=lineNumber;
    }

    public Node getExpr() {
        return expr;
    }


    public void setExpr(Node expr) {
        this.expr = expr;
    }


    @Override
    public String toString() {
        if (expr != null) {
            return nodeName + " (line " + lineNumber + "): " + expr.toString();
        } else {
            return nodeName + " (line " + lineNumber + ")";
        }
    }
}
