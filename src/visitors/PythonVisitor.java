package visitors;

import antlr.pythonParser;
import antlr.pythonParserBaseVisitor;
import models.Node;
import models.jinja.atoms.*;
import models.jinja.expressions.*;
import models.jinja.trailers.CallTrailer;
import models.jinja.trailers.MemberTrailer;
import models.jinja.trailers.SubTrailer;
import models.python.*;
import models.python.expressions.*;
import models.python.blocks.*;
import models.python.expressions.MulExpression;
import models.python.literals.*;

import models.python.simple_statements.*;
import models.python.simple_statements.import_lines.MultiImport;
import models.python.simple_statements.import_lines.SingleImport;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

public class PythonVisitor extends pythonParserBaseVisitor<Node> {

    @Override
    public Node visitSimpleStmts(pythonParser.SimpleStmtsContext ctx) {
        if (ctx.simpleStmt().size() > 2) {
            List<Node> statements = new ArrayList<>();

            for (var stmt : ctx.simpleStmt()) {
                Node stmtNode = visit(stmt);
                statements.add(stmtNode);
            }

            BlockNode blockNode = new BlockNode(statements);
            blockNode.setNodeName("simple statements list");
            blockNode.setLineNumber(ctx.getStart().getLine());
            return blockNode;
        } else {
            return this.visit(ctx.simpleStmt(0));
        }
    }

    // simple statements

    @Override
    public Node visitSingleImport(pythonParser.SingleImportContext ctx) {
        Node importedName = this.visit(ctx.name()); // type NameList is returned
        importedName.setNodeName("imported name");

        // check if there is "AS NAME"
        IdType importAlias = null;
        if (ctx.NAME() != null) {
            importAlias = new IdType(ctx.NAME().getText());
            importAlias.setNodeName("alias for imported name");
            importAlias.setLineNumber(ctx.NAME().getSymbol().getLine());
        }

        SingleImport singleImport = new SingleImport(importedName, importAlias);
        singleImport.setNodeName("single import line");
        singleImport.setLineNumber(ctx.getStart().getLine());

        return singleImport;
    }

    @Override
    public Node visitMultiImport(pythonParser.MultiImportContext ctx) {
        Node fromName = this.visit(ctx.name()); // type DotTrailableName is returned
        fromName.setNodeName("multi import from name");

        // getting imported names
        ArrayList<Node> importedNamesList = new ArrayList<>();

        for (int i = 0; i < ctx.NAME().size(); i++) {
            IdType name = new IdType(ctx.NAME(i).getText());
            name.setNodeName("imported name");
            name.setLineNumber(ctx.NAME(i).getSymbol().getLine());

            importedNamesList.add(name);
        }

        MultiImport multiImport = new MultiImport(fromName, importedNamesList);
        multiImport.setNodeName("multi import line");
        multiImport.setLineNumber(ctx.getStart().getLine());

        return multiImport;
    }

    @Override
    public Node visitAssignLine(pythonParser.AssignLineContext ctx) {
        Node target = this.visit(ctx.target());
        target.setNodeName("assign target");
        target.setLineNumber(ctx.target().getStart().getLine());

        Node expr = this.visit(ctx.ternaryExpr());
        expr.setNodeName("assigned expr");
        expr.setLineNumber(ctx.ternaryExpr().getStart().getLine());

        AssignLine assignLine = new AssignLine(target, expr);
        assignLine.setNodeName("assign line");
        assignLine.setLineNumber(ctx.getStart().getLine());

        return assignLine;
    }

    @Override
    public Node visitReturnLine(pythonParser.ReturnLineContext ctx) {
        Node expr = null;

        if (ctx.returnExpr() != null) {
            expr = this.visit(ctx.returnExpr());
            expr.setNodeName("return expr");
            expr.setLineNumber(ctx.returnExpr().getStart().getLine());
        }

        ReturnLine returnLine = new ReturnLine(expr);
        returnLine.setNodeName("returnLine");
        returnLine.setLineNumber(ctx.getStart().getLine());

        return returnLine;
    }

    @Override
    public Node visitTupleReturnWithoutParens(pythonParser.TupleReturnWithoutParensContext ctx) {
        return visitChildren(ctx); // مجرد default behavior
    }

    @Override
    public Node visitSingleReturn(pythonParser.SingleReturnContext ctx) {
        return visitChildren(ctx); // مجرد default behavior
    }

    @Override
    public Node visitExprLine(pythonParser.ExprLineContext ctx) {

        Node expr = visit(ctx.ternaryExpr());
        expr.setNodeName("expr");
        expr.setLineNumber(ctx.ternaryExpr().getStart().getLine());

        ExprLine exprLine = new ExprLine(expr);
        exprLine.setNodeName("expr line");
        exprLine.setLineNumber(ctx.getStart().getLine());

        return exprLine;
    }

    @Override
    public Node visitTernaryExpr(pythonParser.TernaryExprContext ctx) {
        Node trueExpr = visit(ctx.orExpr(0));

        // expr is not a ternary expr
        if (ctx.IF() == null) {
            return trueExpr;
        } else {
            trueExpr.setNodeName("ternary trueExpr");

            Node condition = visit(ctx.orExpr(1));
            trueExpr.setNodeName("ternary condition");

            Node falseExpr = visit(ctx.ternaryExpr());
            trueExpr.setNodeName("ternary falseExpr");

            TernaryExpr ternaryExpr = new TernaryExpr(trueExpr, condition, falseExpr);
            ternaryExpr.setLineNumber(ctx.getStart().getLine());
            ternaryExpr.setNodeName("ternary expr");
            return ternaryExpr;
        }
    }

    @Override
    public Node visitParenthedGenExpr(pythonParser.ParenthedGenExprContext ctx) {
        return this.visit(ctx.genExpr());
    }

    @Override
    public Node visitGenExpr(pythonParser.GenExprContext ctx) {
        Node value = this.visit(ctx.value());

        IdType name = new IdType(ctx.NAME().getText());
        name.setNodeName("id");
        name.setLineNumber(ctx.NAME().getSymbol().getLine());

        Node inExpr = this.visit(ctx.ternaryExpr(0));
        Node ifExpr = null;
        if (ctx.ternaryExpr(1) != null)
            ifExpr = this.visit(ctx.ternaryExpr(1));

        GenExpression genExpr = new GenExpression(value, name, inExpr, ifExpr);
        genExpr.setNodeName("genExpr");
        genExpr.setLineNumber(ctx.start.getLine());
        return genExpr;
    }

    @Override
    public Node visitPass(pythonParser.PassContext ctx) {
        Pass pass = new Pass();
        pass.setNodeName("pass");
        pass.setLineNumber(ctx.PASS().getSymbol().getLine());

        return pass;
    }

    // ================================

    @Override
    public Node visitId(pythonParser.IdContext ctx) {
        IdType idType = new IdType(ctx.NAME().getText());
        idType.setNodeName("id");
        idType.setLineNumber(ctx.NAME().getSymbol().getLine());

        return idType;
    }

    @Override
    public Node visitName(pythonParser.NameContext ctx) {
        Node id = this.visit(ctx.id());

        ArrayList<Node> trailerList = null;
        if (ctx.dotTrailer() != null && !ctx.dotTrailer().isEmpty()) {
            trailerList = new ArrayList<>();

            for (int i = 0; i < ctx.dotTrailer().size(); i++)
                trailerList.add(this.visit(ctx.dotTrailer(i)));
        }

        Name name = new Name(id, trailerList);
        name.setNodeName("name");
        name.setLineNumber(ctx.getStart().getLine());

        return name;
    }

    @Override
    public Node visitValue(pythonParser.ValueContext ctx) {
        Node baseValue = this.visit(ctx.baseValue());
        baseValue.setNodeName("base value");
        baseValue.setLineNumber(ctx.baseValue().getStart().getLine());

        ArrayList<Node> trailers = null;
        if (ctx.valueTrailer() != null && !ctx.valueTrailer().isEmpty()) {
            trailers = new ArrayList<>();

            for (int i = 0; i < ctx.valueTrailer().size(); i++)
                trailers.add(this.visit(ctx.valueTrailer(i)));
        }

        Value value = new Value(baseValue, trailers);
        value.setNodeName("value node");
        value.setLineNumber(ctx.getStart().getLine());

        return value;
    }

    // trailers

    @Override
    public Node visitDotTrailer(pythonParser.DotTrailerContext ctx) {
        Node name = new IdType(ctx.NAME().getText());
        name.setNodeName("id");
        name.setLineNumber(ctx.NAME().getSymbol().getLine());

        MemberTrailer memberTrailer = new MemberTrailer(name);
        memberTrailer.setNodeName("dot trailer");
        memberTrailer.setLineNumber(ctx.DOT().getSymbol().getLine());

        return memberTrailer;
    }

    @Override
    public Node visitSquareTrailer(pythonParser.SquareTrailerContext ctx) {
        Node expr = this.visit(ctx.ternaryExpr());

        SubTrailer subTrailer = new SubTrailer(expr);
        subTrailer.setNodeName("square trailer");
        subTrailer.setLineNumber(ctx.getStart().getLine());

        return subTrailer;
    }

    @Override
    public Node visitCallArgs(pythonParser.CallArgsContext ctx) {
        Node argList = null;

        if (ctx.callList() != null) {
            argList = this.visit(ctx.callList());
        }

        CallTrailer callTrailer = new CallTrailer(argList);
        callTrailer.setNodeName("call trailer");
        callTrailer.setLineNumber(ctx.getStart().getLine());

        return callTrailer;
    }

    @Override
    public Node visitCallList(pythonParser.CallListContext ctx) {
        ArrayList<Node> argList = new ArrayList<>();

        for (int i = 0; i < ctx.callArg().size(); i++)
            argList.add(this.visit(ctx.callArg().get(i)));

        ArgumentList argumentList = new ArgumentList(argList);
        argumentList.setNodeName("call list");
        argumentList.setLineNumber(ctx.getStart().getLine());

        return argumentList;
    }

    @Override
    public Node visitCallArg(pythonParser.CallArgContext ctx) {
        Node expr = this.visit(ctx.ternaryExpr());
        Node argName = null;

        if (ctx.NAME() != null) {
            argName = new IdType(ctx.NAME().getText());
            argName.setNodeName("id");
            argName.setLineNumber(ctx.NAME().getSymbol().getLine());
        }

        Argument argument = new Argument(expr, argName);
        argument.setNodeName("call arg");
        argument.setLineNumber(ctx.getStart().getLine());

        return argument;
    }

    // ===================================
    // expressions

    @Override
    public Node visitOrExpr(pythonParser.OrExprContext ctx) {
        List<pythonParser.AndExprContext> exprListCtx = ctx.andExpr();

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
    public Node visitAndExpr(pythonParser.AndExprContext ctx) {
        // checking if it's an "and" expr
        if (ctx.children.size() > 1) {

            ArrayList<Node> exprList = new ArrayList<>();
            for (int i = 0; i < ctx.equalExpr().size(); i++) {
                exprList.add(this.visit(ctx.equalExpr().get(i)));
            }

            AndExpression andExpression = new AndExpression(exprList);
            andExpression.setNodeName("and expr");
            andExpression.setLineNumber(ctx.getStart().getLine());

            return andExpression;
        }
        // check if remaining expr is "equal" expr
        else {
            return this.visit(ctx.children.getFirst());
        }
    }

    @Override
    public Node visitAddExpr(pythonParser.AddExprContext ctx) {
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
    public Node visitPlusOperator(pythonParser.PlusOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("plus operator");

        return operator;
    }

    @Override
    public Node visitMinusOperator(pythonParser.MinusOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("minus operator");

        return operator;
    }

    @Override
    public Node visitMulExpr(pythonParser.MulExprContext ctx) {

        List<Node> exprList = new ArrayList<>();
        List<Operator> operators = new ArrayList<>();

        // زيارة كل unaryExpr
        for (var unaryCtx : ctx.unaryExpr()) {
            exprList.add(this.visit(unaryCtx));
        }

        // زيارة كل muloperator
        for (var opCtx : ctx.mulOperator()) {
            operators.add((Operator) this.visit(opCtx));
        }

        // إذا لم يوجد operator (single expr فقط)
        if (operators.isEmpty()) return exprList.get(0);

        // إنشاء MulExpression
        models.python.expressions.MulExpression mulExpr = new MulExpression(exprList, operators);
        mulExpr.setNodeName("mul expr");
        mulExpr.setLineNumber(ctx.getStart().getLine());

        return mulExpr;
    }

    @Override
    public Node visitCompareExpr(pythonParser.CompareExprContext ctx) {

        if (ctx.children.size() > 1) {
            ArrayList<Node> exprList = new ArrayList<>();
            ArrayList<Node> optorList = new ArrayList<>();

            int optorCount = ctx.compareOptor().size();

            for (int i = 0; i < optorCount; i++) {
                exprList.add(visit(ctx.addExpr(i)));
                optorList.add(visit(ctx.compareOptor(i)));
            }
            exprList.add(visit(ctx.addExpr(optorCount)));

            CompareExpression compareExpression = new CompareExpression(exprList, optorList);
            compareExpression.setLineNumber(ctx.getStart().getLine());
            return compareExpression;
        } else {
            return this.visit(ctx.addExpr(0));
        }
    }

    @Override
    public Node visitCompareOptor(pythonParser.CompareOptorContext ctx) {
        CompareOperator compareOperator = new CompareOperator(ctx.getText());
        compareOperator.setLineNumber(ctx.getStart().getLine());
        return compareOperator;
    }

    /**
     * A leading + or - sign, e.g. {@code x = -5}.
     *
     * Python had no unary operator at all before this: MINUS existed only as a
     * binary operator inside addExpr, so a bare negative literal was a syntax
     * error. Mirrors NodeVisitor.visitUnaryExpr on the template side.
     */
    @Override
    public Node visitUnaryExpr(pythonParser.UnaryExprContext ctx) {
        if (ctx.unaryExpr() == null) {
            return this.visit(ctx.powExpr());
        }

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
    }

    /** Exponentiation, e.g. {@code x = 2 ** 3}. Right-associative. */
    @Override
    public Node visitPowExpr(pythonParser.PowExprContext ctx) {
        if (ctx.DOUBLESTAR() == null) {
            return this.visit(ctx.singleExpr());
        }

        Node baseValueExpr = this.visit(ctx.singleExpr());
        Node powerValueExpr = this.visit(ctx.unaryExpr());

        PowerExpression powerExpression = new PowerExpression(baseValueExpr, powerValueExpr);
        powerExpression.setNodeName("power expr");
        powerExpression.setLineNumber(ctx.DOUBLESTAR().getSymbol().getLine());

        return powerExpression;
    }

    /**
     * Captures the * / % operator itself, not just its operands.
     *
     * mulOperator has no labelled alternatives, so without this override ANTLR's
     * visitChildren() ran instead and returned null for the whole rule. That
     * null went straight into MulExpression's operator list, so every
     * multiplication, division and modulo in the AST recorded its operands and
     * lost the operator between them - the same fault that was fixed earlier for
     * + and -, left behind on the mul rule because nothing printed these nodes.
     */
    @Override
    public Node visitMulOperator(pythonParser.MulOperatorContext ctx) {
        Operator operator = new Operator(ctx.getText());
        operator.setLineNumber(ctx.getStart().getLine());
        operator.setNodeName("mul operator");

        return operator;
    }

    @Override
    public Node visitEqualExpr(pythonParser.EqualExprContext ctx) {
        if (ctx.children.size() > 1) {
            EqualExpression eqExpr = new EqualExpression();
            eqExpr.setNodeName("equal expr");
            eqExpr.setLineNumber(ctx.getStart().getLine());

            // visit compareExpr
            for (int i = 0; i < ctx.compareExpr().size(); i++) {
                eqExpr.addExpr(visit(ctx.compareExpr(i)));
            }

            // store operators
            for (var opToken : ctx.getTokens(pythonParser.EQUALEQUAL)) eqExpr.addOperator(opToken.getText());
            for (var opToken : ctx.getTokens(pythonParser.NOTEQUAL)) eqExpr.addOperator(opToken.getText());

            return eqExpr;
        } else {
            return visit(ctx.compareExpr(0));
        }
    }

    @Override
    public Node visitNegatedExpr(pythonParser.NegatedExprContext ctx) {
        Node expr = this.visit(ctx.singleExpr());

        NegatedExpression negatedExpression = new NegatedExpression(expr);
        negatedExpression.setLineNumber(ctx.getStart().getLine());

        return negatedExpression;
    }

    // literals
    @Override
    public Node visitInt(pythonParser.IntContext ctx) {
        IntType intType = new IntType(ctx.INT().getText());
        intType.setNodeName("int type");
        intType.setLineNumber(ctx.INT().getSymbol().getLine());

        return intType;
    }

    @Override
    public Node visitFloat(pythonParser.FloatContext ctx) {
        FloatType floatType = new FloatType(ctx.FLOAT().getText());
        floatType.setNodeName("float type");
        floatType.setLineNumber(ctx.FLOAT().getSymbol().getLine());

        return floatType;
    }

    @Override
    public Node visitString(pythonParser.StringContext ctx) {
        StringType stringType = new StringType(ctx.STRING().getText());
        stringType.setNodeName("string type");
        stringType.setLineNumber(ctx.STRING().getSymbol().getLine());

        return stringType;
    }

    @Override
    public Node visitTrue(pythonParser.TrueContext ctx) {
        TrueValue trueValue = new TrueValue();
        trueValue.setNodeName("true");
        trueValue.setLineNumber(ctx.TRUE().getSymbol().getLine());

        return trueValue;
    }

    @Override
    public Node visitFalse(pythonParser.FalseContext ctx) {
        FalseValue falseValue = new FalseValue();
        falseValue.setNodeName("true");
        falseValue.setLineNumber(ctx.FALSE().getSymbol().getLine());

        return falseValue;
    }

    @Override
    public Node visitNone(pythonParser.NoneContext ctx) {
        NoneValue noneValue = new NoneValue();
        noneValue.setNodeName("true");
        noneValue.setLineNumber(ctx.NONE().getSymbol().getLine());

        return noneValue;
    }

    @Override
    public Node visitListVal(pythonParser.ListValContext ctx) {
        ArrayList<Node> exprList = null;

        if (ctx.listItem() != null && !ctx.listItem().isEmpty()) {
            exprList = new ArrayList<>();

            for (int i = 0; i < ctx.listItem().size(); i++)
                exprList.add(this.visit(ctx.listItem(i).ternaryExpr()));
        }

        ListType list = new ListType(exprList);
        list.setNodeName("list type");
        list.setLineNumber(ctx.getStart().getLine());

        return list;
    }

    @Override
    public Node visitDictVal(pythonParser.DictValContext ctx) {
        ArrayList<Node> itemList = null;

        if (ctx.dictItem() != null && !ctx.dictItem().isEmpty()) {
            itemList = new ArrayList<>();

            for (int i = 0; i < ctx.dictItem().size(); i++)
                itemList.add(this.visit(ctx.dictItem(i)));
        }

        Dict dict = new Dict(itemList);
        dict.setNodeName("dictionary literal");
        dict.setLineNumber(ctx.getStart().getLine());

        return dict;
    }

    @Override
    public Node visitDictItem(pythonParser.DictItemContext ctx) {
        Node literal = this.visit(ctx.literal());
        Node expr = this.visit(ctx.ternaryExpr());

        DictItem dictItem = new DictItem(literal, expr);
        dictItem.setNodeName("dict item");
        dictItem.setLineNumber(ctx.getStart().getLine());

        return dictItem;
    }

    @Override
    public Node visitFunc(pythonParser.FuncContext ctx) {

        IdType funcName = new IdType(ctx.NAME().getText());
        funcName.setNodeName("func name");
        funcName.setLineNumber(ctx.NAME().getSymbol().getLine());

        Node decorator = null;
        if (ctx.decorator() != null) {
            decorator = this.visit(ctx.decorator());
            decorator.setNodeName("func decorator");
            decorator.setLineNumber(ctx.decorator().getStart().getLine());
        }

        ArrayList<Node> funcArgs = null;
        if (ctx.funcArgs().argsNames() != null) {
            funcArgs = new ArrayList<>();

            List<TerminalNode> argNameTerList = ctx.funcArgs().argsNames().NAME();
            for (int i = 0; i < argNameTerList.size(); i++) {

                IdType id = new IdType(argNameTerList.get(i).getText());
                id.setNodeName("func arg");
                id.setLineNumber(argNameTerList.get(i).getSymbol().getLine());

                funcArgs.add(id);
            }
        }

        Node funcBlock = null;
        var aaa = ctx.block();

        if (ctx.block() != null) {
            funcBlock = this.visit(ctx.block());
            funcBlock.setNodeName("func body");
        }

        Func func = new Func(decorator, funcName, funcArgs, funcBlock);
        func.setNodeName("func");
        func.setLineNumber(ctx.getStart().getLine());

        return func;
    }

    @Override
    public Node visitDecorator(pythonParser.DecoratorContext ctx) {
        Node name = this.visit(ctx.name());

        List<Node> callArgs = null;

        if (ctx.callArgs() != null && !ctx.callArgs().isEmpty()) {
            callArgs = new ArrayList<>();
            for (var expr : ctx.callArgs().callList().callArg()) {
                callArgs.add(visit(expr));
            }
        }

        Decorator decorator = new Decorator(name, callArgs);
        decorator.setLineNumber(ctx.getStart().getLine());
        return decorator;
    }

    @Override
    public Node visitBlock(pythonParser.BlockContext ctx) {

        List<Node> statements = new ArrayList<>();

        for (var stmt : ctx.stmt()) {
            Node stmtNode = visit(stmt);
            statements.add(stmtNode);
        }

        BlockNode blockNode = new BlockNode(statements);
        blockNode.setNodeName("block");
        blockNode.setLineNumber(ctx.getStart().getLine());

        return blockNode;
    }

    @Override
    public Node visitIfBlock(pythonParser.IfBlockContext ctx) {
        Node ifCondition = this.visit(ctx.ternaryExpr());
        ifCondition.setNodeName("if condition");

        Node ifBody = this.visit(ctx.block());
        ifBody.setNodeName("if body");

        IfBlock ifBlock = new IfBlock(ifCondition, ifBody);
        ifBlock.setLineNumber(ctx.getStart().getLine());

        // getting elif blocks
        if (!ctx.elifBlock().isEmpty()) {
            ArrayList<Node> elifBlockList = new ArrayList<>();

            for (int i = 0; i < ctx.elifBlock().size(); i++) {
                elifBlockList.add(this.visit(ctx.elifBlock(i)));
            }
            ifBlock.setElifBlockList(elifBlockList);
        }

        // getting else block
        if (ctx.elseBlock() != null) {
            Node elseBlock = this.visit(ctx.elseBlock());
            ifBlock.setElseBlock(elseBlock);
        }

        return ifBlock;
    }

    @Override
    public Node visitElifBlock(pythonParser.ElifBlockContext ctx) {
        Node elifCondition = this.visit(ctx.ternaryExpr());
        elifCondition.setNodeName("elif condition");

        Node elifBody = this.visit(ctx.block());
        elifBody.setNodeName("elif body");

        ElifBlock elifBlock = new ElifBlock(elifCondition, elifBody);
        elifBlock.setLineNumber(ctx.getStart().getLine());
        return elifBlock;
    }

    @Override
    public Node visitElseBlock(pythonParser.ElseBlockContext ctx) {
        BlockNode block = (BlockNode) this.visit(ctx.block());
        ElseBlock elseBlock = new ElseBlock(block.statements);
        elseBlock.setLineNumber(ctx.getStart().getLine());
        return elseBlock;
    }

    @Override
    public Node visitForBlock(pythonParser.ForBlockContext ctx) {

        int line = ctx.start.getLine();

        String iterator = ctx.NAME().getText();          // الاسم بعد FOR
        Node iterable = visit(ctx.ternaryExpr());               // expr بعد IN
        BlockNode body = (BlockNode) visit(ctx.block()); // جسم الحلقة

        return new ForNode(line, iterator, iterable, body);
    }

    @Override
    public Node visitWhileBlock(pythonParser.WhileBlockContext ctx) {

        int line = ctx.start.getLine();

        Node condition = visit(ctx.ternaryExpr());               // expr بعد WHILE
        BlockNode body = (BlockNode) visit(ctx.block());  // جسم الحلقة

        return new WhileNode(line, condition, body);
    }


}
