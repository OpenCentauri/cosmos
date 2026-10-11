SUMMARY = "Fast implementation of asyncio event loop on top of libuv"
HOMEPAGE = "https://github.com/MagicStack/uvloop"
DESCRIPTION = "Fast implementation of asyncio event loop on top of libuv"
SECTION = "devel/python"
LICENSE = "Apache-2.0 & MIT"
LIC_FILES_CHKSUM = "file://LICENSE-APACHE;md5=bb92739ddad0a2811957bd98bdb90474 \
                    file://LICENSE-MIT;md5=489c8bc34154e4b59f5c58e664f7d70f"

SRC_URI[sha256sum] = "28d160f51ab4da3b187063652e643dea6831072add4adc1e6d62afbe73b6be27"

inherit pypi python_setuptools_build_meta

DEPENDS += " \
    libuv \
    python3-cython-native \
    python3-packaging-native \
"

PEP517_BUILD_OPTS = "--config-setting=--build-option='build_ext --use-system-libuv'"

BBCLASSEXTEND = "native nativesdk"
