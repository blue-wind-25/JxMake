/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isRepOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isUnaryMinusOperand;

/**
 * Family-agnostic base for {@link DeclarationAlignmentRuleCurly} -- everything in this file used
 * to live directly in {@code DeclarationAlignmentRule} before the curly/indent class-refactor. No
 * behavior change, mechanical move only (see STATE_COMMON.md's Class Refactor section). An
 * optional {@code DeclarationAlignmentRuleIndent} sibling was scaffolded for Python3's possible
 * reuse but was never adopted (Python3 went bespoke instead) and was removed as unused during the
 * 2026-07-28 cleanup pass -- see STATE_COMMON.md's "Project refactoring/cleanup pass".
 */
public abstract class DeclarationAlignmentRuleCore {

    /**
     * Control-flow keywords whose own condition/argument parens must never be mistaken for a
     * C-style cast's parens by {@link #isCStyleCastClose} -- see that method's call site
     */
    private static final Set<String> CONTROL_FLOW_KEYWORDS = setOf(
        "if", "while", "for", "switch", "catch", "do", "else"
    );

    protected final Lang lang;
    // Raised private -> protected (RDD_KEY_139's "loosen-then-extend" precedent) so
    // KotlinDeclarationAlignmentRule can reuse it for its own width-budget pre-check
    // (RDD_KEY_162) -- purely additive, no behavior change
    protected final int lineLengthLimit;

    protected DeclarationAlignmentRuleCore(final Lang lang, final int lineLengthLimit)
    {
        this.lang            = lang;
        this.lineLengthLimit = lineLengthLimit;
    }

    protected static Set<String> setOf(final String... words)
    {
        final Set<String> s = new HashSet<>();
        java.util.Collections.addAll(s, words);

        return s;
    }

    /**
     * Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     * (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change.
     */
    protected String renderTokens(final List<Token> tokens)
    {
        final StringBuilder sb = new StringBuilder();
        // JS/TS-only (STATE_JS_TS.md Checkpoint 20): an object-literal property `key: value`
        // colon must be tight before / spaced after (`a: 1`), but this method's generic
        // `needsSpaceBetween` has no colon-specific case at all -- a plain `:` is simply not in
        // `isTightToken`'s set for any language, so the default fallback (space on both sides,
        // correct for a ternary `cond ? 1 : 2`) was also wrongly applied to an object-literal
        // property colon whenever the initializer went through this shared join point (e.g. a
        // declaration's `= {a: 1, b: 2}` rendered via `renderTokens(r.initTokens)`), producing
        // `{ a : 1, b : 2 }`. `jsObjectPropertyColons` pre-classifies (via a lightweight
        // bracket-stack scan, see `computeJsObjectPropertyColons`) exactly which `:` tokens are a
        // genuine property-key colon at this frame's own depth -- a ternary `:` inside the same
        // object literal's value position, or a nested paren/bracket's own colon (e.g. a TS
        // arrow-parameter type colon, already separately normalized by
        // `JsTsSpecificRule.enforceTypeColonSpacing`'s own later flat pass), is left completely
        // out of the set and keeps this method's existing default spacing. Empty (zero-cost, zero
        // behavior change) for every other language.
        final java.util.Set<Token> jsObjectPropertyColons = (lang.isJs || lang.isTs)
                ? computeJsObjectPropertyColons(tokens) : java.util.Collections.emptySet();
        Token prev = null;
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if(prev != null) {
                if( !jsObjectPropertyColons.contains(
                    t
                ) && !isUnaryMinusOperand(
                    tokens, i
                ) && needsSpaceBetween(
                    prev, t, tokens, i
                ) ) sb.append(
                    ' '
                );
            } // if
            sb.append(t.text);
            prev = t;
        } // for

        return sb.toString();
    }

    /**
     * JS/TS-only helper for {@link #renderTokens}: identifies every `:` token that is a
     * genuine object-literal property-key colon (tight-before), as opposed to a ternary `:`
     * or a colon nested inside a paren/bracket frame (e.g. a TS parameter type annotation,
     * already handled downstream by {@code JsTsSpecificRule.enforceTypeColonSpacing}). Tracks a
     * small per-frame stack: each `{`/`(`/`[` pushes a frame; a `{` is classified as an
     * object-literal frame unless its immediately preceding significant token is an
     * IDENTIFIER, `=>`, or `)` (a block body, not a literal) -- the very first token of an
     * initializer (`prevSig == null`) is treated as an object literal, matching `= {...}`'s
     * own shape. Within an object-literal frame, an `expectingKey` flag starts `true` right
     * after `{`/a top-level `,`, and is cleared the first time a `:` is seen at that frame's own
     * depth -- so only that first colon per key/value pair is classified; a later ternary `:`
     * in the same value position (`{a: cond ? 1 : 2}`) is correctly left unclassified since
     * `expectingKey` is already `false` by the time it's reached.
     */
    private java.util.Set<Token> computeJsObjectPropertyColons(final List<Token> tokens)
    {
        final java.util.Set<Token> result = new java.util.HashSet<>();
        final java.util.Deque<Boolean> objFrame = new java.util.ArrayDeque<>();
        final java.util.Deque<Boolean> expectKey = new java.util.ArrayDeque<>();
        Token prevSig = null;
        for(final Token t : tokens) {
            if( isGapToken(t) ) continue;
            if( isPunct(t, "{") ) {
                final boolean isObj = prevSig == null || !( prevSig.type == TokenType.IDENTIFIER || isOp(
                    prevSig, "=>"
                ) || isPunct(
                    prevSig, ")"
                ) );
                objFrame.push(isObj);
                expectKey.push(isObj);
            } // if
            else if( isPunct(t, "}") ) {
                if( !objFrame.isEmpty() ) {
                    objFrame.pop();
                    expectKey.pop();
                }
            }
            else if( isPunct(t, "(") || isPunct(t, "[") ) {
                objFrame.push(false);
                expectKey.push(false);
            }
            else if( isPunct(t, ")") || isPunct(t, "]") ) {
                if( !objFrame.isEmpty() ) {
                    objFrame.pop();
                    expectKey.pop();
                }
            }
            else if( isPunct(t, ",") && !objFrame.isEmpty() && objFrame.peek() ) {
                expectKey.pop();
                expectKey.push(true);
            }
            else if( isOp(
                t, ":"
            ) && !objFrame.isEmpty() && objFrame.peek() && !expectKey.isEmpty() && expectKey.peek() ) {
                result.add(t);
                expectKey.pop();
                expectKey.push(false);
            }
            prevSig = t;
        } // for

        return result;
    }

    /**
     * Renders initializer value tokens (the right-hand side of `= expr`) where `*` and `&`
     * may represent either binary operators or unary pointer/reference operators. Uses
     * lookahead to distinguish binary `*`/`&`: if followed by an IDENTIFIER or NUMBER and
     * preceded by an IDENTIFIER or WHITESPACE, a space is inserted before the operator.
     * Also suppresses spacing between unary dereference `*`/`**` and the following
     * identifier in C/C++ expression contexts (e.g. `*ptr`, `**ptr`), while all other
     * spacing follows the normal token-spacing rules.
     */
    protected String renderInitTokens(final List<Token> tokens)
    {
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t    = tokens.get(i);
            final Token prev = i > 0 ? tokens.get(i - 1) : null;
            final Token next = i < tokens.size() - 1 ? tokens.get(i + 1) : null;
            if(prev != null) {
                if( isTightToken(t) && ( isOp(t, "*") || isOp(t, "&") )
                        && next != null
                        && (next.type == TokenType.IDENTIFIER || next.type == TokenType.NUMBER) ) {
                    if(prev.type != TokenType.IDENTIFIER && prev.type != TokenType.WHITESPACE) {
                    }
                    else {
                        sb.append(' '); // Binary * or & in expression context
                    }
                } // if
                else if( !isUnaryMinusOperand(
                    tokens, i
                ) && needsSpaceBetween(
                    prev, t, tokens, i
                ) ) {
                    final Token prev2 = i > 1 ? tokens.get(i - 2) : null;
                    if( t.type == TokenType.IDENTIFIER && isRepOp(prev, '*')
                        && (prev2 == null || prev2.type == TokenType.OP)
                        && (lang.isC || lang.isCpp) ) {
                        // Pointer dereference: add nothing
                    }
                    else if( isPunct(prev, ")") && isCStyleCastClose(tokens, i - 1) ) {
                        // C-style cast `(Type)expr`: add nothing
                    }
                    else {
                        sb.append(' ');
                    }
                }
            } // if
            sb.append(t.text);
        } // for

        return sb.toString();
    }

    /**
     * True iff the `)` at `closeIdx` in `tokens` closes a C-style cast: `(Type)` where the
     * content between the matching `(` and `)` is just a type-like token sequence
     * (IDENTIFIER/KEYWORD plus optional `*`), and the token before the matching `(` is not
     * an IDENTIFIER/`)`/`]` (which would make it a function call or subscript instead)
     */
    protected boolean isCStyleCastClose(final List<Token> tokens, final int closeIdx)
    {
        int depth   = 0;
        int openIdx = -1;
        for(int k = closeIdx; k >= 0; --k) {
            final Token t = tokens.get(k);
            if( isPunct(t, ")") ) {
                ++depth;
            }
            else if( isPunct(t, "(") ) {
                --depth;
                if(depth == 0) {
                    openIdx = k;
                    break;
                }
            }
        } // for
        if(openIdx < 0 || openIdx == closeIdx - 1) return false; // Empty parens
        final Token before = openIdx > 0 ? tokens.get(openIdx - 1) : null;
        if( before != null && ( before.type == TokenType.IDENTIFIER || isPunct(
            before, ")"
        ) || isPunct(
            before, "]"
        ) ) ) return false; // function call / subscript, not a cast
        // A control-flow keyword's own condition parens (`if(node instanceof X)`,
        // `while(cond)`, `for(...)`, `switch(...)`, `catch(...)`) has the exact same shape this
        // method looks for -- IDENTIFIER/KEYWORD-only content, and `if`/`while`/etc. are
        // KEYWORD tokens, not IDENTIFIER/`)`/`]`, so they slip past the check above. Left
        // unguarded, a braceless `if(cond)stmt` collapsed onto one line by
        // `BlockStructureRule` gets its condition's closing paren misclassified as a C-style
        // cast close, suppressing the space that must separate it from the following
        // statement (`if(node instanceof RecordPatternExpr)reporter.report(...)` instead of
        // `... RecordPatternExpr) reporter.report(...)`) -- only when this whole construct is
        // itself rendered as a declaration's initializer via `renderInitTokens`.
        if( before != null && before.type == TokenType.KEYWORD && CONTROL_FLOW_KEYWORDS.contains(
            before.text
        ) ) return false;
        for(int k = openIdx + 1; k < closeIdx; ++k) {
            final Token t = tokens.get(k);
            if( t.type != TokenType.IDENTIFIER && t.type != TokenType.KEYWORD && !isRepOp(
                t, '*'
            ) ) return false;
        }

        return true;
    }

    /**
     * Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     * (its own {@code renderTokens} override, real-code testing against {@code square/okio}) --
     * purely additive, no behavior change
     */
    protected boolean needsSpaceBetween(final Token prev, final Token cur)
    {
        return needsSpaceBetween(prev, cur, null, -1);
    }

    /**
     * Same as {@link #needsSpaceBetween(Token, Token)} but with the full token list + current
     * index available, needed only by the C6d function-type-position lookahead below (every
     * other rule in this method is a purely local prev/cur decision). Callers that can't supply
     * a list (none currently) fall back to the 2-arg overload, which passes {@code tokens = null}
     * -- {@link #isAnnotationFunctionTypeParen} treats a missing list as "not a function type",
     * preserving this method's pre-C6d tight-after-`@` behavior exactly.
     */
    protected boolean needsSpaceBetween(
        final Token       prev,
        final Token       cur,
        final List<Token> tokens,
        final int         curIdx
    )
    {
        // Kotlin's newer square-bracket destructuring lambda-parameter-list syntax (`{ [x, y] ->
        // x + y }`) opens directly with `[` right after the lambda's own `{` -- e.g. a `val`
        // initializer's trailing lambda (`val result = list.map { [x, y] -> x + y }`) renders
        // through this class's own renderTokens/renderInitTokens, a separate duplicate of
        // MiscRuleCore's tight-attachment rules (see this method's other duplicated cases below).
        // `isTightToken`'s `[`-is-always-tight rule exists for indexing/subscript shapes (`a[i]`),
        // where `[` always follows an identifier/closing bracket/paren, never a bare `{` --
        // Kotlin has no bracket array-literal syntax, so `{` directly followed by `[` is
        // unambiguous: it can only be this destructuring param list, never an indexing
        // expression. Mirrors MiscRuleCore.needsSpaceBetween's identical carve-out (C6j).
        if( lang.isKotlin && isPunct(cur, "[") && isPunct(prev, "{") ) return true;
        // A segmented JS/TS template literal's `${`/`}` hole-boundary tokens (`TEMPLATE_HOLE_OPEN`/
        // `TEMPLATE_HOLE_CLOSE`, emitted by `TokenizerCurly.emitTemplateLiteralSegmented` whenever
        // `lang.isJsxSyntax`) are always tight against whatever precedes/follows them on both sides
        // -- they reconstruct one continuous piece of source text (a template literal), never a
        // real expression boundary this class's own duplicated adjacency rules should touch. This
        // duplicate of `MiscRuleCore.needsSpaceBetween` is exercised whenever a declaration
        // statement (e.g. `const label = \`User: ${name}\`;`) is grouped/rendered by
        // `DeclarationAlignmentRuleCurly`/`JsTsDeclarationAlignmentRule` -- found via
        // `DEBUG_PHASES`-gated phase tracing once JSX detection widened to plain `.js`/`.mjs`/
        // `.cjs` (STATE_JS_TS.md's 2026-08-13 implementation section) exposed this shape at
        // real-fixture scale for the first time; the segmented shape's original `.jsx`/`.tsx`-only
        // validation never happened to include a template literal inside a declaration statement.
        if(prev.type == TokenType.TEMPLATE_HOLE_OPEN || prev.type == TokenType.TEMPLATE_HOLE_CLOSE || cur.type == TokenType.TEMPLATE_HOLE_OPEN || cur.type == TokenType.TEMPLATE_HOLE_CLOSE) return false;
        if( isTightToken(cur) ) return false;
        // Kotlin's negated type-check/containment operators (`!is`, `!in`) are a single tight
        // lexical unit -- STYLE_KOTLIN.md renders them with no space between `!` and the
        // keyword, unlike a plain `is`/`in` (`a is B`, always spaced). The tokenizer still
        // lexes `!` and `is`/`in` as two separate tokens, so without this check the generic
        // KEYWORD-gets-a-leading-space default below inserted a space here, corrupting `!is`/
        // `!in` into `! is`/`! in` -- a Kotlin parse error found via dogfood-testing
        // RobotCoding gui_frontend_android's ProgramBuilder.kt (`it !is _FunctionItem`).
        if( lang.isKotlin && isOp(
            prev, "!"
        ) && cur.type == TokenType.KEYWORD && ( "is".equals(
            cur.text
        ) || "in".equals(
            cur.text
        ) ) ) return false;
        // C6d: an annotation identifier directly ahead of a function-*type* literal's opening `(`
        // (`@Composable (Params) -> Type`, as a parameter type or property type) needs a space --
        // the mirror image of `@JsNoLifting { ... }`'s tight case further below. Without this
        // carve-out, the general call-tight rule right after (any `IDENTIFIER` immediately before
        // `(` is a call/declaration paren, always tight -- correct for `@Composable(x)`'s own
        // annotation-argument-list shape) fires first and wrongly tightens this case too, since
        // both shapes are `IDENTIFIER` then `(` at this join -- disambiguated only by lookback (is
        // `prev` itself an annotation name, i.e. immediately preceded by `@`?) plus lookahead (is
        // this specific `(...)` actually a function type, i.e. its matching `)` followed by
        // `->`?) via `isAnnotationFunctionTypeParen`.
        if( lang.isKotlin && isPunct(
            cur, "("
        ) && prev.type == TokenType.IDENTIFIER && tokens != null && curIdx >= 2 && isOp(
            tokens.get(curIdx - 2), "@"
        ) && isAnnotationFunctionTypeParen(
            tokens, curIdx
        ) ) return true;
        if( isPunct(
            cur, "("
        ) && (prev.type == TokenType.IDENTIFIER || prev.type == TokenType.ANGLE_BRACKET_CLOSE) ) return false;
        // Kotlin `get`/`set` accessor keywords act like an ordinary call name immediately before
        // `(` (e.g. a merged §8 one-liner property, `val x : Int get() = 1`, RDD_KEY_133) --
        // `get`/`set` are lexed as TokenType.KEYWORD (STYLE_KOTLIN.md's soft-keyword list), not
        // IDENTIFIER, so the general identifier-before-`(` tight rule above doesn't fire for them
        // without this carve-out. Gated to Kotlin only; no-op for C/C++/Java (neither keyword is
        // reserved there, so this case never reaches this method for them).
        if( lang.isKotlin && isPunct(
            cur, "("
        ) && prev.type == TokenType.KEYWORD && ( "get".equals(
            prev.text
        ) || "set".equals(
            prev.text
        ) ) ) return false;
        // C/C++/Java-only: a brace-initializer directly after a type/identifier (`int arr[] =
        // {1,2,3}`, C++'s uniform-init `Widget w{}`) is tight, no space before `{`. Kotlin has no
        // such shape -- its only identifier-then-`{` construct is a trailing lambda (`x?.let {
        // it + 1 }`), which STYLE_KOTLIN.md's own worked examples always show with a space before
        // the `{` -- so this must not fire for Kotlin (was wrongly collapsing `.let { ... }` to
        // `.let{ ... }` before this gate, since `renderKotlinTokens`/`renderTokens` reuses this
        // same shared method for a declaration's initializer tokens).
        if( isPunct(
            cur, "{"
        ) && !lang.isKotlin && (prev.type == TokenType.IDENTIFIER || prev.type == TokenType.ANGLE_BRACKET_CLOSE) ) return false;
        if( prev.type == TokenType.ANGLE_BRACKET_OPEN || isOp(
            prev, "::"
        ) || isOp(
            prev, "."
        ) || isOp(
            prev, "->"
        ) || isPunct(
            prev, "["
        ) || isPunct(
            prev, "("
        ) ) return false;
        // A prefix `++`/`--` immediately followed by an identifier (`++i`, `--count`) only ever
        // occurs in prefix position at this join (postfix pairs the operator *after* the
        // identifier, `i++`, never before) -- always tight, no space. Mirrors the same fix in
        // `MiscRuleCore.needsSpaceBetween` (this method is a documented duplicate of its
        // tight-attachment rules); found via the same openrewrite/rewrite dogfood idempotency case
        // (a `for(...; ++i)` header's increment clause re-rendered through this join point on
        // round2 lost the tight join `MiscRuleCurly.enforcePreIncrement`'s swap-render path had
        // produced on round1).
        if( ( isOp(
            prev, "++"
        ) || isOp(
            prev, "--"
        ) ) && cur.type == TokenType.IDENTIFIER ) return false;
        // A unary logical-NOT `!` is always tight against its operand (`!atEnd`, `!foo.bar()`)
        // -- C/C++/Java/Kotlin/JS/TS have no binary `!` operator at all (Kotlin's own `!is`/`!in`
        // carve-out above already handles its one keyword-operand exception before reaching this
        // fallback), so this join is unconditionally tight regardless of language. Mirrors the
        // same fix in `MiscRuleCore.needsSpaceBetween`. Found via `RDD_KEY_238`'s follow-up
        // "general expression-statement operator spacing" gap -- a plain assignment statement's
        // RHS (`groupStart = (!atEnd && ...) ? i : -1;`, self-hosting dogfood on
        // `TomlSpecificRule.java`/`CssSpecificRule.java`/`YamlSpecificRule.java`/
        // `JsonSpecificRule.java`) rendered through this shared join point with a spurious space
        // after `!` (`! atEnd`) once the assignment-RHS respacing fix below started re-deriving
        // spacing for that shape.
        if( isOp(prev, "!") ) return false;
        // JS/TS spread/rest `...` is always tight against what follows it (`...rest`,
        // `...items`) -- STYLE_JS_TS.md §3, enforced file-wide by
        // JsTsSpecificRule.enforceSpreadRestSpacing, which runs long after this class's own
        // declaration-alignment pass (FormatterCurly Phase 0 vs. its later flat Phase-1 pass).
        // Without this, a destructuring-pattern LHS's `nameText` (built via this method through
        // `renderTokens(patternTokens)`) measures `...rest` with a stray space (`... rest`,
        // 22 chars) that the later pass then strips from the *rendered* text -- but not before
        // ColumnGrid has already baked that 1-char-too-wide measurement into a sibling row's
        // padding (`const { id, name, ...rest } = obj;` / `const x = 1;` misaligned by one
        // column). Mirroring the later pass's own tight-after rule here up front keeps the two
        // in agreement from the start.
        if( (lang.isJs || lang.isTs) && isOp(prev, "...") ) return false;
        // An annotation's `@` immediately preceding an expression-position annotation (Kotlin's
        // `val lambda = @JsNoLifting { ... }`, an annotation directly ahead of a lambda/call
        // rather than a declaration/param-target) is tight against the identifier that follows
        // it, same as MiscRuleCore.needsSpaceBetween's declaration-site/param-target `@` rule --
        // that method is never reached here because a declaration initializer's tokens (e.g. a
        // `val`'s `= @JsNoLifting { ... }` RHS) render through this class's own
        // renderTokens/renderInitTokens, a separate duplicate of the tight-attachment rules (see
        // this method's other duplicated cases above). Without this, the generic
        // OPERATOR-gets-a-leading-space default below inserted a space here, corrupting
        // `@JsNoLifting` into `@ JsNoLifting` -- a Kotlin parse error found via
        // JetBrains/kotlin dogfood testing (`libraries/stdlib/js/runtime/reflectRuntime.kt`'s
        // `val lambda = @JsNoLifting { throwUnsupportedOperationException(...) }`).
        if( lang.isKotlin && isOp(prev, "@") ) return false;
        // C6b: Kotlin 2.4's multi-dollar string interpolation prefix (`$$"..."`, `$$$"""..."""`)
        // is tokenized as a plain IDENTIFIER (the tokenizer's `isIdentifierChar`/`isIdentifierStart`
        // already treat `$` as an identifier char, same as bare `$x` interpolation), immediately
        // followed by the STRING token it prefixes -- no gap allowed by the grammar. Without this,
        // the generic default below inserts a space (`$$ "$key1"`), a Kotlin parse error. Exact
        // duplicate of `MiscRuleCore.needsSpaceBetween`'s identical carve-out.
        if( lang.isKotlin && prev.type == TokenType.IDENTIFIER && isDollarRun(
            prev.text
        ) && cur.type == TokenType.STRING ) return false;

        return true;
    }

    /**
     * True iff {@code text} is exactly `"$$"` or `"$$$"` -- Kotlin 2.4's multi-dollar string
     * interpolation prefix (2 or 3 dollar signs, per the language spec; a single bare `$` has no
     * such meaning and is left alone). Used only by the C6b carve-out above. Exact duplicate of
     * `MiscRuleCore.isDollarRun`.
     */
    protected static boolean isDollarRun(final String text)
    {
        return "$$".equals(text) || "$$$".equals(text);
    }

    /**
     * True iff `tokens.get(parenIdx)` is `(` and it opens a function type's parameter list --
     * i.e. its matching `)` is followed (skipping whitespace/comments/newlines) by `->`, OR by a
     * `?` (C6k-5: a nullable function type used as a parameter's default-bearing type wraps the
     * whole thing in its own parens, `@Composable( () -> Unit )?` -- here `parenIdx` is the
     * *outer* paren, whose own matching `)` is followed by `?`, not `->` (the `->` sits inside,
     * before the close). A bare `?` immediately after `@Identifier(...)` is unambiguous evidence
     * of this shape -- an annotation invocation's own argument list is never itself followed by
     * `?`, so no further lookahead is needed, same "unambiguous by construction" reasoning as
     * C6j's `{`-then-`[` carve-out). Used only by C6d's annotation-vs-function-type
     * disambiguation above; `tokens == null` (the 2-arg {@link #needsSpaceBetween} overload, no
     * list available) conservatively returns {@code false}, preserving pre-C6d behavior for any
     * caller that can't supply a list.
     */
    private boolean isAnnotationFunctionTypeParen(final List<Token> tokens, final int parenIdx)
    {
        if( tokens == null || parenIdx < 0 || parenIdx >= tokens.size() ) return false;
        int depth    = 0;
        int closeIdx = -1;
        for( int k = parenIdx; k < tokens.size(); ++k ) {
            final Token t = tokens.get(k);
            if( isPunct(t, "(") ) {
                ++depth;
            }
            else if( isPunct(t, ")") ) {
                --depth;
                if(depth == 0) {
                    closeIdx = k;
                    break;
                }
            }
        } // for
        if(closeIdx < 0) return false;
        for( int k = closeIdx + 1; k < tokens.size(); ++k ) {
            final Token t = tokens.get(k);
            if( isGapToken(t) ) continue;
            return isOp(t, "->") || isOp(t, "?");
        }

        return false;
    }

    protected boolean isTightToken(final Token t)
    {
        if(t.type == TokenType.ANGLE_BRACKET_OPEN || t.type == TokenType.ANGLE_BRACKET_CLOSE) return true;
        if( isPunct(t, ",") || isPunct(t, "[") || isPunct(t, "]") || isPunct(t, ")") ) return true;
        // Kotlin's bare `?` (type nullability suffix, e.g. `Int?`) is always tight against the
        // preceding type token -- see MiscRule.isTightToken's identical, already-established
        // reasoning for this same rule (STYLE_KOTLIN.md's `Type?` rendering via
        // KotlinDeclarationAlignmentRule.renderKotlinTokens, which reuses this method).
        if( lang.isKotlin && isOp(t, "?") ) return true;
        // isRepOp(t, '&') matches ANY run of `&` characters, including Kotlin's `&&`
        // logical-AND operator -- it was written for C/C++'s repeated pointer/reference
        // operators (`**`, `&&` as an rvalue-reference declarator), which don't exist in
        // Kotlin. Without this gate, `val x = a && b` loses its space before `&&` (rendered
        // as `a&& b`) because isTightToken wrongly treats `&&` as tight. Kotlin has no
        // unary/repeated `*`/`&` construct at all, so both checks are gated to non-Kotlin.
        // Same reasoning applies to JS/TS (STATE_JS_TS.md §11 declaration-alignment-grid
        // checkpoint, found via harness testing `JsTsDeclarationAlignmentRule`): JS/TS has no
        // pointer/reference `*`/`&`/`**`/`&&`-as-declarator construct either -- `*` is always
        // multiplication (or generator-function `*`, itself never adjacent to an operand this
        // way) and `&`/`&&` are always bitwise-AND/logical-AND, so without this same exclusion
        // `x => x * 2` loses its space before `*` (`x* 2`) when rendered through this shared
        // method (e.g. a declaration initializer's grid cell). Java is excluded for the exact
        // same reason (RDD_KEY_TBD, self-hosting dogfood bug): Java has no pointer/reference
        // declarator syntax at all -- `&`/`&&` there are always bitwise-AND/logical-AND (or,
        // for a bare single `&`, an intersection-type bound like `<T extends A & B>`, itself
        // conventionally spaced) -- so treating a run of `&` as tight wrongly collapsed
        // `x >= 2 && y` into `x >= 2&& y` wherever this shared join point rendered a Java
        // declaration initializer (found in the formatter's own
        // `XmlSpecificRule.renderScriptOrStyle`-adjacent `shouldFosterParent` local:
        // `final boolean fostered = ... >= LEVEL_TABLE_FOSTER&& ! fosterBufferStack.isEmpty()
        // && ...`). `*` stays tight for Java (pointer syntax doesn't exist there either, but
        // Java's own `*` is only ever multiplication or an import-on-demand wildcard, neither
        // of which reaches this declaration-initializer join point) -- left as-is to avoid
        // touching a code path with no observed bug.
        return ( !lang.isKotlin && !lang.isJs && !lang.isTs
                && ( isRepOp(t, '*') || ( !lang.isJava && isRepOp(t, '&') ) ) )
                || isOp(t, "::") || isOp(t, ".") || isOp(t, "->");
    }

    /**
     * Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     * (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change.
     */
    protected List<List<Token>> splitStatements(final List<Token> scopeTokens)
    {
        final List<List<Token>> statements = new ArrayList<>();
              List<Token>       current    = new ArrayList<>();
        final int               n          = scopeTokens.size();
              int               depth      = 0;
              int               idx        = 0;

        while(idx < n) {
            final Token t = scopeTokens.get(idx);
            current.add(t);
            ++idx;

            // A preprocessor directive reached at top level must always start a fresh statement,
            // even mid-run: a semicolon-less macro-invocation "statement" (e.g. a bare pragma-push
            // macro call, no trailing `;`) otherwise never closes `current`, so a `#if`/`#endif`
            // between it and the next real `;`-terminated statement gets folded into the *middle*
            // of that statement's token list -- invisible to `parseDeclaration`'s field-based
            // reconstruction, which silently drops any token it doesn't recognize as part of a
            // declaration's shape (real compile-breaking bug: an `#if` disappearing while its
            // paired `#endif`, now the next statement's own leading token, survives -- see
            // real_code_regressions_34). Back the just-appended directive token out, close out any
            // real accumulated content as its own statement first, then let it lead a new one --
            // consistent with `hasCommentBefore`'s already-established leading-directive handling.
            if( depth == 0 && (t.type == TokenType.PREPROCESSOR || t.type == TokenType.MACRO_DEF) ) {
                current.remove( current.size() - 1 );
                if( !significantOnly(current).isEmpty() ) {
                    statements.add(current);
                    current = new ArrayList<>();
                }
                current.add(t);
                continue;
            } // if

            if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) {
                ++depth;
                continue;
            }
            if( isPunct(t, ")") || isPunct(t, "]") ) {
                --depth;
                continue;
            }
            if( isPunct(t, "}") ) {
                --depth;
                if(depth == 0) {
                    // Peek ahead: if the next significant token is `;` this `}` closes a
                    // brace-initializer inside a declaration (e.g. `auto x = T{...};`), not
                    // a method/class body — let the `;` emit the statement instead. A next
                    // token of `=` is the same "not a body close" case for JS/TS's
                    // destructuring-pattern LHS (`const { id, name } = obj;`, RDD_KEY_182) --
                    // this shape is not otherwise valid in any curly-family language directly
                    // after the keyword/name position, so extending the check here is safe for
                    // every language, not just JS/TS.
                    boolean nextIsSemi = false;
                    for(int peek = idx; peek < n; ++peek) {
                        final Token nx = scopeTokens.get(peek);
                        if(nx.type == TokenType.WHITESPACE || nx.type == TokenType.NEWLINE || nx.type == TokenType.COMMENT_LINE || nx.type == TokenType.COMMENT_BLOCK) continue;
                        nextIsSemi = isPunct(nx, ";") || isOp(nx, "=");
                        break;
                    }
                    if(!nextIsSemi) {
                        idx = pullTrailingSameLine(scopeTokens, current, idx);
                        statements.add(current);
                        current = new ArrayList<>();
                    }
                } // if
                continue;
            } // if

            if( depth == 0 && t.type == TokenType.OP && ":".equals(t.text)
                    && isAccessSpecifierColon(current) ) {
                idx = pullTrailingSameLine(scopeTokens, current, idx);
                statements.add(current);
                current = new ArrayList<>();
                continue;
            }
            if( depth == 0 && t.type == TokenType.PUNCT && ";".equals(t.text) ) {
                idx = pullTrailingSameLine(scopeTokens, current, idx);
                statements.add(current);
                current = new ArrayList<>();
            }
        } // while

        if( !current.isEmpty() ) statements.add(current);

        return statements;
    }

    /**
     * Pulls a same-line trailing comment after a just-closed statement so it stays attached
     * to that statement instead of becoming the next statement's leading token -- ported from
     * {@code MiscRule.splitAssignmentStatements}'s identical depth-aware splitting algorithm
     * (see STATE.md "`DeclarationAlignmentRule.splitStatements` depth-awareness fix").
     */
    private int pullTrailingSameLine(
        final List<Token> tokens,
        final List<Token> current,
        final int         from
    )
    {
          int idx = from;
    final int n   = tokens.size();
        while(idx < n) {
            final Token next = tokens.get(idx);
            if(next.type == TokenType.WHITESPACE || next.type == TokenType.COMMENT_LINE
                    || next.type == TokenType.COMMENT_BLOCK) {
                current.add(next);
                ++idx;
            }
            else {
                break;
            }
        } // while

        return idx;
    }

    /**
     * True iff {@code current} contains exactly one significant non-gap token followed by {@code :}
     * and that token is {@code public}, {@code private}, or {@code protected} -- i.e. this `:` is
     * a C++ access-specifier label boundary, not a ternary or bitfield colon.
     */
    private boolean isAccessSpecifierColon(final List<Token> current)
    {
        final List<Token> sig = significantOnly(current);
        if( sig.size() != 2 ) return false;
        final Token kw  = sig.get(0);
        final Token col = sig.get(1);

        return kw.type == TokenType.KEYWORD
                && ( "public".equals(
                    kw.text
                ) || "private".equals(
                    kw.text
                ) || "protected".equals(
                    kw.text
                ) )
                && col.type == TokenType.OP && ":".equals(col.text);
    }

    /**
     * Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     * (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change.
     */
    protected boolean hasCommentBefore(final List<Token> stmt)
    {
        for(final Token t : stmt) {
            if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK || t.type == TokenType.PREPROCESSOR || t.type == TokenType.MACRO_DEF) return true;
            if(t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) break;
        }

        return false;
    }

    /**
     * Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     * (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change.
     */
    protected boolean hasBlankLineBefore(final List<Token> stmt)
    {
        int newlineRun = 0;
        for(final Token t : stmt) {
            if(t.type == TokenType.NEWLINE) {
                ++newlineRun;
                if(newlineRun >= 2) return true;
            }
            else if(t.type == TokenType.WHITESPACE) {
                // Ignore -- doesn't break or extend the newline run
            }
            else if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK
                    || t.type == TokenType.PREPROCESSOR || t.type == TokenType.MACRO_DEF) {
                newlineRun = 0; // A comment/preprocessor line consumes that line's content slot
            }
            else {
                break;
            }
        } // for

        return false;
    }

    /**
     * Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     * (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change.
     */
    protected int lastSignificantIdx(final List<Token> tokens, final int from, final int to)
    {
        for(int k = to - 1; k >= from; --k) {
            if( !isGapToken( tokens.get(k) ) ) return k;
        }

        return -1;
    }

    /**
     * Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     * (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change.
     */
    protected Token findTrailingComment(final List<Token> stmt)
    {
        for( int k = stmt.size() - 1; k >= 0; --k ) {
            final Token t = stmt.get(k);
            if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) return t;
            if(t.type != TokenType.WHITESPACE) break;
        }

        return null;
    }

    /**
     * Visibility raised private -> protected for {@code KotlinDeclarationAlignmentRule} reuse
     * (STYLE_KOTLIN.md §6, RDD_KEY_103) -- purely additive, no behavior change.
     */
    protected List<Token> significantOnly(final List<Token> stmt)
    {
        final List<Token> sig = new ArrayList<>();
        for(final Token t : stmt) {
            switch(t.type) {
                case WHITESPACE    : /* FALL-THROUGH */
                case NEWLINE       : /* FALL-THROUGH */
                case COMMENT_LINE  : /* FALL-THROUGH */
                case COMMENT_BLOCK : /* FALL-THROUGH */
                case PREPROCESSOR  : /* FALL-THROUGH */
                case MACRO_DEF     : continue  ;
                default            : sig.add(t);
            } // switch
        } // for

        return sig;
    }

} // class DeclarationAlignmentRuleCore
