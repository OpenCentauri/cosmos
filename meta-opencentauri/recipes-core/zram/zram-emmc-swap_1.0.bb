SUMMARY = "eMMC swapfile fallback for low-memory systems"
DESCRIPTION = "Creates and activates a swapfile on eMMC as a lower-priority fallback behind zram compressed swap."
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "file://zram-emmc-swap"

inherit update-rc.d

INITSCRIPT_NAME = "zram-emmc-swap"
INITSCRIPT_PARAMS = "defaults 21 79"

RDEPENDS:${PN} = "zram util-linux-swaponoff util-linux-mkswap"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/zram-emmc-swap ${D}${sysconfdir}/init.d/
}

FILES:${PN} = "${sysconfdir}/init.d/zram-emmc-swap"
