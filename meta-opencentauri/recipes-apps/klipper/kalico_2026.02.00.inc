HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/OpenCentauri/kalico.git;protocol=https;branch=sims/merge-kalico-2 \
    file://0001-allow-save-tare.patch \
    file://0002-remove-save-config-subfile-check.patch \
"
SRCREV = "2b8a8b2073e697c9e420e9f2a1fcea3f3729e13c"

PR = "r4"

S = "${WORKDIR}/git"

DEPENDS = " \
    python3-native \
"
