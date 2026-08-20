package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates the human-readable build log while echoing it to the console,
 * so compiler_output/generation_log.txt and what the user sees stay identical.
 */
public class BuildLog {

    private final List<String> lines = new ArrayList<>();
    private final boolean echo;

    public BuildLog(boolean echo) {
        this.echo = echo;
        add("Flask static-site compiler");
        add("Run at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    /** Starts a new titled section. */
    public void section(String title) {
        add("");
        add("=== " + title + " ===");
    }

    /** Logs a routine step. */
    public void info(String message) {
        add("  " + message);
    }

    /** Logs a non-fatal problem worth surfacing. */
    public void warn(String message) {
        add("  WARNING  " + message);
    }

    /** Logs a fatal problem. */
    public void error(String message) {
        add("  ERROR    " + message);
    }

    private void add(String line) {
        lines.add(line);
        if (echo) System.out.println(line);
    }

    /** Writes the accumulated log to disk. */
    public void writeTo(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Files.write(file, String.join(System.lineSeparator(), lines)
                .getBytes(StandardCharsets.UTF_8));
    }
}
