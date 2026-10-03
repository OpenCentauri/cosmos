DESCRIPTION = "COSMOS Disk Space Checker"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "file://check-disk.sh"

inherit allarch

RDEPENDS:${PN} = " \
    curl \
    screen-actions \
"

do_install[vardeps] += "DISTRO_VERSION"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${UNPACKDIR}/check-disk.sh ${D}${bindir}/check-disk
}

FILES:${PN} = "${bindir}/check-disk"
