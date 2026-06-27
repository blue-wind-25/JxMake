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
        public final int nameFrom;            // start of qualified name (= nameIdx for unqualified)
        public final int nameIdx;
        public final int paramsFrom, paramsTo;
        public final int bodyFrom, bodyTo;    // -1/-1 for declarations
        public final int memberFrom, memberTo; // full original span, for verbatim passthrough
        public final Token trailingComment;    // nullable
        public final boolean blankLineBefore;
        public final String postParenQualifier; // "" or " const" etc.; never contains "override"
        public final String pureSpecifier;      // null / "= 0" / "= delete" / "= default"
        public final boolean isDefinition;      // true = { body }, false = ; terminated

        Member(final List<Token> modifiers, final int returnTypeFrom, final int returnTypeTo,
                final int nameFrom, final int nameIdx, final int paramsFrom, final int paramsTo,
                final int bodyFrom, final int bodyTo, final int memberFrom, final int memberTo,
                final Token trailingComment, final boolean blankLineBefore,
                final String postParenQualifier, final String pureSpecifier,
                final boolean isDefinition) {
            this.modifiers = modifiers;
            this.returnTypeFrom = returnTypeFrom;
            this.returnTypeTo = returnTypeTo;
            this.nameFrom = nameFrom;
            this.nameIdx = nameIdx;
            this.paramsFrom = paramsFrom;
            this.paramsTo = paramsTo;
            this.bodyFrom = bodyFrom;
            this.bodyTo = bodyTo;
            this.memberFrom = memberFrom;
            this.memberTo = memberTo;
            this.trailingComment = trailingComment;
            this.blankLineBefore = blankLineBefore;
            this.postParenQualifier = postParenQualifier;
            this.pureSpecifier = pureSpecifier;
            this.isDefinition = isDefinition;
        }
    }

    // ── Group detection ─────────────────────────────────────────────────────────
    /**
     * Splits one class/struct/enum body's full token range (including nested method-body
     * tokens -- unlike {@code DeclarationAlignmentRule}, this rule needs to see inside `{ }`)
     * into maximal runs of 2+ textually adjacent single-statement one-liner methods. A blank
     * line, a comment-only gap, any member not recognised as a one-liner, or a change in member
     * kind (definition vs. declaration vs. pure-specifier) breaks the current run. A run of
     * length 1 is never returned as a group (STYLE.md §14).
     *
     * Declaration grouping (isDefinition=false) is only enabled when the scope tokens contain
     * at least one access-specifier label (public:/private:/protected:), which identifies the
     * scope as a C++ class/struct body. In Java, declaration grouping is always enabled since
     * Java class bodies have no such labels.
     */
    public List<List<Member>> groupOneLiners(final List<Token> scopeTokens) {
        final boolean isClassScope = isJava || hasAccessSpecifier(scopeTokens);
        final List<int[]> spans = splitMembers(scopeTokens);
        final List<List<Member>> groups = new ArrayList<>();
        List<Member> current = new ArrayList<>();

        for (final int[] span : spans) {
            Member m = parseOneLinerMember(scopeTokens, span[0], span[1]);
            // In file/namespace scope, only process definitions; skip declarations.
            if (m != null && !isClassScope && !m.isDefinition) {
                m = null;
            }
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
            // Break on kind change: definition vs. declaration, or with/without pure-specifier.
            if (!current.isEmpty()) {
                final Member prev = current.get(current.size() - 1);
                final boolean kindChanged = prev.isDefinition != m.isDefinition
                        || (prev.pureSpecifier == null) != (m.pureSpecifier == null);
                if (kindChanged) {
                    if (current.size() >= 2) {
                        groups.add(current);
                    }
                    current = new ArrayList<>();
                }
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
     * Renders one aligned group (already passed through {@code excludeOutliers}) into source
     * lines (STYLE.md §14, STYLE_JAVA.md §5). Three rendering modes, all sharing modifier
     * columns (Java only) and return-type column:
     *
     * <ul>
     *   <li><b>Definitions</b>: nested callGrid pads name and params; {@code { body }} columns.
     *   <li><b>Plain declarations</b>: name column padded via ColumnGrid; params verbatim (not
     *       internally aligned); {@code qualifier;} appended as last (ragged) column.
     *   <li><b>Pure-specifier declarations</b> ({@code = 0 / = delete / = default}): entire
     *       {@code name(params)qualifier} cell is verbatim and not internally aligned; only
     *       {@code = X;} aligns as the last column.
     * </ul>
     */
    public List<String> render(final List<Token> tokens, final List<Member> group) {
        final boolean isPureSpecifier = group.get(0).pureSpecifier != null;
        final boolean isDef = group.get(0).isDefinition;

        // Modifier columns (Java only).
        final int modifierColumns = isJava ? modifierPriority.columnCount() : 0;
        final boolean[] modifierActive = new boolean[modifierColumns];
        if (isJava) {
            for (final Member m : group) {
                for (final Token mod : m.modifiers) {
                    final int rank = modifierPriority.priorityOf(mod.text);
                    if (rank >= 0) {
                        modifierActive[rank] = true;
                    }
                }
            }
        }

        // Build per-member call cells.
        final String[] callCells = new String[group.size()];
        if (isDef) {
            // Definitions: pad name and params via nested callGrid (existing behaviour).
            final ColumnGrid callGrid = new ColumnGrid();
            for (final Member m : group) {
                // Trailing "" keeps params from being the last cell so ColumnGrid pads it
                // even when empty (e.g. "getX()").
                callGrid.addRow(new String[] {cellText(tokens, m.nameFrom, m.nameIdx + 1),
                        cellText(tokens, m.paramsFrom, m.paramsTo), ""});
            }
            final List<String[]> callPadded = callGrid.flush();
            for (int i = 0; i < group.size(); i++) {
                final Member m = group.get(i);
                final String[] call = callPadded.get(i);
                callCells[i] = call[0] + "(" + call[1] + ")" + m.postParenQualifier;
            }
        } else if (!isPureSpecifier) {
            // Plain declarations: type column aligns name start; name and params verbatim.
            for (int i = 0; i < group.size(); i++) {
                final Member m = group.get(i);
                callCells[i] = cellText(tokens, m.nameFrom, m.nameIdx + 1).trim()
                        + "(" + cellText(tokens, m.paramsFrom, m.paramsTo).trim() + ")"
                        + m.postParenQualifier;
            }
        } else {
            // Pure-specifier declarations: entire name(params)qualifier verbatim.
            for (int i = 0; i < group.size(); i++) {
                final Member m = group.get(i);
                callCells[i] = cellText(tokens, m.nameFrom, m.nameIdx + 1).trim()
                        + "(" + cellText(tokens, m.paramsFrom, m.paramsTo).trim() + ")"
                        + m.postParenQualifier;
            }
        }

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

            if (isDef) {
                cells.add(callCells[idx]);
                cells.add("{");
                cells.add(cellText(tokens, m.bodyFrom, m.bodyTo));
                cells.add("}");
            } else if (isPureSpecifier) {
                // call cell is NOT last: ColumnGrid pads it so = X; aligns.
                cells.add(callCells[idx]);
                cells.add(m.pureSpecifier + ";");
            } else {
                // Plain declaration: call cell IS last (ragged); ";" appended directly.
                cells.add(callCells[idx] + ";");
            }

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
     * Recognises one of three shapes (all on a single source line, no intervening NEWLINE):
     * <ul>
     *   <li>Definition:              {@code [mods]* retType [Q::]name(params) [quals] { stmt; }}
     *   <li>Plain declaration:       {@code [mods]* retType [Q::]name(params) [quals] ;}
     *   <li>Pure-specifier decl:     {@code [mods]* retType [Q::]name(params) [quals] = 0|delete|default ;}
     * </ul>
     * Returns null for: constructors, methods with {@code override} qualifier (those are
     * implementing a base-class contract, not getter/setter pairs), {@code throws} clauses,
     * fields, multi-line members, and any other unrecognised shape.
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

        // Extend nameIdx backwards for qualified names (e.g. "Processor::method").
        int nameFrom = nameIdx;
        while (nameFrom > returnTypeFrom) {
            final int prevA = prevSignificant(tokens, nameFrom - 1, returnTypeFrom);
            if (prevA < 0 || !isOp(tokens.get(prevA), "::")) {
                break;
            }
            final int prevB = prevSignificant(tokens, prevA - 1, returnTypeFrom);
            if (prevB < 0 || prevB < returnTypeFrom) {
                break;
            }
            final Token tb = tokens.get(prevB);
            if (tb.type != TokenType.IDENTIFIER && tb.type != TokenType.KEYWORD) {
                break;
            }
            nameFrom = prevB;
        }

        final int returnTypeTo = trimTrailingWs(tokens, returnTypeFrom, nameFrom);
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

        // Collect post-paren qualifiers (C++ only). Override-annotated members are never
        // getter/setter pairs -- skip them.
        final StringBuilder qualBuilder = new StringBuilder();
        int afterParen = parenCloseIdx + 1;
        while (true) {
            final int sigIdx = nextSignificant(tokens, afterParen, to);
            if (sigIdx < 0) {
                break;
            }
            final Token t = tokens.get(sigIdx);
            if (!isPostParenQualifier(t)) {
                break;
            }
            if ("override".equals(t.text)) {
                return null; // never group override declarations
            }
            if ("noexcept".equals(t.text)) {
                final int nextSig = nextSignificant(tokens, sigIdx + 1, to);
                if (nextSig >= 0 && isPunct(tokens.get(nextSig), "(")) {
                    return null; // noexcept(expr) form not supported
                }
            }
            qualBuilder.append(" ").append(t.text);
            afterParen = sigIdx + 1;
        }
        final String postParenQualifier = qualBuilder.toString();

        // Check for pure-specifier: = 0 / = delete / = default.
        String pureSpecifier = null;
        final int eqSigIdx = nextSignificant(tokens, afterParen, to);
        if (eqSigIdx >= 0 && isOp(tokens.get(eqSigIdx), "=")) {
            final int specIdx = nextSignificant(tokens, eqSigIdx + 1, to);
            if (specIdx >= 0) {
                final Token st = tokens.get(specIdx);
                final boolean isPure = (st.type == TokenType.NUMBER && "0".equals(st.text))
                        || (st.type == TokenType.KEYWORD
                                && ("delete".equals(st.text) || "default".equals(st.text)));
                if (isPure) {
                    pureSpecifier = "= " + st.text;
                    afterParen = specIdx + 1;
                }
            }
        }

        // Determine terminator: { (definition) or ; (declaration).
        final int terminatorIdx = nextSignificant(tokens, afterParen, to);
        if (terminatorIdx < 0) {
            return null;
        }
        final Token terminator = tokens.get(terminatorIdx);

        final boolean isDefinition;
        final int bodyFrom;
        final int bodyTo;
        final Token trailingComment;

        if (isPunct(terminator, "{")) {
            if (pureSpecifier != null) {
                return null; // = 0 { ... } is not a valid shape
            }
            isDefinition = true;
            final int closeBraceIdx = matchBracket(tokens, terminatorIdx, "{", "}");
            if (closeBraceIdx < 0) {
                return null;
            }
            final int lastSig = lastSignificantIndex(tokens, closeBraceIdx + 1, to);
            if (lastSig >= 0) {
                final Token t = tokens.get(lastSig);
                if (t.type != TokenType.COMMENT_LINE && t.type != TokenType.COMMENT_BLOCK) {
                    return null; // stray tokens after closing brace
                }
                trailingComment = t;
            } else {
                trailingComment = null;
            }
            bodyFrom = trimLeadingWs(tokens, terminatorIdx + 1, closeBraceIdx);
            bodyTo = trimTrailingWs(tokens, bodyFrom, closeBraceIdx);
            if (!isSingleStatementBody(tokens, bodyFrom, bodyTo)) {
                return null;
            }
        } else if (isPunct(terminator, ";")) {
            isDefinition = false;
            bodyFrom = -1;
            bodyTo = -1;
            final int lastSig = lastSignificantIndex(tokens, terminatorIdx + 1, to);
            if (lastSig >= 0) {
                final Token t = tokens.get(lastSig);
                if (t.type != TokenType.COMMENT_LINE && t.type != TokenType.COMMENT_BLOCK) {
                    return null; // stray tokens after ";"
                }
                trailingComment = t;
            } else {
                trailingComment = null;
            }
        } else {
            return null; // throws clause or other unrecognised form
        }

        return new Member(modifiers, returnTypeFrom, returnTypeTo, nameFrom, nameIdx,
                paramsFrom, paramsTo, bodyFrom, bodyTo, from, to, trailingComment, blankBefore,
                postParenQualifier, pureSpecifier, isDefinition);
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

    private boolean isOp(final Token t, final String text) {
        return t.type == TokenType.OP && text.equals(t.text);
    }

    /** True iff {@code t} is a C++ post-paren qualifier that can appear between {@code )} and
     *  the function body or terminator (const, volatile, noexcept, override, final). */
    private boolean isPostParenQualifier(final Token t) {
        if (isJava || t.type != TokenType.KEYWORD) {
            return false;
        }
        switch (t.text) {
            case "const":
            case "volatile":
            case "noexcept":
            case "override":
            case "final":
                return true;
            default:
                return false;
        }
    }

    /** True iff the scope tokens contain at least one access-specifier label
     *  ({@code public:} / {@code private:} / {@code protected:}) at depth 0.
     *  Used to distinguish class/struct bodies from file/namespace scopes. */
    private boolean hasAccessSpecifier(final List<Token> tokens) {
        int depth = 0;
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{")) {
                depth++;
            } else if (isPunct(t, "}")) {
                depth--;
            } else if (depth == 0 && t.type == TokenType.KEYWORD
                    && ("public".equals(t.text) || "private".equals(t.text)
                            || "protected".equals(t.text))) {
                final int next = nextSignificant(tokens, i + 1, tokens.size());
                if (next >= 0 && isOp(tokens.get(next), ":")) {
                    return true;
                }
            }
        }
        return false;
    }

    private int prevSignificant(final List<Token> tokens, final int from, final int lowerBound) {
        for (int i = from; i >= lowerBound; i--) {
            if (!isInsignificant(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
