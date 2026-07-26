SUMMARY = "Fluidd - Web Interface for Klipper"
DESCRIPTION = "Fluidd is a free and open-source Klipper web interface for \
    managing your 3D printer. Features responsive UI supporting desktop, \
    tablets and mobile with customizable layouts."
HOMEPAGE = "https://github.com/fluidd-core/fluidd"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://index.html;md5=02c023fdf3a0f62d1a070d1163c7f4c5"

inherit python3native

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "https://github.com/fluidd-core/fluidd/releases/download/v${PV}/fluidd.zip;downloadfilename=fluidd-${PV}.zip;subdir=fluidd \
    file://opencentauri-fluidd-theme \
"
SRC_URI[sha256sum] = "e42d4e8b14a3a0b20573485c882cc4dcfac33d9fbd946c8803a942be282e2b6e"

S = "${UNPACKDIR}/fluidd"

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
    install -d ${D}/var/www/fluidd
    cp -r ${S}/* ${D}/var/www/fluidd/
    install -m 0644 \
        ${UNPACKDIR}/opencentauri-fluidd-theme/logo_opencentauri.svg \
        ${UNPACKDIR}/opencentauri-fluidd-theme/carbon-logo-red.webp \
        ${UNPACKDIR}/opencentauri-fluidd-theme/opencentauri-logo-small.png \
        ${UNPACKDIR}/opencentauri-fluidd-theme/opencentauri-theme.css \
        ${D}/var/www/fluidd/
    ${PYTHON} ${UNPACKDIR}/opencentauri-fluidd-theme/apply-opencentauri-fluidd-theme.py ${D}/var/www/fluidd

}

FILES:${PN} = " \
    /var/www/fluidd \
"
