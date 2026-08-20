package visitors;

import models.App;
import models.Node;
import models.RouteInfo;
import models.jinja.atoms.FloatType;
import models.jinja.atoms.IdType;
import models.jinja.atoms.IntType;
import models.jinja.atoms.ListType;
import models.jinja.atoms.StringType;
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
import models.python.expressions.EqualExpression;
import models.python.expressions.GenExpression;
import models.python.literals.Dict;
import models.python.literals.DictItem;
import models.python.literals.FalseValue;
import models.python.literals.NoneValue;
import models.python.literals.TrueValue;
import models.python.simple_statements.AssignLine;
import models.python.simple_statements.ReturnLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the data-preparation results from the Python AST: module-level
 * values, and one RouteInfo per @app.route function.
 *
 * This is the "generator" stage of the pipeline. It works purely on the AST and
 * deliberately does not consult the symbol table, which belongs to semantic
 * analysis.
 *
 * Two things changed from the earlier version. Dict literals are now walked as
 * AST nodes instead of being printed with toString() and re-parsed by splitting
 * on ", " (which corrupted any string value containing a comma). And routes are
 * described generically instead of the generator hardcoding the names
 * "product", "products" and "product_id".
 */
public class PythonDataExtractor {

    /** Matches a Flask URL parameter such as &lt;int:product_id&gt; or &lt;name&gt;. */
    private static final Pattern URL_PARAM = Pattern.compile("<(?:[^:>]+:)?([^>]+)>");

    private final Map<String, Object> moduleVars = new LinkedHashMap<>();
    private final List<RouteInfo> routes = new ArrayList<>();

    public void extract(App app) {
        // Module-level assignments first, so routes can resolve names against them.
        for (Node node : app.nodes) {
            if (node instanceof AssignLine) {
                AssignLine assign = (AssignLine) node;
                String name = nameOf(assign.target);
                if (name != null) moduleVars.put(name, toJavaValue(assign.expr));
            }
        }
        for (Node node : app.nodes) {
            if (node instanceof Func) extractRoute((Func) node);
        }
    }

    public Map<String, Object> getModuleVars() {
        return moduleVars;
    }

    public List<RouteInfo> getRoutes() {
        return routes;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ROUTES
    // ═══════════════════════════════════════════════════════════════════════

    private void extractRoute(Func func) {
        if (!(func.decorator instanceof Decorator)) return;
        Decorator decorator = (Decorator) func.decorator;
        if (!isRouteDecorator(decorator)) return;

        RouteInfo route = new RouteInfo();
        route.name = nameOf(func.funcName);
        route.line = func.getLineNumber();
        route.urlPattern = firstStringArg(decorator);

        if (route.name == null) return;

        if (route.urlPattern != null) {
            Matcher m = URL_PARAM.matcher(route.urlPattern);
            while (m.find()) route.params.add(m.group(1));
        }

        List<Node> body = statementsOf(func.funcBlock);
        findRenderTemplate(route, body);
        if (route.isParameterized()) findItemSelection(route, body);

        routes.add(route);
    }

    private boolean isRouteDecorator(Decorator decorator) {
        if (!(decorator.name instanceof Name)) return false;
        Name name = (Name) decorator.name;
        if (!"app".equals(nameOf(name.id))) return false;
        if (name.trailerList == null || name.trailerList.isEmpty()) return false;
        Node first = name.trailerList.get(0);
        if (!(first instanceof MemberTrailer)) return false;
        return "route".equals(memberName((MemberTrailer) first));
    }

    private String firstStringArg(Decorator decorator) {
        if (decorator.callArgs == null || decorator.callArgs.isEmpty()) return null;
        Node first = decorator.callArgs.get(0);
        if (!(first instanceof Argument)) return null;
        Object value = toJavaValue(((Argument) first).expr);
        return value == null ? null : String.valueOf(value);
    }

    /** Finds the return render_template(...) call and records template + context. */
    private void findRenderTemplate(RouteInfo route, List<Node> body) {
        for (Node stmt : body) {
            if (!(stmt instanceof ReturnLine)) continue;
            Node expr = ((ReturnLine) stmt).returnExpr;
            if (!(expr instanceof Value)) continue;

            Value value = (Value) expr;
            if (!"render_template".equals(baseName(value))) continue;

            CallTrailer call = firstCallTrailer(value.trailerList);
            if (call == null) continue;

            for (Node argNode : argumentsOf(call)) {
                Argument arg = (Argument) argNode;
                if (arg.argName == null) {
                    Object name = toJavaValue(arg.expr);
                    if (name != null) route.templateName = String.valueOf(name);
                } else {
                    route.context.put(nameOf(arg.argName), toJavaValue(arg.expr));
                }
            }
            return;
        }
    }

    /**
     * Works out what a parameterized route enumerates, from code shaped like:
     *
     *   product = next((p for p in products if p["id"] == product_id), None)
     *
     * which yields collection "products", item variable "product" and key "id".
     * Nothing here is specific to products; it reads the generator expression.
     */
    private void findItemSelection(RouteInfo route, List<Node> body) {
        for (Node stmt : body) {
            if (!(stmt instanceof AssignLine)) continue;
            AssignLine assign = (AssignLine) stmt;

            GenExpression gen = findGenExpression(assign.expr, 0);
            if (gen == null) continue;

            String collection = nameOf(unwrap(gen.inExpr));
            if (collection == null || !(moduleVars.get(collection) instanceof List)) continue;

            route.collectionName = collection;
            route.itemVarName = nameOf(assign.target);
            route.itemKeyName = matchedKey(gen.ifExpr, route.params);
            if (route.itemKeyName != null) return;
        }
    }

    /** Depth-limited search for a generator expression anywhere in a value. */
    private GenExpression findGenExpression(Node node, int depth) {
        if (node == null || depth > 6) return null;
        if (node instanceof GenExpression) return (GenExpression) node;

        if (node instanceof Value) {
            Value value = (Value) node;
            GenExpression found = findGenExpression(value.baseValue, depth + 1);
            if (found != null) return found;
            if (value.trailerList != null) {
                for (Node trailer : value.trailerList) {
                    if (trailer instanceof CallTrailer) {
                        for (Node arg : argumentsOf((CallTrailer) trailer)) {
                            found = findGenExpression(((Argument) arg).expr, depth + 1);
                            if (found != null) return found;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * From a filter like {@code p["id"] == product_id}, returns "id" when the
     * other side of the comparison is one of the route's URL parameters.
     */
    private String matchedKey(Node ifExpr, List<String> params) {
        if (!(ifExpr instanceof EqualExpression)) return null;
        EqualExpression equality = (EqualExpression) ifExpr;
        List<Node> operands = equality.getExprList();
        if (operands.size() < 2) return null;

        String key = null;
        boolean matchesParam = false;
        for (Node operand : operands) {
            String subscript = subscriptKey(operand);
            if (subscript != null) key = subscript;
            String plain = nameOf(unwrap(operand));
            if (plain != null && params.contains(plain)) matchesParam = true;
        }
        return matchesParam ? key : null;
    }

    /** Returns the literal key of an expression like {@code p["id"]}. */
    private String subscriptKey(Node node) {
        Node unwrapped = node instanceof Value ? node : null;
        if (unwrapped == null) return null;
        Value value = (Value) unwrapped;
        if (value.trailerList == null) return null;
        for (Node trailer : value.trailerList) {
            if (trailer instanceof SubTrailer) {
                Object key = toJavaValue(((SubTrailer) trailer).expr);
                if (key != null) return String.valueOf(key);
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AST  ->  JAVA VALUES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Converts a literal AST node into a plain Java value.
     * Returns null for anything not statically knowable, which the generator
     * treats as "not available at build time".
     */
    private Object toJavaValue(Node node) {
        if (node == null) return null;

        if (node instanceof StringType) return stripQuotes(((StringType) node).value);
        if (node instanceof IntType)    return ((IntType) node).value;
        if (node instanceof FloatType)  return ((FloatType) node).value;
        if (node instanceof TrueValue)  return Boolean.TRUE;
        if (node instanceof FalseValue) return Boolean.FALSE;
        if (node instanceof NoneValue)  return null;

        if (node instanceof ListType) {
            List<Object> out = new ArrayList<>();
            ListType list = (ListType) node;
            if (list.itemList != null) {
                for (Node item : list.itemList) out.add(toJavaValue(item));
            }
            return out;
        }

        // Walk dict entries directly rather than printing and re-parsing them.
        if (node instanceof Dict) {
            Map<String, Object> out = new LinkedHashMap<>();
            Dict dict = (Dict) node;
            if (dict.itemList != null) {
                for (Node item : dict.itemList) {
                    if (!(item instanceof DictItem)) continue;
                    DictItem entry = (DictItem) item;
                    Object key = toJavaValue(entry.literal);
                    if (key != null) out.put(String.valueOf(key), toJavaValue(entry.expr));
                }
            }
            return out;
        }

        if (node instanceof IdType) {
            String name = ((IdType) node).name;
            return moduleVars.containsKey(name) ? moduleVars.get(name) : null;
        }

        if (node instanceof Name) {
            Name name = (Name) node;
            if (name.trailerList == null || name.trailerList.isEmpty()) {
                return toJavaValue(name.id);
            }
            return null;
        }

        if (node instanceof Value) {
            Value value = (Value) node;
            if (value.trailerList == null || value.trailerList.isEmpty()) {
                return toJavaValue(value.baseValue);
            }
            return null; // A call or member access is not a static literal.
        }

        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  SMALL HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private List<Node> statementsOf(Node block) {
        if (block instanceof BlockNode && ((BlockNode) block).statements != null) {
            return ((BlockNode) block).statements;
        }
        return new ArrayList<>();
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

    private CallTrailer firstCallTrailer(List<Node> trailers) {
        if (trailers == null) return null;
        for (Node trailer : trailers) {
            if (trailer instanceof CallTrailer) return (CallTrailer) trailer;
        }
        return null;
    }

    private String baseName(Value value) {
        return nameOf(value.baseValue);
    }

    /** Unwraps a Value/Name shell to reach the identifier underneath. */
    private Node unwrap(Node node) {
        if (node instanceof Value) {
            Value value = (Value) node;
            if (value.trailerList == null || value.trailerList.isEmpty()) {
                return unwrap(value.baseValue);
            }
        }
        if (node instanceof Name) {
            Name name = (Name) node;
            if (name.trailerList == null || name.trailerList.isEmpty()) return unwrap(name.id);
        }
        return node;
    }

    private String nameOf(Node node) {
        if (node == null) return null;
        if (node instanceof IdType) return ((IdType) node).name;
        if (node instanceof Name)   return nameOf(((Name) node).id);
        if (node instanceof Value)  return nameOf(((Value) node).baseValue);
        return null;
    }

    private String memberName(MemberTrailer trailer) {
        if (trailer.id instanceof IdType) return ((IdType) trailer.id).name;
        String text = trailer.toString();
        return text.startsWith(".") ? text.substring(1) : text;
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        if (s.length() >= 2
                && ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\"")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
