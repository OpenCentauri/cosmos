DESCRIPTION = "OpenCentauri Base Image"
LICENSE = "GPL-3.0-only"

IMAGE_INSTALL = "packagegroup-core-boot ${CORE_IMAGE_EXTRA_INSTALL}"

IMAGE_LINGUAS = " "

inherit core-image

IMAGE_FEATURES += "ssh-server-dropbear"

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
    fluidd \
    atomscreen \
    guppyscreen \
    grumpyscreen \
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
    dev-by-id \
    psplash \
    opencentauri-bootlogos \
    swu-flasher \
    camera-led-bridge \
    chrony \
"

INITRAMFS_IMAGE = "core-image-tiny-initramfs"
INITRAMFS_FSTYPES = "cpio.gz"
INITRAMFS_IMAGE_BUNDLE = "1"
