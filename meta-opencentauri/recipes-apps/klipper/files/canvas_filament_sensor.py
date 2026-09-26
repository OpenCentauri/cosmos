# Centauri Carbon 2 filament sensor as virtual input pins (emulated MCU)
#
# Copyright (C) 2026  James Turton <james.turton@gmx.com>
#
# This file may be distributed under the terms of the GNU GPLv3 license.
#
# Exposes the sensor as a pin chip, so the standard filament sensors (and
# anything else that takes a button pin) can use it. No Kalico patch needed:
# the chip answers the MCU calls that buttons.MCU_buttons makes and sends it
# "buttons_state" messages, as an MCU running the buttons code would. This
# relies on MCU_buttons internals and can break if buttons.py changes.
#
#   [canvas_filament_sensor]        # must come before the sections using its pins
#   serial: /dev/ttyS1
#   #baud: 115200
#   #chip_name: filament_sensor
#   #runout_on_disconnect: False      # drop the filament pin when the sensor is lost
#
#   [filament_switch_sensor filament_sensor]
#   switch_pin: filament_sensor:filament
#   runout_distance: 770
#
#   [filament_motion_sensor filament_motion]
#   switch_pin: filament_sensor:encoder
#   detection_length: 15
#   extruder: extruder
#   # Let the switch sensor handle a normal runout (the encoder stops once the
#   # tail has passed); only pause for a jam, i.e. filament present but not moving
#   pause_on_runout: False
#   runout_gcode:
#       {% if printer["filament_switch_sensor filament_sensor"].filament_detected %}
#           PAUSE
#       {% endif %}
#
# Pins: "filament" is the switch level (1 = filament present); "encoder" toggles
# once per poll in which the edge counter moved. Both accept "!" (invert); "^"
# (pull-up) is accepted and ignored.
#
# The sensor talks to the host on the canvas port over RS485:
#
#   frame:   3D 80 LEN CRC8 | id cmd data... | CRC16_hi CRC16_lo
#   CRC8:    poly 0x39, init 0x66, over the 3 header bytes
#   CRC16:   CCITT poly 0x1021, init 0x913D, over everything before it
#   cmd 7E:  reply data = [switch level, encoder level, u32be edge count]

import logging
import os
import serial

FRAME_START = 0x3D
FRAME_MARKER = 0x80
HEADER_LEN = 4  # start, marker, length, CRC8
FRAME_OVERHEAD = HEADER_LEN + 2  # + CRC16
MIN_FRAME_LEN = FRAME_OVERHEAD + 2  # + id, cmd
CRC8_POLY, CRC8_INIT = 0x39, 0x66
CRC16_POLY, CRC16_INIT = 0x1021, 0x913D
DEVICE_ID = 0x05
CMD_VERSION = 0x02
CMD_STATUS = 0x7E
RETRY_TIME = 5.0
EDGE_MASK = 0xFFFFFFFF
PINS = ("filament", "encoder")


######################################################################
# Framing
######################################################################


def crc8(data, crc=CRC8_INIT):
    for b in data:
        crc ^= b
        for _ in range(8):
            crc = ((crc << 1) ^ CRC8_POLY) if crc & 0x80 else (crc << 1)
            crc &= 0xFF
    return crc


def crc16(data, crc=CRC16_INIT):
    for b in data:
        crc ^= b << 8
        for _ in range(8):
            crc = ((crc << 1) ^ CRC16_POLY) if crc & 0x8000 else (crc << 1)
            crc &= 0xFFFF
    return crc


def build_frame(payload):
    frame = bytearray((FRAME_START, FRAME_MARKER, len(payload) + FRAME_OVERHEAD))
    frame.append(crc8(frame))
    frame.extend(payload)
    c = crc16(frame)
    frame.extend((c >> 8, c & 0xFF))
    return bytes(frame)


class FrameParser:
    """Byte-stream parser for short frames; resyncs on FRAME_START"""

    def __init__(self):
        self.buf = bytearray()
        self.errors = 0

    def feed(self, data):
        self.buf.extend(data)
        payloads = []
        buf = self.buf
        while True:
            i = buf.find(FRAME_START)
            if i < 0:
                del buf[:]
                break
            del buf[:i]
            if len(buf) < HEADER_LEN:
                break
            length = buf[2]
            if (
                buf[1] != FRAME_MARKER
                or length < MIN_FRAME_LEN
                or crc8(buf[:3]) != buf[3]
            ):
                del buf[:1]
                continue
            if len(buf) < length:
                break
            frame = buf[:length]
            if crc16(frame[:-2]) != (frame[-2] << 8) | frame[-1]:
                self.errors += 1
                del buf[:1]
                continue
            payloads.append(bytes(frame[HEADER_LEN:-2]))
            del buf[:length]
        return payloads


######################################################################
# Sensor + pin chip: [canvas_filament_sensor]
######################################################################


class _Command:
    def __init__(self, handler):
        self.handler = handler

    def send(self, data=(), minclock=0, reqclock=0):
        self.handler(data)


class CanvasFilamentSensor:
    def __init__(self, config):
        self.printer = config.get_printer()
        self.reactor = self.printer.get_reactor()
        self.serial_port = config.get("serial")
        self.baud = config.getint("baud", 115200, minval=2400)
        self.device_id = config.getint(
            "device_id", DEVICE_ID, minval=0, maxval=255
        )
        self.poll_interval = config.getfloat(
            "poll_interval", 0.1, minval=0.02, maxval=5.0
        )
        self.max_missed = config.getint("max_missed", 10, minval=1)
        self.chip_name = config.get("chip_name", "filament_sensor")
        self.runout_on_disconnect = config.getboolean(
            "runout_on_disconnect", False
        )
        # Serial state
        self.ser = None
        self.fd_handle = None
        self.parser = FrameParser()
        self.connected = False
        self.awaiting_reply = False
        self.want_version = False
        self.missed = 0
        self.version = None
        self.present = None
        self.edges = None
        self.open_error = None
        # Pin state
        self.levels = dict.fromkeys(PINS, 0)
        self.last_edges = None
        self.next_oid = 0
        self.config_callbacks = []
        # oid -> {"pins": [...], "handler": cb, "ack": n, "last": state}
        self.groups = {}
        ppins = self.printer.lookup_object("pins")
        self.pin_error = ppins.error
        ppins.register_chip(self.chip_name, self)
        self.poll_timer = self.reactor.register_timer(self._poll_event)
        self.printer.register_event_handler("klippy:connect", self._connect)
        self.printer.register_event_handler("klippy:ready", self._handle_ready)
        self.printer.register_event_handler(
            "klippy:disconnect", self._handle_disconnect
        )

    def _connect(self):
        # A real MCU runs these while building its config
        for cb in self.config_callbacks:
            cb()

    def _handle_ready(self):
        self.reactor.update_timer(self.poll_timer, self.reactor.NOW)

    def _handle_disconnect(self):
        self.reactor.update_timer(self.poll_timer, self.reactor.NEVER)
        self._close()

    # Pin chip interface
    def setup_pin(self, pin_type, pin_params):
        raise self.pin_error(
            "%s pins can only be used as button inputs" % (self.chip_name,)
        )

    # The subset of the MCU interface used by buttons.MCU_buttons
    def register_config_callback(self, cb):
        self.config_callbacks.append(cb)

    def create_oid(self):
        oid = self.next_oid
        self.next_oid += 1
        # "last": None so the first update is always sent and MCU_buttons
        # sees the initial level of inverted pins
        self.groups[oid] = {"pins": [], "handler": None, "ack": 0, "last": None}
        return oid

    def add_config_cmd(self, cmd, is_init=False, on_restart=False):
        # Only "buttons_add oid=%d pos=%d pin=%s pull_up=%d" matters
        parts = cmd.split()
        if parts[0] != "buttons_add":
            return
        args = dict(p.split("=", 1) for p in parts[1:])
        pin = args["pin"]
        if pin not in PINS:
            raise self.pin_error(
                "unknown pin %s:%s (use %s)"
                % (self.chip_name, pin, ", ".join(PINS))
            )
        self.groups[int(args["oid"])]["pins"].append(pin)

    def alloc_command_queue(self):
        return None

    def lookup_command(self, msgformat, cq=None):
        # Only "buttons_ack oid=%c count=%c"
        return _Command(self._handle_ack)

    def get_query_slot(self, oid):
        return 0

    def seconds_to_clock(self, time):
        return 0

    def register_response(self, cb, msg, oid=None):
        self.groups[oid]["handler"] = cb

    def _handle_ack(self, data):
        oid, count = data
        self.groups[oid]["ack"] += count

    # Serial port handling
    def _open(self):
        try:
            self.ser = serial.Serial(
                self.serial_port,
                self.baud,
                timeout=0,
                write_timeout=0,
                exclusive=True,
            )
        except (OSError, serial.SerialException) as e:
            if str(e) != self.open_error:
                logging.warning(
                    "canvas_filament_sensor: unable to open %s: %s",
                    self.serial_port,
                    e,
                )
                self.open_error = str(e)
            self.ser = None
            return False
        self.open_error = None
        self.parser = FrameParser()
        self.fd_handle = self.reactor.register_fd(
            self.ser.fileno(), self._handle_read
        )
        logging.info("canvas_filament_sensor: opened %s", self.serial_port)
        return True

    def _close(self):
        if self.fd_handle is not None:
            self.reactor.unregister_fd(self.fd_handle)
            self.fd_handle = None
        if self.ser is not None:
            try:
                self.ser.close()
            except (OSError, serial.SerialException):
                pass
            self.ser = None

    def _io_error(self, eventtime, err):
        logging.warning(
            "canvas_filament_sensor: %s error: %s", self.serial_port, err
        )
        self._close()
        self._set_disconnected(eventtime)

    # Polling
    def _poll_event(self, eventtime):
        if self.ser is None and not self._open():
            return eventtime + RETRY_TIME
        if self.awaiting_reply:
            self.missed += 1
            if self.missed >= self.max_missed:
                self._set_disconnected(eventtime)
        cmd = CMD_STATUS
        if self.want_version:
            cmd = CMD_VERSION
            self.want_version = False
        try:
            self.ser.write(build_frame(bytes((self.device_id, cmd))))
        except (OSError, serial.SerialException) as e:
            self._io_error(eventtime, e)
            return eventtime + RETRY_TIME
        self.awaiting_reply = True
        return eventtime + self.poll_interval

    def _handle_read(self, eventtime):
        try:
            data = os.read(self.ser.fileno(), 4096)
        except OSError as e:
            self._io_error(eventtime, e)
            return
        for payload in self.parser.feed(data):
            self._process_payload(eventtime, payload)

    def _process_payload(self, eventtime, payload):
        if len(payload) < 2 or payload[0] != self.device_id:
            return
        cmd, data = payload[1], payload[2:]
        self.awaiting_reply = False
        self.missed = 0
        if cmd == CMD_VERSION:
            self.version = data.decode("ascii", "replace")
            logging.info("canvas_filament_sensor: version %s", self.version)
        elif cmd == CMD_STATUS and len(data) == 6:
            if not self.connected:
                self.connected = True
                self.want_version = True
                logging.info("canvas_filament_sensor: sensor connected")
            self.present = bool(data[0])
            self.edges = int.from_bytes(data[2:6], "big")
            self._update_pins(eventtime, self.present, self.edges)

    def _set_disconnected(self, eventtime):
        self.awaiting_reply = False
        self.missed = 0
        if not self.connected:
            return
        self.connected = False
        self.version = None
        logging.warning("canvas_filament_sensor: sensor lost")
        self._update_pins(eventtime, None, None)

    # Pin updates
    def _update_pins(self, eventtime, present, edges):
        if edges is None:
            # Disconnected: the pins keep their last level unless asked
            self.last_edges = None
            if self.runout_on_disconnect:
                self.levels["filament"] = 0
        else:
            self.levels["filament"] = int(present)
            if self.last_edges is not None:
                delta = (edges - self.last_edges) & EDGE_MASK
                # ignore a sensor restart (count went back)
                if delta and delta <= EDGE_MASK // 2:
                    self.levels["encoder"] ^= 1
            self.last_edges = edges
        for oid, group in self.groups.items():
            if group["handler"] is None:
                continue
            # Raw levels; MCU_buttons applies the invert mask itself
            state = 0
            for i, name in enumerate(group["pins"]):
                state |= self.levels[name] << i
            if state == group["last"]:
                continue
            group["last"] = state
            group["handler"](
                {
                    "oid": oid,
                    "ack_count": group["ack"] & 0xFF,
                    "state": bytes((state,)),
                    "#receive_time": eventtime,
                }
            )

    def get_status(self, eventtime):
        return {
            "connected": self.connected,
            "version": self.version,
            "filament_present": self.present,
            "edges": self.edges,
            "crc_errors": self.parser.errors,
            "pins": dict(self.levels),
        }


def load_config(config):
    return CanvasFilamentSensor(config)
