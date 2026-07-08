package com.jxmake.formatter.classifier;

import com.jxmake.formatter.Lang;

/** RDD_KEY_96: per-language keyword lists (no shared list across C/C++/Java/Kotlin) + two-stage
 *  check -- a cheap membership test first, then contextual scoring only on actual keyword
 *  matches. Keeps the common case (no keyword present) O(1)-ish and avoids scoring cost on
 *  comments that can't be ambiguous in the first place. */
public final class KeywordAmbiguityGate {

    private KeywordAmbiguityGate() {
    }

    // TODO(comment-grammar): stage 1 -- cheap per-language keyword membership test. Returns true
    // iff the comment's leading word is a keyword in lang's own list (see RDD_KEY_96; lists must
    // stay separate per language, mirroring MiscRule.isCommentNoCapitalizeWord's precedent).
    public static boolean hasLeadingKeywordMatch(final String commentText, final Lang lang) {
        throw new UnsupportedOperationException("KeywordAmbiguityGate stage 1 not yet implemented");
    }

    // TODO(comment-grammar): stage 2 -- contextual scoring, only invoked when stage 1 returns
    // true. Resolves the ambiguity a bare keyword-membership test can't (e.g. "static" as an
    // English adjective vs. the language keyword).
    public static boolean resolveAmbiguousKeyword(final CommentFeatureVector features) {
        throw new UnsupportedOperationException("KeywordAmbiguityGate stage 2 not yet implemented");
    }
}
