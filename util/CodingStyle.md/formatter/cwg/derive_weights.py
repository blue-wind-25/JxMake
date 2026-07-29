#!/usr/bin/env python3
"""Derives CommentClassifierWeights constants from the labeled examples in
cwg/examples_{c,cpp,java,kotlin}.md via a small logistic regression trained in this script
(pure Python, no dependencies). Run with: python3 cwg/derive_weights.py

This is the reusable counterpart to the by-hand derivation in cwg/weights.md -- extend the
DATASET below when adding new labeled examples, re-run, and copy the printed constants into
src/com/jxmake/formatter/classifier/CommentClassifierWeights.java.
"""

import math

# One row per labeled example across cwg/examples_{c,cpp,java,kotlin}.md.
# (source, index, paren, arrow, semicolon, url_or_number, label) -- label 1 = YES (normalize),
# 0 = NO. "arrow" is CommentFeatureVector.nextTokenIsArrow -- added to catch a when/match-branch
# shape like "is Foo -> handle(foo)" that the other three features can't see (kotlin #2 below).
DATASET = [
    # examples_c.md
    ("c", 1, 0, 0, 0, 0, 1), ("c", 2, 0, 0, 1, 0, 0), ("c", 3, 1, 0, 0, 0, 0),
    ("c", 4, 0, 0, 0, 0, 1), ("c", 5, 0, 0, 1, 0, 0), ("c", 6, 0, 0, 0, 1, 1),
    ("c", 7, 0, 0, 0, 1, 0), ("c", 8, 0, 0, 0, 0, 1), ("c", 9, 0, 0, 0, 0, 1),
    ("c", 10, 0, 0, 1, 0, 0), ("c", 11, 0, 0, 0, 0, 1), ("c", 12, 1, 0, 0, 0, 0),
    # rows 13-17 added 2026-07-30 to fix the KEYWORD_BIAS regression (see cwg/examples_c.md).
    ("c", 13, 0, 0, 0, 0, 0), ("c", 14, 0, 0, 0, 0, 0), ("c", 15, 0, 0, 0, 0, 0),
    ("c", 16, 0, 0, 0, 0, 0), ("c", 17, 0, 0, 0, 0, 0),
    # examples_cpp.md
    ("cpp", 1, 0, 0, 0, 0, 1), ("cpp", 2, 1, 0, 1, 0, 0), ("cpp", 3, 0, 0, 0, 0, 1),
    ("cpp", 4, 1, 0, 0, 1, 0), ("cpp", 5, 0, 0, 0, 0, 1), ("cpp", 6, 1, 0, 1, 0, 0),
    ("cpp", 7, 0, 0, 0, 0, 1), ("cpp", 8, 1, 0, 1, 0, 0), ("cpp", 9, 0, 0, 0, 0, 1),
    ("cpp", 10, 0, 0, 0, 1, 0),
    # rows 11-15 are real regressions from test/cpp_modern_inp.cpp and test/cpp_combined_inp.cpp,
    # plus hand-authored analogues, added 2026-07-30 (see cwg/examples_cpp.md).
    ("cpp", 11, 0, 0, 0, 0, 0), ("cpp", 12, 0, 0, 0, 0, 0), ("cpp", 13, 0, 0, 0, 0, 0),
    ("cpp", 14, 0, 0, 0, 0, 0), ("cpp", 15, 0, 0, 0, 0, 0),
    # examples_java.md
    ("java", 1, 0, 0, 0, 0, 1), ("java", 2, 0, 0, 1, 0, 0), ("java", 3, 0, 0, 0, 0, 1),
    ("java", 4, 1, 0, 1, 0, 0), ("java", 5, 0, 0, 0, 0, 1), ("java", 6, 0, 0, 1, 1, 0),
    ("java", 7, 0, 0, 0, 0, 1), ("java", 8, 1, 0, 1, 0, 0), ("java", 9, 0, 0, 0, 0, 1),
    ("java", 10, 0, 0, 1, 0, 0),
    # rows 11-18 are real regressions from test/java_core_inp.java, test/java_combined_inp.java,
    # and test/java_comments_inp.java, plus hand-authored analogues, added 2026-07-30
    # (see cwg/examples_java.md).
    ("java", 11, 0, 0, 0, 0, 0), ("java", 12, 0, 0, 0, 0, 0), ("java", 13, 0, 0, 0, 0, 0),
    ("java", 14, 0, 0, 0, 0, 0), ("java", 15, 0, 0, 0, 0, 0), ("java", 16, 0, 0, 0, 0, 0),
    ("java", 17, 0, 0, 0, 0, 0), ("java", 18, 0, 0, 0, 0, 0),
    # examples_kotlin.md -- row 2 ("is Foo -> handle(foo)") used to be a documented outlier (label
    # NO, no mechanical feature fired) before nextTokenIsArrow existed; now arrow=1 catches it.
    ("kotlin", 1, 0, 0, 0, 0, 1), ("kotlin", 2, 0, 1, 0, 0, 0), ("kotlin", 3, 0, 0, 0, 0, 1),
    ("kotlin", 4, 1, 0, 1, 0, 0), ("kotlin", 5, 0, 0, 0, 0, 1), ("kotlin", 6, 0, 0, 1, 0, 0),
    ("kotlin", 7, 0, 0, 0, 0, 1), ("kotlin", 8, 1, 0, 1, 0, 0),
    # rows 9-12 added 2026-07-30, hand-authored analogues (see cwg/examples_kotlin.md).
    ("kotlin", 9, 0, 0, 0, 0, 0), ("kotlin", 10, 0, 0, 0, 0, 0),
    ("kotlin", 11, 0, 0, 0, 0, 0), ("kotlin", 12, 0, 0, 0, 0, 0),
]

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
    # weights: [bias, w_paren, w_arrow, w_semicolon, w_url_or_number]
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
        # L2 penalty gradient is lambda * w for each regularized weight (skip index 0, the bias).
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
    print("is not derived from this dataset -- see cwg/weights.md 'Main path'.")
    print("====================================================================================================")
    print()


if __name__ == "__main__": report(train())
