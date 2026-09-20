#!/usr/bin/env python3
"""Wrap a plain SWU into the Elegoo CC2 OTA bundle (.zip.sig).

Format (reverse engineered from stock cc2 01.xx/02.xx packages, see
cc-fw-tools-cc2 TOOLS/cc2_swu_decrypt.py and TOOLS/forge.py):

  <name>.zip.sig          ELEG header (type 4, unencrypted) + zip
  zip: ota-package-list.json.sig   ELEG header (type 3, AES-256-CBC) + ciphertext
       <name>.swu.sig              ELEG header (type 0, AES-256-CBC) + ciphertext

ELEG header is 512 bytes, little-endian:
  0x00 magic "ELEG"           0x08 plain filesize (u64)
  0x04 type | (0x80 if enc)   0x10 filename (48 bytes)
  0x05 version 1.2 (2 bytes)  0x90 encrypt_offset (u64) = 0
                              0x98 encrypt_length (u64) = enc size if enc else 0
                              0xA0 AES IV (16 bytes)      0xB0 encrypt_filesize (u64)
                              0xE0 sha256(payload as shipped) (32 bytes)
                              0x100 RSA-2048 PKCS1v15 signature over the payload (256 bytes)

Only stdlib + the openssl CLI are used so this runs in a stock Yocto task
with just python3-native and openssl-native.
"""

import hashlib
import io
import json
import os
import struct
import subprocess
import sys
import zipfile
from datetime import datetime

# Output is intentionally not reproducible: random IV + wall-clock timestamp per build.

MAGIC = b'ELEG'
HEADER_SIZE = 512


def run(args, data):
    r = subprocess.run(args, input=data, capture_output=True)
    if r.returncode != 0:
        raise RuntimeError(f"{args[0]} failed: {r.stderr.decode(errors='replace')}")
    return r.stdout


def sha256(data):
    return hashlib.sha256(data).digest()


def aes_cbc_encrypt(key_hex, iv, plain):
    # zero-pad to AES block size; always at least one block of padding so
    # encrypt_filesize > filesize (matches forge.py; consumers strip by length)
    pad_len = (-len(plain)) % 16 or 16
    padded = plain + b'\x00' * pad_len
    enc = run(['openssl', 'enc', '-aes-256-cbc', '-nopad', '-K', key_hex, '-iv', iv.hex()], padded)
    return enc, len(padded)


def rsa_sign(priv_key_path, payload):
    return run(['openssl', 'dgst', '-sha256', '-sign', priv_key_path], payload)


def build_header(*, package_type, encrypted, filename, filesize,
                 encrypt_filesize, iv, payload, priv_key_path):
    hdr = bytearray(HEADER_SIZE)
    hdr[0:4] = MAGIC
    hdr[0x04] = (package_type & 0x7F) | (0x80 if encrypted else 0)
    hdr[0x05] = 1  # version_major (stock packages all use 1.2)
    hdr[0x06] = 2  # version_minor
    struct.pack_into('<Q', hdr, 0x08, filesize)
    name = filename.encode('ascii')
    if len(name) >= 48:
        raise ValueError(f'filename too long: {filename}')
    hdr[0x10:0x10 + len(name)] = name
    struct.pack_into('<Q', hdr, 0x90, 0)  # encrypt_offset
    struct.pack_into('<Q', hdr, 0x98, encrypt_filesize if encrypted else 0)
    hdr[0xA0:0xB0] = iv
    struct.pack_into('<Q', hdr, 0xB0, encrypt_filesize)
    hdr[0xE0:0x100] = sha256(payload)
    sig = rsa_sign(priv_key_path, payload)
    if len(sig) != 256:
        raise RuntimeError(f'RSA signature is {len(sig)} bytes, expected 256')
    hdr[0x100:0x200] = sig
    return bytes(hdr)


def eleg_encrypt(pkg_type, filename, plain, key_hex, priv_key_path):
    iv = os.urandom(16)
    enc, enc_size = aes_cbc_encrypt(key_hex, iv, plain)
    return build_header(package_type=pkg_type, encrypted=True, filename=filename,
                        filesize=len(plain), encrypt_filesize=enc_size,
                        iv=iv, payload=enc, priv_key_path=priv_key_path) + enc


def main():
    if len(sys.argv) != 7:
        print(f'usage: {sys.argv[0]} <update.swu> <out.zip.sig> <fw-version XX.XX.XX.XX>'
              ' <update_class> <keydir> <name-prefix>')
        sys.exit(2)
    swu_path, out_path, fw_version, update_class, keydir, prefix = sys.argv[1:]

    key_hex = open(os.path.join(keydir, 'aes_key.key'), 'rb').read().hex()
    priv = os.path.join(keydir, 'private_key.pem')

    swu = open(swu_path, 'rb').read()
    stamp = datetime.now().strftime('%Y%m%d%H%M%S')
    base = f'{prefix}_{fw_version}_{stamp}'

    # inner: encrypted swu
    swu_sig = eleg_encrypt(0, f'{base}.swu', swu, key_hex, priv)

    # ota package list (daemon version gate: fw_version must be in its whitelist)
    pkglist = {'packages': [{'file': f'{base}.swu.sig', 'hash': hashlib.sha256(swu_sig).hexdigest()}],
               'version': fw_version, 'update_class': update_class}
    json_sig = eleg_encrypt(3, 'ota-package-list.json',
                            json.dumps(pkglist).encode('ascii'), key_hex, priv)

    # outer: unencrypted zip of the two .sig members
    zbio = io.BytesIO()
    with zipfile.ZipFile(zbio, 'w', zipfile.ZIP_DEFLATED) as zf:
        zf.writestr('ota-package-list.json.sig', json_sig)
        zf.writestr(f'{base}.swu.sig', swu_sig)
    blob = zbio.getvalue()

    outer = build_header(package_type=4, encrypted=False, filename=f'{base}.zip',
                         filesize=len(blob), encrypt_filesize=len(blob),
                         iv=b'\x00' * 16, payload=blob, priv_key_path=priv) + blob

    with open(out_path, 'wb') as f:
        f.write(outer)
    print(f'wrote {out_path} ({len(outer)} bytes), swu member {base}.swu.sig')


if __name__ == '__main__':
    main()
