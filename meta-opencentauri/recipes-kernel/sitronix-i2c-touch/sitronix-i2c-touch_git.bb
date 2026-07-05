SUMMARY = "Sitronix I2C touchscreen driver"
DESCRIPTION = "Out-of-tree kernel driver for Sitronix I2C touch controllers."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://drivers/input/touchscreen/sitronix_ts_i2c.c;beginline=1;endline=16;md5=3fafee9b9a765f6d785c30673e2e7bf6"

inherit module

SRCREV = "e32bedef7897ce2c691a7da252ccf6e342881351"

# ponytail: auto-load so the DT i2c touchscreen node probes before gui-switcher starts
KERNEL_MODULE_AUTOLOAD += "sitronix_ts_i2c"
SRC_URI = "git://github.com/OpenCentauri/sitronix-i2c-touch.git;protocol=https;branch=main"

EXTRA_OEMAKE += "-C ${STAGING_KERNEL_DIR} M=${S}"

MODULES_MODULE_SYMVERS_LOCATION = "."
