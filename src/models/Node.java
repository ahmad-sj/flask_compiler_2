package models;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for every AST node.
 *
 * Each node carries the three pieces of identity the project requires:
 *   - a node name/type   (defaults to the concrete class name)
 *   - a node ID (number) (unique and stable for the life of the run)
 *   - a source line number
 *
 * Subclasses override print(int) to render themselves; NodeBody and the block
 * nodes recurse into their children, so printing the root prints the tree.
 */
public abstract class Node {

    /** Hands out node IDs in construction order, starting at 1. */
    private static final AtomicInteger ID_SEQUENCE = new AtomicInteger(0);

    protected final int nodeId;
    protected String nodeName;
    protected int lineNumber;

    protected Node() {
        this.nodeId = ID_SEQUENCE.incrementAndGet();
        // Sensible default so every node has a type even when the visitor does
        // not set one explicitly; setNodeName overrides it where it is called.
        this.nodeName = getClass().getSimpleName();
    }

    /** Resets the ID sequence. Only for tests that assert on specific IDs. */
    public static void resetIdSequence() {
        ID_SEQUENCE.set(0);
    }

    public int getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /** "#12 assign line (line 7)" - the identity header used by the tree printer. */
    public String header() {
        return "#" + nodeId + " " + nodeName
                + (lineNumber > 0 ? " (line " + lineNumber + ")" : "");
    }

    public String print(int level) {
        return "################## method print is not overrided in class: " + nodeName + ", indent level: " + level + " ##################\n";
    }

    /**
     * Indentation for one nesting level.
     *
     * Three spaces, matching the width of the "|- " branch markers so children
     * line up under their parent's text. This used to emit three backticks per
     * level, which at any real depth produced runs like ``````````````` and
     * made the printed tree read as broken Markdown.
     */
    public String getIndent(int level) {
        StringBuilder indent = new StringBuilder();

        for (int i = 0; i < level; i++) {
            indent.append("   ");
        }

        return indent.toString();
    }
}
