"""Validate rendered macro arguments with the exact pinned Kalico parser.

Optional offline integration suite: set COSMOS_KALICO_SOURCE to a source root
containing klippy/gcode.py from the recipe's pin. No firmware/hardware is started.
"""
import hashlib
import importlib.util
import os
from pathlib import Path
import sys
import types
import unittest

from test_skew_macros import FILES, MacroRunner


@unittest.skipUnless(os.environ.get("COSMOS_KALICO_SOURCE"),
                     "Set COSMOS_KALICO_SOURCE for pinned-firmware parser tests")
class PinnedParserTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        path = Path(os.environ["COSMOS_KALICO_SOURCE"]) / "klippy/gcode.py"
        expected = "b97ecaf736a2417f06a443f2be353e9a72cba4f16233f2ac379420cb380d480e"
        if hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise RuntimeError("Expected gcode.py from Kalico 26e46bd536cd5b55c4028dd2409a2210262fc9ed")
        recipe = (FILES.parent / "kalico_2026.02.00.inc").read_text()
        if 'SRCREV = "26e46bd536cd5b55c4028dd2409a2210262fc9ed"' not in recipe:
            raise RuntimeError("Update the parser integration fixture for the new Kalico pin")
        package = "_cosmos_parser_test"
        # gcode imports mathutil for unrelated numeric conversion commands.
        parent = types.ModuleType(package)
        setattr(parent, "mathutil", types.ModuleType(package + ".mathutil"))
        sys.modules[package] = parent
        cls.addClassCleanup(sys.modules.pop, package, None)
        spec = importlib.util.spec_from_file_location(package + ".gcode", path)
        if spec is None or spec.loader is None:
            raise RuntimeError("Unable to load pinned Kalico parser")
        firmware = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(firmware)
        cls.firmware = firmware

    def parse_command(self, cmd, args=""):
        # Execute the pinned traditional dispatcher, not a hand-split stand-in.
        # Its handler captures the GCodeCommand before any template is rendered.
        parsed = []
        dispatcher = self.firmware.GCodeDispatch.__new__(self.firmware.GCodeDispatch)
        dispatcher._script_context = 0
        dispatcher.output_callbacks = []
        dispatcher.gcode_handlers = {cmd: parsed.append}
        dispatcher._process_commands([cmd + " " + args], need_ack=False)
        self.assertEqual(len(parsed), 1)
        gcmd = parsed[0]
        if not dispatcher.is_traditional_gcode(cmd):
            dispatcher._get_extended_params(gcmd)
        return gcmd

    def runner(self):
        parse_command = self.parse_command

        class ParserRunner(MacroRunner):
            @staticmethod
            def parse_params(name, rawparams):
                cmd = "G28" if name == "__HOMING_OVERRIDE" else name
                return parse_command(cmd, rawparams).get_command_parameters()

            def command(self, cmd, args="", owner="test"):
                parse_command(cmd, args)
                super().command(cmd, args, owner)

        return ParserRunner()

    def test_traditional_homing_params_match_offline_runner(self):
        for axes, expected in (
            ("", {"G": "28"}),
            ("X", {"G": "28", "X": ""}),
            ("Y", {"G": "28", "Y": ""}),
            ("Z", {"G": "28", "Z": ""}),
            ("XY", {"G": "28", "XY": ""}),
            ("X Y", {"G": "28", "X": "", "Y": ""}),
            ("X0 Y0", {"G": "28", "X": "0", "Y": "0"}),
            ("XYZ", {"G": "28", "XYZ": ""}),
        ):
            with self.subTest(axes=axes):
                actual = self.parse_command("G28", axes).get_command_parameters()
                self.assertEqual(actual, expected)
                self.assertEqual(MacroRunner.parse_params("__HOMING_OVERRIDE", axes), actual)

    def test_native_parser_rejects_bare_axes_for_extended_macro(self):
        runner = self.runner()
        with self.assertRaises(self.firmware.CommandError):
            runner.command("_HOME_WITHOUT_SKEW", "XY")

    def test_partial_homing_macro_arguments_pass_native_parser(self):
        for axes, forwarded, expected in (
            ("", "XYZ", ["Y", "X", "Y", "Z"]),
            ("X", "X", ["X"]),
            ("Y", "Y", ["Y"]),
            ("Z", "Z", ["Z"]),
            ("XY", "XY", ["Y", "X", "Y"]),
            ("X Y", "XY", ["Y", "X", "Y"]),
            ("X0 Y0", "XY", ["Y", "X", "Y"]),
            ("XYZ", "XYZ", ["Y", "X", "Y", "Z"]),
        ):
            with self.subTest(axes=axes):
                runner = self.runner()
                runner.run("LOAD_SKEW_IF_EXISTS")
                runner.command("G28", axes)
                homes = [args for owner, cmd, args, _ in runner.trace
                         if owner == "_HOME_WITHOUT_SKEW" and cmd == "G28"]
                self.assertEqual(homes, expected)
                forwarded_args = [args for _, cmd, args, _ in runner.trace
                                  if cmd == "_HOME_WITHOUT_SKEW"]
                self.assertEqual(forwarded_args, ["AXES=" + forwarded])
                self.assertTrue(runner.skew)
                self.assertFalse(any(move[2] for move in runner.moves))

    def test_conditional_compact_xy_homing_never_native_homes_z(self):
        runner = self.runner()
        runner.printer["toolhead"]["homed_axes"] = "z"
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.run("_CG28", "AXIS=XY")
        homes = [args for owner, cmd, args, _ in runner.trace
                 if owner == "_HOME_WITHOUT_SKEW" and cmd == "G28"]
        self.assertEqual(homes, ["Y", "X", "Y"])
        self.assertFalse(any(move[2] for move in runner.moves))
        self.assertTrue(runner.skew)

    def test_cancel_reaches_heater_shutdown_with_native_parser(self):
        runner = self.runner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.run("CANCEL_PRINT")
        homes = [args for owner, cmd, args, _ in runner.trace
                 if owner == "_HOME_WITHOUT_SKEW" and cmd == "G28"]
        self.assertEqual(homes, ["Y", "X", "Y"])
        self.assertFalse(any(move[2] for move in runner.moves))
        self.assertFalse(runner.skew)
        self.assertTrue(any(cmd == "TURN_OFF_HEATERS" for _, cmd, _, _ in runner.trace))

    def test_z_calibration_passes_native_parser(self):
        runner = self.runner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.run("CALIBRATE_Z_OFFSET")
        self.assertTrue(runner.skew)
        self.assertFalse(any(move[2] for move in runner.moves))


if __name__ == "__main__":
    unittest.main()
