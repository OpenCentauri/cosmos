HOMEPAGE = "https://github.com/plastikman/dragonbreath-klipper"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=ff1231f0087400bc406945200e2f0ef0"
SUMMARY = "dragonbreath-klipper"
DESCRIPTION = "Klipper module for the DragonBreath-firmware Panda Breath chamber heater."

SRC_URI = "git://github.com/plastikman/dragonbreath-klipper.git;protocol=https;branch=main"

# Pinned deliberately. The device firmware owns all heater policy, so bump this
# only after testing the module against a current DragonBreath release.
# Requires API v2: device firmware 1.0.0 or newer, unchanged through v1.1.10.
SRCREV = "c5f531b656599e3574571e534c83ec39a78de0b5"

S = "${WORKDIR}/git"

RDEPENDS:${PN} = " \
    klipper \
"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    # Module only. The config the user adds to printer.cfg to enable it is
    # documented rather than shipped, so nothing here can activate itself.
    install -d ${D}${datadir}/klipper/klippy/extras
    install -m 0644 ${S}/dragonbreath.py ${D}${datadir}/klipper/klippy/extras/dragonbreath.py
}

FILES:${PN} = " \
    ${datadir}/klipper/klippy/extras/dragonbreath.py \
"
