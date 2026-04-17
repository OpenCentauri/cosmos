SUMMARY = "Python bindings for libsodium based on ctypes"
LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://LICENSE;md5=9c8a70e4958da99fa55f2fa83dd35c8d"

inherit pypi python_poetry_core
SRC_URI[sha256sum] = "f3418da7df29e6d9b11fd7d990289d16397dc1020e4e35192e11aee826922860"

RDEPENDS:${PN} += " \
    libsodium-dev \
"

INSANE_SKIP:${PN} += "dev-deps"
