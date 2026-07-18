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
 * JSON/JSON5 tokenizer -- extends {@link TokenizerSimpleBraced} (RDD_KEY_190). Produces a flat
 * token stream; the recursive-descent parser in {@code JsonSpecificRule} builds its own tree from
 * this stream, so no brace/paren-depth tracking happens here. A JSON5 string containing a
 * backslash-newline continuation is emitted as a single opaque {@code STRING} token spanning every
 * continuation line, per STYLE_DATA_FORMATS.md §1.3 -- its internal content (including leading
 * whitespace on continuation lines) is never inspected beyond finding the closing quote.
 */
public final class JsonTokenizer extends TokenizerSimpleBraced {

    public List<Token> tokenize(final String src) {
        this.source = src;
        this.pos = 0;
        this.length = src.length();
        final List<Token> tokens = new ArrayList<>();

        while (pos < length) {
            final char c = source.charAt(pos);
            if (c == '\r' || c == '\n') {
                tokens.add(emitNewline());
                continue;
            }
            if (c == ' ' || c == '\t') {
                tokens.add(emitWhitespace());
                continue;
            }
            if (c == '/' && peek(1) == '/') {
                tokens.add(emitLineComment());
                continue;
            }
            if (c == '/' && peek(1) == '*') {
                tokens.add(emitBlockComment());
                continue;
            }
            if (c == '"' || c == '\'') {
                tokens.add(emitString(c));
                continue;
            }
            if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',') {
                tokens.add(new Token(TokenType.PUNCT, String.valueOf(c), 0, 0, null));
                pos++;
                continue;
            }
            if (c == '+' || c == '-') {
                tokens.add(new Token(TokenType.OP, String.valueOf(c), 0, 0, null));
                pos++;
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek(1)))) {
                tokens.add(emitJsonNumber());
                continue;
            }
            if (isIdentifierStart(c)) {
                tokens.add(emitIdentifier());
                continue;
            }
            // Unknown character (shouldn't occur in valid JSON/JSON5) -- pass through opaque so
            // the formatter never silently drops or corrupts unexpected input.
            tokens.add(new Token(TokenType.OP, String.valueOf(c), 0, 0, null));
            pos++;
        }
        return tokens;
    }

    private Token emitLineComment() {
        final int start = pos;
        while (pos < length && source.charAt(pos) != '\n' && source.charAt(pos) != '\r') {
            pos++;
        }
        return new Token(TokenType.COMMENT_LINE, source.substring(start, pos), 0, 0, null);
    }

    /** Consumes a quoted string, including JSON5 backslash-newline continuations, as one opaque
     *  token -- see STYLE_DATA_FORMATS.md §1.3. */
    private Token emitString(final char quote) {
        final int start = pos;
        pos++; // opening quote
        while (pos < length) {
            final char c = source.charAt(pos);
            if (c == '\\') {
                // Backslash followed by anything (including a line terminator, JSON5's
                // continuation) is consumed as a pair -- never inspected further.
                pos++;
                if (pos < length) {
                    if (source.charAt(pos) == '\r') {
                        pos++;
                        if (pos < length && source.charAt(pos) == '\n') {
                            pos++;
                        }
                    } else {
                        pos++;
                    }
                }
                continue;
            }
            if (c == quote) {
                pos++;
                break;
            }
            if (c == '\n' || c == '\r') {
                // Unterminated string reaching a raw newline -- stop here rather than swallowing
                // the rest of the file; the parser surfaces this as malformed input.
                break;
            }
            pos++;
        }
        return new Token(TokenType.STRING, source.substring(start, pos), 0, 0, null);
    }

    /** Handles RFC 8259 numbers plus JSON5 additions (hex integers, leading/trailing decimal
     *  point) -- a leading sign, if any, is tokenized separately as an {@code OP} token. */
    private Token emitJsonNumber() {
        final int start = pos;
        if (source.charAt(pos) == '0' && (peek(1) == 'x' || peek(1) == 'X')) {
            pos += 2;
            while (pos < length && isHexDigit(source.charAt(pos))) {
                pos++;
            }
            return new Token(TokenType.NUMBER, source.substring(start, pos), 0, 0, null);
        }
        while (pos < length && Character.isDigit(source.charAt(pos))) {
            pos++;
        }
        if (pos < length && source.charAt(pos) == '.') {
            pos++;
            while (pos < length && Character.isDigit(source.charAt(pos))) {
                pos++;
            }
        }
        if (pos < length && (source.charAt(pos) == 'e' || source.charAt(pos) == 'E')) {
            final int expStart = pos;
            pos++;
            if (pos < length && (source.charAt(pos) == '+' || source.charAt(pos) == '-')) {
                pos++;
            }
            if (pos < length && Character.isDigit(source.charAt(pos))) {
                while (pos < length && Character.isDigit(source.charAt(pos))) {
                    pos++;
                }
            } else {
                pos = expStart; // not actually an exponent -- back off
            }
        }
        return new Token(TokenType.NUMBER, source.substring(start, pos), 0, 0, null);
    }

    /** Unquoted JSON5 object key, or a bare value keyword (`true`/`false`/`null`, JSON5's
     *  `Infinity`/`NaN`) -- disambiguated by the parser based on position, not here. */
    private Token emitIdentifier() {
        final int start = pos;
        pos++;
        while (pos < length && isIdentifierPart(source.charAt(pos))) {
            pos++;
        }
        return new Token(TokenType.IDENTIFIER, source.substring(start, pos), 0, 0, null);
    }

    private boolean isHexDigit(final char c) {
        return Character.isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
