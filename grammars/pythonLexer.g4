lexer grammar pythonLexer;

@lexer::header {
package antlr;
import org.antlr.v4.runtime.*;
import java.util.*;
}


tokens { INDENT, DEDENT }

@members {
    private static final int TAB_LENGTH = 8;
    private boolean atStartOfLine = true;

    private Deque<Integer> indents = new ArrayDeque<>();
    private LinkedList<Token> pending = new LinkedList<>();
    private int opened = 0;

    {
       indents.push(0);
    }

  @Override
  public Token nextToken() {

      // 1) إخراج أي توكنات معلقة أولًا
      if (!pending.isEmpty()) {
          Token p = pending.poll();

          if (p.getType() == NEWLINE) {
              atStartOfLine = true;
          } else if (p.getType() != INDENT && p.getType() != DEDENT) {
              atStartOfLine = false;
          }

          return p;
      }

      // 2) اقرأ توكن جديد
      Token t = super.nextToken();

      // 3) EOF
      if (t.getType() == EOF) {
          while (indents.size() > 1) {
              indents.pop();
              pending.add(new CommonToken(DEDENT, ""));
          }
          pending.add(t);
          return pending.poll();
      }

      // 4) تجاهل HIDDEN tokens في بداية السطر
      if (atStartOfLine && t.getChannel() == Token.HIDDEN_CHANNEL) {
          return nextToken();
      }

      // 5) تحديث حالة atStartOfLine
      if (t.getType() == NEWLINE) {
          atStartOfLine = true;
      } else {
          atStartOfLine = false;
      }

      return t;
  }


    private void emitIndentation(String spaces) {
        int indent = countIndent(spaces);
        int prev = indents.peek();

        if (indent > prev) {
            indents.push(indent);
            pending.add(new CommonToken(INDENT, "iiindent"));
        }
        else if (indent < prev) {
            while (indents.peek() > indent) {
                indents.pop();
                pending.add(new CommonToken(DEDENT, "dddedent"));
            }
            if (indents.peek() != indent) {
                throw new RuntimeException("IndentationError: inconsistent indentation");
            }
        }
    }

    private int countIndent(String spaces) {
        int count = 0;
        for (char c : spaces.toCharArray()) {
            count += (c == '\t') ? TAB_LENGTH : 1;
        }
        return count;
    }

    private void openBrace() { opened++; }
    private void closeBrace() { opened--; }
}

// NEWLINE

NEWLINE
    : '\r'? '\n' [ \t]* {
        if (opened == 0) {
            String spaces = getText().replaceAll("[^\t ]", "");

            // 1) أضف NEWLINE أولًا
            pending.add(new CommonToken(NEWLINE, "\n"));

            // 2) أضف INDENT/DEDENT مباشرة إلى pending
            emitIndentation(spaces);

            // 3) في بداية السطر
            atStartOfLine = true;

            // 4) أخبر ANTLR بعدم إخراج هذا token مباشرة
            setChannel(HIDDEN);
        }
    }
;



WS
    : [ \t]+ -> channel(HIDDEN)
    ;






// Keywords

DEF     : 'def';
CLASS   : 'class';
IF      : 'if';
ELIF    : 'elif';
ELSE    : 'else';
FOR     : 'for';
WHILE   : 'while';
RETURN  : 'return';
BREAK   : 'break';
CONTINUE: 'continue';
PASS    : 'pass';
IMPORT  : 'import';
FROM    : 'from';
AS      : 'as';
IN      : 'in';
TRUE    : 'True';
FALSE   : 'False';
NONE    : 'None';
AND     : 'and';
OR      : 'or';
NOT     : 'not';


// Operators

EQUAL           : '=';
NOTEQUAL        : '!=';
EQUALEQUAL      : '==';
LESSTHAN        : '<';
GREATERTHAN     : '>';
LESSOREQUAL     : '<=';
GREATEROREQUAL  : '>=';
PLUS            : '+';
MINUS           : '-';
STAR            : '*';
SLASH           : '/';
PERCENT         : '%';
COLON           : ':';
COMMA           : ',';
DOT             : '.';
AT              : '@';
ARROW           : '->';
SEMICOLON       : ';';


// Brackets (IMPORTANT)

LPAREN   : '(' { openBrace(); };
RPAREN  : ')' { closeBrace(); };
LSB    : '[' { openBrace(); };
RSB   : ']' { closeBrace(); };
LBRACE     : '{' { openBrace(); };
RBRACE   : '}' { closeBrace(); };


// Literals & Identifiers



NAME   : [a-zA-Z_][a-zA-Z0-9_]*;
FLOAT  : [0-9]+ '.' [0-9]*;
INT    : [0-9]+;
STRING
    : '"' (~["\\] | '\\' .)* '"'
    | '\'' (~['\\] | '\\' .)* '\''
    ;


// Comments

COMMENT : '#' ~[\r\n]* -> skip
;

