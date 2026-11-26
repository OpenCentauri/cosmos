#!/usr/bin/env python3
import importlib.util
import tempfile
from pathlib import Path


module_path = Path(__file__).with_name("apply-opencentauri-fluidd-theme.py")
spec = importlib.util.spec_from_file_location("apply_fluidd_theme", module_path)
assert spec is not None and spec.loader is not None
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

old_theme = "theme:{isDark:!0,logo:{src:`logo_fluidd.svg`},color:`#2196F3`,backgroundLogo:!0}"
new_theme = f"theme:{{isDark:!0,logo:{{src:`{module.LOGO_SRC}`}},color:`{module.PRIMARY_COLOR}`,backgroundLogo:!0}}"

with tempfile.TemporaryDirectory() as tmp:
    assets = Path(tmp) / "assets"
    assets.mkdir()
    bundle = assets / "index-test.js"

    bundle.write_text(f"const defaults={{{old_theme}}};")
    module.patch_state_chunk(assets.parent)
    assert bundle.read_text() == f"const defaults={{{new_theme}}};"

    bundle.write_text(old_theme + old_theme)
    module.patch_state_chunk(assets.parent)
    assert bundle.read_text() == new_theme + old_theme

    bundle.write_text("const defaults={};")
    try:
        module.patch_state_chunk(assets.parent)
    except RuntimeError:
        pass
    else:
        raise AssertionError("missing stock default must fail loudly")

print("default theme patch handles main bundles, duplicates, and missing defaults")
