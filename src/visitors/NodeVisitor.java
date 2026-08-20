package visitors;

import antlr.templateParser;
import antlr.templateParserBaseVisitor;
import models.Node;
import models.NodeBody;
import models.NormalText;
import models.css.CssBlock;
import models.css.properties.Property;
import models.css.properties.PropertyValue;
import models.css.selectors.*;
import models.html.attributes.*;
import models.html.elements.HtmlRegularElement;
import models.html.elements.HtmlSelfClosingElement;
import models.html.elements.HtmlStyleElement;
import models.jinja.JinjaExpression;
import models.jinja.atoms.*;
import models.jinja.blocks.*;
import models.jinja.expressions.*;
import models.jinja.trailers.CallTrailer;
import models.jinja.trailers.MemberTrailer;
import models.jinja.trailers.SubTrailer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import symbols.SymbolTable;

import java.util.ArrayList;
import java.util.List;

public class NodeVisitor extends templateParserBaseVisitor<Node> {
    public SymbolTable symbolTable;

    public NodeVisitor(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    @Override
    public Node visitTemplateText(templateParser.TemplateTextContext ctx) {
        NormalText normalText = new NormalText(ctx.NORMAL_TEXT().getText());
        normalText.setNodeName("raw text");
        normalText.setLineNumber(ctx.NORMAL_TEXT().getSymbol().getLine());

        return normalText;
    }

    // =================================================================================================================

    //<editor-fold desc="HTML & CSS visit methods">

    @Override
    public Node visitHtmlRegularElement(templateParser.HtmlRegularElementContext ctx) {
        // getting tag name
        String tagName = ctx.htmlStartTag().START_TAG_NAME().getText();

        // getting tag attributes
        ArrayList<Node> attrList = null;

        if (ctx.htmlStartTag().htmlTagAttr() != null) {
            attrList = new ArrayList<>();

            for (int i = 0; i < ctx.htmlStartTag().htmlTagAttr().size(); i++)
                attrList.add(this.visit(ctx.htmlStartTag().htmlTagAttr().get(i)));
        }

        //getting element body
        Node elementBody = null;

        if (ctx.htmlElementBody() != null) {
            elementBody = this.visit(ctx.htmlElementBody());
            elementBody.setNodeName(tagName + " element body");
            elementBody.setLineNumber(ctx.htmlElementBody().getStart().getLine());
        }

        // return html element
        HtmlRegularElement htmlRegularElement = new HtmlRegularElement(tagName, attrList, elementBody);
        htmlRegularElement.setNodeName(tagName + " html element");
        htmlRegularElement.setLineNumber(ctx.getStart().getLine());

        return htmlRegularElement;
    }

    @Override
    public Node visitHtmlSelfClosingElement(templateParser.HtmlSelfClosingElementContext ctx) {
        return this.visit(ctx.htmlSelfClosingTag());
    }

    @Override
    public Node visitHtmlElementBody(templateParser.HtmlElementBodyContext ctx) {
        ArrayList<Node> nodes = new ArrayList<>();

        for (ParseTree child : ctx.children)
            nodes.add(this.visit(child));

        NodeBody nodeBody = new NodeBody(nodes);
        nodeBody.setNodeName("html element body");
        nodeBody.setLineNumber(ctx.getStart().getLine());

        return nodeBody;
    }

    @Override
    public Node visitHtmlSelfClosingTag(templateParser.HtmlSelfClosingTagContext ctx) {
        String tagName = ctx.START_TAG_NAME().getText();

        List<templateParser.HtmlTagAttrContext> attributesContext = ctx.htmlTagAttr();

        // getting element attributes
        ArrayList<Node> attrList = null;

        if (ctx.htmlTagAttr() != null) {
            attrList = new ArrayList<>();

            for (int i = 0; i < ctx.htmlTagAttr().size(); i++)
                attrList.add(this.visit(ctx.htmlTagAttr().get(i)));
        }

        HtmlSelfClosingElement htmlSelfClosingElement = new HtmlSelfClosingElement(tagName, attrList);
        htmlSelfClosingElement.setNodeName(tagName);
        htmlSelfClosingElement.setLineNumber(ctx.START_TAG_NAME().getSymbol().getLine());

        return htmlSelfClosingElement;
    }

    @Override
    public Node visitBooleanAttr(templateParser.BooleanAttrContext ctx) {
        String attrName = ctx.ATTR_NAME().getText();

        BooleanAttribute booleanAttribute = new BooleanAttribute(attrName);
        booleanAttribute.setNodeName(attrName);
        booleanAttribute.setLineNumber(ctx.ATTR_NAME().getSymbol().getLine());

        return booleanAttribute;
    }

    @Override
    public Node visitAttrWithUnquotedVal(templateParser.AttrWithUnquotedValContext ctx) {
        String attributeName = ctx.ATTR_NAME().getText();
        String attributeValue = ctx.ATTR_VALUE_UNQUOTED().getText();

        UnquotedAttribute unquotedAttribute = new UnquotedAttribute(attributeName, attributeValue);
        unquotedAttribute.setNodeName(attributeName);
        unquotedAttribute.setLineNumber(ctx.ATTR_NAME().getSymbol().getLine());

        return unquotedAttribute;
    }

    @Override
    public Node visitAttrWithQuotedVal(templateParser.AttrWithQuotedValContext ctx) {
        String attributeName = ctx.ATTR_NAME().getText();

        // getting attribute values
        ArrayList<Node> attrValList = null;

        if (ctx.quotedValElem() != null) {
            attrValList = new ArrayList<>();

            for (int i = 0; i < ctx.quotedValElem().size(); i++)
                attrValList.add(this.visit(ctx.quotedValElem().get(i)));
        }

        QuotedAttribute quotedAttribute = new QuotedAttribute(attributeName, attrValList);
        quotedAttribute.setNodeName("html element attribute: " + attributeName);
        quotedAttribute.setLineNumber(ctx.ATTR_NAME().getSymbol().getLine());

        return quotedAttribute;
    }

    @Override
    public Node visitQuotedValElem(templateParser.QuotedValElemContext ctx) {
        if (ctx.jinjaAttrVal() != null) {
            Node jinjaExprAsAttrVal = this.visit(ctx.jinjaAttrVal());
            jinjaExprAsAttrVal.setNodeName("jinja expr as atrr val");
            jinjaExprAsAttrVal.setLineNumber(ctx.jinjaAttrVal().getStart().getLine());

            return jinjaExprAsAttrVal;
        } else {
            Token attrValCtx = ctx.ATTR_VAL_TEXT().getSymbol();

            AttributeValue attributeValue = new AttributeValue(attrValCtx.getText());
            attributeValue.setNodeName("html" + attrValCtx.getText());
            attributeValue.setLineNumber(attrValCtx.getLine());

            return attributeValue;
        }
    }

    @Override
    public Node visitStyleAttr(templateParser.StyleAttrContext ctx) {
        String attrName = ctx.STYLE_ATTR().getText(); // attrName = style

        // getting properties
        ArrayList<Node> propList = null;

        if (ctx.inlineStyleProp() != null) {
            propList = new ArrayList<>();

            for (int i = 0; i < ctx.inlineStyleProp().size(); i++) {
                propList.add(this.visit(ctx.inlineStyleProp().get(i)));
            }
        }

        StyleAttribute styleAttribute = new StyleAttribute(attrName, propList);
        styleAttribute.setNodeName(attrName);
        styleAttribute.setLineNumber(ctx.STYLE_ATTR().getSymbol().getLine());
        return styleAttribute;
    }

    @Override
    public Node visitInlineStyleProp(templateParser.InlineStylePropContext ctx) {
        String propName = ctx.CSS_INLINE_PROP_NAME().getText();

        ArrayList<Node> propValues = new ArrayList<>();

        for (TerminalNode propValue : ctx.CSS_PROP_VAL())
            propValues.add(new PropertyValue(propValue.getText()));

        return new Property(propName, propValues);
    }

    @Override
    public Node visitHtmlStyleElem(templateParser.HtmlStyleElemContext ctx) {
        //getting style element name
        String tagName = ctx.htmlStyleElemOpenTag().STYLE_TAG_START_NAME().getText();

        // getting style element body
        NodeBody htmlElementBody = null;

        if (ctx.cssBlock() != null) {
            ArrayList<Node> blockList = new ArrayList<>();

            for (int i = 0; i < ctx.cssBlock().size(); i++)
                blockList.add(this.visit(ctx.cssBlock().get(i)));

            htmlElementBody = new NodeBody(blockList);
            htmlElementBody.setNodeName("style element body");
            htmlElementBody.setLineNumber(ctx.cssBlock().getFirst().getStart().getLine());
        }

        HtmlStyleElement htmlStyleElement = new HtmlStyleElement(tagName, null, htmlElementBody);
        htmlStyleElement.setNodeName("html style element");
        htmlStyleElement.setLineNumber(ctx.getStart().getLine());

        return htmlStyleElement;
    }

    @Override
    public Node visitCssBlock(templateParser.CssBlockContext ctx) {
        // getting css block selectors
        Node selectorList = this.visit(ctx.selectorList());
        selectorList.setNodeName("css block selectors");
        selectorList.setLineNumber(ctx.selectorList().getStart().getLine());

        //getting css block properties
        ArrayList<Node> propList = null;

        if (ctx.cssProp() != null) {
            propList = new ArrayList<>();

            for (int i = 0; i < ctx.cssProp().size(); i++)
                propList.add(this.visit(ctx.cssProp().get(i)));

        }
        // returning css block object
        CssBlock cssBlock = new CssBlock(selectorList, propList);
        cssBlock.setNodeName("css block");
        cssBlock.setLineNumber(ctx.getStart().getLine());

        return cssBlock;
    }

    @Override
    public Node visitSingleSelector(templateParser.SingleSelectorContext ctx) {
        return this.visit(ctx.children.getFirst());
    }

    @Override
    public Node visitDescendentSelector(templateParser.DescendentSelectorContext ctx) {
        ArrayList<Node> selectorList = new ArrayList<>();

        for (int i = 0; i < ctx.selector().size(); i++)
            selectorList.add(this.visit(ctx.selector().get(i)));

        DescendantSelector descendantSelector = new DescendantSelector(selectorList);
        descendantSelector.setNodeName("descendant selector");
        descendantSelector.setLineNumber(ctx.getStart().getLine());

        return descendantSelector;
    }

    @Override
    public Node visitGroupSelector(templateParser.GroupSelectorContext ctx) {
        ArrayList<Node> selectorList = new ArrayList<>();

        for (int i = 0; i < ctx.selector().size(); i++)
            selectorList.add(this.visit(ctx.selector().get(i)));

        GroupSelector groupSelector = new GroupSelector(selectorList);
        groupSelector.setNodeName("group selector");
        groupSelector.setLineNumber(ctx.getStart().getLine());

        return groupSelector;
    }

    @Override
    public Node visitSelector(templateParser.SelectorContext ctx) {
        return this.visit(ctx.children.getFirst());
    }

    @Override
    public Node visitIdSelector(templateParser.IdSelectorContext ctx) {
        String idName = ctx.CSS_SEL_ID().getText().substring(1);

        IdSelector idSelector = new IdSelector(idName);
        idSelector.setNodeName("css id selector: " + idName);
        idSelector.setLineNumber(ctx.CSS_SEL_ID().getSymbol().getLine());

        return idSelector;
    }

    @Override
    public Node visitClassSelector(templateParser.ClassSelectorContext ctx) {
        String className = ctx.CSS_SEL_CLASS().getText().substring(1);

        ClassSelector classSelector = new ClassSelector(className);
        classSelector.setNodeName("class id selector: " + className);
        classSelector.setLineNumber(ctx.CSS_SEL_CLASS().getSymbol().getLine());

        return classSelector;
    }

    @Override
    public Node visitElementSelector(templateParser.ElementSelectorContext ctx) {
        String elementName = ctx.CSS_SEL_ELEM().getText();

        ElementSelector elementSelector = new ElementSelector(elementName);
        elementSelector.setNodeName("class element selector: " + elementName);
        elementSelector.setLineNumber(ctx.CSS_SEL_ELEM().getSymbol().getLine());

        return elementSelector;
    }

    @Override
    public Node visitPseudoClassSelector(templateParser.PseudoClassSelectorContext ctx) {
        // getting selector first part
        Node selector = this.visit(ctx.simpleSelector());
        selector.setNodeName("pseudo selector id");
        selector.setLineNumber(ctx.simpleSelector().getStart().getLine());

        // getting selector state
        String selectorState = ctx.CSS_SEL_STATE().getSymbol().getText().substring(1);

        // return pseudoClassSelector object
        PseudoClassSelector pseudoClassSelector = new PseudoClassSelector(selector, selectorState);
        pseudoClassSelector.setNodeName("pseudo class selector: " + selector.toString());
        pseudoClassSelector.setLineNumber(ctx.getStart().getLine());

        return pseudoClassSelector;
    }

    @Override
    public Node visitCssProp(templateParser.CssPropContext ctx) {
        String propName = ctx.BLK_PROP_NAME().getText();

        ArrayList<Node> propValues = new ArrayList<>();

        for (int i = 0; i < ctx.CSS_PROP_VAL().size(); i++)
            propValues.add(new PropertyValue(ctx.CSS_PROP_VAL().get(i).getText()));

        Property property = new Property(propName, propValues);
        property.setNodeName(propName + " css property");
        property.setLineNumber(ctx.getStart().getLine());

        return property;
    }

    //</editor-fold>

    // =================================================================================================================

    //<editor-fold desc="Jinja elements visit methods">

    @Override
    public Node visitExtendsBlock(templateParser.ExtendsBlockContext ctx) {
        String templateName = ctx.STRING().getText();

        symbolTable.enterScope(templateName + " template");

        ExtendsBlock extendsBlock = new ExtendsBlock(templateName);
        extendsBlock.setNodeName("extends block: " + templateName);
        extendsBlock.setLineNumber(ctx.getStart().getLine());

        return extendsBlock;
    }

    @Override
    public Node visitSetStatement(templateParser.SetStatementContext ctx) {
        Token idToken = ctx.ID().getSymbol();

        IdType id = new IdType(idToken.getText());
        id.setNodeName("set statement id");
        id.setLineNumber(idToken.getLine());

        Node idValueExpr = this.visit(ctx.expression());
        idValueExpr.setNodeName("set statement id value");
        idValueExpr.setLineNumber(ctx.expression().getStart().getLine());

        SetStatement setStatement = new SetStatement(id, idValueExpr);
        setStatement.setNodeName("set statement");
        setStatement.setLineNumber(ctx.getStart().getLine());

        // adding var to symbols table
        symbolTable.currentScope.define(idToken.getText(), "var", "Node", idValueExpr);


        return setStatement;
    }

    @Override
    public Node visitInheritBlock(templateParser.InheritBlockContext ctx) {
        String blockName = ctx.inheritBlockStart().ID().getText();

        StringType stringType = new StringType(blockName);

        symbolTable.enterScope(blockName + " block");
        symbolTable.currentScope.define(blockName, "block name", "StringType", stringType);

        // getting block body
        NodeBody nodeBody = null;

        if (ctx.inheritBlockBody() != null) {
            ArrayList<Node> nodeList = new ArrayList<>();

            for (int i = 0; i < ctx.inheritBlockBody().children.size(); i++) {
                nodeList.add(this.visit(ctx.inheritBlockBody().getChild(i)));
            }

            nodeBody = new NodeBody(nodeList);
            nodeBody.setNodeName("inherit block");
            nodeBody.setLineNumber(ctx.getStart().getLine());
        }
        InheritedBlock inheritedBlock = new InheritedBlock(blockName, nodeBody);
        inheritedBlock.setNodeName("block " + blockName);
        inheritedBlock.setLineNumber(ctx.getStart().getLine());

        symbolTable.exitScope();

        return inheritedBlock;
    }

    @Override
    public Node visitJinjaAttrVal(templateParser.JinjaAttrValContext ctx) {
        Node expr = this.visit(ctx.expression());
        expr.setNodeName("expr as html attr");
        expr.setLineNumber(ctx.getStart().getLine());

        return new JinjaExpression(expr);
    }

    @Override
    public Node visitJinjaExpression(templateParser.JinjaExpressionContext ctx) {

        Node expression = this.visit(ctx.expression());

        JinjaExpression jinjaExpression = new JinjaExpression(expression);
        jinjaExpression.setNodeName("jinja expression");
        jinjaExpression.setLineNumber(ctx.jinjaExprStart().getStart().getLine());

        return jinjaExpression;
    }

    @Override
    public Node visitForBlock(templateParser.ForBlockContext ctx) {

        int blockLine = ctx.forStartStatement().getStart().getLine();
        int blockCol = ctx.forStartStatement().getStart().getCharPositionInLine();

        symbolTable.enterScope("for block at " + blockLine + ":" + blockCol);

        // getting loop vars
        ArrayList<Node> loopVarList = new ArrayList<>();
        List<TerminalNode> idList = ctx.forStartStatement().ID();

        for (int i = 0; i < idList.size(); i++) {

            IdType loopVar = new IdType(idList.get(i).getText());
            loopVar.setNodeName("id: " + idList.get(i).getText());
            loopVar.setLineNumber(idList.get(i).getSymbol().getLine());

            loopVarList.add(loopVar);

            symbolTable.define(loopVar.name, "id", "IdType", loopVar);
        }

        // getting iterable expr
        Node iterable = this.visit(ctx.forStartStatement().expression());
        iterable.setNodeName("for iterable expr");
        iterable.setLineNumber(ctx.forStartStatement().expression().getStart().getLine());

        // getting for body
        Node forBody = null;
        if (ctx.forBody() != null)
            forBody = this.visit(ctx.forBody());

        // return for block node
        ForBlock forBlock = new ForBlock(loopVarList, iterable, forBody);
        forBlock.setNodeName("for block");
        forBlock.setLineNumber(ctx.getStart().getLine());

        symbolTable.exitScope();

        return forBlock;
    }

    @Override
    public Node visitForBody(templateParser.ForBodyContext ctx) {
        ArrayList<Node> nodeList = new ArrayList<>();

        for (int i = 0; i < ctx.children.size(); i++)
            nodeList.add(this.visit(ctx.children.get(i)));

        NodeBody nodeBody = new NodeBody(nodeList);
        nodeBody.setNodeName("for block body");
        nodeBody.setLineNumber(ctx.getStart().getLine());

        return nodeBody;
    }

    @Override
    public Node visitElseBlock(templateParser.ElseBlockContext ctx) {

        int blockLine = ctx.getStart().getLine();
        int blockCol = ctx.getStart().getCharPositionInLine();

        symbolTable.enterScope("else block at " + blockLine + ":" + blockCol);

        NodeBody nodeBody = null;

        if (ctx.subBlock() != null && !ctx.subBlock().isEmpty()) {
            ArrayList<Node> nodeList = new ArrayList<>();

            for (int i = 0; i < ctx.subBlock().size(); i++)
                nodeList.add(this.visit(ctx.subBlock().get(i)));

            nodeBody = new NodeBody(nodeList);
            nodeBody.setNodeName("else block body");
            nodeBody.setLineNumber(ctx.getStart().getLine());
        }

        ElseBlock elseBlock = new ElseBlock(nodeBody);
        elseBlock.setNodeName("else block");
        elseBlock.setLineNumber(ctx.getStart().getLine());

        symbolTable.exitScope();

        return elseBlock;
    }

    @Override
    public Node visitElifBlock(templateParser.ElifBlockContext ctx) {

        int blockLine = ctx.getStart().getLine();
        int blockCol = ctx.getStart().getCharPositionInLine();

        symbolTable.enterScope("elif block at " + blockLine + ":" + blockCol);

        Node condition = this.visit(ctx.expression());
        condition.setNodeName("elif condition");
        condition.setLineNumber(ctx.expression().getStart().getLine());

        NodeBody nodeBody = null;

        if (ctx.subBlock() != null && !ctx.subBlock().isEmpty()) {
            ArrayList<Node> nodeList = new ArrayList<>();

            for (int i = 0; i < ctx.subBlock().size(); i++) {
                nodeList.add(this.visit(ctx.subBlock().get(i)));
            }

            nodeBody = new NodeBody(nodeList);
            nodeBody.setNodeName("elif body");
            nodeBody.setLineNumber(ctx.subBlock().getFirst().getStart().getLine());
        }

        ElifBlock elifBlock = new ElifBlock(condition, nodeBody);
        elifBlock.setNodeName("elif block");
        elifBlock.setLineNumber(ctx.getStart().getLine());

        symbolTable.exitScope();

        return elifBlock;
    }

    @Override
    public Node visitSubBlock(templateParser.SubBlockContext ctx) {
        return this.visit(ctx.children.getFirst());
    }

    @Override
    public Node visitIfBlock(templateParser.IfBlockContext ctx) {

        int blockLine = ctx.getStart().getLine();
        int blockCol = ctx.getStart().getCharPositionInLine();

        symbolTable.enterScope("if block at " + blockLine + ":" + blockCol);

        // getting if condition
        Node condition = this.visit(ctx.ifStatmentStart().expression());
        condition.setNodeName("if condition");
        condition.setLineNumber(ctx.ifStatmentStart().expression().getStart().getLine());

        // getting if body
        NodeBody nodeBody = null;

        if (ctx.ifBody() != null) {
            ArrayList<Node> nodeList = new ArrayList<>();

            for (int i = 0; i < ctx.ifBody().children.size(); i++) {
                nodeList.add(this.visit(ctx.ifBody().children.get(i)));
            }

            nodeBody = new NodeBody(nodeList);
            nodeBody.setNodeName("if body");
            nodeBody.setLineNumber(ctx.ifBody().getStart().getLine());
        }

        // return if block object
        IfBlock ifBlock = new IfBlock(condition, nodeBody);
        ifBlock.setNodeName("if block");
        ifBlock.setLineNumber(ctx.getStart().getLine());

        symbolTable.exitScope();

        return ifBlock;
    }

    //</editor-fold>

    // =================================================================================================================

    //<editor-fold desc="Expressions visit methods">

    @Override
    public Node visitExpression(templateParser.ExpressionContext ctx) {
        // check if expr is "or" / "ternary" exp
        if (ctx.defaultExpr() == null) {
            // check if expr is "or" exp
            if (ctx.ternaryExt() == null) {
                return this.visit(ctx.orExpr());
            }
            // expr is "ternary" exp
            else {
                Node condExpr = this.visit(ctx.orExpr());
                condExpr.setNodeName("ternary cond");
                condExpr.setLineNumber(ctx.orExpr().start.getLine());

                Node trueExpr = this.visit(ctx.ternaryExt().expression().getFirst());
                trueExpr.setNodeName("ternary true expr");
                trueExpr.setLineNumber(ctx.ternaryExt().expression().getFirst().getStart().getLine());

                Node falseExpr = this.visit(ctx.ternaryExt().expression().getLast());
                falseExpr.setNodeName("ternary false expr");
                falseExpr.setLineNumber(ctx.ternaryExt().expression().getLast().getStart().getLine());

                TernaryExpression ternaryExpression = new TernaryExpression(condExpr, trueExpr, falseExpr);
                ternaryExpression.setNodeName("ternary expr");
                ternaryExpression.setLineNumber(ctx.getStart().getLine());

                return ternaryExpression;
            }
        }
        // expr is "default" expr
        else {
            Node value = this.visit(ctx.defaultExpr().orExpr());
            value.setNodeName("default expr main value");
            value.setLineNumber(ctx.defaultExpr().orExpr().getStart().getLine());

            Node defaultValue = this.visit(ctx.defaultExpr().expression());
            defaultValue.setNodeName("default expr backup value");
            defaultValue.setLineNumber(ctx.defaultExpr().expression().getStart().getLine());

            DefaultExpression defaultExpression = new DefaultExpression(value, defaultValue);
            defaultExpression.setNodeName("default expr");
            defaultExpression.setLineNumber(ctx.defaultExpr().getStart().getLine());

            return defaultExpression;
        }
    }

    @Override
    public Node visitOrExpr(templateParser.OrExprContext ctx) {
        List<templateParser.AndExprContext> exprListCtx = ctx.andExpr();

        // checking if it's an "or" expr
        if (exprListCtx.size() > 1) {
            ArrayList<Node> exprList = new ArrayList<>();

            for (int i = 0; i < exprListCtx.size(); i++) {
                exprList.add(this.visit(exprListCtx.get(i)));
            }

            OrExpression orExpression = new OrExpression(exprList);
            orExpression.setNodeName("or expr");
            orExpression.setLineNumber(ctx.OR().getFirst().getSymbol().getLine());

            return orExpression;
        }
        // check if remaining expr is "and" expr
        else {
            return this.visit(ctx.andExpr().getFirst());
        }
    }

    @Override
    public Node visitAndExpr(templateParser.AndExprContext ctx) {
        // checking if it's an "and" expr
        if (ctx.children.size() > 1) {

            ArrayList<Node> exprList = new ArrayList<>();
            for (int i = 0; i < ctx.notExpr().size(); i++) {
                exprList.add(this.visit(ctx.notExpr().get(i)));
            }

            AndExpression andExpression = new AndExpression(exprList);
            andExpression.setNodeName("and expr");
            andExpression.setLineNumber(ctx.getStart().getLine());

            return andExpression;
        }
        // check if remaining expr is "not" expr
        else {
            return this.visit(ctx.children.getFirst());
        }
    }

    @Override
    public Node visitNotExpr(templateParser.NotExprContext ctx) {
        // checking if it's "not" expr
        if (ctx.notExpr() != null) {
            Node expression = this.visit(ctx.notExpr());

            NotExpression notExpression = new NotExpression(expression);
            notExpression.setNodeName("not expr");
            notExpression.setLineNumber(ctx.NOT().getSymbol().getLine());

            return notExpression;
        }
        // check if remaining expr is "compare" expr
        else {
            return this.visit(ctx.compareExpr());
        }
    }

    @Override
    public Node visitIsExpr(templateParser.IsExprContext ctx) {
        if (ctx.IS() != null) {
            // visit "concat" expr
            Node expr = this.visit(ctx.concatExpr());

            // check if expr is negated (not is used)
            boolean negated = (ctx.NOT() != null);

            // getting id
            IdType id = new IdType(ctx.ID().getText());
            id.setNodeName("id");
            id.setLineNumber(ctx.ID().getSymbol().getLine());

            // return in expr
            IsExpression isExpression = new IsExpression(expr, negated, id);
            isExpression.setNodeName("is expr");
            isExpression.setLineNumber(ctx.getStart().getLine());

            return isExpression;
        } else
            return this.visit(ctx.concatExpr());
    }

    @Override
    public Node visitCompExpr(templateParser.CompExprContext ctx) {

        Node expr1 = this.visit(ctx.pipeExpr().getFirst());
        Node expr2 = this.visit(ctx.pipeExpr().getLast());
        Node compOptor = this.visit(ctx.comparisonOperator());

        ComparisonExpression compExpr = new ComparisonExpression(expr1, expr2, compOptor);
        compExpr.setNodeName("comparison expr");
        compExpr.setLineNumber(ctx.getStart().getLine());

        return compExpr;
    }

    @Override
    public Node visitInExpr(templateParser.InExprContext ctx) {

        // check if expr is "in" expr
        if (ctx.pipeExpr().size() > 1) {
            // visit "pipe" expressions
            Node expr1 = this.visit(ctx.pipeExpr().getFirst());
            Node expr2 = this.visit(ctx.pipeExpr().getLast());

            InExpression inExpression = new InExpression(expr1, expr2);
            inExpression.setNodeName("in expr");
            inExpression.setLineNumber(ctx.IN().getSymbol().getLine());

            return inExpression;
        }
        // visit "pipe" expr
        else {
            return this.visit(ctx.pipeExpr().getFirst());
        }
    }

    @Override
    public Node visitEqualOperator(templateParser.EqualOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("== operator");

        return operator;
    }

    @Override
    public Node visitNotEqualOperator(templateParser.NotEqualOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("!= operator");

        return operator;
    }

    @Override
    public Node visitLessThanOperator(templateParser.LessThanOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("< operator");

        return operator;
    }

    @Override
    public Node visitGreaterThanOperator(templateParser.GreaterThanOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("> operator");

        return operator;
    }

    @Override
    public Node visitLessOrEqualOperator(templateParser.LessOrEqualOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("<= operator");

        return operator;
    }

    @Override
    public Node visitGreaterOrEqualOperator(templateParser.GreaterOrEqualOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName(">= operator");

        return operator;
    }

    @Override
    public Node visitPipeExpr(templateParser.PipeExprContext ctx) {

        if (ctx.filter() == null || ctx.filter().isEmpty()) {
            return this.visit(ctx.concatExpr());
        } else {
            Node expr = this.visit(ctx.concatExpr());
            ArrayList<Node> filterList = new ArrayList<>();

            for (int i = 0; i < ctx.filter().size(); i++)
                filterList.add(this.visit(ctx.filter().get(i)));

            PipeExpression pipeExpression = new PipeExpression(expr, filterList);
            pipeExpression.setNodeName("pipe expr");
            pipeExpression.setLineNumber(ctx.getStart().getLine());

            return pipeExpression;
        }
    }

    @Override
    public Node visitFilter(templateParser.FilterContext ctx) {
        Node filterName = new IdType(ctx.ID().getText());
        filterName.setNodeName("filterName");
        filterName.setLineNumber(ctx.ID().getSymbol().getLine());

        Node argList = null;

        if (ctx.argumentList() != null)
            argList = this.visit(ctx.argumentList());

        FilterExpression filter = new FilterExpression(filterName, argList);
        filter.setNodeName("filter expr");
        filter.setLineNumber(ctx.getStart().getLine());

        return filter;
    }

    @Override
    public Node visitArgumentList(templateParser.ArgumentListContext ctx) {
        ArrayList<Node> argList = new ArrayList<>();

        for (int i = 0; i < ctx.argument().size(); i++)
            argList.add(this.visit(ctx.argument().get(i)));

        ArgumentList argumentList = new ArgumentList(argList);
        argumentList.setNodeName("arg list");
        argumentList.setLineNumber(ctx.getStart().getLine());

        return argumentList;
    }

    @Override
    public Node visitArgument(templateParser.ArgumentContext ctx) {
        Node expr = this.visit(ctx.expression());
        Node argName = null;

        if (ctx.ID() != null) {
            argName = new IdType(ctx.ID().getText());
            argName.setNodeName("id");
            argName.setLineNumber(ctx.ID().getSymbol().getLine());
        }

        Argument argument = new Argument(expr, argName);
        argument.setNodeName("argument");
        argument.setLineNumber(ctx.getStart().getLine());

        return argument;
    }

    @Override
    public Node visitConcatExpr(templateParser.ConcatExprContext ctx) {
        // check if expr is "concat" expr
        if (ctx.children.size() > 1) {

            ArrayList<Node> exprList = new ArrayList<>();
            for (int i = 0; i < ctx.addExpr().size(); i++) {
                exprList.add(this.visit(ctx.addExpr().get(i)));
            }

            ConcatExpression concatExpression = new ConcatExpression(exprList);
            concatExpression.setNodeName("concat expr");
            concatExpression.setLineNumber(ctx.getStart().getLine());

            return concatExpression;
        }
        // visit "add" expr
        else {
            return this.visit(ctx.addExpr().getFirst());
        }
    }

    @Override
    public Node visitAddExpr(templateParser.AddExprContext ctx) {
        // checking if it's an "add" expr
        if (ctx.children.size() > 1) {

            ArrayList<Node> exprList = new ArrayList<>();
            for (int i = 0; i < ctx.mulExpr().size(); i++) {
                exprList.add(this.visit(ctx.mulExpr().get(i)));
            }

            // Keep the operators: without them '-' was indistinguishable from '+'.
            ArrayList<Node> operatorList = new ArrayList<>();
            for (int i = 0; i < ctx.addExprOptor().size(); i++) {
                operatorList.add(this.visit(ctx.addExprOptor().get(i)));
            }

            AddExpression addExpression = new AddExpression(exprList, operatorList);
            addExpression.setNodeName("add expr");
            addExpression.setLineNumber(ctx.getStart().getLine());

            return addExpression;
        }
        // visit mul expr
        else {
            return this.visit(ctx.mulExpr().getFirst());
        }
    }

    @Override
    public Node visitPlusOperator(templateParser.PlusOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("plus operator");

        return operator;
    }

    @Override
    public Node visitMinusOperator(templateParser.MinusOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("minus operator");

        return operator;
    }

    @Override
    public Node visitMulExpr(templateParser.MulExprContext ctx) {
        // check if it's mul expr
        if (ctx.children.size() > 1) {
            ArrayList<Node> exprList = new ArrayList<>();

            for (int i = 0; i < ctx.unaryExpr().size(); i++) {
                exprList.add(this.visit(ctx.unaryExpr().get(i)));
            }

            // Keep the operators: without them '/' and '%' collapsed into '*'.
            ArrayList<Node> operatorList = new ArrayList<>();
            for (int i = 0; i < ctx.mulExprOptor().size(); i++) {
                operatorList.add(this.visit(ctx.mulExprOptor().get(i)));
            }

            MulExpression mulExpression = new MulExpression(exprList, operatorList);
            mulExpression.setNodeName("mul expr");
            mulExpression.setLineNumber(ctx.getStart().getLine());

            return mulExpression;
        }
        // visit unary expr
        else {
            return this.visit(ctx.unaryExpr().getFirst());
        }
    }

    @Override
    public Node visitMulOperator(templateParser.MulOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("mul operator");

        return operator;
    }

    @Override
    public Node visitDivOperator(templateParser.DivOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("div operator");

        return operator;
    }

    @Override
    public Node visitFloorDivOperator(templateParser.FloorDivOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("floor div operator");

        return operator;
    }

    @Override
    public Node visitModOperator(templateParser.ModOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("mod operator");

        return operator;
    }

    @Override
    public Node visitUnaryExpr(templateParser.UnaryExprContext ctx) {
        if (ctx.unaryExpr() != null) {
            Node expr = this.visit(ctx.unaryExpr());
            Node sign;

            if (ctx.MINUS() != null) {
                sign = new UnaryOperator(ctx.MINUS().getText());
                sign.setLineNumber(ctx.MINUS().getSymbol().getLine());
            } else {
                sign = new UnaryOperator(ctx.PLUS().getText());
                sign.setLineNumber(ctx.PLUS().getSymbol().getLine());
            }
            sign.setNodeName("sign type");

            UnaryExpression unaryExpression = new UnaryExpression(sign, expr);
            unaryExpression.setNodeName("unary expr");
            unaryExpression.setLineNumber(ctx.getStart().getLine());

            return unaryExpression;
        } else {
            return this.visit(ctx.powExpr());
        }
    }

    @Override
    public Node visitPowExpr(templateParser.PowExprContext ctx) {
        // check if expr is "power" expr
        if (ctx.POW() != null) {
            // visit "primary" expr
            Node baseValueExpr = this.visit(ctx.primary());
            // visit "unary" expr
            Node powerValueExpr = this.visit(ctx.unaryExpr());

            PowerExpression powerExpression = new PowerExpression(baseValueExpr, powerValueExpr);
            powerExpression.setNodeName("power expr");
            powerExpression.setLineNumber(ctx.POW().getSymbol().getLine());

            return powerExpression;
        }
        // expr is "primary" expr
        else {
            return this.visit(ctx.primary());
        }
    }

    @Override
    public Node visitId(templateParser.IdContext ctx) {
        IdType idType = new IdType(ctx.ID().getText());
        idType.setNodeName("id: " + ctx.ID().getText());
        idType.setLineNumber(ctx.ID().getSymbol().getLine());

        return idType;
    }

    @Override
    public Node visitInt(templateParser.IntContext ctx) {
        IntType intType = new IntType(ctx.INT().getText());
        intType.setNodeName("int type");
        intType.setLineNumber(ctx.INT().getSymbol().getLine());

        return intType;
    }

    @Override
    public Node visitFloat(templateParser.FloatContext ctx) {
        FloatType floatType = new FloatType(ctx.FLOAT().getText());
        floatType.setNodeName("float type");
        floatType.setLineNumber(ctx.FLOAT().getSymbol().getLine());

        return floatType;
    }

    @Override
    public Node visitString(templateParser.StringContext ctx) {
        StringType stringType = new StringType(ctx.STRING().getText());
        stringType.setNodeName("string type");
        stringType.setLineNumber(ctx.STRING().getSymbol().getLine());

        return stringType;
    }

    @Override
    public Node visitParenthedExpr(templateParser.ParenthedExprContext ctx) {
        Node expr = this.visit(ctx.expression());

        ParenthedExpression parenthedExpression = new ParenthedExpression(expr);
        parenthedExpression.setNodeName("parenthed expr");
        parenthedExpression.setLineNumber(ctx.getStart().getLine());

        return parenthedExpression;
    }

    @Override
    public Node visitList(templateParser.ListContext ctx) {
        ArrayList<Node> exprList = null;

        if (ctx.expression() != null && !ctx.expression().isEmpty()) {
            exprList = new ArrayList<>();

            for (templateParser.ExpressionContext expr : ctx.expression())
                exprList.add(this.visit(expr));
        }

        ListType listType = new ListType(exprList);
        listType.setNodeName("list type");
        listType.setLineNumber(ctx.getStart().getLine());

        return listType;
    }

    @Override
    public Node visitDict(templateParser.DictContext ctx) {
        ArrayList<Node> pairList = null;

        if (ctx.pair() != null && !ctx.pair().isEmpty()) {
            pairList = new ArrayList<>();

            for (templateParser.PairContext pair : ctx.pair()) {
                pairList.add(this.visit(pair));
            }
        }

        DictType dictType = new DictType(pairList);
        dictType.setNodeName("dict type");
        dictType.setLineNumber(ctx.LBRACE().getSymbol().getLine());

        return dictType;
    }

    @Override
    public Node visitPrimary(templateParser.PrimaryContext ctx) {
        Node atom = this.visit(ctx.atom());
        ArrayList<Node> trailerList = null;

        if (ctx.trailer() != null && !ctx.trailer().isEmpty()) {
            trailerList = new ArrayList<>();

            for (int i = 0; i < ctx.trailer().size(); i++) {
                trailerList.add(this.visit(ctx.trailer().get(i)));
            }
        }

        PrimaryExpression primary = new PrimaryExpression(atom, trailerList);
        primary.setNodeName("primary type");
        primary.setLineNumber(ctx.atom().getStart().getLine());

        return primary;
    }

    @Override
    public Node visitMemberTrailer(templateParser.MemberTrailerContext ctx) {
        Node id = new IdType(ctx.ID().getText());
        id.setNodeName("id");
        id.setLineNumber(ctx.ID().getSymbol().getLine());

        MemberTrailer memberTrailer = new MemberTrailer(id);
        memberTrailer.setNodeName("member trailer");
        memberTrailer.setLineNumber(ctx.DOT().getSymbol().getLine());

        return memberTrailer;
    }

    @Override
    public Node visitSubTrailer(templateParser.SubTrailerContext ctx) {
        Node expr = this.visit(ctx.expression());

        SubTrailer subTrailer = new SubTrailer(expr);
        subTrailer.setNodeName("sub trailer");
        subTrailer.setLineNumber(ctx.getStart().getLine());

        return subTrailer;
    }

    @Override
    public Node visitCallTrailer(templateParser.CallTrailerContext ctx) {
        Node argList = null;

        if (ctx.argumentList() != null) {
            argList = this.visit(ctx.argumentList());
        }

        CallTrailer callTrailer = new CallTrailer(argList);
        callTrailer.setNodeName("call trailer");
        callTrailer.setLineNumber(ctx.LPAREN().getSymbol().getLine());

        return callTrailer;
    }

    @Override
    public Node visitPair(templateParser.PairContext ctx) {
        Node expr1 = this.visit(ctx.expression().getFirst());
        Node expr2 = this.visit(ctx.expression().getLast());

        PairType pairType = new PairType(expr1, expr2);
        pairType.setNodeName("pair type");
        pairType.setLineNumber(ctx.getStart().getLine());

        return pairType;
    }

    //</editor-fold>

    // =================================================================================================================
}