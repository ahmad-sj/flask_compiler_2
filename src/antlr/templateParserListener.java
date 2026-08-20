// Generated from templateParser.g4 by ANTLR 4.13.2
 package antlr; 
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link templateParser}.
 */
public interface templateParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link templateParser#template}.
	 * @param ctx the parse tree
	 */
	void enterTemplate(templateParser.TemplateContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#template}.
	 * @param ctx the parse tree
	 */
	void exitTemplate(templateParser.TemplateContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#jinjaElement}.
	 * @param ctx the parse tree
	 */
	void enterJinjaElement(templateParser.JinjaElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#jinjaElement}.
	 * @param ctx the parse tree
	 */
	void exitJinjaElement(templateParser.JinjaElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#templateText}.
	 * @param ctx the parse tree
	 */
	void enterTemplateText(templateParser.TemplateTextContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#templateText}.
	 * @param ctx the parse tree
	 */
	void exitTemplateText(templateParser.TemplateTextContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetStatement(templateParser.SetStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetStatement(templateParser.SetStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#ifBlock}.
	 * @param ctx the parse tree
	 */
	void enterIfBlock(templateParser.IfBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#ifBlock}.
	 * @param ctx the parse tree
	 */
	void exitIfBlock(templateParser.IfBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#ifStatmentStart}.
	 * @param ctx the parse tree
	 */
	void enterIfStatmentStart(templateParser.IfStatmentStartContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#ifStatmentStart}.
	 * @param ctx the parse tree
	 */
	void exitIfStatmentStart(templateParser.IfStatmentStartContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#ifBody}.
	 * @param ctx the parse tree
	 */
	void enterIfBody(templateParser.IfBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#ifBody}.
	 * @param ctx the parse tree
	 */
	void exitIfBody(templateParser.IfBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#ifBodyElem}.
	 * @param ctx the parse tree
	 */
	void enterIfBodyElem(templateParser.IfBodyElemContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#ifBodyElem}.
	 * @param ctx the parse tree
	 */
	void exitIfBodyElem(templateParser.IfBodyElemContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#ifStatmentEnd}.
	 * @param ctx the parse tree
	 */
	void enterIfStatmentEnd(templateParser.IfStatmentEndContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#ifStatmentEnd}.
	 * @param ctx the parse tree
	 */
	void exitIfStatmentEnd(templateParser.IfStatmentEndContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#elifBlock}.
	 * @param ctx the parse tree
	 */
	void enterElifBlock(templateParser.ElifBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#elifBlock}.
	 * @param ctx the parse tree
	 */
	void exitElifBlock(templateParser.ElifBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#elseBlock}.
	 * @param ctx the parse tree
	 */
	void enterElseBlock(templateParser.ElseBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#elseBlock}.
	 * @param ctx the parse tree
	 */
	void exitElseBlock(templateParser.ElseBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#subBlock}.
	 * @param ctx the parse tree
	 */
	void enterSubBlock(templateParser.SubBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#subBlock}.
	 * @param ctx the parse tree
	 */
	void exitSubBlock(templateParser.SubBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#forBlock}.
	 * @param ctx the parse tree
	 */
	void enterForBlock(templateParser.ForBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#forBlock}.
	 * @param ctx the parse tree
	 */
	void exitForBlock(templateParser.ForBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#forStartStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStartStatement(templateParser.ForStartStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#forStartStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStartStatement(templateParser.ForStartStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#forEndStatement}.
	 * @param ctx the parse tree
	 */
	void enterForEndStatement(templateParser.ForEndStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#forEndStatement}.
	 * @param ctx the parse tree
	 */
	void exitForEndStatement(templateParser.ForEndStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#forBody}.
	 * @param ctx the parse tree
	 */
	void enterForBody(templateParser.ForBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#forBody}.
	 * @param ctx the parse tree
	 */
	void exitForBody(templateParser.ForBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#extendsBlock}.
	 * @param ctx the parse tree
	 */
	void enterExtendsBlock(templateParser.ExtendsBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#extendsBlock}.
	 * @param ctx the parse tree
	 */
	void exitExtendsBlock(templateParser.ExtendsBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#inheritBlock}.
	 * @param ctx the parse tree
	 */
	void enterInheritBlock(templateParser.InheritBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#inheritBlock}.
	 * @param ctx the parse tree
	 */
	void exitInheritBlock(templateParser.InheritBlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#inheritBlockBody}.
	 * @param ctx the parse tree
	 */
	void enterInheritBlockBody(templateParser.InheritBlockBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#inheritBlockBody}.
	 * @param ctx the parse tree
	 */
	void exitInheritBlockBody(templateParser.InheritBlockBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#inheritBlockStart}.
	 * @param ctx the parse tree
	 */
	void enterInheritBlockStart(templateParser.InheritBlockStartContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#inheritBlockStart}.
	 * @param ctx the parse tree
	 */
	void exitInheritBlockStart(templateParser.InheritBlockStartContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#inheritBlockEnd}.
	 * @param ctx the parse tree
	 */
	void enterInheritBlockEnd(templateParser.InheritBlockEndContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#inheritBlockEnd}.
	 * @param ctx the parse tree
	 */
	void exitInheritBlockEnd(templateParser.InheritBlockEndContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#jinjaExpression}.
	 * @param ctx the parse tree
	 */
	void enterJinjaExpression(templateParser.JinjaExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#jinjaExpression}.
	 * @param ctx the parse tree
	 */
	void exitJinjaExpression(templateParser.JinjaExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#jinjaExprStart}.
	 * @param ctx the parse tree
	 */
	void enterJinjaExprStart(templateParser.JinjaExprStartContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#jinjaExprStart}.
	 * @param ctx the parse tree
	 */
	void exitJinjaExprStart(templateParser.JinjaExprStartContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#jinjaExprEnd}.
	 * @param ctx the parse tree
	 */
	void enterJinjaExprEnd(templateParser.JinjaExprEndContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#jinjaExprEnd}.
	 * @param ctx the parse tree
	 */
	void exitJinjaExprEnd(templateParser.JinjaExprEndContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(templateParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(templateParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#ternaryExt}.
	 * @param ctx the parse tree
	 */
	void enterTernaryExt(templateParser.TernaryExtContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#ternaryExt}.
	 * @param ctx the parse tree
	 */
	void exitTernaryExt(templateParser.TernaryExtContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#defaultExpr}.
	 * @param ctx the parse tree
	 */
	void enterDefaultExpr(templateParser.DefaultExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#defaultExpr}.
	 * @param ctx the parse tree
	 */
	void exitDefaultExpr(templateParser.DefaultExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#orExpr}.
	 * @param ctx the parse tree
	 */
	void enterOrExpr(templateParser.OrExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#orExpr}.
	 * @param ctx the parse tree
	 */
	void exitOrExpr(templateParser.OrExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void enterAndExpr(templateParser.AndExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#andExpr}.
	 * @param ctx the parse tree
	 */
	void exitAndExpr(templateParser.AndExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#notExpr}.
	 * @param ctx the parse tree
	 */
	void enterNotExpr(templateParser.NotExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#notExpr}.
	 * @param ctx the parse tree
	 */
	void exitNotExpr(templateParser.NotExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isExpr}
	 * labeled alternative in {@link templateParser#compareExpr}.
	 * @param ctx the parse tree
	 */
	void enterIsExpr(templateParser.IsExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isExpr}
	 * labeled alternative in {@link templateParser#compareExpr}.
	 * @param ctx the parse tree
	 */
	void exitIsExpr(templateParser.IsExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code compExpr}
	 * labeled alternative in {@link templateParser#compareExpr}.
	 * @param ctx the parse tree
	 */
	void enterCompExpr(templateParser.CompExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code compExpr}
	 * labeled alternative in {@link templateParser#compareExpr}.
	 * @param ctx the parse tree
	 */
	void exitCompExpr(templateParser.CompExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inExpr}
	 * labeled alternative in {@link templateParser#compareExpr}.
	 * @param ctx the parse tree
	 */
	void enterInExpr(templateParser.InExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inExpr}
	 * labeled alternative in {@link templateParser#compareExpr}.
	 * @param ctx the parse tree
	 */
	void exitInExpr(templateParser.InExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code equalOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterEqualOperator(templateParser.EqualOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code equalOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitEqualOperator(templateParser.EqualOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code notEqualOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterNotEqualOperator(templateParser.NotEqualOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code notEqualOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitNotEqualOperator(templateParser.NotEqualOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lessThanOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterLessThanOperator(templateParser.LessThanOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lessThanOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitLessThanOperator(templateParser.LessThanOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code greaterThanOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterGreaterThanOperator(templateParser.GreaterThanOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code greaterThanOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitGreaterThanOperator(templateParser.GreaterThanOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code lessOrEqualOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterLessOrEqualOperator(templateParser.LessOrEqualOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code lessOrEqualOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitLessOrEqualOperator(templateParser.LessOrEqualOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code greaterOrEqualOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterGreaterOrEqualOperator(templateParser.GreaterOrEqualOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code greaterOrEqualOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitGreaterOrEqualOperator(templateParser.GreaterOrEqualOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#pipeExpr}.
	 * @param ctx the parse tree
	 */
	void enterPipeExpr(templateParser.PipeExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#pipeExpr}.
	 * @param ctx the parse tree
	 */
	void exitPipeExpr(templateParser.PipeExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#filter}.
	 * @param ctx the parse tree
	 */
	void enterFilter(templateParser.FilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#filter}.
	 * @param ctx the parse tree
	 */
	void exitFilter(templateParser.FilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(templateParser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(templateParser.ArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(templateParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(templateParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#concatExpr}.
	 * @param ctx the parse tree
	 */
	void enterConcatExpr(templateParser.ConcatExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#concatExpr}.
	 * @param ctx the parse tree
	 */
	void exitConcatExpr(templateParser.ConcatExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#addExpr}.
	 * @param ctx the parse tree
	 */
	void enterAddExpr(templateParser.AddExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#addExpr}.
	 * @param ctx the parse tree
	 */
	void exitAddExpr(templateParser.AddExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code plusOperator}
	 * labeled alternative in {@link templateParser#addExprOptor}.
	 * @param ctx the parse tree
	 */
	void enterPlusOperator(templateParser.PlusOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code plusOperator}
	 * labeled alternative in {@link templateParser#addExprOptor}.
	 * @param ctx the parse tree
	 */
	void exitPlusOperator(templateParser.PlusOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code minusOperator}
	 * labeled alternative in {@link templateParser#addExprOptor}.
	 * @param ctx the parse tree
	 */
	void enterMinusOperator(templateParser.MinusOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code minusOperator}
	 * labeled alternative in {@link templateParser#addExprOptor}.
	 * @param ctx the parse tree
	 */
	void exitMinusOperator(templateParser.MinusOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#mulExpr}.
	 * @param ctx the parse tree
	 */
	void enterMulExpr(templateParser.MulExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#mulExpr}.
	 * @param ctx the parse tree
	 */
	void exitMulExpr(templateParser.MulExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mulOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 */
	void enterMulOperator(templateParser.MulOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mulOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 */
	void exitMulOperator(templateParser.MulOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code divOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 */
	void enterDivOperator(templateParser.DivOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code divOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 */
	void exitDivOperator(templateParser.DivOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code floorDivOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 */
	void enterFloorDivOperator(templateParser.FloorDivOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code floorDivOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 */
	void exitFloorDivOperator(templateParser.FloorDivOperatorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code modOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 */
	void enterModOperator(templateParser.ModOperatorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code modOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 */
	void exitModOperator(templateParser.ModOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpr(templateParser.UnaryExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#unaryExpr}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpr(templateParser.UnaryExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#powExpr}.
	 * @param ctx the parse tree
	 */
	void enterPowExpr(templateParser.PowExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#powExpr}.
	 * @param ctx the parse tree
	 */
	void exitPowExpr(templateParser.PowExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code id}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterId(templateParser.IdContext ctx);
	/**
	 * Exit a parse tree produced by the {@code id}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitId(templateParser.IdContext ctx);
	/**
	 * Enter a parse tree produced by the {@code int}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterInt(templateParser.IntContext ctx);
	/**
	 * Exit a parse tree produced by the {@code int}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitInt(templateParser.IntContext ctx);
	/**
	 * Enter a parse tree produced by the {@code float}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterFloat(templateParser.FloatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code float}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitFloat(templateParser.FloatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code string}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterString(templateParser.StringContext ctx);
	/**
	 * Exit a parse tree produced by the {@code string}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitString(templateParser.StringContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthedExpr}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterParenthedExpr(templateParser.ParenthedExprContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthedExpr}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitParenthedExpr(templateParser.ParenthedExprContext ctx);
	/**
	 * Enter a parse tree produced by the {@code list}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterList(templateParser.ListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code list}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitList(templateParser.ListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dict}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterDict(templateParser.DictContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dict}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitDict(templateParser.DictContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#pair}.
	 * @param ctx the parse tree
	 */
	void enterPair(templateParser.PairContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#pair}.
	 * @param ctx the parse tree
	 */
	void exitPair(templateParser.PairContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(templateParser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(templateParser.PrimaryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code memberTrailer}
	 * labeled alternative in {@link templateParser#trailer}.
	 * @param ctx the parse tree
	 */
	void enterMemberTrailer(templateParser.MemberTrailerContext ctx);
	/**
	 * Exit a parse tree produced by the {@code memberTrailer}
	 * labeled alternative in {@link templateParser#trailer}.
	 * @param ctx the parse tree
	 */
	void exitMemberTrailer(templateParser.MemberTrailerContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subTrailer}
	 * labeled alternative in {@link templateParser#trailer}.
	 * @param ctx the parse tree
	 */
	void enterSubTrailer(templateParser.SubTrailerContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subTrailer}
	 * labeled alternative in {@link templateParser#trailer}.
	 * @param ctx the parse tree
	 */
	void exitSubTrailer(templateParser.SubTrailerContext ctx);
	/**
	 * Enter a parse tree produced by the {@code callTrailer}
	 * labeled alternative in {@link templateParser#trailer}.
	 * @param ctx the parse tree
	 */
	void enterCallTrailer(templateParser.CallTrailerContext ctx);
	/**
	 * Exit a parse tree produced by the {@code callTrailer}
	 * labeled alternative in {@link templateParser#trailer}.
	 * @param ctx the parse tree
	 */
	void exitCallTrailer(templateParser.CallTrailerContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlElement}.
	 * @param ctx the parse tree
	 */
	void enterHtmlElement(templateParser.HtmlElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlElement}.
	 * @param ctx the parse tree
	 */
	void exitHtmlElement(templateParser.HtmlElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlRegularElement}.
	 * @param ctx the parse tree
	 */
	void enterHtmlRegularElement(templateParser.HtmlRegularElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlRegularElement}.
	 * @param ctx the parse tree
	 */
	void exitHtmlRegularElement(templateParser.HtmlRegularElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlStartTag}.
	 * @param ctx the parse tree
	 */
	void enterHtmlStartTag(templateParser.HtmlStartTagContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlStartTag}.
	 * @param ctx the parse tree
	 */
	void exitHtmlStartTag(templateParser.HtmlStartTagContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlElementBody}.
	 * @param ctx the parse tree
	 */
	void enterHtmlElementBody(templateParser.HtmlElementBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlElementBody}.
	 * @param ctx the parse tree
	 */
	void exitHtmlElementBody(templateParser.HtmlElementBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlEndTag}.
	 * @param ctx the parse tree
	 */
	void enterHtmlEndTag(templateParser.HtmlEndTagContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlEndTag}.
	 * @param ctx the parse tree
	 */
	void exitHtmlEndTag(templateParser.HtmlEndTagContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlSelfClosingElement}.
	 * @param ctx the parse tree
	 */
	void enterHtmlSelfClosingElement(templateParser.HtmlSelfClosingElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlSelfClosingElement}.
	 * @param ctx the parse tree
	 */
	void exitHtmlSelfClosingElement(templateParser.HtmlSelfClosingElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlSelfClosingTag}.
	 * @param ctx the parse tree
	 */
	void enterHtmlSelfClosingTag(templateParser.HtmlSelfClosingTagContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlSelfClosingTag}.
	 * @param ctx the parse tree
	 */
	void exitHtmlSelfClosingTag(templateParser.HtmlSelfClosingTagContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlTagAttr}.
	 * @param ctx the parse tree
	 */
	void enterHtmlTagAttr(templateParser.HtmlTagAttrContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlTagAttr}.
	 * @param ctx the parse tree
	 */
	void exitHtmlTagAttr(templateParser.HtmlTagAttrContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#booleanAttr}.
	 * @param ctx the parse tree
	 */
	void enterBooleanAttr(templateParser.BooleanAttrContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#booleanAttr}.
	 * @param ctx the parse tree
	 */
	void exitBooleanAttr(templateParser.BooleanAttrContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#attrWithUnquotedVal}.
	 * @param ctx the parse tree
	 */
	void enterAttrWithUnquotedVal(templateParser.AttrWithUnquotedValContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#attrWithUnquotedVal}.
	 * @param ctx the parse tree
	 */
	void exitAttrWithUnquotedVal(templateParser.AttrWithUnquotedValContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#attrWithQuotedVal}.
	 * @param ctx the parse tree
	 */
	void enterAttrWithQuotedVal(templateParser.AttrWithQuotedValContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#attrWithQuotedVal}.
	 * @param ctx the parse tree
	 */
	void exitAttrWithQuotedVal(templateParser.AttrWithQuotedValContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#quotedValElem}.
	 * @param ctx the parse tree
	 */
	void enterQuotedValElem(templateParser.QuotedValElemContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#quotedValElem}.
	 * @param ctx the parse tree
	 */
	void exitQuotedValElem(templateParser.QuotedValElemContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#jinjaAttrVal}.
	 * @param ctx the parse tree
	 */
	void enterJinjaAttrVal(templateParser.JinjaAttrValContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#jinjaAttrVal}.
	 * @param ctx the parse tree
	 */
	void exitJinjaAttrVal(templateParser.JinjaAttrValContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#styleAttr}.
	 * @param ctx the parse tree
	 */
	void enterStyleAttr(templateParser.StyleAttrContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#styleAttr}.
	 * @param ctx the parse tree
	 */
	void exitStyleAttr(templateParser.StyleAttrContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#inlineStyleProp}.
	 * @param ctx the parse tree
	 */
	void enterInlineStyleProp(templateParser.InlineStylePropContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#inlineStyleProp}.
	 * @param ctx the parse tree
	 */
	void exitInlineStyleProp(templateParser.InlineStylePropContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlStyleElem}.
	 * @param ctx the parse tree
	 */
	void enterHtmlStyleElem(templateParser.HtmlStyleElemContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlStyleElem}.
	 * @param ctx the parse tree
	 */
	void exitHtmlStyleElem(templateParser.HtmlStyleElemContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlStyleElemOpenTag}.
	 * @param ctx the parse tree
	 */
	void enterHtmlStyleElemOpenTag(templateParser.HtmlStyleElemOpenTagContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlStyleElemOpenTag}.
	 * @param ctx the parse tree
	 */
	void exitHtmlStyleElemOpenTag(templateParser.HtmlStyleElemOpenTagContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#htmlStyleElemCloseTag}.
	 * @param ctx the parse tree
	 */
	void enterHtmlStyleElemCloseTag(templateParser.HtmlStyleElemCloseTagContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#htmlStyleElemCloseTag}.
	 * @param ctx the parse tree
	 */
	void exitHtmlStyleElemCloseTag(templateParser.HtmlStyleElemCloseTagContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#cssBlock}.
	 * @param ctx the parse tree
	 */
	void enterCssBlock(templateParser.CssBlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#cssBlock}.
	 * @param ctx the parse tree
	 */
	void exitCssBlock(templateParser.CssBlockContext ctx);
	/**
	 * Enter a parse tree produced by the {@code singleSelector}
	 * labeled alternative in {@link templateParser#selectorList}.
	 * @param ctx the parse tree
	 */
	void enterSingleSelector(templateParser.SingleSelectorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code singleSelector}
	 * labeled alternative in {@link templateParser#selectorList}.
	 * @param ctx the parse tree
	 */
	void exitSingleSelector(templateParser.SingleSelectorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code descendentSelector}
	 * labeled alternative in {@link templateParser#selectorList}.
	 * @param ctx the parse tree
	 */
	void enterDescendentSelector(templateParser.DescendentSelectorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code descendentSelector}
	 * labeled alternative in {@link templateParser#selectorList}.
	 * @param ctx the parse tree
	 */
	void exitDescendentSelector(templateParser.DescendentSelectorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code groupSelector}
	 * labeled alternative in {@link templateParser#selectorList}.
	 * @param ctx the parse tree
	 */
	void enterGroupSelector(templateParser.GroupSelectorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code groupSelector}
	 * labeled alternative in {@link templateParser#selectorList}.
	 * @param ctx the parse tree
	 */
	void exitGroupSelector(templateParser.GroupSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#selector}.
	 * @param ctx the parse tree
	 */
	void enterSelector(templateParser.SelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#selector}.
	 * @param ctx the parse tree
	 */
	void exitSelector(templateParser.SelectorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code idSelector}
	 * labeled alternative in {@link templateParser#simpleSelector}.
	 * @param ctx the parse tree
	 */
	void enterIdSelector(templateParser.IdSelectorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code idSelector}
	 * labeled alternative in {@link templateParser#simpleSelector}.
	 * @param ctx the parse tree
	 */
	void exitIdSelector(templateParser.IdSelectorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code classSelector}
	 * labeled alternative in {@link templateParser#simpleSelector}.
	 * @param ctx the parse tree
	 */
	void enterClassSelector(templateParser.ClassSelectorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code classSelector}
	 * labeled alternative in {@link templateParser#simpleSelector}.
	 * @param ctx the parse tree
	 */
	void exitClassSelector(templateParser.ClassSelectorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code elementSelector}
	 * labeled alternative in {@link templateParser#simpleSelector}.
	 * @param ctx the parse tree
	 */
	void enterElementSelector(templateParser.ElementSelectorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code elementSelector}
	 * labeled alternative in {@link templateParser#simpleSelector}.
	 * @param ctx the parse tree
	 */
	void exitElementSelector(templateParser.ElementSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#pseudoClassSelector}.
	 * @param ctx the parse tree
	 */
	void enterPseudoClassSelector(templateParser.PseudoClassSelectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#pseudoClassSelector}.
	 * @param ctx the parse tree
	 */
	void exitPseudoClassSelector(templateParser.PseudoClassSelectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link templateParser#cssProp}.
	 * @param ctx the parse tree
	 */
	void enterCssProp(templateParser.CssPropContext ctx);
	/**
	 * Exit a parse tree produced by {@link templateParser#cssProp}.
	 * @param ctx the parse tree
	 */
	void exitCssProp(templateParser.CssPropContext ctx);
}