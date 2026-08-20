/*
 * Drives the generated static site in jsdom to prove add / edit / delete
 * actually persist through localStorage across page loads.
 */
const { JSDOM, VirtualConsole } = require("jsdom");
const fs = require("fs");
const path = require("path");
const { pathToFileURL } = require("url");

const OUT = process.argv[2];
let pass = 0, fail = 0;

function check(desc, cond, extra) {
    if (cond) { console.log("  PASS  " + desc); pass++; }
    else { console.log("  FAIL  " + desc + (extra ? "  -> " + extra : "")); fail++; }
}

// One shared storage object, so navigating between pages keeps state the way a
// real browser would.
const storage = (() => {
    let data = {};
    return {
        getItem: k => (k in data ? data[k] : null),
        setItem: (k, v) => { data[k] = String(v); },
        removeItem: k => { delete data[k]; },
        clear: () => { data = {}; },
        get length() { return Object.keys(data).length; },
        key: i => Object.keys(data)[i] ?? null,
        _dump: () => data
    };
})();

/** Loads a generated page, executing data.js and script.js against shared storage. */
async function open(page, query) {
    const file = path.join(OUT, page);
    if (!fs.existsSync(file)) throw new Error("no such page: " + page);

    const url = pathToFileURL(file).href + (query ? "?" + query : "");
    const vc = new VirtualConsole();
    vc.on("jsdomError", e => { if (!/Could not load|Not implemented: navigation/.test(e.message)) console.log("    [jsdom] " + e.message); });

    const dom = new JSDOM(fs.readFileSync(file, "utf8"), {
        url,
        runScripts: "outside-only",
        virtualConsole: vc
    });

    // Wire our shared storage in before any page script runs.
    Object.defineProperty(dom.window, "localStorage", { value: storage, configurable: true });

    // Let jsdom's own DOMContentLoaded fire first. Evaluating the scripts before
    // it means the runtime's listener catches that event AND the one dispatched
    // below, binding every handler twice -- a harness artifact, not a page bug.
    await new Promise(resolve => setTimeout(resolve, 0));

    // Execute the two scripts the page references, in order.
    for (const src of ["data.js", "script.js"]) {
        dom.window.eval(fs.readFileSync(path.join(OUT, src), "utf8"));
    }
    dom.window.document.dispatchEvent(new dom.window.Event("DOMContentLoaded"));
    return dom;
}

function items(dom) {
    const raw = storage.getItem("flask-compiler-site");
    return raw ? JSON.parse(raw).products : null;
}

function cardTitles(dom) {
    return [...dom.window.document.querySelectorAll("[data-list] .card-title")]
        .map(el => el.textContent.trim());
}

(async () => {
    console.log("\n[A] index seeds storage from data.js and renders the list");
    let dom = await open("index.html");
    let list = items(dom);
    check("storage seeded from build data", list && list.length === 3, list && list.length);
    check("index renders 3 cards", cardTitles(dom).length === 3, cardTitles(dom).join("|"));
    check("card titles match the data",
        cardTitles(dom)[0] === "Wireless Headphones", cardTitles(dom)[0]);

    console.log("\n[B] add: submit the add form, then reload the index");
    dom = await open("add_product.html");
    let form = dom.window.document.querySelector('[data-form="add"]');
    check("add form found", !!form);
    form.querySelector('[name="name"]').value = "Desk Lamp";
    form.querySelector('[name="price"]').value = "42.50";
    form.querySelector('[name="details"]').value = "Adjustable LED lamp.";
    form.querySelector('[name="image"]').value = "lamp.png";
    form.dispatchEvent(new dom.window.Event("submit", { bubbles: true, cancelable: true }));

    list = items(dom);
    check("storage now holds 4 items", list.length === 4, list.length);
    check("new item got the next id", list[3].id === 4, list[3].id);
    check("new item kept its price as a number", list[3].price === 42.5, list[3].price);

    dom = await open("index.html");
    check("index shows the new product after reload",
        cardTitles(dom).includes("Desk Lamp"), cardTitles(dom).join("|"));
    check("index now shows 4 cards", cardTitles(dom).length === 4, cardTitles(dom).length);

    console.log("\n[C] detail: the new product has no pre-rendered page, uses ?id=");
    dom = await open("product_detail.html", "product_id=4");
    let name = dom.window.document.querySelector('[data-field="name"]').textContent.trim();
    let price = dom.window.document.querySelector('[data-field="price"]').textContent.trim();
    check("shell page renders the new product", name === "Desk Lamp", name);
    check("price formatted to 2dp", price === "42.50", price);
    let editHref = dom.window.document.querySelector('[data-link="edit"]').getAttribute("href");
    check("edit link points at the new item", /id=4/.test(editHref), editHref);

    console.log("\n[D] edit: change a build-time product, check it persists");
    dom = await open("edit_product.html", "product_id=2");
    form = dom.window.document.querySelector('[data-form="edit"]');
    check("edit form pre-filled from storage",
        form.querySelector('[name="name"]').value === "Mechanical Keyboard",
        form.querySelector('[name="name"]').value);
    form.querySelector('[name="name"]').value = "Ergonomic Keyboard";
    form.querySelector('[name="price"]').value = "149.99";
    form.dispatchEvent(new dom.window.Event("submit", { bubbles: true, cancelable: true }));

    list = items(dom);
    const edited = list.find(p => p.id === 2);
    check("edit persisted the new name", edited.name === "Ergonomic Keyboard", edited.name);
    check("edit persisted the new price", edited.price === 149.99, edited.price);
    check("edit did not add or drop items", list.length === 4, list.length);

    console.log("\n[E] the pre-rendered detail page reflects the edit");
    dom = await open("product_detail_2.html");
    name = dom.window.document.querySelector('[data-field="name"]').textContent.trim();
    check("pre-rendered page hydrated from storage", name === "Ergonomic Keyboard", name);

    console.log("\n[F] delete: remove a product, confirm it is gone");
    dom = await open("product_detail_1.html");
    dom.window.confirm = () => true;
    const del = dom.window.document.querySelector('[data-action="delete"]');
    check("delete form found", !!del);
    del.dispatchEvent(new dom.window.Event("submit", { bubbles: true, cancelable: true }));

    list = items(dom);
    check("storage down to 3 items", list.length === 3, list.length);
    check("deleted item is gone", !list.some(p => p.id === 1), JSON.stringify(list.map(p => p.id)));

    dom = await open("index.html");
    check("index no longer shows the deleted product",
        !cardTitles(dom).includes("Wireless Headphones"), cardTitles(dom).join("|"));

    console.log("\n[G] delete is cancellable");
    dom = await open("product_detail_2.html");
    dom.window.confirm = () => false;
    dom.window.document.querySelector('[data-action="delete"]')
        .dispatchEvent(new dom.window.Event("submit", { bubbles: true, cancelable: true }));
    check("cancelling delete keeps the item", items(dom).length === 3, items(dom).length);

    console.log("\n[H] empty state after deleting everything");
    storage.setItem("flask-compiler-site", JSON.stringify({ products: [] }));
    dom = await open("index.html");
    check("empty-state message shown",
        !!dom.window.document.querySelector("[data-empty]"),
        dom.window.document.querySelector("[data-list]").innerHTML.slice(0, 60));
    check("no cards rendered", cardTitles(dom).length === 0, cardTitles(dom).length);

    console.log("\n" + (fail === 0 ? "ALL " + pass + " CHECKS PASSED" : fail + " of " + (pass + fail) + " FAILED"));
    process.exit(fail === 0 ? 0 : 1);
})().catch(e => { console.error("harness error:", e); process.exit(2); });
