/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * STYLE_TOOLING.md §3 (PowerShell) rule logic. A real tokenizer (character-level state machine) is
 * required per the style file: single/double-quoted strings (with {@code $(...)} / {@code ${...}}
 * interpolation inside expandable contexts), here-strings {@code @"..."@} / {@code @'...'@}, line
 * comments {@code #}, and block comments {@code <# ... #>} (nested) must all be recognized so none
 * of the six rules ever fires inside them.
 *
 * <p>Tokenizer-only milestone (STATE_TOOLING.md checklist): pass A classifies every character as
 * code ('C') or opaque ('O' -- string / here-string / comment content) and re-emits the input
 * byte-identical. Expandable string/here-string interiors stay opaque, but {@code $(...)}
 * subexpressions nested inside them re-enter code mode (so future brace-depth indentation can see
 * real scriptblock braces). {@code ${...}} variable names stay fully opaque. The six §3.x
 * transforms land in later checklist items on top of this classification.
 */
public final class PowerShellSpecificRule {

    private final int indentWidth;

    public PowerShellSpecificRule(final int indentWidth)
    {
        this.indentWidth = Math.max(1, indentWidth);
    }

    // ---- Pass A: char-level classification (identity emit for now) ----------------------------

    /**
     * Stack frame for the tokenizer. {@code type} values:
     * <ul>
     *   <li>{@code 'S'} -- single-quoted string {@code '...'}</li>
     *   <li>{@code 'D'} -- double-quoted string {@code "..."}</li>
     *   <li>{@code 'H'} -- expandable here-string {@code @"..."@}</li>
     *   <li>{@code 'h'} -- literal here-string {@code @'...'@}</li>
     *   <li>{@code '#'} -- line comment</li>
     *   <li>{@code 'B'} -- block comment {@code <# ... #>} (nest depth in {@link #parenDepth})</li>
     *   <li>{@code 'C'} -- subexpression {@code $(...)} (code interior; paren depth tracked)</li>
     *   <li>{@code 'V'} -- braced variable {@code ${...}} (opaque interior; brace depth tracked)</li>
     * </ul>
     */
    private static final class Frame {

        final char type;
        int        parenDepth; // reused: paren depth for 'C', nest depth for 'B', brace depth for 'V'

        Frame(final char type)
        {
            this.type = type;
        }

    } // class Frame

    private static final class PassAResult {

        String transformed;
        char[] kind; // per original character: 'C' / 'O'

    } // class PassAResult

    /** Accumulates characters and flushes on kind change (identity for both kinds, for now) */
    private static final class RunBuffer {

        private final StringBuilder out  = new StringBuilder();
        private final StringBuilder run  = new StringBuilder();
        private       char          kind = 'C';

        void emit(final char c, final char k)
        {
            if( run.length() > 0 && kind != k ) flush();
            kind = k;
            run.append(c);
        }

        void flush()
        {
            if( run.length() == 0 ) return;
            // Tokenizer-only: both 'C' and 'O' runs are emitted unchanged. Later §3.x token-level
            // rules (operator spacing, etc.) will transform 'C' runs here, matching BashSpecificRule.
            out.append(run);
            run.setLength(0);
        }

        String result()
        {
            flush();

            return out.toString();
        }

    } // class RunBuffer

    private PassAResult runPassA(final String content)
    {
        final char[]      kind  = new char[content.length()];
        final List<Frame> stack = new ArrayList<>();
        final RunBuffer   buf   = new RunBuffer();
              boolean     atLineStart = true;
              int         i     = 0;

        while( i < content.length() ) {
            final Frame top = stack.isEmpty() ? null : stack.get( stack.size() - 1 );
            final char  c   = content.charAt(i);

            // ---- line comment ---------------------------------------------------------------
            if( top != null && top.type == '#' ) {
                if(c == '\n') {
                    stack.remove( stack.size() - 1 );
                    kind[i] = 'C';
                    buf.emit(c, 'C');
                    atLineStart = true;
                    ++i;
                    continue;
                }
                kind[i] = 'O';
                buf.emit(c, 'O');
                atLineStart = false;
                ++i;
                continue;
            }

            // ---- block comment (nestable) ---------------------------------------------------
            if( top != null && top.type == 'B' ) {
                if( c == '<' && i + 1 < content.length() && content.charAt(i + 1) == '#' ) {
                    kind[i] = kind[i + 1] = 'O';
                    buf.emit('<', 'O'); buf.emit('#', 'O');
                    top.parenDepth++;
                    atLineStart = false;
                    i += 2;
                    continue;
                }
                if( c == '#' && i + 1 < content.length() && content.charAt(i + 1) == '>' ) {
                    kind[i] = kind[i + 1] = 'O';
                    buf.emit('#', 'O'); buf.emit('>', 'O');
                    if(top.parenDepth > 0) top.parenDepth--;
                    else stack.remove( stack.size() - 1 );
                    atLineStart = false;
                    i += 2;
                    continue;
                }
                kind[i] = 'O';
                buf.emit(c, 'O');
                if(c == '\n') atLineStart = true;
                else atLineStart = false;
                ++i;
                continue;
            }

            // ---- single-quoted string -------------------------------------------------------
            if( top != null && top.type == 'S' ) {
                // '' is the only escape inside single-quoted strings
                if( c == '\'' && i + 1 < content.length() && content.charAt(i + 1) == '\'' ) {
                    kind[i] = kind[i + 1] = 'O';
                    buf.emit('\'', 'O'); buf.emit('\'', 'O');
                    atLineStart = false;
                    i += 2;
                    continue;
                }
                kind[i] = 'O';
                buf.emit(c, 'O');
                if(c == '\'') stack.remove( stack.size() - 1 );
                if(c == '\n') atLineStart = true;
                else atLineStart = false;
                ++i;
                continue;
            }

            // ---- literal here-string @'...'@ ------------------------------------------------
            if( top != null && top.type == 'h' ) {
                if( atLineStart && c == '\'' && i + 1 < content.length() && content.charAt(i + 1) == '@' ) {
                    kind[i] = kind[i + 1] = 'O';
                    buf.emit('\'', 'O'); buf.emit('@', 'O');
                    stack.remove( stack.size() - 1 );
                    atLineStart = false;
                    i += 2;
                    continue;
                }
                kind[i] = 'O';
                buf.emit(c, 'O');
                if(c == '\n') atLineStart = true;
                else atLineStart = false;
                ++i;
                continue;
            }

            // ---- double-quoted string / expandable here-string ------------------------------
            if( top != null && ( top.type == 'D' || top.type == 'H' ) ) {
                // Expandable here-string terminator: "@ at column 0
                if( top.type == 'H' && atLineStart && c == '"' && i + 1 < content.length()
                        && content.charAt(i + 1) == '@' ) {
                    kind[i] = kind[i + 1] = 'O';
                    buf.emit('"', 'O'); buf.emit('@', 'O');
                    stack.remove( stack.size() - 1 );
                    atLineStart = false;
                    i += 2;
                    continue;
                }
                // Backtick escape (next char stays opaque, does not close the string)
                if( c == '`' && i + 1 < content.length() ) {
                    kind[i] = 'O';
                    buf.emit(c, 'O');
                    ++i;
                    kind[i] = 'O';
                    buf.emit( content.charAt(i), 'O' );
                    if( content.charAt(i) == '\n' ) atLineStart = true;
                    else atLineStart = false;
                    ++i;
                    continue;
                }
                // Subexpression $(...) -- re-enter code mode so future brace rules see real braces
                if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '(' ) {
                    stack.add( new Frame('C') );
                    kind[i] = kind[i + 1] = 'C';
                    buf.emit('$', 'C'); buf.emit('(', 'C');
                    atLineStart = false;
                    i += 2;
                    continue;
                }
                // Braced variable ${...} -- stay opaque (name, not code)
                if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '{' ) {
                    stack.add( new Frame('V') );
                    kind[i] = kind[i + 1] = 'O';
                    buf.emit('$', 'O'); buf.emit('{', 'O');
                    atLineStart = false;
                    i += 2;
                    continue;
                }
                kind[i] = 'O';
                buf.emit(c, 'O');
                if( top.type == 'D' && c == '"' ) stack.remove( stack.size() - 1 );
                if(c == '\n') atLineStart = true;
                else atLineStart = false;
                ++i;
                continue;
            }

            // ---- braced variable ${...} (opaque) --------------------------------------------
            if( top != null && top.type == 'V' ) {
                if(c == '{') {
                    top.parenDepth++;
                    kind[i] = 'O';
                    buf.emit(c, 'O');
                    atLineStart = false;
                    ++i;
                    continue;
                }
                if(c == '}') {
                    if(top.parenDepth > 0) {
                        top.parenDepth--;
                        kind[i] = 'O';
                        buf.emit(c, 'O');
                    } else {
                        stack.remove( stack.size() - 1 );
                        kind[i] = 'O';
                        buf.emit(c, 'O');
                    }
                    atLineStart = false;
                    ++i;
                    continue;
                }
                kind[i] = 'O';
                buf.emit(c, 'O');
                if(c == '\n') atLineStart = true;
                else atLineStart = false;
                ++i;
                continue;
            }

            // ---- subexpression $(...) interior = code (falls through to NORMAL rules) -------
            // top == null OR top.type == 'C': both accept the same NORMAL starters. For 'C', a
            // closing ')' at depth 0 pops the frame (handled after NORMAL starters so quotes /
            // comments / here-strings / nested $( still win); nested '(' bumps depth.

            // ==== NORMAL / subexpression-code context ========================================

            // Backtick escape: next character is literal code (so `" does not open a string)
            if( c == '`' && i + 1 < content.length() ) {
                kind[i] = 'C';
                buf.emit(c, 'C');
                ++i;
                kind[i] = 'C';
                buf.emit( content.charAt(i), 'C' );
                if( content.charAt(i) == '\n' ) atLineStart = true;
                else atLineStart = false;
                ++i;
                continue;
            }

            // Here-string open: @" or @' followed by optional WS then newline
            if( c == '@' && i + 1 < content.length()
                    && ( content.charAt(i + 1) == '"' || content.charAt(i + 1) == '\'' )
                    && isHereStringOpen(content, i) ) {
                final char q = content.charAt(i + 1);
                stack.add( new Frame(q == '"' ? 'H' : 'h') );
                // Emit @, quote, optional WS, and the opening newline all as opaque
                int p = i;
                while( p < content.length() ) {
                    final char ch = content.charAt(p);
                    kind[p] = 'O';
                    buf.emit(ch, 'O');
                    ++p;
                    if(ch == '\n') break;
                }
                atLineStart = true; // body begins at start of next line (we consumed the newline)
                i = p;
                continue;
            }

            // Block comment
            if( c == '<' && i + 1 < content.length() && content.charAt(i + 1) == '#' ) {
                stack.add( new Frame('B') );
                kind[i] = kind[i + 1] = 'O';
                buf.emit('<', 'O'); buf.emit('#', 'O');
                atLineStart = false;
                i += 2;
                continue;
            }

            // Line comment -- '#' always starts a comment outside strings/here-strings in PS
            if(c == '#') {
                stack.add( new Frame('#') );
                kind[i] = 'O';
                buf.emit(c, 'O');
                atLineStart = false;
                ++i;
                continue;
            }

            // Single / double quoted strings
            if(c == '\'') {
                stack.add( new Frame('S') );
                kind[i] = 'O';
                buf.emit(c, 'O');
                atLineStart = false;
                ++i;
                continue;
            }
            if(c == '"') {
                stack.add( new Frame('D') );
                kind[i] = 'O';
                buf.emit(c, 'O');
                atLineStart = false;
                ++i;
                continue;
            }

            // Subexpression $(...) at code level
            if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '(' ) {
                stack.add( new Frame('C') );
                kind[i] = kind[i + 1] = 'C';
                buf.emit('$', 'C'); buf.emit('(', 'C');
                atLineStart = false;
                i += 2;
                continue;
            }

            // Braced variable ${...} at code level (opaque name)
            if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '{' ) {
                stack.add( new Frame('V') );
                kind[i] = kind[i + 1] = 'O';
                buf.emit('$', 'O'); buf.emit('{', 'O');
                atLineStart = false;
                i += 2;
                continue;
            }

            // Bare paren inside subexpression was handled above; at top-level, '(' / ')' are code.
            // Re-check subexpression paren when we fell through from quote-priority path with '('/')'.
            if( top != null && top.type == 'C' ) {
                if(c == '(') {
                    top.parenDepth++;
                    kind[i] = 'C';
                    buf.emit(c, 'C');
                    atLineStart = false;
                    ++i;
                    continue;
                }
                if(c == ')') {
                    if(top.parenDepth > 0) {
                        top.parenDepth--;
                        kind[i] = 'C';
                        buf.emit(c, 'C');
                    } else {
                        stack.remove( stack.size() - 1 );
                        kind[i] = 'C';
                        buf.emit(c, 'C');
                    }
                    atLineStart = false;
                    ++i;
                    continue;
                }
            }

            kind[i] = 'C';
            buf.emit(c, 'C');
            if(c == '\n') atLineStart = true;
            else atLineStart = false;
            ++i;
        } // while

        final PassAResult result = new PassAResult();
        result.transformed = buf.result();
        result.kind         = kind;

        return result;
    }

    /**
     * {@code @"}/{@code @'} opens a here-string only when the quote is followed by optional
     * spaces/tabs and then a newline (PowerShell requires the opening quote to end its line).
     */
    private static boolean isHereStringOpen(final String content, final int atIdx)
    {
        int p = atIdx + 2; // past @ and quote
        while( p < content.length() && ( content.charAt(p) == ' ' || content.charAt(p) == '\t' ) ) ++p;
        return p < content.length() && content.charAt(p) == '\n';
    }

    /**
     * A line is "pure" (eligible for future structural §3.x rules) if its first non-whitespace
     * character is real code -- excludes here-string body lines and full-comment lines without
     * over-rejecting normal code lines that merely contain a quoted string later on.
     */
    static boolean[] computeLinePurity(final String content, final char[] kind, final int lineCount)
    {
        final boolean[] pure  = new boolean[lineCount];
        int             line  = 0;
        boolean         found = false;
        for( int i = 0; i < kind.length && line < lineCount; ++i ) {
            final char c = content.charAt(i);
            if(c == '\n') {
                if(!found) pure[line] = true;
                ++line;
                found = false;
                continue;
            }
            if( !found && c != ' ' && c != '\t' && c != '\r' ) {
                pure[line] = kind[i] == 'C';
                found      = true;
            }
        } // for
        if( line < lineCount && !found ) pure[line] = true;

        return pure;
    }

    /**
     * Package-visible classification entry point for smoke tests. Returns a string the same length
     * as {@code content} whose characters are {@code 'C'} (code) or {@code 'O'} (opaque).
     */
    String classifyKinds(final String content)
    {
        return new String( runPassA(content).kind );
    }

    public String format(final String content)
    {
        // Tokenizer-only milestone: classify (so the kind map / purity helpers are exercised and
        // ready for §3.1+) but apply no transforms yet -- output is byte-identical to input.
        final PassAResult passA = runPassA(content);
        return passA.transformed;
    }

} // class PowerShellSpecificRule
