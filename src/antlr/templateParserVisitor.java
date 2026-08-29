// Generated from grammars/templateParser.g4 by ANTLR 4.13.2
 package antlr; 
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link templateParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface templateParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link templateParser#template}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplate(templateParser.TemplateContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#jinjaElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJinjaElement(templateParser.JinjaElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#templateText}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemplateText(templateParser.TemplateTextContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#setStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetStatement(templateParser.SetStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#ifBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfBlock(templateParser.IfBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#ifStatmentStart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatmentStart(templateParser.IfStatmentStartContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#ifBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfBody(templateParser.IfBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#ifBodyElem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfBodyElem(templateParser.IfBodyElemContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#ifStatmentEnd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatmentEnd(templateParser.IfStatmentEndContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#elifBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElifBlock(templateParser.ElifBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#elseBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseBlock(templateParser.ElseBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#subBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubBlock(templateParser.SubBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#forBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForBlock(templateParser.ForBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#forStartStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForStartStatement(templateParser.ForStartStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#forEndStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForEndStatement(templateParser.ForEndStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#forBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForBody(templateParser.ForBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#extendsBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtendsBlock(templateParser.ExtendsBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#inheritBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInheritBlock(templateParser.InheritBlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#inheritBlockBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInheritBlockBody(templateParser.InheritBlockBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#inheritBlockStart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInheritBlockStart(templateParser.InheritBlockStartContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#inheritBlockEnd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInheritBlockEnd(templateParser.InheritBlockEndContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#jinjaExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJinjaExpression(templateParser.JinjaExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#jinjaExprStart}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJinjaExprStart(templateParser.JinjaExprStartContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#jinjaExprEnd}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJinjaExprEnd(templateParser.JinjaExprEndContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(templateParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#ternaryExt}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTernaryExt(templateParser.TernaryExtContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#defaultExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefaultExpr(templateParser.DefaultExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#orExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrExpr(templateParser.OrExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#andExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAndExpr(templateParser.AndExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#notExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotExpr(templateParser.NotExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code isExpr}
	 * labeled alternative in {@link templateParser#compareExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIsExpr(templateParser.IsExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code compExpr}
	 * labeled alternative in {@link templateParser#compareExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompExpr(templateParser.CompExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code inExpr}
	 * labeled alternative in {@link templateParser#compareExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInExpr(templateParser.InExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code equalOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEqualOperator(templateParser.EqualOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code notEqualOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNotEqualOperator(templateParser.NotEqualOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lessThanOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLessThanOperator(templateParser.LessThanOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code greaterThanOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreaterThanOperator(templateParser.GreaterThanOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code lessOrEqualOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLessOrEqualOperator(templateParser.LessOrEqualOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code greaterOrEqualOperator}
	 * labeled alternative in {@link templateParser#comparisonOperator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGreaterOrEqualOperator(templateParser.GreaterOrEqualOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#pipeExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPipeExpr(templateParser.PipeExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#filter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilter(templateParser.FilterContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#argumentList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgumentList(templateParser.ArgumentListContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(templateParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#concatExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConcatExpr(templateParser.ConcatExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#addExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAddExpr(templateParser.AddExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code plusOperator}
	 * labeled alternative in {@link templateParser#addExprOptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPlusOperator(templateParser.PlusOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code minusOperator}
	 * labeled alternative in {@link templateParser#addExprOptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMinusOperator(templateParser.MinusOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#mulExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulExpr(templateParser.MulExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code mulOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulOperator(templateParser.MulOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code divOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDivOperator(templateParser.DivOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code floorDivOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloorDivOperator(templateParser.FloorDivOperatorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code modOperator}
	 * labeled alternative in {@link templateParser#mulExprOptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModOperator(templateParser.ModOperatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#unaryExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryExpr(templateParser.UnaryExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#powExpr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPowExpr(templateParser.PowExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code id}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId(templateParser.IdContext ctx);
	/**
	 * Visit a parse tree produced by the {@code int}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInt(templateParser.IntContext ctx);
	/**
	 * Visit a parse tree produced by the {@code float}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloat(templateParser.FloatContext ctx);
	/**
	 * Visit a parse tree produced by the {@code string}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString(templateParser.StringContext ctx);
	/**
	 * Visit a parse tree produced by the {@code parenthedExpr}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParenthedExpr(templateParser.ParenthedExprContext ctx);
	/**
	 * Visit a parse tree produced by the {@code list}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList(templateParser.ListContext ctx);
	/**
	 * Visit a parse tree produced by the {@code dict}
	 * labeled alternative in {@link templateParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDict(templateParser.DictContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#pair}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPair(templateParser.PairContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(templateParser.PrimaryContext ctx);
	/**
	 * Visit a parse tree produced by the {@code memberTrailer}
	 * labeled alternative in {@link templateParser#trailer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMemberTrailer(templateParser.MemberTrailerContext ctx);
	/**
	 * Visit a parse tree produced by the {@code subTrailer}
	 * labeled alternative in {@link templateParser#trailer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubTrailer(templateParser.SubTrailerContext ctx);
	/**
	 * Visit a parse tree produced by the {@code callTrailer}
	 * labeled alternative in {@link templateParser#trailer}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallTrailer(templateParser.CallTrailerContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlElement(templateParser.HtmlElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlRegularElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlRegularElement(templateParser.HtmlRegularElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlStartTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlStartTag(templateParser.HtmlStartTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlElementBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlElementBody(templateParser.HtmlElementBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlEndTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlEndTag(templateParser.HtmlEndTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlSelfClosingElement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlSelfClosingElement(templateParser.HtmlSelfClosingElementContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlSelfClosingTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlSelfClosingTag(templateParser.HtmlSelfClosingTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlTagAttr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlTagAttr(templateParser.HtmlTagAttrContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#booleanAttr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanAttr(templateParser.BooleanAttrContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#attrWithUnquotedVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttrWithUnquotedVal(templateParser.AttrWithUnquotedValContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#attrWithQuotedVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttrWithQuotedVal(templateParser.AttrWithQuotedValContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#quotedValElem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuotedValElem(templateParser.QuotedValElemContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#jinjaAttrVal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJinjaAttrVal(templateParser.JinjaAttrValContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#styleAttr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStyleAttr(templateParser.StyleAttrContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#inlineStyleProp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInlineStyleProp(templateParser.InlineStylePropContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlStyleElem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlStyleElem(templateParser.HtmlStyleElemContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlStyleElemOpenTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlStyleElemOpenTag(templateParser.HtmlStyleElemOpenTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#htmlStyleElemCloseTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHtmlStyleElemCloseTag(templateParser.HtmlStyleElemCloseTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#cssBlock}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCssBlock(templateParser.CssBlockContext ctx);
	/**
	 * Visit a parse tree produced by the {@code singleSelector}
	 * labeled alternative in {@link templateParser#selectorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleSelector(templateParser.SingleSelectorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code descendentSelector}
	 * labeled alternative in {@link templateParser#selectorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDescendentSelector(templateParser.DescendentSelectorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code groupSelector}
	 * labeled alternative in {@link templateParser#selectorList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupSelector(templateParser.GroupSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#selector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelector(templateParser.SelectorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code idSelector}
	 * labeled alternative in {@link templateParser#simpleSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdSelector(templateParser.IdSelectorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code classSelector}
	 * labeled alternative in {@link templateParser#simpleSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClassSelector(templateParser.ClassSelectorContext ctx);
	/**
	 * Visit a parse tree produced by the {@code elementSelector}
	 * labeled alternative in {@link templateParser#simpleSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElementSelector(templateParser.ElementSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#pseudoClassSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPseudoClassSelector(templateParser.PseudoClassSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link templateParser#cssProp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCssProp(templateParser.CssPropContext ctx);
}