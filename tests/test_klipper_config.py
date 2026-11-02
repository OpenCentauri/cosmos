"""Load actual COSMOS configs through recipe-patched Kalico, without hardware.

Optional offline test: COSMOS_KALICO_SOURCE supplies the unpatched recipe pin.
The hashed files below (including import dependencies) are copied and patched in
scratch; a Git checkout is not required. Requires Jinja2 and GNU patch. Printer
registration is doubled; config parsing and native template loading are real.
"""
import hashlib
import importlib
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile
from types import ModuleType, SimpleNamespace as NS
import unittest


ROOT = Path(__file__).resolve().parents[1]
RECIPE = ROOT / "meta-opencentauri/recipes-apps/klipper"
FILES = RECIPE / "files"
PIN = "26e46bd536cd5b55c4028dd2409a2210262fc9ed"
HASHES = {
    "configfile.py": "569e002ff1fae1875ed06bf7c867747af5c8d3fb198f78d9458c004dc8d52634",
    "mathutil.py": "2731edcd60b6c34a2c92e6a94d80d04e54b576883951af265aff93d90b395b43",
    "queuelogger.py": "1b29231a75e1d77c0e29d52442fbe1d80db7d73b6257cf0d94d0d2281147de0b",
    "extras/__init__.py": "eae347ebaaf733905470e1b9cd4f40cdcf770f7d8e25d7db92b1b1945e26b8e3",
    "extras/danger_options.py": "40af79d5ed1b272404193a92a8b801493652d14858777965e8ab743d981fcbb2",
    "extras/gcode_macro.py": "7f4ecbce7ebed664c215ede99280972830a10584ed99bc6e48167db03fef0b9b",
    "extras/homing_override.py": "9689dde76c7e726d9b60ac713bdee5125d5df7f6095366d00634dc5d68125a91",
}
CANVAS = "centauri-carbon-1/canvas-toolhead.cfg"


@unittest.skipUnless(os.environ.get("COSMOS_KALICO_SOURCE"),
                     "Set COSMOS_KALICO_SOURCE for pinned config-loading tests")
class PinnedConfigTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        source = Path(os.environ["COSMOS_KALICO_SOURCE"]) / "klippy"
        recipe = (RECIPE / "kalico_2026.02.00.inc").read_text()
        if f'SRCREV = "{PIN}"' not in recipe:
            raise AssertionError("Kalico pin changed: refresh config-loading fixture")
        cls.temp = tempfile.TemporaryDirectory(prefix="cosmos-config-loading-")
        cls.addClassCleanup(cls.temp.cleanup)
        dest = Path(cls.temp.name)
        for name, digest in HASHES.items():
            data = (source / name).read_bytes()
            if hashlib.sha256(data).hexdigest() != digest:
                raise AssertionError(f"{name} differs from pinned Kalico {PIN}")
            target = dest / "klippy" / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
        recipe += (RECIPE / "kalico_2026.02.00.bb").read_text()
        for name in re.findall(r'file://([^\s"\\]+\.patch)', recipe):
            patch = FILES / name
            if not any(f"+++ b/klippy/{module}\n" in patch.read_text()
                       for module in HASHES):
                continue
            # The existing SAVE_CONFIG patch lacks a final newline: use recipe
            # patch semantics, not git apply, and prohibit fuzzy matching.
            command = ["patch", "--batch", "--fuzz=0", "-p1", "-i", str(patch)]
            subprocess.run(command + ["--dry-run"], cwd=dest, check=True,
                           capture_output=True, text=True)
            subprocess.run(command, cwd=dest, check=True,
                           capture_output=True, text=True)

        # gcode_macro imports klippy by name. Avoid booting klippy.__init__ and
        # restore every prior klippy module even when an import fails.
        saved = {name: module for name, module in sys.modules.items()
                 if name == "klippy" or name.startswith("klippy.")}
        try:
            for name in saved:
                del sys.modules[name]
            package = ModuleType("klippy")
            package.__path__ = [str(dest / "klippy")]
            sys.modules["klippy"] = package
            cls.configfile = importlib.import_module("klippy.configfile")
            cls.gcode_macro = importlib.import_module("klippy.extras.gcode_macro")
            cls.homing_override = importlib.import_module("klippy.extras.homing_override")
        finally:
            for name in tuple(sys.modules):
                if name == "klippy" or name.startswith("klippy."):
                    del sys.modules[name]
            sys.modules.update(saved)

    def parse_config(self, paths):
        entry = Path(self.temp.name) / "printer.cfg"
        entry.write_text("".join(f"[include {FILES / path}]\n" for path in paths))
        objects = {"gcode": NS(error=RuntimeError, ready_gcode_handlers={},
                               register_command=lambda *args, **kwargs: None),
                   "homing": object()}
        printer = NS(lookup_object=lambda name, default=None: objects.get(name, default),
                     load_object=lambda config, name: objects[name],
                     config_error=RuntimeError, command_error=RuntimeError)
        config = self.configfile.PrinterConfig(printer).read_config(str(entry))
        macro = self.gcode_macro.PrinterGCodeMacro(config)
        objects["gcode_macro"] = macro
        return config, macro

    def test_model_templates_load_after_native_config_parsing(self):
        for variant in ("cc1", "canvas", "cc2"):
            with self.subTest(variant=variant):
                machine = "centauri-carbon-2" if variant == "cc2" else "centauri-carbon-1"
                paths = ["calibration.cfg", "shared-macros.cfg", f"{machine}/macros.cfg"]
                if variant == "canvas":
                    paths.append(CANVAS)
                config, macro = self.parse_config(paths)
                self.homing_override.HomingOverride(config.getsection("homing_override"))
                loaded = set()
                for section in config.fileconfig.sections():
                    for option in config.fileconfig.options(section):
                        if option == "gcode" or option.endswith("_gcode"):
                            with self.subTest(section=section, option=option):
                                template = macro.load_template(config.getsection(section), option)
                                loaded.add(template.name)
                self.assertIn("homing_override:gcode", loaded)
                self.assertIn("gcode_macro _CALIBRATE_ALL_STEP_2:gcode", loaded)
                self.assertIn("gcode_macro CLEAN_NOZZLE:gcode", loaded)
                self.assertIn("gcode_macro _CLEAN_NOZZLE:gcode", loaded)

    def test_canvas_inherits_public_clean_and_keeps_private_override(self):
        base = ["calibration.cfg", "shared-macros.cfg", "centauri-carbon-1/macros.cfg"]
        cc1, _ = self.parse_config(base)
        canvas, _ = self.parse_config(base + [CANVAS])
        overrides, _ = self.parse_config([CANVAS])
        public, private = "gcode_macro CLEAN_NOZZLE", "gcode_macro _CLEAN_NOZZLE"
        self.assertFalse(overrides.has_section(public),
                         "CANVAS must inherit the CC1 public safety wrapper")
        self.assertEqual(dict(canvas.fileconfig.items(public)),
                         dict(cc1.fileconfig.items(public)))
        self.assertEqual(dict(canvas.fileconfig.items(private)),
                         dict(overrides.fileconfig.items(private)))
        self.assertNotEqual(canvas.getsection(private).get("gcode"),
                            cc1.getsection(private).get("gcode"))

        # Reintroducing the inherited wrapper must not change any merged option.
        wrapper = cc1.getsection(public).get("gcode").strip()
        duplicate = Path(self.temp.name) / "redundant-canvas.cfg"
        duplicate.write_text(f"[{public}]\ngcode:\n"
                             + "\n".join("    " + line for line in wrapper.splitlines())
                             + "\n\n" + (FILES / CANVAS).read_text())
        original, _ = self.parse_config(base + [duplicate])
        self.assertEqual(
            {s: dict(canvas.fileconfig.items(s)) for s in canvas.fileconfig.sections()},
            {s: dict(original.fileconfig.items(s)) for s in original.fileconfig.sections()})


if __name__ == "__main__":
    unittest.main()
