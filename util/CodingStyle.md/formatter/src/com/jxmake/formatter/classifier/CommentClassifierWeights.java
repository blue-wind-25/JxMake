/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.classifier;

/** Weight constants for {@link CommentClassifier}'s scoring formula ({@code score = w*x + bias},
 *  threshold compare). Derived per RDD_KEY_97 (frontier-model-assisted, offline, one-time) from
 *  the labeled example sets in {@code cwg/} -- see {@code cwg/weights.md} for the full derivation
 *  and per-example verification. Do not hand-tune or corpus-train these directly; extend the
 *  {@code cwg/} example sets and re-run {@code cwg/derive_weights.py}, per that file's "Extending
 *  this" section. */
public final class CommentClassifierWeights {

    // Main path (CommentClassifier.classify, no leading-keyword ambiguity): everything reaching
    // this point already cleared both gates, so a constant bias that always clears THRESHOLD
    // restores the pre-classifier deterministic "normalize" behavior for the non-ambiguous
    // majority case. See cwg/weights.md "Main path".
    public static final double BIAS = 1.0;
    public static final double THRESHOLD = 0.0;

    // Keyword-ambiguity path (KeywordAmbiguityGate.resolveAmbiguousKeyword, stage 2). L2-regularized
    // logistic regression trained on all 40 labeled examples across
    // cwg/examples_{c,cpp,java,kotlin}.md by cwg/derive_weights.py (lambda=0.1, 5000 epochs,
    // lr=0.5) -- see cwg/weights.md "Keyword-ambiguity path" for the derivation and the
    // per-example verification (38/40, same two documented outliers as the prior hand-picked
    // values).
    public static final double KEYWORD_BIAS = 2.16510;
    public static final double KEYWORD_WEIGHT_PAREN = -3.73682;
    public static final double KEYWORD_WEIGHT_SEMICOLON = -4.68148;
    public static final double KEYWORD_WEIGHT_URL_OR_NUMBER = -2.52881;
    public static final double KEYWORD_THRESHOLD = 0.0;

    private CommentClassifierWeights() {
    }
}
