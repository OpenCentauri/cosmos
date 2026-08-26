# Config manager variant for minimal-display image (guppyscreen)
PACKAGECONFIG[minimal-display] = "minimal-display"

SRC_URI:append = " ${@bb.utils.contains('PACKAGECONFIG', 'minimal-display', 'file://default-hh-minimal-display.conf', '', d)}"

do_install:append() {
    if ${@bb.utils.contains('PACKAGECONFIG', 'minimal-display', 'true', 'false', d)}; then
        install -m 0644 ${WORKDIR}/default-hh-minimal-display.conf ${D}${sysconfdir}/klipper/config/cosmos.conf
        install -m 0644 ${WORKDIR}/default-hh-minimal-display.conf ${D}${datadir}/config-manager/default.conf
    fi
}