/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.classifier;

/** Entry point for the comment-normalization classifier: {@code (feature vector) -> YES/NO/
 *  ABSTAIN}, nothing else, per STATE_COMMENT_GRAMMAR.md's hard architectural constraint. Callers
 *  (the two {@code MiscRule} funnel points, once wired -- see that file's "Suggested order" step
 *  2) must treat {@link CommentDecision#ABSTAIN} exactly as {@code normalize-comment-*}
 *  {@code off} for that one comment. Not yet wired into {@code MiscRule}. */
public final class CommentClassifier {

    private CommentClassifier() {
    }

    public static CommentDecision classify(final CommentFeatureVector features) {
        // Gate 1 (RDD_KEY_95): non-Latin script anywhere defeats language-ID entirely.
        if (features.hasNonLatinScript) {
            return CommentDecision.ABSTAIN;
        }
        // Gate 2 (RDD_KEY_96) stage 1: leading-keyword ambiguity. Stage 2
        // (KeywordAmbiguityGate.resolveAmbiguousKeyword) is not yet implemented -- it needs real
        // contextual-scoring weights, blocked on RDD_KEY_97/98 (see
        // STATE_COMMENT_GRAMMAR.md's checklist) -- so an unresolved ambiguity here falls straight
        // to ABSTAIN rather than calling the stub. Once stage 2 lands, this should call it instead
        // of hard-coding ABSTAIN.
        if (features.hasLeadingKeywordMatch) {
            return CommentDecision.ABSTAIN;
        }
        // score = w . x + bias: no per-feature weight vector exists yet (RDD_KEY_97 is blocked),
        // so score degenerates to the bias constant alone -- 0.0 today, meaning every comment
        // that clears both gates still sits exactly at CommentClassifierWeights.THRESHOLD (also
        // 0.0) and must resolve to ABSTAIN, never YES, per that class's own javadoc.
        final double score = CommentClassifierWeights.BIAS;
        if (score > CommentClassifierWeights.THRESHOLD) {
            return CommentDecision.YES;
        }
        return CommentDecision.ABSTAIN;
    }
}
