// Generated from C:/Users/Admin/Desktop/flask_compiler_2/grammars/pythonParser.g4 by ANTLR 4.13.2
 package antlr; 
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link pythonParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface pythonParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link pythonParser#prog}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProg(pythonParser.ProgContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#progSimple}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgSimple(pythonParser.ProgSimpleContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#progTrivial}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProgTrivial(pythonParser.ProgTrivialContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#stmtList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmtList(pythonParser.StmtListContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#commentLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommentLine(pythonParser.CommentLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#nl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNl(pythonParser.NlContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#stmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStmt(pythonParser.StmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#simpleStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimpleStmt(pythonParser.SimpleStmtContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleImport}
	 * labeled alternative in {@link pythonParser#importLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleImport(pythonParser.SingleImportContext ctx);
	/**
	 * Visit a parse tree produced by the {@code multiImport}
	 * labeled alternative in {@link pythonParser#importLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiImport(pythonParser.MultiImportContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#pass}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPass(pythonParser.PassContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId(pythonParser.IdContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitName(pythonParser.NameContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#assignLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignLine(pythonParser.AssignLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#target}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTarget(pythonParser.TargetContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(pythonParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#valueTrailer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValueTrailer(pythonParser.ValueTrailerContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#dotTrailer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDotTrailer(pythonParser.DotTrailerContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#squareTrailer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSquareTrailer(pythonParser.SquareTrailerContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#baseValue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBaseValue(pythonParser.BaseValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#parenthedGenExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthedGenExpr(pythonParser.ParenthedGenExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#parenthedExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthedExpr(pythonParser.ParenthedExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#tupleExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleExpr(pythonParser.TupleExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#genExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenExpr(pythonParser.GenExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#callArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallArgs(pythonParser.CallArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#callList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallList(pythonParser.CallListContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#callArg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallArg(pythonParser.CallArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#singleExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleExpr(pythonParser.SingleExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#returnLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturnLine(pythonParser.ReturnLineContext ctx);
	/**
	 * Visit a parse tree produced by the {@code tupleReturnWithoutParens}
	 * labeled alternative in {@link pythonParser#returnExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTupleReturnWithoutParens(pythonParser.TupleReturnWithoutParensContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleReturn}
	 * labeled alternative in {@link pythonParser#returnExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleReturn(pythonParser.SingleReturnContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#exprLine}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprLine(pythonParser.ExprLineContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#ternaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryExpr(pythonParser.TernaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#orExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpr(pythonParser.OrExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#andExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpr(pythonParser.AndExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#equalExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualExpr(pythonParser.EqualExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#compareExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompareExpr(pythonParser.CompareExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#addExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddExpr(pythonParser.AddExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code plusOperator}
	 * labeled alternative in {@link pythonParser#addExprOptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlusOperator(pythonParser.PlusOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code minusOperator}
	 * labeled alternative in {@link pythonParser#addExprOptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinusOperator(pythonParser.MinusOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#mulExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulExpr(pythonParser.MulExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#muiltoperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMuiltoperator(pythonParser.MuiltoperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#blockStmt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockStmt(pythonParser.BlockStmtContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#decorator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDecorator(pythonParser.DecoratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#funcArgs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFuncArgs(pythonParser.FuncArgsContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#argsNames}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgsNames(pythonParser.ArgsNamesContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#func}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc(pythonParser.FuncContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(pythonParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#ifBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfBlock(pythonParser.IfBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#elifBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElifBlock(pythonParser.ElifBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#elseBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseBlock(pythonParser.ElseBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#forBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForBlock(pythonParser.ForBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#whileBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileBlock(pythonParser.WhileBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#listVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListVal(pythonParser.ListValContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#listItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListItem(pythonParser.ListItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#listItemSeparator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListItemSeparator(pythonParser.ListItemSeparatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#dictVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDictVal(pythonParser.DictValContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#dictItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDictItem(pythonParser.DictItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#dictItemSeparator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDictItemSeparator(pythonParser.DictItemSeparatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(pythonParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#int}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInt(pythonParser.IntContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#float}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloat(pythonParser.FloatContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString(pythonParser.StringContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#true}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrue(pythonParser.TrueContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#false}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFalse(pythonParser.FalseContext ctx);
	/**
	 * Visit a parse tree produced by {@link pythonParser#none}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNone(pythonParser.NoneContext ctx);
}