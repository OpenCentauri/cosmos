# SWUpdate Firmware Deployment implementation
#
# Copyright (C) 2026  James Turton <james.turton@gmx.com>
#
# This file may be distributed under the terms of the GNU GPLv3 license.

from __future__ import annotations
import pathlib
import shlex
import shutil
import logging
from datetime import datetime
import distro
from .app_deploy import AppDeploy
from .common import AppType, Channel
from .git_deploy import GIT_MAX_LOG_CNT

# Annotation imports
from typing import (
    TYPE_CHECKING,
    Any,
    Tuple,
    Optional,
    Dict,
    List,
    cast
)
if TYPE_CHECKING:
    from ...confighelper import ConfigHelper
    from ..machine import Machine

SHORT_HASH_LEN = 10
DOWNLOAD_TIMEOUT = 3600.
DOWNLOAD_PROGRESS_INTERVAL = 1.
INSTALL_TIMEOUT = 1800.
REBOOT_DELAY = 2.

class SwuDeploy(AppDeploy):
    def __init__(self, config: ConfigHelper) -> None:
        super().__init__(config, "SWUpdate")
        self.repo = config.get("repo").strip().strip("/")
        self.owner, self.project_name = self.repo.split("/", 1)
        self.asset_name = config.get("asset_name")
        self.install_cmd = config.get("install_command", "flash")
        self.nightly_tag: str = config.get("nightly_release_tag", "0.0.0")
        self.version: str = "?"
        self.remote_version: str = "?"
        self.remote_hash: str = "?"
        self.rollback_version: str = "?"
        self.branch: str = "?"
        self.last_error: str = ""
        self.dl_info: Tuple[str, str, int] = ("?", "?", 0)
        self.commits_behind: List[Dict[str, Any]] = []
        self.commits_behind_count: int = 0
        self.warnings: List[str] = []
        self.reboot_pending: bool = False

    def _validate_install(self) -> None:
        self.warnings.clear()
        self._is_valid = True
        self.version = distro.os_release_attr("version_id") or "?"
        if self.version == "?":
            self.warnings.append(
                "Unable to detect installed version from VERSION_ID in os-release"
            )
            self._is_valid = False
        cmd = shlex.split(self.install_cmd)
        if not cmd or shutil.which(cmd[0]) is None:
            self.warnings.append(
                f"Install command '{self.install_cmd}' not found"
            )
            self._is_valid = False

    async def initialize(self) -> Dict[str, Any]:
        storage = await super().initialize()
        self._validate_install()
        self.remote_version = storage.get("remote_version", "?")
        self.remote_hash = storage.get("remote_hash", "?")
        self.rollback_version = storage.get("rollback_version", "?")
        self.branch = storage.get("branch", "?")
        self.last_error = storage.get("last_error", "")
        dl_info: List[Any] = storage.get("dl_info", ["?", "?", 0])
        self.dl_info = cast(Tuple[str, str, int], tuple(dl_info))
        self.commits_behind = storage.get("commits_behind", [])
        self.commits_behind_count = storage.get("cbh_count", 0)
        if storage.get("version", self.version) != self.version:
            self.commits_behind = []
            self.commits_behind_count = 0
            self.last_refresh_time = 0.
        if not self.needs_refresh():
            self._log_app_info()
        return storage

    def get_persistent_data(self) -> Dict[str, Any]:
        storage = super().get_persistent_data()
        storage.update({
            "version": self.version,
            "remote_version": self.remote_version,
            "remote_hash": self.remote_hash,
            "rollback_version": self.rollback_version,
            "branch": self.branch,
            "dl_info": list(self.dl_info),
            "commits_behind": self.commits_behind,
            "cbh_count": self.commits_behind_count,
            "last_error": self.last_error
        })
        return storage

    async def refresh(self) -> None:
        try:
            self._validate_install()
            if self.channel == Channel.DEV:
                await self._refresh_nightly()
            else:
                await self._refresh_stable()
        except Exception:
            logging.exception(f"{self.prefix}Error Refreshing")
        self._log_app_info()
        self._save_state()

    async def _github_request(self, resource: str) -> Any:
        client = self.cmd_helper.get_http_client()
        resp = await client.github_api_request(
            resource, attempts=3, retry_pause_time=.5
        )
        if resp.status_code == 304:
            return resp.json() if resp.content else None
        if resp.has_error():
            self.log_info(f"Github Request Error - {resp.error}")
            self.last_error = str(resp.error)
            return None
        self.last_error = ""
        return resp.json()

    def _find_asset(self, release: Dict[str, Any]) -> Tuple[str, str, int]:
        for asset in release.get("assets", []):
            if asset.get("name", "") == self.asset_name:
                return (
                    asset.get("browser_download_url", "?"),
                    asset.get("content_type", "?"),
                    asset.get("size", 0)
                )
        return ("?", "?", 0)

    async def _refresh_stable(self) -> None:
        release = await self._github_request(f"repos/{self.repo}/releases/latest")
        if not release:
            return
        self.remote_version = release.get("tag_name", "?")
        self.remote_hash = "?"
        self.branch = release.get("target_commitish", "?")
        self.dl_info = self._find_asset(release)
        self.commits_behind = []
        self.commits_behind_count = 0

    async def _refresh_nightly(self) -> None:
        release = await self._github_request(
            f"repos/{self.repo}/releases/tags/{self.nightly_tag}"
        )
        if not release:
            return
        self.branch = release.get("target_commitish", "?")
        self.dl_info = self._find_asset(release)
        commits = await self._github_request(
            f"repos/{self.repo}/commits?sha={self.nightly_tag}&per_page=1"
        )
        if not commits:
            return
        self.remote_hash = commits[0].get("sha", "?")
        self.remote_version = self.remote_hash[:SHORT_HASH_LEN]
        if self.version == "?":
            return
        comparison = await self._github_request(
            f"repos/{self.repo}/compare/{self.version}...{self.remote_hash}"
        )
        if comparison is None:
            if self.last_error:
                self.commits_behind = []
                self.commits_behind_count = 0
            return
        self.commits_behind_count = comparison.get("ahead_by", 0)
        new_commits: List[Dict[str, Any]] = comparison.get("commits", [])
        self.commits_behind = [
            self._format_commit(commit)
            for commit in reversed(new_commits[-GIT_MAX_LOG_CNT:])
        ]

    def _format_commit(self, commit: Dict[str, Any]) -> Dict[str, Any]:
        info: Dict[str, Any] = commit.get("commit", {})
        author: Dict[str, Any] = info.get("author", {})
        subject, _, message = info.get("message", "").partition("\n")
        date: str = author.get("date", "")
        timestamp = 0
        if date:
            timestamp = int(
                datetime.fromisoformat(date.replace("Z", "+00:00")).timestamp()
            )
        return {
            "sha": commit.get("sha", ""),
            "author": author.get("name", ""),
            "date": str(timestamp),
            "subject": subject.strip(),
            "message": message.strip(),
            "tag": None
        }

    def _log_app_info(self) -> None:
        warn_str = ""
        if self.warnings:
            warn_str = "\nWarnings:\n"
            warn_str += "\n".join([f" {item}" for item in self.warnings])
        dl_url, content_type, size = self.dl_info
        self.log_info(
            f"Detected\n"
            f"Repo: {self.repo}\n"
            f"Channel: {self.channel}\n"
            f"Local Version: {self.version}\n"
            f"Remote Version: {self.remote_version}\n"
            f"Remote Hash: {self.remote_hash}\n"
            f"Commits Behind: {self.commits_behind_count}\n"
            f"Valid: {self._is_valid}\n"
            f"Download Url: {dl_url}\n"
            f"Download Size: {size}\n"
            f"Content Type: {content_type}\n"
            f"Rollback Version: {self.rollback_version}"
            f"{warn_str}"
        )

    def _update_available(self) -> bool:
        if self.channel == Channel.DEV:
            return self.commits_behind_count > 0
        return self.remote_version not in ("?", self.version)

    async def update(
        self, rollback_info: Optional[Tuple[str, str, int]] = None
    ) -> bool:
        if self.reboot_pending:
            raise self.server.error(
                f"{self.prefix}Reboot pending, aborting update"
            )
        if not self._is_valid:
            raise self.server.error(
                f"{self.prefix}Invalid install detected, aborting update"
            )
        if rollback_info is not None:
            dl_info = rollback_info
            new_ver = self.rollback_version
            start_msg = "Rolling Back..."
        else:
            if not self._update_available():
                return False
            dl_info = self.dl_info
            new_ver = self.remote_version
            start_msg = "Updating..."
        dl_url, _, size = dl_info
        if dl_url == "?":
            raise self.server.error(
                f"{self.prefix}Release asset '{self.asset_name}' not found"
            )
        current_version = self.version
        event_loop = self.server.get_event_loop()
        self.notify_status(start_msg)
        self.notify_status("Downloading Release...")
        td = await self.cmd_helper.create_tempdir(self.name, "app")
        try:
            swu_file = pathlib.Path(td.name).joinpath(self.asset_name)
            await self._download(dl_url, swu_file, size)
            self.notify_status("Download Complete, installing firmware...")
            await self.cmd_helper.run_cmd(
                f"{self.install_cmd} {shlex.quote(str(swu_file))}",
                timeout=INSTALL_TIMEOUT, notify=True, log_stderr=True
            )
        finally:
            await event_loop.run_in_thread(td.cleanup)
        if rollback_info is None:
            self.rollback_version = current_version
        self.reboot_pending = True
        self._save_state()
        self.notify_status(f"Installed version {new_ver}, rebooting...")
        event_loop.delay_callback(REBOOT_DELAY, self._reboot)
        msg = "Update Finished..." if rollback_info is None else "Rollback Complete"
        self.notify_status(msg, is_complete=True)
        return True

    async def _download(self, url: str, dest: pathlib.Path, size: int) -> None:
        last_pct = -1

        def report_progress(eventtime: float) -> float:
            nonlocal last_pct
            try:
                downloaded = dest.stat().st_size
            except OSError:
                downloaded = 0
            pct = min(100, int(downloaded / size * 100)) if size > 0 else 0
            if pct != last_pct:
                last_pct = pct
                self.cmd_helper.on_download_progress(pct, size, downloaded)
            return eventtime + DOWNLOAD_PROGRESS_INTERVAL

        timer = self.server.get_event_loop().register_timer(report_progress)
        timer.start()
        try:
            await self.cmd_helper.run_cmd(
                f"curl -fsSL --retry 3 -o {shlex.quote(str(dest))} "
                f"{shlex.quote(url)}",
                timeout=DOWNLOAD_TIMEOUT, log_stderr=True
            )
        finally:
            timer.stop()
        report_progress(0.)

    async def _reboot(self) -> None:
        if self.cmd_helper.is_update_busy():
            self.server.get_event_loop().delay_callback(REBOOT_DELAY, self._reboot)
            return
        self.log_info("Rebooting to complete firmware update")
        machine: Machine = self.server.lookup_component("machine")
        await machine.get_system_provider().reboot()

    async def rollback(self) -> bool:
        if self.rollback_version in ("?", self.version):
            return False
        release = await self._github_request(
            f"repos/{self.repo}/releases/tags/{self.rollback_version}"
        )
        if not release:
            raise self.server.error(
                f"No release found for rollback version {self.rollback_version}"
            )
        return await self.update(self._find_asset(release))

    def get_update_status(self) -> Dict[str, Any]:
        status = super().get_update_status()
        warnings = list(self.warnings)
        if self.remote_version != "?" and self.dl_info[0] == "?":
            warnings.append(
                f"Asset '{self.asset_name}' not found in remote release"
            )
        status.update({
            "name": self.name,
            "owner": self.owner,
            "repo_name": self.project_name,
            "version": self.version,
            "remote_version": self.remote_version,
            "rollback_version": self.rollback_version,
            "last_error": self.last_error,
            "warnings": warnings,
            "anomalies": []
        })
        if self.channel != Channel.DEV:
            status["configured_type"] = str(AppType.WEB)
            return status
        repo_url = f"https://github.com/{self.repo}"
        status.update({
            "configured_type": str(AppType.GIT_REPO),
            "detected_type": str(AppType.GIT_REPO),
            "repo_detected": True,
            "remote_alias": "origin",
            "branch": self.branch,
            "remote_url": repo_url,
            "recovery_url": repo_url,
            "current_hash": self.version,
            "remote_hash": self.remote_hash,
            "is_dirty": False,
            "detached": False,
            "commits_behind": self.commits_behind,
            "commits_behind_count": self.commits_behind_count,
            "git_messages": [],
            "full_version_string": self.version,
            "pristine": True,
            "corrupt": False
        })
        return status
