#!/usr/bin/env python3

import sys, os, subprocess

def transform_url(url : str) -> str:
    if url.startswith("https://github.com/OpenCentauri/cosmos/actions/runs/"):
        run_id = url.split("/")[7]
        return f"https://nightly.link/OpenCentauri/cosmos/actions/runs/{run_id}/CC1%20Firmware.zip"

    raise ValueError(f"Unsupported URL format: {url}")

if __name__ == "__main__":
    os.chdir("/user-resource/.tmp")
    if len(sys.argv) != 2:
        print("Usage: flash-artifact <url>")
        sys.exit(1)

    url = sys.argv[1]
    filename = "firmware.zip"

    print(f"Downloading {url} to {filename}...")
    subprocess.run(["curl", "-L", "-o", filename, transform_url(url)], check=True)
    print(f"Unpacking {filename}...")
    subprocess.run(["unzip", "-o", filename], check=True)
    print("Installing firmware...")
    subprocess.run(["flash", "./opencentauri-upgrade-elegoo-centauri-carbon1.rootfs.swu"], check=True)
    print("Rebooting...")
    os.remove(filename)
    os.remove("./opencentauri-upgrade-elegoo-centauri-carbon1.rootfs.swu")
    subprocess.run(["reboot"], check=True)