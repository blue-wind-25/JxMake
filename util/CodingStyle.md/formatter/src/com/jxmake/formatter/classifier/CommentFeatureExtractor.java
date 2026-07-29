/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.classifier;

import java.util.regex.Pattern;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

/** Builds a {@link CommentFeatureVector} from raw comment text. Pure function, no formatter
 *  mutation -- see STATE_COMMENT_GRAMMAR.md's hard architectural constraint. Not yet wired into
 *  {@code MiscRuleCore}; implementation is step 1 of that file's "Handoff note" suggested order. */
public final class CommentFeatureExtractor {

    // Deliberately permissive -- a false-positive URL/filename/number match only ever costs a
    // classifier ABSTAIN (per the hard constraint's "never guess" rule), never a wrong YES.
    private static final Pattern URL_OR_FILENAME_OR_NUMBER = Pattern.compile(
            "https?://\\S+|\\b\\w+\\.[A-Za-z]{1,4}\\b|\\d+");

    private CommentFeatureExtractor() {
    }

    public static CommentFeatureVector extract(final String commentText, final Lang lang) {
        return extract(commentText, lang, TokenType.COMMENT_LINE);
    }

    public static CommentFeatureVector extract(final String commentText, final Lang lang, final TokenType commentType) {
        return extract(commentText, lang, commentType, 0);
    }

    /** Same as {@link #extract(String, Lang, TokenType)}, but {@code targetWordIndex} (per
     *  {@link com.jxmake.formatter.classifier.gru.GruClassifier#tokenize}) scopes
     *  {@code hasLeadingKeywordMatch} to only fire when the decision is actually about the
     *  leading word (index 0) -- e.g. {@code MiscRuleCore}'s strip-trailing-period call site
     *  passes the *last* token's index, since the ambiguous trailing dot has nothing to do with
     *  whatever keyword the comment happens to start with. Without this, a comment like
     *  "return true if ... type ." would abstain from stripping its stray trailing period just
     *  because it starts with the keyword "return" -- a real regression found 2026-07-30 fixing
     *  `test/real_code_regressions_54_inp.java`, see STATE_AI.md's 2026-07-30 section. */
    public static CommentFeatureVector extract(final String commentText, final Lang lang,
            final TokenType commentType, final int targetWordIndex) {
        final int[] targetBounds = leadingWordBounds(commentText);
        final String targetWord = commentText.substring(targetBounds[0], targetBounds[1]);
        final String previousWord = ""; // targetWord is always the comment's leading word (see class javadoc)
        final String nextWord = nextWord(commentText, targetBounds[1]);
        final boolean nextCharIsOpenParen = nextNonWhitespaceCharIs(commentText, targetBounds[1], '(');
        // Deliberately scans the whole tail after the target word, not just the immediate next
        // token -- catches a when/match-branch shape like "is Foo -> handle(foo)" where the
        // arrow follows an intervening identifier, not the target word directly. Same permissive
        // philosophy as URL_OR_FILENAME_OR_NUMBER: a false-positive arrow match only ever costs
        // an ABSTAIN, never a wrong YES.
        final boolean nextTokenIsArrow = commentText.indexOf("->", targetBounds[1]) >= 0;
        final boolean containsSemicolon = commentText.indexOf(';') >= 0;
        final boolean containsUrlOrFilenameOrNumber = URL_OR_FILENAME_OR_NUMBER.matcher(commentText).find();
        final boolean hasNonLatinScript = NonLatinScriptGate.containsNonLatinScript(commentText);
        final boolean hasLeadingKeywordMatch = targetWordIndex == 0
                && KeywordAmbiguityGate.hasLeadingKeywordMatch(commentText, lang);
        final boolean isDecorativeOnly = DecorativeSeparatorGate.isDecorativeOnly(commentText);
        return new CommentFeatureVector(targetWord, previousWord, nextWord, nextCharIsOpenParen,
                nextTokenIsArrow, containsSemicolon, containsUrlOrFilenameOrNumber, commentType,
                hasNonLatinScript, hasLeadingKeywordMatch, isDecorativeOnly);
    }

    /** [start, end) of the first contiguous run of letters/digits/underscore after skipping
     *  leading whitespace -- same extraction MiscRuleCore.capitalizeFirstLetter and
     *  KeywordAmbiguityGate.hasLeadingKeywordMatch use, kept in sync by construction here since
     *  both callers now route through this. */
    private static int[] leadingWordBounds(final String commentText) {
        int start = 0;
        while (start < commentText.length() && Character.isWhitespace(commentText.charAt(start))) {
            start++;
        }
        int end = start;
        while (end < commentText.length()
                && (Character.isLetterOrDigit(commentText.charAt(end)) || commentText.charAt(end) == '_')) {
            end++;
        }
        return new int[] {start, end};
    }

    /** The next contiguous word (letters/digits/underscore) starting at or after {@code from},
     *  skipping any non-word characters in between, or "" if there is none. */
    private static String nextWord(final String commentText, final int from) {
        int start = from;
        while (start < commentText.length()
                && !Character.isLetterOrDigit(commentText.charAt(start)) && commentText.charAt(start) != '_') {
            start++;
        }
        int end = start;
        while (end < commentText.length()
                && (Character.isLetterOrDigit(commentText.charAt(end)) || commentText.charAt(end) == '_')) {
            end++;
        }
        return commentText.substring(start, end);
    }

    private static boolean nextNonWhitespaceCharIs(final String commentText, final int from, final char c) {
        int i = from;
        while (i < commentText.length() && Character.isWhitespace(commentText.charAt(i))) {
            i++;
        }
        return i < commentText.length() && commentText.charAt(i) == c;
    }
}
