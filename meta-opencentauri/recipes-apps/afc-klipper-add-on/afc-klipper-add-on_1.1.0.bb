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

PR = "r2"

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

    # Patch shipped AFC.cfg defaults for the Centauri Carbon layout.
    # The Centauri Carbon's stock toolhead provides four on-bed locations
    # we can map AFC's load/unload macros onto (see klipper recipe macros.cfg):
    #   * rear-right purge tray at (X=202, Y=264.5) — used by MOVE_TO_TRAY,
    #     LOAD_FILAMENT; serves as park + poop location for AFC.
    #   * physical brush at (X=128, Y=261.5), 30mm wide along X — driven
    #     by WIPE_NOZZLE in macros.cfg; AFC_BRUSH X-scrubs over the same
    #     range as wipe step.
    #   * front-right filament cutter at X=255: cutter is mounted to the
    #     right of the toolhead and engages by pressing the toolhead in -Y
    #     against the front case (UNLOAD_FILAMENT does Y=30→Y=3 at X=255).
    #     AFC_CUT models this as cut_direction="front" with pin_loc_xy at
    #     the lever-engage point and cut_move_dist sized for full
    #     compression at Y=3.
    # Stock has no kick post, so kick stays disabled. form_tip handles tip
    # formation on unload alongside the physical cut.
    #
    # Other tweaks:
    #   * VarFile: absolute path under /etc/klipper/config keeps AFC state
    #     on the rw overlay and visible through moonraker's "config" root.
    #   * moonraker_port: this image runs moonraker on :80, not :7125.
    sed -i \
        -e 's|^VarFile:.*|VarFile: ${sysconfdir}/klipper/config/AFC/AFC.var|' \
        -e 's|^#moonraker_port: 7125|moonraker_port: 80|' \
        -e 's|^kick: True|kick: False|' \
        -e 's|^form_tip: False|form_tip: True|' \
        ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/AFC.cfg

    # Patch AFC_Macro_Vars.cfg with Centauri-Carbon-tuned positions:
    #   * park_loc_xy / purge_loc_xy → rear-right tray (X=202, Y=264.5),
    #     same spot cosmos MOVE_TO_TRAY / LOAD_FILAMENT use.
    #   * z_purge_move=False → AFC_POOP would otherwise drop to Z=0.6mm
    #     absolute before purging, which collides with the tray mid-print.
    #     Cosmos LOAD_FILAMENT just extrudes at the current Z; mirror that.
    #   * brush_loc → cosmos brush center (X=128, Y=261.5), Z=-1 to skip
    #     the Z move; cosmos WIPE_NOZZLE only reaches the brush via a
    #     strain-gauge re-home which we can't do mid-print, so AFC_BRUSH
    #     scrubs at whatever Z the toolhead is already at.
    #   * y_brush=False → cosmos brush is a single X-axis line at Y=261.5,
    #     no Y back-and-forth; matches WIPE_NOZZLE's X-only scrub pattern.
    #   * pin_loc_xy / cut_direction → front-right corner cutter. Cutter
    #     is mounted on the right side of the toolhead at X=255 and is
    #     engaged by pressing the toolhead in -Y against the front case;
    #     UNLOAD_FILAMENT goes Y=30 (clearance) → Y=3 (fully compressed).
    #     pin_loc_y=11 places lever-engagement ~8mm before full
    #     compression at the default cut_move_dist=8.5, leaving the
    #     default pin_park_dist=6 (toolhead parks at Y=17 before the cut).
    sed -i \
        -e 's|^variable_park_loc_xy *: *-99, *-99|variable_park_loc_xy              : 202, 264.5|' \
        -e 's|^variable_purge_loc_xy *: *-99, *-99|variable_purge_loc_xy             : 202, 264.5|' \
        -e 's|^variable_z_purge_move *: *True|variable_z_purge_move             : False|' \
        -e 's|^variable_brush_loc *: *-99,-99,-1|variable_brush_loc                : 128, 261.5, -1|' \
        -e 's|^variable_y_brush *: *True|variable_y_brush                  : False|' \
        -e 's|^variable_pin_loc_xy *: *-99, *-99|variable_pin_loc_xy               : 255, 11|' \
        -e 's|^variable_cut_direction *: *"left"|variable_cut_direction            : "front"|' \
        ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/AFC_Macro_Vars.cfg

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
