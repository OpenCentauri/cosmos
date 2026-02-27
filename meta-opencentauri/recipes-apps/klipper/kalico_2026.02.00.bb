SUMMARY = "Kalico 3D Printer Firmware"
DESCRIPTION = "Klipper, but Limitless"
HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/KalicoCrew/kalico.git;protocol=https;branch=main \
    file://klipper-init-d \
    file://printer.cfg"
SRCREV = "c70d21abd88757a6653f5081a125ea26be13c2be"

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

RPROVIDES:${PN} += "klipper"

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
    cp ${WORKDIR}/printer.cfg ${D}${sysconfdir}/klipper

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    cp ${WORKDIR}/klipper-init-d ${D}${sysconfdir}/init.d/klipper
    chmod 0755 ${D}${sysconfdir}/init.d/klipper
}

FILES:${PN} = " \
    ${datadir}/klipper \
    ${sysconfdir}/init.d/klipper \
    ${sysconfdir}/klipper \
"

CONFFILES:${PN} = "${sysconfdir}/klipper/printer.cfg"
