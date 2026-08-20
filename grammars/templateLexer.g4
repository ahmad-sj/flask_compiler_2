lexer grammar templateLexer;

@header{
package antlr;
}

import templateFragments;

J_EXPR_START: '{{' -> pushMode(EXPRESSION_MODE);
J_STMNT_START: '{%' -> pushMode(J_STMNT_MODE);
J_COMMENT: '{#' (~[#])* '#}' -> skip;

DOCTYPE: '<!' [dD][oO][cC][tT][yY][pP][eE] ~[>]* '>';
HTML_COMMENT: '<!--' .*? '-->' -> skip;
CLOSE_TAG_START: '</' -> pushMode(END_TAG_MODE);
START_TAG_OPEN: '<' -> pushMode(START_TAG_MODE);

WS: [ \t\r\n]+ -> skip;
NORMAL_TEXT: ~[<>{}\t\r\n]+;

// ==========================================================================================

mode J_STMNT_MODE;
   IF: 'if' -> pushMode(EXPRESSION_MODE);
   ELIF: 'elif' -> pushMode(EXPRESSION_MODE);
   FOR: 'for' -> pushMode(EXPRESSION_MODE);
   EXTENDS: 'extends' -> pushMode(EXPRESSION_MODE);
   BLOCK: 'block' -> pushMode(EXPRESSION_MODE);
   SET: 'set' -> pushMode(EXPRESSION_MODE);
   ELSE: 'else';
   ENDIF: 'endif';
   ENDFOR: 'endfor';
   ENDBLOCK: 'endblock';
   J_STMNT_END: '%}' -> popMode;
   J_STMNT_WS: [ \t\r\n] -> skip;

mode EXPRESSION_MODE;
    MUL: '*';
    PLUS: '+';
    MINUS: '-';
    DIV: '/';
    FLOORDIV: '//';
    MOD: '%';
    POW: '**';
    AND: 'and';
    OR: 'or';
    NOT: 'not';
    IS: 'is';
    EQ: '==';
    NEQ: '!=';
    GT: '>';
    GE: '>=';
    LT: '<';
    LE: '<=';
    IN: 'in';
    QMARK: '?';
    ELVIS: '??';
    ASSIGN: '=';
    TILDE: '~';
    PIPELINE: '|';
    COMMA: ',';
    DOT: '.';
    COLON: ':';
    LPAREN: '(';
    RPAREN: ')';
    LSB: '[';
    RSB: ']';
    LBRACE: LBRACE_FRAG;
    RBRACE: RBRACE_FRAG;
    FLOAT: FLOAT_FRAG;
    INT: INT_FRAG;
    STRING: STRING_FRAG;
    ID: ID_FRAG;
    J_EXPR_END: '}}' -> popMode;
    J_EXPR_STMNT_END: '%}' -> popMode, popMode;
    EXPRESSION_MODE_WS: [ \t\r\n]+ -> skip;

// ==========================================================================================

//HTML Modes
mode START_TAG_MODE;
   STYLE_TAG_START_NAME: 'style' -> pushMode(STYLE_START_TAG_MODE);
   START_TAG_NAME: HTML_TAG_NAME -> pushMode(INSIDE_START_TAG_MODE);
   START_TAG_WS: [ \t\r\n]+ -> skip;

mode END_TAG_MODE;
   END_TAG_NAME: HTML_TAG_NAME;
   END_TAG_CLOSE: RAB_FRAG -> popMode;
   END_TAG_MODE_WS: [ \t\r\n]+ -> skip;

mode STYLE_START_TAG_MODE;
   STYLE_TAG_START_CLOSE: RAB_FRAG -> pushMode(CSS_BLK);
   STYLE_START_TAG_MODE_WS: [ \t\r\n]+ -> skip;

mode STYLE_END_TAG_MODE;
   STYLE_END_TAG_NAME: 'style';
   STYLE_END_TAG_CLOSE: RAB_FRAG -> mode(DEFAULT_MODE);
   STYLE_END_TAG_MODE_WS: [ \t\r\n]+ -> skip;

mode INSIDE_START_TAG_MODE;
    STYLE_ATTR: 'style' -> pushMode(CSS_INLINE);
    ATTR_NAME: [a-zA-Z][a-zA-Z-]*;
    ATTR_EQ: ASSIGN_FRAG -> pushMode(ATTR_VAL);
    INSIDE_START_TAG_J_EXPR_OPEN: '{{' -> pushMode(EXPRESSION_MODE);
    START_TAG_CLOSE: '>' -> popMode, popMode; // also add self closing tag end
    SELF_CLOSING_TAG_CLOSE: '/>' -> popMode, popMode; // also add self closing tag end
    INSIDE_START_TAG_MODE_WS: [ \t\r\n]+ -> skip;

mode ATTR_VAL;
    ATTR_DQUOTE_START: '"' -> pushMode(ATTR_VAL_QOUTED);
    ATTR_VALUE_UNQUOTED: ~[ \t\r\n>{}"'=/]+ -> popMode;
    ATTR_VAL_WS: [ \t\r\n]+ -> skip;

mode ATTR_VAL_QOUTED;
    ATTR_VAL_J_EXPR_START: '{{' -> pushMode(EXPRESSION_MODE);
    ATTR_VAL_TEXT: [a-zA-Z0-9-=.!?:;'(),/#+@%&*_~|]+;
    ATTR_DQUOTE_END: '"' -> popMode, popMode;
    ATTR_VAL_QOUTED_WS: [ \t\r\n,]+ -> skip;

// ==========================================================================================

//CSS Modes
mode CSS_BLK;
   CLOSE_STYLE_START: '</' -> mode(STYLE_END_TAG_MODE);
   CSS_SEL_ID: HASH CSS_ID;
   CSS_SEL_CLASS: DOT_FRAG [a-zA-Z\-_] [a-zA-Z0-9\-_]*;
   CSS_SEL_ELEM: HTML_TAG_NAME;
   CSS_SEL_STATE: COLON_FRAG CSS_STATE;
   CSS_SEL_COMMA: COMMA_FRAG;
   CSS_LBRACE: LBRACE_FRAG -> pushMode(CSS_BLK_PROP);
   STYLE_EXIT_RAB: RAB_FRAG -> popMode;
   CSS_WS: [ \t\r\n]+ -> skip;

mode CSS_INLINE;
   CSS_INLINE_EQ: ASSIGN_FRAG;
   CSS_INLINE_DQUOT_START: DQUOT_FRAG -> pushMode(CSS_INLINE_PROP);
   CSS_INLINE_WS: [ \t\r\n]+ -> skip;

mode CSS_BLK_PROP;
   BLK_PROP_NAME: CSS_PROP_NAME;
   BLK_COLON: COLON_FRAG -> pushMode(CSS_PROP_VALUES);
   BLK_RBRACE: RBRACE_FRAG -> popMode;
   BLK_WS: [ \t\r\n]+ -> skip;

mode CSS_INLINE_PROP;
   CSS_INLINE_PROP_NAME: CSS_PROP_NAME;
   CSS_INLINE_PROP_COLON: COLON_FRAG -> pushMode(CSS_PROP_VALUES);
   CSS_INLINE_PROP_DQUOT_END: DQUOT_FRAG -> popMode, popMode;
   CSS_INLINE_PROP_WS: [ \t\r\n]+ -> skip;

mode CSS_PROP_VALUES;
   CSS_PROP_VAL: [a-zA-Z0-9#%(),.-]+;
   CSS_PROP_SEMICOLON: SEMICOLON_FRAG -> popMode;
   CSS_PROP_VALUES_WS: [ \t\r\n]+ -> skip;
