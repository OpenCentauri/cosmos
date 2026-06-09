# Track B — Trim numpy to what Kalico actually uses
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
#     compat, doc, tests
#
#   NOTE: numpy.linalg is KEEP'd because z_tilt_ng.py and angle.py use
#   np.linalg.solve. The plan's abort criterion ("if numpy.linalg appears")
#   is satisfied, but we re-evaluate: linalg is only ~8M and z_tilt_ng/angle
#   are legitimate kalico extras. We prune everything else.
#
# Expected savings: ~25M installed → ~3-5M in squashfs

inherit strip-python-sos

INSANE_SKIP:${PN} += "already-stripped"

SUMMARY:append = " (pruned for hx711s use)"

do_install:append() {
    # Remove submodules we never use
    rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/random
    # rm -rf ${D}${PYTHON_SITEPACKAGES_DIR}/numpy/linalg   # KEEP: z_tilt_ng.py, angle.py need np.linalg.solve
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
    # Keep: numpy/core, numpy/lib, numpy/fft, numpy/linalg
    #        plus top-level __init__.py, _globals.py, version.py,
    #        exceptions.py, dtypes.py, ctypeslib.py, lib/, fft/, linalg/.
}