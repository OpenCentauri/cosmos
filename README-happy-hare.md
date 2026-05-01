# OpenCentauri HH Images

This repository extends the standard OpenCentauri firmware with Happy Hare (MMU) enabled images for the Elegoo Centauri Carbon.

## Available Images

| Image | Description |
|-------|-------------|
| `opencentauri-upgrade` | Standard image (as defined in origin/main) |
| `opencentauri-upgrade-hh-full` | Standard + Happy Hare + Mainsail |
| `opencentauri-upgrade-hh-minimal` | Happy Hare without web frontend |

---

## HH-Full Image

> ⚠️ **WARNING: UNTESTED - USE AT YOUR OWN RISK**
>
> The `opencentauri-upgrade-hh-full` image is provided for completeness only.
> It has **NEVER been tested** and is **likely NOT WORKING**.
>
> The Centauri Carbon mainboard has only 128MB RAM. Running Happy Hare + Mainsail + Fluidd + multiple screens simultaneously will likely exceed available resources.
>
> **For a working MMU setup, use `opencentauri-upgrade-hh-minimal` instead.**

---

## HH-Minimal Image (Recommended)

The HH-Minimal image is designed for stable MMU operation on the resource-limited Centauri Carbon (128MB RAM). It includes:

- **Happy Hare** - Full MMU software support
- **Kalico + Moonraker** - Klipper firmware with API backend
- **No Web Frontend** - Mainsail/Fluidd removed to save memory

Use `tools/local-mainsail/` to run Mainsail on your PC and connect to the printer's moonraker API.

### Network Setup Required

> ⚠️ **Important: No Touchscreen Interface Available**
>
> The HH-Minimal image removes all touchscreen interfaces (atomscreen, guppyscreen, grumpyscreen). You must configure network access to connect to Moonraker (port 80).
>
> **Test network connectivity on the base image first!** Before flashing HH-Minimal, verify your network setup works on the standard OpenCentauri image.

#### Option 1: USB Ethernet Adapter

Connect a USB Ethernet adapter to the printer's USB port. Most adapters are auto-detected.

#### Option 2: WiFi via USB Stick (wpa_supplicant)

The printer automatically reads `wpa_supplicant.conf` from a USB stick when inserted.

Create `wpa_supplicant.conf` on a USB stick:

```conf
ctrl_interface=/var/run/wpa_supplicant
network={
    ssid="YOUR_WIFI_SSID"
    psk="YOUR_WIFI_PASSWORD"
}
```

Insert the USB stick - the file is automatically copied to `/etc/wpa_supplicant.conf` and WiFi connects.

Once connected, access Moonraker at `http://<printer-ip>:80`.

### Removed Packages

#### fluidd & mainsail (frontend)
Web frontends removed - no browser interface available on printer.
**Note:** Use `tools/local-mainsail/` to run Mainsail on your PC and connect to the printer's moonraker API (port 80).

#### atomscreen, guppyscreen & grumpyscreen
Touchscreen interfaces removed.
**Note:** The built-in touchscreen will not show any printer interface. Control via local Mainsail only.

#### ustreamer
Webcam streaming server removed.
**Note:** Webcam streaming will not be available via moonraker. Consider external webcam hosting if needed.

#### fbdoom
DOOM game removed.
**Note:** Removed because all screen interfaces are removed - no way to display the game.

#### v4l-utils
Video4Linux utilities removed.
**Note:** Webcam configuration tools not available. If you need webcam tuning, install externally.

### Added Packages

#### happy-hare
MMU software suite installed.
**Note:** Full Happy Hare installation with MMU configs in `/etc/klipper/config/mmu/`.

**Important:** Happy Hare's `install.sh` must be run on an external machine to generate MMU-specific config files for your hardware. The generated configs must either:
- Be added to the build (replace files in `recipes-apps/happy-hare/files/mmu/` before building)
- Or manually copied to the printer after installation (`/etc/klipper/config/mmu/`)

#### kalico-firmware-mmu
MMU-specific Klipper firmware for the MMU MCU board.
**Note:** Required if your MMU uses a separate MCU (e.g., ERB board). This firmware must be flashed to the MMU MCU after installation.

### Known Issues

#### Chamber Lighting
The camera-related shell commands were removed from the HH-Minimal image. This causes the following error on startup:

```
Error running command {CAMERA_LIGHT_ON}
```

**Note:** Chamber lighting will not work because `v4l2-ctl` is not available (removed with v4l-utils package). Without `v4l2-ctl`, there is no way to control the camera backlight compensation which is used for chamber lighting.

### PACKAGECONFIG Options

#### opencentauri-hh-full (Kalico)
Enables modified macros.cfg for HH-Full image:
- `SILENT` parameter - suppress prompts during filament loading (for automated MMU operation)
- `LENGTH` parameter - custom purge length for `_LOAD_FILAMENT_STEP_PUSH`
- `SET_DISPLAY_TEXT_COSMOS` macro - wrapper with `rename_existing` to avoid conflict with Happy Hare's `SET_DISPLAY_TEXT`

#### opencentauri-hh-minimal (Kalico)
Enables modified macros.cfg and minimal shell.cfg for HH-Minimal image:
- Same macros as `opencentauri-hh-full`
- Minimal `shell.cfg` overrides original - camera light and DOOM shell commands removed

#### config-only (Mainsail)
Removes frontend files but keeps Mainsail config macros.
**Note:** Used by HH-Minimal to save memory. The config provides essential macros needed by Mainsail to communicate with moonraker. The moonraker API is still available - use `tools/local-mainsail/` for web interface.

### Editable Configuration Files

#### mmu_vars.cfg (Happy Hare)
Added as `CONFFILES` - user edits are preserved during firmware upgrades.
**Note:** Happy Hare calibration variables at `/etc/klipper/config/mmu/mmu_vars.cfg`. Contains servo positions, gate offsets, bowden lengths, and other MMU-specific settings that change during calibration. Template is empty - will be populated by Happy Hare after calibration.

### Path Adjustments for OpenCentauri

Happy Hare config paths are adapted for OpenCentauri's directory structure:

| File | Setting | Original Happy Hare | OpenCentauri |
|------|---------|---------------------|--------------|
| mmu_macro_vars.cfg | `filename` (save_variables) | varies | `/etc/klipper/config/mmu/mmu_vars.cfg` |
| mmu_macro_vars.cfg | `path` (virtual_sdcard) | `~/printer_data/gcodes` | `/etc/klipper/gcodes` |

**Note:** If you run Happy Hare's `install.sh` to generate new configs, you must adjust these paths to match OpenCentauri's structure.

---

## Build & Testing Tools

See the `tools/` directory for build and testing utilities:

- **`tools/local-build/`** - Docker-based Yocto build environment for creating SWU images
- **`tools/local-mainsail/`** - Local Mainsail frontend for HH-Minimal (no frontend installed on printer)

### Quick Build

```bash
cd tools/local-build
./build.sh
```

### Testing HH-Minimal Locally

The HH-Minimal image has no web frontend. Use `tools/local-mainsail/` to run Mainsail on your PC:

```bash
cd tools/local-mainsail
# Edit config.json to set your printer's IP address
./start.sh
# Access at http://localhost:8080
```

---

> **Note:** Release publishing configuration was intentionally not included in the build script. This decision is left to the project maintainers.