require katapult.inc

SUMMARY = "Katapult Bed Bootloader Deployer"
DESCRIPTION = "Builds the Katapult deployer binary for upgrading the bed bootloader."

SRC_URI += " \
    file://config.bed \
"

DEPENDS += "gcc-arm-none-eabi-native"

EXTRA_OEMAKE += "KCONFIG_CONFIG=../config.bed"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/out/deployer.bin ${D}/lib/firmware/katapult-deployer-bed.bin
}

FILES:${PN} = " \
    /lib/firmware/katapult-deployer-bed.bin \
"
