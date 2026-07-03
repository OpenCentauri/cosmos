SUMMARY = "AIC8800 USB and SDIO WiFi driver"
DESCRIPTION = "Out-of-tree kernel driver for the AIC8800 chipset."
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

inherit module

SRCREV = "bd11969265809a0fc948f1107c8256bbb2c1aa60"
SRC_URI = "git://github.com/radxa-pkg/aic8800.git;protocol=https;branch=main"

AIC_BUS:elegoo-centauri-carbon1 = "USB"
AIC_BUS:elegoo-centauri-carbon2 = "SDIO"
AIC_DRIVER_DIR:elegoo-centauri-carbon1 = "src/USB/driver_fw/drivers/aic8800"
AIC_DRIVER_DIR:elegoo-centauri-carbon2 = "src/SDIO/driver_fw/driver/aic8800"

do_radxa_patches() {
    cd ${S}
    git checkout -- .
    while read -r p; do
        git apply --ignore-whitespace -C1 "debian/patches/$p"
    done < debian/patches/series
}
addtask radxa_patches after do_patch before do_configure

EXTRA_OEMAKE += "-C ${STAGING_KERNEL_DIR} M=${S}/${AIC_DRIVER_DIR} \
    CONFIG_PREALLOC_RX_SKB=n CONFIG_PREALLOC_TXQ=n \
    CONFIG_AIC8800_BTLPM_SUPPORT=n"


MODULES_MODULE_SYMVERS_LOCATION = "${AIC_DRIVER_DIR}"

do_install:append() {
    install -d ${D}/lib/firmware/aic8800_fw/${AIC_BUS}/aic8800D80
    install -m 0644 ${S}/src/${AIC_BUS}/driver_fw/fw/aic8800D80/* \
        ${D}/lib/firmware/aic8800_fw/${AIC_BUS}/aic8800D80/
}

FILES:${PN} += "/lib/firmware/aic8800_fw"
