/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.tokenizer;

import com.jxmake.formatter.Lang;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Indentation-block-family tokenizer (Python3 -- see STATE_PYTHON3.md). Real lexing of
 * whitespace/newlines/comments/numbers/identifiers-or-keywords/single-line and triple-quoted
 * strings/f-strings/operators/punctuation, mirroring {@link TokenizerCurly}'s generic-emitter
 * usage. Deliberately NOT yet covered (still out of scope until a later checklist item):
 * INDENT/DEDENT synthesis IS now handled -- see {@link #synthesizeIndentation} -- run as a
 * post-pass over the flat token stream produced by {@link #tokenize}'s main loop. `(`/`[`/`{`
 * (see {@link #emitOpenBracket}/{@link #emitCloseBracket}) now track {@code parenDepth} as a
 * single merged bracket-nesting counter (unlike the curly family, Python has no separate
 * scope-vs-expression distinction between `{` and `(`/`[` -- all three suppress logical line
 * breaks identically per the language grammar); {@code braceDepth} is unused/always 0 for this
 * tokenizer. The `:=` walrus operator (PEP 572) IS handled -- see {@link #emitWalrus} -- emitted
 * as a single OP token rather than falling out as `:` PUNCT + `=` OP.
 *
 * <p>F-strings are sub-tokenized following CPython 3.12+'s own FSTRING_START/MIDDLE/END scheme
 * (see {@link #emitFString}): literal text segments become opaque {@code FSTRING_MIDDLE} tokens,
 * each `{...}` field's expression portion is tokenized as ordinary Python tokens (so a later rule
 * pass can apply normal expression-spacing rules per STYLE_PYTHON3.md §5), and an optional
 * `!conversion`/`:format_spec` tail is preserved opaque (format specs are literal, not code; a
 * nested `{expr}` field *within* a format spec is brace-balanced to find the correct end but is
 * NOT itself recursively sub-tokenized -- a known, documented limitation of this slice).
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

    // Valid Python 3 string-prefix letters (case-insensitive, 1-2 chars, e.g. r/b/u/f/rb/br/
    // rf/fr) -- checked against a completed IDENTIFIER token immediately followed by a quote to
    // decide whether that identifier is actually a string prefix rather than an ordinary name.
    private static final Set<String> STRING_PREFIXES = setOf(
            "r", "b", "u", "f", "rb", "br", "rf", "fr");

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
            dispatchToken(tokens);
        }
        return synthesizeIndentation(tokens);
    }

    /** Post-pass over the flat token stream: inserts {@code INDENT}/{@code DEDENT} markers at
     *  the start of each logical line whose indentation width differs from the enclosing block's,
     *  per Python's significant-whitespace grammar. Mirrors CPython's own tokenizer algorithm
     *  (a stack of indent widths, compared line-by-line) without attempting its stricter
     *  tabs/spaces consistency error (`TabError`) -- this formatter assumes syntactically valid
     *  input, same posture as every other language here.
     *
     *  <p>A physical line only starts a new logical line -- and is therefore eligible to open/
     *  close a block -- when: it is not blank, is not comment-only (STYLE_PYTHON3.md: blank and
     *  comment-only lines never affect indentation tracking), the previous line's `NEWLINE` was
     *  not inside an open bracket (`parenDepth > 0`, tracked by {@link #emitOpenBracket}/{@link
     *  #emitCloseBracket}), and the previous line did not end in a backslash line-continuation
     *  (see {@link #isBackslashContinuation}). `INDENT`/`DEDENT` tokens carry no source text of
     *  their own (zero-width markers) but carry the new indent width as their {@code text} field
     *  for any later rule pass that needs it (e.g. the per-block indent-size/style rescaling work
     *  noted as an open question in STATE_PYTHON3.md). Any leading `WHITESPACE` token of the line
     *  is preserved verbatim in the output either way -- this pass only inserts markers, it never
     *  rewrites existing tokens' text. */
    private List<Token> synthesizeIndentation(final List<Token> tokens) {
        final List<Token> out = new ArrayList<>();
        final Deque<Integer> indentStack = new ArrayDeque<>();
        indentStack.push(0);
        boolean atLineStart = true;
        int i = 0;
        final int n = tokens.size();

        while (i < n) {
            if (atLineStart) {
                int width = 0;
                if (tokens.get(i).type == TokenType.WHITESPACE) {
                    width = indentWidth(tokens.get(i).text);
                    out.add(tokens.get(i));
                    i++;
                }
                final boolean blankOrComment = i >= n
                        || tokens.get(i).type == TokenType.NEWLINE
                        || tokens.get(i).type == TokenType.COMMENT_LINE;
                if (!blankOrComment) {
                    if (width > indentStack.peek()) {
                        indentStack.push(width);
                        out.add(new Token(TokenType.INDENT, String.valueOf(width), 0, 0, null));
                    } else {
                        while (indentStack.peek() > width) {
                            indentStack.pop();
                            out.add(new Token(TokenType.DEDENT, String.valueOf(indentStack.peek()),
                                    0, 0, null));
                        }
                    }
                }
                atLineStart = false;
                continue;
            }

            final Token t = tokens.get(i);
            out.add(t);
            i++;
            if (t.type == TokenType.NEWLINE) {
                final boolean insideBrackets = t.parenDepth > 0;
                if (!insideBrackets && !isBackslashContinuation(out)) {
                    atLineStart = true;
                }
            }
        }

        while (indentStack.peek() > 0) {
            indentStack.pop();
            out.add(new Token(TokenType.DEDENT, String.valueOf(indentStack.peek()), 0, 0, null));
        }
        return out;
    }

    /** {@code out}'s last element is a just-appended `NEWLINE` -- true when the token right
     *  before it is a `\` OP (a backslash line-continuation, which per Python grammar must be
     *  the line's final character, making it immediately adjacent to the newline with nothing,
     *  not even whitespace, between). */
    private boolean isBackslashContinuation(final List<Token> out) {
        if (out.size() < 2) {
            return false;
        }
        final Token prev = out.get(out.size() - 2);
        return prev.type == TokenType.OP && "\\".equals(prev.text);
    }

    /** Python's own indentation-width rule (CPython tokenizer): each space advances by 1, each
     *  tab advances to the next multiple of 8, and a form-feed resets the count to 0. Used only
     *  to compare relative indentation levels for INDENT/DEDENT purposes -- not a tabs/spaces
     *  validity check. */
    private static int indentWidth(final String whitespace) {
        int width = 0;
        for (int k = 0; k < whitespace.length(); k++) {
            final char c = whitespace.charAt(k);
            if (c == '\t') {
                width = (width / 8 + 1) * 8;
            } else if (c == '\f') {
                width = 0;
            } else {
                width++;
            }
        }
        return width;
    }

    /** One dispatch step: appends exactly one token to {@code out}, except for a string-prefixed
     *  identifier immediately followed by a quote, which appends the prefix IDENTIFIER plus
     *  either one STRING token (plain/triple-quoted) or the full FSTRING_START/.../FSTRING_END
     *  sequence (f-strings, via {@link #emitFString}). Shared between the top-level {@link
     *  #tokenize} loop and {@link #emitFStringField}'s expression sub-scan so both stay in sync
     *  on identifier/string/number/operator handling. */
    private void dispatchToken(final List<Token> out) {
        final char c = source.charAt(pos);

        if (c == '\r' || c == '\n') {
            out.add(emitNewline());
        } else if (c == ' ' || c == '\t') {
            out.add(emitWhitespace());
        } else if (c == '#') {
            out.add(emitLineComment());
        } else if (isIdentifierStart(c)) {
            final Token idTok = emitIdentifierOrKeyword();
            final boolean followedByQuote = pos < length
                    && (source.charAt(pos) == '"' || source.charAt(pos) == '\'');
            if (idTok.type == TokenType.IDENTIFIER && followedByQuote
                    && STRING_PREFIXES.contains(idTok.text.toLowerCase(java.util.Locale.ROOT))) {
                out.add(idTok);
                final char q = source.charAt(pos);
                final boolean triple = peek(1) == q && peek(2) == q;
                if (idTok.text.toLowerCase(java.util.Locale.ROOT).indexOf('f') >= 0) {
                    out.addAll(emitFString(q, triple));
                } else if (triple) {
                    out.add(emitTripleQuotedString(q));
                } else {
                    out.add(emitSimpleString(q));
                }
            } else {
                out.add(idTok);
            }
        } else if ((c == '"' || c == '\'') && peek(1) == c && peek(2) == c) {
            out.add(emitTripleQuotedString(c));
        } else if (c == '"' || c == '\'') {
            out.add(emitSimpleString(c));
        } else if (Character.isDigit(c) || (c == '.' && Character.isDigit(peek(1)))) {
            out.add(emitNumber());
        } else if (c == ':' && peek(1) == '=') {
            out.add(emitWalrus());
        } else if (c == '(' || c == '[' || c == '{') {
            out.add(emitOpenBracket(c));
        } else if (c == ')' || c == ']' || c == '}') {
            out.add(emitCloseBracket(c));
        } else if (c == ',' || c == ':' || c == ';') {
            out.add(emitPunct(c));
        } else {
            out.add(emitOperator());
        }
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

    /** F-string body (prefix already consumed/emitted by {@link #dispatchToken}), following
     *  CPython 3.12+'s FSTRING_START/MIDDLE/END scheme. Emits FSTRING_START (opening quote(s)),
     *  then alternates FSTRING_MIDDLE literal-text tokens with `{...}` fields (see {@link
     *  #emitFStringField}), then FSTRING_END (closing quote(s)). A doubled `{{`/`}}` is an
     *  escaped literal brace and stays inside the surrounding FSTRING_MIDDLE text verbatim. */
    private List<Token> emitFString(final char quote, final boolean triple) {
        final List<Token> out = new ArrayList<>();
        final int quoteLen = triple ? 3 : 1;
        final int openStart = pos;
        pos += quoteLen;
        out.add(new Token(TokenType.FSTRING_START, source.substring(openStart, pos), braceDepth,
                parenDepth, null));

        int literalStart = pos;
        while (pos < length) {
            final char c = source.charAt(pos);
            if (c == '\\' && pos + 1 < length) {
                pos += 2;
                continue;
            }
            final boolean atClosingQuote = c == quote && (!triple || (peek(1) == quote && peek(2) == quote));
            if (atClosingQuote) {
                if (pos > literalStart) {
                    out.add(new Token(TokenType.FSTRING_MIDDLE, source.substring(literalStart, pos),
                            braceDepth, parenDepth, null));
                }
                final int closeStart = pos;
                pos += quoteLen;
                out.add(new Token(TokenType.FSTRING_END, source.substring(closeStart, pos),
                        braceDepth, parenDepth, null));
                return out;
            }
            if ((c == '{' && peek(1) == '{') || (c == '}' && peek(1) == '}')) {
                pos += 2; // escaped literal brace, stays in the literal text
                continue;
            }
            if (c == '{') {
                if (pos > literalStart) {
                    out.add(new Token(TokenType.FSTRING_MIDDLE, source.substring(literalStart, pos),
                            braceDepth, parenDepth, null));
                }
                out.add(emitPunct('{'));
                emitFStringField(out);
                literalStart = pos;
                continue;
            }
            pos++;
        }
        // Unterminated f-string -- flush whatever literal text remains rather than losing it.
        if (pos > literalStart) {
            out.add(new Token(TokenType.FSTRING_MIDDLE, source.substring(literalStart, pos),
                    braceDepth, parenDepth, null));
        }
        return out;
    }

    /** Scans one f-string `{...}` field's contents -- called with {@code pos} positioned right
     *  after the opening `{` (already emitted by {@link #emitFString}). Tokenizes the expression
     *  portion via {@link #dispatchToken} (same dispatch the top-level tokenizer uses, so nested
     *  strings/f-strings/brackets/numbers all work normally), tracking a local bracket depth so a
     *  nested `:`/`}` (e.g. a dict literal or slice inside the expression) isn't mistaken for the
     *  field's own format-spec separator or closing brace. At depth 0, a `!` immediately followed
     *  by a conversion letter (`r`/`s`/`a`) and then `:`/`}` starts an opaque conversion OP token;
     *  a `:` starts an opaque {@link #emitFStringFormatSpec}. Consumes and emits the field's
     *  closing `}` before returning. */
    private void emitFStringField(final List<Token> out) {
        int depth = 0;
        while (pos < length) {
            final char c = source.charAt(pos);
            if (depth == 0 && c == '}') {
                out.add(emitPunct('}'));
                return;
            }
            if (depth == 0 && c == '!' && "rsa".indexOf(peek(1)) >= 0
                    && (peek(2) == ':' || peek(2) == '}')) {
                final int start = pos;
                pos += 2;
                out.add(new Token(TokenType.OP, source.substring(start, pos), braceDepth,
                        parenDepth, null));
                continue;
            }
            if (depth == 0 && c == ':') {
                out.add(emitFStringFormatSpec());
                continue;
            }
            if (c == '(' || c == '[' || c == '{') {
                depth++;
            } else if (c == ')' || c == ']' || c == '}') {
                depth--;
            }
            dispatchToken(out);
        }
    }

    /** Opaque `:format_spec` tail of an f-string field (STYLE_PYTHON3.md §5: "format spec is
     *  opaque" -- a literal spec string, not code, preserved exactly as written). Brace-balances
     *  to find the field's true closing `}` (a format spec may itself contain a nested `{expr}`
     *  replacement field per Python grammar), but does NOT recursively sub-tokenize that nested
     *  field -- a documented limitation of this slice, consistent with the class javadoc. */
    private Token emitFStringFormatSpec() {
        final int start = pos;
        pos++; // ':'
        int depth = 0;
        while (pos < length) {
            final char c = source.charAt(pos);
            if (c == '{') {
                depth++;
                pos++;
            } else if (c == '}') {
                if (depth == 0) {
                    break;
                }
                depth--;
                pos++;
            } else {
                pos++;
            }
        }
        return new Token(TokenType.FSTRING_FORMAT_SPEC, source.substring(start, pos), braceDepth,
                parenDepth, null);
    }

    private Token emitPunct(final char c) {
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    /** `(`/`[`/`{` all merge into one bracket-nesting counter (unlike the curly family, Python
     *  attaches no separate scope meaning to `{` -- it's only ever a dict/set literal), stored in
     *  the inherited {@code parenDepth} field. Any of the three suppresses logical-line/NEWLINE
     *  significance identically, which is what {@link #synthesizeIndentation} relies on. */
    private Token emitOpenBracket(final char c) {
        parenDepth++;
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    /** Counterpart to {@link #emitOpenBracket}. Guards against going negative on unbalanced/
     *  syntactically invalid input rather than throwing -- this tokenizer assumes valid input but
     *  shouldn't corrupt {@code parenDepth} for the rest of the file if that assumption is ever
     *  violated. */
    private Token emitCloseBracket(final char c) {
        if (parenDepth > 0) {
            parenDepth--;
        }
        pos++;
        return new Token(TokenType.PUNCT, String.valueOf(c), braceDepth, parenDepth, null);
    }

    /** The `:=` walrus operator (PEP 572), emitted as a single OP token rather than falling out
     *  as separate `:` PUNCT + `=` OP -- dispatched from {@link #tokenize} before the general
     *  punct branch since `:` is otherwise claimed by {@link #emitPunct}. */
    private Token emitWalrus() {
        final int start = pos;
        pos += 2;
        return new Token(TokenType.OP, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }

    private Token emitOperator() {
        final int start = pos;
        pos++;
        return new Token(TokenType.OP, source.substring(start, pos), braceDepth, parenDepth,
                null);
    }
}
