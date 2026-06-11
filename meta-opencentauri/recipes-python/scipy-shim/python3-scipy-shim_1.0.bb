SUMMARY = "Drop-in scipy replacement — pure-Python/numpy implementation of scipy.signal functions for Kalico's hx711s tap filter"
HOMEPAGE = "https://github.com/OpenCentauri/cosmos"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/BSD-3-Clause;md5=550794465ba0ec5312d6919e203a55f9"

SRC_URI += "file://scipy"

inherit python3-dir

# No do_compile needed — pure Python files, just install them.
do_compile[noexec] = "1"

do_install:append() {
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}
    cp -r ${WORKDIR}/scipy ${D}${PYTHON_SITEPACKAGES_DIR}/
    # Remove tests and pycache from the installed package
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/scipy/tests
    find ${D}${PYTHON_SITEPACKAGES_DIR}/scipy -name '__pycache__' -type d -exec rm -rf {} + 2>/dev/null || true
    find ${D}${PYTHON_SITEPACKAGES_DIR}/scipy -name '*.pyc' -delete 2>/dev/null || true
}

FILES:${PN} = "${PYTHON_SITEPACKAGES_DIR}/scipy"

RDEPENDS:${PN} = "python3-numpy"

BBCLASSEXTEND = "native"