SUMMARY = "AIC8800 firmware files"
DESCRIPTION = "Firmware blobs and power-limit tables for the AIC8800D80 SDIO Wi-Fi / Bluetooth combo."
LICENSE = "CLOSED"
SECTION = "base"

SRC_URI = "file://aic8800_fw \
           file://aic8800_config \
           file://aic_powerlimit_8800d80.txt \
           file://fmacfw_8800d80_u02.bin \
           file://fw_patch_8800d80_u02.bin \
           file://fw_patch_table_8800d80_u02.bin \
           file://lmacfw_rf_8800d80_u02.bin \
           file://calibmode_8800d80.bin \
           file://aic_userconfig_8800d80.txt"

S = "${WORKDIR}"

inherit allarch

do_install() {
    install -d ${D}/lib/firmware/rtlbt
    install -m 0644 ${S}/aic8800_fw ${D}/lib/firmware/rtlbt/aic8800_fw
    install -m 0644 ${S}/aic8800_config ${D}/lib/firmware/rtlbt/aic8800_config

    install -d ${D}/lib/firmware/aic8800D80
    install -m 0644 ${S}/aic_powerlimit_8800d80.txt ${D}/lib/firmware/aic8800D80/
    install -m 0644 ${S}/fmacfw_8800d80_u02.bin ${D}/lib/firmware/aic8800D80/
    install -m 0644 ${S}/fw_patch_8800d80_u02.bin ${D}/lib/firmware/aic8800D80/
    install -m 0644 ${S}/fw_patch_table_8800d80_u02.bin ${D}/lib/firmware/aic8800D80/
    install -m 0644 ${S}/lmacfw_rf_8800d80_u02.bin ${D}/lib/firmware/aic8800D80/
    install -m 0644 ${S}/calibmode_8800d80.bin ${D}/lib/firmware/aic8800D80/
    install -m 0644 ${S}/aic_userconfig_8800d80.txt ${D}/lib/firmware/aic8800D80/
}

FILES:${PN} += "/lib/firmware/rtlbt/* /lib/firmware/aic8800D80/*"
