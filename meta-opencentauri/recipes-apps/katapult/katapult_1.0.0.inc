HOMEPAGE = "https://github.com/Arksine/katapult"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=db95b6e40dc7d26d8308b6b7375637b6"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = " \
    git://github.com/Arksine/katapult.git;protocol=https;branch=master \
    file://01-add-support-for-48kib-offset.patch \
    file://0001-Add-DEPLOYER_PAYLOAD-parameter.patch \
"
SRCREV = "32584cbbb66c4dc85fc87c0fa87ed508f7c2df52"

S = "${WORKDIR}/git"

DEPENDS = " \
    python3-native \
"