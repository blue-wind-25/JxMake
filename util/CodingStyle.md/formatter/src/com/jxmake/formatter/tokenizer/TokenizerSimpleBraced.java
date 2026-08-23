/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.tokenizer;

/**
 * Shared base for the "SimpleBraced" family: languages that are brace/bracket-delimited like the
 * curly-imperative family, but have no keywords, control flow, or preprocessor -- currently
 * JSON/JSON5 ({@link JsonTokenizer}), with CSS ({@code STYLE_DATA_FORMATS.md} §3) as the family's
 * other planned member. Holds only what both concretely share today: `/* *\/`-style block comment
 * scanning (JSON5 and CSS both use it; JSON5 additionally has `//` line comments, which stay in
 * {@link JsonTokenizer} since CSS has no line-comment syntax). Everything else
 * (string/number/selector/property grammar) is family-member-specific and lives in the concrete
 * sibling.
 */
public abstract class TokenizerSimpleBraced extends TokenizerCore {

    protected Token emitBlockComment()
    {
        final int start = pos;
        pos += 2;
        while( pos < length && !( source.charAt(pos) == '*' && peek(1) == '/' ) ) pos++;
        if(pos < length) pos += 2;

        return new Token( TokenType.COMMENT_BLOCK, source.substring(start, pos), 0, 0, null );
    }

    /**
     * Consumes a quoted string as one opaque {@code STRING} token, from an already-positioned
     * opening {@code quote} through its matching close (or an un-escaped raw newline, treated as
     * unterminated and left for the parser to surface). {@code allowLineContinuation} additionally
     * treats a backslash immediately followed by {@code \r}[{@code \n}] as consuming the whole
     * line-terminator pair (JSON5's backslash-newline string continuation, STYLE_DATA_FORMATS.md
     * §1.3); when {@code false} a backslash simply consumes exactly one following character
     * regardless of what it is (CSS has no such continuation rule). Shared by
     * {@link JsonTokenizer#emitString}/{@link CssTokenizer#emitString}, previously reimplemented
     * independently in each.
     */
    protected final Token emitQuotedString(final char quote, final boolean allowLineContinuation)
    {
        final int start = pos;
        ++pos; // Opening quote
        while(pos < length) {
            final char c = source.charAt(pos);
            if(c == '\\') {
                // Backslash followed by anything is consumed as a pair -- never inspected further
                ++pos;
                if(pos < length) {
                    if( allowLineContinuation && source.charAt(pos) == '\r' ) {
                        ++pos;
                        if( pos < length && source.charAt(pos) == '\n' ) pos++;
                    }
                    else {
                        ++pos;
                    }
                } // if
                continue;
            } // if
            if(c == quote) {
                ++pos;
                break;
            }
            if(c == '\n' || c == '\r') {
                // Unterminated string reaching a raw newline -- stop here rather than swallowing
                // the rest of the file; the parser surfaces this as malformed input
                break;
            }
            ++pos;
        } // while

        return new Token( TokenType.STRING, source.substring(start, pos), 0, 0, null );
    }

} // class TokenizerSimpleBraced
