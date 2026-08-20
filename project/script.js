/*
 * Client runtime for the generated static site.
 *
 * The generated pages are static files, so add / edit / delete have no server
 * to post to. This keeps the collection in localStorage instead — seeded once
 * from the build-time data in data.js, and treated as the source of truth from
 * then on. It is the same idea as stashing an auth token in localStorage: the
 * browser holds the state between page loads.
 *
 * Nothing here names "product" or "products". The collection, its key field and
 * the page for each route all come from the metadata the compiler emits into
 * data.js, so this file works for any model the backend defines.
 */
(function () {
    "use strict";

    var STORAGE_KEY = "flask-compiler-site";

    var seed   = window.__SITE_DATA__   || {};
    var routes = window.__SITE_ROUTES__ || {};

    // ── Which collection this site is about ────────────────────────────────
    // Taken from the first route that expands over one.
    var model = null;
    Object.keys(routes).forEach(function (name) {
        if (!model && routes[name].collection) model = routes[name];
    });
    var COLLECTION = model ? model.collection : null;
    var KEY = model ? model.key : "id";

    if (!COLLECTION) return; // Nothing dynamic on this site.

    // ═══════════════════════════════════════════════════════════════════════
    //  STORAGE
    // ═══════════════════════════════════════════════════════════════════════

    function readStore() {
        try {
            return JSON.parse(window.localStorage.getItem(STORAGE_KEY));
        } catch (e) {
            return null; // Corrupt or unavailable: fall back to the seed.
        }
    }

    function writeStore(state) {
        try {
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
            return true;
        } catch (e) {
            // Private mode or quota exhausted. The page still works for this
            // visit; the change just will not survive a reload.
            console.warn("Could not persist changes:", e.message);
            return false;
        }
    }

    function state() {
        var stored = readStore();
        if (stored && stored[COLLECTION]) return stored;
        var fresh = JSON.parse(JSON.stringify(seed));
        writeStore(fresh);
        return fresh;
    }

    function items() {
        var list = state()[COLLECTION];
        return Array.isArray(list) ? list : [];
    }

    function saveItems(list) {
        var s = state();
        s[COLLECTION] = list;
        writeStore(s);
    }

    function nextKey(list) {
        var max = 0;
        list.forEach(function (item) {
            var n = parseInt(item[KEY], 10);
            if (!isNaN(n) && n > max) max = n;
        });
        return max + 1;
    }

    function findItem(list, id) {
        for (var i = 0; i < list.length; i++) {
            if (String(list[i][KEY]) === String(id)) return list[i];
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  URLS
    // ═══════════════════════════════════════════════════════════════════════

    function pageFor(routeName, id) {
        var route = routes[routeName];
        if (!route) return "#";
        if (!route.parameterized || id === undefined || id === null) return route.page;
        // Pre-rendered per-item page if the compiler made one, otherwise the
        // shell page with the id in the query string.
        return route.page.replace(/\.html$/, "") + ".html?" + encodeURIComponent(route.param || "id")
               + "=" + encodeURIComponent(id);
    }

    /**
     * The id of the item this page is showing: from ?id=N, or from the
     * pre-rendered filename suffix (product_detail_2.html -> 2).
     */
    function currentId() {
        var params = new URLSearchParams(window.location.search);
        var names = [KEY];
        Object.keys(routes).forEach(function (n) {
            if (routes[n].param) names.push(routes[n].param);
        });
        for (var i = 0; i < names.length; i++) {
            if (params.has(names[i])) return params.get(names[i]);
        }

        var file = window.location.pathname.split("/").pop().replace(/\.html$/, "");
        var match = file.match(/_([^_]+)$/);
        return match ? match[1] : null;
    }

    function go(routeName, id) {
        window.location.href = pageFor(routeName, id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RENDERING
    // ═══════════════════════════════════════════════════════════════════════

    function formatValue(field, value) {
        if (value === undefined || value === null) return "";
        if (field === "price") {
            var n = Number(value);
            return isNaN(n) ? String(value) : n.toFixed(2);
        }
        return String(value);
    }

    /** Fills __FIELD__ placeholders in a cloned template. */
    function fillPlaceholders(html, item) {
        return html.replace(/__([A-Z0-9_]+)__/g, function (whole, name) {
            var field = name.toLowerCase();
            var value = field === "id" ? item[KEY] : item[field];
            if (value === undefined) return whole;
            // Placeholders sit in both text and attribute positions.
            return escapeAttr(formatValue(field, value));
        });
    }

    function escapeAttr(text) {
        return String(text)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;");
    }

    /** Rebuilds the listing from storage, replacing the pre-rendered cards. */
    function renderList() {
        var container = document.querySelector("[data-list]");
        if (!container) return;

        var itemTpl  = document.querySelector("[data-item-template]");
        var emptyTpl = document.querySelector("[data-empty-template]");
        if (!itemTpl) return;

        var list = items();
        var html = "";

        if (list.length === 0) {
            html = emptyTpl ? emptyTpl.innerHTML : "";
        } else {
            list.forEach(function (item) {
                html += fillPlaceholders(itemTpl.innerHTML, item);
            });
        }
        container.innerHTML = html;
    }

    /** Fills a detail page's [data-field] elements from storage. */
    function renderDetail() {
        var container = document.querySelector("[data-detail]");
        if (!container) return;

        var item = findItem(items(), currentId());
        if (!item) {
            container.innerHTML = "<p>This item no longer exists. "
                + '<a href="' + pageFor("index") + '">Back to list</a></p>';
            document.querySelectorAll("[data-field]").forEach(function (el) {
                if (!container.contains(el)) el.textContent = "Not found";
            });
            return;
        }

        document.querySelectorAll("[data-field]").forEach(function (el) {
            var field = el.getAttribute("data-field");
            var value = formatValue(field, item[field]);
            if (el.tagName === "IMG") {
                el.setAttribute("src", value);
                el.setAttribute("alt", formatValue("name", item.name));
            } else {
                el.textContent = value;
            }
        });

        if (item.name) document.title = item.name + " - " + document.title.split(" - ").pop();

        // Point per-item links at this item, which matters on the shell page
        // where the compiler had no id to bake in.
        document.querySelectorAll("[data-link]").forEach(function (el) {
            el.setAttribute("href", pageFor(el.getAttribute("data-link"), item[KEY]));
        });
    }

    /** Loads the current item into an edit form. */
    function populateEditForm() {
        var form = document.querySelector('[data-form="edit"]');
        if (!form) return;

        var item = findItem(items(), currentId());
        if (!item) return;

        Object.keys(item).forEach(function (field) {
            var input = form.querySelector('[name="' + field + '"]');
            if (input) input.value = item[field];
        });

        document.querySelectorAll("[data-link]").forEach(function (el) {
            el.setAttribute("href", pageFor(el.getAttribute("data-link"), item[KEY]));
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MUTATIONS
    // ═══════════════════════════════════════════════════════════════════════

    function formValues(form) {
        var values = {};
        new FormData(form).forEach(function (value, name) {
            values[name] = value;
        });
        // Keep numeric-looking fields numeric so sorting and formatting behave.
        Object.keys(values).forEach(function (name) {
            if (values[name] !== "" && !isNaN(Number(values[name]))) {
                values[name] = Number(values[name]);
            }
        });
        return values;
    }

    function bindAddForm() {
        var form = document.querySelector('[data-form="add"]');
        if (!form) return;

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            var list = items();
            var item = formValues(form);
            item[KEY] = nextKey(list);
            list.push(item);
            saveItems(list);
            go(form.getAttribute("data-redirect") || "index");
        });
    }

    function bindEditForm() {
        var form = document.querySelector('[data-form="edit"]');
        if (!form) return;

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            var id = currentId();
            var list = items();
            var item = findItem(list, id);
            if (!item) return go("index");

            var updated = formValues(form);
            Object.keys(updated).forEach(function (field) {
                item[field] = updated[field];
            });
            saveItems(list);
            go(form.getAttribute("data-redirect") || "index", item[KEY]);
        });
    }

    function bindDelete() {
        var form = document.querySelector('[data-action="delete"]');
        if (!form) return;

        form.addEventListener("submit", function (event) {
            event.preventDefault();
            var message = form.getAttribute("data-confirm");
            if (message && !window.confirm(message)) return;

            var id = currentId();
            saveItems(items().filter(function (item) {
                return String(item[KEY]) !== String(id);
            }));
            go(form.getAttribute("data-redirect") || "index");
        });
    }

    /** Confirmation for any other form that asks for it. */
    function bindConfirms() {
        document.querySelectorAll("form[data-confirm]").forEach(function (form) {
            if (form.hasAttribute("data-action")) return; // handled above
            form.addEventListener("submit", function (event) {
                if (!window.confirm(form.getAttribute("data-confirm"))) event.preventDefault();
            });
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BOOT
    // ═══════════════════════════════════════════════════════════════════════

    document.addEventListener("DOMContentLoaded", function () {
        renderList();
        renderDetail();
        populateEditForm();
        bindAddForm();
        bindEditForm();
        bindDelete();
        bindConfirms();
    });
})();
