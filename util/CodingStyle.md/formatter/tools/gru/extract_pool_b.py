#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.

"""Extracts Pool B (period-ambiguity) candidates for Step 3's GRU training set --
STATE_AI.md's "Training-set acquisition" section, filter design per RDD_EXT_15.

Reads the same corpus format tools/gru/extract_comments.py writes and
tools/gru/CommentAbstainTally.java / tools/gru/ExtractPoolA.java read
("<lang>\\t<escaped comment text>"). Unlike Pool A, this is NOT derived from
CommentClassifier's ABSTAIN output -- Pool B targets a different ambiguity
class entirely (comments that discuss punctuation itself, or unusual
abbreviations beyond the already-handled e.g./i.e./file-extension cases, that
defeat MiscRule.stripSoleTrailingPeriod's dotCount != 1 heuristic -- see the
mid-word-dot TODO in STATE_AI.md), so this applies its own grep-based
recall-favoring filter directly to the raw comment text instead.

Per RDD_EXT_15, a comment is a Pool B candidate if either:
  (a) it contains 2+ "." characters, at least one of which is surrounded by
      whitespace on both sides (the punctuation-discussion case, e.g.
      "the dot . here"), or
  (b) it contains a known abbreviation-adjacent token (etc./vs./approx./a
      single capital letter followed by a dot, e.g. "extern C.") that is not
      immediately followed by more lowercase sentence text.

This is deliberately recall-favoring, not precise -- false positives are
expected and get discarded during Pool B's by-hand labeling pass (see
STATE_AI.md's "Labeling -- Pool B"), not filtered out here.

Usage:
    python3 extract_pool_b.py <extracted-comments-path> --out <output-path>
"""

import argparse
import re
import sys

WHITESPACE_SURROUNDED_DOT_RE = re.compile(r"\s\.\s")

ABBREVIATION_TOKENS = ("etc.", "vs.", "approx.")
ABBREVIATION_TOKEN_RE = re.compile(
    r"\b(?:" + "|".join(re.escape(tok) for tok in ABBREVIATION_TOKENS) + r")(?![a-z])"
)
# Single capital letter followed by a dot (e.g. "extern C."), not itself the
# start of a new sentence's next word run -- reject if immediately followed by
# more lowercase text (that's ordinary mid-sentence prose, not the
# abbreviation-adjacent shape this pool targets).
SINGLE_CAPITAL_DOT_RE = re.compile(r"\b[A-Z]\.(?![a-z])")


def unescape(escaped):
    out = []
    i   = 0
    while i < len(escaped):
        c = escaped[i]
        if c == "\\" and i + 1 < len(escaped):
            nxt = escaped[i + 1]
            if nxt == "n":
                out.append("\n")
                i += 2
                continue
            if nxt == "t":
                out.append("\t")
                i += 2
                continue
            if nxt == "\\":
                out.append("\\")
                i += 2
                continue
        out.append(c)
        i += 1

    return "".join(out)


def is_pool_b_candidate(comment_text):
    dot_count = comment_text.count(".")
    if dot_count >= 2 and WHITESPACE_SURROUNDED_DOT_RE.search(comment_text): return True
    if ABBREVIATION_TOKEN_RE.search(comment_text): return True
    if SINGLE_CAPITAL_DOT_RE.search(comment_text): return True

    return False


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input", help="Path from extract_comments.py")
    parser.add_argument("--out", required=True, help="Output file path")
    args = parser.parse_args()

    total     = 0
    malformed = 0
    kept      = 0
    with open(args.input, "r", encoding="utf-8") as inp, \
            open(args.out, "w", encoding="utf-8") as out:
        for line in inp:
            line = line.rstrip("\n")
            if not line: continue
            tab = line.find("\t")
            if tab < 0:
                malformed += 1
                continue
            total        += 1
            lang_name     = line[:tab]
            escaped_text  = line[tab + 1:]
            comment_text  = unescape(escaped_text)

            if is_pool_b_candidate(comment_text):
                out.write(f"{lang_name}\t{escaped_text}\n")
                kept += 1

    print(f"extract_pool_b: {total} comment(s) read ({malformed} malformed line(s) skipped), "
          f"{kept} kept as Pool B (period-ambiguity) candidate(s)", file=sys.stderr)


if __name__ == "__main__": main()
