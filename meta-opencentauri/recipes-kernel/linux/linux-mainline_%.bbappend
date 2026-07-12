DEPENDS += "u-boot-tools-native"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI:append = " \
	file://elegoo-centauri-carbon1.dts;subdir=${BP}/arch/${ARCH}/boot/dts/allwinner \
	file://elegoo-centauri-carbon2.dts;subdir=${BP}/arch/${ARCH}/boot/dts/allwinner \
	file://sunxi-r528-msgbox.c;subdir=${BP}/drivers/mailbox \
	file://sunxi_r528_remoteproc.c;subdir=${BP}/drivers/remoteproc \
	file://panel-sitronix-st77922.c;subdir=${BP}/drivers/gpu/drm/panel \
	file://0001-Add-elegoo-centauri-carbon.dts.patch \
	file://0001-Add-support-for-r528-msgbox-and-remoteproc.patch \
	file://0002-drm-add-RB-channel-swap-support-for-panels-with-swap.patch \
	file://0004-drm-panel-add-sitronix-st77922.patch \
	file://0005-drm-sun4i-tcon-top-register-clocks-in-probe.patch \
	file://fragment.cfg \
	file://0001-dt-bindings-pwm-Add-binding-for-Allwinner-D1-T113-S3.patch \
	file://0002-pwm-Add-Allwinner-s-D1-T113-S3-R329-SoCs-PWM-support.patch \
	file://0003-riscv-dts-allwinner-d1-Add-pwm-node.patch \
	file://0001-Make-CONFIG_FB-select-CONFIG_FB_BACKLIGHT.patch \
"

SRC_URI:append:elegoo-centauri-carbon2 = " \
	file://cc2-mipi.cfg \
"
