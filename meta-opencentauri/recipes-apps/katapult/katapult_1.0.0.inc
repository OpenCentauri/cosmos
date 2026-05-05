HOMEPAGE = "https://github.com/Arksine/katapult"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=db95b6e40dc7d26d8308b6b7375637b6"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = " \
    git://github.com/Arksine/katapult.git;protocol=https;branch=master \
    file://0001-Add-DEPLOYER_PAYLOAD-parameter.patch \
    file://0002-add-support-for-48kib-and-64kib-offset.patch \
"
SRCREV = "ec59b9bb9ad6c2ec8d4dc6831fbc77f0b308e29e"

S = "${WORKDIR}/git"

DEPENDS = " \
    python3-native \
"