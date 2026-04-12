DEPENDS += "u-boot-tools-native"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI:append:elegoo-centauri-carbon1 = " \
	file://elegoo-centauri-carbon1.dts;subdir=linux-${PV}/arch/${ARCH}/boot/dts/allwinner \
	file://sunxi-r528-msgbox.c;subdir=linux-${PV}/drivers/mailbox \
	file://sunxi_r528_remoteproc.c;subdir=linux-${PV}/drivers/remoteproc \
	file://0001-Add-elegoo-centauri-carbon1.dts.patch \
	file://0001-Add-support-for-r528-msgbox-and-remoteproc.patch \
	file://0002-drm-add-RB-channel-swap-support-for-panels-with-swap.patch \
	file://fragment.cfg \
	file://0001-dt-bindings-pwm-Add-binding-for-Allwinner-D1-T113-S3.patch \
	file://0002-pwm-Add-Allwinner-s-D1-T113-S3-R329-SoCs-PWM-support.patch \
	file://0003-riscv-dts-allwinner-d1-Add-pwm-node.patch \
"
