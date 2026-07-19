/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCurly;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isComment;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
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

/**
 * Landing spot for JavaScript/TypeScript-only STYLE_JS_TS.md sections not reusable from the
 * shared curly-family rule classes -- mirrors {@link KotlinSpecificRule}'s role for Kotlin.
 * Shared between JS and TS (gate internally on {@code lang.isTs} for TS-only additions such as
 * type annotations/interfaces/enums, per RDD_KEY_187), rather than splitting into separate
 * per-language classes.
 *
 * <p>Checkpoint 3 (this checkpoint): STYLE_JS_TS.md §2 (statement-termination semicolon
 * insertion) only. See STATE_JS_TS.md's checklist for what's implemented so far.
 */
public final class JsTsSpecificRule {

    private final Lang lang;
    private final int lineLengthLimit;

    public JsTsSpecificRule(final Lang lang) {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public JsTsSpecificRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public JsTsSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
    }

    // ── §2 Statement termination (semicolon insertion) ──────────────────────────────
    /** Tokens that mean "the statement isn't finished yet" when they're the last significant
     *  token seen before a would-be statement boundary -- a trailing binary/assignment operator,
     *  a comma, a colon (object-literal key or ternary), an opening bracket, or a keyword that
     *  can never end a statement on its own. */
    private static final Set<String> CONTINUATION_OPS = new HashSet<>(Arrays.asList(
            "=", "+", "-", "*", "/", "%", "&&", "||", "??", "?", ".", "=>", "==", "===", "!=",
            "!==", "<", ">", "<=", ">=", "+=", "-=", "*=", "/=", "%=", "&&=", "||=", "??=", "&",
            "|", "^", "<<", ">>", ">>>", "...", "**", "**=", "&=", "|=", "^=", "<<=", ">>=", ">>>="));

    private static final Set<String> CONTINUATION_KEYWORDS = new HashSet<>(Arrays.asList(
            "typeof", "new", "in", "instanceof", "else", "try", "finally", "do", "case",
            "default", "extends", "implements", "delete", "void", "of", "as", "from"));

    /**
     * STYLE_JS_TS.md §2: always insert an explicit semicolon at the end of a statement, never
     * rely on ASI. Statement boundaries are found via a depth counter that increments across
     * `(`/`[` and any `{` classified as an *expression* brace (arrow-function body, object
     * literal, etc. -- {@link #isExpressionBrace}), but resets to a fresh 0 across a `{`
     * classified as a *statement* brace (function/class/control-flow body) -- so a `return`
     * inside a deeply-nested method body is evaluated at depth 0 relative to its own immediately
     * enclosing block, not the file's absolute brace nesting. A statement boundary is reached
     * either at a depth-0 NEWLINE or right before a depth-0 statement-brace's own closing `}`;
     * whether a semicolon is actually needed there is decided by {@link #needsSemicolonAfter}.
     * Conservative posture matching every other pass in this codebase: any candidate boundary
     * touching a frozen token is left completely untouched.
     */
    public String enforceSemicolonInsertion(final List<Token> tokens) {
        final Map<Integer, Integer> braceOpenToClose = matchBraces(tokens);
        final Map<Integer, Boolean> braceResetDepth = new HashMap<>();
        final Map<Integer, Boolean> braceNeedsSemicolon = new HashMap<>();
        classifyBraces(tokens, braceOpenToClose, braceResetDepth, braceNeedsSemicolon);
        final Map<Integer, Integer> parenOpenToClose = matchParens(tokens);
        final Map<Integer, Integer> parenCloseToOpen = new HashMap<>();
        for (final Map.Entry<Integer, Integer> e : parenOpenToClose.entrySet()) {
            parenCloseToOpen.put(e.getValue(), e.getKey());
        }
        final Map<Integer, Integer> braceCloseToOpen = new HashMap<>();
        for (final Map.Entry<Integer, Integer> e : braceOpenToClose.entrySet()) {
            braceCloseToOpen.put(e.getValue(), e.getKey());
        }

        final Map<Integer, String> overrides = new HashMap<>();
        final Deque<Integer> depthStack = new ArrayDeque<>();
        int depth = 0;
        int lastSigIdx = -1;
        final int n = tokens.size();

        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);

            if (t.type == TokenType.NEWLINE) {
                if (depth == 0 && lastSigIdx >= 0) {
                    maybeInsertSemicolon(tokens, lastSigIdx, i, braceNeedsSemicolon, braceCloseToOpen,
                            parenCloseToOpen, overrides);
                }
                continue;
            }
            if (isGapToken(t)) {
                continue;
            }

            if (isPunct(t, "}") && depth == 0 && Boolean.TRUE.equals(braceResetDepth.get(braceCloseToOpen.get(i)))
                    && lastSigIdx >= 0) {
                // Statement-body brace about to close at depth 0 with no trailing NEWLINE first
                // (a one-liner body, e.g. `{ return x }`) -- the boundary is right here, before
                // `}`. Covers both true statement blocks (function/class/control-flow, never need
                // a semicolon on `}` itself) and arrow-function block bodies (do need one).
                maybeInsertSemicolon(tokens, lastSigIdx, i, braceNeedsSemicolon, braceCloseToOpen,
                        parenCloseToOpen, overrides);
            }

            if (isPunct(t, "(") || isPunct(t, "[")) {
                depthStack.push(depth);
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]")) {
                if (!depthStack.isEmpty()) {
                    depth = depthStack.pop();
                }
            } else if (isPunct(t, "{")) {
                depthStack.push(depth);
                depth = Boolean.TRUE.equals(braceResetDepth.get(i)) ? 0 : depth + 1;
            } else if (isPunct(t, "}")) {
                if (!depthStack.isEmpty()) {
                    depth = depthStack.pop();
                }
            }

            lastSigIdx = i;
        }

        return render(tokens, overrides);
    }

    /** Decides whether a semicolon is needed right after {@code lastSigIdx} (the last real
     *  content token of a candidate statement), and if so records an override appending `;` to
     *  that token's own text. {@code boundaryIdx} is the NEWLINE or closing-`}` token that
     *  triggered the check -- used only to look ahead for a following `{` (control-flow/
     *  function/class header continuation) and to bail out on a frozen span. */
    private void maybeInsertSemicolon(final List<Token> tokens, final int lastSigIdx, final int boundaryIdx,
            final Map<Integer, Boolean> braceNeedsSemicolon, final Map<Integer, Integer> braceCloseToOpen,
            final Map<Integer, Integer> parenCloseToOpen, final Map<Integer, String> overrides) {
        final Token lastSig = tokens.get(lastSigIdx);
        if (lastSig.frozen || tokens.get(boundaryIdx).frozen) {
            return;
        }
        if (overrides.containsKey(lastSigIdx)) {
            return; // already handled (e.g. both a `}` boundary and a following NEWLINE fired)
        }
        // If the next real content across the boundary is `{`, this is a control-flow/function/
        // class header continuing onto its own-line Allman brace -- never a statement end.
        if (tokens.get(boundaryIdx).type == TokenType.NEWLINE) {
            final int nextSig = nextSignificantIndex(tokens, boundaryIdx + 1);
            if (nextSig >= 0 && isPunct(tokens.get(nextSig), "{")) {
                return;
            }
            // STYLE_JS_TS.md §11.1: a union/intersection type may wrap break-before-operator
            // (`type Y = A\n | B\n | C;`), leading `|`/`&` on the next line -- that's still the
            // same statement continuing, not a new one, even though the current line's own last
            // token (`A`) isn't itself a trailing continuation operator.
            if (nextSig >= 0 && (isOp(tokens.get(nextSig), "|") || isOp(tokens.get(nextSig), "&"))) {
                return;
            }
        }
        if (needsSemicolonAfter(tokens, lastSigIdx, braceNeedsSemicolon, braceCloseToOpen, parenCloseToOpen)) {
            overrides.put(lastSigIdx, lastSig.text + ";");
        }
    }

    private boolean needsSemicolonAfter(final List<Token> tokens, final int idx,
            final Map<Integer, Boolean> braceNeedsSemicolon, final Map<Integer, Integer> braceCloseToOpen,
            final Map<Integer, Integer> parenCloseToOpen) {
        final Token t = tokens.get(idx);
        if (isPunct(t, ";") || isPunct(t, "{") || isPunct(t, ",") || isPunct(t, ":")
                || isPunct(t, "(") || isPunct(t, "[")) {
            return false;
        }
        if (t.type == TokenType.OP && CONTINUATION_OPS.contains(t.text)) {
            return false;
        }
        if (t.type == TokenType.KEYWORD && CONTINUATION_KEYWORDS.contains(t.text)) {
            return false;
        }
        if (endsWithDecoratorApplication(tokens, idx, parenCloseToOpen)) {
            return false;
        }
        if (isPunct(t, "}")) {
            final Integer openIdx = braceCloseToOpen.get(idx);
            return openIdx != null && Boolean.TRUE.equals(braceNeedsSemicolon.get(openIdx));
        }
        return true;
    }

    /** True if {@code idx} is the closing `)` of a decorator's own argument list
     *  (`@Name(...)`), or {@code idx} is a bare decorator name with no argument list
     *  (`@Name`) -- either way, this token ends a decorator application, not a statement, and a
     *  trailing semicolon must never be inserted after it. */
    private boolean endsWithDecoratorApplication(final List<Token> tokens, final int idx,
            final Map<Integer, Integer> parenCloseToOpen) {
        final Token t = tokens.get(idx);
        if (isPunct(t, ")")) {
            final Integer openParenIdx = parenCloseToOpen.get(idx);
            if (openParenIdx == null) {
                return false;
            }
            final int nameIdx = prevSignificantIndex(tokens, openParenIdx - 1);
            return isDecoratorName(tokens, nameIdx);
        }
        if (t.type == TokenType.IDENTIFIER) {
            return isDecoratorName(tokens, idx);
        }
        return false;
    }

    /** True if the token at {@code nameIdx} is an identifier immediately (tight, no gap other
     *  than the `@` itself) preceded by `@` -- a decorator's own name. */
    private boolean isDecoratorName(final List<Token> tokens, final int nameIdx) {
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return false;
        }
        final int atIdx = prevSignificantIndex(tokens, nameIdx - 1);
        return atIdx >= 0 && isOp(tokens.get(atIdx), "@");
    }

    /**
     * Classifies every `{` in {@code openToClose}'s key set along two independent axes, based on
     * the nearest significant token immediately preceding the `{`:
     * <ul>
     * <li>{@code resetDepth} (into {@code outResetDepth}) -- true if the brace's own body is a
     * list of *statements* (needs its own depth-0 frame so a `return`/expression statement inside
     * it is evaluated for its own semicolon) rather than a comma-separated list of *values*
     * (object-literal properties, array elements, destructuring targets -- never individually
     * semicolon-terminated). True for every construct that isn't a value-shaped brace (function/
     * method/class/interface/control-flow bodies) *and* for an arrow-function block body (`=>
     * {`) specifically, since unlike every other value-preceded brace, an arrow body's own
     * contents are statements, not comma-separated values.
     * <li>{@code needsSemicolon} (into {@code outNeedsSemicolon}) -- true if this brace is itself
     * a *value* (assigned, returned, passed as an argument, etc.) whose own closing `}` can be
     * the tail of a statement needing a trailing `;` (object literal, array-pattern default,
     * arrow-function body assigned to something) -- false for a bare definition header
     * (function/class/interface/enum/control-flow) whose closing `}` never takes a semicolon.
     * </ul>
     * Defaults to "not a value" when the preceding token doesn't match any of the recognized
     * value-starting shapes -- same conservative "don't guess, assume the common case" posture as
     * the rest of this file.
     */
    private void classifyBraces(final List<Token> tokens, final Map<Integer, Integer> openToClose,
            final Map<Integer, Boolean> outResetDepth, final Map<Integer, Boolean> outNeedsSemicolon) {
        for (final Integer openIdx : openToClose.keySet()) {
            final int prevIdx = prevSignificantIndex(tokens, openIdx - 1);
            final Token prev = prevIdx >= 0 ? tokens.get(prevIdx) : null;
            final boolean isArrowBody = isOp(prev, "=>");
            final boolean isValue = prev != null && (
                    isArrowBody || isOp(prev, "=") || isPunct(prev, "(") || isPunct(prev, "[")
                            || isPunct(prev, ",") || isOp(prev, ":") || isOp(prev, "??") || isOp(prev, "||")
                            || isOp(prev, "&&") || isOp(prev, "?") || isOp(prev, "...")
                            || (prev.type == TokenType.KEYWORD
                                    && ("return".equals(prev.text) || "yield".equals(prev.text)
                                            || "throw".equals(prev.text) || "typeof".equals(prev.text)
                                            // `const`/`let`/`var { ... } = ...` -- an object-destructuring
                                            // pattern on a declaration LHS, a value/pattern brace (never
                                            // individually semicolon-terminated inside), not a statement
                                            // body -- same distinction Checkpoint 5 already made for the
                                            // array-bracket form via MiscRuleCore.needsSpaceBetween.
                                            || "const".equals(prev.text) || "let".equals(prev.text)
                                            || "var".equals(prev.text))));
            outResetDepth.put(openIdx, !isValue || isArrowBody);
            outNeedsSemicolon.put(openIdx, isValue);
        }
    }

    private Map<Integer, Integer> matchBraces(final List<Token> tokens) {
        final Map<Integer, Integer> openToClose = new HashMap<>();
        final Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{")) {
                stack.push(i);
            } else if (isPunct(t, "}") && !stack.isEmpty()) {
                openToClose.put(stack.pop(), i);
            }
        }
        return openToClose;
    }

    private Map<Integer, Integer> matchParens(final List<Token> tokens) {
        final Map<Integer, Integer> openToClose = new HashMap<>();
        final Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(")) {
                stack.push(i);
            } else if (isPunct(t, ")") && !stack.isEmpty()) {
                openToClose.put(stack.pop(), i);
            }
        }
        return openToClose;
    }

    private int nextSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from; i < tokens.size(); i++) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int prevSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from; i >= 0; i--) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private String render(final List<Token> tokens, final Map<Integer, String> overrides) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            final String override = overrides.get(i);
            out.append(override != null ? override : tokens.get(i).text);
        }
        return out.toString();
    }

    // ── §3 Spread / rest (`...`) tight spacing ───────────────────────────────────────
    /**
     * STYLE_JS_TS.md §3: spread/rest `...` never has a space after it -- tight against the
     * identifier/expression it precedes (`...rest`, `...items`, `{...defaults, ...overrides}`).
     * One-sided (unlike §7's `?.`/`??`, which are tight/spaced on both sides): JS/TS's `...` is
     * always a prefix operator, so only the gap immediately following it is ever touched -- the
     * gap immediately before it (after `(`/`,`/`{`/`[`) is ordinary call/literal spacing, already
     * governed by whatever already renders those contexts, and is left alone here. No general
     * expression-level operator-respacing pass already covers this (same reasoning as Kotlin's
     * `enforceNullSafetyOperatorSpacing` javadoc), so this is its own flat single-pass scan.
     * Conservative bailout matching every other pass in this file: a gap containing a comment, a
     * NEWLINE, or a frozen token is left completely untouched.
     */
    public String enforceSpreadRestSpacing(final List<Token> tokens) {
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

            final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean afterSpread = lastSignificant != null && isOp(lastSignificant, "...");

            if (gapBlocked || !afterSpread) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            // afterSpread && !gapBlocked: gap dropped entirely, nothing appended.

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

    // ── §7 Optional chaining / nullish coalescing ────────────────────────────────────
    /**
     * STYLE_JS_TS.md §7: `?.` is tight (no surrounding space), `??` is spaced like a normal binary
     * operator -- direct analog of Kotlin's `?.`/`?:` treatment
     * ({@link KotlinSpecificRule#enforceNullSafetyOperatorSpacing}), reused here structurally with
     * JS/TS's own operator names. No general expression-level operator-respacing pass already
     * covers this, so it's its own flat pass, same conservative bailout (a gap containing a
     * comment, a NEWLINE, or a frozen token is left untouched).
     */
    public String enforceOptionalChainingSpacing(final List<Token> tokens) {
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

            final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean adjacentToTightOp = isOptionalChain(lastSignificant) || isOptionalChain(t);
            final boolean adjacentToNullish = !adjacentToTightOp && (isNullishCoalesce(lastSignificant) || isNullishCoalesce(t));

            if (gapBlocked || (!adjacentToTightOp && !adjacentToNullish)) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            } else if (adjacentToNullish) {
                out.append(' ');
            }
            // adjacentToTightOp && !gapBlocked: gap dropped entirely, nothing appended.

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

    private boolean isOptionalChain(final Token t) {
        return t != null && isOp(t, "?.");
    }

    private boolean isNullishCoalesce(final Token t) {
        return t != null && (isOp(t, "??") || isOp(t, "??="));
    }

    // ── §4 Template literal `${...}` interpolation spacing ───────────────────────────
    /**
     * STYLE_JS_TS.md §4: a template literal's raw text is preserved exactly as written (already
     * satisfied by the tokenizer emitting the whole backtick literal, including every {@code
     * ${...}} interpolation, as one opaque STRING token -- see {@code
     * TokenizerCurly.emitTemplateLiteral}'s own javadoc), but each {@code ${...}} interpolation's
     * *interior expression* gets normal expression spacing (STYLE.md §3.1), same as Kotlin's own
     * {@code ${...}} string-template interpolation is described as an analog for (though Kotlin's
     * own interpolation is, in this codebase, *also* left opaque -- there is no existing precedent
     * to reuse here, this is new territory). Approach: find every top-level {@code ${...}} span in
     * the literal's raw text (a hand-rolled scanner mirroring the tokenizer's own {@code
     * skipTemplateInterpolation} nesting rules -- brace depth, nested quoted strings, and a nested
     * backtick template treated as one opaque quoted span, not reformatted itself this pass), then
     * for each span: re-tokenize just the interior substring in isolation via a fresh {@code
     * TokenizerCurly} for the same language, and re-join its significant tokens via {@code
     * MiscRuleCurly.renderTokens} (accessible here since {@code protected} + same package,
     * {@code com.jxmake.formatter.rules}) -- the same generic tight/loose token-adjacency spacing
     * every other rendering path in this codebase already uses (operators, calls, generics, etc.),
     * with no new spacing logic invented for this pass. Conservative bailout, matching every other
     * pass in this file: a span containing a NEWLINE or comment token (multi-line or commented
     * interpolation), a frozen token, or a tokenizer-rejected fragment is left byte-for-byte
     * untouched rather than guessed at. A nested template literal inside an interpolation
     * (`` `${`inner ${x}`}` ``) is treated as opaque quoted text for span-finding purposes and its
     * own interior is not recursively reformatted this pass -- a documented, narrow scope limit,
     * not attempted given how rare doubly-nested interpolation is in practice.
     */
    public String enforceTemplateLiteralInterpolationSpacing(final List<Token> tokens) {
        if (!lang.isJs && !lang.isTs) {
            return render(tokens, new HashMap<>());
        }
        final Map<Integer, String> overrides = new HashMap<>();
        final TokenizerCurly innerTokenizer = new TokenizerCurly(lang);
        final MiscRuleCurly misc = new MiscRuleCurly(lang, false, false);
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.STRING && !t.frozen && t.text.length() >= 2
                    && t.text.charAt(0) == '`') {
                final String rewritten = rewriteTemplateLiteral(t.text, innerTokenizer, misc);
                if (rewritten != null && !rewritten.equals(t.text)) {
                    overrides.put(i, rewritten);
                }
            }
        }
        return render(tokens, overrides);
    }

    /** Finds every top-level `${...}` span in {@code text} (a whole backtick-delimited template
     *  literal, opening/closing backtick included) and re-renders each interior expression via
     *  {@code renderTokens}, splicing the results back into the literal's raw text. Returns
     *  {@code text} unchanged if there are no interpolations, or if any span's own interior fails
     *  its conservative reformat check (see {@link #reformatInterpolationInterior}). */
    private String rewriteTemplateLiteral(final String text, final TokenizerCurly innerTokenizer,
            final MiscRuleCurly misc) {
        final List<int[]> spans = findInterpolationSpans(text);
        if (spans.isEmpty()) {
            return text;
        }
        final StringBuilder out = new StringBuilder();
        int last = 0;
        for (final int[] span : spans) {
            final int start = span[0];
            final int end = span[1];
            out.append(text, last, start);
            final String interior = text.substring(start, end);
            final String rewritten = reformatInterpolationInterior(interior, innerTokenizer, misc);
            out.append(rewritten != null ? rewritten : interior);
            last = end;
        }
        out.append(text.substring(last));
        return out.toString();
    }

    /** Scans a template literal's raw text (opening/closing backtick included) for every
     *  top-level `${...}` interpolation, returning each as a {@code [interiorStart, interiorEnd)}
     *  index pair (excluding the `${`/`}` delimiters themselves). Mirrors {@code
     *  TokenizerCurly.skipTemplateInterpolation}'s nesting rules: `{`/`}` depth counting, with
     *  nested `"`/`'`/`` ` `` quoted spans skipped as opaque units so an interior brace inside a
     *  string literal (or a nested template) doesn't corrupt depth counting. */
    private List<int[]> findInterpolationSpans(final String text) {
        final List<int[]> spans = new ArrayList<>();
        final int end = text.length() - 1; // exclude the closing backtick
        int pos = 1; // skip the opening backtick
        while (pos < end) {
            final char c = text.charAt(pos);
            if (c == '\\') {
                pos += 2;
                continue;
            }
            if (c == '$' && pos + 1 < end && text.charAt(pos + 1) == '{') {
                final int interiorStart = pos + 2;
                int depth = 1;
                int p = interiorStart;
                while (p < end && depth > 0) {
                    final char cc = text.charAt(p);
                    if (cc == '\\') {
                        p += 2;
                        continue;
                    } else if (cc == '{') {
                        depth++;
                        p++;
                    } else if (cc == '}') {
                        depth--;
                        p++;
                    } else if (cc == '"' || cc == '\'' || cc == '`') {
                        p = skipQuotedSpan(text, p, end, cc);
                    } else {
                        p++;
                    }
                }
                if (depth == 0) {
                    spans.add(new int[] { interiorStart, p - 1 });
                }
                pos = p;
                continue;
            }
            pos++;
        }
        return spans;
    }

    /** Skips a quoted span (`"`/`'`/`` ` ``-delimited, {@code quote} is the delimiter char)
     *  starting at {@code p} (the opening delimiter itself), returning the index right after its
     *  closing delimiter -- or {@code boundIdx} if unterminated within that bound. A nested
     *  backtick template's own interior (including any of its own `${...}`) is treated as opaque
     *  by this same skip -- not reformatted, see this section's own javadoc scope note. */
    private int skipQuotedSpan(final String text, final int p, final int boundIdx, final char quote) {
        int i = p + 1;
        while (i < boundIdx) {
            final char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == quote) {
                return i + 1;
            }
            i++;
        }
        return boundIdx;
    }

    /** Re-tokenizes {@code interior} (a `${...}` interpolation's raw interior substring) in
     *  isolation and re-joins its significant tokens via {@code renderTokens}'s ordinary
     *  tight/loose adjacency rules. Returns {@code null} (caller leaves the original interior
     *  untouched) if the interior is empty/blank, contains a NEWLINE or comment token (multi-line
     *  or commented interpolation -- out of this flat pass's scope), or contains a frozen token. */
    private String reformatInterpolationInterior(final String interior, final TokenizerCurly innerTokenizer,
            final MiscRuleCurly misc) {
        if (interior.trim().isEmpty()) {
            return null;
        }
        final List<Token> innerTokens;
        try {
            innerTokens = innerTokenizer.tokenize(interior);
        } catch (final RuntimeException e) {
            return null;
        }
        final List<Token> significant = new ArrayList<>();
        for (final Token t : innerTokens) {
            if (t.type == TokenType.NEWLINE || isComment(t) || t.frozen) {
                return null;
            }
            if (t.type == TokenType.WHITESPACE) {
                continue;
            }
            significant.add(t);
        }
        if (significant.isEmpty()) {
            return null;
        }
        return misc.renderTokens(significant);
    }

    // ── §5 Named function / class-method Allman brace style ──────────────────────────
    /**
     * STYLE_JS_TS.md §5: a named function declaration (`function foo() {}`) or class method
     * (`render() {}`, including getters/setters, `async`/`static`/generator `*` variants, and
     * constructors) moves its own `{` to its own line (Allman) -- mirrors
     * {@code JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle}'s role for Java, but
     * substantially simplified: JS/TS has no `throws` clause, no compact-constructor shape, and no
     * enum-constant-body false positive to guard against (JS/TS enums, §12, have no per-constant
     * body syntax at all).
     *
     * <p>Candidate signal: the `{`'s own header, walked backward from the `{`, must resolve to a
     * `)` whose matching `(` is itself immediately preceded by an IDENTIFIER (the function/method
     * name) -- this alone excludes every control-flow brace (`if`/`while`/`for`/`switch`/`catch`
     * precede their `(` with a KEYWORD, never an IDENTIFIER) and every anonymous function
     * expression (`function(...) {}`, whose `(` is preceded directly by the `function` keyword,
     * not a name). Between that `)` and the `{` there are two possible shapes: (a) directly
     * adjacent (plain JS, or a TS method/function with no return-type annotation), or (b) a TS
     * return-type annotation (`): Promise<Result> {`) -- {@link #findHeaderCloseParen} walks
     * backward from the `{` looking for a `:` immediately preceded by a `)`, so the return type's
     * own content (however complex -- generics, unions, etc.) is never inspected, only relocated
     * along with the brace.
     *
     * <p>An arrow function's block body (`=> {`) is excluded by construction -- its `{` is
     * directly preceded by `=>`, never by `)` or a `:`-return-type tail, so it never matches the
     * header-walk above at all; STYLE_JS_TS.md §6's K&R-for-arrow-block-bodies rule needs no
     * explicit exclusion check here.
     *
     * <p>Exceptions, all "stays K&R, not Allman", per §5's own worked-example text: (1) an
     * empty body (`{}`, nothing between the braces) -- {@link #isEmptyBody}; (2) a one-liner
     * whose entire `{ ... }` body sits on one physical line (covers getter/setter one-liner
     * groups and any other short one-liner method per STYLE.md §14's squeeze-onto-one-line shape,
     * without needing a getter/setter-specific check -- any one-liner method stays K&R the same
     * way) -- {@link #isSingleLineBraceBody}.
     *
     * <p>A `{` already on its own line (a NEWLINE already present in the gap between the header's
     * last token and the `{`) is left untouched -- idempotent. Any frozen token across the header
     * span is a conservative bailout, matching every other pass in this file.
     */
    public String enforceMethodDefinitionAllmanBraceStyle(final List<Token> tokens) {
        if (!lang.isJs && !lang.isTs) {
            return render(tokens, new HashMap<>());
        }
        final Map<Integer, Integer> braceOpenToClose = matchBraces(tokens);
        final Map<Integer, Integer> parenOpenToClose = matchParens(tokens);
        final Map<Integer, Integer> overrides = new HashMap<>();

        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), "{")) {
                continue;
            }
            final int lastHeaderTokenIdx = prevSignificantIndex(tokens, i - 1);
            if (lastHeaderTokenIdx < 0) {
                continue;
            }
            final int closeParenIdx = findHeaderCloseParen(tokens, lastHeaderTokenIdx);
            if (closeParenIdx < 0) {
                continue;
            }
            final Integer openParenIdx = findMatchingOpenParen(tokens, closeParenIdx);
            if (openParenIdx == null) {
                continue;
            }
            final int nameIdx = prevSignificantIndex(tokens, openParenIdx - 1);
            if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
                continue;
            }
            if (hasNewlineOrCommentBetween(tokens, lastHeaderTokenIdx, i)) {
                continue;
            }
            if (anyFrozen(tokens, nameIdx, i + 1)) {
                continue;
            }
            final Integer closeBraceIdx = braceOpenToClose.get(i);
            if (closeBraceIdx == null) {
                continue;
            }
            if (isEmptyBody(tokens, i, closeBraceIdx) || isSingleLineBraceBody(tokens, i, closeBraceIdx)) {
                continue;
            }
            overrides.put(lastHeaderTokenIdx, i); // marker: this token's gap-to-brace gets rewritten below
        }
        if (overrides.isEmpty()) {
            return render(tokens, new HashMap<>());
        }
        return renderAllmanBraceMoves(tokens, overrides, parenOpenToClose);
    }

    /** Walks backward from {@code fromIdx} (the last significant token seen before a candidate
     *  `{`) to find the header's own closing `)`. If {@code fromIdx} itself is `)`, that's the
     *  direct-adjacency (plain JS, or TS with no return type) case. Otherwise looks for a `:`
     *  return-type-annotation tail whose own immediately-preceding significant token is `)` --
     *  the return type's own interior content between that `:` and {@code fromIdx} is never
     *  otherwise inspected. Returns -1 if neither shape is found before a statement-breaking
     *  token (`;`, `{`, `}`) is hit. */
    private int findHeaderCloseParen(final List<Token> tokens, final int fromIdx) {
        if (isPunct(tokens.get(fromIdx), ")")) {
            return fromIdx;
        }
        int j = fromIdx;
        int steps = 0;
        while (j >= 0 && steps < 500) {
            final Token t = tokens.get(j);
            if (isPunct(t, ";") || isPunct(t, "{") || isPunct(t, "}")) {
                return -1;
            }
            if (isOp(t, ":")) {
                final int before = prevSignificantIndex(tokens, j - 1);
                if (before >= 0 && isPunct(tokens.get(before), ")")) {
                    return before;
                }
                return -1;
            }
            j = prevSignificantIndex(tokens, j - 1);
            steps++;
        }
        return -1;
    }

    private Integer findMatchingOpenParen(final List<Token> tokens, final int closeParenIdx) {
        int depth = 0;
        for (int i = closeParenIdx; i >= 0; i--) {
            if (isPunct(tokens.get(i), ")")) {
                depth++;
            } else if (isPunct(tokens.get(i), "(")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return null;
    }

    /** True if {@code braceIdx}/{@code closeBraceIdx} delimit an empty body -- nothing (not even
     *  a comment) between the two braces. */
    private boolean isEmptyBody(final List<Token> tokens, final int braceIdx, final int closeBraceIdx) {
        return nextSignificantIndex(tokens, braceIdx + 1) == closeBraceIdx
                && !hasNewlineOrCommentBetween(tokens, braceIdx, closeBraceIdx);
    }

    /** True if no NEWLINE token appears anywhere between {@code braceIdx} and
     *  {@code closeBraceIdx} inclusive -- the whole body sits on one physical line. */
    private boolean isSingleLineBraceBody(final List<Token> tokens, final int braceIdx, final int closeBraceIdx) {
        for (int i = braceIdx; i <= closeBraceIdx; i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return false;
            }
        }
        return true;
    }

    private boolean hasNewlineOrCommentBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.NEWLINE || type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    private boolean anyFrozen(final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (tokens.get(i).frozen) {
                return true;
            }
        }
        return false;
    }

    /** Second pass: given the set of `{` indices approved for Allman conversion (keyed by the
     *  last header token that immediately precedes each, in {@code headerTokenToBrace}), rebuilds
     *  the token stream, replacing the gap right after each such header token with a NEWLINE plus
     *  the header line's own leading indentation, followed by the brace. */
    private String renderAllmanBraceMoves(final List<Token> tokens, final Map<Integer, Integer> headerTokenToBrace,
            final Map<Integer, Integer> parenOpenToClose) {
        final StringBuilder out = new StringBuilder();
        int i = 0;
        final int n = tokens.size();
        while (i < n) {
            out.append(tokens.get(i).text);
            final Integer braceIdx = headerTokenToBrace.get(i);
            if (braceIdx != null) {
                out.append('\n').append(lineIndent(tokens, i));
                out.append(tokens.get(braceIdx).text);
                // Skip the original gap + `{` we just relocated.
                i = braceIdx + 1;
                continue;
            }
            i++;
        }
        return out.toString();
    }

    /** Line-leading whitespace of the physical line containing token {@code idx} -- {@code ""} if
     *  that line has no leading whitespace (column-0 start). */
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

    // ── §6 Arrow functions -- spacing and always-kept parameter parens ───────────────
    /**
     * STYLE_JS_TS.md §6: `=>` is always spaced (one space on both sides), same as Kotlin's `->`
     * ({@link KotlinSpecificRule#enforceArrowSpacing}, reused here structurally). Confirmed via a
     * standalone harness before writing this method that `=>` spacing is <b>not</b> already free
     * from any existing generic pass -- this codebase has no general from-scratch binary-operator
     * respacing pass for any language (e.g. `const x=1;`/`const y = 1+2;` both round-trip with
     * their original spacing completely untouched), so `=>` needs its own dedicated flat pass,
     * same conservative bailout (a gap containing a comment, a NEWLINE, or a frozen token is left
     * untouched) as every other pass in this file.
     *
     * <p>The other two §6 items need no code here: (1) K&R brace style for an arrow block body is
     * already satisfied by construction -- {@link #enforceMethodDefinitionAllmanBraceStyle}'s own
     * candidate signal never matches a `{` directly preceded by `=>`, so an arrow body's brace is
     * simply never touched by any Allman-conversion pass, staying wherever it was written (K&R);
     * (2) same-line no-brace for a single-expression body is likewise never rewritten either way
     * (this codebase never auto-adds or auto-strips braces around a body -- see
     * `collapseSingleExpressionBlocks`'s narrower control-flow-only scope -- so a single-expression
     * arrow body written braceless stays braceless, matching §6's own worked examples, without any
     * dedicated pass).
     */
    public String enforceArrowSpacing(final List<Token> tokens) {
        if (!lang.isJs && !lang.isTs) {
            return render(tokens, new HashMap<>());
        }
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

            final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean adjacentToArrow = isOp(lastSignificant, "=>") || isOp(t, "=>");

            if (gapBlocked || !adjacentToArrow) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            } else {
                out.append(' ');
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

    /**
     * STYLE_JS_TS.md §6: an arrow function's parameter list keeps its parens even for a single
     * untyped parameter -- `(n) => ...`, never bare `n => ...` -- "for alignment consistency with
     * multi-parameter arrows in the same group" (the style doc's own stated reasoning). Read as an
     * always-normalize rule (not a preserve-as-written one): every `=>` whose immediately
     * preceding significant token is a bare IDENTIFIER (never already parenthesized -- a
     * multi/typed/zero-parameter arrow's `=>` is always preceded by `)` instead, untouched by this
     * check) gets that identifier wrapped in parens. Conservative bailout matching this file's
     * other passes: a gap containing a NEWLINE or comment between the identifier and `=>`, or
     * either token being frozen, leaves the pair untouched.
     */
    public String enforceArrowFunctionParameterParens(final List<Token> tokens) {
        if (!lang.isJs && !lang.isTs) {
            return render(tokens, new HashMap<>());
        }
        final Map<Integer, String> overrides = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (!isOp(t, "=>") || t.frozen) {
                continue;
            }
            final int prevIdx = prevSignificantIndex(tokens, i - 1);
            if (prevIdx < 0) {
                continue;
            }
            final Token prev = tokens.get(prevIdx);
            if (prev.type != TokenType.IDENTIFIER || prev.frozen) {
                continue;
            }
            if (hasNewlineOrCommentBetween(tokens, prevIdx, i)) {
                continue;
            }
            overrides.put(prevIdx, "(" + prev.text + ")");
        }
        return render(tokens, overrides);
    }

    // ── §11.2 Class field modifier-priority table ────────────────────────────────────
    /** STYLE_JS_TS.md §11.2's fixed six-slot modifier order -- `declare` first (ambient marker),
     *  then visibility (`public`/`private`/`protected`, mutually exclusive so all three share one
     *  slot), then `static`, `abstract`, `override`, `readonly` last (parallels Java's `final`
     *  taking the position right before the name). */
    private static final List<String> MODIFIER_ORDER = Arrays.asList(
            "declare", "public", "private", "protected", "static", "abstract", "override", "readonly");
    private static final Map<String, Integer> MODIFIER_PRIORITY = new HashMap<>();
    static {
        for (int i = 0; i < MODIFIER_ORDER.size(); i++) {
            MODIFIER_PRIORITY.put(MODIFIER_ORDER.get(i), i);
        }
    }

    /**
     * STYLE_JS_TS.md §11.2: normalizes a scrambled run of two-or-more consecutive class-member
     * modifier keywords into the canonical order above, e.g. `readonly static private x: number;`
     * → `private static readonly x: number;`. TS-only (`lang.isTs`) -- JS has none of these
     * modifier keywords at all (JS class fields have no `public`/`private`/`static`-as-a-modifier
     * grammar in this codebase's scope). A run of a single modifier keyword is left completely
     * untouched (nothing to reorder). Applied wherever a maximal run of 2+ consecutive modifier
     * keywords appears (not narrowly gated to "inside a class body" specifically) -- in valid
     * TS/JS syntax these keywords never co-occur consecutively outside a class-member modifier
     * list, so this is a safe, conservative scope choice that also naturally covers method
     * modifiers (`private static foo()`) the same way, consistent with the table's own Java-
     * derived precedent of one shared modifier order for both fields and methods. Only a
     * same-line, comment-free, non-frozen run is reordered -- a run containing a NEWLINE, a
     * comment, or a frozen token in any of its internal gaps is left untouched, matching this
     * file's usual conservative bailout posture.
     */
    public String reorderClassFieldModifiers(final List<Token> tokens) {
        if (!lang.isTs) {
            return render(tokens, new HashMap<>());
        }
        final StringBuilder out = new StringBuilder();
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isModifierKeyword(t) && !t.frozen) {
                final List<Token> runKeywords = new ArrayList<>();
                final List<List<Token>> gapsAfter = new ArrayList<>();
                int j = i;
                boolean brokenRun = false;
                while (j < n && isModifierKeyword(tokens.get(j)) && !tokens.get(j).frozen) {
                    runKeywords.add(tokens.get(j));
                    int k = j + 1;
                    final List<Token> gap = new ArrayList<>();
                    while (k < n && isGapToken(tokens.get(k))) {
                        final Token g = tokens.get(k);
                        if (isComment(g) || g.type == TokenType.NEWLINE || g.frozen) {
                            brokenRun = true;
                        }
                        gap.add(g);
                        k++;
                    }
                    gapsAfter.add(gap);
                    if (brokenRun) {
                        j = k;
                        break;
                    }
                    j = k;
                }
                if (!brokenRun && runKeywords.size() >= 2) {
                    final List<Token> sorted = new ArrayList<>(runKeywords);
                    sorted.sort((a, b) -> MODIFIER_PRIORITY.get(a.text) - MODIFIER_PRIORITY.get(b.text));
                    for (int idx = 0; idx < sorted.size(); idx++) {
                        out.append(sorted.get(idx).text);
                        for (final Token g : gapsAfter.get(idx)) {
                            out.append(g.text);
                        }
                    }
                } else {
                    for (int idx = i; idx < j; idx++) {
                        out.append(tokens.get(idx).text);
                    }
                }
                i = j;
                continue;
            }
            out.append(t.text);
            i++;
        }
        return out.toString();
    }

    private boolean isModifierKeyword(final Token t) {
        return t != null && t.type == TokenType.KEYWORD && MODIFIER_PRIORITY.containsKey(t.text);
    }

    // ── §11.1 Union / intersection type spacing (`|`, `&`) ───────────────────────────
    /**
     * STYLE_JS_TS.md §11.1: `|`/`&` used as TS union/intersection type operators get ordinary
     * binary-operator spacing -- one space on both sides. TS-only (`lang.isTs`); a bare bitwise
     * `|`/`&` in JS expression position is out of this section's scope and deliberately left
     * untouched (no existing pass in this codebase spaces bitwise `|`/`&` either, confirmed via a
     * standalone harness before writing this method -- so applying this pass to TS-only, rather
     * than gating on "type position" specifically, is a conservative scope choice: TS's own
     * bitwise `|`/`&` usages get the same spacing as its union/intersection ones, both being
     * ordinary ASCII binary operators the style doc treats identically once JS's silence on
     * bitwise-operator spacing is accepted as a pre-existing, unrelated gap rather than something
     * this pass needs to work around). Compound tokens (`||`, `&&`, `|=`, `&=`, `||=`, `&&=`) are
     * already lexed as their own distinct multi-char ops and never match the single-char `|`/`&`
     * check here, so they're untouched by construction. Break-style on overflow (STYLE_JS_TS.md
     * §11.1's break-before-operator vs. break-after-operator worked examples) is preserved as
     * written -- this pass only touches the gap immediately around a same-line `|`/`&`, a gap
     * already blocked by an embedded NEWLINE is left alone by the same conservative bailout every
     * other pass in this file uses.
     */
    public String enforceUnionIntersectionSpacing(final List<Token> tokens) {
        if (!lang.isTs) {
            return render(tokens, new HashMap<>());
        }
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

            final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean adjacentToUnionOrIntersection = isUnionOrIntersection(lastSignificant) || isUnionOrIntersection(t);

            if (gapBlocked || !adjacentToUnionOrIntersection) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            } else {
                out.append(' ');
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

    private boolean isUnionOrIntersection(final Token t) {
        return t != null && (isOp(t, "|") || isOp(t, "&"));
    }

    // ── §11 TypeScript type-annotation colon spacing ─────────────────────────────────
    /**
     * STYLE_JS_TS.md §11: a type-annotation colon (declarator, function/arrow parameter,
     * function return type) has no space before it and exactly one space after -- disambiguated
     * from an object-literal/destructuring-pattern key colon, a ternary-expression colon, and a
     * switch `case`/`default` label colon purely via local bracket-stack context, since the raw
     * token shape (`IDENTIFIER :`) is otherwise identical across all four. TS-only (JS has no
     * type annotations at all, so this is a no-op passthrough for {@code lang.isJs}). This is a
     * flat spacing pass only -- it does NOT implement STYLE_JS_TS.md §11's declaration-alignment-
     * grid column integration (RDD_KEY_183's `=`-aligned group behavior for consecutive
     * declarations); that remains unimplemented, see STATE_JS_TS.md. Conservative bailout
     * matching every other pass in this file: a gap touching a comment, NEWLINE, or frozen token
     * is left untouched, and a colon adjacent to any frozen token is never reclassified.
     *
     * <p>Classification (see {@link #classifyTypeColons}): a colon is a type colon when either
     * (a) its immediately preceding significant token is `)`, unless that `)` closes a
     * parenthesized `case (...):` label's own condition (return-type colon), or (b) its
     * immediately preceding significant token is an IDENTIFIER (or `?` tight after an
     * IDENTIFIER, for TS's `name?: type` optional-marker shape) whose own preceding context is
     * either inside a `(...)` parameter list directly after `(`/`,` (parameter colon), or at
     * statement level directly after `let`/`const`/`var`/`,` while still inside an open
     * declarator list (declaration colon). Every other shape -- object-literal/destructuring key
     * colons (enclosing bracket is a value `{`), ternary colons (preceding context is `?`, not
     * `(`/`,`/a declarator keyword), and case/default labels (preceding token is a literal or the
     * `default` keyword, never an IDENTIFIER or `)` from a plain unparenthesized label) -- falls
     * through unclassified and is left byte-for-byte as this pass found it.
     */
    public String enforceTypeColonSpacing(final List<Token> tokens) {
        if (!lang.isTs) {
            return render(tokens, new HashMap<>());
        }
        final Set<Integer> typeColons = classifyTypeColons(tokens);

        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        Token lastSignificant = null;
        int lastSigIdx = -1;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean beforeTypeColon = typeColons.contains(i);
            final boolean afterTypeColon = lastSigIdx >= 0 && typeColons.contains(lastSigIdx);

            if (gapBlocked || (!beforeTypeColon && !afterTypeColon)) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            } else if (afterTypeColon) {
                out.append(' ');
            }
            // beforeTypeColon && !gapBlocked && !afterTypeColon: gap dropped (tight before ':').

            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            lastSigIdx = i;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    /** Bracket-stack scan producing the index set of every `:` token classified as a
     *  type-annotation colon. See {@link #enforceTypeColonSpacing}'s javadoc for the exact rules. */
    private Set<Integer> classifyTypeColons(final List<Token> tokens) {
        final Set<Integer> result = new HashSet<>();
        final Map<Integer, Integer> braceOpenToClose = matchBraces(tokens);
        final Set<Integer> valueBraces = new HashSet<>();
        for (final Integer openIdx : braceOpenToClose.keySet()) {
            if (isValuePrecededBrace(tokens, openIdx)) {
                valueBraces.add(openIdx);
            }
        }
        final Map<Integer, Integer> parenOpenToClose = matchParens(tokens);
        final Map<Integer, Integer> parenCloseToOpen = new HashMap<>();
        for (final Map.Entry<Integer, Integer> e : parenOpenToClose.entrySet()) {
            parenCloseToOpen.put(e.getValue(), e.getKey());
        }

        final Deque<String> stack = new ArrayDeque<>();
        boolean inDeclarator = false;

        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                continue;
            }

            if (isPunct(t, ";")) {
                inDeclarator = false;
            } else if (t.type == TokenType.KEYWORD
                    && ("let".equals(t.text) || "const".equals(t.text) || "var".equals(t.text))
                    && (stack.isEmpty() || "BLOCK".equals(stack.peek()))) {
                inDeclarator = true;
            } else if (isOp(t, ":") && !t.frozen) {
                final int prevIdx = prevSignificantIndex(tokens, i - 1);
                if (prevIdx >= 0 && !tokens.get(prevIdx).frozen) {
                    if (isTypeColonAt(tokens, i, prevIdx, stack, inDeclarator, parenCloseToOpen)) {
                        result.add(i);
                    }
                }
            }

            if (isPunct(t, "(")) {
                stack.push("PAREN");
            } else if (isPunct(t, "[")) {
                stack.push("BRACKET");
            } else if (isPunct(t, "{")) {
                stack.push(valueBraces.contains(i) ? "OBJ" : "BLOCK");
            } else if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                if (!stack.isEmpty()) {
                    stack.pop();
                }
            }
        }
        return result;
    }

    private boolean isTypeColonAt(final List<Token> tokens, final int colonIdx, final int prevIdx,
            final Deque<String> stack, final boolean inDeclarator, final Map<Integer, Integer> parenCloseToOpen) {
        final Token prev = tokens.get(prevIdx);
        if (isPunct(prev, ")")) {
            return !isCaseLabelParen(tokens, prevIdx, parenCloseToOpen);
        }
        int idIdx = -1;
        if (prev.type == TokenType.IDENTIFIER) {
            idIdx = prevIdx;
        } else if (isOp(prev, "?")) {
            final int maybeId = prevSignificantIndex(tokens, prevIdx - 1);
            if (maybeId >= 0 && tokens.get(maybeId).type == TokenType.IDENTIFIER) {
                idIdx = maybeId;
            }
        }
        if (idIdx < 0) {
            return false;
        }
        final int ctxIdx = prevSignificantIndex(tokens, idIdx - 1);
        final Token ctx = ctxIdx >= 0 ? tokens.get(ctxIdx) : null;
        final boolean paramCtx = "PAREN".equals(stack.peek())
                && ctx != null && (isPunct(ctx, "(") || isPunct(ctx, ","));
        final boolean declaratorCtx = inDeclarator
                && (stack.isEmpty() || "BLOCK".equals(stack.peek()))
                && ctx != null && (isPunct(ctx, ",") || (ctx.type == TokenType.KEYWORD
                        && ("let".equals(ctx.text) || "const".equals(ctx.text) || "var".equals(ctx.text))));
        return paramCtx || declaratorCtx;
    }

    /** True if {@code closeParenIdx} closes a `case (...)：`-style parenthesized case-label
     *  condition -- i.e. the token immediately before the matching `(` is the `case` keyword --
     *  the one shape where a bare `)` immediately followed by `:` is NOT a return-type colon. */
    private boolean isCaseLabelParen(final List<Token> tokens, final int closeParenIdx,
            final Map<Integer, Integer> parenCloseToOpen) {
        final Integer openIdx = parenCloseToOpen.get(closeParenIdx);
        if (openIdx == null) {
            return false;
        }
        final int beforeOpen = prevSignificantIndex(tokens, openIdx - 1);
        return beforeOpen >= 0 && tokens.get(beforeOpen).type == TokenType.KEYWORD
                && "case".equals(tokens.get(beforeOpen).text);
    }

    /** Same "is this `{` a value/pattern brace" heuristic as {@link #classifyBraces}'s
     *  {@code isValue} local, duplicated (not shared) since it's used from a different pass with
     *  different bookkeeping needs (a bracket-kind stack, not resetDepth/needsSemicolon maps). */
    private boolean isValuePrecededBrace(final List<Token> tokens, final int openIdx) {
        final int prevIdx = prevSignificantIndex(tokens, openIdx - 1);
        final Token prev = prevIdx >= 0 ? tokens.get(prevIdx) : null;
        if (prev == null) {
            return false;
        }
        return isOp(prev, "=>") || isOp(prev, "=") || isPunct(prev, "(") || isPunct(prev, "[")
                || isPunct(prev, ",") || isOp(prev, ":") || isOp(prev, "??") || isOp(prev, "||")
                || isOp(prev, "&&") || isOp(prev, "?") || isOp(prev, "...")
                || (prev.type == TokenType.KEYWORD && ("return".equals(prev.text) || "yield".equals(prev.text)
                        || "throw".equals(prev.text) || "typeof".equals(prev.text)
                        || "const".equals(prev.text) || "let".equals(prev.text) || "var".equals(prev.text)));
    }

    // ── §9 Decorators ────────────────────────────────────────────────────────────

    /**
     * STYLE_JS_TS.md §9: {@code @} binds tight to the decorator name, no space, "same as any
     * other unary prefix". Confirmed via a standalone harness before writing this method that
     * this is <b>not</b> already free from any existing generic pass -- a deliberately mis-spaced
     * {@code @ Inject(TOKEN)} round-tripped completely untouched, and the generic
     * `needsSpaceBetween` join used elsewhere in this codebase defaults to inserting a space
     * between an `@` OP token and a following IDENTIFIER (no existing "tight unary prefix"
     * exception for `@`, unlike e.g. `!`/`~`). Flat gap-normalizing scan, same conservative
     * bailout shape as every other spacing pass in this file: a gap containing a comment, a
     * NEWLINE, or a frozen token is left untouched. `@` only ever appears in valid JS/TS source
     * as a decorator marker (no bitwise/other operator use, unlike Kotlin's overloaded operators),
     * so no additional context check is needed before tightening every occurrence.
     */
    public String enforceDecoratorTightAtSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        int i = 0;
        final int n = tokens.size();
        while (i < n) {
            final Token t = tokens.get(i);
            out.append(t.text);
            if (isOp(t, "@") && !t.frozen) {
                final int nextSig = nextSignificantIndex(tokens, i + 1);
                if (nextSig > i + 1) {
                    final List<Token> gap = tokens.subList(i + 1, nextSig);
                    final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g)
                            || g.type == TokenType.NEWLINE || g.frozen);
                    if (!gapBlocked) {
                        i = nextSig;
                        continue;
                    }
                }
            }
            i++;
        }
        return out.toString();
    }

    /**
     * STYLE_JS_TS.md §9's overflow cascade: when a decorator plus the target it precedes would
     * exceed {@link #lineLengthLimit} on one line, drop the decorator to its own line first --
     * keeping the target on the next line at the same indentation -- before falling back to
     * wrapping the decorator's own argument list (that second step needs no new code here: a
     * decorator's {@code @Name(args)} call already matches the generic
     * {@code MiscRuleCurly.enforceCallLineBreaking} scan's "IDENTIFIER (" candidate shape, so an
     * overlong decorator-with-args, once alone on its own line, is already wrapped by that
     * existing pass same as any other overlong call). This method only ever does the first step
     * (own-line drop) -- it never touches the decorator's own argument list.
     *
     * <p>Only an <b>inline</b> decorator (its own line still holds more content after it -- no
     * NEWLINE between the decorator's own closing token and what follows) is a candidate; a
     * decorator already on its own line is left completely alone, matching §9's own "the
     * formatter never moves a decorator from one placement to the other" placement-preservation
     * rule -- this pass only ever inserts a break for an inline decorator that doesn't fit, never
     * removes one that's already own-line, and never merges an own-line decorator back inline
     * either way.
     *
     * <p>Must run in Phase 1, before {@code enforceComplexityPadding}/{@code
     * enforceCallLineBreaking} (same ordering constraint as this file's other Phase 1 structural
     * passes, e.g. {@code enforceArrowFunctionParameterParens}) -- inserting a line break here
     * changes what "the rest of this line" even is for those later width-driven passes, and they
     * need to see the post-split shape on the very first format pass for the "does it fit"
     * decision to stay stable across reformats.
     */
    public String enforceDecoratorOverflowCascade(final List<Token> tokens) {
        final Map<Integer, Integer> parenOpenToClose = matchParens(tokens);
        final StringBuilder out = new StringBuilder();
        int i = 0;
        final int n = tokens.size();
        while (i < n) {
            final Token t = tokens.get(i);
            if (isOp(t, "@") && !t.frozen) {
                final int decoratorEnd = findDecoratorEnd(tokens, i, parenOpenToClose);
                final int nextSig = decoratorEnd >= 0 ? nextSignificantIndex(tokens, decoratorEnd + 1) : -1;
                final boolean alreadyOwnLine = nextSig >= 0 && hasNewlineBetween(tokens, decoratorEnd + 1, nextSig);
                if (decoratorEnd >= 0 && nextSig >= 0 && !alreadyOwnLine && !anyFrozen(tokens, i, decoratorEnd + 1)) {
                    final String indent = lineIndent(tokens, i);
                    final int lineEnd = lineEndIndex(tokens, nextSig);
                    final String wholeLine = indent
                            + collapseTokensToOneLine(tokens.subList(lineStartTokenIndex(tokens, i), lineEnd));
                    if (wholeLine.length() > lineLengthLimit) {
                        // Drop: emit the decorator itself, then a NEWLINE + the same
                        // indentation, then let the loop continue from the target.
                        out.append(collapseTokensToOneLine(tokens.subList(i, decoratorEnd + 1)));
                        out.append('\n').append(indent);
                        i = nextSig;
                        continue;
                    }
                }
            }
            out.append(t.text);
            i++;
        }
        return out.toString();
    }

    /** Index of the last token of a decorator application starting at {@code atIdx} (the
     *  {@code @} token itself): the decorator name identifier for a bare {@code @Name}, or the
     *  matching {@code )} for {@code @Name(args)}. Returns -1 if the shape isn't recognized (no
     *  identifier immediately follows {@code @}). */
    private int findDecoratorEnd(final List<Token> tokens, final int atIdx,
            final Map<Integer, Integer> parenOpenToClose) {
        final int nameIdx = nextSignificantIndex(tokens, atIdx + 1);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return -1;
        }
        // A qualified decorator name (`@ns.Name`) walks forward over `.identifier` pairs too.
        int end = nameIdx;
        while (true) {
            final int dotIdx = nextSignificantIndex(tokens, end + 1);
            if (dotIdx < 0 || !isOp(tokens.get(dotIdx), ".")) {
                break;
            }
            final int idIdx = nextSignificantIndex(tokens, dotIdx + 1);
            if (idIdx < 0 || tokens.get(idIdx).type != TokenType.IDENTIFIER) {
                break;
            }
            end = idIdx;
        }
        final int afterName = nextSignificantIndex(tokens, end + 1);
        if (afterName >= 0 && isPunct(tokens.get(afterName), "(")) {
            final Integer closeIdx = parenOpenToClose.get(afterName);
            if (closeIdx != null) {
                return closeIdx;
            }
        }
        return end;
    }

    /** True if any {@code NEWLINE} token appears in {@code [from, to)}. */
    private boolean hasNewlineBetween(final List<Token> tokens, final int from, final int to) {
        for (int i = from; i < to && i < tokens.size(); i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return true;
            }
        }
        return false;
    }

    /** First token index of the physical line containing token {@code idx}. */
    private int lineStartTokenIndex(final List<Token> tokens, final int idx) {
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return i + 1;
            }
        }
        return 0;
    }

    /** Index one past the last token of the physical line containing token {@code idx} (i.e. the
     *  index of the line's own trailing {@code NEWLINE}, or the token count if the file ends
     *  without one). */
    private int lineEndIndex(final List<Token> tokens, final int idx) {
        for (int i = idx; i < tokens.size(); i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return i;
            }
        }
        return tokens.size();
    }

    /** Plain concatenation of each token's own raw text across {@code [from, to)} of a token
     *  sublist -- used only for this method's own line-length estimate and for re-emitting the
     *  decorator's own span verbatim when dropping it to its own line; not a general-purpose
     *  renderer (does not touch spacing, unlike {@code MiscRuleCurly.collapseTokensToOneLine}). */
    private String collapseTokensToOneLine(final List<Token> slice) {
        final StringBuilder sb = new StringBuilder();
        for (final Token t : slice) {
            sb.append(t.text);
        }
        return sb.toString();
    }
}
