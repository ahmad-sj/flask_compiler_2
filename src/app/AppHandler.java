package app;

import antlr.pythonLexer;
import antlr.pythonParser;
import models.App;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;
import symbols.SymbolTable;
import visitors.AppVisitor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class AppHandler {
    Path path;
    SymbolTable symbolTable;

    public AppHandler(Path path, SymbolTable symbolTable) {
        this.path = path;
        this.symbolTable = symbolTable;
    }

    public void start() {
        // getting parser for python file
        try {
            // مسار الملف app.py
            Path appFilePath = this.path.resolve("tests/app.py");

            // إنشاء Lexer و Parser
            CharStream input = CharStreams.fromFileName(appFilePath.toString());
            pythonLexer lexer = new pythonLexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);


            // طباعة جميع التوكنات للتأكد من INDENT و DEDENT
//            printLexerTokens(tokens);


            // تمرير التوكنات للـ parser
            pythonParser parser = new pythonParser(tokens);


            // إنشاء Parse Tree من القاعدة الرئيسية
            ParseTree tree = parser.prog();


            // طباعة Parse Tree بشكل نصي
//            System.out.println("\nParse Tree (text format):");
//            System.out.println(tree.toStringTree(parser));


//             طباعة Parse Tree بشكل شجري (Hierarchy)
//            System.out.println("\nParse Tree (hierarchy):");
//            printTree(tree, parser, 0);


            // visiting parse tree
            AppVisitor appVisitor = new AppVisitor();
            App app = appVisitor.visit(tree);

            // printing AST
            IO.println("================================================================================");
            for (int i = 0; i < app.nodes.size(); i++) {
                IO.println(app.nodes.get(i).print(0));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // دالة مساعدة لطباعة شجرة Parse Tree بشكل هرمي
    private static void printTree(ParseTree tree, pythonParser parser, int indent) {
        for (int i = 0; i < indent; i++) System.out.print("  ");
        String nodeText = Trees.getNodeText(tree, parser);

        // إبراز INDENT / DEDENT
        if (nodeText.equals("iiindent")) {
            System.out.println("▶▶▶▶▶▶▶▶▶ INDENT");
        } else if (nodeText.equals("dddedent")) {
            System.out.println("◀◀◀◀◀◀◀◀◀ DEDENT");
        } else {
            System.out.println("``" + nodeText.replace("\n", "\\n") + "``");
        }

        for (int i = 0; i < tree.getChildCount(); i++) {
            printTree(tree.getChild(i), parser, indent + 1);
        }
    }

    private void printLexerTokens(CommonTokenStream tokens) {
        tokens.fill(); // اجلب كل التوكنات
        List<Token> tokenList = tokens.getTokens();
        System.out.println("Tokens (type : text) including INDENT/DEDENT:");
        for (Token t : tokenList) {
            IO.print("line: " + t.getLine() + "\t\t");
            String tokenName = pythonLexer.VOCABULARY.getSymbolicName(t.getType());
            System.out.printf("%s : '%s'%n", tokenName, t.getText().replace("\r", "\\r").replace("\n", "\\n"));
        }
    }
}
