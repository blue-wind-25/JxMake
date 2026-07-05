/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.evaluator.ComplexityPaddingEvaluator;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
            setOf("if", "while", "for", "switch", "catch");

    // Keywords that must never be titlecased when they start a comment sentence, split by the
    // language they actually belong to -- a C/C++-only keyword like `inline` must not suppress
    // capitalization in a Java comment, and vice versa.
    private static final Set<String> COMMENT_NO_CAPITALIZE_C = setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double",
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long",
            "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct",
            "switch", "typedef", "union", "unsigned", "void", "volatile", "while");

    private static final Set<String> COMMENT_NO_CAPITALIZE_CPP = setOf(
            "alignas", "alignof", "asm", "bool", "catch", "char16_t", "char32_t", "class",
            "co_await", "co_return", "co_yield", "concept", "consteval", "constexpr", "constinit",
            "const_cast", "decltype", "delete", "dynamic_cast", "explicit", "export", "false",
            "final", "friend", "mutable", "namespace", "new", "noexcept", "nullptr", "operator",
            "override", "private", "protected", "public", "reinterpret_cast", "requires",
            "static_assert", "static_cast", "template", "this", "thread_local", "throw", "true",
            "try", "typeid", "typename", "using", "virtual", "wchar_t");

    private static final Set<String> COMMENT_NO_CAPITALIZE_JAVA = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "else", "enum", "extends", "final", "finally",
            "for", "goto", "if", "implements", "import", "instanceof", "interface", "native",
            "new", "package", "permits", "private", "protected", "public", "record", "return",
            "sealed", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "try", "var", "void", "volatile", "while", "yield", "null",
            "true", "false");

    private final Lang lang;
    private final boolean normalizeCommentStartCase;
    private final boolean normalizeCommentEndPeriod;
    public final int indentWidth;
    public final int lineLengthLimit;

    public MiscRule(final Lang lang, final boolean normalizeCommentStartCase,
            final boolean normalizeCommentEndPeriod) {
        this(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, DEFAULT_INDENT_WIDTH,
                DEFAULT_LINE_LENGTH_LIMIT);
    }

    public MiscRule(final Lang lang, final boolean normalizeCommentStartCase,
            final boolean normalizeCommentEndPeriod, final int indentWidth, final int lineLengthLimit) {
        this.lang = lang;
        this.normalizeCommentStartCase = normalizeCommentStartCase;
        this.normalizeCommentEndPeriod = normalizeCommentEndPeriod;
        this.indentWidth = indentWidth;
        this.lineLengthLimit = lineLengthLimit;
    }

    private static Set<String> setOf(final String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    // ── §1 Indentation ───────────────────────────────────────────────────────────
    /** Tab display size and spaces-per-level default, per STYLE.md §1 -- overridable via the
     *  `indent-size` config key (see {@link #indentWidth}). Shared by any rule (this one or a
     *  future one, e.g. §8's signature wrapping) that needs to *generate* new indentation. */
    public static final int DEFAULT_INDENT_WIDTH = 4;

    /**
     * Converts every line's leading indentation run to the requested style, per STYLE.md §1's
     * `indent-style = spaces | tabs` modes (resolved -- see "§1 indentation scope" in Resolved
     * Design Decisions). `indent-style = auto` is deliberately not handled here: it requires
     * whole-project context to determine the dominant style, which is a `Main.java`/
     * `Config.java`-orchestration-time decision made by a separate, not-yet-built detector class
     * -- that class is expected to resolve "auto" down to a concrete `spaces`/`tabs` choice and
     * call this method with that choice, so this method itself never has to interpret "auto".
     * Only the whitespace run at the very start of each line is touched; whitespace elsewhere
     * (mid-line alignment padding, trailing whitespace) is never indentation. A line whose
     * indentation width (tabs expanded at {@link #indentWidth}) is not an exact multiple of
     * {@link #indentWidth} is irregular/malformed indentation and is left completely untouched
     * rather than guessed at.
     */
    public String convertIndentation(final List<Token> tokens, final String indentStyle) {
        if (!"spaces".equals(indentStyle) && !"tabs".equals(indentStyle)) {
            throw new IllegalArgumentException("convertIndentation only handles spaces|tabs, got: " + indentStyle);
        }
        final StringBuilder out = new StringBuilder();
        boolean atLineStart = true;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (atLineStart && t.type == TokenType.WHITESPACE && !t.frozen) {
                out.append(renderIndent(t.text, indentStyle));
                atLineStart = false;
                i++;
                continue;
            }
            out.append(t.text);
            atLineStart = (t.type == TokenType.NEWLINE);
            i++;
        }
        return out.toString();
    }

    private String renderIndent(final String original, final String indentStyle) {
        int width = 0;
        for (int i = 0; i < original.length(); i++) {
            width += (original.charAt(i) == '\t') ? (indentWidth - (width % indentWidth)) : 1;
        }
        if (width % indentWidth != 0) {
            return original;
        }
        return indentText(width / indentWidth, indentStyle);
    }

    /** Renders `level` indent levels in the requested style -- shared by §1's line converter
     *  above and §8's signature-wrapping below, which both need to *generate* brand-new
     *  indentation (as opposed to converting indentation that already exists in source). */
    private String indentText(final int level, final String indentStyle) {
        final boolean tabs = "tabs".equals(indentStyle);
        final char unit = tabs ? '\t' : ' ';
        final int count = tabs ? level : level * indentWidth;
        final StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(unit);
        }
        return sb.toString();
    }

    // ── §2 Line Length ───────────────────────────────────────────────────────────
    /**
     * STYLE.md §2's 100-char soft limit default -- overridable via the `line-length` config key
     * (see {@link #lineLengthLimit}). No rule in this class acts on it directly: §2 itself
     * defers its only described mechanical fix (line-breaking) to §8 (Function Signatures, not
     * yet implemented), and describes no other mechanical rewrite for an over-length line --
     * §2 is therefore a no-op section here beyond exposing this constant for §8's eventual use.
     */
    public static final int DEFAULT_LINE_LENGTH_LIMIT = 100;

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
                    && gap.stream().noneMatch(this::isCommentOrNewline)
                    && !t.frozen && !lastSignificant.frozen && gap.stream().noneMatch(g -> g.frozen);
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

    // ── §3.1 Condition complexity padding ───────────────────────────────────────
    private static final ComplexityPaddingEvaluator COMPLEXITY_EVALUATOR = new ComplexityPaddingEvaluator();

    /**
     * Pads or tightens every `(...)` and `[...]` in the token stream per STYLE.md §3.1: exactly
     * one space just inside both delimiters when {@link ComplexityPaddingEvaluator#isLoose} reports
     * the content "loose" (a nested `(`/`[` is present anywhere in it), or zero width (tight)
     * otherwise. Applies universally -- not limited to control-flow conditions. One exclusion:
     * a `(` whose immediately preceding significant token is an IDENTIFIER and whose matching `)`
     * is followed by `{`, `->`, or a trailing qualifier (`const`/`override`/`noexcept`/`throws`/
     * `final`) is a function/method definition parameter list and is left untouched. A comment or
     * `NEWLINE` in a given side's own gap blocks the rewrite for that side only.
     */
    public String enforceComplexityPadding(final List<Token> tokens) {
        final Map<Integer, Boolean> looseByOpenIdx = new HashMap<>();
        final Map<Integer, Boolean> looseByCloseIdx = new HashMap<>();
        final int n = tokens.size();

        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(")) {
                final int prevSigIdx = prevSignificantIndex(tokens, i - 1);
                if (prevSigIdx >= 0 && tokens.get(prevSigIdx).type == TokenType.IDENTIFIER) {
                    final int closeIdx = matchParenForward(tokens, i);
                    if (closeIdx < 0) {
                        continue;
                    }
                    final int afterCloseIdx = nextSignificantIndex(tokens, closeIdx + 1);
                    if (afterCloseIdx >= 0 && isFunctionDefFollower(tokens.get(afterCloseIdx))) {
                        continue;
                    }
                    final boolean loose = COMPLEXITY_EVALUATOR.isLoose(tokens.subList(i + 1, closeIdx));
                    looseByOpenIdx.put(i, loose);
                    looseByCloseIdx.put(closeIdx, loose);
                } else {
                    final int closeIdx = matchParenForward(tokens, i);
                    if (closeIdx < 0) {
                        continue;
                    }
                    final boolean loose = COMPLEXITY_EVALUATOR.isLoose(tokens.subList(i + 1, closeIdx));
                    looseByOpenIdx.put(i, loose);
                    looseByCloseIdx.put(closeIdx, loose);
                }
            } else if (isPunct(t, "[")) {
                final int closeIdx = matchBracketForward(tokens, i);
                if (closeIdx < 0) {
                    continue;
                }
                final boolean loose = COMPLEXITY_EVALUATOR.isLoose(tokens.subList(i + 1, closeIdx));
                looseByOpenIdx.put(i, loose);
                looseByCloseIdx.put(closeIdx, loose);
            }
        }

        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        Token lastSignificant = null;
        int lastSignificantIdx = -1;
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean lastIsOpen = isPunct(lastSignificant, "(") || isPunct(lastSignificant, "[");
            final boolean curIsClose = isPunct(t, ")") || isPunct(t, "]");
            final Boolean afterOpen = lastIsOpen ? looseByOpenIdx.get(lastSignificantIdx) : null;
            final Boolean beforeClose = curIsClose ? looseByCloseIdx.get(i) : null;
            final boolean gapHasBlocker = gap.stream().anyMatch(this::isCommentOrNewline)
                    || t.frozen || (lastSignificant != null && lastSignificant.frozen)
                    || gap.stream().anyMatch(g -> g.frozen);

            if (!gapHasBlocker && (Boolean.TRUE.equals(afterOpen) || Boolean.TRUE.equals(beforeClose))) {
                out.append(' ');
                gap.clear();
            } else if (!gapHasBlocker && (Boolean.FALSE.equals(afterOpen) || Boolean.FALSE.equals(beforeClose))) {
                gap.clear();
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
                gap.clear();
            }

            out.append(t.text);
            lastSignificant = t;
            lastSignificantIdx = i;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    private boolean isFunctionDefFollower(final Token t) {
        if (isPunct(t, "{") || isOp(t, "->")) {
            return true;
        }
        if (t.type == TokenType.KEYWORD) {
            final String s = t.text;
            return "const".equals(s) || "override".equals(s) || "noexcept".equals(s)
                    || "throws".equals(s) || "final".equals(s);
        }
        return false;
    }

    private int matchBracketForward(final List<Token> tokens, final int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), "[")) {
                depth++;
            } else if (isPunct(tokens.get(i), "]")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    // ── §3.3 `{}` initializer / block spacing ───────────────────────────────────
    /**
     * Normalizes spacing inside brace-initializer lists (array/struct initializers, `= { ... }`
     * contexts) per STYLE.md §3.3: empty `{}` is always tight; non-empty `{ ... }` gets exactly
     * one space just inside both the opening and closing brace, at every nesting level. Also
     * normalizes comma spacing within the same initializer context (no space before `,`, exactly
     * one space after) -- needed because a brace-initializer whose contents span nested `{...}`
     * groups (e.g. `{ {1,2}, {3,4} }`) is deliberately rejected by
     * `DeclarationAlignmentRule.parseDeclaration` (initializer ending in `}` is left untouched, see
     * its own doc comment) and so never gets comma spacing from that rule's `renderInitTokens`.
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
        // Parallel stack: true only for the frame directly opened by `=` (a fresh top-level
        // initializer), false for every other frame -- including a nested initializer frame
        // continuing an already-active parent initializer, and any non-initializer scope brace
        // (function/control-flow body) that happens to enclose an initializer. `initStack` alone
        // can't distinguish "outermost" this way since a non-initializer enclosing scope brace
        // (e.g. a function body) also occupies a stack slot, offsetting a naive depth count.
        final Deque<Boolean> outermostStack = new ArrayDeque<>();
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

            final boolean inInit = !initStack.isEmpty() && initStack.peek();
            // Inside-brace padding applies only at the outermost initializer level -- STYLE.md
            // §3.3's worked example pads only the outer pair of a nested brace-initializer
            // (`{ {1, 2}, {3, 4} }`), leaving inner element braces tight. Comma spacing, by
            // contrast, applies at every nesting level.
            final boolean atOutermostInit = inInit && !outermostStack.isEmpty() && outermostStack.peek();
            final boolean afterInitOpen = isPunct(lastSignificant, "{") && atOutermostInit;
            final boolean beforeInitClose = isPunct(t, "}") && atOutermostInit;
            final boolean beforeComma = isPunct(t, ",") && inInit;
            final boolean afterComma = isPunct(lastSignificant, ",") && inInit;
            final boolean gapHasBlocker = gap.stream().anyMatch(this::isCommentOrNewline)
                    || t.frozen || (lastSignificant != null && lastSignificant.frozen)
                    || gap.stream().anyMatch(g -> g.frozen);

            if (((afterInitOpen && isPunct(t, "}")) || beforeComma) && !gapHasBlocker) {
                gap.clear();
            } else if ((afterInitOpen || beforeInitClose || afterComma) && !gapHasBlocker) {
                out.append(' ');
                gap.clear();
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
                gap.clear();
            }

            if (isPunct(t, "{")) {
                final boolean startsNewInit = isOp(lastSignificant, "=");
                final boolean isInit = startsNewInit
                        || ((isPunct(lastSignificant, "{") || isPunct(lastSignificant, ","))
                                && !initStack.isEmpty() && initStack.peek());
                initStack.push(isInit);
                outermostStack.push(startsNewInit);
            } else if (isPunct(t, "}") && !initStack.isEmpty()) {
                initStack.pop();
                outermostStack.pop();
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
                if (opIdx >= 0 && isIncrementOp(tokens.get(opIdx)) && noBlockerBetween(tokens, i, opIdx)
                        && !t.frozen && !tokens.get(opIdx).frozen) {
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
            if (tokens.get(incrStart).frozen || tokens.get(opIdx).frozen) {
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

    // ── §6 Assignment and Compound Operator Alignment ───────────────────────────
    private static final Set<String> ASSIGNMENT_OPS = setOf(
            "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=");

    /** One parsed bare assignment statement (`target op value;` -- no declared type; a typed
     *  declaration's own `= value` is STYLE.md §5/`DeclarationAlignmentRule`'s concern, not this
     *  rule's). {@code multiLine} rows (STYLE.md §6's "Multi-line right-hand sides") carry the
     *  value split across {@code firstLineValueTokens}/{@code secondLineValueTokens} instead of
     *  {@code valueTokens}, which is null for those rows -- see {@link #parseAssignment}. */
    public static final class Assignment {
        public final Token target;
        public final String lhsText; // full rendered LHS (may span multiple tokens)
        public final Token operator;
        public final List<Token> valueTokens;
        public final Token trailingComment; // nullable
        public final boolean blankLineBefore;
        public final boolean multiLine;
        public final boolean breakBeforeOperator; // only meaningful when multiLine
        public final List<Token> firstLineValueTokens; // only set when multiLine
        public final List<Token> secondLineValueTokens; // only set when multiLine

        private Assignment(final Token target, final String lhsText, final Token operator,
                final List<Token> valueTokens, final Token trailingComment,
                final boolean blankLineBefore, final boolean multiLine,
                final boolean breakBeforeOperator, final List<Token> firstLineValueTokens,
                final List<Token> secondLineValueTokens) {
            this.target = target;
            this.lhsText = lhsText;
            this.operator = operator;
            this.valueTokens = valueTokens;
            this.trailingComment = trailingComment;
            this.blankLineBefore = blankLineBefore;
            this.multiLine = multiLine;
            this.breakBeforeOperator = breakBeforeOperator;
            this.firstLineValueTokens = firstLineValueTokens;
            this.secondLineValueTokens = secondLineValueTokens;
        }

        static Assignment singleLine(final Token target, final String lhsText, final Token operator,
                final List<Token> valueTokens, final Token trailingComment,
                final boolean blankLineBefore) {
            return new Assignment(target, lhsText, operator, valueTokens, trailingComment,
                    blankLineBefore, false, false, null, null);
        }

        static Assignment multiLine(final Token target, final String lhsText, final Token operator,
                final boolean breakBeforeOperator, final List<Token> firstLineValueTokens,
                final List<Token> secondLineValueTokens, final Token trailingComment,
                final boolean blankLineBefore) {
            return new Assignment(target, lhsText, operator, null, trailingComment,
                    blankLineBefore, true, breakBeforeOperator, firstLineValueTokens,
                    secondLineValueTokens);
        }
    }

    /**
     * Splits one scope's direct-content tokens (caller-extracted, no deeper-nested tokens --
     * same scoping contract as `DeclarationAlignmentRule.groupDeclarations`) into maximal runs of
     * textually-adjacent bare assignment statements (STYLE.md §6, resolved -- see "§6 grouping
     * and rendering" in Resolved Design Decisions: same textually-adjacent-run signal as §14). A
     * blank line, a comment-only gap, or any statement not recognized as a bare assignment breaks
     * the current run. Unlike `GetterSetterRule.groupOneLiners`'s 2+ minimum, a run of length 1 is
     * still returned here -- STYLE.md §6 explicitly wants a lone variable to "align trivially with
     * itself," which {@link #render} achieves for free (group size 1 means both padding widths
     * just equal that one row's own widths).
     */
    public List<List<Assignment>> groupAssignments(final List<Token> scopeTokens) {
        final List<List<Token>> statements = splitAssignmentStatements(scopeTokens);
        final List<List<Assignment>> groups = new ArrayList<>();
        List<Assignment> current = new ArrayList<>();

        for (final List<Token> stmt : statements) {
            final boolean blankBefore = hasBlankLineBeforeStmt(stmt);
            final Assignment a = parseAssignment(stmt, blankBefore);
            if (a == null) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            if (blankBefore && !current.isEmpty()) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(a);
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    /**
     * Renders one alignment group (STYLE.md §6) as two independently fixed-width columns per
     * row -- `maxNameLen` (the widest target name) and `maxPrefixLen` (the widest operator text
     * minus its trailing `=`) -- so that every row's `=` lands on the same column regardless of
     * which compound operator it uses (resolved -- see "§6 grouping and rendering": a single
     * `ColumnGrid` left-pad column on the concatenated name+operator text does NOT reproduce
     * STYLE.md's worked example; this manual two-field padding does). The right-hand side is
     * never reformatted -- its original token text (including internal spacing) is reproduced
     * verbatim, since STYLE.md describes alignment of the `=` column only, not a rewrite of
     * arbitrary expression spacing. An optional trailing-comment column reuses `ColumnGrid` to
     * align comments across the group, same precedent as `DeclarationAlignmentRule`/
     * `GetterSetterRule`. A {@code multiLine} row (STYLE.md §6's "Multi-line right-hand sides")
     * cannot participate in that `ColumnGrid` pass -- its single `value+";"` cell would only ever
     * hold the first physical line, so any later comment-column padding computed from it would be
     * wrong -- so such rows are rendered separately by {@link #renderMultiLine} and spliced back
     * into the group's line order afterward; their own trailing comment (rare, undocumented by any
     * worked example) is appended directly after the second line's `;` rather than column-aligned.
     */
    public List<String> render(final List<Assignment> group) {
        int maxNameLen = 0;
        int maxPrefixLen = 0;
        for (final Assignment a : group) {
            maxNameLen = Math.max(maxNameLen, a.lhsText.length());
            maxPrefixLen = Math.max(maxPrefixLen, assignOpPrefix(a.operator).length());
        }
        // +1 unconditionally -- even the group's widest operator still needs its own leading
        // space (verified against STYLE.md's worked example: maxPrefixLen=2 from ">>=" there,
        // but every row's rendered gap is 3, i.e. naturalMax+1, not naturalMax)
        maxPrefixLen++;
        final int lhsWidth = maxNameLen + maxPrefixLen + 1; // +1 for "="

        final ColumnGrid grid = new ColumnGrid();
        for (final Assignment a : group) {
            if (a.multiLine) {
                continue;
            }
            final String lhs = padRight(a.lhsText, maxNameLen)
                    + padLeft(assignOpPrefix(a.operator), maxPrefixLen) + "=";
            final List<String> cells = new ArrayList<>();
            cells.add(lhs);
            cells.add(joinVerbatim(a.valueTokens) + ";");
            if (a.trailingComment != null) {
                cells.add(a.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }
        final List<String[]> flushed = grid.flush();

        final List<String> lines = new ArrayList<>();
        int flushIdx = 0;
        for (final Assignment a : group) {
            if (a.multiLine) {
                lines.addAll(renderMultiLine(a, maxNameLen, maxPrefixLen, lhsWidth));
            } else {
                lines.add(String.join(" ", flushed.get(flushIdx)));
                flushIdx++;
            }
        }
        return lines;
    }

    /**
     * Renders one multi-line-right-hand-side row (STYLE.md §6) as exactly two lines: line 1 is
     * `lhs + " " + firstLineValueTokens` (identical shape to the single-line case); line 2 is
     * indentation alone, sized to land the continuation at the documented target column, followed
     * by `secondLineValueTokens` and the terminating `;`. Breaking *before* an operator (the
     * operator is the first token of {@code secondLineValueTokens}) targets the `=` column itself
     * -- index `lhsWidth - 1`, since `lhs` is exactly `lhsWidth` characters wide and ends in `=`.
     * Breaking *after* an operator (the operator is the last token of {@code
     * firstLineValueTokens}) targets the column immediately after `=`, i.e. where the first
     * operand began on line 1 -- index `lhsWidth + 1` (`lhs` then one space then the operand).
     * Both target columns are computed from the whole group's `lhsWidth`, not this row's own
     * unpadded name/operator length, so a multi-line row's continuation lines up correctly even
     * when other rows in the same group have longer names/operators.
     */
    private List<String> renderMultiLine(final Assignment a, final int maxNameLen,
            final int maxPrefixLen, final int lhsWidth) {
        final String lhs = padRight(a.lhsText, maxNameLen)
                + padLeft(assignOpPrefix(a.operator), maxPrefixLen) + "=";
        final String line1 = lhs + " " + joinVerbatim(a.firstLineValueTokens);

        final int indentLen = a.breakBeforeOperator ? lhsWidth - 1 : lhsWidth + 1;
        final StringBuilder line2 = new StringBuilder(padRight("", indentLen));
        line2.append(joinVerbatim(a.secondLineValueTokens)).append(';');
        if (a.trailingComment != null) {
            line2.append(' ').append(a.trailingComment.text);
        }
        return Arrays.asList(line1, line2.toString());
    }

    private String assignOpPrefix(final Token operator) {
        return operator.text.substring(0, operator.text.length() - 1);
    }

    private String joinVerbatim(final List<Token> tokens) {
        final StringBuilder sb = new StringBuilder();
        for (final Token t : tokens) {
            sb.append(t.text);
        }
        return sb.toString();
    }

    private static String padRight(final String s, final int width) {
        final StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String padLeft(final String s, final int width) {
        final StringBuilder sb = new StringBuilder();
        while (sb.length() + s.length() < width) {
            sb.append(' ');
        }
        sb.append(s);
        return sb.toString();
    }

    /**
     * Parses the shape `target op value ;` (STYLE.md §6), either entirely on one source line, or
     * spanning exactly two (STYLE.md §6's "Multi-line right-hand sides", see {@link
     * #classifyMultiLineBreak}). `target` must be a single bare `IDENTIFIER` -- a member access
     * (`obj.field`), an array element (`arr[i]`), or a pointer deref (`*ptr`) has no STYLE.md
     * worked example to justify guessing at, so any of those leave this method returning null (the
     * statement is left untouched and breaks the current alignment run, same as any other
     * unrecognized statement). A comment or `NEWLINE` between the target and the first value token
     * (i.e. the `target op` portion must itself be on one line) blocks recognition entirely, as
     * does any comment anywhere in the value -- only a single `NEWLINE` inside the value, at a
     * point `classifyMultiLineBreak` can classify as breaking directly before or after an
     * operator, is accepted; two or more `NEWLINE`s, or a break point unrelated to an operator
     * (e.g. mid-operand), have no STYLE.md worked example and are left untouched.
     */
    private Assignment parseAssignment(final List<Token> stmt, final boolean blankBefore) {
        final int targetIdx = nextSignificantIndex(stmt, 0);
        if (targetIdx < 0) {
            return null;
        }
        final TokenType targetType = stmt.get(targetIdx).type;
        if (targetType != TokenType.IDENTIFIER && targetType != TokenType.KEYWORD) {
            return null;
        }
        // `auto [a, b] = expr;` (a C++17 structured binding) starts with the `auto` keyword
        // followed by a `[` -- that's DeclarationAlignmentRule's shape, not an assignment to a
        // subscript of a variable literally named "auto". Left unrecognized here so the
        // declaration pass's rendering isn't re-parsed and re-collapsed by this pass.
        if (targetType == TokenType.KEYWORD && "auto".equals(stmt.get(targetIdx).text)) {
            return null;
        }
        // Scan forward to find the assignment operator, allowing member-access chains
        // (obj.field, ptr->field) and subscript expressions (arr[i]) in the LHS.
        // State 0: after target/field, expecting op or member-access operator.
        // State 1: after . or ->, expecting the field-name IDENTIFIER.
        // State 2: inside [...] subscript (depth-tracked).
        int opIdx = -1;
        int scanState = 0;
        int scanDepth = 0;
        for (int k = targetIdx + 1; k < stmt.size(); k++) {
            final Token t = stmt.get(k);
            if (isGapToken(t)) { continue; }
            if (scanState == 2) {
                if (isPunct(t, "[")) { scanDepth++; }
                else if (isPunct(t, "]")) { scanDepth--; if (scanDepth == 0) { scanState = 0; } }
                else if (isPunct(t, ";")) { return null; }
                continue;
            }
            if (scanState == 1) {
                if (t.type != TokenType.IDENTIFIER) { return null; }
                scanState = 0;
                continue;
            }
            if (t.type == TokenType.OP && ASSIGNMENT_OPS.contains(t.text)) {
                opIdx = k;
                break;
            }
            if (isOp(t, ".") || isOp(t, "->")) { scanState = 1; continue; }
            if (isPunct(t, "[")) { scanState = 2; scanDepth = 1; continue; }
            return null;
        }
        if (opIdx < 0) { return null; }

        // Render the full LHS for alignment (handles single- and multi-token LHS).
        final List<Token> lhsSigTokens = new ArrayList<>();
        for (int k = targetIdx; k < opIdx; k++) {
            final Token t = stmt.get(k);
            if (!isGapToken(t)) { lhsSigTokens.add(t); }
        }
        final String lhsText = renderTokens(lhsSigTokens);

        final int valueFrom = nextSignificantIndex(stmt, opIdx + 1);
        if (valueFrom < 0 || !noBlockerBetween(stmt, targetIdx, valueFrom)) {
            return null;
        }
        final int semiIdx = findTopLevelSemicolon(stmt, valueFrom);
        if (semiIdx < 0) {
            return null;
        }
        int valueTo = semiIdx;
        while (valueTo > valueFrom && isGapToken(stmt.get(valueTo - 1))) {
            valueTo--;
        }
        if (valueTo <= valueFrom) {
            return null;
        }
        for (int i = semiIdx + 1; i < stmt.size(); i++) {
            final Token t = stmt.get(i);
            if (t.type != TokenType.WHITESPACE && t.type != TokenType.COMMENT_LINE
                    && t.type != TokenType.COMMENT_BLOCK) {
                return null; // stray tokens after `;` -- not a clean single statement
            }
        }

        int newlineCount = 0;
        int newlineIdx = -1;
        for (int i = valueFrom; i < valueTo; i++) {
            final Token t = stmt.get(i);
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                return null;
            }
            if (t.type == TokenType.NEWLINE) {
                newlineCount++;
                newlineIdx = i;
            }
        }
        final Token trailingComment = findTrailingAssignComment(stmt);
        if (newlineCount == 0) {
            final List<Token> value = new ArrayList<>(stmt.subList(valueFrom, valueTo));
            return Assignment.singleLine(stmt.get(targetIdx), lhsText, stmt.get(opIdx), value,
                    trailingComment, blankBefore);
        }

        // STYLE.md §6's own "multi-line right-hand side" shape is exactly one newline, split at
        // an operator. Anything else here (more than one newline, or a single newline that isn't
        // an operator break) is most likely a call whose arguments `MiscRule.enforceCallLineBreaking`
        // already wrapped across lines on a prior pass, once fed back in as this pass's input --
        // reject re-deriving that shape and fall through to the verbatim branch below instead of
        // returning null, so this row still participates in the group's LHS/`=` alignment (using
        // its own `lhsText` for width) rather than splitting the run into smaller subgroups, which
        // would otherwise make a fresh format and a reformat of that format's own already-wrapped
        // output disagree on group membership/padding (non-idempotent).
        if (newlineCount == 1) {
            int line1End = newlineIdx;
            while (line1End > valueFrom && isGapToken(stmt.get(line1End - 1))) {
                line1End--;
            }
            int line2Start = newlineIdx + 1;
            while (line2Start < valueTo && isGapToken(stmt.get(line2Start))) {
                line2Start++;
            }
            if (line1End > valueFrom && line2Start < valueTo) {
                final List<Token> line1 = new ArrayList<>(stmt.subList(valueFrom, line1End));
                final List<Token> line2 = new ArrayList<>(stmt.subList(line2Start, valueTo));
                final Boolean breakBeforeOperator = classifyMultiLineBreak(line1, line2);
                if (breakBeforeOperator != null) {
                    return Assignment.multiLine(stmt.get(targetIdx), lhsText, stmt.get(opIdx),
                            breakBeforeOperator, line1, line2, trailingComment, blankBefore);
                }
            }
        }
        final List<Token> value = new ArrayList<>(stmt.subList(valueFrom, valueTo));
        return Assignment.singleLine(stmt.get(targetIdx), lhsText, stmt.get(opIdx), value,
                trailingComment, blankBefore);
    }

    /**
     * Classifies a multi-line right-hand side's break point per STYLE.md §6: {@code true} if
     * {@code line2}'s first token is an operator ("breaking before an operator"), {@code false} if
     * {@code line1}'s last token is an operator ("breaking after an operator"), {@code null} if
     * neither holds -- no STYLE.md worked example covers a break unrelated to an operator (e.g.
     * mid-operand), so that shape is left unrecognized rather than guessed at. Checked in this
     * order so a (rare, ambiguous) break where both sides touch an operator resolves to the
     * "before" reading.
     */
    private Boolean classifyMultiLineBreak(final List<Token> line1, final List<Token> line2) {
        if (line2.get(0).type == TokenType.OP) {
            return Boolean.TRUE;
        }
        if (line1.get(line1.size() - 1).type == TokenType.OP) {
            return Boolean.FALSE;
        }
        return null;
    }

    private int findTopLevelSemicolon(final List<Token> tokens, final int from) {
        int depth = 0;
        for (int i = from; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                depth--;
            } else if (depth == 0 && isPunct(t, ";")) {
                return i;
            }
        }
        return -1;
    }

    private Token findTrailingAssignComment(final List<Token> stmt) {
        for (int k = stmt.size() - 1; k >= 0; k--) {
            final Token t = stmt.get(k);
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                return t;
            }
            if (t.type != TokenType.WHITESPACE) {
                break;
            }
        }
        return null;
    }

    /**
     * Splits scope tokens into statement-or-block spans, depth-tracked across `(`/`[`/`{` (and
     * their closes) so that neither a parenthesized sub-expression's internal punctuation (e.g. a
     * `for(...; ...; ...)` header's own `;`s, or a lambda body's own `;`) nor a nested `{ }` block
     * ends the span early -- only a `;` or a balancing `}` at combined depth 0 does. A balancing
     * `}` produces an opaque span (e.g. an `if`/`for`/method-body block that leaked into this
     * scope) that `parseAssignment` will always reject, same as any other unrecognized statement.
     * A same-line trailing comment is pulled into the span it follows, same precedent as
     * `DeclarationAlignmentRule.splitStatements`.
     */
    private List<List<Token>> splitAssignmentStatements(final List<Token> scopeTokens) {
        final List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        final int n = scopeTokens.size();
        int depth = 0;
        int idx = 0;

        while (idx < n) {
            final Token t = scopeTokens.get(idx);
            current.add(t);
            idx++;

            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
                continue;
            }
            if (isPunct(t, ")") || isPunct(t, "]")) {
                depth--;
                continue;
            }
            if (isPunct(t, "}")) {
                depth--;
                if (depth == 0) {
                    idx = pullTrailingSameLine(scopeTokens, current, idx);
                    statements.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            if (depth == 0 && isPunct(t, ";")) {
                idx = pullTrailingSameLine(scopeTokens, current, idx);
                statements.add(current);
                current = new ArrayList<>();
            }
        }
        if (!current.isEmpty()) {
            statements.add(current);
        }
        return statements;
    }

    private int pullTrailingSameLine(final List<Token> tokens, final List<Token> current, final int from) {
        int idx = from;
        final int n = tokens.size();
        while (idx < n) {
            final Token next = tokens.get(idx);
            if (next.type == TokenType.WHITESPACE || next.type == TokenType.COMMENT_LINE
                    || next.type == TokenType.COMMENT_BLOCK) {
                current.add(next);
                idx++;
            } else {
                break;
            }
        }
        return idx;
    }

    /** Same blank-line-before detection as `DeclarationAlignmentRule.hasBlankLineBefore`. */
    private boolean hasBlankLineBeforeStmt(final List<Token> stmt) {
        int newlineRun = 0;
        for (final Token t : stmt) {
            if (t.type == TokenType.NEWLINE) {
                newlineRun++;
                if (newlineRun >= 2) {
                    return true;
                }
            } else if (t.type == TokenType.WHITESPACE) {
                // ignore -- doesn't break or extend the newline run
            } else if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                newlineRun = 0;
            } else {
                break;
            }
        }
        return false;
    }

    // ── §8 Function Signatures ──────────────────────────────────────────────────
    /** One parameter: `[typeTokens] name [sizeTokens]` (the C array-param suffix, e.g. `int x[]`).
     *  A param whose shape doesn't reduce to this (a default value, a bare `...`, etc.) has no
     *  STYLE.md worked example to justify guessing at -- {@link #parseSignature} bails the whole
     *  signature rather than mis-rendering one param. */
    public static final class Param {
        public final List<Token> typeTokens;
        public final Token name;
        public final List<Token> sizeTokens;
        public final Token comment;
        public final Token leadingComment;

        Param(final List<Token> typeTokens, final Token name, final List<Token> sizeTokens,
                final Token comment, final Token leadingComment) {
            this.typeTokens = typeTokens;
            this.name = name;
            this.sizeTokens = sizeTokens;
            this.comment = comment;
            this.leadingComment = leadingComment;
        }
    }

    /** One parsed function signature: `leadTokens name ( params )`, where `leadTokens` is every
     *  token before the name (modifiers and return type, not split apart -- §8 has no per-row
     *  alignment group across multiple signatures the way §5's modifier columns do, so there is
     *  no need to classify them individually). `explicitVoidParam` records a literal C `(void)`
     *  parameter list, distinct from a truly empty `()`, so {@link #render} can reproduce it. */
    public static final class Signature {
        public final List<Token> leadTokens;
        public final Token name;
        public final List<Param> params;
        public final boolean explicitVoidParam;

        Signature(final List<Token> leadTokens, final Token name, final List<Param> params,
                final boolean explicitVoidParam) {
            this.leadTokens = leadTokens;
            this.name = name;
            this.params = params;
            this.explicitVoidParam = explicitVoidParam;
        }
    }

    /**
     * Parses `sigTokens` -- a function signature already isolated by the caller, spanning from
     * its first lead token (the first modifier, or the return type if there are none) through
     * and including the parameter list's closing `)`, and nothing past it (no `throws` clause,
     * no `{`/`;`) -- into a {@link Signature}. This rule's job is rendering (inline vs. broken,
     * param alignment), not discovering where a function signature starts in arbitrary source;
     * that boundary-finding is left to the caller, same granularity precedent as
     * `DeclarationAlignmentRule.parseDeclaration`'s pre-split `stmt` contract.
     * The name is identified as the IDENTIFIER immediately before the first depth-0 `(` (depth
     * tracked over Java generics via the tokenizer's distinct `ANGLE_BRACKET_OPEN`/`_CLOSE`
     * token types, so a generic return type like `Map<String, Integer> get(...)` doesn't
     * misidentify `Integer` as the name). Returns null -- leaving the candidate completely
     * untouched -- whenever: the shape doesn't match at all; `sigTokens` has trailing tokens past
     * the matched `)` (the caller included something this method doesn't handle); or any
     * parameter fails to parse (a default value, e.g. C++'s `int x = 0`, or any other shape with
     * no STYLE.md worked example).
     */
    public Signature parseSignature(final List<Token> sigTokens) {
        final List<Token> sig = significantWithComments(sigTokens);
        int openParen = -1;
        int nameIdx = -1;
        int depth = 0;
        for (int i = 0; i < sig.size(); i++) {
            final Token t = sig.get(i);
            if (t.type == TokenType.ANGLE_BRACKET_OPEN) {
                depth++;
            } else if (t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                depth--;
            } else if (depth == 0 && isPunct(t, "(") && i > 0
                    && sig.get(i - 1).type == TokenType.IDENTIFIER) {
                openParen = i;
                nameIdx = i - 1;
                break;
            }
        }
        if (openParen < 0) {
            return null;
        }
        final int closeParen = matchParenForward(sig, openParen);
        if (closeParen != sig.size() - 1) {
            return null;
        }

        final List<Token> leadTokens = new ArrayList<>(sig.subList(0, nameIdx));
        final Token name = sig.get(nameIdx);
        final List<Token> paramsSlice = sig.subList(openParen + 1, closeParen);

        if (paramsSlice.isEmpty()) {
            return new Signature(leadTokens, name, new ArrayList<Param>(), false);
        }
        if (paramsSlice.size() == 1 && paramsSlice.get(0).type == TokenType.KEYWORD
                && "void".equals(paramsSlice.get(0).text)) {
            return new Signature(leadTokens, name, new ArrayList<Param>(), true);
        }

        final List<List<Token>> parts = splitTopLevelCommas(paramsSlice);
        // A comment immediately after a comma (before the next param's own tokens) belongs to
        // the *previous* param -- e.g. `int b,   // second\nint c` -- but the comma-split above
        // leaves it as the leading token of the next part. Reattach it as that previous part's
        // trailing comment before parsing, so it doesn't get swept into the next param's type.
        for (int i = 0; i < parts.size() - 1; i++) {
            final List<Token> next = parts.get(i + 1);
            if (!next.isEmpty() && (next.get(0).type == TokenType.COMMENT_LINE
                    || next.get(0).type == TokenType.COMMENT_BLOCK)) {
                parts.get(i).add(next.remove(0));
            }
        }

        final List<Param> params = new ArrayList<>();
        for (final List<Token> slice : parts) {
            final Param p = parseParam(slice);
            if (p == null) {
                return null;
            }
            params.add(p);
        }
        return new Signature(leadTokens, name, params, false);
    }

    private List<List<Token>> splitTopLevelCommas(final List<Token> tokens) {
        final List<List<Token>> parts = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int depth = 0;
        for (final Token t : tokens) {
            if (isPunct(t, "(") || isPunct(t, "[") || t.type == TokenType.ANGLE_BRACKET_OPEN) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]") || t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                depth--;
            } else if (depth == 0 && isPunct(t, ",")) {
                parts.add(current);
                current = new ArrayList<>();
                continue;
            }
            current.add(t);
        }
        parts.add(current);
        return parts;
    }

    /** Splits {@code paramsSlice} into top-level (depth-0) comma-separated arguments -- exactly
     *  like {@link #splitTopLevelCommas}, but tracking paren/bracket/angle depth *cumulatively
     *  across the whole slice* rather than resetting it to 0 per original source line -- then
     *  groups consecutive arguments back into "rows" by which original line each one *starts* on.
     *  An argument that itself spans multiple lines (e.g. a lone nested call whose own argument
     *  list wraps) stays one argument in one row; only a depth-0 {@code NEWLINE} seen since the
     *  last depth-0 comma starts a new row. This replaces the old {@code splitOnNewlines} +
     *  per-line {@link #splitTopLevelCommas} combination, which mis-split a nested comma sitting at
     *  a line break (still inside an unclosed paren from the previous line) as if it were a local
     *  top-level split point, corrupting the rendered output (see RDD_KEY_5 addendum). Trailing/
     *  leading empty parts (a dangling comma with nothing after it before the closing paren) are
     *  dropped. A depth-0 {@code NEWLINE} only counts as starting a new row if it occurs *before*
     *  the part's first significant token (a leading newline) -- a trailing newline after a part's
     *  last significant token (e.g. the newline before a lone closing-paren line) must not
     *  retroactively mark that already-started part as beginning a new row, or a sibling argument
     *  that legitimately shared a source line with it gets wrongly split onto its own line. */
    private List<List<List<Token>>> groupByOriginalLine(final List<Token> paramsSlice) {
        final List<List<Token>> parts = new ArrayList<>();
        final List<Boolean> startsNewRow = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        boolean currentHasSignificant = false;
        int depth = 0;
        boolean pendingNewRow = true; // first part always starts a new row
        for (final Token t : paramsSlice) {
            if (isPunct(t, "(") || isPunct(t, "[") || t.type == TokenType.ANGLE_BRACKET_OPEN) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]") || t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                depth--;
            } else if (depth == 0 && isPunct(t, ",")) {
                parts.add(current);
                startsNewRow.add(pendingNewRow);
                current = new ArrayList<>();
                currentHasSignificant = false;
                pendingNewRow = false;
                continue;
            } else if (depth == 0 && t.type == TokenType.NEWLINE && !currentHasSignificant) {
                pendingNewRow = true;
            }
            current.add(t);
            if (!isGapToken(t)) {
                currentHasSignificant = true;
            }
        }
        parts.add(current);
        startsNewRow.add(pendingNewRow);

        final List<List<List<Token>>> rows = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            if (significantOnly(parts.get(i)).isEmpty()) {
                continue; // dangling trailing/leading comma artifact
            }
            if (startsNewRow.get(i) || rows.isEmpty()) {
                rows.add(new ArrayList<>());
            }
            rows.get(rows.size() - 1).add(parts.get(i));
        }
        return rows;
    }

    /** Parses one already-significant-only param slice, peeling a trailing `[size]` run (same
     *  depth-tracked peel-off precedent as `DeclarationAlignmentRule.parseDeclaration`'s
     *  `sizeTokens` loop) before requiring the final remaining token to be the IDENTIFIER name. */
    private Param parseParam(final List<Token> rawSlice) {
        if (rawSlice.isEmpty()) {
            return null;
        }
        Token comment = null;
        List<Token> slice = rawSlice;
        final Token last = rawSlice.get(rawSlice.size() - 1);
        if (last.type == TokenType.COMMENT_LINE || last.type == TokenType.COMMENT_BLOCK) {
            comment = last;
            slice = rawSlice.subList(0, rawSlice.size() - 1);
        }
        if (slice.isEmpty()) {
            return null;
        }
        Token leadingComment = null;
        final Token first = slice.get(0);
        if (slice.size() > 1
                && (first.type == TokenType.COMMENT_LINE || first.type == TokenType.COMMENT_BLOCK)) {
            leadingComment = first;
            slice = slice.subList(1, slice.size());
        }
        for (final Token t : slice) {
            if (isOp(t, "=")) {
                return null; // default value -- no STYLE.md worked example, bail the whole signature
            }
        }
        int end = slice.size();
        final List<Token> sizeTokens = new ArrayList<>();
        while (end > 0 && isPunct(slice.get(end - 1), "]")) {
            int depth = 0;
            int openIdx = -1;
            for (int k = end - 1; k >= 0; k--) {
                final Token t = slice.get(k);
                if (isPunct(t, "]")) {
                    depth++;
                } else if (isPunct(t, "[")) {
                    depth--;
                    if (depth == 0) {
                        openIdx = k;
                        break;
                    }
                }
            }
            if (openIdx < 0) {
                break;
            }
            sizeTokens.addAll(0, slice.subList(openIdx, end));
            end = openIdx;
        }
        if (end <= 0) {
            return null;
        }
        final Token name = slice.get(end - 1);
        if (name.type != TokenType.IDENTIFIER) {
            return null;
        }
        final List<Token> typeTokens = new ArrayList<>(slice.subList(0, end - 1));
        if (typeTokens.isEmpty()) {
            return null;
        }
        return new Param(typeTokens, name, sizeTokens, comment, leadingComment);
    }

    /**
     * Renders one signature (STYLE.md §8) inline if it fits within {@link #lineLengthLimit}
     * at its starting column (`indentLevel * indentWidth`, per STYLE.md §1's tab-display-size-4
     * convention -- visual column, not raw character count, so the comparison is meaningful
     * regardless of `indentStyle`), or broken to one parameter per line otherwise. A zero-param
     * signature (including an explicit C `(void)`) is always rendered inline -- breaking achieves
     * nothing with no parameter to place on its own line, so an over-length zero-param signature
     * is left long rather than "broken" into a meaningless single-line shape.
     * <p>Broken form: the parameter type column is padded to {@code maxTypeLen + 1} --
     * unconditionally, the same convention established for §6's `maxPrefixLen` -- before the
     * normal single-space join; verified character-by-character against STYLE.md §8's own worked
     * example (`const char*`/`uint8_t`/`uint16_t`, max width 11, every row's gap is
     * `(11 - thisWidth) + 2`, i.e. `maxTypeLen + 1` padding plus one join space, not
     * `maxTypeLen + 1` total) that a plain `maxTypeLen`-width column (matching §5's declaration
     * grid) under-pads by exactly one space relative to this section's own example. The closing
     * `)` is indented to `indentLevel` (already resolved -- see STATE.md's §8 checklist: matches
     * the first character of the signature itself); parameter lines are indented to
     * `indentLevel + 1`.
     */
    public List<String> render(final Signature sig, final int indentLevel, final String indentStyle) {
        final String lead = renderTokens(sig.leadTokens);
        final boolean leadNeedsSpace = !sig.leadTokens.isEmpty()
                && needsSpaceBetween(sig.leadTokens.get(sig.leadTokens.size() - 1), sig.name,
                        Collections.<Token>emptySet(), Collections.<Token>emptySet());
        final String head = (lead.isEmpty() ? "" : lead + (leadNeedsSpace ? " " : "")) + sig.name.text + "(";
        final String inline = head + renderParamsInline(sig) + ")";
        final int startColumn = indentLevel * indentWidth;

        // Param comments don't count toward the line-length break decision -- only the code
        // itself should trigger wrapping to the multi-line param-per-line form.
        int commentLen = 0;
        for (final Param p : sig.params) {
            if (p.comment != null) {
                commentLen += p.comment.text.length() + 1;
            }
        }
        // A `//` line comment on any param can never be rendered inline -- it would swallow
        // every token after it on the physical line (including the params/`)`/`{` that follow),
        // silently corrupting the rest of the file when re-tokenized by a later pass.
        boolean hasLineComment = false;
        for (final Param p : sig.params) {
            if (p.comment != null && p.comment.type == TokenType.COMMENT_LINE) {
                hasLineComment = true;
                break;
            }
        }
        if (!hasLineComment
                && (sig.params.isEmpty() || startColumn + inline.length() - commentLen <= lineLengthLimit)) {
            return Collections.singletonList(inline);
        }

        int maxTypeLen = 0;
        for (final Param p : sig.params) {
            if (p.leadingComment == null) {
                maxTypeLen = Math.max(maxTypeLen, renderTokens(p.typeTokens).length());
            }
        }
        final int typeColWidth = maxTypeLen + 1;
        int maxNameCommaLen = 0;
        for (int i = 0; i < sig.params.size(); i++) {
            final Param p = sig.params.get(i);
            final String nameCommaText = p.name.text + renderTokens(p.sizeTokens)
                    + (i < sig.params.size() - 1 ? "," : "");
            maxNameCommaLen = Math.max(maxNameCommaLen, nameCommaText.length());
        }

        final List<String> lines = new ArrayList<>();
        lines.add(head);
        final String paramIndent = indentText(indentLevel + 1, indentStyle);
        for (int i = 0; i < sig.params.size(); i++) {
            final Param p = sig.params.get(i);
            final String typeText = renderTokens(p.typeTokens);
            final String nameCommaText = p.name.text + renderTokens(p.sizeTokens)
                    + (i < sig.params.size() - 1 ? "," : "");
            final String nameText = p.comment != null
                    ? padRight(nameCommaText, maxNameCommaLen) + " " + p.comment.text
                    : nameCommaText;
            final String leadPrefix = p.leadingComment != null ? p.leadingComment.text + " " : "";
            final String typeCell = p.leadingComment != null ? typeText + " " : padRight(typeText, typeColWidth);
            lines.add(paramIndent + leadPrefix + typeCell + nameText);
        }
        lines.add(indentText(indentLevel, indentStyle) + ")");
        return lines;
    }

    private String renderParamsInline(final Signature sig) {
        if (sig.params.isEmpty()) {
            return sig.explicitVoidParam ? "void" : "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sig.params.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            final Param p = sig.params.get(i);
            sb.append(renderTokens(p.typeTokens)).append(' ')
                    .append(p.name.text).append(renderTokens(p.sizeTokens));
            if (p.comment != null) {
                sb.append(' ').append(p.comment.text);
            }
        }
        return sb.toString();
    }

    /** Joins tokens into canonical spaced text -- exact copy of
     *  `DeclarationAlignmentRule.renderTokens`'s tight-attachment rules (`*`/`&`/`::`/generics/
     *  `[`/`]`/`,`), duplicated here rather than shared since neither class currently exposes
     *  these as a shared utility (each rule class keeps its own small token-joining helpers). */
    private String renderTokens(final List<Token> tokens) {
        final Set<Token> templateOpens = new HashSet<>();
        final Set<Token> templateCloses = new HashSet<>();
        templateAngleTokens(tokens, templateOpens, templateCloses);
        final StringBuilder sb = new StringBuilder();
        Token prev = null;
        for (final Token t : tokens) {
            if (prev != null && needsSpaceBetween(prev, t, templateOpens, templateCloses)) {
                sb.append(' ');
            }
            sb.append(t.text);
            prev = t;
        }
        return sb.toString();
    }

    /** A leading `template<...>` clause's `<`/`>` tokens are never reclassified to
     *  {@code ANGLE_BRACKET_OPEN}/{@code _CLOSE} by the tokenizer (it only arms on an
     *  identifier/cast-keyword before `<`, not the `template` keyword -- see
     *  `DeclarationAlignmentRule`'s own template-prefix handling for the same precedent), so
     *  {@link #needsSpaceBetween}/{@link #isTightToken} would otherwise space them like a
     *  comparison operator. Populates the identity sets of every open/close `<`/`>` token
     *  belonging to such a clause (depth-matched on the raw `<`/`>` OP tokens themselves) so the
     *  caller can treat them as tight without touching the tokens' actual type. */
    private void templateAngleTokens(final List<Token> tokens, final Set<Token> opens, final Set<Token> closes) {
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.KEYWORD && "template".equals(t.text)
                    && i + 1 < tokens.size() && isOp(tokens.get(i + 1), "<")) {
                int depth = 0;
                for (int j = i + 1; j < tokens.size(); j++) {
                    final Token u = tokens.get(j);
                    if (isOp(u, "<")) {
                        depth++;
                        opens.add(u);
                    } else if (isOp(u, ">")) {
                        depth--;
                        closes.add(u);
                        if (depth == 0) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean needsSpaceBetween(final Token prev, final Token cur, final Set<Token> templateOpens,
            final Set<Token> templateCloses) {
        if (isTightToken(cur) || templateCloses.contains(cur) || templateOpens.contains(cur)) {
            return false;
        }
        // A type keyword (`void`, `int`, ...) directly followed by `(` is a function-type's
        // return type inside a template argument (`std::function<void(int)>`) -- keywords can
        // never be called, so this is never a call-site space, only a tight function-type join.
        if (isPunct(cur, "(") && (prev.type == TokenType.IDENTIFIER
                || prev.type == TokenType.ANGLE_BRACKET_CLOSE || prev.type == TokenType.KEYWORD)) {
            return false;
        }
        if (prev.type == TokenType.ANGLE_BRACKET_OPEN || isOp(prev, "::") || isOp(prev, ".") || isOp(prev, "->") || isPunct(prev, "[") || isPunct(prev, "(")
                || templateOpens.contains(prev)) {
            return false;
        }
        return true;
    }

    private boolean isTightToken(final Token t) {
        if (t.type == TokenType.ANGLE_BRACKET_OPEN || t.type == TokenType.ANGLE_BRACKET_CLOSE) {
            return true;
        }
        if (isPunct(t, ",") || isPunct(t, "[") || isPunct(t, "]") || isPunct(t, ")")) {
            return true;
        }
        return Token.isRepOp(t, '*') || Token.isRepOp(t, '&') || isOp(t, "...") || isOp(t, "::") || isOp(t, ".") || isOp(t, "->");
    }

    private List<Token> significantOnly(final List<Token> stmt) {
        final List<Token> sig = new ArrayList<>();
        for (final Token t : stmt) {
            if (!isGapToken(t)) {
                sig.add(t);
            }
        }
        return sig;
    }

    /** Like {@link #significantOnly}, but keeps comment tokens -- used by {@link #parseSignature}
     *  so a parameter's inline block comment survives parsing instead of being silently dropped;
     *  only whitespace/newlines are gap tokens here. */
    private List<Token> significantWithComments(final List<Token> stmt) {
        final List<Token> sig = new ArrayList<>();
        for (final Token t : stmt) {
            if (t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) {
                sig.add(t);
            }
        }
        return sig;
    }

    // ── §9 Blank Line Before `return` ───────────────────────────────────────────
    private static final class FuncFrame {
        final boolean isFunctionBody;
        final boolean multiLine;
        boolean sawContent;

        FuncFrame(final boolean isFunctionBody, final boolean multiLine) {
            this.isFunctionBody = isFunctionBody;
            this.multiLine = multiLine;
            this.sawContent = false;
        }
    }

    /**
     * Inserts exactly one blank line before a `return` statement when (STYLE.md §9): the
     * enclosing function body is itself multi-line, AND the `return` sits directly inside that
     * body (not inside a further-nested block) with at least one statement already before it.
     * Reuses the same gap-buffering / "leave an existing blank line untouched, only ever add a
     * missing one" precedent as `BlockStructureRule.insertNamedConstructBlankLines`.
     * <p>"Function body" is detected the same minimal, purely structural signal already noted
     * (for a different, not-yet-wired-in purpose) in this file's "§11 K&R brace style detection"
     * Resolved Design Decision: a `{` whose immediately preceding significant token is a `)`
     * whose matching `(` is itself preceded by an IDENTIFIER -- which is enough on its own to
     * exclude every control-flow brace (`if`/`while`/`for`/`switch`/`catch` are all preceded by a
     * KEYWORD there, never an IDENTIFIER) and every lambda (preceded by `->`, never `)`), with no
     * per-keyword exclusion list needed. One more guard is added beyond that precedent: if the
     * identifier itself is preceded by `new`, this is a constructor call / anonymous class
     * instantiation, not a method definition -- excluded so an anonymous class's own body is
     * never misclassified as the function body of whatever constructor created it. Known,
     * deliberate gap (no STYLE.md worked example to resolve it against): a C++ method with a
     * trailing qualifier between `)` and `{` (`void foo() const { ... }`) is not recognized --
     * the brace is misclassified as not-a-function-body and this rule simply does nothing there,
     * never anything actively wrong.
     * <p>STYLE.md §9's only documented exclusion is the brace-less `if(x) return y;` shape (§10);
     * generalized here to a brace-less `while`/`for` controlled body too, since the underlying
     * reasoning STYLE.md states for the exclusion -- "the `return` is at function scope... not
     * inside a nested block" -- applies identically to those, even without literal braces.
     * <p>A `return` whose immediately preceding gap contains a comment, or contains zero
     * newlines (i.e. shares a source line with the previous statement), is left untouched --
     * neither shape has a STYLE.md worked example to justify guessing where the blank line (or,
     * for the zero-newline case, a new line break that doesn't yet exist at all) should go.
     */
    public String insertBlankLineBeforeReturn(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final Deque<FuncFrame> stack = new ArrayDeque<>();
        final List<Token> gap = new ArrayList<>();
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean hasComment = gap.stream()
                    .anyMatch(g -> g.type == TokenType.COMMENT_LINE || g.type == TokenType.COMMENT_BLOCK);
            final long newlineCount = gap.stream().filter(g -> g.type == TokenType.NEWLINE).count();
            if (shouldForceBlankBeforeReturn(tokens, i, stack) && !hasComment && newlineCount >= 1) {
                appendGapWithForcedBlank(out, gap, newlineCount);
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            gap.clear();

            if (!stack.isEmpty()) {
                stack.peek().sawContent = true;
            }
            if (isPunct(t, "{")) {
                final boolean isFuncBody = isFunctionBodyBrace(tokens, i);
                stack.push(new FuncFrame(isFuncBody, isFuncBody && spansMultipleLines(tokens, i)));
            } else if (isPunct(t, "}") && !stack.isEmpty()) {
                stack.pop();
            }
            out.append(t.text);
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    private boolean shouldForceBlankBeforeReturn(final List<Token> tokens, final int idx,
            final Deque<FuncFrame> stack) {
        final Token t = tokens.get(idx);
        if (t.type != TokenType.KEYWORD || !"return".equals(t.text) || stack.isEmpty() || t.frozen) {
            return false;
        }
        final FuncFrame top = stack.peek();
        if (!top.isFunctionBody || !top.multiLine || !top.sawContent) {
            return false;
        }
        return !isBraceLessControlFlowReturn(tokens, idx);
    }

    private void appendGapWithForcedBlank(final StringBuilder out, final List<Token> gap, final long newlineCount) {
        if (newlineCount >= 2) {
            for (final Token g : gap) {
                out.append(g.text);
            }
            return;
        }
        boolean inserted = false;
        for (final Token g : gap) {
            out.append(g.text);
            if (!inserted && g.type == TokenType.NEWLINE) {
                out.append('\n');
                inserted = true;
            }
        }
    }

    /** A `return` immediately preceded by `)` whose matching `(` is preceded by `if`/`while`/
     *  `for`/`switch` is the controlled body of a brace-less single-statement control-flow
     *  construct -- not at function scope, regardless of which frame is on top of the stack
     *  (a brace-less body never pushes its own frame). */
    private boolean isBraceLessControlFlowReturn(final List<Token> tokens, final int returnIdx) {
        final int closeParen = prevSignificantIndex(tokens, returnIdx - 1);
        if (closeParen < 0 || !isPunct(tokens.get(closeParen), ")")) {
            return false;
        }
        final int openParen = matchParenBackward(tokens, closeParen);
        if (openParen < 0) {
            return false;
        }
        final int kwIdx = prevSignificantIndex(tokens, openParen - 1);
        return kwIdx >= 0 && tokens.get(kwIdx).type == TokenType.KEYWORD
                && TIGHT_PAREN_KEYWORDS.contains(tokens.get(kwIdx).text);
    }

    private boolean isFunctionBodyBrace(final List<Token> tokens, final int braceIdx) {
        // Skip a `throws ExceptionType, ExceptionType...` clause, if present.
        int closeParen = skipThrowsClauseBackward(tokens, prevSignificantIndex(tokens, braceIdx - 1));
        // Skip post-paren qualifiers (const, volatile, noexcept, override, final, throws).
        while (closeParen >= 0 && isFunctionBodyQualifier(tokens.get(closeParen))) {
            closeParen = prevSignificantIndex(tokens, closeParen - 1);
        }
        // Also handle trailing return type: `auto foo() -> ReturnType {`
        if (closeParen >= 0 && !isPunct(tokens.get(closeParen), ")")) {
            closeParen = findCloseParenBeforeTrailingReturnType(tokens, closeParen);
        }
        if (closeParen < 0 || !isPunct(tokens.get(closeParen), ")")) {
            return false;
        }
        final int openParen = matchParenBackward(tokens, closeParen);
        if (openParen < 0) {
            return false;
        }
        final int nameIdx = prevSignificantIndex(tokens, openParen - 1);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return false;
        }
        final int beforeName = prevSignificantIndex(tokens, nameIdx - 1);
        return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                || !"new".equals(tokens.get(beforeName).text);
    }

    /** If {@code fromIdx} is the last token of a `throws ExceptionType, ExceptionType...` clause
     *  (a comma-separated identifier/qualified-name list immediately preceded by the `throws`
     *  keyword), returns the index just before that keyword; otherwise returns {@code fromIdx}
     *  unchanged. */
    private int skipThrowsClauseBackward(final List<Token> tokens, final int fromIdx) {
        int i = fromIdx;
        if (i < 0 || tokens.get(i).type != TokenType.IDENTIFIER) {
            return fromIdx;
        }
        while (i >= 0) {
            if (tokens.get(i).type != TokenType.IDENTIFIER) {
                return fromIdx;
            }
            int prev = prevSignificantIndex(tokens, i - 1);
            while (prev >= 0 && isOp(tokens.get(prev), ".")) {
                prev = prevSignificantIndex(tokens, prev - 1);
                if (prev < 0 || tokens.get(prev).type != TokenType.IDENTIFIER) {
                    return fromIdx;
                }
                prev = prevSignificantIndex(tokens, prev - 1);
            }
            if (prev >= 0 && tokens.get(prev).type == TokenType.KEYWORD && "throws".equals(tokens.get(prev).text)) {
                return prevSignificantIndex(tokens, prev - 1);
            }
            if (prev >= 0 && isPunct(tokens.get(prev), ",")) {
                i = prevSignificantIndex(tokens, prev - 1);
                continue;
            }
            return fromIdx;
        }
        return fromIdx;
    }

    private boolean isFunctionBodyQualifier(final Token t) {
        if (t.type != TokenType.KEYWORD) {
            return false;
        }
        switch (t.text) {
            case "const":
            case "volatile":
            case "noexcept":
            case "override":
            case "final":
            case "throws":
                return true;
            default:
                return false;
        }
    }

    /** Scans backward from {@code fromIdx} through a trailing return type, tracking angle-bracket
     *  and paren depth; returns the function's close paren that precedes {@code ->} (skipping any
     *  post-paren qualifiers between {@code )} and {@code ->}), or -1 if not found. */
    private int findCloseParenBeforeTrailingReturnType(final List<Token> tokens, final int fromIdx) {
        int depth = 0;
        for (int i = fromIdx; i >= 0; i--) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                continue;
            }
            if (isPunct(t, ">") || t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                depth++;
            } else if (isPunct(t, "<") || t.type == TokenType.ANGLE_BRACKET_OPEN) {
                depth--;
            } else if (isPunct(t, ")")) {
                depth++;
            } else if (isPunct(t, "(")) {
                depth--;
            } else if (depth == 0 && isOp(t, "->")) {
                int beforeArrow = prevSignificantIndex(tokens, i - 1);
                while (beforeArrow >= 0 && isFunctionBodyQualifier(tokens.get(beforeArrow))) {
                    beforeArrow = prevSignificantIndex(tokens, beforeArrow - 1);
                }
                return (beforeArrow >= 0 && isPunct(tokens.get(beforeArrow), ")")) ? beforeArrow : -1;
            } else if (depth == 0 && (isPunct(t, "{") || isPunct(t, "}"))) {
                return -1;
            }
        }
        return -1;
    }

    /** True iff a `NEWLINE` token appears anywhere between `openBraceIdx` and its matching `}`. */
    private boolean spansMultipleLines(final List<Token> tokens, final int openBraceIdx) {
        int depth = 0;
        for (int k = openBraceIdx; k < tokens.size(); k++) {
            final Token t = tokens.get(k);
            if (isPunct(t, "{")) {
                depth++;
            } else if (isPunct(t, "}")) {
                depth--;
                if (depth == 0) {
                    return false;
                }
            } else if (t.type == TokenType.NEWLINE) {
                return true;
            }
        }
        return false;
    }

    private int prevSignificantIndex(final List<Token> tokens, final int from) {
        int i = from;
        while (i >= 0 && isGapToken(tokens.get(i))) {
            i--;
        }
        return i;
    }

    private int matchParenBackward(final List<Token> tokens, final int closeIdx) {
        int depth = 0;
        for (int i = closeIdx; i >= 0; i--) {
            if (isPunct(tokens.get(i), ")")) {
                depth++;
            } else if (isPunct(tokens.get(i), "(")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    // ── §15 Comment Style ────────────────────────────────────────────────────────
    /**
     * Capitalizes the first letter and strips a sole trailing period (STYLE.md §15) on every
     * `//` comment, on every `/* ... *&#47;` block comment that is already a single line, and on
     * a multi-line `/* ... *&#47;` block comment that already follows the conventional ` * `
     * continuation-marker banner shape (see {@link #reformatMultiLineBlockComment}) -- a
     * multi-line block comment that does *not* already use that marker convention (raw wrapped
     * prose, commented-out code, ASCII art) is left completely untouched, per the user's resolved
     * scope decision: only normalize within an already-recognizable shape, never restructure
     * arbitrary content. STYLE.md's separator-alignment rule remains a separate, deferred item.
     * <p>Per the Resolved Design Decision ("§15 comment scope and sentence detection"), this
     * method applies unconditionally to every comment token it sees, <i>except</i> for the
     * STYLE.md-documented exemption for labels/markers/closing-comments (`// for i`,
     * `/* FALL-THROUGH *&#47;`), which is now detected structurally (RDD_KEY_75 follow-up; see
     * {@link #isClosingBraceLabelComment}) rather than relied upon via pipeline ordering alone --
     * ordering by itself (this pass must run before `BlockStructureRule`'s §7 and `SwitchRule`'s
     * §13 passes that create those comments) only holds the first time a file is formatted; a
     * re-format of already-formatted output sees the generated comments as ordinary input and,
     * without this detection, would wrongly capitalize a lowercase label like `// switch`.
     */
    public String enforceCommentStyle(final List<Token> tokens) {
        final Map<Integer, String> lineCommentContent = computeLineCommentGroups(tokens);
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.frozen) {
                out.append(t.text);
            } else if (t.type == TokenType.COMMENT_LINE) {
                final String content = lineCommentContent.get(i);
                if (content == null) {
                    out.append(t.text);
                } else {
                    out.append("//").append(content);
                }
            } else if (t.type == TokenType.COMMENT_BLOCK && !t.text.contains("\n") && !t.text.contains("\r")) {
                final String inner = t.text.substring(2, t.text.length() - 2);
                if ("FALL-THROUGH".equals(inner.trim())) {
                    out.append(t.text);
                } else {
                    out.append("/*").append(applyCommentTextRules(inner)).append("*/");
                }
            } else if (t.type == TokenType.COMMENT_BLOCK) {
                out.append(reformatMultiLineBlockComment(t.text, indentBefore(tokens, i)));
            } else if (t.type == TokenType.PREPROCESSOR) {
                out.append(capitalizePreprocessorTrailingComment(t.text));
            } else {
                out.append(t.text);
            }
        }
        return out.toString();
    }

    /** A `#define NAME VALUE // comment` line is lexed as one opaque {@code PREPROCESSOR} token
     *  (see {@link com.jxmake.formatter.tokenizer.TokenizerCore.TokenType#PREPROCESSOR}'s own
     *  doc), so its trailing `//` comment never becomes a separate {@code COMMENT_LINE} token and
     *  is skipped by the loop above. Applies the same capitalization rule directly to the text
     *  portion after a top-level (not inside a string/char literal) `//`, if any. */
    private String capitalizePreprocessorTrailingComment(final String text) {
        final int idx = findTopLevelLineCommentStart(text);
        if (idx < 0) {
            return text;
        }
        final String before = text.substring(0, idx);
        final String inner = text.substring(idx + 2);
        return before + "//" + capitalizeFirstLetter(inner);
    }

    /** Index of a `//` sequence not nested inside a `"..."` or `'...'` literal, or -1 if none. */
    private int findTopLevelLineCommentStart(final String text) {
        boolean inString = false;
        boolean inChar = false;
        for (int i = 0; i < text.length() - 1; i++) {
            final char c = text.charAt(i);
            if (inString) {
                if (c == '\\') {
                    i++;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (inChar) {
                if (c == '\\') {
                    i++;
                } else if (c == '\'') {
                    inChar = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '\'') {
                inChar = true;
            } else if (c == '/' && text.charAt(i + 1) == '/') {
                return i;
            }
        }
        return -1;
    }

    /** Groups consecutive `//` line comments -- back to back with no blank line between -- into
     *  one §15 sentence-detection unit, the same way a multi-line `/* ... *&#47;` block comment
     *  already is: the trailing period is stripped only when it is the sole `.` across every line
     *  of the group, not just the last line alone. A closing-brace-label comment
     *  ({@link #isClosingBraceLabelComment}) breaks the chain entirely (never a link). A
     *  separator-alignment comment ({@link #parseSeparatorComment}) still counts as a chain link
     *  for dot-counting purposes (an ordinary prose sentence can coincidentally look like one, see
     *  RDD_KEY_47 follow-up) but is never itself rewritten -- rendered verbatim, exactly as before.
     *  Returns each rewritable group member's already-capitalized, period-decided replacement
     *  content, keyed by its token index; a token absent from the map must be rendered verbatim by
     *  the caller. */
    private Map<Integer, String> computeLineCommentGroups(final List<Token> tokens) {
        final Map<Integer, String> result = new HashMap<>();
        int i = 0;
        while (i < tokens.size()) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.COMMENT_LINE && isCommentChainLink(tokens, i)) {
                final List<Integer> group = new ArrayList<>();
                group.add(i);
                int j = i;
                int next;
                while ((next = nextCommentChainLinkIfAdjacent(tokens, j)) >= 0) {
                    group.add(next);
                    j = next;
                }
                final List<String> contents = new ArrayList<>();
                for (final int idx : group) {
                    contents.add(tokens.get(idx).text.substring(2));
                }
                final int lastIdx = group.size() - 1;
                if (isCommentRewritable(tokens, group.get(lastIdx))) {
                    stripSoleTrailingPeriodAcrossLines(contents);
                }
                for (int k = 0; k < group.size(); k++) {
                    if (isCommentRewritable(tokens, group.get(k))) {
                        result.put(group.get(k), capitalizeFirstLetter(contents.get(k)));
                    }
                }
                i = j + 1;
            } else {
                i++;
            }
        }
        return result;
    }

    /** True iff the {@code COMMENT_LINE} token at {@code idx} extends a §15 sentence-detection
     *  chain: any plain `//` comment that is not a closing-brace label. */
    private boolean isCommentChainLink(final List<Token> tokens, final int idx) {
        return !isClosingBraceLabelComment(tokens, idx);
    }

    /** True iff the {@code COMMENT_LINE} token at {@code idx} may actually be rewritten (not a
     *  separator-alignment label, handled instead by {@link #alignCommentSeparators}). */
    private boolean isCommentRewritable(final List<Token> tokens, final int idx) {
        final Token t = tokens.get(idx);
        return parseSeparatorComment(t.text, idx) == null;
    }

    /** If the token at {@code idx} is a `//` chain-link comment, and it is followed -- after
     *  exactly one {@code NEWLINE} and only {@code WHITESPACE} otherwise (no blank line, no other
     *  token) -- by another chain-link `//` comment, returns that next comment's token index;
     *  otherwise returns -1. */
    private int nextCommentChainLinkIfAdjacent(final List<Token> tokens, final int idx) {
        int p = idx + 1;
        int newlineCount = 0;
        while (p < tokens.size()) {
            final TokenType type = tokens.get(p).type;
            if (type == TokenType.WHITESPACE) {
                p++;
            } else if (type == TokenType.NEWLINE) {
                newlineCount++;
                if (newlineCount > 1) {
                    return -1;
                }
                p++;
            } else {
                break;
            }
        }
        if (p >= tokens.size() || newlineCount != 1) {
            return -1;
        }
        if (tokens.get(p).type == TokenType.COMMENT_LINE && isCommentChainLink(tokens, p)) {
            return p;
        }
        return -1;
    }

    /** True iff the {@code COMMENT_LINE} token at {@code idx} is immediately preceded, on the
     *  same physical line (only {@code WHITESPACE} in between, no {@code NEWLINE}), by a `}` --
     *  optionally followed by a `;` (C/C++ `struct`/`class`/`enum`/`union` definitions) -- the
     *  exact shape {@code BlockStructureRule.addClosingComments} generates (STYLE.md §7's
     *  `// label` closing comments). Catches both freshly-generated and user-written instances of
     *  this shape alike, consistent with STYLE.md's own "labels/markers/closing-comments" framing
     *  not distinguishing the two. */
    private boolean isClosingBraceLabelComment(final List<Token> tokens, final int idx) {
        int p = idx - 1;
        while (p >= 0 && tokens.get(p).type == TokenType.WHITESPACE) {
            p--;
        }
        if (p < 0) {
            return false;
        }
        if (isPunct(tokens.get(p), ";")) {
            p--;
            while (p >= 0 && tokens.get(p).type == TokenType.WHITESPACE) {
                p--;
            }
            if (p < 0) {
                return false;
            }
        }
        if (!isPunct(tokens.get(p), "}")) {
            return false;
        }
        // A generated/genuine closing-comment label only ever follows a `}` that sits alone on
        // its own line (STYLE.md §7's rendering); a `}` sharing its line with the rest of a
        // one-liner body (`{ return v_; }`) can never carry one, so a trailing comment there
        // (e.g. a one-liner getter's `// getter`) is just an ordinary comment.
        int q = p - 1;
        while (q >= 0 && tokens.get(q).type == TokenType.WHITESPACE) {
            q--;
        }
        return q < 0 || tokens.get(q).type == TokenType.NEWLINE;
    }

    /** One trailing `//` comment recognized as a separator-alignment label, parsed from a single
     *  comment token. */
    private static final class SepMatch {
        final int tokenIndex;
        final String label;
        final char sep;
        final String rest;

        SepMatch(final int tokenIndex, final String label, final char sep, final String rest) {
            this.tokenIndex = tokenIndex;
            this.label = label;
            this.sep = sep;
            this.rest = rest;
        }
    }

    /**
     * Resolved -- see STATE.md "§15 separator alignment": a trailing `//` comment qualifies as a
     * separator-alignment label iff its text (after `//`) contains exactly one character that is
     * (a) not a Unicode letter or digit ({@code Character.isLetterOrDigit}, so accented letters
     * like `ü` count as alphanumeric and can never be a separator) and (b) flanked by a literal
     * space on both sides. That single character is the separator; everything before it (trimmed)
     * is the label, everything after it (trimmed) is the rest. A comment with zero or 2+ such
     * candidates, or where label/rest would be empty, does not qualify and returns {@code null}.
     */
    private SepMatch parseSeparatorComment(final String commentText, final int tokenIndex) {
        final String content = commentText.substring(2);
        int sepPos = -1;
        for (int i = 1; i < content.length() - 1; i++) {
            final char c = content.charAt(i);
            if (Character.isWhitespace(c) || Character.isLetterOrDigit(c)) {
                continue;
            }
            if (content.charAt(i - 1) == ' ' && content.charAt(i + 1) == ' ') {
                if (sepPos != -1) {
                    return null;
                }
                sepPos = i;
            }
        }
        if (sepPos == -1) {
            return null;
        }
        final String label = content.substring(0, sepPos).trim();
        final String rest = content.substring(sepPos + 1).trim();
        if (label.isEmpty() || rest.isEmpty()) {
            return null;
        }
        return new SepMatch(tokenIndex, label, content.charAt(sepPos), rest);
    }

    /**
     * STYLE.md §15 separator alignment (resolved -- see STATE.md "§15 separator alignment"):
     * pads the label portion of trailing `//` comments so a shared separator character lines up
     * vertically across a run of physically-adjacent lines, independent of what kind of statement
     * (if any) precedes the comment on each line -- this rule looks only at comment text, never
     * at another rule's alignment-group structure. A "line" is the token span between two
     * `NEWLINE` tokens (or list start/end); it qualifies only if its last significant token is a
     * `COMMENT_LINE` that {@link #parseSeparatorComment} recognizes. A blank line, a line with no
     * trailing comment, or a comment that doesn't match the separator shape naturally breaks the
     * run (it simply doesn't qualify) -- same "doesn't match, breaks the group" posture used by
     * every other grouping rule in this file. A run must also share the same separator character
     * to stay together, and must have at least 2 qualifying lines before anything is rewritten --
     * a lone qualifying line is left byte-for-byte untouched, same minimum-group-size precedent as
     * §14's getter/setter grouping.
     */
    public String alignCommentSeparators(final List<Token> tokens) {
        final List<SepMatch> perLine = new ArrayList<>();
        int lineStart = 0;
        for (int i = 0; i <= tokens.size(); i++) {
            if (i == tokens.size() || tokens.get(i).type == TokenType.NEWLINE) {
                perLine.add(findTrailingSeparatorComment(tokens, lineStart, i));
                lineStart = i + 1;
            }
        }

        final Map<Integer, String> rewrites = new HashMap<>();
        int runStart = 0;
        while (runStart < perLine.size()) {
            if (perLine.get(runStart) == null) {
                runStart++;
                continue;
            }
            int runEnd = runStart + 1;
            while (runEnd < perLine.size() && perLine.get(runEnd) != null
                    && perLine.get(runEnd).sep == perLine.get(runStart).sep) {
                runEnd++;
            }
            if (runEnd - runStart >= 2) {
                int maxLabelLen = 0;
                for (int i = runStart; i < runEnd; i++) {
                    maxLabelLen = Math.max(maxLabelLen, perLine.get(i).label.length());
                }
                for (int i = runStart; i < runEnd; i++) {
                    final SepMatch m = perLine.get(i);
                    final String newText = "// " + padRight(m.label, maxLabelLen) + " " + m.sep + " " + m.rest;
                    rewrites.put(m.tokenIndex, newText);
                }
            }
            runStart = runEnd;
        }

        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            final String rewritten = rewrites.get(i);
            out.append(rewritten != null ? rewritten : tokens.get(i).text);
        }
        return out.toString();
    }

    /** The {@link SepMatch} for the line spanning {@code [from, to)}, or {@code null} if that
     *  line's last significant token isn't a qualifying separator-alignment `//` comment. */
    private SepMatch findTrailingSeparatorComment(final List<Token> tokens, final int from, final int to) {
        for (int i = to - 1; i >= from; i--) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.WHITESPACE) {
                continue;
            }
            if (t.type != TokenType.COMMENT_LINE || t.frozen) {
                return null;
            }
            return parseSeparatorComment(t.text, i);
        }
        return null;
    }

    /**
     * Normalizes a multi-line `/* ... *&#47;` block comment into STYLE.md §15's banner shape
     * (`/*` alone on its own line, each content line as ` * &lt;content&gt;`, `*&#47;` alone on
     * its own line, all indented to match {@code indent} -- the original comment's own line
     * indentation) -- but only when every physical line is already recognizable as using the
     * conventional `*`-per-line continuation-marker convention (resolved -- see "§15 multi-line
     * block comment banner reformatting" in STATE.md). If any continuation line, once its
     * leading whitespace is stripped, does not start with `*`, the whole comment is returned
     * unchanged -- this is what correctly skips raw wrapped prose and commented-out code, neither
     * of which has a STYLE.md worked example sanctioning a rewrite.
     * <p>Once recognized, each line's content is extracted by stripping its leading whitespace,
     * its leading `*`, and at most one following space (the closing line additionally has its
     * trailing `*&#47;` stripped first). The first and last physical lines (which carry `/*` and
     * `*&#47;` respectively) only contribute a content line if what remains is non-empty; a
     * genuinely blank *middle* line (a bare `*` with nothing else) is preserved as an intentional
     * blank paragraph separator. Text rules then apply across the whole extracted content exactly
     * like the single-line case, generalized over multiple lines: the first content line's
     * leading letter is always capitalized; the trailing period on the very last content line is
     * stripped only if it is the sole `.` across all content lines (an ellipsis, or any
     * abbreviation followed by more sentence text, is never touched), matching STYLE.md's "single
     * sentence never ends in a period, even one that merely got wrapped onto multiple physical
     * lines" reading already established for `//` comments.
     */
    private String reformatMultiLineBlockComment(final String text, final String indent) {
        final String[] rawLines = text.split("\r\n|\r|\n", -1);
        final int n = rawLines.length;
        for (int i = 1; i < n; i++) {
            if (!stripLeadingWhitespace(rawLines[i]).startsWith("*")) {
                return text;
            }
        }

        final String firstContent = rawLines[0].substring(2).trim();

        final String lastStripped = stripLeadingWhitespace(rawLines[n - 1]);
        final String lastContent;
        if ("*/".equals(lastStripped)) {
            lastContent = "";
        } else {
            final String afterMarker = afterLeadingStarMarker(lastStripped);
            if (!afterMarker.endsWith("*/")) {
                return text;
            }
            lastContent = trimTrailing(afterMarker.substring(0, afterMarker.length() - 2));
        }

        final List<String> contentLines = new ArrayList<>();
        if (!firstContent.isEmpty()) {
            contentLines.add(firstContent);
        }
        for (int i = 1; i < n - 1; i++) {
            contentLines.add(trimTrailing(afterLeadingStarMarker(stripLeadingWhitespace(rawLines[i]))));
        }
        if (!lastContent.isEmpty()) {
            contentLines.add(lastContent);
        }

        if (!contentLines.isEmpty()) {
            contentLines.set(0, capitalizeFirstLetter(contentLines.get(0)));
        }
        stripSoleTrailingPeriodAcrossLines(contentLines);

        final StringBuilder out = new StringBuilder("/*");
        for (final String line : contentLines) {
            out.append('\n').append(indent).append(" *");
            if (!line.isEmpty()) {
                out.append(' ').append(line);
            }
        }
        out.append('\n').append(indent).append(" */");
        return out.toString();
    }

    /** Drops a line's leading whitespace, returning the remainder unchanged. */
    private String stripLeadingWhitespace(final String line) {
        int i = 0;
        while (i < line.length() && Character.isWhitespace(line.charAt(i))) {
            i++;
        }
        return line.substring(i);
    }

    /** Drops a leading `*` and at most one space immediately after it. Caller must have already
     *  verified {@code wsStrippedLine} starts with `*`. */
    private String afterLeadingStarMarker(final String wsStrippedLine) {
        String rest = wsStrippedLine.substring(1);
        if (rest.startsWith(" ")) {
            rest = rest.substring(1);
        }
        return rest;
    }

    private String trimTrailing(final String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    /** Cross-line generalization of {@link #stripSoleTrailingPeriod}: strips the trailing `.` on
     *  the last entry only when it is the sole `.` across every entry. */
    private void stripSoleTrailingPeriodAcrossLines(final List<String> lines) {
        if (!normalizeCommentEndPeriod || lines.isEmpty()) {
            return;
        }
        int dotCount = 0;
        for (final String l : lines) {
            for (int i = 0; i < l.length(); i++) {
                if (l.charAt(i) == '.') {
                    dotCount++;
                }
            }
        }
        if (dotCount != 1) {
            return;
        }
        final int lastIdx = lines.size() - 1;
        final String last = lines.get(lastIdx);
        if (last.isEmpty() || last.charAt(last.length() - 1) != '.') {
            return;
        }
        lines.set(lastIdx, last.substring(0, last.length() - 1));
    }

    /** The leading whitespace of the line containing the token at idx, or "" if it isn't first on
     *  its line -- same precedent as `BlockStructureRule.indentBefore`. */
    private String indentBefore(final List<Token> tokens, final int idx) {
        final StringBuilder indent = new StringBuilder();
        int i = idx - 1;
        while (i >= 0 && tokens.get(i).type == TokenType.WHITESPACE) {
            indent.insert(0, tokens.get(i).text);
            i--;
        }
        return (i < 0 || tokens.get(i).type == TokenType.NEWLINE) ? indent.toString() : "";
    }

    private String applyCommentTextRules(final String content) {
        return stripSoleTrailingPeriod(capitalizeFirstLetter(content));
    }

    private String capitalizeFirstLetter(final String content) {
        if (!normalizeCommentStartCase) {
            return content;
        }
        for (int i = 0; i < content.length(); i++) {
            final char c = content.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (Character.isLetter(c) && Character.isLowerCase(c)) {
                // Extract the first word to check whether it is a keyword.
                int end = i;
                while (end < content.length()
                        && (Character.isLetterOrDigit(content.charAt(end))
                                || content.charAt(end) == '_')) {
                    end++;
                }
                if (isCommentNoCapitalizeWord(content.substring(i, end))) {
                    return content;
                }
                return content.substring(0, i) + Character.toUpperCase(c) + content.substring(i + 1);
            }
            break;
        }
        return content;
    }

    /** True iff `word` is a keyword in the current file's language ({@link #lang}) that must
     *  never be titlecased when it starts a comment sentence -- checked against the
     *  language-specific set only, so a C/C++-only keyword like `inline` never suppresses
     *  capitalization in a Java comment, and vice versa. */
    private boolean isCommentNoCapitalizeWord(final String word) {
        if (lang.isJava) {
            return COMMENT_NO_CAPITALIZE_JAVA.contains(word);
        }
        if (lang.isCpp) {
            return COMMENT_NO_CAPITALIZE_C.contains(word) || COMMENT_NO_CAPITALIZE_CPP.contains(word);
        }
        return COMMENT_NO_CAPITALIZE_C.contains(word);
    }

    /** Strips the trailing `.` only when it is the sole `.` in `content` -- this also leaves an
     *  ellipsis (`...`) untouched for free, since an ellipsis's dot count is never exactly 1. */
    private String stripSoleTrailingPeriod(final String content) {
        if (!normalizeCommentEndPeriod) {
            return content;
        }
        int end = content.length();
        while (end > 0 && Character.isWhitespace(content.charAt(end - 1))) {
            end--;
        }
        if (end == 0 || content.charAt(end - 1) != '.') {
            return content;
        }
        int dotCount = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '.') {
                dotCount++;
            }
        }
        if (dotCount != 1) {
            return content;
        }
        return content.substring(0, end - 1) + content.substring(end);
    }

    // ── §8 Function Calls and Forward Declarations ──────────────────────────────
    /** Same literal-4-space unit as `CppSpecificRule`/`JavaSpecificRule`/`SwitchRule`'s own
     *  `DEFAULT_INDENT_UNIT` -- this pass generates new indentation as raw text appended to an
     *  existing line's own leading whitespace (see {@link #lineIndent}), the same precedent as
     *  `CppSpecificRule.enforceRequiresClausePlacement`, rather than going through
     *  {@link #indentText}'s `indentLevel`+`indentStyle` scheme: this is a flat whole-file token
     *  pass with no tracked recursion depth (unlike `ScopePipeline.applySignaturePass`, which is
     *  why true signatures already have an integer depth to call {@link #render} with and calls/
     *  declarations here don't). The later §1 `convertIndentation` pass normalizes whichever style
     *  the project actually wants from this raw text. */
    private static final String DEFAULT_INDENT_UNIT = "    ";

    /**
     * Finds every call/forward-declaration candidate -- an IDENTIFIER immediately followed by a
     * `(` whose matching `)` is *not* immediately followed by `{` (that shape is a true function
     * signature, already fully handled, deterministically, by `ScopePipeline.applySignaturePass`
     * before this pass ever runs) -- and rewrites it per STYLE.md §8's four candidate forms
     * (RDD_EXT_4 through RDD_EXT_9; see also RDD_KEY_4 for this method's own architecture). Three
     * scope-limiting decisions, each consistent with this file's existing conservative posture
     * elsewhere, were made writing this method (see RDD_KEY_5 for the full write-up):
     * <ul>
     * <li><b>Nesting:</b> once a `(` is recognized as a candidate (regardless of whether it ends
     * up rewritten), every token through its matching `)` is "claimed" and skipped for the rest of
     * this scan -- a call/declaration nested inside another's argument list is never independently
     * processed in the same pass; its original text rides along verbatim as part of the outer
     * candidate's own rendering (or is left alone if the outer doesn't rewrite). A later format run
     * would pick up a still-too-long nested call once the outer has already been broken and the
     * nested one has more room, or remains its own top-level candidate if the outer no longer
     * contains it.</li>
     * <li><b>Comments:</b> any comment token anywhere between a candidate's `(` and `)` disqualifies
     * the *entire* candidate -- it is left byte-for-byte untouched. STYLE.md §8's fuller comment
     * rules (trailing-comment-preserved, comment-only-line-preserved, inline-block-comment-
     * normalized-in-place, leading-preamble-comment-disqualifies) are not implemented; this is a
     * deliberate, documented gap, the same "a comment in the gap blocks the rewrite" posture used by
     * {@link #enforceKeywordSpacing} and `CppSpecificRule.enforceRequiresClausePlacement`.</li>
     * <li><b>Call vs. declaration:</b> {@link #parseSignature} is attempted on the candidate's own
     * `name(...)` span; success (every comma-separated segment fits {@link Param}'s typed
     * "[type] name [size]" shape) means a forward declaration -- rendered with the existing typed
     * {@link Signature}/{@link Param} machinery. Failure means a plain call -- rendered from the
     * raw, untyped per-argument token slices instead (no type/name split attempted).</li>
     * </ul>
     * A zero-parameter candidate (including an explicit C `(void)`, which still parses to an empty
     * {@link Signature}) is always left alone -- breaking achieves nothing with no argument to place
     * on its own line, same precedent as {@link #render}'s own zero-param handling.
     */
    public String enforceCallLineBreaking(final List<Token> tokens) {
        final List<int[]> spans = new ArrayList<>();
        final List<String> renders = new ArrayList<>();
        int scanCursor = 0;

        for (int i = 0; i < tokens.size(); i++) {
            if (i < scanCursor || !isPunct(tokens.get(i), "(")) {
                continue;
            }
            final int nameIdx = prevSignificantIndex(tokens, i - 1);
            if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
                continue;
            }
            final int closeIdx = matchParenForward(tokens, i);
            if (closeIdx < 0) {
                continue;
            }
            scanCursor = closeIdx + 1; // claim the interior -- see "Nesting" above, applies even if skipped below

            final int afterClose = nextSignificantIndex(tokens, closeIdx + 1);
            if (afterClose >= 0 && isPunct(tokens.get(afterClose), "{")) {
                continue; // true signature -- ScopePipeline's concern, not ours
            }
            if (hasCommentBetween(tokens, i, closeIdx)) {
                continue; // see "Comments" above
            }
            if (anyFrozen(tokens, nameIdx, closeIdx + 1)) {
                continue; // frozen span (RDD_KEY_90 §A) -- left untouched
            }

            final String rendered = renderCallCandidate(tokens, nameIdx, i, closeIdx);
            if (rendered != null) {
                spans.add(new int[] { i, closeIdx + 1 });
                renders.add(rendered);
            }
        }

        if (spans.isEmpty()) {
            return joinVerbatim(tokens);
        }
        final StringBuilder out = new StringBuilder();
        int cursor = 0;
        for (int s = 0; s < spans.size(); s++) {
            final int[] span = spans.get(s);
            appendRange(out, tokens, cursor, span[0]);
            out.append(renders.get(s));
            cursor = span[1];
        }
        appendRange(out, tokens, cursor, tokens.size());
        return out.toString();
    }

    /** Dispatches one candidate (its `(`/`)` span already located by the caller) to the
     *  appropriate option per RDD_EXT_4's candidate-availability matrix, collapsed to the
     *  deterministic subset since AI selection (Step 2) is permanently not feasible: a multi-line
     *  source candidate always gets Option 2 (preserve groups); a single-line candidate that
     *  already fits returns {@code null} (Option 0, no change); otherwise Option 1 (dropped),
     *  falling back to Option 3 (one-per-line) if dropped itself doesn't fit. Returns the
     *  replacement text for the half-open span {@code [openIdx, closeIdx]} (i.e. starting with the
     *  `(` token's own text and ending with the `)` token's own text), or {@code null} for no
     *  change. */
    private String renderCallCandidate(final List<Token> tokens, final int nameIdx, final int openIdx,
            final int closeIdx) {
        final Signature sig = parseSignature(tokens.subList(nameIdx, closeIdx + 1));
        if (sig != null && sig.params.isEmpty()) {
            return null; // zero-param -- never broken, see this method's class-level doc comment
        }

        final List<Token> paramsSlice = tokens.subList(openIdx + 1, closeIdx);
        final String baseIndent = lineIndent(tokens, nameIdx);

        if (containsNewline(paramsSlice)) {
            // renderCallPreserveGroups/renderDeclarationPreserveGroups re-split each original
            // source line independently, resetting paren-depth tracking to 0 per line -- correct
            // only when paramsSlice actually holds multiple top-level (sibling) arguments. A single
            // argument that itself spans multiple lines (e.g. a lone nested call whose own
            // argument list wraps) has zero top-level commas in the full slice; splitting it
            // per-line would misdetect a nested comma sitting at a line break as a local top-level
            // split point and then append a synthetic comma on top of the one already present,
            // duplicating it. Leave such single-argument candidates untouched (Option 0).
            if (splitTopLevelCommas(paramsSlice).size() <= 1) {
                return null;
            }
            final List<String> lines = (sig != null)
                    ? renderDeclarationPreserveGroups(paramsSlice, baseIndent)
                    : renderCallPreserveGroups(paramsSlice, baseIndent);
            return lines == null ? null : "(\n" + String.join("\n", lines);
        }

        final String wholeLine = baseIndent
                + collapseToOneLine(tokens, lineStartIndex(tokens, nameIdx), lineEndIndex(tokens, closeIdx) - 1);
        if (wholeLine.length() <= lineLengthLimit) {
            return null; // Option 0 -- already fits, no change
        }

        final List<String> dropped = (sig != null) ? renderDropped(sig, baseIndent) : renderCallDropped(paramsSlice, baseIndent);
        final List<String> lines = (dropped != null) ? dropped
                : (sig != null) ? renderOnePerLine(sig, baseIndent) : renderCallOnePerLine(paramsSlice, baseIndent);
        return "(\n" + String.join("\n", lines);
    }

    /** Option 1 (dropped form) for a forward declaration: every param on one line, indented one
     *  level under {@code baseIndent}, with `)` on its own line back at {@code baseIndent} --
     *  mirrors {@link #renderParamsInline}'s join. Returns {@code null} (caller falls back to
     *  {@link #renderOnePerLine}, Option 3) if even this single params line exceeds
     *  {@link #lineLengthLimit}. */
    private List<String> renderDropped(final Signature sig, final String baseIndent) {
        final String paramsLine = baseIndent + DEFAULT_INDENT_UNIT + renderParamsInline(sig);
        if (paramsLine.length() > lineLengthLimit) {
            return null;
        }
        return Arrays.asList(paramsLine, baseIndent + ")");
    }

    /** Option 3 (one-per-line) for a forward declaration -- same type-column-padded shape as
     *  {@link #render}'s broken form, parameterized by raw {@code baseIndent} text instead of an
     *  integer level (this pass has no tracked recursion depth, see
     *  {@link #DEFAULT_INDENT_UNIT}'s doc comment), so this is a deliberate sibling rather than a
     *  direct reuse of {@link #render}. */
    private List<String> renderOnePerLine(final Signature sig, final String baseIndent) {
        int maxTypeLen = 0;
        for (final Param p : sig.params) {
            if (p.leadingComment == null) {
                maxTypeLen = Math.max(maxTypeLen, renderTokens(p.typeTokens).length());
            }
        }
        final int typeColWidth = maxTypeLen + 1;
        final String paramIndent = baseIndent + DEFAULT_INDENT_UNIT;
        int maxNameCommaLen = 0;
        for (int i = 0; i < sig.params.size(); i++) {
            final Param p = sig.params.get(i);
            final String nameCommaText = p.name.text + renderTokens(p.sizeTokens)
                    + (i < sig.params.size() - 1 ? "," : "");
            maxNameCommaLen = Math.max(maxNameCommaLen, nameCommaText.length());
        }

        final List<String> lines = new ArrayList<>();
        for (int i = 0; i < sig.params.size(); i++) {
            final Param p = sig.params.get(i);
            final String typeText = renderTokens(p.typeTokens);
            final String nameCommaText = p.name.text + renderTokens(p.sizeTokens)
                    + (i < sig.params.size() - 1 ? "," : "");
            final String nameText = p.comment != null
                    ? padRight(nameCommaText, maxNameCommaLen) + " " + p.comment.text
                    : nameCommaText;
            final String leadPrefix = p.leadingComment != null ? p.leadingComment.text + " " : "";
            final String typeCell = p.leadingComment != null ? typeText + " " : padRight(typeText, typeColWidth);
            lines.add(paramIndent + leadPrefix + typeCell + nameText);
        }
        lines.add(baseIndent + ")");
        return lines;
    }

    /** Option 1 (dropped form) for a plain call: every argument on one line, untyped, split on
     *  top-level commas (raw -- {@link #splitTopLevelCommas} tracks paren/bracket/angle depth
     *  itself, so it doesn't need {@link #significantOnly} first) and each argument rendered via
     *  {@link #collapseTokensToOneLine} rather than {@link #renderTokens} (see that method's doc
     *  comment for why -- a nested call inside an argument must not get its own parens spread
     *  apart); the top-level `,` separators between sibling arguments are inserted explicitly here
     *  instead, normalized to `", "` regardless of original spacing. Returns {@code null} (caller
     *  falls back to {@link #renderCallOnePerLine}) if the line doesn't fit. */
    private List<String> renderCallDropped(final List<Token> paramsSlice, final String baseIndent) {
        final List<List<Token>> args = splitTopLevelCommas(paramsSlice);
        final StringBuilder argsText = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) {
                argsText.append(", ");
            }
            argsText.append(collapseTokensToOneLine(args.get(i)));
        }
        final String paramsLine = baseIndent + DEFAULT_INDENT_UNIT + argsText;
        if (paramsLine.length() > lineLengthLimit) {
            return null;
        }
        return Arrays.asList(paramsLine, baseIndent + ")");
    }

    /** Option 3 (one-per-line) for a plain call: each top-level argument on its own line, no
     *  column alignment (untyped arguments have no type column to align) -- only
     *  {@link #splitTopLevelCommas} is needed, not the typed {@link #parseParam} machinery; each
     *  argument rendered via {@link #collapseTokensToOneLine}, same nested-call-safety reason as
     *  {@link #renderCallDropped}. */
    private List<String> renderCallOnePerLine(final List<Token> paramsSlice, final String baseIndent) {
        final List<List<Token>> args = splitTopLevelCommas(paramsSlice);
        final String argIndent = baseIndent + DEFAULT_INDENT_UNIT;

        final List<String> lines = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            lines.add(argIndent + collapseTokensToOneLine(args.get(i)) + (i < args.size() - 1 ? "," : ""));
        }
        lines.add(baseIndent + ")");
        return lines;
    }

    /** Option 2 (preserve groups) for a plain call: keeps the source's existing line breaks
     *  exactly (one output line per original physical line that had any significant content) --
     *  a line that turns out to have nothing significant on it (a stray blank line) is dropped
     *  entirely -- no STYLE.md worked example sanctions preserving a blank line inside an argument
     *  list. Within each line, arguments are split on top-level commas and each rendered via
     *  {@link #collapseTokensToOneLine} (not {@link #renderTokens} -- same nested-call-safety
     *  reason as {@link #renderCallDropped}); sibling arguments on the same line are joined with an
     *  explicit normalized `", "`, and a line's last argument gets a trailing `,` unless it is the
     *  very last argument overall (mirrors {@link #renderDeclarationPreserveGroups}'s
     *  {@code isLastOverall} logic, so a multi-arg trailing line like `c, d,` / `e` renders with the
     *  comma exactly where the original grouping implies more follows). Every preserved line is
     *  re-indented to one level under {@code baseIndent} -- "preserve groups" means preserve which
     *  arguments share a line, not preserve arbitrary original indentation depth, same distinction
     *  {@link #renderDeclarationPreserveGroups} makes. */
    private List<String> renderCallPreserveGroups(final List<Token> paramsSlice, final String baseIndent) {
        final List<List<List<Token>>> rows = groupByOriginalLine(paramsSlice);
        if (rows.isEmpty()) {
            return null; // shouldn't happen -- caller only calls this when a newline was found -- bail safe
        }

        final String argIndent = baseIndent + DEFAULT_INDENT_UNIT;
        final List<String> lines = new ArrayList<>();
        for (int r = 0; r < rows.size(); r++) {
            final List<List<Token>> row = rows.get(r);
            final StringBuilder line = new StringBuilder(argIndent);
            for (int c = 0; c < row.size(); c++) {
                if (c > 0) {
                    line.append(", ");
                }
                line.append(collapseTokensToOneLine(row.get(c)));
                if (c == row.size() - 1 && !(r == rows.size() - 1 && c == row.size() - 1)) {
                    line.append(",");
                }
            }
            lines.add(line.toString());
        }
        lines.add(baseIndent + ")");
        return lines;
    }

    /** Option 2 (preserve groups) for a forward declaration: same per-original-line grouping as
     *  {@link #renderCallPreserveGroups}, but each line's params are parsed (typed) via
     *  {@link #parseParam} and laid out in a {@link ColumnGrid} with two columns per parameter
     *  slot position (type, name) -- slot N's type/name cells align vertically across every line
     *  that has an Nth parameter, matching STYLE_NEXT_EXT.md's worked example, reusing
     *  `DeclarationAlignmentRule`'s "plain `ColumnGrid` + one join space" convention (not
     *  {@link #render}'s own `maxTypeLen + 1` convention, which RDD_EXT_4 ties specifically to §5's
     *  grid, not §8's signature padding). No comment column is needed here: a candidate with any
     *  comment between its parens is already rejected entirely by {@link #enforceCallLineBreaking}
     *  before this method is ever called, so STYLE.md §8's per-line trailing-comment rule for this
     *  option is a documented gap, not implemented (see RDD_KEY_5). Returns {@code null} -- leaving
     *  the whole candidate untouched -- if any line's params don't reduce to {@link Param}'s typed
     *  shape (the same "bail the whole signature" posture {@link #parseSignature} itself uses). */
    private List<String> renderDeclarationPreserveGroups(final List<Token> paramsSlice, final String baseIndent) {
        final List<List<Param>> rows = new ArrayList<>();
        for (final List<List<Token>> lineParts : groupByOriginalLine(paramsSlice)) {
            final List<Param> row = new ArrayList<>();
            for (final List<Token> part : lineParts) {
                final Param p = parseParam(significantOnly(part));
                if (p == null) {
                    return null;
                }
                row.add(p);
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        if (rows.isEmpty()) {
            return null; // shouldn't happen -- caller only calls this when a newline was found -- bail safe
        }

        final ColumnGrid grid = new ColumnGrid();
        for (int r = 0; r < rows.size(); r++) {
            final List<Param> row = rows.get(r);
            final String[] cells = new String[row.size() * 2];
            for (int c = 0; c < row.size(); c++) {
                final Param p = row.get(c);
                final boolean isLastOverall = (r == rows.size() - 1) && (c == row.size() - 1);
                cells[c * 2] = renderTokens(p.typeTokens);
                cells[c * 2 + 1] = p.name.text + renderTokens(p.sizeTokens) + (isLastOverall ? "" : ",");
            }
            grid.addRow(cells);
        }

        final String argIndent = baseIndent + DEFAULT_INDENT_UNIT;
        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(argIndent + String.join(" ", row));
        }
        lines.add(baseIndent + ")");
        return lines;
    }

    /** True iff any token in {@code [fromExclusive, toExclusive)} is a {@code NEWLINE} -- the
     *  multi-line-source detection signal for {@link #renderCallCandidate}'s Option 2 branch. */
    private boolean containsNewline(final List<Token> tokens) {
        for (final Token t : tokens) {
            if (t.type == TokenType.NEWLINE) {
                return true;
            }
        }
        return false;
    }

    /** True iff any token in {@code (fromExclusive, toExclusive)} is a comment -- same "comment in
     *  the gap blocks the rewrite" signal as `CppSpecificRule.hasCommentBetween`, duplicated here
     *  per this file's established no-shared-helpers-across-rule-classes precedent (see
     *  {@link #renderTokens}'s doc comment). */
    private boolean hasCommentBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    /** Appends {@code tokens[fromInclusive, toExclusive)}'s own text verbatim -- same precedent as
     *  `CppSpecificRule.appendRange`, duplicated here (see {@link #hasCommentBetween}'s doc
     *  comment). */
    private void appendRange(final StringBuilder out, final List<Token> tokens, final int fromInclusive,
            final int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            out.append(tokens.get(i).text);
        }
    }

    /** Renders {@code tokens[fromInclusive, toInclusive]} verbatim except every whitespace/newline
     *  run collapses to exactly one space -- same precedent as
     *  `CppSpecificRule.collapseToOneLine`, duplicated here (see {@link #hasCommentBetween}'s doc
     *  comment); used only to *measure* a would-be single-line rendering against
     *  {@link #lineLengthLimit}, never committed to output as-is. */
    private String collapseToOneLine(final List<Token> tokens, final int fromInclusive, final int toInclusive) {
        final StringBuilder sb = new StringBuilder();
        for (int i = fromInclusive; i <= toInclusive; i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                continue;
            }
            sb.append(t.text);
        }
        return sb.toString().trim();
    }

    /** Same whitespace/newline-run-collapsing as {@link #collapseToOneLine}, but over a detached
     *  sublist (as returned by {@link #splitTopLevelCommas}/{@link #groupByOriginalLine}) rather than
     *  an index range into the original token list. Used to render one call argument's own tokens
     *  -- deliberately does *not* route through {@link #renderTokens}, since an argument may itself
     *  contain a nested call/parenthesized sub-expression (`bar(1, 2)`); `renderTokens`'s
     *  tight-attachment rules don't know `(`/`)` should stay tight to a preceding identifier (those
     *  rules were written for type-token lists, which never contain a literal call), so routing an
     *  arbitrary expression through it would spread `bar ( 1, 2 )` apart. Collapsing only existing
     *  whitespace runs -- never inserting a space where the source had none -- reproduces the
     *  nested expression's own spacing as-is instead of guessing at it, consistent with this pass's
     *  "claim and skip" rule for nested candidates (see {@link #enforceCallLineBreaking}'s doc
     *  comment): a nested call's interior is never independently analyzed, so it rides along
     *  unprocessed rather than being normalized. */
    private String collapseTokensToOneLine(final List<Token> tokens) {
        final StringBuilder sb = new StringBuilder();
        for (final Token t : tokens) {
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                continue;
            }
            sb.append(t.text);
        }
        return sb.toString().trim();
    }

    /** The index of the first significant token on the physical line containing {@code idx} --
     *  same precedent as `CppSpecificRule.lineStartIndex`, duplicated here (see
     *  {@link #hasCommentBetween}'s doc comment), adapted to this class's own
     *  {@link #nextSignificantIndex} (inclusive-of-{@code from} semantics, unlike
     *  `CppSpecificRule`'s exclusive one -- hence the {@code + 1} below). */
    private int lineStartIndex(final List<Token> tokens, final int idx) {
        int newlineIdx = -1;
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                newlineIdx = i;
                break;
            }
        }
        final int firstSig = nextSignificantIndex(tokens, newlineIdx + 1);
        return firstSig < 0 ? idx : firstSig;
    }

    /** The index one past the last significant token on the physical line containing {@code idx}
     *  (i.e. up to, but excluding, the line's own {@code NEWLINE}, or {@code tokens.size()} at
     *  end of file) -- the counterpart to {@link #lineStartIndex}, needed because a call/
     *  declaration candidate's own `)` is not necessarily the end of its physical line (e.g. a
     *  nested call like {@code std::rotr(...)} inside an outer {@code static_cast<T>( ... ) );}):
     *  measuring a candidate's "does it fit on one line" length only up to its own `)` silently
     *  ignores trailing same-line text after it, undercounting the true rendered line length. */
    private int lineEndIndex(final List<Token> tokens, final int idx) {
        int i = idx;
        while (i < tokens.size() && tokens.get(i).type != TokenType.NEWLINE) {
            i++;
        }
        return i;
    }

    /** Line-leading whitespace of the physical line containing token {@code idx} -- "" if that
     *  line has no leading whitespace (column-0 start) -- same precedent as
     *  `CppSpecificRule.lineIndent`/`JavaSpecificRule.lineIndent`, duplicated here (see
     *  {@link #hasCommentBetween}'s doc comment). Unlike {@link #indentBefore}, this works
     *  correctly even when {@code idx} itself is not the first significant token on its line (e.g.
     *  the call name in {@code auto x = foo(a, b, c);}), which {@link #enforceCallLineBreaking}'s
     *  candidates frequently are not. */
    private String lineIndent(final List<Token> tokens, final int idx) {
        int newlineIdx = -1;
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                newlineIdx = i;
                break;
            }
        }
        final int afterNewline = newlineIdx + 1;
        if (afterNewline < tokens.size() && tokens.get(afterNewline).type == TokenType.WHITESPACE) {
            return tokens.get(afterNewline).text;
        }
        return "";
    }

    // ── Token-scanning helpers ───────────────────────────────────────────────────
    // isGapToken/isPunct/isOp are centralized on TokenizerCore.Token (static-imported above).
    private boolean isCommentOrNewline(final Token t) {
        return t.type == TokenType.NEWLINE || t.type == TokenType.COMMENT_LINE
                || t.type == TokenType.COMMENT_BLOCK;
    }

    /** {@code true} if any token in {@code [fromInclusive, toExclusive)} is frozen (RDD_KEY_90
     *  §A) -- used by structural/span-level passes to skip a whole candidate unit rather than try
     *  to partially rewrite it. */
    private boolean anyFrozen(final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (tokens.get(i).frozen) {
                return true;
            }
        }
        return false;
    }
}
