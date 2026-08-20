package models;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the generator needs to know about one @app.route function.
 *
 * A route with no URL parameters produces a single page. A route with a
 * parameter (e.g. /product/&lt;int:product_id&gt;) produces one page per item of
 * the collection it selects from, which is discovered from the generator
 * expression in the function body rather than hardcoded to any variable name.
 */
public class RouteInfo {

    /** Flask view function name, i.e. the name url_for() refers to. */
    public String name;

    /** Raw URL pattern from the decorator, e.g. "/product/&lt;int:product_id&gt;". */
    public String urlPattern;

    /** URL parameter names in order, e.g. ["product_id"]. Empty for a static route. */
    public List<String> params = new ArrayList<>();

    /** Template passed to render_template, or null if the route renders nothing. */
    public String templateName;

    /** Keyword arguments passed to render_template that resolve to module data. */
    public Map<String, Object> context = new LinkedHashMap<>();

    /** Line the route function was declared on, for diagnostics. */
    public int line;

    // ── Per-item expansion, filled in only for parameterized routes ────────

    /** Module-level collection the route selects an item from, e.g. "products". */
    public String collectionName;

    /** Local name bound to the selected item, e.g. "product". */
    public String itemVarName;

    /** Item key compared against the URL parameter, e.g. "id". */
    public String itemKeyName;

    public boolean isParameterized() {
        return !params.isEmpty();
    }

    /** True when we know how to enumerate the items this route expands over. */
    public boolean canExpand() {
        return isParameterized()
                && collectionName != null
                && itemVarName != null
                && itemKeyName != null;
    }

    public boolean rendersTemplate() {
        return templateName != null;
    }

    @Override
    public String toString() {
        return name + " " + urlPattern
                + (templateName != null ? " -> " + templateName : " (no template)")
                + (canExpand() ? "  [per-item over " + collectionName + "]" : "");
    }
}
