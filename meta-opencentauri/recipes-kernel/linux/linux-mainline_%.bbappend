DEPENDS += "u-boot-tools-native"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI:append = " \
	file://elegoo-centauri-carbon1.dts;subdir=${BP}/arch/${ARCH}/boot/dts/allwinner \
	file://elegoo-centauri-carbon2.dts;subdir=${BP}/arch/${ARCH}/boot/dts/allwinner \
	file://sunxi-r528-msgbox.c;subdir=${BP}/drivers/mailbox \
	file://sunxi_r528_remoteproc.c;subdir=${BP}/drivers/remoteproc \
	file://st77922.c;subdir=${BP}/drivers/input/touchscreen \
	file://0001-Add-elegoo-centauri-carbon.dts.patch \
	file://0001-Add-support-for-r528-msgbox-and-remoteproc.patch \
	file://0006-serial-8250_dw-fix-em485-on-Allwinner.patch \
	file://fragment.cfg \
	file://0001-dt-bindings-pwm-Add-binding-for-Allwinner-D1-T113-S3.patch \
	file://0002-pwm-Add-Allwinner-s-D1-T113-S3-R329-SoCs-PWM-support.patch \
	file://0003-riscv-dts-allwinner-d1-Add-pwm-node.patch \
	file://0001-Make-CONFIG_FB-select-CONFIG_FB_BACKLIGHT.patch \
	file://0001-Add-support-for-st77922-touchscreen-driver.patch \
"

SRC_URI:append:centauri-carbon-2 = " file://cc2-mmc-debug.cfg"
