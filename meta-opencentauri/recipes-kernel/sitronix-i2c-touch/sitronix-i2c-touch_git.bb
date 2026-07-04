SUMMARY = "Sitronix I2C touchscreen driver"
DESCRIPTION = "Out-of-tree kernel driver for Sitronix I2C touch controllers."
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://drivers/input/touchscreen/sitronix_ts_i2c.c;beginline=1;endline=16;md5=989cf65ee1592eae4e00b7638769a387"

inherit module

SRCREV = "da63b6f9578001ab2456bbf59066e893ea6ca3f1"
SRC_URI = "git://github.com/OpenCentauri/sitronix-i2c-touch.git;protocol=https;branch=main"

EXTRA_OEMAKE += "-C ${STAGING_KERNEL_DIR} M=${S} modules"

MODULES_MODULE_SYMVERS_LOCATION = "."
