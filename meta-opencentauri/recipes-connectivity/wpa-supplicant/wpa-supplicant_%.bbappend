# helixscreen (and guppyscreen-style screens) link against libwpa_client.a
# for WiFi control via wpa_ctrl. Yocto defaults DISABLE_STATIC=" --disable-static",
# which skips the libwpa_client.a build in upstream wpa-supplicant_2.10.bb.
# Clear it so the static library and the wpa_ctrl.h header both get installed
# into ${D}${libdir} / ${D}${includedir}.
DISABLE_STATIC = ""
