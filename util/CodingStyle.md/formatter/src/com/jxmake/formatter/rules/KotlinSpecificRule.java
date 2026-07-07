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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** Tracks, for one open `{`, whether it is an `enum class` body and whether its
     *  mandatory entries/members `;` separator has already been located. */
    private static final class EnumBodyState {
        final boolean isEnumClass;
        boolean separatorFound;

        EnumBodyState(final boolean isEnumClass) {
            this.isEnumClass = isEnumClass;
        }
    }

    /**
     * STYLE_KOTLIN.md §1: strip all optional statement-terminating `;`. The only `;` kept is an
     * `enum class` body's entries/members separator (§2), and only when member declarations
     * actually follow it -- if nothing follows before the closing `}`, that `;` is optional too
     * and gets stripped like any other.
     */
    public List<Token> stripOptionalSemicolons(final List<Token> tokens) {
        final Deque<EnumBodyState> enumBodies = new ArrayDeque<>();
        boolean sawEnum = false;
        boolean sawEnumClass = false;

        // Pass 1: identify which `;` token indices are the mandatory enum separator.
        final boolean[] keep = new boolean[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                continue;
            }
            if (t.type == TokenType.KEYWORD && "enum".equals(t.text)) {
                sawEnum = true;
                continue;
            }
            if (t.type == TokenType.KEYWORD && "class".equals(t.text) && sawEnum) {
                sawEnumClass = true;
                sawEnum = false;
                continue;
            }
            if (isPunct(t, "{")) {
                enumBodies.push(new EnumBodyState(sawEnumClass));
                sawEnumClass = false;
                sawEnum = false;
                continue;
            }
            if (isPunct(t, "}")) {
                if (!enumBodies.isEmpty()) {
                    enumBodies.pop();
                }
                sawEnum = false;
                sawEnumClass = false;
                continue;
            }
            if (isPunct(t, ";")) {
                final EnumBodyState body = enumBodies.isEmpty() ? null : enumBodies.peek();
                if (body != null && body.isEnumClass && !body.separatorFound
                        && hasMoreContentBeforeClose(tokens, i)) {
                    body.separatorFound = true;
                    keep[i] = true;
                }
                sawEnum = false;
                sawEnumClass = false;
                continue;
            }
            // Any other token (the class name, generics, a supertype/constructor clause) is
            // part of the still-open `enum class ... {` header -- leave the pending flags alone
            // so they survive until the body's opening `{` is reached. `enum` on its own (with no
            // `class` yet observed) is cleared only by reaching another `enum`/`class`/`{`/`}`/`;`.
        }

        // Pass 2: rebuild the list, dropping every `;` not marked to keep.
        final List<Token> result = new ArrayList<>(tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, ";") && !keep[i]) {
                continue;
            }
            result.add(t);
        }
        return result;
    }

    /** True if there is at least one non-gap, non-`}` token between {@code semicolonIdx} and
     *  the `}` that closes the current brace depth -- i.e. member declarations actually follow. */
    private boolean hasMoreContentBeforeClose(final List<Token> tokens, final int semicolonIdx) {
        for (int j = semicolonIdx + 1; j < tokens.size(); j++) {
            final Token t = tokens.get(j);
            if (isGapToken(t)) {
                continue;
            }
            return !isPunct(t, "}");
        }
        return false;
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
}
