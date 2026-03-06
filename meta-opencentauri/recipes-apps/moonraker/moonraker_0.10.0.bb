SUMMARY = "Moonraker - Web API Server for Klipper"
DESCRIPTION = "Moonraker is a Python 3 based web server that exposes APIs \
    with which client applications may use to interact with the 3D printing \
    firmware Klipper."
HOMEPAGE = "https://github.com/Arksine/moonraker"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=db95b6e40dc7d26d8308b6b7375637b6"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/Arksine/moonraker.git;protocol=https;branch=master \
    file://moonraker-init-d \
    file://moonraker.conf"
SRCREV = "16e530eb663218faa6ccd97ffb0583f1880e2983"

S = "${WORKDIR}/git"

inherit python3-dir update-rc.d

DEPENDS = " \
    python3-native \
"

RDEPENDS:${PN} = " \
    python3 \
    python3-tornado \
    python3-pyserial \
    python3-pyserial-asyncio \
    python3-pillow \
    python3-streaming-form-data \
    python3-distro \
    python3-inotify-simple \
    python3-libnacl \
    python3-paho-mqtt \
    python3-zeroconf \
    python3-preprocess-cancellation \
    python3-jinja2 \
    python3-dbus-fast \
    python3-apprise \
    python3-ldap3 \
    python3-periphery \
    python3-importlib-metadata \
    python3-zipp \
    python3-smart-open \
    kalico \
"

INITSCRIPT_NAME = "moonraker"
INITSCRIPT_PARAMS = "defaults 96 4"

do_configure() {
    :
}

do_compile() {
    :
}

do_install() {
    # Install moonraker python package
    install -d ${D}/root/moonraker
    cp -r ${S}/moonraker ${D}/root/moonraker/

    # Printer data directories
    install -d ${D}/root/printer_data
    install -d ${D}/root/printer_data/config
    install -d ${D}/root/printer_data/gcodes
    install -d ${D}/root/printer_data/comms

    # Install default moonraker config
    cp ${WORKDIR}/moonraker.conf ${D}/root/printer_data/config/moonraker.conf

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    cp ${WORKDIR}/moonraker-init-d ${D}${sysconfdir}/init.d/moonraker
    chmod 0755 ${D}${sysconfdir}/init.d/moonraker
}

FILES:${PN} = " \
    /root/moonraker \
    ${sysconfdir}/init.d/moonraker \
    /root/printer_data/config/moonraker.conf \
    /root/printer_data/gcodes \
    /root/printer_data/comms \
"

CONFFILES:${PN} = "/root/printer_data/config/moonraker.conf"
