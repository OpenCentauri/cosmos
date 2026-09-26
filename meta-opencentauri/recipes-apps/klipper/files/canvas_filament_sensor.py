# Elegoo Centauri 2 filament sensor
#
# Copyright (C) 2026  James Turton <james.turton@gmx.com>
#
# This file may be distributed under the terms of the GNU GPLv3 license.
#
# The communicates with the host using the canvas port over RS485.
#
#   frame:   3D 80 LEN CRC8 | id cmd data... | CRC16_hi CRC16_lo
#   CRC8:    poly 0x39, init 0x66, over the 3 header bytes
#   CRC16:   CCITT poly 0x1021, init 0x913D, over everything before it
#   cmd 7E:  reply data = [switch level, encoder level, u32be edge count]

import logging
import os

import serial

from . import filament_motion_sensor, filament_switch_sensor

DEVICE_ID = 0x05
CMD_VERSION = 0x02
CMD_STATUS = 0x7E
RETRY_TIME = 5.0
EDGE_MASK = 0xFFFFFFFF


######################################################################
# Framing
######################################################################


def crc8(data, crc=0x66):
    for b in data:
        crc ^= b
        for _ in range(8):
            crc = ((crc << 1) ^ 0x39) if crc & 0x80 else (crc << 1)
            crc &= 0xFF
    return crc


def crc16(data, crc=0x913D):
    for b in data:
        crc ^= b << 8
        for _ in range(8):
            crc = ((crc << 1) ^ 0x1021) if crc & 0x8000 else (crc << 1)
            crc &= 0xFFFF
    return crc


def build_frame(payload):
    frame = bytearray((0x3D, 0x80, len(payload) + 6))
    frame.append(crc8(frame))
    frame.extend(payload)
    c = crc16(frame)
    frame.extend((c >> 8, c & 0xFF))
    return bytes(frame)


class FrameParser:
    """Byte-stream parser for short frames; resyncs on the 0x3D magic"""

    def __init__(self):
        self.buf = bytearray()
        self.errors = 0

    def feed(self, data):
        self.buf.extend(data)
        payloads = []
        buf = self.buf
        while True:
            i = buf.find(0x3D)
            if i < 0:
                del buf[:]
                break
            del buf[:i]
            if len(buf) < 4:
                break
            length = buf[2]
            if buf[1] != 0x80 or length < 8 or crc8(buf[:3]) != buf[3]:
                del buf[:1]
                continue
            if len(buf) < length:
                break
            frame = buf[:length]
            if crc16(frame[:-2]) != (frame[-2] << 8) | frame[-1]:
                self.errors += 1
                del buf[:1]
                continue
            payloads.append(bytes(frame[4:-2]))
            del buf[:length]
        return payloads


######################################################################
# Bus owner: [canvas_filament_sensor]
######################################################################


class CanvasFilamentSensor:
    def __init__(self, config):
        self.printer = config.get_printer()
        self.reactor = self.printer.get_reactor()
        self.serial_port = config.get("serial", "/dev/ttyS1")
        self.baud = config.getint("baud", 115200, minval=2400)
        self.device_id = config.getint(
            "device_id", DEVICE_ID, minval=0, maxval=255
        )
        self.poll_interval = config.getfloat(
            "poll_interval", 0.1, minval=0.02, maxval=5.0
        )
        self.max_missed = config.getint("max_missed", 10, minval=1)
        self.invert_switch = config.getboolean("invert_switch", False)
        self.ser = None
        self.fd_handle = None
        self.parser = FrameParser()
        self.clients = []
        self.connected = False
        self.awaiting_reply = False
        self.want_version = False
        self.missed = 0
        self.version = None
        self.present = None
        self.edges = None
        self.open_error = None
        self.poll_timer = self.reactor.register_timer(self._poll_event)
        self.printer.register_event_handler("klippy:ready", self._handle_ready)
        self.printer.register_event_handler(
            "klippy:disconnect", self._handle_disconnect
        )

    def register_client(self, callback):
        """callback(eventtime, present, edges); both None on disconnect"""
        self.clients.append(callback)

    def _handle_ready(self):
        self.reactor.update_timer(self.poll_timer, self.reactor.NOW)

    def _handle_disconnect(self):
        self.reactor.update_timer(self.poll_timer, self.reactor.NEVER)
        self._close()

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
            self.present = bool(data[0]) != self.invert_switch
            self.edges = int.from_bytes(data[2:6], "big")
            for cb in self.clients:
                cb(eventtime, self.present, self.edges)

    def _set_disconnected(self, eventtime):
        self.awaiting_reply = False
        self.missed = 0
        if not self.connected:
            return
        self.connected = False
        self.version = None
        logging.warning("canvas_filament_sensor: sensor lost")
        for cb in self.clients:
            cb(eventtime, None, None)

    def get_status(self, eventtime):
        return {
            "connected": self.connected,
            "version": self.version,
            "filament_present": self.present,
            "edges": self.edges,
            "crc_errors": self.parser.errors,
        }


def _register_alias(config, prefix, obj):
    # Expose as "filament_switch_sensor NAME" etc. so front-ends that look for
    # the stock sensor objects pick these up unchanged
    name = "%s %s" % (prefix, config.get_name().split()[-1])
    printer = config.get_printer()
    if printer.lookup_object(name, None) is None:
        printer.add_object(name, obj)


######################################################################
# [canvas_filament_switch_sensor NAME]
######################################################################


class CanvasSwitchSensor(filament_switch_sensor.SwitchSensor):
    # SwitchSensor with the button replaced by the sensor's switch level; all
    # other behaviour (runout_distance, check_on_print_start, ...) is shared
    def __init__(self, config):
        self.printer = config.get_printer()
        gcode_macro = self.printer.load_object(config, "gcode_macro")
        bus = self.printer.load_object(config, "canvas_filament_sensor")
        self.runout_on_disconnect = config.getboolean(
            "runout_on_disconnect", False
        )
        runout_distance = config.getfloat("runout_distance", 0.0, minval=0.0)
        self.check_on_print_start = config.getboolean(
            "check_on_print_start", False
        )
        self.reactor = self.printer.get_reactor()
        self.estimated_print_time = None
        self.runout_helper = filament_switch_sensor.RunoutHelper(
            config, self, runout_distance
        )
        if config.get("immediate_runout_gcode", None) is not None:
            self.runout_helper.immediate_runout_gcode = (
                gcode_macro.load_template(config, "immediate_runout_gcode", "")
            )
        self.get_status = self.runout_helper.get_status
        self.printer.register_event_handler("klippy:ready", self._handle_ready)
        self.printer.register_event_handler(
            "idle_timeout:printing", self._handle_printing
        )
        bus.register_client(self._sensor_update)
        _register_alias(config, "filament_switch_sensor", self)

    def _sensor_update(self, eventtime, present, edges):
        if present is None:
            if self.runout_on_disconnect:
                self.runout_helper.note_filament_present(eventtime, False)
            return
        self.runout_helper.note_filament_present(eventtime, present)


######################################################################
# [canvas_filament_motion_sensor NAME]
######################################################################


class CanvasMotionSensor(filament_motion_sensor.EncoderSensor):
    # EncoderSensor fed from the sensor's edge counter instead of a pin. The
    # counter has no direction, so any new edge counts as filament movement.
    def __init__(self, config):
        self.printer = config.get_printer()
        self.gcode = self.printer.lookup_object("gcode")
        bus = self.printer.load_object(config, "canvas_filament_sensor")
        self.extruder_name = config.get("extruder", "extruder")
        self.detection_length = config.getfloat(
            "detection_length", 15.0, above=0.0
        )
        self.mm_per_edge = config.getfloat("mm_per_edge", 3.7, above=0.0)
        self.reactor = self.printer.get_reactor()
        self.runout_helper = filament_switch_sensor.RunoutHelper(config, self)
        self.get_status = self.runout_helper.get_status
        self.extruder = None
        self.estimated_print_time = None
        self.filament_runout_pos = None
        self.last_edges = None
        self.total_edges = 0
        self.printer.register_event_handler("klippy:ready", self._handle_ready)
        self.printer.register_event_handler(
            "idle_timeout:printing", self._handle_printing
        )
        self.printer.register_event_handler(
            "idle_timeout:ready", self._handle_not_printing
        )
        self.printer.register_event_handler(
            "idle_timeout:idle", self._handle_not_printing
        )
        bus.register_client(self._sensor_update)
        _register_alias(config, "filament_motion_sensor", self)

    def _sensor_update(self, eventtime, present, edges):
        # On disconnect no edges arrive, so a print runs out after
        # detection_length, as with a broken encoder wire
        if edges is None:
            self.last_edges = None
            return
        last, self.last_edges = self.last_edges, edges
        if not present:
            # Once the tail has passed the sensor there is nothing left to
            # move the encoder; leave runout to the switch sensor (and its
            # runout_distance) instead of stalling here
            if self.extruder is not None:
                self._update_filament_runout_pos(eventtime)
            return
        if last is None:
            return
        delta = (edges - last) & EDGE_MASK
        if not delta or delta > EDGE_MASK // 2:
            # no movement, or the sensor restarted and its count went back
            return
        self.total_edges += delta
        self.encoder_event(eventtime, 1)

    def sensor_get_status(self, eventtime):
        status = super().sensor_get_status(eventtime)
        status["mm_per_edge"] = float(self.mm_per_edge)
        status["filament_moved"] = float(self.total_edges * self.mm_per_edge)
        return status


def load_config(config):
    return CanvasFilamentSensor(config)
