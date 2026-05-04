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

    # Patch shipped AFC.cfg defaults for the OpenCentauri layout:
    #   * VarFile: AFC saves to klippy CWD-relative path by default; absolute
    #     path under /etc/klipper/config keeps state on the rw overlay and
    #     visible through moonraker's "config" file root.
    #   * moonraker_port: this image runs moonraker on :80, not the upstream :7125.
    sed -i \
        -e 's|^VarFile:.*|VarFile: ${sysconfdir}/klipper/config/AFC/AFC.var|' \
        -e 's|^#moonraker_port: 7125|moonraker_port: 80|' \
        ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/AFC.cfg

    # Writable dir for AFC.var / AFC.var.unit (created on overlay at runtime).
    install -d ${D}${sysconfdir}/klipper/config/AFC

    # Pre-wired BTT ViViD config (serial-by-id paths matching BTT firmware).
    install -m 0644 ${WORKDIR}/btt-vivid.cfg       ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/
}

FILES:${PN} = " \
    ${datadir}/klipper/klippy/extras/AFC*.py \
    ${sysconfdir}/klipper/config/klipper-readonly/AFC \
    ${sysconfdir}/klipper/config/AFC \
"

RDEPENDS:${PN} = "kalico"
