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
 * <p>Pass A classifies every character as code ('C') or opaque ('O'). Expandable string/here-string
 * interiors stay opaque, but {@code $(...)} subexpressions nested inside them re-enter code mode
 * (so brace-depth indentation can see real scriptblock braces). {@code ${...}} variable names stay
 * fully opaque. Structural passes re-tokenize as needed after transforms that reshuffle text.
 *
 * <p>Landed rules: §3.1 naive brace-depth indentation; §3.2 operator spacing + {@code =}
 * alignment. Remaining §3.3–§3.6 land in later checklist items.
 */
public final class PowerShellSpecificRule {

    private final int indentWidth;

    public PowerShellSpecificRule(final int indentWidth)
    {
        this.indentWidth = Math.max(1, indentWidth);
    }

    // ---- Pass A: char-level classification ----------------------------------------------------

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
            // Both 'C' and 'O' runs are emitted unchanged here. Token-level §3.x transforms
            // (operator spacing, brace spacing) plug in on 'C' flushes in later checklist items.
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
     * A line is "pure" (eligible for structural §3.x rules) if its first non-whitespace character
     * is real code -- excludes here-string body lines and full-comment lines without over-rejecting
     * normal code lines that merely contain a quoted string later on.
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

    // ---- Line splitting / joining -------------------------------------------------------------

    private static final class Lines {

        final List<String> lines;
        final boolean      endsWithNewline;

        Lines(final String content)
        {
            this.endsWithNewline = content.endsWith("\n");
            final String[] raw   = content.split("\n", -1);
            this.lines           = new ArrayList<>( java.util.Arrays.asList(raw) );
            if( endsWithNewline && !lines.isEmpty() ) lines.remove( lines.size() - 1 );
        }

        String join()
        {
            final StringBuilder sb = new StringBuilder();
            for( int i = 0; i < lines.size(); ++i ) {
                sb.append( lines.get(i) );
                if( i + 1 < lines.size() || endsWithNewline ) sb.append('\n');
            }

            return sb.toString();
        }

    } // class Lines

    private static String leadingWhitespace(final String line)
    {
        int i = 0;
        while( i < line.length() && ( line.charAt(i) == ' ' || line.charAt(i) == '\t' ) ) ++i;

        return line.substring(0, i);
    }

    private String indent(final int depth)
    {
        return repeatChar( ' ', Math.max(0, depth) * indentWidth );
    }

    private static String repeatChar(final char c, final int count)
    {
        final StringBuilder sb = new StringBuilder( Math.max(0, count) );
        for( int i = 0; i < count; ++i ) sb.append(c);

        return sb.toString();
    }

    /**
     * Per-line kind slices aligned with {@link Lines#lines} (newline characters themselves are
     * omitted; each slice is exactly the line's character kinds).
     */
    private static char[][] lineKinds(final String content, final char[] kind, final int lineCount)
    {
        final char[][] out  = new char[lineCount][];
        int            line = 0;
        int            start = 0;
        for( int i = 0; i <= content.length() && line < lineCount; ++i ) {
            if( i == content.length() || content.charAt(i) == '\n' ) {
                final int len = i - start;
                out[line] = new char[len];
                if(len > 0) System.arraycopy(kind, start, out[line], 0, len);
                ++line;
                start = i + 1;
            }
        } // for

        return out;
    }

    // ---- §3.1 Naive brace-depth indentation ---------------------------------------------------

    /**
     * Reindents pure-code lines by a running brace-depth count over code-kind {@code '{'}/{@code '}'}
     * only (strings/here-strings/comments never contribute). Here-string bodies and full-comment
     * lines (non-pure) keep their original leading whitespace. Leading {@code '}'} on a line indent
     * at {@code depth - leadingCloses} so a lone closer sits at the depth of its opener.
     */
    private String applyBraceIndent(final String content)
    {
        final PassAResult  passA  = runPassA(content);
        final Lines        lines  = new Lines(passA.transformed);
        final boolean[]    pure   = computeLinePurity(passA.transformed, passA.kind, lines.lines.size());
        final char[][]     kinds  = lineKinds(passA.transformed, passA.kind, lines.lines.size());
        final List<String> out    = new ArrayList<>( lines.lines.size() );
              int          depth  = 0;

        for( int li = 0; li < lines.lines.size(); ++li ) {
            final String line    = lines.lines.get(li);
            final String trimmed = line.trim();
            final char[] lk      = kinds[li];

            if( trimmed.isEmpty() ) {
                out.add("");
                continue;
            }

            final int leadingCloses = countLeadingCloses(trimmed, line, lk);
            final int open          = countCodeChar(trimmed, line, lk, '{');
            final int close         = countCodeChar(trimmed, line, lk, '}');

            if( pure[li] ) {
                final int printDepth = Math.max(0, depth - leadingCloses);
                out.add( indent(printDepth) + trimmed );
            } else {
                // Non-pure (here-string body, full-line comment, etc.): leave byte-identical.
                out.add(line);
            }

            depth = Math.max(0, depth + open - close);
        } // for

        lines.lines.clear();
        lines.lines.addAll(out);

        return lines.join();
    }

    /**
     * Count code-kind {@code '}'} at the start of {@code trimmed} before any other non-whitespace
     * code character. {@code trimmed} is {@code line.trim()}; kinds are indexed into the original
     * {@code line} (so leading whitespace on {@code line} must be skipped when mapping).
     */
    private static int countLeadingCloses(final String trimmed, final String line, final char[] lineKind)
    {
        final int base = leadingWhitespace(line).length();
        int       n    = 0;
        for( int i = 0; i < trimmed.length(); ++i ) {
            final char c = trimmed.charAt(i);
            if(c == ' ' || c == '\t' || c == '\r') continue;
            final int ki = base + i;
            // trimmed is line without leading/trailing ws -- map carefully:
            // base + index into the untrimmed middle. Since trimmed strips trailing too, indices
            // into line are: first non-ws of line + i, for i scanning trimmed which has no lead ws.
            // Actually trimmed = line.trim(), so line[base + i] == trimmed[i] for i in [0, trimmed.length)
            // only if there's no difference from trailing strip affecting... yes for i < trimmed.length(),
            // line.charAt(base + i) == trimmed.charAt(i).
            if( ki < 0 || ki >= lineKind.length ) break;
            if( lineKind[ki] != 'C' ) {
                // opaque non-ws at the start (e.g. comment line) -- not a leading close
                break;
            }
            if(c == '}') n++;
            else break;
        } // for

        return n;
    }

    private static int countCodeChar(
        final String trimmed, final String line, final char[] lineKind, final char target
    )
    {
        final int base = leadingWhitespace(line).length();
        int       n    = 0;
        for( int i = 0; i < trimmed.length(); ++i ) {
            final int ki = base + i;
            if( ki >= 0 && ki < lineKind.length && lineKind[ki] == 'C' && trimmed.charAt(i) == target ) n++;
        }

        return n;
    }

    // ---- §3.2 Operator spacing + `=` alignment ------------------------------------------------

    /**
     * Kind-aware operator spacing over code characters only: spaces around {@code =}/{@code +=}/
     * {@code -=}/{@code *=}/{@code /=}/{@code %=} and around binary {@code +}/{@code *}/{@code /}/
     * {@code %} (Bash-style conservative left-hand check). Bare {@code -} is left alone so
     * PowerShell {@code -gt}/{@code -eq}/{@code -Path} tokens are never split.
     */
    private String applyOperatorSpacing(final String content)
    {
        final PassAResult passA = runPassA(content);
        final String      s     = passA.transformed;
        final char[]      kind  = passA.kind;
        final StringBuilder sb  = new StringBuilder( s.length() + 16 );

        for( int i = 0; i < s.length(); ++i ) {
            if( kind[i] != 'C' ) {
                sb.append( s.charAt(i) );
                continue;
            }
            final char c = s.charAt(i);

            // Compound assignment += -= *= /= %=
            if( ( c == '+' || c == '-' || c == '*' || c == '/' || c == '%' )
                    && i + 1 < s.length() && s.charAt(i + 1) == '='
                    && kind[i + 1] == 'C' ) {
                stripTrailingSpaces(sb);
                sb.append(' ').append(c).append('=').append(' ');
                i = skipSpaces(s, i + 2) - 1;
                continue;
            }

            // Plain assignment =
            if(c == '=') {
                stripTrailingSpaces(sb);
                sb.append(" = ");
                i = skipSpaces(s, i + 1) - 1;
                continue;
            }

            // Binary + * / %  (not -, see method javadoc)
            if( ( c == '+' || c == '*' || c == '/' || c == '%' )
                    && isBinaryLeft(sb)
                    && !( i + 1 < s.length() && s.charAt(i + 1) == c ) /* not ++ // etc. */ ) {
                stripTrailingSpaces(sb);
                sb.append(' ').append(c).append(' ');
                i = skipSpaces(s, i + 1) - 1;
                continue;
            }

            sb.append(c);
        } // for

        return sb.toString();
    }

    private static void stripTrailingSpaces(final StringBuilder sb)
    {
        while( sb.length() > 0 ) {
            final char p = sb.charAt( sb.length() - 1 );
            if(p == ' ' || p == '\t') sb.setLength( sb.length() - 1 );
            else break;
        }
    }

    private static int skipSpaces(final String s, final int from)
    {
        int j = from;
        while( j < s.length() && ( s.charAt(j) == ' ' || s.charAt(j) == '\t' ) ) ++j;

        return j;
    }

    /** True when the builder's last non-space char looks like an expression end (binary-op left). */
    private static boolean isBinaryLeft(final StringBuilder sb)
    {
        int i = sb.length() - 1;
        while( i >= 0 && ( sb.charAt(i) == ' ' || sb.charAt(i) == '\t' ) ) --i;
        if(i < 0) return false;
        final char p = sb.charAt(i);

        return Character.isLetterOrDigit(p) || p == '_' || p == ')' || p == ']' || p == '}' || p == '$'
                || p == '"' || p == '\'';
    }

    private static final class AssignItem {

        String indent;
        String lhs;
        String op;  // "=", "+=", ...
        String rhs;

    } // class AssignItem

    /**
     * Block-scoped {@code =} alignment (RDD_KEY_254): consecutive pure assignment lines form a
     * group broken by a blank line or any non-assignment line. Aligns the operator column to the
     * longest LHS in the group. Assignment detection uses the first code-kind {@code =}/{@code +=}
     * /etc. at brace/paren/bracket depth 0 on the line (so {@code =} inside strings or nested
     * scriptblocks does not count).
     */
    private String applyAssignAlignment(final String content)
    {
        final PassAResult  passA = runPassA(content);
        final Lines        lines = new Lines(passA.transformed);
        final boolean[]    pure  = computeLinePurity(passA.transformed, passA.kind, lines.lines.size());
        final char[][]     kinds = lineKinds(passA.transformed, passA.kind, lines.lines.size());
        final List<String> out   = new ArrayList<>();
        final List<AssignItem> group = new ArrayList<>();

        for( int li = 0; li < lines.lines.size(); ++li ) {
            final String line = lines.lines.get(li);
            if( line.trim().isEmpty() ) {
                flushAssignGroup(out, group);
                out.add("");
                continue;
            }
            final AssignItem item = pure[li] ? parseAssign(line, kinds[li]) : null;
            if(item != null) {
                group.add(item);
                continue;
            }
            flushAssignGroup(out, group);
            out.add(line);
        } // for
        flushAssignGroup(out, group);

        lines.lines.clear();
        lines.lines.addAll(out);

        return lines.join();
    }

    private static AssignItem parseAssign(final String line, final char[] kind)
    {
        final int lead = leadingWhitespace(line).length();
        int depth = 0;
        for( int i = lead; i < line.length(); ++i ) {
            if( i < kind.length && kind[i] != 'C' ) continue;
            final char c = line.charAt(i);
            if(c == '(' || c == '[' || c == '{') { depth++; continue; }
            if(c == ')' || c == ']' || c == '}') { if(depth > 0) depth--; continue; }
            if(depth != 0) continue;

            // compound
            if( i + 1 < line.length() && line.charAt(i + 1) == '='
                    && ( c == '+' || c == '-' || c == '*' || c == '/' || c == '%' )
                    && ( i + 1 >= kind.length || kind[i + 1] == 'C' ) ) {
                return makeAssign(line, lead, i, 2);
            }
            if(c == '=') {
                return makeAssign(line, lead, i, 1);
            }
        } // for

        return null;
    }

    private static AssignItem makeAssign(final String line, final int lead, final int opAt, final int opLen)
    {
        final AssignItem item = new AssignItem();
        item.indent = line.substring(0, lead);
        item.lhs    = line.substring(lead, opAt).replaceAll("[ \\t]+$", "");
        if( item.lhs.isEmpty() ) return null; // e.g. line starting with =
        item.op     = line.substring(opAt, opAt + opLen);
        item.rhs    = line.substring(opAt + opLen).replaceAll("^[ \\t]+", "");

        return item;
    }

    private static void flushAssignGroup(final List<String> out, final List<AssignItem> group)
    {
        if( group.isEmpty() ) return;
        int maxLhs = 0;
        for( final AssignItem it : group ) maxLhs = Math.max( maxLhs, it.lhs.length() );
        for( final AssignItem it : group ) {
            final int pad = maxLhs - it.lhs.length();
            out.add( it.indent + it.lhs + repeatChar(' ', pad) + " " + it.op + " " + it.rhs );
        }
        group.clear();
    }

    public String format(final String content)
    {
        String s = content;
        s = applyOperatorSpacing(s); // §3.2 spacing (before indent so alignment sees spaced ops)
        s = applyBraceIndent(s);     // §3.1
        s = applyAssignAlignment(s); // §3.2 alignment (after indent so indent is preserved)

        return s;
    }

} // class PowerShellSpecificRule
