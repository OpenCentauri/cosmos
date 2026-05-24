require kalico_${PV}.inc
inherit update-rc.d

SUMMARY = "Kalico 3D Printer Firmware"
DESCRIPTION = "Klipper, but Limitless"

SRC_URI += " file://config.mainboard \
    file://klipper-firmware-dsp-init-d \
"

DEPENDS += " gcc-xtensa-hifi4-elf-native"

RPROVIDES:${PN} += "klipper-firmware-dsp"

# Foundation plan H4 fix (2026-05-23): DSP recipe gains a .sha256
# sidecar parallel to bed/toolhead. Closes Class 244 (content-hash
# markers not metadata markers) gap discovered in the foundation-plan
# audit. PR bump forces ipk reinstall on upgrade so the new sidecar
# lands on existing devices. init.d-side verification before remoteproc
# load lands as a follow-up; the sidecar itself is the load-bearing
# tamper-detect substrate.
PR = "r3"

EXTRA_OEMAKE += " KCONFIG_CONFIG=../config.mainboard"

# Remap absolute paths embedded in the DSP firmware ELF so the blob does
# not carry the build host's working directory. Kalico's top-level
# Makefile honors EXTRA_CFLAGS via `CFLAGS += $(EXTRA_CFLAGS)`. gcc's
# -ffile-prefix-map covers DWARF debug paths and __FILE__ expansions in
# one flag. Two mappings needed: the source tree under WORKDIR and the
# native cross-compiler's sysinclude tree under RECIPE_SYSROOT_NATIVE.
export EXTRA_CFLAGS = "-ffile-prefix-map=${WORKDIR}=/build -ffile-prefix-map=${RECIPE_SYSROOT_NATIVE}=/sysroot"

INITSCRIPT_NAME = "klipper-firmware-dsp"
INITSCRIPT_PARAMS = "defaults 94 4"

# FQ2 deferred (2026-05-23): removing INHIBIT_PACKAGE_STRIP +
# INHIBIT_PACKAGE_DEBUG_SPLIT caused bitbake do_package to fail with:
# arm-poky-linux-gnueabi-objcopy: Unable to recognise the format of the
# input file rproc-1700000.dsp-fw
# Root cause: Yocto's do_package splits debug symbols using the TARGET
# toolchain (arm-poky-linux-gnueabi), but rproc-1700000.dsp-fw is Xtensa
# HiFi4 not ARM. Proper fix requires overriding STRIP / OBJCOPY for this
# package to xtensa-hifi4-elf-* OR a custom do_package_split that bypasses
# the per-package debug-info pipeline. Re-deferred until that design lands.
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
INHIBIT_SYSROOT_STRIP = "1"

INSANE_SKIP:${PN} = "arch"

do_install() {
    install -d ${D}/lib/firmware

    # FQ2 v2 (Jack-checkpoint 2026-05-23): strip Xtensa debug symbols.
    # Yocto's default do_package debug-split pipeline uses arm-poky-linux-
    # gnueabi-objcopy which cannot read Xtensa ELF; FQ2 v1 attempt
    # (remove INHIBITs, let Yocto strip) fails. INHIBIT_PACKAGE_STRIP +
    # INHIBIT_PACKAGE_DEBUG_SPLIT stay enabled; we strip manually using
    # the matching Xtensa cross-tool from gcc-xtensa-hifi4-elf-native.
    # --strip-debug removes DWARF (verified ~81.7% size reduction in
    # bench probe 2026-05-23: 428228 -> 78396 bytes) but keeps the
    # symbol table intact so remoteproc symbol resolution at firmware
    # load still works.
    xtensa-nxp_rt700_hifi4_zephyr-elf-strip --strip-debug ${S}/out/klipper.elf

    cp -r ${S}/out/klipper.elf ${D}/lib/firmware/rproc-1700000.dsp-fw

    # .sha256 sidecar: defense-in-depth against on-disk corruption
    # between SWU apply and the next remoteproc load event. Parallel
    # to bed/toolhead pattern. init.d-side verification lands as a
    # follow-up; the sidecar itself is the load-bearing tamper-detect
    # substrate. Closes Class 244 violation per foundation-plan-
    # ensemble-audit 2026-05-23 (H4).
    sha256sum ${D}/lib/firmware/rproc-1700000.dsp-fw \
        | awk '{print $1}' > ${D}/lib/firmware/rproc-1700000.dsp-fw.sha256

    # Install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    cp ${WORKDIR}/klipper-firmware-dsp-init-d ${D}${sysconfdir}/init.d/klipper-firmware-dsp
    chmod 0755 ${D}${sysconfdir}/init.d/klipper-firmware-dsp
}

FILES:${PN} = " \
    /lib/firmware/rproc-1700000.dsp-fw \
    /lib/firmware/rproc-1700000.dsp-fw.sha256 \
    ${sysconfdir}/init.d/klipper-firmware-dsp \
"
