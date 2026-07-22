require kalico_${PV}.inc
inherit update-rc.d

SUMMARY = "Kalico 3D Printer Firmware"
DESCRIPTION = "Klipper, but Limitless"

SRC_URI += " \
    file://config.canvas \
    file://klipper-firmware-canvas-init-d \
"

PR = "r3"

DEPENDS += "gcc-arm-none-eabi-native"
RDEPENDS:${PN} = " \
    flashtool \
    canvas-bootloader-upgrade \
    config-manager \
    mcu-flasher \
"

RPROVIDES:${PN} += "klipper-firmware-canvas"

EXTRA_OEMAKE += "KCONFIG_CONFIG=../config.canvas"

INITSCRIPT_NAME = "klipper-firmware-canvas"
INITSCRIPT_PARAMS = "defaults 94 4"

do_install() {
    install -d ${D}/lib/firmware
    cp -r ${S}/out/klipper.bin ${D}/lib/firmware/klipper-canvas.bin
    echo "${SRCREV}-${PR}" > ${D}/lib/firmware/klipper-canvas.bin.ver

    install -d ${D}${sysconfdir}/init.d
    cp ${WORKDIR}/klipper-firmware-canvas-init-d ${D}${sysconfdir}/init.d/klipper-firmware-canvas
    chmod 0755 ${D}${sysconfdir}/init.d/klipper-firmware-canvas
}

FILES:${PN} = " \
    /lib/firmware/klipper-canvas.bin \
    /lib/firmware/klipper-canvas.bin.ver \
    ${sysconfdir}/init.d/klipper-firmware-canvas \
"
