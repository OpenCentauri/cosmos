HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/OpenCentauri/kalico.git;protocol=https;branch=james/trsync \
    file://0001-allow-save-tare.patch \
    file://0002-remove-save-config-subfile-check.patch \
"
SRCREV = "af54513157ae6f596784c798326826d89b35753d"

PR = "r5"

S = "${WORKDIR}/git"

DEPENDS = " \
    python3-native \
"
