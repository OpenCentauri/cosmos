SUMMARY = "Mainsail - Web Interface for Klipper"
DESCRIPTION = "Mainsail is the popular web interface for managing and \
    controlling 3D printers with Klipper."
HOMEPAGE = "https://github.com/mainsail-crew/mainsail"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://index.html;md5=ec818674470b1a5a042b3c94b6a3f05e"

PR = "r1"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = " \
    https://github.com/mainsail-crew/mainsail/releases/download/v${PV}/mainsail.zip;subdir=mainsail \
    file://default.json \
"
SRC_URI[sha256sum] = "c4d9d96f89851c6ae0709c2f20725447f3fe8c57cf193869b0083441bd93378a"

S = "${UNPACKDIR}/mainsail"

RDEPENDS:${PN} = " \
    klipper \
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

    # Seed interface settings on first use or an interface settings reset.
    # Existing Mainsail settings in Moonraker's database remain unchanged.
    install -d ${D}${sysconfdir}/klipper/config/.theme
    install -m 0644 ${UNPACKDIR}/default.json ${D}${sysconfdir}/klipper/config/.theme/default.json
}

FILES:${PN} = " \
    /var/www/mainsail \
    ${sysconfdir}/klipper/config/.theme/default.json \
"
