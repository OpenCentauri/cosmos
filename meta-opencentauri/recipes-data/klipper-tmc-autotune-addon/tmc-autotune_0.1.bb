HOMEPAGE = "https://github.com/andrewmcgr/klipper_tmc_autotune"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"
SUMMARY = "TMC Autotune"
DESCRIPTION = "TMC stepper driver autotuning Klipper python extra" 

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = " \
    git://github.com/andrewmcgr/klipper_tmc_autotune.git;protocol=https;branch=main \
    file://motor_database.cfg \
"

SRCREV = "3f979750b758ac95c67e19f658f60566c19fff82"

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
    cp ${S}/autotune_tmc.py ${S}/motor_constants.py ${D}${datadir}/klipper/klippy/extras/
    cp ${WORKDIR}/motor_database.cfg ${D}${datadir}/klipper/klippy/extras/
}

FILES:${PN} = " \
    ${datadir}/klipper/klippy/extras/autotune_tmc.py \
    ${datadir}/klipper/klippy/extras/motor_constants.py \
    ${datadir}/klipper/klippy/extras/motor_database.cfg \
"