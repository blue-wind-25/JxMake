#!/usr/bin/env python3
# Copyright (C) 2022-2026 Aloysius Indrayanto
#
# This file is distributed under the Apache License, Version 2.0.
# See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.

"""Converts the hand-labeled `tools/classifier_weights/examples_{c,cpp,java,kotlin,js,ts}.md` markdown
tables (the corpus `CommentClassifierWeights`' linear-classifier constants are derived from, see
`tools/classifier_weights/weights.md`) into RDD_EXT_21-schema rows
(`<lang>\\t<label:YES|NO>\\t<targetWordIndex>\\t<escaped-text>`) so they can be folded into the
GRU's training corpus alongside `sample_default.txt`.

Why this exists (see STATE_AI.md's "GRU predicts YES on every example" finding): sample_default.txt
is auto-labeled *by the rule-based CommentClassifier itself*, so it structurally never contains the
genuinely hard leading-keyword-ambiguous cases -- those live only in these hand-labeled example sets.
Unlike sample_default.txt's auto-labeling, these rows already carry a human-reviewed ground-truth
label and must NOT be re-labeled by CommentClassifier -- the whole point is to inject the cases the
rule-based classifier's own corpus can't produce.

Every example in these files is a leading-keyword-ambiguity (Pool A) case -- KeywordAmbiguityGate's
own leadingWord() extraction is what put each one in scope for hand-labeling in the first place --
so targetWordIndex is always 0, same convention as add_target_index.py's pool-a case.

Table format assumption: a markdown table whose header row contains a "Comment text" column and a
"Label" column (column order otherwise varies file to file, e.g. examples_kotlin.md has an extra
"arrow?" column examples_c.md doesn't) -- columns are located by header name, not fixed position.
Comment text cells are backtick-quoted code spans (`` `...` ``); the backticks are stripped and the
cell content is used verbatim as the comment text before RDD_EXT_20 escaping.

Usage:
    python3 convert_classifier_weights_examples.py <dir-with-examples-*.md> --out <output-file>
"""

import argparse
import glob
import os
import re
import sys

LANG_BY_STEM = {
    "examples_c": "c",
    "examples_cpp": "cpp",
    "examples_java": "java",
    "examples_kotlin": "kotlin",
    # Added 2026-07-31 (STATE_AI.md's "extend classifier_weights" session): js/ts got their own
    # examples_*.md files once KeywordAmbiguityGate gained real KEYWORDS_JS/KEYWORDS_TS sets --
    # a new examples_<lang>.md file is otherwise silently skipped by this script's glob.glob
    # iteration unless its stem is added here too
    "examples_js": "js",
    "examples_ts": "ts",
    # Added 2026-08-10 (STATE_AI.md's "grow hand-labeled hard-case corpus" session): python3 was
    # confirmed to be the only data-format/tooling language that actually reaches
    # KeywordAmbiguityGate (MiscRuleIndent wired `#`-comment normalization through the same
    # classifyComment path 2026-08-08) -- json5/css/yaml/toml/xml/html5/makefile/bash/powershell
    # never reach it and deliberately have no examples_<lang>.md file
    "examples_python3": "python3",
}

CELL_SPLIT_RE = re.compile(r"(?<!\\)\|")


def escape(comment_text):
    # Mirrors extract_comments.py's escape() exactly, so downstream tooling (GruTrainer,
    # add_target_index.py-style unescaping) treats these rows identically to any other
    # RDD_EXT_20/21 source
    return comment_text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")


def split_row(line):
    cells = CELL_SPLIT_RE.split(line.strip())
    # A well-formed "| a | b | c |" row splits into ["", " a ", " b ", " c ", ""]
    if cells and cells[0].strip() == "": cells = cells[1:]
    if cells and cells[-1].strip() == "": cells = cells[:-1]

    return [c.strip() for c in cells]


def extract_comment_text(cell):
    # Cells are backtick-quoted code spans, e.g. "`static int cache_size;`". Strip exactly one
    # leading/trailing backtick pair; fall back to the raw cell if a row doesn't follow that
    # convention rather than dropping the row.
    if len(cell) >= 2 and cell.startswith("`") and cell.endswith("`"): return cell[1:-1]

    return cell


def convert_file(path):
    stem = os.path.splitext(os.path.basename(path))[0]
    lang = LANG_BY_STEM.get(stem)
    if lang is None:
        print(f"convert_classifier_weights_examples: skipping {path} -- unrecognized filename", file=sys.stderr)
        return []

    # A file's example table can be split across multiple `| N | ... |` blocks separated by
    # ordinary prose paragraphs (e.g. examples_c.md rows 13-17 resume after a corrections note,
    # with no repeated header row) -- so the header's column mapping, once found, applies to
    # every numbered data row for the rest of the file rather than resetting between blocks.
    rows      = []
    text_col  = None
    label_col = None
    with open(path, "r", encoding="utf-8") as inp:
        for line in inp:
            stripped = line.strip()
            if not stripped.startswith("|"): continue

            cells = split_row(stripped)

            if text_col is None:
                # Candidate header row -- look for "Comment text"/"Label" columns by name
                lowered = [c.lower() for c in cells]
                if "comment text" in lowered and "label" in lowered:
                    text_col  = lowered.index("comment text")
                    label_col = lowered.index("label")
                continue

            # A numbered data row's first cell is the row number; skip the separator row
            # ("|---|---|...") and any other non-data "|"-prefixed line (e.g. a second,
            # differently-shaped table later in the same file).
            if not cells or not re.fullmatch(r"\d+", cells[0]): continue

            if len(cells) <= max(text_col, label_col): continue
            label = cells[label_col].strip()
            if label not in ("YES", "NO"): continue

            comment_text = extract_comment_text(cells[text_col])
            if not comment_text: continue

            rows.append((lang, label, escape(comment_text)))

    return rows


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input_dir", help="directory holding examples_{c,cpp,java,kotlin,js,ts}.md")
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    all_rows = []
    for path in sorted(glob.glob(os.path.join(args.input_dir, "examples_*.md"))):
        all_rows.extend(convert_file(path))

    with open(args.out, "w", encoding="utf-8") as out:
        for lang, label, escaped_text in all_rows:
            out.write(f"{lang}\t{label}\t0\t{escaped_text}\n")

    print(f"convert_classifier_weights_examples: wrote {len(all_rows)} hand-labeled example(s) "
          f"to {args.out}", file=sys.stderr)


if __name__ == "__main__": main()
