package visitors;

import models.App;
import models.Node;
import models.jinja.atoms.IdType;
import models.jinja.atoms.IntType;
import models.jinja.atoms.StringType;
import models.jinja.expressions.Argument;
import models.jinja.expressions.ArgumentList;
import models.jinja.trailers.CallTrailer;
import models.python.BlockNode;
import models.python.Decorator;
import models.python.Func;
import models.python.Name;
import models.python.Value;
import models.python.literals.Dict;
import models.python.literals.FalseValue;
import models.python.literals.NoneValue;
import models.python.literals.TrueValue;
import models.python.simple_statements.AssignLine;
import models.python.simple_statements.ReturnLine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PythonDataExtractor {

    private Map<String, Object> moduleVars;
    private Map<String, Map<String, Object>> routeContexts;
    private Map<String, String> routeTemplates;
    private Map<String, String> urlForRoutes;

    public PythonDataExtractor() {
        moduleVars = new HashMap<>();
        routeContexts = new HashMap<>();
        routeTemplates = new HashMap<>();
        urlForRoutes = new HashMap<>();
    }

    public void extract(App app) {
        for (Node node : app.nodes) {
            extractNode(node);
        }
    }

    private void extractNode(Node node) {
        if (node instanceof AssignLine) {
            AssignLine assign = (AssignLine) node;
            if (assign.target instanceof IdType) {
                extractAssignLine(assign);
            }
        } else if (node instanceof Func) {
            extractFunc((Func) node);
        }
    }

    private void extractAssignLine(AssignLine assign) {
        String name = ((IdType) assign.target).name;
        Object value = convertNodeToObject(assign.expr);
        moduleVars.put(name, value);
    }

    private void extractFunc(Func func) {
        String funcName = null;
        if (func.funcName instanceof IdType) {
            funcName = ((IdType) func.funcName).name;
        }

        // Check if it has a route decorator
        if (func.decorator instanceof Decorator) {
            Decorator dec = (Decorator) func.decorator;
            if (isRouteDecorator(dec) && funcName != null) {
                String url = extractRouteUrl(dec);
                if (url != null) {
                    urlForRoutes.put(funcName, funcName);
                }
            }
        }

        // Scan function body for render_template calls
        if (func.funcBlock instanceof BlockNode) {
            BlockNode block = (BlockNode) func.funcBlock;
            for (Node stmt : block.statements) {
                if (stmt instanceof ReturnLine) {
                    ReturnLine ret = (ReturnLine) stmt;
                    if (ret.returnExpr instanceof Value) {
                        Value val = (Value) ret.returnExpr;
                        Node base = val.baseValue;
                        String baseName = null;
                        if (base instanceof IdType) {
                            baseName = ((IdType) base).name;
                        } else if (base instanceof Name) {
                            baseName = ((IdType) ((Name) base).id).name;
                        }
                        if ("render_template".equals(baseName)) {
                            extractRenderTemplate(funcName, val);
                        }
                    }
                }
            }
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
        if (!(firstTrailer instanceof models.jinja.trailers.MemberTrailer)) return false;
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

    private void extractRenderTemplate(String routeName, Value value) {
        if (value.trailerList == null) return;
        for (Node trailer : value.trailerList) {
            if (trailer instanceof CallTrailer) {
                CallTrailer ct = (CallTrailer) trailer;
                if (ct.argList instanceof ArgumentList) {
                    ArgumentList al = (ArgumentList) ct.argList;
                    String templateFile = null;
                    Map<String, Object> context = new HashMap<>();
                    for (int i = 0; i < al.argList.size(); i++) {
                        Argument arg = (Argument) al.argList.get(i);
                        if (arg.argName == null) {
                            // Positional argument - template filename
                            templateFile = extractStringValue(arg.expr);
                        } else {
                            // Keyword argument - context variable
                            String argName = extractStringValue(arg.argName);
                            Object argValue = resolveArgValue(arg.expr);
                            context.put(argName, argValue);
                        }
                    }
                    if (templateFile != null && routeName != null) {
                        routeContexts.put(routeName, context);
                        routeTemplates.put(routeName, templateFile);
                    }
                }
            }
        }
    }

    private String extractStringValue(Node node) {
        if (node instanceof Value) {
            return extractStringValue(((Value) node).baseValue);
        }
        if (node instanceof StringType) {
            String val = ((StringType) node).value;
            if (val.length() >= 2 && ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\"")))) {
                return val.substring(1, val.length() - 1);
            }
            return val;
        }
        if (node instanceof IdType) {
            return ((IdType) node).name;
        }
        return node.toString();
    }

    private Object resolveArgValue(Node expr) {
        if (expr instanceof IdType) {
            String name = ((IdType) expr).name;
            return moduleVars.get(name);
        }
        if (expr instanceof Name) {
            String name = ((IdType) ((Name) expr).id).name;
            return moduleVars.get(name);
        }
        if (expr instanceof Value) {
            return resolveArgValue(((Value) expr).baseValue);
        }
        return convertNodeToObject(expr);
    }

    private Object convertNodeToObject(Node node) {
        if (node instanceof IntType) {
            return ((IntType) node).value;
        }
        if (node instanceof models.jinja.atoms.FloatType) {
            return ((models.jinja.atoms.FloatType) node).value;
        }
        if (node instanceof StringType) {
            String val = ((StringType) node).value;
            if (val.length() >= 2 && ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\"")))) {
                return val.substring(1, val.length() - 1);
            }
            return val;
        }
        if (node instanceof TrueValue) {
            return Boolean.TRUE;
        }
        if (node instanceof FalseValue) {
            return Boolean.FALSE;
        }
        if (node instanceof NoneValue) {
            return null;
        }
        if (node instanceof models.jinja.atoms.ListType) {
            models.jinja.atoms.ListType list = (models.jinja.atoms.ListType) node;
            List<Object> result = new ArrayList<>();
            if (list.itemList != null) {
                for (Node item : list.itemList) {
                    result.add(convertNodeToObject(item));
                }
            }
            return result;
        }
        if (node instanceof Dict) {
            return parseDictString(node.toString());
        }
        if (node instanceof Value) {
            return convertNodeToObject(((Value) node).baseValue);
        }
        if (node instanceof Name) {
            return convertNodeToObject(((Name) node).id);
        }
        if (node instanceof IdType) {
            return moduleVars.get(((IdType) node).name);
        }
        return node.toString();
    }

    private List<Object> parseListString(String s) {
        s = s.substring(1, s.length() - 1).trim(); // Remove [ and ]
        List<Object> result = new ArrayList<>();
        if (s.isEmpty()) return result;
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= s.length()) break;
            if (s.charAt(i) == '{') {
                int end = findMatchingBrace(s, i);
                if (end > 0) {
                    result.add(parseDictString(s.substring(i, end + 1)));
                    i = end + 1;
                } else {
                    break;
                }
            } else {
                int end = s.indexOf(", ", i);
                if (end < 0) end = s.length();
                String itemStr = s.substring(i, end).trim();
                result.add(evaluateValueString(itemStr));
                i = end;
            }
        }
        return result;
    }

    private Map<String, Object> parseDictString(String s) {
        s = s.substring(1, s.length() - 1).trim(); // Remove { and }
        Map<String, Object> result = new HashMap<>();
        if (s.isEmpty()) return result;
        List<String> items = new ArrayList<>();
        int i = 0;
        int start = 0;
        while (i < s.length()) {
            if (s.charAt(i) == ',' && i + 1 < s.length() && s.charAt(i + 1) == ' ') {
                items.add(s.substring(start, i));
                start = i + 2;
            }
            i++;
        }
        if (start < s.length()) {
            items.add(s.substring(start));
        }
        for (String item : items) {
            int idx = item.indexOf(" : ");
            if (idx > 0) {
                String key = item.substring(0, idx).trim();
                String valueStr = item.substring(idx + 3).trim();
                if (key.length() >= 2 && ((key.startsWith("'") && key.endsWith("'")) || (key.startsWith("\"") && key.endsWith("\"")))) {
                    key = key.substring(1, key.length() - 1);
                }
                result.put(key, evaluateValueString(valueStr));
            }
        }
        return result;
    }

    private int findMatchingBrace(String s, int start) {
        int depth = 1;
        for (int i = start + 1; i < s.length(); i++) {
            if (s.charAt(i) == '{') depth++;
            else if (s.charAt(i) == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private Object evaluateValueString(String s) {
        s = s.trim();
        if (s.isEmpty()) return "";
        if (s.matches("\\d+")) return Integer.valueOf(s);
        if (s.matches("\\d+\\.\\d+")) return Double.valueOf(s);
        if (s.equals("True")) return Boolean.TRUE;
        if (s.equals("False")) return Boolean.FALSE;
        if (s.equals("None")) return null;
        if (s.length() >= 2 && ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\"")))) {
            s = s.substring(1, s.length() - 1);
        }
        return s;
    }

    public Map<String, Object> getModuleVars() {
        return moduleVars;
    }

    public Map<String, Map<String, Object>> getRouteContexts() {
        return routeContexts;
    }

    public Map<String, String> getRouteTemplates() {
        return routeTemplates;
    }

    public Map<String, String> getUrlForRoutes() {
        return urlForRoutes;
    }
}
