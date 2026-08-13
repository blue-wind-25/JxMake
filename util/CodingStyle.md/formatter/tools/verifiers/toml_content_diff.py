#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

# This script shells out to Node.js (see parse_toml_via_node below), so run it
# with the following env vars set (Node.js + its packages are installed under
# these non-default locations, not the system paths):
#
#     export LD_LIBRARY_PATH=/opt/gcc-7.5.0/lib64:/opt/gcc-7.5.0/lib:/opt/isl-0.16.1/lib
#     export NODE_PATH=/opt/node-v24.14.0-linux-x64/lib/node_modules:~/mynpm/node_modules
#     export PATH=/opt/node-v24.14.0-linux-x64/bin:~/mynpm/bin:$PATH

"""
toml_content_diff.py - content-preservation checker for TOML, modeled on
css_content_diff.py / xml_content_diff.py.

Python 3.6 on this system has no stdlib `tomllib` (3.11+ only) and no `toml`/
`tomli` package installed, so this script shells out to a tiny Node.js helper
that uses the `smol-toml` package (already installed for toml_syntax_check.js) to parse
each file to JSON, then compares the resulting Python data structures for
deep equality (dict/list/scalar), the same principle as tomllib-based
comparison would give.

Usage (two modes -- see also print_usage() below):
    Single pair:
        python3 toml_content_diff.py <original.toml> <formatted.toml>
    Batch (one Python process invocation over a whole corpus, avoiding a
    process restart per file -- rel-path list, one path per line, relative
    to both base dirs alike; note each pair still shells out to its own Node
    subprocess, same as single-pair mode always has -- batching only saves
    the Python/shell process restart, not the Node one):
        python3 toml_content_diff.py <original_base_dir> <formatted_base_dir> <toml_rel_path_file_list.txt>

Requires the same LD_LIBRARY_PATH/NODE_PATH/PATH env as toml_syntax_check.js (see
STATE_DATA_FORMATS.md's "Dogfood Output Validation" section).

Before each pair's diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>" line is
printed -- lets a hang/slow file in a large batch run be pinpointed, matching
js_ts_content_diff.js's/java_content_diff.java's/kotlin_content_diff.java's
own precedent. In batch mode, a rel-path missing from either base dir (or
both) is a warning, not a crash -- the file is skipped and the run continues;
the final SUMMARY line and process exit code still reflect it. A parse
failure for one pair in batch mode is also caught and counted as a
MISMATCH/ERROR rather than aborting the whole batch.

Exit 0 if parsed data structures match (content preserved, all pairs in batch
mode), 1 otherwise with a description of the mismatch (or if any pair is
missing/errors in batch mode), 2 if either file fails to parse as TOML at all
in single-pair mode, or on a usage error.
"""
import datetime
import json
import os
import subprocess
import sys

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

NODE = os.environ.get("NODE", "node")


class ParseError(Exception):
    pass


def parse_toml_via_node(path):
    proc = subprocess.run(
        [NODE, "-e", NODE_HELPER, "--", path],
        stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True
    )
    if proc.returncode != 0: raise ParseError(proc.stderr.strip())
    try:
        return json.loads(proc.stdout)
    except json.JSONDecodeError as e:
        raise ParseError("failed to decode JSON from node helper: %s" % e)


def timestamp_now():
    now = datetime.datetime.now()

    return now.strftime("%Y-%m-%d %H:%M:%S.") + "%03d" % (now.microsecond // 1000)


def print_timestamped_header(rel_path):
    print("[%s] %s" % (timestamp_now(), rel_path))


def compare_one(orig_path, fmt_path, orig_label, fmt_label):
    """The full single-pair check, shared by both modes. Raises ParseError if
    either file fails to parse. Returns True if the parsed data structures
    match, False on a mismatch (mismatch details printed to stderr)."""
    try:
        orig_data = parse_toml_via_node(orig_path)
    except ParseError as e:
        raise ParseError("original file failed to parse as TOML: %s: %s" % (orig_path, e))

    try:
        fmt_data = parse_toml_via_node(fmt_path)
    except ParseError as e:
        raise ParseError("formatted file failed to parse as TOML: %s: %s" % (fmt_path, e))

    if orig_data == fmt_data:
        print("OK: content preserved (%s == %s)" % (orig_label, fmt_label))

        return True

    sys.stderr.write("MISMATCH: parsed TOML data structures differ between %s and %s\n" % (orig_label, fmt_label))
    sys.stderr.write("--- original (parsed) ---\n")
    sys.stderr.write(json.dumps(orig_data, indent=2, sort_keys=True) + "\n")
    sys.stderr.write("--- formatted (parsed) ---\n")
    sys.stderr.write(json.dumps(fmt_data, indent=2, sort_keys=True) + "\n")

    return False


def print_usage():
    sys.stderr.write("Usage: toml_content_diff.sh <original.toml> <formatted.toml>\n")
    sys.stderr.write("       toml_content_diff.sh <original_base_dir> <formatted_base_dir> <toml_rel_path_file_list.txt>\n")


def run_single(orig_arg, fmt_arg):
    print_timestamped_header(orig_arg)

    orig_exists, fmt_exists = os.path.exists(orig_arg), os.path.exists(fmt_arg)
    if not orig_exists or not fmt_exists:
        if not orig_exists and not fmt_exists:
            sys.stderr.write("WARNING: both %s and %s are missing\n" % (orig_arg, fmt_arg))
        elif not orig_exists: sys.stderr.write("WARNING: %s is missing\n" % orig_arg)
        else: sys.stderr.write("WARNING: %s is missing\n" % fmt_arg)
        sys.exit(1)

    try:
        sys.exit(0 if compare_one(orig_arg, fmt_arg, orig_arg, fmt_arg) else 1)
    except ParseError as e:
        sys.stderr.write("ERROR: %s\n" % e)
        sys.exit(2)


def run_batch(orig_base_dir, fmt_base_dir, file_list_path):
    with open(file_list_path, "r", encoding="utf-8") as f:
        rel_paths = [line.strip() for line in f if line.strip()]

    ok_count = mismatch_count = missing_count = 0

    for rel in rel_paths:
        orig_path = os.path.join(orig_base_dir, rel)
        fmt_path  = os.path.join(fmt_base_dir, rel)

        print_timestamped_header(rel)

        orig_exists, fmt_exists = os.path.exists(orig_path), os.path.exists(fmt_path)
        if not orig_exists and not fmt_exists:
            print("  WARNING: missing from both %s and %s -- skipping" % (orig_base_dir, fmt_base_dir))
            missing_count += 1
            continue
        if not orig_exists:
            print("  WARNING: missing from %s -- skipping" % orig_base_dir)
            missing_count += 1
            continue
        if not fmt_exists:
            print("  WARNING: missing from %s -- skipping" % fmt_base_dir)
            missing_count += 1
            continue

        try:
            if compare_one(orig_path, fmt_path, rel, rel): ok_count += 1
            else: mismatch_count += 1
        except Exception as e:
            print("  ERROR: %s" % e)
            mismatch_count += 1

    print("")
    print("SUMMARY: %d OK, %d MISMATCH/ERROR, %d MISSING (of %d files checked)" % (
        ok_count, mismatch_count, missing_count, ok_count + mismatch_count + missing_count))

    if mismatch_count > 0 or missing_count > 0: sys.exit(1)


def main():
    args = sys.argv[1:]
    if len(args) == 2: run_single(args[0], args[1])
    elif len(args) == 3: run_batch(args[0], args[1], args[2])
    else:
        print_usage()
        sys.exit(2)


if __name__ == "__main__": main()
