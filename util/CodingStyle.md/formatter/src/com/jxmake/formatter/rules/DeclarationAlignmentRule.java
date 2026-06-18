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

    public DeclarationAlignmentRule(String language) {
        this.language = language;
        if ("java".equals(language)) {
            this.modifierPriority = new JavaModifierPriority();
            this.typeKeywords = TYPE_KEYWORDS_JAVA;
        } else {
            this.modifierPriority = new CppModifierPriority();
            this.typeKeywords = "cpp".equals(language) ? TYPE_KEYWORDS_CPP : TYPE_KEYWORDS_C;
        }
    }

    private static Set<String> setOf(String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    private static Set<String> union(Set<String> a, Set<String> b) {
        Set<String> result = new HashSet<>(a);
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

        Declaration(List<Token> modifiers, List<Token> typeTokens, Token name,
                List<Token> sizeTokens, List<Token> initTokens, Token trailingComment,
                boolean blankLineBefore) {
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
    public List<List<Declaration>> groupDeclarations(List<Token> scopeTokens) {
        List<List<Token>> statements = splitStatements(scopeTokens);

        List<List<Declaration>> groups = new ArrayList<>();
        List<Declaration> current = new ArrayList<>();

        for (List<Token> stmt : statements) {
            boolean blankBefore = hasBlankLineBefore(stmt);
            Declaration decl = parseDeclaration(stmt, blankBefore);

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
    public List<Declaration> reorderStatics(List<Declaration> group) {
        List<Declaration> output = new ArrayList<>();
        List<Declaration> pending = new ArrayList<>();

        for (Declaration d : group) {
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

    private boolean isStatic(Declaration d) {
        for (Token m : d.modifiers) {
            if ("static".equals(m.text)) {
                return true;
            }
        }
        return false;
    }

    private boolean dependsOnAny(Declaration d, List<Declaration> candidates) {
        for (Declaration c : candidates) {
            if (referencesName(d.sizeTokens, c.name.text) || referencesName(d.initTokens, c.name.text)) {
                return true;
            }
        }
        return false;
    }

    private boolean referencesName(List<Token> tokens, String name) {
        for (Token t : tokens) {
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
    public List<String> render(List<Declaration> originalGroup) {
        List<Declaration> group = reorderStatics(originalGroup);
        boolean isJava = "java".equals(language);
        int modifierColumns = modifierPriority.columnCount();
        boolean[] modifierActive = new boolean[modifierColumns];
        boolean postConstActive = false;

        List<TypeSplit> splits = new ArrayList<>(group.size());
        for (Declaration d : group) {
            for (Token m : d.modifiers) {
                int rank = modifierPriority.priorityOf(m.text);
                if (rank >= 0) {
                    modifierActive[rank] = true;
                }
            }
            TypeSplit split = isJava ? null : splitCppType(d.typeTokens);
            if (split != null && !split.postConst.isEmpty()) {
                postConstActive = true;
            }
            splits.add(split);
        }

        ColumnGrid grid = new ColumnGrid();
        for (int idx = 0; idx < group.size(); idx++) {
            Declaration d = group.get(idx);
            List<String> cells = new ArrayList<>();

            String[] modCells = new String[modifierColumns];
            Arrays.fill(modCells, "");
            for (Token m : d.modifiers) {
                int rank = modifierPriority.priorityOf(m.text);
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
                TypeSplit split = splits.get(idx);
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

        List<String> lines = new ArrayList<>();
        for (String[] row : grid.flush()) {
            lines.add(String.join(" ", row));
        }
        return lines;
    }

    private String renderNameCell(Declaration d) {
        StringBuilder sb = new StringBuilder();
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

        TypeSplit(String typeAndStar, String postConst) {
            this.typeAndStar = typeAndStar;
            this.postConst = postConst;
        }
    }

    private TypeSplit splitCppType(List<Token> typeTokens) {
        List<Token> tokens = typeTokens;
        String postConst = "";
        int n = tokens.size();
        if (n >= 2) {
            Token last = tokens.get(n - 1);
            Token secondLast = tokens.get(n - 2);
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
    private String renderTokens(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        Token prev = null;
        for (Token t : tokens) {
            if (prev != null && needsSpaceBetween(prev, t)) {
                sb.append(' ');
            }
            sb.append(t.text);
            prev = t;
        }
        return sb.toString();
    }

    private boolean needsSpaceBetween(Token prev, Token cur) {
        if (isTightToken(cur)) {
            return false;
        }
        if (prev.type == TokenType.ANGLE_BRACKET_OPEN || isOp(prev, "::") || isPunct(prev, "[")) {
            return false;
        }
        return true;
    }

    private boolean isTightToken(Token t) {
        if (t.type == TokenType.ANGLE_BRACKET_OPEN || t.type == TokenType.ANGLE_BRACKET_CLOSE) {
            return true;
        }
        if (isPunct(t, ",") || isPunct(t, "[") || isPunct(t, "]")) {
            return true;
        }
        return isOp(t, "*") || isOp(t, "&") || isOp(t, "::");
    }

    // ── Statement splitting ─────────────────────────────────────────────────────
    private List<List<Token>> splitStatements(List<Token> scopeTokens) {
        List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int n = scopeTokens.size();
        int idx = 0;

        while (idx < n) {
            Token t = scopeTokens.get(idx);
            current.add(t);
            idx++;

            if (t.type == TokenType.PUNCT && ";".equals(t.text)) {
                // Pull in a same-line trailing comment so it stays with this
                // statement instead of becoming the next statement's leading token.
                while (idx < n) {
                    Token next = scopeTokens.get(idx);
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

    private boolean hasBlankLineBefore(List<Token> stmt) {
        int newlineRun = 0;
        for (Token t : stmt) {
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
    private Declaration parseDeclaration(List<Token> stmt, boolean blankBefore) {
        Token trailingComment = findTrailingComment(stmt);
        List<Token> sig = significantOnly(stmt);

        if (sig.isEmpty() || !isPunct(sig.get(sig.size() - 1), ";")) {
            return null;
        }
        List<Token> body = sig.subList(0, sig.size() - 1);
        if (body.isEmpty()) {
            return null;
        }

        int i = 0;
        List<Token> modifiers = new ArrayList<>();
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
        List<Token> initTokens;
        int end;
        if (eqIdx >= 0) {
            initTokens = new ArrayList<>(body.subList(eqIdx + 1, body.size()));
            end = eqIdx;
        } else {
            initTokens = new ArrayList<>();
            end = body.size();
        }

        List<Token> sizeTokens = new ArrayList<>();
        while (end > i && isPunct(body.get(end - 1), "]")) {
            int depth = 0;
            int openIdx = -1;
            for (int k = end - 1; k >= i; k--) {
                Token t = body.get(k);
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
            sizeTokens.addAll(0, body.subList(openIdx, end));
            end = openIdx;
        }

        if (end <= i) {
            return null;
        }
        Token name = body.get(end - 1);
        if (name.type != TokenType.IDENTIFIER) {
            return null;
        }
        List<Token> typeTokens = new ArrayList<>(body.subList(i, end - 1));
        if (typeTokens.isEmpty()) {
            return null;
        }
        Token firstType = typeTokens.get(0);
        if (firstType.type == TokenType.KEYWORD && !typeKeywords.contains(firstType.text)) {
            return null; // e.g. return/throw/assert/break -- not a declaration
        }

        return new Declaration(modifiers, typeTokens, name, sizeTokens, initTokens,
                trailingComment, blankBefore);
    }

    private Token findTrailingComment(List<Token> stmt) {
        for (int k = stmt.size() - 1; k >= 0; k--) {
            Token t = stmt.get(k);
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                return t;
            }
            if (t.type != TokenType.WHITESPACE) {
                break;
            }
        }
        return null;
    }

    private List<Token> significantOnly(List<Token> stmt) {
        List<Token> sig = new ArrayList<>();
        for (Token t : stmt) {
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

    private boolean isPunct(Token t, String text) {
        return t.type == TokenType.PUNCT && text.equals(t.text);
    }

    private boolean isOp(Token t, String text) {
        return t.type == TokenType.OP && text.equals(t.text);
    }
}
