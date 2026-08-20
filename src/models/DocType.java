package models;

/**
 * The document type declaration, e.g. {@code <!DOCTYPE html>}.
 *
 * The lexer used to discard this token outright, so it never reached the AST
 * and every generated page was emitted without one, putting browsers into
 * quirks mode. It is now a real node that renders like any other.
 */
public class DocType extends Node {

    public String declaration;

    public DocType(String declaration) {
        this.declaration = declaration;
    }

    @Override
    public String toString() {
        return declaration;
    }

    @Override
    public String print(int level) {
        return "doctype: " + declaration + "\n";
    }
}
