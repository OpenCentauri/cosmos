DEPENDS += "u-boot-tools-native"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI:append:elegoo-centauri-carbon1 = " file://elegoo-centauri-carbon1.dts;subdir=linux-6.6.85/arch/${ARCH}/boot/dts/allwinner \
	file://0001-Add-elegoo-centauri-carbon1.dts.patch"
