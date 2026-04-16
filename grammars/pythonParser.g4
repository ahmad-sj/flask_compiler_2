
parser grammar pythonParser;


options { tokenVocab=pythonLexer; }

@header{ package antlr; }

prog
    : progSimple EOF
    | progTrivial EOF
    ;

progSimple :(NEWLINE* INDENT? stmt  DEDENT? NEWLINE* DEDENT? )* NEWLINE* ;  //finish

progTrivial : commentLine NEWLINE* ;        //finish

stmtList
    : (nl* stmt)+ nl* ;                 //finish

commentLine
    : COMMENT        //finish
    ;

nl
    : (NEWLINE)+        //finish
    ;

stmt
    : simpleStmt        //finish
    | blockStmt
    | commentLine       //finish
    ;

simpleStmt       // not necessary if all children visit methods are implemented
    : importLine   // finish
    | assignLine  // finish
    | returnLine // finish
    | exprLine   // finish
    | pass       // finish
    ;

importLine
    : IMPORT name (AS NAME)?                             #singleImport  //  finish
    | FROM name IMPORT NAME (COMMA NAME)*                   #multiImport   // finish
    ;


pass: PASS;     // finish

id: NAME;                  // finish

name                       // finish
    : id dotTrailer*
    ;

assignLine                 // finish
    : target EQUAL ternaryExpr
    ;

target                  // not necessary if all children visit methods are implemented
    : id                // done
    | value             // done
    ;

value                   // done
    : baseValue valueTrailer*
    ;

valueTrailer                // not necessary if all children visit methods are implemented
    : dotTrailer            // done
    | squareTrailer         // done
    | callArgs              // done
    ;

dotTrailer: DOT NAME;       // done

squareTrailer: LSB ternaryExpr RSB;     // done

baseValue                   // not necessary if all children visit methods are implemented
    : id                    // done
    | literal
    | tupleExpr
    | parenthedExpr
    | parenthedGenExpr
    ;

parenthedGenExpr: LPAREN genExpr RPAREN;

parenthedExpr: LPAREN ternaryExpr RPAREN;

tupleExpr
    : LPAREN ternaryExpr COMMA ternaryExpr (COMMA ternaryExpr)* COMMA? RPAREN
    ;

genExpr
    : value FOR NAME IN ternaryExpr (IF ternaryExpr)?
    ;


callArgs                // finish
    : LPAREN callList RPAREN
    ;

callList                // finish
    : callArg (COMMA callArg)* COMMA?
    ;

callArg                     //finish
    : NAME EQUAL ternaryExpr
    | ternaryExpr
    ;

// callExpr: دعم function calls مع generator expressions
singleExpr
    : NOT singleExpr
    | value
    | id LPAREN (callArg (COMMA callArg)*)? RPAREN
    ;

returnLine
    :     RETURN  returnExpr?   ;           //finish

returnExpr
    : ternaryExpr (COMMA ternaryExpr)+    # tupleReturnWithoutParens      //done
    | ternaryExpr                         # singleReturn                  //done
    ;

exprLine                // finish
    : ternaryExpr
    ;

ternaryExpr
    : orExpr (IF orExpr ELSE ternaryExpr)?         //done
    ;

orExpr                  // done
    : andExpr (OR andExpr)*
    ;

andExpr                 // done
    : equalExpr (AND equalExpr)*
    ;

equalExpr
    : compareExpr ((EQUALEQUAL | NOTEQUAL) compareExpr)*            //done
    ;

compareExpr
    : addExpr ((LESSTHAN | GREATERTHAN | LESSOREQUAL | GREATEROREQUAL) addExpr)*        //done
    ;

addExpr                                 // done
    : mulExpr (addExprOptor mulExpr)*
    ;

addExprOptor
    : PLUS          #plusOperator       // done
    | MINUS         #minusOperator      // done
    ;

mulExpr
    : singleExpr (muiltoperator singleExpr)* //done
    ;

muiltoperator:
(STAR | SLASH | PERCENT);               //done

// Block statements
blockStmt
    : func          //finish
    | ifBlock       //finish
    | forBlock      //finish
    | whileBlock    //done
    ;

decorator
    : AT name callArgs?             //finish
    ;

funcArgs
    : LPAREN argsNames? RPAREN           //finish
    ;

argsNames
    : NAME (COMMA NAME)*                        //finish
    ;

func
    : (decorator  nl)? DEF NAME funcArgs COLON nl block?        //finish
;

block
    : INDENT stmtList DEDENT               //finish
    ;


ifBlock                             //finish
    : IF ternaryExpr COLON nl* block
      (elifBlock)*
      (elseBlock)?
    ;

elifBlock
    : ELIF ternaryExpr COLON nl* block
    ;

elseBlock
    : ELSE COLON nl* block
    ;

forBlock
    : FOR NAME IN ternaryExpr COLON nl block       //done
    ;

whileBlock
    : WHILE ternaryExpr COLON nl block                 //done
    ;

// Lists
listVal                 // done
    : LSB
      (NEWLINE | WS)*
      listItem? (listItemSeparator listItem)* listItemSeparator?
      (NEWLINE | WS)*
      RSB
    ;

listItem                // not necessary
    : ternaryExpr (NEWLINE | WS)*
    ;

listItemSeparator       // not necessary
    : COMMA (NEWLINE | WS)*
    ;

// Dictionaries
dictVal                 // done
    : LBRACE
      (NEWLINE | WS)*
      dictItem? (dictItemSeparator dictItem)* dictItemSeparator?
      (NEWLINE | WS)*
      RBRACE
    ;

dictItem                // done
    : literal COLON ternaryExpr (NEWLINE | WS)*
    ;

dictItemSeparator       // not necessary
    : COMMA (NEWLINE | WS)*
    ;

literal
    : int           // done
    | float         // done
    | string        // done
    | true          // done
    | false         // done
    | none          // done
    | listVal       // done
    | dictVal       // done
    ;

int: INT;
float: FLOAT;
string: STRING;
true: TRUE;
false: FALSE;
none: NONE;
