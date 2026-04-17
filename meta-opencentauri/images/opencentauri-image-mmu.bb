#@TYPE: Image
#@NAME: OpenCentauri MMU Image
#@DESCRIPTION: OpenCentauri Image optimized for MMU operation with Happy Hare.
# Removes all native UIs and uses external web interface for maximum RAM availability.

DESCRIPTION = "OpenCentauri Image optimized for MMU operation (Happy Hare)"
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
    camera-led-bridge \
    logrotate \
    happy-hare \
"

# zram auf 200% für MMU-Betrieb (ausgewogen für 128MB RAM = 256MB Swap)
ROOTFS_POSTPROCESS_COMMAND:append = " \
    install_zram_config ; \
"

install_zram_config() {
    install -d ${IMAGE_ROOTFS}/etc/default
    echo "FACTOR=200" > ${IMAGE_ROOTFS}/etc/default/zram

    # Swappiness und Memory-Parameter für MMU optimieren (ausgewogen)
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
        ;;
esac
exit 0
EOF
    chmod +x ${IMAGE_ROOTFS}/etc/init.d/mmuprotect
}

# Memory-Optimierungen
IMAGE_FEATURES:remove = "splash"
IMAGE_FEATURES:remove = "tools-debug"

INITRAMFS_IMAGE = "core-image-tiny-initramfs"
INITRAMFS_FSTYPES = "cpio.gz"
INITRAMFS_IMAGE_BUNDLE = "1"

# Nur für Centauri Carbon 1 (128MB RAM)
COMPATIBLE_MACHINE = "elegoo-centauri-carbon1"
