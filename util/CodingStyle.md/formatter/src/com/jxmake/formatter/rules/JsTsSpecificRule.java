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

import com.jxmake.formatter.FormatterSimpleBraced;
import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;
import com.jxmake.formatter.tokenizer.TokenizerCurly;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isComment;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

/**
 * Landing spot for JavaScript/TypeScript-only STYLE_JS_TS.md sections not reusable from the
 * shared curly-family rule classes -- mirrors {@link KotlinSpecificRule}'s role for Kotlin.
 * Shared between JS and TS (gate internally on {@code lang.isTs} for TS-only additions such as
 * type annotations/interfaces/enums, per RDD_KEY_187), rather than splitting into separate
 * per-language classes.
 *
 * <p>Checkpoint 3 (this checkpoint): STYLE_JS_TS.md §2 (statement-termination semicolon
 * insertion) only. See STATE_JS_TS.md's checklist for what's implemented so far.
 */
public final class JsTsSpecificRule {

    private final Lang   lang;
    private final int    lineLengthLimit;
    private final String defaultIndentUnit;

    public JsTsSpecificRule(final Lang lang)
    {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public JsTsSpecificRule(final Lang lang, final int lineLengthLimit)
    {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public JsTsSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth)
    {
        this.lang            = lang;
        this.lineLengthLimit = lineLengthLimit;
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < indentWidth; ++i) sb.append(' ');
        this.defaultIndentUnit = sb.toString();
    }

    // ── §2 Statement termination (semicolon insertion) ──────────────────────────────
    /**
     * Tokens that mean "the statement isn't finished yet" when they're the last significant
     *  token seen before a would-be statement boundary -- a trailing binary/assignment operator,
     *  a comma, a colon (object-literal key or ternary), an opening bracket, or a keyword that
     *  can never end a statement on its own
     */
    private static final Set<String> CONTINUATION_OPS = new HashSet<>( Arrays.asList(
        "=",
        "+",
        "-",
        "*",
        "/",
        "%",
        "&&",
        "||",
        "??",
        "?",
        ".",
        "=>",
        "==",
        "===",
        "!=",
        "!==",
        "<",
        ">",
        "<=",
        ">=",
        "+=",
        "-=",
        "*=",
        "/=",
        "%=",
        "&&=",
        "||=",
        "??=",
        "&",
        "|",
        "^",
        "<<",
        ">>",
        ">>>",
        "...",
        "**",
        "**=",
        "&=",
        "|=",
        "^=",
        "<<=",
        ">>=",
        ">>>=",
        ":"
    ) );

    /**
     * Operators that, when they're the FIRST significant token on the line following a
     *  candidate statement boundary (leading-operator continuation style -- method chaining
     *  `.get()\n.expect()`, comma-first declarator lists `var a = 1\n  , b = 2`, ternary
     *  break-before-operator `cond\n  ? x\n  : y`, logical-operator chains, etc.), can *never*
     *  legally begin a new statement -- so their presence there means the previous line's
     *  statement is not actually finished yet, regardless of what its own last token was.
     *  Deliberately excludes ops that have a valid unary/statement-leading use (`+`, `-`, `!`,
     *  `...` (rest/spread can't start a statement either, but is excluded out of caution as
     *  it is not observed as a real leading-continuation shape) -- so this set stays narrower
     *  than {@link #CONTINUATION_OPS} on purpose.
     */
    private static final Set<String> LEADING_CONTINUATION_OPS = new HashSet<>( Arrays.asList(
        ".",
        "?.",
        "?",
        ":",
        "&&",
        "||",
        "??",
        "==",
        "===",
        "!=",
        "!==",
        "<=",
        ">=",
        "+=",
        "-=",
        "*=",
        "/=",
        "%=",
        "&&=",
        "||=",
        "??=",
        "*",
        "/",
        "%",
        "&",
        "^",
        "<<",
        ">>",
        ">>>",
        "**"
    ) );

    // "void" is deliberately excluded here even though it's a prefix operator (`void 0`) in
    // expression position -- as such it's never the LAST token of a statement (its operand
    // always follows), so it would only ever matter here as TS's *type*-position `void` (a
    // function/arrow return-type annotation, `(): void` / `=> void`), which genuinely IS the
    // last token before a statement boundary and must not block semicolon insertion there
    private static final Set<String> CONTINUATION_KEYWORDS = new HashSet<>( Arrays.asList(
        "typeof",
        "new",
        "in",
        "instanceof",
        "else",
        "try",
        "finally",
        "do",
        "case",
        "default",
        "extends",
        "implements",
        "delete",
        "of",
        "as",
        "from"
    ) );

    /**
     * STYLE_JS_TS.md §2: always insert an explicit semicolon at the end of a statement, never
     * rely on ASI. Statement boundaries are found via a depth counter that increments across
     * `(`/`[` and any `{` classified as an *expression* brace (arrow-function body, object
     * literal, etc. -- {@link #isExpressionBrace}), but resets to a fresh 0 across a `{`
     * classified as a *statement* brace (function/class/control-flow body) -- so a `return`
     * inside a deeply-nested method body is evaluated at depth 0 relative to its own immediately
     * enclosing block, not the file's absolute brace nesting. A statement boundary is reached
     * either at a depth-0 NEWLINE or right before a depth-0 statement-brace's own closing `}`;
     * whether a semicolon is actually needed there is decided by {@link #needsSemicolonAfter}.
     * Conservative posture matching every other pass in this codebase: any candidate boundary
     * touching a frozen token is left completely untouched.
     */
    public String enforceSemicolonInsertion(final List<Token> tokens)
    {
        final Map<Integer, Integer> braceOpenToClose    = matchBraces(tokens);
        final Map<Integer, Boolean> braceResetDepth     = new HashMap<>();
        final Map<Integer, Boolean> braceNeedsSemicolon = new HashMap<>();
        classifyBraces(tokens, braceOpenToClose, braceResetDepth, braceNeedsSemicolon);
        final Map<Integer, Integer> parenOpenToClose = matchParens(tokens);
        final Map<Integer, Integer> parenCloseToOpen = new HashMap<>();
        for( final Map.Entry<Integer, Integer> e : parenOpenToClose.entrySet() ) parenCloseToOpen.put(
            e.getValue(), e.getKey()
        );
        final Map<Integer, Integer> braceCloseToOpen = new HashMap<>();
        for( final Map.Entry<Integer, Integer> e : braceOpenToClose.entrySet() ) braceCloseToOpen.put(
            e.getValue(), e.getKey()
        );

        final Map<Integer, String> overrides  = new HashMap<>();
        final Deque<Integer>       depthStack = new ArrayDeque<>();
              int                  depth      = 0;
              int                  lastSigIdx = -1;
        final int                  n          = tokens.size();

        for(int i = 0; i < n; ++i) {
            final Token t = tokens.get(i);

            if(t.type == TokenType.NEWLINE) {
                if(depth == 0 && lastSigIdx >= 0) maybeInsertSemicolon(
                    tokens,
                    lastSigIdx,
                    i,
                    braceNeedsSemicolon,
                    braceCloseToOpen,
                    parenCloseToOpen,
                    overrides
                );
                continue;
            } // if
            if( isGapToken(t) ) continue;

            if( isPunct(
                t, "}"
            ) && depth == 0 && Boolean.TRUE.equals(
                braceResetDepth.get( braceCloseToOpen.get(i) )
            )
                    && lastSigIdx >= 0 ) {
                // Statement-body brace about to close at depth 0 with no trailing NEWLINE first
                // (a one-liner body, e.g. `{ return x }`) -- the boundary is right here, before
                // `}`. Covers both true statement blocks (function/class/control-flow, never need
                // a semicolon on `}` itself) and arrow-function block bodies (do need one).
                maybeInsertSemicolon(
                    tokens, lastSigIdx, i, braceNeedsSemicolon, braceCloseToOpen,
                    parenCloseToOpen, overrides
                );
            } // if

            if( isPunct(t, "(") || isPunct(t, "[") || t.type == TokenType.ANGLE_BRACKET_OPEN
                    || t.type == TokenType.TEMPLATE_HOLE_OPEN ) {
                // A template literal's `${...}` hole is a single expression, exactly like a `(...)`
                // group -- no statement boundary can occur inside it. Real-corpus dogfood
                // (STATE_JS_TS.md's Step 2 Increment 5, excalidraw's SearchMenu.tsx) found this was
                // missing: a NEWLINE right after `${` was seen at depth 0 (since TEMPLATE_HOLE_OPEN
                // wasn't tracked here at all), so a stray `;` got inserted right after `${` when the
                // hole's own expression (a multi-line ternary) wrapped onto its own line.
                depthStack.push(depth);
                ++depth;
            } // if
            else if( isPunct(
                t, ")"
            ) || isPunct(
                t, "]"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE || t.type == TokenType.TEMPLATE_HOLE_CLOSE ) {
                if( !depthStack.isEmpty() ) depth = depthStack.pop();
            }
            else if( isPunct(t, "{") ) {
                depthStack.push(depth);
                depth = Boolean.TRUE.equals( braceResetDepth.get(i) ) ? 0 : depth + 1;
            }
            else if( isPunct(t, "}") ) {
                if( !depthStack.isEmpty() ) depth = depthStack.pop();
            }
            lastSigIdx = i;
        } // for

        return render(tokens, overrides);
    }

    /**
     * Decides whether a semicolon is needed right after {@code lastSigIdx} (the last real
     *  content token of a candidate statement), and if so records an override appending `;` to
     *  that token's own text. {@code boundaryIdx} is the NEWLINE or closing-`}` token that
     *  triggered the check -- used only to look ahead for a following `{` (control-flow/
     *  function/class header continuation) and to bail out on a frozen span.
     */
    private void maybeInsertSemicolon(
        final List<Token>           tokens,
        final int                   lastSigIdx,
        final int                   boundaryIdx,
        final Map<Integer, Boolean> braceNeedsSemicolon,
        final Map<Integer, Integer> braceCloseToOpen,
        final Map<Integer, Integer> parenCloseToOpen,
        final Map<Integer, String>  overrides
    )
    {
        final Token lastSig = tokens.get(lastSigIdx);
        if( lastSig.frozen || tokens.get(boundaryIdx).frozen ) return;
        if( overrides.containsKey(
            lastSigIdx
        ) ) return; // Already handled (e.g. both a `}` boundary and a following NEWLINE fired)
        // If the next real content across the boundary is `{`, this is a control-flow/function/
        // class header continuing onto its own-line Allman brace -- never a statement end
        if( tokens.get(boundaryIdx).type == TokenType.NEWLINE ) {
            final int nextSig = nextSignificantIndex(tokens, boundaryIdx + 1);
            if( nextSig >= 0 && isPunct( tokens.get(nextSig), "{" ) ) return;
            // STYLE_JS_TS.md §11.1: a union/intersection type may wrap break-before-operator
            // (`type Y = A\n | B\n | C;`), leading `|`/`&` on the next line -- that's still the
            // same statement continuing, not a new one, even though the current line's own last
            // token (`A`) isn't itself a trailing continuation operator.
            if( nextSig >= 0 && ( isOp(
                tokens.get(nextSig), "|"
            ) || isOp(
                tokens.get(nextSig), "&"
            ) ) ) return;
            // Leading-operator continuation style: method chaining (`.get()\n.expect()`),
            // ternary break-before-operator (`cond\n  ? x\n  : y`), logical-operator chains,
            // etc. -- none of these operators can legally begin a new statement, so their
            // presence as the very next significant token means the previous line's statement
            // isn't actually finished, regardless of its own last token.
            if( nextSig >= 0 && tokens.get(
                nextSig
            ).type == TokenType.OP && LEADING_CONTINUATION_OPS.contains(
                tokens.get(nextSig).text
            ) ) return;
            // Comma-first declarator-list style (`var a = 1\n  , b = 2`) -- a leading `,` can
            // never begin a new statement either
            if( nextSig >= 0 && isPunct( tokens.get(nextSig), "," ) ) return;
            // `class`/`interface` header wrapping its own `extends`/`implements` clause onto the
            // next line (`export interface ParserOptions\n  extends ErrorHandlingOptions {`) --
            // a leading `extends`/`implements` can never begin a new statement, so the previous
            // line's declaration name isn't actually finished yet (vuejs/core dogfood,
            // `compiler-core/src/options.ts`).
            if( nextSig >= 0 && tokens.get(
                nextSig
            ).type == TokenType.KEYWORD && ( "extends".equals(
                tokens.get(nextSig).text
            ) || "implements".equals(
                tokens.get(nextSig).text
            ) ) ) return;
        } // if
        if( needsSemicolonAfter(
            tokens, lastSigIdx, braceNeedsSemicolon, braceCloseToOpen, parenCloseToOpen
        ) ) overrides.put(
            lastSigIdx, lastSig.text + ";"
        );
    }

    private boolean needsSemicolonAfter(
        final List<Token>           tokens,
        final int                   idx,
        final Map<Integer, Boolean> braceNeedsSemicolon,
        final Map<Integer, Integer> braceCloseToOpen,
        final Map<Integer, Integer> parenCloseToOpen
    )
    {
        final Token t = tokens.get(idx);
        if( isPunct(
            t, ";"
        ) || isPunct(
            t, "{"
        ) || isPunct(
            t, ","
        ) || isPunct(
            t, ":"
        ) || isPunct(
            t, "("
        ) || isPunct(
            t, "["
        ) ) return false;
        if(t.type == TokenType.TEMPLATE_HOLE_OPEN) return false;
        if( t.type == TokenType.OP && CONTINUATION_OPS.contains(t.text) ) return false;
        if( t.type == TokenType.KEYWORD && CONTINUATION_KEYWORDS.contains(t.text) ) return false;
        if( endsWithDecoratorApplication(tokens, idx, parenCloseToOpen) ) return false;
        if( isPunct(t, "}") ) {
            final Integer openIdx = braceCloseToOpen.get(idx);
            return openIdx != null && Boolean.TRUE.equals( braceNeedsSemicolon.get(openIdx) );
        }

        return true;
    }

    /**
     * True if {@code idx} is the closing `)` of a decorator's own argument list
     *  (`@Name(...)`), or {@code idx} is a bare decorator name with no argument list
     *  (`@Name`) -- either way, this token ends a decorator application, not a statement, and a
     *  trailing semicolon must never be inserted after it.
     */
    private boolean endsWithDecoratorApplication(
        final List<Token>           tokens,
        final int                   idx,
        final Map<Integer, Integer> parenCloseToOpen
    )
    {
        final Token t = tokens.get(idx);
        if( isPunct(t, ")") ) {
            final Integer openParenIdx = parenCloseToOpen.get(idx);
            if(openParenIdx == null) return false;
            final int nameIdx = prevSignificantIndex(tokens, openParenIdx - 1);
            return isDecoratorName(tokens, nameIdx);
        }
        if(t.type == TokenType.IDENTIFIER) return isDecoratorName(tokens, idx);

        return false;
    }

    /**
     * True if the token at {@code nameIdx} is an identifier immediately (tight, no gap other
     *  than the `@` itself) preceded by `@` -- a decorator's own name
     */
    private boolean isDecoratorName(final List<Token> tokens, final int nameIdx)
    {
        if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) return false;
        final int atIdx = prevSignificantIndex(tokens, nameIdx - 1);

        return atIdx >= 0 && isOp( tokens.get(atIdx), "@" );
    }

    /**
     * Classifies every `{` in {@code openToClose}'s key set along two independent axes, based on
     * the nearest significant token immediately preceding the `{`:
     * <ul>
     * <li>{@code resetDepth} (into {@code outResetDepth}) -- true if the brace's own body is a
     * list of *statements* (needs its own depth-0 frame so a `return`/expression statement inside
     * it is evaluated for its own semicolon) rather than a comma-separated list of *values*
     * (object-literal properties, array elements, destructuring targets -- never individually
     * semicolon-terminated). True for every construct that isn't a value-shaped brace (function/
     * method/class/interface/control-flow bodies) *and* for an arrow-function block body (`=>
     * {`) specifically, since unlike every other value-preceded brace, an arrow body's own
     * contents are statements, not comma-separated values.
     * <li>{@code needsSemicolon} (into {@code outNeedsSemicolon}) -- true if this brace is itself
     * a *value* (assigned, returned, passed as an argument, etc.) whose own closing `}` can be
     * the tail of a statement needing a trailing `;` (object literal, array-pattern default,
     * arrow-function body assigned to something) -- false for a bare definition header
     * (function/class/interface/enum/control-flow) whose closing `}` never takes a semicolon.
     * </ul>
     * Defaults to "not a value" when the preceding token doesn't match any of the recognized
     * value-starting shapes -- same conservative "don't guess, assume the common case" posture as
     * the rest of this file.
     */
    private void classifyBraces(
        final List<Token>           tokens,
        final Map<Integer, Integer> openToClose,
        final Map<Integer, Boolean> outResetDepth,
        final Map<Integer, Boolean> outNeedsSemicolon
    )
    {
        for( final Integer openIdx : openToClose.keySet() ) {
            final int     prevIdx             = prevSignificantIndex(tokens, openIdx - 1);
            final Token   prev                = prevIdx >= 0 ? tokens.get(prevIdx) : null;
            final boolean isImportBraceHeader = prev != null && ( ( prev.type == TokenType.KEYWORD && "import".equals(
                prev.text
            ) ) || ( "type".equals(
                prev.text
            ) && isKeywordAt(
                tokens, prevSignificantIndex(tokens, prevIdx - 1), "import"
            ) ) );
            if(isImportBraceHeader) {
                // §15: a named-import list's `{ ... }` (e.g. `import { Widget } from "...";`) is
                // neither a statement-body brace (its interior is a comma-separated specifier
                // list, not statements -- must not reset depth to 0, or an interior comma-less
                // last specifier gets a bogus `;` inserted before `}`) nor is its own closing `}`
                // ever the tail of a semicolon-needing statement (the real statement terminator
                // comes after the following `from "path"` clause, not right after `}`).
                outResetDepth.put(openIdx, false);
                outNeedsSemicolon.put(openIdx, false);
                continue;
            } // if
            final boolean isExportBraceHeader = prev != null && ( ( prev.type == TokenType.KEYWORD && "export".equals(
                prev.text
            ) ) || ( "type".equals(
                prev.text
            ) && isKeywordAt(
                tokens, prevSignificantIndex(tokens, prevIdx - 1), "export"
            ) ) );
            if(isExportBraceHeader) {
                // Same "comma-separated specifier list, not statements" shape as an import brace
                // header, above -- must not reset depth to 0 either (vuejs/core dogfood, single-
                // specifier one-liner `export { baseCompile } from './compile'` wrongly got a
                // bogus `;` inserted before its own `}`). Unlike an import (which always has a
                // trailing `from "path"` clause), a re-export (`export { Foo } from "./mod"`) and
                // a plain named export (`export { Foo, Bar };`) are both legal -- only the former
                // has its real statement terminator after the `from` clause; the latter's own `}`
                // really is the statement's last token and does need a trailing `;`.
                final Integer closeIdx      = openToClose.get(openIdx);
                final int     afterClose    = closeIdx != null ? nextSignificantIndex(
                    tokens, closeIdx + 1
                ) : -1;
                final boolean hasFromClause = afterClose >= 0 && tokens.get(
                    afterClose
                ).type == TokenType.KEYWORD && "from".equals(
                    tokens.get(afterClose).text
                );
                outResetDepth.put(openIdx, false);
                outNeedsSemicolon.put(openIdx, !hasFromClause);
                continue;
            } // if
            if( lang.isTs && isTypeAliasObjectBrace(tokens, openIdx) ) {
                // §14: `type X = { ... };`'s object-shaped body is a `;`-separated property-
                // signature list (interface-shaped), not a comma-separated object-literal value
                // list -- must reset depth to 0 like any statement-body brace so each member's own
                // trailing NEWLINE is checked for a missing `;` (this brace is preceded by `=`,
                // which the generic isValue check below would otherwise classify as an ordinary
                // object-literal value brace, same misclassification `enforceInterfaceTypeAliasMemberColonAlignment`
                // guards against via this same helper). Unlike an `interface`/enum body, the whole
                // `type X = { ... }` declaration is itself a statement needing its own trailing
                // `;` after `}` -- so needsSemicolon is true here, opposite of the IFACE case below.
                outResetDepth.put(openIdx, true);
                outNeedsSemicolon.put(openIdx, true);
                continue;
            } // if
            if( isEnumBodyBrace(tokens, openIdx) ) {
                // §12: a TS enum body's `{ ... }` is a comma-separated member list, not a
                // statement list (must not reset depth to 0, or a comma-less trailing member gets
                // a bogus `;` inserted before `}` -- the same class of bug Checkpoint 16 already
                // fixed for import brace headers, above), and its own closing `}` is never the
                // tail of a semicolon-needing statement (an enum declaration takes no trailing
                // `;` at all, per STYLE_JS_TS.md §12's own text).
                outResetDepth.put(openIdx, false);
                outNeedsSemicolon.put(openIdx, false);
                continue;
            } // if
            final boolean isArrowBody = isOp(prev, "=>");
            final boolean isValue     = prev != null && ( isArrowBody || isOp(
                prev, "="
            ) || isPunct(
                prev, "("
            ) || isPunct(
                prev, "["
            ) || isPunct(
                prev, ","
            ) || isOp(
                prev, ":"
            ) || isOp(
                prev, "??"
            ) || isOp(
                prev, "||"
            ) || isOp(
                prev, "&&"
            ) || isOp(
                prev, "?"
            ) || isOp(
                prev, "..."
            ) || prev.type == TokenType.ANGLE_BRACKET_OPEN || ( lang.isTs && ( isOp(
                prev, "|"
            ) || isOp(
                prev, "&"
            ) ) ) || ( prev.type == TokenType.KEYWORD && ( "return".equals(
                prev.text
            ) || "yield".equals(
                prev.text
            ) || "throw".equals(
                prev.text
            ) || "typeof".equals(
                prev.text
            ) || "const".equals(
                prev.text
            ) || "let".equals(
                prev.text
            ) || "var".equals(
                prev.text
            ) ) ) || isLegacyCastBrace(
                tokens, openIdx
            ) );
            outResetDepth.put(openIdx, !isValue || isArrowBody);
            outNeedsSemicolon.put(openIdx, isValue);
        } // for
    }

    /**
     * True iff {@code idx} is a valid index into {@code tokens} and that token is the KEYWORD
     *  {@code text} -- small guard used by {@link #classifyBraces}'s `import type {` lookback so
     *  a negative "not found" index from {@link #prevSignificantIndex} doesn't need its own
     *  bounds check at every call site
     */
    private boolean isKeywordAt(final List<Token> tokens, final int idx, final String text)
    {
        return idx >= 0 && tokens.get(
            idx
        ).type == TokenType.KEYWORD && text.equals(
            tokens.get(idx).text
        );
    }

    /**
     * True iff the `{` at {@code braceIdx} opens a TS enum body -- i.e. it is directly preceded
     *  by an IDENTIFIER (the enum's own name) whose own preceding significant token is the `enum`
     *  KEYWORD (TS has no `extends`/generic clause on an enum name, unlike class/interface, so no
     *  intervening-clause walk is needed the way Java's own {@code isEnumBodyBrace} needs one).
     *  `const enum Name { ... }` is covered too -- the `const` modifier sits before `enum`, outside
     *  this check's own two-token lookback, so it doesn't need to be inspected at all.
     */
    private boolean isEnumBodyBrace(final List<Token> tokens, final int braceIdx)
    {
        final int nameIdx = prevSignificantIndex(tokens, braceIdx - 1);
        if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) return false;

        return isKeywordAt( tokens, prevSignificantIndex(tokens, nameIdx - 1), "enum" );
    }

    private Map<Integer, Integer> matchBraces(final List<Token> tokens)
    {
        final Map<Integer, Integer> openToClose = new HashMap<>();
        final Deque<Integer>        stack       = new ArrayDeque<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
                 if( isPunct(t, "{") )                     stack.push(i);
            else if( isPunct(t, "}") && !stack.isEmpty() ) openToClose.put( stack.pop(), i );
        }

        return openToClose;
    }

    private Map<Integer, Integer> matchParens(final List<Token> tokens)
    {
        final Map<Integer, Integer> openToClose = new HashMap<>();
        final Deque<Integer>        stack       = new ArrayDeque<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
                 if( isPunct(t, "(") ) stack.push(i);
            else if( isPunct(t, ")") && !stack.isEmpty() ) openToClose.put( stack.pop(), i );
        }

        return openToClose;
    }

    private int nextSignificantIndex(final List<Token> tokens, final int from)
    {
        for( int i = from; i < tokens.size(); ++i ) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    private int prevSignificantIndex(final List<Token> tokens, final int from)
    {
        for(int i = from; i >= 0; --i) {
            if( !isGapToken( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    private String render(final List<Token> tokens, final Map<Integer, String> overrides)
    {
        final StringBuilder out = new StringBuilder();
        for( int i = 0; i < tokens.size(); ++i ) {
            final String override = overrides.get(i);
            out.append( override != null ? override : tokens.get(i).text );
        }

        return out.toString();
    }

    // ── §3 Spread / rest (`...`) tight spacing ───────────────────────────────────────
    /**
     * STYLE_JS_TS.md §3: spread/rest `...` never has a space after it -- tight against the
     * identifier/expression it precedes (`...rest`, `...items`, `{...defaults, ...overrides}`).
     * One-sided (unlike §7's `?.`/`??`, which are tight/spaced on both sides): JS/TS's `...` is
     * always a prefix operator, so only the gap immediately following it is ever touched -- the
     * gap immediately before it (after `(`/`,`/`{`/`[`) is ordinary call/literal spacing, already
     * governed by whatever already renders those contexts, and is left alone here. No general
     * expression-level operator-respacing pass already covers this (same reasoning as Kotlin's
     * `enforceNullSafetyOperatorSpacing` javadoc), so this is its own flat single-pass scan.
     * Conservative bailout matching every other pass in this file: a gap containing a comment, a
     * NEWLINE, or a frozen token is left completely untouched.
     */
    public String enforceSpreadRestSpacing(final List<Token> tokens)
    {
        final StringBuilder out             = new StringBuilder();
        final List<Token>   gap             = new ArrayList<>();
              Token         lastSignificant = null;
        final int           n               = tokens.size();
              int           i               = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean gapBlocked  = gap.stream().anyMatch(
                g->isComment(g) || g.type == TokenType.NEWLINE || g.frozen
            ) || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean afterSpread = lastSignificant != null && isOp(lastSignificant, "...");

            if(gapBlocked || !afterSpread) {
                for(final Token g : gap) out.append(g.text);
            }
            // AfterSpread && !gapBlocked: gap dropped entirely, nothing appended

            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    // ── §7 Optional chaining / nullish coalescing ────────────────────────────────────
    /**
     * STYLE_JS_TS.md §7: `?.` is tight (no surrounding space), `??` is spaced like a normal binary
     * operator -- direct analog of Kotlin's `?.`/`?:` treatment
     * ({@link KotlinSpecificRule#enforceNullSafetyOperatorSpacing}), reused here structurally with
     * JS/TS's own operator names. No general expression-level operator-respacing pass already
     * covers this, so it's its own flat pass, same conservative bailout (a gap containing a
     * comment, a NEWLINE, or a frozen token is left untouched).
     */
    public String enforceOptionalChainingSpacing(final List<Token> tokens)
    {
        final StringBuilder out             = new StringBuilder();
        final List<Token>   gap             = new ArrayList<>();
              Token         lastSignificant = null;
        final int           n               = tokens.size();
              int           i               = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean gapBlocked        = gap.stream().anyMatch(
                g->isComment(g) || g.type == TokenType.NEWLINE || g.frozen
            ) || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean adjacentToTightOp = isOptionalChain(
                lastSignificant
            ) || isOptionalChain(
                t
            );
            final boolean adjacentToNullish = !adjacentToTightOp && ( isNullishCoalesce(
                lastSignificant
            ) || isNullishCoalesce(
                t
            ) );

            if( gapBlocked || (!adjacentToTightOp && !adjacentToNullish) ) {
                for(final Token g : gap) out.append(g.text);
            }
            else if(adjacentToNullish) {
                out.append(' ');
            }
            // AdjacentToTightOp && !gapBlocked: gap dropped entirely, nothing appended

            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    private boolean isOptionalChain(final Token t)
    {
        return t != null && isOp(t, "?.");
    }

    private boolean isNullishCoalesce(final Token t)
    {
        return t != null && ( isOp(t, "??") || isOp(t, "??=") );
    }

    // ── §4 Template literal `${...}` interpolation spacing ───────────────────────────
    /**
     * STYLE_JS_TS.md §4: a template literal's raw text is preserved exactly as written (already
     * satisfied by the tokenizer emitting the whole backtick literal, including every {@code
     * ${...}} interpolation, as one opaque STRING token -- see {@code
     * TokenizerCurly.emitTemplateLiteral}'s own javadoc), but each {@code ${...}} interpolation's
     * *interior expression* gets normal expression spacing (STYLE.md §3.1), same as Kotlin's own
     * {@code ${...}} string-template interpolation is described as an analog for (though Kotlin's
     * own interpolation is, in this codebase, *also* left opaque -- there is no existing precedent
     * to reuse here, this is new territory). Approach: find every top-level {@code ${...}} span in
     * the literal's raw text (a hand-rolled scanner mirroring the tokenizer's own {@code
     * skipTemplateInterpolation} nesting rules -- brace depth, nested quoted strings, and a nested
     * backtick template treated as one opaque quoted span, not reformatted itself this pass), then
     * for each span: re-tokenize just the interior substring in isolation via a fresh {@code
     * TokenizerCurly} for the same language, and re-join its significant tokens via {@code
     * MiscRuleCurly.renderTokens} (accessible here since {@code protected} + same package,
     * {@code com.jxmake.formatter.rules}) -- the same generic tight/loose token-adjacency spacing
     * every other rendering path in this codebase already uses (operators, calls, generics, etc.),
     * with no new spacing logic invented for this pass. Conservative bailout, matching every other
     * pass in this file: a span containing a NEWLINE or comment token (multi-line or commented
     * interpolation), a frozen token, or a tokenizer-rejected fragment is left byte-for-byte
     * untouched rather than guessed at. A nested template literal inside an interpolation
     * (`` `${`inner ${x}`}` ``) is treated as opaque quoted text for span-finding purposes (its own
     * outer `${...}` delimiters aren't re-scanned as part of the enclosing literal's own span list),
     * but its interior IS still recursively reformatted -- {@link #reformatInterpolationInterior}
     * re-runs this same {@code rewriteTemplateLiteral} on any nested backtick STRING token found
     * among its own significant tokens before final re-joining, so nesting of any depth is handled.
     */
    public String enforceTemplateLiteralInterpolationSpacing(final List<Token> tokens)
    {
        if(!lang.isJs && !lang.isTs) return render( tokens, new HashMap<>() );
        final Map<Integer, String> overrides      = new HashMap<>();
        final TokenizerCurly       innerTokenizer = new TokenizerCurly(lang);
        final MiscRuleCurly        misc           = new MiscRuleCurly(lang, false, false);
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( t.type == TokenType.STRING && !t.frozen && t.text.length() >= 2
                    && t.text.charAt(0) == '`' ) {
                final String rewritten = rewriteTemplateLiteral(t.text, innerTokenizer, misc);
                if( rewritten != null && !rewritten.equals(t.text) ) overrides.put(i, rewritten);
            }
            // `.jsx`/`.tsx` only (STATE_JS_TS.md's 2026-08-13 scoping session, sub-context 0/1
            // implementation): a template literal on this extension is no longer one opaque
            // STRING token (TokenizerCurly.emitTemplateLiteralSegmented splits it so a `${`-opened
            // hole's interior is real tokens, letting findJsxSpans detect JSX inside a hole) -- the
            // text-based rewriteTemplateLiteral path above never matches a segmented literal's own
            // partial STRING segments, so without this branch a `.tsx`/`.jsx` hole's interpolation-
            // spacing normalization (this method's whole purpose) would silently stop firing for
            // every such file, a real regression found empirically (not predicted by the scoping
            // session's static trace) once JSX-in-hole detection was implemented and manually
            // verified against a `${a+b}` case. Token-based instead of text-based since the hole's
            // interior is already real tokens here -- simpler than re-tokenizing a substring.
            else if(t.type == TokenType.TEMPLATE_HOLE_OPEN) {
                final int closeIdx = findMatchingTemplateHoleClose(tokens, i);
                if(closeIdx > i) {
                    final String rendered = renderTemplateHoleInterior(
                        tokens.subList(i + 1, closeIdx), misc
                    );
                    if(rendered != null) {
                        overrides.put(i + 1, rendered);
                        for(int k = i + 2; k < closeIdx; ++k) overrides.put(k, "");
                    }
                    i = closeIdx; // Skip past the whole hole -- nested holes inside it were
                                  // Already folded into `rendered` by renderTemplateHoleInterior's
                                  // own recursion, not independently reprocessed by this loop
                } // if
            }
        } // for

        return render(tokens, overrides);
    }

    /**
     * Finds the {@code TEMPLATE_HOLE_CLOSE} matching the {@code TEMPLATE_HOLE_OPEN} at
     *  {@code openIdx}, tracking nesting depth so a nested template literal's own hole pair inside
     *  doesn't prematurely match. Returns -1 if unbalanced (defensive only -- the tokenizer itself
     *  guarantees balanced pairs by construction; never expected to fire).
     */
    private int findMatchingTemplateHoleClose(final List<Token> tokens, final int openIdx)
    {
        int depth = 1;
        for( int i = openIdx + 1; i < tokens.size(); ++i ) {
            final TokenType ty = tokens.get(i).type;
            if(ty == TokenType.TEMPLATE_HOLE_OPEN) ++depth;
            else if(ty == TokenType.TEMPLATE_HOLE_CLOSE) {
                --depth;
                if(depth == 0) return i;
            }
        } // for

        return -1;
    }

    /**
     * Re-renders a segmented template-literal hole's already-tokenized interior (the slice
     *  strictly between its {@code TEMPLATE_HOLE_OPEN}/{@code TEMPLATE_HOLE_CLOSE} pair) via
     *  {@code renderTokens}'s ordinary tight/loose adjacency rules -- the token-based counterpart
     *  of {@link #reformatInterpolationInterior}'s text-based version, used for `.jsx`/`.tsx`
     *  files where the hole's interior is real tokens rather than raw substring text. Same
     *  conservative bailout as the text-based path: returns {@code null} (caller leaves the
     *  original interior untouched) if empty/blank, or if any significant token is a NEWLINE,
     *  comment, or frozen (a {@code JSX_SPAN} is frozen by design -- {@code true} to preserve its
     *  raw text byte-for-byte, but that is a deliberate keep-as-is, not a bailout reason, so it is
     *  explicitly exempted from the frozen check here). A bare nested {@code TEMPLATE_HOLE_OPEN}
     *  found directly in the slice (STATE_JS_TS.md sub-context 2) is recursively reformatted via
     *  this same method and spliced back in as one synthetic {@code ${...}} STRING token, mirroring
     *  {@link #reformatInterpolationInterior}'s own recursive-nesting structure. A nested template
     *  *literal* (its own STRING/HOLE_OPEN/.../STRING segment chain, opening backtick to closing
     *  backtick) is folded whole into a single synthetic STRING token spanning the entire literal
     *  -- its own segments are never pushed into the significant-token list individually, since
     *  {@code renderTokens} spaces adjacent STRING tokens apart as if they were two separate value
     *  expressions, which corrupted (and on repeated passes, snowballed) the nested literal's raw
     *  text; folding it into one token sidesteps that entirely.
     */
    private String renderTemplateHoleInterior(final List<Token> interior, final MiscRuleCurly misc)
    {
        final List<Token> significant = new ArrayList<>();
              int         i           = 0;
        while( i < interior.size() ) {
            final Token t = interior.get(i);
            if( t.type == TokenType.NEWLINE || isComment(t) ) return null;
            if(t.frozen && t.type != TokenType.JSX_SPAN) return null;
            if(t.type == TokenType.WHITESPACE) { ++i; continue; }
            if( t.type == TokenType.STRING && !t.text.isEmpty() && t.text.charAt(0) == '`' ) {
                // Start of a nested template literal's segment chain (STRING, [HOLE_OPEN,
                //  interior, HOLE_CLOSE, STRING]*). Earlier this folded each of its own
                //  TEMPLATE_HOLE_OPEN spans into one synthetic STRING but still pushed the
                //  literal's own surrounding STRING segments into `significant` as separate
                //  tokens -- renderTokens then treated adjacent STRING tokens as two distinct
                //  value expressions needing a separating space, corrupting (and, on repeated
                //  passes, snowballing) the literal's own raw text. Fixed by folding the WHOLE
                //  nested literal -- every one of its own segments and holes, recursively -- into
                //  a single synthetic STRING token before it ever reaches `significant`, so it
                //  participates in adjacency decisions exactly as it would have as one token if
                //  the tokenizer had never segmented it (found via a non-idempotent round-trip
                //  probe on `a ${ `b ${x+1}` } d`, STATE_JS_TS.md sub-context 2).
                final StringBuilder combined = new StringBuilder(t.text);
                      boolean       closed   = t.text.length() >= 2 && t.text.charAt(
                          t.text.length() - 1
                      ) == '`';
                ++i;
                while(!closed) {
                    if( i >= interior.size() || interior.get(
                        i
                    ).type != TokenType.TEMPLATE_HOLE_OPEN ) return null; // Malformed/unexpected shape -- bail defensively
                    final int closeIdx = findMatchingTemplateHoleClose(interior, i);
                    if(closeIdx < 0) return null; // Unbalanced -- should be unreachable, bail defensively
                    final String        nested = renderTemplateHoleInterior(
                        interior.subList(i + 1, closeIdx), misc
                    );
                    final StringBuilder raw    = new StringBuilder();
                    for(int k = i + 1; k < closeIdx; ++k) raw.append( interior.get(k).text );
                    combined.append(
                        "${"
                    ).append(
                        nested != null ? nested : raw.toString()
                    ).append(
                        '}'
                    );
                    i = closeIdx + 1;
                    if( i >= interior.size() || interior.get(
                        i
                    ).type != TokenType.STRING ) return null; // Malformed/unexpected shape -- bail defensively
                    final Token seg = interior.get(i);
                    combined.append(seg.text);
                    closed = !seg.text.isEmpty() && seg.text.charAt( seg.text.length() - 1 ) == '`';
                    ++i;
                } // while !closed
                significant.add(
                    new Token( TokenType.STRING, combined.toString(), t.braceDepth, t.parenDepth, null )
                );
                continue;
            } // if
            if(t.type == TokenType.TEMPLATE_HOLE_OPEN) {
                final int closeIdx = findMatchingTemplateHoleClose(interior, i);
                if(closeIdx < 0) return null; // Unbalanced -- should be unreachable, bail defensively
                final String        nested = renderTemplateHoleInterior(
                    interior.subList(i + 1, closeIdx), misc
                );
                final StringBuilder raw    = new StringBuilder();
                for(int k = i + 1; k < closeIdx; ++k) raw.append( interior.get(k).text );
                final Token synthetic = new Token(
                    TokenType.STRING,
                    "${" + ( nested != null ? nested : raw.toString() ) + "}",
                    t.braceDepth,
                    t.parenDepth,
                    null
                );
                significant.add(synthetic);
                i = closeIdx + 1;
                continue;
            } // if
            if(t.type == TokenType.TEMPLATE_HOLE_CLOSE) return null; // Unreachable, defensive only
            significant.add(t);
            ++i;
        } // while
        if( significant.isEmpty() ) return null;

        return misc.renderTokens(significant);
    }

    /**
     * Finds every top-level `${...}` span in {@code text} (a whole backtick-delimited template
     *  literal, opening/closing backtick included) and re-renders each interior expression via
     *  {@code renderTokens}, splicing the results back into the literal's raw text. Returns
     *  {@code text} unchanged if there are no interpolations, or if any span's own interior fails
     *  its conservative reformat check (see {@link #reformatInterpolationInterior}).
     */
    private String rewriteTemplateLiteral(
        final String         text,
        final TokenizerCurly innerTokenizer,
        final MiscRuleCurly  misc
    )
    {
        final List<int[]> spans = findInterpolationSpans(text);
        if( spans.isEmpty() ) return text;
        final StringBuilder out  = new StringBuilder();
              int           last = 0;
        for( final int[] span : spans ) {
            final int start = span[0];
            final int end   = span[1];
            out.append(text, last, start);
            final String interior  = text.substring(start, end);
            final String rewritten = reformatInterpolationInterior(interior, innerTokenizer, misc);
            out.append(rewritten != null ? rewritten : interior);
            last = end;
        } // for
        out.append( text.substring(last) );

        return out.toString();
    }

    /**
     * Scans a template literal's raw text (opening/closing backtick included) for every
     *  top-level `${...}` interpolation, returning each as a {@code [interiorStart, interiorEnd)}
     *  index pair (excluding the `${`/`}` delimiters themselves). Mirrors {@code
     *  TokenizerCurly.skipTemplateInterpolation}'s nesting rules: `{`/`}` depth counting, with
     *  nested `"`/`'`/`` ` `` quoted spans skipped as opaque units so an interior brace inside a
     *  string literal (or a nested template) doesn't corrupt depth counting.
     */
    private List<int[]> findInterpolationSpans(final String text)
    {
        final List<int[]> spans = new ArrayList<>();
        final int         end   = text.length() - 1; // Exclude the closing backtick
              int         pos   = 1;                 // Skip the opening backtick
        while(pos < end) {
            final char c = text.charAt(pos);
            if(c == '\\') {
                pos += 2;
                continue;
            }
            if( c == '$' && pos + 1 < end && text.charAt(pos + 1) == '{' ) {
                final int interiorStart = pos + 2;
                      int depth         = 1;
                      int p             = interiorStart;
                while(p < end && depth > 0) {
                    final char cc = text.charAt(p);
                    if(cc == '\\') {
                        p += 2;
                        continue;
                    }
                    else if(cc == '{') {
                        ++depth;
                        ++p;
                    }
                    else if(cc == '}') {
                        --depth;
                        ++p;
                    }
                    else if(cc == '"' || cc == '\'' || cc == '`') {
                        p = skipQuotedSpan(text, p, end, cc);
                    }
                    else {
                        ++p;
                    }
                } // while
                if(depth == 0) {
                    spans.add( new int[] { interiorStart, p - 1 } );
                }
                pos = p;
                continue;
            } // if
            ++pos;
        } // while

        return spans;
    }

    /**
     * Skips a quoted span (`"`/`'`/`` ` ``-delimited, {@code quote} is the delimiter char)
     *  starting at {@code p} (the opening delimiter itself), returning the index right after its
     *  closing delimiter -- or {@code boundIdx} if unterminated within that bound. A nested
     *  backtick template's own interior (including any of its own `${...}`) is treated as opaque
     *  by this same skip -- not reformatted, see this section's own javadoc scope note.
     */
    private int skipQuotedSpan(final String text, final int p, final int boundIdx, final char quote)
    {
        int i = p + 1;
        while(i < boundIdx) {
            final char c = text.charAt(i);
            if(c == '\\') {
                i += 2;
                continue;
            }
            if(c == quote) return i + 1;
            ++i;
        } // while

        return boundIdx;
    }

    /**
     * Re-tokenizes {@code interior} (a `${...}` interpolation's raw interior substring) in
     *  isolation and re-joins its significant tokens via {@code renderTokens}'s ordinary
     *  tight/loose adjacency rules. Returns {@code null} (caller leaves the original interior
     *  untouched) if the interior is empty/blank, contains a NEWLINE or comment token (multi-line
     *  or commented interpolation -- out of this flat pass's scope), or contains a frozen token.
     *  A nested backtick-delimited STRING token among the interior's own significant tokens (a
     *  template literal nested inside this interpolation, e.g. `` `${`inner ${x}`}` ``) is
     *  recursively reformatted via {@code rewriteTemplateLiteral} before re-joining, so nesting of
     *  any depth gets its own `${...}` spacing normalized, not just the outermost level.
     */
    private String reformatInterpolationInterior(
        final String         interior,
        final TokenizerCurly innerTokenizer,
        final MiscRuleCurly  misc
    )
    {
        if( interior.trim().isEmpty() ) return null;
        final List<Token> innerTokens;
        try {
            innerTokens = innerTokenizer.tokenize(interior);
        }
        catch(final RuntimeException e) {
            return null;
        }
        final List<Token> significant = new ArrayList<>();
        for(final Token t : innerTokens) {
            if( t.type == TokenType.NEWLINE || isComment(t) || t.frozen ) return null;
            if(t.type == TokenType.WHITESPACE) continue;
            if( t.type == TokenType.STRING && t.text.length() >= 2 && t.text.charAt(
                0
            ) == '`' ) t.text = rewriteTemplateLiteral(
                t.text, innerTokenizer, misc
            );
            significant.add(t);
        } // for
        if( significant.isEmpty() ) return null;

        return misc.renderTokens(significant);
    }

    // ── §5 Named function / class-method Allman brace style ──────────────────────────
    /**
     * STYLE_JS_TS.md §5: a named function declaration (`function foo() {}`) or class method
     * (`render() {}`, including getters/setters, `async`/`static`/generator `*` variants, and
     * constructors) moves its own `{` to its own line (Allman) -- mirrors
     * {@code JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle}'s role for Java, but
     * substantially simplified: JS/TS has no `throws` clause, no compact-constructor shape, and no
     * enum-constant-body false positive to guard against (JS/TS enums, §12, have no per-constant
     * body syntax at all).
     *
     * <p>Candidate signal: the `{`'s own header, walked backward from the `{`, must resolve to a
     * `)` whose matching `(` is itself immediately preceded by an IDENTIFIER (the function/method
     * name) -- this alone excludes every control-flow brace (`if`/`while`/`for`/`switch`/`catch`
     * precede their `(` with a KEYWORD, never an IDENTIFIER) and every anonymous function
     * expression (`function(...) {}`, whose `(` is preceded directly by the `function` keyword,
     * not a name). Between that `)` and the `{` there are two possible shapes: (a) directly
     * adjacent (plain JS, or a TS method/function with no return-type annotation), or (b) a TS
     * return-type annotation (`): Promise<Result> {`) -- {@link #findHeaderCloseParen} walks
     * backward from the `{` looking for a `:` immediately preceded by a `)`, so the return type's
     * own content (however complex -- generics, unions, etc.) is never inspected, only relocated
     * along with the brace.
     *
     * <p>An arrow function's block body (`=> {`) is excluded by construction -- its `{` is
     * directly preceded by `=>`, never by `)` or a `:`-return-type tail, so it never matches the
     * header-walk above at all; STYLE_JS_TS.md §6's K&R-for-arrow-block-bodies rule needs no
     * explicit exclusion check here.
     *
     * <p>Exceptions, all "stays K&R, not Allman", per §5's own worked-example text: (1) an
     * empty body (`{}`, nothing between the braces) -- {@link #isEmptyBody}; (2) a one-liner
     * whose entire `{ ... }` body sits on one physical line (covers getter/setter one-liner
     * groups and any other short one-liner method per STYLE.md §14's squeeze-onto-one-line shape,
     * without needing a getter/setter-specific check -- any one-liner method stays K&R the same
     * way) -- {@link #isSingleLineBraceBody}.
     *
     * <p>A `{` already on its own line (a NEWLINE already present in the gap between the header's
     * last token and the `{`) is left untouched -- idempotent. Any frozen token across the header
     * span is a conservative bailout, matching every other pass in this file.
     */
    public String enforceMethodDefinitionAllmanBraceStyle(final List<Token> tokens)
    {
        if(!lang.isJs && !lang.isTs) return render( tokens, new HashMap<>() );
        final Map<Integer, Integer> braceOpenToClose = matchBraces(tokens);
        final Map<Integer, Integer> parenOpenToClose = matchParens(tokens);
        final Map<Integer, Integer> overrides        = new HashMap<>();

        for( int i = 0; i < tokens.size(); ++i ) {
            if( !isPunct( tokens.get(i), "{" ) ) continue;
            final int lastHeaderTokenIdx = prevSignificantIndex(tokens, i - 1);
            if(lastHeaderTokenIdx < 0) continue;
            final int closeParenIdx = findHeaderCloseParen(tokens, lastHeaderTokenIdx);
            if(closeParenIdx < 0) continue;
            final Integer openParenIdx = findMatchingOpenParen(tokens, closeParenIdx);
            if(openParenIdx == null) continue;
            int beforeParenIdx = prevSignificantIndex(tokens, openParenIdx - 1);
            if( beforeParenIdx >= 0 && tokens.get(
                beforeParenIdx
            ).type == TokenType.ANGLE_BRACKET_CLOSE ) {
                beforeParenIdx = matchAngleOpenBackward(tokens, beforeParenIdx);
                if(beforeParenIdx >= 0) beforeParenIdx = prevSignificantIndex(
                    tokens, beforeParenIdx - 1
                );
            }
            final int nameIdx = beforeParenIdx;
            if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) continue;
            if( hasNewlineOrCommentBetween(tokens, lastHeaderTokenIdx, i) ) continue;
            if( MiscRuleCore.anyFrozen(tokens, nameIdx, i + 1) ) continue;
            final Integer closeBraceIdx = braceOpenToClose.get(i);
            if(closeBraceIdx == null) continue;
            if( isEmptyBody(
                tokens, i, closeBraceIdx
            ) || isSingleLineBraceBody(
                tokens, i, closeBraceIdx
            ) ) continue;
            overrides.put(
                lastHeaderTokenIdx, i
            ); // Marker: this token's gap-to-brace gets rewritten below
        } // for
        if( overrides.isEmpty() ) return render( tokens, new HashMap<>() );

        return renderAllmanBraceMoves(tokens, overrides, parenOpenToClose);
    }

    /**
     * Walks backward from {@code fromIdx} (the last significant token seen before a candidate
     *  `{`) to find the header's own closing `)`. If {@code fromIdx} itself is `)`, that's the
     *  direct-adjacency (plain JS, or TS with no return type) case. Otherwise looks for a `:`
     *  return-type-annotation tail whose own immediately-preceding significant token is `)` --
     *  the return type's own interior content between that `:` and {@code fromIdx} is never
     *  otherwise inspected. Returns -1 if neither shape is found before a statement-breaking
     *  token (`;`, `{`, `}`) is hit.
     */
    private int findHeaderCloseParen(final List<Token> tokens, final int fromIdx)
    {
        if( isPunct( tokens.get(fromIdx), ")" ) ) return fromIdx;
        int j     = fromIdx;
        int steps = 0;
        while(j >= 0 && steps < 500) {
            final Token t = tokens.get(j);
            if( isPunct(t, ";") || isPunct(t, "{") || isPunct(t, "}") ) return -1;
            if( isOp(t, ":") ) {
                final int before = prevSignificantIndex(tokens, j - 1);
                if( before >= 0 && isPunct( tokens.get(before), ")" ) ) return before;
                return -1;
            }
            j = prevSignificantIndex(tokens, j - 1);
            ++steps;
        } // while

        return -1;
    }

    /**
     * Index of the `<` matching the `>` at {@code closeIdx} (a generic type-parameter list's
     *  closing bracket), via local backward depth counting over the tokenizer's own
     *  {@code ANGLE_BRACKET_OPEN}/{@code ANGLE_BRACKET_CLOSE} reclassification (not raw `<`/`>`
     *  operator tokens, which would also match unrelated comparisons) -- structurally identical to
     *  {@code BlockStructureRule.matchAngleOpenBackward}, duplicated locally rather than shared
     *  since that method is private to its own class. Returns -1 if unmatched.
     */
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

    private Integer findMatchingOpenParen(final List<Token> tokens, final int closeParenIdx)
    {
        int depth = 0;
        for(int i = closeParenIdx; i >= 0; --i) {
            if( isPunct( tokens.get(i), ")" ) ) {
                ++depth;
            }
            else if( isPunct( tokens.get(i), "(" ) ) {
                --depth;
                if(depth == 0) return i;
            }
        } // for

        return null;
    }

    /**
     * True if {@code braceIdx}/{@code closeBraceIdx} delimit an empty body -- nothing (not even
     *  a comment) between the two braces
     */
    private boolean isEmptyBody(
        final List<Token> tokens,
        final int         braceIdx,
        final int         closeBraceIdx
    )
    {
        return nextSignificantIndex(tokens, braceIdx + 1) == closeBraceIdx
                && !hasNewlineOrCommentBetween(tokens, braceIdx, closeBraceIdx);
    }

    /**
     * True if no NEWLINE token appears anywhere between {@code braceIdx} and
     *  {@code closeBraceIdx} inclusive -- the whole body sits on one physical line
     */
    private boolean isSingleLineBraceBody(
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
     * Second pass: given the set of `{` indices approved for Allman conversion (keyed by the
     *  last header token that immediately precedes each, in {@code headerTokenToBrace}), rebuilds
     *  the token stream, replacing the gap right after each such header token with a NEWLINE plus
     *  the header line's own leading indentation, followed by the brace
     */
    private String renderAllmanBraceMoves(
        final List<Token>           tokens,
        final Map<Integer, Integer> headerTokenToBrace,
        final Map<Integer, Integer> parenOpenToClose
    )
    {
        final StringBuilder out = new StringBuilder();
              int           i   = 0;
        final int           n   = tokens.size();
        while(i < n) {
            out.append( tokens.get(i).text );
            final Integer braceIdx = headerTokenToBrace.get(i);
            if(braceIdx != null) {
                out.append('\n').append( lineIndent(tokens, i) );
                out.append( tokens.get(braceIdx).text );
                // Skip the original gap + `{` we just relocated.
                i = braceIdx + 1;
                continue;
            } // if
            ++i;
        } // while

        return out.toString();
    }

    /**
     * Reindents a leading multi-line block comment (captured verbatim from the source, so its
     *  continuation lines still carry whatever absolute indentation they had at the *original*
     *  code depth) to align under {@code indentPrefix} at the *new* depth this comment is being
     *  re-emitted at (e.g. by an alignment-grid rewrite that may reindent the code around it).
     *  Unlike {@link FormatterSimpleBraced#reindentBlockComment}, which only prepends
     *  {@code indentPrefix} in front of whatever whitespace a continuation line already has (correct
     *  when that line has none of its own, e.g. freshly generated text), this fully strips each
     *  continuation line's existing leading whitespace first and reconstructs
     *  {@code indentPrefix + " " + strippedLine} -- correct regardless of what depth the comment's
     *  interior lines were originally written at, and idempotent (re-running against already
     *  correctly-reindented text is a no-op). Single-line comments (no {@code '\n'}) pass through
     *  unchanged.
     */
    private static String reindentLeadingComment(
        final String commentText,
        final String indentPrefix
    )
    {
        if( commentText.indexOf('\n') < 0 ) return commentText;
        final String[]      lines = commentText.split("\n", -1);
        final StringBuilder sb    = new StringBuilder( lines[0] );
        for(int i = 1; i < lines.length; ++i) {
            int j = 0;
            while( j < lines[i].length() && Character.isWhitespace( lines[i].charAt(j) ) ) j++;
            sb.append('\n').append(indentPrefix).append(' ').append( lines[i].substring(j) );
        }

        return sb.toString();
    }

    /**
     * Line-leading whitespace of the physical line containing token {@code idx} -- {@code ""} if
     *  that line has no leading whitespace (column-0 start)
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
     * Rendered column (0-based, in characters) of the physical line up to but not including
     *  token {@code idx} -- i.e. how many characters of that line's own text precede it. Used to
     *  compute the alignment target column for continuation-line indentation.
     */
    private int lineColumnOf(final List<Token> tokens, final int idx)
    {
        int col = 0;
        for( int i = idx - 1; i >= 0 && tokens.get(
            i
        ).type != TokenType.NEWLINE; --i ) col += tokens.get(
            i
        ).text.length();

        return col;
    }

    // ── Step 2 "context 11" Increment 2: self-closing-tag attribute wrap ─────────────
    /**
     * STATE_JS_TS.md's Step 2 "context 11" scoping session, Increments 2-3 of the suggested
     *  5-increment breakdown: the actual wrap-decision function, applied to any root
     *  {@code JSX_SPAN} opening tag -- self-closing (Increment 2) or with children (Increment 3).
     *  Mirrors {@link MiscRuleCurly#enforceCallLineBreaking}'s decision-ladder shape (fits on one
     *  line -> no change; else one-attribute-per-line) without routing through that method's
     *  comma-split/typed-signature machinery, which doesn't fit JSX's whitespace-separated,
     *  comma-less attribute list (sub-context 3). Per sub-context 2's whitespace-significance
     *  hazard, only the opening tag itself (up through its own {@code >} / {@code />}) is ever
     *  rewritten -- everything from {@link Token#jsxOpeningTagEndOffset} onward (children plus the
     *  closing tag, when present) is spliced back in byte-for-byte untouched, never re-tokenized
     *  or re-rendered.
     */
    public String enforceJsxSelfClosingAttributeWrap(final List<Token> tokens)
    {
        final StringBuilder out = new StringBuilder();
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.JSX_SPAN) {
                final String wrapped = renderJsxSelfClosingWrapCandidate(tokens, i);
                out.append(wrapped != null ? wrapped : t.text);
                continue;
            }
            out.append(t.text);
        } // for

        return out.toString();
    }

    /**
     * Returns replacement text for the root {@code JSX_SPAN} at {@code idx} if its opening tag
     *  doesn't fit on its current line and needs one-attribute-per-line wrapping, or {@code null}
     *  for no change (Option 0: already fits, or has no attributes to wrap in the first place).
     *  Width is measured over the opening tag only ({@link Token#jsxOpeningTagEndOffset} bytes of
     *  {@code text}), never over any children -- a children-bearing tag's child content is opaque
     *  to this measurement, same approximation Increment 1's {@code JsxWrapDiagnostics} already
     *  uses, so a short opening tag with huge children never spuriously wraps.
     */
    private String renderJsxSelfClosingWrapCandidate(final List<Token> tokens, final int idx)
    {
        final Token t = tokens.get(idx);
        if(t.jsxOpeningTagEndOffset < 0) return null;
        if( t.jsxAttrBoundaries == null || t.jsxAttrBoundaries.isEmpty() ) return null;

        final int openingTagWidth = t.jsxOpeningTagEndOffset;
        final int width           = lineColumnOf(tokens, idx) + openingTagWidth;
        if(width <= lineLengthLimit) return null; // Already fits -- Option 0

        final String        indent     = lineIndent(tokens, idx);
        final String        attrIndent = indent + defaultIndentUnit;
        final String        text       = t.text;
        final List<Integer> b          = t.jsxAttrBoundaries;

        // Opening tag text only -- `tail` (children + closing tag, when present) is never
        // touched, only spliced back in verbatim at the very end (sub-context 2)
        final String openingTagText = text.substring(0, t.jsxOpeningTagEndOffset);
        final String tail           = text.substring(t.jsxOpeningTagEndOffset);

        int tagOpenEnd = b.get(0);
        while( tagOpenEnd > 0 && Character.isWhitespace(
            openingTagText.charAt(tagOpenEnd - 1)
        ) ) --tagOpenEnd;

        final boolean selfClosing = openingTagText.endsWith("/>");
        final String  closeMarker = selfClosing ? "/>" : ">";
        final String  lastSeg     = openingTagText.substring( b.get( b.size() - 1 ) );
        final int     closeAt     = lastSeg.lastIndexOf(closeMarker);
        if(closeAt < 0) return null; // Defensive -- always true for a well-formed root tag, but never guess
        final String lastAttrText = lastSeg.substring(0, closeAt).trim();

        final StringBuilder wrapped = new StringBuilder( openingTagText.substring(0, tagOpenEnd) );
        for( int a = 0; a < b.size() - 1; ++a ) {
            final String seg = openingTagText.substring( b.get(a), b.get(a + 1) ).trim();
            wrapped.append('\n').append(attrIndent).append(seg);
        }
        wrapped.append('\n').append(attrIndent).append(lastAttrText);
        wrapped.append('\n').append(indent).append(closeMarker);
        wrapped.append(tail);

        return wrapped.toString();
    }

    // ── §11.1 Union/intersection type continuation-line alignment ────────────────────
    /**
     * STYLE_JS_TS.md §11.1: a `type X = A | B | C;` alias whose union/intersection RHS overflows
     * and wraps onto multiple physical lines preserves the author's own break-before-operator vs.
     * break-after-operator choice untouched ({@link #enforceUnionIntersectionSpacing} already
     * handles same-line `|`/`&` spacing) -- this pass only re-indents each continuation line so
     * its operand column-aligns under the RHS's first token on the declaration's own line, same
     * as any other continuation-alignment shape in this codebase. {@link
     * JsTsDeclarationAlignmentRule#parseTypeAlias} deliberately bails (returns {@code null},
     * leaving the statement's original text untouched) on any multi-line initializer, so a
     * dedicated pass is needed here rather than folding this into that grid renderer.
     *
     * <p>Only `type NAME = ...;` top-level alias declarations are handled (matches this file's
     * other §11.1 alignment support) -- an inline union type elsewhere (a parameter/return-type
     * annotation) is out of scope, same bailout posture as everywhere else in this file: any gap
     * touching a frozen token, or a shape this method doesn't recognize, is left completely
     * untouched.
     */
    public String enforceUnionTypeContinuationIndent(final List<Token> tokens)
    {
        if(!lang.isTs) return render( tokens, new HashMap<>() );
        final StringBuilder out = new StringBuilder();
              int           i   = 0;
        final int           n   = tokens.size();
        while(i < n) {
            final Token t = tokens.get(i);
            if( t.type == TokenType.KEYWORD && "type".equals(t.text) && !t.frozen
                    && ( i == 0 || isStatementBoundary(tokens, i) ) ) {
                final int rewritten = tryRewriteUnionTypeAlias(tokens, i, out);
                if(rewritten > i) {
                    i = rewritten;
                    continue;
                }
            } // if
            out.append(t.text);
            ++i;
        } // while

        return out.toString();
    }

    /**
     * True when the nearest preceding significant token to {@code idx} is a statement boundary
     *  (`;`, `{`, `}`) or there isn't one (start of file/scope) -- i.e. {@code idx} can validly
     *  start a new top-level statement.
     */
    private boolean isStatementBoundary(final List<Token> tokens, final int idx)
    {
        final int prevIdx = prevSignificantIndex(tokens, idx - 1);
        if(prevIdx < 0) return true;
        final Token prev = tokens.get(prevIdx);

        return isPunct(prev, ";") || isPunct(prev, "{") || isPunct(prev, "}");
    }

    /**
     * Attempts to rewrite one {@code type NAME = ...;} alias starting at the `type` keyword
     *  (index {@code typeIdx}) whose RHS is a multi-line union/intersection expression, appending
     *  the rewritten text (with continuation lines re-indented) directly to {@code out} and
     *  returning the index just past the statement's `;` on success, or returning {@code typeIdx}
     *  unchanged (having appended nothing) if this shape isn't recognized.
     */
    private int tryRewriteUnionTypeAlias(
        final List<Token>   tokens,
        final int           typeIdx,
        final StringBuilder out
    )
    {
        final int nameIdx = nextSignificantIndex(tokens, typeIdx + 1);
        if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) return typeIdx;
        int eqIdx = nextSignificantIndex(tokens, nameIdx + 1);
        if(eqIdx < 0) return typeIdx;
        if( isOp( tokens.get(eqIdx), "<" ) ) {
            int depth = 0;
            int j     = eqIdx;
            while( j < tokens.size() ) {
                if( isOp( tokens.get(j), "<" ) ) {
                    ++depth;
                }
                else if( isOp( tokens.get(j), ">" ) ) {
                    --depth;
                    if(depth == 0) {
                        j = nextSignificantIndex(tokens, j + 1);
                        break;
                    }
                }
                j = nextSignificantIndex(tokens, j + 1);
            } // while
            eqIdx = j;
        } // if
        if( eqIdx < 0 || !isOp( tokens.get(eqIdx), "=" ) ) return typeIdx;
        final int rhsStart = nextSignificantIndex(tokens, eqIdx + 1);
        if(rhsStart < 0) return typeIdx;
        int     semiIdx    = rhsStart;
        int     depth      = 0;
        boolean hasUnion   = false;
        boolean spansLines = false;
        while( semiIdx < tokens.size() ) {
            final Token t = tokens.get(semiIdx);
            if( isPunct(
                t, "("
            ) || isPunct(
                t, "["
            ) || isPunct(
                t, "{"
            ) || t.type == TokenType.ANGLE_BRACKET_OPEN ) depth++;
            else if( isPunct(
                t, ")"
            ) || isPunct(
                t, "]"
            ) || isPunct(
                t, "}"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE ) depth--;
            else if( depth == 0 && isPunct(t, ";") ) break;
            else if( depth == 0 && ( isOp(t, "|") || isOp(t, "&") ) ) hasUnion = true;
            else if(t.type == TokenType.NEWLINE) spansLines = true;
            else if(t.frozen) return typeIdx;
            ++semiIdx;
        } // while
        if( semiIdx >= tokens.size() || !hasUnion || !spansLines ) return typeIdx;

        final int rhsCol = lineColumnOf(tokens, rhsStart);
        for(int k = typeIdx; k < rhsStart; ++k) out.append( tokens.get(k).text );
        int k           = rhsStart;
        int nestedDepth = 0;
        while(k < semiIdx) {
            final Token t = tokens.get(k);
            if( isPunct(
                t, "("
            ) || isPunct(
                t, "["
            ) || isPunct(
                t, "{"
            ) || t.type == TokenType.ANGLE_BRACKET_OPEN ) {
                ++nestedDepth;
            }
            else if( isPunct(
                t, ")"
            ) || isPunct(
                t, "]"
            ) || isPunct(
                t, "}"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE ) {
                --nestedDepth;
            }
            // Only re-indent a continuation line that breaks at the union/intersection's own
            // top level (nestedDepth == 0) -- a NEWLINE inside a nested object-type literal
            // member (e.g. `type X = { ...multi-line-body... } & Y;`) belongs to that nested
            // shape's own indentation (already normalized by an earlier pass/ScopePipelineCurly)
            // and must be left completely untouched here, or its lines get wrongly forced onto
            // this alias's RHS column (STYLE_JS_TS.md §11.1 real-code regression).
            if(t.type == TokenType.NEWLINE && nestedDepth == 0) {
                out.append('\n');
                ++k;
                while( k < semiIdx && tokens.get(k).type == TokenType.WHITESPACE ) k++;
                final boolean breaksBeforeOperator = k < semiIdx && ( isOp(
                    tokens.get(k), "|"
                ) || isOp(
                    tokens.get(k), "&"
                ) );
                final int     indent               = breaksBeforeOperator ? Math.max(
                    0, rhsCol - 2
                ) : rhsCol;
                for(int s = 0; s < indent; ++s) out.append(' ');
                continue;
            } // if
            out.append(t.text);
            ++k;
        } // while
        out.append( tokens.get(semiIdx).text ); // `;`
        return semiIdx + 1;
    }

    // ── §6 Arrow functions -- spacing and always-kept parameter parens ───────────────
    /**
     * STYLE_JS_TS.md §6: `=>` is always spaced (one space on both sides), same as Kotlin's `->`
     * ({@link KotlinSpecificRule#enforceArrowSpacing}, reused here structurally). Confirmed via a
     * standalone harness before writing this method that `=>` spacing is <b>not</b> already free
     * from any existing generic pass -- this codebase has no general from-scratch binary-operator
     * respacing pass for any language (e.g. `const x=1;`/`const y = 1+2;` both round-trip with
     * their original spacing completely untouched), so `=>` needs its own dedicated flat pass,
     * same conservative bailout (a gap containing a comment, a NEWLINE, or a frozen token is left
     * untouched) as every other pass in this file.
     *
     * <p>The other two §6 items need no code here: (1) K&R brace style for an arrow block body is
     * already satisfied by construction -- {@link #enforceMethodDefinitionAllmanBraceStyle}'s own
     * candidate signal never matches a `{` directly preceded by `=>`, so an arrow body's brace is
     * simply never touched by any Allman-conversion pass, staying wherever it was written (K&R);
     * (2) same-line no-brace for a single-expression body is likewise never rewritten either way
     * (this codebase never auto-adds or auto-strips braces around a body -- see
     * `collapseSingleExpressionBlocks`'s narrower control-flow-only scope -- so a single-expression
     * arrow body written braceless stays braceless, matching §6's own worked examples, without any
     * dedicated pass).
     */
    public String enforceArrowSpacing(final List<Token> tokens)
    {
        if(!lang.isJs && !lang.isTs) return render( tokens, new HashMap<>() );
        final StringBuilder out             = new StringBuilder();
        final List<Token>   gap             = new ArrayList<>();
              Token         lastSignificant = null;
        final int           n               = tokens.size();
              int           i               = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean gapBlocked      = gap.stream().anyMatch(
                g->isComment(g) || g.type == TokenType.NEWLINE || g.frozen
            ) || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean adjacentToArrow = isOp(lastSignificant, "=>") || isOp(t, "=>");

            if(gapBlocked || !adjacentToArrow) {
                for(final Token g : gap) out.append(g.text);
            }
            else {
                out.append(' ');
            }

            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    /**
     * STYLE_JS_TS.md §6: an arrow function's parameter list keeps its parens even for a single
     * untyped parameter -- `(n) => ...`, never bare `n => ...` -- "for alignment consistency with
     * multi-parameter arrows in the same group" (the style doc's own stated reasoning). Read as an
     * always-normalize rule (not a preserve-as-written one): every `=>` whose immediately
     * preceding significant token is a bare IDENTIFIER (never already parenthesized -- a
     * multi/typed/zero-parameter arrow's `=>` is always preceded by `)` instead, untouched by this
     * check) gets that identifier wrapped in parens. Conservative bailout matching this file's
     * other passes: a gap containing a NEWLINE or comment between the identifier and `=>`, or
     * either token being frozen, leaves the pair untouched.
     */
    public String enforceArrowFunctionParameterParens(final List<Token> tokens)
    {
        if(!lang.isJs && !lang.isTs) return render( tokens, new HashMap<>() );
        final Map<Integer, String> overrides = new HashMap<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( !isOp(t, "=>") || t.frozen ) continue;
            final int prevIdx = prevSignificantIndex(tokens, i - 1);
            if(prevIdx < 0) continue;
            final Token prev = tokens.get(prevIdx);
            if(prev.type != TokenType.IDENTIFIER || prev.frozen) continue;
            if( hasNewlineOrCommentBetween(tokens, prevIdx, i) ) continue;
            // TS explicit return-type annotation (`(node: Node): node is Function => {...}`,
            // `(a): SomeType => {...}`) -- the identifier immediately before `=>` is the tail of
            // the return type, not a bare single arrow parameter (a real bare-param arrow can
            // never itself carry a `:`/type-predicate `is` right before it -- that shape requires
            // parens on the parameter, not on the return type). Without this check, wrapping
            // produced `node is (Function) =>`, a real TS parse error (found via vuejs/core
            // dogfood tsc typecheck, `compiler-core/src/babelUtils.ts`'s `isFunctionType`).
            // Recognized by the token immediately preceding the identifier being the type-
            // predicate keyword `is` or the return-type colon `:` itself, or (vuejs/core dogfood,
            // `shared/src/general.ts`'s `hasOwn`: `key is keyof typeof val => ...`) a unary
            // type-operator keyword (`typeof`/`keyof`) applied directly to the identifier as part
            // of the same return-type expression -- `typeof`/`keyof` can never themselves
            // legally precede a bare arrow parameter (that shape requires parens on the
            // parameter, not a type operator), so their presence right before the identifier
            // means it's still part of the return type, same conclusion as the `:`/`is` cases.
            //
            // A dotted/qualified return type or type predicate (`ts.server.PluginModule =>`,
            // `tss.Node =>`, `node is ts.Diagnostic =>`) has `prev` as merely the LAST segment
            // of a `.`-chain, not directly preceded by `:`/`is`/`typeof`/`keyof` -- its immediate
            // predecessor is a `.` instead. Without walking back over the whole chain first, this
            // check missed that case and wrapped just the tail segment (`ts.server.(PluginModule)
            // =>`), a real TS parse error -- found via `angular/angular` dogfood tsc parse-check,
            // `override_rename_ts_plugin.ts`/`template_target.ts`/`checker.ts`. Walk back over any
            // number of `IDENTIFIER '.'` pairs first so `anchorIdx` lands on the chain's own first
            // segment before checking what precedes THAT for the same `:`/`is`/`typeof`/`keyof`
            // markers.
            //
            // A union return type/type-predicate whose last member is a bare identifier (`x is A |
            // B =>`, `A | B | C =>`) has the same shape: `prev`/`anchorIdx` lands on `B`/`C`, but the
            // real anchor to bail-out-check is whatever precedes the FIRST union member, past every
            // `| operand` pair (and each operand may itself be a dotted chain, e.g. `x is A | ts.B
            // =>`). Without walking back over `|` too, only the dotted-chain shape was covered --
            // found via `microsoft/TypeScript` dogfood parse-check (`services/symbolDisplay.ts`:
            // `declaration is AccessorDeclaration | PropertyDeclaration =>` ->
            // `... | (PropertyDeclaration) =>`; also `services/mapCode.ts`, `compiler/checker.ts`).
            // Alternate dotted-chain and union walk-backs until neither makes progress, so chained
            // unions (`A | B | C`) and dotted union members both resolve to the true first anchor.
            int anchorIdx = prevIdx;
            while(true) {
                boolean moved = false;
                while(true) {
                    final int dotIdx = prevSignificantIndex(tokens, anchorIdx - 1);
                    if( dotIdx < 0 || !isOp( tokens.get(dotIdx), "." ) ) break;
                    final int identIdx = prevSignificantIndex(tokens, dotIdx - 1);
                    if( identIdx < 0 || tokens.get(identIdx).type != TokenType.IDENTIFIER ) break;
                    anchorIdx = identIdx;
                    moved     = true;
                } // while
                final int pipeIdx = prevSignificantIndex(tokens, anchorIdx - 1);
                if( pipeIdx >= 0 && isOp( tokens.get(pipeIdx), "|" ) ) {
                    final int identIdx = prevSignificantIndex(tokens, pipeIdx - 1);
                    if( identIdx >= 0 && tokens.get(identIdx).type == TokenType.IDENTIFIER ) {
                        anchorIdx = identIdx;
                        moved     = true;
                    }
                } // if
                if(!moved) break;
            } // while
            final int prevPrevIdx = prevSignificantIndex(tokens, anchorIdx - 1);
            if(prevPrevIdx >= 0) {
                final Token prevPrev = tokens.get(prevPrevIdx);
                if( isOp(
                    prevPrev, ":"
                ) || ( prevPrev.type == TokenType.KEYWORD && ( "is".equals(
                    prevPrev.text
                ) || "typeof".equals(
                    prevPrev.text
                ) || "keyof".equals(
                    prevPrev.text
                ) ) ) ) continue;
            } // if
            overrides.put(prevIdx, "(" + prev.text + ")");
        } // for

        return render(tokens, overrides);
    }

    // ── §11.2 Class field modifier-priority table ────────────────────────────────────
    /**
     * STYLE_JS_TS.md §11.2's fixed six-slot modifier order -- `declare` first (ambient marker),
     *  then visibility (`public`/`private`/`protected`, mutually exclusive so all three share one
     *  slot), then `static`, `abstract`, `override`, `readonly` last (parallels Java's `final`
     *  taking the position right before the name).
     */
    private static final List<String>         MODIFIER_ORDER    = Arrays.asList(
        "declare", "public", "private", "protected", "static", "abstract", "override", "readonly"
    );
    private static final Map<String, Integer> MODIFIER_PRIORITY = new HashMap<>();
    static {
        for( int i = 0; i < MODIFIER_ORDER.size(); ++i ) MODIFIER_PRIORITY.put(
            MODIFIER_ORDER.get(i), i
        );
    }

    /**
     * STYLE_JS_TS.md §11.2: normalizes a scrambled run of two-or-more consecutive class-member
     * modifier keywords into the canonical order above, e.g. `readonly static private x: number;`
     * → `private static readonly x: number;`. TS-only (`lang.isTs`) -- JS has none of these
     * modifier keywords at all (JS class fields have no `public`/`private`/`static`-as-a-modifier
     * grammar in this codebase's scope). A run of a single modifier keyword is left completely
     * untouched (nothing to reorder). Applied wherever a maximal run of 2+ consecutive modifier
     * keywords appears (not narrowly gated to "inside a class body" specifically) -- in valid
     * TS/JS syntax these keywords never co-occur consecutively outside a class-member modifier
     * list, so this is a safe, conservative scope choice that also naturally covers method
     * modifiers (`private static foo()`) the same way, consistent with the table's own Java-
     * derived precedent of one shared modifier order for both fields and methods. Only a
     * same-line, comment-free, non-frozen run is reordered -- a run containing a NEWLINE, a
     * comment, or a frozen token in any of its internal gaps is left untouched, matching this
     * file's usual conservative bailout posture.
     */
    public String reorderClassFieldModifiers(final List<Token> tokens)
    {
        if(!lang.isTs) return render( tokens, new HashMap<>() );
        final StringBuilder out = new StringBuilder();
        final int           n   = tokens.size();
              int           i   = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if( isModifierKeyword(t) && !t.frozen ) {
                final List<Token>       runKeywords = new ArrayList<>();
                final List<List<Token>> gapsAfter   = new ArrayList<>();
                      int               j           = i;
                      boolean           brokenRun   = false;
                while( j < n && isModifierKeyword( tokens.get(j) ) && !tokens.get(j).frozen ) {
                    runKeywords.add( tokens.get(j) );
                      int         k   = j + 1;
                final List<Token> gap = new ArrayList<>();
                    while( k < n && isGapToken( tokens.get(k) ) ) {
                        final Token g = tokens.get(k);
                        if( isComment(
                            g
                        ) || g.type == TokenType.NEWLINE || g.frozen ) brokenRun = true;
                        gap.add(g);
                        ++k;
                    } // while
                    gapsAfter.add(gap);
                    if(brokenRun) {
                        j = k;
                        break;
                    }
                    j = k;
                } // while
                if( !brokenRun && runKeywords.size() >= 2 ) {
                    final List<Token> sorted = new ArrayList<>(runKeywords);
                    sorted.sort(
                        (a, b) -> MODIFIER_PRIORITY.get(a.text) - MODIFIER_PRIORITY.get(b.text)
                    );
                    for( int idx = 0; idx < sorted.size(); ++idx ) {
                        out.append( sorted.get(idx).text );
                        for( final Token g : gapsAfter.get(idx) ) out.append(g.text);
                    }
                } // if
                else {
                    for(int idx = i; idx < j; ++idx) out.append( tokens.get(idx).text );
                }
                i = j;
                continue;
            } // if
            out.append(t.text);
            ++i;
        } // while

        return out.toString();
    }

    private boolean isModifierKeyword(final Token t)
    {
        return t != null && t.type == TokenType.KEYWORD && MODIFIER_PRIORITY.containsKey(t.text);
    }

    // ── §12 Enum member formatting ───────────────────────────────────────────────────
    /**
     * One parsed enum member: an optional list of standalone (own-line) leading comments, the
     *  member's own name, whether it carries an explicit `= value` (and that value's raw text,
     *  trimmed, if so -- internal spacing within the value expression itself is left exactly as
     *  originally written, out of this method's scope), and an optional same-line trailing
     *  comment. {@code trailingComment} is mutable -- it may be discovered either immediately
     *  after the member's own name/value (before its trailing comma) or immediately after that
     *  comma (before the next member), so it's filled in from whichever position is found.
     */
    private static final class EnumMember {

        private final List<String> leadingComments;
        private final String       name;
        private final boolean      hasValue;
        private final String       value;
        private       String       trailingComment;

        EnumMember(
            final List<String> leadingComments,
            final String       name,
            final boolean      hasValue,
            final String       value
        )
        {
            this.leadingComments = leadingComments;
            this.name            = name;
            this.hasValue        = hasValue;
            this.value           = value;
        }

    } // class EnumMember

    /**
     * STYLE_JS_TS.md §12: a TS enum body (`enum Name { ... }` / `const enum Name { ... }`) is
     * always rendered one member per line, regardless of how it was originally written -- the
     * style doc's own text says this "derives from the C++ `enum class` handling already
     * implemented in the JAR" and members are "always one-per-line ... either way", so an
     * originally same-line comma-separated member list (e.g. {@code enum Color { Red, Green,
     * Blue }}) is reflowed, not left as written; this is a deliberate, doc-confirmed exception to
     * this codebase's usual "preserve original layout unless a specific rule requires otherwise"
     * posture, not a guess. When any member in the body has an explicit {@code = value}, every
     * such member's name column is `=`-column-aligned against its like siblings (a member with no
     * explicit value renders as a plain {@code Name,}, not padded -- there's no `=` for it to
     * align against, and the style doc's own "no explicit member values" example shows no
     * alignment at all for an all-implicit-value enum). Every member always ends in a trailing
     * `,` -- never a semicolon, and never omitted even for the last member (matching both of the
     * style doc's own worked examples) -- enum bodies are comma-separated member lists, not
     * statement lists; a bogus semicolon here would be the same class of bug already found and
     * fixed for import brace headers ({@link #enforceSemicolonInsertion}, Checkpoint 16) and
     * documented as still-broken for enums in STATE_JS_TS.md's Checkpoint 21 bug 8 -- see
     * {@link #isEnumBodyBrace}'s new {@code classifyBraces} disjunct for the actual fix.
     * A standalone (own-line) comment immediately preceding a member is preserved verbatim on its
     * own line ahead of that member's rendered line; a same-line trailing comment (found either
     * right after the member's own name/value or right after its trailing comma) is preserved on
     * that member's own rendered line. The closing `}`'s own closing comment and the guaranteed
     * blank-line-after-`{`/before-`}` are both left entirely to the existing general-purpose
     * passes ({@link BlockStructureRule#addClosingComments}/{@code insertNamedConstructBlankLines}
     * -- unrelated to enums specifically, already correctly fire for a named-construct brace via
     * the tokenizer's own enum name-tagging), not duplicated here.
     * Conservative bailout, matching every other pass in this file: an enum body containing any
     * frozen token, a member whose name isn't a plain identifier, a floating comment with no
     * member attached after it, or a member value expression spanning multiple physical lines is
     * left completely untouched rather than guessed at.
     */
    public String enforceEnumMemberFormatting(final List<Token> tokens)
    {
        final Map<Integer, Integer> braces     = matchBraces(tokens);
        final List<int[]>           enumBodies = new ArrayList<>();
        for( final Map.Entry<Integer, Integer> e : braces.entrySet() ) {
            if( isEnumBodyBrace( tokens, e.getKey() ) ) {
                enumBodies.add( new int[] { e.getKey(), e.getValue() } );
            }
        }
        if( enumBodies.isEmpty() ) {
            final StringBuilder sb = new StringBuilder();
            for(final Token t : tokens) sb.append(t.text);
            return sb.toString();
        }
        enumBodies.sort( (a, b) -> Integer.compare( a[0], b[0] ) );

        final StringBuilder out    = new StringBuilder();
              int           cursor = 0;
        for( final int[] body : enumBodies ) {
            final int openIdx  = body[0];
            final int closeIdx = body[1];
            if( MiscRuleCore.anyFrozen(tokens, openIdx, closeIdx + 1) ) continue;
            final String rewritten = rewriteEnumBody(tokens, openIdx, closeIdx);
            if(rewritten == null) continue;
            for(int i = cursor; i <= openIdx; ++i) out.append( tokens.get(i).text );
            out.append(rewritten);
            cursor = closeIdx; // Resume from the closing `}` token itself
        } // for
        for( int i = cursor; i < tokens.size(); ++i ) out.append( tokens.get(i).text );

        return out.toString();
    }

    /**
     * Rewrites the interior of one enum body (tokens strictly between {@code openIdx}'s `{` and
     *  {@code closeIdx}'s `}`, not including either brace) into the one-member-per-line,
     *  `=`-aligned-when-applicable layout described on {@link #enforceEnumMemberFormatting}, or
     *  returns {@code null} if {@link #parseEnumMembers} can't safely parse it
     */
    private String rewriteEnumBody(final List<Token> tokens, final int openIdx, final int closeIdx)
    {
        final List<EnumMember> members = parseEnumMembers(tokens, openIdx, closeIdx);
        if(members == null) return null;
        int maxNameWidth = 0;
        for(final EnumMember m : members) {
            if(m.hasValue) maxNameWidth = Math.max( maxNameWidth, m.name.length() );
        }
        final String        bodyIndent   = lineIndent(tokens, openIdx);
        final String        memberIndent = bodyIndent + defaultIndentUnit;
        final StringBuilder sb           = new StringBuilder();
        for(final EnumMember m : members) {
            for(final String comment : m.leadingComments) sb.append(
                '\n'
            ).append(
                memberIndent
            ) .append(
                reindentLeadingComment(comment, memberIndent)
            );
            sb.append('\n').append(memberIndent);
            if(m.hasValue) sb.append(
                padRight(m.name, maxNameWidth)
            ).append(
                " = "
            ).append(
                m.value
            );
            else sb.append(m.name);
            sb.append(',');
            if(m.trailingComment != null) sb.append("  ").append(m.trailingComment);
        } // for
        sb.append('\n').append(bodyIndent);

        return sb.toString();
    }

    /**
     * Parses an enum body's interior (between {@code openIdx} and {@code closeIdx}, exclusive)
     *  into an ordered list of {@link EnumMember}s via a single forward scan, tracking whether the
     *  most recently seen top-level separator was a comma on the *same physical line* as what
     *  follows it (so a same-line trailing comment right after a member's own comma is correctly
     *  attributed back to that member, not misread as a standalone leading comment for the next
     *  one). Returns {@code null} (bail, leave this enum body untouched) if a non-identifier
     *  top-level token is found where a member name is expected, if a member's value expression
     *  spans a NEWLINE, or if a floating comment is left with no member following it.
     */
    private List<EnumMember> parseEnumMembers(
        final List<Token> tokens,
        final int         openIdx,
        final int         closeIdx
    )
    {
        final List<EnumMember> members                 = new ArrayList<>();
        final List<String>     leadingComments         = new ArrayList<>();
              boolean          sameLineAsPrevSeparator = false;
              int              i                       = openIdx + 1;
        while(i < closeIdx) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE) {
                ++i;
                continue;
            }
            if(t.type == TokenType.NEWLINE) {
                sameLineAsPrevSeparator = false;
                ++i;
                continue;
            }
            if( isPunct(t, ",") ) {
                sameLineAsPrevSeparator = true;
                ++i;
                continue;
            }
            if( isComment(t) ) {
                if( sameLineAsPrevSeparator && !members.isEmpty() ) members.get(
                    members.size() - 1
                ).trailingComment = t.text;
                else leadingComments.add(t.text);
                ++i;
                continue;
            } // if
            if(t.type != TokenType.IDENTIFIER) return null;
            final String name = t.text;
            ++i;
            while( i < closeIdx && tokens.get(i).type == TokenType.WHITESPACE ) i++;
            boolean hasValue = false;
            String  value    = null;
            if( i < closeIdx && isOp( tokens.get(i), "=" ) ) {
                hasValue = true;
                ++i;
                final StringBuilder valueBuf = new StringBuilder();
                      int           depth    = 0;
                while(i < closeIdx) {
                    final Token vt = tokens.get(i);
                    if( depth == 0 && ( isPunct(vt, ",") || isComment(vt) ) ) break;
                    if(depth == 0 && vt.type == TokenType.NEWLINE) {
                        // Ends the value here rather than bailing the whole enum: this is
                        // overwhelmingly the last member with no trailing comma before `}`
                        // (`Pending=3\n}`), not an actual multi-line value expression
                        break;
                    }
                         if( isPunct(vt, "(") || isPunct(vt, "[") || isPunct(vt, "{") ) depth++;
                    else if( isPunct(vt, ")") || isPunct(vt, "]") || isPunct(vt, "}") ) depth--;
                    valueBuf.append(vt.text);
                    ++i;
                } // while
                value = valueBuf.toString().trim();
            } // if
            // A same-line trailing comment can sit directly after the name/value, before the
            // member's own trailing comma (less common than after the comma, but valid)
            while( i < closeIdx && tokens.get(i).type == TokenType.WHITESPACE ) i++;
            String trailingComment = null;
            if( i < closeIdx && isComment( tokens.get(i) ) ) {
                trailingComment = tokens.get(i).text;
                ++i;
            }
            final EnumMember member = new EnumMember(
                new ArrayList<>(leadingComments), name, hasValue, value
            );
            member.trailingComment = trailingComment;
            leadingComments.clear();
            members.add(member);
            sameLineAsPrevSeparator = false;
        } // while
        if( !leadingComments.isEmpty() ) return null; // A floating comment with no member attached after it -- don't drop it
        return members.isEmpty() ? null : members;
    }

    // ── §11.2 Class field declaration-alignment grid ──────────────────────────────────
    private static final Set<String> CLASS_FIELD_MODIFIERS = new HashSet<>( Arrays.asList(
        "declare", "public", "private", "protected", "static", "abstract", "override", "readonly"
    ) );

    /**
     * One parsed simple class-field declaration (`[modifiers] name[?][: type][ = init];`) inside
     *  a class body -- the class-body analog of {@link InterfaceMember}, plus the modifier phrase
     *  and optional initializer §11.2 field declarations carry that interface members never do.
     */
    private static final class ClassField {

        private final List<String> leadingComments;
        private final String       modifierPhrase;  // "" if no modifiers
        private final String       name;
        private final String       type;
        private final boolean      hasValue;
        private final String       value;           // Null unless hasValue
        private       String       trailingComment;
        private final int          startIdx;
        private final int          endIdx;          // Exclusive: index of the first token of whatever follows

        ClassField(
            final List<String> leadingComments,
            final String       modifierPhrase,
            final String       name,
            final String       type,
            final boolean      hasValue,
            final String       value,
            final int          startIdx,
            final int          endIdx
        )
        {
            this.leadingComments = leadingComments;
            this.modifierPhrase  = modifierPhrase;
            this.name            = name;
            this.type            = type;
            this.hasValue        = hasValue;
            this.value           = value;
            this.startIdx        = startIdx;
            this.endIdx          = endIdx;
        }

    } // class ClassField

    /**
     * STYLE_JS_TS.md §11.2: consecutive simple class-field declarations (no blank line separating
     * them) form a declaration-alignment grid, same shape as Java's modifier/type/name grid
     * (STYLE.md §5) -- the modifier phrase is padded as one unit so the name column aligns, the
     * name is padded so the `:` column aligns, and when any member in the group carries an
     * initializer, the type is additionally padded so the `=` column aligns too. A method, an
     * untyped field (no `:`), or any field shape {@link #tryParseClassField} doesn't recognize
     * ends the current group (rendered as-is up to that point) without touching the unrecognized
     * member itself -- same conservative "only rewrite what's fully understood" posture as
     * {@link #enforceInterfaceTypeAliasMemberColonAlignment}, but per-group rather than per-whole-
     * body, since a class body (unlike an interface) routinely mixes fields with methods.
     */
    public String enforceClassFieldAlignmentGrid(final List<Token> tokens)
    {
        if(!lang.isTs) return render( tokens, new HashMap<>() );
        final Map<Integer, Integer> braces        = matchBraces(tokens);
        final List<Integer>         allClassOpens = new ArrayList<>();
        for( final Map.Entry<Integer, Integer> e : braces.entrySet() ) {
            if( "CLASS".equals(
                classBraceKind( tokens, e.getKey() )
            ) ) allClassOpens.add(
                e.getKey()
            );
        } // for
        allClassOpens.sort(Integer::compare);
        // A class body can legitimately nest another class (e.g. an anonymous `return class
        // extends Base { ... };` expression inside an outer class's method, seen in nestjs/nest's
        // Module.createModuleReferenceType). The single linear `cursor` sweep below assumes every
        // selected class span is disjoint -- given an inner class nested inside an already-selected
        // outer one, the outer's own `rewriteClassFieldGroups` call already copies the inner class's
        // entire span through byte-for-byte (as an ordinary unrecognized member, via
        // `skipTopLevelMember`), so re-processing the inner span again as its own top-level entry
        // both duplicates its content in the output and -- far worse -- walks `cursor` backward to
        // the inner class's own (earlier) `closeIdx`, causing the final raw-copy loop below to
        // re-emit everything from there to the true end of file a second time. Keep only the
        // outermost class open at each nesting level; a nested class's own fields simply don't get
        // the alignment-grid treatment (same conservative "only rewrite what's fully understood"
        // posture as the rest of this method -- extending the grid into nested class bodies is
        // future work, not a regression from before this fix).
        final List<Integer> classOpens   = new ArrayList<>();
              int           coveredUntil = -1;
        for(final int openIdx : allClassOpens) {
            if(openIdx < coveredUntil) continue; // Nested inside an already-selected outer class -- skip
            classOpens.add(openIdx);
            coveredUntil = braces.get(openIdx);
        }

        final StringBuilder out    = new StringBuilder();
              int           cursor = 0;
        for(final int openIdx : classOpens) {
            final int closeIdx = braces.get(openIdx);
            if( MiscRuleCore.anyFrozen(tokens, openIdx, closeIdx + 1) ) continue;
            final String rewritten = rewriteClassFieldGroups(tokens, openIdx, closeIdx);
            if(rewritten == null) continue;
            for(int i = cursor; i <= openIdx; ++i) out.append( tokens.get(i).text );
            out.append(rewritten);
            cursor = closeIdx;
        } // for
        for( int i = cursor; i < tokens.size(); ++i ) out.append( tokens.get(i).text );

        return out.toString();
    }

    /**
     * Rewrites the interior of one class body (strictly between {@code openIdx}'s `{` and
     *  {@code closeIdx}'s `}`), aligning each maximal run of consecutive simple field declarations
     *  and copying everything else (methods, blank lines, comments that aren't a field's own
     *  leading comment) through byte-for-byte
     */
    private String rewriteClassFieldGroups(
        final List<Token> tokens,
        final int         openIdx,
        final int         closeIdx
    )
    {
        final StringBuilder    out             = new StringBuilder();
        final List<ClassField> group           = new ArrayList<>();
        final List<String>     leadingComments = new ArrayList<>();
              int              copyFrom        = openIdx + 1;
              int              i               = openIdx + 1;
        // Tracks where leadingComments' first comment token started, so the blank-line
        // preservation below can stop before it -- flushClassFieldGroup renders each leading
        // comment with its own trailing newline, so counting the newline after it here too
        // would double it up
        int leadingCommentsStartIdx = -1;
        while(i < closeIdx) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                ++i;
                continue;
            }
            if( isComment(t) ) {
                if( leadingComments.isEmpty() ) leadingCommentsStartIdx = i;
                leadingComments.add(t.text);
                ++i;
                continue;
            }
            final ClassField field = tryParseClassField(
                tokens, i, closeIdx, new ArrayList<>(leadingComments)
            );
            if(field == null) {
                if( flushClassFieldGroup(
                    tokens, group, openIdx, out
                ) ) copyFrom = skipOneNewline(
                    tokens, copyFrom
                );
                leadingComments.clear();
                leadingCommentsStartIdx = -1;
                // Not a recognized simple field -- copy raw source through untouched from
                // copyFrom up to this member's own end, then resume grouping after it
                final int memberEnd = skipTopLevelMember(tokens, i, closeIdx);
                for(int k = copyFrom; k < memberEnd; ++k) out.append( tokens.get(k).text );
                copyFrom = memberEnd;
                i        = memberEnd;
                continue;
            } // if
            if( blankLineBetween(
                tokens, group.isEmpty() ? -1 : lastFieldEnd(group), field.startIdx
            ) ) {
                if( flushClassFieldGroup(
                    tokens, group, openIdx, out
                ) ) copyFrom = skipOneNewline(
                    tokens, copyFrom
                );
            } // if
            if( group.isEmpty() ) {
                // Preserve any blank line(s) between the previous content and this group's first
                // field, but drop the trailing indentation whitespace -- flushClassFieldGroup
                // supplies its own indent for the first rendered field, so copying raw
                // indentation here too would double it up. Stop before any leading comment --
                // flushClassFieldGroup renders that comment (and its own trailing newline)
                // separately below.
                final int rawCopyEnd = leadingCommentsStartIdx >= 0 ? leadingCommentsStartIdx : i;
                for(int k = copyFrom; k < rawCopyEnd; ++k) {
                    if( tokens.get(k).type == TokenType.NEWLINE ) out.append('\n');
                }
            } // if
            group.add(field);
            leadingComments.clear();
            leadingCommentsStartIdx = -1;
            copyFrom                = field.endIdx;
            i                       = field.endIdx;
        } // while
        if( flushClassFieldGroup(
            tokens, group, openIdx, out
        ) ) copyFrom = skipOneNewline(
            tokens, copyFrom
        );
        if( !leadingComments.isEmpty() ) {
            // A floating comment with nothing after it -- copy it through untouched
            for(int k = copyFrom; k < closeIdx; ++k) out.append( tokens.get(k).text );
            copyFrom = closeIdx;
        }
        for(int k = copyFrom; k < closeIdx; ++k) out.append( tokens.get(k).text );

        return out.toString();
    }

    private int lastFieldEnd(final List<ClassField> group)
    {
        return group.get( group.size() - 1 ).endIdx;
    }

    /**
     * If the token at {@code idx} is a NEWLINE, returns {@code idx + 1}; otherwise returns
     *  {@code idx} unchanged. Used right after {@link #flushClassFieldGroup} to avoid
     *  double-counting the line-ending newline its own rendering already emitted for the last
     *  member, before any subsequent raw-copy span starting at that same position.
     */
    private int skipOneNewline(final List<Token> tokens, final int idx)
    {
        return idx < tokens.size() && tokens.get(idx).type == TokenType.NEWLINE ? idx + 1 : idx;
    }

    /**
     * True when two NEWLINE tokens appear back-to-back (with only WHITESPACE, never a COMMENT or
     *  anything else, between them) somewhere in {@code tokens} between {@code fromIdx} (exclusive;
     *  -1 means "no prior field, never a blank-line break") and {@code toIdx} (exclusive) -- a
     *  genuine blank source line, as opposed to merely two-or-more NEWLINEs total across the gap.
     *  A same-line leading comment (`/** @internal *&#47; readonly x: T;`) forces its own line when
     *  {@link #flushClassFieldGroup} renders a group (one NEWLINE after the comment, one more after
     *  the previous field's `;`) without there ever having been a real blank line in the source --
     *  simply counting total NEWLINEs previously miscounted that shape as blank on a second pass fed
     *  the first pass's own rendered (now comment-on-its-own-line) output, splitting a group that
     *  should have stayed joined and making the resulting column alignment non-idempotent (found via
     *  `microsoft/TypeScript`'s `server/editorServices.ts`/`server/project.ts`).
     */
    private boolean blankLineBetween(final List<Token> tokens, final int fromIdx, final int toIdx)
    {
        if(fromIdx < 0) return false;
        boolean sawNewline = false;
        for(int i = fromIdx; i < toIdx; ++i) {
            final TokenType t = tokens.get(i).type;
            if(t == TokenType.NEWLINE) {
                if(sawNewline) return true;
                sawNewline = true;
            }
            else if(t != TokenType.WHITESPACE) {
                sawNewline = false;
            }
        } // for

        return false;
    }

    /**
     * Renders {@code group} (if non-empty) as an aligned grid into {@code out}, then clears it.
     *  Returns {@code true} iff anything was flushed (group was non-empty).
     */
    private boolean flushClassFieldGroup(
        final List<Token>      tokens,
        final List<ClassField> group,
        final int              classOpenIdx,
        final StringBuilder    out
    )
    {
        if( group.isEmpty() ) return false;
        int     maxModifierWidth        = 0;
        int     maxNameWidth            = 0;
        int     maxTypeWidthAmongValued = 0;
        boolean anyValued               = false;
        for(final ClassField f : group) {
            maxModifierWidth = Math.max( maxModifierWidth, f.modifierPhrase.length() );
            maxNameWidth     = Math.max( maxNameWidth, f.name.length() );
            if(f.hasValue) {
                anyValued               = true;
                maxTypeWidthAmongValued = Math.max( maxTypeWidthAmongValued, f.type.length() );
            }
        } // for
        final String memberIndent = lineIndent(tokens, classOpenIdx) + defaultIndentUnit;
        for(final ClassField f : group) {
            for(final String comment : f.leadingComments) out.append(
                memberIndent
            ) .append(
                reindentLeadingComment(comment, memberIndent)
            ) .append(
                '\n'
            );
            out.append(memberIndent);
            if(maxModifierWidth > 0) out.append(
                padRight(f.modifierPhrase, maxModifierWidth)
            ).append(
                ' '
            );
            out.append( padRight(f.name, maxNameWidth) ).append(" : ");
            if(f.hasValue) out.append(
                padRight(f.type, maxTypeWidthAmongValued)
            ).append(
                " = "
            ).append(
                f.value
            );
            else out.append( anyValued ? padRight(f.type, maxTypeWidthAmongValued) : f.type );
            out.append(';');
            if(f.trailingComment != null) out.append("  ").append(f.trailingComment);
            out.append('\n');
        } // for
        group.clear();

        return true;
    }

    /**
     * Attempts to parse a simple class-field declaration starting at {@code start} (already
     *  known to be a non-comment, non-gap token). Returns {@code null} if this isn't a recognized
     *  field shape (a method, an untyped/computed member, a multi-line type/initializer, ...).
     */
    private ClassField tryParseClassField(
        final List<Token>  tokens,
        final int          start,
        final int          closeIdx,
        final List<String> leadingComments
    )
    {
          int           i         = start;
    final StringBuilder modPhrase = new StringBuilder();
        while( i < closeIdx && tokens.get(
            i
        ).type == TokenType.KEYWORD && CLASS_FIELD_MODIFIERS.contains(
            tokens.get(i).text
        ) ) {
            if( modPhrase.length() > 0 ) modPhrase.append(' ');
            modPhrase.append( tokens.get(i).text );
            i = nextSignificantIndex(tokens, i + 1);
            if(i < 0 || i >= closeIdx) return null;
        }
        if( tokens.get(i).type != TokenType.IDENTIFIER ) return null;
        String name = tokens.get(i).text;
        i = nextSignificantIndex(tokens, i + 1);
        if(i < 0 || i >= closeIdx) return null;
        if( isOp( tokens.get(i), "?" ) || isOp( tokens.get(i), "!" ) ) {
            name = name + tokens.get(i).text;
            i    = nextSignificantIndex(tokens, i + 1);
            if(i < 0 || i >= closeIdx) return null;
        }
        if( isPunct(
            tokens.get(i), "("
        ) || tokens.get(
            i
        ).type == TokenType.ANGLE_BRACKET_OPEN ) return null; // Method, not a field
        if( !isOp( tokens.get(i), ":" ) ) return null; // Untyped field -- out of this grid's scope
        i = nextSignificantIndex(tokens, i + 1);
        if(i < 0 || i >= closeIdx) return null;
        final StringBuilder typeBuf = new StringBuilder();
              int           depth   = 0;
        while(i < closeIdx) {
            final Token vt = tokens.get(i);
            if( depth == 0 && ( isPunct(vt, ";") || isOp(vt, "=") ) ) break;
            if(vt.type == TokenType.NEWLINE) return null; // Multi-line type expression -- rare, not handled
            if( isPunct(
                vt, "("
            ) || isPunct(
                vt, "["
            ) || isPunct(
                vt, "{"
            ) || vt.type == TokenType.ANGLE_BRACKET_OPEN ) depth++;
            else if( isPunct(
                vt, ")"
            ) || isPunct(
                vt, "]"
            ) || isPunct(
                vt, "}"
            ) || vt.type == TokenType.ANGLE_BRACKET_CLOSE ) depth--;
            typeBuf.append(vt.text);
            ++i;
        } // while
        final String type = typeBuf.toString().trim();
        if( type.isEmpty() || i >= closeIdx ) return null;
        i = nextSignificantIndex(tokens, i);
        if(i < 0 || i >= closeIdx) return null;
        boolean hasValue = false;
        String  value    = null;
        if( isOp( tokens.get(i), "=" ) ) {
            hasValue = true;
            i        = nextSignificantIndex(tokens, i + 1);
            if(i < 0 || i >= closeIdx) return null;
            final StringBuilder valueBuf     = new StringBuilder();
                  boolean       pendingSpace = false;
            depth = 0;
            while(i < closeIdx) {
                final Token vt = tokens.get(i);
                if( depth == 0 && isPunct(vt, ";") ) break;
                if(vt.type == TokenType.NEWLINE) {
                    // A multi-line initializer -- most commonly one a *previous round's* own
                    // call-wrap (`enforceCallLineBreaking`, which runs both before and after this
                    // pass in FormatterCurly.format's Phase 4) already wrapped across lines, since
                    // this grid's own column padding can push a call past `lineLengthLimit`. This
                    // pass's group-membership/column-width decision must stay round-invariant, so
                    // collapse the newline (and its continuation line's leading indentation) into a
                    // single soft space instead of bailing to `null` -- bailing here previously
                    // misclassified the field as an unrecognized member on round2 (fed round1's own
                    // wrapped output), splitting/re-widening the alignment group differently than
                    // round1 saw it, a non-idempotency bug (`web_animations_player_spec.ts`).
                    pendingSpace = true;
                    ++i;
                    continue;
                } // if
                if(vt.type == TokenType.WHITESPACE && pendingSpace) {
                    ++i; // Swallow continuation-line indentation -- not part of the logical text
                    continue;
                }
                if(pendingSpace) {
                    if( valueBuf.length() > 0 && valueBuf.charAt(
                        valueBuf.length() - 1
                    ) != ' ' ) valueBuf.append(
                        ' '
                    );
                    pendingSpace = false;
                } // if
                     if( isPunct(vt, "(") || isPunct(vt, "[") || isPunct(vt, "{") ) depth++;
                else if( isPunct(vt, ")") || isPunct(vt, "]") || isPunct(vt, "}") ) depth--;
                valueBuf.append(vt.text);
                ++i;
            } // while
            value = valueBuf.toString().trim();
            if( value.isEmpty() || i >= closeIdx ) return null;
            i = nextSignificantIndex(tokens, i);
            if(i < 0 || i >= closeIdx) return null;
        } // if
        if( !isPunct( tokens.get(i), ";" ) ) return null;
        int endIdx = i + 1;
        // A same-line trailing comment travels with the field
        String trailingComment = null;
        int    afterSemi       = endIdx;
        while( afterSemi < closeIdx && tokens.get(
            afterSemi
        ).type == TokenType.WHITESPACE ) afterSemi++;
        if( afterSemi < closeIdx && isComment( tokens.get(afterSemi) ) ) {
            trailingComment = tokens.get(afterSemi).text;
            endIdx          = afterSemi + 1;
        }
        final ClassField field = new ClassField(
            leadingComments, modPhrase.toString(), name, type, hasValue, value, start, endIdx
        );
        field.trailingComment = trailingComment;

        return field;
    }

    /**
     * Skips forward from {@code start} (a member this pass doesn't recognize -- a method, an
     *  untyped field, etc.) past that member's own end: either a depth-0 `;` or, for a member
     *  with its own brace body (a method), the matching `}` of that body. Bracket-depth-aware over
     *  `(`/`[`/`{`/`<>` so nested structure never trips an early depth-0 match.
     */
    private int skipTopLevelMember(final List<Token> tokens, final int start, final int closeIdx)
    {
        int depth = 0;
        int i     = start;
        while(i < closeIdx) {
            final Token t = tokens.get(i);
            if( isPunct(
                t, "("
            ) || isPunct(
                t, "["
            ) || isPunct(
                t, "{"
            ) || t.type == TokenType.ANGLE_BRACKET_OPEN ) {
                ++depth;
            }
            else if( isPunct(
                t, ")"
            ) || isPunct(
                t, "]"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE ) {
                --depth;
            }
            else if( isPunct(t, "}") ) {
                --depth;
                if(depth == 0) return i + 1;
            }
            else if( depth == 0 && isPunct(t, ";") ) {
                return i + 1;
            }
            ++i;
        } // while

        return closeIdx;
    }

    private static String padRight(final String s, final int width)
    {
        if( s.length() >= width ) return s;
        final StringBuilder sb = new StringBuilder(s);
        while( sb.length() < width ) sb.append(' ');

        return sb.toString();
    }

    // ── §11.1 Union / intersection type spacing (`|`, `&`) ───────────────────────────
    /**
     * STYLE_JS_TS.md §11.1: `|`/`&` used as TS union/intersection type operators get ordinary
     * binary-operator spacing -- one space on both sides. TS-only (`lang.isTs`); a bare bitwise
     * `|`/`&` in JS expression position is out of this section's scope and deliberately left
     * untouched (no existing pass in this codebase spaces bitwise `|`/`&` either, confirmed via a
     * standalone harness before writing this method -- so applying this pass to TS-only, rather
     * than gating on "type position" specifically, is a conservative scope choice: TS's own
     * bitwise `|`/`&` usages get the same spacing as its union/intersection ones, both being
     * ordinary ASCII binary operators the style doc treats identically once JS's silence on
     * bitwise-operator spacing is accepted as a pre-existing, unrelated gap rather than something
     * this pass needs to work around). Compound tokens (`||`, `&&`, `|=`, `&=`, `||=`, `&&=`) are
     * already lexed as their own distinct multi-char ops and never match the single-char `|`/`&`
     * check here, so they're untouched by construction. Break-style on overflow (STYLE_JS_TS.md
     * §11.1's break-before-operator vs. break-after-operator worked examples) is preserved as
     * written -- this pass only touches the gap immediately around a same-line `|`/`&`, a gap
     * already blocked by an embedded NEWLINE is left alone by the same conservative bailout every
     * other pass in this file uses.
     */
    public String enforceUnionIntersectionSpacing(final List<Token> tokens)
    {
        if(!lang.isTs) return render( tokens, new HashMap<>() );
        final StringBuilder out             = new StringBuilder();
        final List<Token>   gap             = new ArrayList<>();
              Token         lastSignificant = null;
        final int           n               = tokens.size();
              int           i               = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean gapBlocked                    = gap.stream().anyMatch(
                g->isComment(g) || g.type == TokenType.NEWLINE || g.frozen
            ) || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean adjacentToUnionOrIntersection = isUnionOrIntersection(
                lastSignificant
            ) || isUnionOrIntersection(
                t
            );

            if(gapBlocked || !adjacentToUnionOrIntersection) {
                for(final Token g : gap) out.append(g.text);
            }
            else {
                out.append(' ');
            }

            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    private boolean isUnionOrIntersection(final Token t)
    {
        return t != null && ( isOp(t, "|") || isOp(t, "&") );
    }

    // ── §11 TypeScript type-annotation colon spacing ─────────────────────────────────
    /**
     * STYLE_JS_TS.md §11: a type-annotation colon (declarator, function/arrow parameter,
     * function return type) has no space before it and exactly one space after -- disambiguated
     * from an object-literal/destructuring-pattern key colon, a ternary-expression colon, and a
     * switch `case`/`default` label colon purely via local bracket-stack context, since the raw
     * token shape (`IDENTIFIER :`) is otherwise identical across all four. TS-only (JS has no
     * type annotations at all, so this is a no-op passthrough for {@code lang.isJs}). This is a
     * flat spacing pass only -- it does NOT implement STYLE_JS_TS.md §11's declaration-alignment-
     * grid column integration (RDD_KEY_183's `=`-aligned group behavior for consecutive
     * declarations); that remains unimplemented, see STATE_JS_TS.md. Conservative bailout
     * matching every other pass in this file: a gap touching a comment, NEWLINE, or frozen token
     * is left untouched, and a colon adjacent to any frozen token is never reclassified.
     *
     * <p>Classification (see {@link #classifyTypeColons}): a colon is a type colon when either
     * (a) its immediately preceding significant token is `)`, unless that `)` closes a
     * parenthesized `case (...):` label's own condition (return-type colon), or (b) its
     * immediately preceding significant token is an IDENTIFIER (or `?` tight after an
     * IDENTIFIER, for TS's `name?: type` optional-marker shape) whose own preceding context is
     * either inside a `(...)` parameter list directly after `(`/`,` (parameter colon), or at
     * statement level directly after `let`/`const`/`var`/`,` while still inside an open
     * declarator list (declaration colon). Every other shape -- object-literal/destructuring key
     * colons (enclosing bracket is a value `{`), ternary colons (preceding context is `?`, not
     * `(`/`,`/a declarator keyword), and case/default labels (preceding token is a literal or the
     * `default` keyword, never an IDENTIFIER or `)` from a plain unparenthesized label) -- falls
     * through unclassified and is left byte-for-byte as this pass found it.
     */
    public String enforceTypeColonSpacing(final List<Token> tokens)
    {
        if(!lang.isTs) return render( tokens, new HashMap<>() );
        final Set<Integer> typeColons = classifyTypeColons(tokens);

        final StringBuilder out             = new StringBuilder();
        final List<Token>   gap             = new ArrayList<>();
              Token         lastSignificant = null;
              int           lastSigIdx      = -1;
        final int           n               = tokens.size();
              int           i               = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean gapBlocked      = gap.stream().anyMatch(
                g->isComment(g) || g.type == TokenType.NEWLINE || g.frozen
            ) || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean beforeTypeColon = typeColons.contains(i);
            final boolean afterTypeColon  = lastSigIdx >= 0 && typeColons.contains(lastSigIdx);

            if( gapBlocked || (!beforeTypeColon && !afterTypeColon) ) {
                for(final Token g : gap) out.append(g.text);
            }
            else if(afterTypeColon) {
                out.append(' ');
            }
            // BeforeTypeColon && !gapBlocked && !afterTypeColon: gap dropped (tight before ':')

            gap.clear();
            out.append(t.text);
            lastSignificant = t;
            lastSigIdx      = i;
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    /**
     * STYLE_JS_TS.md §13: a generic type-argument list's `,` separator follows the same
     * no-space-before/one-space-after rule as any other comma (STYLE.md §3.1), but a plain
     * `Map<string,number>` never gets routed through any declaration/interface/enum rewrite
     * pass -- those only rebuild tokens inside their own specific rows/members, not an arbitrary
     * type annotation sitting elsewhere (e.g. a class-field type with no accompanying `=`). This
     * is a narrow, dedicated flat scan restricted to commas directly between an
     * ANGLE_BRACKET_OPEN and its matching ANGLE_BRACKET_CLOSE (tracked via a simple depth
     * counter, since {@code reclassifyAngleBrackets} has already retyped the brackets
     * themselves by this point in the pipeline), same conservative gap-bailout shape as this
     * file's other spacing passes.
     */
    public String enforceGenericArgumentCommaSpacing(final List<Token> tokens)
    {
        if(!lang.isTs) return render( tokens, new HashMap<>() );
        final StringBuilder out             = new StringBuilder();
        final List<Token>   gap             = new ArrayList<>();
              Token         lastSignificant = null;
              int           angleDepth      = 0;
        final int           n               = tokens.size();
              int           i               = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean gapBlocked = gap.stream().anyMatch(
                g->isComment(g) || g.type == TokenType.NEWLINE || g.frozen
            ) || (lastSignificant != null && lastSignificant.frozen) || t.frozen;
            final boolean afterComma = lastSignificant != null && isPunct(
                lastSignificant, ","
            ) && angleDepth > 0;

            if(gapBlocked || !afterComma) {
                for(final Token g : gap) out.append(g.text);
            }
            else {
                out.append(' ');
            }

            gap.clear();
            out.append(t.text);
                 if(t.type == TokenType.ANGLE_BRACKET_OPEN)  angleDepth++;
            else if(t.type == TokenType.ANGLE_BRACKET_CLOSE) angleDepth = Math.max(
                0, angleDepth - 1
            );
            lastSignificant = t;
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }

    /**
     * Bracket-stack scan producing the index set of every `:` token classified as a
     *  type-annotation colon. See {@link #enforceTypeColonSpacing}'s javadoc for the exact rules.
     */
    private Set<Integer> classifyTypeColons(final List<Token> tokens)
    {
        final Set<Integer>          result           = new HashSet<>();
        final Map<Integer, Integer> braceOpenToClose = matchBraces(tokens);
        final Set<Integer>          valueBraces      = new HashSet<>();
        final Map<Integer, String>  braceKind        = new HashMap<>();
        for( final Integer openIdx : braceOpenToClose.keySet() ) {
            if( isValuePrecededBrace(tokens, openIdx) ) valueBraces.add(openIdx);
            final String kind = classBraceKind(tokens, openIdx);
            if(kind != null) braceKind.put(openIdx, kind);
        }
        final Map<Integer, Integer> parenOpenToClose = matchParens(tokens);
        final Map<Integer, Integer> parenCloseToOpen = new HashMap<>();
        for( final Map.Entry<Integer, Integer> e : parenOpenToClose.entrySet() ) parenCloseToOpen.put(
            e.getValue(), e.getKey()
        );

        final Deque<String> stack        = new ArrayDeque<>();
              boolean       inDeclarator = false;

        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) continue;

            if( isPunct(t, ";") ) {
                inDeclarator = false;
            }
            else if( t.type == TokenType.KEYWORD
                    && ( "let".equals(t.text) || "const".equals(t.text) || "var".equals(t.text) )
                    && ( stack.isEmpty() || "BLOCK".equals( stack.peek() ) ) ) {
                inDeclarator = true;
            }
            else if( isOp(t, ":") && !t.frozen ) {
                final int prevIdx = prevSignificantIndex(tokens, i - 1);
                if( prevIdx >= 0 && !tokens.get(prevIdx).frozen ) {
                    if( isTypeColonAt(
                        tokens, i, prevIdx, stack, inDeclarator, parenCloseToOpen
                    ) ) result.add(
                        i
                    );
                } // if
            }

            if( isPunct(t, "(") ) {
                stack.push("PAREN");
            }
            else if( isPunct(t, "[") ) {
                stack.push("BRACKET");
            }
            else if( isPunct(t, "{") ) {
                final String kind = braceKind.get(i);
                if( "CLASS".equals(kind) ) stack.push("BLOCK:CLASS");
                else                       stack.push( valueBraces.contains(i) ? "OBJ" : "BLOCK" );
            }
            else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) {
                if( !stack.isEmpty() ) stack.pop();
            }
        } // for

        return result;
    }

    private boolean isTypeColonAt(
        final List<Token>           tokens,
        final int                   colonIdx,
        final int                   prevIdx,
        final Deque<String>         stack,
        final boolean               inDeclarator,
        final Map<Integer, Integer> parenCloseToOpen
    )
    {
        final Token prev = tokens.get(prevIdx);
        if( isPunct(
            prev, ")"
        ) ) return !isCaseLabelParen(
            tokens, prevIdx, parenCloseToOpen
        ) && !isGroupingExpressionParen(
            tokens, prevIdx, parenCloseToOpen
        );
        int idIdx = -1;
        if(prev.type == TokenType.IDENTIFIER) {
            idIdx = prevIdx;
        }
        else if( isOp(prev, "?") ) {
            final int maybeId = prevSignificantIndex(tokens, prevIdx - 1);
            if( maybeId >= 0 && tokens.get(maybeId).type == TokenType.IDENTIFIER ) idIdx = maybeId;
        }
        if(idIdx < 0) return false;
        final int     ctxIdx   = prevSignificantIndex(tokens, idIdx - 1);
        final Token   ctx      = ctxIdx >= 0 ? tokens.get(ctxIdx) : null;
        final boolean paramCtx = "PAREN".equals(
            stack.peek()
        ) && ctx != null && ( isPunct(
            ctx, "("
        ) || isPunct(
            ctx, ","
        ) );
        // A single-declarator `let`/`const`/`var` statement's own type colon (`let x: T = v;`) is
        // now grid-aligned by `JsTsDeclarationAlignmentRule` (STATE_JS_TS.md §11 declaration-
        // alignment-grid checkpoint) -- suppressing it here avoids this flat pass collapsing that
        // grid's own column padding back down to a single tight space right after it runs (Phase 4
        // runs after ScopePipelineCurly's grid pass, so without this exclusion the flat pass would
        // always win and silently destroy the alignment on every format). A multi-declarator
        // statement (`let a: number, b: string;`) is untouched by the grid (out of its scope, see
        // JsTsDeclarationAlignmentRule's class doc) and still needs this flat pass for both its
        // first declarator's colon (`ctx` is the `let`/`const`/`var` keyword itself, same as a
        // single-declarator statement) and every subsequent declarator's colon (`ctx` is `,`) --
        // distinguished from the single-declarator case via a forward scan for a depth-0 comma
        // before the statement's terminating `;`.
        final boolean declaratorCtx = inDeclarator && ( stack.isEmpty() || "BLOCK".equals(
            stack.peek()
        ) ) && ctx != null && ( isPunct(
            ctx, ","
        ) || ( ctx.type == TokenType.KEYWORD && ( "let".equals(
            ctx.text
        ) || "const".equals(
            ctx.text
        ) || "var".equals(
            ctx.text
        ) ) && hasTopLevelCommaBeforeSemicolon(
            tokens, colonIdx
        ) ) );
        // A class field's own `: type` colon (`private x: number;`) -- Checkpoint 7 deliberately
        // left this untouched ("class-field colons... deliberately NOT touched -- out of this
        // checkpoint's scope"); STATE_JS_TS.md's Checkpoint 21 bug 7 confirmed the gap is real
        // (never spaced at all, not merely unaligned). Recognized only when the enclosing frame is
        // specifically tagged `BLOCK:CLASS` (a class body brace, via `classBraceKind`) -- not any
        // ordinary `BLOCK` frame -- so a labeled statement's colon inside an ordinary function/
        // control-flow body (also a plain `BLOCK` frame, indistinguishable by shape alone) is never
        // misclassified. `ctx` must be the class body's own opening `{` (first member), a prior
        // member's terminating `;`, or one of §11.2's own modifier keywords (covers a run of
        // multiple modifiers, since each one's own preceding token is walked back to naturally).
        final boolean memberCtx = "BLOCK:CLASS".equals(
            stack.peek()
        ) && ctx != null && ( isPunct(
            ctx, "{"
        ) || isPunct(
            ctx, ";"
        ) || ( ctx.type == TokenType.KEYWORD && MODIFIER_PRIORITY.containsKey(
            ctx.text
        ) ) );

        return paramCtx || declaratorCtx || memberCtx;
    }

    /**
     * Classifies the `{` at {@code braceIdx} as the body brace of a `class` or `interface`
     *  declaration (returns {@code "CLASS"}/{@code "IFACE"}), or {@code null} if it's neither.
     *  Scans backward from {@code braceIdx} tracking `(`/`[`/`<...>` nesting (an `extends`/
     *  `implements` clause's own type arguments, e.g. `class Foo extends Bar<T> implements Baz {`,
     *  never contain a `{` themselves, so a depth-0 `{`/`}`/`;` encountered first means "no class/
     *  interface header found -- give up", not "false positive risk") until either a depth-0
     *  `class`/`interface` KEYWORD is found (match) or a depth-0 statement/block boundary (`{`,
     *  `}`, `;`) is hit first (no match, e.g. an ordinary function/control-flow body or an object-
     *  literal brace).
     */
    private String classBraceKind(final List<Token> tokens, final int braceIdx)
    {
        int depth = 0;
        for(int i = braceIdx - 1; i >= 0; --i) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) continue;
            if( isPunct(
                t, ")"
            ) || isPunct(
                t, "]"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE ) depth++;
            else if( isPunct(
                t, "("
            ) || isPunct(
                t, "["
            ) || t.type == TokenType.ANGLE_BRACKET_OPEN ) depth--;
            else if( depth == 0 && ( isPunct(
                t, "{"
            ) || isPunct(
                t, "}"
            ) || isPunct(
                t, ";"
            ) ) ) return null;
            else if( depth == 0 && t.type == TokenType.KEYWORD && "class".equals(
                t.text
            ) ) {
                // `class`/`interface` are also legal TS property/field names (e.g. `readonly
                // class: ExpressionWithTypeArguments & { readonly expression: ...; };`, seen in
                // microsoft/TypeScript's compiler/types.ts JSDocAugmentsTag/JSDocImplementsTag) --
                // the tokenizer still tags that occurrence as a KEYWORD token by text alone, so a
                // bare text match here misclassifies the field's own nested inline object-type-
                // literal `{...}` brace as if it were this "class" token's OWN class-declaration
                // body. A real class-declaration keyword is always immediately followed by the
                // class name (an IDENTIFIER); a field-name usage is immediately followed by `:`
                // (optionally `?` first) instead -- skip (keep walking backward) rather than
                // returning "CLASS" when that's the shape seen.
                if( isFieldNameKeywordUsage(tokens, i) ) continue;
                return "CLASS";
            }
            else if( depth == 0 && t.type == TokenType.KEYWORD && "interface".equals(
                t.text
            ) ) {
                if( isFieldNameKeywordUsage(tokens, i) ) continue;
                return "IFACE";
            }
        } // for

        return null;
    }

    /**
     * True iff the KEYWORD token at index {@code kwIdx} (text `class` or `interface`) is being
     *  used as a TS property/field name, not a class/interface declaration keyword -- i.e. the
     *  next significant (non-gap) token forward is `:` or `?` (an optional-marker before `:`)
     *  rather than an identifier (the declared class/interface's own name). See
     *  {@link #classBraceKind}'s call sites for the exact bug this guards against.
     */
    private boolean isFieldNameKeywordUsage(final List<Token> tokens, final int kwIdx)
    {
        for( int j = kwIdx + 1; j < tokens.size(); ++j ) {
            final Token t = tokens.get(j);
            if( isGapToken(t) ) continue;
            return isOp(t, ":") || isOp(t, "?") || isPunct(t, "?");
        } // for
        return false;
    }

    /**
     * True iff the `{` at {@code braceIdx} is the object-shaped body of a `type X = { ... };`
     *  alias declaration -- i.e. immediately preceded by `=`, whose own enclosing statement (walked
     *  backward the same depth-aware way as {@link #classBraceKind}) starts with the `type`
     *  KEYWORD. Excludes non-object type aliases (`type Status = "a" | "b";`, no brace at all) and
     *  ordinary `=`-preceded object-literal initializers (`const obj = { ... };`, whose enclosing
     *  statement starts with `const`/`let`/`var`, never `type`) by construction.
     */
    /**
     * True iff the `{` at {@code braceIdx} is immediately preceded by a TS legacy angle-bracket
     *  cast (`<Type>{...}`, e.g. `push(<WaitCallback>{doneCb: cb})`) -- i.e. a plain OP `>` whose
     *  matching plain OP `<` sits directly before a single (optionally dotted) type name, and that
     *  `<` itself follows a value-starting token (`(`, `,`, `=`, `return`, etc.). Such a `<...>`
     *  is never reclassified to ANGLE_BRACKET_OPEN/CLOSE by
     *  {@code TokenizerCurly.reclassifyAngleBrackets}, which only tracks a `<` preceded by an
     *  IDENTIFIER (a generic clause) -- so it's still a plain OP pair by the time this runs.
     */
    private boolean isLegacyCastBrace(final List<Token> tokens, final int braceIdx)
    {
        final int closeAngleIdx = prevSignificantIndex(tokens, braceIdx - 1);
        if( closeAngleIdx < 0 || !isOp( tokens.get(closeAngleIdx), ">" ) ) return false;
        int cursor = prevSignificantIndex(tokens, closeAngleIdx - 1);
        if( cursor < 0 || tokens.get(cursor).type != TokenType.IDENTIFIER ) return false;
        // Walk back over a dotted type name (`<Foo.Bar>`), same chain-walk shape as the arrow-
        // parameter dotted-type bail-out above.
        while(true) {
            final int dotIdx = prevSignificantIndex(tokens, cursor - 1);
            if( dotIdx < 0 || !isOp( tokens.get(dotIdx), "." ) ) break;
            final int identIdx = prevSignificantIndex(tokens, dotIdx - 1);
            if( identIdx < 0 || tokens.get(identIdx).type != TokenType.IDENTIFIER ) break;
            cursor = identIdx;
        } // while
        final int openAngleIdx = prevSignificantIndex(tokens, cursor - 1);
        if( openAngleIdx < 0 || !isOp( tokens.get(openAngleIdx), "<" ) ) return false;
        final int beforeOpenIdx = prevSignificantIndex(tokens, openAngleIdx - 1);
        if(beforeOpenIdx < 0) return true; // Cast at the very start of the token stream
        final Token beforeOpen = tokens.get(beforeOpenIdx);

        return isPunct(beforeOpen, "(") || isPunct(beforeOpen, "[") || isPunct(beforeOpen, ",")
                || isOp(beforeOpen, "=") || isOp(beforeOpen, ":") || isOp(beforeOpen, "??")
                || isOp(beforeOpen, "||") || isOp(beforeOpen, "&&") || isOp(beforeOpen, "?")
                || ( beforeOpen.type == TokenType.KEYWORD
                        && ( "return".equals(beforeOpen.text) || "yield".equals(beforeOpen.text)
                                || "throw".equals(beforeOpen.text) ) );
    }

    private boolean isTypeAliasObjectBrace(final List<Token> tokens, final int braceIdx)
    {
        final int prevIdx = prevSignificantIndex(tokens, braceIdx - 1);
        if( prevIdx < 0 || !isOp( tokens.get(prevIdx), "=" ) ) return false;
        // `type X = { ... } | { ... }` (a union of two-or-more inline object types on the RHS,
        // vuejs/core dogfood `ref.test-d.ts`/`watch.test-d.ts`: `type Steps = { step: '1' } |
        // { step: '2' }`) -- the FIRST `{...}` here is preceded by `=` exactly like a plain
        // single-object type alias, but it is not the whole RHS: treating it as one (resetDepth
        // + needsSemicolon both true, same as the real single-object case just below) makes the
        // depth-0 boundary land on this brace's own `}` instead of the real statement end, which
        // then corrupts every subsequent line's indentation via the alignment-grid/depth-reset
        // interaction (not just a missing/extra semicolon). Detected by matching this brace's own
        // close and checking whether a union/intersection operator follows it -- if so, fall
        // through to the ordinary isValue-brace classification below instead (conservative: no
        // member `;`-alignment for a union member's object type, but no depth corruption either).
        int matchDepth = 0;
        int closeIdx   = -1;
        for( int i = braceIdx; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( isPunct(t, "{") ) {
                ++matchDepth;
            }
            else if( isPunct(t, "}") ) {
                --matchDepth;
                if(matchDepth == 0) {
                    closeIdx = i;
                    break;
                }
            }
        } // for
        if(closeIdx >= 0) {
            final int afterClose = nextSignificantIndex(tokens, closeIdx + 1);
            if( afterClose >= 0 && ( isOp(
                tokens.get(afterClose), "|"
            ) || isOp(
                tokens.get(afterClose), "&"
            ) ) ) return false;
        } // if
        int depth = 0;
        for(int i = prevIdx - 1; i >= 0; --i) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) continue;
            if( isPunct(
                t, ")"
            ) || isPunct(
                t, "]"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE ) depth++;
            else if( isPunct(
                t, "("
            ) || isPunct(
                t, "["
            ) || t.type == TokenType.ANGLE_BRACKET_OPEN ) depth--;
            else if( depth == 0 && ( isPunct(
                t, ";"
            ) || isPunct(
                t, "{"
            ) || isPunct(
                t, "}"
            ) ) ) return false;
            else if( depth == 0 && t.type == TokenType.KEYWORD && "type".equals(
                t.text
            ) ) return true;
        } // for

        return false;
    }

    // ── §14 interface/type-alias member `:` alignment ────────────────────────────────
    /**
     * One parsed `interface`/object-shaped `type`-alias member: optional leading (own-line)
     *  comments, the member's rendered "name phrase" (an optional {@code readonly } prefix plus the
     *  property name plus an optional trailing {@code ?}), its type expression's raw text (trimmed,
     *  internal spacing preserved exactly as originally written), and an optional same-line trailing
     *  comment -- structurally the direct TS analog of {@link EnumMember}, same fields modulo the
     *  `=`-value pair being a `:`-type pair instead
     */
    private static final class InterfaceMember {

        private final List<String> leadingComments;
        private final String       namePhrase;
        private final String       type;
        private       String       trailingComment;

        InterfaceMember(
            final List<String> leadingComments,
            final String       namePhrase,
            final String       type
        )
        {
            this.leadingComments = leadingComments;
            this.namePhrase      = namePhrase;
            this.type            = type;
        }

    } // class InterfaceMember

    /**
     * STYLE_JS_TS.md §14: an `interface` body or an object-shaped `type X = { ... };` alias body
     * aligns every member's `:` in a shared column, same alignment concept as §11's declaration-
     * alignment grid -- "member/property lists inside an `interface` or object-shaped `type` alias
     * align their `:` the same way §11 above aligns parameter/variable type annotations". Unlike
     * §12's enum bodies, this does NOT force a same-line member list onto separate lines -- the
     * style doc's own worked examples are already one-member-per-line as written (real-world
     * interface/type-alias bodies overwhelmingly are too), so a same-line member list (e.g. a
     * degenerate one-liner `interface P { a: number; b: string; }`) is judged out of this pass's
     * scope and left completely untouched (conservative, "never guess past an unrecognized shape"
     * posture, matching every other pass in this file) -- only re-derives column padding for a body
     * whose members are already one-per-line. TS-only (`lang.isTs`); JS has no `interface`/type-
     * annotated `type` alias syntax at all.
     *
     * <p>Member shape recognized (see {@link #parseInterfaceMembers}): an optional {@code readonly}
     * modifier, a plain identifier name, an optional {@code ?} optional-marker, {@code :}, a type
     * expression (bracket-depth-aware scan so nested `(`/`[`/`{`/`<>` — e.g. a function-type member
     * like {@code onSelect : (id: string) => void;} — don't prematurely end the scan), and a
     * terminating `;`. Any other member shape in the same body (a method-signature member with no
     * colon directly after its name, e.g. {@code onSelect(id: string): void;}; an index signature,
     * e.g. {@code [key: string]: number;}; a member whose type expression spans a NEWLINE) makes
     * the WHOLE body bail out untouched rather than partially aligning it -- same conservative
     * per-construct bailout {@link #parseEnumMembers} already uses.
     */
    public String enforceInterfaceTypeAliasMemberColonAlignment(final List<Token> tokens)
    {
        if(!lang.isTs) return render( tokens, new HashMap<>() );
        final Map<Integer, Integer> braces = matchBraces(tokens);
        final List<int[]>           bodies = new ArrayList<>();
        for( final Map.Entry<Integer, Integer> e : braces.entrySet() ) {
            final int openIdx = e.getKey();
            if( "IFACE".equals(
                classBraceKind(tokens, openIdx)
            ) || isTypeAliasObjectBrace(
                tokens, openIdx
            ) ) {
                bodies.add( new int[] { openIdx, e.getValue() } );
            }
        } // for
        if( bodies.isEmpty() ) {
            final StringBuilder sb = new StringBuilder();
            for(final Token t : tokens) sb.append(t.text);
            return sb.toString();
        }
        bodies.sort( (a, b) -> Integer.compare( a[0], b[0] ) );

        final StringBuilder out    = new StringBuilder();
              int           cursor = 0;
        for( final int[] body : bodies ) {
            final int openIdx  = body[0];
            final int closeIdx = body[1];
            if( MiscRuleCore.anyFrozen(tokens, openIdx, closeIdx + 1) ) continue;
            final String rewritten = rewriteInterfaceBody(tokens, openIdx, closeIdx);
            if(rewritten == null) continue;
            for(int i = cursor; i <= openIdx; ++i) out.append( tokens.get(i).text );
            out.append(rewritten);
            cursor = closeIdx; // Resume from the closing `}` token itself
        } // for
        for( int i = cursor; i < tokens.size(); ++i ) out.append( tokens.get(i).text );

        return out.toString();
    }

    /**
     * Rewrites the interior of one `interface`/type-alias body into the always-`: `-column-aligned
     *  layout described on {@link #enforceInterfaceTypeAliasMemberColonAlignment}, or returns
     *  {@code null} if {@link #parseInterfaceMembers} can't safely parse it
     */
    private String rewriteInterfaceBody(
        final List<Token> tokens,
        final int         openIdx,
        final int         closeIdx
    )
    {
        final List<InterfaceMember> members = parseInterfaceMembers(tokens, openIdx, closeIdx);
        if(members == null) return null;
        int maxNameWidth = 0;
        for(final InterfaceMember m : members) maxNameWidth = Math.max(
            maxNameWidth, m.namePhrase.length()
        );
        final String        bodyIndent   = lineIndent(tokens, openIdx);
        final String        memberIndent = bodyIndent + defaultIndentUnit;
        final StringBuilder sb           = new StringBuilder();
        for(final InterfaceMember m : members) {
            for(final String comment : m.leadingComments) sb.append(
                '\n'
            ).append(
                memberIndent
            ) .append(
                reindentLeadingComment(comment, memberIndent)
            );
            sb.append('\n').append(memberIndent);
            sb.append(
                padRight(m.namePhrase, maxNameWidth)
            ).append(
                " : "
            ).append(
                m.type
            ).append(
                ';'
            );
            if(m.trailingComment != null) sb.append("  ").append(m.trailingComment);
        } // for
        sb.append('\n').append(bodyIndent);

        return sb.toString();
    }

    /**
     * Parses an `interface`/type-alias body's interior (between {@code openIdx} and
     *  {@code closeIdx}, exclusive) into an ordered list of {@link InterfaceMember}s via a single
     *  forward scan, mirroring {@link #parseEnumMembers}'s shape but with `;`-terminated `name :
     *  type` members instead of `,`-terminated `name [= value]` ones. Returns {@code null} (bail,
     *  leave the whole body untouched) on any unrecognized member shape, a member value spanning a
     *  NEWLINE, or a floating comment with no member following it.
     */
    private List<InterfaceMember> parseInterfaceMembers(
        final List<Token> tokens,
        final int         openIdx,
        final int         closeIdx
    )
    {
        final List<InterfaceMember> members                 = new ArrayList<>();
        final List<String>          leadingComments         = new ArrayList<>();
              boolean               sameLineAsPrevSeparator = false;
              int                   i                       = openIdx + 1;
        while(i < closeIdx) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE) {
                ++i;
                continue;
            }
            if(t.type == TokenType.NEWLINE) {
                sameLineAsPrevSeparator = false;
                ++i;
                continue;
            }
            if( isComment(t) ) {
                if( sameLineAsPrevSeparator && !members.isEmpty() ) members.get(
                    members.size() - 1
                ).trailingComment = t.text;
                else leadingComments.add(t.text);
                ++i;
                continue;
            } // if
            final StringBuilder namePhrase = new StringBuilder();
            while( i < closeIdx && tokens.get(
                i
            ).type == TokenType.KEYWORD && "readonly".equals(
                tokens.get(i).text
            ) ) {
                namePhrase.append( tokens.get(i).text ).append(' ');
                ++i;
                while( i < closeIdx && tokens.get(i).type == TokenType.WHITESPACE ) i++;
            }
            if( i >= closeIdx || tokens.get(
                i
            ).type != TokenType.IDENTIFIER ) return null; // Not a recognized member shape (index signature, method signature, ...)
            namePhrase.append( tokens.get(i).text );
            ++i;
            if( i < closeIdx && isOp( tokens.get(i), "?" ) ) {
                namePhrase.append('?');
                ++i;
            }
            while( i < closeIdx && tokens.get(i).type == TokenType.WHITESPACE ) i++;
            if( i >= closeIdx || !isOp(
                tokens.get(i), ":"
            ) ) return null; // E.g. a method-signature member (`onSelect(id: string): void;`)
            ++i;
            while( i < closeIdx && tokens.get(i).type == TokenType.WHITESPACE ) i++;
            final StringBuilder typeBuf = new StringBuilder();
                  int           depth   = 0;
            while(i < closeIdx) {
                final Token vt = tokens.get(i);
                if( depth == 0 && isPunct(vt, ";") ) break;
                if(vt.type == TokenType.NEWLINE) return null; // Multi-line member type expression -- rare, not handled
                if( isPunct(
                    vt, "("
                ) || isPunct(
                    vt, "["
                ) || isPunct(
                    vt, "{"
                ) || vt.type == TokenType.ANGLE_BRACKET_OPEN ) depth++;
                else if( isPunct(
                    vt, ")"
                ) || isPunct(
                    vt, "]"
                ) || isPunct(
                    vt, "}"
                ) || vt.type == TokenType.ANGLE_BRACKET_CLOSE ) depth--;
                typeBuf.append(vt.text);
                ++i;
            } // while
            if(i >= closeIdx) return null; // No terminating ';' found -- malformed/unsupported
            ++i; // Skip ';'
            final String type = typeBuf.toString().trim();
            if( type.isEmpty() ) return null;
            while( i < closeIdx && tokens.get(i).type == TokenType.WHITESPACE ) i++;
            String trailingComment = null;
            if( i < closeIdx && isComment( tokens.get(i) ) ) {
                trailingComment = tokens.get(i).text;
                ++i;
            }
            final InterfaceMember member = new InterfaceMember(
                new ArrayList<>(leadingComments), namePhrase.toString(), type
            );
            member.trailingComment = trailingComment;
            leadingComments.clear();
            members.add(member);
            sameLineAsPrevSeparator = true;
        } // while
        if( !leadingComments.isEmpty() ) return null; // A floating comment with no member attached after it -- don't drop it
        return members.isEmpty() ? null : members;
    }

    /**
     * Depth-aware forward scan (over `(`/`[`/`{` nesting) from {@code fromIdx} for a depth-0
     *  `,` reached before the statement's own depth-0 terminating `;` -- true iff the enclosing
     *  `let`/`const`/`var` statement is a multi-declarator statement, per {@link
     *  #isTypeColonAt}'s declarator-colon suppression note above
     */
    private boolean hasTopLevelCommaBeforeSemicolon(final List<Token> tokens, final int fromIdx)
    {
        int depth = 0;
        for( int i = fromIdx + 1; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) continue;
                 if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) depth++;
            else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) depth--;
            else if( depth == 0 && isPunct(t, ",") ) return true;
            else if( depth == 0 && isPunct(t, ";") ) return false;
        } // for

        return false;
    }

    /**
     * True if {@code closeParenIdx} closes a `case (...):`-style parenthesized case-label
     *  condition -- i.e. the token immediately before the matching `(` is the `case` keyword --
     *  the one shape where a bare `)` immediately followed by `:` is NOT a return-type colon.
     */
    private boolean isCaseLabelParen(
        final List<Token>           tokens,
        final int                   closeParenIdx,
        final Map<Integer, Integer> parenCloseToOpen
    )
    {
        final Integer openIdx = parenCloseToOpen.get(closeParenIdx);
        if(openIdx == null) return false;
        final int beforeOpen = prevSignificantIndex(tokens, openIdx - 1);

        return beforeOpen >= 0 && tokens.get(beforeOpen).type == TokenType.KEYWORD
                && "case".equals( tokens.get(beforeOpen).text );
    }

    /**
     * True if {@code closeParenIdx} closes a plain grouping/sub-expression `(...)` -- e.g. a
     *  parenthesized ternary (`cond ? (a ? b : c) : d`) or any other parenthesized expression --
     *  rather than a function/arrow/constructor parameter list, the other shape where a bare `)`
     *  immediately followed by `:` is NOT a return-type colon. A real parameter list's `(` is
     *  always immediately preceded by the function/method name (IDENTIFIER), a generic clause's
     *  closing `>` (`foo<T>(...)`), a signature keyword (`function`/`constructor`/`new`/`get`/
     *  `set`/etc.), or nothing at all (an anonymous function expression's very first token) --
     *  never by an operator. A grouping paren, by contrast, always sits in the middle of an
     *  expression and is therefore always immediately preceded by an operator (`?`, `:`, `&&`,
     *  `||`, `=`, `return`, etc.) (vuejs/core dogfood, `shared/src/typeUtils.ts`'s
     *  `T extends object ? (keyof T extends K ? true : false) : false`, where the outer ternary's
     *  own `:` was misclassified as a return-type colon because its preceding `)` closes the
     *  parenthesized nested-ternary sub-expression, not a parameter list).
     */
    private boolean isGroupingExpressionParen(
        final List<Token>           tokens,
        final int                   closeParenIdx,
        final Map<Integer, Integer> parenCloseToOpen
    )
    {
        final Integer openIdx = parenCloseToOpen.get(closeParenIdx);
        if(openIdx == null) return false;
        final int beforeOpen = prevSignificantIndex(tokens, openIdx - 1);
        if(beforeOpen < 0) return false;
        final Token before = tokens.get(beforeOpen);

        return before.type == TokenType.OP && !"=>".equals(before.text);
    }

    /**
     * Same "is this `{` a value/pattern brace" heuristic as {@link #classifyBraces}'s
     *  {@code isValue} local, duplicated (not shared) since it's used from a different pass with
     *  different bookkeeping needs (a bracket-kind stack, not resetDepth/needsSemicolon maps)
     */
    private boolean isValuePrecededBrace(final List<Token> tokens, final int openIdx)
    {
        final int   prevIdx = prevSignificantIndex(tokens, openIdx - 1);
        final Token prev    = prevIdx >= 0 ? tokens.get(prevIdx) : null;
        if(prev == null) return false;

        return isOp(prev, "=>") || isOp(prev, "=") || isPunct(prev, "(") || isPunct(prev, "[")
                || isPunct(prev, ",") || isOp(prev, ":") || isOp(prev, "??") || isOp(prev, "||")
                || isOp(prev, "&&") || isOp(prev, "?") || isOp(prev, "...")
                || ( prev.type == TokenType.KEYWORD && ( "return".equals(
                    prev.text
                ) || "yield".equals(
                    prev.text
                )
                        || "throw".equals(prev.text) || "typeof".equals(prev.text)
                        || "const".equals(
                            prev.text
                        ) || "let".equals(
                            prev.text
                        ) || "var".equals(
                            prev.text
                        ) ) );
    }

    // ── §9 Decorators ────────────────────────────────────────────────────────────

    /**
     * STYLE_JS_TS.md §9: {@code @} binds tight to the decorator name, no space, "same as any
     * other unary prefix". Confirmed via a standalone harness before writing this method that
     * this is <b>not</b> already free from any existing generic pass -- a deliberately mis-spaced
     * {@code @ Inject(TOKEN)} round-tripped completely untouched, and the generic
     * `needsSpaceBetween` join used elsewhere in this codebase defaults to inserting a space
     * between an `@` OP token and a following IDENTIFIER (no existing "tight unary prefix"
     * exception for `@`, unlike e.g. `!`/`~`). Flat gap-normalizing scan, same conservative
     * bailout shape as every other spacing pass in this file: a gap containing a comment, a
     * NEWLINE, or a frozen token is left untouched. `@` only ever appears in valid JS/TS source
     * as a decorator marker (no bitwise/other operator use, unlike Kotlin's overloaded operators),
     * so no additional context check is needed before tightening every occurrence.
     */
    public String enforceDecoratorTightAtSpacing(final List<Token> tokens)
    {
        final StringBuilder out = new StringBuilder();
              int           i   = 0;
        final int           n   = tokens.size();
        while(i < n) {
            final Token t = tokens.get(i);
            out.append(t.text);
            if( isOp(t, "@") && !t.frozen ) {
                final int nextSig = nextSignificantIndex(tokens, i + 1);
                if(nextSig > i + 1) {
                    final List<Token> gap        = tokens.subList(i + 1, nextSig);
                    final boolean     gapBlocked = gap.stream().anyMatch(
                        g->isComment(g) || g.type == TokenType.NEWLINE || g.frozen
                    );
                    if(!gapBlocked) {
                        i = nextSig;
                        continue;
                    }
                } // if
            } // if
            ++i;
        } // while

        return out.toString();
    }

    /**
     * STYLE_JS_TS.md §9's overflow cascade: when a decorator plus the target it precedes would
     * exceed {@link #lineLengthLimit} on one line, drop the decorator to its own line first --
     * keeping the target on the next line at the same indentation -- before falling back to
     * wrapping the decorator's own argument list (that second step needs no new code here: a
     * decorator's {@code @Name(args)} call already matches the generic
     * {@code MiscRuleCurly.enforceCallLineBreaking} scan's "IDENTIFIER (" candidate shape, so an
     * overlong decorator-with-args, once alone on its own line, is already wrapped by that
     * existing pass same as any other overlong call). This method only ever does the first step
     * (own-line drop) -- it never touches the decorator's own argument list.
     *
     * <p>Only an <b>inline</b> decorator (its own line still holds more content after it -- no
     * NEWLINE between the decorator's own closing token and what follows) is a candidate; a
     * decorator already on its own line is left completely alone, matching §9's own "the
     * formatter never moves a decorator from one placement to the other" placement-preservation
     * rule -- this pass only ever inserts a break for an inline decorator that doesn't fit, never
     * removes one that's already own-line, and never merges an own-line decorator back inline
     * either way.
     *
     * <p>Must run in Phase 1, before {@code enforceComplexityPadding}/{@code
     * enforceCallLineBreaking} (same ordering constraint as this file's other Phase 1 structural
     * passes, e.g. {@code enforceArrowFunctionParameterParens}) -- inserting a line break here
     * changes what "the rest of this line" even is for those later width-driven passes, and they
     * need to see the post-split shape on the very first format pass for the "does it fit"
     * decision to stay stable across reformats.
     */
    public String enforceDecoratorOverflowCascade(final List<Token> tokens)
    {
        final Map<Integer, Integer> parenOpenToClose = matchParens(tokens);
        final StringBuilder         out              = new StringBuilder();
              int                   i                = 0;
        final int                   n                = tokens.size();
        while(i < n) {
            final Token t = tokens.get(i);
            if( isOp(t, "@") && !t.frozen ) {
                final int     decoratorEnd   = findDecoratorEnd(tokens, i, parenOpenToClose);
                final int     nextSig        = decoratorEnd >= 0 ? nextSignificantIndex(
                    tokens, decoratorEnd + 1
                ) : -1;
                final boolean alreadyOwnLine = nextSig >= 0 && hasNewlineBetween(
                    tokens, decoratorEnd + 1, nextSig
                );
                if( decoratorEnd >= 0 && nextSig >= 0 && !alreadyOwnLine && !MiscRuleCore.anyFrozen(
                    tokens, i, decoratorEnd + 1
                ) ) {
                    final String indent    = lineIndent(tokens, i);
                    final int    lineEnd   = lineEndIndex(tokens, nextSig);
                    final String wholeLine = indent + collapseTokensToOneLine(
                        tokens.subList( lineStartTokenIndex(tokens, i), lineEnd )
                    );
                    if( wholeLine.length() > lineLengthLimit ) {
                        // Drop: emit the decorator itself, then a NEWLINE + the same
                        // indentation, then let the loop continue from the target
                        out.append(
                            collapseTokensToOneLine( tokens.subList(i, decoratorEnd + 1) )
                        );
                        out.append('\n').append(indent);
                        i = nextSig;
                        continue;
                    } // if
                } // if
            } // if
            out.append(t.text);
            ++i;
        } // while

        return out.toString();
    }

    /**
     * Index of the last token of a decorator application starting at {@code atIdx} (the
     *  {@code @} token itself): the decorator name identifier for a bare {@code @Name}, or the
     *  matching {@code )} for {@code @Name(args)}. Returns -1 if the shape isn't recognized (no
     *  identifier immediately follows {@code @}).
     */
    private int findDecoratorEnd(
        final List<Token>           tokens,
        final int                   atIdx,
        final Map<Integer, Integer> parenOpenToClose
    )
    {
        final int nameIdx = nextSignificantIndex(tokens, atIdx + 1);
        if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) return -1;
        // A qualified decorator name (`@ns.Name`) walks forward over `.identifier` pairs too.
        int end = nameIdx;
        while(true) {
            final int dotIdx = nextSignificantIndex(tokens, end + 1);
            if( dotIdx < 0 || !isOp( tokens.get(dotIdx), "." ) ) break;
            final int idIdx = nextSignificantIndex(tokens, dotIdx + 1);
            if( idIdx < 0 || tokens.get(idIdx).type != TokenType.IDENTIFIER ) break;
            end = idIdx;
        } // while
        final int afterName = nextSignificantIndex(tokens, end + 1);
        if( afterName >= 0 && isPunct( tokens.get(afterName), "(" ) ) {
            final Integer closeIdx = parenOpenToClose.get(afterName);
            if(closeIdx != null) return closeIdx;
        }

        return end;
    }

    /** True if any {@code NEWLINE} token appears in {@code [from, to)} */
    private boolean hasNewlineBetween(final List<Token> tokens, final int from, final int to)
    {
        for( int i = from; i < to && i < tokens.size(); ++i ) {
            if( tokens.get(i).type == TokenType.NEWLINE ) return true;
        }

        return false;
    }

    /** First token index of the physical line containing token {@code idx} */
    private int lineStartTokenIndex(final List<Token> tokens, final int idx)
    {
        for(int i = idx; i >= 0; --i) {
            if( tokens.get(i).type == TokenType.NEWLINE ) return i + 1;
        }

        return 0;
    }

    /**
     * Index one past the last token of the physical line containing token {@code idx} (i.e. the
     *  index of the line's own trailing {@code NEWLINE}, or the token count if the file ends
     *  without one).
     */
    private int lineEndIndex(final List<Token> tokens, final int idx)
    {
        for( int i = idx; i < tokens.size(); ++i ) {
            if( tokens.get(i).type == TokenType.NEWLINE ) return i;
        }

        return tokens.size();
    }

    /**
     * Plain concatenation of each token's own raw text across {@code [from, to)} of a token
     *  sublist -- used only for this method's own line-length estimate and for re-emitting the
     *  decorator's own span verbatim when dropping it to its own line; not a general-purpose
     *  renderer (does not touch spacing, unlike {@code MiscRuleCurly.collapseTokensToOneLine}).
     */
    private String collapseTokensToOneLine(final List<Token> slice)
    {
        final StringBuilder sb = new StringBuilder();
        for(final Token t : slice) sb.append(t.text);

        return sb.toString();
    }

    // ── §15 Import ordering ──────────────────────────────────────────────────────────
    /**
     * The three fixed §15 bucket names -- {@code groupOrder} must be an exact permutation of
     *  this set, mirroring {@code JavaSpecificRule.IMPORT_GROUP_KEYS}'s validation posture.
     */
    private static final Set<String> IMPORT_GROUP_KEYS = new HashSet<>( Arrays.asList(
        "builtin", "third-party", "local"
    ) );

    /**
     * Reasonably complete (not exhaustive-to-the-letter) list of Node built-in module names,
     *  matched against a bare specifier's own leading path segment (e.g. {@code "fs/promises"}
     *  still classifies as {@code builtin} via its {@code "fs"} segment). A {@code node:}-prefixed
     *  specifier is always {@code builtin} regardless of list membership (RDD_KEY_195's
     *  classification order, leg 1) -- unambiguous by ecosystem convention, doesn't rely on this
     *  list being complete.
     */
    private static final Set<String> NODE_BUILTIN_MODULES = new HashSet<>( Arrays.asList(
        "assert",
        "async_hooks",
        "buffer",
        "child_process",
        "cluster",
        "console",
        "constants",
        "crypto",
        "dgram",
        "diagnostics_channel",
        "dns",
        "domain",
        "events",
        "fs",
        "http",
        "http2",
        "https",
        "inspector",
        "module",
        "net",
        "os",
        "path",
        "perf_hooks",
        "process",
        "punycode",
        "querystring",
        "readline",
        "repl",
        "stream",
        "string_decoder",
        "sys",
        "test",
        "timers",
        "tls",
        "trace_events",
        "tty",
        "url",
        "util",
        "v8",
        "vm",
        "wasi",
        "worker_threads",
        "zlib"
    ) );

    /**
     * One successfully-parsed {@code import ...;} declaration -- {@code startIdx}/{@code
     *  semicolonIdx} bound its own original token span, re-emitted verbatim (not canonically
     *  regenerated, unlike Java's import-ordering pass) when reordering, since JS/TS import
     *  clauses (named-import lists, {@code type} modifier, default+namespace combinations) have
     *  enough internal shape variety that preserving the original text is safer than trying to
     *  reconstruct it.
     */
    private static final class ParsedJsImport {

        final int startIdx;
        final int semicolonIdx;
        // Exclusive end of this import's own emitted span -- normally semicolonIdx + 1, but
        // extended past a trailing same-line comment (RDD_KEY_197) so that comment travels with
        // this import through reordering instead of being treated as a floating comment between
        // imports
        final int    endIdx;
        final String path;

        ParsedJsImport(
            final int    startIdx,
            final int    semicolonIdx,
            final int    endIdx,
            final String path
        )
        {
            this.startIdx     = startIdx;
            this.semicolonIdx = semicolonIdx;
            this.endIdx       = endIdx;
            this.path         = path;
        }

    } // class ParsedJsImport

    /**
     * STYLE_JS_TS.md §15: groups and sorts top-level {@code import} declarations into {@code
     * groupOrder}'s buckets (a permutation of {@link #IMPORT_GROUP_KEYS}), alphabetically within
     * each bucket if {@code sortAlphabetically}, with {@code blankLines + 1} newlines between
     * non-empty groups -- same group/blank-line shape as {@code JavaSpecificRule
     * .enforceImportOrdering}. Only real, top-level (brace-depth 0) {@code import} declarations
     * are recognized -- a dynamic {@code import(...)} call expression or {@code import.meta} is
     * left completely alone (not even inspected for reordering purposes), and {@code export ...
     * from "...";} re-export statements are out of this pass's scope entirely (not covered by
     * STYLE_JS_TS.md §15's own worked example, which only shows {@code import}).
     *
     * <p>Classification (RDD_KEY_195, applied in this priority order): (1) {@code node:}-prefixed
     * or the specifier's leading path segment matches {@link #NODE_BUILTIN_MODULES} -- {@code
     * builtin}; (2) starts with {@code ./} or {@code ../} -- {@code local}; (3) everything else --
     * {@code third-party}. Per RDD_KEY_195, this is a known, accepted simplification: a bare
     * specifier resolved to the project's own source tree via a bundler/tsconfig {@code
     * baseUrl}/{@code paths} mechanism (e.g. {@code "components/Widget"}) is classified {@code
     * third-party}, not {@code local} -- there is no source-root config concept in this formatter
     * to tell the two apart.
     *
     * <p>Bails the entire pass (returns {@code tokens} byte-for-byte unchanged) on: a comment
     * found anywhere inside an import declaration or floating between two otherwise-clean import
     * declarations (never silently drop a comment via reordering, same posture as Java's own
     * pass); any frozen span inside an import declaration; a declaration with no discoverable
     * module-path {@code STRING} token or no terminating {@code ;} before EOF. A file with zero
     * recognized import declarations is also a no-op.
     *
     * @param groupOrder must be a permutation of exactly {@link #IMPORT_GROUP_KEYS} -- a
     *        config-validation precondition, so an invalid value throws rather than silently
     *        dropping a bucket's imports
     */
    public String enforceImportOrdering(
        final List<Token>  tokens,
        final List<String> groupOrder,
        final boolean      sortAlphabetically,
        final int          blankLines
    )
    {
        if( !new HashSet<>(groupOrder).equals(
            IMPORT_GROUP_KEYS
        ) || groupOrder.size() != IMPORT_GROUP_KEYS.size() ) throw new IllegalArgumentException(
            "groupOrder must be a permutation of " + IMPORT_GROUP_KEYS + ", got: " + groupOrder
        );

        int     depth          = 0;
        int     firstImportIdx = -1;
        int     prevEndIdx     = -1;
        int     lastEndIdx     = -1;
        boolean blocked        = false;
        // RDD_KEY_197: a standalone comment between two imports no longer bails the whole pass --
        // it segments the import list instead (same "blank line breaks the group" precedent used
        // elsewhere in this codebase). Each segment is grouped/sorted independently; the comment's
        // own original text (verbatim, including its surrounding gap) is preserved in place
        // between the two segments it separated.
        final List<List<ParsedJsImport>> segments         = new ArrayList<>();
        final List<String>               interSegmentGaps = new ArrayList<>();
              List<ParsedJsImport>       currentSegment   = new ArrayList<>();
        final int                        n                = tokens.size();
              int                        i                = 0;
        while(i < n) {
            final Token t = tokens.get(i);
            if( isPunct(t, "{") ) {
                ++depth;
                ++i;
                continue;
            }
            if( isPunct(t, "}") ) {
                --depth;
                ++i;
                continue;
            }
            if( depth == 0 && t.type == TokenType.KEYWORD && "import".equals(t.text) ) {
                final int nextSig = nextSignificantIndex(tokens, i + 1);
                if( nextSig >= 0 && ( isPunct(
                    tokens.get(nextSig), "("
                ) || isPunct(
                    tokens.get(nextSig), "."
                ) ) ) {
                    // Dynamic `import(...)` call or `import.meta` -- not a declaration, leave
                    // completely alone.
                    ++i;
                    continue;
                }
                if(firstImportIdx < 0) {
                    firstImportIdx = i;
                }
                else if( hasCommentBetween(tokens, prevEndIdx, i) ) {
                    // A trailing same-line comment on the previous import is already folded into
                    // that import's own endIdx below, so any comment still found in this gap is a
                    // standalone one -- a segment boundary, not a bail condition
                    segments.add(currentSegment);
                    currentSegment = new ArrayList<>();
                    interSegmentGaps.add(
                        collapseTokensToOneLine( tokens.subList(prevEndIdx, i) )
                    );
                }
                final ParsedJsImport parsed = parseJsImportStatement(tokens, i);
                if(parsed == null) {
                    blocked = true;
                    break;
                }
                final int endIdx = extendPastTrailingComment(tokens, parsed.semicolonIdx + 1);
                if( MiscRuleCore.anyFrozen(tokens, i, endIdx) ) {
                    blocked = true;
                    break;
                }
                final ParsedJsImport withSpan = new ParsedJsImport(
                    parsed.startIdx, parsed.semicolonIdx, endIdx, parsed.path
                );
                currentSegment.add(withSpan);
                prevEndIdx = endIdx;
                lastEndIdx = endIdx;
                i          = endIdx;
                continue;
            } // if
            ++i;
        } // while
        segments.add(currentSegment);

        final boolean anyImports = segments.stream().anyMatch( seg->!seg.isEmpty() );
        if(blocked || !anyImports) return render( tokens, java.util.Collections.emptyMap() );

        final StringBuilder body = new StringBuilder();
        for( int s = 0; s < segments.size(); ++s ) {
            if(s > 0) body.append( interSegmentGaps.get(s - 1) );
            body.append(
                renderImportSegment( tokens, segments.get(s), groupOrder, sortAlphabetically, blankLines )
            );
        }

        final StringBuilder out = new StringBuilder();
        out.append( collapseTokensToOneLine( tokens.subList(0, firstImportIdx) ) );
        out.append(body);
        out.append( collapseTokensToOneLine( tokens.subList(lastEndIdx, n) ) );

        return out.toString();
    }

    /**
     * Groups/sorts and renders one segment's imports (a maximal run of imports uninterrupted by
     *  a standalone comment, RDD_KEY_197) into its own bucketed, blank-line-separated text --
     *  the same shape {@code enforceImportOrdering} used to produce for the whole file before
     *  segmenting at standalone comments was introduced
     */
    private String renderImportSegment(
        final List<Token>          tokens,
        final List<ParsedJsImport> imports,
        final List<String>         groupOrder,
        final boolean              sortAlphabetically,
        final int                  blankLines
    )
    {
        final Map<String, List<ParsedJsImport>> buckets = new HashMap<>();
        for(final String key : IMPORT_GROUP_KEYS) buckets.put( key, new ArrayList<>() );
        for(final ParsedJsImport imp : imports) buckets.get(
            classifyJsImportGroup(imp.path)
        ).add(
            imp
        );
        if(sortAlphabetically) {
            for( final List<ParsedJsImport> group : buckets.values() ) group.sort(
                (a, b) -> a.path.compareTo(b.path)
            );
        }

        final StringBuilder body            = new StringBuilder();
              boolean       emittedAnyGroup = false;
        for(final String groupKey : groupOrder) {
            final List<ParsedJsImport> members = buckets.get(groupKey);
            if( members.isEmpty() ) continue;
            if(emittedAnyGroup) {
                for(int b = 0; b < blankLines + 1; ++b) body.append('\n');
            }
            for( int m = 0; m < members.size(); ++m ) {
                if(m > 0) body.append('\n');
                final ParsedJsImport imp = members.get(m);
                body.append( collapseTokensToOneLine( tokens.subList(imp.startIdx, imp.endIdx) ) );
            }
            emittedAnyGroup = true;
        } // for groupKey

        return body.toString();
    }

    /**
     * Parses one {@code import ...;} declaration starting at the {@code import} keyword token at
     *  {@code importIdx}. Permissive about the shape of the clause between {@code import} and the
     *  module-path {@code STRING} token (default import, namespace {@code * as x}, named-import
     *  list, {@code type} modifier, side-effect-only form with no {@code from} clause at all) --
     *  it only needs to locate the path string and the terminating {@code ;}, not fully validate
     *  the clause grammar, since the original token span is re-emitted verbatim rather than
     *  reconstructed. Returns {@code null} -- signaling "bail the entire pass" to the caller --
     *  if a comment is found anywhere inside the declaration, or if no module-path {@code STRING}
     *  token or no terminating {@code ;} is found before EOF.
     */
    private ParsedJsImport parseJsImportStatement(final List<Token> tokens, final int importIdx)
    {
        final int n            = tokens.size();
              int stringIdx    = -1;
              int semicolonIdx = -1;
              int p            = importIdx + 1;
        while(p < n) {
            final Token t = tokens.get(p);
            if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) return null;
            if(t.type == TokenType.STRING && stringIdx < 0) stringIdx = p;
            if( isPunct(t, ";") ) {
                semicolonIdx = p;
                break;
            }
            ++p;
        } // while
        if(stringIdx < 0 || semicolonIdx < 0) return null;

        return new ParsedJsImport(
            importIdx, semicolonIdx, semicolonIdx + 1,
            stringLiteralContent( tokens.get(stringIdx).text )
        );
    }

    /**
     * RDD_KEY_197: extends a just-parsed import's span past a trailing same-line comment right
     *  after its `;` (only whitespace, no NEWLINE, in between) -- that comment travels with its
     *  own import through reordering rather than being (mis)treated as a floating comment between
     *  imports that would otherwise segment the list. Returns {@code fromIdx} unchanged (no
     *  trailing comment found) otherwise.
     */
    private int extendPastTrailingComment(final List<Token> tokens, final int fromIdx)
    {
          int idx = fromIdx;
    final int n   = tokens.size();
        while( idx < n && tokens.get(idx).type == TokenType.WHITESPACE ) idx++;
        if( idx < n && ( tokens.get(
            idx
        ).type == TokenType.COMMENT_LINE || tokens.get(
            idx
        ).type == TokenType.COMMENT_BLOCK ) ) return idx + 1;

        return fromIdx;
    }

    /**
     * Strips the surrounding quote characters (`'`/`"`/`` ` ``) from a {@code STRING} token's raw
     *  text -- module-path specifiers never contain the quote character they're delimited by
     *  unescaped, so a plain substring is sufficient (no escape-processing needed)
     */
    private String stringLiteralContent(final String raw)
    {
        if( raw.length() >= 2 ) return raw.substring( 1, raw.length() - 1 );

        return raw;
    }

    /**
     * True iff a {@code COMMENT_LINE}/{@code COMMENT_BLOCK} token exists anywhere in {@code
     *  tokens[fromInclusive, toExclusive)} -- used to detect a floating comment between two
     *  otherwise-clean import declarations, which would otherwise be silently dropped by
     *  reordering
     */
    private boolean hasCommentBetween(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toExclusive
    )
    {
        for( int idx = Math.max(fromInclusive, 0); idx < toExclusive; ++idx ) {
            final TokenType type = tokens.get(idx).type;
            if(type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) return true;
        }

        return false;
    }

    /**
     * Classification priority (RDD_KEY_195): {@code node:}-prefixed or a known Node built-in
     *  name &gt; relative-path-prefixed ({@code local}) &gt; everything else ({@code
     *  third-party})
     */
    private String classifyJsImportGroup(final String path)
    {
        if( path.startsWith("node:") ) return "builtin";
        final int    slash          = path.indexOf('/');
        final String leadingSegment = slash < 0 ? path : path.substring(0, slash);
        if( NODE_BUILTIN_MODULES.contains(leadingSegment) ) return "builtin";
        if( path.startsWith("./") || path.startsWith("../") ) return "local";

        return "third-party";
    }

} // class JsTsSpecificRule
