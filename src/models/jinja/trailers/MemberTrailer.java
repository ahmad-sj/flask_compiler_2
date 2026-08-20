package models.jinja.trailers;

import models.Node;

public class MemberTrailer extends Trailer {
    public Node id;

    public MemberTrailer(Node id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "." + this.id.toString();
    }

    @Override
    public String print(int level) {
        String indent = getIndent(level);

        return "member trailer:\n"
                + indent + "├─ line no: " + lineNumber + "\n"
                + indent + "└─ name: " + this.id.print(level)
                ;
    }
}
