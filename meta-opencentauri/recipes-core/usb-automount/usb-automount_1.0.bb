DESCRIPTION = "USB storage auto-mount via udev"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "\
    file://99-usb-automount.rules \
    file://usb-mount \
"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "udev screen-actions"

do_install() {
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${S}/99-usb-automount.rules \
        ${D}${sysconfdir}/udev/rules.d/99-usb-automount.rules

    install -d ${D}${bindir}
    install -m 0755 ${S}/usb-mount ${D}${bindir}/usb-mount
}

FILES:${PN} = "\
    ${sysconfdir}/udev/rules.d/99-usb-automount.rules \
    ${bindir}/usb-mount \
"
