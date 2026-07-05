SUMMARY = "Sitronix I2C touchscreen driver"
DESCRIPTION = "Out-of-tree kernel driver for Sitronix I2C touch controllers."
LICENSE = "GPL-2.0-only"

inherit module

SRCREV = "1a1a7dee1f4f51c0328321ae2b1a87db061ebc93"

# auto-load so the DT i2c touchscreen node probes before gui-switcher starts
KERNEL_MODULE_AUTOLOAD += "sitronix_ts_i2c"
SRC_URI = "git://github.com/OpenCentauri/sitronix-i2c-touch.git;protocol=https;branch=main"

LIC_FILES_CHKSUM = "file://drivers/input/touchscreen/sitronix_ts_i2c.c;beginline=1;endline=16;md5=f143033daf9191787c1015051dfac1a7"

EXTRA_OEMAKE += "-C ${STAGING_KERNEL_DIR} M=${S}"

MODULES_MODULE_SYMVERS_LOCATION = "."
