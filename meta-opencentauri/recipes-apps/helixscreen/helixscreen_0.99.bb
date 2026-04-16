SUMMARY = "HelixScreen - touch UI for Klipper"
DESCRIPTION = "A modern, lightweight touchscreen interface for Klipper 3D printers"
HOMEPAGE = "https://github.com/prestonbrown/helixscreen"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "gitsm://github.com/prestonbrown/helixscreen.git;protocol=https;branch=main \
    file://helixscreen.init \
"
# Main-branch commit that carries the assets/config split (RO seeds out of
# config/), the find_readable / writable_path / get_data_dir resolver helpers,
# and the PLATFORM_TARGET=yocto Makefile path.
SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit pkgconfig update-rc.d

DEPENDS = " \
    libhv \
    openssl \
    spdlog \
    fmt \
    alsa-lib \
    libusb1 \
    wpa-supplicant \
    libnl \
    zlib \
"

# helix-screen talks to Klipper exclusively via the Moonraker WebSocket+REST
# API; klipper itself is a transitive dep of moonraker. moonraker is enough.
RDEPENDS:${PN} = " \
    moonraker \
    wpa-supplicant \
"

# HelixScreen has a PLATFORM_TARGET=yocto mode in its Makefile that honors the
# bitbake-provided toolchain + CFLAGS env and skips all in-tree submodule dep
# builds (deps come via DEPENDS above).
EXTRA_OEMAKE = " \
    PLATFORM_TARGET=yocto \
    CC='${CC}' \
    CXX='${CXX}' \
    AR='${AR}' \
    LD='${LD}' \
    STRIP='${STRIP}' \
    RANLIB='${RANLIB}' \
    OBJCOPY='${OBJCOPY}' \
"

# Run the upstream Makefile. Required for both gitsm-fetched and externalsrc
# builds — externalsrc's default do_compile is a no-op.
do_compile() {
    cd ${S}
    oe_runmake
}

# Layout (FHS-aligned, matches guppyscreen / grumpyscreen conventions):
#   ${bindir}/helix-screen                — primary binary (real, not symlink)
#   ${bindir}/helix-splash                — early-boot splash binary
#   ${bindir}/helix-watchdog              — crash dialog + auto-restart
#   ${bindir}/helix-launcher.sh           — supervisor wrapper
#   ${datadir}/helixscreen/ui_xml/        — runtime-loaded LVGL XML layouts
#   ${datadir}/helixscreen/assets/        — fonts, images, sounds, RO seed
#                                           configs (assets/config/printer_database.json,
#                                           presets/, themes/defaults/, etc.)
#   ${sysconfdir}/init.d/helixscreen      — SysV init script
#   ${sysconfdir}/klipper/config/helixscreen/settings.json
#                                         — default user settings (CONFFILES;
#                                           preserved across opkg upgrade).
#                                           Init script exports HELIX_CONFIG_DIR
#                                           pointing at this directory so
#                                           runtime state writes land here too.
HELIX_DATA_DIR = "${datadir}/helixscreen"
HELIX_USER_CONFIG_DIR = "${sysconfdir}/klipper/config/helixscreen"

do_install() {
    # --- binaries -----------------------------------------------------------
    install -d ${D}${bindir}
    install -m 0755 ${B}/build/yocto/bin/helix-screen ${D}${bindir}/helix-screen
    for exe in helix-splash helix-watchdog; do
        if [ -x ${B}/build/yocto/bin/$exe ]; then
            install -m 0755 ${B}/build/yocto/bin/$exe ${D}${bindir}/$exe
        fi
    done
    if [ -x ${S}/scripts/helix-launcher.sh ]; then
        install -m 0755 ${S}/scripts/helix-launcher.sh ${D}${bindir}/helix-launcher.sh
    fi

    # --- read-only data root ------------------------------------------------
    install -d ${D}${HELIX_DATA_DIR}/assets
    cp -r ${S}/ui_xml ${D}${HELIX_DATA_DIR}/ui_xml
    # Exclude dev-only test fixtures (~150 MB of mock-mode gcodes / timelapse
    # frames) and macOS metadata. tar+pipe is portable; avoids needing rsync
    # in HOSTTOOLS.
    (cd ${S} && tar cf - \
        --exclude=test_gcodes \
        --exclude=test_timelapse \
        --exclude='*.gcode' \
        --exclude='.DS_Store' \
        assets) | (cd ${D}${HELIX_DATA_DIR} && tar xf -)

    # --- writable user-config dir + default settings.json --------------------
    # Ship the cosmos preset as the initial settings.json. Marked CONFFILES so
    # opkg upgrades leave user edits alone. The app writes runtime state
    # (telemetry_*.json, tool_spools.json, custom_images/, etc.) into this same
    # directory at runtime via $HELIX_CONFIG_DIR.
    install -d ${D}${HELIX_USER_CONFIG_DIR}
    if [ -f ${S}/assets/config/presets/cc1.json ]; then
        install -m 0644 ${S}/assets/config/presets/cc1.json ${D}${HELIX_USER_CONFIG_DIR}/settings.json
    fi

    # --- init script -------------------------------------------------------
    # The shipped helixscreen.init template has DAEMON_DIR / HELIX_CONFIG_DIR /
    # HELIX_DATA_DIR placeholders that get rewritten here so the script matches
    # this packaging's actual install paths. The same template is the one used
    # for tarball installs — only the DAEMON_DIR / env paths differ.
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/helixscreen.init ${D}${sysconfdir}/init.d/helixscreen
    sed -i \
        -e "s|@HELIX_BINDIR@|${bindir}|" \
        -e "s|@HELIX_DATA_DIR@|${HELIX_DATA_DIR}|" \
        -e "s|@HELIX_USER_CONFIG_DIR@|${HELIX_USER_CONFIG_DIR}|" \
        ${D}${sysconfdir}/init.d/helixscreen
}

FILES:${PN} = " \
    ${bindir}/helix-screen \
    ${bindir}/helix-splash \
    ${bindir}/helix-watchdog \
    ${bindir}/helix-launcher.sh \
    ${HELIX_DATA_DIR} \
    ${sysconfdir}/init.d/helixscreen \
    ${HELIX_USER_CONFIG_DIR} \
"

CONFFILES:${PN} = "${HELIX_USER_CONFIG_DIR}/settings.json"

INITSCRIPT_NAME = "helixscreen"
# Don't auto-start: cosmos uses gui-switcher (config-manager ui screen_ui)
# to pick which UI runs. See README in this layer for activation.
INITSCRIPT_PARAMS = "disable"

# --- post-install: migrate state from a previous tarball install ------------
# When opkg installs onto a system that already has the legacy
# /user-resource/helixscreen tarball install, copy the user's writable state
# into the new ${HELIX_USER_CONFIG_DIR}. Bundled seeds (printer_database.json,
# themes/defaults/, presets/, etc.) are NOT copied — those now live in
# ${HELIX_DATA_DIR}/assets/config/ and find_readable() falls back to them
# automatically when the user dir doesn't have a per-file override.
pkg_postinst:${PN}() {
    legacy_dir=$D/user-resource/helixscreen/config
    target_dir=$D${HELIX_USER_CONFIG_DIR}

    if [ -d "$legacy_dir" ] && [ ! -f "$target_dir/.migrated_from_tarball" ]; then
        mkdir -p "$target_dir"

        # Writable user state — copy if present, never overwrite existing files
        # in the new dir (so a prior opkg install's state wins over an even
        # older tarball).
        for f in settings.json helixconfig.json telemetry_config.json \
                 telemetry_device.json telemetry_queue.json \
                 telemetry_snapshot.json tool_spools.json \
                 ace_slot_overrides.json pending_remap.json \
                 helixscreen.env crash_history.json; do
            if [ -f "$legacy_dir/$f" ] && [ ! -f "$target_dir/$f" ]; then
                cp "$legacy_dir/$f" "$target_dir/$f"
            fi
        done

        # User-managed dirs
        for d in custom_images printer_database.d themes; do
            if [ -d "$legacy_dir/$d" ] && [ ! -d "$target_dir/$d" ]; then
                cp -r "$legacy_dir/$d" "$target_dir/$d"
            fi
        done

        # Drop user themes/defaults — those are shipped now in
        # ${HELIX_DATA_DIR}/assets/config/themes/defaults/ and will be
        # found via find_readable's data_dir fallback.
        rm -rf "$target_dir/themes/defaults"

        touch "$target_dir/.migrated_from_tarball"
    fi
}
