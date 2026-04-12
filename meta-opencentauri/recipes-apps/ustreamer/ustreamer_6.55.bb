SUMMARY = "µStreamer - Lightweight and fast MJPEG-HTTP streamer"
SECTION = "app"
LICENSE = "GPL-3.0-only"
#" GPL-3.0 license"
LIC_FILES_CHKSUM = " \
	file://LICENSE;md5=d32239bcb673463ab874e80d47fae504 \
"

inherit pkgconfig update-rc.d

PV = "6.55"

S = "${WORKDIR}/git"

INITSCRIPT_NAME = "ustreamer"
INITSCRIPT_PARAMS = "defaults 97 5"

SRC_URI = " \
	git://github.com/pikvm/ustreamer.git;protocol=https;branch=master \
	file://ustreamer-init-d \
	file://ustreamer-wrapper \
"
SRCREV = "88460b72e191035d04355e25106af817cbfe069e"

SRC_URI[sha256sum] = "51a8a974d55d5139a86c055df63b99e90e31e9d6af62f69b2decccdb02a29092"

DEPENDS += " libbsd libevent libjpeg-turbo "

do_install() {
	install -m 0755 -d ${D}${bindir}
	install -m 0755 ${S}/src/ustreamer.bin ${D}${bindir}/ustreamer

	install -d ${D}${sysconfdir}/init.d
	install -m 0755 ${WORKDIR}/ustreamer-init-d ${D}${sysconfdir}/init.d/ustreamer

	install -m 0755 ${WORKDIR}/ustreamer-wrapper ${D}${bindir}/ustreamer-wrapper
}

FILES:${PN} = " \
	${bindir}/ustreamer \
	${bindir}/ustreamer-wrapper \
	${sysconfdir}/init.d/ustreamer \
"
