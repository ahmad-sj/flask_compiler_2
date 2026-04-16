package app;

import antlr.templateLexer;
import antlr.templateParser;
import models.Node;
import models.Template;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import symbols.SymbolTable;
import visitors.TemplateVisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class TemplatesHandler {

    ArrayList<String> files;
    SymbolTable symbolTable;
    Path path;

    public TemplatesHandler(Path path, SymbolTable symbolTable) {
        this.path = path;
        this.symbolTable = symbolTable;
    }

    public void start() {
        // creating an array with file names to be parsed
        ArrayList<String> fileNames = new ArrayList<>();
        fileNames.add("tests/base.html");
        fileNames.add("tests/index.html");
        fileNames.add("tests/add.html");
        fileNames.add("tests/detail.html");
        fileNames.add("tests/tests.html");

        // getting files full paths
        ArrayList<String> files = new ArrayList<>();
        for (String fileName : fileNames) {

            // get full path for file name number i
            Path filePathObject = path.resolve(fileName);

            // add file full path to files array
            files.add(filePathObject.toString());
        }

        templateParser parser;

        for (int i = 0; i < files.size(); i++) {
            parser = getParser(files.get(i));

            // tell antlr to build a parse tree
            // parse from the start symbol (template)
            ParseTree antlrAST = parser.template();

            // getting file name
            String fileName = Paths.get(files.get(i)).getFileName().toString();

            // create a visitor for converting the parse tree into node object
            TemplateVisitor templateVisitor = new TemplateVisitor(fileName, symbolTable);

            // visit parse tree built by antlr
            Template template = templateVisitor.visit(antlrAST);

            IO.println("\n######################### " + fileName + " #########################\n");

            for (Node node : template.nodes) {
                System.out.println(node.print(0));
            }
        }

        IO.println("\n######################### " + "Symbols Table" + " #########################\n");
        symbolTable.print();
    }

    // types of parser and lexer are specific to the grammar name template.
    private static templateParser getParser(String fileName) {
        templateParser parser = null;
        try {
            CharStream input = CharStreams.fromFileName(fileName);
            templateLexer lexer = new templateLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            parser = new templateParser(tokens);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return parser;
    }
}



