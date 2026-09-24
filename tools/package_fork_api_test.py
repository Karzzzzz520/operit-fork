#!/usr/bin/env python3
"""Package examples/fork-api-test into com.operit.fork.api_test.toolpkg (ZIP_STORED)."""

import os
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "examples", "fork-api-test")
OUT = os.path.join(ROOT, "com.operit.fork.api_test.toolpkg")
FILES = ["manifest.json", "main.js"]


def main() -> None:
    missing = [name for name in FILES if not os.path.isfile(os.path.join(SRC, name))]
    if missing:
        raise SystemExit("missing package files: " + ", ".join(missing))
    with zipfile.ZipFile(OUT, "w", zipfile.ZIP_STORED) as archive:
        for name in FILES:
            archive.write(os.path.join(SRC, name), name)
    print("packaged", OUT)
    print("size", os.path.getsize(OUT))


if __name__ == "__main__":
    main()
