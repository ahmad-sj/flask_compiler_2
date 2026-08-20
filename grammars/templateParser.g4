parser grammar templateParser;

options {tokenVocab=templateLexer;}

@header{ package antlr; }

template
    : DOCTYPE? extendsBlock? (htmlElement | jinjaElement)* EOF
    ;

jinjaElement
    : inheritBlock
    | ifBlock
    | forBlock
    | jinjaExpression
    | setStatement
    ;

templateText
    : NORMAL_TEXT
    ;

// ============================================================

setStatement
    : J_STMNT_START SET ID ASSIGN expression J_EXPR_STMNT_END;

// ============================================================

// conditional blocks
ifBlock
    : ifStatmentStart ifBody? ifStatmentEnd
    ;

ifStatmentStart
    : J_STMNT_START IF expression J_EXPR_STMNT_END
    ;

ifBody
    : ifBodyElem+ elseBlock?
    | elseBlock
    ;

ifBodyElem
    : ifBlock
    | elifBlock
    | forBlock
    | jinjaExpression
    | htmlElement
    | templateText
    | setStatement
    ;

ifStatmentEnd
    : J_STMNT_START ENDIF J_STMNT_END
    ;

elifBlock
    : J_STMNT_START ELIF expression J_EXPR_STMNT_END subBlock*
    ;

elseBlock
    : J_STMNT_START ELSE J_STMNT_END subBlock*
    ;

// body for: for loop, elif, else, inherit blocks.
subBlock
    : ifBlock
    | forBlock
    | jinjaExpression
    | htmlElement
    | templateText
    | setStatement
    ;

// ============================================================

forBlock
    : forStartStatement forBody? forEndStatement
    ;

forStartStatement
    : J_STMNT_START FOR ID (COMMA ID)* IN expression J_EXPR_STMNT_END
    ;

forEndStatement
    : J_STMNT_START ENDFOR J_STMNT_END
    ;

forBody
    : subBlock+ elseBlock?
    ;

// ============================================================

extendsBlock
    : J_STMNT_START EXTENDS STRING J_EXPR_STMNT_END
    ;

// ============================================================

inheritBlock
    : inheritBlockStart inheritBlockBody? inheritBlockEnd
    ;

inheritBlockBody: (subBlock | inheritBlock)+;

inheritBlockStart
    : J_STMNT_START BLOCK ID J_EXPR_STMNT_END
    ;

inheritBlockEnd
    : J_STMNT_START ENDBLOCK J_STMNT_END
    ;

// ============================================================

jinjaExpression
    : jinjaExprStart expression jinjaExprEnd
    ;

jinjaExprStart
    : J_EXPR_START
    | INSIDE_START_TAG_J_EXPR_OPEN
    ;

jinjaExprEnd
    : J_EXPR_END
    ;

expression
    : orExpr ternaryExt?  // ternary ?:
    | defaultExpr
    ;

ternaryExt
    : QMARK expression COLON expression;

defaultExpr
    : orExpr ELVIS expression;

orExpr
    : andExpr (OR andExpr)*
    ;

andExpr
    : notExpr (AND notExpr)*
    ;

notExpr
    : NOT notExpr
    | compareExpr
    ;

compareExpr
    : concatExpr (IS (NOT)? ID)?                #isExpr
    | pipeExpr comparisonOperator pipeExpr      #compExpr
    | pipeExpr (IN pipeExpr)?                   #inExpr
    ;

comparisonOperator
    : EQ        #equalOperator
    | NEQ       #notEqualOperator
    | LT        #lessThanOperator
    | GT        #greaterThanOperator
    | LE        #lessOrEqualOperator
    | GE        #greaterOrEqualOperator
    ;

pipeExpr
    : concatExpr filter*
    ;

filter
    : PIPELINE ID (LPAREN argumentList? RPAREN)?;

argumentList
    : argument (COMMA argument)*
    ;

argument
    : expression
    | ID ASSIGN expression
    ;

concatExpr
    : addExpr (TILDE addExpr)*
    ;

addExpr
    : mulExpr (addExprOptor mulExpr)*
    ;

addExprOptor
    : PLUS          #plusOperator
    | MINUS         #minusOperator
    ;

mulExpr
    : unaryExpr (mulExprOptor unaryExpr)*
    ;

mulExprOptor
    : MUL           #mulOperator
    | DIV           #divOperator
    | FLOORDIV      #floorDivOperator
    | MOD           #modOperator
    ;

unaryExpr
    : (PLUS | MINUS) unaryExpr
    | powExpr
    ;

powExpr
    : primary (POW unaryExpr)?
    ;

atom
     : ID                                               #id
     | INT                                              #int
     | FLOAT                                            #float
     | STRING                                           #string
     | LPAREN expression RPAREN                         #parenthedExpr
     | LSB (expression (COMMA expression)*)? RSB        #list
     | LBRACE (pair (COMMA pair)*)? RBRACE              #dict
     ;

pair
    : expression COLON expression
    ;

 primary
     : atom trailer*
     ;

trailer
    : DOT ID                            #memberTrailer
    | LSB expression RSB                #subTrailer
    | LPAREN argumentList? RPAREN       #callTrailer
    ;

// =====================================================================================

htmlElement
    : htmlRegularElement
    | htmlSelfClosingElement
    ;

htmlRegularElement
    : htmlStartTag htmlElementBody? htmlEndTag;

htmlStartTag
    : START_TAG_OPEN START_TAG_NAME htmlTagAttr* START_TAG_CLOSE;

htmlElementBody
    : (htmlElement | htmlStyleElem | jinjaElement | templateText)+
    ;

htmlEndTag
    : CLOSE_TAG_START END_TAG_NAME END_TAG_CLOSE;

htmlSelfClosingElement
    : htmlSelfClosingTag;

htmlSelfClosingTag
    : START_TAG_OPEN START_TAG_NAME htmlTagAttr* SELF_CLOSING_TAG_CLOSE;

htmlTagAttr
    : styleAttr
    | booleanAttr
    | attrWithQuotedVal
    | attrWithUnquotedVal
    | jinjaExpression
    ;

booleanAttr
    : ATTR_NAME
    ;

attrWithUnquotedVal
    : ATTR_NAME ATTR_EQ ATTR_VALUE_UNQUOTED;

attrWithQuotedVal
    : ATTR_NAME ATTR_EQ ATTR_DQUOTE_START quotedValElem* ATTR_DQUOTE_END;

quotedValElem
    : ATTR_VAL_TEXT
    | jinjaAttrVal
    ;

jinjaAttrVal
    : ATTR_VAL_J_EXPR_START expression J_EXPR_END;

styleAttr
    : STYLE_ATTR CSS_INLINE_EQ CSS_INLINE_DQUOT_START inlineStyleProp* CSS_INLINE_PROP_DQUOT_END;

inlineStyleProp
    : CSS_INLINE_PROP_NAME CSS_INLINE_PROP_COLON CSS_PROP_VAL+ CSS_PROP_SEMICOLON;

htmlStyleElem
    : htmlStyleElemOpenTag cssBlock* htmlStyleElemCloseTag;

htmlStyleElemOpenTag
    : START_TAG_OPEN STYLE_TAG_START_NAME STYLE_TAG_START_CLOSE;

htmlStyleElemCloseTag
    : CLOSE_STYLE_START STYLE_END_TAG_NAME STYLE_END_TAG_CLOSE;

// =====================================================================================

// CSS rules
cssBlock
    : selectorList CSS_LBRACE cssProp* BLK_RBRACE;

selectorList
    : selector                                #singleSelector
    | selector+                               #descendentSelector
    | selector (CSS_SEL_COMMA selector)+      #groupSelector
    ;

selector
    : simpleSelector
    | pseudoClassSelector
    ;

simpleSelector
    : CSS_SEL_ID        #idSelector
    | CSS_SEL_CLASS     #classSelector
    | CSS_SEL_ELEM      #elementSelector
    ;

pseudoClassSelector
    : simpleSelector CSS_SEL_STATE;

cssProp: BLK_PROP_NAME BLK_COLON CSS_PROP_VAL+ CSS_PROP_SEMICOLON;
