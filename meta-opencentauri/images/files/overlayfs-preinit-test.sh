#!/bin/sh
# Host smoke test for the init.d overlay-cleanup block inside
# overlayfs-etc-preinit.sh.in. Renders the template like overlayfs-etc.bbclass
# does, extracts the sentinel-marked block, and runs it against a fake
# upper/lower pair. mknod cases skip without CAP_MKNOD.
set -u
HERE=$(cd "$(dirname "$0")" && pwd)
TEMPLATE="$HERE/overlayfs-etc-preinit.sh.in"
ROOT=$(mktemp -d "${TMPDIR:-/tmp}/overlay-test.XXXXXX")
FAILED=0
trap 'rm -rf "$ROOT"' EXIT

RENDERED="$ROOT/preinit.sh"
python3 - "$TEMPLATE" > "$RENDERED" <<'EOF'
import sys
args = {
    'OVERLAYFS_ETC_MOUNT_POINT': '/data',
    'OVERLAYFS_ETC_MOUNT_OPTIONS': 'defaults',
    'OVERLAYFS_ETC_FSTYPE': 'ext4',
    'OVERLAYFS_ETC_DEVICE': '/dev/mmcblk0p10',
    'SBIN_INIT_NAME': '/sbin/init.orig',
    'OVERLAYFS_ETC_EXPOSE_LOWER': 'false',
    'CREATE_MOUNT_DIRS': 'false',
}
sys.stdout.write(open(sys.argv[1]).read().format(**args))
EOF

# Extract exactly the block that ships, between the sentinel comments.
sed -n '/# init.d overlay cleanup: BEGIN/,/# init.d overlay cleanup: END/p' \
    "$RENDERED" > "$ROOT/block.sh"
grep -q 'LOWER_INITD' "$ROOT/block.sh" || { echo "FAIL: block extraction empty"; exit 1; }

UPPER="$ROOT/upper"
LOWER="$ROOT/lower"

setup() {
    rm -rf "$UPPER" "$LOWER"
    mkdir -p "$UPPER/init.d" "$LOWER"
}

run_block() {
    env UPPER_DIR="$UPPER" LOWER_INITD="$LOWER" sh "$ROOT/block.sh"
}

check() {
    desc="$1"; shift
    if "$@"; then echo "ok: $desc"; else echo "FAIL: $desc"; FAILED=1; fi
}

# image-owned regular upper copy removed (image wins)
setup
: > "$UPPER/init.d/zram"
: > "$LOWER/zram"
run_block
check "image-owned upper copy removed" test ! -e "$UPPER/init.d/zram"

# user-created script (not shipped by image) preserved
setup
: > "$UPPER/init.d/my-script"
run_block
check "user-only script preserved" test -f "$UPPER/init.d/my-script"

# whiteout cases need CAP_MKNOD
if mknod "$ROOT/probe" c 0 0 2>/dev/null; then
    rm -f "$ROOT/probe"

    # kernel whiteout (char 0:0) hiding an image-owned script: removed
    setup
    mknod "$UPPER/init.d/zram" c 0 0
    : > "$LOWER/zram"
    run_block
    check "whiteout hiding image script removed" test ! -e "$UPPER/init.d/zram"

    # whiteout for something the image does not ship: preserved
    setup
    mknod "$UPPER/init.d/user-deleted" c 0 0
    run_block
    check "unrelated whiteout preserved" test -c "$UPPER/init.d/user-deleted"
else
    echo "skip: whiteout cases (no CAP_MKNOD)"
fi

# no init.d in overlay at all: no-op
setup
rmdir "$UPPER/init.d"
run_block
check "missing upper init.d is a no-op" test ! -d "$UPPER/init.d"

[ "$FAILED" -eq 0 ] && echo "PASS" || exit 1
