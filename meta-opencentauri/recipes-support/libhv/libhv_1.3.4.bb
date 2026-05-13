SUMMARY = "libhv — event loop, HTTP, WebSocket, MQTT library"
DESCRIPTION = "A c/c++ network library for developing TCP/UDP/SSL/HTTP/WebSocket/MQTT \
    client/server. Used by helixscreen for Moonraker WebSocket + HTTP clients."
HOMEPAGE = "https://github.com/ithewei/libhv"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=122a2f8324611e54381a1de69934481b"

SRC_URI = "git://github.com/ithewei/libhv.git;protocol=https;branch=master \
    file://libhv-dns-resolver-fallback.patch \
    file://libhv-set-soversion.patch \
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
# cmake install copies them to ${includedir}/hv. Headers and the CMake
# package config belong in -dev so downstream consumers can find_package(hv)
# at build time, while the runtime package only ships the .so.
FILES:${PN}-dev += "${libdir}/cmake/hv"

# With libhv-set-soversion.patch applied, cmake produces the standard
# libhv.so -> libhv.so.1 -> libhv.so.1.3.4 symlink chain natively and stamps
# DT_SONAME=libhv.so.1 into the ELF. Default Yocto FILES split picks up the
# right pieces:
#   runtime: libhv.so.1, libhv.so.${PV}
#   -dev:    libhv.so
