//package models.python.simpleStatements;
//
//import models.Node;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class ReturnLine extends Node {
//
//    List<Node> expr =new ArrayList<>();
//
//    public ReturnLine() {
//    }
//
//    public ReturnLine(Node expr) {
//        this.expr.add(expr);
//    }
//
//    @Override
//    public String toString() {
//        return (expr != null ? "return " + this.expr.toString() : "return \n");
//    }
//}

package models.python.simple_statements;

import models.Node;

public class ExprLine extends Node {

    public Node returnExpr;

    public ExprLine(Node returnExpr) {
        this.returnExpr = returnExpr;
    }

    @Override
    public String print(int level) {

        String indent = getIndent(level);

        return "expr line\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "└─ expr: " + returnExpr.print(level + 2)
                ;

    }

    @Override
    public String toString() {
        return "- expr line:" +
                "\nline no: " + lineNumber +
                "\nexpr: " + returnExpr.toString();

    }
}
