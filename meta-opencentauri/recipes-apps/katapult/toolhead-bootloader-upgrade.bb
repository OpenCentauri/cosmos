require katapult.inc

SUMMARY = "Katapult Toolhead Bootloader Deployer"
DESCRIPTION = "Builds the Katapult deployer binary for upgrading the toolhead bootloader."

SRC_URI += " \
    file://config.toolhead \
"

DEPENDS += "gcc-arm-none-eabi-native"

EXTRA_OEMAKE += "KCONFIG_CONFIG=../config.toolhead"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/out/deployer.bin ${D}/lib/firmware/katapult-deployer-toolhead.bin
}

FILES:${PN} = " \
    /lib/firmware/katapult-deployer-toolhead.bin \
"
