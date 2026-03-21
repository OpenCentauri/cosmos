require kalico_${PV}.inc

SUMMARY = "Kalico 3D Printer Firmware"
DESCRIPTION = "Klipper, but Limitless"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://klipper-init-d \
    file://printer.cfg \
    file://macros.cfg \
    file://bed.cfg \
    file://mainboard.cfg \
    file://toolhead.cfg \
    file://misc.cfg \
"

inherit python3-dir update-rc.d

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
    kalico-firmware-dsp \
    kalico-firmware-toolhead \
    kalico-firmware-bed \
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

    # Install default kalico config
    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
    install -m 0644 ${WORKDIR}/printer.cfg ${D}${sysconfdir}/klipper/config/

    # Copy non-printer .cfg files to readonly folder
    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly
    install -m 0644 ${WORKDIR}/macros.cfg ${WORKDIR}/bed.cfg ${WORKDIR}/mainboard.cfg \
        ${WORKDIR}/toolhead.cfg ${WORKDIR}/misc.cfg ${D}${sysconfdir}/klipper/config/klipper-readonly

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/klipper-init-d ${D}${sysconfdir}/init.d/klipper
}

FILES:${PN} = " \
    ${datadir}/klipper \
    ${sysconfdir}/init.d/klipper \
    ${sysconfdir}/klipper/config \
"

CONFFILES:${PN} = " \
    ${sysconfdir}/klipper/config/printer.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/macros.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/bed.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/mainboard.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/toolhead.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/misc.cfg \
"
