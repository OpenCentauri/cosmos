"""Regression checks for actual COSMOS Jinja macros; no printer connection.

This runner models command ordering, macro state and skew enablement, not
kinematics, heaters, bed mesh, or firmware motion. Run with:
    python -m unittest discover -s tests -v
Requires Jinja2 (also required by the firmware build).
"""
import ast
import copy
from pathlib import Path
import re
import shlex
import unittest

import jinja2

FILES = Path(__file__).resolve().parents[1] / "meta-opencentauri/recipes-apps/klipper/files"


class MacroError(Exception):
    pass


class MacroRunner:
    def __init__(self, variant="cc1", profile=True, printing=True, paused=False):
        self.macros = {}
        self.variables = {}
        self.sections = {}
        machine = "centauri-carbon-2" if variant == "cc2" else "centauri-carbon-1"
        paths = [FILES / "client.cfg", FILES / "shared-macros.cfg",
                 FILES / "calibration.cfg", FILES / machine / "macros.cfg"]
        if variant == "canvas":
            paths.append(FILES / machine / "canvas-toolhead.cfg")
        for path in paths:
            for match in re.finditer(r"^\[([^\]]+)\]\n(.*?)(?=^\[|\Z)", path.read_text(), re.M | re.S):
                section, body = match.groups()
                before, sep, code = body.partition("gcode:")
                self.sections[section] = code if sep else body
                if not section.startswith("gcode_macro "):
                    continue
                name = section.split(" ", 1)[1]
                variables = self.variables.setdefault(section, {})
                for key, value in re.findall(r"^variable_(\w+)\s*:\s*(.+)$", before, re.M):
                    variables[key] = ast.literal_eval(value.split("#", 1)[0].strip())
                if sep:
                    self.macros[name] = code
        self.printer = copy.deepcopy(self.variables)
        self.macros["__HOMING_OVERRIDE"] = self.sections["homing_override"]
        self.printer.update({
            "toolhead": {"homed_axes": "xyz"},
            "extruder": {"target": 230, "temperature": 230, "can_extrude": True},
            "heater_bed": {"target": 60},
            "pause_resume": {"is_paused": paused},
            "print_stats": {"state": "paused" if paused else "printing" if printing else "standby"},
            "configfile": {"config": {"skew_correction my_skew_profile": {}} if profile else {},
                           "settings": {"extruder": {"min_extrude_temp": 170}}},
            "gcode_move": {"position": {"x": 128, "y": 128, "z": 100}},
            "bed_mesh": {"profiles": {"default": {}}},
            "gcode_macro _CHECK_CALIBRATION_VARS": {"all_calibrated": True},
        })
        self.printer["gcode_macro _global_var"]["target_extruder"] = 230
        if profile:
            self.printer["skew_correction"] = {"current_profile_name": ""}
        self.env = jinja2.Environment(
            block_start_string="{%", block_end_string="%}",
            variable_start_string="{", variable_end_string="}",
            undefined=jinja2.StrictUndefined,
        )
        self.skew = False
        self.trace = []
        self.moves = []
        self.fail_command: str | None = None
        self.hooks = {}
        self.active_macros = set()
        # Commands unrelated to this test's state are intentionally inert.
        self.stubs = {"SET_DISPLAY_TEXT", "_SAVE_FAN_STATE", "_RESTORE_FAN_STATE",
                      "TURN_OFF_FANS", "RESET_VELOCITY_LIMIT", "M106", "M107",
                      "_CLOSE_PROMPT", "_SHOW_PROMPT", "SMART_PARK", "LINE_PURGE",
                      "_SAVE_Z_OFFSET", "_SCREWS_TILT_PROMPT"}
        self.inert_commands = {
            "BED_MESH_CALIBRATE_BASE", "BED_MESH_CLEAR", "BED_MESH_PROFILE",
            "COLD_EXTRUDE", "FORCE_MOVE", "G28", "G4", "G90", "G91",
            "LOAD_CELL_CALIBRATE", "M104", "M109", "M140", "M190", "M220",
            "M221", "M400", "M83", "M84", "PROBE", "RESPOND",
            "SAVE_GCODE_STATE", "SCREWS_TILT_CALCULATE_BASE", "SET_GCODE_OFFSET",
            "SET_TEMPERATURE_FAN_TARGET", "TURN_OFF_HEATERS", "TEMPERATURE_WAIT",
        }

    @staticmethod
    def parse_params(name, rawparams):
        if name in ("__HOMING_OVERRIDE", "M191"):
            # Match Kalico's traditional tokenization: XY is ONE key, not X/Y.
            command = "G28" if name == "__HOMING_OVERRIDE" else name
            parts = re.split(r"([A-Z_]+|[A-Z*])", (command + " " + rawparams).split(";", 1)[0].upper())
            return {parts[i]: parts[i + 1].strip() for i in range(1, len(parts), 2)}
        params = {}
        for token in shlex.split(rawparams, comments=True):
            if "=" not in token:
                raise MacroError("Malformed command: %s %s" % (name, rawparams))
            key, value = token.split("=", 1)
            params[key.upper()] = value
        return params

    def render(self, name, rawparams=""):
        params = self.parse_params(name, rawparams)
        context = copy.deepcopy(self.printer.get("gcode_macro " + name, {}))
        context.update(printer=copy.deepcopy(self.printer), params=params,
                       rawparams=rawparams, action_raise_error=self.error,
                       action_respond_info=lambda message: "")
        return self.env.from_string(self.macros[name]).render(**context)

    @staticmethod
    def error(message):
        raise MacroError(message)

    def run(self, name, rawparams=""):
        if name in self.active_macros:
            raise MacroError("Macro %s called recursively" % name)
        self.active_macros.add(name)
        try:
            self.execute_template(name, rawparams)
        finally:
            self.active_macros.remove(name)

    def execute_template(self, name, rawparams):
        # Like firmware, render the whole template before executing its commands.
        script = self.render(name, rawparams)
        for raw in script.splitlines():
            line = raw.strip()
            if not line or line.startswith(("#", ";")):
                continue
            line = line.split(";", 1)[0].split("#", 1)[0].strip()
            cmd, _, args = line.partition(" ")
            if cmd == "RETURN":
                return
            self.command(cmd, args.strip(), name)

    def command(self, cmd, args="", owner="test"):
        self.trace.append((owner, cmd, args, self.skew))
        if cmd == self.fail_command:
            raise MacroError("injected command failure")
        if cmd in self.hooks:
            self.hooks[cmd](self)
        if cmd in self.stubs:
            return
        if cmd in self.macros:
            self.run(cmd, args)
            return
        if cmd == "G28" and "__HOMING_OVERRIDE" not in self.active_macros:
            self.run("__HOMING_OVERRIDE", args)
            return
        params = dict(token.split("=", 1) for token in shlex.split(args) if "=" in token)
        if cmd == "SET_GCODE_VARIABLE":
            self.printer["gcode_macro " + params["MACRO"]][params["VARIABLE"]] = ast.literal_eval(params["VALUE"])
        elif cmd == "SET_SKEW":
            self.skew = False
            # Real Kalico deliberately retains current_profile_name on CLEAR.
        elif cmd == "SKEW_PROFILE":
            if "LOAD" in params:
                assert params["LOAD"] == "my_skew_profile"
                self.skew = True
                self.printer["skew_correction"]["current_profile_name"] = params["LOAD"]
        elif cmd in ("G0", "G1"):
            coords = dict(re.findall(r"([XYZ])([-+\d.]+)", args))
            if coords:
                self.moves.append((owner, cmd + " " + args, self.skew))
                self.printer["gcode_move"]["position"].update({k.lower(): float(v) for k, v in coords.items()})
        elif cmd == "PAUSE_BASE":
            self.printer["pause_resume"]["is_paused"] = True
            self.printer["print_stats"]["state"] = "paused"
        elif cmd == "RESUME_BASE":
            if self.printer["pause_resume"]["is_paused"]:
                self.printer["pause_resume"]["is_paused"] = False
                self.printer["print_stats"]["state"] = "printing"
        elif cmd == "CANCEL_PRINT_BASE":
            self.printer["print_stats"]["state"] = "cancelled"
        elif cmd == "CLEAR_PAUSE":
            self.printer["pause_resume"]["is_paused"] = False
        elif cmd not in self.inert_commands:
            raise MacroError("Unsupported command in test runner: " + cmd)


class MaintenanceSkewTests(unittest.TestCase):
    def test_rejected_kick_preserves_printing_correction(self):
        for variant in ("cc1", "canvas", "cc2"):
            for failure in ("target", "cold", "unhomed"):
                with self.subTest(variant=variant, failure=failure):
                    runner = MacroRunner(variant)
                    runner.run("LOAD_SKEW_IF_EXISTS")
                    if failure == "target":
                        runner.printer["extruder"]["target"] = 170
                    elif failure == "cold":
                        runner.printer["extruder"]["can_extrude"] = False
                    else:
                        runner.printer["toolhead"]["homed_axes"] = "z"
                    with self.assertRaises(MacroError):
                        runner.run("KICK")
                    self.assertTrue(runner.skew)
                    self.assertFalse(runner.moves)

    def test_partial_homing_preserves_axes_without_malformed_arguments(self):
        for axes, expected in (("X", ["X"]), ("Y", ["Y"]),
                               ("XY", ["Y", "X", "Y"]), ("Z", ["Z"]),
                               ("X Y", ["Y", "X", "Y"]), ("X0 Y0", ["Y", "X", "Y"]),
                               ("XYZ", ["Y", "X", "Y", "Z"]), ("", ["Y", "X", "Y", "Z"])):
            with self.subTest(axes=axes):
                runner = MacroRunner()
                runner.run("LOAD_SKEW_IF_EXISTS")
                runner.run("__HOMING_OVERRIDE", axes)
                homes = [args for _, cmd, args, _ in runner.trace if cmd == "G28"]
                self.assertEqual(homes, expected)
                self.assertFalse(any(move[2] for move in runner.moves))
                self.assertTrue(runner.skew)

    def test_idle_timeout_clears_stale_correction(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.printer["print_stats"]["state"] = "complete"
        runner.run("_IDLE_TIMEOUT")
        self.assertFalse(runner.skew)

    def test_completed_job_cannot_restore_stale_printing_state(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.printer["print_stats"]["state"] = "complete"
        runner.run("KICK")
        self.assertFalse(runner.skew)

    def test_idle_paused_and_unconfigured_maintenance_never_enables_skew(self):
        for variant in ("cc1", "canvas", "cc2"):
            for mode in ("idle", "paused", "no_profile", "preparation"):
                for macro in ("PURGE", "KICK", "MOVE_TO_TRAY", "CLEAN_NOZZLE", "CUT_FILAMENT"):
                    with self.subTest(variant=variant, mode=mode, macro=macro):
                        runner = MacroRunner(variant, profile=mode != "no_profile",
                                             printing=mode != "idle", paused=mode == "paused")
                        runner.run(macro)
                        self.assertFalse(runner.skew)
                        self.assertFalse(any(move[2] for move in runner.moves))
                        self.assertFalse(any(cmd == "SKEW_PROFILE" for _, cmd, _, _ in runner.trace))

    def test_early_tray_return_still_restores_outer_state(self):
        for variant, x in (("cc1", 202), ("canvas", 202), ("cc2", 52.5)):
            with self.subTest(variant=variant):
                runner = MacroRunner(variant)
                runner.printer["gcode_move"]["position"].update(x=x, y=264)
                runner.run("LOAD_SKEW_IF_EXISTS")
                runner.run("MOVE_TO_TRAY")
                self.assertFalse(runner.moves)
                self.assertTrue(runner.skew)

    def test_nested_purge_reloads_only_once_at_the_end(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.trace.clear()
        runner.run("PURGE")
        loads = [i for i, entry in enumerate(runner.trace) if entry[1] == "SKEW_PROFILE"]
        self.assertEqual(len(loads), 1)
        self.assertFalse(any(entry[1] in ("G0", "G1") for entry in runner.trace[loads[0] + 1:]))

    def test_maintenance_errors_leave_skew_off_and_resume_recovers(self):
        for failure in ("render", "command"):
            with self.subTest(failure=failure):
                runner = MacroRunner()
                runner.run("LOAD_SKEW_IF_EXISTS")
                if failure == "render":
                    runner.printer["extruder"]["can_extrude"] = False
                else:
                    runner.fail_command = "G1"
                with self.assertRaises(MacroError):
                    runner.run("PURGE" if failure == "render" else "KICK")
                self.assertFalse(runner.skew)
                runner.printer["extruder"]["can_extrude"] = True
                runner.fail_command = None
                runner.run("PAUSE")
                runner.run("RESUME")
                self.assertTrue(runner.skew)

    def test_pause_inside_maintenance_invalidates_its_restore(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.macros["_PURGE"] += "\nPAUSE\n"
        runner.run("PURGE")
        self.assertTrue(runner.printer["pause_resume"]["is_paused"])
        self.assertFalse(runner.skew)
        runner.run("RESUME")
        self.assertTrue(runner.skew)

    def test_failed_base_resume_leaves_skew_off(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.run("PAUSE")
        runner.fail_command = "RESUME_BASE"
        with self.assertRaises(MacroError):
            runner.run("RESUME")
        self.assertFalse(runner.skew)

    def test_purge_parameters_and_public_macro_variables_survive_wrapping(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.run("PURGE", "PURGE_LENGTH=51")
        self.assertTrue(any(cmd == "G1" and args == "E51.0 F300"
                            for _, cmd, args, _ in runner.trace))
        runner.printer["gcode_macro CUT_FILAMENT"]["cut_depth"] = 7.5
        runner.run("CUT_FILAMENT")
        self.assertTrue(any(move[1] == "G0 Y7.5 F6000" for move in runner.moves))
        runner.printer["gcode_macro KICK"]["end_kick_x"] = 179
        runner.run("KICK")
        self.assertTrue(any(move[1] == "G1 X179" for move in runner.moves))

    def test_only_fixed_profile_is_loaded(self):
        runner = MacroRunner(profile=False)
        runner.printer["configfile"]["config"]["skew_correction custom"] = {}
        runner.printer["skew_correction"] = {"current_profile_name": "custom"}
        runner.run("LOAD_SKEW_IF_EXISTS", "PROFILE=custom")
        self.assertFalse(runner.skew)
        self.assertFalse(any(cmd == "SKEW_PROFILE" for _, cmd, _, _ in runner.trace))

    def test_cancel_and_end_leave_correction_off(self):
        for macro in ("PRINT_END", "CANCEL_PRINT"):
            with self.subTest(macro=macro):
                runner = MacroRunner()
                runner.run("LOAD_SKEW_IF_EXISTS")
                runner.run(macro)
                self.assertFalse(runner.skew)
                self.assertFalse(any(move[2] for move in runner.moves))
                self.assertTrue(any(cmd == "TURN_OFF_HEATERS" for _, cmd, _, _ in runner.trace))

    def test_homing_and_calibration_moves_are_uncorrected(self):
        for macro in ("__HOMING_OVERRIDE", "LOADCELL_Z_HOME", "CALIBRATE_Z_OFFSET",
                      "BED_MESH_CALIBRATE", "MANUAL_LEVELING", "SCREWS_TILT_CALCULATE"):
            with self.subTest(macro=macro):
                runner = MacroRunner()
                runner.run("LOAD_SKEW_IF_EXISTS")
                runner.run(macro)
                self.assertTrue(runner.moves)
                self.assertFalse(any(move[2] for move in runner.moves))
                self.assertTrue(runner.skew)

    def test_outer_maintenance_keeps_lifecycle_ownership(self):
        for macro, args, boundary in (("M191", "S40", "TEMPERATURE_WAIT"),
                                      ("_CALIBRATE_ALL_STEP_2", "", "PID_CALIBRATE")):
            for mode in ("printing", "idle", "paused", "no_profile", "preparation", "interrupted"):
                with self.subTest(macro=macro, mode=mode):
                    runner = MacroRunner(profile=mode != "no_profile",
                                         printing=mode != "idle", paused=mode == "paused")
                    runner.printer['temperature_sensor chamber'] = {'temperature': 40.}
                    runner.printer['gcode_macro _COSMOS_SETTINGS'] = {
                        'full_calibrate_hotend_temperature': 230, 'full_calibrate_bed_temperature': 60}
                    runner.stubs.update({'RUN_SHELL_COMMAND', 'SHAPER_CALIBRATE', 'PID_CALIBRATE',
                                         'ACCELEROMETER_DEBUG_READ', '_SAVE_CONFIG_PROMPT'})
                    if mode in ("printing", "interrupted"):
                        runner.run("LOAD_SKEW_IF_EXISTS")
                    if mode == "interrupted":
                        runner.hooks[boundary] = lambda current: current.run("CLEAR_SKEW_IF_EXISTS")
                    runner.trace.clear()
                    runner.run(macro, args)
                    self.assertTrue(runner.moves)
                    self.assertFalse(any(move[2] for move in runner.moves))
                    self.assertEqual(runner.skew, mode == "printing")
                    loads = [cmd for _, cmd, _, _ in runner.trace if cmd == "SKEW_PROFILE"]
                    self.assertEqual(len(loads), int(mode == "printing"))

    def test_unload_suspends_skew_through_its_final_exit_move(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.run("UNLOAD_FILAMENT")
        self.assertFalse(any(move[2] for move in runner.moves))
        self.assertTrue(runner.skew)

    def test_pause_resume_restores_position_before_reloading_skew(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.run("PAUSE")
        self.assertFalse(runner.skew)
        runner.trace.clear()
        runner.run("RESUME")
        base = [entry for entry in runner.trace if entry[1] == "RESUME_BASE"]
        self.assertEqual(len(base), 1)
        self.assertFalse(base[0][3], "PAUSE_STATE was saved with correction off")
        self.assertTrue(runner.skew)
        self.assertFalse(runner.printer["pause_resume"]["is_paused"])

    def test_resume_during_preparation_does_not_enable_skew(self):
        runner = MacroRunner()
        # print_stats is already printing, but PRINT_START has not loaded skew.
        runner.run("PAUSE")
        runner.run("RESUME")
        self.assertFalse(runner.skew)

    def test_start_clears_stale_skew_before_preparation(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.trace.clear()
        runner.run("PRINT_START", "BED=60 EXTRUDER=230 CHAMBER=0")
        preparation = [entry for entry in runner.trace if entry[1] in ("G28", "SMART_PARK", "LINE_PURGE")]
        self.assertTrue(preparation)
        self.assertFalse(any(entry[3] for entry in preparation))
        self.assertTrue(runner.skew)

    def test_unpaused_resume_does_not_move_heat_or_load_skew(self):
        runner = MacroRunner(printing=False)
        runner.run("RESUME")
        self.assertFalse(runner.moves)
        self.assertFalse(runner.skew)
        self.assertFalse(any(cmd in ("M109", "G1", "SKEW_PROFILE")
                             for _, cmd, _, _ in runner.trace))

    def test_lifecycle_clear_invalidates_outer_maintenance_restore(self):
        runner = MacroRunner()
        runner.run("LOAD_SKEW_IF_EXISTS")
        runner.macros["_PURGE"] += "\nCLEAR_SKEW_IF_EXISTS\n"
        runner.run("PURGE")
        self.assertFalse(runner.skew, "an old wrapper must not undo lifecycle cleanup")

    def test_direct_maintenance_suspends_skew_and_restores(self):
        for variant in ("cc1", "canvas", "cc2"):
            for macro in ("KICK", "MOVE_TO_TRAY", "CLEAN_NOZZLE", "CUT_FILAMENT"):
                with self.subTest(variant=variant, macro=macro):
                    runner = MacroRunner(variant)
                    runner.run("LOAD_SKEW_IF_EXISTS")
                    runner.run(macro)
                    self.assertTrue(runner.moves)
                    self.assertFalse(any(move[2] for move in runner.moves),
                                     "maintenance emitted movement with skew enabled")
                    self.assertTrue(runner.skew)

    def test_purge_suspends_skew_for_every_nested_move_and_restores(self):
        for variant in ("cc1", "canvas", "cc2"):
            with self.subTest(variant=variant):
                runner = MacroRunner(variant)
                runner.run("LOAD_SKEW_IF_EXISTS")
                self.assertTrue(runner.skew)
                runner.run("PURGE")
                self.assertTrue(runner.moves)
                self.assertFalse([move for move in runner.moves if move[2]],
                                 "maintenance emitted movement with skew enabled")
                self.assertTrue(runner.skew, "print correction must resume after maintenance")


if __name__ == "__main__":
    unittest.main()
