/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.tokenizer;

import com.jxmake.formatter.Lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Indentation-block-family tokenizer (Python3 -- see STATE_PYTHON3.md). Real lexing of
 * whitespace/newlines/comments/numbers/identifiers-or-keywords/single-line and triple-quoted
 * strings/operators/punctuation, mirroring {@link TokenizerCurly}'s generic-emitter usage.
 * Deliberately NOT yet covered (still out of scope until a later checklist item): f-string
 * interpolation-boundary sub-tokenization (a prefixed string is lexed as identifier-then-opaque-
 * string, `{...}` interior not split out), the `:=` walrus operator's own OP text (currently
 * falls out of {@link #emitOperator} as plain `:` then `=` -- fine for opaque pass-through, wrong
 * once assignment-alignment rules need to recognize walrus as one unit), and INDENT/DEDENT
 * synthesis (Python's indentation is significant/load-bearing -- see STATE_PYTHON3.md's Open
 * Questions -- this slice only emits {@code WHITESPACE}/{@code NEWLINE} tokens verbatim, same as
 * every other family, with no structural depth tracking yet).
 */
public class TokenizerIndent extends TokenizerCore {

    private static Set<String> setOf(final String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    // STYLE_PYTHON3.md targets latest Python 3 (3.15+); includes soft keywords `match`/`case`/
    // `_`/`type` is deliberately excluded here -- those are context-sensitive identifiers, not
    // unconditional keywords, and misclassifying them as KEYWORD would break their use as
    // ordinary identifiers elsewhere (`match = 1` is valid Python). Left as plain IDENTIFIER;
    // any soft-keyword-aware logic belongs in a rule pass, not the tokenizer.
    private static final Set<String> KEYWORDS_PYTHON = setOf(
            "False", "None", "True", "and", "as", "assert", "async", "await", "break", "class",
            "continue", "def", "del", "elif", "else", "except", "finally", "for", "from",
            "global", "if", "import", "in", "is", "lambda", "nonlocal", "not", "or", "pass",
            "raise", "return", "try", "while", "with", "yield");

    private final Lang lang;

    public TokenizerIndent(final Lang lang) {
        this.lang = lang;
    }

    public List<Token> tokenize(final String source) {
        this.source = source;
        this.pos = 0;
        this.length = source.length();
        this.braceDepth = 0;
        this.parenDepth = 0;

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

            final Token t;
            if (c == '#') {
                t = emitLineComment();
            } else if ((c == '"' || c == '\'') && peek(1) == c && peek(2) == c) {
                t = emitTripleQuotedString(c);
            } else if (c == '"' || c == '\'') {
                t = emitSimpleString(c);
            } else if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek(1)))) {
                t = emitNumber();
            } else if (isIdentifierStart(c)) {
                t = emitIdentifierOrKeyword();
            } else if (c == '(' || c == '[' || c == '{' || c == ')' || c == ']' || c == '}'
                    || c == ',' || c == ':' || c == ';') {
                t = emitPunct(c);
            } else {
                t = emitOperator();
            }

            tokens.add(t);
        }

        return tokens;
    }

    /** Python identifiers never include `$` (unlike the curly family's JS/TS-driven
     *  {@link TokenizerCore#isIdentifierStart}); overridden here so a stray `$` (not valid
     *  Python syntax at all) falls through to {@link #emitOperator} as an opaque single
     *  character rather than silently starting an identifier scan. */
    @Override
    protected boolean isIdentifierStart(final char c) {
        return Character.isLetter(c) || c == '_';
    }

    @Override
    protected boolean isIdentifierPart(final char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private Token emitIdentifierOrKeyword() {
        final int start = pos;
        while (pos < length && isIdentifierPart(source.charAt(pos))) {
            pos++;
        }
        final String text = source.substring(start, pos);
        final TokenType type = KEYWORDS_PYTHON.contains(text) ? TokenType.KEYWORD
                : TokenType.IDENTIFIER;
        return new Token(type, text, braceDepth, parenDepth, null);
    }

    private Token emitLineComment() {
        final int start = pos;
        while (pos < length && source.charAt(pos) != '\r' && source.charAt(pos) != '\n') {
            pos++;
        }
        return new Token(TokenType.COMMENT_LINE, source.substring(start, pos), braceDepth,
                parenDepth, null);
    }

    /** Single-line, single/double-quoted string literal (the tokenize loop dispatches the
     *  triple-quoted case to {@link #emitTripleQuotedString} before reaching here -- see class
     *  javadoc for what's still out of scope, e.g. f-string interpolation). Any string-prefix
     *  letters (`r`/`b`/`f`/`u`, any case/combination) were already consumed as a leading
     *  IDENTIFIER token by the tokenize loop's normal dispatch before this method is reached;
     *  this method only handles the quoted body starting at the quote character itself. */
    private Token emitSimpleString(final char quote) {
        final int start = pos;
        pos++; // opening quote
        while (pos < length) {
            final char c = source.charAt(pos);
            if (c == '\\' && pos + 1 < length) {
                pos += 2;
                continue;
            }
            if (c == quote) {
                pos++;
                break;
            }
            if (c == '\r' || c == '\n') {
                break; // unterminated on this line -- stop, don't swallow the newline
            }
            pos++;
        }
        return new Token(TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    /** Triple-quoted string/docstring (RDD_KEY_186: opaque, preserved verbatim beyond the
     *  opening `"""`/`'''`, may span multiple lines/embed the other quote character singly or
     *  doubly). Emitted as one {@code STRING} token including any embedded newlines -- callers
     *  that need per-line indentation info must not assume one token is one line, same
     *  precedent as {@link TokenizerCurly}'s block comments/text blocks. */
    private Token emitTripleQuotedString(final char quote) {
        final int start = pos;
        pos += 3; // opening triple-quote
        while (pos < length) {
            final char c = source.charAt(pos);
            if (c == '\\' && pos + 1 < length) {
                pos += 2;
                continue;
            }
            if (c == quote && peek(1) == quote && peek(2) == quote) {
                pos += 3;
                break;
            }
            pos++;
        }
        return new Token(TokenType.STRING, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    private Token emitPunct(final char c) {
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    private Token emitOperator() {
        final int start = pos;
        pos++;
        return new Token(TokenType.OP, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }
}
