SUMMARY = "Moonraker - Web API Server for Klipper"
DESCRIPTION = "Moonraker is a Python 3 based web server that exposes APIs \
    with which client applications may use to interact with the 3D printing \
    firmware Klipper."
HOMEPAGE = "https://github.com/Arksine/moonraker"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=db95b6e40dc7d26d8308b6b7375637b6"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = " \
    git://github.com/Arksine/moonraker.git;protocol=https;branch=master \
    file://swu_deploy.py;subdir=${BP}/moonraker/components/update_manager/ \
    file://moonraker-init-d \
    file://moonraker.conf \
    file://moonraker-readonly.conf \
    file://moonraker.asvc \
    file://0001-Serve-static-files.patch \
    file://0001-Reduce-log-rotate-threshold.patch \
    file://0001-Add-support-for-SWUDeploy.patch \
    file://0002-Add-sysvinit-as-machine-provider.patch \
"

SRCREV = "9e676eba6b02661a4dfa3ec6e7ac3f3504498e6d"

PR = "r3"

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
    python3-msgspec \
    python3-uvloop \
    python3-aiofiles \
    curl \
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
    install -d ${D}${datadir}/moonraker
    cp -r ${S}/moonraker ${D}${datadir}/moonraker/

    # Install default moonraker config
    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
    install -m 0644 ${UNPACKDIR}/moonraker.conf ${D}${sysconfdir}/klipper/config/

    # Copy readonly config file to readonly folder
    install -d ${D}${sysconfdir}/klipper/config/moonraker-readonly
    install -m 0644 ${UNPACKDIR}/moonraker-readonly.conf ${D}${sysconfdir}/klipper/config/moonraker-readonly/moonraker.conf

    # Install moonraker services file
    install -m 0644 ${UNPACKDIR}/moonraker.asvc ${D}${sysconfdir}/klipper/

    # Symlink gcodes to /user-resource
    ln -sf /user-resource/gcodes ${D}${sysconfdir}/klipper/gcodes
    # Symlink logs to /board-resource
    ln -sf /board-resource ${D}${sysconfdir}/klipper/logs

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/moonraker-init-d ${D}${sysconfdir}/init.d/moonraker
}

FILES:${PN} = " \
    ${datadir}/moonraker \
    ${sysconfdir}/init.d/moonraker \
    ${sysconfdir}/klipper/moonraker.asvc \
    ${sysconfdir}/klipper/config/moonraker.conf \
    ${sysconfdir}/klipper/config/moonraker-readonly/moonraker.conf \
    ${sysconfdir}/klipper/gcodes \
    ${sysconfdir}/klipper/logs \
"

CONFFILES:${PN} = "${sysconfdir}/klipper/config/moonraker.conf"
