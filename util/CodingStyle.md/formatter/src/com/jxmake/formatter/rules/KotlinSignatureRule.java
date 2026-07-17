/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * STYLE_KOTLIN.md §7: constructor/function parameter list line-breaking and column alignment
 * (§7.1's default-value `=` spacing folds into the same parser, since a default value is just
 * one more optional trailing part of a single parameter's grammar). Extends {@link MiscRuleCurly} to
 * reuse its language-agnostic boundary-finding/rendering primitives ({@code matchParenForward},
 * {@code significantWithComments}, {@code splitTopLevelCommas}, {@code renderTokens},
 * {@code indentText} -- each raised private -> protected in the base class for this purpose,
 * mirroring {@link KotlinDeclarationAlignmentRule}'s RDD_KEY_103 precedent), but not its
 * {@code Param}/{@code Signature} model, which is hard-baked to C/Java's
 * {@code [typeTokens] name [sizeTokens]} token order. Kotlin's
 * {@code [modifiers] name : type [= default]} grammar is reversed (name before type), so §7 gets
 * its own parameter model, parser, and grid-rendering method here.
 */
public class KotlinSignatureRule extends MiscRuleCurly {

    private static final List<String> PARAM_MODIFIERS =
            java.util.Arrays.asList("vararg", "crossinline", "noinline", "val", "var");

    public KotlinSignatureRule(final Lang lang) {
        super(lang, false, false);
    }

    public KotlinSignatureRule(final Lang lang, final int indentWidth, final int lineLengthLimit) {
        super(lang, false, false, indentWidth, lineLengthLimit);
    }

    /** One parsed `[modifiers] name : type [= default]` parameter. {@code typeTokens} is never
     *  empty on a successfully parsed param -- Kotlin requires an explicit type on every
     *  function/constructor parameter, unlike a `val`/`var` property's optional inferred type
     *  (STYLE_KOTLIN.md §6) -- so {@link #parseKotlinParam} returns null (bailing the whole
     *  signature, same "never guess past an unrecognized shape" posture as {@code MiscRuleCore}'s own
     *  {@code parseParam}) rather than modeling a missing type. */
    public static final class KotlinParam {
        public final List<Token> modifiers;
        public final Token name;
        public final List<Token> typeTokens;
        public final List<Token> defaultTokens; // empty if none
        public final Token comment;
        public final Token leadingComment;

        KotlinParam(final List<Token> modifiers, final Token name, final List<Token> typeTokens,
                final List<Token> defaultTokens, final Token comment, final Token leadingComment) {
            this.modifiers = modifiers;
            this.name = name;
            this.typeTokens = typeTokens;
            this.defaultTokens = defaultTokens;
            this.comment = comment;
            this.leadingComment = leadingComment;
        }
    }

    /** One parsed signature: `leadTokens name ( params )`, `leadTokens` being every token before
     *  the name (`fun`, an optional `<T>` generic-parameter clause, an optional extension-function
     *  receiver type, modifiers) -- same "not split apart" posture as {@code MiscRuleCurly.Signature},
     *  since §7 has no per-row alignment across multiple signatures. {@code trailingComma} records
     *  whether the source's last parameter was itself followed by a comma before `)`, so
     *  {@link #render} can preserve it exactly as written (STYLE_KOTLIN.md §7.2 -- never added,
     *  never removed). */
    public static final class KotlinSignature {
        public final List<Token> leadTokens;
        public final Token name;
        public final List<KotlinParam> params;
        public final boolean trailingComma;

        KotlinSignature(final List<Token> leadTokens, final Token name, final List<KotlinParam> params,
                final boolean trailingComma) {
            this.leadTokens = leadTokens;
            this.name = name;
            this.params = params;
            this.trailingComma = trailingComma;
        }
    }

    /**
     * Parses `sigTokens` -- already isolated by the caller, spanning from the first lead token
     * through the parameter list's closing `)` and nothing past it -- into a {@link KotlinSignature}.
     * Boundary-finding (locating the name via the IDENTIFIER immediately before the first depth-0
     * `(`) duplicates {@code MiscRuleCurly.parseSignature}'s own logic rather than reusing it directly --
     * same "exact copy, not shared utility" precedent already used for this file's {@code
     * renderTokens} lineage, since that boundary-finding is small and neither class currently
     * exposes it as a standalone method. Returns null if the shape doesn't match, if there are
     * trailing tokens past the matched `)`, or if any parameter fails to parse.
     */
    public KotlinSignature parseKotlinSignature(final List<Token> sigTokens) {
        final List<Token> sig = significantWithComments(sigTokens);
        int openParen = -1;
        int nameIdx = -1;
        int depth = 0;
        for (int i = 0; i < sig.size(); i++) {
            final Token t = sig.get(i);
            if (t.type == TokenType.ANGLE_BRACKET_OPEN) {
                depth++;
            } else if (t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                depth--;
            } else if (depth == 0 && isPunct(t, "(") && i > 0
                    && sig.get(i - 1).type == TokenType.IDENTIFIER) {
                // The first `IDENTIFIER (` on the line is not always the real signature -- a
                // leading `context(raise: Raise<Error>)` clause (Kotlin context receivers) is
                // itself an `IDENTIFIER (...)` shape that can share the same physical line as the
                // real `fun name(...)` when this pass runs before MiscRuleCurly.enforceCallLineBreaking
                // has had a chance to split it onto its own line(s) -- confirmed via harness
                // (arrow-kt/arrow's `RaiseContext.kt`, `context(raise: Raise<Error>) @RaiseDSL
                // @IgnorableReturnValue public inline fun <Error, B : Any> ensureNotNull(...)`)
                // this caused a round1-vs-round2 idempotency flap: round1 misidentified
                // `context(`'s own paren as the signature, found its close paren wasn't the
                // slice's last token, and bailed out (returning null) leaving the real signature
                // completely unprocessed/unwrapped; round2 (re-parsing round1's own output, where
                // `context(...)` was by then already split across lines by the unrelated call-
                // breaking pass) no longer had this ambiguity and correctly wrapped it. Instead of
                // bailing on the first non-matching candidate, skip past it and keep scanning for
                // the real signature's own `(`.
                final int candidateClose = matchParenForward(sig, i);
                if (candidateClose == sig.size() - 1) {
                    openParen = i;
                    nameIdx = i - 1;
                    break;
                }
                if (candidateClose > i) {
                    i = candidateClose;
                }
            }
        }
        if (openParen < 0) {
            return null;
        }
        final int closeParen = matchParenForward(sig, openParen);
        if (closeParen != sig.size() - 1) {
            return null;
        }

        final List<Token> leadTokens = new ArrayList<>(sig.subList(0, nameIdx));
        final Token name = sig.get(nameIdx);
        final List<Token> paramsSlice = sig.subList(openParen + 1, closeParen);

        if (paramsSlice.isEmpty()) {
            return new KotlinSignature(leadTokens, name, new ArrayList<KotlinParam>(), false);
        }

        final List<List<Token>> parts = splitTopLevelCommas(paramsSlice);
        // A trailing comma after the last param (`foo(x: Int,)`) leaves an empty final part from
        // the comma split above -- capture that as `trailingComma` before dropping it, so
        // STYLE_KOTLIN.md §7.2's "preserve exactly as written" can be honored on render.
        boolean trailingComma = false;
        if (parts.size() > 1 && significantOnly(parts.get(parts.size() - 1)).isEmpty()) {
            trailingComma = true;
            parts.remove(parts.size() - 1);
        }
        for (int i = 0; i < parts.size() - 1; i++) {
            final List<Token> next = parts.get(i + 1);
            if (!next.isEmpty() && (next.get(0).type == TokenType.COMMENT_LINE
                    || next.get(0).type == TokenType.COMMENT_BLOCK)) {
                parts.get(i).add(next.remove(0));
            }
        }

        final List<KotlinParam> params = new ArrayList<>();
        for (final List<Token> slice : parts) {
            final KotlinParam p = parseKotlinParam(slice);
            if (p == null) {
                return null;
            }
            params.add(p);
        }
        return new KotlinSignature(leadTokens, name, params, trailingComma);
    }

    /** Parses one already-comma-split param slice as `[modifiers] name : type [= default]`,
     *  returning null for anything that doesn't match -- an annotation-prefixed param, a
     *  destructuring lambda param, or any other shape with no STYLE_KOTLIN.md §7 worked example. */
    private KotlinParam parseKotlinParam(final List<Token> rawSlice) {
        if (rawSlice.isEmpty()) {
            return null;
        }
        Token comment = null;
        List<Token> slice = rawSlice;
        final Token last = rawSlice.get(rawSlice.size() - 1);
        if (last.type == TokenType.COMMENT_LINE || last.type == TokenType.COMMENT_BLOCK) {
            comment = last;
            slice = rawSlice.subList(0, rawSlice.size() - 1);
        }
        if (slice.isEmpty()) {
            return null;
        }
        Token leadingComment = null;
        final Token first = slice.get(0);
        if (slice.size() > 1
                && (first.type == TokenType.COMMENT_LINE || first.type == TokenType.COMMENT_BLOCK)) {
            leadingComment = first;
            slice = slice.subList(1, slice.size());
        }
        if (slice.isEmpty()) {
            return null;
        }

        int i = 0;
        final List<Token> modifiers = new ArrayList<>();
        while (i < slice.size() && slice.get(i).type == TokenType.IDENTIFIER
                && PARAM_MODIFIERS.contains(slice.get(i).text)
                && i + 1 < slice.size() && slice.get(i + 1).type == TokenType.IDENTIFIER) {
            modifiers.add(slice.get(i));
            i++;
        }
        while (i < slice.size() && slice.get(i).type == TokenType.KEYWORD
                && PARAM_MODIFIERS.contains(slice.get(i).text)) {
            modifiers.add(slice.get(i));
            i++;
        }

        if (i >= slice.size() || slice.get(i).type != TokenType.IDENTIFIER) {
            return null;
        }
        final Token name = slice.get(i);
        i++;

        if (i >= slice.size() || !isOp(slice.get(i), ":")) {
            return null; // Kotlin requires an explicit type on every param -- never guess one
        }
        i++;

        final int typeStart = i;
        while (i < slice.size() && !isOp(slice.get(i), "=")) {
            i++;
        }
        final List<Token> typeTokens = new ArrayList<>(slice.subList(typeStart, i));
        if (typeTokens.isEmpty()) {
            return null;
        }

        List<Token> defaultTokens = Collections.emptyList();
        if (i < slice.size() && isOp(slice.get(i), "=")) {
            i++;
            defaultTokens = new ArrayList<>(slice.subList(i, slice.size()));
            i = slice.size();
        }

        if (i != slice.size()) {
            return null;
        }
        return new KotlinParam(modifiers, name, typeTokens, defaultTokens, comment, leadingComment);
    }

    /**
     * Renders one signature (STYLE_KOTLIN.md §7) inline if it fits within {@link #lineLengthLimit}
     * at its starting column, or broken to one parameter per line otherwise -- same line-length
     * decision as {@code MiscRuleCurly.render(Signature, ...)}. Broken form uses a {@link ColumnGrid}
     * (name, `: type`, `= default`) rather than the base class's manual width pre-computation,
     * same "grammar simple enough that the grid alone produces the required alignment" reasoning
     * as {@link KotlinDeclarationAlignmentRule#renderPropertyGroup} -- the worked example's
     * `:`-column-detached-from-name spacing (`id    : Long,`) falls out of `ColumnGrid.flush()`'s
     * per-column padding plus a single-space join with no extra arithmetic needed.
     */
    public List<String> render(final KotlinSignature sig, final int indentLevel, final String indentStyle) {
        final String lead = renderTokens(sig.leadTokens);
        final String head = (lead.isEmpty() || lead.endsWith(".") ? lead : lead + " ") + sig.name.text + "(";
        final String inline = head + renderParamsInline(sig) + ")";
        final int startColumn = indentLevel * indentWidth;

        int commentLen = 0;
        boolean hasLineComment = false;
        for (final KotlinParam p : sig.params) {
            if (p.comment != null) {
                commentLen += p.comment.text.length() + 1;
                if (p.comment.type == TokenType.COMMENT_LINE) {
                    hasLineComment = true;
                }
            }
        }
        if (!hasLineComment
                && (sig.params.isEmpty() || startColumn + inline.length() - commentLen <= lineLengthLimit)) {
            return Collections.singletonList(inline);
        }

        // A param carrying its own leading same-line comment (e.g. `/* Nullable */ x : Int?`) is
        // excluded from the shared ColumnGrid, same "exclude it from the group -- don't let it
        // distort the alignment of the others" precedent as STYLE.md's getter/setter grouping: the
        // grid only sees each row's bare name cell, not the leading-comment prefix rendered ahead of
        // it, so including such a row would silently pad its sibling rows' name columns out to a
        // width that never actually lines up visually once the prefix is prepended.
        final ColumnGrid grid = new ColumnGrid();
        final List<Integer> gridParamIdx = new ArrayList<>();
        final String[] soloLine = new String[sig.params.size()];
        for (int idx = 0; idx < sig.params.size(); idx++) {
            final KotlinParam p = sig.params.get(idx);
            final boolean isLast = idx == sig.params.size() - 1;
            final String modPrefix = p.modifiers.isEmpty() ? "" : renderTokens(p.modifiers) + " ";
            final String comma = (!isLast || sig.trailingComma) ? "," : "";
            // The comma is never its own grid column -- appending it as a bare cell would make
            // `ColumnGrid` pad every row's type cell out to the widest sibling before the comma
            // (an extra stray gap, e.g. "id    : Long   ,"), since only a row's own true *last*
            // cell is left unpadded. Attaching it directly to whichever cell is actually last for
            // that row (type, or default when present) keeps that cell correctly unpadded and
            // matches STYLE_KOTLIN.md §7's worked example (`val id    : Long,`, no gap before `,`).
            final List<String> cells = new ArrayList<>();
            cells.add(modPrefix + p.name.text);
            if (p.defaultTokens.isEmpty()) {
                cells.add(": " + renderTokens(p.typeTokens) + comma);
            } else {
                cells.add(": " + renderTokens(p.typeTokens));
                cells.add("= " + renderTokens(p.defaultTokens) + comma);
            }
            if (p.comment != null) {
                cells.add(p.comment.text);
            }
            final String leadPrefix = p.leadingComment != null ? p.leadingComment.text + " " : "";
            if (p.leadingComment != null) {
                soloLine[idx] = leadPrefix + trimTrailingSpaces(String.join(" ", cells));
            } else {
                grid.addRow(cells.toArray(new String[0]));
                gridParamIdx.add(idx);
            }
        }

        final List<String> lines = new ArrayList<>();
        lines.add(head);
        final String paramIndent = indentText(indentLevel + 1, indentStyle);
        final List<String[]> rows = grid.flush();
        final String[] renderedLine = new String[sig.params.size()];
        for (int r = 0; r < rows.size(); r++) {
            renderedLine[gridParamIdx.get(r)] = trimTrailingSpaces(String.join(" ", rows.get(r)));
        }
        for (int idx = 0; idx < sig.params.size(); idx++) {
            final String line = soloLine[idx] != null ? soloLine[idx] : renderedLine[idx];
            lines.add(paramIndent + line);
        }
        lines.add(indentText(indentLevel, indentStyle) + ")");
        return lines;
    }

    private String renderParamsInline(final KotlinSignature sig) {
        if (sig.params.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sig.params.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            final KotlinParam p = sig.params.get(i);
            if (!p.modifiers.isEmpty()) {
                sb.append(renderTokens(p.modifiers)).append(' ');
            }
            sb.append(p.name.text).append(": ").append(renderTokens(p.typeTokens));
            if (!p.defaultTokens.isEmpty()) {
                sb.append(" = ").append(renderTokens(p.defaultTokens));
            }
            if (p.comment != null) {
                sb.append(' ').append(p.comment.text);
            }
        }
        if (sig.trailingComma) {
            sb.append(','); // STYLE_KOTLIN.md §7.2 -- preserved exactly as written, even inline
        }
        return sb.toString();
    }

    /** STYLE_KOTLIN.md §9: everything after a function signature's parameter-list close paren --
     *  an optional `: ReturnType` and/or an optional expression body (`= expr`). Either or both may
     *  be empty/absent (a block-bodied function with an inferred return type has neither; one with
     *  an explicit return type but a `{ ... }` block body has {@code returnTypeTokens} only). The
     *  leading `:` and `=` themselves are stripped here (not carried in the token lists), matching
     *  {@link KotlinParam}'s own established convention of rendering its `: type`/`= default` cells
     *  with an explicit literal prefix rather than keeping the operator token in the slice. */
    public static final class FunctionTail {
        public final List<Token> returnTypeTokens; // empty if no explicit return type
        public final List<Token> exprTokens; // empty if not expression-bodied
        public final boolean hasEqual;

        FunctionTail(final List<Token> returnTypeTokens, final List<Token> exprTokens, final boolean hasEqual) {
            this.returnTypeTokens = returnTypeTokens;
            this.exprTokens = exprTokens;
            this.hasEqual = hasEqual;
        }
    }

    /** Parses the tokens following a signature's closing `)` -- everything up to (not including)
     *  the function's own body-opening `{` for a block-bodied function, or through the end of the
     *  expression for an expression-bodied one. Returns null only if given no tokens to parse
     *  (there is always at least a valid, possibly-empty {@link FunctionTail} otherwise). */
    public FunctionTail parseFunctionTail(final List<Token> tailTokens) {
        final List<Token> sig = significantWithComments(tailTokens);
        int i = 0;
        List<Token> returnType = Collections.emptyList();
        if (i < sig.size() && isOp(sig.get(i), ":")) {
            i++;
            final int start = i;
            while (i < sig.size() && !isOp(sig.get(i), "=")) {
                i++;
            }
            returnType = new ArrayList<>(sig.subList(start, i));
        }
        List<Token> expr = Collections.emptyList();
        boolean hasEqual = false;
        if (i < sig.size() && isOp(sig.get(i), "=")) {
            hasEqual = true;
            i++;
            expr = new ArrayList<>(sig.subList(i, sig.size()));
        }
        return new FunctionTail(returnType, expr, hasEqual);
    }

    /**
     * STYLE_KOTLIN.md §9: renders a full function signature including its `: ReturnType`/`= expr`
     * tail, extending {@link #render}'s params-only line-breaking with one further fallback level.
     * Three tiers, tried in order: (1) everything -- lead, params, tail -- fits on one line as
     * written, left exactly as-is (STYLE_KOTLIN.md §9's "standalone, fits inline" case); (2) break
     * the parameter list first (delegating to {@link #render} exactly as §7 already does) and
     * append the tail to the resulting `)` line -- if that combined line now fits, stop there; (3)
     * only if the tail includes an expression body (`= expr`) and tier 2 still doesn't fit, wrap
     * `= expr` onto its own line indented one level past the signature, mirroring how §7.1's
     * named-argument `=` wraps rather than inventing a new break rule (per the style doc's own
     * text). A `: ReturnType` with no `=` (an explicit-return-type block-bodied function) has
     * nothing left to wrap once tier 2 fails -- STYLE_KOTLIN.md §9 only documents the expression-
     * bodied case, so tier 3 is a no-op for that shape and the combined line is used as-is.
     */
    public List<String> renderWithTail(final KotlinSignature sig, final FunctionTail tail,
            final int indentLevel, final String indentStyle) {
        final String returnTypeStr = tail.returnTypeTokens.isEmpty() ? "" : ": " + renderTokens(tail.returnTypeTokens);
        // tail.exprTokens is empty when the `=` is immediately followed by a `{`-led construct
        // (e.g. a lambda literal body) that the tail parser deliberately doesn't consume, leaving
        // the remainder of the statement untouched downstream. In that case appending "= " (with
        // a trailing space baked in) double-spaces against the untouched remainder's own leading
        // whitespace before its own `{`/next line -- and since a bare "=" with no baked space is
        // recomputed identically every format pass while the untouched remainder is preserved
        // as-is, the trailing-space count would otherwise grow by one on every re-format
        // (non-idempotent). Only append the separating space when there is an actual rendered
        // expression to separate it from.
        final String exprStr = tail.hasEqual ? (tail.exprTokens.isEmpty() ? "=" : "= " + renderTokens(tail.exprTokens)) : "";
        final String tailStr = returnTypeStr.isEmpty() ? exprStr
                : (exprStr.isEmpty() ? returnTypeStr : returnTypeStr + " " + exprStr);

        final String lead = renderTokens(sig.leadTokens);
        final String head = (lead.isEmpty() || lead.endsWith(".") ? lead : lead + " ") + sig.name.text + "(";
        final String inline = appendTailPart(head + renderParamsInline(sig) + ")", tailStr);
        final int startColumn = indentLevel * indentWidth;

        boolean hasLineComment = false;
        int commentLen = 0;
        for (final KotlinParam p : sig.params) {
            if (p.comment != null) {
                commentLen += p.comment.text.length() + 1;
                if (p.comment.type == TokenType.COMMENT_LINE) {
                    hasLineComment = true;
                }
            }
        }
        if (!hasLineComment && startColumn + inline.length() - commentLen <= lineLengthLimit) {
            return Collections.singletonList(inline);
        }

        final List<String> lines;
        final String lastLine;
        if (!sig.params.isEmpty()) {
            // render() may itself decide the params-only inline form already fits (its own
            // bypass check has no knowledge of the tail this method is appending), returning an
            // immutable Collections.singletonList -- always re-wrap in a fresh mutable list since
            // this method may still need to rewrite that one line below.
            lines = new ArrayList<>(render(sig, indentLevel, indentStyle));
            lastLine = lines.get(lines.size() - 1);
        } else {
            // Nothing to break in an empty parameter list -- the tail is the only thing that
            // might still need to wrap (tier 3 below).
            lastLine = head + ")";
            lines = new ArrayList<>();
            lines.add(lastLine);
        }

        if (tailStr.isEmpty()) {
            return lines;
        }
        final String combined = appendTailPart(lastLine, tailStr);
        if (combined.length() <= lineLengthLimit || !tail.hasEqual) {
            lines.set(lines.size() - 1, combined);
            return lines;
        }

        final String closeLine = appendTailPart(lastLine, returnTypeStr) + " =";
        lines.set(lines.size() - 1, closeLine);
        lines.add(indentText(indentLevel + 1, indentStyle) + renderTokens(tail.exprTokens));
        return lines;
    }

    /** Joins {@code tailPart} onto {@code base} with no space if it's a `: ReturnType` cell (its
     *  leading `:` is tight against the preceding `)`, matching every other STYLE_KOTLIN.md `:`
     *  placement in this file) or with one space if it's an `= expr` cell (a normal binary-operator
     *  spacing) -- distinguished by {@code tailPart}'s own leading character, since both shapes are
     *  passed through this same join point (inline, params-broken-but-tail-fits, and the
     *  return-type-only half of the wrap-`=` case all share it). No-op if {@code tailPart} is empty. */
    private String appendTailPart(final String base, final String tailPart) {
        if (tailPart.isEmpty()) {
            return base;
        }
        return tailPart.startsWith(":") ? base + tailPart : base + " " + tailPart;
    }

    private String trimTrailingSpaces(final String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}
