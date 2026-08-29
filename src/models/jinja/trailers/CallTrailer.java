package models.jinja.trailers;

import models.Node;

public class CallTrailer extends Trailer {
    public Node argList;

    public CallTrailer(Node argList) {
        this.argList = argList;
    }

    @Override
    public String toString() {
        return (argList == null ? "" : "(" + argList.toString() + ")");
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return header() + "\n" +
                (argList == null
                        ? indent + "└─ line no: " + lineNumber + "\n"
                        : indent + "├─ line no: " + lineNumber + "\n"
                        + indent + "└─ arg list: " + argList.print(level)
                );
    }
}
