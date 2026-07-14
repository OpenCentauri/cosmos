#!/usr/bin/env python3
import subprocess, json, sys

def get_current_version() -> str:
    with open("/etc/issue") as f:
        return f.read().strip().split(" ")[2]

def get_version_branch() -> str:
    return subprocess.run(["config-manager", "update", "release"], capture_output=True, text=True, check=True).stdout

def is_check_for_updates_enabled() -> bool:
    result = subprocess.run(["config-manager", "update", "check_for_updates"], capture_output=True, text=True, check=True)
    return result.stdout.strip().lower() == "true"

def make_request(url: str) -> list|None:
    try:
        result = subprocess.run(["curl", "-sf", url], capture_output=True, text=True, check=True)
        return json.loads(result.stdout)
    except (subprocess.CalledProcessError, json.JSONDecodeError):
        return None

def get_latest_release() -> str|None:
    data = make_request("https://api.github.com/repos/OpenCentauri/cosmos/releases?per_page=5")
    
    if data is None:
        return None
    
    for release in data:
        if release.get("prerelease", False):
            continue
        return release.get("tag_name", None)
    
    return None
    
def get_latest_commit() -> str|None:
    data = make_request("https://api.github.com/repos/OpenCentauri/cosmos/commits?per_page=1")
    
    if data is None:
        return None
    
    if len(data) == 0:
        return None
    
    return data[0].get("sha", None)

def notify_update_availabe(new_version : str, current_version : str):
    title = "Update Available"
    message = f"An upgrade from COSMOS version {current_version} to {new_version} is available. Upgrade to the latest version for new features and bugfixes."
    subprocess.run(["uiprompt", title, message])
    print(title)
    print(message)

def main():
    if not is_check_for_updates_enabled():
        print("Update check is disabled. Skipping update check.")
        return
    
    version = sys.argv[1] if len(sys.argv) > 1 else get_current_version()

    if "PR" in version:
        print("Running a PR build. Skipping update check.")
        return
    
    is_stable = get_version_branch() == "stable"
    remote_version = get_latest_release() if is_stable else get_latest_commit()
    
    if remote_version is None:
        print("Failed to fetch latest version information.")
        return

    is_match = remote_version == version if is_stable else remote_version.startswith(version)
    if not is_match:
        notify_update_availabe(remote_version, version)
    else:
        print("No updates available.")

if __name__ == "__main__":
    main()