DESCRIPTION = "WebUI Switcher"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://webui-switcher.init \
"

inherit update-rc.d

INITSCRIPT_NAME = "webui-switcher"
INITSCRIPT_PARAMS = "defaults 97 4"

RDEPENDS:${PN} = " \
    busybox \
"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/webui-switcher.init ${D}${sysconfdir}/init.d/webui-switcher
}

FILES_${PN} += " \
    ${sysconfdir}/init.d/webui-switcher \
"
