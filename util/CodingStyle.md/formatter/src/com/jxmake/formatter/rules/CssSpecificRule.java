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
import com.jxmake.formatter.tokenizer.CssTokenizer;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

/**
 * CSS STYLE_DATA_FORMATS.md §3 rule logic. Not curly/indent/tag-based; sibling of
 * {@link JsonSpecificRule} under the "SimpleBraced" family (RDD_KEY_189/190) --
 * see {@link com.jxmake.formatter.FormatterSimpleBraced}.
 *
 * <p>Implements §3.1 (property/value colon alignment within a group, broken by blank
 * lines/comments/a nested at-rule or selector boundary -- at-rules and native-nesting
 * {@code &} blocks recurse into the same block-body parser, so nested groups align
 * independently) and §3.2 (standard indent handling, no bracket-complexity rule needed).
 */
public final class CssSpecificRule {

    private final Lang    lang;
    private final int     lineLengthLimit;
    private final int     indentWidth;
    private final String  indentUnit;
    private final boolean normalizeCommentStartCase;
    private final boolean normalizeCommentEndPeriod;

    public CssSpecificRule(final Lang lang)
    {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public CssSpecificRule(final Lang lang, final int lineLengthLimit)
    {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public CssSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth)
    {
        this(lang, lineLengthLimit, indentWidth, "spaces");
    }

    public CssSpecificRule(
        final Lang   lang,
        final int    lineLengthLimit,
        final int    indentWidth,
        final String indentStyle
    )
    {
        this(lang, lineLengthLimit, indentWidth, indentStyle, true);
    }

    public CssSpecificRule(
        final Lang    lang,
        final int     lineLengthLimit,
        final int     indentWidth,
        final String  indentStyle,
        final boolean normalizeCommentStartCase
    )
    {
        this(lang, lineLengthLimit, indentWidth, indentStyle, normalizeCommentStartCase, false);
    }

    public CssSpecificRule(
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
     * Malformed CSS that the parser cannot make sense of -- caught generically by {@code Main}'s
     *  per-file error handling, same as any other rule class's runtime failure
     */
    public static final class CssParseException extends RuntimeException {

        public CssParseException(final String message)
        {
            super(message);
        }

    } // class CssParseException

    // ---- AST ---------------------------------------------------------------------------------

    /** A declaration (`prop: value;`) or a statement at-rule with no colon (`@import "x";`) */
    private static final class Decl {

        final String  prop;
        final String  value;
        final boolean hasColon;
        final String  midComment; // Comment wedged between the property and its ':' -- see Item

        Decl(final String prop, final String value, final boolean hasColon, final String midComment)
        {
            this.prop       = prop;
            this.value      = value;
            this.hasColon   = hasColon;
            this.midComment = midComment;
        }

    } // class Decl

    /**
     * A selector or at-rule header plus its block body -- covers plain rules, `@media`/
     *  `@supports`/`@keyframes`, and native-nesting `&` blocks uniformly
     */
    private static final class Rule {

        final String     header;
        final List<Item> body;

        Rule(final String header, final List<Item> body)
        {
            this.header = header;
            this.body   = body;
        }

    } // class Rule

    private static final class Item {

        List<String> leadingComments = new ArrayList<>();
        boolean      blankBefore;
        Decl         decl;                                // Exactly one of decl/rule/rawFrozen is non-null, or all null for a dangling
        Rule         rule;                                // Trailing-comment-only placeholder before a block's closing '}'
        String       trailingComment;
        String       rawFrozen;                           // `/*% JXM_CFMT_DIS */` .. `ENA` span, emitted verbatim, unformatted

    } // class Item

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
     *  text (in order) and whether a blank line occurred anywhere in the span -- a group-break
     *  signal per §3.1. CSS has no `//` line comments, unlike JSON5's trivia scan.
     */
    private void collectTrivia(
        final Cursor       c,
        final List<String> comments,
        final boolean[]    blankOut
    )
    {
        int newlines = 0;
        while( c.cur() != null ) {
            final Token t = c.cur();
            if(t.frozen) break; // `/*% JXM_CFMT_DIS */` span -- let the caller capture it raw
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
            if(t.type == TokenType.COMMENT_BLOCK) {
                comments.add( normComment(t.text) );
                newlines = 0;
                c.i++;
                continue;
            }
            break;
        } // while
    }

    private String collectTrailingComment(final Cursor c)
    {
        final int save = c.i;
        while( c.cur() != null && c.cur().type == TokenType.WHITESPACE ) c.i++;
        final Token t = c.cur();
        if(t != null && t.type == TokenType.COMMENT_BLOCK) {
            c.i++;
            return normComment(t.text);
        }
        c.i = save;

        return null;
    }

    private static final int TERM_BRACE_OPEN           = 0;
    private static final int TERM_SEMI                 = 1;
    private static final int TERM_BRACE_CLOSE_IMPLICIT = 2;

    private List<Item> parseBlockBody(final Cursor c, final boolean topLevel)
    {
        final List<Item> items = new ArrayList<>();
        while(true) {
            final Item      item  = new Item();
            final boolean[] blank = { false };
            // Peek past leading whitespace/newlines (without consuming) to see whether they lead
            // directly into a frozen (`/*% JXM_CFMT_DIS */` .. `ENA`) span. If so, capture that
            // leading gap plus the whole frozen span verbatim -- including its original
            // indentation -- rather than discarding it via collectTrivia and re-deriving
            // structural indentation the normal way.
            int peek = c.i;
            while( peek < c.toks.size() && ( c.toks.get(
                peek
            ).type == TokenType.WHITESPACE || c.toks.get(
                peek
            ).type == TokenType.NEWLINE ) ) peek++;
            if( peek < c.toks.size() && c.toks.get(peek).frozen ) {
                final StringBuilder raw            = new StringBuilder();
                      boolean       skippedLineSep = false;
                while(c.i < peek) {
                    // The first newline in the leading gap is the plain line-separator every
                    // item's own render already supplies (via its trailing '\n') -- skip only
                    // that one so we don't introduce a spurious blank line, but keep any
                    // indentation whitespace (and further blank-line newlines) after it verbatim
                    if( !skippedLineSep && c.cur().type == TokenType.NEWLINE ) {
                        skippedLineSep = true;
                        c.i++;
                        continue;
                    }
                    raw.append( c.cur().text );
                    c.i++;
                } // while
                while( c.cur() != null && c.cur().frozen ) {
                    raw.append( c.cur().text );
                    c.i++;
                }
                item.rawFrozen = raw.toString();
                items.add(item);
                continue;
            } // if

            collectTrivia(c, item.leadingComments, blank);
            item.blankBefore = blank[0];

            final Token t = c.cur();
            if(t == null) {
                if(topLevel) {
                    if( !item.leadingComments.isEmpty() || item.blankBefore ) items.add(item);
                    break;
                }
                throw new CssParseException("unterminated block, reached end of input");
            } // if
            if( Token.isPunct(t, "}") ) {
                if(topLevel) throw new CssParseException("unexpected '}' at top level");
                c.i++;
                if( !item.leadingComments.isEmpty() || item.blankBefore ) items.add(item);
                break;
            }

            // Scan the header: raw text up to the next top-level (paren-depth 0) '{', ';', or
            // this block's own closing '}' (the last declaration in a block need not have a
            // trailing ';'). Also records the first top-level ':' offset, used only if this
            // turns out to be a declaration.
            final StringBuilder text               = new StringBuilder();
                  int           parenDepth         = 0;
                  int           colonOffset        = -1;
                  int           terminator         = -1;
            final List<String>  preColonComments   = new ArrayList<>();
            final List<Integer> preColonCommentPos = new ArrayList<>();
            while(true) {
                final Token tok = c.cur();
                if(tok == null) {
                    if(topLevel) {
                        terminator = TERM_BRACE_CLOSE_IMPLICIT;
                        break;
                    }
                    throw new CssParseException("unterminated block, reached end of input");
                } // if
                if( Token.isPunct(tok, "(") ) {
                    ++parenDepth;
                    text.append(tok.text);
                    c.i++;
                    continue;
                }
                if( Token.isPunct(tok, ")") ) {
                    --parenDepth;
                    text.append(tok.text);
                    c.i++;
                    continue;
                }
                if( parenDepth == 0 && Token.isPunct(tok, "{") ) {
                    terminator = TERM_BRACE_OPEN;
                    c.i++;
                    break;
                }
                if( parenDepth == 0 && Token.isPunct(tok, ";") ) {
                    terminator = TERM_SEMI;
                    c.i++;
                    break;
                }
                if( parenDepth == 0 && Token.isPunct(tok, "}") ) {
                    terminator = TERM_BRACE_CLOSE_IMPLICIT;
                    break; // Do not consume -- the outer loop's pre-check handles it
                }
                if( parenDepth == 0 && colonOffset < 0 && Token.isPunct(
                    tok, ":"
                ) ) colonOffset = text.length();
                if(tok.type == TokenType.WHITESPACE || tok.type == TokenType.NEWLINE) {
                    if( text.length() > 0 && text.charAt(
                        text.length() - 1
                    ) != ' ' ) text.append(
                        ' '
                    );
                    c.i++;
                    continue;
                } // if
                if(tok.type == TokenType.COMMENT_BLOCK) {
                    // A comment before the top-level ':' is buffered rather than appended
                    // in-place: if this header turns out to be a colon'd declaration, it
                    // becomes a `prop /* comment */ : value` mid-comment (excluded from
                    // colon-alignment groups); otherwise (a selector/at-rule header, or a
                    // colon-less statement) it's spliced back into the header text below
                    if(colonOffset < 0) {
                        preColonComments.add( normComment(tok.text) );
                        preColonCommentPos.add( text.length() );
                    }
                    else {
                        text.append(tok.text);
                    }
                    c.i++;
                    continue;
                } // if
                text.append(tok.text);
                c.i++;
            } // while

            if(terminator == TERM_BRACE_OPEN || colonOffset < 0) {
                for( int k = preColonComments.size() - 1; k >= 0; --k ) text.insert(
                    preColonCommentPos.get(k), preColonComments.get(k)
                );
            }

            if(terminator == TERM_BRACE_OPEN) {
                item.rule            = new Rule( text.toString().trim(), parseBlockBody(c, false) );
                item.trailingComment = collectTrailingComment(c);
            }
            else {
                final String full = text.toString().trim();
                if(colonOffset >= 0) {
                    final String midComment = preColonComments.isEmpty() ? null : preColonComments.get(
                        preColonComments.size() - 1
                    );
                    item.decl = new Decl(
                        full.substring(0, colonOffset).trim(),
                        full.substring(colonOffset + 1).trim(), true, midComment
                    );
                } // if
                else {
                    item.decl = new Decl(full, "", false, null);
                }
                if(terminator == TERM_SEMI) item.trailingComment = collectTrailingComment(c);
                // TERM_BRACE_CLOSE_IMPLICIT: leave the '}' for the outer loop's pre-check
            }
            items.add(item);
        } // while

        return items;
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

    private void renderItems(final List<Item> items, final int depth, final StringBuilder out)
    {
        final List<String> keys       = new ArrayList<>();
        final String[]     padding    = new String[ items.size() ];
              int          groupStart = -1;
        for( int i = 0; i <= items.size(); ++i ) {
            final boolean atEnd           = i == items.size();
            final boolean isAlignableDecl = !atEnd && items.get(
                i
            ).decl != null && items.get(
                i
            ).decl.hasColon && items.get(
                i
            ).decl.midComment == null;
            final boolean breaksBefore    = atEnd || !isAlignableDecl || !items.get(
                i
            ).leadingComments.isEmpty() || items.get(
                i
            ).blankBefore;
            if(breaksBefore) {
                if(groupStart >= 0 && i > groupStart) {
                    keys.clear();
                    for(int g = groupStart; g < i; ++g) keys.add( items.get(g).decl.prop );
                    final String[] groupPad = FormatterSimpleBraced.padKeysForColonAlignment(keys);
                    for(int g = groupStart; g < i; ++g) padding[g] = groupPad[g - groupStart];
                }
                groupStart = (!atEnd && isAlignableDecl) ? i : -1;
            } // if
            else if(groupStart < 0) {
                groupStart = i;
            }
        } // for

        for( int i = 0; i < items.size(); ++i ) {
            final Item item = items.get(i);
            if(i > 0 && item.blankBefore) out.append('\n');
            for(final String comment : item.leadingComments) out.append(
                indent(depth)
            ).append(
                FormatterSimpleBraced.reindentBlockComment( comment, indent(depth) )
            ) .append(
                '\n'
            );
            if(item.rawFrozen != null) {
                out.append(item.rawFrozen).append('\n');
                continue;
            }
            if(item.decl == null && item.rule == null) continue; // Dangling trailing-comment placeholder
            out.append( indent(depth) );
            if(item.rule != null) {
                out.append(item.rule.header).append(" {\n");
                renderItems(item.rule.body, depth + 1, out);
                out.append( indent(depth) ).append('}');
            }
            else if(item.decl.hasColon) {
                if(item.decl.midComment != null) out.append(
                    item.decl.prop
                ).append(
                    ' '
                ).append(
                    item.decl.midComment
                ).append(
                    " : "
                ) .append(
                    item.decl.value
                ).append(
                    ';'
                );
                else out.append(
                    item.decl.prop
                ).append(
                    padding[i] != null ? padding[i] : " "
                ) .append(
                    ": "
                ).append(
                    item.decl.value
                ).append(
                    ';'
                );
            }
            else {
                out.append(item.decl.prop).append(';');
            }
            if(item.trailingComment != null) out.append(' ').append(item.trailingComment);
            out.append('\n');
        } // for
    }

    /** Tokenizes, parses, and re-renders {@code content} per STYLE_DATA_FORMATS.md §3. */
    public String format(final String content)
    {
        final List<Token> tokens = new CssTokenizer().tokenize(content);
        com.jxmake.formatter.tokenizer.TokenizerCore.markFrozenSpans(tokens, false);
        final Cursor        c        = new Cursor(tokens);
        final List<Item>    topLevel = parseBlockBody(c, true);
        final StringBuilder out      = new StringBuilder();
        renderItems(topLevel, 0, out);

        return out.toString();
    }

} // class CssSpecificRule
