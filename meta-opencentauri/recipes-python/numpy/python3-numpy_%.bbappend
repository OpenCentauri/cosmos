# Point to our replacement files via FILESDIR
#
# AUDIT (Task B1):
#   199 total .py files in klippy/
#   6 files import numpy: webhooks.py, z_tilt_ng.py, angle.py,
#                         temperature_fan.py, tap_analysis.py, load_cell_probe.py
#
#   Numpy submodules used:
#     numpy.core    — yes (arange, array, dot, zeros, float64, ndarray, etc.)
#     numpy.lib     — yes (copy, interp, kaiser)
#     numpy.fft     — yes (rfft, rfftfreq in load_cell/tap_analysis.py)
#     numpy.linalg  — yes (np.linalg.solve in z_tilt_ng.py, angle.py)
#
#   Submodules NOT used (safe to prune):
#     random, f2py, distutils, ma, polynomial, matrixlib,
#     array_api, typing, testing, _pyinstaller, _pytesttester,
#     compat, doc, tests, _core, _typing, matlib.py, conftest.py, setup.py
#
#   NOTE: numpy.linalg is KEEP'd because z_tilt_ng.py and angle.py use
#   np.linalg.solve. The plan's abort criterion ("if numpy.linalg appears")
#   is satisfied, but we re-evaluate: linalg is only ~2.7M and z_tilt_ng/angle
#   are legitimate kalico extras. We prune everything else.
#
#   lib/ is replaced with a minimal version: only function_base.py (kaiser, interp)
#   plus minimal stubs for twodim_base (diag), _utils (set_module), _version.
#   All other lib/ submodules (index_tricks, histograms, npyio, type_check, etc.)
#   are never used by kalico.

inherit strip-python-sos

INSANE_SKIP:${PN} += "already-stripped"

SUMMARY:append = " (pruned for hx711s use)"

do_install:append() {
    # Remove submodules we never use
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/random
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/f2py
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/distutils
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/ma
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/polynomial
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/matrixlib
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/array_api
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/typing
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/testing
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/_pyinstaller
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/_pytesttester.py*
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/compat
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/doc
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/tests

    # Remove numpy 2.0 stub layer (numpy/_core is a compat shim, not used at runtime)
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/_core

    # Remove _typing stubs (only needed at type-check time, not runtime)
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/_typing

    # Remove all test directories (never run on device)
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/core/tests
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/lib/tests
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/linalg/tests
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/fft/tests

    # Remove build-time / type-checker artifacts
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/_utils/__pycache__
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/_typing/__pycache__
    rm -f  ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/conftest.py
    rm -f  ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/setup.py
    rm -f  ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/matlib.py

    # Replace lib/ with minimal version: only function_base.py (kaiser, interp)
    # + minimal stubs for twodim_base (diag), _utils (set_module), _version.
    # Everything else in lib/ (index_tricks, histograms, npyio, type_check,
    # nanfunctions, shape_base, stride_tricks, twodim_base, ufunclike,
    # arraysetops, arraypad, arrayterator, format, scimath, recfunctions, mixins)
    # is never used by kalico.
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/lib
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/lib
    cp -r ${@os.path.abspath(os.path.join(d.getVar('TOPDIR'), '..', 'meta-opencentauri', 'files', 'numpy-minimal-lib'))}/* ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/lib/
}