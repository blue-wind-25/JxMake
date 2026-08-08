/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;
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
        final String      body,
        final boolean     normalizeStartCase,
        final boolean     normalizeEndPeriod,
        final Set<String> noCapitalizeWords
    )
    {
        return normalize(body, normalizeStartCase, normalizeEndPeriod, noCapitalizeWords, false);
    }

    /** Same as {@link #normalize(String, boolean, boolean, Set)}, plus {@code multiSentenceCase}. */
    static String normalize(
        final String      body,
        final boolean     normalizeStartCase,
        final boolean     normalizeEndPeriod,
        final Set<String> noCapitalizeWords,
        final boolean     multiSentenceCase
    )
    {
        String text = body;
        if(normalizeEndPeriod)  text = stripSoleTrailingPeriod(text);
        if(normalizeStartCase)  text = capitalizeFirstLetter(text, noCapitalizeWords);
        if(multiSentenceCase && normalizeStartCase) {
            final List<String> single = new ArrayList<>();
            single.add(text);
            capitalizeMultiSentence(single, noCapitalizeWords);
            text = single.get(0);
        }

        return text;
    }

    private static String capitalizeFirstLetter(
        final String      text,
        final Set<String> noCapitalizeWords
    )
    {
        int i = 0;
        while( i < text.length() && text.charAt(i) == ' ' ) ++i;
        if( i >= text.length() ) return text;

        int wordEnd = i;
        while( wordEnd < text.length() && Character.isLetter( text.charAt(wordEnd) ) ) ++wordEnd;
        if( noCapitalizeWords != null && noCapitalizeWords.contains(
            text.substring(i, wordEnd)
        ) ) return text;

        final char ch = text.charAt(i);
        if( !Character.isLetter(ch) || !Character.isLowerCase(ch) ) return text;

        return text.substring(0, i) + Character.toUpperCase(ch) + text.substring(i + 1);
    }

    /**
     * 2026-08-08 session: chain-groups consecutive standalone `#` comments the same way curly
     *  chains `//` (RDD_KEY_265/RDD_KEY_266) -- shared by all five languages that use this class'
     *  `#`-comment shape (yaml/toml -- data-formats job; makefile/bash/powershell -- tooling job).
     *  {@code bodies} is each comment's text *after* the leading `#`, in source order; {@code
     *  blankBeforeEach.get(k)} is true iff a blank line separated comment {@code k} from comment
     *  {@code k-1} (index 0's value is irrelevant -- a group always starts fresh at a sub-run's
     *  beginning). Within a chain (no blank line between consecutive comments), only the first
     *  comment's start is capitalized, and a sole trailing `.` is stripped only if it's the only
     *  `.` across the whole chain, from the chain's last comment only -- matching curly's
     *  `computeLineCommentGroups` shape. A singleton chain (length 1) reduces to the pre-existing
     *  per-comment-token behavior.
     */
    static List<String> normalizeChain(
        final List<String>  bodies,
        final List<Boolean> blankBeforeEach,
        final boolean       normalizeStartCase,
        final boolean       normalizeEndPeriod,
        final Set<String>   noCapitalizeWords
    )
    {
        return normalizeChain(
            bodies, blankBeforeEach, normalizeStartCase, normalizeEndPeriod, noCapitalizeWords, false
        );
    }

    /**
     * Same as {@link #normalizeChain(List, List, boolean, boolean, Set)}, plus {@code
     * multiSentenceCase} (the {@code normalize-comment-start-case-multiline} config key, default
     * off). When on, capitalizes sentence 2+ of a chain the same way {@link #capitalizeFirstLetter}
     * already handles sentence 1 -- this family has no classifier/GRU stack to reuse (per this
     * class' own doc comment, it's the deterministic ad hoc pattern), so the leading-word decision
     * for each detected sentence boundary reuses the same deterministic {@code noCapitalizeWords}
     * check as sentence 1, applied to a synthetic combined text joining the chain's bodies (the
     * same way {@code MiscRuleCore#capitalizeMultiSentence} joins a curly `//` group), then mapped
     * back onto each original per-line body rather than re-splitting the combined text.
     */
    static List<String> normalizeChain(
        final List<String>  bodies,
        final List<Boolean> blankBeforeEach,
        final boolean       normalizeStartCase,
        final boolean       normalizeEndPeriod,
        final Set<String>   noCapitalizeWords,
        final boolean       multiSentenceCase
    )
    {
        final List<String> out = new ArrayList<>();
        final int          n   = bodies.size();
              int          i   = 0;
        while(i < n) {
            int j = i;
            while( j + 1 < n && !blankBeforeEach.get(j + 1) ) ++j;
            final List<String> chain = new ArrayList<>( bodies.subList(i, j + 1) );
            if(normalizeEndPeriod) {
                int dotCount = 0;
                for(final String c : chain) for(
                    int k = 0; k < c.length(); ++k
                ) if( c.charAt(k) == '.' ) ++dotCount;
                if(dotCount == 1) {
                    final int lastIdx = chain.size() - 1;
                    chain.set( lastIdx, stripSoleTrailingPeriod( chain.get(lastIdx) ) );
                } // if
            } // if
            if( normalizeStartCase && !chain.isEmpty() ) chain.set(
                0, capitalizeFirstLetter( chain.get(0), noCapitalizeWords )
            );
            if( multiSentenceCase && normalizeStartCase ) capitalizeMultiSentence(chain, noCapitalizeWords);
            out.addAll(chain);
            i = j + 1;
        } // while

        return out;
    }

    /**
     * See {@link #normalizeChain(List, List, boolean, boolean, Set, boolean)}'s doc comment --
     * mutates {@code chain} in place, capitalizing sentence 2+ of the combined text back onto each
     * original element.
     */
    private static void capitalizeMultiSentence(
        final List<String>  chain,
        final Set<String>   noCapitalizeWords
    )
    {
        if( chain.isEmpty() ) return;
        final int[]         lineStart = new int[chain.size()];
        final StringBuilder combined  = new StringBuilder();
        for( int i = 0; i < chain.size(); ++i ) {
            if(i > 0) combined.append(' ');
            lineStart[i] = combined.length();
            combined.append( chain.get(i) );
        }
        final String combinedText = combined.toString();
        final java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("[.!?]\\s+([a-z])").matcher(combinedText);
        final java.util.Map<Integer, Character> capitalized = new java.util.HashMap<>();
        while( matcher.find() ) {
            final int letterPos = matcher.start(1);
            if(letterPos == 0) continue;
            if( !MiscRuleCore.isEligibleSentenceBoundary( combinedText, matcher.start(), letterPos ) ) {
                continue;
            }
            int end = letterPos;
            while( end < combinedText.length() && Character.isLetter( combinedText.charAt(end) ) ) ++end;
            if( noCapitalizeWords != null && noCapitalizeWords.contains(
                combinedText.substring(letterPos, end)
            ) ) continue;
            capitalized.put( letterPos, Character.toUpperCase( combinedText.charAt(letterPos) ) );
        } // while
        if( capitalized.isEmpty() ) return;

        for( int i = 0; i < chain.size(); ++i ) {
            final int     start = lineStart[i];
            final int     end   = start + chain.get(i).length();
            StringBuilder line  = null;
            for( final java.util.Map.Entry<Integer, Character> e : capitalized.entrySet() ) {
                final int pos = e.getKey();
                if( pos >= start && pos < end ) {
                    if(line == null) line = new StringBuilder( chain.get(i) );
                    line.setCharAt( pos - start, e.getValue() );
                }
            }
            if(line != null) chain.set( i, line.toString() );
        } // for
    }

    /**
     * Shared deferred-placeholder mechanism for character-level tokenizers (Bash, PowerShell) that
     *  cannot look ahead to know whether a standalone `#` comment starts a chain: each comment's raw
     *  body is buffered here behind a unique placeholder marker emitted into the pass-A output in the
     *  comment's place; once the whole file has been scanned, {@link #resolve} groups strictly
     *  line-consecutive entries into chains, normalizes each chain via {@link #normalizeChain}, and
     *  substitutes the final text back over every placeholder. Makefile's tokenizer is line-oriented
     *  and does simple lookahead instead -- it has no need for this mechanism. Originally implemented
     *  independently (and identically) in {@code BashSpecificRule} and {@code PowerShellSpecificRule}
     *  for RDD_KEY_267; extracted here since both languages' logic was the same modulo marker text.
     */
    static final class ChainCollector {

        private static final class Entry {

            final String placeholder;
            final String body;
            final int    lineNo;

            Entry(final String placeholder, final String body, final int lineNo)
            {
                this.placeholder = placeholder;
                this.body        = body;
                this.lineNo      = lineNo;
            }

        } // class Entry

        private final String      markerPrefix;
        private final String      markerSuffix;
        private final List<Entry> entries = new ArrayList<>();
        private       int         seq;

        /**
         * {@code markerPrefix}/{@code markerSuffix} bracket each generated placeholder -- caller picks
         *  text guaranteed not to collide with real source content (e.g. Bash wraps in {@code U+0007}).
         */
        ChainCollector(final String markerPrefix, final String markerSuffix)
        {
            this.markerPrefix = markerPrefix;
            this.markerSuffix = markerSuffix;
        }

        /** Allocates a new placeholder, records {@code body}/{@code lineNo} behind it, and returns the marker text */
        String defer(final String body, final int lineNo)
        {
            final String placeholder = markerPrefix + (seq ++) + markerSuffix;
            entries.add( new Entry(placeholder, body, lineNo) );

            return placeholder;
        }

        /**
         * Groups collected entries into strictly line-consecutive chains, normalizes each chain, and
         *  substitutes every placeholder in {@code transformed} with its final text. A no-op if no
         *  comment was deferred.
         */
        String resolve(
            final String      transformed,
            final boolean     normalizeStartCase,
            final boolean     normalizeEndPeriod,
            final Set<String> noCapitalizeWords
        )
        {
            return resolve(transformed, normalizeStartCase, normalizeEndPeriod, noCapitalizeWords, false);
        }

        /** Same as {@link #resolve(String, boolean, boolean, Set)}, plus {@code multiSentenceCase}. */
        String resolve(
            final String      transformed,
            final boolean     normalizeStartCase,
            final boolean     normalizeEndPeriod,
            final Set<String> noCapitalizeWords,
            final boolean     multiSentenceCase
        )
        {
            if( entries.isEmpty() ) return transformed;

            String out = transformed;
            int    i   = 0;
            while( i < entries.size() ) {
                int j = i;
                while( j + 1 < entries.size() && entries.get(
                    j + 1
                ).lineNo == entries.get(
                    j
                ).lineNo + 1 ) ++j;

                final List<String>  bodies = new ArrayList<>();
                final List<Boolean> blanks = new ArrayList<>();
                for(int k = i; k <= j; ++k) { bodies.add(
                    entries.get(k).body
                ); blanks.add(
                    false
                ); }
                final List<String> normalized = normalizeChain(
                    bodies, blanks, normalizeStartCase, normalizeEndPeriod, noCapitalizeWords, multiSentenceCase
                );
                for(int k = i; k <= j; ++k) out = out.replace(
                    entries.get(k).placeholder, normalized.get(k - i)
                );
                i = j + 1;
            } // while

            return out;
        }

    } // class ChainCollector

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
