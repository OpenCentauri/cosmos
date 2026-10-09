SUMMARY = "First-boot Centauri Carbon 2 hardware detection"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

COMPATIBLE_MACHINE = "^centauri-carbon-2$"
PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = "file://centauri-carbon-2-detect.init file://probe-canvas.py"
S = "${UNPACKDIR}"

inherit update-rc.d

# All MCU firmware flash services start at 94; Klipper starts at 95.
INITSCRIPT_NAME = "centauri-carbon-2-detect"
INITSCRIPT_PARAMS = "start 93 2 3 4 5 ."

RDEPENDS:${PN} = "config-manager python3-core python3-pyserial"

do_install() {
    install -d ${D}${sysconfdir}/init.d ${D}${libexecdir}
    install -m 0755 ${S}/centauri-carbon-2-detect.init ${D}${sysconfdir}/init.d/centauri-carbon-2-detect
    install -m 0755 ${S}/probe-canvas.py ${D}${libexecdir}/probe-canvas
    sed -i 's,@LIBEXECDIR@,${libexecdir},g' ${D}${sysconfdir}/init.d/centauri-carbon-2-detect
}

FILES:${PN} = "${sysconfdir}/init.d/centauri-carbon-2-detect ${libexecdir}/probe-canvas"
