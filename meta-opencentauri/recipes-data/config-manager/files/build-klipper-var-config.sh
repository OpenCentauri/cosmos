#!/bin/sh

set -eu

CONFIG_MANAGER="/usr/bin/config-manager"
OUTPUT_DIR="/etc/klipper/config/klipper-readonly"
OUTPUT_FILE="${OUTPUT_DIR}/_vars.cfg"

get_config_value() {
	"${CONFIG_MANAGER}" klipper "$1"
}

sync_camera_led_to_chamber_led=$(get_config_value sync_camera_led_to_chamber_led)
camera_led_default_on=$(get_config_value camera_led_default_on)

mkdir -p "${OUTPUT_DIR}"

cat > "${OUTPUT_FILE}" <<EOF
# Generated on startup from cosmos.conf

[gcode_macro _COSMOS_SETTINGS]
variable_sync_camera_led_to_chamber_led: ${sync_camera_led_to_chamber_led}
variable_camera_led_default_on: ${camera_led_default_on}
gcode:

EOF

