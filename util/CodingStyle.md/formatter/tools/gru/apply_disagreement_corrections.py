#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.

"""Applies tools/gru/disagreement_corrections.txt (hand-verified label corrections, see that
file's own header) onto a freshly (re)generated tools/gru/sample_default.txt, in place.

Why this exists: `make gru-acquire-corpus` regenerates sample_default.txt from scratch on every
run (re-extract, re-auto-label via GenerateSampleDefault's rule-based CommentClassifier pass) --
so a corrected label hand-edited directly into sample_default.txt would be silently wiped out by
the very next run. disagreement_corrections.txt is a separate, permanent file surviving every
regeneration; this script re-applies it each time, right after the existing
classifier_weights_examples.tsv append and before the final exact-duplicate-line dedup.

A correction and the auto-labeled row it corrects necessarily share the same comment text (that's
what "correcting a label" means) -- so a naive append would leave both the old wrong-labeled row
and the new corrected row present side by side (the exact-duplicate-line dedup doesn't help, since
the two lines differ in their <label> field). This script instead does an override merge, keyed on
everything EXCEPT the label column (<lang>, <targetWordIndex>, <escaped-comment-text>): any
sample_default.txt row whose key matches a correction is dropped, then every correction row is
appended -- so the correction always wins, never coexists with the label it replaces.

Usage:
    python3 apply_disagreement_corrections.py <sample-default-path> <corrections-path>

If <corrections-path> has no data rows (only #-comments/blank lines, e.g. the empty template
before the disagreement-sampling process has produced any confirmed corrections yet), this is a
no-op -- <sample-default-path> is left byte-identical.
"""

import sys


def parse_data_lines(path):
    """Returns a list of (key, full_line) for every non-blank, non-#-comment line in `path`,
    where key = (lang, target_word_index, escaped_comment_text) -- i.e. every RDD_EXT_21 column
    except the label. Malformed lines (fewer than 4 tab-separated columns) are skipped, mirroring
    GruTrainer.readExamples's own tolerance for stray malformed rows.
    """
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#"): continue
            fields = line.split("\t")
            if len(fields) != 4: continue
            lang, _label, target_word_index, escaped_text = fields
            key = (lang, target_word_index, escaped_text)
            rows.append((key, line))

    return rows


def main():
    if len(sys.argv) != 3:
        sys.stderr.write("Usage: apply_disagreement_corrections.py <sample-default-path> <corrections-path>\n")
        return 2

    sample_path, corrections_path = sys.argv[1], sys.argv[2]

    corrections = parse_data_lines(corrections_path)
    if not corrections:
        print("apply_disagreement_corrections: no corrections to apply (0 data rows in "
              + corrections_path + ")")
        return 0

    correction_keys = {key for key, _line in corrections}

    with open(sample_path, "r", encoding="utf-8") as f:
        original_lines = [line.rstrip("\n") for line in f]

    kept_lines = []
    overridden = 0
    for line in original_lines:
        if not line or line.startswith("#"):
            kept_lines.append(line)
            continue
        fields = line.split("\t")
        if len(fields) != 4:
            kept_lines.append(line)
            continue
        lang, _label, target_word_index, escaped_text = fields
        key = (lang, target_word_index, escaped_text)
        if key in correction_keys:
            overridden += 1
            continue
        kept_lines.append(line)

    kept_lines.extend(line for _key, line in corrections)

    with open(sample_path, "w", encoding="utf-8") as f:
        for line in kept_lines:
            f.write(line + "\n")

    print("apply_disagreement_corrections: applied " + str(len(corrections))
          + " correction(s) (" + str(overridden) + " conflicting auto-labeled row(s) overridden)")

    return 0


if __name__ == "__main__": sys.exit(main())
