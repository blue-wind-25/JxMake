#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
# See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.

"""Adds RDD_EXT_21's targetWordIndex column to an RDD_EXT_20-schema labeled-examples
file (`<lang>\\t<label:YES|NO>\\t<escaped-text>`), producing the RDD_EXT_21 schema
GruTrainer now reads (`<lang>\\t<label>\\t<targetWordIndex>\\t<escaped-text>`).

Convention (RDD_EXT_21): the target word is the leading keyword for Pool A
(keyword-ambiguity) examples -- always token index 0, since KeywordAmbiguityGate's
own leadingWord() extraction (the thing that made these ABSTAIN in the first place)
is exactly the first token GruClassifier.tokenize would produce. For Pool B
(period-ambiguity) examples the target is the comment's last token -- the position
MiscRuleCore's sole-trailing-period stripping decision would apply to, whether or
not that token actually is a "." (many Pool B candidates are ambiguous-looking but
don't end in a period at all, in which case there is nothing to strip and the label
is trivially NO -- see STATE_AI.md's Pool B labeling methodology).

tokenize() below mirrors com.jxmake.formatter.classifier.gru.GruClassifier.tokenize
exactly (word-char runs vs. individual other characters, whitespace skipped) --
RDD_EXT_13's bit-for-bit-identical requirement for tokenize()/hashBucket() doesn't
technically cover this one-off conversion script, but staying consistent avoids an
off-by-one between how this script counts tokens and how GruTrainer/GruClassifier
count them when they later re-tokenize the same text.

Usage:
    python3 add_target_index.py <pool-a|pool-b> <input-labeled-file> --out <output-file>
"""

import argparse
import sys


def unescape(escaped):
    out = []
    i = 0
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


def is_word_char(c):
    return c.isalnum() or c == "_"


def tokenize(text):
    tokens = []
    i = 0
    n = len(text)
    while i < n:
        c = text[i]
        if c.isspace():
            i += 1
            continue
        if is_word_char(c):
            start = i
            while i < n and is_word_char(text[i]):
                i += 1
            tokens.append(text[start:i])
        else:
            tokens.append(c)
            i += 1
    return tokens


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("pool", choices=["pool-a", "pool-b"])
    parser.add_argument("input", help="RDD_EXT_20-schema labeled-examples file")
    parser.add_argument("--out", required=True, help="Output file path (RDD_EXT_21 schema)")
    args = parser.parse_args()

    total = 0
    skipped_no_tokens = 0
    with open(args.input, "r", encoding="utf-8") as inp, open(args.out, "w", encoding="utf-8") as out:
        for line in inp:
            line = line.rstrip("\n")
            if not line:
                continue
            parts = line.split("\t", 2)
            if len(parts) != 3:
                continue
            lang_name, label, escaped_text = parts
            if label not in ("YES", "NO"):
                continue
            comment_text = unescape(escaped_text)
            tokens = tokenize(comment_text)
            if not tokens:
                skipped_no_tokens += 1
                continue
            target_index = 0 if args.pool == "pool-a" else len(tokens) - 1
            total += 1
            out.write(f"{lang_name}\t{label}\t{target_index}\t{escaped_text}\n")

    print(f"add_target_index: wrote {total} example(s) to {args.out} "
          f"({skipped_no_tokens} skipped for having no tokens)", file=sys.stderr)


if __name__ == "__main__":
    main()
