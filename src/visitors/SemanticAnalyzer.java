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

/**
 * SemanticAnalyzer performs a complete static analysis of the AST built by the parser.
 * It runs 14 semantic checks (type, scope, Flask-specific, and structural) and returns
 * a list of SemanticError objects for each violation found.
 *
 * The analyzer uses a SymbolTable to track variable and function definitions across
 * nested scopes, so it can detect undefined references, scope violations, and redefinitions.
 */
public class SemanticAnalyzer {

    // ─── Symbol table & state ─────────────────────────────────────────────

    /** Tracks the current symbol table with global and nested scopes. */
    private SymbolTable symbolTable;

    /** Accumulates all semantic errors found during analysis. */
    private List<SemanticError> errors;

    /**
     * Registry of every symbol ever defined anywhere in the file.
     * Used to distinguish "undefined" from "defined in a sibling scope".
     */
    private Map<String, Symbol> allDefinitions;

    /** Set of parameter names for the function currently being analyzed. Null when outside any function. */
    private Set<String> currentFuncParams;

    /** True when the walker is inside the body of a route-decorated function. */
    private boolean insideRouteFunc;

    // ─── Whitelist ─────────────────────────────────────────────────────────

    /**
     * Names that are always considered safe (builtins and special identifiers).
     * These do NOT need to be explicitly imported or defined in the symbol table.
     * "Flask" is included because the analyzer checks for it via import scanning,
     * but once imported it should be resolvable without triggering UNDEFINED_VARIABLE.
     */
    private static final Set<String> ALWAYS_SAFE = new HashSet<>(Arrays.asList(
            "print", "len", "range", "int", "float", "str", "bool", "list",
            "dict", "tuple", "set", "type", "isinstance", "max", "min",
            "sum", "abs", "round", "enumerate", "zip", "map", "filter",
            "sorted", "reversed", "open", "input", "super", "hasattr",
            "getattr", "setattr", "next", "iter", "any", "all",
            "True", "False", "None", "__name__",
            "Flask"
    ));

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Main entry point: analyzes the entire program and returns all discovered errors.
     *
     * @param app the root App node containing all top-level statements
     * @return list of SemanticError; empty list means the program is semantically valid
     */
    public List<SemanticError> analyze(App app) {
        // Initialize fresh state for each analysis run
        errors = new ArrayList<>();
        symbolTable = new SymbolTable();
        allDefinitions = new HashMap<>();
        currentFuncParams = null;
        insideRouteFunc = false;

        // ── Check 1–6: Flask bootstrap checks (imports, app instance, routes, duplicates)
        checkFlaskBootstrap(app);

        // Walk every top-level node and build the symbol table while checking semantics
        for (Node node : app.nodes) {
            analyzeNode(node);
        }

        // ── Check 7: Missing Return in Route Function
        // We scan top-level functions after the full walk because the symbol table
        // is already built; this avoids false negatives if the function is defined
        // after its first use.
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

        // ── Check 10: Infinite Recursion (No Base Case)
        // Only runs after the full walk so that all function definitions are known.
        for (Node node : app.nodes) {
            if (node instanceof Func) {
                Func func = (Func) node;
                String funcName = null;
                if (func.funcName instanceof IdType) {
                    funcName = ((IdType) func.funcName).name;
                }
                // Guard: skip functions without any return to avoid double-reporting with Check 7.
                if (funcName != null && containsReturn(func.funcBlock) && callsItself(func.funcBlock, funcName) && !hasIfWithReturn(func.funcBlock)) {
                    errors.add(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, func.getLineNumber(), funcName,
                            "Function '" + funcName + "' calls itself with no base case — possible infinite recursion"));
                }
            }
        }

        return errors;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CHECKS 1–6: FLASK BOOTSTRAP (imports, app instance, routes, duplicates)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Scans the top-level program nodes for Flask-specific structural requirements:
     *   1) Flask must be imported.
     *   2) A Flask app instance must be created (e.g., app = Flask(__name__)).
     *   3) At least one route must be defined.
     *   4) Duplicate routes are detected and reported.
     *
     * This is a pre-scan that runs before the main AST walk so that Flask errors
     * are collected first and independently of the symbol table.
     *
     * @param app the root App node
     */
    private void checkFlaskBootstrap(App app) {
        boolean hasFlaskImport = false;
        boolean hasAppInstance = false;
        boolean hasRoute = false;
        Set<String> seenRoutes = new HashSet<>();  // tracks route URLs to detect duplicates

        for (Node node : app.nodes) {
            // ── Detect "from flask import Flask" ──
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
            }
            // ── Detect "app = Flask(__name__)" ──
            else if (node instanceof AssignLine) {
                AssignLine assign = (AssignLine) node;
                Node target = assign.target;
                Node expr = assign.expr;
                // Pattern: target is an identifier and expr is a value starting with "Flask"
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
                        // Look for the call trailer "(...)" to confirm it's an instance creation
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
            }
            // ── Detect route definitions and check for duplicates ──
            else if (node instanceof Func) {
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

        // Report missing bootstrap elements (all reported at line 1 because they are file-level)
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

    /**
     * Determines whether a decorator is a route decorator (e.g., @app.route('/')).
     * The pattern we match is: base name "app", followed by a member trailer "route".
     *
     * @param dec the Decorator node to inspect
     * @return true if this decorator is a route decorator
     */
    private boolean isRouteDecorator(Decorator dec) {
        if (dec.name == null || !(dec.name instanceof Name)) return false;
        Name name = (Name) dec.name;
        if (!(name.id instanceof IdType)) return false;
        String baseName = ((IdType) name.id).name;
        if (!"app".equals(baseName)) return false;
        if (name.trailerList == null || name.trailerList.isEmpty()) return false;
        Node firstTrailer = name.trailerList.get(0);
        if (!(firstTrailer instanceof MemberTrailer)) return false;
        String attr = firstTrailer.toString();   // e.g., ".route"
        return attr.startsWith(".") && attr.length() > 1 && "route".equals(attr.substring(1));
    }

    /**
     * Extracts the URL string from the first argument of a route decorator.
     * The parser stores string literals with surrounding quotes, so we strip them.
     *
     * @param dec the route decorator
     * @return the raw URL string, or null if not found / not a string literal
     */
    private String extractRouteUrl(Decorator dec) {
        if (dec.callArgs == null || dec.callArgs.isEmpty()) return null;
        Node firstArg = dec.callArgs.get(0);
        if (!(firstArg instanceof Argument)) return null;
        Argument arg = (Argument) firstArg;
        if (arg.expr == null) return null;
        Node expr = arg.expr;
        String raw = null;
        // The argument may be a bare StringType or a Value wrapping a StringType
        if (expr instanceof StringType) {
            raw = ((StringType) expr).value;
        } else if (expr instanceof Value) {
            Node base = ((Value) expr).baseValue;
            if (base instanceof StringType) {
                raw = ((StringType) base).value;
            }
        }
        if (raw == null) return null;
        // Strip surrounding quotes (single or double) to get the clean URL
        if (raw.length() >= 2 && ((raw.startsWith("'") && raw.endsWith("'")) || (raw.startsWith("\"") && raw.endsWith("\"")))) {
            raw = raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    /**
     * Helper: returns true if the given function has a route decorator.
     *
     * @param func the Func node to check
     * @return true if the function is decorated with @app.route
     */
    private boolean isRouteFunc(Func func) {
        if (!(func.decorator instanceof Decorator)) return false;
        return isRouteDecorator((Decorator) func.decorator);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AST HELPERS: RETURN / RECURSION / UNREACHABLE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Recursively searches a node tree for any ReturnLine.
     * Used by Check 7 (Missing Return) and Check 10 (Infinite Recursion).
     *
     * @param node the AST node to search
     * @return true if the subtree contains at least one return statement
     */
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

    /**
     * Recursively searches the AST subtree to see if a function calls itself.
     * The pattern is: a Value node whose base name matches the function name
     * and has a CallTrailer (parentheses) somewhere in its trailer chain.
     *
     * @param node the AST node to search
     * @param name the function name to look for
     * @return true if the function calls itself within the given subtree
     */
    private boolean callsItself(Node node, String name) {
        if (node == null) return false;
        // Direct call: value node with the function name as base and a call trailer
        if (node instanceof Value) {
            Value val = (Value) node;
            Node base = val.baseValue;
            // Case 1: base is an IdType (e.g., index())
            if (base instanceof IdType && name.equals(((IdType) base).name)) {
                if (val.trailerList != null) {
                    for (Node t : val.trailerList) {
                        if (t instanceof CallTrailer) return true;
                    }
                }
            }
            // Case 2: base is a Name (e.g., app.index()) — we still match the inner id
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
        // Recurse into all child containers
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

    /**
     * Recursively searches for an IfBlock that contains a return statement.
     * An if-with-return is considered a "base case" for the infinite recursion check.
     *
     * @param node the AST node to search
     * @return true if any if/elif/else block in the subtree contains a return
     */
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

    // ═══════════════════════════════════════════════════════════════════════
    //  MAIN AST WALKER
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * The core recursive AST walker. Every node type is dispatched here.
     * Responsibilities:
     *   - Build the symbol table (define variables, functions, imports, loop iterators)
     *   - Enter/exit scopes for blocks, functions, if/else, for, while
     *   - Recurse into sub-expressions
     *   - Trigger type checks at the appropriate AST levels
     *
     * @param node the current AST node to analyze
     */
    private void analyzeNode(Node node) {
        if (node == null) return;

        // ── AssignLine ──
        //   Defines a new variable in the current scope.
        //   Check 13: if the target shadows a function parameter, emit SCOPE_ERROR.
        //   Check 5: if the target is already defined in the current scope, emit SCOPE_ERROR.
        if (node instanceof AssignLine) {
            AssignLine assign = (AssignLine) node;
            if (assign.target instanceof IdType) {
                String targetName = ((IdType) assign.target).name;
                // Check 13 — Parameter Shadowing
                if (currentFuncParams != null && currentFuncParams.contains(targetName)) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, assign.getLineNumber(), targetName,
                            "'" + targetName + "' shadows a function parameter"));
                    analyzeNode(assign.expr);   // still recurse into the expression
                    return;                     // skip the define to avoid double-error
                }
                // Define the symbol (may throw if already defined in current scope)
                try {
                    defineSymbol(targetName, "var", inferType(assign.expr), assign.expr);
                } catch (RuntimeException e) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), targetName,
                            "'" + targetName + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                }
            } else if (assign.target instanceof Name) {
                // e.g., obj.attr = value — analyze the left-hand side as a read
                analyzeNode(assign.target);
            }
            analyzeNode(assign.expr);   // always recurse into the right-hand side
        }
        // ── SingleImport ──
        //   e.g., import flask  or  import flask as f
        //   Registers the imported name (or alias) in the current scope.
        else if (node instanceof SingleImport) {
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
        }
        // ── MultiImport ──
        //   e.g., from flask import Flask, render_template
        //   Registers each imported name in the current scope.
        else if (node instanceof MultiImport) {
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
        }
        // ── Func ──
        //   Defines the function name in the current scope, then enters a new scope
        //   for the function body and registers its parameters.
        //   Check 11: route function names must not shadow Python builtins.
        else if (node instanceof Func) {
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
            // Define the function symbol in the current scope
            if (funcName != null) {
                try {
                    defineSymbol(funcName, "func", "Node", func.funcName);
                    // Store parameter count so Check 9 can verify call arity later
                    Symbol funcSym = symbolTable.currentScope.resolve(funcName);
                    if (funcSym != null) {
                        funcSym.attributes.put("paramCount", func.funcArgs != null ? func.funcArgs.size() : 0);
                    }
                } catch (RuntimeException e) {
                    errors.add(new SemanticError(SemanticError.ErrorType.SCOPE_ERROR, node.getLineNumber(), funcName,
                            "'" + funcName + "' already defined in scope '" + symbolTable.currentScope.name + "'"));
                }
            }
            // Enter the function's local scope
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
            // Exit the function scope and reset function-specific state
            currentFuncParams = null;
            insideRouteFunc = false;
            symbolTable.exitScope();
        }
        // ── Decorator ──
        //   Recurse into the decorator name and its arguments.
        else if (node instanceof Decorator) {
            Decorator dec = (Decorator) node;
            analyzeNode(dec.name);
            if (dec.callArgs != null) {
                for (Node arg : dec.callArgs) {
                    if (arg instanceof Argument) {
                        Argument a = (Argument) arg;
                        analyzeNode(a.expr);
                        // a.argName is a keyword name (e.g., methods=...), not a variable read
                    } else {
                        analyzeNode(arg);
                    }
                }
            }
        }
        // ── IfBlock ──
        //   The condition is analyzed in the current scope; the body gets its own scope.
        else if (node instanceof IfBlock) {
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
        }
        // ── ElifBlock ──
        //   Same scoping rules as IfBlock.
        else if (node instanceof ElifBlock) {
            ElifBlock elif = (ElifBlock) node;
            analyzeNode(elif.condition);
            symbolTable.enterScope("elif:line" + elif.getLineNumber());
            analyzeNode(elif.body);
            symbolTable.exitScope();
        }
        // ── ElseBlock ──
        //   Gets its own scope so variables defined inside it stay local.
        else if (node instanceof ElseBlock) {
            ElseBlock elseBlock = (ElseBlock) node;
            symbolTable.enterScope("else:line" + elseBlock.getLineNumber());
            for (Node stmt : elseBlock.statements) {
                analyzeNode(stmt);
            }
            symbolTable.exitScope();
        }
        // ── ForNode ──
        //   Check 4: the iterable must not be an IntType or FloatType.
        //   The iterator variable is defined inside the for-loop's local scope.
        else if (node instanceof ForNode) {
            ForNode forNode = (ForNode) node;
            analyzeNode(forNode.getIterable());
            // Unwrap Value wrappers to reach the underlying identifier
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
        }
        // ── WhileNode ──
        //   The loop body gets its own scope.
        else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            analyzeNode(whileNode.getCondition());
            symbolTable.enterScope("while:line" + whileNode.getLineNumber());
            analyzeNode(whileNode.getBody());
            symbolTable.exitScope();
        }
        // ── BlockNode ──
        //   Recurse into every statement.
        //   Check 8: Unreachable Code After Return — if a return appears, every statement after it is unreachable.
        else if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            for (Node stmt : block.statements) {
                analyzeNode(stmt);
            }
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
        }
        // ── ExprLine ──
        //   A standalone expression (often a function call). Just recurse into it.
        else if (node instanceof ExprLine) {
            ExprLine exprLine = (ExprLine) node;
            analyzeNode(exprLine.returnExpr);
        }
        // ── ReturnLine ──
        //   Recurse into the return expression to catch undefined variables / type errors.
        else if (node instanceof ReturnLine) {
            ReturnLine ret = (ReturnLine) node;
            analyzeNode(ret.returnExpr);
        }
        // ── Value ──
        //   The most complex node: may represent a call, a subscript, or a simple read.
        //   Delegates to analyzeValue() for detailed inspection.
        else if (node instanceof Value) {
            Value val = (Value) node;
            analyzeValue(val);
        }
        // ── Name ──
        //   A dotted name (e.g., app.route). The base identifier is treated as a read.
        else if (node instanceof Name) {
            Name name = (Name) node;
            analyzeName(name);
        }
        // ── IdType ──
        //   A bare identifier (e.g., x). Treated as a read.
        else if (node instanceof IdType) {
            IdType id = (IdType) node;
            checkRead(id.name, id);
        }
        // ── AddExpression ──
        //   Check 2: Type Mismatch in Addition — cannot add String + Int/Float.
        else if (node instanceof AddExpression) {
            AddExpression add = (AddExpression) node;
            for (Node expr : add.exprList) {
                analyzeNode(expr);
            }
            checkTypeMismatchAdd(add);
        }
        // ── CompareExpression ──
        //   Check 3: Type Mismatch in Comparison — cannot compare String < Int/Float.
        else if (node instanceof CompareExpression) {
            CompareExpression cmp = (CompareExpression) node;
            for (Node expr : cmp.exprList) {
                analyzeNode(expr);
            }
            checkTypeMismatchCompare(cmp);
        }
        // ── EqualExpression ──
        //   No custom check yet, but recurse into sub-expressions.
        else if (node instanceof EqualExpression) {
            EqualExpression eq = (EqualExpression) node;
            for (Node expr : eq.getExprList()) {
                analyzeNode(expr);
            }
        }
        // ── GenExpression ──
        //   Generator expressions introduce a local variable (the loop iterator).
        else if (node instanceof GenExpression) {
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
        }
        // ── TernaryExpr ──
        //   Recurse into all three branches.
        else if (node instanceof TernaryExpr) {
            TernaryExpr ternary = (TernaryExpr) node;
            analyzeNode(ternary.trueExpr);
            analyzeNode(ternary.condition);
            analyzeNode(ternary.falseExpr);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VALUE ANALYSIS (calls, subscripts, trailers)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Analyzes a Value node, which may represent a function call, a subscript access,
     * or a combination of trailers (e.g., obj.method()[0]).
     *
     * Checks performed here:
     *   Check 1: Type Error — Call on a primitive variable
     *   Check 3: Type Error — Subscript on an unsupported type
     *   Check 9: Wrong Argument Count
     *   Check 12: redirect Without url_for
     *   Check 14: app.run() Inside a Route
     *
     * @param val the Value node to analyze
     */
    private void analyzeValue(Value val) {
        Node base = val.baseValue;
        ArrayList<Node> trailers = val.trailerList;

        // Analyze the base expression first (may trigger reads / defines)
        analyzeNode(base);

        // Try to resolve the base name in the symbol table
        String baseName = null;
        Symbol baseSym = null;
        if (base instanceof Name) {
            baseName = ((IdType) ((Name) base).id).name;
            baseSym = symbolTable.currentScope.resolve(baseName);
        } else if (base instanceof IdType) {
            baseName = ((IdType) base).name;
            baseSym = symbolTable.currentScope.resolve(baseName);
        }

        boolean hasCallTrailer = false;          // tracks if there is a "(...)" trailer
        boolean subscriptDirectlyOnBase = false; // tracks if the first trailer is a subscript
        int positionalArgCount = 0;              // counts positional arguments for arity checks

        // Walk the trailer chain to gather metadata
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
                                // a.argName is a keyword argument name, not a variable read
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

        // If the base was resolved in the symbol table, perform symbol-aware checks
        if (baseSym != null) {
            if (hasCallTrailer) {
                // Check 1 — Type Error: Call on a primitive variable
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
            // Check 3 — Type Error: Subscript on a primitive type
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

    /**
     * Extracts the first positional argument expression from a Value node that has a CallTrailer.
     * Used by Check 12 (redirect without url_for).
     *
     * @param val the Value node containing a call trailer
     * @return the first positional Argument's expr, or null if not found
     */
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

    /**
     * Determines whether a node is a string literal (StringType or a Value wrapping one).
     * Used by Check 12 to detect redirect("/path") instead of redirect(url_for(...)).
     *
     * @param node the node to inspect
     * @return true if the node represents a string literal
     */
    private boolean isStringLiteral(Node node) {
        if (node instanceof StringType) return true;
        if (node instanceof Value) {
            Node base = ((Value) node).baseValue;
            if (base instanceof StringType) return true;
        }
        return false;
    }

    /**
     * Detects whether a Value node represents an app.run(...) call.
     * Pattern: base name "app" with a member trailer "run" and a call trailer.
     *
     * @param val the Value node to inspect
     * @return true if the node is an app.run() call
     */
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
                String attr = t.toString();  // e.g., ".run"
                if (attr.startsWith(".") && attr.length() > 1 && "run".equals(attr.substring(1))) {
                    foundRun = true;
                }
            } else if (t instanceof CallTrailer) {
                foundCall = true;
            }
        }
        return foundRun && foundCall;
    }

    /**
     * Analyzes a Name node (dotted access like app.route).
     * The base identifier is treated as a variable read.
     *
     * @param name the Name node to analyze
     */
    private void analyzeName(Name name) {
        String baseName = ((IdType) name.id).name;
        checkRead(baseName, name);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCOPE & SYMBOL HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Checks whether a variable read is valid.
     *   1) If the name is in the ALWAYS_SAFE whitelist, skip.
     *   2) Try to resolve the name in the current scope chain.
     *   3) If not found, check allDefinitions to see if it was defined in a sibling scope
     *      (which would be a SCOPE_ERROR rather than UNDEFINED_VARIABLE).
     *   4) If truly not defined anywhere, report UNDEFINED_VARIABLE.
     *
     * @param name the identifier being read
     * @param node the AST node where the read occurs (used for line number)
     */
    private void checkRead(String name, Node node) {
        if (ALWAYS_SAFE.contains(name)) {
            return;
        }
        Symbol sym = symbolTable.currentScope.resolve(name);
        if (sym == null) {
            // Not found in current scope chain — but was it defined elsewhere?
            Symbol definedElsewhere = allDefinitions.get(name);
            if (definedElsewhere != null) {
                checkScopeError(name, definedElsewhere, node);
            } else {
                errors.add(new SemanticError(SemanticError.ErrorType.UNDEFINED_VARIABLE, node.getLineNumber(), name,
                        "Variable '" + name + "' is not defined"));
            }
        } else {
            // Found in scope chain — still verify it wasn't defined in a sibling block
            checkScopeError(name, sym, node);
        }
    }

    /**
     * Defines a symbol in the current scope and registers it in the global allDefinitions map.
     * The map is used later to distinguish "undefined" from "defined in a sibling scope".
     *
     * @param name the symbol name
     * @param kind the symbol kind ("var", "func", etc.)
     * @param type the inferred type (e.g., "IntType", "StringType")
     * @param value the AST node representing the value
     */
    private void defineSymbol(String name, String kind, String type, Node value) {
        symbolTable.currentScope.define(name, kind, type, value);
        Symbol sym = symbolTable.currentScope.resolve(name);
        if (sym != null) {
            allDefinitions.put(name, sym);
        }
    }

    /**
     * Checks whether a variable is being used outside its defining scope.
     * In this analyzer, any scope deeper than the global scope (depth > 1) is considered
     * a "sibling block" if the current scope is not a descendant of the definition scope.
     *
     * @param name the variable name
     * @param sym the Symbol where the variable was originally defined
     * @param node the node where the read occurs
     */
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

    /**
     * Extracts a plain string name from a Name or IdType node.
     * Used when importing to determine what identifier to register.
     *
     * @param node the node to extract a name from
     * @return the plain name string, or null if the node is neither Name nor IdType
     */
    private String extractNameFromNameNode(Node node) {
        if (node instanceof Name) {
            return ((IdType) ((Name) node).id).name;
        } else if (node instanceof IdType) {
            return ((IdType) node).name;
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TYPE INFERENCE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Infers the type of an AST node for type-checking purposes.
     *   - IdType: look up the symbol in the current scope chain
     *   - Value/Name: unwrap to the underlying base value
     *   - Everything else: return the class name (e.g., "IntType", "StringType")
     *
     * @param node the node whose type we want to infer
     * @return a string representing the type, or "unknown" if unresolved
     */
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

    /**
     * Determines whether a type string represents a primitive (non-callable, non-subscriptable) type.
     *
     * @param type the type string to test
     * @return true if the type is primitive
     */
    private boolean isPrimitiveType(String type) {
        return "IntType".equals(type) || "FloatType".equals(type) || "StringType".equals(type)
                || "TrueValue".equals(type) || "FalseValue".equals(type) || "NoneValue".equals(type);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SCOPE GEOMETRY HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Computes the nesting depth of a scope (0 = global, 1 = first child, etc.).
     *
     * @param scope the scope to measure
     * @return the depth in the scope tree
     */
    private int getDepth(Scope scope) {
        int depth = 0;
        Scope s = scope;
        while (s.parent != null) {
            depth++;
            s = s.parent;
        }
        return depth;
    }

    /**
     * Checks whether 'ancestor' is the same as 'descendant' or any of its ancestors.
     *
     * @param ancestor the potential ancestor scope
     * @param descendant the scope to test
     * @return true if ancestor is on descendant's parent chain
     */
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

    // ═══════════════════════════════════════════════════════════════════════
    //  CHECK 2 & 3: TYPE MISMATCH DETECTORS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Check 2: Type Mismatch in Addition.
     * Detects illegal combinations such as String + Int or String + Float.
     * Python would allow "a" + 1 at runtime, but for a Flask compiler we flag it.
     *
     * @param add the AddExpression node to inspect
     */
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

    /**
     * Check 3: Type Mismatch in Comparison.
     * Detects illegal comparisons with ordering operators (<, >, <=, >=)
     * between String and numeric types.
     *
     * @param cmp the CompareExpression node to inspect
     */
    private void checkTypeMismatchCompare(CompareExpression cmp) {
        ArrayList<Node> exprs = cmp.exprList;
        ArrayList<Node> optors = cmp.optorList;
        if (exprs == null || optors == null || exprs.size() < 2) return;
        for (int i = 0; i < optors.size(); i++) {
            String op = optors.get(i).toString().trim();
            // Only ordering operators are problematic across string/numeric
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
