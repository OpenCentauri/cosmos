FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append:elegoo-centauri-carbon1 = " \
	file://elegoo-centauri-carbon1.dts;subdir=u-boot-${PV}/arch/${ARCH}/dts \
	file://elegoo_centauri_carbon1_defconfig;subdir=u-boot-${PV}/configs \
	file://elegoo-centauri-carbon1.env;subdir=u-boot-${PV}/board/sunxi \
	file://0001-Add-elegoo-centauri-carbon1.dts.patch \
	file://0001-Fix-ac-remmaping-on-R528-S3.patch \
	file://0001-sunxi-r528-add-display-support-with-RB-channel-swap.patch \
	file://0001-Reduce-size-of-framebuffer.patch \
"
