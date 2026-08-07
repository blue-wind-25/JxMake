/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;

import com.jxmake.formatter.tokenizer.TokenizerCore;

import com.jxmake.formatter.FormatterSimpleBraced;
import com.jxmake.formatter.Lang;

/**
 * STYLE_DATA_FORMATS.md §6 (TOML) rule logic. Neither curly, indent-based, tag-based, nor
 * SimpleBraced per RDD_KEY_189/191 -- unlike YAML, TOML has no indentation-significant nesting at
 * all (tables express nesting through dotted header names, §6.2), so this is a flat, single-pass
 * line parser -- simpler than {@link YamlSpecificRule}'s recursive block parser.
 *
 * <p>Implements §6.1 (`=`-alignment groups, broken by blank lines/comments/table headers), §6.2
 * (`[section]`/`[[array]]` headers are non-joining rule headers, no added indentation for the keys
 * under them), §6.3 (array tight/loose per nested-container presence, reusing JSON's §1.2 shape),
 * §6.4 (inline tables always single-line -- a TOML grammar constraint), and §6.5 (dotted keys and
 * string quote styles preserved as written). Also implements its own {@code #%}-based
 * {@code JXM_CFMT_DIS}/{@code ENA} frozen-span detection, same posture as {@link YamlSpecificRule}.
 */
public final class TomlSpecificRule {

    private final Lang    lang;
    private final int     indentWidth;
    private final String  indentUnit;
    private final boolean normalizeCommentStartCase;
    private final boolean normalizeCommentEndPeriod;

    public TomlSpecificRule(final Lang lang)
    {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public TomlSpecificRule(final Lang lang, final int lineLengthLimit)
    {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public TomlSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth)
    {
        this(lang, lineLengthLimit, indentWidth, true);
    }

    public TomlSpecificRule(
        final Lang    lang,
        final int     lineLengthLimit,
        final int     indentWidth,
        final boolean normalizeCommentStartCase
    )
    {
        this(lang, lineLengthLimit, indentWidth, normalizeCommentStartCase, false);
    }

    public TomlSpecificRule(
        final Lang    lang,
        final int     lineLengthLimit,
        final int     indentWidth,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod
    )
    {
        this.lang                      = lang;
        this.indentWidth               = Math.max(1, indentWidth);
        this.indentUnit                = repeatChar(' ', this.indentWidth);
        this.normalizeCommentStartCase = normalizeCommentStartCase;
        this.normalizeCommentEndPeriod = normalizeCommentEndPeriod;
    }

    /**
     * Malformed TOML input that the parser cannot make sense of -- caught generically by
     *  {@code Main}'s per-file error handling, same as any other rule class's runtime failure
     */
    public static final class TomlParseException extends RuntimeException {

        public TomlParseException(final String message)
        {
            super(message);
        }

    } // class TomlParseException

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
     * 2026-08-08 session: chain-groups a run of consecutive leading `#` comments the same way curly
     *  chains `//` (RDD_KEY_265/RDD_KEY_266) -- delegates to
     *  {@link ToolingCommentNormalizer#normalizeChain}, shared with YAML/Makefile/Bash/PowerShell.
     *  {@code raw} holds unnormalized, trimmed `#...`-prefixed comment lines in source order;
     *  {@code blankBefore.get(k)} is true iff a blank line separated comment {@code k} from comment
     *  {@code k-1}.
     */
    private List<String> finalizeComments(final List<String> raw, final List<Boolean> blankBefore)
    {
        if( raw.isEmpty() ) return raw;
        final List<String> bodies = new ArrayList<>();
        for(final String c : raw) bodies.add( c.substring(1) ); // strip leading '#'
        final List<String> normBodies = ToolingCommentNormalizer.normalizeChain(
            bodies, blankBefore, normalizeCommentStartCase, normalizeCommentEndPeriod, null
        );
        final List<String> out = new ArrayList<>();
        for(final String b : normBodies) out.add("#" + b);

        return out;
    }

    private String normComment(final String commentText)
    {
        String text = normalizeCommentEndPeriod
                ? ToolingCommentNormalizer.stripSoleTrailingPeriod(commentText) : commentText;
        if(!normalizeCommentStartCase) return text;
        int i = 1;
        while( i < text.length() && text.charAt(i) == ' ' ) i++;
        if( i < text.length() ) {
            final char ch = text.charAt(i);
            if( Character.isLetter(
                ch
            ) && Character.isLowerCase(
                ch
            ) ) return text.substring(
                0, i
            ) + Character.toUpperCase(
                ch
            ) + text.substring(
                i + 1
            );
        } // if

        return text;
    }

    /** Finds the first unquoted, unbracketed '=' -- the key/value separator. Returns -1 if none. */
    private static int findAssignmentEquals(final String s)
    {
        boolean inSingle = false;
        boolean inDouble = false;
        int     depth    = 0;
        for( int i = 0; i < s.length(); ++i ) {
            final char ch = s.charAt(i);
            if(inSingle) {
                if(ch == '\'') inSingle = false;
                continue;
            }
            if(inDouble) {
                     if(ch == '\\') i++;
                else if(ch == '"')  inDouble = false;
                continue;
            }
                 if(ch == '\'')              inSingle = true;
            else if(ch == '"')               inDouble = true;
            else if(ch == '{' || ch == '[')  depth++;
            else if(ch == '}' || ch == ']')  depth--;
            else if(ch == '=' && depth == 0) return i;
        } // for

        return -1;
    }

    /**
     * Splits off a same-line trailing `#` comment, respecting quotes -- a `#` only starts a
     *  comment when at the start of the string or preceded by whitespace. Returns a two-element
     *  array: [codePart (right-trimmed), commentPartOrNull].
     */
    private static String[] splitTrailingComment(final String s)
    {
        boolean inSingle = false;
        boolean inDouble = false;
        for( int i = 0; i < s.length(); ++i ) {
            final char ch = s.charAt(i);
            if(inSingle) {
                if(ch == '\'') inSingle = false;
                continue;
            }
            if(inDouble) {
                     if(ch == '\\') i++;
                else if(ch == '"')  inDouble = false;
                continue;
            }
                 if(ch == '\'') inSingle = true;
            else if(ch == '"') inDouble = true;
            else if( ch == '#' && ( i == 0 || s.charAt(
                i - 1
            ) == ' ' || s.charAt(
                i - 1
            ) == '\t' ) ) {
                return new String[] {rtrim( s.substring(0, i) ), s.substring(i)};
            }
        } // for

        return new String[] {rtrim(s), null};
    }

    /**
     * Returns {@code """}/{@code '''} if {@code value} opens a multi-line basic/literal string,
     *  else {@code null}
     */
    private static String multilineStringDelim(final String value)
    {
        if( value.startsWith("\"\"\"") ) return "\"\"\"";
        if( value.startsWith("'''") ) return "'''";

        return null;
    }

    /**
     * True when {@code trimmedLine} is a {@code key = """.../key = '''...} line whose
     *  multi-line-string value is not closed on the same physical line -- i.e. real continuation
     *  lines must be consumed (TOML v1.0 core syntax; found missing entirely via rust-lang/cargo
     *  dogfood testing, see the "Line-based parsing" loop).
     */
    private static boolean isUnterminatedMultilineStringLine(final String trimmedLine)
    {
        final int eq = findAssignmentEquals(trimmedLine);
        if(eq < 0) return false;
        final String value = trimmedLine.substring(eq + 1).trim();
        final String delim = multilineStringDelim(value);
        if(delim == null) return false;

        return !value.substring( delim.length() ).contains(delim);
    }

    private static String rtrim(final String s)
    {
        int end = s.length();
        while( end > 0 && Character.isWhitespace( s.charAt(end - 1) ) ) end--;

        return s.substring(0, end);
    }

    /**
     * Net {@code [`/`{` minus `]`/`}`} depth change across {@code s}, ignoring bracket-looking
     *  characters inside quotes -- used to detect a `key = value` line whose array/inline-table
     *  value spans multiple physical input lines (only produced by this formatter's own §6.3 loose
     *  array output, but input may echo it back, e.g. under an idempotency check).
     */
    private static int bracketBalance(final String s)
    {
        boolean inSingle = false;
        boolean inDouble = false;
        int     depth    = 0;
        for( int i = 0; i < s.length(); ++i ) {
            final char ch = s.charAt(i);
            if(inSingle) {
                if(ch == '\'') inSingle = false;
                continue;
            }
            if(inDouble) {
                     if(ch == '\\') i++;
                else if(ch == '"')  inDouble = false;
                continue;
            }
                 if(ch == '\'')             inSingle = true;
            else if(ch == '"')              inDouble = true;
            else if(ch == '{' || ch == '[') depth++;
            else if(ch == '}' || ch == ']') depth--;
        } // for

        return depth;
    }

    // ---- AST -----------------------------------------------------------------------------------

    private abstract static class ValueNode {
    }

    private static final class Scalar extends ValueNode {

        final String raw;

        Scalar(final String raw)
        {
            this.raw = raw;
        }

    } // class Scalar

    private static final class Entry {

        final String    key;
        final ValueNode value;

        Entry(final String key, final ValueNode value)
        {
            this.key   = key;
            this.value = value;
        }

    } // class Entry

    private static final class Arr extends ValueNode {

        final List<ValueNode> elements = new ArrayList<>();

    } // class Arr

    private static final class Tbl extends ValueNode {

        final List<Entry> entries = new ArrayList<>();

    } // class Tbl

    private static boolean isAtomsOnly(final Arr arr)
    {
        for(final ValueNode v : arr.elements) {
            if( !(v instanceof Scalar) ) return false;
        }

        return true;
    }

    private static final class ValueCursor {

        final String s;
              int    i;

        ValueCursor(final String s)
        {
            this.s = s;
        }

        void skipWs()
        {
            while( i < s.length() && s.charAt(i) == ' ' ) i++;
        }

        char cur()
        {
            return i < s.length() ? s.charAt(i) : '\0';
        }

    } // class ValueCursor

    private ValueNode parseValue(final ValueCursor c)
    {
        c.skipWs();
        final char ch = c.cur();
        if(ch == '[') {
            c.i++;
            final Arr arr = new Arr();
            c.skipWs();
            if( c.cur() == ']' ) {
                c.i++;
                return arr;
            }
            while(true) {
                c.skipWs();
                arr.elements.add( parseValue(c) );
                c.skipWs();
                if( c.cur() == ',' ) {
                    c.i++;
                    continue;
                }
                if( c.cur() == ']' ) {
                    c.i++;
                    break;
                }
                throw new TomlParseException( "unterminated array near: " + c.s.substring(c.i) );
            } // while
            return arr;
        } // if
        if(ch == '{') {
            c.i++;
            final Tbl tbl = new Tbl();
            c.skipWs();
            if( c.cur() == '}' ) {
                c.i++;
                return tbl;
            }
            while(true) {
                c.skipWs();
                final String key = readScalarText(c, true).trim();
                c.skipWs();
                if( c.cur() != '=' ) throw new TomlParseException(
                    "expected '=' in inline table near: " + c.s.substring(c.i)
                );
                c.i++;
                c.skipWs();
                final ValueNode value = parseValue(c);
                tbl.entries.add( new Entry(key, value) );
                c.skipWs();
                if( c.cur() == ',' ) {
                    c.i++;
                    continue;
                }
                if( c.cur() == '}' ) {
                    c.i++;
                    break;
                }
                throw new TomlParseException(
                    "unterminated inline table near: " + c.s.substring(c.i)
                );
            } // while
            return tbl;
        } // if

        return new Scalar( readScalarText(c, false).trim() );
    }

    /**
     * Reads a scalar (unquoted, or a quoted string) up to the next structural character
     *  ({@code , ] }}, or -- for an inline-table key -- {@code =})
     */
    private String readScalarText(final ValueCursor c, final boolean stopAtEquals)
    {
        final int start = c.i;
        if( c.cur() == '"' || c.cur() == '\'' ) {
            final char quote = c.cur();
            c.i++;
            while( c.i < c.s.length() && c.s.charAt(c.i) != quote ) {
                if( quote == '"' && c.s.charAt(c.i) == '\\' ) c.i++;
                c.i++;
            }
            if( c.i < c.s.length() ) c.i++;
            while( c.i < c.s.length() && c.s.charAt(
                c.i
            ) != ',' && c.s.charAt(
                c.i
            ) != ']' && c.s.charAt(
                c.i
            ) != '}' && !( stopAtEquals && c.s.charAt(
                c.i
            ) == '=' ) ) c.i++;
            return c.s.substring(start, c.i);
        } // if
        while( c.i < c.s.length() ) {
            final char ch = c.s.charAt(c.i);
            if( ch == ',' || ch == ']' || ch == '}' || (stopAtEquals && ch == '=') ) break;
            c.i++;
        }

        return c.s.substring(start, c.i);
    }

    private String renderValueTight(final ValueNode node)
    {
        if(node instanceof Scalar) return ( (Scalar) node ).raw;
        if(node instanceof Tbl) {
            final Tbl           tbl = (Tbl)node;
            final StringBuilder sb  = new StringBuilder("{ ");
            for( int i = 0; i < tbl.entries.size(); ++i ) {
                if(i > 0) sb.append(", ");
                final Entry e = tbl.entries.get(i);
                sb.append(e.key).append(" = ").append( renderValueTight(e.value) );
            }
            return sb.append(" }").toString();
        } // if
        final Arr           arr = (Arr)node;
        final StringBuilder sb  = new StringBuilder("[");
        for( int i = 0; i < arr.elements.size(); ++i ) {
            if(i > 0) sb.append(", ");
            sb.append( renderValueTight( arr.elements.get(i) ) );
        }

        return sb.append(']').toString();
    }

    /**
     * §6.3: an array of atoms stays tight; one containing a nested array/inline table goes loose,
     *  one element per line, indented one level under the key's own column.
     */
    private void renderValue(final ValueNode node, final StringBuilder out)
    {
        if( node instanceof Arr && !isAtomsOnly( (Arr) node ) ) {
            final Arr arr = (Arr)node;
            out.append(" [\n");
            for( int i = 0; i < arr.elements.size(); ++i ) {
                out.append( indent(1) ).append( renderValueTight( arr.elements.get(i) ) );
                if( i < arr.elements.size() - 1 ) out.append(',');
                out.append('\n');
            }
            out.append(']');
            return;
        } // if
        out.append(' ').append( renderValueTight(node) );
    }

    // ---- Line-based parsing ---------------------------------------------------------------------

    private static final class Item {

        List<String> leadingComments = new ArrayList<>();
        boolean      blankBefore;
        boolean      isFrozen;
        List<String> frozenLines;
        boolean      isHeader;
        String       headerRaw;
        boolean      dangling;
        String       key;
        ValueNode    value;
        String       trailingComment;

        boolean isKeyed()
        {
            return key != null;
        }

    } // class Item

    public String format(final String content)
    {
        final boolean      endsWithNewline = content.endsWith("\n");
        final String[]     rawLines        = content.split("\n", -1);
        final List<String> lines           = new ArrayList<>( java.util.Arrays.asList(rawLines) );
        if( endsWithNewline && !lines.isEmpty() ) lines.remove( lines.size() - 1 );

        final List<Item>    items               = new ArrayList<>();
              List<String>  pendingComments     = new ArrayList<>();
              List<Boolean> pendingCommentBlank = new ArrayList<>();
              boolean       pendingBlank        = false;
              int           idx                 = 0;
        while( idx < lines.size() ) {
            final String raw     = lines.get(idx);
            final String trimmed = raw.trim();
            if( trimmed.isEmpty() ) {
                pendingBlank = true;
                ++idx;
                continue;
            }
            if( trimmed.startsWith("#") ) {
                if( ("#% " + TokenizerCore.JXM_CFMT_DIS).equals(trimmed) ) {
                    final Item item = new Item();
                    item.leadingComments = finalizeComments(pendingComments, pendingCommentBlank);
                    item.blankBefore     = pendingBlank;
                    pendingComments      = new ArrayList<>();
                    pendingCommentBlank  = new ArrayList<>();
                    pendingBlank         = false;
                    item.isFrozen        = true;
                    item.frozenLines     = new ArrayList<>();
                    item.frozenLines.add(raw);
                    ++idx;
                    while( idx < lines.size() && !("#% " + TokenizerCore.JXM_CFMT_ENA).equals(
                        lines.get(idx).trim()
                    ) ) {
                        item.frozenLines.add( lines.get(idx) );
                        ++idx;
                    }
                    if( idx < lines.size() ) {
                        item.frozenLines.add( lines.get(idx) );
                        ++idx;
                    }
                    items.add(item);
                    continue;
                } // if
                pendingComments.add(trimmed); // raw text; grouped/normalized in finalizeComments
                pendingCommentBlank.add(pendingBlank);
                pendingBlank = false;
                ++idx;
                continue;
            } // if
            final Item item = new Item();
            item.leadingComments = finalizeComments(pendingComments, pendingCommentBlank);
            item.blankBefore     = pendingBlank;
            pendingComments      = new ArrayList<>();
            pendingCommentBlank  = new ArrayList<>();
            pendingBlank         = false;
            if( trimmed.startsWith("[") ) {
                item.isHeader  = true;
                item.headerRaw = raw;
                ++idx;
            }
            else if( isUnterminatedMultilineStringLine(trimmed) ) {
                // A `key = """`/`'''`-opened multi-line basic/literal string (TOML v1.0 core
                // syntax) has no bracket to balance-track like an array/inline-table continuation
                // does -- found via rust-lang/cargo dogfood testing (triagebot.toml's
                // `message = """\...` block). Consume subsequent RAW (untrimmed) lines verbatim,
                // preserving the string's real embedded newlines, until the matching closing
                // delimiter line is found; the whole span becomes one opaque Scalar value (same
                // "preserve exactly" treatment as JSON5's multi-line string continuations).
                final int           eq         = findAssignmentEquals(trimmed);
                final String        firstChunk = trimmed.substring(eq + 1).trim();
                final String        delim      = multilineStringDelim(firstChunk);
                final StringBuilder valueSpan  = new StringBuilder(firstChunk);
                ++idx;
                boolean closed = false;
                while( !closed && idx < lines.size() ) {
                    // Append subsequent lines verbatim (untrimmed) -- indentation/whitespace
                    // inside a multi-line string's body is real content, not incidental
                    // formatting, and must be preserved exactly
                    final String nextRaw = lines.get(idx);
                    valueSpan.append('\n').append(nextRaw);
                    ++idx;
                    if( nextRaw.contains(delim) ) closed = true;
                } // while
                if(!closed) throw new TomlParseException(
                    "unterminated multi-line string starting: " + trimmed
                );
                item.key   = trimmed.substring(0, eq).trim();
                item.value = new Scalar( valueSpan.toString() );
            }
            else {
                String logical = trimmed;
                ++idx;
                int balance = bracketBalance(logical);
                while( balance > 0 && idx < lines.size() ) {
                    // Strip each continuation line's own trailing comment before joining --
                    // otherwise an interior comment (e.g. `"target/", # note` mid-array) gets
                    // treated by the final splitTrailingComment() call as extending to the very
                    // end of the whole joined logical line, swallowing the array's closing `]`/`}`
                    // as "comment text" and producing a spurious "unterminated array" parse error.
                    // Found via rust-lang/cargo dogfood testing (Cargo.toml's `exclude = [...]`).
                    final String cont = splitTrailingComment( lines.get(idx).trim() )[0];
                    logical  = logical + " " + cont;
                    balance += bracketBalance(cont);
                    ++idx;
                } // while
                final String[] parts = splitTrailingComment(logical);
                final String   code  = parts[0];
                item.trailingComment = parts[1] != null ? normComment( parts[1] ) : null;
                final int eq = findAssignmentEquals(code);
                if(eq < 0) throw new TomlParseException(
                    "expected 'key = value' line, got: " + trimmed
                );
                item.key   = code.substring(0, eq).trim();
                item.value = parseValue( new ValueCursor( code.substring(eq + 1).trim() ) );
            }
            items.add(item);
        } // while
        if( !pendingComments.isEmpty() || pendingBlank ) {
            final Item d = new Item();
            d.leadingComments = finalizeComments(pendingComments, pendingCommentBlank);
            d.blankBefore     = pendingBlank;
            d.dangling        = true;
            items.add(d);
        } // if

        final StringBuilder out = new StringBuilder();
        renderItems(items, out);

        return out.toString();
    }

    private void renderItems(final List<Item> items, final StringBuilder out)
    {
        final String[] padding    = new String[ items.size() ];
              int      groupStart = -1;
        for( int i = 0; i <= items.size(); ++i ) {
            final boolean atEnd        = i == items.size();
            final boolean breaksBefore = atEnd || !items.get(
                i
            ).isKeyed() || !items.get(
                i
            ).leadingComments.isEmpty() || items.get(
                i
            ).blankBefore;
            if(breaksBefore) {
                if(groupStart >= 0 && i > groupStart) {
                    final List<String> keys = new ArrayList<>();
                    for(int g = groupStart; g < i; ++g) keys.add( items.get(g).key );
                    final String[] groupPad = FormatterSimpleBraced.padKeysForColonAlignment(keys);
                    for(int g = groupStart; g < i; ++g) padding[g] = groupPad[g - groupStart];
                }
                groupStart = ( !atEnd && items.get(i).isKeyed() ) ? i : -1;
            } // if
            else if(groupStart < 0) {
                groupStart = i;
            }
        } // for

        for( int i = 0; i < items.size(); ++i ) {
            final Item item = items.get(i);
            if(i > 0 && item.blankBefore) out.append('\n');
            for(final String comment : item.leadingComments) out.append(comment).append('\n');
            if(item.dangling) continue;
            if(item.isFrozen) {
                for(final String raw : item.frozenLines) out.append(raw).append('\n');
                continue;
            }
            if(item.isHeader) {
                out.append(item.headerRaw).append('\n');
                continue;
            }
            out.append(item.key).append( padding[i] ).append('=');
            renderValue(item.value, out);
            if(item.trailingComment != null) out.append(' ').append(item.trailingComment);
            out.append('\n');
        } // for
    }

} // class TomlSpecificRule
