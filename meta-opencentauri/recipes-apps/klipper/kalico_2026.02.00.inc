HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/OpenCentauri/kalico.git;protocol=https;branch=rpmsg-with-new-hx71x"
SRCREV = "afe7178d0859f3cbc80e591473f86ee64183122b"

PR = "r4"

S = "${WORKDIR}/git"

DEPENDS = " \
    python3-native \
"
