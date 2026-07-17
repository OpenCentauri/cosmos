require kalico_${PV}.inc

SUMMARY = "Kalico 3D Printer Firmware"
DESCRIPTION = "Klipper, but Limitless"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI += " \
    file://klipper-init-d \
    file://config/shared/macros.cfg \
    file://config/shared/calibration.cfg \
    file://config/shared/kamp.cfg \
    file://config/shared/client.cfg \
    file://config/shared/shell.cfg \
    file://config/shared/screen.cfg \
    file://config/cc1/printer.cfg \
    file://config/cc1/machine.cfg \
    file://config/cc2/printer.cfg \
    file://config/cc2/machine.cfg \
"

CFG_VARIANT = "cc1"
CFG_VARIANT:elegoo-centauri-carbon2 = "cc2"

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
    config-manager \
    kalico-firmware-dsp \
    kalico-firmware-toolhead \
    kalico-firmware-bed \
"

# CC2 uses the stock MCU applications. Only power-cycle them and ask the
# Elegoo bootloader to jump to the application; do not install CC1 firmware.
RDEPENDS:${PN}:remove:elegoo-centauri-carbon2 = " \
    kalico-firmware-toolhead \
    kalico-firmware-bed \
"
RDEPENDS:${PN}:append:elegoo-centauri-carbon2 = " \
    cc2-toolhead-mcu \
    cc2-bed-mcu \
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

do_install[vardeps] += "DISTRO_NAME DISTRO_VERSION"

do_install() {
    # Install klipper python package
    install -d ${D}${datadir}/klipper
    cp -r ${S}/klippy ${D}${datadir}/klipper/

    # Set our ver
    sed -i 's/APP_NAME = "Kalico"/APP_NAME = "${DISTRO_NAME}"/' ${D}${datadir}/klipper/klippy/__init__.py
    echo "Release - ${DISTRO_VERSION}" > ${D}${datadir}/klipper/klippy/.version

    # Remove any .pyc files to avoid TMPDIR references
    find ${D} -name '*.pyc' -delete
    find ${D} -name '__pycache__' -type d -exec rm -rf {} + 2>/dev/null || true

    # Make config_examples to suppress moonraker warnings
    install -d ${D}${datadir}/klipper/config

    # make docs dir to suppress moonraker warnings
    install -d ${D}${datadir}/klipper/docs

    # Install default kalico config
    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
    install -d ${D}${sysconfdir}/klipper/config/shared
    install -d ${D}${sysconfdir}/klipper/config/cc1
    install -d ${D}${sysconfdir}/klipper/config/cc2
    cp -r ${UNPACKDIR}/config/shared/. ${D}${sysconfdir}/klipper/config/shared/
    cp -r ${UNPACKDIR}/config/cc1/. ${D}${sysconfdir}/klipper/config/cc1/
    cp -r ${UNPACKDIR}/config/cc2/. ${D}${sysconfdir}/klipper/config/cc2/

    # Keep the selected variant at the established live paths used by
    # klipper-init-d and by user tooling.
    install -m 0644 ${UNPACKDIR}/config/${CFG_VARIANT}/printer.cfg \
        ${D}${sysconfdir}/klipper/config/printer.cfg

    # Shared files are the base; variant files can override them. printer.cfg
    # stays at the live user-editable path and is never copied readonly.
    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly
    cp -r ${UNPACKDIR}/config/shared/. \
        ${D}${sysconfdir}/klipper/config/klipper-readonly/
    for cfg in ${UNPACKDIR}/config/${CFG_VARIANT}/*.cfg; do
        name=$(basename "$cfg")
        if [ "$name" != "printer.cfg" ]; then
            install -m 0644 "$cfg" \
                ${D}${sysconfdir}/klipper/config/klipper-readonly/
        fi
    done

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/klipper-init-d ${D}${sysconfdir}/init.d/klipper
}

FILES:${PN} = " \
    ${datadir}/klipper \
    ${sysconfdir}/init.d/klipper \
    ${sysconfdir}/klipper/config \
"

CONFFILES:${PN} = " \
    ${sysconfdir}/klipper/config/printer.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/macros.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/machine.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/shell.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/screen.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/calibration.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/kamp.cfg \
    ${sysconfdir}/klipper/config/klipper-readonly/client.cfg \
"
