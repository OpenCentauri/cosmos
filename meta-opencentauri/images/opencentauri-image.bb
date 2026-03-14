DESCRIPTION = "OpenCentauri Image"
LICENSE = "GPL-3.0-only"

IMAGE_INSTALL = "packagegroup-core-boot ${CORE_IMAGE_EXTRA_INSTALL}"

IMAGE_FSTYPES += "wic squashfs"

IMAGE_LINGUAS = " "

inherit core-image extract-partition

IMAGE_FEATURES += "\
    ssh-server-dropbear \
    package-management \
    read-only-rootfs \
"

CORE_IMAGE_EXTRA_INSTALL += "\
    usbutils \
    libgpiod \
    libgpiod-tools \
    kernel-modules \
    rtw88 \
    wpa-supplicant \
    iw \
    kalico \
    moonraker \
    mainsail \
    guppyscreen \
    htop \
    i2c-tools \
    nano \
    devmem2 \
    mjpg-streamer \
    swupdate \
    u-boot-fw-utils \
    zram \
    zram-emmc-swap \
    usb-automount \
    psplash \
    opencentauri-bootlogos \
"

WKS_FILES = "opencentauri-usb-image.wks.in"
WKS_FILE_DEPENDS += "squashfs-tools-native"

EXTRACT_PARTITION_LABELS = "boot bootlogos"
WIC_CREATE_EXTRA_ARGS += "--no-fstab-update"

INITRAMFS_IMAGE = "core-image-tiny-initramfs"
INITRAMFS_FSTYPES = "cpio.gz"
INITRAMFS_IMAGE_BUNDLE = "1"
