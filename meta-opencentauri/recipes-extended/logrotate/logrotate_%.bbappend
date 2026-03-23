FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " \
    file://opencentauri \
"

do_install:append (){
    install -p -m 644 ${WORKDIR}/opencentauri ${D}${sysconfdir}/logrotate.d/opencentauri

    if ${@bb.utils.contains('DISTRO_FEATURES', 'sysvinit', 'true', 'false', d)}; then
        mkdir -p ${D}${sysconfdir}/cron.hourly
        mv ${D}${sysconfdir}/cron.daily/logrotate ${D}${sysconfdir}/cron.hourly/logrotate
    fi
}
