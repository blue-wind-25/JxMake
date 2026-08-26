/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.classifier;

import java.util.regex.Pattern;

/**
 * Detects a comment that reads as commented-out code -- a trailing {@code ;} (the comment's own
 * last non-whitespace character) combined with a second, independent code-shape signal
 * (assignment/call/increment-decrement/typed-declaration). Per STATE_AI.md's 2026-07-30
 * disagreement-sampling pass: a bare trailing {@code ;} alone is unsafe -- a ~8% false-positive
 * rate was measured against real English prose that happens to end a clause with a semicolon
 * (e.g. "...cannot be expressed in RFC 3339's 4-digit form;"). Requiring a second confirming
 * signal is the mitigation that finding called for; unlike the reverted leading-hyphen gate (see
 * STATE_AI.md's "Bug 2" 2026-07-30 note), this is deliberately a *combination* of two independent
 * signals, not a single common-in-prose one.
 *
 *  <p>Presence-based like {@link DecorativeSeparatorGate}, not scored: any one of the four
 *  code-shape sub-patterns firing anywhere in the comment, together with a trailing {@code ;}, is
 *  enough. Accepted false-skip risk (asymmetric-risk design used throughout this classifier): a
 *  false match only ever costs an unnecessary NO (skip normalization) on a rare prose line, never
 *  a wrong capitalize/normalize of real prose.
 */
public final class CommentedOutCodeGate {

    // Call-shape: an identifier (optionally dotted, e.g. "System.out.println") immediately
    // followed by "(" -- no intervening whitespace, since real English essentially never writes
    // "word(" with no space before the parenthetical, but a real call/invocation always does
    private static final Pattern CALL_SHAPE = Pattern.compile(
        "\\b[A-Za-z_]\\w*(?:\\.[A-Za-z_]\\w*)*\\("
    );

    // Assignment-shape: identifier (optionally array-indexed) followed by a bare "=" -- excludes
    // "==", "!=", "<=", ">=" via the lookbehind/lookahead so ordinary comparison-operator
    // discussion in prose doesn't false-positive
    private static final Pattern ASSIGNMENT_SHAPE = Pattern.compile(
        "\\b[A-Za-z_]\\w*(?:\\[[^\\]\\n]{0,40}\\])?\\s*(?<![=!<>])=(?!=)"
    );

    // Increment/decrement-shape: "++" or "--" directly adjacent to a word character on at least
    // one side (e.g. "blockNo++", "--count") -- narrower than a bare "--" so a prose em-dash-style
    // "--" surrounded by spaces on both sides doesn't match
    private static final Pattern INCREMENT_DECREMENT_SHAPE = Pattern.compile(
        "\\w(?:\\+\\+|--)|(?:\\+\\+|--)\\w"
    );

    // Typed-declaration-shape: a type-looking word (a known primitive/common type keyword, a
    // sized-integer alias like "uint16_t"/"int32_t", or a capitalized identifier like a class
    // name) followed by a plain identifier then "=" or ";" -- e.g. "Event event;",
    // "uint16_t ctr = 0;". Requires the *first* word to look like a type (not just any two bare
    // words) specifically to avoid matching ordinary two-word prose endings like "...4-digit
    // form;" (neither "4-digit" nor "form" is a type-looking word).
    private static final Pattern DECLARATION_SHAPE = Pattern.compile(
        "\\b(?:int|long|short|char|float|double|bool|boolean|void|auto|var|const|unsigned|signed" + "|size_t|u?int(?:8|16|32|64)_t|[A-Z][A-Za-z0-9_]*)\\s+[a-z_]\\w*\\s*[=;]"
    );

    private CommentedOutCodeGate()
    {
    }

    public static boolean looksLikeCommentedOutCode(final String commentText)
    {
        if( !endsWithSemicolon(commentText) ) return false;

        return CALL_SHAPE.matcher(commentText).find()
            || ASSIGNMENT_SHAPE.matcher(commentText).find()
            || INCREMENT_DECREMENT_SHAPE.matcher(commentText).find()
            || DECLARATION_SHAPE.matcher(commentText).find();
    }

    /**
     * Same trailing-whitespace-skip-then-check-last-char shape as
     * {@code LicenseBlockGate.endsWithSentenceEndingPunctuation}, but a different terminal
     * character set for a different question (code-shape vs. sentence-ending). Deliberately not
     * factored into a shared helper -- only these 2 occurrences codebase-wide (grepped
     * 2026-08-22), and neither Gate class has an existing shared-utility home to put one in;
     * introducing a new class for one shared loop line would be premature abstraction.
     */
    private static boolean endsWithSemicolon(final String commentText)
    {
        int i = commentText.length() - 1;
        while( i >= 0 && Character.isWhitespace( commentText.charAt(i) ) ) i--;

        return i >= 0 && commentText.charAt(i) == ';';
    }

} // class CommentedOutCodeGate
