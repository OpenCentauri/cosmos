#!/usr/bin/env python3
"""Default off Mainsail's built-in Screws Tilt Adjust dialog.

The MANUAL_LEVELING/_SCREWS_TILT_PROMPT macros (cosmos#248) are the
leveling UX; Mainsail's native dialog stacks on top of them. Users can
re-enable via Settings -> Interface.
"""
import sys
from pathlib import Path


def replace_exact(text, old, new, count, path):
    found = text.count(old)
    if found != count:
        raise RuntimeError(f"{path}: expected {count}x {old[:60]!r}, found {found}")
    return text.replace(old, new)


def main():
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {Path(sys.argv[0]).name} /path/to/mainsail-webroot")
    webroot = Path(sys.argv[1])
    bundles = sorted((webroot / "assets").glob("index-*.js"))
    if len(bundles) != 1:
        raise RuntimeError(f"{webroot}: expected exactly one assets/index-*.js bundle, found {len(bundles)}")
    path = bundles[0]
    text = path.read_text(encoding="utf-8")
    # Store default (fresh settings DBs)
    text = replace_exact(text, "boolScrewsTiltAdjustDialog:!0", "boolScrewsTiltAdjustDialog:!1", 1, path)
    # `?? true` fallbacks in the dialog + settings-page getters (old DBs missing the key)
    text = replace_exact(
        text,
        "uiSettings.boolScrewsTiltAdjustDialog)==null?!0:e",
        "uiSettings.boolScrewsTiltAdjustDialog)==null?!1:e",
        2,
        path,
    )
    path.write_text(text, encoding="utf-8")

    # Verify end state independently of the apply step (catches wrong-occurrence
    # patches and upstream layouts where a second defaults object appears).
    final = path.read_text(encoding="utf-8")
    if final.count("boolScrewsTiltAdjustDialog:!1") != 1:
        raise RuntimeError(f"{path}: screws-tilt dialog default not patched exactly once")
    if "boolScrewsTiltAdjustDialog:!0" in final:
        raise RuntimeError(f"{path}: screws-tilt dialog default still ON")
    if final.count("uiSettings.boolScrewsTiltAdjustDialog)==null?!1:e") != 2:
        raise RuntimeError(f"{path}: getter fallbacks not patched")
    if "uiSettings.boolScrewsTiltAdjustDialog)==null?!0:e" in final:
        raise RuntimeError(f"{path}: getter fallbacks still ON")


if __name__ == "__main__":
    main()
