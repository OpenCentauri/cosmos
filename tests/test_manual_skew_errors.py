"""Manual command errors versus the independent, pinned virtual-SD worker.

Uses the same optional pinned-source inputs as the AFC integration suite.
Actuators and reactor scheduling are doubled; dispatcher, pause and SD code are real.
"""
import importlib.util
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

    def test_manual_execution_failure_stops_sd_before_next_print_move(self):
        for with_afc in (False, True):
            with self.subTest(afc=with_afc):
                runner, afc, dispatcher, sd, pending = self.printing(with_afc)
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
