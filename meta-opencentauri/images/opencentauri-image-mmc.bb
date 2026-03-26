require opencentauri-image-base.bb
inherit extract-partition

DESCRIPTION = "OpenCentauri eMMC Image"
LICENSE = "GPL-3.0-only"

IMAGE_FSTYPES += "wic squashfs"
EXTRA_IMAGECMD:squashfs = "-comp lz4"

IMAGE_FEATURES += "read-only-rootfs overlayfs-etc"

WKS_FILES = "opencentauri-mmc-image.wks.in"
WKS_FILE_DEPENDS += "squashfs-tools-native"

EXTRACT_PARTITION_LABELS = "bootA bootlogos"

OVERLAYFS_ETC_MOUNT_POINT = "/data"
OVERLAYFS_ETC_DEVICE = "/dev/mmcblk0p10"
OVERLAYFS_ETC_FSTYPE ?= "ext4"
OVERLAYFS_ETC_CREATE_MOUNT_DIRS = "0"
OVERLAYFS_ETC_INIT_TEMPLATE = "${FILE_DIRNAME}/files/overlayfs-etc-preinit.sh.in"
