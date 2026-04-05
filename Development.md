# Elegoo Centauri Carbon - Yocto Firmware Build

This repository contains a Yocto Project-based firmware build system for the Elegoo Centauri Carbon 3D printer family. The mainboard of these printers is powered by an Allwinner R528 SoC.

_**Unsure about what COSMOS is? [Check the FAQ to learn more](./FAQ.md)**_

## Prerequisites

Before starting the build process, you need to install the required dependencies on your host system (Ubuntu/Debian is assumed):

```bash
sudo apt update
sudo apt install gawk wget git diffstat unzip texinfo gcc build-essential \
     chrpath socat cpio python3 python3-pip python3-pexpect xz-utils debianutils \
     iputils-ping python3-git python3-jinja2 libegl1-mesa libsdl1.2-dev \
     pylint xterm python3-subunit mesa-common-dev zstd liblz4-tool file locales \
     bmap-tools sunxi-tools
```

## Build Configuration Options

### Machine Selection (CC1 vs CC2)

The build supports different machine configurations for the Centauri Carbon variants:

| Machine | Description |
|---------|-------------|
| `elegoo-centauri-carbon1` | Original Centauri Carbon 1 (default) |
| `elegoo-centauri-carbon2` | Centauri Carbon 2 |

The default in `build/conf/local.conf` is set to CC1:
```bash
MACHINE ?= "elegoo-centauri-carbon1"
```

To build for CC2, specify the machine on the command line:
```bash
MACHINE=elegoo-centauri-carbon2 bitbake opencentauri-image-usb
```

### Image Type Selection (USB vs eMMC)

Choose the appropriate image type based on your target boot media:

| Image Recipe | Target Media | Use Case |
|--------------|--------------|----------|
| `opencentauri-image-usb` | USB drive | Development, testing, or running from USB |
| `opencentauri-image-mmc` | Internal eMMC | Production installation on printer internal storage |

#### USB Image (`opencentauri-image-usb`)
- Boots from USB drive
- Read-write root filesystem
- Suitable for development and testing
- Larger partition layout for USB storage

**Important U-Boot Configuration for USB Builds:**
When building the USB image, the MMC environment storage options must be removed from the U-Boot defconfig for your target machine. These settings cause U-Boot to store its environment on the internal eMMC, which conflicts with USB boot.

For **CC1**, edit:
- `meta-opencentauri/recipes-bsp/u-boot/files/elegoo-centauri-carbon1/elegoo_centauri_carbon_defconfig`

For **CC2**, edit:
- `meta-opencentauri/recipes-bsp/u-boot/files/elegoo-centauri-carbon2/elegoo_centauri_carbon_defconfig`

Remove (or ensure absent) these configuration options:
```
CONFIG_ENV_IS_IN_MMC=y
CONFIG_ENV_OFFSET=0x1A66000
CONFIG_ENV_OFFSET_REDUND=0x1AA5000
CONFIG_SYS_REDUNDAND_ENVIRONMENT=y
```

This is required for proper USB boot functionality.

#### eMMC Image (`opencentauri-image-mmc`)
- Installs to internal eMMC storage
- Read-only SquashFS root with overlay filesystem for `/etc` on `/data` partition
- Optimized for production use
- Includes A/B boot partition scheme for safe updates via swupdate

## How to Build

1. **Initialize the build environment:**
   Source the environment setup script provided by Poky to prepare your shell for BitBake.
   ```bash
   source poky/oe-init-build-env build
   ```

2. **Configure your target (if needed):**
   Ensure `MACHINE` is set correctly in `build/conf/local.conf`:
   ```bash
   # For CC1 or CC2
   MACHINE ?= "elegoo-centauri-carbon1"
   ```

3. **Run BitBake:**
   Choose the appropriate image recipe for your target:
   
   **For USB booting:**
   ```bash
   bitbake opencentauri-image-usb
   ```
   
   **For eMMC/internal storage:**
   ```bash
   bitbake opencentauri-image-mmc
   ```
   
   *Note: The first build will take a significant amount of time as it downloads and compiles all necessary packages from source.*

## Build Outputs

After a successful build, the output files are located in:

```
tmp/deploy/images/elegoo-centauri-carbon1/
```

### USB Image Outputs
- `opencentauri-image-usb-elegoo-centauri-carbon1.rootfs.wic.gz` - Compressed disk image for USB drives

### eMMC Image Outputs
- `opencentauri-image-mmc-elegoo-centauri-carbon1.rootfs.wic.gz` - Full disk image for eMMC
- `bootA.img` - Extracted boot partition image (for swupdate)
- `bootlogos.img` - Extracted boot logos partition image (for swupdate)
- `rootfs.squashfs` - SquashFS root filesystem

## Disk Space Requirements

Building a complete Yocto image requires a substantial amount of disk space. Based on current build sizes, you should expect the project directory (including downloaded sources, build artifacts, and caches) to use approximately **38GB to 40GB** of disk space. Please ensure you have adequate free space before starting the build.

## Running on the Centauri Carbon 1

### USB Boot Method (Development)

Note that the current install requires having a serial UART connected to the CC1 motherboard, as well as a FEL USB cable attached. This will prevent the toolhead from being plugged in!

1. **Install built firmware image to a USB drive.**
   *(Warning: This is a destructive operation! Replace `sdX` with your actual USB drive device like `sdb`, `sdc`, etc.)*
   ```bash
   sudo bmaptool copy tmp/deploy/images/elegoo-centauri-carbon1/opencentauri-image-usb-elegoo-centauri-carbon1.rootfs.wic.gz /dev/sdX
   ```

2. **Boot into FEL Mode.**
   Connect via serial UART. Power on the printer and boot into the Elegoo u-boot, pressing any key to abort the normal boot process. Issue the following command to boot to FEL mode:
   ```
   efex
   ```

3. **Boot the new Yocto firmware image via USB from FEL mode.**
   Run the following commands on your host machine to load the mainline u-boot:
   ```bash
   sunxi-fel uboot tmp/deploy/images/elegoo-centauri-carbon1/u-boot-sunxi-with-spl.bin
   ```

   The mainline u-boot followed by the mainline Linux kernel should now boot! This will start up Klipper, Moonraker, Mainsail daemons, and a dropbear SSH server.

   **Important:** You need to unplug the FEL USB as it will conflict with the mainboard's ability to talk to the toolhead.

4. **Connect and Configure WiFi.**
   Login as the `root` user via the serial UART console.

   Configure your WiFi SSID and password:
   ```bash
   wpa_passphrase "SSID" "password" > /etc/wpa_supplicant.conf
   ```
   Then restart the WiFi adapter:
   ```bash
   ifdown wlan0 && sleep 5 && ifup wlan0
   ```

5. **Access the Printer Interface.**
   Find the printer's IP address by running `ip a`. Access the Mainsail interface by visiting the printer's IP address via HTTP (port 80) in your web browser!

### eMMC Install Method (Production)

For installing to internal eMMC storage, use the swupdate-based installation:

1. Build the `opencentauri-upgrade` target. This recipe depends on `opencentauri-image-mmc` and produces the SWUpdate bundle:
   ```bash
   bitbake opencentauri-upgrade
   ```
2. Locate the generated `.swu` file in `tmp/deploy/images/elegoo-centauri-carbon1/` (e.g. `opencentauri-upgrade-elegoo-centauri-carbon1.swu`).
3. Copy the `.swu` file to a FAT32-formatted USB drive in the `install_opencentauri` folder, renaming it to `update.swu`.
4. Insert the USB drive into the printer
5. Import the `IMPORT_ME_DO_NOT_PRINT` file via the printer screen as you would for stock OpenCentauri

## Configuration and Services

- **Klipper Configuration:** In the current build, the Klipper `printer.cfg` is located in `/etc/klipper/config/printer.cfg`.
- **Services:** Everything is running as an `init.d` service. You can restart Klipper by running:
  ```bash
  service klipper restart
  ```

## Troubleshooting Build Issues

### Clean Build
If you encounter build issues, try cleaning the specific package:
```bash
bitbake -c cleansstate <package-name>
bitbake <image-target>
```

### Full Reset
For a complete clean build (removes all build artifacts):
```bash
rm -rf tmp/
source poky/oe-init-build-env build
bitbake <image-target>
```
