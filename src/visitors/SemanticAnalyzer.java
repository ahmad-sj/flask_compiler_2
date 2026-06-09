package visitors;

import models.App;
import models.Node;
import models.jinja.atoms.IdType;
import models.jinja.atoms.StringType;
import models.jinja.expressions.AddExpression;
import models.jinja.expressions.Argument;
import models.jinja.expressions.ArgumentList;
import models.jinja.trailers.CallTrailer;
import models.jinja.trailers.MemberTrailer;
import models.jinja.trailers.SubTrailer;
import models.python.BlockNode;
import models.python.Decorator;
import models.python.Func;
import models.python.Name;
import models.python.Value;
import models.python.blocks.ElifBlock;
import models.python.blocks.ElseBlock;
import models.python.blocks.ForNode;
import models.python.blocks.IfBlock;
import models.python.blocks.WhileNode;
import models.python.expressions.CompareExpression;
import models.python.expressions.EqualExpression;
import models.python.expressions.GenExpression;
import models.python.simple_statements.AssignLine;
import models.python.simple_statements.ExprLine;
import models.python.simple_statements.ReturnLine;
import models.python.simple_statements.TernaryExpr;
import models.python.simple_statements.import_lines.MultiImport;
import models.python.simple_statements.import_lines.SingleImport;
import symbols.Scope;
import symbols.Symbol;
import symbols.SymbolTable;
import symbols.SemanticError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SemanticAnalyzer {

    private SymbolTable symbolTable;
    private List<SemanticError> errors;
    private Map<String, Symbol> allDefinitions;
    private Set<String> currentFuncParams;
    private boolean insideRouteFunc;

    private static final Set<String> ALWAYS_SAFE = new HashSet<>(Arrays.asList(
            "print", "len", "range", "int", "float", "str", "bool", "list",
            "dict", "tuple", "set", "type", "isinstance", "max", "min",
            "sum", "abs", "round", "enumerate", "zip", "map", "filter",
            "sorted", "reversed", "open", "input", "super", "hasattr",
            "getattr", "setattr", "next", "iter", "any", "all",
            "True", "False", "None", "__name__",
            "Flask"
    ));

    public List<SemanticError> analyze(App app) {
        errors = new ArrayList<>();
        symbolTable = new SymbolTable();
        allDefinitions = new HashMap<>();
        currentFuncParams = null;
        insideRouteFunc = false;
        checkFlaskBootstrap(app);
        for (Node node : app.nodes) {
            analyzeNode(node);
        }
        // Check 7 — Missing Return in Route Function
        for (Node node : app.nodes) {
            if (node instanceof Func) {
                Func func = (Func) node;
                if (isRouteFunc(func)) {
                    if (!containsReturn(func.funcBlock)) {
                        String funcName = null;
                        if (func.funcName instanceof IdType) {
                            funcName = ((IdType) func.funcName).name;
                        }
                        errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, func.getLineNumber(),
                                funcName != null ? funcName : "",
                                "Route '" + (funcName != null ? funcName : "") + "' has no return statement"));
                    }
                }
            }
        }
        // Check 10 — Infinite Recursion (No Base Case)
        for (Node node : app.nodes) {
            if (node instanceof Func) {
                Func func = (Func) node;
                String funcName = null;
                if (func.funcName instanceof IdType) {
                    funcName = ((IdType) func.funcName).name;
                }
                if (funcName != null && containsReturn(func.funcBlock) && callsItself(func.funcBlock, funcName) && !hasIfWithReturn(func.funcBlock)) {
                    errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, func.getLineNumber(), funcName,
                            "Function '" + funcName + "' calls itself with no base case — possible infinite recursion"));
                }
            }
        }
        return errors;
    }

    private void checkFlaskBootstrap(App app) {
        boolean hasFlaskImport = false;
        boolean hasAppInstance = false;
        boolean hasRoute = false;
        Set<String> seenRoutes = new HashSet<>();

        for (Node node : app.nodes) {
            if (node instanceof MultiImport) {
                MultiImport mi = (MultiImport) node;
                if (mi.importedNames != null) {
                    for (Node n : mi.importedNames) {
                        if (n instanceof IdType && "Flask".equals(((IdType) n).name)) {
                            hasFlaskImport = true;
                        }
                    }
                }
            } else if (node instanceof SingleImport) {
                SingleImport si = (SingleImport) node;
                String baseName = extractNameFromNameNode(si.importedName);
                if ("Flask".equals(baseName)) {
                    hasFlaskImport = true;
                }
            } else if (node instanceof AssignLine) {
                AssignLine assign = (AssignLine) node;
                Node target = assign.target;
                Node expr = assign.expr;
                if (target instanceof IdType && expr instanceof Value) {
                    Value val = (Value) expr;
                    Node base = val.baseValue;
                    String baseName = null;
                    if (base instanceof Name) {
                        baseName = ((IdType) ((Name) base).id).name;
                    } else if (base instanceof IdType) {
                        baseName = ((IdType) base).name;
                    }
                    if ("Flask".equals(baseName)) {
                        if (val.trailerList != null) {
                            for (Node t : val.trailerList) {
                                if (t instanceof CallTrailer) {
                                    hasAppInstance = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            } else if (node instanceof Func) {
                Func func = (Func) node;
                if (func.decorator instanceof Decorator) {
                    Decorator dec = (Decorator) func.decorator;
                    if (isRouteDecorator(dec)) {
                        hasRoute = true;
                        String url = extractRouteUrl(dec);
                        if (url != null) {
                            if (seenRoutes.contains(url)) {
                                errors.add(new SemanticError(SemanticError.ErrorType.MISSING_FLASK_VARIABLE, func.getLineNumber(), url,
                                        "Duplicate route '" + url + "' — only the first definition will be used"));
                            } else {
                                seenRoutes.add(url);
                            }
                        }
                    }
                }
            }
        }

        if (!hasFlaskImport) {
            errors.add(new SemanticError(SemanticError.ErrorType.MISSING_FLASK_VARIABLE, 1, "Flask",
                    "Flask is not imported. Add 'from flask import Flask'"));
        }
        if (!hasAppInstance) {
            errors.add(new SemanticError(SemanticError.ErrorType.MISSING_FLASK_VARIABLE, 1, "Flask(__name__)",
                    "No Flask app instance found. Add 'app = Flask(__name__)'"));
        }
        if (!hasRoute) {
            errors.add(new SemanticError(SemanticError.ErrorType.MISSING_FLASK_VARIABLE, 1, "@app.route",
                    "No route defined. Add at least one '@app.route(...)' decorator"));
        }
    }

    private boolean isRouteDecorator(Decorator dec) {
        if (dec.name == null || !(dec.name instanceof Name)) return false;
        Name name = (Name) dec.name;
        if (!(name.id instanceof IdType)) return false;
        String baseName = ((IdType) name.id).name;
        if (!"app".equals(baseName)) return false;
        if (name.trailerList == null || name.trailerList.isEmpty()) return false;
        Node firstTrailer = name.trailerList.get(0);
        if (!(firstTrailer instanceof MemberTrailer)) return false;
        String attr = firstTrailer.toString();
        return attr.startsWith(".") && attr.length() > 1 && "route".equals(attr.substring(1));
    }

    private String extractRouteUrl(Decorator dec) {
        if (dec.callArgs == null || dec.callArgs.isEmpty()) return null;
        Node firstArg = dec.callArgs.get(0);
        if (!(firstArg instanceof Argument)) return null;
        Argument arg = (Argument) firstArg;
        if (arg.expr == null) return null;
        Node expr = arg.expr;
        String raw = null;
        if (expr instanceof StringType) {
            raw = ((StringType) expr).value;
        } else if (expr instanceof Value) {
            Node base = ((Value) expr).baseValue;
            if (base instanceof StringType) {
                raw = ((StringType) base).value;
            }
        }
        if (raw == null) return null;
        if (raw.length() >= 2 && ((raw.startsWith("'") && raw.endsWith("'")) || (raw.startsWith("\"") && raw.endsWith("\"")))) {
            raw = raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private boolean isRouteFunc(Func func) {
        if (!(func.decorator instanceof Decorator)) return false;
        return isRouteDecorator((Decorator) func.decorator);
    }

    private boolean containsReturn(Node node) {
        if (node == null) return false;
        if (node instanceof ReturnLine) return true;
        if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            for (Node stmt : block.statements) {
                if (containsReturn(stmt)) return true;
            }
        } else if (node instanceof IfBlock) {
            IfBlock ifBlock = (IfBlock) node;
            if (containsReturn(ifBlock.body)) return true;
            if (ifBlock.elifBlockList != null) {
                for (Node elif : ifBlock.elifBlockList) {
                    if (containsReturn(elif)) return true;
                }
            }
            if (containsReturn(ifBlock.elseBlock)) return true;
        } else if (node instanceof ElifBlock) {
            ElifBlock elif = (ElifBlock) node;
            if (containsReturn(elif.body)) return true;
        } else if (node instanceof ForNode) {
            ForNode forNode = (ForNode) node;
            if (containsReturn(forNode.getBody())) return true;
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            if (containsReturn(whileNode.getBody())) return true;
        }
        return false;
    }

    private boolean callsItself(Node node, String name) {
        if (node == null) return false;
        if (node instanceof Value) {
            Value val = (Value) node;
            Node base = val.baseValue;
            if (base instanceof IdType && name.equals(((IdType) base).name)) {
                if (val.trailerList != null) {
                    for (Node t : val.trailerList) {
                        if (t instanceof CallTrailer) return true;
                    }
                }
            }
            if (base instanceof Name) {
                if (name.equals(((IdType) ((Name) base).id).name)) {
                    if (val.trailerList != null) {
                        for (Node t : val.trailerList) {
                            if (t instanceof CallTrailer) return true;
                        }
                    }
                }
            }
        }
        if (node instanceof BlockNode) {
            for (Node stmt : ((BlockNode) node).statements) {
                if (callsItself(stmt, name)) return true;
            }
        } else if (node instanceof IfBlock) {
            IfBlock ifBlock = (IfBlock) node;
            if (callsItself(ifBlock.condition, name)) return true;
            if (callsItself(ifBlock.body, name)) return true;
            if (ifBlock.elifBlockList != null) {
                for (Node elif : ifBlock.elifBlockList) {
                    if (callsItself(elif, name)) return true;
                }
            }
            if (callsItself(ifBlock.elseBlock, name)) return true;
        } else if (node instanceof ElifBlock) {
            if (callsItself(((ElifBlock) node).condition, name)) return true;
            if (callsItself(((ElifBlock) node).body, name)) return true;
        } else if (node instanceof ElseBlock) {
            for (Node stmt : ((ElseBlock) node).statements) {
                if (callsItself(stmt, name)) return true;
            }
        } else if (node instanceof ForNode) {
            ForNode forNode = (ForNode) node;
            if (callsItself(forNode.getIterable(), name)) return true;
            if (callsItself(forNode.getBody(), name)) return true;
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            if (callsItself(whileNode.getCondition(), name)) return true;
            if (callsItself(whileNode.getBody(), name)) return true;
        } else if (node instanceof ReturnLine) {
            if (callsItself(((ReturnLine) node).returnExpr, name)) return true;
        } else if (node instanceof ExprLine) {
            if (callsItself(((ExprLine) node).returnExpr, name)) return true;
        } else if (node instanceof AssignLine) {
            if (callsItself(((AssignLine) node).expr, name)) return true;
        } else if (node instanceof AddExpression) {
            for (Node expr : ((AddExpression) node).exprList) {
                if (callsItself(expr, name)) return true;
            }
        } else if (node instanceof CompareExpression) {
            for (Node expr : ((CompareExpression) node).exprList) {
                if (callsItself(expr, name)) return true;
            }
        } else if (node instanceof EqualExpression) {
            for (Node expr : ((EqualExpression) node).getExprList()) {
                if (callsItself(expr, name)) return true;
            }
        } else if (node instanceof GenExpression) {
            GenExpression gen = (GenExpression) node;
            if (callsItself(gen.valueNode, name)) return true;
            if (callsItself(gen.inExpr, name)) return true;
            if (callsItself(gen.ifExpr, name)) return true;
        } else if (node instanceof TernaryExpr) {
            TernaryExpr ternary = (TernaryExpr) node;
            if (callsItself(ternary.trueExpr, name)) return true;
            if (callsItself(ternary.condition, name)) return true;
            if (callsItself(ternary.falseExpr, name)) return true;
        } else if (node instanceof Decorator) {
            Decorator dec = (Decorator) node;
            if (callsItself(dec.name, name)) return true;
            if (dec.callArgs != null) {
                for (Node arg : dec.callArgs) {
                    if (callsItself(arg, name)) return true;
                }
            }
        } else if (node instanceof Argument) {
            if (callsItself(((Argument) node).expr, name)) return true;
        } else if (node instanceof ArgumentList) {
            for (Node arg : ((ArgumentList) node).argList) {
                if (callsItself(arg, name)) return true;
            }
        } else if (node instanceof CallTrailer) {
            if (callsItself(((CallTrailer) node).argList, name)) return true;
        } else if (node instanceof SubTrailer) {
            if (callsItself(((SubTrailer) node).expr, name)) return true;
        } else if (node instanceof Name) {
            if (callsItself(((Name) node).id, name)) return true;
        }
        return false;
    }

    private boolean hasIfWithReturn(Node node) {
        if (node == null) return false;
        if (node instanceof IfBlock) {
            if (containsReturn(node)) return true;
        }
        if (node instanceof BlockNode) {
            for (Node stmt : ((BlockNode) node).statements) {
                if (hasIfWithReturn(stmt)) return true;
            }
        } else if (node instanceof IfBlock) {
            if (hasIfWithReturn(((IfBlock) node).body)) return true;
            if (((IfBlock) node).elifBlockList != null) {
                for (Node elif : ((IfBlock) node).elifBlockList) {
                    if (hasIfWithReturn(elif)) return true;
                }
            }
            if (hasIfWithReturn(((IfBlock) node).elseBlock)) return true;
        } else if (node instanceof ElifBlock) {
            if (hasIfWithReturn(((ElifBlock) node).body)) return true;
        } else if (node instanceof ForNode) {
            if (hasIfWithReturn(((ForNode) node).getBody())) return true;
        } else if (node instanceof WhileNode) {
            if (hasIfWithReturn(((WhileNode) node).getBody())) return true;
        } else if (node instanceof ElseBlock) {
            for (Node stmt : ((ElseBlock) node).statements) {
                if (hasIfWithReturn(stmt)) return true;
            }
        }
        return false;
    }

    private void analyzeNode(Node node) {
        if (node == null) return;

        if (node instanceof AssignLine) {
            AssignLine assign = (AssignLine) node;
            if (assign.target instanceof IdType) {
                String targetName = ((IdType) assign.target).name;
                if (currentFuncParams != null && currentFuncParams.contains(targetName)) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, assign.getLineNumber(), targetName,
                            "'" + targetName + "' shadows a function parameter"));
                    analyzeNode(assign.expr);
                    return;
                }
                try {
                    defineSymbol(targetName, "var", inferType(assign.expr), assign.expr);
                } catch (RuntimeException e) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), targetName,
                            "'" + targetName + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                }
            } else if (assign.target instanceof Name) {
                analyzeNode(assign.target);
            }
            analyzeNode(assign.expr);
        } else if (node instanceof SingleImport) {
            SingleImport imp = (SingleImport) node;
            String nameToDefine = null;
            if (imp.importAlias instanceof IdType) {
                nameToDefine = ((IdType) imp.importAlias).name;
            } else {
                nameToDefine = extractNameFromNameNode(imp.importedName);
            }
            if (nameToDefine != null) {
                try {
                    defineSymbol(nameToDefine, "var", "Node", imp.importedName);
                } catch (RuntimeException e) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), nameToDefine,
                            "'" + nameToDefine + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                }
            }
        } else if (node instanceof MultiImport) {
            MultiImport imp = (MultiImport) node;
            if (imp.importedNames != null) {
                for (Node n : imp.importedNames) {
                    if (n instanceof IdType) {
                        String name = ((IdType) n).name;
                        try {
                            defineSymbol(name, "var", "Node", n);
                        } catch (RuntimeException e) {
                            errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), name,
                                    "'" + name + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                        }
                    }
                }
            }
        } else if (node instanceof Func) {
            Func func = (Func) node;
            String funcName = null;
            if (func.funcName instanceof IdType) {
                funcName = ((IdType) func.funcName).name;
            }
            // Check 11 — Route Function Shadows Builtin
            if (isRouteFunc(func) && funcName != null && ALWAYS_SAFE.contains(funcName)) {
                errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, func.getLineNumber(), funcName,
                        "Route function '" + funcName + "' shadows a Python builtin"));
            }
            if (funcName != null) {
                try {
                    defineSymbol(funcName, "func", "Node", func.funcName);
                    Symbol funcSym = symbolTable.currentScope.resolve(funcName);
                    if (funcSym != null) {
                        funcSym.attributes.put("paramCount", func.funcArgs != null ? func.funcArgs.size() : 0);
                    }
                } catch (RuntimeException e) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), funcName,
                            "'" + funcName + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                }
            }
            symbolTable.enterScope("func:" + (funcName != null ? funcName : "anon"));
            if (func.funcArgs != null) {
                currentFuncParams = new HashSet<>();
                for (Node arg : func.funcArgs) {
                    if (arg instanceof IdType) {
                        String argName = ((IdType) arg).name;
                        currentFuncParams.add(argName);
                        try {
                            defineSymbol(argName, "var", "Node", arg);
                        } catch (RuntimeException e) {
                            errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), argName,
                                    "'" + argName + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                        }
                    }
                }
            }
            if (isRouteFunc(func)) {
                insideRouteFunc = true;
            }
            analyzeNode(func.decorator);
            analyzeNode(func.funcBlock);
            currentFuncParams = null;
            insideRouteFunc = false;
            symbolTable.exitScope();
        } else if (node instanceof Decorator) {
            Decorator dec = (Decorator) node;
            analyzeNode(dec.name);
            if (dec.callArgs != null) {
                for (Node arg : dec.callArgs) {
                    if (arg instanceof Argument) {
                        Argument a = (Argument) arg;
                        analyzeNode(a.expr);
                        // a.argName is a keyword argument parameter name, not a variable read
                    } else {
                        analyzeNode(arg);
                    }
                }
            }
        } else if (node instanceof IfBlock) {
            IfBlock ifBlock = (IfBlock) node;
            analyzeNode(ifBlock.condition);
            symbolTable.enterScope("if:line" + ifBlock.getLineNumber());
            analyzeNode(ifBlock.body);
            symbolTable.exitScope();
            if (ifBlock.elifBlockList != null) {
                for (Node elif : ifBlock.elifBlockList) {
                    analyzeNode(elif);
                }
            }
            analyzeNode(ifBlock.elseBlock);
        } else if (node instanceof ElifBlock) {
            ElifBlock elif = (ElifBlock) node;
            analyzeNode(elif.condition);
            symbolTable.enterScope("elif:line" + elif.getLineNumber());
            analyzeNode(elif.body);
            symbolTable.exitScope();
        } else if (node instanceof ElseBlock) {
            ElseBlock elseBlock = (ElseBlock) node;
            symbolTable.enterScope("else:line" + elseBlock.getLineNumber());
            for (Node stmt : elseBlock.statements) {
                analyzeNode(stmt);
            }
            symbolTable.exitScope();
        } else if (node instanceof ForNode) {
            ForNode forNode = (ForNode) node;
            analyzeNode(forNode.getIterable());
            Node iterableNode = forNode.getIterable();
            if (iterableNode instanceof Value) {
                iterableNode = ((Value) iterableNode).baseValue;
            }
            if (iterableNode instanceof IdType) {
                String iterName = ((IdType) iterableNode).name;
                Symbol iterSym = symbolTable.currentScope.resolve(iterName);
                if (iterSym != null && ("IntType".equals(iterSym.type) || "FloatType".equals(iterSym.type))) {
                    errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, forNode.getLineNumber(), iterName,
                            "'" + iterName + "' is not iterable (type: " + iterSym.type + ")"));
                }
            }
            symbolTable.enterScope("for:line" + forNode.getLineNumber());
            if (forNode.getIterator() != null) {
                try {
                    defineSymbol(forNode.getIterator(), "var", "Node", forNode);
                } catch (RuntimeException e) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), forNode.getIterator(),
                            "'" + forNode.getIterator() + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                }
            }
            analyzeNode(forNode.getBody());
            symbolTable.exitScope();
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            analyzeNode(whileNode.getCondition());
            symbolTable.enterScope("while:line" + whileNode.getLineNumber());
            analyzeNode(whileNode.getBody());
            symbolTable.exitScope();
        } else if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            for (Node stmt : block.statements) {
                analyzeNode(stmt);
            }
            // Check 8 — Unreachable Code After Return
            for (int i = 0; i < block.statements.size(); i++) {
                if (block.statements.get(i) instanceof ReturnLine) {
                    if (i < block.statements.size() - 1) {
                        Node nextStmt = block.statements.get(i + 1);
                        errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, nextStmt.getLineNumber(), "",
                                "Unreachable code after return statement"));
                    }
                    break;
                }
            }
        } else if (node instanceof ExprLine) {
            ExprLine exprLine = (ExprLine) node;
            analyzeNode(exprLine.returnExpr);
        } else if (node instanceof ReturnLine) {
            ReturnLine ret = (ReturnLine) node;
            analyzeNode(ret.returnExpr);
        } else if (node instanceof Value) {
            Value val = (Value) node;
            analyzeValue(val);
        } else if (node instanceof Name) {
            Name name = (Name) node;
            analyzeName(name);
        } else if (node instanceof IdType) {
            IdType id = (IdType) node;
            checkRead(id.name, id);
        } else if (node instanceof AddExpression) {
            AddExpression add = (AddExpression) node;
            for (Node expr : add.exprList) {
                analyzeNode(expr);
            }
            checkTypeMismatchAdd(add);
        } else if (node instanceof CompareExpression) {
            CompareExpression cmp = (CompareExpression) node;
            for (Node expr : cmp.exprList) {
                analyzeNode(expr);
            }
            checkTypeMismatchCompare(cmp);
        } else if (node instanceof EqualExpression) {
            EqualExpression eq = (EqualExpression) node;
            for (Node expr : eq.getExprList()) {
                analyzeNode(expr);
            }
        } else if (node instanceof GenExpression) {
            GenExpression gen = (GenExpression) node;
            analyzeNode(gen.inExpr);
            if (gen.nameNode instanceof IdType) {
                String iterName = ((IdType) gen.nameNode).name;
                try {
                    defineSymbol(iterName, "var", "Node", gen.nameNode);
                } catch (RuntimeException e) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), iterName,
                            "'" + iterName + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                }
            }
            analyzeNode(gen.valueNode);
            analyzeNode(gen.ifExpr);
        } else if (node instanceof TernaryExpr) {
            TernaryExpr ternary = (TernaryExpr) node;
            analyzeNode(ternary.trueExpr);
            analyzeNode(ternary.condition);
            analyzeNode(ternary.falseExpr);
        }
    }

    private void analyzeValue(Value val) {
        Node base = val.baseValue;
        ArrayList<Node> trailers = val.trailerList;

        analyzeNode(base);

        String baseName = null;
        Symbol baseSym = null;
        if (base instanceof Name) {
            baseName = ((IdType) ((Name) base).id).name;
            baseSym = symbolTable.currentScope.resolve(baseName);
        } else if (base instanceof IdType) {
            baseName = ((IdType) base).name;
            baseSym = symbolTable.currentScope.resolve(baseName);
        }

        boolean hasCallTrailer = false;
        boolean subscriptDirectlyOnBase = false;
        int positionalArgCount = 0;

        if (trailers != null) {
            for (int i = 0; i < trailers.size(); i++) {
                Node trailer = trailers.get(i);
                if (trailer instanceof CallTrailer) {
                    hasCallTrailer = true;
                    CallTrailer ct = (CallTrailer) trailer;
                    if (ct.argList instanceof ArgumentList) {
                        ArgumentList al = (ArgumentList) ct.argList;
                        for (Node argNode : al.argList) {
                            if (argNode instanceof Argument) {
                                Argument a = (Argument) argNode;
                                if (a.argName == null) {
                                    positionalArgCount++;
                                }
                                analyzeNode(a.expr);
                                // a.argName is a keyword argument parameter name, not a variable read
                            } else {
                                analyzeNode(argNode);
                            }
                        }
                    } else {
                        analyzeNode(ct.argList);
                    }
                } else if (trailer instanceof SubTrailer) {
                    if (i == 0) {
                        subscriptDirectlyOnBase = true;
                    }
                    SubTrailer st = (SubTrailer) trailer;
                    analyzeNode(st.expr);
                }
            }
        }

        if (baseSym != null) {
            if (hasCallTrailer) {
                if ("var".equals(baseSym.kind) && isPrimitiveType(baseSym.type)) {
                    errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, val.getLineNumber(), baseName,
                            "'" + baseName + "' is not callable (type: " + baseSym.type + ")"));
                }
                // Check 9 — Wrong Argument Count
                if ("func".equals(baseSym.kind)) {
                    int expected = baseSym.attributes.get("paramCount") instanceof Integer
                            ? (Integer) baseSym.attributes.get("paramCount") : 0;
                    if (positionalArgCount != expected) {
                        errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, val.getLineNumber(), baseName,
                                "'" + baseName + "' expects " + expected + " argument(s), got " + positionalArgCount));
                    }
                }
                // Check 12 — redirect Without url_for
                if ("redirect".equals(baseName)) {
                    Node argExpr = extractFirstPositionalArgFromValue(val);
                    if (argExpr != null && isStringLiteral(argExpr)) {
                        errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, val.getLineNumber(), "redirect",
                                "redirect() called with a string literal — use url_for() instead"));
                    }
                }
                // Check 14 — app.run() Inside a Route
                if (insideRouteFunc && isAppRun(val)) {
                    errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, val.getLineNumber(), "app.run",
                            "app.run() called inside a route function — move it to 'if __name__ == __main__'"));
                }
            }
            if (subscriptDirectlyOnBase) {
                if (baseSym.type != null && !"Node".equals(baseSym.type) && !"unknown".equals(baseSym.type)) {
                    if (!"ListType".equals(baseSym.type) && !"DictType".equals(baseSym.type) && !"StringType".equals(baseSym.type)) {
                        errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, val.getLineNumber(), baseName,
                                "'" + baseName + "' does not support subscript access (type: " + baseSym.type + ")"));
                    }
                }
            }
        }
    }

    private Node extractFirstPositionalArgFromValue(Value val) {
        if (val.trailerList == null) return null;
        for (Node trailer : val.trailerList) {
            if (trailer instanceof CallTrailer) {
                CallTrailer ct = (CallTrailer) trailer;
                if (ct.argList instanceof ArgumentList) {
                    ArgumentList al = (ArgumentList) ct.argList;
                    for (Node argNode : al.argList) {
                        if (argNode instanceof Argument) {
                            Argument a = (Argument) argNode;
                            if (a.argName == null) {
                                return a.expr;
                            }
                        }
                    }
                }
                return null;
            }
        }
        return null;
    }

    private boolean isStringLiteral(Node node) {
        if (node instanceof StringType) return true;
        if (node instanceof Value) {
            Node base = ((Value) node).baseValue;
            if (base instanceof StringType) return true;
        }
        return false;
    }

    private boolean isAppRun(Value val) {
        Node base = val.baseValue;
        String baseName = null;
        if (base instanceof Name) {
            baseName = ((IdType) ((Name) base).id).name;
        } else if (base instanceof IdType) {
            baseName = ((IdType) base).name;
        }
        if (!"app".equals(baseName)) return false;
        if (val.trailerList == null) return false;
        boolean foundRun = false;
        boolean foundCall = false;
        for (Node t : val.trailerList) {
            if (t instanceof MemberTrailer) {
                String attr = t.toString();
                if (attr.startsWith(".") && attr.length() > 1 && "run".equals(attr.substring(1))) {
                    foundRun = true;
                }
            } else if (t instanceof CallTrailer) {
                foundCall = true;
            }
        }
        return foundRun && foundCall;
    }

    private void analyzeName(Name name) {
        String baseName = ((IdType) name.id).name;
        checkRead(baseName, name);
    }

    private void checkRead(String name, Node node) {
        if (ALWAYS_SAFE.contains(name)) {
            return;
        }
        Symbol sym = symbolTable.currentScope.resolve(name);
        if (sym == null) {
            Symbol definedElsewhere = allDefinitions.get(name);
            if (definedElsewhere != null) {
                checkScopeError(name, definedElsewhere, node);
            } else {
                errors.add(new SemanticError(SemanticError.ErrorType.UNDEFINED_VARIABLE, node.getLineNumber(), name,
                        "Variable '" + name + "' is not defined"));
            }
        } else {
            checkScopeError(name, sym, node);
        }
    }

    private void defineSymbol(String name, String kind, String type, Node value) {
        symbolTable.currentScope.define(name, kind, type, value);
        Symbol sym = symbolTable.currentScope.resolve(name);
        if (sym != null) {
            allDefinitions.put(name, sym);
        }
    }

    private void checkScopeError(String name, Symbol sym, Node node) {
        int defDepth = getDepth(sym.scope);
        if (defDepth > 1) {
            int curDepth = getDepth(symbolTable.currentScope);
            if (curDepth <= defDepth && !isAncestorOrSame(sym.scope, symbolTable.currentScope)) {
                errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), name,
                        "'" + name + "' used outside its defining scope '" + sym.scope.name + "'"));
            }
        }
    }

    private String extractNameFromNameNode(Node node) {
        if (node instanceof Name) {
            return ((IdType) ((Name) node).id).name;
        } else if (node instanceof IdType) {
            return ((IdType) node).name;
        }
        return null;
    }

    private String inferType(Node node) {
        if (node instanceof IdType) {
            Symbol sym = symbolTable.currentScope.resolve(((IdType) node).name);
            if (sym != null && sym.type != null) {
                return sym.type;
            }
            return "unknown";
        }
        if (node instanceof Value) {
            return inferType(((Value) node).baseValue);
        }
        if (node instanceof Name) {
            return inferType(((Name) node).id);
        }
        return node.getClass().getSimpleName();
    }

    private boolean isPrimitiveType(String type) {
        return "IntType".equals(type) || "FloatType".equals(type) || "StringType".equals(type)
                || "TrueValue".equals(type) || "FalseValue".equals(type) || "NoneValue".equals(type);
    }

    private int getDepth(Scope scope) {
        int depth = 0;
        Scope s = scope;
        while (s.parent != null) {
            depth++;
            s = s.parent;
        }
        return depth;
    }

    private boolean isAncestorOrSame(Scope ancestor, Scope descendant) {
        Scope s = descendant;
        while (s != null) {
            if (s == ancestor) {
                return true;
            }
            s = s.parent;
        }
        return false;
    }

    private void checkTypeMismatchAdd(AddExpression add) {
        ArrayList<Node> exprs = add.exprList;
        if (exprs == null || exprs.size() < 2) return;
        for (int i = 0; i < exprs.size(); i++) {
            String t1 = inferType(exprs.get(i));
            for (int j = i + 1; j < exprs.size(); j++) {
                String t2 = inferType(exprs.get(j));
                if ("StringType".equals(t1) && ("IntType".equals(t2) || "FloatType".equals(t2))) {
                    errors.add(new SemanticError(SemanticError.ErrorType.TYPE_MISMATCH, add.getLineNumber(), "",
                            "Cannot add 'StringType' and '" + t2 + "'"));
                } else if ("StringType".equals(t2) && ("IntType".equals(t1) || "FloatType".equals(t1))) {
                    errors.add(new SemanticError(SemanticError.ErrorType.TYPE_MISMATCH, add.getLineNumber(), "",
                            "Cannot add 'StringType' and '" + t1 + "'"));
                }
            }
        }
    }

    private void checkTypeMismatchCompare(CompareExpression cmp) {
        ArrayList<Node> exprs = cmp.exprList;
        ArrayList<Node> optors = cmp.optorList;
        if (exprs == null || optors == null || exprs.size() < 2) return;
        for (int i = 0; i < optors.size(); i++) {
            String op = optors.get(i).toString().trim();
            if ("<".equals(op) || ">".equals(op) || "<=".equals(op) || ">=".equals(op)) {
                String left = inferType(exprs.get(i));
                String right = inferType(exprs.get(i + 1));
                if ("StringType".equals(left) && ("IntType".equals(right) || "FloatType".equals(right))) {
                    errors.add(new SemanticError(SemanticError.ErrorType.TYPE_MISMATCH, cmp.getLineNumber(), "",
                            "Cannot compare 'StringType' with '" + right + "'"));
                } else if ("StringType".equals(right) && ("IntType".equals(left) || "FloatType".equals(left))) {
                    errors.add(new SemanticError(SemanticError.ErrorType.TYPE_MISMATCH, cmp.getLineNumber(), "",
                            "Cannot compare 'StringType' with '" + left + "'"));
                }
            }
        }
    }
}
