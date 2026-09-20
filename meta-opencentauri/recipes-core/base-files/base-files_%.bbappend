FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " \
    file://fstab \
    file://motd-opencentauri \
"

do_install:append() {
    install -d ${D}/user-resource
    install -d ${D}/board-resource
    install -d ${D}/boot-resource
    install -d ${D}/data
    install -m 0644 ${UNPACKDIR}/motd-opencentauri ${D}${sysconfdir}/motd
}

do_install:append:elegoo-centauri-carbon2() {
    install -d ${D}/opt/usr
}
