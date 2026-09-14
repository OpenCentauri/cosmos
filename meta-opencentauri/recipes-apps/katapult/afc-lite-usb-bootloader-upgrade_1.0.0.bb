require katapult_${PV}.inc

SUMMARY = "Katapult AFC-Lite USB Bootloader Deployer"
DESCRIPTION = "Builds the Katapult deployer binary for the BoxTurtle AFC-Lite board over USB."

SRC_URI += " \
    file://config.afc-lite-usb \
"

DEPENDS += "gcc-arm-none-eabi-native"

EXTRA_OEMAKE += "KCONFIG_CONFIG=../config.afc-lite-usb"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/out/deployer.bin ${D}/lib/firmware/katapult-deployer-afc-lite-usb.bin
}

FILES:${PN} = " \
    /lib/firmware/katapult-deployer-afc-lite-usb.bin \
"
