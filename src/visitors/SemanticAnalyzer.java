package visitors;

import models.App;
import models.Node;
import models.jinja.atoms.IdType;
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
        checkFlaskBootstrap(app);
        for (Node node : app.nodes) {
            analyzeNode(node);
        }
        return errors;
    }

    private void checkFlaskBootstrap(App app) {
        boolean hasFlaskImport = false;
        boolean hasAppInstance = false;
        boolean hasRoute = false;

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
                    Node decName = dec.name;
                    if (decName instanceof Name) {
                        Name name = (Name) decName;
                        String baseName = ((IdType) name.id).name;
                        if ("app".equals(baseName) && name.trailerList != null && !name.trailerList.isEmpty()) {
                            Node firstTrailer = name.trailerList.get(0);
                            if (firstTrailer instanceof MemberTrailer) {
                                String attr = firstTrailer.toString();
                                if (attr.startsWith(".") && attr.length() > 1) {
                                    String attrName = attr.substring(1);
                                    if ("route".equals(attrName)) {
                                        hasRoute = true;
                                    }
                                }
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

    private void analyzeNode(Node node) {
        if (node == null) return;

        if (node instanceof AssignLine) {
            AssignLine assign = (AssignLine) node;
            if (assign.target instanceof IdType) {
                String targetName = ((IdType) assign.target).name;
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
                try {
                    defineSymbol(funcName, "var", "Node", func.funcName);
                } catch (RuntimeException e) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), funcName,
                            "'" + funcName + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                }
            }
            symbolTable.enterScope("func:" + (funcName != null ? funcName : "anon"));
            if (func.funcArgs != null) {
                for (Node arg : func.funcArgs) {
                    if (arg instanceof IdType) {
                        String argName = ((IdType) arg).name;
                        try {
                            defineSymbol(argName, "var", "Node", arg);
                        } catch (RuntimeException e) {
                            errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), argName,
                                    "'" + argName + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                        }
                    }
                }
            }
            analyzeNode(func.decorator);
            analyzeNode(func.funcBlock);
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

        if (trailers != null) {
            for (int i = 0; i < trailers.size(); i++) {
                Node trailer = trailers.get(i);
                if (trailer instanceof CallTrailer) {
                    hasCallTrailer = true;
                    CallTrailer ct = (CallTrailer) trailer;
                    if (ct.argList instanceof ArgumentList) {
                        ArgumentList al = (ArgumentList) ct.argList;
                        for (Node arg : al.argList) {
                            if (arg instanceof Argument) {
                                Argument a = (Argument) arg;
                                analyzeNode(a.expr);
                                // a.argName is a keyword argument parameter name, not a variable read
                            } else {
                                analyzeNode(arg);
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
