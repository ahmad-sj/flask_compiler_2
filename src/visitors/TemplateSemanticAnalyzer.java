package visitors;

import models.Node;
import models.NodeBody;
import models.RouteInfo;
import models.Template;
import models.html.attributes.QuotedAttribute;
import models.html.elements.HtmlElement;
import models.html.elements.HtmlRegularElement;
import models.jinja.JinjaExpression;
import models.jinja.atoms.DictType;
import models.jinja.atoms.IdType;
import models.jinja.atoms.ListType;
import models.jinja.atoms.PairType;
import models.jinja.blocks.ElifBlock;
import models.jinja.blocks.ElseBlock;
import models.jinja.blocks.ExtendsBlock;
import models.jinja.blocks.ForBlock;
import models.jinja.blocks.IfBlock;
import models.jinja.blocks.InheritedBlock;
import models.jinja.blocks.SetStatement;
import models.jinja.expressions.AddExpression;
import models.jinja.expressions.AndExpression;
import models.jinja.expressions.Argument;
import models.jinja.expressions.ArgumentList;
import models.jinja.expressions.ComparisonExpression;
import models.jinja.expressions.ConcatExpression;
import models.jinja.expressions.DefaultExpression;
import models.jinja.expressions.FilterExpression;
import models.jinja.expressions.InExpression;
import models.jinja.expressions.IsExpression;
import models.jinja.expressions.MulExpression;
import models.jinja.expressions.NotExpression;
import models.jinja.expressions.OrExpression;
import models.jinja.expressions.ParenthedExpression;
import models.jinja.expressions.PipeExpression;
import models.jinja.expressions.PowerExpression;
import models.jinja.expressions.PrimaryExpression;
import models.jinja.expressions.TernaryExpression;
import models.jinja.expressions.UnaryExpression;
import models.jinja.trailers.CallTrailer;
import symbols.SemanticError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Semantic analysis for templates.
 *
 * SemanticAnalyzer only ever sees the Python AST, so nothing checked the
 * templates against the data the backend actually passes them. A template could
 * reference a variable no route supplies, call url_for on a route that does not
 * exist, extend a missing base, or pipe through an unknown filter, and the build
 * would report success while emitting a broken or empty page.
 *
 * Each template is analysed once per route that renders it, because the context
 * a template sees is the context of the route rendering it. The {% extends %}
 * chain is followed so a base template is checked with the child's context,
 * which is exactly how the renderer resolves it.
 */
public class TemplateSemanticAnalyzer {

    /** Names Jinja resolves itself, never supplied by the backend. */
    private static final Set<String> BUILTIN_NAMES = new HashSet<>(Arrays.asList(
            "loop", "true", "false", "none", "True", "False", "None"));

    private final Map<String, Template> templates;
    private final List<RouteInfo> routes;
    private final Set<String> moduleVarNames;

    private final List<SemanticError> errors = new ArrayList<>();
    private final Set<String> reported = new LinkedHashSet<>();
    private final Set<String> routeNames = new LinkedHashSet<>();

    /** Template currently being walked, for error messages. */
    private String currentTemplate;

    public TemplateSemanticAnalyzer(Map<String, Template> templates,
                                    List<RouteInfo> routes,
                                    Set<String> moduleVarNames) {
        this.templates = templates;
        this.routes = routes;
        this.moduleVarNames = moduleVarNames;
        for (RouteInfo route : routes) routeNames.add(route.name);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════

    public List<SemanticError> analyze() {
        for (RouteInfo route : routes) {
            if (!route.rendersTemplate()) continue;

            Template template = templates.get(route.templateName);
            if (template == null) {
                // Previously only a generation warning, so a typo'd template
                // name still produced a "successful" build with a page missing.
                report(new SemanticError(SemanticError.ErrorType.MISSING_FLASK_VARIABLE,
                        route.line, route.templateName,
                        "Route '" + route.name + "' renders template '" + route.templateName
                                + "', which does not exist in templates/"));
                continue;
            }
            analyzeTemplateForRoute(template, route);
        }

        // Templates nothing renders are still parsed; check their extends target
        // so a broken base is reported even if no route reaches it yet.
        for (Map.Entry<String, Template> entry : templates.entrySet()) {
            currentTemplate = entry.getKey();
            checkExtendsTarget(entry.getValue());
        }
        return errors;
    }

    /** Walks a template plus its inheritance chain with one route's context. */
    private void analyzeTemplateForRoute(Template template, RouteInfo route) {
        Set<String> context = new LinkedHashSet<>(moduleVarNames);
        context.addAll(route.context.keySet());
        // A parameterized route binds the selected item under its own name.
        if (route.itemVarName != null) context.add(route.itemVarName);

        Set<String> visited = new HashSet<>();
        Template current = template;
        String currentName = route.templateName;

        // Walk child first, then each ancestor, all with the same context.
        while (current != null && visited.add(currentName)) {
            currentTemplate = currentName;
            walk(current.nodes, new LinkedHashSet<>(context));

            ExtendsBlock extends_ = findExtends(current);
            if (extends_ == null) break;

            String parentName = ExpressionEvaluator.unquote(extends_.templateName);
            Template parent = templates.get(parentName);
            if (parent == null) {
                report(new SemanticError(SemanticError.ErrorType.MISSING_FLASK_VARIABLE,
                        extends_.getLineNumber(), parentName,
                        "Template '" + currentName + "' extends '" + parentName
                                + "', which does not exist"));
                break;
            }
            current = parent;
            currentName = parentName;
        }
    }

    /** Reports a broken {% extends %} even in a template no route renders. */
    private void checkExtendsTarget(Template template) {
        ExtendsBlock extends_ = findExtends(template);
        if (extends_ == null) return;
        String parentName = ExpressionEvaluator.unquote(extends_.templateName);
        if (!templates.containsKey(parentName)) {
            report(new SemanticError(SemanticError.ErrorType.MISSING_FLASK_VARIABLE,
                    extends_.getLineNumber(), parentName,
                    "Template '" + currentTemplate + "' extends '" + parentName
                            + "', which does not exist"));
        }
    }

    private ExtendsBlock findExtends(Template template) {
        if (template == null || template.nodes == null) return null;
        for (Node node : template.nodes) {
            if (node instanceof ExtendsBlock) return (ExtendsBlock) node;
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NODE WALK
    // ═══════════════════════════════════════════════════════════════════════

    /** scope is the set of names visible here; blocks extend a copy of it. */
    private void walk(List<Node> nodes, Set<String> scope) {
        if (nodes == null) return;
        for (Node node : nodes) walkNode(node, scope);
    }

    private void walkNode(Node node, Set<String> scope) {
        if (node == null) return;

        if (node instanceof JinjaExpression) {
            checkExpression(((JinjaExpression) node).expression, scope, node.getLineNumber());

        } else if (node instanceof NodeBody) {
            walk(((NodeBody) node).nodeList, scope);

        } else if (node instanceof IfBlock) {
            IfBlock block = (IfBlock) node;
            checkExpression(block.condition, scope, block.getLineNumber());
            walkNode(block.nodeBody, new LinkedHashSet<>(scope));

        } else if (node instanceof ElifBlock) {
            ElifBlock block = (ElifBlock) node;
            checkExpression(block.condition, scope, block.getLineNumber());
            walkNode(block.nodeBody, new LinkedHashSet<>(scope));

        } else if (node instanceof ElseBlock) {
            walkNode(((ElseBlock) node).nodeBody, new LinkedHashSet<>(scope));

        } else if (node instanceof ForBlock) {
            walkFor((ForBlock) node, scope);

        } else if (node instanceof InheritedBlock) {
            walkNode(((InheritedBlock) node).nodeBody, new LinkedHashSet<>(scope));

        } else if (node instanceof SetStatement) {
            SetStatement set = (SetStatement) node;
            checkExpression(set.expr, scope, set.getLineNumber());
            // Visible from this point on, so add after checking the value.
            String name = nameOf(set.id);
            if (name != null) scope.add(name);

        } else if (node instanceof HtmlElement) {
            walkHtml((HtmlElement) node, scope);
        }
        // ExtendsBlock, NormalText and DocType carry nothing to check.
    }

    /**
     * The loop variables exist only inside the body. Using one after
     * {% endfor %} is a real error the renderer would silently render as blank.
     */
    private void walkFor(ForBlock block, Set<String> scope) {
        checkExpression(block.iterable, scope, block.getLineNumber());

        Set<String> inner = new LinkedHashSet<>(scope);
        if (block.loopVars != null) {
            for (Node var : block.loopVars) {
                String name = nameOf(var);
                if (name != null) inner.add(name);
            }
        }
        inner.add("loop");
        walkNode(block.nodeBody, inner);
    }

    private void walkHtml(HtmlElement element, Set<String> scope) {
        if (element.attrList != null) {
            for (Node attr : element.attrList) {
                if (!(attr instanceof QuotedAttribute)) continue;
                List<Node> values = ((QuotedAttribute) attr).attrValList;
                if (values == null) continue;
                for (Node value : values) {
                    if (value instanceof JinjaExpression) {
                        checkExpression(((JinjaExpression) value).expression, scope,
                                value.getLineNumber());
                    }
                }
            }
        }
        if (element instanceof HtmlRegularElement) {
            walkNode(((HtmlRegularElement) element).elementBody, scope);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  EXPRESSIONS
    // ═══════════════════════════════════════════════════════════════════════

    private void checkExpression(Node expr, Set<String> scope, int line) {
        if (expr == null) return;

        if (expr instanceof IdType) {
            checkName(((IdType) expr).name, scope, line);

        } else if (expr instanceof PrimaryExpression) {
            checkPrimary((PrimaryExpression) expr, scope, line);

        } else if (expr instanceof PipeExpression) {
            PipeExpression pipe = (PipeExpression) expr;
            checkExpression(pipe.expr, scope, line);
            if (pipe.filterList != null) {
                for (Node filter : pipe.filterList) checkFilter(filter, scope, line);
            }

        } else if (expr instanceof AddExpression) {
            for (Node e : ((AddExpression) expr).exprList) checkExpression(e, scope, line);
        } else if (expr instanceof MulExpression) {
            for (Node e : ((MulExpression) expr).exprList) checkExpression(e, scope, line);
        } else if (expr instanceof AndExpression) {
            for (Node e : ((AndExpression) expr).exprList) checkExpression(e, scope, line);
        } else if (expr instanceof OrExpression) {
            for (Node e : ((OrExpression) expr).exprList) checkExpression(e, scope, line);
        } else if (expr instanceof ConcatExpression) {
            for (Node e : ((ConcatExpression) expr).exprList) checkExpression(e, scope, line);

        } else if (expr instanceof ComparisonExpression) {
            ComparisonExpression cmp = (ComparisonExpression) expr;
            checkExpression(cmp.expr1, scope, line);
            checkExpression(cmp.expr2, scope, line);
        } else if (expr instanceof InExpression) {
            InExpression in = (InExpression) expr;
            checkExpression(in.expr1, scope, line);
            checkExpression(in.expr2, scope, line);
        } else if (expr instanceof PowerExpression) {
            PowerExpression pow = (PowerExpression) expr;
            checkExpression(pow.baseValueExpr, scope, line);
            checkExpression(pow.powerValueExpr, scope, line);
        } else if (expr instanceof TernaryExpression) {
            TernaryExpression t = (TernaryExpression) expr;
            checkExpression(t.condExpr, scope, line);
            checkExpression(t.trueExpr, scope, line);
            checkExpression(t.falseExpr, scope, line);
        } else if (expr instanceof DefaultExpression) {
            DefaultExpression d = (DefaultExpression) expr;
            checkExpression(d.expr, scope, line);
            checkExpression(d.defaultExpr, scope, line);

        } else if (expr instanceof NotExpression) {
            checkExpression(((NotExpression) expr).expression, scope, line);
        } else if (expr instanceof ParenthedExpression) {
            checkExpression(((ParenthedExpression) expr).expr, scope, line);
        } else if (expr instanceof UnaryExpression) {
            checkExpression(((UnaryExpression) expr).expr, scope, line);

        } else if (expr instanceof IsExpression) {
            IsExpression is = (IsExpression) expr;
            checkExpression(is.expr, scope, line);
            String test = nameOf(is.id);
            if (test != null && !ExpressionEvaluator.SUPPORTED_TESTS.contains(test)) {
                report(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, line, test,
                        at() + "unknown test 'is " + test + "'"));
            }

        } else if (expr instanceof ListType) {
            ListType list = (ListType) expr;
            if (list.itemList != null) {
                for (Node item : list.itemList) checkExpression(item, scope, line);
            }
        } else if (expr instanceof DictType) {
            DictType dict = (DictType) expr;
            if (dict.pairsList != null) {
                for (Node pair : dict.pairsList) {
                    if (pair instanceof PairType) {
                        checkExpression(((PairType) pair).expr2, scope, line);
                    }
                }
            }
        }
        // Literals need no checking.
    }

    /**
     * Checks a name/member/call chain. Only the base name is resolved against
     * the context; member names after a dot are data keys, not variables.
     */
    private void checkPrimary(PrimaryExpression primary, Set<String> scope, int line) {
        boolean isCall = primary.trailerList != null
                && !primary.trailerList.isEmpty()
                && primary.trailerList.get(0) instanceof CallTrailer;

        if (primary.atom instanceof IdType && isCall) {
            checkCall(((IdType) primary.atom).name,
                    (CallTrailer) primary.trailerList.get(0), scope, line);
        } else {
            checkExpression(primary.atom, scope, line);
        }

        if (primary.trailerList == null) return;
        for (Node trailer : primary.trailerList) {
            if (trailer instanceof CallTrailer && trailer != primary.trailerList.get(0)) {
                checkArguments((CallTrailer) trailer, scope, line);
            } else if (trailer instanceof models.jinja.trailers.SubTrailer) {
                checkExpression(((models.jinja.trailers.SubTrailer) trailer).expr, scope, line);
            }
        }
    }

    private void checkCall(String funcName, CallTrailer call, Set<String> scope, int line) {
        if (!ExpressionEvaluator.SUPPORTED_FUNCTIONS.contains(funcName)) {
            report(new SemanticError(SemanticError.ErrorType.UNDEFINED_VARIABLE, line, funcName,
                    at() + "call to unknown function '" + funcName + "()'"));
            return;
        }
        if ("url_for".equals(funcName)) checkUrlFor(call, line);
        checkArguments(call, scope, line);
    }

    /** url_for's first argument must name a route the backend actually defines. */
    private void checkUrlFor(CallTrailer call, int line) {
        List<Node> args = argumentsOf(call);
        if (args.isEmpty()) {
            report(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, line, "url_for",
                    at() + "url_for() requires a route name"));
            return;
        }
        Argument first = (Argument) args.get(0);
        if (first.argName != null) return; // keyword-only call; nothing to resolve

        String routeName = literalString(first.expr);
        if (routeName == null) return; // computed at render time, cannot check

        if (!routeNames.contains(routeName)) {
            report(new SemanticError(SemanticError.ErrorType.MISSING_FLASK_VARIABLE, line, routeName,
                    at() + "url_for('" + routeName + "') refers to no route defined in app.py"));
        }
    }

    private void checkArguments(CallTrailer call, Set<String> scope, int line) {
        for (Node node : argumentsOf(call)) {
            checkExpression(((Argument) node).expr, scope, line);
        }
    }

    private void checkFilter(Node filterNode, Set<String> scope, int line) {
        String name;
        if (filterNode instanceof FilterExpression) {
            FilterExpression filter = (FilterExpression) filterNode;
            name = nameOf(filter.filterName);
            if (filter.argList instanceof ArgumentList) {
                ArgumentList list = (ArgumentList) filter.argList;
                if (list.argList != null) {
                    for (Node n : list.argList) {
                        if (n instanceof Argument) checkExpression(((Argument) n).expr, scope, line);
                    }
                }
            }
        } else {
            name = nameOf(filterNode);
        }
        if (name != null && !ExpressionEvaluator.SUPPORTED_FILTERS.contains(name)) {
            report(new SemanticError(SemanticError.ErrorType.TYPE_ERROR, line, name,
                    at() + "unknown filter '" + name + "'. Supported: "
                            + ExpressionEvaluator.SUPPORTED_FILTERS));
        }
    }

    private void checkName(String name, Set<String> scope, int line) {
        if (name == null || BUILTIN_NAMES.contains(name) || scope.contains(name)) return;
        report(new SemanticError(SemanticError.ErrorType.UNDEFINED_VARIABLE, line, name,
                at() + "'" + name + "' is not provided to this template"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /** Deduplicates: one template is analysed once per route that renders it. */
    private void report(SemanticError error) {
        if (reported.add(error.toString())) errors.add(error);
    }

    private String at() {
        return currentTemplate == null ? "" : currentTemplate + ": ";
    }

    private List<Node> argumentsOf(CallTrailer call) {
        List<Node> out = new ArrayList<>();
        if (call.argList instanceof ArgumentList) {
            ArgumentList list = (ArgumentList) call.argList;
            if (list.argList != null) {
                for (Node n : list.argList) if (n instanceof Argument) out.add(n);
            }
        }
        return out;
    }

    private String literalString(Node node) {
        if (node instanceof models.jinja.atoms.StringType) {
            return ExpressionEvaluator.unquote(((models.jinja.atoms.StringType) node).value);
        }
        if (node instanceof PrimaryExpression) {
            PrimaryExpression primary = (PrimaryExpression) node;
            if (primary.trailerList == null || primary.trailerList.isEmpty()) {
                return literalString(primary.atom);
            }
        }
        return null;
    }

    private String nameOf(Node node) {
        if (node instanceof IdType) return ((IdType) node).name;
        return node == null ? null : node.toString();
    }
}
