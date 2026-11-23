HOMEPAGE = "https://github.com/mainsail-crew/moonraker-timelapse"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"
SUMMARY = "Moonraker timelapse"
DESCRIPTION = "Timelapse Plugin for moonraker"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

PV = "0.0.1+git"

SRCREV = "c7fff11e542b95e0e15b8bb1443cea8159ac0274"
SRC_URI = " \
    git://github.com/mainsail-crew/moonraker-timelapse.git;protocol=https;branch=main \
    file://timelapse.conf \
"

RDEPENDS:${PN} = " \
    moonraker \
    ffmpeg \
"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}${datadir}/moonraker/moonraker/components
    install -m 0644 ${S}/component/timelapse.py ${D}${datadir}/moonraker/moonraker/components/

    install -d ${D}${sysconfdir}/klipper/config/moonraker-readonly
    install -m 0644 ${UNPACKDIR}/timelapse.conf ${D}${sysconfdir}/klipper/config/moonraker-readonly/

    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly
    install -m 0644 ${S}/klipper_macro/timelapse.cfg ${D}${sysconfdir}/klipper/config/klipper-readonly/
}

FILES:${PN} = " \
    ${datadir}/moonraker/moonraker/components/timelapse.py \
    ${sysconfdir}/klipper/config/moonraker-readonly/timelapse.conf \
    ${sysconfdir}/klipper/config/klipper-readonly/timelapse.cfg \
"