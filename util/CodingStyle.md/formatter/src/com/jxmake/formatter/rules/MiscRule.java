/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catch-all for the remaining generic STYLE.md sections not owned by another rule class:
 * §1, §2, §3.2, §3.3, §4, §6, §8, §9, §15.
 */
public class MiscRule {

    private static final Set<String> TIGHT_PAREN_KEYWORDS =
            setOf("if", "while", "for", "switch");

    private final String language;

    public MiscRule(final String language) {
        this.language = language;
    }

    private static Set<String> setOf(final String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    // ── §3.2 Keyword spacing ─────────────────────────────────────────────────────
    /**
     * Collapses any whitespace-only gap between a control-flow keyword (`if`/`while`/`for`/
     * `switch` -- exactly STYLE.md §3.2's four keywords) and its following `(` down to zero
     * width. A comment or a `NEWLINE` in the gap blocks the rewrite for that occurrence, same
     * conservative posture as `BlockStructureRule`'s brace-style passes -- relocating a comment
     * unambiguously is out of scope. This method never touches what's *inside* the `(...)`;
     * deciding whether the contents are padded is STYLE.md §3.1, already implemented as the
     * `isLoose` evaluation in `ComplexityPaddingEvaluator` -- wiring that evaluation into an
     * actual rewrite pass is separate, not-yet-assigned work.
     */
    public String enforceKeywordSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        Token lastSignificant = null;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean collapse = isPunct(t, "(") && lastSignificant != null
                    && lastSignificant.type == TokenType.KEYWORD
                    && TIGHT_PAREN_KEYWORDS.contains(lastSignificant.text)
                    && gap.stream().noneMatch(this::isCommentOrNewline);
            if (!collapse) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    // ── §3.3 `{}` initializer / block spacing ───────────────────────────────────
    /**
     * Normalizes spacing inside brace-initializer lists (array/struct initializers, `= { ... }`
     * contexts) per STYLE.md §3.3: empty `{}` is always tight; non-empty `{ ... }` gets exactly
     * one space just inside both the opening and closing brace, at every nesting level.
     * Control-flow and function/class body braces (STYLE.md §11/§7, already handled by
     * `BlockStructureRule`) are never touched -- those braces are never directly preceded by
     * `=`, `{`, or `,` while nested inside an already-recognized initializer, which is the
     * structural signal this method uses to recognize an initializer brace (no AST available;
     * confirmed against `BlockStructureRule.qualifiesForKAndR` that an initializer brace, whose
     * preceding token is `=`/`{`/`,`, never matches that method's K&R/lambda criteria, so the
     * two rules never fight over the same brace). A comment or `NEWLINE` immediately inside a
     * brace blocks the rewrite for that side, same conservative posture as the rest of this
     * file -- a genuinely multi-line initializer is left untouched.
     */
    public String enforceInitializerBraceSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        final Deque<Boolean> initStack = new ArrayDeque<>();
        Token lastSignificant = null;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean afterInitOpen = isPunct(lastSignificant, "{")
                    && !initStack.isEmpty() && initStack.peek();
            final boolean beforeInitClose = isPunct(t, "}")
                    && !initStack.isEmpty() && initStack.peek();
            final boolean gapHasBlocker = gap.stream().anyMatch(this::isCommentOrNewline);

            if (afterInitOpen && isPunct(t, "}")) {
                gap.clear();
            } else if ((afterInitOpen || beforeInitClose) && !gapHasBlocker) {
                out.append(' ');
                gap.clear();
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
                gap.clear();
            }

            if (isPunct(t, "{")) {
                final boolean isInit = isOp(lastSignificant, "=")
                        || ((isPunct(lastSignificant, "{") || isPunct(lastSignificant, ","))
                                && !initStack.isEmpty() && initStack.peek());
                initStack.push(isInit);
            } else if (isPunct(t, "}") && !initStack.isEmpty()) {
                initStack.pop();
            }

            out.append(t.text);
            lastSignificant = t;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    // ── §4 Pre/Post Increment and Decrement ─────────────────────────────────────
    /**
     * Rewrites a bare postfix `i++;`/`i--;` expression statement (its value discarded) to
     * prefix `++i;`/`--i;`, per STYLE.md §4's "always prefer pre-increment except when post-
     * increment semantics are required by the surrounding expression" rule. Two shapes are
     * recognized, both via plain token-position checks (no AST):
     *   (1) A standalone statement: the IDENTIFIER is immediately preceded (ignoring a
     *       whitespace/comment/newline gap) by a statement boundary (`;`, `{`, `}`, or the
     *       start of the scope) and immediately followed by `;`, with paren/bracket depth 0 at
     *       that point -- this excludes a function call's arguments, an array index, and a
     *       for-loop header's own clauses (all "value used" or otherwise-handled contexts).
     *       A brace-less single-statement control-flow body (`if(x) i++;`) is NOT recognized as
     *       a boundary here -- that would require statement-start detection well beyond what
     *       STYLE.md's own "i++; at statement level" framing asks for, so it is left untouched
     *       as a documented gap rather than guessed at.
     *   (2) A `for(...; ...; i++)` loop's increment clause -- found by locating each `for`
     *       keyword's matching parens and splitting on the two top-level `;` inside; the third
     *       clause is rewritten when (and only when) it consists of exactly one IDENTIFIER
     *       followed by `++`/`--` and nothing else, since STYLE.md frames "prefer pre" as the
     *       general rule with exceptions only for value-is-used cases, and a discarded loop
     *       increment is not one of those. A more complex increment clause (comma-separated,
     *       `arr[i++]`, etc.) is left untouched -- STYLE.md gives no worked example beyond the
     *       single bare `i++`/`i--` shape.
     * Any candidate whose identifier/operator gap contains a comment or a `NEWLINE` is left
     * untouched, same conservative posture as the rest of this file.
     */
    public String enforcePreIncrement(final List<Token> tokens) {
        final Map<Integer, Integer> spans = new HashMap<>();
        collectBareStatementSpans(tokens, spans);
        collectForIncrementSpans(tokens, spans);
        return renderWithSwappedSpans(tokens, spans);
    }

    private void collectBareStatementSpans(final List<Token> tokens, final Map<Integer, Integer> spans) {
        final int n = tokens.size();
        int depth = 0;
        Token lastSignificant = null;

        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                continue;
            }
            if (isPunct(t, "(") || isPunct(t, "[")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]")) {
                depth--;
            } else if (depth == 0 && t.type == TokenType.IDENTIFIER && isStatementBoundary(lastSignificant)) {
                final int opIdx = nextSignificantIndex(tokens, i + 1);
                if (opIdx >= 0 && isIncrementOp(tokens.get(opIdx)) && noBlockerBetween(tokens, i, opIdx)) {
                    final int termIdx = nextSignificantIndex(tokens, opIdx + 1);
                    if (termIdx >= 0 && isPunct(tokens.get(termIdx), ";")) {
                        spans.put(i, opIdx);
                    }
                }
            }
            lastSignificant = t;
        }
    }

    private void collectForIncrementSpans(final List<Token> tokens, final Map<Integer, Integer> spans) {
        final int n = tokens.size();

        for (int i = 0; i < n; i++) {
            if (tokens.get(i).type != TokenType.KEYWORD || !"for".equals(tokens.get(i).text)) {
                continue;
            }
            final int openParen = nextSignificantIndex(tokens, i + 1);
            if (openParen < 0 || !isPunct(tokens.get(openParen), "(")) {
                continue;
            }
            final int closeParen = matchParenForward(tokens, openParen);
            if (closeParen < 0) {
                continue;
            }

            final List<Integer> semiIdx = new ArrayList<>();
            int depth = 0;
            for (int k = openParen + 1; k < closeParen; k++) {
                final Token tk = tokens.get(k);
                if (isPunct(tk, "(") || isPunct(tk, "[")) {
                    depth++;
                } else if (isPunct(tk, ")") || isPunct(tk, "]")) {
                    depth--;
                } else if (depth == 0 && isPunct(tk, ";")) {
                    semiIdx.add(k);
                }
            }
            if (semiIdx.size() != 2) {
                continue;
            }

            final int incrStart = nextSignificantIndex(tokens, semiIdx.get(1) + 1);
            if (incrStart < 0 || incrStart >= closeParen || tokens.get(incrStart).type != TokenType.IDENTIFIER) {
                continue;
            }
            final int opIdx = nextSignificantIndex(tokens, incrStart + 1);
            if (opIdx < 0 || opIdx >= closeParen || !isIncrementOp(tokens.get(opIdx))
                    || !noBlockerBetween(tokens, incrStart, opIdx)) {
                continue;
            }
            final int afterOp = nextSignificantIndex(tokens, opIdx + 1);
            if (afterOp != closeParen) {
                continue;
            }
            spans.put(incrStart, opIdx);
        }
    }

    private String renderWithSwappedSpans(final List<Token> tokens, final Map<Integer, Integer> spans) {
        final StringBuilder out = new StringBuilder();
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Integer opIdx = spans.get(i);
            if (opIdx != null) {
                out.append(tokens.get(opIdx).text).append(tokens.get(i).text);
                i = opIdx + 1;
                continue;
            }
            out.append(tokens.get(i).text);
            i++;
        }
        return out.toString();
    }

    private int matchParenForward(final List<Token> tokens, final int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), "(")) {
                depth++;
            } else if (isPunct(tokens.get(i), ")")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isStatementBoundary(final Token t) {
        return t == null || isPunct(t, ";") || isPunct(t, "{") || isPunct(t, "}");
    }

    private boolean isIncrementOp(final Token t) {
        return t.type == TokenType.OP && ("++".equals(t.text) || "--".equals(t.text));
    }

    private boolean noBlockerBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            if (isCommentOrNewline(tokens.get(i))) {
                return false;
            }
        }
        return true;
    }

    private int nextSignificantIndex(final List<Token> tokens, final int from) {
        int i = from;
        while (i < tokens.size() && isGapToken(tokens.get(i))) {
            i++;
        }
        return i < tokens.size() ? i : -1;
    }

    // ── Token-scanning helpers ───────────────────────────────────────────────────
    private boolean isGapToken(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    private boolean isCommentOrNewline(final Token t) {
        return t.type == TokenType.NEWLINE || t.type == TokenType.COMMENT_LINE
                || t.type == TokenType.COMMENT_BLOCK;
    }

    private boolean isPunct(final Token t, final String text) {
        return t != null && t.type == TokenType.PUNCT && text.equals(t.text);
    }

    private boolean isOp(final Token t, final String text) {
        return t != null && t.type == TokenType.OP && text.equals(t.text);
    }
}
