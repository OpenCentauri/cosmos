#!/bin/sh
# Host smoke test for the zram init script. Fakes modprobe/mkswap/swapon etc.
set -u
SCRIPT=$(cd "$(dirname "$0")/../files" && pwd)/init
ROOT=$(mktemp -d "${TMPDIR:-/tmp}/zram-test.XXXXXX")
FAILED=0
trap 'rm -rf "$ROOT"' EXIT

setup() {
    CASE="$ROOT/case"
    rm -rf "$CASE"
    mkdir -p "$CASE/bin" "$CASE/sys/block/zram0" "$CASE/proc/sys/vm"
    printf 'Filename\t\t\tType\t\tSize\tUsed\tPriority\n' > "$CASE/proc/swaps"
    printf 'MemTotal:       114656 kB\n' > "$CASE/proc/meminfo"
    : > "$CASE/sys/block/zram0/reset"
    : > "$CASE/sys/block/zram0/comp_algorithm"
    : > "$CASE/sys/block/zram0/disksize"
    : > "$CASE/log"
    rm -f "$CASE"/fail-* "$CASE"/always-fail-*

    for key in swappiness watermark_boost_factor watermark_scale_factor page-cluster vfs_cache_pressure; do
        : > "$CASE/proc/sys/vm/$key"
    done

    cat > "$CASE/bin/logger" <<'EOF'
#!/bin/sh
printf '%s\n' "$*" >> "$CASE/log"
EOF
    cat > "$CASE/bin/modprobe" <<'EOF'
#!/bin/sh
[ -f "$CASE/fail-modprobe-once" ] && { rm -f "$CASE/fail-modprobe-once"; exit 1; }
[ -f "$CASE/always-fail-modprobe" ] && exit 1
exit 0
EOF
    cat > "$CASE/bin/mkswap" <<'EOF'
#!/bin/sh
exit 0
EOF
    cat > "$CASE/bin/swapon" <<'EOF'
#!/bin/sh
[ -f "$CASE/fail-swapon-once" ] && { rm -f "$CASE/fail-swapon-once"; exit 1; }
grep -q "^${ZRAM_DEV}[[:space:]]" "$PROC_SWAPS" 2>/dev/null || \
    printf '%s\tpartition\t235520\t0\t100\n' "$ZRAM_DEV" >> "$PROC_SWAPS"
exit 0
EOF
    cat > "$CASE/bin/swapoff" <<'EOF'
#!/bin/sh
tmp="$PROC_SWAPS.tmp"
awk -v dev="$ZRAM_DEV" 'NR == 1 || $1 != dev { print }' "$PROC_SWAPS" > "$tmp"
mv "$tmp" "$PROC_SWAPS"
exit 0
EOF
    cat > "$CASE/bin/rmmod" <<'EOF'
#!/bin/sh
exit 0
EOF
    cat > "$CASE/bin/sleep" <<'EOF'
#!/bin/sh
exit 0
EOF
    chmod 0755 "$CASE"/bin/*
}

run_init() {
    env PATH="$CASE/bin:/bin:/usr/bin" \
        CASE="$CASE" \
        ZRAM_DEV="$CASE/zram0" \
        ZRAM_SYSFS="$CASE/sys/block/zram0" \
        PROC_SWAPS="$CASE/proc/swaps" \
        MEMINFO="$CASE/proc/meminfo" \
        SYSCTL_DIR="$CASE/proc/sys" \
        "$SCRIPT" "$@"
}

check() {
    desc="$1"; shift
    if "$@"; then echo "ok: $desc"; else echo "FAIL: $desc"; FAILED=1; fi
}

# success + idempotence
setup
run_init start >/dev/null
check "start succeeds" grep -q "$CASE/zram0" "$CASE/proc/swaps"
check "logs success" grep -q "active:" "$CASE/log"
run_init start >/dev/null
check "second start is no-op" grep -q "already active" "$CASE/log"

# retry after one modprobe failure
setup
: > "$CASE/fail-modprobe-once"
run_init start >/dev/null
check "recovers from one modprobe failure" grep -q "$CASE/zram0" "$CASE/proc/swaps"
check "logged attempt 2" grep -q "attempt 2/5" "$CASE/log"

# bounded failure
setup
: > "$CASE/always-fail-modprobe"
run_init start >/dev/null && { echo "FAIL: permanent modprobe failure should not succeed"; FAILED=1; }
check "gives up after 5 attempts" grep -q "after 5 attempts" "$CASE/log"
check "no swap left active" sh -c "! grep -q \"$CASE/zram0\" \"$CASE/proc/swaps\""

[ "$FAILED" -eq 0 ] && echo "PASS" || exit 1
