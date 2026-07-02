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
            "unsigned", "struct", "enum", "union", "bool", "_Bool");

    private static final Set<String> TYPE_KEYWORDS_CPP = union(TYPE_KEYWORDS_C,
            setOf("bool", "wchar_t", "char16_t", "char32_t", "auto", "class"));

    private static final Set<String> TYPE_KEYWORDS_JAVA = setOf(
            "boolean", "byte", "char", "double", "float", "int", "long", "short", "void", "var");

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
        public final List<Token> bitfieldWidth; // empty if not a bitfield
        public final Token trailingComment; // nullable
        public final boolean blankLineBefore;

        Declaration(final List<Token> modifiers, final List<Token> typeTokens, final Token name,
                final List<Token> sizeTokens, final List<Token> initTokens,
                final List<Token> bitfieldWidth, final Token trailingComment,
                final boolean blankLineBefore) {
            this.modifiers = modifiers;
            this.typeTokens = typeTokens;
            this.name = name;
            this.sizeTokens = sizeTokens;
            this.initTokens = initTokens;
            this.bitfieldWidth = bitfieldWidth;
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
     * optional post-pointer `const` column, the name+size+`;` (or, for C/C++
     * bitfields, the name+`:`+width+`;` per STYLE_C_CPP.md §6), and an optional
     * trailing comment column. Columns unused by the whole group are omitted
     * rather than rendered as dead padding. Statics are reordered first
     * (see `reorderStatics`).
     */
    public List<String> render(final List<Declaration> originalGroup) {
        // C/C++ declarations must not be reordered -- changing order can alter semantics.
        final List<Declaration> group = "java".equals(language) ? reorderStatics(originalGroup) : originalGroup;

        // Function forward declarations use a simpler 2-column layout (no modifier columns).
        boolean allAreFuncDecls = !group.isEmpty();
        for (final Declaration d : group) {
            if (d.sizeTokens.isEmpty() || !isPunct(d.sizeTokens.get(0), "(")) {
                allAreFuncDecls = false;
                break;
            }
        }
        if (allAreFuncDecls) {
            return renderFunctionForwardGroup(group);
        }

        final boolean isJava = "java".equals(language);
        final int modifierColumns = modifierPriority.columnCount();
        final boolean[] modifierActive = new boolean[modifierColumns];
        boolean postConstActive = false;
        int bitfieldNameWidth = 0;

        final List<TypeSplit> splits = new ArrayList<>(group.size());
        for (final Declaration d : group) {
            if (!d.bitfieldWidth.isEmpty()) {
                bitfieldNameWidth = Math.max(bitfieldNameWidth, d.name.text.length());
            }
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

        boolean isStructuredBinding = false;
        if ("cpp".equals(language)) {
            for (final Declaration d : group) {
                if ("[".equals(d.name.text)) {
                    isStructuredBinding = true;
                }
            }
        }

        int maxInitNameWidth = 0;
        for (final Declaration d : group) {
            if (!d.initTokens.isEmpty()) {
                if (isStructuredBinding && !"[".equals(d.name.text)) {
                   d.name.text = ' ' + d.name.text;
                }
                maxInitNameWidth = Math.max(maxInitNameWidth,
                        d.name.text.length() + renderTokens(d.sizeTokens).length());
            }
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

            cells.add(renderNameCell(d, bitfieldNameWidth, maxInitNameWidth));

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

    /**
     * Renders the name+size+`;` (or, for a bitfield, name+`:`+width+`;`) cell.
     * Per STYLE_C_CPP.md §6, the bitfield name is padded only against other
     * bitfield names in the same group (`bitfieldNameWidth`), not against
     * unrelated declarations' names+sizes in the same group -- the trailing
     * comment column then falls out of ColumnGrid's normal per-column
     * max-width padding once this cell's content is fixed.
     * `maxInitNameWidth` pads the name+size portion when any sibling declaration
     * in the group has an initializer, aligning all `=` signs.
     */
    private String renderNameCell(final Declaration d, final int bitfieldNameWidth, final int maxInitNameWidth) {
        final StringBuilder sb = new StringBuilder();
        if (!d.bitfieldWidth.isEmpty()) {
            sb.append(d.name.text);
            for (int pad = d.name.text.length(); pad < bitfieldNameWidth; pad++) {
                sb.append(' ');
            }
            sb.append(" : ").append(renderTokens(d.bitfieldWidth));
            sb.append(";");
            return sb.toString();
        }
        final String nameAndSize = d.name.text + renderTokens(d.sizeTokens);
        if (!d.initTokens.isEmpty()) {
            sb.append(nameAndSize);
            for (int pad = nameAndSize.length(); pad < maxInitNameWidth; pad++) {
                sb.append(' ');
            }
            sb.append(" = ").append(renderInitTokens(d.initTokens));
        } else {
            sb.append(nameAndSize);
        }
        sb.append(";");
        return sb.toString();
    }

    private List<String> renderFunctionForwardGroup(final List<Declaration> group) {
        final ColumnGrid grid = new ColumnGrid();
        for (final Declaration d : group) {
            final List<Token> allTypeTokens = new ArrayList<>(d.modifiers);
            allTypeTokens.addAll(d.typeTokens);
            final String typeStr = renderTokens(allTypeTokens);
            String nameStr = d.name.text + renderTokens(d.sizeTokens);
            if (!d.initTokens.isEmpty()) {
                nameStr += " = " + renderTokens(d.initTokens);
            }
            nameStr += ";";
            grid.addRow(new String[] { typeStr, nameStr });
        }
        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(String.join(" ", row));
        }
        return lines;
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

    /** Renders initializer value tokens (the right-hand side of `= expr`) where `*` and `&`
     *  may represent either binary operators or unary pointer/reference operators. Uses
     *  lookahead to distinguish binary `*`/`&`: if followed by an IDENTIFIER or NUMBER and
     *  preceded by an IDENTIFIER or WHITESPACE, a space is inserted before the operator.
     *  Also suppresses spacing between unary dereference `*`/`**` and the following
     *  identifier in C/C++ expression contexts (e.g. `*ptr`, `**ptr`), while all other
     *  spacing follows the normal token-spacing rules. */
    private String renderInitTokens(final List<Token> tokens) {
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
                        && ("c".equals(language) || "cpp".equals(language))) {
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
    private boolean isCStyleCastClose(final List<Token> tokens, final int closeIdx) {
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
        for (int k = openIdx + 1; k < closeIdx; k++) {
            final Token t = tokens.get(k);
            if (t.type != TokenType.IDENTIFIER && t.type != TokenType.KEYWORD
                    && !Token.isRepOp(t, '*')) {
                return false;
            }
        }
        return true;
    }

    private boolean needsSpaceBetween(final Token prev, final Token cur) {
        if (isTightToken(cur)) {
            return false;
        }
        if (isPunct(cur, "(") && (prev.type == TokenType.IDENTIFIER
                || prev.type == TokenType.ANGLE_BRACKET_CLOSE)) {
            return false;
        }
        if (isPunct(cur, "{") && prev.type == TokenType.IDENTIFIER) {
            return false;
        }
        if (prev.type == TokenType.ANGLE_BRACKET_OPEN || isOp(prev, "::") || isOp(prev, ".") || isOp(prev, "->") || isPunct(prev, "[") || isPunct(prev, "(")) {
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
        return Token.isRepOp(t, '*') || Token.isRepOp(t, '&') || isOp(t, "::") || isOp(t, ".") || isOp(t, "->");
    }

    /** True if {@code initTokens} represents a function-declaration specifier (`= 0`, `= delete`,
     *  `= default`) rather than a true variable initializer. */
    private static boolean isFuncDeclSpecifier(final List<Token> initTokens) {
        if (initTokens.size() != 1) {
            return false;
        }
        final Token t = initTokens.get(0);
        return "0".equals(t.text) || "delete".equals(t.text) || "default".equals(t.text);
    }

    // ── Statement splitting ─────────────────────────────────────────────────────
    private List<List<Token>> splitStatements(final List<Token> scopeTokens) {
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
            } else if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK
                    || t.type == TokenType.PREPROCESSOR || t.type == TokenType.MACRO_DEF) {
                newlineRun = 0; // a comment/preprocessor line consumes that line's content slot
            } else {
                break;
            }
        }
        return false;
    }

    // ── Declaration parsing ──────────────────────────────────────────────────────
    /**
     * True iff `initTokens` is a single top-level `{ ... }` brace-initializer with no
     * nested `{` and no `;` inside -- i.e. a flat aggregate init like `{ a, b, c }`
     * that can be rendered verbatim on one line and safely column-aligned.
     */
    private boolean isFlatAggregateInit(final List<Token> initTokens) {
        if (!isPunct(initTokens.get(0), "{")) {
            return false;
        }
        int depth = 0;
        for (int k = 0; k < initTokens.size(); k++) {
            final Token t = initTokens.get(k);
            if (isPunct(t, "{")) {
                depth++;
                if (depth > 1) {
                    return false;
                }
            } else if (isPunct(t, "}")) {
                depth--;
                if (depth == 0 && k != initTokens.size() - 1) {
                    return false;
                }
            } else if (isPunct(t, ";")) {
                return false;
            }
        }
        return depth == 0;
    }

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

        if ("cpp".equals(language)) {
            final Declaration binding = parseStructuredBinding(modifiers, body, i, trailingComment, blankBefore);
            if (binding != null) {
                return binding;
            }
        }

        int colonIdx = -1;
        for (int j = i; j < body.size(); j++) {
            if (isOp(body.get(j), ":")) {
                colonIdx = j;
                break;
            }
        }
        if (colonIdx >= 0) {
            return parseBitfield(modifiers, body, i, colonIdx, trailingComment, blankBefore);
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
        // Reject if the initializer ends with `}` and isn't a flat aggregate init (e.g. a
        // lambda body, class/struct body, or nested-brace init) -- those can't safely be
        // column-aligned. A flat `{ a, b, c }` with no nested `{` or `;` inside is safe.
        if (!initTokens.isEmpty() && isPunct(initTokens.get(initTokens.size() - 1), "}")
                && !isFlatAggregateInit(initTokens)) {
            return null;
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

        // Strip a trailing function-parameter list (...) to handle forward declarations.
        // Also fires for `= 0` / `= delete` / `= default` suffixes (pure-virtual / deleted /
        // defaulted), which are function specifiers, not true variable initializers.
        if (sizeEnd > i && isPunct(body.get(sizeEnd - 1), ")")
                && (initTokens.isEmpty() || isFuncDeclSpecifier(initTokens))) {
            int depth2 = 0;
            int parenOpenIdx = -1;
            for (int k = sizeEnd - 1; k >= i; k--) {
                final Token t = body.get(k);
                if (isPunct(t, ")")) {
                    depth2++;
                } else if (isPunct(t, "(")) {
                    depth2--;
                    if (depth2 == 0) {
                        parenOpenIdx = k;
                        break;
                    }
                }
            }
            if (parenOpenIdx > i) {
                sizeTokens.addAll(0, body.subList(parenOpenIdx, sizeEnd));
                sizeEnd = parenOpenIdx;
            }
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
        // Reject member-access expressions and qualified-name accesses (ptr->field, obj.field,
        // Type::member) in type position -- the last type token being `::` means the "name" is
        // actually the RHS of a scope-resolution expression, not a variable being declared.
        for (final Token t : typeTokens) {
            if (isOp(t, "->") || isOp(t, ".")) {
                return null;
            }
        }
        if (isOp(typeTokens.get(typeTokens.size() - 1), "::")) {
            return null;
        }
        final Token firstType = typeTokens.get(0);
        if (firstType.type == TokenType.KEYWORD) {
            if (!typeKeywords.contains(firstType.text)) {
                return null; // e.g. return/throw/assert/break -- not a declaration
            }
        } else if (firstType.type != TokenType.IDENTIFIER) {
            return null; // e.g. a bare `++`/`--`/`!` prefix -- not a type, not a declaration
        }

        return new Declaration(modifiers, typeTokens, name, sizeTokens, initTokens,
                new ArrayList<Token>(), trailingComment, blankBefore);
    }

    /**
     * Parses the `auto [a, b, ...] = expr;` shape of a C++17 structured binding
     * (STYLE_CPP20.md §1). Legal type prefixes before the bracket list are only
     * `auto`/`const`/`volatile`/`&`/`&&` -- never a real identifier -- so if an
     * IDENTIFIER token or a top-level `=` appears before the first top-level `[`,
     * this isn't a structured binding and the caller falls through to the normal
     * name/size parsing path. Returns null (not just "no match" but also any
     * malformed/unbalanced shape) so the caller always has a safe fallback.
     */
    private Declaration parseStructuredBinding(final List<Token> modifiers, final List<Token> body,
            final int typeStart, final Token trailingComment, final boolean blankBefore) {
        int bracketStart = -1;
        for (int j = typeStart; j < body.size(); j++) {
            final Token t = body.get(j);
            if (isPunct(t, "[")) {
                bracketStart = j;
                break;
            }
            if (isOp(t, "=") || t.type == TokenType.IDENTIFIER) {
                return null;
            }
        }
        if (bracketStart < 0 || bracketStart <= typeStart) {
            return null;
        }

        int depth = 0;
        int bracketEnd = -1;
        for (int j = bracketStart; j < body.size(); j++) {
            final Token t = body.get(j);
            if (isPunct(t, "[")) {
                depth++;
            } else if (isPunct(t, "]")) {
                depth--;
                if (depth == 0) {
                    bracketEnd = j;
                    break;
                }
            }
        }
        if (bracketEnd < 0) {
            return null; // unbalanced -- bail, don't touch this statement
        }

        final List<Token> typeTokens = new ArrayList<>(body.subList(typeStart, bracketStart));
        final Token firstType = typeTokens.get(0);
        if (firstType.type != TokenType.KEYWORD || !typeKeywords.contains(firstType.text)) {
            return null;
        }

        int eqIdx = -1;
        for (int j = bracketEnd + 1; j < body.size(); j++) {
            if (isOp(body.get(j), "=")) {
                eqIdx = j;
                break;
            }
        }
        if (eqIdx < 0) {
            return null; // structured bindings are always initialized
        }
        final List<Token> initTokens = new ArrayList<>(body.subList(eqIdx + 1, body.size()));
        if (initTokens.isEmpty()) {
            return null;
        }

        // `name` must stay a real token from `body` (identity-anchored back to the original
        // statement by ScopePipeline's splice-back map) -- so the opening `[` itself is the
        // name cell's first token, and the rest of the bracket (interior + closing `]`) rides
        // along as `sizeTokens`, exactly like a real array-size suffix would. renderNameCell
        // concatenates the two unchanged, producing the atomic "[a, b, c]" cell.
        final Token bindingName = body.get(bracketStart);
        final List<Token> bracketRest = new ArrayList<>(body.subList(bracketStart + 1, bracketEnd + 1));

        return new Declaration(modifiers, typeTokens, bindingName, bracketRest,
                initTokens, new ArrayList<Token>(), trailingComment, blankBefore);
    }

    /** Parses the `Type name : width` shape of a C/C++ bitfield (STYLE_C_CPP.md §6). */
    private Declaration parseBitfield(final List<Token> modifiers, final List<Token> body,
            final int typeStart, final int colonIdx, final Token trailingComment,
            final boolean blankBefore) {
        if (colonIdx <= typeStart) {
            return null;
        }
        final Token name = body.get(colonIdx - 1);
        if (name.type != TokenType.IDENTIFIER) {
            return null;
        }
        final List<Token> typeTokens = new ArrayList<>(body.subList(typeStart, colonIdx - 1));
        if (typeTokens.isEmpty()) {
            return null;
        }
        final Token firstType = typeTokens.get(0);
        if (firstType.type == TokenType.KEYWORD) {
            if (!typeKeywords.contains(firstType.text)) {
                return null;
            }
        } else if (firstType.type != TokenType.IDENTIFIER) {
            return null;
        }
        final List<Token> widthTokens = new ArrayList<>(body.subList(colonIdx + 1, body.size()));
        if (widthTokens.isEmpty()) {
            return null;
        }
        // A real bitfield width is a simple integer expression -- never contains `{`.
        // Reject `enum Foo : Base { ... }` and `class Foo : Base { ... }` which reach
        // parseBitfield via the `:` in their inheritance/base-type specifier.
        for (final Token wt : widthTokens) {
            if (isPunct(wt, "{")) {
                return null;
            }
        }
        return new Declaration(modifiers, typeTokens, name, new ArrayList<Token>(),
                new ArrayList<Token>(), widthTokens, trailingComment, blankBefore);
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
                case PREPROCESSOR:
                case MACRO_DEF:
                    continue;
                default:
                    sig.add(t);
            }
        }
        return sig;
    }

    private static boolean isPunct(final Token t, final String text) {
        return t.type == TokenType.PUNCT && text.equals(t.text);
    }

    private static boolean isOp(final Token t, final String text) {
        return t.type == TokenType.OP && text.equals(t.text);
    }
}
