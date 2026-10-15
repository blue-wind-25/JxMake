/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;

import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

/**
 * C/C++ spacing around a single `*`/`&` inside an expression (a declaration initializer or an
 * assignment's right-hand side), shared by {@code DeclarationAlignmentRuleCore.renderInitTokens}
 * and {@code MiscRuleCore.renderExpressionTokens}. The operator is binary when its left neighbour
 * ends a value (identifier, number, literal, `]`, a non-cast `)`, a postfix `++`/`--`,
 * `sizeof(...)`) and something operand-like follows; otherwise it is unary (dereference /
 * address-of) and stays tight against its operand, spaced on the left only after an operator,
 * `?`/`:`, `,` or a keyword (RDD_KEY_375). The token lists are significant-only (no whitespace).
 */
final class CPointerOpSpacing {

    private static final int NONE   = 0;
    private static final int BINARY = 1;
    private static final int UNARY  = 2;

    /** Keywords followed by an expression, after which a unary `*`/`&` is spaced */
    private static final Set<String> EXPRESSION_KEYWORDS = new HashSet<>( Arrays.asList(
        "return", "case", "throw", "co_return", "co_yield", "co_await", "sizeof"
    ) );

    /** Operators that are binary only after a value (`a - b` vs `-b`) */
    private static final Set<String> AMBIGUOUS_OPS = new HashSet<>( Arrays.asList(
        "*", "&", "+", "-"
    ) );

    /** Operators after which a unary `*`/`&` stays tight (`!*p`, `++*p`, `a->*b`) */
    private static final Set<String> TIGHT_PREFIX_OPS = new HashSet<>( Arrays.asList(
        "!", "~", "++", "--", ".", "->", "::"
    ) );

    private CPointerOpSpacing()
    {
    }

    /** Keywords whose `(` opens an expression, not a parameter list (`if(a && b)`, `return(x)`) */
    private static final Set<String> EXPRESSION_PAREN_KEYWORDS = new HashSet<>( Arrays.asList(
        "if", "while", "for", "switch", "return", "case", "throw", "sizeof", "alignof", "decltype"
    ) );

    /**
     * Whether a space goes before {@code tokens.get(i)}, or null when neither it nor its left
     * neighbour is a single `*`/`&` and it is not a logical `&&` (the caller's own rules decide)
     */
    static Boolean spaceBefore(
        final List<Token>  tokens,
        final int          i,
        final IntPredicate castAt,
        final boolean      cpp
    )
    {
        if(i <= 0) return null;
        final Token t = tokens.get(i);
        if( isOp(t, "&&") ) return isLogicalAnd(tokens, i, castAt, cpp) ? Boolean.TRUE : null;
        final int self = classify(tokens, i, castAt);
        if(self == BINARY) return true;
        if(self == UNARY) return unaryNeedsSpaceBefore(tokens, i, castAt);
        final int left = classify(tokens, i - 1, castAt);
        if(left == BINARY) return true;
        if(left == UNARY) return false;

        return null;
    }

    /**
     * `&&` between a value and an operand is a logical AND (spaced, `f(x) && ok`); it is tight on
     * the left elsewhere as the C++ rvalue-reference sigil. In C++ `Foo&& x` inside a lambda or
     * function-type parameter list (`[](Foo&& x)`, `void(Foo&& x)`) has the same shape, so a `&&`
     * whose enclosing `(` follows `]` or a non-expression keyword stays tight there (RDD_KEY_376)
     */
    private static boolean isLogicalAnd(
        final List<Token>  tokens,
        final int          i,
        final IntPredicate castAt,
        final boolean      cpp
    )
    {
        final Token next = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
        if( !endsValue(tokens, i - 1, castAt) || !startsOperand(next) ) return false;

        return !cpp || !inParameterList(tokens, i);
    }

    private static boolean inParameterList(final List<Token> tokens, final int i)
    {
        int depth = 0;
        for(int k = i - 1; k >= 0; --k) {
            final Token t = tokens.get(k);
            if( isPunct(t, ")") ) ++depth;
            else if( isPunct(t, "(") && depth-- == 0 ) {
                if(k == 0) return false;
                final Token before = tokens.get(k - 1);
                if( isPunct(before, "]") ) return true; // Lambda `[...](...)`

                final boolean exprKeyword = EXPRESSION_PAREN_KEYWORDS.contains(before.text);

                return before.type == TokenType.KEYWORD && !exprKeyword;
            }
        } // for

        return false;
    }

    private static int classify(final List<Token> tokens, final int i, final IntPredicate castAt)
    {
        final Token t = tokens.get(i);
        if( !isOp(t, "*") && !isOp(t, "&") ) return NONE;
        final Token next = i + 1 < tokens.size() ? tokens.get(i + 1) : null;
        if( i > 0 && endsValue(tokens, i - 1, castAt) && startsOperand(next) ) return BINARY;

        return UNARY;
    }

    private static boolean unaryNeedsSpaceBefore(
        final List<Token>  tokens,
        final int          i,
        final IntPredicate castAt
    )
    {
        final Token prev = tokens.get(i - 1);
        // A type keyword (`(int*)`) keeps the declarator sigil tight
        if(prev.type == TokenType.KEYWORD) return EXPRESSION_KEYWORDS.contains(prev.text);
        if( isPunct(prev, ",") || "?".equals(prev.text) || ":".equals(prev.text) ) return true;
        if(prev.type != TokenType.OP) return false; // `(`, `[`, `{`, a cast's `)`
        if( TIGHT_PREFIX_OPS.contains(prev.text) ) return false;
        if( AMBIGUOUS_OPS.contains(prev.text) ) return i >= 2 && endsValue(tokens, i - 2, castAt);

        return true;
    }

    private static boolean endsValue(
        final List<Token>  tokens,
        final int          j,
        final IntPredicate isCast
    )
    {
        final Token t = tokens.get(j);
        if(t.type == TokenType.IDENTIFIER || t.type == TokenType.NUMBER) return true;
        if(t.type == TokenType.STRING || t.type == TokenType.CHAR) return true;
        if(t.type == TokenType.KEYWORD) return "this".equals(t.text) || "nullptr".equals(t.text);
        if( isPunct(t, "]") ) return true;
        // Postfix `++`/`--` ends a value (`n++ & mask`)
        if( isOp(t, "++") || isOp(t, "--") ) return j > 0 && isOperandEnd( tokens.get(j - 1) );
        if( isPunct(t, ")") ) return !isCast.test(j) || isCallLikeParens(tokens, j);

        return false;
    }

    private static boolean isOperandEnd(final Token t)
    {
        return t.type == TokenType.IDENTIFIER || isPunct(t, ")") || isPunct(t, "]");
    }

    /**
     * True iff the `)` at {@code closeIdx} closes `sizeof(...)`/`alignof(...)`/`decltype(...)` or a
     * template call's argument list (`at<T>(i)`) -- shapes the cast check misreads as `(Type)`
     */
    private static boolean isCallLikeParens(final List<Token> tokens, final int closeIdx)
    {
        int depth = 0;
        for(int k = closeIdx; k >= 0; --k) {
            final Token t = tokens.get(k);
            if( isPunct(t, ")") ) ++depth;
            else if( isPunct(t, "(") && --depth == 0 ) {
                if(k == 0) return false;
                final Token before = tokens.get(k - 1);
                if(before.type == TokenType.ANGLE_BRACKET_CLOSE) return true;
                final String kw = before.text;

                return "sizeof".equals(kw) || "alignof".equals(kw) || "decltype".equals(kw);
            }
        } // for

        return false;
    }

    private static boolean startsOperand(final Token next)
    {
        if(next == null) return false;
        if( next.type == TokenType.PUNCT && ")],;}".contains(next.text) ) return false;
        if(next.type == TokenType.ANGLE_BRACKET_CLOSE) return false;

        return !( next.type == TokenType.OP && ( ">".equals(next.text) || "=".equals(next.text) ) );
    }

} // class CPointerOpSpacing
