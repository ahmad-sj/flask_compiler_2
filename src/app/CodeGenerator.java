package app;

import models.App;
import models.RouteInfo;
import models.Template;
import util.BuildLog;
import visitors.ExpressionEvaluator;
import visitors.JinjaRenderer;
import visitors.PythonDataExtractor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns the extracted Python data plus the parsed templates into static HTML.
 *
 * One page is produced per route. A route whose URL carries a parameter
 * produces one page per item of the collection it selects from, which is read
 * from the route model rather than hardcoded to any particular variable name.
 */
public class CodeGenerator {

    private final App app;
    private final Map<String, Template> templates;
    private final CompilerConfig config;
    private final BuildLog log;

    private final List<String> problems = new ArrayList<>();
    private final Set<String> reportedProblems = new LinkedHashSet<>();

    private Map<String, Object> moduleVars = new LinkedHashMap<>();
    private List<RouteInfo> routes = new ArrayList<>();
    private final Map<String, RouteInfo> routesByName = new LinkedHashMap<>();

    private int pagesGenerated;

    /** Set while rendering a shell page, whose blank fields are expected. */
    private boolean suppressProblems;

    /** Extraction results, produced by FlaskCompiler before the template checks. */
    private final PythonDataExtractor extractor;

    public CodeGenerator(App app, Map<String, Template> templates,
                         CompilerConfig config, BuildLog log,
                         PythonDataExtractor extractor) {
        this.app = app;
        this.templates = templates;
        this.config = config;
        this.log = log;
        this.extractor = extractor;
    }

    public int getPagesGenerated() {
        return pagesGenerated;
    }

    public List<String> getProblems() {
        return problems;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MAIN
    // ═══════════════════════════════════════════════════════════════════════

    /** Runs generation. Returns false if it was skipped or failed. */
    public boolean generate() throws IOException {
        log.section("Generation");

        if (app == null) {
            log.error("No Python AST available - generation skipped.");
            return false;
        }
        if (!app.semanticErrors.isEmpty()) {
            log.error("Skipped: " + app.semanticErrors.size()
                    + " semantic error(s) must be fixed before generating.");
            return false;
        }

        // ── Data the templates will be rendered against ───────────────────
        // Extraction already ran in FlaskCompiler so the template analyzer
        // could check against the same context this renders with.
        moduleVars = extractor.getModuleVars();
        routes = extractor.getRoutes();
        for (RouteInfo route : routes) routesByName.put(route.name, route);

        log.info("Module-level variables: " + moduleVars.keySet());
        log.info("Routes discovered: " + routes.size());
        for (RouteInfo route : routes) log.info("  " + route);

        if (routes.isEmpty()) {
            log.warn("No @app.route functions found - no pages to generate.");
        }

        Files.createDirectories(config.outputDir);

        JinjaRenderer renderer = new JinjaRenderer(templates, this::urlFor, this::reportProblem);

        // ── Render one page per route (or per item for parameterized routes)
        for (RouteInfo route : routes) {
            if (!route.rendersTemplate()) {
                log.info("Route '" + route.name + "' renders no template - skipped.");
                continue;
            }
            Template template = templates.get(route.templateName);
            if (template == null) {
                log.warn("Route '" + route.name + "' references unknown template '"
                        + route.templateName + "' - skipped.");
                continue;
            }
            if (route.isParameterized()) {
                renderParameterizedRoute(route, template, renderer);
            } else {
                renderSinglePage(route, template, renderer, baseContext(route), pageName(route, null));
            }
        }

        copyStaticAssets();

        log.info("Pages generated: " + pagesGenerated);
        if (!problems.isEmpty()) {
            log.warn(problems.size() + " rendering problem(s) - see semantic_report.txt");
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PAGE RENDERING
    // ═══════════════════════════════════════════════════════════════════════

    private void renderParameterizedRoute(RouteInfo route, Template template,
                                          JinjaRenderer renderer) throws IOException {
        if (!route.canExpand()) {
            log.warn("Route '" + route.name + "' takes URL parameter(s) " + route.params
                    + " but the collection it selects from could not be determined"
                    + " - generating a single page without item data.");
            renderSinglePage(route, template, renderer, baseContext(route), pageName(route, null));
            return;
        }

        Object collection = moduleVars.get(route.collectionName);
        if (!(collection instanceof List)) {
            log.warn("Route '" + route.name + "' expands over '" + route.collectionName
                    + "', which is not a list - skipped.");
            return;
        }

        List<?> items = (List<?>) collection;
        if (items.isEmpty()) {
            log.warn("Route '" + route.name + "' expands over an empty '"
                    + route.collectionName + "' - generating the shell page only.");
        }

        for (Object item : items) {
            Map<String, Object> context = baseContext(route);
            context.put(route.itemVarName, item);

            Object key = itemKey(item, route.itemKeyName);
            if (key == null) {
                log.warn("Item in '" + route.collectionName + "' has no '"
                        + route.itemKeyName + "' - skipped.");
                continue;
            }
            renderSinglePage(route, template, renderer, context, pageName(route, key));
        }

        renderShellPage(route, template, renderer);
    }

    /**
     * Renders the un-suffixed page for a parameterized route, e.g.
     * product_detail.html, used as product_detail.html?id=N.
     *
     * Items created in the browser after the build have no pre-rendered page,
     * so the runtime sends them here and fills the fields from localStorage.
     * The item is bound to an empty map so the template's field lookups render
     * blank rather than reporting an undefined variable for every field.
     */
    private void renderShellPage(RouteInfo route, Template template,
                                 JinjaRenderer renderer) throws IOException {
        Map<String, Object> context = baseContext(route);
        context.put(route.itemVarName, new LinkedHashMap<String, Object>());

        // Every field lookup on the empty item is expected to come up blank, so
        // those diagnostics are noise rather than findings.
        suppressProblems = true;
        try {
            renderSinglePage(route, template, renderer, context, pageName(route, null));
        } finally {
            suppressProblems = false;
        }
    }

    private void renderSinglePage(RouteInfo route, Template template, JinjaRenderer renderer,
                                  Map<String, Object> context, String fileName) throws IOException {
        String html = renderer.render(template, context);
        Path target = config.outputDir.resolve(fileName);
        Files.write(target, html.getBytes(StandardCharsets.UTF_8));
        pagesGenerated++;
        log.info("Rendered " + route.templateName + " -> " + fileName
                + " (" + html.length() + " bytes)");
    }

    /**
     * Module data plus the route's render_template keyword arguments.
     * Names the route passed explicitly win over module-level ones.
     */
    private Map<String, Object> baseContext(RouteInfo route) {
        Map<String, Object> context = new LinkedHashMap<>(moduleVars);
        for (Map.Entry<String, Object> entry : route.context.entrySet()) {
            // A kwarg that resolved to null was a local the extractor could not
            // evaluate statically; keep the module value if there is one.
            if (entry.getValue() != null || !context.containsKey(entry.getKey())) {
                context.put(entry.getKey(), entry.getValue());
            }
        }
        return context;
    }

    private Object itemKey(Object item, String keyName) {
        if (item instanceof Map) return ((Map<?, ?>) item).get(keyName);
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  URL RESOLUTION
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Maps url_for('route', k=v) onto the generated filename for that route.
     * Replaces the previous version, which recognised only the literal route
     * names "detail", "edit" and "delete" and the parameter "product_id".
     */
    private String urlFor(String routeName, Map<String, Object> params) {
        RouteInfo route = routesByName.get(routeName);
        if (route == null) {
            reportProblem("url_for('" + routeName + "') refers to no known route");
            return "#";
        }
        if (!route.rendersTemplate()) {
            // e.g. a POST-only delete endpoint: there is no static page for it.
            reportProblem("url_for('" + routeName + "') points at a route that renders"
                    + " no template; emitting '#'");
            return "#";
        }
        if (!route.isParameterized()) return pageName(route, null);

        // Use the parameter the route actually declares.
        for (String declared : route.params) {
            if (params.containsKey(declared)) return pageName(route, params.get(declared));
        }
        if (params.size() == 1) return pageName(route, params.values().iterator().next());

        reportProblem("url_for('" + routeName + "') is missing parameter(s) " + route.params);
        return pageName(route, null);
    }

    /** Output filename: the template's base name, suffixed for per-item pages. */
    private String pageName(RouteInfo route, Object key) {
        String base = stripExtension(route.templateName);
        return key == null ? base + ".html" : base + "_" + key + ".html";
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STATIC ASSETS
    // ═══════════════════════════════════════════════════════════════════════

    /** Copies app.py, style.css and script.js to the output untransformed. */
    private void copyStaticAssets() throws IOException {
        log.section("Static assets");
        for (String asset : CompilerConfig.STATIC_ASSETS) {
            Path source = config.inputDir.resolve(asset);
            if (!Files.exists(source)) {
                log.info(asset + " not present - skipped.");
                continue;
            }
            Path target = config.outputDir.resolve(asset);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Copied " + asset + " (" + Files.size(source) + " bytes)");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PROBLEM REPORTING
    // ═══════════════════════════════════════════════════════════════════════

    /** Records a rendering problem once, however many pages hit it. */
    private void reportProblem(String message) {
        if (suppressProblems) return;
        if (reportedProblems.add(message)) problems.add(message);
    }
}
