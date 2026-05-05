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
    install -d ${D}${datadir}/klipper/extras
    cp -r ${S}/extras/* ${D}${datadir}/klipper/extras/
    rm ${D}${datadir}/klipper/extras/__init__.py

    # Install config files
    install -d ${D}${sysconfdir}/klipper/config/extras-readonly
    install -m 0644 ${WORKDIR}/canvas.cfg ${WORKDIR}/afc.cfg ${D}${sysconfdir}/klipper/config/extras-readonly
}

FILES:${PN} = " \
    ${datadir}/klipper/extras/AFC_buffer.py \
    ${datadir}/klipper/extras/AFC_stats.py \
    ${datadir}/klipper/extras/AFC_Toolchanger.py \
    ${datadir}/klipper/extras/AFC_respond.py \
    ${datadir}/klipper/extras/AFC_extruder.py \
    ${datadir}/klipper/extras/AFC_unit.py \
    ${datadir}/klipper/extras/AFC_prep.py \
    ${datadir}/klipper/extras/AFC_led.py \
    ${datadir}/klipper/extras/AFC_button.py \
    ${datadir}/klipper/extras/AFC_vivid.py \
    ${datadir}/klipper/extras/AFC_logger.py \
    ${datadir}/klipper/extras/AFC_spool.py \
    ${datadir}/klipper/extras/AFC_BoxTurtle.py \
    ${datadir}/klipper/extras/AFC_lane.py \
    ${datadir}/klipper/extras/AFC_canvas.py \
    ${datadir}/klipper/extras/AFC_hub.py \
    ${datadir}/klipper/extras/AFC_canvas_lane.py \
    ${datadir}/klipper/extras/openams_integration.py \
    ${datadir}/klipper/extras/AFC_poop.py \
    ${datadir}/klipper/extras/AFC_stepper.py \
    ${datadir}/klipper/extras/AFC_QuattroBox.py \
    ${datadir}/klipper/extras/AFC_utils.py \
    ${datadir}/klipper/extras/AFC_HTLF.py \
    ${datadir}/klipper/extras/AFC_form_tip.py \
    ${datadir}/klipper/extras/AFC_assist.py \
    ${datadir}/klipper/extras/AFC.py \
    ${datadir}/klipper/extras/AFC_OpenAMS.py \
    ${datadir}/klipper/extras/AFC_functions.py \
    ${datadir}/klipper/extras/AFC_NightOwl.py \
    ${datadir}/klipper/extras/AFC_error.py \
    ${sysconfdir}/klipper/config/extras-readonly/afc.cfg \
    ${sysconfdir}/klipper/config/extras-readonly/canvas.cfg \
"