#!/bin/sh
set -eu

src=/opt/usr/wifi/wpa_supplicant.conf
dest=/mnt/exUDISK/wpa_supplicant.conf

# A printer without saved WiFi credentials can still be upgraded.
[ -f "$src" ] || exit 0

# Keep the first SSID and PSK lines, preserving quotes and escapes.
ssid=$(awk '/^[ \t]*ssid[ \t]*=/ { print; exit }' "$src")
psk=$(awk '/^[ \t]*psk[ \t]*=/ { print; exit }' "$src")

if [ -z "$ssid" ]; then
    echo "No saved SSID; skipping WiFi export."
    exit 0
fi

umask 077
cat > "$dest" <<EOF
ctrl_interface=/var/run/wpa_supplicant
update_config=1
network={
$ssid
$psk
}
EOF

sync && sync
