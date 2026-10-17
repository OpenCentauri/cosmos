#!/usr/bin/env python3
"""Manage COSMOS addons.

Usage: addon list | addon install NAME | addon remove NAME
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
from datetime import datetime
from pathlib import Path
import re
import shutil
import subprocess
import sys


PLUGINS_DIR = Path("/etc/klipper/plugins")
MOONRAKER_CONF = Path("/etc/klipper/config/moonraker.conf")
INIT_DIR = Path("/etc/init.d")
SECTION_HEADER = re.compile(r"^\s*\[([^]\r\n]+)\]\s*(?:[#;].*)?$")


class MoonrakerConfigWriter:
    def __init__(self, moonraker_config_path: Path):
        self.path = Path(moonraker_config_path)

    def backup(self):
        """Save a timestamped copy beside the other printer config backups."""
        backup_dir = self.path.parent / "config-backups"
        backup_dir.mkdir(parents=True, exist_ok=True)
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")
        destination = backup_dir / f"moonraker-{timestamp}.conf"
        shutil.copy2(self.path, destination)

    @staticmethod
    def _without_section(config, section_name):
        """Remove one section, preserving the rest of the config verbatim."""
        lines = []
        removing = False
        for line in config.splitlines(keepends=True):
            header = SECTION_HEADER.match(line)
            if header:
                removing = header.group(1).strip() == section_name
            if not removing:
                lines.append(line)
        return "".join(lines)

    def add(self, section_name: str, section: dict):
        config = self.path.read_text(encoding="utf-8")
        updated = self._without_section(config, section_name)
        if updated and not updated.endswith("\n"):
            updated += "\n"
        updated += f"[{section_name}]\n"
        updated += "".join(f"{key}: {value}\n" for key, value in section.items())
        self.backup()
        self.path.write_text(updated, encoding="utf-8")

    def remove(self, section_name: str):
        config = self.path.read_text(encoding="utf-8")
        updated = self._without_section(config, section_name)
        if updated == config:
            return False
        self.backup()
        self.path.write_text(updated, encoding="utf-8")
        return True


MOONRAKER_CONFIG_WRITER = MoonrakerConfigWriter(MOONRAKER_CONF)


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
            MOONRAKER_CONFIG_WRITER.add(f"update_manager {self.name}", {
                "type": "git_repo",
                "path": str(self.folder),
                "origin": self.repository_url,
                "primary_branch": self.branch,
                "managed_services": "klipper",
            })
        except (OSError, RuntimeError, subprocess.CalledProcessError):
            if self.folder.exists():
                shutil.rmtree(self.folder)
            raise
        print(f"Installed {self.name}.")
        print(f"To use this addon, you likely have to edit printer.cfg. Read the README for instructions:\n{url}")
        self._restart_services()

    def uninstall(self):
        section_name = f"update_manager {self.name}"
        if self.folder.exists():
            if not self.folder.is_dir():
                raise RuntimeError(f"Expected an addon directory at {self.folder}")
            MOONRAKER_CONFIG_WRITER.remove(section_name)
            shutil.rmtree(self.folder)
        elif not MOONRAKER_CONFIG_WRITER.remove(section_name):
            print(f"{self.name} is not installed.")
            return
        print(f"Removed {self.name}.")
        self._restart_services()


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
    commands.add_parser("list", help="List available addons and installation status")
    for command in ("install", "remove"):
        subparser = commands.add_parser(
            command, aliases=["uninstall"] if command == "remove" else []
        )
        subparser.add_argument("name", choices=sorted(KLIPPY_ADDONS))
    args = parser.parse_args()
    try:
        addons = {name: KlipperAddon(name, entry) for name, entry in KLIPPY_ADDONS.items()}
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
