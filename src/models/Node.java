package models;

public abstract class Node {
    //    public String name;
    protected String nodeName;
    protected int lineNumber;

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String print(int level) {
        return "################## method print is not overrided in class: " + nodeName + ", indent level: " + level + " ##################\n";
    }

    public String getIndent(int level) {
        StringBuilder indent = new StringBuilder();

        for (int i = 0; i < level; i++) {
            indent.append("```");
        }

        return indent.toString();
    }

    public int getLineNumber() {
        return lineNumber;
    }
}
