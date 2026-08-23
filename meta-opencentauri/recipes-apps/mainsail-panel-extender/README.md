# Mainsail Panel Extender

Framework that lets addon recipes add custom panels to the Mainsail
dashboard without forking or rebuilding Mainsail. Addon panels are
first-class citizens: they reuse Mainsail's own Panel component (native
toolbar with icon + title, collapse button with the native animation,
theming), render inside the dashboard columns, and appear in
**Settings > Dashboard** where users can reorder them, move them
between columns and hide them. Layout and collapse state are stored by
Mainsail itself in the moonraker database, per viewport, like every
built-in panel.

## Architecture

Three pieces, no Mainsail fork:

1. **Moonraker** (patch `0002-Serve-web-UI-addon-panels.patch`) serves
   the addon files. `addons_path` in the `[server]` section enables it
   and accepts one root per line (the image default is `/var/www/addons`
   plus `/user-resource/webui-addons`, so runtime-installed addons on
   writable storage can add panels too — roots are checked per request,
   no moonraker restart needed):
   - `/addons/*` — static files from that directory
   - `/addons/manifest.json` — generated per request: every directory
     under `<addons_path>/panels/` containing a `panel.js` is listed.
     Installing a file is all it takes to register an addon.
2. **Mainsail** (recipe `mainsail_*.bb`) gets one line injected into
   `index.html` at build time: `<script src="/addons/loader.js">`. The
   service worker precache revision in `sw.js` is bumped so browsers
   with a cached page pick up the change.
3. **`loader.js`** (this recipe) fetches the manifest, loads each
   `panel.js`, and hooks the running Mainsail app:
   - extends the `gui/getAllPossiblePanels` Vuex getter with the addon
     panel names, so Mainsail's own layout machinery (dashboard
     columns, settings lists, saved layouts) treats them as built-ins;
   - registers a Vue component `<id>-panel` per addon that renders
     Mainsail's Panel component with the addon body in its slot;
   - toolbar buttons and popout menus reuse Vuetify's `v-btn`/`v-menu`.

   If Mainsail internals change in a future version and the hooks fail,
   the loader logs to the console and falls back to plain cards below
   the dashboard, so addons stay usable.

## Adding a panel — quick start

A panel is one JavaScript file. Minimal example:

```js
// panel.js
window.CosmosPanels.register({
    id: 'hello',
    title: 'Hello',
    mount(el, ctx) {
        el.textContent = 'Hello from an addon panel'
    },
})
```

Install it to `/var/www/addons/panels/<name>/panel.js` from a recipe:

```bitbake
SUMMARY = "Hello dashboard panel"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI = "file://panel.js"
RDEPENDS:${PN} = "mainsail-panel-extender"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}/var/www/addons/panels/hello
    install -m 0644 ${WORKDIR}/panel.js ${D}/var/www/addons/panels/hello/panel.js
}

FILES:${PN} = "/var/www/addons/panels/hello"
```

Add the recipe to the image and rebuild. No manifest to edit, no other
registration step.

A complete working example ships with the framework: the **Sticky Note**
panel (`files/stickynote-panel-example/panel.js`, installed to
`/var/www/addons/panels/stickynote/`). It exercises the whole API —
inline SVG icon, popout menu with a "Clear note" action, square layout,
and persistent state in the moonraker database. It is hidden by default
(`defaultVisible: false`) — enable it in Settings > Dashboard.

## Registration API

```js
window.CosmosPanels.register({
    id: 'my-panel',          // REQUIRED. Unique, [a-z0-9-] only, no '_'
                             // (underscore is reserved by mainsail).
    title: 'My Panel',       // Toolbar title + name in Settings > Dashboard.
    icon: 'M12,2L2,7...',    // Optional. Either an MDI SVG path string
                             // (24x24 viewBox, mainsail convention — copy
                             // from @mdi/js) or a complete inline
                             // '<svg>...</svg>' string with any viewBox;
                             // its fill is recolored to follow the theme.
                             // Default: a puzzle icon.
    defaultVisible: false,   // Optional (default true). false = panel
                             // starts hidden; users enable it in
                             // Settings > Dashboard. Once they touch it
                             // there, their choice is saved and wins.
    mount(el, ctx) {},       // REQUIRED. Build your UI inside el (the
                             // card body, a v-card__text div).
    unmount(el) {},          // Optional cleanup (timers, sockets).
    buttons: [               // Optional toolbar buttons, rendered left of
                             // the collapse button like native panels.
        { icon: 'M...', onClick(ctx) {} },   // plain action button
        { icon: 'M...', menu(el, ctx) {},    // popout menu (native
          closeOnContentClick: true },       // v-menu). Build content in
                             // el — fresh on every open. Set
                             // closeOnContentClick: false for menus with
                             // controls that should stay open.
    ],
})
```

### Panel lifecycle — what your code must handle

- **`mount` runs every time the panel enters the DOM** — first load and
  every navigation back to the dashboard — always with a fresh, empty
  `el`. Rebuild your DOM and reload state in `mount`; don't cache
  elements across mounts.
- **Collapse is free.** The minimize button, animation and persistence
  are Mainsail's. The panel DOM stays alive while collapsed (state such
  as a half-typed input survives).
- **Users control placement.** Your panel starts at the bottom of
  dashboard column 1; users move/hide it in Settings > Dashboard.
  Don't assume a position or that the panel is visible.
- **Persistent state goes in the moonraker database**, not
  localStorage, so it survives reboots and is shared across browsers:
  `GET/POST /server/database/item` with your own namespace (see the
  sticky-note example pattern: debounced saves on input).
- **Styling:** reuse Mainsail's look by using Vuetify utility classes
  (`v-btn`, `pa-2`, `d-flex`, ...) and theme variables
  (`var(--v-primary-base)`); colors inherit from the card. Don't set
  `display` on `el` itself — use an inner wrapper for flex/grid.

### ctx — Moonraker helpers

Same-origin, no auth needed on the device:

- `ctx.apiGet(path)` — GET, resolves to parsed JSON, rejects on non-2xx
- `ctx.apiPost(path, body?)` — POST with JSON body
- `ctx.gcode(script)` — run a G-code script

For push updates, open your own WebSocket to `/websocket` (moonraker
JSON-RPC) or poll.

### Extra assets

Files next to `panel.js` are served under `/addons/panels/<name>/` —
css, images, extra scripts. Load them from `mount` with relative-to-app
URLs, e.g. `fetch('/addons/panels/my-panel/data.json')`.

## Runtime-installed and in-development panels

`/user-resource/webui-addons` is writable and scanned per request: drop
`panels/<name>/panel.js` under it (from an addon installer script, or
`scp` while developing) and reload the browser — no rebuild, no
moonraker restart. Addon files are served with `Cache-Control:
no-cache`. Installing a panel there is safe on images without the
extender: nothing references the directory.

## Limitations

- Mainsail only. Fluidd has no addon hook (the loader is injected into
  Mainsail's index.html).
- In the Settings > Dashboard list, addon panels show Mainsail's
  generic info icon — the icon mapping there is compiled in. The panel
  itself shows your icon.
- The loader touches Mainsail internals (Vuex getter, Panel/VMenu
  components). A future Mainsail bump may need the loader revisited;
  until then panels degrade to plain non-collapsible cards and an error
  is logged to the browser console.
