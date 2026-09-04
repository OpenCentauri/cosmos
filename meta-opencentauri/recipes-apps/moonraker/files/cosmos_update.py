# COSMOS firmware updater for Moonraker's update_manager
#
# Registers a "cosmos" entry with update_manager so Mainsail, Fluidd and any
# other client that speaks the update_manager API can see the installed and
# latest COSMOS version, start an update, and follow its progress through the
# standard notify_update_response stream. No changes to update_manager itself
# are needed: the updater is added to its table from this component.
#
# Configuration (moonraker.conf):
#
#   [update_manager]
#   enable_system_updates: False
#
#   [cosmos_update]
#   update_command: /usr/bin/update-cosmos   # command whose stdout is shown as progress
#   update_timeout: 3600                      # seconds before the update is aborted
#   refresh_interval: 24                      # hours between version checks
#
# This file may be distributed under the terms of the GNU GPLv3 license.

from __future__ import annotations
import configparser
import logging
import pathlib
import time
from typing import TYPE_CHECKING, Any, Dict, List, Optional

from .update_manager.base_deploy import BaseDeploy

if TYPE_CHECKING:
    from ..confighelper import ConfigHelper
    from .update_manager.update_manager import UpdateManager

ISSUE_FILE = pathlib.Path("/etc/issue")
COSMOS_CONF = pathlib.Path("/etc/klipper/config/cosmos.conf")
GITHUB_OWNER = "OpenCentauri"
GITHUB_REPO = "cosmos"
MAX_COMMITS = 30


class CosmosDeploy(BaseDeploy):
    def __init__(self, config: ConfigHelper) -> None:
        super().__init__(config, name="cosmos")
        self.update_cmd: str = config.get("update_command", "/usr/bin/update-cosmos")
        self.update_timeout: float = config.getfloat("update_timeout", 3600.)
        self.version: str = "?"
        self.remote_version: str = "?"
        self.channel: str = "stable"
        self.commits_behind: List[Dict[str, Any]] = []
        self.last_error: str = ""

    async def initialize(self) -> Dict[str, Any]:
        storage = await super().initialize()
        self.remote_version = storage.get("remote_version", "?")
        self.commits_behind = storage.get("commits_behind", [])
        self.last_error = storage.get("last_error", "")
        await self._read_local_state()
        return storage

    async def _read_local_state(self) -> None:
        # Installed version, e.g. "OpenCentauri Cosmos 26.08.0 \n \l"
        try:
            parts = ISSUE_FILE.read_text().split()
            self.version = parts[2] if len(parts) > 2 else "?"
        except Exception:
            self.log_exc("Unable to read the installed version", traceback=False)
            self.version = "?"
        # Update channel from cosmos.conf, via config-manager if available
        channel = ""
        try:
            scmd = self.cmd_helper.get_shell_command()
            channel = await scmd.exec_cmd("config-manager update release", timeout=10.)
            channel = channel.strip()
        except Exception:
            channel = ""
        if not channel:
            try:
                parser = configparser.ConfigParser()
                parser.read(COSMOS_CONF)
                channel = parser.get("update", "release", fallback="stable")
            except Exception:
                channel = "stable"
        self.channel = channel or "stable"

    def _api(self):
        return self.cmd_helper.get_http_client()

    async def refresh(self) -> None:
        await self._read_local_state()
        self.last_error = ""
        try:
            if self.channel == "nightly":
                await self._refresh_nightly()
            else:
                await self._refresh_stable()
        except Exception as e:
            self.last_error = str(e)
            self.log_exc(f"Version check failed: {e}", traceback=False)
        self._save_state()

    async def _refresh_stable(self) -> None:
        resp = await self._api().github_api_request(
            f"repos/{GITHUB_OWNER}/{GITHUB_REPO}/releases?per_page=10"
        )
        if resp.has_error():
            raise self.server.error(f"GitHub request failed: {resp.error}")
        releases = resp.json()
        latest = None
        for release in releases:
            if release.get("prerelease") or release.get("draft"):
                continue
            latest = release.get("tag_name")
            break
        if not latest:
            raise self.server.error("No stable release found on GitHub")
        self.remote_version = str(latest)
        self.commits_behind = []
        self.log_info(f"installed {self.version}, latest {self.remote_version}")

    async def _refresh_nightly(self) -> None:
        # Nightly builds are identified by a short commit hash
        resp = await self._api().github_api_request(
            f"repos/{GITHUB_OWNER}/{GITHUB_REPO}/compare/{self.version}...main"
        )
        if resp.has_error():
            raise self.server.error(f"GitHub request failed: {resp.error}")
        data = resp.json()
        commits = data.get("commits", [])[-MAX_COMMITS:]
        behind: List[Dict[str, Any]] = []
        for c in commits:
            commit = c.get("commit", {})
            author = commit.get("author", {})
            msg = commit.get("message", "")
            try:
                date = time.mktime(time.strptime(
                    author.get("date", ""), "%Y-%m-%dT%H:%M:%SZ")) - time.timezone
            except Exception:
                date = 0
            behind.append({
                "sha": c.get("sha", ""),
                "author": author.get("name", ""),
                "date": int(date),
                "subject": msg.split("\n", 1)[0],
                "message": msg,
                "tag": None,
            })
        self.commits_behind = behind
        head = data.get("commits", [])
        self.remote_version = head[-1]["sha"][:10] if head else self.version
        self.log_info(
            f"installed {self.version}, {data.get('ahead_by', 0)} commit(s) behind main"
        )

    async def update(self) -> bool:
        if self.remote_version in ("?", self.version):
            self.notify_status(f"Reinstalling COSMOS {self.version}...")
        else:
            self.notify_status(f"Updating COSMOS {self.version} to {self.remote_version}...")
        self.notify_status(
            "The printer reboots by itself when the update is installed. "
            "Do not power it off."
        )
        try:
            await self.cmd_helper.run_cmd(
                self.update_cmd, timeout=self.update_timeout, notify=True,
                log_stderr=True
            )
        except Exception as e:
            self.last_error = str(e)
            self._save_state()
            raise self.log_exc(f"COSMOS update failed: {e}", traceback=False)
        # update-cosmos issues the reboot itself; this is the last thing the
        # clients hear before the connection drops.
        self.notify_status("COSMOS update installed, the printer is rebooting", is_complete=True)
        return True

    def get_update_status(self) -> Dict[str, Any]:
        status = super().get_update_status()
        nightly = self.channel == "nightly"
        status.update({
            "name": self.name,
            "configured_type": "git_repo" if nightly else "zip",
            "detected_type": "git_repo" if nightly else "zip",
            "channel": self.channel,
            "owner": GITHUB_OWNER,
            "repo_name": GITHUB_REPO,
            "version": self.version,
            "remote_version": self.remote_version,
            "current_hash": self.version if nightly else "",
            "remote_hash": self.remote_version if nightly else "",
            "commits_behind": self.commits_behind,
            "is_valid": True,
            "is_dirty": False,
            "detached": False,
            "corrupt": False,
            "branch": "main",
            "remote_alias": "origin",
            "last_error": self.last_error,
            "warnings": [],
            "anomalies": [],
            "info_tags": ["desc=COSMOS firmware"],
        })
        return status

    def get_persistent_data(self) -> Dict[str, Any]:
        data = super().get_persistent_data()
        data.update({
            "remote_version": self.remote_version,
            "commits_behind": self.commits_behind,
            "last_error": self.last_error,
        })
        return data


class CosmosUpdate:
    def __init__(self, config: ConfigHelper) -> None:
        self.server = config.get_server()
        um: UpdateManager = self.server.load_component(config, "update_manager")
        updaters = um.get_updaters()
        # Neither Klipper nor Moonraker is a git checkout on this image, so
        # update_manager holds placeholder entries for them that show up as
        # empty rows in the UIs. Drop them; COSMOS updates both anyway.
        for name in ("klipper", "moonraker"):
            if type(updaters.get(name)) is BaseDeploy:
                updaters.pop(name, None)
        if "cosmos" in updaters:
            raise config.error("update_manager already has a 'cosmos' entry")
        self.deploy = CosmosDeploy(config)
        updaters["cosmos"] = self.deploy
        # update_manager re-creates its klipper entry (as a background task)
        # every time Klippy connects, so prune the placeholder again shortly
        # after that happens.
        self.server.register_event_handler(
            "server:klippy_identified", self._schedule_prune
        )
        logging.info("cosmos_update: registered COSMOS updater with update_manager")

    def _schedule_prune(self) -> None:
        loop = self.server.get_event_loop()
        loop.delay_callback(2., self._prune_placeholders)
        loop.delay_callback(15., self._prune_placeholders)

    def _prune_placeholders(self, eventtime: float = 0.) -> None:
        um: UpdateManager = self.server.lookup_component("update_manager")
        updaters = um.get_updaters()
        for name in ("klipper", "moonraker"):
            if type(updaters.get(name)) is BaseDeploy:
                updaters.pop(name, None)


def load_component(config: ConfigHelper) -> CosmosUpdate:
    return CosmosUpdate(config)
