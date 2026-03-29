FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " \
	file://elegoo-centauri-carbon.dts;subdir=u-boot-${PV}/arch/${ARCH}/dts \
	file://elegoo_centauri_carbon_defconfig;subdir=u-boot-${PV}/configs \
	file://elegoo-centauri-carbon.env;subdir=u-boot-${PV}/board/sunxi \
	file://0001-Add-elegoo-centauri-carbon.dts.patch \
	file://0001-Fix-ac-remmaping-on-R528-S3.patch \
	file://0001-sunxi-r528-add-display-support-with-RB-channel-swap.patch \
	file://0001-Reduce-size-of-framebuffer.patch \
"
