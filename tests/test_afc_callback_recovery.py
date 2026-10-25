"""Pinned AFC callback recovery through native pause and the real SD worker.

Uses test_afc_skew_integration's optional pinned sources; never drives hardware.
Only actuators, sensor state and reactor scheduling are doubled.
"""
import io
import threading
from types import SimpleNamespace as NS
import unittest
from unittest.mock import Mock

import test_afc_skew_integration as integration
import test_manual_skew_errors as manual


class AFCCallbackRecoveryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        manual.ManualErrorTests.setUpClass()
        cls.addClassCleanup(manual.ManualErrorTests.doClassCleanups)
        cls.classes = integration.AFCIntegrationTests.classes

    def runout(self, variant='cc1'):
        runner, afc, dispatcher, sd, pending = manual.ManualErrorTests().printing(variant=variant)
        afc.z_hop, afc.resume_speed, afc.resume_z_speed = 5., 120., 25.
        afc.park_pre_load, afc.park_pre_load_cmd = True, 'MOVE_TO_TRAY'
        afc.kick = afc.wipe = False
        afc.function.select_loaded_lane = lambda: None
        afc.clear_spool_after_eject = False
        afc.function.afc, afc.function.printer = afc, runner.firmware_printer
        afc.function.is_printing = self.classes['function'].is_printing.__get__(afc.function)
        old = afc.lanes['lane1']
        old.__dict__.update(runout_lane='lane2', afc=afc, logger=Mock(), gcode=runner.gcode,
            printer=runner.firmware_printer, raw_load_state=True,
            fila_prep=NS(runout_helper=NS(sensor_enabled=True)))
        runner.firmware_printer.state_message = 'Printer is ready'
        old._perform_infinite_runout = self.classes['lane']._perform_infinite_runout.__get__(old)
        old.unit_obj.lane_not_ready = Mock()
        old.unit_obj.eject_lane = Mock()
        old.unit_obj.return_to_home = Mock()
        runner.stubs.add('AFC_SWAP_MAPPING')
        return runner, afc, old, dispatcher, sd, pending

    def button(self, runner, action):
        runner.gcode.create_gcode_command = lambda name, line, params: integration.Command('', name)
        button = self.classes['debounce'].__new__(self.classes['debounce'])
        button.__dict__.update(logical_state=True, physical_state=False, debounce_delay=.2,
            latest_eventtime=0., button_action=action,
            printer=runner.firmware_printer, gcode=runner.gcode)
        return button

    def test_native_restore_error_debounce_pause_recovers_first_sd_move(self):
        for variant in ('cc1', 'canvas', 'cc2'):
            with self.subTest(variant=variant):
                runner, afc, old, dispatcher, sd, pending = self.runout(variant)
                manual.ManualErrorTests.enable_fan_state(runner)
                button = self.button(runner, self.classes['lane'].handle_prep_runout.__get__(old))
                failed_at = []
                downstream = runner.transform.next_transform.move
                def fail_restore(pos, speed):
                    if afc.current == 'lane2' and not afc.in_toolchange:
                        runner.transform.next_transform.move = downstream
                        failed_at.append(len(runner.native_moves))
                        raise integration.MacroError('one-shot native restore motion error')
                    downstream(pos, speed)
                runner.transform.next_transform.move = fail_restore
                command_errors = []
                send = dispatcher.printer.send_event
                def send_event(name):
                    if name == 'gcode:command_error':
                        command_errors.append(name)
                    return send(name)
                dispatcher.printer.send_event = send_event
                before = list(runner.physical)
                generation = runner.printer['gcode_macro LOAD_SKEW_IF_EXISTS']['generation']
                button._debounce_event(1.)
                runner.sync()
                self.assertEqual(failed_at, [len(runner.native_moves)], 'fallback must not attempt any motion')
                self.assertFalse(command_errors, 'native fault must bypass the host command-error hook')
                self.assertFalse(any(cmd == 'PAUSE' for _, cmd, _, _ in runner.trace))
                self.assertTrue(runner.pause.is_paused)
                self.assertFalse(sd.is_active())
                self.assertTrue(afc.position_saved)
                self.assertFalse(pending)
                self.assertFalse(runner.skew)
                paused = dict(runner.printer['gcode_macro LOAD_SKEW_IF_EXISTS'])
                pause_snapshot = runner.move.saved_states['PAUSE_STATE']
                self.assertEqual(pause_snapshot['last_position'], runner.move.last_position)
                runner.native_moves.clear()
                sd.work_handler(0.)
                self.assertFalse(runner.native_moves)
                runner.defer_print_stats = True
                afc.error.cmd_AFC_RESUME(integration.Command('', 'RESUME'))
                self.assertFalse(runner.pause.is_paused)
                self.assertFalse(afc.position_saved)
                self.assertEqual(len(pending), 1)
                self.assertFalse([m for m in runner.native_moves if any(m[2])])
                for want, actual in zip(before[:3], runner.physical[:3]):
                    self.assertAlmostEqual(want, actual)
                runner.native_moves.clear()
                pending.pop()(0.)
                self.assertAlmostEqual(runner.native_moves[0][0][0], 99.,
                    msg='native fallback recovery lost hypothetical XY=.01 correction')
                self.assertEqual(runner.native_moves[0][2], (.01, 0., 0.))
                self.assertAlmostEqual(runner.printer['fan']['speed'], .6,
                    msg='native fallback must preserve cooling on the first resumed SD move')
                self.assertEqual(paused['phase'], 'paused')
                self.assertGreater(paused['generation'], generation)

    def test_preparation_runout_resumes_raw_sd_without_loading_skew(self):
        for variant in ('cc1', 'canvas', 'cc2'):
            for profile in (True, False):
                with self.subTest(variant=variant, profile=profile):
                    runner, afc, old, dispatcher, sd, pending = self.runout(variant)
                    runner.run('CLEAR_SKEW_IF_EXISTS')
                    if not profile:
                        runner.transform.skew_profiles.clear()
                        runner.printer['configfile']['config'].clear()
                    runner.trace.clear()
                    runner.native_moves.clear()
                    afc.tool_cmds = {'T0': 'lane1', 'T1': 'lane2'}
                    runner.stubs.add('T0')
                    runner.hooks['T0'] = lambda _: afc.cmd_CHANGE_TOOL(integration.Command('', 'T0'))
                    runner.hooks['AFC_SWAP_MAPPING'] = lambda _: afc.tool_cmds.update(T0='lane2', T1='lane1')
                    dispatcher.gcode_handlers['PRINT_START'] = lambda cmd: runner.run(
                        'PRINT_START', cmd.get_raw_command_parameters())
                    part_moves = []
                    def print_move(cmd):
                        runner.command('G1', cmd.get_raw_command_parameters())
                        part_moves.append(runner.native_moves[-1])
                    dispatcher.gcode_handlers['G1'] = print_move
                    sd.gcode.run_script = dispatcher.run_script
                    sd.current_file = io.StringIO(
                        'PRINT_START BED=60 EXTRUDER=230 CHAMBER=0 TOOL=0\nG1 X100 Y100 Z10\n')
                    sd.file_size = len(sd.current_file.getvalue())
                    sd.on_error_gcode = NS(render=lambda: '')
                    # Yield at the real worker's first read, before PRINT_START.
                    # Threads supply resumable stacks; printer bodies never run concurrently.
                    waiting, release = threading.Event(), threading.Event()
                    worker = None
                    def reactor_pause(when):
                        if threading.current_thread() is worker:
                            waiting.set()
                            assert release.wait(10), 'callback did not release SD'
                        elif sd.must_pause_work and worker is not None and worker.is_alive():
                            release.set()
                            worker.join(10)
                            assert not worker.is_alive(), 'SD failed to acknowledge pause'
                    sd.reactor.pause = reactor_pause
                    worker = threading.Thread(target=sd.work_handler, args=(0.,), daemon=True)
                    worker.start()
                    try:
                        self.assertTrue(waiting.wait(10), 'SD did not reach its read/yield')
                        self.assertEqual(runner.printer['print_stats']['state'], 'printing')
                        self.assertEqual(runner.printer['gcode_macro LOAD_SKEW_IF_EXISTS']['phase'], 'off')
                        self.assertEqual(sd.file_position, 0)
                        self.assertFalse(sd.cmd_from_sd)
                        self.classes['lane'].handle_prep_runout(old, 1., False)
                    finally:
                        if worker.is_alive():
                            sd.must_pause_work = True
                        release.set()
                        worker.join(10)
                    self.assertFalse(worker.is_alive())
                    self.assertEqual(afc.current, 'lane2')
                    self.assertFalse(afc.in_toolchange)
                    self.assertFalse(afc.error_state)
                    self.assertEqual(len(pending), 1, 'successful preparation rollover stranded raw-SD pause')
                    self.assertFalse(runner.pause.sd_paused)
                    self.assertFalse(runner.pause.is_paused)
                    self.assertFalse(afc.position_saved)
                    self.assertFalse(runner.skew)
                    self.assertEqual(runner.printer['gcode_macro LOAD_SKEW_IF_EXISTS']['phase'], 'off')
                    self.assertFalse([m for m in runner.native_moves if any(m[2])])
                    self.assertFalse(any(cmd in ('SKEW_PROFILE', 'PAUSE_BASE') for _, cmd, _, _ in runner.trace))
                    old.unit_obj.lane_not_ready.assert_called_once_with(old)
                    self.assertFalse(part_moves)
                    # The old stack has exited; resume the newly scheduled worker.
                    self.assertFalse(sd.must_pause_work)
                    pending.pop()(0.)
                    self.assertIsNone(sd.current_file)
                    self.assertEqual(sd.file_position, sd.file_size)
                    self.assertEqual(runner.printer['print_stats']['state'], 'complete')
                    self.assertEqual(len(part_moves), 1)
                    self.assertAlmostEqual(part_moves[0][0][0], 99. if profile else 100.)
                    self.assertEqual(part_moves[0][2], (.01 if profile else 0., 0., 0.))
                    self.assertEqual(sum(cmd == 'SKEW_PROFILE' for _, cmd, _, _ in runner.trace), int(profile))

    def test_preparation_runout_keeps_lifecycle_invalidation_guards(self):
        for profile in (True, False):
            for change in ('cleanup', 'pause', 'afc_error', 'print_error'):
                with self.subTest(profile=profile, change=change):
                    runner, afc, old, dispatcher, sd, pending = self.runout()
                    runner.run('CLEAR_SKEW_IF_EXISTS')
                    if not profile:
                        runner.transform.skew_profiles.clear()
                        runner.printer['configfile']['config'].clear()
                    def interrupt(_):
                        if change == 'cleanup':
                            runner.run('CLEAR_SKEW_IF_EXISTS')
                        elif change == 'pause':
                            runner.run('PAUSE')
                        elif change == 'afc_error':
                            afc.error.AFC_error('error during preparation rollover')
                        else:
                            sd.print_stats.note_error('print error during preparation rollover')
                    runner.hooks['AFC_SWAP_MAPPING'] = interrupt
                    runner.trace.clear()
                    self.classes['lane'].handle_prep_runout(old, 1., False)
                    self.assertEqual(afc.current, 'lane2')
                    self.assertFalse(afc.in_toolchange)
                    self.assertFalse(pending, 'invalidated preparation must not release SD work')
                    self.assertFalse(sd.is_active())
                    self.assertFalse(runner.skew)
                    self.assertFalse([m for m in runner.native_moves if any(m[2])])
                    self.assertFalse(any(cmd == 'SKEW_PROFILE' for _, cmd, _, _ in runner.trace))
                    old.unit_obj.lane_not_ready.assert_not_called()

    def test_debounce_fallback_preserves_existing_pause_and_absent_cosmos(self):
        for mode in ('already_paused', 'no_cosmos'):
            with self.subTest(mode=mode):
                runner, afc, dispatcher, sd, pending = manual.ManualErrorTests().printing()
                manual.ManualErrorTests.enable_fan_state(runner)
                if mode == 'already_paused':
                    afc.save_pos()
                    runner.script('_SKEW_PAUSE\n_SAVE_FAN_STATE')
                    runner.pause.cmd_PAUSE(integration.Command('', 'PAUSE'))
                    runner.command('M107')
                else:
                    runner.run('CLEAR_SKEW_IF_EXISTS')
                    runner.objects.pop('gcode_macro LOAD_SKEW_IF_EXISTS')
                state = dict(runner.printer['gcode_macro LOAD_SKEW_IF_EXISTS'])
                fans = dict(runner.printer['gcode_macro _global_var']['fan_states'])
                saved = dict(runner.move.saved_states)
                afc_snapshot = list(afc.last_gcode_position)
                runner.native_moves.clear()
                runner.trace.clear()
                def fail(*args, **kwargs):
                    raise integration.MacroError('native callback error')
                self.button(runner, fail)._debounce_event(1.)
                self.assertTrue(runner.pause.is_paused)
                self.assertFalse(sd.is_active())
                self.assertFalse(pending)
                self.assertFalse(runner.native_moves)
                self.assertFalse(any(cmd in ('_SKEW_PAUSE', '_SAVE_FAN_STATE', 'PAUSE')
                                     for _, cmd, _, _ in runner.trace))
                self.assertEqual(runner.printer['gcode_macro LOAD_SKEW_IF_EXISTS'], state)
                self.assertEqual(runner.printer['gcode_macro _global_var']['fan_states'], fans)
                if mode == 'already_paused':
                    self.assertEqual(runner.move.saved_states, saved)
                    self.assertEqual(afc.last_gcode_position, afc_snapshot)


if __name__ == '__main__':
    unittest.main()
