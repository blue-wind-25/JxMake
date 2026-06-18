/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.grid.CppModifierPriority;
import com.jxmake.formatter.grid.JavaModifierPriority;
import com.jxmake.formatter.grid.ModifierPriority;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeclarationAlignmentRule {

    private static final Set<String> TYPE_KEYWORDS_C = setOf(
            "void", "char", "short", "int", "long", "float", "double", "signed",
            "unsigned", "struct", "enum", "union");

    private static final Set<String> TYPE_KEYWORDS_CPP = union(TYPE_KEYWORDS_C,
            setOf("bool", "wchar_t", "char16_t", "char32_t", "auto", "class"));

    private static final Set<String> TYPE_KEYWORDS_JAVA = setOf(
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void");

    private final String language;
    private final ModifierPriority modifierPriority;
    private final Set<String> typeKeywords;

    public DeclarationAlignmentRule(final String language) {
        this.language = language;
        if ("java".equals(language)) {
            this.modifierPriority = new JavaModifierPriority();
            this.typeKeywords = TYPE_KEYWORDS_JAVA;
        } else {
            this.modifierPriority = new CppModifierPriority();
            this.typeKeywords = "cpp".equals(language) ? TYPE_KEYWORDS_CPP : TYPE_KEYWORDS_C;
        }
    }

    private static Set<String> setOf(final String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    private static Set<String> union(final Set<String> a, final Set<String> b) {
        final Set<String> result = new HashSet<>(a);
        result.addAll(b);
        return result;
    }

    /** One parsed declaration statement. */
    public static final class Declaration {
        public final List<Token> modifiers;
        public final List<Token> typeTokens;
        public final Token name;
        public final List<Token> sizeTokens;
        public final List<Token> initTokens;
        public final Token trailingComment; // nullable
        public final boolean blankLineBefore;

        Declaration(final List<Token> modifiers, final List<Token> typeTokens, final Token name,
                final List<Token> sizeTokens, final List<Token> initTokens,
                final Token trailingComment, final boolean blankLineBefore) {
            this.modifiers = modifiers;
            this.typeTokens = typeTokens;
            this.name = name;
            this.sizeTokens = sizeTokens;
            this.initTokens = initTokens;
            this.trailingComment = trailingComment;
            this.blankLineBefore = blankLineBefore;
        }
    }

    /**
     * Splits one scope's direct-content tokens (already extracted by the caller --
     * e.g. one class/struct body at a fixed braceDepth, with no deeper-nested
     * tokens included) into groups of consecutive declaration statements. A blank
     * line, or any statement not recognized as a variable/field declaration, breaks
     * the current group (STYLE.md §5).
     */
    public List<List<Declaration>> groupDeclarations(final List<Token> scopeTokens) {
        final List<List<Token>> statements = splitStatements(scopeTokens);

        final List<List<Declaration>> groups = new ArrayList<>();
        List<Declaration> current = new ArrayList<>();

        for (final List<Token> stmt : statements) {
            final boolean blankBefore = hasBlankLineBefore(stmt);
            final Declaration decl = parseDeclaration(stmt, blankBefore);

            if (decl == null) {
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
            current.add(decl);
        }

        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    // ── Static reorder safety ───────────────────────────────────────────────────
    /**
     * Moves `static` declarations to the front of the group (STYLE.md §5),
     * except where a preceding non-static is a size/value dependency of a
     * static (its name appears in that static's size or init tokens) -- the
     * whole run of not-yet-placed non-statics accumulated since the last
     * flush is kept immediately before such a static, preserving their
     * relative order, rather than attempting a finer-grained reorder whose
     * safety would be unclear.
     */
    public List<Declaration> reorderStatics(final List<Declaration> group) {
        final List<Declaration> output = new ArrayList<>();
        final List<Declaration> pending = new ArrayList<>();

        for (final Declaration d : group) {
            if (isStatic(d)) {
                if (dependsOnAny(d, pending)) {
                    output.addAll(pending);
                    pending.clear();
                }
                output.add(d);
            } else {
                pending.add(d);
            }
        }
        output.addAll(pending);
        return output;
    }

    private boolean isStatic(final Declaration d) {
        for (final Token m : d.modifiers) {
            if ("static".equals(m.text)) {
                return true;
            }
        }
        return false;
    }

    private boolean dependsOnAny(final Declaration d, final List<Declaration> candidates) {
        for (final Declaration c : candidates) {
            if (referencesName(d.sizeTokens, c.name.text) || referencesName(d.initTokens, c.name.text)) {
                return true;
            }
        }
        return false;
    }

    private boolean referencesName(final List<Token> tokens, final String name) {
        for (final Token t : tokens) {
            if (t.type == TokenType.IDENTIFIER && name.equals(t.text)) {
                return true;
            }
        }
        return false;
    }

    // ── Column grid rendering ───────────────────────────────────────────────────
    /**
     * Renders one declaration group into aligned source lines (STYLE.md §5,
     * STYLE_C_CPP.md §4): fixed modifier columns (only those actually used
     * anywhere in the group), the type (with C/C++ pointer attached), an
     * optional post-pointer `const` column, the name+size+`;`, and an optional
     * trailing comment column. Columns unused by the whole group are omitted
     * rather than rendered as dead padding. Statics are reordered first
     * (see `reorderStatics`).
     */
    public List<String> render(final List<Declaration> originalGroup) {
        final List<Declaration> group = reorderStatics(originalGroup);
        final boolean isJava = "java".equals(language);
        final int modifierColumns = modifierPriority.columnCount();
        final boolean[] modifierActive = new boolean[modifierColumns];
        boolean postConstActive = false;

        final List<TypeSplit> splits = new ArrayList<>(group.size());
        for (final Declaration d : group) {
            for (final Token m : d.modifiers) {
                final int rank = modifierPriority.priorityOf(m.text);
                if (rank >= 0) {
                    modifierActive[rank] = true;
                }
            }
            final TypeSplit split = isJava ? null : splitCppType(d.typeTokens);
            if (split != null && !split.postConst.isEmpty()) {
                postConstActive = true;
            }
            splits.add(split);
        }

        final ColumnGrid grid = new ColumnGrid();
        for (int idx = 0; idx < group.size(); idx++) {
            final Declaration d = group.get(idx);
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

            if (isJava) {
                cells.add(renderTokens(d.typeTokens));
            } else {
                final TypeSplit split = splits.get(idx);
                cells.add(split.typeAndStar);
                if (postConstActive) {
                    cells.add(split.postConst);
                }
            }

            cells.add(renderNameCell(d));

            if (d.trailingComment != null) {
                cells.add(d.trailingComment.text);
            }

            grid.addRow(cells.toArray(new String[0]));
        }

        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(String.join(" ", row));
        }
        return lines;
    }

    private String renderNameCell(final Declaration d) {
        final StringBuilder sb = new StringBuilder();
        sb.append(d.name.text);
        sb.append(renderTokens(d.sizeTokens));
        if (!d.initTokens.isEmpty()) {
            sb.append(" = ").append(renderTokens(d.initTokens));
        }
        sb.append(";");
        return sb.toString();
    }

    /** Splits C/C++ type tokens into the base-type(+pointer) cell and an optional post-pointer `const` cell. */
    private static final class TypeSplit {
        final String typeAndStar;
        final String postConst;

        TypeSplit(final String typeAndStar, final String postConst) {
            this.typeAndStar = typeAndStar;
            this.postConst = postConst;
        }
    }

    private TypeSplit splitCppType(final List<Token> typeTokens) {
        List<Token> tokens = typeTokens;
        String postConst = "";
        final int n = tokens.size();
        if (n >= 2) {
            final Token last = tokens.get(n - 1);
            final Token secondLast = tokens.get(n - 2);
            if (last.type == TokenType.KEYWORD && "const".equals(last.text)
                    && isOp(secondLast, "*")) {
                postConst = "const";
                tokens = tokens.subList(0, n - 1);
            }
        }
        return new TypeSplit(renderTokens(tokens), postConst);
    }

    /**
     * Joins tokens into canonical spaced text: `*`/`&`/`::`/generics/`[`/`]`/`,`
     * attach tightly per STYLE_C_CPP.md §4 conventions (e.g. `uint8_t*`,
     * `std::vector<int>`, `buffer[64]`); everything else gets a single space.
     */
    private String renderTokens(final List<Token> tokens) {
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

    private boolean needsSpaceBetween(final Token prev, final Token cur) {
        if (isTightToken(cur)) {
            return false;
        }
        if (prev.type == TokenType.ANGLE_BRACKET_OPEN || isOp(prev, "::") || isPunct(prev, "[")) {
            return false;
        }
        return true;
    }

    private boolean isTightToken(final Token t) {
        if (t.type == TokenType.ANGLE_BRACKET_OPEN || t.type == TokenType.ANGLE_BRACKET_CLOSE) {
            return true;
        }
        if (isPunct(t, ",") || isPunct(t, "[") || isPunct(t, "]")) {
            return true;
        }
        return isOp(t, "*") || isOp(t, "&") || isOp(t, "::");
    }

    // ── Statement splitting ─────────────────────────────────────────────────────
    private List<List<Token>> splitStatements(final List<Token> scopeTokens) {
        final List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        final int n = scopeTokens.size();
        int idx = 0;

        while (idx < n) {
            final Token t = scopeTokens.get(idx);
            current.add(t);
            idx++;

            if (t.type == TokenType.PUNCT && ";".equals(t.text)) {
                // Pull in a same-line trailing comment so it stays with this
                // statement instead of becoming the next statement's leading token.
                while (idx < n) {
                    final Token next = scopeTokens.get(idx);
                    if (next.type == TokenType.WHITESPACE || next.type == TokenType.COMMENT_LINE
                            || next.type == TokenType.COMMENT_BLOCK) {
                        current.add(next);
                        idx++;
                    } else {
                        break;
                    }
                }
                statements.add(current);
                current = new ArrayList<>();
            }
        }

        if (!current.isEmpty()) {
            statements.add(current);
        }
        return statements;
    }

    private boolean hasBlankLineBefore(final List<Token> stmt) {
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
                newlineRun = 0; // a comment line consumes that line's content slot
            } else {
                break;
            }
        }
        return false;
    }

    // ── Declaration parsing ──────────────────────────────────────────────────────
    private Declaration parseDeclaration(final List<Token> stmt, final boolean blankBefore) {
        final Token trailingComment = findTrailingComment(stmt);
        final List<Token> sig = significantOnly(stmt);

        if (sig.isEmpty() || !isPunct(sig.get(sig.size() - 1), ";")) {
            return null;
        }
        final List<Token> body = sig.subList(0, sig.size() - 1);
        if (body.isEmpty()) {
            return null;
        }

        int i = 0;
        final List<Token> modifiers = new ArrayList<>();
        while (i < body.size() && body.get(i).type == TokenType.KEYWORD
                && modifierPriority.isModifier(body.get(i).text)) {
            modifiers.add(body.get(i));
            i++;
        }
        if (i >= body.size()) {
            return null;
        }

        int eqIdx = -1;
        for (int j = i; j < body.size(); j++) {
            if (isOp(body.get(j), "=")) {
                eqIdx = j;
                break;
            }
        }
        final List<Token> initTokens;
        final int end;
        if (eqIdx >= 0) {
            initTokens = new ArrayList<>(body.subList(eqIdx + 1, body.size()));
            end = eqIdx;
        } else {
            initTokens = new ArrayList<>();
            end = body.size();
        }

        final List<Token> sizeTokens = new ArrayList<>();
        int sizeEnd = end;
        while (sizeEnd > i && isPunct(body.get(sizeEnd - 1), "]")) {
            int depth = 0;
            int openIdx = -1;
            for (int k = sizeEnd - 1; k >= i; k--) {
                final Token t = body.get(k);
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
                break; // unbalanced -- bail, don't touch this statement
            }
            sizeTokens.addAll(0, body.subList(openIdx, sizeEnd));
            sizeEnd = openIdx;
        }

        if (sizeEnd <= i) {
            return null;
        }
        final Token name = body.get(sizeEnd - 1);
        if (name.type != TokenType.IDENTIFIER) {
            return null;
        }
        final List<Token> typeTokens = new ArrayList<>(body.subList(i, sizeEnd - 1));
        if (typeTokens.isEmpty()) {
            return null;
        }
        final Token firstType = typeTokens.get(0);
        if (firstType.type == TokenType.KEYWORD && !typeKeywords.contains(firstType.text)) {
            return null; // e.g. return/throw/assert/break -- not a declaration
        }

        return new Declaration(modifiers, typeTokens, name, sizeTokens, initTokens,
                trailingComment, blankBefore);
    }

    private Token findTrailingComment(final List<Token> stmt) {
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

    private List<Token> significantOnly(final List<Token> stmt) {
        final List<Token> sig = new ArrayList<>();
        for (final Token t : stmt) {
            switch (t.type) {
                case WHITESPACE:
                case NEWLINE:
                case COMMENT_LINE:
                case COMMENT_BLOCK:
                    continue;
                default:
                    sig.add(t);
            }
        }
        return sig;
    }

    private boolean isPunct(final Token t, final String text) {
        return t.type == TokenType.PUNCT && text.equals(t.text);
    }

    private boolean isOp(final Token t, final String text) {
        return t.type == TokenType.OP && text.equals(t.text);
    }
}
