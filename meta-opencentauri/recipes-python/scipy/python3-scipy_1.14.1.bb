SUMMARY = "SciPy: Scientific Library for Python"
HOMEPAGE = "https://www.scipy.org"
LICENSE = "BSD-3-Clause & BSD-2-Clause & MIT & BSL-1.0 & Qhull & BSD-3-Clause-LBNL"
LIC_FILES_CHKSUM = " \
	file://LICENSE.txt;md5=5f477c3073ea2d02a70a764319f5f873 \
	file://LICENSES_bundled.txt;md5=339da992791ab835a9b0c61a6b958ea0 \
"

inherit pkgconfig pypi python_mesonpy

SRC_URI[sha256sum] = "5a275584e726026a5699459aa72f828a610821006228e841b94275c4a7c08417"

SRC_URI += " \
	file://use-unversioned-numpy.patch \
	file://0001-Conditionally-import-C-modules.patch \
"

DEPENDS += " \
	python3-numpy-native \
	python3-pybind11-native \
	python3-pythran-native \
	python3-gast-native \
	python3-beniget-native \
	python3-ply-native \
	xsimd \
"

DEPENDS:append:class-target = " \
	python3-scipy-native \
	python3-numpy \
"

RDEPENDS:${PN} += " \
	python3-numpy \
"

PACKAGECONFIG ?= "openblas pythran"

PACKAGECONFIG[openblas] = "-Dblas=openblas -Dlapack=openblas,,openblas,openblas"
PACKAGECONFIG[lapack] = "-Dblas=lapack -Dlapack=lapack,,lapack,lapack"
PACKAGECONFIG[f77] = "-Duse-g77-abi=true,,,"
PACKAGECONFIG[pythran] = "-Duse-pythran=true,-Duse-pythran=false,python3-pythran-native python3-pythran,python3-pythran"

CLEANBROKEN = "1"

export LAPACK = "${STAGING_LIBDIR}"
export BLAS = "${STAGING_LIBDIR}"

F90:class-native = "${FC}"
F90:class-target = "${TARGET_PREFIX}gfortran"
export F90

export F77 = "${TARGET_PREFIX}gfortran"

BBCLASSEXTEND = "native nativesdk"

do_configure:append:class-target() {
	if ! grep -q "numpy-include-dir" "${WORKDIR}/meson.cross" ; then
		sed -i "s,\[properties\],\[properties\]\nnumpy-include-dir = \'${RECIPE_SYSROOT}${PYTHON_SITEPACKAGES_DIR}/numpy/core/include/\' ,g" "${WORKDIR}/meson.cross"
	fi
}
