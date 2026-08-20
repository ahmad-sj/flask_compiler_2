package app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Input and output locations for one compiler run.
 *
 * Layout follows the project spec:
 *
 *   &lt;input&gt;/app.py              backend data + routes
 *   &lt;input&gt;/templates/*.jinja   templates (.html also accepted)
 *   &lt;input&gt;/style.css           static assets, copied verbatim
 *   &lt;input&gt;/script.js
 *
 *   output/                     rendered pages + copied assets
 *   compiler_output/            ASTs, semantic report, generation log
 */
public class CompilerConfig {

    public final Path inputDir;
    public final Path outputDir;
    public final Path compilerOutputDir;

    /** Set when the input names a .py file directly rather than a project directory. */
    private final Path explicitAppFile;

    /** Static files copied to the output directory untransformed. */
    public static final String[] STATIC_ASSETS = {"app.py", "style.css", "script.js"};

    public CompilerConfig(Path inputDir, Path outputDir, Path compilerOutputDir,
                          Path explicitAppFile) {
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        this.compilerOutputDir = compilerOutputDir;
        this.explicitAppFile = explicitAppFile;
    }

    /** When false, the AST and symbol table are written to files but not echoed. */
    public boolean printTrees = true;

    /** Echo the lexer token streams. Off by default: they are long. */
    public boolean printTokens = false;

    /** Echo the ANTLR parse trees. Off by default: they are very long. */
    public boolean printParseTree = false;

    /**
     * Builds a config from command-line arguments.
     *
     *   FlaskCompiler [input] [outputDir] [compilerOutputDir] [--quiet-ast]
     *
     * input may be a project directory, or a single .py file. Pointing at one
     * file is what the semantic test fixtures use: each is a standalone backend
     * with no templates of its own.
     *
     * --quiet-ast suppresses the console tree dump; the text files in
     * compiler_output/ are written either way.
     */
    public static CompilerConfig fromArgs(String[] args) {
        Path cwd = Paths.get("").toAbsolutePath();

        // Separate flags from positional paths so order does not matter.
        List<String> positional = new ArrayList<>();
        boolean quietAst = false;
        boolean tokens = false;
        boolean parseTree = false;
        for (String arg : args) {
            if (arg == null || arg.isEmpty()) continue;
            if (arg.startsWith("--")) {
                switch (arg) {
                    case "--quiet-ast":       quietAst = true; break;
                    case "--print-tokens":    tokens = true; break;
                    case "--print-parse-tree": parseTree = true; break;
                    case "--print-all":       tokens = true; parseTree = true; quietAst = false; break;
                    default: System.err.println("Ignoring unknown option: " + arg);
                }
            } else {
                positional.add(arg);
            }
        }

        Path input = cwd.resolve(positional.size() > 0 ? positional.get(0) : "project");
        Path out   = cwd.resolve(positional.size() > 1 ? positional.get(1) : "output");
        Path co    = cwd.resolve(positional.size() > 2 ? positional.get(2) : "compiler_output");

        CompilerConfig config;
        // A .py path names the backend directly; its folder becomes the input dir.
        if (Files.isRegularFile(input) && input.toString().toLowerCase().endsWith(".py")) {
            Path parent = input.getParent() != null ? input.getParent() : cwd;
            config = new CompilerConfig(parent, out, co, input);
        } else {
            config = new CompilerConfig(input, out, co, null);
        }
        config.printTrees = !quietAst;
        config.printTokens = tokens;
        config.printParseTree = parseTree;
        return config;
    }

    public Path appFile() {
        return explicitAppFile != null ? explicitAppFile : inputDir.resolve("app.py");
    }

    public Path templatesDir() {
        return inputDir.resolve("templates");
    }

    /** True when the run targets a single backend file rather than a full project. */
    public boolean isSingleFileMode() {
        return explicitAppFile != null;
    }
}
