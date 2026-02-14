FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:elegoo-centauri-carbon1 = "file://elegoo-centauri-carbon1.dts;subdir=git/arch/${ARCH}/dts \
	file://elegoo_centauri_carbon1_defconfig;subdir=git/configs"
