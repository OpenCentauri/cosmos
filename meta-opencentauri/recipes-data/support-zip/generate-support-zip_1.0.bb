DESCRIPTION = "Support zip generator script"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = "file://generate-support-zip"

S = "${UNPACKDIR}"

inherit allarch

do_install() {
	install -d ${D}${bindir}
	install -m 0755 ${S}/generate-support-zip ${D}${bindir}/generate-support-zip
}

FILES:${PN} = "${bindir}/generate-support-zip"
