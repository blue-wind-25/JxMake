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

    // TODO(comment-grammar): apply the two gates (NonLatinScriptGate, KeywordAmbiguityGate) in
    // order, then score = w . x + bias via CommentClassifierWeights, threshold-compare into a
    // CommentDecision. With all-zero placeholder weights this must always return ABSTAIN -- see
    // CommentClassifierWeights' javadoc for why that's the safe default.
    public static CommentDecision classify(final CommentFeatureVector features) {
        throw new UnsupportedOperationException("CommentClassifier not yet implemented");
    }
}
