INHIBIT_DEFAULT_DEPS = "1"

FILES:${PN} = "${libexecdir} ${bindir}"

do_install() {
    install -d ${D}${bindir} ${D}${libexecdir}/${BP}/
    cp -r ${S}/. ${D}${libexecdir}/${BP}

    # Symlink all executables into bindir
    for f in ${D}${libexecdir}/${BP}/bin/*; do
        ln -rs $f ${D}${bindir}/$(basename $f)
    done
}

INSANE_SKIP:${PN} = "already-stripped libdir staticdev file-rdeps arch dev-so"

INHIBIT_SYSROOT_STRIP = "1"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"

# Need to mark these as private until do_package's soname-finder only looks in $libdir
# PRIVATE_LIBS = "libgcc_s.so.1 libstdc++.so.6"

BBCLASSEXTEND = "native nativesdk"

# Skipping file deps - we don't control the dependencies for prebuilt libraries, resulting in
# nothing provides libcrypt.so.1()(64bit) needed by nativesdk-gcc-xtensa-hifi4-elf
# when packaged as RPM for SDK.
SKIP_FILEDEPS = "1"

COMPATIBLE_HOST = "(x86_64|aarch64).*-linux"

SUMMARY = "Xtensa Zephyr Toolchain - hifi4 bare-metal target (xtensa-nxp_rt700_hifi4_zephyr)"
LICENSE = "GPL-3.0-with-GCC-exception & GPL-3.0-only"

LIC_FILES_CHKSUM:aarch64 = "file://share/licenses/gcc/COPYING;md5=59530bdf33659b29e73d4adb9f9f6552"
LIC_FILES_CHKSUM:x86-64 = "file://share/licenses/gcc/COPYING;md5=59530bdf33659b29e73d4adb9f9f6552"

SRC_URI = "https://github.com/zephyrproject-rtos/sdk-ng/releases/download/v${PV}/toolchain_linux-${HOST_ARCH}_xtensa-nxp_rt700_hifi4_zephyr-elf.tar.xz;name=gcc-${HOST_ARCH}"
SRC_URI[gcc-aarch64.sha256sum] = "857ffd120af6fee573afbc7f29ec0519d2643a9613588c70eb9f6f3488bad1b9"
SRC_URI[gcc-x86_64.sha256sum] = "8dbd260061e3fb05696bb19f026f6eeeb965eb37c0113a63bbfc040836b612c5"

S = "${UNPACKDIR}/xtensa-nxp_rt700_hifi4_zephyr-elf"
