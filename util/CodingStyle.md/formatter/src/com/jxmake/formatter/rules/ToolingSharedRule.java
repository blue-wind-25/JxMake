/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Shared helper logic that was structurally identical, byte-for-byte, across {@link
 * MakefileSpecificRule}, {@link BashSpecificRule}, and {@link PowerShellSpecificRule} (2026-08-16
 * cleanup-pass consolidation, mirroring the {@code YamlTomlSharedRule} pattern already used for
 * YAML/TOML): a negative-clamping {@code repeatChar} and the {@code indent(depth)} built on top of
 * it. Not merged with {@code YamlTomlSharedRule}/{@link com.jxmake.formatter.FormatterSimpleBraced}
 * because those two families' own {@code repeatChar} does not clamp a negative {@code count}
 * (callers there never pass one) -- kept as a separate, intentionally-narrower helper rather than
 * force one shared implementation onto both clamping conventions.
 */
final class ToolingSharedRule {

    private ToolingSharedRule()
    {
    }

    static String repeatChar(final char c, final int count)
    {
        final StringBuilder sb = new StringBuilder( Math.max(0, count) );
        for(int i = 0; i < count; ++i) sb.append(c);

        return sb.toString();
    }

    static String indent(final int depth, final int indentWidth)
    {
        return repeatChar( ' ', Math.max(0, depth) * indentWidth );
    }

    // Byte-identical backslash-continuation helpers, promoted 2026-08-17 (RDD_KEY_305) from a
    // duplicate found in both MakefileSpecificRule and EiniSpecificRule (Bash/PowerShell do not
    // use this shape, so they are left out of this helper)
    static boolean endsWithContinuation(final String value)
    {
        return value.endsWith("\\") && !value.endsWith("\\\\");
    }

    static String stripContinuation(final String value)
    {
        if( endsWithContinuation(value) ) return value.substring( 0, value.length() - 1 ).trim();

        return value;
    }

    // Byte-identical leading-whitespace scanner, promoted from duplicate copies in
    // MakefileSpecificRule/BashSpecificRule/PowerShellSpecificRule
    static String leadingWhitespace(final String line)
    {
        int i = 0;
        while( i < line.length() && ( line.charAt(i) == ' ' || line.charAt(i) == '\t' ) ) ++i;

        return line.substring(0, i);
    }

    /**
     * A source's `\n`-split physical lines plus whether it ended with a trailing `\n` -- promoted
     * from a byte-identical private copy in {@link PowerShellSpecificRule}; {@link
     * MakefileSpecificRule}/{@link BashSpecificRule}/{@link EiniSpecificRule}/{@link
     * JxMakeSpecificRule} each independently wrote out the same split-then-strip-trailing-empty
     * construction and join-back logic instead of using a shared type for it. {@code lines} is
     * mutable and meant to be edited in place before calling {@link #join}.
     */
    static final class Lines {

        final List<String> lines;
        final boolean      endsWithNewline;

        Lines(final String content)
        {
            this.endsWithNewline = content.endsWith("\n");
            final String[] raw = content.split("\n", -1);
            this.lines = new ArrayList<>( Arrays.asList(raw) );
            if( endsWithNewline && !lines.isEmpty() ) lines.remove( lines.size() - 1 );
        }

        String join()
        {
            return joinLines(lines, endsWithNewline);
        }

    } // class Lines

    /** Joins {@code lines} back with `\n`, appending a final trailing `\n` only if {@code endsWithNewline} */
    static String joinLines(final List<String> lines, final boolean endsWithNewline)
    {
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < lines.size(); ++i ) {
            sb.append( lines.get(i) );
            if( i + 1 < lines.size() || endsWithNewline ) sb.append('\n');
        }

        return sb.toString();
    }

} // class ToolingSharedRule
