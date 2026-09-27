# Elegoo Canvas RFID module
#
# Copyright (C) 2026  James Turton <james.turton@gmx.com>
#
# This file may be distributed under the terms of the GNU GPLv3 license.
#
# The reader is an MCU-fronted MFRC522-class NFC-A module on I2C address 0x50,
# bit-banged from the Canvas MCU. It reads NTAG21x tags only. Tag contents are
# decoded with the OpenRFID tag processors (https://github.com/suchmememanyskill/OpenRFID),
# used as a library.
#
#   request: A0 . 00 . seq . CLASS . CMD . len16 . data . XOR(CLASS..data)
#   reply:   A0 . 00 . seq . CLASS . STATUS . len16 . data . check byte
#
# Unimplemented or not-yet-processed commands are answered with a copy of the
# request, so replies are read after a fixed delay and echoes are retried.

import importlib
import logging
import sys

from . import bus

FRAME_ADDR = 0xA0
CLS_MODULE, CLS_NTAG = 0x01, 0x03
CMD_MODULE_INFO, CMD_HOT_START, CMD_TAG_ID = 0x01, 0x03, 0x06
CMD_IDENTIFY_PAGE = 0x09
CMD_MULTI_PAGE_READ = 0x06
STATUS_OK, STATUS_NO_TAG = 0x00, 0x01
REPLY_OVERHEAD = 8  # 7 header bytes + XOR
# 11 pages = 52-byte reply, the most that fits in one i2c_read_response
MAX_READ_PAGES = 11
# Physical page count (incl. header and config pages) by CC size byte
PAGE_COUNTS_BY_CC_SIZE = {
    0x06: 16,  # MIFARE Ultralight
    0x12: 45,  # NTAG213
    0x3E: 135,  # NTAG215
    0x6D: 231,  # NTAG216
}
# NTAG213 incl. config pages, used when the capability container is unknown
DEFAULT_LAST_PAGE = 44
READ_RETRY_TIME = 5.0
ELEGOO_MAGIC = bytes((0x36, 0xEE, 0xEE, 0xEE))

# OpenRFID processors for tags this module can read (NTAG / Ultralight)
PROCESSORS = {
    "elegoo": ("tag.elegoo.processor", "ElegooTagProcessor"),
    "tigertag": ("tag.tigertag.processor", "TigerTagProcessor"),
    "openspool": ("tag.openspool.processor", "OpenspoolTagProcessor"),
    "opentag3d": ("tag.opentag3d.processor", "OpenTag3DTagProcessor"),
    "spoolease": ("tag.spoolease.processor", "SpooleaseTagProcessor"),
    "anycubic": ("tag.anycubic.processor", "AnycubicTagProcessor"),
}


def xor8(data):
    c = 0
    for b in data:
        c ^= b
    return c


def build_frame(cls, cmd, data=b"", seq=0):
    body = bytes((cls, cmd, len(data) >> 8, len(data) & 0xFF)) + bytes(data)
    return bytes((FRAME_ADDR, 0, seq)) + body + bytes((xor8(body),))


def parse_reply(frame, rx):
    """Returns (status, data), or None if the reply is an echo of the
    request (not processed yet), a stale reply to another command, or fails
    its checks"""
    if len(rx) < REPLY_OVERHEAD or rx[: len(frame)] == frame:
        return None
    n = (rx[5] << 8) | rx[6]
    if rx[0] != FRAME_ADDR or rx[3] != frame[3]:
        return None
    if len(rx) < n + REPLY_OVERHEAD:
        return None
    return rx[4], bytes(rx[7 : 7 + n])


def tag_last_page(head):
    # Capability container (page 3): E1 . version . NDEF area size / 8 . access.
    # The NDEF area can be smaller than the user memory (NTAG215/216), so map
    # the factory CC size to the chip's physical page count, as OpenRFID does
    cc = head[12:16]
    if len(cc) == 4 and cc[0] == 0xE1 and cc[2] in PAGE_COUNTS_BY_CC_SIZE:
        return PAGE_COUNTS_BY_CC_SIZE[cc[2]] - 1
    return DEFAULT_LAST_PAGE


class OpenRFID:
    """The OpenRFID tag processors, imported from an OpenRFID src tree"""

    def __init__(self, path, names):
        if path not in sys.path:
            sys.path.append(path)
        self.ScanResult = importlib.import_module(
            "reader.scan_result"
        ).ScanResult
        self.ultralight = importlib.import_module(
            "tag.tag_types"
        ).TagType.MifareUltralight
        self.processors = []
        for name in names:
            module, cls = PROCESSORS[name]
            proc = getattr(importlib.import_module(module), cls)
            self.processors.append(proc({"__name": name}))

    def identify(self, uid, data):
        """Returns the filament dict of the first processor that recognises
        the tag, or None"""
        scan = self.ScanResult(
            self.ultralight, bytes.fromhex(uid), b"\x00\x44", b"", b"\x00"
        )
        for proc in self.processors:
            try:
                filament = proc.process_tag(scan, data)
            except Exception as e:
                logging.info("canvas_rfid: %s processor: %s", proc.name, e)
                continue
            if filament is not None:
                result = filament.to_dict()
                result["color_hex"] = "%06X" % (result["rgb"],)
                return result
        return None


class CanvasRFID:
    def __init__(self, config):
        self.printer = config.get_printer()
        self.reactor = self.printer.get_reactor()
        self.name = config.get_name().split()[-1]
        self.i2c = bus.MCU_I2C_from_config(config, default_addr=0x50)
        variant = config.getchoice("variant", ["cc1", "cc2"], "cc1")
        self.use_seq = variant == "cc1"
        # This command filter all tags to a specific manufacturer which we don't
        # want to do
        self.init_module = config.getboolean("init_module", False)
        self.poll_interval = config.getfloat(
            "poll_interval", 1.0, minval=0.2
        )
        self.poll_while_printing = config.getboolean(
            "poll_while_printing", False
        )
        self.reply_delay = config.getfloat(
            "reply_delay", 0.05, minval=0.01, maxval=1.0
        )
        self.retries = config.getint("retries", 3, minval=0)
        self.max_pages = config.getint(
            "max_pages", 231, minval=MAX_READ_PAGES, maxval=256
        )
        names = config.getlist(
            "openrfid_processors", list(PROCESSORS), count=None
        )
        for n in names:
            if n not in PROCESSORS:
                raise config.error(
                    "canvas_rfid: unknown openrfid processor '%s'" % (n,)
                )
        path = config.get("openrfid_path", "/usr/share/openrfid")
        try:
            self.openrfid = OpenRFID(path, names)
        except Exception as e:
            raise config.error(
                "canvas_rfid: cannot load OpenRFID from %s: %s" % (path, e)
            )
        self.gcode_macro = self.printer.load_object(config, "gcode_macro")
        self.tag_gcode = None
        if config.get("tag_gcode", None) is not None:
            self.tag_gcode = self.gcode_macro.load_template(
                config, "tag_gcode"
            )
        self.seq = 0
        self.lock = self.reactor.mutex()
        self.initialized = False
        self.version = None
        self.print_stats = None
        self.failed_uid = None
        self.failed_until = 0.0
        self.uid = None
        # Result of the last tag read; kept after the tag is removed
        self.filament = None
        self.scan_id = 0
        self.scan_time = 0.0
        self.errors = 0
        self.poll_timer = self.reactor.register_timer(self._poll_event)
        self.printer.register_event_handler("klippy:ready", self._handle_ready)
        gcode = self.printer.lookup_object("gcode")
        gcode.register_mux_command(
            "QUERY_CANVAS_RFID",
            "RFID",
            self.name,
            self.cmd_QUERY_CANVAS_RFID,
            desc=self.cmd_QUERY_CANVAS_RFID_help,
        )
        gcode.register_mux_command(
            "CANVAS_RFID_DUMP",
            "RFID",
            self.name,
            self.cmd_CANVAS_RFID_DUMP,
            desc=self.cmd_CANVAS_RFID_DUMP_help,
        )

    def _handle_ready(self):
        self.print_stats = self.printer.lookup_object("print_stats", None)
        self.reactor.update_timer(self.poll_timer, self.reactor.NOW)

    # Low-level transaction
    def _transact(self, cls, cmd, data=b"", rxlen=24):
        seq = 0
        if self.use_seq:
            seq, self.seq = self.seq, (self.seq + 1) & 0xFF
        frame = build_frame(cls, cmd, data, seq)
        with self.lock:
            self.i2c.i2c_write(frame[1:])  # the MCU sends the address byte
            for _ in range(self.retries + 1):
                self.reactor.pause(self.reactor.monotonic() + self.reply_delay)
                params = self.i2c.i2c_read(b"", rxlen)
                reply = parse_reply(frame, bytearray(params["response"]))
                if reply is not None:
                    return reply
        self.errors += 1
        return None

    def _read_pages(self, start, end):
        data = bytearray()
        for first in range(start, end + 1, MAX_READ_PAGES):
            last = min(first + MAX_READ_PAGES - 1, end)
            n = last - first + 1
            reply = self._transact(
                CLS_NTAG,
                CMD_MULTI_PAGE_READ,
                bytes((first, last)),
                rxlen=4 * n + REPLY_OVERHEAD,
            )
            if reply is None or reply[0] != STATUS_OK or len(reply[1]) != 4 * n:
                return None
            data += reply[1]
        return bytes(data)

    def _read_tag(self):
        # Pages 0-10 hold the capability container, which gives the tag size
        head = self._read_pages(0, MAX_READ_PAGES - 1)
        if head is None:
            return None
        last = min(tag_last_page(head), self.max_pages - 1)
        if last < MAX_READ_PAGES:
            return head[: 4 * (last + 1)]
        rest = self._read_pages(MAX_READ_PAGES, last)
        if rest is None:
            return None
        return head + rest

    def _init_module(self):
        # What the stock cc1 firmware sends at boot: hot start, then only
        # report tags whose page 16 starts with the Elegoo magic
        self._transact(CLS_MODULE, CMD_HOT_START, rxlen=8)
        ident = b"\x00\x10" + ELEGOO_MAGIC
        self._transact(CLS_MODULE, CMD_IDENTIFY_PAGE, ident, rxlen=8)

    def _read_module_info(self):
        # "HW version SW version release date", e.g. "EF-A5-V1.0.072 V1.01 20250625"
        reply = self._transact(CLS_MODULE, CMD_MODULE_INFO, rxlen=40)
        if reply is None or reply[0] != STATUS_OK:
            logging.warning("canvas_rfid %s: no module info reply", self.name)
            return
        self.version = reply[1].decode("ascii", "replace").strip()
        logging.info("canvas_rfid %s: module %s", self.name, self.version)

    def _poll_event(self, eventtime):
        if (
            not self.poll_while_printing
            and self.print_stats is not None
            and self.print_stats.state == "printing"
        ):
            return eventtime + self.poll_interval
        if not self.initialized:
            self._read_module_info()
            if self.init_module:
                self._init_module()
            self.initialized = True
        reply = self._transact(CLS_MODULE, CMD_TAG_ID, rxlen=16)
        if reply is None:
            return eventtime + self.poll_interval
        status, data = reply
        # data = [uid length, uid...]
        if status != STATUS_OK or not data or data[0] != len(data) - 1:
            if self.uid is not None:
                self._respond("tag removed")
                self.uid = None
                self.printer.send_event(
                    "canvas_rfid:tag", self.name, None, None
                )
            return eventtime + self.poll_interval
        uid = data[1:].hex().upper()
        if uid == self.uid:
            return eventtime + self.poll_interval
        if uid == self.failed_uid and eventtime < self.failed_until:
            return eventtime + self.poll_interval
        pages = self._read_tag()
        if pages is None:
            # e.g. MIFARE Classic, or a tag at the edge of the field
            if uid != self.failed_uid:
                self._respond("tag %s not readable (not NTAG?)" % (uid,))
            self.failed_uid = uid
            self.failed_until = eventtime + READ_RETRY_TIME
            return eventtime + self.poll_interval
        self.uid = uid
        self.filament = self.openrfid.identify(uid, pages)
        if self.filament is None:
            self._respond("tag %s: unrecognised tag" % (uid,))
        else:
            self.scan_id += 1
            self.scan_time = eventtime
            self._respond("tag %s: %s" % (uid, self._describe()))
        self.printer.send_event(
            "canvas_rfid:tag", self.name, self.uid, self.filament
        )
        if self.tag_gcode is not None:
            self.reactor.register_callback(self._run_tag_gcode)
        return eventtime + self.poll_interval

    def _describe(self):
        f = self.filament
        return "%s %s #%s %d-%dC (%s)" % (
            f["manufacturer"],
            " ".join([f["type"]] + f["modifiers"]),
            f["color_hex"],
            f["hotend_min_temp_c"],
            f["hotend_max_temp_c"],
            f["source_processor"],
        )

    def _respond(self, msg):
        msg = "Canvas RFID %s: %s" % (self.name, msg)
        self.printer.lookup_object("gcode").respond_info(msg)

    def _run_tag_gcode(self, eventtime):
        context = self.gcode_macro.create_template_context(eventtime)
        context["uid"] = self.uid
        context["filament"] = dict(self.filament or {})
        context["scan_id"] = self.scan_id
        gcode = self.printer.lookup_object("gcode")
        try:
            gcode.run_script(self.tag_gcode.render(context))
        except Exception:
            logging.exception("canvas_rfid %s: tag_gcode error", self.name)

    def get_status(self, eventtime):
        return {
            "tag_present": self.uid is not None,
            "uid": self.uid,
            "filament": dict(self.filament or {}),
            "scan_id": self.scan_id,
            "scan_age": eventtime - self.scan_time if self.scan_id else 0.0,
            "errors": self.errors,
            "version": self.version,
        }

    cmd_QUERY_CANVAS_RFID_help = "Report the spool tag on a Canvas RFID reader"

    def cmd_QUERY_CANVAS_RFID(self, gcmd):
        if self.uid is None:
            lines = ["Canvas RFID %s: no tag" % (self.name,)]
        else:
            lines = ["Canvas RFID %s: tag %s" % (self.name, self.uid)]
        if self.filament is None:
            if self.uid is not None:
                lines.append("unrecognised tag")
        else:
            lines.append("last scan (#%d): %s" % (self.scan_id, self._describe()))
            lines.extend(
                "%s: %s" % kv
                for kv in self.filament.items()
                if kv[0] not in ("colors", "colors_rgba", "colors_rgba_hex")
            )
        gcmd.respond_info("\n".join(lines))

    cmd_CANVAS_RFID_DUMP_help = "Dump the pages of the tag on a Canvas reader"

    def cmd_CANVAS_RFID_DUMP(self, gcmd):
        start = gcmd.get_int("START", 0, minval=0, maxval=255)
        end = gcmd.get_int("END", 44, minval=start, maxval=255)
        pages = self._read_pages(start, end)
        if pages is None:
            raise gcmd.error("Canvas RFID %s: read failed" % (self.name,))
        gcmd.respond_info(
            "\n".join(
                "page %3d: %s" % (start + i // 4, pages[i : i + 4].hex(" "))
                for i in range(0, len(pages), 4)
            )
        )


def load_config_prefix(config):
    return CanvasRFID(config)
