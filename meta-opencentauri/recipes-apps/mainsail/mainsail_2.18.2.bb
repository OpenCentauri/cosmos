SUMMARY = "Mainsail - Web Interface for Klipper"
DESCRIPTION = "Mainsail is the popular web interface for managing and \
    controlling 3D printers with Klipper."
HOMEPAGE = "https://github.com/mainsail-crew/mainsail"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://index.html;md5=e041ea1952fb60d9e673d6c3f4d16802"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

SRC_URI = "https://github.com/mainsail-crew/mainsail/releases/download/v${PV}/mainsail.zip;subdir=mainsail"
SRC_URI[sha256sum] = "df2ba7c301f7bfc8ac9f122741a6ba08356d679ecfa1f62f898d0337802d5de5"

S = "${WORKDIR}/mainsail"

PR = "r1"

RDEPENDS:${PN} = " \
    klipper \
    moonraker \
    mainsail-panel-extender \
"

do_configure() {
    :
}

do_compile() {
    :
}

do_install() {
    # Install static web files
    install -d ${D}/var/www/mainsail
    cp -r ${S}/* ${D}/var/www/mainsail/

    # Inject the mainsail-panel-extender panel loader (served by moonraker)
    sed -i 's|</head>|<script src="/addons/loader.js" defer></script></head>|' \
        ${D}/var/www/mainsail/index.html

    # Bump the service worker precache revision for index.html, otherwise
    # browsers that cached the unpatched page never fetch the new one
    NEW_REV=$(md5sum ${D}/var/www/mainsail/index.html | cut -d' ' -f1)
    sed -i "s|{url:\"index.html\",revision:\"[0-9a-f]*\"}|{url:\"index.html\",revision:\"$NEW_REV\"}|" \
        ${D}/var/www/mainsail/sw.js
}

FILES:${PN} = " \
    /var/www/mainsail \
"