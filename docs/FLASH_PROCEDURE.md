# Flash procedure - cosmos-0.0.7-brofalo.7-nightly.49 on Centauri Carbon

**Tier 3** (irreversible + external + production hardware). Pause for Jack at the bench.

**SWU artifact (built 2026-05-22 from `dce2ce9`, GH Actions run [26309172503](https://github.com/Brofalo/pono-print-os/actions/runs/26309172503)):**

- File: `cosmos-0.0.7-brofalo.7-nightly.49.swu`
- Size: `120,105,472` bytes (~115 MB)
- SHA256: `46ebd28c0765f4b67bb285e8ba1f2b2da0091967ee7807d3516f3418bbeeef2b`
- Version string in bootenv: `0.0.7-brofalo.7-nightly`
- Hardware compat: `1.0`
- Target: `elegoo-centauri-carbon1`
- Signing: **UNSIGNED** (cosign signing in development per [SECURITY.md](../SECURITY.md); bench-CC scope-exempt per CLAUDE.md Signed-First Supply Chain rule)
- Internal A/B install scheme: bootA.img + rootfs squashfs + u-boot + bootlogos, each sha256-anchored in manifest (all 4 verified clean against actual content)

**Local staging path** (.gitignored under `build/`):
- `C:/Brofalo/pono-print-os/build/cosmos-update.swu` (USB-rename copy, same bytes)
- `C:/Brofalo/pono-print-os/build/cosmos-0.0.7-brofalo.7-nightly.49.swu` (versioned copy)

Both copies verified SHA256 = `46ebd28c0765f4b67bb285e8ba1f2b2da0091967ee7807d3516f3418bbeeef2b`.

---

## Pre-flash gate (Class 249 Foundation-Flawless; ALL rows must pass before flash)

| # | Check | Pass criteria | How to verify |
|---|---|---|---|
| P1 | Printer reachable | ping ok at `192.168.50.112` (wired) OR `192.168.50.145` (WiFi) | `ping 192.168.50.112` from any host on the LAN; expect <5ms reply |
| P2 | Mainsail webUI live | HTTP 200 on `http://192.168.50.112/` | Open in browser; should see Mainsail dashboard |
| P3 | No print in progress | Mainsail shows "Idle" or "Ready"; touchscreen LCD shows idle home | Visual check at printer |
| P4 | Bed clear, nozzle at home / safe-Z | Visual | Look at the printer |
| P5 | Current FW version captured | Note the running version BEFORE the flash | Mainsail -> Machine page OR `cat /etc/os-release` via touchscreen terminal (if available) |
| P6 | Revert path proven visible | Touchscreen "Switch to OC Patched" button is reachable | Visual on touchscreen |
| P7 | USB stick ready (FAT32) | A FAT32-formatted USB stick available; >=128 MB free | Standard USB stick |
| P8 | Power supply reliable | UPS or stable wall power; no in-progress brownout | Confirm power source |

If any row fails: STOP. Diagnose. Do not proceed.

---

## Flash paths (pick ONE)

### Path A: USB stick + touchscreen Update COSMOS button (RECOMMENDED)

Best for: routine flash; no SSH required; works even with broken network.

Steps:

1. Copy `cosmos-update.swu` (from `C:/Brofalo/pono-print-os/build/cosmos-update.swu`) to the root of a FAT32 USB stick. The file MUST be named `cosmos-update.swu` or `update.swu`; the on-printer `find-local-firmware` helper looks for those two names only.
2. Verify SHA256 on the USB stick matches the artifact: `46ebd28c0765f4b67bb285e8ba1f2b2da0091967ee7807d3516f3418bbeeef2b`.
3. Insert the USB stick into the printer's USB-A port. The usb-automount udev rule will mount the stick at `/tmp/usb/<device>/`.
4. Touchscreen: press **Update COSMOS** button. The `update-cosmos` script runs; `find-local-firmware` discovers the USB-staged SWU and consumes it without any network fetch.
5. Watch the touchscreen: progress bar should advance through the 4 image installs (bootA -> rootfs -> u-boot -> bootlogos). swupdate should report success.
6. Printer reboots automatically after install. Wait ~60s.

### Path B: SSH-pre-staged + update-cosmos CLI

Best for: scripted / repeat flashes after SSH access is established. **CURRENTLY BLOCKED**: Dropbear is up but default OC creds were rejected at COSMOS bring-up (SKILL.md Status: "SSH on COSMOS: Dropbear up but default OC creds rejected. Needs password set via touchscreen/Mainsail."). Set the password first via touchscreen or Mainsail terminal before this path is usable.

Steps (once SSH unblocked):

1. From Jack's Windows shell: `scp build/cosmos-update.swu root@192.168.50.112:/user-resource/update.swu`
2. SSH: `ssh root@192.168.50.112 'update-cosmos'`
3. Watch output: `update-cosmos: using pre-staged SWU at /user-resource/update.swu (no download)` followed by `flash /user-resource/update.swu` running.
4. Printer reboots automatically after install. SSH session drops; reconnect in ~60s.

### Path C: Touchscreen Update COSMOS WITHOUT USB (NOT RECOMMENDED)

The `update-cosmos` script falls back to `curl` from GitHub if no local file is found. The hardcoded `FW_URL` points to **OpenCentauri upstream**, NOT Brofalo. Pressing Update COSMOS with no local SWU will pull the WRONG firmware (upstream nightly, not our Brofalo cosmos-0.0.7-brofalo.7-nightly.49). Do not use this path unless you intentionally want upstream OpenCentauri.

To change `FW_URL` to point at Brofalo, edit `meta-opencentauri/recipes-data/update-scripts/files/update-cosmos` and rebuild the SWU. Out of scope for this flash.

---

## During-flash observation

Live-watch from the touchscreen + Mainsail console (if reachable). swupdate writes per-image progress to:
- Touchscreen progress bar
- `/var/log/swupdate.log` (if Mainsail terminal is open)
- Serial console (ttyS0) if a UART debug cable is attached

Expected timing per image:
- bootA.img (6.4 MB) -> ~5 sec
- rootfs squashfs (102 MB) -> ~60 sec
- u-boot (528 KB) -> ~1 sec
- bootlogos (6.4 MB) -> ~5 sec

Total wall-time post-press: ~90 sec to flash, +60 sec reboot = ~2.5 min.

---

## Post-flash gate (full smoke battery per Class 249)

| # | Check | Pass criteria | How to verify |
|---|---|---|---|
| F1 | Printer boots cleanly | Touchscreen lights up; Mainsail reachable on `http://192.168.50.112/` within 60s of reboot | Browser + ping |
| F2 | New version visible in bootenv | `swu_version = "0.0.7-brofalo.7-nightly"` | SSH `fw_printenv swu_version` OR Mainsail terminal |
| F3 | Mainsail + Moonraker live | Dashboard renders; Klipper status "Ready" | Browser |
| F4 | Klipper MCUs loaded | `tail -n 50 /board-resource/klippy.log \| grep "Loaded MCU"` shows 3 lines (mcu, bed, hotend) with build timestamp `>= 20260522` | Mainsail terminal |
| F5 | Built-in LIS2DW visible | `SHAPER_CALIBRATE AXIS=X` does NOT error with "accelerometer not found" | Mainsail console |
| F6 | A/B partition table healthy | `cat /proc/mtd` shows mmcblk0 with p4/p5 + p7/p8 partitions; the partition just flashed shows "now_X_next_Y" matches expectation | `fw_printenv boot_part mmc_root systemAB_next` |
| F7 | M112 Tests 1-3 per docs/M112_VERIFY.md | Heater drops within 500ms, stepper releases within 1s, fan drops within 1s | Run `docs/M112_VERIFY.md` from the pono-print repo |
| F8 | Bed mesh re-run at 45C | `BED_MESH_CALIBRATE BED_TEMP=45` runs clean | Mainsail console; mesh stored in slot for (plate_type=PEI, bed_temp=45) |
| F9 | Input shaping re-validated at print temp | `_CALIBRATE_ALL_STEP_4 EXTRUDER_TEMP=260 BED_TEMP=45` runs clean | Mainsail console; ZV @ ~52 Hz X / ~44 Hz Y expected |
| F10 | Calibration cube prints clean | 20mm cube, Easy-PA stack, no obvious defects (no missed steps, no failed first layer, no obvious dimensional drift) | Physical print + caliper measurement |
| F11 | Easy-PA temps actually take effect | Bed reads 45C (not 100C); nozzle 260C initial then 265C subsequent (not 280C) | Mainsail temp graph during cube print |
| F12 | OE companion COSMOS reconfig deployed | If `scripts/oe_companion_cosmos.py` ran cleanly post-flash, OctoEverywhere container on pono-pi shows the new firmware version | OE cloud dashboard |

Rows F1-F6 are mandatory pre-print. Rows F7-F11 are pre-multi-day-print. Row F12 is optional / deferred.

If any of F1-F4 fails within 5 minutes of reboot: emergency revert via touchscreen **Switch to OC Patched**.

---

## Revert procedure (if flash fails or post-flash gate fails)

The A/B partition scheme means the previous slot is still intact:
- If we just flashed slot A, slot B has the previous good cosmos.
- If we just flashed slot B, slot A has the previous good cosmos.

To revert: touchscreen **Switch to OC Patched** button. This re-points u-boot to the OTHER slot. Reboot. The previous-good cosmos boots.

If touchscreen is dead post-flash: SSH (if creds set) and run `fw_setenv systemAB_next <other-slot>` then `reboot`. If no SSH: USB recovery (cosmos-disk-recovery offline SWU flash from `recipes-core/cosmos-disk-recovery/cosmos-disk-recovery_1.0.bb`); see that recipe for the offline path.

**Do NOT flash a stock Elegoo .swu directly** - bricks the toolhead MCU per SKILL.md.

---

## Evidence to capture

1. Pre-flash: photo of touchscreen showing idle home + Mainsail dashboard showing version
2. During-flash: photo of touchscreen progress bar at ~50%
3. Post-flash: photo of touchscreen home with NEW version visible; Mainsail Machine page with `swu_version`
4. F4 output: copy klippy.log "Loaded MCU" lines
5. F10 cube: photo + caliper measurement
6. swupdate.log: `cat /var/log/swupdate.log` post-reboot (full log of the flash session)

Stash evidence under `docs/evidence/2026-05-XX-flash-0.0.7-brofalo.7-nightly.49/`.

---

## Bank result

Bank as `feedback_swu_flash_PASS_2026_05_XX.md` or `feedback_swu_flash_FAIL_2026_05_XX.md` depending on outcome:
- F1-F12 PASS/FAIL per the criteria above
- Evidence paths
- If FAIL: which gate + which test; this is the FIRST production-class Brofalo SWU flash, so a FAIL is a Class 217 SAFETY-CRITICAL surface
- If PASS: this closes the "we keep building, never flashing" loop named by Jack 2026-05-24

## Cross-references

- `docs/M112_VERIFY.md` (PR #26 on PONOdata/pono-print): Tests 1-3 for F7
- `SECURITY.md`: current signing posture (cosign in development; SWU unsigned)
- `CHANGELOG.md`: 0.0.7-brofalo.7-nightly.49 release entry
- CLAUDE.md "Signed-First Supply Chain for Production Systems": bench-CC scope exemption
- Class 217 (Cosmos brick / Gate Na alone): structural pass != deployment-ready
- Class 249 (Foundation-Flawless-Before-Scope): this smoke matrix is the load-bearing battery before any next-scope additions to the printer
- `~/.claude/skills/pono-print/SKILL.md`: COSMOS firmware track + calibration anchor problem context
