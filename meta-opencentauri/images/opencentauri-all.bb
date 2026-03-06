DESCRIPTION = "OpenCentauri -- build all images in one target"
LICENSE = "GPL-3.0-only"

# This is a meta-recipe with no packages of its own.
# Building it triggers all deployable OpenCentauri artifacts:
#   - opencentauri-image      (squashfs rootfs)
#   - opencentauri-initramfs  (cpio.gz initramfs; via opencentauri-boot-image)
#   - opencentauri-boot-image (vfat boot partition, kernel+DTB+initramfs)
#   - u-boot                  (U-Boot with SPL)

inherit nopackages

do_build[depends] = "\
    opencentauri-image:do_image_complete \
    opencentauri-boot-image:do_deploy \
    u-boot:do_deploy \
"
