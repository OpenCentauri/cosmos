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
heatsoak=$(get_config_value heatsoak)
adaptive_mesh=$(get_config_value adaptive_mesh)
adaptive_purge=$(get_config_value adaptive_purge)
full_calibrate_hotend_temperature=$(get_config_value full_calibrate_hotend_temperature)
full_calibrate_bed_temperature=$(get_config_value full_calibrate_bed_temperature)
bypass_calibration=$(get_config_value bypass_calibration)

mkdir -p "${OUTPUT_DIR}"

cat > "${OUTPUT_FILE}" <<EOF
# Generated on startup from cosmos.conf

[gcode_macro _COSMOS_SETTINGS]
variable_sync_camera_led_to_chamber_led: ${sync_camera_led_to_chamber_led}
variable_camera_led_default_on: ${camera_led_default_on}
variable_heatsoak: ${heatsoak}
variable_adaptive_mesh: ${adaptive_mesh}
variable_adaptive_purge: ${adaptive_purge}
variable_full_calibrate_hotend_temperature: ${full_calibrate_hotend_temperature}
variable_full_calibrate_bed_temperature: ${full_calibrate_bed_temperature}
variable_bypass_calibration: ${bypass_calibration}
gcode:

EOF

