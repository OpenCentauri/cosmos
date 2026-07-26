inherit update-rc.d

SUMMARY = "Guppy Screen - Native Touch UI for Klipper/Moonraker"
DESCRIPTION = "Guppy Screen is a native touch UI for 3D printers running \
    Klipper/Moonraker. Built on LVGL as a standalone executable with no \
    dependency on X/Wayland display servers."
HOMEPAGE = "https://github.com/ballaswag/guppyscreen"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "gitsm://github.com/ballaswag/guppyscreen.git;protocol=https;branch=main \
    file://guppyscreen.init \
    file://guppyconfig.json \
    file://0001-lv_driver_fb_ioctls.patch \
    file://0002-spdlog_fmt_initializer_list.patch \
    file://0001-Use-printer-data-config-folder.patch \
"
SRCREV = "cf5c6d7539a2dca090ca71c177f57a2d96df443a"

DEPENDS = "cmake-native"

RDEPENDS:${PN} = " \
    klipper \
    moonraker \
    gui-switcher \
"

INITSCRIPT_NAME = "guppyscreen"
INITSCRIPT_PARAMS = "disable"

EXTRA_OEMAKE = " \
    CROSS_COMPILE=yocto- \
    CC='${CC}' \
    CXX='${CXX}' \
    AR='${AR}' \
"

do_configure() {
    :
}

do_compile() {
    cd ${S}

    # Build bundled libraries
    oe_runmake wpaclient
    oe_runmake libhv.a

    # Build bundled spdlog via cmake with compiler wrappers
    # (spdlog 1.12 with bundled fmt v9 is required by guppyscreen)
    mkdir -p ${S}/spdlog/build

    BARE_CC=$(echo '${CC}' | awk '{print $1}')
    BARE_CXX=$(echo '${CXX}' | awk '{print $1}')
    MACHINE_FLAGS=$(echo '${CC}' | sed "s|^[^ ]* *||")

    printf '#!/bin/sh\nexec %s %s "$@"\n' "$BARE_CC" "$MACHINE_FLAGS" > ${S}/spdlog/build/cc-wrapper
    printf '#!/bin/sh\nexec %s %s "$@"\n' "$BARE_CXX" "$MACHINE_FLAGS" > ${S}/spdlog/build/cxx-wrapper
    chmod +x ${S}/spdlog/build/cc-wrapper ${S}/spdlog/build/cxx-wrapper

    cat > ${S}/spdlog/build/yocto-toolchain.cmake <<TOOLCHAIN
set(CMAKE_SYSTEM_NAME Linux)
set(CMAKE_SYSTEM_PROCESSOR arm)
set(CMAKE_SYSROOT ${RECIPE_SYSROOT})
set(CMAKE_C_COMPILER ${S}/spdlog/build/cc-wrapper)
set(CMAKE_CXX_COMPILER ${S}/spdlog/build/cxx-wrapper)
set(CMAKE_AR ${AR} CACHE FILEPATH "Archiver")
set(CMAKE_RANLIB ${RANLIB} CACHE FILEPATH "Ranlib")
set(CMAKE_FIND_ROOT_PATH_MODE_PROGRAM NEVER)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
TOOLCHAIN

    cmake -B ${S}/spdlog/build -S ${S}/spdlog/ \
        -DCMAKE_TOOLCHAIN_FILE=${S}/spdlog/build/yocto-toolchain.cmake \
        -DSPDLOG_BUILD_SHARED=OFF \
        -Wno-dev
    make -C ${S}/spdlog/build -j${BB_NUMBER_THREADS}

    # Build guppyscreen
    oe_runmake default \
        LDFLAGS="-lm -L${S}/libhv/lib -L${S}/spdlog/build \
            -l:libhv.a -latomic -lpthread \
            -L${S}/wpa_supplicant/wpa_supplicant/ -l:libwpa_client.a \
            -lstdc++fs -l:libspdlog.a \
            ${LDFLAGS}" \
        GUPPYSCREEN_VERSION="${PV}"
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/build/bin/guppyscreen ${D}${bindir}/

    install -d ${D}${datadir}/guppyscreen/themes
    if [ -d ${S}/themes ]; then
        cp -r ${S}/themes/* ${D}${datadir}/guppyscreen/themes/
    fi

    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
    install -m 0644 ${UNPACKDIR}/guppyconfig.json ${D}${sysconfdir}/klipper/config/

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/guppyscreen.init ${D}${sysconfdir}/init.d/guppyscreen
}

FILES:${PN} = " \
    ${bindir}/guppyscreen \
    ${datadir}/guppyscreen \
    ${sysconfdir}/klipper/config/guppyconfig.json \
    ${sysconfdir}/init.d/guppyscreen \
"

CONFFILES:${PN} = "${sysconfdir}/klipper/config/guppyconfig.json"

INSANE_SKIP:${PN} = "ldflags"
