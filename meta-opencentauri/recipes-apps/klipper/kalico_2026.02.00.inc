HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/OpenCentauri/kalico.git;protocol=https;branch=cs1237 \
    file://0001-remove-save-config-subfile-check.patch \
    file://0002-reduce-calibration-difference-tolerance.patch \
    file://0001-Reduce-log-rotate-threshold.patch \
    file://0001-implement-pulldown-resistor-in-thermistor-calc.patch \
    file://0001-probe-home-z-includes-z-offset.patch \
    file://0001-force-specific-input-shaper.patch \
    file://0003-drv8833-motor-control.patch \
    file://0004-allow-config-exclude.patch \
    file://0001-Increase-max-velocity-in-resonance-tester.patch \
    file://0005-stm32-serial-rs485-de-pin.patch \
    file://canvas_rfid.py;subdir=${BP}/klippy/extras \
"
SRCREV = "5e7d9bce5e052c3db682efa961df37727cabc282"

PR = "r16"

DEPENDS = " \
    python3-native \
"
