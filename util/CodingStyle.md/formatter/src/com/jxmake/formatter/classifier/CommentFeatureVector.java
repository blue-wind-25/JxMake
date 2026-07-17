/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier;

import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

/** Feature vector extracted from a single comment for {@link CommentClassifier} scoring. Pure
 *  data -- extraction lives in {@link CommentFeatureExtractor}, scoring in
 *  {@link CommentClassifier}. See STATE_COMMENT_GRAMMAR.md's "Scope split" section for the
 *  feature list this is meant to hold.
 *
 *  <p>{@link #targetWord} is the word the classifier is being asked to make a decision about
 *  (currently always the comment's leading word, since the two funnel points that will call
 *  this -- {@code capitalizeFirstLetter} and {@code stripSoleTrailingPeriod*} -- both hinge on
 *  a leading-word ambiguity; {@link #previousWord} and {@link #nextWord} are its within-comment
 *  neighbors, not surrounding code tokens, per the hard architectural constraint that the
 *  classifier only ever sees comment text). */
public final class CommentFeatureVector {

    public final String targetWord;
    public final String previousWord;
    public final String nextWord;
    public final boolean nextCharIsOpenParen;
    public final boolean nextTokenIsArrow;
    public final boolean containsSemicolon;
    public final boolean containsUrlOrFilenameOrNumber;
    public final TokenType commentType;
    public final boolean hasNonLatinScript;
    public final boolean hasLeadingKeywordMatch;

    public CommentFeatureVector(final String targetWord, final String previousWord, final String nextWord,
            final boolean nextCharIsOpenParen, final boolean nextTokenIsArrow, final boolean containsSemicolon,
            final boolean containsUrlOrFilenameOrNumber, final TokenType commentType,
            final boolean hasNonLatinScript, final boolean hasLeadingKeywordMatch) {
        this.targetWord = targetWord;
        this.previousWord = previousWord;
        this.nextWord = nextWord;
        this.nextCharIsOpenParen = nextCharIsOpenParen;
        this.nextTokenIsArrow = nextTokenIsArrow;
        this.containsSemicolon = containsSemicolon;
        this.containsUrlOrFilenameOrNumber = containsUrlOrFilenameOrNumber;
        this.commentType = commentType;
        this.hasNonLatinScript = hasNonLatinScript;
        this.hasLeadingKeywordMatch = hasLeadingKeywordMatch;
    }
}
