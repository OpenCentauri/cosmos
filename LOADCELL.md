# Load-cell probe failure investigation

Last updated: 2026-07-20

## Scope and current state

This document tracks the intermittent Kalico probing failure reported as:

```text
Load Cell Probe Error: force exceeded drift_safety_limit before triggering!
```

The investigation is on `doom/loadcell`, based on `origin/main` at `a28b9a7`.
The existing local-only files and `build/conf/local.conf` changes are unrelated
and must remain untracked/unstaged.

The printer was inspected read-only after the failure. No G-Code, restart,
configuration change, firmware flash, or motion command was sent during this
pass, so `carbon2u` retains the useful post-failure state.

## Executive finding

The evidence strongly supports a firmware-side glitch-classification gap, not
real 1 kg mechanical drift.

One HX711 channel intermittently returns exactly `-1` (a 24-bit all-ones
frame). The host detects the per-channel jump and replaces it, but homing is
stopped on the bed MCU before the host processes that bulk-data frame. The MCU
has its own glitch check, but it compares only the **sum** of all channels to a
hard-coded `200000`-count threshold. This printer's drift guard is only
`105 counts/g * 1000 g = 105000 counts`. A corrupt summed jump can therefore be:

```text
large enough to trip the probe:       > 105000 counts
small enough to pass MCU glitch test: <= 200000 counts
```

That is exactly what happened in the retained failure. This is the leading
root cause and gives us a targeted bed-MCU firmware fix to test without
weakening probe safety.

## Software and configuration under test

The Yocto recipe pins the OpenCentauri Kalico fork's `hx711s-new` branch at:

```text
fa59d011f425c5aef6f06abc08efe43f7f6c569d
PR = r7
```

Relevant recipe:
`meta-opencentauri/recipes-apps/klipper/kalico_2026.02.00.inc`.

The running bed MCU confirms the same source revision:

```text
Loaded MCU 'bed' ... Kalico fa59d011-dirty-20260719_014415-cosmos-runner
```

The `-dirty` suffix is expected from downstream recipe patches. Checksums of
the running host-side `hx711s.py` and `load_cell_probe.py` match the locally
built image exactly.

Current relevant machine configuration:

```ini
[bed_mesh]
speed: 150
horizontal_move_z: 2
mesh_min: 10,10
mesh_max: 246,246
probe_count: 9,9

[load_cell_probe]
sensor_type: hx711s
sdo_pins: bed:PB14, bed:PC8, bed:PB15, bed:PA8
sclk_pins: bed:PB13, bed:PC7, bed:PC6, bed:PC9
sensor_orientation: inverted
sample_rate: 80
gain: A-128
speed: 2.0
sample_retract_dist: 0.5
lift_speed: 10.0
counts_per_gram: 105
reference_tare_counts: 71000, 71000, 71000, 71000
z_offset: 0.0
pullback_distance: 0.4
pullback_speed: 1.0
```

No explicit `drift_safety_limit` is configured, so Kalico uses its documented
default of 1000 g. Kalico describes this as a safety guard that must not be
casually disabled; it is evaluated before the SOS trigger filter. See the
[load-cell safety documentation](https://github.com/OpenCentauri/kalico/blob/fa59d011f425c5aef6f06abc08efe43f7f6c569d/docs/Load_Cell.md#L178-L184)
and the [MCU safety check](https://github.com/OpenCentauri/kalico/blob/fa59d011f425c5aef6f06abc08efe43f7f6c569d/src/load_cell_probe.c#L123-L163).

## Retained `carbon2u` failure evidence

The failure occurred during the heated bed-mesh portion of `FULL_CALIBRATE`:

- bed target/temperature: approximately 60 C
- nozzle target/temperature: approximately 140 C
- mesh: 9 x 9
- last successful point: `(98.501, 68.996)`, `z=-0.096221`
- the following probe attempt aborted with the drift-safety error
- no MCU shutdown, communication timeout, `DESYNC`, `READ_TOO_LONG`, forced
  sensor restart, or invalid MCU bytes accompanied the failure

The decisive adjacent log lines are:

```text
probe at 98.501,68.996 is z=-0.096221
Probe pos:[98.5, 69.0, -0.09622064038272493]
retry policy=2
Error during homing probe: Load Cell Probe Error: force exceeded drift_safety_limit before triggering!
Error during homing probe: Load Cell Probe Error: force exceeded drift_safety_limit before triggering!
Error during homing probe: Load Cell Probe Error: force exceeded drift_safety_limit before triggering!
load_cell_probe: glitch dropped at t=3331.248; channel(s) [1] jumped (now=[143744, -1, 264097, 4394] last=[143826, -144489, 264082, 4188])
```

## Original issue #241 sample

The issue report contains the following raw host-side glitch records. They
are preserved here verbatim so the test result can be compared against the
original failure without relying on the issue page:

```text
load_cell_probe: glitch dropped at t=2501.483; channel(s) [1] jumped (now=[-155235, -1, -62751, 474210] last=[-155168, -310564, -62757, 474271])
load_cell_probe: glitch dropped at t=2507.727; channel(s) [0] jumped (now=[-1, -310749, -62435, 474152] last=[-155181, -310757, -62686, 474145])
load_cell_probe: glitch dropped at t=2509.260; channel(s) [1] jumped (now=[-155137, -1, -62600, 474298] last=[-155185, -310779, -62539, 474366])
load_cell_probe: glitch dropped at t=2542.416; channel(s) [1] jumped (now=[-154917, -1, -62082, 474642] last=[-154707, -310348, -61933, 474616])
load_cell_probe: glitch dropped at t=2592.866; channel(s) [0] jumped (now=[-1, -310064, -61460, 475185] last=[-154207, -310151, -61475, 475140])
load_cell_probe: glitch dropped at t=2600.950; channel(s) [0] jumped (now=[-1, -309825, -61667, 475141] last=[-154023, -309835, -61705, 475248])
load_cell_probe: glitch dropped at t=2662.799; channel(s) [1] jumped (now=[-153409, -1, -60962, 476215] last=[-153481, -309103, -61052, 475995])
load_cell_probe: glitch dropped at t=2684.625; channel(s) [0] jumped (now=[-1, -309159, -60895, 476382] last=[-153252, -309040, -60912, 476291])
load_cell_probe: glitch dropped at t=2692.967; channel(s) [0] jumped (now=[-1, -309131, -60652, 476287] last=[-153239, -309116, -60595, 476300])
load_cell_probe: glitch dropped at t=2706.176; channel(s) [0] jumped (now=[-1, -308783, -60913, 476374] last=[-152967, -309000, -60683, 476265])
load_cell_probe: glitch dropped at t=2707.894; channel(s) [1] jumped (now=[-152854, -1, -60842, 476441] last=[-152855, -308916, -60758, 476501])
load_cell_probe: glitch dropped at t=2709.549; channel(s) [0] jumped (now=[-1, -308815, -60844, 476578] last=[-152915, -308702, -60821, 476556])
```

The normal channel-0 value near `-155000` changing to `-1` is a jump of
about `155000` counts: large enough to exceed this machine's `105000`-count
drift guard, but below the old MCU-wide `200000`-count sum threshold. Channel
1 jumps from about `-310000` to `-1`, which is larger still. This explains why
the old sum-only filter was machine- and channel-dependent.

## Implemented fix and build status

Patch `0003-fix-hx711s-mcu-glitch-filter.patch` changes the bed MCU reader to
compare each HX711S channel against its last good value using the existing
`100000`-count physical-jump bound. One suspicious frame is quarantined. A
following frame that returns to the prior range is treated as an isolated
corruption; two consecutive samples near a new value are accepted as a real
force change. Raw samples remain in the bulk buffer for host diagnostics, and
the host drift/force safety limits are unchanged.

The STM32F401 bed firmware compiled and linked successfully, and the complete
Yocto `opencentauri-upgrade` image completed all 6521 tasks successfully. The
first image flash preserved calibration but did not replace the bed MCU because
the package revision remained `r7`; bumping `PR` to `r8` forced the init script
to install the patched firmware. The saved `SAVE_CONFIG` block survived both
reboots, including the four-channel tare.

## Hardware validation on `carbon2u`

After the `r8` image was installed:

1. A cold `G28` completed. Homing logged an isolated channel-2 `-1` frame but
   did not abort and produced no new drift-safety error.
2. `LOAD_CELL_DIAGNOSTIC` ran for ten seconds successfully.
3. A complete 9 x 9 `BED_MESH_CALIBRATE PROFILE=default` completed and saved
   the profile. It encountered multiple channel-1/channel-2 `-1` frames with
   no drift-safety abort, MCU shutdown, or communication failure.
4. A second complete 9 x 9 mesh also completed and saved to `default`, again
   with additional `-1` frames but no safety or MCU error.

The repeated meshes demonstrate the targeted behavior: corrupt frames remain
visible in host diagnostics, while one-frame jumps no longer falsely trip the
probe safety range. The physical source of the `-1` reads remains a follow-up
hardware/signal-integrity investigation.

The three identical error lines appear to be propagation through the homing,
probe, and macro layers; they are not evidence of three independent bad ADC
frames.

### Exact count arithmetic

```text
previous sum = 143826 - 144489 + 264082 + 4188 = 267607
bad sum      = 143744 - 1      + 264097 + 4394 = 412234
sum jump     = 412234 - 267607                 = 144627 counts

host per-channel jump on channel 1:
abs(-1 - -144489) = 144488 counts

configured drift band:
105 counts/g * 1000 g = 105000 counts
```

Therefore:

- `144488 > 100000`: the host's per-channel detector correctly calls it a
  glitch.
- `144627 <= 200000`: the MCU's sum detector incorrectly accepts it.
- `144627 > 105000`: the accepted sample crosses the probe's drift band and
  stops homing.

The error is produced immediately on the MCU. The host receives and decodes
the bulk frame afterward, which is why `glitch dropped` follows the homing
error in the log even though both describe the same ADC sample.

### Frequency and channel pattern

In the retained boot:

| Interval | Total glitches | Channel 1 | Channel 2 |
| --- | ---: | ---: | ---: |
| Through the failure at `t=3331.248` | 85 | 13 | 72 |
| Entire retained log at collection time | 121 | 22 | 99 |

Every collected glitch had the affected channel become exactly `-1`.

This channel distribution also fits the threshold bug:

- channel 1 was near `-144000`; changing to `-1` produces a roughly 144000
  count sum jump, which passes the MCU's 200000 threshold but exceeds the
  105000 drift band;
- channel 2 was near `+264000`; changing to `-1` produces a roughly 264000
  count sum jump, so the MCU suppresses it before it reaches the probe;
- consequently, the many channel-2 glitches are mostly harmless while a rarer
  channel-1 glitch can abort a probe.

## Why multiple printers see a random failure

[Cosmos issue #241](https://github.com/OpenCentauri/cosmos/issues/241)
contains the same intermittent bed-mesh error from another printer, another
user confirming the inconsistency, and the same exact `-1` channel signature.
That report's normal channel magnitudes differ from `carbon2u`, so a different
channel falls into the vulnerable 105000-to-200000 count window.

This explains the apparently random and machine-dependent behavior:

1. The new multi-HX711 driver is shared by all affected printers.
2. A framing-valid all-ones sample occurs intermittently on one ADC channel.
3. Whether it stops probing depends on that channel's normal offset and the
   sum of the other channels, not only on whether corruption occurred.
4. More probing creates more opportunities for a bad frame. This is consistent
   with reports that long meshes or KAMP appear worse without requiring KAMP
   itself to be defective.

This is more consistent with a systemic firmware handling defect than with
several printers independently developing a real one-kilogram load transient.
Wiring, grounding, or EMI may influence how often a corrupt frame occurs, but
the driver must safely reject a recognized corrupt frame regardless of its
physical trigger.

## Relevant new-driver behavior and regression boundary

Kalico's generic load-cell probing implementation deliberately performs the
endstop decision on the MCU for deterministic motion stopping. The design is
described in upstream [Kalico PR #760](https://github.com/KalicoCrew/kalico/pull/760).
That architecture means a host-only retry, filter, or Python change cannot
prevent this particular false endstop error.

The multi-HX711 driver exists only on OpenCentauri's feature branch, not on
KalicoCrew `main`. Its recent hardening sequence was:

- [wait for all sensors to be ready](https://github.com/OpenCentauri/kalico/commit/97fff812dcda57234483eb7280c25d442f3ae32a)
- [read all load cells in lockstep](https://github.com/OpenCentauri/kalico/commit/88c6162582431cbe5d753238340580c9b58940f9)
- [detect torn reads](https://github.com/OpenCentauri/kalico/commit/ca304125ee6f39d240c5d6ff4e03c140dd7da4db)
- [hold the last valid torn sample](https://github.com/OpenCentauri/kalico/commit/3cbab5175b86b88247db12d6ef68eb287109f710)
- [detect and log framing-valid glitches](https://github.com/OpenCentauri/kalico/commit/fa59d011f425c5aef6f06abc08efe43f7f6c569d)

The final commit acknowledges that corrupt frames can pass the earlier framing
checks. It adds a host threshold of 100000 counts per channel, but an MCU
threshold of 200000 counts on the summed sample. See its
[host detector](https://github.com/OpenCentauri/kalico/blob/fa59d011f425c5aef6f06abc08efe43f7f6c569d/klippy/extras/load_cell/hx711s.py#L145-L204)
and [MCU detector](https://github.com/OpenCentauri/kalico/blob/fa59d011f425c5aef6f06abc08efe43f7f6c569d/src/sensor_hx711s.c#L213-L232).
No later commit exists on `hx711s-new` as of this investigation.

An all-ones 24-bit frame becomes signed count `-1`, and the extra gain/channel
bits are also ones, so the current extra-bit framing check does not classify
it as `DESYNC`. The existing pre/post DOUT test also did not classify the
retained frame as a torn read. It reaches the later heuristic detector instead.

## Ranked hypotheses

### 1. MCU sum threshold lets a known corrupt frame reach the probe — very high confidence

This is directly demonstrated by the retained frame and exact thresholds. It
is sufficient to explain the observed abort even without knowing what
electrically caused the all-ones read.

### 2. HX711 lockstep read has an intermittent all-ones acquisition failure — high confidence

The repeated exact `-1`, framing-valid shape is not plausible mechanical
drift. It may be a narrow DOUT/read timing race, a readiness assumption that
does not hold for every chip, or GPIO/bit-bang timing sensitivity. Board noise,
grounding, and wiring could change the frequency. This is the underlying data
source to isolate after preventing it from defeating probe safety.

### 3. The drift guard is merely too small — low confidence as a root cause

The guard is operating exactly as designed on the bad sample. Increasing it
above roughly 1377 g would hide this one frame but would reduce collision
protection and would not cover channels with larger offsets. Disabling it is
explicitly unsafe and would leave corrupt input available to the trigger
filter.

### 4. Tare duration, thermal drift, or residual motion — possible secondary effects, unlikely for this failure

The default dynamic tare is only 0.1 s (8 samples at 80 SPS), and hot meshes
can expose real drift or motion settling issues. Those deserve controlled
testing, but they do not explain a single channel becoming exactly `-1` at the
same instant as the error. A longer tare or dwell cannot repair a corrupt
sample that occurs after tare.

### 5. SOS/tap analysis/classifier defect — low confidence for this error

The MCU evaluates the raw drift range before SOS filtering, and tap analysis
runs after a successful trigger. Neither can create this pre-trigger safety
reason. They may cause separate tap-quality failures and should not be mixed
into this fix.

### 6. Host/MCU transport loss — low confidence for this failure

The bed MCU makes this decision locally, and its direct sensor-to-probe path
does not depend on the Linux host receiving the bulk frame in time. The
retained run has no matching communication timeout, MCU shutdown, invalid-byte
count, or HX711 `READ_TOO_LONG`/`DESYNC` error.

## Recommended fix direction

### First test patch: classify per channel on the MCU

Replace the MCU's sum-only history with last-good counts for every HX711
channel and apply the same per-channel rule used by the host before forwarding
the sum to `load_cell_probe_report_sample()`.

Important properties:

- keep the raw frame in the bulk buffer so the host still identifies and logs
  the bad channel;
- do not forward a recognized isolated corrupt frame into the MCU endstop;
- do not update the last-good channel values from a rejected frame;
- reset history when measurements restart;
- retain the existing torn-read, desync, overflow, and watchdog behavior;
- add a dropped-frame counter/channel mask if it fits the MCU protocol cleanly.

A minimal per-channel 100000-count test should reject the exact retained frame
and confirm the diagnosis. A sum-only reduction from 200000 to 100000 would
also reject this sample, but it remains vulnerable to cancellation between
channels and to machines with different per-channel offsets. It is useful as
an A/B experiment, not the preferred final design.

### Production safety refinement: bounded confirmation

A permanent spike filter must not hide a genuine, sustained collision. A
large real force step could also exceed a per-sample heuristic threshold.
The production implementation should quarantine only the first suspicious
sample and use the following sample to distinguish:

- return close to the last good reading: isolated corrupt frame, drop/hold it;
- remain close to the suspicious level or continue in the same direction:
  genuine force change, forward it to the safety/trigger path immediately.

At 80 SPS, one confirmation sample costs about 12.5 ms, or 0.025 mm of Z
travel at the configured 2 mm/s. This must be validated against the existing
watchdog and collision-force requirements. An exact all-ones fast path may be
appropriate in addition to the general confirmation logic, but it should not
be the only protection against other corrupt values.

The current sum filter already suppresses large single frames, so this work
should improve its safety semantics rather than add an entirely new behavior.

### Do not use as the production fix

- `drift_safety_limit: 0`
- broadly increasing `drift_safety_limit`
- retrying indefinitely in macros
- masking the exception in bed mesh/KAMP
- only changing Python filtering
- slowing all probes or adding a large unconditional dwell

These either reduce safety, hide failures, or run too late in the data path.

## Reproduction and isolation plan

### Phase 0 — preserve baseline evidence (complete)

- Captured exact Kalico and bed-MCU revision.
- Captured config, failure context, temperatures, raw/last channel values, and
  glitch counts without altering printer state.
- Correlated the failed sample against all three relevant thresholds.

Do not rotate/delete the current `/board-resource/klippy.log` until the first
patch is ready. The concise evidence needed for development is recorded here;
the multi-megabyte raw log should not be committed.

### Phase 1 — deterministic host-side regression test

Before moving the printer, construct a small test around the MCU classifier
logic using recorded count sequences:

1. steady valid frames near the retained baseline;
2. the exact channel-1 `-1` frame from `t=3331.248`;
3. return to the prior baseline;
4. a channel-2 `-1` frame currently caught only because its sum exceeds
   200000;
5. a legitimate gradual contact ramp through the 75 g trigger;
6. a genuine sudden/sustained overload through the 1000 g drift guard;
7. simultaneous opposing channel jumps to test sum cancellation;
8. corrupt first frame after sensor restart.

Assertions:

- the recorded isolated glitches never reach `load_cell_probe_report_sample`;
- the normal trigger ramp still reaches it without delay;
- a sustained overload is never filtered indefinitely and still stops motion;
- state recovers on the first valid frame after an isolated glitch;
- integer-difference and sum calculations cannot overflow.

This gives a fast A/B demonstration of current versus patched behavior and
protects the safety edge cases that hardware-only mesh testing would miss.

### Phase 2 — add targeted diagnostics

For a diagnostic build, record enough state to prove each decision without
logging at 80 Hz:

- previous and current per-channel counts;
- channel bitmask that caused rejection;
- previous/current sum and per-channel delta;
- active tare, drift minimum/maximum, and trigger threshold;
- number of isolated frames, confirmed sustained changes, torn reads,
  desyncs, and overflows;
- probe position and bed/nozzle temperatures from the host at failure.

Prefer counters plus one rate-limited warning over MCU-side per-sample text.
The existing host bulk stream should continue to carry the raw corrupt frame.

### Phase 3 — safe hardware reproduction on `carbon2u`

Run each condition first on current firmware to establish rate, then on the
test patch. Change one variable at a time and retain the log for each run.

1. Idle, cold, motors off: 10-minute acquisition/diagnostic capture.
2. Idle with bed at 60 C and nozzle at 140 C: 10-minute capture.
3. XY motion at mesh speed while Z remains safely clear of the bed.
4. A small number of fixed-center probes, then 20-probe accuracy testing.
5. One 3 x 3 cold mesh, then one 3 x 3 hot mesh.
6. Repeated full 9 x 9 hot meshes only after the smaller tests pass.
7. `FULL_CALIBRATE`, including load-cell tare, input shaping, and bed mesh.
8. KAMP/adaptive mesh using the same temperatures and probing speed.

Stop on any new safety, position, heater, MCU, or tap-quality error. Do not
disable either force safety guard to obtain a passing run.

### Phase 4 — isolate the all-ones acquisition source

Once false endstop aborts are safely contained, vary acquisition conditions:

- 80 SPS versus 10 SPS (diagnostic only; 10 SPS increases trigger latency and
  overshoot and is not a likely production setting);
- heaters off/on and motors idle/moving to look for EMI correlation;
- cold versus thermally soaked bed;
- per-channel physical connector/pin mapping, one controlled swap at a time,
  to learn whether the fault follows the HX711/load cell, GPIO input, cable,
  or logical index;
- small, bounded changes to read timing/readiness checks in separate builds;
- logic-analyzer capture of DOUT and SCLK around an all-ones frame if the
  event rate remains high enough.

The goal is to distinguish a chip conversion/read race from board-level
signal integrity. Even if a hardware contributor is found, the MCU should
still fail safely on a framing-valid corrupt frame.

### Phase 5 — only then test secondary probe parameters

If patched firmware still produces drift errors without a matching corrupt
frame, test these independently:

1. `tare_time`: 0.10, 0.25, then 0.50 s;
2. a short pre-tare settling dwell;
3. `sample_retract_dist`: 0.5, 1.0, then 2.0 mm;
4. cold versus hot probing;
5. fixed point versus long mesh travel;
6. drift-filter disabled versus the configured/calibrated cutoff, if one is
   later enabled.

These experiments isolate residual vibration, bed creep, thermal drift, and
Bowden/drag-chain loading. They should not be combined with the glitch patch
in the first A/B test.

## Validation and acceptance criteria

A candidate is ready for broader testing when all of the following are true:

- deterministic regression vectors pass, including a sustained real-force
  step that cannot be hidden by the glitch classifier;
- the retained `t=3331.248` frame is rejected before the MCU probe path;
- raw corrupt frames remain visible and attributable by channel on the host;
- no false drift abort in at least 25 hot full 9 x 9 meshes (2025 taps);
- no regression in trigger height, probe repeatability, tap-quality retries,
  or peak contact force;
- cold/hot `FULL_CALIBRATE` and KAMP/adaptive meshes complete;
- the test is repeated on at least one additional affected mainboard, ideally
  one whose glitching channel has a different normal offset;
- watchdog, desync, read-too-long, overflow, force-safety, and drift-safety
  tests still stop safely and report the correct reason.

The 25-mesh target is intentionally exposure-based: this failure depends on a
rare ADC frame, so one or two successful meshes would not demonstrate that the
handling is fixed.

## Proposed implementation sequence for this branch

1. Add deterministic classifier tests and reproduce the retained failure in
   the current code model.
2. Implement per-channel MCU history and a minimal aligned classifier.
3. Add bounded confirmation so genuine sustained force cannot be hidden.
4. Add concise counters/rate-limited diagnostics.
5. Build and flash `carbon2u`; run the phased cold/hot test matrix.
6. Investigate acquisition timing/hardware correlation separately if `-1`
   frames continue, while verifying they no longer abort safe probing.
7. Submit the HX711 driver fix upstream to `OpenCentauri/kalico`, then pin the
   corrected revision in the Yocto recipe and document validation in the PR.

## Current conclusion

The next experiment should not adjust probe configuration. It should patch
the **bed MCU's HX711 multi-channel classifier** and replay the exact failing
frame. The retained data supplies both a deterministic regression vector and
a numerical explanation for why the existing June 26 workaround improved
some glitches but still fails for multiple users.
