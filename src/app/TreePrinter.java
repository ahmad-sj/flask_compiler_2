package app;

import models.App;
import models.Node;
import models.Template;
import symbols.SymbolTable;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.Trees;

import java.nio.charset.Charset;
import java.util.List;

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
    //  LEXER TOKENS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Renders the token stream: what the lexer produced, before any parsing.
     *
     * Shown as index, line:column, symbolic token name and text. Newlines and
     * tabs are escaped so one token stays on one line, and the synthetic
     * INDENT/DEDENT tokens the Python lexer inserts are marked, since they are
     * the whole reason an indentation-sensitive language can be parsed by a
     * context-free grammar.
     */
    public static String renderTokens(CommonTokenStream tokens, Vocabulary vocabulary,
                                      String sourceName) {
        StringBuilder out = new StringBuilder();
        banner(out, "LEXER TOKENS - " + sourceName);

        tokens.fill();
        List<Token> list = tokens.getTokens();

        out.append(String.format("%-6s %-10s %-26s %s", "#", "line:col", "token", "text")).append(nl());
        out.append(divider());

        int shown = 0;
        for (Token token : list) {
            if (token.getType() == Token.EOF) continue;
            String name = vocabulary.getSymbolicName(token.getType());
            if (name == null) name = vocabulary.getDisplayName(token.getType());

            String text = token.getText()
                    .replace("\\", "\\\\").replace("\r", "\\r")
                    .replace("\n", "\\n").replace("\t", "\\t");
            if (text.length() > 40) text = text.substring(0, 37) + "...";

            String marker = "";
            if (name != null && name.contains("INDENT")) marker = "   <-- synthetic";

            out.append(String.format("%-6d %-10s %-26s '%s'%s",
                    shown++, token.getLine() + ":" + token.getCharPositionInLine(),
                    name, text, marker)).append(nl());
        }
        out.append(nl()).append("Total tokens: ").append(shown).append(nl());
        return out.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PARSE TREE (concrete syntax tree)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Renders the ANTLR parse tree: every grammar rule the parser matched.
     *
     * This is the concrete syntax tree, distinct from the AST. It contains one
     * node per grammar rule and per token, including punctuation the AST throws
     * away, so it shows how the grammar actually derived the input.
     */
    public static String renderParseTree(ParseTree tree, Parser parser, String sourceName) {
        StringBuilder out = new StringBuilder();
        banner(out, "PARSE TREE - " + sourceName);
        if (tree == null) {
            out.append("(no parse tree)").append(nl());
            return out.toString();
        }
        int[] nodeCount = {0};
        appendParseNode(out, tree, parser, 0, nodeCount);
        out.append(nl()).append("Total parse-tree nodes: ").append(nodeCount[0]).append(nl());
        return out.toString();
    }

    private static void appendParseNode(StringBuilder out, ParseTree tree, Parser parser,
                                        int depth, int[] count) {
        count[0]++;

        for (int i = 0; i < depth; i++) out.append("  ");

        String text = Trees.getNodeText(tree, parser);
        boolean isRule = tree instanceof ParserRuleContext;

        if (isRule) {
            // A grammar rule: name it and show where it starts.
            ParserRuleContext rule = (ParserRuleContext) tree;
            out.append(text);
            if (rule.getStart() != null) out.append("   (line ").append(rule.getStart().getLine()).append(')');
        } else {
            // A terminal: show the matched text, escaped onto one line.
            String escaped = text.replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
            if (escaped.length() > 40) escaped = escaped.substring(0, 37) + "...";
            out.append("'").append(escaped).append("'");
        }
        out.append(nl());

        for (int i = 0; i < tree.getChildCount(); i++) {
            appendParseNode(out, tree.getChild(i), parser, depth + 1, count);
        }
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
