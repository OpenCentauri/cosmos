SUMMARY = "Boot-time hardware probe"
DESCRIPTION = "Detects WiFi chip, drivers, and network interface at boot and emits /run/hardware-scan.env for downstream services."
HOMEPAGE = "https://github.com/Brofalo/pono-print-cosmos"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "\
    file://hardware-scan-init-d \
    file://hardware-scan \
"

S = "${WORKDIR}"

RDEPENDS:${PN} = "kmod iproute2 busybox"

inherit update-rc.d

INITSCRIPT_NAME = "hardware-scan"
# G06 (v4 audit): include stop entry for symmetry. Without it update-rc.d
# places no K-links in rc0/rc6, so hardware-scan never receives a stop
# signal at shutdown. The probe's /run/hardware-scan.env is in tmpfs and
# vanishes on power-off, but any downstream service that reads it during
# shutdown phase would otherwise see stale state.
INITSCRIPT_PARAMS = "start 20 S . stop 80 0 6 ."

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/hardware-scan-init-d ${D}${sysconfdir}/init.d/hardware-scan

    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/hardware-scan ${D}${sbindir}/hardware-scan
}

FILES:${PN} = "\
    ${sysconfdir}/init.d/hardware-scan \
    ${sbindir}/hardware-scan \
"
