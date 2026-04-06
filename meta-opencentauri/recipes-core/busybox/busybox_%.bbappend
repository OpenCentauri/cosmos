FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " \
    file://httpd.cfg \
    file://ntp.cfg \
    file://cron.cfg \
    file://crontab \
"

INITSCRIPT_PACKAGES += "${PN}"
INITSCRIPT_NAME:${PN} = "busybox-cron"
INITSCRIPT_PARAMS:${PN} = "defaults 90"

do_install:append (){
    install -d ${D}${sysconfdir}/cron
    install -d ${D}${sysconfdir}/cron/crontabs
    install -m 0600 ${WORKDIR}/crontab ${D}${sysconfdir}/cron/crontabs/root
}
