DESCRIPTION = "OpenCentauri Boot Logos"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://bootlogo.bmp \
    file://gradient.bmp \
    file://hackerinside.bmp \
    file://lines2.bmp \
    file://lines3.bmp \
    file://magic.bin \
    file://official.bmp \
    file://swap.sh \
"

S = "${UNPACKDIR}"

do_install() {
    install -d ${D}/boot-resource
    install -m 0644 ${S}/bootlogo.bmp ${D}/boot-resource/bootlogo.bmp
    install -m 0644 ${S}/gradient.bmp ${D}/boot-resource/gradient.bmp
    install -m 0644 ${S}/hackerinside.bmp ${D}/boot-resource/hackerinside.bmp
    install -m 0644 ${S}/lines2.bmp ${D}/boot-resource/lines2.bmp
    install -m 0644 ${S}/lines3.bmp ${D}/boot-resource/lines3.bmp
    install -m 0644 ${S}/magic.bin ${D}/boot-resource/magic.bin
    install -m 0644 ${S}/official.bmp ${D}/boot-resource/official.bmp
    install -m 0755 ${S}/swap.sh ${D}/boot-resource/swap.sh
}

FILES:${PN} = " \
    /boot-resource/bootlogo.bmp \
    /boot-resource/gradient.bmp \
    /boot-resource/hackerinside.bmp \
    /boot-resource/lines2.bmp \
    /boot-resource/lines3.bmp \
    /boot-resource/magic.bin \
    /boot-resource/official.bmp \
    /boot-resource/swap.sh \
"