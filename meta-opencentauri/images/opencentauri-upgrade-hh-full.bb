DESCRIPTION = "OpenCentauri HH-Full Upgrade Image (Standard + Happy Hare + Mainsail)"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "file://sw-description-hh-full"

do_copy_swu_description() {
    cp ${WORKDIR}/sw-description-hh-full ${WORKDIR}/sw-description
}
addtask do_copy_swu_description before do_swuimage after do_unpack

S = "${WORKDIR}"

IMAGE_DEPENDS = "opencentauri-image-hh-full-mmc virtual/kernel u-boot"

SWUPDATE_IMAGES = " \
    opencentauri-image-hh-full-mmc-elegoo-centauri-carbon1.rootfs \
    bootA \
    bootlogos \
    u-boot-sunxi-with-spl \
"

SWUPDATE_IMAGES_FSTYPES[opencentauri-image-hh-full-mmc-elegoo-centauri-carbon1.rootfs] = ".squashfs"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[opencentauri-image-hh-full-mmc-elegoo-centauri-carbon1.rootfs] = "1"

SWUPDATE_IMAGES_FSTYPES[bootA] = ".img"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[bootA] = "1"

SWUPDATE_IMAGES_FSTYPES[bootlogos] = ".img"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[bootlogos] = "1"

SWUPDATE_IMAGES_FSTYPES[u-boot-sunxi-with-spl] = ".bin"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[u-boot-sunxi-with-spl] = "1"

SWUPDATE_SIGNING = "RSA"
SWUPDATE_PRIVATE_KEY = "${TOPDIR}/../meta-opencentauri/images/files/swupdate_private.pem"

inherit swupdate
