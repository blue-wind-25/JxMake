/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.jxmake.formatter.rules.DeclarationAlignmentRuleCurly;
import com.jxmake.formatter.rules.DeclarationAlignmentRuleCurly.Declaration;
import com.jxmake.formatter.rules.GetterSetterRuleCore.Member;
import com.jxmake.formatter.rules.GetterSetterRuleCurly;
import com.jxmake.formatter.rules.JsTsDeclarationAlignmentRule;
import com.jxmake.formatter.rules.KotlinDeclarationAlignmentRule;
import com.jxmake.formatter.rules.KotlinDeclarationAlignmentRule.Row;
import com.jxmake.formatter.rules.KotlinSignatureRule;
import com.jxmake.formatter.rules.KotlinSignatureRule.FunctionTail;
import com.jxmake.formatter.rules.KotlinSignatureRule.KotlinSignature;
import com.jxmake.formatter.rules.MiscRuleCore.Assignment;
import com.jxmake.formatter.rules.MiscRuleCurly;
import com.jxmake.formatter.rules.MiscRuleCurly.Signature;
import com.jxmake.formatter.tokenizer.TokenizerCore;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;
import com.jxmake.formatter.tokenizer.TokenizerCurly;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

/**
 * Curly-brace-family (C/C++/Java/Kotlin) scope pipeline -- everything in this file used to live
 * directly in {@code ScopePipeline} before the curly/indent/tags class-refactor (see
 * `STATE_COMMON.md`'s "Class Refactor" checklist). No behavior change, mechanical move only;
 * every existing Kotlin-vs-C/C++/Java internal branch is unchanged.
 *
 * <p>Finds scope/signature boundaries in a whole-file token stream and splices grouped/rendered
 * output from {@link DeclarationAlignmentRule}, {@link GetterSetterRule}, and {@link MiscRuleCurly}
 * back into the source text on their behalf -- those rule classes' grouping methods explicitly
 * document that boundary-finding and splice-back are the caller's job (see STATE.md's
 * "ScopePipeline.java" section).
 *
 * <p>One {@link #process(String)} call runs four passes -- STYLE.md §5 declarations, §6
 * assignments, §8 signatures, §14 getter/setter -- on every scope, outer-first, recursing into
 * every brace-block found at each level after that scope's own four passes complete (so STYLE.md
 * §5/§6 apply anywhere in code, recursively, not just class/struct bodies -- see STATE.md's
 * "STYLE.md §5/§6 scope -- anywhere in code, recursively").
 */
public final class ScopePipelineCurly extends ScopePipelineCore {

    private final Lang                          lang;
    private final String                        indentStyle;
    private final TokenizerCurly                tokenizer;
    private final DeclarationAlignmentRuleCurly declarationRule;
    private final GetterSetterRuleCurly         getterSetterRule;
    private final MiscRuleCurly                 miscRule;
    // STYLE_KOTLIN.md §6/§7/§7.1/§7.2/§9/§12: reversed `name : type` grammar needs its own parser/
    // renderer (see KotlinDeclarationAlignmentRule/KotlinSignatureRule's own class comments) --
    // null for every other language, same "constructed only if needed" precedent as Formatter's
    // own kotlinRule.
    private final KotlinDeclarationAlignmentRule kotlinDeclarationRule;
    private final KotlinSignatureRule            kotlinSignatureRule;
    // STATE_JS_TS.md §11: JS/TS's `let x: Type = value` is likewise reversed relative to C/Java's
    // `Type name = value` -- mirrors the Kotlin field above, null for every other language.
    private final JsTsDeclarationAlignmentRule jsTsDeclarationRule;
    private final boolean                      formatOff;

    public ScopePipelineCurly(
        final Lang    lang,
        final String  indentStyle,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod
    )
    {
        this(lang, indentStyle, normalizeCommentStartCase, normalizeCommentEndPeriod, false);
    }

    public ScopePipelineCurly(
        final Lang    lang,
        final String  indentStyle,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean formatOff
    )
    {
        this(lang, indentStyle, normalizeCommentStartCase, normalizeCommentEndPeriod, formatOff,
                MiscRuleCurly.DEFAULT_INDENT_WIDTH, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public ScopePipelineCurly(
        final Lang    lang,
        final String  indentStyle,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean formatOff,
        final int     indentWidth,
        final int     lineLengthLimit
    )
    {
        this(lang, indentStyle, normalizeCommentStartCase, normalizeCommentEndPeriod, false,
                formatOff, indentWidth, lineLengthLimit);
    }

    public ScopePipelineCurly(
        final Lang    lang,
        final String  indentStyle,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean commentNormalizationClassifier,
        final boolean formatOff,
        final int     indentWidth,
        final int     lineLengthLimit
    )
    {
        this(lang, indentStyle, normalizeCommentStartCase, normalizeCommentEndPeriod,
                commentNormalizationClassifier, false, "", formatOff, indentWidth, lineLengthLimit);
    }

    /**
     * Full constructor additionally taking the {@code gru-classifier}/{@code gru-weights-path}
     *  config values (STATE_AI.md Step 3) -- see {@code MiscRuleCore}'s own full constructor.
     */
    public ScopePipelineCurly(
        final Lang    lang,
        final String  indentStyle,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean commentNormalizationClassifier,
        final boolean gruClassifier,
        final String  gruWeightsPath,
        final boolean formatOff,
        final int     indentWidth,
        final int     lineLengthLimit
    )
    {
        super(indentWidth);
        this.lang                  = lang;
        this.indentStyle           = indentStyle;
        this.tokenizer             = new TokenizerCurly(lang);
        this.declarationRule       = new DeclarationAlignmentRuleCurly(lang, lineLengthLimit);
        this.getterSetterRule      = lang.isKotlin
                ? new com.jxmake.formatter.rules.KotlinGetterSetterRule(
                    lang, indentWidth, lineLengthLimit
                )
                : new GetterSetterRuleCurly(lang, indentWidth, lineLengthLimit);
        this.miscRule              = new MiscRuleCurly(
            lang, normalizeCommentStartCase, normalizeCommentEndPeriod,
            commentNormalizationClassifier, gruClassifier, gruWeightsPath, indentWidth, lineLengthLimit
        );
        this.kotlinDeclarationRule = lang.isKotlin
                                   ? new KotlinDeclarationAlignmentRule(
                                       lang, indentWidth, lineLengthLimit
                                   ) : null;
        this.kotlinSignatureRule   = lang.isKotlin
                                   ? new KotlinSignatureRule(
                                       lang, indentWidth, lineLengthLimit
                                   ) : null;
        this.jsTsDeclarationRule   = (lang.isJs || lang.isTs)
                                   ? new JsTsDeclarationAlignmentRule(lang, lineLengthLimit) : null;
        this.formatOff             = formatOff;
    }

    /**
     * Wraps {@link TokenizerCore#tokenize} and stamps frozen-span state (RDD_KEY_90 §A) on every
     *  re-tokenize, same as {@code Formatter}'s tokenizer wrapper. {@code startFrozen} must be the
     *  frozen state observed at {@code s}'s own starting boundary -- a substring extracted mid-file
     *  (a recursed-into child scope) may not textually contain the JXM_CFMT_DIS marker that caused
     *  that state, so it cannot be re-derived by scanning {@code s} alone.
     */
    private List<Token> tokenize(final String s, final boolean startFrozen)
    {
        final List<Token> tokens = tokenizer.tokenize(s);
        TokenizerCore.markFrozenSpans(tokens, startFrozen);

        return tokens;
    }

    /**
     * Splits a scope's full token range into contiguous top-level spans -- a third port of the
     * depth-aware splitting algorithm already duplicated in
     * {@code DeclarationAlignmentRule.splitStatements} and {@code MiscRuleCurly.splitAssignmentStatements},
     * but additionally recording the matching open-brace index for any span that closes via a
     * top-level `}` rather than a `;`. This one helper serves three jobs (see STATE.md's
     * "Child-scope / signature-candidate discovery"): anchor-token-to-span lookup for §5/§6
     * splice-back, finding every child scope to recurse into, and finding §8 signature candidates.
     */
    private List<Span> splitTopLevelSpans(final List<Token> tokens)
    {
        final List<Span> spans    = new ArrayList<>();
        final int        n        = tokens.size();
              int        depth    = 0;
              int        start    = 0;
              int        idx      = 0;
              int        braceIdx = - 1;               // this span's own top-level `{`, if any

        while(idx < n) {
            final Token t = tokens.get(idx);

            if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) {
                if( isPunct(t, "{") && depth == 0 ) braceIdx = idx;
                ++depth;
                ++idx;
                continue;
            }
            if( isPunct(t, ")") || isPunct(t, "]") ) {
                --depth;
                ++idx;
                continue;
            }
            if( isPunct(t, "}") ) {
                --depth;
                final int closeBraceIdx = idx;
                ++idx;
                if(depth == 0) {
                    // Peek ahead: if the next significant token is `;`, this `}` is ambiguous --
                    // it could close a genuine named-construct body (`struct Foo { ... };`) or a
                    // brace-initializer nested in a declaration (`auto x = T{...};`, nested
                    // `{ {1,2}, {3,4} };`). Function/control-flow/lambda bodies and other genuine
                    // scopes are never followed by `;`, so in that (common) case this is
                    // unambiguously a real scope and no further check is needed. Only in the
                    // ambiguous `;`-followed case does `isScopeOpeningBrace` disambiguate via a
                    // backward scan for a construct keyword.
                    boolean nextIsSemi  = false;
                    boolean nextIsArrow = false;
                    for(int peek = idx; peek < n; ++peek) {
                        final Token nx = tokens.get(peek);
                        if( isGapToken(nx) ) continue;
                        nextIsSemi  = isPunct(nx, ";");
                        nextIsArrow = isOp(nx, "->");
                        break;
                    } // for
                    // A `}` immediately followed by `->` is never a genuine scope close -- it can
                    // only be a C++20 requires-expression compound-requirement
                    // (`{ expr } -> Concept;`), whose braces must stay part of the enclosing
                    // `requires { ... }` span, never split out as their own child scope (doing so
                    // previously mis-recursed into it, corrupting indentation non-idempotently --
                    // see real_code_regressions_34).
                    if( !nextIsArrow && ( !nextIsSemi || isScopeOpeningBrace(
                        tokens, braceIdx, start
                    ) ) ) {
                        idx = pullTrailingSameLine(tokens, idx);
                        spans.add( new Span(start, idx, braceIdx, closeBraceIdx) );
                        start    = idx;
                        braceIdx = -1;
                    }
                } // if
                continue;
            } // if
            if( depth == 0 && isPunct(t, ";") ) {
                ++idx;
                idx = pullTrailingSameLine(tokens, idx);
                spans.add( new Span(start, idx, -1, -1) );
                start    = idx;
                braceIdx = -1;
                continue;
            } // if
            if( depth == 0 && isOp(t, ":") && isAccessSpecifierLabel(tokens, start, idx) ) {
                ++idx;
                idx = pullTrailingSameLine(tokens, idx);
                spans.add( new Span(start, idx, -1, -1) );
                start    = idx;
                braceIdx = -1;
                continue;
            } // if
            ++idx;
        } // while
        if(start < n) spans.add( new Span(start, n, -1, -1) );

        return spans;
    }

    /**
     * True iff {@code text} starts a named construct -- ported from
     *  {@code BlockStructureRule.isNamedConstructStartKeyword}.
     */
    private boolean isNamedConstructStartKeyword(final String text)
    {
        switch(text) {
            case "class"     : /* FALL-THROUGH */ case "struct"    : /* FALL-THROUGH */ case "enum"      : /* FALL-THROUGH */ case "namespace" : /* FALL-THROUGH */
            case "concept"   : /* FALL-THROUGH */ case "interface" : /* FALL-THROUGH */ case "record"    : return true ;
            default          : return false;
        }
    }

    /**
     * True iff the `{` at {@code braceIdx} genuinely opens a named-construct body (class/struct/
     * union/enum/namespace/etc.) rather than a brace-initializer expression that merely happens to
     * be followed by `;` (e.g. `auto x = T{...};`, nested `{ {1,2}, {3,4} };`). Only called for
     * that ambiguous `;`-followed case (see caller) -- function/control-flow/lambda bodies are
     * never followed by `;` and so never reach here. Scans every significant token between
     * {@code spanStart} and {@code braceIdx} for a construct-introducing keyword (`class`,
     * `struct`, ...); found anywhere in that range (not just immediately before the brace) so an
     * intervening base-class list (`: public Base`), attribute-specifier (`alignas(16)`), or
     * template header doesn't defeat the match. A brace-initializer's span never contains such a
     * keyword (its lead tokens are a type/`auto` and `=`, or nothing at all), so this scan
     * distinguishes the two shapes cleanly -- except for a C-style elaborated-type declarator
     * (`struct sigaction sa = { };`), whose lead tokens genuinely do contain the keyword (the type
     * tag being referenced, not declared) followed by an `=` and a brace-initializer. A top-level
     * `=` anywhere in the scanned range rules out a construct declaration outright (neither a
     * class/struct/union/enum header nor its optional base-class list/attributes/template header
     * can contain one), so it unambiguously flags this as the elaborated-type-declarator shape
     * instead.
     */
    private boolean isScopeOpeningBrace(
        final List<Token> tokens,
        final int         braceIdx,
        final int         spanStart
    )
    {
        boolean sawConstructKeyword = false;
        int     depth               = 0;
        for(int i = spanStart; i < braceIdx; ++i) {
            final Token t = tokens.get(i);
                 if( isPunct(t, "(") || isPunct(t, "[") ) depth++;
            else if( isPunct(t, ")") || isPunct(t, "]") ) depth--;
            else if( depth == 0 && isOp(t, "=") ) return false;
            else if( t.type == TokenType.KEYWORD && isNamedConstructStartKeyword(
                t.text
            ) ) sawConstructKeyword = true;
        } // for

        return sawConstructKeyword;
    }

    /**
     * True iff {@code tokens[start, colonIdx)} contains exactly one significant token and it is
     *  `public`/`private`/`protected` -- a C++ access-specifier label, which (unlike a ternary's
     *  `:`) must end its own span here rather than being absorbed into the member that follows.
     *  Without this, "public:\n    Foo() {}" is swept into one span whose lead tokens become
     *  "public :", corrupting {@link #applySignaturePass}'s candidate-signature parse. Deliberately
     *  narrow: a bare ternary always has more than one significant lead token (e.g. `a ? b`), and a
     *  `case`/`default` switch label never matches this keyword set, so neither is affected.
     */
    private boolean isAccessSpecifierLabel(
        final List<Token> tokens,
        final int         start,
        final int         colonIdx
    )
    {
        int only = - 1;
        for(int i = start; i < colonIdx; ++i) {
            if( !isGapToken( tokens.get(i) ) ) {
                if(only >= 0) return false;
                only = i;
            }
        }
        if(only < 0) return false;
        final Token t = tokens.get(only);

        return t.type == TokenType.KEYWORD
                && ( "public".equals(
                    t.text
                ) || "private".equals(
                    t.text
                ) || "protected".equals(
                    t.text
                ) );
    }

    /**
     * Extends {@code from} forward over a same-line trailing comment, so it stays attached to the
     *  span that just closed instead of becoming the next span's leading content -- same algorithm
     *  as {@code DeclarationAlignmentRule.pullTrailingSameLine}, returning just the new index since
     *  this caller tracks spans by index range rather than building a token sublist.
     */
    private int pullTrailingSameLine(final List<Token> tokens, final int from)
    {
          int idx = from;
    final int n   = tokens.size();
        while(idx < n) {
            final TokenType ty = tokens.get(idx).type;
            if(ty == TokenType.WHITESPACE || ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) idx++;
            else break;
        }

        return idx;
    }

    /**
     * True if the `{` at {@code braceIdx} opens a `namespace NAME { ... }` (or nested
     *  `namespace a::b { ... }`) body -- found by walking back over the optional
     *  `IDENTIFIER (:: IDENTIFIER)*` name chain to check for an immediately preceding
     *  `namespace` keyword. {@code Token.name} alone can't distinguish this from any other
     *  named construct (it holds just the bare name, e.g. `"audio"`, not the construct
     *  keyword).
     */
    private boolean isNamespaceScope(final List<Token> tokens, final int braceIdx)
    {
        if( tokens.get(braceIdx).name == null ) return false;
        int p = prevSignificantIndex(tokens, braceIdx);
        if( p < 0 || tokens.get(p).type != TokenType.IDENTIFIER ) return false;
        while(true) {
            final int q = prevSignificantIndex(tokens, p);
            if( q < 0 || !isOp( tokens.get(q), "::" ) ) break;
            final int r = prevSignificantIndex(tokens, q);
            if( r < 0 || tokens.get(r).type != TokenType.IDENTIFIER ) break;
            p = r;
        } // while
        final int kwIdx = prevSignificantIndex(tokens, p);

        return kwIdx >= 0 && tokens.get(kwIdx).type == TokenType.KEYWORD
                && "namespace".equals( tokens.get(kwIdx).text );
    }

    /**
     * Returns the leading whitespace of the line that contains the span's first significant
     *  token -- i.e. the indentation of the named construct whose one-liner body we are about
     *  to pre-expand.  Scans forward to the first non-gap token, then backward to the preceding
     *  newline; the text between that newline and the token is the indentation.
     */
    /**
     * True iff {@code colonIdx} is the terminating `:` of a `case EXPR :` / `default :` switch
     *  label, found by scanning backward over the label's constant-expression tokens (bare
     *  identifiers/numbers/`::`-qualified names) until the `case`/`default` keyword itself is
     *  reached, or a `;`/`}`/`{`/`:` is hit first (in which case this is some other kind of
     *  colon -- inheritance list, ternary, constructor initializer list -- and not a label)
     */
    private boolean isCaseOrDefaultLabelColon(final List<Token> tokens, final int colonIdx)
    {
        for(int i = colonIdx - 1; i >= 0; --i) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) continue;
            if( t.type == TokenType.KEYWORD && ( "case".equals(
                t.text
            ) || "default".equals(
                t.text
            ) ) ) return true;
            if( isPunct(
                t, ";"
            ) || isPunct(
                t, "}"
            ) || isPunct(
                t, "{"
            ) || isOp(
                t, ":"
            ) ) return false;
        } // for

        return false;
    }

    private String findParentIndent(
        final List<Token> tokens,
        final Span        span,
        final String      inheritedIndent
    )
    {
        // Find where the construct actually governing openBraceIdx begins -- NOT simply the
        // first significant token in the whole span, since a span can carry more than one
        // leading statement/label ahead of the one that opens this brace (e.g. `case 1:` is not
        // its own span -- see splitTopLevelSpans -- so a span for the `if` immediately following
        // a `case` label has that label's tokens sitting before `if` within the SAME span; using
        // the span's own first token as anchor would incorrectly land on `case` and later return
        // that label's line indent instead of the `if`'s own). Scan backward from openBraceIdx
        // instead, skipping over balanced parens/brackets, stopping at the nearest top-level
        // `;`, `}`, or `case`/`default` label colon -- whichever comes first identifies the true
        // start of the statement that owns this brace.
        int stmtStart  = span.start;
        int parenDepth = 0;
        for(int i = span.openBraceIdx - 1; i >= span.start; --i) {
            final Token tok = tokens.get(i);
            if( isPunct(tok, ")") || isPunct(tok, "]") ) {
                ++parenDepth;
                continue;
            }
            if( isPunct(tok, "(") || isPunct(tok, "[") ) {
                --parenDepth;
                continue;
            }
            if(parenDepth > 0) continue;
            if( isPunct(tok, ";") ) {
                stmtStart = i + 1;
                break;
            }
            if( isPunct(tok, "}") ) {
                stmtStart = i + 1;
                break;
            }
            if( isOp(tok, ":") && isCaseOrDefaultLabelColon(tokens, i) ) {
                stmtStart = i + 1;
                break;
            }
            // Kotlin-only: a property's `get`/`set` accessor body (`set(value) { ... }`) is a
            // separate statement from the property declaration line right above it
            // (`var count: Int = 0`), but that declaration line is conventionally
            // newline-terminated with no `;` (STYLE_KOTLIN.md §1) -- invisible to the `;`/`}`
            // check above, written for C/Java's semicolon-terminated grammar. Without this, the
            // backward scan for `set(value) {`'s owning statement walks straight past the
            // newline separating it from the property declaration and keeps going (no `;` to
            // stop it), landing `stmtStart`/the anchor on the property declaration's own first
            // token (`var`) instead of `set` -- `findParentIndent` then returns the property's
            // shallower indent instead of the accessor's own deeper one, under-indenting the
            // accessor body's closing `}` by one level. Scoped narrowly to a depth-0 NEWLINE
            // immediately followed (ignoring gap tokens) by the `get`/`set` soft keyword, rather
            // than any Kotlin newline-terminated statement boundary in general, since a broader
            // rule would also misfire on legitimate multi-line fluent call chains (`foo()\n
            // .bar()\n.baz { }`) where a leading `.`/`?.` on the continuation line means the
            // newline is NOT a statement boundary -- not attempted here, out of scope for this
            // fix, no evidence of that shape in any fixture.
            if(lang.isKotlin && tok.type == TokenType.NEWLINE) {
                final int next = nextSignificantIndex(tokens, i);
                if( next >= 0 && next < span.openBraceIdx && tokens.get(
                    next
                ).type == TokenType.KEYWORD
                        && ( "get".equals(
                            tokens.get(next).text
                        ) || "set".equals(
                            tokens.get(next).text
                        ) ) ) {
                    stmtStart = next;
                    break;
                }
            } // if
        } // for
        // Anchor on the first significant token at or after stmtStart: normally the construct's
        // own keyword/name (`class Foo {`), but for a bare compound statement (`{ ... }` with no
        // preceding keyword) the `{` itself is the first significant token, so openBraceIdx
        // doubles as the anchor in that case.
        int anchor = span.openBraceIdx;
        for(int i = stmtStart; i < span.openBraceIdx; ++i) {
            // A preprocessor directive belonging to this span's own leading gap (e.g. a
            // preceding sibling span ends right where a `#endif` starts) always sits at column
            // 0 and is never part of the actual construct -- skip it like a gap token so the
            // real first token (e.g. `public`) becomes the anchor instead.
            if( !isGapToken( tokens.get(i) ) && tokens.get(i).type != TokenType.PREPROCESSOR ) {
                anchor = i;
                break;
            }
        } // for
        // Kotlin-only: `splitTopLevelSpans` splits the token stream on top-level BRACE spans, so
        // a braceless `if (...) { ... } else someExpr` statement (the `if`-branch has its own
        // brace span, the braceless `else someExpr` branch does not) gets cut in two -- the
        // `if`-branch is processed as its own span, and `else someExpr` becomes leading, unbraced
        // text at the very START of the NEXT real (braced) span, rather than a genuine new
        // statement. `stmtStart`/`anchor` above default to that next span's own start, landing
        // squarely on this dangling `else` -- which is not a real statement boundary at all, just
        // where the previous span's extraction happened to stop. Left uncorrected, a LATER
        // declaration's own brace search (backward scan bounded by ITS span.start, which sits
        // after this dangling `else someExpr`) can walk straight past several real statements
        // with no `;`/`}` of their own (Kotlin's brace-less, newline-terminated expression-bodied
        // declarations) and wrongly land back on this same dangling `else` as its anchor --
        // returning that unrelated, EARLIER statement's line indent instead of its own. Found via
        // `kotlinx.coroutines`'s `BufferedChannel.kt`
        // (`onUndeliveredElementReceiveCancellationConstructor`, a bare, unnamed nested-lambda-
        // chain closing-brace drift distinct from RDD_KEY_136/152/158/159/160). Fix: when the
        // anchor found above is itself a dangling `else`/`catch`/`finally`, it can never be a
        // real statement's own start -- skip forward past its one-line, brace-less body to the
        // next depth-0 NEWLINE, then re-anchor on whatever real statement follows.
        if( lang.isKotlin && anchor < span.openBraceIdx && tokens.get(
            anchor
        ).type == TokenType.KEYWORD
                && ( "else".equals(
                    tokens.get(anchor).text
                ) || "catch".equals(
                    tokens.get(anchor).text
                )
                        || "finally".equals( tokens.get(anchor).text ) ) ) {
            int k = anchor + 1;
            while( k < span.openBraceIdx && tokens.get(k).type != TokenType.NEWLINE ) k++;
            for(int i = k; i < span.openBraceIdx; ++i) {
                if( !isGapToken( tokens.get(i) ) && tokens.get(i).type != TokenType.PREPROCESSOR ) {
                    anchor = i;
                    break;
                }
            }
        } // if
        // Search backward across the whole token list, not just from span.start: a preceding
        // sibling span (e.g. one ending in a preprocessor directive) can leave this span's own
        // start coinciding with its first significant token, with the actual leading
        // newline+indent having been consumed as part of the PREVIOUS span instead -- bounding
        // the search at span.start would then find nothing and silently fall back to "".
        for(int j = anchor - 1; j >= 0; --j) {
            final Token tok = tokens.get(j);
            if(tok.type == TokenType.NEWLINE) return joinText(tokens, j + 1, anchor);
            // The anchor is not the first significant token on its own physical line (e.g. a
            // still-K&R `} else {`, before Formatter's later placeElseOnOwnLine pass converts it
            // to Allman). Scanning further back would generally cross into a PRECEDING
            // statement/block's own text -- EXCEPT when that immediately preceding token is
            // itself a `}`: by C-family grammar, an `else`/`catch`/`finally` can only ever
            // directly follow the closing brace of the construct it attaches to (`if { ... }
            // else`, `try { ... } catch`), so that `}` sits at exactly the same nesting depth
            // this keyword belongs at once placed on its own line. Deriving the indent from that
            // brace's own physical line (rather than giving up) keeps this span's closing-brace
            // force-reindent (processScope's trailing-gap logic below) consistent between a
            // fresh K&R-input format and a reformat of that same output once
            // placeElseOnOwnLine has already moved the keyword onto its own Allman line --
            // previously these two passes disagreed (returning null pre-Allman, a real indent
            // post-Allman), corrupting only the closing `}`'s placement non-idempotently while
            // the body's own statements stayed put (found via `openrewrite/rewrite`'s
            // `rewrite-python/.../Autodetect.java`, the `visitCollectionLiteral` `else if`
            // closing-brace drift).
            if( isPunct(
                tok, "}"
            ) && tokens.get(
                anchor
            ).type == TokenType.KEYWORD && ( "else".equals(
                tokens.get(anchor).text
            ) || "catch".equals(
                tokens.get(anchor).text
            ) || "finally".equals(
                tokens.get(anchor).text
            ) ) ) return braceLineIndent(
                tokens, j
            );
            if( !isGapToken(tok) ) return null;
        } // for
        // Ran off the start of `tokens` without ever finding a NEWLINE. `tokens` here is a
        // recursively-extracted child fragment (see `processScope`'s `joinText(current,
        // span.openBraceIdx + 1, span.closeBraceIdx)`) whose very first line is a nested
        // construct that shares no captured newline with the fragment's own start -- either it
        // shares its opening line with the parent's own `{` (e.g. `struct Foo { enum Bar {` on
        // one source line), or it is simply the first statement in the fragment and an earlier
        // pass stripped the leading newline/indent before this point was reached. Either way,
        // this construct occupies the same textual "slot" a normal statement directly inside
        // the current frame would -- i.e. exactly `inheritedIndent`, the real indent `processScope`
        // already resolved (recursively, text-first) for the frame currently being processed.
        // Trusting a synthesized "" or a depth-derived guess here previously corrupted nesting
        // whenever the frame's real indent didn't match a naive per-level assumption -- see
        // STATE.md's `frozen`/`CaseSensitive`/`enum Choice` bug (depth-based guess) and the
        // `catch.hpp` `analyse()` if/else bug (namespaces don't consume a `depth` level but their
        // body content is still genuinely indented one level in the real source).
        return inheritedIndent;
    }

    /**
     * Kotlin-only: true if this span's own header (from its true statement start, not just
     *  {@code span.start} -- a span can carry more than one leading statement, same caveat
     *  {@code findParentIndent} itself documents) contains a depth-0 {@code where} keyword --
     *  i.e. a generic {@code where} clause, which {@code KotlinSpecificRule.
     *  enforceWhereClausePlacement} can wrap onto its own indented continuation line(s). Used by
     *  {@code processScope} to force {@code spanIndent} over a volatile {@code braceIndent} for a
     *  `fun` with a wrapped `where` clause, the same posture RDD_KEY_159 already gives
     *  `isNamedScope` -- `NAMED_CONSTRUCT_KOTLIN` never includes `fun`, so that fix alone doesn't
     *  cover this shape (RDD_KEY_214's own deferred 4-file residual). Depth tracked over
     *  parens/brackets/reclassified generic angle brackets so a bound's own generic argument
     *  (`Comparable<T>`) or the param list's own parens never falsely end the scan early.
     */
    private boolean headerHasTopLevelWhereClause(final List<Token> tokens, final Span span)
    {
        int depth     = 0;
        int stmtStart = span.start;
        for(int i = span.openBraceIdx - 1; i >= span.start; --i) {
            final Token tok = tokens.get(i);
            if( isPunct(
                tok, ")"
            ) || isPunct(
                tok, "]"
            ) || tok.type == TokenType.ANGLE_BRACKET_CLOSE ) {
                ++depth;
                continue;
            }
            if( isPunct(
                tok, "("
            ) || isPunct(
                tok, "["
            ) || tok.type == TokenType.ANGLE_BRACKET_OPEN ) {
                --depth;
                continue;
            }
            if(depth > 0) continue;
            if( isPunct(tok, ";") || isPunct(tok, "}") ) {
                stmtStart = i + 1;
                break;
            }
        } // for
        depth = 0;
        for(int i = stmtStart; i < span.openBraceIdx; ++i) {
            final Token t = tokens.get(i);
            if( isPunct(
                t, "("
            ) || isPunct(
                t, "["
            ) || t.type == TokenType.ANGLE_BRACKET_OPEN ) depth++;
            else if( isPunct(
                t, ")"
            ) || isPunct(
                t, "]"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE ) depth--;
            else if( depth == 0 && t.type == TokenType.KEYWORD && "where".equals(
                t.text
            ) ) return true;
        } // for

        return false;
    }

    /**
     * Returns the leading whitespace of the physical line containing {@code idx} (typically an
     *  {@code openBraceIdx}), or {@code null} if no NEWLINE precedes it within {@code tokens}.
     *  Unlike {@link #findParentIndent}, which anchors on the STATEMENT's own start (skipping
     *  back over a multi-line fluent chain to, e.g., `AlertDialog.Builder(this)`), this looks at
     *  only the immediate physical line the brace itself sits on -- e.g. the `.setPositiveButton(
     *  "Ok") {` continuation line, not the chain's first line. A trailing-lambda body's own
     *  indent, and its closing `}`'s alignment, follow the brace's own line, not the whole
     *  statement's first line -- see the Kotlin `MainActivity._checkRecovery()` closing-brace
     *  indentation drift bug (RDD_KEY_136).
     */
    private String braceLineIndent(final List<Token> tokens, final int idx)
    {
        int i = idx - 1;
        while( i >= 0 && tokens.get(i).type != TokenType.NEWLINE ) i--;
        if(i < 0) return null;
        // Kotlin-only anticipation of KotlinSpecificRule.formatWhenExpressions' later
        // arrow-alignment merge (RDD_KEY_1xx): a nested `when { ... }` used as a `when` branch's
        // body currently sits with its `when {` alone on its own physical line (this method's
        // normal read gives that line's indent), but a later phase in the same pipeline merges
        // the branch's label onto that same line (`is Foo -> when {`) whenever the merged line
        // still fits -- which it always does here, since the body's own first line is just `{`.
        // Left unaccounted for, this scope's own indent (and its closing `}`'s placement) is
        // computed against the PRE-merge physical line on a fresh format, but the POST-merge
        // physical line on a reformat of already-merged output -- a real idempotency bug found via
        // `square/kotlinpoet` real-code testing (`TypeName.kt`'s `get(type: Type, ...)`). Detect
        // the shape up front and anchor on the eventual (post-merge) line instead, so both passes
        // agree.
        final int mergedLineStart = findMergingWhenBranchLineStart(tokens, i, idx);
        if(mergedLineStart >= 0) i = mergedLineStart;
        final StringBuilder sb = new StringBuilder();
        for(int j = i + 1; j < idx; ++j) {
            final Token t = tokens.get(j);
            if(t.type != TokenType.WHITESPACE) break;
            sb.append(t.text);
        }

        return sb.toString();
    }

    /**
     * If {@code openBrace}'s own physical line (whose leading NEWLINE is {@code ownLineNewline})
     *  consists of nothing but a bare `when {`/`when(subject) {` (a nested `when` expression used
     *  as a `when` branch's body) OR a plain trailing-lambda call head (`identifier(...)? {`, e.g.
     *  `buildCodeBlock {`), and the immediately preceding physical line ends with a top-level
     *  `->`, returns the index of the NEWLINE that starts THAT preceding line (the branch label's
     *  own line) -- the line the two will be merged onto by
     *  {@code KotlinSpecificRule.formatWhenExpressions}' arrow-alignment pass, later in the same
     *  pipeline. Returns -1 if the shape doesn't match. The plain-call-head case covers a `when`
     *  branch whose body is a call with only a trailing lambda, no other body content
     *  (`kmAnnotations.kt`'s `is ArrayKClassValue -> \n  buildCodeBlock { ... }`), same merge
     *  mechanism as the `when`-only case originally handled.
     */
    private int findMergingWhenBranchLineStart(
        final List<Token> tokens,
        final int         ownLineNewline,
        final int         openBrace
    )
    {
        if(!lang.isKotlin || ownLineNewline < 0) return -1;
        int j = ownLineNewline + 1;
        while( j < tokens.size() && tokens.get(j).type == TokenType.WHITESPACE ) j++;
        if(j >= openBrace) return -1;
        final Token   head       = tokens.get(j);
        final boolean isWhenHead = head.type == TokenType.KEYWORD&& "when".equals(head.text);
        final boolean isCallHead = head.type == TokenType.IDENTIFIER;
        if(!isWhenHead && !isCallHead) return -1;
        int k = j + 1;
        while( k < openBrace && tokens.get(k).type == TokenType.WHITESPACE ) k++;
        // A subject-form `when(subject) { ... }` has a parenthesized subject expression between
        // the keyword and the brace -- skip a balanced `(...)` span before checking for the
        // brace, same shape as bare `when { ... }` otherwise (RDD_KEY_152 originally only handled
        // the subject-less form; `ReflectiveClassInspector.kt`'s `when(kotlin) { ... }` nested
        // inside a `when` branch surfaced this gap).
        if( k < openBrace && isPunct( tokens.get(k), "(" ) ) {
            int depth = 0;
            while(k < openBrace) {
                if( isPunct( tokens.get(k), "(" ) ) {
                    ++depth;
                }
                else if( isPunct( tokens.get(k), ")" ) ) {
                    --depth;
                    if(depth == 0) {
                        ++k;
                        break;
                    }
                }
                ++k;
            } // while
            while( k < openBrace && tokens.get(k).type == TokenType.WHITESPACE ) k++;
        } // if
        if(k != openBrace) return -1;
        // `when` is the first significant token on its own line, immediately followed (modulo
        // whitespace, or an optional parenthesized subject expression) by `openBrace` itself --
        // now check the preceding physical line ends with a top-level `->`
        int p = ownLineNewline - 1;
        while( p >= 0 && tokens.get(p).type == TokenType.WHITESPACE ) p--;
        if( p < 0 || !isOp( tokens.get(p), "->" ) ) return -1;
        int q = p - 1;
        while( q >= 0 && tokens.get(q).type != TokenType.NEWLINE ) q--;

        return q;
    }

    // ── §5 declarations pass ─────────────────────────────────────────────────────

    /**
     * Runs {@code DeclarationAlignmentRule.groupDeclarations} on {@code tokens} and splices each
     * group's rendered grid back in as one contiguous block. {@code Declaration} carries no
     * original-position fields, so each group's first/last member is anchored back to a token
     * index via identity lookup (see STATE.md's "Splice-back mechanics"), and that index's
     * enclosing {@link Span} supplies the actual replacement range -- the span already includes
     * the statement's trailing `;`/comment via the identical depth-aware algorithm.
     */
    private String applyDeclarationsPass(final List<Token> tokens, final int depth)
    {
        if(lang.isKotlin) return applyKotlinDeclarationsPass(tokens, depth);
        if(lang.isJs || lang.isTs) return applyJsTsDeclarationsPass(tokens);
        final List<List<Declaration>> groups       = declarationRule.groupDeclarations(tokens);
        final List<Span>              spans        = splitTopLevelSpans(tokens);
        final Map<Token, Integer>     indexOf      = buildIndexMap(tokens);
        final List<Replacement>       replacements = new ArrayList<>();

        for(final List<Declaration> group : groups) {
            final Declaration first = group.get(0);
            final Declaration last  = group.get( group.size() - 1 );
            final Token firstAnchor = !first.templatePrefix.isEmpty() ? first.templatePrefix.get(0)
                    : first.modifiers.isEmpty() ? first.typeTokens.get(0) : first.modifiers.get(0);
            final int  firstIdx  = indexOf.get(firstAnchor);
            final int  lastIdx   = indexOf.get(last.name);
            final Span firstSpan = findSpanContaining(spans, firstIdx);
            final Span lastSpan  = findSpanContaining(spans, lastIdx);
            // Use firstIdx (the declaration's own first real token), not firstSpan.start: the
            // span's leading gap can contain a previous statement's trailing JXM_CFMT_ENA/DIS
            // marker (always itself stamped frozen -- see markFrozenSpans), which must not cause
            // this unrelated, unfrozen declaration to be mistaken for frozen content.
            if( anyFrozen(tokens, firstIdx, lastSpan.end) ) continue;

            final List<String> lines = declarationRule.render(group);
            // `lines.get(0)` may already start with its own column-alignment padding (e.g. blank
            // space matching a sibling's `static` width, STYLE.md §5) -- on a re-format of
            // already-formatted source, that same padding is literally present in the raw text
            // right before `firstIdx` too, indistinguishable by character alone from real code
            // indentation. Stripping exactly that many trailing spaces off the raw gap before
            // treating the remainder as "indent" keeps this pass idempotent; on a first-time
            // format (no pre-existing padding in the source) there's nothing to strip and this
            // is a no-op.
            final String rawLeadingGapFull = joinText(tokens, firstSpan.start, firstIdx);
            final int    freshPad          = leadingSpaceCount( lines.get(0) );
            // On a genuine re-format, the raw gap's trailing-space count is trueIndent + freshPad
            // (the previous pass wrote both), so it is always >= freshPad. On a first-time format
            // the gap has only trueIndent spaces with no self-padding; if that count is already
            // less than freshPad (e.g. a struct member's real 4-space indent vs. a 6-wide
            // "const "-column pad from a sibling), stripping would eat into real indentation, so
            // skip the strip entirely in that case.
            final int trailingSpaces = leadingSpaceCount(
                new StringBuilder(rawLeadingGapFull).reverse().toString()
            );
            // A genuine re-format's raw gap is trueIndent + freshPad, where trueIndent is
            // itself always a valid multiple of indentWidth (the previous pass's own written
            // indent) -- so stripping freshPad off a real re-format always lands back on a
            // multiple-of-indentWidth boundary with no rounding needed. A first-time format
            // whose true indent merely happens to be numerically >= freshPad (e.g. an 8-space
            // method body vs. a 6-wide "final "-column pad, `java_combined_inp.java`'s
            // `evaluateAt2`) strips into a non-multiple remainder that `normalizeIndent` then
            // has to round back up, silently eating a real indent level. Guard by only
            // accepting the strip when its result is already indentWidth-aligned.
            final String strippedCandidate = stripTrailingSpaces(rawLeadingGapFull, freshPad);
            final int    strippedWidth     = trailingIndent(strippedCandidate).length();
            final String rawLeadingGap     = trailingSpaces >= freshPad
                    && strippedWidth % indentWidth == 0
                    ? strippedCandidate : rawLeadingGapFull;
            final String rawIndent   = trailingIndent(rawLeadingGap);
            final String indent      = normalizeIndent(rawIndent);
            final String leadingGap  = normalizeLeadingGap(rawLeadingGap, rawIndent, indent);
            final String text        = leadingGap + String.join("\n" + indent, lines);
                  int    lastTermEnd = lastSpan.end;
            while( lastTermEnd > lastSpan.start && isWhitespaceOrNewline(
                tokens.get(lastTermEnd - 1)
            ) ) lastTermEnd--;
            replacements.add( new Replacement(firstSpan.start, lastTermEnd, text) );
        } // for

        return splice(tokens, replacements);
    }

    /**
     * STYLE_KOTLIN.md §6/§12 -- Kotlin's `[modifiers] val|var name [: type] [= init]` grammar is
     *  reversed relative to C/Java's `[modifiers] Type name [= init]`, so {@code
     *  KotlinDeclarationAlignmentRule} parses/renders it with its own {@code Row} model rather
     *  than the base {@code Declaration} model above. Kotlin properties/locals are conventionally
     *  newline-terminated with no `;`, so {@code splitTopLevelSpans} (built around `;`/`}`-
     *  terminated C/Java statements) cannot be reused to find each declaration's own span --
     *  instead this anchors directly on each row's own leading token ({@code firstAnchor}, never
     *  empty) and trailing token ({@code lastAnchor}), both always present in {@code tokens} by
     *  identity, via {@code buildIndexMap}.
     *  <p>A plain declaration (§6) and a destructuring declaration (§12) are grouped together in
     *  one merged pass here -- RDD_KEY_107's original "own group stream, never merged" design was
     *  revisited and reversed on explicit user request, matching this codebase's existing C++
     *  structured-bindings precedent (`auto [b, c] = ...` already aligns in the same group as a
     *  plain `int a = ...`); see RDD_LOG.md's RDD_KEY_107 entry for the full revision note.
     */
    private String applyKotlinDeclarationsPass(final List<Token> tokens, final int depth)
    {
        final Map<Token, Integer> indexOf      = buildIndexMap(tokens);
        final List<Replacement>   replacements = new ArrayList<>();
        // RDD_KEY_162: depth * indentWidth is the same "nestDepth * indentWidth" column estimate
        // GetterSetterRule's own width pre-check (RDD_KEY_139) uses -- this group's true final
        // indent isn't known until addKotlinDeclReplacement below derives it from the raw leading
        // gap text, but that's close enough to budget against (Kotlin declarations are never
        // re-indented relative to their own scope)
        final int estimatedIndent = depth * indentWidth;

        for( final List<Row> group : kotlinDeclarationRule.groupAlignableDeclarations(tokens) ) {
            final Row first = group.get(0);
            final Row last  = group.get( group.size() - 1 );
            addKotlinDeclReplacement(
                tokens, indexOf, replacements, first.firstAnchor, last.lastAnchor,
                kotlinDeclarationRule.renderAlignedGroup(group, estimatedIndent)
            );
        } // for

        return splice(tokens, replacements);
    }

    /**
     * STYLE_JS_TS.md §11 -- mirrors {@link #applyKotlinDeclarationsPass} (JS/TS's `let x: Type =
     *  value` is likewise name-before-type, reversed relative to C/Java), but JS/TS statements are
     *  always `;`-terminated (STYLE_JS_TS.md §2), unlike Kotlin's newline-terminated properties --
     *  {@link JsTsDeclarationAlignmentRule} still exposes {@code lastAnchor} the same way, so the
     *  same identity-based {@link #addKotlinDeclReplacement} splice-back helper is reused as-is.
     */
    private String applyJsTsDeclarationsPass(final List<Token> tokens)
    {
        final Map<Token, Integer> indexOf      = buildIndexMap(tokens);
        final List<Replacement>   replacements = new ArrayList<>();

        for( final List<JsTsDeclarationAlignmentRule.Row> group
                : jsTsDeclarationRule.groupAlignableDeclarations(tokens) ) {
            final JsTsDeclarationAlignmentRule.Row first = group.get(0);
            final JsTsDeclarationAlignmentRule.Row last = group.get( group.size() - 1 );
            addKotlinDeclReplacement(
                tokens, indexOf, replacements, first.keyword, last.lastAnchor,
                jsTsDeclarationRule.renderAlignedGroup(group)
            );
        } // for

        return splice(tokens, replacements);
    }

    /**
     * Shared splice-range/leading-gap computation for one Kotlin declaration group -- same
     *  idempotent leading-padding-strip logic as {@link #applyDeclarationsPass}, but the group's
     *  own leading-gap boundary is derived from the previous significant token (there is no
     *  {@code Span} to anchor on for newline-terminated Kotlin statements) and the trailing
     *  boundary is exactly one past {@code lastAnchor} (already the group's true last token --
     *  no trailing whitespace to trim, unlike a `;`/`}`-terminated C/Java span)
     */
    private void addKotlinDeclReplacement(
        final List<Token>         tokens,
        final Map<Token, Integer> indexOf,
        final List<Replacement>   replacements,
        final Token               firstAnchor,
        final Token               lastAnchor,
        final List<String>        lines
    )
    {
        final int firstIdx = indexOf.get(firstAnchor);
        final int lastIdx  = indexOf.get(lastAnchor);
        if( anyFrozen(tokens, firstIdx, lastIdx + 1) ) return;
        final int    gapStart          = prevSignificantIndex(tokens, firstIdx) + 1;
        final String rawLeadingGapFull = joinText(tokens, gapStart, firstIdx);
        final int    freshPad          = leadingSpaceCount( lines.get(0) );
        final int    trailingSpaces    = leadingSpaceCount(
            new StringBuilder(rawLeadingGapFull).reverse().toString()
        );
        final String rawLeadingGap = trailingSpaces >= freshPad
                ? stripTrailingSpaces(rawLeadingGapFull, freshPad) : rawLeadingGapFull;
        final String rawIndent  = trailingIndent(rawLeadingGap);
        final String indent     = normalizeIndent(rawIndent);
        final String leadingGap = normalizeLeadingGap(rawLeadingGap, rawIndent, indent);
        final String text       = leadingGap + String.join("\n" + indent, lines);
        replacements.add( new Replacement(gapStart, lastIdx + 1, text) );
    }

    // ── Oversized aggregate-init closing-brace pass ────────────────────────────────

    /**
     * A brace-initializer (`name = { ... };`) too large or otherwise ineligible for
     * {@code DeclarationAlignmentRule}'s one-line collapse (e.g. a byte/word table spanning many
     * source lines, left untouched by the width guard in `parseDeclaration`) still has its
     * closing `}` dangling at the end of the last data line, e.g. {@code ... 0xAC, 0x62};}. Move
     * that `}` onto its own line at the declaration's own indent -- matching this codebase's
     * general convention of a closing brace on its own line (STYLE.md's Allman-style bodies,
     * `}; // struct Foo` for named constructs) -- without touching any of the untouched data
     * lines above it. Only fires when the brace-initializer's own `{...}` already spans a
     * newline (a short flat init that {@code DeclarationAlignmentRule} successfully collapsed to
     * one line by this point has no newline left inside it, so this pass is a no-op for it) and
     * the `}` is not already alone on its own line.
     */
    private String applyOversizedAggregateInitClosingBracePass(final List<Token> tokens)
    {
        final List<Replacement> replacements = new ArrayList<>();
        final int               n            = tokens.size();
        for(int idx = 0; idx < n; ++idx) {
            if( !isOp( tokens.get(idx), "=" ) ) continue;
            final int openIdx = nextSignificantIndex(tokens, idx);
            if( openIdx < 0 || !isPunct( tokens.get(openIdx), "{" ) ) continue;
            int     depth            = 1;
            int     k                = openIdx + 1;
            boolean hasNewlineInside = false;
            while(k < n && depth > 0) {
                final Token tk = tokens.get(k);
                if( isPunct(tk, "{") ) {
                    ++depth;
                }
                else if( isPunct(tk, "}") ) {
                    --depth;
                    if(depth == 0) break;
                }
                else if(tk.type == TokenType.NEWLINE) {
                    hasNewlineInside = true;
                }
                ++k;
            } // while
            if(depth != 0 || !hasNewlineInside) continue;
            final int closeIdx = k;
            final int semiIdx  = nextSignificantIndex(tokens, closeIdx);
            if( semiIdx < 0 || !isPunct( tokens.get(semiIdx), ";" ) ) continue;
            int     wsStart  = closeIdx;
            boolean sameLine = false;
            while(wsStart > openIdx + 1) {
                final Token pt = tokens.get(wsStart - 1);
                if(pt.type == TokenType.NEWLINE) break;
                if(pt.type == TokenType.WHITESPACE) {
                    --wsStart;
                    continue;
                }
                sameLine = true;
                break;
            } // while
            if(!sameLine) continue; // `}` is already alone on its own line -- nothing to do
            String indent = "";
            for(int p = openIdx - 1; p >= 0; --p) {
                if( tokens.get(p).type == TokenType.NEWLINE ) {
                    final StringBuilder sb = new StringBuilder();
                          int           q  = p + 1;
                    while( q < openIdx && tokens.get(q).type == TokenType.WHITESPACE ) {
                        sb.append( tokens.get(q).text );
                        ++q;
                    }
                    indent = sb.toString();
                    break;
                } // if
            } // for p
            replacements.add( new Replacement(wsStart, closeIdx, "\n" + indent) );
        } // for idx

        return splice(tokens, replacements);
    }

    // ── §6 assignments pass ──────────────────────────────────────────────────────

    /**
     * Identical shape to {@link #applyDeclarationsPass} using {@code MiscRuleCurly.groupAssignments}/
     *  {@code render} -- an {@code Assignment}'s {@code target} is always the statement's own
     *  first significant token (per {@code MiscRuleCurly.parseAssignment}), so it doubles as both the
     *  start- and end-anchor.
     */
    private String applyAssignmentsPass(final List<Token> tokens)
    {
        final List<List<Assignment>> groups       = miscRule.groupAssignments(tokens);
        final List<Span>             spans        = splitTopLevelSpans(tokens);
        final Map<Token, Integer>    indexOf      = buildIndexMap(tokens);
        final List<Replacement>      replacements = new ArrayList<>();

        for(final List<Assignment> group : groups) {
            final Assignment first     = group.get(0);
            final Assignment last      = group.get( group.size() - 1 );
            final int        firstIdx  = indexOf.get(first.target);
            final int        lastIdx   = indexOf.get(last.target);
            final Span       firstSpan = findSpanContaining(spans, firstIdx);
            final Span       lastSpan  = findSpanContaining(spans, lastIdx);
            // See applyDeclarationsPass: use firstIdx, not firstSpan.start, so a marker in the
            // leading gap doesn't falsely mark this unfrozen assignment group as frozen.
            if( anyFrozen(tokens, firstIdx, lastSpan.end) ) continue;

            final String       rawLeadingGap = joinText(tokens, firstSpan.start, firstIdx);
            final String       rawIndent     = trailingIndent(rawLeadingGap);
            final String       indent        = normalizeIndent(rawIndent);
            final String       leadingGap    = normalizeLeadingGap(
                rawLeadingGap, rawIndent, indent
            );
            final List<String> lines         = miscRule.render(group);
            final String       text          = leadingGap + String.join("\n" + indent, lines);
                  int          lastTermEnd   = lastSpan.end;
            while( lastTermEnd > lastSpan.start && isWhitespaceOrNewline(
                tokens.get(lastTermEnd - 1)
            ) ) lastTermEnd--;
            replacements.add( new Replacement(firstSpan.start, lastTermEnd, text) );
        } // for

        return splice(tokens, replacements);
    }

    // ── §8 signatures pass ───────────────────────────────────────────────────────

    /**
     * Scans {@code tokens} for signature candidates -- a top-level `{`-terminated span whose
     * brace is directly preceded (skipping gaps) by a `)` whose matching `(` is itself preceded by
     * an IDENTIFIER not preceded by `new` (ported from {@code JavaSpecificRule.isCandidateMethodName}/
     * {@code CppSpecificRule.isCandidateSignatureName}), excluding a Java enum constant body
     * ({@code isEnumConstantBody}) -- and splices {@code MiscRuleCurly.render(Signature, depth,
     * indentStyle)}'s output over just the `[leadStart, closeParenIdx]` range, leaving the body
     * untouched.
     */
    private String applySignaturePass(final List<Token> tokens, final int depth)
    {
        final List<Span>        spans        = splitTopLevelSpans(tokens);
        final List<Replacement> replacements = new ArrayList<>();

        for(final Span span : spans) {
            if(span.openBraceIdx < 0) continue;
            int closeParenIdx = prevSignificantIndex(tokens, span.openBraceIdx);
            if(closeParenIdx < 0) continue;
            // A C++ constructor's member-initializer list (`: field(val), other(val2)`) sits
            // between the parameter list's own `)` and the body `{`, so the "nearest paren before
            // `{`" found above is actually the last initializer's own closing paren, not the
            // signature's -- redirect back to the `)` immediately before the list's introducing
            // top-level `:` when one is found, so the signature-parsing below sees the real
            // parameter list instead of misparsing the last initializer as a bogus signature
            int memberInitColonIdx = - 1;
            if(lang.isCpp) {
                final int colonIdx = findTopLevelMemberInitColon(
                    tokens, span.start, span.openBraceIdx
                );
                if(colonIdx >= 0) {
                    final int realClose = prevSignificantIndex(tokens, colonIdx);
                    if( realClose >= 0 && isPunct( tokens.get(realClose), ")" ) ) {
                        closeParenIdx      = realClose;
                        memberInitColonIdx = colonIdx;
                    }
                } // if
            } // if
            // For Java: handle a `throws` clause between `)` and `{`
            int throwsEndIdx = - 1;
            // For Kotlin: handle an explicit `: ReturnType` between `)` and `{` (STYLE_KOTLIN.md
            // §9) -- absent entirely for a `Unit`-inferred function/constructor, where closeParenIdx
            // above is already the real `)` and this is a no-op.
            int kotlinTailEndIdx = - 1;
            if( !isPunct( tokens.get(closeParenIdx), ")" ) ) {
                if(lang.isKotlin) {
                    final int realCloseParen = findLastTopLevelCloseParen(
                        tokens, span.start, span.openBraceIdx
                    );
                    if(realCloseParen < 0) continue;
                    final int afterParen = nextSignificantIndex(tokens, realCloseParen);
                    if( afterParen < 0 || !isOp( tokens.get(afterParen), ":" ) ) {
                        // No `:` immediately after the found `)` -- not a `: ReturnType` tail,
                        // just an unrelated top-level `)` (e.g. a preceding call in a fluent
                        // chain like `x.foo().bar { ... }`). Bail rather than misread it as a
                        // signature.
                        continue;
                    } // if
                    if( hasTopLevelBlankLine(tokens, afterParen, span.openBraceIdx) ) {
                        // A genuine `: ReturnType` tail is a single, unbroken type expression
                        // ending at its own `{`/`=` -- it never contains a blank line. Found via
                        // real-code testing against `square/okio`: a headerless multiplatform
                        // declaration with no body of its own (`expect fun getEnv(name: String):
                        // String?`) has its `)`/`:` wrongly matched as this iteration's
                        // `realCloseParen`/tail when `span.openBraceIdx` actually belongs to a
                        // later, unrelated `val ... by lazy { ... }` declaration -- silently
                        // merging the two into one bogus signature+tail and eating the blank line
                        // and the `val`'s own leading text. Bail rather than merge across a real
                        // statement boundary.
                        continue;
                    } // if
                    if( !hasTopLevelOperator(tokens, afterParen, span.openBraceIdx, "=")
                            && hasTopLevelNewline(tokens, afterParen, span.openBraceIdx) ) {
                        // C6i (JetBrains/kotlin dogfood): even with NO blank line, a genuine
                        // `: ReturnType {` tail (no `=` -- a block-bodied function/constructor, not
                        // an expression-bodied one) is always on one unbroken physical line -- it
                        // never contains a NEWLINE of its own before `{`. Without this check, a
                        // headerless one-liner interface member with no body (`fun clear(): Unit`)
                        // immediately followed -- on the very next line, no blank line between -- by
                        // an unrelated named construct's own opening brace (e.g. a nested
                        // `interface Bar {`) had its `)`/`:` wrongly matched as this iteration's
                        // `realCloseParen`/tail: the whole span from `clear()`'s `)` through the
                        // interface's own name got swallowed into `parseFunctionTail`'s tail range
                        // and flattened onto one line with no separators, silently fusing two
                        // unrelated statements together
                        // (`libraries/stdlib/js-ir-minimal-for-test/.../collectionsWithoutExportLogic.kt`'s
                        // `MutableMap`). Bail rather than merge across a real statement boundary --
                        // same posture as the blank-line check just above, just catching the
                        // no-blank-line variant of the same "unrelated statement boundary" shape.
                        // The `hasTopLevelOperator(..., "=")` guard keeps the pre-existing legitimate
                        // case working: an expression-bodied function's `: ReturnType =` tail
                        // legitimately continues onto a later physical line before reaching its own
                        // trailing-lambda body's `{` (`fixture _33`, `TypeSpecHolder.kt`'s
                        // `addTypes`) -- that shape always has a depth-0 `=` in this range, this
                        // bug's shape never does.
                        continue;
                    } // if
                    kotlinTailEndIdx = closeParenIdx;
                    closeParenIdx    = realCloseParen;
                } // if
                else if(lang.isJava) {
                    final int realCloseParen = findCloseParenBeforeThrows(tokens, closeParenIdx);
                    if(realCloseParen < 0) continue;
                    throwsEndIdx  = closeParenIdx;
                    closeParenIdx = realCloseParen;
                }
                else {
                    continue;
                }
            } // if
            final int openParenIdx = matchParenBackward(tokens, closeParenIdx);
            if( openParenIdx < 0 || !isCandidateSignatureName(tokens, openParenIdx) ) continue;
            if( lang.isJava && isEnumConstantBody(tokens, span.openBraceIdx) ) continue;

            int leadStart = nextSignificantIndex(tokens, span.start - 1);
            if(leadStart < 0) continue;
            // A preprocessor directive (e.g. a lone `#endif` closing a field-level `#ifdef`
            // guard) sitting on its own line before this method is not part of the signature --
            // isGapToken() deliberately treats PREPROCESSOR/MACRO_DEF as significant (so
            // statement-splitting elsewhere doesn't swallow it), but that means
            // nextSignificantIndex() stops on it here, making it look like the signature's own
            // first token. Left uncorrected, the whole directive line gets pulled out of
            // leadingGap and re-glued directly onto the rendered signature's first line with no
            // newline. Skip past any number of leading directive lines (each still its own
            // physical line, i.e. followed by a NEWLINE) to find the real lead token instead.
            while( leadStart >= 0 && ( tokens.get(
                leadStart
            ).type == TokenType.PREPROCESSOR || tokens.get(
                leadStart
            ).type == TokenType.MACRO_DEF ) ) leadStart = nextSignificantIndex(
                tokens, leadStart
            );
            if(leadStart < 0) continue;
            if( anyFrozen(
                tokens,
                leadStart,
                (throwsEndIdx >= 0 ? throwsEndIdx : kotlinTailEndIdx >= 0 ? kotlinTailEndIdx : closeParenIdx) + 1
            ) ) continue;
            // For Java: skip past any leading @Annotation tokens so they stay verbatim in
            // leadingGap (on their own line) rather than being absorbed into the signature's
            // lead-token list and collapsed onto the method declaration line.
            // For C/C++: start the signature from the same line as the function name --
            // a `template<...>` header that precedes the function on its own line must
            // stay verbatim in leadingGap rather than being pulled into parseSignature,
            // where it would be collapsed with the return type onto one line.
            final int sigLeadStart;
            if(lang.isJava) {
                sigLeadStart = skipAnnotations(tokens, leadStart, closeParenIdx);
            }
            else {
                final int nameIdx = prevSignificantIndex(tokens, openParenIdx);
                if(nameIdx >= 0) {
                    // Find the last NEWLINE within the span (before nameIdx)
                    int newlineIdx = - 1;
                    for(int j = nameIdx - 1; j >= leadStart; --j) {
                        if( tokens.get(j).type == TokenType.NEWLINE ) { newlineIdx = j; break; }
                    }
                    if(newlineIdx < 0) {
                        // No NEWLINE between leadStart and nameIdx -- same physical line
                        sigLeadStart = leadStart;
                    }
                    else {
                        // Function name is on a later line; start the signature there
                        final int lineFirst = nextSignificantIndex(tokens, newlineIdx);
                        sigLeadStart = lineFirst >= 0
                                ? extendOverLeadingRequiresAndTemplate(tokens, lineFirst, leadStart)
                                : leadStart;
                    }
                } // if
                else {
                    sigLeadStart = leadStart;
                }
            }
            if(lang.isKotlin) {
                final KotlinSignature kotlinSig = kotlinSignatureRule.parseKotlinSignature(
                    tokens.subList(sigLeadStart, closeParenIdx + 1)
                );
                if(kotlinSig == null) continue;
                final int tailEnd = kotlinTailEndIdx >= 0 ? kotlinTailEndIdx : prevSignificantIndex(
                    tokens, span.openBraceIdx
                );
                final FunctionTail                          tail = kotlinSignatureRule.parseFunctionTail(
                    tokens.subList(closeParenIdx + 1, tailEnd + 1)
                );
                if(tail == null) continue;
                final List<String>  kotlinLines      = kotlinSignatureRule.renderWithTail(
                    kotlinSig, tail, depth, indentStyle
                );
                final String        kotlinLeadingGap = joinText(tokens, span.start, sigLeadStart);
                final StringBuilder kotlinText       = new StringBuilder(
                    kotlinLeadingGap
                ).append(
                    kotlinLines.get(0)
                );
                for( int i = 1; i < kotlinLines.size(); ++i ) kotlinText.append(
                    '\n'
                ).append(
                    kotlinLines.get(i)
                );
                replacements.add(
                    new Replacement( span.start, tailEnd + 1, kotlinText.toString() )
                );
                continue;
            } // if
            final Signature sig = miscRule.parseSignature(
                tokens.subList(sigLeadStart, closeParenIdx + 1)
            );
            if(sig == null) continue;

            // A member-initializer list can start right after the signature's `)` on the very
            // same output line (`) : tag(` above) -- that trailing text counts against the line
            // length just as much as the signature itself, so the wrap decision must include it.
            // Otherwise a signature at the boundary can measure as "fits" here while the actual
            // rendered line (with `: field(` appended) runs over, only to get wrapped on a later
            // pass once the input already has it pre-wrapped -- unstable across repeated formats.
            // Restricted to this member-init-list case specifically (rather than any trailing
            // same-line text): an ordinary function/method body's `{` hasn't been Allman-placed
            // onto its own line yet at this point in the pipeline (that's a later phase), so a
            // same-line `{` here is not yet the real, final same-line neighbor and including it
            // would itself be a source of round-to-round instability.
            int trailingLen = 0;
            if(memberInitColonIdx >= 0) {
                int trailingLineEnd = closeParenIdx + 1;
                while( trailingLineEnd < tokens.size() && tokens.get(
                    trailingLineEnd
                ).type != TokenType.NEWLINE ) trailingLineEnd++;
                trailingLen = joinText(tokens, closeParenIdx + 1, trailingLineEnd).length();
            } // if
            final List<String>  lines      = miscRule.render(sig, depth, indentStyle, trailingLen);
            final String        leadingGap = joinText(tokens, span.start, sigLeadStart);
            final StringBuilder text       = new StringBuilder(leadingGap).append( lines.get(0) );
            for( int i = 1; i < lines.size(); ++i ) text.append('\n').append( lines.get(i) );
            if(throwsEndIdx >= 0) {
                // Append normalized throws clause: scan significant tokens from `throws` keyword
                // through the last exception class name, joining with single spaces
                int ti = nextSignificantIndex(tokens, closeParenIdx);
                while(ti >= 0 && ti <= throwsEndIdx) {
                    text.append(' ').append( tokens.get(ti).text );
                    ti = nextSignificantIndex(tokens, ti);
                }
                replacements.add(
                    new Replacement( span.start, throwsEndIdx + 1, text.toString() )
                );
            } // if
            else {
                replacements.add(
                    new Replacement( span.start, closeParenIdx + 1, text.toString() )
                );
            }
        } // for span

        return splice(tokens, replacements);
    }

    /**
     * Scans forward past zero or more {@code @Identifier} / {@code @Identifier(args)}
     * annotation tokens starting at {@code from}, returning the index of the first
     * non-annotation significant token. Returns {@code from} if nothing is skipped or
     * if skipping would reach or exceed {@code limit}.
     */
    private int skipAnnotations(final List<Token> tokens, final int from, final int limit)
    {
        int i = from;
        while( i >= 0 && i < limit && isOp( tokens.get(i), "@" ) ) {
            final int nameIdx = nextSignificantIndex(tokens, i);
            if( nameIdx < 0 || nameIdx >= limit || tokens.get(
                nameIdx
            ).type != TokenType.IDENTIFIER ) break;
            final int afterName = nextSignificantIndex(tokens, nameIdx);
            if( afterName >= 0 && afterName < limit && isPunct( tokens.get(afterName), "(" ) ) {
                final int closeParen = matchParenForward(tokens, afterName);
                if(closeParen < 0 || closeParen >= limit) break;
                i = nextSignificantIndex(tokens, closeParen);
            }
            else {
                i = afterName;
            }
            if(i < 0 || i >= limit) break;
        } // while

        return (i < 0 || i >= limit) ? from : i;
    }

    /**
     * True iff the token immediately before {@code openIdx} is an IDENTIFIER not itself preceded
     *  by `new` -- the candidate-signature-name signal, ported from
     *  {@code JavaSpecificRule.isCandidateMethodName}/{@code CppSpecificRule.isCandidateSignatureName}.
     */
    /**
     * Finds a top-level (depth-0) `:` between {@code from} and {@code to} that introduces a
     *  constructor's member-initializer list -- recognized by being immediately preceded by `)`
     *  (the parameter list's own close paren) and not itself part of `::`. Returns -1 if none
     *  found (an ordinary function/control-flow body, or a C++11 base-class-in-braces case with
     *  no initializer list at all).
     */
    /**
     * Finds the last depth-0 `)` in {@code [from, to)} -- for Kotlin, the parameter list's own
     *  closing paren, when it isn't the token immediately before {@code to} (an explicit `:
     *  ReturnType` tail sits between them, STYLE_KOTLIN.md §9). Angle brackets (`<T>` generics,
     *  `List<Int>` in a return type) don't affect `(`/`)` depth so need no special handling here.
     */
    private int findLastTopLevelCloseParen(final List<Token> tokens, final int from, final int to)
    {
        int depth = 0;
        int found = - 1;
        for(int i = from; i < to; ++i) {
            final Token t = tokens.get(i);
            if( isPunct(t, "(") ) {
                ++depth;
            }
            else if( isPunct(t, ")") ) {
                --depth;
                if(depth == 0) found = i;
            }
        } // for

        return found;
    }

    /**
     * True iff a depth-0 (outside any `(`/`[`/`{`, relative to {@code from}) {@code OP} token
     *  with text {@code opText} appears anywhere in {@code tokens[from, to)} -- used by {@code
     *  applySignaturePass}'s Kotlin `: ReturnType` tail detection (C6i fix) to tell a genuine
     *  expression-bodied function's `: ReturnType =` tail (legitimately allowed to continue onto a
     *  later physical line before its own trailing-lambda body's `{`) apart from a block-bodied
     *  `: ReturnType {` tail (which must never contain a NEWLINE of its own before `{`)
     */
    private boolean hasTopLevelOperator(
        final List<Token> tokens,
        final int         from,
        final int         to,
        final String      opText
    )
    {
        int depth = 0;
        for(int i = from; i < to; ++i) {
            final Token t = tokens.get(i);
                 if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) depth++;
            else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) depth--;
            else if( depth == 0 && isOp(t, opText) ) return true;
        }

        return false;
    }

    /**
     * True iff two consecutive {@code TokenType.NEWLINE} tokens (a blank source line) appear
     *  anywhere in {@code tokens[from, to)} -- used by {@code applySignaturePass}'s Kotlin
     *  `: ReturnType` tail detection to refuse treating a candidate tail as reaching all the way
     *  to a later, unrelated declaration's `{` across a genuine statement/paragraph break.
     */
    private boolean hasTopLevelBlankLine(final List<Token> tokens, final int from, final int to)
    {
        boolean sawNewline = false;
        for(int i = from; i < to; ++i) {
            final TokenType type = tokens.get(i).type;
            if(type == TokenType.NEWLINE) {
                if(sawNewline) return true;
                sawNewline = true;
            }
            else if(type != TokenType.WHITESPACE) {
                sawNewline = false;
            }
        } // for

        return false;
    }

    private int findTopLevelMemberInitColon(final List<Token> tokens, final int from, final int to)
    {
        int depth = 0;
        for(int i = from; i < to; ++i) {
            final Token t = tokens.get(i);
            if( isPunct(t, "(") || isPunct(t, "[") ) {
                ++depth;
            }
            else if( isPunct(t, ")") || isPunct(t, "]") ) {
                --depth;
            }
            else if( depth == 0 && isOp(t, ":") ) {
                final int prev = prevSignificantIndex(tokens, i);
                if( prev >= 0 && isPunct( tokens.get(prev), ")" ) ) return i;
            }
        } // for

        return -1;
    }

    private boolean isCandidateSignatureName(final List<Token> tokens, final int openIdx)
    {
        final int nameIdx = prevSignificantIndex(tokens, openIdx);
        if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) return false;
        final int beforeName = prevSignificantIndex(tokens, nameIdx);

        return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                || !"new".equals( tokens.get(beforeName).text );
    }

    /**
     * True iff the `{` at {@code braceIdx} is a Java enum constant's anonymous constant-body --
     *  detected via its matching `}` being immediately followed by `,` or `;` -- ported from
     *  {@code JavaSpecificRule.isEnumConstantBody}.
     */
    private boolean isEnumConstantBody(final List<Token> tokens, final int braceIdx)
    {
        final int closeBraceIdx = matchBraceForward(tokens, braceIdx);
        if(closeBraceIdx < 0) return false;
        final int next = nextSignificantIndex(tokens, closeBraceIdx);

        return next >= 0 && ( isPunct(
            tokens.get(next), ","
        ) || isPunct(
            tokens.get(next), ";"
        ) );
    }

    /**
     * For Java `throws` clauses: given the token at {@code fromIdx} (the significant token
     * immediately before `{`) that is NOT `)`, checks if it is the last exception class name
     * in a {@code throws} clause. Scans backward through comma-separated IDENTIFIERs to the
     * {@code throws} keyword, then expects `)` immediately before it.
     * Returns the index of the `)` of the method parameter list, or -1 if no such pattern found.
     */
    private int findCloseParenBeforeThrows(final List<Token> tokens, final int fromIdx)
    {
        int i = fromIdx;
        if( i < 0 || tokens.get(i).type != TokenType.IDENTIFIER ) return -1;
        while(i >= 0) {
            final Token t = tokens.get(i);
                 if(t.type == TokenType.IDENTIFIER) i = prevSignificantIndex(tokens, i - 1);
            else if( isPunct(t, ",") )              i = prevSignificantIndex(tokens, i - 1);
            else                                    break;
        }
        if( i < 0 || tokens.get(
            i
        ).type != TokenType.KEYWORD || !"throws".equals(
            tokens.get(i).text
        ) ) return -1;
        final int closeParen = prevSignificantIndex(tokens, i - 1);

        return ( closeParen >= 0 && isPunct( tokens.get(closeParen), ")" ) ) ? closeParen : -1;
    }

    // ── §14 getter/setter pass ───────────────────────────────────────────────────

    /**
     * Runs {@code GetterSetterRule.groupOneLiners} then {@code excludeOutliers} on {@code tokens},
     * splicing each remaining member's own rendered row over its own {@code memberFrom}/
     * {@code memberTo} range individually -- not as one contiguous block, since an excluded
     * outlier in the middle of a run leaves the surviving members textually non-adjacent (see
     * STATE.md's checklist note on this). A group that drops below 2 members after exclusion is
     * skipped entirely, per {@code excludeOutliers}'s own contract.
     *
     * <p>RDD_KEY_219 (Kotlin only, see {@code renderKotlinFilteredRuns} below): for Kotlin, a
     * member excluded here by {@code excludeOutliers}'s width-ratio test is exactly the shape
     * that {@code KotlinGetterSetterRule.parseKotlinOneLinerMember} will fail to re-parse as a
     * one-liner once a LATER phase ({@code KotlinSignatureRule}'s own param-list wrap) turns its
     * padded line genuinely multi-line -- at which point {@code groupOneLiners} itself hard-breaks
     * the group there on a re-format. Rendering the surviving members either side of that excluded
     * member as one flat shared-width group (as this method still does for C/C++/Java below) is
     * therefore not idempotent for Kotlin: round1 pads both sides to one shared width, round2 (once
     * the excluded member is genuinely multi-line) sees two independently-narrower groups. C/C++/
     * Java members never turn genuinely multi-line this way after this pass runs (no equivalent
     * later phase re-wraps a one-liner accessor's own signature), so their existing flat-group
     * behavior here is left untouched -- this fix is scoped to Kotlin only, see
     * {@code renderKotlinFilteredRuns}.
     */
    private String applyGetterSetterPass(final List<Token> tokens, final int depth)
    {
        final List<List<Member>>  groups       = getterSetterRule.groupOneLiners(tokens, depth);
        final Map<Token, Integer> indexOf      = buildIndexMap(tokens);
        final List<Replacement>   replacements = new ArrayList<>();

        for(final List<Member> group : groups) {
            final List<Member> filtered = getterSetterRule.excludeOutliers(tokens, group);
            if( filtered.isEmpty() ) continue;
            final List<String> lines = lang.isKotlin
                    ? renderKotlinFilteredRuns(tokens, group, filtered, depth)
                    : getterSetterRule.render(tokens, filtered);
            for( int i = 0; i < filtered.size(); ++i ) {
                final Member                                                         m = filtered.get(
                    i
                );
                final int startIdx = m.templatePrefixFrom != m.templatePrefixTo ? m. templatePrefixFrom : m.returnTypeFrom;
                final int sigIdx = m.modifiers.isEmpty() ?                           startIdx           : indexOf.get(
                    m.modifiers.get(0)
                );
                // Use sigIdx (the member's own first real token), not m.memberFrom: memberFrom's
                // leading gap can contain a previous statement's trailing marker comment (always
                // itself stamped frozen), which must not falsely freeze this unrelated member.
                if( anyFrozen(tokens, sigIdx, m.memberTo) ) continue;
                final String leadingGap = joinText(tokens, m.memberFrom, sigIdx);
                replacements.add(
                    new Replacement( m.memberFrom, m.memberTo, leadingGap + lines.get(i) )
                );
            } // for i
        } // for group

        return splice(tokens, replacements);
    }

    /**
     * RDD_KEY_219 -- Kotlin-only companion to {@link #applyGetterSetterPass}: renders {@code
     * filtered} (the post-{@code excludeOutliers} survivors) as maximal CONTIGUOUS runs with
     * respect to their positions in the original (pre-exclusion) {@code group}, instead of one
     * flat shared-width group spanning the whole original group. Each run is rendered
     * independently via {@code getterSetterRule.render}, matching the width a Kotlin re-format
     * would naturally compute once an excluded member (sitting between two runs) has become
     * genuinely multi-line and hard-breaks {@code groupOneLiners}'s own grouping at that point --
     * see {@link #applyGetterSetterPass}'s doc comment for the full idempotency rationale.
     *
     * <p>RDD_KEY_220: {@code depth} is threaded through to {@code getterSetterRule}'s own
     * width-budget-aware {@code render(tokens, run, depth)} overload (a no-op for C/C++/Java,
     * real budget-exclusion logic for {@link KotlinGetterSetterRule}), so a run's own members can
     * additionally be excluded from THEIR shared column grid if group padding alone would push a
     * member's rendered width past the line-length limit -- see that overload's own doc comment.
     */
    private List<String> renderKotlinFilteredRuns(
        final List<Token>  tokens,
        final List<Member> group,
        final List<Member> filtered,
        final int          depth
    )
    {
        final java.util.Set<Member> keep = java.util.Collections.newSetFromMap(
            new java.util.IdentityHashMap<>()
        );
        keep.addAll(filtered);
        final List<String> result = new ArrayList<>();
              int          i      = 0;
        while( i < group.size() ) {
            if( !keep.contains( group.get(i) ) ) {
                ++i;
                continue;
            }
            final List<Member> run = new ArrayList<>();
                  int          j   = i;
            while( j < group.size() && keep.contains( group.get(j) ) ) {
                run.add( group.get(j) );
                ++j;
            }
            result.addAll( getterSetterRule.render(tokens, run, depth) );
            i = j;
        } // while

        return result;
    }

    // ── Recursion driver ─────────────────────────────────────────────────────────

    /**
     * Runs the four passes above, in fixed order (re-tokenizing between each, same
     * "chained via re-tokenizing between passes" precedent used throughout this codebase), then
     * recurses outer-first into every child block found in the final token list, splicing each
     * child's processed text back in place
     */
    private String processScope(
        final List<Token> tokens,
        final int         depth,
        final boolean     scopeStartFrozen,
        final String      inheritedIndent
    )
    {
        List<Token> current = tokens;
        current = tokenize( applyDeclarationsPass(current, depth), scopeStartFrozen );
        current = tokenize(
            applyOversizedAggregateInitClosingBracePass(current), scopeStartFrozen
        );
        current = tokenize( applyAssignmentsPass(current), scopeStartFrozen );
        current = tokenize( applySignaturePass(current, depth), scopeStartFrozen );
        current = tokenize( applyGetterSetterPass(current, depth), scopeStartFrozen );

        final List<Span>        spans                   = splitTopLevelSpans(current);
        final List<Replacement> replacements            = new ArrayList<>();
              int               prevCloseBraceIdx       = - 1;
              String            prevEffectiveSpanIndent = null;
        for(final Span span : spans) {
            if(span.openBraceIdx < 0) continue;
            // A child scope extracted as raw text below may not textually contain the
            // JXM_CFMT_DIS marker that caused its own `{` to already be frozen on entry (the
            // marker can live outside this span entirely) -- re-tokenizing it must seed that
            // same frozen state explicitly rather than defaulting to unfrozen (RDD_KEY_90 §A)
            final boolean childStartFrozen = current.get(span.openBraceIdx).frozen;
                  String  childSource      = joinText(
                      current, span.openBraceIdx + 1, span.closeBraceIdx
                  );
            final boolean isNamedScope     = current.get(span.openBraceIdx).name != null;
            // This span's own real indent (text-derived where possible, else propagated from
            // the frame currently being processed -- see `findParentIndent`'s fallback). Used
            // for (1) pre-expanding a named one-liner body below, (2) the indent inherited by
            // whatever is recursed into below, and (3) this span's own closing-brace placement.
            final String spanIndent = findParentIndent(current, span, inheritedIndent);
            // Kotlin-only: a trailing lambda argument's `{` can sit on a continuation line of a
            // multi-line fluent chain (e.g. `AlertDialog.Builder(this)\n    .setTitle(...)\n
            // .setPositiveButton("Ok") {`), deeper than the chain's own first line. `spanIndent`
            // above (via `findParentIndent`) anchors on that FIRST line (`AlertDialog.Builder`),
            // since it deliberately walks back to the whole statement's start (needed for, e.g.,
            // `case 1:` labels). But the lambda body's own indent, and its closing `}`'s
            // alignment, follow the brace's own physical line (`.setPositiveButton(...)`), not
            // the chain's first line -- using `spanIndent` here under-indented the body by
            // whatever the chain's own continuation indent added, and misplaced the closing `}`
            // to match (RDD_KEY_136, `MainActivity._checkRecovery()`'s closing-brace drift).
            // Gated to Kotlin: C/C++/Java's `;`-terminated, brace-anchored control-flow bodies
            // don't commonly place a block-opening `{` at the end of a multi-line fluent chain
            // the way a Kotlin trailing lambda does, and this fix is unverified for those
            // languages' own multi-line-chain shapes.
            final String braceIndent = lang.isKotlin ? braceLineIndent(
                current, span.openBraceIdx
            ) : null;
            final String effectiveSpanIndent;
            // Kotlin-only: a `catch`/`finally` span directly chained onto the immediately
            // preceding span's own closing `}` (`} catch (...) {` / `} finally {`, i.e. one
            // logical try/catch/finally statement split into multiple top-level spans by this
            // method) must share that PRECEDING span's own resolved indent, not derive one of
            // its own. `spanIndent`/`braceIndent` above are both read from this span's own
            // physical text -- for a `try { ... }` whose own multi-line signature got merged
            // onto a single line earlier in this same pass (KotlinSignatureRule's
            // parseFunctionTail, e.g. `internal actual fun f(x: String): String? = try {`), the
            // `try` block's own indent is correctly re-derived as the (now shallower) merged
            // line's indent, but the following `catch`'s `{` still sits on its ORIGINAL,
            // pre-merge physical line -- one level deeper than it now truly is -- so
            // `braceIndent` reads a stale, too-deep value with nothing to correct it (found via
            // `kotlinx.coroutines`'s `SystemProps.kt`, the try/catch-as-expression idempotency
            // bug documented in STATE_KOTLIN.md's Open Questions).
            final int     chainKwIdx            = nextSignificantIndex(current, span.start - 1);
            final boolean isChainedCatchFinally = lang.isKotlin&& prevEffectiveSpanIndent != null&& prevCloseBraceIdx >= 0&& nextSignificantIndex(
                current, prevCloseBraceIdx
            ) == chainKwIdx&& chainKwIdx >= 0&& chainKwIdx < current.size()&& current.get(
                chainKwIdx
            ).type == TokenType.KEYWORD&& ( "catch".equals(
                current.get(chainKwIdx).text
            ) || "finally".equals(
                current.get(chainKwIdx).text
            ) );
            // Kotlin-only, same "inherit the preceding span's already-resolved indent" posture as
            // `isChainedCatchFinally` above, generalized to an ordinary fluent-chain continuation
            // (`}.apply {`, `}?.let {`, etc, not just `catch`/`finally`). `braceIndent`/`spanIndent`
            // are both read from THIS span's own physical text, but when this span's `{` opens a
            // trailing-lambda call chained directly (`.`/`?.`, no gap) onto the IMMEDIATELY
            // preceding span's own closing `}`, the preceding span's closing `}` is itself
            // volatile -- which physical column it lands on can differ round-to-round for reasons
            // unrelated to this span at all (e.g. RDD_KEY_159's named-scope fix, or simply this
            // span's own indent affecting a later pass's rewrap of the closing text) -- so reading
            // `braceIndent`/`spanIndent` off of it directly is not a stable fixed point. The
            // preceding span's own ALREADY-RESOLVED `effectiveSpanIndent` (`prevEffectiveSpanIndent`)
            // is a stable value computed the same way every pass regardless of how its own closing
            // text physically renders, so anchoring on it here transitively stabilizes the whole
            // chain (found via `JetBrains/kotlin`'s `declarationBuilders.kt`, `addFunction {
            // ... }.apply { ... }` -- round1 read `}.apply {`'s own physical-line indent (volatile,
            // 4sp), round2 re-read it as 0sp once the PREVIOUS span's own close text had reflowed).
            // Also covers a trailing-lambda call chained onto the preceding span's `}` via a
            // boolean infix operator (`||`/`&&`) rather than a direct `.`/`?.` member-access --
            // e.g. `declarations.any { ... } || declarations.any { ... }` -- same volatile-anchor
            // problem: the second `.any {`'s own physical line is not a stable fixed point because
            // it depends on how the first `.any { ... }`'s own closing `}` happens to render this
            // pass (found via `JetBrains/kotlin`'s `TopLevelPhases.kt`,
            // `isReferencedByNativeRuntime`, RDD_KEY_215's own residual).
            final boolean isChainedBooleanOp  = chainKwIdx >= 0&& chainKwIdx < current.size()&& current.get(
                chainKwIdx
            ).type == TokenType.OP&& ( "||".equals(
                current.get(chainKwIdx).text
            ) || "&&".equals(
                current.get(chainKwIdx).text
            ) );
            final boolean isChainedFluentCall = ! isChainedCatchFinally&& lang.isKotlin&& prevEffectiveSpanIndent != null&& prevCloseBraceIdx >= 0&& nextSignificantIndex(
                current, prevCloseBraceIdx
            ) == chainKwIdx&& chainKwIdx >= 0&& chainKwIdx < current.size()&& ( isChainedBooleanOp || ( current.get(
                chainKwIdx
            ).type == TokenType.OP&& ( ".".equals(
                current.get(chainKwIdx).text
            ) || "?.".equals(
                current.get(chainKwIdx).text
            ) ) ) );
            // A NAMED construct (class/interface/object/fun) must always indent its body relative
            // to its own header's start column (`spanIndent`), never to the physical line its
            // `{` happens to land on. A named header can wrap across multiple lines for reasons
            // that have nothing to do with a trailing-lambda call chain -- e.g. a Kotlin
            // multi-line generic `where` clause (`enforceWhereClausePlacement`, RDD_KEY_106)
            // whose continuation lines are indented deep to align under `where`, with the `{`
            // itself trailing the last constraint line. `braceIndent` reading that continuation
            // line's indent and `spanIndent == 0 < braceIndent` wrongly won under the plain
            // `braceIndent.length() > spanIndent.length()` test below, indenting the whole class
            // body (and its closing `}`/`} // class X` comment) under the `where` continuation
            // instead of column 0 -- reproduces only on a second format pass, once the `where`
            // clause is already wrapped in the input text (found via `kotlinx.coroutines`'s
            // `ThreadSafeHeap.kt`). The `braceIndent`-wins case (RDD_KEY_136) is for UNNAMED
            // trailing-lambda bodies at the end of a fluent call chain, where the brace's own
            // physical line genuinely is the body's real anchor -- that shape is never
            // `isNamedScope`.
            //
            // The same volatile-`braceIndent`-wins bug also hits a `fun` with a wrapped generic
            // `where` clause (RDD_KEY_159's own fix only covers `isNamedScope`, which
            // `NAMED_CONSTRUCT_KOTLIN` never includes `fun` in) -- e.g. `fun <T> f(...)\n    where
            // T : X {` reformats fine on round1 (`spanIndent` anchors the fun's own header start),
            // but on round2 `braceIndent` reads the now-wrapped `where` continuation line's own
            // (deeper) indent and wins the `braceIndent.length() > spanIndent.length()` test below,
            // pushing the whole body one level deeper each pass -- non-idempotent (found via
            // `JetBrains/kotlin`'s `TestStepBuilder.kt`/`common.kt`/`KaBaseSymbolRelationProvider.kt`/
            // `TopLevelPhases.kt`, RDD_KEY_214's own deferred residual). Fixed the same way as
            // `isNamedScope`: detect a depth-0 `where` keyword anywhere in this span's own header
            // (from the true statement start, not just `span.start`, mirroring `findParentIndent`'s
            // own backward scan) and force `spanIndent` in that case too.
            final boolean hasWhereClauseHeader = lang.isKotlin&& headerHasTopLevelWhereClause(
                current, span
            );
            if(isChainedCatchFinally || isChainedFluentCall)                            effectiveSpanIndent = prevEffectiveSpanIndent;
            else if(braceIndent == null || isNamedScope || hasWhereClauseHeader)        effectiveSpanIndent = spanIndent;
            else if( spanIndent == null || braceIndent.length() > spanIndent.length() ) effectiveSpanIndent = braceIndent;
            else effectiveSpanIndent = spanIndent;
            // Pre-expand named-construct one-liner bodies (`struct Foo { int a; int b; };`)
            // into multi-line form so that applyDeclarationsPass/applyAssignmentsPass in the
            // child scope see newline-separated source and produce correctly-indented output.
            // Non-named scopes (function/loop/lambda bodies) are left alone -- their one-liner
            // bodies must stay inline for getter/setter grouping and Allman detection.
            if( isNamedScope && !hasTopLevelNewline(
                current, span.openBraceIdx + 1, span.closeBraceIdx
            ) ) {
                final String trimmed = childSource.trim();
                if( !trimmed.isEmpty() && spanIndent != null ) childSource = "\n" + spanIndent + "    " + trimmed + "\n" + spanIndent;
            }
            // The indent statements directly inside the recursed-into child scope should have --
            // one level deeper than this span's own line, regardless of scope kind (`depth`
            // itself deliberately does NOT increment for `namespace` bodies for other reasons,
            // but a namespace body is still genuinely indented one level in real source, so the
            // *real*-indent thread below must increment unconditionally to match)
            final String childInheritedIndent = (effectiveSpanIndent != null ? effectiveSpanIndent : inheritedIndent) + indentUnit();
            final String childResult;
            if( !isNamedScope && !hasTopLevelNewline(
                current, span.openBraceIdx + 1, span.closeBraceIdx
            ) ) {
                // One-liner non-named body (method/constructor/loop `{ ... }` kept on its
                // original single line) -- recursing would run it through the §5/§6
                // declaration/assignment grouping passes, which assume a real multi-statement
                // block and split+column-align each statement onto its own line. That's wrong
                // here: a single-line body must stay exactly as written so later passes
                // (GetterSetterRule one-liner grouping, Allman-brace conversion) can still
                // recognize and handle it as a one-liner.
                childResult = childSource;
            } // if
            else {
                // A `namespace` body is never indented (STYLE_C_CPP.md §7's closing-comment
                // examples show namespace content flush with the namespace itself), unlike
                // every other named construct (class/struct/enum) or function/loop body -- so
                // it must not consume an indentation level the way `depth + 1` otherwise would.
                final int childDepth = isNamespaceScope(
                    current, span.openBraceIdx
                ) ? depth : depth + 1;
                final String                                                          rawChildResult = processScope(
                    tokenize(childSource, childStartFrozen),
                    childDepth,
                    childStartFrozen,
                    childInheritedIndent
                );
                // The gap between the last statement and the closing `}` belongs to no
                // statement, so no pass above ever re-derives its indentation from depth --
                // it is otherwise carried through verbatim from the original source (e.g. a
                // misindented `}` that happened to line up with its own body rather than with
                // the frame that opened it). Force it to the frame's own indent here, unless
                // the closing brace or its immediate content is a frozen (JXM_CFMT_DIS) region
                // that must be left byte-for-byte untouched.
                // Only force-reindent when the original gap right before `}` is pure
                // whitespace: a comment sitting there (e.g. between a block and a following
                // `else`) is content other passes already position/associate correctly, and
                // blindly reindenting around it has been observed to corrupt that placement.
                if( anyFrozen(current, span.openBraceIdx, span.closeBraceIdx + 1)
                        || trailingGapHasComment(
                            current, span.closeBraceIdx
                        ) || effectiveSpanIndent == null
                        || rawChildResult.trim().isEmpty() ) {
                    // An empty/whitespace-only body (`{}`/`{ }`) has no statement to hang a
                    // trailing gap off of -- forcing a "\n" + indent here would turn a genuinely
                    // empty body into an expanded `{\n}`, which a prior fix deliberately stopped
                    // doing (see STATE.md's java_modern empty-named-construct-body entry).
                    childResult = rawChildResult;
                } // if
                else {
                    // Whether the trailing gap being force-reindented already contained one or
                    // more deliberate blank source lines (extra consecutive NEWLINEs in the
                    // whitespace run being trimmed) -- if so, preserve that same blank-line count
                    // rather than collapsing straight to the closing brace. Whether this force-
                    // reindent branch fires at all depends on effectiveSpanIndent being
                    // text-derivable, which is NOT idempotent across passes for a statement
                    // anchored on a bare `else`/`catch`/`finally`: `findParentIndent` returns
                    // null (skip force-reindent) when that keyword still shares its physical
                    // line with the preceding `}` (K&R, pre-placeElseOnOwnLine, i.e. a fresh
                    // format's first pass), but returns a real indent (force-reindent fires) once
                    // a prior pass has already moved it onto its own line (Allman, i.e. every
                    // pass after the first) -- without this blank-line rescue, blank line(s) right
                    // before such a closing brace survived round1 only to be silently dropped (or
                    // reduced) on round2 (found via `javaparser/javaparser`'s `TypeExtractor.java`).
                    final int           trailingNewlines = Math.max(
                        1, trailingRunNewlineCount(rawChildResult)
                    );
                    final StringBuilder newlines         = new StringBuilder();
                    for(int nlI = 0; nlI < trailingNewlines; ++nlI) newlines.append('\n');
                    childResult = trimTrailingWhitespace(
                        rawChildResult
                    ) + newlines + effectiveSpanIndent;
                }
            }
            replacements.add(
                new Replacement(span.openBraceIdx + 1, span.closeBraceIdx, childResult)
            );
            prevCloseBraceIdx       = span.closeBraceIdx;
            prevEffectiveSpanIndent = effectiveSpanIndent;
        } // for

        return splice(current, replacements);
    }

    /**
     * Public entry point: tokenizes {@code source} and runs the recursive scope pipeline,
     *  starting at depth 0. The one method {@code Main.java} calls once per file.
     */
    public String process(final String source)
    {
        return processScope( tokenize(source, formatOff), 0, formatOff, "" );
    }

    /**
     * Starting from the declarator's own line ({@code lineFirst}), pulls in an immediately
     *  preceding leading `requires` clause line (`template<T>\n    requires ...\n Decl`), and --
     *  only when such a requires line was found -- the `template<...>` header line above *that*,
     *  so the whole group collapses onto the declarator's line. A bare `template<...>` header
     *  with no requires clause is left untouched (stays verbatim on its own line, per the
     *  resolved decision covering `cpp_modern_inp.cpp` Bug 4a) since the requires-line pull is
     *  what gates the template-line pull.
     */
    private int extendOverLeadingRequiresAndTemplate(
        final List<Token> tokens,
        final int         lineFirst,
        final int         leadStart
    )
    {
        int     cur            = lineFirst;
        boolean pulledRequires = false;
        while(true) {
            // `cur`'s own line is separated from the line above it by the NEWLINE directly
            // preceding `cur` (`ownLineSep`) -- the previous line's own first token sits between
            // the NEWLINE *before that* (`prevLineSep`) and `ownLineSep`
            int ownLineSep = - 1;
            for(int j = cur - 1; j >= leadStart; --j) {
                if( tokens.get(j).type == TokenType.NEWLINE ) { ownLineSep = j; break; }
            }
            if(ownLineSep < 0) break;
            int prevLineSep = - 1;
            for(int j = ownLineSep - 1; j >= leadStart; --j) {
                if( tokens.get(j).type == TokenType.NEWLINE ) { prevLineSep = j; break; }
            }
            final int prevLineFirst = prevLineSep >= 0
                    ? nextSignificantIndex(tokens, prevLineSep)
                    : (leadStart < ownLineSep ? leadStart : -1);
            if(prevLineFirst < 0 || prevLineFirst >= cur) break;
            final Token first = tokens.get(prevLineFirst);
            // A trailing `//` line comment on this line (its own last significant token,
            // scanning strictly within [prevLineFirst, ownLineSep)) means the line was
            // deliberately ended there in the original source -- joining it onto `cur`'s line
            // would silently swallow everything appended after it into the comment (compile-
            // breaking). Range-v3's `template(...)(\n    requires (...)) //\nDecl(...)` macro-
            // emulation shape hits exactly this. A trailing `/* ... */` block comment is *not*
            // the same hazard -- it's self-terminating, so joining more code after it on the
            // same rendered line is safe (and is the existing, intended behavior exercised by
            // `cpp_comments_inp.cpp`'s `requires /* numeric */ ... /* constraint */` case).
            int lastOnPrevLine = - 1;
            for(int j = ownLineSep - 1; j >= prevLineFirst; --j) {
                final TokenType tt = tokens.get(j).type;
                if(tt != TokenType.WHITESPACE && tt != TokenType.NEWLINE) { lastOnPrevLine = j; break; }
            }
            final boolean prevLineEndsInComment = lastOnPrevLine >= 0&& tokens.get(
                lastOnPrevLine
            ).type == TokenType.COMMENT_LINE;
            if( !pulledRequires && first.type == TokenType.KEYWORD && "requires".equals(first.text)
                    && !prevLineEndsInComment ) {
                cur            = prevLineFirst;
                pulledRequires = true;
                continue;
            }
            // Only a genuine `template<...>` generic-parameter header may be pulled in here --
            // range-v3's concept-emulation-macro convention (`#define template(...) ...`, see
            // `detail/prologue.hpp`) reuses the bare `template` keyword spelling followed by `(`
            // instead of `<` for a completely different macro-call shape
            // (`template(typename I)(requires ...)`). Without this `<`-check, that shape was
            // mistaken for the classic header and pulled onto the declarator's line together
            // with its own trailing `//` comment, silently commenting out the declarator that
            // followed on the same rendered line (a compile-breaking bug, not just cosmetic).
            final int afterTemplateIdx = nextSignificantIndex(tokens, prevLineFirst);
            if( pulledRequires && first.type == TokenType.KEYWORD && "template".equals(
                first.text
            ) && afterTemplateIdx >= 0 && isOp(
                tokens.get(afterTemplateIdx), "<"
            ) ) cur = prevLineFirst;
            break;
        } // while

        return cur;
    }

} // class ScopePipelineCurly
