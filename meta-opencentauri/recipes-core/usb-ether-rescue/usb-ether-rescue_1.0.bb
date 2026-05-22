# Failsafe wired LAN bring-up. When WiFi is dead or absent, plugging in
# any of the USB-Ethernet adapters the kernel already supports (RTL8152,
# RTL8153, SMSC95XX, AX88179, ASIX, DM9601, SR9700, Pegasus, MCS7830;
# see recipes-kernel/linux/linux-mainline/elegoo-centauri-carbon1/
# usb-net-adapters.cfg) gives a no-touch wired LAN. The dongle path
# does not depend on WiFi state or on the bed MCU bootloader, so it
# remains available when other recovery surfaces are unavailable.
#
# Companion to wifi-rescue: wifi-rescue handles the wireless-driver
# unbind/rebind path; usb-ether-rescue handles hot-plug Ethernet.
SUMMARY = "Auto-bring-up USB Ethernet adapters for failsafe LAN recovery"
DESCRIPTION = "Hot-plug aware DHCP bring-up for USB-Ethernet adapters \
plugged into a Centauri Carbon when WiFi is dead or absent."
HOMEPAGE = "https://github.com/Brofalo/cosmos-staging"
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
