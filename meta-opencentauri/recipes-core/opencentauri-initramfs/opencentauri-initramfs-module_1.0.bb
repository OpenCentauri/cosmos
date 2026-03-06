SUMMARY = "OpenCentauri initramfs-framework module for overlay + printer_data boot"
DESCRIPTION = "Initramfs-framework module that mounts UDISK, sets up overlayfs \
over the squashfs rootfs, seeds printer_data on first boot, and bind-mounts it."
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/COPYING.MIT;md5=3da9cfbcb788c80a0384361b4de20420"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "file://opencentauri"

RDEPENDS:${PN} = "initramfs-framework-base initramfs-module-rootfs"

do_install() {
    install -d ${D}/init.d
    install -m 0755 ${WORKDIR}/opencentauri ${D}/init.d/91-opencentauri
}

FILES:${PN} = "/init.d/91-opencentauri"
