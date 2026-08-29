package models.python.simple_statements.import_lines;

import models.Node;

import java.util.ArrayList;

public class MultiImport extends Node {
    public Node fromName; // of type NameList
    public ArrayList<Node> importedNames; // of type TypeName

    public MultiImport(Node fromName, ArrayList<Node> importedNames) {
        this.fromName = fromName;
        this.importedNames = importedNames;
    }

    @Override
    public String print(int level) {

        String indent = getIndent(level);

        StringBuilder namesList = new StringBuilder();

        for (int i = 0; i < importedNames.size(); i++) {
            namesList.append(importedNames.get(i).toString());

            if (i + 1 < importedNames.size())
                namesList.append(", ");
        }

        return header() + "\n" +
                indent + "├─ line no: " + lineNumber + "\n" +
                indent + "├─ from name: " + fromName.print(level + 2) +
                indent + "└─ imported names: " + namesList.toString() + "\n"
                ;

    }

    @Override
    public String toString() {
        StringBuilder imports = new StringBuilder();

        for (int i = 0; i < importedNames.size(); i++) {
            imports.append(importedNames.get(i));

            if (i + 1 < importedNames.size())
                imports.append(", ");
        }

        return "- multi import" +
                "\nline no: " + lineNumber +
                "\nfrom name: " + fromName.toString() +
                "\nimport names: " + imports.toString() + "\n";
    }
}
