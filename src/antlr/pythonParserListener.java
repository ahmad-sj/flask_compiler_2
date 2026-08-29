// Generated from grammars/pythonParser.g4 by ANTLR 4.13.2
 package antlr; 
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link pythonParser}.
 */
public interface pythonParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link pythonParser#prog}.
	 * @param ctx the parse tree
	 */
	void enterProg(pythonParser.ProgContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#prog}.
	 * @param ctx the parse tree
	 */
	void exitProg(pythonParser.ProgContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#stmt}.
	 * @param ctx the parse tree
	 */
	void enterStmt(pythonParser.StmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#stmt}.
	 * @param ctx the parse tree
	 */
	void exitStmt(pythonParser.StmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#simpleStmts}.
	 * @param ctx the parse tree
	 */
	void enterSimpleStmts(pythonParser.SimpleStmtsContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#simpleStmts}.
	 * @param ctx the parse tree
	 */
	void exitSimpleStmts(pythonParser.SimpleStmtsContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#simpleStmt}.
	 * @param ctx the parse tree
	 */
	void enterSimpleStmt(pythonParser.SimpleStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#simpleStmt}.
	 * @param ctx the parse tree
	 */
	void exitSimpleStmt(pythonParser.SimpleStmtContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleImport}
	 * labeled alternative in {@link pythonParser#importLine}.
	 * @param ctx the parse tree
	 */
	void enterSingleImport(pythonParser.SingleImportContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleImport}
	 * labeled alternative in {@link pythonParser#importLine}.
	 * @param ctx the parse tree
	 */
	void exitSingleImport(pythonParser.SingleImportContext ctx);
	/**
	 * Enter a parse tree produced by the {@code multiImport}
	 * labeled alternative in {@link pythonParser#importLine}.
	 * @param ctx the parse tree
	 */
	void enterMultiImport(pythonParser.MultiImportContext ctx);
	/**
	 * Exit a parse tree produced by the {@code multiImport}
	 * labeled alternative in {@link pythonParser#importLine}.
	 * @param ctx the parse tree
	 */
	void exitMultiImport(pythonParser.MultiImportContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#pass}.
	 * @param ctx the parse tree
	 */
	void enterPass(pythonParser.PassContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#pass}.
	 * @param ctx the parse tree
	 */
	void exitPass(pythonParser.PassContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#id}.
	 * @param ctx the parse tree
	 */
	void enterId(pythonParser.IdContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#id}.
	 * @param ctx the parse tree
	 */
	void exitId(pythonParser.IdContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#name}.
	 * @param ctx the parse tree
	 */
	void enterName(pythonParser.NameContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#name}.
	 * @param ctx the parse tree
	 */
	void exitName(pythonParser.NameContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#assignLine}.
	 * @param ctx the parse tree
	 */
	void enterAssignLine(pythonParser.AssignLineContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#assignLine}.
	 * @param ctx the parse tree
	 */
	void exitAssignLine(pythonParser.AssignLineContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#target}.
	 * @param ctx the parse tree
	 */
	void enterTarget(pythonParser.TargetContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#target}.
	 * @param ctx the parse tree
	 */
	void exitTarget(pythonParser.TargetContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#value}.
	 * @param ctx the parse tree
	 */
	void enterValue(pythonParser.ValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#value}.
	 * @param ctx the parse tree
	 */
	void exitValue(pythonParser.ValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#valueTrailer}.
	 * @param ctx the parse tree
	 */
	void enterValueTrailer(pythonParser.ValueTrailerContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#valueTrailer}.
	 * @param ctx the parse tree
	 */
	void exitValueTrailer(pythonParser.ValueTrailerContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#dotTrailer}.
	 * @param ctx the parse tree
	 */
	void enterDotTrailer(pythonParser.DotTrailerContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#dotTrailer}.
	 * @param ctx the parse tree
	 */
	void exitDotTrailer(pythonParser.DotTrailerContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#squareTrailer}.
	 * @param ctx the parse tree
	 */
	void enterSquareTrailer(pythonParser.SquareTrailerContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#squareTrailer}.
	 * @param ctx the parse tree
	 */
	void exitSquareTrailer(pythonParser.SquareTrailerContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#baseValue}.
	 * @param ctx the parse tree
	 */
	void enterBaseValue(pythonParser.BaseValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#baseValue}.
	 * @param ctx the parse tree
	 */
	void exitBaseValue(pythonParser.BaseValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#parenthedGenExpr}.
	 * @param ctx the parse tree
	 */
	void enterParenthedGenExpr(pythonParser.ParenthedGenExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#parenthedGenExpr}.
	 * @param ctx the parse tree
	 */
	void exitParenthedGenExpr(pythonParser.ParenthedGenExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#parenthedExpr}.
	 * @param ctx the parse tree
	 */
	void enterParenthedExpr(pythonParser.ParenthedExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#parenthedExpr}.
	 * @param ctx the parse tree
	 */
	void exitParenthedExpr(pythonParser.ParenthedExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#tupleExpr}.
	 * @param ctx the parse tree
	 */
	void enterTupleExpr(pythonParser.TupleExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#tupleExpr}.
	 * @param ctx the parse tree
	 */
	void exitTupleExpr(pythonParser.TupleExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#genExpr}.
	 * @param ctx the parse tree
	 */
	void enterGenExpr(pythonParser.GenExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#genExpr}.
	 * @param ctx the parse tree
	 */
	void exitGenExpr(pythonParser.GenExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#callArgs}.
	 * @param ctx the parse tree
	 */
	void enterCallArgs(pythonParser.CallArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#callArgs}.
	 * @param ctx the parse tree
	 */
	void exitCallArgs(pythonParser.CallArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#callList}.
	 * @param ctx the parse tree
	 */
	void enterCallList(pythonParser.CallListContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#callList}.
	 * @param ctx the parse tree
	 */
	void exitCallList(pythonParser.CallListContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#callArg}.
	 * @param ctx the parse tree
	 */
	void enterCallArg(pythonParser.CallArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#callArg}.
	 * @param ctx the parse tree
	 */
	void exitCallArg(pythonParser.CallArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#singleExpr}.
	 * @param ctx the parse tree
	 */
	void enterSingleExpr(pythonParser.SingleExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#singleExpr}.
	 * @param ctx the parse tree
	 */
	void exitSingleExpr(pythonParser.SingleExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#negatedExpr}.
	 * @param ctx the parse tree
	 */
	void enterNegatedExpr(pythonParser.NegatedExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#negatedExpr}.
	 * @param ctx the parse tree
	 */
	void exitNegatedExpr(pythonParser.NegatedExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#callExpr}.
	 * @param ctx the parse tree
	 */
	void enterCallExpr(pythonParser.CallExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#callExpr}.
	 * @param ctx the parse tree
	 */
	void exitCallExpr(pythonParser.CallExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#returnLine}.
	 * @param ctx the parse tree
	 */
	void enterReturnLine(pythonParser.ReturnLineContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#returnLine}.
	 * @param ctx the parse tree
	 */
	void exitReturnLine(pythonParser.ReturnLineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tupleReturnWithoutParens}
	 * labeled alternative in {@link pythonParser#returnExpr}.
	 * @param ctx the parse tree
	 */
	void enterTupleReturnWithoutParens(pythonParser.TupleReturnWithoutParensContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tupleReturnWithoutParens}
	 * labeled alternative in {@link pythonParser#returnExpr}.
	 * @param ctx the parse tree
	 */
	void exitTupleReturnWithoutParens(pythonParser.TupleReturnWithoutParensContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleReturn}
	 * labeled alternative in {@link pythonParser#returnExpr}.
	 * @param ctx the parse tree
	 */
	void enterSingleReturn(pythonParser.SingleReturnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleReturn}
	 * labeled alternative in {@link pythonParser#returnExpr}.
	 * @param ctx the parse tree
	 */
	void exitSingleReturn(pythonParser.SingleReturnContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#exprLine}.
	 * @param ctx the parse tree
	 */
	void enterExprLine(pythonParser.ExprLineContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#exprLine}.
	 * @param ctx the parse tree
	 */
	void exitExprLine(pythonParser.ExprLineContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#ternaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExpr(pythonParser.TernaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#ternaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExpr(pythonParser.TernaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#orExpr}.
	 * @param ctx the parse tree
	 */
	void enterOrExpr(pythonParser.OrExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#orExpr}.
	 * @param ctx the parse tree
	 */
	void exitOrExpr(pythonParser.OrExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void enterAndExpr(pythonParser.AndExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void exitAndExpr(pythonParser.AndExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#equalExpr}.
	 * @param ctx the parse tree
	 */
	void enterEqualExpr(pythonParser.EqualExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#equalExpr}.
	 * @param ctx the parse tree
	 */
	void exitEqualExpr(pythonParser.EqualExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#compareExpr}.
	 * @param ctx the parse tree
	 */
	void enterCompareExpr(pythonParser.CompareExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#compareExpr}.
	 * @param ctx the parse tree
	 */
	void exitCompareExpr(pythonParser.CompareExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#compareOptor}.
	 * @param ctx the parse tree
	 */
	void enterCompareOptor(pythonParser.CompareOptorContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#compareOptor}.
	 * @param ctx the parse tree
	 */
	void exitCompareOptor(pythonParser.CompareOptorContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#addExpr}.
	 * @param ctx the parse tree
	 */
	void enterAddExpr(pythonParser.AddExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#addExpr}.
	 * @param ctx the parse tree
	 */
	void exitAddExpr(pythonParser.AddExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code plusOperator}
	 * labeled alternative in {@link pythonParser#addExprOptor}.
	 * @param ctx the parse tree
	 */
	void enterPlusOperator(pythonParser.PlusOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code plusOperator}
	 * labeled alternative in {@link pythonParser#addExprOptor}.
	 * @param ctx the parse tree
	 */
	void exitPlusOperator(pythonParser.PlusOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code minusOperator}
	 * labeled alternative in {@link pythonParser#addExprOptor}.
	 * @param ctx the parse tree
	 */
	void enterMinusOperator(pythonParser.MinusOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code minusOperator}
	 * labeled alternative in {@link pythonParser#addExprOptor}.
	 * @param ctx the parse tree
	 */
	void exitMinusOperator(pythonParser.MinusOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#mulExpr}.
	 * @param ctx the parse tree
	 */
	void enterMulExpr(pythonParser.MulExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#mulExpr}.
	 * @param ctx the parse tree
	 */
	void exitMulExpr(pythonParser.MulExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#mulOperator}.
	 * @param ctx the parse tree
	 */
	void enterMulOperator(pythonParser.MulOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#mulOperator}.
	 * @param ctx the parse tree
	 */
	void exitMulOperator(pythonParser.MulOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#compoundStmt}.
	 * @param ctx the parse tree
	 */
	void enterCompoundStmt(pythonParser.CompoundStmtContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#compoundStmt}.
	 * @param ctx the parse tree
	 */
	void exitCompoundStmt(pythonParser.CompoundStmtContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#decorator}.
	 * @param ctx the parse tree
	 */
	void enterDecorator(pythonParser.DecoratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#decorator}.
	 * @param ctx the parse tree
	 */
	void exitDecorator(pythonParser.DecoratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#funcArgs}.
	 * @param ctx the parse tree
	 */
	void enterFuncArgs(pythonParser.FuncArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#funcArgs}.
	 * @param ctx the parse tree
	 */
	void exitFuncArgs(pythonParser.FuncArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#argsNames}.
	 * @param ctx the parse tree
	 */
	void enterArgsNames(pythonParser.ArgsNamesContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#argsNames}.
	 * @param ctx the parse tree
	 */
	void exitArgsNames(pythonParser.ArgsNamesContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#func}.
	 * @param ctx the parse tree
	 */
	void enterFunc(pythonParser.FuncContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#func}.
	 * @param ctx the parse tree
	 */
	void exitFunc(pythonParser.FuncContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(pythonParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(pythonParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#ifBlock}.
	 * @param ctx the parse tree
	 */
	void enterIfBlock(pythonParser.IfBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#ifBlock}.
	 * @param ctx the parse tree
	 */
	void exitIfBlock(pythonParser.IfBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#elifBlock}.
	 * @param ctx the parse tree
	 */
	void enterElifBlock(pythonParser.ElifBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#elifBlock}.
	 * @param ctx the parse tree
	 */
	void exitElifBlock(pythonParser.ElifBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#elseBlock}.
	 * @param ctx the parse tree
	 */
	void enterElseBlock(pythonParser.ElseBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#elseBlock}.
	 * @param ctx the parse tree
	 */
	void exitElseBlock(pythonParser.ElseBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#forBlock}.
	 * @param ctx the parse tree
	 */
	void enterForBlock(pythonParser.ForBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#forBlock}.
	 * @param ctx the parse tree
	 */
	void exitForBlock(pythonParser.ForBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#whileBlock}.
	 * @param ctx the parse tree
	 */
	void enterWhileBlock(pythonParser.WhileBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#whileBlock}.
	 * @param ctx the parse tree
	 */
	void exitWhileBlock(pythonParser.WhileBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#listVal}.
	 * @param ctx the parse tree
	 */
	void enterListVal(pythonParser.ListValContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#listVal}.
	 * @param ctx the parse tree
	 */
	void exitListVal(pythonParser.ListValContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#listItem}.
	 * @param ctx the parse tree
	 */
	void enterListItem(pythonParser.ListItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#listItem}.
	 * @param ctx the parse tree
	 */
	void exitListItem(pythonParser.ListItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#listItemSeparator}.
	 * @param ctx the parse tree
	 */
	void enterListItemSeparator(pythonParser.ListItemSeparatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#listItemSeparator}.
	 * @param ctx the parse tree
	 */
	void exitListItemSeparator(pythonParser.ListItemSeparatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#dictVal}.
	 * @param ctx the parse tree
	 */
	void enterDictVal(pythonParser.DictValContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#dictVal}.
	 * @param ctx the parse tree
	 */
	void exitDictVal(pythonParser.DictValContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#dictItem}.
	 * @param ctx the parse tree
	 */
	void enterDictItem(pythonParser.DictItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#dictItem}.
	 * @param ctx the parse tree
	 */
	void exitDictItem(pythonParser.DictItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#dictItemSeparator}.
	 * @param ctx the parse tree
	 */
	void enterDictItemSeparator(pythonParser.DictItemSeparatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#dictItemSeparator}.
	 * @param ctx the parse tree
	 */
	void exitDictItemSeparator(pythonParser.DictItemSeparatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(pythonParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(pythonParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#int}.
	 * @param ctx the parse tree
	 */
	void enterInt(pythonParser.IntContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#int}.
	 * @param ctx the parse tree
	 */
	void exitInt(pythonParser.IntContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#float}.
	 * @param ctx the parse tree
	 */
	void enterFloat(pythonParser.FloatContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#float}.
	 * @param ctx the parse tree
	 */
	void exitFloat(pythonParser.FloatContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#string}.
	 * @param ctx the parse tree
	 */
	void enterString(pythonParser.StringContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#string}.
	 * @param ctx the parse tree
	 */
	void exitString(pythonParser.StringContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#true}.
	 * @param ctx the parse tree
	 */
	void enterTrue(pythonParser.TrueContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#true}.
	 * @param ctx the parse tree
	 */
	void exitTrue(pythonParser.TrueContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#false}.
	 * @param ctx the parse tree
	 */
	void enterFalse(pythonParser.FalseContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#false}.
	 * @param ctx the parse tree
	 */
	void exitFalse(pythonParser.FalseContext ctx);
	/**
	 * Enter a parse tree produced by {@link pythonParser#none}.
	 * @param ctx the parse tree
	 */
	void enterNone(pythonParser.NoneContext ctx);
	/**
	 * Exit a parse tree produced by {@link pythonParser#none}.
	 * @param ctx the parse tree
	 */
	void exitNone(pythonParser.NoneContext ctx);
}