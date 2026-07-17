/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore;
import com.jxmake.formatter.tokenizer.TokenizerCurly;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isComment;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isKeyword;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwitchRule {

    // Fallback unit-width when a switch's own line indentation can't be used to derive the
    // project's actual indent unit (see deriveUnit) -- STYLE.md §1 default.
    private static final int DEFAULT_INDENT_WIDTH = MiscRule.DEFAULT_INDENT_WIDTH;

    private final TokenizerCurly tokenizer;
    private final int lineLengthLimit;
    private final String defaultIndentUnit;

    public SwitchRule(final Lang lang) {
        this(lang, MiscRule.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public SwitchRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, DEFAULT_INDENT_WIDTH);
    }

    public SwitchRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this.tokenizer = new TokenizerCurly(lang);
        this.lineLengthLimit = lineLengthLimit;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentWidth; i++) {
            sb.append(' ');
        }
        this.defaultIndentUnit = sb.toString();
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
        // A `//` line comment appearing before the body's first real statement token (e.g. right
        // after the case label, `case 1: // note`) always forces that statement onto the next
        // physical line -- a `//` comment can never be followed by more code on the same line --
        // so such a case can never be rendered in the single-line inline form, unlike a `/* */`
        // block comment in the same position (`case 1: /* note */ return 1;`), which can.
        for (int i = from; i < firstSig; i++) {
            if (tokens.get(i).type == TokenType.COMMENT_LINE) {
                return true;
            }
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
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int lastSignificantIndex(final List<Token> tokens, final int from, final int to) {
        for (int i = to - 1; i >= from; i--) {
            if (!isGapToken(tokens.get(i))) {
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
            if (sw.cases.isEmpty() || !isNonInline(tokens, sw) || !needsWork(tokens, sw)
                    || anyFrozen(tokens, sw.openBrace, sw.closeBrace + 1)) {
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

    /** Ensures the gap [fromIdx, toIdxExclusive) contains a blank line. A standalone comment
     *  glued to the label/statement that follows it (e.g. `// comment before case` right before
     *  `case 1:`) stays right where it is -- the blank line is still guaranteed, but only in the
     *  sub-gap before that first comment, same as {@code BlockStructureRule.ensureBlankLine}'s
     *  own comment-preserving precedent (never relocate a comment, just still ensure the blank
     *  line around it). Only a comment that starts its own new line qualifies for that treatment
     *  -- a comment trailing on the same line as the preceding content (e.g. `} // if`, a
     *  closing-comment left by an earlier format pass) is that content's own trailing comment,
     *  not a leading comment for what follows, and must not be treated as a blocking anchor here
     *  (doing so previously split it from its `}` onto its own orphaned line). */
    private void ensureBlankLineInGap(final List<Token> tokens, final int fromIdx,
            final int toIdxExclusive, final Map<Integer, String> insertAfter) {
        int firstCommentIdx = -1;
        for (int i = fromIdx; i < toIdxExclusive; i++) {
            if (isComment(tokens.get(i)) && startsOwnLine(tokens, i, fromIdx - 1)) {
                firstCommentIdx = i;
                break;
            }
        }
        final int scanEnd = firstCommentIdx < 0 ? toIdxExclusive : firstCommentIdx;
        int newlineCount = 0;
        int firstNewlineIdx = -1;
        for (int i = fromIdx; i < scanEnd; i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.NEWLINE) {
                newlineCount++;
                if (firstNewlineIdx < 0) {
                    firstNewlineIdx = i;
                }
            }
        }
        if (newlineCount >= 2) {
            return;
        }
        if (newlineCount == 0) {
            insertAfter.merge(fromIdx - 1, "\n\n", String::concat);
        } else {
            insertAfter.merge(firstNewlineIdx, "\n", String::concat);
        }
    }

    /** True if the token at {@code idx} is the first thing on its own line: scanning backward
     *  from it, only WHITESPACE is found before hitting a NEWLINE or {@code floor} (the last
     *  real-content token before the gap being searched, guaranteed non-whitespace/non-newline,
     *  so reaching it without a NEWLINE means {@code idx} is still on that content's own line). */
    private boolean startsOwnLine(final List<Token> tokens, final int idx, final int floor) {
        for (int i = idx - 1; i >= floor; i--) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.NEWLINE) {
                return true;
            }
            if (t.type != TokenType.WHITESPACE) {
                return false;
            }
        }
        return false;
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
            if (!isGapToken(t)) {
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
        return defaultIndentUnit;
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
            if (sw.cases.isEmpty() || isNonInline(tokens, sw)
                    || anyFrozen(tokens, sw.openBrace, sw.closeBrace + 1)) {
                continue;
            }
            final List<CaseRow> rows = classifyAll(tokens, sw);
            if (rows == null) {
                continue;
            }
            applyInlineAlignment(tokens, rows, overrides);
        }

        return render(tokens, overrides, Collections.emptyMap());
    }

    // ── Fallthrough marking (STYLE.md §13) ───────────────────────────────────────
    /**
     * Inserts a `/* FALL-THROUGH *<i></i>/` comment right after the `:` of every case whose body is
     * completely empty and which is not the switch's last case (an empty last case has nothing to
     * fall through into). Works identically for inline and non-inline switches -- it only touches
     * the single token immediately after the `:`, never the surrounding spacing -- so non-inline's
     * existing no-space-before-`:` convention and inline's `:`-column alignment (handled separately
     * by {@link #alignInlineSwitches}, which recognizes this exact marker on a later pass) both fall
     * out for free. Already-marked cases are detected via {@link #findFallthroughMarker} and left
     * alone, making this idempotent; a case with an unrelated comment in its empty body is also left
     * alone rather than guessing where the marker should go relative to it.
     */
    public String markFallthrough(final List<Token> tokens) {
        final List<SwitchBlock> switches = findSwitches(tokens);
        final Map<Integer, String> overrides = new HashMap<>();

        for (final SwitchBlock sw : switches) {
            for (int i = 0; i < sw.cases.size() - 1; i++) {
                final CaseLabel c = sw.cases.get(i);
                final int from = c.colonIdx + 1;
                final int to = c.bodyEnd;
                if (firstSignificantIndex(tokens, from, to) >= 0) {
                    continue; // has real content -- not a fallthrough candidate
                }
                if (findFallthroughMarker(tokens, from, to) != -1) {
                    continue; // already marked, or an unrelated comment is present -- leave it alone
                }
                if (tokens.get(c.colonIdx).frozen) {
                    continue;
                }
                overrides.put(c.colonIdx, tokens.get(c.colonIdx).text + " /* FALL-THROUGH */");
            }
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
        final boolean fallthrough; // empty body, not the switch's last case

        CaseRow(final int kwIdx, final int lastIdx, final String label, final boolean hasContent,
                final boolean callShaped, final String name, final String args, final String plain,
                final boolean hasBreak, final boolean fallthrough) {
            this.kwIdx = kwIdx;
            this.lastIdx = lastIdx;
            this.label = label;
            this.hasContent = hasContent;
            this.callShaped = callShaped;
            this.name = name;
            this.args = args;
            this.plain = plain;
            this.hasBreak = hasBreak;
            this.fallthrough = fallthrough;
        }
    }

    /** Classifies every case in sw, or returns null if any one doesn't fit a recognized shape. */
    private List<CaseRow> classifyAll(final List<Token> tokens, final SwitchBlock sw) {
        final List<CaseRow> rows = new ArrayList<>(sw.cases.size());
        for (int i = 0; i < sw.cases.size(); i++) {
            final CaseRow row = classify(tokens, sw.cases.get(i), i == sw.cases.size() - 1);
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
     * anything more exotic (multiple statements, a brace-wrapped body) returns null so the caller
     * skips the whole switch rather than guessing. Any comment found in the body other than an
     * already-present fallthrough marker (see {@link #findFallthroughMarker}) also returns null --
     * this rule has no way to decide where a user's own comment belongs once the body is rewritten,
     * so it leaves the whole switch untouched rather than silently dropping it.
     */
    private CaseRow classify(final List<Token> tokens, final CaseLabel c, final boolean isLast) {
        final boolean isDefault = "default".equals(tokens.get(c.kwIdx).text);
        final String label = isDefault ? "default"
                : "case " + literalSlice(tokens, c.kwIdx + 1, c.colonIdx).trim();

        final int from = c.colonIdx + 1;
        final int to = c.bodyEnd;
        final int markerIdx = findFallthroughMarker(tokens, from, to);
        if (markerIdx == -2) {
            return null; // an unrelated comment is present -- don't guess, leave the switch alone
        }

        final int firstSig = firstSignificantIndex(tokens, from, to);
        if (firstSig < 0) {
            final int lastIdx = markerIdx >= 0 ? markerIdx : c.colonIdx;
            return new CaseRow(c.kwIdx, lastIdx, label, false, false, null, null, null, false, !isLast);
        }

        if (isKeyword(tokens.get(firstSig), "break")) {
            final int semi = skipNonSignificant(tokens, firstSig + 1);
            if (semi >= to || !isPunct(tokens.get(semi), ";")
                    || firstSignificantIndex(tokens, semi + 1, to) >= 0) {
                return null;
            }
            // Treat break as plain content so it lands in the content column (aligned with
            // return/call statements), not as a trailing terminal that would precede its padding.
            return new CaseRow(c.kwIdx, semi, label, true, false, null, null, "break", false, false);
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
            return new CaseRow(c.kwIdx, semi, label, true, callShaped, name, args, plain, false, false);
        }
        if (isKeyword(tokens.get(afterSemi), "break")) {
            final int semi2 = skipNonSignificant(tokens, afterSemi + 1);
            if (semi2 >= to || !isPunct(tokens.get(semi2), ";")
                    || firstSignificantIndex(tokens, semi2 + 1, to) >= 0) {
                return null;
            }
            return new CaseRow(c.kwIdx, semi2, label, true, callShaped, name, args, plain, true, false);
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

    /**
     * Scans [from, to) for a fallthrough marker comment (STYLE.md §13's "FALL-THROUGH" text):
     * returns its index if found alone, -1 if there is no comment at all, or -2 if a comment is
     * present that does not match (more than one comment, or text that doesn't contain
     * "FALL-THROUGH") -- callers treat -2 as "don't guess, leave this case alone" so a user's own
     * comment is never silently dropped.
     */
    private int findFallthroughMarker(final List<Token> tokens, final int from, final int to) {
        int found = -1;
        for (int i = from; i < to; i++) {
            final Token t = tokens.get(i);
            if (isComment(t)) {
                if (found >= 0 || !t.text.contains("FALL-THROUGH")) {
                    return -2;
                }
                found = i;
            }
        }
        return found;
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
     *
     * <p>Column-padding a row can push it past {@link #lineLengthLimit} even when its original,
     * unpadded form fit -- and since this rule never breaks a row across lines (it only aligns
     * columns), a padded row that overflows has nowhere else for the extra width to go. Left
     * unchecked, that overflowing row would then be re-measured -- and re-broken -- by
     * `MiscRule.enforceCallLineBreaking` the next time the file is formatted (it runs earlier in
     * the pipeline than this pass and never re-runs after it), which is not idempotent: this
     * pass would see the resulting multi-line call and, no longer recognizing the case as
     * one-statement-per-line, leave the switch alone -- silently un-fixing its own output the
     * second time round. So every row's final rendered length is checked against the limit before
     * any override is committed; if even one row would overflow, the whole switch is left
     * byte-for-byte untouched instead, same conservative posture as every other
     * can't-confidently-reconstruct case in this class.
     */
    private void applyInlineAlignment(final List<Token> tokens, final List<CaseRow> rows,
            final Map<Integer, String> overrides) {
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
                // Add an empty sentinel cell so ColumnGrid's ragged-row rule pads the label
                // column to the same width as rows that have content (the sentinel is the
                // last cell in its row and therefore never padded itself).
                outerGrid.addRow(new String[] {label, ""});
            } else {
                final String terminator = (row.hasContent ? ";" : "") + (row.hasBreak
                        ? (row.hasContent ? " break;" : "break;") : "");
                outerGrid.addRow(new String[] {label, row.hasContent ? content[i] : "", terminator});
            }
        }
        final List<String[]> outerPadded = outerGrid.flush();

        final String[] rowText = new String[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            final CaseRow row = rows.get(i);
            final String[] cell = outerPadded.get(i);
            if (!row.hasContent && !row.hasBreak) {
                // 2-cell row: cell[1] is empty sentinel used only to force label padding.
                rowText[i] = cell[0] + ":" + (row.fallthrough ? " /* FALL-THROUGH */" : "");
            } else {
                rowText[i] = cell[0] + ":" + " " + cell[1] + cell[2];
            }
            final int indentLen = indentBefore(tokens, row.kwIdx).length();
            if (indentLen + rowText[i].length() > lineLengthLimit) {
                return;
            }
        }

        for (int i = 0; i < rows.size(); i++) {
            final CaseRow row = rows.get(i);
            final String text = rowText[i];
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
        while (i < n && isGapToken(tokens.get(i))) {
            i++;
        }
        return i;
    }

    private boolean anyFrozen(final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (tokens.get(i).frozen) {
                return true;
            }
        }
        return false;
    }

}
