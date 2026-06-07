package models.python.expressions;

import models.Node;

import java.util.ArrayList;

//compareExpr
//    : addExpr (compareOptor addExpr)*        //done
//        ;
//
//compareOptor
//    : (LESSTHAN | GREATERTHAN | LESSOREQUAL | GREATEROREQUAL);
public class CompareExpression extends Node {

    public ArrayList<Node> exprList;
    public ArrayList<Node> optorList;


    public CompareExpression(ArrayList<Node> exprList, ArrayList<Node> optorList) {
        this.exprList = exprList;
        this.optorList = optorList;
        this.nodeName = "compare expr";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        int optorCount = this.optorList.size();
        for (int i = 0; i < optorCount; i++) {
            sb.append(exprList.get(i))
                    .append(" ").append(optorList.get(i)).append(" ");
        }
        sb.append(exprList.get(optorCount)).append("\n");
        return sb.toString();
    }

    @Override
    public String print(int level) {
        return this.toString();
    }
}
