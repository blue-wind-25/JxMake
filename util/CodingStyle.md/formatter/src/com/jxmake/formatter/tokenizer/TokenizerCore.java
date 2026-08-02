/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.tokenizer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Slim, language-family-agnostic base for every tokenizer sibling ({@link TokenizerCurly} for
 * C/C++/Java/Kotlin/JS/TS, and future {@code TokenizerIndent}/{@code TokenizerTags} for
 * Python3/XML-HTML5). Holds only what every family's scan needs regardless of scoping-delimiter
 * shape: the {@link Token}/{@link TokenType} model, the shared scan-position fields, the generic
 * char/number/whitespace/newline emitters, and {@link #markFrozenSpans}. Family-specific lexing
 * (keyword sets, brace/paren tracking, string/comment/preprocessor handling, angle-bracket
 * disambiguation, etc.) lives in the sibling classes, not here.
 */
public class TokenizerCore {

    public enum TokenType {

        KEYWORD,
        IDENTIFIER,
        NUMBER,
        STRING,
        CHAR,
        OP,
        PUNCT,
        COMMENT_LINE,
        COMMENT_BLOCK,
        SHEBANG,             // JS/TS only -- opaque `#!...` first line; Python's `#` is already its
                             // Real comment char and C/C++'s already goes through PREPROCESSOR, so
                             // JS/TS is the only curly-family language where `#!` wasn't already
                             // inert (see TokenizerCurly#emitShebangLine)
        WHITESPACE,
        NEWLINE,
        PREPROCESSOR,        // C/C++ only — opaque single-line #-directive
        MACRO_DEF,           // C/C++ only — opaque multiline #define with \ continuations
        ANGLE_BRACKET_OPEN,  // generic/template context
        ANGLE_BRACKET_CLOSE, // generic/template context
        FSTRING_START,       // Python3 only — opening quote(s) of an f-string, see TokenizerIndent
        FSTRING_MIDDLE,      // Python3 only — literal text segment of an f-string between fields
        FSTRING_END,         // Python3 only — closing quote(s) of an f-string
        FSTRING_FORMAT_SPEC, // Python3 only — opaque `:format_spec` tail of an f-string field
        INDENT,              // Python3 only — synthesized, see TokenizerIndent#synthesizeIndentation
        DEDENT               // Python3 only — synthesized, see TokenizerIndent#synthesizeIndentation

    } // enum TokenType

    public static final class Token {

        public final TokenType type;
        public       String    text;
        public final int       braceDepth;
        public final int       parenDepth;
        public final String    name;       // For `{`/`}` only: pushed/popped construct name, else null
        public       boolean   frozen;     // set by markFrozenSpans; true = opaque pass-through, never transformed

        public Token(
            final TokenType type,
            final String    text,
            final int       braceDepth,
            final int       parenDepth,
            final String    name
        )
        {
            this.type       = type;
            this.text       = text;
            this.braceDepth = braceDepth;
            this.parenDepth = parenDepth;
            this.name       = name;
        }

        /**
         * Null-safe: {@code t} is a `PUNCT` token whose text equals {@code text}. Centralizes
         *  what used to be a byte-for-byte-identical private helper duplicated in nearly every
         *  rule class.
         */
        public static boolean isPunct(final Token t, final String text)
        {
            return t != null && t.type == TokenType.PUNCT && text.equals(t.text);
        }

        /** Null-safe: {@code t} is an `OP` token whose text equals {@code text} */
        public static boolean isOp(final Token t, final String text)
        {
            return t != null && t.type == TokenType.OP && text.equals(t.text);
        }

        /** Null-safe: {@code t} is an `OP` token whose text equals repeated {@code ch} */
        public static boolean isRepOp(final Token t, final char ch)
        {
            if( t == null || t.type != TokenType.OP || t.text.isEmpty() ) return false;
            for( int i = 0; i < t.text.length(); ++i ) {
                if( t.text.charAt(i) != ch ) return false;
            }

            return true;
        }

        /** Null-safe: {@code t} is a `KEYWORD` token whose text equals {@code text} */
        public static boolean isKeyword(final Token t, final String text)
        {
            return t != null && t.type == TokenType.KEYWORD && text.equals(t.text);
        }

        /** Null-safe: {@code t} is a line or block comment token */
        public static boolean isComment(final Token t)
        {
            return t != null && (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK);
        }

        /**
         * Null-safe: {@code t} is whitespace, a newline, or a comment -- a token every rule
         *  class's rendering passes skip over when scanning for the next significant token
         */
        public static boolean isGapToken(final Token t)
        {
            return t != null && (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                    || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK
                    || t.type == TokenType.SHEBANG);
        }

    } // class Token

    /**
     * Small keyword/operator-set-literal builder -- previously re-implemented byte-identically
     *  in both {@link TokenizerCurly} and {@link TokenizerIndent} (and independently in a few
     *  {@code rules} package classes outside this hierarchy); promoted here during the
     *  2026-07-28 cleanup pass since both tokenizer siblings shared this exact base already
     */
    protected static Set<String> setOf(final String... words)
    {
        return new HashSet<>( Arrays.asList(words) );
    }

    private static final java.util.regex.Pattern FORMAT_DIS_MARKER = java.util.regex.Pattern.compile(
            "^//%\\s*JXM_CFMT_DIS\\s*$|^/\\*%\\s*JXM_CFMT_DIS\\s*\\*/$");
    private static final java.util.regex.Pattern FORMAT_ENA_MARKER = java.util.regex.Pattern.compile(
            "^//%\\s*JXM_CFMT_ENA\\s*$|^/\\*%\\s*JXM_CFMT_ENA\\s*\\*/$");

    /**
     * Scans {@code tokens} in order, toggling a frozen/unfrozen state on
     *  {@code //% JXM_CFMT_DIS}/{@code ENA} (and block-comment equivalent) marker comments, and
     *  stamps {@link Token#frozen} on every token accordingly. A marker token itself is always
     *  stamped frozen (never reformatted/removed), regardless of whether it disables or
     *  re-enables. {@code startFrozen} seeds the initial state (set from {@code --format-off}).
     */
    public static void markFrozenSpans(final List<Token> tokens, final boolean startFrozen)
    {
        boolean frozen = startFrozen;
        for(final Token t : tokens) {
            if( Token.isComment(t) ) {
                final String trimmed = t.text.trim();
                if( FORMAT_DIS_MARKER.matcher(trimmed).matches() ) {
                    frozen = true;
                }
                else if( FORMAT_ENA_MARKER.matcher(trimmed).matches() ) {
                    t.frozen = true;
                    frozen   = false;
                    continue;
                }
            } // if
            t.frozen = frozen;
        } // for
    }

    protected String source;
    protected int    pos;
    protected int    length;

    protected int braceDepth;
    protected int parenDepth;

    protected Token emitNumber()
    {
        final int start = pos;
        while(pos < length) {
            final char c = source.charAt(pos);
            // A `.` followed by another `.` is never a decimal point -- it's the start of
            // Kotlin's `..`/`..<` range operator (e.g. `1..10`), which must not be swallowed
            // into the number literal.
            if( c == '.' && peek(1) == '.' ) break;
            if( Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == '\'' ) {
                ++pos;
                continue;
            }
            if( (c == '+' || c == '-') && pos > start ) {
                final char prev = source.charAt(pos - 1);
                if(prev == 'e' || prev == 'E' || prev == 'p' || prev == 'P') {
                    ++pos;
                    continue;
                }
            } // if
            break;
        } // while

        return new Token(
            TokenType.NUMBER, source.substring(start, pos), braceDepth, parenDepth,
            null
        );
    }

    protected Token emitWhitespace()
    {
        final int start = pos;
        while( pos < length && ( source.charAt(pos) == ' ' || source.charAt(pos) == '\t' ) ) pos++;

        return new Token(
            TokenType.WHITESPACE, source.substring(start, pos), braceDepth,
            parenDepth, null
        );
    }

    protected Token emitNewline()
    {
        final int start = pos;
        if( source.charAt(pos) == '\r' ) {
            ++pos;
            if( pos < length && source.charAt(pos) == '\n' ) pos++;
        }
        else {
            ++pos;
        }

        return new Token(
            TokenType.NEWLINE, source.substring(start, pos), braceDepth, parenDepth,
            null
        );
    }

    protected boolean isIdentifierStart(final char c)
    {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    protected boolean isIdentifierPart(final char c)
    {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    protected char peek(final int offset)
    {
        final int p = pos + offset;

        return p < length ? source.charAt(p) : '\0';
    }

} // class TokenizerCore
