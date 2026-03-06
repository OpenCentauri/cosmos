#!/bin/sh
# /usr/local/bin/usb-mount.sh
# Called by udev to mount/unmount USB storage partitions

case "$1" in
    add)
        MOUNT_POINT="/mnt/usb/$2"
        mkdir -p "$MOUNT_POINT"
        mount "/dev/$2" "$MOUNT_POINT" -o rw,noexec,nosuid,nodev
        ;;
    remove)
        MOUNT_POINT="/mnt/usb/$2"
        umount "$MOUNT_POINT" 2>/dev/null
        rmdir "$MOUNT_POINT" 2>/dev/null
        ;;
    *)
        echo "Usage: $0 {add|remove} <device>"
        exit 1
        ;;
esac
