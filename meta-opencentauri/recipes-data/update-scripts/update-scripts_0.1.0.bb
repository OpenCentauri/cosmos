DESCRIPTION = "Update Scripts"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = " \
    file://factory-reset \
    file://update-cosmos \
    file://switch-to-stock \
"

RDEPENDS:${PN} = "curl swu-flasher"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/factory-reset ${D}${bindir}/
    install -m 0755 ${WORKDIR}/update-cosmos ${D}${bindir}/
    install -m 0755 ${WORKDIR}/switch-to-stock ${D}${bindir}/
}

FILES_${PN} += " \
    ${bindir}/factory-reset \
    ${bindir}/update-cosmos \
    ${bindir}/switch-to-stock \
"
