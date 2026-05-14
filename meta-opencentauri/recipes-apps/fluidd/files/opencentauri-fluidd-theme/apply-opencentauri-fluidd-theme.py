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
    # Add data-fluidd-theme to html tag for immediate CSS scoping (prevents FOUC)
    text = text.replace("<html lang=\"en\">", f'<html lang="en" data-fluidd-theme="{APP_NAME}">')
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
    # Set OpenCentauri as the default active theme for new users
    config["theme"] = {
        "color": PRIMARY_COLOR,
        "isDark": True,
        "logo": {
            "src": LOGO_SRC,
            "dark": "#232323",
            "light": "#ffffff",
        },
    }
    path.write_text(json.dumps(config, indent=2) + "\n")


def patch_main_bundle(webroot):
    """Patch the main JS bundle to add theme-aware dataset toggling."""
    bundles = sorted((webroot / "assets").glob("index-*.js"))
    if len(bundles) != 1:
        raise RuntimeError(f"{webroot}: expected exactly one assets/index-*.js bundle, found {len(bundles)}")

    path = bundles[0]
    text = path.read_text()

    # Patch onThemeChange to toggle a data attribute for CSS scoping.
    # We detect OpenCentauri by logo src (not name) because the name field
    # persists across theme switches via Fluidd's object-merge in updateTheme.
    old_on_theme_change = "async onThemeChange(e,t){let n=Io.framework.theme;n.dark=t.isDark,n.currentTheme.primary=t.color,n.currentTheme[`primary-offset`]=new ft(t.color).desaturate(5).darken(10).toHexString(),n.themes.dark.logo=t.logo.light,n.themes.light.logo=t.logo.dark}"
    new_on_theme_change = 'async onThemeChange(e,t){let n=Io.framework.theme;n.dark=t.isDark,n.currentTheme.primary=t.color,n.currentTheme[`primary-offset`]=new ft(t.color).desaturate(5).darken(10).toHexString(),n.themes.dark.logo=t.logo.light,n.themes.light.logo=t.logo.dark;document.documentElement.dataset.fluiddTheme=(t.logo?.src===`logo_opencentauri.svg`)?`OpenCentauri`:``}'
    text = replace_once(text, old_on_theme_change, new_on_theme_change, path)

    # Override the default favicon globally (all themes show OpenCentauri favicon)
    old_icon = 'get defaultIconDataUrl(){let e=`<svg width=\"56\" height=\"56\" viewBox=\"0 0 56 56\" xmlns=\"http://www.w3.org/2000/svg\"><g><path fill=\"${this.primaryOffsetColor}\" d=\"m14.853 33.757 11.617 9.66q.196.169.419.287l.002.001.044.023.017.009.03.014.027.013.021.01.037.016.01.005c.263.111.54.173.817.185h.01l.044.002h.104l.046-.002h.007a2.3 2.3 0 0 0 .818-.186l.008-.003.041-.018.016-.007.03-.015.032-.015.014-.007.044-.023.004-.002a2.3 2.3 0 0 0 .419-.287l10.86-9.031 5.646 3.727L28 56 9.243 37.398Zm.409-14.452 11.425 7.35c.407.267.86.4 1.313.4s.905-.133 1.312-.4l11.426-7.349 8.737 4.462L28 41.628 6.525 23.767zM28 0l24.42 8.993L28 24.699 3.58 8.99Z\" /><path fill=\"${this.primaryColor}\" d=\"m28 0-.135.05h.15v24.64L52.42 8.991Zm12.738 19.307-11.425 7.347-.06.04a2.4 2.4 0 0 1-1.237.36v14.56l21.459-17.846zm-.347 15.08-10.86 9.031q-.196.167-.418.285l-.006.002-.043.024-.013.007-.033.016-.03.014-.015.007-.041.018-.008.004c-.263.112-.54.173-.819.185h-.007l-.045.002h-.037v12.002l18.021-17.87Z\" /></g></svg>`;return`data:image/svg+xml;base64,${btoa(e)}`}'
    new_icon = f'get defaultIconDataUrl(){{return`./{ICON_SRC}`}}'
    text = replace_once(text, old_icon, new_icon, path)

    path.write_text(text)


def patch_state_chunk(webroot):
    """Patch the initial Vuex state so OpenCentauri is the default theme."""
    chunks = sorted((webroot / "assets").glob("state-*.js"))
    if not chunks:
        return  # Not all builds have this chunk

    old_theme = "theme:{isDark:!0,logo:{src:`logo_fluidd.svg`},color:`#2196F3`,backgroundLogo:!0}"
    new_theme = f"theme:{{isDark:!0,logo:{{src:`{LOGO_SRC}`}},color:`{PRIMARY_COLOR}`,backgroundLogo:!0}}"

    for path in chunks:
        text = path.read_text()
        if old_theme in text:
            text = text.replace(old_theme, new_theme)
            path.write_text(text)
            return  # Patched successfully

    raise RuntimeError(f"{webroot}/assets: expected hardcoded default theme not found in any state chunk")


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
    patch_state_chunk(webroot)


if __name__ == "__main__":
    main()
