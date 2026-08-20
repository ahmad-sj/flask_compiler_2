package app;

import antlr.templateLexer;
import antlr.templateParser;
import models.Template;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import symbols.SymbolTable;
import util.BuildLog;
import util.Source;
import util.SyntaxErrors;
import visitors.TemplateVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Discovers and parses every template in the templates directory.
 *
 * Templates are found by scanning the directory rather than from a hardcoded
 * list, and every one goes through the parser. A previous version special-cased
 * base.html and read it as raw text, bypassing the parser entirely, which meant
 * that file had no AST at all.
 */
public class TemplatesHandler {

    /** Extensions treated as templates. */
    private static final String[] TEMPLATE_EXTENSIONS = {".jinja", ".html", ".jinja2", ".htm"};

    private final CompilerConfig config;
    private final SymbolTable symbolTable;
    private final BuildLog log;
    private final List<SyntaxErrors.Entry> allSyntaxErrors = new ArrayList<>();

    public TemplatesHandler(CompilerConfig config, SymbolTable symbolTable, BuildLog log) {
        this.config = config;
        this.symbolTable = symbolTable;
        this.log = log;
    }

    public List<SyntaxErrors.Entry> getSyntaxErrors() {
        return allSyntaxErrors;
    }

    /** Returns parsed templates keyed by file name, or null on a fatal problem. */
    public Map<String, Template> parseAll() {
        Path dir = config.templatesDir();
        log.section("Parsing templates");

        if (!Files.isDirectory(dir)) {
            // Checking a single backend file is a legitimate mode: there are no
            // templates to find and semantic analysis can still run.
            if (config.isSingleFileMode()) {
                log.info("No templates directory - analysing the backend only.");
            } else {
                log.error("Templates directory not found: " + dir);
            }
            return new LinkedHashMap<>();
        }

        List<Path> files = discover(dir);
        if (files.isEmpty()) {
            log.warn("No template files found in " + dir);
            return new LinkedHashMap<>();
        }

        Map<String, Template> templates = new LinkedHashMap<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            Template template = parseOne(file, name);
            if (template != null) {
                templates.put(name, template);
                log.info("Parsed " + name + ": " + template.nodes.size() + " top-level node(s)");
            }
        }
        return templates;
    }

    private List<Path> discover(Path dir) {
        List<Path> found = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                  .filter(TemplatesHandler::isTemplate)
                  .sorted()
                  .forEach(found::add);
        } catch (IOException e) {
            log.error("Could not list " + dir + ": " + e.getMessage());
        }
        return found;
    }

    private static boolean isTemplate(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        for (String extension : TEMPLATE_EXTENSIONS) {
            if (name.endsWith(extension)) return true;
        }
        return false;
    }

    private Template parseOne(Path file, String name) {
        try {
            SyntaxErrors errors = new SyntaxErrors(name);

            CharStream input = Source.read(file);

            templateLexer lexer = new templateLexer(input);
            lexer.removeErrorListeners();
            lexer.addErrorListener(errors);

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            templateParser parser = new templateParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(errors);

            ParseTree tree = parser.template();

            if (!errors.isEmpty()) {
                allSyntaxErrors.addAll(errors.getEntries());
                log.error(errors.count() + " syntax error(s) in " + name + ":");
                for (SyntaxErrors.Entry entry : errors.getEntries()) log.error("  " + entry);
                return null;
            }

            Template template = new TemplateVisitor(name, symbolTable).visit(tree);
            if (template == null) {
                log.error("Failed to build an AST for " + name);
                return null;
            }
            return template;

        } catch (IOException e) {
            log.error("Could not read " + file + ": " + e.getMessage());
            return null;
        }
    }
}
