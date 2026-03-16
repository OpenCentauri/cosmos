inherit cargo update-rc.d

SUMMARY = "Camera LED Bridge"
DESCRIPTION = "Control camera LED based on GPIO input"
HOMEPAGE = "https://github.com/OpenCentauri/OpenCentauri"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://../LICENSE;md5=0a18a528575a965515cdd877f88b3c4c"

SRC_URI = "git://github.com/OpenCentauri/OpenCentauri.git;protocol=https;branch=main \
    crate://crates.io/bitflags/2.11.0 \
    crate://crates.io/cfg-if/1.0.4 \
    crate://crates.io/cfg_aliases/0.2.1 \
    crate://crates.io/libc/0.2.183 \
    crate://crates.io/nix/0.31.2 \
    file://camera-led-bridge.init \
"
SRCREV = "28296f0f9107e8f0723b22176a0ae8bc66390336"

SRC_URI[bitflags-2.11.0.sha256sum] = "843867be96c8daad0d758b57df9392b6d8d271134fce549de6ce169ff98a92af"
SRC_URI[cfg-if-1.0.4.sha256sum] = "9330f8b2ff13f34540b44e946ef35111825727b38d33286ef986142615121801"
SRC_URI[cfg_aliases-0.2.1.sha256sum] = "613afe47fcd5fac7ccf1db93babcb082c5994d996f20b8b159f2ad1658eb5724"
SRC_URI[libc-0.2.183.sha256sum] = "b5b646652bf6661599e1da8901b3b9522896f01e736bad5f723fe7a3a27f899d"
SRC_URI[nix-0.31.2.sha256sum] = "5d6d0705320c1e6ba1d912b5e37cf18071b6c2e9b7fa8215a1e8a7651966f5d3"

S = "${WORKDIR}/git/camera-led-bridge"

INITSCRIPT_NAME = "camera-led-bridge"
INITSCRIPT_PARAMS = "defaults 96 4"

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/camera-led-bridge.init ${D}${sysconfdir}/init.d/camera-led-bridge
}
