package app;

import models.App;
import models.Node;
import models.Template;
import symbols.SymbolTable;

import java.nio.charset.Charset;

import java.util.Map;

/**
 * Driver for the per-node print methods (project requirement §5).
 *
 * Each Node subclass knows how to render itself via print(int level) and
 * recurses into its own children. This walks the roots, calls those methods,
 * and frames the result with each node's identity — node name/type, node ID and
 * source line — so the output is readable as a tree.
 *
 * The text is returned rather than printed directly so the same rendering can
 * go to the console and to a file in compiler_output/.
 */
public final class TreePrinter {

    private TreePrinter() {
    }

    /** Renders the whole Python AST. */
    public static String renderPythonAst(App app, String sourceName) {
        StringBuilder out = new StringBuilder();
        banner(out, "PYTHON AST - " + sourceName);

        if (app == null || app.nodes == null || app.nodes.isEmpty()) {
            out.append("(empty)").append(nl());
            return out.toString();
        }

        for (int i = 0; i < app.nodes.size(); i++) {
            Node node = app.nodes.get(i);
            out.append(nl());
            out.append("[").append(i + 1).append("/").append(app.nodes.size()).append("] ")
               .append(node.header()).append(nl());
            out.append(divider());
            out.append(node.print(0));
        }
        out.append(nl()).append("Total top-level statements: ").append(app.nodes.size()).append(nl());
        return out.toString();
    }

    /** Renders every parsed template's AST. */
    public static String renderTemplateAsts(Map<String, Template> templates) {
        StringBuilder out = new StringBuilder();
        banner(out, "JINJA / HTML / CSS AST");

        if (templates == null || templates.isEmpty()) {
            out.append("(no templates)").append(nl());
            return out.toString();
        }

        for (Map.Entry<String, Template> entry : templates.entrySet()) {
            Template template = entry.getValue();
            out.append(nl()).append("######### ").append(entry.getKey()).append(" #########").append(nl());

            if (template == null || template.nodes == null || template.nodes.isEmpty()) {
                out.append("(empty)").append(nl());
                continue;
            }
            for (Node node : template.nodes) {
                out.append(nl()).append(node.header()).append(nl());
                out.append(divider());
                out.append(node.print(0));
            }
        }
        return out.toString();
    }

    /** Renders the symbol table. */
    public static String renderSymbolTable(SymbolTable symbolTable) {
        StringBuilder out = new StringBuilder();
        banner(out, "SYMBOL TABLE");
        out.append(symbolTable.render());
        out.append(nl())
           .append("Scopes: ").append(symbolTable.scopeCount())
           .append("   Symbols: ").append(symbolTable.symbolCount())
           .append(nl());
        return out.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSOLE ENCODING
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * The print methods draw the tree with Unicode box characters. A Windows
     * console running a legacy code page cannot encode those and shows "??"
     * instead, which makes the whole dump unreadable.
     *
     * Files are always written as UTF-8; only console output is transliterated,
     * and only when the console genuinely cannot represent the characters.
     */
    public static String forConsole(String text) {
        return consoleHandlesBoxDrawing() ? text : toAscii(text);
    }

    private static boolean consoleHandlesBoxDrawing() {
        try {
            return consoleCharset().newEncoder().canEncode("├─└│");
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Charset consoleCharset() {
        // stdout.encoding is what the JVM actually writes System.out with.
        for (String property : new String[]{"stdout.encoding", "native.encoding", "file.encoding"}) {
            String name = System.getProperty(property);
            if (name == null) continue;
            try {
                return Charset.forName(name);
            } catch (RuntimeException ignored) {
                // Try the next one.
            }
        }
        return Charset.defaultCharset();
    }

    private static String toAscii(String text) {
        return text
                .replace('├', '+')
                .replace('└', '\\')
                .replace('│', '|')
                .replace('─', '-')
                .replace('┌', '+')
                .replace('┐', '+')
                .replace('┘', '+')
                .replace('┼', '+')
                .replace('▶', '>')
                .replace('—', '-')
                .replace('•', '*');
    }

    private static void banner(StringBuilder out, String title) {
        out.append(nl())
           .append("================================================================").append(nl())
           .append(" ").append(title).append(nl())
           .append("================================================================").append(nl());
    }

    private static String divider() {
        return "----------------------------------------------------------------" + nl();
    }

    private static String nl() {
        return System.lineSeparator();
    }
}
