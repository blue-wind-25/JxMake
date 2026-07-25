/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.grid.JavaModifierPriority;
import com.jxmake.formatter.grid.ModifierPriority;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * STYLE.md §14 / STYLE_JAVA.md §5 -- Getter/Setter/Checker Group Alignment.
 */
public class GetterSetterRuleCurly extends GetterSetterRuleCore {

    /**
     * Statement keywords that can never legitimately begin a method's return type -- used to
     * reject a braceless single-statement control-flow body (e.g. {@code if (x == null) throw
     * new Y(...);}) from being misparsed as a one-liner getter/setter-style member. See
     * {@link #parseOneLinerMember} for the full rationale.
     */
    private static final Set<String> STATEMENT_KEYWORDS = new HashSet<>(Arrays.asList(
            "if", "else", "while", "for", "do", "switch", "try", "catch", "finally", "throw",
            "return", "synchronized"));

    private final ModifierPriority modifierPriority; // null for C/C++ -- no modifier column there

    public GetterSetterRuleCurly(final Lang lang) {
        this(lang, MiscRuleCurly.DEFAULT_INDENT_WIDTH, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public GetterSetterRuleCurly(final Lang lang, final int indentWidth, final int lineLengthLimit) {
        super(lang, indentWidth, lineLengthLimit);
        this.modifierPriority = lang.isJava ? new JavaModifierPriority() : null;
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
     *
     * @param depth nesting depth of {@code scopeTokens}' enclosing scope (0 = file/namespace
     *              level), used only to estimate each candidate's rendered column for the
     *              line-length pre-check in {@link #parseOneLinerMember} -- same {@code depth}
     *              {@link com.jxmake.formatter.ScopePipeline#processScope} already threads to
     *              {@code applySignaturePass}'s own inline-fit check.
     */
    public List<List<Member>> groupOneLiners(final List<Token> scopeTokens, final int depth) {
        final boolean isClassScope = lang.isJava || hasAccessSpecifier(scopeTokens);
        final List<int[]> spans = splitMembers(scopeTokens);
        final List<List<Member>> groups = new ArrayList<>();
        List<Member> current = new ArrayList<>();

        for (final int[] span : spans) {
            Member m = parseOneLinerMember(scopeTokens, span[0], span[1], depth);
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
    private static final int OUTLIER_RATIO = 3;

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
        // A constructor member has no return type at all (STYLE.md §14 doesn't define a
        // sensible shared return-type column start with a sibling that does have one, e.g.
        // `EngineBase(const EngineBase&) = delete;` next to `EngineBase& operator=(...) =
        // delete;`) -- when any member in the group is like this, the return-type and
        // name/params cells are merged into one so the empty-type row isn't left-padded to
        // the width of its sibling's real return type.
        boolean mergeReturnTypeIntoCall = false;
        for (final Member m : group) {
            if (m.returnTypeTo <= m.returnTypeFrom) {
                mergeReturnTypeIntoCall = true;
                break;
            }
        }

        // A leading C++ `template<...>` clause (out-of-line class-template member definitions)
        // is its own column, separate from the return type -- otherwise the return-type column's
        // padding would land after the clause (and the qualified `Class<Impl>::` name) instead of
        // right after the actual return type.
        boolean hasTemplatePrefix = false;
        for (final Member m : group) {
            if (m.templatePrefixTo > m.templatePrefixFrom) {
                hasTemplatePrefix = true;
                break;
            }
        }

        // Modifier columns (Java only).
        final int modifierColumns = lang.isJava ? modifierPriority.columnCount() : 0;
        final boolean[] modifierActive = new boolean[modifierColumns];
        if (lang.isJava) {
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
            // Definitions: pad name and params via nested callGrid.
            // For params, split each member's params into type and name so they can be
            // padded independently -- otherwise a single wide params cell pads trailing
            // spaces after the name rather than between type and name.
            final String[] typeTexts = new String[group.size()];
            final String[] nameTexts = new String[group.size()];
            int maxTypeWidth = 0;
            int maxNameWidth = 0;
            boolean canSplitParams = true;

            for (int i = 0; i < group.size(); i++) {
                final Member m = group.get(i);
                if (m.paramsFrom >= m.paramsTo) {
                    // Empty params -- no split; left as null to signal "use verbatim"
                    typeTexts[i] = null;
                    nameTexts[i] = null;
                    continue;
                }
                // Find the last IDENTIFIER in the params range as the param name
                int nameTokenIdx = -1;
                for (int k = m.paramsTo - 1; k >= m.paramsFrom; k--) {
                    final Token tk = tokens.get(k);
                    if (tk.type == TokenType.IDENTIFIER) {
                        nameTokenIdx = k;
                        break;
                    }
                    if (!isInsignificant(tk)) {
                        break;
                    }
                }
                if (nameTokenIdx < 0) {
                    canSplitParams = false;
                    break;
                }
                final int typeEnd = trimTrailingWs(tokens, m.paramsFrom, nameTokenIdx);
                typeTexts[i] = cellText(tokens, m.paramsFrom, typeEnd);
                nameTexts[i] = tokens.get(nameTokenIdx).text;
                maxTypeWidth = Math.max(maxTypeWidth, typeTexts[i].length());
                maxNameWidth = Math.max(maxNameWidth, nameTexts[i].length());
            }
            // True when no member in this group has an actual type token before its single
            // param's name (JS/TS untyped params, e.g. "set x(value)") -- in that case the
            // type/name separator space below must be omitted entirely, not just padded to a
            // zero-width type column, or a leading space leaks in before the param name (e.g.
            // "x( value)").
            final boolean noTypeColumn = maxTypeWidth == 0;

            final ColumnGrid callGrid = new ColumnGrid();
            for (int i = 0; i < group.size(); i++) {
                final Member m = group.get(i);
                final String paramsCell;
                if (!canSplitParams || typeTexts[i] == null) {
                    // Empty params or unsplittable: use verbatim (ColumnGrid pads)
                    paramsCell = cellText(tokens, m.paramsFrom, m.paramsTo);
                } else if (noTypeColumn) {
                    // No member in the group has a real type -- just the (padded) name, no
                    // separator space that would otherwise leak in front of it.
                    paramsCell = padRight(nameTexts[i], maxNameWidth);
                } else {
                    // Pre-padded: type and name in separate columns of fixed width
                    paramsCell = padRight(typeTexts[i], maxTypeWidth) + " " + padRight(nameTexts[i], maxNameWidth);
                }
                // Trailing "" keeps params from being the last cell so ColumnGrid pads it
                // even when empty (e.g. "getX()").
                callGrid.addRow(new String[] {cellText(tokens, m.nameFrom, m.nameIdx + 1),
                        paramsCell, ""});
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

            if (lang.isJava) {
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

            if (hasTemplatePrefix) {
                cells.add(cellText(tokens, m.templatePrefixFrom, m.templatePrefixTo));
            }

            final String returnTypeText = cellText(tokens, m.returnTypeFrom, m.returnTypeTo);
            if (mergeReturnTypeIntoCall) {
                final String prefix = returnTypeText.isEmpty() ? "" : returnTypeText + " ";
                callCells[idx] = prefix + callCells[idx];
            } else {
                cells.add(returnTypeText);
            }

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
     * fields, multi-line members, members whose one-line rendering would exceed
     * {@link #lineLengthLimit} at their estimated column (see below), and any other
     * unrecognised shape.
     *
     * <p>The length pre-check exists because {@code enforceCallLineBreaking} runs in a later
     * pipeline phase and would break an over-long one-liner body across multiple lines anyway --
     * without this check, a fresh format groups/pads such a member as if it were staying inline
     * (using its original single-line text, still short at this point), only for the later phase
     * to break it, leaving the group's column padding stale; reformatting that already-broken
     * output then correctly excludes the now-multi-line member via {@code hasNewlineBetween}
     * above, changing the surviving members' padding on the second pass. Excluding it here too,
     * on the very first pass, keeps the decision (and thus the padding) stable across repeated
     * formats. The check is approximate (raw pre-padding text length, not the exact final
     * rendering), but only has to agree with {@code enforceCallLineBreaking}'s own verdict well
     * enough to avoid flip-flopping, since once excluded here it can never re-enter a group (a
     * broken-across-lines member always fails {@code hasNewlineBetween} on every later pass).
     */
    protected Member parseOneLinerMember(final List<Token> tokens, final int from, final int to, final int nestDepth) {
        final int firstSig = firstSignificantIndex(tokens, from, to);
        if (firstSig < 0) {
            return null;
        }
        final boolean blankBefore = hasBlankLineRun(tokens, from, firstSig);
        if (hasNewlineBetween(tokens, firstSig, to)) {
            return null;
        }
        // A `case ...` / `default ...` switch label is never a getter/setter-style member --
        // reject it here rather than letting `findNameBeforeParen` below treat its label tokens
        // (up through the `->`) as a bogus "return type" in front of the arrow body's own
        // trailing call (e.g. Java's `default -> throw new AssertionError(x);` misparsed as
        // return-type "default -> throw new" + name "AssertionError"), which then grid-aligns
        // garbage padding into an unrelated sibling case's body (see STATE.md).
        if (lang.isJava && tokens.get(firstSig).type == TokenType.KEYWORD
                && ("case".equals(tokens.get(firstSig).text) || "default".equals(tokens.get(firstSig).text))) {
            return null;
        }
        int pos = firstSig;
        final List<Token> modifiers = new ArrayList<>();
        if (lang.isJava) {
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
        // A leading C++ `template<...>` clause (e.g. an out-of-line member-function definition
        // of a class template, `template<AudioProcessor Impl> float Engine<Impl>::getGain()...`)
        // is not part of the return type -- depth-matched on the raw `<`/`>` OP tokens, same
        // precedent as `DeclarationAlignmentRule`'s own `templatePrefix` handling (the tokenizer
        // never reclassifies these to `ANGLE_BRACKET_OPEN`/`_CLOSE` since `template`, not an
        // identifier/cast-keyword, precedes the `<`).
        int templatePrefixFrom = pos;
        int templatePrefixTo = pos;
        if (!lang.isJava) {
            final int templateKwIdx = nextSignificant(tokens, pos, to);
            if (templateKwIdx >= 0 && tokens.get(templateKwIdx).type == TokenType.KEYWORD
                    && "template".equals(tokens.get(templateKwIdx).text)) {
                final int ltIdx = nextSignificant(tokens, templateKwIdx + 1, to);
                if (ltIdx >= 0 && isOp(tokens.get(ltIdx), "<")) {
                    int depth = 0;
                    int closeIdx = -1;
                    for (int k = ltIdx; k < to; k++) {
                        final Token tk = tokens.get(k);
                        if (isOp(tk, "<")) {
                            depth++;
                        } else if (isOp(tk, ">")) {
                            depth--;
                            if (depth == 0) {
                                closeIdx = k;
                                break;
                            }
                        }
                    }
                    if (closeIdx >= 0) {
                        templatePrefixFrom = templateKwIdx;
                        templatePrefixTo = closeIdx + 1;
                        pos = templatePrefixTo;
                    }
                }
            }
        }

        final int returnTypeFrom = nextSignificant(tokens, pos, to);
        if (returnTypeFrom < 0) {
            return null;
        }

        final int nameIdx = findNameBeforeParen(tokens, returnTypeFrom, to);
        if (nameIdx < 0) {
            return null;
        }

        // Extend nameIdx backwards for qualified names (e.g. "Processor::method"), for the
        // `operator` keyword of an operator-overload name (e.g. "operator="), and for a
        // destructor's `~` marker (e.g. "~Engine") -- none of these are a real return type.
        int nameFrom = nameIdx;
        final int beforeName = prevSignificant(tokens, nameFrom - 1, returnTypeFrom);
        if (beforeName >= 0 && beforeName >= returnTypeFrom
                && tokens.get(beforeName).type == TokenType.KEYWORD
                && "operator".equals(tokens.get(beforeName).text)) {
            nameFrom = beforeName;
        } else if (beforeName >= 0 && beforeName >= returnTypeFrom
                && isOp(tokens.get(beforeName), "~")) {
            nameFrom = beforeName;
        }
        while (nameFrom > returnTypeFrom) {
            final int prevA = prevSignificant(tokens, nameFrom - 1, returnTypeFrom);
            if (prevA < 0 || !isOp(tokens.get(prevA), "::")) {
                break;
            }
            int prevB = prevSignificant(tokens, prevA - 1, returnTypeFrom);
            if (prevB < 0 || prevB < returnTypeFrom) {
                break;
            }
            // A qualifier segment may itself be a class-template specialization, e.g.
            // "Engine<Impl>::getGain" -- skip back over the depth-matched "<...>" to reach the
            // class-name identifier before it.
            if (tokens.get(prevB).type == TokenType.ANGLE_BRACKET_CLOSE) {
                int depth = 0;
                int openIdx = -1;
                for (int k = prevB; k >= returnTypeFrom; k--) {
                    final Token tk = tokens.get(k);
                    if (tk.type == TokenType.ANGLE_BRACKET_CLOSE) {
                        depth++;
                    } else if (tk.type == TokenType.ANGLE_BRACKET_OPEN) {
                        depth--;
                        if (depth == 0) {
                            openIdx = k;
                            break;
                        }
                    }
                }
                if (openIdx < 0) {
                    break;
                }
                prevB = prevSignificant(tokens, openIdx - 1, returnTypeFrom);
                if (prevB < 0 || prevB < returnTypeFrom) {
                    break;
                }
            }
            final Token tb = tokens.get(prevB);
            if (tb.type != TokenType.IDENTIFIER && tb.type != TokenType.KEYWORD) {
                break;
            }
            nameFrom = prevB;
        }

        // No return type before the name -- e.g. a constructor (`EngineBase(...)`) or destructor
        // (`~Engine()`). Only accepted later if it turns out to carry a pure-specifier
        // (`= delete`/`= default`), so a bare function-call-shaped statement is never
        // misclassified as a member.
        final boolean noReturnType = nameFrom == returnTypeFrom;

        final int effectiveReturnTypeFrom = noReturnType ? nameFrom : returnTypeFrom;
        final int returnTypeTo = trimTrailingWs(tokens, effectiveReturnTypeFrom, nameFrom);
        if (returnTypeTo < effectiveReturnTypeFrom || (!noReturnType && returnTypeTo <= returnTypeFrom)) {
            return null;
        }
        // Reject candidates whose "return type" span contains `.` or `=` -- those only appear
        // here when this isn't actually a method declaration/definition at all, but a plain
        // statement (e.g. `var trimmed = item.trim();`, `result.add(trimmed);`) that happens to
        // end in `identifier(...)` immediately before the terminator. A real return type is only
        // ever built from identifiers/keywords, `::`, template `<...>`, `*`, and `&`.
        for (int k = returnTypeFrom; k < returnTypeTo; k++) {
            final Token t = tokens.get(k);
            if (isOp(t, ".") || isOp(t, "=")) {
                return null;
            }
        }
        // Reject candidates whose "return type" span contains a control-flow/statement keyword
        // (e.g. `if (begin == null) throw new IllegalArgumentException(...)`) -- those only
        // appear here when this is actually a braceless single-statement `if`/`while`/etc. body
        // (or its trailing call), not a method declaration/definition whose "return type" just
        // happens to end in `identifier(...)`. Same misparse class as the `case`/`default`
        // guard above, generalized to every statement keyword that can never legitimately begin
        // a return type.
        for (int k = returnTypeFrom; k < returnTypeTo; k++) {
            final Token t = tokens.get(k);
            if (t.type == TokenType.KEYWORD && STATEMENT_KEYWORDS.contains(t.text)) {
                return null;
            }
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

        if (noReturnType && pureSpecifier == null) {
            return null; // bare constructor call/declaration -- not a member we can align
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
            Token foundComment = null;
            for (int ci = closeBraceIdx + 1; ci < to; ci++) {
                final Token t = tokens.get(ci);
                if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                    foundComment = t;
                    break;
                } else if (t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) {
                    return null; // stray non-comment token after }
                }
            }
            trailingComment = foundComment;
            bodyFrom = trimLeadingWs(tokens, terminatorIdx + 1, closeBraceIdx);
            bodyTo = trimTrailingWs(tokens, bodyFrom, closeBraceIdx);
        } else if (isPunct(terminator, ";")) {
            isDefinition = false;
            bodyFrom = -1;
            bodyTo = -1;
            Token foundComment = null;
            for (int ci = terminatorIdx + 1; ci < to; ci++) {
                final Token t = tokens.get(ci);
                if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                    foundComment = t;
                    break;
                } else if (t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) {
                    return null; // stray non-comment token after ";"
                }
            }
            trailingComment = foundComment;
        } else {
            return null; // throws clause or other unrecognised form
        }

        // A body containing a non-empty-arg call (as opposed to a trivial `return x;`/`x = y;`
        // statement) is exactly the shape `enforceCallLineBreaking` (Phase 1, later in the
        // pipeline) may break across multiple lines if the full rendered line doesn't fit --
        // check that predicted width here so grouping/padding is decided consistently whether
        // this is a fresh format (body still on one physical line) or a reformat of output
        // already broken by that later phase (see this method's class-level doc comment). Bodies
        // with no such call (only field access/assignment) are never touched by that pass
        // regardless of line length, so the check must not apply to them, or legitimately long
        // column-aligned one-liners (e.g. verbose template-qualified C++ names) would be wrongly
        // excluded.
        //
        // A non-definition member (plain declaration or pure-specifier `= 0`/`= delete`/
        // `= default`) has no body to inspect, but its own `(params)` list is exactly the shape
        // `enforceCallLineBreaking` may wrap later (RDD_KEY_86's "IDENTIFIER ( ... )" detection,
        // gated only on the call/decl not being followed by `{`) whenever it doesn't fit -- same
        // divergence risk as the definition case just above, just triggered by the member's own
        // parameter list instead of a call inside its body. Found via `microsoft/STL`'s
        // `filesystem.hpp` `recursive_directory_iterator` (a long copy-constructor declaration
        // whose own too-long, non-empty parameter list contributed its full un-wrapped width to
        // this group's alignment column on a fresh format, since at this point in the pipeline it
        // is still one raw physical line and so still passed the `hasNewlineBetween` check above --
        // but was excluded from the group entirely on a reformat of that fresh format's own output,
        // where `enforceCallLineBreaking` had already wrapped it across multiple physical lines,
        // narrowing the group and its alignment column between rounds). A zero-arg `name()` is
        // never broken by that pass (mirroring `hasBreakableCall`'s own doc comment), so only a
        // non-empty parameter list needs this check.
        final boolean hasBreakableParams = !isDefinition && paramsFrom < paramsTo;
        if ((isDefinition && hasBreakableCall(tokens, bodyFrom, bodyTo)) || hasBreakableParams) {
            final int estimatedColumn = nestDepth * indentWidth;
            final int estimatedWidth = estimatedColumn + cellText(tokens, firstSig, to).length();
            if (estimatedWidth > lineLengthLimit) {
                return null;
            }
        }

        return new Member(modifiers, templatePrefixFrom, templatePrefixTo, effectiveReturnTypeFrom, returnTypeTo,
                nameFrom, nameIdx, paramsFrom, paramsTo, bodyFrom, bodyTo, from, to, trailingComment, blankBefore,
                postParenQualifier, pureSpecifier, isDefinition);
    }



    // ── Local bracket matching ──────────────────────────────────────────────────

    // ── Token-scanning helpers ───────────────────────────────────────────────────







    /** True iff {@code t} is a C++ post-paren qualifier that can appear between {@code )} and
     *  the function body or terminator (const, volatile, noexcept, override, final). */
    private boolean isPostParenQualifier(final Token t) {
        if (lang.isJava || t.type != TokenType.KEYWORD) {
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

}
