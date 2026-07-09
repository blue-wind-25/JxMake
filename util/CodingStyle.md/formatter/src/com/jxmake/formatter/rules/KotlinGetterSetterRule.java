/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * STYLE_KOTLIN.md §8/§9: extends {@link GetterSetterRule} for Kotlin expression-bodied
 * one-liner functions (e.g. {@code fun getX(): Int = 1}), fixing the RDD_KEY_132 gap where
 * {@code GetterSetterRule.groupOneLiners} never grouped Kotlin one-liners at all --
 * {@code isClassScope}'s gate (Java, or a C++-style {@code public:}/{@code private:} label)
 * never recognizes Kotlin, and {@code parseOneLinerMember}'s modifier-consuming loop plus
 * name/return-type scanning assume C/Java's {@code [modifiers] ReturnType name(...)} token
 * order, the reverse of Kotlin's {@code [modifiers] fun name(...): ReturnType = expr}.
 *
 * Same "loosen shared-class visibility, then extend" pattern as
 * {@link KotlinDeclarationAlignmentRule} (RDD_KEY_103) and {@link KotlinSignatureRule}
 * (RDD_KEY_104): the base class's C/C++/Java-agnostic private helpers were raised to
 * {@code protected} (no behavior change), and this subclass supplies its own newline-terminated
 * member splitter, its own name-before-type one-liner parser, and its own column-grid renderer,
 * reusing {@link GetterSetterRule.Member} purely as an index-range container (its
 * {@code bodyFrom}/{@code bodyTo} fields are repurposed here to hold the expression-bodied
 * function's {@code = expr} span rather than a brace-delimited block body, so
 * {@code excludeOutliers}'s width-based outlier exclusion still works unmodified).
 *
 * <p>Scope: only expression-bodied one-liner functions (§9) -- the shape harness-confirmed
 * broken in STATE_KOTLIN.md's Open Questions (`fun getX(): Int = 1` / `getY` / `getZ` failing to
 * column-align). Property accessor one-liners ({@code get()}/{@code set()}, §8) are a separate,
 * structurally different shape (no {@code fun} keyword, embedded inside a property declaration)
 * and are intentionally left unhandled here -- STYLE_KOTLIN.md §8's "preserve as written"
 * requirement is still satisfied for them (this class never touches anything that doesn't match
 * its own {@code fun}-anchored shape), it is only the alignment *upgrade* that remains out of
 * scope for that shape.
 */
public class KotlinGetterSetterRule extends GetterSetterRule {

    private static final List<String> FUN_MODIFIERS = Arrays.asList(
            "public", "private", "protected", "internal", "override", "open", "final",
            "abstract", "inline", "suspend", "operator", "infix", "tailrec", "external");

    public KotlinGetterSetterRule(final Lang lang) {
        super(lang);
    }

    public KotlinGetterSetterRule(final Lang lang, final int indentWidth, final int lineLengthLimit) {
        super(lang, indentWidth, lineLengthLimit);
    }

    /**
     * Kotlin has no C/Java-style class-scope/`{ }`-nesting-vs-file-scope distinction that would
     * change this rule's behavior (no access-specifier labels, and top-level file-scope
     * expression-bodied functions are just as groupable as class members) -- always true, same
     * unconditional posture as the base class's own {@code lang.isJava} branch.
     */
    @Override
    public List<List<Member>> groupOneLiners(final List<Token> scopeTokens, final int depth) {
        final List<int[]> spans = splitKotlinMemberSpans(scopeTokens);
        final List<List<Member>> groups = new ArrayList<>();
        List<Member> current = new ArrayList<>();

        for (final int[] span : spans) {
            final Member m = parseKotlinOneLinerMember(scopeTokens, span[0], span[1], depth);
            if (m == null) {
                if (current.size() >= 2) {
                    groups.add(current);
                }
                current = new ArrayList<>();
                continue;
            }
            if (m.blankLineBefore && !current.isEmpty()) {
                if (current.size() >= 2) {
                    groups.add(current);
                }
                current = new ArrayList<>();
            }
            current.add(m);
        }
        if (current.size() >= 2) {
            groups.add(current);
        }
        return groups;
    }

    /**
     * Splits {@code scopeTokens} into newline-terminated statement spans (plus `;`-terminated
     * ones, in case a stray semicolon survives), mirroring
     * {@code KotlinDeclarationAlignmentRule.splitKotlinStatements}'s int-index-based sibling --
     * the base class's own {@code splitMembers} assumes every statement ends in a top-level `;`
     * or a `{ ... }` block, which is wrong for Kotlin's newline-terminated, semicolon-optional
     * grammar (an expression-bodied one-liner has neither).
     */
    private List<int[]> splitKotlinMemberSpans(final List<Token> scopeTokens) {
        final List<int[]> spans = new ArrayList<>();
        final int n = scopeTokens.size();
        int start = 0;
        int depth = 0;
        boolean sawSignificant = false;
        int i = 0;
        while (i < n) {
            final Token t = scopeTokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
                sawSignificant = true;
                i++;
                continue;
            }
            if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                depth--;
                sawSignificant = true;
                i++;
                continue;
            }
            if (depth == 0 && isPunct(t, ";")) {
                i++;
                spans.add(new int[] {start, i});
                start = i;
                sawSignificant = false;
                continue;
            }
            if (depth == 0 && t.type == TokenType.NEWLINE && sawSignificant) {
                spans.add(new int[] {start, i});
                start = i; // the newline itself becomes the next span's leading gap
                sawSignificant = false;
                continue;
            }
            if (!isInsignificant(t)) {
                sawSignificant = true;
            }
            i++;
        }
        if (start < n) {
            spans.add(new int[] {start, n});
        }
        return spans;
    }

    /**
     * Recognises a single-line {@code [modifiers] fun name(params) [: ReturnType] = expr}
     * expression-bodied function (STYLE_KOTLIN.md §9). Returns null for anything else --
     * block-bodied functions ({@code { ... }}), functions with no {@code =} at all, multi-line
     * members, and any unrecognised shape -- same "never guess past an unrecognized shape"
     * posture as every other Kotlin-aware rule in this codebase.
     */
    private Member parseKotlinOneLinerMember(final List<Token> tokens, final int from, final int to,
            final int nestDepth) {
        final int firstSig = firstSignificantIndex(tokens, from, to);
        if (firstSig < 0) {
            return null;
        }
        final boolean blankBefore = hasBlankLineRun(tokens, from, firstSig);
        if (hasNewlineBetween(tokens, firstSig, to)) {
            return null;
        }

        int pos = firstSig;
        while (pos < to) {
            final Token t = tokens.get(pos);
            if (isInsignificant(t)) {
                pos++;
                continue;
            }
            if (t.type == TokenType.KEYWORD && FUN_MODIFIERS.contains(t.text)) {
                pos++;
                continue;
            }
            break;
        }

        final int funIdx = nextSignificant(tokens, pos, to);
        if (funIdx < 0 || tokens.get(funIdx).type != TokenType.KEYWORD || !"fun".equals(tokens.get(funIdx).text)) {
            return null; // only plain functions (not extension receivers/generics) supported
        }
        final int returnTypeFrom = firstSig; // "[modifiers] fun" rendered verbatim as one lead cell
        final int returnTypeTo = funIdx + 1;

        final int nameIdx = nextSignificant(tokens, funIdx + 1, to);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return null;
        }

        final int parenOpenIdx = nextSignificant(tokens, nameIdx + 1, to);
        if (parenOpenIdx < 0 || !isPunct(tokens.get(parenOpenIdx), "(")) {
            return null;
        }
        final int parenCloseIdx = matchBracket(tokens, parenOpenIdx, "(", ")");
        if (parenCloseIdx < 0) {
            return null;
        }
        final int paramsFrom = trimLeadingWs(tokens, parenOpenIdx + 1, parenCloseIdx);
        final int paramsTo = trimTrailingWs(tokens, paramsFrom, parenCloseIdx);

        int scanFrom = parenCloseIdx + 1;
        String postParenQualifier = "";
        final int colonIdx = nextSignificant(tokens, scanFrom, to);
        if (colonIdx >= 0 && isOp(tokens.get(colonIdx), ":")) {
            final int typeStart = nextSignificant(tokens, colonIdx + 1, to);
            if (typeStart < 0) {
                return null;
            }
            int angleDepth = 0;
            int typeEnd = -1;
            for (int k = typeStart; k < to; k++) {
                final Token tk = tokens.get(k);
                if (isPunct(tk, "(") || tk.type == TokenType.ANGLE_BRACKET_OPEN) {
                    angleDepth++;
                } else if (isPunct(tk, ")") || tk.type == TokenType.ANGLE_BRACKET_CLOSE) {
                    angleDepth--;
                } else if (angleDepth == 0 && isOp(tk, "=")) {
                    typeEnd = k;
                    break;
                }
            }
            if (typeEnd < 0) {
                return null; // no '=' after the return type -- not an expression body
            }
            final int typeTrimEnd = trimTrailingWs(tokens, typeStart, typeEnd);
            if (typeTrimEnd <= typeStart) {
                return null;
            }
            postParenQualifier = cellText(tokens, typeStart, typeTrimEnd);
            scanFrom = typeEnd;
        }

        final int eqIdx = nextSignificant(tokens, scanFrom, to);
        if (eqIdx < 0 || !isOp(tokens.get(eqIdx), "=")) {
            return null; // block-bodied, or an explicit-return-type function with no expr body
        }

        // Find the last non-whitespace/newline token in range -- distinguishing a genuine
        // trailing same-line comment (kept as its own cell) from the expression body itself,
        // since isInsignificant() treats comments the same as whitespace for ordinary trimming.
        int lastIdx = to - 1;
        while (lastIdx > eqIdx && (tokens.get(lastIdx).type == TokenType.WHITESPACE
                || tokens.get(lastIdx).type == TokenType.NEWLINE)) {
            lastIdx--;
        }
        if (lastIdx <= eqIdx) {
            return null; // no expression body
        }
        Token trailingComment = null;
        int bodyTo;
        if (tokens.get(lastIdx).type == TokenType.COMMENT_LINE || tokens.get(lastIdx).type == TokenType.COMMENT_BLOCK) {
            trailingComment = tokens.get(lastIdx);
            bodyTo = trimTrailingWs(tokens, eqIdx + 1, lastIdx);
        } else {
            bodyTo = lastIdx + 1;
        }
        final int bodyFrom = trimLeadingWs(tokens, eqIdx + 1, bodyTo);
        if (bodyFrom >= bodyTo) {
            return null; // empty expression body
        }

        return new Member(new ArrayList<Token>(), returnTypeFrom, returnTypeFrom,
                returnTypeFrom, returnTypeTo,
                nameIdx, nameIdx, paramsFrom, paramsTo,
                bodyFrom, bodyTo, from, to,
                trailingComment, blankBefore,
                postParenQualifier, null, false);
    }

    /**
     * Renders one aligned group of expression-bodied one-liner functions (STYLE_KOTLIN.md §9):
     * {@code [modifiers] fun} / {@code name(params)[: ReturnType]} / {@code = expr} as three
     * grid-aligned columns, plus a trailing (ragged) comment column when present -- the base
     * class's own {@code render} cannot be reused as-is since it hard-codes a `{ body };`
     * definition/declaration shape that doesn't exist in Kotlin's postfix-type, no-semicolon
     * grammar.
     */
    @Override
    public List<String> render(final List<Token> tokens, final List<Member> group) {
        final ColumnGrid grid = new ColumnGrid();
        for (final Member m : group) {
            final List<String> cells = new ArrayList<>();
            cells.add(cellText(tokens, m.returnTypeFrom, m.returnTypeTo));
            final String callCell = cellText(tokens, m.nameFrom, m.nameIdx + 1)
                    + "(" + cellText(tokens, m.paramsFrom, m.paramsTo) + ")";
            cells.add(callCell);
            cells.add(m.postParenQualifier.isEmpty() ? "" : ": " + m.postParenQualifier);
            cells.add("= " + cellText(tokens, m.bodyFrom, m.bodyTo));
            if (m.trailingComment != null) {
                cells.add(m.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }
        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(String.join(" ", row));
        }
        return lines;
    }
}
