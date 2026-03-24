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

RDEPENDS:${PN} = " \
    curl \
    swu-flasher \
    flashtool \
    toolhead-bootloader-stock \
    bed-bootloader-stock \
"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/factory-reset ${D}${bindir}/
    install -m 0755 ${WORKDIR}/update-cosmos ${D}${bindir}/
    install -m 0755 ${WORKDIR}/switch-to-stock ${D}${bindir}/
    install -m 0755 ${WORKDIR}/switch-to-oc-patched ${D}${bindir}/
    install -m 0755 ${WORKDIR}/swu-decrypt.py ${D}${bindir}/
}

FILES_${PN} += " \
    ${bindir}/factory-reset \
    ${bindir}/update-cosmos \
    ${bindir}/switch-to-stock \
    ${bindir}/switch-to-oc-patched \
    ${bindir}/swu-decrypt.py \
"
