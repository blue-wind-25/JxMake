package com.jxmake.formatter.classifier;

/** Feature vector extracted from a single comment for {@link CommentClassifier} scoring. Pure
 *  data -- extraction lives in {@link CommentFeatureExtractor}, scoring in
 *  {@link CommentClassifier}. See STATE_COMMENT_GRAMMAR.md's "Scope split" section for the
 *  feature list this is meant to hold. */
public final class CommentFeatureVector {

    // TODO(comment-grammar): previous word, next word, next-char-is-'(', contains semicolon,
    // contains URL/filename/number, parser-provided comment type, per-language keyword-match
    // flag, non-Latin-script presence flag, etc. Fields are placeholders until
    // CommentFeatureExtractor is implemented -- see STATE_COMMENT_GRAMMAR.md "Suggested order"
    // step 1.
}
