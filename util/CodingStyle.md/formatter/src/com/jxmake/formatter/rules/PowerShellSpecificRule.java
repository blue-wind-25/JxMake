/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * <p>Implements STYLE_TOOLING.md §3.1–§3.6: brace-depth indent; operator/{@code =} alignment;
 * pipeline split/right-align; hashtable spacing; switch keyword-paren spacing + arm {@code \{}
 * alignment; and {@code \{}/{@code \}} spacing (RDD_KEY_258, including single-line scriptblocks).
 *
 * <p>Line comments ({@code #...}) also get the optional §0 ad hoc normalization (RDD_KEY_261):
 * first-letter capitalization and sole-trailing-period stripping via
 * {@link ToolingCommentNormalizer}, plain (no word-exception list -- PowerShell's own keyword/
 * cmdlet surface is too broad to usefully curate one, per RDD_KEY_261). Block comments
 * ({@code <# ... #>}) are left untouched -- out of scope.
 */
public final class PowerShellSpecificRule {

    private final int     indentWidth;
    private final boolean normalizeCommentStartCase;
    private final boolean normalizeCommentEndPeriod;
    // `normalize-comment-start-case-multiline` -- default off, set post-construction (matches the
    // curly family's MiscRuleCore#setNormalizeCommentMultiSentenceCase pattern)
    private boolean normalizeCommentMultiSentenceCase = false;

    public PowerShellSpecificRule(
        final int     indentWidth,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod
    )
    {
        this.indentWidth               = Math.max(1, indentWidth);
        this.normalizeCommentStartCase = normalizeCommentStartCase;
        this.normalizeCommentEndPeriod = normalizeCommentEndPeriod;
    }

    public void setNormalizeCommentMultiSentenceCase(final boolean value)
    {
        this.normalizeCommentMultiSentenceCase = value;
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

        final char    type;
              int     parenDepth; // Reused: paren depth for 'C', nest depth for 'B', brace depth for 'V'
              boolean standalone; // '#'-frames only: true iff nothing but whitespace precedes it on its line
              int     lineNo;     // '#'-frames only: 0-based source line index this comment starts on

        Frame(final char type)
        {
            this.type = type;
        }

    } // class Frame

    private static final class PassAResult {

        String transformed;
        char[] kind;        // per original character: 'C' / 'O'

    } // class PassAResult

    /** Accumulates characters and flushes on kind change (identity for both kinds, for now) */
    private static final class RunBuffer {

        private final StringBuilder out     = new StringBuilder();
        private final StringBuilder kindOut = new StringBuilder(); // Per emitted OUTPUT character, aligned with `out`
        private final StringBuilder run     = new StringBuilder();
        private       char          kind    = 'C';

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
            for( int i = 0; i < run.length(); ++i ) kindOut.append(kind);
            run.setLength(0);
        }

        String result()
        {
            flush();

            return out.toString();
        }

        /** Per-output-character kind, aligned with {@link #result()} -- call only after {@link #result()} */
        String kindResult()
        {
            return kindOut.toString();
        }

    } // class RunBuffer

    private PassAResult runPassA(final String content)
    {
        final List<Frame>   stack       = new ArrayList<>();
        final RunBuffer     buf         = new RunBuffer();
        final StringBuilder commentBody = new StringBuilder();
        final ToolingCommentNormalizer.ChainCollector chainCollector =
            new ToolingCommentNormalizer.ChainCollector("\u0007CHAIN", "\u0007");
                boolean atLineStart = true;
                int     i           = 0;

        while( i < content.length() ) {
            final Frame top = stack.isEmpty() ? null : stack.get( stack.size() - 1 );
            final char  c   = content.charAt(i);

            // ---- line comment ---------------------------------------------------------------
            if(top != null && top.type == '#') {
                if(c == '\n') {
                    stack.remove( stack.size() - 1 );
                    if(top.standalone) {
                        final String placeholder = chainCollector.defer(
                            commentBody.toString(), top.lineNo
                        );
                        for( int p = 0; p < placeholder.length(); ++p ) buf.emit(
                            placeholder.charAt(p), 'O'
                        );
                    } // if
                    else {
                        emitNormalizedComment( buf, commentBody.toString() );
                    }
                    commentBody.setLength(0);
                    buf.emit(c, 'C');
                    atLineStart = true;
                    ++i;
                    continue;
                } // if
                commentBody.append(c);
                atLineStart = false;
                ++i;
                continue;
            } // if

            // ---- block comment (nestable) ---------------------------------------------------
            if(top != null && top.type == 'B') {
                if( c == '<' && i + 1 < content.length() && content.charAt(i + 1) == '#' ) {
                    buf.emit('<', 'O'); buf.emit('#', 'O');
                    top.parenDepth++;
                    atLineStart  = false;
                    i           += 2;
                    continue;
                } // if
                if( c == '#' && i + 1 < content.length() && content.charAt(i + 1) == '>' ) {
                    buf.emit('#', 'O'); buf.emit('>', 'O');
                    if(top.parenDepth > 0) top.parenDepth--;
                    else                   stack.remove( stack.size() - 1 );
                    atLineStart  = false;
                    i           += 2;
                    continue;
                } // if
                buf.emit(c, 'O');
                if(c == '\n') atLineStart = true;
                else          atLineStart = false;
                ++i;
                continue;
            } // if

            // ---- single-quoted string -------------------------------------------------------
            if(top != null && top.type == 'S') {
                // '' is the only escape inside single-quoted strings
                if( c == '\'' && i + 1 < content.length() && content.charAt(i + 1) == '\'' ) {
                    buf.emit('\'', 'O'); buf.emit('\'', 'O');
                    atLineStart  = false;
                    i           += 2;
                    continue;
                } // if
                buf.emit(c, 'O');
                if(c == '\'') stack.remove( stack.size() - 1 );
                if(c == '\n') atLineStart = true;
                else          atLineStart = false;
                ++i;
                continue;
            } // if

            // ---- literal here-string @'...'@ ------------------------------------------------
            if(top != null && top.type == 'h') {
                if( atLineStart && c == '\'' && i + 1 < content.length() && content.charAt(
                    i + 1
                ) == '@' ) {
                    buf.emit('\'', 'O'); buf.emit('@', 'O');
                    stack.remove( stack.size() - 1 );
                    atLineStart  = false;
                    i           += 2;
                    continue;
                } // if
                buf.emit(c, 'O');
                if(c == '\n') atLineStart = true;
                else          atLineStart = false;
                ++i;
                continue;
            } // if

            // ---- double-quoted string / expandable here-string ------------------------------
            if( top != null && (top.type == 'D' || top.type == 'H') ) {
                // Expandable here-string terminator: "@ at column 0
                if( top.type == 'H' && atLineStart && c == '"' && i + 1 < content.length()
                        && content.charAt(i + 1) == '@' ) {
                    buf.emit('"', 'O'); buf.emit('@', 'O');
                    stack.remove( stack.size() - 1 );
                    atLineStart  = false;
                    i           += 2;
                    continue;
                } // if
                // Backtick escape (next char stays opaque, does not close the string)
                if( c == '`' && i + 1 < content.length() ) {
                    buf.emit(c, 'O');
                    ++i;
                    buf.emit( content.charAt(i), 'O' );
                    if( content.charAt(i) == '\n' ) atLineStart = true;
                    else                            atLineStart = false;
                    ++i;
                    continue;
                } // if
                // Subexpression $(...) -- re-enter code mode so future brace rules see real braces
                if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '(' ) {
                    stack.add( new Frame('C') );
                    buf.emit('$', 'C'); buf.emit('(', 'C');
                    atLineStart  = false;
                    i           += 2;
                    continue;
                } // if
                // Braced variable ${...} -- stay opaque (name, not code)
                if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '{' ) {
                    stack.add( new Frame('V') );
                    buf.emit('$', 'O'); buf.emit('{', 'O');
                    atLineStart  = false;
                    i           += 2;
                    continue;
                } // if
                buf.emit(c, 'O');
                if(top.type == 'D' && c == '"') stack.remove( stack.size() - 1 );
                if(c == '\n') atLineStart = true;
                else          atLineStart = false;
                ++i;
                continue;
            } // if

            // ---- braced variable ${...} (opaque) --------------------------------------------
            if(top != null && top.type == 'V') {
                if(c == '{') {
                    top.parenDepth++;
                    buf.emit(c, 'O');
                    atLineStart = false;
                    ++i;
                    continue;
                } // if
                if(c == '}') {
                    if(top.parenDepth > 0) {
                        top.parenDepth--;
                        buf.emit(c, 'O');
                    }
                    else {
                        stack.remove( stack.size() - 1 );
                        buf.emit(c, 'O');
                    }
                    atLineStart = false;
                    ++i;
                    continue;
                } // if
                buf.emit(c, 'O');
                if(c == '\n') atLineStart = true;
                else          atLineStart = false;
                ++i;
                continue;
            } // if

            // ---- subexpression $(...) interior = code (falls through to NORMAL rules) -------
            // top == null OR top.type == 'C': both accept the same NORMAL starters. For 'C', a
            // closing ')' at depth 0 pops the frame (handled after NORMAL starters so quotes /
            // comments / here-strings / nested $( still win); nested '(' bumps depth.

            // ==== NORMAL / subexpression-code context ========================================

            // Backtick escape: next character is literal code (so `" does not open a string)
            if( c == '`' && i + 1 < content.length() ) {
                buf.emit(c, 'C');
                ++i;
                buf.emit( content.charAt(i), 'C' );
                if( content.charAt(i) == '\n' ) atLineStart = true;
                else                            atLineStart = false;
                ++i;
                continue;
            } // if

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
                    buf.emit(ch, 'O');
                    ++p;
                    if(ch == '\n') break;
                } // while
                atLineStart = true; // Body begins at start of next line (we consumed the newline)
                i           = p;
                continue;
            } // if

            // Block comment
            if( c == '<' && i + 1 < content.length() && content.charAt(i + 1) == '#' ) {
                stack.add( new Frame('B') );
                buf.emit('<', 'O'); buf.emit('#', 'O');
                atLineStart  = false;
                i           += 2;
                continue;
            } // if

            // Line comment -- '#' always starts a comment outside strings/here-strings in PS
            if(c == '#') {
                final Frame f         = new Frame('#');
                      int   lineStart = i;
                while( lineStart > 0 && content.charAt(lineStart - 1) != '\n' ) --lineStart;
                f.standalone = content.substring(lineStart, i).trim().isEmpty();
                int ln = 0;
                for(int p = 0; p < lineStart; ++p) if( content.charAt(p) == '\n' ) ++ln;
                f.lineNo = ln;
                stack.add(f);
                buf.emit(c, 'O');
                atLineStart = false;
                ++i;
                continue;
            } // if

            // Single / double quoted strings
            if(c == '\'') {
                stack.add( new Frame('S') );
                buf.emit(c, 'O');
                atLineStart = false;
                ++i;
                continue;
            } // if
            if(c == '"') {
                stack.add( new Frame('D') );
                buf.emit(c, 'O');
                atLineStart = false;
                ++i;
                continue;
            } // if

            // Subexpression $(...) at code level
            if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '(' ) {
                stack.add( new Frame('C') );
                buf.emit('$', 'C'); buf.emit('(', 'C');
                atLineStart  = false;
                i           += 2;
                continue;
            } // if

            // Braced variable ${...} at code level (opaque name)
            if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '{' ) {
                stack.add( new Frame('V') );
                buf.emit('$', 'O'); buf.emit('{', 'O');
                atLineStart  = false;
                i           += 2;
                continue;
            } // if

            // Bare paren inside subexpression was handled above; at top-level, '(' / ')' are code.
            // Re-check subexpression paren when we fell through from quote-priority path with '('/')'.
            if(top != null && top.type == 'C') {
                if(c == '(') {
                    top.parenDepth++;
                    buf.emit(c, 'C');
                    atLineStart = false;
                    ++i;
                    continue;
                } // if
                if(c == ')') {
                    if(top.parenDepth > 0) {
                        top.parenDepth--;
                        buf.emit(c, 'C');
                    }
                    else {
                        stack.remove( stack.size() - 1 );
                        buf.emit(c, 'C');
                    }
                    atLineStart = false;
                    ++i;
                    continue;
                } // if
            } // if

            buf.emit(c, 'C');
            if(c == '\n') atLineStart = true;
            else          atLineStart = false;
            ++i;
        } // while

        if( commentBody.length() > 0 ) {
            final Frame top = stack.isEmpty() ? null : stack.get( stack.size() - 1 );
            if(top != null && top.type == '#' && top.standalone) {
                final String placeholder = chainCollector.defer(
                    commentBody.toString(), top.lineNo
                );
                for( int p = 0; p < placeholder.length(); ++p ) buf.emit(
                    placeholder.charAt(p), 'O'
                );
            } // if
            else {
                emitNormalizedComment( buf, commentBody.toString() );
            }
        } // if

        final String preResolveTransformed = buf.result();
        final String preResolveKind        = buf.kindResult();
        final String transformed           = chainCollector.resolve(
            preResolveTransformed,
            normalizeCommentStartCase,
            normalizeCommentEndPeriod,
            null,
            normalizeCommentMultiSentenceCase
        );
        // Must run after resolve() above -- reuses its per-entry resolvedLength to keep the kind
        // string aligned with `transformed` (RDD: kind[] was previously indexed against the original
        // `content` string, diverging from `transformed` once a standalone comment's placeholder was
        // substituted for a different-length final comment text -- every downstream consumer already
        // indexes kind[] against `transformed`, so kind must be built aligned to it instead)
        final String resolvedKind = chainCollector.resolveKind(
            preResolveTransformed, preResolveKind
        );

        final PassAResult result = new PassAResult();
        result.transformed = transformed;
        result.kind        = resolvedKind.toCharArray();

        return result;
    }

    /** Normalizes a `#` line comment's body (text after the `#`) and re-emits it as opaque ('O') characters */
    private void emitNormalizedComment(final RunBuffer buf, final String body)
    {
        final String normalized = ToolingCommentNormalizer.normalize(
            body,
            normalizeCommentStartCase,
            normalizeCommentEndPeriod,
            null,
            normalizeCommentMultiSentenceCase
        );
        for( int i = 0; i < normalized.length(); ++i ) buf.emit( normalized.charAt(i), 'O' );
    }

    /**
     * {@code @"}/{@code @'} opens a here-string only when the quote is followed by optional
     * spaces/tabs and then a newline (PowerShell requires the opening quote to end its line)
     */
    private static boolean isHereStringOpen(final String content, final int atIdx)
    {
        int p = atIdx + 2; // past @ and quote
        while( p < content.length() && ( content.charAt(
            p
        ) == ' ' || content.charAt(
            p
        ) == '\t' ) ) ++p;

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
              int       line  = 0;
              boolean   found = false;
        for(int i = 0; i < kind.length && line < lineCount; ++i) {
            final char c = content.charAt(i);
            if(c == '\n') {
                if(!found) pure[line] = true;
                ++line;
                found = false;
                continue;
            }
            if(!found && c != ' ' && c != '\t' && c != '\r') {
                pure[line] = kind[i] == 'C';
                found      = true;
            }
        } // for
        if(line < lineCount && !found) pure[line] = true;

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
            final String[] raw = content.split("\n", -1);
            this.lines = new ArrayList<>( java.util.Arrays.asList(raw) );
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
        return ToolingSharedRule.leadingWhitespace(line);
    }

    // Trims only ' '/'\t' (not the broader Character.isWhitespace set rtrim() uses elsewhere in
    // this codebase) -- must match the exact semantics of the "[ \\t]+$" / "^[ \\t]+" regexes this
    // replaces, since these operate on already-line-split PowerShell source where only space/tab
    // are meaningful trailing/leading filler.
    private static String rtrimSpaceTab(final String s)
    {
        int end = s.length();
        while( end > 0 && ( s.charAt(end - 1) == ' ' || s.charAt(end - 1) == '\t' ) ) end--;

        return s.substring(0, end);
    }

    private static String ltrimSpaceTab(final String s)
    {
        int start = 0;
        while( start < s.length() && ( s.charAt(
            start
        ) == ' ' || s.charAt(
            start
        ) == '\t' ) ) start++;

        return s.substring(start);
    }

    private String indent(final int depth)
    {
        return ToolingSharedRule.indent(depth, indentWidth);
    }

    private static String repeatChar(final char c, final int count)
    {
        return ToolingSharedRule.repeatChar(c, count);
    }

    /**
     * Per-line kind slices aligned with {@link Lines#lines} (newline characters themselves are
     * omitted; each slice is exactly the line's character kinds)
     */
    private static char[][] lineKinds(final String content, final char[] kind, final int lineCount)
    {
        final char[][] out   = new char[lineCount][];
              int      line  = 0;
              int      start = 0;
        for( int i = 0; i <= content.length() && line < lineCount; ++i ) {
            if( i == content.length() || content.charAt(i) == '\n' ) {
                final int len = i - start;
                out[line] = new char[len];
                if(len > 0) System.arraycopy( kind, start, out[line], 0, len );
                ++line;
                start = i + 1;
            } // if
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
        final PassAResult  passA    = runPassA(content);
        final Lines        lines    = new Lines(passA.transformed);
        final boolean[]    pure     = computeLinePurity(
            passA.transformed, passA.kind, lines.lines.size()
        );
        final char[][]     kinds    = lineKinds(
            passA.transformed, passA.kind, lines.lines.size()
        );
        final List<String> out      = new ArrayList<>( lines.lines.size() );
              int          depth    = 0;
              boolean      contLine = false;                                                                // True when this line is a backtick continuation of the previous one

        for( int li = 0; li < lines.lines.size(); ++li ) {
            final String line    = lines.lines.get(li);
            final String trimmed = line.trim();
            final char[] lk      = kinds[li];

            if( trimmed.isEmpty() ) {
                out.add("");
                contLine = false;
                continue;
            }

            // Scope depth tracks '('/'['/'{' and ')'/']'/'}' uniformly (not just braces) so a
            // multi-line paren/bracket group (e.g. a `param(...)` block, a multi-line call argument
            // list) indents like any other nested construct instead of being reindented flat to the
            // enclosing brace depth -- still "naive" (STYLE_TOOLING.md §3.1: every opener/closer
            // treated identically regardless of construct kind), just no longer brace-only.
            final int leadingCloses = countLeadingCloses(trimmed, line, lk);
            final int open          = countCodeChar(trimmed, line, lk, '{', '(', '[');
            final int close         = countCodeChar(trimmed, line, lk, '}', ')', ']');

            if( pure[li] ) {
                final int printDepth = Math.max(0, depth - leadingCloses) + (contLine ? 1 : 0);
                out.add( indent(printDepth) + trimmed );
            }
            else {
                // Non-pure (here-string body, full-line comment, etc.): leave byte-identical.
                out.add(line);
            }

            depth    = Math.max(0, depth + open - close);
            contLine = pure[li] && lineEndsWithBacktick(line, lk);
        } // for

        lines.lines.clear();
        lines.lines.addAll(out);

        return lines.join();
    }

    /**
     * True when {@code line} ends (ignoring nothing -- PowerShell requires the backtick to be the
     * literal last character) with a code-kind backtick line-continuation -- i.e. the next physical
     * line is a continuation of this statement and should be indented one level deeper.
     */
    private static boolean lineEndsWithBacktick(final String line, final char[] lk)
    {
        int n     = line.length() - 1;
        int count = 0;
        while( n >= 0 && line.charAt(n) == '`' && n < lk.length && lk[n] == 'C' ) {
            ++count;
            --n;
        }

        return (count % 2) == 1; // Odd trailing-backtick run = an unescaped continuation backtick
    }

    /**
     * Count code-kind {@code '}'} at the start of {@code trimmed} before any other non-whitespace
     * code character. {@code trimmed} is {@code line.trim()}; kinds are indexed into the original
     * {@code line} (so leading whitespace on {@code line} must be skipped when mapping).
     */
    private static int countLeadingCloses(
        final String trimmed,
        final String line,
        final char[] lineKind
    )
    {
        final int base = leadingWhitespace(line).length();
              int n    = 0;
        for( int i = 0; i < trimmed.length(); ++i ) {
            final char c = trimmed.charAt(i);
            if(c == ' ' || c == '\t' || c == '\r') continue;
            final int ki = base + i;
            // Trimmed is line without leading/trailing ws -- map carefully:
            // base + index into the untrimmed middle. Since trimmed strips trailing too, indices
            // into line are: first non-ws of line + i, for i scanning trimmed which has no lead ws.
            // Actually trimmed = line.trim(), so line[base + i] == trimmed[i] for i in [0, trimmed.length)
            // only if there's no difference from trailing strip affecting... yes for i < trimmed.length(),
            // line.charAt(base + i) == trimmed.charAt(i).
            if(ki < 0 || ki >= lineKind.length) break;
            if( lineKind[ki] != 'C' ) {
                // Opaque non-ws at the start (e.g. comment line) -- not a leading close
                break;
            }
            if(c == '}' || c == ')' || c == ']') n++;
            else                                 break;
        } // for

        return n;
    }

    private static int countCodeChar(
        final String  trimmed,
        final String  line,
        final char[]  lineKind,
        final char... targets
    )
    {
        final int base = leadingWhitespace(line).length();
              int n    = 0;
        for( int i = 0; i < trimmed.length(); ++i ) {
            final int ki = base + i;
            if( ki < 0 || ki >= lineKind.length || lineKind[ki] != 'C' ) continue;
            final char c = trimmed.charAt(i);
            for(final char t : targets) {
                if(c == t) { ++n; break; }
            }
        } // for i

        return n;
    }

    // ---- §3.2 Operator spacing + `=` alignment ------------------------------------------------

    /**
     * Kind-aware operator spacing over code characters only: spaces around {@code =}/{@code +=}/
     * {@code -=}/{@code *=}/{@code /=}/{@code %=} and around binary {@code +}/{@code *}/
     * {@code %} (Bash-style conservative left-hand check). Bare {@code -} is left alone so
     * PowerShell {@code -gt}/{@code -eq}/{@code -Path} tokens are never split. Binary (non-compound)
     * {@code /} is deliberately never spaced -- see the implementation comment for why.
     */
    private String applyOperatorSpacing(final String content)
    {
        final PassAResult   passA = runPassA(content);
        final String        s     = passA.transformed;
        final char[]        kind  = passA.kind;
        final StringBuilder sb    = new StringBuilder( s.length() + 16 );

        for( int i = 0; i < s.length(); ++i ) {
            if( kind[i] != 'C' ) {
                sb.append( s.charAt(i) );
                continue;
            }
            final char c = s.charAt(i);

            // Compound assignment += -= *= /= %=
            if( (c == '+' || c == '-' || c == '*' || c == '/' || c == '%')
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

            // Binary + * %  (not -, see method javadoc). `/` is deliberately excluded from this
            // list: unlike +/*/%, PowerShell's char-level tokenizer here cannot distinguish
            // expression-mode division from a bareword command-argument path/URL using `/` as a
            // separator (e.g. `Copy-Item -Force $profileDir/* $targetProfileDir`,
            // `-LiteralPath $ruleDocDirectory/README.md`, `https://api.nuget.org/v3/index.json`
            // typed unquoted) -- real-code dogfooding on PSScriptAnalyzer/PSScriptAnalyzer found
            // multiple such paths/URLs corrupted into extra, wrongly-split arguments and zero
            // instances of genuine division, so spacing bare `/` is not applied at all (the `/=`
            // compound-assignment case above is unaffected -- that form is unambiguous).
            if( (c == '+' || c == '*' || c == '%')
                    && isBinaryLeft(sb)
                    && !( i + 1 < s.length() && s.charAt(i + 1) == c ) /* Not ++ // etc */ ) {
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
            else                      break;
        }
    }

    private static int skipSpaces(final String s, final int from)
    {
        int j = from;
        while( j < s.length() && ( s.charAt(j) == ' ' || s.charAt(j) == '\t' ) ) ++j;

        return j;
    }

    /** True when the builder's last non-space char looks like an expression end (binary-op left) */
    private static boolean isBinaryLeft(final StringBuilder sb)
    {
        int i = sb.length() - 1;
        while( i >= 0 && ( sb.charAt(i) == ' ' || sb.charAt(i) == '\t' ) ) --i;
        if(i < 0) return false;
        final char p = sb.charAt(i);

        return Character.isLetterOrDigit(
            p
        ) || p == '_' || p == ')' || p == ']' || p == '}' || p == '$'
                || p == '"' || p == '\'';
    }

    private static final class AssignItem {

        String indent;
        String lhs;
        String op;     // "=", "+=", ...
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
        final PassAResult      passA = runPassA(content);
        final Lines            lines = new Lines(passA.transformed);
        final boolean[]        pure  = computeLinePurity(
            passA.transformed, passA.kind, lines.lines.size()
        );
        final char[][]         kinds = lineKinds(
            passA.transformed, passA.kind, lines.lines.size()
        );
        final List<String>     out   = new ArrayList<>();
        final List<AssignItem> group = new ArrayList<>();

        for( int li = 0; li < lines.lines.size(); ++li ) {
            final String line = lines.lines.get(li);
            if( line.trim().isEmpty() ) {
                flushAssignGroup(out, group);
                out.add("");
                continue;
            }
            final AssignItem item = pure[li] ? parseAssign( line, kinds[li] ) : null;
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
        final int lead  = leadingWhitespace(line).length();
              int depth = 0;
        for( int i = lead; i < line.length(); ++i ) {
            if( i < kind.length && kind[i] != 'C' ) continue;
            final char c = line.charAt(i);
            if(c == '(' || c == '[' || c == '{') { ++depth; continue; }
            if(c == ')' || c == ']' || c == '}') { if(depth > 0) depth--; continue; }
            if(depth != 0) continue;

            // Compound
            if( i + 1 < line.length() && line.charAt(
                i + 1
            ) == '=' && (c == '+' || c == '-' || c == '*' || c == '/' || c == '%') && ( i + 1 >= kind.length || kind[i + 1] == 'C' ) ) return makeAssign(
                line, lead, i, 2
            );
            if(c == '=') return makeAssign(line, lead, i, 1);
        } // for

        return null;
    }

    private static AssignItem makeAssign(
        final String line,
        final int    lead,
        final int    opAt,
        final int    opLen
    )
    {
        final AssignItem item = new AssignItem();
        item.indent = line.substring(0, lead);
        item.lhs    = rtrimSpaceTab( line.substring(lead, opAt) );
        if( item.lhs.isEmpty() ) return null; // E.g. line starting with =
        item.op  = line.substring(opAt, opAt + opLen);
        item.rhs = ltrimSpaceTab( line.substring(opAt + opLen) );

        return item;
    }

    private static void flushAssignGroup(final List<String> out, final List<AssignItem> group)
    {
        if( group.isEmpty() ) return;
        int maxLhs = 0;
        for(final AssignItem it : group) maxLhs = Math.max( maxLhs, it.lhs.length() );
        for(final AssignItem it : group) {
            final int pad = maxLhs - it.lhs.length();
            out.add( it.indent + it.lhs + repeatChar(' ', pad) + " " + it.op + " " + it.rhs );
        }
        group.clear();
    }

    // ---- §3.3 Pipeline: always split + right-align `|` ---------------------------------------

    /**
     * Unconditionally splits depth-0 code-kind {@code |} pipelines (one segment per line after the
     * first), right-aligns {@code |} to the longest segment, and indents continuations by one level
     * under the pipeline's base indent. Inline scriptblock arguments stay single-line because
     * pipes inside {@code {}} are depth &gt; 0 (RDD_KEY_256). Already-split pipelines are first
     * re-joined (trailing depth-0 {@code |} continues onto the next pure line) so the pass is
     * idempotent.
     */
    private String applyPipelineSplit(final String content)
    {
        final PassAResult  passA = runPassA(content);
        final Lines        lines = new Lines(passA.transformed);
        final boolean[]    pure  = computeLinePurity(
            passA.transformed, passA.kind, lines.lines.size()
        );
        final char[][]     kinds = lineKinds( passA.transformed, passA.kind, lines.lines.size() );
        final List<String> out   = new ArrayList<>();

        int i = 0;
        while( i < lines.lines.size() ) {
            final String line = lines.lines.get(i);
            if( !pure[i] || line.trim().isEmpty() || !hasDepth0Pipe( line, kinds[i] ) ) {
                out.add(line);
                ++i;
                continue;
            }

            final String       baseIndent = leadingWhitespace(line);
            final List<String> segs       = new ArrayList<>();
                  int          j          = i;
                  boolean      cont       = true;
            while( cont && j < lines.lines.size() ) {
                if(j > i) {
                    if( !pure[j] || lines.lines.get(j).trim().isEmpty() ) break;
                    // Only consume as continuation when the previous line ended with a depth-0 pipe
                    if( !lineEndsWithDepth0Pipe( lines.lines.get(j - 1), kinds[j - 1] ) ) break;
                }
                final List<String> part = splitDepth0Pipes( lines.lines.get(j), kinds[j] );
                // Skip empties from a trailing depth-0 `|` (both mid-group continuations and the
                // final "ends with |" line) -- otherwise re-formatting an already-split pipeline
                // inserts blank `|`-only segments and breaks idempotency
                for(final String p : part) {
                    if( !p.isEmpty() ) segs.add(p);
                }
                cont = lineEndsWithDepth0Pipe( lines.lines.get(j), kinds[j] );
                ++j;
            } // while

            if( segs.size() < 2 ) {
                out.add(line);
                ++i;
                continue;
            }

            final String contIndent = baseIndent + indent(1);
            // Absolute column of `|` so every non-last segment's pipe lines up (STYLE §3.3 example),
            // with at least one space between the segment text and `|`.
            int pipeCol = 0;
            for( int s = 0; s + 1 < segs.size(); ++s ) {
                final int pref = ( (s == 0) ? baseIndent : contIndent ).length();
                pipeCol = Math.max( pipeCol, pref + segs.get(s).length() + 1 );
            }
            for( int s = 0; s < segs.size(); ++s ) {
                final String prefix = (s == 0) ? baseIndent : contIndent;
                if( s + 1 < segs.size() ) {
                    final int spaces = Math.max(
                        1, pipeCol - prefix.length() - segs.get(s).length()
                    );
                    out.add( prefix + segs.get(s) + repeatChar(' ', spaces) + "|" );
                }
                else {
                    out.add( prefix + segs.get(s) );
                }
            } // for
            i = j;
        } // while

        lines.lines.clear();
        lines.lines.addAll(out);

        return lines.join();
    }

    private static boolean hasDepth0Pipe(final String line, final char[] kind)
    {
        int depth = 0;
        for( int i = 0; i < line.length(); ++i ) {
            if( i < kind.length && kind[i] != 'C' ) continue;
            final char c = line.charAt(i);
                 if(c == '(' || c == '[' || c == '{') depth++;
            else if(c == ')' || c == ']' || c == '}') { if(depth > 0) depth--; }
            else if( depth == 0 && c == '|' && !isDoublePipe(line, i) ) return true;
        } // for

        return false;
    }

    private static boolean lineEndsWithDepth0Pipe(final String line, final char[] kind)
    {
        // Find last non-ws char; it must be a depth-0 |
        int end = line.length() - 1;
        while( end >= 0 && ( line.charAt(end) == ' ' || line.charAt(end) == '\t' ) ) --end;
        if( end < 0 || line.charAt(end) != '|' ) return false;
        if( isDoublePipe(line, end) || ( end > 0 && line.charAt(end - 1) == '|' ) ) return false;
        int depth = 0;
        for(int i = 0; i <= end; ++i) {
            if( i < kind.length && kind[i] != 'C' ) continue;
            final char c = line.charAt(i);
                 if(c == '(' || c == '[' || c == '{') depth++;
            else if(c == ')' || c == ']' || c == '}') { if(depth > 0) depth--; }
        }

        return depth == 0; // the final | sits at depth 0 when the scan ends at depth 0
    }

    private static boolean isDoublePipe(final String line, final int i)
    {
        return ( i + 1 < line.length() && line.charAt(i + 1) == '|' )
                || ( i > 0 && line.charAt(i - 1) == '|' );
    }

    /** Split line on depth-0 code-kind lone {@code |}; returned segments are trimmed */
    private static List<String> splitDepth0Pipes(final String line, final char[] kind)
    {
        final List<String> segs  = new ArrayList<>();
        final int          lead  = leadingWhitespace(line).length();
              int          start = lead;
              int          depth = 0;
        for( int i = lead; i < line.length(); ++i ) {
            if( i < kind.length && kind[i] != 'C' ) continue;
            final char c = line.charAt(i);
            if(c == '(' || c == '[' || c == '{') depth++;
            else if(c == ')' || c == ']' || c == '}') { if(depth > 0) depth--; }
            else if( depth == 0 && c == '|' && !isDoublePipe(line, i) ) {
                segs.add( line.substring(start, i).trim() );
                start = i + 1;
            }
        } // for
        segs.add( line.substring(start).trim() );

        return segs;
    }

    // ---- §3.5 switch formatting (+ shared keyword-paren spacing) ------------------------------

    private static final Pattern KEYWORD_PAREN = Pattern.compile(
        "(?i)(?<![A-Za-z0-9_.])(if|elseif|while|for|foreach|switch|catch|trap|until)(\\s*)\\("
    );

    /**
     * Insert a single space between a control keyword and its opening {@code (}
     * ({@code switch($x)} → {@code switch ($x)}, same for {@code if}/{@code while}/…). Kind-aware:
     * only rewrites when the keyword and {@code (} are both code-kind.
     */
    private String applyKeywordParenSpacing(final String content)
    {
        final PassAResult   passA = runPassA(content);
        final String        s     = passA.transformed;
        final char[]        kind  = passA.kind;
        final Matcher       m     = KEYWORD_PAREN.matcher(s);
        final StringBuilder sb    = new StringBuilder( s.length() + 8 );
              int           last  = 0;
        while( m.find() ) {
            final int kwStart = m.start(1);
            final int paren   = m.end() - 1; // Index of (
            if( kind[kwStart] != 'C' || kind[paren] != 'C' ) continue;
            sb.append(s, last, kwStart);
            sb.append( m.group(1) ).append(" (");
            last = m.end();
        } // while
        sb.append( s, last, s.length() );

        return sb.toString();
    }

    private static final class ArmItem {

        String indent;
        String pattern;
        String rest;    // Begins with '{'

    } // class ArmItem

    /**
     * Align {@code \{} on consecutive switch-arm-like lines (same indent), using the same
     * group-boundary rule as §3.2 (blank or non-arm breaks the group -- RDD_KEY_254). A line is an
     * arm when its first depth-0 code {@code \{} is preceded by a non-empty pattern that is not a
     * control-flow header ({@code if (...)}/{@code function ...}/…).
     */
    private String applySwitchArmAlignment(final String content)
    {
        final PassAResult   passA = runPassA(content);
        final Lines         lines = new Lines(passA.transformed);
        final boolean[]     pure  = computeLinePurity(
            passA.transformed, passA.kind, lines.lines.size()
        );
        final char[][]      kinds = lineKinds( passA.transformed, passA.kind, lines.lines.size() );
        final List<String>  out   = new ArrayList<>();
        final List<ArmItem> group = new ArrayList<>();

        for( int li = 0; li < lines.lines.size(); ++li ) {
            final String line = lines.lines.get(li);
            if( line.trim().isEmpty() ) {
                flushArmGroup(out, group);
                out.add("");
                continue;
            }
            final ArmItem item = pure[li] ? parseArm( line, kinds[li] ) : null;
            if(item != null) {
                // Break group on indent change
                if( !group.isEmpty() && !group.get(
                    0
                ).indent.equals(
                    item.indent
                ) ) flushArmGroup(
                    out, group
                );
                group.add(item);
                continue;
            } // if
            flushArmGroup(out, group);
            out.add(line);
        } // for
        flushArmGroup(out, group);

        lines.lines.clear();
        lines.lines.addAll(out);

        return lines.join();
    }

    private static ArmItem parseArm(final String line, final char[] kind)
    {
        final int lead  = leadingWhitespace(line).length();
              int depth = 0;
        for( int i = lead; i < line.length(); ++i ) {
            if( i < kind.length && kind[i] != 'C' ) continue;
            final char c = line.charAt(i);
            if(c == '(' || c == '[') { ++depth; continue; }
            if(c == ')' || c == ']') { if(depth > 0) depth--; continue; }
            // Do not track '{}' for depth here -- the first depth-0 '{' opens the arm body
            if(depth != 0) continue;
            // A depth-0 pipe before the '{' means this is a pipeline stage (e.g.
            // `... | Where-Object {...}`), not a switch arm -- do not misclassify it as one
            // (fixes an idempotency bug: pre-split, a still-unsplit pipeline's trailing
            // scriptblock read as an arm pattern spanning the whole line; post-split, the same
            // line no longer does, so alignment padding flip-flopped between rounds).
            if( c == '|' && !isDoublePipe(line, i) ) return null;
            if(c == '{') {
                // Hashtable opener `@{` is not a switch arm (would become `@ {` via the pad space)
                if( i > 0 && line.charAt(i - 1) == '@' ) return null;
                final String pattern = rtrimSpaceTab( line.substring(lead, i) );
                if( pattern.isEmpty() ) return null;
                if( isControlHeader(pattern) ) return null;
                final ArmItem item = new ArmItem();
                item.indent  = line.substring(0, lead);
                item.pattern = pattern;
                item.rest    = line.substring(i);       // Starts with {

                return item;
            } // if
        } // for

        return null;
    }

    private static final Pattern CONTROL_HEADER_KEYWORD = Pattern.compile(
        "(?i)^(if|elseif|else|while|for|foreach|switch|function|filter|catch|trap|try|finally|" + "begin|process|end|dynamicparam)\\b"
    );

    private static boolean isControlHeader(final String pattern)
    {
        final String t = pattern.trim();
        // Word-at-start check against control keywords (case-insensitive)
        final Matcher m = CONTROL_HEADER_KEYWORD.matcher(t);

        return m.find();
    }

    private static void flushArmGroup(final List<String> out, final List<ArmItem> group)
    {
        if( group.isEmpty() ) return;
        int maxPat = 0;
        for(final ArmItem it : group) maxPat = Math.max( maxPat, it.pattern.length() );
        for(final ArmItem it : group) {
            final int pad = maxPat - it.pattern.length();
            out.add( it.indent + it.pattern + repeatChar(' ', pad) + " " + it.rest );
        }
        group.clear();
    }

    // ---- §3.6 `{` / `}` spacing ---------------------------------------------------------------

    /**
     * Exactly one space before an opening {@code \{} (except after {@code @} so {@code @{} stays
     * tight) and, when the brace pair is non-empty on the same line, one space after {@code \{} and
     * one space before {@code \}}. Empty {@code \{\}} stays tight. Applies everywhere, including
     * single-line scriptblock arguments (RDD_KEY_258). Opaque regions are never touched.
     */
    private String applyBraceSpacing(final String content)
    {
        final PassAResult   passA = runPassA(content);
        final String        s     = passA.transformed;
        final char[]        kind  = passA.kind;
        final StringBuilder sb    = new StringBuilder( s.length() + 16 );

        for( int i = 0; i < s.length(); ++i ) {
            if( kind[i] != 'C' ) {
                sb.append( s.charAt(i) );
                continue;
            }
            final char c = s.charAt(i);

            if(c == '{') {
                stripTrailingSpaces(sb);
                if( sb.length() > 0 ) {
                    final char p = sb.charAt( sb.length() - 1 );
                    if(p != '@' && p != '\n' && p != '{') sb.append(' ');
                }
                sb.append('{');
                int j = skipSpaces(s, i + 1);
                if( j < s.length() && s.charAt(j) != '\n' && s.charAt(j) != '}' ) sb.append(' ');
                i = j - 1;
                continue;
            } // if

            if(c == '}') {
                stripTrailingSpaces(sb);
                if( sb.length() > 0 ) {
                    final char p = sb.charAt( sb.length() - 1 );
                    if(p != '{' && p != '\n') sb.append(' ');
                }
                sb.append('}');
                continue;
            } // if

            sb.append(c);
        } // for

        return sb.toString();
    }

    public String format(final String content)
    {
        // §3.3 pipeline split runs before §3.2/§3.5 alignment passes: both alignment passes'
        // line-grouping depends on whether a pipeline is already split onto multiple lines (a
        // still-unsplit `... | Where-Object {...}` line reads very differently to the group-
        // boundary/arm-pattern scanners than its post-split form), so running them beforehand made
        // the result depend on how many times the content had already been formatted -- an
        // idempotency bug. Splitting first makes every pass downstream see the same stable shape.

        // §3.4 single-line hashtables: never expanded to multi-line (RDD_KEY_257); no extra pass.

        String s = content;
        s = applyKeywordParenSpacing(s); // §3.5 (also benefits if/while/... examples in §3.1)
        s = applyOperatorSpacing(s);     // §3.2 spacing
        s = applyBraceSpacing(s);        // §3.6 (before indent/align so later passes see spaced braces)
        s = applyBraceIndent(s);         // §3.1 (also multi-line hashtable bodies -- §3.4)
        s = applyPipelineSplit(s);       // §3.3 (after indent so continuation uses base+1 level)
        s = applyAssignAlignment(s);     // §3.2 alignment (also multi-line hashtable entries -- §3.4)
        s = applySwitchArmAlignment(s);  // §3.5 arm `{` alignment (after indent)
        return s;
    }

} // class PowerShellSpecificRule
