/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isComment;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

public class BlockStructureRule {

    private static final Set<String> SINGLE_EXPR_KEYWORDS = setOf("if", "while", "for");

    // A nested compound construct is not a "single EXPRESSION" (STYLE.md §10's own title) --
    // collapsing e.g. `if(x) { if(y) foo(); }` to `if(x) if(y) foo();` would introduce a
    // dangling-construct ambiguity that the worked examples (return/continue/break) never
    // exercise, so such bodies are left braced rather than guessed at.
    private static final Set<String> COMPOUND_BODY_KEYWORDS = setOf(
        "if", "while", "for", "switch", "do", "try"
    );

    // STYLE.md §11 K&R list: keywords whose body brace is preceded by a `( ... )` condition.
    private static final Set<String> PAREN_KR_KEYWORDS = setOf(
        "if", "while", "for", "switch", "catch"
    );

    // STYLE.md §11 K&R list: keywords whose body brace follows the bare keyword, no condition.
    private static final Set<String> BARE_KR_KEYWORDS = setOf("else", "do", "try", "finally");

    // STYLE.md §7 default; overridable via `closing-comment-min-lines` once Config.java exists.
    private static final int DEFAULT_CLOSING_COMMENT_MIN_LINES = 5;

    // Primitive/built-in type keywords that can lead a local variable declaration in C/C++/Java
    // (`int x = 1;`, `boolean ignored = ...;`) -- mirrors `DeclarationAlignmentRuleCurly`'s own
    // `TYPE_KEYWORDS_C`/`TYPE_KEYWORDS_JAVA` sets (kept as a separate narrow copy here rather than
    // a shared import, same precedent as this file's own `TIGHT_PAREN_KEYWORDS` copy of
    // `MiscRuleCore.TIGHT_PAREN_KEYWORDS`). Used by `isSingleStatementBody`'s declaration guard
    // below, alongside the existing `final`/`const` leading-token check, which only caught a
    // declaration when explicitly qualified -- a plain `int saveCursor = cursor;` slipped through
    // (found via openrewrite/rewrite real-code testing,
    // `ReloadableJava25ParserVisitor.java`'s `parsePackage`: `if (...) { int saveCursor = cursor;
    // }` collapsed to the illegal braceless `if (...) int saveCursor = cursor;` -- javac rejects it
    // with "variable declaration not allowed here").
    private static final Set<String> PRIMITIVE_TYPE_KEYWORDS = setOf(
        "void",
        "char",
        "short",
        "int",
        "long",
        "float",
        "double",
        "boolean",
        "byte",
        "signed",
        "unsigned",
        "struct",
        "enum",
        "union",
        "bool",
        "_Bool",
        "wchar_t",
        "char16_t",
        "char32_t",
        "auto",
        "class",
        "var"
    );

    private final Lang lang;
    private final int  closingCommentMinLines;
    /**
     * One indentation level, used by {@link #insertNamedConstructBlankLines} to synthesize a
     *  properly indented line when splitting a same-line nested body (e.g. `struct Foo { enum
     *  Bar {`) onto its own line -- built from the configured `indent-size` (see the
     *  constructor), not a hardcoded literal, same bug class as `SwitchRule.deriveUnit`'s own
     *  former fallback.
     */
    private final String indentUnit;
    /**
     * STYLE.md's overall line-length ceiling, used only by {@link #alignBracelessElseIfChain} to
     *  refuse to column-pad a chain branch past this width -- see that method's own javadoc for
     *  why (vuejs/core real-code testing: a padded-but-still-fits-check-stale consequent call
     *  could otherwise silently render over the limit with no downstream pass left to re-wrap
     *  it, since this pass runs last).
     */
    private final int lineLengthLimit;
    /**
     * Configured `indent-size`, kept alongside the pre-built {@link #indentUnit} string so
     *  {@link #expandedIndentWidth} can tab-expand a raw (not-yet-converted) leading-whitespace
     *  run the same way {@code MiscRuleCore.expandedIndentWidth} does -- see that method's own
     *  javadoc for why a plain {@code String.length()} undercounts a tab.
     */
    private final int indentWidth;

    public BlockStructureRule(final Lang lang)
    {
        this(lang, DEFAULT_CLOSING_COMMENT_MIN_LINES);
    }

    public BlockStructureRule(final Lang lang, final int closingCommentMinLines)
    {
        this(lang, closingCommentMinLines, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public BlockStructureRule(
        final Lang lang,
        final int  closingCommentMinLines,
        final int  indentWidth
    )
    {
        this(lang, closingCommentMinLines, indentWidth, MiscRuleCore.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public BlockStructureRule(
        final Lang lang,
        final int  closingCommentMinLines,
        final int  indentWidth,
        final int  lineLengthLimit
    )
    {
        this.lang                   = lang;
        this.closingCommentMinLines = closingCommentMinLines;
        this.lineLengthLimit        = lineLengthLimit;
        this.indentWidth            = indentWidth;
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < indentWidth; ++i) sb.append(' ');
        this.indentUnit = sb.toString();
    }

    private static Set<String> setOf(final String... words)
    {
        return new HashSet<>( Arrays.asList(words) );
    }

    // ── Single-expression blocks (STYLE.md §10) ─────────────────────────────────
    /**
     * Scans a token slice and rewrites every `if`/`while`/`for` whose controlled body is a
     * single statement (`if(x) return y;`, `if(x) continue;`, `if(x) break;`) from braced,
     * possibly multi-line form into the brace-less single-line form. Bodies that already
     * omit braces, hold more than one statement, or are themselves a nested compound
     * construct are left untouched verbatim, byte-for-byte. Everything outside a qualifying
     * body -- including unrelated tokens, whitespace, and comments -- is passed through
     * unchanged.
     */
    public String collapseSingleExpressionBlocks(final List<Token> tokens)
    {
        final StringBuilder out = new StringBuilder();
        final int           n   = tokens.size();
              int           i   = 0;
        // Kotlin-only running depth of unmatched `(`/`[` seen so far (never touched for
        // C/C++/Java, whose `if`/`else` are only ever statements). See the `isKotlinExpressionIf`
        // comment below for why this matters; also used directly to gate the bare `else` branch,
        // which has no condition of its own to anchor a similar check against.
        int kotlinParenDepth = 0;
        // Kotlin-only: set true the moment the main loop passes an `if` keyword exempted via
        // the new depth-0 function/property-expression-body case (`isKotlinExprBodyIf` below,
        // RDD_KEY_177) and cleared the moment it's consumed by that same if's paired bare
        // `else` (or overwritten/cleared by the next `if` reached). The bare-`else` branch
        // further below has no condition of its own to run the same "preceded by a fun-tail
        // `=`" check against, so it needs this flag carried forward from its own `if` to know
        // it must be exempted from collapse too -- else-branch handling is a structurally
        // separate case of this same loop's dispatch, so without this flag it would still treat
        // the exempted if-expression's own trailing `else` arm as an ordinary statement-`else`
        // and collapse its (possibly multi-line, `enforceCallLineBreaking`-wrapped) body back
        // onto one line, reproducing the exact round1/round2 flap this fix targets one token
        // later (found via arrow-kt/arrow's Comparison.kt `sort2` while verifying this fix).
        boolean pendingKotlinExprBodyElse = false;

        while(i < n) {
            final Token t = tokens.get(i);
            if(lang.isKotlin) {
                     if( isPunct(t, "(") || isPunct(t, "[") ) kotlinParenDepth++;
                else if( isPunct(t, ")") || isPunct(t, "]") ) kotlinParenDepth--;
            }
            // Kotlin-only: unlike C/C++/Java, `if` doubles as a value expression (`val x = if
            // (c) a else b`), and such an expression-position `if` can itself be wrapped in
            // parens as part of some larger expression (`(if (c) a else b) + rest`). A
            // statement-position `if`/`else` never sits inside an unmatched `(`/`[` that this
            // same pass didn't itself open -- matchControlBlock always fully consumes its own
            // condition's parens before returning, so by the time the main loop reaches the
            // `if` keyword itself, kotlinParenDepth is always back to whatever it was before
            // that `if`'s own condition -- but a *wrapping* paren around the whole if-expression
            // (opened before the `if` keyword and not yet closed) leaves depth > 0 here, which
            // is exactly the signal that distinguishes this shape. Left un-collapsed, the
            // braceless-collapse branch further below misreads the `else`-arm's trailing value
            // plus whatever follows the wrapping paren as if they were the tail of a braceless
            // statement body, eating the newline that separates this statement from the next
            // one entirely (found via dogfood-testing against RobotCoding
            // gui_frontend_android's ToolbarActions.kt / MainViewModel.kt: `val display = (if
            // (x != null) "..." else "") ...` got fused onto the same line as the following
            // `showMessage(...)` call with no separator -- invalid Kotlin). An *unparenthesized*
            // expression-position `if` (`val x = if (c) a else b`, depth 0 here) is deliberately
            // left alone -- STYLE_KOTLIN.md's own worked examples (and `real_code_regressions_18`)
            // rely on this same braceless-collapse path to wrap it, and it does not have this
            // bug: with no wrapping paren to hide behind, the whole if/else IS the statement's
            // entire RHS, so there is no following sibling content within the same statement for
            // it to over-consume.
            // Depth-0 sibling case (RDD_KEY_177): an if-expression used as an entire
            // expression-bodied function/property's whole body (`fun sort2(...) = if (leq(a, b))
            // Pair(a, b) else Pair(b, a)`) is likewise NOT a statement, even though it sits at
            // kotlinParenDepth == 0 (no wrapping paren) -- it's directly preceded by a bare `=`
            // at the same depth, i.e. the RHS of a `KotlinSignatureRule` function-tail or a `val`/
            // `var` initializer. Left uncollapsed, this pass and `enforceCallLineBreaking` fought
            // over two different stable states depending on pass ordering: round1 (whole
            // signature+body still one long physical line) let `enforceCallLineBreaking` wrap
            // everything consistently, but round2 (an earlier stage had already stickily wrapped
            // just the signature) let this pass re-collapse the body alone onto its own
            // now-short-looking line, which then measured as fitting in isolation and stayed
            // collapsed -- a genuine non-fixed-point flap (found via arrow-kt/arrow's
            // Comparison.kt `sort2`). Exempting this shape here leaves
            // `enforceCallLineBreaking` as the sole source of truth for it, same as the
            // depth-0 unparenthesized (no preceding `=`) case already left alone above.
            final boolean isKotlinExprBodyIf;
            if( lang.isKotlin && "if".equals(t.text) && kotlinParenDepth == 0 ) {
                final int prevIdx = prevSignificantIndex(tokens, i - 1);
                isKotlinExprBodyIf = prevIdx >= 0 && isOp( tokens.get(prevIdx), "=" )
                                   && isFunctionExprBodyEquals(tokens, prevIdx);
            }
            else {
                isKotlinExprBodyIf = false;
            }
            final boolean isKotlinExpressionIf = lang.isKotlin && "if".equals(
                t.text
            ) && (kotlinParenDepth > 0 || isKotlinExprBodyIf);
            if( lang.isKotlin && "if".equals(t.text) ) {
                // Track/refresh for the paired bare `else` further below -- see the flag's own
                // declaration comment above the loop
                pendingKotlinExprBodyElse = isKotlinExprBodyIf;
            }
            // A `while` at the tail of a `do { ... } while (cond)` looks identical, at this
            // scan's level, to a genuine loop-starting `while (cond)` -- both are a KEYWORD
            // "while" followed by a `(...)` condition with no `{` after it (a do-while's
            // trailing `while (cond)` has nothing after its `)` but a statement terminator,
            // same shape as Kotlin's already-braceless multi-line body). Found via real-code
            // compile-checking against `square/okio`'s `Buffer.kt`: `} while (!done && head !=
            // null)` followed on the next (blank-separated) line by an unrelated `size -=
            // seen.toLong()` statement got misread as this `while`'s own braceless body and
            // fused onto the same line with no separator -- a first-pass, not merely
            // idempotency, compile error (RDD_KEY_151). Guard: bail if this "while" is
            // immediately preceded (skipping non-significant tokens) by a `}` whose matching
            // `{` is itself immediately preceded by a `do` keyword.
            final boolean isDoWhileTail = "while".equals(t.text) && isDoWhileTailKeyword(tokens, i);
            if( !isKotlinExpressionIf && !isDoWhileTail && t.type == TokenType.KEYWORD
                    && SINGLE_EXPR_KEYWORDS.contains(t.text) ) {
                final ControlBlock block = matchControlBlock(tokens, i);
                if(block != null && block.openBraceIndex >= 0) {
                    if( !isPartOfElseChain(
                        tokens, i, block, n
                    ) && !anyFrozen(
                        tokens, i, block.closeBraceIndex + 1
                    ) ) {
                        final String collapsed = tryCollapse(tokens, i, block);
                        if(collapsed != null) {
                            out.append(collapsed);
                            final int afterBrace = block.closeBraceIndex + 1;
                            i = appendChainNewlineBeforeElse(
                                tokens, afterBrace, n, out,
                                mostRecentLineIndent(tokens, i)
                            );
                            continue;
                        } // if
                    } // if
                } // if
                else if(block != null && block.openBraceIndex < 0 && lang.isKotlin) {
                    // Kotlin's braceless `if`/`while`/`for` body can start life already
                    // brace-free but spread across two physical lines (`if(x)\n    stmt` --
                    // e.g. `test/kt_combined_inp.kt`'s `if(i <= 5)\n    return@forEach`), unlike
                    // C/C++/Java where a braceless body is only ever produced BY this same pass
                    // collapsing an originally-braced one -- there is no pre-existing
                    // "already-braceless-but-multi-line" input shape for those languages to
                    // exercise this branch against. STYLE.md §10's own worked examples
                    // (`if(x) return y;`) show the collapsed form as the target either way, so a
                    // braceless multi-line body still needs joining onto one line here.
                    if( !isPartOfElseChainBraceless(tokens, i, block, n)
                            && !anyFrozen(tokens, i, block.closeParenIndex + 1) ) {
                        final int[]  bodyEnd   = new int[1];
                        final String collapsed = tryCollapseBraceless(tokens, i, block, bodyEnd);
                        if(collapsed != null) {
                            out.append(collapsed);
                            i = bodyEnd[0];
                            continue;
                        }
                    } // if
                }
                else if(block != null && block.openBraceIndex < 0 && !lang.isKotlin) {
                    // C/C++/Java sibling of the Kotlin branch just above, but for the opposite
                    // reason: unlike Kotlin, a C/C++/Java braceless body normally is only ever
                    // produced BY this same pass collapsing an originally-braced one (see that
                    // branch's own comment) -- except on a *second* format pass over this pass's
                    // own prior output, where the body arrives already braceless. The forced
                    // newline this pass inserts before a following `else` (via
                    // appendChainNewlineBeforeElse, on the braced-collapse path above) is what
                    // gives an `if`/`else if`/`else` chain its one-branch-per-line shape; nothing
                    // else in the pipeline re-inserts it. On a fresh (still-braced) round, that
                    // insertion happens naturally as a side effect of the collapse above. On a
                    // reformat-of-already-collapsed round, `ScopePipelineCurly`'s declaration/
                    // assignment-RHS pass (which has no multi-line-render path and always joins a
                    // declaration's whole initializer back onto one physical line) runs before
                    // this pass and erases those forced newlines again -- with no brace left for
                    // this pass to re-collapse and re-split, the whole chain then stays joined
                    // onto one line, a genuine non-fixed-point flap (found via dogfood-testing
                    // openrewrite/rewrite's MethodMatcherBenchmark.java: a `.map(name -> { ...
                    // if/else-if chain ... })` lambda body rendered with each branch on its own
                    // line on a fresh format, but fully joined onto one giant line on a reformat
                    // of that same output). Fixed the same way as the Kotlin branch above: leave
                    // the already-formatted body's own text untouched (verbatim, not re-rendered
                    // via `renderInline`, since it is already exactly one line with no interior
                    // newlines to flatten), but still force the newline before a following `else`.
                    if( !anyFrozen(tokens, i, block.closeParenIndex + 1) ) {
                        final int bodyEnd = findBracelessStatementEnd(
                            tokens, block.closeParenIndex + 1, n
                        );
                        if( bodyEnd >= 0 && !anyFrozen(
                            tokens, block.closeParenIndex + 1, bodyEnd
                        ) ) {
                            for( final Token bt : tokens.subList(i, bodyEnd) ) out.append(bt.text);
                            i = appendChainNewlineBeforeElse(
                                tokens, bodyEnd, n, out,
                                mostRecentLineIndent(tokens, i)
                            );
                            continue;
                        } // if
                    } // if
                }
            } // if
            else if( t.type == TokenType.KEYWORD && "else".equals(t.text) && lang.isKotlin
                    && kotlinParenDepth == 0 && pendingKotlinExprBodyElse ) {
                // Paired `else` of an `isKotlinExprBodyIf`-exempted if-expression (RDD_KEY_177) --
                // consume the flag and fall through to the default single-token append below,
                // leaving this `else` and its (possibly multi-line, `enforceCallLineBreaking`-
                // owned) body completely untouched, mirroring the `if` arm's own exemption
                pendingKotlinExprBodyElse = false;
                out.append(t.text);
                ++i;
                continue;
            }
            else if( t.type == TokenType.KEYWORD && "else".equals(t.text) && lang.isKotlin
                    && kotlinParenDepth == 0 ) {
                // Bare `else` (not `else if` -- that's still an `if`, handled by the branch
                // above once the main loop reaches it) with an already-braceless multi-line body
                // (`else\n    stmt`, RDD_KEY_124's sibling gap -- e.g. `test/kt_combined_inp.kt`'s
                // `if (it.isEmpty())\n    0\nelse\n    it.toInt()`) is the one shape RDD_KEY_124
                // deliberately left unhandled (that fix is keyed off `if`/`while`/`for`'s own `(
                // ...)` condition, which a bare `else` never has). Collapsing it to one line
                // (`else it.toInt()`) is the same STYLE.md §10 single-statement omission,
                // structurally -- but `kt_combined_out.kt` additionally column-pads `else`'s body
                // to align with the *preceding* `if` branch's own body (`if(...) 0` / `else
                // it.toInt()`, both starting at the same column), which has no STYLE_KOTLIN.md
                // worked example to justify as a general rule (one fixture occurrence isn't
                // enough evidence to derive a trigger condition from) -- so only the collapse
                // itself is done here, with a plain single space, same as every other collapse in
                // this method; the padding gap is left open (see STATE_KOTLIN.md Open Questions).
                final int     next     = skipNonSignificant(tokens, i + 1);
                final boolean isElseIf = next < n && tokens.get(
                    next
                ).type == TokenType.KEYWORD && "if".equals(
                    tokens.get(next).text
                );
                final boolean isBraced = next < n && isPunct( tokens.get(next), "{" );
                // A `when` expression's `else -> body` branch label is lexically identical to a
                // bare statement-`else` up to this point (a KEYWORD "else" not followed by "if"
                // or "{"), but its body is introduced by `->`, not implicitly braceless the way a
                // real `if`/`else` chain's single-statement body is -- collapseBracelessBody
                // would otherwise treat everything from `->` up to the arm's true end as one
                // undifferentiated braceless statement span and join multiple statements onto one
                // line with no `;` between them (found via dogfood-testing RobotCoding
                // gui_frontend_android's Optimizer.kt: an `else -> { var x = ...; for(...) {...};
                // x }` block-bodied arm got flattened to one line, a Kotlin parse error).
                final boolean isWhenArrow = next < n && isOp( tokens.get(next), "->" );
                if( !isElseIf && !isBraced && !isWhenArrow && !anyFrozen(tokens, i, i + 1) ) {
                    final int[]  bodyEnd   = new int[1];
                    final String collapsed = collapseBracelessBody(
                        tokens, i, i + 1, "else", bodyEnd
                    );
                    if(collapsed != null) {
                        out.append(collapsed);
                        i = bodyEnd[0];
                        continue;
                    }
                } // if
            }
            else if( t.type == TokenType.KEYWORD && "else".equals(t.text) && !lang.isKotlin ) {
                // C/C++/Java sibling of the Kotlin bare-`else` branch above: the braced-body path
                // (line 96-127) only ever collapses `if`/`else if` keywords, never a *bare*
                // terminal `else { ... }`, which has no condition of its own to anchor the main
                // loop's SINGLE_EXPR_KEYWORDS dispatch. Handled here instead, gated by the same
                // whole-chain opt-in safety check as every other branch in this feature
                // (chainAllBranchesCollapsible, invoked from this else's own chain-start `if`).
                final int     next     = skipWhitespaceOnly(tokens, i + 1);
                final boolean isElseIf = next < n && tokens.get(
                    next
                ).type == TokenType.KEYWORD && "if".equals(
                    tokens.get(next).text
                );
                if( !isElseIf && next < n && isPunct( tokens.get(next), "{" )
                        && !anyFrozen(tokens, i, next + 1) ) {
                    final int chainStart = findChainStart(tokens, i);
                    if( chainStart >= 0 && chainAllBranchesCollapsible(tokens, chainStart, n) ) {
                        int depth = 1;
                        int j     = next + 1;
                        while(j < n && depth > 0) {
                            final Token tk = tokens.get(j);
                                 if( isPunct(tk, "{") ) depth++;
                            else if( isPunct(tk, "}") ) depth--;
                            ++j;
                        }
                        if(depth == 0) {
                            final List<Token> contents = tokens.subList(next + 1, j - 1);
                            if( isSingleStatementBody(contents) && !anyFrozen(tokens, i, j) ) {
                                final String candidate = "else " + renderInline(contents);
                                // JS/TS root cause #3 (STATE_JS_TS.md, "2026-07-30 design/
                                // scoping pass") -- same gate as `tryCollapse`/
                                // `collapseBracelessBody`, see `refuseUnrescuableCollapse`'s
                                // javadoc. This bare-terminal-`else` chain-collapse path builds
                                // its own candidate inline rather than routing through either of
                                // those methods, so it needs its own call to the same gate.
                                if( !refuseUnrescuableCollapse(
                                    tokens, i, next + 1, j - 2, candidate
                                ) ) {
                                    out.append(candidate);
                                    i = j;
                                    continue;
                                } // if
                            } // if
                        } // if
                    } // if
                } // if
            }
            out.append(t.text);
            ++i;
        } // while

        return out.toString();
    }

    /** Token-index span of an `if`/`while`/`for` condition and, if present, its braced body */
    private static final class ControlBlock {

        final int closeParenIndex;
        final int openBraceIndex;  // -1 if the body already has no braces
        final int closeBraceIndex; // Meaningless when openBraceIndex == -1

        ControlBlock(final int closeParenIndex, final int openBraceIndex, final int closeBraceIndex)
        {
            this.closeParenIndex = closeParenIndex;
            this.openBraceIndex  = openBraceIndex;
            this.closeBraceIndex = closeBraceIndex;
        }

    } // class ControlBlock

    /**
     * Locates the `( ... )` condition following the keyword at {@code kwIndex}, and the
     * `{ ... }` body after it if one is present. Bracket matching is local depth counting
     * (mirrors `DeclarationAlignmentRuleCurly`'s `[`/`]` matching) rather than relying on the
     * tokenizer's running depth fields, since this method must work on any bounded slice.
     * Returns null on unbalanced brackets -- caller leaves the input untouched.
     */
    private ControlBlock matchControlBlock(final List<Token> tokens, final int kwIndex)
    {
        final int n = tokens.size();
              int i = skipNonSignificant(tokens, kwIndex + 1);
        if( i >= n || !isPunct( tokens.get(i), "(" ) ) return null;

        int depth = 1;
        ++i;
        while(i < n && depth > 0) {
            final Token tk = tokens.get(i);
                 if( isPunct(tk, "(") || isPunct(tk, "[") ) depth++;
            else if( isPunct(tk, ")") || isPunct(tk, "]") ) depth--;
            ++i;
        }
        if(depth != 0) return null;
        final int closeParen = i - 1;

        final int afterParen = skipNonSignificant(tokens, closeParen + 1);
        if( afterParen >= n || !isPunct(
            tokens.get(afterParen), "{"
        ) ) return new ControlBlock(
            closeParen, -1, -1
        );

        int bdepth = 1;
        int j      = afterParen + 1;
        while(j < n && bdepth > 0) {
            final Token tk = tokens.get(j);
                 if( isPunct(tk, "{") ) bdepth++;
            else if( isPunct(tk, "}") ) bdepth--;
            ++j;
        }
        if(bdepth != 0) return null;

        return new ControlBlock(closeParen, afterParen, j - 1);
    }

    /**
     * Returns the collapsed single-line rendering of the keyword/condition plus the block's
     * lone statement, or null if the body does not qualify for §10 omission: more than one
     * top-level `;`, trailing content after the sole `;` other than a comment, an interleaved
     * comment before the statement, or a nested compound construct as the body
     */
    private String tryCollapse(
        final List<Token>  tokens,
        final int          kwIndex,
        final ControlBlock block
    )
    {
        final List<Token> contents = tokens.subList(
            block.openBraceIndex + 1, block.closeBraceIndex
        );
        if( !isSingleStatementBody(contents) ) return null;
        // `renderInline` flattens every gap (whitespace/newline) to a single space, with no
        // special handling for a `//` line comment -- a line comment's text runs to the end of
        // its original physical line, so once flattened, every token that followed it in the
        // source (remaining condition tokens, the closing `)`, and -- via the caller's own
        // `prefix + " " + body` join -- the entire collapsed body/`;`/enclosing `}` too) gets
        // silently absorbed into that one `//` comment and vanishes from the rendered output.
        // Found via real-code testing (NVIDIA/stdexec's `__detail/__counting_scopes.hpp`,
        // `__base_scope::try_join`'s two `compare_exchange_weak(...)` calls, each with a
        // multi-line argument list carrying trailing `//` comments between arguments) -- a
        // 50-error compile cascade rooted in "expected '}' at end of input" once collapsed.
        // Refuse to collapse (leave the original braced, multi-line form untouched) whenever the
        // condition itself carries a line comment; block comments (`/* ... */`) don't extend to
        // end-of-line and are safe to inline as-is.
        if( containsLineComment( tokens.subList(kwIndex, block.closeParenIndex + 1) ) ) return null;
        // C6e: same hazard as this method's own condition-comment guard just above, but for a
        // multi-statement trailing-lambda body used as a boolean sub-expression *inside* the
        // condition itself (`.all { ... }`/`.any { ... }`/`.none { ... }` called directly in an
        // `if(...)`/`&&`/`||` position, e.g. `if (result.all { val klass = ...; klass != null &&
        // ... })`) -- structurally the same family as the already-fixed C3
        // (`MiscRuleCurly.renderCallCandidate`'s brace-depth-aware bail), but reached via a
        // different code path: C3 covers a lambda as a call's own *argument*; here the lambda is
        // embedded as a sub-expression of the `if`'s condition, which this method renders via the
        // condition-flattening `renderInline` call below, with no brace-depth awareness at all. Found
        // via JetBrains/kotlin real-code testing (`KClassImpl.kt`'s
        // `computeAllSupertypes`/`computeLegacySupertypes`: `if (result.all { val klass = ... klass
        // != null && (...) }) { result += StandardKTypes.ANY }` fused the lambda's two statements
        // onto one line with no separator, a compile error). Bail (leave the original, still-braced
        // multi-line form untouched) whenever the condition itself contains a nested `{...}` block
        // spanning more than one physical line -- reuses `containsMultilineNestedBrace`, the same
        // helper `isKotlinSingleStatementBody`/`collapseBracelessBody` already use to guard the body
        // side of this same collapse family.
        if( containsMultilineNestedBrace(
            tokens.subList(kwIndex, block.closeParenIndex + 1)
        ) ) return null;
        // Refuse to collapse when the body itself contains a `{`/`}` pair (an object literal or
        // similar brace-delimited expression, e.g. `if (x) foo.value = { a: 1, b: 2 };`) --
        // `renderInline` flattens the whole body (including that inner literal's own original
        // multi-line layout, if any) onto one physical line with no structural memory of it.
        // `enforceCallLineBreaking` can later re-wrap a call nested inside that literal, but has
        // no signal that the literal's own closing `}` should land on its own line rather than
        // staying fused to whatever text follows the wrapped call's own closing `)` -- stable only
        // once the input already has that closing `}` split onto its own line from a prior format
        // pass (i.e. never stable starting from a fresh, still-braced source). Leaving the body
        // braced sidesteps the whole class of bug entirely; STYLE.md's own worked examples for
        // this rule never include an object-literal consequent, so no coverage is lost (found via
        // vuejs/core real-code testing, `parser.ts`'s `onCloseTag`'s `inlineTemplateProp.value =
        // { ... }` and `compiler-sfc/resolveType.ts`'s analogous shape).
        if( containsBrace(contents) ) return null;
        final String prefix    = tightenParenPrefix(
            tokens.get(kwIndex).text,
            renderInline( tokens.subList(kwIndex, block.closeParenIndex + 1) )
        );
        final String body      = renderInline(contents);
        final String candidate = prefix + " " + body;
        // JS/TS root cause #3 (STATE_JS_TS.md, "2026-07-30 design/scoping pass") -- see
        // `refuseUnrescuableCollapse`'s javadoc for the full mechanism.
        if( refuseUnrescuableCollapse(
            tokens, kwIndex, kwIndex, block.closeBraceIndex - 1, candidate
        ) ) return null;

        return candidate;
    }

    /**
     * True iff any token in {@code slice} is a `{` or `}` punctuator -- see {@link #tryCollapse}
     *  for why a body containing one is refused collapse
     */
    private boolean containsBrace(final List<Token> slice)
    {
        for(final Token t : slice) {
            if( isPunct(t, "{") || isPunct(t, "}") ) return true;
        }

        return false;
    }

    /**
     * True iff any token in {@code slice} is a {@code COMMENT_LINE} -- the "unsafe to flatten
     *  onto one physical line via {@link #renderInline}" signal used by {@link #tryCollapse}
     */
    private boolean containsLineComment(final List<Token> slice)
    {
        for(final Token t : slice) {
            if(t.type == TokenType.COMMENT_LINE) return true;
        }

        return false;
    }

    /**
     * Strips the space between {@code keyword} and a following `(` in {@code rendered} if
     *  {@code keyword} is one of {@link #TIGHT_PAREN_KEYWORDS} -- see {@link #tryCollapseBraceless}
     *  for why this must happen at collapse time rather than being left to a later pass
     */
    private String tightenParenPrefix(final String keyword, final String rendered)
    {
        if( TIGHT_PAREN_KEYWORDS.contains(
            keyword
        ) && rendered.startsWith(
            keyword + " ("
        ) ) return keyword + rendered.substring(
            keyword.length() + 1
        );

        return rendered;
    }

    /**
     * Indentation (spaces/tabs) of the physical source line that {@code index} sits on, found
     * by walking backward for the nearest {@code NEWLINE} token and reading the
     * {@code WHITESPACE} token right after it. For a K&R `} else if (...)` line this is the
     * closing brace's own indent, which is what a collapsed chain's `else`/`else if` line should
     * inherit -- it always matches the opening `if`'s indent in practice, since the `}` that
     * used to sit there was itself aligned with its `if`. Returns "" if no indent is found (e.g.
     * {@code index} is on the first line of the file).
     */
    private String mostRecentLineIndent(final List<Token> tokens, final int index)
    {
        int p = Math.min( index, tokens.size() ) - 1;
        while( p >= 0 && tokens.get(p).type != TokenType.NEWLINE ) p--;
        final int wsIdx = p + 1;
        if( wsIdx < tokens.size() && tokens.get(
            wsIdx
        ).type == TokenType.WHITESPACE ) return tokens.get(
            wsIdx
        ).text;

        return "";
    }

    /**
     * Scans forward from {@code start} (the token right after an already-braceless `if`/
     * `else if`/`for`/`while`'s condition-closing `)`) for the top-level (depth-0 in `(`/`[`/`{`)
     * terminating `;` of its single-statement body, returning the index just past it, or -1 if
     * none is found before {@code n} or a `{` is encountered (meaning this is not actually a
     * simple single-statement braceless body -- caller leaves the input untouched rather than
     * guess). See the C/C++/Java already-braceless branch in
     * {@link #collapseSingleExpressionBlocks} for why this is needed at all.
     */
    private int findBracelessStatementEnd(final List<Token> tokens, final int start, final int n)
    {
        int depth = 0;
        for(int k = start; k < n; ++k) {
            final Token t = tokens.get(k);
            if( isPunct(t, "(") || isPunct(t, "[") ) {
                ++depth;
            }
            else if( isPunct(t, ")") || isPunct(t, "]") ) {
                --depth;
                if(depth < 0) return -1;
            }
            else if( isPunct(t, "{") || isPunct(t, "}") ) {
                return -1;
            }
            else if( depth == 0 && isPunct(t, ";") ) {
                return k + 1;
            }
        } // for

        return -1;
    }

    /**
     * After a chain member (`if`/`else if`) has just been collapsed to one line, forces the
     * following `else` (if the next significant token is one) onto its own line at
     * {@code indent} -- K&R input has `} else` sitting on the same physical line as the closing
     * brace, which {@link #collapseSingleExpressionBlocks} would otherwise reproduce verbatim,
     * joining the whole chain onto a single line instead of the Allman-per-branch,
     * column-aligned shape {@link #alignBracelessElseIfChain} expects to align. Returns the
     * index to resume scanning from (unchanged if the next token is not `else`).
     *
     * <p>If the gap between {@code from} and the `else` already contains a real {@code NEWLINE}
     * (the already-braceless, second-round-or-later case -- the previous round's
     * {@link #alignBracelessElseIfChain} may have left-padded the *preceding* `if`/`else if`
     * line's own indent to column-align its keyword, per RDD_KEY's keyword-alignment extension),
     * that gap's own text is copied through verbatim instead of being discarded and resynthesized
     * from {@code indent} -- {@code indent} is derived from the `if`/`else if` statement's own
     * current line ({@link #mostRecentLineIndent}), which is exactly the line that padding may
     * have widened, so blindly reusing it here would leak that cosmetic padding onto the
     * following `else`'s own, already-correct, unpadded indent -- a non-idempotency bug. Only
     * synthesize a fresh `indent` when there is no pre-existing newline to preserve (the classic
     * same-line K&R `} else` case this method was originally written for).
     */
    private int appendChainNewlineBeforeElse(
        final List<Token>   tokens,
        final int           from,
        final int           n,
        final StringBuilder out,
        final String        indent
    )
    {
        final int next = skipWhitespaceOnly(tokens, from);
        if( next < n && tokens.get(
            next
        ).type == TokenType.KEYWORD && "else".equals(
            tokens.get(next).text
        ) ) {
            boolean hasNewline = false;
            for(int g = from; g < next; ++g) {
                if( tokens.get(g).type == TokenType.NEWLINE ) {
                    hasNewline = true;
                    break;
                }
            }
            if(hasNewline) {
                for(int g = from; g < next; ++g) out.append( tokens.get(g).text );
            }
            else {
                out.append('\n').append(indent);
            }

            return next;
        } // if

        return from;
    }

    /**
     * True iff {@code contents} (a braced body's interior) holds exactly one qualifying
     *  top-level statement per STYLE.md §10 -- extracted out of {@link #tryCollapse} so the
     *  same check can be dry-run, without producing any rendered text, over every branch of an
     *  {@code if}/{@code else if}/{@code else} chain (see {@link #chainAllBranchesCollapsible})
     *  to decide whether the *whole* chain qualifies before any single branch is touched.
     */
    private boolean isSingleStatementBody(final List<Token> contents)
    {
        final List<Token> sig = new ArrayList<>();
        for(final Token t : contents) {
            if(t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) sig.add(t);
        }
        if( sig.isEmpty() ) return false;
        // A local variable declaration ("final boolean ignored = ...;"/"const auto x = ...;")
        // is not a legal braceless if/while/for body in C/C++/Java -- only an *expression*
        // statement, `;`, or a nested compound statement/keyword-statement qualifies there.
        // `final`/`const` as the leading token is an unambiguous declaration signal (found via
        // real-code testing, apache/ant's FileUtils.java: `if (!f.canWrite() && ON_WINDOWS) {
        // final boolean ignored = f.setWritable(true); }` collapsed to a braceless `if` whose
        // body is a bare declaration -- javac rejects it with "variable declaration not allowed
        // here"). Refuse collapse rather than emit invalid code.
        if( "final".equals( sig.get(0).text ) || "const".equals( sig.get(0).text ) ) return false;
        // Sibling case to the `final`/`const` check just above: an un-qualified declaration whose
        // leading token is itself a primitive/built-in type keyword (`int x = ...;`) is just as
        // illegal a braceless body -- only Kotlin's `val`/`var` are handled separately below since
        // Kotlin has no primitive-type-keyword declaration shape.
        if( !lang.isKotlin
                && sig.size() >= 2
                && sig.get(0).type == TokenType.KEYWORD
                && PRIMITIVE_TYPE_KEYWORDS.contains( sig.get(0).text )
                && sig.get(1).type == TokenType.IDENTIFIER
        ) return false;

        int semiCount = 0;
        int semiIdx   = -1;
        for( int k = 0; k < sig.size(); ++k ) {
            if( isPunct( sig.get(k), ";" ) ) {
                ++semiCount;
                semiIdx = k;
            }
        }
        if(lang.isKotlin) {
            // Kotlin has no mandatory statement-terminating `;` (STYLE_KOTLIN.md §1) -- a body can
            // hold several newline-separated statements with an optional trailing `;` on only the
            // last one (e.g. `_K += kappa\ngrisuRound(...)\nreturn len;`, found via
            // `JetBrains/kotlin` real-code testing, `Number2String.kt`'s `grisuFastPath`): the
            // C/C++/Java `semiCount == 1` fast path below wrongly treated that as a qualifying
            // single statement (its own no-comment-after/no-comment-before checks say nothing about
            // an earlier *un-terminated* statement boundary), so `tryCollapse` fused all three
            // statements onto one physical line with no separators -- a real compile error. Route
            // every Kotlin body through `isKotlinSingleStatementBody` instead, regardless of
            // `semiCount`; it re-derives "exactly one top-level statement" from depth-0 NEWLINE
            // boundaries, which is correct whether or not a trailing `;` happens to be present.
            if( !isKotlinSingleStatementBody(contents) ) return false;
        } // if
        else if(semiCount == 1) {
            for( int k = semiIdx + 1; k < sig.size(); ++k ) {
                final TokenType ty = sig.get(k).type;
                if(ty != TokenType.COMMENT_LINE && ty != TokenType.COMMENT_BLOCK) return false;
            }
            for(int k = 0; k < semiIdx; ++k) {
                final TokenType ty = sig.get(k).type;
                if(ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) return false;
            }
        }
        else {
            return false;
        }

        final Token first = sig.get(0);
        if(first.type != TokenType.KEYWORD) return true;
        // A local declaration (`val`/`var`) is a statement, not an expression -- Kotlin does not
        // allow `if (x) val y = ...` without braces (declarations are illegal as the sole body of
        // a braceless control-flow statement). Disqualify it the same way COMPOUND_BODY_KEYWORDS
        // disqualifies a nested compound body, or `renderInline`/`collapseBracelessBody`'s later
        // flattening step would emit invalid Kotlin. Found via arrow-kt/arrow real-code testing
        // (`RaiseAccumulate.kt`'s `addErrors`: `if (errors != null) { val _ = accumulateAll(errors) }`
        // was collapsed to the illegal `if (errors != null) val _ = accumulateAll(errors)`).
        if( "val".equals(first.text) || "var".equals(first.text) ) return false;

        return !COMPOUND_BODY_KEYWORDS.contains(first.text);
    }

    /**
     * Kotlin-only sibling of the `;`-count check above: true iff {@code contents} (a braced
     *  body's interior, including whitespace/newlines) holds exactly one top-level statement,
     *  where "top-level" means outside any `(`/`[`/`{` nesting and no comment tokens are present
     *  anywhere in the body. A depth-0 {@code NEWLINE} after some content has already been seen
     *  marks a statement boundary; any further significant token after that boundary means more
     *  than one statement, so the body doesn't qualify for the §10 single-statement omission.
     */
    private boolean isKotlinSingleStatementBody(final List<Token> contents)
    {
        // A syntactically-single statement (e.g. a `when`/`synchronized`/`if`-as-expression) can
        // still carry its own nested `{...}` block with several statements inside -- `renderInline`
        // (used by every caller of this method to actually produce the collapsed text) flattens
        // *all* whitespace/newlines in `contents` to a single space with no brace-depth awareness,
        // so collapsing here would fuse that nested block's separate statements onto one physical
        // line with no `;` separators (Kotlin has none) -- a real compile error. Found via
        // kotlinx.coroutines real-code testing (`LimitedDispatcher.kt`'s
        // `obtainTaskOrDeallocateWorker()`: a `while (true) { when (...) { null -> synchronized(lock)
        // { stmt; stmt; stmt } ... } }` had the entire multi-statement `synchronized` body fused).
        // Bail out (refuse to treat as a collapsible single statement) whenever a nested `{...}`
        // block contains an internal newline at brace-depth > 0 -- that block must stay exploded.
        if( containsMultilineNestedBrace(contents) ) return false;
        int     depth                   = 0;
        boolean sawContent              = false;
        boolean sawBoundaryAfterContent = false;
        for(final Token t : contents) {
            if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) return false;
            if(t.type == TokenType.PUNCT) {
                     if( "(".equals(t.text) || "[".equals(t.text) || "{".equals(t.text) ) depth++;
                else if( ")".equals(t.text) || "]".equals(t.text) || "}".equals(t.text) ) depth--;
            }
            if( ( t.type == TokenType.NEWLINE || isPunct(t, ";") ) && depth == 0 ) {
                if(sawContent) sawBoundaryAfterContent = true;
                continue;
            }
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) continue;
            if(sawBoundaryAfterContent) return false;
            sawContent = true;
        } // for

        return sawContent;
    }

    /**
     * True iff {@code contents} contains a nested {@code {...}} block (brace-depth > 0 relative
     *  to {@code contents} itself) with at least one NEWLINE inside it -- i.e. the block is not a
     *  trivial one-liner and must stay exploded across physical lines. See
     *  {@link #isKotlinSingleStatementBody}'s javadoc for why this matters: a single Kotlin
     *  statement (one `when`/`synchronized`/etc.) can itself own such a block, and flattening it
     *  via {@code renderInline} would fuse its separate inner statements with no separator.
     */
    private boolean containsMultilineNestedBrace(final List<Token> contents)
    {
        int braceDepth = 0;
        for(final Token t : contents) {
                 if( t.type == TokenType.PUNCT && "{".equals(t.text) ) braceDepth++;
            else if( t.type == TokenType.PUNCT && "}".equals(t.text) ) braceDepth--;
            else if(t.type == TokenType.NEWLINE && braceDepth > 0)     return true;
        }

        return false;
    }

    /**
     * Kotlin-only sibling of {@link #tryCollapse} for a body that is already brace-free but
     *  spans more than one physical line (`if(x)\n    stmt`) -- joins the condition and its sole
     *  statement onto one line per STYLE.md §10, mirroring the braced path's single-statement
     *  and no-nested-compound-body checks. Scans forward from right after the condition's `)`,
     *  tracking `(`/`[`/`{` nesting depth, until a depth-0 {@code NEWLINE} following some
     *  content (the statement's own end) or a depth-going-negative `}` (this body was the last
     *  statement in its enclosing scope) is reached; bails (returns null, leaving the input
     *  untouched) on any comment token in the body, an empty body, or a nested compound-body
     *  keyword as the sole statement -- same conservative posture as {@link #tryCollapse}. On
     *  success, {@code outBodyEnd[0]} is set to the token index one past the joined body (so the
     *  caller's main loop can resume scanning from there).
     */
    /**
     * Keywords whose own text is directly tight against a following `(` with no space
     *  (`if(`, `while(`, `for(`, `switch(`, `catch(`, `when(`) -- mirrors {@code
     *  MiscRuleCore.TIGHT_PAREN_KEYWORDS} exactly (kept as its own private copy rather than a shared
     *  import since the two classes have no other coupling). Used by {@link #tryCollapseBraceless}
     *  below to make its collapsed prefix already reflect the final tight spacing, rather than the
     *  original source's `if (` form.
     */
    private static final Set<String> TIGHT_PAREN_KEYWORDS = setOf(
        "if", "while", "for", "switch", "catch", "when"
    );

    private String tryCollapseBraceless(
        final List<Token>  tokens,
        final int          kwIndex,
        final ControlBlock block,
        final int[]        outBodyEnd
    )
    {
        // C6f: same hazard as `tryCollapse`'s own condition-comment guard just above it in this
        // file (see that guard's comment for the full mechanism) -- `renderInline` below flattens
        // the condition's whitespace/newlines to single spaces with no notion that a `//` line
        // comment consumes to end-of-line, so a condition containing one (e.g. a comment nested
        // deep inside a trailing-lambda argument of a call within the condition, not just a
        // top-level comment) silently swallows every token that follows it -- the rest of the
        // condition, the closing `)`, and (via this method's own `prefix + " " + body` join in
        // `collapseBracelessBody`) the entire braceless body too. `tryCollapse` picked this up via
        // its own guard at line ~454, but `tryCollapseBraceless` -- structurally the sibling path
        // for a braceless (non-`{}`) single-statement body -- never had the analogous check, so a
        // condition-embedded comment reaching this method specifically was never caught. Found via
        // JetBrains/kotlin real-code testing (`KClassMembers.kt`'s
        // `isVisibleAsFunctionInCurrentClass`: `if (outer.any { ... inner.any { ... call(prop) {
        // if (cond) x else { // comment\n// comment2\nstmt } } && (...) } }) return false` --the
        // condition's own nested trailing-lambda body carries the comment, several call-levels
        // deep, well past this method's own `kwIndex..closeParenIndex` slice boundary but still
        // inside it). Bail (leave the input untouched) exactly like `tryCollapse` does.
        if( containsLineComment( tokens.subList(kwIndex, block.closeParenIndex + 1) ) ) return null;
        // C6e: sibling guard to `tryCollapse`'s own condition-embedded-multi-statement-lambda check
        // (see that method's comment for the full mechanism/repro) -- a braceless `if(...)`'s
        // condition can equally embed a multi-statement trailing lambda as a boolean sub-expression
        // (`.all { ... }`/`.any { ... }` etc.), which `renderInline` below would flatten with no
        // separator. Bail exactly like `tryCollapse` does.
        if( containsMultilineNestedBrace(
            tokens.subList(kwIndex, block.closeParenIndex + 1)
        ) ) return null;
        // Tighten `keyword (` -> `keyword(` here, at collapse time, rather than leaving it to a
        // later pass (MiscRuleCore's own tight-paren-keyword spacing fix). Left untightened, this
        // collapsed line is one character too wide right at the line-length boundary -- found via
        // real-code testing against `square/okio`'s `ZipFiles.kt`: a braced `if (...) { throw
        // IOException("...") }` whose true final-width single-line form is exactly 100 chars (the
        // limit) collapsed with the untightened `if (` prefix measured 101 chars by a later
        // call-line-breaking pass that runs before the tight-paren-spacing fixup, wrapping the
        // call unnecessarily; reformatting that already-wrapped output on a second pass measured
        // the (by-then-tightened) 100-char line correctly and left it on one line -- a real,
        // reproducible idempotency bug, not just cosmetic.
        final String prefix = tightenParenPrefix(
            tokens.get(kwIndex).text,
            renderInline( tokens.subList(kwIndex, block.closeParenIndex + 1) )
        );

        return collapseBracelessBody(
            tokens, kwIndex, block.closeParenIndex + 1, prefix, outBodyEnd
        );
    }

    /**
     * Shared body-scanning/rendering core of {@link #tryCollapseBraceless}, generalized to start
     * scanning from an arbitrary {@code fromIndex} rather than always right after an `if`/`while`/
     * `for` condition's closing `)` -- lets a bare braceless `else\n    stmt` (no condition of its
     * own to anchor on) reuse the exact same single-statement/no-comment/no-nested-compound-body
     * qualification logic via the caller passing {@code elseKwIndex + 1} instead. {@code prefix}
     * is prepended verbatim (already-rendered `if(...)`/`while(...)`/`for(...)`, or the literal
     * `"else"`) ahead of a single space and the rendered body.
     */
    private String collapseBracelessBody(
        final List<Token> tokens,
        final int         indentAnchorIdx,
        final int         fromIndex,
        final String      prefix,
        final int[]       outBodyEnd
    )
    {
        final int n         = tokens.size();
        final int bodyStart = skipNonSignificant(tokens, fromIndex);
        if(bodyStart >= n) return null;
        int     depth              = 0;
        boolean sawContent         = false;
        boolean sawTrailingComment = false;
        int     bodyEnd            = -1;
        for(int k = bodyStart; k < n; ++k) {
            final Token t = tokens.get(k);
            if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                // A comment embedded before the rest of the body's own content would need to move
                // out of place to render inline -- bail, same conservative posture as everywhere
                // else in this class. But a genuine *trailing* same-line comment on the single
                // statement (nothing but the comment itself between it and the body's terminating
                // newline/`}`) is safe to carry along verbatim onto the collapsed line.
                if(!sawContent) return null;
                sawTrailingComment = true;
                continue;
            } // if
            if(sawTrailingComment && t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) return null;
            if(t.type == TokenType.PUNCT) {
                if( "(".equals(t.text) || "[".equals(t.text) || "{".equals(t.text) ) {
                    ++depth;
                }
                else if( ")".equals(t.text) || "]".equals(t.text) ) {
                    --depth;
                }
                else if( "}".equals(t.text) ) {
                    if(depth == 0) {
                        bodyEnd = k; // This body was the last statement in its enclosing scope
                        break;
                    }
                    --depth;
                }
            } // if
            if(t.type == TokenType.NEWLINE && depth == 0) {
                if(sawContent) {
                    bodyEnd = k;
                    break;
                }
                continue;
            } // if
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) continue;
            sawContent = true;
        } // for
        if(!sawContent) return null;
        if(bodyEnd < 0) bodyEnd = n; // Ran off the end of `tokens` (e.g. this body is the very last statement)
        final List<Token> contents = tokens.subList(bodyStart, bodyEnd);
        final List<Token> sig      = new ArrayList<>();
        for(final Token t : contents) {
            if(t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) sig.add(t);
        }
        if( sig.isEmpty() ) return null;
        final Token first = sig.get(0);
        if( first.type == TokenType.KEYWORD && COMPOUND_BODY_KEYWORDS.contains(
            first.text
        ) ) return null;
        // Unlike `tryCollapse`'s braced-body path (which routes through
        // `isKotlinSingleStatementBody`, itself gated by `containsMultilineNestedBrace`), this
        // braceless-body scan above only tracks `(`/`[`/`{` nesting depth to find where the body
        // *ends* -- it never verifies the body is actually a single statement once it can itself
        // own a multi-line `{...}` block (e.g. a trailing-lambda call like `buildList(10) { ... }`
        // as the sole statement of a braceless `if`/bare `else`). `renderInline` below flattens
        // every WHITESPACE/NEWLINE in `contents` to one space with no brace-depth awareness, so
        // without this check such a body's own internal multi-statement block gets fused onto one
        // line with no `;` separator between its statements -- a genuine Kotlin compile error.
        // Found via arrow-kt/arrow real-code testing (`Either.kt`'s `zipOrAccumulate`: `else
        // buildList(10) { if (a is Left) add(a.value) if (b is Left) add(b.value) ... }` had its
        // ten `if`/`add` statements fused with no separator). Bail (leave the input untouched,
        // same conservative posture as every other guard in this method) whenever the body
        // contains such a block -- mirrors `tryCollapse`'s own guard exactly.
        if( containsMultilineNestedBrace(contents) ) return null;

        final String body = renderInline(contents);
        outBodyEnd[0] = bodyEnd;
        // D4 (JetBrains/kotlin dogfood): when this braceless body's own enclosing scope's real
        // `}` immediately terminates it on the same physical line (the "}"-at-depth-0 break case
        // above, `bodyEnd` pointing at that `}` token, not the NEWLINE-at-depth-0 case), `contents`
        // -- and thus `renderInline`'s `body` -- ends at the last real body token; `renderInline`
        // never emits *trailing* whitespace (see its own doc), so a source-preserved single space
        // between the body's last token and that following `}` (e.g. `) }`) was silently dropped,
        // producing `)}` -- a real round1-vs-round2 flap once a prior pass had already stripped an
        // enclosing `if`'s own braces (this method's caller then reprocesses an
        // already-braceless, already-multi-line body via this path instead of `tryCollapse`'s
        // braced path, which never has this loss since it lets the outer loop re-append that
        // untouched whitespace token verbatim rather than folding it into a render). Preserve it by
        // re-appending a single space whenever the source actually had one there.
        final boolean restoreTrailingSpace = bodyEnd < n && isPunct(
            tokens.get(bodyEnd), "}"
        ) && bodyEnd > bodyStart && ( tokens.get(
            bodyEnd - 1
        ).type == TokenType.WHITESPACE || tokens.get(
            bodyEnd - 1
        ).type == TokenType.NEWLINE );
        final String  candidate            = restoreTrailingSpace ? prefix + " " + body + " " : prefix + " " + body;
        // JS/TS root cause #3 (STATE_JS_TS.md, "2026-07-30 design/scoping pass"): refuse this
        // collapse when the joined one-line candidate would exceed `lineLengthLimit` AND nothing
        // later can rescue it (no breakable call in the body for `MiscRuleCurly
        // .enforceCallLineBreaking` to wrap across lines afterward) -- see `refuseUnrescuableCollapse`'s
        // own javadoc for the full mechanism/why a naive width-only guard was reverted before.
        if( refuseUnrescuableCollapse(
            tokens, indentAnchorIdx, indentAnchorIdx, bodyEnd - 1, candidate
        ) ) return null;

        return candidate;
    }

    /**
     * Braceless-body sibling of {@link #isPartOfElseChain} -- unlike the braced-body case
     *  (STYLE_C_CPP.md §10, whole chain forced to stay braced together), a braceless `else if`
     *  branch has no such all-or-nothing rule: each branch of an {@code if}/{@code else if}/
     *  {@code else} chain collapses to one line independently, same STYLE.md §10 single-statement
     *  omission as a standalone {@code if}. {@link KotlinSpecificRule#alignBracelessElseIfChain}
     *  runs afterward to column-align the whole collapsed chain's bodies. Always {@code false} --
     *  kept as a named hook (rather than inlined at the one call site) documenting that this was a
     *  deliberate choice, not an oversight.
     */
    private boolean isPartOfElseChainBraceless(
        final List<Token>  tokens,
        final int          kwIndex,
        final ControlBlock block,
        final int          n
    )
    {
        return false;
    }

    /**
     * True if the `if` at {@code kwIndex} is part of an {@code else}/
     *  {@code else if} chain -- either its closing `}` is followed by {@code else},
     *  or the keyword itself is directly preceded by {@code else} (i.e. it is an
     *  {@code else if} branch). For Kotlin, membership alone suppresses collapse of this
     *  particular braced branch (unchanged legacy behavior -- Kotlin's own chain collapse is
     *  driven entirely through the already-braceless path, see
     *  {@link #isPartOfElseChainBraceless}). For C/C++/Java, membership only suppresses
     *  collapse when the *whole* chain does not qualify to collapse together
     *  (see {@link #chainAllBranchesCollapsible}) -- STYLE_C_CPP.md §10's all-or-nothing rule
     *  cuts both ways: a chain where every branch is a qualifying single statement is free to
     *  drop braces on every branch at once, opt-in only, never partially.
     */
    private boolean isPartOfElseChain(
        final List<Token>  tokens,
        final int          kwIndex,
        final ControlBlock block,
        final int          n
    )
    {
        if( !isElseChainMember(tokens, kwIndex, block, n) ) return false;
        if(lang.isKotlin) return true;

        return !chainAllBranchesCollapsible(tokens, kwIndex, n);
    }

    private boolean isElseChainMember(
        final List<Token>  tokens,
        final int          kwIndex,
        final ControlBlock block,
        final int          n
    )
    {
        final int afterClose = skipNonSignificant(tokens, block.closeBraceIndex + 1);
        if( afterClose < n && tokens.get(
            afterClose
        ).type == TokenType.KEYWORD && "else".equals(
            tokens.get(afterClose).text
        ) ) return true;
        int prev = kwIndex - 1;
        while( prev >= 0 && ( tokens.get(
            prev
        ).type == TokenType.WHITESPACE || tokens.get(
            prev
        ).type == TokenType.NEWLINE ) ) prev--;

        return prev >= 0 && tokens.get(prev).type == TokenType.KEYWORD
                && "else".equals( tokens.get(prev).text );
    }

    /**
     * C/C++/Java-only whole-chain brace-safety scan (STYLE_C_CPP.md §10 opt-in, per user
     *  instruction: collapse every branch of an {@code if}/{@code else if}/{@code else} chain
     *  to braceless only when NONE of them needs to keep its braces -- a single multi-statement
     *  or otherwise non-qualifying branch anywhere in the chain leaves the *entire* chain
     *  untouched, byte-for-byte, never a partial collapse. Walks from the chain's first `if`
     *  (found by walking backward over any preceding {@code else}) forward through every
     *  {@code else if}/bare {@code else} branch; a branch that is already brace-free is
     *  trivially fine (nothing to remove) and does not block the scan. Returns {@code false}
     *  (chain stays fully braced) on any malformed/unbalanced structure, matching this class's
     *  conservative "don't guess" posture elsewhere.
     */
    private boolean chainAllBranchesCollapsible(
        final List<Token> tokens,
        final int         kwIndex,
        final int         n
    )
    {
        final int start = findChainStart(tokens, kwIndex);
        if(start < 0) return false;

        int pos = start;
        while(true) {
            if( tokens.get(
                pos
            ).type == TokenType.KEYWORD && "else".equals(
                tokens.get(pos).text
            ) ) {
                final int next = skipWhitespaceOnly(tokens, pos + 1);
                if( next < n && ( tokens.get(
                    next
                ).type == TokenType.COMMENT_LINE || tokens.get(
                    next
                ).type == TokenType.COMMENT_BLOCK ) ) return false;
                if( next < n && tokens.get(
                    next
                ).type == TokenType.KEYWORD && "if".equals(
                    tokens.get(next).text
                ) ) {
                    pos = next;
                    continue;
                }
                // Bare (terminal) else
                if(next >= n) return true;
                if( isPunct( tokens.get(next), "{" ) ) {
                    int depth = 1;
                    int j     = next + 1;
                    while(j < n && depth > 0) {
                        final Token tk = tokens.get(j);
                             if( isPunct(tk, "{") ) depth++;
                        else if( isPunct(tk, "}") ) depth--;
                        ++j;
                    }
                    if(depth != 0) return false;
                    return isSingleStatementBody( tokens.subList(next + 1, j - 1) );
                } // if
                return true; // Already brace-free bare else -- nothing to collapse, chain ends here
            } // if

            // `pos` is an `if`
            final ControlBlock block = matchControlBlock(tokens, pos);
            if(block == null) return false;
            if(block.openBraceIndex >= 0) {
                if( !isSingleStatementBody(
                    tokens.subList(block.openBraceIndex + 1, block.closeBraceIndex)
                ) ) return false;
                final int after = skipWhitespaceOnly(tokens, block.closeBraceIndex + 1);
                if( after < n && ( tokens.get(
                    after
                ).type == TokenType.COMMENT_LINE || tokens.get(
                    after
                ).type == TokenType.COMMENT_BLOCK ) ) return false; // Comment between this branch and the next -- don't guess past it
                if( after >= n || tokens.get(
                    after
                ).type != TokenType.KEYWORD || !"else".equals(
                    tokens.get(after).text
                ) ) return true; // Chain ends here, every branch so far qualifies
                pos = after;
                continue;
            } // if
            // Already brace-free branch: this session scopes the collapsible-chain check to
            // chains built entirely of originally-braced branches (the shape STYLE_C_CPP.md
            // §10's worked examples and this feature's own fixtures exercise); a chain with a
            // pre-existing brace-free branch bails conservatively rather than guessing at its
            // (harder to locate without a closing brace) statement boundary.
            return false;
        } // while
    }

    /**
     * Skips backward over a run of WHITESPACE/NEWLINE tokens only (comments deliberately NOT
     *  skipped -- see {@link #findChainStart}); returns the index of the nearest non-gap token,
     *  or -1 if none remains
     */
    private int skipWhitespaceOnlyBackward(final List<Token> tokens, final int from)
    {
        int i = from;
        while( i >= 0 && ( tokens.get(
            i
        ).type == TokenType.WHITESPACE || tokens.get(
            i
        ).type == TokenType.NEWLINE ) ) i--;

        return i;
    }

    /**
     * Given the index of a `}` that is claimed to close the previous branch of an else-if
     *  chain, matches it backward to its `{`, then further back through the branch's own
     *  `( ... )` condition to the `if` keyword that opens it. Returns -1 if {@code closeBraceIdx}
     *  is not actually a `}`, or the structure is unbalanced/malformed.
     */
    private int closeBraceToOwningIf(final List<Token> tokens, final int closeBraceIdx)
    {
        if( closeBraceIdx < 0 || !isPunct( tokens.get(closeBraceIdx), "}" ) ) return -1;
        int depth = 1;
        int k     = closeBraceIdx - 1;
        while(k >= 0 && depth > 0) {
            final Token tk = tokens.get(k);
                 if( isPunct(tk, "}") ) depth++;
            else if( isPunct(tk, "{") ) depth--;
            --k;
        }
        if(depth != 0) return -1;
        final int openBrace  = k + 1;
        final int beforeOpen = skipWhitespaceOnlyBackward(tokens, openBrace - 1);
        if( beforeOpen < 0 || !isPunct( tokens.get(beforeOpen), ")" ) ) return -1;
        int pdepth = 1;
        int m      = beforeOpen - 1;
        while(m >= 0 && pdepth > 0) {
            final Token tk = tokens.get(m);
                 if( isPunct(tk, ")") ) pdepth++;
            else if( isPunct(tk, "(") ) pdepth--;
            --m;
        }
        if(pdepth != 0) return -1;
        final int openParen   = m + 1;
        final int beforeParen = skipWhitespaceOnlyBackward(tokens, openParen - 1);
        if( beforeParen >= 0 && tokens.get(
            beforeParen
        ).type == TokenType.KEYWORD && "if".equals(
            tokens.get(beforeParen).text
        ) ) return beforeParen;

        return -1;
    }

    /**
     * For the `if`/`else` keyword at {@code kwIndex} (a member of some else-if chain), finds
     *  the immediately preceding branch's own `if` keyword, if one exists. Returns -2 if
     *  {@code kwIndex} is an `if` with no `else` immediately before it (i.e. it has no
     *  predecessor -- it IS the chain's first branch). Returns -1 on a malformed/unbalanced
     *  structure, or if a comment sits in a gap this must see past to judge (conservative
     *  "don't guess past a comment" bail, matching this class's posture elsewhere -- e.g.
     *  `real_code_regressions_5_inp.cpp`'s `withComment`, a `/* ... *\/` directly between a
     *  branch's `}` and the following `else`).
     */
    private int prevChainBranchIf(final List<Token> tokens, final int kwIndex)
    {
        final boolean isElseTok = tokens.get(
            kwIndex
        ).type == TokenType.KEYWORD && "else".equals(
            tokens.get(kwIndex).text
        );
        if(isElseTok) {
            final int beforeElse = skipWhitespaceOnlyBackward(tokens, kwIndex - 1);
            if( beforeElse < 0 || tokens.get(
                beforeElse
            ).type == TokenType.COMMENT_LINE || tokens.get(
                beforeElse
            ).type == TokenType.COMMENT_BLOCK ) return -1;
            return closeBraceToOwningIf(tokens, beforeElse);
        } // if
        // KwIndex is an `if`: an else-if only if directly preceded by `else`
        final int beforeIf = skipWhitespaceOnlyBackward(tokens, kwIndex - 1);
        if(beforeIf < 0) return -2;
        if( tokens.get(
            beforeIf
        ).type == TokenType.COMMENT_LINE || tokens.get(
            beforeIf
        ).type == TokenType.COMMENT_BLOCK ) {
            // A comment directly before this `if` can never mean it's an else-if (grammar
            // requires the literal `else` token immediately before, comments aside don't change
            // that), so this is just this chain's own first branch -- no predecessor to find
            return -2;
        }
        if( tokens.get(
            beforeIf
        ).type != TokenType.KEYWORD || !"else".equals(
            tokens.get(beforeIf).text
        ) ) return -2;
        final int beforeElse = skipWhitespaceOnlyBackward(tokens, beforeIf - 1);
        if( beforeElse < 0 || tokens.get(
            beforeElse
        ).type == TokenType.COMMENT_LINE || tokens.get(
            beforeElse
        ).type == TokenType.COMMENT_BLOCK ) return -1;

        return closeBraceToOwningIf(tokens, beforeElse);
    }

    /**
     * Walks backward from {@code anchorIndex} (an `if` or bare `else` token that is some
     *  member of an else-if chain) to the index of the chain's very first `if`, by repeatedly
     *  hopping to {@link #prevChainBranchIf}'s result. Returns -1 on any unbalanced structure or
     *  comment this must see past (see {@link #prevChainBranchIf}'s javadoc) -- notably NOT on
     *  reaching a branch with no predecessor, which just ends the walk successfully (that branch
     *  IS the chain start).
     */
    private int findChainStart(final List<Token> tokens, final int anchorIndex)
    {
        int cur = anchorIndex;
        while(true) {
            final int prevIf = prevChainBranchIf(tokens, cur);
            if(prevIf == -1) return -1;
            if(prevIf == -2) return cur;
            cur = prevIf;
        }
    }

    /** Joins tokens onto one line: any run of whitespace/newlines between tokens becomes one space */
    private String renderInline(final List<Token> tokens)
    {
        final StringBuilder sb           = new StringBuilder();
              boolean       pendingSpace = false;
        for(final Token t : tokens) {
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                if( sb.length() > 0 ) pendingSpace = true;
                continue;
            }
            if(pendingSpace) {
                sb.append(' ');
                pendingSpace = false;
            }
            sb.append(t.text);
        } // for

        return sb.toString();
    }

    // ── Non-function block brace style (STYLE.md §11) ───────────────────────────
    /**
     * Scans a token slice and moves every `{` that opens a control-flow body
     * (`if`/`else`/`else if`/`for`/`while`/`do`/`switch`/`try`/`catch`/`finally`), a named
     * construct (`class`/`struct`/`enum`/`enum class`/`namespace`/`interface`, already tagged
     * by the tokenizer's name stack -- see `Token.name`), or a lambda body (Java `(...) -> {`,
     * C++ `[...](...)  {` or its `-> Type {` trailing-return-type form -- STYLE_C_CPP.md §2 /
     * STYLE_JAVA.md §2) onto the same line as the preceding keyword/condition/declaration,
     * separated by exactly one space (K&R). A lambda is treated as a value embedded in a larger
     * declaration or call rather than a standalone definition, so unlike a named function it
     * does not get Allman style. Anything in the gap other than whitespace/newlines (i.e. a
     * comment) blocks the rewrite for that brace, since relocating the comment unambiguously is
     * out of scope here. A `{` that does not classify as one of those cases -- most importantly
     * a function/method definition, where the preceding `)`'s matching `(` is preceded by an
     * identifier rather than a keyword or a lambda's `]` -- is left completely untouched; Allman
     * function brace style is normalized elsewhere (Tier 1, language-specific files), not by
     * this method.
     */
    public String enforceKAndRBraceStyle(final List<Token> tokens)
    {
        final StringBuilder out = new StringBuilder();
        final List<Token>   gap = new ArrayList<>();
        final int           n   = tokens.size();
              int           i   = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                    || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                gap.add(t);
                ++i;
                continue;
            }

            // RDD_KEY_169: never collapse the gap onto a single space when the nearest real
            // token before it is a PREPROCESSOR directive (e.g. `#endif`) -- gluing the `{`
            // onto that line would put real code after the directive on the same physical
            // line, which a `#endif`/`#if`/etc. token's own opaque to-end-of-line lexing then
            // swallows whole on the next retokenization pass (the `{` vanishes into the
            // PREPROCESSOR token's text instead of being lexed as PUNCT), desyncing every
            // brace-depth/frame counter downstream. Leave the brace on its own line instead.
            final int     prevRealIdx        = prevSignificantIndex(tokens, i - 1);
            final boolean prevIsPreprocessor = prevRealIdx >= 0 && tokens.get(
                prevRealIdx
            ).type == TokenType.PREPROCESSOR;
            if( isPunct(t, "{") && gap.stream().noneMatch(Token::isComment)
                    && qualifiesForKAndR(
                        tokens, i
                    ) && !t.frozen && gap.stream().noneMatch(
                        g -> g.frozen
                    )
                    && !prevIsPreprocessor ) {
                out.append(' ');
            }
            else {
                for(final Token g : gap) out.append(g.text);
            }
            gap.clear();
            out.append(t.text);
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    /** True if the `{` at braceIdx opens a K&R-styled construct per STYLE.md §11 (see caller doc). */
    private boolean qualifiesForKAndR(final List<Token> tokens, final int braceIdx)
    {
        if( tokens.get(braceIdx).name != null ) return true;

        final int prevIdx = prevSignificantIndex(tokens, braceIdx - 1);
        if(prevIdx < 0) return false;
        final Token prev = tokens.get(prevIdx);
        if( prev.type == TokenType.KEYWORD && BARE_KR_KEYWORDS.contains(prev.text) ) return true;
        if( isPunct(prev, ")") ) {
            final int openParenIdx = matchOpenBackward(tokens, prevIdx);
            if(openParenIdx >= 0) {
                final int kwIdx = prevSignificantIndex(tokens, openParenIdx - 1);
                if( kwIdx >= 0 && tokens.get(
                    kwIdx
                ).type == TokenType.KEYWORD && PAREN_KR_KEYWORDS.contains(
                    tokens.get(kwIdx).text
                ) ) return true;
            } // if
        } // if

        return isLambdaBrace(tokens, prevIdx);
    }

    /**
     * True if the `{` whose immediately preceding significant token is at prevIdx opens a
     * lambda body: Java `(params) -> {` / `param -> {`, or C++ `[capture](params) {` / bare
     * `[capture] {` / either form followed by a trailing `-> Type {`
     */
    private boolean isLambdaBrace(final List<Token> tokens, final int prevIdx)
    {
        final Token prev = tokens.get(prevIdx);
        if(lang.isJava) return isOp(prev, "->");
        if( isPunct(prev, "]") ) return true;
        if( isPunct(prev, ")") && precededByCaptureList(tokens, prevIdx) ) return true;

        return isCppTrailingReturnLambda(tokens, prevIdx);
    }

    /** True if the `)` at closeParenIdx's matching `(` is immediately preceded by a `]` */
    private boolean precededByCaptureList(final List<Token> tokens, final int closeParenIdx)
    {
        final int openParenIdx = matchOpenBackward(tokens, closeParenIdx);
        if(openParenIdx < 0) return false;
        final int beforeOpen = prevSignificantIndex(tokens, openParenIdx - 1);

        return beforeOpen >= 0 && isPunct( tokens.get(beforeOpen), "]" );
    }

    // Bounds the backward walk over a C++ trailing return type (`-> Type`) before giving up --
    // real return types are short, and this keeps a non-lambda `)` { with unrelated code before
    // it from causing a runaway scan
    private static final int MAX_RETURN_TYPE_TOKENS = 20;

    /** True if `{` at prevIdx+1(gap) is a C++ lambda's `[capture](params) -> Type {` body */
    private boolean isCppTrailingReturnLambda(final List<Token> tokens, final int prevIdx)
    {
        int j     = prevIdx;
        int steps = 0;
        while(j >= 0 && steps < MAX_RETURN_TYPE_TOKENS) {
            final Token cur = tokens.get(j);
            if( isOp(cur, "->") ) {
                final int beforeArrow = prevSignificantIndex(tokens, j - 1);
                if(beforeArrow < 0) return false;
                final Token b = tokens.get(beforeArrow);
                if( isPunct(b, "]") ) return true;
                return isPunct(b, ")") && precededByCaptureList(tokens, beforeArrow);
            } // if
            if( !isTypeIshToken(cur) ) return false;
            j = prevSignificantIndex(tokens, j - 1);
            ++steps;
        } // while

        return false;
    }

    /** Tokens that can plausibly appear inside a return-type expression before `->` */
    private boolean isTypeIshToken(final Token t)
    {
        switch(t.type) {
            case IDENTIFIER: /* FALL-THROUGH */
            case KEYWORD: /* FALL-THROUGH */
            case ANGLE_BRACKET_OPEN: /* FALL-THROUGH */
            case ANGLE_BRACKET_CLOSE:
                return true;
            case OP:
                return "::".equals(t.text) || "*".equals(t.text) || "&".equals(t.text);
            case PUNCT:
                return ",".equals(t.text);
            default:
                return false;
        } // switch
    }

    /**
     * True iff the `while` keyword at {@code whileIdx} is the tail of a `do { ... } while
     *  (cond)` construct -- i.e. the nearest preceding significant token is a `}` whose matching
     *  `{` is itself immediately preceded (skipping non-significant tokens) by a `do` keyword. See
     *  the call site's comment (RDD_KEY_151) for why this guard exists.
     */
    private boolean isDoWhileTailKeyword(final List<Token> tokens, final int whileIdx)
    {
        final int closeBraceIdx = prevSignificantIndex(tokens, whileIdx - 1);
        if( closeBraceIdx < 0 || !isPunct( tokens.get(closeBraceIdx), "}" ) ) return false;
        int depth = 1;
        int j     = closeBraceIdx - 1;
        while(j >= 0 && depth > 0) {
            final Token tk = tokens.get(j);
                 if( isPunct(tk, "}") ) depth++;
            else if( isPunct(tk, "{") ) depth--;
            --j;
        }
        if(depth != 0) return false;
        final int beforeOpenBrace = prevSignificantIndex(tokens, j);

        return beforeOpenBrace >= 0 && tokens.get(beforeOpenBrace).type == TokenType.KEYWORD
                && "do".equals( tokens.get(beforeOpenBrace).text );
    }

    /**
     * True when the `=` at {@code eqIdx} is a Kotlin expression-bodied *function's* tail
     * separator (`fun ... (...) = expr`) rather than a `val`/`var` declaration's initializer
     * separator (`val x = expr`) -- both look identical at the point of the `=` itself
     * (RDD_KEY_177). Distinguishes them by walking backward from `eqIdx` to the enclosing
     * statement's own leading keyword, tracking bracket depth so a nested `(`/`{` inside the
     * signature/initializer doesn't false-trigger on a boundary token that belongs to a nested
     * scope. Stops at the first depth-0 `fun` (function) or `val`/`var` (property) keyword seen,
     * or at a depth-0 statement boundary (`;`, `{`, `}`, or a depth-0 NEWLINE) if neither is
     * found first.
     */
    private boolean isFunctionExprBodyEquals(final List<Token> tokens, final int eqIdx)
    {
        int depth = 0;
        for(int j = eqIdx - 1; j >= 0; --j) {
            final Token tok = tokens.get(j);
            if( isPunct(tok, ")") || isPunct(tok, "]") || isPunct(tok, "}") ) {
                ++depth;
                continue;
            }
            if( isPunct(tok, "(") || isPunct(tok, "[") || isPunct(tok, "{") ) {
                if(depth == 0) return false;
                --depth;
                continue;
            }
            if(depth > 0) continue;
            if( tok.type == TokenType.KEYWORD && "fun".equals(tok.text) ) return true;
            if( tok.type == TokenType.KEYWORD && ( "val".equals(
                tok.text
            ) || "var".equals(
                tok.text
            ) ) ) return false;
            if( isPunct(tok, ";") || tok.type == TokenType.NEWLINE ) return false;
        } // for

        return false;
    }

    /** Index of the nearest significant token at or before `from`, or -1 if none */
    private int prevSignificantIndex(final List<Token> tokens, final int from)
    {
        int i = from;
        while(i >= 0) {
            final TokenType ty = tokens.get(i).type;
            if(ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) i--;
            else break;
        }

        return i;
    }

    /** Index of the `(` matching the `)` at closeParenIdx, via local backward depth counting, or -1 */
    private int matchOpenBackward(final List<Token> tokens, final int closeParenIdx)
    {
        int depth = 1;
        int i     = closeParenIdx - 1;
        while(i >= 0 && depth > 0) {
            final Token tk = tokens.get(i);
                 if( isPunct(tk, ")") || isPunct(tk, "]") ) depth++;
            else if( isPunct(tk, "(") || isPunct(tk, "[") ) depth--;
            --i;
        }

        return depth == 0 ? i + 1 : -1;
    }

    /**
     * Like {@link #skipNonSignificant} but stops AT a comment token instead of skipping past
     *  it -- used by the chain-collapse safety checks, where a comment sitting between two
     *  branches (`}` /* ... *\/ else {`) must block collapse of that gap rather than being
     *  silently skipped over, same "don't guess past a comment" posture as everywhere else in
     *  this class (see e.g. `real_code_regressions_5_inp.cpp`'s `withComment`).
     */
    private int skipWhitespaceOnly(final List<Token> tokens, final int from)
    {
        final int n = tokens.size();
              int i = from;
        while( i < n && ( tokens.get(
            i
        ).type == TokenType.WHITESPACE || tokens.get(
            i
        ).type == TokenType.NEWLINE ) ) i++;

        return i;
    }

    private int skipNonSignificant(final List<Token> tokens, final int from)
    {
        final int n = tokens.size();
              int i = from;
        while(i < n) {
            final TokenType ty = tokens.get(i).type;
            if(ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) i++;
            else break;
        }

        return i;
    }

    // ── `else` / `else if` placement (STYLE.md §12) ─────────────────────────────
    /**
     * Scans a token slice and ensures every `else`/`else if` that directly follows a block's
     * closing `}` starts on its own line, inserting a newline (plus the `}`'s own indentation,
     * so `else` lines up under it) when the two currently share a line. A gap that already
     * contains a newline -- including a deliberate blank line -- is left exactly as-is: STYLE.md
     * §12 treats that blank line as an optional, context-driven separator (e.g. the preceding
     * branch exits unconditionally) that this method must never add or remove on its own. A
     * comment sitting in the gap blocks the rewrite, since relocating it unambiguously is out of
     * scope. An `else` not directly preceded by a `}` -- e.g. the previous branch was itself a
     * brace-less single-statement body per §10 -- is left untouched; §12 only specifies
     * placement relative to a preceding block's closing brace.
     */
    public String placeElseOnOwnLine(final List<Token> tokens)
    {
        final StringBuilder out        = new StringBuilder();
        final List<Token>   gap        = new ArrayList<>();
        final int           n          = tokens.size();
              int           lastSigIdx = -1;
              int           i          = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                    || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                gap.add(t);
                ++i;
                continue;
            }

            if( t.type == TokenType.KEYWORD && "else".equals(t.text) && lastSigIdx >= 0
                    && isPunct( tokens.get(lastSigIdx), "}" )
                    && gap.stream().noneMatch(g -> g.type == TokenType.NEWLINE)
                    && !t.frozen && !tokens.get(lastSigIdx).frozen ) {
                final String indent = indentBefore(tokens, lastSigIdx);
                out.append('\n').append(indent);
                for(final Token g : gap) {
                    if( isComment(g) ) out.append(g.text);
                }
                if( gap.stream().anyMatch(Token::isComment) ) out.append('\n').append(indent);
            } // if
            else {
                for(final Token g : gap) out.append(g.text);
            }
            gap.clear();
            out.append(t.text);
            lastSigIdx = i;
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    /** The leading whitespace of the line containing the token at idx, or "" if it isn't first on its line */
    private String indentBefore(final List<Token> tokens, final int idx)
    {
        final StringBuilder indent = new StringBuilder();
              int           i      = idx - 1;
        while( i >= 0 && tokens.get(i).type == TokenType.WHITESPACE ) {
            indent.insert( 0, tokens.get(i).text );
            --i;
        }

        return ( i < 0 || tokens.get(i).type == TokenType.NEWLINE ) ? indent.toString() : "";
    }

    /**
     * Identical in structure to {@link #placeElseOnOwnLine}: when `catch` or `finally` is
     * found directly after a `}` on the same line (no newline in the gap, no comment in the
     * gap), it is moved onto the next line at the `}`'s own indentation level. This mirrors
     * how STYLE.md treats these keywords: their body `{` is K&R-placed (already handled by
     * {@link #enforceKAndRBraceStyle}), but the keyword itself follows the preceding closing
     * brace on a new line, not appended to it.
     */
    public String placeCatchFinallyOnOwnLine(final List<Token> tokens)
    {
        final StringBuilder out        = new StringBuilder();
        final List<Token>   gap        = new ArrayList<>();
        final int           n          = tokens.size();
              int           lastSigIdx = -1;
              int           i          = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                    || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                gap.add(t);
                ++i;
                continue;
            }

            if( t.type == TokenType.KEYWORD
                    && ( "catch".equals(t.text) || "finally".equals(t.text) )
                    && lastSigIdx >= 0
                    && isPunct( tokens.get(lastSigIdx), "}" )
                    && gap.stream().noneMatch(g -> g.type == TokenType.NEWLINE)
                    && !t.frozen && !tokens.get(lastSigIdx).frozen ) {
                final String indent = indentBefore(tokens, lastSigIdx);
                out.append('\n').append(indent);
                for(final Token g : gap) {
                    if( isComment(g) ) out.append(g.text);
                }
                if( gap.stream().anyMatch(Token::isComment) ) out.append('\n').append(indent);
            } // if
            else {
                for(final Token g : gap) out.append(g.text);
            }
            gap.clear();
            out.append(t.text);
            lastSigIdx = i;
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    // ── Named-construct blank lines (STYLE.md §7) ───────────────────────────────
    /**
     * Scans a token slice and ensures exactly one blank line immediately follows the `{` and
     * immediately precedes the `}` of every named construct (`class`/`struct`/`enum`/
     * `enum class`/`namespace`/`interface`/`extern "C"` -- anything the tokenizer tagged via
     * `Token.name`), regardless of how short the body is, per STYLE.md §7. A gap that already
     * has one or more blank lines is left untouched; a gap with exactly one newline gets a
     * second one inserted right after it; a gap with no newline at all (a same-line `{}` body)
     * gets `"\n\n"` prepended. Control-flow blocks (`for`/`while`/`if`/`switch`/etc., where
     * `Token.name` is null) are never touched here -- STYLE.md §7 says their existing blank
     * lines must be preserved exactly as written, which this method already does simply by
     * not inspecting them. See {@link #ensureBlankLine} for how a comment in the gap is handled.
     */
    public String insertNamedConstructBlankLines(final List<Token> tokens)
    {
        final int n = tokens.size();

        // Match every '{' to its '}' via simple depth counting, so a named brace's true
        // boundary can be found regardless of iteration order
        final Map<Integer, Integer> matchClose = new HashMap<>();
        final Deque<Integer>        braceStack = new ArrayDeque<>();
        for(int i = 0; i < n; ++i) {
            final Token t = tokens.get(i);
                 if( isPunct(t, "{") ) braceStack.push(i);
            else if( isPunct(
                t, "}"
            ) && !braceStack.isEmpty() ) matchClose.put(
                braceStack.pop(), i
            );
        } // for

        // For each named, non-empty-body '{', the guaranteed blank line normally belongs right
        // after '{' / right before '}' -- but if a preprocessor directive (e.g. an `#ifdef
        // __cplusplus` / `#endif` pair wrapping just the brace, as with a guarded `extern "C"`)
        // sits directly against the brace with no blank line of its own, the boundary moves past
        // it: the blank line separates the guard from the real body content, not the brace from
        // the guard immediately touching it.
        //
        // `blankBeforeIndent`/`blankAfterIndent` cover the case where the split point never was
        // the start of its own physical line to begin with (e.g. `struct Foo { enum Bar {` --
        // the whole nested construct started life on one source line): with no pre-existing
        // NEWLINE in that gap for `ensureBlankLine` to anchor indentation on, the body-first-line
        // gets the construct's own line indent plus one level, and the `}`-line gets the
        // construct's own line indent, mirroring where those lines would already be if this
        // hadn't been a same-line body.
        final Set<Integer>         blankBeforeIdx    = new HashSet<>();
        final Set<Integer>         blankAfterIdx     = new HashSet<>();
        final Map<Integer, String> blankBeforeIndent = new HashMap<>();
        final Map<Integer, String> blankAfterIndent  = new HashMap<>();
        for(int i = 0; i < n; ++i) {
            final Token t = tokens.get(i);
            if( !isPunct(t, "{") || t.name == null || isEmptyBraceBody(tokens, i) ) continue;
            final Integer closeIdx = matchClose.get(i);
            if(closeIdx == null) continue;
            final int beforeIdx = skipGuardForward(tokens, i);
            final int afterIdx  = skipGuardBackward(tokens, closeIdx);
            // Only the two boundary gaps this method actually rewrites need to be frozen-checked
            // -- not the entire body span. RDD_KEY_129: the old whole-span `anyFrozen(tokens, i,
            // closeIdx + 1)` check meant one unrelated JXM_CFMT_DIS/ENA region nested anywhere
            // inside this named construct's body (e.g. a frozen method deep inside an outer
            // class) silently suppressed the guaranteed blank line at the *outer* construct's own
            // `{`/`}`, even though neither boundary gap itself was ever going to be touched.
            if( anyFrozen(
                tokens, i, beforeIdx + 1
            ) || anyFrozen(
                tokens, afterIdx, closeIdx + 1
            ) ) continue;
            blankBeforeIdx.add(beforeIdx);
            blankAfterIdx.add(afterIdx);
            final String baseIndent = lineIndent(tokens, i);
            blankBeforeIndent.put(beforeIdx, baseIndent + indentUnit);
            blankAfterIndent.put(afterIdx, baseIndent);
        } // for

        final StringBuilder out             = new StringBuilder();
        final List<Token>   gap             = new ArrayList<>();
              int           lastSignificant = -1;
        for(int i = 0; i < n; ++i) {
            final Token t = tokens.get(i);
            if( isGap(t) ) {
                gap.add(t);
                continue;
            }
            final boolean needBefore = blankBeforeIdx.contains(i);
            final boolean needAfter  = lastSignificant >= 0 && blankAfterIdx.contains(
                lastSignificant
            );
            if(needBefore || needAfter) {
                final String indentIfNoNewline = needBefore ? blankBeforeIndent.get(
                    i
                ) : blankAfterIndent.get(
                    lastSignificant
                );
                out.append( ensureBlankLine(gap, indentIfNoNewline) );
            } // if
            else {
                for(final Token g : gap) out.append(g.text);
            }
            gap.clear();
            out.append(t.text);
            lastSignificant = i;
        } // for
        for(final Token g : gap) out.append(g.text);

        return out.toString();
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
     * Column width of a raw (not-yet-converted) leading-whitespace run, expanding each
     *  {@code '\t'} to the next {@link #indentWidth} tab stop -- duplicated from {@code
     *  MiscRuleCore.expandedIndentWidth} (this class has no shared ancestor with the `*Curly`
     *  rule-class hierarchy, same "each rule class matches its own local conventions" duplication
     *  precedent as {@code JavaSpecificRule.isSingleLineBody}'s own copy).
     */
    private int expandedIndentWidth(final String original)
    {
        int width = 0;
        for( int i = 0; i < original.length(); ++i ) width += ( original.charAt(
            i
        ) == '\t' ) ? ( indentWidth - (width % indentWidth) ) : 1;

        return width;
    }

    /**
     * True if {@code [from, to]} contains at least one {@code name(args)} call with a non-empty
     *  argument list -- the shape {@code MiscRuleCurly.enforceCallLineBreaking} may later break
     *  across lines if it doesn't fit (zero-arg calls are never broken). Duplicated from {@code
     *  JavaSpecificRule}/{@code GetterSetterRuleCurly}'s identical helper, same duplication
     *  precedent as {@link #expandedIndentWidth} above.
     */
    private boolean hasBreakableCall(final List<Token> tokens, final int from, final int to)
    {
        for(int i = from; i <= to; ++i) {
            final Token t = tokens.get(i);
            if(t.type != TokenType.IDENTIFIER) continue;
            final int parenIdx = nextSignificantIndexLocal(tokens, i);
            if( parenIdx < 0 || parenIdx > to || !isPunct( tokens.get(parenIdx), "(" ) ) continue;
            final int closeIdx = matchParenForwardLocal(tokens, parenIdx);
            if(closeIdx < 0 || closeIdx > to) continue;
            final int argsFrom = nextSignificantIndexLocal(tokens, parenIdx);
            if(argsFrom >= 0 && argsFrom < closeIdx) return true;
        } // for

        return false;
    }

    /**
     * Index of the next non-whitespace/non-newline token after {@code from}, or {@code -1} if
     *  none -- local copy, see {@link #hasBreakableCall}'s own duplication note
     */
    private int nextSignificantIndexLocal(final List<Token> tokens, final int from)
    {
        for( int i = from + 1; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if(t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) return i;
        }

        return -1;
    }

    /**
     * Index of the `(` at {@code openIdx}'s matching `)`, or {@code -1} if unmatched within
     *  {@code tokens} -- local copy, see {@link #hasBreakableCall}'s own duplication note
     */
    private int matchParenForwardLocal(final List<Token> tokens, final int openIdx)
    {
        int depth = 0;
        for( int i = openIdx; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
                 if( isPunct(t, "(") ) ++depth;
            else if( isPunct(t, ")") ) {
                --depth;
                if(depth == 0) return i;
            }
        } // for

        return -1;
    }

    /**
     * JS/TS-only guard for `tryCollapse`/`collapseBracelessBody`'s single-statement-body
     *  brace/braceless collapse (STATE_JS_TS.md, root cause #3, "2026-07-30 design/scoping pass"):
     *  refuses the collapse when the joined one-line {@code candidate} would exceed {@link
     *  #lineLengthLimit} AND nothing later can rescue it. Reuses the same
     *  {@link #hasBreakableCall}/raw-width-estimate heuristic already used by {@code
     *  JavaSpecificRule.isSingleLineBody}/{@code KotlinSpecificRule}'s analogous method/{@code
     *  GetterSetterRuleCurly.parseOneLinerMember} for the identical underlying problem -- not a
     *  true two-pass simulation of {@code MiscRuleCurly.enforceCallLineBreaking}'s later wrap
     *  decision (an earlier attempt at a naive width-only guard, with no {@code hasBreakableCall}
     *  gate, was reverted: it refused to collapse every braceless if/else with a wrappable-call
     *  body, breaking 5 fixtures -- see the angular cluster-4 root-cause-#3 writeup in
     *  STATE_JS_TS.md for the full story). If the body contains at least one breakable call,
     *  collapsing is still safe even when over-limit: {@code enforceCallLineBreaking} will wrap
     *  that call's arguments across lines afterward (a braceless consequent can legally span
     *  multiple physical lines as long as it's still one statement), and both round1 and round2
     *  predict the same "will wrap" outcome from this same heuristic, so idempotency holds.
     *
     * <p>Deliberate implementation-detail deviation from the original scoping-pass wording ("...
     *  and {@code hasBreakableCall} is false over the candidate's body span"): {@code
     *  hasBreakableCall} here scans the whole candidate span (condition/prefix AND body), not just
     *  the body. Found necessary against the already-passing `real_code_regressions_141` fixture
     *  (a braced `if (longCondition-with-a-breakable-call) { x(); }` where `x()` is a zero-arg
     *  body call with nothing to wrap, but the *condition*'s own `isCssClassMatching(...)` call is
     *  what `enforceCallLineBreaking` wraps to rescue the line) -- restricting the scan to the body
     *  alone wrongly refused a collapse that was already correct, working, tested behavior
     *  (root-cause #2's fix). The same "both round1 and round2 predict the same wrap outcome from
     *  the same heuristic" idempotency argument applies equally to a rescuing wrap inside the
     *  condition as inside the body, so widening the scan doesn't reopen the reverted naive
     *  attempt's failure mode (that attempt had no {@code hasBreakableCall} gate at all).
     * @param indentAnchorIdx token index on the candidate's own leading physical line (the
     *     keyword itself), used to measure the candidate's true rendered column width the same
     *     way {@code enforceCallLineBreaking}'s own fits-check does (indent + collapsed text).
     * @param scanFrom first token index of the span to scan for a rescuing breakable call,
     *     inclusive (covers the condition/prefix as well as the body -- see the deviation note
     *     above).
     * @param scanTo last token index of the span to scan, inclusive -- may end up
     *     {@code < scanFrom} for an empty span, which {@link #hasBreakableCall} handles safely
     *     (no iterations, returns {@code false}).
     */
    private boolean refuseUnrescuableCollapse(
        final List<Token> tokens,
        final int         indentAnchorIdx,
        final int         scanFrom,
        final int         scanTo,
        final String      candidate
    )
    {
        if( !(lang.isJs || lang.isTs) ) return false;
        final int width = expandedIndentWidth(
            lineIndent(tokens, indentAnchorIdx)
        ) + candidate.length();
        if(width <= lineLengthLimit) return false;

        return !hasBreakableCall(tokens, scanFrom, scanTo);
    }

    /**
     * True iff nothing but whitespace and exactly one NEWLINE sits between tokens at indices
     *  {@code fromExclusive} and {@code toExclusive} -- i.e. the two lines are adjacent with no
     *  blank line between them.
     */
    private boolean isSingleNewlineGap(
        final List<Token> tokens,
        final int         fromExclusive,
        final int         toExclusive
    )
    {
        int newlines = 0;
        for(int k = fromExclusive + 1; k < toExclusive; ++k) {
            if( tokens.get(k).type == TokenType.NEWLINE ) newlines++;
        }

        return newlines == 1;
    }

    /**
     * From a named `{` at {@code openIdx}, walks forward past any run of preprocessor directive
     *  lines sitting directly against the brace (no blank line of their own), returning the index
     *  of the first token that represents real body content
     */
    private int skipGuardForward(final List<Token> tokens, final int openIdx)
    {
        int i = openIdx;
        while(true) {
            int j = i + 1;
            while( j < tokens.size() && isGap( tokens.get(j) ) ) j++;
            if( j >= tokens.size() ) return j;
            if( tokens.get(j).type == TokenType.PREPROCESSOR && isSingleNewlineGap(tokens, i, j) ) {
                i = j;
                continue;
            }
            return j;
        } // while
    }

    /**
     * From a named `}` at {@code closeIdx}, walks backward past any run of preprocessor
     *  directive lines sitting directly against the brace (no blank line of their own), returning
     *  the index of the last token that represents real body content
     */
    private int skipGuardBackward(final List<Token> tokens, final int closeIdx)
    {
        int i = closeIdx;
        while(true) {
            int j = i - 1;
            while( j >= 0 && isGap( tokens.get(j) ) ) j--;
            if(j < 0) return j;
            if( tokens.get(j).type == TokenType.PREPROCESSOR && isSingleNewlineGap(tokens, j, i) ) {
                i = j;
                continue;
            }
            return j;
        } // while
    }

    /**
     * True if the `{` at {@code openIdx} is immediately followed (ignoring gap tokens) by its
     *  own matching `}`, i.e. an empty body -- {@code { }} or {@code {}}.
     */
    private boolean isEmptyBraceBody(final List<Token> tokens, final int openIdx)
    {
        for( int k = openIdx + 1; k < tokens.size(); ++k ) {
            final Token t = tokens.get(k);
            if( isGap(t) ) continue;
            return isPunct(t, "}");
        }

        return false;
    }

    /**
     * Guarantees the gap contains a blank line. A comment already sitting on its own line (at
     * least one NEWLINE precedes it in the gap) does not block this -- the blank line is inserted
     * ahead of it, same as it would be ahead of the first real token, and everything from the
     * comment onward is left untouched. A comment with nothing but whitespace before it (a
     * trailing same-line comment glued to the previous token, e.g. `stuff; // note`) is left
     * attached to that line -- relocating *it* ahead of a synthesized blank line would be
     * ambiguous, consistent with `enforceKAndRBraceStyle`/`placeElseOnOwnLine` above -- but the
     * blank line is still guaranteed in the remainder of the gap, right after the comment's own
     * line, rather than being dropped entirely.
     */
    /**
     * @param indentIfNoNewline indentation to use for the resumed content when the gap has no
     *     pre-existing NEWLINE at all (i.e. the content was never the first token of its own
     *     physical line -- a same-line nested body being split apart here for the first time).
     *     May be {@code null} if no such indentation was computed (kept permissive rather than
     *     throwing, since a stray same-line separator is still better than a crash).
     */
    private String ensureBlankLine(final List<Token> gap, final String indentIfNoNewline)
    {
        int     firstCommentIdx           = -1;
        boolean newlineBeforeFirstComment = false;
        for( int i = 0; i < gap.size(); ++i ) {
            final Token g = gap.get(i);
            if( isComment(g) ) {
                firstCommentIdx = i;
                break;
            }
            if(g.type == TokenType.NEWLINE) newlineBeforeFirstComment = true;
        } // for
        if(firstCommentIdx >= 0 && !newlineBeforeFirstComment) {
            // Comment sits on the same physical line as whatever precedes the gap (only
            // whitespace, no NEWLINE, in between) -- a trailing same-line comment glued to the
            // previous token, not a leading comment of the next member. Keep it glued to that
            // line, but still guarantee a blank line in what remains of the gap after it.
            final StringBuilder sb = new StringBuilder();
            for(int i = 0; i <= firstCommentIdx; ++i) sb.append( gap.get(i).text );
            sb.append(
                ensureBlankLine( gap.subList( firstCommentIdx + 1, gap.size() ), indentIfNoNewline )
            );
            return sb.toString();
        } // if

        final int prefixEnd    = firstCommentIdx < 0 ? gap.size() : firstCommentIdx;
              int newlineCount = 0;
        for(int i = 0; i < prefixEnd; ++i) {
            if( gap.get(i).type == TokenType.NEWLINE ) newlineCount++;
        }

        final StringBuilder sb = new StringBuilder();
        if(newlineCount == 0) {
            // No existing NEWLINE in the gap to anchor indentation on -- discard the stale
            // inline separator (a bare same-line space, e.g. `{ enum Bar {`'s single space)
            // rather than reusing it as the new line's indentation, and synthesize a properly
            // indented line instead.
            sb.append("\n\n");
            if(indentIfNoNewline != null) sb.append(indentIfNoNewline);
            for(int i = 0; i < prefixEnd; ++i) {
                final Token g = gap.get(i);
                if(g.type != TokenType.WHITESPACE) sb.append(g.text);
            }
        } // if
        else {
            boolean insertedExtra = newlineCount != 1;
            for(int i = 0; i < prefixEnd; ++i) {
                final Token g = gap.get(i);
                sb.append(g.text);
                if(!insertedExtra && g.type == TokenType.NEWLINE) {
                    sb.append('\n');
                    insertedExtra = true;
                }
            } // for
        }
        for( int i = prefixEnd; i < gap.size(); ++i ) sb.append( gap.get(i).text );

        return sb.toString();
    }

    private boolean isGap(final Token t)
    {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    // ── Closing comments on blocks (STYLE.md §7) ────────────────────────────────
    /** What kind of construct a `{` opens, for closing-comment purposes */
    private enum Kind {

        NAMED, FOR, WHILE, IF, SWITCH, FUNCTION, EXCLUDED, OTHER

    } // enum Kind

    /** A currently-open brace's classification, tracked on a stack while scanning forward */
    private static final class Frame {

        final int     openIdx;
        final Kind    kind;
        final String  label;          // Fixed text, excluding any nested-disambiguation variable
        final int     openParen;      // -1 unless kind is FOR/WHILE/SWITCH
        final int     closeParen;     // -1 unless kind is FOR/WHILE/SWITCH
              boolean sameKindNested; // Set true if an ancestor (or descendant) of the same kind exists

        private Frame(
            final int    openIdx,
            final Kind   kind,
            final String label,
            final int    openParen,
            final int    closeParen
        )
        {
            this.openIdx    = openIdx;
            this.kind       = kind;
            this.label      = label;
            this.openParen  = openParen;
            this.closeParen = closeParen;
        }

        static Frame named(final int openIdx, final String label)
        {
            return new Frame(openIdx, Kind.NAMED, label, -1, -1);
        }

        static Frame control(
            final int    openIdx,
            final Kind   kind,
            final String label,
            final int    openParen,
            final int    closeParen
        )
        {
            return new Frame(openIdx, kind, label, openParen, closeParen);
        }

        static Frame excluded(final int openIdx)
        {
            return new Frame(openIdx, Kind.EXCLUDED, null, -1, -1);
        }

        static Frame other(final int openIdx)
        {
            return new Frame(openIdx, Kind.OTHER, null, -1, -1);
        }

    } // class Frame

    /**
     * Scans a token slice and appends a `// label` comment after the `}` of every block that
     * qualifies per STYLE.md §7: named constructs always (regardless of length), and
     * `for`/`while`/`if`/`switch` bodies only when their content exceeds
     * {@code closingCommentMinLines}. When two control-flow blocks of the same kind are nested
     * simultaneously, both get a disambiguating variable appended (`for i`, `while running`,
     * `switch opcode`) when one can be extracted -- `if` never gets a variable (no STYLE.md
     * example shows one, and `if` conditions are typically compound). `case` labels, naked
     * `{ ... }` blocks, `else`/`else if`, and unnamed namespaces never get a comment, since none
     * of those braces are ever classified as NAMED/FOR/WHILE/IF/SWITCH by {@link #classifyBrace}.
     * A trailing `;` right after `}` (C/C++ `struct`/`class`/`enum`/`union` definitions) is
     * skipped over so the comment lands after it, not before -- landing before it would put the
     * `;` on a comment line and silently break the statement. If anything other than whitespace
     * precedes the next newline at the chosen insertion point (more code on the same line, or an
     * existing trailing comment), the comment is skipped entirely rather than risking corruption
     * or duplication.
     */
    public String addClosingComments(final List<Token> tokens)
    {
        final int                  n             = tokens.size();
        final Deque<Frame>         stack         = new ArrayDeque<>();
        final Map<Integer, String> comments      = new HashMap<>();
        final Map<Integer, String> replaceTokens = new HashMap<>();    // idx → replacement text

        for(int i = 0; i < n; ++i) {
            final Token t = tokens.get(i);
            if( isPunct(t, "{") ) {
                final Frame f = classifyBrace(tokens, i);
                if(f.kind == Kind.FOR || f.kind == Kind.WHILE || f.kind == Kind.SWITCH) {
                    boolean foundAncestor = false;
                    for(final Frame anc : stack) {
                        if(anc.kind == f.kind) {
                            anc.sameKindNested = true;
                            foundAncestor      = true;
                        }
                    }
                    if(foundAncestor) f.sameKindNested = true;
                } // if
                stack.push(f);
            } // if
            else if( isPunct(t, "}") ) {
                if( stack.isEmpty() ) continue;
                final Frame f                  = stack.pop();
                final int   insertAt           = commentInsertionIndex(tokens, i);
                final int   existingCommentIdx = findExistingLineComment(tokens, insertAt, n);
                // Only the tokens this method might actually rewrite need to be frozen-checked --
                // the closing brace itself through the insertion point (plus any existing trailing
                // comment there). RDD_KEY_129: this used to scan the *entire* block span
                // `[f.openIdx, i]` for any frozen token, which meant one unrelated
                // JXM_CFMT_DIS/ENA region nested anywhere inside an outer named construct (e.g. a
                // frozen method body inside a class) silently suppressed that outer construct's
                // own closing comment too, even though nothing about the outer `}` itself was
                // ever going to be touched.
                final int frozenCheckEnd = Math.max(insertAt, existingCommentIdx) + 1;
                if( anyFrozen(tokens, i, frozenCheckEnd) ) continue;
                final String comment = decideComment(tokens, f, i);
                if(comment != null) {
                    if( safeToCommentAfter(tokens, insertAt) ) {
                        comments.put(insertAt, comment);
                    }
                    else if(existingCommentIdx >= 0) {
                        replaceTokens.put(existingCommentIdx, "// " + comment);
                        normalizeWhitespaceBefore(
                            tokens, insertAt + 1, existingCommentIdx, replaceTokens
                        );
                    }
                } // if
                else if( existingCommentIdx >= 0
                        && isLikelyClosingComment( tokens.get(existingCommentIdx).text ) ) {
                    replaceTokens.put(existingCommentIdx, "");
                    clearWhitespaceBefore(tokens, insertAt + 1, existingCommentIdx, replaceTokens);
                }
            }
        } // for i

        final StringBuilder out = new StringBuilder();
        for(int i = 0; i < n; ++i) {
            final String replacement = replaceTokens.get(i);
            if(replacement != null) out.append(replacement);
            else                    out.append( tokens.get(i).text );
            final String c = comments.get(i);
            if(c != null) out.append(" // ").append(c);
        } // for

        return out.toString();
    }

    /**
     * Finds the index of a COMMENT_LINE token directly after {@code afterIdx} (skipping only
     *  WHITESPACE), or -1 if the first non-whitespace token is not a COMMENT_LINE
     */
    private int findExistingLineComment(final List<Token> tokens, final int afterIdx, final int n)
    {
        int k = afterIdx + 1;
        while( k < n && tokens.get(k).type == TokenType.WHITESPACE ) k++;

        return ( k < n && tokens.get(k).type == TokenType.COMMENT_LINE ) ? k : -1;
    }

    /**
     * True if a comment's text looks like a stale/wrong closing-comment artifact left over
     *  from a previous format pass: starts with {@code "// end "} (the closing-comment
     *  convention used when a block that used to warrant one no longer does) followed by only
     *  word characters (letters, digits, underscore) and spaces, with no punctuation or
     *  symbols. An ordinary short comment that happens to be a single alphanumeric word or
     *  phrase (e.g. `// getter`, `// validator`) does not start with {@code "end "} and so is
     *  never mistaken for a stale closing-comment artifact.
     */
    private boolean isLikelyClosingComment(final String text)
    {
        if( !text.startsWith("// end ") ) return false;
        final String body = text.substring(7);
        if( body.isEmpty() ) return false;
        for( int i = 0; i < body.length(); ++i ) {
            final char c = body.charAt(i);
            if( c != ' ' && !Character.isLetterOrDigit(c) && c != '_' ) return false;
        }

        return true;
    }

    /** Collapses all WHITESPACE tokens in [{@code from}, {@code before}) to a single space */
    private void normalizeWhitespaceBefore(
        final List<Token>          tokens,
        final int                  from,
        final int                  before,
        final Map<Integer, String> replaceTokens
    )
    {
        boolean first = true;
        for(int k = from; k < before; ++k) {
            if( tokens.get(k).type == TokenType.WHITESPACE ) {
                replaceTokens.put(k, first ? " " : "");
                first = false;
            }
        }
    }

    /** Removes all WHITESPACE tokens in [{@code from}, {@code before}) */
    private void clearWhitespaceBefore(
        final List<Token>          tokens,
        final int                  from,
        final int                  before,
        final Map<Integer, String> replaceTokens
    )
    {
        for(int k = from; k < before; ++k) {
            if( tokens.get(k).type == TokenType.WHITESPACE ) replaceTokens.put(k, "");
        }
    }

    /** Classifies the `{` at braceIdx for closing-comment purposes; see {@link #addClosingComments} */
    private Frame classifyBrace(final List<Token> tokens, final int braceIdx)
    {
        final Token brace = tokens.get(braceIdx);
        if(brace.name != null) return classifyNamed(tokens, braceIdx, brace.name);

        final int prevIdx = prevSignificantIndex(tokens, braceIdx - 1);
        if(prevIdx < 0) return Frame.other(braceIdx);
        if( isAnonymousClassBrace(tokens, prevIdx) ) return Frame.named(braceIdx, "class");
        if(lang.isKotlin) {
            final String kotlinHeadlessLabel = classifyKotlinHeadlessNamed(tokens, prevIdx);
            if(kotlinHeadlessLabel != null) return Frame.named(braceIdx, kotlinHeadlessLabel);
        }

        final Token prev = tokens.get(prevIdx);
        if( lang.isTs && isOp(prev, "=") ) {
            // RDD_KEY_196: an object-shaped `type X = { ... };` alias is a named construct in the
            // same sense as `interface`/`class`/`enum` per STYLE.md §7's universal rule -- always
            // gets a closing comment regardless of body length. Walk backward (depth-aware, past
            // any generic-parameter clause on the alias name) from the `=` to confirm the enclosing
            // statement actually starts with the `type` keyword, so this doesn't misfire on an
            // ordinary `const obj = { ... };` object-literal initializer.
            final String typeAliasName = typeAliasNameBeforeEquals(tokens, prevIdx);
            if(typeAliasName != null) return Frame.named(braceIdx, "type " + typeAliasName);
        } // if
        if( isPunct(prev, ")") ) {
            final int openParen = matchOpenBackward(tokens, prevIdx);
            final int kwIdx     = openParen >= 0 ? prevSignificantIndex(tokens, openParen - 1) : -1;
            if( kwIdx >= 0 && tokens.get(kwIdx).type == TokenType.KEYWORD ) {
                final String kw = tokens.get(kwIdx).text;
                if( "if".equals(kw) ) {
                    final int beforeIf = prevSignificantIndex(tokens, kwIdx - 1);
                    if( beforeIf >= 0 && tokens.get(
                        beforeIf
                    ).type == TokenType.KEYWORD && "else".equals(
                        tokens.get(beforeIf).text
                    ) ) return Frame.excluded(
                        braceIdx
                    );
                    return Frame.control(braceIdx, Kind.IF, "if", -1, -1);
                } // if
                if( "for".equals(
                    kw
                ) ) return Frame.control(
                    braceIdx, Kind.FOR, "for", openParen, prevIdx
                );
                if( "while".equals(
                    kw
                ) ) return Frame.control(
                    braceIdx, Kind.WHILE, "while", openParen, prevIdx
                );
                if( "switch".equals(
                    kw
                ) ) return Frame.control(
                    braceIdx, Kind.SWITCH, "switch", openParen, prevIdx
                );
            } // if
            else if( (lang.isJs || lang.isTs) && kwIdx >= 0
                    && tokens.get(kwIdx).type == TokenType.IDENTIFIER ) {
                // STYLE_JS_TS.md §1's baseline-inherited closing-comment section explicitly
                // extends STYLE.md §7 to named function declarations and class methods (its own
                // worked example: `} // function foo`) -- this codebase's shared `classifyBrace`
                // otherwise only recognizes if/for/while/switch control-flow bodies and
                // tokenizer-tagged named constructs (class/enum/etc.) here, silently leaving
                // every function/method body brace as `Frame.other` (no closing comment ever,
                // regardless of length). The identifier directly before the parameter list's `(`
                // is always the bare method/function name with no modifier prefix (`async`/
                // `static`/a generator's `*` are all separate, non-adjacent tokens further back),
                // so no extra modifier-stripping is needed here. Threshold-gated exactly like
                // FOR/WHILE/SWITCH above (not an unconditional NAMED-style comment) -- an ordinary
                // function isn't a "named construct" per STYLE.md §7's own distinct always-labeled
                // list (class/struct/enum/namespace/interface).
                return Frame.control( braceIdx, Kind.FUNCTION, tokens.get(kwIdx).text, -1, -1 );
            }
        } // if
        else if( prev.type == TokenType.KEYWORD && "else".equals(prev.text) ) {
            return Frame.excluded(braceIdx);
        }

        return Frame.other(braceIdx);
    }

    /** Builds the "kind name" label (`class Foo`, `enum class State`, `extern "C"`, ...). */
    private Frame classifyNamed(final List<Token> tokens, final int braceIdx, final String name)
    {
        if( name.indexOf(
            ' '
        ) >= 0 ) return Frame.named(
            braceIdx, name
        ); // Already a complete label, e.g. `extern "C"`

        if( isConceptRequiresExpressionBody(tokens, braceIdx, name) ) {
            // `concept Name = requires(...) {` -- the requires-expression's own parameter list
            // sits between the name and the body brace, same gap problem as `record`'s component
            // list below. Checked first: `findRecordComponentListClose` below matches on the
            // immediate `)` predecessor unconditionally (java-only in practice, since only `record`
            // ever arms `pendingRecordName`, but it would otherwise misclassify this same shape).
            return Frame.named(braceIdx, "concept " + name);
        } // if

        if(lang.isJava) {
            final int recordCloseParen = findRecordComponentListClose(tokens, braceIdx);
            if(recordCloseParen >= 0) {
                // `record Name(...) [implements TypeList] {` -- the component list (and an optional
                // implements clause) sits between the name and the body brace, so the name isn't the
                // token directly before `{` like it is for class/interface/enum.
                final int openParen   = matchOpenBackward(tokens, recordCloseParen);
                final int nameIdx     = openParen >= 0 ? prevSignificantIndex(
                    tokens, openParen - 1
                ) : -1;
                final int recordKwIdx = nameIdx >= 0 ? prevSignificantIndex(
                    tokens, nameIdx - 1
                ) : -1;
                if( recordKwIdx >= 0 && tokens.get(
                    recordKwIdx
                ).type == TokenType.KEYWORD && "record".equals(
                    tokens.get(recordKwIdx).text
                ) ) return Frame.named(
                    braceIdx, "record " + name
                );
                return Frame.named(braceIdx, name);
            } // if
        } // if
        // Qualified namespace name (`namespace alpha::beta::gamma {`): findConstructNameIndex
        // matches single-token identifiers only, so look up the first segment instead and render
        // the closing-comment label with the STYLE.md-preferred space separator.
        if( name.indexOf(':') >= 0 ) {
            final String firstSegment = name.substring( 0, name.indexOf(':') );
            final int    qualNameIdx  = findConstructNameIndex(tokens, braceIdx, firstSegment);
            if(qualNameIdx >= 0) {
                final int qualKwIdx = findConstructKeywordIndex(tokens, qualNameIdx - 1);
                if( qualKwIdx >= 0 && tokens.get(
                    qualKwIdx
                ).type == TokenType.KEYWORD && "namespace".equals(
                    tokens.get(qualKwIdx).text
                ) ) return Frame.named(
                    braceIdx, "namespace " + name.replace("::", " ")
                );
            } // if
        } // if
        // Search backward past inheritance/base-type clauses (and attribute-specifiers like
        // `alignas(16)`) to find the construct keyword
        final int    nameIdx = findConstructNameIndex(tokens, braceIdx, name);
              String label   = name;
        if(nameIdx >= 0) {
            final int kwIdx = findConstructKeywordIndex(tokens, nameIdx - 1);
            if( kwIdx >= 0 && tokens.get(kwIdx).type == TokenType.KEYWORD ) {
                final String kw       = tokens.get(kwIdx).text;
                final int    beforeKw = prevSignificantIndex(tokens, kwIdx - 1);
                if( "class".equals(
                    kw
                ) && beforeKw >= 0 && tokens.get(
                    beforeKw
                ).type == TokenType.KEYWORD && "enum".equals(
                    tokens.get(beforeKw).text
                ) ) label = "enum class " + name;
                else label = kw + " " + name;
            } // if
        } // if

        return Frame.named(braceIdx, label);
    }

    /**
     * Finds the `)` closing a record's component list, scanning backward from {@code braceIdx}
     * across an optional trailing `implements TypeList` clause (the only thing Java permits
     * between a record's component list and its body brace). Returns -1 if the immediate
     * predecessor chain doesn't match this shape (not a record, or an unrecognized shape this
     * bounded-effort scan doesn't cover -- e.g. anything other than `implements` at the top
     * level). Angle-bracket depth is tracked so generic bounds inside the implements clause
     * (`implements Comparable<? super Point>`) don't false-positive on `super`/`extends` as an
     * unexpected keyword.
     */
    private int findRecordComponentListClose(final List<Token> tokens, final int braceIdx)
    {
        int angleDepth = 0;
        int i          = prevSignificantIndex(tokens, braceIdx - 1);
        while(i >= 0) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                ++angleDepth;
            }
            else if(t.type == TokenType.ANGLE_BRACKET_OPEN) {
                --angleDepth;
            }
            else if(angleDepth == 0) {
                if( isPunct(t, ")") ) return i;
                if( t.type == TokenType.KEYWORD && !"implements".equals(t.text) ) return -1;
            }
            i = prevSignificantIndex(tokens, i - 1);
        } // while

        return -1;
    }

    /**
     * True iff {@code braceIdx}'s `{` is the body of a `concept Name = requires(...) { ... }`
     * definition -- i.e. the exact predecessor chain `) requires ( ... ) <- = <- name <-
     * concept`, scanning backward via {@link #matchOpenBackward}'s same local depth counting used
     * by the record-component-list check above. {@code name} is the already-detected construct
     * name (from the `{` token's own {@code name} field), confirmed here to match the identifier
     * immediately before the `=` -- guards against a coincidentally-shaped unrelated expression.
     */
    private boolean isConceptRequiresExpressionBody(
        final List<Token> tokens,
        final int         braceIdx,
        final String      name
    )
    {
        final int closeParenIdx = prevSignificantIndex(tokens, braceIdx - 1);
        if( closeParenIdx < 0 || !isPunct( tokens.get(closeParenIdx), ")" ) ) return false;
        final int openParenIdx = matchOpenBackward(tokens, closeParenIdx);
        if(openParenIdx < 0) return false;
        final int requiresIdx = prevSignificantIndex(tokens, openParenIdx - 1);
        if( requiresIdx < 0 || tokens.get(
            requiresIdx
        ).type != TokenType.KEYWORD || !"requires".equals(
            tokens.get(requiresIdx).text
        ) ) return false;
        final int eqIdx = prevSignificantIndex(tokens, requiresIdx - 1);
        if( eqIdx < 0 || !isOp( tokens.get(eqIdx), "=" ) ) return false;
        final int nameIdx = prevSignificantIndex(tokens, eqIdx - 1);
        if( nameIdx < 0 || tokens.get(
            nameIdx
        ).type != TokenType.IDENTIFIER || !name.equals(
            tokens.get(nameIdx).text
        ) ) return false;
        final int conceptKwIdx = prevSignificantIndex(tokens, nameIdx - 1);

        return conceptKwIdx >= 0 && tokens.get(conceptKwIdx).type == TokenType.KEYWORD
                && "concept".equals( tokens.get(conceptKwIdx).text );
    }

    /**
     * True if the `{` whose immediately preceding significant token is at prevIdx opens a Java
     * anonymous class body: `new Identifier(args) {` or `new Identifier<T>(args) {`. Qualified
     * names (`new pkg.Identifier() {`) are not recognized -- out of scope, same bounded-effort
     * spirit as `isCppTrailingReturnLambda`'s scan cap above.
     */
    private boolean isAnonymousClassBrace(final List<Token> tokens, final int prevIdx)
    {
        if( !lang.isJava || !isPunct( tokens.get(prevIdx), ")" ) ) return false;
        final int openParen = matchOpenBackward(tokens, prevIdx);
        if(openParen < 0) return false;
        int beforeOpen = prevSignificantIndex(tokens, openParen - 1);
        if( beforeOpen >= 0 && tokens.get(beforeOpen).type == TokenType.ANGLE_BRACKET_CLOSE ) {
            beforeOpen = matchAngleOpenBackward(tokens, beforeOpen);
            beforeOpen = beforeOpen >= 0 ? prevSignificantIndex(tokens, beforeOpen - 1) : -1;
        }
        if( beforeOpen < 0 || tokens.get(beforeOpen).type != TokenType.IDENTIFIER ) return false;
        final int newIdx = prevSignificantIndex(tokens, beforeOpen - 1);

        return newIdx >= 0 && tokens.get(newIdx).type == TokenType.KEYWORD
                && "new".equals( tokens.get(newIdx).text );
    }

    /**
     * Kotlin-only: recognizes a headless (nameless) `init { }`, `companion object { }`, or
     * anonymous `object [: SuperType(...), Iface<T>] { }` body brace -- none of these ever arm
     * the tokenizer's {@code pendingNamedConstructName} (it requires a following IDENTIFIER,
     * which these shapes never have), so {@code brace.name} is always null for them and they'd
     * otherwise fall through to {@link Frame#other}, losing STYLE_KOTLIN.md SS3.1/SS3.4's mandatory
     * blank-lines/closing-comment treatment. Named `object Foo { }`/`companion object Foo { }`
     * DO get a real name from the tokenizer and are handled generically by {@link #classifyNamed}
     * already -- this method is only reached when {@code brace.name == null}.
     *
     * <p>Walks backward from the brace across an optional `: SuperType(...), Iface<T>, ...`
     * clause looking for a bare `object` keyword (bounded-effort scan, same spirit as
     * {@link #isAnonymousClassBrace} above): anything encountered that isn't part of such a
     * clause (an operator, a `;`/`{`/`}` boundary, ...) aborts the scan and returns null.
     */
    private String classifyKotlinHeadlessNamed(final List<Token> tokens, final int prevIdx)
    {
        final Token prev = tokens.get(prevIdx);
        if( prev.type == TokenType.KEYWORD && "init".equals(prev.text) ) return "init";
        int i = prevIdx;
        while(i >= 0) {
            final Token t = tokens.get(i);
            if( t.type == TokenType.KEYWORD && "object".equals(t.text) ) {
                final int beforeObject = prevSignificantIndex(tokens, i - 1);
                if( beforeObject >= 0 && tokens.get(
                    beforeObject
                ).type == TokenType.KEYWORD && "companion".equals(
                    tokens.get(beforeObject).text
                ) ) return "companion object";
                return "object";
            } // if
            if(t.type == TokenType.IDENTIFIER || t.type == TokenType.KEYWORD) {
                i = prevSignificantIndex(tokens, i - 1);
                continue;
            }
            if( isPunct(t, ")") ) {
                final int open = matchOpenBackward(tokens, i);
                if(open < 0) return null;
                i = prevSignificantIndex(tokens, open - 1);
                continue;
            }
            if(t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                final int open = matchAngleOpenBackward(tokens, i);
                if(open < 0) return null;
                i = prevSignificantIndex(tokens, open - 1);
                continue;
            }
            if( isPunct(t, ",") || isOp(t, ":") || isPunct(t, ".") ) {
                i = prevSignificantIndex(tokens, i - 1);
                continue;
            }
            return null;
        } // while

        return null;
    }

    /** Index of the `<` matching the `>` at closeIdx, via local backward depth counting, or -1 */
    private int matchAngleOpenBackward(final List<Token> tokens, final int closeIdx)
    {
        int depth = 1;
        int i     = closeIdx - 1;
        while(i >= 0 && depth > 0) {
            final TokenType ty = tokens.get(i).type;
                 if(ty == TokenType.ANGLE_BRACKET_CLOSE) depth++;
            else if(ty == TokenType.ANGLE_BRACKET_OPEN)  depth--;
            --i;
        }

        return depth == 0 ? i + 1 : -1;
    }

    /** Decides the closing-comment text for frame f closing at closeIdx, or null for no comment */
    private String decideComment(final List<Token> tokens, final Frame f, final int closeIdx)
    {
        switch(f.kind) {

            case NAMED:
                return isEmptyBraceBody(tokens, f.openIdx) ? null : f.label;

            case FOR: /* FALL-THROUGH */
            case WHILE: /* FALL-THROUGH */
            case SWITCH: /* FALL-THROUGH */
            case FUNCTION:
                if( countContentLines(
                    tokens, f.openIdx, closeIdx
                ) <= closingCommentMinLines ) return null;
                final String var = f.sameKindNested ? extractVariable(tokens, f) : null;
                return var != null ? f.label + " " + var : f.label;

            case IF:
                return countContentLines(tokens, f.openIdx, closeIdx) > closingCommentMinLines
                        ? f.label : null;

            default:
                return null;

        } // switch
    }

    /**
     * If the `=` at {@code eqIdx} is immediately preceded (walking back past an optional generic
     *  parameter clause) by `type IDENTIFIER`, returns that identifier; otherwise null. Depth-aware
     *  the same way {@link #classifyBrace}'s sibling helpers are, so it doesn't misfire crossing a
     *  `;`/`{`/`}` statement boundary.
     */
    private String typeAliasNameBeforeEquals(final List<Token> tokens, final int eqIdx)
    {
        int i = prevSignificantIndex(tokens, eqIdx - 1);
        if(i < 0) return null;
        if( tokens.get(i).type == TokenType.ANGLE_BRACKET_CLOSE ) {
            int depth = 1;
            --i;
            while(i >= 0 && depth > 0) {
                final Token t = tokens.get(i);
                if( isGap(t) ) {
                    --i;
                    continue;
                }
                     if(t.type == TokenType.ANGLE_BRACKET_CLOSE) depth++;
                else if(t.type == TokenType.ANGLE_BRACKET_OPEN)  depth--;
                --i;
            } // while
            i = prevSignificantIndex(tokens, i);
        } // if
        if( i < 0 || tokens.get(i).type != TokenType.IDENTIFIER ) return null;
        final String name  = tokens.get(i).text;
        final int    kwIdx = prevSignificantIndex(tokens, i - 1);
        if( kwIdx >= 0 && tokens.get(
            kwIdx
        ).type == TokenType.KEYWORD && "type".equals(
            tokens.get(kwIdx).text
        ) ) return name;

        return null;
    }

    private int countContentLines(final List<Token> tokens, final int openIdx, final int closeIdx)
    {
        int count = 0;
        for(int k = openIdx + 1; k < closeIdx; ++k) {
            if( tokens.get(k).type == TokenType.NEWLINE ) count++;
        }

        return count;
    }

    /**
     * Index to insert the comment at: the `}`, or a trailing `;` right after it, if present --
     *  also skipping past a single typedef-alias identifier between them (C's
     *  `typedef enum/struct NAME { ... } ALIAS;`), so the alias/`;` themselves aren't split from
     *  the body they close.
     */
    private int commentInsertionIndex(final List<Token> tokens, final int closeIdx)
    {
          int k = closeIdx + 1;
    final int n = tokens.size();
        while( k < n && tokens.get(k).type == TokenType.WHITESPACE ) k++;
        if( k < n && tokens.get(k).type == TokenType.IDENTIFIER ) {
            int j = k + 1;
            while( j < n && tokens.get(j).type == TokenType.WHITESPACE ) j++;
            if( j < n && isPunct( tokens.get(j), ";" ) ) return j;
        }

        return k < n && isPunct( tokens.get(k), ";" ) ? k : closeIdx;
    }

    /** True if nothing but whitespace separates idx from the next newline (or end of input) */
    private boolean safeToCommentAfter(final List<Token> tokens, final int idx)
    {
          int k = idx + 1;
    final int n = tokens.size();
        while( k < n && tokens.get(k).type == TokenType.WHITESPACE ) k++;

        return k >= n || tokens.get(k).type == TokenType.NEWLINE;
    }

    private String extractVariable(final List<Token> tokens, final Frame f)
    {
        if(f.kind == Kind.FOR) return extractForVariable(tokens, f.openParen, f.closeParen);

        return extractSingleIdentifier( tokens.subList(f.openParen + 1, f.closeParen) );
    }

    /**
     * The loop variable's name: the first identifier in the init clause (declared or not), or
     * if the init clause is empty, the first identifier in the increment clause, or for a
     * range-based/for-each `for(... name : ...)`, the identifier immediately before the `:`.
     * Null if none of those shapes match (e.g. a variable-less `for(;;)`).
     */
    private String extractForVariable(
        final List<Token> tokens,
        final int         openParen,
        final int         closeParen
    )
    {
        final List<Token>   body     = tokens.subList(openParen + 1, closeParen);
              int           depth    = 0;
              int           colonIdx = -1;
        final List<Integer> semiIdx  = new ArrayList<>();
        for( int k = 0; k < body.size(); ++k ) {
            final Token t = body.get(k);
                 if( isPunct(t, "(") || isPunct(t, "[") ) depth++;
            else if( isPunct(t, ")") || isPunct(t, "]") ) depth--;
            else if( depth == 0 && isPunct(t, ";") ) semiIdx.add(k);
            else if( depth == 0 && colonIdx < 0 && isOp(t, ":") ) colonIdx = k;
        } // for

        if( semiIdx.isEmpty() && colonIdx >= 0 ) {
            int k = colonIdx - 1;
            while( k >= 0 && isGap( body.get(k) ) ) k--;
            return k >= 0 && body.get(k).type == TokenType.IDENTIFIER ? body.get(k).text : null;
        }
        if( !semiIdx.isEmpty() ) {
            final String initName = firstIdentifier( body.subList( 0, semiIdx.get(0) ) );
            if(initName != null) return initName;
            if( semiIdx.size() >= 2 ) return firstIdentifier(
                body.subList( semiIdx.get(1) + 1, body.size() )
            );
        } // if

        return null;
    }

    private String firstIdentifier(final List<Token> seg)
    {
        for(final Token t : seg) {
            if(t.type == TokenType.IDENTIFIER) return t.text;
        }

        return null;
    }

    /**
     * The `while`/`switch` controlling expression's variable, only when it reduces to one bare
     * identifier (optionally negated, e.g. `!done`) -- anything more compound has no single
     * representative variable, so this returns null and the caller falls back to a bare label.
     */
    // ── Named-construct header spacing (STYLE.md §11) ───────────────────────────
    /**
     * Scans a token slice and collapses every run of spaces/tabs within a named-construct
     * header (the range from the opening keyword to the `{`, e.g. `class   Foo   :   public   Bar`)
     * to a single space.  Only {@code WHITESPACE} tokens (spaces/tabs) are collapsed; NEWLINE
     * tokens are left untouched, so a header that was already broken across lines by earlier
     * passes is not corrupted.  The header range is identified by scanning backward from each
     * `{` whose {@code name} field is non-null -- the tokenizer guarantees that field is set
     * exactly for the opening brace of every named construct.
     */
    public String enforceNamedConstructHeaderSpacing(final List<Token> tokens)
    {
        final int       n        = tokens.size();
        final boolean[] collapse = new boolean[n];

        for(int i = 0; i < n; ++i) {
            final Token t = tokens.get(i);
            if( !isPunct(t, "{") || t.name == null ) continue;
            int headerStart = -1;
            for(int j = i - 1; j >= 0; --j) {
                final Token     prev = tokens.get(j);
                final TokenType ty   = prev.type;
                if(ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) continue;
                if( ty == TokenType.KEYWORD && isNamedConstructStartKeyword(prev.text) ) {
                    headerStart = j;
                    if( "class".equals(prev.text) ) {
                        final int before = prevSignificantIndex(tokens, j - 1);
                        if( before >= 0 && tokens.get(
                            before
                        ).type == TokenType.KEYWORD && "enum".equals(
                            tokens.get(before).text
                        ) ) headerStart = before;
                    } // if
                    // Extend backward past any preceding modifier keywords (public, abstract, etc.)
                    int ext = prevSignificantIndex(tokens, headerStart - 1);
                    while( ext >= 0 && tokens.get(ext).type == TokenType.KEYWORD
                            && !isNamedConstructStartKeyword( tokens.get(ext).text ) ) {
                        headerStart = ext;
                        ext         = prevSignificantIndex(tokens, ext - 1);
                    }
                    break;
                } // if
                if( ty == TokenType.PUNCT && ( ";".equals(
                    prev.text
                ) || "{".equals(
                    prev.text
                ) || "}".equals(
                    prev.text
                ) ) ) break;
            } // for j
            if( headerStart >= 0 && !anyFrozen(tokens, headerStart, i + 1) ) {
                for(int j = headerStart; j < i; ++j) collapse[j] = true;
            }
        } // for i

        final StringBuilder out = new StringBuilder();
        for(int i = 0; i < n; ++i) {
            final Token t = tokens.get(i);
            // A header spanning multiple physical lines (e.g. a Kotlin primary constructor's
            // parameter list broken one-per-line before the class body `{`) has its own
            // per-line leading indentation as WHITESPACE tokens too -- collapsing those down to
            // a single space along with genuine mid-line multi-space runs would destroy that
            // indentation entirely (found via a Kotlin `class Widget(\n val name: String,\n ...)
            // {` fixture, where every param line lost its real indent down to one column). Only
            // collapse a WHITESPACE run that sits on the same line as what precedes it -- i.e.
            // not immediately preceded by a NEWLINE (or the very start of the token stream).
            final boolean isLineIndent = i == 0 || tokens.get(i - 1).type == TokenType.NEWLINE;
            if( collapse[i] && t.type == TokenType.WHITESPACE && !isLineIndent ) out.append(' ');
            else                                                                 out.append(t.text);
        } // for

        return out.toString();
    }

    private boolean isNamedConstructStartKeyword(final String text)
    {
        switch(text) {
            case "class"     : /* FALL-THROUGH */ case "struct"    : /* FALL-THROUGH */ case "enum"      : /* FALL-THROUGH */ case "namespace" : /* FALL-THROUGH */
            case "concept"   : /* FALL-THROUGH */ case "interface" : /* FALL-THROUGH */ case "record"    : return true ;
            default          : return false;
        }
    }

    /**
     * Scans backward from {@code fromIdx} for the nearest KEYWORD that satisfies
     * {@link BlockStructureRule#isNamedConstructStartKeyword}, skipping over non-keyword tokens
     * (e.g. attribute-specifiers like {@code alignas(16)} that can appear between the keyword
     * and the construct name).  Stops at {@code ;}, {@code {}, or {@code }}.
     */
    private int findConstructKeywordIndex(final List<Token> tokens, final int fromIdx)
    {
        for(int i = fromIdx; i >= 0; --i) {
            final Token     t  = tokens.get(i);
            final TokenType ty = t.type;
            if(ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) continue;
            if( ty == TokenType.KEYWORD && isNamedConstructStartKeyword(t.text) ) return i;
            if( ty == TokenType.PUNCT && ( ";".equals(
                t.text
            ) || "{".equals(
                t.text
            ) || "}".equals(
                t.text
            ) ) ) break;
        } // for

        return -1;
    }

    /**
     * Searches backward from {@code braceIdx} for the first {@code IDENTIFIER} token whose
     * text equals {@code name}, stopping at any {@code ;}, {@code {}, or {@code }} boundary.
     * Returns the index of that token, or -1 if not found.  Used by {@link #classifyNamed} to
     * locate the construct name across an inheritance or base-type clause so the keyword
     * immediately before it can be extracted for the closing-comment label.
     */
    private int findConstructNameIndex(
        final List<Token> tokens,
        final int         braceIdx,
        final String      name
    )
    {
        for(int i = braceIdx - 1; i >= 0; --i) {
            final Token     t  = tokens.get(i);
            final TokenType ty = t.type;
            if(ty == TokenType.WHITESPACE || ty == TokenType.NEWLINE || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) continue;
            if( ty == TokenType.IDENTIFIER && name.equals(t.text) ) return i;
            if( ty == TokenType.PUNCT && ( ";".equals(t.text) || "{".equals(t.text) ) ) break;
        } // for

        return -1;
    }

    private String extractSingleIdentifier(final List<Token> body)
    {
        final List<Token> sig = new ArrayList<>();
        for(final Token t : body) {
            if( !isGap(t) ) sig.add(t);
        }
        if( sig.size() == 1 && sig.get(0).type == TokenType.IDENTIFIER ) return sig.get(0).text;
        if( sig.size() == 2 && isOp(
            sig.get(0), "!"
        ) && sig.get(
            1
        ).type == TokenType.IDENTIFIER ) return sig.get(
            1
        ).text;

        return null;
    }

    /**
     * {@code true} if any token in {@code [fromInclusive, toExclusive)} is frozen (RDD_KEY_90
     *  §A) -- used by structural/span-level passes to skip a whole candidate unit rather than try
     *  to partially rewrite it
     */
    private boolean anyFrozen(
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
     * Joins tokens' literal text verbatim, with no whitespace normalization (unlike
     *  {@link #renderInline}) -- used by {@link #alignBracelessElseIfChain}, which works on the
     *  already fully-formatted line-by-line text rather than a token stream
     */
    private String joinVerbatim(final List<Token> tokens)
    {
        final StringBuilder out = new StringBuilder();
        for(final Token t : tokens) out.append(t.text);

        return out.toString();
    }

    /**
     * Column-aligns the collapsed one-line bodies of an {@code if}/{@code else if}/{@code else}
     * chain so every branch's body starts at the same column -- the widest of the chain's own
     * {@code if(...) }/{@code else if(...) } condition widths, never the bare final
     * {@code else}'s (which has no condition of its own to contribute). Originally Kotlin-only
     * (RDD_KEY_128); moved here and generalized to run for every language once C/C++/Java grew
     * their own opt-in braceless chain-collapse ({@link #chainAllBranchesCollapsible}) --
     * nothing about this pass is Kotlin-specific, it is pure line/text matching over whatever
     * {@code if(}/{@code else if(}/{@code else } lines {@link #collapseSingleExpressionBlocks}
     * already produced.
     *
     * <p>Deliberately implemented as its own final, line-based, text-level pass -- not folded
     * into {@code collapseSingleExpressionBlocks} (which produces the one-line body in the first
     * place) -- because at collapse time the `if` branch's own rendered width is not yet final:
     * later passes (e.g. STYLE.md §3.1's complexity-padding tightening) can still change a
     * condition's rendered width. Waiting until every earlier pass has settled the `if` line's
     * final text -- this method runs last, after Phase 4 -- avoids staleness entirely; column
     * widths are simply counted from the final rendered characters.
     *
     * <p>Only fires on a maximal run of consecutive, byte-identical-indent lines starting with
     * {@code if(}, followed by zero or more {@code else if(} lines, optionally terminated by one
     * bare {@code else} line -- any line breaking that shape (different indent, a braced body,
     * anything else) ends the run there, same conservative "only touch what unambiguously
     * matches" posture as the rest of this class.
     */
    public String alignBracelessElseIfChain(final List<Token> tokens)
    {
        final String[] lines = joinVerbatim(tokens).split("\n", -1);
        // A CRLF-original input can still carry a stray trailing '\r' on some/all of these split
        // lines at this point in the pipeline -- final CRLF/LF normalization only happens once, at
        // the very end, in Main.applyLineEndings (see that method's own comment on why: an
        // untouched WHITESPACE token from CRLF input can carry '\r' straight through). Left in
        // place, that extra character skews every length-based computation below (indentLen is
        // unaffected since '\r' only ever appears at a line's end, but `body.length()`/`end`/
        // `target` all include it), which only visibly matters when a computed width sits exactly
        // on the `lineLengthLimit` boundary -- present on round1 (CRLF-original text mid-pipeline)
        // and absent on round2 (LF-only, already-normalized input), silently changing which side of
        // the guard a branch lands on and making this method non-idempotent on CRLF sources. Strip
        // it here, before any measurement -- safe to simply drop rather than restore, since the
        // final output's line-ending style is independently re-derived from the *original* file by
        // `Main.applyLineEndings`, never from '\r' bytes surviving inside the internal pipeline.
        for(int li = 0; li < lines.length; ++li) {
            if( lines[li].endsWith(
                "\r"
            ) ) lines[li] = lines[li].substring(
                0, lines[li].length() - 1
            );
        } // for
                int i = 0;
        while(i < lines.length) {
            int indentLen = leadingWhitespaceLength( lines[i] );
            if( !lines[i].regionMatches(indentLen, "if(", 0, 3) ) {
                ++i;
                continue;
            }
            final List<Integer> chain = new ArrayList<>();
            chain.add(i);
            int j = i + 1;
            while(j < lines.length) {
                final int jIndent = leadingWhitespaceLength( lines[j] );
                      int matchAt = jIndent;
                if(jIndent != indentLen) {
                    // The `if` line above may itself already be left-padded (this same method,
                    // a previous round) to column-align its keyword with `else if` -- its own
                    // leading whitespace then legitimately runs wider than its sibling `else
                    // if`/`else` lines' shared, un-padded base indent. Recognize that specific,
                    // generically-derived shape (this chain's very first member only, `k == 0`,
                    // widened by exactly `"else if(".length() - "if(".length()`, the same delta
                    // the left-padding step below derives) and re-anchor `indentLen` on the
                    // sibling's own (narrower, canonical) indent instead of rejecting the whole
                    // chain outright -- otherwise every subsequent reformat loses the chain
                    // (and, with it, both alignments) the moment the first round's left-padding
                    // makes indentLen stop matching verbatim.
                    if( chain.size() == 1 && jIndent < indentLen && indentLen - jIndent == "else if(".length() - "if(".length() ) {
                        // Strip the stale left-padding back off `lines[i]` so every later step
                        // (kwLen/leftPad computation, prefixEnd measurement) operates on a fresh,
                        // un-padded baseline exactly like a first-round chain -- re-deriving the pad
                        // from scratch below, rather than re-detecting "already correctly padded" as
                        // a special case, is both simpler and immune to the pad amount ever silently
                        // drifting out of sync with the current chain's own widths
                        lines[i]  = lines[i].substring(0, jIndent) + lines[i].substring(indentLen);
                        indentLen = jIndent;
                        matchAt   = jIndent;
                    } // if
                    // Opposite direction: a bare `else` (no adjacent `if` on its own line, so it
                    // has no keyword-shape of its own to column-align, unlike `else if`) that some
                    // earlier structural/statement indent-fixup pass has already re-indented one
                    // level deeper than its paired `if` -- observed on angular/angular's
                    // shared.ts/directive_outputs.ts round2 (that earlier pass treats an
                    // already-braceless standalone `else` as an orphaned continuation statement,
                    // apparently keying off whether the line still carries this same method's own
                    // previous-round column-padding artifact). Recognize the shape narrowly (chain
                    // still size 1, so only the immediate `if`'s bare else -- never an `else if`
                    // member, which has no known/expected deeper-indent shape) and strip the
                    // excess back down to the `if`'s own (already-canonical) indent so the
                    // chain-scan sees a match instead of rejecting the whole chain.
                    else if( chain.size() == 1 && jIndent > indentLen
                            && lines[j].regionMatches(jIndent, "else ", 0, 5)
                            && !lines[j].regionMatches(jIndent, "else if", 0, 7) ) {
                        lines[j] = lines[j].substring(0, indentLen) + lines[j].substring(jIndent);
                        matchAt  = indentLen;
                    }
                    else break;
                } // if
                if( lines[j].regionMatches(matchAt, "else if(", 0, 8) ) {
                    chain.add(j);
                    ++j;
                    continue;
                }
                if( lines[j].regionMatches(matchAt, "else ", 0, 5)
                        && !lines[j].regionMatches(matchAt, "else if", 0, 7) ) {
                    chain.add(j);
                    ++j;
                }
                break;
            } // while
            if( chain.size() < 2 ) {
                ++i;
                continue;
            }

            // Left-pad the leading keyword of every non-bare-else branch (in practice always
            // just the lone `if`) up to the widest keyword prefix in the chain, so the
            // keyword/condition portion also starts at a shared column -- same "never the bare
            // final else's" exclusion as the body-column target below, since a bare else has no
            // condition of its own to align against. Derived generically (never hardcoded)
            // so it stays correct for a plain two-branch if/else chain (no `else if` at all --
            // both widths already equal, leftPad is 0, no churn) as well as any wider chain.
            final int[] kwLen    = new int[ chain.size() ];
                  int   maxKwLen = -1;
            for( int k = 0; k < chain.size(); ++k ) {
                final String  line       = lines[ chain.get(k) ];
                final boolean isElseIf   = line.regionMatches(indentLen, "else if(", 0, 8);
                final boolean isBareElse = line.regionMatches(
                    indentLen, "else ", 0, 5
                ) && !isElseIf;
                if(isBareElse) {
                    kwLen[k] = -1; // Sentinel: bare else never contributes/receives left-padding
                    continue;
                }
                kwLen[k] = isElseIf ? "else if".length() : "if".length();
                maxKwLen = Math.max( maxKwLen, kwLen[k] );
            } // for
            final int[] leftPad = new int[ chain.size() ];
            for( int k = 0; k < chain.size(); ++k ) {
                if( kwLen[k] < 0 ) continue; // Bare else: leftPad stays 0
                final int pad = maxKwLen - kwLen[k];
                if(pad <= 0) continue;
                final int lineIdx = chain.get(k);
                // Same lineLengthLimit guard as the body-column padding below -- left-padding
                // also makes the line longer, so a branch whose line would overflow the limit
                // once padded is left at its own natural (unpadded) keyword column instead
                if( lines[lineIdx].length() + pad > lineLengthLimit ) continue;
                leftPad[k] = pad;
                final StringBuilder sb = new StringBuilder(
                    lines[lineIdx].substring(0, indentLen)
                );
                for(int s = 0; s < pad; ++s) sb.append(' ');
                sb.append( lines[lineIdx].substring(indentLen) );
                lines[lineIdx] = sb.toString();
            } // for

            final int[]   prefixEnd = new int[ chain.size() ];
                  int     target    = -1;
                  boolean ok        = true;
            for( int k = 0; k < chain.size(); ++k ) {
                final String  line       = lines[ chain.get(k) ];
                final boolean isElseIf   = line.regionMatches(indentLen, "else if(", 0, 8);
                final boolean isBareElse = line.regionMatches(
                    indentLen, "else ", 0, 5
                ) && !isElseIf;
                if(isBareElse) {
                    prefixEnd[k] = indentLen + "else".length();
                    continue;
                }
                final int openParen  = isElseIf ? indentLen + "else if".length() : indentLen + leftPad[k] + 2;
                      int depth      = 0;
                      int closeParen = -1;
                for( int c = openParen; c < line.length(); ++c ) {
                    final char ch = line.charAt(c);
                    if(ch == '(') {
                        ++depth;
                    }
                    else if(ch == ')') {
                        --depth;
                        if(depth == 0) {
                            closeParen = c;
                            break;
                        }
                    }
                } // for c
                if( closeParen < 0 || closeParen + 2 > line.length() || line.charAt(
                    closeParen + 1
                ) != ' ' ) {
                    ok = false;
                    break;
                }
                prefixEnd[k] = closeParen + 1;
                target       = Math.max(
                    target, prefixEnd[k] + 1
                ); // +1: desired body column, one past the space
            } // for k
            if(!ok || target < 0) {
                i = j;
                continue;
            }

            for( int k = 0; k < chain.size(); ++k ) {
                final int    lineIdx = chain.get(k);
                final String line    = lines[lineIdx];
                final int    end     = prefixEnd[k];
                      String body    = line.substring(end);
                while( !body.isEmpty() && body.charAt(0) == ' ' ) body = body.substring(1);
                if( body.isEmpty() ) continue;
                final int spaces = Math.max(1, target - end);
                // Refuse to pad this branch past lineLengthLimit *when the un-padded line would
                // otherwise still fit* -- alignBracelessElseIfChain runs last (see this method's
                // own javadoc), so there is no downstream pass left to re-wrap a consequent call
                // that this column padding alone pushes over the limit; a fresh format would
                // otherwise commit a width no re-check ever validates, going stale (and
                // re-collapsing back down) the moment the file is formatted again (found via
                // vuejs/core real-code testing -- widespread across the corpus, e.g. `scripts/
                // release.js`'s `else console.log(...)`). Leaving this one branch un-aligned
                // (single space, its own natural width) is the same conservative "only touch what
                // unambiguously matches" posture the rest of this class already uses elsewhere. If
                // the branch's own natural (unpadded) width already exceeds the limit -- nothing
                // this pass does created that, some earlier pass already accepted it as
                // unavoidably long -- padding it further changes nothing about whether a
                // downstream pass would need to react, so it's still applied (matches existing
                // accepted test output for e.g. long `if(...) return "..." + ...;` bodies).
                final boolean naturalAlreadyOverLimit = end + 1 + body.length() > lineLengthLimit;
                if( !naturalAlreadyOverLimit && end + spaces + body.length() > lineLengthLimit ) continue;
                final StringBuilder sb = new StringBuilder( line.substring(0, end) );
                for(int s = 0; s < spaces; ++s) sb.append(' ');
                sb.append(body);
                lines[lineIdx] = sb.toString();
            } // for
            i = j;
        } // while

        return String.join("\n", lines);
    }

    /** Length of the whitespace run (spaces/tabs only) at the start of {@code line} */
    private int leadingWhitespaceLength(final String line)
    {
        int n = 0;
        while( n < line.length() && ( line.charAt(n) == ' ' || line.charAt(n) == '\t' ) ) n++;

        return n;
    }

} // class BlockStructureRule
