require bigtreetech-mms_${PV}.inc

SUMMARY = "BIGTREETECH MMS ViViD MCU Firmware"
DESCRIPTION = "Pre-built firmware binary for the ViViD MCU (STM32G0B1xx)"

RDEPENDS:${PN} = "flashtool"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"
INSANE_SKIP:${PN} = "arch"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/firmware/klipper_stm32g0b1xx_8kb_usb.bin ${D}/lib/firmware/mms-vivid.bin
}

FILES:${PN} = "/lib/firmware/mms-vivid.bin"
