# Elegoo E400 Lite filament switcher — Klipper integration
#
# Communicates with the E400 Lite 4-channel AMS-style filament changer
# via Modbus RTU (over UART/USB-UART adapter) or USB CDC debug CLI.
#
# Protocol reference: ../PROTOCOL.md
#
# Copyright 2024 — released under GPL-v3 (same as Klipper)

import serial
import struct
import logging

# ── Modbus register map ──────────────────────────────────────────────

# Write registers (FC06)
REG_W_RFID_ENABLE    = 0x3000
REG_W_UNLOAD         = 0x3034
REG_W_LOAD           = 0x3035
REG_W_SELECT         = 0x3037
REG_W_EXTRUDER_SENS  = 0x304A
REG_W_PRE_LOAD_SPD   = 0x3080
REG_W_PRE_LOAD_DIST  = 0x3081
REG_W_LOAD_SPD       = 0x3082
REG_W_UNLOAD_SPD     = 0x3083
REG_W_LOAD_X_SPD     = 0x3084
REG_W_UNLOAD_X_SPD   = 0x3085
REG_W_LOAD_X_DIST    = 0x3086
REG_W_UNLOAD_X_DIST  = 0x3087

# Read registers (FC03)
REG_R_CHAN_STATE      = 0x3034  # active channel + channel state
REG_R_ACTIVE_CH      = 0x3037
REG_R_FILAMENT_DET   = 0x304B  # per-channel filament presence bitmask
REG_R_SW_VERSION     = 0x3190  # (major<<8)|(minor<<4)|patch
REG_R_HW_VERSION     = 0x3191
REG_R_SERIAL         = 0x3192


# ── Modbus CRC-16 ────────────────────────────────────────────────────

def _crc16(data):
    """Standard Modbus CRC-16 (poly 0xA001, init 0xFFFF)."""
    crc = 0xFFFF
    for b in data:
        crc ^= b
        for _ in range(8):
            if crc & 1:
                crc = (crc >> 1) ^ 0xA001
            else:
                crc >>= 1
    return crc & 0xFFFF


# ── Exception ─────────────────────────────────────────────────────────

class E400Error(Exception):
    pass


# ── Main class ────────────────────────────────────────────────────────

class ElegooE400:
    def __init__(self, config):
        self.printer = config.get_printer()
        self.reactor = self.printer.get_reactor()
        self.gcode = self.printer.lookup_object('gcode')

        # Serial / transport
        self.serial_port = config.get('serial')
        self.baud = config.getint('baud', 115200)
        self.mode = config.get('mode', 'modbus')
        self.slave_addr = config.getint('slave_address', 2)
        self.serial_timeout = config.getfloat('serial_timeout', 0.5)
        self.retries = config.getint('retries', 2)

        # Operation timing
        self.load_timeout = config.getfloat('load_timeout', 45.)
        self.unload_timeout = config.getfloat('unload_timeout', 45.)
        self.poll_interval = config.getfloat('poll_interval', 0.5)
        self.min_wait = config.getfloat('min_operation_wait', 3.)
        self.cli_wait = config.getfloat('cli_command_wait', 15.)

        if self.mode not in ('modbus', 'cli'):
            raise config.error(
                "elegoo_e400: 'mode' must be 'modbus' or 'cli'")

        self.ser = None
        self.current_tool = -1

        # Register GCode commands
        for name, handler, desc in [
            ('EL_CANVAS_CC_STATUS',      self.cmd_STATUS,
             "Report E400 Lite status"),
            ('EL_CANVAS_CC_VERSION',     self.cmd_VERSION,
             "Report E400 Lite firmware version"),
            ('EL_CANVAS_CC_LOAD',        self.cmd_LOAD,
             "Load filament: EL_CANVAS_CC_LOAD CHANNEL=<0-3>"),
            ('EL_CANVAS_CC_UNLOAD',      self.cmd_UNLOAD,
             "Unload filament: EL_CANVAS_CC_UNLOAD CHANNEL=<0-3>"),
            ('EL_CANVAS_CC_SELECT',      self.cmd_SELECT,
             "Set active channel: EL_CANVAS_CC_SELECT CHANNEL=<0-3>"),
            ('EL_CANVAS_CC_CHANGE_TOOL', self.cmd_CHANGE_TOOL,
             "Full tool change: EL_CANVAS_CC_CHANGE_TOOL CHANNEL=<0-3>"),
            ('EL_CANVAS_CC_SET_SPEEDS',  self.cmd_SET_SPEEDS,
             "Set E400 Lite speeds: LOAD=<mm/s> UNLOAD=<mm/s>"),
            ('EL_CANVAS_CC_CLEAR_ERROR', self.cmd_CLEAR_ERROR,
             "Attempt to clear E400 Lite error state"),
        ]:
            self.gcode.register_command(name, handler, desc=desc)

        self.printer.register_event_handler(
            'klippy:connect', self._handle_connect)
        self.printer.register_event_handler(
            'klippy:disconnect', self._handle_disconnect)

    # ── Klipper object interface ──────────────────────────────────────

    def get_status(self, eventtime):
        return {
            'current_tool': self.current_tool,
            'mode': self.mode,
        }

    # ── Lifecycle ─────────────────────────────────────────────────────

    def _handle_connect(self):
        self._open_serial()
        if self.mode == 'cli':
            self._cli_activate()
        else:
            self._modbus_probe()

    def _handle_disconnect(self):
        if self.ser:
            try:
                self.ser.close()
            except Exception:
                pass
            self.ser = None

    def _open_serial(self):
        try:
            self.ser = serial.Serial(
                port=self.serial_port,
                baudrate=self.baud,
                bytesize=serial.EIGHTBITS,
                parity=serial.PARITY_NONE,
                stopbits=serial.STOPBITS_ONE,
                timeout=self.serial_timeout,
            )
            logging.info("E400: opened %s @ %d baud (%s mode)",
                         self.serial_port, self.baud, self.mode)
        except Exception as e:
            raise self.printer.config_error(
                "E400: cannot open serial port %s: %s"
                % (self.serial_port, e))

    def _modbus_probe(self):
        """Read version and active channel at startup."""
        try:
            ver = self._read_version()
            self.gcode.respond_info("E400 Lite firmware %s" % ver)
        except E400Error as e:
            logging.warning("E400: version query failed: %s", e)
        try:
            ch = self._read_active_channel()
            self.current_tool = ch
            self.gcode.respond_info("E400 active channel: %d" % ch)
        except E400Error as e:
            logging.warning("E400: channel query failed: %s", e)

    # ── Modbus RTU transport ──────────────────────────────────────────

    def _mb_build(self, fc, register, value):
        pdu = struct.pack('>BBHH', self.slave_addr, fc, register, value)
        return pdu + struct.pack('<H', _crc16(pdu))

    def _mb_transact(self, tx, rx_len):
        if not self.ser or not self.ser.is_open:
            raise E400Error("serial port not open")
        last_err = None
        for attempt in range(1 + self.retries):
            try:
                self.ser.reset_input_buffer()
                self.ser.write(tx)
                self.ser.flush()
                rx = self.ser.read(rx_len)
                if len(rx) < 5:
                    last_err = E400Error(
                        "short response (%d/%d bytes)"
                        % (len(rx), rx_len))
                    continue
                # CRC
                if _crc16(rx[:-2]) != struct.unpack('<H', rx[-2:])[0]:
                    last_err = E400Error("CRC mismatch")
                    continue
                # Slave address
                if rx[0] != self.slave_addr:
                    raise E400Error("wrong slave addr %d" % rx[0])
                # Modbus exception response
                if rx[1] & 0x80:
                    raise E400Error(
                        "Modbus exception 0x%02X" % rx[2])
                return rx
            except (OSError, serial.SerialException) as e:
                last_err = E400Error("serial I/O: %s" % e)
        raise last_err

    def _write_reg(self, register, value):
        """FC06 — write single holding register."""
        tx = self._mb_build(0x06, register, value & 0xFFFF)
        self._mb_transact(tx, 8)

    def _read_regs(self, register, count=1):
        """FC03 — read holding registers. Returns list of uint16."""
        tx = self._mb_build(0x03, register, count)
        rx = self._mb_transact(tx, 5 + 2 * count)
        data = rx[3:-2]
        return [struct.unpack('>H', data[i:i+2])[0]
                for i in range(0, len(data), 2)]

    # ── CLI transport ─────────────────────────────────────────────────

    def _cli_activate(self):
        """Send ENTER x2 to activate the E400 debug CLI."""
        if not self.ser:
            return
        try:
            self.ser.write(b'\r\n\r\n')
            self.ser.flush()
            self.reactor.pause(self.reactor.monotonic() + 0.5)
            self.ser.reset_input_buffer()
            logging.info("E400: CLI activation sequence sent")
        except Exception as e:
            logging.warning("E400: CLI activation failed: %s", e)

    def _cli_send(self, command):
        if not self.ser:
            raise E400Error("serial port not open")
        self.ser.reset_input_buffer()
        self.ser.write(('%s\r\n' % command).encode('ascii'))
        self.ser.flush()

    # ── High-level reads (Modbus only) ────────────────────────────────

    def _read_version(self):
        v = self._read_regs(REG_R_SW_VERSION)[0]
        return "%d.%d.%d" % ((v >> 8) & 0xFF, (v >> 4) & 0x0F, v & 0x0F)

    def _read_active_channel(self):
        return self._read_regs(REG_R_ACTIVE_CH)[0] & 0x03

    def _read_channel_state(self):
        """Returns (channel, state) from the combined register."""
        raw = self._read_regs(REG_R_CHAN_STATE)[0]
        return (raw >> 8) & 0xFF, raw & 0xFF

    def _read_filament_bitmask(self):
        return self._read_regs(REG_R_FILAMENT_DET)[0]

    # ── Wait for operation completion ─────────────────────────────────

    def _wait_op(self, timeout, label="operation"):
        if self.mode == 'cli':
            self.reactor.pause(
                self.reactor.monotonic() + self.cli_wait)
            return
        # Modbus: let the E400 start the operation, then poll for
        # state stability (3 consecutive identical reads).
        deadline = self.reactor.monotonic() + timeout
        self.reactor.pause(
            self.reactor.monotonic() + self.min_wait)
        prev = None
        stable = 0
        while self.reactor.monotonic() < deadline:
            try:
                _, state = self._read_channel_state()
                if state == prev:
                    stable += 1
                    if stable >= 3:
                        logging.info(
                            "E400: %s done (state 0x%02X)", label, state)
                        return
                else:
                    stable = 0
                prev = state
            except E400Error as e:
                logging.debug("E400: poll error in %s: %s", label, e)
                stable = 0
            self.reactor.pause(
                self.reactor.monotonic() + self.poll_interval)
        raise E400Error("%s timed out (%.0fs)" % (label, timeout))

    # ── Operations ────────────────────────────────────────────────────

    def _do_load(self, channel):
        if self.mode == 'modbus':
            self._write_reg(REG_W_LOAD, channel)
        else:
            self._cli_send("feed_cmd %d" % channel)
        self._wait_op(self.load_timeout, "load ch%d" % channel)

    def _do_unload(self, channel):
        if self.mode == 'modbus':
            self._write_reg(REG_W_UNLOAD, channel)
        else:
            self._cli_send("unload_cmd %d" % channel)
        self._wait_op(self.unload_timeout, "unload ch%d" % channel)

    def _do_select(self, channel):
        if self.mode == 'modbus':
            self._write_reg(REG_W_SELECT, channel)
        self.current_tool = channel

    # ── GCode command handlers ────────────────────────────────────────

    def cmd_STATUS(self, gcmd):
        if self.mode == 'modbus':
            try:
                ch, state = self._read_channel_state()
                fdet = self._read_filament_bitmask()
                gcmd.respond_info(
                    "E400: channel=%d state=0x%02X filament=0x%02X "
                    "current_tool=%d" % (ch, state, fdet, self.current_tool))
                for i in range(4):
                    detected = bool(fdet & (1 << i))
                    gcmd.respond_info("  ch%d: %s" % (
                        i, "filament detected" if detected else "empty"))
            except E400Error as e:
                gcmd.respond_info("E400 status error: %s" % e)
        else:
            gcmd.respond_info(
                "E400 (CLI mode): current_tool=%d" % self.current_tool)

    def cmd_VERSION(self, gcmd):
        if self.mode != 'modbus':
            gcmd.respond_info("E400: version query requires modbus mode")
            return
        try:
            ver = self._read_version()
            gcmd.respond_info("E400 Lite firmware: %s" % ver)
        except E400Error as e:
            gcmd.respond_info("E400 version error: %s" % e)

    def cmd_LOAD(self, gcmd):
        ch = gcmd.get_int('CHANNEL', minval=0, maxval=3)
        gcmd.respond_info("E400: loading channel %d ..." % ch)
        try:
            self._do_load(ch)
        except E400Error as e:
            raise self.gcode.error("E400: load failed: %s" % e)
        self.current_tool = ch
        gcmd.respond_info("E400: channel %d loaded" % ch)

    def cmd_UNLOAD(self, gcmd):
        ch = gcmd.get_int('CHANNEL', minval=0, maxval=3)
        gcmd.respond_info("E400: unloading channel %d ..." % ch)
        try:
            self._do_unload(ch)
        except E400Error as e:
            raise self.gcode.error("E400: unload failed: %s" % e)
        gcmd.respond_info("E400: channel %d unloaded" % ch)

    def cmd_SELECT(self, gcmd):
        ch = gcmd.get_int('CHANNEL', minval=0, maxval=3)
        try:
            self._do_select(ch)
        except E400Error as e:
            raise self.gcode.error("E400: select failed: %s" % e)
        gcmd.respond_info("E400: active channel set to %d" % ch)

    def cmd_CHANGE_TOOL(self, gcmd):
        ch = gcmd.get_int('CHANNEL', minval=0, maxval=3)
        if self.current_tool == ch:
            gcmd.respond_info("E400: already on channel %d" % ch)
            return
        old = self.current_tool
        gcmd.respond_info("E400: tool change %d -> %d" % (old, ch))
        try:
            if old >= 0:
                self._do_unload(old)
            self._do_select(ch)
            self._do_load(ch)
        except E400Error as e:
            raise self.gcode.error("E400: tool change failed: %s" % e)
        gcmd.respond_info("E400: now on channel %d" % ch)

    def cmd_SET_SPEEDS(self, gcmd):
        if self.mode != 'modbus':
            raise self.gcode.error(
                "E400: SET_SPEEDS requires modbus mode")
        load_spd = gcmd.get_int('LOAD', None, minval=1, maxval=200)
        unload_spd = gcmd.get_int('UNLOAD', None, minval=1, maxval=200)
        try:
            if load_spd is not None:
                self._write_reg(REG_W_LOAD_SPD, load_spd)
                gcmd.respond_info(
                    "E400: load speed -> %d mm/s" % load_spd)
            if unload_spd is not None:
                self._write_reg(REG_W_UNLOAD_SPD, unload_spd)
                gcmd.respond_info(
                    "E400: unload speed -> %d mm/s" % unload_spd)
        except E400Error as e:
            raise self.gcode.error("E400: set speeds failed: %s" % e)
        if load_spd is None and unload_spd is None:
            gcmd.respond_info("Usage: EL_CANVAS_CC_SET_SPEEDS LOAD=N UNLOAD=N")

    def cmd_CLEAR_ERROR(self, gcmd):
        if self.mode != 'modbus':
            gcmd.respond_info(
                "E400: CLEAR_ERROR not available in CLI mode")
            return
        try:
            # Re-select the current channel — this resets the command
            # register and clears transient error states.
            if self.current_tool >= 0:
                self._write_reg(REG_W_SELECT, self.current_tool)
            gcmd.respond_info("E400: error clear sent")
        except E400Error as e:
            raise self.gcode.error(
                "E400: clear error failed: %s" % e)


def load_config(config):
    return ElegooE400(config)
