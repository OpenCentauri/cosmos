require katapult_${PV}.inc

SUMMARY = "Katapult Canvas Bootloader Deployer"
DESCRIPTION = "Builds the Katapult deployer binary for upgrading the canvas bootloader."

SRC_URI += " \
    file://config.canvas \
"

DEPENDS += "gcc-arm-none-eabi-native"

EXTRA_OEMAKE += "KCONFIG_CONFIG=../config.canvas"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/out/deployer.bin ${D}/lib/firmware/katapult-deployer-canvas.bin
}

FILES:${PN} = " \
    /lib/firmware/katapult-deployer-canvas.bin \
"
