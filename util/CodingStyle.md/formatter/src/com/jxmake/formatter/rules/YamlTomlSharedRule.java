/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import com.jxmake.formatter.FormatterSimpleBraced;

/**
 * Shared helper logic that was structurally identical between {@link YamlSpecificRule} and
 * {@link TomlSpecificRule} (flagged in STATE_DATA_FORMATS.md as a possible future DRY
 * improvement, now factored here): `#`-comment same-line splitting, the `#`-prefixed
 * trailing-comment start-case/end-period normalization, and the `=`/`:`-alignment group-padding
 * computation shared by both rules' {@code renderItems}. Pure pull-up -- no behavior change.
 * {@code repeatChar}/{@code indent} (2026-08-16 cleanup-pass consolidation) are thin pass-throughs
 * to {@link com.jxmake.formatter.FormatterSimpleBraced}, kept here too so YAML/TOML's own
 * {@code indent(int)} wrappers have a same-package delegate to call, same as before.
 */
final class YamlTomlSharedRule {

    private YamlTomlSharedRule()
    {
    }

    static String repeatChar(final char c, final int count)
    {
        return FormatterSimpleBraced.repeatChar(c, count);
    }

    /** Thin pass-through to {@link FormatterSimpleBraced#indent} -- same 2026-08-16 consolidation */
    static String indent(final int depth, final String indentUnit)
    {
        return FormatterSimpleBraced.indent(depth, indentUnit);
    }

    static String rtrim(final String s)
    {
        int end = s.length();
        while( end > 0 && Character.isWhitespace( s.charAt(end - 1) ) ) end--;

        return s.substring(0, end);
    }

    /**
     * Splits off a same-line trailing `#` comment from {@code s}, respecting quotes -- a `#` only
     *  starts a comment when it is at the start of the string or preceded by whitespace. Returns a
     *  two-element array: [codePart (right-trimmed), commentPartOrNull].
     */
    static String[] splitTrailingComment(final String s)
    {
        boolean inSingle = false;
        boolean inDouble = false;
        for( int i = 0; i < s.length(); ++i ) {
            final char ch = s.charAt(i);
            if(inSingle) {
                if(ch == '\'') inSingle = false;
                continue;
            }
            if(inDouble) {
                     if(ch == '\\') i++;
                else if(ch == '"')  inDouble = false;
                continue;
            }
                 if(ch == '\'') inSingle = true;
            else if(ch == '"') inDouble = true;
            else if( ch == '#' && ( i == 0 || s.charAt(
                i - 1
            ) == ' ' || s.charAt(
                i - 1
            ) == '\t' ) ) {
                return new String[] {rtrim( s.substring(0, i) ), s.substring(i)};
            }
        } // for

        return new String[] {rtrim(s), null};
    }

    /**
     * Normalizes a same-line trailing `#...` comment: optional sole-trailing-period stripping,
     *  then optional first-letter capitalization (skipping a already-uppercase/non-letter leading
     *  character) -- identical logic previously duplicated verbatim in {@code YamlSpecificRule}
     *  and {@code TomlSpecificRule}'s own private {@code normComment}.
     */
    static String normComment(
        final String  commentText,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod
    )
    {
        String text = normalizeCommentEndPeriod ? ToolingCommentNormalizer.stripSoleTrailingPeriod(
            commentText
        ) : commentText;
        if(!normalizeCommentStartCase) return text;
        int i = 1;
        while( i < text.length() && text.charAt(i) == ' ' ) i++;
        if( i < text.length() ) {
            final char ch = text.charAt(i);
            if( Character.isLetter(
                ch
            ) && Character.isLowerCase(
                ch
            ) ) return text.substring(
                0, i
            ) + Character.toUpperCase(
                ch
            ) + text.substring(
                i + 1
            );
        } // if

        return text;
    }

    /**
     * Computes per-item `=`/`:`-alignment padding for a run of {@code size} items, grouping
     *  adjacent keyed items with no leading comment/blank-line break between them into independent
     *  alignment groups (same shape as JSON's §1.1 grouping) -- identical loop previously
     *  duplicated verbatim in {@code YamlSpecificRule}/{@code TomlSpecificRule}'s own
     *  {@code renderItems}. Indexed via predicates/accessor rather than a shared item type, since
     *  the two callers' own {@code Item} classes are unrelated (YAML's carries sequence/mapping
     *  fields TOML has no use for, and vice versa).
     */
    /**
     * Terminal-condition callback for {@link #scanQuoteAwareBracket} -- invoked once per character
     *  not consumed by quote/bracket handling, after any bracket-depth delta for that character has
     *  already been applied to {@code depth}. Return {@code true} to stop the scan at index
     *  {@code i}.
     */
    interface BracketScanStop {

        boolean shouldStopAt(String s, int i, char ch, int depth);

    } // interface BracketScanStop

    /**
     * Result of {@link #scanQuoteAwareBracket}: {@code stopIndex} is the index of the first
     *  character {@code stop} matched, or {@code -1} if it never matched (including when
     *  {@code stop} was {@code null}); {@code finalDepth} is the net bracket depth at the end of
     *  the scan (meaningful for a caller like {@code bracketBalance} that scans with no terminal
     *  condition -- {@code stopIndex} is always {@code -1} in that case). Plain result-holder, not
     *  a packed/encoded int, to avoid any sentinel-collision risk when depth can legitimately go
     *  negative (e.g. a lone {@code ]} continuation line).
     */
    static final class BracketScanResult {

        final int stopIndex;
        final int finalDepth;

        BracketScanResult(final int stopIndex, final int finalDepth)
        {
            this.stopIndex  = stopIndex;
            this.finalDepth = finalDepth;
        }

    } // class BracketScanResult

    /**
     * Shared core of the quote-aware ({@code '`/`"`-respecting, backslash-escape-aware inside
     *  double quotes only) `{}`/`[]`-depth scanner previously duplicated (with a different terminal
     *  condition each time) as {@code TomlSpecificRule.bracketBalance}/{@code findAssignmentEquals}
     *  and {@code YamlSpecificRule.findMappingColon}. {@code stop} is consulted for every character
     *  not itself consumed as a quote/bracket character; pass {@code null} for a scan with no
     *  terminal condition (i.e. one that always runs to the end of {@code s}).
     */
    static BracketScanResult scanQuoteAwareBracket(final String s, final BracketScanStop stop)
    {
        boolean inSingle = false;
        boolean inDouble = false;
        int     depth    = 0;
        for( int i = 0; i < s.length(); ++i ) {
            final char ch = s.charAt(i);
            if(inSingle) {
                if(ch == '\'') inSingle = false;
                continue;
            }
            if(inDouble) {
                     if(ch == '\\') i++;
                else if(ch == '"')  inDouble = false;
                continue;
            }
                 if(ch == '\'') inSingle = true;
            else if(ch == '"')  inDouble = true;
            else                {
                     if(ch == '{' || ch == '[') depth++;
                else if(ch == '}' || ch == ']') depth--;
                if( stop != null && stop.shouldStopAt(
                    s, i, ch, depth
                ) ) return new BracketScanResult(i, depth);
            }
        } // for

        return new BracketScanResult(-1, depth);
    }

    static String[] computeColonAlignmentPadding(
        final int                 size,
        final IntPredicate        isKeyed,
        final IntPredicate        hasLeadingComments,
        final IntPredicate        blankBefore,
        final IntFunction<String> keyAt
    )
    {
        final String[] padding    = new String[size];
              int      groupStart = -1;
        for(int i = 0; i <= size; ++i) {
            final boolean atEnd        = i == size;
            final boolean breaksBefore = atEnd || !isKeyed.test(
                i
            ) || hasLeadingComments.test(
                i
            ) || blankBefore.test(
                i
            );
            if(breaksBefore) {
                if(groupStart >= 0 && i > groupStart) {
                    final List<String> keys = new ArrayList<>();
                    for(int g = groupStart; g < i; ++g) keys.add( keyAt.apply(g) );
                    final String[] groupPad = FormatterSimpleBraced.padKeysForColonAlignment(keys);
                    for(int g = groupStart; g < i; ++g) padding[g] = groupPad[g - groupStart];
                }
                groupStart = ( !atEnd && isKeyed.test(i) ) ? i : -1;
            } // if
            else if(groupStart < 0) {
                groupStart = i;
            }
        } // for

        return padding;
    }

} // class YamlTomlSharedRule
