SUMMARY = "A static analyzer for Python2 and Python3 code"
HOMEPAGE = "https://github.com/serge-sans-paille/beniget/"
LICENSE = "BSD-3-Clause"
LIC_FILES_CHKSUM = "file://LICENSE;md5=02550c296a72ab0b70961eb70a5a7242"

inherit pypi setuptools3

SRC_URI[sha256sum] = "a0258537e65e7e14ec33a86802f865a667f949bb6c73646d55e42f7c45a052ae"

BBCLASSEXTEND = "native"
