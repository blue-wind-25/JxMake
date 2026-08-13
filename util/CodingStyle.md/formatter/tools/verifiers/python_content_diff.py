#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""
python_content_diff.py - content-preservation checker for Python, modeled on
toml_content_diff.py / xml_content_diff.py / yaml_content_diff.py.

Unlike those, Python has a real parser in its own stdlib (`ast`), so this
script uses it directly rather than shelling out to a Node.js helper.

Parses BOTH the original and formatted file with `ast.parse`, then compares
`ast.dump(tree, include_attributes=False)` (position/line/col attributes
stripped, since formatting legitimately changes those) for structural
equality. If formatting changed only whitespace/style, the two dumps must be
byte-identical -- any difference means a statement's nesting, a token's
value, or a node's shape changed, i.e. real corruption (most critically:
an indentation-based scoping change, since Python's indentation IS its
scoping, per STATE_PYTHON3.md).

Usage (two modes -- see also print_usage() below):
    Single pair:
        python3 python_content_diff.py <original.py> <formatted.py>
    Batch (one Python process invocation over a whole corpus, avoiding a
    process restart per file -- rel-path list, one path per line, relative
    to both base dirs alike):
        python3 python_content_diff.py <original_base_dir> <formatted_base_dir> <py_rel_path_file_list.txt>

Before each pair's AST diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>"
line is printed -- lets a hang/slow file in a large batch run be pinpointed,
matching js_ts_content_diff.js's/java_content_diff.java's/
kotlin_content_diff.java's own precedent. In batch mode, a rel-path missing
from either base dir (or both) is a warning, not a crash -- the file is
skipped and the run continues; the final SUMMARY line and process exit code
still reflect it. A parse failure for one pair in batch mode is also caught
and counted as a MISMATCH/ERROR rather than aborting the whole batch.

Exit 0 if the two ASTs are structurally identical (content preserved, all
pairs in batch mode), 1 if they differ (prints a highlighted description of
the first mismatch) or if any pair is missing/errors in batch mode, 2 if
either file fails to parse as Python at all in single-pair mode, or on a
usage error.
"""
import ast
import datetime
import os
import sys


class ParseError(Exception):
    pass


def parse_file(path):
    with open(path, "r", encoding="utf-8") as f:
        source = f.read()
    try:
        return ast.parse(source, filename=path)
    except SyntaxError as e:
        raise ParseError(str(e))


def dump(tree):
    # Include_attributes=False strips lineno/col_offset/end_lineno/end_col_offset --
    # exactly the fields formatting is expected to change; every other field
    # (node type, field values, child ordering/nesting) must match exactly
    return ast.dump(tree, include_attributes=False, indent=2)


def timestamp_now():
    now = datetime.datetime.now()

    return now.strftime("%Y-%m-%d %H:%M:%S.") + "%03d" % (now.microsecond // 1000)


def print_timestamped_header(rel_path):
    print("[%s] %s" % (timestamp_now(), rel_path))


def compare_one(orig_path, fmt_path, orig_label, fmt_label):
    """The full single-pair AST-diff check, shared by both modes. Raises
    ParseError if either file fails to parse (callers decide how to handle
    that per-mode). Returns True if structurally identical, False on a
    mismatch (mismatch details printed to stderr either way)."""
    try:
        orig_tree = parse_file(orig_path)
    except ParseError as e:
        raise ParseError("original file failed to parse as Python: %s: %s" % (orig_path, e))

    try:
        fmt_tree = parse_file(fmt_path)
    except ParseError as e:
        raise ParseError("formatted file failed to parse as Python: %s: %s" % (fmt_path, e))

    orig_dump = dump(orig_tree)
    fmt_dump  = dump(fmt_tree)

    if orig_dump == fmt_dump:
        print("OK: AST structurally identical (%s == %s)" % (orig_label, fmt_label))

        return True

    sys.stderr.write("MISMATCH: AST structure differs between %s and %s\n" % (orig_label, fmt_label))
    orig_lines = orig_dump.splitlines()
    fmt_lines  = fmt_dump.splitlines()
    for i, (a, b) in enumerate(zip(orig_lines, fmt_lines)):
        if a != b:
            sys.stderr.write("first differing line (%d):\n" % (i + 1))
            sys.stderr.write("  original : %s\n" % a)
            sys.stderr.write("  formatted: %s\n" % b)
            break

    else:
        sys.stderr.write("(one AST dump is a strict prefix of the other -- "
                          "orig %d lines, formatted %d lines)\n" % (len(orig_lines), len(fmt_lines)))

    return False


def print_usage():
    sys.stderr.write("Usage: python_content_diff.sh <original.py> <formatted.py>\n")
    sys.stderr.write("       python_content_diff.sh <original_base_dir> <formatted_base_dir> <py_rel_path_file_list.txt>\n")


def run_single(orig_arg, fmt_arg):
    print_timestamped_header(orig_arg)

    orig_exists, fmt_exists = os.path.exists(orig_arg), os.path.exists(fmt_arg)
    if not orig_exists or not fmt_exists:
        if not orig_exists and not fmt_exists: sys.stderr.write("WARNING: both %s and %s are missing\n" % (orig_arg, fmt_arg))
        elif not orig_exists:                  sys.stderr.write("WARNING: %s is missing\n" % orig_arg)
        else:                                  sys.stderr.write("WARNING: %s is missing\n" % fmt_arg)
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
            else:                                          mismatch_count += 1
        except Exception as e:
            print("  ERROR: %s" % e)
            mismatch_count += 1

    print("")
    print("SUMMARY: %d OK, %d MISMATCH/ERROR, %d MISSING (of %d files checked)" % (
        ok_count, mismatch_count, missing_count, ok_count + mismatch_count + missing_count))

    if mismatch_count > 0 or missing_count > 0: sys.exit(1)


def main():
    args = sys.argv[1:]
    if len(args) == 2:   run_single(args[0], args[1])
    elif len(args) == 3: run_batch(args[0], args[1], args[2])
    else:
        print_usage()
        sys.exit(2)


if __name__ == "__main__": main()
