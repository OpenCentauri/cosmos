SUMMARY = "Disable WiFi power management for Centauri Carbon1"
DESCRIPTION = "Prevents WiFi disconnect when display sleeps/wakes"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://disable-wifi-powersave.sh"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${S}/disable-wifi-powersave.sh ${D}${sysconfdir}/init.d/disable-wifi-powersave
    
    # Create symlinks for auto-start
    install -d ${D}${sysconfdir}/rc5.d
    ln -sf ../init.d/disable-wifi-powersave ${D}${sysconfdir}/rc5.d/S99disable-wifi-powersave
}

FILES:${PN} = "${sysconfdir}"

RDEPENDS:${PN} = "wireless-tools"
