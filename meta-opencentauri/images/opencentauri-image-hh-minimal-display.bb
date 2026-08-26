#@TYPE: Image
#@NAME: OpenCentauri Minimal Display Image
#@DESCRIPTION: OpenCentauri Minimal Image mit Happy Hare und GuppyScreen (RAM-optimiert für 128MB)

DESCRIPTION = "OpenCentauri Minimal Display Image mit Happy Hare und GuppyScreen"
LICENSE = "GPL-3.0-only"

IMAGE_INSTALL = "packagegroup-core-boot ${CORE_IMAGE_EXTRA_INSTALL}"

IMAGE_LINGUAS = " "

inherit core-image

# Enable MMU/COSMOS macros for this image
PACKAGECONFIG:pn-kalico = "opencentauri-hh-minimal"

# Use guppyscreen as default screen UI
PACKAGECONFIG:pn-config-manager = "minimal-display"

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
    kalico-firmware-mmu \
    moonraker \
    htop \
    i2c-tools \
    nano \
    devmem2 \
    swupdate \
    u-boot-fw-utils \
    zram \
    zram-emmc-swap \
    usb-automount \
    dev-by-id \
    psplash \
    opencentauri-bootlogos \
    swu-flasher \
    update-scripts \
    logrotate \
    happy-hare \
    mainsail-config \
    config-manager \
    grumpyscreen \
    gui-switcher \
"

# zram auf 250% für MMU-Betrieb + Display (128MB RAM = 320MB Swap)
ROOTFS_POSTPROCESS_COMMAND:append = " \
    install_zram_config ; \
    set_guppyscreen_default ; \
"

set_guppyscreen_default() {
    # Set grumpyscreen as default screen UI (MMU/Happy Hare support)
    cat > ${IMAGE_ROOTFS}${datadir}/config-manager/default.conf <<'EOF'
[ui]
screen_ui = grumpyscreen
web_ui = mainsail

[update]
release = stable
EOF
    cp ${IMAGE_ROOTFS}${datadir}/config-manager/default.conf ${IMAGE_ROOTFS}${sysconfdir}/klipper/config/cosmos.conf
}

install_zram_config() {
    install -d ${IMAGE_ROOTFS}/etc/default
    echo "FACTOR=250" > ${IMAGE_ROOTFS}/etc/default/zram

    # Swappiness und Memory-Parameter für MMU + Display optimieren
    install -d ${IMAGE_ROOTFS}/etc/sysctl.d
    echo 'vm.swappiness=100' > ${IMAGE_ROOTFS}/etc/sysctl.d/99-mmu-memory.conf
    echo 'vm.vfs_cache_pressure=150' >> ${IMAGE_ROOTFS}/etc/sysctl.d/99-mmu-memory.conf
    echo 'vm.watermark_boost_factor=0' >> ${IMAGE_ROOTFS}/etc/sysctl.d/99-mmu-memory.conf
    echo 'vm.page-cluster=0' >> ${IMAGE_ROOTFS}/etc/sysctl.d/99-mmu-memory.conf

    # OOM-Score Anpassung für Klipper vorbereiten
    install -d ${IMAGE_ROOTFS}/etc/init.d
    cat > ${IMAGE_ROOTFS}/etc/init.d/mmuprotect <<'EOF'
#!/bin/sh
### BEGIN INIT INFO
# Provides: mmuprotect
# Required-Start: klipper moonraker
# Default-Start: 2 3 4 5
# Short-Description: Protect Klipper and MMU from OOM killer
### END INIT INFO

case "$1" in
    start)
        # Klipper OOM-Score senken (weniger likely to be killed)
        if [ -f /var/run/klipper.pid ]; then
            echo -500 > /proc/$(cat /var/run/klipper.pid)/oom_score_adj 2>/dev/null || true
        fi
        # Guppyscreen OOM-Score senken
        if [ -f /var/run/guppyscreen.pid ]; then
            echo -300 > /proc/$(cat /var/run/guppyscreen.pid)/oom_score_adj 2>/dev/null || true
        fi
        ;;
esac
exit 0
EOF
    chmod +x ${IMAGE_ROOTFS}/etc/init.d/mmuprotect
}

# Memory-Optimierungen
IMAGE_FEATURES:remove = "splash"
IMAGE_FEATURES:remove = "tools-debug"

# Explizit entfernte Pakete für Minimal-Image (atomscreen ist RAM-intensiver)
IMAGE_INSTALL:remove = "atomscreen"
BAD_PACKAGES =+ "atomscreen"

INITRAMFS_IMAGE = "core-image-tiny-initramfs"
INITRAMFS_FSTYPES = "cpio.gz"
INITRAMFS_IMAGE_BUNDLE = "1"

# Nur für Centauri Carbon 1 (128MB RAM)
COMPATIBLE_MACHINE = "elegoo-centauri-carbon1"