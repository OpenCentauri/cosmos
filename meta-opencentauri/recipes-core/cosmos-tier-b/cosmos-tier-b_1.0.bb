SUMMARY = "Cosmos writeback-tuning sysctls for eMMC longevity"
DESCRIPTION = "Installs /etc/sysctl.d/99-cosmos-tier-b.conf and an init \
script that applies it at boot. Tunes vm.dirty_ratio, \
vm.dirty_background_ratio, and expire/writeback intervals to reduce \
eMMC wear amplification on long prints. Companion to the \
zram-emmc-swap_1.0.bb recipe."
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://99-cosmos-tier-b.conf \
    file://cosmos-tier-b-sysctl \
"

inherit update-rc.d

INITSCRIPT_NAME = "cosmos-tier-b-sysctl"
# Run early (priority 03), before zram (priority 20) and zram-emmc-swap
# (priority 21). The sysctl values are independent of the swap setup;
# applying them early means klipper / moonraker start under the tuned
# writeback regime instead of the default 20% / 10%.
#
# "defaults" creates start S03 links at runlevels 2 3 4 5 and stop K03
# links at runlevels 0 1 6 (the script handles stop as a no-op; sysctl
# values persist in the running kernel and only need re-apply on fresh
# boot). Earlier "start 03 S ." was a bug: it only registered at runlevel
# S (single-user), so multi-user boot never applied the tunings.
INITSCRIPT_PARAMS = "defaults 03"

RDEPENDS:${PN} = ""

do_install() {
    install -d ${D}${sysconfdir}/sysctl.d
    install -m 0644 ${WORKDIR}/99-cosmos-tier-b.conf ${D}${sysconfdir}/sysctl.d/

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/cosmos-tier-b-sysctl ${D}${sysconfdir}/init.d/
}

FILES:${PN} = " \
    ${sysconfdir}/sysctl.d/99-cosmos-tier-b.conf \
    ${sysconfdir}/init.d/cosmos-tier-b-sysctl \
"
