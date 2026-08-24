/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.List;
import java.util.Map;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isComment;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;

/**
 * Shared helper logic that was structurally identical, byte-for-byte, across a subset of
 * {@link JavaSpecificRule}, {@link CppSpecificRule}, {@link KotlinSpecificRule}, and
 * {@link JsTsSpecificRule} (plus, for the at-or-before/at-or-after pair, {@code MiscRuleCore}):
 * plain {@link Token}/{@link TokenType} navigation and rendering that carries no per-language
 * semantics (unlike, say, each class's own {@code lineIndent}, which stays intentionally
 * duplicated per this file's established no-shared-helpers-across-rule-classes precedent -- see
 * {@code MiscRuleCurly.hasCommentBetween}'s doc comment). Not every method here is used by every
 * one of those classes; each keeps its own private delegating wrapper (or, for {@code
 * MiscRuleCore}, a protected one, since its own subclasses call it too) for whichever subset it
 * needs, same shape as {@code ToolingSharedRule}'s delegation pattern.
 */
final class TokenNavigationRule {

    private TokenNavigationRule()
    {
    }

    /** Index of the first significant token on the same physical line as {@code idx} */
    static int lineStartIndex(final List<Token> tokens, final int idx)
    {
        int newlineIdx = -1;
        for(int i = idx; i >= 0; --i) {
            if( tokens.get(i).type == TokenType.NEWLINE ) {
                newlineIdx = i;
                break;
            }
        }
        final int firstSig = nextSignificantIndexAfter(tokens, newlineIdx);

        return firstSig < 0 ? idx : firstSig;
    }

    /**
     * Index of the nearest significant token strictly before {@code from} ({@code from} itself is
     * never returned, unlike {@link #prevSignificantIndexAtOrBefore}'s at-or-before scan), or
     * {@code -1} if none
     */
    static int prevSignificantIndexBefore(final List<Token> tokens, final int from)
    {
        for(int i = from - 1; i >= 0; --i) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    /**
     * Index of the nearest significant token strictly after {@code from} ({@code from} itself is
     * never returned, unlike {@link #nextSignificantIndexAtOrAfter}'s at-or-after scan), or
     * {@code -1} if none
     */
    static int nextSignificantIndexAfter(final List<Token> tokens, final int from)
    {
        for( int i = from + 1; i < tokens.size(); ++i ) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    /**
     * Index of the nearest significant token at-or-after {@code from} ({@code from} itself is
     * returned if it's already significant, unlike {@link #nextSignificantIndexAfter}'s
     * strictly-after scan), or {@code -1} if none
     */
    static int nextSignificantIndexAtOrAfter(final List<Token> tokens, final int from)
    {
        int i = from;
        while( i < tokens.size() && isGapToken( tokens.get(i) ) ) i++;

        return i < tokens.size() ? i : -1;
    }

    /**
     * Index of the nearest significant token at-or-before {@code from} ({@code from} itself is
     * returned if it's already significant, unlike {@link #prevSignificantIndexBefore}'s
     * strictly-before scan), or {@code -1} if none (including when {@code from} itself is
     * already negative)
     */
    static int prevSignificantIndexAtOrBefore(final List<Token> tokens, final int from)
    {
        int i = from;
        while( i >= 0 && isGapToken( tokens.get(i) ) ) i--;

        return i;
    }

    /**
     * True iff a tight-spacing rewrite is disallowed across {@code gap} (the gap tokens between
     * {@code lastSignificant} and {@code t}) because a comment, a newline, or a frozen token
     * (either inside the gap itself, or {@code lastSignificant}/{@code t} bookending it) is
     * present -- byte-identical shape previously reimplemented at every tight-spacing rewrite call
     * site across {@link JsTsSpecificRule}/{@link KotlinSpecificRule}
     */
    static boolean isGapBlocked(final List<Token> gap, final Token lastSignificant, final Token t)
    {
        return gap.stream().anyMatch(
            g->isComment(g) || g.type == TokenType.NEWLINE || g.frozen
        ) || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
    }

    /**
     * True iff {@code t} is an OP token consisting solely of `.`/`*` characters -- covers a plain
     * `.` separator, a plain `*` wildcard, and {@code TokenizerCurly}'s combined `.*` multi-char
     * pointer-to-member OP token (shared across all languages, since import/package path parsing
     * needs this same character-class check regardless of which curly language it's scanning).
     */
    static boolean isPathOp(final Token t)
    {
        if( t == null || t.type != TokenType.OP || t.text.isEmpty() ) return false;
        for( int i = 0; i < t.text.length(); ++i ) {
            final char c = t.text.charAt(i);
            if(c != '.' && c != '*') return false;
        }

        return true;
    }

    /** True iff {@code parts} starts with every element of {@code prefix}, in order */
    static boolean matchesPrefix(final String[] parts, final List<String> prefix)
    {
        if( parts.length < prefix.size() ) return false;
        for( int i = 0; i < prefix.size(); ++i ) {
            if( !parts[i].equals( prefix.get(i) ) ) return false;
        }

        return true;
    }

    /** Renders {@code tokens} with each entry in {@code overrides} substituted for that token's own text */
    static String render(final List<Token> tokens, final Map<Integer, String> overrides)
    {
        final StringBuilder out = new StringBuilder();
        for( int i = 0; i < tokens.size(); ++i ) {
            final String override = overrides.get(i);
            out.append( override != null ? override : tokens.get(i).text );
        }

        return out.toString();
    }

} // class TokenNavigationRule
