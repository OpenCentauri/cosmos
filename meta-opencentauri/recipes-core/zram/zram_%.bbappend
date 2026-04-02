FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += "file://zram-default"

do_install:append() {
    install -d ${D}${sysconfdir}/default
    install -m 0644 ${WORKDIR}/zram-default ${D}${sysconfdir}/default/zram
}
