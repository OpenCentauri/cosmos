require kalico_${PV}.inc
inherit update-rc.d

SUMMARY = "Kalico 3D Printer Firmware"
DESCRIPTION = "Klipper, but Limitless"

SRC_URI += " file://config.mainboard \
    file://klipper-firmware-dsp-init-d \
"

DEPENDS += " gcc-xtensa-hifi4-elf-native"

RPROVIDES:${PN} += "klipper-firmware-dsp"

EXTRA_OEMAKE += " KCONFIG_CONFIG=../config.mainboard"

INITSCRIPT_NAME = "klipper-firmware-dsp"
INITSCRIPT_PARAMS = "defaults 94 4"

INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"

INSANE_SKIP:${PN} = "arch buildpaths"

do_install() {
    install -d ${D}/lib/firmware
    cp -r ${S}/out/klipper.elf ${D}/lib/firmware/rproc-1700000.dsp-fw

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    cp ${UNPACKDIR}/klipper-firmware-dsp-init-d ${D}${sysconfdir}/init.d/klipper-firmware-dsp
    chmod 0755 ${D}${sysconfdir}/init.d/klipper-firmware-dsp
}

FILES:${PN} = " \
    /lib/firmware/rproc-1700000.dsp-fw \
    ${sysconfdir}/init.d/klipper-firmware-dsp \
"
