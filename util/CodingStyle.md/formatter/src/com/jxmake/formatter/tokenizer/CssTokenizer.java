/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.tokenizer;

import java.util.ArrayList;
import java.util.List;

/**
 * CSS tokenizer -- extends {@link TokenizerSimpleBraced} (RDD_KEY_190), sibling of
 * {@link JsonTokenizer}. Deliberately coarse-grained: CSS selector/value grammar (combinators,
 * pseudo-classes, functions, units, hex colors, etc.) is not individually modeled -- everything
 * that isn't whitespace, a comment, a string, or one of the structural punctuation characters
 * ({@code { } ( ) ; : , &}) is emitted as one contiguous {@code OP} run. {@code CssSpecificRule}'s
 * parser only needs to find top-level (paren-depth 0) {@code {}/;/:} boundaries -- it reconstructs
 * selector/value text by concatenating token text, not by re-parsing it.
 */
public final class CssTokenizer extends TokenizerSimpleBraced {

    private static final String STRUCTURAL = "{}();:,&";

    public List<Token> tokenize(final String src)
    {
        this.source = src;
        this.pos    = 0;
        this.length = src.length();
        final List<Token> tokens = new ArrayList<>();

        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\r' || c == '\n') {
                tokens.add( emitNewline() );
                continue;
            }
            if(c == ' ' || c == '\t') {
                tokens.add( emitWhitespace() );
                continue;
            }
            if( c == '/' && peek(1) == '*' ) {
                tokens.add( emitBlockComment() );
                continue;
            }
            if(c == '"' || c == '\'') {
                tokens.add( emitString(c) );
                continue;
            }
            if( STRUCTURAL.indexOf(c) >= 0 ) {
                tokens.add( new Token( TokenType.PUNCT, String.valueOf(c), 0, 0, null ) );
                ++pos;
                continue;
            }
            tokens.add( emitTextRun() );
        } // while

        return tokens;
    }

    private Token emitString(final char quote)
    {
        final int start = pos;
        ++pos;
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\\') {
                ++pos;
                if(pos < length) pos++;
                continue;
            }
            if(c == quote) {
                ++pos;
                break;
            }
            if(c == '\n' || c == '\r') break; // Unterminated string reaching a raw newline
            ++pos;
        } // while

        return new Token( TokenType.STRING, source.substring(start, pos), 0, 0, null );
    }

    /**
     * A run of any characters that are not whitespace, a comment start, a quote, or one of the
     *  structural punctuation characters -- covers selector fragments, property names, at-rule
     *  keywords, and value text (numbers, units, hex colors, function names) uniformly
     */
    private Token emitTextRun()
    {
        final int start = pos;
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == ' ' || c == '\t' || c == '\r' || c == '\n') break;
            if( c == '/' && peek(1) == '*' ) break;
            if(c == '"' || c == '\'') break;
            if( STRUCTURAL.indexOf(c) >= 0 ) break;
            ++pos;
        } // while

        return new Token( TokenType.OP, source.substring(start, pos), 0, 0, null );
    }

} // class CssTokenizer
