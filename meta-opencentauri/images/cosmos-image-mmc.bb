require cosmos-image-base.bb
inherit extract-partition

DESCRIPTION = "Cosmos eMMC Image"
LICENSE = "GPL-3.0-only"

IMAGE_FSTYPES += "wic squashfs"
EXTRA_IMAGECMD:squashfs = "-comp lz4"

IMAGE_FEATURES += "read-only-rootfs overlayfs-etc"

WKS_FILES = "cosmos-${MACHINE}.wks.in"
WKS_FILE_DEPENDS += "squashfs-tools-native"
WIC_STOCK_ENV = "${FILE_DIRNAME}/files/${MACHINE}/wic-stock.env"
do_image_wic[file-checksums] += "${WIC_STOCK_ENV}:True"

# Match the SWU installer's 8 KiB bootloader location. Both stock layouts
# fit in 16 GPT entries; Wic's default 128 entries would overlap that write.
# Run before image compression and bmap generation so all artifacts agree.
do_image_wic[depends] += "virtual/bootloader:do_deploy u-boot-mkenvimage-native:do_populate_sysroot"

IMAGE_CMD:wic:append() {
    sgdisk --resize-table=16 "$out.wic"
    dd if="${DEPLOY_DIR_IMAGE}/${SPL_BINARY}" of="$out.wic" bs=1K seek=8 conv=notrunc

}

cosmos_prepare_wic_seed() {
    rm -rf ${WORKDIR}/wic-seed
    # Leave /etc defaults in the read-only rootfs; only seed empty overlay dirs.
    install -d ${WORKDIR}/wic-seed/data/overlay-etc/upper
    install -d ${WORKDIR}/wic-seed/data/overlay-etc/work
    install -d ${WORKDIR}/wic-seed/user/.tmp
    install -d ${WORKDIR}/wic-seed/user/gcodes

    if [ "${MACHINE}" = "centauri-carbon-2" ]; then
        # fstab bind-mounts this directory before Klipper/Moonraker start.
        install -d ${WORKDIR}/wic-seed/user/logs
        install -d ${WORKDIR}/wic-seed/private
        # Seed the FAT private partition without provisioned device credentials.
        printf ',,' > ${WORKDIR}/wic-seed/private/ELEGOO.txt
        chmod 0644 ${WORKDIR}/wic-seed/private/ELEGOO.txt
    fi
}

# Wic's image task is generated dynamically; use its prefunc hook.
cosmos_prepare_wic_env() {
    # Merge stock rollback helpers into the eMMC defaults for the WIC only.
    # Last value wins; sort for reproducible output. mkenvimage supplies the
    # CRC, redundancy flag and padding expected by U-Boot.
    awk -F= '!/^#/ && NF > 1 { settings[$1] = $0 } END { for (key in settings) print settings[key] }' \
        "${DEPLOY_DIR_IMAGE}/u-boot-initial-env-emmc" "${WIC_STOCK_ENV}" \
        | LC_ALL=C sort > "${WORKDIR}/wic-seed/u-boot-initial-env-emmc"
    uboot-mkenvimage -r -s 0x20000 \
        -o "${WORKDIR}/wic-seed/u-boot-initial-env-emmc.bin" \
        "${WORKDIR}/wic-seed/u-boot-initial-env-emmc"
}

do_image_wic[prefuncs] += "cosmos_prepare_wic_seed cosmos_prepare_wic_env"

EXTRACT_PARTITION_LABELS = "bootA bootlogos"

OVERLAYFS_ETC_MOUNT_POINT = "/data"
OVERLAYFS_ETC_DEVICE:centauri-carbon-1 = "/dev/mmcblk0p10"
OVERLAYFS_ETC_DEVICE:centauri-carbon-2 = "/dev/mmcblk0p9"
OVERLAYFS_ETC_FSTYPE ?= "ext4"
OVERLAYFS_ETC_CREATE_MOUNT_DIRS = "0"
OVERLAYFS_ETC_INIT_TEMPLATE = "${FILE_DIRNAME}/files/overlayfs-etc-preinit.sh.in"
