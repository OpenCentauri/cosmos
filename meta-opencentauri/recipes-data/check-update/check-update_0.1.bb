DESCRIPTION = "COSMOS update availability checker"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "file://check-update.py"

inherit allarch

RDEPENDS:${PN} = " \
    config-manager \
    curl \
    python3-core \
    python3-json \
    screen-actions \
"

do_install[vardeps] += "DISTRO_VERSION"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/check-update.py ${D}${bindir}/check-update
}

FILES:${PN} = "${bindir}/check-update"
