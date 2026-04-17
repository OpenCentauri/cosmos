FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
PACKAGE_ARCH = "${MACHINE_ARCH}"
SRC_URI += " \
    file://fragment.cfg \
    file://hwrevision \
    file://swupdate_public.pem \
    file://swupdate.cfg \
    file://0001-swupdate-add-awboot-handler.patch \
"
DEPENDS += "allwinner-ota-burnboot"
RDEPENDS:${PN} += "allwinner-ota-burnboot"

SECURITY_LDFLAGS:remove = "-Wl,--gc-sections"
TARGET_LDFLAGS:remove = "-Wl,--gc-sections"
LDFLAGS:remove = "-Wl,--gc-sections"

do_install:append() {
    install -d ${D}${sysconfdir}
    install -m 0644 ${WORKDIR}/swupdate.cfg ${D}${sysconfdir}/swupdate.cfg
    install -m 0644 ${WORKDIR}/hwrevision ${D}${sysconfdir}/hwrevision
    install -m 0644 ${WORKDIR}/swupdate_public.pem ${D}${sysconfdir}/swupdate_public.pem

    rm -rf ${D}${sysconfdir}/init.d
}

pkg_postinst:${PN} () {
    exit 0
}

INITSCRIPT_NAME = ""
INITSCRIPT_PARAMS = ""
