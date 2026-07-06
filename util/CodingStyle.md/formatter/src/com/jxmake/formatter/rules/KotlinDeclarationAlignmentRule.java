/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.grid.KotlinModifierPriority;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * STYLE_KOTLIN.md §6: variable/property declaration column alignment. Extends
 * {@link DeclarationAlignmentRule} to reuse its language-agnostic statement-splitting/
 * grouping-break infrastructure ({@code splitStatements}, {@code hasBlankLineBefore},
 * {@code hasCommentBefore}, {@code significantOnly}, {@code renderTokens},
 * {@code findTrailingComment} -- each raised private -> protected in the base class for this
 * purpose, RDD_KEY_103), but not its {@code Declaration} parsing/rendering, which is hard-baked
 * to C/Java's {@code [modifiers] Type name [= init]} token order. Kotlin's
 * {@code [modifiers] val/var name [: type] [= init]} grammar is fundamentally reversed (name
 * before type), so §6 gets its own declaration model, parser, and grid-rendering method here.
 */
public class KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule {

    private final KotlinModifierPriority modifierPriority = new KotlinModifierPriority();

    public KotlinDeclarationAlignmentRule(final Lang lang) {
        super(lang);
    }

    public KotlinDeclarationAlignmentRule(final Lang lang, final int lineLengthLimit) {
        super(lang, lineLengthLimit);
    }

    /** One parsed `val`/`var` property or local-variable declaration. */
    public static final class KotlinDecl {
        public final List<Token> modifiers; // includes the val/var keyword itself (shared slot 5)
        public final Token name;
        public final List<Token> typeTokens; // empty if the type is omitted (inferred)
        public final List<Token> initTokens; // empty if there's no initializer
        public final Token trailingComment; // nullable

        KotlinDecl(final List<Token> modifiers, final Token name, final List<Token> typeTokens,
                final List<Token> initTokens, final Token trailingComment) {
            this.modifiers = modifiers;
            this.name = name;
            this.typeTokens = typeTokens;
            this.initTokens = initTokens;
            this.trailingComment = trailingComment;
        }
    }

    /**
     * Splits one scope's direct-content tokens into groups of consecutive `val`/`var`
     * declaration statements -- same grouping-break rule as the base class's
     * {@code groupDeclarations} (STYLE.md §5): a blank line, a standalone leading comment, or
     * any statement that doesn't parse as a `val`/`var` declaration breaks the current group.
     */
    public List<List<KotlinDecl>> groupPropertyDeclarations(final List<Token> scopeTokens) {
        final List<List<Token>> statements = splitKotlinStatements(scopeTokens);
        final List<List<KotlinDecl>> groups = new ArrayList<>();
        List<KotlinDecl> current = new ArrayList<>();

        for (final List<Token> stmt : statements) {
            final KotlinDecl decl = parseKotlinDeclaration(stmt);
            if (decl == null) {
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
            current.add(decl);
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    /**
     * The base class's {@code splitStatements} only splits on a top-level `;` or a `}` that
     * closes a brace-initializer -- correct for C/C++/Java, where every statement is
     * `;`-terminated, but Kotlin properties are conventionally newline-terminated with no `;`
     * at all, so reusing it verbatim would merge every property in a scope into one giant
     * "statement". This splits at every top-level (depth 0, outside any `(`/`[`/`{`) NEWLINE
     * that follows at least one significant token since the last split, in addition to `;` --
     * one declaration per line, matching every STYLE_KOTLIN.md §6 worked example. A declaration
     * whose initializer genuinely spans multiple physical lines is not the focus of any
     * worked example; it will end up split mid-expression and {@link #parseKotlinDeclaration}
     * will simply fail to parse it (leftover unconsumed tokens), so that entry is conservatively
     * left out of any alignment group rather than mis-aligned -- never guess past an
     * unrecognized shape, same posture as the rest of this class.
     */
    private List<List<Token>> splitKotlinStatements(final List<Token> scopeTokens) {
        final List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int depth = 0;
        boolean sawSignificant = false;

        for (final Token t : scopeTokens) {
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
                current.add(t);
                sawSignificant = true;
                continue;
            }
            if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                depth--;
                current.add(t);
                sawSignificant = true;
                continue;
            }
            if (depth == 0 && isPunct(t, ";")) {
                current.add(t);
                statements.add(current);
                current = new ArrayList<>();
                sawSignificant = false;
                continue;
            }
            if (depth == 0 && t.type == TokenType.NEWLINE && sawSignificant) {
                statements.add(current);
                current = new ArrayList<>();
                current.add(t); // this newline becomes the next statement's leading gap
                sawSignificant = false;
                continue;
            }
            current.add(t);
            if (!isGapToken(t)) {
                sawSignificant = true;
            }
        }
        if (!current.isEmpty()) {
            statements.add(current);
        }
        return statements;
    }

    /** Parses one statement's tokens as `[modifiers] val|var name [: type] [= init]`, or
     *  returns null if it doesn't match this shape -- any other statement (a function call,
     *  control-flow, a function/class declaration, an annotation-prefixed property, etc.)
     *  breaks the group, same conservative "don't guess past an unrecognized shape" posture
     *  used throughout this codebase, rather than the base class's much broader C/Java grammar. */
    private KotlinDecl parseKotlinDeclaration(final List<Token> stmt) {
        final List<Token> sig = significantOnly(stmt);
        if (sig.isEmpty()) {
            return null;
        }

        int i = 0;
        final List<Token> modifiers = new ArrayList<>();
        while (i < sig.size() && isPlainModifier(sig.get(i))) {
            modifiers.add(sig.get(i));
            i++;
        }
        if (i >= sig.size() || !isValOrVar(sig.get(i))) {
            return null;
        }
        modifiers.add(sig.get(i)); // val/var itself -- KotlinModifierPriority's shared slot 5
        i++;

        if (i >= sig.size() || sig.get(i).type != TokenType.IDENTIFIER) {
            return null;
        }
        final Token name = sig.get(i);
        i++;

        List<Token> typeTokens = new ArrayList<>();
        if (i < sig.size() && isOp(sig.get(i), ":")) {
            i++;
            final int typeStart = i;
            while (i < sig.size() && !isOp(sig.get(i), "=")) {
                i++;
            }
            typeTokens = sig.subList(typeStart, i);
        }

        List<Token> initTokens = new ArrayList<>();
        if (i < sig.size() && isOp(sig.get(i), "=")) {
            i++;
            initTokens = sig.subList(i, sig.size());
            i = sig.size();
        }

        if (i != sig.size()) {
            return null; // trailing tokens this parser doesn't understand -- never guess
        }
        return new KotlinDecl(modifiers, name, typeTokens, initTokens, findTrailingComment(stmt));
    }

    private boolean isPlainModifier(final Token t) {
        return modifierPriority.isModifier(t.text) && !isValOrVar(t);
    }

    private boolean isValOrVar(final Token t) {
        return "val".equals(t.text) || "var".equals(t.text);
    }

    /**
     * Renders one group of consecutive {@link KotlinDecl}s into a STYLE_KOTLIN.md §6 column
     * grid: {@code [visibility] [modality/override/const/lateinit] [val|var] name [: type]
     * [= init]}, each its own {@link ColumnGrid} column so alignment (including the `:` column
     * lining up detached from the variable name) falls out of the grid automatically -- unlike
     * the base class's C/C++ {@code render()}, which must manually pre-pad a combined name+init
     * cell because bitfield/structured-binding shapes there force name and init to share one
     * cell; Kotlin's simpler grammar doesn't need that. A modifier column, or the type/init
     * column, is only emitted at all if some declaration in the group actually uses it -- same
     * "only emit active columns" precedent as the base class's {@code modifierActive} array.
     */
    public List<String> renderPropertyGroup(final List<KotlinDecl> group) {
        final int modifierColumns = modifierPriority.columnCount();
        final boolean[] modifierActive = new boolean[modifierColumns];
        boolean anyType = false;
        boolean anyInit = false;
        for (final KotlinDecl d : group) {
            for (final Token m : d.modifiers) {
                final int rank = modifierPriority.priorityOf(m.text);
                if (rank >= 0) {
                    modifierActive[rank] = true;
                }
            }
            anyType = anyType || !d.typeTokens.isEmpty();
            anyInit = anyInit || !d.initTokens.isEmpty();
        }

        final ColumnGrid grid = new ColumnGrid();
        for (final KotlinDecl d : group) {
            final List<String> cells = new ArrayList<>();
            final String[] modCells = new String[modifierColumns];
            Arrays.fill(modCells, "");
            for (final Token m : d.modifiers) {
                final int rank = modifierPriority.priorityOf(m.text);
                if (rank >= 0) {
                    modCells[rank] = m.text;
                }
            }
            for (int r = 0; r < modifierColumns; r++) {
                if (modifierActive[r]) {
                    cells.add(modCells[r]);
                }
            }
            cells.add(d.name.text);
            if (anyType) {
                cells.add(d.typeTokens.isEmpty() ? "" : ": " + renderKotlinTokens(d.typeTokens));
            }
            if (anyInit) {
                cells.add(d.initTokens.isEmpty() ? "" : "= " + renderKotlinTokens(d.initTokens));
            }
            if (d.trailingComment != null) {
                cells.add(d.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }

        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(trimTrailingSpaces(String.join(" ", row)));
        }
        return lines;
    }

    /**
     * {@code renderTokens} is inherited from the C/C++/Java base class, whose tight-token
     * table (`isTightToken`) has no notion of Kotlin's `?.`/`!!` null-safety operators
     * (STYLE_KOTLIN.md §5, {@code KotlinSpecificRule.enforceNullSafetyOperatorSpacing}) --
     * that pass operates on the whole-file token stream separately and isn't wired into this
     * grid-cell renderer, so `renderTokens` leaves its generic default (a single space) before
     * `?.`/`!!`. This strips exactly that one leftover space so the two passes agree.
     */
    private String renderKotlinTokens(final List<Token> tokens) {
        return renderTokens(tokens)
                .replaceAll("\\s+(?=(\\?\\.|!!))", "")
                .replaceAll("(?<=(\\?\\.|!!))\\s+", "");
    }

    private String trimTrailingSpaces(final String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}
