HOMEPAGE = "https://github.com/suchmememanyskill/AFC-Klipper-Add-On"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=db95b6e40dc7d26d8308b6b7375637b6"
SUMMARY = "AFC-Klipper-Add-On"
DESCRIPTION = "Automated Filament Changer Software." 

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = " \
    git://github.com/suchmememanyskill/AFC-Klipper-Add-On.git;protocol=https;branch=DEV \
    file://afc.cfg \
    file://canvas.cfg \
"

SRCREV = "9b5291954d27fdcfb5913a79c72e15884a9935b7"

S = "${WORKDIR}/git"

DEPENDS = " \
    python3-native \
"

RDEPENDS:${PN} = " \
    klipper \
"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    # Install klippy extras
    install -d ${D}${datadir}/klipper/klippy/extras
    cp -r ${S}/extras/* ${D}${datadir}/klipper/klippy/extras/
    rm ${D}${datadir}/klipper/klippy/extras/__init__.py

    # Install config files
    install -d ${D}${sysconfdir}/klipper/config/extras-readonly
    install -m 0644 ${WORKDIR}/canvas.cfg ${WORKDIR}/afc.cfg ${D}${sysconfdir}/klipper/config/extras-readonly
}

FILES:${PN} = " \
    ${datadir}/klipper/klippy/extras/AFC_buffer.py \
    ${datadir}/klipper/klippy/extras/AFC_stats.py \
    ${datadir}/klipper/klippy/extras/AFC_Toolchanger.py \
    ${datadir}/klipper/klippy/extras/AFC_respond.py \
    ${datadir}/klipper/klippy/extras/AFC_extruder.py \
    ${datadir}/klipper/klippy/extras/AFC_unit.py \
    ${datadir}/klipper/klippy/extras/AFC_prep.py \
    ${datadir}/klipper/klippy/extras/AFC_led.py \
    ${datadir}/klipper/klippy/extras/AFC_button.py \
    ${datadir}/klipper/klippy/extras/AFC_vivid.py \
    ${datadir}/klipper/klippy/extras/AFC_logger.py \
    ${datadir}/klipper/klippy/extras/AFC_spool.py \
    ${datadir}/klipper/klippy/extras/AFC_BoxTurtle.py \
    ${datadir}/klipper/klippy/extras/AFC_lane.py \
    ${datadir}/klipper/klippy/extras/AFC_canvas.py \
    ${datadir}/klipper/klippy/extras/AFC_hub.py \
    ${datadir}/klipper/klippy/extras/AFC_canvas_lane.py \
    ${datadir}/klipper/klippy/extras/openams_integration.py \
    ${datadir}/klipper/klippy/extras/AFC_poop.py \
    ${datadir}/klipper/klippy/extras/AFC_stepper.py \
    ${datadir}/klipper/klippy/extras/AFC_QuattroBox.py \
    ${datadir}/klipper/klippy/extras/AFC_utils.py \
    ${datadir}/klipper/klippy/extras/AFC_HTLF.py \
    ${datadir}/klipper/klippy/extras/AFC_form_tip.py \
    ${datadir}/klipper/klippy/extras/AFC_assist.py \
    ${datadir}/klipper/klippy/extras/AFC.py \
    ${datadir}/klipper/klippy/extras/AFC_OpenAMS.py \
    ${datadir}/klipper/klippy/extras/AFC_functions.py \
    ${datadir}/klipper/klippy/extras/AFC_NightOwl.py \
    ${datadir}/klipper/klippy/extras/AFC_error.py \
    ${sysconfdir}/klipper/config/extras-readonly/afc.cfg \
    ${sysconfdir}/klipper/config/extras-readonly/canvas.cfg \
"