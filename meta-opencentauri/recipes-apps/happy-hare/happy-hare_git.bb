SUMMARY = "Happy Hare MMU Firmware for Klipper (ERCF v2)"
DESCRIPTION = "Multi-Material-Unit firmware extension for Klipper. \
    Supports ERCF v2 with FYSETC ERB V2.0 board. \
    Optimized for low-memory systems (128MB RAM)."
HOMEPAGE = "https://github.com/moggieuk/Happy-Hare"
LICENSE = "CLOSED"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "git://github.com/moggieuk/Happy-Hare.git;protocol=https;branch=main \
    file://mmu \
    file://mmu.cfg.erb-v2 \
    file://mmu_hardware.cfg.erb-v2 \
    file://mmu_parameters.cfg.erb-v2 \
    file://happy-hare-init \
"

SRCREV = "${AUTOREV}"

S = "${WORKDIR}/git"

inherit update-rc.d python3-dir

DEPENDS = " \
    python3-native \
    klipper \
"

RDEPENDS:${PN} = " \
    python3 \
    python3-pyserial \
    python3-jinja2 \
    python3-msgspec \
    klipper \
    moonraker \
    zram \
    bash \
"

INITSCRIPT_NAME = "happy-hare-setup"
INITSCRIPT_PARAMS = "defaults 94 6"

do_configure() {
    :
}

do_compile() {
    :
}

do_install() {
    # Install Happy Hare Python-Module
    install -d ${D}${datadir}/happy-hare

    # Kopiere alle vorhandenen Dateien aus dem Git-Repo
    cp -r ${S}/* ${D}${datadir}/happy-hare/ 2>/dev/null || true

    # Install Klipper-Erweiterung (mmu.py nach klippy/extras)
    install -d ${D}${datadir}/klipper/klippy/extras
    if [ -f ${S}/extras/mmu/mmu.py ]; then
        install -m 0644 ${S}/extras/mmu/mmu.py ${D}${datadir}/klipper/klippy/extras/mmu.py
    fi
    # Install additional Happy Hare extras
    if [ -d ${S}/extras ]; then
        cp -r ${S}/extras/* ${D}${datadir}/klipper/klippy/extras/ 2>/dev/null || true
    fi

    # Install Konfigurations-Vorlagen für FYSETC ERB V2.0
    install -d ${D}${sysconfdir}/klipper/config/happy-hare
    install -m 0644 ${WORKDIR}/mmu.cfg.erb-v2 ${D}${sysconfdir}/klipper/config/happy-hare/mmu.cfg
    install -m 0644 ${WORKDIR}/mmu_hardware.cfg.erb-v2 ${D}${sysconfdir}/klipper/config/happy-hare/mmu_hardware.cfg
    install -m 0644 ${WORKDIR}/mmu_parameters.cfg.erb-v2 ${D}${sysconfdir}/klipper/config/happy-hare/mmu_parameters.cfg

    # Install MMU config from local mmu folder
    install -d ${D}${sysconfdir}/klipper/config/mmu/base
    install -d ${D}${sysconfdir}/klipper/config/mmu/addons
    install -d ${D}${sysconfdir}/klipper/config/mmu/optional
    if [ -d "${WORKDIR}/mmu/base" ]; then
        cp -r ${WORKDIR}/mmu/base/*.cfg ${D}${sysconfdir}/klipper/config/mmu/base/ 2>/dev/null || true
    fi
    if [ -d "${WORKDIR}/mmu/addons" ]; then
        cp -r ${WORKDIR}/mmu/addons/*.cfg ${D}${sysconfdir}/klipper/config/mmu/addons/ 2>/dev/null || true
    fi
    if [ -d "${WORKDIR}/mmu/optional" ]; then
        cp -r ${WORKDIR}/mmu/optional/*.cfg ${D}${sysconfdir}/klipper/config/mmu/optional/ 2>/dev/null || true
    fi
    if [ -f "${WORKDIR}/mmu/mmu_vars.cfg" ]; then
        install -m 0644 ${WORKDIR}/mmu/mmu_vars.cfg ${D}${sysconfdir}/klipper/config/mmu/mmu_vars.cfg
    fi

    # Install Init-Skript für Setup
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/happy-hare-init ${D}${sysconfdir}/init.d/happy-hare-setup

    # Symlink für Moonraker Update-Manager (optional)
    install -d ${D}/home/mainsail
    ln -sf ${datadir}/happy-hare ${D}/home/mainsail/Happy-Hare
}

FILES:${PN} = " \
    ${datadir}/happy-hare \
    ${datadir}/klipper/klippy/extras/mmu.py \
    ${sysconfdir}/klipper/config/happy-hare \
    ${sysconfdir}/klipper/config/mmu \
    ${sysconfdir}/init.d/happy-hare-setup \
    /home/mainsail/Happy-Hare \
"

# Allow Klipper files in non-standard location
FILES:${PN} += "${datadir}/klipper"

# Config files that should be editable (like printer.cfg)
CONFFILES:${PN} = " \
    ${sysconfdir}/klipper/config/mmu/mmu_vars.cfg \
"

# Memory-Optimierung: Strip Python-Bytecode
do_install:append() {
    find ${D} -name '*.pyc' -delete || true
    find ${D} -name '__pycache__' -type d -exec rm -rf {} + || true
}

# Nur auf Centauri Carbon 1 bauen (128MB RAM)
COMPATIBLE_MACHINE = "elegoo-centauri-carbon1"