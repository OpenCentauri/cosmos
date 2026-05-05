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

PR = "r6"

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
    #   * silicone wipe tray at X=173..187, Y=262.5..264.5 — driven by
    #     M729 in macros.cfg; AFC_BRUSH wipes over the same area as the
    #     wipe step. (The cosmos physical brush at X=128, Y=261.5 in
    #     WIPE_NOZZLE needs a strain-gauge re-home and sits at near-zero
    #     Z, so it's only safe during calibration, not mid-print swaps.)
    #   * front-right filament cutter at X=255: cutter is mounted to the
    #     right of the toolhead and engages by pressing the toolhead in -Y
    #     against the front case (UNLOAD_FILAMENT does Y=30→Y=3 at X=255).
    #     AFC_CUT models this as cut_direction="front" with pin_loc_xy at
    #     the lever-engage point and cut_move_dist sized for full
    #     compression at Y=3.
    # Stock has no kick post, so kick stays disabled. form_tip stays
    # disabled too (upstream default): the physical AFC_CUT slices the
    # filament cleanly at the cutter, the post-cut retract pulls the
    # stub back into the buffer, and tip-forming on top of that just
    # adds blobbing/stringing without improving the cut end.
    #
    # Other tweaks:
    #   * VarFile: absolute path under /etc/klipper/config keeps AFC state
    #     on the rw overlay and visible through moonraker's "config" root.
    #   * moonraker_port: this image runs moonraker on :80, not :7125.
    sed -i \
        -e 's|^VarFile:.*|VarFile: ${sysconfdir}/klipper/config/AFC/AFC.var|' \
        -e 's|^#moonraker_port: 7125|moonraker_port: 80|' \
        -e 's|^kick: True|kick: False|' \
        ${D}${sysconfdir}/klipper/config/klipper-readonly/AFC/AFC.cfg

    # Patch AFC_Macro_Vars.cfg with Centauri-Carbon-tuned positions:
    #   * park_loc_xy / purge_loc_xy → rear-right tray (X=202, Y=264.5),
    #     same spot cosmos MOVE_TO_TRAY / LOAD_FILAMENT use.
    #   * z_purge_move=False → AFC_POOP would otherwise drop to Z=0.6mm
    #     absolute before purging, which collides with the tray mid-print.
    #     Cosmos LOAD_FILAMENT just extrudes at the current Z; mirror that.
    #   * brush_loc → silicone wipe tray center (X=180, Y=263.5), Z=-1 to
    #     skip the Z move; the tray's silicone fingers wipe at whatever Z
    #     the toolhead is at, matching cosmos M729's behaviour.
    #   * brush_width=14 / brush_depth=2 → tray spans X=173..187 (=14mm)
    #     and Y=262.5..264.5 (=2mm); these are the X/Y excursions M729
    #     uses for its wipe strokes.
    #   * y_brush=True → wipe along both Y lines (Y=262.5 + Y=264.5),
    #     matching M729's two-row scrub pattern.
    #   * brush_count=5 → M729 does 5 X-passes per row.
    #   * pin_loc_xy / cut_direction / cut_move_dist → front-right corner
    #     cutter. Cutter is mounted on the right side of the toolhead at
    #     X=255 and is engaged by pressing the toolhead in -Y against the
    #     front case. cosmos UNLOAD_FILAMENT goes Y=30 (clearance) → Y=3
    #     to cut, but the lever needs more travel than that to fully chop
    #     thicker filament; bump cut_move_dist to 12 so the toolhead
    #     reaches Y=-1 (full_cut_y = pin_loc_y - cut_move_dist), well
    #     within Y position_min=-2.5. pin_park_dist stays at the default 6
    #     (toolhead parks at Y=17 before approaching).
    sed -i \
        -e 's|^variable_park_loc_xy *: *-99, *-99|variable_park_loc_xy              : 202, 264.5|' \
        -e 's|^variable_purge_loc_xy *: *-99, *-99|variable_purge_loc_xy             : 202, 264.5|' \
        -e 's|^variable_z_purge_move *: *True|variable_z_purge_move             : False|' \
        -e 's|^variable_brush_loc *: *-99,-99,-1|variable_brush_loc                : 180, 263.5, -1|' \
        -e 's|^variable_brush_width *: *30|variable_brush_width              : 14|' \
        -e 's|^variable_brush_depth *: *10|variable_brush_depth              : 2|' \
        -e 's|^variable_brush_count *: *4|variable_brush_count              : 5|' \
        -e 's|^variable_pin_loc_xy *: *-99, *-99|variable_pin_loc_xy               : 255, 11|' \
        -e 's|^variable_cut_direction *: *"left"|variable_cut_direction            : "front"|' \
        -e 's|^variable_cut_move_dist *: *8\.5|variable_cut_move_dist            : 10|' \
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
