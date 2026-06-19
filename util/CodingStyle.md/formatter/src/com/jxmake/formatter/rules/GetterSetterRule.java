/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.grid.JavaModifierPriority;
import com.jxmake.formatter.grid.ModifierPriority;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * STYLE.md §14 / STYLE_JAVA.md §5 -- Getter/Setter/Checker Group Alignment.
 */
public class GetterSetterRule {

    private final String language;
    private final boolean isJava;
    private final ModifierPriority modifierPriority; // null for C/C++ -- no modifier column there

    public GetterSetterRule(final String language) {
        this.language = language;
        this.isJava = "java".equals(language);
        this.modifierPriority = isJava ? new JavaModifierPriority() : null;
    }

    /** One parsed one-liner method candidate -- all fields are index ranges into the caller's token list. */
    public static final class Member {
        public final List<Token> modifiers;   // Java only; empty for C/C++
        public final int returnTypeFrom, returnTypeTo;
        public final int nameIdx;
        public final int paramsFrom, paramsTo;
        public final int bodyFrom, bodyTo;
        public final int memberFrom, memberTo; // full original span, for verbatim passthrough
        public final Token trailingComment;    // nullable
        public final boolean blankLineBefore;

        Member(final List<Token> modifiers, final int returnTypeFrom, final int returnTypeTo,
                final int nameIdx, final int paramsFrom, final int paramsTo, final int bodyFrom,
                final int bodyTo, final int memberFrom, final int memberTo,
                final Token trailingComment, final boolean blankLineBefore) {
            this.modifiers = modifiers;
            this.returnTypeFrom = returnTypeFrom;
            this.returnTypeTo = returnTypeTo;
            this.nameIdx = nameIdx;
            this.paramsFrom = paramsFrom;
            this.paramsTo = paramsTo;
            this.bodyFrom = bodyFrom;
            this.bodyTo = bodyTo;
            this.memberFrom = memberFrom;
            this.memberTo = memberTo;
            this.trailingComment = trailingComment;
            this.blankLineBefore = blankLineBefore;
        }
    }

    // ── Group detection ─────────────────────────────────────────────────────────
    /**
     * Splits one class/struct/enum body's full token range (including nested method-body
     * tokens -- unlike {@code DeclarationAlignmentRule}, this rule needs to see inside `{ }`)
     * into maximal runs of 2+ textually adjacent single-statement one-liner methods. A blank
     * line, a comment-only gap, or any member that isn't a recognized one-liner method breaks
     * the current run. A run of length 1 is never returned as a group (STYLE.md §14).
     */
    public List<List<Member>> groupOneLiners(final List<Token> scopeTokens) {
        final List<int[]> spans = splitMembers(scopeTokens);
        final List<List<Member>> groups = new ArrayList<>();
        List<Member> current = new ArrayList<>();

        for (final int[] span : spans) {
            final Member m = parseOneLinerMember(scopeTokens, span[0], span[1]);
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

    // ── Outlier exclusion ───────────────────────────────────────────────────────
    private static final int OUTLIER_RATIO = 2;

    /**
     * Excludes outliers from a candidate group: a member is excluded if its body width is more
     * than {@code OUTLIER_RATIO}x the next-widest *remaining* member's body width, applied
     * iteratively (exclude, recompute among what's left, re-check) so that removing one outlier
     * can reveal another (STYLE.md §14). An excluded member is left out of the result entirely --
     * the caller leaves it untouched, same as a non-grouped one-liner. If fewer than 2 members
     * remain after exclusion, the whole group is no longer a group at all, and an empty list is
     * returned -- the caller must then leave every original member of this run untouched too.
     */
    public List<Member> excludeOutliers(final List<Token> tokens, final List<Member> group) {
        final List<Member> remaining = new ArrayList<>(group);
        while (remaining.size() >= 2) {
            int maxWidth = -1;
            int secondWidth = -1;
            int maxIdx = -1;
            for (int i = 0; i < remaining.size(); i++) {
                final int w = bodyWidth(tokens, remaining.get(i));
                if (w > maxWidth) {
                    secondWidth = maxWidth;
                    maxWidth = w;
                    maxIdx = i;
                } else if (w > secondWidth) {
                    secondWidth = w;
                }
            }
            if (maxWidth > secondWidth * OUTLIER_RATIO) {
                remaining.remove(maxIdx);
            } else {
                break;
            }
        }
        return remaining.size() >= 2 ? remaining : new ArrayList<Member>();
    }

    private int bodyWidth(final List<Token> tokens, final Member m) {
        return cellText(tokens, m.bodyFrom, m.bodyTo).length();
    }

    // ── Column grid rendering ───────────────────────────────────────────────────
    /**
     * Renders one aligned group (already passed through `excludeOutliers`) into source lines
     * (STYLE.md §14, STYLE_JAVA.md §5): fixed modifier columns (Java only -- only those actually
     * used anywhere in the group), the return type, a `name(params)` cell (name and params each
     * padded to the group's widest via a nested {@link ColumnGrid}, same precedent as
     * `SwitchRule.applyInlineAlignment`'s call-shaped case rows), `{`, the body, and `}`, plus an
     * optional trailing comment column. The closing `}` column falls out for free since it is just
     * another fixed cell in this same single left-to-right grid pass -- no second pass is needed.
     */
    public List<String> render(final List<Token> tokens, final List<Member> group) {
        final int modifierColumns = isJava ? modifierPriority.columnCount() : 0;
        final boolean[] modifierActive = new boolean[modifierColumns];
        for (final Member m : group) {
            for (final Token mod : m.modifiers) {
                final int rank = modifierPriority.priorityOf(mod.text);
                if (rank >= 0) {
                    modifierActive[rank] = true;
                }
            }
        }

        final ColumnGrid callGrid = new ColumnGrid();
        for (final Member m : group) {
            // Trailing "" keeps params from being the last cell, so ColumnGrid's ragged-row
            // rule doesn't skip padding it when params is empty (e.g. "getX()").
            callGrid.addRow(new String[] {cellText(tokens, m.nameIdx, m.nameIdx + 1),
                    cellText(tokens, m.paramsFrom, m.paramsTo), ""});
        }
        final List<String[]> callPadded = callGrid.flush();

        final ColumnGrid grid = new ColumnGrid();
        for (int idx = 0; idx < group.size(); idx++) {
            final Member m = group.get(idx);
            final List<String> cells = new ArrayList<>();

            if (isJava) {
                final String[] modCells = new String[modifierColumns];
                Arrays.fill(modCells, "");
                for (final Token mod : m.modifiers) {
                    final int rank = modifierPriority.priorityOf(mod.text);
                    if (rank >= 0) {
                        modCells[rank] = mod.text;
                    }
                }
                for (int r = 0; r < modifierColumns; r++) {
                    if (modifierActive[r]) {
                        cells.add(modCells[r]);
                    }
                }
            }

            cells.add(cellText(tokens, m.returnTypeFrom, m.returnTypeTo));

            final String[] call = callPadded.get(idx);
            cells.add(call[0] + "(" + call[1] + ")");

            cells.add("{");
            cells.add(cellText(tokens, m.bodyFrom, m.bodyTo));
            cells.add("}");

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

    private String cellText(final List<Token> tokens, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(tokens.get(i).text);
        }
        return sb.toString();
    }

    /**
     * Splits a class/struct/enum body's tokens into top-level members: each spans from the end
     * of the previous member to either a top-level `;` (field, import, etc.) or a complete
     * top-level `{ ... }` block (method/constructor/nested-type body), plus any same-line
     * trailing comment. Uses local relative depth counting on the slice, not the tokenizer's
     * absolute `braceDepth` field -- same precedent as `BlockStructureRule`/`SwitchRule`.
     */
    private List<int[]> splitMembers(final List<Token> scopeTokens) {
        final List<int[]> members = new ArrayList<>();
        final int n = scopeTokens.size();
        int start = 0;
        int depth = 0;
        int i = 0;

        while (i < n) {
            final Token t = scopeTokens.get(i);
            if (isPunct(t, "{")) {
                depth++;
                i++;
                continue;
            }
            if (isPunct(t, "}")) {
                depth--;
                i++;
                if (depth == 0) {
                    final int end = consumeTrailingSameLine(scopeTokens, i);
                    members.add(new int[] {start, end});
                    start = end;
                }
                continue;
            }
            if (depth == 0 && isPunct(t, ";")) {
                i++;
                final int end = consumeTrailingSameLine(scopeTokens, i);
                members.add(new int[] {start, end});
                start = end;
                continue;
            }
            i++;
        }
        if (start < n) {
            members.add(new int[] {start, n});
        }
        return members;
    }

    private int consumeTrailingSameLine(final List<Token> tokens, final int from) {
        int idx = from;
        final int n = tokens.size();
        while (idx < n) {
            final TokenType ty = tokens.get(idx).type;
            if (ty == TokenType.WHITESPACE || ty == TokenType.COMMENT_LINE
                    || ty == TokenType.COMMENT_BLOCK) {
                idx++;
            } else {
                break;
            }
        }
        return idx;
    }

    // ── One-liner method recognition ────────────────────────────────────────────
    /**
     * Recognizes the shape `[modifiers]* returnType name ( params ) { oneStatement; }` entirely
     * on one source line (no `NEWLINE` token between its first significant token and its closing
     * `}`). Anything that doesn't match exactly -- a field, a multi-line method, a constructor (no
     * return type), a method with a `throws` clause, a multi-statement or brace-wrapped body, etc.
     * -- returns null and is left completely untouched by this rule, same conservative posture as
     * the rest of the formatter.
     */
    private Member parseOneLinerMember(final List<Token> tokens, final int from, final int to) {
        final int firstSig = firstSignificantIndex(tokens, from, to);
        if (firstSig < 0) {
            return null;
        }
        final boolean blankBefore = hasBlankLineRun(tokens, from, firstSig);
        if (hasNewlineBetween(tokens, firstSig, to)) {
            return null;
        }

        int pos = firstSig;
        final List<Token> modifiers = new ArrayList<>();
        if (isJava) {
            while (pos < to) {
                final Token t = tokens.get(pos);
                if (isInsignificant(t)) {
                    pos++;
                    continue;
                }
                if (t.type == TokenType.KEYWORD && modifierPriority.isModifier(t.text)) {
                    modifiers.add(t);
                    pos++;
                    continue;
                }
                break;
            }
        }
        final int returnTypeFrom = nextSignificant(tokens, pos, to);
        if (returnTypeFrom < 0) {
            return null;
        }

        final int nameIdx = findNameBeforeParen(tokens, returnTypeFrom, to);
        if (nameIdx < 0 || nameIdx == returnTypeFrom) {
            return null; // no return type before the name -- e.g. a constructor
        }
        final int returnTypeTo = trimTrailingWs(tokens, returnTypeFrom, nameIdx);
        if (returnTypeTo <= returnTypeFrom) {
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

        final int braceIdx = nextSignificant(tokens, parenCloseIdx + 1, to);
        if (braceIdx < 0 || !isPunct(tokens.get(braceIdx), "{")) {
            return null; // e.g. a `throws` clause -- not a recognized one-liner shape
        }
        final int closeBraceIdx = matchBracket(tokens, braceIdx, "{", "}");
        if (closeBraceIdx < 0) {
            return null;
        }

        final int lastSig = lastSignificantIndex(tokens, closeBraceIdx + 1, to);
        final Token trailingComment;
        if (lastSig >= 0) {
            final Token t = tokens.get(lastSig);
            if (t.type != TokenType.COMMENT_LINE && t.type != TokenType.COMMENT_BLOCK) {
                return null; // stray tokens after the closing brace -- not a clean one-liner
            }
            trailingComment = t;
        } else {
            trailingComment = null;
        }

        final int bodyFrom = trimLeadingWs(tokens, braceIdx + 1, closeBraceIdx);
        final int bodyTo = trimTrailingWs(tokens, bodyFrom, closeBraceIdx);
        if (!isSingleStatementBody(tokens, bodyFrom, bodyTo)) {
            return null;
        }

        return new Member(modifiers, returnTypeFrom, returnTypeTo, nameIdx, paramsFrom, paramsTo,
                bodyFrom, bodyTo, from, to, trailingComment, blankBefore);
    }

    /** First IDENTIFIER in [from, to) whose next significant token is `(`; -1 if none. */
    private int findNameBeforeParen(final List<Token> tokens, final int from, final int to) {
        int i = from;
        while (i < to) {
            final Token t = tokens.get(i);
            if (!isInsignificant(t)) {
                if (t.type == TokenType.IDENTIFIER) {
                    final int next = nextSignificant(tokens, i + 1, to);
                    if (next >= 0 && isPunct(tokens.get(next), "(")) {
                        return i;
                    }
                }
            }
            i++;
        }
        return -1;
    }

    /**
     * True iff [from, to) contains exactly one `;` at local bracket depth 0, and that `;` is the
     * last significant token in the range -- i.e. exactly one statement, nothing trailing after
     * it (STYLE.md §14's "single-statement one-liner" requirement).
     */
    private boolean isSingleStatementBody(final List<Token> tokens, final int from, final int to) {
        int depth = 0;
        int semiCount = 0;
        int semiIdx = -1;
        int lastSig = -1;
        for (int i = from; i < to; i++) {
            final Token t = tokens.get(i);
            if (isInsignificant(t)) {
                continue;
            }
            lastSig = i;
            if (t.type == TokenType.PUNCT) {
                switch (t.text) {
                    case "(":
                    case "[":
                    case "{":
                        depth++;
                        break;
                    case ")":
                    case "]":
                    case "}":
                        depth--;
                        break;
                    case ";":
                        if (depth == 0) {
                            semiCount++;
                            semiIdx = i;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return semiCount == 1 && semiIdx == lastSig;
    }

    // ── Local bracket matching ──────────────────────────────────────────────────
    private int matchBracket(final List<Token> tokens, final int openIdx, final String open,
            final String close) {
        int depth = 1;
        int i = openIdx + 1;
        final int n = tokens.size();
        while (i < n && depth > 0) {
            final Token t = tokens.get(i);
            if (isPunct(t, open)) {
                depth++;
            } else if (isPunct(t, close)) {
                depth--;
            }
            i++;
        }
        return depth == 0 ? i - 1 : -1;
    }

    // ── Token-scanning helpers ───────────────────────────────────────────────────
    private boolean isInsignificant(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    private int firstSignificantIndex(final List<Token> tokens, final int from, final int to) {
        for (int i = from; i < to; i++) {
            if (!isInsignificant(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int lastSignificantIndex(final List<Token> tokens, final int from, final int to) {
        for (int i = to - 1; i >= from; i--) {
            if (!isInsignificant(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int nextSignificant(final List<Token> tokens, final int from, final int to) {
        return firstSignificantIndex(tokens, from, to);
    }

    private int trimLeadingWs(final List<Token> tokens, final int from, final int to) {
        int start = from;
        while (start < to && isInsignificant(tokens.get(start))) {
            start++;
        }
        return start;
    }

    private int trimTrailingWs(final List<Token> tokens, final int from, final int to) {
        int end = to;
        while (end > from && isInsignificant(tokens.get(end - 1))) {
            end--;
        }
        return end;
    }

    /** True iff any `NEWLINE` token appears in [from, to) -- i.e. the span crosses a source line. */
    private boolean hasNewlineBetween(final List<Token> tokens, final int from, final int to) {
        for (int i = from; i < to; i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return true;
            }
        }
        return false;
    }

    /** Same blank-line-before detection as `DeclarationAlignmentRule.hasBlankLineBefore`. */
    private boolean hasBlankLineRun(final List<Token> tokens, final int from, final int to) {
        int newlineRun = 0;
        for (int i = from; i < to; i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.NEWLINE) {
                newlineRun++;
                if (newlineRun >= 2) {
                    return true;
                }
            } else if (t.type == TokenType.WHITESPACE) {
                // ignore -- doesn't break or extend the newline run
            } else if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                newlineRun = 0;
            } else {
                break;
            }
        }
        return false;
    }

    private boolean isPunct(final Token t, final String text) {
        return t.type == TokenType.PUNCT && text.equals(t.text);
    }
}
