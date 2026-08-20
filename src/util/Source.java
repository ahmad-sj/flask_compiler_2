package util;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads a source file into a CharStream for ANTLR.
 *
 * Files saved by Windows editors and by PowerShell's Set-Content commonly begin
 * with a UTF-8 byte order mark. The lexers have no rule for U+FEFF, so a BOM
 * surfaced as "token recognition error at: '?'" on line 1 and failed the whole
 * parse. Stripping it here keeps that from being the user's problem.
 */
public final class Source {

    private static final char BOM = '\uFEFF';

    private Source() {
    }

    public static CharStream read(Path file) throws IOException {
        String text = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        if (!text.isEmpty() && text.charAt(0) == BOM) {
            text = text.substring(1);
        }
        return CharStreams.fromString(text, file.getFileName().toString());
    }
}
