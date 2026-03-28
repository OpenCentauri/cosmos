SUMMARY = "lzbench - in-memory benchmark of open-source compression algorithms"
DESCRIPTION = "lzbench is an in-memory benchmark of open-source LZ77/LZSS/LZMA \
compressors. It joins all compressors into a single executable, compiling them \
with the same compiler and optimizations for fair comparison."
HOMEPAGE = "https://github.com/inikep/lzbench"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=d328bcd7c6004f4f7adae0842ed19146"

SRCREV = "74d7438dbfc8119055796053b24abd29e65617a0"
SRC_URI = "git://github.com/inikep/lzbench.git;protocol=https;branch=master"

S = "${WORKDIR}/git"

DEPENDS = ""

do_configure[noexec] = "1"

EXTRA_OEMAKE = " \
    CC='${CC}' \
    CXX='${CXX}' \
    USER_CFLAGS='${CFLAGS}' \
    USER_CXXFLAGS='${CXXFLAGS}' \
    USER_LDFLAGS='${LDFLAGS}' \
    BUILD_STATIC=0 \
    DONT_BUILD_LZSSE=1 \
    DONT_BUILD_DENSITY=1 \
    DONT_BUILD_YAPPY=1 \
    DONT_BUILD_LZMAT=1 \
    DONT_BUILD_LZRW=1 \
    DONT_BUILD_WFLZ=1 \
"

do_compile() {
    oe_runmake -j${@oe.utils.cpu_count()} lzbench
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/lzbench ${D}${bindir}/lzbench
}

FILES:${PN} = "${bindir}/lzbench"
