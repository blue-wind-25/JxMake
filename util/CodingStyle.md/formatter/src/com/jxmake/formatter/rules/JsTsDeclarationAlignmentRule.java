/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.List;

/**
 * STYLE_JS_TS.md §11 declaration-alignment-grid support for `let`/`const`/`var` declarations.
 * Extends {@link DeclarationAlignmentRuleCurly} to reuse its language-agnostic statement-
 * splitting/grouping-break infrastructure ({@code splitStatements}, {@code hasBlankLineBefore},
 * {@code hasCommentBefore}, {@code significantOnly}, {@code renderTokens}, {@code
 * findTrailingComment}, RDD_KEY_103's precedent), but not its {@code Declaration} parsing/
 * rendering, which is hard-baked to C/Java's {@code [modifiers] Type name [= init]} token order
 * -- JS/TS's `let x: Type = value` is name-before-type, the same reversed grammar Kotlin's
 * {@code val x: Type = value} already solved (RDD_KEY_103/104), so this class mirrors {@link
 * KotlinDeclarationAlignmentRule}'s structural shape rather than extending the base class's own
 * parser (the base's {@code parseDeclaration} is {@code private} and its index arithmetic
 * unconditionally takes the *last* token before size/params as the name -- no seam to inject a
 * reversed-grammar branch, same finding Kotlin's own class doc already recorded).
 *
 * <p>Unlike Kotlin, JS/TS statements are always `;`-terminated (STYLE_JS_TS.md §2 -- ASI is
 * never relied upon), so this class reuses the base class's own {@code splitStatements} directly
 * instead of Kotlin's newline-based {@code splitKotlinStatements}. JS/TS also has no modifier
 * table for local `let`/`const`/`var` declarations (unlike Kotlin's `KotlinModifierPriority` or
 * TS's own separate class-field modifier table, §11.2, already handled by {@code
 * JsTsSpecificRule.reorderClassFieldModifiers} as a flat pass, not this class), so the rendered
 * grid here has no modifier columns at all -- just keyword, name, optional `:` type (TS only),
 * optional `=` init, optional trailing comment.
 *
 * <p><b>Deliberately out of scope this checkpoint (see STATE_JS_TS.md for the full note):</b>
 * destructuring-pattern LHS (`const { a, b } = obj;`, `const [x, y] = arr;`, RDD_KEY_182) and
 * multi-declarator statements (`let a = 1, b = 2;`) both bail out of {@link #parseDeclaration}
 * (return null) rather than being parsed -- they are left completely untouched by this class, the
 * same as before this checkpoint, so the pre-existing Checkpoint 5/6 destructuring-space/bogus-
 * semicolon fixes (which operate independently, via {@code MiscRuleCore.needsSpaceBetween}/
 * {@code JsTsSpecificRule.classifyBraces}, never through this class) are not at risk of
 * regression. `type X = ...` alias groups (RDD_KEY_183) are a separate future extension too --
 * `type` is not recognized as a declaration keyword here at all.
 */
public class JsTsDeclarationAlignmentRule extends DeclarationAlignmentRuleCurly {

    public JsTsDeclarationAlignmentRule(final Lang lang, final int lineLengthLimit) {
        super(lang, lineLengthLimit);
    }

    /** One parsed `let`/`const`/`var` declaration. */
    public static final class Row {
        public final Token keyword; // let/const/var itself
        public final Token name;
        public final List<Token> typeTokens; // TS only; always empty for plain JS
        public final List<Token> initTokens; // empty if there's no initializer
        public final Token trailingComment; // nullable
        public final Token lastAnchor; // splice-back end (inclusive)

        Row(final Token keyword, final Token name, final List<Token> typeTokens,
                final List<Token> initTokens, final Token trailingComment, final Token lastAnchor) {
            this.keyword = keyword;
            this.name = name;
            this.typeTokens = typeTokens;
            this.initTokens = initTokens;
            this.trailingComment = trailingComment;
            this.lastAnchor = lastAnchor;
        }
    }

    private boolean isDeclKeyword(final Token t) {
        return t.type == TokenType.KEYWORD
                && ("let".equals(t.text) || "const".equals(t.text) || "var".equals(t.text));
    }

    /**
     * Parses one statement's tokens as {@code let|const|var name [: type] [= init] ;}, or
     * returns null if it doesn't match this shape -- any other statement (destructuring LHS,
     * multi-declarator, a function call, control-flow, a class/function declaration, etc.)
     * breaks the group, same conservative "don't guess past an unrecognized shape" posture as
     * {@code KotlinDeclarationAlignmentRule.parseKotlinDeclaration}.
     */
    private Row parseDeclaration(final List<Token> stmt) {
        final List<Token> sig = significantOnly(stmt);
        if (sig.isEmpty() || !isDeclKeyword(sig.get(0))) {
            return null;
        }
        final Token keyword = sig.get(0);
        int i = 1;

        // Only a plain single identifier declarator is handled here -- a destructuring pattern
        // (`{`/`[` right after the keyword) is deliberately left unparsed, see class doc.
        if (i >= sig.size() || sig.get(i).type != TokenType.IDENTIFIER) {
            return null;
        }
        final Token name = sig.get(i);
        i++;

        List<Token> typeTokens = new ArrayList<>();
        if (lang.isTs && i < sig.size() && isOp(sig.get(i), ":")) {
            i++;
            final int typeStart = i;
            int depth = 0;
            while (i < sig.size()) {
                final Token t = sig.get(i);
                if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                    depth++;
                } else if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                    depth--;
                } else if (depth == 0 && (isOp(t, "=") || isPunct(t, ";") || isPunct(t, ","))) {
                    break;
                }
                i++;
            }
            if (i < sig.size() && isPunct(sig.get(i), ",")) {
                return null; // multi-declarator with a type -- not this checkpoint's scope
            }
            typeTokens = sig.subList(typeStart, i);
        }

        List<Token> initTokens = new ArrayList<>();
        if (i < sig.size() && isOp(sig.get(i), "=")) {
            final Token eqToken = sig.get(i);
            i++;
            final int initStart = i;
            int depth = 0;
            while (i < sig.size()) {
                final Token t = sig.get(i);
                if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                    depth++;
                } else if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                    depth--;
                } else if (depth == 0 && (isPunct(t, ";") || isPunct(t, ","))) {
                    break;
                }
                i++;
            }
            if (i < sig.size() && isPunct(sig.get(i), ",")) {
                return null; // multi-declarator -- not this checkpoint's scope
            }
            initTokens = sig.subList(initStart, i);
            if (!initTokens.isEmpty() && spansMultipleLines(stmt, eqToken)) {
                return null;
            }
            if (hasCommentAfter(stmt, eqToken)) {
                return null;
            }
        }

        if (i >= sig.size() || !isPunct(sig.get(i), ";")) {
            return null; // not the plain `;`-terminated shape this parser understands
        }
        final Token semi = sig.get(i);
        i++;
        if (i != sig.size()) {
            return null; // trailing tokens this parser doesn't understand -- never guess
        }

        final Token trailingComment = findTrailingComment(stmt);
        final Token lastAnchor = trailingComment != null ? trailingComment : semi;
        return new Row(keyword, name, typeTokens, initTokens, trailingComment, lastAnchor);
    }

    /** Same "embedded comment inside the initializer would be silently dropped" bailout as
     *  {@code KotlinDeclarationAlignmentRule.hasCommentAfter} -- a trailing end-of-line comment
     *  (after {@code stmt}'s own last significant token) is excluded, since that one is already
     *  carried separately via {@link #findTrailingComment}. */
    private boolean hasCommentAfter(final List<Token> stmt, final Token afterToken) {
        int lastSigIdx = -1;
        for (int k = 0; k < stmt.size(); k++) {
            if (!isGapToken(stmt.get(k))) {
                lastSigIdx = k;
            }
        }
        boolean seen = false;
        for (int k = 0; k < stmt.size(); k++) {
            final Token t = stmt.get(k);
            if (seen && k < lastSigIdx
                    && (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK)) {
                return true;
            }
            if (t == afterToken) {
                seen = true;
            }
        }
        return false;
    }

    /** Same "never flatten a genuine multi-line block/lambda initializer onto one line" bailout
     *  as {@code KotlinDeclarationAlignmentRule.spansMultipleLines}, including its paren-depth
     *  carve-out for a brace-free initializer whose own nested call argument list was wrapped
     *  across lines by a previous {@code MiscRuleCurly.enforceCallLineBreaking} pass. */
    private boolean spansMultipleLines(final List<Token> stmt, final Token afterToken) {
        boolean seen = false;
        int parenDepth = 0;
        int braceDepth = 0;
        for (final Token t : stmt) {
            if (seen) {
                if (isPunct(t, "(") || isPunct(t, "[")) {
                    parenDepth++;
                } else if (isPunct(t, ")") || isPunct(t, "]")) {
                    parenDepth--;
                } else if (isPunct(t, "{")) {
                    braceDepth++;
                } else if (isPunct(t, "}")) {
                    braceDepth--;
                } else if (t.type == TokenType.NEWLINE && (braceDepth > 0 || (parenDepth == 0 && braceDepth == 0))) {
                    return true;
                }
            }
            if (t == afterToken) {
                seen = true;
            }
        }
        return false;
    }

    /**
     * Splits one scope's direct-content tokens into groups of consecutive `let`/`const`/`var`
     * declaration statements -- same grouping-break rule as the base class's {@code
     * groupDeclarations} (STYLE.md §5)/Kotlin's {@code groupAlignableDeclarations}: a blank line,
     * a standalone leading comment, or any statement that doesn't parse as a plain declaration
     * (per this class's deliberately narrow {@link #parseDeclaration}) breaks the current group.
     */
    public List<List<Row>> groupAlignableDeclarations(final List<Token> scopeTokens) {
        final List<List<Token>> statements = splitStatements(scopeTokens);
        final List<List<Row>> groups = new ArrayList<>();
        List<Row> current = new ArrayList<>();

        for (final List<Token> stmt : statements) {
            final Row row = parseDeclaration(stmt);
            if (row == null) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            final boolean breakBefore = hasBlankLineBefore(stmt) || hasCommentBefore(stmt);
            if (breakBefore && !current.isEmpty()) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(row);
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    /**
     * Renders one group of consecutive {@link Row}s into a STYLE_JS_TS.md §11 column grid:
     * {@code let|const|var name [: type] = init;}, each its own {@link ColumnGrid} column so
     * both the `:` and `=` columns align "for free" via the grid's own per-column max-width
     * padding -- same shape as {@code KotlinDeclarationAlignmentRule.renderAlignedGroupRaw}. A
     * column (`:` type, `=` init, trailing comment) is only emitted at all if some row in the
     * group actually uses it, same "only emit active columns" precedent used throughout this
     * codebase's grid renderers.
     */
    public List<String> renderAlignedGroup(final List<Row> group) {
        boolean anyType = false;
        boolean anyInit = false;
        for (final Row r : group) {
            anyType = anyType || !r.typeTokens.isEmpty();
            anyInit = anyInit || !r.initTokens.isEmpty();
        }

        final ColumnGrid grid = new ColumnGrid();
        for (final Row r : group) {
            final List<String> cells = new ArrayList<>();
            cells.add(r.keyword.text);
            cells.add(r.name.text);
            if (anyType) {
                cells.add(r.typeTokens.isEmpty() ? "" : ": " + renderTokens(r.typeTokens));
            }
            if (anyInit) {
                cells.add(r.initTokens.isEmpty() ? "" : "= " + renderTokens(r.initTokens));
            }
            // Attach the statement-terminating `;` directly to this row's own last non-empty
            // cell (name, or type/init if present) -- never as its own separately-joined cell,
            // which would leave a stray space before it (`1 ;` instead of `1;`) once ColumnGrid
            // joins cells with a single space.
            int lastNonEmpty = cells.size() - 1;
            while (lastNonEmpty > 0 && cells.get(lastNonEmpty).isEmpty()) {
                lastNonEmpty--;
            }
            cells.set(lastNonEmpty, cells.get(lastNonEmpty) + ";");
            if (r.trailingComment != null) {
                cells.add(r.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }

        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(trimTrailingSpaces(String.join(" ", row)));
        }
        return lines;
    }

    private String trimTrailingSpaces(final String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }

}
