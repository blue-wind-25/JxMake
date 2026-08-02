/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier;

/**
 * Weight constants for {@link CommentClassifier}'s scoring formula ({@code score = w*x + bias},
 *  threshold compare). Derived per RDD_KEY_97 (frontier-model-assisted, offline, one-time) from
 *  the labeled example sets in {@code tools/classifier_weights/} -- see {@code tools/classifier_weights/weights.md} for the full derivation
 *  and per-example verification. Do not hand-tune or corpus-train these directly; extend the
 *  {@code tools/classifier_weights/} example sets and re-run {@code tools/classifier_weights/derive_weights.py}, per that file's "Extending
 *  this" section.
 */
public final class CommentClassifierWeights {

    // Main path (CommentClassifier.classify, no leading-keyword ambiguity): everything reaching
    // this point already cleared both gates, so a constant bias that always clears THRESHOLD
    // restores the pre-classifier deterministic "normalize" behavior for the non-ambiguous
    // majority case. See tools/classifier_weights/weights.md "Main path".
    public static final double BIAS      = 1.0;
    public static final double THRESHOLD = 0.0;

    // Keyword-ambiguity path (KeywordAmbiguityGate.resolveAmbiguousKeyword, stage 2). L2-regularized
    // logistic regression trained on all 522 labeled examples across
    // tools/classifier_weights/examples_{c,cpp,java,kotlin,js,ts}.md by tools/classifier_weights/derive_weights.py
    // (lambda=0.1, 5000 epochs, lr=0.5) -- see tools/classifier_weights/weights.md "Keyword-ambiguity path" for the
    // derivation and the per-example verification (407/522; mismatches are all the intentional asymmetric-risk
    // tradeoff -- a zero-signal keyword-led comment now resolves to NO/ABSTAIN by default, since
    // real code overwhelmingly uses that shape as a code reference rather than English prose, and
    // a false skip is zero-cost while a false positive is a visible bug; see tools/classifier_weights/weights.md and
    // STATE_AI.md's 2026-07-30, 2026-07-31, and 2026-08-01 sections for the full analysis and the
    // real fixture regressions/hand-labeled hard-case growth that motivated re-deriving these
    // constants). KEYWORD_WEIGHT_ARROW covers a when/match-branch shape (e.g. "is Foo ->
    // handle(foo)") the other three features can't see on their own.
    public static final double KEYWORD_BIAS                 = - 1.18218;
    public static final double KEYWORD_WEIGHT_PAREN         = - 2.17830;
    public static final double KEYWORD_WEIGHT_ARROW         = - 0.64725;
    public static final double KEYWORD_WEIGHT_SEMICOLON     = - 2.66553;
    public static final double KEYWORD_WEIGHT_URL_OR_NUMBER = - 0.03338;
    public static final double KEYWORD_THRESHOLD            = 0.0;

    private CommentClassifierWeights()
    {
    }

} // class CommentClassifierWeights
