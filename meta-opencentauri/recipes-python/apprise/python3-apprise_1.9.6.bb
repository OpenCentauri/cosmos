SUMMARY = "Push Notifications that work with just about every platform!"
LICENSE = "LGPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=74aac17862618045268cd493914a5b51"

inherit pypi python_setuptools_build_meta
SRC_URI[sha256sum] = "4206be9cb5694a3d08dd8e0393bbb9b36212ac3a7769c2633620055e75c6caef"

DEPENDS = " \
	python3-babel-native \
    python3-setuptools-native \
    python3-wheel-native \
"

RDEPENDS:${PN} += " \
    python3-certifi \
    python3-click \
    python3-markdown \
    python3-pyyaml \
    python3-requests \
    python3-requests-oauthlib \
"
