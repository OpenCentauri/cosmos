require kalico_${PV}.inc

SUMMARY = "Kalico 3D Printer Firmware"
DESCRIPTION = "Klipper, but Limitless"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://klipper-init-d \
    file://printer.cfg"

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
    install -d ${D}/root/klipper
    cp -r ${S}/klippy ${D}/root/klipper/

    # Preserve .git so moonraker update_manager can report version and pull updates
    cp -r ${S}/.git ${D}/root/klipper/.git
    rm -f ${D}/root/klipper/.git/hooks/*.sample

    # Remove any .pyc files to avoid TMPDIR references
    find ${D} -name '*.pyc' -delete
    find ${D} -name '__pycache__' -type d -exec rm -rf {} + 2>/dev/null || true

    # Config directory
    install -d ${D}/root/printer_data
    install -d ${D}/root/printer_data/config
    install -d ${D}/root/printer_data/logs
    cp ${WORKDIR}/printer.cfg ${D}/root/printer_data/config/printer.cfg

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    cp ${WORKDIR}/klipper-init-d ${D}${sysconfdir}/init.d/klipper
    chmod 0755 ${D}${sysconfdir}/init.d/klipper
}

FILES:${PN} = " \
    /root/klipper \
    ${sysconfdir}/init.d/klipper \
    /root/printer_data \
"

CONFFILES:${PN} = "/root/printer_data/config/printer.cfg"
