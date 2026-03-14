require kalico_${PV}.inc
inherit update-rc.d

SUMMARY = "Kalico 3D Printer Firmware"
DESCRIPTION = "Klipper, but Limitless"

SRC_URI += " \
    file://config.toolhead \
    file://klipper-firmware-toolhead-init-d \
"

DEPENDS += "gcc-arm-none-eabi-native"
RDEPENDS:${PN} += "mcu-flasher"
RPROVIDES:${PN} += "klipper-firmware-toolhead"

EXTRA_OEMAKE += "KCONFIG_CONFIG=../config.toolhead"

INITSCRIPT_NAME = "klipper-firmware-toolhead"
INITSCRIPT_PARAMS = "defaults 94 4"

do_install() {
    install -d ${D}/lib/firmware
    cp -r ${S}/out/klipper.bin ${D}/lib/firmware/klipper-toolhead.bin
    echo ${SRCREV} > ${D}/lib/firmware/klipper-toolhead.bin.ver

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    cp ${WORKDIR}/klipper-firmware-toolhead-init-d ${D}${sysconfdir}/init.d/klipper-firmware-toolhead
    chmod 0755 ${D}${sysconfdir}/init.d/klipper-firmware-toolhead
}

FILES:${PN} = " \
    /lib/firmware/klipper-toolhead.bin \
    /lib/firmware/klipper-toolhead.bin.ver \
    ${sysconfdir}/init.d/klipper-firmware-toolhead \
"
