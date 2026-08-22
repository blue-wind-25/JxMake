/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.classifier;

import java.util.regex.Pattern;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

/**
 * Builds a {@link CommentFeatureVector} from raw comment text. Pure function, no formatter
 * mutation -- see STATE_COMMENT_GRAMMAR.md's hard architectural constraint. Not yet wired into
 * {@code MiscRuleCore}; implementation is step 1 of that file's "Handoff note" suggested order.
 */
public final class CommentFeatureExtractor {

    // Deliberately permissive -- a false-positive URL/filename/number match only ever costs a
    // classifier ABSTAIN (per the hard constraint's "never guess" rule), never a wrong YES
    private static final Pattern URL_OR_FILENAME_OR_NUMBER = Pattern.compile(
        "https?://\\S+|\\b\\w+\\.[A-Za-z]{1,4}\\b|\\d+"
    );

    private CommentFeatureExtractor()
    {
    }

    public static CommentFeatureVector extract(final String commentText, final Lang lang)
    {
        return extract(commentText, lang, TokenType.COMMENT_LINE);
    }

    public static CommentFeatureVector extract(
        final String    commentText,
        final Lang      lang,
        final TokenType commentType
    )
    {
        return extract(commentText, lang, commentType, 0);
    }

    /**
     * Same as {@link #extract(String, Lang, TokenType)}, but {@code targetWordIndex} (per
     * {@link com.jxmake.formatter.classifier.gru.GruClassifier#tokenize}) scopes
     * {@code hasLeadingKeywordMatch} to only fire when the decision is actually about the
     * leading word (index 0) -- e.g. {@code MiscRuleCore}'s strip-trailing-period call site
     * passes the *last* token's index, since the ambiguous trailing dot has nothing to do with
     * whatever keyword the comment happens to start with. Without this, a comment like
     * "return true if ... type ." would abstain from stripping its stray trailing period just
     * because it starts with the keyword "return" -- a real regression found 2026-07-30 fixing
     * `test/real_code_regressions_54_inp.java`, see STATE_AI.md's 2026-07-30 section.
     */
    public static CommentFeatureVector extract(
        final String    commentText,
        final Lang      lang,
        final TokenType commentType,
        final int       targetWordIndex
    )
    {
        final int[]   targetBounds        = leadingWordBounds(commentText);
        final String  targetWord          = commentText.substring(
            targetBounds[0], targetBounds[1]
        );
        final String  previousWord        = "";                                                         // TargetWord is always the comment's leading word (see class javadoc)
        final String  nextWord            = nextWord( commentText, targetBounds[1] );
        final boolean nextCharIsOpenParen = nextNonWhitespaceCharIs(
            commentText, targetBounds[1], '('
        );
        // Deliberately scans the whole tail after the target word, not just the immediate next
        // token -- catches a when/match-branch shape like "is Foo -> handle(foo)" where the
        // arrow follows an intervening identifier, not the target word directly. Same permissive
        // philosophy as URL_OR_FILENAME_OR_NUMBER: a false-positive arrow match only ever costs
        // an ABSTAIN, never a wrong YES.
        final boolean nextTokenIsArrow              = commentText.indexOf(
            "->", targetBounds[1]
        ) >= 0;
        final boolean containsSemicolon             = commentText.indexOf(';') >= 0;
        final boolean containsUrlOrFilenameOrNumber = URL_OR_FILENAME_OR_NUMBER.matcher(
            commentText
        ).find();
        final boolean hasNonLatinScript             = NonLatinScriptGate.containsNonLatinScript(
            commentText
        );
        final boolean hasLeadingKeywordMatch        = targetWordIndex == 0 && KeywordAmbiguityGate.hasLeadingKeywordMatch(
            targetWord, lang
        );
        final boolean isDecorativeOnly              = DecorativeSeparatorGate.isDecorativeOnly(
            commentText
        );
        // TargetWordIndex == 0 scoped, same as hasLeadingKeywordMatch above -- this signals a
        // slash-separated list of code tokens/identifiers immediately after the leading word (e.g.
        // "sizeTokens/initTokens get flattened...", "open/final/abstract/sealed share one
        // column..."), not a keyword-membership match, so it must fire independent of
        // hasLeadingKeywordMatch to catch non-keyword identifiers too (found 2026-07-30 dogfooding
        // the formatter on its own source -- see STATE_AI.md's 2026-07-30 section).
        final boolean leadingWordFollowedBySlash = targetWordIndex == 0 && targetBounds[1] < commentText.length() && commentText.charAt(
            targetBounds[1]
        ) == '/';
        // Same rationale/scoping as leadingWordFollowedBySlash above, but for a call/reference
        // shape instead of a slash-list -- an identifier immediately followed by "(" with no
        // intervening whitespace (e.g. "getName() defaults...", "toString() returns...") is a
        // code reference, not prose, even when the leading word isn't a language keyword and so
        // never reaches Gate 2's KeywordAmbiguityGate scoring (which already independently weighs
        // nextCharIsOpenParen, but only for keyword leading words). Found 2026-08-10 self-
        // formatting dogfood: a retrained GRU classifier capitalized "getName()" to "GetName()" in
        // a real comment, which no existing mechanical gate caught. Mirrors CommentedOutCodeGate's
        // CALL_SHAPE precedent (identifier immediately followed by "(").
        final boolean leadingWordFollowedByParen = targetWordIndex == 0 && targetBounds[1] < commentText.length() && commentText.charAt(
            targetBounds[1]
        ) == '(';
        // Not targetWordIndex-scoped -- unlike hasLeadingKeywordMatch/leadingWordFollowedBySlash,
        // this is a whole-comment shape signal (trailing ";" plus a second code-shape signal
        // anywhere in the text), independent of which token the decision hinges on. See
        // CommentedOutCodeGate's javadoc and STATE_AI.md's 2026-07-30 disagreement-sampling
        // findings for the two-signal-required rationale.
        final boolean looksLikeCommentedOutCode = CommentedOutCodeGate.looksLikeCommentedOutCode(
            commentText
        );
        // Not targetWordIndex-scoped, same as looksLikeCommentedOutCode above -- a multi-line
        // license/copyright block is a whole-comment shape signal (newline count + terminal
        // punctuation + license vocabulary/border line), independent of which token the decision
        // hinges on. See LicenseBlockGate's javadoc and STATE_AI.md's "further NO-producing
        // gates" TODO for the design rationale.
        final boolean looksLikeLicenseBlock = LicenseBlockGate.looksLikeLicenseBlock(commentText);

        return new CommentFeatureVector(
            targetWord, previousWord, nextWord, nextCharIsOpenParen,
            nextTokenIsArrow, containsSemicolon, containsUrlOrFilenameOrNumber, commentType,
            hasNonLatinScript, hasLeadingKeywordMatch, isDecorativeOnly, leadingWordFollowedBySlash,
            leadingWordFollowedByParen, looksLikeCommentedOutCode, looksLikeLicenseBlock
        );
    }

    /**
     * [start, end) of the first contiguous run of letters/digits/underscore after skipping
     * leading whitespace -- same extraction MiscRuleCore.capitalizeFirstLetter uses. The
     * resulting {@code targetWord} is passed straight into
     * {@link KeywordAmbiguityGate#hasLeadingKeywordMatch(String, Lang)} rather than that method
     * re-extracting it from {@code commentText} itself.
     */
    private static int[] leadingWordBounds(final String commentText)
    {
        int start = 0;
        while( start < commentText.length() && Character.isWhitespace(
            commentText.charAt(start)
        ) ) start++;
        int end = start;
        while( end < commentText.length() && ( Character.isLetterOrDigit(
            commentText.charAt(end)
        ) || commentText.charAt(
            end
        ) == '_' ) ) end++;

        return new int[] {start, end};
    }

    /**
     * The next contiguous word (letters/digits/underscore) starting at or after {@code from},
     * skipping any non-word characters in between, or "" if there is none
     */
    private static String nextWord(final String commentText, final int from)
    {
        int start = from;
        while( start < commentText.length() && !Character.isLetterOrDigit(
            commentText.charAt(start)
        ) && commentText.charAt(
            start
        ) != '_' ) start++;
        int end = start;
        while( end < commentText.length() && ( Character.isLetterOrDigit(
            commentText.charAt(end)
        ) || commentText.charAt(
            end
        ) == '_' ) ) end++;

        return commentText.substring(start, end);
    }

    private static boolean nextNonWhitespaceCharIs(
        final String commentText,
        final int    from,
        final char   c
    )
    {
        int i = from;
        while( i < commentText.length() && Character.isWhitespace( commentText.charAt(i) ) ) i++;

        return i < commentText.length() && commentText.charAt(i) == c;
    }

} // class CommentFeatureExtractor
