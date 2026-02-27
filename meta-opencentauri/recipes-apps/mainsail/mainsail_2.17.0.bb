SUMMARY = "Mainsail - Web Interface for Klipper"
DESCRIPTION = "Mainsail is the popular web interface for managing and \
    controlling 3D printers with Klipper."
HOMEPAGE = "https://github.com/mainsail-crew/mainsail"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://index.html;md5=cda929aa8b78d319a89b240b5df815f9"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "https://github.com/mainsail-crew/mainsail/releases/download/v${PV}/mainsail.zip;subdir=mainsail \
    file://mainsail-nginx"
SRC_URI[sha256sum] = "d010f4df25557d520ccdbb8e42fc381df2288e6a5c72d3838a5a2433c7a31d4e"

S = "${WORKDIR}/mainsail"

RDEPENDS:${PN} = " \
    nginx \
    moonraker \
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

    # Install nginx site config
    install -d ${D}${sysconfdir}/nginx/sites-available
    install -d ${D}${sysconfdir}/nginx/sites-enabled

    cp ${WORKDIR}/mainsail-nginx ${D}${sysconfdir}/nginx/sites-available/mainsail

    # Symlink to sites-enabled
    ln -sf ${sysconfdir}/nginx/sites-available/mainsail \
        ${D}${sysconfdir}/nginx/sites-enabled/mainsail
}

FILES:${PN} = " \
    /var/www/mainsail \
    ${sysconfdir}/nginx/sites-available/mainsail \
    ${sysconfdir}/nginx/sites-enabled/mainsail \
"

CONFFILES:${PN} = "${sysconfdir}/nginx/sites-available/mainsail"
