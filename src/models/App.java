package models;

import java.util.ArrayList;
import java.util.List;
import symbols.SemanticError;

public class App {
    public ArrayList<Node> nodes;
    public List<SemanticError> semanticErrors = new ArrayList<>();

    public App() {
        this.nodes = new ArrayList<>();
    }

    public void addNode(Node n) {
        nodes.add(n);
    }

}
