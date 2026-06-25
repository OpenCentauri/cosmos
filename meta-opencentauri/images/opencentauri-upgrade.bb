DESCRIPTION = "OpenCentauri Upgrade Image"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://sw-description \
"

IMAGE_DEPENDS = "opencentauri-image-mmc virtual/kernel u-boot"

SWUPDATE_IMAGES = " \
    opencentauri-image-mmc \
    bootA \
    bootlogos \
    u-boot-sunxi-with-spl \
"

SWUPDATE_IMAGES_FSTYPES[opencentauri-image-mmc] = ".rootfs.squashfs"

SWUPDATE_IMAGES_FSTYPES[bootA] = ".img"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[bootA] = "1"

SWUPDATE_IMAGES_FSTYPES[bootlogos] = ".img"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[bootlogos] = "1"

SWUPDATE_IMAGES_FSTYPES[u-boot-sunxi-with-spl] = ".bin"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[u-boot-sunxi-with-spl] = "1"

SWUPDATE_SIGNING = "RSA"
SWUPDATE_PRIVATE_KEY = "${THISDIR}/files/${MACHINE}/swupdate_private.pem"

inherit swupdate
