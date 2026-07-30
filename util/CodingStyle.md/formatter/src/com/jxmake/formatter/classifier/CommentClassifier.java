/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier;

/**
 * Entry point for the comment-normalization classifier: {@code (feature vector) -> YES/NO/
 *  ABSTAIN}, nothing else, per STATE_COMMENT_GRAMMAR.md's hard architectural constraint. Callers
 *  (the two {@code MiscRuleCore} funnel points, once wired -- see that file's "Suggested order" step
 *  2) must treat {@link CommentDecision#ABSTAIN} exactly as {@code normalize-comment-*}
 *  {@code off} for that one comment. Not yet wired into {@code MiscRuleCore}.
 */
public final class CommentClassifier {

    private CommentClassifier()
    {
    }

    public static CommentDecision classify(final CommentFeatureVector features)
    {
        // Gate 1 (RDD_KEY_95): non-Latin script anywhere defeats language-ID entirely
        if(features.hasNonLatinScript) return CommentDecision.ABSTAIN;
        // Gate 1b: decorative/symbol-only separator (e.g. "****...****", "#####...#####") -- no
        // letter or digit anywhere, so it cannot be prose by construction. First real NO-producing
        // path in this method (previously every reachable comment resolved YES or ABSTAIN -- see
        // RDD_KEY_96's note and STATE_AI.md's "why the GRU only ever returns YES/ABSTAIN" finding).
        if(features.isDecorativeOnly) return CommentDecision.NO;
        // Gate 1c (found 2026-07-30 self-formatting dogfood): leading word immediately followed by
        // a slash (no whitespace) signals a slash-separated list of code tokens/identifiers (e.g.
        // "sizeTokens/initTokens get flattened...", "open/final/abstract/sealed share one
        // column...", "wx/uh/az/ar/ah are short-lived...") -- a code reference, not prose, even
        // when the leading word isn't a language keyword and so never reaches Gate 2 below. Real
        // English "a/b" constructs (and/or, km/h) are rare enough at a comment's very start that
        // the accepted cost is a false skip (zero-cost per the asymmetric-risk design), never a
        // wrong capitalize.
        if(features.leadingWordFollowedBySlash) return CommentDecision.NO;
        // Gate 1d (found 2026-07-30 disagreement-sampling pass, item 6 in STATE_AI.md's "further
        // NO-producing gates" TODO): a comment ending in ";" combined with a second, independent
        // code-shape signal (assignment/call/increment-decrement/typed-declaration) reads as
        // commented-out code, not prose -- e.g. "fmap[j] = a;", "blockNo++;",
        // "System.out.println(...);", "uint16_t ctr = 0;", "Event event;". A bare trailing ";"
        // alone was measured unsafe (~8% false-positive rate on real prose that ends a clause with
        // a semicolon) -- see CommentedOutCodeGate's javadoc for why the second signal is required.
        if(features.looksLikeCommentedOutCode) return CommentDecision.NO;
        // Gate 1e (tracker item 22 / former "further NO-producing gates" item 2): a comment
        // spanning 2+ newlines, whose last non-whitespace character is not sentence-ending
        // punctuation, combined with a second, independent license/copyright-specific signal
        // (named vocabulary like "Copyright"/"SPDX-License-Identifier"/"Licensed under") reads
        // as a multi-line license/copyright block, not prose --
        // formalizes tools/gru/README.txt's Pool-B hand-labeling shortcut ("spans 2+ newlines, a
        // license block not a single sentence -> NO") into a real two-signal gate so genuine
        // multi-line prose paragraphs (which almost always end in terminal punctuation) don't
        // misfire. See LicenseBlockGate's javadoc for the full rationale.
        if(features.looksLikeLicenseBlock) return CommentDecision.NO;
        // Gate 2 (RDD_KEY_96) stage 1: leading-keyword ambiguity. Stage 2
        // (KeywordAmbiguityGate.resolveAmbiguousKeyword) resolves it via its own scoring formula
        // -- see tools/classifier_weights/weights.md for the derivation. Per the hard architectural constraint, a
        // stage-2 "not prose" result is ABSTAIN, not NO (no NO-producing path exists yet).
        if(features.hasLeadingKeywordMatch) return KeywordAmbiguityGate.resolveAmbiguousKeyword(
            features
        ) ? CommentDecision.YES : CommentDecision.ABSTAIN;
        // Score = w . x + bias: everything reaching this point already cleared both gates, so
        // this is the non-ambiguous majority case -- BIAS/THRESHOLD are set (tools/classifier_weights/weights.md
        // "Main path") so this always resolves to YES, matching pre-classifier behavior.
        final double score = CommentClassifierWeights.BIAS;
        if(score > CommentClassifierWeights.THRESHOLD) return CommentDecision.YES;

        return CommentDecision.ABSTAIN;
    }

} // class CommentClassifier
