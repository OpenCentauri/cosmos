#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE="cosmos-builder"
TARGET="${1:-opencentauri-upgrade}"

if ! docker image inspect "${IMAGE}" &>/dev/null; then
    docker build -f "${SCRIPT_DIR}/Dockerfile.build" -t "${IMAGE}" "${SCRIPT_DIR}"
fi

docker run \
    -v "${SCRIPT_DIR}:/cosmos" \
    -w /cosmos \
    "${IMAGE}" \
    bash -c "source poky/oe-init-build-env build && bitbake ${TARGET}"
