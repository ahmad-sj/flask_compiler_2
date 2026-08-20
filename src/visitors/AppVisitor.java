package visitors;

import antlr.pythonParser;
import antlr.pythonParserBaseVisitor;
import models.App;
import models.Node;

/**
 * Builds the App AST from the Python parse tree.
 *
 * This visitor only constructs the tree. Semantic analysis used to run here as
 * a side effect of visiting the program node, printing straight to stdout,
 * which made "parse" and "check" the same step and gave the caller no way to
 * run one without the other. FlaskCompiler now invokes SemanticAnalyzer as its
 * own phase.
 */
public class AppVisitor extends pythonParserBaseVisitor<App> {

    @Override
    public App visitProg(pythonParser.ProgContext ctx) {
        App app = new App();
        PythonVisitor pythonVisitor = new PythonVisitor();

        for (int i = 0; i < ctx.stmt().size(); i++) {
            Node child = pythonVisitor.visit(ctx.stmt().get(i));
            app.addNode(child);
        }

        return app;
    }
}
