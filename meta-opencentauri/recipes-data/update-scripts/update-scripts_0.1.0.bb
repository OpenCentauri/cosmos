DESCRIPTION = "Update Scripts"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://factory-reset \
    file://update-cosmos \
    file://switch-to-stock \
    file://switch-to-oc-patched \
    file://swu-decrypt.py \
"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = " \
    curl \
    swu-flasher \
    flashtool \
    toolhead-bootloader-stock \
    bed-bootloader-stock \
    config-manager \
"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/factory-reset ${D}${bindir}/
    install -m 0755 ${S}/update-cosmos ${D}${bindir}/
    install -m 0755 ${S}/switch-to-stock ${D}${bindir}/
    install -m 0755 ${S}/switch-to-oc-patched ${D}${bindir}/
    install -m 0755 ${S}/swu-decrypt.py ${D}${bindir}/

    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
}

FILES:${PN} += " \
    ${bindir}/factory-reset \
    ${bindir}/update-cosmos \
    ${bindir}/switch-to-stock \
    ${bindir}/switch-to-oc-patched \
    ${bindir}/swu-decrypt.py \
"
