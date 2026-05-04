require bigtreetech-mms_${PV}.inc

SUMMARY = "BIGTREETECH MMS Buffer MCU Firmware"
DESCRIPTION = "Pre-built firmware binary for the Buffer MCU (STM32F042x6)"

RDEPENDS:${PN} = "flashtool"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"
INSANE_SKIP:${PN} = "arch"

do_install() {
    install -d ${D}/lib/firmware
    install -m 0644 ${S}/firmware/klipper_stm32f042x6_8kb_usb.bin ${D}/lib/firmware/mms-buffer.bin
}

FILES:${PN} = "/lib/firmware/mms-buffer.bin"
