DESCRIPTION = "Trigger Allwinner EFEX/FEL mode on next reboot"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "file://efex"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "devmem2"

do_install() {
    install -d ${D}${sbindir}
    install -m 0755 ${S}/efex ${D}${sbindir}/efex
}
