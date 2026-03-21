SUMMARY = "Mainsail - Web Interface for Klipper"
DESCRIPTION = "Mainsail is the popular web interface for managing and \
    controlling 3D printers with Klipper."
HOMEPAGE = "https://github.com/mainsail-crew/mainsail"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://index.html;md5=cda929aa8b78d319a89b240b5df815f9"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "https://github.com/mainsail-crew/mainsail/releases/download/v${PV}/mainsail.zip;subdir=mainsail \
    file://mainsail.cfg \
"
SRC_URI[sha256sum] = "d010f4df25557d520ccdbb8e42fc381df2288e6a5c72d3838a5a2433c7a31d4e"

S = "${WORKDIR}/mainsail"

RDEPENDS:${PN} = " \
    nginx \
    moonraker \
    webui-switcher \
"

do_configure() {
    :
}

do_compile() {
    :
}

do_install() {
    # Install static web files
    install -d ${D}/var/www/mainsail
    cp -r ${S}/* ${D}/var/www/mainsail/

    # Install default mainsail config
    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly
    install -m 0644 ${WORKDIR}/mainsail.cfg ${D}${sysconfdir}/klipper/config/klipper-readonly/
}

FILES:${PN} = " \
    /var/www/mainsail \
    ${sysconfdir}/klipper/config/klipper-readonly/mainsail.cfg \
"

CONFFILES:${PN} = " \
    ${sysconfdir}/klipper/config/klipper-readonly/mainsail.cfg \
"
