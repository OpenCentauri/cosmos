HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/OpenCentauri/kalico.git;protocol=https;branch=paul/hx711-reliability \
    file://0001-remove-save-config-subfile-check.patch \
    file://0002-reduce-calibration-difference-tolerance.patch \
    file://0001-Reduce-log-rotate-threshold.patch \
    file://0001-Don-t-write-tmp-files-to-ramdisk.patch \
    file://0001-implement-pulldown-resistor-in-thermistor-calc.patch \
    file://0001-probe-home-z-includes-z-offset.patch \
    file://0001-force-specific-input-shaper.patch \
    file://0001-Memory-optimisation-in-shaper_calibrate.patch \
    file://0002-persist-set-input-shaper.patch \
    file://0003-adxl345-stream-raw-writes.patch \
"
SRCREV = "9044d16e49295c3c405601a3818c390a9951b213"

PR = "r9"

S = "${WORKDIR}/git"

DEPENDS = " \
    python3-native \
"
