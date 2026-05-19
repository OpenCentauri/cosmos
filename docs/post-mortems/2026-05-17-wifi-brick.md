# 2026-05-17 cosmos WiFi brick  --  post-mortem

**Date of brick:** 2026-05-17.
**Date of recovery:** 2026-05-18 via UART to mainboard J4.
**Affected unit:** jack-primary's Elegoo Centauri Carbon, cosmos `0.0.6` build (`fedcf64` / May 16 SWU).
**Severity:** unit LAN-unreachable; touchscreen WiFi spinner indefinitely; revert blocked because kalico had already been flashed to the bed MCU (Class 217).

## Symptoms

- Touchscreen renders normally; WiFi screen shows endless connect spinner.
- Configured "Lee Network" SSID never appears in the touchscreen WiFi list.
- Printer not at its historical LAN IP; not reachable from any subnet host.
- About panel empty (Moonraker unreachable).
- Standard recovery via `usb-mount` + `emergency.swu` stock-rollback failed at `restore-mcu-firmware` because kalico was on the bed MCU (Class 217 anchor; this part was correctly diagnosed at the time).

## Misdiagnosis path

For ~24 hours the working hypothesis was: AIC8800 driver bind failure matching upstream issue [shenmintao/aic8800d80#46](https://github.com/shenmintao/aic8800d80/issues/46) (a known firmware-load timing race where `aic_load_fw` fails to open `/lib/firmware/aic8800D80/fmacfw_8800d80_u02.bin` at boot, fixable by manual `modprobe -r aic8800_fdrv && modprobe aic8800_fdrv`).

This produced PR #6 ([`docs/research/aic8800.md`](../research/aic8800.md), 171 lines) cataloguing four candidate fixes (A defer auto-load, B driver patch, C firmware-readable pre-flight in `wifi-rescue`, D `CONFIG_FW_LOADER_USER_HELPER` + udev). All anchored against the upstream issue. All speculative pending dmesg from the bricked unit.

### Why the AIC8800 anchor was wrong

The printer's actual WiFi radio is a Realtek RTL8821CU, not an AIC8800. UART recovery on 2026-05-18 ran `lsusb` for the first time:

```text
Bus 001 Device 004: ID 0bda:c811 Realtek Semiconductor Corp. 802.11ac NIC
```

Driver loaded for it: `rtw_8821cu`. Cosmos ships `meta-opencentauri/recipes-kernel/rtw88/rtw88_git.bb` for exactly this case.

`aic8800_fdrv` was ALSO loaded (cosmos ships it for other Centauri Carbon SKUs that DO carry AIC8800). The naive `lsmod | grep -iE 'aic|rtw|wlan'` view from the bench-side debugging chain showed `aic8800_fdrv` listed, which got mistaken for the active radio driver. Nobody ran `lsusb` against the printer to identify the chip by VID:PID. Class 218 ("Load Hardware Specs Before Physical Instruction") was already banked at this point; the lesson didn't apply itself because the recognition signals enumerated wiring / pin / connector contexts, not "name a driver from the running fleet's debug telemetry."

## Actual root cause

Two compounding configuration bugs in `/etc/wpa_supplicant.conf` (operator-provisioned during initial Marlindog WiFi setup, then shipped unchanged into the cosmos image build's `/etc`):

1. **`GROUP=netdev` on a busybox image with no `netdev` group.** Cosmos 0.0.6 ships a minimal Yocto rootfs without the `netdev` group that desktop distros use. The conf line:

   ```ini
   ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
   ```

   caused `wpa_supplicant` to fail control-interface initialization with:

   ```text
   CTRL: Invalid group 'netdev'
   Failed to initialize control interface 'DIR=/var/run/wpa_supplicant GROUP=netdev'
   ```

   `wpa_supplicant` then exited without attempting any association. `wifi-rescue`'s modprobe-cycle was useless because it does not touch the supplicant.

2. **Literal SSID mismatch.** `wpa_supplicant.conf` carried `ssid="Lee Network"` (capital N, no trailing space). The router actually broadcasts the string `Lee network` followed by one trailing space character (lowercase n; the trailing space is significant and was verified via `iw dev wlan0 scan` once the supplicant was up). `wpa_supplicant` is strict on literal SSID match; `netsh wlan show networks` on Spambook had normalized the displayed name and hidden the divergence from the operator.

After both were fixed via UART shell (`sed -i` to strip the GROUP= clause + rewrite the SSID line) the supplicant came up, associated on the 5 GHz band (RTL8821CU at signal -23 dBm, 433 Mbit/s VHT-MCS 9 80MHz), and progressed to the DHCP-isolation phase that the recovery thread is still working through.

## Recovery path that worked

1. DSD TECH SH-U09C5 USB-to-TTL adapter (FTDI FT232RL, multi-voltage) into Belkin powered hub on pono-pi.
2. Adapter wired to mainboard J4 (right-side 4-pin 2.54mm header; pin 1 GND square pad, pin 2 TX, pin 3 RX, pin 4 5V leave open). Voltage verified at 3.3V on TX-to-GND with a multimeter before connecting.
3. Power on printer; arm `screen /dev/ttyUSB0 115200` on pono-pi.
4. Cosmos booted cleanly to a `cosmos login:` prompt. Linux kernel, klipper, moonraker, grumpyscreen, ustreamer all started normally.
5. Logged in as `root` (no password), read `/data/wifi-rescue.log` → found the `Invalid group 'netdev'` error.
6. `lsusb` → identified the actual chip (RTL8821CU, not AIC8800).
7. `iw dev wlan0 scan` → revealed the SSID literal mismatch.
8. Fixed both in `/etc/wpa_supplicant.conf`, restarted networking, association succeeded.

Subsequent DHCP / mesh-routing issues are downstream of the root-cause fix and being worked through in real time.

## What was wrong with the diagnosis chain

| Failure | Consequence | Pre-emptive control |
|---|---|---|
| `lsmod`-only WiFi driver identification (Maui never ran `lsusb` on the printer) | 24h of upstream-issue-#46 spelunking on the wrong chip | Class 218 recognition signal extension: "before naming the WiFi driver, run `lsusb` and grep for known wireless VIDs (`0bda` Realtek, `0e8d` MediaTek, `148f` Ralink, `a69c` AIC8800, `0cf3` Atheros, `2357` TP-Link, `13b1` Linksys)" |
| Assumed `wpa_supplicant.conf` was healthy without ever reading it | All effort spent on driver layer; the supplicant was failing at init | Class 218 recognition signal extension: when the symptom is "WiFi does not associate" and the driver is loaded, read `wpa_supplicant.conf` + supplicant log BEFORE any kernel-side investigation |
| Trusted `netsh wlan show networks` SSID display as canonical | SSID-literal mismatch hidden by Windows's display normalization | Anchor against the AP's beacon directly (`iw scan`, `wpa_cli scan_results`) when configuring `wpa_supplicant` |
| `wifi-rescue` script could not auto-heal a config bug | The script ran S80 on every boot, detected interface absent, modprobe-cycled, gave up. The supplicant-init error never triggered any compensating action | wifi-rescue commit [fa28c17](../../meta-opencentauri/recipes-core/wifi-rescue/files/wifi-rescue): add `validate_wpa_group()` pre-check that strips GROUP= when the named group is absent. Defensive + reversible (backup at `<conf>.broken-group-<name>-stripped`) |
| `WIFI_RESCUE_USB_VENDORS` default was `a69c` only | USB unbind/rebind branch could not reach the Realtek radio if it ever became the failure surface | wifi-rescue commit [fa28c17](../../meta-opencentauri/recipes-core/wifi-rescue/files/wifi-rescue-default): default extended to `a69c 0bda` |

## What the cosmos source got right

- `wifi-rescue` ALREADY listed `rtw_8821cu` in `WIFI_RESCUE_DRIVERS` (line 17). The driver-reload chain would have tried it. It did not fix anything because the failure was upstream of any driver concern.
- `meta-opencentauri/recipes-kernel/rtw88/rtw88_git.bb` exists; RTL8821CU support is intentional.
- Class 217 closure work (PRs #3 / #4) already gated `restore-mcu-firmware` and `update-cosmos` correctly for the kalico-on-MCU substrate; that part of the brick (the revert-path blockage) was independently real and remains correctly diagnosed.

## What the cosmos source still gets wrong (open items)

- `wifi-rescue` does not detect literal SSID mismatch as a failure mode. It cannot safely auto-fix this (rewriting the SSID is destructive without operator intent), but it could log a loud warning when `wpa_cli scan_results` shows a strong AP whose SSID differs only in case/whitespace from the configured SSID. Filed implicit; not shipped this pass.
- `wpa_supplicant.conf` validation could move further upstream: a pre-flight at first boot that sanity-checks the config (group existence, SSID-vs-scan-results sanity if WiFi is up briefly) and writes findings to `/var/log/wpa-config-validate.log`. Larger scope; not shipped.
- Operator-side: there is no tooling to validate a `wpa_supplicant.conf` BEFORE flashing it into a SWU. The shipped conf came from an upstream cosmos image that had been built on a desktop distro with the `netdev` group; the cosmos image build did not catch the mismatch.

## Cross-references

- [Class 217](../lessons/class-217-revert-paths-must-survive-substrate.md)  --  still valid for the revert-path-blocked side of the brick (kalico-on-MCU). Not the same failure as this post-mortem covers.
- [Class 218](../lessons/class-218-load-specs-before-physical-instruction.md)  --  instance #2 banked from this misdiagnosis. The original instance (2026-05-17, ~3h of rear-panel UART wire-mapping) was hardware-pin specific; this instance extends recognition signals to "naming a driver from `lsmod` rather than `lsusb`."
- [docs/research/aic8800.md](../research/aic8800.md)  --  preserved as reference research for Centauri Carbon SKUs that DO ship AIC8800. Now carries a CORRECTION banner pointing at this post-mortem.
- [docs/recovery.md](../recovery.md)  --  operator decision tree. Branch A (USB-Ethernet for WiFi-dead) was the right design but assumed reachable rootfs without a working WiFi config; this post-mortem shows the additional supplicant-config failure mode.

## Provenance

- Recovery thread: parallel session 2026-05-18 evening, UART captures + diagnostic sweeps via pyserial.
- Author of this post-mortem: Maui (audit thread, same date), anchored against the parallel-thread findings and the cosmos source at s-plus-batch `fa28c17`.
- Original brick handoff: [`HANDOFF-COSMOS-V4-2026-05-17.md`](../../HANDOFF-COSMOS-V4-2026-05-17.md) at the Brofalo working tree root (Pono-side note, not in the cosmos repo).
- Original brick memory: `feedback_cosmos_brick_post_mortem_2026_05_17.md` in the Pono memory dir (now carries a CORRECTION-2026-05-18 banner cross-referencing this doc).
