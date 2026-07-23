#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""
yaml_content_diff.py -- content-preservation checker for YAML dogfood testing.

Parses both an original and a formatted YAML file with PyYAML's
`yaml.safe_load_all` (multi-document stream aware, same reasoning as
`yaml_sc.js`'s `loadAll()`) and compares the resulting Python data structures
(dict/list/scalar) document-by-document for equality. This proves keys,
values, and structure were not altered by formatting -- it does NOT catch
comment-only changes (comment normalization is a separate, lighter-weight
concern; see the accompanying text-based comment scan below, best-effort).

Usage:
    python3 yaml_content_diff.py <original.yaml> <formatted.yaml>

Exit codes:
    0 - all documents' parsed structures match (comment differences, if any,
        are printed as informational only, not a failure)
    1 - a structural/data mismatch was found (documents differ in count,
        or some document's parsed value differs) -- description printed
    2 - one of the two files failed to parse as YAML at all (not applicable
        to a real dogfood run where both files should already be
        syntax-checked separately)
"""
import sys
import re

try:
    import yaml
except ImportError:
    print("ERROR: PyYAML not installed (pip3 install --user pyyaml)", file=sys.stderr)
    sys.exit(2)


def load_docs(path):
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    return list(yaml.safe_load_all(text)), text


def comment_lines(text):
    """Best-effort: collect stripped '#'-starting comment line bodies, in order.
    Not perfect (doesn't handle '#' inside strings), but good enough as a
    lightweight informational signal, not the primary check."""
    out = []
    for line in text.splitlines():
        s = line.strip()
        if s.startswith("#"):
            out.append(s)
    return out


def main():
    if len(sys.argv) != 3:
        print("Usage: python3 yaml_content_diff.py <original.yaml> <formatted.yaml>", file=sys.stderr)
        sys.exit(2)

    orig_path, fmt_path = sys.argv[1], sys.argv[2]

    try:
        orig_docs, orig_text = load_docs(orig_path)
    except yaml.YAMLError as e:
        print(f"ERROR: original file failed to parse: {orig_path}\n{e}", file=sys.stderr)
        sys.exit(2)

    try:
        fmt_docs, fmt_text = load_docs(fmt_path)
    except yaml.YAMLError as e:
        print(f"ERROR: formatted file failed to parse: {fmt_path}\n{e}", file=sys.stderr)
        sys.exit(2)

    mismatches = []

    if len(orig_docs) != len(fmt_docs):
        mismatches.append(
            f"document count differs: original={len(orig_docs)} formatted={len(fmt_docs)}"
        )
    else:
        for i, (o, f) in enumerate(zip(orig_docs, fmt_docs)):
            if o != f:
                mismatches.append(f"document {i}: parsed structure differs\n  original:  {o!r}\n  formatted: {f!r}")

    if mismatches:
        print(f"MISMATCH: {orig_path} vs {fmt_path}")
        for m in mismatches:
            print(f"  - {m}")
        sys.exit(1)

    # informational-only comment scan (not a failure condition by itself)
    oc = comment_lines(orig_text)
    fc = comment_lines(fmt_text)
    if oc != fc and len(oc) == len(fc):
        diffs = [(a, b) for a, b in zip(oc, fc) if a != b]
        if diffs:
            print(f"OK: content preserved (data structures match). Note: {len(diffs)} comment line(s) textually changed (informational only):")
            for a, b in diffs[:10]:
                print(f"    {a!r} -> {b!r}")
            sys.exit(0)
    elif oc != fc:
        print(f"OK: content preserved (data structures match). Note: comment line count differs ({len(oc)} -> {len(fc)}), informational only.")
        sys.exit(0)

    print(f"OK: content preserved: {orig_path} vs {fmt_path}")
    sys.exit(0)


if __name__ == "__main__":
    main()
