/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

/**
 * Slim, language-family-agnostic base for every scope-pipeline sibling ({@link ScopePipelineCurly}
 * for C/C++/Java/Kotlin, and {@code ScopePipelineIndent} for Python3). Holds only what every
 * family's splice-back needs regardless of scoping shape: the {@link Span}/{@link Replacement}
 * models, the splice/indent/whitespace primitives, and generic token-scanning helpers with no
 * curly-specific branching. Family-specific span-splitting, pass-driving, and recursion (the four
 * STYLE.md §5/§6/§8/§14 passes, Kotlin-vs-C/C++/Java branches, etc.) live in the sibling classes,
 * not here.
 */
public abstract class ScopePipelineCore {

    protected final int indentWidth;

    protected ScopePipelineCore(final int indentWidth)
    {
        this.indentWidth = indentWidth;
    }

    public abstract String process(String source);

    /**
     * One top-level (depth-0) span of a scope's token list: either a `;`-terminated statement,
     * or a `{ }`-block-terminated member, plus any same-line trailing comment. {@code end} is
     * exclusive. {@code openBraceIdx}/{@code closeBraceIdx} are -1 for a statement span;
     * otherwise they are this span's own top-level brace pair.
     */
    protected static final class Span {

        final int start;
        final int end;
        final int openBraceIdx;
        final int closeBraceIdx;

        Span(final int start, final int end, final int openBraceIdx, final int closeBraceIdx)
        {
            this.start         = start;
            this.end           = end;
            this.openBraceIdx  = openBraceIdx;
            this.closeBraceIdx = closeBraceIdx;
        }

    } // class Span

    /** One contiguous source-text replacement, by token-index range (end exclusive) */
    protected static final class Replacement {

        final int    start;
        final int    end;
        final String text;

        Replacement(final int start, final int end, final String text)
        {
            this.start = start;
            this.end   = end;
            this.text  = text;
        }

    } // class Replacement

    protected Span findSpanContaining(final List<Span> spans, final int idx)
    {
        for(final Span s : spans) {
            if(s.start <= idx && idx < s.end) return s;
        }

        return null;
    }

    /**
     * Reassembles {@code tokens}' source text, substituting each {@code replacements} range
     * (assumed sorted by {@code start}, non-overlapping) and passing every other token through
     * verbatim
     */
    protected String splice(final List<Token> tokens, final List<Replacement> replacements)
    {
        final StringBuilder out = new StringBuilder();
        final int           n   = tokens.size();
              int           i   = 0;
              int           r   = 0;
        while(i < n) {
            if( r < replacements.size() && replacements.get(r).start == i ) {
                final Replacement rep = replacements.get(r);
                out.append(rep.text);
                i = rep.end;
                ++r;
                continue;
            } // if
            out.append( tokens.get(i).text );
            ++i;
        } // while

        return out.toString();
    }

    protected String joinText(final List<Token> tokens, final int from, final int to)
    {
        final StringBuilder sb = new StringBuilder();
        for(int i = from; i < to; ++i) sb.append( tokens.get(i).text );

        return sb.toString();
    }

    /**
     * The indentation of the line a leading gap ends on -- the text after its last `\n`, or the
     * whole gap if it contains none. Truncated at the first non-space/non-tab character: a gap
     * can legitimately contain a same-line leading comment before the first declaration in a
     * group (e.g. STL's `/* [[no_unique_address]] *&#47; _Vw _Range;`), and that comment text is
     * not real indentation -- callers use this value both to compute the group's own visual
     * `leadingGap` (comment included, correctly, via the separate `rawLeadingGap` string) *and*
     * as the per-line join separator between every sibling declaration/assignment in the same
     * group. Without truncation, a same-line leading comment on just the *first* member leaked
     * into that join separator and got silently duplicated onto every subsequent sibling line --
     * found via `microsoft/STL` real-code testing (`ranges.hpp`'s `/* [[no_unique_address]] *&#47;
     * _Vw _Range;` immediately followed by un-commented `_Count`/`_Remainder` siblings in the same
     * alignment group), and the root cause of the "declaration-alignment column-padding
     * non-idempotency" gap (the duplicated-vs-not-yet-duplicated comment changes the raw leading
     * gap's character count between rounds, in turn changing whether `applyDeclarationsPass`'s
     * own idempotent-strip heuristic treats it as pre-existing padding).
     */
    protected String trailingIndent(final String gap)
    {
        final int    nl           = gap.lastIndexOf('\n');
        final String afterNewline = nl >= 0 ? gap.substring(nl + 1) : gap;
              int    end          = 0;
        while( end < afterNewline.length() && ( afterNewline.charAt(
            end
        ) == ' ' || afterNewline.charAt(
            end
        ) == '\t' ) ) end++;

        return afterNewline.substring(0, end);
    }

    /** Count of leading `' '` characters in {@code s} (0 if it doesn't start with one) */
    protected int leadingSpaceCount(final String s)
    {
        int count = 0;
        while( count < s.length() && s.charAt(count) == ' ' ) count++;

        return count;
    }

    /** Removes up to {@code n} trailing `' '` characters from the end of {@code s} */
    protected String stripTrailingSpaces(final String s, final int n)
    {
        int end       = s.length();
        int remaining = n;
        while( remaining > 0 && end > 0 && s.charAt(end - 1) == ' ' ) {
            --end;
            --remaining;
        }

        return s.substring(0, end);
    }

    /**
     * Rounds `rawIndent` (spaces/tabs) up to the nearest multiple of {@link #indentWidth}.
     * Returns `rawIndent` unchanged when it is already a valid indentation (zero, or a positive
     * multiple of indentWidth).  Only non-zero non-multiples (e.g. 2-space source) are touched.
     */
    protected String normalizeIndent(final String rawIndent)
    {
        int width = 0;
        for( int i = 0; i < rawIndent.length(); ++i ) {
            final char c = rawIndent.charAt(i);
            if(c == '\t') width = ( (width / indentWidth) + 1 ) * indentWidth;
            else          width++;
        }
        // Zero-width is valid (global/top-level scope, column 0).  Multiples of indentWidth
        // are valid.  Only round up a non-zero non-multiple (malformed indentation in source).
        if(width == 0 || width % indentWidth == 0) return rawIndent;
        final int           normalized = ( (width + indentWidth - 1) / indentWidth )* indentWidth;
        final StringBuilder sb         = new StringBuilder(normalized);
        for(int i = 0; i < normalized; ++i) sb.append(' ');

        return sb.toString();
    }

    /**
     * Returns a `leadingGap` that ends with `normalizedIndent` on its final line.  Only acts
     * when `leadingGap` already has a newline (multi-line indented content); if `leadingGap`
     * has no newline the content is inline and the gap is left unchanged -- callers that need
     * to expand a one-liner named-scope body pre-process it before calling processScope.
     */
    protected String normalizeLeadingGap(
        final String leadingGap,
        final String rawIndent,
        final String normalizedIndent
    )
    {
        if( rawIndent.equals(normalizedIndent) ) return leadingGap;
        final int nl = leadingGap.lastIndexOf('\n');
        if(nl < 0) return leadingGap;

        return leadingGap.substring(0, nl + 1) + normalizedIndent;
    }

    /**
     * Strips trailing spaces/tabs/newlines/carriage-returns from {@code s} -- used to discard a
     * scope's original, unnormalized gap before its closing `}` so a fresh {@code "\n" + indent}
     * can be appended in its place
     */
    protected String trimTrailingWhitespace(final String s)
    {
        int end = s.length();
        while(end > 0) {
            final char c = s.charAt(end - 1);
            if(c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
            --end;
        }

        return s.substring(0, end);
    }

    /**
     * Counts the {@code '\n'} characters in the pure-whitespace run at the very end of {@code s}
     * (what {@link #trimTrailingWhitespace} would strip) -- one for a plain trailing newline,
     * two or more when genuine blank source line(s) sat in the gap being force-reindented
     */
    protected int trailingRunNewlineCount(final String s)
    {
        int newlineCount = 0;
        int i            = s.length() - 1;
        while(i >= 0) {
            final char c = s.charAt(i);
            if(c != ' ' && c != '\t' && c != '\n' && c != '\r') break;
            if(c == '\n') newlineCount++;
            --i;
        }

        return newlineCount;
    }

    protected Map<Token, Integer> buildIndexMap(final List<Token> tokens)
    {
        final Map<Token, Integer> indexOf = new IdentityHashMap<>();
        for( int i = 0; i < tokens.size(); ++i ) indexOf.put( tokens.get(i), i );

        return indexOf;
    }

    /** One indentation level's worth of literal spaces, per {@link #indentWidth} */
    protected String indentUnit()
    {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < indentWidth; ++i) sb.append(' ');

        return sb.toString();
    }

    /**
     * Like {@code Token.isGapToken} but excludes comments -- used to trim a declaration/assignment
     * group's replaced span down to its true trailing content without eating a same-line trailing
     * comment that the group's own rendered {@code text} did NOT already re-include verbatim
     * (unlike a mid-group member, the group's *last* member's trailing comment is only captured
     * once, by {@code Declaration.trailingComment}/{@code Assignment.trailingComment} and rendered
     * into `text` -- if this trim treated the comment as trimmable "gap" too, it would stay behind
     * in the untouched source right after the replaced span, duplicating it in the output).
     */
    protected boolean isWhitespaceOrNewline(final Token t)
    {
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
    protected boolean hasTopLevelNewline(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toExclusive
    )
    {
        int depth = 0;
        for(int i = fromInclusive; i < toExclusive; ++i) {
            final Token t = tokens.get(i);
            if( t.type == TokenType.PUNCT && ( "(".equals(t.text) || "[".equals(t.text) ) ) depth++;
            else if( t.type == TokenType.PUNCT && ( ")".equals(
                t.text
            ) || "]".equals(
                t.text
            ) ) ) depth--;
            else if(t.type == TokenType.NEWLINE && depth == 0) return true;
        } // for

        return false;
    }

    protected boolean anyFrozen(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toExclusive
    )
    {
        for(int i = fromInclusive; i < toExclusive; ++i) {
            if( tokens.get(i).frozen ) return true;
        }

        return false;
    }

    /**
     * True iff a {@code COMMENT_LINE}/{@code COMMENT_BLOCK} token sits anywhere in the pure-gap
     * run immediately before {@code closeBraceIdx} (i.e. between it and the nearest preceding
     * non-gap token).
     */
    protected boolean trailingGapHasComment(final List<Token> tokens, final int closeBraceIdx)
    {
        for(int i = closeBraceIdx - 1; i >= 0; --i) {
            final TokenType ty = tokens.get(i).type;
            if(ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) return true;
            if( !isGapToken( tokens.get(i) ) ) return false;
        }

        return false;
    }

    protected int prevSignificantIndex(final List<Token> tokens, final int from)
    {
        for(int i = from - 1; i >= 0; --i) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    protected int nextSignificantIndex(final List<Token> tokens, final int from)
    {
        for( int i = from + 1; i < tokens.size(); ++i ) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    protected int matchParenForward(final List<Token> tokens, final int openIdx)
    {
        int depth = 0;
        for( int i = openIdx; i < tokens.size(); ++i ) {
            if( isPunct( tokens.get(i), "(" ) ) {
                ++depth;
            }
            else if( isPunct( tokens.get(i), ")" ) ) {
                --depth;
                if(depth == 0) return i;
            }
        } // for

        return -1;
    }

    protected int matchParenBackward(final List<Token> tokens, final int closeIdx)
    {
        int depth = 0;
        for(int i = closeIdx; i >= 0; --i) {
            if( isPunct( tokens.get(i), ")" ) ) {
                ++depth;
            }
            else if( isPunct( tokens.get(i), "(" ) ) {
                --depth;
                if(depth == 0) return i;
            }
        } // for

        return -1;
    }

    protected int matchBraceForward(final List<Token> tokens, final int openIdx)
    {
        int depth = 0;
        for( int i = openIdx; i < tokens.size(); ++i ) {
            if( isPunct( tokens.get(i), "{" ) ) {
                ++depth;
            }
            else if( isPunct( tokens.get(i), "}" ) ) {
                --depth;
                if(depth == 0) return i;
            }
        } // for

        return -1;
    }

} // class ScopePipelineCore
