# Offline SWU flash surface. When the network is unavailable, the
# operator's recovery path is a local SWU. Cosmos already has the
# network-based variants in update-scripts (switch-to-stock and
# switch-to-oc-patched, both of which curl an SWU from a public URL);
# this recipe adds the offline peer: flash an SWU already on disk or
# on an inserted USB thumb drive, no network or DNS required.
#
# Two source paths:
#   1. Cached at /data/recovery/{oc,stock}-restore.swu. Operator
#      provisions once via USB or scp; later restores do not need
#      the USB stick.
#   2. Inserted USB thumb drive at /tmp/usb/*/<name>.swu. Reuses the
#      existing usb-automount mount point. Does not touch the
#      emergency.swu auto-flash path that usb-automount owns;
#      operator-named files only, operator-invoked (no auto-flash on
#      hot-plug).

SUMMARY = "Offline SWU flash from local disk or USB thumb drive"
DESCRIPTION = "Operator-invoked recovery flash from /data/recovery/ or \
inserted USB-mounted SWUs. Companion to switch-to-stock / \
switch-to-oc-patched: same flash primitives, no network required."
HOMEPAGE = "https://github.com/Brofalo/cosmos-staging"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "\
    file://cosmos-disk-recovery-init-d \
    file://cosmos-disk-recovery \
    file://cosmos-disk-recovery-default \
"

S = "${WORKDIR}"

RDEPENDS:${PN} = "swu-flasher swupdate update-scripts"

inherit update-rc.d

INITSCRIPT_NAME = "cosmos-disk-recovery"
# After local-fs mounts settle (so /data is visible) but before user
# services need ready state. Stop early on shutdown.
INITSCRIPT_PARAMS = "defaults 88 12"

do_install() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/cosmos-disk-recovery-init-d ${D}${sysconfdir}/init.d/cosmos-disk-recovery

    install -d ${D}${sbindir}
    install -m 0755 ${WORKDIR}/cosmos-disk-recovery ${D}${sbindir}/cosmos-disk-recovery

    install -d ${D}${sysconfdir}/default
    install -m 0644 ${WORKDIR}/cosmos-disk-recovery-default ${D}${sysconfdir}/default/cosmos-disk-recovery
}

FILES:${PN} = "\
    ${sysconfdir}/init.d/cosmos-disk-recovery \
    ${sysconfdir}/default/cosmos-disk-recovery \
    ${sbindir}/cosmos-disk-recovery \
"
