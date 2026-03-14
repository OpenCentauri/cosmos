require katapult.inc

SUMMARY = "Katapult Flash Tool"
DESCRIPTION = "Flash tool script from the Katapult bootloader project for flashing MCU firmware."

RDEPENDS:${PN} = " \
    python3 \
    python3-pyserial \
"

do_install() {
    install -d ${D}${base_sbindir}
    install -m 0755 ${S}/scripts/flashtool.py ${D}${base_sbindir}/flashtool
}

FILES:${PN} = "${base_sbindir}/flashtool"
