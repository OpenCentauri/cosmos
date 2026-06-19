SUMMARY = "Fluidd - Web Interface for Klipper"
DESCRIPTION = "Fluidd is a free and open-source Klipper web interface for \
    managing your 3D printer. Features responsive UI supporting desktop, \
    tablets and mobile with customizable layouts."
HOMEPAGE = "https://github.com/fluidd-core/fluidd"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://index.html;md5=3b00ecfc948a3467a588bf3a3eb33a00"

inherit python3native

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "https://github.com/fluidd-core/fluidd/releases/download/v${PV}/fluidd.zip;downloadfilename=fluidd-${PV}.zip;subdir=fluidd \
    file://opencentauri-fluidd-theme \
"
SRC_URI[sha256sum] = "f08e9d438fdce472553e1ce46a9be62f5ababb4b0f64f65efbd4561d9379653c"

S = "${WORKDIR}/fluidd"

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
        ${WORKDIR}/opencentauri-fluidd-theme/logo_opencentauri.svg \
        ${WORKDIR}/opencentauri-fluidd-theme/carbon-logo-red.webp \
        ${WORKDIR}/opencentauri-fluidd-theme/opencentauri-logo-small.png \
        ${WORKDIR}/opencentauri-fluidd-theme/opencentauri-theme.css \
        ${D}/var/www/fluidd/
    ${PYTHON} ${WORKDIR}/opencentauri-fluidd-theme/apply-opencentauri-fluidd-theme.py ${D}/var/www/fluidd

}

FILES:${PN} = " \
    /var/www/fluidd \
"
