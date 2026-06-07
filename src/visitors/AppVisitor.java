package visitors;

import antlr.pythonParser;
import antlr.pythonParserBaseVisitor;
import models.App;
import models.Node;
import symbols.SemanticError;

import java.util.List;

public class AppVisitor extends pythonParserBaseVisitor<App> {
    @Override
    public App visitProg(pythonParser.ProgContext ctx) {
        App app = new App();

        PythonVisitor pythonVisitor = new PythonVisitor();

        for (int i = 0; i < ctx.stmt().size(); i++) {
            Node child = pythonVisitor.visit(ctx.stmt(i));
            app.addNode(child);
        }

        // ── Semantic analysis ──────────────────────────────
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        List<SemanticError> errors = analyzer.analyze(app);

        if (errors.isEmpty()) {
            System.out.println("[Semantic] No errors found.");
        } else {
            System.out.println("[Semantic] " + errors.size() + " error(s) found:");
            for (SemanticError e : errors) System.out.println("  " + e);
        }
        // ───────────────────────────────────────────────────

        return app;
    }
}
