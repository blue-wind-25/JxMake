#!/usr/bin/env python3
"""Derives CommentClassifierWeights constants from the labeled examples in
tools/classifier_weights/examples_{c,cpp,java,kotlin,js,ts,python3}.md via a small logistic regression
trained in this script (pure Python, no dependencies). Run with:
    python3 tools/classifier_weights/derive_weights.py

This is the reusable counterpart to the by-hand derivation in tools/classifier_weights/weights.md.
DATASET is parsed directly from the examples_*.md tables' own paren?/arrow?/semi?/url/num?/Label
columns (see load_dataset() below) -- it is NOT hand-maintained, specifically so adding rows to an
examples_*.md file (as tools/gru/convert_classifier_weights_examples.py's hand-labeled corpus rows
already require doing) automatically flows into this script's next run with no separate transcription
step to remember or get out of sync. After adding examples, just re-run this script and copy the
printed constants into src/com/jxmake/formatter/classifier/CommentClassifierWeights.java.
"""

import glob
import math
import os
import re

# Mirrors tools/gru/convert_classifier_weights_examples.py's LANG_BY_STEM exactly -- kept as a
# separate copy rather than a shared import since every tool under tools/ is deliberately
# self-contained (see e.g. extract_comments.py, GenerateSampleDefault.java). A new examples_<lang>.md
# file needs a stem added here too, same caveat as that script's own LANG_BY_STEM.
LANG_BY_STEM = {
    "examples_c": "c",
    "examples_cpp": "cpp",
    "examples_java": "java",
    "examples_kotlin": "kotlin",
    "examples_js": "js",
    "examples_ts": "ts",
    "examples_python3": "python3",
}

CELL_SPLIT_RE = re.compile(r"(?<!\\)\|")

# Feature columns located by header name (case-insensitive), like "Comment text"/"Label" in
# convert_classifier_weights_examples.py. "arrow?" (CommentFeatureVector.nextTokenIsArrow) only
# exists in examples_kotlin.md today -- absent elsewhere, defaults to 0 (JS/TS/C/C++/Java have no
# `->` branch-arrow shape in the language). "paren?"/"semi?"/"url/num?" are required in every file.
FEATURE_COLUMNS = ("paren?", "arrow?", "semi?", "url/num?")


def split_row(line):
    cells = CELL_SPLIT_RE.split(line.strip())
    if cells and cells[0].strip() == "": cells = cells[1:]
    if cells and cells[-1].strip() == "": cells = cells[:-1]

    return [c.strip() for c in cells]


def cell_to_bit(cell):
    return 1 if cell.strip().lower() == "yes" else 0


def parse_examples_file(path):
    stem = os.path.splitext(os.path.basename(path))[0]
    lang = LANG_BY_STEM.get(stem)
    if lang is None: return []

    rows      = []
    col_index = None  # Header name -> cell index, once the header row is found
    label_col = None
    with open(path, "r", encoding="utf-8") as inp:
        for line in inp:
            stripped = line.strip()
            if not stripped.startswith("|"): continue

            cells = split_row(stripped)

            if col_index is None:
                lowered = [c.lower() for c in cells]
                if "label" in lowered and all(c in lowered for c in ("paren?", "semi?", "url/num?")):
                    label_col = lowered.index("label")
                    col_index = {name: (lowered.index(name) if name in lowered else None)
                                 for name in FEATURE_COLUMNS}
                continue

            # Numbered data row; skip the "|---|...|" separator and anything else non-data.
            if not cells or not re.fullmatch(r"\d+", cells[0]): continue
            if len(cells) <= max(label_col, *(i for i in col_index.values() if i is not None)):
                continue

            label_cell = cells[label_col].strip()
            if label_cell not in ("YES", "NO"): continue

            index = int(cells[0])
            features = [cell_to_bit(cells[col_index[name]]) if col_index[name] is not None else 0
                        for name in FEATURE_COLUMNS]
            label = 1 if label_cell == "YES" else 0
            rows.append((lang, index) + tuple(features) + (label,))

    return rows


def load_dataset(examples_dir):
    dataset = []
    for path in sorted(glob.glob(os.path.join(examples_dir, "examples_*.md"))):
        dataset.extend(parse_examples_file(path))

    return dataset


# (source, index, paren, arrow, semicolon, url_or_number, label) -- label 1 = YES (normalize),
# 0 = NO. "arrow" is CommentFeatureVector.nextTokenIsArrow -- added to catch a when/match-branch
# shape like "is Foo -> handle(foo)" that the other three features can't see (kotlin #2).
DATASET = load_dataset(os.path.dirname(os.path.abspath(__file__)))

LEARNING_RATE = 0.5
EPOCHS        = 5000

# L2 regularization strength. Without this, gradient descent on (near-)separable data never
# converges -- weights just keep growing every epoch to push predictions closer to 0/1 confidence,
# so the raw magnitude is an arbitrary function of EPOCHS rather than a meaningful number. The
# penalty term caps that growth at a finite optimum, at the cost of some prediction confidence.
# Bias is deliberately excluded from the penalty (standard practice -- regularizing bias just
# shifts the decision boundary off zero for no benefit).
L2_LAMBDA = 0.1


def sigmoid(z):
    return 1.0 / (1.0 + math.exp(-z))


def train():
    # Weights: [bias, w_paren, w_arrow, w_semicolon, w_url_or_number]
    w = [0.0, 0.0, 0.0, 0.0, 0.0]
    n = len(DATASET)
    for _ in range(EPOCHS):
        grad = [0.0, 0.0, 0.0, 0.0, 0.0]
        for _, _, paren, arrow, semi, urlnum, label in DATASET:
            x    = [1.0, float(paren), float(arrow), float(semi), float(urlnum)]
            pred = sigmoid(sum(wi * xi for wi, xi in zip(w, x)))
            err  = pred - label
            for i in range(5):
                grad[i] += err * x[i]
        # L2 penalty gradient is lambda * w for each regularized weight (skip index 0, the bias)
        for i in range(1, 5): grad[i] += L2_LAMBDA * w[i]
        w = [wi - LEARNING_RATE * gi / n for wi, gi in zip(w, grad)]

    return w


def report(w):
    bias, w_paren, w_arrow, w_semi, w_urlnum = w
    print()
    print("====================================================================================================")
    print("Trained keyword-ambiguity weights (logistic regression, %d epochs, lr=%.2f):" % (EPOCHS, LEARNING_RATE))
    print("    KEYWORD_BIAS                 = %+.5f" % bias)
    print("    KEYWORD_WEIGHT_PAREN         = %+.5f" % w_paren)
    print("    KEYWORD_WEIGHT_ARROW         = %+.5f" % w_arrow)
    print("    KEYWORD_WEIGHT_SEMICOLON     = %+.5f" % w_semi)
    print("    KEYWORD_WEIGHT_URL_OR_NUMBER = %+.5f" % w_urlnum)
    print("    KEYWORD_THRESHOLD            =  0.00000 (sigmoid decision boundary)")
    print()
    print("Per-example check (score = w.x + bias, predicted YES iff score > 0):")
    mistakes = 0
    for source, idx, paren, arrow, semi, urlnum, label in DATASET:
        score     = bias + w_paren * paren + w_arrow * arrow + w_semi * semi + w_urlnum * urlnum
        predicted = 1 if score > 0 else 0
        expected  = "YES" if label else "NO"
        got       = "YES" if predicted else "NO"
        flag      = "" if predicted == label else " <-- MISMATCH"
        if predicted != label: mistakes += 1
        print("  %-6s #%-2d score=%+9.5f expected=%-3s predicted=%-3s%s" % (source, idx, score, expected, got, flag))
    print()
    print("%d/%d examples classified as expected (%d mismatch(es))." % (len(DATASET) - mistakes, len(DATASET), mistakes))
    print()
    print("Main path (CommentClassifier.classify, no leading-keyword ambiguity): everything")
    print("reaching that path already cleared both gates, so BIAS=1.0 / THRESHOLD=0.0 (always YES)")
    print("is not derived from this dataset -- see tools/classifier_weights/weights.md 'Main path'.")
    print("====================================================================================================")
    print()


if __name__ == "__main__": report(train())
