SUMMARY = "Arbitrary precision decimal floating point arithmetic library"
HOMEPAGE = "https://www.bytereef.org/mpdecimal/"
LICENSE = "BSD-2-Clause"
LIC_FILES_CHKSUM = "file://COPYRIGHT.txt;md5=61bb27059c72d53804ef475f5b18d38f"

SRC_URI = "https://www.bytereef.org/software/mpdecimal/releases/${BP}.tar.gz"
SRC_URI[sha256sum] = "96d33abb4bb0070c7be0fed4246cd38416188325f820468214471938545b1ac8"

inherit autotools

EXTRA_OECONF = "--disable-cxx --disable-doc"

BBCLASSEXTEND = "native nativesdk"
