DESCRIPTION = "Script to flash SWUpdate images"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://flash \
    file://readonly-config-reset \
"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "swupdate u-boot-fw-utils"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/flash ${D}${bindir}/flash
    install -m 0755 ${S}/readonly-config-reset ${D}${bindir}/readonly-config-reset
}

FILES:${PN} = " \
    ${bindir}/flash \
    ${bindir}/readonly-config-reset \
"
