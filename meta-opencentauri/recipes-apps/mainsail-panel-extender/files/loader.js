/*
 * Mainsail Panel Extender - COSMOS web UI addon panel loader.
 *
 * Injected into the Mainsail index.html. Discovers addon panels via
 * /addons/manifest.json (served by Moonraker, see the addons_path
 * option), loads each panel script and integrates registered panels
 * into the Mainsail dashboard as native panels: they reuse Mainsail's
 * own Panel component (toolbar, collapse button + animation, theming),
 * render inside the dashboard columns and appear in Settings >
 * Dashboard where they can be reordered, moved between columns and
 * hidden. Layout and collapse state are stored by Mainsail itself in
 * the moonraker database.
 *
 * Panel scripts register themselves with:
 *
 *   window.CosmosPanels.register({
 *       id: 'my-panel',            // required, unique, [a-z0-9-] only
 *       title: 'My Panel',         // toolbar + settings title
 *       icon: 'M12,2L2,7...',      // optional MDI SVG path (24x24)
 *       mount(el, ctx) {},         // build your UI in el. Called every
 *                                  // time the panel (re)enters the DOM
 *                                  // (route changes), with a fresh el.
 *       unmount(el) {},            // optional cleanup
 *       buttons: [                 // optional toolbar buttons
 *           { icon: 'M...', onClick(ctx) {} },     // plain action
 *           { icon: 'M...', menu(el, ctx) {} },    // popout menu; build
 *                                  // its content in el (fresh on every
 *                                  // open). Pass closeOnContentClick:
 *                                  // false to keep it open on click.
 *       ],
 *   })
 *
 * ctx provides Moonraker API helpers:
 *   ctx.apiGet(path)            -> parsed JSON
 *   ctx.apiPost(path, body?)    -> parsed JSON
 *   ctx.gcode(script)           -> run a G-code script
 *
 * Integration hooks into Mainsail internals (Vue/Vuex instances and the
 * Panel/VMenu/VBtn/VIcon components). If those change in a future
 * Mainsail version, the loader falls back to plain cards.
 */
;(function () {
    'use strict'

    var panels = new Map() // id -> definition
    var hiddenByDefault = [] // panel names registered with defaultVisible: false
    var native = null // { Vue, store, i18n } once integrated
    var failed = false

    var ctx = {
        apiGet: function (path) {
            return fetch(path).then(function (r) {
                if (!r.ok) throw new Error(path + ' -> ' + r.status)
                return r.json()
            })
        },
        apiPost: function (path, body) {
            return fetch(path, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: body ? JSON.stringify(body) : undefined,
            }).then(function (r) {
                if (!r.ok) throw new Error(path + ' -> ' + r.status)
                return r.json()
            })
        },
        gcode: function (script) {
            return ctx.apiPost(
                '/printer/gcode/script?script=' + encodeURIComponent(script)
            )
        },
    }

    window.CosmosPanels = {
        register: function (def) {
            if (
                !def ||
                typeof def.mount !== 'function' ||
                !/^[a-z0-9][a-z0-9-]*$/.test(def.id || '')
            ) {
                console.error('[panel-extender] invalid panel registration', def)
                return
            }
            if (panels.has(def.id)) {
                console.warn('[panel-extender] duplicate panel id ignored:', def.id)
                return
            }
            panels.set(def.id, def)
            if (def.defaultVisible === false) hiddenByDefault.push(def.id)
            if (native) installPanel(def)
            else if (failed) legacySync()
        },
        ctx: ctx,
    }

    // mdi-puzzle, default icon for panels that don't bring one
    var DEFAULT_ICON =
        'M20.5,11H19V7C19,5.89 18.1,5 17,5H13V3.5A2.5,2.5 0 0,0 10.5,1A2.5,' +
        '2.5 0 0,0 8,3.5V5H4A2,2 0 0,0 2,7V10.8H3.5C5,10.8 6.2,12 6.2,13.5C' +
        '6.2,15 5,16.2 3.5,16.2H2V20A2,2 0 0,0 4,22H7.8V20.5C7.8,19 9,17.8 ' +
        '10.5,17.8C12,17.8 13.2,19 13.2,20.5V22H17A2,2 0 0,0 19,20V16H20.5A' +
        '2.5,2.5 0 0,0 23,13.5A2.5,2.5 0 0,0 20.5,11Z'

    /* ------------------------------------------------------------------ */
    /* Native integration: hook the Vue app and Vuex store                */
    /* ------------------------------------------------------------------ */

    function capitalizedName(id) {
        // 'sticky-note' -> 'StickyNote', matching mainsail's getPanelName()
        return id
            .split('-')
            .map(function (s) {
                return s.charAt(0).toUpperCase() + s.slice(1)
            })
            .join('')
    }

    function integrate(vm) {
        // vm is the App component instance; the base Vue constructor with
        // the global component registry lives on the root instance
        var Vue = vm.$root.constructor
        var store = vm.$store
        var i18n = vm.$i18n
        if (!Vue.component || !store || !i18n) throw new Error('no app hooks')

        // Reactive list of addon panel names. Extending the result of the
        // gui/getAllPossiblePanels getter makes mainsail's getPanels()
        // treat addon panels like built-ins: it auto-appends them to the
        // first dashboard column and the Settings > Dashboard lists, and
        // accepts them in saved layouts.
        var names = Vue.observable
            ? Vue.observable({ list: [] })
            : new Vue({ data: { list: [] } })

        // panel names in any saved layout of a viewport; a panel absent
        // here is one mainsail auto-added (default visible:true) — for
        // defaultVisible:false panels we flip that default until the
        // user enables them in Settings > Dashboard (which saves them)
        function savedNames(viewport) {
            var dash = (store.state.gui || {}).dashboard || {}
            var out = {}
            for (var k in dash) {
                if (k.indexOf(viewport + 'Layout') !== 0) continue
                ;(dash[k] || []).forEach(function (p) {
                    if (p && p.name) out[p.name] = true
                })
            }
            return out
        }

        var getters = store.getters
        store.getters = new Proxy(getters, {
            get: function (target, key) {
                var value = target[key]
                if (key === 'gui/getAllPossiblePanels')
                    return value.concat(names.list)
                if (key === 'gui/getPanels' && hiddenByDefault.length)
                    return function (viewport, column, onlyVisible) {
                        var list = value(viewport, column, false)
                        var saved = savedNames(viewport)
                        list = list.map(function (p) {
                            return hiddenByDefault.indexOf(p.name) >= 0 &&
                                !saved[p.name]
                                ? Object.assign({}, p, { visible: false })
                                : p
                        })
                        return onlyVisible
                            ? list.filter(function (p) { return p.visible })
                            : list
                    }
                return value
            },
        })

        native = { Vue: Vue, store: store, i18n: i18n, names: names }
        panels.forEach(installPanel)
    }

    function installPanel(def) {
        // Refuse ids that collide with mainsail's own panels — the native
        // component would win locally while our name pollutes the layouts
        try {
            var builtin = native.store.getters['gui/getAllPossiblePanels']
            if (builtin.indexOf(def.id) >= 0) {
                console.error(
                    '[panel-extender] panel id collides with a mainsail ' +
                        'panel, not installing:', def.id)
                panels.delete(def.id)
                return
            }
        } catch (e) { /* getter unavailable; proceed */ }

        // Settings > Dashboard shows $t('Panels.<Name>Panel.Headline')
        var msg = { Panels: {} }
        msg.Panels[capitalizedName(def.id) + 'Panel'] = {
            Headline: def.title || def.id,
        }
        native.i18n.availableLocales.forEach(function (locale) {
            native.i18n.mergeLocaleMessage(locale, msg)
        })

        // The dashboard renders layout entries as <component :is="name + '-panel'">,
        // resolved through the global registry.
        native.Vue.component(def.id + '-panel', makeWrapper(def))
        native.names.list.push(def.id)
    }

    /* ------------------------------------------------------------------ */
    /* Mainsail component resolution                                      */
    /* ------------------------------------------------------------------ */

    // Mainsail's Panel component is identified by its unique prop set;
    // Vuetify components (v-menu, v-btn, v-icon) keep their names in
    // options.name. All are found in the component registries up the
    // parent chain (e.g. PageDashboard -> StatusPanel -> Panel).
    var ctorCache = {}

    function scanComponents(comps, depth, match) {
        if (!comps) return null
        var k, c, found
        for (k in comps) {
            if (match(comps[k])) return comps[k]
        }
        if (depth > 0) {
            for (k in comps) {
                c = comps[k]
                found =
                    c &&
                    c.options &&
                    scanComponents(c.options.components, depth - 1, match)
                if (found) return found
            }
        }
        return null
    }

    function findCtor(vm, cacheKey, match) {
        if (ctorCache[cacheKey]) return ctorCache[cacheKey]
        for (var p = vm; p; p = p.$parent) {
            var found = scanComponents(p.$options.components, 1, match)
            if (found) {
                ctorCache[cacheKey] = found
                return found
            }
        }
        return null
    }

    function byName(name) {
        return function (c) {
            return !!(c && c.options && c.options.name === name)
        }
    }

    function isPanelCtor(c) {
        var o = c && c.options
        return !!(o && o.props && o.props.cardClass && o.props.collapsible)
    }

    // def.icon may be an MDI path ('M...') or a complete inline '<svg>'
    // string (e.g. Material Symbols with a non-24x24 viewBox). Inline
    // SVGs are recolored to follow the theme.
    function isSvgIcon(icon) {
        return /^\s*<svg/i.test(icon || '')
    }

    function themedSvg(icon) {
        return icon
            .replace(/fill="[^"]*"/g, 'fill="currentColor"')
            .replace(/(width|height)="[^"]*"/g, '$1="24"')
    }

    /* ------------------------------------------------------------------ */
    /* Panel wrapper component                                            */
    /* ------------------------------------------------------------------ */

    // Renders a toolbar button for a def.buttons entry: a plain action
    // button, or a native v-menu popout whose content the addon builds.
    function makeButton(h, vm, b) {
        var VBtn = findCtor(vm, 'v-btn', byName('v-btn'))
        var VIcon = findCtor(vm, 'v-icon', byName('v-icon'))
        var VMenu = b.menu && findCtor(vm, 'v-menu', byName('v-menu'))
        if (!VBtn || !VIcon) return null

        function btn(data) {
            // icon + tile matches mainsail's panel toolbar buttons
            // (square hover, not the round default)
            data.props = { icon: true, tile: true }
            return h(VBtn, data, [h(VIcon, [b.icon || DEFAULT_ICON])])
        }

        if (b.menu && VMenu) {
            return h(
                VMenu,
                {
                    props: {
                        offsetY: true,
                        closeOnContentClick: b.closeOnContentClick !== false,
                    },
                    scopedSlots: {
                        activator: function (sp) {
                            return btn({ on: sp.on, attrs: sp.attrs })
                        },
                    },
                },
                [
                    h({
                        // fresh content each time the menu opens
                        render: function (hh) {
                            return hh('div', {
                                class:
                                    'v-card v-sheet v-card__text ' +
                                    (vm.$vuetify.theme.dark
                                        ? 'theme--dark'
                                        : 'theme--light'),
                            })
                        },
                        mounted: function () {
                            try {
                                b.menu(this.$el, ctx)
                            } catch (e) {
                                console.error('[panel-extender] menu failed', e)
                            }
                        },
                    }),
                ]
            )
        }
        return btn({
            on: {
                click: function () {
                    if (b.onClick) b.onClick(ctx)
                },
            },
        })
    }

    // Vue component wrapping the addon body in mainsail's own Panel
    // component (native toolbar, collapse button + animation, collapse
    // persistence). Falls back to a plain card if Panel can't be found.
    function makeWrapper(def) {
        return {
            name: def.id + '-panel',
            props: { panelId: { type: String, default: null } },
            mounted: function () {
                try {
                    def.mount(this.$refs.body, ctx)
                } catch (e) {
                    console.error('[panel-extender] mount failed: ' + def.id, e)
                }
            },
            beforeDestroy: function () {
                if (def.unmount)
                    try {
                        def.unmount(this.$refs.body)
                    } catch (e) {
                        /* ignore */
                    }
            },
            render: function (h) {
                var Panel = findCtor(this, 'panel', isPanelCtor)
                var body = h('div', { class: 'v-card__text', ref: 'body' })
                if (!Panel) {
                    // registries may not be reachable yet; retry once the
                    // tree is attached
                    var self = this
                    this.$nextTick(function () {
                        if (findCtor(self, 'panel', isPanelCtor))
                            self.$forceUpdate()
                    })
                    var t = this.$vuetify.theme.dark
                        ? 'theme--dark'
                        : 'theme--light'
                    return h(
                        'div',
                        { class: 'v-card v-sheet panel mb-3 mb-md-6 ' + t },
                        [
                            h(
                                'div',
                                {
                                    class: 'v-card__title subheading',
                                    style: { height: '48px' },
                                },
                                def.title || def.id
                            ),
                            body,
                        ]
                    )
                }
                var children = [body]
                if (def.buttons && def.buttons.length) {
                    var vm = this
                    def.buttons.forEach(function (b) {
                        // straight into Panel's buttons slot — it brings
                        // its own flex wrapper
                        var v = makeButton(h, vm, b)
                        if (v) {
                            ;(v.data = v.data || {}).slot = 'buttons'
                            children.push(v)
                        }
                    })
                }
                var props = {
                    title: def.title || def.id,
                    collapsible: true,
                    cardClass: def.id + '-panel',
                }
                if (isSvgIcon(def.icon)) {
                    // Panel's icon slot allows arbitrary inline SVGs
                    children.push(
                        h('span', {
                            slot: 'icon',
                            class:
                                'v-icon notranslate v-icon--left ' +
                                (this.$vuetify.theme.dark
                                    ? 'theme--dark'
                                    : 'theme--light'),
                            domProps: { innerHTML: themedSvg(def.icon) },
                        })
                    )
                } else {
                    props.icon = def.icon || DEFAULT_ICON
                }
                return h(Panel, { props: props }, children)
            },
        }
    }

    /* ------------------------------------------------------------------ */
    /* Legacy fallback: append plain cards below the dashboard            */
    /* ------------------------------------------------------------------ */

    var legacyCards = new Map()

    function legacyBuildCard(def) {
        var t = document.querySelector('.v-application.theme--light')
            ? 'theme--light'
            : 'theme--dark'
        var card = document.createElement('div')
        card.className = 'v-card v-sheet ' + t + ' panel mb-3'
        card.dataset.cosmosPanel = def.id
        var header = document.createElement('header')
        header.className =
            'panel-toolbar v-sheet v-toolbar v-toolbar--dense v-toolbar--flat ' + t
        header.style.height = '48px'
        header.innerHTML =
            '<div class="v-toolbar__content" style="height:48px">' +
            '<div class="v-toolbar__title d-flex align-center">' +
            '<span class="subheading"></span></div></div>'
        header.querySelector('.subheading').textContent = def.title || def.id
        card.appendChild(header)
        var body = document.createElement('div')
        body.className = 'v-card__text'
        card.appendChild(body)
        return { card: card, body: body, mounted: false }
    }

    function legacySync() {
        if (location.pathname !== '/' || panels.size === 0) return
        var pc = document.getElementById('page-container')
        if (!pc) return
        var host = pc.firstElementChild || pc
        var container = document.getElementById('cosmos-addon-panels')
        if (!container) {
            container = document.createElement('div')
            container.id = 'cosmos-addon-panels'
            container.className = 'row'
            container.innerHTML = '<div class="col col-12"></div>'
        }
        if (container.parentElement !== host) host.appendChild(container)
        var column = container.firstElementChild
        panels.forEach(function (def, id) {
            var entry = legacyCards.get(id)
            if (!entry) {
                entry = legacyBuildCard(def)
                legacyCards.set(id, entry)
            }
            if (entry.card.parentElement !== column) column.appendChild(entry.card)
            if (!entry.mounted) {
                entry.mounted = true
                try {
                    def.mount(entry.body, ctx)
                } catch (e) {
                    console.error('[panel-extender] mount failed: ' + id, e)
                }
            }
        })
    }

    /* ------------------------------------------------------------------ */
    /* Bootstrap                                                          */
    /* ------------------------------------------------------------------ */

    function waitForApp() {
        var app = document.getElementById('app')
        var vm = app && app.__vue__
        if (vm && vm.$store && vm.$i18n) {
            try {
                integrate(vm)
            } catch (e) {
                console.error(
                    '[panel-extender] native integration failed, ' +
                        'falling back to plain cards',
                    e
                )
                failed = true
                new MutationObserver(legacySync).observe(
                    document.documentElement,
                    { childList: true, subtree: true }
                )
                legacySync()
            }
            return
        }
        setTimeout(waitForApp, 200)
    }

    function loadPanels() {
        fetch('/addons/manifest.json')
            .then(function (r) {
                return r.json()
            })
            .then(function (manifest) {
                ;(manifest.panels || []).forEach(function (p) {
                    var s = document.createElement('script')
                    s.src = p.script
                    s.onerror = function () {
                        console.error('[panel-extender] failed to load', p.script)
                    }
                    document.head.appendChild(s)
                })
            })
            .catch(function () {
                /* no addons available */
            })
        waitForApp()
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', loadPanels)
    } else {
        loadPanels()
    }
})()
