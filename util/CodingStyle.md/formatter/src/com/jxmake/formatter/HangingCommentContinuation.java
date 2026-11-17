/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter;

import java.util.ArrayList;
import java.util.List;

import com.jxmake.formatter.tokenizer.TokenizerCore;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;
import com.jxmake.formatter.tokenizer.TokenizerCurly;

/**
 * A trailing `//` comment continued on the following line(s), each continuation a standalone `//`
 * comment at exactly the trailing comment's column:
 *
 * <pre>
 * bool gemm = false;    // cuBLAS gemm (cuDNN's FMA
 *                       // algorithms are slow here)
 * </pre>
 *
 * Every pass treats a standalone comment line on its own, so the continuation kept its old column
 * once the trailing comment moved, was capitalized as a new sentence and ended the declaration's
 * alignment group (RDD_KEY_381). {@link #join} pulls the continuation lines out into a side table
 * before formatting and leaves an indexed marker at the end of the trailing comment, so the passes
 * see one ordinary trailing comment; {@link #split} puts the lines back, verbatim, under wherever
 * that comment ended up.
 */
final class HangingCommentContinuation {

    private static final char MARK = '\u0001';

    /** The text with its continuation lines folded away, and those lines by marker index */
    static final class Joined {

        final String             text;
        final List<List<String>> lines; // Per marker: each continuation comment's text

        Joined(final String text, final List<List<String>> lines)
        {
            this.text  = text;
            this.lines = lines;
        }

    } // class Joined

    private HangingCommentContinuation()
    {
    }

    /** Folds every continuation away (returns {@code content} itself when there is none) */
    static Joined join(
        final String  content,
        final Lang    lang,
        final boolean formatOff,
        final int     tabWidth
    )
    {
        final List<List<String>> lines = new ArrayList<>();
        if( content.indexOf(MARK) >= 0 ) return new Joined(content, lines);
        final List<Token> tokens = new TokenizerCurly(lang).tokenize(content);
        TokenizerCore.markFrozenSpans(tokens, formatOff);

        final StringBuilder sb = new StringBuilder( content.length() );
              int           i  = 0;
        while( i < tokens.size() ) {
            final Token t = tokens.get(i);
            sb.append(t.text);
            ++i;
            if( !isFoldableComment(t) || isStandalone(tokens, i - 1) ) continue;
            final int          column = visualColumn(tokens, i - 1, tabWidth);
            final List<String> conts  = new ArrayList<>();
            // [NEWLINE] [WHITESPACE] COMMENT_LINE, ending its line, at the same column
            while( i + 1 < tokens.size() && tokens.get(i).type == TokenType.NEWLINE ) {
                int j = i + 1;
                if( tokens.get(j).type == TokenType.WHITESPACE ) ++j;
                if( j >= tokens.size() || !isFoldableComment( tokens.get(j) ) ) break;
                if( j + 1 < tokens.size() && tokens.get(j + 1).type != TokenType.NEWLINE ) break;
                if( visualColumn(tokens, j, tabWidth) != column ) break;
                conts.add( tokens.get(j).text );
                i = j + 1;
            } // while
            if( conts.isEmpty() ) continue;
            sb.append(MARK).append( lines.size() ).append(MARK);
            lines.add(conts);
        } // while

        return new Joined( lines.isEmpty() ? content : sb.toString(), lines );
    }

    /**
     * Puts every folded line back under its comment, or returns null when a marker is missing or
     * no longer ends a `//` comment (the caller then formats the original text instead)
     */
    static String split(final String formatted, final Lang lang, final Joined joined)
    {
        if( joined.lines.isEmpty() ) return formatted;
        final List<Token>   tokens = new TokenizerCurly(lang).tokenize(formatted);
        final StringBuilder sb     = new StringBuilder( formatted.length() );
        final boolean[]     seen   = new boolean[ joined.lines.size() ];
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t   = tokens.get(i);
            final int   end = t.text.length() - 1;
            if( t.type != TokenType.COMMENT_LINE || end < 0 || t.text.charAt(end) != MARK ) {
                if( t.text.indexOf(MARK) >= 0 ) return null; // A marker outside a comment's end
                sb.append(t.text);
                continue;
            }
            final int start = t.text.lastIndexOf(MARK, end - 1);
            if(start < 0) return null;
            final int idx;
            try {
                idx = Integer.parseInt( t.text.substring(start + 1, end) );
            }
            catch(final NumberFormatException e) {
                return null;
            }
            if( idx < 0 || idx >= seen.length || seen[idx] ) return null;
            seen[idx] = true;
            final String head = t.text.substring(0, start);
            if( head.indexOf(MARK) >= 0 ) return null;
            // The formatted line up to the comment's `//`, blanked: same column, same tab/space mix
            final String ws = blankOut( linePrefix(sb) );
            sb.append(head);
            final List<String> conts = joined.lines.get(idx);
            for(final String line : conts) sb.append('\n').append(ws).append(line);
        } // for
        for(final boolean s : seen) {
            if(!s) return null;
        }

        return sb.toString();
    }

    private static boolean isFoldableComment(final Token t)
    {
        return t.type == TokenType.COMMENT_LINE && !t.frozen && t.text.startsWith("//")
                && !t.text.contains("JXM_CFMT");
    }

    /** True iff only whitespace precedes the token on its line */
    private static boolean isStandalone(final List<Token> tokens, final int idx)
    {
        int p = idx - 1;
        while( p >= 0 && tokens.get(p).type == TokenType.WHITESPACE ) --p;

        return p < 0 || tokens.get(p).type == TokenType.NEWLINE;
    }

    private static int visualColumn(final List<Token> tokens, final int idx, final int tabWidth)
    {
        final StringBuilder line = new StringBuilder();
        for(int p = idx - 1; p >= 0; --p) {
            final Token t = tokens.get(p);
            if(t.type == TokenType.NEWLINE) break;
            line.insert(0, t.text);
        }

        return visualWidth( line.toString(), tabWidth );
    }

    private static int visualWidth(final String s, final int tabWidth)
    {
        final int tab = Math.max(1, tabWidth);
              int col = 0;
        for( int k = 0; k < s.length(); ++k ) {
            if( s.charAt(k) == '\t' ) col += tab - col % tab;
            else                      ++col;
        }

        return col;
    }

    /** The text of the current (last) line built so far */
    private static String linePrefix(final StringBuilder sb)
    {
        final int nl = sb.lastIndexOf("\n");

        return sb.substring(nl + 1);
    }

    /** {@code prefix} with every character but a tab turned into a space (same visual width) */
    private static String blankOut(final String prefix)
    {
        final StringBuilder ws = new StringBuilder( prefix.length() );
        for( int k = 0; k < prefix.length(); ++k ) {
            final char c = prefix.charAt(k);
            ws.append(c == '\t' ? c : ' ');
        }

        return ws.toString();
    }

} // class HangingCommentContinuation
