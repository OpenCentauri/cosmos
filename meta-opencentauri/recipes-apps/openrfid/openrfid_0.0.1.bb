SUMMARY = "OpenRFID filament tag decoders"
DESCRIPTION = "Filament spool RFID tag processors"
HOMEPAGE = "https://github.com/suchmememanyskill/OpenRFID"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

SRC_URI = "git://github.com/suchmememanyskill/OpenRFID.git;protocol=https;branch=main"
SRCREV = "1a6f605d0334157b532afdd14f89fc182d9000f6"
PR = "r1"

inherit allarch

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}${datadir}/openrfid
    cp -r ${S}/src/* ${D}${datadir}/openrfid/
}

FILES:${PN} = "${datadir}/openrfid"

RDEPENDS:${PN} = " \
    python3-core \
    python3-crypt \
    python3-datetime \
    python3-json \
    python3-logging \
    python3-netclient \
"
