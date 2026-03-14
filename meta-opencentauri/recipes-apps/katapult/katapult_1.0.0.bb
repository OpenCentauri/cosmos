require katapult_${PV}.inc

SUMMARY = "Katapult Bootloader and Flashing Tools"

RDEPENDS:${PN} = " \
    flashtool \
    toolhead-bootloader-upgrade \
    bed-bootloader-upgrade \
"

ALLOW_EMPTY:${PN} = "1"