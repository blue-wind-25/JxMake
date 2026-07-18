/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.FormatterSimpleBraced;
import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.JsonTokenizer;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON/JSON5 STYLE_DATA_FORMATS.md §1 rule logic -- shared between the two (gated internally on
 * {@code lang.isJson5} for JSON5-only additions such as comments/trailing commas/unquoted keys),
 * per RDD_KEY_189. Not curly/indent/tag-based; extends nothing tokenizer-wise itself, but drives
 * {@link JsonTokenizer} and is a sibling of the future CSS rule class under the "SimpleBraced"
 * family (RDD_KEY_190) -- see {@link com.jxmake.formatter.FormatterSimpleBraced}.
 *
 * <p>Implements §1.1 (colon alignment within a group, broken by blank lines/comments/nesting-depth
 * changes), §1.2 (tight atoms-only arrays vs. loose arrays containing objects/arrays or overflowing
 * the line-length limit), and §1.3 (JSON5 backslash-newline string continuations are opaque --
 * already guaranteed by {@link JsonTokenizer} emitting the whole string as one token).
 */
public final class JsonSpecificRule {

    private final Lang lang;
    private final int lineLengthLimit;
    private final int indentWidth;
    private final String indentUnit;

    public JsonSpecificRule(final Lang lang) {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public JsonSpecificRule(final Lang lang, final int lineLengthLimit) {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public JsonSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth) {
        this(lang, lineLengthLimit, indentWidth, "spaces");
    }

    public JsonSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth,
            final String indentStyle) {
        this.lang = lang;
        this.lineLengthLimit = lineLengthLimit;
        this.indentWidth = indentWidth;
        this.indentUnit = "tabs".equals(indentStyle) ? "\t" : repeatChar(' ', Math.max(0, indentWidth));
    }

    /** Malformed JSON/JSON5 input that the parser cannot make sense of -- caught generically by
     *  {@code Main}'s per-file error handling, same as any other rule class's runtime failure. */
    public static final class JsonParseException extends RuntimeException {
        public JsonParseException(final String message) {
            super(message);
        }
    }

    // ---- AST ---------------------------------------------------------------------------------

    private abstract static class Node {
    }

    private static final class Scalar extends Node {
        final String raw;

        Scalar(final String raw) {
            this.raw = raw;
        }
    }

    /** One member (object) or element (array). {@code key} is null for array elements. */
    private static final class Item {
        List<String> leadingComments = new ArrayList<>();
        boolean blankBefore;
        String key;
        Node value;
        String trailingComment;
        boolean hasComma;
    }

    private static final class Container extends Node {
        final boolean isObject;
        final List<Item> items = new ArrayList<>();

        Container(final boolean isObject) {
            this.isObject = isObject;
        }
    }

    // ---- Parsing ------------------------------------------------------------------------------

    private static final class Cursor {
        final List<Token> toks;
        int i;

        Cursor(final List<Token> toks) {
            this.toks = toks;
        }

        Token cur() {
            return i < toks.size() ? toks.get(i) : null;
        }
    }

    private static boolean isSignificant(final Token t) {
        return t != null && t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE
                && t.type != TokenType.COMMENT_LINE && t.type != TokenType.COMMENT_BLOCK;
    }

    /** Consumes whitespace/newlines/comments up to the next significant token, recording comment
     *  text (in order) and whether a blank line (2+ consecutive newlines) occurred anywhere in the
     *  span -- both a group-break signal per §1.1. */
    private void collectTrivia(final Cursor c, final List<String> comments, final boolean[] blankOut) {
        int newlines = 0;
        while (c.cur() != null) {
            final Token t = c.cur();
            if (t.type == TokenType.WHITESPACE) {
                c.i++;
                continue;
            }
            if (t.type == TokenType.NEWLINE) {
                newlines++;
                if (newlines > 1) {
                    blankOut[0] = true;
                }
                c.i++;
                continue;
            }
            if (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                comments.add(t.text);
                newlines = 0;
                c.i++;
                continue;
            }
            break;
        }
    }

    /** Same-line trailing comment right after a value/comma -- only consumed if no newline is
     *  crossed first. */
    private String collectTrailingComment(final Cursor c) {
        final int save = c.i;
        while (c.cur() != null && c.cur().type == TokenType.WHITESPACE) {
            c.i++;
        }
        final Token t = c.cur();
        if (t != null && (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK)) {
            c.i++;
            return t.text;
        }
        c.i = save;
        return null;
    }

    private Node parseValue(final Cursor c) {
        final Token t = c.cur();
        if (t == null) {
            throw new JsonParseException("unexpected end of input");
        }
        if (Token.isPunct(t, "{")) {
            return parseContainer(c, true);
        }
        if (Token.isPunct(t, "[")) {
            return parseContainer(c, false);
        }
        if (t.type == TokenType.STRING || t.type == TokenType.NUMBER || t.type == TokenType.IDENTIFIER) {
            c.i++;
            return new Scalar(t.text);
        }
        if (t.type == TokenType.OP && ("+".equals(t.text) || "-".equals(t.text))) {
            final Token next = c.i + 1 < c.toks.size() ? c.toks.get(c.i + 1) : null;
            if (next != null && (next.type == TokenType.NUMBER || next.type == TokenType.IDENTIFIER)) {
                c.i += 2;
                return new Scalar(t.text + next.text);
            }
        }
        throw new JsonParseException("unexpected token: " + t.text);
    }

    private Container parseContainer(final Cursor c, final boolean isObject) {
        c.i++; // consume '{' or '['
        final Container container = new Container(isObject);
        final String closer = isObject ? "}" : "]";
        while (true) {
            final Item item = new Item();
            final boolean[] blank = {false};
            collectTrivia(c, item.leadingComments, blank);
            item.blankBefore = blank[0];

            final Token t = c.cur();
            if (t == null) {
                throw new JsonParseException("unterminated " + (isObject ? "object" : "array"));
            }
            if (Token.isPunct(t, closer)) {
                c.i++;
                if (!item.leadingComments.isEmpty() || item.blankBefore) {
                    item.value = null;
                    container.items.add(item);
                }
                break;
            }

            if (isObject) {
                if (t.type != TokenType.STRING && t.type != TokenType.IDENTIFIER) {
                    throw new JsonParseException("expected object key, got: " + t.text);
                }
                item.key = t.text;
                c.i++;
                while (c.cur() != null && (c.cur().type == TokenType.WHITESPACE
                        || c.cur().type == TokenType.NEWLINE)) {
                    c.i++;
                }
                if (!Token.isPunct(c.cur(), ":")) {
                    throw new JsonParseException("expected ':' after key " + item.key);
                }
                c.i++;
                while (c.cur() != null && (c.cur().type == TokenType.WHITESPACE
                        || c.cur().type == TokenType.NEWLINE)) {
                    c.i++;
                }
            }

            item.value = parseValue(c);

            while (c.cur() != null && c.cur().type == TokenType.WHITESPACE) {
                c.i++;
            }
            if (Token.isPunct(c.cur(), ",")) {
                item.hasComma = true;
                c.i++;
            }
            item.trailingComment = collectTrailingComment(c);
            container.items.add(item);
        }
        return container;
    }

    // ---- Rendering ------------------------------------------------------------------------------

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

    /** True if {@code node} is a scalar, or a container with no members/elements at all -- these
     *  never force their containing array loose. */
    private boolean containsContainer(final Node node) {
        return node instanceof Container;
    }

    private void render(final Node node, final int depth, final StringBuilder out) {
        if (node instanceof Scalar) {
            out.append(((Scalar) node).raw);
            return;
        }
        final Container c = (Container) node;
        if (c.items.isEmpty()) {
            out.append(c.isObject ? "{}" : "[]");
            return;
        }
        if (c.isObject) {
            renderObject(c, depth, out);
        } else {
            renderArray(c, depth, out);
        }
    }

    private void renderObject(final Container c, final int depth, final StringBuilder out) {
        out.append('{').append('\n');
        renderItems(c.items, depth + 1, out, true);
        out.append(indent(depth)).append('}');
    }

    /** §1.2: an array stays tight (single line) only if every element is a scalar (no nested
     *  object/array) and no element carries a comment, and the tight rendering fits within the
     *  configured line-length limit at this nesting depth. */
    private void renderArray(final Container c, final int depth, final StringBuilder out) {
        boolean canBeTight = true;
        for (final Item item : c.items) {
            if (item.value == null || containsContainer(item.value) || !item.leadingComments.isEmpty()
                    || item.trailingComment != null || item.blankBefore) {
                canBeTight = false;
                break;
            }
        }
        if (canBeTight) {
            final StringBuilder tight = new StringBuilder("[");
            for (int i = 0; i < c.items.size(); i++) {
                if (i > 0) {
                    tight.append(", ");
                }
                render(c.items.get(i).value, depth + 1, tight);
            }
            tight.append(']');
            if (indent(depth).length() + tight.length() <= lineLengthLimit) {
                out.append(tight);
                return;
            }
        }
        out.append('[').append('\n');
        renderItems(c.items, depth + 1, out, false);
        out.append(indent(depth)).append(']');
    }

    private void renderItems(final List<Item> items, final int depth, final StringBuilder out,
            final boolean isObject) {
        // Compute colon-alignment groups (object only): a run of consecutive real items (not
        // dangling trailing-comment placeholders) with no leading comment/blank line between them.
        final List<String> keys = isObject ? new ArrayList<>() : null;
        String[] padding = null;
        if (isObject) {
            padding = new String[items.size()];
            int groupStart = -1; // -1 = no open group
            for (int i = 0; i <= items.size(); i++) {
                final boolean atEnd = i == items.size();
                final boolean isDangling = !atEnd && items.get(i).value == null;
                // A group boundary falls *before* item i whenever it carries a leading
                // comment/blank line (§1.1) or is a dangling trailing-comment placeholder (no
                // key at all) -- the item itself (if it has a key) still starts a fresh group
                // rather than being dropped from alignment entirely.
                final boolean breaksBefore = atEnd || isDangling
                        || !items.get(i).leadingComments.isEmpty() || items.get(i).blankBefore;
                if (breaksBefore) {
                    if (groupStart >= 0 && i > groupStart) {
                        keys.clear();
                        for (int g = groupStart; g < i; g++) {
                            keys.add(items.get(g).key);
                        }
                        final String[] groupPad = FormatterSimpleBraced.padKeysForColonAlignment(keys);
                        for (int g = groupStart; g < i; g++) {
                            padding[g] = groupPad[g - groupStart];
                        }
                    }
                    groupStart = (!atEnd && !isDangling) ? i : -1;
                } else if (groupStart < 0) {
                    groupStart = i;
                }
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
            if (item.value == null) {
                continue; // dangling trailing-comment placeholder, no value to render
            }
            out.append(indent(depth));
            if (isObject) {
                out.append(item.key).append(padding[i]).append(':').append(' ');
            }
            render(item.value, depth, out);
            if (item.hasComma) {
                out.append(',');
            }
            if (item.trailingComment != null) {
                out.append(' ').append(item.trailingComment);
            }
            out.append('\n');
        }
    }

    /** Tokenizes, parses, and re-renders {@code content} per STYLE_DATA_FORMATS.md §1. */
    public String format(final String content) {
        final List<Token> tokens = new JsonTokenizer().tokenize(content);
        final Cursor c = new Cursor(tokens);
        final List<String> leadingComments = new ArrayList<>();
        final boolean[] blank = {false};
        collectTrivia(c, leadingComments, blank);
        final Node root = parseValue(c);
        final List<String> trailingComments = new ArrayList<>();
        collectTrivia(c, trailingComments, new boolean[1]);
        if (c.cur() != null) {
            throw new JsonParseException("unexpected trailing content: " + c.cur().text);
        }

        final StringBuilder out = new StringBuilder();
        for (final String comment : leadingComments) {
            out.append(comment).append('\n');
        }
        render(root, 0, out);
        out.append('\n');
        for (final String comment : trailingComments) {
            out.append(comment).append('\n');
        }
        return out.toString();
    }
}
