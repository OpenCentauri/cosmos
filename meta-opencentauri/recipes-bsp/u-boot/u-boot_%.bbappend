FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI:append = " \
	file://elegoo-centauri-carbon1.dts;subdir=u-boot-${PV}/dts/upstream/src/arm \
	file://elegoo-centauri-carbon2.dts;subdir=u-boot-${PV}/dts/upstream/src/arm \
	file://elegoo_centauri_carbon1_defconfig;subdir=u-boot-${PV}/configs \
	file://elegoo_centauri_carbon2_defconfig;subdir=u-boot-${PV}/configs \
	file://usb-cc1.config;subdir=u-boot-${PV}/configs \
	file://usb-cc2.config;subdir=u-boot-${PV}/configs \
	file://usb-msd.config;subdir=u-boot-${PV}/configs \
	file://elegoo-centauri-carbon1.env;subdir=u-boot-${PV}/board/sunxi \
	file://elegoo-centauri-carbon2.env;subdir=u-boot-${PV}/board/sunxi \
	file://0001-Fix-ac-remmaping-on-R528-S3.patch \
	file://0001-sunxi-r528-add-display-support-with-RB-channel-swap.patch \
	file://0001-Reduce-size-of-framebuffer.patch \
"
