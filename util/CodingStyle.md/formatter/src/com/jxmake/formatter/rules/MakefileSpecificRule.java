/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STYLE_TOOLING.md §1 (Makefile) rule logic. Line-oriented, no tokenizer needed beyond
 * distinguishing tab-prefixed recipe lines (never touched -- Make is whitespace-sensitive there)
 * from everything else. Implements §1.1 (`=`/`:=`/`+=`/`?=` alignment groups, broken by a blank
 * line or any non-matching line per RDD_KEY_254), §1.2 (backslash-continuation alignment under the
 * first line's value start column), §1.3 (target `:` spacing per RDD_KEY_255), and §1.4
 * (`ifdef`/`ifeq`/`ifneq`/`else`/`endif` body indentation). Comments and any other construct are
 * explicitly out of scope (STYLE_TOOLING.md §0) and left byte-identical.
 */
public final class MakefileSpecificRule {

    private static final Pattern ASSIGN = Pattern.compile(
        "^([A-Za-z_][A-Za-z0-9_.]*)\\s*(:=|\\+=|\\?=|=)\\s*(.*)$"
    );
    private static final Pattern TARGET = Pattern.compile("^([^:\\s][^:]*?)\\s*(::?)\\s*(.*)$");
    private static final Pattern DIRECTIVE_OPEN = Pattern.compile("^(ifdef|ifndef|ifeq|ifneq)\\b.*$");
    private static final Pattern DIRECTIVE_ELSE = Pattern.compile("^else\\b.*$");
    private static final Pattern DIRECTIVE_ENDIF = Pattern.compile("^endif\\b.*$");

    private final int indentWidth;

    public MakefileSpecificRule(final int indentWidth)
    {
        this.indentWidth = Math.max(1, indentWidth);
    }

    private static final class AsgnItem {

        String       name;
        String       op;
        List<String> valueParts;
    }

    public String format(final String content)
    {
        final boolean        endsWithNewline = content.endsWith("\n");
        final String[]       rawLines        = content.split("\n", -1);
        final List<String>   lines           = new ArrayList<>( java.util.Arrays.asList(rawLines) );
        if( endsWithNewline && !lines.isEmpty() ) lines.remove( lines.size() - 1 );

        final List<String>   out       = new ArrayList<>();
        final List<AsgnItem> group     = new ArrayList<>();
              int            condDepth = 0;
              int            groupDepth = 0;
              int            idx        = 0;

        while( idx < lines.size() ) {
            final String raw     = lines.get(idx);
            final String trimmed = raw.trim();

            if( !raw.isEmpty() && raw.charAt(0) == '\t' ) {
                flushGroup(out, group, groupDepth);
                out.add(raw);
                ++idx;
                continue;
            }

            if( trimmed.isEmpty() ) {
                flushGroup(out, group, groupDepth);
                out.add("");
                ++idx;
                continue;
            }

            if( DIRECTIVE_OPEN.matcher(trimmed).matches() ) {
                flushGroup(out, group, groupDepth);
                out.add( indent(condDepth) + trimmed );
                ++condDepth;
                ++idx;
                continue;
            }

            if( DIRECTIVE_ELSE.matcher(trimmed).matches() ) {
                flushGroup(out, group, groupDepth);
                out.add( indent(condDepth - 1) + trimmed );
                ++idx;
                continue;
            }

            if( DIRECTIVE_ENDIF.matcher(trimmed).matches() ) {
                flushGroup(out, group, groupDepth);
                --condDepth;
                out.add( indent(condDepth) + trimmed );
                ++idx;
                continue;
            }

            final Matcher asgn = ASSIGN.matcher(trimmed);
            if( asgn.matches() ) {
                if( group.isEmpty() ) groupDepth = condDepth;
                final AsgnItem item = new AsgnItem();
                item.name       = asgn.group(1);
                item.op         = asgn.group(2);
                item.valueParts = new ArrayList<>();
                String value = asgn.group(3);
                while( endsWithContinuation(value) && idx + 1 < lines.size() ) {
                    item.valueParts.add( stripContinuation(value) );
                    ++idx;
                    value = lines.get(idx).trim();
                }
                item.valueParts.add( stripContinuation(value) );
                group.add(item);
                ++idx;
                continue;
            }

            final Matcher target = TARGET.matcher(trimmed);
            if( target.matches() ) {
                flushGroup(out, group, groupDepth);
                final String prereqs = target.group(3).trim().replaceAll("\\s+", " ");
                final String line    = target.group(1).trim() + target.group(2) + " " + prereqs;
                out.add( indent(condDepth) + line.trim() );
                ++idx;
                continue;
            }

            flushGroup(out, group, groupDepth);
            out.add(raw);
            ++idx;
        } // while

        flushGroup(out, group, groupDepth);

        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < out.size(); ++i ) {
            sb.append( out.get(i) );
            if( i + 1 < out.size() || endsWithNewline ) sb.append('\n');
        }

        return sb.toString();
    }

    private void flushGroup(final List<String> out, final List<AsgnItem> group, final int groupDepth)
    {
        if( group.isEmpty() ) return;
        int width = 0;
        for( final AsgnItem item : group ) width = Math.max( width, item.name.length() + item.op.length() );
        ++width;
        final String prefix = indent(groupDepth);
        for( final AsgnItem item : group ) {
            final int     pad      = width - item.name.length() - item.op.length();
            final String  firstVal = item.valueParts.get(0);
            final boolean multi    = item.valueParts.size() > 1;
            out.add(
                prefix + item.name + repeatChar(' ', pad) + item.op + " " + firstVal + ( multi ? " \\" : "" )
            );
            for( int i = 1; i < item.valueParts.size(); ++i ) {
                final boolean last = i == item.valueParts.size() - 1;
                out.add(
                    prefix + repeatChar(' ', width + 1) + item.valueParts.get(i) + ( last ? "" : " \\" )
                );
            }
        } // for
        group.clear();
    }

    private static boolean endsWithContinuation(final String value)
    {
        return value.endsWith("\\") && !value.endsWith("\\\\");
    }

    private static String stripContinuation(final String value)
    {
        if( endsWithContinuation(value) ) return value.substring( 0, value.length() - 1 ).trim();

        return value;
    }

    private String indent(final int depth)
    {
        return repeatChar( ' ', Math.max(0, depth) * indentWidth );
    }

    private static String repeatChar(final char c, final int count)
    {
        final StringBuilder sb = new StringBuilder( Math.max(0, count) );
        for(int i = 0; i < count; ++i) sb.append(c);

        return sb.toString();
    }

} // class MakefileSpecificRule
