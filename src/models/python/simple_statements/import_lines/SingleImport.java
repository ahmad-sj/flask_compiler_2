package models.python.simple_statements.import_lines;

import models.Node;
import models.python.Statement;

public class SingleImport extends Statement {


    Node importedName; // of type Name
    Node importAlias; // of type TypeName

    public SingleImport(Node importedName, Node importAlias) {
        this.importedName = importedName;
        this.importAlias = importAlias;
    }


    @Override
    public String print(int level) {

        String indent = getIndent(level);

        return "- single import\n" +
                indent + "├─ line no: " + lineNumber + "\n" +
                (importAlias == null
                        ? indent + "└─ imported name: " + importedName.print(level + 2)
                        : indent + "├─ imported name: " + importedName.print(level + 2)
                        + indent + "└─ import alias: " + importAlias.print(level + 2)
                );

    }

    @Override
    public String toString() {

        return "single import" +
                "\nline no: " + lineNumber +
                "\nimported name: " + importedName.toString() +
                (importAlias == null
                        ? ""
                        : " as " + importAlias.toString() + "\n"
                )
                ;
    }
}
