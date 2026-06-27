/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * C/C++-specific STYLE_C_CPP.md sections not owned by another rule class: §1, §2 (Allman
 * conversion), §3, §4, §9, §10, §11. (§5/§6 are already handled by {@code MiscRule}/
 * {@code DeclarationAlignmentRule}; §7's additional C/C++ closing-comment cases and the lambda
 * part of §2 are already handled by {@code BlockStructureRule}; §8 is explicitly
 * preserve-as-is.)
 */
public class CppSpecificRule {

    /** STYLE_C_CPP.md §10: number of blank lines required between header zones. */
    private static final int HEADER_ZONE_BLANK_LINES = 2;

    /** One indentation level, used by {@link #enforceRequiresClausePlacement} when wrapping a
     *  trailing `requires` clause to its own line -- same precedent as
     *  {@code JavaSpecificRule.DEFAULT_INDENT_UNIT}/{@code SwitchRule.DEFAULT_INDENT_UNIT}. */
    private static final String DEFAULT_INDENT_UNIT = "    ";

    private final String language;

    public CppSpecificRule(final String language) {
        this.language = language;
    }

    /**
     * STYLE_C_CPP.md §1: C always writes an empty parameter list as `(void)`; C++ always omits
     * it (`()`). Resolved -- see STATE.md "§1 empty parameter list": rather than build general
     * declaration-vs-call detection (no STYLE.md worked example exercises that distinction, and
     * the cost of guessing wrong here is corrupting working code, not just a missed format), the
     * two directions are handled with two different, independently-safe signals:
     * <ul>
     * <li>C, `()` → `(void)`: only rewritten when the matching `)` is directly followed by `{`
     * (skipping whitespace/comments) -- i.e. only recognized function <b>definitions</b>, the
     * same "identifier before `(`, body brace after `)`" signal already used elsewhere in this
     * codebase (see {@code BlockStructureRule}'s function-body brace detection) to distinguish a
     * definition from a call without an AST, since a call is never followed by `{`. A bare
     * prototype (`void foo();`, no body) is a documented, deliberate gap -- it is structurally
     * identical to a call (`foo();`) without a body-brace signal to anchor on, so it is left
     * untouched rather than guessed at.</li>
     * <li>C++, `(void)` → `()`: rewritten wherever it occurs, body or no body, with no extra
     * signal needed -- `IDENTIFIER(void)` is never valid call syntax (`void` cannot be passed as
     * an argument), so every occurrence directly after a candidate function name is a signature,
     * by construction of the language itself.</li>
     * </ul>
     * Both directions require the identifier immediately before `(` to not itself be preceded by
     * `new` (excludes a constructor call via `new Identifier()`, same guard already used by
     * {@code BlockStructureRule.isFunctionBodyBrace}), and both skip the rewrite entirely if a
     * comment sits between the parens (nothing to guess at structurally, but consistent with this
     * file's existing "a comment in the gap blocks the rewrite" posture).
     */
    public String enforceEmptyParameterList(final List<Token> tokens) {
        final boolean isC = "c".equals(language);
        final Map<Integer, Integer> spans = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), "(") || !isCandidateSignatureName(tokens, i)) {
                continue;
            }
            final int closeIdx = matchParenForward(tokens, i);
            if (closeIdx < 0 || hasCommentBetween(tokens, i, closeIdx)) {
                continue;
            }
            if (isC) {
                if (isEmptyBetween(tokens, i, closeIdx) && isFollowedByFunctionBody(tokens, closeIdx)) {
                    spans.put(i, closeIdx);
                }
            } else if (isOnlyVoidBetween(tokens, i, closeIdx)) {
                spans.put(i, closeIdx);
            }
        }

        final StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < tokens.size()) {
            final Integer closeIdx = spans.get(i);
            if (closeIdx != null) {
                out.append(isC ? "(void)" : "()");
                i = closeIdx + 1;
            } else {
                out.append(tokens.get(i).text);
                i++;
            }
        }
        return out.toString();
    }

    /** True iff the token immediately before {@code openIdx} is an IDENTIFIER not itself preceded
     *  by `new` -- the candidate-function-name signal shared by both rewrite directions. */
    private boolean isCandidateSignatureName(final List<Token> tokens, final int openIdx) {
        final int nameIdx = prevSignificantIndex(tokens, openIdx);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return false;
        }
        final int beforeName = prevSignificantIndex(tokens, nameIdx);
        return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                || !"new".equals(tokens.get(beforeName).text);
    }

    /** True iff {@code closeIdx} (a `)`) is directly followed by `{` -- the function-definition
     *  signal, never true for a call. */
    private boolean isFollowedByFunctionBody(final List<Token> tokens, final int closeIdx) {
        final int next = nextSignificantIndex(tokens, closeIdx);
        return next >= 0 && isPunct(tokens.get(next), "{");
    }

    private boolean isEmptyBetween(final List<Token> tokens, final int openIdx, final int closeIdx) {
        for (int i = openIdx + 1; i < closeIdx; i++) {
            if (!isGapToken(tokens.get(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isOnlyVoidBetween(final List<Token> tokens, final int openIdx, final int closeIdx) {
        int sigIdx = -1;
        for (int i = openIdx + 1; i < closeIdx; i++) {
            if (isGapToken(tokens.get(i))) {
                continue;
            }
            if (sigIdx != -1) {
                return false;
            }
            sigIdx = i;
        }
        return sigIdx != -1 && tokens.get(sigIdx).type == TokenType.KEYWORD
                && "void".equals(tokens.get(sigIdx).text);
    }

    private boolean hasCommentBetween(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    /**
     * STYLE_C_CPP.md §2 (brace-placement half only): a recognized function <b>definition</b>'s
     * own brace moves to its own line (Allman) whenever it is currently K&amp;R/same-line -- the
     * exact inverse of {@code BlockStructureRule.enforceKAndRBraceStyle}, which deliberately
     * leaves this exact shape (`)` preceded by an IDENTIFIER) untouched today. Resolved -- see
     * STATE.md "§2 one-liner scope": asked the user whether the one-liner exception should ever
     * actively collapse an existing multi-line body down to a one-liner; the user chose
     * brace-placement only -- this method never inspects or changes how many physical lines the
     * body itself spans, it only ever relocates the opening `{`. That single rule already
     * reproduces both of STYLE_C_CPP.md §2's worked examples for free: a body that already sits
     * on one physical line (`{ _x = 0; }`) keeps that shape verbatim, just moved down a line; a
     * body that already spans multiple lines keeps that shape too.
     *
     * <p>Candidate signal reuses §1's {@code isCandidateSignatureName}: the `{` is directly
     * preceded (no comment, no newline in the gap) by a `)` whose matching `(` is itself preceded
     * by a candidate function name (IDENTIFIER, not itself preceded by `new`) -- this naturally
     * excludes every control-flow brace (`if`/`while`/`for`/`switch`/`catch` precede their `(`
     * with a KEYWORD, never an IDENTIFIER) and every lambda (a lambda's `{` is preceded by
     * `]`/a `)` whose matching `(` is preceded by `]`, or by a trailing return type -- never
     * directly by a bare `)`-after-identifier). A trailing qualifier or return type between `)`
     * and `{` (`void foo() const { ... }`, `auto foo() -> int { ... }`) is a documented,
     * deliberate gap, identical in spirit to the one already accepted for §9's
     * blank-line-before-return rule -- the immediate-predecessor check that excludes
     * control-flow/lambda braces also excludes these, and there is no STYLE.md worked example to
     * justify guessing past that signal. A `{` already on its own line (gap already contains a
     * NEWLINE) is left untouched -- idempotent, and the per-occurrence indentation target reuses
     * the closing `)`'s own line-leading indentation, which by STYLE.md §8's own rule ("closing
     * `)` ... indented to match the first character of the function signature itself") is correct
     * whether the signature is a single line or already broken across several.
     *
     * <p>RDD_KEY_75 (supersedes RDD_KEY_60): a one-liner function whose entire `{ ... }` body
     * sits on one physical line is deferred rather than converted immediately -- if it is
     * textually adjacent (no blank line, no comment-only gap) to another such one-liner, both are
     * left alone, mirroring {@code JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle}'s
     * identical fix (see that method's doc comment for the full rationale).
     */
    public String enforceFunctionDefinitionAllmanBraceStyle(final List<Token> tokens) {
        final Map<Integer, Integer> gapToBrace = new HashMap<>();
        final List<OneLinerCandidate> oneLiners = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            if (!isPunct(tokens.get(i), "{")) {
                continue;
            }
            // Walk past post-paren qualifiers (const, volatile, noexcept, override, final)
            // so that e.g. `func() const {` is handled the same as `func() {`.
            int closeParenIdx = prevSignificantIndex(tokens, i);
            while (closeParenIdx >= 0 && isDefinitionQualifier(tokens.get(closeParenIdx))) {
                closeParenIdx = prevSignificantIndex(tokens, closeParenIdx);
            }
            if (closeParenIdx < 0 || !isPunct(tokens.get(closeParenIdx), ")")) {
                continue;
            }
            if (!isFunctionDefinitionCloseParen(tokens, closeParenIdx)) {
                continue;
            }
            if (hasNewlineOrCommentBetween(tokens, closeParenIdx, i)) {
                continue;
            }
            // gapStart: first token after the last qualifier (or after ")"), so qualifiers
            // before "{" are preserved in the output and only the trailing whitespace is replaced.
            final int lastBeforeBrace = prevSignificantIndex(tokens, i);
            final int gapStart = lastBeforeBrace + 1;
            final int closeBraceIdx = matchBraceForward(tokens, i);
            if (closeBraceIdx >= 0 && isSingleLineBody(tokens, i, closeBraceIdx)) {
                final int openParenIdx = matchParenBackward(tokens, closeParenIdx);
                final int nameIdx = prevSignificantIndex(tokens, openParenIdx);
                oneLiners.add(new OneLinerCandidate(nameIdx, closeParenIdx, i, closeBraceIdx, gapStart));
                continue;
            }
            gapToBrace.put(gapStart, i);
        }

        final boolean[] grouped = new boolean[oneLiners.size()];
        for (int idx = 1; idx < oneLiners.size(); idx++) {
            final OneLinerCandidate prev = oneLiners.get(idx - 1);
            final OneLinerCandidate cur = oneLiners.get(idx);
            final int prevBoundary = findPrevSiblingBoundary(tokens, cur.nameIdx);
            if (prevBoundary == prev.closeBraceIdx && !breaksOneLinerRun(tokens, prevBoundary, cur.nameIdx)) {
                grouped[idx - 1] = true;
                grouped[idx] = true;
            }
        }
        for (int idx = 0; idx < oneLiners.size(); idx++) {
            if (!grouped[idx]) {
                final OneLinerCandidate c = oneLiners.get(idx);
                gapToBrace.put(c.gapStart, c.braceIdx);
            }
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

    /** One function-definition `{ ... }` whose body sits entirely on one physical line --
     *  candidate for staying K&amp;R if adjacent to another one-liner (RDD_KEY_75). */
    private static final class OneLinerCandidate {
        final int nameIdx;
        final int closeParenIdx;
        final int braceIdx;
        final int closeBraceIdx;
        final int gapStart; // first token index after last qualifier (or after ")"), for Allman key

        OneLinerCandidate(final int nameIdx, final int closeParenIdx, final int braceIdx,
                final int closeBraceIdx, final int gapStart) {
            this.nameIdx = nameIdx;
            this.closeParenIdx = closeParenIdx;
            this.braceIdx = braceIdx;
            this.closeBraceIdx = closeBraceIdx;
            this.gapStart = gapStart;
        }
    }

    /** True iff no {@code NEWLINE} token appears between {@code braceIdx} and {@code closeBraceIdx}
     *  inclusive -- the whole `{ ... }` span is one physical line. */
    private boolean isSingleLineBody(final List<Token> tokens, final int braceIdx, final int closeBraceIdx) {
        for (int i = braceIdx; i <= closeBraceIdx; i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                return false;
            }
        }
        return true;
    }

    /** Scans backward from {@code fromIdx} for the nearest top-level `}`/`;` (the previous
     *  sibling's own end), or -1 if a `{` is hit first or the start of the token list is reached.
     *  Bounded-effort: does not depth-track, same posture as the rest of this codebase's
     *  non-AST heuristics. */
    private int findPrevSiblingBoundary(final List<Token> tokens, final int fromIdx) {
        for (int i = fromIdx - 1; i >= 0; i--) {
            final Token t = tokens.get(i);
            if (isPunct(t, "}") || isPunct(t, ";")) {
                return i;
            }
            if (isPunct(t, "{")) {
                return -1;
            }
        }
        return -1;
    }

    /** True iff a blank line (two or more consecutive {@code NEWLINE} tokens) or any comment
     *  appears strictly between {@code fromExclusive} and {@code toExclusive}. */
    private boolean breaksOneLinerRun(final List<Token> tokens, final int fromExclusive, final int toExclusive) {
        int newlineRun = 0;
        for (int i = fromExclusive + 1; i < toExclusive; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.NEWLINE) {
                newlineRun++;
                if (newlineRun >= 2) {
                    return true;
                }
            } else if (type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) {
                return true;
            } else if (type != TokenType.WHITESPACE) {
                newlineRun = 0;
            }
        }
        return false;
    }

    /** Forward `{`/`}` bracket match -- the brace-pair analog of {@link #matchParenForward}. */
    private int matchBraceForward(final List<Token> tokens, final int openIdx) {
        int depth = 0;
        for (int i = openIdx; i < tokens.size(); i++) {
            if (isPunct(tokens.get(i), "{")) {
                depth++;
            } else if (isPunct(tokens.get(i), "}")) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private boolean isFunctionDefinitionCloseParen(final List<Token> tokens, final int closeParenIdx) {
        final int openParenIdx = matchParenBackward(tokens, closeParenIdx);
        return openParenIdx >= 0 && isCandidateSignatureName(tokens, openParenIdx);
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

    /** Line-leading whitespace of the physical line containing token {@code idx} -- "" if that
     *  line has no leading whitespace (column-0 start). */
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

    /**
     * STYLE_C_CPP.md §3: a single-level template angle-bracket pair stays tight
     * (`vector<int>`); any pair whose content (anywhere within its span, however deeply nested)
     * contains another `<>` pair gets exactly one space padded just inside both its own `<` and
     * its own `>` (`vector< vector<int> >`). Flagged in STATE.md as a correctness rule, not just
     * style -- the padding is what prevents two adjacent `>` characters belonging to different
     * pairs from being lexed as a single `>>` shift operator on pre-C++11 compilers -- so this is
     * fully specified already (unlike several other sections in this file, no
     * {@code AskUserQuestion} was needed): every matched pair is classified as tight or padded by
     * this one rule, with no remaining ambiguity to resolve.
     *
     * <p>Pair matching reuses the tokenizer's own {@code ANGLE_BRACKET_OPEN}/{@code _CLOSE}
     * disambiguation (the same token types {@code MiscRule.parseSignature} tracks for Java
     * generics) via a simple forward stack, since the tokenizer has already resolved any `>>`
     * lexing ambiguity at tokenize time -- by the time these tokens reach this rule, nesting is
     * already unambiguous and properly paired. A pair "contains another `<>` at any depth" iff at
     * least one other {@code ANGLE_BRACKET_OPEN} token lies strictly between its own open and
     * close indices, regardless of how many other tokens or how many nesting levels separate
     * them -- this is what correctly pads <i>every</i> ancestor pair on a 3+-level chain
     * (`A< B< C<int> > >`), not just the immediate outer pair, while leaving the innermost,
     * childless pair tight.
     *
     * <p>Rendering reuses the same gap-buffering technique as {@code MiscRule}'s spacing passes
     * (e.g. {@code enforceInitializerBraceSpacing}): the gap immediately after a flagged
     * {@code ANGLE_BRACKET_OPEN} and immediately before a flagged {@code ANGLE_BRACKET_CLOSE} is
     * collapsed to exactly one space (zero width for an unflagged/tight pair's own open or
     * close), unless that gap contains a comment or newline, which blocks the rewrite for that
     * one side only -- consistent with this codebase's existing "a comment/newline in the gap
     * blocks the rewrite" posture throughout. Since the C tokenizer never emits
     * {@code ANGLE_BRACKET_OPEN}/{@code _CLOSE} tokens at all (templates don't exist in C), this
     * method is a no-op (output equals input) when {@code language} is `"c"`, with no separate
     * early-return guard needed.
     */
    public String enforceTemplateAngleBracketSpacing(final List<Token> tokens) {
        final Set<Integer> needsPadding = nestedAnglePairIndices(tokens);

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

            final boolean afterOpen = lastSignificant != null
                    && lastSignificant.type == TokenType.ANGLE_BRACKET_OPEN;
            final boolean beforeClose = t.type == TokenType.ANGLE_BRACKET_CLOSE;
            final boolean gapHasBlocker = hasCommentOrNewline(gap);

            if ((afterOpen || beforeClose) && !gapHasBlocker) {
                final boolean pad = (afterOpen && needsPadding.contains(lastSignificantIdx))
                        || (beforeClose && needsPadding.contains(i));
                if (pad) {
                    out.append(' ');
                }
                gap.clear();
            } else {
                for (final Token g : gap) {
                    out.append(g.text);
                }
                gap.clear();
            }

            // Always one literal char per angle token, never t.text: the tokenizer's `>>`
            // split keeps both characters on the first ANGLE_BRACKET_CLOSE token's text (">>")
            // and gives the second one a zero-width placeholder token right after, so any space
            // padding inserted between the two must land between two single-char emissions, not
            // after a 2-char one.
            if (t.type == TokenType.ANGLE_BRACKET_OPEN) {
                out.append('<');
            } else if (t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                out.append('>');
            } else {
                out.append(t.text);
            }
            lastSignificant = t;
            lastSignificantIdx = i;
            i++;
        }
        for (final Token g : gap) {
            out.append(g.text);
        }
        return out.toString();
    }

    /** Indices of every {@code ANGLE_BRACKET_OPEN}/{@code _CLOSE} token that is part of a
     *  matched pair whose span contains at least one other such pair -- the set of "needs
     *  padding" occurrences for {@link #enforceTemplateAngleBracketSpacing}. */
    private Set<Integer> nestedAnglePairIndices(final List<Token> tokens) {
        final Deque<Integer> openStack = new ArrayDeque<>();
        final Map<Integer, Integer> openToClose = new HashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            final TokenType ty = tokens.get(i).type;
            if (ty == TokenType.ANGLE_BRACKET_OPEN) {
                openStack.push(i);
            } else if (ty == TokenType.ANGLE_BRACKET_CLOSE && !openStack.isEmpty()) {
                openToClose.put(openStack.pop(), i);
            }
        }

        final Set<Integer> needsPadding = new HashSet<>();
        for (final Map.Entry<Integer, Integer> entry : openToClose.entrySet()) {
            final int openIdx = entry.getKey();
            final int closeIdx = entry.getValue();
            boolean hasNested = false;
            for (int j = openIdx + 1; j < closeIdx; j++) {
                if (tokens.get(j).type == TokenType.ANGLE_BRACKET_OPEN) {
                    hasNested = true;
                    break;
                }
            }
            if (hasNested) {
                needsPadding.add(openIdx);
                needsPadding.add(closeIdx);
            }
        }
        return needsPadding;
    }

    private boolean hasCommentOrNewline(final List<Token> gap) {
        for (final Token g : gap) {
            final TokenType type = g.type;
            if (type == TokenType.NEWLINE || type == TokenType.COMMENT_LINE
                    || type == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    /**
     * STYLE_CPP20.md §2.2/§2.3: a trailing `requires` clause on a function/template signature
     * always trails the closing `)` of the parameter list -- on the same line if the combined
     * line (the physical line the `)` already sits on, plus ` requires <clause>`) fits within
     * {@link MiscRule#LINE_LENGTH_LIMIT}, otherwise wrapped to its own line indented one level
     * under the function name's line-leading indent. Detection: a KEYWORD `requires` token whose
     * previous significant token is `)` is a trailing clause (it follows a parameter list);
     * anything else (`=`, or no previous token at all) is a requires-<i>expression</i> body
     * (`concept Drawable = requires(T t) { ... }`) -- left completely untouched, same posture as
     * STYLE_CPP20.md §2.1/§2.3's "nested compound requirements ... left completely untouched".
     *
     * <p>The clause's own end is the first `{` or `;` reached scanning forward from `requires`
     * -- a deliberate, non-AST signal, same conservative posture as the rest of this file: an ad
     * hoc requires-<i>expression</i> used as the entire trailing constraint (`f() requires
     * requires(T a) { a.foo(); }`) has no STYLE_CPP20.md worked example and is a documented gap,
     * not specially detected -- its own `{` would be (mis)read as the terminator. A comment
     * anywhere in the replaced span blocks the rewrite entirely, consistent with this file's
     * existing "a comment in the gap blocks the rewrite" posture.
     */
    public String enforceRequiresClausePlacement(final List<Token> tokens) {
        final List<int[]> spans = new ArrayList<>();
        final List<String> renders = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (t.type != TokenType.KEYWORD || !"requires".equals(t.text)) {
                continue;
            }
            final int closeParenIdx = prevSignificantIndex(tokens, i);
            if (closeParenIdx < 0 || !isPunct(tokens.get(closeParenIdx), ")")) {
                continue;
            }
            final int clauseEndIdx = findRequiresClauseEnd(tokens, i);
            if (clauseEndIdx < 0 || clauseEndIdx <= i + 1) {
                continue;
            }
            if (hasCommentBetween(tokens, closeParenIdx, clauseEndIdx)) {
                continue;
            }

            final int lineStartIdx = lineStartIndex(tokens, closeParenIdx);
            final String baseIndent = lineIndent(tokens, closeParenIdx);
            final String clauseExpr = collapseToOneLine(tokens, i + 1, clauseEndIdx - 1);
            final String combined = baseIndent + collapseToOneLine(tokens, lineStartIdx, closeParenIdx)
                    + " requires " + clauseExpr;

            String rendered = combined.length() <= MiscRule.LINE_LENGTH_LIMIT
                    ? " requires " + clauseExpr
                    : "\n" + baseIndent + DEFAULT_INDENT_UNIT + "requires " + clauseExpr;
            if (isPunct(tokens.get(clauseEndIdx), "{")) {
                rendered += " ";
            }

            spans.add(new int[] { closeParenIdx + 1, clauseEndIdx });
            renders.add(rendered);
        }

        if (spans.isEmpty()) {
            return joinVerbatim(tokens);
        }

        final StringBuilder out = new StringBuilder();
        int cursor = 0;
        for (int s = 0; s < spans.size(); s++) {
            final int[] span = spans.get(s);
            appendRange(out, tokens, cursor, span[0]);
            out.append(renders.get(s));
            cursor = span[1];
        }
        appendRange(out, tokens, cursor, tokens.size());
        return out.toString();
    }

    /** The first `{`/`;` reached scanning forward from {@code requiresIdx}, or -1 if neither is
     *  found -- the trailing clause's own end (exclusive), per
     *  {@link #enforceRequiresClausePlacement}'s doc comment. */
    private int findRequiresClauseEnd(final List<Token> tokens, final int requiresIdx) {
        for (int i = requiresIdx + 1; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (isPunct(t, "{") || isPunct(t, ";")) {
                return i;
            }
        }
        return -1;
    }

    /** The index of the first significant token on the physical line containing {@code idx} --
     *  the line-collapsing analog of {@link #lineIndent}. */
    private int lineStartIndex(final List<Token> tokens, final int idx) {
        int newlineIdx = -1;
        for (int i = idx; i >= 0; i--) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                newlineIdx = i;
                break;
            }
        }
        final int firstSig = nextSignificantIndex(tokens, newlineIdx);
        return firstSig < 0 ? idx : firstSig;
    }

    /** Renders {@code tokens[fromInclusive, toInclusive]} verbatim except every whitespace/newline
     *  run collapses to exactly one space -- used to measure a would-be single-line rendering
     *  against {@link MiscRule#LINE_LENGTH_LIMIT} without actually committing to it. */
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

    /**
     * STYLE_C_CPP.md §10: a header file has a fixed 4-zone layout -- copyright block, header
     * guard (`#ifndef`/`#define` pair, or `#pragma once`), body, closing `#endif` (absent for the
     * `#pragma once` form) -- separated by exactly {@link #HEADER_ZONE_BLANK_LINES} blank lines.
     * Resolved via two {@code AskUserQuestion}s before writing this method (see STATE.md "§10
     * header zone spacing" and "§10 #endif trailing comment"): zone spacing is enforced strictly
     * in both directions (collapses excess blank lines, not just a floor, unlike §7's
     * "exactly one" precedent), and the closing `#endif`'s trailing `// GUARD_NAME` comment is
     * always normalized to the current guard name regardless of whether a rename actually fired,
     * inserting one even if the file's `#endif` was previously bare.
     *
     * <p>{@code detectHeaderZones} recognizes the shape conservatively: the first significant
     * token must be a {@code COMMENT_BLOCK}; the next, separated only by whitespace/newlines (a
     * comment in that gap aborts detection), must be a `#pragma once` {@code PREPROCESSOR} token
     * or an `#ifndef NAME` one immediately (whitespace/newlines only, no comment) followed by a
     * matching `#define NAME`; for the `#ifndef` form, the matching closing `#endif` is located by
     * depth-counting every `#if`/`#ifdef`/`#ifndef` (+1) and `#endif` (-1) {@code PREPROCESSOR}
     * token after the guard (mirroring {@code TokenizerCore}'s own `preprocessorDepth` tracking,
     * applied after tokenization since that depth isn't stored per-token), and nothing but
     * trailing whitespace/newline may follow it. Any deviation from this shape -- a missing
     * copyright block, a comment between zones, a mismatched `#ifndef`/`#define` name pair, an
     * unmatched `#endif`, trailing content after the closing `#endif`, or an empty body -- aborts
     * detection entirely and the file is returned byte-for-byte unchanged, the same conservative
     * "don't guess past an unrecognized shape" posture used throughout this codebase, since
     * STYLE_C_CPP.md gives no worked "before" example for any malformed header. The body's own
     * content (verbatim between the guard and the closing `#endif`/end-of-file) is untouched --
     * §10 only fixes inter-zone spacing and the guard, not body formatting.
     *
     * <p>Guard-name derivation (`deriveGuardName`) is a pure, mechanical transform of whatever
     * {@code filePath} string the caller supplies (uppercase, `.`/`/`/`\` &rarr; `_`) -- e.g.
     * `audio/Codec.h` &rarr; `AUDIO_CODEC_H`, matching STYLE_C_CPP.md §10's own worked example
     * once the caller has already stripped any project-root prefix (`src/`) the example's path
     * omits; this method has no project-layout knowledge to do that stripping itself, so it is
     * the caller's responsibility, deferred like other not-yet-wired `Main.java`/`Config.java`
     * concerns elsewhere in this file. `renameGuard` stands in for the not-yet-existent
     * `header-guard-rename` config key (default off): when false, or when the existing guard
     * already matches, the existing name is kept and only spacing/the `#endif` comment are
     * normalized. The actual "warn, don't rename" side effect that default implies has nowhere to
     * go yet (no `Config`/CLI output mechanism exists) and is deferred to whoever wires this
     * method into `Main.java`. `header-guard-style` (preserve/ifndef/pragma-once) needs no code at
     * all right now -- this method already only ever normalizes within whichever of the two forms
     * is already present and never converts between them, which is exactly the documented default
     * ("preserve existing") with nothing else implemented to switch to yet.
     */
    public String enforceHeaderFileStructure(final List<Token> tokens, final String filePath,
            final boolean renameGuard) {
        final HeaderZones z = detectHeaderZones(tokens);
        if (z == null) {
            return joinVerbatim(tokens);
        }

        final StringBuilder out = new StringBuilder();
        appendRange(out, tokens, 0, z.copyrightEnd + 1);
        appendBlankLineGap(out);

        if (z.isPragmaOnce) {
            appendRange(out, tokens, z.guardOpenIdx, z.guardOpenIdx + 1);
            appendBlankLineGap(out);
            appendRange(out, tokens, z.bodyStart, z.bodyEnd);
            return out.toString();
        }

        final String expectedGuard = deriveGuardName(filePath);
        final String effectiveGuard = renameGuard && !expectedGuard.equals(z.actualGuardName)
                ? expectedGuard : z.actualGuardName;

        out.append("#ifndef ").append(effectiveGuard);
        appendRange(out, tokens, z.guardOpenIdx + 1, z.guardDefineIdx);
        out.append("#define ").append(effectiveGuard);
        appendBlankLineGap(out);
        appendRange(out, tokens, z.bodyStart, z.bodyEnd);
        appendBlankLineGap(out);
        out.append("#endif // ").append(effectiveGuard);
        appendRange(out, tokens, z.endifIdx + 1, tokens.size());
        return out.toString();
    }

    /** Detected zone boundaries for {@link #enforceHeaderFileStructure}, or {@code null} from the
     *  detector if the token list doesn't match the expected shape. */
    private static final class HeaderZones {
        int copyrightEnd;
        boolean isPragmaOnce;
        int guardOpenIdx;
        int guardDefineIdx; // == guardOpenIdx for the pragma-once form
        String actualGuardName; // null for the pragma-once form
        int bodyStart;
        int bodyEnd; // exclusive
        int endifIdx; // -1 for the pragma-once form
    }

    private HeaderZones detectHeaderZones(final List<Token> tokens) {
        final int n = tokens.size();
        final int copyrightIdx = nextNonBlankIndex(tokens, 0);
        if (copyrightIdx < 0 || tokens.get(copyrightIdx).type != TokenType.COMMENT_BLOCK) {
            return null;
        }
        final HeaderZones z = new HeaderZones();
        z.copyrightEnd = copyrightIdx;

        final int guardIdx = nextNonBlankIndex(tokens, copyrightIdx + 1);
        if (guardIdx < 0 || tokens.get(guardIdx).type != TokenType.PREPROCESSOR) {
            return null;
        }
        final String guardText = tokens.get(guardIdx).text;

        if (isPragmaOnceDirective(guardText)) {
            z.isPragmaOnce = true;
            z.guardOpenIdx = guardIdx;
            z.guardDefineIdx = guardIdx;
            z.endifIdx = -1;
        } else if ("ifndef".equals(directiveWord(guardText))) {
            z.isPragmaOnce = false;
            z.guardOpenIdx = guardIdx;
            z.actualGuardName = extractDirectiveName(guardText, "ifndef");
            if (z.actualGuardName == null) {
                return null;
            }
            final int defineIdx = nextNonBlankIndex(tokens, guardIdx + 1);
            if (defineIdx < 0 || tokens.get(defineIdx).type != TokenType.PREPROCESSOR) {
                return null;
            }
            final String defineName = extractDirectiveName(tokens.get(defineIdx).text, "define");
            if (defineName == null || !defineName.equals(z.actualGuardName)) {
                return null;
            }
            z.guardDefineIdx = defineIdx;
        } else {
            return null;
        }

        final int afterGuard = (z.isPragmaOnce ? z.guardOpenIdx : z.guardDefineIdx) + 1;
        final int bodyStart = nextNonBlankIndex(tokens, afterGuard);
        if (bodyStart < 0) {
            return null;
        }
        z.bodyStart = bodyStart;

        if (z.isPragmaOnce) {
            z.bodyEnd = n;
            return z;
        }

        int depth = 1;
        int endifIdx = -1;
        for (int p = z.guardDefineIdx + 1; p < n; p++) {
            final Token t = tokens.get(p);
            if (t.type != TokenType.PREPROCESSOR) {
                continue;
            }
            final String word = directiveWord(t.text);
            if ("if".equals(word) || "ifdef".equals(word) || "ifndef".equals(word)) {
                depth++;
            } else if ("endif".equals(word)) {
                depth--;
                if (depth == 0) {
                    endifIdx = p;
                    break;
                }
            }
        }
        if (endifIdx < 0) {
            return null;
        }
        for (int p = endifIdx + 1; p < n; p++) {
            final TokenType ty = tokens.get(p).type;
            if (ty != TokenType.WHITESPACE && ty != TokenType.NEWLINE) {
                return null;
            }
        }

        final int lastBodySig = prevNonBlankIndex(tokens, endifIdx - 1);
        if (lastBodySig < z.bodyStart) {
            return null;
        }
        z.bodyEnd = lastBodySig + 1;
        z.endifIdx = endifIdx;
        return z;
    }

    /** Uppercase, with `.`/`/`/`\` replaced by `_` -- e.g. `audio/Codec.h` → `AUDIO_CODEC_H`. */
    private String deriveGuardName(final String filePath) {
        final StringBuilder sb = new StringBuilder(filePath.length());
        for (int i = 0; i < filePath.length(); i++) {
            final char c = filePath.charAt(i);
            if (c == '.' || c == '/' || c == '\\') {
                sb.append('_');
            } else {
                sb.append(Character.toUpperCase(c));
            }
        }
        return sb.toString();
    }

    private boolean isPragmaOnceDirective(final String text) {
        if (!"pragma".equals(directiveWord(text))) {
            return false;
        }
        final String afterHash = text.trim().substring(1).trim();
        final String afterPragma = afterHash.substring("pragma".length()).trim();
        return "once".equals(afterPragma);
    }

    /** The directive keyword of a {@code PREPROCESSOR} token's text (e.g. `"ifndef"`, `"endif"`,
     *  `"pragma"`), or `""` if the text doesn't start with `#`. */
    private String directiveWord(final String text) {
        final String t = text.trim();
        if (!t.startsWith("#")) {
            return "";
        }
        final String rest = t.substring(1);
        int p = 0;
        while (p < rest.length() && Character.isWhitespace(rest.charAt(p))) {
            p++;
        }
        final int start = p;
        while (p < rest.length() && (Character.isLetterOrDigit(rest.charAt(p)) || rest.charAt(p) == '_')) {
            p++;
        }
        return rest.substring(start, p);
    }

    /** The macro name of an `#ifndef NAME` / `#define NAME` directive's text, requiring nothing
     *  else (e.g. a trailing comment) follow it on the line -- {@code null} if the shape doesn't
     *  match exactly. */
    private String extractDirectiveName(final String text, final String expectedDirective) {
        final String t = text.trim();
        if (!t.startsWith("#")) {
            return null;
        }
        String rest = t.substring(1).trim();
        if (!rest.startsWith(expectedDirective)) {
            return null;
        }
        rest = rest.substring(expectedDirective.length()).trim();
        return isValidIdentifierName(rest) ? rest : null;
    }

    private boolean isValidIdentifierName(final String s) {
        if (s.isEmpty() || !(Character.isLetter(s.charAt(0)) || s.charAt(0) == '_')) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }
        return true;
    }

    private void appendBlankLineGap(final StringBuilder out) {
        for (int i = 0; i < HEADER_ZONE_BLANK_LINES + 1; i++) {
            out.append('\n');
        }
    }

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

    private int nextNonBlankIndex(final List<Token> tokens, final int from) {
        for (int i = from; i < tokens.size(); i++) {
            final TokenType ty = tokens.get(i).type;
            if (ty != TokenType.WHITESPACE && ty != TokenType.NEWLINE) {
                return i;
            }
        }
        return -1;
    }

    private int prevNonBlankIndex(final List<Token> tokens, final int from) {
        for (int i = from; i >= 0; i--) {
            final TokenType ty = tokens.get(i).type;
            if (ty != TokenType.WHITESPACE && ty != TokenType.NEWLINE) {
                return i;
            }
        }
        return -1;
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

    private int prevSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from - 1; i >= 0; i--) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private int nextSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from + 1; i < tokens.size(); i++) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /** True iff {@code t} is a C++ post-paren qualifier keyword that can appear between {@code )}
     *  and {@code {}} in a function definition (const, volatile, noexcept, override, final). */
    private boolean isDefinitionQualifier(final Token t) {
        if (t.type != TokenType.KEYWORD) {
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

    private boolean isGapToken(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    private boolean isPunct(final Token t, final String text) {
        return t != null && t.type == TokenType.PUNCT && text.equals(t.text);
    }
}
