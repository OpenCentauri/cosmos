SUMMARY = "AIC8800 Wi-Fi driver"
DESCRIPTION = "Out-of-tree kernel driver for the AIC8800 chipset."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://drivers/aic8800/aic8800_fdrv/rwnx_main.c;beginline=1;endline=11;md5=0ed0561fb91deaf3177b5ce8941f51df"

inherit module

SRCREV = "05710dff05dabce66ab3ee80f40484892c512b3c"
SRC_URI = "git://github.com/shenmintao/aic8800d80.git;protocol=https;branch=main \
    file://0001-sdio-guards.patch"

EXTRA_OEMAKE += "-C ${STAGING_KERNEL_DIR} M=${S}/drivers/aic8800 \
    CONFIG_SDIO_SUPPORT=${@bb.utils.contains('MACHINE', 'elegoo-centauri-carbon2', 'y', 'n', d)} \
    CONFIG_USB_SUPPORT=${@bb.utils.contains('MACHINE', 'elegoo-centauri-carbon1', 'y', 'n', d)} \
    CONFIG_AIC_LOADFW_SUPPORT=${@bb.utils.contains('MACHINE', 'elegoo-centauri-carbon1', 'm', 'n', d)} \
    CONFIG_PREALLOC_RX_SKB=n CONFIG_PREALLOC_TXQ=n"

MODULES_MODULE_SYMVERS_LOCATION = "drivers/aic8800"

RDEPENDS:${PN} += "aic8800-firmware"
