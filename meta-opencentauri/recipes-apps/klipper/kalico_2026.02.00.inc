HOMEPAGE = "https://kalico.gg/"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=1ebbd3e34237af26da5dc08a4e440464"

FILESEXTRAPATHS:prepend := "${THISDIR}/files:"

# Foundation plan Step 4 cutover (2026-05-23): source switched from
# OpenCentauri/kalico to Brofalo/pono-kalico. The OC fork's pre-existing
# patches that were applied via SRC_URI now live as native commits in
# pono-kalico (foundation plan H2 commitment = 5 patches as commits with
# upstream-author preservation). This eliminates the do_patch invocation
# for the migrated patches, which in turn eliminates the -dirty
# version-string artifact that confused every audit up to 2026-05-23.
#
# Migrated to commits (per pono-kalico HEAD 5d0a9ed8):
#   0001-Reduce-log-rotate-threshold.patch       -> 4218e722 (James Turton)
#   0003-load_cell_fusion-clear-sensor-updates   -> fb924646 (rjc862003)
#   0004-load_cell_probe-int-to-float-helpers    -> f7147f4e (Dmitry Butyugin)
#   0001-remove-save-config-subfile-check.patch  -> 5d0a9ed8 (Sims via OC PR #174)
#
# Plus KalicoCrew main back-pick:
#   heaters: lost_update_tolerance configurable  -> 299dedef (dalegaard via fc33b620)
#
# Still applied via SRC_URI (deferred per foundation plan conditions):
#   0002-reduce-calibration-difference-tolerance.patch (gated on HX711 bench)
#
# When HX711 bench unblocks Patch 2, SRC_URI becomes
# "git://Brofalo/pono-kalico.git" alone (no patches).

SRC_URI = "git://github.com/Brofalo/pono-kalico.git;protocol=https;branch=rpmsg-with-new-hx71x \
    file://0002-reduce-calibration-difference-tolerance.patch \
"
SRCREV = "1f3aee1098e90c6d89513fd1acb5e83a0f56675a"

# PR bump r5 -> r6: AQ2 KalicoCrew back-merge batch landed on
# pono-kalico (2026-05-23). 3 new commits absorbed:
#   32e92f89  Load Cell Tap Analysis (#836)
#   714764d1  probe: alternating probe direction support (#882)
#   836dcb27  trapq extract-all-moves default (#872)
# Plus 4 confirmed no-ops (already in tree via OC PR #760 merge) and
# DIVERGENCE.md updated. ipk hash changes; mirror the bed/toolhead/dsp
# firmware recipes' PR bump cadence.
PR = "r6"

S = "${WORKDIR}/git"

DEPENDS = " \
    python3-native \
"
