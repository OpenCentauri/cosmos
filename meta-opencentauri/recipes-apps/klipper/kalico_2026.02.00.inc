HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/OpenCentauri/kalico.git;protocol=https;branch=sharespace-sram \
    file://0001-remove-save-config-subfile-check.patch \
    file://0002-reduce-calibration-difference-tolerance.patch \
    file://0001-Reduce-log-rotate-threshold.patch \
    file://0001-Don-t-write-tmp-files-to-ramdisk.patch \
    file://0001-implement-pulldown-resistor-in-thermistor-calc.patch \
    file://0001-probe-home-z-includes-z-offset.patch \
    file://0001-force-specific-input-shaper.patch \
"
SRCREV = "b6e69c825466ff5ed0f6ce1f0a8e303f2137592f"

PR = "r8"

DEPENDS = " \
    python3-native \
"
