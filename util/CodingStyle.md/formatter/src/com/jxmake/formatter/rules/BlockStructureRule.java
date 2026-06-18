/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockStructureRule {

    private static final Set<String> SINGLE_EXPR_KEYWORDS = setOf("if", "while", "for");

    // A nested compound construct is not a "single EXPRESSION" (STYLE.md §10's own title) --
    // collapsing e.g. `if(x) { if(y) foo(); }` to `if(x) if(y) foo();` would introduce a
    // dangling-construct ambiguity that the worked examples (return/continue/break) never
    // exercise, so such bodies are left braced rather than guessed at.
    private static final Set<String> COMPOUND_BODY_KEYWORDS =
            setOf("if", "while", "for", "switch", "do", "try");

    private final String language;

    public BlockStructureRule(final String language) {
        this.language = language;
    }

    private static Set<String> setOf(final String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    // ── Single-expression blocks (STYLE.md §10) ─────────────────────────────────
    /**
     * Scans a token slice and rewrites every `if`/`while`/`for` whose controlled body is a
     * single statement (`if(x) return y;`, `if(x) continue;`, `if(x) break;`) from braced,
     * possibly multi-line form into the brace-less single-line form. Bodies that already
     * omit braces, hold more than one statement, or are themselves a nested compound
     * construct are left untouched verbatim, byte-for-byte. Everything outside a qualifying
     * body -- including unrelated tokens, whitespace, and comments -- is passed through
     * unchanged.
     */
    public String collapseSingleExpressionBlocks(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.KEYWORD && SINGLE_EXPR_KEYWORDS.contains(t.text)) {
                final ControlBlock block = matchControlBlock(tokens, i);
                if (block != null && block.openBraceIndex >= 0) {
                    final String collapsed = tryCollapse(tokens, i, block);
                    if (collapsed != null) {
                        out.append(collapsed);
                        i = block.closeBraceIndex + 1;
                        continue;
                    }
                }
            }
            out.append(t.text);
            i++;
        }

        return out.toString();
    }

    /** Token-index span of an `if`/`while`/`for` condition and, if present, its braced body. */
    private static final class ControlBlock {
        final int closeParenIndex;
        final int openBraceIndex;  // -1 if the body already has no braces
        final int closeBraceIndex; // meaningless when openBraceIndex == -1

        ControlBlock(final int closeParenIndex, final int openBraceIndex,
                final int closeBraceIndex) {
            this.closeParenIndex = closeParenIndex;
            this.openBraceIndex = openBraceIndex;
            this.closeBraceIndex = closeBraceIndex;
        }
    }

    /**
     * Locates the `( ... )` condition following the keyword at {@code kwIndex}, and the
     * `{ ... }` body after it if one is present. Bracket matching is local depth counting
     * (mirrors `DeclarationAlignmentRule`'s `[`/`]` matching) rather than relying on the
     * tokenizer's running depth fields, since this method must work on any bounded slice.
     * Returns null on unbalanced brackets -- caller leaves the input untouched.
     */
    private ControlBlock matchControlBlock(final List<Token> tokens, final int kwIndex) {
        final int n = tokens.size();
        int i = skipNonSignificant(tokens, kwIndex + 1);
        if (i >= n || !isPunct(tokens.get(i), "(")) {
            return null;
        }

        int depth = 1;
        i++;
        while (i < n && depth > 0) {
            final Token tk = tokens.get(i);
            if (isPunct(tk, "(") || isPunct(tk, "[")) {
                depth++;
            } else if (isPunct(tk, ")") || isPunct(tk, "]")) {
                depth--;
            }
            i++;
        }
        if (depth != 0) {
            return null;
        }
        final int closeParen = i - 1;

        final int afterParen = skipNonSignificant(tokens, closeParen + 1);
        if (afterParen >= n || !isPunct(tokens.get(afterParen), "{")) {
            return new ControlBlock(closeParen, -1, -1);
        }

        int bdepth = 1;
        int j = afterParen + 1;
        while (j < n && bdepth > 0) {
            final Token tk = tokens.get(j);
            if (isPunct(tk, "{")) {
                bdepth++;
            } else if (isPunct(tk, "}")) {
                bdepth--;
            }
            j++;
        }
        if (bdepth != 0) {
            return null;
        }

        return new ControlBlock(closeParen, afterParen, j - 1);
    }

    /**
     * Returns the collapsed single-line rendering of the keyword/condition plus the block's
     * lone statement, or null if the body does not qualify for §10 omission: more than one
     * top-level `;`, trailing content after the sole `;` other than a comment, an interleaved
     * comment before the statement, or a nested compound construct as the body.
     */
    private String tryCollapse(final List<Token> tokens, final int kwIndex,
            final ControlBlock block) {
        final List<Token> contents = tokens.subList(block.openBraceIndex + 1,
                block.closeBraceIndex);

        final List<Token> sig = new ArrayList<>();
        for (final Token t : contents) {
            if (t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) {
                sig.add(t);
            }
        }
        if (sig.isEmpty()) {
            return null;
        }

        int semiCount = 0;
        int semiIdx = -1;
        for (int k = 0; k < sig.size(); k++) {
            if (isPunct(sig.get(k), ";")) {
                semiCount++;
                semiIdx = k;
            }
        }
        if (semiCount != 1) {
            return null;
        }

        for (int k = semiIdx + 1; k < sig.size(); k++) {
            final TokenType ty = sig.get(k).type;
            if (ty != TokenType.COMMENT_LINE && ty != TokenType.COMMENT_BLOCK) {
                return null;
            }
        }
        for (int k = 0; k < semiIdx; k++) {
            final TokenType ty = sig.get(k).type;
            if (ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) {
                return null;
            }
        }

        final Token first = sig.get(0);
        if (first.type == TokenType.KEYWORD && COMPOUND_BODY_KEYWORDS.contains(first.text)) {
            return null;
        }

        final String prefix = renderInline(tokens.subList(kwIndex, block.closeParenIndex + 1));
        final String body = renderInline(contents);
        return prefix + " " + body;
    }

    /** Joins tokens onto one line: any run of whitespace/newlines between tokens becomes one space. */
    private String renderInline(final List<Token> tokens) {
        final StringBuilder sb = new StringBuilder();
        boolean pendingSpace = false;
        for (final Token t : tokens) {
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                if (sb.length() > 0) {
                    pendingSpace = true;
                }
                continue;
            }
            if (pendingSpace) {
                sb.append(' ');
                pendingSpace = false;
            }
            sb.append(t.text);
        }
        return sb.toString();
    }

    private int skipNonSignificant(final List<Token> tokens, final int from) {
        final int n = tokens.size();
        int i = from;
        while (i < n) {
            final TokenType ty = tokens.get(i).type;
            if (ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE
                    || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) {
                i++;
            } else {
                break;
            }
        }
        return i;
    }

    private boolean isPunct(final Token t, final String text) {
        return t.type == TokenType.PUNCT && text.equals(t.text);
    }
}
