/* ===========================================================================
   Browser runtime for the generated static site.
   ---------------------------------------------------------------------------
   The compiler emits a STATIC site: there is no server, so the <form> elements
   it generates have nowhere to post. This file makes add / edit / delete work
   in the browser by keeping the product list in localStorage and re-rendering
   the pages from it.

   It is NOT part of the compiler. It is a plain asset copied to output/
   untransformed, exactly like style.css (CompilerConfig.STATIC_ASSETS).

   It depends on nothing: no framework, no build step, no data.js. The initial
   product list is read out of the page the compiler already rendered.

   HOW IT FINDS THINGS
     The templates carry data-* hooks that survive generation:
       [data-list="products"]  the card grid on the index page
       [data-item]             one product card
       [data-detail]           the detail page container
       [data-field="name"]     a field to fill on the detail page
       [data-form="add"]       the add form
       [data-form="edit"]      the edit form
       [data-empty]            the "no products yet" block
     Everything below is driven by those, plus the page's own file name.

   PAGE NAMING
     The compiler names per-item pages <template>_<id>.html, and also emits an
     un-suffixed shell page per parameterised route. Items created in the
     browser have no pre-rendered page, so their links point at the shell with
     a query string: product_detail.html?id=7.
   =========================================================================== */

(function () {
    'use strict';

    var STORE = 'flask-compiler-site';

    /* ---------------------------------------------------------------------
       STORAGE
       localStorage throws in a few real situations - private windows, site
       data blocked, quota - so every access is guarded. A failure degrades to
       "no persistence", never to a broken page.
       --------------------------------------------------------------------- */

    function read() {
        try {
            var raw = window.localStorage.getItem(STORE);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            return null;
        }
    }

    function write(state) {
        try {
            window.localStorage.setItem(STORE, JSON.stringify(state));
        } catch (e) {
            /* nothing to do: the current page is still correct, it just will
               not survive a reload. */
        }
    }

    /* ---------------------------------------------------------------------
       PAGE IDENTITY
       --------------------------------------------------------------------- */

    function fileName() {
        var path = window.location.pathname;
        var slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    /** "index" | "add" | "edit" | "delete" | "detail" | null */
    function pageKind() {
        var f = fileName();
        if (f === '' || f.indexOf('index') === 0) return 'index';
        if (f.indexOf('add_product') === 0) return 'add';
        if (f.indexOf('edit_product') === 0) return 'edit';
        if (f.indexOf('delete_product') === 0) return 'delete';
        if (f.indexOf('product_detail') === 0) return 'detail';
        return null;
    }

    /**
     * Which product this page is about.
     * A pre-rendered page carries it in the file name (edit_product_3.html).
     * A shell page carries it in the query string (edit_product.html?id=7),
     * which is how items created in the browser are reached.
     */
    function currentId() {
        var m = /_(\d+)\.html$/.exec(fileName());
        if (m) return Number(m[1]);

        var q = /[?&]id=(\d+)/.exec(window.location.search);
        return q ? Number(q[1]) : null;
    }

    /* ---------------------------------------------------------------------
       SEEDING
       On the very first visit there is nothing stored, so the list is read out
       of the page the compiler rendered. The index cards carry id, name, price
       and image; details only exists on a detail page, so it is filled in
       later, the first time that page is opened.
       --------------------------------------------------------------------- */

    function text(el) {
        return el ? String(el.textContent).trim() : '';
    }

    function idFromHref(href) {
        if (!href) return null;
        var m = /_(\d+)\.html/.exec(href) || /[?&]id=(\d+)/.exec(href);
        return m ? Number(m[1]) : null;
    }

    function seedFromIndex() {
        var items = [];
        var cards = document.querySelectorAll('[data-item]');

        for (var i = 0; i < cards.length; i++) {
            var card = cards[i];
            var link = card.querySelector('a[href]');
            var img = card.querySelector('img');
            var id = idFromHref(link ? link.getAttribute('href') : null);
            if (id === null) continue;

            items.push({
                id: id,
                name: text(card.querySelector('.card-title')),
                // "$79.99" -> 79.99 ; strip anything that is not a digit or dot
                price: Number(text(card.querySelector('.price')).replace(/[^0-9.]/g, '')) || 0,
                image: img ? img.getAttribute('src') : '',
                details: ''
            });
        }
        return items;
    }

    /** Ids the compiler produced a real page for. Anything else uses the shell. */
    function prerenderedIds(items) {
        var ids = [];
        for (var i = 0; i < items.length; i++) ids.push(items[i].id);
        return ids;
    }

    function state() {
        var s = read();
        if (s && s.items) return s;

        var items = pageKind() === 'index' ? seedFromIndex() : [];
        s = {
            items: items,
            prerendered: prerenderedIds(items),
            nextId: nextFreeId(items)
        };
        // Only persist a seed that actually found something, so opening a
        // detail page first does not store an empty list and hide everything.
        if (items.length) write(s);
        return s;
    }

    function nextFreeId(items) {
        var max = 0;
        for (var i = 0; i < items.length; i++) {
            if (items[i].id > max) max = items[i].id;
        }
        return max + 1;
    }

    function find(s, id) {
        for (var i = 0; i < s.items.length; i++) {
            if (s.items[i].id === id) return s.items[i];
        }
        return null;
    }

    /* ---------------------------------------------------------------------
       LINKS
       A product the compiler saw has a real page. One added in the browser
       does not, so it is reached through the un-suffixed shell page.
       --------------------------------------------------------------------- */

    function hrefFor(s, base, id) {
        var known = s.prerendered && s.prerendered.indexOf(id) >= 0;
        return known ? base + '_' + id + '.html' : base + '.html?id=' + id;
    }

    /* ---------------------------------------------------------------------
       OUTPUT SAFETY
       The compiler does not HTML-escape substituted values, and neither does
       innerHTML. Anything that came from a form is escaped here before it is
       written back into the page.
       --------------------------------------------------------------------- */

    function esc(value) {
        return String(value === null || value === undefined ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function money(value) {
        var n = Number(value);
        return isNaN(n) ? '0.00' : n.toFixed(2);
    }

    /* ---------------------------------------------------------------------
       INDEX - rebuild the grid from storage
       --------------------------------------------------------------------- */

    /**
     * The template's own "no products yet" block, creating it if the compiler
     * never emitted one.
     *
     * {% for %}...{% else %} is resolved at BUILD time: the else branch only
     * reaches the HTML if the list was already empty when the site was
     * generated. Deleting the last product in the browser is a runtime event
     * the compiler never saw, so the block has to be supplied here.
     */
    function emptyBlock(grid) {
        var existing = document.querySelector('[data-empty]');
        if (existing) return existing;

        var node = document.createElement('p');
        node.setAttribute('data-empty', '');
        node.innerHTML = 'No products yet. <a href="add_product.html">Add the first one!</a>';
        node.style.display = 'none';
        grid.appendChild(node);
        return node;
    }

    function renderIndex(s) {
        var grid = document.querySelector('[data-list="products"]');
        if (!grid) return;

        var empty = emptyBlock(grid);

        if (!s.items.length) {
            var cards = grid.querySelectorAll('[data-item]');
            for (var c = 0; c < cards.length; c++) cards[c].parentNode.removeChild(cards[c]);
            empty.style.display = '';
            return;
        }
        empty.style.display = 'none';

        var html = '';
        for (var i = 0; i < s.items.length; i++) {
            var p = s.items[i];
            html +=
                '<div class="card" data-item>' +
                  '<img src="' + esc(p.image) + '" alt="' + esc(p.name) + '"/>' +
                  '<div class="card-body">' +
                    '<h3 class="card-title">' + esc(p.name) + '</h3>' +
                    '<p class="price">$' + money(p.price) + '</p>' +
                    '<a href="' + hrefFor(s, 'product_detail', p.id) + '" class="btn"> View Details </a>' +
                    '<a href="' + hrefFor(s, 'edit_product', p.id) + '" class="btn" data-link="edit">Edit</a>' +
                  '</div>' +
                '</div>';
        }

        // Replace only the cards; whatever else the template put in the grid
        // (the empty-state paragraph) is preserved above.
        var old = grid.querySelectorAll('[data-item]');
        for (var k = 0; k < old.length; k++) old[k].parentNode.removeChild(old[k]);
        grid.insertAdjacentHTML('afterbegin', html);
    }

    /* ---------------------------------------------------------------------
       DETAIL - fill the fields from storage, and capture details on first view
       --------------------------------------------------------------------- */

    function renderDetail(s) {
        var id = currentId();
        if (id === null) return;

        var item = find(s, id);

        // First time this page is opened the stored record has no details,
        // because the index cards do not carry them. Take them from the page
        // the compiler rendered, then remember them.
        if (item && !item.details) {
            var rendered = document.querySelector('[data-field="details"]');
            if (rendered && text(rendered)) {
                item.details = text(rendered);
                write(s);
            }
        }
        if (!item) return;

        setField('name', item.name);
        setField('details', item.details);
        setField('price', money(item.price));

        var img = document.querySelector('img[data-field="image"]');
        if (img) {
            img.setAttribute('src', item.image);
            img.setAttribute('alt', item.name);
        }
        var heading = document.querySelector('h2[data-field="name"]');
        if (heading) heading.textContent = item.name;

        retarget(s, item.id);
    }

    function setField(name, value) {
        var nodes = document.querySelectorAll('[data-field="' + name + '"]');
        for (var i = 0; i < nodes.length; i++) {
            if (nodes[i].tagName === 'IMG') continue;
            nodes[i].textContent = value;
        }
    }

    /** Point Edit / Delete / Cancel links at this item, shell page included. */
    function retarget(s, id) {
        var links = document.querySelectorAll('a[href]');
        for (var i = 0; i < links.length; i++) {
            var href = links[i].getAttribute('href');
            if (/^(https?:|#|mailto:)/.test(href)) continue;

            if (href.indexOf('edit_product') === 0) {
                links[i].setAttribute('href', hrefFor(s, 'edit_product', id));
            } else if (href.indexOf('delete_product') === 0) {
                links[i].setAttribute('href', hrefFor(s, 'delete_product', id));
            } else if (href.indexOf('product_detail') === 0) {
                links[i].setAttribute('href', hrefFor(s, 'product_detail', id));
            }
        }
    }

    /* ---------------------------------------------------------------------
       ADD
       --------------------------------------------------------------------- */

    function wireAdd(s) {
        var form = document.querySelector('[data-form="add"]');
        if (!form) return;

        form.addEventListener('submit', function (event) {
            event.preventDefault();

            var name = value(form, 'name');
            var price = value(form, 'price');
            if (!name || !price) return;   // the inputs are marked required

            s.items.push({
                id: s.nextId,
                name: name,
                price: Number(price) || 0,
                details: value(form, 'details'),
                image: value(form, 'image') ||
                       'https://via.placeholder.com/400x300?text=No+Image'
            });
            s.nextId = s.nextId + 1;
            write(s);

            go(form, 'index.html');
        });
    }

    /* ---------------------------------------------------------------------
       EDIT - prefill from storage, save back
       --------------------------------------------------------------------- */

    function wireEdit(s) {
        var form = document.querySelector('[data-form="edit"]');
        if (!form) return;

        var id = currentId();
        var item = id === null ? null : find(s, id);

        if (item) {
            setValue(form, 'name', item.name);
            setValue(form, 'price', item.price);
            setValue(form, 'details', item.details);
            setValue(form, 'image', item.image);
            retarget(s, item.id);
        }

        form.addEventListener('submit', function (event) {
            event.preventDefault();
            if (!item) return;

            item.name = value(form, 'name');
            item.price = Number(value(form, 'price')) || 0;
            item.details = value(form, 'details');
            item.image = value(form, 'image') || item.image;
            write(s);

            go(form, hrefFor(s, 'product_detail', item.id));
        });
    }

    /* ---------------------------------------------------------------------
       DELETE - the confirmation page's form removes the item
       --------------------------------------------------------------------- */

    function wireDelete(s) {
        var form = document.querySelector('form');
        if (!form) return;

        var id = currentId();
        if (id !== null) retarget(s, id);

        form.addEventListener('submit', function (event) {
            event.preventDefault();
            if (id === null) return;

            for (var i = 0; i < s.items.length; i++) {
                if (s.items[i].id === id) {
                    s.items.splice(i, 1);
                    break;
                }
            }
            write(s);
            go(form, 'index.html');
        });
    }

    /* ---------------------------------------------------------------------
       FORM HELPERS
       --------------------------------------------------------------------- */

    function value(form, name) {
        var field = form.querySelector('[name="' + name + '"]');
        return field ? String(field.value).trim() : '';
    }

    function setValue(form, name, v) {
        var field = form.querySelector('[name="' + name + '"]');
        if (field) field.value = v === null || v === undefined ? '' : v;
    }

    /**
     * Where to go after a form succeeds.
     * The templates already say it: data-redirect="index" / "detail".
     * The fallback is used when that attribute is absent.
     */
    function go(form, fallback) {
        var target = form.getAttribute('data-redirect');
        window.location.href = target === 'index' ? 'index.html' : fallback;
    }

    /* ---------------------------------------------------------------------
       START
       --------------------------------------------------------------------- */

    function start() {
        var kind = pageKind();
        if (!kind) return;

        var s = state();

        switch (kind) {
            case 'index':  renderIndex(s); break;
            case 'detail': renderDetail(s); break;
            case 'add':    wireAdd(s); break;
            case 'edit':   wireEdit(s); break;
            case 'delete': wireDelete(s); break;
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', start);
    } else {
        start();
    }

    /* Reset from the console:
         localStorage.removeItem('flask-compiler-site'); location.reload();     */
})();
