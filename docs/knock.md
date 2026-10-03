# Knock gestures

Knock a rhythm on the printer frame and COSMOS runs a macro. The toolhead
accelerometer hears the knocks; no extra hardware. It only listens while the
printer is idle, never during a print, a move, or a resonance test, and it
costs nothing while printing.

## Quick start

All commands go in the console (Mainsail or Fluidd) with the printer idle.

1. `KNOCK` shows the current state. Knock the frame twice: the lights toggle.
   That is the built-in double tap.
2. `KNOCK CALIBRATE=1` teaches it your tempo. Follow the console: 5 quick
   knocks, 5 quick knocks again, then 3 knocks with a deliberate pause between
   them. Done once, saved.
3. Record a pattern:

       KNOCK RECORD=1 NAME=lamp MACRO=_KNOCK_TOGGLE_LIGHTS

   Knock your rhythm (two knocks or more). The console tells you what it heard
   and asks you to knock it again. Knock it again and it is bound.
4. Knock that rhythm whenever the printer is idle. The console shows what it
   heard and what it ran.

Patterns match by the proportions of the gaps between knocks, so knocking the
same rhythm faster or slower still works.

## The KNOCK command

| Parameter | Effect |
|---|---|
| `KNOCK` | List recorded patterns, built-in patterns, tempo and state |
| `LIST=1` | Same as bare `KNOCK` |
| `ENABLED=0` / `ENABLED=1` | Turn the feature off / on. Off means no polling and the accelerometer is powered down. Persists. |
| `CALIBRATE=1` | Tempo calibration, guided by the console |
| `RECORD=1 NAME=<name>` | Record a pattern under that name. Needs at least one action below. |
| `MACRO=<gcode>` | Action: run this command or macro. With arguments use quotes: `MACRO="M117 hello"` |
| `PARROT=1` | Action: parrot mode. The toolhead moves to the bed centre and replays your rhythm as small jerks. Homes first if needed. |
| `PRINT="<text>"` | Action: print this text to the console. Combines with MACRO or PARROT. |
| `DELETE=<name>` | Remove a recorded pattern |
| `REMIND=<name>` | Forgot a pattern? The console shows its rhythm and the toolhead plays it back, like parrot mode. Works for built-in patterns too. |

Examples:

    KNOCK RECORD=1 NAME=home MACRO=G28
    KNOCK RECORD=1 NAME=warm MACRO="M140 S60" PRINT="bed warming"
    KNOCK RECORD=1 NAME=echo PARROT=1
    KNOCK REMIND=warm
    KNOCK DELETE=echo
    KNOCK ENABLED=0

Messages you may see:

- `that pattern already belongs to '...'`: that rhythm is taken. Knock a different one.
- `that was different`: the confirmation knock did not match. Start over.
- `no pattern matched`: heard the knocks, nothing bound to that rhythm.

## Turning it off

In `cosmos.conf`, section `[klipper]`, set `knock = False` and reboot. The
module is then not loaded at all. `KNOCK ENABLED=0` turns it off at runtime
without a reboot.

## Where to knock

Anywhere on the frame works. The top front corners are reliable. Knocks need
to be at least 0.15 s apart; a pause longer than about a second ends the
pattern (up to 1.6 s after tempo calibration). Any number of knocks per
pattern.

## Configuration

`extras-readonly/knock.cfg` is included automatically when `knock = True`.
Options in its `[knock]` section:

| Option | Default | Meaning |
|---|---|---|
| `accel_chip` | `lis2dw` | Accelerometer section to use |
| `threshold` | `0.06` | Knock detection threshold in g. The default is the chip's most sensitive setting. Raise it if fans or the neighbours trigger it. |
| `odr` | `1600` | Accelerometer sample rate: 400, 800 or 1600 |
| `poll_hz` | `20` | How often the flag is read. Lower saves a little CPU, adds latency. |
| `tolerance` | `0.35` | How far a gap's share of the pattern may deviate from the recording |
| `echo` | `True` | Console line for every knock group |
| `verbose` | `False` | Log gap timings to klippy.log |

Built-in patterns are `[knock_pattern <name>]` sections with a `gcode` and
either `count: N` (any N knocks) or `pattern: .-.` (one symbol per gap,
`.` short, `-` long). Recorded patterns take precedence over built-in ones.
Add your own to `printer.cfg`.

## How it works

The LIS2DW12 on the toolhead has a hardware tap detector. While the printer is
idle it runs at 1600 Hz and latches a flag per knock. A poll at 20 Hz reads the
flag, counts knocks, times the gaps, and closes a group after a second of
silence. Recorded patterns store the gap proportions; a group matches when
every gap agrees within the tolerance. State lives in
`/user-resource/knock/knock.json`.

Cost: while the printer is idle the poll adds about 2% of one core and a few
kilobytes of memory. While printing, moving, or running a resonance test the
accelerometer is powered down and nothing is polled; the module only checks
the printer state once a second. Knocks during a print are ignored. Disabled
in cosmos.conf: not loaded at all.

Direction detection (which side of the frame was knocked) was tried and
dropped: the toolhead hangs on belts and rings the same way wherever the frame
is hit.
