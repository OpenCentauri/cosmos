require bigtreetech-mms_${PV}.inc

SUMMARY = "BIGTREETECH MMS (Multi-Material System) for Klipper"
DESCRIPTION = "ViViD multi-material system integration for Klipper 3D printer firmware"

RDEPENDS:${PN} = "kalico"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    # Install MMS Python modules into Klipper extras tree
    install -d ${D}${datadir}/klipper/klippy/extras/mms
    cd ${S}/klippy/extras/mms
    find . -type d -exec install -d ${D}${datadir}/klipper/klippy/extras/mms/{} \;
    find . -name '*.py' -exec install -m 0644 {} ${D}${datadir}/klipper/klippy/extras/mms/{} \;

    find ${D} -name '*.pyc' -delete
    find ${D} -name '__pycache__' -type d -exec rm -rf {} + 2>/dev/null || true

    # Install MMS config templates
    install -d ${D}${sysconfdir}/klipper/config/bigtreetech-mms
    cp -r ${S}/config/bigtreetech-mms/* ${D}${sysconfdir}/klipper/config/bigtreetech-mms/

    # Install utility scripts
    install -d ${D}${datadir}/bigtreetech-mms/scripts
    install -m 0755 ${S}/scripts/verify_firmware.py ${D}${datadir}/bigtreetech-mms/scripts/
    install -m 0755 ${S}/scripts/config_editor.py ${D}${datadir}/bigtreetech-mms/scripts/
    install -m 0755 ${S}/scripts/config_opt_enable.py ${D}${datadir}/bigtreetech-mms/scripts/
    install -m 0755 ${S}/scripts/config_val_copy.py ${D}${datadir}/bigtreetech-mms/scripts/
}

FILES:${PN} = " \
    ${datadir}/klipper/klippy/extras/mms \
    ${sysconfdir}/klipper/config/bigtreetech-mms \
    ${datadir}/bigtreetech-mms \
"

CONFFILES:${PN} = " \
    ${sysconfdir}/klipper/config/bigtreetech-mms/mms/mms.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/mms/mms-includes.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/mms/mms-macros.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/hardware/mms-stepper.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/hardware/mms-slot.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/hardware/mms-led.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/hardware/mms-heater.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/hardware/mms-rfid.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/base/mms-motion.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/base/mms-cut.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/base/mms-purge.cfg \
    ${sysconfdir}/klipper/config/bigtreetech-mms/extend/mms-extend.cfg \
"
