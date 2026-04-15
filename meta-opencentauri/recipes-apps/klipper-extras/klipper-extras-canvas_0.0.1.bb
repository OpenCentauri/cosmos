SUMMARY = "Klipper plugin for Elegoo elegoo_canvas_cc Lite"
HOMEPAGE = "https://git.devminer.xyz/DevMiner/e400-lite-klipper"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://elegoo_canvas_cc.py;beginline=8;endline=8;md5=eee880805f7e255841bdbe04c87fe6de"

SRC_URI = " \
    file://elegoo_canvas_cc.cfg \
    file://elegoo_canvas_cc.py \
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
    install -m 0644 ${S}/elegoo_canvas_cc.py ${D}${datadir}/klipper/klippy/extras/

    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly
    install -m 0644 ${S}/elegoo_canvas_cc.cfg ${D}${sysconfdir}/klipper/config/klipper-readonly
}

FILES:${PN} = " \
    ${datadir}/klipper/klippy/extras/elegoo_canvas_cc.py \
    ${sysconfdir}/klipper/config/klipper-readonly/elegoo_canvas_cc.cfg \
"
