SUMMARY = "MJPG-streamer takes JPGs from Linux-UVC compatible webcams, filesystem or other input plugins and streams them as M-JPEG via HTTP to webbrowsers, VLC and other software. It is the successor of uvc-streamer, a Linux-UVC streaming application with Pan/Tilt"

LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=751419260aa954499f7abaabaa882bbe"

SRCREV = "5554f42c352ecfa7edaec6fc51e507afce605a34"
SRC_URI = "git://github.com/jacksonliam/mjpg-streamer.git;protocol=https;branch=master \
    file://mjpg-streamer-init-d"

DEPENDS = "libgphoto2 v4l-utils"

S = "${WORKDIR}/git/mjpg-streamer-experimental"

inherit cmake update-rc.d

OECMAKE_GENERATOR="Unix Makefiles"
TARGET_CFLAGS += "-fcommon"

INITSCRIPT_NAME = "mjpg-streamer"
INITSCRIPT_PARAMS = "defaults 97 5"

do_install() {
    oe_runmake install DESTDIR=${D}

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    cp ${WORKDIR}/mjpg-streamer-init-d ${D}${sysconfdir}/init.d/mjpg-streamer
    chmod 0755 ${D}${sysconfdir}/init.d/mjpg-streamer
}

FILES_${PN} += "${libdir}/*.so"
FILES_${PN} += "${sysconfdir}/init.d/mjpg-streamer"
