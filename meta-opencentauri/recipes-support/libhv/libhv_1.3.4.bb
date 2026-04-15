SUMMARY = "libhv — event loop, HTTP, WebSocket, MQTT library"
DESCRIPTION = "A c/c++ network library for developing TCP/UDP/SSL/HTTP/WebSocket/MQTT \
    client/server. Used by helixscreen for Moonraker WebSocket + HTTP clients."
HOMEPAGE = "https://github.com/ithewei/libhv"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=122a2f8324611e54381a1de69934481b"

SRC_URI = "git://github.com/ithewei/libhv.git;protocol=https;branch=master \
    file://libhv-dns-resolver-fallback.patch \
"
SRCREV = "71770e04becaa149e0ef8ffc4d3900c5466ddddb"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

S = "${WORKDIR}/git"

DEPENDS = "openssl zlib"

inherit cmake pkgconfig

EXTRA_OECMAKE = " \
    -DBUILD_SHARED=ON \
    -DBUILD_STATIC=OFF \
    -DBUILD_EXAMPLES=OFF \
    -DBUILD_UNITTEST=OFF \
    -DWITH_OPENSSL=ON \
    -DWITH_HTTP=ON \
    -DWITH_HTTP_SERVER=ON \
    -DWITH_HTTP_CLIENT=ON \
    -DWITH_EVPP=ON \
    -DWITH_MQTT=OFF \
"

# libhv generates hv/json.hpp etc. into include/hv at configure time;
# cmake install copies them to ${includedir}/hv. Also ship the CMake
# package configuration so downstream cmake projects can find_package(hv).
FILES:${PN} += "${libdir}/cmake/hv"

# Suppress "file /usr/include/hv/... not shipped" warnings by making -dev pick
# up all public headers (default behavior, but be explicit for clarity).
# libhv's CMakeLists doesn't set SOVERSION, so `make install` produces
# /usr/lib/libhv.so as a plain ELF rather than the usual
# libhv.so -> libhv.so.1 -> libhv.so.1.3.4 symlink chain. Rename it at install
# time to match the PROJECT_VERSION and create the expected symlinks so the
# shared object lands in the runtime package and -lhv still resolves at build
# time.
do_install:append() {
    if [ -f ${D}${libdir}/libhv.so ] && [ ! -L ${D}${libdir}/libhv.so ]; then
        mv ${D}${libdir}/libhv.so ${D}${libdir}/libhv.so.${PV}
        ln -s libhv.so.${PV} ${D}${libdir}/libhv.so.1
        ln -s libhv.so.${PV} ${D}${libdir}/libhv.so
    fi
}
