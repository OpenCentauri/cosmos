SUMMARY = "Fluidd - Web Interface for Klipper"
DESCRIPTION = "Fluidd is a free and open-source Klipper web interface for \
    managing your 3D printer. Features responsive UI supporting desktop, \
    tablets and mobile with customizable layouts."
HOMEPAGE = "https://github.com/fluidd-core/fluidd"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://index.html;md5=641200d3fbd91b44d56ad8c7a74f25e5"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "https://github.com/fluidd-core/fluidd/releases/download/v${PV}/fluidd.zip;subdir=fluidd \
    file://fluidd.cfg \
"
SRC_URI[sha256sum] = "3f9dbe9ac5db51d1025a5737b207d83558c07cb0d7af34d3c5832c868fa72454"

S = "${WORKDIR}/fluidd"

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
    install -d ${D}/var/www/fluidd
    cp -r ${S}/* ${D}/var/www/fluidd/

    # Install default fluidd config
    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly
    install -m 0644 ${WORKDIR}/fluidd.cfg ${D}${sysconfdir}/klipper/config/klipper-readonly/
}

FILES:${PN} = " \
    /var/www/fluidd \
    ${sysconfdir}/klipper/config/klipper-readonly/fluidd.cfg \
"

CONFFILES:${PN} = " \
    ${sysconfdir}/klipper/config/klipper-readonly/fluidd.cfg \
"
