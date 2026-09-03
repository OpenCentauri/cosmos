HOMEPAGE = "https://github.com/julianschill/klipper-led_effect"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"
SUMMARY = "LED effects module for Klipper"
DESCRIPTION = "Klipper add-on module providing addressable LED animations and effects."

SRC_URI = "git://github.com/julianschill/klipper-led_effect.git;protocol=https;branch=master"

SRCREV = "266f1049c7172c2fba0da4a52314dcfc0c3bb56f"

S = "${WORKDIR}/git"

RDEPENDS:${PN} = " \
    klipper \
"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}${datadir}/klipper/klippy/extras
    install -m 0644 ${S}/src/led_effect.py ${D}${datadir}/klipper/klippy/extras/led_effect.py
}

FILES:${PN} = " \
    ${datadir}/klipper/klippy/extras/led_effect.py \
"
