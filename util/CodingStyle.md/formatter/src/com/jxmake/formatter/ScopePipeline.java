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

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

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

    private final Lang lang;
    private final String indentStyle;
    private final TokenizerCore tokenizer;
    private final DeclarationAlignmentRule declarationRule;
    private final GetterSetterRule getterSetterRule;
    private final MiscRule miscRule;
    private final boolean formatOff;

    public ScopePipeline(final Lang lang, final String indentStyle,
            final boolean normalizeCommentStartCase, final boolean normalizeCommentEndPeriod) {
        this(lang, indentStyle, normalizeCommentStartCase, normalizeCommentEndPeriod, false);
    }

    public ScopePipeline(final Lang lang, final String indentStyle,
            final boolean normalizeCommentStartCase, final boolean normalizeCommentEndPeriod,
            final boolean formatOff) {
        this(lang, indentStyle, normalizeCommentStartCase, normalizeCommentEndPeriod, formatOff,
                MiscRule.DEFAULT_INDENT_WIDTH, MiscRule.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public ScopePipeline(final Lang lang, final String indentStyle,
            final boolean normalizeCommentStartCase, final boolean normalizeCommentEndPeriod,
            final boolean formatOff, final int indentWidth, final int lineLengthLimit) {
        this.lang = lang;
        this.indentStyle = indentStyle;
        this.tokenizer = new TokenizerCore(lang);
        this.declarationRule = new DeclarationAlignmentRule(lang, lineLengthLimit);
        this.getterSetterRule = new GetterSetterRule(lang, indentWidth, lineLengthLimit);
        this.miscRule = new MiscRule(lang, normalizeCommentStartCase, normalizeCommentEndPeriod,
                indentWidth, lineLengthLimit);
        this.formatOff = formatOff;
    }

    /** Wraps {@link TokenizerCore#tokenize} and stamps frozen-span state (RDD_KEY_90 §A) on every
     *  re-tokenize, same as {@code Formatter}'s tokenizer wrapper. {@code startFrozen} must be the
     *  frozen state observed at {@code s}'s own starting boundary -- a substring extracted mid-file
     *  (a recursed-into child scope) may not textually contain the JXM_CFMT_DIS marker that caused
     *  that state, so it cannot be re-derived by scanning {@code s} alone. */
    private List<Token> tokenize(final String s, final boolean startFrozen) {
        final List<Token> tokens = tokenizer.tokenize(s);
        TokenizerCore.markFrozenSpans(tokens, startFrozen);
        return tokens;
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

    /** Rounds `rawIndent` (spaces/tabs) up to the nearest multiple of {@link MiscRule#indentWidth}.
     *  Returns `rawIndent` unchanged when it is already a valid indentation (zero, or a positive
     *  multiple of indentWidth).  Only non-zero non-multiples (e.g. 2-space source) are touched. */
    private String normalizeIndent(final String rawIndent) {
        final int indentWidth = miscRule.indentWidth;
        int width = 0;
        for (int i = 0; i < rawIndent.length(); i++) {
            final char c = rawIndent.charAt(i);
            if (c == '\t') {
                width = ((width / indentWidth) + 1) * indentWidth;
            } else {
                width++;
            }
        }
        // Zero-width is valid (global/top-level scope, column 0).  Multiples of indentWidth
        // are valid.  Only round up a non-zero non-multiple (malformed indentation in source).
        if (width == 0 || width % indentWidth == 0) {
            return rawIndent;
        }
        final int normalized = ((width + indentWidth - 1) / indentWidth)
                * indentWidth;
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

    /** True if the `{` at {@code braceIdx} opens a `namespace NAME { ... }` (or nested
     *  `namespace a::b { ... }`) body -- found by walking back over the optional
     *  `IDENTIFIER (:: IDENTIFIER)*` name chain to check for an immediately preceding
     *  `namespace` keyword. {@code Token.name} alone can't distinguish this from any other
     *  named construct (it holds just the bare name, e.g. `"audio"`, not the construct
     *  keyword). */
    private boolean isNamespaceScope(final List<Token> tokens, final int braceIdx) {
        if (tokens.get(braceIdx).name == null) {
            return false;
        }
        int p = prevSignificantIndex(tokens, braceIdx - 1);
        if (p < 0 || tokens.get(p).type != TokenType.IDENTIFIER) {
            return false;
        }
        while (true) {
            final int q = prevSignificantIndex(tokens, p - 1);
            if (q < 0 || !isOp(tokens.get(q), "::")) {
                break;
            }
            final int r = prevSignificantIndex(tokens, q - 1);
            if (r < 0 || tokens.get(r).type != TokenType.IDENTIFIER) {
                break;
            }
            p = r;
        }
        final int kwIdx = prevSignificantIndex(tokens, p - 1);
        return kwIdx >= 0 && tokens.get(kwIdx).type == TokenType.KEYWORD
                && "namespace".equals(tokens.get(kwIdx).text);
    }

    /** Returns the leading whitespace of the line that contains the span's first significant
     *  token -- i.e. the indentation of the named construct whose one-liner body we are about
     *  to pre-expand.  Scans forward to the first non-gap token, then backward to the preceding
     *  newline; the text between that newline and the token is the indentation. */
    /** True iff {@code colonIdx} is the terminating `:` of a `case EXPR :` / `default :` switch
     *  label, found by scanning backward over the label's constant-expression tokens (bare
     *  identifiers/numbers/`::`-qualified names) until the `case`/`default` keyword itself is
     *  reached, or a `;`/`}`/`{`/`:` is hit first (in which case this is some other kind of
     *  colon -- inheritance list, ternary, constructor initializer list -- and not a label). */
    private boolean isCaseOrDefaultLabelColon(final List<Token> tokens, final int colonIdx) {
        for (int i = colonIdx - 1; i >= 0; i--) {
            final Token t = tokens.get(i);
            if (isGapToken(t)) {
                continue;
            }
            if (t.type == TokenType.KEYWORD && ("case".equals(t.text) || "default".equals(t.text))) {
                return true;
            }
            if (isPunct(t, ";") || isPunct(t, "}") || isPunct(t, "{") || isOp(t, ":")) {
                return false;
            }
        }
        return false;
    }

    private String findParentIndent(final List<Token> tokens, final Span span, final int depth) {
        // Find where the construct actually governing openBraceIdx begins -- NOT simply the
        // first significant token in the whole span, since a span can carry more than one
        // leading statement/label ahead of the one that opens this brace (e.g. `case 1:` is not
        // its own span -- see splitTopLevelSpans -- so a span for the `if` immediately following
        // a `case` label has that label's tokens sitting before `if` within the SAME span; using
        // the span's own first token as anchor would incorrectly land on `case` and later return
        // that label's line indent instead of the `if`'s own). Scan backward from openBraceIdx
        // instead, skipping over balanced parens/brackets, stopping at the nearest top-level
        // `;`, `}`, or `case`/`default` label colon -- whichever comes first identifies the true
        // start of the statement that owns this brace.
        int stmtStart = span.start;
        int parenDepth = 0;
        for (int i = span.openBraceIdx - 1; i >= span.start; i--) {
            final Token tok = tokens.get(i);
            if (isPunct(tok, ")") || isPunct(tok, "]")) {
                parenDepth++;
                continue;
            }
            if (isPunct(tok, "(") || isPunct(tok, "[")) {
                parenDepth--;
                continue;
            }
            if (parenDepth > 0) {
                continue;
            }
            if (isPunct(tok, ";") || isPunct(tok, "}")) {
                stmtStart = i + 1;
                break;
            }
            if (isOp(tok, ":") && isCaseOrDefaultLabelColon(tokens, i)) {
                stmtStart = i + 1;
                break;
            }
        }
        // Anchor on the first significant token at or after stmtStart: normally the construct's
        // own keyword/name (`class Foo {`), but for a bare compound statement (`{ ... }` with no
        // preceding keyword) the `{` itself is the first significant token, so openBraceIdx
        // doubles as the anchor in that case.
        int anchor = span.openBraceIdx;
        for (int i = stmtStart; i < span.openBraceIdx; i++) {
            // A preprocessor directive belonging to this span's own leading gap (e.g. a
            // preceding sibling span ends right where a `#endif` starts) always sits at column
            // 0 and is never part of the actual construct -- skip it like a gap token so the
            // real first token (e.g. `public`) becomes the anchor instead.
            if (!isGapToken(tokens.get(i)) && tokens.get(i).type != TokenType.PREPROCESSOR) {
                anchor = i;
                break;
            }
        }
        // Search backward across the whole token list, not just from span.start: a preceding
        // sibling span (e.g. one ending in a preprocessor directive) can leave this span's own
        // start coinciding with its first significant token, with the actual leading
        // newline+indent having been consumed as part of the PREVIOUS span instead -- bounding
        // the search at span.start would then find nothing and silently fall back to "".
        for (int j = anchor - 1; j >= 0; j--) {
            final Token tok = tokens.get(j);
            if (tok.type == TokenType.NEWLINE) {
                return joinText(tokens, j + 1, anchor);
            }
            // The anchor is not the first significant token on its own physical line (e.g. a
            // still-K&R `} else {`, before Formatter's later placeElseOnOwnLine pass converts it
            // to Allman) -- there's no well-defined frame indent to derive here yet; scanning
            // further back would cross into a PRECEDING statement/block's own text (as happened
            // here, once returning a string that included a stray `}`). Signal "unknown" so the
            // caller leaves the closing-brace gap untouched instead.
            if (!isGapToken(tok)) {
                return null;
            }
        }
        // Ran off the start of `tokens` without ever finding a NEWLINE. At true top level
        // (depth 0, called with the whole file's token list, OR a namespace body -- namespaces
        // never consume a depth level, see `isNamespaceScope` in `processScope` -- whose own
        // opening line is itself at column 0) that legitimately means "column 0, no indent".
        // But `tokens` here can also be a recursively-extracted child fragment (see
        // `processScope`'s `joinText(current, span.openBraceIdx + 1, span.closeBraceIdx)`) whose
        // very first line is a NESTED named construct that shared its opening line with the
        // parent's own `{` (e.g. `struct Foo { enum Bar {` on one source line) -- that line's
        // real leading indent was never captured in this fragment at all, since the fragment
        // starts strictly after the parent's `{`. Trusting "" there would force this construct's
        // own closing `}` back to column 0, corrupting otherwise well-formed nesting (see
        // STATE.md's `frozen`/`CaseSensitive`/`enum Choice` bug).
        // `depth` (already correctly tracked by `processScope`'s own recursion, independent of
        // any text-based derivation) resolves the ambiguity: depth 0 keeps the legitimate "" --
        // this is the only case ever reached with a truly-zero nesting level (whether genuine
        // file root or a non-indenting namespace body, both correctly want column 0 here -- do
        // NOT special-case them apart). depth > 0 means this fallback fired one level further in
        // than `depth` itself accounts for (the same-source-line scenario above always loses
        // exactly one level of newline context relative to `depth`'s own bookkeeping), so it
        // must synthesize depth + 1 indent units, not depth, to match the sibling construct's
        // already-correctly-placed opening line (see `BlockStructureRule`'s own placement).
        if (depth == 0) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (depth + 1) * miscRule.indentWidth; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** Strips trailing spaces/tabs/newlines/carriage-returns from {@code s} -- used to discard a
     *  scope's original, unnormalized gap before its closing `}` so a fresh {@code "\n" + indent}
     *  can be appended in its place. */
    private String trimTrailingWhitespace(final String s) {
        int end = s.length();
        while (end > 0) {
            final char c = s.charAt(end - 1);
            if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                break;
            }
            end--;
        }
        return s.substring(0, end);
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
            // Use firstIdx (the declaration's own first real token), not firstSpan.start: the
            // span's leading gap can contain a previous statement's trailing JXM_CFMT_ENA/DIS
            // marker (always itself stamped frozen -- see markFrozenSpans), which must not cause
            // this unrelated, unfrozen declaration to be mistaken for frozen content.
            if (anyFrozen(tokens, firstIdx, lastSpan.end)) {
                continue;
            }

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
            while (lastTermEnd > lastSpan.start && isWhitespaceOrNewline(tokens.get(lastTermEnd - 1))) {
                lastTermEnd--;
            }
            replacements.add(new Replacement(firstSpan.start, lastTermEnd, text));
        }
        return splice(tokens, replacements);
    }

    // ── Oversized aggregate-init closing-brace pass ────────────────────────────────

    /**
     * A brace-initializer (`name = { ... };`) too large or otherwise ineligible for
     * {@code DeclarationAlignmentRule}'s one-line collapse (e.g. a byte/word table spanning many
     * source lines, left untouched by the width guard in `parseDeclaration`) still has its
     * closing `}` dangling at the end of the last data line, e.g. {@code ... 0xAC, 0x62};}. Move
     * that `}` onto its own line at the declaration's own indent -- matching this codebase's
     * general convention of a closing brace on its own line (STYLE.md's Allman-style bodies,
     * `}; // struct Foo` for named constructs) -- without touching any of the untouched data
     * lines above it. Only fires when the brace-initializer's own `{...}` already spans a
     * newline (a short flat init that {@code DeclarationAlignmentRule} successfully collapsed to
     * one line by this point has no newline left inside it, so this pass is a no-op for it) and
     * the `}` is not already alone on its own line.
     */
    private String applyOversizedAggregateInitClosingBracePass(final List<Token> tokens) {
        final List<Replacement> replacements = new ArrayList<>();
        final int n = tokens.size();
        for (int idx = 0; idx < n; idx++) {
            if (!isOp(tokens.get(idx), "=")) {
                continue;
            }
            final int openIdx = nextSignificantIndex(tokens, idx);
            if (openIdx < 0 || !isPunct(tokens.get(openIdx), "{")) {
                continue;
            }
            int depth = 1;
            int k = openIdx + 1;
            boolean hasNewlineInside = false;
            while (k < n && depth > 0) {
                final Token tk = tokens.get(k);
                if (isPunct(tk, "{")) {
                    depth++;
                } else if (isPunct(tk, "}")) {
                    depth--;
                    if (depth == 0) {
                        break;
                    }
                } else if (tk.type == TokenType.NEWLINE) {
                    hasNewlineInside = true;
                }
                k++;
            }
            if (depth != 0 || !hasNewlineInside) {
                continue;
            }
            final int closeIdx = k;
            final int semiIdx = nextSignificantIndex(tokens, closeIdx);
            if (semiIdx < 0 || !isPunct(tokens.get(semiIdx), ";")) {
                continue;
            }
            int wsStart = closeIdx;
            boolean sameLine = false;
            while (wsStart > openIdx + 1) {
                final Token pt = tokens.get(wsStart - 1);
                if (pt.type == TokenType.NEWLINE) {
                    break;
                }
                if (pt.type == TokenType.WHITESPACE) {
                    wsStart--;
                    continue;
                }
                sameLine = true;
                break;
            }
            if (!sameLine) {
                continue; // `}` is already alone on its own line -- nothing to do
            }
            String indent = "";
            for (int p = openIdx - 1; p >= 0; p--) {
                if (tokens.get(p).type == TokenType.NEWLINE) {
                    final StringBuilder sb = new StringBuilder();
                    int q = p + 1;
                    while (q < openIdx && tokens.get(q).type == TokenType.WHITESPACE) {
                        sb.append(tokens.get(q).text);
                        q++;
                    }
                    indent = sb.toString();
                    break;
                }
            }
            replacements.add(new Replacement(wsStart, closeIdx, "\n" + indent));
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
            // See applyDeclarationsPass: use firstIdx, not firstSpan.start, so a marker in the
            // leading gap doesn't falsely mark this unfrozen assignment group as frozen.
            if (anyFrozen(tokens, firstIdx, lastSpan.end)) {
                continue;
            }

            final String rawLeadingGap = joinText(tokens, firstSpan.start, firstIdx);
            final String rawIndent = trailingIndent(rawLeadingGap);
            final String indent = normalizeIndent(rawIndent);
            final String leadingGap = normalizeLeadingGap(rawLeadingGap, rawIndent, indent);
            final List<String> lines = miscRule.render(group);
            final String text = leadingGap + String.join("\n" + indent, lines);
            int lastTermEnd = lastSpan.end;
            while (lastTermEnd > lastSpan.start && isWhitespaceOrNewline(tokens.get(lastTermEnd - 1))) {
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
                if (!lang.isJava) {
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
            if (lang.isJava && isEnumConstantBody(tokens, span.openBraceIdx)) {
                continue;
            }

            int leadStart = nextSignificantIndex(tokens, span.start - 1);
            if (leadStart < 0) {
                continue;
            }
            // A preprocessor directive (e.g. a lone `#endif` closing a field-level `#ifdef`
            // guard) sitting on its own line before this method is not part of the signature --
            // isGapToken() deliberately treats PREPROCESSOR/MACRO_DEF as significant (so
            // statement-splitting elsewhere doesn't swallow it), but that means
            // nextSignificantIndex() stops on it here, making it look like the signature's own
            // first token. Left uncorrected, the whole directive line gets pulled out of
            // leadingGap and re-glued directly onto the rendered signature's first line with no
            // newline. Skip past any number of leading directive lines (each still its own
            // physical line, i.e. followed by a NEWLINE) to find the real lead token instead.
            while (leadStart >= 0 && (tokens.get(leadStart).type == TokenType.PREPROCESSOR
                    || tokens.get(leadStart).type == TokenType.MACRO_DEF)) {
                leadStart = nextSignificantIndex(tokens, leadStart);
            }
            if (leadStart < 0) {
                continue;
            }
            if (anyFrozen(tokens, leadStart, (throwsEndIdx >= 0 ? throwsEndIdx : closeParenIdx) + 1)) {
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
            if (lang.isJava) {
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
                        sigLeadStart = lineFirst >= 0
                                ? extendOverLeadingRequiresAndTemplate(tokens, lineFirst, leadStart)
                                : leadStart;
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
    private String applyGetterSetterPass(final List<Token> tokens, final int depth) {
        final List<List<Member>> groups = getterSetterRule.groupOneLiners(tokens, depth);
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
                final int startIdx = m.templatePrefixFrom != m.templatePrefixTo ? m.templatePrefixFrom : m.returnTypeFrom;
                final int sigIdx = m.modifiers.isEmpty() ? startIdx : indexOf.get(m.modifiers.get(0));
                // Use sigIdx (the member's own first real token), not m.memberFrom: memberFrom's
                // leading gap can contain a previous statement's trailing marker comment (always
                // itself stamped frozen), which must not falsely freeze this unrelated member.
                if (anyFrozen(tokens, sigIdx, m.memberTo)) {
                    continue;
                }
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
    private String processScope(final List<Token> tokens, final int depth, final boolean scopeStartFrozen) {
        List<Token> current = tokens;
        current = tokenize(applyDeclarationsPass(current), scopeStartFrozen);
        current = tokenize(applyOversizedAggregateInitClosingBracePass(current), scopeStartFrozen);
        current = tokenize(applyAssignmentsPass(current), scopeStartFrozen);
        current = tokenize(applySignaturePass(current, depth), scopeStartFrozen);
        current = tokenize(applyGetterSetterPass(current, depth), scopeStartFrozen);

        final List<Span> spans = splitTopLevelSpans(current);
        final List<Replacement> replacements = new ArrayList<>();
        for (final Span span : spans) {
            if (span.openBraceIdx < 0) {
                continue;
            }
            // A child scope extracted as raw text below may not textually contain the
            // JXM_CFMT_DIS marker that caused its own `{` to already be frozen on entry (the
            // marker can live outside this span entirely) -- re-tokenizing it must seed that
            // same frozen state explicitly rather than defaulting to unfrozen (RDD_KEY_90 §A).
            final boolean childStartFrozen = current.get(span.openBraceIdx).frozen;
            String childSource = joinText(current, span.openBraceIdx + 1, span.closeBraceIdx);
            final boolean isNamedScope = current.get(span.openBraceIdx).name != null;
            // Pre-expand named-construct one-liner bodies (`struct Foo { int a; int b; };`)
            // into multi-line form so that applyDeclarationsPass/applyAssignmentsPass in the
            // child scope see newline-separated source and produce correctly-indented output.
            // Non-named scopes (function/loop/lambda bodies) are left alone -- their one-liner
            // bodies must stay inline for getter/setter grouping and Allman detection.
            if (isNamedScope && !hasTopLevelNewline(current, span.openBraceIdx + 1, span.closeBraceIdx)) {
                final String trimmed = childSource.trim();
                if (!trimmed.isEmpty()) {
                    final String parentIndent = findParentIndent(current, span, depth);
                    if (parentIndent != null) {
                        childSource = "\n" + parentIndent + "    " + trimmed + "\n" + parentIndent;
                    }
                }
            }
            final String childResult;
            if (!isNamedScope && !hasTopLevelNewline(current, span.openBraceIdx + 1, span.closeBraceIdx)) {
                // One-liner non-named body (method/constructor/loop `{ ... }` kept on its
                // original single line) -- recursing would run it through the §5/§6
                // declaration/assignment grouping passes, which assume a real multi-statement
                // block and split+column-align each statement onto its own line. That's wrong
                // here: a single-line body must stay exactly as written so later passes
                // (GetterSetterRule one-liner grouping, Allman-brace conversion) can still
                // recognize and handle it as a one-liner.
                childResult = childSource;
            } else {
                // A `namespace` body is never indented (STYLE_C_CPP.md §7's closing-comment
                // examples show namespace content flush with the namespace itself), unlike
                // every other named construct (class/struct/enum) or function/loop body -- so
                // it must not consume an indentation level the way `depth + 1` otherwise would.
                final int childDepth = isNamespaceScope(current, span.openBraceIdx) ? depth : depth + 1;
                final String rawChildResult = processScope(tokenize(childSource, childStartFrozen), childDepth,
                        childStartFrozen);
                // The gap between the last statement and the closing `}` belongs to no
                // statement, so no pass above ever re-derives its indentation from depth --
                // it is otherwise carried through verbatim from the original source (e.g. a
                // misindented `}` that happened to line up with its own body rather than with
                // the frame that opened it). Force it to the frame's own indent here, unless
                // the closing brace or its immediate content is a frozen (JXM_CFMT_DIS) region
                // that must be left byte-for-byte untouched.
                // Only force-reindent when the original gap right before `}` is pure
                // whitespace: a comment sitting there (e.g. between a block and a following
                // `else`) is content other passes already position/associate correctly, and
                // blindly reindenting around it has been observed to corrupt that placement.
                final String parentIndent = findParentIndent(current, span, depth);
                if (anyFrozen(current, span.openBraceIdx, span.closeBraceIdx + 1)
                        || trailingGapHasComment(current, span.closeBraceIdx) || parentIndent == null
                        || rawChildResult.trim().isEmpty()) {
                    // An empty/whitespace-only body (`{}`/`{ }`) has no statement to hang a
                    // trailing gap off of -- forcing a "\n" + indent here would turn a genuinely
                    // empty body into an expanded `{\n}`, which a prior fix deliberately stopped
                    // doing (see STATE.md's java_modern empty-named-construct-body entry).
                    childResult = rawChildResult;
                } else {
                    childResult = trimTrailingWhitespace(rawChildResult) + "\n" + parentIndent;
                }
            }
            replacements.add(new Replacement(span.openBraceIdx + 1, span.closeBraceIdx, childResult));
        }
        return splice(current, replacements);
    }

    /** Public entry point: tokenizes {@code source} and runs the recursive scope pipeline,
     *  starting at depth 0. The one method {@code Main.java} calls once per file. */
    public String process(final String source) {
        return processScope(tokenize(source, formatOff), 0, formatOff);
    }

    // ── Token-scanning helpers ───────────────────────────────────────────────────
    // isPunct/isOp/isGapToken are centralized on TokenizerCore.Token (static-imported below);
    // prevSignificantIndex/nextSignificantIndex use the exclusive-of-`from` convention (matching
    // JavaSpecificRule/CppSpecificRule, since isCandidateSignatureName/isEnumConstantBody above
    // are ported verbatim from there), not MiscRule's inclusive-of-`from` convention.

    /** Like {@code Token.isGapToken} but excludes comments -- used to trim a declaration/assignment
     *  group's replaced span down to its true trailing content without eating a same-line trailing
     *  comment that the group's own rendered {@code text} did NOT already re-include verbatim
     *  (unlike a mid-group member, the group's *last* member's trailing comment is only captured
     *  once, by {@code Declaration.trailingComment}/{@code Assignment.trailingComment} and rendered
     *  into `text` -- if this trim treated the comment as trimmable "gap" too, it would stay behind
     *  in the untouched source right after the replaced span, duplicating it in the output). */
    private boolean isWhitespaceOrNewline(final Token t) {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE;
    }

    /**
     * True iff a {@code NEWLINE} appears anywhere in {@code [fromInclusive, toExclusive)} while
     * paren/bracket depth (relative to {@code fromInclusive}) is exactly 0. Used instead of a
     * raw {@code String.contains("\n")} check to decide whether a one-liner body is still a
     * single logical statement: on a fresh format a one-liner body never contains a newline at
     * all, but on a *reformat* of already-formatted output, {@code MiscRule.enforceCallLineBreaking}
     * may have already broken an over-length call's argument list across multiple physical
     * lines while leaving it one logical statement -- those newlines are strictly inside the
     * call's own parens (depth > 0) and must not be mistaken for a real multi-statement body.
     */
    private boolean hasTopLevelNewline(final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        int depth = 0;
        for (int i = fromInclusive; i < toExclusive; i++) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.PUNCT && ("(".equals(t.text) || "[".equals(t.text))) {
                depth++;
            } else if (t.type == TokenType.PUNCT && (")".equals(t.text) || "]".equals(t.text))) {
                depth--;
            } else if (t.type == TokenType.NEWLINE && depth == 0) {
                return true;
            }
        }
        return false;
    }

    private boolean anyFrozen(final List<Token> tokens, final int fromInclusive, final int toExclusive) {
        for (int i = fromInclusive; i < toExclusive; i++) {
            if (tokens.get(i).frozen) {
                return true;
            }
        }
        return false;
    }

    /** True iff a {@code COMMENT_LINE}/{@code COMMENT_BLOCK} token sits anywhere in the pure-gap
     *  run immediately before {@code closeBraceIdx} (i.e. between it and the nearest preceding
     *  non-gap token). */
    private boolean trailingGapHasComment(final List<Token> tokens, final int closeBraceIdx) {
        for (int i = closeBraceIdx - 1; i >= 0; i--) {
            final TokenType ty = tokens.get(i).type;
            if (ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) {
                return true;
            }
            if (!isGapToken(tokens.get(i))) {
                return false;
            }
        }
        return false;
    }

    private int prevSignificantIndex(final List<Token> tokens, final int from) {
        for (int i = from - 1; i >= 0; i--) {
            if (!isGapToken(tokens.get(i))) {
                return i;
            }
        }
        return -1;
    }

    /** Starting from the declarator's own line ({@code lineFirst}), pulls in an immediately
     *  preceding leading `requires` clause line (`template<T>\n    requires ...\n Decl`), and --
     *  only when such a requires line was found -- the `template<...>` header line above *that*,
     *  so the whole group collapses onto the declarator's line. A bare `template<...>` header
     *  with no requires clause is left untouched (stays verbatim on its own line, per the
     *  resolved decision covering `cpp_modern_inp.cpp` Bug 4a) since the requires-line pull is
     *  what gates the template-line pull. */
    private int extendOverLeadingRequiresAndTemplate(final List<Token> tokens, final int lineFirst,
            final int leadStart) {
        int cur = lineFirst;
        boolean pulledRequires = false;
        while (true) {
            // `cur`'s own line is separated from the line above it by the NEWLINE directly
            // preceding `cur` (`ownLineSep`) -- the previous line's own first token sits between
            // the NEWLINE *before that* (`prevLineSep`) and `ownLineSep`.
            int ownLineSep = -1;
            for (int j = cur - 1; j >= leadStart; j--) {
                if (tokens.get(j).type == TokenType.NEWLINE) { ownLineSep = j; break; }
            }
            if (ownLineSep < 0) {
                break;
            }
            int prevLineSep = -1;
            for (int j = ownLineSep - 1; j >= leadStart; j--) {
                if (tokens.get(j).type == TokenType.NEWLINE) { prevLineSep = j; break; }
            }
            final int prevLineFirst = prevLineSep >= 0
                    ? nextSignificantIndex(tokens, prevLineSep)
                    : (leadStart < ownLineSep ? leadStart : -1);
            if (prevLineFirst < 0 || prevLineFirst >= cur) {
                break;
            }
            final Token first = tokens.get(prevLineFirst);
            if (!pulledRequires && first.type == TokenType.KEYWORD && "requires".equals(first.text)) {
                cur = prevLineFirst;
                pulledRequires = true;
                continue;
            }
            if (pulledRequires && first.type == TokenType.KEYWORD && "template".equals(first.text)) {
                cur = prevLineFirst;
            }
            break;
        }
        return cur;
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
