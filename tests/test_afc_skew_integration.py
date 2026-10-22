"""Software-only integration of pinned AFC, Kalico and actual COSMOS macros.

Run with COSMOS_AFC_SOURCE=<clean checkout at recipe SRCREV> and
COSMOS_KALICO_SOURCE=<Kalico root containing klippy/extras at recipe SRCREV>:
    python -m unittest discover -s tests -p 'test_afc_skew_integration.py' -v
The recipe's patches are checked/applied to a separate temporary source tree.
No network or hardware access. Without source directories these tests skip.
AFC_TEST_UNPATCHED=1 is a diagnostic negative control, not a release test.
Hardware/sensor/telemetry doubles do not simulate queues, heaters or real mesh.
"""
import ast
import hashlib
import os
from pathlib import Path
import re
import shlex
import subprocess
import tempfile
import traceback
from types import SimpleNamespace as NS
import unittest
from unittest.mock import Mock

from test_skew_macros import MacroRunner, MacroError

ROOT = Path(__file__).resolve().parents[1]
RECIPE = ROOT / 'meta-opencentauri/recipes-data/klipper-afc-addon/afc_0.1.bb'
PIN = '0fd50f2229b685cdb9b84793150ab0798e16eb03'
KALICO_PIN = '26e46bd536cd5b55c4028dd2409a2210262fc9ed'
KALICO_HASHES = {
    'skew_correction.py': 'd3ca565a3831dbdd31cd176e5808572fe838218f2994982289f97e031a243259',
    'gcode_move.py': 'ef3970c806bfb1318d5d0582278b02681a8f4b8d55bf8fb8a83519899cc4aed7',
    'pause_resume.py': 'f635141e42c94fde4d68e545888db9504c50509cc27f5d63836d37f1b312b6da',
    'virtual_sdcard.py': '5042b206990b99327ed2aa71bbcdb8aefd8ec7d4158ebed90077099e33674625',
}


def extract_class(path, name, methods, namespace=None):
    """Compile actual function bodies, avoiding hardware-only module imports."""
    tree = ast.parse(path.read_text(), filename=str(path))
    node = next(n for n in tree.body if isinstance(n, ast.ClassDef) and n.name == name)
    node.bases = []
    node.body = [n for n in node.body if isinstance(n, ast.FunctionDef)
                 and (n.name in methods or n.name.startswith('_cosmos_'))]
    for method in node.body:
        assert isinstance(method, ast.FunctionDef)
        method.decorator_list = []
    module = ast.Module(body=[ast.ImportFrom(module='__future__',
        names=[ast.alias(name='annotations')], level=0), node], type_ignores=[])
    ns = dict(namespace or {})
    exec(compile(ast.fix_missing_locations(module), str(path), 'exec'), ns)
    return ns[name]


class Command:
    def __init__(self, args='', command='CHANGE_TOOL'):
        self.args = args
        self.name = command
        self.params = {}
        for token in shlex.split(args):
            if '=' in token:
                k, v = token.split('=', 1)
            else:
                k, v = token[:1], token[1:]
            self.params[k.upper()] = v
    def get(self, key, default=None):
        return self.params.get(key, default)
    def get_int(self, key, default=0, **kw):
        return int(self.get(key, default))
    def get_float(self, key, default=0., **kw):
        value = self.get(key, default)
        return None if value is None else float(value)
    def get_command_parameters(self):
        return self.params
    def get_commandline(self):
        return self.name + ' ' + self.args
    def get_raw_command_parameters(self):
        return self.args
    def respond_info(self, *args):
        pass
    error = MacroError


class IntegrationRunner(MacroRunner):
    """Real coordinate transforms and move/state commands under macro execution."""
    def __init__(self, classes, variant='cc1', factors=(.01, 0., 0.), mesh=.0):
        super().__init__(variant)
        self.stubs.add('SET_IDLE_TIMEOUT')
        # Handled by actual Kalico below, not by the macro-only runner.
        self.inert_commands = set(self.inert_commands) | {'RESTORE_GCODE_STATE'}
        self.native_moves = []
        self.defer_print_stats = False
        self.physical = [99., 100., 10. + mesh, 20.]
        self.objects = {}
        self.firmware_printer = NS(lookup_object=lambda name, default=None:
                                   self.objects.get(name, default))
        self.gcode = NS(run_script_from_command=self.script, respond_info=lambda *a: None)
        self.move = classes['move'].__new__(classes['move'])
        self.move.__dict__.update(printer=self.firmware_printer, is_printer_ready=True,
            base_position=[0., 0., 0., 3.], homing_position=[0., 0., 0., 0.],
            last_position=[99., 100., 10., 20.], speed=30., speed_factor=1/60,
            extrude_factor=1., absolute_coord=True, absolute_extrude=True,
            axis_map={'X': 0, 'Y': 1, 'Z': 2, 'E': 3}, saved_states={})
        self.transform = classes['skew'].__new__(classes['skew'])
        self.transform.__dict__.update(printer=self.firmware_printer, xy_factor=0.,
            xz_factor=0., yz_factor=0., current_profile_name='',
            skew_profiles={'my_skew_profile': dict(zip(('xy_skew','xz_skew','yz_skew'), factors))})
        # A reversible downstream offset stands in for mesh. Saving raw toolhead
        # coordinates would apply this offset twice, unlike pre-mesh calc_skew.
        def downstream_move(pos, speed):
            self.native_moves.append((list(pos), self.skew, tuple(self.coefficients())))
            self.physical[:] = pos
            self.physical[2] += mesh
        def downstream_position():
            pos = list(self.physical)
            pos[2] -= mesh
            return pos
        self.transform.next_transform = NS(move=downstream_move, get_position=downstream_position)
        self.move.move_with_transform = self.transform.move
        self.move.position_with_transform = self.transform.get_position
        self.pause = classes['pause'].__new__(classes['pause'])
        self.pause.__dict__.update(gcode=self.gcode, is_paused=False, sd_paused=False,
            pause_command_sent=False, v_sd=None, recover_velocity=50.)
        self.objects.update(gcode_move=self.move, skew_correction=self.transform,
            print_stats=NS(get_status=lambda t: dict(self.printer['print_stats'])),
            pause_resume=self.pause, **{'gcode_macro LOAD_SKEW_IF_EXISTS':
            NS(get_status=lambda t: dict(self.printer['gcode_macro LOAD_SKEW_IF_EXISTS']))})
        self.move.reset_last_position()
        self.sync()

    def coefficients(self):
        return [getattr(self.transform, plane + '_factor') for plane in ('xy','xz','yz')]

    def sync(self):
        self.printer['gcode_move']['position'] = dict(zip('xyze', self.move.last_position))
        self.printer['pause_resume']['is_paused'] = self.pause.is_paused

    def script(self, script):
        for line in script.splitlines():
            cmd, _, args = line.partition(' ')
            self.command(cmd, args, 'AFC')

    def command(self, cmd, args='', owner='test'):
        super().command(cmd, args, owner)
        if cmd in self.macros or cmd in self.stubs:
            return
        gcmd = Command(args)
        if cmd == 'SET_SKEW':
            self.transform.cmd_SET_SKEW(gcmd)
        elif cmd == 'SKEW_PROFILE':
            self.transform.cmd_SKEW_PROFILE(gcmd)
        elif cmd in ('G0', 'G1'):
            self.move.cmd_G1(gcmd)
        elif cmd in ('G90', 'G91', 'G92', 'M82', 'M83', 'SAVE_GCODE_STATE', 'RESTORE_GCODE_STATE'):
            getattr(self.move, 'cmd_' + cmd)(gcmd)
        elif cmd in ('PAUSE_BASE', 'RESUME_BASE'):
            if cmd == 'RESUME_BASE' and self.defer_print_stats:
                self.printer['print_stats']['state'] = 'paused'
            getattr(self.pause, 'cmd_' + cmd.split('_')[0])(gcmd)
        self.sync()


def make_afc(classes, runner):
    afc = classes['afc'].__new__(classes['afc'])
    runner.printer['AFC'] = {}
    noop = lambda *a, **kw: None
    function = NS(is_homed=lambda **kw: True, check_homed=lambda: True,
        is_paused=lambda: runner.pause.is_paused, in_print=lambda: True,
        check_absolute_mode=noop, log_toolhead_pos=noop,
        get_current_extruder=lambda: 'extruder', _handle_activate_extruder=noop)
    afc.__dict__.update(printer=runner.firmware_printer, reactor=NS(monotonic=lambda: 0.),
        function=function, logger=Mock(), gcode_move=runner.move, gcode=runner.gcode,
        toolhead=NS(get_position=lambda: list(runner.physical), wait_moves=noop,
                    get_status=lambda t: {'axis_maximum': [256., 280., 256.]}),
        resume_speed=50., resume_z_speed=10., z_hop=2., speed=30.,
        position_saved=False, error_state=False, in_toolchange=False, next_lane_load=None,
        current='lane1', current_loading=None, number_of_toolchanges=0, current_toolchange=0,
        _check_bypass=lambda **kw: False, verify_macro_positions=lambda: '',
        capture_toolhead_temp=lambda: None, restore_toolhead_temp=noop,
        save_vars=noop, _set_display_status=noop, _check_extruder_temp=lambda lane: True,
        afcDeltaTime=Mock(), afc_stats=Mock(), spool=Mock(), post_load_macro=None,
        post_unload_macro=None, park_pre_load=False, park_pre_load_cmd=None,
        poop=True, poop_cmd='PURGE', kick=True, kick_cmd='KICK', wipe=True,
        wipe_cmd='CLEAN_NOZZLE', tool_cut=True, tool_cut_cmd='CUT_FILAMENT',
        park=True, park_cmd='MOVE_TO_TRAY', form_tip=False,
        last_gcode_position=[99.,100.,10.,20.])
    extruder = NS(name='extruder', th_extruder_name='extruder', lane_loaded='lane1',
                  lanes=['lane1','lane2'], estats=Mock(), is_standalone=lambda: False)
    unit = NS(lane_loading=noop, lane_tool_loaded=noop, lane_unloading=noop)
    lanes = {}
    for name in ('lane1','lane2'):
        lane = NS(name=name, extruder_obj=extruder, unit_obj=unit, hub_obj=NS(state=False),
            hub='hub', load_state=True, need_purge=False, status='loaded', _afc_prep_done=True,
            is_direct_hub=lambda: False, espooler=Mock(), spool_id=1,
            enable_fault_detection=noop, disable_buffer=noop, do_enable=noop,
            set_tool_loaded=noop, enable_buffer=noop,
            get_td1_data_load=noop, lane_load_count=Mock(),
            custom_load_cmd=None, custom_unload_cmd=None)
        lanes[name] = lane
    afc.lanes = lanes
    # Sensor/feeder hardware seam only; real TOOL_* and sequence functions,
    # native moves and real cut/park/purge/wipe callback dispatch remain in use.
    def unit_load(lane, extruder):
        afc.move_e_pos(5., 5.)
        afc.current = lane.name
        return True
    def unit_unload(lane, extruder):
        afc.move_e_pos(-2., 5.)
        afc.do_tool_cut_tip_form(lane, extruder)
        afc.move_z_pos(afc.gcode_move.last_position[2] + 1, 'after callbacks')
        afc.current = None
        extruder.lane_loaded = None
        return True
    unit.unit_load_lane = unit_load
    unit.unit_unload_lane = unit_unload
    error = classes['error'].__new__(classes['error'])
    error.__dict__.update(afc=afc, logger=Mock(), pause_resume=runner.pause, pause=False,
        AFC_RENAME_RESUME_NAME='RESUME', AFC_RENAME_PAUSE_NAME='PAUSE',
        error_timeout=600, idle_timeout_val=600)
    error.set_error_state = lambda value: setattr(afc, 'error_state', value)
    def fail(*a, **kw):
        afc.error_state = True
    error.AFC_error = fail
    error.fix = fail
    error.handle_lane_failure = fail
    afc.error = error
    return afc


class AFCIntegrationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        source = os.environ.get('COSMOS_AFC_SOURCE')
        kalico = os.environ.get('COSMOS_KALICO_SOURCE')
        if not source or not kalico:
            raise unittest.SkipTest('set COSMOS_AFC_SOURCE and COSMOS_KALICO_SOURCE for pinned integration')
        source, kalico = Path(source), Path(kalico) / 'klippy/extras'
        kalico_recipe = ROOT / 'meta-opencentauri/recipes-apps/klipper/kalico_2026.02.00.inc'
        if f'SRCREV = "{KALICO_PIN}"' not in kalico_recipe.read_text():
            raise AssertionError('Kalico pin changed: refresh pinned helper hashes and coverage')
        for name, digest in KALICO_HASHES.items():
            if hashlib.sha256((kalico / name).read_bytes()).hexdigest() != digest:
                raise AssertionError(f'{name} differs from the pinned Kalico helper')
        pin_match = re.search(r'SRCREV = "([0-9a-f]+)"', RECIPE.read_text())
        assert pin_match is not None
        pin = pin_match.group(1)
        if pin != PIN:
            raise AssertionError('AFC pin changed: review and refresh integration coverage')
        head = subprocess.check_output(['git','-C',str(source),'rev-parse','HEAD'], text=True).strip()
        if head != pin:
            raise AssertionError('COSMOS_AFC_SOURCE must be the exact recipe pin')
        cls.temp = tempfile.TemporaryDirectory(prefix='cosmos-afc-integration-')
        cls.addClassCleanup(cls.temp.cleanup)
        dest = Path(cls.temp.name)
        # Export committed bytes, never a modified developer worktree.
        for file in ('AFC.py','AFC_error.py','AFC_functions.py'):
            path = dest / 'extras' / file
            path.parent.mkdir(exist_ok=True)
            path.write_bytes(subprocess.check_output(['git','-C',str(source),'show',f'{pin}:extras/{file}']))
        patches = re.findall(r'file://([^\s"\\]+\.patch)', RECIPE.read_text())
        if not os.environ.get('AFC_TEST_UNPATCHED'):
            for patch in patches:
                path = RECIPE.parent / 'files' / patch
                subprocess.run(['git','apply','--check',str(path)], cwd=dest, check=True, capture_output=True)
                subprocess.run(['git','apply',str(path)], cwd=dest, check=True, capture_output=True)
        env = dict(traceback=traceback, re=re,
            State=NS(IDLE='idle',LOADING='loading',UNLOADING='unloading',RESTORING_POS='restoring'),
            AFCLaneState=NS(TOOL_LOADING='loading',TOOL_UNLOADING='unloading',INFINITE_RUNOUT='infinite'))
        helpers = ast.parse((dest/'extras/AFC_functions.py').read_text())
        helper_nodes: list[ast.stmt] = [ast.ImportFrom(module='__future__',
            names=[ast.alias(name='annotations')], level=0)]
        helper_nodes.extend(n for n in helpers.body if isinstance(n, ast.FunctionDef)
            and n.name in ('round_floats', 'get_gcode_absolute_extrude', 'set_gcode_absolute_extrude'))
        helpers.body = helper_nodes
        exec(compile(ast.fix_missing_locations(helpers), 'AFC_functions.py', 'exec'), env)
        afc_methods = ['TOOL_LOAD','_TOOL_LOAD','TOOL_UNLOAD','_TOOL_UNLOAD','CHANGE_TOOL','_CHANGE_TOOL',
            'cmd_TOOL_LOAD','cmd_TOOL_UNLOAD','cmd_CHANGE_TOOL','save_pos','restore_pos','_restore_pos',
            'move_z_pos','move_e_pos','_get_resume_speed','_get_resume_speedz',
            'do_poop_kick_wipe','do_tool_cut_tip_form','load_sequence','unload_sequence']
        cls.classes = {
            'afc': extract_class(dest/'extras/AFC.py','afc',afc_methods,env),
            'error': extract_class(dest/'extras/AFC_error.py','afcError', ['cmd_AFC_PAUSE','cmd_AFC_RESUME']),
            'move': extract_class(kalico/'gcode_move.py','GCodeMove', ['reset_last_position','cmd_G1',
                'cmd_G90','cmd_G91','cmd_G92','cmd_M82','cmd_M83','cmd_SAVE_GCODE_STATE','cmd_RESTORE_GCODE_STATE']),
            'skew': extract_class(kalico/'skew_correction.py','PrinterSkew', ['calc_skew','calc_unskew',
                'get_position','move','_update_skew','cmd_SET_SKEW','cmd_SKEW_PROFILE']),
            'sd': extract_class(kalico/'virtual_sdcard.py','VirtualSD', ['do_resume']),
            'pause': extract_class(kalico/'pause_resume.py','PauseResume', ['cmd_PAUSE','cmd_RESUME',
                'send_pause_command','send_resume_command','is_sd_active'])}

    def setup_print(self, variant='cc1', **kw):
        runner = IntegrationRunner(self.classes, variant, **kw)
        runner.run('LOAD_SKEW_IF_EXISTS')
        afc = make_afc(self.classes, runner)
        runner.trace.clear()
        runner.native_moves.clear()
        return runner, afc

    def test_resume_reloads_before_virtual_sd_work_updates_print_stats(self):
        runner, afc = self.setup_print()
        afc.error.cmd_AFC_PAUSE(Command())
        sd = self.classes['sd'].__new__(self.classes['sd'])
        pending = []
        sd.work_timer = None
        sd.work_handler = lambda t: runner.printer['print_stats'].update(state='printing')
        sd.reactor = NS(NOW=0., register_timer=lambda callback, when: pending.append(callback) or 'pending')
        runner.pause.sd_paused = True
        runner.pause.v_sd = sd
        runner.defer_print_stats = True
        afc.error.cmd_AFC_RESUME(Command())
        self.assertEqual(len(pending), 1)
        self.assertEqual(runner.printer['print_stats']['state'], 'paused')
        self.assertFalse(runner.pause.is_paused)
        self.assertTrue(runner.skew, 'reload must not wait for asynchronous SD note_start')
        self.assertFalse([m for m in runner.native_moves if any(m[2])])
        pending.pop()(0.)
        runner.command('G1', 'X100 Y100 Z10')
        self.assertTrue(runner.native_moves[-1][1])
        self.assertAlmostEqual(runner.physical[0], 99.)

    def test_resume_specific_tail_still_rejects_lifecycle_invalidation(self):
        for change in ('cancel', 'pause', 'error'):
            with self.subTest(change=change):
                runner, afc = self.setup_print()
                afc.save_pos()
                runner.printer['print_stats']['state'] = 'paused'
                original = runner.transform.next_transform.move
                def interrupt(pos, speed):
                    runner.transform.next_transform.move = original
                    original(pos, speed)
                    if change == 'cancel':
                        runner.run('CLEAR_SKEW_IF_EXISTS')
                    elif change == 'pause':
                        runner.pause.is_paused = True
                    else:
                        afc.error_state = True
                runner.transform.next_transform.move = interrupt
                afc.restore_pos(resuming=True)
                self.assertFalse(runner.skew)
                self.assertFalse(any(cmd == 'SKEW_PROFILE' for _,cmd,_,_ in runner.trace))

    def test_registered_command_entry_points_reach_guarded_operations(self):
        for operation in ('TOOL_LOAD', 'TOOL_UNLOAD', 'CHANGE_TOOL'):
            with self.subTest(operation=operation):
                runner, afc = self.setup_print()
                afc.tool_cmds = {'T0': 'lane1', 'T1': 'lane2'}
                target = 'lane1' if operation == 'TOOL_UNLOAD' else 'lane2'
                getattr(afc, 'cmd_' + operation)(Command('LANE=' + target, operation))
                self.assertFalse(afc.error_state)
                self.assertTrue(runner.native_moves)
                self.assertFalse([m for m in runner.native_moves if any(m[2])])
                self.assertTrue(runner.skew)
                self.assertEqual(sum(cmd == 'SKEW_PROFILE' for _,cmd,_,_ in runner.trace), 1)

    def test_failed_operations_never_reload_or_strand_an_owner(self):
        for operation in ('TOOL_LOAD', 'TOOL_UNLOAD', 'CHANGE_TOOL'):
            for failure in ('false', 'exception'):
                with self.subTest(operation=operation, failure=failure):
                    runner, afc = self.setup_print()
                    lane = afc.lanes['lane1' if operation == 'TOOL_UNLOAD' else 'lane2']
                    unit = lane.unit_obj
                    name = 'unit_load_lane' if operation == 'TOOL_LOAD' else 'unit_unload_lane'
                    original = getattr(unit, name)
                    if failure == 'false':
                        setattr(unit, name, lambda *args: False)
                    else:
                        def fail(*args):
                            raise MacroError('injected feeder error')
                        setattr(unit, name, fail)
                    if failure == 'exception' and operation != 'CHANGE_TOOL':
                        with self.assertRaises(MacroError):
                            getattr(afc, operation)(lane)
                    else:
                        getattr(afc, operation)(lane)
                    self.assertFalse(runner.skew)
                    self.assertFalse(any(cmd == 'SKEW_PROFILE' for _,cmd,_,_ in runner.trace))
                    setattr(unit, name, original)
                    # A subsequent explicit pause/resume can recover; there is
                    # no global depth/pending flag left by the failed scope.
                    afc.error_state = False
                    afc.error.cmd_AFC_PAUSE(Command())
                    afc.error.cmd_AFC_RESUME(Command())
                    self.assertTrue(runner.skew)

    def test_pause_and_cancel_invalidate_successful_operation_tail(self):
        for lifecycle in ('PAUSE', 'CANCEL_PRINT'):
            runner, afc = self.setup_print()
            unit = afc.lanes['lane1'].unit_obj
            original = unit.unit_unload_lane
            def interrupt(*args):
                result = original(*args)
                runner.run(lifecycle)
                return result
            unit.unit_unload_lane = interrupt
            self.assertTrue(afc.TOOL_UNLOAD(afc.lanes['lane1']))
            self.assertFalse(runner.skew)
            self.assertFalse([m for m in runner.native_moves if any(m[2])])

    def test_idle_preparation_paused_and_missing_profile_never_reload(self):
        for mode in ('idle', 'preparation', 'paused', 'no_profile'):
            with self.subTest(mode=mode):
                runner, afc = self.setup_print()
                runner.run('CLEAR_SKEW_IF_EXISTS')
                if mode == 'idle':
                    runner.printer['print_stats']['state'] = 'standby'
                elif mode == 'paused':
                    runner.run('LOAD_SKEW_IF_EXISTS')
                    afc.error.cmd_AFC_PAUSE(Command())
                elif mode == 'no_profile':
                    runner.printer['configfile']['config'].clear()
                    runner.objects.pop('skew_correction')
                    runner.printer.pop('skew_correction')
                    runner.run('LOAD_SKEW_IF_EXISTS')
                runner.trace.clear()
                runner.native_moves.clear()
                self.assertTrue(afc.TOOL_UNLOAD(afc.lanes['lane1']))
                self.assertFalse(runner.skew)
                self.assertFalse([m for m in runner.native_moves if any(m[2])])
                self.assertFalse(any(cmd == 'SKEW_PROFILE' for _,cmd,_,_ in runner.trace))

    def test_saved_position_is_stable_across_repeated_saves_and_scopes(self):
        runner, afc = self.setup_print(factors=(.01, .02, -.015), mesh=.75)
        expected = list(runner.physical)
        afc.save_pos()
        snapshot = list(afc.last_gcode_position)
        runner.run('_SKEW_SUSPEND')
        afc.move_z_pos(30.)
        afc.save_pos()
        self.assertEqual(afc.last_gcode_position, snapshot)
        runner.run('LOAD_SKEW_IF_EXISTS')
        runner.native_moves.clear()
        afc.restore_pos()
        self.assertFalse([m for m in runner.native_moves if any(m[2])])
        for want, actual in zip(expected[:3], runner.physical[:3]):
            self.assertAlmostEqual(want, actual)
        self.assertTrue(runner.skew)

    def test_failed_native_restore_keeps_skew_off_and_snapshot_available(self):
        runner, afc = self.setup_print()
        afc.save_pos()
        def reject_move(pos, speed):
            raise MacroError('injected motion limit error')
        runner.transform.next_transform.move = reject_move
        with self.assertRaises(MacroError):
            afc.restore_pos()
        self.assertFalse(runner.skew)
        self.assertTrue(afc.position_saved)
        self.assertFalse(any(cmd == 'SKEW_PROFILE' for _,cmd,_,_ in runner.trace))

    def test_refused_cosmos_resume_keeps_afc_snapshot_and_error(self):
        runner, afc = self.setup_print()
        afc.error.cmd_AFC_PAUSE(Command())
        afc.error_state = True
        sensor = runner.printer['gcode_macro _global_var']['runout_sensor_name']
        runner.printer[sensor] = {'enabled': True, 'filament_detected': False}
        afc.error.cmd_AFC_RESUME(Command())
        self.assertTrue(runner.pause.is_paused)
        self.assertTrue(afc.position_saved, 'refused RESUME must not consume the AFC snapshot')
        self.assertTrue(afc.error_state)
        self.assertFalse(runner.skew)

    def test_afc_pause_resume_recovers_both_saved_skew_frames(self):
        for manual_error in ('render', 'command', None):
            for factors in ((.01, 0., 0.), (.01, .02, -.015)):
                with self.subTest(manual_error=manual_error, factors=factors):
                    runner, afc = self.setup_print(factors=factors, mesh=.75)
                    if manual_error:
                        if manual_error == 'render':
                            runner.printer['extruder']['can_extrude'] = False
                        else:
                            runner.fail_command = 'G1'
                        with self.assertRaises(MacroError):
                            runner.run('KICK')
                        runner.printer['extruder']['can_extrude'] = True
                        runner.fail_command = None
                        self.assertFalse(runner.skew)
                    expected = list(runner.physical)
                    saved_e = (runner.move.last_position[3] - runner.move.base_position[3])
                    afc.error.cmd_AFC_PAUSE(Command())
                    self.assertTrue(runner.pause.is_paused)
                    afc.error.cmd_AFC_RESUME(Command())
                    for want, actual in zip(expected[:3], runner.physical[:3]):
                        self.assertAlmostEqual(want, actual,
                            msg='AFC restore interpreted saved coordinates in the wrong skew frame')
                    self.assertFalse([m for m in runner.native_moves if any(m[2])])
                    self.assertTrue(runner.skew)
                    self.assertFalse(runner.pause.is_paused)
                    self.assertFalse(afc.position_saved)
                    self.assertAlmostEqual(saved_e, runner.move.last_position[3] - runner.move.base_position[3])

    def test_load_and_change_keep_one_outer_restore_boundary(self):
        for operation in ('TOOL_LOAD', 'CHANGE_TOOL'):
            for variant in ('cc1', 'canvas', 'cc2'):
                with self.subTest(operation=operation, variant=variant):
                    runner, afc = self.setup_print(variant)
                    before = list(runner.physical)
                    if operation == 'TOOL_LOAD':
                        afc.current = None
                        afc.lanes['lane2'].extruder_obj.lane_loaded = None
                    getattr(afc, operation)(afc.lanes['lane2'])
                    self.assertFalse(afc.error_state)
                    self.assertFalse(afc.in_toolchange)
                    self.assertGreater(len(runner.native_moves), 5)
                    self.assertFalse([m for m in runner.native_moves if any(m[2])])
                    self.assertTrue(runner.skew)
                    self.assertEqual(sum(cmd == 'SKEW_PROFILE' for _,cmd,_,_ in runner.trace), 1)
                    if operation == 'CHANGE_TOOL':
                        for expected, actual in zip(before[:3], runner.physical[:3]):
                            self.assertAlmostEqual(expected, actual)

    def test_native_unload_and_nested_callbacks_are_skew_free(self):
        runner, afc = self.setup_print()
        self.assertTrue(afc.TOOL_UNLOAD(afc.lanes['lane1']))
        self.assertGreater(len(runner.native_moves), 5)
        self.assertFalse([m for m in runner.native_moves if any(m[2])],
                         'native AFC hop or nested callback moved with skew enabled')
        self.assertTrue(runner.skew)
        self.assertEqual(sum(cmd == 'SKEW_PROFILE' for _,cmd,_,_ in runner.trace), 1)


if __name__ == '__main__':
    unittest.main()
