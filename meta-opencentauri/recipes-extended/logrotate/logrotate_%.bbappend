FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " \
    file://opencentauri \
    file://root.crontab \
"

do_install:append (){
    install -p -m 644 ${WORKDIR}/opencentauri ${D}${sysconfdir}/logrotate.d/opencentauri

    if ${@bb.utils.contains('DISTRO_FEATURES', 'sysvinit', 'true', 'false', d)}; then
        install -d ${D}${sysconfdir}/crontabs
        install -m 0600 ${WORKDIR}/root.crontab ${D}${sysconfdir}/crontabs/root
    fi
}
