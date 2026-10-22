HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/KalicoCrew/kalico.git;protocol=https;branch=main \
    file://0001-remove-save-config-subfile-check.patch \
    file://0002-reduce-calibration-difference-tolerance.patch \
    file://0001-Reduce-log-rotate-threshold.patch \
    file://0001-implement-pulldown-resistor-in-thermistor-calc.patch \
    file://0001-probe-home-z-includes-z-offset.patch \
    file://0001-force-specific-input-shaper.patch \
    file://0003-drv8833-motor-control.patch \
    file://0004-allow-config-exclude.patch \
    file://0005-stm32-serial-rs485-de-pin.patch \
    file://0006-Add-support-for-hifi4.patch \
    file://0007-Add-HX711s-load-cell-probe-driver.patch \
    file://canvas_rfid.py;subdir=${BP}/klippy/extras \
    file://canvas_filament_sensor.py;subdir=${BP}/klippy/extras \
"
SRCREV = "5b5942799ba33e50cf2b67b5258acff7214b3001"

PR = "r19"

DEPENDS = " \
    python3-native \
"
