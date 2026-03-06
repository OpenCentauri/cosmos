DESCRIPTION = "OpenCentauri initramfs /init script"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "file://init"

# Must run as PID 1 in the initramfs -- do not split or strip
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"

do_install() {
    install -d ${D}
    install -m 0755 ${WORKDIR}/init ${D}/init
}

FILES:${PN} = "/init"
