/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

/**
 * Java-specific STYLE_JAVA.md sections not owned by another rule class: §2 (method-definition
 * Allman conversion) and §7 (Import Ordering). (§1/§4/§5/§6/§8 are already fully covered by
 * already-COMPLETE general/shared rule files -- see STATE.md's "`JavaSpecificRule.java` scoping"
 * Resolved Design Decision for the full cross-check; §3 needs zero code in this file either, since
 * it is satisfied by {@code MiscRuleCore.enforceConditionComplexityPadding}, STYLE.md §3.1.)
 */
public class JavaSpecificRule {

    /**
     * The six fixed classification buckets every import is sorted into, per the resolved
     * STYLE_JAVA.md §7 reading (see STATE.md "§7 import group order/count contradiction" --
     * trust the worked example). {@code groupOrder} passed into {@link #enforceImportOrdering}
     * configures only the *emission order* of these always-the-same six buckets, never which
     * buckets exist -- so it must be a permutation of exactly this set.
     */
    private static final Set<String> IMPORT_GROUP_KEYS = new HashSet<>( Arrays.asList(
        "java", "com", "org", "other", "local", "static"
    ) );

    private final int lineLengthLimit;
    // "code + comment" line-length limit (`line-length-with-comment` config key) -- see
    // Config.lineLengthWithComment()'s doc comment. Used by isSingleLineBody's fits-prediction,
    // which must agree with MiscRuleCurly.enforceCallLineBreaking's own comment-inclusive
    // fits-check (see that method's doc comment above).
    private final int lineLengthWithCommentLimit;
    private final int indentWidth;
    /**
     * Fallback one-indent-level unit when it can't be derived from the class/interface's own
     * body indentation -- built from the configured `indent-size` (see the constructor), not a
     * hardcoded literal, same bug class as `SwitchRule.deriveUnit`'s own former fallback.
     */
    private final String defaultIndentUnit;

    public JavaSpecificRule(final Lang lang)
    {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public JavaSpecificRule(final Lang lang, final int lineLengthLimit)
    {
        this(lang, lineLengthLimit, MiscRuleCurly.DEFAULT_INDENT_WIDTH);
    }

    public JavaSpecificRule(final Lang lang, final int lineLengthLimit, final int indentWidth)
    {
        this(lang, lineLengthLimit, indentWidth, MiscRuleCore.DEFAULT_LINE_LENGTH_WITH_COMMENT_LIMIT);
    }

    public JavaSpecificRule(
        final Lang lang,
        final int  lineLengthLimit,
        final int  indentWidth,
        final int  lineLengthWithCommentLimit
    )
    {
        this.lineLengthLimit            = lineLengthLimit;
        this.lineLengthWithCommentLimit = lineLengthWithCommentLimit;
        this.indentWidth                = indentWidth;
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < indentWidth; ++i) sb.append(' ');
        this.defaultIndentUnit = sb.toString();
    }

    /**
     * STYLE_JAVA.md §2: a recognized Java <b>method definition</b>'s own brace moves to its own
     * line (Allman) whenever it is currently K&amp;R/same-line. Class/interface/enum body braces
     * and every control-flow block already correctly stay K&amp;R via the shared, language-general
     * {@code BlockStructureRule} (`Token.name`/keyword-based classification), and lambda bodies
     * already correctly stay K&amp;R via {@code BlockStructureRule.isLambdaBrace}'s Java branch
     * (preceding token is `->`) -- none of those need touching here. One-liner exception:
     * brace-placement only, same resolution as {@code CppSpecificRule}'s §2 (see STATE.md "§2
     * one-liner scope") -- this method never inspects or changes how many physical lines the body
     * itself spans, it only ever relocates the opening `{`.
     *
     * <p>Candidate signal mirrors {@code CppSpecificRule.isCandidateSignatureName}: the `{` is
     * directly preceded (no comment, no newline in the gap) by a `)` whose matching `(` is itself
     * preceded by a candidate method name (IDENTIFIER, not itself preceded by `new`) -- this
     * naturally excludes every control-flow brace (`if`/`while`/`for`/`switch`/`catch`/`try` precede
     * their `(` with a KEYWORD, never an IDENTIFIER, or have no `(` at all) and every lambda (a
     * lambda's `{` is preceded directly by `->`, never by a bare `)`-after-identifier). A `throws`
     * clause between `)` and `{` (`void foo() throws IOException { ... }`) is a documented,
     * deliberate gap, identical in spirit to C++'s trailing-qualifier gap in its own §2 -- the
     * immediate-predecessor check (`{` directly preceded by `)`) excludes it, and there is no
     * STYLE_JAVA.md worked example to justify guessing past that signal.
     *
     * <p>One Java-specific false positive the C++ version of this signal never has to consider:
     * an enum constant's anonymous constant-body (`RED("red") { ... }`) is structurally identical
     * to a method-definition signature (`)` preceded by a matching `(` preceded by an IDENTIFIER
     * not preceded by `new`) -- Java enum constants never use `new` the way anonymous classes do.
     * Left unguarded, this would wrongly Allman-convert a constant body, violating STYLE_JAVA.md
     * §2's "method definitions only" scope. Guarded via {@code isEnumConstantBody}: a candidate is
     * excluded if its matching `}` is immediately followed (skipping whitespace/comments/newlines)
     * by `,` or `;` -- the universal separator/terminator of an enum constant list, and a shape a
     * real method body's closing `}` can never be followed by in Java (unlike C/C++, a Java method
     * body brace is never followed by a bare `;`). This leaves one documented residual gap: the
     * <i>last</i> constant in an enum with no trailing members and no trailing `;` (legal Java) has
     * its body's `}` followed directly by the enum's own closing `}`, indistinguishable from an
     * ordinary last-member-in-a-body shape without enum-body-context tracking this codebase doesn't
     * do elsewhere either -- left untouched-but-possibly-misconverted in that narrow case, same
     * "no AST, bounded-effort" posture as the rest of this codebase's documented gaps.
     *
     * <p>A `{` already on its own line (gap already contains a NEWLINE) is left untouched --
     * idempotent. The per-occurrence indentation target reuses the closing `)`'s own
     * line-leading indentation, same as {@code CppSpecificRule}'s identical method.
     *
     * <p>RDD_KEY_75/RDD_KEY_89: a one-liner method whose entire `{ ... }` body sits on one
     * physical line is never converted to Allman -- confirmed via `make test` against
     * java_modern_out.java/combined_out.java, where standalone one-liners (e.g. `distance()`,
     * `hasError()`, `isActive()`, each the only one-liner in its enclosing scope) stay K&R just
     * as reliably as ones adjacent to another one-liner (STYLE.md §14 groups).
     */
    public String enforceMethodDefinitionAllmanBraceStyle(final List<Token> tokens)
    {
        final Map<Integer, Integer>   gapToBrace = new HashMap<>();
        final List<OneLinerCandidate> oneLiners  = new ArrayList<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            if( !isPunct( tokens.get(i), "{" ) || tokens.get(i).name != null ) {
                // `name != null` means the tokenizer already tagged this as a named-construct
                // body (class/interface/enum/record) -- always K&R, handled by
                // `BlockStructureRule.qualifiesForKAndR`, never Allman. Without this guard a
                // record's own body brace (`record Point(...) {`, or with a trailing
                // `implements` clause) is structurally indistinguishable from a method
                // definition / compact-constructor shape and would be wrongly re-broken here.
                continue;
            } // if
            final int prevIdx = prevSignificantIndex(tokens, i);
            if(prevIdx < 0) continue;
            if( !isPunct( tokens.get(prevIdx), ")" ) ) {
                // Check for Java `throws` clause: `void foo() throws IOException {`
                final int throwsCloseParen = findCloseParenBeforeThrows(tokens, prevIdx);
                if( throwsCloseParen >= 0 && !MiscRuleCore.hasNewlineOrCommentBetween(tokens, prevIdx, i)
                        && isMethodDefinitionCloseParen(tokens, throwsCloseParen)
                        && !isEnumConstantBody(tokens, i)
                        && !MiscRuleCore.anyFrozen(tokens, throwsCloseParen, i + 1) ) {
                    final int closeBraceIdx = MiscRuleCore.matchBraceForward(tokens, i);
                    if( closeBraceIdx >= 0 && isSingleLineBody(tokens, i, closeBraceIdx) ) {
                        final int openParenIdx = MiscRuleCore.matchParenBackward(tokens, throwsCloseParen);
                        final int nameIdx      = prevSignificantIndex(tokens, openParenIdx);
                        oneLiners.add(
                            new OneLinerCandidate(nameIdx, throwsCloseParen, i, closeBraceIdx)
                        );
                    } // if
                    else {
                        // Keep `throws IOException` in output; only move `{` to its own line
                        gapToBrace.put(prevIdx + 1, i);
                    }
                } // if
                else if( isCompactConstructorBrace(tokens, prevIdx, i)
                        && !MiscRuleCore.hasNewlineOrCommentBetween(tokens, prevIdx, i)
                        && !MiscRuleCore.anyFrozen(tokens, prevIdx, i + 1) ) {
                    gapToBrace.put(prevIdx + 1, i);
                }
                continue;
            } // if
            final int closeParenIdx = prevIdx;
            if( !isMethodDefinitionCloseParen(tokens, closeParenIdx) ) continue;
            if( MiscRuleCore.hasNewlineOrCommentBetween(tokens, closeParenIdx, i) ) continue;
            if( isEnumConstantBody(tokens, i) ) continue;
            if( MiscRuleCore.anyFrozen(tokens, closeParenIdx, i + 1) ) continue;
            final int closeBraceIdx = MiscRuleCore.matchBraceForward(tokens, i);
            if( closeBraceIdx >= 0 && isSingleLineBody(tokens, i, closeBraceIdx) ) {
                final int openParenIdx = MiscRuleCore.matchParenBackward(tokens, closeParenIdx);
                final int nameIdx      = prevSignificantIndex(tokens, openParenIdx);
                oneLiners.add( new OneLinerCandidate(nameIdx, closeParenIdx, i, closeBraceIdx) );
                continue;
            }
            gapToBrace.put(closeParenIdx + 1, i);
        } // for

        final StringBuilder out = new StringBuilder();
              int           i   = 0;
        while( i < tokens.size() ) {
            final Integer braceIdx = gapToBrace.get(i);
            if(braceIdx != null) {
                out.append('\n').append( lineIndent(tokens, i - 1) );
                out.append( tokens.get(braceIdx).text );
                i = braceIdx + 1;
            }
            else {
                out.append( tokens.get(i).text );
                ++i;
            }
        } // while

        return out.toString();
    }

    /**
     * One method-definition `{ ... }` whose body sits entirely on one physical line --
     * always stays K&amp;R (RDD_KEY_75/RDD_KEY_89), never converted to Allman.
     */
    private static final class OneLinerCandidate {

        final int nameIdx;
        final int closeParenIdx;
        final int braceIdx;
        final int closeBraceIdx;

        OneLinerCandidate(
            final int nameIdx,
            final int closeParenIdx,
            final int braceIdx,
            final int closeBraceIdx
        )
        {
            this.nameIdx       = nameIdx;
            this.closeParenIdx = closeParenIdx;
            this.braceIdx      = braceIdx;
            this.closeBraceIdx = closeBraceIdx;
        }

    } // class OneLinerCandidate

    /**
     * True iff no {@code NEWLINE} token appears between {@code braceIdx} and {@code closeBraceIdx}
     * inclusive -- the whole `{ ... }` span is one physical line -- AND the body isn't predicted
     * to be broken across lines later by {@code MiscRuleCurly.enforceCallLineBreaking} (Phase 1, later
     * in the pipeline) anyway. Without that second condition, a fresh format sees the body still
     * on one physical line (still short, pre-call-breaking) and keeps `{` K&amp;R inline, but
     * reformatting that already-broken output sees a genuinely multi-line body and moves `{` to
     * Allman -- a pass-ordering idempotency bug identical in shape to the one already documented
     * on {@code GetterSetterRuleCurly.parseOneLinerMember}. The prediction only has to agree with
     * {@code enforceCallLineBreaking}'s own verdict well enough to avoid flip-flopping: once this
     * method predicts "too long" and goes Allman, the body only ever grows more lines after that
     * (never re-collapses), so every later pass keeps agreeing.
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
        if( hasBreakableCall(tokens, braceIdx, closeBraceIdx) ) {
            final int lineStart = lineStartIndex(tokens, braceIdx);
            // Find the end of the physical line, including any trailing same-line `//` comment
            // (e.g. a §14-group alignment comment) -- `MiscRuleCurly.enforceCallLineBreaking`'s own
            // fit-check (`collapseToOneLine`) measures the whole physical line including such a
            // trailing comment, so omitting it here made this prediction disagree with that later
            // pass whenever the comment alone pushed an otherwise-under-limit line over
            // `lineLengthLimit`: round1 predicted "fits" (comment excluded) and kept `{` K&R, but
            // the call got broken onto multiple lines under it anyway; round2 then saw genuine
            // embedded NEWLINEs and flipped to Allman -- an idempotency bug.
            int lineEnd = closeBraceIdx;
            for( int k = closeBraceIdx + 1; k < tokens.size(); ++k ) {
                if( tokens.get(k).type == TokenType.NEWLINE ) break;
                lineEnd = k;
            }
            // Match `collapseToOneLine`'s own width measurement exactly: any whitespace/newline
            // run (including this method's own body's internal single-space gaps, and any
            // wide alignment-padding gap before a trailing comment) collapses to exactly one
            // space, not its raw character count -- otherwise leftover un-collapsed source
            // padding (e.g. hand-aligned comment columns already present in the *original*
            // source, not yet re-collapsed by this early pass) could overstate the width and
            // wrongly predict "too long" for a line that, once rendered, actually fits.
            // `MiscRuleCurly.enforceCallLineBreaking`'s own fit-check measures `baseIndent +
            // collapseToOneLine(...)` -- the physical line's leading indentation counts too, not
            // just the significant-token span starting at `lineStart` (which is the first
            // *significant* token, excluding leading whitespace). Omitting it undercounted every
            // indented one-liner's width by its indent depth.
            int     width        = MiscRuleCore.expandedIndentWidth( lineIndent(tokens, lineStart), indentWidth );
            boolean lastWasSpace = false;
            for(int k = lineStart; k <= lineEnd; ++k) {
                final Token t = tokens.get(k);
                if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                    if(!lastWasSpace && width > 0) width++;
                    lastWasSpace = true;
                    continue;
                }
                width        += t.text.length();
                lastWasSpace  = false;
            } // for
            // Only use the comment-aware limit when a trailing same-line comment is actually
            // present between the close brace and `lineEnd` -- otherwise this is a plain code-only
            // line and must keep agreeing with `MiscRuleCurly.enforceCallLineBreaking`'s own
            // code-only fits-check (`lineLengthLimit`), which itself only switches to
            // `lineLengthWithCommentLimit` when it detects a trailing comment (see that method).
            final int effectiveLimit = hasCommentBetween(
                tokens, closeBraceIdx, lineEnd + 1
            ) ? lineLengthWithCommentLimit : lineLengthLimit;
            if(width > effectiveLimit) return false;
        } // if

        return true;
    }

    /**
     * True if {@code [from, to]} contains at least one {@code name(args)} call with a non-empty
     * argument list -- the shape {@code MiscRuleCurly.enforceCallLineBreaking} may later break across
     * lines if it doesn't fit (zero-arg calls are never broken, see that method's own doc
     * comment). Duplicated from {@code GetterSetterRuleCurly}'s identical helper -- same "each rule
     * class matches its own local conventions" precedent as {@code isSingleLineBody} itself,
     * already duplicated across this class and {@code CppSpecificRule}.
     */
    private boolean hasBreakableCall(final List<Token> tokens, final int from, final int to)
    {
        for(int i = from; i <= to; ++i) {
            final Token t = tokens.get(i);
            if(t.type != TokenType.IDENTIFIER) continue;
            final int parenIdx = nextSignificantIndex(tokens, i);
            if( parenIdx < 0 || parenIdx > to || !isPunct( tokens.get(parenIdx), "(" ) ) continue;
            final int closeIdx = MiscRuleCore.matchParenForward(tokens, parenIdx);
            if(closeIdx < 0 || closeIdx > to) continue;
            final int argsFrom = nextSignificantIndex(tokens, parenIdx);
            if(argsFrom >= 0 && argsFrom < closeIdx) return true;
        } // for

        return false;
    }

    private boolean isMethodDefinitionCloseParen(final List<Token> tokens, final int closeParenIdx)
    {
        final int openParenIdx = MiscRuleCore.matchParenBackward(tokens, closeParenIdx);

        return openParenIdx >= 0 && isCandidateMethodName(tokens, openParenIdx);
    }

    /**
     * For Java {@code throws} clauses: given the token at {@code fromIdx} (the significant token
     * immediately before {@code {}) that is NOT {@code )}, checks if it is the last exception
     * class name in a {@code throws} clause. Scans backward through comma-separated IDENTIFIERs
     * to the {@code throws} keyword, then expects {@code )} immediately before it.
     * Returns the index of the {@code )} of the method parameter list, or -1 if no such pattern.
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

    /**
     * True iff the token immediately before {@code openIdx} is an IDENTIFIER not itself preceded
     * by `new` -- the candidate-method-name signal, mirroring
     * {@code CppSpecificRule.isCandidateSignatureName}.
     */
    private boolean isCandidateMethodName(final List<Token> tokens, final int openIdx)
    {
        final int nameIdx = prevSignificantIndex(tokens, openIdx);
        if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) return false;
        final int beforeName = prevSignificantIndex(tokens, nameIdx);

        return beforeName < 0 || tokens.get(beforeName).type != TokenType.KEYWORD
                || !"new".equals( tokens.get(beforeName).text );
    }

    /**
     * Access modifiers legal on a compact canonical constructor -- the only keywords that may
     * sit between a member boundary and the constructor's own name
     */
    private static final Set<String> COMPACT_CTOR_MODIFIERS = new HashSet<>( Arrays.asList(
        "public", "private", "protected"
    ) );

    /**
     * True iff the `{` at {@code braceIdx} (whose immediately preceding significant token,
     * already confirmed not a `)`, sits at {@code identIdx}) is a record's compact canonical
     * constructor -- `public Point { ... }`, no parameter list at all (STYLE_JAVA17.md §1).
     * A bare IDENTIFIER directly before `{` is structurally ambiguous: besides a compact
     * constructor, it's also produced by an enum constant body omitting constructor args
     * (`RED { ... }`, excluded via {@code isEnumConstantBody}) and -- the case that bit the
     * first version of this method -- the <i>last type in an enclosing class/interface's own
     * `implements`/`extends` clause</i> (`class Foo implements Bar {`), since `Bar` is just as
     * bare an IDENTIFIER as a constructor name. Distinguished by walking back from the name,
     * over any access modifiers (the only thing legal before a real constructor name), and
     * requiring what's left to be a member/scope boundary (`}`, `;`, `{`, or start of file) --
     * `implements`/`extends`/`,` never satisfy that, so they correctly fail this check instead
     * of needing `Token.name` (which can't see back across an arbitrarily long `implements`
     * list either, so it's null for both the true positive and the false positive alike here).
     */
    private boolean isCompactConstructorBrace(
        final List<Token> tokens,
        final int         identIdx,
        final int         braceIdx
    )
    {
        if( tokens.get(identIdx).type != TokenType.IDENTIFIER ) return false;
        if( isEnumConstantBody(tokens, braceIdx) ) return false;
        int i = prevSignificantIndex(tokens, identIdx);
        while( i >= 0 && tokens.get(
            i
        ).type == TokenType.KEYWORD && COMPACT_CTOR_MODIFIERS.contains(
            tokens.get(i).text
        ) ) i = prevSignificantIndex(
            tokens, i - 1
        );

        return i < 0 || isPunct( tokens.get(i), "}" ) || isPunct( tokens.get(i), ";" )
                || isPunct( tokens.get(i), "{" );
    }

    /**
     * True iff the `{` at {@code braceIdx} is an enum constant's anonymous constant-body --
     * detected via its matching `}` being immediately followed by `,` or `;`, the universal
     * enum-constant-list separator/terminator (see this method's caller's doc comment for the
     * residual gap this heuristic doesn't cover)
     */
    private boolean isEnumConstantBody(final List<Token> tokens, final int braceIdx)
    {
        final int closeBraceIdx = MiscRuleCore.matchBraceForward(tokens, braceIdx);
        if(closeBraceIdx < 0) return false;
        final int next = nextSignificantIndex(tokens, closeBraceIdx);

        return next >= 0 && ( isPunct(
            tokens.get(next), ","
        ) || isPunct(
            tokens.get(next), ";"
        ) );
    }

    /**
     * When a Java enum body has trailing members after its constant list (methods, fields,
     * constructors), detaches the constant-list-terminating `;` onto its own line, with a blank
     * line before and after it -- e.g.:
     * <pre>
     *     public enum State {
     *
     *         IDLE, RUNNING, PAUSED, ERROR
     *
     *         ;
     *
     *         public boolean isActive() { return this == RUNNING; }
     *
     *     }
     * </pre>
     * An enum with no trailing members (the `;` is the very last thing before the enum's own `}`,
     * or absent entirely -- legal Java) is left untouched: there is nothing to separate the
     * constant list from.
     */
    public String separateEnumConstantListTerminator(final List<Token> tokens)
    {
        final Map<Integer, String> terminators = findEnumConstantListTerminators(tokens);
        if( terminators.isEmpty() ) {
            final StringBuilder sb = new StringBuilder();
            for(final Token t : tokens) sb.append(t.text);
            return sb.toString();
        }
        final StringBuilder out             = new StringBuilder();
        final List<Token>   gap             = new ArrayList<>();
              int           lastSignificant = -1;
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( isGapToken(t) ) {
                gap.add(t);
                continue;
            }
            final boolean thisIsTerminator  = terminators.containsKey(i);
            final boolean prevWasTerminator = lastSignificant >= 0 && terminators.containsKey(
                lastSignificant
            );
            if(thisIsTerminator || prevWasTerminator) {
                final String indent = thisIsTerminator ? terminators.get(
                    i
                ) : terminators.get(
                    lastSignificant
                );
                out.append('\n').append('\n').append(indent);
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
     * Finds every Java enum body's constant-list-terminating `;` that has at least one more
     * member after it before the enum's own `}` -- keyed by token index, valued by the indent
     * string of the enum body's member lines (derived from its first member's own line)
     */
    private Map<Integer, String> findEnumConstantListTerminators(final List<Token> tokens)
    {
        final Map<Integer, String> result = new HashMap<>();
        for( int i = 0; i < tokens.size(); ++i ) {
            if( !isPunct( tokens.get(i), "{" ) || !isEnumBodyBrace(tokens, i) ) continue;
            final int closeBraceIdx = MiscRuleCore.matchBraceForward(tokens, i);
            if(closeBraceIdx < 0) continue;
            final int firstMember = nextSignificantIndex(tokens, i + 1);
            if(firstMember < 0 || firstMember >= closeBraceIdx) continue;
            // Derived from the enum body's own opening-brace line indent plus one indent unit,
            // NOT from `lineIndentAt(tokens, firstMember)`'s own current line: on a reformat, the
            // first member's line indent already reflects whatever this same pass (or a later
            // alignment pass) stamped onto it last round, so reusing it verbatim compounds by one
            // level per pass (8 -> 10 -> 12 -> ...) -- an idempotency bug. The enum body's own
            // `{` line indent is stable across passes (it comes from brace-depth-based reindent,
            // not append-only drift), so anchoring to it plus a single indent unit is absolute.
            final String indent = lineIndentAt(tokens, i) + defaultIndentUnit;
                  int    depth  = 0;
            for(int p = i + 1; p < closeBraceIdx; ++p) {
                final Token t = tokens.get(p);
                if( isGapToken(t) ) continue;
                if( isPunct(t, "(") || isPunct(t, "{") ) {
                    ++depth;
                }
                else if( isPunct(t, ")") || isPunct(t, "}") ) {
                    --depth;
                }
                else if( depth == 0 && isPunct(t, ";") ) {
                    final int next = nextSignificantIndex(tokens, p + 1);
                    if( next >= 0 && next < closeBraceIdx && !MiscRuleCore.anyFrozen(
                        tokens, firstMember, closeBraceIdx
                    ) ) result.put(
                        p, indent
                    );
                    break;
                }
            } // for p
        } // for i

        return result;
    }

    /**
     * True iff the `{` at {@code braceIdx} opens a Java enum's body -- scans backward past the
     * enum's name and any `implements`/generic-bound clause tokens until it finds the `enum`
     * keyword, bailing out on an intervening `{`/`}`/`;` (a different construct entirely)
     */
    private boolean isEnumBodyBrace(final List<Token> tokens, final int braceIdx)
    {
        int p = prevSignificantIndex(tokens, braceIdx - 1);
        while(p >= 0) {
            final Token t = tokens.get(p);
            if( t.type == TokenType.KEYWORD && "enum".equals(t.text) ) return true;
            if( isPunct(t, "{") || isPunct(t, "}") || isPunct(t, ";") ) return false;
            p = prevSignificantIndex(tokens, p - 1);
        }

        return false;
    }

    /**
     * The indentation of the physical line containing token {@code idx} -- the whitespace run
     * immediately after the nearest preceding {@code NEWLINE} (or the start of the file), taken
     * regardless of whether {@code idx} itself is that line's first token
     */
    private String lineIndentAt(final List<Token> tokens, final int idx)
    {
        int p = idx - 1;
        while( p >= 0 && tokens.get(p).type != TokenType.NEWLINE ) p--;
        final StringBuilder indent = new StringBuilder();
        for(int k = p + 1; k < idx; ++k) {
            if( tokens.get(k).type == TokenType.WHITESPACE ) indent.append( tokens.get(k).text );
            else                                             break;
        }

        return indent.toString();
    }

    /**
     * STYLE_JAVA.md §7: groups every top-level {@code import} statement into six fixed buckets
     * (static, java/javax, org, com, local, other), sorts within each bucket, and re-renders the
     * whole import block in {@code groupOrder}'s order, separated by {@code blankLines} blank
     * line(s) between non-empty groups. Per the resolved "trust the worked example" reading (see
     * STATE.md), classification priority is static &gt; local &gt; java/javax &gt; org &gt; com
     * &gt; other, but {@code groupOrder} (typically {@code java, com, org, other, local, static})
     * controls only emission order, not classification.
     *
     * <p>Local-package detection: the first {@code package} declaration found in {@code tokens}
     * supplies the local prefix -- its top {@code importDepth} dot-separated components. An import
     * is "local" iff its own leading components match that prefix component-for-component (a
     * wildcard import like {@code com.mycompany.*} still matches on its non-wildcard prefix). If no
     * {@code package} declaration is found, the local bucket is simply never populated.
     *
     * <p>Each `import [static] a.b.c[.*];` statement is parsed token-by-token
     * ({@link #parseImportStatement}) by concatenating IDENTIFIER/`.`/`*` tokens directly (Java
     * import paths never contain meaningful internal whitespace) -- any comment found anywhere
     * inside one import statement, or floating in the gap between two import statements, aborts
     * the *entire* pass and returns {@code tokens} byte-for-byte unchanged, same "never guess past
     * an unrecognized shape" posture used throughout this codebase; losing a comment via silent
     * reordering is not an acceptable failure mode. A file with zero import statements is also a
     * no-op. Regenerated import lines are canonical text ({@code "import " ["static "] path ";"}),
     * discarding original internal spacing -- same "restructured content is regenerated, not
     * preserved verbatim" precedent as {@code DeclarationAlignmentRuleCurly}'s static reordering.
     * "Unused imports are not removed" (STYLE_JAVA.md's own words) is honored by construction: no
     * usage analysis is performed anywhere in this method.
     *
     * @param groupOrder must be a permutation of exactly the six fixed bucket names in
     *        {@link #IMPORT_GROUP_KEYS} -- a config-validation precondition, not a per-file
     *        content-shape judgment call, so an invalid value throws rather than silently dropping
     *        a bucket's imports
     */
    public String enforceImportOrdering(
        final List<Token>  tokens,
        final List<String> groupOrder,
        final boolean      sortAlphabetically,
        final int          importDepth,
        final int          blankLines
    )
    {
        if( !new HashSet<>(groupOrder).equals(
            IMPORT_GROUP_KEYS
        ) || groupOrder.size() != IMPORT_GROUP_KEYS.size() ) throw new IllegalArgumentException(
            "groupOrder must be a permutation of " + IMPORT_GROUP_KEYS + ", got: " + groupOrder
        );

        final List<String> localPrefix = findLocalPrefix(tokens, importDepth);

          int                depth            = 0;
          int                firstImportIdx   = -1;
          int                prevSemicolonIdx = -1;
          int                lastSemicolonIdx = -1;
          boolean            blocked          = false;
    final List<ParsedImport> imports          = new ArrayList<>();
    final int                n                = tokens.size();
          int                i                = 0;
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
                if(firstImportIdx < 0) {
                    firstImportIdx = i;
                }
                else if( hasCommentBetween(tokens, prevSemicolonIdx + 1, i) ) {
                    blocked = true;
                    break;
                }
                final ParsedImport parsed = parseImportStatement(tokens, i);
                if(parsed == null) {
                    blocked = true;
                    break;
                }
                if( MiscRuleCore.anyFrozen(tokens, i, parsed.semicolonIdx + 1) ) {
                    blocked = true;
                    break;
                }
                imports.add(parsed);
                prevSemicolonIdx = parsed.semicolonIdx;
                lastSemicolonIdx = parsed.semicolonIdx;
                i                = parsed.semicolonIdx + 1;
                continue;
            } // if
            ++i;
        } // while

        if( blocked || imports.isEmpty() ) return MiscRuleCore.joinVerbatim(tokens);

        final Map<String, List<ParsedImport>> buckets = new HashMap<>();
        for(final String key : IMPORT_GROUP_KEYS) buckets.put( key, new ArrayList<>() );
        for(final ParsedImport imp : imports) buckets.get(
            classifyImportGroup(imp, localPrefix)
        ).add(
            imp
        );
        if(sortAlphabetically) {
            for( final List<ParsedImport> group : buckets.values() ) group.sort(
                (a, b) -> a.path.compareTo(b.path)
            );
        }

        final StringBuilder body            = new StringBuilder();
              boolean       emittedAnyGroup = false;
        for(final String groupKey : groupOrder) {
            final List<ParsedImport> members = buckets.get(groupKey);
            if( members.isEmpty() ) continue;
            if(emittedAnyGroup) {
                for(int b = 0; b < blankLines + 1; ++b) body.append('\n');
            }
            for( int m = 0; m < members.size(); ++m ) {
                if(m > 0) body.append('\n');
                final ParsedImport imp = members.get(m);
                body.append("import ");
                if(imp.isStatic) body.append("static ");
                body.append(imp.path).append(';');
            } // for m
            emittedAnyGroup = true;
        } // for groupKey

        final StringBuilder out = new StringBuilder();
        appendRange(out, tokens, 0, firstImportIdx);
        out.append(body);
        appendRange(out, tokens, lastSemicolonIdx + 1, n);

        return out.toString();
    }

    /** One successfully-parsed `import [static] path;` statement */
    private static final class ParsedImport {

        final boolean isStatic;
        final String  path;
        final int     semicolonIdx;

        ParsedImport(final boolean isStatic, final String path, final int semicolonIdx)
        {
            this.isStatic     = isStatic;
            this.path         = path;
            this.semicolonIdx = semicolonIdx;
        }

    } // class ParsedImport

    /**
     * Parses one `import [static] a.b.c[.*];` statement starting at the `import` keyword token at
     * {@code importIdx}. Returns {@code null} -- signaling "bail the entire pass" to the caller --
     * if a comment is found anywhere inside the statement, or if any token other than
     * WHITESPACE/NEWLINE, the `static` keyword (only before any path token), an IDENTIFIER, or a
     * dot/star OP token is encountered before the terminating `;`. Never guesses past an
     * unrecognized import shape.
     *
     * <p>Dot/star OP tokens need a dedicated check ({@link #isPathOp}) rather than a literal `"."`/
     * `"*"` text match: {@code TokenizerCurly}'s {@code MULTI_CHAR_OPS} list includes C++'s
     * pointer-to-member operator `".*"`, shared across languages, so a wildcard import's trailing
     * `.{@literal *}` lexes as a single combined OP token with text {@code ".*"}, not two separate
     * single-char tokens -- discovered via a failing smoke-test case on `import pkg.*;`.</p>
     */
    private ParsedImport parseImportStatement(final List<Token> tokens, final int importIdx)
    {
          boolean       isStatic     = false;
          boolean       sawPathToken = false;
    final StringBuilder path         = new StringBuilder();
    final int           n            = tokens.size();
          int           p            = importIdx + 1;
        while(p < n) {
            final Token t = tokens.get(p);
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                ++p;
                continue;
            }
            if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) return null;
            if( isPunct(
                t, ";"
            ) ) return sawPathToken ? new ParsedImport(
                isStatic, path.toString(), p
            ) : null;
            if( !sawPathToken && !isStatic && t.type == TokenType.KEYWORD && "static".equals(
                t.text
            ) ) {
                isStatic = true;
                ++p;
                continue;
            }
            if( t.type == TokenType.IDENTIFIER || isPathOp(t) ) {
                path.append(t.text);
                sawPathToken = true;
                ++p;
                continue;
            }
            return null;
        } // while

        return null;
    }

    /**
     * True iff a {@code COMMENT_LINE}/{@code COMMENT_BLOCK} token exists anywhere in
     * {@code tokens[fromInclusive, toExclusive)} -- used to detect a floating comment between two
     * otherwise-clean import statements, which would otherwise be silently dropped by reordering
     */
    private boolean hasCommentBetween(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toExclusive
    )
    {
        for( int i = Math.max(fromInclusive, 0); i < toExclusive; ++i ) {
            final TokenType type = tokens.get(i).type;
            if(type == TokenType.COMMENT_LINE || type == TokenType.COMMENT_BLOCK) return true;
        }

        return false;
    }

    /**
     * Reads the first {@code package a.b.c;} declaration in {@code tokens} (best-effort -- this
     * is a non-destructive lookup, not a rewrite, so a malformed/commented package line just
     * yields whatever IDENTIFIER tokens are found rather than bailing) and returns its top
     * {@code importDepth} dot-components. Empty list if no `package` declaration exists.
     */
    private List<String> findLocalPrefix(final List<Token> tokens, final int importDepth)
    {
        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( t.type == TokenType.KEYWORD && "package".equals(t.text) ) {
                final List<String> components = new ArrayList<>();
                      int          p          = i + 1;
                while( p < tokens.size() && !isPunct( tokens.get(p), ";" ) ) {
                    if( tokens.get(
                        p
                    ).type == TokenType.IDENTIFIER ) components.add(
                        tokens.get(p).text
                    );
                    ++p;
                } // while
                return components.subList( 0, Math.min( importDepth, components.size() ) );
            } // if
        } // for

        return Collections.emptyList();
    }

    /**
     * Classification priority: static &gt; local &gt; java/javax &gt; org &gt; com &gt; other --
     * see {@link #enforceImportOrdering}'s doc comment
     */
    private String classifyImportGroup(final ParsedImport imp, final List<String> localPrefix)
    {
        if(imp.isStatic) return "static";
        final String[] parts = imp.path.split("\\.");
        if( !localPrefix.isEmpty() && matchesPrefix(parts, localPrefix) ) return "local";
        final String first = parts.length > 0 ? parts[0] : "";
        if( "java".equals(first) || "javax".equals(first) ) return "java";
        if( "com".equals(first) ) return "com";
        if( "org".equals(first) ) return "org";

        return "other";
    }
    
    private boolean matchesPrefix(final String[] parts, final List<String> prefix)
    {
        if( parts.length < prefix.size() ) return false;
        for( int i = 0; i < prefix.size(); ++i ) {
            if( !parts[i].equals( prefix.get(i) ) ) return false;
        }

        return true;
    }

    /**
     * STYLE_JAVA17.md §2: line-breaks a {@code permits} clause -- inline if the full
     * class/interface declaration line (from the {@code class}/{@code interface} keyword's own
     * line start through the body's opening {@code {}) fits within {@code
     * lineLengthLimit}, otherwise one permitted type per line, column-aligned under the
     * first type, with the body's {@code {} trailing the last type. Always re-decides from
     * scratch regardless of the file's current wrapped/unwrapped shape -- same "regenerate, don't
     * preserve" posture as {@link #enforceImportOrdering}'s static reordering, so the pass is
     * idempotent. Bails per occurrence (leaves that one untouched) if a comment is found anywhere
     * between the declaration's line start and the body's {@code {}, or if the permitted-type list
     * can't be parsed -- never guesses past an unrecognized shape, same posture as {@link
     * #enforceImportOrdering}.
     */
    public String enforcePermitsClauseLineBreaking(final List<Token> tokens)
    {
        final List<int[]>  spans   = new ArrayList<>();
        final List<String> renders = new ArrayList<>();

        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( t.type != TokenType.KEYWORD || !"permits".equals(t.text) ) continue;
            final int classIdx = prevClassOrInterfaceKeyword(tokens, i);
            if(classIdx < 0) continue;
            final int openBraceIdx = nextPunct(tokens, i, "{");
            if(openBraceIdx < 0) continue;
            final int declStart = lineStartIndex(tokens, classIdx);
            if( hasCommentBetween(tokens, declStart, openBraceIdx + 1) ) continue;
            final int prevSigIdx = prevSignificantIndex(tokens, i);
            if(prevSigIdx < 0) continue;
            final List<String> types = parsePermittedTypes(tokens, i + 1, openBraceIdx);
            if(types == null) continue;
            if( MiscRuleCore.anyFrozen(tokens, declStart, openBraceIdx + 1) ) continue;

            final String baseIndent    = lineIndent(tokens, declStart);
            final String collapsedFull = baseIndent + collapseToOneLine(
                tokens, declStart, openBraceIdx
            );
            final String rendered      = collapsedFull.length() <= lineLengthLimit ? renderPermitsInline(
                types
            ) : renderPermitsWrapped(
                tokens, baseIndent, openBraceIdx, types
            );

            spans.add( new int[] { prevSigIdx + 1, openBraceIdx + 1 } );
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

    private String renderPermitsInline(final List<String> types)
    {
        return " permits " + String.join(", ", types) + " {";
    }

    /**
     * Derives the one-indent-level unit from the gap between the declaration's own indent and its
     * body's first member's indent (same precedent as {@code SwitchRule.deriveUnit}), falling back
     * to {@link #defaultIndentUnit}.
     */
    private String renderPermitsWrapped(
        final List<Token>  tokens,
        final String       baseIndent,
        final int          openBraceIdx,
        final List<String> types
    )
    {
        final String unit  = deriveIndentUnit(tokens, baseIndent, openBraceIdx);
        final String align = repeatChar(
            ' ', baseIndent.length() + unit.length() + "permits ".length()
        );

        final StringBuilder sb = new StringBuilder();
        sb.append('\n').append(baseIndent).append(unit).append("permits ");
        for( int idx = 0; idx < types.size(); ++idx ) {
            if(idx > 0) sb.append('\n').append(align);
            sb.append( types.get(idx) );
            if( idx < types.size() - 1 ) sb.append(',');
        }
        sb.append(" {");

        return sb.toString();
    }

    private String deriveIndentUnit(
        final List<Token> tokens,
        final String      baseIndent,
        final int         openBraceIdx
    )
    {
        final int firstBodySig = nextSignificantIndex(tokens, openBraceIdx);
        if(firstBodySig < 0) return defaultIndentUnit;
        final String bodyIndent = lineIndent(tokens, firstBodySig);
        if( bodyIndent.length() > baseIndent.length() && bodyIndent.startsWith(
            baseIndent
        ) ) return bodyIndent.substring(
            baseIndent.length()
        );

        return defaultIndentUnit;
    }

    private String repeatChar(final char c, final int count)
    {
        final StringBuilder sb = new StringBuilder(count);
        for(int i = 0; i < count; ++i) sb.append(c);

        return sb.toString();
    }

    /**
     * Splits {@code tokens[fromInclusive, toExclusive)} on top-level commas (depth-tracked across
     * `&lt;...&gt;` generic argument lists) into one collapsed-to-one-line, trimmed string per
     * permitted type. Returns {@code null} if the list is empty -- an unparseable/empty shape this
     * method never guesses past. Multi-char OPs like `&gt;&gt;` that close two nested generic levels
     * at once are a known, accepted residual gap (permitted-type lists are realistically always
     * simple class names, never deeply nested generics).
     */
    private List<String> parsePermittedTypes(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toExclusive
    )
    {
        final List<String>  types      = new ArrayList<>();
        final StringBuilder current    = new StringBuilder();
              int           angleDepth = 0;
        for(int i = fromInclusive; i < toExclusive; ++i) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) return null;
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                if( current.length() > 0 && current.charAt(
                    current.length() - 1
                ) != ' ' ) current.append(
                    ' '
                );
                continue;
            } // if
            if( isPunct(t, "<") ) {
                ++angleDepth;
            }
            else if( isPunct(t, ">") ) {
                --angleDepth;
            }
            else if( angleDepth == 0 && isPunct(t, ",") ) {
                types.add( current.toString().trim() );
                current.setLength(0);
                continue;
            }
            current.append(t.text);
        } // for
        final String last = current.toString().trim();
        if( last.isEmpty() ) return null;
        types.add(last);

        return types;
    }

    /**
     * Collapses {@code tokens[fromInclusive, toInclusive]} to one physical line by replacing every
     * WHITESPACE/NEWLINE run between significant tokens with a single space -- used only to measure
     * the would-be-inline rendering length, never emitted verbatim
     */
    private String collapseToOneLine(
        final List<Token> tokens,
        final int         fromInclusive,
        final int         toInclusive
    )
    {
        final StringBuilder sb = new StringBuilder();
        for(int i = fromInclusive; i <= toInclusive; ++i) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.WHITESPACE || t.type == TokenType.NEWLINE) {
                if( sb.length() > 0 && sb.charAt( sb.length() - 1 ) != ' ' ) sb.append(' ');
                continue;
            }
            sb.append(t.text);
        } // for

        return sb.toString().trim();
    }

    /**
     * Sentinel returned by {@link #findCaseArrowOrColon} when a label's top-level `:` is found
     * instead of `->` -- the switch is colon-form, owned by {@code SwitchRule}, never touched here
     */
    private static final int COLON_FOUND = -2;

    /**
     * Sentinel returned by {@link #findCaseArrowOrColon} when neither terminator is found before
     * hitting a depth-0 `;`/`{` boundary first -- a malformed/unrecognized label shape
     */
    private static final int ARROW_NOT_FOUND = -1;

    /**
     * STYLE_JAVA17.md §3: column-aligns the `->` across every case of an arrow-labeled switch
     * (a switch *expression*, or the same arrow syntax used as a statement -- the rule doesn't
     * distinguish, since the token shape is identical either way). This is a distinct construct
     * from the `:`-labeled switch statement already fully handled by {@code SwitchRule}'s
     * STYLE.md §13 passes, which this method never touches: arrow-labeled and colon-labeled cases
     * never co-exist in one switch per the JLS, so the two rules' switch discovery is naturally
     * disjoint -- {@link #findArrowCases} returns {@code null} (skip, untouched) the instant it
     * finds a label terminated by `:` instead of `->`.
     *
     * <p>All-or-nothing per switch (§3.1, §7 resolved decision): if any case in a given switch has
     * a block body (`-> {`), that whole switch is left untouched -- no alignment at all, same
     * conservative posture as STYLE.md §13's own inline-alignment bail-out. Nothing else is needed
     * for a block-body case: its `{` already renders K&amp;R via {@code
     * BlockStructureRule.isLambdaBrace}'s existing `->`-preceded-brace branch (lambdas and arrow-form
     * switch cases are structurally identical at that brace), and already gets no closing comment
     * via the same not-`)`-preceded shape in {@code BlockStructureRule.classifyBrace} -- both already
     * correct, pre-existing, general-purpose behavior, not modified here.
     *
     * <p>A switch with a malformed/unrecognized label shape is also left completely untouched, same
     * "never guess past an unrecognized shape" posture used throughout this codebase. Only the label
     * span -- from the `case`/`default` keyword through the `->` and the single space after it -- is
     * ever rewritten; body content (everything from the first significant token after `->` onward)
     * is never touched, so a block body's internal formatting, a multi-line expression, or a
     * `throw` statement survives exactly as written.
     */
    public String enforceSwitchExpressionArrowAlignment(final List<Token> tokens)
    {
        final Map<Integer, String> overrides = new HashMap<>();

        for( int i = 0; i < tokens.size(); ++i ) {
            final Token t = tokens.get(i);
            if( t.type != TokenType.KEYWORD || !"switch".equals(t.text) ) continue;
            final int openParenIdx = nextSignificantIndex(tokens, i);
            if( openParenIdx < 0 || !isPunct( tokens.get(openParenIdx), "(" ) ) continue;
            final int closeParenIdx = MiscRuleCore.matchParenForward(tokens, openParenIdx);
            if(closeParenIdx < 0) continue;
            final int openBraceIdx = nextSignificantIndex(tokens, closeParenIdx);
            if( openBraceIdx < 0 || !isPunct( tokens.get(openBraceIdx), "{" ) ) continue;
            final int closeBraceIdx = MiscRuleCore.matchBraceForward(tokens, openBraceIdx);
            if(closeBraceIdx < 0) continue;

            final List<ArrowCase> cases = findArrowCases(tokens, openBraceIdx, closeBraceIdx);
            if( cases == null || cases.isEmpty() ) continue; // Colon-form (SwitchRule's), or malformed -- never ours

            boolean anyBlockBody = false;
            for(final ArrowCase c : cases) {
                if(c.blockBody) {
                    anyBlockBody = true;
                    break;
                }
            }
            if( !anyBlockBody && !MiscRuleCore.anyFrozen(
                tokens, openBraceIdx, closeBraceIdx + 1
            ) ) applyArrowAlignment(
                tokens, cases, closeBraceIdx, overrides
            );
        } // for i

        return render(tokens, overrides);
    }

    /**
     * One `case <label> ->` / `default ->` arrow-form case found directly (brace depth 0 relative
     * to the switch's own `{`) inside an arrow-labeled switch body
     */
    private static final class ArrowCase {

        final int     kwIdx;
        final int     bodyStartIdx; // First significant token after `->`
        final String  label;        // raw "case ..." / "default" text, whitespace-collapsed and trimmed
        final boolean blockBody;    // Body starts with `{`

        ArrowCase(
            final int     kwIdx,
            final int     bodyStartIdx,
            final String  label,
            final boolean blockBody
        )
        {
            this.kwIdx        = kwIdx;
            this.bodyStartIdx = bodyStartIdx;
            this.label        = label;
            this.blockBody    = blockBody;
        }

    } // class ArrowCase

    /**
     * Finds every direct (brace-depth-0) `case`/`default` label inside [openBraceIdx,
     * closeBraceIdx) and classifies it as arrow-form. Returns {@code null} the instant any label's
     * terminator turns out to be a top-level `:` ({@link #COLON_FOUND} -- the switch belongs to
     * {@code SwitchRule} instead) or can't be found at all ({@link #ARROW_NOT_FOUND} -- malformed).
     */
    private List<ArrowCase> findArrowCases(
        final List<Token> tokens,
        final int         openBraceIdx,
        final int         closeBraceIdx
    )
    {
        final List<ArrowCase> cases = new ArrayList<>();
              int             depth = 0;
        for(int i = openBraceIdx + 1; i < closeBraceIdx; ++i) {
            final Token t = tokens.get(i);
            if( isPunct(t, "{") ) {
                ++depth;
            }
            else if( isPunct(t, "}") ) {
                --depth;
            }
            else if( depth == 0 && t.type == TokenType.KEYWORD
                    && ( "case".equals(t.text) || "default".equals(t.text) ) ) {
                final int arrowIdx = findCaseArrowOrColon(tokens, i, closeBraceIdx);
                if(arrowIdx == COLON_FOUND || arrowIdx == ARROW_NOT_FOUND) return null;
                final int bodyStartIdx = nextSignificantIndex(tokens, arrowIdx);
                if(bodyStartIdx < 0) return null;
                final String label = collapseToOneLine(tokens, i, arrowIdx - 1);
                cases.add(
                    new ArrowCase( i, bodyStartIdx, label, isPunct( tokens.get(bodyStartIdx), "{" ) )
                );
                // Skip past this case's own arrow -- a multi-value label like
                // "case null, default ->" contains the keyword "default" *inside* the label
                // itself (before the arrow); without this, the outer scan would re-match that
                // embedded "default" as if it were its own case start, sharing the same arrow
                // and duplicating the label on every subsequent format pass
                i = arrowIdx;
            }
        } // for

        return cases;
    }

    /**
     * The top-level (paren/bracket-depth 0) `->` terminating a `case`/`default` label starting at
     * {@code kwIdx}, or {@link #COLON_FOUND}/{@link #ARROW_NOT_FOUND} -- see {@link #findArrowCases}
     */
    private int findCaseArrowOrColon(final List<Token> tokens, final int kwIdx, final int limit)
    {
        int depth = 0;
        for(int i = kwIdx + 1; i < limit; ++i) {
            final Token t = tokens.get(i);
                 if( isPunct(t, "(") || isPunct(t, "[") ) depth++;
            else if( isPunct(t, ")") || isPunct(t, "]") ) depth--;
            else if( depth == 0 && isOp(t, "->") ) return i;
            else if( depth == 0 && isOp(t, ":") ) return COLON_FOUND;
            else if( depth == 0 && ( isPunct(t, ";") || isPunct(t, "{") ) ) return ARROW_NOT_FOUND;
        } // for

        return ARROW_NOT_FOUND;
    }

    /**
     * Pads every case's label to the widest in {@code cases} (trailing-cell trick, same precedent
     * as {@code SwitchRule.applyInlineAlignment}'s label cell) and rewrites only the label span --
     * body content from {@code bodyStartIdx} onward is left completely untouched.
     */
    private void applyArrowAlignment(
        final List<Token>          tokens,
        final List<ArrowCase>      cases,
        final int                  closeBraceIdx,
        final Map<Integer, String> overrides
    )
    {
        final ColumnGrid grid = new ColumnGrid();
        for(final ArrowCase c : cases) {
            grid.addRow( new String[] {c.label + " ", ""} );
        }
        final List<String[]> padded = grid.flush();

        for( int i = 0; i < cases.size(); ++i ) {
            final ArrowCase c         = cases.get(i);
            final String    labelPart = padded.get(i)[0] + "-> ";
            // Predict the resulting single physical line's width before actually joining --
            // otherwise a case whose body was originally split onto its own line (fitting there)
            // can be joined here into a line that overflows lineLengthLimit, a decision
            // MiscRuleCurly.enforceCallLineBreaking (Phase 1, earlier in the pipeline) never gets a
            // chance to react to since it already ran against the pre-join layout. Left
            // unchecked, this flip-flops across reformats: fresh format joins+overflows, then
            // reformatting the already-joined (over-length) output lets enforceCallLineBreaking
            // finally see and break the too-long call -- not idempotent. Same
            // predict-before-committing posture as isSingleLineBody above.
            final int    bodyEndIdx  = i + 1 < cases.size() ? cases.get(
                i + 1
            ).kwIdx - 1 : closeBraceIdx - 1;
            final int    indent      = lineIndentWidth(tokens, c.kwIdx);
            final String bodyOneLine = collapseToOneLine(tokens, c.bodyStartIdx, bodyEndIdx);
            if( indent + labelPart.length() + bodyOneLine.length() > lineLengthLimit ) continue; // Leave this one case's label/body untouched, byte-for-byte
            overrides.put(c.kwIdx, labelPart);
            for(int k = c.kwIdx + 1; k < c.bodyStartIdx; ++k) overrides.put(k, "");
        } // for
    }

    /**
     * Total text length of the run of WHITESPACE tokens immediately preceding {@code idx} --
     * the leading indentation of {@code idx}'s own physical line (0 if {@code idx} isn't first
     * on its line, i.e. no WHITESPACE token directly precedes it).
     */
    private int lineIndentWidth(final List<Token> tokens, final int idx)
    {
        int width = 0;
        for( int i = idx - 1; i >= 0 && tokens.get(
            i
        ).type == TokenType.WHITESPACE; --i ) width += tokens.get(
            i
        ).text.length();

        return width;
    }

    /**
     * Renders {@code tokens} with each entry in {@code overrides} substituted for that token's
     * own text -- same minimal-touch rendering precedent as {@code SwitchRule.render}.
     */
    private String render(final List<Token> tokens, final Map<Integer, String> overrides)
    {
        final StringBuilder out = new StringBuilder();
        for( int i = 0; i < tokens.size(); ++i ) {
            final String override = overrides.get(i);
            out.append( override != null ? override : tokens.get(i).text );
        }

        return out.toString();
    }

    /**
     * Nearest preceding {@code class}/{@code interface} KEYWORD token, or -1 -- bounded-effort,
     * no depth tracking, same posture as the rest of this codebase's non-AST heuristics
     */
    private int prevClassOrInterfaceKeyword(final List<Token> tokens, final int fromExclusive)
    {
        for(int i = fromExclusive - 1; i >= 0; --i) {
            final Token t = tokens.get(i);
            if( t.type == TokenType.KEYWORD && ( "class".equals(
                t.text
            ) || "interface".equals(
                t.text
            ) ) ) return i;
        } // for

        return -1;
    }

    /** Nearest following PUNCT token matching {@code text}, or -1 */
    private int nextPunct(final List<Token> tokens, final int fromExclusive, final String text)
    {
        for( int i = fromExclusive + 1; i < tokens.size(); ++i ) {
            if( isPunct( tokens.get(i), text ) ) return i;
        }

        return -1;
    }

    /** Index of the first significant token on the same physical line as {@code idx} */
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

    /** Appends the literal text of {@code tokens[fromInclusive, toExclusive)} verbatim */
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
     * True iff {@code t} is an OP token consisting solely of `.`/`*` characters -- covers a plain
     * `.` separator, a plain `*` wildcard, and {@code TokenizerCurly}'s combined `.* ` multi-char
     * pointer-to-member OP token (see {@link #parseImportStatement}'s doc comment).
     */
    private boolean isPathOp(final Token t)
    {
        if( t == null || t.type != TokenType.OP || t.text.isEmpty() ) return false;
        for( int i = 0; i < t.text.length(); ++i ) {
            final char c = t.text.charAt(i);
            if(c != '.' && c != '*') return false;
        }

        return true;
    }


    /**
     * Line-leading whitespace of the physical line containing token {@code idx} -- "" if that
     * line has no leading whitespace (column-0 start)
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

} // class JavaSpecificRule
