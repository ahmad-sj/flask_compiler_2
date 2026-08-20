package util;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects lexer and parser errors instead of printing them to stderr.
 *
 * ANTLR's default listener writes to the console and lets compilation carry on,
 * which previously meant a template could fail to tokenize and still be treated
 * as if it had parsed cleanly. Collecting the errors lets the compiler report
 * them properly and refuse to generate from a broken parse.
 */
public class SyntaxErrors extends BaseErrorListener {

    /** One lexer/parser diagnostic. */
    public static class Entry {
        public final String file;
        public final int line;
        public final int column;
        public final String message;

        Entry(String file, int line, int column, String message) {
            this.file = file;
            this.line = line;
            this.column = column;
            this.message = message;
        }

        @Override
        public String toString() {
            return file + ":" + line + ":" + column + "  " + message;
        }
    }

    private final String fileName;
    private final List<Entry> entries = new ArrayList<>();

    public SyntaxErrors(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        entries.add(new Entry(fileName, line, charPositionInLine, msg));
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int count() {
        return entries.size();
    }
}
