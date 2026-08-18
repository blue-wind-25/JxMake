#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.

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

Usage (two modes -- see also print_usage() below):
    Single pair:
        python3 css_content_diff.py <original.css> <formatted.css>
    Batch (one Python process invocation over a whole corpus, avoiding a
    process restart per file -- rel-path list, one path per line, relative
    to both base dirs alike):
        python3 css_content_diff.py <original_base_dir> <formatted_base_dir> <css_rel_path_file_list.txt>

Before each pair's diff, a "[yyyy-MM-dd HH:mm:ss.SSS] <relative path>" line is
printed -- lets a hang/slow file in a large batch run be pinpointed, matching
js_ts_content_diff.js's/java_content_diff.java's/kotlin_content_diff.java's
own precedent. In batch mode, a rel-path missing from either base dir (or
both) is a warning, not a crash -- the file is skipped and the run continues;
the final SUMMARY line and process exit code still reflect it. One pair's
compare_one() is wrapped in try/except so a bad file doesn't abort the rest
of the batch.

Exit 0 if content is preserved (all pairs, in batch mode), 1 if any
mismatch/missing file/error is found (description printed for each), 2 on a
usage error.
"""
import datetime
import os
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


def timestamp_now():
    now = datetime.datetime.now()

    return now.strftime("%Y-%m-%d %H:%M:%S.") + "%03d" % (now.microsecond // 1000)


def print_timestamped_header(rel_path):
    print("[%s] %s" % (timestamp_now(), rel_path))


def compare_one(orig_path, fmt_path, orig_label, fmt_label):
    """The full single-pair check, shared by both modes. Returns True if
    content is preserved, False on a mismatch (mismatch details printed to
    stdout either way, matching the tool's pre-existing single-pair output)."""
    orig = open(orig_path, encoding='utf-8').read()
    fmt  = open(fmt_path, encoding='utf-8').read()

    ok = True

    # 1. Comments
    c_orig = extract_comments(orig)
    c_fmt  = extract_comments(fmt)
    if len(c_orig) != len(c_fmt):
        ok = False
        print(f"COMMENT COUNT MISMATCH ({orig_label} vs {fmt_label}): original={len(c_orig)} formatted={len(c_fmt)}")
    else:
        for i, (a, b) in enumerate(zip(c_orig, c_fmt)):
            if a != b:
                ok = False
                print(f"COMMENT DIFF at index {i} ({orig_label} vs {fmt_label}):")
                print(f"  ORIGINAL : {a!r}")
                print(f"  FORMATTED: {b!r}")

    # 2. Token stream (comments stripped, whitespace/colon-spacing normalized)
    t_orig = strip_comments_and_normalize(orig)
    t_fmt  = strip_comments_and_normalize(fmt)
    if t_orig != t_fmt:
        ok = False
        print(f"TOKEN STREAM MISMATCH ({orig_label} vs {fmt_label}, property/value/selector content changed):")
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
        print(f"!important COUNT MISMATCH ({orig_label} vs {fmt_label}): original={imp_orig} formatted={imp_fmt}")

    # 4. vendor-prefix counts
    vp_orig = vendor_prefix_counts(orig)
    vp_fmt  = vendor_prefix_counts(fmt)
    if vp_orig != vp_fmt:
        ok = False
        print(f"VENDOR-PREFIX MISMATCH ({orig_label} vs {fmt_label}):")
        keys = set(vp_orig) | set(vp_fmt)
        for k in sorted(keys):
            a, b = vp_orig.get(k, 0), vp_fmt.get(k, 0)
            if a != b:
                print(f"  {k}: original={a} formatted={b}")

    if ok:
        print(f"OK: content preserved ({orig_label} == {fmt_label}: {len(c_orig)} comments, "
              f"{imp_orig} !important, {sum(vp_orig.values())} vendor-prefixed "
              f"properties, all matched)")

    return ok


def print_usage():
    sys.stderr.write("Usage: css_content_diff.sh <original.css> <formatted.css>\n")
    sys.stderr.write("       css_content_diff.sh <original_base_dir> <formatted_base_dir> <css_rel_path_file_list.txt>\n")


def run_single(orig_arg, fmt_arg):
    print_timestamped_header(orig_arg)

    orig_exists, fmt_exists = os.path.exists(orig_arg), os.path.exists(fmt_arg)
    if not orig_exists or not fmt_exists:
        if not orig_exists and not fmt_exists:
            print(f"WARNING: both {orig_arg} and {fmt_arg} are missing")
        elif not orig_exists: print(f"WARNING: {orig_arg} is missing")
        else: print(f"WARNING: {fmt_arg} is missing")
        sys.exit(1)

    sys.exit(0 if compare_one(orig_arg, fmt_arg, orig_arg, fmt_arg) else 1)


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
            else: mismatch_count += 1
        except Exception as e:
            print(f"  ERROR: {e}")
            mismatch_count += 1

    print("")
    print(f"SUMMARY: {ok_count} OK, {mismatch_count} MISMATCH/ERROR, {missing_count} MISSING "
          f"(of {ok_count + mismatch_count + missing_count} files checked)")

    if mismatch_count > 0 or missing_count > 0: sys.exit(1)


def main():
    args = sys.argv[1:]
    if len(args) == 2: run_single(args[0], args[1])
    elif len(args) == 3: run_batch(args[0], args[1], args[2])
    else:
        print_usage()
        sys.exit(2)


if __name__ == '__main__': main()
