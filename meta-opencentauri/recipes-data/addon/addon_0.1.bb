SUMMARY = "COSMOS addon manager"
DESCRIPTION = "Install, list, and remove Klipper addons and manage their Moonraker update configuration"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "file://addon.py"

S = "${UNPACKDIR}"

inherit allarch

RDEPENDS:${PN} = " \
    git \
    klipper \
    moonraker \
    python3-core \
    python3-datetime \
"

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/addon.py ${D}${bindir}/addon
    ln -s addon ${D}${bindir}/addons
}

FILES:${PN} = "${bindir}/addon ${bindir}/addons"
