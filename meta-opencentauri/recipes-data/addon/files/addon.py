#!/usr/bin/env python3
"""Manage COSMOS addons.

Usage: addon init | addon list | addon install NAME | addon remove NAME
"""

KLIPPY_ADDONS = {
    "dragonbreath": {
        "origin": "https://github.com/plastikman/dragonbreath-klipper",
        "description": "Klipper integration for the custom DragonBreath firmware for the Biqu Panda Breath",
        "branch": "main"
    },
    "knock-macros": {
        "origin": "https://github.com/shawn-makes-stuff/Klipper-Knock-Macros",
        "description": "Knock a rhythm on the printer frame to run a macro.",
        "folder": "knock",
        "branch": "main"
    },
    "led-effect": {
        "origin": "https://github.com/julianschill/klipper-led_effect",
        "description": "LED effects plugin for klipper",
        "folder": "led_effect",
        "branch": "master",
        "extra_files": {
            "__init__.py": "from .src.led_effect import load_config, load_config_prefix",
            "src/__init__.py": ""
        }
    }
}

import argparse
from abc import ABC, abstractmethod
from pathlib import Path
import shutil
import subprocess
import sys


PLUGINS_DIR = Path("/etc/klipper/plugins")
MOONRAKER_ADDONS_CONF = Path("/etc/klipper/config/moonraker-readonly/addons.conf")
INIT_DIR = Path("/etc/init.d")


class Addon(ABC):
    @property
    @abstractmethod
    def name(self) -> str:
        pass

    @abstractmethod
    def is_installed(self) -> bool:
        pass

    @property
    @abstractmethod
    def description(self) -> str:
        pass

    @property
    @abstractmethod
    def repository_url(self) -> str:
        pass

    @abstractmethod
    def install(self):
        pass

    @abstractmethod
    def uninstall(self):
        pass


class KlipperAddon(Addon):
    def __init__(self, name: str, entry: dict):
        self._name = name
        self._description = entry.get("description", "")
        self._repository_url = entry["origin"]
        self.branch = entry["branch"]
        self.extra_files = entry.get("extra_files", {})
        # Kalico discovers plugins by folder name, which must match the config section.
        self.folder : Path = PLUGINS_DIR / entry.get("folder", name)

    @property
    def name(self) -> str:
        return self._name

    def is_installed(self) -> bool:
        return self.folder.is_dir()

    @property
    def description(self) -> str:
        return self._description

    @property
    def repository_url(self) -> str:
        return self._repository_url

    @staticmethod
    def _restart_services():
        failed = []
        print("Restarting Moonraker and Klipper...", flush=True)
        for service in ("moonraker", "klipper"):
            try:
                subprocess.run(
                    [str(INIT_DIR / service), "restart"],
                    check=True,
                    stdout=subprocess.DEVNULL,
                    stderr=subprocess.DEVNULL,
                )
            except (OSError, subprocess.CalledProcessError):
                failed.append(service)
        if failed:
            raise RuntimeError(
                "Addon changes were saved, but restart failed for: " + ", ".join(failed)
            )

    def _readme_url(self):
        origin = self.repository_url.rstrip("/")
        if origin.endswith(".git"):
            origin = origin[:-4]
        return f"{origin}/blob/{self.branch}/README.md"

    def _write_extra_files(self):
        for relative_path, contents in self.extra_files.items():
            destination = self.folder / Path(relative_path)
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_text(contents, encoding="utf-8")

    def install(self):
        if self.folder.exists() or self.folder.is_symlink():
            raise RuntimeError(
                f"{self.name} already exists at {self.folder}; remove it first."
            )
        print(f"Repository: {self.repository_url}")
        print(
            "WARNING: Double-check this addon before installing it. "
            "This addon lives outside the COSMOS repository; it is not developed by the OpenCentauri team."
        )
        try:
            confirmed = input("Type 'continue' to install: ") == "continue"
        except (EOFError, KeyboardInterrupt):
            print()
            confirmed = False
        if not confirmed:
            print("Installation cancelled.")
            return

        try:
            print(f"Downloading {self.name}...", flush=True)
            subprocess.run(
                ["git", "clone", "--single-branch", "--branch", self.branch,
                 "--", self.repository_url, str(self.folder)],
                check=True,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
            )
            self._write_extra_files()
            url = self._readme_url()
        except (OSError, RuntimeError, subprocess.CalledProcessError):
            if self.folder.exists():
                shutil.rmtree(self.folder)
            raise
        print(f"Installed {self.name}.")
        print(f"To use this addon, you likely have to edit printer.cfg. Read the README for instructions:\n{url}")
        self._restart_services()

    def uninstall(self):
        if self.folder.exists():
            if not self.folder.is_dir():
                raise RuntimeError(f"Expected an addon directory at {self.folder}")
            shutil.rmtree(self.folder)
        else:
            print(f"{self.name} is not installed.")
            return
        print(f"Removed {self.name}.")
        self._restart_services()


def init_addons(addons: dict[str, KlipperAddon]):
    """Generate Moonraker update configuration for the installed addons."""
    config = "# Generated by addon init. Do not edit.\n"
    for name, addon in sorted(addons.items()):
        if not addon.is_installed():
            continue
        config += f"\n[update_manager {name}]\n"
        config += "type: git_repo\n"
        config += f"path: {addon.folder}\n"
        config += f"origin: {addon.repository_url}\n"
        config += f"primary_branch: {addon.branch}\n"
        config += "managed_services: klipper\n"
    MOONRAKER_ADDONS_CONF.parent.mkdir(parents=True, exist_ok=True)
    MOONRAKER_ADDONS_CONF.write_text(config, encoding="utf-8")


def list_addons(addons : dict[str, KlipperAddon]):
    for name, addon in sorted(addons.items()):
        print(f"{name}: {addon.repository_url}")
        print(f"  { 'Installed' if addon.is_installed() else 'Not installed'}")
        if addon.description:
            print(f"  {addon.description}")
        print()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    commands.add_parser("init", help="Generate Moonraker configuration for installed addons")
    commands.add_parser("list", help="List available addons and installation status")
    for command in ("install", "remove"):
        subparser = commands.add_parser(
            command, aliases=["uninstall"] if command == "remove" else []
        )
        subparser.add_argument("name", choices=sorted(KLIPPY_ADDONS))
    args = parser.parse_args()
    try:
        addons = {name: KlipperAddon(name, entry) for name, entry in KLIPPY_ADDONS.items()}
        if args.command == "init":
            init_addons(addons)
            return 0
        if args.command == "list":
            list_addons(addons)
            return 0
        addon = addons[args.name]
        if args.command == "install":
            addon.install()
        else:
            addon.uninstall()
    except (OSError, RuntimeError, subprocess.CalledProcessError) as error:
        print(f"addons: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
