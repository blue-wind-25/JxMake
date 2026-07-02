/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.DeclarationAlignmentRule;
import com.jxmake.formatter.rules.DeclarationAlignmentRule.Declaration;
import com.jxmake.formatter.rules.GetterSetterRule;
import com.jxmake.formatter.rules.GetterSetterRule.Member;
import com.jxmake.formatter.rules.MiscRule;
import com.jxmake.formatter.rules.MiscRule.Assignment;
import com.jxmake.formatter.rules.MiscRule.Signature;
import com.jxmake.formatter.tokenizer.TokenizerCore;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds scope/signature boundaries in a whole-file token stream and splices grouped/rendered
 * output from {@link DeclarationAlignmentRule}, {@link GetterSetterRule}, and {@link MiscRule}
 * back into the source text on their behalf -- those rule classes' grouping methods explicitly
 * document that boundary-finding and splice-back are the caller's job (see STATE.md's
 * "ScopePipeline.java" section).
 *
 * <p>One {@link #process(String)} call runs four passes -- STYLE.md §5 declarations, §6
 * assignments, §8 signatures, §14 getter/setter -- on every scope, outer-first, recursing into
 * every brace-block found at each level after that scope's own four passes complete (so STYLE.md
 * §5/§6 apply anywhere in code, recursively, not just class/struct bodies -- see STATE.md's
 * "STYLE.md §5/§6 scope -- anywhere in code, recursively").
 */
public class ScopePipeline {

    private final String language;
    private final String indentStyle;
    private final TokenizerCore tokenizer;
    private final DeclarationAlignmentRule declarationRule;
    private final GetterSetterRule getterSetterRule;
    private final MiscRule miscRule;

    public ScopePipeline(final String language, final String indentStyle) {
        this.language = language;
        this.indentStyle = indentStyle;
        this.tokenizer = new TokenizerCore(language);
        this.declarationRule = new DeclarationAlignmentRule(language);
        this.getterSetterRule = new GetterSetterRule(language);
        this.miscRule = new MiscRule(language);
    }

    // ── Top-level span splitting ─────────────────────────────────────────────────

    /** One top-level (depth-0) span of a scope's token list: either a `;`-terminated statement,
     *  or a `{ }`-block-terminated member, plus any same-line trailing comment. {@code end} is
     *  exclusive. {@code openBraceIdx}/{@code closeBraceIdx} are -1 for a statement span;
     *  otherwise they are this span's own top-level brace pair. */
    private static final class Span {
        final int start;
        final int end;
        final int openBraceIdx;
        final int closeBraceIdx;

        Span(final int start, final int end, final int openBraceIdx, final int closeBraceIdx) {
            this.start = start;
            this.end = end;
            this.openBraceIdx = openBraceIdx;
            this.closeBraceIdx = closeBraceIdx;
        }
    }

    /**
     * Splits a scope's full token range into contiguous top-level spans -- a third port of the
     * depth-aware splitting algorithm already duplicated in
     * {@code DeclarationAlignmentRule.splitStatements} and {@code MiscRule.splitAssignmentStatements},
     * but additionally recording the matching open-brace index for any span that closes via a
     * top-level `}` rather than a `;`. This one helper serves three jobs (see STATE.md's
     * "Child-scope / signature-candidate discovery"): anchor-token-to-span lookup for §5/§6
     * splice-back, finding every child scope to recurse into, and finding §8 signature candidates.
     */
    private List<Span> splitTopLevelSpans(final List<Token> tokens) {
        final List<Span> spans = new ArrayList<>();
        final int n = tokens.size();
        int depth = 0;
        int start = 0;
        int idx = 0;
        int braceIdx = -1; // this span's own top-level `{`, if any

        while (idx < n) {
            final Token t = tokens.get(idx);

            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                if (isPunct(t, "{") && depth == 0) {
                    braceIdx = idx;
                }
                depth++;
                idx++;
                continue;
            }
            if (isPunct(t, ")") || isPunct(t, "]")) {
                depth--;
                idx++;
                continue;
            }
            if (isPunct(t, "}")) {
                depth--;
                final int closeBraceIdx = idx;
                idx++;
                if (depth == 0) {
                    // Peek ahead: if the next significant token is `;`, this `}` is ambiguous --
                    // it could close a genuine named-construct body (`struct Foo { ... };`) or a
                    // brace-initializer nested in a declaration (`auto x = T{...};`, nested
                    // `{ {1,2}, {3,4} };`). Function/control-flow/lambda bodies and other genuine
                    // scopes are never followed by `;`, so in that (common) case this is
                    // unambiguously a real scope and no further check is needed. Only in the
                    // ambiguous `;`-followed case does `isScopeOpeningBrace` disambiguate via a
                    // backward scan for a construct keyword.
                    boolean nextIsSemi = false;
                    for (int peek = idx; peek < n; peek++) {
                        final Token nx = tokens.get(peek);
                        if (isGapToken(nx)) {
                            continue;
                        }
                        nextIsSemi = isPunct(nx, ";");
                        break;
                    }
                    if (!nextIsSemi || isScopeOpeningBrace(tokens, braceIdx, start)) {
                        idx = pullTrailingSameLine(tokens, idx);
                        spans.add(new Span(start, idx, braceIdx, closeBraceIdx));
                        start = idx;
                        braceIdx = -1;
                    }
                }
                continue;
            }
            if (depth == 0 && isPunct(t, ";")) {
                idx++;
                idx = pullTrailingSameLine(tokens, idx);
                spans.add(new Span(start, idx, -1, -1));
                start = idx;
                braceIdx = -1;
                continue;
            }
            if (depth == 0 && isOp(t, ":") && isAccessSpecifierLabel(tokens, start, idx)) {
                idx++;
                idx = pullTrailingSameLine(tokens, idx);
                spans.add(new Span(start, idx, -1, -1));
                start = idx;
                braceIdx = -1;
                continue;
            }
            idx++;
        }
        if (start < n) {
            spans.add(new Span(start, n, -1, -1));
        }
        return spans;
    }

    /** True iff {@code text} starts a named construct -- ported from
     *  {@code BlockStructureRule.isNamedConstructStartKeyword}. */
    private boolean isNamedConstructStartKeyword(final String text) {
        switch (text) {
            case "class": case "struct": case "enum": case "namespace":
            case "concept": case "interface": case "record":
                return true;
            default:
                return false;
        }
    }

    /**
     * True iff the `{` at {@code braceIdx} genuinely opens a named-construct body (class/struct/
     * union/enum/namespace/etc.) rather than a brace-initializer expression that merely happens to
     * be followed by `;` (e.g. `auto x = T{...};`, nested `{ {1,2}, {3,4} };`). Only called for
     * that ambiguous `;`-followed case (see caller) -- function/control-flow/lambda bodies are
     * never followed by `;` and so never reach here. Scans every significant token between
     * {@code spanStart} and {@code braceIdx} for a construct-introducing keyword (`class`,
     * `struct`, ...); found anywhere in that range (not just immediately before the brace) so an
     * intervening base-class list (`: public Base`), attribute-specifier (`alignas(16)`), or
     * template header doesn't defeat the match. A brace-initializer's span never contains such a
     * keyword (its lead tokens are a type/`auto` and `=`, or nothing at all), so this scan
     * distinguishes the two shapes cleanly.
     */
    private boolean isScopeOpeningBrace(final List<Token> tokens, final int braceIdx, final int spanStart) {
        for (int i = spanStart; i < braceIdx; i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.KEYWORD && isNamedConstructStartKeyword(t.text)) {
                return true;
            }
        }
        return false;
    }

    /** True iff {@code tokens[start, colonIdx)} contains exactly one significant token and it is
     *  `public`/`private`/`protected` -- a C++ access-specifier label, which (unlike a ternary's
     *  `:`) must end its own span here rather than being absorbed into the member that follows.
     *  Without this, "public:\n    Foo() {}" is swept into one span whose lead tokens become
     *  "public :", corrupting {@link #applySignaturePass}'s candidate-signature parse. Deliberately
     *  narrow: a bare ternary always has more than one significant lead token (e.g. `a ? b`), and a
     *  `case`/`default` switch label never matches this keyword set, so neither is affected. */
    private boolean isAccessSpecifierLabel(final List<Token> tokens, final int start, final int colonIdx) {
        int only = -1;
        for (int i = start; i < colonIdx; i++) {
            if (!isGapToken(tokens.get(i))) {
                if (only >= 0) {
                    return false;
                }
                only = i;
            }
        }
        if (only < 0) {
            return false;
        }
        final Token t = tokens.get(only);
        return t.type == TokenType.KEYWORD
                && ("public".equals(t.text) || "private".equals(t.text) || "protected".equals(t.text));
    }

    /** Extends {@code from} forward over a same-line trailing comment, so it stays attached to the
     *  span that just closed instead of becoming the next span's leading content -- same algorithm
     *  as {@code DeclarationAlignmentRule.pullTrailingSameLine}, returning just the new index since
     *  this caller tracks spans by index range rather than building a token sublist. */
    private int pullTrailingSameLine(final List<Token> tokens, final int from) {
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

    private Span findSpanContaining(final List<Span> spans, final int idx) {
        for (final Span s : spans) {
            if (s.start <= idx && idx < s.end) {
                return s;
            }
        }
        return null;
    }

    // ── Splice-back primitives ───────────────────────────────────────────────────

    /** One contiguous source-text replacement, by token-index range (end exclusive). */
    private static final class Replacement {
        final int start;
        final int end;
        final String text;

        Replacement(final int start, final int end, final String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }

    /** Reassembles {@code tokens}' source text, substituting each {@code replacements} range
     *  (assumed sorted by {@code start}, non-overlapping) and passing every other token through
     *  verbatim. */
    private String splice(final List<Token> tokens, final List<Replacement> replacements) {
        final StringBuilder out = new StringBuilder();
        final int n = tokens.size();
        int i = 0;
        int r = 0;
        while (i < n) {
            if (r < replacements.size() && replacements.get(r).start == i) {
                final Replacement rep = replacements.get(r);
                out.append(rep.text);
                i = rep.end;
                r++;
                continue;
            }
            out.append(tokens.get(i).text);
            i++;
        }
        return out.toString();
    }

    private String joinText(final List<Token> tokens, final int from, final int to) {
        final StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(tokens.get(i).text);
        }
        return sb.toString();
    }

    /** The indentation of the line a leading gap ends on -- the text after its last `\n`, or the
     *  whole gap if it contains none. */
    private String trailingIndent(final String gap) {
        final int nl = gap.lastIndexOf('\n');
        return nl >= 0 ? gap.substring(nl + 1) : gap;
    }

    /** Count of leading `' '` characters in {@code s} (0 if it doesn't start with one). */
    private int leadingSpaceCount(final String s) {
        int count = 0;
        while (count < s.length() && s.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    /** Removes up to {@code n} trailing `' '` characters from the end of {@code s}. */
    private String stripTrailingSpaces(final String s, final int n) {
        int end = s.length();
        int remaining = n;
        while (remaining > 0 && end > 0 && s.charAt(end - 1) == ' ') {
            end--;
            remaining--;
        }
        return s.substring(0, end);
    }

    /** Rounds `rawIndent` (spaces/tabs) up to the nearest multiple of {@link MiscRule#INDENT_WIDTH}.
     *  Returns `rawIndent` unchanged when it is already a valid indentation (zero, or a positive
     *  multiple of INDENT_WIDTH).  Only non-zero non-multiples (e.g. 2-space source) are touched. */
    private String normalizeIndent(final String rawIndent) {
        int width = 0;
        for (int i = 0; i < rawIndent.length(); i++) {
            final char c = rawIndent.charAt(i);
            if (c == '\t') {
                width = ((width / MiscRule.INDENT_WIDTH) + 1) * MiscRule.INDENT_WIDTH;
            } else {
                width++;
            }
        }
        // Zero-width is valid (global/top-level scope, column 0).  Multiples of INDENT_WIDTH
        // are valid.  Only round up a non-zero non-multiple (malformed indentation in source).
        if (width == 0 || width % MiscRule.INDENT_WIDTH == 0) {
            return rawIndent;
        }
        final int normalized = ((width + MiscRule.INDENT_WIDTH - 1) / MiscRule.INDENT_WIDTH)
                * MiscRule.INDENT_WIDTH;
        final StringBuilder sb = new StringBuilder(normalized);
        for (int i = 0; i < normalized; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** Returns a `leadingGap` that ends with `normalizedIndent` on its final line.  Only acts
     *  when `leadingGap` already has a newline (multi-line indented content); if `leadingGap`
     *  has no newline the content is inline and the gap is left unchanged -- callers that need
     *  to expand a one-liner named-scope body pre-process it before calling processScope. */
    private String normalizeLeadingGap(final String leadingGap, final String rawIndent,
            final String normalizedIndent) {
        if (rawIndent.equals(normalizedIndent)) {
            return leadingGap;
        }
        final int nl = leadingGap.lastIndexOf('\n');
        if (nl < 0) {
            return leadingGap;
        }
        return leadingGap.substring(0, nl + 1) + normalizedIndent;
    }

    /** Returns the leading whitespace of the line that contains the span's first significant
     *  token -- i.e. the indentation of the named construct whose one-liner body we are about
     *  to pre-expand.  Scans forward to the first non-gap token, then backward to the preceding
     *  newline; the text between that newline and the token is the indentation. */
    private String findParentIndent(final List<Token> tokens, final Span span) {
        for (int i = span.start; i < span.openBraceIdx; i++) {
            if (!isGapToken(tokens.get(i))) {
                for (int j = i - 1; j >= span.start; j--) {
                    if (tokens.get(j).type == TokenType.NEWLINE) {
                        return joinText(tokens, j + 1, i);
                    }
                }
                return "";
            }
        }
        return "";
    }

    private Map<Token, Integer> buildIndexMap(final List<Token> tokens) {
        final Map<Token, Integer> indexOf = new IdentityHashMap<>();
        for (int i = 0; i < tokens.size(); i++) {
            indexOf.put(tokens.get(i), i);
        }
        return indexOf;
    }

    // ── §5 declarations pass ─────────────────────────────────────────────────────

    /**
     * Runs {@code DeclarationAlignmentRule.groupDeclarations} on {@code tokens} and splices each
     * group's rendered grid back in as one contiguous block. {@code Declaration} carries no
     * original-position fields, so each group's first/last member is anchored back to a token
     * index via identity lookup (see STATE.md's "Splice-back mechanics"), and that index's
     * enclosing {@link Span} supplies the actual replacement range -- the span already includes
     * the statement's trailing `;`/comment via the identical depth-aware algorithm.
     */
    private String applyDeclarationsPass(final List<Token> tokens) {
        final List<List<Declaration>> groups = declarationRule.groupDeclarations(tokens);
        final List<Span> spans = splitTopLevelSpans(tokens);
        final Map<Token, Integer> indexOf = buildIndexMap(tokens);
        final List<Replacement> replacements = new ArrayList<>();

        for (final List<Declaration> group : groups) {
            final Declaration first = group.get(0);
            final Declaration last = group.get(group.size() - 1);
            final Token firstAnchor = !first.templatePrefix.isEmpty() ? first.templatePrefix.get(0)
                    : first.modifiers.isEmpty() ? first.typeTokens.get(0) : first.modifiers.get(0);
            final int firstIdx = indexOf.get(firstAnchor);
            final int lastIdx = indexOf.get(last.name);
            final Span firstSpan = findSpanContaining(spans, firstIdx);
            final Span lastSpan = findSpanContaining(spans, lastIdx);

            final List<String> lines = declarationRule.render(group);
            // `lines.get(0)` may already start with its own column-alignment padding (e.g. blank
            // space matching a sibling's `static` width, STYLE.md §5) -- on a re-format of
            // already-formatted source, that same padding is literally present in the raw text
            // right before `firstIdx` too, indistinguishable by character alone from real code
            // indentation. Stripping exactly that many trailing spaces off the raw gap before
            // treating the remainder as "indent" keeps this pass idempotent; on a first-time
            // format (no pre-existing padding in the source) there's nothing to strip and this
            // is a no-op.
            final String rawLeadingGapFull = joinText(tokens, firstSpan.start, firstIdx);
            final int freshPad = leadingSpaceCount(lines.get(0));
            // On a genuine re-format, the raw gap's trailing-space count is trueIndent + freshPad
            // (the previous pass wrote both), so it is always >= freshPad. On a first-time format
            // the gap has only trueIndent spaces with no self-padding; if that count is already
            // less than freshPad (e.g. a struct member's real 4-space indent vs. a 6-wide
            // "const "-column pad from a sibling), stripping would eat into real indentation, so
            // skip the strip entirely in that case.
            final int trailingSpaces = leadingSpaceCount(new StringBuilder(rawLeadingGapFull).reverse().toString());
            final String rawLeadingGap = trailingSpaces >= freshPad
                    ? stripTrailingSpaces(rawLeadingGapFull, freshPad) : rawLeadingGapFull;
            final String rawIndent = trailingIndent(rawLeadingGap);
            final String indent = normalizeIndent(rawIndent);
            final String leadingGap = normalizeLeadingGap(rawLeadingGap, rawIndent, indent);
            final String text = leadingGap + String.join("\n" + indent, lines);
            int lastTermEnd = lastSpan.end;
            while (lastTermEnd > lastSpan.start && isGapToken(tokens.get(lastTermEnd - 1))) {
                lastTermEnd--;
            }
            replacements.add(new Replacement(firstSpan.start, lastTermEnd, text));
        }
        return splice(tokens, replacements);
    }

    // ── §6 assignments pass ──────────────────────────────────────────────────────

    /** Identical shape to {@link #applyDeclarationsPass} using {@code MiscRule.groupAssignments}/
     *  {@code render} -- an {@code Assignment}'s {@code target} is always the statement's own
     *  first significant token (per {@code MiscRule.parseAssignment}), so it doubles as both the
     *  start- and end-anchor. */
    private String applyAssignmentsPass(final List<Token> tokens) {
        final List<List<Assignment>> groups = miscRule.groupAssignments(tokens);
        final List<Span> spans = splitTopLevelSpans(tokens);
        final Map<Token, Integer> indexOf = buildIndexMap(tokens);
        final List<Replacement> replacements = new ArrayList<>();

        for (final List<Assignment> group : groups) {
            final Assignment first = group.get(0);
            final Assignment last = group.get(group.size() - 1);
            final int firstIdx = indexOf.get(first.target);
            final int lastIdx = indexOf.get(last.target);
            final Span firstSpan = findSpanContaining(spans, firstIdx);
            final Span lastSpan = findSpanContaining(spans, lastIdx);

            final String rawLeadingGap = joinText(tokens, firstSpan.start, firstIdx);
            final String rawIndent = trailingIndent(rawLeadingGap);
            final String indent = normalizeIndent(rawIndent);
            final String leadingGap = normalizeLeadingGap(rawLeadingGap, rawIndent, indent);
            final List<String> lines = miscRule.render(group);
            final String text = leadingGap + String.join("\n" + indent, lines);
            int lastTermEnd = lastSpan.end;
            while (lastTermEnd > lastSpan.start && isGapToken(tokens.get(lastTermEnd - 1))) {
                lastTermEnd--;
            }
            replacements.add(new Replacement(firstSpan.start, lastTermEnd, text));
        }
        return splice(tokens, replacements);
    }

    // ── §8 signatures pass ───────────────────────────────────────────────────────

    /**
     * Scans {@code tokens} for signature candidates -- a top-level `{`-terminated span whose
     * brace is directly preceded (skipping gaps) by a `)` whose matching `(` is itself preceded by
     * an IDENTIFIER not preceded by `new` (ported from {@code JavaSpecificRule.isCandidateMethodName}/
     * {@code CppSpecificRule.isCandidateSignatureName}), excluding a Java enum constant body
     * ({@code isEnumConstantBody}) -- and splices {@code MiscRule.render(Signature, depth,
     * indentStyle)}'s output over just the `[leadStart, closeParenIdx]` range, leaving the body
     * untouched.
     */
    private String applySignaturePass(final List<Token> tokens, final int depth) {
        final List<Span> spans = splitTopLevelSpans(tokens);
        final List<Replacement> replacements = new ArrayList<>();

        for (final Span span : spans) {
            if (span.openBraceIdx < 0) {
                continue;
            }
            int closeParenIdx = prevSignificantIndex(tokens, span.openBraceIdx);
            if (closeParenIdx < 0) {
                continue;
            }
            // For Java: handle a `throws` clause between `)` and `{`.
            int throwsEndIdx = -1;
            if (!isPunct(tokens.get(closeParenIdx), ")")) {
                if (!"java".equals(language)) {
                    continue;
                }
                final int realCloseParen = findCloseParenBeforeThrows(tokens, closeParenIdx);
                if (realCloseParen < 0) {
                    continue;
                }
                throwsEndIdx = closeParenIdx;
                closeParenIdx = realCloseParen;
            }
            final int openParenIdx = matchParenBackward(tokens, closeParenIdx);
            if (openParenIdx < 0 || !isCandidateSignatureName(tokens, openParenIdx)) {
                continue;
            }
            if ("java".equals(language) && isEnumConstantBody(tokens, span.openBraceIdx)) {
                continue;
            }

            final int leadStart = nextSignificantIndex(tokens, span.start - 1);
            if (leadStart < 0) {
                continue;
            }
            // For Java: skip past any leading @Annotation tokens so they stay verbatim in
            // leadingGap (on their own line) rather than being absorbed into the signature's
            // lead-token list and collapsed onto the method declaration line.
            // For C/C++: start the signature from the same line as the function name --
            // a `template<...>` header that precedes the function on its own line must
            // stay verbatim in leadingGap rather than being pulled into parseSignature,
            // where it would be collapsed with the return type onto one line.
            final int sigLeadStart;
            if ("java".equals(language)) {
                sigLeadStart = skipAnnotations(tokens, leadStart, closeParenIdx);
            } else {
                final int nameIdx = prevSignificantIndex(tokens, openParenIdx);
                if (nameIdx >= 0) {
                    // Find the last NEWLINE within the span (before nameIdx).
                    int newlineIdx = -1;
                    for (int j = nameIdx - 1; j >= leadStart; j--) {
                        if (tokens.get(j).type == TokenType.NEWLINE) { newlineIdx = j; break; }
                    }
                    if (newlineIdx < 0) {
                        // No NEWLINE between leadStart and nameIdx -- same physical line.
                        sigLeadStart = leadStart;
                    } else {
                        // Function name is on a later line; start the signature there.
                        final int lineFirst = nextSignificantIndex(tokens, newlineIdx);
                        sigLeadStart = lineFirst >= 0 ? lineFirst : leadStart;
                    }
                } else {
                    sigLeadStart = leadStart;
                }
            }
            final Signature sig = miscRule.parseSignature(
                    tokens.subList(sigLeadStart, closeParenIdx + 1));
            if (sig == null) {
                continue;
            }

            final List<String> lines = miscRule.render(sig, depth, indentStyle);
            final String leadingGap = joinText(tokens, span.start, sigLeadStart);
            final StringBuilder text = new StringBuilder(leadingGap).append(lines.get(0));
            for (int i = 1; i < lines.size(); i++) {
                text.append('\n').append(lines.get(i));
            }
            if (throwsEndIdx >= 0) {
                // Append normalized throws clause: scan significant tokens from `throws` keyword
                // through the last exception class name, joining with single spaces.
                int ti = nextSignificantIndex(tokens, closeParenIdx);
                while (ti >= 0 && ti <= throwsEndIdx) {
                    text.append(' ').append(tokens.get(ti).text);
                    ti = nextSignificantIndex(tokens, ti);
                }
                replacements.add(new Replacement(span.start, throwsEndIdx + 1, text.toString()));
            } else {
                replacements.add(new Replacement(span.start, closeParenIdx + 1, text.toString()));
            }
        }
        return splice(tokens, replacements);
    }

    /**
     * Scans forward past zero or more {@code @Identifier} / {@code @Identifier(args)}
     * annotation tokens starting at {@code from}, returning the index of the first
     * non-annotation significant token. Returns {@code from} if nothing is skipped or
     * if skipping would reach or exceed {@code limit}.
     */
    private int skipAnnotations(final List<Token> tokens, final int from, final int limit) {
        int i = from;
        while (i >= 0 && i < limit && isOp(tokens.get(i), "@")) {
            final int nameIdx = nextSignificantIndex(tokens, i);
            if (nameIdx < 0 || nameIdx >= limit
                    || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
                break;
            }
            final int afterName = nextSignificantIndex(tokens, nameIdx);
            if (afterName >= 0 && afterName < limit && isPunct(tokens.get(afterName), "(")) {
                final int closeParen = matchParenForward(tokens, afterName);
                if (closeParen < 0 || closeParen >= limit) {
                    break;
                }
                i = nextSignificantIndex(tokens, closeParen);
            } else {
                i = afterName;
            }
            if (i < 0 || i >= limit) {
                break;
            }
        }
        return (i < 0 || i >= limit) ? from : i;
    }

    /** True iff the token immediately before {@code openIdx} is an IDENTIFIER not itself preceded
     *  by `new` -- the candidate-signature-name signal, ported from
     *  {@code JavaSpecificRule.isCandidateMethodName}/{@code CppSpecificRule.isCandidateSignatureName}. */
    private boolean isCandidateSignatureName(final List<Token> tokens, final int openIdx) {
        final int nameIdx = prevSignificantIndex(tokens, openIdx);
        if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
            return false;
        }
        final int beforeName = prevSignificantIndex(tokens, nameIdx);
        return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                || !"new".equals(tokens.get(beforeName).text);
    }

    /** True iff the `{` at {@code braceIdx} is a Java enum constant's anonymous constant-body --
     *  detected via its matching `}` being immediately followed by `,` or `;` -- ported from
     *  {@code JavaSpecificRule.isEnumConstantBody}. */
    private boolean isEnumConstantBody(final List<Token> tokens, final int braceIdx) {
        final int closeBraceIdx = matchBraceForward(tokens, braceIdx);
        if (closeBraceIdx < 0) {
            return false;
        }
        final int next = nextSignificantIndex(tokens, closeBraceIdx);
        return next >= 0 && (isPunct(tokens.get(next), ",") || isPunct(tokens.get(next), ";"));
    }

    /**
     * For Java `throws` clauses: given the token at {@code fromIdx} (the significant token
     * immediately before `{`) that is NOT `)`, checks if it is the last exception class name
     * in a {@code throws} clause. Scans backward through comma-separated IDENTIFIERs to the
     * {@code throws} keyword, then expects `)` immediately before it.
     * Returns the index of the `)` of the method parameter list, or -1 if no such pattern found.
     */
    private int findCloseParenBeforeThrows(final List<Token> tokens, final int fromIdx) {
        int i = fromIdx;
        if (i < 0 || tokens.get(i).type != TokenType.IDENTIFIER) {
            return -1;
        }
        while (i >= 0) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.IDENTIFIER) {
                i = prevSignificantIndex(tokens, i - 1);
            } else if (isPunct(t, ",")) {
                i = prevSignificantIndex(tokens, i - 1);
            } else {
                break;
            }
        }
        if (i < 0 || tokens.get(i).type != TokenType.KEYWORD || !"throws".equals(tokens.get(i).text)) {
            return -1;
        }
        final int closeParen = prevSignificantIndex(tokens, i - 1);
        return (closeParen >= 0 && isPunct(tokens.get(closeParen), ")")) ? closeParen : -1;
    }

    // ── §14 getter/setter pass ───────────────────────────────────────────────────

    /**
     * Runs {@code GetterSetterRule.groupOneLiners} then {@code excludeOutliers} on {@code tokens},
     * splicing each remaining member's own rendered row over its own {@code memberFrom}/
     * {@code memberTo} range individually -- not as one contiguous block, since an excluded
     * outlier in the middle of a run leaves the surviving members textually non-adjacent (see
     * STATE.md's checklist note on this). A group that drops below 2 members after exclusion is
     * skipped entirely, per {@code excludeOutliers}'s own contract.
     */
    private String applyGetterSetterPass(final List<Token> tokens) {
        final List<List<Member>> groups = getterSetterRule.groupOneLiners(tokens);
        final Map<Token, Integer> indexOf = buildIndexMap(tokens);
        final List<Replacement> replacements = new ArrayList<>();

        for (final List<Member> group : groups) {
            final List<Member> filtered = getterSetterRule.excludeOutliers(tokens, group);
            if (filtered.isEmpty()) {
                continue;
            }
            final List<String> lines = getterSetterRule.render(tokens, filtered);
            for (int i = 0; i < filtered.size(); i++) {
                final Member m = filtered.get(i);
                final int sigIdx = m.modifiers.isEmpty() ? m.returnTypeFrom : indexOf.get(m.modifiers.get(0));
                final String leadingGap = joinText(tokens, m.memberFrom, sigIdx);
                replacements.add(new Replacement(m.memberFrom, m.memberTo, leadingGap + lines.get(i)));
            }
        }
        return splice(tokens, replacements);
    }

    // ── Recursion driver ─────────────────────────────────────────────────────────

    /**
     * Runs the four passes above, in fixed order (re-tokenizing between each, same
     * "chained via re-tokenizing between passes" precedent used throughout this codebase), then
     * recurses outer-first into every child block found in the final token list, splicing each
     * child's processed text back in place.
     */
    private String processScope(final List<Token> tokens, final int depth) {
        List<Token> current = tokens;
        current = tokenizer.tokenize(applyDeclarationsPass(current));
        current = tokenizer.tokenize(applyAssignmentsPass(current));
        current = tokenizer.tokenize(applySignaturePass(current, depth));
        current = tokenizer.tokenize(applyGetterSetterPass(current));

        final List<Span> spans = splitTopLevelSpans(current);
        final List<Replacement> replacements = new ArrayList<>();
        for (final Span span : spans) {
            if (span.openBraceIdx < 0) {
                continue;
            }
            String childSource = joinText(current, span.openBraceIdx + 1, span.closeBraceIdx);
            final boolean isNamedScope = current.get(span.openBraceIdx).name != null;
            // Pre-expand named-construct one-liner bodies (`struct Foo { int a; int b; };`)
            // into multi-line form so that applyDeclarationsPass/applyAssignmentsPass in the
            // child scope see newline-separated source and produce correctly-indented output.
            // Non-named scopes (function/loop/lambda bodies) are left alone -- their one-liner
            // bodies must stay inline for getter/setter grouping and Allman detection.
            if (isNamedScope && !childSource.contains("\n")) {
                final String trimmed = childSource.trim();
                if (!trimmed.isEmpty()) {
                    final String parentIndent = findParentIndent(current, span);
                    childSource = "\n" + parentIndent + "    " + trimmed + "\n" + parentIndent;
                }
            }
            final String childResult;
            if (!isNamedScope && !childSource.contains("\n")) {
                // One-liner non-named body (method/constructor/loop `{ ... }` kept on its
                // original single line) -- recursing would run it through the §5/§6
                // declaration/assignment grouping passes, which assume a real multi-statement
                // block and split+column-align each statement onto its own line. That's wrong
                // here: a single-line body must stay exactly as written so later passes
                // (GetterSetterRule one-liner grouping, Allman-brace conversion) can still
                // recognize and handle it as a one-liner.
                childResult = childSource;
            } else {
                childResult = processScope(tokenizer.tokenize(childSource), depth + 1);
            }
            replacements.add(new Replacement(span.openBraceIdx + 1, span.closeBraceIdx, childResult));
        }
        return splice(current, replacements);
    }

    /** Public entry point: tokenizes {@code source} and runs the recursive scope pipeline,
     *  starting at depth 0. The one method {@code Main.java} calls once per file. */
    public String process(final String source) {
        return processScope(tokenizer.tokenize(source), 0);
    }

    // ── Token-scanning helpers ───────────────────────────────────────────────────
    // Ported low-level helpers, duplicated per this codebase's one-owner-per-class precedent.
    // prevSignificantIndex/nextSignificantIndex use the exclusive-of-`from` convention (matching
    // JavaSpecificRule/CppSpecificRule, since isCandidateSignatureName/isEnumConstantBody above
    // are ported verbatim from there), not MiscRule's inclusive-of-`from` convention.

    private boolean isPunct(final Token t, final String text) {
        return t != null && t.type == TokenType.PUNCT && text.equals(t.text);
    }

    private boolean isOp(final Token t, final String text) {
        return t != null && t.type == TokenType.OP && text.equals(t.text);
    }

    private boolean isGapToken(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
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
}
