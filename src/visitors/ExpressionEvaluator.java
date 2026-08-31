package visitors;

import models.Node;
import models.jinja.atoms.DictType;
import models.jinja.atoms.FloatType;
import models.jinja.atoms.IdType;
import models.jinja.atoms.IntType;
import models.jinja.atoms.ListType;
import models.jinja.atoms.PairType;
import models.jinja.atoms.StringType;
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
import models.jinja.expressions.UnaryOperator;
import models.jinja.trailers.CallTrailer;
import models.jinja.trailers.MemberTrailer;
import models.jinja.trailers.SubTrailer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates a Jinja expression by walking its AST.
 *
 * This replaces the previous approach of turning the parsed tree back into text
 * and splitting that text on '.', '|' and '(' — which could not distinguish a
 * dot inside a string literal from a member access, and silently returned the
 * raw source text for anything it failed to understand.
 *
 * Unknown names resolve to null and are reported through the Problems sink so
 * a typo surfaces as a diagnostic instead of leaking the variable name into the
 * generated page.
 */
public class ExpressionEvaluator {

    /** Filters applyFilter understands. The analyzer checks names against this. */
    public static final Set<String> SUPPORTED_FILTERS = new LinkedHashSet<>(Arrays.asList(
            "format", "upper", "lower", "title", "trim", "length", "count",
            "int", "float", "round", "default", "join", "escape", "e"));

    /** Tests the "is" operator understands. */
    public static final Set<String> SUPPORTED_TESTS = new LinkedHashSet<>(Arrays.asList(
            "defined", "undefined", "none", "null", "even", "odd", "string", "number"));

    /** Functions callable from a template. */
    public static final Set<String> SUPPORTED_FUNCTIONS = new LinkedHashSet<>(Arrays.asList(
            "url_for", "range", "length", "len"));

    /** Receives non-fatal problems found while evaluating. */
    public interface Problems {
        void report(String message);
    }

    /** Resolves url_for(route, params) to a generated page filename. */
    public interface UrlResolver {
        String urlFor(String routeName, Map<String, Object> params);
    }

    private final UrlResolver urlResolver;
    private final Problems problems;

    public ExpressionEvaluator(UrlResolver urlResolver, Problems problems) {
        this.urlResolver = urlResolver;
        this.problems = problems;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════

    public Object eval(Node expr, Map<String, Object> context) {
        if (expr == null) return null;

        // ── Atoms ─────────────────────────────────────────────────────────
        if (expr instanceof IdType)     return resolveName(((IdType) expr).name, context);
        if (expr instanceof StringType) return unquote(((StringType) expr).value);
        if (expr instanceof IntType)    return ((IntType) expr).value;
        if (expr instanceof FloatType)  return ((FloatType) expr).value;
        if (expr instanceof ListType)   return evalList((ListType) expr, context);
        if (expr instanceof DictType)   return evalDict((DictType) expr, context);

        // ── Access chains ─────────────────────────────────────────────────
        if (expr instanceof PrimaryExpression) return evalPrimary((PrimaryExpression) expr, context);

        // ── Operators ─────────────────────────────────────────────────────
        if (expr instanceof ParenthedExpression) return eval(((ParenthedExpression) expr).expr, context);
        if (expr instanceof AddExpression)       return evalAdd((AddExpression) expr, context);
        if (expr instanceof MulExpression)       return evalMul((MulExpression) expr, context);
        if (expr instanceof PowerExpression)     return evalPower((PowerExpression) expr, context);
        if (expr instanceof UnaryExpression)     return evalUnary((UnaryExpression) expr, context);
        if (expr instanceof ComparisonExpression) return evalComparison((ComparisonExpression) expr, context);
        if (expr instanceof AndExpression)       return evalAnd((AndExpression) expr, context);
        if (expr instanceof OrExpression)        return evalOr((OrExpression) expr, context);
        if (expr instanceof NotExpression)       return !truthy(eval(((NotExpression) expr).expression, context));
        if (expr instanceof InExpression)        return evalIn((InExpression) expr, context);
        if (expr instanceof IsExpression)        return evalIs((IsExpression) expr, context);
        if (expr instanceof TernaryExpression)   return evalTernary((TernaryExpression) expr, context);
        if (expr instanceof ConcatExpression)    return evalConcat((ConcatExpression) expr, context);
        if (expr instanceof DefaultExpression)   return evalDefault((DefaultExpression) expr, context);
        if (expr instanceof PipeExpression)      return evalPipe((PipeExpression) expr, context);

        problems.report("Cannot evaluate expression node " + expr.getClass().getSimpleName()
                + " at line " + expr.getLineNumber());
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ATOMS
    // ═══════════════════════════════════════════════════════════════════════

    private Object resolveName(String name, Map<String, Object> context) {
        if (context.containsKey(name)) return context.get(name);
        // Jinja's literal keywords are not context variables.
        if ("true".equals(name) || "True".equals(name))   return Boolean.TRUE;
        if ("false".equals(name) || "False".equals(name)) return Boolean.FALSE;
        if ("none".equals(name) || "None".equals(name))   return null;
        problems.report("Undefined template variable '" + name + "'");
        return null;
    }

    private List<Object> evalList(ListType list, Map<String, Object> context) {
        List<Object> out = new ArrayList<>();
        if (list.itemList != null) {
            for (Node item : list.itemList) out.add(eval(item, context));
        }
        return out;
    }

    private Map<String, Object> evalDict(DictType dict, Map<String, Object> context) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (dict.pairsList != null) {
            for (Node pair : dict.pairsList) {
                if (pair instanceof PairType) {
                    PairType p = (PairType) pair;
                    out.put(str(eval(p.expr1, context)), eval(p.expr2, context));
                }
            }
        }
        return out;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ACCESS CHAINS:  name.member  name[key]  func(args)
    // ═══════════════════════════════════════════════════════════════════════

    private Object evalPrimary(PrimaryExpression primary, Map<String, Object> context) {
        // A call like url_for(...) parses as atom=url_for followed by a CallTrailer,
        // so the callee is resolved before the atom is looked up as a variable.
        if (primary.atom instanceof IdType && hasLeadingCall(primary)) {
            String funcName = ((IdType) primary.atom).name;
            CallTrailer call = (CallTrailer) primary.trailerList.get(0);
            Object result = callFunction(funcName, call, context);
            return applyTrailers(result, primary.trailerList, 1, context);
        }

        Object value = eval(primary.atom, context);
        return applyTrailers(value, primary.trailerList, 0, context);
    }

    private boolean hasLeadingCall(PrimaryExpression primary) {
        return primary.trailerList != null
                && !primary.trailerList.isEmpty()
                && primary.trailerList.get(0) instanceof CallTrailer;
    }

    private Object applyTrailers(Object value, List<Node> trailers, int from,
                                 Map<String, Object> context) {
        if (trailers == null) return value;

        for (int i = from; i < trailers.size(); i++) {
            Node trailer = trailers.get(i);

            if (trailer instanceof MemberTrailer) {
                value = member(value, memberName((MemberTrailer) trailer));
            } else if (trailer instanceof SubTrailer) {
                value = subscript(value, eval(((SubTrailer) trailer).expr, context));
            } else if (trailer instanceof CallTrailer) {
                problems.report("Calling a value is not supported in templates: " + trailer);
                return null;
            }
        }
        return value;
    }

    private String memberName(MemberTrailer trailer) {
        if (trailer.id instanceof IdType) return ((IdType) trailer.id).name;
        // Fall back to the printed form, dropping the leading dot.
        String text = trailer.id == null ? trailer.toString() : trailer.id.toString();
        return text.startsWith(".") ? text.substring(1) : text;
    }

    /** Jinja resolves foo.bar against dict keys first, then list indices. */
    private Object member(Object target, String name) {
        if (target == null) {
            problems.report("Cannot read '" + name + "' of an undefined value");
            return null;
        }
        if (target instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) target;
            if (!map.containsKey(name)) {
                problems.report("Key '" + name + "' not present in " + map.keySet());
                return null;
            }
            return map.get(name);
        }
        if (target instanceof List && "length".equals(name)) return ((List<?>) target).size();
        problems.report("Cannot read '" + name + "' of " + typeName(target));
        return null;
    }

    private Object subscript(Object target, Object key) {
        if (target == null) {
            problems.report("Cannot index an undefined value");
            return null;
        }
        if (target instanceof Map)  return ((Map<?, ?>) target).get(str(key));
        if (target instanceof List) {
            List<?> list = (List<?>) target;
            if (!(key instanceof Number)) {
                problems.report("List index must be a number, got " + typeName(key));
                return null;
            }
            int i = ((Number) key).intValue();
            if (i < 0) i += list.size();
            if (i < 0 || i >= list.size()) {
                problems.report("List index " + key + " out of range (size " + list.size() + ")");
                return null;
            }
            return list.get(i);
        }
        problems.report("Cannot index " + typeName(target));
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FUNCTION CALLS
    // ═══════════════════════════════════════════════════════════════════════

    private Object callFunction(String name, CallTrailer call, Map<String, Object> context) {
        List<Object> positional = new ArrayList<>();
        Map<String, Object> keyword = new LinkedHashMap<>();
        readArguments(call, context, positional, keyword);

        if ("url_for".equals(name)) {
            if (positional.isEmpty()) {
                problems.report("url_for() needs a route name");
                return "#";
            }
            return urlResolver.urlFor(str(positional.get(0)), keyword);
        }
        if ("range".equals(name)) return range(positional);
        if ("length".equals(name) || "len".equals(name)) {
            return positional.isEmpty() ? 0 : sizeOf(positional.get(0));
        }

        problems.report("Unknown function '" + name + "()' in template");
        return null;
    }

    private void readArguments(CallTrailer call, Map<String, Object> context,
                               List<Object> positional, Map<String, Object> keyword) {
        if (!(call.argList instanceof ArgumentList)) return;
        ArgumentList args = (ArgumentList) call.argList;
        if (args.argList == null) return;

        for (Node node : args.argList) {
            if (!(node instanceof Argument)) continue;
            Argument arg = (Argument) node;
            if (arg.argName == null) {
                positional.add(eval(arg.expr, context));
            } else {
                keyword.put(nameOf(arg.argName), eval(arg.expr, context));
            }
        }
    }

    private String nameOf(Node node) {
        if (node instanceof IdType) return ((IdType) node).name;
        return node.toString();
    }

    private List<Object> range(List<Object> args) {
        int start = 0, end = 0;
        if (args.size() == 1) end = intOf(args.get(0));
        else if (args.size() >= 2) { start = intOf(args.get(0)); end = intOf(args.get(1)); }
        List<Object> out = new ArrayList<>();
        for (int i = start; i < end; i++) out.add(i);
        return out;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FILTERS
    // ═══════════════════════════════════════════════════════════════════════

    private Object evalPipe(PipeExpression pipe, Map<String, Object> context) {
        Object value = eval(pipe.expr, context);
        if (pipe.filterList == null) return value;
        for (Node filter : pipe.filterList) {
            value = applyFilter(value, filter, context);
        }
        return value;
    }

    private Object applyFilter(Object value, Node filterNode, Map<String, Object> context) {
        String name;
        List<Object> args = new ArrayList<>();

        if (filterNode instanceof FilterExpression) {
            FilterExpression filter = (FilterExpression) filterNode;
            name = nameOf(filter.filterName);
            if (filter.argList instanceof ArgumentList) {
                ArgumentList list = (ArgumentList) filter.argList;
                if (list.argList != null) {
                    for (Node n : list.argList) {
                        if (n instanceof Argument) args.add(eval(((Argument) n).expr, context));
                    }
                }
            }
        } else {
            name = nameOf(filterNode);
        }

        switch (name) {
            case "format":
                return formatFilter(value, args);
            case "upper":
                return str(value).toUpperCase();
            case "lower":
                return str(value).toLowerCase();
            case "title":
                return titleCase(str(value));
            case "trim":
                return str(value).trim();
            case "length":
            case "count":
                return sizeOf(value);
            case "int":
                return intOf(value);
            case "float":
                return doubleOf(value);
            case "round":
                return (double) Math.round(doubleOf(value));
            case "default":
                return value == null ? (args.isEmpty() ? "" : args.get(0)) : value;
            case "join":
                return joinFilter(value, args.isEmpty() ? "" : str(args.get(0)));
            case "escape":
            case "e":
                return escapeHtml(str(value));
            default:
                problems.report("Unknown filter '" + name + "'");
                return value;
        }
    }

    /**
     * Jinja's format filter is printf-style with the *subject* as the format
     * string: {{ "%.2f"|format(price) }}.
     */
    private Object formatFilter(Object value, List<Object> args) {
        String pattern = str(value);
        try {
            return String.format(pattern, coerceToConversions(pattern, args));
        } catch (Exception e) {
            problems.report("format filter failed for pattern \"" + pattern + "\": " + e.getMessage());
            return pattern;
        }
    }

    /** One printf conversion: optional flags, width and precision, then the conversion letter. */
    private static final Pattern FORMAT_SPEC =
            Pattern.compile("%[-#+ 0,(]*\\d*(?:\\.\\d+)?([a-zA-Z%])");

    /**
     * Converts each argument to the Java type its conversion requires.
     *
     * String.format is strict about the argument's runtime type: %f rejects an
     * Integer and %d rejects a Double. Extraction produces whichever type the
     * literal in app.py happened to have, so {{ "%.2f"|format(product.price) }}
     * threw as soon as one price was written 129 rather than 129.99 - and the
     * catch above then put the pattern itself into the page, so a build that
     * reported success emitted "$%.2f". The conversion decides the type here,
     * which is what the template author meant by writing it.
     *
     * A pattern using explicit argument indices (%1$s) is left untouched, since
     * positional matching no longer describes which argument each one consumes.
     */
    private static Object[] coerceToConversions(String pattern, List<Object> args) {
        Object[] values = args.toArray();
        if (pattern.indexOf('$') >= 0) return values;

        Matcher spec = FORMAT_SPEC.matcher(pattern);
        int index = 0;
        while (spec.find() && index < values.length) {
            char conversion = Character.toLowerCase(spec.group(1).charAt(0));
            // %% and %n stand for themselves and consume no argument.
            if (conversion == '%' || conversion == 'n') continue;

            if (conversion == 'f' || conversion == 'e' || conversion == 'g' || conversion == 'a') {
                values[index] = doubleOf(values[index]);
            } else if (conversion == 'd' || conversion == 'o' || conversion == 'x') {
                values[index] = (long) doubleOf(values[index]);
            }
            index++;
        }
        return values;
    }

    private String joinFilter(Object value, String separator) {
        if (!(value instanceof List)) return str(value);
        StringBuilder sb = new StringBuilder();
        List<?> list = (List<?>) value;
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(separator);
            sb.append(str(list.get(i)));
        }
        return sb.toString();
    }

    private String titleCase(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean atStart = true;
        for (char c : s.toCharArray()) {
            sb.append(atStart ? Character.toUpperCase(c) : Character.toLowerCase(c));
            atStart = !Character.isLetterOrDigit(c);
        }
        return sb.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  OPERATORS
    // ═══════════════════════════════════════════════════════════════════════

    private Object evalAdd(AddExpression add, Map<String, Object> context) {
        Object acc = eval(add.exprList.get(0), context);
        for (int i = 1; i < add.exprList.size(); i++) {
            Object rhs = eval(add.exprList.get(i), context);
            String op = add.operatorAt(i - 1);
            // '+' concatenates when either side is a string, matching Jinja.
            if ("+".equals(op) && (acc instanceof String || rhs instanceof String)) {
                acc = str(acc) + str(rhs);
            } else {
                acc = arithmetic(acc, rhs, op);
            }
        }
        return acc;
    }

    private Object evalMul(MulExpression mul, Map<String, Object> context) {
        Object acc = eval(mul.exprList.get(0), context);
        for (int i = 1; i < mul.exprList.size(); i++) {
            acc = arithmetic(acc, eval(mul.exprList.get(i), context), mul.operatorAt(i - 1));
        }
        return acc;
    }

    private Object arithmetic(Object a, Object b, String op) {
        double x = doubleOf(a), y = doubleOf(b);
        double result;
        switch (op) {
            case "+": result = x + y; break;
            case "-": result = x - y; break;
            case "*": result = x * y; break;
            case "/":
                if (y == 0) { problems.report("Division by zero"); return null; }
                result = x / y; break;
            case "%":
                if (y == 0) { problems.report("Modulo by zero"); return null; }
                result = x % y; break;
            default:
                problems.report("Unknown operator '" + op + "'");
                return null;
        }
        return narrow(result, a, b);
    }

    /** Keeps integer arithmetic integral so 2*3 prints as 6, not 6.0. */
    private Object narrow(double result, Object a, Object b) {
        boolean bothIntegral = isIntegral(a) && isIntegral(b);
        if (bothIntegral && result == Math.rint(result) && !Double.isInfinite(result)) {
            return (int) result;
        }
        return result;
    }

    private boolean isIntegral(Object o) {
        return o instanceof Integer || o instanceof Long || o instanceof Short || o instanceof Byte;
    }

    private Object evalPower(PowerExpression power, Map<String, Object> context) {
        double base = doubleOf(eval(power.baseValueExpr, context));
        double exp = doubleOf(eval(power.powerValueExpr, context));
        double result = Math.pow(base, exp);
        return result == Math.rint(result) && !Double.isInfinite(result) ? (int) result : result;
    }

    private Object evalUnary(UnaryExpression unary, Map<String, Object> context) {
        Object value = eval(unary.expr, context);
        String sign = unary.unaryOperator instanceof UnaryOperator
                ? ((UnaryOperator) unary.unaryOperator).sign
                : String.valueOf(unary.unaryOperator);
        if ("-".equals(sign)) {
            Object negated = arithmetic(0, value, "-");
            return negated;
        }
        if ("not".equals(sign)) return !truthy(value);
        return value;
    }

    private Object evalComparison(ComparisonExpression cmp, Map<String, Object> context) {
        Object a = eval(cmp.expr1, context);
        Object b = eval(cmp.expr2, context);
        String op = cmp.compOptor == null ? "==" : cmp.compOptor.toString().trim();

        switch (op) {
            case "==": return equalValues(a, b);
            case "!=": return !equalValues(a, b);
        }
        if (a instanceof Number && b instanceof Number) {
            int c = Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
            switch (op) {
                case "<":  return c < 0;
                case "<=": return c <= 0;
                case ">":  return c > 0;
                case ">=": return c >= 0;
            }
        }
        problems.report("Cannot compare " + typeName(a) + " " + op + " " + typeName(b));
        return false;
    }

    private boolean equalValues(Object a, Object b) {
        if (a == null || b == null) return a == b;
        if (a instanceof Number && b instanceof Number) {
            return ((Number) a).doubleValue() == ((Number) b).doubleValue();
        }
        return a.equals(b);
    }

    private Object evalAnd(AndExpression and, Map<String, Object> context) {
        for (Node e : and.exprList) {
            if (!truthy(eval(e, context))) return false;
        }
        return true;
    }

    private Object evalOr(OrExpression or, Map<String, Object> context) {
        for (Node e : or.exprList) {
            if (truthy(eval(e, context))) return true;
        }
        return false;
    }

    private Object evalIn(InExpression in, Map<String, Object> context) {
        Object needle = eval(in.expr1, context);
        Object haystack = eval(in.expr2, context);
        if (haystack instanceof List) {
            for (Object o : (List<?>) haystack) if (equalValues(o, needle)) return true;
            return false;
        }
        if (haystack instanceof Map)    return ((Map<?, ?>) haystack).containsKey(str(needle));
        if (haystack instanceof String) return ((String) haystack).contains(str(needle));
        return false;
    }

    private Object evalIs(IsExpression is, Map<String, Object> context) {
        Object value = eval(is.expr, context);
        String test = is.id == null ? "" : nameOf(is.id);
        boolean result;
        switch (test) {
            case "defined":   result = value != null; break;
            case "undefined": result = value == null; break;
            case "none":
            case "null":      result = value == null; break;
            case "even":      result = intOf(value) % 2 == 0; break;
            case "odd":       result = intOf(value) % 2 != 0; break;
            case "string":    result = value instanceof String; break;
            case "number":    result = value instanceof Number; break;
            default:
                problems.report("Unknown test 'is " + test + "'");
                result = false;
        }
        return is.negated != result;
    }

    private Object evalTernary(TernaryExpression ternary, Map<String, Object> context) {
        return truthy(eval(ternary.condExpr, context))
                ? eval(ternary.trueExpr, context)
                : eval(ternary.falseExpr, context);
    }

    private Object evalConcat(ConcatExpression concat, Map<String, Object> context) {
        StringBuilder sb = new StringBuilder();
        for (Node e : concat.exprList) sb.append(str(eval(e, context)));
        return sb.toString();
    }

    private Object evalDefault(DefaultExpression def, Map<String, Object> context) {
        Object value = eval(def.expr, context);
        return value != null ? value : eval(def.defaultExpr, context);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVERSIONS
    // ═══════════════════════════════════════════════════════════════════════

    public static boolean truthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String)  return !((String) value).isEmpty();
        if (value instanceof Number)  return ((Number) value).doubleValue() != 0;
        if (value instanceof List)    return !((List<?>) value).isEmpty();
        if (value instanceof Map)     return !((Map<?, ?>) value).isEmpty();
        return true;
    }

    /** Renders a value the way Jinja would print it. */
    public static String str(Object value) {
        if (value == null) return "";
        if (value instanceof Double) {
            double d = (Double) value;
            // Print whole doubles without a trailing .0, as Python does not show it
            // for values that came from integer arithmetic.
            if (d == Math.rint(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return String.valueOf((long) d);
            }
        }
        return String.valueOf(value);
    }

    private static int intOf(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(str(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double doubleOf(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof Boolean) return ((Boolean) value) ? 1 : 0;
        try {
            return Double.parseDouble(str(value).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int sizeOf(Object value) {
        if (value instanceof List)   return ((List<?>) value).size();
        if (value instanceof Map)    return ((Map<?, ?>) value).size();
        if (value instanceof String) return ((String) value).length();
        return 0;
    }

    private static String typeName(Object value) {
        if (value == null) return "undefined";
        if (value instanceof List) return "list";
        if (value instanceof Map) return "dict";
        if (value instanceof String) return "string";
        if (value instanceof Number) return "number";
        if (value instanceof Boolean) return "boolean";
        return value.getClass().getSimpleName();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** Strips a matching pair of surrounding quotes from a literal. */
    public static String unquote(String s) {
        if (s == null) return null;
        if (s.length() >= 2
                && ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\"")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
