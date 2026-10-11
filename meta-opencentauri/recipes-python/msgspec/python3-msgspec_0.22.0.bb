SUMMARY = "A fast serialization and validation library, with builtin support for JSON, MessagePack, YAML, and TOML."
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=c21402f8022478021f697044388b97c4"

inherit pypi python_setuptools_build_meta
SRC_URI[sha256sum] = "0a13624a4969159fe35d8c2a3d377b2b61bbd8585e327440d5e52725affcce38"

DEPENDS += "python3-setuptools-scm-native"

RDEPENDS:${PN} = " \
	python3-pyyaml \
	python3-tomli \
	python3-tomli-w \
"
