# Strip debug info from every .so under site-packages.
# Drops ~30% off the numpy/scipy footprint with no runtime cost.
do_install:append() {
    if [ -d ${D}${PYTHON_SITEPACKAGES_DIR} ]; then
        find ${D}${PYTHON_SITEPACKAGES_DIR} -name '*.so' -type f \
            -exec ${HOST_PREFIX}strip --strip-unneeded {} \;
        find ${D}${PYTHON_SITEPACKAGES_DIR} -name '*.so' -type f \
            -exec ${TARGET_PREFIX}strip --strip-unneeded {} \;
    fi
}
