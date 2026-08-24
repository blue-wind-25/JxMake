/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.evaluator;

import java.util.List;
import java.util.function.Predicate;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;

public class ComplexityPaddingEvaluator {

    /**
     * STYLE_KOTLIN.md §17: `Type.(Params) -> ReturnType` is a single function-type token, not a
     * nested-paren construct -- its own `(...)` (the receiver's invocation-site parameter list,
     * empty or populated) must not itself count as "nesting" when this content is examined as the
     * inside of some *enclosing* `(...)`/`[...]` (e.g. a parenthesized type annotation wrapping
     * the whole function type, `val block: (StringBuilder.() -> Unit) = {}`). Detected purely by
     * local token shape -- a `(` immediately preceded by `.` whose matching `)` is immediately
     * followed by `->` -- with no language check, since C/C++/Java never produce a `.` directly
     * followed by `(` in this position (Java has no `Type.() -> Ret` syntax at all), making this a
     * pure no-op for those languages, same reasoning as `MiscRuleCore.TIGHT_PAREN_KEYWORDS`'s Kotlin
     * `"when"` addition (RDD_KEY_100). Only the receiver-parens span itself is skipped over (not
     * recursed into) -- a function type genuinely nested *inside* one of its own parameters (e.g.
     * `((Int) -> String, Boolean) -> Unit`) is STYLE_KOTLIN.md §17's own documented "Known gap"
     * and intentionally still falls back to the default (loose) behavior below.
     */
    public boolean isLoose(final List<Token> contentTokens)
    {
        final int n = contentTokens.size();
        for(int i = 0; i < n; ++i) {
            final Token t = contentTokens.get(i);
            if( t.type == TokenType.PUNCT && "(".equals(t.text) ) {
                if( isReceiverFunctionTypeParens(contentTokens, i) ) {
                    final int closeIdx = matchParen(contentTokens, i);
                    if(closeIdx >= 0) {
                        i = closeIdx;
                        continue;
                    }
                } // if
                return true;
            } // if
            if( t.type == TokenType.PUNCT && "[".equals(t.text) ) return true;
            // A segmented JS/TS template literal's `${...}` hole (`TEMPLATE_HOLE_OPEN`/
            // `TEMPLATE_HOLE_CLOSE`, emitted by `TokenizerCurly.emitTemplateLiteralSegmented`
            // whenever `lang.isJsxSyntax` -- widened to plain `.js`/`.mjs`/`.cjs` files too, see
            // `Lang.isJsxSyntax`'s javadoc/STATE_JS_TS.md's 2026-08-13 implementation section) can
            // contain a real nested call/array expression of its own (e.g.
            // `` `${pico.green(`x`)}` ``'s inner `pico.green(...)` call). Before segmentation, the
            // whole template literal was one opaque STRING token this evaluator never looked
            // inside, so such an argument always evaluated as simple/tight; skipping the hole's
            // interior here (rather than recursing into it) preserves that exact same tight
            // outcome for the *enclosing* call's own padding decision -- the template literal
            // itself is still, as a whole, one argument value, not "a call containing nested
            // parens" from the enclosing call's point of view. (The hole's own interior expression
            // gets its own, separate complexity-padding pass when it is itself later examined as
            // the inside of a real `(`/`[` -- this skip only affects how the literal looks from
            // the *outside*.)
            if(t.type == TokenType.TEMPLATE_HOLE_OPEN) {
                final int closeIdx = matchTemplateHole(contentTokens, i);
                if(closeIdx >= 0) i = closeIdx;
            }
        } // for

        return false;
    }

    private int matchTemplateHole(final List<Token> tokens, final int openIdx)
    {
        return matchDepth(
            tokens, openIdx, t -> t.type == TokenType.TEMPLATE_HOLE_OPEN,
            t -> t.type == TokenType.TEMPLATE_HOLE_CLOSE
        );
    }

    private boolean isReceiverFunctionTypeParens(final List<Token> tokens, final int openIdx)
    {
        final int prevIdx = prevSignificant(tokens, openIdx - 1);
        if( prevIdx < 0 || tokens.get(
            prevIdx
        ).type != TokenType.OP || !".".equals(
            tokens.get(prevIdx).text
        ) ) return false;
        final int closeIdx = matchParen(tokens, openIdx);
        if(closeIdx < 0) return false;
        final int nextIdx = nextSignificant(tokens, closeIdx + 1);

        return nextIdx >= 0 && tokens.get(
            nextIdx
        ).type == TokenType.OP && "->".equals(
            tokens.get(nextIdx).text
        );
    }

    private int matchParen(final List<Token> tokens, final int openIdx)
    {
        return matchDepth(
            tokens, openIdx, t -> t.type == TokenType.PUNCT && "(".equals(t.text),
            t -> t.type == TokenType.PUNCT && ")".equals(t.text)
        );
    }

    /** Scans forward from {@code openIdx}, tracking nesting depth via {@code isOpen}/{@code isClose}, and returns the index where depth returns to 0 (or -1) */
    private int matchDepth(
        final List<Token>      tokens,
        final int              openIdx,
        final Predicate<Token> isOpen,
        final Predicate<Token> isClose
    )
    {
        int depth = 0;
        for( int i = openIdx; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( isOpen.test(t) ) {
                ++depth;
            }
            else if( isClose.test(t) ) {
                --depth;
                if(depth == 0) return i;
            }
        } // for

        return -1;
    }

    private int prevSignificant(final List<Token> tokens, final int fromIdx)
    {
        for(int i = fromIdx; i >= 0; --i) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    private int nextSignificant(final List<Token> tokens, final int fromIdx)
    {
        for( int i = fromIdx; i < tokens.size(); ++i ) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

} // class ComplexityPaddingEvaluator
