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

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Family-agnostic base for {@link DeclarationAlignmentRuleCurly} (and, in the future,
 * {@code DeclarationAlignmentRuleIndent}) -- everything in this file used to live directly in
 * {@code DeclarationAlignmentRule} before the curly/indent class-refactor. No behavior change,
 * mechanical move only (see STATE_COMMON.md's Class Refactor section).
 */
public abstract class DeclarationAlignmentRuleCore {

    /** Control-flow keywords whose own condition/argument parens must never be mistaken for a
     *  C-style cast's parens by {@link #isCStyleCastClose} -- see that method's call site. */
    private static final Set<String> CONTROL_FLOW_KEYWORDS = setOf(
            "if", "while", "for", "switch", "catch", "do", "else");

    protected final Lang lang;
    // Raised private -> protected (RDD_KEY_139's "loosen-then-extend" precedent) so
    // KotlinDeclarationAlignmentRule can reuse it for its own width-budget pre-check
    // (RDD_KEY_162) -- purely additive, no behavior change.
    protected final int lineLengthLimit;

    protected DeclarationAlignmentRuleCore(final Lang lang, final int lineLengthLimit) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
    }

    protected static Set<String> setOf(final String... words) {
        final Set<String> s = new HashSet<>();
        java.util.Collections.addAll(s, words);
        return s;
    }

    /** Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     *  (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change. */
    protected String renderTokens(final List<Token> tokens) {
        final StringBuilder sb = new StringBuilder();
        Token prev = null;
        for (final Token t : tokens) {
            if (prev != null && needsSpaceBetween(prev, t)) {
                sb.append(' ');
            }
            sb.append(t.text);
            prev = t;
        }
        return sb.toString();
    }

    /** Renders initializer value tokens (the right-hand side of `= expr`) where `*` and `&`
     *  may represent either binary operators or unary pointer/reference operators. Uses
     *  lookahead to distinguish binary `*`/`&`: if followed by an IDENTIFIER or NUMBER and
     *  preceded by an IDENTIFIER or WHITESPACE, a space is inserted before the operator.
     *  Also suppresses spacing between unary dereference `*`/`**` and the following
     *  identifier in C/C++ expression contexts (e.g. `*ptr`, `**ptr`), while all other
     *  spacing follows the normal token-spacing rules. */
    protected String renderInitTokens(final List<Token> tokens) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            final Token prev = i > 0 ? tokens.get(i - 1) : null;
            final Token next = i < tokens.size() - 1 ? tokens.get(i + 1) : null;
            if (prev != null) {
                if (isTightToken(t) && (isOp(t, "*") || isOp(t, "&"))
                        && next != null
                        && (next.type == TokenType.IDENTIFIER || next.type == TokenType.NUMBER)) {
                    if(prev.type != TokenType.IDENTIFIER && prev.type != TokenType.WHITESPACE) {
                    }
                    else {
                        sb.append(' '); // binary * or & in expression context
                    }
                } else if (needsSpaceBetween(prev, t)) {
                    final Token prev2 = i > 1 ? tokens.get(i - 2) : null;
                    if (t.type == TokenType.IDENTIFIER && Token.isRepOp(prev, '*')
                        && (prev2 == null || prev2.type == TokenType.OP)
                        && (lang.isC || lang.isCpp)) {
                        // pointer dereference: add nothing
                    }
                    else if (isPunct(prev, ")") && isCStyleCastClose(tokens, i - 1)) {
                        // C-style cast `(Type)expr`: add nothing
                    }
                    else {
                        sb.append(' ');
                    }
                }
            }
            sb.append(t.text);
        }
        return sb.toString();
    }

    /**
     * True iff the `)` at `closeIdx` in `tokens` closes a C-style cast: `(Type)` where the
     * content between the matching `(` and `)` is just a type-like token sequence
     * (IDENTIFIER/KEYWORD plus optional `*`), and the token before the matching `(` is not
     * an IDENTIFIER/`)`/`]` (which would make it a function call or subscript instead).
     */
    protected boolean isCStyleCastClose(final List<Token> tokens, final int closeIdx) {
        int depth = 0;
        int openIdx = -1;
        for (int k = closeIdx; k >= 0; k--) {
            final Token t = tokens.get(k);
            if (isPunct(t, ")")) {
                depth++;
            } else if (isPunct(t, "(")) {
                depth--;
                if (depth == 0) {
                    openIdx = k;
                    break;
                }
            }
        }
        if (openIdx < 0 || openIdx == closeIdx - 1) {
            return false; // empty parens
        }
        final Token before = openIdx > 0 ? tokens.get(openIdx - 1) : null;
        if (before != null && (before.type == TokenType.IDENTIFIER
                || isPunct(before, ")") || isPunct(before, "]"))) {
            return false; // function call / subscript, not a cast
        }
        // A control-flow keyword's own condition parens (`if(node instanceof X)`,
        // `while(cond)`, `for(...)`, `switch(...)`, `catch(...)`) has the exact same shape this
        // method looks for -- IDENTIFIER/KEYWORD-only content, and `if`/`while`/etc. are
        // KEYWORD tokens, not IDENTIFIER/`)`/`]`, so they slip past the check above. Left
        // unguarded, a braceless `if(cond)stmt` collapsed onto one line by
        // `BlockStructureRule` gets its condition's closing paren misclassified as a C-style
        // cast close, suppressing the space that must separate it from the following
        // statement (`if(node instanceof RecordPatternExpr)reporter.report(...)` instead of
        // `... RecordPatternExpr) reporter.report(...)`) -- only when this whole construct is
        // itself rendered as a declaration's initializer via `renderInitTokens`.
        if (before != null && before.type == TokenType.KEYWORD
                && CONTROL_FLOW_KEYWORDS.contains(before.text)) {
            return false;
        }
        for (int k = openIdx + 1; k < closeIdx; k++) {
            final Token t = tokens.get(k);
            if (t.type != TokenType.IDENTIFIER && t.type != TokenType.KEYWORD
                    && !Token.isRepOp(t, '*')) {
                return false;
            }
        }
        return true;
    }

    /** Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     *  (its own {@code renderTokens} override, real-code testing against {@code square/okio}) --
     *  purely additive, no behavior change. */
    protected boolean needsSpaceBetween(final Token prev, final Token cur) {
        if (isTightToken(cur)) {
            return false;
        }
        // Kotlin's negated type-check/containment operators (`!is`, `!in`) are a single tight
        // lexical unit -- STYLE_KOTLIN.md renders them with no space between `!` and the
        // keyword, unlike a plain `is`/`in` (`a is B`, always spaced). The tokenizer still
        // lexes `!` and `is`/`in` as two separate tokens, so without this check the generic
        // KEYWORD-gets-a-leading-space default below inserted a space here, corrupting `!is`/
        // `!in` into `! is`/`! in` -- a Kotlin parse error found via dogfood-testing
        // RobotCoding gui_frontend_android's ProgramBuilder.kt (`it !is _FunctionItem`).
        if (lang.isKotlin && isOp(prev, "!") && cur.type == TokenType.KEYWORD
                && ("is".equals(cur.text) || "in".equals(cur.text))) {
            return false;
        }
        if (isPunct(cur, "(") && (prev.type == TokenType.IDENTIFIER
                || prev.type == TokenType.ANGLE_BRACKET_CLOSE)) {
            return false;
        }
        // Kotlin `get`/`set` accessor keywords act like an ordinary call name immediately before
        // `(` (e.g. a merged §8 one-liner property, `val x : Int get() = 1`, RDD_KEY_133) --
        // `get`/`set` are lexed as TokenType.KEYWORD (STYLE_KOTLIN.md's soft-keyword list), not
        // IDENTIFIER, so the general identifier-before-`(` tight rule above doesn't fire for them
        // without this carve-out. Gated to Kotlin only; no-op for C/C++/Java (neither keyword is
        // reserved there, so this case never reaches this method for them).
        if (lang.isKotlin && isPunct(cur, "(") && prev.type == TokenType.KEYWORD
                && ("get".equals(prev.text) || "set".equals(prev.text))) {
            return false;
        }
        // C/C++/Java-only: a brace-initializer directly after a type/identifier (`int arr[] =
        // {1,2,3}`, C++'s uniform-init `Widget w{}`) is tight, no space before `{`. Kotlin has no
        // such shape -- its only identifier-then-`{` construct is a trailing lambda (`x?.let {
        // it + 1 }`), which STYLE_KOTLIN.md's own worked examples always show with a space before
        // the `{` -- so this must not fire for Kotlin (was wrongly collapsing `.let { ... }` to
        // `.let{ ... }` before this gate, since `renderKotlinTokens`/`renderTokens` reuses this
        // same shared method for a declaration's initializer tokens).
        if (isPunct(cur, "{") && !lang.isKotlin
                && (prev.type == TokenType.IDENTIFIER || prev.type == TokenType.ANGLE_BRACKET_CLOSE)) {
            return false;
        }
        if (prev.type == TokenType.ANGLE_BRACKET_OPEN || isOp(prev, "::") || isOp(prev, ".") || isOp(prev, "->") || isPunct(prev, "[") || isPunct(prev, "(")) {
            return false;
        }
        return true;
    }

    protected boolean isTightToken(final Token t) {
        if (t.type == TokenType.ANGLE_BRACKET_OPEN || t.type == TokenType.ANGLE_BRACKET_CLOSE) {
            return true;
        }
        if (isPunct(t, ",") || isPunct(t, "[") || isPunct(t, "]") || isPunct(t, ")")) {
            return true;
        }
        // Kotlin's bare `?` (type nullability suffix, e.g. `Int?`) is always tight against the
        // preceding type token -- see MiscRule.isTightToken's identical, already-established
        // reasoning for this same rule (STYLE_KOTLIN.md's `Type?` rendering via
        // KotlinDeclarationAlignmentRule.renderKotlinTokens, which reuses this method).
        if (lang.isKotlin && isOp(t, "?")) {
            return true;
        }
        // Token.isRepOp(t, '&') matches ANY run of `&` characters, including Kotlin's `&&`
        // logical-AND operator -- it was written for C/C++'s repeated pointer/reference
        // operators (`**`, `&&` as an rvalue-reference declarator), which don't exist in
        // Kotlin. Without this gate, `val x = a && b` loses its space before `&&` (rendered
        // as `a&& b`) because isTightToken wrongly treats `&&` as tight. Kotlin has no
        // unary/repeated `*`/`&` construct at all, so both checks are gated to non-Kotlin.
        return (!lang.isKotlin && (Token.isRepOp(t, '*') || Token.isRepOp(t, '&')))
                || isOp(t, "::") || isOp(t, ".") || isOp(t, "->");
    }

    /** Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     *  (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change. */
    protected List<List<Token>> splitStatements(final List<Token> scopeTokens) {
        final List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        final int n = scopeTokens.size();
        int depth = 0;
        int idx = 0;

        while (idx < n) {
            final Token t = scopeTokens.get(idx);
            current.add(t);
            idx++;

            // A preprocessor directive reached at top level must always start a fresh statement,
            // even mid-run: a semicolon-less macro-invocation "statement" (e.g. a bare pragma-push
            // macro call, no trailing `;`) otherwise never closes `current`, so a `#if`/`#endif`
            // between it and the next real `;`-terminated statement gets folded into the *middle*
            // of that statement's token list -- invisible to `parseDeclaration`'s field-based
            // reconstruction, which silently drops any token it doesn't recognize as part of a
            // declaration's shape (real compile-breaking bug: an `#if` disappearing while its
            // paired `#endif`, now the next statement's own leading token, survives -- see
            // real_code_regressions_34). Back the just-appended directive token out, close out any
            // real accumulated content as its own statement first, then let it lead a new one --
            // consistent with `hasCommentBefore`'s already-established leading-directive handling.
            if (depth == 0 && (t.type == TokenType.PREPROCESSOR || t.type == TokenType.MACRO_DEF)) {
                current.remove(current.size() - 1);
                if (!significantOnly(current).isEmpty()) {
                    statements.add(current);
                    current = new ArrayList<>();
                }
                current.add(t);
                continue;
            }

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
                    // Peek ahead: if the next significant token is `;` this `}` closes a
                    // brace-initializer inside a declaration (e.g. `auto x = T{...};`), not
                    // a method/class body — let the `;` emit the statement instead.
                    boolean nextIsSemi = false;
                    for (int peek = idx; peek < n; peek++) {
                        final Token nx = scopeTokens.get(peek);
                        if (nx.type == TokenType.WHITESPACE || nx.type == TokenType.NEWLINE
                                || nx.type == TokenType.COMMENT_LINE
                                || nx.type == TokenType.COMMENT_BLOCK) {
                            continue;
                        }
                        nextIsSemi = isPunct(nx, ";");
                        break;
                    }
                    if (!nextIsSemi) {
                        idx = pullTrailingSameLine(scopeTokens, current, idx);
                        statements.add(current);
                        current = new ArrayList<>();
                    }
                }
                continue;
            }

            if (depth == 0 && t.type == TokenType.OP && ":".equals(t.text)
                    && isAccessSpecifierColon(current)) {
                idx = pullTrailingSameLine(scopeTokens, current, idx);
                statements.add(current);
                current = new ArrayList<>();
                continue;
            }
            if (depth == 0 && t.type == TokenType.PUNCT && ";".equals(t.text)) {
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

    /** Pulls a same-line trailing comment after a just-closed statement so it stays attached
     *  to that statement instead of becoming the next statement's leading token -- ported from
     *  {@code MiscRule.splitAssignmentStatements}'s identical depth-aware splitting algorithm
     *  (see STATE.md "`DeclarationAlignmentRule.splitStatements` depth-awareness fix"). */
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

    /** True iff {@code current} contains exactly one significant non-gap token followed by {@code :}
     *  and that token is {@code public}, {@code private}, or {@code protected} -- i.e. this `:` is
     *  a C++ access-specifier label boundary, not a ternary or bitfield colon. */
    private boolean isAccessSpecifierColon(final List<Token> current) {
        final List<Token> sig = significantOnly(current);
        if (sig.size() != 2) {
            return false;
        }
        final Token kw = sig.get(0);
        final Token col = sig.get(1);
        return kw.type == TokenType.KEYWORD
                && ("public".equals(kw.text) || "private".equals(kw.text) || "protected".equals(kw.text))
                && col.type == TokenType.OP && ":".equals(col.text);
    }

    /** Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     *  (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change. */
    protected boolean hasCommentBefore(final List<Token> stmt) {
        for (final Token t : stmt) {
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK
                    || t.type == TokenType.PREPROCESSOR || t.type == TokenType.MACRO_DEF) {
                return true;
            }
            if (t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) {
                break;
            }
        }
        return false;
    }

    /** Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     *  (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change. */
    protected boolean hasBlankLineBefore(final List<Token> stmt) {
        int newlineRun = 0;
        for (final Token t : stmt) {
            if (t.type == TokenType.NEWLINE) {
                newlineRun++;
                if (newlineRun >= 2) {
                    return true;
                }
            } else if (t.type == TokenType.WHITESPACE) {
                // ignore -- doesn't break or extend the newline run
            } else if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK
                    || t.type == TokenType.PREPROCESSOR || t.type == TokenType.MACRO_DEF) {
                newlineRun = 0; // a comment/preprocessor line consumes that line's content slot
            } else {
                break;
            }
        }
        return false;
    }

    /** Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     *  (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change. */
    protected int lastSignificantIdx(final List<Token> tokens, final int from, final int to) {
        for (int k = to - 1; k >= from; k--) {
            if (!isGapToken(tokens.get(k))) {
                return k;
            }
        }
        return -1;
    }

    /** Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     *  (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change. */
    protected Token findTrailingComment(final List<Token> stmt) {
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

    /** Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     *  (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change. */
    protected List<Token> significantOnly(final List<Token> stmt) {
        final List<Token> sig = new ArrayList<>();
        for (final Token t : stmt) {
            switch (t.type) {
                case WHITESPACE:
                case NEWLINE:
                case COMMENT_LINE:
                case COMMENT_BLOCK:
                case PREPROCESSOR:
                case MACRO_DEF:
                    continue;
                default:
                    sig.add(t);
            }
        }
        return sig;
    }

}
