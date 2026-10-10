SECTION = "kernel"
DESCRIPTION = "Mainline Longterm Linux kernel"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"
COMPATIBLE_MACHINE = "(sun4i|sun5i|sun7i|sun8i|sun9i|sun50i)"

inherit kernel

require recipes-kernel/linux/linux.inc

LINUX_VERSION = "${PV}"

KBRANCH = "linux-7.2.y"

KMETA_AUDIT = ""

# Pull in the devicetree files into the rootfs
RDEPENDS_${KERNEL_PACKAGE_NAME}-base += "kernel-devicetree"

KERNEL_EXTRA_ARGS += "LOADADDR=${UBOOT_ENTRYPOINT}"

S = "${UNPACKDIR}/${BP}"

SRC_URI = " \
    git://git.kernel.org/pub/scm/linux/kernel/git/stable/linux.git;protocol=https;branch=${KBRANCH} \
    file://defconfig \
"

# v${PV}
SRCREV = "bd50482a9c7a6eb91468f1064a346a75e677e692"

FILES_${KERNEL_PACKAGE_NAME}-base:append = " ${nonarch_base_libdir}/modules/${KERNEL_VERSION}/modules.builtin.modinfo"
