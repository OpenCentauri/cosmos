DESCRIPTION = "Scripts for sending UI prompt actions to the printer screen"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://uiprompt \
    file://uiclear \
"

RDEPENDS:${PN} = "curl"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/uiprompt ${D}${bindir}/uiprompt
    install -m 0755 ${WORKDIR}/uiclear ${D}${bindir}/uiclear
}

FILES:${PN} = " \
    ${bindir}/uiprompt \
    ${bindir}/uiclear \
"
