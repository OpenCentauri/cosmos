DESCRIPTION = "OpenCentauri Upgrade Image"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://sw-description \
    file://swupdate_private.pem \
"

IMAGE_DEPENDS = "opencentauri-image virtual/kernel u-boot"

SWUPDATE_IMAGES = " \
    opencentauri-image-elegoo-centauri-carbon1.rootfs \
    boot \
    u-boot-sunxi-with-spl \
"

SWUPDATE_IMAGES_FSTYPES[opencentauri-image-elegoo-centauri-carbon1.rootfs] = ".squashfs"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[opencentauri-image-elegoo-centauri-carbon1.rootfs] = "1"

SWUPDATE_IMAGES_FSTYPES[boot] = ".img"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[boot] = "1"

SWUPDATE_IMAGES_FSTYPES[u-boot-sunxi-with-spl] = ".bin"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[u-boot-sunxi-with-spl] = "1"

SWUPDATE_SIGNING = "RSA"
SWUPDATE_PRIVATE_KEY = "${THISDIR}/files/swupdate_private.pem"

inherit swupdate
