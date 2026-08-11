/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;

import com.jxmake.formatter.FormatterSimpleBraced;
import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.JsonTokenizer;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

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

    private final Lang    lang;
    private final int     lineLengthLimit;
    private final int     indentWidth;
    private final String  indentUnit;
    private final boolean normalizeCommentStartCase;
    private final boolean normalizeCommentEndPeriod;

    public JsonSpecificRule(final Lang lang)
    {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public JsonSpecificRule(final Lang lang, final int lineLengthLimit)
    {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public JsonSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth)
    {
        this(lang, lineLengthLimit, indentWidth, "spaces");
    }

    public JsonSpecificRule(
        final Lang   lang,
        final int    lineLengthLimit,
        final int    indentWidth,
        final String indentStyle
    )
    {
        this(lang, lineLengthLimit, indentWidth, indentStyle, true);
    }

    public JsonSpecificRule(
        final Lang    lang,
        final int     lineLengthLimit,
        final int     indentWidth,
        final String  indentStyle,
        final boolean normalizeCommentStartCase
    )
    {
        this(lang, lineLengthLimit, indentWidth, indentStyle, normalizeCommentStartCase, false);
    }

    public JsonSpecificRule(
        final Lang    lang,
        final int     lineLengthLimit,
        final int     indentWidth,
        final String  indentStyle,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod
    )
    {
        this.lang                      = lang;
        this.lineLengthLimit           = lineLengthLimit;
        this.indentWidth               = indentWidth;
        this.indentUnit                = "tabs".equals(
            indentStyle
        ) ? "\t" : repeatChar(
            ' ', Math.max(0, indentWidth)
        );
        this.normalizeCommentStartCase = normalizeCommentStartCase;
        this.normalizeCommentEndPeriod = normalizeCommentEndPeriod;
    }

    private String normComment(final String commentText)
    {
        return FormatterSimpleBraced.normalizeComment(
            commentText, normalizeCommentStartCase, normalizeCommentEndPeriod
        );
    }

    /**
     * Malformed JSON/JSON5 input that the parser cannot make sense of -- caught generically by
     *  {@code Main}'s per-file error handling, same as any other rule class's runtime failure
     */
    public static final class JsonParseException extends RuntimeException {

        public JsonParseException(final String message)
        {
            super(message);
        }

    } // class JsonParseException

    // ---- AST ---------------------------------------------------------------------------------

    private abstract static class Node {
    }

    private static final class Scalar extends Node {

        final String raw;

        Scalar(final String raw)
        {
            this.raw = raw;
        }

    } // class Scalar

    /** One member (object) or element (array). {@code key} is null for array elements. */
    private static final class Item {

        List<String> leadingComments = new ArrayList<>();
        boolean      blankBefore;
        String       key;
        String       midComment;                          // A comment wedged between the key and its ':' -- forces this item out
                            // Of any colon-alignment group, same posture as a leading comment
        Node    value;
        String  trailingComment;
        boolean hasComma;

    } // class Item

    private static final class Container extends Node {

        final boolean    isObject;
        final List<Item> items = new ArrayList<>();

        Container(final boolean isObject)
        {
            this.isObject = isObject;
        }

    } // class Container

    // ---- Parsing ------------------------------------------------------------------------------

    private static final class Cursor {

        final List<Token> toks;
              int         i;

        Cursor(final List<Token> toks)
        {
            this.toks = toks;
        }

        Token cur()
        {
            return i < toks.size() ? toks.get(i) : null;
        }

    } // class Cursor

    /**
     * Consumes whitespace/newlines/comments up to the next significant token, recording comment
     *  text (in order) and whether a blank line (2+ consecutive newlines) occurred anywhere in the
     *  span -- both a group-break signal per §1.1.
     */
    private void collectTrivia(
        final Cursor       c,
        final List<String> comments,
        final boolean[]    blankOut
    )
    {
        final List<String>  rawTexts    = new ArrayList<>();
        final List<Boolean> blankBefore = new ArrayList<>();
              int           newlines    = 0;
        while( c.cur() != null ) {
            final Token t = c.cur();
            if(t.type == TokenType.WHITESPACE) {
                c.i++;
                continue;
            }
            if(t.type == TokenType.NEWLINE) {
                ++newlines;
                if(newlines > 1) blankOut[0] = true;
                c.i++;
                continue;
            }
            if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                rawTexts.add(t.text);
                blankBefore.add(newlines > 1);
                newlines = 0;
                c.i++;
                continue;
            } // if
            break;
        } // while
        FormatterSimpleBraced.normalizeCommentTrivia(
            rawTexts, blankBefore, normalizeCommentStartCase, normalizeCommentEndPeriod, comments
        );
    }

    /**
     * Same-line trailing comment right after a value/comma -- only consumed if no newline is
     *  crossed first
     */
    private String collectTrailingComment(final Cursor c)
    {
        final int save = c.i;
        while( c.cur() != null && c.cur().type == TokenType.WHITESPACE ) c.i++;
        final Token t = c.cur();
        if( t != null && (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) ) {
            c.i++;
            return normComment(t.text);
        }
        c.i = save;

        return null;
    }

    private Node parseValue(final Cursor c)
    {
        final Token t = c.cur();
        if(t == null) throw new JsonParseException("unexpected end of input");
        if( Token.isPunct(t, "{") ) return parseContainer(c, true);
        if( Token.isPunct(t, "[") ) return parseContainer(c, false);
        if(t.type == TokenType.STRING || t.type == TokenType.NUMBER || t.type == TokenType.IDENTIFIER) {
            c.i++;
            return new Scalar(t.text);
        }
        if( t.type == TokenType.OP && ( "+".equals(t.text) || "-".equals(t.text) ) ) {
            final Token next = c.i + 1 < c.toks.size() ? c.toks.get(c.i + 1) : null;
            if( next != null && (next.type == TokenType.NUMBER || next.type == TokenType.IDENTIFIER) ) {
                c.i += 2;
                return new Scalar(t.text + next.text);
            }
        } // if
        throw new JsonParseException("unexpected token: " + t.text);
    }

    private Container parseContainer(final Cursor c, final boolean isObject)
    {
        c.i++; // Consume '{' or '['
        final Container container = new Container(isObject);
        final String    closer    = isObject ? "}" : "]";
        while(true) {
            final Item      item  = new Item();
            final boolean[] blank = { false };
            collectTrivia(c, item.leadingComments, blank);
            item.blankBefore = blank[0];

            final Token t = c.cur();
            if(t == null) throw new JsonParseException(
                "unterminated " + (isObject ? "object" : "array")
            );
            if( Token.isPunct(t, closer) ) {
                c.i++;
                // A dangling blank line with no comment before the closer carries no information
                // that survives to rendered output (it would only ever be rendered via
                // renderItems' `i > 0 && item.blankBefore` check, which never fires for the first
                // item in the list) -- keeping it as a placeholder made an empty-looking container
                // non-empty per c.items.isEmpty(), forcing loose "{\n}" rendering that collapses to
                // tight "{}" on the very next reformat (non-idempotent). Only a real dangling
                // trailing comment needs the placeholder.
                if( !item.leadingComments.isEmpty() ) {
                    item.value = null;
                    container.items.add(item);
                }
                break;
            } // if

            if(isObject) {
                if(t.type != TokenType.STRING && t.type != TokenType.IDENTIFIER) throw new JsonParseException(
                    "expected object key, got: " + t.text
                );
                item.key = t.text;
                c.i++;
                while( c.cur() != null && ( c.cur().type == TokenType.WHITESPACE || c.cur().type == TokenType.NEWLINE ) ) c.i++;
                if( c.cur() != null && ( c.cur().type == TokenType.COMMENT_LINE
                        || c.cur().type == TokenType.COMMENT_BLOCK ) ) {
                    item.midComment = normComment( c.cur().text );
                    c.i++;
                    while( c.cur() != null && ( c.cur().type == TokenType.WHITESPACE || c.cur().type == TokenType.NEWLINE ) ) c.i++;
                }
                if( !Token.isPunct(
                    c.cur(), ":"
                ) ) throw new JsonParseException(
                    "expected ':' after key " + item.key
                );
                c.i++;
                while( c.cur() != null && ( c.cur().type == TokenType.WHITESPACE || c.cur().type == TokenType.NEWLINE ) ) c.i++;
            } // if

            item.value = parseValue(c);

            while( c.cur() != null && c.cur().type == TokenType.WHITESPACE ) c.i++;
            if( Token.isPunct( c.cur(), "," ) ) {
                item.hasComma = true;
                c.i++;
            }
            item.trailingComment = collectTrailingComment(c);
            container.items.add(item);
        } // while

        return container;
    }

    // ---- Rendering ------------------------------------------------------------------------------

    private static String repeatChar(final char c, final int count)
    {
        final StringBuilder sb = new StringBuilder(count);
        for(int i = 0; i < count; ++i) sb.append(c);

        return sb.toString();
    }

    private String indent(final int depth)
    {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < depth; ++i) sb.append(indentUnit);

        return sb.toString();
    }

    /**
     * True if {@code node} is a scalar, or a container with no members/elements at all -- these
     *  never force their containing array loose
     */
    private boolean containsContainer(final Node node)
    {
        return node instanceof Container;
    }

    private void render(final Node node, final int depth, final StringBuilder out)
    {
        if(node instanceof Scalar) {
            out.append( ( (Scalar) node ).raw );
            return;
        }
        final Container c = (Container)node;
        if( c.items.isEmpty() ) {
            out.append(c.isObject ? "{}" : "[]");
            return;
        }
        if(c.isObject) renderObject(c, depth, out);
        else           renderArray(c, depth, out);
    }

    /**
     * True if every item in {@code c} is a plain scalar-valued member/element with no comments/
     *  blank lines -- the shared tight-rendering eligibility check for both objects and arrays
     */
    private boolean canBeTight(final Container c)
    {
        for(final Item item : c.items) {
            if( item.value == null || containsContainer(
                item.value
            ) || !item.leadingComments.isEmpty() || item.trailingComment != null || item.blankBefore ) return false;
        }

        return true;
    }

    /**
     * §1.2: an object stays tight (single line, {@code { key : value, ... }} with inner padding
     *  spaces) only if every member is a scalar (no nested object/array), no member carries a
     *  comment, and the tight rendering fits within the configured line-length limit.
     */
    private void renderObject(final Container c, final int depth, final StringBuilder out)
    {
        // Unlike arrays, an object with 2+ members always goes loose (multi-line, so its members
        // get their own independent colon-alignment group per §1.1) -- only a single-member object
        // is eligible for tight single-line rendering, since there's nothing to align otherwise.
        if( c.items.size() == 1 && canBeTight(c) ) {
            final StringBuilder tight = new StringBuilder("{ ");
            for( int i = 0; i < c.items.size(); ++i ) {
                final Item item = c.items.get(i);
                if(i > 0) tight.append(", ");
                tight.append(item.key).append(" : ");
                render(item.value, depth + 1, tight);
                if( item.hasComma && i == c.items.size() - 1 ) tight.append(',');
            } // for
            tight.append(" }");
            if( indent(depth).length() + tight.length() <= lineLengthLimit ) {
                out.append(tight);
                return;
            }
        } // if
        out.append('{').append('\n');
        renderItems(c.items, depth + 1, out, true);
        out.append( indent(depth) ).append('}');
    }

    /**
     * §1.2: an array stays tight (single line) only if every element is a scalar (no nested
     *  object/array) and no element carries a comment, and the tight rendering fits within the
     *  configured line-length limit at this nesting depth.
     */
    private void renderArray(final Container c, final int depth, final StringBuilder out)
    {
        if( canBeTight(c) ) {
            final StringBuilder tight = new StringBuilder("[");
            for( int i = 0; i < c.items.size(); ++i ) {
                final Item item = c.items.get(i);
                if(i > 0) tight.append(", ");
                render(item.value, depth + 1, tight);
                if( item.hasComma && i == c.items.size() - 1 ) tight.append(',');
            }
            tight.append(']');
            if( indent(depth).length() + tight.length() <= lineLengthLimit ) {
                out.append(tight);
                return;
            }
        } // if
        out.append('[').append('\n');
        renderItems(c.items, depth + 1, out, false);
        out.append( indent(depth) ).append(']');
    }

    private void renderItems(
        final List<Item>    items,
        final int           depth,
        final StringBuilder out,
        final boolean       isObject
    )
    {
        // Compute colon-alignment groups (object only): a run of consecutive real items (not
        // dangling trailing-comment placeholders) with no leading comment/blank line between them
        final List<String> keys    = isObject ? new ArrayList<>() : null;
              String[]     padding = null;
        if(isObject) {
            padding = new String[ items.size() ];
            int groupStart = -1; // -1 = no open group
            for( int i = 0; i <= items.size(); ++i ) {
                final boolean atEnd      = i == items.size();
                final boolean isDangling = !atEnd && items.get(i).value == null;
                // A group boundary falls *before* item i whenever it carries a leading
                // comment/blank line (§1.1) or is a dangling trailing-comment placeholder (no
                // key at all) -- the item itself (if it has a key) still starts a fresh group
                // rather than being dropped from alignment entirely.
                final boolean hasMidComment = !atEnd && items.get(i).midComment != null;
                final boolean breaksBefore  = atEnd || isDangling || hasMidComment || !items.get(
                    i
                ).leadingComments.isEmpty() || items.get(
                    i
                ).blankBefore;
                if(breaksBefore) {
                    if(groupStart >= 0 && i > groupStart) {
                        keys.clear();
                        for(int g = groupStart; g < i; ++g) keys.add( items.get(g).key );
                        final String[] groupPad = FormatterSimpleBraced.padKeysForColonAlignment(
                            keys
                        );
                        for(int g = groupStart; g < i; ++g) padding[g] = groupPad[g - groupStart];
                    } // if
                    groupStart = (!atEnd && !isDangling && !hasMidComment) ? i : -1;
                } // if
                else if(groupStart < 0) {
                    groupStart = i;
                }
            } // for
        } // if

        for( int i = 0; i < items.size(); ++i ) {
            final Item item = items.get(i);
            if(i > 0 && item.blankBefore) out.append('\n');
            for(final String comment : item.leadingComments) out.append(
                indent(depth)
            ) .append(
                FormatterSimpleBraced.reindentBlockComment( comment, indent(depth) )
            ) .append(
                '\n'
            );
            if(item.value == null) continue; // Dangling trailing-comment placeholder, no value to render
            out.append( indent(depth) );
            if(isObject) {
                out.append(item.key);
                if(item.midComment != null) out.append(' ').append(item.midComment).append(" : ");
                else                        out.append( padding[i] ).append(':').append(' ');
            }
            render(item.value, depth, out);
            if(item.hasComma) out.append(',');
            if(item.trailingComment != null) out.append(' ').append(item.trailingComment);
            out.append('\n');
        } // for
    }

    /** Tokenizes, parses, and re-renders {@code content} per STYLE_DATA_FORMATS.md §1. */
    public String format(final String content)
    {
        final List<Token>  tokens          = new JsonTokenizer().tokenize(content);
        final Cursor       c               = new Cursor(tokens);
        final List<String> leadingComments = new ArrayList<>();
        final boolean[]    blank           = { false };
        collectTrivia(c, leadingComments, blank);
        final Node         root             = parseValue(c);
        final List<String> trailingComments = new ArrayList<>();
        collectTrivia( c, trailingComments, new boolean[1] );
        if( c.cur() != null ) throw new JsonParseException(
            "unexpected trailing content: " + c.cur().text
        );

        final StringBuilder out = new StringBuilder();
        for(final String comment : leadingComments) out.append(comment).append('\n');
        render(root, 0, out);
        out.append('\n');
        for(final String comment : trailingComments) out.append(comment).append('\n');

        return out.toString();
    }

} // class JsonSpecificRule
