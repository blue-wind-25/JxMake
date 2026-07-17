/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.classifier;

/** Entry point for the comment-normalization classifier: {@code (feature vector) -> YES/NO/
 *  ABSTAIN}, nothing else, per STATE_COMMENT_GRAMMAR.md's hard architectural constraint. Callers
 *  (the two {@code MiscRuleCore} funnel points, once wired -- see that file's "Suggested order" step
 *  2) must treat {@link CommentDecision#ABSTAIN} exactly as {@code normalize-comment-*}
 *  {@code off} for that one comment. Not yet wired into {@code MiscRuleCore}. */
public final class CommentClassifier {

    private CommentClassifier() {
    }

    public static CommentDecision classify(final CommentFeatureVector features) {
        // Gate 1 (RDD_KEY_95): non-Latin script anywhere defeats language-ID entirely.
        if (features.hasNonLatinScript) {
            return CommentDecision.ABSTAIN;
        }
        // Gate 2 (RDD_KEY_96) stage 1: leading-keyword ambiguity. Stage 2
        // (KeywordAmbiguityGate.resolveAmbiguousKeyword) resolves it via its own scoring formula
        // -- see cwg/weights.md for the derivation. Per the hard architectural constraint, a
        // stage-2 "not prose" result is ABSTAIN, not NO (no NO-producing path exists yet).
        if (features.hasLeadingKeywordMatch) {
            return KeywordAmbiguityGate.resolveAmbiguousKeyword(features) ? CommentDecision.YES : CommentDecision.ABSTAIN;
        }
        // score = w . x + bias: everything reaching this point already cleared both gates, so
        // this is the non-ambiguous majority case -- BIAS/THRESHOLD are set (cwg/weights.md
        // "Main path") so this always resolves to YES, matching pre-classifier behavior.
        final double score = CommentClassifierWeights.BIAS;
        if (score > CommentClassifierWeights.THRESHOLD) {
            return CommentDecision.YES;
        }
        return CommentDecision.ABSTAIN;
    }
}
