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

    public KotlinSpecificRule(final Lang lang) {
        this(lang, MiscRule.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public KotlinSpecificRule(final Lang lang, final int lineLengthLimit) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
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
}
