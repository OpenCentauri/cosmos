DESCRIPTION = "Make symbolic links to parition names on mmc"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "\
    file://99-dev-by-id.rules \
"

S = "${UNPACKDIR}"

RDEPENDS:${PN} = "udev"

do_install() {
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${S}/99-dev-by-id.rules \
        ${D}${sysconfdir}/udev/rules.d/99-dev-by-id.rules
}

FILES:${PN} = "\
    ${sysconfdir}/udev/rules.d/99-dev-by-id.rules \
"
