/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catch-all for the remaining generic STYLE.md sections not owned by another rule class:
 * §1, §2, §3.2, §3.3, §4, §6, §8, §9, §15.
 */
public class MiscRule {

    private static final Set<String> TIGHT_PAREN_KEYWORDS =
            setOf("if", "while", "for", "switch");

    private final String language;

    public MiscRule(final String language) {
        this.language = language;
    }

    private static Set<String> setOf(final String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    // ── §1 Indentation ───────────────────────────────────────────────────────────
    /** Tab display size and spaces-per-level, per STYLE.md §1. Shared by any rule (this one or a
     *  future one, e.g. §8's signature wrapping) that needs to *generate* new indentation. */
    public static final int INDENT_WIDTH = 4;

    /**
     * Converts every line's leading indentation run to the requested style, per STYLE.md §1's
     * `indent-style = spaces | tabs` modes (resolved -- see "§1 indentation scope" in Resolved
     * Design Decisions). `indent-style = keep` is deliberately not handled here: it requires
     * whole-project context to determine the dominant style, which is a `Main.java`/
     * `Config.java`-orchestration-time decision made by a separate, not-yet-built detector class
     * -- that class is expected to resolve "keep" down to a concrete `spaces`/`tabs` choice and
     * call this method with that choice, so this method itself never has to interpret "keep".
     * Only the whitespace run at the very start of each line is touched; whitespace elsewhere
     * (mid-line alignment padding, trailing whitespace) is never indentation. A line whose
     * indentation width (tabs expanded at {@link #INDENT_WIDTH}) is not an exact multiple of
     * {@link #INDENT_WIDTH} is irregular/malformed indentation and is left completely untouched
     * rather than guessed at.
     */
    public String convertIndentation(final List<Token> tokens, final String indentStyle) {
        if (!"spaces".equals(indentStyle) && !"tabs".equals(indentStyle)) {
            throw new IllegalArgumentException("convertIndentation only handles spaces|tabs, got: " + indentStyle);
        }
        final StringBuilder out = new StringBuilder();
        boolean atLineStart = true;
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Token t = tokens.get(i);
            if (atLineStart && t.type == TokenType.WHITESPACE) {
                out.append(renderIndent(t.text, indentStyle));
                atLineStart = false;
                i++;
                continue;
            }
            out.append(t.text);
            atLineStart = (t.type == TokenType.NEWLINE);
            i++;
        }
        return out.toString();
    }

    private String renderIndent(final String original, final String indentStyle) {
        int width = 0;
        for (int i = 0; i < original.length(); i++) {
            width += (original.charAt(i) == '\t') ? (INDENT_WIDTH - (width % INDENT_WIDTH)) : 1;
        }
        if (width % INDENT_WIDTH != 0) {
            return original;
        }
        final int levels = width / INDENT_WIDTH;
        final boolean tabs = "tabs".equals(indentStyle);
        final char unit = tabs ? '\t' : ' ';
        final int count = tabs ? levels : levels * INDENT_WIDTH;
        final StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(unit);
        }
        return sb.toString();
    }

    // ── §2 Line Length ───────────────────────────────────────────────────────────
    /**
     * STYLE.md §2's 100-char soft limit. No rule in this class acts on it directly: §2 itself
     * defers its only described mechanical fix (line-breaking) to §8 (Function Signatures, not
     * yet implemented), and describes no other mechanical rewrite for an over-length line --
     * §2 is therefore a no-op section here beyond exposing this constant for §8's eventual use.
     */
    public static final int LINE_LENGTH_LIMIT = 100;

    // ── §3.2 Keyword spacing ─────────────────────────────────────────────────────
    /**
     * Collapses any whitespace-only gap between a control-flow keyword (`if`/`while`/`for`/
     * `switch` -- exactly STYLE.md §3.2's four keywords) and its following `(` down to zero
     * width. A comment or a `NEWLINE` in the gap blocks the rewrite for that occurrence, same
     * conservative posture as `BlockStructureRule`'s brace-style passes -- relocating a comment
     * unambiguously is out of scope. This method never touches what's *inside* the `(...)`;
     * deciding whether the contents are padded is STYLE.md §3.1, already implemented as the
     * `isLoose` evaluation in `ComplexityPaddingEvaluator` -- wiring that evaluation into an
     * actual rewrite pass is separate, not-yet-assigned work.
     */
    public String enforceKeywordSpacing(final List<Token> tokens) {
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

            final boolean collapse = isPunct(t, "(") && lastSignificant != null
                    && lastSignificant.type == TokenType.KEYWORD
                    && TIGHT_PAREN_KEYWORDS.contains(lastSignificant.text)
                    && gap.stream().noneMatch(this::isCommentOrNewline);
            if (!collapse) {
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

    // ── §3.3 `{}` initializer / block spacing ───────────────────────────────────
    /**
     * Normalizes spacing inside brace-initializer lists (array/struct initializers, `= { ... }`
     * contexts) per STYLE.md §3.3: empty `{}` is always tight; non-empty `{ ... }` gets exactly
     * one space just inside both the opening and closing brace, at every nesting level.
     * Control-flow and function/class body braces (STYLE.md §11/§7, already handled by
     * `BlockStructureRule`) are never touched -- those braces are never directly preceded by
     * `=`, `{`, or `,` while nested inside an already-recognized initializer, which is the
     * structural signal this method uses to recognize an initializer brace (no AST available;
     * confirmed against `BlockStructureRule.qualifiesForKAndR` that an initializer brace, whose
     * preceding token is `=`/`{`/`,`, never matches that method's K&R/lambda criteria, so the
     * two rules never fight over the same brace). A comment or `NEWLINE` immediately inside a
     * brace blocks the rewrite for that side, same conservative posture as the rest of this
     * file -- a genuinely multi-line initializer is left untouched.
     */
    public String enforceInitializerBraceSpacing(final List<Token> tokens) {
        final StringBuilder out = new StringBuilder();
        final List<Token> gap = new ArrayList<>();
        final Deque<Boolean> initStack = new ArrayDeque<>();
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

            final boolean afterInitOpen = isPunct(lastSignificant, "{")
                    && !initStack.isEmpty() && initStack.peek();
            final boolean beforeInitClose = isPunct(t, "}")
                    && !initStack.isEmpty() && initStack.peek();
            final boolean gapHasBlocker = gap.stream().anyMatch(this::isCommentOrNewline);

            if (afterInitOpen && isPunct(t, "}")) {
                gap.clear();
            } else if ((afterInitOpen || beforeInitClose) && !gapHasBlocker) {
                out.append(' ');
                gap.clear();
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
                gap.clear();
            }

            if (isPunct(t, "{")) {
                final boolean isInit = isOp(lastSignificant, "=")
                        || ((isPunct(lastSignificant, "{") || isPunct(lastSignificant, ","))
                                && !initStack.isEmpty() && initStack.peek());
                initStack.push(isInit);
            } else if (isPunct(t, "}") && !initStack.isEmpty()) {
                initStack.pop();
            }

            out.append(t.text);
            lastSignificant = t;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    // ── §4 Pre/Post Increment and Decrement ─────────────────────────────────────
    /**
     * Rewrites a bare postfix `i++;`/`i--;` expression statement (its value discarded) to
     * prefix `++i;`/`--i;`, per STYLE.md §4's "always prefer pre-increment except when post-
     * increment semantics are required by the surrounding expression" rule. Two shapes are
     * recognized, both via plain token-position checks (no AST):
     *   (1) A standalone statement: the IDENTIFIER is immediately preceded (ignoring a
     *       whitespace/comment/newline gap) by a statement boundary (`;`, `{`, `}`, or the
     *       start of the scope) and immediately followed by `;`, with paren/bracket depth 0 at
     *       that point -- this excludes a function call's arguments, an array index, and a
     *       for-loop header's own clauses (all "value used" or otherwise-handled contexts).
     *       A brace-less single-statement control-flow body (`if(x) i++;`) is NOT recognized as
     *       a boundary here -- that would require statement-start detection well beyond what
     *       STYLE.md's own "i++; at statement level" framing asks for, so it is left untouched
     *       as a documented gap rather than guessed at.
     *   (2) A `for(...; ...; i++)` loop's increment clause -- found by locating each `for`
     *       keyword's matching parens and splitting on the two top-level `;` inside; the third
     *       clause is rewritten when (and only when) it consists of exactly one IDENTIFIER
     *       followed by `++`/`--` and nothing else, since STYLE.md frames "prefer pre" as the
     *       general rule with exceptions only for value-is-used cases, and a discarded loop
     *       increment is not one of those. A more complex increment clause (comma-separated,
     *       `arr[i++]`, etc.) is left untouched -- STYLE.md gives no worked example beyond the
     *       single bare `i++`/`i--` shape.
     * Any candidate whose identifier/operator gap contains a comment or a `NEWLINE` is left
     * untouched, same conservative posture as the rest of this file.
     */
    public String enforcePreIncrement(final List<Token> tokens) {
        final Map<Integer, Integer> spans = new HashMap<>();
        collectBareStatementSpans(tokens, spans);
        collectForIncrementSpans(tokens, spans);
        return renderWithSwappedSpans(tokens, spans);
    }

    private void collectBareStatementSpans(final List<Token> tokens, final Map<Integer, Integer> spans) {
        final int n = tokens.size();
        int depth = 0;
        Token lastSignificant = null;

        for (int i = 0; i < n; i++) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                continue;
            }
            if (isPunct(t, "(") || isPunct(t, "[")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]")) {
                depth--;
            } else if (depth == 0 && t.type == TokenType.IDENTIFIER && isStatementBoundary(lastSignificant)) {
                final int opIdx = nextSignificantIndex(tokens, i + 1);
                if (opIdx >= 0 && isIncrementOp(tokens.get(opIdx)) && noBlockerBetween(tokens, i, opIdx)) {
                    final int termIdx = nextSignificantIndex(tokens, opIdx + 1);
                    if (termIdx >= 0 && isPunct(tokens.get(termIdx), ";")) {
                        spans.put(i, opIdx);
                    }
                }
            }
            lastSignificant = t;
        }
    }

    private void collectForIncrementSpans(final List<Token> tokens, final Map<Integer, Integer> spans) {
        final int n = tokens.size();

        for (int i = 0; i < n; i++) {
            if (tokens.get(i).type != TokenType.KEYWORD || !"for".equals(tokens.get(i).text)) {
                continue;
            }
            final int openParen = nextSignificantIndex(tokens, i + 1);
            if (openParen < 0 || !isPunct(tokens.get(openParen), "(")) {
                continue;
            }
            final int closeParen = matchParenForward(tokens, openParen);
            if (closeParen < 0) {
                continue;
            }

            final List<Integer> semiIdx = new ArrayList<>();
            int depth = 0;
            for (int k = openParen + 1; k < closeParen; k++) {
                final Token tk = tokens.get(k);
                if (isPunct(tk, "(") || isPunct(tk, "[")) {
                    depth++;
                } else if (isPunct(tk, ")") || isPunct(tk, "]")) {
                    depth--;
                } else if (depth == 0 && isPunct(tk, ";")) {
                    semiIdx.add(k);
                }
            }
            if (semiIdx.size() != 2) {
                continue;
            }

            final int incrStart = nextSignificantIndex(tokens, semiIdx.get(1) + 1);
            if (incrStart < 0 || incrStart >= closeParen || tokens.get(incrStart).type != TokenType.IDENTIFIER) {
                continue;
            }
            final int opIdx = nextSignificantIndex(tokens, incrStart + 1);
            if (opIdx < 0 || opIdx >= closeParen || !isIncrementOp(tokens.get(opIdx))
                    || !noBlockerBetween(tokens, incrStart, opIdx)) {
                continue;
            }
            final int afterOp = nextSignificantIndex(tokens, opIdx + 1);
            if (afterOp != closeParen) {
                continue;
            }
            spans.put(incrStart, opIdx);
        }
    }

    private String renderWithSwappedSpans(final List<Token> tokens, final Map<Integer, Integer> spans) {
        final StringBuilder out = new StringBuilder();
        final int n = tokens.size();
        int i = 0;

        while (i < n) {
            final Integer opIdx = spans.get(i);
            if (opIdx != null) {
                out.append(tokens.get(opIdx).text).append(tokens.get(i).text);
                i = opIdx + 1;
                continue;
            }
            out.append(tokens.get(i).text);
            i++;
        }
        return out.toString();
    }

    private int matchParenForward(final List<Token> tokens, final int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), "(")) {
                depth++;
            } else if (isPunct(tokens.get(i), ")")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isStatementBoundary(final Token t) {
        return t == null || isPunct(t, ";") || isPunct(t, "{") || isPunct(t, "}");
    }

    private boolean isIncrementOp(final Token t) {
        return t.type == TokenType.OP && ("++".equals(t.text) || "--".equals(t.text));
    }

    private boolean noBlockerBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            if (isCommentOrNewline(tokens.get(i))) {
                return false;
            }
        }
        return true;
    }

    private int nextSignificantIndex(final List<Token> tokens, final int from) {
        int i = from;
        while (i < tokens.size() && isGapToken(tokens.get(i))) {
            i++;
        }
        return i < tokens.size() ? i : -1;
    }

    // ── §6 Assignment and Compound Operator Alignment ───────────────────────────
    private static final Set<String> ASSIGNMENT_OPS = setOf(
            "=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=");

    /** One parsed bare assignment statement (`target op value;` -- no declared type; a typed
     *  declaration's own `= value` is STYLE.md §5/`DeclarationAlignmentRule`'s concern, not this
     *  rule's). */
    public static final class Assignment {
        public final Token target;
        public final Token operator;
        public final List<Token> valueTokens;
        public final Token trailingComment; // nullable
        public final boolean blankLineBefore;

        Assignment(final Token target, final Token operator, final List<Token> valueTokens,
                final Token trailingComment, final boolean blankLineBefore) {
            this.target = target;
            this.operator = operator;
            this.valueTokens = valueTokens;
            this.trailingComment = trailingComment;
            this.blankLineBefore = blankLineBefore;
        }
    }

    /**
     * Splits one scope's direct-content tokens (caller-extracted, no deeper-nested tokens --
     * same scoping contract as `DeclarationAlignmentRule.groupDeclarations`) into maximal runs of
     * textually-adjacent bare assignment statements (STYLE.md §6, resolved -- see "§6 grouping
     * and rendering" in Resolved Design Decisions: same textually-adjacent-run signal as §14). A
     * blank line, a comment-only gap, or any statement not recognized as a bare assignment breaks
     * the current run. Unlike `GetterSetterRule.groupOneLiners`'s 2+ minimum, a run of length 1 is
     * still returned here -- STYLE.md §6 explicitly wants a lone variable to "align trivially with
     * itself," which {@link #render} achieves for free (group size 1 means both padding widths
     * just equal that one row's own widths).
     */
    public List<List<Assignment>> groupAssignments(final List<Token> scopeTokens) {
        final List<List<Token>> statements = splitAssignmentStatements(scopeTokens);
        final List<List<Assignment>> groups = new ArrayList<>();
        List<Assignment> current = new ArrayList<>();

        for (final List<Token> stmt : statements) {
            final boolean blankBefore = hasBlankLineBeforeStmt(stmt);
            final Assignment a = parseAssignment(stmt, blankBefore);
            if (a == null) {
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
            current.add(a);
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    /**
     * Renders one alignment group (STYLE.md §6) as two independently fixed-width columns per
     * row -- `maxNameLen` (the widest target name) and `maxPrefixLen` (the widest operator text
     * minus its trailing `=`) -- so that every row's `=` lands on the same column regardless of
     * which compound operator it uses (resolved -- see "§6 grouping and rendering": a single
     * `ColumnGrid` left-pad column on the concatenated name+operator text does NOT reproduce
     * STYLE.md's worked example; this manual two-field padding does). The right-hand side is
     * never reformatted -- its original token text (including internal spacing) is reproduced
     * verbatim, since STYLE.md describes alignment of the `=` column only, not a rewrite of
     * arbitrary expression spacing. An optional trailing-comment column reuses `ColumnGrid` to
     * align comments across the group, same precedent as `DeclarationAlignmentRule`/
     * `GetterSetterRule`. Multi-line right-hand sides are out of scope (see the checklist's
     * deferred sub-item) -- `parseAssignment` never returns one, so every row reaching this
     * method is single-line.
     */
    public List<String> render(final List<Assignment> group) {
        int maxNameLen = 0;
        int maxPrefixLen = 0;
        for (final Assignment a : group) {
            maxNameLen = Math.max(maxNameLen, a.target.text.length());
            maxPrefixLen = Math.max(maxPrefixLen, assignOpPrefix(a.operator).length());
        }
        // +1 unconditionally -- even the group's widest operator still needs its own leading
        // space (verified against STYLE.md's worked example: maxPrefixLen=2 from ">>=" there,
        // but every row's rendered gap is 3, i.e. naturalMax+1, not naturalMax)
        maxPrefixLen++;

        final ColumnGrid grid = new ColumnGrid();
        for (final Assignment a : group) {
            final String lhs = padRight(a.target.text, maxNameLen)
                    + padLeft(assignOpPrefix(a.operator), maxPrefixLen) + "=";
            final List<String> cells = new ArrayList<>();
            cells.add(lhs);
            cells.add(joinVerbatim(a.valueTokens) + ";");
            if (a.trailingComment != null) {
                cells.add(a.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }

        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(String.join(" ", row));
        }
        return lines;
    }

    private String assignOpPrefix(final Token operator) {
        return operator.text.substring(0, operator.text.length() - 1);
    }

    private String joinVerbatim(final List<Token> tokens) {
        final StringBuilder sb = new StringBuilder();
        for (final Token t : tokens) {
            sb.append(t.text);
        }
        return sb.toString();
    }

    private static String padRight(final String s, final int width) {
        final StringBuilder sb = new StringBuilder(s);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String padLeft(final String s, final int width) {
        final StringBuilder sb = new StringBuilder();
        while (sb.length() + s.length() < width) {
            sb.append(' ');
        }
        sb.append(s);
        return sb.toString();
    }

    /**
     * Parses the shape `target op value ;` (STYLE.md §6) entirely on one source line.
     * `target` must be a single bare `IDENTIFIER` -- a member access (`obj.field`), an array
     * element (`arr[i]`), or a pointer deref (`*ptr`) has no STYLE.md worked example to justify
     * guessing at, so any of those leave this method returning null (the statement is left
     * untouched and breaks the current alignment run, same as any other unrecognized statement).
     * A comment or `NEWLINE` anywhere between the target and the terminating `;` blocks
     * recognition entirely -- including the multi-line-right-hand-side case, whose continuation-
     * line rendering is a distinct, not-yet-designed sub-item (see the checklist).
     */
    private Assignment parseAssignment(final List<Token> stmt, final boolean blankBefore) {
        final int targetIdx = nextSignificantIndex(stmt, 0);
        if (targetIdx < 0 || stmt.get(targetIdx).type != TokenType.IDENTIFIER) {
            return null;
        }
        final int opIdx = nextSignificantIndex(stmt, targetIdx + 1);
        if (opIdx < 0 || stmt.get(opIdx).type != TokenType.OP
                || !ASSIGNMENT_OPS.contains(stmt.get(opIdx).text)) {
            return null;
        }
        final int valueFrom = nextSignificantIndex(stmt, opIdx + 1);
        if (valueFrom < 0) {
            return null;
        }
        final int semiIdx = findTopLevelSemicolon(stmt, valueFrom);
        if (semiIdx < 0) {
            return null;
        }
        int valueTo = semiIdx;
        while (valueTo > valueFrom && isGapToken(stmt.get(valueTo - 1))) {
            valueTo--;
        }
        if (valueTo <= valueFrom || !noBlockerBetween(stmt, targetIdx, semiIdx)) {
            return null;
        }
        for (int i = semiIdx + 1; i < stmt.size(); i++) {
            final Token t = stmt.get(i);
            if (t.type != TokenType.WHITESPACE && t.type != TokenType.COMMENT_LINE
                    && t.type != TokenType.COMMENT_BLOCK) {
                return null; // stray tokens after `;` -- not a clean single statement
            }
        }

        final List<Token> value = new ArrayList<>(stmt.subList(valueFrom, valueTo));
        return new Assignment(stmt.get(targetIdx), stmt.get(opIdx), value,
                findTrailingAssignComment(stmt), blankBefore);
    }

    private int findTopLevelSemicolon(final List<Token> tokens, final int from) {
        int depth = 0;
        for (int i = from; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
            } else if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                depth--;
            } else if (depth == 0 && isPunct(t, ";")) {
                return i;
            }
        }
        return -1;
    }

    private Token findTrailingAssignComment(final List<Token> stmt) {
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

    /**
     * Splits scope tokens into statement-or-block spans, depth-tracked across `(`/`[`/`{` (and
     * their closes) so that neither a parenthesized sub-expression's internal punctuation (e.g. a
     * `for(...; ...; ...)` header's own `;`s, or a lambda body's own `;`) nor a nested `{ }` block
     * ends the span early -- only a `;` or a balancing `}` at combined depth 0 does. A balancing
     * `}` produces an opaque span (e.g. an `if`/`for`/method-body block that leaked into this
     * scope) that `parseAssignment` will always reject, same as any other unrecognized statement.
     * A same-line trailing comment is pulled into the span it follows, same precedent as
     * `DeclarationAlignmentRule.splitStatements`.
     */
    private List<List<Token>> splitAssignmentStatements(final List<Token> scopeTokens) {
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
                    idx = pullTrailingSameLine(scopeTokens, current, idx);
                    statements.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            if (depth == 0 && isPunct(t, ";")) {
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

    /** Same blank-line-before detection as `DeclarationAlignmentRule.hasBlankLineBefore`. */
    private boolean hasBlankLineBeforeStmt(final List<Token> stmt) {
        int newlineRun = 0;
        for (final Token t : stmt) {
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

    // ── Token-scanning helpers ───────────────────────────────────────────────────
    private boolean isGapToken(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    private boolean isCommentOrNewline(final Token t) {
        return t.type == TokenType.NEWLINE || t.type == TokenType.COMMENT_LINE
                || t.type == TokenType.COMMENT_BLOCK;
    }

    private boolean isPunct(final Token t, final String text) {
        return t != null && t.type == TokenType.PUNCT && text.equals(t.text);
    }

    private boolean isOp(final Token t, final String text) {
        return t != null && t.type == TokenType.OP && text.equals(t.text);
    }
}
