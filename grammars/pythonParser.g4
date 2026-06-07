
parser grammar pythonParser;


options { tokenVocab=pythonLexer; }

@header{ package antlr; }

prog
    : (NEWLINE | stmt)* EOF
    ;

stmt
    : simpleStmts        //finish
    | compoundStmt
    ;

simpleStmts
    : simpleStmt (SEMICOLON simpleStmt)* SEMICOLON? NEWLINE
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
    : LPAREN callList? RPAREN
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
    : negatedExpr
    | value
    ;

negatedExpr
    : NOT singleExpr;

callExpr
    : id LPAREN callList? RPAREN;

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
    : addExpr (compareOptor addExpr)*        //done
    ;

compareOptor
    : (LESSTHAN | GREATERTHAN | LESSOREQUAL | GREATEROREQUAL);

addExpr                                 // done
    : mulExpr (addExprOptor mulExpr)*
    ;

addExprOptor
    : PLUS          #plusOperator       // done
    | MINUS         #minusOperator      // done
    ;

mulExpr
    : singleExpr (mulOperator singleExpr)* //done
    ;

mulOperator
    : (STAR | SLASH | PERCENT);               //done

// Block statements
compoundStmt
    : func          //finish
    | ifBlock       //finish
    | forBlock      //finish
    | whileBlock    //done
    ;

decorator
    : AT name callArgs? NEWLINE             //finish
    ;

funcArgs
    : LPAREN argsNames? RPAREN           //finish
    ;

argsNames
    : NAME (COMMA NAME)*                        //finish
    ;

func
    : (decorator)? DEF NAME funcArgs COLON block        //finish
;

block
    : NEWLINE INDENT stmt+ DEDENT               //finish
    ;


ifBlock                             //finish
    : IF ternaryExpr COLON block
      (elifBlock)*
      (elseBlock)?
    ;

elifBlock
    : ELIF ternaryExpr COLON block
    ;

elseBlock
    : ELSE COLON block
    ;

forBlock
    : FOR NAME IN ternaryExpr COLON block       //done
    ;

whileBlock
    : WHILE ternaryExpr COLON block                 //done
    ;

// Lists
listVal                 // done
    : LSB
      listItem? (listItemSeparator listItem)* listItemSeparator?
      RSB
    ;

listItem                // not necessary
    : ternaryExpr
    ;

listItemSeparator       // not necessary
    : COMMA
    ;

// Dictionaries
dictVal                 // done
    : LBRACE
      dictItem? (dictItemSeparator dictItem)* dictItemSeparator?
      RBRACE
    ;

dictItem                // done
    : literal COLON ternaryExpr
    ;

dictItemSeparator       // not necessary
    : COMMA
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
