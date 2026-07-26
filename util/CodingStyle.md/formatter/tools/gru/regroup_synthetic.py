#!/usr/bin/env python3
"""
regroup_synthetic.py -- Python 3.6+

Takes one pasted-in file where Pool A and Pool B lines (and possibly output
from several chat responses/models) have been dumped together, and splits
them back into pool_a.tsv / pool_b.tsv, per RDD_EXT_20/21 schema. Malformed
or ambiguous lines go to unresolved.tsv for manual review instead of being
silently dropped.

Whitespace between fields is tolerant: any run of spaces/tabs counts as one
separator (maxsplit=3), so inconsistent copy/paste spacing from chat UIs is
fine as long as each line has at least 3 whitespace-separated fields before
the comment text.

Classification rule:
    targetWordIndex == 0                       -> Pool A
    targetWordIndex == index of the LAST token -> Pool B
    (single-token comment satisfies both       -> Pool A, noted in unresolved
     only if it ALSO fails other checks)
    anything else / malformed                  -> unresolved

Usage:
    python3 regroup_synthetic.py --input pasted.txt --outdir ./out
"""

import argparse
import os
import re
import sys

VALID_LABELS = {"YES", "NO"}
FIELD_SPLIT = re.compile(r"[ \t]+")


def classify(lang, label, idx_str, text):
    """Returns ('A'|'B'|None, reason_if_none)."""
    if lang.strip() == "":
        return None, "empty lang"
    if label not in VALID_LABELS:
        return None, "label not YES/NO: {!r}".format(label)
    try:
        idx = int(idx_str)
    except ValueError:
        return None, "targetWordIndex not an int: {!r}".format(idx_str)
    if idx < 0:
        return None, "negative targetWordIndex"
    if text.strip() == "":
        return None, "empty comment text"

    tokens = text.strip().split()
    last_idx = len(tokens) - 1

    if idx == 0:
        return "A", None
    if idx == last_idx:
        return "B", None
    return None, "targetWordIndex {} matches neither first (0) nor last ({}) token".format(idx, last_idx)


def main():
    ap = argparse.ArgumentParser(description="Regroup scattered Pool A/B lines back into separate files.")
    ap.add_argument("--input", required=True, help="pasted combined file")
    ap.add_argument("--outdir", default=".", help="directory to write pool_a.tsv / pool_b.tsv / unresolved.tsv")
    args = ap.parse_args()

    os.makedirs(args.outdir, exist_ok=True)

    pool_a, pool_b, unresolved = [], [], []
    total_lines = 0
    skipped_blank_or_comment = 0

    with open(args.input, "r", encoding="utf-8") as f:
        for raw_line in f:
            line = raw_line.rstrip("\n").rstrip("\r")
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                skipped_blank_or_comment += 1
                continue

            total_lines += 1
            parts = FIELD_SPLIT.split(stripped, maxsplit=3)
            if len(parts) < 4:
                unresolved.append((line, "fewer than 4 fields"))
                continue

            lang, label, idx_str, text = parts
            pool, reason = classify(lang, label, idx_str, text)
            # normalize the output line to real tabs regardless of input spacing
            norm_line = "\t".join([lang, label, idx_str, text])

            if pool == "A":
                pool_a.append(norm_line)
            elif pool == "B":
                pool_b.append(norm_line)
            else:
                unresolved.append((line, reason))

    a_path = os.path.join(args.outdir, "pool_a.tsv")
    b_path = os.path.join(args.outdir, "pool_b.tsv")
    u_path = os.path.join(args.outdir, "unresolved.tsv")

    with open(a_path, "w", encoding="utf-8") as f:
        f.write("\n".join(pool_a) + ("\n" if pool_a else ""))
    with open(b_path, "w", encoding="utf-8") as f:
        f.write("\n".join(pool_b) + ("\n" if pool_b else ""))
    with open(u_path, "w", encoding="utf-8") as f:
        for line, reason in unresolved:
            f.write("{}\t# {}\n".format(line, reason))

    print("processed {} data lines ({} blank/comment lines skipped)".format(
        total_lines, skipped_blank_or_comment))
    print("  Pool A: {} -> {}".format(len(pool_a), a_path))
    print("  Pool B: {} -> {}".format(len(pool_b), b_path))
    print("  unresolved: {} -> {}".format(len(unresolved), u_path))
    if unresolved:
        print("  (review unresolved.tsv by hand -- these were not discarded, just not auto-classified)",
              file=sys.stderr)


if __name__ == "__main__":
    main()
