/* Sticky Note — example panel for the Mainsail Panel Extender.
 *
 * Demonstrates the full panel API:
 *  - icon: an inline SVG (Material Symbols "note"), recolored to theme
 *  - mount: rebuilt on every dashboard entry, state reloaded from the
 *    moonraker database (survives reboots, shared across browsers)
 *  - buttons: a native popout menu with a "Clear note" action
 *  - layout: body kept square via aspect-ratio, flex on an inner wrapper
 */
var stickynoteClear = null

window.CosmosPanels.register({
    id: 'stickynote',
    title: 'Sticky Note',
    defaultVisible: false, // opt-in: enable it in Settings > Dashboard
    icon: '<svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="#e3e3e3"><path d="M200-200h360v-200h200v-360H200v560Zm0 80q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v400L600-120H200Zm80-280v-80h200v80H280Zm0-160v-80h400v80H280Zm-80 360v-560 560Z"/></svg>',
    buttons: [
        {
            // mdi-dots-vertical
            icon: 'M12,16A2,2 0 0,1 14,18A2,2 0 0,1 12,20A2,2 0 0,1 10,18A2,2 0 0,1 12,16M12,10A2,2 0 0,1 14,12A2,2 0 0,1 12,14A2,2 0 0,1 10,12A2,2 0 0,1 12,10M12,4A2,2 0 0,1 14,6A2,2 0 0,1 12,8A2,2 0 0,1 10,6A2,2 0 0,1 12,4Z',
            menu: function (el) {
                var btn = document.createElement('button')
                btn.className = 'v-btn v-btn--text v-size--small'
                btn.textContent = 'Clear note'
                btn.onclick = function () { stickynoteClear && stickynoteClear() }
                el.appendChild(btn)
            },
        },
    ],
    mount: function (el, ctx) {
        var DB = '/server/database/item'
        var Q = '?namespace=panel_extender&key=stickynote'

        // keep the note square: body height matches its width. The flex
        // layout lives on an inner wrapper because the panel toggles the
        // body's display for collapse.
        el.style.aspectRatio = '1/1'
        var wrap = document.createElement('div')
        wrap.style.cssText = 'display:flex;flex-direction:column;height:100%'
        el.appendChild(wrap)

        var ta = document.createElement('textarea')
        ta.placeholder = 'Write a note...'
        ta.style.cssText =
            'flex:1 1 auto;min-height:0;width:100%;resize:none;padding:8px;' +
            'color:inherit;font:inherit;background:rgba(128,128,128,.08);' +
            'border:1px solid rgba(128,128,128,.35);border-radius:4px;outline:none'
        var status = document.createElement('div')
        status.className = 'text-right'
        status.style.cssText =
            'font-size:.75rem;opacity:.6;min-height:1.2em;flex:0 0 auto'
        wrap.appendChild(ta)
        wrap.appendChild(status)

        ctx.apiGet(DB + Q)
            .then(function (r) { ta.value = r.result.value || '' })
            .catch(function () { /* no note yet */ })

        stickynoteClear = function () {
            ta.value = ''
            ta.dispatchEvent(new Event('input'))
        }

        var timer = null
        ta.addEventListener('input', function () {
            status.textContent = '...'
            clearTimeout(timer)
            timer = setTimeout(function () {
                ctx.apiPost(DB, {
                    namespace: 'panel_extender',
                    key: 'stickynote',
                    value: ta.value,
                })
                    .then(function () { status.textContent = 'saved' })
                    .catch(function () { status.textContent = 'save failed' })
            }, 800)
        })
    },
})
