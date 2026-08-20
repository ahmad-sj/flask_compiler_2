package app;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    /**
     * Builds a config from command-line arguments.
     *
     *   FlaskCompiler [input] [outputDir] [compilerOutputDir]
     *
     * input may be a project directory, or a single .py file. Pointing at one
     * file is what the semantic test fixtures use: each is a standalone backend
     * with no templates of its own.
     */
    public static CompilerConfig fromArgs(String[] args) {
        Path cwd = Paths.get("").toAbsolutePath();
        Path input = cwd.resolve(args.length > 0 ? args[0] : "project");
        Path out   = cwd.resolve(args.length > 1 ? args[1] : "output");
        Path co    = cwd.resolve(args.length > 2 ? args[2] : "compiler_output");

        // A .py path names the backend directly; its folder becomes the input dir.
        if (Files.isRegularFile(input) && input.toString().toLowerCase().endsWith(".py")) {
            Path parent = input.getParent() != null ? input.getParent() : cwd;
            return new CompilerConfig(parent, out, co, input);
        }
        return new CompilerConfig(input, out, co, null);
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
