DESCRIPTION = "USB storage auto-mount via udev"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "\
    file://99-usb-automount.rules \
    file://usb-mount.sh \
"

do_install() {
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/99-usb-automount.rules \
        ${D}${sysconfdir}/udev/rules.d/99-usb-automount.rules

    install -d ${D}/usr/local/bin
    install -m 0755 ${WORKDIR}/usb-mount.sh ${D}/usr/local/bin/usb-mount.sh
}

FILES:${PN} = "\
    ${sysconfdir}/udev/rules.d/99-usb-automount.rules \
    /usr/local/bin/usb-mount.sh \
"
