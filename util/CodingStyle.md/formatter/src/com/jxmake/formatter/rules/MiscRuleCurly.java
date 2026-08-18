/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

/**
 * Catch-all for the remaining generic STYLE.md sections not owned by another rule class,
 * curly-brace-family (C/C++/Java/Kotlin) implementation: §1, §2, §3.2, §3.3, §4, §6, §8, §9, §15.
 */
public class MiscRuleCurly extends MiscRuleCore {

    public MiscRuleCurly(
        final Lang    lang,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod
    )
    {
        this(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, DEFAULT_INDENT_WIDTH,
                DEFAULT_LINE_LENGTH_LIMIT);
    }

    public MiscRuleCurly(
        final Lang    lang,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final int     indentWidth,
        final int     lineLengthLimit
    )
    {
        this(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, false, indentWidth, lineLengthLimit);
    }

    public MiscRuleCurly(
        final Lang    lang,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean commentNormalizationClassifier,
        final int     indentWidth,
        final int     lineLengthLimit
    )
    {
        this(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, commentNormalizationClassifier,
                false, "", indentWidth, lineLengthLimit);
    }

    /**
     * Full constructor additionally taking the {@code gru-classifier}/{@code gru-weights-path}
     * config values (STATE_AI.md Step 3) -- see {@code MiscRuleCore}'s own full constructor.
     */
    public MiscRuleCurly(
        final Lang    lang,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean commentNormalizationClassifier,
        final boolean gruClassifier,
        final String  gruWeightsPath,
        final int     indentWidth,
        final int     lineLengthLimit
    )
    {
        super(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, commentNormalizationClassifier,
                gruClassifier, gruWeightsPath, indentWidth, lineLengthLimit);
    }

    /**
     * Full constructor additionally taking the {@code line-length-with-comment} config value --
     * see {@link MiscRuleCore#lineLengthWithCommentLimit}'s own doc comment
     */
    public MiscRuleCurly(
        final Lang    lang,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean commentNormalizationClassifier,
        final boolean gruClassifier,
        final String  gruWeightsPath,
        final int     indentWidth,
        final int     lineLengthLimit,
        final int     lineLengthWithCommentLimit
    )
    {
        super(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, commentNormalizationClassifier,
                gruClassifier, gruWeightsPath, indentWidth, lineLengthLimit, lineLengthWithCommentLimit);
    }

    /**
     * Rewrites a bare postfix `i++;`/`i--;` expression statement (its value discarded) to
     * prefix `++i;`/`--i;`, per STYLE.md §4's "always prefer pre-increment except when post-
     * increment semantics are required by the surrounding expression" rule. Two shapes are
     * recognized, both via plain token-position checks (no AST):
     *   (1) A standalone statement: the IDENTIFIER is immediately preceded (ignoring a
     *       whitespace/comment/newline gap) by a statement boundary (`;`, `{`, `}`, or the
     *       start of the scope) and immediately followed by `;`, with paren/bracket depth 0 at
     *       that point -- this excludes a function call's arguments, an array index, and a
     *       for-loop header's own clauses (all "value used" or otherwise-handled contexts).
     *       A brace-less single-statement control-flow body (`if(x) i++;`) is NOT recognized as
     *       a boundary here -- that would require statement-start detection well beyond what
     *       STYLE.md's own "i++; at statement level" framing asks for, so it is left untouched
     *       as a documented gap rather than guessed at.
     *   (2) A `for(...; ...; i++)` loop's increment clause -- found by locating each `for`
     *       keyword's matching parens and splitting on the two top-level `;` inside; the third
     *       clause is rewritten when (and only when) it consists of exactly one IDENTIFIER
     *       followed by `++`/`--` and nothing else, since STYLE.md frames "prefer pre" as the
     *       general rule with exceptions only for value-is-used cases, and a discarded loop
     *       increment is not one of those. A more complex increment clause (comma-separated,
     *       `arr[i++]`, etc.) is left untouched -- STYLE.md gives no worked example beyond the
     *       single bare `i++`/`i--` shape.
     * Any candidate whose identifier/operator gap contains a comment or a `NEWLINE` is left
     * untouched, same conservative posture as the rest of this file.
     */
    public String enforcePreIncrement(final List<Token> tokens)
    {
        final Map<Integer, Integer> spans = new HashMap<>();
        collectBareStatementSpans(tokens, spans);
        collectForIncrementSpans(tokens, spans);

        return renderWithSwappedSpans(tokens, spans);
    }
    private void collectBareStatementSpans(
        final List<Token>           tokens,
        final Map<Integer, Integer> spans
    )
    {
        final int   n               = tokens.size();
              int   depth           = 0;
              Token lastSignificant = null;

        for(int i = 0; i < n; ++i) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) continue;
            if( isPunct(t, "(") || isPunct(t, "[") ) {
                ++depth;
            }
            else if( isPunct(t, ")") || isPunct(t, "]") ) {
                --depth;
            }
            else if( depth == 0 && t.type == TokenType.IDENTIFIER && isStatementBoundary(
                lastSignificant
            ) ) {
                final int opIdx = nextSignificantIndex(tokens, i + 1);
                if( opIdx >= 0 && isIncrementOp(
                    tokens.get(opIdx)
                ) && noBlockerBetween(
                    tokens, i, opIdx
                )
                        && !t.frozen && !tokens.get(opIdx).frozen ) {
                    final int termIdx = nextSignificantIndex(tokens, opIdx + 1);
                    if( termIdx >= 0 && isPunct( tokens.get(termIdx), ";" ) ) spans.put(i, opIdx);
                }
            }
            lastSignificant = t;
        } // for
    }
    private void collectForIncrementSpans(
        final List<Token>           tokens,
        final Map<Integer, Integer> spans
    )
    {
        final int n = tokens.size();

        for(int i = 0; i < n; ++i) {
            if( tokens.get(
                i
            ).type != TokenType.KEYWORD || !"for".equals(
                tokens.get(i).text
            ) ) continue;
            final int openParen = nextSignificantIndex(tokens, i + 1);
            if( openParen < 0 || !isPunct( tokens.get(openParen), "(" ) ) continue;
            final int closeParen = matchParenForward(tokens, openParen);
            if(closeParen < 0) continue;

            final List<Integer> semiIdx = new ArrayList<>();
                  int           depth   = 0;
            for(int k = openParen + 1; k < closeParen; ++k) {
                final Token tk = tokens.get(k);
                     if( isPunct(tk, "(") || isPunct(tk, "[") ) depth++;
                else if( isPunct(tk, ")") || isPunct(tk, "]") ) depth--;
                else if( depth == 0 && isPunct(tk, ";") ) semiIdx.add(k);
            }
            if( semiIdx.size() != 2 ) continue;

            final int incrStart = nextSignificantIndex( tokens, semiIdx.get(1) + 1 );
            if( incrStart < 0 || incrStart >= closeParen || tokens.get(
                incrStart
            ).type != TokenType.IDENTIFIER ) continue;
            final int opIdx = nextSignificantIndex(tokens, incrStart + 1);
            if( opIdx < 0 || opIdx >= closeParen || !isIncrementOp(
                tokens.get(opIdx)
            ) || !noBlockerBetween(
                tokens, incrStart, opIdx
            ) ) continue;
            final int afterOp = nextSignificantIndex(tokens, opIdx + 1);
            if(afterOp != closeParen) continue;
            if( tokens.get(incrStart).frozen || tokens.get(opIdx).frozen ) continue;
            spans.put(incrStart, opIdx);
        } // for i
    }
    private String renderWithSwappedSpans(
        final List<Token>           tokens,
        final Map<Integer, Integer> spans
    )
    {
        final StringBuilder out = new StringBuilder();
        final int           n   = tokens.size();
              int           i   = 0;

        while(i < n) {
            final Integer opIdx = spans.get(i);
            if(opIdx != null) {
                out.append( tokens.get(opIdx).text ).append( tokens.get(i).text );
                i = opIdx + 1;
                continue;
            }
            out.append( tokens.get(i).text );
            ++i;
        } // while

        return out.toString();
    }
public static final class Param {

        public final List<Token> typeTokens;
        public final Token       name;
        public final List<Token> sizeTokens;
        public final Token       comment;
        public final Token       leadingComment;

        Param(
            final List<Token> typeTokens,
            final Token       name,
            final List<Token> sizeTokens,
            final Token       comment,
            final Token       leadingComment
        )
        {
            this.typeTokens     = typeTokens;
            this.name           = name;
            this.sizeTokens     = sizeTokens;
            this.comment        = comment;
            this.leadingComment = leadingComment;
        }

} // class Param
public static final class Signature {

        public final List<Token> leadTokens;
        public final Token       name;
        public final List<Param> params;
        public final boolean     explicitVoidParam;

        Signature(
            final List<Token> leadTokens,
            final Token       name,
            final List<Param> params,
            final boolean     explicitVoidParam
        )
        {
            this.leadTokens        = leadTokens;
            this.name              = name;
            this.params            = params;
            this.explicitVoidParam = explicitVoidParam;
        }

} // class Signature
    /**
     * Parses `sigTokens` -- a function signature already isolated by the caller, spanning from
     * its first lead token (the first modifier, or the return type if there are none) through
     * and including the parameter list's closing `)`, and nothing past it (no `throws` clause,
     * no `{`/`;`) -- into a {@link Signature}. This rule's job is rendering (inline vs. broken,
     * param alignment), not discovering where a function signature starts in arbitrary source;
     * that boundary-finding is left to the caller, same granularity precedent as
     * `DeclarationAlignmentRule.parseDeclaration`'s pre-split `stmt` contract.
     * The name is identified as the IDENTIFIER immediately before the first depth-0 `(` (depth
     * tracked over Java generics via the tokenizer's distinct `ANGLE_BRACKET_OPEN`/`_CLOSE`
     * token types, so a generic return type like `Map<String, Integer> get(...)` doesn't
     * misidentify `Integer` as the name). Returns null -- leaving the candidate completely
     * untouched -- whenever: the shape doesn't match at all; `sigTokens` has trailing tokens past
     * the matched `)` (the caller included something this method doesn't handle); or any
     * parameter fails to parse (a default value, e.g. C++'s `int x = 0`, or any other shape with
     * no STYLE.md worked example).
     */
    public Signature parseSignature(final List<Token> sigTokens)
    {
        final List<Token> sig       = significantWithComments(sigTokens);
              int         openParen = -1;
              int         nameIdx   = -1;
              int         depth     = 0;
        for( int i = 0; i < sig.size(); ++i ) {
            final Token t = sig.get(i);
            if(t.type == TokenType.ANGLE_BRACKET_OPEN) {
                ++depth;
            }
            else if(t.type == TokenType.ANGLE_BRACKET_CLOSE) {
                --depth;
            }
            else if( depth == 0 && isPunct(t, "(") && i > 0
                    && sig.get(i - 1).type == TokenType.IDENTIFIER ) {
                openParen = i;
                nameIdx   = i - 1;
                break;
            }
        } // for
        if(openParen < 0) return null;
        final int closeParen = matchParenForward(sig, openParen);
        if( closeParen != sig.size() - 1 ) return null;

        final List<Token> leadTokens = new ArrayList<>( sig.subList(0, nameIdx) );
        // A `//` line comment anywhere among the return-type/qualifier lead tokens (e.g. a
        // multi-line signature where each original line deliberately ends in its own `//`
        // banner/decoration comment) can never be safely rendered here -- the renderer joins
        // `leadTokens` followed by `name`/`(`/params onto one text stream with no
        // per-line-comment awareness, so anything after the *first* such comment would be
        // silently swallowed into it (a compile-breaking corruption, not just a missed
        // reformat -- reproduced by range-v3's `view/view.hpp` `operator|` deleted-overload
        // ASCII-banner-commented declaration). Bail out and leave the whole declaration
        // untouched rather than risk that; a block comment (`/* ... */`) is self-terminating and
        // stays safe to join.
        for(final Token lt : leadTokens) {
            if(lt.type == TokenType.COMMENT_LINE) return null;
        }
        final Token       name        = sig.get(nameIdx);
        final List<Token> paramsSlice = sig.subList(openParen + 1, closeParen);

        if( paramsSlice.isEmpty() ) return new Signature(
            leadTokens, name, new ArrayList<Param>(), false
        );
        if( paramsSlice.size() == 1 && paramsSlice.get(
            0
        ).type == TokenType.KEYWORD && "void".equals(
            paramsSlice.get(0).text
        ) ) return new Signature(
            leadTokens, name, new ArrayList<Param>(), true
        );

        final List<List<Token>> parts = splitTopLevelCommas(paramsSlice);
        // A comment immediately after a comma (before the next param's own tokens) belongs to
        // the *previous* param -- e.g. `int b,   // second\nint c` -- but the comma-split above
        // leaves it as the leading token of the next part. Reattach it as that previous part's
        // trailing comment before parsing, so it doesn't get swept into the next param's type.
        for( int i = 0; i < parts.size() - 1; ++i ) {
            final List<Token> next = parts.get(i + 1);
            if( !next.isEmpty() && ( next.get(
                0
            ).type == TokenType.COMMENT_LINE || next.get(
                0
            ).type == TokenType.COMMENT_BLOCK ) ) parts.get(
                i
            ).add(
                next.remove(0)
            );
        } // for

        final List<Param> params = new ArrayList<>();
        for(final List<Token> slice : parts) {
            final Param p = parseParam(slice);
            if(p == null) return null;
            params.add(p);
        }

        return new Signature(leadTokens, name, params, false);
    }
    protected List<List<Token>> splitTopLevelCommas(final List<Token> tokens)
    {
        final List<List<Token>> parts   = new ArrayList<>();
              List<Token>       current = new ArrayList<>();
              int               depth   = 0;
        for(final Token t : tokens) {
            if( isPunct(t, "(") || isPunct(t, "[") || t.type == TokenType.ANGLE_BRACKET_OPEN ) {
                ++depth;
            }
            else if( isPunct(
                t, ")"
            ) || isPunct(
                t, "]"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE ) {
                --depth;
            }
            else if( depth == 0 && isPunct(t, ",") ) {
                parts.add(current);
                current = new ArrayList<>();
                continue;
            }
            current.add(t);
        } // for
        parts.add(current);

        return parts;
    }
    /**
     * Same split as {@link #splitTopLevelCommas}, but also tracks `{`/`}` depth (in addition to
     * paren/bracket/angle) so a comma inside a brace-bodied argument -- e.g. a lambda's own
     * parameter list, `{ source, target, exception -> ... }` -- is never mistaken for a top-level
     * argument separator. Deliberately a separate method rather than widening {@link
     * #splitTopLevelCommas} itself: that method's result also feeds {@link #parseSignature}/
     * {@link #renderCallDropped}/{@link #renderCallOnePerLine} rendering paths whose existing,
     * already-verified behavior for a C/C++/Java brace-init-list argument this fix must not risk
     * changing; this variant is used only where a brace-bodied argument's internal commas must
     * never split it apart, e.g. {@link #renderCallCandidate}'s per-argument multi-line-brace-body
     * bail check.
     */
    private List<List<Token>> splitTopLevelCommasBraceAware(final List<Token> tokens)
    {
        final List<List<Token>> parts   = new ArrayList<>();
              List<Token>       current = new ArrayList<>();
              int               depth   = 0;
        for(final Token t : tokens) {
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
            ) || isPunct(
                t, "}"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE ) {
                --depth;
            }
            else if( depth == 0 && isPunct(t, ",") ) {
                parts.add(current);
                current = new ArrayList<>();
                continue;
            }
            current.add(t);
        } // for
        parts.add(current);

        return parts;
    }
    /**
     * Splits {@code paramsSlice} into top-level (depth-0) comma-separated arguments -- exactly
     * like {@link #splitTopLevelCommas}, but tracking paren/bracket/angle depth *cumulatively
     * across the whole slice* rather than resetting it to 0 per original source line -- then
     * groups consecutive arguments back into "rows" by which original line each one *starts* on.
     * An argument that itself spans multiple lines (e.g. a lone nested call whose own argument
     * list wraps) stays one argument in one row; only a depth-0 {@code NEWLINE} seen since the
     * last depth-0 comma starts a new row. This replaces the old {@code splitOnNewlines} +
     * per-line {@link #splitTopLevelCommas} combination, which mis-split a nested comma sitting at
     * a line break (still inside an unclosed paren from the previous line) as if it were a local
     * top-level split point, corrupting the rendered output (see RDD_KEY_5 addendum). Trailing/
     * leading empty parts (a dangling comma with nothing after it before the closing paren) are
     * dropped. A depth-0 {@code NEWLINE} only counts as starting a new row if it occurs *before*
     * the part's first significant token (a leading newline) -- a trailing newline after a part's
     * last significant token (e.g. the newline before a lone closing-paren line) must not
     * retroactively mark that already-started part as beginning a new row, or a sibling argument
     * that legitimately shared a source line with it gets wrongly split onto its own line.
     */
    private List<List<List<Token>>> groupByOriginalLine(final List<Token> paramsSlice)
    {
        final List<List<Token>> parts                 = new ArrayList<>();
        final List<Boolean>     startsNewRow          = new ArrayList<>();
              List<Token>       current               = new ArrayList<>();
              boolean           currentHasSignificant = false;
              int               depth                 = 0;
              boolean           pendingNewRow         = true;              // First part always starts a new row
        for(final Token t : paramsSlice) {
            if( isPunct(t, "(") || isPunct(t, "[") || t.type == TokenType.ANGLE_BRACKET_OPEN ) {
                ++depth;
            }
            else if( isPunct(
                t, ")"
            ) || isPunct(
                t, "]"
            ) || t.type == TokenType.ANGLE_BRACKET_CLOSE ) {
                --depth;
            }
            else if( depth == 0 && isPunct(t, ",") ) {
                parts.add(current);
                startsNewRow.add(pendingNewRow);
                current               = new ArrayList<>();
                currentHasSignificant = false;
                pendingNewRow         = false;
                continue;
            }
            else if(depth == 0 && t.type == TokenType.NEWLINE && !currentHasSignificant) {
                pendingNewRow = true;
            }
            current.add(t);
            if( !isGapToken(t) ) currentHasSignificant = true;
        } // for
        parts.add(current);
        startsNewRow.add(pendingNewRow);

        final List<List<List<Token>>> rows = new ArrayList<>();
        for( int i = 0; i < parts.size(); ++i ) {
            if( significantOnly(
                parts.get(i)
            ).isEmpty() ) continue; // Dangling trailing/leading comma artifact
            if( startsNewRow.get(i) || rows.isEmpty() ) rows.add( new ArrayList<>() );
            rows.get( rows.size() - 1 ).add( parts.get(i) );
        } // for

        return rows;
    }
    /**
     * Reports whether {@code paramsSlice}'s last significant token (before the closing paren,
     * since {@code paramsSlice} is already the interior of a call/param list) is a `,` -- i.e.
     * whether the original source already had a trailing comma before `)`. Used only by the
     * Kotlin-gated trailing-comma-preservation exception in {@link #renderCallPreserveGroups}/
     * {@link #renderCallDropped}/{@link #renderCallOnePerLine} (STYLE_KOTLIN.md §7.2 -- a trailing
     * comma must be preserved exactly as written, never added or stripped -- see RDD_LOG.md's
     * trailing-comma-drop entry). {@code groupByOriginalLine}/{@code splitTopLevelCommas} both
     * discard this signal once they've split the slice into parts, so it must be checked against
     * the raw slice directly, before any splitting.
     */
    private boolean hasTrailingComma(final List<Token> paramsSlice)
    {
        for( int i = paramsSlice.size() - 1; i >= 0; --i ) {
            final Token t = paramsSlice.get(i);
            if( isGapToken(t) ) continue;
            return isPunct(t, ",");
        } // for
        return false;
    }
    /**
     * Parses one already-significant-only param slice, peeling a trailing `[size]` run (same
     * depth-tracked peel-off precedent as `DeclarationAlignmentRule.parseDeclaration`'s
     * `sizeTokens` loop) before requiring the final remaining token to be the IDENTIFIER name.
     */
    private Param parseParam(final List<Token> rawSlice)
    {
        if( rawSlice.isEmpty() ) return null;
          Token       comment = null;
          List<Token> slice   = rawSlice;
    final Token       last    = rawSlice.get( rawSlice.size() - 1 );
        if(last.type == TokenType.COMMENT_LINE || last.type == TokenType.COMMENT_BLOCK) {
            comment = last;
            slice   = rawSlice.subList( 0, rawSlice.size() - 1 );
        }
        if( slice.isEmpty() ) return null;
          Token leadingComment = null;
    final Token first          = slice.get(0);
        if( slice.size() > 1
                && (first.type == TokenType.COMMENT_LINE || first.type == TokenType.COMMENT_BLOCK) ) {
            leadingComment = first;
            slice          = slice.subList( 1, slice.size() );
        }
        for(final Token t : slice) {
            if( isOp(
                t, "="
            ) ) return null; // Default value -- no STYLE.md worked example, bail the whole signature
        }
          int         end        = slice.size();
    final List<Token> sizeTokens = new ArrayList<>();
        while( end > 0 && isPunct( slice.get(end - 1), "]" ) ) {
            int depth   = 0;
            int openIdx = -1;
            for(int k = end - 1; k >= 0; --k) {
                final Token t = slice.get(k);
                if( isPunct(t, "]") ) {
                    ++depth;
                }
                else if( isPunct(t, "[") ) {
                    --depth;
                    if(depth == 0) {
                        openIdx = k;
                        break;
                    }
                }
            } // for
            if(openIdx < 0) break;
            sizeTokens.addAll( 0, slice.subList(openIdx, end) );
            end = openIdx;
        } // while
        if(end <= 0) return null;
        final Token name = slice.get(end - 1);
        if(name.type != TokenType.IDENTIFIER) return null;
        final List<Token> typeTokens = new ArrayList<>( slice.subList(0, end - 1) );
        if( typeTokens.isEmpty() ) return null;
        // A real C++ type/qualifier run never ends in a tight-join member-access operator --
        // if it does, this "param" is actually a member-access expression (e.g. a member-
        // initializer-list entry like `_Pmtx(_Other._Pmtx)`, where `_Other._Pmtx` gets sliced
        // here as if it were a `Type name` pair: typeTokens=[_Other, .], name=_Pmtx) that only
        // *looks* like a declarator to this parser. Reject it so the whole signature parse
        // fails and the caller falls back to plain-call rendering (tight-join-`.`/`->`-aware)
        // instead of corrupting the expression by inserting a space after the `.`/`->`.
        final Token lastTypeTok = typeTokens.get( typeTokens.size() - 1 );
        if( isOp(lastTypeTok, ".") || isOp(lastTypeTok, "->") ) return null;

        return new Param(typeTokens, name, sizeTokens, comment, leadingComment);
    }
    /**
     * Renders one signature (STYLE.md §8) inline if it fits within {@link #lineLengthLimit}
     * at its starting column (`indentLevel * indentWidth`, per STYLE.md §1's tab-display-size-4
     * convention -- visual column, not raw character count, so the comparison is meaningful
     * regardless of `indentStyle`), or broken to one parameter per line otherwise. A zero-param
     * signature (including an explicit C `(void)`) is always rendered inline -- breaking achieves
     * nothing with no parameter to place on its own line, so an over-length zero-param signature
     * is left long rather than "broken" into a meaningless single-line shape.
     * <p>Broken form: the parameter type column is padded to {@code maxTypeLen + 1} --
     * unconditionally, the same convention established for §6's `maxPrefixLen` -- before the
     * normal single-space join; verified character-by-character against STYLE.md §8's own worked
     * example (`const char*`/`uint8_t`/`uint16_t`, max width 11, every row's gap is
     * `(11 - thisWidth) + 2`, i.e. `maxTypeLen + 1` padding plus one join space, not
     * `maxTypeLen + 1` total) that a plain `maxTypeLen`-width column (matching §5's declaration
     * grid) under-pads by exactly one space relative to this section's own example. The closing
     * `)` is indented to `indentLevel` (already resolved -- see STATE.md's §8 checklist: matches
     * the first character of the signature itself); parameter lines are indented to
     * `indentLevel + 1`.
     */
    public List<String> render(final Signature sig, final int indentLevel, final String indentStyle)
    {
        return render(sig, indentLevel, indentStyle, 0);
    }
    /**
     * @param trailingLen length of any trailing same-line text after the signature's own `)`
     * (e.g. a constructor's member-initializer-list opener, `: field(`) that the line-length
     * wrap decision below must also account for.
     */
    public List<String> render(
        final Signature sig,
        final int       indentLevel,
        final String    indentStyle,
        final int       trailingLen
    )
    {
        final String lead = renderTokens(sig.leadTokens);
        // JS/TS generator-method marker (`*iterate() {...}`): the bare `*` that can appear as
        // this signature's last lead token is never a pointer/reference declarator sigil in
        // JS/TS (unlike C/C++, where `isTightToken`'s default "tight against the type it
        // modifies, space before the name" behavior below is correct for `int* p`) -- it's
        // always the generator-function marker, which STYLE_JS_TS.md §6/§8 render tight against
        // the method name (`*iterate`, not `* iterate`). `needsSpaceBetween`'s generic default
        // (used for the C/C++ pointer case) would otherwise insert that space here since the
        // shared `isTightToken` only special-cases the token being joined *to* (`cur`), never a
        // bare `*` as `prev`. Scoped narrowly to lead-token-is-bare-`*` so it can't affect any
        // other lead-token join.
        final boolean leadEndsWithGeneratorStar = (lang.isJs || lang.isTs) && !sig.leadTokens.isEmpty() && isOp(
            sig.leadTokens.get( sig.leadTokens.size() - 1 ), "*"
        );
        final boolean leadNeedsSpace            = !sig.leadTokens.isEmpty() && !leadEndsWithGeneratorStar && needsSpaceBetween(
            sig.leadTokens.get( sig.leadTokens.size() - 1 ),
            sig.name,
            Collections.< Token > emptySet(),
            Collections.< Token > emptySet()
        );
        final String  head                      = ( lead.isEmpty() ? "" : lead + (leadNeedsSpace ? " " : "") ) + sig.name.text + "(";
        final String  inline                    = head + renderParamsInline(sig) + ")";
        final int     startColumn               = indentLevel * indentWidth;

        // Param comments don't count toward the line-length break decision -- only the code
        // itself should trigger wrapping to the multi-line param-per-line form
        int commentLen = 0;
        for(final Param p : sig.params) {
            if(p.comment != null) commentLen += p.comment.text.length() + 1;
        }
        // A `//` line comment on any param can never be rendered inline -- it would swallow
        // every token after it on the physical line (including the params/`)`/`{` that follow),
        // silently corrupting the rest of the file when re-tokenized by a later pass
        boolean hasLineComment = false;
        for(final Param p : sig.params) {
            if(p.comment != null && p.comment.type == TokenType.COMMENT_LINE) {
                hasLineComment = true;
                break;
            }
        }
        if( !hasLineComment && ( sig.params.isEmpty() || startColumn + inline.length() + trailingLen - commentLen <= lineLengthLimit ) ) return Collections.singletonList(
            inline
        );

        int maxTypeLen = 0;
        for(final Param p : sig.params) {
            if(p.leadingComment == null) maxTypeLen = Math.max(
                maxTypeLen, renderTokens(p.typeTokens).length()
            );
        }
        final int typeColWidth    = maxTypeLen + 1;
              int maxNameCommaLen = 0;
        for( int i = 0; i < sig.params.size(); ++i ) {
            final Param  p             = sig.params.get(i);
            final String nameCommaText = p.name.text + renderTokens(
                p.sizeTokens
            ) + ( i < sig.params.size() - 1 ? "," : "" );
            maxNameCommaLen = Math.max( maxNameCommaLen, nameCommaText.length() );
        } // for

        final List<String> lines = new ArrayList<>();
        lines.add(head);
        final String paramIndent = indentText(indentLevel + 1, indentStyle);
        for( int i = 0; i < sig.params.size(); ++i ) {
            final Param  p             = sig.params.get(i);
            final String typeText      = renderTokens(p.typeTokens);
            final String nameCommaText = p.name.text + renderTokens(
                p.sizeTokens
            ) + ( i < sig.params.size() - 1 ? "," : "" );
            final String nameText      = p.comment != null ? padRight(
                nameCommaText, maxNameCommaLen
            ) + " " + p.comment.text : nameCommaText;
            // A `//` line comment can never share a physical line with anything after it -- it
            // extends to end-of-line, so inlining it as a prefix before the type/name would
            // silently swallow this param's own declaration (and, once re-tokenized on a later
            // pass, cascade into swallowing whatever followed too) into the comment text,
            // corrupting the parameter list (found via `src/jxm` real-code testing:
            // `SWDFlashLoader.Specifier`'s constructor has a standalone `////...////` banner
            // comment as a section divider between parameter groups). Only a block comment
            // (`/* ... */`, which is self-terminating within the line) is safe to inline this way.
            final boolean leadingIsLineComment = p.leadingComment != null && p.leadingComment.type == TokenType.COMMENT_LINE;
            if(leadingIsLineComment) lines.add(paramIndent + p.leadingComment.text);
            final String leadPrefix = (p.leadingComment != null && !leadingIsLineComment) ? p.leadingComment.text + " " : "";
            // `typeColWidth` is derived only from params with no leadingComment at all (see the
            // `maxTypeLen` loop above), so a param preceded by a line comment -- excluded from
            // that computation -- can have a `typeText` as long as or longer than `typeColWidth`.
            // `padRight` is a no-op once the string already reaches the target width, which would
            // leave zero space between the type and the name, silently merging them into one
            // token on reformat (found via `src/jxm` real-code testing: `STM32QSPI.newQSPICmd`'s
            // `// Instruction` comment before its first param). Guarantee at least one space by
            // never padding to less than `typeText.length() + 1`.
            final String typeCell = (p.leadingComment != null && !leadingIsLineComment) ? typeText + " " : padRight(
                typeText, Math.max( typeColWidth, typeText.length() + 1 )
            );
            lines.add(paramIndent + leadPrefix + typeCell + nameText);
        } // for
        lines.add( indentText(indentLevel, indentStyle) + ")" );

        return lines;
    }
    private String renderParamsInline(final Signature sig)
    {
        if( sig.params.isEmpty() ) return sig.explicitVoidParam ? "void" : "";
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < sig.params.size(); ++i ) {
            if(i > 0) sb.append(", ");
            final Param p = sig.params.get(i);
            sb.append( renderTokens(p.typeTokens) ).append(' ')
                    .append(p.name.text).append( renderTokens(p.sizeTokens) );
            if(p.comment != null) sb.append(' ').append(p.comment.text);
        } // for

        return sb.toString();
    }
    private static final class FuncFrame {

        final boolean isFunctionBody;
        final boolean multiLine;
              boolean sawContent;

        FuncFrame(final boolean isFunctionBody, final boolean multiLine)
        {
            this.isFunctionBody = isFunctionBody;
            this.multiLine      = multiLine;
            this.sawContent     = false;
        }

    } // class FuncFrame
    /**
     * Inserts exactly one blank line before a `return` statement when (STYLE.md §9): the
     * enclosing function body is itself multi-line, AND the `return` sits directly inside that
     * body (not inside a further-nested block) with at least one statement already before it.
     * Reuses the same gap-buffering / "leave an existing blank line untouched, only ever add a
     * missing one" precedent as `BlockStructureRule.insertNamedConstructBlankLines`.
     * <p>"Function body" is detected the same minimal, purely structural signal already noted
     * (for a different, not-yet-wired-in purpose) in this file's "§11 K&R brace style detection"
     * Resolved Design Decision: a `{` whose immediately preceding significant token is a `)`
     * whose matching `(` is itself preceded by an IDENTIFIER -- which is enough on its own to
     * exclude every control-flow brace (`if`/`while`/`for`/`switch`/`catch` are all preceded by a
     * KEYWORD there, never an IDENTIFIER) and every lambda (preceded by `->`, never `)`), with no
     * per-keyword exclusion list needed. One more guard is added beyond that precedent: if the
     * identifier itself is preceded by `new`, this is a constructor call / anonymous class
     * instantiation, not a method definition -- excluded so an anonymous class's own body is
     * never misclassified as the function body of whatever constructor created it. Known,
     * deliberate gap (no STYLE.md worked example to resolve it against): a C++ method with a
     * trailing qualifier between `)` and `{` (`void foo() const { ... }`) is not recognized --
     * the brace is misclassified as not-a-function-body and this rule simply does nothing there,
     * never anything actively wrong.
     * <p>STYLE.md §9's only documented exclusion is the brace-less `if(x) return y;` shape (§10);
     * generalized here to a brace-less `while`/`for` controlled body too, since the underlying
     * reasoning STYLE.md states for the exclusion -- "the `return` is at function scope... not
     * inside a nested block" -- applies identically to those, even without literal braces.
     * <p>A `return` whose immediately preceding gap contains a comment, or contains zero
     * newlines (i.e. shares a source line with the previous statement), is left untouched --
     * neither shape has a STYLE.md worked example to justify guessing where the blank line (or,
     * for the zero-newline case, a new line break that doesn't yet exist at all) should go.
     */
    public String insertBlankLineBeforeReturn(final List<Token> tokens)
    {
        final StringBuilder    out   = new StringBuilder();
        final Deque<FuncFrame> stack = new ArrayDeque<>();
        final List<Token>      gap   = new ArrayList<>();
        final int              n     = tokens.size();
              int              i     = 0;

        while(i < n) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                ++i;
                continue;
            }

            final boolean hasComment   = gap.stream().anyMatch(
                g->g.type == TokenType.COMMENT_LINE || g.type == TokenType.COMMENT_BLOCK
            );
            final long    newlineCount = gap.stream().filter(
                g->g.type == TokenType.NEWLINE
            ).count();
            if( shouldForceBlankBeforeReturn(tokens, i, stack) && hasComment && lang.isKotlin ) {
                // RDD_KEY_129, Kotlin-only carve-out: a `return` directly preceded by its own
                // standalone leading comment (e.g. `// Comment before return` on the line right
                // above) still gets STYLE.md §9's forced blank line -- but the blank belongs
                // *between the comment and the `return`*, not before the comment (which may
                // already have its own blank line separating it from whatever precedes it, and
                // must not be relocated). Java/C++ deliberately keep the original "leave
                // untouched" behavior here (see java_comments_out.java's own
                // "// Line comment before return" case, confirmed via that existing fixture to
                // want no blank line inserted at all) -- this is a genuine per-language style
                // difference, not a shared-class gap.
                appendGapWithForcedBlankAfterLastComment(out, gap);
            } // if
            else if( shouldForceBlankBeforeReturn(
                tokens, i, stack
            ) && !hasComment && newlineCount >= 1 ) {
                appendGapWithForcedBlank(out, gap, newlineCount);
            }
            else {
                for(final Token g : gap) out.append(g.text);
            }
            gap.clear();

            if( !stack.isEmpty() ) stack.peek().sawContent = true;
            if( isPunct(t, "{") ) {
                final boolean isFuncBody = isFunctionBodyBrace(tokens, i);
                stack.push(
                    new FuncFrame( isFuncBody, isFuncBody && spansMultipleLines(tokens, i) )
                );
            }
            else if( isPunct(t, "}") && !stack.isEmpty() ) {
                stack.pop();
            }
            out.append(t.text);
            ++i;
        } // while
        for(final Token g : gap) out.append(g.text);

        return out.toString();
    }
    private boolean shouldForceBlankBeforeReturn(
        final List<Token>      tokens,
        final int              idx,
        final Deque<FuncFrame> stack
    )
    {
        final Token t = tokens.get(idx);
        if( t.type != TokenType.KEYWORD || !"return".equals(
            t.text
        ) || stack.isEmpty() || t.frozen ) return false;
        final FuncFrame top = stack.peek();
        if(!top.isFunctionBody || !top.multiLine || !top.sawContent) return false;

        return !isBraceLessControlFlowReturn(tokens, idx);
    }
    /**
     * Kotlin-only sibling of {@link #appendGapWithForcedBlank} (see RDD_KEY_129): forces the
     * blank line into the sub-gap strictly after the *last* comment token in {@code gap} (the
     * return statement's own standalone leading comment), leaving everything at/before that
     * comment untouched -- if the sub-gap after it already has 2+ newlines, nothing changes; if
     * it has exactly 1, a second is inserted right after it; if it has 0 (comment glued to the
     * same line as the `return`), left untouched, same "no worked example to guess from" posture
     * as the zero-newline case in {@link #appendGapWithForcedBlank}'s caller
     */
    private void appendGapWithForcedBlankAfterLastComment(
        final StringBuilder out,
        final List<Token>   gap
    )
    {
        int lastCommentIdx = -1;
        for( int k = 0; k < gap.size(); ++k ) {
            final Token g = gap.get(k);
            if(g.type == TokenType.COMMENT_LINE || g.type == TokenType.COMMENT_BLOCK) lastCommentIdx = k;
        }
        long afterNewlineCount = 0;
        for( int k = lastCommentIdx + 1; k < gap.size(); ++k ) {
            if( gap.get(k).type == TokenType.NEWLINE ) afterNewlineCount++;
        }
        if(afterNewlineCount == 0 || afterNewlineCount >= 2) {
            for(final Token g : gap) out.append(g.text);
            return;
        }
        boolean inserted = false;
        for( int k = 0; k < gap.size(); ++k ) {
            final Token g = gap.get(k);
            out.append(g.text);
            if(!inserted && k > lastCommentIdx && g.type == TokenType.NEWLINE) {
                out.append('\n');
                inserted = true;
            }
        } // for
    }
    private void appendGapWithForcedBlank(
        final StringBuilder out,
        final List<Token>   gap,
        final long          newlineCount
    )
    {
        if(newlineCount >= 2) {
            for(final Token g : gap) out.append(g.text);
            return;
        }
        boolean inserted = false;
        for(final Token g : gap) {
            out.append(g.text);
            if(!inserted && g.type == TokenType.NEWLINE) {
                out.append('\n');
                inserted = true;
            }
        } // for
    }
    /**
     * A `return` immediately preceded by `)` whose matching `(` is preceded by `if`/`while`/
     * `for`/`switch` is the controlled body of a brace-less single-statement control-flow
     * construct -- not at function scope, regardless of which frame is on top of the stack
     * (a brace-less body never pushes its own frame)
     */
    private boolean isBraceLessControlFlowReturn(final List<Token> tokens, final int returnIdx)
    {
        final int closeParen = prevSignificantIndex(tokens, returnIdx - 1);
        if( closeParen < 0 || !isPunct( tokens.get(closeParen), ")" ) ) return false;
        final int openParen = matchParenBackward(tokens, closeParen);
        if(openParen < 0) return false;
        final int kwIdx = prevSignificantIndex(tokens, openParen - 1);

        return kwIdx >= 0 && tokens.get(kwIdx).type == TokenType.KEYWORD
                && TIGHT_PAREN_KEYWORDS.contains( tokens.get(kwIdx).text );
    }
    private boolean isFunctionBodyBrace(final List<Token> tokens, final int braceIdx)
    {
        // Skip a `throws ExceptionType, ExceptionType...` clause, if present.
        int closeParen = skipThrowsClauseBackward(
            tokens, prevSignificantIndex(tokens, braceIdx - 1)
        );
        // Skip post-paren qualifiers (const, volatile, noexcept, override, final, throws)
        while( closeParen >= 0 && isFunctionBodyQualifier(
            tokens.get(closeParen)
        ) ) closeParen = prevSignificantIndex(
            tokens, closeParen - 1
        );
        // Also handle trailing return type: `auto foo() -> ReturnType {`. Kotlin-only: skip this
        // C++-shaped `->` scan entirely rather than running it first and letting it clobber
        // `closeParen` to -1 on every Kotlin function (Kotlin never has `->` here, so the scan
        // always runs off the front of the signature and fails) before the Kotlin-specific `:`
        // check below ever gets a chance to see the original, still-valid `closeParen`.
        if( closeParen >= 0 && !isPunct(
            tokens.get(closeParen), ")"
        ) && !lang.isKotlin ) closeParen = findCloseParenBeforeTrailingReturnType(
            tokens, closeParen
        );
        // Kotlin's own return-type shape is different again: `fun foo(): ReturnType {` -- a `:`
        // rather than C++'s `->` (STYLE_KOTLIN.md §7). Without this, `isFunctionBodyBrace` never
        // recognized any Kotlin function *with* an explicit return type as a function body at
        // all (the C/Java-shaped check just above requires `)` to directly precede `{`, and the
        // C++ `->`-based check right above this one obviously never matches Kotlin's `:` either)
        // -- silently disabling `insertBlankLineBeforeReturn`/STYLE.md §9 for every such function,
        // never for a Kotlin function with an inferred/omitted return type (which has no `:` at
        // all and already falls straight through to the plain `)` check above, unaffected).
        if( closeParen >= 0 && !isPunct(
            tokens.get(closeParen), ")"
        ) && lang.isKotlin ) closeParen = findCloseParenBeforeKotlinReturnType(
            tokens, closeParen
        );
        if( closeParen < 0 || !isPunct( tokens.get(closeParen), ")" ) ) return false;
        final int openParen = matchParenBackward(tokens, closeParen);
        if(openParen < 0) return false;
        final int nameIdx = prevSignificantIndex(tokens, openParen - 1);
        if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) return false;
        final int beforeName = prevSignificantIndex(tokens, nameIdx - 1);

        return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                || !"new".equals( tokens.get(beforeName).text );
    }
    /**
     * If {@code fromIdx} is the last token of a `throws ExceptionType, ExceptionType...` clause
     * (a comma-separated identifier/qualified-name list immediately preceded by the `throws`
     * keyword), returns the index just before that keyword; otherwise returns {@code fromIdx}
     * unchanged.
     */
    private int skipThrowsClauseBackward(final List<Token> tokens, final int fromIdx)
    {
        int i = fromIdx;
        if( i < 0 || tokens.get(i).type != TokenType.IDENTIFIER ) return fromIdx;
        while(i >= 0) {
            if( tokens.get(i).type != TokenType.IDENTIFIER ) return fromIdx;
            int prev = prevSignificantIndex(tokens, i - 1);
            while( prev >= 0 && isOp( tokens.get(prev), "." ) ) {
                prev = prevSignificantIndex(tokens, prev - 1);
                if( prev < 0 || tokens.get(prev).type != TokenType.IDENTIFIER ) return fromIdx;
                prev = prevSignificantIndex(tokens, prev - 1);
            }
            if( prev >= 0 && tokens.get(
                prev
            ).type == TokenType.KEYWORD && "throws".equals(
                tokens.get(prev).text
            ) ) return prevSignificantIndex(
                tokens, prev - 1
            );
            if( prev >= 0 && isPunct( tokens.get(prev), "," ) ) {
                i = prevSignificantIndex(tokens, prev - 1);
                continue;
            }
            return fromIdx;
        } // while

        return fromIdx;
    }
    /**
     * Kotlin analog of {@link #findCloseParenBeforeTrailingReturnType}: scans backward from
     * {@code fromIdx} through a Kotlin return type (`: ReturnType`), tracking angle-bracket and
     * paren depth; returns the function's close paren that precedes the top-level `:`, or -1 if
     * not found
     */
    private int findCloseParenBeforeKotlinReturnType(final List<Token> tokens, final int fromIdx)
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
            else if( depth == 0 && isOp(t, ":") ) {
                final int beforeColon = prevSignificantIndex(tokens, i - 1);
                return ( beforeColon >= 0 && isPunct(
                    tokens.get(beforeColon), ")"
                ) ) ? beforeColon : -1;
            }
            else if( depth == 0 && ( isPunct(t, "{") || isPunct(t, "}") ) ) {
                return -1;
            }
        } // for

        return -1;
    }
    private boolean isFunctionBodyQualifier(final Token t)
    {
        if(t.type != TokenType.KEYWORD) return false;
        switch(t.text) {
            case "const"    : /* FALL-THROUGH */
            case "volatile" : /* FALL-THROUGH */
            case "noexcept" : /* FALL-THROUGH */
            case "override" : /* FALL-THROUGH */
            case "final"    : /* FALL-THROUGH */
            case "throws"   : return true ;
            default         : return false;
        } // switch
    }
    /**
     * Scans backward from {@code fromIdx} through a trailing return type, tracking angle-bracket
     * and paren depth; returns the function's close paren that precedes {@code ->} (skipping any
     * post-paren qualifiers between {@code )} and {@code ->}), or -1 if not found
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
                int beforeArrow = prevSignificantIndex(tokens, i - 1);
                while( beforeArrow >= 0 && isFunctionBodyQualifier(
                    tokens.get(beforeArrow)
                ) ) beforeArrow = prevSignificantIndex(
                    tokens, beforeArrow - 1
                );
                return ( beforeArrow >= 0 && isPunct(
                    tokens.get(beforeArrow), ")"
                ) ) ? beforeArrow : -1;
            }
            else if( depth == 0 && ( isPunct(t, "{") || isPunct(t, "}") ) ) {
                return -1;
            }
        } // for

        return -1;
    }
    /** True iff a `NEWLINE` token appears anywhere between `openBraceIdx` and its matching `}` */
    private boolean spansMultipleLines(final List<Token> tokens, final int openBraceIdx)
    {
        int depth = 0;
        for( int k = openBraceIdx; k < tokens.size(); ++k ) {
            final Token t = tokens.get(k);
            if( isPunct(t, "{") ) {
                ++depth;
            }
            else if( isPunct(t, "}") ) {
                --depth;
                if(depth == 0) return false;
            }
            else if(t.type == TokenType.NEWLINE) {
                return true;
            }
        } // for

        return false;
    }
    /**
     * Finds every call/forward-declaration candidate -- an IDENTIFIER immediately followed by a
     * `(` whose matching `)` is *not* immediately followed by `{` (that shape is a true function
     * signature, already fully handled, deterministically, by `ScopePipeline.applySignaturePass`
     * before this pass ever runs) -- and rewrites it per STYLE.md §8's four candidate forms
     * (RDD_EXT_4 through RDD_EXT_9; see also RDD_KEY_4 for this method's own architecture). Three
     * scope-limiting decisions, each consistent with this file's existing conservative posture
     * elsewhere, were made writing this method (see RDD_KEY_5 for the full write-up):
     * <ul>
     * <li><b>Nesting:</b> once a `(` is recognized as a candidate (regardless of whether it ends
     * up rewritten), every token through its matching `)` is "claimed" and skipped for the rest of
     * this scan -- a call/declaration nested inside another's argument list is never independently
     * processed in the same pass; its original text rides along verbatim as part of the outer
     * candidate's own rendering (or is left alone if the outer doesn't rewrite). A later format run
     * would pick up a still-too-long nested call once the outer has already been broken and the
     * nested one has more room, or remains its own top-level candidate if the outer no longer
     * contains it.</li>
     * <li><b>Comments:</b> any comment token anywhere between a candidate's `(` and `)` disqualifies
     * the *entire* candidate -- it is left byte-for-byte untouched. STYLE.md §8's fuller comment
     * rules (trailing-comment-preserved, comment-only-line-preserved, inline-block-comment-
     * normalized-in-place, leading-preamble-comment-disqualifies) are not implemented; this is a
     * deliberate, documented gap, the same "a comment in the gap blocks the rewrite" posture used by
     * {@link #enforceKeywordSpacing} and `CppSpecificRule.enforceRequiresClausePlacement`.</li>
     * <li><b>Call vs. declaration:</b> {@link #parseSignature} is attempted on the candidate's own
     * `name(...)` span; success (every comma-separated segment fits {@link Param}'s typed
     * "[type] name [size]" shape) means a forward declaration -- rendered with the existing typed
     * {@link Signature}/{@link Param} machinery. Failure means a plain call -- rendered from the
     * raw, untyped per-argument token slices instead (no type/name split attempted).</li>
     * </ul>
     * A zero-parameter candidate (including an explicit C `(void)`, which still parses to an empty
     * {@link Signature}) is always left alone -- breaking achieves nothing with no argument to place
     * on its own line, same precedent as {@link #render}'s own zero-param handling.
     */
    public String enforceCallLineBreaking(final List<Token> tokens)
    {
        final List<int[]>  spans      = new ArrayList<>();
        final List<String> renders    = new ArrayList<>();
              int          scanCursor = 0;

        for( int i = 0; i < tokens.size(); ++i ) {
            if( i < scanCursor || !isPunct( tokens.get(i), "(" ) ) continue;
            final int nameIdx = prevSignificantIndex(tokens, i - 1);
            if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) continue;
            final int closeIdx = matchParenForward(tokens, i);
            if(closeIdx < 0) continue;
            scanCursor = closeIdx + 1; // Claim the interior -- see "Nesting" above, applies even if skipped below

            final int afterClose = nextSignificantIndex(tokens, closeIdx + 1);
            if( afterClose >= 0 && isPunct(
                tokens.get(afterClose), "{"
            ) ) continue; // True signature -- ScopePipeline's concern, not ours
            if( lang.isKotlin && afterClose >= 0 && isOp( tokens.get(afterClose), ":" )
                    && isKotlinReturnTypeThenBlockBody(tokens, afterClose) ) {
                // Kotlin function signature with an explicit `: ReturnType` tail followed by a
                // real `{`-bodied block (no `=` in between) -- e.g. `override fun copy(...):
                // LambdaTypeName {`. The plain immediate-`{` check above misses this shape since
                // the return type sits between `)` and `{`. Without this, ScopePipeline's own
                // KotlinSignatureRule.render already fully resolved this candidate's column
                // padding + trailing-comma-preserved multi-line form, but this pass would still
                // treat it as an ordinary untyped call and re-wrap it via the untyped
                // call-argument path (RDD_KEY_144's `sigForRender` forcing), silently discarding
                // that padding and trailing comma (RDD_KEY_149). A `: ReturnType = expr`
                // tail is deliberately NOT exempted here -- that shape's untouched trailing
                // expression body is exactly what this pass still needs to account for when
                // deciding whether the signature's own params must wrap (see effectiveLineEndIndex).
                continue; // True signature -- ScopePipeline's concern, not ours
            } // if
            if( hasCommentBetween(tokens, i, closeIdx) ) continue; // See "Comments" above
            if( anyFrozen(
                tokens, nameIdx, closeIdx + 1
            ) ) continue; // Frozen span (RDD_KEY_90 §A) -- left untouched

            final String rendered = renderCallCandidate(tokens, nameIdx, i, closeIdx);
            if(rendered != null) {
                spans.add( new int[] { i, closeIdx + 1 } );
                renders.add(rendered);
            }
        } // for

        if( spans.isEmpty() ) return joinVerbatim(tokens);
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
     * Dispatches one candidate (its `(`/`)` span already located by the caller) to the
     * appropriate option per RDD_EXT_4's candidate-availability matrix, collapsed to the
     * deterministic subset since AI selection (Step 2) is permanently not feasible: a multi-line
     * source candidate always gets Option 2 (preserve groups); a single-line candidate that
     * already fits returns {@code null} (Option 0, no change); otherwise Option 1 (dropped),
     * falling back to Option 3 (one-per-line) if dropped itself doesn't fit. Returns the
     * replacement text for the half-open span {@code [openIdx, closeIdx]} (i.e. starting with the
     * `(` token's own text and ending with the `)` token's own text), or {@code null} for no
     * change.
     */
    private String renderCallCandidate(
        final List<Token> tokens,
        final int         nameIdx,
        final int         openIdx,
        final int         closeIdx
    )
    {
        final Signature sig = parseSignature( tokens.subList(nameIdx, closeIdx + 1) );
        if( sig != null && sig.params.isEmpty() ) return null; // Zero-param -- never broken, see this method's class-level doc comment
        // This C/C++/Java-style `type name` signature parse exists to tell a real forward
        // declaration's parameter list apart from a plain call's argument list (both are just
        // "IDENTIFIER ( ... )" at this scan's level) -- but Kotlin has no such prototype-only
        // declaration shape at all (a Kotlin function signature always starts with `fun` and
        // uses the reversed `name: Type` order, handled entirely by ScopePipeline elsewhere, not
        // this pass). Used only for its zero-param bail-out above (still needed -- a Kotlin
        // function's own empty `()` param list must never be wrapped either, and non-Kotlin
        // reasoning doesn't otherwise change here), the typed rendering paths below (`sig != null`
        // branches) must never be selected for Kotlin: parseParam's generic "last IDENTIFIER is
        // the name, everything before it is the type" heuristic can misparse an ordinary call
        // argument that happens to be a dotted member-access expression with no top-level comma
        // (e.g. `calledFunctions.contains(it.func.funcName)`) as if `it.func.funcName` were a
        // `Type name` pair (type `it.func.`, name `funcName`) -- then the typed dropped/
        // one-per-line render path inserts a column-separator space between them, corrupting the
        // expression into `it.func. funcName`, a parse error (found via dogfood-testing
        // RobotCoding gui_frontend_android's ProgramBuilder.kt). Forcing every Kotlin candidate's
        // render-path selection through the untyped call-argument path instead never inserts a
        // space where the source had none.
        //
        // JS/TS has the exact same exposure, for the exact same structural reason: neither
        // language has a C/C++/Java-style prototype-only forward declaration shape either (a JS/TS
        // function declaration always has an immediate `{` body, already exempted above by the
        // `afterClose == "{"` check), so any "IDENTIFIER ( ... )" this pass still sees is always a
        // plain call, never a real signature. Without this, a multi-arg call whose arguments are
        // each a bare dotted member-access expression (`getInjectionProviders(options.
        // provideInjectionTokensFrom, options.inject)`) can misparse the same way -- each arg's
        // trailing identifier read as a parameter name, everything before it (`options.`) read as
        // its type -- corrupting the expression into `options. provideInjectionTokensFrom` (found
        // via nestjs/nest real-code testing, `configurable-module.builder.ts`).
        final Signature sigForRender = (lang.isKotlin || lang.isJs || lang.isTs) ? null : sig;

        final List<Token> paramsSlice = tokens.subList(openIdx + 1, closeIdx);
        final String      baseIndent  = effectiveCallBaseIndent(tokens, nameIdx);

        if( containsNewline(paramsSlice) ) {
            // Idempotency fix (nestjs/nest real-code testing, `integration/repl/e2e/
            // repl-process.spec.ts`'s `join(workspaceRoot, '...')` call): before falling through
            // to the original-grouping-preserving Option 2 path below, check whether a plain call
            // (not a real forward declaration -- `sigForRender == null`) would actually fit back
            // onto one line if collapsed. Previously this branch preserved the original multi-line
            // grouping *unconditionally* whenever the source already had one, with no fits-check
            // at all -- so a call an author had wrapped (or that a previous format pass had left
            // wrapped) stayed wrapped forever on this pass even once it easily fit under
            // `lineLengthLimit`, while a *fresh* single-line source over the limit still correctly
            // collapsed via Option 1/3 below. That asymmetry meant the exact same logical call could
            // render two different stable shapes depending purely on incidental prior formatting,
            // and a call sitting right at the boundary could flip between a preserved multi-line
            // form (round1, since `renderCallPreserveGroups`'s own per-line rendering happened to
            // differ byte-for-byte from a fresh single-line collapse) and a freshly-collapsed
            // one-line form (round2, once round1's own output was re-scanned and this fits-check
            // -- reached this time because Option 2's own output still technically contains a
            // newline -- finally fired) -- non-idempotent. `wholeLine` here reuses the exact same
            // whitespace-collapsing helper as the measurement below (including any trailing `,`
            // before `)` from a trailing-comma source style), which can only ever *overestimate*
            // length by the one stray comma character -- never underestimate -- so this fits-check
            // is at worst one byte more conservative than the real rendered form, never wrongly
            // permissive.
            //
            // Scoped to JS/TS only (`lang.isJs || lang.isTs`): re-running the full test suite with
            // this fits-check applied unconditionally to every language regressed
            // `real_code_regressions_1` (C++) -- a plain call whose arguments are ordinary
            // expressions (not typed "type name" pairs) also has `sigForRender == null` in C/C++/
            // Java, which is the common case for any real call in those languages too, not a
            // JS/TS-specific signal the way it is here (see the Kotlin/JS/TS-only comment on
            // `sigForRender`'s own assignment above). Collapsing those to one line changed
            // long-settled, deliberately-preserved multi-line C++ call formatting; this bug was
            // only ever reported against a JS/TS call (nestjs/nest's `join(...)`), so the fix stays
            // scoped to where it was actually observed and verified, rather than risking
            // undocumented behavior changes in the other three languages.
            if( sigForRender == null && (lang.isJs || lang.isTs) ) {
                final List<List<Token>> args                  = splitTopLevelCommas(paramsSlice);
                      boolean           anyArgHasBraceNewline = false;
                for(final List<Token> arg : args) {
                    if( containsInternalNewline(arg) && containsBrace(arg) ) {
                        anyArgHasBraceNewline = true;
                        break;
                    }
                }
                if(!anyArgHasBraceNewline) {
                    // Drop a dangling trailing empty group (a trailing comma before `)` with
                    // nothing after it -- e.g. this codebase's own multi-line call style,
                    // `foo(\n  a,\n  b,\n);` -- `splitTopLevelCommas` (unlike its sibling
                    // `groupByOriginalLine`) doesn't drop it itself; without this, the collapsed
                    // one-line form gained a spurious trailing `, ` before `)`.
                    while( !args.isEmpty() && significantOnly(
                        args.get( args.size() - 1 )
                    ).isEmpty() ) args.remove(
                        args.size() - 1
                    );
                    final StringBuilder argsText = new StringBuilder();
                    for( int i = 0; i < args.size(); ++i ) {
                        if(i > 0) argsText.append(", ");
                        argsText.append( collapseTokensToOneLine( args.get(i) ) );
                    }
                    final String candidate = "(" + argsText + ")";
                    // Measure the *actual* tight candidate text (no phantom space at the `(`/`)`
                    // boundary) rather than reusing the loose whitespace-collapsing
                    // `collapseToOneLine` helper used by the sibling non-newline fits-check below --
                    // that helper turns *any* whitespace/newline run (including the newline that
                    // originally followed this call's own `(`) into a single space, which would
                    // measure a phantom `join( workspaceRoot` / `...ts', )` shape that this
                    // candidate's own tight rendering never actually produces, overestimating length
                    // by up to 2 characters and wrongly disqualifying a call that truly fits.
                    final StringBuilder prefix = new StringBuilder();
                    appendRange( prefix, tokens, lineStartIndex(tokens, nameIdx), openIdx );
                    final StringBuilder suffix = new StringBuilder();
                    appendRangeCollapsingTrailingCommentGap(
                        suffix, tokens, closeIdx + 1, effectiveLineEndIndex(tokens, closeIdx)
                    );
                    final int candidateLen = expandedIndentWidth(
                        baseIndent, indentWidth
                    ) + prefix.length() + candidate.length() + suffix.length();
                    if(candidateLen <= lineLengthLimit) return candidate;
                } // if
            } // if
            // renderCallPreserveGroups/renderDeclarationPreserveGroups re-split each original
            // source line independently, resetting paren-depth tracking to 0 per line -- correct
            // only when paramsSlice actually holds multiple top-level (sibling) arguments. A single
            // argument that itself spans multiple lines (e.g. a lone nested call whose own
            // argument list wraps) has zero top-level commas in the full slice; splitting it
            // per-line would misdetect a nested comma sitting at a line break as a local top-level
            // split point and then append a synthetic comma on top of the one already present,
            // duplicating it. Leave such single-argument candidates untouched (Option 0).
            final List<List<Token>> topLevelArgs = splitTopLevelCommas(paramsSlice);
            // Narrowed 2026-08-15 (RDD_KEY_293): the blanket bail above only needs to protect
            // against re-splitting a single argument whose OWN content genuinely spans multiple
            // physical lines (the real misdetection risk the comment describes). When the sole
            // argument's full text sits on exactly one physical line -- the newline(s) present in
            // `paramsSlice` are only the call's own leading/trailing wrap newlines around it, not
            // an internal one -- `groupByOriginalLine` (used by `renderCallPreserveGroups` below)
            // produces exactly one row with no comma-split risk at all, so it's safe to re-derive.
            // Without this narrowing, an already-wrapped single-argument call was frozen at
            // whatever indent it happened to have forever, even after a later pass (e.g.
            // `SwitchRule.formatNonInlineSwitches`'s case-body reindent, which runs between this
            // method's first and second calls in `FormatterCurly.format`) shifted the call's own
            // opening line's indent without touching its continuation/closing lines -- stale
            // indent baked into round1's output, only self-correcting on round2 once
            // `applyDeclarationsPass` re-flattened the (paren-only, no brace) initializer back to
            // one line first. Found via `openrewrite/rewrite`'s
            // `TabsAndIndentsVisitor.java`/`YamlParser.java` idempotency re-verification.
            if( topLevelArgs.size() <= 1 ) {
                // Kotlin/JS/TS keep the original blanket bail unconditionally -- the narrowing
                // below is verified only for C/C++/Java (see this block's own doc comment); widening
                // it to Kotlin/JS/TS regressed `real_code_regressions_43.kt` (a genuine 2-arg
                // candidate elsewhere in the same pipeline run whose own rendering shape depends on
                // this bail's exact scope) and `curly_gdr_multipass_oneliner.js` (interacts badly
                // with the `curly-general-scope-reindent` pre-pass architecture, out of scope here
                // per STATE_C_CPP_JAVA.md's Open Questions). Left untouched, same as before.
                if(lang.isKotlin || lang.isJs || lang.isTs) return null;
                if( topLevelArgs.isEmpty() || containsInternalNewline(
                    topLevelArgs.get(0)
                ) ) return null;
            } // if
            // Same "leave untouched" posture as the single-argument case above, extended to a
            // multi-argument call where one of the *siblings* is itself a multi-line brace body
            // (e.g. a trailing/leading lambda or anonymous-class argument, `Thread({
            // ...multi-line... }, "name")` / `new Timer(0, new ActionListener() {
            // ...multi-statement... })`). renderCallPreserveGroups (via groupByOriginalLine)
            // groups by *original source line* using only paren/bracket/angle-bracket depth, not
            // brace depth -- it never opens a new row once a part has accumulated significant
            // content, so every line inside such a brace body (having no top-level comma of its
            // own to split on) gets silently swallowed into the *same* row as whatever preceded
            // it, then that whole multi-statement row is rendered via a single
            // `collapseTokensToOneLine` call with no line-length check at all. For Kotlin this was
            // already known to be actively invalid (no `;` to disambiguate the merged
            // statements); found via real-code testing (local `anemonesoft` candidate,
            // `HelpBox._jumpToTarget`/`Spreadsheet`'s `del_cell` action) that C/C++/Java are not
            // actually safe either -- merging silently produces a single, unboundedly long output
            // line (STYLE.md's line-length limit is never applied to it), which is unstable across
            // reformats once a later pass (e.g. Java's Allman brace pass) reacts differently to
            // the now-multi-line body on a second pass than the still-one-physical-line original
            // saw on the first. `real_code_regressions_1` fixture's `combine(..., { ret,
            // level1(ret) })` call is unaffected by widening this bail -- that brace argument sits
            // wholly on one original physical line, so `containsNewline(arg)` is false for it.
            // `topLevelArgs` above (from `splitTopLevelCommas`, which tracks paren/bracket/angle
            // depth only, not brace depth) is unsafe for this specific bail check when a
            // named-argument lambda/block body itself contains a depth-0-looking comma -- e.g.
            // Kotlin's `onError = { source, target, exception -> stmt1() stmt2() stmt3() }`: the
            // lambda parameter list's own commas (`source, target, exception`) aren't wrapped in
            // any paren/bracket, so `splitTopLevelCommas` wrongly treats them as top-level argument
            // separators, shattering this one multi-statement brace-bodied argument into several
            // unrelated parts -- none of which individually still looks like "a brace-bodied
            // argument with an internal newline" to this loop, so the bail it exists to provide
            // never fires and the multi-statement body falls through to `renderCallPreserveGroups`,
            // which (per this same comment's paragraph above) fuses the separate statements onto
            // one line with no separator -- invalid Kotlin (found via JetBrains/kotlin dogfood
            // testing, `PathRecursiveFunctionsTest.kt`'s `onError = {...}` named-argument lambda
            // nested inside an already-wrapped `copyToRecursively(...)` call). Recomputed here with
            // a brace-depth-aware split (`splitTopLevelCommasBraceAware`), used only for this bail
            // decision -- never for the `topLevelArgs.size() <= 1` check above or any rendering
            // path below, so behavior for every already-passing shape (including this exact
            // `real_code_regressions_1` C++ case) is unchanged.
            for( final List<Token> arg : splitTopLevelCommasBraceAware(paramsSlice) ) {
                if( containsInternalNewline(arg) && containsBrace(arg) ) return null;
            }
            final List<String> lines = (sigForRender != null) ? renderDeclarationPreserveGroups(
                paramsSlice, baseIndent
            ) : renderCallPreserveGroups(
                paramsSlice, baseIndent
            );
            return lines == null ? null : "(\n" + String.join("\n", lines);
        } // if

        // `effectiveLineEndIndex` extends through any trailing same-line `//`/`/* */` comment, so
        // this measurement is a "code + comment" width whenever such a comment is actually
        // present -- use `lineLengthWithCommentLimit` (the `line-length-with-comment` config key)
        // only in that case; otherwise this is a plain code-only line and must keep using
        // `lineLengthLimit`, exactly as before `line-length-with-comment` was introduced. Using the
        // comment-aware limit unconditionally regressed ~30 fixtures with no trailing comment at
        // all (e.g. `real_code_regressions_3`/`_7`/`_16`), because it silently raised the effective
        // wrap threshold for every call, not just comment-trailing ones.
        final int    lineEndIdx     = effectiveLineEndIndex(tokens, closeIdx);
        final int    effectiveLimit = hasCommentBetween(
            tokens, closeIdx, lineEndIdx
        ) ? lineLengthWithCommentLimit : lineLengthLimit;
        final String wholeLineRest  = collapseToOneLine(
            tokens, lineStartIndex(tokens, nameIdx), lineEndIdx - 1
        );
        if( expandedIndentWidth(
            baseIndent, indentWidth
        ) + wholeLineRest.length() <= effectiveLimit ) return null; // Option 0 -- already fits, no change

        final List<String> dropped = (sigForRender != null) ? renderDropped(
            sigForRender, baseIndent
        ) : renderCallDropped(
            paramsSlice, baseIndent
        );
        final List<String> lines   = (dropped != null) ? dropped : (sigForRender != null) ? renderOnePerLine(
            sigForRender, baseIndent
        ) : renderCallOnePerLine(
            paramsSlice, baseIndent
        );

        return "(\n" + String.join("\n", lines);
    }
    /**
     * Option 1 (dropped form) for a forward declaration: every param on one line, indented one
     * level under {@code baseIndent}, with `)` on its own line back at {@code baseIndent} --
     * mirrors {@link #renderParamsInline}'s join. Returns {@code null} (caller falls back to
     * {@link #renderOnePerLine}, Option 3) if even this single params line exceeds
     * {@link #lineLengthLimit}.
     */
    private List<String> renderDropped(final Signature sig, final String baseIndent)
    {
        final String paramsLine = baseIndent + indentUnit + renderParamsInline(sig);
        if( paramsLine.length() > lineLengthLimit ) return null;

        return Arrays.asList(paramsLine, baseIndent + ")");
    }
    /**
     * Option 3 (one-per-line) for a forward declaration -- same type-column-padded shape as
     * {@link #render}'s broken form, parameterized by raw {@code baseIndent} text instead of an
     * integer level (this pass has no tracked recursion depth, see
     * {@link #indentUnit}'s doc comment), so this is a deliberate sibling rather than a
     * direct reuse of {@link #render}
     */
    private List<String> renderOnePerLine(final Signature sig, final String baseIndent)
    {
        int maxTypeLen = 0;
        for(final Param p : sig.params) {
            if(p.leadingComment == null) maxTypeLen = Math.max(
                maxTypeLen, renderTokens(p.typeTokens).length()
            );
        }
        final int    typeColWidth    = maxTypeLen + 1;
        final String paramIndent     = baseIndent + indentUnit;
              int    maxNameCommaLen = 0;
        for( int i = 0; i < sig.params.size(); ++i ) {
            final Param  p             = sig.params.get(i);
            final String nameCommaText = p.name.text + renderTokens(
                p.sizeTokens
            ) + ( i < sig.params.size() - 1 ? "," : "" );
            maxNameCommaLen = Math.max( maxNameCommaLen, nameCommaText.length() );
        } // for

        final List<String> lines = new ArrayList<>();
        for( int i = 0; i < sig.params.size(); ++i ) {
            final Param  p             = sig.params.get(i);
            final String typeText      = renderTokens(p.typeTokens);
            final String nameCommaText = p.name.text + renderTokens(
                p.sizeTokens
            ) + ( i < sig.params.size() - 1 ? "," : "" );
            final String nameText      = p.comment != null ? padRight(
                nameCommaText, maxNameCommaLen
            ) + " " + p.comment.text : nameCommaText;
            // Same `//` line-comment-can't-share-a-line fix as `render`'s identical loop above --
            // see that method's doc comment for the full root-cause narrative
            final boolean leadingIsLineComment = p.leadingComment != null && p.leadingComment.type == TokenType.COMMENT_LINE;
            if(leadingIsLineComment) lines.add(paramIndent + p.leadingComment.text);
            final String leadPrefix = (p.leadingComment != null && !leadingIsLineComment) ? p.leadingComment.text + " " : "";
            // Same guaranteed-minimum-space fix as `render`'s identical loop above -- see that
            // method's doc comment for the full root-cause narrative
            final String typeCell = (p.leadingComment != null && !leadingIsLineComment) ? typeText + " " : padRight(
                typeText, Math.max( typeColWidth, typeText.length() + 1 )
            );
            lines.add(paramIndent + leadPrefix + typeCell + nameText);
        } // for
        lines.add(baseIndent + ")");

        return lines;
    }
    /**
     * Option 1 (dropped form) for a plain call: every argument on one line, untyped, split on
     * top-level commas (raw -- {@link #splitTopLevelCommas} tracks paren/bracket/angle depth
     * itself, so it doesn't need {@link #significantOnly} first) and each argument rendered via
     * {@link #collapseTokensToOneLine} rather than {@link #renderTokens} (see that method's doc
     * comment for why -- a nested call inside an argument must not get its own parens spread
     * apart); the top-level `,` separators between sibling arguments are inserted explicitly here
     * instead, normalized to `", "` regardless of original spacing. Returns {@code null} (caller
     * falls back to {@link #renderCallOnePerLine}) if the line doesn't fit.
     */
    private List<String> renderCallDropped(final List<Token> paramsSlice, final String baseIndent)
    {
        // Kotlin only (STYLE_KOTLIN.md §7.2): a trailing comma must be preserved exactly as
        // written, never added or stripped -- checked against the raw slice before the
        // dangling-empty-group drop below discards the signal.
        final boolean           keepTrailingComma = lang.isKotlin && hasTrailingComma(paramsSlice);
        final List<List<Token>> args              = splitTopLevelCommas(paramsSlice);
        // Drop a dangling trailing empty group (a trailing comma before `)` with nothing after
        // it -- e.g. this codebase's own multi-line call style, `foo(\n  a,\n  b,\n);` --
        // splitTopLevelCommas (unlike groupByOriginalLine) doesn't drop it itself. Without this,
        // a source call with a trailing comma measured 2 chars ("`, `") wider here than the same
        // call once reformatted (renderCallOnePerLine/renderCallPreserveGroups never emit a
        // trailing comma on the last argument), so this fits-check could reject on a fresh format
        // but accept on a reformat of already-formatted output at the exact same width --
        // non-idempotent (found via angular/angular real-code testing,
        // `packages/router/src/create_router_state.ts`'s `createNode(...)` call).
        while( !args.isEmpty() && significantOnly(
            args.get( args.size() - 1 )
        ).isEmpty() ) args.remove(
            args.size() - 1
        );
        final StringBuilder argsText = new StringBuilder();
        for( int i = 0; i < args.size(); ++i ) {
            if(i > 0) argsText.append(", ");
            argsText.append( collapseTokensToOneLine( args.get(i) ) );
        }
        if(keepTrailingComma) argsText.append(",");
        final String paramsLine = baseIndent + indentUnit + argsText;
        if( paramsLine.length() > lineLengthLimit ) return null;

        return Arrays.asList(paramsLine, baseIndent + ")");
    }
    /**
     * Option 3 (one-per-line) for a plain call: each top-level argument on its own line, no
     * column alignment (untyped arguments have no type column to align) -- only
     * {@link #splitTopLevelCommas} is needed, not the typed {@link #parseParam} machinery; each
     * argument rendered via {@link #collapseTokensToOneLine}, same nested-call-safety reason as
     * {@link #renderCallDropped}
     */
    private List<String> renderCallOnePerLine(
        final List<Token> paramsSlice,
        final String      baseIndent
    )
    {
        // Kotlin only (STYLE_KOTLIN.md §7.2): preserve a source trailing comma exactly as
        // written -- see renderCallDropped's identical check for the full narrative.
        final boolean           keepTrailingComma = lang.isKotlin && hasTrailingComma(paramsSlice);
        final List<List<Token>> args              = splitTopLevelCommas(paramsSlice);
        // Same dangling-trailing-empty-group drop as renderCallDropped -- without it, a source
        // call with a trailing comma renders a stray blank final line here (an empty last group
        // from splitTopLevelCommas)
        while( !args.isEmpty() && significantOnly(
            args.get( args.size() - 1 )
        ).isEmpty() ) args.remove(
            args.size() - 1
        );
        final String argIndent = baseIndent + indentUnit;

        final List<String> lines = new ArrayList<>();
        for( int i = 0; i < args.size(); ++i ) lines.add(
            argIndent + collapseTokensToOneLine( args.get(i) )
                + ( i < args.size() - 1 || keepTrailingComma ? "," : "" )
        );
        lines.add(baseIndent + ")");

        return lines;
    }
    /**
     * Option 2 (preserve groups) for a plain call: keeps the source's existing line breaks
     * exactly (one output line per original physical line that had any significant content) --
     * a line that turns out to have nothing significant on it (a stray blank line) is dropped
     * entirely -- no STYLE.md worked example sanctions preserving a blank line inside an argument
     * list. Within each line, arguments are split on top-level commas and each rendered via
     * {@link #collapseTokensToOneLine} (not {@link #renderTokens} -- same nested-call-safety
     * reason as {@link #renderCallDropped}); sibling arguments on the same line are joined with an
     * explicit normalized `", "`, and a line's last argument gets a trailing `,` unless it is the
     * very last argument overall (mirrors {@link #renderDeclarationPreserveGroups}'s
     * {@code isLastOverall} logic, so a multi-arg trailing line like `c, d,` / `e` renders with the
     * comma exactly where the original grouping implies more follows). Every preserved line is
     * re-indented to one level under {@code baseIndent} -- "preserve groups" means preserve which
     * arguments share a line, not preserve arbitrary original indentation depth, same distinction
     * {@link #renderDeclarationPreserveGroups} makes.
     */
    private List<String> renderCallPreserveGroups(
        final List<Token> paramsSlice,
        final String      baseIndent
    )
    {
        // Kotlin only (STYLE_KOTLIN.md §7.2): preserve a source trailing comma exactly as
        // written -- see renderCallDropped's identical check for the full narrative.
        final boolean                 keepTrailingComma = lang.isKotlin && hasTrailingComma(
            paramsSlice
        );
        final List<List<List<Token>>> rows              = groupByOriginalLine(paramsSlice);
        if( rows.isEmpty() ) return null; // Shouldn't happen -- caller only calls this when a newline was found -- bail safe

        final String       argIndent = baseIndent + indentUnit;
        final List<String> lines     = new ArrayList<>();
        for( int r = 0; r < rows.size(); ++r ) {
            final List<List<Token>> row  = rows.get(r);
            final StringBuilder     line = new StringBuilder(argIndent);
            for( int c = 0; c < row.size(); ++c ) {
                if(c > 0) line.append(", ");
                line.append( collapseTokensToOneLine( row.get(c) ) );
                final boolean isVeryLast = r == rows.size() - 1 && c == row.size() - 1;
                if( c == row.size() - 1 && (!isVeryLast || keepTrailingComma) ) line.append(",");
            } // for c
            lines.add( line.toString() );
        } // for r
        lines.add(baseIndent + ")");

        return lines;
    }
    /**
     * Option 2 (preserve groups) for a forward declaration: same per-original-line grouping as
     * {@link #renderCallPreserveGroups}, but each line's params are parsed (typed) via
     * {@link #parseParam} and laid out in a {@link ColumnGrid} with two columns per parameter
     * slot position (type, name) -- slot N's type/name cells align vertically across every line
     * that has an Nth parameter, matching STYLE_NEXT_EXT.md's worked example, reusing
     * `DeclarationAlignmentRule`'s "plain `ColumnGrid` + one join space" convention (not
     * {@link #render}'s own `maxTypeLen + 1` convention, which RDD_EXT_4 ties specifically to §5's
     * grid, not §8's signature padding). No comment column is needed here: a candidate with any
     * comment between its parens is already rejected entirely by {@link #enforceCallLineBreaking}
     * before this method is ever called, so STYLE.md §8's per-line trailing-comment rule for this
     * option is a documented gap, not implemented (see RDD_KEY_5). Returns {@code null} -- leaving
     * the whole candidate untouched -- if any line's params don't reduce to {@link Param}'s typed
     * shape (the same "bail the whole signature" posture {@link #parseSignature} itself uses).
     */
    private List<String> renderDeclarationPreserveGroups(
        final List<Token> paramsSlice,
        final String      baseIndent
    )
    {
        final List<List<Param>> rows = new ArrayList<>();
        for( final List<List<Token>> lineParts : groupByOriginalLine(paramsSlice) ) {
            final List<Param> row = new ArrayList<>();
            for(final List<Token> part : lineParts) {
                final Param p = parseParam( significantOnly(part) );
                if(p == null) return null;
                row.add(p);
            }
            if( !row.isEmpty() ) rows.add(row);
        } // for lineParts
        if( rows.isEmpty() ) return null; // Shouldn't happen -- caller only calls this when a newline was found -- bail safe

        final ColumnGrid grid = new ColumnGrid();
        for( int r = 0; r < rows.size(); ++r ) {
            final List<Param> row   = rows.get(r);
            final String[]    cells = new String[ row.size()* 2 ];
            for( int c = 0; c < row.size(); ++c ) {
                final Param   p             = row.get(c);
                final boolean isLastOverall = ( r == rows.size() - 1 ) && ( c == row.size() - 1 );
                cells[c* 2]     = renderTokens(p.typeTokens);
                cells[c* 2 + 1] = p.name.text + renderTokens(
                    p.sizeTokens
                ) + (isLastOverall ? "" : ",");
            } // for c
            grid.addRow(cells);
        } // for r

        final String       argIndent = baseIndent + indentUnit;
        final List<String> lines     = new ArrayList<>();
        for( final String[] row : grid.flush() ) lines.add( argIndent + String.join(" ", row) );
        lines.add(baseIndent + ")");

        return lines;
    }
    /**
     * True iff any token in {@code [fromExclusive, toExclusive)} is a {@code NEWLINE} -- the
     * multi-line-source detection signal for {@link #renderCallCandidate}'s Option 2 branch
     */
    private boolean containsNewline(final List<Token> tokens)
    {
        return valueSpansMultipleLines(tokens);
    }
    /**
     * Same signal as {@link #containsNewline}, but only counts a {@code NEWLINE} strictly
     * between this argument's own first and last significant tokens -- i.e. a genuinely
     * multi-line argument body, not merely a formatting newline that happened to land in this
     * argument's *leading* gap because {@link #splitTopLevelCommas} hands the separator's
     * trailing whitespace/newline to the next argument. Used by {@link #renderCallCandidate}'s
     * per-topLevelArg multi-line-brace-body bail so a short, single-physical-line trailing
     * argument (e.g. `real_code_regressions_1`'s `{ ret, level1(ret) }`) that merely starts on
     * its own source line right after a comma doesn't get misclassified as "itself spans
     * multiple lines" and wrongly bail the whole candidate untouched.
     */
    private boolean containsInternalNewline(final List<Token> tokens)
    {
        int first = -1;
        int last  = -1;
        for( int i = 0; i < tokens.size(); ++i ) {
            if( !isGapToken( tokens.get(i) ) ) {
                if(first < 0) first = i;
                last = i;
            }
        }
        if(first < 0 || first == last) return false;
        for(int i = first + 1; i < last; ++i) {
            if( tokens.get(i).type == TokenType.NEWLINE ) return true;
        }

        return false;
    }
    /**
     * True iff any token in {@code tokens} is a {@code {} -- the "this argument is itself a
     * brace-bodied block, not a plain expression" detection signal used alongside
     * {@link #containsNewline} by {@link #renderCallCandidate}'s Option 2 branch
     */
    private boolean containsBrace(final List<Token> tokens)
    {
        for(final Token t : tokens) {
            if( isPunct(t, "{") ) return true;
        }

        return false;
    }
    /**
     * True iff any token in {@code (fromExclusive, toExclusive)} is a comment -- same "comment in
     * the gap blocks the rewrite" signal as `CppSpecificRule.hasCommentBetween`, duplicated here
     * per this file's established no-shared-helpers-across-rule-classes precedent (see
     * {@link #renderTokens}'s doc comment).
     */
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
     * Appends {@code tokens[fromInclusive, toExclusive)}'s own text verbatim -- same precedent as
     * `CppSpecificRule.appendRange`, duplicated here (see {@link #hasCommentBetween}'s doc
     * comment).
     */
    private void appendRange(
        final StringBuilder out,
        final List<Token>   tokens,
        final int           fromInclusive,
        final int           toExclusive
    )
    {
        for(int i = fromInclusive; i < toExclusive; ++i) out.append( tokens.get(i).text );
    }
    /**
     * Same as {@link #appendRange}, except a whitespace run immediately preceding a trailing
     * line comment is collapsed to a single space for measurement purposes. That gap is
     * comment-column alignment padding, whose width depends on the physical layout of sibling
     * statements (e.g. a declaration-alignment grid) and can differ between a fresh single-line
     * call and a reformat of that same call's own already-wrapped output -- counting it verbatim
     * made this fits-check flip inconsistently between the two (found via angular/angular
     * real-code testing, `location_shim.ts`'s `composeUrls`: `this.$$absUrl = ...slice(1); //
     * comment`, non-idempotent without this). Used only for measurement, never rendered into
     * actual output.
     */
    private void appendRangeCollapsingTrailingCommentGap(
        final StringBuilder out,
        final List<Token>   tokens,
        final int           fromInclusive,
        final int           toExclusive
    )
    {
        int i = fromInclusive;
        while(i < toExclusive) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE) {
                int j = i;
                while( j < toExclusive && tokens.get(j).type == TokenType.WHITESPACE ) j++;
                if( j < toExclusive && tokens.get(j).type == TokenType.COMMENT_LINE ) {
                    out.append(' ');
                    i = j;
                    continue;
                }
            } // if
            out.append(t.text);
            ++i;
        } // while
    }
    /**
     * Renders {@code tokens[fromInclusive, toInclusive]} verbatim except every whitespace/newline
     * run that actually *contains* a newline collapses to exactly one space (matching what a real
     * multi-line-to-single-line join would render there) -- same precedent as
     * `CppSpecificRule.collapseToOneLine`, duplicated here (see {@link #hasCommentBetween}'s doc
     * comment); used only to *measure* a would-be single-line rendering against
     * {@link #lineLengthLimit}, never committed to output as-is. A run of pure horizontal
     * whitespace (no {@code NEWLINE} token in it) is preserved verbatim instead of being flattened
     * to one space: it's already-real same-line spacing an earlier pass deliberately produced --
     * most commonly a declaration-alignment grid's `=`-column padding (`const res    = ...`) -- and
     * flattening it here would undercount the candidate's true rendered width by the padding
     * amount, letting a call that doesn't actually fit (once its own line's real padding is
     * accounted for) wrongly pass the fits-check and collapse to one line anyway (found via
     * vuejs/core real-code testing, `scripts/release.js`'s `const res = await fetch(...)` sitting
     * in an alignment group with a longer-named sibling declaration).
     */
    private String collapseToOneLine(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toInclusive
    )
    {
        final StringBuilder sb              = new StringBuilder();
        final List<Token>   gap             = new ArrayList<>();
              Token         prevSignificant = null;
        for(int i = fromInclusive; i <= toInclusive; ++i) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                gap.add(t);
                continue;
            }
            flushCollapseGap(sb, gap, prevSignificant, t);
            gap.clear();
            sb.append(t.text);
            prevSignificant = t;
        } // for
        flushCollapseGap(sb, gap, prevSignificant, null);

        return sb.toString().trim();
    }
    /**
     * Helper for {@link #collapseToOneLine}: appends a gap of consecutive WHITESPACE/NEWLINE
     * tokens either verbatim (pure horizontal whitespace, no NEWLINE in it) or as a single space
     * (a NEWLINE present, same "joined multi-line run becomes one space" rule as before) --
     * unless the gap sits at a tight `.`/`->` member-access join (either side), in which case no
     * space is inserted regardless of how the original line broke. Without this, a wrapped
     * member-access expression whose line happened to break right at the `.`/`->` (e.g. C++'s
     * `_Other.\n_Owns`, found via microsoft/STL real-code testing on `unique_lock`'s copy
     * constructor once its enclosing initializer-list line was long enough to wrap and then
     * collapse back) corrupted the expression into `_Other. _Owns` on the round that re-collapsed
     * it -- same tight-join corruption already fixed once in {@link #collapseTokensToOneLine} for
     * JS/TS's `.`/`?.` (nestjs/nest real-code testing), just never mirrored here for C++'s
     * `.`/`->` case since collapseToOneLine has its own independent implementation.
     */
    private void flushCollapseGap(
        final StringBuilder sb,
        final List<Token>   gap,
        final Token         prevSignificant,
        final Token         nextSignificant
    )
    {
        if( gap.isEmpty() ) return;
        boolean hasNewline = false;
        for(final Token g : gap) {
            if(g.type == TokenType.NEWLINE) {
                hasNewline = true;
                break;
            }
        }
        if(hasNewline) {
            final boolean tightJoin = ( prevSignificant != null && ( isOp(
                prevSignificant, "."
            ) || isOp(
                prevSignificant, "->"
            ) ) ) || ( nextSignificant != null && ( isOp(
                nextSignificant, "."
            ) || isOp(
                nextSignificant, "->"
            ) ) );
            if( !tightJoin && sb.length() > 0 && sb.charAt(
                sb.length() - 1
            ) != ' ' ) sb.append(
                ' '
            );
        } // if
        else {
            for(final Token g : gap) sb.append(g.text);
        }
    }
    /**
     * Same whitespace/newline-run-collapsing as {@link #collapseToOneLine}, but over a detached
     * sublist (as returned by {@link #splitTopLevelCommas}/{@link #groupByOriginalLine}) rather than
     * an index range into the original token list. Used to render one call argument's own tokens
     * -- deliberately does *not* route through {@link #renderTokens}, since an argument may itself
     * contain a nested call/parenthesized sub-expression (`bar(1, 2)`); `renderTokens`'s
     * tight-attachment rules don't know `(`/`)` should stay tight to a preceding identifier (those
     * rules were written for type-token lists, which never contain a literal call), so routing an
     * arbitrary expression through it would spread `bar ( 1, 2 )` apart. Collapsing only existing
     * whitespace runs -- never inserting a space where the source had none -- reproduces the
     * nested expression's own spacing as-is instead of guessing at it, consistent with this pass's
     * "claim and skip" rule for nested candidates (see {@link #enforceCallLineBreaking}'s doc
     * comment): a nested call's interior is never independently analyzed, so it rides along
     * unprocessed rather than being normalized.
     */
    private String collapseTokensToOneLine(final List<Token> tokens)
    {
        final StringBuilder sb              = new StringBuilder();
              Token         prevSignificant = null;
              boolean       pendingSpace    = false;
        for(final Token t : tokens) {
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                pendingSpace = true;
                continue;
            }
            // A wrapped member-access/optional-chaining `.`/`?.` is tight against both the
            // expression before it and the property/method name after it -- when the original
            // source happened to break its line right at that `.` (e.g. `options.\n
            // provideInjectionTokensFrom`), collapsing the run unconditionally into a literal
            // single space corrupted the expression into `options. provideInjectionTokensFrom`
            // (found via nestjs/nest real-code testing). Suppress the space on either side of a
            // tight `.`/`?.` join, regardless of how much original whitespace/newline separated
            // them.
            if( pendingSpace && sb.length() > 0 && !( prevSignificant != null && ( isOp(
                prevSignificant, "."
            ) || isOp(
                prevSignificant, "?."
            ) ) ) && !( isOp(
                t, "."
            ) || isOp(
                t, "?."
            ) ) ) sb.append(
                ' '
            );
            pendingSpace = false;
            sb.append(t.text);
            prevSignificant = t;
        } // for

        return sb.toString().trim();
    }
    /**
     * The index of the first significant token on the physical line containing {@code idx} --
     * same precedent as `CppSpecificRule.lineStartIndex`, duplicated here (see
     * {@link #hasCommentBetween}'s doc comment), adapted to this class's own
     * {@link #nextSignificantIndex} (inclusive-of-{@code from} semantics, unlike
     * `CppSpecificRule`'s exclusive one -- hence the {@code + 1} below).
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
        final int firstSig = nextSignificantIndex(tokens, newlineIdx + 1);

        return firstSig < 0 ? idx : firstSig;
    }
    /**
     * Kotlin-only lookahead used by {@link #enforceCallLineBreaking}'s true-signature exemption:
     * starting at a top-level `:` immediately after a candidate's own `)`, scans forward
     * (depth-aware over `(`/`[`/`{`/`<`) through the return-type tokens to determine whether the
     * signature's tail is a real `{`-bodied block (returns {@code true}) rather than an `=`-led
     * expression body (returns {@code false}) -- a top-level `=` encountered before any top-level
     * `{` means this is NOT the block-body shape. Stops at end of file or the first NEWLINE run
     * that isn't inside an open group (a return type is never itself broken across a blank
     * top-level line).
     */
    private boolean isKotlinReturnTypeThenBlockBody(final List<Token> tokens, final int colonIdx)
    {
        int depth = 0;
        for( int i = colonIdx + 1; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( isPunct(
                t, "("
            ) || isPunct(
                t, "["
            ) || isPunct(
                t, "{"
            ) || t.type == TokenType.ANGLE_BRACKET_OPEN ) {
                if( depth == 0 && isPunct(
                    t, "{"
                ) ) return true; // Top-level `{` reached with no top-level `=` first -- block body
                ++depth;
            }
            else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")
                    || t.type == TokenType.ANGLE_BRACKET_CLOSE ) {
                --depth;
            }
            else if( depth == 0 && isOp(t, "=") ) {
                return false; // `=`-led expression-bodied tail -- not this shape
            }
        } // for

        return false;
    }
    private int effectiveLineEndIndex(final List<Token> tokens, final int idx)
    {
        int i     = idx;
        int depth = 0;
        while( i < tokens.size() ) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.NEWLINE) {
                if(depth <= 0) return i;
            }
            else if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) {
                ++depth;
            }
            else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) {
                --depth;
            }
            ++i;
        } // while

        return i;
    }
    /**
     * Line-leading whitespace of the physical line containing token {@code idx} -- "" if that
     * line has no leading whitespace (column-0 start) -- same precedent as
     * `CppSpecificRule.lineIndent`/`JavaSpecificRule.lineIndent`, duplicated here (see
     * {@link #hasCommentBetween}'s doc comment). Unlike {@link #indentBefore}, this works
     * correctly even when {@code idx} itself is not the first significant token on its line (e.g.
     * the call name in {@code auto x = foo(a, b, c);}), which {@link #enforceCallLineBreaking}'s
     * candidates frequently are not.
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
     * {@link #lineIndent}, except for a Kotlin call candidate whose own physical line will be
     * MERGED onto the line above it later in the same pipeline by {@code
     * KotlinSpecificRule.formatWhenExpressions}' arrow-alignment pass (Phase 4, run well after
     * this pass, Phase 1 -- see {@code Formatter.formatOne}'s phase ordering). That happens
     * whenever a `when` branch's keyword-less body starts its own line (`label -> \n    body`,
     * the body not itself `{`-bodied): `formatWhenExpressions` unconditionally collapses label,
     * arrow, and body onto one line (RDD_KEY_101/§4), no line-length gate. A candidate whose
     * first token is exactly such a body-start reads its own (deeper, branch-body) physical-line
     * indent here, one phase too early -- correct on a fresh format's OWN first pass in isolation,
     * but stale by one level the moment the merge actually happens, so the call's already-baked
     * continuation-line/closing-paren indent visually sits one level deeper than the arrow line
     * it now shares. Reformatting that output (round 2) starts from the already-merged line and
     * gets it right, hence a round1-vs-round2 flap (same "physical-line-anchored decision
     * invalidated by a later merge" root cause as RDD_KEY_136/152/158/159, this time inside
     * {@code MiscRule} rather than {@code ScopePipeline}). Detected the same way {@code
     * ScopePipeline.findMergingWhenBranchLineStart} detects its own nested-`when` variant: the
     * line immediately before {@code nameIdx}'s own line ends (modulo whitespace) with a
     * top-level `->`. Not itself proof the `->` belongs to a genuine `when` branch (a lambda
     * arrow could in principle match too), but a lambda's `->` is essentially never followed by
     * its whole body starting on the very next line with nothing else on the arrow's own line --
     * and {@code formatWhenExpressions} only runs for Kotlin, so this is gated to Kotlin only.
     */
    private String effectiveCallBaseIndent(final List<Token> tokens, final int nameIdx)
    {
        final String ownIndent = lineIndent(tokens, nameIdx);
        if(!lang.isKotlin) return ownIndent;
        // `lineStart` need not be `nameIdx` itself (e.g. `throw IllegalStateException(` -- `throw`
        // leads); the merge-detection below only needs to know THIS line is the branch body's own
        // line, which `lineStart` already tells us regardless of who leads it.
        final int lineStart = lineStartIndex(tokens, nameIdx);
              int p         = lineStart - 1;
        while( p >= 0 && tokens.get(p).type == TokenType.WHITESPACE ) p--;
        if( p < 0 || tokens.get(p).type != TokenType.NEWLINE ) return ownIndent;
        int q = p - 1;
        while( q >= 0 && tokens.get(q).type == TokenType.WHITESPACE ) q--;
        if( q < 0 || !Token.isOp( tokens.get(q), "->" ) ) return ownIndent;

        return lineIndent(tokens, q);
    }

} // class MiscRuleCurly
