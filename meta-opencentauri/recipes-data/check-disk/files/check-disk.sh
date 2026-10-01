#!/bin/sh

ALERTS=""
while read -r line; do
    if [ -n "$line" ]; then
        ALERTS="${ALERTS}${line}. "
    fi
done < <(df -P /user-resource /board-resource | awk 'NR>1 {sub(/%/, "", $5); if ($5 > 90) print $6 " is at " $5 "%"}')

if [ -n "$ALERTS" ]; then
	uiprompt "Disk Space Warning" "ALERT: ${ALERTS}"
fi



