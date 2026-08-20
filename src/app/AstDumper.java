package app;

import models.App;
import models.Node;
import models.Template;
import util.Json;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Serializes the parse trees to JSON for compiler_output/.
 *
 * The dump is produced by reflecting over each node's declared fields rather
 * than hand-writing a case per model class. There are roughly a hundred model
 * classes and they change often; a hand-written dumper would silently omit new
 * ones, whereas this reflects whatever the AST actually holds.
 */
public class AstDumper {

    /** Depth guard, so an unexpected cycle cannot produce an endless document. */
    private static final int MAX_DEPTH = 60;

    /** Serializes the Python AST: one entry per top-level statement. */
    public static String dumpPythonAst(App app, String sourceFile) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("source", sourceFile);
        root.put("kind", "python");

        List<Object> nodes = new ArrayList<>();
        if (app != null && app.nodes != null) {
            for (Node node : app.nodes) nodes.add(toJson(node, 0, new IdentityHashMap<>()));
        }
        root.put("nodeCount", nodes.size());
        root.put("nodes", nodes);
        return Json.write(root);
    }

    /** Serializes every parsed template, keyed by file name. */
    public static String dumpJinjaAst(Map<String, Template> templates) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("kind", "jinja");
        root.put("templateCount", templates.size());

        Map<String, Object> byTemplate = new LinkedHashMap<>();
        for (Map.Entry<String, Template> entry : templates.entrySet()) {
            Template template = entry.getValue();
            List<Object> nodes = new ArrayList<>();
            if (template != null && template.nodes != null) {
                for (Node node : template.nodes) nodes.add(toJson(node, 0, new IdentityHashMap<>()));
            }
            Map<String, Object> one = new LinkedHashMap<>();
            one.put("nodeCount", nodes.size());
            one.put("nodes", nodes);
            byTemplate.put(entry.getKey(), one);
        }
        root.put("templates", byTemplate);
        return Json.write(root);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  REFLECTION
    // ═══════════════════════════════════════════════════════════════════════

    private static Object toJson(Object value, int depth, IdentityHashMap<Object, Boolean> seen) {
        if (value == null) return null;
        if (depth > MAX_DEPTH) return "<max depth reached>";

        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        if (value instanceof List) {
            List<Object> out = new ArrayList<>();
            for (Object item : (List<?>) value) out.add(toJson(item, depth + 1, seen));
            return out;
        }

        if (value instanceof Node) {
            if (seen.containsKey(value)) return "<cycle>";
            seen.put(value, Boolean.TRUE);
            Map<String, Object> out = describeNode((Node) value, depth, seen);
            seen.remove(value);
            return out;
        }

        return String.valueOf(value);
    }

    private static Map<String, Object> describeNode(Node node, int depth,
                                                    IdentityHashMap<Object, Boolean> seen) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("node", node.getClass().getSimpleName());
        if (node.getLineNumber() > 0) out.put("line", node.getLineNumber());

        // Walk the class hierarchy so inherited fields (tagName, attrList, ...)
        // are included alongside the concrete class's own.
        for (Class<?> type = node.getClass(); type != null && type != Object.class;
             type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                // lineNumber and nodeName are already represented above.
                if ("lineNumber".equals(field.getName())) continue;
                if ("nodeName".equals(field.getName())) continue;

                Object value = readField(field, node);
                if (value == null) continue;
                out.put(field.getName(), toJson(value, depth + 1, seen));
            }
        }
        return out;
    }

    private static Object readField(Field field, Object owner) {
        try {
            field.setAccessible(true);
            return field.get(owner);
        } catch (RuntimeException | IllegalAccessException e) {
            // A field we are not permitted to read is described rather than
            // failing the whole dump.
            return "<unreadable: " + field.getType().getSimpleName() + ">";
        }
    }
}
