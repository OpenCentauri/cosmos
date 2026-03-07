DESCRIPTION = "OpenCentauri initramfs for squashfs + overlayfs boot"
LICENSE = "GPL-3.0-only"

# initramfs-framework-base : /init (module loader) + 99-finish (switch_root)
# initramfs-module-rootfs  : 90-rootfs (mounts squashfs via root= cmdline)
# opencentauri-initramfs-module : 91-opencentauri (UDISK, overlay, printer_data)
PACKAGE_INSTALL = "initramfs-framework-base initramfs-module-rootfs opencentauri-initramfs-module busybox"

IMAGE_FSTYPES = "cpio.gz"
IMAGE_FEATURES = ""
IMAGE_LINGUAS = ""

# Do not include a kernel inside the initramfs
PACKAGE_EXCLUDE = "kernel-image-*"

IMAGE_ROOTFS_SIZE = "8192"
IMAGE_ROOTFS_EXTRA_SPACE = "0"

inherit core-image
