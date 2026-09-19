FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " \
    file://fstab \
    file://motd-opencentauri \
"
SRC_URI:append:elegoo-centauri-carbon2 = " \
    file://fstab.elegoo-centauri-carbon2 \
"

do_install:append() {
    install -d ${D}/user-resource
    install -d ${D}/board-resource
    install -d ${D}/boot-resource
    install -d ${D}/opt/usr
    install -d ${D}/data
    install -m 0644 ${UNPACKDIR}/motd-opencentauri ${D}${sysconfdir}/motd
}

# CC2 gets its own mount table (UDISK at /opt/usr, /user-resource bind, no board-resource)
do_install:append:elegoo-centauri-carbon2() {
    install -m 0644 ${UNPACKDIR}/fstab.elegoo-centauri-carbon2 ${D}${sysconfdir}/fstab
}
