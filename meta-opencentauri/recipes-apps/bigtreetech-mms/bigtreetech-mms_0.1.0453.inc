HOMEPAGE = "https://github.com/bigtreetech/BIGTREETECH_MMS"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://klippy/extras/mms/__init__.py;endline=6;md5=84b6c60d7eaf485d965aa2c4552e3134"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/bigtreetech/BIGTREETECH_MMS.git;protocol=https;branch=master \
    file://0001-add-sense-resistor-to-tmc2209-stepper-configs.patch \
"
SRCREV = "c36bc98cd50f15313458476e35f36e668f2f1e03"

S = "${WORKDIR}/git"
