# Skew regression tests

Run from the repository root with Python 3.10+ and Jinja2 installed:

```sh
python -m unittest discover -s tests -v
```

The default suite is offline. Pinned-source integration tests explicitly skip
unless the source directories below are supplied; a skipped run is not full
integration validation. No test contacts a printer or sends hardware commands.

## Pinned-source integration

Provide local source roots from the recipe revisions:

- AFC: `0fd50f2229b685cdb9b84793150ab0798e16eb03`, as a Git checkout containing that
  commit. Tests export the pinned files, not the checkout's working-tree contents.
- Kalico: `26e46bd536cd5b55c4028dd2409a2210262fc9ed`, containing `klippy/gcode.py` and
  `klippy/extras/{skew_correction,gcode_move,pause_resume,virtual_sdcard}.py`.
  Tests verify hashes of the files they execute and check recipe pins.

```sh
COSMOS_AFC_SOURCE=/path/to/AFC-Klipper-Add-On \
COSMOS_KALICO_SOURCE=/path/to/kalico \
python -m unittest discover -s tests -v
```

Git is needed to export AFC and check/apply recipe patches in an isolated temporary
directory. No source downloads occur. Update the integration fixtures deliberately
when rebasing either dependency. `AFC_TEST_UNPATCHED=1` is a diagnostic negative
control for the AFC regression; `KALICO_TEST_UNPATCHED=1` disables the host-side
pause patch for the manual-error negative control. Neither is a release-validation
setting.

### Scope

- `test_skew_macros.py` loads actual CC1, CC2 and CC1 CANVAS templates. Its runner
  models whole-template Jinja evaluation, fresh nested macro snapshots, recursion
  rejection, `RETURN`, variables, strict extended-command arguments, and the order
  of skew changes and motion. Unknown commands fail unless explicitly modeled or
  declared inert/stubbed.
- `test_klipper_parser.py` checks rendered homing, calibration and cancel commands
  against the pinned Kalico parser, including cancellation reaching heater shutdown.
- `test_afc_skew_integration.py` applies the recipe patch and executes actual pinned
  AFC operation/save/restore/pause/resume methods, Kalico coordinate transforms and
  state/pause methods, virtual-SD resume scheduling, and COSMOS templates. Hardware,
  sensor and telemetry doubles replace unrelated dependencies. A reversible
  downstream offset stands in for bed mesh to detect double application.
- `test_manual_skew_errors.py` applies the host Kalico recipe patch and registers
  its real command-error handler. It runs the pinned G-code dispatcher and SD
  worker to verify that rejected requests preserve correction, while errors after
  suspension stop the job before another print move. Recovery is checked both
  with and without AFC, including the first resumed print move.

These are not full firmware simulations. Real mesh interpolation, kinematic limits,
heaters, sensors and the motion queue remain outside scope. A full BitBake build
and controlled real-printer validation are still necessary before release.

## Skew contract

Only the configured `my_skew_profile` is used. COSMOS owns skew through
`LOAD_SKEW_IF_EXISTS`, `CLEAR_SKEW_IF_EXISTS`, and the internal suspension helpers;
arbitrary direct `SET_SKEW`/`SKEW_PROFILE` commands bypass the lifecycle bookkeeping.
A profile name is not an enabled flag: Kalico retains it after `SET_SKEW CLEAR=1`.

- Start clears stale correction before preparation and loads only at its end.
- COSMOS pause clears before saving its return position. Resume rejects unpaused
  calls, restores that position with correction still off, then reloads for a
  paused printing phase. A pause during preparation does not enable correction.
- Each maintenance wrapper snapshots whether correction was enabled, suspends it,
  calls a separate private body, and conditionally restores afterward. Nested
  wrappers see correction off, so only the outermost owner restores it.
- Lifecycle cleanup increments a generation number, invalidating earlier owners.
  A pause/cancel/end inside maintenance cannot be undone by an old restore tail.
- KICK validates homing and temperature before suspension. A rejected request
  does not change correction or pause an otherwise valid print.
- Errors after suspension abort before restoration and leave correction off. The
  host Kalico command-error handler stops an active printing phase automatically,
  saves uncorrected recovery frames, and marks it paused without parking or other
  recovery motion. Resolve the error before resuming. This applies to manual
  console errors too, not just commands originating in the SD file.
- Private bodies preserve public parameters and variables and ensure an early
  `RETURN` cannot bypass the owner's restoration. Separate public wrappers are
  intentional: Kalico forbids recursive calls to the same generic wrapper macro.
- The AFC recipe patch suspends at entire load/unload/tool-change boundaries,
  including native moves outside COSMOS callbacks. Success-only restoration uses
  stack-local ownership, so errors cannot strand a global nesting counter.
- Automatic spool rollover owns suspension across the raw SD pause/change/restore/
  resume sequence. Its successful final boundary reloads before queued print work;
  lifecycle interruptions must not resume the job or restore correction. If rollover
  is nested inside another maintenance owner with configured skew, it retains the
  AFC return position and stays paused without further recovery motion. Finish the
  outer maintenance, then explicitly RESUME; it must not release printing early.
- A bypass load rejection is a no-work outcome, not failed maintenance: it leaves
  existing print correction untouched when bypass policy does not request a pause.
- AFC saved positions are normalized to the coordinate frame **below skew and
  above bed mesh** using the actual transform at save time. This handles snapshots
  made both with skew enabled and after a maintenance error cleared it. Restoring
  raw toolhead coordinates would incorrectly apply bed mesh twice.
- AFC restores its normalized snapshot with correction off, after COSMOS's own
  resume positioning. Only then does it reload printing correction. A guarded
  resume-only tail handles the interval where virtual-SD has scheduled work but
  `print_stats` still says `paused`; ordinary maintenance retains its stricter
  printing-state check. A refused resume preserves the AFC snapshot and error.

Coverage includes direct and nested purge/kick/cut/clean/park operations, non-AFC
unload exit movement, homing/calibration positioning, native AFC maintenance,
idle/paused/no-profile states, early returns, lifecycle invalidation, error
recovery, saved-coordinate frames, and resume ordering.

User overrides should retain public safety wrappers and customize private motion
bodies instead. In particular, CC1 CANVAS overrides `_CLEAN_NOZZLE` while retaining
the guarded public `CLEAN_NOZZLE` entry point. Deploy the AFC recipe patch together
with the host Kalico pause patch and updated macro configuration; the lifecycle
helpers require these macros. The pause patch is in the host recipe only, not the
shared MCU firmware recipe include.
