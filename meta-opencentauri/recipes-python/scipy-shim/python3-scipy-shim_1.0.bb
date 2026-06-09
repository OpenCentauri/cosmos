SUMMARY = "Drop-in scipy replacement — pure-Python/numpy implementation of scipy.signal functions for Kalico's hx711s tap filter"
HOMEPAGE = "https://github.com/OpenCentauri/cosmos"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=5f477c3073ea2d02a70a764319f5f873"

SRC_URI += "file://scipy"
SRC_URI += "file://tests"

inherit python3-dir setuptools3

do_install:append() {
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    cp -r ${WORKDIR}/scipy ${D}${PYTHON_SITEPACKAGES_DIR}/
}

RDEPENDS:${PN} = "python3-numpy"

BBCLASSEXTEND = "native"