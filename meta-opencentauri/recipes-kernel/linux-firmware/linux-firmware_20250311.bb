# linux-firmware bump for cosmos: 20240909 -> 20250311
#
# poky/scarthgap (Yocto 5.0 LTS) ships linux-firmware_20240909.bb (Sep 2024).
# poky/walnascar (Yocto 5.2 LTS) ships linux-firmware_20250311.bb (Mar 2025),
# which is the LTS-validated next stop. The cosmos target ships rtw_8821cu
# (Realtek RTL8821CU USB, 0bda:c811); against APs broadcasting WPA2-PSK+SAE
# transition mode the supplicant reaches ASSOCIATED then silently fails the
# 4-way EAPOL handshake on 20240909. The rtl8821 firmware blobs were
# iterated multiple times between 20240909 and 20250311.
#
# sha256 verified against:
#   https://mirrors.edge.kernel.org/pub/linux/kernel/firmware/sha256sums.asc
#   b1083a36f19aea46f661dcfd4cd462d13933dcb4e7f0dc809525552dd5c3541d
#     linux-firmware-20250311.tar.xz
# Recipe shape mirrors poky:walnascar:meta/recipes-kernel/linux-firmware/linux-firmware_20250311.bb.
#
# Selection: PREFERRED_VERSION_linux-firmware = "20250311" in
# meta-opencentauri/conf/distro/cosmos.conf forces this recipe over the
# poky/scarthgap base (20240909).
#
# linux-firmware tarball bumps are additive (more blobs, updated blobs);
# package-split rules in poky's base recipe match by glob and pick up the
# same files at the new version. Yocto walnascar shipped this exact version
# in production for the 5.2 release cycle.

require recipes-kernel/linux-firmware/linux-firmware_20240909.bb

SRC_URI[sha256sum] = "b1083a36f19aea46f661dcfd4cd462d13933dcb4e7f0dc809525552dd5c3541d"

# WHENCE was updated between 20240909 and 20250311 (new firmware blobs
# added: intel/vpu, additional radeon/i915 entries, etc). scarthgap's
# parent recipe defines WHENCE_CHKSUM as a separately-overridable
# variable specifically so layers can bump it without restating the
# full LIC_FILES_CHKSUM list:
#
#   # poky/meta/recipes-kernel/linux-firmware/linux-firmware_20240909.bb
#   file://WHENCE;md5=${WHENCE_CHKSUM} \
#   WHENCE_CHKSUM = "6ae5ffd807c84809977286ad0b37acdb"
#
# Bump to the 20250311 tarball's md5. Verified against the unpacked
# tarball at build/tmp/work/.../linux-firmware-20250311/WHENCE.
WHENCE_CHKSUM = "886924eb733c4efcec21dff980795771"

# 20250311 Makefile reshaped the install targets vs the 20240909 base poky
# inherits from: `install-nodedup` was dropped, `install` no longer dedups
# by default, and `dedup` is a separate target that calls dedup-firmware.sh
# against the already-installed tree.
#
# scarthgap's PACKAGECONFIG[deduplicate] = "install,install-nodedup,rdfind-native"
# (poky/meta/recipes-kernel/linux-firmware/linux-firmware_20240909.bb:253)
# resolves do_install to `make ... install-nodedup` when deduplicate is not
# in the active PACKAGECONFIG, which fails on 20250311 with
#   make: *** No rule to make target 'install-nodedup'. Stop.
#
# Override the mapping to use targets that exist in 20250311:
#   deduplicate enabled  -> `install dedup` (install, then dedup pass)
#   deduplicate disabled -> `install` (no dedup; old install-nodedup behavior)
# DEPS (rdfind-native) is unchanged because the dedup step still uses it
# via dedup-firmware.sh (verified in the 20250311 tarball Makefile head).
PACKAGECONFIG[deduplicate] = "install dedup,install,rdfind-native"

# 20250311 also added a hard runtime dependency on GNU `parallel` to the
# copy-firmware.sh script when invoked with `-j > 1`:
#
#   has_gnu_parallel() { command -v parallel >/dev/null && parallel --version | grep -Fqi 'gnu parallel'; }
#
# yields `the GNU parallel command is required to use -j` and aborts. The
# Makefile pulls NUM_JOBS from MAKEFLAGS via
#   NUM_JOBS := $(or $(patsubst -j%,%,$(filter -j%,$(MAKEFLAGS))), nproc, 1)
# and Yocto's oe_runmake passes -j${PARALLEL_MAKE} (typically 4 on a
# quad-core host), so NUM_JOBS lands at 4 and the script aborts.
#
# Force NUM_JOBS=1 via EXTRA_OEMAKE so install runs single-jobbed and
# does not depend on parallel being in the recipe-sysroot. Adding
# parallel-native to DEPENDS or HOSTTOOLS is the alternative; the
# no-host-dep path is preferred because firmware install is fast at
# -j1 and dominated by disk I/O.
EXTRA_OEMAKE += "NUM_JOBS=1"
