SUMMARY = "AIC8800 SDIO Wi-Fi driver"
DESCRIPTION = "Out-of-tree SDIO kernel driver for the AIC8800 chipset."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://drivers/aic8800/aic8800_fdrv/rwnx_main.c;beginline=1;endline=11;md5=0ed0561fb91deaf3177b5ce8941f51df"

inherit module

# NOTE: this is a vendor out-of-tree fork. Last built and booted against
# linux-mainline 6.12.80. If you bump the kernel, verify SDIO API compatibility.
SRCREV = "c3aa64baf0d4c72d28c56a60f4096f4f16d46dd3"
SRC_URI = "git://github.com/goecho/aic8800_linux_drvier.git;protocol=https;branch=main"

EXTRA_OEMAKE += "-C ${STAGING_KERNEL_DIR} M=${S}/drivers/aic8800 \
    CONFIG_PREALLOC_RX_SKB=n CONFIG_PREALLOC_TXQ=n"

MODULES_MODULE_SYMVERS_LOCATION = "drivers/aic8800"

RDEPENDS:${PN} += "aic8800-firmware"
