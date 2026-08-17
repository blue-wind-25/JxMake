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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * STYLE_JXMAKE.md rule logic (JxMakeFile scripting language, `--lang jxmake`, `.jxm`/`JxMakeFile`).
 * Narrow, beautification-only scope like the rest of the tooling family (Makefile/Bash/PowerShell/
 * E-INI): four fixed rules -- (1) line-comment normalization (block comments untouched, only
 * uniformly shifted); (2) full forced reindent by block-keyword nesting depth; (3) backslash
 * continuation-line alignment; (4) assignment-operator alignment for contiguous same-depth
 * single-statement assignment lines -- everything else left byte-identical. A real character-level
 * tokenizer underlies string/comment/shell-exec recognition (see {@link #scanForComment}/
 * {@link #maskStringsOnly}) so none of these rules fire inside a raw/single/double/multiline string,
 * a block comment, or shell-exec command text.
 */
public final class JxMakeSpecificRule {

    private final int     indentWidth;
    private final boolean normalizeCommentStartCase;
    private final boolean normalizeCommentEndPeriod;
    private boolean normalizeCommentMultiSentenceCase = false;

    public JxMakeSpecificRule(
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

    // ---- Keyword tables (STYLE_JXMAKE.md §2) -------------------------------------------------

    private static final Set<String> OPENERS = new HashSet<>(
        Arrays.asList("function", "target", "for", "foreach", "while", "do", "repeat", "loop", ".macro")
    );
    private static final Set<String> CLOSERS = new HashSet<>(
        Arrays.asList(
            "endfunction", "endtarget", "endif", "endfor", "endforeach",
            "endwhile", "whilst", "until", "endloop", ".endmacro"
        )
    );
    private static final Set<String> ELIF_ELSE   = new HashSet<>( Arrays.asList("elif", "else") );
    private static final String[]    ASSIGN_OPS  = {":+=", "+:=", ":?=", "?:=", ":=", "+=", "?=", "="};
    private static final String[]    EXEC_MARKERS = {"-+@", "-@", "+@", "?@", "@"};

    // ---- Char-level scanning helpers ---------------------------------------------------------

    /** Index of the first unquoted `#` (line-comment start) on {@code line}, or -1 if none. */
    private static int scanForComment(final String line)
    {
        char quote = 0;
        int  i     = 0;
        while( i < line.length() ) {
            final char c = line.charAt(i);
            if(quote != 0) {
                if(quote == '`') {
                    if(c == '`') quote = 0;
                    ++i;
                    continue;
                }
                if( c == '\\' && i + 1 < line.length() ) { i += 2; continue; }
                if(c == quote) quote = 0;
                ++i;
                continue;
            }
            if(c == '`' || c == '\'' || c == '"') { quote = c; ++i; continue; }
            if(c == '#') return i;
            ++i;
        } // while

        return -1;
    }

    /** Replaces every quoted/raw-string span's interior (quotes kept) with spaces, same length as {@code code}. */
    private static String maskStringsOnly(final String code)
    {
        final StringBuilder out   = new StringBuilder( code.length() );
              char          quote = 0;
              int           i     = 0;
        while( i < code.length() ) {
            final char c = code.charAt(i);
            if(quote != 0) {
                if(quote == '`') {
                    if(c == '`') { out.append(c); quote = 0; ++i; continue; }
                    out.append(' ');
                    ++i;
                    continue;
                }
                if( c == '\\' && i + 1 < code.length() ) { out.append("  "); i += 2; continue; }
                if(c == quote) { out.append(c); quote = 0; ++i; continue; }
                out.append(' ');
                ++i;
                continue;
            }
            if(c == '`' || c == '\'' || c == '"') { quote = c; out.append(c); ++i; continue; }
            out.append(c);
            ++i;
        } // while

        return out.toString();
    }

    /** Leftmost, whitespace-bounded occurrence of an assign-op in {@code masked}; {start,end} or null. */
    private static int[] findAssignOp(final String masked)
    {
        for( int i = 0; i < masked.length(); ++i ) {
            for(final String op : ASSIGN_OPS) {
                if( masked.regionMatches(i, op, 0, op.length()) ) {
                    final boolean leftOk = (i == 0) || Character.isWhitespace( masked.charAt(i - 1) );
                    final int     end    = i + op.length();
                    final boolean rightOk = (end >= masked.length()) || Character.isWhitespace(
                        masked.charAt(end)
                    );
                    if(leftOk && rightOk) return new int[] {i, end};
                } // if
            } // for op
        } // for i

        return null;
    }

    private static String firstToken(final String trimmed)
    {
        int i = 0;
        if( i < trimmed.length() && trimmed.charAt(i) == '.' ) ++i;
        while( i < trimmed.length() && ( Character.isLetterOrDigit(
            trimmed.charAt(i)
        ) || trimmed.charAt(i) == '_' ) ) ++i;

        return trimmed.substring(0, i);
    }

    private static boolean endsWithContinuation(final String value)
    {
        return ToolingSharedRule.endsWithContinuation(value);
    }

    private static String indent(final int depth, final int width)
    {
        return ToolingSharedRule.indent(depth, width);
    }

    private static String repeatChar(final char c, final int count)
    {
        return ToolingSharedRule.repeatChar(c, count);
    }

    private static int firstNonWs(final String line)
    {
        int i = 0;
        while( i < line.length() && ( line.charAt(i) == ' ' || line.charAt(i) == '\t' ) ) ++i;

        return i;
    }

    private static String normalizeComment(final String body, final boolean multiSentence)
    {
        return ToolingCommentNormalizer.normalize(
            body, true, true, null, multiSentence
        );
    }

    // ---- Assignment alignment group (rule 4) -------------------------------------------------

    /**
     * Tracks one open `if`-`elif`-...-`else` chain while the main pass renders it, so that once
     * the matching `endif` is seen the keyword right-alignment sub-rule (STYLE_JXMAKE.md §2) can
     * right-pad every `if`/`elif`/`else` line in the chain to the widest keyword actually used.
     */
    private static final class IfChain {

        final List<int[]> entries = new ArrayList<>(); // { outIndex, keywordLen, baseIndentLen }
        int     maxKeywordLen = 0;
        boolean uniformOneLiner = true; // false once any branch lacks an inline "; body"/body

        void add(final int outIndex, final int keywordLen, final int baseIndentLen, final boolean hasInlineBody)
        {
            entries.add( new int[] { outIndex, keywordLen, baseIndentLen } );
            maxKeywordLen = Math.max( maxKeywordLen, keywordLen );
            if(!hasInlineBody) uniformOneLiner = false;
        }

    } // class IfChain

    /**
     * Right-pads every `if`/`elif`/`else` line recorded in {@code chain} so the keyword is
     * right-justified to the chain's widest keyword width, per STYLE_JXMAKE.md §2's
     * "`if`/`elif`/`else` keyword right-alignment" sub-rule -- but only when every branch in
     * the chain inlined its body on the same physical line (`chain.uniformOneLiner`); if even
     * one branch used the ordinary block form, the whole chain is left at plain depth indent.
     */
    private static void applyIfChainAlignment(final List<String> out, final IfChain chain)
    {
        if(!chain.uniformOneLiner) return;
        for(final int[] entry : chain.entries) {
            final int outIndex      = entry[0];
            final int keywordLen    = entry[1];
            final int baseIndentLen = entry[2];
            final int pad           = chain.maxKeywordLen - keywordLen;
            if(pad <= 0) continue;
            final String line = out.get(outIndex);
            out.set(
                outIndex,
                repeatChar( ' ', baseIndentLen + pad ) + line.substring(baseIndentLen)
            );
        } // for
    }

    private static final class AsgnItem {

        String localTok; // "local" or null
        String constTok; // "const" or null
        String varTok;   // var-name field (e.g. "CC" or "^RefA"), never null
        String op;
        String value;
        String comment; // null if none (already normalized, no leading '#')

    } // class AsgnItem

    /**
     * Splits an assignment statement's collapsed prefix (everything before the assign-op,
     * whitespace-collapsed) into its STYLE_JXMAKE.md §4 fields: optional `local`, optional
     * `const` (grammar order is always `local const`, never the reverse), then the var-name
     * (`^`-prefixed for indirect assignment, which never carries modifiers).
     */
    private static AsgnItem parsePrefixFields(final String prefixCollapsed)
    {
        final AsgnItem item = new AsgnItem();
        if( prefixCollapsed.startsWith("^") ) {
            item.varTok = prefixCollapsed;
            return item;
        } // if

        final String[] toks = prefixCollapsed.split(" ");
        if( toks.length == 3 && "local".equals(toks[0]) && "const".equals(toks[1]) ) {
            item.localTok = toks[0];
            item.constTok = toks[1];
            item.varTok   = toks[2];
        }
        else if( toks.length == 2 && "local".equals(toks[0]) ) {
            item.localTok = toks[0];
            item.varTok   = toks[1];
        }
        else if( toks.length == 2 && "const".equals(toks[0]) ) {
            item.constTok = toks[0];
            item.varTok   = toks[1];
        }
        else {
            item.varTok = toks[toks.length - 1];
        }

        return item;
    }

    // ---- Main entry point -----------------------------------------------------------------

    public String format(final String content)
    {
        final boolean      endsWithNewline = content.endsWith("\n");
        final String[]     rawArr          = content.split("\n", -1);
        final List<String> lines           = new ArrayList<>( Arrays.asList(rawArr) );
        if( endsWithNewline && !lines.isEmpty() ) lines.remove( lines.size() - 1 );

        final List<String>    out       = new ArrayList<>();
        final List<AsgnItem>  group     = new ArrayList<>();
              int             groupDepth = -1;

              int     idx      = 0;
              int     depth    = 0;
              boolean inMlString    = false;
              boolean inBlockComment = false;
              int     blockCommentDelta = 0;
              boolean pendingContinuation = false;
              boolean contIsAssignment   = false;
              int     contValueCol       = 0;
              int     contFallbackDepth  = 0;

        final Deque<IfChain> ifChainStack = new ArrayDeque<>();

        while( idx < lines.size() ) {
            final String line = lines.get(idx);

            // ---- Multiline-string opaque carry -------------------------------------------
            if(inMlString) {
                out.add(line);
                if( line.trim().equals("\"]]") ) inMlString = false;
                ++idx;
                continue;
            } // if

            // ---- Block-comment uniform-shift carry ---------------------------------------
            if(inBlockComment) {
                final int    oldLead = firstNonWs(line);
                final String shifted = repeatChar( ' ', Math.max( 0, oldLead + blockCommentDelta ) )
                        + line.substring(oldLead);
                out.add(shifted);
                if( line.contains("*)") ) inBlockComment = false;
                ++idx;
                continue;
            } // if

            // ---- Continuation-line rendering (rule 3) -------------------------------------
            if(pendingContinuation) {
                final String trimmed = line.trim();
                if( trimmed.isEmpty() ) {
                    out.add("");
                    pendingContinuation = false;
                    ++idx;
                    continue;
                } // if
                final String newLeading = contIsAssignment
                        ? repeatChar(' ', contValueCol)
                        : indent(contFallbackDepth + 1, indentWidth);
                out.add(newLeading + trimmed);
                pendingContinuation = endsWithContinuation(line);
                ++idx;
                continue;
            } // if

            final String trimmed     = line.trim();
            final int    oldLeadEnd  = firstNonWs(line);

            // ---- Blank line ----------------------------------------------------------------
            if( trimmed.isEmpty() ) {
                flushGroup(out, group, groupDepth, indentWidth);
                group.clear();
                groupDepth = -1;
                out.add("");
                ++idx;
                continue;
            } // if

            // ---- Standalone `#` comment-line chain (untouched indentation) -----------------
            if( trimmed.startsWith("#") ) {
                flushGroup(out, group, groupDepth, indentWidth);
                group.clear();
                groupDepth = -1;

                final List<String> prefixes = new ArrayList<>();
                final List<String> bodies   = new ArrayList<>();
                while( idx < lines.size() ) {
                    final String l  = lines.get(idx);
                    final String lt = l.trim();
                    if( !lt.startsWith("#") ) break;
                    final int lead = firstNonWs(l);
                    prefixes.add( l.substring(0, lead) );
                    bodies.add( l.substring(lead + 1) );
                    ++idx;
                } // while
                final List<Boolean> blanks = new ArrayList<>();
                for( int k = 0; k < bodies.size(); ++k ) blanks.add(false);
                final List<String> normalized = ToolingCommentNormalizer.normalizeChain(
                    bodies,
                    blanks,
                    normalizeCommentStartCase,
                    normalizeCommentEndPeriod,
                    null,
                    normalizeCommentMultiSentenceCase
                );
                for( int k = 0; k < normalized.size(); ++k ) out.add(
                    prefixes.get(k) + "#" + normalized.get(k)
                );
                continue;
            } // if

            // ---- Block comment opener (uniform shift as a unit) ----------------------------
            if( trimmed.startsWith("(*") ) {
                flushGroup(out, group, groupDepth, indentWidth);
                group.clear();
                groupDepth = -1;

                final int newLead = depth * indentWidth;
                final int delta   = newLead - oldLeadEnd;
                out.add( repeatChar( ' ', Math.max(0, newLead) ) + line.substring(oldLeadEnd) );
                if( !line.contains("*)") ) {
                    inBlockComment    = true;
                    blockCommentDelta = delta;
                } // if
                ++idx;
                continue;
            } // if

            // ---- Shell-exec marker line (opaque command text) ------------------------------
            String execMarker = null;
            for(final String m : EXEC_MARKERS) if( trimmed.startsWith(m) ) { execMarker = m; break; }
            if(execMarker != null) {
                flushGroup(out, group, groupDepth, indentWidth);
                group.clear();
                groupDepth = -1;

                out.add( indent(depth, indentWidth) + line.substring(oldLeadEnd) );
                ++idx;
                continue;
            } // if

            // ---- Ordinary statement line ----------------------------------------------------
            final int    commentIdx = scanForComment(line);
            final String codeOnly   = commentIdx < 0 ? line : line.substring(0, commentIdx);
            final String masked     = maskStringsOnly(codeOnly);
            final boolean hasSemicolon = masked.contains(";");
            final String  codeTrimmed  = codeOnly.trim();
            final String  token        = firstToken(codeTrimmed);

            // -- Keyword classification / depth bookkeeping --
            int     renderDepth;
            boolean isOpener = false;
            if( CLOSERS.contains(token) ) {
                depth       = Math.max(0, depth - 1);
                renderDepth = depth;
            }
            else if( ELIF_ELSE.contains(token) ) {
                renderDepth = Math.max(0, depth - 1);
            }
            else if( "if".equals(token) ) {
                final boolean oneliner = isOneLinerIf(lines, idx);
                renderDepth = depth;
                if(!oneliner) isOpener = true;
            }
            else if( OPENERS.contains(token) ) {
                renderDepth = depth;
                isOpener    = true;
            }
            else {
                renderDepth = depth;
            }
            final String newLeading = indent(renderDepth, indentWidth);
            final boolean thisContinues = endsWithContinuation(line);

            // -- Multiline-string opener (`[["` alone at end of line) --
            final boolean opensMlString = codeTrimmed.endsWith("[[\"") && !hasSemicolon && !thisContinues;

            if(opensMlString) {
                flushGroup(out, group, groupDepth, indentWidth);
                group.clear();
                groupDepth = -1;
                out.add( newLeading + line.substring(oldLeadEnd) );
                inMlString = true;
                if(isOpener) ++depth;
                ++idx;
                continue;
            } // if

            // -- Assignment detection (only single-statement, non-multi-statement lines) --
            int[] opRange = null;
            if( !hasSemicolon && !CLOSERS.contains(token) && !ELIF_ELSE.contains(token)
                    && !"if".equals(token) && !OPENERS.contains(token) ) {
                opRange = findAssignOp(masked);
            } // if

            if(opRange != null) {
                final String prefixRaw = codeOnly.substring(0, opRange[0]).trim();
                final String opText    = codeOnly.substring( opRange[0], opRange[1] );
                int valueStart = opRange[1];
                while( valueStart < codeOnly.length()
                        && Character.isWhitespace( codeOnly.charAt(valueStart) ) ) ++valueStart;

                if(thisContinues) {
                    flushGroup(out, group, groupDepth, indentWidth);
                    group.clear();
                    groupDepth = -1;
                    out.add( newLeading + line.substring(oldLeadEnd) );
                    pendingContinuation = true;
                    contIsAssignment    = true;
                    contValueCol        = newLeading.length() + (valueStart - oldLeadEnd);
                    ++idx;
                    continue;
                } // if

                if( !prefixRaw.isEmpty() ) {
                    if( groupDepth != -1 && groupDepth != renderDepth ) {
                        flushGroup(out, group, groupDepth, indentWidth);
                        group.clear();
                    } // if
                    groupDepth = renderDepth;

                    final AsgnItem item = parsePrefixFields( prefixRaw.replaceAll("\\s+", " ") );
                    item.op      = opText;
                    item.value   = codeOnly.substring(valueStart).trim();
                    item.comment = (commentIdx < 0) ? null : normalizeComment(
                        line.substring(commentIdx + 1), normalizeCommentMultiSentenceCase
                    );
                    group.add(item);
                    if(isOpener) ++depth;
                    ++idx;
                    continue;
                } // if
            } // if

            // -- Generic statement line (keyword/target/exec-adjacent/plain) --
            flushGroup(out, group, groupDepth, indentWidth);
            group.clear();
            groupDepth = -1;

            final String codePart = codeOnly.substring(oldLeadEnd);
            final String rendered;
            if(commentIdx < 0) {
                rendered = newLeading + line.substring(oldLeadEnd);
            }
            else {
                final String normalized = normalizeComment(
                    line.substring(commentIdx + 1), normalizeCommentMultiSentenceCase
                );
                rendered = newLeading + codePart + "#" + normalized;
            }
            out.add(rendered);
            final int renderedIndex = out.size() - 1;

            // -- if/elif/else keyword right-alignment bookkeeping (STYLE_JXMAKE.md §2) --
            if( "if".equals(token) && isOpener ) {
                final IfChain chain = new IfChain();
                chain.add( renderedIndex, token.length(), newLeading.length(), hasSemicolon );
                ifChainStack.push(chain);
            }
            else if( ELIF_ELSE.contains(token) && !ifChainStack.isEmpty() ) {
                final boolean hasInlineBody = "else".equals(token)
                        ? !codeTrimmed.substring(4).trim().isEmpty()
                        : hasSemicolon;
                ifChainStack.peek().add( renderedIndex, token.length(), newLeading.length(), hasInlineBody );
            }
            else if( "endif".equals(token) && !ifChainStack.isEmpty() ) {
                applyIfChainAlignment( out, ifChainStack.pop() );
            } // else if

            if(thisContinues) {
                pendingContinuation = true;
                contIsAssignment    = false;
                contFallbackDepth   = renderDepth;
            }
            else if(isOpener) {
                ++depth;
            }
            ++idx;
        } // while

        flushGroup(out, group, groupDepth, indentWidth);

        final StringBuilder sb = new StringBuilder();
        for( int i = 0; i < out.size(); ++i ) {
            sb.append( out.get(i) );
            if( i + 1 < out.size() || endsWithNewline ) sb.append('\n');
        }

        return sb.toString();
    }

    private void flushGroup(
        final List<String>   out,
        final List<AsgnItem> group,
        final int            groupDepth,
        final int            indentWidth
    )
    {
        if( group.isEmpty() ) return;
        boolean hasLocal = false;
        boolean hasConst = false;
        int     varWidth = 0;
        for(final AsgnItem item : group) {
            if(item.localTok != null) hasLocal = true;
            if(item.constTok != null) hasConst = true;
            varWidth = Math.max( varWidth, item.varTok.length() );
        } // for
        final int    localWidth = hasLocal ? "local".length() : 0;
        final int    constWidth = hasConst ? "const".length() : 0;
        final String leading    = indent( Math.max(0, groupDepth), indentWidth );

        for(final AsgnItem item : group) {
            final StringBuilder sb = new StringBuilder(leading);
            if(localWidth > 0) {
                final String tok = item.localTok == null ? "" : item.localTok;
                sb.append(tok).append( repeatChar( ' ', localWidth - tok.length() ) ).append(' ');
            } // if
            if(constWidth > 0) {
                final String tok = item.constTok == null ? "" : item.constTok;
                sb.append(tok).append( repeatChar( ' ', constWidth - tok.length() ) ).append(' ');
            } // if
            sb.append( item.varTok ).append( repeatChar( ' ', varWidth - item.varTok.length() ) );
            sb.append(' ').append(item.op).append(' ').append(item.value);
            if(item.comment != null) sb.append(" #").append(item.comment);
            out.add( sb.toString() );
        } // for
        group.clear();
    }

    /**
     * Scans the logical line starting at {@code startIdx} (joining `\`-continued physical lines)
     * for a top-level (bracket/paren depth 0, outside any string/comment) `:` token -- distinguishes
     * an {@code if} one-liner (STYLE_JXMAKE.md §2) from a block-form {@code if}.
     */
    private static boolean isOneLinerIf(final List<String> lines, final int startIdx)
    {
        int depth = 0;
        int i     = startIdx;
        while( i < lines.size() ) {
            final String raw        = lines.get(i);
            final int    commentIdx = scanForComment(raw);
            final String code       = commentIdx < 0 ? raw : raw.substring(0, commentIdx);
            final String masked     = maskStringsOnly(code);
            for( int c = 0; c < masked.length(); ++c ) {
                final char ch = masked.charAt(c);
                if(ch == '(' || ch == '[' || ch == '{') ++depth;
                else if(ch == ')' || ch == ']' || ch == '}') --depth;
                else if(ch == ';') return false; // ';' starts a new statement -- stop scanning
                else if(ch == ':' && depth <= 0) {
                    // Exclude ':=' / ':+=' / ':?=' assign-op colons -- not the one-liner separator.
                    final char next = (c + 1 < masked.length()) ? masked.charAt(c + 1) : '\0';
                    if( next != '=' && next != '+' && next != '?' ) return true;
                } // else if
            } // for c
            if( !endsWithContinuation(raw) ) break;
            ++i;
        } // while

        return false;
    }

} // class JxMakeSpecificRule
