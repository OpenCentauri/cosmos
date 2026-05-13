#!/usr/bin/env python3
import json
import re
import sys
from pathlib import Path


APP_NAME = "OpenCentauri"
DESCRIPTION = "OpenCentauri web interface for the Elegoo Centauri Carbon"
PRIMARY_COLOR = "#D8DADC"
LOGO_SRC = "logo_opencentauri.svg"
ICON_SRC = "opencentauri-icon-gray.webp"
WORDMARK_SRC = "opencentauri-logo-small.png"
THEME_CSS = "opencentauri-theme.css"


def replace_once(text, old, new, path):
    if old not in text:
        raise RuntimeError(f"{path}: expected text not found: {old[:80]!r}")
    return text.replace(old, new, 1)


def patch_index_html(webroot):
    path = webroot / "index.html"
    text = path.read_text()
    text = re.sub(r"<title>.*?</title>", f"<title>{APP_NAME}</title>", text, count=1)
    text = re.sub(
        r'<meta name="description" content="[^"]*" />',
        f'<meta name="description" content="{DESCRIPTION}" />',
        text,
        count=1,
    )
    text = re.sub(
        r'<link rel="icon" type="image/png" sizes="32x32" href="[^"]+" />',
        f'<link rel="icon" type="image/webp" sizes="128x128" href="./{ICON_SRC}" />',
        text,
        count=1,
    )
    text = re.sub(
        r'\s*<link rel="icon" type="image/png" sizes="16x16" href="[^"]+" />\n',
        "\n",
        text,
        count=1,
    )
    text = re.sub(
        r'<meta name="theme-color" content="[^"]*" />',
        f'<meta name="theme-color" content="{PRIMARY_COLOR}" />',
        text,
        count=1,
    )
    text = re.sub(
        r'<meta name="apple-mobile-web-app-title" content="[^"]*" />',
        f'<meta name="apple-mobile-web-app-title" content="{APP_NAME}" />',
        text,
        count=1,
    )
    text = re.sub(
        r'<link rel="apple-touch-icon" sizes="180x180" href="[^"]+" />',
        f'<link rel="apple-touch-icon" sizes="128x128" href="./{ICON_SRC}" />',
        text,
        count=1,
    )
    text = re.sub(
        r'<meta name="msapplication-TileImage" content="[^"]+" />',
        f'<meta name="msapplication-TileImage" content="./{ICON_SRC}" />',
        text,
        count=1,
    )
    if THEME_CSS not in text:
        text = text.replace(
            '<link rel="manifest" href="./manifest.webmanifest">',
            f'<link rel="stylesheet" href="./{THEME_CSS}">\n  <link rel="manifest" href="./manifest.webmanifest">',
            1,
        )
    path.write_text(text)


def patch_manifest(webroot):
    path = webroot / "manifest.webmanifest"
    manifest = json.loads(path.read_text())
    manifest.update(
        {
            "name": APP_NAME,
            "short_name": APP_NAME,
            "description": DESCRIPTION,
            "theme_color": PRIMARY_COLOR,
            "icons": [
                {
                    "src": ICON_SRC,
                    "sizes": "128x128",
                    "type": "image/webp",
                    "purpose": "any maskable",
                }
            ],
        }
    )
    path.write_text(json.dumps(manifest, separators=(",", ":")))


def patch_config(webroot):
    path = webroot / "config.json"
    config = json.loads(path.read_text())
    presets = [p for p in config.get("themePresets", []) if p.get("name") != APP_NAME]
    presets.insert(
        0,
        {
            "name": APP_NAME,
            "color": PRIMARY_COLOR,
            "isDark": True,
            "logo": {
                "src": LOGO_SRC,
                "dark": "#232323",
                "light": "#ffffff",
            },
        },
    )
    config["themePresets"] = presets
    path.write_text(json.dumps(config, indent=2) + "\n")


def patch_main_bundle(webroot):
    bundles = sorted((webroot / "assets").glob("index-*.js"))
    if len(bundles) != 1:
        raise RuntimeError(f"{webroot}: expected exactly one assets/index-*.js bundle, found {len(bundles)}")

    path = bundles[0]
    text = path.read_text()
    text = replace_once(
        text,
        "get pageIcon(){let e=this.printInProgressIconDataUrl||this.defaultIconDataUrl;return[{rel:`icon`,type:`image/svg+xml`,sizes:`32x32`,href:e},{rel:`icon`,type:`image/svg+xml`,sizes:`16x16`,href:e}]}",
        "get pageIcon(){let e=this.printInProgressIconDataUrl||this.defaultIconDataUrl,t=e.startsWith(`data:`)?`image/png`:`image/webp`;return[{rel:`icon`,type:t,sizes:`32x32`,href:e},{rel:`icon`,type:t,sizes:`16x16`,href:e}]}",
        path,
    )
    text, count = re.subn(
        r"get defaultIconDataUrl\(\)\{let e=`<svg width=\"56\" height=\"56\".*?</svg>`;return`data:image/svg\+xml;base64,\$\{btoa\(e\)\}`\}",
        f"get defaultIconDataUrl(){{return`./{ICON_SRC}`}}",
        text,
        count=1,
    )
    if count != 1:
        raise RuntimeError(f"{path}: expected defaultIconDataUrl implementation not found")
    old_logo_src_getter = "get logoSrc(){return`./${this.theme.logo.src}`}"
    if old_logo_src_getter not in text:
        raise RuntimeError(f"{path}: expected logoSrc getter not found")
    text = text.replace(old_logo_src_getter, f"get logoSrc(){{return`./{LOGO_SRC}`}}")
    path.write_text(text)


def verify_assets(webroot):
    for name in (ICON_SRC, WORDMARK_SRC, LOGO_SRC, THEME_CSS):
        path = webroot / name
        if not path.exists():
            raise RuntimeError(f"{path}: missing OpenCentauri theme asset")


def main():
    if len(sys.argv) != 2:
        raise SystemExit(f"usage: {Path(sys.argv[0]).name} /path/to/fluidd-webroot")

    webroot = Path(sys.argv[1])
    verify_assets(webroot)
    patch_index_html(webroot)
    patch_manifest(webroot)
    patch_config(webroot)
    patch_main_bundle(webroot)


if __name__ == "__main__":
    main()
