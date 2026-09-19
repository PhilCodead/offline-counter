"""Restore an already signed public APK from its CI artifact and public byte delta.

The transport contains APK bytes only, never a signing key or password.
Both input and output SHA-256 hashes are checked before publishing.
"""

import base64
import hashlib
import json
from pathlib import Path
import sys
import zlib


def restore(source_path: Path, transport_path: Path, output_path: Path) -> None:
    source = source_path.read_bytes()
    transport = json.loads(zlib.decompress(base64.b64decode(transport_path.read_text())))
    if hashlib.sha256(source).hexdigest() != transport["source_sha256"]:
        raise ValueError("CI artifact SHA-256 mismatch")
    output = bytearray()
    for operation in transport["operations"]:
        if "data" in operation:
            output.extend(base64.b64decode(operation["data"], validate=True))
        else:
            offset, length = operation["offset"], operation["length"]
            if offset < 0 or length < 0 or offset + length > len(source):
                raise ValueError("Invalid source range")
            output.extend(source[offset:offset + length])
    if hashlib.sha256(output).hexdigest() != transport["apk_sha256"]:
        raise ValueError("Signed APK SHA-256 mismatch")
    with output_path.open("xb") as destination:
        destination.write(output)


if __name__ == "__main__":
    restore(*(Path(argument) for argument in sys.argv[1:]))
