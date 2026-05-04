SUMMARY = "Armored Turtle Automated Filament Changer (AFC) Klipper add-on"
DESCRIPTION = "Klipper extras and config for ArmoredTurtle AFC (BoxTurtle, NightOwl, HTLF, QuattroBox, ViViD)"
HOMEPAGE = "https://github.com/ArmoredTurtle/AFC-Klipper-Add-On"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=db95b6e40dc7d26d8308b6b7375637b6"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/ArmoredTurtle/AFC-Klipper-Add-On.git;protocol=https;branch=main \
    file://btt-vivid.cfg \
"
# Tag 1.1.0
SRCREV = "68cfe778d013a7f6694c2c6f21ee2cfc1a8f65c9"

PR = "r0"

S = "${WORKDIR}/git"

# Pure-python drop-in extras; no compile step.
do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    # Klipper extras: drop AFC python modules into kalico's klippy/extras
    # Skip extras/__init__.py — kalico already owns that path.
    install -d ${D}${datadir}/klipper/klippy/extras
    install -m 0644 ${S}/extras/AFC*.py ${D}${datadir}/klipper/klippy/extras/

    # AFC configs as readonly templates alongside other klipper-readonly cfgs
    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC
    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/mcu
    install -d ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/macros
    install -m 0644 ${S}/config/AFC.cfg            ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/
    install -m 0644 ${S}/config/AFC_Macro_Vars.cfg ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/
    install -m 0644 ${S}/config/mcu/*.cfg          ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/mcu/
    install -m 0644 ${S}/config/macros/*.cfg       ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/macros/

    # Pre-wired BTT ViViD config (serial-by-id paths matching BTT firmware).
    install -m 0644 ${WORKDIR}/btt-vivid.cfg       ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/
}

FILES:${PN} = " \
    ${datadir}/klipper/klippy/extras/AFC*.py \
    ${sysconfdir}/klipper/config/klipper-readonly/AFC \
"

RDEPENDS:${PN} = "kalico"
