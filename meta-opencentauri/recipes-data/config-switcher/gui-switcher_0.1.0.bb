DESCRIPTION = "GUI Switcher"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://gui-switcher.init \
"

inherit update-rc.d

INITSCRIPT_NAME = "gui-switcher"
INITSCRIPT_PARAMS = "defaults 96 4"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/gui-switcher.init ${D}${sysconfdir}/init.d/gui-switcher
}

FILES_${PN} += " \
    ${sysconfdir}/init.d/gui-switcher \
"
