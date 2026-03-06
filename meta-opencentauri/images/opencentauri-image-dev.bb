DESCRIPTION = "OpenCentauri Development Image"
LICENSE = "GPL-3.0-only"

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
    rtw88 \
    wpa-supplicant \
    iw \
    kalico \
    moonraker \
    mainsail \
    opencentauri-usb-automount \
    opencentauri-zram-swap \
    nginx \
    git \
    htop \
    i2c-tools \
    nano \
    devmem2 \
    mjpg-streamer \
    opencentauri-firmware \
"
