SUMMARY = "Allwinner OTA Boot Burner"
HOMEPAGE = "https://github.com/jamesturton/allwinner-ota-burnboot"
LICENSE = "GPL-2.0-or-later"
LIC_FILES_CHKSUM = "file://BootHead.h;beginline=1;endline=19;md5=e9660d58c5efc3682f53a4fd8fc43608"

SRC_URI = "git://github.com/jamesturton/allwinner-ota-burnboot.git;protocol=https;branch=main"
SRCREV = "abb7920b78ce6616cef9e4bf8fce32d8edcdd45f"
PR = "r1"

do_install() {
    install -d ${D}${libdir}
    install -m 0755 ${S}/libota-burnboot.so ${D}${libdir}/

    install -d ${D}${includedir}
    install -m 0644 ${S}/OTA_BurnBoot.h ${D}${includedir}/
}

FILES:${PN} += "${libdir}/libota-burnboot.so"
SOLIBS = ".so"
FILES_SOLIBSDEV = ""
