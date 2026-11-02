SUMMARY = "Mount the boot-resource partition selected by u-boot"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "\
    file://99-boot-resource.rules \
"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "udev"

do_install() {
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${S}/99-boot-resource.rules ${D}${sysconfdir}/udev/rules.d/99-boot-resource.rules
}

FILES:${PN} = "\
    ${sysconfdir}/udev/rules.d/99-boot-resource.rules \
"
