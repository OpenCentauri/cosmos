# Workaround for OpenCentauri/cosmos#184 (wlan0 missing or unassociated at boot).
SUMMARY = "Boot-time wlan0 recovery helper"
DESCRIPTION = "Recovers wlan0 across driver-bind, association, and DHCP failure modes after boot."
HOMEPAGE = "https://github.com/Brofalo/pono-print-cosmos"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "\
    file://wifi-rescue-init-d \
    file://wifi-rescue \
    file://wifi-rescue-default \
"

S = "${WORKDIR}"

RDEPENDS:${PN} = "kmod iproute2 init-ifupdown wpa-supplicant"

inherit update-rc.d

INITSCRIPT_NAME = "wifi-rescue"
INITSCRIPT_PARAMS = "defaults 80 20"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/wifi-rescue-init-d ${D}${sysconfdir}/init.d/wifi-rescue

    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/wifi-rescue ${D}${sbindir}/wifi-rescue

    install -d ${D}${sysconfdir}/default
    install -m 0644 ${WORKDIR}/wifi-rescue-default ${D}${sysconfdir}/default/wifi-rescue
}

FILES:${PN} = "\
    ${sysconfdir}/init.d/wifi-rescue \
    ${sysconfdir}/default/wifi-rescue \
    ${sbindir}/wifi-rescue \
"
