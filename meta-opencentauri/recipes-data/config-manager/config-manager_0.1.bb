DESCRIPTION = "Config manager script and default configuration"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://config_manager.py \
    file://default.conf \
"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/config_manager.py ${D}${bindir}/config-manager

    install -d ${D}${sysconfdir}/klipper/config
    install -m 0644 ${WORKDIR}/default.conf ${D}${sysconfdir}/klipper/config/cosmos.conf

    install -d ${D}${datadir}/config-manager
    install -m 0644 ${WORKDIR}/default.conf ${D}${datadir}/config-manager/default.conf
}

FILES:${PN} = " \
    ${bindir}/config-manager \
    ${sysconfdir}/klipper/config/cosmos.conf \
    ${datadir}/config-manager/default.conf \
"

RDEPENDS:${PN} = "python3"