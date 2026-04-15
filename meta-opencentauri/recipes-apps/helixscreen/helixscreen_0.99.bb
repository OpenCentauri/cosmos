SUMMARY = "HelixScreen - Interface for Klipper"
DESCRIPTION = "A modern, lightweight touchscreen interface for Klipper 3D printers"
HOMEPAGE = "https://github.com/prestonbrown/helixscreen"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "gitsm://github.com/prestonbrown/helixscreen.git;protocol=https;branch=main \
    file://helixscreen.init \
"
# Main-branch commit that carries PLATFORM_TARGET=yocto support (Makefile
# YOCTO_BUILD mode, splash/watchdog LDFLAGS preservation, Config::init
# HELIX_CONFIG_DIR override, docker-based dev loop).
SRCREV = "cde268af7a8ea1af4ebab9cad0a57b7ef31a87a1"

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

# HelixScreen has a PLATFORM_TARGET=yocto mode in its Makefile that honors
# the bitbake-provided toolchain + CFLAGS env and skips all in-tree submodule
# dep builds (deps come via DEPENDS above).
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

# Install layout: ${datadir}/helixscreen, with the binary under a bin/
# subdir so helix-screen's data_root_resolver (see
# src/application/data_root_resolver.cpp) can locate ui_xml/, assets/,
# config/ as siblings of bin/ just like the install.sh deployment does.
# A symlink at ${bindir}/helix-screen keeps the binary on $PATH.
#
# /opt is not a viable target on cosmos — cosmos's rootfs is a read-only
# squashfs with no /opt mount point. /user-resource/ (where install.sh
# places the self-installed binary at runtime) is a separate ext4 partition
# and not where Yocto packages belong; Yocto packages are baked into the
# squashfs via ${datadir}/${bindir}.
HELIX_DIR = "${datadir}/helixscreen"

do_install() {
    install -d ${D}${HELIX_DIR}/bin
    install -m 0755 ${B}/build/yocto/bin/helix-screen ${D}${HELIX_DIR}/bin/helix-screen

    # Optional supervisor binaries — ship if the build produced them (splash
    # + watchdog are targets built by the Makefile on embedded platforms).
    for exe in helix-splash helix-watchdog; do
        [ -x ${B}/build/yocto/bin/$exe ] && install -m 0755 ${B}/build/yocto/bin/$exe ${D}${HELIX_DIR}/bin/
    done

    # Launcher wrapper (invokes watchdog or bare binary depending on which
    # supervisors are present).
    [ -x ${S}/scripts/helix-launcher.sh ] && install -m 0755 ${S}/scripts/helix-launcher.sh ${D}${HELIX_DIR}/bin/

    # Asset + layout trees (loaded at runtime; no rebuild required to modify).
    install -d ${D}${HELIX_DIR}/ui_xml
    cp -r ${S}/ui_xml/. ${D}${HELIX_DIR}/ui_xml/

    install -d ${D}${HELIX_DIR}/assets
    cp -r ${S}/assets/. ${D}${HELIX_DIR}/assets/

    # Default/seed config — runtime config is written to HOME by the app, so
    # these ship as reference files only.
    install -d ${D}${HELIX_DIR}/config
    for f in default_layout.json helix_macros.cfg helixscreen.env \
             printer_database.json printing_tips.json settings.json.template; do
        [ -f ${S}/config/$f ] && install -m 0644 ${S}/config/$f ${D}${HELIX_DIR}/config/
    done

    for d in presets sounds themes print_start_profiles printer_database.d platform; do
        if [ -d ${S}/config/$d ]; then
            install -d ${D}${HELIX_DIR}/config/$d
            cp -r ${S}/config/$d/. ${D}${HELIX_DIR}/config/$d/
        fi
    done

    # Convenience symlink so helix-screen is on $PATH.
    install -d ${D}${bindir}
    ln -sf ${HELIX_DIR}/bin/helix-screen ${D}${bindir}/helix-screen

    # SysV init hook (update-rc.d wires it into the right rc*.d dirs). The
    # shipped init script in files/ is a cosmos-specific variant that points
    # HELIX_CONFIG_DIR at ~/printer_data so user settings persist even when
    # the baseline install is on the read-only squashfs rootfs. Only
    # DAEMON_DIR differs by packaging site, so we patch it here.
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/helixscreen.init ${D}${sysconfdir}/init.d/helixscreen
    sed -i 's|^DAEMON_DIR=.*|DAEMON_DIR="${HELIX_DIR}"|' ${D}${sysconfdir}/init.d/helixscreen
}

FILES:${PN} = " \
    ${HELIX_DIR} \
    ${bindir}/helix-screen \
    ${sysconfdir}/init.d/helixscreen \
"

# Runtime deps — Klipper + Moonraker are the upstream ecosystem; without them
# helix-screen launches but has nothing to talk to.
RDEPENDS:${PN} = " \
    klipper \
    moonraker \
    wpa-supplicant \
"

INITSCRIPT_NAME = "helixscreen"
# S80 places us after network / Moonraker.
INITSCRIPT_PARAMS = "start 80 2 3 4 5 . stop 20 0 1 6 ."
