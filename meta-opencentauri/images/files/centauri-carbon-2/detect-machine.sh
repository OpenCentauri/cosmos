#!/bin/sh

versions=/opt/inst/firmware_version/versions.json
if grep -q 'Centauri Carbon 2' "$versions"; then
    machine=centauri-carbon-2
elif grep -q 'Centauri 2' "$versions"; then
    machine=centauri-2
else
    exit 0
fi

fw_setenv machine_type "$machine"
sync && sync

exit 0
