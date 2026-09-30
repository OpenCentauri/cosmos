#!/bin/sh

MOONRAKER_URL="http://localhost/printer/gcode/script"

ALERTS=""
while read -r line; do
    if [ -n "$line" ]; then
        ALERTS="${ALERTS}${line}. "
    fi
done < <(df -P /user-resource /board-resource | awk 'NR>1 {sub(/%/, "", $5); if ($5 > 90) print $6 " is at " $5 "%"}')

if [ -n "$ALERTS" ]; then
    MACRO_CALL="_DISK_WARN_DIALOG MSG=\"ALERT: ${ALERTS}\""

    ENCODED_CALL=$(echo "$MACRO_CALL" | sed 's/ /%20/g')

    curl -s -X POST "${MOONRAKER_URL}?script=${ENCODED_CALL}" > /dev/null
fi



