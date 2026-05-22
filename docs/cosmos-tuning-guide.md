# Cosmos tuning guide

Cosmos ships with a layered set of opt-in optimizations for the Elegoo Centauri Carbon and similar Allwinner R528 boards (128 MB RAM, eMMC storage). This document is the operator-facing reference: what each tuning does, how to enable it, and how to verify it took effect.

## Layered model

Tunings are organized in tiers. Higher tiers depend on the substrate of lower tiers but can be enabled independently.

| Tier | Posture | Examples |
|------|---------|----------|
| Defaults | Always on; baseline cosmos behaviour | zram swap (priority 100), busybox syslogd, schedutil cpufreq governor |
| Tier-A | Opt-in via marker file or klipper include; safe for daily use | CPU governor pinning macros, KSM kernel feature, fstab noatime, USB data offload |
| Tier-B | Opt-in via marker or include; finer-grained | OOM score adjust, writeback sysctl tuning, resource-check macro, thermal gate, crash-log capture |
| Tier-C (future) | Larger substrate changes; planned | print-resume, gcode dedup, OOBE |

The defaults are designed to be safe. Every tuning is opt-in: a printer with no markers + no overrides behaves the same as stock cosmos.

## Tier-A

### CPU governor pinning macros

The R528 ships with the schedutil CPU governor (variable-frequency scaling based on demand). For high-precision moves at the start of a print, pinning to "performance" eliminates frequency-scaling jitter; for idle states, dropping to "powersave" reduces heat + power.

**Enable:** add to `printer.cfg`:

```ini
[include klipper-readonly/cosmos-cpu-tuning.cfg]
[include klipper-readonly/cosmos-shell-cpu-governor.cfg]
```

**Use:** wrap PRINT_START with `_CPU_GOVERNOR_PERFORMANCE` and PRINT_END with `_CPU_GOVERNOR_RESTORE`. The allowlist (performance | schedutil | ondemand | powersave | conservative) prevents arbitrary writes to scaling_governor sysfs.

**Verify:** during a print, `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` should report `performance`. After PRINT_END, it returns to whatever the operator's RESTORE macro pins (default: schedutil).

### KSM (Kernel Same-page Merging)

Kernel feature that scans anonymous memory pages and merges identical ones. On a small-RAM box running multiple Python processes (klipper, moonraker, ustreamer) that share library + interpreter pages, KSM typically reclaims 5-15 MB of RAM.

**Enable:** kernel side is automatic if your image is built with `CONFIG_KSM=y`. Userspace activation:

```sh
echo 1 > /sys/kernel/mm/ksm/run
echo 100 > /sys/kernel/mm/ksm/pages_to_scan
echo 50  > /sys/kernel/mm/ksm/sleep_millisecs
```

To persist across reboot, add the three echoes to a `local.d/` script or to your `/etc/init.d/` post-boot hook.

**Verify:** `cat /sys/kernel/mm/ksm/pages_sharing` after 5-10 minutes of runtime should be a few hundred or more.

### fstab noatime + commit interval

Default mount options on eMMC partitions update access time on every read. On a busy print this drives extra eMMC writes for no operator-visible benefit. The base-files fstab sets `noatime,nodiratime` on the cosmos eMMC partitions plus `commit=60` on `/user-resource` to extend the journal commit interval.

**Enable:** automatic; nothing to do.

**Verify:** `mount | grep mmcblk` should show `noatime,nodiratime` on the listed partitions.

### USB data offload (marker-gated)

Bind-mount selected eMMC directories onto a USB stick so writes land on the USB substrate instead of the eMMC. Reduces eMMC wear; trade-off is the USB stick must stay plugged in to avoid lazy umount.

**Enable:** create `PONO-DATA-OFFLOAD.conf` at the root of a USB stick. Example:

```sh
OFFLOAD_LOG=1
OFFLOAD_GCODES=1
OFFLOAD_THUMBNAILS=1
OFFLOAD_CACHE=0
```

Insert the USB stick. `udev` triggers the mount + bind. Re-insert after reboot to re-bind.

**Filesystem requirements:** ext4 for OFFLOAD_LOG and OFFLOAD_CACHE (need POSIX perms). vfat (FAT32) is acceptable for OFFLOAD_GCODES and OFFLOAD_THUMBNAILS but the script refuses log + cache on vfat to prevent world-readable secrets.

**Verify:** `/run/pono-data-offload.active` lists the USB mount path. `mount | grep /var/log` shows the bind. `logger -t test hello` + `grep test /var/log/messages` on the USB stick confirms writes are landing there.

## Tier-B

### Writeback tuning

Default kernel `vm.dirty_ratio` is 20% / `vm.dirty_background_ratio` is 10% of RAM. On 128 MB RAM that holds 25 MB in writeback before flush, which on long prints produces burst writes that amplify eMMC wear. The cosmos-tier-b recipe pulls these to 5% / 2% so dirty pages flush in smaller, more frequent batches.

**Enable:** automatic via the `cosmos-tier-b` recipe, which installs an init script that reads `/etc/sysctl.d/99-cosmos-tier-b.conf` at boot.

**Verify:** `cat /proc/sys/vm/dirty_ratio` reports 5. `cat /proc/sys/vm/dirty_background_ratio` reports 2.

### OOM score adjust

Under memory pressure the kernel's OOM killer chooses a victim. Without help, it commonly picks klipper (the largest Python process), which interrupts prints. The init scripts set `oom_score_adj=-100` on klipper and moonraker (well below the default 0) so they survive memory pressure unless ustreamer (which is set to +1000) cannot satisfy the kernel's reclaim need alone.

**Enable:** automatic via the modified init scripts.

**Verify:** with klipper running, `cat /proc/$(pgrep -x klipper)/oom_score_adj` reports `-100`. Same for moonraker.

### Resource-check macro

Observability macro that logs a single-line snapshot of memory + load + disk + CPU + KSM state at any point in a print. Threshold flags surface inline (RAM_LOW, DISK_LOW, LOAD_HIGH, CPU_THROTTLE).

**Enable:** add to `printer.cfg`:

```ini
[include klipper-readonly/cosmos-resource-check.cfg]
```

**Use:** call `_PONO_RESOURCE_CHECK` from the console, the start of PRINT_START, or any other macro. Output lands in `klippy.log`. For periodic snapshots, call `_PONO_RESOURCE_PERIODIC_START [DURATION=N]` (default 300 sec) to begin and `_PONO_RESOURCE_PERIODIC_STOP` to halt; both wrap `UPDATE_DELAYED_GCODE` so there is no config-section duplication.

**Verify:** `grep pono-res /board-resource/klippy.log | tail -5` shows the most recent snapshot lines. The format is `pono-res ram=NNMB swap=NNMB load=N.NN disk=NNNMB gov=NAME freq=NNNMHz ksm=NNN <flags>`; `<flags>` is `ok` when no threshold tripped, otherwise a space-separated subset of `RAM_LOW DISK_LOW LOAD_HIGH CPU_THROTTLE MISSING_RAM MISSING_DISK`.

### Thermal gate

R528 thermal sensor lives at `/sys/class/thermal/thermal_zone0/temp` (millidegrees Celsius). Above 75 deg C the SoC starts thermal-throttling; above 80 deg C the operator probably wants a hard powersave fall-back. cosmos-thermal-gate.cfg ships macros for both.

**Enable:** add to `printer.cfg`:

```ini
[include klipper-readonly/cosmos-thermal-gate.cfg]
```

**Use:** wrap PRINT_START with `_PONO_THERMAL_GATE` as the first call. If CPU is above 75 deg C, the gate aborts via `action_raise_error` (PRINT_START stops at the gate; klipper itself stays operational so the operator can wait + retry without FIRMWARE_RESTART). Pass `FORCE=1` to override for a one-shot bypass.

For periodic temp monitoring during long prints, call `_PONO_THERMAL_PERIODIC_START [DURATION=N]` (default 60 sec) and `_PONO_THERMAL_PERIODIC_STOP` to halt. The periodic checker pins the CPU governor to `powersave` if the SoC climbs past 80 deg C as a hard-throttle fallback before reaching the R528 max-spec ~95 deg C.

**Verify:** call `_PONO_THERMAL_REPORT` from the console; klippy.log shows the current temp. `cat /sys/class/thermal/thermal_zone0/temp` confirms the raw value. The Klipper `[temperature_sensor cosmos_cpu]` block exposes the same value to any Klipper-aware UI (Mainsail / Fluidd will display it alongside the printer's hotend / bed temps).

### Persistent crash-log capture

A klippy crash without graceful shutdown loses the in-memory log. The pono-crash-capture script snapshots the previous run's klippy.log, moonraker.log, and a dmesg tail to `/board-resource/crash-logs/<UTC-iso>/` on every klipper start. Rotates to keep the last 8 capture sets.

**Enable:** automatic via the klipper-init-d start path. Disable by writing `CRASH_CAPTURE_ENABLED=0` to `/board-resource/PONO-CRASH-CAPTURE.conf`.

**Verify:** `ls /board-resource/crash-logs/` shows timestamped subdirectories. The most recent reflects the just-prior boot.

## Combined deployment example

A common operator profile is "I want all the Tier-A + Tier-B optimizations enabled at print start":

```ini
# printer.cfg
[include klipper-readonly/cosmos-cpu-tuning.cfg]
[include klipper-readonly/cosmos-shell-cpu-governor.cfg]
[include klipper-readonly/cosmos-resource-check.cfg]
[include klipper-readonly/cosmos-thermal-gate.cfg]

[gcode_macro PRINT_START]
gcode:
    _PONO_THERMAL_GATE
    _PONO_RESOURCE_CHECK
    _CPU_GOVERNOR_PERFORMANCE
    # ... rest of PRINT_START ...

[gcode_macro PRINT_END]
gcode:
    # ... rest of PRINT_END ...
    _CPU_GOVERNOR_RESTORE
    _PONO_RESOURCE_CHECK
```

Plus drop a `PONO-DATA-OFFLOAD.conf` on a USB stick for off-eMMC log + gcode + thumbnail persistence.

## Diagnostic substrate

A measurement script lives at `tools/cosmos_tier_a_baseline.sh` (also covers Tier-B despite the filename) for pre-flash + post-flash snapshots. Run it via UART root shell or SSH:

```sh
sh /opt/tools/cosmos_tier_a_baseline.sh pre /tmp/baseline-before.log
# flash SWU + reboot
sh /opt/tools/cosmos_tier_a_baseline.sh post /tmp/baseline-after.log
diff -u /tmp/baseline-before.log /tmp/baseline-after.log
```

Captures: free / vmstat / meminfo / swaps / zram / KSM state / cpufreq / mount opts / df / vmstat / top processes / Tier-B sysctls / OOM scores / resource-check output / thermal + cpufreq / crash-logs presence.

## Anchors

- `meta-opencentauri/recipes-apps/klipper/files/cosmos-cpu-tuning.cfg` (governor macros)
- `meta-opencentauri/recipes-apps/klipper/files/cosmos-shell-cpu-governor.cfg` (governor shell-command binding)
- `meta-opencentauri/recipes-core/usb-automount/files/usb-mount` (USB offload and marker logic)
- `meta-opencentauri/recipes-core/usb-automount/files/PONO-DATA-OFFLOAD.conf.example` (marker template)
- `meta-opencentauri/recipes-core/cosmos-tier-b/` (writeback sysctls)
- `meta-opencentauri/recipes-apps/klipper/files/klipper-init-d` (OOM adjust and crash-capture hook)
- `meta-opencentauri/recipes-apps/moonraker/files/moonraker-init-d` (OOM adjust)
- `meta-opencentauri/recipes-apps/klipper/files/cosmos-resource-check.cfg` (resource-check macro)
- `meta-opencentauri/recipes-apps/klipper/files/pono-resource-check` (resource-check shell helper)
- `meta-opencentauri/recipes-apps/klipper/files/cosmos-thermal-gate.cfg` (thermal macros)
- `meta-opencentauri/recipes-apps/klipper/files/pono-crash-capture` (crash-capture script)
- `meta-opencentauri/recipes-kernel/linux/linux-mainline/elegoo-centauri-carbon1/mem-optimization.cfg` (KSM + ZSMALLOC)
- `meta-opencentauri/recipes-core/base-files/files/fstab` (noatime)
