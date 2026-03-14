require katapult_${PV}.inc

SUMMARY = "Katapult Bootloader and Flashing Tools"

RDEPENDS:${PN} = " \
    flashtool \
    toolhead-bootloader-upgrade \
    bed-bootloader-upgrade \
"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

ALLOW_EMPTY:${PN} = "1"