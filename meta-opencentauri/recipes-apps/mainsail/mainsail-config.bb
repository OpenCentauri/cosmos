SUMMARY = "Mainsail config macros for Klipper"
DESCRIPTION = "Provides mainsail.cfg macros (PAUSE, RESUME, CANCEL_PRINT) \
    without the web frontend for headless operation."

LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "file://mainsail.cfg"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly
    install -m 0644 ${WORKDIR}/mainsail.cfg ${D}${sysconfdir}/klipper/config/klipper-readonly/
}

FILES:${PN} = "${sysconfdir}/klipper/config/klipper-readonly/mainsail.cfg"

RDEPENDS:${PN} = "kalico"