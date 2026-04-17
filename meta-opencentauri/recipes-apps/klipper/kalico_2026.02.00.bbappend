FILESEXTRAPATHS:prepend := "${THISDIR}/files-hh:"

# PACKAGECONFIG for OpenCentauri HH images
# opencentauri-hh-full: macros.cfg only (original shell.cfg from files/ used)
# opencentauri-hh-minimal: macros.cfg + minimal shell.cfg (camera/doom removed)
PACKAGECONFIG ??= ""
PACKAGECONFIG[opencentauri-hh-full] = ",,,"
PACKAGECONFIG[opencentauri-hh-minimal] = ",,,"

# Use modified macros.cfg for both HH images
SRC_URI:append = "${@bb.utils.contains_any('PACKAGECONFIG', 'opencentauri-hh-full opencentauri-hh-minimal', ' file://macros.cfg', '', d)}"

# Use minimal shell.cfg only for HH-Minimal (removes camera/doom commands)
SRC_URI:append = "${@bb.utils.contains('PACKAGECONFIG', 'opencentauri-hh-minimal', ' file://shell.cfg', '', d)}"