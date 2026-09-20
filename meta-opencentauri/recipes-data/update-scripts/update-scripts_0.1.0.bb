DESCRIPTION = "Update Scripts"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI = " \
    file://factory-reset \
    file://update-cosmos \
    file://switch-to-stock \
    file://switch-to-oc-patched \
    file://swu-decrypt.py \
    file://restore-mcu-firmware \
    file://flash-artifact.py \
"

SRC_URI:append:elegoo-centauri-carbon2 = " \
    file://stock-bed.bin \
    file://stock-toolhead.bin \
"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = " \
    curl \
    swu-flasher \
    config-manager \
"

RDEPENDS:${PN}:append:elegoo-centauri-carbon1 = " \
    flashtool \
    toolhead-bootloader-stock \
    bed-bootloader-stock \
    canvas-bootloader-stock \
"

RDEPENDS:${PN}:append:elegoo-centauri-carbon2 = " \
    mcu-flasher \
"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/factory-reset ${D}${bindir}/
    install -m 0755 ${S}/update-cosmos ${D}${bindir}/
    install -m 0755 ${S}/switch-to-stock ${D}${bindir}/
    install -m 0755 ${S}/switch-to-oc-patched ${D}${bindir}/
    install -m 0755 ${S}/swu-decrypt.py ${D}${bindir}/
    install -m 0755 ${S}/restore-mcu-firmware ${D}${bindir}/
    install -m 0755 ${S}/flash-artifact.py ${D}${bindir}/flash-artifact

    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
}

do_install:append:elegoo-centauri-carbon2() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/stock-bed.bin ${D}/lib/firmware/
    install -m 0644 ${S}/stock-toolhead.bin ${D}/lib/firmware/
}

FILES:${PN} += " \
    ${bindir}/factory-reset \
    ${bindir}/update-cosmos \
    ${bindir}/switch-to-stock \
    ${bindir}/switch-to-oc-patched \
    ${bindir}/swu-decrypt.py \
    ${bindir}/restore-mcu-firmware \
    ${bindir}/flash-artifact \
"
FILES:${PN}:append:elegoo-centauri-carbon2 = " \
    /lib/firmware/stock-bed.bin \
    /lib/firmware/stock-toolhead.bin \
"
