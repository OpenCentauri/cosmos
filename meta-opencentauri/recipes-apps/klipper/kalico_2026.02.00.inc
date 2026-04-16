HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/OpenCentauri/kalico.git;protocol=https;branch=rpmsg-with-new-hx71x \
    git://github.com/pellcorp/klipper.git;protocol=https;branch=jun2025;name=pellcorp;destsuffix=git/pellcorp \
    file://0001-allow-save-tare.patch \
    file://0002-remove-save-config-subfile-check.patch \
"
SRCREV = "afe7178d0859f3cbc80e591473f86ee64183122b"
SRCREV_pellcorp = "02a55be6e1939fdb514761c508548dcc0da7cc3b"

SRCREV_FORMAT = "kalico"

PR = "r4"

S = "${WORKDIR}/git"

DEPENDS = " \
    python3-native \
"
