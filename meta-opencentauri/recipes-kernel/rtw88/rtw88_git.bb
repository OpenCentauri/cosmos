SUMMARY = "Updated Realtek rtw88 WiFi driver (wireless-next backport)"
DESCRIPTION = "Out-of-tree rtw88 driver from lwfinger/rtw88, a backport of \
the wireless-next tree with many stability fixes beyond kernel 6.6."
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://main.c;endline=1;md5=04cb8411563d8726ae2273d76febc90d"

inherit module

SRCREV = "4e777cb6088f95c16ab3da9458ef78db43010d04"
SRC_URI = "git://github.com/lwfinger/rtw88.git;protocol=https;branch=master \
           file://0001-disable-led-support.patch \
           "

S = "${WORKDIR}/git"

EXTRA_OEMAKE += "-C ${STAGING_KERNEL_DIR} M=${S}"

# Auto-load the 8821CU module (pulls in rtw_usb, rtw_8821c, rtw_core)
KERNEL_MODULE_AUTOLOAD += "rtw_8821cu"

# Disable deep power save (RTL8821CU firmware crashes in LPS deep mode)
KERNEL_MODULE_PROBECONF += "rtw_core"
module_conf_rtw_core = "options rtw_core disable_lps_deep=Y"

RDEPENDS:${PN} += "linux-firmware-rtl8821"

COMPATIBLE_MACHINE = "elegoo-centauri-carbon1"
