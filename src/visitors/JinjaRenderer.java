package visitors;

import models.Node;
import models.NodeBody;
import models.NormalText;
import models.Template;
import models.jinja.blocks.ExtendsBlock;
import models.jinja.blocks.ForBlock;
import models.jinja.blocks.IfBlock;
import models.jinja.blocks.InheritedBlock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JinjaRenderer {

    private Map<String, Template> templateRegistry;
    private Map<String, String> urlForRoutes;
    private StringBuilder output;

    public JinjaRenderer(Map<String, Template> templateRegistry, Map<String, String> urlForRoutes) {
        this.templateRegistry = templateRegistry;
        this.urlForRoutes = urlForRoutes;
        this.output = new StringBuilder();
    }

    public String render(Template template, Map<String, Object> context) {
        output = new StringBuilder();
        renderTemplate(template, context);
        return output.toString();
    }

    private void renderTemplate(Template template, Map<String, Object> context) {
        if (template == null || template.nodes == null || template.nodes.isEmpty()) return;

        String html = buildTemplateString(template);

        // Handle template inheritance
        if (template.nodes.get(0) instanceof ExtendsBlock) {
            ExtendsBlock extendsBlock = (ExtendsBlock) template.nodes.get(0);
            String baseTemplateName = extendsBlock.templateName;
            if (baseTemplateName.length() >= 2 && ((baseTemplateName.startsWith("'") && baseTemplateName.endsWith("'")) || (baseTemplateName.startsWith("\"") && baseTemplateName.endsWith("\"")))) {
                baseTemplateName = baseTemplateName.substring(1, baseTemplateName.length() - 1);
            }
            Template baseTemplate = templateRegistry.get(baseTemplateName);
            if (baseTemplate != null) {
                Map<String, String> childBlocks = new HashMap<>();
                for (Node node : template.nodes) {
                    if (node instanceof InheritedBlock) {
                        InheritedBlock inherited = (InheritedBlock) node;
                        String blockStr = inherited.toString();
                        int start = blockStr.indexOf("{% block " + inherited.blockName + " %}\n");
                        if (start >= 0) {
                            start += ("{% block " + inherited.blockName + " %}\n").length();
                            int end = blockStr.indexOf("\n{% endblock %}", start);
                            if (end > 0) {
                                childBlocks.put(inherited.blockName, blockStr.substring(start, end));
                            }
                        }
                    }
                }
                String baseHtml = buildTemplateString(baseTemplate);
                for (Map.Entry<String, String> entry : childBlocks.entrySet()) {
                    String blockName = entry.getKey();
                    String childBlock = entry.getValue();
                    String pattern = "\\{% block " + blockName + " %\\}.*?\\{% endblock %\\}";
                    String replacement = Matcher.quoteReplacement(childBlock);
                    baseHtml = baseHtml.replaceAll(pattern, replacement);
                }
                // Strip any remaining block tags (for blocks that were not overridden)
                baseHtml = baseHtml.replaceAll("\\{% block \\w+ %\\}\\n?", "");
                baseHtml = baseHtml.replaceAll("\\n?\\{% endblock %\\}", "");
                html = baseHtml;
            }
        }

        // Evaluate for loops
        html = evaluateForLoops(html, context);

        // Evaluate if blocks
        html = evaluateIfBlocks(html, context);

        // Evaluate Jinja expressions
        html = evaluateJinjaInHtml(html, context);

        output.append(html);
    }

    private String buildTemplateString(Template template) {
        StringBuilder sb = new StringBuilder();
        if (template != null && template.nodes != null) {
            for (Node node : template.nodes) {
                sb.append(node.toString());
            }
        }
        return sb.toString();
    }

    private String evaluateJinjaInHtml(String html, Map<String, Object> context) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < html.length()) {
            int start = html.indexOf("{{", i);
            if (start < 0) {
                result.append(html.substring(i));
                break;
            }
            result.append(html.substring(i, start));
            int end = html.indexOf("}}", start);
            if (end < 0) {
                result.append(html.substring(start));
                break;
            }
            String expr = html.substring(start + 2, end).trim();
            Object value = evaluateString(expr, context);
            result.append(value != null ? value.toString() : "");
            i = end + 2;
        }
        return result.toString();
    }

    private String evaluateForLoops(String html, Map<String, Object> context) {
        String result = html;
        Pattern forPattern = Pattern.compile("\\{% for (\\w+) in (\\w+) %\\}\\n(.*?)\\{% endfor %\\}", Pattern.DOTALL);
        Matcher matcher = forPattern.matcher(result);
        while (matcher.find()) {
            String loopVar = matcher.group(1);
            String iterableExpr = matcher.group(2);
            String body = matcher.group(3);

            Object iterable = evaluateString(iterableExpr, context);
            StringBuilder loopResult = new StringBuilder();
            if (iterable instanceof List) {
                List<?> list = (List<?>) iterable;
                if (list.isEmpty()) {
                    int elseStart = body.indexOf("{% else %}");
                    if (elseStart >= 0) {
                        String elseBody = body.substring(elseStart + "{% else %}".length());
                        loopResult.append(elseBody);
                    }
                } else {
                    for (Object item : list) {
                        Map<String, Object> childContext = new HashMap<>(context);
                        childContext.put(loopVar, item);
                        String itemBody = evaluateJinjaInHtml(body, childContext);
                        int elseStart = itemBody.indexOf("{% else %}");
                        if (elseStart >= 0) {
                            itemBody = itemBody.substring(0, elseStart);
                        }
                        loopResult.append(itemBody);
                    }
                }
            }

            result = result.substring(0, matcher.start()) + loopResult.toString() + result.substring(matcher.end());
            matcher = forPattern.matcher(result);
        }
        return result;
    }

    private String evaluateIfBlocks(String html, Map<String, Object> context) {
        String result = html;
        Pattern ifPattern = Pattern.compile("\\{% if (.*?) %\\}\\n(.*?)\\{% endif %\\}", Pattern.DOTALL);
        Matcher matcher = ifPattern.matcher(result);
        while (matcher.find()) {
            String condition = matcher.group(1).trim();
            String body = matcher.group(2);

            boolean conditionMet = isTruthy(evaluateString(condition, context));
            String renderedBody = "";
            if (conditionMet) {
                renderedBody = body;
            }

            result = result.substring(0, matcher.start()) + renderedBody + result.substring(matcher.end());
            matcher = ifPattern.matcher(result);
        }
        return result;
    }

    private Object evaluateString(String expr, Map<String, Object> context) {
        expr = expr.trim();
        if (expr.isEmpty()) return "";

        // Handle string literal (must check before dot notation)
        if ((expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'"))) {
            return expr.substring(1, expr.length() - 1);
        }

        // Handle pipe expression
        if (expr.contains("|")) {
            String[] parts = expr.split("\\|", 2);
            Object base = evaluateString(parts[0].trim(), context);
            String filterExpr = parts[1].trim();
            return applyFilter(base, filterExpr, context);
        }

        // Handle function call
        if (expr.contains("(")) {
            int parenIdx = expr.indexOf("(");
            String funcName = expr.substring(0, parenIdx).trim();
            String argsStr = expr.substring(parenIdx + 1, expr.lastIndexOf(")")).trim();
            return callFunction(funcName, argsStr, context);
        }

        // Handle dot notation
        if (expr.contains(".")) {
            String[] parts = expr.split("\\.");
            Object value = context.get(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                if (value instanceof Map) {
                    value = ((Map<?, ?>) value).get(parts[i]);
                } else {
                    return null;
                }
            }
            return value;
        }

        // Handle number
        if (expr.matches("\\d+")) {
            return Integer.valueOf(expr);
        }
        if (expr.matches("\\d+\\.\\d+")) {
            return Double.valueOf(expr);
        }

        // Handle identifier
        if (context.containsKey(expr)) {
            return context.get(expr);
        }

        // Default: treat as string literal
        return expr;
    }

    private Object applyFilter(Object base, String filterExpr, Map<String, Object> context) {
        int parenIdx = filterExpr.indexOf("(");
        if (parenIdx > 0) {
            String filterName = filterExpr.substring(0, parenIdx).trim();
            String argStr = filterExpr.substring(parenIdx + 1, filterExpr.lastIndexOf(")")).trim();
            if ("format".equals(filterName)) {
                Object arg = evaluateString(argStr, context);
                if (base instanceof String) {
                    try {
                        return String.format((String) base, arg);
                    } catch (Exception e) {
                        return base.toString() + arg.toString();
                    }
                }
            }
        }
        return base != null ? base.toString() : "";
    }

    private Object callFunction(String funcName, String argsStr, Map<String, Object> context) {
        if ("url_for".equals(funcName)) {
            return resolveUrlFor(argsStr, context);
        }
        return "";
    }

    private String resolveUrlFor(String argsStr, Map<String, Object> context) {
        String[] args = argsStr.split(",");
        if (args.length == 0) return "";
        String routeName = args[0].trim();
        if (routeName.length() >= 2 && ((routeName.startsWith("'") && routeName.endsWith("'")) || (routeName.startsWith("\"") && routeName.endsWith("\"")))) {
            routeName = routeName.substring(1, routeName.length() - 1);
        }
        String funcName = urlForRoutes.get(routeName);
        if (funcName == null) funcName = routeName;

        // Check for product_id in keyword arguments
        String productId = null;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.contains("=")) {
                String[] kv = arg.split("=", 2);
                if ("product_id".equals(kv[0].trim())) {
                    Object value = evaluateString(kv[1].trim(), context);
                    if (value != null) {
                        productId = value.toString();
                    }
                    break;
                }
            }
        }

        // If detail/edit/delete with product_id, use per-product filename
        if (("detail".equals(funcName) || "edit".equals(funcName) || "delete".equals(funcName)) && productId != null) {
            return funcName + "_" + productId + ".html";
        }

        // Default behavior for all other routes
        String url = funcName + ".html";
        boolean firstParam = true;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.contains("=")) {
                String[] kv = arg.split("=", 2);
                String paramName = kv[0].trim();
                String paramValue = kv[1].trim();
                Object value = evaluateString(paramValue, context);
                url += (firstParam ? "?" : "&") + paramName + "=" + (value != null ? value.toString() : "");
                firstParam = false;
            }
        }
        return url;
    }

    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return !((String) value).isEmpty();
        if (value instanceof Number) return ((Number) value).doubleValue() != 0;
        if (value instanceof List) return !((List<?>) value).isEmpty();
        return true;
    }
}
