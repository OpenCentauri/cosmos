#!/usr/bin/env python3
"""Identify stock encoder/CANVAS hardware before installing MCU firmware."""

import sys

import serial


def crc8(data):
    crc = 0x66
    for byte in data:
        crc ^= byte
        for _ in range(8):
            crc = ((crc << 1) ^ (0x39 if crc & 0x80 else 0)) & 0xFF
    return crc


def crc16(data):
    crc = 0x913D
    for byte in data:
        crc ^= byte << 8
        for _ in range(8):
            crc = ((crc << 1) ^ (0x1021 if crc & 0x8000 else 0)) & 0xFFFF
    return crc


def probe(port, request):
    request = bytes.fromhex(request)
    port.reset_input_buffer()
    port.write(request)
    data = port.read(256)
    for i in range(len(data) - 7):
        frame = data[i:i + data[i + 2]]
        if (len(frame) > 8 and frame[:2] == b"\x3d\x80"
                and len(frame) == frame[2] and frame[4:6] == request[4:6]
                and crc8(frame[:3]) == frame[3]
                and crc16(frame[:-2]) == int.from_bytes(frame[-2:], "big")):
            return True
    return False


def detect(serialport):
    with serial.Serial(serialport, 115200, timeout=0.25, write_timeout=0.25) as port:
        if probe(port, "3D 80 08 92 05 7E 16 B9"):
            return "encoder"
        if probe(port, "3D 80 08 92 00 7F F9 6D"):
            return "canvas"
    return "no reply"


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("Usage: probe-canvas <serialport>")
    try:
        print(detect(sys.argv[1]))
    except (serial.SerialException, OSError) as error:
        # Detection is a best guess: leave CANVAS disabled if probing fails.
        print(f"CANVAS probe failed: {error}", file=sys.stderr)
        print("no reply")
