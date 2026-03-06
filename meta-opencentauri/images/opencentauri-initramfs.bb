DESCRIPTION = "OpenCentauri initramfs for squashfs + overlayfs boot"
LICENSE = "GPL-3.0-only"

# Only busybox (provides mount, switch_root, sh, etc.) and our init script
PACKAGE_INSTALL = "busybox opencentauri-initramfs-init"

IMAGE_FSTYPES = "cpio.gz"
IMAGE_FEATURES = ""
IMAGE_LINGUAS = ""

# Do not include a kernel inside the initramfs
PACKAGE_EXCLUDE = "kernel-image-*"

IMAGE_ROOTFS_SIZE = "8192"
IMAGE_ROOTFS_EXTRA_SPACE = "0"

inherit core-image
