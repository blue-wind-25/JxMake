/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;

import com.jxmake.formatter.Lang;

/**
 * Real formatting logic for XML (STYLE_DATA_FORMATS.md §2), shared with HTML5 (gate internally on
 * {@code lang.isHtml5} for HTML5-only additions -- void elements, the `<script>`/`<style>`
 * embedded-content dispatcher -- neither implemented yet), per RDD_KEY_188. A from-scratch
 * character-cursor recursive-descent parser -- XML's tag/attribute grammar has no natural line
 * boundary the way YAML's indentation-significant grammar does, so (unlike {@link YamlSpecificRule})
 * this is neither line-based nor a reuse of {@code TokenizerCore}/{@code Token} (brace-delimited,
 * imperative-language shaped -- not a fit for tag nesting). Implements its own {@code <!--%}-based
 * {@code JXM_CFMT_DIS}/{@code ENA} frozen-span detection (line-anchored, mirroring
 * {@link YamlSpecificRule}/{@link TomlSpecificRule}'s independent implementations) and its own
 * comment-start-case normalization (comment inner text has no leading delimiter char to skip, unlike
 * {@code #}/{@code //}, so it is simpler than either sibling's {@code normComment}).
 */
public final class XmlSpecificRule {

    public static final class XmlParseException extends RuntimeException {
        public XmlParseException(final String message) {
            super(message);
        }
    }

    private enum NodeType { PI, DOCTYPE, COMMENT, ELEMENT, TEXT, CDATA, FROZEN, RAW }

    /** HTML5 void elements (never a closing tag; any self-closing `/` is normalized away),
     *  per STYLE_DATA_FORMATS.md §4.1. */
    private static final java.util.Set<String> VOID_ELEMENTS = new java.util.HashSet<>(java.util.Arrays.asList(
            "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param",
            "source", "track", "wbr"));

    /** `<script>` MIME types that mean "this is JavaScript" per HTML5 semantics -- anything else
     *  (or a recognized non-executable type such as `application/json`) is left fully opaque. */
    private static final java.util.Set<String> JS_SCRIPT_TYPES = new java.util.HashSet<>(java.util.Arrays.asList(
            "text/javascript", "application/javascript", "application/ecmascript",
            "text/ecmascript", "module"));

    private static final class Node {
        NodeType type;
        String raw;              // PI / DOCTYPE / CDATA / TEXT: verbatim content
        String commentText;      // COMMENT: normalized inner text
        List<String> frozenLines; // FROZEN: raw lines, verbatim, DIS..ENA inclusive
        String tagName;
        List<String> attrs = new ArrayList<>();
        boolean selfClosing;
        List<Node> children;     // null if self-closing; empty list if open/close-with-nothing
        String trailingComment;  // normalized text of a same-line trailing comment, or null
    }

    private final Lang lang;
    private final int lineLengthLimit;
    private final int indentWidth;
    private final boolean useTabs;
    private final boolean normalizeCommentStartCase;

    private String s;
    private int pos;

    public XmlSpecificRule(final Lang lang) {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public XmlSpecificRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public XmlSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this(lang, lineLengthLimit, indentWidth, "tabs", true);
    }

    public XmlSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth,
            final String indentStyle, final boolean normalizeCommentStartCase) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
        this.indentWidth = indentWidth;
        this.useTabs = "tabs".equals(indentStyle);
        this.normalizeCommentStartCase = normalizeCommentStartCase;
    }

    private String indent(final int depth) {
        if (useTabs) {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                sb.append('\t');
            }
            return sb.toString();
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth * indentWidth; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    // ---- cursor helpers ----

    private boolean eof() {
        return pos >= s.length();
    }

    private boolean startsWith(final String tok) {
        return s.regionMatches(pos, tok, 0, tok.length());
    }

    private void skipWs() {
        while (!eof() && Character.isWhitespace(s.charAt(pos))) {
            pos++;
        }
    }

    private void skipInlineWs() {
        while (!eof() && (s.charAt(pos) == ' ' || s.charAt(pos) == '\t')) {
            pos++;
        }
    }

    private String currentLineTrimmed() {
        int end = s.indexOf('\n', pos);
        if (end < 0) {
            end = s.length();
        }
        return s.substring(pos, end).trim();
    }

    // ---- top-level ----

    public String format(final String content) {
        this.s = content;
        this.pos = 0;
        final List<Node> nodes = parseNodes(false);
        if (!eof()) {
            throw new XmlParseException("trailing content after document, near: "
                    + s.substring(pos, Math.min(s.length(), pos + 40)));
        }
        final StringBuilder out = new StringBuilder();
        renderNodes(nodes, 0, out);
        return out.toString();
    }

    private List<Node> parseNodes(final boolean stopAtCloseTag) {
        final List<Node> nodes = new ArrayList<>();
        while (true) {
            skipWs();
            if (eof()) {
                break;
            }
            if (stopAtCloseTag && startsWith("</")) {
                break;
            }
            final Node node = parseSingleNode();
            attachTrailingCommentIfAny(node);
            nodes.add(node);
        }
        return nodes;
    }

    private void attachTrailingCommentIfAny(final Node node) {
        final int save = pos;
        skipInlineWs();
        if (startsWith("<!--")) {
            final int close = s.indexOf("-->", pos + 4);
            if (close >= 0 && s.indexOf('\n', pos) > close) {
                final String inner = s.substring(pos + 4, close).trim();
                if (!inner.startsWith("%")) {
                    node.trailingComment = normComment(inner);
                    pos = close + 3;
                    return;
                }
            }
        }
        pos = save;
    }

    private Node parseSingleNode() {
        if (startsWith("<?")) {
            return parsePi();
        }
        if (startsWith("<!--")) {
            return parseCommentOrFrozen();
        }
        if (startsWith("<!DOCTYPE") || startsWith("<!doctype")) {
            return parseDoctype();
        }
        if (startsWith("<![CDATA[")) {
            return parseCdata();
        }
        if (startsWith("</")) {
            throw new XmlParseException("unexpected closing tag near: "
                    + s.substring(pos, Math.min(s.length(), pos + 40)));
        }
        if (startsWith("<")) {
            return parseElement();
        }
        return parseText();
    }

    private Node parsePi() {
        final int close = s.indexOf("?>", pos + 2);
        if (close < 0) {
            throw new XmlParseException("unterminated processing instruction");
        }
        final Node n = new Node();
        n.type = NodeType.PI;
        n.raw = s.substring(pos, close + 2);
        pos = close + 2;
        return n;
    }

    private Node parseCommentOrFrozen() {
        if ("<!--% JXM_CFMT_DIS -->".equals(currentLineTrimmed())) {
            final Node n = new Node();
            n.type = NodeType.FROZEN;
            n.frozenLines = new ArrayList<>();
            while (true) {
                int end = s.indexOf('\n', pos);
                final boolean hasNl = end >= 0;
                if (!hasNl) {
                    end = s.length();
                }
                final String line = s.substring(pos, end);
                n.frozenLines.add(line);
                pos = hasNl ? end + 1 : end;
                if ("<!--% JXM_CFMT_ENA -->".equals(line.trim()) || eof()) {
                    break;
                }
            }
            return n;
        }
        final int close = s.indexOf("-->", pos + 4);
        if (close < 0) {
            throw new XmlParseException("unterminated comment");
        }
        final Node n = new Node();
        n.type = NodeType.COMMENT;
        n.commentText = normComment(s.substring(pos + 4, close).trim());
        pos = close + 3;
        return n;
    }

    private Node parseDoctype() {
        final int start = pos;
        int depth = 0;
        while (!eof()) {
            final char c = s.charAt(pos);
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
                pos++;
                if (depth == 0) {
                    break;
                }
                continue;
            }
            pos++;
        }
        final Node n = new Node();
        n.type = NodeType.DOCTYPE;
        n.raw = s.substring(start, pos);
        return n;
    }

    private Node parseCdata() {
        final int close = s.indexOf("]]>", pos + 9);
        if (close < 0) {
            throw new XmlParseException("unterminated CDATA section");
        }
        final Node n = new Node();
        n.type = NodeType.CDATA;
        n.raw = s.substring(pos, close + 3);
        pos = close + 3;
        return n;
    }

    private Node parseText() {
        final int start = pos;
        int end = s.indexOf('<', pos);
        if (end < 0) {
            end = s.length();
        }
        pos = end;
        final Node n = new Node();
        n.type = NodeType.TEXT;
        n.raw = s.substring(start, end).trim();
        return n;
    }

    private Node parseElement() {
        pos++; // '<'
        final int nameStart = pos;
        while (!eof() && !Character.isWhitespace(s.charAt(pos)) && s.charAt(pos) != '/' && s.charAt(pos) != '>') {
            pos++;
        }
        final Node n = new Node();
        n.type = NodeType.ELEMENT;
        n.tagName = s.substring(nameStart, pos);
        final String lowerTag = n.tagName.toLowerCase(java.util.Locale.ROOT);
        final boolean isVoid = lang.isHtml5 && VOID_ELEMENTS.contains(lowerTag);
        while (true) {
            skipWs();
            if (eof()) {
                throw new XmlParseException("unterminated tag <" + n.tagName);
            }
            if (startsWith("/>")) {
                n.selfClosing = true;
                pos += 2;
                break;
            }
            if (startsWith(">")) {
                pos += 1;
                break;
            }
            n.attrs.add(parseAttr());
        }
        if (isVoid) {
            n.selfClosing = true;
            return n;
        }
        if (n.selfClosing) {
            return n;
        }
        if (lang.isHtml5 && ("script".equals(lowerTag) || "style".equals(lowerTag))) {
            return finishRawTextElement(n, lowerTag);
        }
        if (lang.isHtml5 && "pre".equals(lowerTag)) {
            return finishRawElement(n, "</pre>");
        }
        n.children = parseNodes(true);
        final String closeTok = "</" + n.tagName + ">";
        skipInlineWs();
        if (!startsWith(closeTok) && !startsWith("</" + n.tagName + " ")) {
            throw new XmlParseException("expected closing tag " + closeTok + " near: "
                    + s.substring(pos, Math.min(s.length(), pos + 40)));
        }
        final int gt = s.indexOf('>', pos);
        pos = gt + 1;
        return n;
    }

    /** `<pre>` content is opaque like CDATA (RDD_KEY_185) -- capture verbatim through the literal
     *  closing tag, no reindentation, byte-for-byte. */
    private Node finishRawElement(final Node n, final String closeTagLower) {
        final int close = indexOfIgnoreCase(s, closeTagLower, pos);
        if (close < 0) {
            throw new XmlParseException("expected closing tag " + closeTagLower);
        }
        n.raw = s.substring(pos, close);
        pos = close + closeTagLower.length();
        n.children = null;
        n.type = NodeType.RAW;
        return n;
    }

    /** `<script>`/`<style>` are HTML5 raw-text elements: content runs verbatim up to the literal
     *  closing tag, never tag-parsed (a `<`/`&` inside JS/CSS source must not confuse the parser). */
    private Node finishRawTextElement(final Node n, final String lowerTag) {
        final String closeTagLower = "</" + lowerTag + ">";
        final int close = indexOfIgnoreCase(s, closeTagLower, pos);
        if (close < 0) {
            throw new XmlParseException("expected closing tag " + closeTagLower);
        }
        n.raw = s.substring(pos, close);
        pos = close + closeTagLower.length();
        n.children = null;
        return n;
    }

    private static int indexOfIgnoreCase(final String haystack, final String needleLower, final int from) {
        final String lower = haystack.toLowerCase(java.util.Locale.ROOT);
        return lower.indexOf(needleLower, from);
    }

    private String parseAttr() {
        final int nameStart = pos;
        while (!eof() && s.charAt(pos) != '=' && !Character.isWhitespace(s.charAt(pos))
                && s.charAt(pos) != '/' && s.charAt(pos) != '>') {
            pos++;
        }
        final String name = s.substring(nameStart, pos);
        skipWs();
        if (eof() || s.charAt(pos) != '=') {
            if (lang.isHtml5) {
                // HTML5 bare boolean attribute (e.g. `checked`, `disabled`) -- no `=value` at all.
                return name;
            }
            throw new XmlParseException("expected '=' after attribute '" + name + "'");
        }
        pos++;
        skipWs();
        if (eof() || (s.charAt(pos) != '"' && s.charAt(pos) != '\'')) {
            throw new XmlParseException("expected quoted value for attribute '" + name + "'");
        }
        final char quote = s.charAt(pos);
        final int valStart = pos;
        pos++;
        while (!eof() && s.charAt(pos) != quote) {
            pos++;
        }
        if (eof()) {
            throw new XmlParseException("unterminated attribute value for '" + name + "'");
        }
        pos++;
        return name + "=" + s.substring(valStart, pos);
    }

    // ---- comment normalization ----

    private String normComment(final String text) {
        if (!normalizeCommentStartCase || text.isEmpty()) {
            return text;
        }
        int i = 0;
        while (i < text.length() && text.charAt(i) == ' ') {
            i++;
        }
        if (i >= text.length() || !Character.isLowerCase(text.charAt(i))) {
            return text;
        }
        return text.substring(0, i) + Character.toUpperCase(text.charAt(i)) + text.substring(i + 1);
    }

    // ---- rendering ----

    private void renderNodes(final List<Node> nodes, final int depth, final StringBuilder out) {
        for (final Node n : nodes) {
            renderNode(n, depth, out);
        }
    }

    private void renderNode(final Node n, final int depth, final StringBuilder out) {
        switch (n.type) {
            case PI:
                out.append(indent(depth)).append(n.raw).append('\n');
                return;
            case DOCTYPE:
                out.append(indent(depth)).append(n.raw).append('\n');
                return;
            case COMMENT:
                out.append(indent(depth)).append("<!-- ").append(n.commentText).append(" -->\n");
                return;
            case CDATA:
                out.append(indent(depth)).append(n.raw).append('\n');
                return;
            case TEXT:
                out.append(indent(depth)).append(n.raw).append('\n');
                return;
            case FROZEN:
                for (final String line : n.frozenLines) {
                    out.append(line).append('\n');
                }
                return;
            case RAW:
                out.append(indent(depth)).append('<').append(n.tagName).append(attrsInline(n.attrs))
                        .append('>').append(n.raw).append("</").append(n.tagName).append(">\n");
                return;
            case ELEMENT:
                renderElement(n, depth, out);
                return;
            default:
                throw new IllegalStateException("unhandled node type: " + n.type);
        }
    }

    private void renderElement(final Node n, final int depth, final StringBuilder out) {
        if (lang.isHtml5 && n.raw != null
                && ("script".equalsIgnoreCase(n.tagName) || "style".equalsIgnoreCase(n.tagName))) {
            renderScriptOrStyle(n, depth, out);
            return;
        }
        final String openTightNoAngle = "<" + n.tagName + attrsInline(n.attrs);
        if (n.selfClosing) {
            final boolean isVoid = lang.isHtml5
                    && VOID_ELEMENTS.contains(n.tagName.toLowerCase(java.util.Locale.ROOT));
            final String close = isVoid ? ">" : "/>";
            final String tightLine = indent(depth) + openTightNoAngle + close;
            if (tightLine.length() <= lineLengthLimit || n.attrs.isEmpty()) {
                appendWithTrailing(out, tightLine, n.trailingComment);
            } else {
                out.append(indent(depth)).append('<').append(n.tagName).append('\n');
                for (int i = 0; i < n.attrs.size(); i++) {
                    out.append(indent(depth + 1)).append(n.attrs.get(i));
                    out.append(i == n.attrs.size() - 1 ? close + "\n" : "\n");
                }
                if (n.trailingComment != null) {
                    out.setLength(out.length() - 1);
                    out.append(" <!-- ").append(n.trailingComment).append(" -->\n");
                }
            }
            return;
        }
        final Node onlyChild = soleContentChild(n.children);
        if (onlyChild != null && (onlyChild.type == NodeType.TEXT || onlyChild.type == NodeType.CDATA)) {
            final String inline = indent(depth) + openTightNoAngle + ">" + onlyChild.raw + "</" + n.tagName + ">";
            appendWithTrailing(out, inline, n.trailingComment);
            return;
        }
        final String tightOpenLine = indent(depth) + openTightNoAngle + ">";
        final boolean fits = tightOpenLine.length() <= lineLengthLimit || n.attrs.isEmpty();
        if (n.children.isEmpty()) {
            if (fits) {
                appendWithTrailing(out, tightOpenLine + "</" + n.tagName + ">", n.trailingComment);
            } else {
                appendWrappedOpenTag(n, depth, out);
                appendWithTrailing(out, indent(depth) + "</" + n.tagName + ">", n.trailingComment);
            }
            return;
        }
        if (fits) {
            out.append(tightOpenLine).append('\n');
        } else {
            appendWrappedOpenTag(n, depth, out);
        }
        renderNodes(n.children, depth + 1, out);
        out.append(indent(depth)).append("</").append(n.tagName).append('>').append('\n');
    }

    /** HTML5 §4.2: `<style>` content splices out to the CSS formatter and back, reindented one
     *  level deeper; `<script>` content splices out to the JS/TS formatter, except a non-JS/TS
     *  `type` (e.g. `application/json`) stays fully opaque. Since JS/TS support is still
     *  scaffold-only (STATE_JS_TS.md), real (non-frozen) JS content currently throws -- callers
     *  must wrap it in a `//% JXM_CFMT_DIS`/`ENA` pair to keep it opaque until JS/TS lands, at
     *  which point this method's scaffold-only guard should be replaced with a real dispatch call
     *  and every fixture's directive pair removed/re-verified (see STATE_JS_TS.md/
     *  STATE_DATA_FORMATS.md). */
    private void renderScriptOrStyle(final Node n, final int depth, final StringBuilder out) {
        final String openTag = indent(depth) + "<" + n.tagName + attrsInline(n.attrs) + ">";
        if ("style".equalsIgnoreCase(n.tagName)) {
            final CssSpecificRule css = new CssSpecificRule(lang, lineLengthLimit, indentWidth,
                    useTabs ? "tabs" : "spaces", normalizeCommentStartCase);
            out.append(openTag).append('\n');
            out.append(reindent(css.format(n.raw.trim()), depth + 1));
            out.append(indent(depth)).append("</").append(n.tagName).append(">\n");
            return;
        }
        final String type = findAttrValue(n.attrs, "type");
        final boolean isJsType = type == null || JS_SCRIPT_TYPES.contains(type.toLowerCase(java.util.Locale.ROOT));
        if (!isJsType || isFrozenScriptContent(n.raw)) {
            out.append(openTag).append(n.raw).append("</").append(n.tagName).append(">\n");
            return;
        }
        throw new XmlParseException("<" + n.tagName + "> content requires JS/TS formatting, which is "
                + "not yet implemented (STATE_JS_TS.md) -- wrap it in a `//% JXM_CFMT_DIS`/`//% "
                + "JXM_CFMT_ENA` pair to keep it opaque until JS/TS support lands");
    }

    /** Whether `raw` (a `<script>` element's inner content, possibly `<![CDATA[ ]]>`-wrapped)
     *  contains a `//% JXM_CFMT_DIS` / `//% JXM_CFMT_ENA` line pair -- the temporary scaffold-only
     *  escape hatch for real JS content until JS/TS formatting lands. */
    private boolean isFrozenScriptContent(final String raw) {
        boolean sawDis = false;
        for (final String line : raw.split("\n", -1)) {
            final String t = line.trim();
            if ("//% JXM_CFMT_DIS".equals(t)) {
                sawDis = true;
            } else if (sawDis && "//% JXM_CFMT_ENA".equals(t)) {
                return true;
            }
        }
        return false;
    }

    private String findAttrValue(final List<String> attrs, final String name) {
        for (final String a : attrs) {
            final int eq = a.indexOf('=');
            if (eq < 0) {
                continue;
            }
            if (!a.substring(0, eq).equalsIgnoreCase(name)) {
                continue;
            }
            String v = a.substring(eq + 1);
            if (v.length() >= 2 && (v.charAt(0) == '"' || v.charAt(0) == '\'')) {
                v = v.substring(1, v.length() - 1);
            }
            return v;
        }
        return null;
    }

    /** Prefixes every non-empty line of already-formatted `text` with `depth` levels of
     *  indentation, for splicing an embedded sub-formatter's (CSS's) output back into HTML at its
     *  correct nesting depth. */
    private String reindent(final String text, final int depth) {
        final String prefix = indent(depth);
        final String[] lines = text.split("\n", -1);
        int count = lines.length;
        if (count > 0 && lines[count - 1].isEmpty()) {
            count--; // drop the single trailing empty element from text's final newline
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            final String line = lines[i];
            if (!line.isEmpty()) {
                sb.append(prefix).append(line);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private void appendWrappedOpenTag(final Node n, final int depth, final StringBuilder out) {
        out.append(indent(depth)).append('<').append(n.tagName).append('\n');
        for (int i = 0; i < n.attrs.size(); i++) {
            out.append(indent(depth + 1)).append(n.attrs.get(i));
            out.append(i == n.attrs.size() - 1 ? ">\n" : "\n");
        }
    }

    private void appendWithTrailing(final StringBuilder out, final String line, final String trailingComment) {
        out.append(line);
        if (trailingComment != null) {
            out.append(" <!-- ").append(trailingComment).append(" -->");
        }
        out.append('\n');
    }

    private Node soleContentChild(final List<Node> children) {
        if (children.size() != 1) {
            return null;
        }
        final Node only = children.get(0);
        return (only.type == NodeType.TEXT || only.type == NodeType.CDATA) ? only : null;
    }

    private String attrsInline(final List<String> attrs) {
        if (attrs.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (final String a : attrs) {
            sb.append(' ').append(a);
        }
        return sb.toString();
    }
}
