/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.Set;

/**
 * Shared ad hoc `#`-comment normalization for Makefile/Bash/PowerShell (STYLE_TOOLING.md §0,
 * RDD_KEY_261): optional first-letter capitalization (gated by {@code normalize-comment-start-case},
 * skipped for a leading word found in {@code noCapitalizeWords}) and optional sole-trailing-period
 * stripping (gated by {@code normalize-comment-end-period}). No classifier/GRU dependency -- this is
 * the "TOML-style ad hoc pattern" RDD_KEY_260 reserved for these three languages, not the curly-only
 * comment-classifier pipeline. {@code body} is the comment text *after* the leading `#` (and, for
 * PowerShell block comments, not applicable -- only line comments use this).
 */
final class ToolingCommentNormalizer {

    private ToolingCommentNormalizer()
    {
    }

    static String normalize(
        final String body, final boolean normalizeStartCase, final boolean normalizeEndPeriod,
        final Set<String> noCapitalizeWords
    )
    {
        String text = body;
        if(normalizeEndPeriod)  text = stripSoleTrailingPeriod(text);
        if(normalizeStartCase)  text = capitalizeFirstLetter(text, noCapitalizeWords);

        return text;
    }

    private static String capitalizeFirstLetter(final String text, final Set<String> noCapitalizeWords)
    {
        int i = 0;
        while( i < text.length() && text.charAt(i) == ' ' ) ++i;
        if( i >= text.length() ) return text;

        int wordEnd = i;
        while( wordEnd < text.length() && Character.isLetter( text.charAt(wordEnd) ) ) ++wordEnd;
        if( noCapitalizeWords != null && noCapitalizeWords.contains( text.substring(i, wordEnd) ) ) return text;

        final char ch = text.charAt(i);
        if( !Character.isLetter(ch) || !Character.isLowerCase(ch) ) return text;

        return text.substring(0, i) + Character.toUpperCase(ch) + text.substring(i + 1);
    }

    /**
     * Strips the trailing `.` only when it is the sole `.` in {@code text} -- an ellipsis (`...`) is
     *  left alone for free. Package-private (not {@code private}) so the data-format rule classes
     *  (TOML/YAML, whose `#`-comment shape matches Makefile/Bash/PowerShell's) can reuse it directly
     *  for {@code normalize-comment-end-period} instead of duplicating the same logic.
     */
    static String stripSoleTrailingPeriod(final String text)
    {
        int end = text.length();
        while( end > 0 && Character.isWhitespace( text.charAt(end - 1) ) ) --end;
        if( end == 0 || text.charAt(end - 1) != '.' ) return text;

        int dotCount = 0;
        for( int i = 0; i < text.length(); ++i ) if( text.charAt(i) == '.' ) ++dotCount;
        if(dotCount != 1) return text;

        int trimEnd = end - 1;
        while( trimEnd > 0 && Character.isWhitespace( text.charAt(trimEnd - 1) ) ) --trimEnd;

        return text.substring(0, trimEnd) + text.substring(end);
    }

} // class ToolingCommentNormalizer
