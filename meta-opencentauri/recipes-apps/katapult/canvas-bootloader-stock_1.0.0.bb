require katapult_${PV}.inc

SUMMARY = "Stock Canvas Bootloader Deployer"
DESCRIPTION = "Builds the Katapult deployer binary for reverting the canvas bootloader to stock."

SRC_URI += " \
    file://config.canvas \
    file://canvas-cc1-bootloader-stock.bin \
"

DEPENDS += "gcc-arm-none-eabi-native"

EXTRA_OEMAKE += " \
    KCONFIG_CONFIG=../config.canvas \
    DEPLOYER_PAYLOAD=../canvas-cc1-bootloader-stock.bin \
"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/out/deployer.bin ${D}/lib/firmware/katapult-deployer-stock-canvas.bin
}

FILES:${PN} = " \
    /lib/firmware/katapult-deployer-stock-canvas.bin \
"