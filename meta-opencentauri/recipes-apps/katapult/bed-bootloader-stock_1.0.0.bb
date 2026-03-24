require katapult_${PV}.inc

SUMMARY = "Stock Bed Bootloader Deployer"
DESCRIPTION = "Builds the Katapult deployer binary for upgrading the bed bootloader to stock."

SRC_URI += " \
    file://config.bed \
    file://bed-bootloader-stock.bin \
"

DEPENDS += "gcc-arm-none-eabi-native"

EXTRA_OEMAKE += " \
    KCONFIG_CONFIG=../config.bed \
    DEPLOYER_PAYLOAD=../bed-bootloader-stock.bin \
"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/out/deployer.bin ${D}/lib/firmware/katapult-deployer-stock-bed.bin
}

FILES:${PN} = " \
    /lib/firmware/katapult-deployer-stock-bed.bin \
"
