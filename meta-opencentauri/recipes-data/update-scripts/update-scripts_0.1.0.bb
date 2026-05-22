DESCRIPTION = "Update Scripts"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://factory-reset \
    file://update-cosmos \
    file://switch-to-stock \
    file://switch-to-oc-patched \
    file://swu-decrypt.py \
    file://restore-mcu-firmware \
    file://flash-artifact.py \
    file://find-local-firmware \
"

RDEPENDS:${PN} = " \
    curl \
    swu-flasher \
    flashtool \
    toolhead-bootloader-stock \
    bed-bootloader-stock \
    config-manager \
"

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/factory-reset ${D}${sbindir}/
    install -m 0755 ${WORKDIR}/update-cosmos ${D}${sbindir}/
    install -m 0755 ${WORKDIR}/switch-to-stock ${D}${sbindir}/
    install -m 0755 ${WORKDIR}/switch-to-oc-patched ${D}${sbindir}/
    install -m 0755 ${WORKDIR}/swu-decrypt.py ${D}${sbindir}/
    install -m 0755 ${WORKDIR}/restore-mcu-firmware ${D}${sbindir}/
    install -m 0755 ${WORKDIR}/flash-artifact.py ${D}${sbindir}/flash-artifact

    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/find-local-firmware ${D}${bindir}/

    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
}

FILES_${PN} += " \
    ${sbindir}/factory-reset \
    ${sbindir}/update-cosmos \
    ${sbindir}/switch-to-stock \
    ${sbindir}/switch-to-oc-patched \
    ${sbindir}/swu-decrypt.py \
    ${sbindir}/restore-mcu-firmware \
    ${sbindir}/flash-artifact \
    ${bindir}/find-local-firmware \
"
