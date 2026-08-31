package app;

import models.App;
import models.Template;
import symbols.SemanticError;
import symbols.SymbolTable;
import util.BuildLog;
import util.SyntaxErrors;
import visitors.PythonDataExtractor;
import visitors.SemanticAnalyzer;
import visitors.TemplateSemanticAnalyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pipeline entry point.
 *
 *   app.py ──▶ Python parser ──▶ Python AST ──┐
 *                                             ├──▶ semantic analysis ──▶ report
 *   templates/ ──▶ Jinja parser ──▶ Jinja AST ┘            │
 *                                                          ▼
 *                                          data extraction ──▶ context
 *                                                          ▼
 *                                              Jinja rendering ──▶ output/*.html
 *
 * Each stage is an explicit step here rather than a side effect of the stage
 * before it, so a failure can be reported against the stage that caused it.
 */
public class FlaskCompiler {

    public static void main(String[] args) {
        CompilerConfig config = CompilerConfig.fromArgs(args);
        BuildLog log = new BuildLog(true);

        int exitCode = 0;
        try {
            exitCode = run(config, log);
        } catch (IOException e) {
            log.error("I/O failure: " + e.getMessage());
            exitCode = 2;
        } finally {
            try {
                log.writeTo(config.compilerOutputDir.resolve("generation_log.txt"));
            } catch (IOException e) {
                System.err.println("Could not write generation_log.txt: " + e.getMessage());
            }
        }
        System.exit(exitCode);
    }

    /** Returns a process exit code: 0 ok, 1 compilation problem, 2 I/O failure. */
    private static int run(CompilerConfig config, BuildLog log) throws IOException {
        log.section("Configuration");
        log.info("Input:            " + config.inputDir);
        log.info("Output:           " + config.outputDir);
        log.info("Compiler output:  " + config.compilerOutputDir);

        if (!Files.isDirectory(config.inputDir)) {
            log.error("Input directory not found: " + config.inputDir);
            log.error("Usage: FlaskCompiler [inputDir] [outputDir] [compilerOutputDir]");
            return 1;
        }
        Files.createDirectories(config.compilerOutputDir);

        SymbolTable symbolTable = new SymbolTable();

        // ── Phase 1: parse the backend ────────────────────────────────────
        AppHandler appHandler = new AppHandler(config, log);
        App app = appHandler.parse();

        // ── Phase 2: parse the templates ──────────────────────────────────
        TemplatesHandler templatesHandler = new TemplatesHandler(config, symbolTable, log);
        Map<String, Template> templates = templatesHandler.parseAll();
        if (templates == null) templates = new LinkedHashMap<>();

        // ── Dump every stage, even if later ones fail ─────────────────────
        // Order matters for the console: tokens, then parse tree, then AST.
        writeStageDumps(config, log, appHandler, templatesHandler);
        writeAstDumps(config, log, app, templates);

        if (app == null) {
            // No analysis table to show: the backend never got that far.
            writeSymbolTables(config, log, null, symbolTable);
            writeSemanticReport(config, log, null, templatesHandler.getSyntaxErrors(),
                    new ArrayList<>());
            log.section("Result");
            log.error("Backend could not be parsed - nothing generated.");
            return 1;
        }

        // ── Phase 3: semantic analysis ────────────────────────────────────
        log.section("Semantic analysis");
        SemanticAnalyzer analyzer = new SemanticAnalyzer();
        List<SemanticError> errors = analyzer.analyze(app);
        app.semanticErrors = errors;

        if (errors.isEmpty()) {
            log.info("No semantic errors found.");
        } else {
            log.error(errors.size() + " semantic error(s):");
            for (SemanticError error : errors) log.error("  " + error);
        }

        // ── Phase 4: extract the render context from the Python AST ───────
        PythonDataExtractor extractor = new PythonDataExtractor();
        extractor.extract(app);

        // ── Phase 5: semantic analysis of the templates ───────────────────
        // Checks each template against the context the route rendering it
        // actually supplies. Without this, a template could reference a name no
        // route passes and the build would still report success.
        List<SemanticError> templateErrors = config.isSingleFileMode()
                ? new ArrayList<>()
                : new TemplateSemanticAnalyzer(templates, extractor.getRoutes(),
                        extractor.getModuleVars().keySet(), symbolTable).analyze();

        // Both tables are dumped here, after every phase that writes to one has
        // run: the Python table does not exist until analyze() has, and the
        // template table gains the scopes phase 5 resolves against. Dumping any
        // earlier would report a table the checks had not finished using.
        writeSymbolTables(config, log, analyzer.getSymbolTable(), symbolTable);

        if (templateErrors.isEmpty()) {
            log.info("No template errors found.");
        } else {
            log.error(templateErrors.size() + " template error(s):");
            for (SemanticError error : templateErrors) log.error("  " + error);
            errors = new ArrayList<>(errors);
            errors.addAll(templateErrors);
            app.semanticErrors = errors;
        }

        // ── Phase 6: generation ───────────────────────────────────────────
        CodeGenerator generator = new CodeGenerator(app, templates, config, log, extractor);
        boolean generated = generator.generate();

        writeSemanticReport(config, log, errors, templatesHandler.getSyntaxErrors(),
                generator.getProblems());

        // ── Result ────────────────────────────────────────────────────────
        log.section("Result");
        if (!errors.isEmpty()) {
            log.error("Generation blocked by " + errors.size() + " semantic error(s).");
            return 1;
        }
        if (!generated) {
            log.error("Generation did not complete.");
            return 1;
        }
        log.info("Success: " + generator.getPagesGenerated() + " page(s) written to "
                + config.outputDir.getFileName() + "/");
        return 0;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  COMPILER OUTPUT ARTIFACTS
    // ═══════════════════════════════════════════════════════════════════════

    private static void writeAstDumps(CompilerConfig config, BuildLog log,
                                      App app, Map<String, Template> templates) {
        log.section("Compiler output");

        // Machine-readable dumps.
        write(config.compilerOutputDir.resolve("ast_python.json"),
                AstDumper.dumpPythonAst(app, config.appFile().getFileName().toString()), log);
        write(config.compilerOutputDir.resolve("ast_jinja.json"),
                AstDumper.dumpJinjaAst(templates), log);

        // Human-readable trees, produced by the per-node print methods.
        String pythonTree = TreePrinter.renderPythonAst(app, config.appFile().getFileName().toString());
        String jinjaTree = TreePrinter.renderTemplateAsts(templates);

        write(config.compilerOutputDir.resolve("ast_python.txt"), pythonTree, log);
        write(config.compilerOutputDir.resolve("ast_jinja.txt"), jinjaTree, log);

        // Print during execution, as the project requires. --quiet-ast skips the
        // console dump; the files above are always written.
        if (config.printTrees) {
            System.out.print(TreePrinter.forConsole(pythonTree));
            System.out.print(TreePrinter.forConsole(jinjaTree));
        } else {
            log.info("Tree dump suppressed (--quiet-ast); see the .txt files above.");
        }
    }

    /**
     * Writes symbol_table.txt, containing both tables.
     *
     * Called after semantic analysis, since the Python table is created inside
     * analyze(). Passing null for it records that analysis did not run rather
     * than silently omitting the section.
     */
    private static void writeSymbolTables(CompilerConfig config, BuildLog log,
                                          SymbolTable analysisTable,
                                          SymbolTable templateTable) {
        String symbols = TreePrinter.renderSymbolTables(analysisTable, templateTable);
        write(config.compilerOutputDir.resolve("symbol_table.txt"), symbols, log);

        if (config.printTrees) {
            System.out.print(TreePrinter.forConsole(symbols));
        }
    }

    /**
     * Writes the lexer token streams and ANTLR parse trees.
     *
     * These are the two stages before the AST: the tokens the lexer produced,
     * and the concrete syntax tree showing which grammar rules matched. They are
     * far longer than the AST, so the console dump is opt-in via --print-tokens
     * and --print-parse-tree while the files are always written.
     */
    private static void writeStageDumps(CompilerConfig config, BuildLog log,
                                        AppHandler appHandler,
                                        TemplatesHandler templatesHandler) {
        StringBuilder tokens = new StringBuilder();
        StringBuilder trees = new StringBuilder();

        String appName = config.appFile().getFileName().toString();
        if (appHandler.getTokenStream() != null) {
            tokens.append(TreePrinter.renderTokens(appHandler.getTokenStream(),
                    antlr.pythonLexer.VOCABULARY, appName));
        }
        if (appHandler.getParseTree() != null) {
            trees.append(TreePrinter.renderParseTree(appHandler.getParseTree(),
                    appHandler.getParser(), appName));
        }

        for (Map.Entry<String, TemplatesHandler.Parsed> entry : templatesHandler.getParsed().entrySet()) {
            TemplatesHandler.Parsed p = entry.getValue();
            tokens.append(TreePrinter.renderTokens(p.tokens,
                    antlr.templateLexer.VOCABULARY, entry.getKey()));
            trees.append(TreePrinter.renderParseTree(p.tree, p.parser, entry.getKey()));
        }

        write(config.compilerOutputDir.resolve("tokens.txt"), tokens.toString(), log);
        write(config.compilerOutputDir.resolve("parse_tree.txt"), trees.toString(), log);

        if (config.printTokens)    System.out.print(TreePrinter.forConsole(tokens.toString()));
        if (config.printParseTree) System.out.print(TreePrinter.forConsole(trees.toString()));
    }

    /**
     * Writes semantic_report.txt: syntax errors, semantic errors, and the
     * non-fatal problems the renderer hit.
     */
    private static void writeSemanticReport(CompilerConfig config, BuildLog log,
                                            List<SemanticError> semanticErrors,
                                            List<SyntaxErrors.Entry> syntaxErrors,
                                            List<String> renderProblems) {
        StringBuilder report = new StringBuilder();
        report.append("SEMANTIC ANALYSIS REPORT").append(System.lineSeparator());
        report.append("========================").append(System.lineSeparator());
        report.append("Input: ").append(config.inputDir).append(System.lineSeparator());
        report.append(System.lineSeparator());

        section(report, "Template syntax errors", syntaxErrors.size());
        for (SyntaxErrors.Entry entry : syntaxErrors) {
            report.append("  ").append(entry).append(System.lineSeparator());
        }
        report.append(System.lineSeparator());

        if (semanticErrors == null) {
            report.append("Semantic checks: not run (backend failed to parse)")
                  .append(System.lineSeparator());
        } else {
            section(report, "Semantic errors", semanticErrors.size());
            for (SemanticError error : semanticErrors) {
                report.append("  ").append(error).append(System.lineSeparator());
            }
        }
        report.append(System.lineSeparator());

        section(report, "Rendering problems", renderProblems.size());
        for (String problem : renderProblems) {
            report.append("  ").append(problem).append(System.lineSeparator());
        }
        report.append(System.lineSeparator());

        int total = syntaxErrors.size()
                + (semanticErrors == null ? 0 : semanticErrors.size())
                + renderProblems.size();
        report.append(total == 0 ? "RESULT: clean" : "RESULT: " + total + " issue(s) found")
              .append(System.lineSeparator());

        write(config.compilerOutputDir.resolve("semantic_report.txt"), report.toString(), log);
    }

    private static void section(StringBuilder out, String title, int count) {
        out.append(title).append(": ").append(count).append(System.lineSeparator());
        out.append("-".repeat(title.length() + 2)).append(System.lineSeparator());
    }

    private static void write(Path file, String content, BuildLog log) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, content.getBytes(StandardCharsets.UTF_8));
            log.info("Wrote " + file.getFileName() + " (" + content.length() + " bytes)");
        } catch (IOException e) {
            log.error("Could not write " + file.getFileName() + ": " + e.getMessage());
        }
    }
}
