PV = "2.12.0"
SRCREV = "3e9dfa2866941655c56877882565e7577de6fc7b"
SRC_URI:remove = "file://0001-Do-not-strip-binaries.patch"

FILESEXTRAPATHS:prepend := "${THISDIR}/${BPN}:"

SRC_URI:append:class-native = " file://avoid-host-includes.patch"
