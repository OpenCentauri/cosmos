SUMMARY = "Carbon2 bed MCU initialization"
DESCRIPTION = "Power and initialize the Carbon2 bed MCU"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://cc2-bed-mcu-init-d"
S = "${UNPACKDIR}"

RDEPENDS:${PN} = "mcu-flasher"
COMPATIBLE_MACHINE = "elegoo-centauri-carbon2"

inherit update-rc.d

INITSCRIPT_NAME = "cc2-bed-mcu"
INITSCRIPT_PARAMS = "defaults 94 5"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/cc2-bed-mcu-init-d \
        ${D}${sysconfdir}/init.d/cc2-bed-mcu
}

FILES:${PN} = "${sysconfdir}/init.d/cc2-bed-mcu"
