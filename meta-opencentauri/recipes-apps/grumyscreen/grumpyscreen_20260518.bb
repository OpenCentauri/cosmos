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
SRCREV = "a7f58c22119c4cbc511d0746ea6c7264de387a1b"
PR = "r2"

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
            GUPPY_SMALL_SCREEN="y" \
            GUPPYSCREEN_BRANCH="${DISTRO}" \
            GUPPYSCREEN_VERSION="${DISTRO_VERSION}" \
            UPDATE_CMD=cosmos_update_cmd \
            UPDATE_TEXT="Update\nCOSMOS" \
            UPDATE_PROMPT="Are you sure you want to update COSMOS?\n\nThis will download and update to the latest version of COSMOS!" \
            UPDATE_SUCCESS="Your printer will restart shortly!" \
            UPDATE_FAILURE="Failed to initiate update COSMOS!" \
            SWITCH_TO_STOCK_TEXT="Switch to OC\nPatched" \
            SWITCH_TO_STOCK_PROMPT="**WARNING** **WARNING** **WARNING**\n\nAre you sure you want to switch to OpenCentauri patched firmware?\n\nThis will take some time, **DO NOT TURN OFF YOUR PRINTER**, just wait for it to reboot." \
            SWITCH_TO_STOCK_FAILURE="Failed to initiate switch to OC Patched!" \
            SWITCH_TO_STOCK_SUCCESS="Your printer will restart shortly!" \
            FACTORY_RESET_TEXT="Factory\nReset" \
            FACTORY_RESET_PROMPT="**WARNING** **WARNING** **WARNING**\n\nAre you sure you want factory reset?\n\nThis will reset all printer setting but it will stay using COSMOS, it will not switch back to stock." \
            FACTORY_RESET_FAILURE="Failed to factory reset!" \
            FACTORY_RESET_SUCCESS="Your printer will restart shortly!"
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/build/bin/guppyscreen ${D}${bindir}/grumpyscreen

    install -d ${D}${datadir}/grumpyscreen/themes
    if [ -d ${S}/themes ]; then
        cp -r ${S}/themes/* ${D}${datadir}/grumpyscreen/themes/
    fi

    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
    install -m 0644 ${WORKDIR}/grumpyscreen.cfg ${D}${sysconfdir}/klipper/config/

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/grumpyscreen.init ${D}${sysconfdir}/init.d/grumpyscreen
}

FILES:${PN} = " \
    ${bindir}/grumpyscreen \
    ${datadir}/grumpyscreen \
    ${sysconfdir}/klipper/config/grumpyscreen.cfg \
    ${sysconfdir}/init.d/grumpyscreen \
"

CONFFILES:${PN} = "${sysconfdir}/klipper/config/grumpyscreen.cfg"
