DESCRIPTION = "zram compressed swap + eMMC swapfile for low-memory systems"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "file://zram-swap"

inherit update-rc.d

INITSCRIPT_NAME = "zram-swap"
INITSCRIPT_PARAMS = "defaults 20 80"

RDEPENDS:${PN} = "util-linux-swaponoff util-linux-mkswap"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/zram-swap ${D}${sysconfdir}/init.d/zram-swap
}

FILES:${PN} = "${sysconfdir}/init.d/zram-swap"
