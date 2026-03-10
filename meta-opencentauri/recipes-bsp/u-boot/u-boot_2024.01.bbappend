FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:elegoo-centauri-carbon1 = "file://elegoo-centauri-carbon1.dts;subdir=git/arch/${ARCH}/dts \
	file://elegoo_centauri_carbon1_defconfig;subdir=git/configs \
	file://elegoo-centauri-carbon1.env;subdir=git/board/sunxi \
	file://0001-Add-elegoo-centauri-carbon1.dts.patch \
	file://0001-Fix-ac-remmaping-on-R528-S3.patch \
	file://0001-sunxi-r528-add-display-support-with-RB-channel-swap.patch"
