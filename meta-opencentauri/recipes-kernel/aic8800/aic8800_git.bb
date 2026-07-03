SUMMARY = "AIC8800 WiFi driver"
DESCRIPTION = "Out-of-tree kernel driver for the AIC8800 chipset (USB on CC1, SDIO on CC2)."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://drivers/aic8800/aic8800_fdrv/rwnx_main.c;beginline=1;endline=11;md5=0ed0561fb91deaf3177b5ce8941f51df"

inherit module

SRCREV = "bd10611475568a9c5f397f94de9eaedc776024dd"
SRC_URI = "git://github.com/OpenCentauri/aic8800d80-sdio.git;protocol=https;branch=sdio-cc2"

EXTRA_OEMAKE += "-C ${STAGING_KERNEL_DIR} M=${S}/drivers/aic8800 \
    CONFIG_PREALLOC_RX_SKB=n CONFIG_PREALLOC_TXQ=n \
    CONFIG_SDIO_SUPPORT=${@bb.utils.contains('MACHINE', 'elegoo-centauri-carbon2', 'y', 'n', d)} \
    CONFIG_USB_SUPPORT=${@bb.utils.contains('MACHINE', 'elegoo-centauri-carbon2', 'n', 'y', d)} \
    CONFIG_AIC_LOADFW_SUPPORT=${@bb.utils.contains('MACHINE', 'elegoo-centauri-carbon2', 'n', 'm', d)} \
    CONFIG_PLATFORM_UBUNTU=n"

MODULES_MODULE_SYMVERS_LOCATION = "drivers/aic8800"

do_install:append() {
    install -d ${D}/lib/firmware/aic8800D80
    install -m 0644 ${S}/fw/aic8800D80/* ${D}/lib/firmware/aic8800D80/
    install -d ${D}/lib/firmware/aic8800D80N
    install -m 0644 ${S}/fw/aic8800D80N/* ${D}/lib/firmware/aic8800D80N/
}

FILES:${PN} += "/lib/firmware/aic8800D80 /lib/firmware/aic8800D80N"
