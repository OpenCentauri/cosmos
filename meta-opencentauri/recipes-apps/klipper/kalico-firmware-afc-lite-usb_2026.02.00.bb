require kalico_${PV}.inc

SUMMARY = "Kalico AFC-Lite USB Firmware"
DESCRIPTION = "Builds the Kalico firmware binary for the BoxTurtle AFC-Lite board over USB."

SRC_URI += " \
    file://config.afc-lite-usb \
"

DEPENDS += "gcc-arm-none-eabi-native"
RDEPENDS:${PN} = " \
    afc-lite-usb-bootloader-upgrade \
"

EXTRA_OEMAKE += "KCONFIG_CONFIG=../config.afc-lite-usb"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/out/klipper.bin ${D}/lib/firmware/kalico-afc-lite-usb.bin
}

FILES:${PN} = " \
    /lib/firmware/kalico-afc-lite-usb.bin \
"
