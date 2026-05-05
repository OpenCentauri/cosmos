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

    # Install config files
    install -d ${D}${sysconfdir}/klipper/config/extras-readonly
    install -m 0644 ${WORKDIR}/canvas.cfg ${WORKDIR}/afc.cfg ${D}${sysconfdir}/klipper/config/extras-readonly
}

FILES:${PN} = " \
    ${datadir}/klipper/AFC* \
    ${sysconfdir}/klipper/config/extras-readonly/afc.cfg \
    ${sysconfdir}/klipper/config/extras-readonly/canvas.cfg \
"