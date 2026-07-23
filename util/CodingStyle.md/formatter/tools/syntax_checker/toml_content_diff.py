#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

# This script shells out to Node.js (see parse_toml_via_node below), so run it
# with the following env vars set (Node.js + its packages are installed under
# these non-default locations, not the system paths):
#
#   export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
#   export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
#   export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH

"""
toml_content_diff.py - content-preservation checker for TOML, modeled on
css_content_diff.py / xml_content_diff.py.

Python 3.6 on this system has no stdlib `tomllib` (3.11+ only) and no `toml`/
`tomli` package installed, so this script shells out to a tiny Node.js helper
that uses the `smol-toml` package (already installed for toml_sc.js) to parse
each file to JSON, then compares the resulting Python data structures for
deep equality (dict/list/scalar), the same principle as tomllib-based
comparison would give.

Usage:
    python3 toml_content_diff.py <original.toml> <formatted.toml>

Requires the same LD_LIBRARY_PATH/NODE_PATH/PATH env as toml_sc.js (see
STATE_DATA_FORMATS.md's "Dogfood Output Validation" section).

Exit 0 if parsed data structures match (content preserved), 1 otherwise with
a description of the mismatch, 2 if either file fails to parse as TOML at all.
"""
import json
import subprocess
import sys
import os

NODE_HELPER = r"""
const { parse } = require('smol-toml');
const fs = require('fs');
const path = process.argv[1];
try {
    const text = fs.readFileSync(path, 'utf8');
    const data = parse(text);
    process.stdout.write(JSON.stringify(data));
} catch (e) {
    process.stderr.write(String(e && e.message ? e.message : e));
    process.exit(1);
}
"""


def parse_toml_via_node(path):
    proc = subprocess.run(
        ["node", "-e", NODE_HELPER, "--", path],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True
    )
    if proc.returncode != 0:
        return None, proc.stderr.strip()
    try:
        return json.loads(proc.stdout), None
    except json.JSONDecodeError as e:
        return None, "failed to decode JSON from node helper: %s" % e


def main():
    if len(sys.argv) != 3:
        sys.stderr.write("usage: toml_content_diff.py <original.toml> <formatted.toml>\n")
        sys.exit(2)

    orig_path, fmt_path = sys.argv[1], sys.argv[2]

    orig_data, orig_err = parse_toml_via_node(orig_path)
    if orig_err is not None:
        sys.stderr.write("ERROR: original file failed to parse as TOML: %s: %s\n" % (orig_path, orig_err))
        sys.exit(2)

    fmt_data, fmt_err = parse_toml_via_node(fmt_path)
    if fmt_err is not None:
        sys.stderr.write("ERROR: formatted file failed to parse as TOML: %s: %s\n" % (fmt_path, fmt_err))
        sys.exit(2)

    if orig_data == fmt_data:
        print("OK: content preserved (%s == %s)" % (orig_path, fmt_path))
        sys.exit(0)
    else:
        sys.stderr.write("MISMATCH: parsed TOML data structures differ between %s and %s\n" % (orig_path, fmt_path))
        sys.stderr.write("--- original (parsed) ---\n")
        sys.stderr.write(json.dumps(orig_data, indent=2, sort_keys=True) + "\n")
        sys.stderr.write("--- formatted (parsed) ---\n")
        sys.stderr.write(json.dumps(fmt_data, indent=2, sort_keys=True) + "\n")
        sys.exit(1)


if __name__ == "__main__":
    main()
