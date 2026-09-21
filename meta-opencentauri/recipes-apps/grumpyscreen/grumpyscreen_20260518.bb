inherit update-rc.d

SUMMARY = "Grumpy Screen - Native Touch UI for Klipper/Moonraker"
DESCRIPTION = "Grumpy Screen is a native touch UI for 3D printers running \
    Klipper/Moonraker. Built on LVGL as a standalone executable with no \
    dependency on X/Wayland display servers."
HOMEPAGE = "https://github.com/pellcorp/grumpyscreen"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "gitsm://github.com/pellcorp/grumpyscreen.git;protocol=https;branch=main \
    file://grumpyscreen.init \
    file://grumpyscreen.cfg \
"
SRCREV = "1606638724577f99292155489e8b920646200cfe"
PR = "r7"

S = "${WORKDIR}/git"

RDEPENDS:${PN} = " \
    klipper \
    moonraker \
    gui-switcher \
    generate-support-zip \
"

INITSCRIPT_NAME = "grumpyscreen"
INITSCRIPT_PARAMS = "disable"

EXTRA_OEMAKE = " \
    CROSS_COMPILE=yocto- \
    CC='${CC}' \
    CXX='${CXX}' \
    AR='${AR}' \
    OBJCOPY='${OBJCOPY}' \
    STRIP='${STRIP}' \
"

INSANE_SKIP:${PN} += "already-stripped"

do_compile[vardeps] += "DISTRO DISTRO_VERSION"

do_compile:prepend() {
    # Force full rebuild to pick up new version
    cd ${S}
    oe_runmake clean || true
}

do_compile() {
    cd ${S}
    oe_runmake libhv.a
    oe_runmake wpaclient
    oe_runmake default \
            LDFLAGS="-lm -L${S}/libhv/lib \
            -l:libhv.a -latomic -lpthread \
            -L${S}/wpa_supplicant/wpa_supplicant/ -l:libwpa_client.a \
            -lstdc++fs \
            ${LDFLAGS}" \
            GUPPY_SMALL_SCREEN="true" \
            GUPPYSCREEN_BRANCH="${DISTRO}" \
            GUPPYSCREEN_VERSION="${DISTRO_VERSION}" \
            COSMOS="true" \
            MMU_BACKENDS="afc"
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/build/bin/grumpyscreen ${D}${bindir}

    install -d ${D}${datadir}/grumpyscreen/themes
    if [ -d ${S}/themes ]; then
        cp -r ${S}/themes/* ${D}${datadir}/grumpyscreen/themes/
    fi

    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
    echo "# Add other extra configuration here.\n" > ${D}${sysconfdir}/klipper/config/grumpyscreen.cfg

    install -d ${D}${sysconfdir}/klipper/config/grumpyscreen-readonly
    install -m 0644 ${WORKDIR}/grumpyscreen.cfg ${D}${sysconfdir}/klipper/config/grumpyscreen-readonly/

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/grumpyscreen.init ${D}${sysconfdir}/init.d/grumpyscreen
}

FILES:${PN} = " \
    ${bindir}/grumpyscreen \
    ${datadir}/grumpyscreen \
    ${sysconfdir}/klipper/config/grumpyscreen.cfg \
    ${sysconfdir}/klipper/config/grumpyscreen-readonly/grumpyscreen.cfg \
    ${sysconfdir}/init.d/grumpyscreen \
"

CONFFILES:${PN} = " \
    ${sysconfdir}/klipper/config/grumpyscreen.cfg \
    ${sysconfdir}/klipper/config/grumpyscreen-readonly/grumpyscreen.cfg \
"
