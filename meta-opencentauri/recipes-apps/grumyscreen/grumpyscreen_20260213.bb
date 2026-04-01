inherit update-rc.d

SUMMARY = "Grumpy Screen - Native Touch UI for Klipper/Moonraker"
DESCRIPTION = "Grumpy Screen is a native touch UI for 3D printers running \
    Klipper/Moonraker. Built on LVGL as a standalone executable with no \
    dependency on X/Wayland display servers."
HOMEPAGE = "https://github.com/pellcorp/grumpyscreen"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "gitsm://github.com/jamesturton/grumpyscreen.git;protocol=https;branch=opencentauri \
    file://0002-Coalesce-file-list-refreshes.patch \
    file://grumpyscreen.init \
    file://grumpyscreen.cfg \
"
SRCREV = "0fabbf49efbd4c0cf35775f05f3e2a637e46e569"
PR = "r2"

S = "${WORKDIR}/git"

RDEPENDS:${PN} = " \
    klipper \
    moonraker \
    gui-switcher \
"

INITSCRIPT_NAME = "grumpyscreen"
INITSCRIPT_PARAMS = "disable"

EXTRA_OEMAKE = " \
    CROSS_COMPILE=yocto- \
    CC='${CC}' \
    CXX='${CXX}' \
    AR='${AR}' \
    OBJCOPY='${OBJCOPY}' \
"

do_compile[vardeps] += "DISTRO DISTRO_VERSION"

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
            GUPPYSCREEN_VERSION="${DISTRO_VERSION}"
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
