SUMMARY = "Klipper plugin for Elegoo Canvas Lite"
HOMEPAGE = "https://git.devminer.xyz/DevMiner/e400-lite-klipper"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://canvas.py;beginline=8;endline=8;md5=eee880805f7e255841bdbe04c87fe6de"

SRC_URI = " \
    file://canvas.cfg \
    file://canvas.py \
"

S = "${WORKDIR}"

RDEPENDS:${PN} = "klipper"

do_configure() {
    :
}

do_compile() {
    :
}

do_install() {
    install -d ${D}${datadir}/klipper/klippy/extras
    install -m 0644 ${S}/canvas.py ${D}${datadir}/klipper/klippy/extras/

    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly
    install -m 0644 ${S}/canvas.cfg ${D}${sysconfdir}/klipper/config/klipper-readonly
}

FILES:${PN} = " \
    ${datadir}/klipper/klippy/extras/canvas.py \
    ${sysconfdir}/klipper/config/klipper-readonly/canvas.cfg \
"
