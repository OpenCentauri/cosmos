DEPENDS += "u-boot-tools-native"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI:append:elegoo-centauri-carbon1 = " \
	file://elegoo-centauri-carbon1.dts;subdir=linux-6.6.85/arch/${ARCH}/boot/dts/allwinner \
	file://sunxi-r528-msgbox.c;subdir=linux-6.6.85/drivers/mailbox \
	file://sunxi_r528_remoteproc.c;subdir=linux-6.6.85/drivers/remoteproc \
	file://0001-Add-elegoo-centauri-carbon1.dts.patch \
	file://0001-Add-support-for-r528-msgbox-and-remoteproc.patch \
	file://0002-drm-add-RB-channel-swap-support-for-panels-with-swap.patch \
	file://0003-thermal-sun8i-add-sun20i-d1-ths-support.patch \
	file://fragment.cfg \
	file://squashfs-overlayfs.cfg \
	file://kernel-size-reduction.cfg \
"
