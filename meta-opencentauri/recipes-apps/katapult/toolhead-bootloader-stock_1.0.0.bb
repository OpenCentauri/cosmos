require katapult_${PV}.inc

SUMMARY = "Stock Toolhead Bootloader Deployer"
DESCRIPTION = "Builds the Katapult deployer binary for reverting the toolhead bootloader to stock."

SRC_URI += " \
    file://config.toolhead \
    file://toolhead-bootloader-stock.bin \
"

DEPENDS += "gcc-arm-none-eabi-native"

EXTRA_OEMAKE += " \
    KCONFIG_CONFIG=../config.toolhead \
    DEPLOYER_PAYLOAD=../toolhead-bootloader-stock.bin \
"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/out/deployer.bin ${D}/lib/firmware/katapult-deployer-stock-toolhead.bin
}

FILES:${PN} = " \
    /lib/firmware/katapult-deployer-stock-toolhead.bin \
"
