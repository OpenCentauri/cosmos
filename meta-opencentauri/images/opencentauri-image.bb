DESCRIPTION = "OpenCentauri Image"
LICENSE = "MIT"

IMAGE_INSTALL = "packagegroup-core-boot ${CORE_IMAGE_EXTRA_INSTALL}"

IMAGE_LINGUAS = " "

inherit core-image

IMAGE_FEATURES += "\
    ssh-server-dropbear \
    package-management \
"

CORE_IMAGE_EXTRA_INSTALL += "\
    usbutils \
    libgpiod \
    libgpiod-tools \
    kernel-modules \
    linux-firmware-rtl8821 \
    wpa-supplicant \
    iw \
    klipper \
    htop \
    i2c-tools \
    nano \
"

#    moonraker
#    mainsail