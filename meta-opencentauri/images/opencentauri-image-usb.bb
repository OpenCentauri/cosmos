require opencentauri-image-base.bb

DESCRIPTION = "OpenCentauri eMMC Image"
LICENSE = "GPL-3.0-only"

# Tinkerer build: runtime opkg installs + bundled extras (fbdoom etc).
IMAGE_FEATURES += "package-management image-extras"

WKS_FILES = "opencentauri-usb-image.wks.in"
