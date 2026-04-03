SUMMARY = "fbDOOM - Doom port for Linux framebuffer devices"
DESCRIPTION = "Adaptation of the original DOOM to be easily portable to \
framebuffer devices with minimal dependencies. Writes directly to /dev/fb0 \
with keyboard input via /dev/input, no SDL required."
HOMEPAGE = "https://github.com/maximevince/fbDOOM"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://README.TXT;md5=1b8ba0a9b01bda7d0b612664e246878c"

SRC_URI = " \
    git://github.com/maximevince/fbDOOM.git;protocol=https;branch=master \
    https://www.jbserver.com/downloads/games/doom/misc/shareware/doom1.wad.zip \
    file://0001-touchscreen-support-ig.patch \
    file://0002-skip-exit-confirmation.patch \
    file://play-doom.sh \
"
SRC_URI[sha256sum] = "c1d1f430e623b5b02693a2ab42988f951fb66ae3bd3add06e557bdf36af0e24f"
SRCREV = "6c599f50e9e8e9436a5c064f42836eb48ff6bde0"

S = "${WORKDIR}/git"

EXTRA_OEMAKE = " \
    'CC=${CC}' \
    NOSDL=1 \
    'CFLAGS=${CFLAGS} -ggdb3 -Wall -DNORMALUNIX -DLINUX -DSNDSERV' \
    'LDFLAGS=${LDFLAGS}' \
    'LIBS=-lm -lc' \
"

do_compile() {
    oe_runmake -C ${S}/fbdoom CROSS_COMPILE=""
}

do_install() {
    install -d ${D}${bindir}
    install -m 0755 ${S}/fbdoom/fbdoom ${D}${bindir}/fbdoom

    # Install wad
    install -d ${D}${datadir}/doom
    install -m 0644 ${WORKDIR}/DOOM1.WAD ${D}${datadir}/doom/doom1.wad

    # Install play-doom script
    install -m 0755 ${WORKDIR}/play-doom.sh ${D}${bindir}/play-doom
}

FILES:${PN} = " \
    ${bindir}/fbdoom \
    ${datadir}/doom/doom1.wad \
    ${bindir}/play-doom \
"
