# Wrap a plain SWU into the Elegoo CC2 OTA bundle (.zip.sig).
# 
#   <name>.zip.sig          ELEG header (type 4, unencrypted) + zip
#   zip: ota-package-list.json.sig   ELEG header (type 3, AES-256-CBC) + ciphertext
#        <name>.swu.sig              ELEG header (type 0, AES-256-CBC) + ciphertext
# 
# ELEG header is 512 bytes, little-endian:
#   0x00 magic "ELEG"           0x08 plain filesize (u64)
#   0x04 type | (0x80 if enc)   0x10 filename (48 bytes)
#   0x05 version 1.2 (2 bytes)  0x90 encrypt_offset (u64) = 0
#                               0x98 encrypt_length (u64) = enc size if enc else 0
#                               0xA0 AES IV (16 bytes)      0xB0 encrypt_filesize (u64)
#                               0xE0 sha256(payload as shipped) (32 bytes)
#                               0x100 RSA-2048 PKCS1v15 signature over the payload (256 bytes)

ELEGOO_OTA_FSTYPE  ??= "zip.sig"

ELEGOO_OTA_VERSION ??= "02.00.00.00"
ELEGOO_OTA_CLASS   ??= "00.00.00.00"
ELEGOO_OTA_PREFIX  ??= "cc2_eeb001"

ELEGOO_OTA_PRIVATE_KEY ??= "${SWUPDATE_PRIVATE_KEY}"
ELEGOO_OTA_AES_KEY ??= "d14e150843e9d16893890756d8f77f674e161a8bebb8f720737ee60e7f8c7e68"

ELEGOO_OTA_SWU     ??= "${IMAGE_LINK_NAME}.swu"
ELEGOO_OTA_IMAGE   ??= "${ELEGOO_OTA_PREFIX}_${ELEGOO_OTA_VERSION}.zip.sig"

ELEGOO_OTA_DEPLOY_DIR = "${WORKDIR}/deploy-${PN}-elegoo-ota"

SSTATETASKS += "do_elegoo_ota"
do_elegoo_ota[umask] = "022"
do_elegoo_ota[dirs] = "${ELEGOO_OTA_DEPLOY_DIR}"
do_elegoo_ota[cleandirs] += "${ELEGOO_OTA_DEPLOY_DIR}"
do_elegoo_ota[sstate-inputdirs] = "${ELEGOO_OTA_DEPLOY_DIR}"
do_elegoo_ota[sstate-outputdirs] = "${DEPLOY_DIR_IMAGE}"
do_elegoo_ota[stamp-extra-info] = "${MACHINE}"

python () {
    if d.getVar('ELEGOO_OTA_FSTYPE') not in (d.getVar('IMAGE_FSTYPES') or "").split():
        d.setVarFlag('do_elegoo_ota', 'noexec', '1')
        return

    d.appendVar('DEPENDS', ' openssl-native')

    d.appendVarFlag('do_elegoo_ota', 'file-checksums',
                    ' %s:True' % d.getVar('ELEGOO_OTA_PRIVATE_KEY'))
}

python do_elegoo_ota() {
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
            bb.fatal(f"elegoo-ota: {args[0]} failed: {r.stderr.decode(errors='replace')}")
        return r.stdout


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
            bb.fatal(f'elegoo-ota: filename too long: {filename}')
        hdr[0x10:0x10 + len(name)] = name
        struct.pack_into('<Q', hdr, 0x90, 0)  # encrypt_offset
        struct.pack_into('<Q', hdr, 0x98, encrypt_filesize if encrypted else 0)
        hdr[0xA0:0xB0] = iv
        struct.pack_into('<Q', hdr, 0xB0, encrypt_filesize)
        hdr[0xE0:0x100] = hashlib.sha256(payload).digest()
        sig = rsa_sign(priv_key_path, payload)
        if len(sig) != 256:
            bb.fatal(f'elegoo-ota: RSA signature is {len(sig)} bytes, expected 256')
        hdr[0x100:0x200] = sig
        return bytes(hdr)


    def eleg_encrypt(pkg_type, filename, plain, key_hex, priv_key_path):
        iv = os.urandom(16)
        enc, enc_size = aes_cbc_encrypt(key_hex, iv, plain)
        return build_header(package_type=pkg_type, encrypted=True, filename=filename,
                            filesize=len(plain), encrypt_filesize=enc_size,
                            iv=iv, payload=enc, priv_key_path=priv_key_path) + enc

    swu_path = os.path.join(d.getVar('DEPLOY_DIR_IMAGE'), d.getVar('ELEGOO_OTA_SWU'))
    if not os.path.exists(swu_path):
        bb.fatal("elegoo-ota: SWU not found: %s" % swu_path)
    out_path = os.path.join(d.getVar('ELEGOO_OTA_DEPLOY_DIR'), d.getVar('ELEGOO_OTA_IMAGE'))
    fw_version = d.getVar('ELEGOO_OTA_VERSION')
    update_class = d.getVar('ELEGOO_OTA_CLASS')
    prefix = d.getVar('ELEGOO_OTA_PREFIX')

    key_hex = d.getVar('ELEGOO_OTA_AES_KEY')
    priv = d.getVar('ELEGOO_OTA_PRIVATE_KEY')

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
    bb.note(f'elegoo-ota: wrote {out_path} ({len(outer)} bytes), swu member {base}.swu.sig')
}
addtask elegoo_ota after do_swuimage before do_build

python do_elegoo_ota_setscene() {
    sstate_setscene(d)
}
addtask do_elegoo_ota_setscene
