/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * STYLE_TOOLING.md §4 (E-INI / Extended INI) rule logic. Line-oriented (no cross-line quoting --
 * quotes never span a physical line), same narrow beautification-only shape as {@link
 * MakefileSpecificRule}/{@link BashSpecificRule}: a fixed five-rule list, everything else left
 * byte-identical. Recognizes `=`/`:` key-value separators, `[name]`/`{name}`/`&lt;name&gt;`/
 * `(name)`/bare-word group headers, and `#`/`;`/`@`/`//`/triple-quote comments, each only outside
 * quoted spans (`'...'`/`"..."`). Implements: §4.1 separator alignment within a contiguous
 * key-value group (blank/non-matching line breaks the group, same semantics as Makefile §1.1),
 * §4.2 indentation snapped to the nearest {@code indentWidth} multiple (no depth concept -- E-INI
 * has no structural nesting), §4.3 backslash-continuation alignment under the first line's value
 * start column (same mechanism as Makefile §1.2), §4.4 comment normalization (reusing {@link
 * ToolingCommentNormalizer}, capitalizing unconditionally -- no Bash-style tool-name skip list),
 * §4.5 no long-line breaking (simply never attempted).
 */
public final class EiniSpecificRule {

    private final int     indentWidth;
    private final boolean normalizeCommentStartCase;
    private final boolean normalizeCommentEndPeriod;
    // `normalize-comment-start-case-multiline` -- default off, set post-construction (matches the
    // curly family's MiscRuleCore#setNormalizeCommentMultiSentenceCase pattern)
    private boolean normalizeCommentMultiSentenceCase = false;

    public EiniSpecificRule(
        final int     indentWidth,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod
    )
    {
        this.indentWidth               = Math.max(1, indentWidth);
        this.normalizeCommentStartCase = normalizeCommentStartCase;
        this.normalizeCommentEndPeriod = normalizeCommentEndPeriod;
    }

    public void setNormalizeCommentMultiSentenceCase(final boolean value)
    {
        this.normalizeCommentMultiSentenceCase = value;
    }

    // ---- Line scan: locates the first unquoted key-value separator and the first unquoted comment
    //      marker on a single physical line ------------------------------------------------------

    private static final class LineScan {

        int  sepIdx     = -1;
        char sepChar;
        int  commentIdx = -1;
        int  markerLen;

    } // class LineScan

    private static LineScan scanLine(final String line)
    {
        final LineScan scan  = new LineScan();
              char     quote = 0;
              int      i     = 0;
        while( i < line.length() ) {
            final char c = line.charAt(i);

            if(quote != 0) {
                if(c == quote) quote = 0;
                ++i;
                continue;
            }

            if( (c == '\'' || c == '"') && i + 2 < line.length()
                    && line.charAt(i + 1) == c && line.charAt(i + 2) == c ) {
                scan.commentIdx = i;
                scan.markerLen  = 3;
                return scan;
            }
            if(c == '\'' || c == '"') { quote = c; ++i; continue; }
            if(c == '#' || c == ';' || c == '@') {
                scan.commentIdx = i;
                scan.markerLen  = 1;
                return scan;
            }
            if( c == '/' && i + 1 < line.length() && line.charAt(i + 1) == '/' ) {
                scan.commentIdx = i;
                scan.markerLen  = 2;
                return scan;
            }
            if( scan.sepIdx < 0 && (c == '=' || c == ':') ) {
                scan.sepIdx  = i;
                scan.sepChar = c;
            }
            ++i;
        } // while

        return scan;
    }

    /** Collapses unquoted whitespace runs to a single space; preserves interior of quoted spans verbatim (§4 key/header rule) */
    private static String collapseOutsideQuotes(final String raw)
    {
        final String        s         = raw.trim();
        final StringBuilder out       = new StringBuilder();
              char          quote     = 0;
              boolean       lastSpace = false;
        for( int i = 0; i < s.length(); ++i ) {
            final char c = s.charAt(i);
            if(quote != 0) {
                out.append(c);
                if(c == quote) quote = 0;
                lastSpace = false;
                continue;
            }
            if(c == '\'' || c == '"') { quote = c; out.append(c); lastSpace = false; continue; }
            if(c == ' ' || c == '\t') {
                if(!lastSpace) { out.append(' '); lastSpace = true; }
                continue;
            }
            out.append(c);
            lastSpace = false;
        } // for

        return out.toString();
    }

    private static boolean endsWithContinuation(final String value)
    {
        return ToolingSharedRule.endsWithContinuation(value);
    }

    private static String stripContinuation(final String value)
    {
        return ToolingSharedRule.stripContinuation(value);
    }

    private static String snapIndentPrefix(final String rawLine, final int indentWidth)
    {
        final int i       = ToolingSharedRule.leadingWhitespace(rawLine).length();
        final int snapped = (i == 0) ? 0 : ( (i + indentWidth - 1) / indentWidth )* indentWidth;

        return repeatChar(' ', snapped);
    }

    private static char closingFor(final char open)
    {
             if(open == '[') return ']';
        else if(open == '{') return '}';
        else if(open == '<') return '>';
        else if(open == '(') return ')';

        return 0;
    }

    private static final class KvItem {

        String       prefix;
        String       key;
        char         sepChar;
        List<String> valueParts;
        String       commentText; // null if none -- includes marker + normalized body

    } // class KvItem

    public String format(final String content)
    {
        final boolean      endsWithNewline = content.endsWith("\n");
        final String[]     rawLines        = content.split("\n", -1);
        final List<String> lines           = new ArrayList<>( java.util.Arrays.asList(rawLines) );
        if( endsWithNewline && !lines.isEmpty() ) lines.remove( lines.size() - 1 );

        final List<String> out   = new ArrayList<>();
        final List<KvItem> group = new ArrayList<>();
              int          idx   = 0;

        while( idx < lines.size() ) {
            final String   line = lines.get(idx);
            final LineScan scan = scanLine(line);

            final int    contentEnd = (scan.commentIdx >= 0) ? scan.commentIdx : line.length();
            final String rawContent = line.substring(0, contentEnd);

            if( scan.commentIdx < 0 && rawContent.trim().isEmpty() ) {
                // Blank line -- breaks any active alignment group
                flushGroup(out, group);
                out.add("");
                ++idx;
                continue;
            } // if

            final String prefix = snapIndentPrefix(line, indentWidth);

            if( rawContent.trim().isEmpty() ) {
                // Comment-only line (nothing but whitespace precedes the marker) -- chain-group with
                //  any immediately-following comment-only lines (no blank line between), same
                //  semantics as MakefileSpecificRule's comment handling, so a multi-line comment
                //  block (e.g. a copyright header) is normalized as one logical unit rather than
                //  each line independently (a per-line-only pass would wrongly strip a sole trailing
                //  `.` on an earlier line of the block).
                flushGroup(out, group);
                final List<String> commentRawLines = new ArrayList<>();
                final List<String> markers         = new ArrayList<>();
                final List<String> bodies          = new ArrayList<>();
                final List<String> prefixes        = new ArrayList<>();
                while( idx < lines.size() ) {
                    final String   l = lines.get(idx);
                    final LineScan s = scanLine(l);
                    final int      e = (s.commentIdx >= 0) ? s.commentIdx : l.length();
                    if( s.commentIdx < 0 || !l.substring(0, e).trim().isEmpty() ) break;
                    commentRawLines.add(l);
                    markers.add( l.substring(s.commentIdx, s.commentIdx + s.markerLen) );
                    bodies.add( l.substring(s.commentIdx + s.markerLen) );
                    prefixes.add( snapIndentPrefix(l, indentWidth) );
                    ++idx;
                } // while
                final List<Boolean> blanks = new ArrayList<>();
                for( int k = 0; k < bodies.size(); ++k ) blanks.add(false);
                final List<String> normalized = ToolingCommentNormalizer.normalizeChain(
                    bodies,
                    blanks,
                    normalizeCommentStartCase,
                    normalizeCommentEndPeriod,
                    null,
                    normalizeCommentMultiSentenceCase
                );
                for( int k = 0; k < normalized.size(); ++k ) out.add(
                    prefixes.get(k) + markers.get(k) + normalized.get(k)
                );
                continue;
            } // if

            String commentText = null;
            if(scan.commentIdx >= 0) {
                final String body       = line.substring(scan.commentIdx + scan.markerLen);
                final String normalized = ToolingCommentNormalizer.normalize(
                    body,
                    normalizeCommentStartCase,
                    normalizeCommentEndPeriod,
                    null,
                    normalizeCommentMultiSentenceCase
                );
                final String marker     = line.substring(
                    scan.commentIdx, scan.commentIdx + scan.markerLen
                );
                commentText = " " + marker + normalized;
            } // if

            if(scan.sepIdx >= 0) {
                // Key-value line
                final String keyRaw   = line.substring(0, scan.sepIdx);
                final String valueRaw = line.substring(scan.sepIdx + 1, contentEnd);

                final KvItem item = new KvItem();
                item.prefix      = prefix;
                item.key         = collapseOutsideQuotes(keyRaw);
                item.sepChar     = scan.sepChar;
                item.valueParts  = new ArrayList<>();
                item.commentText = commentText;

                String value = valueRaw.trim();
                if(commentText == null) {
                    while( endsWithContinuation(value) && idx + 1 < lines.size() ) {
                        item.valueParts.add( stripContinuation(value) );
                        ++idx;
                        value = lines.get(idx).trim();
                    }
                } // if
                item.valueParts.add( stripContinuation(value) );

                group.add(item);
                ++idx;
                continue;
            } // if

            // Header line (bracket-wrapped or bare word sequence)
            flushGroup(out, group);
            final String trimmed = rawContent.trim();
                  String rendered;
            if( trimmed.length() >= 2 && closingFor( trimmed.charAt(0) ) != 0
                    && trimmed.charAt( trimmed.length() - 1 ) == closingFor( trimmed.charAt(0) ) ) {
                final char   open  = trimmed.charAt(0);
                final char   close = closingFor(open);
                final String inner = trimmed.substring( 1, trimmed.length() - 1 );
                rendered = open + collapseOutsideQuotes(inner) + close;
            }
            else {
                rendered = collapseOutsideQuotes(trimmed);
            }
            out.add( prefix + rendered + (commentText == null ? "" : commentText) );
            ++idx;
        } // while

        flushGroup(out, group);

        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < out.size(); ++i ) {
            sb.append( out.get(i) );
            if( i + 1 < out.size() || endsWithNewline ) sb.append('\n');
        }

        return sb.toString();
    }

    private void flushGroup(final List<String> out, final List<KvItem> group)
    {
        if( group.isEmpty() ) return;
        int keyWidth = 0;
        for(final KvItem item : group) keyWidth = Math.max( keyWidth, item.key.length() );
        for(final KvItem item : group) {
            final int     pad      = keyWidth - item.key.length();
            final String  firstVal = item.valueParts.get(0);
            final boolean multi    = item.valueParts.size() > 1;
                  int     valWidth = 0;
            if(multi) for(final String part : item.valueParts) valWidth = Math.max(
                valWidth, part.length()
            );
            out.add(
                item.prefix + item.key + repeatChar(' ', pad) + " " + item.sepChar + " " + firstVal
                        + ( multi ? repeatChar( ' ', valWidth - firstVal.length() ) + " \\" : "" )
                        + (item.commentText == null ? "" : item.commentText)
            );
            final String contPrefix = item.prefix + repeatChar(' ', keyWidth + 3);
            for( int i = 1; i < item.valueParts.size(); ++i ) {
                final boolean last = i == item.valueParts.size() - 1;
                final String  part = item.valueParts.get(i);
                out.add(
                    contPrefix + part + ( last ? "" : repeatChar( ' ', valWidth - part.length() ) + " \\" )
                );
            } // for i
        } // for item
        group.clear();
    }

    private static String repeatChar(final char c, final int count)
    {
        return ToolingSharedRule.repeatChar(c, count);
    }

} // class EiniSpecificRule
