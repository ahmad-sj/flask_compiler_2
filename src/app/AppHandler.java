package app;

import antlr.pythonLexer;
import antlr.pythonParser;
import models.App;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import util.BuildLog;
import util.Source;
import util.SyntaxErrors;
import visitors.AppVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Parses app.py into an App AST.
 *
 * Lexer and parser diagnostics are collected rather than printed to stderr and
 * ignored, so a malformed backend file is reported instead of silently
 * producing a half-built tree.
 */
public class AppHandler {

    private final CompilerConfig config;
    private final BuildLog log;
    private SyntaxErrors syntaxErrors;

    public AppHandler(CompilerConfig config, BuildLog log) {
        this.config = config;
        this.log = log;
    }

    public SyntaxErrors getSyntaxErrors() {
        return syntaxErrors;
    }

    /** Returns the parsed AST, or null if the file is missing or unparseable. */
    public App parse() {
        Path appFile = config.appFile();
        log.section("Parsing backend");

        if (!Files.exists(appFile)) {
            log.error("Backend file not found: " + appFile);
            log.error("Expected an app.py in the input directory.");
            return null;
        }

        try {
            String fileName = appFile.getFileName().toString();
            syntaxErrors = new SyntaxErrors(fileName);

            CharStream input = Source.read(appFile);

            pythonLexer lexer = new pythonLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(syntaxErrors);

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            pythonParser parser = new pythonParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(syntaxErrors);

            ParseTree tree = parser.prog();

            if (!syntaxErrors.isEmpty()) {
                log.error(syntaxErrors.count() + " syntax error(s) in " + fileName + ":");
                for (SyntaxErrors.Entry entry : syntaxErrors.getEntries()) {
                    log.error("  " + entry);
                }
                return null;
            }

            App app = new AppVisitor().visit(tree);
            if (app == null) {
                log.error("Failed to build an AST from " + fileName);
                return null;
            }

            log.info("Parsed " + fileName + ": " + app.nodes.size() + " top-level statement(s)");
            return app;

        } catch (IOException e) {
            log.error("Could not read " + appFile + ": " + e.getMessage());
            return null;
        }
    }
}
