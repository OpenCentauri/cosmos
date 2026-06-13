#!/bin/sh
### BEGIN INIT INFO
# Provides:          wifi-power-fix
# Required-Start:    $local_fs $network
# Required-Stop:     
# Default-Start:     2 3 4 5
# Default-Stop:      
# Short-Description: Fix WiFi power management for Centauri Carbon1
# Description:       Disable WiFi power save to prevent disconnect when display sleeps
### END INIT INFO

# Wait for network to be ready
sleep 5

# Disable WiFi power management
if command -v iwconfig >/dev/null 2>&1; then
    iwconfig wlan0 power off 2>/dev/null && echo "WiFi power save disabled"
fi

# Keep WiFi alive - periodic keepalive
(
    while true; do
        sleep 60
        # Check if WiFi is connected
        if ! iwconfig wlan0 2>/dev/null | grep -q "Access Point"; then
            echo "WiFi disconnected, reconnecting..."
            ifconfig wlan0 down
            sleep 2
            ifconfig wlan0 up
            sleep 5
            iwconfig wlan0 power off 2>/dev/null
        fi
        # Ensure power save stays off
        iwconfig wlan0 power off 2>/dev/null
    done
) &

echo "WiFi power management fix started"
