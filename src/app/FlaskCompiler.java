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

        // ── Dump both ASTs, even if later stages fail ─────────────────────
        writeAstDumps(config, log, app, templates, symbolTable);

        if (app == null) {
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
                        extractor.getModuleVars().keySet()).analyze();

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
                                      App app, Map<String, Template> templates,
                                      SymbolTable symbolTable) {
        log.section("Compiler output");

        // Machine-readable dumps.
        write(config.compilerOutputDir.resolve("ast_python.json"),
                AstDumper.dumpPythonAst(app, config.appFile().getFileName().toString()), log);
        write(config.compilerOutputDir.resolve("ast_jinja.json"),
                AstDumper.dumpJinjaAst(templates), log);

        // Human-readable trees, produced by the per-node print methods.
        String pythonTree = TreePrinter.renderPythonAst(app, config.appFile().getFileName().toString());
        String jinjaTree = TreePrinter.renderTemplateAsts(templates);
        String symbols = TreePrinter.renderSymbolTable(symbolTable);

        write(config.compilerOutputDir.resolve("ast_python.txt"), pythonTree, log);
        write(config.compilerOutputDir.resolve("ast_jinja.txt"), jinjaTree, log);
        write(config.compilerOutputDir.resolve("symbol_table.txt"), symbols, log);

        // Print during execution, as the project requires. --quiet-ast skips the
        // console dump; the files above are always written.
        if (config.printTrees) {
            System.out.print(TreePrinter.forConsole(pythonTree));
            System.out.print(TreePrinter.forConsole(jinjaTree));
            System.out.print(TreePrinter.forConsole(symbols));
        } else {
            log.info("Tree dump suppressed (--quiet-ast); see the .txt files above.");
        }
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
