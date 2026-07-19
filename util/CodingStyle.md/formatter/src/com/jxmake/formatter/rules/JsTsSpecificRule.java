/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
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
}
