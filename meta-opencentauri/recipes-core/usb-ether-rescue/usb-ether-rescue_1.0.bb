# Class 217 recovery path: substrate-independent wired LAN bring-up.
#
# When AIC8800 WiFi fails to bind (cosmos brick of 2026-05-17) or when a
# Centauri Carbon ends up on a network with a broken AP, plugging in any
# of the USB-Ethernet adapters the kernel already supports (RTL8152,
# RTL8153, SMSC95XX, AX88179, ASIX, DM9601, SR9700, Pegasus, MCS7830 -
# see recipes-kernel/linux/linux-mainline/elegoo-centauri-carbon1/
# usb-net-adapters.cfg) MUST give a no-touch wired LAN. The dongle path
# is the only revert path on this hardware that does not depend on the
# state of WiFi or the bed MCU bootloader.
#
# Companion to wifi-rescue: wifi-rescue handles the AIC8800 unbind/rebind
# bandage; usb-ether-rescue handles "WiFi is dead, plug the cable in".
SUMMARY = "Auto-bring-up USB Ethernet adapters for failsafe LAN recovery"
DESCRIPTION = "Hot-plug aware DHCP bring-up for USB-Ethernet adapters \
plugged into a Centauri Carbon when WiFi is dead or absent."
HOMEPAGE = "https://github.com/Brofalo/pono-print-cosmos"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "\
    file://usb-ether-rescue-init-d \
    file://usb-ether-rescue \
    file://usb-ether-rescue-default \
    file://80-usb-net-autoconfig.rules \
"

S = "${WORKDIR}"

RDEPENDS:${PN} = "kmod iproute2 init-ifupdown busybox-udhcpc"

inherit update-rc.d

INITSCRIPT_NAME = "usb-ether-rescue"
# After wifi-rescue (80), before user services. Network must already be
# available (ifupdown completed). Stop early on shutdown.
INITSCRIPT_PARAMS = "defaults 85 15"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/usb-ether-rescue-init-d ${D}${sysconfdir}/init.d/usb-ether-rescue

    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/usb-ether-rescue ${D}${sbindir}/usb-ether-rescue

    install -d ${D}${sysconfdir}/default
    install -m 0644 ${WORKDIR}/usb-ether-rescue-default ${D}${sysconfdir}/default/usb-ether-rescue

    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/80-usb-net-autoconfig.rules ${D}${sysconfdir}/udev/rules.d/80-usb-net-autoconfig.rules
}

FILES:${PN} = "\
    ${sysconfdir}/init.d/usb-ether-rescue \
    ${sysconfdir}/default/usb-ether-rescue \
    ${sysconfdir}/udev/rules.d/80-usb-net-autoconfig.rules \
    ${sbindir}/usb-ether-rescue \
"
