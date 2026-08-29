package visitors;

import models.DocType;
import models.Node;
import models.NodeBody;
import models.NormalText;
import models.Template;
import models.html.attributes.Attribute;
import models.html.attributes.BooleanAttribute;
import models.html.attributes.QuotedAttribute;
import models.html.attributes.UnquotedAttribute;
import models.html.elements.HtmlElement;
import models.html.elements.HtmlRegularElement;
import models.html.elements.HtmlSelfClosingElement;
import models.html.elements.HtmlStyleElement;
import models.jinja.JinjaExpression;
import models.jinja.blocks.ElifBlock;
import models.jinja.blocks.ElseBlock;
import models.jinja.blocks.ExtendsBlock;
import models.jinja.blocks.ForBlock;
import models.jinja.blocks.IfBlock;
import models.jinja.blocks.InheritedBlock;
import models.jinja.blocks.SetStatement;
import models.jinja.atoms.IdType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders a parsed template to HTML by walking its AST.
 *
 * The previous implementation serialized the AST back to template text with
 * toString() and then ran regular expressions over that text. That could not
 * express nesting, silently dropped every {% else %} branch, and reproduced
 * whatever the printer happened to emit rather than what was parsed. This
 * walker visits the nodes directly, so the structure the parser found is the
 * structure that gets rendered.
 */
public class JinjaRenderer {

    private final Map<String, Template> templateRegistry;
    private final ExpressionEvaluator evaluator;
    private final ExpressionEvaluator.Problems problems;

    /** Guards against an {% extends %} cycle. */
    private final Set<String> inheritanceStack = new HashSet<>();

    public JinjaRenderer(Map<String, Template> templateRegistry,
                         ExpressionEvaluator.UrlResolver urlResolver,
                         ExpressionEvaluator.Problems problems) {
        this.templateRegistry = templateRegistry;
        this.problems = problems;
        this.evaluator = new ExpressionEvaluator(urlResolver, problems);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════

    public String render(Template template, Map<String, Object> context) {
        inheritanceStack.clear();
        StringBuilder out = new StringBuilder();
        // The context is copied so {% set %} in one page cannot leak into the next.
        renderTemplate(template, new LinkedHashMap<>(context), out, new HashMap<>());
        return out.toString();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TEMPLATE INHERITANCE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Renders a template, resolving {% extends %} by rendering the parent with
     * this template's {% block %} definitions layered on top.
     */
    private void renderTemplate(Template template, Map<String, Object> context,
                                StringBuilder out, Map<String, InheritedBlock> overrides) {
        if (template == null || template.nodes == null) return;

        ExtendsBlock extendsBlock = findExtends(template);
        if (extendsBlock == null) {
            renderNodes(template.nodes, context, out, overrides);
            return;
        }

        String parentName = ExpressionEvaluator.unquote(extendsBlock.templateName);
        if (!inheritanceStack.add(parentName)) {
            problems.report("Circular {% extends %} detected at '" + parentName + "'");
            return;
        }

        Template parent = templateRegistry.get(parentName);
        if (parent == null) {
            problems.report("{% extends %} refers to unknown template '" + parentName + "'");
            inheritanceStack.remove(parentName);
            return;
        }

        // A child's blocks override the parent's; blocks already overridden by a
        // deeper child win, so only fill in names not yet claimed.
        Map<String, InheritedBlock> merged = new HashMap<>(overrides);
        for (InheritedBlock block : collectBlocks(template.nodes)) {
            merged.putIfAbsent(block.blockName, block);
        }

        renderTemplate(parent, context, out, merged);
        inheritanceStack.remove(parentName);
    }

    private ExtendsBlock findExtends(Template template) {
        for (Node node : template.nodes) {
            if (node instanceof ExtendsBlock) return (ExtendsBlock) node;
        }
        return null;
    }

    /** Collects {% block %} definitions, including ones nested inside other blocks. */
    private List<InheritedBlock> collectBlocks(List<Node> nodes) {
        List<InheritedBlock> found = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof InheritedBlock) {
                InheritedBlock block = (InheritedBlock) node;
                found.add(block);
                if (block.nodeBody instanceof NodeBody) {
                    found.addAll(collectBlocks(((NodeBody) block.nodeBody).nodeList));
                }
            }
        }
        return found;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NODE DISPATCH
    // ═══════════════════════════════════════════════════════════════════════

    private void renderNodes(List<Node> nodes, Map<String, Object> context,
                             StringBuilder out, Map<String, InheritedBlock> overrides) {
        if (nodes == null) return;
        for (Node node : nodes) {
            renderNode(node, context, out, overrides);
        }
    }

    private void renderNode(Node node, Map<String, Object> context,
                            StringBuilder out, Map<String, InheritedBlock> overrides) {
        if (node == null) return;

        if (node instanceof DocType) {
            out.append(((DocType) node).declaration).append('\n');

        } else if (node instanceof NormalText) {
            out.append(((NormalText) node).text);

        } else if (node instanceof JinjaExpression) {
            JinjaExpression expr = (JinjaExpression) node;
            out.append(ExpressionEvaluator.str(evaluator.eval(expr.expression, context)));

        } else if (node instanceof NodeBody) {
            renderBody((NodeBody) node, context, out, overrides);

        } else if (node instanceof IfBlock) {
            renderIf((IfBlock) node, context, out, overrides);

        } else if (node instanceof ForBlock) {
            renderFor((ForBlock) node, context, out, overrides);

        } else if (node instanceof InheritedBlock) {
            renderBlock((InheritedBlock) node, context, out, overrides);

        } else if (node instanceof SetStatement) {
            SetStatement set = (SetStatement) node;
            context.put(nameOf(set.id), evaluator.eval(set.expr, context));

        } else if (node instanceof ExtendsBlock) {
            // Already handled by renderTemplate.

        } else if (node instanceof ElifBlock || node instanceof ElseBlock) {
            // Only meaningful as part of an if/for, which handle them directly.

        } else if (node instanceof HtmlElement) {
            renderHtmlElement((HtmlElement) node, context, out, overrides);

        } else {
            // Unknown node types fall back to their printed form rather than
            // vanishing from the page.
            out.append(node.toString());
        }
    }

    /**
     * Renders a body's children, separating them with newlines.
     *
     * The lexer's NORMAL_TEXT rule excludes \r and \n, so original inter-node
     * whitespace is not recoverable from the token stream; a newline between
     * siblings keeps the output readable and valid.
     */
    private void renderBody(NodeBody body, Map<String, Object> context,
                            StringBuilder out, Map<String, InheritedBlock> overrides) {
        if (body.nodeList == null) return;
        for (int i = 0; i < body.nodeList.size(); i++) {
            Node child = body.nodeList.get(i);
            if (child instanceof ElifBlock || child instanceof ElseBlock) continue;
            renderNode(child, context, out, overrides);
            if (i + 1 < body.nodeList.size()) out.append('\n');
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  {% if %} / {% elif %} / {% else %}
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * The grammar nests elif and else inside the if body, so the if-branch is
     * everything before the first elif/else and each elif/else carries its own
     * body. The previous regex renderer had no way to express this and dropped
     * every alternative branch.
     */
    private void renderIf(IfBlock ifBlock, Map<String, Object> context,
                          StringBuilder out, Map<String, InheritedBlock> overrides) {
        List<Node> body = childrenOf(ifBlock.nodeBody);

        if (ExpressionEvaluator.truthy(evaluator.eval(ifBlock.condition, context))) {
            renderUntilBranch(body, context, out, overrides);
            return;
        }

        for (Node node : body) {
            if (node instanceof ElifBlock) {
                ElifBlock elif = (ElifBlock) node;
                if (ExpressionEvaluator.truthy(evaluator.eval(elif.condition, context))) {
                    renderNode(elif.nodeBody, context, out, overrides);
                    return;
                }
            } else if (node instanceof ElseBlock) {
                renderNode(((ElseBlock) node).nodeBody, context, out, overrides);
                return;
            }
        }
    }

    /** Renders body nodes up to the first elif/else marker. */
    private void renderUntilBranch(List<Node> body, Map<String, Object> context,
                                   StringBuilder out, Map<String, InheritedBlock> overrides) {
        for (int i = 0; i < body.size(); i++) {
            Node node = body.get(i);
            if (node instanceof ElifBlock || node instanceof ElseBlock) return;
            renderNode(node, context, out, overrides);
            if (i + 1 < body.size()) out.append('\n');
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  {% for %} / {% else %}
    // ═══════════════════════════════════════════════════════════════════════

    private void renderFor(ForBlock forBlock, Map<String, Object> context,
                           StringBuilder out, Map<String, InheritedBlock> overrides) {
        Object iterable = evaluator.eval(forBlock.iterable, context);
        List<Node> body = childrenOf(forBlock.nodeBody);
        List<Object> items = asIterable(iterable, forBlock);

        if (items.isEmpty()) {
            // {% for %}...{% else %}...{% endfor %} renders the else branch
            // when the sequence is empty.
            for (Node node : body) {
                if (node instanceof ElseBlock) {
                    renderNode(((ElseBlock) node).nodeBody, context, out, overrides);
                    return;
                }
            }
            return;
        }

        List<String> varNames = loopVarNames(forBlock);
        for (int index = 0; index < items.size(); index++) {
            Map<String, Object> scope = new LinkedHashMap<>(context);
            bindLoopVars(scope, varNames, items.get(index));
            scope.put("loop", loopMetadata(index, items.size()));
            renderUntilBranch(body, scope, out, overrides);
            if (index + 1 < items.size()) out.append('\n');
        }
    }

    /** Supports {% for k, v in pairs %} by unpacking a list or map entry. */
    private void bindLoopVars(Map<String, Object> scope, List<String> names, Object item) {
        if (names.size() == 1) {
            scope.put(names.get(0), item);
            return;
        }
        if (item instanceof List) {
            List<?> parts = (List<?>) item;
            for (int i = 0; i < names.size(); i++) {
                scope.put(names.get(i), i < parts.size() ? parts.get(i) : null);
            }
            return;
        }
        if (item instanceof Map.Entry) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) item;
            scope.put(names.get(0), entry.getKey());
            if (names.size() > 1) scope.put(names.get(1), entry.getValue());
            return;
        }
        // Not unpackable: bind the whole item to the first name.
        scope.put(names.get(0), item);
        for (int i = 1; i < names.size(); i++) scope.put(names.get(i), null);
    }

    /** Jinja's loop variable, providing loop.index and friends. */
    private Map<String, Object> loopMetadata(int index, int total) {
        Map<String, Object> loop = new LinkedHashMap<>();
        loop.put("index", index + 1);
        loop.put("index0", index);
        loop.put("revindex", total - index);
        loop.put("revindex0", total - index - 1);
        loop.put("first", index == 0);
        loop.put("last", index == total - 1);
        loop.put("length", total);
        return loop;
    }

    private List<Object> asIterable(Object value, ForBlock forBlock) {
        List<Object> items = new ArrayList<>();
        if (value == null) {
            problems.report("{% for %} over an undefined value at line " + forBlock.getLineNumber());
            return items;
        }
        if (value instanceof List) {
            items.addAll((List<?>) value);
        } else if (value instanceof Map) {
            items.addAll(((Map<?, ?>) value).entrySet());
        } else if (value instanceof String) {
            for (char c : ((String) value).toCharArray()) items.add(String.valueOf(c));
        } else {
            problems.report("{% for %} needs a list or dict, got "
                    + value.getClass().getSimpleName() + " at line " + forBlock.getLineNumber());
        }
        return items;
    }

    private List<String> loopVarNames(ForBlock forBlock) {
        List<String> names = new ArrayList<>();
        if (forBlock.loopVars != null) {
            for (Node var : forBlock.loopVars) names.add(nameOf(var));
        }
        if (names.isEmpty()) names.add("item");
        return names;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  {% block %}
    // ═══════════════════════════════════════════════════════════════════════

    private void renderBlock(InheritedBlock block, Map<String, Object> context,
                             StringBuilder out, Map<String, InheritedBlock> overrides) {
        InheritedBlock effective = overrides.getOrDefault(block.blockName, block);
        // The override's own body is rendered, but any nested block inside it can
        // still be overridden further down, so keep passing the override map.
        renderNode(effective.nodeBody, context, out, overrides);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HTML
    // ═══════════════════════════════════════════════════════════════════════

    private void renderHtmlElement(HtmlElement element, Map<String, Object> context,
                                   StringBuilder out, Map<String, InheritedBlock> overrides) {
        out.append('<').append(element.tagName);
        renderAttributes(element.attrList, context, out);

        if (element instanceof HtmlSelfClosingElement) {
            out.append("/>");
            return;
        }

        out.append('>');

        Node body = null;
        if (element instanceof HtmlRegularElement) {
            body = ((HtmlRegularElement) element).elementBody;
        } else if (element instanceof HtmlStyleElement) {
            // A <style> element used to fall through to element.toString(),
            // which re-emits the whole element including its own <style> and
            // </style>. The opening tag was therefore written twice and the
            // page came out as "<style><style>...</style>": the first tag was
            // closed by the only </style>, so a browser treated the second one
            // and the entire stylesheet after it as text and dropped the CSS.
            body = ((HtmlStyleElement) element).elementBody;
        } else {
            // No other HtmlElement subclass exists today. Emitting the body
            // verbatim keeps a future one from vanishing, and the closing tag
            // below still balances the opening tag written above.
            out.append(element.toString());
        }

        if (body != null) {
            out.append('\n');
            renderNode(body, context, out, overrides);
            out.append('\n');
        }
        out.append("</").append(element.tagName).append('>');
    }

    private void renderAttributes(List<Node> attributes, Map<String, Object> context,
                                  StringBuilder out) {
        if (attributes == null) return;
        for (Node node : attributes) {
            out.append(' ');
            if (node instanceof QuotedAttribute) {
                QuotedAttribute attr = (QuotedAttribute) node;
                out.append(attr.attrName).append("=\"");
                renderAttributeValue(attr.attrValList, context, out);
                out.append('"');
            } else if (node instanceof BooleanAttribute) {
                out.append(((BooleanAttribute) node).attrName);
            } else if (node instanceof UnquotedAttribute) {
                UnquotedAttribute attr = (UnquotedAttribute) node;
                out.append(attr.attrName).append('=').append(attr.attrValue);
            } else {
                // Style attributes and anything else print themselves.
                out.append(node.toString());
            }
        }
    }

    /**
     * Attribute values are a list of literal chunks and embedded {{ ... }}
     * expressions, e.g. src="{{ product.image }}". Chunks are joined with a
     * space, matching how the parser splits them, but an expression that sits
     * alone must not gain surrounding spaces or URLs would break.
     */
    private void renderAttributeValue(List<Node> parts, Map<String, Object> context,
                                      StringBuilder out) {
        if (parts == null) return;
        for (int i = 0; i < parts.size(); i++) {
            Node part = parts.get(i);
            if (part instanceof JinjaExpression) {
                JinjaExpression expr = (JinjaExpression) part;
                out.append(ExpressionEvaluator.str(evaluator.eval(expr.expression, context)));
            } else {
                out.append(part.toString());
            }
            if (i + 1 < parts.size()) out.append(' ');
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private List<Node> childrenOf(Node body) {
        if (body instanceof NodeBody && ((NodeBody) body).nodeList != null) {
            return ((NodeBody) body).nodeList;
        }
        List<Node> single = new ArrayList<>();
        if (body != null) single.add(body);
        return single;
    }

    private String nameOf(Node node) {
        if (node instanceof IdType) return ((IdType) node).name;
        return node == null ? "" : node.toString();
    }
}
