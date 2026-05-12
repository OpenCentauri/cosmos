SUMMARY = "Fluidd - Web Interface for Klipper"
DESCRIPTION = "Fluidd is a free and open-source Klipper web interface for \
    managing your 3D printer. Features responsive UI supporting desktop, \
    tablets and mobile with customizable layouts."
HOMEPAGE = "https://github.com/fluidd-core/fluidd"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://index.html;md5=e465c9484b3a1201b4cff1978e78b863"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "https://github.com/fluidd-core/fluidd/releases/download/v${PV}/fluidd.zip;downloadfilename=fluidd-${PV}.zip;subdir=fluidd \
    file://fluidd.cfg \
"
SRC_URI[sha256sum] = "b9f003a82ea9061a77c5b2f47c5cdd15c58c8f5f5532960e811be2b01cf0a00b"

S = "${WORKDIR}/fluidd"

RDEPENDS:${PN} = " \
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
