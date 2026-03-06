do_install:append() {
    # Remove the default site that has default_server
    rm -f ${D}${sysconfdir}/nginx/sites-enabled/default_server
}
