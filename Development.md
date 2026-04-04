# Elegoo Centauri Carbon 1 - Yocto Firmware Build

This repository contains a Yocto Project-based firmware build system for the Elegoo Centauri Carbon 1 3D printer. The mainboard of this printer is powered by an Allwinner r528 SoC.

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

## How to Build

1. **Initialize the build environment:**
   Source the environment setup script provided by Poky to prepare your shell for BitBake.
   ```bash
   source poky/oe-init-build-env build
   ```

2. **Run BitBake:**
   Once the environment is set up, you can start the build process for the target image. The primary image recipe for this project is `opencentauri-image`.
   ```bash
   bitbake opencentauri-image
   ```
   *Note: The first build will take a significant amount of time as it downloads and compiles all necessary packages from source.*

## Disk Space Requirements

Building a complete Yocto image requires a substantial amount of disk space. Based on current build sizes, you should expect the project directory (including downloaded sources, build artifacts, and caches) to use approximately **38GB to 40GB** of disk space. Please ensure you have adequate free space before starting the build.

## Running on the Centauri Carbon 1

Note that the current install requires having a serial UART connected to the CC1 motherboard, as well as a FEL USB cable attached. This will prevent the toolhead from being plugged in!

1. **Install built firmware image to a USB drive.**
   *(Warning: This is a destructive operation! Replace `sdX` with your actual USB drive device like `sdb`, `sdc`, etc.)*
   ```bash
   sudo bmaptool copy tmp/deploy/images/elegoo-centauri-carbon1/opencentauri-image-elegoo-centauri-carbon1.rootfs.wic.gz /dev/sdX
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

## Configuration and Services

- **Klipper Configuration:** In the current build, the Klipper `printer.cfg` is located in `/etc/klipper/config/printer.cfg`.
- **Services:** Everything is running as an `init.d` service. You can restart Klipper by running:
  ```bash
  service klipper restart
  ```
