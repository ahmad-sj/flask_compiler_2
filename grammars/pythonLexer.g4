lexer grammar pythonLexer;

@lexer::header {
package antlr;
import org.antlr.v4.runtime.*;
import java.util.*;
}


tokens { INDENT, DEDENT }

options {
    superClass = Python3LexerBase;
}

// NEWLINE
NEWLINE: ({this.atStartOfInput()}? SPACES | ( '\r'? '\n' | '\r' | '\f') SPACES?) {this.onNewLine();};

fragment SPACES: [ \t]+;

fragment COMMENT: '#' ~[\r\n\f]*;

fragment LINE_JOINING: '\\' SPACES? ( '\r'? '\n' | '\r' | '\f');

SKIP_: ( SPACES | COMMENT | LINE_JOINING) -> skip;


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
// Two-character operators come first so maximal munch cannot split '**' into
// two STARs or '//' into two SLASHes.
DOUBLESTAR      : '**';
DOUBLESLASH     : '//';
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

LPAREN   : '(' { this.openBrace(); };
RPAREN  : ')' { this.closeBrace(); };
LSB    : '[' { this.openBrace(); };
RSB   : ']' { this.closeBrace(); };
LBRACE     : '{' { this.openBrace(); };
RBRACE   : '}' { this.closeBrace(); };


// Literals & Identifiers



NAME   : [a-zA-Z_][a-zA-Z0-9_]*;
FLOAT  : [0-9]+ '.' [0-9]*;
INT    : [0-9]+;
STRING
    : '"' (~["\\] | '\\' .)* '"'
    | '\'' (~['\\] | '\\' .)* '\''
    ;


