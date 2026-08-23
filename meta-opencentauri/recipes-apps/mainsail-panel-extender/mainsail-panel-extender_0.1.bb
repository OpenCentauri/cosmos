SUMMARY = "Mainsail Panel Extender"
DESCRIPTION = "Framework that lets addon packages add custom panels to the \
    Mainsail dashboard. Installs a loader script served by Moonraker at \
    /addons/loader.js. Addons install their panels to \
    /var/www/addons/panels/<name>/panel.js and are discovered at runtime \
    via /addons/manifest.json."
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = " \
    file://loader.js \
    file://stickynote-panel-example/panel.js \
"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}/var/www/addons/panels
    install -m 0644 ${WORKDIR}/loader.js ${D}/var/www/addons/loader.js

    # Sticky Note example panel, shipped with the framework
    install -d ${D}/var/www/addons/panels/stickynote
    install -m 0644 ${WORKDIR}/stickynote-panel-example/panel.js \
        ${D}/var/www/addons/panels/stickynote/panel.js
}

FILES:${PN} = " \
    /var/www/addons \
"
