FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " \
    file://klipper \
"

do_install:append (){
    install -p -m 644 ${WORKDIR}/klipper ${D}${sysconfdir}/logrotate.d/klipper
}
