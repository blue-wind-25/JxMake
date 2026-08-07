/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STYLE_TOOLING.md §2 (Bash) rule logic. A real tokenizer (character-level state machine) is
 * required per the style file: single/double quotes, `$'...'`, backticks, `$(...)` command
 * substitution, `$((...))` arithmetic, `#` comments, and heredocs must all be recognized so none
 * of the five rules ever fires inside them. Everything inside `$(...)`/backticks (nested command
 * text) is treated as opaque and left untouched -- reformatting nested commands recursively is out
 * of scope (STYLE_TOOLING.md §2's fixed 5-rule list has no such requirement).
 *
 * <p>Two-pass design: pass A classifies every character as code ('C'), opaque ('O' -- strings,
 * comments, backticks/`$(...)`, heredoc bodies), or arithmetic-interior ('A' -- inside `$((...))`,
 * not itself inside another opaque region), and applies the two character/token-level rules
 * (§2.2 pipe spacing on 'C' runs, §2.5 arithmetic operator spacing on 'A' runs) inline -- neither
 * adds or removes newlines, so line numbering is unaffected. Pass B is line-oriented (like
 * {@link MakefileSpecificRule}) and applies the three structural rules (§2.1 if/then merge, §2.3
 * function brace placement, §2.4 case formatting), guarded by a per-original-line "pure code line"
 * flag (first non-whitespace character is code, not opaque) computed during pass A so structural
 * rules never touch a heredoc-body or full-comment line.
 *
 * <p>Comments also get the optional §0 ad hoc normalization (RDD_KEY_261): first-letter
 * capitalization (skipped when the comment's leading word is a common Unix tool name -- comments
 * routinely open with one, e.g. "grep for the pattern", and it must not be capitalized) and
 * sole-trailing-period stripping, both via {@link ToolingCommentNormalizer}, applied to each `#`
 * line-comment run in pass A.
 */
public final class BashSpecificRule {

    /** Common Unix tool names left lowercase at the start of a comment (RDD_KEY_261). */
    private static final Set<String> NO_CAPITALIZE_TOOLS = new HashSet<>( Arrays.asList(
        "awk", "grep", "sed", "head", "tail", "cut", "sort", "uniq", "tr", "xargs", "find",
        "diff", "tar", "curl", "wget", "ssh", "git", "cat", "echo", "printf", "wc", "chmod",
        "chown", "ln", "mv", "cp", "rm", "mkdir", "ps", "kill", "du", "df"
    ) );

    private final int     indentWidth;
    private final boolean normalizeCommentStartCase;
    private final boolean normalizeCommentEndPeriod;

    public BashSpecificRule(
        final int indentWidth, final boolean normalizeCommentStartCase, final boolean normalizeCommentEndPeriod
    )
    {
        this.indentWidth               = Math.max(1, indentWidth);
        this.normalizeCommentStartCase = normalizeCommentStartCase;
        this.normalizeCommentEndPeriod = normalizeCommentEndPeriod;
    }

    // ---- Pass A: char-level classification + token-level transforms ---------------------------

    private static final class Frame {

        final char    type; // 'S'=squote 'D'=dquote 'Q'=dollarsquote 'B'=backtick 'C'=cmdsub 'A'=arith '#'=comment
        final boolean opaque;
        int           parenDepth;
        boolean       standalone; // '#'-frames only: true iff nothing but whitespace precedes it on its line
        int           lineNo;     // '#'-frames only: 0-based source line index this comment starts on

        Frame(final char type, final boolean opaque)
        {
            this.type   = type;
            this.opaque = opaque;
        }

    } // class Frame

    private static final class PassAResult {

        String  transformed;
        char[]  kind; // per original character: 'C' / 'O' / 'A'

    } // class PassAResult

    /** Accumulates same-kind characters and flushes (applying the token-level transform) on kind change */
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
            if(kind == 'C')       out.append( pipeSpacing( run.toString() ) );
            else if(kind == 'A')  out.append( arithmeticSpacing( run.toString() ) );
            else                  out.append(run);
            run.setLength(0);
        }

        String result()
        {
            flush();

            return out.toString();
        }

    } // class RunBuffer

    private static final class HeredocSpec {

        final String  delim;
        final boolean stripTabs;

        HeredocSpec(final String delim, final boolean stripTabs)
        {
            this.delim     = delim;
            this.stripTabs = stripTabs;
        }

    } // class HeredocSpec

    private PassAResult runPassA(final String content)
    {
        final char[]              kind            = new char[content.length()];
        final List<Frame>         stack           = new ArrayList<>();
        final RunBuffer           buf             = new RunBuffer();
        final List<HeredocSpec>   pendingHeredocs = new ArrayList<>();
        final StringBuilder       commentBody     = new StringBuilder();
        final ToolingCommentNormalizer.ChainCollector chainCollector =
            new ToolingCommentNormalizer.ChainCollector("CHAIN", "");
              int                 i               = 0;

        while( i < content.length() ) {
            final Frame top = stack.isEmpty() ? null : stack.get( stack.size() - 1 );
            final char  c   = content.charAt(i);

            if( top != null && top.type == '#' ) {
                if(c == '\n') {
                    stack.remove( stack.size() - 1 );
                    if(top.standalone) {
                        final String placeholder = chainCollector.defer( commentBody.toString(), top.lineNo );
                        for( int p = 0; p < placeholder.length(); ++p ) buf.emit( placeholder.charAt(p), 'O' );
                    } else {
                        emitNormalizedComment( buf, commentBody.toString() );
                    }
                    commentBody.setLength(0);
                    kind[i] = 'C';
                    buf.emit(c, 'C');
                    ++i;
                    continue;
                }
                kind[i] = 'O';
                commentBody.append(c);
                ++i;
                continue;
            }

            if( top != null && top.type == 'S' ) {
                kind[i] = 'O';
                buf.emit(c, 'O');
                if(c == '\'') stack.remove( stack.size() - 1 );
                ++i;
                continue;
            }

            if( top != null && ( top.type == 'D' || top.type == 'Q' ) ) {
                if( c == '\\' && i + 1 < content.length() ) {
                    kind[i] = 'O';
                    buf.emit(c, 'O');
                    ++i;
                    kind[i] = 'O';
                    buf.emit( content.charAt(i), 'O' );
                    ++i;
                    continue;
                }
                if( top.type == 'D' && c == '$' && i + 1 < content.length() ) {
                    if( content.charAt(i + 1) == '(' && i + 2 < content.length() && content.charAt(i + 2) == '(' ) {
                        stack.add( new Frame('A', false) );
                        kind[i] = kind[i + 1] = kind[i + 2] = 'C';
                        buf.emit('$', 'C'); buf.emit('(', 'C'); buf.emit('(', 'C');
                        i += 3;
                        continue;
                    }
                    if( content.charAt(i + 1) == '(' ) {
                        stack.add( new Frame('C', true) );
                        kind[i] = kind[i + 1] = 'O';
                        buf.emit('$', 'O'); buf.emit('(', 'O');
                        i += 2;
                        continue;
                    }
                }
                if( top.type == 'D' && c == '`' ) {
                    stack.add( new Frame('B', true) );
                    kind[i] = 'O';
                    buf.emit(c, 'O');
                    ++i;
                    continue;
                }
                kind[i] = 'O';
                buf.emit(c, 'O');
                if( ( top.type == 'D' && c == '"' ) || ( top.type == 'Q' && c == '\'' ) ) {
                    stack.remove( stack.size() - 1 );
                }
                ++i;
                continue;
            }

            if( top != null && top.type == 'B' ) {
                if( c == '\\' && i + 1 < content.length() ) {
                    kind[i] = 'O';
                    buf.emit(c, 'O');
                    ++i;
                    kind[i] = 'O';
                    buf.emit( content.charAt(i), 'O' );
                    ++i;
                    continue;
                }
                kind[i] = 'O';
                buf.emit(c, 'O');
                if(c == '`') stack.remove( stack.size() - 1 );
                ++i;
                continue;
            }

            if( top != null && top.type == 'C' ) {
                if(c == '\'') { kind[i] = 'O'; buf.emit(c, 'O'); stack.add( new Frame('S', true) ); ++i; continue; }
                if(c == '"')  { kind[i] = 'O'; buf.emit(c, 'O'); stack.add( new Frame('D', true) ); ++i; continue; }
                if(c == '`')  { kind[i] = 'O'; buf.emit(c, 'O'); stack.add( new Frame('B', true) ); ++i; continue; }
                if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '(' ) {
                    if( i + 2 < content.length() && content.charAt(i + 2) == '(' ) {
                        kind[i] = kind[i + 1] = kind[i + 2] = 'O';
                        buf.emit('$', 'O'); buf.emit('(', 'O'); buf.emit('(', 'O');
                        stack.add( new Frame('A', true) );
                        i += 3;
                        continue;
                    }
                    kind[i] = kind[i + 1] = 'O';
                    buf.emit('$', 'O'); buf.emit('(', 'O');
                    stack.add( new Frame('C', true) );
                    i += 2;
                    continue;
                }
                if(c == '(') { top.parenDepth++; kind[i] = 'O'; buf.emit(c, 'O'); ++i; continue; }
                if(c == ')') {
                    kind[i] = 'O';
                    buf.emit(c, 'O');
                    if(top.parenDepth > 0) top.parenDepth--;
                    else stack.remove( stack.size() - 1 );
                    ++i;
                    continue;
                }
                kind[i] = 'O';
                buf.emit(c, 'O');
                ++i;
                continue;
            }

            if( top != null && top.type == 'A' ) {
                final char inner = top.opaque ? 'O' : 'A';
                if(c == '(') { top.parenDepth++; kind[i] = inner; buf.emit(c, inner); ++i; continue; }
                if(c == ')') {
                    if(top.parenDepth > 0) {
                        top.parenDepth--;
                        kind[i] = inner;
                        buf.emit(c, inner);
                        ++i;
                        continue;
                    }
                    if( i + 1 < content.length() && content.charAt(i + 1) == ')' ) {
                        stack.remove( stack.size() - 1 );
                        kind[i] = kind[i + 1] = 'C';
                        buf.emit(')', 'C'); buf.emit(')', 'C');
                        i += 2;
                        continue;
                    }
                    stack.remove( stack.size() - 1 );
                    kind[i] = 'C';
                    buf.emit(c, 'C');
                    ++i;
                    continue;
                }
                kind[i] = inner;
                buf.emit(c, inner);
                ++i;
                continue;
            }

            // top == null: root/NORMAL context, real top-level code
            if( c == '<' && i + 1 < content.length() && content.charAt(i + 1) == '<'
                    && !( i + 2 < content.length() && content.charAt(i + 2) == '<' ) ) {
                final int parsed = tryParseHeredocOperator(content, i);
                if(parsed > i) {
                    boolean stripTabs = false;
                    int     p         = i + 2;
                    if( p < content.length() && content.charAt(p) == '-' ) { stripTabs = true; ++p; }
                    while( p < content.length() && ( content.charAt(p) == ' ' || content.charAt(p) == '\t' ) ) ++p;
                    final String delim = parseHeredocDelim(content, p);
                    for( int q = i; q < parsed; ++q ) { kind[q] = 'C'; buf.emit( content.charAt(q), 'C' ); }
                    pendingHeredocs.add( new HeredocSpec(delim, stripTabs) );
                    i = parsed;
                    continue;
                }
            }
            if( c == '\n' && !pendingHeredocs.isEmpty() ) {
                kind[i] = 'C';
                buf.emit(c, 'C');
                ++i;
                i = consumeHeredocs(content, i, pendingHeredocs, kind, buf);
                pendingHeredocs.clear();
                continue;
            }
            if(c == '#' && isCommentStart(content, i)) {
                final Frame f = new Frame('#', true);
                int lineStart = i;
                while( lineStart > 0 && content.charAt(lineStart - 1) != '\n' ) --lineStart;
                f.standalone = content.substring(lineStart, i).trim().isEmpty();
                int ln = 0;
                for( int p = 0; p < lineStart; ++p ) if( content.charAt(p) == '\n' ) ++ln;
                f.lineNo = ln;
                stack.add(f);
                kind[i] = 'O';
                buf.emit(c, 'O');
                ++i;
                continue;
            }
            if(c == '\'') { stack.add( new Frame('S', true) ); kind[i] = 'O'; buf.emit(c, 'O'); ++i; continue; }
            if(c == '"')  { stack.add( new Frame('D', true) ); kind[i] = 'O'; buf.emit(c, 'O'); ++i; continue; }
            if(c == '`')  { stack.add( new Frame('B', true) ); kind[i] = 'O'; buf.emit(c, 'O'); ++i; continue; }
            if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '\'' ) {
                stack.add( new Frame('Q', true) );
                kind[i] = kind[i + 1] = 'O';
                buf.emit('$', 'O'); buf.emit('\'', 'O');
                i += 2;
                continue;
            }
            if( c == '$' && i + 1 < content.length() && content.charAt(i + 1) == '(' ) {
                if( i + 2 < content.length() && content.charAt(i + 2) == '(' ) {
                    stack.add( new Frame('A', false) );
                    kind[i] = kind[i + 1] = kind[i + 2] = 'C';
                    buf.emit('$', 'C'); buf.emit('(', 'C'); buf.emit('(', 'C');
                    i += 3;
                    continue;
                }
                stack.add( new Frame('C', true) );
                kind[i] = kind[i + 1] = 'C';
                buf.emit('$', 'C'); buf.emit('(', 'C');
                i += 2;
                continue;
            }
            kind[i] = 'C';
            buf.emit(c, 'C');
            ++i;
        } // while

        if( commentBody.length() > 0 ) {
            final Frame top = stack.isEmpty() ? null : stack.get( stack.size() - 1 );
            if( top != null && top.type == '#' && top.standalone ) {
                final String placeholder = chainCollector.defer( commentBody.toString(), top.lineNo );
                for( int p = 0; p < placeholder.length(); ++p ) buf.emit( placeholder.charAt(p), 'O' );
            } else {
                emitNormalizedComment( buf, commentBody.toString() );
            }
        }

        String transformed = buf.result();
        transformed = chainCollector.resolve(
            transformed, normalizeCommentStartCase, normalizeCommentEndPeriod, NO_CAPITALIZE_TOOLS
        );

        final PassAResult result = new PassAResult();
        result.transformed = transformed;
        result.kind         = kind;

        return result;
    }

    /** Normalizes a `#` line comment's body (text after the `#`) and re-emits it as opaque ('O') characters. */
    private void emitNormalizedComment(final RunBuffer buf, final String body)
    {
        final String normalized = ToolingCommentNormalizer.normalize(
            body, normalizeCommentStartCase, normalizeCommentEndPeriod, NO_CAPITALIZE_TOOLS
        );
        for( int i = 0; i < normalized.length(); ++i ) buf.emit( normalized.charAt(i), 'O' );
    }

    /** Returns the index just past the heredoc delimiter token, or {@code start} if none parses (e.g. bare `<<`) */
    private static int tryParseHeredocOperator(final String content, final int start)
    {
        int p = start + 2;
        if( p < content.length() && content.charAt(p) == '-' ) ++p;
        while( p < content.length() && ( content.charAt(p) == ' ' || content.charAt(p) == '\t' ) ) ++p;
        final String delim = parseHeredocDelim(content, p);
        if( delim.isEmpty() ) return start;
        int end = p;
        if( end < content.length() && ( content.charAt(end) == '\'' || content.charAt(end) == '"' ) ) {
            final char q = content.charAt(end);
            ++end;
            while( end < content.length() && content.charAt(end) != q ) ++end;
            if( end < content.length() ) ++end;
        } else {
            while( end < content.length() && !Character.isWhitespace( content.charAt(end) )
                    && ";&|<>()".indexOf( content.charAt(end) ) < 0 ) ++end;
        }

        return end;
    }

    private static String parseHeredocDelim(final String content, final int start)
    {
        final StringBuilder sb = new StringBuilder();
        if( start < content.length() && ( content.charAt(start) == '\'' || content.charAt(start) == '"' ) ) {
            final char q = content.charAt(start);
            int        p = start + 1;
            while( p < content.length() && content.charAt(p) != q ) { sb.append( content.charAt(p) ); ++p; }
        } else {
            int p = start;
            while( p < content.length() && !Character.isWhitespace( content.charAt(p) )
                    && ";&|<>()".indexOf( content.charAt(p) ) < 0 ) { sb.append( content.charAt(p) ); ++p; }
        }

        return sb.toString();
    }

    /** Consumes heredoc bodies verbatim (marking every character 'O') until each pending delimiter's terminator line is found */
    private static int consumeHeredocs(
        final String content, final int startIdx, final List<HeredocSpec> queue, final char[] kind, final RunBuffer buf
    )
    {
        int idx = startIdx;
        for( final HeredocSpec spec : queue ) {
            while( idx < content.length() ) {
                final int    lineStartIdx = idx;
                int          lineEnd      = content.indexOf('\n', idx);
                if(lineEnd < 0) lineEnd = content.length();
                final String line  = content.substring(lineStartIdx, lineEnd);
                final String check = spec.stripTabs ? stripLeadingTabs(line) : line;
                final boolean isTerm = check.equals(spec.delim);
                for( int p = lineStartIdx; p < lineEnd; ++p ) { kind[p] = 'O'; buf.emit( content.charAt(p), 'O' ); }
                if( lineEnd < content.length() ) {
                    kind[lineEnd] = 'O';
                    buf.emit('\n', 'O');
                    idx = lineEnd + 1;
                } else {
                    idx = lineEnd;
                }
                if(isTerm) break;
                if(lineEnd >= content.length()) break;
            } // while
        } // for

        return idx;
    }

    private static String stripLeadingTabs(final String line)
    {
        int i = 0;
        while( i < line.length() && line.charAt(i) == '\t' ) ++i;

        return line.substring(i);
    }

    /**
     * A `#` starts a comment only at a token boundary (start of content, or preceded by
     *  whitespace/`;`/`|`/`&`/`(`/`)`/`{`/`\n`) -- excludes `${#arr[@]}` (length operator, preceded
     *  by `{` which is itself preceded by `$`) and `$#` (positional-param-count variable).
     */
    private static boolean isCommentStart(final String content, final int i)
    {
        if(i == 0) return true;
        final char prev = content.charAt(i - 1);
        if(prev == '$') return false;
        if( prev == '{' && i >= 2 && content.charAt(i - 2) == '$' ) return false;

        return prev == ' ' || prev == '\t' || prev == ';' || prev == '|' || prev == '&'
                || prev == '(' || prev == ')' || prev == '{' || prev == '\n';
    }

    /** §2.2: exactly one space on each side of a lone `|` (not `||` or `|&`) */
    private static String pipeSpacing(final String run)
    {
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < run.length(); ++i ) {
            final char c = run.charAt(i);
            if( c == '|' && !( i > 0 && run.charAt(i - 1) == '|' ) && !( i + 1 < run.length() && ( run.charAt(i + 1) == '|' || run.charAt(i + 1) == '&' ) ) ) {
                while( sb.length() > 0 && ( sb.charAt( sb.length() - 1 ) == ' ' || sb.charAt( sb.length() - 1 ) == '\t' ) ) sb.setLength( sb.length() - 1 );
                sb.append(" | ");
                int j = i + 1;
                while( j < run.length() && ( run.charAt(j) == ' ' || run.charAt(j) == '\t' ) ) ++j;
                i = j - 1;
                continue;
            }
            sb.append(c);
        } // for

        return sb.toString();
    }

    /** §2.5: single space around binary `+ - * / %` inside `$((...))` (conservative -- unary signs left alone) */
    private static String arithmeticSpacing(final String run)
    {
        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < run.length(); ++i ) {
            final char c = run.charAt(i);
            if( ( c == '+' || c == '-' || c == '*' || c == '/' || c == '%' )
                    && !( i + 1 < run.length() && run.charAt(i + 1) == c )
                    && sb.length() > 0
                    && ( Character.isLetterOrDigit( sb.charAt( sb.length() - 1 ) ) || sb.charAt( sb.length() - 1 ) == ')' || sb.charAt( sb.length() - 1 ) == ']' || sb.charAt( sb.length() - 1 ) == '_' )
            ) {
                while( sb.length() > 0 && sb.charAt( sb.length() - 1 ) == ' ' ) sb.setLength( sb.length() - 1 );
                sb.append(' ').append(c).append(' ');
                int j = i + 1;
                while( j < run.length() && run.charAt(j) == ' ' ) ++j;
                i = j - 1;
                continue;
            }
            sb.append(c);
        } // for

        return sb.toString();
    }

    // ---- Pass B: line-structural rules ---------------------------------------------------------

    private static final Pattern FUNC_BRACE = Pattern.compile(
        "^([A-Za-z_][A-Za-z0-9_]*)\\s*\\(\\s*\\)\\s*\\{\\s*$"
    );
    private static final Pattern CASE_START = Pattern.compile("^case\\s+.+\\s+in\\s*$");
    private static final Pattern CASE_ARM   = Pattern.compile("^([^()][^)]*)\\)\\s*(.*)$");

    public String format(final String content)
    {
        final PassAResult passA = runPassA(content);

        final boolean      endsWithNewline = passA.transformed.endsWith("\n");
        final String[]     rawLines        = passA.transformed.split("\n", -1);
        final List<String> lines           = new ArrayList<>( java.util.Arrays.asList(rawLines) );
        if( endsWithNewline && !lines.isEmpty() ) lines.remove( lines.size() - 1 );

        final boolean[] pure = computeLinePurity(content, passA.kind, lines.size());

        final List<String> out = new ArrayList<>();
        int idx = 0;
        while( idx < lines.size() ) {
            final String line    = lines.get(idx);
            final String trimmed = line.trim();

            if( pure[idx] && !trimmed.isEmpty() && trimmed.equals("if") == false && isIfHeader(trimmed)
                    && idx + 1 < lines.size() && pure[idx + 1] && lines.get(idx + 1).trim().equals("then") ) {
                out.add( leadingWhitespace(line) + trimmed + "; then" );
                idx += 2;
                continue;
            }

            if( pure[idx] ) {
                final Matcher fn = FUNC_BRACE.matcher(trimmed);
                if( fn.matches() ) {
                    final String prefix = leadingWhitespace(line);
                    out.add( prefix + fn.group(1) + "() {" );
                    ++idx;
                    idx = emitBraceBody(lines, pure, idx, prefix, out);
                    continue;
                }
            }

            if( pure[idx] && CASE_START.matcher(trimmed).matches() ) {
                final String prefix = leadingWhitespace(line);
                out.add( prefix + trimmed );
                ++idx;
                idx = emitCaseBody(lines, pure, idx, prefix, out);
                continue;
            }

            out.add(line);
            ++idx;
        } // while

        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < out.size(); ++i ) {
            sb.append( out.get(i) );
            if( i + 1 < out.size() || endsWithNewline ) sb.append('\n');
        }

        return sb.toString();
    }

    private static boolean isIfHeader(final String trimmed)
    {
        if( !( trimmed.equals("if") || trimmed.startsWith("if ") || trimmed.startsWith("if\t") ) ) return false;

        return !trimmed.endsWith(";") && !trimmed.endsWith("then");
    }

    /** Reindents `{...}` body lines by brace-depth (starting at depth 1), emitting the closing `}` at depth 0 */
    private int emitBraceBody(
        final List<String> lines, final boolean[] pure, final int startIdx, final String basePrefix, final List<String> out
    )
    {
        int depth = 1;
        int idx   = startIdx;
        while( idx < lines.size() ) {
            final String raw     = lines.get(idx);
            final String trimmed = raw.trim();

            if( trimmed.isEmpty() ) { out.add(""); ++idx; continue; }
            if( !pure[idx] )        { out.add(raw); ++idx; continue; }

            final int printDepth = trimmed.startsWith("}") ? depth - 1 : depth;
            if( printDepth <= 0 && trimmed.equals("}") ) {
                out.add( basePrefix + "}" );
                return idx + 1;
            }
            out.add( basePrefix + indent( Math.max(printDepth, 0) ) + trimmed );

            for( final char c : trimmed.toCharArray() ) {
                if(c == '{') ++depth;
                else if(c == '}') --depth;
            }
            ++idx;
            if(depth <= 0) return idx;
        } // while

        return idx;
    }

    /** §2.4: pattern lines at the case's own indent, arm bodies + `;;` at one level deeper */
    private int emitCaseBody(
        final List<String> lines, final boolean[] pure, final int startIdx, final String basePrefix, final List<String> out
    )
    {
        final String  bodyPrefix      = basePrefix + indent(1);
        int           idx             = startIdx;
        boolean       expectingPattern = true;
        while( idx < lines.size() ) {
            final String trimmed = lines.get(idx).trim();
            if( pure[idx] && trimmed.equals("esac") ) {
                out.add( basePrefix + "esac" );
                return idx + 1;
            }
            if( pure[idx] && !trimmed.isEmpty() ) {
                final Matcher arm = expectingPattern ? CASE_ARM.matcher(trimmed) : null;
                if( arm != null && arm.matches() ) {
                    out.add( basePrefix + arm.group(1).trim() + ")" );
                    String rest = arm.group(2).trim();
                    if( rest.endsWith(";;") ) {
                        rest = rest.substring( 0, rest.length() - 2 ).trim();
                        if( !rest.isEmpty() ) out.add(bodyPrefix + rest);
                        out.add(bodyPrefix + ";;");
                        expectingPattern = true;
                    } else {
                        if( !rest.isEmpty() ) out.add(bodyPrefix + rest);
                        expectingPattern = false;
                    }
                    ++idx;
                    continue;
                }
                if( trimmed.equals(";;") ) {
                    out.add(bodyPrefix + ";;");
                    expectingPattern = true;
                    ++idx;
                    continue;
                }
                if( trimmed.endsWith(";;") ) {
                    final String rest = trimmed.substring( 0, trimmed.length() - 2 ).trim();
                    if( !rest.isEmpty() ) out.add(bodyPrefix + rest);
                    out.add(bodyPrefix + ";;");
                    expectingPattern = true;
                    ++idx;
                    continue;
                }
                out.add(bodyPrefix + trimmed);
                ++idx;
                continue;
            }
            out.add( trimmed.isEmpty() ? "" : lines.get(idx) );
            ++idx;
        } // while

        return idx;
    }

    /**
     * A line is "pure" (eligible for structural rules 2.1/2.3/2.4) if its first non-whitespace
     *  character is real code -- this correctly excludes heredoc-body lines and full comment lines
     *  (both start with an 'O' character) without over-rejecting normal code lines that merely
     *  *contain* a quoted string later on (e.g. `case "$x" in`, whose first char 'c' is 'C').
     */
    private static boolean[] computeLinePurity(final String content, final char[] kind, final int lineCount)
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
            if( !found && c != ' ' && c != '\t' ) {
                pure[line] = kind[i] == 'C';
                found      = true;
            }
        } // for
        if( line < lineCount && !found ) pure[line] = true;

        return pure;
    }

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
        for(int i = 0; i < count; ++i) sb.append(c);

        return sb.toString();
    }

} // class BashSpecificRule
