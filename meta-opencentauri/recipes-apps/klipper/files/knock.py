# Knock gestures: run gcode when the user knocks a rhythm on the printer frame.
#
# Copyright (C) 2026  Shawn Rains <shawn.rains@area9.dk>
#
# This file may be distributed under the terms of the GNU GPLv3 license.
#
# The toolhead accelerometer (LIS2DW12) runs its hardware tap engine while
# the printer is idle. Every knock latches a per-axis tap flag, which a slow
# poll picks up. Knocks are grouped, the gaps between them are read as a
# rhythm, and the group is matched against recorded patterns and
# [knock_pattern] sections. Nothing runs while the printer is printing,
# moving, or running a resonance test.
#
# One reactor task owns all accelerometer SPI traffic; gcode commands only set
# flags for it.
import json
import logging
import os

REG_CTRL1 = 0x20
REG_CTRL3 = 0x22
REG_CTRL4 = 0x23
REG_CTRL6 = 0x25
REG_TAP_THS_X = 0x30
REG_TAP_THS_Y = 0x31
REG_TAP_THS_Z = 0x32
REG_INT_DUR = 0x33
REG_WAKE_UP_THS = 0x34
REG_TAP_SRC = 0x39
REG_CTRL7 = 0x3F
CTRL1_ODR = {400: 0x74, 800: 0x84, 1600: 0x94}  # high-performance mode
CTRL1_OFF = 0x00
TAP_SRC_HIT = 0x37  # X/Y/Z tap, single, double
TAP_THS_LSB_G = 2.0 / 32  # threshold step at +-2 g
DEBOUNCE = 0.12  # from knock onset; ringing lasts <= 0.1 s, knocks >= 0.15 s
SLEEP_POLL = 5.0  # s between state checks while asleep (busy or disabled)
IDLE_STATES = ("Idle", "Ready")
STATE_FILE = "/user-resource/knock/knock.json"
DEFAULT_TEMPO = {"short_max": 0.42, "group_end": 1.0}
SYMBOL_WEIGHT = {".": 1.0, "-": 2.5}  # relative gap lengths for pattern strings
PARROT_STEP = 1.5  # mm of X jitter per replayed knock
PARROT_FEED = 12000  # mm/min
REMIND_SHORT_GAP = 0.25  # s, shortest gap when replaying without stored timing


class Knock:
    def __init__(self, config):
        self.printer = config.get_printer()
        self.reactor = self.printer.get_reactor()
        self.gcode = self.printer.lookup_object("gcode")
        self.chip_name = config.get("accel_chip", "lis2dw")
        g = config.getfloat("threshold", 0.06, above=0.0)
        self.threshold = max(1, min(31, int(round(g / TAP_THS_LSB_G))))
        self.odr = config.getchoice(
            "odr", {str(k): k for k in CTRL1_ODR}, "1600"
        )
        self.poll = 1.0 / config.getfloat(
            "poll_hz", 20.0, minval=5.0, maxval=50.0
        )
        self.echo = config.getboolean("echo", True)
        self.verbose = config.getboolean("verbose", False)
        self.tolerance = config.getfloat(
            "tolerance", 0.35, minval=0.05, maxval=0.9
        )
        self.state_file = config.get("state_file", STATE_FILE)
        gcode_macro = self.printer.load_object(config, "gcode_macro")
        # (name, count, rhythm or None, profile or None, template)
        self.patterns = []
        self.pattern_gcode = {}  # name -> first gcode line, for KNOCK LIST
        for section in config.get_prefix_sections("knock_pattern "):
            name = section.get_name().split(None, 1)[1]
            rhythm = section.get("pattern", None)
            count = section.getint("count", None, minval=1)
            if rhythm is not None:
                rhythm = rhythm.strip()
                if not rhythm or set(rhythm) - set(".-"):
                    raise config.error(
                        "knock_pattern %s: pattern may only contain . and -"
                        % (name,)
                    )
                if count is not None and count != len(rhythm) + 1:
                    raise config.error(
                        "knock_pattern %s: count does not match pattern"
                        % (name,)
                    )
                count = len(rhythm) + 1
            elif count is None:
                raise config.error(
                    "knock_pattern %s: needs 'pattern' or 'count'" % (name,)
                )
            profile = None
            if rhythm:
                profile = self._profile([SYMBOL_WEIGHT[c] for c in rhythm])
            template = gcode_macro.load_template(section, "gcode")
            self.patterns.append((name, count, rhythm, profile, template))
            first_line = section.get("gcode").strip().splitlines()[:1]
            self.pattern_gcode[name] = (
                first_line[0].strip() if first_line else ""
            )
        # rhythm patterns before count-only ones
        self.patterns.sort(key=lambda p: p[2] is None)
        self.chip = self.idle_timeout = None
        self.enabled = True
        self.tempo = dict(DEFAULT_TEMPO)
        self.bindings = {}  # name -> {count, rhythm, profile, gaps, actions}
        self.calibration = None  # ("quick"|"slow", [quick gaps])
        self.recording = None  # {"name", "macro", "parrot", "print", "first"}
        self.last = {}
        self.gcode.register_command(
            "KNOCK", self.cmd_KNOCK, desc=self.cmd_KNOCK_help
        )
        self.wake = None
        self.printer.register_event_handler("klippy:ready", self._handle_ready)
        for event in ("idle_timeout:ready", "idle_timeout:idle"):
            self.printer.register_event_handler(event, self._wake_up)

    def _wake_up(self, *args):
        if self.wake is not None:
            self.wake.complete(None)

    def _handle_ready(self):
        self.chip = self.printer.lookup_object(self.chip_name)
        self.idle_timeout = self.printer.lookup_object("idle_timeout")
        self._load_state()
        self.reactor.register_callback(self._poll_loop)

    def _say(self, msg):
        self.gcode.respond_info("knock: " + msg)

    # State persistence
    def _load_state(self):
        try:
            with open(self.state_file) as f:
                data = json.load(f)
            self.enabled = bool(data.get("enabled", True))
            tempo = data.get("tempo", {})
            self.tempo = {
                k: float(tempo.get(k, v)) for k, v in DEFAULT_TEMPO.items()
            }
            self.bindings = {
                str(k): dict(v) for k, v in data.get("bindings", {}).items()
            }
            for b in self.bindings.values():  # upgrade older formats
                if not b.get("profile") and b.get("rhythm"):
                    b["profile"] = self._profile(
                        [SYMBOL_WEIGHT[c] for c in b["rhythm"]]
                    )
                if "action" in b:
                    action, value = b.pop("action"), b.pop("value", None)
                    b.setdefault("macro", value if action == "macro" else None)
                    b.setdefault("parrot", action == "parrot")
                    b.setdefault("print", value if action == "print" else None)
        except (IOError, ValueError, TypeError, AttributeError):
            pass
        logging.info(
            "knock: enabled=%s tempo=%s bindings=%s",
            self.enabled,
            self.tempo,
            sorted(self.bindings),
        )

    def _save_state(self):
        try:
            os.makedirs(os.path.dirname(self.state_file), exist_ok=True)
            with open(self.state_file, "w") as f:
                json.dump(
                    {
                        "enabled": self.enabled,
                        "tempo": self.tempo,
                        "bindings": self.bindings,
                    },
                    f,
                )
        except IOError:
            logging.exception("knock: unable to save %s", self.state_file)

    # Accelerometer. Uses the lis2dw object's register helpers and its bulk
    # helper's is_started flag; the tap engine is off whenever a measurement
    # (resonance test, ACCELEROMETER_MEASURE) owns the chip.
    def _arm(self):
        set_reg = self.chip.set_reg
        set_reg(REG_CTRL6, 0x04)  # +-2 g, low noise
        set_reg(REG_CTRL3, 0x10)  # latch tap flags until read
        set_reg(REG_CTRL4, 0x48)  # route taps to INT1 so the latch applies
        set_reg(REG_TAP_THS_X, self.threshold)
        set_reg(REG_TAP_THS_Y, self.threshold)
        set_reg(REG_TAP_THS_Z, 0xE0 | self.threshold)  # enable X, Y, Z
        set_reg(REG_INT_DUR, 0x7F)
        set_reg(REG_WAKE_UP_THS, 0x00)  # single-tap mode
        set_reg(REG_CTRL7, 0x20)  # interrupts enable
        set_reg(REG_CTRL1, CTRL1_ODR[self.odr])
        self.chip.read_reg(REG_TAP_SRC)
        logging.info(
            "knock: armed %s at %d Hz, threshold %d",
            self.chip_name,
            self.odr,
            self.threshold,
        )

    def _disarm(self):
        try:
            self.chip.set_reg(REG_CTRL1, CTRL1_OFF)
        except Exception:
            logging.exception("knock: disarm failed")

    def _is_idle(self, eventtime):
        state = self.idle_timeout.get_status(eventtime)["state"]
        return state in IDLE_STATES and not self.chip.batch_bulk.is_started

    # Runs as a reactor callback for the life of klippy: SPI reads block, so
    # this must be a greenlet (like bulk_sensor), not a timer.
    def _poll_loop(self, eventtime):
        armed = False
        tick = 0
        times = []
        while not self.printer.is_shutdown():
            if armed:
                eventtime = self.reactor.pause(eventtime + self.poll)
            else:
                # Disabled, printing, moving or measuring: nothing runs. The
                # task sleeps until idle_timeout reports the printer idle again
                # (or KNOCK ENABLED=1); the slow timeout only covers a
                # measurement that started while idle.
                self.wake = self.reactor.completion()
                self.wake.wait(eventtime + SLEEP_POLL)
                eventtime = self.reactor.monotonic()
            try:
                if not self.enabled or not self._is_idle(eventtime):
                    if armed:
                        if not self.chip.batch_bulk.is_started:
                            self._disarm()  # chip powered down
                        armed = False
                    times = []
                    continue
                if not armed or (
                    tick % 100 == 0
                    and self.chip.read_reg(REG_CTRL1) != CTRL1_ODR[self.odr]
                ):
                    self._arm()  # also re-arms after a measurement
                    armed = True
                tick += 1
                if self.chip.read_reg(REG_TAP_SRC) & TAP_SRC_HIT:
                    if not times or eventtime - times[-1] > DEBOUNCE:
                        times.append(eventtime)
                if times and eventtime - times[-1] > self.tempo["group_end"]:
                    self._handle_group(times)
                    times = []
            except Exception:
                logging.exception("knock: poll failed, retrying")
                armed = False
                eventtime = self.reactor.pause(eventtime + 2.0)

    # Knock groups
    def _rhythm(self, gaps):
        short = self.tempo["short_max"]
        return "".join("." if g <= short else "-" for g in gaps)

    @staticmethod
    def _profile(gaps):
        total = sum(gaps) or 1.0
        return [g / total for g in gaps]

    def _same_pattern(self, count, rhythm, gaps, ocount, orhythm, oprofile):
        """Tempo-invariant comparison: gap proportions within tolerance. A
        lone gap has no proportions, so two-knock patterns compare their
        absolute short/long symbol instead."""
        if count != ocount:
            return None
        if count < 3 or oprofile is None:
            return 0.0 if rhythm == orhythm else None
        d = max(
            abs(a - b) / max(a, b, 0.05)
            for a, b in zip(self._profile(gaps), oprofile)
        )
        return d if d <= self.tolerance else None

    def _handle_group(self, times):
        gaps = [round(b - a, 3) for a, b in zip(times, times[1:])]
        count = len(times)
        if self.calibration is not None:
            self._calibrate_group(gaps)
            return
        rhythm = self._rhythm(gaps)
        if self.recording is not None:
            self._record_group(count, rhythm, gaps)
            return
        name, action = self._match(count, rhythm, gaps)
        self.last = {
            "count": count,
            "rhythm": rhythm,
            "gaps": gaps,
            "matched": name,
        }
        if self.verbose or name is None:
            logging.info(
                "knock: %d taps rhythm=%s gaps=%s -> %s",
                count,
                rhythm,
                gaps,
                name,
            )
        if self.echo:
            self._say(name if name else "no pattern matched")
        if action is not None:
            try:
                action(gaps)
            except Exception:
                logging.exception("knock: action %s failed", name)

    def _match(self, count, rhythm, gaps):
        best = None
        for name, b in self.bindings.items():
            d = self._same_pattern(
                count, rhythm, gaps, b["count"], b["rhythm"], b.get("profile")
            )
            if d is not None and (best is None or d < best[0]):
                best = (d, name, self._binding_action(b))
        if best is not None:
            return best[1], best[2]
        for name, pcount, prhythm, pprofile, template in self.patterns:
            if prhythm is None:
                hit = pcount == count
            else:
                hit = (
                    self._same_pattern(
                        count, rhythm, gaps, pcount, prhythm, pprofile
                    )
                    is not None
                )
            if hit:
                return name, lambda gaps, t=template: t.run_gcode_from_command()
        return None, None

    def _owner(self, count, rhythm, gaps):
        name, _ = self._match(count, rhythm, gaps)
        return name

    def _binding_action(self, b):
        def run(gaps):
            if b.get("print"):
                self.gcode.respond_info(b["print"])
            if b.get("macro"):
                self.gcode.run_script(b["macro"])
            if b.get("parrot"):
                self._parrot(gaps)

        return run

    def _parrot(self, gaps, run_script=None):
        """Replay a knock sequence as toolhead jitter from the bed centre."""
        toolhead = self.printer.lookup_object("toolhead")
        status = toolhead.get_status(self.reactor.monotonic())
        lo, hi = status["axis_minimum"], status["axis_maximum"]
        centre = [(lo[i] + hi[i]) / 2.0 for i in (0, 1)]
        script = ["SAVE_GCODE_STATE NAME=knock_parrot"]
        homed = status["homed_axes"]
        if not ("x" in homed and "y" in homed):
            script += ['RESPOND MSG="knock: homing first"', "G28"]
        script += [
            'RESPOND MSG="knock: playing - listen to the printer"',
            "G90",
            "G1 X%.1f Y%.1f F6000" % tuple(centre),
            "G91",
        ]
        for gap in gaps + [None]:
            script += [
                "G1 X%.2f F%d" % (PARROT_STEP, PARROT_FEED),
                "G1 X-%.2f F%d" % (PARROT_STEP, PARROT_FEED),
            ]
            if gap is not None:
                script.append("G4 P%d" % (max(0.0, gap - 0.03) * 1000,))
        script.append("RESTORE_GCODE_STATE NAME=knock_parrot")
        (run_script or self.gcode.run_script)("\n".join(script))

    def _binding_gaps(self, b):
        """Gap timings for a binding; older bindings only stored proportions."""
        if b.get("gaps"):
            return list(b["gaps"])
        scale = REMIND_SHORT_GAP / (min(b["profile"]) or 1.0)
        return [round(p * scale, 3) for p in b["profile"]]

    # Tempo calibration
    def _calibrate_group(self, gaps):
        step, quick = self.calibration
        if step == "quick":
            if len(gaps) < 3:
                self._say("that was too few - knock 5 times, quickly")
                return
            quick.append(sorted(gaps)[len(gaps) // 2])
            if len(quick) < 2:
                self._say(">>> good. Once more: 5 quick knocks <<<")
                return
            self.calibration = ("slow", quick)
            self._say(">>> now 3 knocks with a clear pause between them <<<")
            return
        if len(gaps) < 2:
            self._say("that was too few - knock 3 times, with pauses")
            return
        quick_gap = sum(quick) / len(quick)
        slow_gap = sorted(gaps)[len(gaps) // 2]
        if slow_gap < quick_gap * 1.5:
            self._say("the pauses were too short - try again, slower")
            return
        self.tempo = {
            "short_max": round((quick_gap + slow_gap) / 2.0, 3),
            "group_end": round(min(max(slow_gap * 1.7, 0.8), 1.6), 3),
        }
        logging.info(
            "knock: calibrated quick=%.3f slow=%.3f tempo=%s",
            quick_gap,
            slow_gap,
            self.tempo,
        )
        self.calibration = None
        self._save_state()
        self._say("calibrated")

    # Pattern recording
    def _record_group(self, count, rhythm, gaps):
        rec = self.recording
        if count < 2:
            self._say("at least two knocks, please")
            return
        owner = self._owner(count, rhythm, gaps)
        if owner is not None:
            self._say(
                "that pattern already belongs to '%s' - try another" % (owner,)
            )
            return
        if rec["first"] is None:
            rec["first"] = (count, rhythm, gaps)
            self._say(">>> got it. Knock it once more to confirm <<<")
            return
        fcount, frhythm, fgaps = rec["first"]
        same = self._same_pattern(
            count, rhythm, gaps, fcount, frhythm, self._profile(fgaps)
        )
        if same is None:
            rec["first"] = None
            self._say(
                ">>> that was different. Start over: knock the pattern "
                "for '%s' <<<" % (rec["name"],)
            )
            return
        avg = [(a + b) / 2.0 for a, b in zip(fgaps, gaps)]
        self.bindings[rec["name"]] = {
            "count": count,
            "rhythm": frhythm,
            "profile": self._profile(avg),
            "gaps": [round(g, 3) for g in avg],
            "macro": rec["macro"],
            "parrot": rec["parrot"],
            "print": rec["print"],
        }
        logging.info(
            "knock: recorded %s: %d taps rhythm=%s gaps=%s",
            rec["name"],
            count,
            frhythm,
            avg,
        )
        self.recording = None
        self._save_state()
        self._say("'%s' saved" % (rec["name"],))

    # Command
    cmd_KNOCK_help = (
        "KNOCK [ENABLED=0|1] [CALIBRATE=1] [LIST=1] [DELETE=<name>] "
        "[REMIND=<name>] [RECORD=1 NAME=<name> [MACRO=<gcode>] [PARROT=1] "
        "[PRINT=<text>]]"
    )

    def cmd_KNOCK(self, gcmd):
        did = False
        enabled = gcmd.get_int("ENABLED", None)
        if enabled is not None:
            self.enabled = bool(enabled)
            self.calibration = self.recording = None
            self._save_state()
            self._say("enabled" if self.enabled else "disabled")
            self._wake_up()
            did = True
        remind = gcmd.get("REMIND", None)
        if remind is not None:
            self._remind(gcmd, remind)
            did = True
        delete = gcmd.get("DELETE", None)
        if delete is not None:
            if self.bindings.pop(delete, None) is None:
                raise gcmd.error("knock: no pattern named '%s'" % (delete,))
            self._save_state()
            self._say("'%s' deleted" % (delete,))
            did = True
        if gcmd.get_int("CALIBRATE", 0):
            self._require_enabled(gcmd)
            self.recording = None
            self.calibration = ("quick", [])
            self._say(">>> knock 5 times, quickly, then wait <<<")
            did = True
        if gcmd.get_int("RECORD", 0):
            self._require_enabled(gcmd)
            self._start_recording(gcmd)
            did = True
        if gcmd.get_int("LIST", 0) or not did:
            self._list(gcmd)

    def _describe(self, b):
        parts = []
        if b.get("macro"):
            parts.append(b["macro"])
        if b.get("parrot"):
            parts.append("parrot")
        if b.get("print"):
            parts.append('print "%s"' % (b["print"],))
        return ", ".join(parts)

    def _list(self, gcmd):
        lines = ["knock: %s" % ("enabled" if self.enabled else "disabled",)]
        for name, b in sorted(self.bindings.items()):
            lines.append("  %s: %s" % (name, self._describe(b)))
        for name, _, _, _, _ in self.patterns:
            what = self.pattern_gcode.get(name) or "config pattern"
            lines.append("  %s: %s (built-in)" % (name, what))
        if not self.bindings and not self.patterns:
            lines.append("  no patterns yet - KNOCK RECORD=1 NAME=<name> ...")
        gcmd.respond_info("\n".join(lines))

    def _remind(self, gcmd, name):
        b = self.bindings.get(name)
        if b is not None:
            gaps = self._binding_gaps(b)
        else:
            pattern = next((p for p in self.patterns if p[0] == name), None)
            if pattern is None:
                raise gcmd.error("knock: no pattern named '%s'" % (name,))
            _, count, rhythm, _, _ = pattern
            if rhythm:
                gaps = [REMIND_SHORT_GAP * SYMBOL_WEIGHT[c] for c in rhythm]
            else:
                gaps = [REMIND_SHORT_GAP] * (count - 1)
        if not self._is_idle(self.reactor.monotonic()):
            raise gcmd.error("knock: printer is busy")
        self._parrot(gaps, self.gcode.run_script_from_command)

    def _require_enabled(self, gcmd):
        if not self.enabled:
            raise gcmd.error("knock: disabled - KNOCK ENABLED=1 to turn on")

    def _start_recording(self, gcmd):
        name = gcmd.get("NAME", None)
        if not name:
            raise gcmd.error("knock: RECORD needs NAME=<name>")
        macro = gcmd.get("MACRO", None)
        parrot = bool(gcmd.get_int("PARROT", 0))
        text = gcmd.get("PRINT", None)
        if not (macro or parrot or text):
            raise gcmd.error("knock: RECORD needs MACRO=, PARROT=1 or PRINT=")
        if (
            macro
            and macro.split()[0].upper() not in self.gcode.ready_gcode_handlers
        ):
            raise gcmd.error(
                "knock: unknown command or macro '%s'" % (macro.split()[0],)
            )
        if name in self.bindings:
            raise gcmd.error(
                "knock: '%s' already exists - KNOCK DELETE=%s first"
                % (name, name)
            )
        self.calibration = None
        self.recording = {
            "name": name,
            "macro": macro,
            "parrot": parrot,
            "print": text,
            "first": None,
        }
        self._say(">>> knock the pattern for '%s', then wait <<<" % (name,))

    def get_status(self, eventtime):
        return {
            "enabled": self.enabled,
            "last": self.last,
            "tempo": dict(self.tempo),
            "bindings": {k: dict(v) for k, v in self.bindings.items()},
            "mode": "calibrate"
            if self.calibration
            else "record"
            if self.recording
            else None,
        }


def load_config(config):
    return Knock(config)
