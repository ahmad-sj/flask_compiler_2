//GenExpression

package models.python.expressions;

import models.Node;

// genExpr: value FOR NAME IN expr (IF expr)?
public class GenExpression extends Node {
    public Node valueNode;
    public Node nameNode;
    public Node inExpr;
    public Node ifExpr;

    public GenExpression(Node valueNode, Node nameNode, Node inExpr, Node ifExpr) {
        this.valueNode = valueNode;
        this.nameNode = nameNode;
        this.inExpr = inExpr;
        this.ifExpr = ifExpr;
    }

    @Override
    public String print(int level) {
        return "generator expr: \n" +
                "├─ line no: " + this.lineNumber + "\n" +
                "├─ value: " + valueNode.print(level + 1) + "\n" +
                "├─ name: " + nameNode.print(level + 1) + "\n" +
                "├─ inExpr: " + inExpr.print(level + 1) +
                (ifExpr != null ? "\n├─ ifExpr: " + ifExpr.print(level + 1) : "") + "\n"
                ;
    }

    @Override
    public String toString() {
        return "(" + valueNode.toString() + " for "
                + nameNode.toString() + " in "
                + inExpr.toString()
                + (ifExpr != null ? " if " + ifExpr.toString() : "") + ")"
                ;
    }
}
