#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""
yaml_content_diff.py -- content-preservation checker for YAML dogfood testing.

Parses both an original and a formatted YAML file with PyYAML's
`yaml.safe_load_all` (multi-document stream aware, same reasoning as
`yaml_syntax_check.js`'s `loadAll()`) and compares the resulting Python data structures
(dict/list/scalar) document-by-document for equality. This proves keys,
values, and structure were not altered by formatting -- it does NOT catch
comment-only changes (comment normalization is a separate, lighter-weight
concern; see the accompanying text-based comment scan below, best-effort).

Usage (two modes -- see also print_usage() below):
    Single pair:
        python3 yaml_content_diff.py <original.yaml> <formatted.yaml>
    Batch (one Python process invocation over a whole corpus, avoiding a
    process restart per file -- rel-path list, one path per line, relative
    to both base dirs alike):
        python3 yaml_content_diff.py <original_base_dir> <formatted_base_dir> <yaml_rel_path_file_list.txt>

Before each pair's diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>" line is
printed -- lets a hang/slow file in a large batch run be pinpointed, matching
js_ts_content_diff.js's/java_content_diff.java's/kotlin_content_diff.java's
own precedent. In batch mode, a rel-path missing from either base dir (or
both) is a warning, not a crash -- the file is skipped and the run continues;
the final SUMMARY line and process exit code still reflect it. A parse
failure for one pair in batch mode is also caught and counted as a
MISMATCH/ERROR rather than aborting the whole batch.

Exit codes:
    0 - all documents' parsed structures match, for every pair checked
        (comment differences, if any, are printed as informational only,
        not a failure)
    1 - a structural/data mismatch was found (documents differ in count,
        or some document's parsed value differs) -- description printed
        (or if any pair is missing/errors in batch mode)
    2 - (single-pair mode only) one of the two files failed to parse as YAML
        at all -- not applicable to a real dogfood run where both files
        should already be syntax-checked separately; also used for a usage
        error
"""
import datetime
import os
import re
import sys

try:
    import yaml
except ImportError:
    print("ERROR: PyYAML not installed (pip3 install --user pyyaml)", file=sys.stderr)
    sys.exit(2)


class ParseError(Exception):
    pass


def load_docs(path):
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    try:
        return list(yaml.safe_load_all(text)), text
    except yaml.YAMLError as e:
        raise ParseError(str(e))


def comment_lines(text):
    """Best-effort: collect stripped '#'-starting comment line bodies, in order.
    Not perfect (doesn't handle '#' inside strings), but good enough as a
    lightweight informational signal, not the primary check."""
    out = []
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("#"): out.append(s)

    return out


def timestamp_now():
    now = datetime.datetime.now()

    return now.strftime("%Y-%m-%d %H:%M:%S.") + "%03d" % (now.microsecond // 1000)


def print_timestamped_header(rel_path):
    print("[%s] %s" % (timestamp_now(), rel_path))


def compare_one(orig_path, fmt_path, orig_label, fmt_label):
    """The full single-pair check, shared by both modes. Raises ParseError if
    either file fails to parse. Returns True if content is preserved, False
    on a mismatch (mismatch details printed to stdout)."""
    try:
        orig_docs, orig_text = load_docs(orig_path)
    except ParseError as e:
        raise ParseError(f"original file failed to parse: {orig_path}\n{e}")

    try:
        fmt_docs, fmt_text = load_docs(fmt_path)
    except ParseError as e:
        raise ParseError(f"formatted file failed to parse: {fmt_path}\n{e}")

    mismatches = []

    if len(orig_docs) != len(fmt_docs): mismatches.append(
            f"document count differs: original={len(orig_docs)} formatted={len(fmt_docs)}"
        )
    else:
        for i, (o, f) in enumerate(zip(orig_docs, fmt_docs)):
            if o != f:
                mismatches.append(f"document {i}: parsed structure differs\n  original:  {o!r}\n  formatted: {f!r}")

    if mismatches:
        print(f"MISMATCH: {orig_label} vs {fmt_label}")
        for m in mismatches: print(f"  - {m}")

        return False

    # Informational-only comment scan (not a failure condition by itself)
    oc = comment_lines(orig_text)
    fc = comment_lines(fmt_text)
    if oc != fc and len(oc) == len(fc):
        diffs = [(a, b) for a, b in zip(oc, fc) if a != b]
        if diffs:
            print(f"OK: content preserved (data structures match: {orig_label} == {fmt_label}). "
                  f"Note: {len(diffs)} comment line(s) textually changed (informational only):")
            for a, b in diffs[:10]: print(f"    {a!r} -> {b!r}")

            return True
    elif oc != fc:
        print(f"OK: content preserved (data structures match: {orig_label} == {fmt_label}). "
              f"Note: comment line count differs ({len(oc)} -> {len(fc)}), informational only.")

        return True

    print(f"OK: content preserved: {orig_label} vs {fmt_label}")

    return True


def print_usage():
    sys.stderr.write("Usage: yaml_content_diff.sh <original.yaml> <formatted.yaml>\n")
    sys.stderr.write("       yaml_content_diff.sh <original_base_dir> <formatted_base_dir> <yaml_rel_path_file_list.txt>\n")


def run_single(orig_arg, fmt_arg):
    print_timestamped_header(orig_arg)

    orig_exists, fmt_exists = os.path.exists(orig_arg), os.path.exists(fmt_arg)
    if not orig_exists or not fmt_exists:
        if not orig_exists and not fmt_exists: sys.stderr.write(f"WARNING: both {orig_arg} and {fmt_arg} are missing\n")
        elif not orig_exists:                  sys.stderr.write(f"WARNING: {orig_arg} is missing\n")
        else:                                  sys.stderr.write(f"WARNING: {fmt_arg} is missing\n")
        sys.exit(1)

    try:
        sys.exit(0 if compare_one(orig_arg, fmt_arg, orig_arg, fmt_arg) else 1)
    except ParseError as e:
        print(f"ERROR: {e}", file=sys.stderr)
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
            print(f"  WARNING: missing from both {orig_base_dir} and {fmt_base_dir} -- skipping")
            missing_count += 1
            continue
        if not orig_exists:
            print(f"  WARNING: missing from {orig_base_dir} -- skipping")
            missing_count += 1
            continue
        if not fmt_exists:
            print(f"  WARNING: missing from {fmt_base_dir} -- skipping")
            missing_count += 1
            continue

        try:
            if compare_one(orig_path, fmt_path, rel, rel): ok_count += 1
            else:                                          mismatch_count += 1
        except Exception as e:
            print(f"  ERROR: {e}")
            mismatch_count += 1

    print("")
    print(f"SUMMARY: {ok_count} OK, {mismatch_count} MISMATCH/ERROR, {missing_count} MISSING "
          f"(of {ok_count + mismatch_count + missing_count} files checked)")

    if mismatch_count > 0 or missing_count > 0: sys.exit(1)


def main():
    args = sys.argv[1:]
    if len(args) == 2:   run_single(args[0], args[1])
    elif len(args) == 3: run_batch(args[0], args[1], args[2])
    else:
        print_usage()
        sys.exit(2)


if __name__ == "__main__": main()
