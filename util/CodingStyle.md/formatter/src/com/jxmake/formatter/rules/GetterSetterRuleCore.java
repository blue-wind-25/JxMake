/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

/**
 * Family-agnostic base for {@link GetterSetterRuleCurly} -- everything in this file used to live
 * directly in {@code GetterSetterRule} before the curly/indent class-refactor. No behavior
 * change, mechanical move only (see STATE_COMMON.md's Class Refactor section). An optional
 * {@code GetterSetterRuleIndent} sibling was scaffolded for Python3's possible reuse but was never
 * adopted (Python3 went bespoke instead) and was removed as unused during the 2026-07-28 cleanup
 * pass -- see STATE_COMMON.md's "Project refactoring/cleanup pass".
 */
public abstract class GetterSetterRuleCore {

    protected final Lang lang;
    protected final int  indentWidth;
    protected final int  lineLengthLimit;

    protected GetterSetterRuleCore(
        final Lang lang,
        final int  indentWidth,
        final int  lineLengthLimit
    )
    {
        this.lang            = lang;
        this.indentWidth     = indentWidth;
        this.lineLengthLimit = lineLengthLimit;
    }

    /** One parsed one-liner method candidate -- all fields are index ranges into the caller's token list */
    public static final class Member {

        public final List<Token>             modifiers;          // Java only; empty for C/C++
        public final int templatePrefixFrom, templatePrefixTo;   // Equal (empty) unless C++ `template<...>`
        public final int returnTypeFrom,     returnTypeTo;
        public final int                     nameFrom;           // Start of qualified name (= nameIdx for unqualified)
        public final int                     nameIdx;
        public final int paramsFrom,         paramsTo;
        public final int bodyFrom,           bodyTo;             // -1/-1 for declarations
        public final int memberFrom,         memberTo;           // Full original span, for verbatim passthrough
        public final Token                   trailingComment;    // Nullable
        public final boolean                 blankLineBefore;
        public final String                  postParenQualifier; // "" or " const" etc.; never contains "override"
        public final String                  pureSpecifier;      // null / "= 0" / "= delete" / "= default"
        public final boolean                 isDefinition;       // true = { body }, false = ; terminated

        Member(
            final List<Token> modifiers,
            final int         templatePrefixFrom,
            final int         templatePrefixTo,
            final int         returnTypeFrom,
            final int         returnTypeTo,
            final int         nameFrom,
            final int         nameIdx,
            final int         paramsFrom,
            final int         paramsTo,
            final int         bodyFrom,
            final int         bodyTo,
            final int         memberFrom,
            final int         memberTo,
            final Token       trailingComment,
            final boolean     blankLineBefore,
            final String      postParenQualifier,
            final String      pureSpecifier,
            final boolean     isDefinition
        )
        {
            this.modifiers          = modifiers;
            this.templatePrefixFrom = templatePrefixFrom;
            this.templatePrefixTo   = templatePrefixTo;
            this.returnTypeFrom     = returnTypeFrom;
            this.returnTypeTo       = returnTypeTo;
            this.nameFrom           = nameFrom;
            this.nameIdx            = nameIdx;
            this.paramsFrom         = paramsFrom;
            this.paramsTo           = paramsTo;
            this.bodyFrom           = bodyFrom;
            this.bodyTo             = bodyTo;
            this.memberFrom         = memberFrom;
            this.memberTo           = memberTo;
            this.trailingComment    = trailingComment;
            this.blankLineBefore    = blankLineBefore;
            this.postParenQualifier = postParenQualifier;
            this.pureSpecifier      = pureSpecifier;
            this.isDefinition       = isDefinition;
        }

    } // class Member

    protected int bodyWidth(final List<Token> tokens, final Member m)
    {
        return cellText(tokens, m.bodyFrom, m.bodyTo).length();
    }

    protected static String padRight(final String s, final int width)
    {
        final StringBuilder sb = new StringBuilder(s);
        while( sb.length() < width ) sb.append(' ');

        return sb.toString();
    }

    protected String cellText(final List<Token> tokens, final int from, final int to)
    {
        final StringBuilder sb = new StringBuilder();
        for(int i = from; i < to; ++i) sb.append( tokens.get(i).text );

        return sb.toString();
    }

    /**
     * Splits a class/struct/enum body's tokens into top-level members: each spans from the end
     * of the previous member to either a top-level `;` (field, import, etc.) or a complete
     * top-level `{ ... }` block (method/constructor/nested-type body), plus any same-line
     * trailing comment. Uses local relative depth counting on the slice, not the tokenizer's
     * absolute `braceDepth` field -- same precedent as `BlockStructureRule`/`SwitchRule`.
     */
    protected List<int[]> splitMembers(final List<Token> scopeTokens)
    {
        final List<int[]> members  = new ArrayList<>();
        final int         n        = scopeTokens.size();
        final boolean     isJsOrTs = lang.isJs || lang.isTs;
              int         start    = 0;
              int         depth    = 0;
        // JS/TS-only: tracks `(`/`[` nesting separately from `depth` (which only tracks `{`/`}`,
        // unchanged for every other language) so the ASI newline check below never fires inside
        // a parameter list or array/computed-member expression
        int parenDepth = 0;
        int i          = 0;
        int lastSigIdx = -1;

        while(i < n) {
            final Token t = scopeTokens.get(i);
            // JS/TS-only ASI awareness: a class field written without a trailing `;` (legal
            // under JS's semicolon-optional grammar, and JsTsSpecificRule.enforceSemicolonInsertion
            // -- which would normalize this -- runs in Phase 1, AFTER this Phase-0 pass) would
            // otherwise never close its statement here, so it swallows the *next* member's tokens
            // (e.g. the following `static get`/`static set` one-liner) into its own span, corrupting
            // that member's name/return-type parse and desyncing `blankLineBefore` for every member
            // after it -- silently breaking one-liner grouping/column-padding for a sibling pair
            // that has nothing wrong with it (confirmed real bug via `#cache = new Map()` with no
            // trailing `;` ahead of a `static get`/`static set` pair). At depth 0, a NEWLINE right
            // after a token that cannot legally continue an expression onto the next line closes
            // the statement here too, same effect as an inserted `;`.
            if( isJsOrTs && depth == 0 && parenDepth == 0 && t.type == TokenType.NEWLINE
                    && lastSigIdx >= 0 && !isAsiContinuation( scopeTokens.get(lastSigIdx) ) ) {
                ++i;
                final int end = consumeTrailingSameLine(scopeTokens, i);
                members.add( new int[] {start, end} );
                start      = end;
                lastSigIdx = -1;
                continue;
            } // if
            if( isPunct(t, "{") ) {
                ++depth;
                ++i;
                lastSigIdx = i - 1;
                continue;
            }
            if( isPunct(t, "}") ) {
                --depth;
                ++i;
                if(depth == 0) {
                    final int end = consumeTrailingSameLine(scopeTokens, i);
                    members.add( new int[] {start, end} );
                    start      = end;
                    lastSigIdx = -1;
                    continue;
                } // if
                lastSigIdx = i - 1;
                continue;
            } // if
            if( depth == 0 && isPunct(t, ";") ) {
                ++i;
                final int end = consumeTrailingSameLine(scopeTokens, i);
                members.add( new int[] {start, end} );
                start      = end;
                lastSigIdx = -1;
                continue;
            } // if
            if(isJsOrTs) {
                if( t.type == TokenType.PUNCT && ( isPunct(
                    t, "("
                ) || isPunct(
                    t, "["
                ) ) ) parenDepth++;
                else if( t.type == TokenType.PUNCT && ( isPunct(
                    t, ")"
                ) || isPunct(
                    t, "]"
                ) ) ) parenDepth--;
            } // if
            if(t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE && t.type != TokenType.COMMENT_LINE && t.type != TokenType.COMMENT_BLOCK) lastSigIdx = i;
            ++i;
        } // while
        if(start < n) {
            members.add( new int[] {start, n} );
        }

        return members;
    }

    /**
     * True when {@code lastSig} (the last significant token before a candidate depth-0
     *  NEWLINE boundary) means the statement isn't finished yet -- a trailing binary/assignment
     *  operator, comma, or a keyword that can never end a statement on its own. Deliberately a
     *  small, conservative subset of {@code JsTsSpecificRule.CONTINUATION_OPS}/
     *  {@code CONTINUATION_KEYWORDS} (open brackets are handled separately via this method's own
     *  {@code depth} tracking, not needed here) -- this method only needs to avoid mis-splitting
     *  the shapes {@code splitMembers} actually sees (fields/one-liner signatures), not to be a
     *  general-purpose ASI oracle (that's {@code enforceSemicolonInsertion}'s job, in Phase 1).
     */
    private boolean isAsiContinuation(final Token lastSig)
    {
        if(lastSig.type == TokenType.OP) {
            switch(lastSig.text) {
                case "="    : /* FALL-THROUGH */ case "+"    : /* FALL-THROUGH */ case "-"    : /* FALL-THROUGH */ case "*"    : /* FALL-THROUGH */ case "/"    : /* FALL-THROUGH */ case "%"    : /* FALL-THROUGH */ case "&&"   : /* FALL-THROUGH */ case "||"   : /* FALL-THROUGH */
                case "??"   : /* FALL-THROUGH */ case "?"    : /* FALL-THROUGH */ case "."    : /* FALL-THROUGH */ case "=>"   : /* FALL-THROUGH */ case "=="   : /* FALL-THROUGH */ case "==="  : /* FALL-THROUGH */ case "!="   : /* FALL-THROUGH */
                case "!=="  : /* FALL-THROUGH */ case "<"    : /* FALL-THROUGH */ case ">"    : /* FALL-THROUGH */ case "<="   : /* FALL-THROUGH */ case ">="   : /* FALL-THROUGH */ case "+="   : /* FALL-THROUGH */ case "-="   : /* FALL-THROUGH */
                case "*="   : /* FALL-THROUGH */ case "/="   : /* FALL-THROUGH */ case "%="   : /* FALL-THROUGH */ case "&&="  : /* FALL-THROUGH */ case "||="  : /* FALL-THROUGH */ case "??="  : /* FALL-THROUGH */ case "&"    : /* FALL-THROUGH */
                case "|"    : /* FALL-THROUGH */ case "^"    : /* FALL-THROUGH */ case "<<"   : /* FALL-THROUGH */ case ">>"   : /* FALL-THROUGH */ case ">>>"  : /* FALL-THROUGH */ case "..."  : /* FALL-THROUGH */ case "**"   : /* FALL-THROUGH */
                case "**="  : /* FALL-THROUGH */ case "&="   : /* FALL-THROUGH */ case "|="   : /* FALL-THROUGH */ case "^="   : /* FALL-THROUGH */ case "<<="  : /* FALL-THROUGH */ case ">>="  : /* FALL-THROUGH */ case ">>>=" : return true ;
                default     : return false;
            } // switch
        } // if
        if( lastSig.type == TokenType.PUNCT && ",".equals(lastSig.text) ) return true;
        if(lastSig.type == TokenType.KEYWORD) {
            switch(lastSig.text) {
                case "typeof"     : /* FALL-THROUGH */ case "new"        : /* FALL-THROUGH */ case "in"         : /* FALL-THROUGH */ case "instanceof" : /* FALL-THROUGH */ case "else"       : /* FALL-THROUGH */ case "try"        : /* FALL-THROUGH */
                case "finally"    : /* FALL-THROUGH */ case "do"         : /* FALL-THROUGH */ case "case"       : /* FALL-THROUGH */ case "default"    : /* FALL-THROUGH */ case "extends"    : /* FALL-THROUGH */
                case "implements" : /* FALL-THROUGH */ case "delete"     : /* FALL-THROUGH */ case "void"       : /* FALL-THROUGH */ case "of"         : /* FALL-THROUGH */ case "as"         : /* FALL-THROUGH */ case "from"       : return true ;
                default           : return false;
            }
        } // if

        return false;
    }

    protected int consumeTrailingSameLine(final List<Token> tokens, final int from)
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
     * True if {@code [bodyFrom, bodyTo)} contains at least one {@code name(args)} call with a
     *  non-empty argument list -- the shape {@code MiscRule.enforceCallLineBreaking} may later
     *  break across lines if it doesn't fit ({@code name()} zero-arg calls are never broken, see
     *  that method's own doc comment). Visibility raised private -> protected for
     *  {@code KotlinGetterSetterRule} reuse (RDD_KEY_138) -- purely additive, no behavior change.
     */
    protected boolean hasBreakableCall(
        final List<Token> tokens,
        final int         bodyFrom,
        final int         bodyTo
    )
    {
        if(bodyFrom < 0) return false;
        for(int i = bodyFrom; i < bodyTo; ++i) {
            final Token t = tokens.get(i);
            if(t.type != TokenType.IDENTIFIER) continue;
            final int parenIdx = nextSignificant(tokens, i + 1, bodyTo);
            if( parenIdx < 0 || !isPunct( tokens.get(parenIdx), "(" ) ) continue;
            final int closeIdx = matchBracket(tokens, parenIdx, "(", ")");
            if(closeIdx < 0) continue;
            final int argsFrom = nextSignificant(tokens, parenIdx + 1, closeIdx);
            if(argsFrom >= 0) return true;
        } // for

        return false;
    }

    /**
     * First IDENTIFIER in [from, to) whose next significant token is `(`; -1 if none. Also
     * recognizes a C++ operator-overload name (`operator` keyword followed by a single OP
     * token, e.g. `operator=`, `operator==`, `operator<=>`) immediately before `(`, returning
     * the index of the OP token itself -- the caller detects the preceding `operator` keyword
     * separately to extend the name's start back over it.
     */
    protected int findNameBeforeParen(final List<Token> tokens, final int from, final int to)
    {
        int i = from;
        while(i < to) {
            final Token t = tokens.get(i);
            if( !isInsignificant(t) ) {
                if(t.type == TokenType.IDENTIFIER) {
                    final int next = nextSignificant(tokens, i + 1, to);
                    if( next >= 0 && isPunct( tokens.get(next), "(" ) ) return i;
                }
                else if( t.type == TokenType.KEYWORD && "operator".equals(t.text) ) {
                    final int opIdx = nextSignificant(tokens, i + 1, to);
                    if( opIdx >= 0 && tokens.get(opIdx).type == TokenType.OP ) {
                        final int next = nextSignificant(tokens, opIdx + 1, to);
                        if( next >= 0 && isPunct( tokens.get(next), "(" ) ) return opIdx;
                    }
                }
            } // if
            ++i;
        } // while

        return -1;
    }

    protected int matchBracket(
        final List<Token> tokens,
        final int         openIdx,
        final String      open,
        final String      close
    )
    {
          int depth = 1;
          int i     = openIdx + 1;
    final int n     = tokens.size();
        while(i < n && depth > 0) {
            final Token t = tokens.get(i);
                 if( isPunct(t, open) )  depth++;
            else if( isPunct(t, close) ) depth--;
            ++i;
        }

        return depth == 0 ? i - 1 : -1;
    }

    protected boolean isInsignificant(final Token t)
    {
        return t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE
                || t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK;
    }

    protected int firstSignificantIndex(final List<Token> tokens, final int from, final int to)
    {
        for(int i = from; i < to; ++i) {
            if( !isInsignificant( tokens.get(i) ) ) return i;
        }

        return -1;
    }

    protected int nextSignificant(final List<Token> tokens, final int from, final int to)
    {
        return firstSignificantIndex(tokens, from, to);
    }

    protected int trimLeadingWs(final List<Token> tokens, final int from, final int to)
    {
        int start = from;
        while( start < to && isInsignificant( tokens.get(start) ) ) start++;

        return start;
    }

    protected int trimTrailingWs(final List<Token> tokens, final int from, final int to)
    {
        int end = to;
        while( end > from && isInsignificant( tokens.get(end - 1) ) ) end--;

        return end;
    }

    /** True iff any `NEWLINE` token appears in [from, to) -- i.e. the span crosses a source line. */
    protected boolean hasNewlineBetween(final List<Token> tokens, final int from, final int to)
    {
        for(int i = from; i < to; ++i) {
            if( tokens.get(i).type == TokenType.NEWLINE ) return true;
        }

        return false;
    }

    /** Same blank-line-before detection as `DeclarationAlignmentRule.hasBlankLineBefore`. */
    protected boolean hasBlankLineRun(final List<Token> tokens, final int from, final int to)
    {
        int newlineRun = 0;
        for(int i = from; i < to; ++i) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.NEWLINE) {
                ++newlineRun;
                if(newlineRun >= 2) return true;
            }
            else if(t.type == TokenType.WHITESPACE) {
                // Ignore -- doesn't break or extend the newline run
            }
            else if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) {
                newlineRun = 0;
            }
            else {
                break;
            }
        } // for

        return false;
    }

    protected int prevSignificant(final List<Token> tokens, final int from, final int lowerBound)
    {
        for(int i = from; i >= lowerBound; --i) {
            if( !isInsignificant( tokens.get(i) ) ) return i;
        }

        return -1;
    }

} // class GetterSetterRuleCore
