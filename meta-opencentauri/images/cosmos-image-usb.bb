require cosmos-image-base.bb

DESCRIPTION = "Cosmos USB Image"
LICENSE = "GPL-3.0-only"

IMAGE_FEATURES += "package-management"

WKS_FILES = "cosmos-usb-image.wks.in"

WKS_FILE_DEPENDS += "e2fsprogs-native"

# sunxi.inc already appends wic.gz/wic.bmap machine-wide; override (not
# append) so we skip the ~8 GB ext3/tar.gz outputs nobody flashes from here.
IMAGE_FSTYPES = "wic.gz wic.bmap"
