DESCRIPTION = "OpenCentauri Boot Logos"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://bootlogo.bmp \
    file://magic.bin \
"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}/boot-resource
    install -m 0644 ${S}/bootlogo.bmp ${D}/boot-resource/bootlogo.bmp
    install -m 0644 ${S}/magic.bin ${D}/boot-resource/magic.bin
}

FILES:${PN} = " \
    /boot-resource/bootlogo.bmp \
    /boot-resource/magic.bin \
"