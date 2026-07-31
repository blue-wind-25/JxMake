#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""
css_content_diff.py — CSS content-preservation checker.

Complements css_syntax_check.js (postcss-based syntax check: "is it still valid CSS?").
This script instead asks "did the formatter change any *meaning*?" by
comparing an original CSS file against a formatted version of it:

    1. Extracts every /* ... */ comment from each file (whitespace-normalized)
       and diffs them pairwise, in order. Comment text/wording/case/punctuation
       must be byte-identical after whitespace normalization — only
       re-indentation/re-wrapping is allowed. This is the check that would have
       caught the twbs/bootstrap rtlcss directive-comment corruption bug
       (fixture real_code_regressions_69) that a pure syntax check could not,
       since the corrupted comment was still syntactically valid CSS.
    2. Strips all comments, then normalizes all whitespace (including around
       ':') and diffs the remaining token stream. This must be identical too —
       it proves no property/value/selector was added, removed, or reordered
       (colon-spacing/indentation-width differences are expected style changes
       and are normalized away before this comparison, not treated as bugs).
    3. Counts and compares '!important' occurrences (must match exactly — a
       silently dropped '!important' would not show up in a normal diff view
       as dramatically and is a specific known risk).
    4. Counts and compares vendor-prefixed properties (-webkit-/-moz-/-ms-/-o-),
       by exact matched (prefix, property-name) pairs — catches a prefix being
       altered or dropped even if the unprefixed property name is unchanged.

Usage:
    python3 css_content_diff.py <original.css> <formatted.css>

Exit code 0 if all four checks pass (content preserved), 1 otherwise, with a
description of every mismatch printed to stdout.

Used for real-code dogfood testing per STATE_DATA_FORMATS.md's "Dogfood
Output Validation" section — first written to check necolas/normalize.css's
single normalize.css file, but is file-pair-generic and reusable for any CSS
dogfood corpus (run once per file, no batching support needed for a
one-file-at-a-time content diff).
"""
import re
import sys


COMMENT_RE       = re.compile(r'/\*.*?\*/', re.S)
VENDOR_PREFIX_RE = re.compile(r'-(webkit|moz|ms|o)-[a-zA-Z-]+')


def extract_comments(text):
    return [re.sub(r'\s+', ' ', c).strip() for c in COMMENT_RE.findall(text)]


def strip_comments_and_normalize(text):
    stripped = COMMENT_RE.sub('', text)
    stripped = re.sub(r'\s*:\s*', ':', stripped)

    return re.sub(r'\s+', ' ', stripped).strip()


def vendor_prefix_counts(text):
    counts = {}
    for m in VENDOR_PREFIX_RE.finditer(text):
        key = m.group(0)
        counts[key] = counts.get(key, 0) + 1

    return counts


def main():
    if len(sys.argv) != 3:
        print("Usage: css_content_diff.sh <original.css> <formatted.css>")
        sys.exit(2)

    orig_path, fmt_path = sys.argv[1], sys.argv[2]
    orig = open(orig_path, encoding='utf-8').read()
    fmt  = open(fmt_path, encoding='utf-8').read()

    ok = True

    # 1. Comments
    c_orig = extract_comments(orig)
    c_fmt  = extract_comments(fmt)
    if len(c_orig) != len(c_fmt):
        ok = False
        print(f"COMMENT COUNT MISMATCH: original={len(c_orig)} formatted={len(c_fmt)}")
    else:
        for i, (a, b) in enumerate(zip(c_orig, c_fmt)):
            if a != b:
                ok = False
                print(f"COMMENT DIFF at index {i}:")
                print(f"  ORIGINAL : {a!r}")
                print(f"  FORMATTED: {b!r}")

    # 2. Token stream (comments stripped, whitespace/colon-spacing normalized)
    t_orig = strip_comments_and_normalize(orig)
    t_fmt  = strip_comments_and_normalize(fmt)
    if t_orig != t_fmt:
        ok = False
        print("TOKEN STREAM MISMATCH (property/value/selector content changed):")
        for i in range(min(len(t_orig), len(t_fmt))):
            if t_orig[i] != t_fmt[i]:
                lo = max(0, i - 40)
                print(f"  first diff at char {i}:")
                print(f"    ORIGINAL : ...{t_orig[lo:i+40]}...")
                print(f"    FORMATTED: ...{t_fmt[lo:i+40]}...")
                break

        else:
            print(f"  (differ only in trailing length: {len(t_orig)} vs {len(t_fmt)} chars)")

    # 3. !important count
    imp_orig = orig.count('!important')
    imp_fmt  = fmt.count('!important')
    if imp_orig != imp_fmt:
        ok = False
        print(f"!important COUNT MISMATCH: original={imp_orig} formatted={imp_fmt}")

    # 4. vendor-prefix counts
    vp_orig = vendor_prefix_counts(orig)
    vp_fmt  = vendor_prefix_counts(fmt)
    if vp_orig != vp_fmt:
        ok = False
        print("VENDOR-PREFIX MISMATCH:")
        keys = set(vp_orig) | set(vp_fmt)
        for k in sorted(keys):
            a, b = vp_orig.get(k, 0), vp_fmt.get(k, 0)
            if a != b:
                print(f"  {k}: original={a} formatted={b}")

    if ok:
        print(f"OK: content preserved ({len(c_orig)} comments, "
              f"{imp_orig} !important, {sum(vp_orig.values())} vendor-prefixed "
              f"properties, all matched)")
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == '__main__': main()
