DESCRIPTION = "OpenCentauri Upgrade Image"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-3.0-only;md5=c79ff39f19dfec6d293b95dea7b07891"

SRC_URI = " \
    file://sw-description \
"

IMAGE_DEPENDS = "opencentauri-image-mmc virtual/kernel u-boot"

SWUPDATE_IMAGES = " \
    opencentauri-image-mmc \
    bootA \
    bootlogos \
    u-boot-sunxi-with-spl \
"

SWUPDATE_IMAGES_FSTYPES[opencentauri-image-mmc] = ".rootfs.squashfs"

SWUPDATE_IMAGES_FSTYPES[bootA] = ".img"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[bootA] = "1"

SWUPDATE_IMAGES_FSTYPES[bootlogos] = ".img"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[bootlogos] = "1"

SWUPDATE_IMAGES_FSTYPES[u-boot-sunxi-with-spl] = ".bin"
SWUPDATE_IMAGES_NOAPPEND_MACHINE[u-boot-sunxi-with-spl] = "1"

SWUPDATE_SIGNING = "RSA"
SWUPDATE_PRIVATE_KEY = "${THISDIR}/files/${MACHINE}/swupdate_private.pem"

inherit swupdate

# CC2: also wrap the plain SWU into the Elegoo OTA bundle (.zip.sig) so stock
# printers accept it. fw version must stay in daemon-000's 02.xx whitelist range.
ELEGOO_OTA_VERSION:elegoo-centauri-carbon2 ?= "02.00.00.00"
ELEGOO_OTA_CLASS:elegoo-centauri-carbon2 ?= "00.00.00.00"
DEPENDS:append:elegoo-centauri-carbon2 = " python3-native openssl-native"

python do_swuimage:append:elegoo-centauri-carbon2() {
    import subprocess
    machine = d.getVar('MACHINE')
    swudeploy = d.getVar('SWUDEPLOYDIR')
    thisdir = d.getVar('THISDIR')
    ver = d.getVar('ELEGOO_OTA_VERSION')
    cls = d.getVar('ELEGOO_OTA_CLASS')
    subprocess.check_call([
        'python3', os.path.join(thisdir, 'files', 'elegoo-ota-pack.py'),
        os.path.join(swudeploy, f'opencentauri-upgrade-{machine}.rootfs.swu'),
        os.path.join(swudeploy, f'cc2_eeb001_{ver}.zip.sig'),
        ver, cls, os.path.join(thisdir, 'files', machine, 'ota'), 'cc2_eeb001'])
}
