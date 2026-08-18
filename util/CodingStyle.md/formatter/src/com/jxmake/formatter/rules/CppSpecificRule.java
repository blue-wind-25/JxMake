/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jxmake.formatter.FormatterSimpleBraced;
import com.jxmake.formatter.Lang;
import com.jxmake.formatter.evaluator.ComplexityPaddingEvaluator;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

/**
 * C/C++-specific STYLE_C_CPP.md sections not owned by another rule class: §1, §2 (Allman
 * conversion), §3, §4, §9, §10, §11. (§5/§6 are already handled by {@code MiscRuleCurly}/
 * {@code DeclarationAlignmentRuleCurly}; §7's additional C/C++ closing-comment cases and the lambda
 * part of §2 are already handled by {@code BlockStructureRule}; §8 is explicitly
 * preserve-as-is.)
 */
public class CppSpecificRule {

    /** STYLE_C_CPP.md §10: number of blank lines required between header zones. */
    private static final int HEADER_ZONE_BLANK_LINES = 2;

    private final Lang lang;
    private final int  lineLengthLimit;

    /**
     * One indentation level, used by {@link #enforceRequiresClausePlacement} when wrapping a
     *  trailing `requires` clause to its own line -- built from the configured `indent-size`
     *  (see the constructor), not a hardcoded literal, same bug class as
     *  `SwitchRule.deriveUnit`'s own former fallback.
     */
    private final String indentUnit;

    /**
     * Shared evaluator for {@link #enforceAttributeAndSpliceBracketPadding}'s tight/loose
     *  decision -- {@code isLoose} already works unmodified on `[[ ]]`/`[: :]` interior content
     *  since it only looks for nested PUNCT `(`/`[` tokens, and a call's own `(` is exactly that
     */
    private static final ComplexityPaddingEvaluator ATTRIBUTE_COMPLEXITY_EVALUATOR = new ComplexityPaddingEvaluator();

    public CppSpecificRule(final Lang lang)
    {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public CppSpecificRule(final Lang lang, final int lineLengthLimit)
    {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public CppSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth)
    {
        this.lang            = lang;
        this.lineLengthLimit = lineLengthLimit;
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < indentWidth; ++i) sb.append(' ');
        this.indentUnit = sb.toString();
    }

    /**
     * STYLE_C_CPP.md §1: C always writes an empty parameter list as `(void)`; C++ always omits
     * it (`()`). Resolved -- see STATE.md "§1 empty parameter list": rather than build general
     * declaration-vs-call detection (no STYLE.md worked example exercises that distinction, and
     * the cost of guessing wrong here is corrupting working code, not just a missed format), the
     * two directions are handled with two different, independently-safe signals:
     * <ul>
     * <li>C, `()` → `(void)`: only rewritten when the matching `)` is directly followed by `{`
     * (skipping whitespace/comments) -- i.e. only recognized function <b>definitions</b>, the
     * same "identifier before `(`, body brace after `)`" signal already used elsewhere in this
     * codebase (see {@code BlockStructureRule}'s function-body brace detection) to distinguish a
     * definition from a call without an AST, since a call is never followed by `{`. A bare
     * prototype (`void foo();`, no body) is a documented, deliberate gap -- it is structurally
     * identical to a call (`foo();`) without a body-brace signal to anchor on, so it is left
     * untouched rather than guessed at.</li>
     * <li>C++, `(void)` → `()`: rewritten wherever it occurs, body or no body, with no extra
     * signal needed -- `IDENTIFIER(void)` is never valid call syntax (`void` cannot be passed as
     * an argument), so every occurrence directly after a candidate function name is a signature,
     * by construction of the language itself.</li>
     * </ul>
     * Both directions require the identifier immediately before `(` to not itself be preceded by
     * `new` (excludes a constructor call via `new Identifier()`, same guard already used by
     * {@code BlockStructureRule.isFunctionBodyBrace}), and both skip the rewrite entirely if a
     * comment sits between the parens (nothing to guess at structurally, but consistent with this
     * file's existing "a comment in the gap blocks the rewrite" posture).
     */
    public String enforceEmptyParameterList(final List<Token> tokens)
    {
        final Map<Integer, Integer> spans = new HashMap<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            if( !isPunct( tokens.get(i), "(" ) || !isCandidateSignatureName(tokens, i) ) continue;
            final int closeIdx = matchParenForward(tokens, i);
            if( closeIdx < 0 || hasCommentBetween(tokens, i, closeIdx) ) continue;
            if( MiscRuleCore.anyFrozen(tokens, i, closeIdx + 1) ) continue;
            if(lang.isC) {
                if( isEmptyBetween(
                    tokens, i, closeIdx
                ) && isFollowedByFunctionBody(
                    tokens, closeIdx
                ) ) spans.put(
                    i, closeIdx
                );
            } // if
            else if( isOnlyVoidBetween(
                tokens, i, closeIdx
            ) && !isFollowedByOpenParen(
                tokens, closeIdx
            ) ) {
                spans.put(i, closeIdx);
            }
        } // for

        final StringBuilder out = new StringBuilder();
              int           i   = 0;
        while( i < tokens.size() ) {
            final Integer closeIdx = spans.get(i);
            if(closeIdx != null) {
                out.append(lang.isC ? "(void)" : "()");
                i = closeIdx + 1;
            }
            else {
                out.append( tokens.get(i).text );
                ++i;
            }
        } // while

        return out.toString();
    }

    /**
     * True iff the token immediately before {@code openIdx} is an IDENTIFIER not itself preceded
     *  by `new` -- the candidate-function-name signal shared by both rewrite directions
     */
    private boolean isCandidateSignatureName(final List<Token> tokens, final int openIdx)
    {
        final int nameIdx = prevSignificantIndex(tokens, openIdx);
        if(nameIdx < 0) return false;
        final Token nameTok = tokens.get(nameIdx);
        if(nameTok.type == TokenType.IDENTIFIER) {
            final int beforeName = prevSignificantIndex(tokens, nameIdx);
            return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                    || !"new".equals( tokens.get(beforeName).text );
        }
        // Operator overloads: `operator<=>`, `operator==`, etc. — the OP token before `(` is
        // the operator symbol; the token before that must be the `operator` keyword.
        if(nameTok.type == TokenType.OP) {
            final int beforeOp = prevSignificantIndex(tokens, nameIdx);
            return beforeOp >= 0 && tokens.get(beforeOp).type == TokenType.KEYWORD
                    && "operator".equals( tokens.get(beforeOp).text );
        }

        return false;
    }

    /**
     * True iff {@code closeIdx} (a `)`) is directly followed by `{` -- the function-definition
     *  signal, never true for a call
     */
    private boolean isFollowedByFunctionBody(final List<Token> tokens, final int closeIdx)
    {
        final int next = nextSignificantIndex(tokens, closeIdx);

        return next >= 0 && isPunct( tokens.get(next), "{" );
    }

    /**
     * True iff {@code closeIdx} (a `)`) is directly followed by another `(` -- never true for a
     *  real C++ function declarator's empty parameter list (a bare `foo(void)` is followed by
     *  `;`, `{`, a trailing-const/noexcept/override specifier, or a trailing-return-type `->`,
     *  never by another paren group). Concept-emulation-macro conventions like range-v3's
     *  `CPP_ret(void)(requires ...)` (see `detail/prologue.hpp`) reuse this exact
     *  `IDENTIFIER(void)` shape as a macro invocation whose `void` argument is a real,
     *  meaningful macro parameter (the wrapped return type placeholder) -- not an empty
     *  parameter list -- so it must never be silently rewritten away here.
     */
    private boolean isFollowedByOpenParen(final List<Token> tokens, final int closeIdx)
    {
        final int next = nextSignificantIndex(tokens, closeIdx);

        return next >= 0 && isPunct( tokens.get(next), "(" );
    }

    private boolean isEmptyBetween(final List<Token> tokens, final int openIdx, final int closeIdx)
    {
        for(int i = openIdx + 1; i < closeIdx; ++i) {
            if( !isGapToken( tokens.get(i) ) ) return false;
        }

        return true;
    }

    private boolean isOnlyVoidBetween(
        final List<Token> tokens,
        final int         openIdx,
        final int         closeIdx
    )
    {
        int sigIdx = -1;
        for(int i = openIdx + 1; i < closeIdx; ++i) {
            if( isGapToken( tokens.get(i) ) ) continue;
            if(sigIdx != -1) return false;
            sigIdx = i;
        }

        return sigIdx != -1 && tokens.get(sigIdx).type == TokenType.KEYWORD
                && "void".equals( tokens.get(sigIdx).text );
    }

    private boolean hasCommentBetween(
        final List<Token> tokens,
        final int         fromExclusive,
        final int         toExclusive
    )
    {
        for(int i = fromExclusive + 1; i < toExclusive; ++i) {
            final TokenType type = tokens.get(i).type;
            if(type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) return true;
        }

        return false;
    }

    /**
     * True iff a {@code PREPROCESSOR} directive token sits anywhere in
     *  {@code (fromExclusive, toExclusive)} -- see
     *  {@link #enforceRequiresClausePlacement}'s preprocessor-in-clause check
     */
    private boolean hasPreprocessorBetween(
        final List<Token> tokens,
        final int         fromExclusive,
        final int         toExclusive
    )
    {
        for(int i = fromExclusive + 1; i < toExclusive; ++i) {
            if( tokens.get(i).type == TokenType.PREPROCESSOR ) return true;
        }

        return false;
    }

    /**
     * STYLE_C_CPP.md §2 (brace-placement half only): a recognized function <b>definition</b>'s
     * own brace moves to its own line (Allman) whenever it is currently K&amp;R/same-line -- the
     * exact inverse of {@code BlockStructureRule.enforceKAndRBraceStyle}, which deliberately
     * leaves this exact shape (`)` preceded by an IDENTIFIER) untouched today. Resolved -- see
     * STATE.md "§2 one-liner scope": asked the user whether the one-liner exception should ever
     * actively collapse an existing multi-line body down to a one-liner; the user chose
     * brace-placement only -- this method never inspects or changes how many physical lines the
     * body itself spans, it only ever relocates the opening `{`. That single rule already
     * reproduces both of STYLE_C_CPP.md §2's worked examples for free: a body that already sits
     * on one physical line (`{ _x = 0; }`) keeps that shape verbatim, just moved down a line; a
     * body that already spans multiple lines keeps that shape too.
     *
     * <p>Candidate signal reuses §1's {@code isCandidateSignatureName}: the `{` is directly
     * preceded (no comment, no newline in the gap) by a `)` whose matching `(` is itself preceded
     * by a candidate function name (IDENTIFIER, not itself preceded by `new`) -- this naturally
     * excludes every control-flow brace (`if`/`while`/`for`/`switch`/`catch` precede their `(`
     * with a KEYWORD, never an IDENTIFIER) and every lambda (a lambda's `{` is preceded by
     * `]`/a `)` whose matching `(` is preceded by `]`, or by a trailing return type -- never
     * directly by a bare `)`-after-identifier). A trailing qualifier or return type between `)`
     * and `{` (`void foo() const { ... }`, `auto foo() -> int { ... }`) is a documented,
     * deliberate gap, identical in spirit to the one already accepted for §9's
     * blank-line-before-return rule -- the immediate-predecessor check that excludes
     * control-flow/lambda braces also excludes these, and there is no STYLE.md worked example to
     * justify guessing past that signal. A `{` already on its own line (gap already contains a
     * NEWLINE) is left untouched -- idempotent, and the per-occurrence indentation target reuses
     * the closing `)`'s own line-leading indentation, which by STYLE.md §8's own rule ("closing
     * `)` ... indented to match the first character of the function signature itself") is correct
     * whether the signature is a single line or already broken across several.
     *
     * <p>RDD_KEY_75 (supersedes RDD_KEY_60): a one-liner function whose entire `{ ... }` body
     * sits on one physical line is deferred rather than converted immediately -- if it is
     * textually adjacent (no blank line, no comment-only gap) to another such one-liner, both are
     * left alone, mirroring {@code JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle}'s
     * identical fix (see that method's doc comment for the full rationale).
     */
    public String enforceFunctionDefinitionAllmanBraceStyle(final List<Token> tokens)
    {
        final Map<Integer, Integer> gapToBrace  = new HashMap<>();
        final Map<Integer, String>  gapToIndent = new HashMap<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            if( !isPunct( tokens.get(i), "{" ) ) continue;
            // Walk past post-paren qualifiers (const, volatile, noexcept, override, final)
            // so that e.g. `func() const {` is handled the same as `func() {`.
            int closeParenIdx = prevSignificantIndex(tokens, i);
            while( closeParenIdx >= 0 && isDefinitionQualifier(
                tokens.get(closeParenIdx)
            ) ) closeParenIdx = prevSignificantIndex(
                tokens, closeParenIdx
            );
            if( closeParenIdx < 0 || !isPunct( tokens.get(closeParenIdx), ")" ) ) {
                if(closeParenIdx >= 0) {
                    // Try trailing `requires` clause: `f() requires Clause {`
                    final int reqParen = findCloseParenBeforeRequiresClause(tokens, closeParenIdx);
                    if(reqParen >= 0) {
                        closeParenIdx = reqParen;
                    }
                    else {
                        // Possibly a trailing return type: `auto foo() -> ReturnType {`
                        closeParenIdx = findCloseParenBeforeTrailingReturnType(
                            tokens, closeParenIdx
                        );
                    }
                } // if
                if( closeParenIdx < 0 || !isPunct( tokens.get(closeParenIdx), ")" ) ) continue;
            } // if
            // Resolve past any constructor member-initializer list (`: init(val), ...`) to
            // the function's own close paren, so that the Allman brace is indented to the
            // function's own declaration line rather than the last initializer's line.
            final int funcCloseParen = resolveToFunctionCloseParen(tokens, closeParenIdx);
            if( !isFunctionDefinitionCloseParen(tokens, funcCloseParen) ) continue;
            // GapStart: first token after the last qualifier (or after ")"), so qualifiers
            // before "{" are preserved in the output and only the trailing whitespace is replaced
            final int lastBeforeBrace = prevSignificantIndex(tokens, i);
            // Idempotency: already on own line if there is a newline between the IMMEDIATE
            // preceding token (qualifier or last-initializer ")") and "{"
            if( hasNewlineOrCommentBetween(tokens, lastBeforeBrace, i) ) continue;
            final int gapStart = lastBeforeBrace + 1;
            // Keep single-line bodies K&R -- never Allman-convert a one-liner (RDD_KEY_75)
            final int closeBraceIdx = matchBraceForward(tokens, i);
            if( closeBraceIdx >= 0 && isSingleLineBody(tokens, i, closeBraceIdx) ) continue;
            if( MiscRuleCore.anyFrozen(tokens, funcCloseParen, i + 1) ) continue;
            gapToBrace.put(gapStart, i);
            // When the function's own `)` is on a different line than the immediate preceding
            // token (initializer-list case), store the correct indent from the function's line
            final String funcIndent    = lineIndent(tokens, funcCloseParen);
            final String defaultIndent = lineIndent(tokens, lastBeforeBrace);
            if( !funcIndent.equals(defaultIndent) ) gapToIndent.put(gapStart, funcIndent);
        } // for

        final StringBuilder out = new StringBuilder();
              int           i   = 0;
        while( i < tokens.size() ) {
            final Integer braceIdx = gapToBrace.get(i);
            if(braceIdx != null) {
                final String overrideIndent = gapToIndent.get(i);
                final String indent         = overrideIndent != null ? overrideIndent : lineIndent(
                    tokens, i - 1
                );
                out.append('\n').append(indent);
                out.append( tokens.get(braceIdx).text );
                i = braceIdx + 1;
            } // if
            else {
                out.append( tokens.get(i).text );
                ++i;
            }
        } // while

        return out.toString();
    }

    /**
     * Resolves the close-paren of a candidate function definition, walking backward past any
     *  constructor member-initializer list (`: init(val), ...`) to find the function's own `)`.
     *  Returns {@code candidate} itself when no initializer-list colon is found (normal case),
     *  or when a `{`/`}` scope boundary is crossed before finding one (stops at the boundary to
     *  avoid following `:` from a different function's initializer list).
     */
    private int resolveToFunctionCloseParen(final List<Token> tokens, final int candidate)
    {
        int depth = 0;
        for(int i = candidate; i >= 0; --i) {
            final Token t = tokens.get(i);
            if( isPunct(t, ")") || isPunct(t, "]") ) {
                ++depth;
            }
            else if( isPunct(t, "(") || isPunct(t, "[") ) {
                --depth;
            }
            else if( depth == 0 && ( isPunct(t, "{") || isPunct(t, "}") ) ) {
                // Crossed a scope boundary -- stop; no initializer-list in this candidate
                return candidate;
            }
            else if( depth == 0 && isOp(t, ":") ) {
                // Found the initializer-list colon. The function's own `)` is just before this.
                final int funcCloseParen = prevSignificantIndex(tokens, i);
                return ( funcCloseParen >= 0 && isPunct( tokens.get(funcCloseParen), ")" ) )
                        ? funcCloseParen : candidate;
            }
        } // for

        return candidate;
    }

    /**
     * True iff no {@code NEWLINE} token appears between {@code braceIdx} and {@code closeBraceIdx}
     *  inclusive -- the whole `{ ... }` span is one physical line.
     */
    private boolean isSingleLineBody(
        final List<Token> tokens,
        final int         braceIdx,
        final int         closeBraceIdx
    )
    {
        for(int i = braceIdx; i <= closeBraceIdx; ++i) {
            if( tokens.get(i).type == TokenType.NEWLINE ) return false;
        }

        return true;
    }

    /** Forward `{`/`}` bracket match -- the brace-pair analog of {@link #matchParenForward} */
    private int matchBraceForward(final List<Token> tokens, final int openIdx)
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

    /**
     * Scans backward from {@code fromIdx} through a trailing return type expression, tracking
     *  angle-bracket and paren depth, and returns the function's close paren that immediately
     *  precedes {@code ->} (skipping any post-paren qualifiers between {@code )} and
     *  {@code ->}), or -1 if no {@code ->} is found at depth 0 before a scope boundary
     */
    private int findCloseParenBeforeTrailingReturnType(final List<Token> tokens, final int fromIdx)
    {
        int depth = 0;
        for(int i = fromIdx; i >= 0; --i) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) continue;
            if( isPunct(t, ">") || t.type == TokenType.ANGLE_BRACKET_CLOSE ) {
                ++depth;
            }
            else if( isPunct(t, "<") || t.type == TokenType.ANGLE_BRACKET_OPEN ) {
                --depth;
            }
            else if( isPunct(t, ")") ) {
                ++depth;
            }
            else if( isPunct(t, "(") ) {
                --depth;
            }
            else if( depth == 0 && isOp(t, "->") ) {
                // Found the trailing return type arrow; skip any qualifiers and find `)`
                int beforeArrow = prevSignificantIndex(tokens, i);
                while( beforeArrow >= 0 && isDefinitionQualifier(
                    tokens.get(beforeArrow)
                ) ) beforeArrow = prevSignificantIndex(
                    tokens, beforeArrow
                );
                return ( beforeArrow >= 0 && isPunct(
                    tokens.get(beforeArrow), ")"
                ) ) ? beforeArrow : -1;
            }
            else if( depth == 0 && ( isPunct(t, "{") || isPunct(t, "}") || isPunct(t, ";") ) ) {
                // A `;` at depth 0 ends the *previous* statement -- e.g. a deduction guide
                // (`test(...) -> test<Test, TArg>;`) immediately followed by an unrelated
                // `struct suite {` must never let this backward scan cross into that prior
                // statement's own `->`/`)` (found via real-code testing, boost-ext/ut's
                // `struct suite` directly after `ut::test`'s deduction guide).
                return -1;
            }
        } // for

        return -1;
    }

    /**
     * Scans backward from {@code fromIdx} through the expression of a trailing {@code requires}
     *  clause and returns the function's close paren that immediately precedes the {@code requires}
     *  keyword, or -1 if no such pattern is found. Handles angle-bracket and paren depth to avoid
     *  mis-identifying a `)` inside the clause as the function's own close paren.
     */
    private int findCloseParenBeforeRequiresClause(final List<Token> tokens, final int fromIdx)
    {
        int depth = 0;
        for(int i = fromIdx; i >= 0; --i) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) continue;
            if(t.type == TokenType.ANGLE_BRACKET_CLOSE) { ++depth; }
            else if(t.type == TokenType.ANGLE_BRACKET_OPEN) { --depth; }
            else if( isPunct(t, ")") ) { ++depth; }
            else if( isPunct(t, "(") ) { --depth; }
            else if( depth == 0 && t.type == TokenType.KEYWORD && "requires".equals(t.text) ) {
                final int closeParen = prevSignificantIndex(tokens, i);
                return ( closeParen >= 0 && isPunct(
                    tokens.get(closeParen), ")"
                ) ) ? closeParen : -1;
            }
            // A `;` at depth 0 ends the previous statement -- must not let this backward scan
            // cross into it (same fix as findCloseParenBeforeTrailingReturnType above)
            else if( depth == 0 && ( isPunct(
                t, "{"
            ) || isPunct(
                t, "}"
            ) || isPunct(
                t, ";"
            ) ) ) { return -1; }
        } // for

        return -1;
    }

    private boolean isFunctionDefinitionCloseParen(
        final List<Token> tokens,
        final int         closeParenIdx
    )
    {
        final int openParenIdx = matchParenBackward(tokens, closeParenIdx);

        return openParenIdx >= 0 && isCandidateSignatureName(tokens, openParenIdx);
    }

    private boolean hasNewlineOrCommentBetween(
        final List<Token> tokens,
        final int         fromExclusive,
        final int         toExclusive
    )
    {
        for(int i = fromExclusive + 1; i < toExclusive; ++i) {
            final TokenType type = tokens.get(i).type;
            if(type == TokenType.NEWLINE || type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) return true;
        }

        return false;
    }

    /**
     * Line-leading whitespace of the physical line containing token {@code idx} -- "" if that
     *  line has no leading whitespace (column-0 start)
     */
    private String lineIndent(final List<Token> tokens, final int idx)
    {
        int newlineIdx = -1;
        for(int i = idx; i >= 0; --i) {
            if( tokens.get(i).type == TokenType.NEWLINE ) {
                newlineIdx = i;
                break;
            }
        }
        final int afterNewline = newlineIdx + 1;
        if( afterNewline < tokens.size() && tokens.get(
            afterNewline
        ).type == TokenType.WHITESPACE ) return tokens.get(
            afterNewline
        ).text;

        return "";
    }

    /**
     * STYLE_C_CPP.md §3: a single-level template angle-bracket pair stays tight
     * (`vector<int>`); any pair whose content (anywhere within its span, however deeply nested)
     * contains another `<>` pair gets exactly one space padded just inside both its own `<` and
     * its own `>` (`vector< vector<int> >`). Flagged in STATE.md as a correctness rule, not just
     * style -- the padding is what prevents two adjacent `>` characters belonging to different
     * pairs from being lexed as a single `>>` shift operator on pre-C++11 compilers -- so this is
     * fully specified already (unlike several other sections in this file, no
     * {@code AskUserQuestion} was needed): every matched pair is classified as tight or padded by
     * this one rule, with no remaining ambiguity to resolve.
     *
     * <p>Pair matching reuses the tokenizer's own {@code ANGLE_BRACKET_OPEN}/{@code _CLOSE}
     * disambiguation (the same token types {@code MiscRuleCurly.parseSignature} tracks for Java
     * generics) via a simple forward stack, since the tokenizer has already resolved any `>>`
     * lexing ambiguity at tokenize time -- by the time these tokens reach this rule, nesting is
     * already unambiguous and properly paired. A pair "contains another `<>` at any depth" iff at
     * least one other {@code ANGLE_BRACKET_OPEN} token lies strictly between its own open and
     * close indices, regardless of how many other tokens or how many nesting levels separate
     * them -- this is what correctly pads <i>every</i> ancestor pair on a 3+-level chain
     * (`A< B< C<int> > >`), not just the immediate outer pair, while leaving the innermost,
     * childless pair tight.
     *
     * <p>Rendering reuses the same gap-buffering technique as {@code MiscRuleCore}'s spacing passes
     * (e.g. {@code enforceInitializerBraceSpacing}): the gap immediately after a flagged
     * {@code ANGLE_BRACKET_OPEN} and immediately before a flagged {@code ANGLE_BRACKET_CLOSE} is
     * collapsed to exactly one space (zero width for an unflagged/tight pair's own open or
     * close), unless that gap contains a comment or newline, which blocks the rewrite for that
     * one side only -- consistent with this codebase's existing "a comment/newline in the gap
     * blocks the rewrite" posture throughout. Since the C tokenizer never emits
     * {@code ANGLE_BRACKET_OPEN}/{@code _CLOSE} tokens at all (templates don't exist in C), this
     * method is a no-op (output equals input) when {@code language} is `"c"`, with no separate
     * early-return guard needed.
     */
    public String enforceTemplateAngleBracketSpacing(final List<Token> tokens)
    {
        final Set<Integer> needsPadding = nestedAnglePairIndices(tokens);

        final StringBuilder out                = new StringBuilder();
        final List<Token>   gap                = new ArrayList<>();
              Token         lastSignificant    = null;
              int           lastSignificantIdx = -1;
        final int           n                  = tokens.size();
              int           i                  = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean afterOpen     = lastSignificant != null && lastSignificant.type == TokenType.ANGLE_BRACKET_OPEN;
            final boolean beforeClose   = t.type == TokenType.ANGLE_BRACKET_CLOSE;
            final boolean gapHasBlocker = hasCommentOrNewline(
                gap
            ) || t.frozen || (lastSignificant != null && lastSignificant.frozen);

            if( (afterOpen || beforeClose) && !gapHasBlocker ) {
                final boolean pad = ( afterOpen && needsPadding.contains(
                    lastSignificantIdx
                ) ) || ( beforeClose && needsPadding.contains(
                    i
                ) );
                if(pad) out.append(' ');
                gap.clear();
            } // if
            else {
                for(final Token g : gap) out.append(g.text);
                gap.clear();
            }

            // Always one literal char per angle token, never t.text: the tokenizer's `>>`
            // split keeps both characters on the first ANGLE_BRACKET_CLOSE token's text (">>")
            // and gives the second one a zero-width placeholder token right after, so any space
            // padding inserted between the two must land between two single-char emissions, not
            // after a 2-char one.
                 if(t.type == TokenType.ANGLE_BRACKET_OPEN)  out.append('<');
            else if(t.type == TokenType.ANGLE_BRACKET_CLOSE) out.append('>');
            else                                             out.append(t.text);
            lastSignificant    = t;
            lastSignificantIdx = i;
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    /**
     * Indices of every {@code ANGLE_BRACKET_OPEN}/{@code _CLOSE} token that is part of a
     *  matched pair whose span contains at least one other such pair -- the set of "needs
     *  padding" occurrences for {@link #enforceTemplateAngleBracketSpacing}
     */
    private Set<Integer> nestedAnglePairIndices(final List<Token> tokens)
    {
        final Deque<Integer>        openStack   = new ArrayDeque<>();
        final Map<Integer, Integer> openToClose = new HashMap<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            final TokenType ty = tokens.get(i).type;
                 if(ty == TokenType.ANGLE_BRACKET_OPEN) openStack.push(i);
            else if( ty == TokenType.ANGLE_BRACKET_CLOSE && !openStack.isEmpty() ) openToClose.put(
                openStack.pop(), i
            );
        } // for

        final Set<Integer> needsPadding = new HashSet<>();
        for( final Map.Entry<Integer, Integer> entry : openToClose.entrySet() ) {
            final int     openIdx   = entry.getKey();
            final int     closeIdx  = entry.getValue();
                  boolean hasNested = false;
            for(int j = openIdx + 1; j < closeIdx; ++j) {
                if( tokens.get(j).type == TokenType.ANGLE_BRACKET_OPEN ) {
                    hasNested = true;
                    break;
                }
            }
            if(hasNested) {
                needsPadding.add(openIdx);
                needsPadding.add(closeIdx);
            }
        } // for entry

        return needsPadding;
    }

    private boolean hasCommentOrNewline(final List<Token> gap)
    {
        for(final Token g : gap) {
            final TokenType type = g.type;
            if(type == TokenType.NEWLINE || type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) return true;
        }

        return false;
    }

    /**
     * STYLE_CPP26.md §1: pack indexing (`T...[i]`). No space between the pack name, `...`, and
     * the opening `[` -- both surrounding gaps are collapsed to zero width whenever an `...`
     * operator token is immediately (module gap tokens) followed by `[`, using the same
     * gap-buffering rewrite technique as {@link #enforceTemplateAngleBracketSpacing}. Scoped
     * narrowly to this exact `... [` adjacency so ordinary variadic-pack uses of `...` elsewhere
     * (e.g. `Args...)`, `Args...,`) are left untouched -- this method only ever removes space
     * immediately around an `...` that is itself followed by `[`, never any other occurrence.
     * The interior of the `[...]` itself is left to the existing tight/loose bracket-complexity
     * rule (STYLE.md §3.1), which already runs elsewhere and needs no pack-specific change.
     */
    public String enforcePackIndexingSpacing(final List<Token> tokens)
    {
        final int          n                     = tokens.size();
        final Set<Integer> ellipsisBeforeBracket = new HashSet<>();
        for(int i = 0; i < n; ++i) {
            if( !isOp( tokens.get(i), "..." ) ) continue;
            final int nextIdx = nextSignificantIndex(tokens, i);
            if( nextIdx >= 0 && isPunct( tokens.get(nextIdx), "[" ) ) ellipsisBeforeBracket.add(i);
        }
        if( ellipsisBeforeBracket.isEmpty() ) return MiscRuleCore.joinVerbatim(tokens);

        final StringBuilder out                   = new StringBuilder();
        final List<Token>   gap                   = new ArrayList<>();
              Token         lastSignificant       = null;
              boolean       lastWasEllipsisTarget = false;
              int           i                     = 0;
        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean beforeBracketAfterEllipsis    = lastWasEllipsisTarget && isPunct(t, "[");
            final boolean afterIdentifierBeforeEllipsis = ellipsisBeforeBracket.contains(i);
            final boolean gapHasBlocker                 = hasCommentOrNewline(
                gap
            ) || t.frozen || (lastSignificant != null && lastSignificant.frozen);

            if( (beforeBracketAfterEllipsis || afterIdentifierBeforeEllipsis) && !gapHasBlocker ) {
                gap.clear();
            }
            else {
                for(final Token g : gap) out.append(g.text);
                gap.clear();
            }

            out.append(t.text);
            lastSignificant       = t;
            lastWasEllipsisTarget = ellipsisBeforeBracket.contains(i);
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    /**
     * STYLE_CPP20.md §2.2/§2.3: a trailing `requires` clause on a function/template signature
     * always trails the closing `)` of the parameter list -- on the same line if the combined
     * line (the physical line the `)` already sits on, plus ` requires <clause>`) fits within
     * {@link #lineLengthLimit}, otherwise wrapped to its own line indented one level
     * under the function name's line-leading indent. Detection: a KEYWORD `requires` token whose
     * previous significant token is `)` is a trailing clause (it follows a parameter list);
     * anything else (`=`, or no previous token at all) is a requires-<i>expression</i> body
     * (`concept Drawable = requires(T t) { ... }`) -- left completely untouched, same posture as
     * STYLE_CPP20.md §2.1/§2.3's "nested compound requirements ... left completely untouched".
     *
     * <p>The clause's own end is the first `{` or `;` reached scanning forward from `requires`
     * -- a deliberate, non-AST signal, same conservative posture as the rest of this file: an ad
     * hoc requires-<i>expression</i> used as the entire trailing constraint (`f() requires
     * requires(T a) { a.foo(); }`) has no STYLE_CPP20.md worked example and is a documented gap,
     * not specially detected -- its own `{` would be (mis)read as the terminator. A comment
     * anywhere in the replaced span blocks the rewrite entirely, consistent with this file's
     * existing "a comment in the gap blocks the rewrite" posture.
     */
    public String enforceRequiresClausePlacement(final List<Token> tokens)
    {
        final List<int[]>  spans   = new ArrayList<>();
        final List<String> renders = new ArrayList<>();

        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( t.type != TokenType.KEYWORD || !"requires".equals(t.text) ) continue;
            final int closeParenIdx = prevSignificantIndex(tokens, i);
            if( closeParenIdx < 0 || !isPunct( tokens.get(closeParenIdx), ")" ) ) continue;
            final int clauseEndIdx = findRequiresClauseEnd(tokens, i);
            if(clauseEndIdx < 0 || clauseEndIdx <= i + 1) continue;
            if( hasCommentBetween(tokens, closeParenIdx, clauseEndIdx) ) continue;
            if( MiscRuleCore.anyFrozen(tokens, closeParenIdx, clauseEndIdx) ) continue;
            // A preprocessor directive (`#if`/`#else`/`#endif`) inside the clause's own
            // constraint-expression -- e.g. `requires(\n#if COND\n std::formattable<T, CharT>
            // \n#else\n ...\n#endif\n)` (microsoft/proxy's `proxy_fmt.h`/`proxy.h`) -- must stay
            // on its own physical line; collapseToOneLine below would otherwise splice it into
            // the middle of a rendered line, producing invalid, non-compiling C++ (a directive is
            // only ever recognized at the start of a physical line). Same "leave completely
            // untouched" posture as the comment/frozen-span checks above.
            if( hasPreprocessorBetween(tokens, closeParenIdx, clauseEndIdx) ) continue;

            // Use the parameter list's own OPENING paren's line as the declaration's real
            // leading line, not the closing paren's line -- when the source parameter list
            // already spans multiple lines with per-line continuation alignment (e.g. params
            // column-aligned under the function name), `)` can sit on a deeply-indented
            // continuation line that has nothing to do with the declaration's real leading
            // indent, and its collapsed single-line content varies depending on whether the
            // params have already been wrapped by a prior/future enforceCallLineBreaking pass
            // (which runs AFTER this one in FormatterCurly's Phase 1/2 ordering). Measuring fit off
            // of the CLOSE paren's own current physical line was therefore unstable across
            // passes: on a fresh source the params are still on their original (unwrapped)
            // line(s), so the combined length overflowed and `requires` was pushed to its own
            // line -- but on a reformat of already-wrapped output, `)` sits alone on a short
            // line, the combined length fits, and `requires` gets glued back onto it, diverging
            // from round1 (an idempotency bug). Deriving both baseIndent and the fit measurement
            // from the declarator's own opening-paren line instead is stable across passes: it
            // only depends on token content (collapsed to one line), never on incidental
            // wrapping already present in the input. Found via microsoft/proxy dogfood testing
            // (`allocate_proxy` initializer_list overloads in `include/proxy/v4/proxy.h`).
            //
            // The paren directly before `requires` isn't always the parameter list's own close
            // paren -- a trailing `noexcept(...)` exception-spec (or any other chained
            // parenthesized specifier) can sit between the parameter list and `requires`
            // (`foo(Args...) noexcept(expr) requires Clause`). Unwind through any such chain --
            // each closing paren's own opening paren, if immediately preceded by either another
            // `)` or a `noexcept` keyword that itself follows a `)`, is itself just another
            // specifier, not the declarator -- to reach the real parameter list's opening paren
            // (whose own predecessor is the function name, not another specifier). Using an
            // intermediate specifier's own opening-paren line (e.g. a `noexcept(` continuation
            // line, itself possibly awkwardly wrapped by an unrelated pass) as baseIndent would
            // reproduce the same class of instability this fix is for.
            int openParenIdx = matchParenBackward(tokens, closeParenIdx);
            while(openParenIdx >= 0) {
                final int beforeOpen = prevSignificantIndex(tokens, openParenIdx);
                if( beforeOpen >= 0 && isPunct( tokens.get(beforeOpen), ")" ) ) {
                    openParenIdx = matchParenBackward(tokens, beforeOpen);
                    continue;
                }
                if( beforeOpen >= 0 && tokens.get(beforeOpen).type == TokenType.KEYWORD
                        && "noexcept".equals( tokens.get(beforeOpen).text ) ) {
                    final int beforeSpecifier = prevSignificantIndex(tokens, beforeOpen);
                    if( beforeSpecifier >= 0 && isPunct( tokens.get(beforeSpecifier), ")" ) ) {
                        openParenIdx = matchParenBackward(tokens, beforeSpecifier);
                        continue;
                    }
                } // if
                break;
            } // while
            final int    declLineStartIdx = openParenIdx >= 0 ? lineStartIndex(
                tokens, openParenIdx
            ) : lineStartIndex(
                tokens, closeParenIdx
            );
            final String baseIndent       = lineIndent(tokens, declLineStartIdx);
            final String clauseExpr       = collapseToOneLine(tokens, i + 1, clauseEndIdx - 1);
            final String combined         = baseIndent + collapseToOneLine(
                tokens, declLineStartIdx, closeParenIdx
            ) + " requires " + clauseExpr;

            final String rendered = combined.length() <= lineLengthLimit ? " requires " + clauseExpr : "\n" + baseIndent + indentUnit + "requires " + clauseExpr;

            // End the replaced span just past the last non-gap token in the clause, so the
            // original whitespace/newline before `{`/`;` is preserved verbatim in the output.
            // This prevents overwriting a newline that an Allman-brace pass already placed there.
            int spanEnd = clauseEndIdx;
            while( spanEnd > i + 1 && isGapToken( tokens.get(spanEnd - 1) ) ) spanEnd--;

            spans.add( new int[] { closeParenIdx + 1, spanEnd } );
            renders.add(rendered);
        } // for

        if( spans.isEmpty() ) return MiscRuleCore.joinVerbatim(tokens);

        final StringBuilder out    = new StringBuilder();
              int           cursor = 0;
        for( int s = 0; s < spans.size(); ++s ) {
            final int[] span = spans.get(s);
            appendRange( out, tokens, cursor, span[0] );
            out.append( renders.get(s) );
            cursor = span[1];
        }
        appendRange( out, tokens, cursor, tokens.size() );

        return out.toString();
    }

    /**
     * STYLE_CPP26.md §4: a trailing group of one or more `pre`/`post` contract clauses on a
     * function signature -- comparable in shape to {@link #enforceRequiresClausePlacement}: each
     * clause gets its own line at {@code baseIndent + indentUnit}, unless the whole group plus
     * the signature fits on one physical line within {@link #lineLengthLimit}, in which case it
     * stays inline. Unlike `requires`, `pre`/`post` are plain identifiers (not tokenizer
     * keywords), so detection is purely positional: an identifier `pre` or `post` whose previous
     * significant token is `)` (or the `)` of a preceding clause in the same group) begins/
     * continues a group. `contract_assert` is handled separately, by
     * {@link #enforceContractAssertSpacing} -- it is a plain call-statement, not a trailing
     * signature clause, so it needs no placement logic, only the same expression-spacing
     * treatment as a `pre`/`post` clause's argument.
     *
     * <p>Each clause's own parenthesized content is re-spaced as a plain expression (single space
     * around every binary operator) since -- unlike ordinary call-statement arguments, which this
     * codebase leaves verbatim -- STYLE_CPP26.md's own worked examples show `pre`/`post` contents
     * always reformatted (`b!=0` to `b != 0`). For `post`, a top-level `:` (the result-binding
     * identifier) is rendered tight-before/space-after (`r: r * b == a`), matching STYLE.md's
     * ordinary identifier/colon spacing.
     */
    public String enforceContractClausePlacement(final List<Token> tokens)
    {
        final List<int[]>  spans               = new ArrayList<>();
        final List<String> renders             = new ArrayList<>();
        final Set<Integer> consumedCloseParens = new HashSet<>();

        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( t.type != TokenType.IDENTIFIER || !( "pre".equals(
                t.text
            ) || "post".equals(
                t.text
            ) ) ) continue;
            final int anchorCloseParenIdx = prevSignificantIndex(tokens, i);
            if( anchorCloseParenIdx < 0 || !isPunct(
                tokens.get(anchorCloseParenIdx), ")"
            ) || consumedCloseParens.contains(
                anchorCloseParenIdx
            ) ) continue;

            final List<String>  clauseKinds      = new ArrayList<>();
            final List<int[]>   clauseParenSpans = new ArrayList<>();
            final List<Integer> clauseKeywordIdx = new ArrayList<>();
                  int           cursor           = i;
                  boolean       bad              = false;
            while(true) {
                clauseKeywordIdx.add(cursor);
                final int openParenIdx = nextSignificantIndex(tokens, cursor);
                if( openParenIdx < 0 || !isPunct( tokens.get(openParenIdx), "(" ) ) {
                    bad = true;
                    break;
                }
                final int closeParenIdx = matchParenForward(tokens, openParenIdx);
                if(closeParenIdx < 0) {
                    bad = true;
                    break;
                }
                if( hasCommentBetween(
                    tokens, cursor, closeParenIdx
                ) || MiscRuleCore.anyFrozen(
                    tokens, cursor, closeParenIdx
                ) ) {
                    bad = true;
                    break;
                }
                clauseKinds.add( tokens.get(cursor).text );
                clauseParenSpans.add( new int[] { openParenIdx, closeParenIdx } );
                consumedCloseParens.add(closeParenIdx);

                final int afterClose = nextSignificantIndex(tokens, closeParenIdx);
                if( afterClose >= 0 && tokens.get(afterClose).type == TokenType.IDENTIFIER
                        && ( "pre".equals(
                            tokens.get(afterClose).text
                        ) || "post".equals(
                            tokens.get(afterClose).text
                        ) ) ) {
                    cursor = afterClose;
                    continue;
                }
                break;
            } // while
            if( bad || clauseKinds.isEmpty() ) continue;

            // Unwind through any chained specifier (e.g. `noexcept(...)`) between the parameter
            // list and the contract-clause group, same technique as
            // enforceRequiresClausePlacement, to reach the declarator's own opening paren for a
            // stable baseIndent.
            int openParenForIndent = matchParenBackward(tokens, anchorCloseParenIdx);
            while(openParenForIndent >= 0) {
                final int beforeOpen = prevSignificantIndex(tokens, openParenForIndent);
                if( beforeOpen >= 0 && isPunct( tokens.get(beforeOpen), ")" ) ) {
                    openParenForIndent = matchParenBackward(tokens, beforeOpen);
                    continue;
                }
                if( beforeOpen >= 0 && tokens.get(beforeOpen).type == TokenType.KEYWORD
                        && "noexcept".equals( tokens.get(beforeOpen).text ) ) {
                    final int beforeSpecifier = prevSignificantIndex(tokens, beforeOpen);
                    if( beforeSpecifier >= 0 && isPunct( tokens.get(beforeSpecifier), ")" ) ) {
                        openParenForIndent = matchParenBackward(tokens, beforeSpecifier);
                        continue;
                    }
                } // if
                break;
            } // while
            final int    declLineStartIdx = openParenForIndent >= 0 ? lineStartIndex(
                tokens, openParenForIndent
            ) : lineStartIndex(
                tokens, anchorCloseParenIdx
            );
            final String baseIndent       = lineIndent(tokens, declLineStartIdx);

            final List<String>      clauseRenders         = new ArrayList<>();
            final List<List<Token>> clauseLeadingComments = new ArrayList<>();
                  boolean           anyLeadingComment     = false;
            for( int c = 0; c < clauseKinds.size(); ++c ) {
                final int[]   pspan  = clauseParenSpans.get(c);
                final boolean isPost = "post".equals( clauseKinds.get(c) );
                final String  inner  = renderContractExpression(
                    tokens, pspan[0] + 1, pspan[1] - 1, isPost
                );
                clauseRenders.add( clauseKinds.get(c) + "(" + inner + ")" );

                final int         boundaryStart = c == 0 ? anchorCloseParenIdx : clauseParenSpans.get(
                    c - 1
                )[1];
                final List<Token> leading       = collectComments(
                    tokens, boundaryStart, clauseKeywordIdx.get(c)
                );
                clauseLeadingComments.add(leading);
                if( !leading.isEmpty() ) anyLeadingComment = true;
            } // for c

            final StringBuilder inlineJoined = new StringBuilder();
            for(final String r : clauseRenders) inlineJoined.append(' ').append(r);
            final String combined = baseIndent + collapseToOneLine(
                tokens, declLineStartIdx, anchorCloseParenIdx
            ) + inlineJoined;

            // A comment sitting between the signature (or a preceding clause) and this clause has
            // nowhere sensible to go on an inlined single-clause line, so its presence forces the
            // wrapped (one-clause-per-line) rendering regardless of whether the inline form would
            // otherwise fit -- same "can't silently drop it" posture as everywhere else a comment
            // interacts with a rewritten span in this file, except here the comment is preserved
            // instead of blocking the rewrite entirely
            final String rendered;
            if( clauseKinds.size() == 1 && combined.length() <= lineLengthLimit && !anyLeadingComment ) {
                rendered = inlineJoined.toString();
            }
            else {
                final StringBuilder wrapped = new StringBuilder();
                for( int c = 0; c < clauseRenders.size(); ++c ) {
                    for( final Token commentToken : clauseLeadingComments.get(c) ) {
                        final String text = commentToken.type == TokenType.COMMENT_BLOCK ? FormatterSimpleBraced.reindentBlockComment(
                            commentToken.text, baseIndent + indentUnit
                        ) : commentToken.text;
                        wrapped.append('\n').append(baseIndent).append(indentUnit).append(text);
                    } // for commentToken
                    wrapped.append(
                        '\n'
                    ).append(
                        baseIndent
                    ).append(
                        indentUnit
                    ).append(
                        clauseRenders.get(c)
                    );
                } // for c
                rendered = wrapped.toString();
            }

            final int lastCloseParenIdx = clauseParenSpans.get( clauseParenSpans.size() - 1 )[1];
            spans.add( new int[] { anchorCloseParenIdx + 1, lastCloseParenIdx + 1 } );
            renders.add(rendered);
            i = lastCloseParenIdx;
        } // for i

        if( spans.isEmpty() ) return MiscRuleCore.joinVerbatim(tokens);

        final StringBuilder out    = new StringBuilder();
              int           cursor = 0;
        for( int s = 0; s < spans.size(); ++s ) {
            final int[] span = spans.get(s);
            appendRange( out, tokens, cursor, span[0] );
            out.append( renders.get(s) );
            cursor = span[1];
        }
        appendRange( out, tokens, cursor, tokens.size() );

        return out.toString();
    }

    /**
     * STYLE_CPP26.md §4: `contract_assert(cond)` gets the same expression-operator spacing as a
     * `pre`/`post` clause's argument (single space around every binary operator, e.g.
     * `x>=0` &rarr; `x >= 0`), reusing {@link #spaceExpressionTokens}. Detection is purely
     * positional/textual: an identifier `contract_assert` immediately followed by `(`.
     */
    public String enforceContractAssertSpacing(final List<Token> tokens)
    {
        final List<int[]>  spans   = new ArrayList<>();
        final List<String> renders = new ArrayList<>();

        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( t.type != TokenType.IDENTIFIER || !"contract_assert".equals(t.text) ) continue;
            final int openParenIdx = nextSignificantIndex(tokens, i);
            if( openParenIdx < 0 || !isPunct( tokens.get(openParenIdx), "(" ) ) continue;
            final int closeParenIdx = matchParenForward(tokens, openParenIdx);
            if(closeParenIdx < 0) continue;
            if( hasCommentBetween(
                tokens, openParenIdx, closeParenIdx
            ) || MiscRuleCore.anyFrozen(
                tokens, openParenIdx, closeParenIdx
            ) ) continue;
            if(closeParenIdx == openParenIdx + 1) continue;
            final String inner = spaceExpressionTokens(tokens, openParenIdx + 1, closeParenIdx - 1);
            spans.add( new int[] { openParenIdx + 1, closeParenIdx } );
            renders.add(inner);
        } // for

        if( spans.isEmpty() ) return MiscRuleCore.joinVerbatim(tokens);

        final StringBuilder out    = new StringBuilder();
              int           cursor = 0;
        for( int s = 0; s < spans.size(); ++s ) {
            final int[] span = spans.get(s);
            appendRange( out, tokens, cursor, span[0] );
            out.append( renders.get(s) );
            cursor = span[1];
        }
        appendRange( out, tokens, cursor, tokens.size() );

        return out.toString();
    }

    /**
     * `[[ ... ]]` C++ attributes (STYLE_CPP20.md §4.4) and, mirroring the same rule, C++26 §5's
     * `[: ... :]` splice brackets: tight/loose padding of the bracket's own interior depends on
     * whether the content is "simple" (a bare name/value -- stays tight, e.g. `[[nodiscard]]`,
     * `[:refl:]`) or contains a nested call or bracket (goes loose, e.g. `[[ assume(a >= 0) ]]`,
     * `[: computeRefl(x) :]`), reusing {@link ComplexityPaddingEvaluator#isLoose} unmodified --
     * it already only looks for nested PUNCT `(`/`[` tokens among the interior's significant
     * tokens, and a call's own `(` (or a nested nested-bracket access) is exactly that, with no
     * OP-vs-PUNCT distinction needed since `[[`/`]]`/`[:`/`:]` themselves are never *inside* the
     * content being examined, only the two ends of it.
     *
     * <p>Prior to this method, empirical testing found `[[ ]]` attributes were left completely
     * verbatim/unformatted by this formatter -- STYLE_CPP26.md §5's claim that splice brackets
     * "mirror the existing JAR-verified `[[ assume(a >= 0) ]]` case" was aspirational, not actual
     * prior behavior; this method is what makes that claim true for both bracket kinds at once,
     * per explicit user decision when the discrepancy was found.
     *
     * <p>Only the immediate boundary gap (right after the open bracket, right before the close
     * bracket) is rewritten to exactly one space (loose) or zero (tight); everything strictly
     * between the first and last significant interior tokens is copied verbatim, so nested
     * spacing (e.g. inside the call's own parens) is left to whatever other rule already governs
     * it. A pair spanning multiple physical lines, containing a comment, or touching a frozen
     * token is skipped entirely and left untouched, same posture as this file's other rewrites.
     */
    public String enforceAttributeAndSpliceBracketPadding(final List<Token> tokens)
    {
        final int            n     = tokens.size();
        final List<int[]>    pairs = new ArrayList<>();
        final Deque<Integer> stack = new ArrayDeque<>();
        for(int i = 0; i < n; ++i) {
            final Token t = tokens.get(i);
            if( isOp(t, "[[") || isOp(t, "[:") ) {
                stack.push(i);
            }
            else if( isOp(t, "]]") || isOp(t, ":]") ) {
                if( !stack.isEmpty() ) {
                    final int     openIdx  = stack.pop();
                    final String  openText = tokens.get(openIdx).text;
                    final boolean matches  = ( "[[".equals(
                        openText
                    ) && "]]".equals(
                        t.text
                    ) ) || ( "[:".equals(
                        openText
                    ) && ":]".equals(
                        t.text
                    ) );
                    if(matches) {
                        pairs.add( new int[] { openIdx, i } );
                    }
                } // if
            }
        } // for
        if( pairs.isEmpty() ) return MiscRuleCore.joinVerbatim(tokens);

        final List<int[]>  spans   = new ArrayList<>();
        final List<String> renders = new ArrayList<>();
        for( final int[] pair : pairs ) {
            final int openIdx  = pair[0];
            final int closeIdx = pair[1];
            if(closeIdx <= openIdx + 1) continue;
            if( hasCommentBetween(
                tokens, openIdx, closeIdx
            ) || MiscRuleCore.anyFrozen(
                tokens, openIdx, closeIdx + 1
            ) || hasNewlineBetween(
                tokens, openIdx, closeIdx
            ) ) continue;
            final List<Token> content = new ArrayList<>();
            for(int i = openIdx + 1; i < closeIdx; ++i) {
                if( !isGapToken( tokens.get(i) ) ) content.add( tokens.get(i) );
            }
            if( content.isEmpty() ) continue;
            final boolean loose      = ATTRIBUTE_COMPLEXITY_EVALUATOR.isLoose(content);
            final String  desiredPad = loose ? " " : "";

            final int           firstSigIdx = nextSignificantIndex(tokens, openIdx);
            final int           lastSigIdx  = prevSignificantIndex(tokens, closeIdx);
            final StringBuilder render      = new StringBuilder();
            render.append( tokens.get(openIdx).text );
            render.append(desiredPad);
            appendRange(render, tokens, firstSigIdx, lastSigIdx + 1);
            render.append(desiredPad);
            render.append( tokens.get(closeIdx).text );
            spans.add( new int[] { openIdx, closeIdx + 1 } );
            renders.add( render.toString() );
        } // for pair

        if( spans.isEmpty() ) return MiscRuleCore.joinVerbatim(tokens);

        final StringBuilder out    = new StringBuilder();
              int           cursor = 0;
        for( int s = 0; s < spans.size(); ++s ) {
            final int[] span = spans.get(s);
            appendRange( out, tokens, cursor, span[0] );
            out.append( renders.get(s) );
            cursor = span[1];
        }
        appendRange( out, tokens, cursor, tokens.size() );

        return out.toString();
    }

    /**
     * STYLE_CPP26.md §5: `^^` binds tight to its operand, no space, same as C++'s existing unary
     * `*`/`&` prefix-operator spacing (STYLE_C_CPP.md). Detection is purely positional: an OP
     * token `^^` immediately followed (modulo gap tokens) by any significant token collapses the
     * gap between them to zero width, provided the gap has no comment/newline and neither side is
     * frozen -- same guard posture as every other rewrite in this file. `^^` has no meaningful
     * "before" side to also tighten (it is always the reflection operator's own leading token, not
     * something that follows an operand), so only the trailing gap is touched.
     */
    public String enforceReflectionOperatorSpacing(final List<Token> tokens)
    {
        final int          n             = tokens.size();
        final Set<Integer> reflectionOps = new HashSet<>();
        for(int i = 0; i < n; ++i) {
            if( isOp( tokens.get(i), "^^" ) ) reflectionOps.add(i);
        }
        if( reflectionOps.isEmpty() ) return MiscRuleCore.joinVerbatim(tokens);

        final StringBuilder out                 = new StringBuilder();
        final List<Token>   gap                 = new ArrayList<>();
              Token         lastSignificant     = null;
              boolean       lastWasReflectionOp = false;
              int           i                   = 0;
        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean gapHasBlocker = hasCommentOrNewline(
                gap
            ) || t.frozen || (lastSignificant != null && lastSignificant.frozen);

            if(lastWasReflectionOp && !gapHasBlocker) {
                gap.clear();
            }
            else {
                for(final Token g : gap) out.append(g.text);
                gap.clear();
            }

            out.append(t.text);
            lastSignificant     = t;
            lastWasReflectionOp = reflectionOps.contains(i);
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    /**
     * Every {@code COMMENT_LINE}/{@code COMMENT_BLOCK} token in {@code (fromExclusive,
     *  toExclusive)}, in source order -- used by {@link #enforceContractClausePlacement} to pull
     *  comments sitting between a signature/clause and the next clause out of the span it's about
     *  to overwrite, so they can be re-inserted rather than silently dropped
     */
    private List<Token> collectComments(
        final List<Token> tokens,
        final int         fromExclusive,
        final int         toExclusive
    )
    {
        final List<Token> comments = new ArrayList<>();
        for(int i = fromExclusive + 1; i < toExclusive; ++i) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) comments.add(
                t
            );
        }

        return comments;
    }

    private boolean hasNewlineBetween(
        final List<Token> tokens,
        final int         fromExclusive,
        final int         toExclusive
    )
    {
        for(int i = fromExclusive + 1; i < toExclusive; ++i) {
            if( tokens.get(i).type == TokenType.NEWLINE ) return true;
        }

        return false;
    }

    /**
     * Re-spaces a contract clause's parenthesized content as a plain expression -- single space
     *  around every significant token pair except tight-binding to `(`/`)`/`,` -- per
     *  {@link #enforceContractClausePlacement}'s doc comment. For {@code isPost}, a top-level `:`
     *  (paren-depth 0 within the range) splits the result-binding identifier from the expression,
     *  rendered tight-before/space-after (`r: expr`); absent a top-level `:`, falls back to
     *  spacing the whole range as one expression.
     */
    private String renderContractExpression(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toInclusive,
        final boolean     isPost
    )
    {
        int colonIdx = -1;
        if(isPost) {
            int depth = 0;
            for(int i = fromInclusive; i <= toInclusive; ++i) {
                final Token t = tokens.get(i);
                if( isPunct(t, "(") || isPunct(t, "[") ) {
                    ++depth;
                }
                else if( isPunct(t, ")") || isPunct(t, "]") ) {
                    --depth;
                }
                else if( depth == 0 && isOp(t, ":") ) {
                    colonIdx = i;
                    break;
                }
            } // for
        } // if
        if(colonIdx >= 0) {
            final String name = spaceExpressionTokens(tokens, fromInclusive, colonIdx - 1);
            final String expr = spaceExpressionTokens(tokens, colonIdx + 1, toInclusive);
            return name + ": " + expr;
        }

        return spaceExpressionTokens(tokens, fromInclusive, toInclusive);
    }

    /**
     * Single space between every pair of significant tokens in {@code [fromInclusive,
     *  toInclusive]}, except no space after `(` and no space before `)`/`,`/`;` -- a minimal
     *  expression pretty-printer for {@link #renderContractExpression}
     */
    private String spaceExpressionTokens(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toInclusive
    )
    {
        final List<Token> sig = new ArrayList<>();
        for(int i = fromInclusive; i <= toInclusive; ++i) {
            if( !isGapToken( tokens.get(i) ) ) sig.add( tokens.get(i) );
        }
        final StringBuilder sb = new StringBuilder();
        for( int j = 0; j < sig.size(); ++j ) {
            final Token cur = sig.get(j);
            if(j > 0) {
                final Token   prev    = sig.get(j - 1);
                final boolean noSpace = isPunct(
                    prev, "("
                ) || isPunct(
                    cur, ")"
                ) || isPunct(
                    cur, ","
                ) || isPunct(
                    cur, ";"
                );
                if(!noSpace) sb.append(' ');
            } // if
            sb.append(cur.text);
        } // for

        return sb.toString();
    }

    /**
     * The first `{`/`;` reached scanning forward from {@code requiresIdx}, or -1 if neither is
     *  found -- the trailing clause's own end (exclusive), per
     *  {@link #enforceRequiresClausePlacement}'s doc comment
     */
    private int findRequiresClauseEnd(final List<Token> tokens, final int requiresIdx)
    {
        for( int i = requiresIdx + 1; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( isPunct(t, "{") || isPunct(t, ";") ) return i;
        }

        return -1;
    }

    /**
     * The index of the first significant token on the physical line containing {@code idx} --
     *  the line-collapsing analog of {@link #lineIndent}
     */
    private int lineStartIndex(final List<Token> tokens, final int idx)
    {
        int newlineIdx = -1;
        for(int i = idx; i >= 0; --i) {
            if( tokens.get(i).type == TokenType.NEWLINE ) {
                newlineIdx = i;
                break;
            }
        }
        final int firstSig = nextSignificantIndex(tokens, newlineIdx);

        return firstSig < 0 ? idx : firstSig;
    }

    /**
     * Renders {@code tokens[fromInclusive, toInclusive]} verbatim except every whitespace/newline
     *  run collapses to exactly one space -- used to measure a would-be single-line rendering
     *  against {@link #lineLengthLimit} without actually committing to it
     */
    private String collapseToOneLine(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toInclusive
    )
    {
        final StringBuilder sb              = new StringBuilder();
              Token         prevSignificant = null;
        for(int i = fromInclusive; i <= toInclusive; ++i) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                // A wrapped member-access `.`/`->` is tight against both sides -- when the
                // original line happened to break right at that token, unconditionally inserting
                // a space here corrupted the expression (e.g. `_Other.\n_Owns` -> `_Other. _Owns`,
                // found via microsoft/STL real-code testing). Same fix as
                // MiscRuleCurly.collapseToOneLine's own identical gap.
                final Token   next      = i + 1 <= toInclusive ? nextSignificantForCollapse(
                    tokens, i, toInclusive
                ) : null;
                final boolean tightJoin = ( prevSignificant != null && ( isOp(
                    prevSignificant, "."
                ) || isOp(
                    prevSignificant, "->"
                ) ) ) || ( next != null && ( isOp(
                    next, "."
                ) || isOp(
                    next, "->"
                ) ) );
                if( !tightJoin && sb.length() > 0 && sb.charAt(
                    sb.length() - 1
                ) != ' ' ) sb.append(
                    ' '
                );
                continue;
            } // if
            sb.append(t.text);
            prevSignificant = t;
        } // for

        return sb.toString().trim();
    }
    /**
     * Helper for {@link #collapseToOneLine}: finds the next non-WHITESPACE/NEWLINE token at or
     *  after {@code fromExclusive + 1}, up to {@code toInclusive}, or {@code null} if none
     */
    private Token nextSignificantForCollapse(
        final List<Token> tokens,
        final int         fromExclusive,
        final int         toInclusive
    )
    {
        for(int j = fromExclusive + 1; j <= toInclusive; ++j) {
            final Token t = tokens.get(j);
            if(t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) return t;
        }

        return null;
    }

    /**
     * STYLE_C_CPP.md §10: a header file has a fixed 4-zone layout -- copyright block, header
     * guard (`#ifndef`/`#define` pair, or `#pragma once`), body, closing `#endif` (absent for the
     * `#pragma once` form) -- separated by exactly {@link #HEADER_ZONE_BLANK_LINES} blank lines.
     * Resolved via two {@code AskUserQuestion}s before writing this method (see STATE.md "§10
     * header zone spacing" and "§10 #endif trailing comment"): zone spacing is enforced strictly
     * in both directions (collapses excess blank lines, not just a floor, unlike §7's
     * "exactly one" precedent), and the closing `#endif`'s trailing `// GUARD_NAME` comment is
     * always normalized to the current guard name regardless of whether a rename actually fired,
     * inserting one even if the file's `#endif` was previously bare.
     *
     * <p>{@code detectHeaderZones} recognizes the shape conservatively: the first significant
     * token must be a {@code COMMENT_BLOCK}; the next, separated only by whitespace/newlines (a
     * comment in that gap aborts detection), must be a `#pragma once` {@code PREPROCESSOR} token
     * or an `#ifndef NAME` one immediately (whitespace/newlines only, no comment) followed by a
     * matching `#define NAME`; for the `#ifndef` form, the matching closing `#endif` is located by
     * depth-counting every `#if`/`#ifdef`/`#ifndef` (+1) and `#endif` (-1) {@code PREPROCESSOR}
     * token after the guard (mirroring {@code TokenizerCurly}'s own `preprocessorDepth` tracking,
     * applied after tokenization since that depth isn't stored per-token), and nothing but
     * trailing whitespace/newline may follow it. Any deviation from this shape -- a missing
     * copyright block, a comment between zones, a mismatched `#ifndef`/`#define` name pair, an
     * unmatched `#endif`, trailing content after the closing `#endif`, or an empty body -- aborts
     * detection entirely and the file is returned byte-for-byte unchanged, the same conservative
     * "don't guess past an unrecognized shape" posture used throughout this codebase, since
     * STYLE_C_CPP.md gives no worked "before" example for any malformed header. The body's own
     * content (verbatim between the guard and the closing `#endif`/end-of-file) is untouched --
     * §10 only fixes inter-zone spacing and the guard, not body formatting.
     *
     * <p>Guard-name derivation (`deriveGuardName`) is a pure, mechanical transform of whatever
     * {@code filePath} string the caller supplies (uppercase, `.`/`/`/`\` &rarr; `_`) -- e.g.
     * `audio/Codec.h` &rarr; `AUDIO_CODEC_H`, matching STYLE_C_CPP.md §10's own worked example
     * once the caller has already stripped any project-root prefix (`src/`) the example's path
     * omits; this method has no project-layout knowledge to do that stripping itself, so it is
     * the caller's responsibility, deferred like other not-yet-wired `Main.java`/`Config.java`
     * concerns elsewhere in this file. `renameGuard` stands in for the not-yet-existent
     * `header-guard-rename` config key (default off): when false, or when the existing guard
     * already matches, the existing name is kept and only spacing/the `#endif` comment are
     * normalized. This method already only ever normalizes within whichever of the two guard
     * forms (`#ifndef`/`#pragma once`) is already present and never converts between them --
     * there used to be a `header-guard-style` config key for this, removed since nothing ever
     * implemented the conversion; re-add it if that ever happens.
     */
    public String enforceHeaderFileStructure(
        final List<Token> tokens,
        final String      filePath,
        final boolean     renameGuard
    )
    {
        final HeaderZones z = detectHeaderZones(tokens);
        if( z == null || MiscRuleCore.anyFrozen(
            tokens, 0, z.isPragmaOnce ? z.guardOpenIdx + 1 : z.endifIdx + 1
        ) ) return MiscRuleCore.joinVerbatim(
            tokens
        );

        final StringBuilder out = new StringBuilder();
        appendRange(out, tokens, 0, z.copyrightEnd + 1);
        appendBlankLineGap(out);

        if(z.isPragmaOnce) {
            appendRange(out, tokens, z.guardOpenIdx, z.guardOpenIdx + 1);
            appendBlankLineGap(out);
            appendRange(out, tokens, z.bodyStart, z.bodyEnd);
            return out.toString();
        }

        final String expectedGuard  = deriveGuardName(filePath);
        final String effectiveGuard = renameGuard && !expectedGuard.equals(
            z.actualGuardName
        ) ? expectedGuard : z.actualGuardName;

        out.append("#ifndef ").append(effectiveGuard);
        appendRange(out, tokens, z.guardOpenIdx + 1, z.guardDefineIdx);
        out.append("#define ").append(effectiveGuard);
        appendBlankLineGap(out);
        appendRange(out, tokens, z.bodyStart, z.bodyEnd);
        appendBlankLineGap(out);
        out.append("#endif // ").append(effectiveGuard);
        appendRange( out, tokens, z.endifIdx + 1, tokens.size() );

        return out.toString();
    }

    /**
     * Detected zone boundaries for {@link #enforceHeaderFileStructure}, or {@code null} from the
     *  detector if the token list doesn't match the expected shape
     */
    private static final class HeaderZones {

        int     copyrightEnd;
        boolean isPragmaOnce;
        int     guardOpenIdx;
        int     guardDefineIdx;  // == guardOpenIdx for the pragma-once form
        String  actualGuardName; // Null for the pragma-once form
        int     bodyStart;
        int     bodyEnd;         // Exclusive
        int     endifIdx;        // -1 for the pragma-once form

    } // class HeaderZones

    private HeaderZones detectHeaderZones(final List<Token> tokens)
    {
        final int n            = tokens.size();
        final int copyrightIdx = nextNonBlankIndex(tokens, 0);
        if( copyrightIdx < 0 || tokens.get(
            copyrightIdx
        ).type != TokenType.COMMENT_BLOCK ) return null;
        final HeaderZones z = new HeaderZones();
        z.copyrightEnd = copyrightIdx;

        final int guardIdx = nextNonBlankIndex(tokens, copyrightIdx + 1);
        if( guardIdx < 0 || tokens.get(guardIdx).type != TokenType.PREPROCESSOR ) return null;
        final String guardText = tokens.get(guardIdx).text;

        if( isPragmaOnceDirective(guardText) ) {
            z.isPragmaOnce   = true;
            z.guardOpenIdx   = guardIdx;
            z.guardDefineIdx = guardIdx;
            z.endifIdx       = -1;
        }
        else if( "ifndef".equals( directiveWord(guardText) ) ) {
            z.isPragmaOnce    = false;
            z.guardOpenIdx    = guardIdx;
            z.actualGuardName = extractDirectiveName(guardText, "ifndef");
            if(z.actualGuardName == null) return null;
            final int defineIdx = nextNonBlankIndex(tokens, guardIdx + 1);
            if( defineIdx < 0 || tokens.get(defineIdx).type != TokenType.PREPROCESSOR ) return null;
            final String defineName = extractDirectiveName( tokens.get(defineIdx).text, "define" );
            if( defineName == null || !defineName.equals(z.actualGuardName) ) return null;
            z.guardDefineIdx = defineIdx;
        }
        else {
            return null;
        }

        final int afterGuard = (z.isPragmaOnce ? z.guardOpenIdx : z.guardDefineIdx) + 1;
        final int bodyStart  = nextNonBlankIndex(tokens, afterGuard);
        if(bodyStart < 0) return null;
        z.bodyStart = bodyStart;

        if(z.isPragmaOnce) {
            z.bodyEnd = n;
            return z;
        }

        int depth    = 1;
        int endifIdx = -1;
        for(int p = z.guardDefineIdx + 1; p < n; ++p) {
            final Token t = tokens.get(p);
            if(t.type != TokenType.PREPROCESSOR) continue;
            final String word = directiveWord(t.text);
            if( "if".equals(word) || "ifdef".equals(word) || "ifndef".equals(word) ) {
                ++depth;
            }
            else if( "endif".equals(word) ) {
                --depth;
                if(depth == 0) {
                    endifIdx = p;
                    break;
                }
            }
        } // for
        if(endifIdx < 0) return null;
        for(int p = endifIdx + 1; p < n; ++p) {
            final TokenType ty = tokens.get(p).type;
            if(ty != TokenType.WHITESPACE && ty != TokenType.NEWLINE) return null;
        }

        final int lastBodySig = prevNonBlankIndex(tokens, endifIdx - 1);
        if(lastBodySig < z.bodyStart) return null;
        z.bodyEnd  = lastBodySig + 1;
        z.endifIdx = endifIdx;

        return z;
    }

    /** Uppercase, with `.`/`/`/`\` replaced by `_` -- e.g. `audio/Codec.h` → `AUDIO_CODEC_H`. */
    private String deriveGuardName(final String filePath)
    {
        final StringBuilder sb = new StringBuilder( filePath.length() );
        for( int i = 0; i < filePath.length(); ++i ) {
            final char c = filePath.charAt(i);
            if(c == '.' || c == '/' || c == '\\') sb.append('_');
            else                                  sb.append( Character.toUpperCase(c) );
        }

        return sb.toString();
    }

    private boolean isPragmaOnceDirective(final String text)
    {
        if( !"pragma".equals( directiveWord(text) ) ) return false;
        final String afterHash   = text.trim().substring(1).trim();
        final String afterPragma = afterHash.substring( "pragma".length() ).trim();

        return "once".equals(afterPragma);
    }

    /**
     * The directive keyword of a {@code PREPROCESSOR} token's text (e.g. `"ifndef"`, `"endif"`,
     *  `"pragma"`), or `""` if the text doesn't start with `#`.
     */
    private String directiveWord(final String text)
    {
        final String t = text.trim();
        if( !t.startsWith("#") ) return "";
        final String rest = t.substring(1);
              int    p    = 0;
        while( p < rest.length() && Character.isWhitespace( rest.charAt(p) ) ) p++;
        final int start = p;
        while( p < rest.length() && ( Character.isLetterOrDigit(
            rest.charAt(p)
        ) || rest.charAt(
            p
        ) == '_' ) ) p++;

        return rest.substring(start, p);
    }

    /**
     * The macro name of an `#ifndef NAME` / `#define NAME` directive's text, requiring nothing
     *  else (e.g. a trailing comment) follow it on the line -- {@code null} if the shape doesn't
     *  match exactly.
     */
    private String extractDirectiveName(final String text, final String expectedDirective)
    {
        final String t = text.trim();
        if( !t.startsWith("#") ) return null;
        String rest = t.substring(1).trim();
        if( !rest.startsWith(expectedDirective) ) return null;
        rest = rest.substring( expectedDirective.length() ).trim();

        return isValidIdentifierName(rest) ? rest : null;
    }

    private boolean isValidIdentifierName(final String s)
    {
        if( s.isEmpty() || !( Character.isLetter(
            s.charAt(0)
        ) || s.charAt(
            0
        ) == '_' ) ) return false;
        for( int i = 0; i < s.length(); ++i ) {
            final char c = s.charAt(i);
            if( !( Character.isLetterOrDigit(c) || c == '_' ) ) return false;
        }

        return true;
    }

    private void appendBlankLineGap(final StringBuilder out)
    {
        for(int i = 0; i < HEADER_ZONE_BLANK_LINES + 1; ++i) out.append('\n');
    }

    private void appendRange(
        final StringBuilder out,
        final List<Token>   tokens,
        final int           fromInclusive,
        final int           toExclusive
    )
    {
        for(int i = fromInclusive; i < toExclusive; ++i) out.append( tokens.get(i).text );
    }


    private int nextNonBlankIndex(final List<Token> tokens, final int from)
    {
        for( int i = from; i < tokens.size(); ++i ) {
            final TokenType ty = tokens.get(i).type;
            if(ty != TokenType.WHITESPACE && ty != TokenType.NEWLINE) return i;
        }

        return -1;
    }

    private int prevNonBlankIndex(final List<Token> tokens, final int from)
    {
        for(int i = from; i >= 0; --i) {
            final TokenType ty = tokens.get(i).type;
            if(ty != TokenType.WHITESPACE && ty != TokenType.NEWLINE) return i;
        }

        return -1;
    }

    private int matchParenForward(final List<Token> tokens, final int openIdx)
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

    private int matchParenBackward(final List<Token> tokens, final int closeIdx)
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

    private int prevSignificantIndex(final List<Token> tokens, final int from)
    {
        for(int i = from - 1; i >= 0; --i) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    private int nextSignificantIndex(final List<Token> tokens, final int from)
    {
        for( int i = from + 1; i < tokens.size(); ++i ) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    /**
     * True iff {@code t} is a C++ post-paren qualifier keyword that can appear between {@code )}
     *  and {@code {}} in a function definition (const, volatile, noexcept, override, final)
     */
    private boolean isDefinitionQualifier(final Token t)
    {
        if(t.type != TokenType.KEYWORD) return false;
        switch(t.text) {
            case "const"    : /* FALL-THROUGH */
            case "volatile" : /* FALL-THROUGH */
            case "noexcept" : /* FALL-THROUGH */
            case "override" : /* FALL-THROUGH */
            case "final"    : return true ;
            default         : return false;
        } // switch
    }

    /**
     * Matches a function-like `#define NAME(...)` -- never realigned, since the name/value
     *  relationship there isn't a simple name-column/value-column pair.
     */
    private static final Pattern FUNC_DEFINE = Pattern.compile("^#define\\s+[A-Za-z_]\\w*\\(");

    /**
     * Matches a scalar `#define NAME VALUE` (or `NAME VALUE // comment`) -- group 1 is the
     *  name, group 2 is the value onward (untouched by realignment, only the gap before it
     *  is rewritten). A `#define` with no value at all (e.g. a header guard) does not match
     *  and is left alone.
     */
    private static final Pattern VALUE_DEFINE = Pattern.compile(
        "^#define\\s+([A-Za-z_]\\w*)\\s+(\\S.*)$"
    );

    /**
     * `format-macros = on`: realigns the value column of consecutive scalar `#define` lines
     * (STYLE_C_CPP.md macro grouping) so every value in the run starts at the same column,
     * one space past the longest macro name in the run. A "run" is broken by a blank line, a
     * comment, any non-`#define` line, a function-like `#define NAME(...)`, or a valueless
     * `#define NAME` (header guards) -- each such line ends the current run without joining
     * it, and starts a fresh (possibly empty) one. Only the name-to-value gap is rewritten;
     * the value itself (including any trailing same-line comment) is passed through verbatim.
     */
    public String alignMacroDefinitions(final List<Token> tokens)
    {
        final List<List<Integer>> runs    = new ArrayList<>();
              List<Integer>       current = new ArrayList<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( t.type == TokenType.PREPROCESSOR && isAlignableDefine(t.text) ) {
                if( !current.isEmpty() && !isAdjacentDefineLine(
                    tokens, current.get( current.size() - 1 ), i
                ) ) {
                    runs.add(current);
                    current = new ArrayList<>();
                }
                current.add(i);
            } // if
            else if(t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) {
                if( !current.isEmpty() ) {
                    runs.add(current);
                    current = new ArrayList<>();
                }
            }
        } // for
        if( !current.isEmpty() ) runs.add(current);

        final Map<Integer, String> replacements = new HashMap<>();
        for(final List<Integer> run : runs) {
            if( run.size() < 2 ) continue;
            boolean runFrozen = false;
            for(final int idx : run) {
                if( tokens.get(idx).frozen ) {
                    runFrozen = true;
                    break;
                }
            }
            if(runFrozen) continue;
              int                  maxNameLen = 0;
        final Map<Integer, String> names      = new HashMap<>();
            for(final int idx : run) {
                final Matcher m = VALUE_DEFINE.matcher( tokens.get(idx).text );
                m.matches();
                names.put( idx, m.group(1) );
                maxNameLen = Math.max( maxNameLen, m.group(1).length() );
            }
            for(final int idx : run) {
                final Matcher m = VALUE_DEFINE.matcher( tokens.get(idx).text );
                m.matches();
                final String        name    = names.get(idx);
                final StringBuilder padding = new StringBuilder();
                for( int p = 0; p < maxNameLen - name.length() + 1; ++p ) padding.append(' ');
                replacements.put( idx, "#define " + name + padding + m.group(2) );
            } // for idx
        } // for run

        final StringBuilder out = new StringBuilder();
        for( int i = 0; i < tokens.size(); ++i ) {
            final String replacement = replacements.get(i);
            out.append( replacement != null ? replacement : tokens.get(i).text );
        }

        return out.toString();
    }

    private boolean isAlignableDefine(final String text)
    {
        return !FUNC_DEFINE.matcher(text).find() && VALUE_DEFINE.matcher(text).matches();
    }

    /**
     * True iff, scanning from just after {@code prevIdx} to just before {@code curIdx}, the
     *  only tokens present are whitespace and exactly one newline -- i.e. the two `#define`
     *  tokens sit on immediately consecutive lines with no blank line between them.
     */
    private boolean isAdjacentDefineLine(
        final List<Token> tokens,
        final int         prevIdx,
        final int         curIdx
    )
    {
        int newlineCount = 0;
        for(int j = prevIdx + 1; j < curIdx; ++j) {
            final TokenType ty = tokens.get(j).type;
                 if(ty == TokenType.NEWLINE)    newlineCount++;
            else if(ty != TokenType.WHITESPACE) return false;
        }

        return newlineCount == 1;
    }

} // class CppSpecificRule
