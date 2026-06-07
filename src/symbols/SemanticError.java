package symbols;

public class SemanticError {

    public enum ErrorType {
        UNDEFINED_VARIABLE,
        TYPE_ERROR,
        SCOPE_ERROR,
        TYPE_MISMATCH,
        MISSING_FLASK_VARIABLE
    }

    public final ErrorType errorType;
    public final int line;
    public final String message;
    public final String symbolName;

    public SemanticError(ErrorType errorType, int line, String symbolName, String message) {
        this.errorType = errorType;
        this.line = line;
        this.symbolName = symbolName;
        this.message = message;
    }

    @Override
    public String toString() {
        return "[" + errorType + "] line " + line + " | '" + symbolName + "' — " + message;
    }
}
