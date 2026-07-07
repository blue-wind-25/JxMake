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

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * STYLE_KOTLIN.md §7: constructor/function parameter list line-breaking and column alignment
 * (§7.1's default-value `=` spacing folds into the same parser, since a default value is just
 * one more optional trailing part of a single parameter's grammar). Extends {@link MiscRule} to
 * reuse its language-agnostic boundary-finding/rendering primitives ({@code matchParenForward},
 * {@code significantWithComments}, {@code splitTopLevelCommas}, {@code renderTokens},
 * {@code indentText} -- each raised private -> protected in the base class for this purpose,
 * mirroring {@link KotlinDeclarationAlignmentRule}'s RDD_KEY_103 precedent), but not its
 * {@code Param}/{@code Signature} model, which is hard-baked to C/Java's
 * {@code [typeTokens] name [sizeTokens]} token order. Kotlin's
 * {@code [modifiers] name : type [= default]} grammar is reversed (name before type), so §7 gets
 * its own parameter model, parser, and grid-rendering method here.
 */
public class KotlinSignatureRule extends MiscRule {

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
     *  signature, same "never guess past an unrecognized shape" posture as {@code MiscRule}'s own
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
     *  receiver type, modifiers) -- same "not split apart" posture as {@code MiscRule.Signature},
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
     * `(`) duplicates {@code MiscRule.parseSignature}'s own logic rather than reusing it directly --
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
                openParen = i;
                nameIdx = i - 1;
                break;
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
     * decision as {@code MiscRule.render(Signature, ...)}. Broken form uses a {@link ColumnGrid}
     * (name, `: type`, `= default`) rather than the base class's manual width pre-computation,
     * same "grammar simple enough that the grid alone produces the required alignment" reasoning
     * as {@link KotlinDeclarationAlignmentRule#renderPropertyGroup} -- the worked example's
     * `:`-column-detached-from-name spacing (`id    : Long,`) falls out of `ColumnGrid.flush()`'s
     * per-column padding plus a single-space join with no extra arithmetic needed.
     */
    public List<String> render(final KotlinSignature sig, final int indentLevel, final String indentStyle) {
        final String lead = renderTokens(sig.leadTokens);
        final String head = (lead.isEmpty() ? "" : lead + " ") + sig.name.text + "(";
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

        final ColumnGrid grid = new ColumnGrid();
        final List<String> leadPrefixes = new ArrayList<>();
        final List<Token> leadingComments = new ArrayList<>();
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
            grid.addRow(cells.toArray(new String[0]));
            leadPrefixes.add(p.leadingComment != null ? p.leadingComment.text + " " : "");
            leadingComments.add(p.leadingComment);
        }

        final List<String> lines = new ArrayList<>();
        lines.add(head);
        final String paramIndent = indentText(indentLevel + 1, indentStyle);
        final List<String[]> rows = grid.flush();
        for (int idx = 0; idx < rows.size(); idx++) {
            final String joined = trimTrailingSpaces(String.join(" ", rows.get(idx)));
            lines.add(paramIndent + leadPrefixes.get(idx) + joined);
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

    private String trimTrailingSpaces(final String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }
}
