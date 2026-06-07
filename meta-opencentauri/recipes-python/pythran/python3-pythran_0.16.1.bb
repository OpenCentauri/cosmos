SUMMARY = "Ahead of Time compiler for numeric kernels"
HOMEPAGE = "https://pythran.readthedocs.io/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=e277a0b6033e0cb3d510c86b74144b01"

DEPENDS = "python3-gast"

inherit pypi python_setuptools_build_meta

SRC_URI[sha256sum] = "861748c0f9c7d422b32724b114b3817d818ed4eab86c09781aa0a3f7ceabb7f9"

RDEPENDS:${PN} = " \
	python3-beniget \
	python3-gast \
"

BBCLASSEXTEND = "native"
