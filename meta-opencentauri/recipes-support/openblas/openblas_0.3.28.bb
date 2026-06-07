#
#   Copyright (c) 2016 Intel Corporation. All rights reserved.
#   Copyright (c) 2019 Luxoft Sweden AB
#
#   SPDX-License-Identifier: MIT
#

DESCRIPTION = "OpenBLAS is an optimized BLAS library based on GotoBLAS2 1.13 BSD version."
SUMMARY = "OpenBLAS : An optimized BLAS library"
AUTHOR = "Alexander Leiva <norxander@gmail.com>"
HOMEPAGE = "http://www.openblas.net/"
SECTION = "libs"
LICENSE = "BSD-3-Clause"

DEPENDS = "make libgfortran patchelf-native"

LIC_FILES_CHKSUM = "file://LICENSE;md5=5adf4792c949a00013ce25d476a2abc0"

SRC_URI = "\
	file://0001-fix-cblas-installation-ignores-the-value-of-NO_CBLAS.patch \
	git://github.com/xianyi/OpenBLAS.git;protocol=https;branch=develop \
	"

SRCREV = "5ef8b1964658f9cb6a6324a06f6a1a022609b0c5"

S = "${WORKDIR}/git"

# Used for TARGET=... , documented in TargetList.txt
BLAS_X86_ARCH ?= "ATOM"
BLAS_AARCH32_ARCH ?= "CORTEXA9"
BLAS_AARCH64_ARCH ?= "ARMV8"
BLAS_ARM_ARCH ?= "ARMV7"

def map_arch(a, d):
        import re
        if re.match('i.86$', a): return d.getVar('BLAS_X86_ARCH')
        elif re.match('x86_64$', a): return d.getVar('BLAS_X86_ARCH')
        elif re.match('aarch32$', a): return d.getVar('BLAS_AARCH32_ARCH')
        elif re.match('aarch64$', a): return d.getVar('BLAS_AARCH64_ARCH')
        elif re.match('arm$', a): return d.getVar('BLAS_ARM_ARCH')
        return a

def map_bits(a, d):
        import re
        if re.match('i.86$', a): return 32
        elif re.match('x86_64$', a): return 64
        elif re.match('aarch32$', a): return 32
        elif re.match('aarch64$', a): return 64
        elif re.match('arm$', a): return 32
        return 32

def map_extra_options(a, d):
        import re
        if re.match('arm$', a): return '-mfpu=neon-vfpv4 -mfloat-abi=hard'
        return ''

PACKAGECONFIG[lapack] = ""
PACKAGECONFIG[lapacke] = ""
PACKAGECONFIG[cblas] = ""
PACKAGECONFIG[affinity] = ""
PACKAGECONFIG[openmp] = ""
PACKAGECONFIG[dynarch] = ""

PACKAGECONFIG ??= "openmp"

do_compile () {
	oe_runmake HOSTCC="${BUILD_CC}" \
		CC="${TARGET_PREFIX}gcc ${TOOLCHAIN_OPTIONS} ${@map_extra_options(d.getVar('TARGET_ARCH', True), d)}" \
		PREFIX=${exec_prefix} \
		CROSS=1 \
		CROSS_SUFFIX=${HOST_PREFIX} \
		NO_STATIC=1 \
		${@bb.utils.contains('PACKAGECONFIG', 'lapack', 'NO_LAPACK=0', 'NO_LAPACK=1', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'lapacke', 'NO_LAPACKE=0', 'NO_LAPACKE=1', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'cblas', 'NO_CBLAS=0', 'NO_CBLAS=1', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'affinity', 'NO_AFFINITY=0', 'NO_AFFINITY=1', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'openmp', 'USE_OPENMP=1', 'USE_OPENMP=0', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'dynarch', 'DYNAMIC_ARCH=1', 'DYNAMIC_ARCH=0', d)} \
		BINARY='${@map_bits(d.getVar('TARGET_ARCH', True), d)}' \
		TARGET='${@map_arch(d.getVar('TARGET_ARCH', True), d)}'
}

do_install() {
	oe_runmake HOSTCC="${BUILD_CC}" \
		CC="${TARGET_PREFIX}gcc ${TOOLCHAIN_OPTIONS}" \
		PREFIX=${exec_prefix} \
		CROSS=1 \
		CROSS_SUFFIX=${HOST_PREFIX} \
		NO_STATIC=1 \
		${@bb.utils.contains('PACKAGECONFIG', 'lapack', 'NO_LAPACK=0', 'NO_LAPACK=1', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'lapacke', 'NO_LAPACKE=0', 'NO_LAPACKE=1', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'cblas', 'NO_CBLAS=0', 'NO_CBLAS=1', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'affinity', 'NO_AFFINITY=0', 'NO_AFFINITY=1', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'openmp', 'USE_OPENMP=1', 'USE_OPENMP=0', d)} \
		${@bb.utils.contains('PACKAGECONFIG', 'dynarch', 'DYNAMIC_ARCH=1', 'DYNAMIC_ARCH=0', d)} \
		BINARY='${@map_bits(d.getVar('TARGET_ARCH', True), d)}' \
		TARGET='${@map_arch(d.getVar('TARGET_ARCH', True), d)}' \
		DESTDIR=${D} \
		install


	rm -rf ${D}${bindir}

	cd ${D}${libdir}
	cp -ar libopenblas*r*.so libblas.so.3
	patchelf --set-soname libblas.so.3 libblas.so.3
	ln -s libblas.so.3 libblas.so
}

FILES:${PN} = "${libdir}/lib*"
FILES:${PN}-dev = "${includedir} ${libdir}/lib${PN}.a ${libdir}/libblas.a ${libdir}/cmake ${libdir}/pkgconfig ${libdir}/libopenblas.so ${libdir}/libblas.so"

DEPENDS:remove:class-native = "libgfortran"
BBCLASSEXTEND = "native"
