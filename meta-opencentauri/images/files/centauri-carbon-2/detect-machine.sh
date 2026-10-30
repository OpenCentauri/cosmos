#!/bin/sh
set -eu

versions=/opt/inst/firmware_version/versions.json
if grep -q 'Centauri Carbon 2' "$versions"; then
    machine=centauri-carbon-2
elif grep -q 'Centauri 2' "$versions"; then
    machine=centauri-2
else
    echo "Unable to detect the printer model from $versions" >&2
    exit 1
fi

fw_setenv machine_type "$machine"
sync && sync
