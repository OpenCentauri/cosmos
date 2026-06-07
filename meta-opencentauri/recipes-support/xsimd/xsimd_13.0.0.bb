DESCRIPTION = "C++ wrappers for SIMD intrinsics and parallelized, optimized mathematical functions (SSE, AVX, AVX512, NEON, SVE)"
HOMEPAGE = "https://github.com/xtensor-stack/xsimd"
LICENSE = "BSD-3-Clause"

inherit cmake

#PR = "r1"

LIC_FILES_CHKSUM = "file://LICENSE;md5=fdd4d7dc8bdd9ae7181dd0bca68007a2"

SRC_URI = "git://github.com/xtensor-stack/xsimd.git;protocol=https;branch=master"

SRCREV = "5ac7edf30d0f519e0b7344b933382e4fc02fdee7"

S = "${WORKDIR}/git"

BBCLASSEXTEND = "native nativesdk"
