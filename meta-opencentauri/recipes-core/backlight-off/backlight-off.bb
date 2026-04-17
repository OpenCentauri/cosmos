SUMMARY = "Turn off display for headless operation"
DESCRIPTION = "Init script to disable display backlight and blank framebuffer \
    after boot for headless MMU setups without touchscreen interface."

LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://backlight-off.init"

S = "${WORKDIR}"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/backlight-off.init ${D}${sysconfdir}/init.d/backlight-off
}

inherit update-rc.d

INITSCRIPT_NAME = "backlight-off"
INITSCRIPT_PARAMS = "start 99 S . stop 10 0 ."

FILES:${PN} = "${sysconfdir}/init.d/backlight-off"

RDEPENDS:${PN} = ""