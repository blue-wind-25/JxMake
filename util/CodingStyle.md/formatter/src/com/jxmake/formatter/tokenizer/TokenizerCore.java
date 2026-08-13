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
        DEDENT,              // Python3 only — synthesized, see TokenizerIndent#synthesizeIndentation
        JSX_SPAN,            // JS/TS `.jsx`/`.tsx` only — opaque whole-JSX-tree span, see
                             // TokenizerCurly#findJsxSpans. `text` holds the full raw source span
                             // (opening `<` through the matching close/self-close `>`), including any
                             // embedded newlines; `frozen` is always true. No new fields added to
                             // Token for this -- see STATE_JS_TS.md's 2026-08-12 design session.
        TEMPLATE_HOLE_OPEN,  // JS/TS `.jsx`/`.tsx` only — a template literal's `${` hole boundary,
                             // See TokenizerCurly#emitTemplateLiteralSegmented. Dedicated type
                             // (STATE_JS_TS.md's 2026-08-13 scoping session sub-context 1, option
                             // (b)) rather than a plain PUNCT "${" -- option (a) (reusing PUNCT) was
                             // tried first per the scoping session's own recommendation and found,
                             // empirically, to break existing `isPunct(t, "}")`-based ASI/statement
                             // logic that doesn't check for a matching real `{` (see RDD_LOG for the
                             // key); a dedicated type is invisible to every such check by construction.
        TEMPLATE_HOLE_CLOSE  // The matching `}` closing a TEMPLATE_HOLE_OPEN. Never a plain PUNCT

                             // "}" for the same reason as above -- see TokenizerCurly#emitTemplateHoleInterior

    } // enum TokenType

    public static final class Token {

        public final TokenType type;
        public       String    text;
        public final int       braceDepth;
        public final int       parenDepth;
        public final String    name;       // For `{`/`}` only: pushed/popped construct name, else null
        public       boolean   frozen;     // set by markFrozenSpans; true = opaque pass-through, never transformed

        // JSX_SPAN-only, STATE_JS_TS.md's Step 2 "context 11" scoping session, sub-context 1 --
        // NOT user-facing, additive-only structural data alongside the existing frozen/opaque
        // `text` shape (never consulted by any pre-existing pass). `jsxOpeningTagEndOffset` is the
        // offset into `text` (0 == the span's own leading `<`) of the character immediately after
        // the opening tag's closing `>`/`/>` -- i.e. `text.substring(0, jsxOpeningTagEndOffset)` is
        // exactly the opening tag, attribute list included, nothing from any child/closing tag.
        // `jsxAttrBoundaries` is the parallel list of offsets (same 0-based scheme) where each
        // attribute in that opening tag begins, in source order -- both `-1`/`null` for every
        // non-`JSX_SPAN` token and for a `JSX_SPAN` whose "opening tag" is itself a closing tag
        // (structurally can't happen as a span root, kept `-1` defensively). Populated only by
        // TokenizerCurly#findJsxSpans (Increment 1 of Step 2's 5-increment breakdown --
        // detect-and-measure-only, see STATE_JS_TS.md for the full plan); no other code writes
        // these fields, and reading them has zero effect on any rendered output as of that
        // increment -- consumption is limited to JsxWrapDiagnostics's internal-only measurement
        // hook.
        public int           jsxOpeningTagEndOffset = -1;
        public List<Integer> jsxAttrBoundaries      = null;

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

        /**
         * True iff {@code tokens.get(index)} is the operand immediately following a unary
         *  `-`/`+` at {@code tokens.get(index - 1)} -- i.e. that `-`/`+` is not itself preceded
         *  by another operand (identifier/number/string/char/closing `)`/`]`), which would make
         *  it binary instead. A caller's pairwise (prev, cur) spacing decision has no way to see
         *  the token before `prev`, so without this check a leading/embedded unary sign (e.g.
         *  `int aaa = +1;`, or a ternary's `-1` else-arm) renders with a spurious space
         *  (`= + 1`, `- 1`). Centralized here (RDD_KEY_238 follow-up) after being found
         *  independently duplicated, byte-for-byte identical, in both
         *  {@code DeclarationAlignmentRuleCore} and {@code MiscRuleCore} -- same "duplicated
         *  small token helper" pattern {@link #isPunct}/{@link #isOp}/etc. already centralized
         *  here; this one is a pure function of {@code (tokens, index)} with no per-instance
         *  {@code lang}/config dependency, so unlike the class-refactor's other same-named-helper
         *  duplicates (`setOf`, `isTightToken`'s language-specific bodies), promoting it here is a
         *  same-behavior move, not a new shared-utility-class decision. Both call sites now
         *  delegate to this method; neither keeps its own copy.
         */
        public static boolean isUnaryMinusOperand(final List<Token> tokens, final int index)
        {
            if(index == 0) return false;
            final Token prevTok = tokens.get(index - 1);
            if( !( isOp(prevTok, "-") || isOp(prevTok, "+") ) ) return false;
            if(index - 2 < 0) return true; // Nothing before the sign -- must be unary
            final Token beforeSign = tokens.get(index - 2);

            return !( beforeSign.type == TokenType.IDENTIFIER || beforeSign.type == TokenType.NUMBER
                    || beforeSign.type == TokenType.STRING || beforeSign.type == TokenType.CHAR
                    || isPunct(beforeSign, ")") || isPunct(beforeSign, "]") );
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

    public static final String JXM_CFMT_DIS = "JXM_CFMT_DIS";
    public static final String JXM_CFMT_ENA = "JXM_CFMT_ENA";
    public static final String JXM_CFMT_CFG = "JXM_CFMT_CFG";
    public static final String JXM_CFMT_GDR = "JXM_CFMT_GDR";

    private static final java.util.regex.Pattern FORMAT_DIS_MARKER = java.util.regex.Pattern.compile(
        "^//%\\s*" + JXM_CFMT_DIS + "\\s*$|^/\\*%\\s*" + JXM_CFMT_DIS + "\\s*\\*/$"
    );
    private static final java.util.regex.Pattern FORMAT_ENA_MARKER = java.util.regex.Pattern.compile(
        "^//%\\s*" + JXM_CFMT_ENA + "\\s*$|^/\\*%\\s*" + JXM_CFMT_ENA + "\\s*\\*/$"
    );

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
