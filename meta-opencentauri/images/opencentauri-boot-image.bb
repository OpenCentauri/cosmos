DESCRIPTION = "OpenCentauri boot partition image (vfat with kernel+DTB+initramfs)"
LICENSE = "GPL-3.0-only"

inherit nopackages deploy

DEPENDS = "virtual/kernel opencentauri-initramfs dosfstools-native mtools-native"

# bootA/bootB partitions are 6300 blocks of 1024 bytes = 6,451,200 bytes.
BOOT_IMAGE_SIZE ?= "6300"

do_deploy() {
    BOOT_IMG="${DEPLOYDIR}/opencentauri-boot.vfat"

    # Create empty vfat image
    dd if=/dev/zero of=${BOOT_IMG} bs=1024 count=${BOOT_IMAGE_SIZE}
    mkfs.vfat ${BOOT_IMG}

    # Copy kernel
    mcopy -i ${BOOT_IMG} ${DEPLOY_DIR_IMAGE}/zImage ::zImage

    # Copy DTB
    mcopy -i ${BOOT_IMG} ${DEPLOY_DIR_IMAGE}/elegoo-centauri-carbon1.dtb ::elegoo-centauri-carbon1.dtb

    # Copy initramfs (image recipes produce a .rootfs.cpio.gz symlink)
    mcopy -i ${BOOT_IMG} \
        ${DEPLOY_DIR_IMAGE}/opencentauri-initramfs-${MACHINE}.rootfs.cpio.gz \
        ::initramfs.cpio.gz
}

addtask deploy after do_install before do_build
do_deploy[depends] = "virtual/kernel:do_deploy opencentauri-initramfs:do_image_complete"
