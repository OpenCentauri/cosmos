DESCRIPTION = "OpenCentauri Upgrade Image"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://sw-description \
"

IMAGE_DEPENDS = "opencentauri-image-mmc virtual/kernel u-boot"

SWUPDATE_IMAGES = " \
    opencentauri-image-mmc-elegoo-centauri-carbon1.rootfs \
    bootA \
    bootlogos \
    u-boot-sunxi-with-spl \
"

SWUPDATE_IMAGES_FSTYPES[opencentauri-image-mmc-elegoo-centauri-carbon1.rootfs] = ".squashfs"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[opencentauri-image-mmc-elegoo-centauri-carbon1.rootfs] = "1"

SWUPDATE_IMAGES_FSTYPES[bootA] = ".img"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[bootA] = "1"

SWUPDATE_IMAGES_FSTYPES[bootlogos] = ".img"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[bootlogos] = "1"

SWUPDATE_IMAGES_FSTYPES[u-boot-sunxi-with-spl] = ".bin"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[u-boot-sunxi-with-spl] = "1"

# Build-time signing posture is gated behind SWUPDATE_ENABLE_SIGNING so an
# operator who wants signed SWUs can opt in without the recipe carrying a
# latent reference to a private key that may not be present in the tree.
# Install-time verification is independent and lives in
# meta-opencentauri/recipes-support/swupdate/files/fragment.cfg.
#
# To enable build-time signing:
#   1. Drop swupdate_private.pem into recipes-support/swupdate/files/
#      (do NOT commit; key material stays out of the public tree).
#   2. Confirm it pairs with the shipped swupdate_public.pem via
#      `openssl rsa -in priv -pubout | diff - public.pem`.
#   3. Set SWUPDATE_ENABLE_SIGNING="1" in local.conf (NOT in this
#      recipe; that would re-introduce the drift this guard removes).
SWUPDATE_ENABLE_SIGNING ?= "0"
SWUPDATE_SIGNING = "${@'RSA' if d.getVar('SWUPDATE_ENABLE_SIGNING') == '1' else ''}"
SWUPDATE_PRIVATE_KEY = "${@'${THISDIR}/files/swupdate_private.pem' if d.getVar('SWUPDATE_ENABLE_SIGNING') == '1' else ''}"

inherit swupdate
