/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.FormatterSimpleBraced;
import com.jxmake.formatter.Lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * STYLE_DATA_FORMATS.md §5 (YAML) rule logic. Neither curly, indent-based, tag-based, nor
 * SimpleBraced per RDD_KEY_189/191 -- YAML's grammar is indentation-significant rather than
 * brace-delimited, so this class uses its own line-based recursive block parser instead of
 * {@code TokenizerCore}'s char-token-stream machinery.
 *
 * <p>Implements §5.1 (space-only indentation -- {@code indent-style} is ignored, only
 * {@code indent-size} applies), §5.2 (colon-alignment groups, broken by blank lines/comments),
 * §5.3 (sequence items indent one level deeper, or align under a mapping-item's first inline key),
 * §5.4 (flow collections preserved unless they overflow {@code line-length}, then block-converted),
 * §5.5 (anchors/aliases/tags preserved verbatim), §5.6 (block scalar bodies are opaque), and §5.7
 * (multi-document `---`/`...` streams, each an independent structural-depth reset). Also implements
 * its own {@code #%}-based {@code JXM_CFMT_DIS}/{@code ENA} frozen-span detection (YAML has no
 * block-comment form for {@code TokenizerCore.markFrozenSpans} to reuse).
 */
public final class YamlSpecificRule {

    private final Lang lang;
    private final int lineLengthLimit;
    private final int indentWidth;
    private final String indentUnit;
    private final boolean normalizeCommentStartCase;

    public YamlSpecificRule(final Lang lang) {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public YamlSpecificRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public YamlSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this(lang, lineLengthLimit, indentWidth, true);
    }

    public YamlSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth,
            final boolean normalizeCommentStartCase) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
        this.indentWidth = Math.max(1, indentWidth);
        this.indentUnit = repeatChar(' ', this.indentWidth);
        this.normalizeCommentStartCase = normalizeCommentStartCase;
    }

    /** Malformed YAML input that the parser cannot make sense of -- caught generically by
     *  {@code Main}'s per-file error handling, same as any other rule class's runtime failure. */
    public static final class YamlParseException extends RuntimeException {
        public YamlParseException(final String message) {
            super(message);
        }
    }

    private static String repeatChar(final char c, final int count) {
        final StringBuilder sb = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    private String indent(final int depth) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append(indentUnit);
        }
        return sb.toString();
    }

    private String normComment(final String commentText) {
        if (!normalizeCommentStartCase) {
            return commentText;
        }
        int i = 1;
        while (i < commentText.length() && commentText.charAt(i) == ' ') {
            i++;
        }
        if (i < commentText.length()) {
            final char ch = commentText.charAt(i);
            if (Character.isLetter(ch) && Character.isLowerCase(ch)) {
                return commentText.substring(0, i) + Character.toUpperCase(ch) + commentText.substring(i + 1);
            }
        }
        return commentText;
    }

    /** Finds the first unquoted, unbracketed ':' that acts as a mapping-key separator (must be
     *  followed by a space or end-of-string) -- distinguishes "name: Widget" from a bare scalar
     *  like "https://example.com" or an inline flow collection. Returns -1 if none. */
    private static int findMappingColon(final String s) {
        boolean inSingle = false;
        boolean inDouble = false;
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            final char ch = s.charAt(i);
            if (inSingle) {
                if (ch == '\'') {
                    inSingle = false;
                }
                continue;
            }
            if (inDouble) {
                if (ch == '\\') {
                    i++;
                } else if (ch == '"') {
                    inDouble = false;
                }
                continue;
            }
            if (ch == '\'') {
                inSingle = true;
            } else if (ch == '"') {
                inDouble = true;
            } else if (ch == '{' || ch == '[') {
                depth++;
            } else if (ch == '}' || ch == ']') {
                depth--;
            } else if (ch == ':' && depth == 0 && (i + 1 == s.length() || s.charAt(i + 1) == ' ')) {
                return i;
            }
        }
        return -1;
    }

    /** Splits off a same-line trailing `#` comment from {@code s}, respecting quotes -- a `#` only
     *  starts a comment when it is at the start of the string or preceded by whitespace. Returns a
     *  two-element array: [codePart (right-trimmed), commentPartOrNull]. */
    private static String[] splitTrailingComment(final String s) {
        boolean inSingle = false;
        boolean inDouble = false;
        for (int i = 0; i < s.length(); i++) {
            final char ch = s.charAt(i);
            if (inSingle) {
                if (ch == '\'') {
                    inSingle = false;
                }
                continue;
            }
            if (inDouble) {
                if (ch == '\\') {
                    i++;
                } else if (ch == '"') {
                    inDouble = false;
                }
                continue;
            }
            if (ch == '\'') {
                inSingle = true;
            } else if (ch == '"') {
                inDouble = true;
            } else if (ch == '#' && (i == 0 || s.charAt(i - 1) == ' ' || s.charAt(i - 1) == '\t')) {
                return new String[] {rtrim(s.substring(0, i)), s.substring(i)};
            }
        }
        return new String[] {rtrim(s), null};
    }

    private static String rtrim(final String s) {
        int end = s.length();
        while (end > 0 && Character.isWhitespace(s.charAt(end - 1))) {
            end--;
        }
        return s.substring(0, end);
    }

    // ---- Line model ----------------------------------------------------------------------------

    private static final class Line {
        final String raw;
        final int indent;
        final String content;

        Line(final String raw) {
            this.raw = raw;
            int i = 0;
            while (i < raw.length() && raw.charAt(i) == ' ') {
                i++;
            }
            this.indent = i;
            this.content = raw.substring(i);
        }

        boolean isBlank() {
            return content.isEmpty();
        }
    }

    // ---- AST -------------------------------------------------------------------------------------

    private static final class Item {
        List<String> leadingComments = new ArrayList<>();
        boolean blankBefore;
        boolean isSeq;
        boolean isFrozen;
        List<String> frozenLines;
        boolean dangling; // trailing comment(s)/blank with no following item at this block level
        String key; // non-null for mapping items
        String inlineValue; // raw scalar/flow/anchor text after ':' or '- ' on the same line
        String trailingComment;
        List<Item> children; // nested block belonging to this item (mapping or sequence)
        boolean seqOfMapping; // sequence item whose value is an inline-first-key mapping
        String blockScalarBody; // raw verbatim body (joined by \n) for | / > scalars
        String multilineScalarBody; // raw verbatim continuation lines (joined by \n) for a
                                     // quoted scalar whose closing quote isn't on the key's own line

        boolean isKeyed() {
            return key != null;
        }
    }

    // ---- Flow ({}/[]) parsing ----------------------------------------------------------------------

    private abstract static class FlowNode {
    }

    private static final class FlowScalar extends FlowNode {
        final String raw;

        FlowScalar(final String raw) {
            this.raw = raw;
        }
    }

    private static final class FlowEntry {
        final String key;
        final FlowNode value;

        FlowEntry(final String key, final FlowNode value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class FlowMap extends FlowNode {
        final List<FlowEntry> entries = new ArrayList<>();
    }

    private static final class FlowSeq extends FlowNode {
        final List<FlowNode> elements = new ArrayList<>();
    }

    private static final class FlowCursor {
        final String s;
        int i;

        FlowCursor(final String s) {
            this.s = s;
        }

        void skipWs() {
            while (i < s.length() && s.charAt(i) == ' ') {
                i++;
            }
        }

        char cur() {
            return i < s.length() ? s.charAt(i) : '\0';
        }
    }

    private static boolean looksLikeFlow(final String s) {
        return s.startsWith("{") || s.startsWith("[");
    }

    private FlowNode parseFlow(final FlowCursor c) {
        c.skipWs();
        final char ch = c.cur();
        if (ch == '{') {
            c.i++;
            final FlowMap map = new FlowMap();
            c.skipWs();
            if (c.cur() == '}') {
                c.i++;
                return map;
            }
            while (true) {
                c.skipWs();
                final String key = readFlowScalarText(c, true);
                c.skipWs();
                if (c.cur() != ':') {
                    throw new YamlParseException("expected ':' in flow mapping near: " + c.s.substring(c.i));
                }
                c.i++;
                c.skipWs();
                final FlowNode value = parseFlow(c);
                map.entries.add(new FlowEntry(key.trim(), value));
                c.skipWs();
                if (c.cur() == ',') {
                    c.i++;
                    continue;
                }
                if (c.cur() == '}') {
                    c.i++;
                    break;
                }
                throw new YamlParseException("unterminated flow mapping near: " + c.s.substring(c.i));
            }
            return map;
        }
        if (ch == '[') {
            c.i++;
            final FlowSeq seq = new FlowSeq();
            c.skipWs();
            if (c.cur() == ']') {
                c.i++;
                return seq;
            }
            while (true) {
                c.skipWs();
                seq.elements.add(parseFlow(c));
                c.skipWs();
                if (c.cur() == ',') {
                    c.i++;
                    continue;
                }
                if (c.cur() == ']') {
                    c.i++;
                    break;
                }
                throw new YamlParseException("unterminated flow sequence near: " + c.s.substring(c.i));
            }
            return seq;
        }
        return new FlowScalar(readFlowScalarText(c, false).trim());
    }

    /** Reads a flow scalar (unquoted, or a quoted string) up to the next structural character
     *  ({@code , ] }}, or -- for a mapping key -- {@code :}). */
    private String readFlowScalarText(final FlowCursor c, final boolean stopAtColon) {
        final int start = c.i;
        if (c.cur() == '"' || c.cur() == '\'') {
            final char quote = c.cur();
            c.i++;
            while (c.i < c.s.length() && c.s.charAt(c.i) != quote) {
                if (quote == '"' && c.s.charAt(c.i) == '\\') {
                    c.i++;
                }
                c.i++;
            }
            if (c.i < c.s.length()) {
                c.i++;
            }
            return c.s.substring(start, c.i);
        }
        while (c.i < c.s.length()) {
            final char ch = c.s.charAt(c.i);
            if (ch == ',' || ch == ']' || ch == '}' || (stopAtColon && ch == ':')) {
                break;
            }
            c.i++;
        }
        return c.s.substring(start, c.i);
    }

    private String renderFlowTight(final FlowNode node) {
        if (node instanceof FlowScalar) {
            return ((FlowScalar) node).raw;
        }
        if (node instanceof FlowMap) {
            final FlowMap map = (FlowMap) node;
            final StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < map.entries.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                final FlowEntry e = map.entries.get(i);
                sb.append(e.key).append(": ").append(renderFlowTight(e.value));
            }
            return sb.append('}').toString();
        }
        final FlowSeq seq = (FlowSeq) node;
        final StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < seq.elements.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(renderFlowTight(seq.elements.get(i)));
        }
        return sb.append(']').toString();
    }

    private boolean fits(final String alreadyWritten, final int extraLen) {
        return alreadyWritten.length() + extraLen <= lineLengthLimit;
    }

    /** Renders a keyed item's flow value: inline if the whole line fits within line-length, else
     *  block-converted per §5.4, recursing so nested flow collections get the same overflow test at
     *  their own depth. {@code keyPrefix} is the fully-rendered {@code key<pad>:} text (already
     *  appended to {@code out} at the correct indent, no trailing space/value yet). */
    private void renderFlowValue(final FlowNode node, final int depth, final String keyPrefix,
            final StringBuilder out) {
        final String tight = renderFlowTight(node);
        if (fits(keyPrefix, 1 + tight.length())) {
            out.append(' ').append(tight).append('\n');
            return;
        }
        out.append('\n');
        renderFlowBlock(node, depth + 1, out);
    }

    private void renderFlowValueOrScalar(final FlowNode value, final int depth, final String keyPrefix,
            final StringBuilder out) {
        if (value instanceof FlowScalar) {
            out.append(' ').append(((FlowScalar) value).raw).append('\n');
            return;
        }
        renderFlowValue(value, depth, keyPrefix, out);
    }

    private void renderFlowBlock(final FlowNode node, final int depth, final StringBuilder out) {
        if (node instanceof FlowMap) {
            final FlowMap map = (FlowMap) node;
            final List<String> keys = new ArrayList<>();
            for (final FlowEntry e : map.entries) {
                keys.add(e.key);
            }
            final String[] pad = FormatterSimpleBraced.padKeysForColonAlignment(keys);
            for (int i = 0; i < map.entries.size(); i++) {
                final FlowEntry e = map.entries.get(i);
                final String keyPrefix = indent(depth) + e.key + pad[i] + ":";
                out.append(keyPrefix);
                renderFlowValueOrScalar(e.value, depth, keyPrefix, out);
            }
            return;
        }
        final FlowSeq seq = (FlowSeq) node;
        for (final FlowNode elem : seq.elements) {
            if (elem instanceof FlowScalar) {
                out.append(indent(depth)).append("- ").append(((FlowScalar) elem).raw).append('\n');
                continue;
            }
            final String tight = renderFlowTight(elem);
            final String prefix = indent(depth) + "- ";
            if (fits(prefix, tight.length())) {
                out.append(prefix).append(tight).append('\n');
            } else {
                out.append(indent(depth)).append("-\n");
                renderFlowBlock(elem, depth + 1, out);
            }
        }
    }

    // ---- Line-based block parsing --------------------------------------------------------------

    private List<Line> lines;
    private int pos;

    private Line peek() {
        return pos < lines.size() ? lines.get(pos) : null;
    }

    /** Parses a single homogeneous block (all mapping keys, or all sequence dashes) whose items sit
     *  at exactly {@code blockIndent}. Stops (without consuming) at dedent, at end of input, or at a
     *  line whose shape (dash vs. key) doesn't match the block's own kind -- a block is always
     *  homogeneous per YAML's grammar, so a shape mismatch at the same column means control belongs
     *  back to the caller's own block instead. */
    private List<Item> parseBlock(final int blockIndent) {
        final List<Item> items = new ArrayList<>();
        List<String> pendingComments = new ArrayList<>();
        boolean pendingBlank = false;
        Boolean isSeqBlock = null;
        while (pos < lines.size()) {
            final Line ln = peek();
            if (ln.isBlank()) {
                pendingBlank = true;
                pos++;
                continue;
            }
            if (ln.indent != blockIndent) {
                break;
            }
            if (ln.content.startsWith("#")) {
                if ("#% JXM_CFMT_DIS".equals(ln.content)) {
                    final Item item = new Item();
                    item.leadingComments = pendingComments;
                    item.blankBefore = pendingBlank;
                    pendingComments = new ArrayList<>();
                    pendingBlank = false;
                    item.isFrozen = true;
                    item.frozenLines = new ArrayList<>();
                    item.frozenLines.add(ln.raw);
                    pos++;
                    while (pos < lines.size() && !"#% JXM_CFMT_ENA".equals(peek().content)) {
                        item.frozenLines.add(peek().raw);
                        pos++;
                    }
                    if (pos < lines.size()) {
                        item.frozenLines.add(peek().raw);
                        pos++;
                    }
                    items.add(item);
                    continue;
                }
                pendingComments.add(normComment(ln.content));
                pos++;
                continue;
            }
            final boolean lineIsSeq = ln.content.equals("-") || ln.content.startsWith("- ");
            if (isSeqBlock != null && isSeqBlock.booleanValue() != lineIsSeq) {
                break;
            }
            isSeqBlock = lineIsSeq;

            final Item item = new Item();
            item.leadingComments = pendingComments;
            item.blankBefore = pendingBlank;
            pendingComments = new ArrayList<>();
            pendingBlank = false;

            if (lineIsSeq) {
                parseSeqItem(item, ln);
            } else {
                parseKeyItem(item, ln);
            }
            items.add(item);
        }
        if (!pendingComments.isEmpty() || pendingBlank) {
            final Item d = new Item();
            d.leadingComments = pendingComments;
            d.blankBefore = pendingBlank;
            d.dangling = true;
            items.add(d);
        }
        return items;
    }

    private void parseKeyItem(final Item item, final Line ln) {
        pos++;
        final String[] parts = splitTrailingComment(ln.content);
        final String code = parts[0];
        final int colon = findMappingColon(code);
        if (colon < 0) {
            throw new YamlParseException("expected 'key:' mapping line, got: " + ln.content);
        }
        item.key = code.substring(0, colon).trim();
        final String after = code.substring(colon + 1).trim();
        if (after.startsWith("|") || after.startsWith(">")) {
            item.inlineValue = after;
            item.blockScalarBody = captureBlockScalarBody(ln.indent);
            return;
        }
        if (!after.isEmpty() && (after.charAt(0) == '\'' || after.charAt(0) == '"')
                && findClosingQuote(after, 1, after.charAt(0)) < 0) {
            // A quoted (single or double) scalar whose closing quote isn't on this physical line --
            // YAML allows a quoted scalar to wrap across multiple lines (common in real-world CRD/
            // API description fields). Preserved verbatim (opaque, like a block scalar body) rather
            // than reflowed/folded, since exact fold-whitespace semantics aren't needed for a
            // round-trip-preserving formatter and this keeps the fix minimal/idempotent.
            parseMultilineQuotedScalar(item, after, ln.indent);
            return;
        }
        item.trailingComment = parts[1] != null ? normComment(parts[1]) : null;
        if (after.isEmpty()) {
            final Line next = peek();
            if (next != null && !next.isBlank()) {
                final boolean nextIsSeq = next.content.equals("-") || next.content.startsWith("- ");
                // A sequence child is allowed at the same indent as its parent key (a common,
                // valid YAML style); a mapping child must be strictly deeper to avoid ambiguity
                // with the next sibling key at the parent's own indent.
                if (nextIsSeq ? next.indent >= ln.indent : next.indent > ln.indent) {
                    item.children = parseBlock(next.indent);
                }
            }
            return;
        }
        item.inlineValue = after;
        if (looksLikeFlow(after)) {
            return; // rendered via flow parsing/overflow-check at render time
        }
        // A scalar value can still carry a nested block underneath it (an anchor followed by a
        // nested mapping, e.g. "anchor_example: &default" then an indented "color: blue"). But a
        // more-indented following line that doesn't itself look like a mapping key or sequence dash
        // is instead a continuation of an unquoted (plain) scalar that wraps across physical lines
        // (common in real-world CRD/API description fields, e.g. "description: Foo is a bar and\n
        // baz.") -- captured verbatim/opaque, same approach as the quoted-scalar case above.
        if (peek() != null && !peek().isBlank() && peek().indent > ln.indent) {
            final Line next = peek();
            final boolean nextIsSeqLine = next.content.equals("-") || next.content.startsWith("- ");
            final boolean nextIsKeyLine = !nextIsSeqLine && findMappingColon(next.content) >= 0;
            if (nextIsSeqLine || nextIsKeyLine) {
                item.children = parseBlock(next.indent);
            } else {
                parseMultilinePlainScalar(item, ln.indent);
            }
        }
    }

    /** Consumes physical lines (verbatim, raw) following a keyed item's unquoted plain scalar value,
     *  as long as each is more-indented than the key and doesn't itself look like a new mapping key
     *  or sequence dash (which would instead be a genuine nested block, handled by the caller before
     *  reaching here). Stored verbatim/opaque in {@code item.multilineScalarBody}, same rendering and
     *  idempotency rationale as {@link #parseMultilineQuotedScalar}. */
    private void parseMultilinePlainScalar(final Item item, final int keyIndent) {
        final List<String> bodyLines = new ArrayList<>();
        while (peek() != null && !peek().isBlank() && peek().indent > keyIndent) {
            final Line next = peek();
            final boolean isSeqLine = next.content.equals("-") || next.content.startsWith("- ");
            final boolean isKeyLine = !isSeqLine && findMappingColon(next.content) >= 0;
            if (isSeqLine || isKeyLine) {
                break;
            }
            bodyLines.add(repeatChar(' ', next.indent - keyIndent) + next.content);
            pos++;
        }
        if (!bodyLines.isEmpty()) {
            item.multilineScalarBody = String.join("\n", bodyLines);
        }
    }

    /** Returns the index of the unescaped closing {@code quote} character in {@code s} starting the
     *  scan at {@code start}, or -1 if not found on this line/segment. A doubled single-quote
     *  ({@code ''}) inside a single-quoted scalar is YAML's escape for a literal quote (not a
     *  closer); a backslash-escaped char is skipped whole inside a double-quoted scalar. */
    private static int findClosingQuote(final String s, final int start, final char quote) {
        int i = start;
        while (i < s.length()) {
            final char c = s.charAt(i);
            if (quote == '\'') {
                if (c == '\'') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '\'') {
                        i += 2;
                        continue;
                    }
                    return i;
                }
            } else {
                if (c == '\\') {
                    i += 2;
                    continue;
                }
                if (c == '"') {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    /** Consumes physical lines (verbatim, raw) following a keyed item's quoted scalar value until the
     *  scalar's closing quote is found (or input runs out / a blank line is hit -- a conservative
     *  stopping point, since a blank line inside a real quoted scalar continuation is rare in
     *  practice). The captured continuation lines are stored verbatim/opaque in
     *  {@code item.multilineScalarBody} -- not reflowed -- so re-formatting the formatter's own
     *  output is trivially idempotent (the captured lines pass through byte-for-byte). */
    private void parseMultilineQuotedScalar(final Item item, final String firstLineAfter, final int keyIndent) {
        final char quote = firstLineAfter.charAt(0);
        final List<String> bodyLines = new ArrayList<>();
        while (peek() != null && !peek().isBlank()) {
            final Line next = peek();
            pos++;
            bodyLines.add(repeatChar(' ', Math.max(0, next.indent - keyIndent)) + next.content);
            if (findClosingQuote(next.content, 0, quote) >= 0) {
                break;
            }
        }
        item.inlineValue = firstLineAfter;
        item.multilineScalarBody = String.join("\n", bodyLines);
    }

    private void parseSeqItem(final Item item, final Line ln) {
        pos++;
        item.isSeq = true;
        final String rest = ln.content.equals("-") ? "" : ln.content.substring(2);
        final int innerCol = ln.indent + 2;
        if (rest.trim().isEmpty()) {
            if (peek() != null && !peek().isBlank() && peek().indent >= innerCol) {
                item.children = parseBlock(peek().indent);
            }
            return;
        }
        final String[] parts = splitTrailingComment(rest);
        final String code = parts[0];
        final int colon = findMappingColon(code);
        if (colon >= 0) {
            item.seqOfMapping = true;
            final Item firstKey = new Item();
            firstKey.key = code.substring(0, colon).trim();
            final String after = code.substring(colon + 1).trim();
            if (after.startsWith("|") || after.startsWith(">")) {
                firstKey.inlineValue = after;
                firstKey.blockScalarBody = captureBlockScalarBody(innerCol - 2);
            } else if (!after.isEmpty() && (after.charAt(0) == '\'' || after.charAt(0) == '"')
                    && findClosingQuote(after, 1, after.charAt(0)) < 0) {
                // Same multi-line quoted-scalar continuation handling as parseKeyItem, for a
                // sequence-of-mapping's first (inline) key.
                parseMultilineQuotedScalar(firstKey, after, innerCol);
            } else {
                firstKey.trailingComment = parts[1] != null ? normComment(parts[1]) : null;
                if (!after.isEmpty()) {
                    firstKey.inlineValue = after;
                }
                final Line nextLn = peek();
                final boolean nextIsSeqLine = nextLn != null && !nextLn.isBlank()
                        && (nextLn.content.equals("-") || nextLn.content.startsWith("- "));
                final boolean nextIsKeyLine = nextLn != null && !nextLn.isBlank() && !nextIsSeqLine
                        && findMappingColon(nextLn.content) >= 0;
                // A sequence child of firstKey is allowed at the same indent as firstKey itself
                // (innerCol) -- the common "- apiGroups:\n  - \"*\"" k8s manifest style -- same
                // same-indent-sequence-child rule as parseKeyItem's own handling. A mapping child
                // must be strictly deeper than innerCol, and not equal to innerCol (which would
                // instead belong to the siblingKeys block parsed right below).
                if ((after.isEmpty() || !looksLikeFlow(after)) && nextLn != null && !nextLn.isBlank()
                        && (nextIsSeqLine ? nextLn.indent >= innerCol
                                : (nextIsKeyLine && nextLn.indent > innerCol - 2 && nextLn.indent != innerCol))) {
                    firstKey.children = parseBlock(nextLn.indent);
                } else if (!after.isEmpty() && !looksLikeFlow(after) && nextLn != null && !nextLn.isBlank()
                        && !nextIsSeqLine && !nextIsKeyLine && nextLn.indent > innerCol) {
                    // A plain (unquoted) scalar continuation wrapping across physical lines --
                    // same disambiguation as parseKeyItem's own tail handling, applied here for a
                    // sequence-of-mapping's first (inline) key (e.g. "- description: Foo is a bar\n
                    // and baz." under an "additionalPrinterColumns:" style sequence).
                    parseMultilinePlainScalar(firstKey, innerCol);
                }
            }
            final List<Item> siblingKeys = parseBlock(innerCol);
            item.children = new ArrayList<>();
            item.children.add(firstKey);
            item.children.addAll(siblingKeys);
            return;
        }
        if (code.startsWith("|") || code.startsWith(">")) {
            // A block scalar as a plain (non-keyed) sequence item's own value, e.g.
            // "- |\n    line one\n    line two" (a script/command string in a plain YAML
            // sequence) -- same block-scalar handling as the mapping-key and seqOfMapping-
            // firstKey cases above, just without an intervening "key:" prefix.
            item.inlineValue = code;
            item.blockScalarBody = captureBlockScalarBody(innerCol - 2);
            return;
        }
        if (!code.isEmpty() && (code.charAt(0) == '\'' || code.charAt(0) == '"')
                && findClosingQuote(code, 1, code.charAt(0)) < 0) {
            // Same multi-line quoted-scalar continuation handling as the other cases, for a
            // plain (non-keyed) sequence item's own quoted value.
            parseMultilineQuotedScalar(item, code, innerCol);
            return;
        }
        item.inlineValue = code;
        item.trailingComment = parts[1] != null ? normComment(parts[1]) : null;
    }

    /** Captures a {@code |}/{@code >} block scalar's body lines, stored with each line's
     *  indentation as a delta <em>relative to the header key's own original indent</em>
     *  (not absolute/verbatim) -- same reasoning and same idempotency requirement as
     *  {@link #parseMultilineQuotedScalar}/{@link #parseMultilinePlainScalar}: the header key's
     *  rendered column can shift (indent-size changes, colon-alignment padding, nesting-depth
     *  differences elsewhere in the renderer), and an absolute-indent copy would drift out of
     *  sync with it, breaking idempotency (and potentially validity, since the body must stay
     *  more-indented than the header to still parse as part of the block scalar). */
    private String captureBlockScalarBody(final int headerIndent) {
        final List<String> body = new ArrayList<>();
        while (pos < lines.size() && (peek().isBlank() || peek().indent > headerIndent)) {
            final Line next = peek();
            pos++;
            if (next.isBlank()) {
                body.add("");
            } else {
                body.add(repeatChar(' ', Math.max(0, next.indent - headerIndent)) + next.content);
            }
        }
        return String.join("\n", body);
    }

    // ---- Rendering ------------------------------------------------------------------------------

    private void renderItems(final List<Item> items, final int depth, final StringBuilder out) {
        // Colon-alignment groups: a run of adjacent keyed items with no leading comment/blank/
        // frozen-span break between them, same shape as JSON's §1.1 grouping.
        final String[] padding = new String[items.size()];
        int groupStart = -1;
        for (int i = 0; i <= items.size(); i++) {
            final boolean atEnd = i == items.size();
            final boolean breaksBefore = atEnd || !items.get(i).isKeyed()
                    || !items.get(i).leadingComments.isEmpty() || items.get(i).blankBefore;
            if (breaksBefore) {
                if (groupStart >= 0 && i > groupStart) {
                    final List<String> keys = new ArrayList<>();
                    for (int g = groupStart; g < i; g++) {
                        keys.add(items.get(g).key);
                    }
                    final String[] groupPad = FormatterSimpleBraced.padKeysForColonAlignment(keys);
                    for (int g = groupStart; g < i; g++) {
                        padding[g] = groupPad[g - groupStart];
                    }
                }
                groupStart = (!atEnd && items.get(i).isKeyed()) ? i : -1;
            } else if (groupStart < 0) {
                groupStart = i;
            }
        }

        for (int i = 0; i < items.size(); i++) {
            final Item item = items.get(i);
            if (i > 0 && item.blankBefore) {
                out.append('\n');
            }
            for (final String comment : item.leadingComments) {
                out.append(indent(depth)).append(comment).append('\n');
            }
            if (item.dangling) {
                continue;
            }
            if (item.isFrozen) {
                for (final String raw : item.frozenLines) {
                    out.append(raw).append('\n');
                }
                continue;
            }
            if (item.isSeq) {
                renderSeqItem(item, depth, out);
            } else {
                renderKeyItem(item, depth, padding[i], out);
            }
        }
    }

    /** Renders a multi-line scalar's continuation lines: {@code body} holds each continuation line
     *  as its indentation <em>relative to its own key's original indent</em> (not absolute), joined
     *  by {@code \n} -- see {@link #parseMultilinePlainScalar}/{@link #parseMultilineQuotedScalar}.
     *  Rendering re-anchors each line to the key's newly-computed {@code indent(depth)} plus that
     *  preserved relative delta, so the continuation always stays deeper than its key regardless of
     *  indent-size or nesting-depth changes introduced elsewhere by reformatting -- required for
     *  idempotency (a verbatim/absolute-indent copy would drift out of sync with a reindented key
     *  line, e.g. after a global indent-size change or an unrelated nesting-depth fix). */
    private void appendMultilineScalarBody(final String body, final int depth, final StringBuilder out) {
        appendMultilineScalarBody(body, indent(depth), out);
    }

    /** Same as {@link #appendMultilineScalarBody(String, int, StringBuilder)} but anchored at an
     *  explicit prefix string rather than a clean {@code indent(depth)} value -- needed for
     *  contexts (e.g. sequence-of-mapping sibling keys) whose rendered column is an offset like
     *  {@code indent(depth) + "  "} rather than a depth the {@code indent()} helper itself models.
     *  Blank body lines (originally-blank lines inside the block scalar/continuation) are emitted
     *  as truly empty lines, not padded with trailing whitespace. */
    private void appendMultilineScalarBody(final String body, final String keyIndentStr, final StringBuilder out) {
        for (final String line : body.split("\n", -1)) {
            if (line.isEmpty()) {
                out.append('\n');
            } else {
                out.append(keyIndentStr).append(line).append('\n');
            }
        }
    }

    private void renderKeyItem(final Item item, final int depth, final String pad, final StringBuilder out) {
        final String keyPrefix = indent(depth) + item.key + pad + ":";
        if (item.blockScalarBody != null) {
            out.append(keyPrefix).append(' ').append(item.inlineValue).append('\n');
            if (!item.blockScalarBody.isEmpty()) {
                appendMultilineScalarBody(item.blockScalarBody, depth, out);
            }
            return;
        }
        if (item.multilineScalarBody != null) {
            out.append(keyPrefix).append(' ').append(item.inlineValue).append('\n');
            appendMultilineScalarBody(item.multilineScalarBody, depth, out);
            return;
        }
        if (item.inlineValue != null && looksLikeFlow(item.inlineValue)) {
            out.append(keyPrefix);
            renderFlowValue(parseFlow(new FlowCursor(item.inlineValue)), depth, keyPrefix, out);
            return;
        }
        out.append(keyPrefix);
        if (item.inlineValue != null) {
            out.append(' ').append(item.inlineValue);
        }
        if (item.trailingComment != null) {
            out.append(' ').append(item.trailingComment);
        }
        out.append('\n');
        if (item.children != null) {
            renderItems(item.children, depth + 1, out);
        }
    }

    private void renderSeqItem(final Item item, final int depth, final StringBuilder out) {
        if (item.seqOfMapping) {
            renderSeqOfMapping(item, depth, out);
            return;
        }
        final String dashPrefix = indent(depth) + "- ";
        if (item.children != null) {
            out.append(indent(depth)).append('-').append('\n');
            renderItems(item.children, depth + 1, out);
            return;
        }
        if (item.blockScalarBody != null) {
            out.append(dashPrefix).append(item.inlineValue).append('\n');
            if (!item.blockScalarBody.isEmpty()) {
                // Captured relative to the dash line's own indent (see captureBlockScalarBody's
                // caller, which passes ln.indent, i.e. innerCol - 2) -- whose rendered equivalent
                // is indent(depth) itself, NOT a "+2" offset (that offset is only correct for
                // multilineScalarBody below, whose capture baseline is the value's own column,
                // innerCol).
                appendMultilineScalarBody(item.blockScalarBody, depth, out);
            }
            return;
        }
        if (item.multilineScalarBody != null) {
            out.append(dashPrefix).append(item.inlineValue).append('\n');
            appendMultilineScalarBody(item.multilineScalarBody, indent(depth) + "  ", out);
            return;
        }
        if (item.inlineValue != null && looksLikeFlow(item.inlineValue)) {
            out.append(dashPrefix);
            renderFlowValue(parseFlow(new FlowCursor(item.inlineValue)), depth + 1, dashPrefix, out);
            return;
        }
        out.append(dashPrefix).append(item.inlineValue);
        if (item.trailingComment != null) {
            out.append(' ').append(item.trailingComment);
        }
        out.append('\n');
    }

    /** A sequence item that's itself a mapping (`- name: Widget`): the first key stays inline after
     *  the `-`, subsequent keys in the same item-mapping align one column past the `-` and space
     *  (under the first key), per §5.3. */
    private void renderSeqOfMapping(final Item item, final int depth, final StringBuilder out) {
        final List<Item> children = item.children;
        // A dangling item (trailing comment(s)/blank line with no following sibling key at this
        // block level -- see parseBlock's own tail handling) has a null key and isn't a real keyed
        // row: it must be excluded from colon-alignment padding (its null key would otherwise NPE
        // in padKeysForColonAlignment) and rendered as bare comment line(s) instead of a "key:" row.
        final List<String> keys = new ArrayList<>();
        for (final Item c : children) {
            if (!c.dangling) {
                keys.add(c.key);
            }
        }
        final String[] pad = FormatterSimpleBraced.padKeysForColonAlignment(keys);
        final String dashPrefix = indent(depth) + "- ";
        final String alignPrefix = indent(depth) + "  ";
        int padIdx = 0;
        for (int i = 0; i < children.size(); i++) {
            final Item c = children.get(i);
            if (c.dangling) {
                for (final String comment : c.leadingComments) {
                    out.append(alignPrefix).append(comment).append('\n');
                }
                continue;
            }
            for (final String comment : c.leadingComments) {
                out.append(i == 0 ? dashPrefix : alignPrefix).append(comment).append('\n');
            }
            final String prefix = (i == 0 ? dashPrefix : alignPrefix) + c.key + pad[padIdx] + ":";
            padIdx++;
            out.append(prefix);
            if (c.blockScalarBody != null) {
                out.append(' ').append(c.inlineValue).append('\n');
                if (!c.blockScalarBody.isEmpty()) {
                    // The first (inline) key's block scalar is captured relative to the dash
                    // line's own indent (indent(depth)); a sibling key's is captured relative to
                    // its own key line's indent, which equals alignPrefix's column. Different
                    // baseline per position, unlike multilineScalarBody (which uses the value's
                    // own column, alignPrefix, for both).
                    appendMultilineScalarBody(c.blockScalarBody, i == 0 ? indent(depth) : alignPrefix, out);
                }
                continue;
            }
            if (c.multilineScalarBody != null) {
                out.append(' ').append(c.inlineValue).append('\n');
                appendMultilineScalarBody(c.multilineScalarBody, alignPrefix, out);
                continue;
            }
            if (c.inlineValue != null && looksLikeFlow(c.inlineValue)) {
                renderFlowValue(parseFlow(new FlowCursor(c.inlineValue)), depth + 1, prefix, out);
                continue;
            }
            if (c.inlineValue != null) {
                out.append(' ').append(c.inlineValue);
            }
            if (c.trailingComment != null) {
                out.append(' ').append(c.trailingComment);
            }
            out.append('\n');
            if (c.children != null) {
                renderItems(c.children, depth + 2, out);
            }
        }
    }

    private void renderDocument(final List<String> docRawLines, final StringBuilder out) {
        lines = new ArrayList<>();
        for (final String raw : docRawLines) {
            lines.add(new Line(raw));
        }
        pos = 0;
        if (lines.isEmpty()) {
            return;
        }
        final int blockIndent = lines.get(0).indent;
        final List<Item> items = parseBlock(blockIndent);
        renderItems(items, 0, out);
    }

    /** Line-splits, parses, and re-renders {@code content} per STYLE_DATA_FORMATS.md §5. `---`
     *  document separators and `...` end markers are preserved as written and reset structural
     *  depth for whatever follows, per §5.7. */
    public String format(final String content) {
        final boolean endsWithNewline = content.endsWith("\n");
        final String[] rawLines = content.split("\n", -1);
        final List<String> allLines = new ArrayList<>(Arrays.asList(rawLines));
        if (endsWithNewline && !allLines.isEmpty()) {
            allLines.remove(allLines.size() - 1);
        }
        final StringBuilder out = new StringBuilder();
        final List<String> docLines = new ArrayList<>();
        for (final String raw : allLines) {
            final String trimmed = raw.trim();
            if (trimmed.equals("---") || trimmed.equals("...")) {
                if (!docLines.isEmpty()) {
                    renderDocument(docLines, out);
                    docLines.clear();
                }
                out.append(raw).append('\n');
                continue;
            }
            docLines.add(raw);
        }
        if (!docLines.isEmpty()) {
            renderDocument(docLines, out);
        }
        return out.toString();
    }
}
