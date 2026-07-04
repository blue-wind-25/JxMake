/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isComment;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BlockStructureRule {

    private static final Set<String> SINGLE_EXPR_KEYWORDS = setOf("if", "while", "for");

    // A nested compound construct is not a "single EXPRESSION" (STYLE.md §10's own title) --
    // collapsing e.g. `if(x) { if(y) foo(); }` to `if(x) if(y) foo();` would introduce a
    // dangling-construct ambiguity that the worked examples (return/continue/break) never
    // exercise, so such bodies are left braced rather than guessed at.
    private static final Set<String> COMPOUND_BODY_KEYWORDS =
            setOf("if", "while", "for", "switch", "do", "try");

    // STYLE.md §11 K&R list: keywords whose body brace is preceded by a `( ... )` condition.
    private static final Set<String> PAREN_KR_KEYWORDS = setOf("if", "while", "for", "switch", "catch");

    // STYLE.md §11 K&R list: keywords whose body brace follows the bare keyword, no condition.
    private static final Set<String> BARE_KR_KEYWORDS = setOf("else", "do", "try", "finally");

    // STYLE.md §7 default; overridable via `closing-comment-min-lines` once Config.java exists.
    private static final int DEFAULT_CLOSING_COMMENT_MIN_LINES = 5;

    private final Lang lang;
    private final int closingCommentMinLines;

    public BlockStructureRule(final Lang lang) {
        this(lang, DEFAULT_CLOSING_COMMENT_MIN_LINES);
    }

    public BlockStructureRule(final Lang lang, final int closingCommentMinLines) {
        this.lang = lang;
        this.closingCommentMinLines = closingCommentMinLines;
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
                    if (!isPartOfElseChain(tokens, i, block, n) && !anyFrozen(tokens, i, block.closeBraceIndex + 1)) {
                        final String collapsed = tryCollapse(tokens, i, block);
                        if (collapsed != null) {
                            out.append(collapsed);
                            i = block.closeBraceIndex + 1;
                            continue;
                        }
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

    /** True if the `if` at {@code kwIndex} is part of an {@code else}/
     *  {@code else if} chain -- either its closing `}` is followed by {@code else},
     *  or the keyword itself is directly preceded by {@code else} (i.e. it is an
     *  {@code else if} branch). In either case collapsing the branch to a one-liner
     *  is suppressed so all branches in the chain keep braces (STYLE_C_CPP.md §10). */
    private boolean isPartOfElseChain(final List<Token> tokens, final int kwIndex,
            final ControlBlock block, final int n) {
        final int afterClose = skipNonSignificant(tokens, block.closeBraceIndex + 1);
        if (afterClose < n && tokens.get(afterClose).type == TokenType.KEYWORD
                && "else".equals(tokens.get(afterClose).text)) {
            return true;
        }
        int prev = kwIndex - 1;
        while (prev >= 0 && (tokens.get(prev).type == TokenType.WHITESPACE
                || tokens.get(prev).type == TokenType.NEWLINE)) {
            prev--;
        }
        return prev >= 0 && tokens.get(prev).type == TokenType.KEYWORD
                && "else".equals(tokens.get(prev).text);
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

    // ── Non-function block brace style (STYLE.md §11) ───────────────────────────
    /**
     * Scans a token slice and moves every `{` that opens a control-flow body
     * (`if`/`else`/`else if`/`for`/`while`/`do`/`switch`/`try`/`catch`/`finally`), a named
     * construct (`class`/`struct`/`enum`/`enum class`/`namespace`/`interface`, already tagged
     * by the tokenizer's name stack -- see `Token.name`), or a lambda body (Java `(...) -> {`,
     * C++ `[...](...)  {` or its `-> Type {` trailing-return-type form -- STYLE_C_CPP.md §2 /
     * STYLE_JAVA.md §2) onto the same line as the preceding keyword/condition/declaration,
     * separated by exactly one space (K&R). A lambda is treated as a value embedded in a larger
     * declaration or call rather than a standalone definition, so unlike a named function it
     * does not get Allman style. Anything in the gap other than whitespace/newlines (i.e. a
     * comment) blocks the rewrite for that brace, since relocating the comment unambiguously is
     * out of scope here. A `{` that does not classify as one of those cases -- most importantly
     * a function/method definition, where the preceding `)`'s matching `(` is preceded by an
     * identifier rather than a keyword or a lambda's `]` -- is left completely untouched; Allman
     * function brace style is normalized elsewhere (Tier 1, language-specific files), not by
     * this method.
     */
    public String enforceKAndRBraceStyle(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                    || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                gap.add(t);
                i++;
                continue;
            }

            if (isPunct(t, "{") && gap.stream().noneMatch(Token::isComment)
                    && qualifiesForKAndR(tokens, i) && !t.frozen && gap.stream().noneMatch(g -> g.frozen)) {
                out.append(' ');
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            gap.clear();
            out.append(t.text);
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    /** True if the `{` at braceIdx opens a K&R-styled construct per STYLE.md §11 (see caller doc). */
    private boolean qualifiesForKAndR(final List<Token> tokens, final int braceIdx) {
        if (tokens.get(braceIdx).name != null) {
            return true;
        }

        final int prevIdx = prevSignificantIndex(tokens, braceIdx - 1);
        if (prevIdx < 0) {
            return false;
        }
        final Token prev = tokens.get(prevIdx);
        if (prev.type == TokenType.KEYWORD && BARE_KR_KEYWORDS.contains(prev.text)) {
            return true;
        }
        if (isPunct(prev, ")")) {
            final int openParenIdx = matchOpenBackward(tokens, prevIdx);
            if (openParenIdx >= 0) {
                final int kwIdx = prevSignificantIndex(tokens, openParenIdx - 1);
                if (kwIdx >= 0 && tokens.get(kwIdx).type == TokenType.KEYWORD
                        && PAREN_KR_KEYWORDS.contains(tokens.get(kwIdx).text)) {
                    return true;
                }
            }
        }
        return isLambdaBrace(tokens, prevIdx);
    }

    /**
     * True if the `{` whose immediately preceding significant token is at prevIdx opens a
     * lambda body: Java `(params) -> {` / `param -> {`, or C++ `[capture](params) {` / bare
     * `[capture] {` / either form followed by a trailing `-> Type {`.
     */
    private boolean isLambdaBrace(final List<Token> tokens, final int prevIdx) {
        final Token prev = tokens.get(prevIdx);
        if (lang.isJava) {
            return isOp(prev, "->");
        }
        if (isPunct(prev, "]")) {
            return true;
        }
        if (isPunct(prev, ")") && precededByCaptureList(tokens, prevIdx)) {
            return true;
        }
        return isCppTrailingReturnLambda(tokens, prevIdx);
    }

    /** True if the `)` at closeParenIdx's matching `(` is immediately preceded by a `]`. */
    private boolean precededByCaptureList(final List<Token> tokens, final int closeParenIdx) {
        final int openParenIdx = matchOpenBackward(tokens, closeParenIdx);
        if (openParenIdx < 0) {
            return false;
        }
        final int beforeOpen = prevSignificantIndex(tokens, openParenIdx - 1);
        return beforeOpen >= 0 && isPunct(tokens.get(beforeOpen), "]");
    }

    // Bounds the backward walk over a C++ trailing return type (`-> Type`) before giving up --
    // real return types are short, and this keeps a non-lambda `)` { with unrelated code before
    // it from causing a runaway scan.
    private static final int MAX_RETURN_TYPE_TOKENS = 20;

    /** True if `{` at prevIdx+1(gap) is a C++ lambda's `[capture](params) -> Type {` body. */
    private boolean isCppTrailingReturnLambda(final List<Token> tokens, final int prevIdx) {
        int j = prevIdx;
        int steps = 0;
        while (j >= 0 && steps < MAX_RETURN_TYPE_TOKENS) {
            final Token cur = tokens.get(j);
            if (isOp(cur, "->")) {
                final int beforeArrow = prevSignificantIndex(tokens, j - 1);
                if (beforeArrow < 0) {
                    return false;
                }
                final Token b = tokens.get(beforeArrow);
                if (isPunct(b, "]")) {
                    return true;
                }
                return isPunct(b, ")") && precededByCaptureList(tokens, beforeArrow);
            }
            if (!isTypeIshToken(cur)) {
                return false;
            }
            j = prevSignificantIndex(tokens, j - 1);
            steps++;
        }
        return false;
    }

    /** Tokens that can plausibly appear inside a return-type expression before `->`. */
    private boolean isTypeIshToken(final Token t) {
        switch (t.type) {
            case IDENTIFIER:
            case KEYWORD:
            case ANGLE_BRACKET_OPEN:
            case ANGLE_BRACKET_CLOSE:
                return true;
            case OP:
                return "::".equals(t.text) || "*".equals(t.text) || "&".equals(t.text);
            case PUNCT:
                return ",".equals(t.text);
            default:
                return false;
        }
    }

    /** Index of the nearest significant token at or before `from`, or -1 if none. */
    private int prevSignificantIndex(final List<Token> tokens, final int from) {
        int i = from;
        while (i >= 0) {
            final TokenType ty = tokens.get(i).type;
            if (ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE
                    || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) {
                i--;
            } else {
                break;
            }
        }
        return i;
    }

    /** Index of the `(` matching the `)` at closeParenIdx, via local backward depth counting, or -1. */
    private int matchOpenBackward(final List<Token> tokens, final int closeParenIdx) {
        int depth = 1;
        int i = closeParenIdx - 1;
        while (i >= 0 && depth > 0) {
            final Token tk = tokens.get(i);
            if (isPunct(tk, ")") || isPunct(tk, "]")) {
                depth++;
            } else if (isPunct(tk, "(") || isPunct(tk, "[")) {
                depth--;
            }
            i--;
        }
        return depth == 0 ? i + 1 : -1;
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

    // ── `else` / `else if` placement (STYLE.md §12) ─────────────────────────────
    /**
     * Scans a token slice and ensures every `else`/`else if` that directly follows a block's
     * closing `}` starts on its own line, inserting a newline (plus the `}`'s own indentation,
     * so `else` lines up under it) when the two currently share a line. A gap that already
     * contains a newline -- including a deliberate blank line -- is left exactly as-is: STYLE.md
     * §12 treats that blank line as an optional, context-driven separator (e.g. the preceding
     * branch exits unconditionally) that this method must never add or remove on its own. A
     * comment sitting in the gap blocks the rewrite, since relocating it unambiguously is out of
     * scope. An `else` not directly preceded by a `}` -- e.g. the previous branch was itself a
     * brace-less single-statement body per §10 -- is left untouched; §12 only specifies
     * placement relative to a preceding block's closing brace.
     */
    public String placeElseOnOwnLine(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        final int n = tokens.size();
        int lastSigIdx = -1;
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                    || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                gap.add(t);
                i++;
                continue;
            }

            if (t.type == TokenType.KEYWORD && "else".equals(t.text) && lastSigIdx >= 0
                    && isPunct(tokens.get(lastSigIdx), "}")
                    && gap.stream().noneMatch(g -> g.type == TokenType.NEWLINE)
                    && !t.frozen && !tokens.get(lastSigIdx).frozen) {
                final String indent = indentBefore(tokens, lastSigIdx);
                out.append('\n').append(indent);
                for (final Token g : gap) {
                    if (isComment(g)) {
                        out.append(g.text);
                    }
                }
                if (gap.stream().anyMatch(Token::isComment)) {
                    out.append('\n').append(indent);
                }
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            gap.clear();
            out.append(t.text);
            lastSigIdx = i;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    /** The leading whitespace of the line containing the token at idx, or "" if it isn't first on its line. */
    private String indentBefore(final List<Token> tokens, final int idx) {
        final StringBuilder indent = new StringBuilder();
        int i = idx - 1;
        while (i >= 0 && tokens.get(i).type == TokenType.WHITESPACE) {
            indent.insert(0, tokens.get(i).text);
            i--;
        }
        return (i < 0 || tokens.get(i).type == TokenType.NEWLINE) ? indent.toString() : "";
    }

    /**
     * Identical in structure to {@link #placeElseOnOwnLine}: when `catch` or `finally` is
     * found directly after a `}` on the same line (no newline in the gap, no comment in the
     * gap), it is moved onto the next line at the `}`'s own indentation level. This mirrors
     * how STYLE.md treats these keywords: their body `{` is K&R-placed (already handled by
     * {@link #enforceKAndRBraceStyle}), but the keyword itself follows the preceding closing
     * brace on a new line, not appended to it.
     */
    public String placeCatchFinallyOnOwnLine(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        final int n = tokens.size();
        int lastSigIdx = -1;
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                    || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                gap.add(t);
                i++;
                continue;
            }

            if (t.type == TokenType.KEYWORD
                    && ("catch".equals(t.text) || "finally".equals(t.text))
                    && lastSigIdx >= 0
                    && isPunct(tokens.get(lastSigIdx), "}")
                    && gap.stream().noneMatch(g -> g.type == TokenType.NEWLINE)
                    && !t.frozen && !tokens.get(lastSigIdx).frozen) {
                final String indent = indentBefore(tokens, lastSigIdx);
                out.append('\n').append(indent);
                for (final Token g : gap) {
                    if (isComment(g)) {
                        out.append(g.text);
                    }
                }
                if (gap.stream().anyMatch(Token::isComment)) {
                    out.append('\n').append(indent);
                }
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            gap.clear();
            out.append(t.text);
            lastSigIdx = i;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    // ── Named-construct blank lines (STYLE.md §7) ───────────────────────────────
    /**
     * Scans a token slice and ensures exactly one blank line immediately follows the `{` and
     * immediately precedes the `}` of every named construct (`class`/`struct`/`enum`/
     * `enum class`/`namespace`/`interface`/`extern "C"` -- anything the tokenizer tagged via
     * `Token.name`), regardless of how short the body is, per STYLE.md §7. A gap that already
     * has one or more blank lines is left untouched; a gap with exactly one newline gets a
     * second one inserted right after it; a gap with no newline at all (a same-line `{}` body)
     * gets `"\n\n"` prepended. Control-flow blocks (`for`/`while`/`if`/`switch`/etc., where
     * `Token.name` is null) are never touched here -- STYLE.md §7 says their existing blank
     * lines must be preserved exactly as written, which this method already does simply by
     * not inspecting them. See {@link #ensureBlankLine} for how a comment in the gap is handled.
     */
    public String insertNamedConstructBlankLines(final List<Token> tokens) {
        final int n = tokens.size();

        // Match every '{' to its '}' via simple depth counting, so a named brace's true
        // boundary can be found regardless of iteration order.
        final Map<Integer, Integer> matchClose = new HashMap<>();
        final Deque<Integer> braceStack = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{")) {
                braceStack.push(i);
            } else if (isPunct(t, "}") && !braceStack.isEmpty()) {
                matchClose.put(braceStack.pop(), i);
            }
        }

        // For each named, non-empty-body '{', the guaranteed blank line normally belongs right
        // after '{' / right before '}' -- but if a preprocessor directive (e.g. an `#ifdef
        // __cplusplus` / `#endif` pair wrapping just the brace, as with a guarded `extern "C"`)
        // sits directly against the brace with no blank line of its own, the boundary moves past
        // it: the blank line separates the guard from the real body content, not the brace from
        // the guard immediately touching it.
        final Set<Integer> blankBeforeIdx = new HashSet<>();
        final Set<Integer> blankAfterIdx = new HashSet<>();
        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (!isPunct(t, "{") || t.name == null || isEmptyBraceBody(tokens, i)) {
                continue;
            }
            final Integer closeIdx = matchClose.get(i);
            if (closeIdx == null || anyFrozen(tokens, i, closeIdx + 1)) {
                continue;
            }
            blankBeforeIdx.add(skipGuardForward(tokens, i));
            blankAfterIdx.add(skipGuardBackward(tokens, closeIdx));
        }

        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        int lastSignificant = -1;
        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (isGap(t)) {
                gap.add(t);
                continue;
            }
            final boolean needBlank = blankBeforeIdx.contains(i)
                    || (lastSignificant >= 0 && blankAfterIdx.contains(lastSignificant));
            if (needBlank) {
                out.append(ensureBlankLine(gap));
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            gap.clear();
            out.append(t.text);
            lastSignificant = i;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    /** True iff nothing but whitespace and exactly one NEWLINE sits between tokens at indices
     *  {@code fromExclusive} and {@code toExclusive} -- i.e. the two lines are adjacent with no
     *  blank line between them. */
    private boolean isSingleNewlineGap(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        int newlines = 0;
        for (int k = fromExclusive + 1; k < toExclusive; k++) {
            if (tokens.get(k).type == TokenType.NEWLINE) {
                newlines++;
            }
        }
        return newlines == 1;
    }

    /** From a named `{` at {@code openIdx}, walks forward past any run of preprocessor directive
     *  lines sitting directly against the brace (no blank line of their own), returning the index
     *  of the first token that represents real body content. */
    private int skipGuardForward(final List<Token> tokens, final int openIdx) {
        int i = openIdx;
        while (true) {
            int j = i + 1;
            while (j < tokens.size() && isGap(tokens.get(j))) {
                j++;
            }
            if (j >= tokens.size()) {
                return j;
            }
            if (tokens.get(j).type == TokenType.PREPROCESSOR && isSingleNewlineGap(tokens, i, j)) {
                i = j;
                continue;
            }
            return j;
        }
    }

    /** From a named `}` at {@code closeIdx}, walks backward past any run of preprocessor
     *  directive lines sitting directly against the brace (no blank line of their own), returning
     *  the index of the last token that represents real body content. */
    private int skipGuardBackward(final List<Token> tokens, final int closeIdx) {
        int i = closeIdx;
        while (true) {
            int j = i - 1;
            while (j >= 0 && isGap(tokens.get(j))) {
                j--;
            }
            if (j < 0) {
                return j;
            }
            if (tokens.get(j).type == TokenType.PREPROCESSOR && isSingleNewlineGap(tokens, j, i)) {
                i = j;
                continue;
            }
            return j;
        }
    }

    /** True if the `{` at {@code openIdx} is immediately followed (ignoring gap tokens) by its
     *  own matching `}`, i.e. an empty body -- {@code { }} or {@code {}}. */
    private boolean isEmptyBraceBody(final List<Token> tokens, final int openIdx) {
        for (int k = openIdx + 1; k < tokens.size(); k++) {
            final Token t = tokens.get(k);
            if (isGap(t)) {
                continue;
            }
            return isPunct(t, "}");
        }
        return false;
    }

    /**
     * Guarantees the gap contains a blank line. A comment already sitting on its own line (at
     * least one NEWLINE precedes it in the gap) does not block this -- the blank line is inserted
     * ahead of it, same as it would be ahead of the first real token, and everything from the
     * comment onward is left untouched. A comment with nothing but whitespace before it (a
     * trailing same-line comment glued to the previous token, e.g. `stuff; // note`) is left
     * attached to that line -- relocating *it* ahead of a synthesized blank line would be
     * ambiguous, consistent with `enforceKAndRBraceStyle`/`placeElseOnOwnLine` above -- but the
     * blank line is still guaranteed in the remainder of the gap, right after the comment's own
     * line, rather than being dropped entirely.
     */
    private String ensureBlankLine(final List<Token> gap) {
        int firstCommentIdx = -1;
        boolean newlineBeforeFirstComment = false;
        for (int i = 0; i < gap.size(); i++) {
            final Token g = gap.get(i);
            if (isComment(g)) {
                firstCommentIdx = i;
                break;
            }
            if (g.type == TokenType.NEWLINE) {
                newlineBeforeFirstComment = true;
            }
        }
        if (firstCommentIdx >= 0 && !newlineBeforeFirstComment) {
            // Comment sits on the same physical line as whatever precedes the gap (only
            // whitespace, no NEWLINE, in between) -- a trailing same-line comment glued to the
            // previous token, not a leading comment of the next member. Keep it glued to that
            // line, but still guarantee a blank line in what remains of the gap after it.
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i <= firstCommentIdx; i++) {
                sb.append(gap.get(i).text);
            }
            sb.append(ensureBlankLine(gap.subList(firstCommentIdx + 1, gap.size())));
            return sb.toString();
        }

        final int prefixEnd = firstCommentIdx < 0 ? gap.size() : firstCommentIdx;
        int newlineCount = 0;
        for (int i = 0; i < prefixEnd; i++) {
            if (gap.get(i).type == TokenType.NEWLINE) {
                newlineCount++;
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (newlineCount == 0) {
            sb.append("\n\n");
        }
        boolean insertedExtra = newlineCount != 1;
        for (int i = 0; i < prefixEnd; i++) {
            final Token g = gap.get(i);
            sb.append(g.text);
            if (!insertedExtra && g.type == TokenType.NEWLINE) {
                sb.append('\n');
                insertedExtra = true;
            }
        }
        for (int i = prefixEnd; i < gap.size(); i++) {
            sb.append(gap.get(i).text);
        }
        return sb.toString();
    }

    private boolean isGap(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    // ── Closing comments on blocks (STYLE.md §7) ────────────────────────────────
    /** What kind of construct a `{` opens, for closing-comment purposes. */
    private enum Kind { NAMED, FOR, WHILE, IF, SWITCH, EXCLUDED, OTHER }

    /** A currently-open brace's classification, tracked on a stack while scanning forward. */
    private static final class Frame {
        final int openIdx;
        final Kind kind;
        final String label;       // fixed text, excluding any nested-disambiguation variable
        final int openParen;      // -1 unless kind is FOR/WHILE/SWITCH
        final int closeParen;     // -1 unless kind is FOR/WHILE/SWITCH
        boolean sameKindNested;   // set true if an ancestor (or descendant) of the same kind exists

        private Frame(final int openIdx, final Kind kind, final String label,
                final int openParen, final int closeParen) {
            this.openIdx = openIdx;
            this.kind = kind;
            this.label = label;
            this.openParen = openParen;
            this.closeParen = closeParen;
        }

        static Frame named(final int openIdx, final String label) {
            return new Frame(openIdx, Kind.NAMED, label, -1, -1);
        }

        static Frame control(final int openIdx, final Kind kind, final String label,
                final int openParen, final int closeParen) {
            return new Frame(openIdx, kind, label, openParen, closeParen);
        }

        static Frame excluded(final int openIdx) {
            return new Frame(openIdx, Kind.EXCLUDED, null, -1, -1);
        }

        static Frame other(final int openIdx) {
            return new Frame(openIdx, Kind.OTHER, null, -1, -1);
        }
    }

    /**
     * Scans a token slice and appends a `// label` comment after the `}` of every block that
     * qualifies per STYLE.md §7: named constructs always (regardless of length), and
     * `for`/`while`/`if`/`switch` bodies only when their content exceeds
     * {@code closingCommentMinLines}. When two control-flow blocks of the same kind are nested
     * simultaneously, both get a disambiguating variable appended (`for i`, `while running`,
     * `switch opcode`) when one can be extracted -- `if` never gets a variable (no STYLE.md
     * example shows one, and `if` conditions are typically compound). `case` labels, naked
     * `{ ... }` blocks, `else`/`else if`, and unnamed namespaces never get a comment, since none
     * of those braces are ever classified as NAMED/FOR/WHILE/IF/SWITCH by {@link #classifyBrace}.
     * A trailing `;` right after `}` (C/C++ `struct`/`class`/`enum`/`union` definitions) is
     * skipped over so the comment lands after it, not before -- landing before it would put the
     * `;` on a comment line and silently break the statement. If anything other than whitespace
     * precedes the next newline at the chosen insertion point (more code on the same line, or an
     * existing trailing comment), the comment is skipped entirely rather than risking corruption
     * or duplication.
     */
    public String addClosingComments(final List<Token> tokens) {
        final int n = tokens.size();
        final Deque<Frame> stack = new ArrayDeque<>();
        final Map<Integer, String> comments = new HashMap<>();
        final Map<Integer, String> replaceTokens = new HashMap<>(); // idx → replacement text

        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{")) {
                final Frame f = classifyBrace(tokens, i);
                if (f.kind == Kind.FOR || f.kind == Kind.WHILE || f.kind == Kind.SWITCH) {
                    boolean foundAncestor = false;
                    for (final Frame anc : stack) {
                        if (anc.kind == f.kind) {
                            anc.sameKindNested = true;
                            foundAncestor = true;
                        }
                    }
                    if (foundAncestor) {
                        f.sameKindNested = true;
                    }
                }
                stack.push(f);
            } else if (isPunct(t, "}")) {
                if (stack.isEmpty()) {
                    continue;
                }
                final Frame f = stack.pop();
                if (anyFrozen(tokens, f.openIdx, i + 1)) {
                    continue;
                }
                final String comment = decideComment(tokens, f, i);
                final int insertAt = commentInsertionIndex(tokens, i);
                final int existingCommentIdx = findExistingLineComment(tokens, insertAt, n);
                if (comment != null) {
                    if (safeToCommentAfter(tokens, insertAt)) {
                        comments.put(insertAt, comment);
                    } else if (existingCommentIdx >= 0) {
                        replaceTokens.put(existingCommentIdx, "// " + comment);
                        normalizeWhitespaceBefore(tokens, insertAt + 1, existingCommentIdx, replaceTokens);
                    }
                } else if (existingCommentIdx >= 0
                        && isLikelyClosingComment(tokens.get(existingCommentIdx).text)) {
                    replaceTokens.put(existingCommentIdx, "");
                    clearWhitespaceBefore(tokens, insertAt + 1, existingCommentIdx, replaceTokens);
                }
            }
        }

        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < n; i++) {
            final String replacement = replaceTokens.get(i);
            if (replacement != null) {
                out.append(replacement);
            } else {
                out.append(tokens.get(i).text);
            }
            final String c = comments.get(i);
            if (c != null) {
                out.append(" // ").append(c);
            }
        }
        return out.toString();
    }

    /** Finds the index of a COMMENT_LINE token directly after {@code afterIdx} (skipping only
     *  WHITESPACE), or -1 if the first non-whitespace token is not a COMMENT_LINE. */
    private int findExistingLineComment(final List<Token> tokens, final int afterIdx, final int n) {
        int k = afterIdx + 1;
        while (k < n && tokens.get(k).type == TokenType.WHITESPACE) {
            k++;
        }
        return (k < n && tokens.get(k).type == TokenType.COMMENT_LINE) ? k : -1;
    }

    /** True if a comment's text looks like a stale/wrong closing-comment artifact left over
     *  from a previous format pass: starts with {@code "// end "} (the closing-comment
     *  convention used when a block that used to warrant one no longer does) followed by only
     *  word characters (letters, digits, underscore) and spaces, with no punctuation or
     *  symbols. An ordinary short comment that happens to be a single alphanumeric word or
     *  phrase (e.g. `// getter`, `// validator`) does not start with {@code "end "} and so is
     *  never mistaken for a stale closing-comment artifact. */
    private boolean isLikelyClosingComment(final String text) {
        if (!text.startsWith("// end ")) {
            return false;
        }
        final String body = text.substring(7);
        if (body.isEmpty()) {
            return false;
        }
        for (int i = 0; i < body.length(); i++) {
            final char c = body.charAt(i);
            if (c != ' ' && !Character.isLetterOrDigit(c) && c != '_') {
                return false;
            }
        }
        return true;
    }

    /** Collapses all WHITESPACE tokens in [{@code from}, {@code before}) to a single space. */
    private void normalizeWhitespaceBefore(final List<Token> tokens, final int from,
            final int before, final Map<Integer, String> replaceTokens) {
        boolean first = true;
        for (int k = from; k < before; k++) {
            if (tokens.get(k).type == TokenType.WHITESPACE) {
                replaceTokens.put(k, first ? " " : "");
                first = false;
            }
        }
    }

    /** Removes all WHITESPACE tokens in [{@code from}, {@code before}). */
    private void clearWhitespaceBefore(final List<Token> tokens, final int from,
            final int before, final Map<Integer, String> replaceTokens) {
        for (int k = from; k < before; k++) {
            if (tokens.get(k).type == TokenType.WHITESPACE) {
                replaceTokens.put(k, "");
            }
        }
    }

    /** Classifies the `{` at braceIdx for closing-comment purposes; see {@link #addClosingComments}. */
    private Frame classifyBrace(final List<Token> tokens, final int braceIdx) {
        final Token brace = tokens.get(braceIdx);
        if (brace.name != null) {
            return classifyNamed(tokens, braceIdx, brace.name);
        }

        final int prevIdx = prevSignificantIndex(tokens, braceIdx - 1);
        if (prevIdx < 0) {
            return Frame.other(braceIdx);
        }
        if (isAnonymousClassBrace(tokens, prevIdx)) {
            return Frame.named(braceIdx, "class");
        }

        final Token prev = tokens.get(prevIdx);
        if (isPunct(prev, ")")) {
            final int openParen = matchOpenBackward(tokens, prevIdx);
            final int kwIdx = openParen >= 0 ? prevSignificantIndex(tokens, openParen - 1) : -1;
            if (kwIdx >= 0 && tokens.get(kwIdx).type == TokenType.KEYWORD) {
                final String kw = tokens.get(kwIdx).text;
                if ("if".equals(kw)) {
                    final int beforeIf = prevSignificantIndex(tokens, kwIdx - 1);
                    if (beforeIf >= 0 && tokens.get(beforeIf).type == TokenType.KEYWORD
                            && "else".equals(tokens.get(beforeIf).text)) {
                        return Frame.excluded(braceIdx);
                    }
                    return Frame.control(braceIdx, Kind.IF, "if", -1, -1);
                }
                if ("for".equals(kw)) {
                    return Frame.control(braceIdx, Kind.FOR, "for", openParen, prevIdx);
                }
                if ("while".equals(kw)) {
                    return Frame.control(braceIdx, Kind.WHILE, "while", openParen, prevIdx);
                }
                if ("switch".equals(kw)) {
                    return Frame.control(braceIdx, Kind.SWITCH, "switch", openParen, prevIdx);
                }
            }
        } else if (prev.type == TokenType.KEYWORD && "else".equals(prev.text)) {
            return Frame.excluded(braceIdx);
        }
        return Frame.other(braceIdx);
    }

    /** Builds the "kind name" label (`class Foo`, `enum class State`, `extern "C"`, ...). */
    private Frame classifyNamed(final List<Token> tokens, final int braceIdx, final String name) {
        if (name.indexOf(' ') >= 0) {
            return Frame.named(braceIdx, name); // already a complete label, e.g. `extern "C"`
        }

        if (isConceptRequiresExpressionBody(tokens, braceIdx, name)) {
            // `concept Name = requires(...) {` -- the requires-expression's own parameter list
            // sits between the name and the body brace, same gap problem as `record`'s component
            // list below. Checked first: `findRecordComponentListClose` below matches on the
            // immediate `)` predecessor unconditionally (java-only in practice, since only `record`
            // ever arms `pendingRecordName`, but it would otherwise misclassify this same shape).
            return Frame.named(braceIdx, "concept " + name);
        }

        if (lang.isJava) {
            final int recordCloseParen = findRecordComponentListClose(tokens, braceIdx);
            if (recordCloseParen >= 0) {
                // `record Name(...) [implements TypeList] {` -- the component list (and an optional
                // implements clause) sits between the name and the body brace, so the name isn't the
                // token directly before `{` like it is for class/interface/enum.
                final int openParen = matchOpenBackward(tokens, recordCloseParen);
                final int nameIdx = openParen >= 0 ? prevSignificantIndex(tokens, openParen - 1) : -1;
                final int recordKwIdx = nameIdx >= 0 ? prevSignificantIndex(tokens, nameIdx - 1) : -1;
                if (recordKwIdx >= 0 && tokens.get(recordKwIdx).type == TokenType.KEYWORD
                        && "record".equals(tokens.get(recordKwIdx).text)) {
                    return Frame.named(braceIdx, "record " + name);
                }
                return Frame.named(braceIdx, name);
            }
        }
        // Qualified namespace name (`namespace alpha::beta::gamma {`): findConstructNameIndex
        // matches single-token identifiers only, so look up the first segment instead and render
        // the closing-comment label with the STYLE.md-preferred space separator.
        if (name.indexOf(':') >= 0) {
            final String firstSegment = name.substring(0, name.indexOf(':'));
            final int qualNameIdx = findConstructNameIndex(tokens, braceIdx, firstSegment);
            if (qualNameIdx >= 0) {
                final int qualKwIdx = findConstructKeywordIndex(tokens, qualNameIdx - 1);
                if (qualKwIdx >= 0 && tokens.get(qualKwIdx).type == TokenType.KEYWORD
                        && "namespace".equals(tokens.get(qualKwIdx).text)) {
                    return Frame.named(braceIdx, "namespace " + name.replace("::", " "));
                }
            }
        }
        // Search backward past inheritance/base-type clauses (and attribute-specifiers like
        // `alignas(16)`) to find the construct keyword.
        final int nameIdx = findConstructNameIndex(tokens, braceIdx, name);
        String label = name;
        if (nameIdx >= 0) {
            final int kwIdx = findConstructKeywordIndex(tokens, nameIdx - 1);
            if (kwIdx >= 0 && tokens.get(kwIdx).type == TokenType.KEYWORD) {
                final String kw = tokens.get(kwIdx).text;
                final int beforeKw = prevSignificantIndex(tokens, kwIdx - 1);
                if ("class".equals(kw) && beforeKw >= 0 && tokens.get(beforeKw).type == TokenType.KEYWORD
                        && "enum".equals(tokens.get(beforeKw).text)) {
                    label = "enum class " + name;
                } else {
                    label = kw + " " + name;
                }
            }
        }
        return Frame.named(braceIdx, label);
    }

    /**
     * Finds the `)` closing a record's component list, scanning backward from {@code braceIdx}
     * across an optional trailing `implements TypeList` clause (the only thing Java permits
     * between a record's component list and its body brace). Returns -1 if the immediate
     * predecessor chain doesn't match this shape (not a record, or an unrecognized shape this
     * bounded-effort scan doesn't cover -- e.g. anything other than `implements` at the top
     * level). Angle-bracket depth is tracked so generic bounds inside the implements clause
     * (`implements Comparable<? super Point>`) don't false-positive on `super`/`extends` as an
     * unexpected keyword.
     */
    private int findRecordComponentListClose(final List<Token> tokens, final int braceIdx) {
        int angleDepth = 0;
        int i = prevSignificantIndex(tokens, braceIdx - 1);
        while (i >= 0) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                angleDepth++;
            } else if (t.type == TokenType.ANGLE_BRACKET_OPEN) {
                angleDepth--;
            } else if (angleDepth == 0) {
                if (isPunct(t, ")")) {
                    return i;
                }
                if (t.type == TokenType.KEYWORD && !"implements".equals(t.text)) {
                    return -1;
                }
            }
            i = prevSignificantIndex(tokens, i - 1);
        }
        return -1;
    }

    /**
     * True iff {@code braceIdx}'s `{` is the body of a `concept Name = requires(...) { ... }`
     * definition -- i.e. the exact predecessor chain `) requires ( ... ) <- = <- name <-
     * concept`, scanning backward via {@link #matchOpenBackward}'s same local depth counting used
     * by the record-component-list check above. {@code name} is the already-detected construct
     * name (from the `{` token's own {@code name} field), confirmed here to match the identifier
     * immediately before the `=` -- guards against a coincidentally-shaped unrelated expression.
     */
    private boolean isConceptRequiresExpressionBody(final List<Token> tokens, final int braceIdx, final String name) {
        final int closeParenIdx = prevSignificantIndex(tokens, braceIdx - 1);
        if (closeParenIdx < 0 || !isPunct(tokens.get(closeParenIdx), ")")) {
            return false;
        }
        final int openParenIdx = matchOpenBackward(tokens, closeParenIdx);
        if (openParenIdx < 0) {
            return false;
        }
        final int requiresIdx = prevSignificantIndex(tokens, openParenIdx - 1);
        if (requiresIdx < 0 || tokens.get(requiresIdx).type != TokenType.KEYWORD
                || !"requires".equals(tokens.get(requiresIdx).text)) {
            return false;
        }
        final int eqIdx = prevSignificantIndex(tokens, requiresIdx - 1);
        if (eqIdx < 0 || !isOp(tokens.get(eqIdx), "=")) {
            return false;
        }
        final int nameIdx = prevSignificantIndex(tokens, eqIdx - 1);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER
                || !name.equals(tokens.get(nameIdx).text)) {
            return false;
        }
        final int conceptKwIdx = prevSignificantIndex(tokens, nameIdx - 1);
        return conceptKwIdx >= 0 && tokens.get(conceptKwIdx).type == TokenType.KEYWORD
                && "concept".equals(tokens.get(conceptKwIdx).text);
    }

    /**
     * True if the `{` whose immediately preceding significant token is at prevIdx opens a Java
     * anonymous class body: `new Identifier(args) {` or `new Identifier<T>(args) {`. Qualified
     * names (`new pkg.Identifier() {`) are not recognized -- out of scope, same bounded-effort
     * spirit as `isCppTrailingReturnLambda`'s scan cap above.
     */
    private boolean isAnonymousClassBrace(final List<Token> tokens, final int prevIdx) {
        if (!lang.isJava || !isPunct(tokens.get(prevIdx), ")")) {
            return false;
        }
        final int openParen = matchOpenBackward(tokens, prevIdx);
        if (openParen < 0) {
            return false;
        }
        int beforeOpen = prevSignificantIndex(tokens, openParen - 1);
        if (beforeOpen >= 0 && tokens.get(beforeOpen).type == TokenType.ANGLE_BRACKET_CLOSE) {
            beforeOpen = matchAngleOpenBackward(tokens, beforeOpen);
            beforeOpen = beforeOpen >= 0 ? prevSignificantIndex(tokens, beforeOpen - 1) : -1;
        }
        if (beforeOpen < 0 || tokens.get(beforeOpen).type != TokenType.IDENTIFIER) {
            return false;
        }
        final int newIdx = prevSignificantIndex(tokens, beforeOpen - 1);
        return newIdx >= 0 && tokens.get(newIdx).type == TokenType.KEYWORD
                && "new".equals(tokens.get(newIdx).text);
    }

    /** Index of the `<` matching the `>` at closeIdx, via local backward depth counting, or -1. */
    private int matchAngleOpenBackward(final List<Token> tokens, final int closeIdx) {
        int depth = 1;
        int i = closeIdx - 1;
        while (i >= 0 && depth > 0) {
            final TokenType ty = tokens.get(i).type;
            if (ty == TokenType.ANGLE_BRACKET_CLOSE) {
                depth++;
            } else if (ty == TokenType.ANGLE_BRACKET_OPEN) {
                depth--;
            }
            i--;
        }
        return depth == 0 ? i + 1 : -1;
    }

    /** Decides the closing-comment text for frame f closing at closeIdx, or null for no comment. */
    private String decideComment(final List<Token> tokens, final Frame f, final int closeIdx) {
        switch (f.kind) {
            case NAMED:
                return isEmptyBraceBody(tokens, f.openIdx) ? null : f.label;
            case FOR:
            case WHILE:
            case SWITCH:
                if (countContentLines(tokens, f.openIdx, closeIdx) <= closingCommentMinLines) {
                    return null;
                }
                final String var = f.sameKindNested ? extractVariable(tokens, f) : null;
                return var != null ? f.label + " " + var : f.label;
            case IF:
                return countContentLines(tokens, f.openIdx, closeIdx) > closingCommentMinLines
                        ? f.label : null;
            default:
                return null;
        }
    }

    private int countContentLines(final List<Token> tokens, final int openIdx, final int closeIdx) {
        int count = 0;
        for (int k = openIdx + 1; k < closeIdx; k++) {
            if (tokens.get(k).type == TokenType.NEWLINE) {
                count++;
            }
        }
        return count;
    }

    /** Index to insert the comment at: the `}`, or a trailing `;` right after it, if present --
     *  also skipping past a single typedef-alias identifier between them (C's
     *  `typedef enum/struct NAME { ... } ALIAS;`), so the alias/`;` themselves aren't split from
     *  the body they close. */
    private int commentInsertionIndex(final List<Token> tokens, final int closeIdx) {
        int k = closeIdx + 1;
        final int n = tokens.size();
        while (k < n && tokens.get(k).type == TokenType.WHITESPACE) {
            k++;
        }
        if (k < n && tokens.get(k).type == TokenType.IDENTIFIER) {
            int j = k + 1;
            while (j < n && tokens.get(j).type == TokenType.WHITESPACE) {
                j++;
            }
            if (j < n && isPunct(tokens.get(j), ";")) {
                return j;
            }
        }
        return k < n && isPunct(tokens.get(k), ";") ? k : closeIdx;
    }

    /** True if nothing but whitespace separates idx from the next newline (or end of input). */
    private boolean safeToCommentAfter(final List<Token> tokens, final int idx) {
        int k = idx + 1;
        final int n = tokens.size();
        while (k < n && tokens.get(k).type == TokenType.WHITESPACE) {
            k++;
        }
        return k >= n || tokens.get(k).type == TokenType.NEWLINE;
    }

    private String extractVariable(final List<Token> tokens, final Frame f) {
        if (f.kind == Kind.FOR) {
            return extractForVariable(tokens, f.openParen, f.closeParen);
        }
        return extractSingleIdentifier(tokens.subList(f.openParen + 1, f.closeParen));
    }

    /**
     * The loop variable's name: the first identifier in the init clause (declared or not), or
     * if the init clause is empty, the first identifier in the increment clause, or for a
     * range-based/for-each `for(... name : ...)`, the identifier immediately before the `:`.
     * Null if none of those shapes match (e.g. a variable-less `for(;;)`).
     */
    private String extractForVariable(final List<Token> tokens, final int openParen,
            final int closeParen) {
        final List<Token> body = tokens.subList(openParen + 1, closeParen);
        int depth = 0;
        int colonIdx = -1;
        final List<Integer> semiIdx = new ArrayList<>();
        for (int k = 0; k < body.size(); k++) {
            final Token t = body.get(k);
            if (isPunct(t, "(") || isPunct(t, "[")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]")) {
                depth--;
            } else if (depth == 0 && isPunct(t, ";")) {
                semiIdx.add(k);
            } else if (depth == 0 && colonIdx < 0 && isOp(t, ":")) {
                colonIdx = k;
            }
        }

        if (semiIdx.isEmpty() && colonIdx >= 0) {
            int k = colonIdx - 1;
            while (k >= 0 && isGap(body.get(k))) {
                k--;
            }
            return k >= 0 && body.get(k).type == TokenType.IDENTIFIER ? body.get(k).text : null;
        }
        if (!semiIdx.isEmpty()) {
            final String initName = firstIdentifier(body.subList(0, semiIdx.get(0)));
            if (initName != null) {
                return initName;
            }
            if (semiIdx.size() >= 2) {
                return firstIdentifier(body.subList(semiIdx.get(1) + 1, body.size()));
            }
        }
        return null;
    }

    private String firstIdentifier(final List<Token> seg) {
        for (final Token t : seg) {
            if (t.type == TokenType.IDENTIFIER) {
                return t.text;
            }
        }
        return null;
    }

    /**
     * The `while`/`switch` controlling expression's variable, only when it reduces to one bare
     * identifier (optionally negated, e.g. `!done`) -- anything more compound has no single
     * representative variable, so this returns null and the caller falls back to a bare label.
     */
    // ── Named-construct header spacing (STYLE.md §11) ───────────────────────────
    /**
     * Scans a token slice and collapses every run of spaces/tabs within a named-construct
     * header (the range from the opening keyword to the `{`, e.g. `class   Foo   :   public   Bar`)
     * to a single space.  Only {@code WHITESPACE} tokens (spaces/tabs) are collapsed; NEWLINE
     * tokens are left untouched, so a header that was already broken across lines by earlier
     * passes is not corrupted.  The header range is identified by scanning backward from each
     * `{` whose {@code name} field is non-null -- the tokenizer guarantees that field is set
     * exactly for the opening brace of every named construct.
     */
    public String enforceNamedConstructHeaderSpacing(final List<Token> tokens) {
        final int n = tokens.size();
        final boolean[] collapse = new boolean[n];

        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (!isPunct(t, "{") || t.name == null) {
                continue;
            }
            int headerStart = -1;
            for (int j = i - 1; j >= 0; j--) {
                final Token prev = tokens.get(j);
                final TokenType ty = prev.type;
                if (ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE
                        || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) {
                    continue;
                }
                if (ty == TokenType.KEYWORD && isNamedConstructStartKeyword(prev.text)) {
                    headerStart = j;
                    if ("class".equals(prev.text)) {
                        final int before = prevSignificantIndex(tokens, j - 1);
                        if (before >= 0 && tokens.get(before).type == TokenType.KEYWORD
                                && "enum".equals(tokens.get(before).text)) {
                            headerStart = before;
                        }
                    }
                    // Extend backward past any preceding modifier keywords (public, abstract, etc.)
                    int ext = prevSignificantIndex(tokens, headerStart - 1);
                    while (ext >= 0 && tokens.get(ext).type == TokenType.KEYWORD
                            && !isNamedConstructStartKeyword(tokens.get(ext).text)) {
                        headerStart = ext;
                        ext = prevSignificantIndex(tokens, ext - 1);
                    }
                    break;
                }
                if (ty == TokenType.PUNCT
                        && (";".equals(prev.text) || "{".equals(prev.text) || "}".equals(prev.text))) {
                    break;
                }
            }
            if (headerStart >= 0 && !anyFrozen(tokens, headerStart, i + 1)) {
                for (int j = headerStart; j < i; j++) {
                    collapse[j] = true;
                }
            }
        }

        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (collapse[i] && t.type == TokenType.WHITESPACE) {
                out.append(' ');
            } else {
                out.append(t.text);
            }
        }
        return out.toString();
    }

    private boolean isNamedConstructStartKeyword(final String text) {
        switch (text) {
            case "class": case "struct": case "enum": case "namespace":
            case "concept": case "interface": case "record":
                return true;
            default:
                return false;
        }
    }

    /**
     * Scans backward from {@code fromIdx} for the nearest KEYWORD that satisfies
     * {@link BlockStructureRule#isNamedConstructStartKeyword}, skipping over non-keyword tokens
     * (e.g. attribute-specifiers like {@code alignas(16)} that can appear between the keyword
     * and the construct name).  Stops at {@code ;}, {@code {}, or {@code }}.
     */
    private int findConstructKeywordIndex(final List<Token> tokens, final int fromIdx) {
        for (int i = fromIdx; i >= 0; i--) {
            final Token t = tokens.get(i);
            final TokenType ty = t.type;
            if (ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE
                    || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) {
                continue;
            }
            if (ty == TokenType.KEYWORD && isNamedConstructStartKeyword(t.text)) {
                return i;
            }
            if (ty == TokenType.PUNCT && (";".equals(t.text) || "{".equals(t.text) || "}".equals(t.text))) {
                break;
            }
        }
        return -1;
    }

    /**
     * Searches backward from {@code braceIdx} for the first {@code IDENTIFIER} token whose
     * text equals {@code name}, stopping at any {@code ;}, {@code {}, or {@code }} boundary.
     * Returns the index of that token, or -1 if not found.  Used by {@link #classifyNamed} to
     * locate the construct name across an inheritance or base-type clause so the keyword
     * immediately before it can be extracted for the closing-comment label.
     */
    private int findConstructNameIndex(final List<Token> tokens, final int braceIdx, final String name) {
        for (int i = braceIdx - 1; i >= 0; i--) {
            final Token t = tokens.get(i);
            final TokenType ty = t.type;
            if (ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE
                    || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) {
                continue;
            }
            if (ty == TokenType.IDENTIFIER && name.equals(t.text)) {
                return i;
            }
            if (ty == TokenType.PUNCT && (";".equals(t.text) || "{".equals(t.text))) {
                break;
            }
        }
        return -1;
    }

    private String extractSingleIdentifier(final List<Token> body) {
        final List<Token> sig = new ArrayList<>();
        for (final Token t : body) {
            if (!isGap(t)) {
                sig.add(t);
            }
        }
        if (sig.size() == 1 && sig.get(0).type == TokenType.IDENTIFIER) {
            return sig.get(0).text;
        }
        if (sig.size() == 2 && isOp(sig.get(0), "!") && sig.get(1).type == TokenType.IDENTIFIER) {
            return sig.get(1).text;
        }
        return null;
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
