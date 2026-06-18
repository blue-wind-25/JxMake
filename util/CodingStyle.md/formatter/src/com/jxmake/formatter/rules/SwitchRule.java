/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwitchRule {

    // Fallback when a switch's own line indentation can't be used to derive the project's
    // actual indent unit (see deriveUnit) -- STYLE.md §1 default.
    private static final String DEFAULT_INDENT_UNIT = "    ";

    private final String language;
    private final TokenizerCore tokenizer;

    public SwitchRule(final String language) {
        this.language = language;
        this.tokenizer = new TokenizerCore(language);
    }

    // ── Switch/case structure discovery ─────────────────────────────────────────
    /** One `switch( ... ) { ... }` block and its direct (non-nested) `case`/`default` labels. */
    private static final class SwitchBlock {
        final int switchKwIdx;
        final int openBrace;
        final int closeBrace;
        final List<CaseLabel> cases;

        SwitchBlock(final int switchKwIdx, final int openBrace, final int closeBrace,
                final List<CaseLabel> cases) {
            this.switchKwIdx = switchKwIdx;
            this.openBrace = openBrace;
            this.closeBrace = closeBrace;
            this.cases = cases;
        }
    }

    /** One `case EXPR :` / `default :` label and the token range of its body. */
    private static final class CaseLabel {
        final int kwIdx;
        final int colonIdx;
        final int bodyEnd; // exclusive -- next label's kwIdx, or the switch's closeBrace

        CaseLabel(final int kwIdx, final int colonIdx, final int bodyEnd) {
            this.kwIdx = kwIdx;
            this.colonIdx = colonIdx;
            this.bodyEnd = bodyEnd;
        }
    }

    /**
     * Finds every `switch( ... ) { ... }` in the slice, including nested ones (each is matched
     * independently via local depth counting, so a nested switch's own cases are never confused
     * with its enclosing switch's cases -- they live at brace depth 1 relative to the outer
     * switch's body, while {@link #findCaseLabels} only looks at depth 0).
     */
    private List<SwitchBlock> findSwitches(final List<Token> tokens) {
        final List<SwitchBlock> result = new ArrayList<>();
        final int n = tokens.size();
        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (t.type != TokenType.KEYWORD || !"switch".equals(t.text)) {
                continue;
            }
            final int openParen = skipNonSignificant(tokens, i + 1);
            if (openParen >= n || !isPunct(tokens.get(openParen), "(")) {
                continue;
            }
            final int closeParen = matchParen(tokens, openParen);
            if (closeParen < 0) {
                continue;
            }
            final int openBrace = skipNonSignificant(tokens, closeParen + 1);
            if (openBrace >= n || !isPunct(tokens.get(openBrace), "{")) {
                continue;
            }
            final int closeBrace = matchBrace(tokens, openBrace);
            if (closeBrace < 0) {
                continue;
            }
            result.add(new SwitchBlock(i, openBrace, closeBrace,
                    findCaseLabels(tokens, openBrace, closeBrace)));
        }
        return result;
    }

    /** `case`/`default` labels directly inside [openBrace, closeBrace), at brace depth 0. */
    private List<CaseLabel> findCaseLabels(final List<Token> tokens, final int openBrace,
            final int closeBrace) {
        final List<int[]> starts = new ArrayList<>(); // {kwIdx, colonIdx}
        int depth = 0;
        for (int i = openBrace + 1; i < closeBrace; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{")) {
                depth++;
            } else if (isPunct(t, "}")) {
                depth--;
            } else if (depth == 0 && t.type == TokenType.KEYWORD
                    && ("case".equals(t.text) || "default".equals(t.text))) {
                final int colon = findLabelColon(tokens, i, closeBrace);
                if (colon >= 0) {
                    starts.add(new int[] {i, colon});
                }
            }
        }

        final List<CaseLabel> cases = new ArrayList<>(starts.size());
        for (int k = 0; k < starts.size(); k++) {
            final int bodyEnd = k + 1 < starts.size() ? starts.get(k + 1)[0] : closeBrace;
            cases.add(new CaseLabel(starts.get(k)[0], starts.get(k)[1], bodyEnd));
        }
        return cases;
    }

    /** The top-level `:` terminating a `case EXPR` / `default` label, or -1 if malformed. */
    private int findLabelColon(final List<Token> tokens, final int kwIdx, final int limit) {
        int depth = 0;
        for (int i = kwIdx + 1; i < limit; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]")) {
                depth--;
            } else if (depth == 0 && isOp(t, ":")) {
                return i;
            } else if (depth == 0 && isPunct(t, ";")) {
                return -1;
            }
        }
        return -1;
    }

    // ── Inline vs non-inline classification (STYLE.md §13) ──────────────────────
    /** True if any case's body spans more than one source line -- the switch needs non-inline formatting. */
    private boolean isNonInline(final List<Token> tokens, final SwitchBlock sw) {
        for (final CaseLabel c : sw.cases) {
            if (caseSpansMultipleLines(tokens, c.colonIdx + 1, c.bodyEnd)) {
                return true;
            }
        }
        return false;
    }

    private boolean caseSpansMultipleLines(final List<Token> tokens, final int from, final int to) {
        final int firstSig = firstSignificantIndex(tokens, from, to);
        final int lastSig = lastSignificantIndex(tokens, from, to);
        if (firstSig < 0) {
            return false; // empty/fallthrough body -- trivially fits on one line
        }
        for (int i = firstSig; i <= lastSig; i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return true;
            }
        }
        return false;
    }

    private int firstSignificantIndex(final List<Token> tokens, final int from, final int to) {
        for (int i = from; i < to; i++) {
            if (!isGap(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int lastSignificantIndex(final List<Token> tokens, final int from, final int to) {
        for (int i = to - 1; i >= from; i--) {
            if (!isGap(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    // ── Non-inline switch (STYLE.md §13) ─────────────────────────────────────────
    /**
     * For every switch where at least one case's body spans multiple lines, ensures a blank
     * line after the switch's opening `{`, after each non-empty case's body (the gap before the
     * next label or the switch's closing `}` -- which is the same gap as "before the closing
     * `}`" for the last case, so that part of STYLE.md §13 falls out for free), and normalizes
     * the indentation of a case body already wrapped in `{ ... }` immediately after its `:` (same
     * line, i.e. already K&R): the body's content is shifted by a constant delta so it sits two
     * levels inside the case label while preserving whatever relative indentation already exists
     * among its lines (so nested constructs inside the case body aren't flattened), and the body's
     * own closing `}` plus every line after it up to the next label is shifted by a separate
     * constant delta to the intermediate (one-level) indentation. A case whose body is NOT already
     * `{ ... }`-wrapped on the same line as its `:` is left completely untouched -- per the
     * resolved design decision, this rule never adds or removes a case's brace wrapper, only
     * normalizes indentation within whatever shape is already present. A fallthrough label with no
     * body content gets no blank line, matching STYLE.md §13's fallthrough worked example.
     */
    public String formatNonInlineSwitches(final List<Token> initialTokens) {
        List<Token> tokens = initialTokens;
        while (true) {
            final List<SwitchBlock> switches = findSwitches(tokens);
            final SwitchBlock target = pickInnermostNeedingWork(tokens, switches);
            if (target == null) {
                return render(tokens, Collections.emptyMap(), Collections.emptyMap());
            }

            final Map<Integer, String> overrides = new HashMap<>();
            final Map<Integer, String> insertAfter = new HashMap<>();
            applyNonInlineBlankLines(tokens, target, insertAfter);
            applyNonInlineCaseIndent(tokens, target, overrides);
            tokens = tokenizer.tokenize(render(tokens, overrides, insertAfter));
        }
    }

    /**
     * Picks the smallest-span (innermost) switch that still has formatting work to do, or null
     * if none remain. A nested switch's [openBrace, closeBrace) range is always strictly
     * contained within its enclosing switch's, so span size is a reliable depth proxy: fixing
     * innermost switches first and re-tokenizing between each fix means an enclosing switch's
     * later body-shift uniformly carries the already-correct nested switch along with it
     * (uniform shifts preserve relative indentation), instead of two independent shifts
     * computed from stale pre-edit positions clobbering each other in a shared overrides map.
     */
    private SwitchBlock pickInnermostNeedingWork(final List<Token> tokens, final List<SwitchBlock> switches) {
        SwitchBlock best = null;
        for (final SwitchBlock sw : switches) {
            if (sw.cases.isEmpty() || !isNonInline(tokens, sw) || !needsWork(tokens, sw)) {
                continue;
            }
            if (best == null || (sw.closeBrace - sw.openBrace) < (best.closeBrace - best.openBrace)) {
                best = sw;
            }
        }
        return best;
    }

    /** True if applying the non-inline blank-line/indent fixes to sw would change anything. */
    private boolean needsWork(final List<Token> tokens, final SwitchBlock sw) {
        final Map<Integer, String> overrides = new HashMap<>();
        final Map<Integer, String> insertAfter = new HashMap<>();
        applyNonInlineBlankLines(tokens, sw, insertAfter);
        applyNonInlineCaseIndent(tokens, sw, overrides);
        return !overrides.isEmpty() || !insertAfter.isEmpty();
    }

    private void applyNonInlineBlankLines(final List<Token> tokens, final SwitchBlock sw,
            final Map<Integer, String> insertAfter) {
        final int firstLabelIdx = sw.cases.get(0).kwIdx;
        ensureBlankLineInGap(tokens, sw.openBrace + 1, firstLabelIdx, insertAfter);

        for (final CaseLabel c : sw.cases) {
            final int contentEnd = lastSignificantIndex(tokens, c.colonIdx + 1, c.bodyEnd);
            if (contentEnd < 0) {
                continue; // empty/fallthrough body -- no blank line (preserves the §13 example)
            }
            ensureBlankLineInGap(tokens, contentEnd + 1, c.bodyEnd, insertAfter);
        }
    }

    /** Ensures the gap [fromIdx, toIdxExclusive) contains a blank line, unless it holds a comment. */
    private void ensureBlankLineInGap(final List<Token> tokens, final int fromIdx,
            final int toIdxExclusive, final Map<Integer, String> insertAfter) {
        boolean hasComment = false;
        int newlineCount = 0;
        int firstNewlineIdx = -1;
        for (int i = fromIdx; i < toIdxExclusive; i++) {
            final Token t = tokens.get(i);
            if (isComment(t)) {
                hasComment = true;
            }
            if (t.type == TokenType.NEWLINE) {
                newlineCount++;
                if (firstNewlineIdx < 0) {
                    firstNewlineIdx = i;
                }
            }
        }
        if (hasComment || newlineCount >= 2) {
            return;
        }
        if (newlineCount == 0) {
            insertAfter.merge(fromIdx - 1, "\n\n", String::concat);
        } else {
            insertAfter.merge(firstNewlineIdx, "\n", String::concat);
        }
    }

    private void applyNonInlineCaseIndent(final List<Token> tokens, final SwitchBlock sw,
            final Map<Integer, String> overrides) {
        final String switchIndent = indentBefore(tokens, sw.switchKwIdx);
        for (final CaseLabel c : sw.cases) {
            final int braceIdx = caseBodyBraceIndex(tokens, c);
            if (braceIdx < 0) {
                continue;
            }
            final int braceClose = matchBrace(tokens, braceIdx);
            if (braceClose < 0 || braceClose >= c.bodyEnd
                    || !containsNewline(tokens, braceIdx, braceClose)) {
                continue;
            }

            final String caseIndent = indentBefore(tokens, c.kwIdx);
            final String unit = deriveUnit(switchIndent, caseIndent);
            final String bodyIndent = caseIndent + unit + unit;
            final String tailIndent = caseIndent + unit;

            int firstBodyLine = -1;
            for (int j = braceIdx; j < braceClose; j++) {
                if (tokens.get(j).type == TokenType.NEWLINE) {
                    firstBodyLine = j + 1;
                    break;
                }
            }
            if (firstBodyLine >= 0 && firstBodyLine < braceClose) {
                final int bodyDelta = bodyIndent.length() - currentLineIndent(tokens, firstBodyLine).length();
                shiftLines(tokens, braceIdx + 1, braceClose, bodyDelta, overrides);
            }

            // Bound the tail shift at the start of the next label's (or closing `}`'s) own line --
            // not at c.bodyEnd itself, which sits mid-line at the next label's keyword and would
            // otherwise pull that label's own leading indentation into this case's tail shift.
            final int tailBound = lineStartOf(tokens, c.bodyEnd);
            final int tailDelta = tailIndent.length() - currentLineIndent(tokens, braceClose).length();
            shiftLineAndFollowing(tokens, braceClose, tailBound, tailDelta, overrides);
        }
    }

    /** The `{` directly after `c`'s `:` (same line, already K&R), or -1 if not in that shape. */
    private int caseBodyBraceIndex(final List<Token> tokens, final CaseLabel c) {
        int i = c.colonIdx + 1;
        while (i < c.bodyEnd) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.NEWLINE) {
                return -1;
            }
            if (!isGap(t)) {
                return isPunct(t, "{") ? i : -1;
            }
            i++;
        }
        return -1;
    }

    /**
     * The project's one-indent-level unit, derived from the difference between the switch's own
     * line indent and its case label's line indent (the case label is always exactly one level
     * deeper than the switch). Falls back to {@link #DEFAULT_INDENT_UNIT} if the case's indent
     * doesn't cleanly extend the switch's (e.g. the switch isn't first on its own line).
     */
    private String deriveUnit(final String switchIndent, final String caseIndent) {
        if (caseIndent.length() > switchIndent.length() && caseIndent.startsWith(switchIndent)) {
            return caseIndent.substring(switchIndent.length());
        }
        return DEFAULT_INDENT_UNIT;
    }

    private boolean containsNewline(final List<Token> tokens, final int from, final int to) {
        for (int j = from; j < to; j++) {
            if (tokens.get(j).type == TokenType.NEWLINE) {
                return true;
            }
        }
        return false;
    }

    /** The leading whitespace text of the line containing tokens.get(anchorIdx), or "" if none. */
    private String currentLineIndent(final List<Token> tokens, final int anchorIdx) {
        return tokens.get(lineStartOf(tokens, anchorIdx)).type == TokenType.WHITESPACE
                ? tokens.get(lineStartOf(tokens, anchorIdx)).text : "";
    }

    /** Index of the first token of the line containing tokens.get(idx). */
    private int lineStartOf(final List<Token> tokens, final int idx) {
        int i = idx;
        while (i > 0 && tokens.get(i - 1).type != TokenType.NEWLINE) {
            i--;
        }
        return i;
    }

    /**
     * Shifts every line found via newline-scanning within [from, to) by delta characters (positive
     * extends the existing indent, negative trims it), preserving each line's indentation relative
     * to the others -- unlike setting every line to one fixed target, this does not flatten any
     * nested construct inside the shifted range.
     */
    private void shiftLines(final List<Token> tokens, final int from, final int to, final int delta,
            final Map<Integer, String> overrides) {
        if (delta == 0) {
            return;
        }
        for (int j = from; j < to; j++) {
            if (tokens.get(j).type == TokenType.NEWLINE) {
                final int next = j + 1;
                if (next < to && tokens.get(next).type != TokenType.NEWLINE) {
                    shiftLineIndent(tokens, next, delta, overrides);
                }
            }
        }
    }

    /** Like {@link #shiftLines}, but also shifts anchorIdx's own line (found by walking backward). */
    private void shiftLineAndFollowing(final List<Token> tokens, final int anchorIdx, final int to,
            final int delta, final Map<Integer, String> overrides) {
        if (delta == 0) {
            return;
        }
        shiftLineIndent(tokens, lineStartOf(tokens, anchorIdx), delta, overrides);
        shiftLines(tokens, anchorIdx, to, delta, overrides);
    }

    private void shiftLineIndent(final List<Token> tokens, final int lineStart, final int delta,
            final Map<Integer, String> overrides) {
        final Token t = tokens.get(lineStart);
        final String current = t.type == TokenType.WHITESPACE ? t.text : "";
        final char unitChar = current.isEmpty() ? ' ' : current.charAt(0);
        final int newLen = Math.max(0, current.length() + delta);
        final StringBuilder sb = new StringBuilder();
        for (int k = 0; k < newLen; k++) {
            sb.append(unitChar);
        }
        if (t.type == TokenType.WHITESPACE) {
            overrides.put(lineStart, sb.toString());
        } else if (newLen > 0) {
            overrides.put(lineStart, sb.toString() + t.text);
        }
    }

    /** The leading whitespace of the line containing the token at idx, or "" if it isn't first on its line. */
    private String indentBefore(final List<Token> tokens, final int idx) {
        final StringBuilder indent = new StringBuilder();
        int i = idx - 1;
        while (i >= 0 && tokens.get(i).type == TokenType.WHITESPACE) {
            indent.insert(0, tokens.get(i).text);
            i--;
        }
        return (i < 0 || tokens.get(i).type == TokenType.NEWLINE) ? indent.toString() : "";
    }

    // ── Inline switch (STYLE.md §13) ─────────────────────────────────────────────
    /**
     * For every switch where every case's body fits on one line, aligns the case label's `:`
     * column, the statement content column (splitting a bare `name(args);` statement into
     * name/`(`/args/`)` sub-columns when a row happens to have that shape -- STYLE.md's worked
     * example mixes call-shaped and non-call-shaped rows in the same aligned group, so only rows
     * that ARE call-shaped get the sub-column treatment; others contribute their literal statement
     * text as one opaque cell, still aligned against the call-shaped rows' assembled width), and
     * the trailing `;`/`break;` column. Blank lines between cases are never touched (STYLE.md says
     * preserve as-is -- no mechanical add/remove). If any case in a switch does not match the
     * simple "one statement, optional trailing `break;`" shape this rule can confidently
     * reconstruct, the entire switch is left byte-for-byte untouched -- same conservative,
     * no-partial-rewrite posture as the rest of this rule.
     */
    public String alignInlineSwitches(final List<Token> tokens) {
        final List<SwitchBlock> switches = findSwitches(tokens);
        final Map<Integer, String> overrides = new HashMap<>();

        for (final SwitchBlock sw : switches) {
            if (sw.cases.isEmpty() || isNonInline(tokens, sw)) {
                continue;
            }
            final List<CaseRow> rows = classifyAll(tokens, sw);
            if (rows == null) {
                continue;
            }
            applyInlineAlignment(rows, overrides);
        }

        return render(tokens, overrides, Collections.emptyMap());
    }

    /** One classified inline case row, ready for column alignment. */
    private static final class CaseRow {
        final int kwIdx;
        final int lastIdx; // last token belonging to this row's content (colonIdx if none)
        final String label; // "case EXPR" or "default" -- no baked-in trailing space yet
        final boolean hasContent;
        final boolean callShaped;
        final String name; // valid iff callShaped
        final String args; // valid iff callShaped
        final String plain; // valid iff hasContent && !callShaped
        final boolean hasBreak;

        CaseRow(final int kwIdx, final int lastIdx, final String label, final boolean hasContent,
                final boolean callShaped, final String name, final String args, final String plain,
                final boolean hasBreak) {
            this.kwIdx = kwIdx;
            this.lastIdx = lastIdx;
            this.label = label;
            this.hasContent = hasContent;
            this.callShaped = callShaped;
            this.name = name;
            this.args = args;
            this.plain = plain;
            this.hasBreak = hasBreak;
        }
    }

    /** Classifies every case in sw, or returns null if any one doesn't fit a recognized shape. */
    private List<CaseRow> classifyAll(final List<Token> tokens, final SwitchBlock sw) {
        final List<CaseRow> rows = new ArrayList<>(sw.cases.size());
        for (final CaseLabel c : sw.cases) {
            final CaseRow row = classify(tokens, c);
            if (row == null) {
                return null;
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * Recognizes a case body that is either empty (fallthrough), exactly `break;`, or exactly one
     * top-level-`;`-terminated statement optionally followed by `break;` and nothing else --
     * anything more exotic (multiple statements, a brace-wrapped body, a trailing comment) returns
     * null so the caller skips the whole switch rather than guessing.
     */
    private CaseRow classify(final List<Token> tokens, final CaseLabel c) {
        final boolean isDefault = "default".equals(tokens.get(c.kwIdx).text);
        final String label = isDefault ? "default"
                : "case " + literalSlice(tokens, c.kwIdx + 1, c.colonIdx).trim();

        final int from = c.colonIdx + 1;
        final int to = c.bodyEnd;
        final int firstSig = firstSignificantIndex(tokens, from, to);
        if (firstSig < 0) {
            return new CaseRow(c.kwIdx, c.colonIdx, label, false, false, null, null, null, false);
        }

        if (isKeyword(tokens.get(firstSig), "break")) {
            final int semi = skipNonSignificant(tokens, firstSig + 1);
            if (semi >= to || !isPunct(tokens.get(semi), ";")
                    || firstSignificantIndex(tokens, semi + 1, to) >= 0) {
                return null;
            }
            return new CaseRow(c.kwIdx, semi, label, false, false, null, null, null, true);
        }

        final int semi = findTopLevelSemicolon(tokens, firstSig, to);
        if (semi < 0) {
            return null;
        }
        final int realEnd = lastSignificantIndex(tokens, firstSig, semi);

        boolean callShaped = false;
        String name = null;
        String args = null;
        if (tokens.get(firstSig).type == TokenType.IDENTIFIER) {
            final int openParen = skipNonSignificant(tokens, firstSig + 1);
            if (openParen < semi && isPunct(tokens.get(openParen), "(")
                    && matchParen(tokens, openParen) == realEnd) {
                callShaped = true;
                name = tokens.get(firstSig).text;
                args = literalSlice(tokens, openParen + 1, realEnd);
            }
        }
        final String plain = callShaped ? null : literalSlice(tokens, firstSig, realEnd + 1);

        final int afterSemi = firstSignificantIndex(tokens, semi + 1, to);
        if (afterSemi < 0) {
            return new CaseRow(c.kwIdx, semi, label, true, callShaped, name, args, plain, false);
        }
        if (isKeyword(tokens.get(afterSemi), "break")) {
            final int semi2 = skipNonSignificant(tokens, afterSemi + 1);
            if (semi2 >= to || !isPunct(tokens.get(semi2), ";")
                    || firstSignificantIndex(tokens, semi2 + 1, to) >= 0) {
                return null;
            }
            return new CaseRow(c.kwIdx, semi2, label, true, callShaped, name, args, plain, true);
        }
        return null;
    }

    /** The first top-level (paren/bracket-depth 0) `;` from `from` (inclusive) up to `to`, or -1. */
    private int findTopLevelSemicolon(final List<Token> tokens, final int from, final int to) {
        int depth = 0;
        for (int i = from; i < to; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]")) {
                depth--;
            } else if (depth == 0 && isPunct(t, ";")) {
                return i;
            }
        }
        return -1;
    }

    /** Concatenates raw token text over [from, to), preserving whatever spacing already exists. */
    private String literalSlice(final List<Token> tokens, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(tokens.get(i).text);
        }
        return sb.toString();
    }

    private boolean isKeyword(final Token t, final String text) {
        return t.type == TokenType.KEYWORD && text.equals(t.text);
    }

    /**
     * Builds the aligned replacement text for every row and writes it into overrides: the
     * call-shaped rows among them are name/`(`/args/`)`-aligned via a nested {@link ColumnGrid}
     * first, then every row's resulting content cell is aligned against ALL rows (call-shaped or
     * not) via the outer grid, alongside the label cell -- reproducing STYLE.md §13's mixed
     * call/assignment worked example exactly. The label cell always carries one baked-in trailing
     * space before its grid padding is computed, since `ColumnGrid` only adds padding when a cell
     * is shorter than the column's widest entry -- without that baked-in space, the row with the
     * single widest label would render with no gap at all before its `:`.
     */
    private void applyInlineAlignment(final List<CaseRow> rows, final Map<Integer, String> overrides) {
        final ColumnGrid callGrid = new ColumnGrid();
        for (final CaseRow row : rows) {
            if (row.callShaped) {
                // Trailing "" keeps args from being the last cell, so ColumnGrid's
                // ragged-row rule doesn't skip padding it when args is empty (e.g. "doA()").
                callGrid.addRow(new String[] {row.name, row.args, ""});
            }
        }
        final List<String[]> callPadded = callGrid.flush();

        final String[] content = new String[rows.size()];
        int callIdx = 0;
        for (int i = 0; i < rows.size(); i++) {
            final CaseRow row = rows.get(i);
            if (row.callShaped) {
                final String[] cell = callPadded.get(callIdx++);
                content[i] = cell[0] + "(" + cell[1] + ")";
            } else if (row.hasContent) {
                content[i] = row.plain;
            }
        }

        final ColumnGrid outerGrid = new ColumnGrid();
        for (int i = 0; i < rows.size(); i++) {
            final CaseRow row = rows.get(i);
            final String label = row.label + " ";
            if (!row.hasContent && !row.hasBreak) {
                outerGrid.addRow(new String[] {label});
            } else {
                final String terminator = (row.hasContent ? ";" : "") + (row.hasBreak
                        ? (row.hasContent ? " break;" : "break;") : "");
                outerGrid.addRow(new String[] {label, row.hasContent ? content[i] : "", terminator});
            }
        }
        final List<String[]> outerPadded = outerGrid.flush();

        for (int i = 0; i < rows.size(); i++) {
            final CaseRow row = rows.get(i);
            final String[] cell = outerPadded.get(i);
            final String text = cell.length == 1 ? cell[0] + ":" : cell[0] + ":" + " " + cell[1] + cell[2];
            overrides.put(row.kwIdx, text);
            for (int k = row.kwIdx + 1; k <= row.lastIdx; k++) {
                overrides.put(k, "");
            }
        }
    }

    // ── Rendering ──────────────────────────────────────────────────────────────
    private String render(final List<Token> tokens, final Map<Integer, String> overrides,
            final Map<Integer, String> insertAfter) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            final String override = overrides.get(i);
            out.append(override != null ? override : tokens.get(i).text);
            final String extra = insertAfter.get(i);
            if (extra != null) {
                out.append(extra);
            }
        }
        return out.toString();
    }

    // ── Token-matching helpers ────────────────────────────────────────────────
    private int matchBrace(final List<Token> tokens, final int openIdx) {
        int depth = 1;
        int i = openIdx + 1;
        final int n = tokens.size();
        while (i < n && depth > 0) {
            if (isPunct(tokens.get(i), "{")) {
                depth++;
            } else if (isPunct(tokens.get(i), "}")) {
                depth--;
            }
            i++;
        }
        return depth == 0 ? i - 1 : -1;
    }

    private int matchParen(final List<Token> tokens, final int openIdx) {
        int depth = 1;
        int i = openIdx + 1;
        final int n = tokens.size();
        while (i < n && depth > 0) {
            if (isPunct(tokens.get(i), "(") || isPunct(tokens.get(i), "[")) {
                depth++;
            } else if (isPunct(tokens.get(i), ")") || isPunct(tokens.get(i), "]")) {
                depth--;
            }
            i++;
        }
        return depth == 0 ? i - 1 : -1;
    }

    private int skipNonSignificant(final List<Token> tokens, final int from) {
        final int n = tokens.size();
        int i = from;
        while (i < n && isGap(tokens.get(i))) {
            i++;
        }
        return i;
    }

    private boolean isGap(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    private boolean isComment(final Token t) {
        return t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    private boolean isPunct(final Token t, final String text) {
        return t.type == TokenType.PUNCT && text.equals(t.text);
    }

    private boolean isOp(final Token t, final String text) {
        return t.type == TokenType.OP && text.equals(t.text);
    }
}
