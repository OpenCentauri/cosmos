SUMMARY = "Klipper 3D Printer Firmware"
DESCRIPTION = "Klipper is a 3D printer firmware that combines the power of a general purpose computer with one or more micro-controllers."
HOMEPAGE = "https://www.klipper3d.org/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/Klipper3d/klipper.git;protocol=https;branch=master \
    file://klipper-init-d"
SRCREV = "61c0c8d2ef40340781835dd53fb04cc7a454e37a"

S = "${WORKDIR}/git"

inherit python3-dir update-rc.d

DEPENDS = " \
    python3-native \
"

RDEPENDS:${PN} = " \
    python3 \
    python3-cffi \
    python3-greenlet \
    python3-jinja2 \
    python3-markupsafe \
    python3-pyserial \
    python3-numpy \
    python3-can \
    python3-msgspec \
"

INITSCRIPT_NAME = "klipper"
INITSCRIPT_PARAMS = "defaults 95 5"

do_configure() {
    :
}

do_compile() {
    # Cross-compile the C helper library that Klipper normally builds at runtime
    cd ${S}/klippy/chelper
    ${CC} -shared -fPIC -flto -fwhole-program -o c_helper.so \
        -O2 ${CFLAGS} ${LDFLAGS} \
        pyhelper.c \
        serialqueue.c \
        stepcompress.c \
        itersolve.c \
        trapq.c \
        pollreactor.c \
        msgblock.c \
        trdispatch.c \
        kin_cartesian.c \
        kin_corexy.c \
        kin_corexz.c \
        kin_delta.c \
        kin_polar.c \
        kin_rotary_delta.c \
        kin_winch.c \
        kin_extruder.c \
        kin_shaper.c \
        kin_idex.c \
        -lm
}

do_install() {
    # Install klipper python package
    install -d ${D}${datadir}/klipper
    cp -r ${S}/klippy ${D}${datadir}/klipper/

    # Remove any .pyc files to avoid TMPDIR references
    find ${D} -name '*.pyc' -delete
    find ${D} -name '__pycache__' -type d -exec rm -rf {} + 2>/dev/null || true

    # Config directory
    install -d ${D}${sysconfdir}/klipper

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    cp -r ${WORKDIR}/klipper-init-d ${D}${sysconfdir}/init.d/klipper
    chmod 0755 ${D}${sysconfdir}/init.d/klipper
}

FILES:${PN} = " \
    ${datadir}/klipper \
    ${sysconfdir}/init.d/klipper \
    ${sysconfdir}/klipper \
"

CONFFILES:${PN} = "${sysconfdir}/klipper/printer.cfg"
