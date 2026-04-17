# bbappend for mainsail - allows installing only config without frontend
# Used by minimal image which needs mainsail.cfg macros but no web interface

PACKAGECONFIG ??= ""
PACKAGECONFIG[config-only] = ",,,"

# When config-only is enabled, skip frontend installation
do_install:append() {
    if ${@bb.utils.contains('PACKAGECONFIG', 'config-only', 'true', 'false', d)}; then
        # Remove frontend files, keep only config
        rm -rf ${D}/var/www/mainsail
    fi
}

# Adjust FILES when config-only is enabled
FILES:${PN}:remove = "${@bb.utils.contains('PACKAGECONFIG', 'config-only', '/var/www/mainsail', '', d)}"