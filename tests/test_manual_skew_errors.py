"""Manual command errors versus the independent, pinned virtual-SD worker.

Uses the same optional pinned-source inputs as the AFC integration suite.
Actuators and reactor scheduling are doubled; dispatcher, pause and SD code are real.
"""
import importlib.util
import inspect
import io
import logging
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
from types import SimpleNamespace as NS
import unittest
from unittest.mock import Mock

import test_afc_skew_integration as integration
import test_klipper_parser as parser
from test_skew_macros import MacroError


class ManualErrorTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        integration.AFCIntegrationTests.setUpClass()
        cls.addClassCleanup(integration.AFCIntegrationTests.doClassCleanups)
        parser.PinnedParserTests.setUpClass()
        cls.addClassCleanup(parser.PinnedParserTests.doClassCleanups)
        cls.firmware = parser.PinnedParserTests.firmware
        cls.kalico = Path(os.environ['COSMOS_KALICO_SOURCE']) / 'klippy'
        temporary = tempfile.TemporaryDirectory(prefix='cosmos-pause-errors-')
        cls.addClassCleanup(temporary.cleanup)
        dest = Path(temporary.name)
        (dest / 'klippy/extras').mkdir(parents=True)
        shutil.copy2(cls.kalico / 'extras/pause_resume.py', dest / 'klippy/extras/pause_resume.py')
        recipe = integration.ROOT / 'meta-opencentauri/recipes-apps/klipper/kalico_2026.02.00.bb'
        if not os.environ.get('KALICO_TEST_UNPATCHED'):
            for name in re.findall(r'file://([^\s"\\]+\.patch)', recipe.read_text()):
                patch = recipe.parent / 'files' / name
                subprocess.run(['git', 'apply', '--check', str(patch)], cwd=dest, check=True, capture_output=True)
                subprocess.run(['git', 'apply', str(patch)], cwd=dest, check=True, capture_output=True)
        spec = importlib.util.spec_from_file_location('cosmos_pause_errors', dest / 'klippy/extras/pause_resume.py')
        assert spec is not None and spec.loader is not None
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        cls.pause_class = module.PauseResume
        cls.sd_class = integration.extract_class(cls.kalico / 'extras/virtual_sdcard.py', 'VirtualSD',
            ['is_active', 'do_pause', 'do_resume', 'work_handler'], dict(logging=logging, sys=sys))

    def printing(self, with_afc=True, variant='cc1'):
        runner, afc = integration.AFCIntegrationTests().setup_print(variant)
        if with_afc:
            runner.objects['AFC'] = afc
        else:
            runner.printer.pop('AFC')
        events = {'gcode:command_error': [runner.move.reset_last_position]}
        runner.firmware_printer.register_event_handler = lambda name, cb: events.setdefault(name, []).append(cb)
        runner.firmware_printer.get_reactor = lambda: NS(monotonic=lambda: 0.)
        runner.gcode.register_command = lambda *args, **kwargs: None
        runner.objects.update(gcode=runner.gcode, webhooks=NS(register_endpoint=lambda *args: None))
        config = NS(get_printer=lambda: runner.firmware_printer, getfloat=lambda name, default: default)
        runner.pause = self.pause_class(config)
        runner.objects['pause_resume'] = afc.error.pause_resume = runner.pause
        runner.sync()
        dispatcher = self.firmware.GCodeDispatch.__new__(self.firmware.GCodeDispatch)

        class Mutex:
            def __enter__(self): return self
            def __exit__(self, *args): return False
            def test(self): return False

        def send_event(name):
            for callback in events.get(name, []):
                callback()
            runner.sync()

        dispatcher.__dict__.update(_script_context=0, mutex=Mutex(), error=MacroError,
            is_printer_ready=True, is_fileinput=False,
            output_callbacks=[], _respond_error=lambda msg: None, printer=NS(send_event=send_event),
            gcode_handlers={name: (lambda cmd, macro=name: runner.run(macro)) for name in ('KICK', 'PURGE')})
        sd = self.sd_class.__new__(self.sd_class)
        pending = []
        stats = NS(note_start=lambda: runner.printer['print_stats'].update(state='printing'),
            note_pause=lambda: runner.printer['print_stats'].update(state='paused'),
            note_complete=lambda: runner.printer['print_stats'].update(state='complete'),
            note_error=lambda msg: runner.printer['print_stats'].update(state='error'))
        sd.__dict__.update(work_timer='active', cmd_from_sd=False, must_pause_work=False,
            current_file=io.StringIO('G1 X100 Y100 Z10\n'), file_position=0, next_file_position=0,
            print_stats=stats, gcode=NS(get_mutex=lambda: dispatcher.mutex, run_script=runner.script,
                respond_raw=lambda *args: None, error=MacroError))

        def pause(when):
            if sd.must_pause_work and sd.work_timer is not None:
                sd.work_handler(0.)

        sd.reactor = NS(NOW=0., NEVER=float('inf'), monotonic=lambda: 0., pause=pause,
            unregister_timer=lambda timer: None,
            register_timer=lambda cb, when: pending.append(cb) or 'pending')
        runner.pause.v_sd = sd
        return runner, afc, dispatcher, sd, pending

    @staticmethod
    def enable_fan_state(runner, speed=.6):
        # Keep the real fan macros and parser; double only the final PWM write.
        runner.parse_params = lambda name, rawparams: parser.PinnedParserTests().parse_command(
            'G28' if name == '__HOMING_OVERRIDE' else name, rawparams).get_command_parameters()
        runner.printer['fan'] = {'speed': speed}
        runner.stubs.difference_update({'_SAVE_FAN_STATE', '_RESTORE_FAN_STATE', 'M106', 'M107'})
        runner.stubs.add('M006')
        original = runner.command

        def command(cmd, args='', owner='test'):
            if cmd == 'M006':
                gcmd = parser.PinnedParserTests().parse_command(cmd, args)
                runner.printer['fan']['speed'] = float(gcmd.get('S', 255.)) / 255.
            return original(cmd, args, owner)

        runner.command = command

    def test_manual_execution_failure_stops_sd_before_next_print_move(self):
        for with_afc in (False, True):
            with self.subTest(afc=with_afc):
                runner, afc, dispatcher, sd, pending = self.printing(with_afc)
                self.enable_fan_state(runner)
                runner.fail_command = 'G1'
                with self.assertRaises(MacroError):
                    dispatcher.run_script('KICK')
                self.assertTrue(runner.pause.is_paused)
                self.assertFalse(sd.is_active())
                self.assertFalse(runner.skew)
                self.assertEqual(runner.printer['gcode_macro LOAD_SKEW_IF_EXISTS']['phase'], 'paused')
                self.assertIn('PAUSE_STATE', runner.move.saved_states)
                if with_afc:
                    self.assertTrue(afc.position_saved)
                runner.fail_command = None
                runner.native_moves.clear()
                sd.work_handler(0.)
                self.assertFalse(runner.native_moves, 'failed maintenance must not release another SD move')
                if with_afc:
                    afc.error.cmd_AFC_RESUME(integration.Command())
                else:
                    runner.run('RESUME')
                self.assertTrue(runner.skew)
                self.assertFalse(runner.pause.is_paused)
                runner.native_moves.clear()
                pending.pop()(0.)
                self.assertAlmostEqual(runner.native_moves[0][0][0], 99.)
                self.assertEqual(runner.native_moves[0][2], (.01, 0., 0.))
                self.assertAlmostEqual(runner.printer['fan']['speed'], .6)


    def test_rejected_kick_keeps_the_next_print_move_corrected(self):
        for variant in ('cc1', 'canvas', 'cc2'):
            with self.subTest(variant=variant):
                runner, afc, dispatcher, sd, pending = self.printing(variant=variant)
                runner.printer['extruder']['target'] = 170
                with self.assertRaises(MacroError):
                    dispatcher.run_script('KICK')
                self.assertFalse(runner.pause.is_paused)
                self.assertTrue(sd.is_active())
                self.assertFalse(runner.native_moves)
                sd.work_handler(0.)
                self.assertAlmostEqual(runner.native_moves[0][0][0], 99.)
                self.assertEqual(runner.native_moves[0][2], (.01, 0., 0.))

    def test_nested_render_failure_stops_sd_without_extra_recovery_motion(self):
        runner, afc, dispatcher, sd, pending = self.printing()
        runner.printer['extruder']['can_extrude'] = False
        with self.assertRaises(MacroError):
            dispatcher.run_script('PURGE')
        self.assertTrue(runner.pause.is_paused)
        self.assertFalse(sd.is_active())
        self.assertFalse(runner.skew)
        self.assertTrue(runner.native_moves)
        self.assertFalse([move for move in runner.native_moves if any(move[2])])
        self.assertNotIn('PAUSE', [cmd for _, cmd, _, _ in runner.trace],
                         'the error boundary must not call the parking macro')
        runner.native_moves.clear()
        sd.work_handler(0.)
        self.assertFalse(runner.native_moves)

    def test_outer_maintenance_moves_are_uncorrected(self):
        for variant in ('cc1', 'canvas', 'cc2'):
            for name, args in (('M191', 'S40'), ('_CALIBRATE_ALL_STEP_2', '')):
                with self.subTest(variant=variant, command=name):
                    runner, afc, dispatcher, sd, pending = self.printing(variant=variant)
                    runner.parse_params = lambda name, rawparams: parser.PinnedParserTests().parse_command(
                        'G28' if name == '__HOMING_OVERRIDE' else name, rawparams).get_command_parameters()
                    runner.printer['temperature_sensor chamber'] = {'temperature': 40.}
                    runner.printer['gcode_macro _COSMOS_SETTINGS'] = {
                        'full_calibrate_hotend_temperature': 230, 'full_calibrate_bed_temperature': 60}
                    # Hardware calibration and the save dialog stay inert; actual
                    # positioning, nested macros, parser and transforms execute.
                    runner.stubs.update({'RUN_SHELL_COMMAND', 'SHAPER_CALIBRATE',
                        'PID_CALIBRATE', 'ACCELEROMETER_DEBUG_READ', '_SAVE_CONFIG_PROMPT'})
                    runner.run(name, args)
                    self.assertTrue(runner.native_moves)
                    self.assertFalse(any(any(move[2]) for move in runner.native_moves))
                    self.assertTrue(any(abs(move[0][0] - 128.) < 1e-8 and
                                        abs(move[0][1] - 128.) < 1e-8 for move in runner.native_moves))
                    self.assertTrue(runner.skew)

    def test_failed_start_defers_skew_until_explicit_resume(self):
        runner, afc, dispatcher, sd, pending = self.printing()
        runner.run('CLEAR_SKEW_IF_EXISTS')
        afc.current = None
        lane = afc.lanes['lane1']
        lane.extruder_obj.lane_loaded = None
        lane.load_state = False
        lane.current_map = 'T0'
        lane.unit_obj.lane_fault = Mock()
        afc.tool_cmds = {'T0': 'lane1'}
        error_class = integration.AFCIntegrationTests.classes['error']
        afc.error.AFC_error = error_class.AFC_error.__get__(afc.error)
        afc.error.set_error_state = error_class.set_error_state.__get__(afc.error)
        source = Path(integration.AFCIntegrationTests.temp.name) / 'extras/AFC_error.py'
        failure_class = integration.extract_class(source, 'afcError', ['handle_lane_failure'],
            dict(inspect=inspect, AFCLaneState=NS(ERROR='error')))
        afc.error.handle_lane_failure = failure_class.handle_lane_failure.__get__(afc.error)
        # Match AFC's installed command remap. The empty-lane check itself
        # synchronously pauses; no artificial callback bypasses the G-code lock.
        runner.macros['COSMOS_PAUSE'] = runner.macros.pop('PAUSE')
        afc.error.AFC_RENAME_PAUSE_NAME = 'COSMOS_PAUSE'
        original = runner.command

        def command(cmd, args='', owner='test'):
            if cmd == 'T0':
                return afc.cmd_CHANGE_TOOL(integration.Command(args, 'T0'))
            if cmd == 'PAUSE':
                return afc.error.cmd_AFC_PAUSE(integration.Command(args, 'PAUSE'))
            out = original(cmd, args, owner)
            if cmd == 'PAUSE_BASE' and sd.cmd_from_sd:
                # note_pause happens only after the current SD script returns.
                runner.printer['print_stats']['state'] = 'printing'
            return out

        runner.command = command
        dispatcher.gcode_handlers['PRINT_START'] = lambda cmd: runner.run(
            'PRINT_START', cmd.get_raw_command_parameters())
        sd.gcode.run_script = dispatcher.run_script
        sd.current_file = io.StringIO(
            'PRINT_START BED=60 EXTRUDER=230 CHAMBER=0 TOOL=0\nG1 X100 Y100 Z10\n')
        sd.on_error_gcode = NS(render=lambda: '')
        with self.assertLogs(level='WARNING'):
            sd.work_handler(0.)
        self.assertEqual(lane.unit_obj.lane_fault.call_count, 1)
        self.assertTrue(runner.pause.is_paused)
        self.assertFalse(sd.is_active())
        self.assertTrue(afc.error_state)
        self.assertFalse(runner.skew)
        self.assertEqual(runner.printer['gcode_macro LOAD_SKEW_IF_EXISTS']['phase'], 'paused')
        # The operator repairs the lane, then uses the real AFC recovery path.
        lane.load_state = True
        lane.extruder_obj.lane_loaded = afc.current = lane.name
        with self.assertLogs(level='WARNING'):
            afc.error.cmd_AFC_RESUME(integration.Command())
        self.assertFalse(runner.pause.is_paused)
        self.assertTrue(runner.skew)
        runner.native_moves.clear()
        sd.gcode.run_script = runner.script
        pending.pop()(0.)
        self.assertAlmostEqual(runner.native_moves[0][0][0], 99.)
        self.assertEqual(runner.native_moves[0][2], (.01, 0., 0.))

    def test_error_outside_configured_print_skew_does_not_pause(self):
        for state in ('no_profile', 'off', 'enabled', 'no_cosmos'):
            with self.subTest(state=state):
                runner, afc, dispatcher, sd, pending = self.printing()
                if state == 'no_profile':
                    runner.run('CLEAR_SKEW_IF_EXISTS')
                    runner.transform.skew_profiles.clear()
                    runner.printer['gcode_macro LOAD_SKEW_IF_EXISTS']['phase'] = 'printing'
                elif state == 'off':
                    runner.run('CLEAR_SKEW_IF_EXISTS')
                elif state == 'no_cosmos':
                    runner.objects.pop('gcode_macro LOAD_SKEW_IF_EXISTS')
                # Invalid KICK is rejected without touching the existing state.
                runner.printer['extruder']['target'] = 170
                with self.assertRaises(MacroError):
                    dispatcher.run_script('KICK')
                self.assertFalse(runner.pause.is_paused)
                self.assertTrue(sd.is_active())
                self.assertFalse(afc.position_saved)

    def test_error_from_sd_stops_without_waiting_for_its_own_worker(self):
        runner, afc, dispatcher, sd, pending = self.printing()
        sd.current_file = io.StringIO('KICK\nG1 X100 Y100 Z10\n')
        sd.on_error_gcode = NS(render=lambda: '')
        sd.gcode.run_script = dispatcher.run_script
        runner.fail_command = 'G1'
        sd.work_handler(0.)
        self.assertTrue(runner.pause.is_paused)
        self.assertFalse(sd.is_active())
        self.assertFalse(runner.skew)
        self.assertEqual(runner.printer['print_stats']['state'], 'error')
        self.assertFalse(pending)
        self.assertFalse([move for move in runner.native_moves if any(move[2])])


if __name__ == '__main__':
    unittest.main()
