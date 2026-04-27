require opencentauri-image-base.bb

DESCRIPTION = "OpenCentauri HH-Full Image (Standard + Happy Hare)"
LICENSE = "GPL-3.0-only"

# Enable MMU/COSMOS macros for this image
PACKAGECONFIG:pn-kalico = "opencentauri-hh-full"

# Happy Hare und MMU-Optimierungen hinzufügen
CORE_IMAGE_EXTRA_INSTALL += "\
    happy-hare \
    kalico-firmware-mmu \
    zram-emmc-swap \
    gui-switcher \
"

# zram-Konfiguration für MMU-Betrieb vorbereiten (200% Faktor für 128MB RAM = 256MB Swap)
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
}