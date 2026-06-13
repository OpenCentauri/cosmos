#!/bin/sh
# Disable WiFi Power Management for Centauri Carbon1
# Prevents WiFi disconnect when display sleeps/wakes

# Create network config script
cat > /etc/network/if-pre-up.d/wifi-powersave << 'EOF'
#!/bin/sh
# Disable WiFi power saving
if [ -n "$IWCONFIG" ]; then
    iwconfig "$IFACE" power off 2>/dev/null || true
fi
EOF

chmod +x /etc/network/if-pre-up.d/wifi-powersave

# Also create a systemd service to ensure it runs at boot
cat > /etc/systemd/system/wifi-powersave-off.service << 'EOF'
[Unit]
Description=Disable WiFi Power Management
After=network.target

[Service]
Type=oneshot
ExecStart=/sbin/iwconfig wlan0 power off
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF

# Enable the service
ln -sf /etc/systemd/system/wifi-powersave-off.service /etc/systemd/system/multi-user.target.wants/wifi-powersave-off.service 2>/dev/null || true

# Set persistent WiFi config
cat >> /etc/network/interfaces.d/wlan0 << 'EOF'
# Disable power management
wireless-power off
EOF

echo "WiFi Power Management disabled successfully"
