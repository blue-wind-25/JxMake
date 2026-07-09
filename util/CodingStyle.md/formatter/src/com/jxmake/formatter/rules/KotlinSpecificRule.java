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

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isComment;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Kotlin-only STYLE_KOTLIN.md/STYLE_KOTLIN2.md sections flagged "(c)" in `STATE_KOTLIN.md`'s
 * Step 1 scoping table -- none of this is reusable from the shared rule classes. See that table
 * for why each section lands here rather than in a shared file.
 */
public class KotlinSpecificRule {

    private final Lang lang;
    private final int lineLengthLimit;

    /** One indentation level, used by {@link #enforceWhereClausePlacement} when wrapping a
     *  trailing `where` clause to its own line -- built from the configured `indent-size` (see
     *  the constructor), not a hardcoded literal, same precedent as `CppSpecificRule.indentUnit`. */
    private final String indentUnit;

    public KotlinSpecificRule(final Lang lang) {
        this(lang, MiscRule.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public KotlinSpecificRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, MiscRule.DEFAULT_INDENT_WIDTH);
    }

    public KotlinSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentWidth; i++) {
            sb.append(' ');
        }
        this.indentUnit = sb.toString();
    }

    // ── §4 `when` expression ─────────────────────────────────────────────────────
    /** One `EXPR -> body` / `else -> body` branch found directly (brace/paren depth 0 relative to
     *  the `when`'s own `{`) inside a `when` body. */
    private static final class WhenBranch {
        final int labelStart;
        final int bodyStart;
        final int bodyEnd; // inclusive, last significant token of the body
        final String label; // raw "EXPR" / "else" text, whitespace-collapsed and trimmed

        WhenBranch(final int labelStart, final int bodyStart, final int bodyEnd, final String label) {
            this.labelStart = labelStart;
            this.bodyStart = bodyStart;
            this.bodyEnd = bodyEnd;
            this.label = label;
        }
    }

    /**
     * STYLE_KOTLIN.md §4: for every `when [(subject)] { ... }`, ensures a blank line right after
     * the opening `{` (before the first branch) and right before the closing `}` (matching
     * STYLE.md §13's switch-statement blank-line treatment); appends a `// when subject` closing
     * comment (or bare `// when` for a subject-less `when { ... }`) after the `}`, per STYLE.md
     * §7's construct-labeling rule; and column-aligns `->` across every branch. Unlike
     * {@code JavaSpecificRule.enforceSwitchExpressionArrowAlignment}'s all-or-nothing bail-out on
     * any block body, STYLE_KOTLIN.md §4's own worked example aligns `->` even when one branch has
     * a block body (`2    -> {`), so every branch is aligned independently here -- this is a
     * genuine behavioral difference from Java's rule, not an oversight, and is why this lives here
     * rather than as an extension of that method (see `STATE_KOTLIN.md`'s Step 1 note for §4).
     *
     * <p>Branch boundaries can't be found the way Java's `case`/`default` keyword marks each label
     * (Kotlin's `when` branches have no such leading keyword, just a bare condition expression), so
     * this method requires one branch per physical line to find the boundary unambiguously (a
     * depth-0 `->` starts a branch's body; a depth-0 `NEWLINE` after a non-block body, or a
     * block body's own matching `}`, ends it) -- exactly the shape shown in every
     * STYLE_KOTLIN.md §4 example. A `when` that doesn't fit this shape (findWhenBranches returns
     * null) is left completely untouched, same conservative posture used throughout this codebase.
     */
    public String formatWhenExpressions(final List<Token> tokens) {
        final Map<Integer, String> overrides = new HashMap<>();
        final Map<Integer, String> insertAfter = new HashMap<>();

        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type != TokenType.KEYWORD || !"when".equals(t.text)) {
                continue;
            }
            int j = nextSignificantIndex(tokens, i + 1);
            if (j < 0) {
                continue;
            }
            String subject = null;
            if (isPunct(tokens.get(j), "(")) {
                final int closeParen = matchParenForward(tokens, j);
                if (closeParen < 0) {
                    continue;
                }
                subject = literalSlice(tokens, j + 1, closeParen).trim();
                j = nextSignificantIndex(tokens, closeParen + 1);
            }
            if (j < 0 || !isPunct(tokens.get(j), "{")) {
                continue;
            }
            final int openBrace = j;
            final int closeBrace = matchBraceForward(tokens, openBrace);
            if (closeBrace < 0 || anyFrozen(tokens, openBrace, closeBrace + 1)) {
                continue;
            }

            final List<WhenBranch> branches = findWhenBranches(tokens, openBrace, closeBrace);
            if (branches == null || branches.isEmpty()) {
                continue;
            }

            ensureBlankLineInGap(tokens, openBrace + 1, branches.get(0).labelStart, insertAfter);
            final int lastBodyEnd = branches.get(branches.size() - 1).bodyEnd;
            ensureBlankLineInGap(tokens, lastBodyEnd + 1, closeBrace, insertAfter);
            applyClosingComment(tokens, closeBrace, subject, overrides);
            applyArrowAlignment(tokens, branches, overrides);
        }

        return render(tokens, overrides, insertAfter);
    }

    /** Finds every `when` branch in [openBrace+1, closeBrace), or {@code null} if the body
     *  doesn't fit the required one-branch-per-line shape (see {@link #formatWhenExpressions}). */
    private List<WhenBranch> findWhenBranches(final List<Token> tokens, final int openBrace,
            final int closeBrace) {
        final List<WhenBranch> branches = new ArrayList<>();
        int pos = nextSignificantIndex(tokens, openBrace + 1);
        while (pos >= 0 && pos < closeBrace) {
            final int arrowIdx = findTopLevelArrow(tokens, pos, closeBrace);
            if (arrowIdx < 0) {
                return null;
            }
            final String label = literalSlice(tokens, pos, arrowIdx).trim();
            final int bodyStart = nextSignificantIndex(tokens, arrowIdx + 1);
            if (bodyStart < 0 || bodyStart >= closeBrace) {
                return null;
            }

            final int bodyEnd;
            int nextPos;
            if (isPunct(tokens.get(bodyStart), "{")) {
                final int bodyClose = matchBraceForward(tokens, bodyStart);
                if (bodyClose < 0 || bodyClose > closeBrace) {
                    return null;
                }
                bodyEnd = bodyClose;
                nextPos = nextSignificantIndex(tokens, bodyClose + 1);
            } else {
                final int nlIdx = findTopLevelNewline(tokens, bodyStart, closeBrace);
                if (nlIdx < 0) {
                    bodyEnd = lastSignificantIndex(tokens, bodyStart, closeBrace);
                    nextPos = closeBrace;
                } else {
                    bodyEnd = lastSignificantIndex(tokens, bodyStart, nlIdx);
                    nextPos = nextSignificantIndex(tokens, nlIdx + 1);
                }
            }
            branches.add(new WhenBranch(pos, bodyStart, bodyEnd, label));
            if (nextPos < 0 || nextPos >= closeBrace) {
                break;
            }
            pos = nextPos;
        }
        return branches;
    }

    /** The first top-level (paren/bracket/brace-depth 0 relative to `from`) `->` OP token in
     *  [from, limit), or -1 if none is found -- a nested `{`/`(` (e.g. a lambda inside the branch
     *  condition) is skipped over so its own `->` is never mistaken for the branch's. */
    private int findTopLevelArrow(final List<Token> tokens, final int from, final int limit) {
        int depth = 0;
        for (int i = from; i < limit; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                depth--;
            } else if (depth == 0 && isOp(t, "->")) {
                return i;
            }
        }
        return -1;
    }

    /** The first top-level NEWLINE token in [from, limit), or -1 if none is found. */
    private int findTopLevelNewline(final List<Token> tokens, final int from, final int limit) {
        int depth = 0;
        for (int i = from; i < limit; i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                depth--;
            } else if (depth == 0 && t.type == TokenType.NEWLINE) {
                return i;
            }
        }
        return -1;
    }

    /** Guarantees the gap [fromIdx, toExclusive) contains a blank line, same mechanism as
     *  {@code SwitchRule.ensureBlankLineInGap}. A comment anywhere in the gap blocks the rewrite
     *  for that occurrence entirely -- conservative, since deciding where the blank line belongs
     *  relative to the comment isn't needed for this method's narrower callers (both call sites
     *  only ever cover a single-line gap, unlike {@code SwitchRule}'s per-comment-anchored variant). */
    private void ensureBlankLineInGap(final List<Token> tokens, final int fromIdx,
            final int toExclusive, final Map<Integer, String> insertAfter) {
        for (int i = fromIdx; i < toExclusive; i++) {
            if (isComment(tokens.get(i))) {
                return;
            }
        }
        int newlineCount = 0;
        int firstNewlineIdx = -1;
        for (int i = fromIdx; i < toExclusive; i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
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

    /** Appends `// when subject` (or bare `// when` if {@code subject} is null/empty) after
     *  {@code closeBrace}, unless something other than whitespace already follows on the same
     *  line (existing content or a comment -- left alone rather than risking corruption/duplication,
     *  same posture as {@code BlockStructureRule.addClosingComments}). */
    private void applyClosingComment(final List<Token> tokens, final int closeBrace,
            final String subject, final Map<Integer, String> overrides) {
        int i = closeBrace + 1;
        while (i < tokens.size() && tokens.get(i).type == TokenType.WHITESPACE) {
            i++;
        }
        if (i < tokens.size() && tokens.get(i).type != TokenType.NEWLINE) {
            return;
        }
        if (tokens.get(closeBrace).frozen) {
            return;
        }
        final String label = subject != null && !subject.isEmpty() ? "when " + subject : "when";
        overrides.put(closeBrace, tokens.get(closeBrace).text + " // " + label);
    }

    /** Pads every branch's label to the widest in {@code branches} (trailing-cell trick, same
     *  precedent as {@code SwitchRule.applyInlineAlignment}'s label cell) and rewrites only the
     *  label+arrow span -- body content from {@code bodyStart} onward is left untouched. Skips
     *  (leaves byte-for-byte untouched) any individual branch whose padded label would push its
     *  own same-line body content past {@code lineLengthLimit}, same predict-before-committing
     *  posture as {@code JavaSpecificRule.applyArrowAlignment}. */
    private void applyArrowAlignment(final List<Token> tokens, final List<WhenBranch> branches,
            final Map<Integer, String> overrides) {
        final ColumnGrid grid = new ColumnGrid();
        for (final WhenBranch b : branches) {
            grid.addRow(new String[] {b.label + " ", ""});
        }
        final List<String[]> padded = grid.flush();

        for (int i = 0; i < branches.size(); i++) {
            final WhenBranch b = branches.get(i);
            final String labelPart = padded.get(i)[0] + "-> ";
            final int sameLineEnd = firstNewlineOrEnd(tokens, b.bodyStart, b.bodyEnd + 1);
            final String bodySameLine = literalSlice(tokens, b.bodyStart, sameLineEnd);
            final int indent = lineIndentWidth(tokens, b.labelStart);
            if (indent + labelPart.length() + bodySameLine.length() > lineLengthLimit) {
                continue;
            }
            overrides.put(b.labelStart, labelPart);
            for (int k = b.labelStart + 1; k < b.bodyStart; k++) {
                overrides.put(k, "");
            }
        }
    }

    /** The index of the first NEWLINE token in [from, limit), or {@code limit} if none is found. */
    private int firstNewlineOrEnd(final List<Token> tokens, final int from, final int limit) {
        for (int i = from; i < limit; i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return i;
            }
        }
        return limit;
    }

    /** Total text length of the run of WHITESPACE tokens immediately preceding {@code idx} --
     *  the leading indentation of {@code idx}'s own physical line (0 if {@code idx} isn't first
     *  on its line). */
    private int lineIndentWidth(final List<Token> tokens, final int idx) {
        int width = 0;
        for (int i = idx - 1; i >= 0 && tokens.get(i).type == TokenType.WHITESPACE; i--) {
            width += tokens.get(i).text.length();
        }
        return width;
    }

    /** Concatenates raw token text over [from, to), preserving whatever spacing already exists. */
    private String literalSlice(final List<Token> tokens, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(tokens.get(i).text);
        }
        return sb.toString();
    }

    private int nextSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from; i < tokens.size(); i++) {
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
        return from;
    }

    private int matchParenForward(final List<Token> tokens, final int openIdx) {
        int depth = 1;
        int i = openIdx + 1;
        final int n = tokens.size();
        while (i < n && depth > 0) {
            if (isPunct(tokens.get(i), "(")) {
                depth++;
            } else if (isPunct(tokens.get(i), ")")) {
                depth--;
            }
            i++;
        }
        return depth == 0 ? i - 1 : -1;
    }

    private int matchBraceForward(final List<Token> tokens, final int openIdx) {
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

    private boolean anyFrozen(final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (tokens.get(i).frozen) {
                return true;
            }
        }
        return false;
    }

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

    // ── §5 Null-safety operators ─────────────────────────────────────────────────
    /**
     * STYLE_KOTLIN.md §5: `?.` and `!!` are tight (no surrounding space, same treatment as
     * C/C++'s `*`/`&`); `?:` is spaced like a normal binary operator (`&&`, `+`). Unlike §4, this
     * isn't scoped to one construct -- these operators can appear in any expression anywhere in
     * the file, and no shared class does general expression-level operator re-spacing today
     * (`MiscRule.isTightToken`/`needsSpaceBetween` only fire inside signature/param rendering,
     * and assignment RHS values are joined verbatim). This is therefore a single flat pass over
     * the whole token stream, collapsing/normalizing the whitespace gap on either side of every
     * `?.`/`!!`/`?:` occurrence -- conservative like every other whitespace-collapsing pass in
     * this codebase: a gap containing a comment, a NEWLINE, or a frozen token is left completely
     * untouched (never risk relocating a comment or reflowing a frozen/disabled span).
     */
    public String enforceNullSafetyOperatorSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        Token lastSignificant = null;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean adjacentToTightOp = isTightNullOp(lastSignificant) || isTightNullOp(t);
            final boolean adjacentToElvis = !adjacentToTightOp && (isElvisOp(lastSignificant) || isElvisOp(t));

            if (gapBlocked || (!adjacentToTightOp && !adjacentToElvis)) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            } else if (adjacentToElvis) {
                out.append(' ');
            }
            // adjacentToTightOp && !gapBlocked: gap dropped entirely, nothing appended.

            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    private boolean isTightNullOp(final Token t) {
        return t != null && (isOp(t, "?.") || isOp(t, "!!"));
    }

    private boolean isElvisOp(final Token t) {
        return t != null && isOp(t, "?:");
    }

    // ── §10 Range operator spacing ────────────────────────────────────────────────
    /**
     * STYLE_KOTLIN.md §10: the range operator (`..`/`..<`) stays tight, same treatment as §5's
     * `?.`/`!!` -- no spaced variant to worry about (unlike `?:`), so this is a simpler one-sided
     * version of {@link #enforceNullSafetyOperatorSpacing}'s state machine, kept as its own
     * method rather than folded into that one since it's a different STYLE_KOTLIN.md section
     * with its own RDD entry. The loop head's own keywords (`in`, `until`, `downTo`, `step`) need
     * no code here at all -- `in` is already lexed as `TokenType.KEYWORD` and `until`/`downTo`/
     * `step` as plain `TokenType.IDENTIFIER` (confirmed via a standalone harness), so none of them
     * are mistaken for a `(`/`[` by {@link com.jxmake.formatter.evaluator.ComplexityPaddingEvaluator#isLoose}
     * and their own spacing is already left alone by every shared class, same passive-preservation
     * reasoning as §15's infix-call word-operator spacing. Same conservative bailout as every
     * other pass in this file: a gap containing a comment, a NEWLINE, or a frozen token is left
     * completely untouched.
     */
    public String enforceRangeOperatorSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        Token lastSignificant = null;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean adjacentToRangeOp = isRangeOp(lastSignificant) || isRangeOp(t);

            if (!gapBlocked && adjacentToRangeOp) {
                // gap dropped entirely, nothing appended -- tight on both sides.
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }

            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    private boolean isRangeOp(final Token t) {
        return t != null && (isOp(t, "..") || isOp(t, "..<"));
    }

    // ── §11 Labeled jumps ────────────────────────────────────────────────────────
    /** Tracks progress through a `return@label`/`break@loop`/`continue@loop` jump, or a
     *  `label@` declaration, as tokens are consumed left to right. */
    private enum JumpState {
        NONE, AFTER_JUMP_KEYWORD, AFTER_JUMP_AT, AFTER_JUMP_LABEL, AFTER_PLAIN_IDENT, AFTER_DECL_AT
    }

    /**
     * STYLE_KOTLIN.md §11: `return@label`/`break@loop`/`continue@loop` are tight around the `@`
     * (no space either side of it or between it and the keyword/label), and a label declaration
     * (`outer@`) is likewise tight between the identifier and the `@`. What follows the label --
     * a jump's value expression, or whatever the declared label is attached to (`outer@ for(...)`)
     * -- is spaced from it with exactly one space, same as a normal keyword-followed-by-identifier
     * gap. No shared class recognizes this token shape (a keyword or identifier immediately glued
     * to `@`), so this is its own flat pass over the whole token stream, tracking a small left-to-
     * right state machine to tell a jump's `@label` apart from a declaration's `label@`. Same
     * conservative bailout as §5: any gap containing a comment, a NEWLINE, or a frozen token is
     * left completely untouched (never risk relocating a comment or reflowing a frozen span, and
     * never force a space onto a jump with no trailing value, where the gap is just the statement's
     * closing NEWLINE).
     */
    public String enforceLabeledJumpSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        JumpState state = JumpState.NONE;
        Token lastSignificant = null;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean tightBeforeAt = isOp(t, "@")
                    && (state == JumpState.AFTER_JUMP_KEYWORD || state == JumpState.AFTER_PLAIN_IDENT);
            final boolean tightAfterJumpAt = state == JumpState.AFTER_JUMP_AT && t.type == TokenType.IDENTIFIER;
            final boolean forceSpace = state == JumpState.AFTER_JUMP_LABEL || state == JumpState.AFTER_DECL_AT;

            if (gapBlocked) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            } else if (forceSpace) {
                out.append(' ');
            } else if (!tightBeforeAt && !tightAfterJumpAt) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            // tightBeforeAt || tightAfterJumpAt, unblocked: gap dropped entirely.

            gap.clear();
            out.append(t.text);

            if (t.type == TokenType.KEYWORD && isJumpKeyword(t.text)) {
                state = JumpState.AFTER_JUMP_KEYWORD;
            } else if (state == JumpState.AFTER_JUMP_KEYWORD && isOp(t, "@")) {
                state = JumpState.AFTER_JUMP_AT;
            } else if (state == JumpState.AFTER_JUMP_AT && t.type == TokenType.IDENTIFIER) {
                state = JumpState.AFTER_JUMP_LABEL;
            } else if (t.type == TokenType.IDENTIFIER) {
                state = JumpState.AFTER_PLAIN_IDENT;
            } else if (state == JumpState.AFTER_PLAIN_IDENT && isOp(t, "@")) {
                state = JumpState.AFTER_DECL_AT;
            } else {
                state = JumpState.NONE;
            }

            lastSignificant = t;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    private boolean isJumpKeyword(final String text) {
        return "return".equals(text) || "break".equals(text) || "continue".equals(text);
    }

    // ── §17/§17.1 Function-type / lambda-parameter arrow spacing ────────────────
    /**
     * STYLE_KOTLIN.md §17/§17.1: every `->` -- a function type's own arrow (`(Int) -> String`,
     * `Type.(Params) -> ReturnType`) and a lambda literal's own parameter-list arrow (`{ x, y ->
     * x + y }`) alike -- is spaced exactly one space on each side, "one consistent arrow-spacing
     * rule across all three constructs" per §17.1's own text (the third construct being `when`'s
     * arrow, §4). A `when`-branch's own selector arrow is excluded here -- {@link
     * #formatWhenExpressions} already fully owns that arrow's spacing (column-aligned padding,
     * not a flat single space), so this pass must never touch it or it would collapse that
     * alignment back down to one space regardless of which pass happens to run first/last once
     * these are eventually wired together. {@link #collectWhenBranchArrowIndices} identifies
     * exactly those arrow token indices (by walking every `when` block the same way {@link
     * #formatWhenExpressions} does) so they can be skipped by index, not by construct-shape
     * guessing. Same conservative bailout as every other pass in this file: a gap containing a
     * comment, a NEWLINE, or a frozen token on either side of the arrow is left completely
     * untouched for that side (critical for the multi-line lambda body case, `{ item ->\n
     * item.transform()\n}`, where forcing a space before the newline would introduce trailing
     * whitespace).
     */
    public String enforceArrowSpacing(final List<Token> tokens) {
        final java.util.Set<Integer> whenArrows = collectWhenBranchArrowIndices(tokens);
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        Token lastSignificant = null;
        int lastSignificantIdx = -1;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean blocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean forceSpaceBefore = !blocked && isOp(t, "->") && !whenArrows.contains(i);
            final boolean forceSpaceAfter = !blocked && lastSignificant != null && isOp(lastSignificant, "->")
                    && !whenArrows.contains(lastSignificantIdx);

            if (forceSpaceBefore || forceSpaceAfter) {
                out.append(' ');
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }

            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            lastSignificantIdx = i;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    /** Every `when`-branch selector arrow's own token index, across every `when` block in
     *  {@code tokens} -- walks the same `when(...) { ... }` shape {@link #formatWhenExpressions}
     *  recognizes, so {@link #enforceArrowSpacing} can exclude exactly those arrows (already
     *  owned by §4's column alignment) without duplicating that method's alignment logic. */
    private java.util.Set<Integer> collectWhenBranchArrowIndices(final List<Token> tokens) {
        final java.util.Set<Integer> arrows = new java.util.HashSet<>();
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type != TokenType.KEYWORD || !"when".equals(t.text)) {
                continue;
            }
            int j = nextSignificantIndex(tokens, i + 1);
            if (j < 0) {
                continue;
            }
            if (isPunct(tokens.get(j), "(")) {
                final int closeParen = matchParenForward(tokens, j);
                if (closeParen < 0) {
                    continue;
                }
                j = nextSignificantIndex(tokens, closeParen + 1);
            }
            if (j < 0 || !isPunct(tokens.get(j), "{")) {
                continue;
            }
            final int openBrace = j;
            final int closeBrace = matchBraceForward(tokens, openBrace);
            if (closeBrace < 0) {
                continue;
            }
            final List<WhenBranch> branches = findWhenBranches(tokens, openBrace, closeBrace);
            if (branches == null) {
                continue;
            }
            for (final WhenBranch b : branches) {
                final int arrowIdx = findTopLevelArrow(tokens, b.labelStart, b.bodyStart);
                if (arrowIdx >= 0) {
                    arrows.add(arrowIdx);
                }
            }
        }
        return arrows;
    }

    // ── §16 Annotation use-site targets ──────────────────────────────────────────
    private static final java.util.Set<String> USE_SITE_TARGETS = new java.util.HashSet<>(java.util.Arrays.asList(
            "file", "property", "field", "get", "set", "receiver", "param", "setparam", "delegate"));

    private enum UseSiteState {
        NONE, AFTER_AT, AFTER_TARGET, AFTER_COLON
    }

    /**
     * STYLE_KOTLIN.md §16: an annotation use-site target (`@field:`, `@get:`, `@param:`, `@set:`,
     * etc.) is tight around its `:` -- no space either side of it between the target keyword and
     * the annotation name that follows. Matched purely on token text against the fixed set of
     * legal Kotlin use-site targets (not `TokenType.KEYWORD`, since not all of them --
     * `delegate` in particular -- are lexed as keywords), so this needs no tokenizer change.
     * Deliberately narrow: only the target-to-`:`-to-name shape is touched; the gap between `@`
     * and the target itself, and everything after the annotation name, is left completely alone,
     * matching this codebase's existing posture of never actively enforcing general annotation
     * spacing (no rule anywhere reformats plain `@Override`-style spacing either). Same
     * conservative bailout as every other pass in this file: any gap containing a comment, a
     * NEWLINE, or a frozen token is left untouched.
     */
    public String enforceAnnotationUseSiteTargetSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        UseSiteState state = UseSiteState.NONE;
        Token lastSignificant = null;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                i++;
                continue;
            }

            final boolean gapBlocked = gap.stream().anyMatch(g -> isComment(g) || g.type == TokenType.NEWLINE || g.frozen)
                    || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean tightBeforeColon = state == UseSiteState.AFTER_TARGET && isOp(t, ":");
            final boolean tightAfterColon = state == UseSiteState.AFTER_COLON;

            if (gapBlocked || (!tightBeforeColon && !tightAfterColon)) {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            // tightBeforeColon || tightAfterColon, unblocked: gap dropped entirely.

            gap.clear();
            out.append(t.text);

            if (isOp(t, "@")) {
                state = UseSiteState.AFTER_AT;
            } else if (state == UseSiteState.AFTER_AT && USE_SITE_TARGETS.contains(t.text)) {
                state = UseSiteState.AFTER_TARGET;
            } else if (state == UseSiteState.AFTER_TARGET && isOp(t, ":")) {
                state = UseSiteState.AFTER_COLON;
            } else {
                state = UseSiteState.NONE;
            }

            lastSignificant = t;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    // ── §14 Generic `where` clause ───────────────────────────────────────────────
    /** One `TypeParam : Bound` entry of a `where` clause, found between two top-level commas
     *  (or between `where` and the clause's own end for the first/only entry). */
    private static final class WhereBound {
        final int start;
        final int end; // inclusive, last significant token of the bound

        WhereBound(final int start, final int end) {
            this.start = start;
            this.end = end;
        }
    }

    /**
     * STYLE_KOTLIN.md §14: a trailing generic `where` clause (`fun <T> merge(...): T where T :
     * Comparable<T>, T : Serializable {`) stays on the signature's own line if the whole thing
     * (signature line + ` where ` + all bounds joined with `, `) fits within {@link
     * #lineLengthLimit}; otherwise `where` drops to its own line indented one level under the
     * signature's own line-leading indent, and every bound breaks onto its own line at the
     * top-level comma (never at a bound's own `:`), column-aligned under the first bound's start
     * column -- same "trailing qualifier attaches to the signature, breaks only at its own
     * natural token" posture as `CppSpecificRule.enforceRequiresClausePlacement`, which this
     * mirrors structurally (this file's own precedent, not a shared-class extension, since
     * `CppSpecificRule` is itself a per-language file). Bound text itself (including whatever `:`
     * spacing already exists) is never rewritten -- only the placement of `where` and the commas
     * between bounds. A single bound line that still doesn't fit is left to overflow, per
     * STYLE_KOTLIN.md §14's own explicit exception (same posture as §12's destructuring lists) --
     * there is no finer-grained break point below one-bound-per-line. A comment anywhere in the
     * clause, or a frozen token, blocks the rewrite for that occurrence entirely, same
     * conservative posture as every other rule in this file.
     */
    public String enforceWhereClausePlacement(final List<Token> tokens) {
        final Map<Integer, String> overrides = new HashMap<>();
        final Map<Integer, String> insertAfter = new HashMap<>();
        final java.util.Set<Integer> suppressed = new java.util.HashSet<>();

        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type != TokenType.KEYWORD || !"where".equals(t.text)) {
                continue;
            }
            final int clauseEndIdx = findWhereClauseEnd(tokens, i);
            if (clauseEndIdx < 0 || clauseEndIdx <= i + 1) {
                continue;
            }
            if (anyFrozen(tokens, i, clauseEndIdx)) {
                continue;
            }

            final List<WhereBound> bounds = splitWhereBounds(tokens, i + 1, clauseEndIdx);
            if (bounds.isEmpty() || hasCommentBetween(tokens, i, clauseEndIdx)) {
                continue;
            }

            final int lineStartIdx = lineStartIndex(tokens, i);
            final String baseIndent = lineIndent(tokens, i);
            final String sigLine = collapseToOneLine(tokens, lineStartIdx, i - 1);
            final List<String> boundTexts = new ArrayList<>();
            for (final WhereBound b : bounds) {
                boundTexts.add(literalSlice(tokens, b.start, b.end + 1).trim());
            }
            final String combined = baseIndent + sigLine + " where " + String.join(", ", boundTexts);

            if (combined.length() <= lineLengthLimit) {
                continue; // already fits inline as-is -- leave byte-for-byte untouched
            }

            // Wrap: `where` onto its own line one indent level under the signature; each bound
            // onto its own line, aligned under the first bound's start column.
            final String whereIndent = baseIndent + indentUnit;
            final String boundIndent = spaces(whereIndent.length() + "where ".length());

            overrides.put(i, "\n" + whereIndent + "where");
            for (int j = i - 1; j >= 0 && isGapToken(tokens.get(j)); j--) {
                suppressed.add(j);
            }
            for (int j = i + 1; j < bounds.get(0).start; j++) {
                suppressed.add(j);
            }
            for (int k = 0; k < bounds.size(); k++) {
                final WhereBound b = bounds.get(k);
                final String prefix = k == 0 ? " " : "\n" + boundIndent;
                overrides.put(b.start, prefix + boundTexts.get(k));
                for (int j = b.start + 1; j <= b.end; j++) {
                    suppressed.add(j);
                }
                if (k < bounds.size() - 1) {
                    final int commaIdx = nextSignificantIndex(tokens, b.end + 1);
                    overrides.put(commaIdx, ",");
                    final int nextStart = bounds.get(k + 1).start;
                    for (int j = commaIdx + 1; j < nextStart; j++) {
                        suppressed.add(j);
                    }
                }
                // The gap after the last bound (up to clauseEndIdx, the `{`/`;`) is left
                // untouched -- same "don't overwrite a newline an Allman-brace pass already
                // placed there" posture as CppSpecificRule.enforceRequiresClausePlacement.
            }
        }

        return renderSuppressing(tokens, overrides, insertAfter, suppressed);
    }

    /** The first `{`/`;` reached scanning forward from {@code whereIdx}, or -1 if neither is
     *  found -- the clause's own end (exclusive), per {@link #enforceWhereClausePlacement}'s doc
     *  comment. */
    private int findWhereClauseEnd(final List<Token> tokens, final int whereIdx) {
        for (int i = whereIdx + 1; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{") || isPunct(t, ";")) {
                return i;
            }
        }
        return -1;
    }

    /** Splits [fromInclusive, toExclusive) into bounds at every top-level comma -- depth tracked
     *  over parens/brackets/braces and the tokenizer's own reclassified generic angle brackets
     *  (`ANGLE_BRACKET_OPEN`/`_CLOSE`), so a bound's own generic argument (`Comparable<T>`) is
     *  never mistaken for a bound separator. */
    private List<WhereBound> splitWhereBounds(final List<Token> tokens, final int fromInclusive,
            final int toExclusive) {
        final List<WhereBound> bounds = new ArrayList<>();
        int depth = 0;
        int start = nextSignificantIndex(tokens, fromInclusive);
        int lastSig = -1;
        for (int i = start; i >= 0 && i < toExclusive; i++) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                continue;
            }
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")
                    || t.type == TokenType.ANGLE_BRACKET_OPEN) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")
                    || t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                depth--;
            } else if (depth == 0 && isPunct(t, ",")) {
                if (lastSig < 0) {
                    return Collections.emptyList();
                }
                bounds.add(new WhereBound(start, lastSig));
                start = nextSignificantIndex(tokens, i + 1);
                lastSig = -1;
                continue;
            }
            lastSig = i;
        }
        if (lastSig < 0 || start < 0) {
            return Collections.emptyList();
        }
        bounds.add(new WhereBound(start, lastSig));
        return bounds;
    }

    /** True if a comment token lies anywhere in {@code (fromExclusive, toExclusive)} -- blocks
     *  the rewrite entirely, same posture as {@code CppSpecificRule.hasCommentBetween}. */
    private boolean hasCommentBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            if (isComment(tokens.get(i))) {
                return true;
            }
        }
        return false;
    }

    private String spaces(final int count) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** The index of the first significant token on the physical line containing {@code idx} --
     *  same purpose as {@code CppSpecificRule.lineStartIndex}. */
    private int lineStartIndex(final List<Token> tokens, final int idx) {
        int newlineIdx = -1;
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                newlineIdx = i;
                break;
            }
        }
        final int firstSig = nextSignificantIndex(tokens, newlineIdx < 0 ? 0 : newlineIdx);
        return firstSig < 0 ? idx : firstSig;
    }

    /** Line-leading whitespace of the physical line containing token {@code idx} -- "" if that
     *  line has no leading whitespace (column-0 start). Same purpose as
     *  {@code CppSpecificRule.lineIndent}. */
    private String lineIndent(final List<Token> tokens, final int idx) {
        int newlineIdx = -1;
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                newlineIdx = i;
                break;
            }
        }
        final int afterNewline = newlineIdx + 1;
        if (afterNewline < tokens.size() && tokens.get(afterNewline).type == TokenType.WHITESPACE) {
            return tokens.get(afterNewline).text;
        }
        return "";
    }

    /** Renders {@code tokens[fromInclusive, toInclusive]} verbatim except every whitespace/newline
     *  run collapses to exactly one space -- used to measure a would-be single-line rendering
     *  against {@link #lineLengthLimit} without actually committing to it. Same purpose as
     *  {@code CppSpecificRule.collapseToOneLine}. */
    private String collapseToOneLine(final List<Token> tokens, final int fromInclusive, final int toInclusive) {
        final StringBuilder sb = new StringBuilder();
        for (int i = fromInclusive; i <= toInclusive; i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
                    sb.append(' ');
                }
                continue;
            }
            sb.append(t.text);
        }
        return sb.toString().trim();
    }

    /** Same as {@link #render} but drops any token index present in {@code suppressed} entirely
     *  (used to elide a bound's interior gap tokens once its whole span has been replaced by a
     *  single override at its start index). */
    private String renderSuppressing(final List<Token> tokens, final Map<Integer, String> overrides,
            final Map<Integer, String> insertAfter, final java.util.Set<Integer> suppressed) {
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (suppressed.contains(i)) {
                continue;
            }
            final String override = overrides.get(i);
            out.append(override != null ? override : tokens.get(i).text);
            final String extra = insertAfter.get(i);
            if (extra != null) {
                out.append(extra);
            }
        }
        return out.toString();
    }

    // ── §2 `enum class` with members ────────────────────────────────────────────
    /**
     * STYLE_KOTLIN.md §2: when a Kotlin `enum class` body has trailing members after its entry
     * list, the mandatory `;` separating them is detached onto its own line with a blank line
     * before and after it -- the same "emphasis" shape as Java's optional enum-constant-list `;`
     * (`JavaSpecificRule.separateEnumConstantListTerminator`, which this method mirrors rather
     * than reuses -- that class is per-language, not shared, so a Kotlin call path into it would
     * cross the C/C++/Java-vs-Kotlin file boundary this codebase otherwise keeps strict). The
     * blank line right after `enum class XXX {` and right before the body's own closing `}` is
     * already produced for free by the shared `BlockStructureRule.insertNamedConstructBlankLines`
     * (confirmed via a standalone harness before writing this method -- Kotlin's `enum`/`class`
     * keywords already satisfy that method's existing "keyword before `class` is `enum`" check,
     * no code needed there) -- this method only adds the blank lines around the `;` itself, which
     * that method does not touch. An enum with no trailing members (the `;` is absent, or is the
     * very last thing before the enum's own `}`) is left untouched: nothing to separate the entry
     * list from. A frozen span anywhere between the entry list and the closing brace blocks the
     * rewrite for that enum body entirely, same conservative posture as every other pass in this
     * file.
     */
    public String separateEnumConstantListTerminator(final List<Token> rawTokens) {
        final List<Token> tokens = relocateEnumTerminatorTrailingComments(rawTokens);
        final Map<Integer, String> terminators = findEnumConstantListTerminators(tokens);
        if (terminators.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (final Token t : tokens) {
                sb.append(t.text);
            }
            return sb.toString();
        }
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        int lastSignificant = -1;
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                gap.add(t);
                continue;
            }
            final boolean thisIsTerminator = terminators.containsKey(i);
            final boolean prevWasTerminator = lastSignificant >= 0 && terminators.containsKey(lastSignificant);
            if (thisIsTerminator || prevWasTerminator) {
                final String indent = thisIsTerminator ? terminators.get(i) : terminators.get(lastSignificant);
                out.append(forceBlankLine(gap, indent));
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
            }
            gap.clear();
            out.append(t.text);
            lastSignificant = i;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    /** Renders a gap that straddles the enum-list-terminating `;` as a forced blank line, without
     *  discarding any comment the original gap carried (e.g. a same-line trailing comment on the
     *  `;` itself, or a standalone comment on its own line before the next member/declaration) --
     *  the naive fixed "\n\n" + indent replacement used to silently drop both. A same-line trailing
     *  comment (anything before the gap's first {@code NEWLINE}) is preserved verbatim right after
     *  the terminator; everything from that first newline onward is preserved too, with only the
     *  run of blank newlines immediately following it collapsed down to exactly one forced blank
     *  line (comments and their own surrounding newlines further into the gap are left untouched). */
    private String forceBlankLine(final List<Token> gap, final String indent) {
        boolean hasComment = false;
        for (final Token g : gap) {
            if (g.type == TokenType.COMMENT_LINE || g.type == TokenType.COMMENT_BLOCK) {
                hasComment = true;
                break;
            }
        }
        if (!hasComment) {
            return "\n\n" + indent;
        }
        final StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < gap.size() && gap.get(i).type != TokenType.NEWLINE) {
            sb.append(gap.get(i).text);
            i++;
        }
        sb.append('\n').append('\n');
        if (i >= gap.size()) {
            // No newline followed the same-line part (e.g. a trailing comment right before the
            // terminator) -- nothing else in the gap supplies the next line's indentation.
            sb.append(indent);
            return sb.toString();
        }
        i++;
        while (i < gap.size() && gap.get(i).type == TokenType.NEWLINE) {
            i++;
        }
        sb.append(indent);
        if (i < gap.size() && gap.get(i).type == TokenType.WHITESPACE) {
            // Skip one leading indentation token from the original gap -- we just supplied our own.
            i++;
        }
        while (i < gap.size()) {
            sb.append(gap.get(i).text);
            i++;
        }
        return sb.toString();
    }

    /** The mandatory `;` this method relocates onto its own line often carries a same-line
     *  trailing comment (`NOT_FOUND(404);   // missing`) that, textually, trails the `;` -- but
     *  semantically describes the entry before it, not the relocated `;` itself. Left in place, the
     *  forced blank-line rewrite below would either strand it awkwardly after the relocated `;` or
     *  (before this fix existed) silently drop it. This pass moves such a comment (and its single
     *  leading whitespace token, if any) back before the terminator, so it reads as the entry's own
     *  trailing comment once the `;` moves to its own line. Recomputes terminators after each move
     *  since a move shifts every later token's index. */
    private List<Token> relocateEnumTerminatorTrailingComments(final List<Token> tokensIn) {
        List<Token> tokens = tokensIn;
        while (true) {
            final Map<Integer, String> terminators = findEnumConstantListTerminators(tokens);
            Integer target = null;
            for (final Integer p : terminators.keySet()) {
                int scan = p + 1;
                int wsIdx = -1;
                if (scan < tokens.size() && tokens.get(scan).type == TokenType.WHITESPACE) {
                    wsIdx = scan;
                    scan++;
                }
                if (scan < tokens.size() && (tokens.get(scan).type == TokenType.COMMENT_LINE
                        || tokens.get(scan).type == TokenType.COMMENT_BLOCK)) {
                    target = p;
                    break;
                }
            }
            if (target == null) {
                return tokens;
            }
            final int p = target;
            int scan = p + 1;
            int wsIdx = -1;
            if (scan < tokens.size() && tokens.get(scan).type == TokenType.WHITESPACE) {
                wsIdx = scan;
                scan++;
            }
            final int commentIdx = scan;
            final List<Token> moved = new ArrayList<>();
            if (wsIdx >= 0) {
                // The relocated `;` itself is dropped from this line entirely (moved onto its own
                // line by the caller's forced-blank-line rewrite), so the comment's original
                // same-line whitespace -- sized to align past `NOT_FOUND(404);` -- is now one
                // character short of the same visual column past bare `NOT_FOUND(404)`. Pad it back
                // out by the width of the terminator being removed.
                final Token ws = tokens.get(wsIdx);
                moved.add(new Token(ws.type, ws.text + " ", ws.braceDepth, ws.parenDepth, ws.name));
            }
            moved.add(tokens.get(commentIdx));
            final List<Token> next = new ArrayList<>(tokens);
            next.remove(commentIdx);
            if (wsIdx >= 0) {
                next.remove(wsIdx);
            }
            next.addAll(p, moved);
            tokens = next;
        }
    }

    /** Finds every Kotlin `enum class` body's entry-list-terminating `;` that has at least one
     *  more member after it before the enum's own `}` -- keyed by token index, valued by the
     *  indent string of the enum body's member lines (derived from its first member's own line). */
    private Map<Integer, String> findEnumConstantListTerminators(final List<Token> tokens) {
        final Map<Integer, String> result = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), "{") || !isEnumBodyBrace(tokens, i)) {
                continue;
            }
            final int closeBraceIdx = matchBraceForward(tokens, i);
            if (closeBraceIdx < 0) {
                continue;
            }
            final int firstMember = nextSignificantIndex(tokens, i + 1);
            if (firstMember < 0 || firstMember >= closeBraceIdx) {
                continue;
            }
            final String indent = lineIndent(tokens, firstMember);
            int depth = 0;
            for (int p = i + 1; p < closeBraceIdx; p++) {
                final Token t = tokens.get(p);
                if (isGapToken(t)) {
                    continue;
                }
                if (isPunct(t, "(") || isPunct(t, "{")) {
                    depth++;
                } else if (isPunct(t, ")") || isPunct(t, "}")) {
                    depth--;
                } else if (depth == 0 && isPunct(t, ";")) {
                    final int next = nextSignificantIndex(tokens, p + 1);
                    if (next >= 0 && next < closeBraceIdx && !anyFrozen(tokens, firstMember, closeBraceIdx)) {
                        result.put(p, indent);
                    }
                    break;
                }
            }
        }
        return result;
    }

    /** True iff the `{` at {@code braceIdx} opens a Kotlin `enum class` body -- scans backward
     *  past the enum's name and any supertype/generic-bound clause tokens until it finds the
     *  `enum` keyword, bailing out on an intervening `{`/`}`/`;` (a different construct entirely).
     *  Same shape as {@code JavaSpecificRule.isEnumBodyBrace}. */
    private boolean isEnumBodyBrace(final List<Token> tokens, final int braceIdx) {
        int p = prevSignificantIndex(tokens, braceIdx - 1);
        while (p >= 0) {
            final Token t = tokens.get(p);
            if (t.type == TokenType.KEYWORD && "enum".equals(t.text)) {
                return true;
            }
            if (isPunct(t, "{") || isPunct(t, "}") || isPunct(t, ";")) {
                return false;
            }
            p = prevSignificantIndex(tokens, p - 1);
        }
        return false;
    }

    private int prevSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from; i >= 0; i--) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    // ── §3/§3.3 Function/secondary-constructor body Allman conversion ──────────
    /**
     * STYLE_KOTLIN.md §3/§3.3: a Kotlin function or secondary-constructor definition's own body
     * `{` moves to its own line (Allman) whenever it is currently K&amp;R/same-line, mirroring
     * {@code JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle}/
     * {@code CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle} -- confirmed via harness
     * (STATE_KOTLIN.md's §3/§3.3 reclassification) that no shared logic does this for Kotlin;
     * {@code BlockStructureRule.enforceKAndRBraceStyle} only ever adds K&amp;R, never removes it.
     * Control-flow blocks, named-construct bodies (`class`/`object`/`interface`/`enum`/`init`,
     * tagged via {@code Token.name} by the tokenizer), and lambda bodies are untouched here, same
     * as the Java/C++ versions -- lambda bodies in particular never reach this method's candidate
     * shape, since a lambda's `{` is never directly preceded by a bare `)` the way a signature's
     * closing paren is.
     *
     * <p>Candidate signature detection deliberately differs from Java/C++'s "IDENTIFIER-before-
     * paren, not preceded by `new`" signal -- Kotlin has no `new` keyword, and critically, Kotlin's
     * trailing-lambda call syntax (`someCall(args) { ... }`, extremely common) is token-shape-
     * identical to a function definition (`)` directly followed by `{`) with no `new`-style signal
     * to rule it out. Guessing past this would misfire constantly on ordinary calls. Instead, this
     * method requires unambiguous positive evidence: scanning backward from the parameter list's
     * name past an optional extension-receiver chain (`Type.`/`Type&lt;Args&gt;.`) and an optional
     * `&lt;T&gt;` function type-parameter clause, the scan must land exactly on the `fun` keyword
     * (see {@link #isFunctionOrConstructorCloseParen}) -- or, for a secondary constructor, the
     * token immediately before `(` must be the `constructor` keyword itself. Anything else (an
     * ordinary call, however shaped) bails out (not a candidate) rather than guessing, same "give
     * up rather than guess" posture as {@code KotlinSignatureRule.parseKotlinSignature}'s null
     * return. This scan shape also incidentally excludes a Kotlin enum entry's own anonymous body
     * (`RED(0xFF0000) { override fun toString() = ... }`) for free -- an enum entry name is never
     * preceded by `fun`, so it can never satisfy this method's positive-evidence requirement,
     * unlike Java's version which needs a separate, explicit {@code isEnumConstantBody} exclusion.
     *
     * <p>A `{` already on its own line (gap already contains a `NEWLINE`) is left untouched --
     * idempotent. A body whose entire `{ ... }` span sits on one physical line is never converted
     * (same one-liner exception as the Java/C++ versions, RDD_KEY_75/RDD_KEY_89) -- unlike those
     * two, this method has no call-line-breaking-prediction refinement, since no Kotlin equivalent
     * of {@code MiscRule.enforceCallLineBreaking} exists yet; if one is added later, this method
     * may need the same idempotency refinement those two already carry.
     */
    public String enforceFunctionDefinitionAllmanBraceStyle(final List<Token> tokens) {
        final Map<Integer, Integer> gapToBrace = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), "{") || tokens.get(i).name != null) {
                continue;
            }
            final int lastBeforeBrace = prevSignificantIndex(tokens, i - 1);
            if (lastBeforeBrace < 0) {
                continue;
            }
            final int closeParenIdx = findSignatureCloseParenBeforeBrace(tokens, lastBeforeBrace);
            if (closeParenIdx < 0 || !isFunctionOrConstructorCloseParen(tokens, closeParenIdx)) {
                continue;
            }
            if (hasNewlineOrCommentBetween(tokens, closeParenIdx, i)) {
                continue;
            }
            if (anyFrozen(tokens, closeParenIdx, i + 1)) {
                continue;
            }
            final int closeBraceIdx = matchBraceForward(tokens, i);
            if (closeBraceIdx >= 0 && isSingleLineBody(tokens, i, closeBraceIdx)) {
                continue;
            }
            gapToBrace.put(lastBeforeBrace + 1, i);
        }

        final StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < tokens.size()) {
            final Integer braceIdx = gapToBrace.get(i);
            if (braceIdx != null) {
                out.append('\n').append(lineIndent(tokens, i - 1));
                out.append(tokens.get(braceIdx).text);
                i = braceIdx + 1;
            } else {
                out.append(tokens.get(i).text);
                i++;
            }
        }
        return out.toString();
    }

    /** Given the last significant token before a candidate body `{`, returns the index of the
     *  signature's parameter-list `)` -- either {@code lastBeforeBrace} itself (no return type),
     *  or, if a `: ReturnType` sits between the `)` and the `{` (e.g. `fun f(): String {`), the
     *  `)` found by scanning back over the return-type span for a depth-0 `:`. Returns -1 for any
     *  other shape (bail, not a candidate). */
    private int findSignatureCloseParenBeforeBrace(final List<Token> tokens, final int lastBeforeBrace) {
        if (isPunct(tokens.get(lastBeforeBrace), ")")) {
            return lastBeforeBrace;
        }
        int depth = 0;
        int i = lastBeforeBrace;
        while (i >= 0) {
            final Token t = tokens.get(i);
            if (isAngleClose(t) || isPunct(t, ")") || isPunct(t, "]")) {
                depth++;
            } else if (isAngleOpen(t) || isPunct(t, "(") || isPunct(t, "[")) {
                depth--;
                if (depth < 0) {
                    return -1;
                }
            } else if (depth == 0 && isOp(t, ":")) {
                final int before = prevSignificantIndex(tokens, i - 1);
                return (before >= 0 && isPunct(tokens.get(before), ")")) ? before : -1;
            }
            i = prevSignificantIndex(tokens, i - 1);
        }
        return -1;
    }

    /** True iff the `)` at {@code closeParenIdx} is a function or secondary-constructor
     *  definition's parameter-list close paren -- see {@link #enforceFunctionDefinitionAllmanBraceStyle}'s
     *  own doc comment for the backward-scan shape and why it's deliberately conservative. */
    private boolean isFunctionOrConstructorCloseParen(final List<Token> tokens, final int closeParenIdx) {
        final int openParenIdx = matchParenBackward(tokens, closeParenIdx);
        if (openParenIdx < 0) {
            return false;
        }
        int p = prevSignificantIndex(tokens, openParenIdx - 1);
        if (p < 0) {
            return false;
        }
        if (tokens.get(p).type == TokenType.KEYWORD && "constructor".equals(tokens.get(p).text)) {
            return true;
        }
        if (tokens.get(p).type != TokenType.IDENTIFIER) {
            return false;
        }
        int q = prevSignificantIndex(tokens, p - 1);
        if (q >= 0 && isOp(tokens.get(q), ".")) {
            // Extension-function receiver chain: `Type.` or `Type<Args>.` before the name.
            int r = prevSignificantIndex(tokens, q - 1);
            if (r < 0) {
                return false;
            }
            if (isAngleClose(tokens.get(r))) {
                r = skipAngleBracketsBackward(tokens, r);
                if (r < 0) {
                    return false;
                }
                r = prevSignificantIndex(tokens, r - 1);
                if (r < 0) {
                    return false;
                }
            }
            if (tokens.get(r).type != TokenType.IDENTIFIER) {
                return false;
            }
            q = prevSignificantIndex(tokens, r - 1);
        }
        if (q >= 0 && isAngleClose(tokens.get(q))) {
            // The function's own `<T>` type-parameter clause: `fun <T> ...`.
            q = skipAngleBracketsBackward(tokens, q);
            if (q < 0) {
                return false;
            }
            q = prevSignificantIndex(tokens, q - 1);
        }
        return q >= 0 && tokens.get(q).type == TokenType.KEYWORD && "fun".equals(tokens.get(q).text);
    }

    /** True for either a reclassified {@code ANGLE_BRACKET_CLOSE} or a plain {@code OP(">")} --
     *  the tokenizer only reclassifies `<`/`>` into angle-bracket tokens in contexts it recognizes
     *  as generic-usage-like (e.g. `List<T>`); a function's own `<T>` type-parameter clause right
     *  after `fun` is not one of those contexts, so it stays plain `OP` tokens (confirmed via
     *  harness). Both shapes mean the same thing here, so both are accepted. */
    private boolean isAngleClose(final Token t) {
        return t.type == TokenType.ANGLE_BRACKET_CLOSE || isOp(t, ">");
    }

    private boolean isAngleOpen(final Token t) {
        return t.type == TokenType.ANGLE_BRACKET_OPEN || isOp(t, "<");
    }

    /** Given the index of an angle-close token (real or plain `OP(">")`), returns the index of its
     *  matching angle-open token, or -1 if unbalanced. */
    private int skipAngleBracketsBackward(final List<Token> tokens, final int closeIdx) {
        int depth = 0;
        for (int i = closeIdx; i >= 0; i--) {
            if (isAngleClose(tokens.get(i))) {
                depth++;
            } else if (isAngleOpen(tokens.get(i))) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int matchParenBackward(final List<Token> tokens, final int closeIdx) {
        int depth = 0;
        for (int i = closeIdx; i >= 0; i--) {
            if (isPunct(tokens.get(i), ")")) {
                depth++;
            } else if (isPunct(tokens.get(i), "(")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean hasNewlineOrCommentBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.NEWLINE || type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    /** True iff no {@code NEWLINE} appears between {@code braceIdx} and {@code closeBraceIdx}
     *  inclusive -- the whole `{ ... }` body sits on one physical line, always left K&amp;R. */
    private boolean isSingleLineBody(final List<Token> tokens, final int braceIdx, final int closeBraceIdx) {
        for (int i = braceIdx; i <= closeBraceIdx; i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return false;
            }
        }
        return true;
    }

    // ── §1 Semicolon stripping ──────────────────────────────────────────────
    /**
     * STYLE_KOTLIN.md §1: strip every statement-terminating `;` that the language doesn't
     * actually require. Kept only where required: (1) an enum entry-list's own mandatory
     * terminator when member declarations follow (§2's `findEnumConstantListTerminators` --
     * reused here, not re-derived, to keep the "what counts as mandatory" definition in exactly
     * one place); (2) a `;` deliberately separating two statements kept on the same physical
     * line -- removing that one without also merging/splitting the statements would silently
     * change meaning, so it's left untouched rather than guessed at.
     *
     * <p>A `;` qualifies for stripping when nothing but whitespace/comments follows it before
     * the next {@code NEWLINE} (or end of file) -- i.e. it is the last significant thing on its
     * line, per {@link #isTrailingSemicolon}. Any whitespace immediately preceding a stripped
     * `;` is removed along with it, so `foo() ;` doesn't leave a stray trailing space.
     */
    public String stripOptionalSemicolons(final List<Token> tokens) {
        final Map<Integer, String> enumTerminators = findEnumConstantListTerminators(tokens);
        final Set<Integer> skip = new HashSet<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), ";") || enumTerminators.containsKey(i)) {
                continue;
            }
            if (anyFrozen(tokens, i, i + 1)) {
                continue;
            }
            if (!isTrailingSemicolon(tokens, i)) {
                continue;
            }
            skip.add(i);
            int w = i - 1;
            while (w >= 0 && tokens.get(w).type == TokenType.WHITESPACE) {
                skip.add(w);
                w--;
            }
        }
        if (skip.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (final Token t : tokens) {
                sb.append(t.text);
            }
            return sb.toString();
        }
        final StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (skip.contains(i)) {
                continue;
            }
            out.append(tokens.get(i).text);
        }
        return out.toString();
    }

    /** True iff the `;` at {@code idx} is the last significant thing on its physical line --
     *  the next non-gap token (if any) starts on a later line, or there is none (end of file).
     *  A trailing line/block comment after the `;` does not count as "another statement". */
    private boolean isTrailingSemicolon(final List<Token> tokens, final int idx) {
        for (int i = idx + 1; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.NEWLINE) {
                return true;
            }
            if (isGapToken(t)) {
                continue;
            }
            return false;
        }
        return true;
    }

    /** The six fixed classification buckets every Kotlin import is sorted into, per
     *  STYLE_KOTLIN.md §24. Mirrors {@code JavaSpecificRule.IMPORT_GROUP_KEYS}'s six-bucket
     *  shape, but swaps {@code static} for {@code kotlin} -- Kotlin has no {@code import static}
     *  keyword, so "this is a static import" isn't lexically detectable the way it is in Java (a
     *  companion-object-member or top-level-function import uses the exact same {@code import
     *  a.b.c} syntax as any other import). {@code groupOrder} passed into {@link
     *  #enforceKotlinImportOrdering} configures only the *emission order* of these always-the-same
     *  six buckets, never which buckets exist -- so it must be a permutation of exactly this set. */
    private static final Set<String> KOTLIN_IMPORT_GROUP_KEYS = new HashSet<>(
            Arrays.asList("kotlin", "java", "android", "com", "org", "other", "local"));

    /**
     * STYLE_KOTLIN.md §24: groups every top-level {@code import} statement into six fixed buckets
     * (kotlin, java/javax, org, com, local, other), sorts within each bucket, and re-renders the
     * whole import block in {@code groupOrder}'s order, separated by {@code blankLines} blank
     * line(s) between non-empty groups. Mirrors {@code JavaSpecificRule.enforceImportOrdering}'s
     * overall shape (per-language mirroring, not shared-class reuse -- same precedent as every
     * other Kotlin-only method in this file), with two Kotlin-specific differences:
     * <ul>
     *   <li>No {@code static} bucket (see {@link #KOTLIN_IMPORT_GROUP_KEYS}'s doc comment) --
     *       classification priority is local &gt; kotlin &gt; java/javax &gt; org &gt; com &gt;
     *       other, one step shorter than Java's static-first chain.</li>
     *   <li>An import statement is never required to be {@code ;}-terminated (STYLE_KOTLIN.md §1
     *       strips the optional trailing {@code ;} elsewhere in the pipeline) -- its own end is
     *       whichever comes first: an optional {@code ;}, or a NEWLINE/EOF. A tolerated trailing
     *       {@code ;}, if present, is simply dropped on regeneration, consistent with §1's own
     *       posture, never preserved.</li>
     * </ul>
     *
     * <p>Local-package detection, comment-blocking, and frozen-span-blocking all follow the exact
     * same posture as the Java version: the first {@code package a.b.c} declaration supplies the
     * local prefix (its top {@code importDepth} dot-components); any comment found anywhere inside
     * one import statement or floating between two import statements aborts the entire pass
     * unchanged (losing a comment via silent reordering is not an acceptable failure mode); a file
     * with zero imports is a no-op; "unused imports are not removed" is honored by construction (no
     * usage analysis performed). Import aliases ({@code import foo.Bar as Baz}) sort/group by the
     * pre-alias path per STYLE_KOTLIN.md §24's own text, and are re-emitted with their {@code as
     * Baz} suffix intact.
     *
     * @param groupOrder must be a permutation of exactly the six fixed bucket names in
     *        {@link #KOTLIN_IMPORT_GROUP_KEYS} -- a config-validation precondition, not a per-file
     *        content-shape judgment call, so an invalid value throws rather than silently dropping
     *        a bucket's imports
     */
    public String enforceKotlinImportOrdering(final List<Token> tokens, final List<String> groupOrder,
            final boolean sortAlphabetically, final int importDepth, final int blankLines) {
        if (!new HashSet<>(groupOrder).equals(KOTLIN_IMPORT_GROUP_KEYS)
                || groupOrder.size() != KOTLIN_IMPORT_GROUP_KEYS.size()) {
            throw new IllegalArgumentException(
                    "groupOrder must be a permutation of " + KOTLIN_IMPORT_GROUP_KEYS + ", got: " + groupOrder);
        }

        final List<String> localPrefix = findLocalPackagePrefix(tokens, importDepth);

        int depth = 0;
        int firstImportIdx = -1;
        int prevEndIdx = -1;
        int lastEndIdx = -1;
        boolean blocked = false;
        final List<ParsedKotlinImport> imports = new ArrayList<>();
        final int n = tokens.size();
        int i = 0;
        while (i < n) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{")) {
                depth++;
                i++;
                continue;
            }
            if (isPunct(t, "}")) {
                depth--;
                i++;
                continue;
            }
            if (depth == 0 && t.type == TokenType.KEYWORD && "import".equals(t.text)) {
                if (firstImportIdx < 0) {
                    firstImportIdx = i;
                } else if (hasCommentBetween(tokens, prevEndIdx, i)) {
                    blocked = true;
                    break;
                }
                final ParsedKotlinImport parsed = parseKotlinImportStatement(tokens, i);
                if (parsed == null) {
                    blocked = true;
                    break;
                }
                if (anyFrozen(tokens, i, parsed.endIdx + 1)) {
                    blocked = true;
                    break;
                }
                imports.add(parsed);
                prevEndIdx = parsed.endIdx;
                lastEndIdx = parsed.endIdx;
                i = parsed.endIdx + 1;
                continue;
            }
            i++;
        }

        if (blocked || imports.isEmpty()) {
            return joinVerbatim(tokens);
        }

        final Map<String, List<ParsedKotlinImport>> buckets = new HashMap<>();
        for (final String key : KOTLIN_IMPORT_GROUP_KEYS) {
            buckets.put(key, new ArrayList<>());
        }
        for (final ParsedKotlinImport imp : imports) {
            buckets.get(classifyKotlinImportGroup(imp, localPrefix)).add(imp);
        }
        if (sortAlphabetically) {
            for (final List<ParsedKotlinImport> group : buckets.values()) {
                group.sort((a, b) -> a.path.compareTo(b.path));
            }
        }

        final StringBuilder body = new StringBuilder();
        boolean emittedAnyGroup = false;
        for (final String groupKey : groupOrder) {
            final List<ParsedKotlinImport> members = buckets.get(groupKey);
            if (members.isEmpty()) {
                continue;
            }
            if (emittedAnyGroup) {
                for (int b = 0; b < blankLines + 1; b++) {
                    body.append('\n');
                }
            }
            for (int m = 0; m < members.size(); m++) {
                if (m > 0) {
                    body.append('\n');
                }
                final ParsedKotlinImport imp = members.get(m);
                body.append("import ").append(imp.path);
                if (imp.alias != null) {
                    body.append(" as ").append(imp.alias);
                }
            }
            emittedAnyGroup = true;
        }

        final StringBuilder out = new StringBuilder();
        appendRange(out, tokens, 0, firstImportIdx);
        out.append(body);
        appendRange(out, tokens, lastEndIdx + 1, n);
        return out.toString();
    }

    /** One successfully-parsed `import a.b.c[.*] [as Alias]` statement. {@code alias} is
     *  {@code null} when no `as` clause is present. */
    private static final class ParsedKotlinImport {
        final String path;
        final String alias;
        final int endIdx;

        ParsedKotlinImport(final String path, final String alias, final int endIdx) {
            this.path = path;
            this.alias = alias;
            this.endIdx = endIdx;
        }
    }

    /**
     * Parses one `import a.b.c[.*] [as Alias][;]` statement starting at the `import` keyword
     * token at {@code importIdx}. Unlike Java, a trailing `;` is never required -- the statement's
     * own end is whichever comes first: a `;`, or a NEWLINE/EOF (see {@link
     * #enforceKotlinImportOrdering}'s doc comment). Returns {@code null} -- signaling "bail the
     * entire pass" to the caller -- if a comment is found anywhere inside the statement, or if any
     * token other than WHITESPACE, an IDENTIFIER/dot/star path token, the `as` keyword (only after
     * at least one path token), or its alias IDENTIFIER is encountered before the statement ends.
     * Never guesses past an unrecognized import shape.
     */
    private ParsedKotlinImport parseKotlinImportStatement(final List<Token> tokens, final int importIdx) {
        boolean sawPathToken = false;
        boolean sawAs = false;
        String alias = null;
        final StringBuilder path = new StringBuilder();
        final int n = tokens.size();
        int p = importIdx + 1;
        int lastContentIdx = importIdx;
        while (p < n) {
            final Token t = tokens.get(p);
            if (t.type == TokenType.WHITESPACE) {
                p++;
                continue;
            }
            if (t.type == TokenType.NEWLINE) {
                break;
            }
            if (isComment(t)) {
                return null;
            }
            if (isPunct(t, ";")) {
                lastContentIdx = p;
                p++;
                break;
            }
            if (!sawAs && sawPathToken && t.type == TokenType.KEYWORD && "as".equals(t.text)) {
                sawAs = true;
                lastContentIdx = p;
                p++;
                continue;
            }
            if (sawAs && alias == null && t.type == TokenType.IDENTIFIER) {
                alias = t.text;
                lastContentIdx = p;
                p++;
                continue;
            }
            if (!sawAs && (t.type == TokenType.IDENTIFIER || isPathOp(t))) {
                path.append(t.text);
                sawPathToken = true;
                lastContentIdx = p;
                p++;
                continue;
            }
            return null;
        }
        if (!sawPathToken || (sawAs && alias == null)) {
            return null;
        }
        return new ParsedKotlinImport(path.toString(), alias, lastContentIdx);
    }

    /** True iff {@code t} is an OP token consisting solely of `.`/`*` characters -- covers a plain
     *  `.` separator, a plain `*` wildcard, and {@code TokenizerCore}'s combined `.*` multi-char
     *  pointer-to-member OP token (shared across all languages, so a wildcard import's trailing
     *  `.*` lexes as one combined OP token here too, same discovery as
     *  {@code JavaSpecificRule.isPathOp}'s own doc comment). */
    private boolean isPathOp(final Token t) {
        if (t == null || t.type != TokenType.OP || t.text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < t.text.length(); i++) {
            final char c = t.text.charAt(i);
            if (c != '.' && c != '*') {
                return false;
            }
        }
        return true;
    }

    /** Reads the first {@code package a.b.c} declaration in {@code tokens} (best-effort lookup,
     *  not a rewrite) and returns its top {@code importDepth} dot-components. Empty list if no
     *  `package` declaration exists. Kotlin's `package` statement, like `import`, is never
     *  `;`-terminated in practice -- this scan stops at whichever comes first, `;` or NEWLINE/EOF,
     *  same posture as {@link #parseKotlinImportStatement}. */
    private List<String> findLocalPackagePrefix(final List<Token> tokens, final int importDepth) {
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.KEYWORD && "package".equals(t.text)) {
                final List<String> components = new ArrayList<>();
                int p = i + 1;
                while (p < tokens.size() && tokens.get(p).type != TokenType.NEWLINE
                        && !isPunct(tokens.get(p), ";")) {
                    if (tokens.get(p).type == TokenType.IDENTIFIER) {
                        components.add(tokens.get(p).text);
                    }
                    p++;
                }
                return components.subList(0, Math.min(importDepth, components.size()));
            }
        }
        return Collections.emptyList();
    }

    /** Classification priority: local &gt; kotlin &gt; java/javax &gt; org &gt; com &gt; other --
     *  see {@link #KOTLIN_IMPORT_GROUP_KEYS}'s doc comment for why there is no `static` bucket. */
    private String classifyKotlinImportGroup(final ParsedKotlinImport imp, final List<String> localPrefix) {
        final String[] parts = imp.path.split("\\.");
        if (!localPrefix.isEmpty() && matchesPrefix(parts, localPrefix)) {
            return "local";
        }
        final String first = parts.length > 0 ? parts[0] : "";
        if ("kotlin".equals(first)) {
            return "kotlin";
        }
        if ("java".equals(first) || "javax".equals(first)) {
            return "java";
        }
        if ("android".equals(first)) {
            return "android";
        }
        if ("com".equals(first)) {
            return "com";
        }
        if ("org".equals(first)) {
            return "org";
        }
        return "other";
    }

    private boolean matchesPrefix(final String[] parts, final List<String> prefix) {
        if (parts.length < prefix.size()) {
            return false;
        }
        for (int i = 0; i < prefix.size(); i++) {
            if (!parts[i].equals(prefix.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Appends the literal text of {@code tokens[fromInclusive, toExclusive)} verbatim. */
    private void appendRange(final StringBuilder out, final List<Token> tokens, final int fromInclusive,
            final int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            out.append(tokens.get(i).text);
        }
    }

    private String joinVerbatim(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        for (final Token t : tokens) {
            out.append(t.text);
        }
        return out.toString();
    }

    /**
     * RDD_KEY_128: `kt_combined_out.kt` (user-verified, not guessed) wants a collapsed
     * single-line `else` body (RDD_KEY_127) padded with spaces so it starts at the same column as
     * its immediately preceding single-line {@code if(...) body} branch's own body -- e.g.
     * {@code if( it.isEmpty() ) 0} / {@code else               it.toInt()}, both bodies starting
     * at the same column.
     *
     * <p>Deliberately implemented as its own final, line-based, text-level pass -- not folded into
     * {@code BlockStructureRule.collapseSingleExpressionBlocks} (which produces the one-line
     * {@code else} body in the first place, RDD_KEY_127) -- because at collapse time the `if`
     * branch's own rendered width is not yet final: STYLE.md §3.1's complexity-padding tightening
     * (`enforceComplexityPadding`, e.g. turning a condition's `if (` into tight `if(`) still runs
     * *after* the collapse pass in {@code Formatter.formatOne}'s pipeline, more than once. An
     * initial attempt computed the target column at collapse time from the `if` line's
     * then-current rendered width and threaded it through to the `else` line's render -- root-
     * caused via debug prints to be stale by exactly one column in this fixture: the second
     * `if (it.isEmpty())` occurrence in `kt_combined_inp.kt` had not yet been tightened to
     * `if(...)` at collapse time, so the snapshotted width was one character too wide, and the
     * resulting padding one space too many. Waiting until every earlier pass has settled the
     * `if` line's final text -- this method runs last, after Phase 4 -- avoids that staleness
     * entirely; column widths are simply counted from the final rendered characters.
     *
     * <p>Only fires on an exact `<indent>if(...) <body>` line immediately followed by an
     * `<indent>else <body>` line with byte-identical indentation -- the `if` condition's matching
     * close paren is found via local depth counting (handles a nested call in the condition, e.g.
     * `if( System.currentTimeMillis() < 0 )`), and the pairing is trusted by construction: Kotlin's
     * grammar only ever places a bare `else` directly after its own `if`'s body, so any two
     * adjacent lines matching this exact shape are definitionally one `if`/`else` pair, never an
     * unrelated coincidence. Skips (leaves untouched) `else if` (a different, non-terminal branch)
     * and a braced `else {` (unrelated, pre-existing gap -- braced `else` bodies are never touched
     * by this pass or by RDD_KEY_127/124's collapse, out of scope here too). A body that would
     * need *fewer* than one space of padding (the `if` line is narrower than `else` plus its own
     * body) still gets exactly one space, same floor as every other single-space join in this
     * codebase -- never a negative pad.
     */
    public String alignBracelessElseWithIf(final List<Token> tokens) {
        final String[] lines = joinVerbatim(tokens).split("\n", -1);
        for (int i = 0; i + 1 < lines.length; i++) {
            final String ifLine = lines[i];
            final String elseLine = lines[i + 1];
            final int indentLen = leadingWhitespaceLength(ifLine);
            if (indentLen != leadingWhitespaceLength(elseLine)
                    || !ifLine.regionMatches(indentLen, "if(", 0, 3)) {
                continue;
            }
            if (!elseLine.regionMatches(indentLen, "else ", 0, 5)
                    || elseLine.regionMatches(indentLen, "else if", 0, 7)) {
                continue;
            }
            final int openParen = indentLen + 2; // right after "if"
            int depth = 0;
            int closeParen = -1;
            for (int c = openParen; c < ifLine.length(); c++) {
                final char ch = ifLine.charAt(c);
                if (ch == '(') {
                    depth++;
                } else if (ch == ')') {
                    depth--;
                    if (depth == 0) {
                        closeParen = c;
                        break;
                    }
                }
            }
            if (closeParen < 0 || closeParen + 2 > ifLine.length()
                    || ifLine.charAt(closeParen + 1) != ' ' || closeParen + 2 == ifLine.length()) {
                continue; // not a single-line `if(...) <body>` shape
            }
            final int ifBodyCol = closeParen + 2;
            final String elseIndent = elseLine.substring(0, indentLen);
            String elseBody = elseLine.substring(indentLen + "else".length());
            int elseBodyStart = 0;
            while (elseBodyStart < elseBody.length() && elseBody.charAt(elseBodyStart) == ' ') {
                elseBodyStart++;
            }
            elseBody = elseBody.substring(elseBodyStart);
            if (elseBody.isEmpty()) {
                continue;
            }
            final int elsePrefixLen = indentLen + "else".length();
            final int spaces = Math.max(1, ifBodyCol - elsePrefixLen);
            final StringBuilder sb = new StringBuilder(elseIndent).append("else");
            for (int s = 0; s < spaces; s++) {
                sb.append(' ');
            }
            sb.append(elseBody);
            lines[i + 1] = sb.toString();
        }
        return String.join("\n", lines);
    }

    /** Length of the whitespace run (spaces/tabs only) at the start of {@code line}. */
    private int leadingWhitespaceLength(final String line) {
        int n = 0;
        while (n < line.length() && (line.charAt(n) == ' ' || line.charAt(n) == '\t')) {
            n++;
        }
        return n;
    }
}
