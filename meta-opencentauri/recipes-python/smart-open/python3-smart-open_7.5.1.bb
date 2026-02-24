SUMMARY = "Utils for streaming large files (S3, HDFS, GCS, SFTP, Azure Blob Storage, gzip, bz2, zst...)"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://LICENSE;md5=5c68cd40b6115d100370f3dcee3924cb"

PYPI_PACKAGE = "smart_open"

inherit pypi python_setuptools_build_meta
SRC_URI[sha256sum] = "3f08e16827c4733699e6b2cc40328a3568f900cb12ad9a3ad233ba6c872d9fe7"

DEPENDS += " \
    python3-setuptools-scm-native \
    python3-setuptools-native \
    python3-wheel-native \
"
