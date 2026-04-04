#!/bin/sh

/etc/init.d/gui-switcher stop
dd if=/dev/zero of=/dev/fb0
openvt -s -w -- fbdoom -iwad /usr/share/doom/doom1.wad
dd if=/dev/zero of=/dev/fb0
/etc/init.d/gui-switcher start