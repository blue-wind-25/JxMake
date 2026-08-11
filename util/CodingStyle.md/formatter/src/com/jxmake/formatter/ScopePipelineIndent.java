/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jxmake.formatter.evaluator.PythonBracketComplexityEvaluator;
import com.jxmake.formatter.rules.MiscRuleIndent;
import com.jxmake.formatter.rules.MiscRuleIndent.PyAssignment;
import com.jxmake.formatter.rules.MiscRuleIndent.PyImport;
import com.jxmake.formatter.rules.MiscRuleIndent.PyParam;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;
import com.jxmake.formatter.tokenizer.TokenizerIndent;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isKeyword;

/**
 * Indentation-block-family scope pipeline (Python3 -- see STATE_PYTHON3.md). Tokenizes via
 * {@link TokenizerIndent}, applies STYLE_PYTHON3.md §2's assignment-alignment pass ({@link
 * #applyAssignmentAlignment}) and §3's import-ordering pass ({@link #applyImportSort}), and
 * renders the (possibly-replaced) token stream back to source text -- every token kind's original
 * text passes through verbatim except a grouped assignment's own `target...value` span (replaced
 * with its padded/aligned rendering), a grouped run of import statements (replaced with the same
 * lines re-ordered, each line's own text otherwise untouched), and the synthesized zero-text
 * {@code INDENT}/{@code DEDENT} markers (always skipped). Further §4-9 rule passes land as later
 * calls inside {@link #process}, mirroring {@link ScopePipelineCurly}'s own multi-pass shape.
 */
public final class ScopePipelineIndent extends ScopePipelineCore {

    private final Lang                             lang;
    private final MiscRuleIndent                   miscRule;
    private final PythonBracketComplexityEvaluator bracketEval = new PythonBracketComplexityEvaluator();
    private final int                              lineLength;
    private final boolean                          pythonImportSort;
    private final int                              pythonImportBlankLines;

    public ScopePipelineIndent(final Lang lang, final int indentWidth)
    {
        this(lang, indentWidth, Config.DEFAULT_LINE_LENGTH);
    }

    public ScopePipelineIndent(final Lang lang, final int indentWidth, final int lineLength)
    {
        this(lang, indentWidth, lineLength, true, 1);
    }

    /**
     * {@code pythonImportSort}/{@code pythonImportBlankLines} gate/parameterize §3's import-
     *  ordering pass ({@link #applyImportSort}) -- see {@code Config#isPythonImportSort}/
     *  {@code Config#pythonImportBlankLines}. The two-/three-arg constructors above default both
     *  to their {@code Config} default (on / 1) for any caller that doesn't need to thread a real
     *  {@code Config} through (kept for backward compatibility). This constructor also leaves
     *  comment-normalization (see the 10-arg constructor below) off, matching its own prior
     *  behavior before that pass existed -- no other in-tree caller besides test code needs it.
     */
    public ScopePipelineIndent(
        final Lang    lang,
        final int     indentWidth,
        final int     lineLength,
        final boolean pythonImportSort,
        final int     pythonImportBlankLines
    )
    {
        this(lang, indentWidth, lineLength, pythonImportSort, pythonImportBlankLines,
                false, false, false, false, "");
    }

    /**
     * Full constructor additionally threading the `normalize-comment-start-case`/
     *  `normalize-comment-end-period`/`comment-normalization-classifier`/`gru-classifier`/
     *  `gru-weights-path` config values through to {@link #miscRule} (2026-08-08 session, comment
     *  normalization for python3 -- see STATE_PYTHON3.md), the same classifier/GRU-backed decision
     *  path the curly family's own {@code MiscRuleCurly#enforceCommentStyle} already uses via
     *  {@code MiscRuleCore#capitalizeFirstLetter(String)}/{@code #stripSoleTrailingPeriodAcrossLines}
     *  -- {@link MiscRuleIndent} needed no new classifier-integration code of its own, only its own
     *  {@code #}-comment chain-grouping ({@link MiscRuleIndent#computeHashCommentGroups}) built on
     *  top of those already-shared, already-gated primitives. {@link #applyCommentNormalization}
     *  is the new pass this constructor enables.
     */
    public ScopePipelineIndent(
        final Lang    lang,
        final int     indentWidth,
        final int     lineLength,
        final boolean pythonImportSort,
        final int     pythonImportBlankLines,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean commentNormalizationClassifier,
        final boolean gruClassifier,
        final String  gruWeightsPath
    )
    {
        super(indentWidth);
        this.lang                   = lang;
        this.miscRule               = new MiscRuleIndent(
            lang, normalizeCommentStartCase, normalizeCommentEndPeriod, commentNormalizationClassifier,
            gruClassifier, gruWeightsPath, indentWidth, 0
        );
        this.lineLength             = lineLength;
        this.pythonImportSort       = pythonImportSort;
        this.pythonImportBlankLines = pythonImportBlankLines;
    }

    @Override
    public String process(final String source)
    {
        final List<Token>       tokens       = new TokenizerIndent(lang).tokenize(source);
        final List<RawLine>     rawLines     = splitRawLines(tokens);
        final List<Replacement> replacements = new ArrayList<>();
        replacements.addAll( applyAssignmentAlignment(tokens, rawLines) );
        replacements.addAll( applyImportSort(tokens, rawLines) );
        replacements.addAll( applyDecoratorSpacing(tokens, rawLines) );
        replacements.addAll( applyFStringSpacing(tokens) );
        replacements.addAll( applySignatureAlignment(tokens, rawLines) );
        replacements.addAll( applyCaseColonAlignment(tokens, rawLines) );
        replacements.addAll( applySingleStatementBody(tokens, rawLines) );
        replacements.addAll( applyControlFlowBlankLines(tokens, rawLines) );
        replacements.addAll( applyCommentNormalization(tokens) );
        // Ties on `start` must put zero-width insertions (start == end, e.g. §9's blank-line-before
        // insertion) ahead of any wider, token-consuming replacement at that same position (e.g. §8's
        // header-rewriting join) -- a zero-width entry renders without advancing the render() cursor,
        // so ordering it first lets both compose (blank line inserted, then the join's own rewritten
        // text follows); ordering it second would let the wider replacement's jump-ahead strand the
        // zero-width entry as stale and silently drop it (see render()'s own javadoc for the general
        // stale-entry posture this preserves).
        replacements.sort( Comparator.<Replacement>comparingInt(r -> r.start)
                .thenComparingInt(r -> r.end - r.start) );

        return render(tokens, replacements);
    }

    /**
     * `normalize-comment-start-case`/`normalize-comment-end-period` for python3's `#` comments
     *  (2026-08-08 session -- see STATE_PYTHON3.md; previously not wired up for python3 at all).
     *  Delegates the actual chain-grouping/normalization decision entirely to {@link
     *  MiscRuleIndent#computeHashCommentGroups}, then turns each changed {@code COMMENT_LINE}
     *  token into a single-token {@link Replacement} (a comment token is never split across
     *  physical lines, so `[idx, idx+1)` always exactly covers it). A group member whose
     *  normalized body is identical to its current body (nothing to change, or the config keys/
     *  classifier gate left it untouched) is skipped -- keeps this pass's own replacement count
     *  proportional to actual changes, same posture as every other pass in this class.
     */
    private List<Replacement> applyCommentNormalization(final List<Token> tokens)
    {
        final List<Replacement>    replacements = new ArrayList<>();
        final Map<Integer, String> groups       = miscRule.computeHashCommentGroups(tokens);
        for( final Map.Entry<Integer, String> e : groups.entrySet() ) {
            final int    idx  = e.getKey();
            final String body = e.getValue();
            if( !("#" + body).equals( tokens.get(idx).text ) ) replacements.add(
                new Replacement(idx, idx + 1, "#" + body)
            );
        } // for

        return replacements;
    }

    /**
     * One logical line: {@code [start, end)} is its full token range including its own leading
     *  {@code INDENT}/{@code DEDENT} markers (if any) and its terminating {@code NEWLINE} (absent
     *  only for a file's last line); {@code contentStart} is the first token after those leading
     *  markers/whitespace. {@code depth} is the indentation depth this line's own statement lives
     *  at, after applying its own leading markers. A line whose range spans more than one {@code
     *  NEWLINE} token (bracket/backslash continuation) is a multi-physical-line statement -- every
     *  rule pass in this class treats those as unrecognized (out of scope for this slice, same
     *  "documented gap, not a guess" precedent used throughout this job) rather than guessing at
     *  their shape.
     */
    private static final class RawLine {

        final int     start;
        final int     end;
        final int     contentStart;
        final int     depth;
        final boolean multiPhysicalLine;

        RawLine(
            final int     start,
            final int     end,
            final int     contentStart,
            final int     depth,
            final boolean multiPhysicalLine
        )
        {
            this.start             = start;
            this.end               = end;
            this.contentStart      = contentStart;
            this.depth             = depth;
            this.multiPhysicalLine = multiPhysicalLine;
        }

    } // class RawLine

    private List<RawLine> splitRawLines(final List<Token> tokens)
    {
        final List<RawLine> lines = new ArrayList<>();
        final int           n     = tokens.size();
              int           i     = 0;
              int           depth = 0;
        while(i < n) {
            int p = i;
            if( p < n && tokens.get(p).type == TokenType.WHITESPACE ) p++;
            while( p < n && tokens.get(p).type == TokenType.INDENT ) {
                ++depth;
                ++p;
            }
            while( p < n && tokens.get(p).type == TokenType.DEDENT ) {
                --depth;
                ++p;
            }
            final int     contentStart = p;
                  int     j            = p;
                  boolean multi        = false;
            while(j < n) {
                final Token t = tokens.get(j);
                if(t.type == TokenType.NEWLINE) {
                    final boolean insideBrackets = t.parenDepth > 0;
                    final boolean backslash      = j > 0 && tokens.get(
                        j - 1
                    ).type == TokenType.OP && "\\".equals(
                        tokens.get(j - 1).text
                    );
                    ++j;
                    if(!insideBrackets && !backslash) break;
                    multi = true;
                    continue;
                } // if
                ++j;
            } // while
            lines.add( new RawLine(i, j, contentStart, depth, multi) );
            i = j;
        } // while

        return lines;
    }

    private int nextSignificant(final List<Token> tokens, final int from, final int to)
    {
        int i = from;
        while( i < to && isGapToken( tokens.get(i) ) ) i++;

        return i < to ? i : -1;
    }

    /**
     * Same as {@link #nextSignificant}, but also skips a backslash line-continuation OP token (a
     *  lone {@code "\"} is never a real Python operator elsewhere -- it's only ever emitted as an
     *  OP token by {@code TokenizerIndent} for this exact continuation use, per {@code
     *  TokenizerIndent#isBackslashContinuation}'s own javadoc, so treating it as transparent here is
     *  safe). {@link #isGapToken} itself can't be widened for this because it's shared with every
     *  other pass, where a backslash's presence still matters (e.g. §2's own {@code
     *  RawLine#multiPhysicalLine} boundary detection). Used by {@link #classifyImport}'s comma-list
     *  parsing so a backslash-continued `import a, \` / `from x import a, \` continuation's comma
     *  is followed correctly onto the next physical line's first name, instead of the `\` token
     *  itself being mistaken for the next name and causing a safe-bail (never a crash, but a real
     *  coverage gap this method exists to close).
     */
    private int nextSignificantSkipBackslash(final List<Token> tokens, final int from, final int to)
    {
        int i = nextSignificant(tokens, from, to);
        while( i >= 0 && i < to && tokens.get(
            i
        ).type == TokenType.OP && "\\".equals(
            tokens.get(i).text
        ) ) i = nextSignificant(
            tokens, i + 1, to
        );

        return i;
    }

    // ── §2: Assignment Alignment ─────────────────────────────────────────────────────

    /**
     * Classifies each {@code rawLines} entry as a §2 assignment candidate or not, then groups
     *  consecutive same-depth candidates (a blank line, a comment-only line, a depth change, or a
     *  non-candidate statement all break the group -- STYLE_PYTHON3.md §2: "a blank line or a
     *  comment breaks the group") and returns one {@link Replacement} per grouped assignment,
     *  replacing only its own `target...value` span -- the line's own indentation, surrounding
     *  blank lines/comments, and any trailing same-line comment are left untouched.
     */
    private List<Replacement> applyAssignmentAlignment(
        final List<Token>   tokens,
        final List<RawLine> rawLines
    )
    {
        final List<Replacement>  replacements = new ArrayList<>();
              List<PyAssignment> group        = new ArrayList<>();
              List<int[]>        groupSpans   = new ArrayList<>(); // [assignStart, assignEnd] per group member
              int                groupDepth   = -1;
        for(final RawLine line : rawLines) {
            final PyAssignment a = line.multiPhysicalLine ? null : classifyAssignment(tokens, line);
            if( a != null && ( group.isEmpty() || line.depth == groupDepth ) ) {
                group.add(a);
                groupSpans.add(
                    new int[] { indexOf(tokens, a.target), lastIndexOfValue(tokens, a) }
                );
                groupDepth = line.depth;
                continue;
            } // if
            flushAssignmentGroup(group, groupSpans, replacements);
            group      = new ArrayList<>();
            groupSpans = new ArrayList<>();
            if(a != null) {
                group.add(a);
                groupSpans.add(
                    new int[] { indexOf(tokens, a.target), lastIndexOfValue(tokens, a) }
                );
                groupDepth = line.depth;
            } // if
            else {
                groupDepth = -1;
            }
        } // for
        flushAssignmentGroup(group, groupSpans, replacements);

        return replacements;
    }

    private int indexOf(final List<Token> tokens, final Token target)
    {
        for( int i = 0; i < tokens.size(); ++i ) {
            if( tokens.get(i) == target ) return i;
        }

        return -1;
    }

    private int lastIndexOfValue(final List<Token> tokens, final PyAssignment a)
    {
        final Token last = a.valueTokens.get( a.valueTokens.size() - 1 );
        for( int i = tokens.size() - 1; i >= 0; --i ) {
            if( tokens.get(i) == last ) return i + 1;
        }

        return -1;
    }

    private void flushAssignmentGroup(
        final List<PyAssignment> group,
        final List<int[]>        spans,
        final List<Replacement>  replacements
    )
    {
        if( group.isEmpty() ) return;
        final List<String> rendered = miscRule.renderPyGroup(group);
        for( int i = 0; i < group.size(); ++i ) replacements.add(
            new Replacement( spans.get(i)[0], spans.get(i)[1], rendered.get(i) )
        );
    }

    /**
     * Classifies one line's content range as a §2 assignment candidate. Rejects unless: the first
     *  significant token is a bare {@code IDENTIFIER}, the next significant token is an {@code OP}
     *  in {@link com.jxmake.formatter.rules.MiscRuleCore#ASSIGNMENT_OPS}, and at least one
     *  significant, non-comment value token follows.
     */
    private PyAssignment classifyAssignment(final List<Token> tokens, final RawLine line)
    {
        final int targetIdx = nextSignificant(tokens, line.contentStart, line.end);
        if( targetIdx < 0 || tokens.get(targetIdx).type != TokenType.IDENTIFIER ) return null;
        final int opIdx = nextSignificant(tokens, targetIdx + 1, line.end);
        if( opIdx < 0 || tokens.get(
            opIdx
        ).type != TokenType.OP || !MiscRuleIndent.isAssignmentOp(
            tokens.get(opIdx).text
        ) ) return null;
        final int valueFrom = nextSignificant(tokens, opIdx + 1, line.end);
        if(valueFrom < 0) return null;
        int lastValueIdx = -1;
        for(int k = line.end - 1; k >= valueFrom; --k) {
            final Token t = tokens.get(k);
            if(t.type == TokenType.IDENTIFIER || t.type == TokenType.KEYWORD || t.type == TokenType.NUMBER
                    || t.type == TokenType.STRING || t.type == TokenType.CHAR || t.type == TokenType.OP
                    || t.type == TokenType.PUNCT) {
                lastValueIdx = k;
                break;
            }
        } // for
        if(lastValueIdx < 0) return null;

        return new PyAssignment(
            tokens.get(targetIdx), tokens.get(opIdx),
            tokens.subList(valueFrom, lastValueIdx + 1)
        );
    }

    // ── §3: Import Ordering ──────────────────────────────────────────────────────────

    /**
     * Classifies each {@code rawLines} entry as a §3 import candidate or not, groups consecutive
     *  candidates at the same depth (a blank line, a comment-only line, a depth change, or a
     *  non-import statement all break the group -- STYLE_PYTHON3.md §3.2's "any non-import
     *  statement breaks the group" plus this slice's own conservative extension to blank/comment
     *  lines, since the style doc is silent on how a blank line or an attached comment should
     *  move when statements are physically reordered around it; treating them as group boundaries
     *  sidesteps that ambiguity entirely rather than guessing), stable-sorts each group per §3.1/
     *  §3.3, and returns one {@link Replacement} per group covering the whole group's line range,
     *  replacing it with the same lines' own verbatim text (indentation, content, trailing
     *  same-line comment) in the new order.
     *
     * <p>Gated on {@code pythonImportSort} (RDD_KEY_247): when off, the whole pass -- both this
     *  reordering and {@link #applyImportGroupBlankLines}'s blank-line normalization below -- is a
     *  complete no-op, mirroring the Java/JS-family precedent's own "off" posture for its own
     *  sort flag (see {@code Config#isJavaImportSort}/{@code Config#isJsImportSort}).
     */
    private List<Replacement> applyImportSort(
        final List<Token>   tokens,
        final List<RawLine> rawLines
    )
    {
        if(!pythonImportSort) return new ArrayList<>();

        final List<Replacement> replacements = new ArrayList<>();
        final List<int[]>       groupRanges  = new ArrayList<>(); // [startIdx, endIdx, depth) into rawLines
              List<RawLine>     group        = new ArrayList<>();
              List<PyImport>    groupImports = new ArrayList<>();
              int               groupDepth   = -1;
              int               groupStart   = -1;
        final int               n            = rawLines.size();
        for(int idx = 0; idx < n; ++idx) {
            final RawLine line = rawLines.get(idx);
            // Unlike §2's applyAssignmentAlignment, a multiPhysicalLine candidate is no longer
            // rejected outright here -- classifyImport itself now recognizes a parenthesized
            // `from X import (...)` (possibly spanning many physical lines) and a backslash-
            // continued `import a, \` form, each safely (see classifyImport's own javadoc for the
            // per-shape safety guarantees).
            final PyImport imp = classifyImport(tokens, line);
            if( imp != null && ( group.isEmpty() || line.depth == groupDepth ) ) {
                if( group.isEmpty() ) groupStart = idx;
                group.add(line);
                groupImports.add(imp);
                groupDepth = line.depth;
                continue;
            } // if
            if( !group.isEmpty() ) {
                flushImportGroup(tokens, group, groupImports, replacements);
                groupRanges.add( new int[] { groupStart, idx, groupDepth } );
            }
            group        = new ArrayList<>();
            groupImports = new ArrayList<>();
            if(imp != null) {
                groupStart = idx;
                group.add(line);
                groupImports.add(imp);
                groupDepth = line.depth;
            }
            else {
                groupDepth = -1;
            }
        } // for
        if( !group.isEmpty() ) {
            flushImportGroup(tokens, group, groupImports, replacements);
            groupRanges.add( new int[] { groupStart, n, groupDepth } );
        }

        replacements.addAll( applyImportGroupBlankLines(tokens, rawLines, groupRanges) );

        return replacements;
    }

    /**
     * STYLE_PYTHON3.md §3.2's {@code python-import-blank-lines}, mirroring {@code
     *  JsTsSpecificRule#renderImportSegment}/{@code JavaSpecificRule#enforceImportOrdering}'s own
     *  "insert {@code blankLines} blank lines between adjacent groups" shape (RDD_KEY_247) -- but
     *  scoped narrowly to the one case unambiguous for Python's own (bucket-less, purely
     *  adjacency-based) grouping: two consecutive recognized import groups at the SAME depth,
     *  separated ONLY by blank physical line(s) (no comment, no depth change, no other statement
     *  in between -- {@link #applyImportSort}'s own group-boundary rules guarantee any same-depth
     *  boundary has at least one such line between the two groups, so this never fires on a
     *  zero-length gap). A gap containing a comment or spanning a depth change is left completely
     *  untouched -- normalizing blank-line count across a comment or into/out of a nested scope
     *  isn't what this key documents, and STYLE_PYTHON3.md's own worked example never shows either
     *  shape.
     */
    private List<Replacement> applyImportGroupBlankLines(
        final List<Token>   tokens,
        final List<RawLine> rawLines,
        final List<int[]>   groupRanges
    )
    {
        final List<Replacement> replacements = new ArrayList<>();
        for( int g = 0; g + 1 < groupRanges.size(); ++g ) {
            final int[] cur  = groupRanges.get(g);
            final int[] next = groupRanges.get(g + 1);
            if( cur[2] != next[2] ) continue; // Different depth -- not a plain blank-line boundary
            final int gapFrom = cur[1];
            final int gapTo   = next[0];
            if(gapFrom >= gapTo) continue; // No gap at all (shouldn't happen at equal depth, defensive)
            boolean allBlank = true;
            for(int idx = gapFrom; idx < gapTo; ++idx) {
                if( !isBlankLine( tokens, rawLines.get(idx) ) ) {
                    allBlank = false;
                    break;
                }
            }
            if(!allBlank) continue;
            final int currentBlankLines = gapTo - gapFrom;
            if(currentBlankLines == pythonImportBlankLines) continue;
            final StringBuilder text = new StringBuilder();
            for(int b = 0; b < pythonImportBlankLines; ++b) text.append('\n');
            replacements.add(
                new Replacement(
                    rawLines.get(gapFrom).start, rawLines.get(gapTo - 1).end, text.toString()
                )
            );
        } // for g

        return replacements;
    }

    /**
     * True iff {@code line} has no significant content at all -- a bare blank physical line (its
     *  own {@code [contentStart, end)} span holds nothing but {@code WHITESPACE}/{@code NEWLINE}).
     *  A comment-only line returns {@code false} (a {@code COMMENT_LINE}/{@code COMMENT_BLOCK}
     *  token is significant for this check even though {@link
     *  com.jxmake.formatter.tokenizer.TokenizerCore.Token#isGapToken} treats it as a gap token for
     *  other purposes) -- {@link #applyImportGroupBlankLines} must never conflate the two.
     */
    private boolean isBlankLine(final List<Token> tokens, final RawLine line)
    {
        for(int i = line.contentStart; i < line.end; ++i) {
            final TokenType type = tokens.get(i).type;
            if(type != TokenType.WHITESPACE && type != TokenType.NEWLINE) return false;
        }

        return true;
    }

    private void flushImportGroup(
        final List<Token>       tokens,
        final List<RawLine>     group,
        final List<PyImport>    imports,
        final List<Replacement> replacements
    )
    {
        if( group.isEmpty() ) return;
          boolean      anyChange = false;
    final List<String> lineTexts = new ArrayList<>();
        for( int i = 0; i < group.size(); ++i ) {
            final RawLine      line        = group.get(i);
            final PyImport     imp         = imports.get(i);
            final List<String> sortedUnits = sortedNameUnits(imp);
            if(sortedUnits == null) {
                lineTexts.add( verbatimLineText(tokens, line.start, line.end) );
                continue;
            }
            anyChange = true;
            final StringBuilder text = new StringBuilder();
            text.append( verbatimLineText(tokens, line.start, imp.nameListStart) );
            for( int u = 0; u < sortedUnits.size(); ++u ) {
                if(u > 0) text.append(", ");
                text.append( sortedUnits.get(u) );
            }
            text.append( verbatimLineText(tokens, imp.nameListEnd, line.end) );
            lineTexts.add( text.toString() );
        } // for i
        final List<Integer> order = new ArrayList<>();
        for( int i = 0; i < imports.size(); ++i ) order.add(i);
        order.sort( Comparator.comparing( (Integer idx) -> imports.get(idx) ) );
        for( int i = 0; i < order.size(); ++i ) {
            if( order.get(i) != i ) {
                anyChange = true;
                break;
            }
        }
        if(!anyChange) return;
        final StringBuilder text = new StringBuilder();
        for(final int idx : order) text.append( lineTexts.get(idx) );
        replacements.add(
            new Replacement( group.get(0).start, group.get( group.size() - 1 ).end, text.toString() )
        );
    }

    /**
     * Returns {@code imp}'s {@code nameUnitTexts} reordered to match its own sorted {@code names},
     *  or {@code null} if {@code imp} has no name list ({@code Kind.IMPORT}) or its names are
     *  already in sorted order (nothing to rebuild).
     */
    private List<String> sortedNameUnits(final PyImport imp)
    {
        if( imp.nameListStart < 0 || imp.names.size() < 2 ) return null;
        final List<Integer> order = new ArrayList<>();
        for( int i = 0; i < imp.names.size(); ++i ) order.add(i);
        order.sort( Comparator.comparing( (Integer idx) -> imp.names.get(idx) ) );
        boolean alreadySorted = true;
        for( int i = 0; i < order.size(); ++i ) {
            if( order.get(i) != i ) {
                alreadySorted = false;
                break;
            }
        }
        if(alreadySorted) return null;
        final List<String> out = new ArrayList<>();
        for(final int idx : order) out.add( imp.nameUnitTexts.get(idx) );

        return out;
    }

    private String verbatimLineText(final List<Token> tokens, final int start, final int end)
    {
        final StringBuilder sb = new StringBuilder();
        for(int i = start; i < end; ++i) {
            final Token t = tokens.get(i);
            if(t.type != TokenType.INDENT && t.type != TokenType.DEDENT) sb.append(t.text);
        }

        return sb.toString();
    }

    /**
     * Classifies one line (now including a {@link RawLine#multiPhysicalLine} candidate) as a §3
     *  import candidate: `import dotted[.dotted...][ as alias][, ...]` (single- or multi-module, the
     *  latter on one physical line or backslash-continued) or `from [.[.[...]]][dotted] import
     *  (name[ as alias][, ...] | *)` (bare comma-list or a parenthesized, possibly multi-physical-
     *  line, list). Every shape's within-clause name list is still only reordered when doing so can't
     *  silently drop content: a parenthesized list containing any comment token anywhere between its
     *  own `(`/`)` disables {@code nameListStart}/{@code nameListEnd} (kept at {@code -1}, matching
     *  the pre-existing "no name-list to rebuild" shape {@code sortedNameUnits} already treats as a
     *  no-op) -- {@code names}/{@code moduleName}/{@code kind} are still populated, so the statement
     *  still participates in cross-statement group sorting and always moves as one verbatim block
     *  (comments, original per-line layout, and comma/parenthesis style all preserved exactly) via
     *  {@link #flushImportGroup}'s existing "no sortedUnits -- reproduce the whole line verbatim"
     *  path. A backslash-continued list can never carry a mid-list comment (the backslash must be the
     *  physical line's last character, which a `#` comment would swallow, guaranteeing a syntax
     *  error) so no equivalent guard is needed there. Any shape this method can't confidently parse
     *  (malformed brackets, unrecognized trailing tokens, empty parens) returns {@code null} --
     *  §3.2's group-boundary rule then treats the whole line as an ordinary non-import statement,
     *  safely excluding it from grouping/sorting without corrupting it.
     */
    private PyImport classifyImport(final List<Token> tokens, final RawLine line)
    {
        final int kwIdx = nextSignificant(tokens, line.contentStart, line.end);
        if( kwIdx < 0 || tokens.get(kwIdx).type != TokenType.KEYWORD ) return null;
        final Token kw = tokens.get(kwIdx);
        if( isKeyword(kw, "import") ) {
            final int firstIdx = nextSignificant(tokens, kwIdx + 1, line.end);
            if( firstIdx < 0 || tokens.get(firstIdx).type != TokenType.IDENTIFIER ) return null;
            final List<String> names         = new ArrayList<>();
            final List<String> unitTexts     = new ArrayList<>();
            final int          nameListStart = firstIdx;
                  int          lastUnitEnd   = firstIdx;
                  int          q             = firstIdx;
            while(true) {
                final int    unitStart  = q;
                final String moduleName = readDottedName(tokens, q, line.end);
                if( moduleName == null || moduleName.isEmpty() ) return null;
                int after = advancePastDottedName(tokens, q, line.end);
                // AdvancePastDottedName returns -1 when the dotted name is the last significant
                // token on the line (e.g. the final module in `import c, a`, or a lone `import os`)
                // -- unlike `after`, which stays -1 for the control-flow checks below (matching the
                // pre-existing single-module code's own >= 0 guards), `unitEnd` must never be -1 or
                // include the trailing NEWLINE/whitespace gap: scan backward for the name's own last
                // significant token instead of defaulting to `line.end` (which would splice the
                // physical line's terminating NEWLINE text into this unit, corrupting the rebuild).
                int unitEnd = after >= 0 ? after : prevSignificant(
                    tokens, line.end - 1, unitStart - 1
                ) + 1;
                if( after >= 0 && after < line.end && isKeyword( tokens.get(after), "as" ) ) {
                    after = nextSignificant(tokens, after + 1, line.end);
                    if( after < 0 || tokens.get(after).type != TokenType.IDENTIFIER ) return null;
                    unitEnd = after + 1;
                    after   = nextSignificant(tokens, after + 1, line.end);
                }
                names.add(moduleName);
                unitTexts.add( verbatimLineText(tokens, unitStart, unitEnd) );
                lastUnitEnd = unitEnd;
                if( after >= 0 && after < line.end && tokens.get(after).type == TokenType.PUNCT
                        && ",".equals( tokens.get(after).text ) ) {
                    q = nextSignificantSkipBackslash(tokens, after + 1, line.end);
                    if( q < 0 || tokens.get(q).type != TokenType.IDENTIFIER ) return null;
                    continue;
                }
                if(after >= 0 && after < line.end) return null; // Trailing junk -- unrecognized, bail safely
                break;
            } // while
            if( names.isEmpty() ) return null;
            // Single-module `import a.b.c[ as alias]` keeps the pre-existing no-name-list shape --
            // nothing to reorder within one module. Multi-module `import a, b, c` gets the same
            // name-list rebuild machinery §3's `from` clause already uses (single physical line or
            // backslash-continued only, per this method's own call-site gating below).
            if( names.size() < 2 ) return new PyImport(
                PyImport.Kind.IMPORT, names.get(0), new ArrayList<>()
            );

            return new PyImport(
                PyImport.Kind.IMPORT, names.get(0), names, nameListStart, lastUnitEnd, unitTexts
            );
        } // if
        if( isKeyword(kw, "from") ) {
              int           p      = nextSignificant(tokens, kwIdx + 1, line.end);
        final StringBuilder module = new StringBuilder();
            while( p >= 0 && p < line.end && ( tokens.get(
                p
            ).type == TokenType.OP && ".".equals(
                tokens.get(p).text
            ) ) ) {
                module.append('.');
                p = nextSignificant(tokens, p + 1, line.end);
            }
            if( p >= 0 && p < line.end && tokens.get(p).type == TokenType.IDENTIFIER ) {
                final String rest = readDottedName(tokens, p, line.end);
                if(rest == null) return null;
                module.append(rest);
                p = advancePastDottedName(tokens, p, line.end);
            }
            if( module.length() == 0 ) return null;
            final int importKwIdx = nextSignificant(tokens, p, line.end);
            if( importKwIdx < 0 || !isKeyword( tokens.get(importKwIdx), "import" ) ) return null;
            int q = nextSignificant(tokens, importKwIdx + 1, line.end);
            if(q < 0) return null;
            if( tokens.get(q).type == TokenType.OP && "*".equals( tokens.get(q).text ) ) {
                final List<String> names = new ArrayList<>();
                names.add("*");
                return new PyImport( PyImport.Kind.FROM, module.toString(), names );
            }
            boolean parenthesized = false;
            int     parenOpenIdx  = -1;
            int     closeIdx      = -1;
            int     scanLimit     = line.end;
            if( tokens.get(q).type == TokenType.PUNCT && "(".equals( tokens.get(q).text ) ) {
                closeIdx = matchBracket(tokens, q, line.end);
                if(closeIdx < 0) return null; // Malformed brackets -- bail safely
                parenthesized = true;
                parenOpenIdx  = q;
                scanLimit     = closeIdx;
                q             = nextSignificant(tokens, q + 1, closeIdx);
                if(q < 0) return null; // Empty parens -- nothing to import, malformed
            } // if
            final List<String> names         = new ArrayList<>();
            final List<String> unitTexts     = new ArrayList<>();
            final int          nameListStart = q;
                  int          lastUnitEnd   = q;
            while( q >= 0 && q < scanLimit && tokens.get(q).type == TokenType.IDENTIFIER ) {
                final int unitStart = q;
                names.add( tokens.get(q).text );
                int unitEnd = q + 1;                                     // Index right after the name (or alias) itself, no trailing gap
                int after   = nextSignificant(tokens, q + 1, scanLimit);
                if( after >= 0 && after < scanLimit && isKeyword( tokens.get(after), "as" ) ) {
                    after = nextSignificant(tokens, after + 1, scanLimit);
                    if( after < 0 || tokens.get(after).type != TokenType.IDENTIFIER ) return null;
                    unitEnd = after + 1;
                    after   = nextSignificant(tokens, after + 1, scanLimit);
                }
                lastUnitEnd = unitEnd;
                unitTexts.add( verbatimLineText(tokens, unitStart, unitEnd) );
                if( after >= 0 && after < scanLimit && tokens.get(after).type == TokenType.PUNCT
                        && ",".equals( tokens.get(after).text ) ) {
                    q = nextSignificantSkipBackslash(tokens, after + 1, scanLimit);
                    continue;
                }
                q = after;
                break;
            } // while
            if( names.isEmpty() ) return null;
            if(parenthesized) {
                // Whatever's left before the close paren must be nothing, or a single trailing comma
                // -- any other shape here is unrecognized, bail safely rather than guess
                final int afterList = nextSignificant(tokens, lastUnitEnd, scanLimit);
                if(afterList >= 0 && afterList < scanLimit) {
                    final boolean soleTrailingComma = tokens.get(
                        afterList
                    ).type == TokenType.PUNCT && ",".equals(
                        tokens.get(afterList).text
                    ) && nextSignificant(
                        tokens, afterList + 1, scanLimit
                    ) < 0;
                    if(!soleTrailingComma) return null;
                } // if
            } // if
            final boolean isFuture = "__future__".equals( module.toString() );
            // Scan the *whole* parenthesized span, not just [nameListStart, closeIdx) -- a comment
            // can sit right after the opening `(` itself, before any name (real-world find:
            // django/db/models/__init__.py's `from django.db.models.fields.related import (  #
            // isort:skip`), and would otherwise go undetected by a narrower nameListStart-based scan.
            final boolean hasComment = parenthesized && containsComment(
                tokens, parenOpenIdx, closeIdx
            );
            return new PyImport(
                isFuture ? PyImport.Kind.FUTURE : PyImport.Kind.FROM, module.toString(), names,
                hasComment ? -1 : nameListStart, hasComment ? -1 : lastUnitEnd,
                hasComment ? null : unitTexts
            );
        } // if

        return null;
    }

    /**
     * Reads a dotted/comma-list-leading name run (`a.b.c`) starting at {@code from}, stopping at
     *  the first token that isn't an {@code IDENTIFIER}/{@code .} continuation -- used both for a
     *  plain `import a.b.c` module name and for a `from`-clause's non-relative module tail. Returns
     *  null if {@code from} isn't an {@code IDENTIFIER}.
     */
    private String readDottedName(final List<Token> tokens, final int from, final int to)
    {
        if( from < 0 || from >= to || tokens.get(from).type != TokenType.IDENTIFIER ) return null;
        final StringBuilder sb = new StringBuilder();
              int           i  = from;
        sb.append( tokens.get(i).text );
        i = nextSignificant(tokens, i + 1, to);
        while( i >= 0 && i < to && tokens.get(
            i
        ).type == TokenType.OP && ".".equals(
            tokens.get(i).text
        ) ) {
            final int nameIdx = nextSignificant(tokens, i + 1, to);
            if( nameIdx < 0 || nameIdx >= to || tokens.get(
                nameIdx
            ).type != TokenType.IDENTIFIER ) break;
            sb.append('.').append( tokens.get(nameIdx).text );
            i = nextSignificant(tokens, nameIdx + 1, to);
        } // while

        return sb.toString();
    }

    private int advancePastDottedName(final List<Token> tokens, final int from, final int to)
    {
        int i    = from;
        int next = nextSignificant(tokens, i + 1, to);
        while( next >= 0 && next < to && tokens.get(
            next
        ).type == TokenType.OP && ".".equals(
            tokens.get(next).text
        ) ) {
            final int nameIdx = nextSignificant(tokens, next + 1, to);
            if( nameIdx < 0 || nameIdx >= to || tokens.get(
                nameIdx
            ).type != TokenType.IDENTIFIER ) return next;
            i    = nameIdx;
            next = nextSignificant(tokens, nameIdx + 1, to);
        } // while

        return next;
    }

    // ── §4: Decorators ───────────────────────────────────────────────────────────────

    /**
     * STYLE_PYTHON3.md §4: a decorator line's own `@` binds tight to whatever follows it (any
     *  whitespace between them is removed), and every `(`/`[`/`{` pair anywhere in the decorator's
     *  own expression (the call's argument list, plus any bracket nested inside it, e.g. a
     *  `methods=[...]` kwarg) gets its immediate delimiter gap normalized per §1's tight/loose
     *  test ({@link PythonBracketComplexityEvaluator#isLooseParen}/{@code isLooseBracket}/
     *  {@code isLooseBrace}) -- one space just inside the opener/closer when loose, none when
     *  tight, same delimiter-only-padding convention {@code MiscRuleCore#enforceComplexityPadding}
     *  uses for the C-family (comma/operator spacing inside the content is deliberately left
     *  untouched, not this pass's concern, same division of responsibility as that method).
     *  Multi-physical-line decorators (a wrapped call spanning a bracket continuation) are left
     *  completely untouched by the bracket-padding step above -- same "documented gap, not a
     *  guess" precedent as §2/§3. A single-physical-line decorator whose call still overflows
     *  `line-length` after padding is handed to {@link #tryWrapDecoratorCall}, which (per
     *  STYLE_PYTHON3.md §4's "Overflow" worked example) breaks the outermost call's own
     *  top-level argument list one-per-line with a trailing comma -- the general line-length-
     *  based call-argument-wrapping mechanism the C-family's `enforceCallLineBreaking` has but
     *  this codebase never ported (STATE_PYTHON3.md §4's documented gap, now closed for the
     *  decorator-call case specifically; §6's own signature-wrap decision remains its own,
     *  separate open gap).
     */
    private List<Replacement> applyDecoratorSpacing(
        final List<Token>   tokens,
        final List<RawLine> rawLines
    )
    {
        final List<Replacement> replacements = new ArrayList<>();
        for(final RawLine line : rawLines) {
            if(line.multiPhysicalLine) continue;
            final int atIdx = nextSignificant(tokens, line.contentStart, line.end);
            if( atIdx < 0 || tokens.get(
                atIdx
            ).type != TokenType.OP || !"@".equals(
                tokens.get(atIdx).text
            ) ) continue;
            final int nameIdx = nextSignificant(tokens, atIdx + 1, line.end);
            if(nameIdx < 0) continue;
            final Replacement tight = normalizeGap(tokens, atIdx + 1, nameIdx, "");
            if(tight != null) replacements.add(tight);

            final List<Replacement> bracketReplacements = new ArrayList<>();
            applyBracketPadding(tokens, nameIdx, line.end, bracketReplacements);

            final List<Replacement> combined = new ArrayList<>(bracketReplacements);
            if(tight != null) combined.add(tight);
            final Replacement wrap = tryWrapDecoratorCall(tokens, line, nameIdx, combined);
            if(wrap != null) {
                replacements.add(wrap);
                for(final Replacement r : bracketReplacements) {
                    if(r.start < wrap.start || r.start >= wrap.end) replacements.add(r);
                }
            }
            else {
                replacements.addAll(bracketReplacements);
            }
        } // for

        return replacements;
    }

    /**
     * STYLE_PYTHON3.md §4's "Overflow" case: the decorator's own outermost call `(...)`
     *  exceeding `line-length` wraps its top-level argument list one-per-line (each argument
     *  indented one level past the `@` line, a trailing comma after the last argument, the
     *  closing `)` back at the `@` line's own indent) -- the same shape the worked example in
     *  §4 shows. Returns {@code null} (leave the line untouched) whenever: the decorator has no
     *  trailing call at all (`@dataclass`, `@x.setter`); the call isn't the very last thing on
     *  the line (a trailing comment, or something chained after the closing `)`, both left as a
     *  documented gap rather than guessed at); the call is empty (zero-arg, never wrapped, same
     *  precedent as {@code MiscRuleCurly#enforceCallLineBreaking}); a comment sits anywhere
     *  inside the call's parens (comment-disqualifies-the-candidate, same posture as the
     *  C-family); or the padded line already fits within {@code lineLength} as-is.
     */
    private Replacement tryWrapDecoratorCall(
        final List<Token>       tokens,
        final RawLine           line,
        final int               nameIdx,
        final List<Replacement> paddingReplacements
    )
    {
        final int lastSigOnLine = prevSignificant(tokens, line.end - 1, nameIdx);
        if( lastSigOnLine < nameIdx || tokens.get(
            lastSigOnLine
        ).type != TokenType.PUNCT || !")".equals(
            tokens.get(lastSigOnLine).text
        ) ) return null; // No trailing call, e.g. `@dataclass`/`@x.setter`

        int openIdx = -1;
        int depth   = 0;
        for( int j = lastSigOnLine; j >= nameIdx; --j ) {
            final Token t = tokens.get(j);
            if(t.type != TokenType.PUNCT) continue;
            if( isCloseBracketText(t.text) ) {
                ++depth;
            }
            else if( isOpenBracketText(t.text) ) {
                --depth;
                if(depth == 0) {
                    openIdx = j;
                    break;
                }
            }
        } // for
        if( openIdx < 0 || !"(".equals( tokens.get(openIdx).text ) ) return null;

        if( containsComment(tokens, openIdx, lastSigOnLine + 1) ) return null; // Comment inside the call
        if( containsComment(tokens, lastSigOnLine + 1, line.end) ) return null; // Trailing comment after `)`

        final int contentFirst = nextSignificant(tokens, openIdx + 1, lastSigOnLine);
        if(contentFirst < 0) return null; // Zero-arg call, never wrapped

        final String paddedLine = renderSpan(
            tokens, line.contentStart, lastSigOnLine + 1, paddingReplacements
        );
        if( physicalLineLength(paddedLine) <= lineLength ) return null; // Already fits

        final List<int[]> args = splitTopLevelArgs(tokens, openIdx + 1, lastSigOnLine);
        if(args.isEmpty()) return null;

        // Re-derive bracket padding scoped to strictly *inside* the call's own parens (`(openIdx +
        // 1, lastSigOnLine)`) rather than reusing `paddingReplacements` as-is -- that list also
        // carries the outer call pair's own open/close delimiter-gap entries (computed over the
        // whole decorator including its outermost `(`/`)`), whose `start` can coincide with the
        // first argument's own first significant token (e.g. a loose call's open-gap `start ==
        // openIdx + 1 == args.get(0)[0]`) and would otherwise get double-applied as a spurious
        // leading space on the first rendered argument. Recomputing over the narrower interior
        // range naturally excludes both outer-pair entries (their own `(`/`)` tokens sit outside
        // this range) while still finding every genuinely-nested bracket pair inside the arguments.
        final List<Replacement> interior = new ArrayList<>();
        applyBracketPadding(tokens, openIdx + 1, lastSigOnLine, interior);

        final String        baseIndent = leadingIndent(tokens, line);
        final String        argIndent  = baseIndent + indentUnit();
        final StringBuilder sb         = new StringBuilder();
        sb.append("(\n");
        for(final int[] arg : args) {
            sb.append(argIndent).append( renderSpan(tokens, arg[0], arg[1], interior) )
                    .append(",\n");
        }
        sb.append(baseIndent).append(")");

        return new Replacement(openIdx, lastSigOnLine + 1, sb.toString());
    }

    /**
     * Splits {@code [start, end)} on top-level (bracket-depth-0) commas, returning each segment
     *  trimmed to its own significant range ({@code [firstSig, lastSig + 1)}). An empty trailing
     *  segment (a source trailing comma immediately before {@code end}) is dropped rather than
     *  producing a phantom empty argument -- {@link #tryWrapDecoratorCall} always synthesizes its
     *  own trailing comma on every argument it emits, so an already-present one is redundant, not
     *  additive.
     */
    private List<int[]> splitTopLevelArgs(final List<Token> tokens, final int start, final int end)
    {
        final List<int[]> args     = new ArrayList<>();
              int          depth   = 0;
              int          segStart = start;
        for( int i = start; i < end; ++i ) {
            final Token t = tokens.get(i);
            if(t.type != TokenType.PUNCT) continue;
            if( isOpenBracketText(t.text) ) {
                ++depth;
            }
            else if( isCloseBracketText(t.text) ) {
                --depth;
            }
            else if( depth == 0 && ",".equals(t.text) ) {
                addTrimmedArg(tokens, segStart, i, args);
                segStart = i + 1;
            } // if
        } // for
        addTrimmedArg(tokens, segStart, end, args);

        return args;
    }

    private void addTrimmedArg(
        final List<Token> tokens,
        final int         from,
        final int         to,
        final List<int[]> out
    )
    {
        final int first = nextSignificant(tokens, from, to);
        if(first < 0) return; // Empty segment (trailing comma) -- not a real argument
        final int last = prevSignificant(tokens, to - 1, first - 1);
        out.add( new int[] { first, last + 1 } );
    }

    /**
     * Reassembles {@code [start, end)}'s source text verbatim, applying any {@code
     *  replacements} entry whose {@code start} falls in range, same left-to-right cursor walk
     *  {@link #render} uses for the whole token stream -- a scoped, read-only sibling used to
     *  measure/re-render a single sub-span (a decorator call's own text, or one of its
     *  arguments) without disturbing the file-wide {@code replacements} list a caller is still
     *  assembling.
     */
    private String renderSpan(
        final List<Token>       tokens,
        final int               start,
        final int               end,
        final List<Replacement> replacements
    )
    {
        final List<Replacement> sorted = new ArrayList<>();
        for(final Replacement r : replacements) {
            if(r.start >= start && r.start < end) sorted.add(r);
        }
        sorted.sort( Comparator.comparingInt(r -> r.start) );

        final StringBuilder out = new StringBuilder();
              int            i   = start;
              int            r   = 0;
        while(i < end) {
            while( r < sorted.size() && sorted.get(r).start < i ) ++r;
            if( r < sorted.size() && sorted.get(r).start == i ) {
                out.append( sorted.get(r).text );
                i = sorted.get(r).end;
                ++r;
                continue;
            }
            final Token t = tokens.get(i);
            if(t.type != TokenType.INDENT && t.type != TokenType.DEDENT) out.append(t.text);
            ++i;
        } // while

        return out.toString();
    }

    /**
     * Recursively normalizes the immediate delimiter gap of every `(`/`[`/`{` pair found in
     *  {@code [from, to)} (a decorator's own expression range) -- applies at every nesting level,
     *  not just the outermost call, mirroring {@code enforceComplexityPadding}'s uniform-depth
     *  posture
     */
    private void applyBracketPadding(
        final List<Token>       tokens,
        final int               from,
        final int               to,
        final List<Replacement> out
    )
    {
        int i                     = from;
        int lastFStringFieldClose = -1;
        while(i < to) {
            final Token t = tokens.get(i);
            if( t.type == TokenType.PUNCT && isOpenBracketText(t.text) ) {
                final int close = matchBracket(tokens, i, to);
                if(close < 0) {
                    ++i;
                    continue;
                }
                // An f-string field's own `{`/`}` (emitted as plain PUNCT by TokenizerIndent#
                // emitFStringField, indistinguishable by type/text alone from a dict/set literal
                // brace) is owned by applyFStringSpacing, not this pass -- the field-open `{` is
                // always immediately preceded by FSTRING_START/FSTRING_MIDDLE with no gap token in
                // between (real code find: `@click.custom_version_option(lambda ctx:
                // f"{ctx.info_name} 1.0")` in pallets/click's test_basic.py -- padding it here as an
                // ordinary non-empty-brace-is-loose dict/set literal produced `{ ctx.info_name }`,
                // which applyFStringSpacing's own unconditional open-gap trim then silently undid on
                // a second formatting pass, a non-idempotency bug; skip the whole field's content
                // entirely here rather than just the outer pair, since nothing downstream in this
                // recursive descent should touch it either). Two adjacent fields with no literal
                // text between them (e.g. `f'{a}{b}'`, real find: `@register(f'Struct331_
                // {signedness}{n}', set_name=True)` in cpython's test_generated_structs.py) emit no
                // FSTRING_MIDDLE between the first field's closing `}` and the second field's
                // opening `{`, so the adjacency check alone misses the second field -- also treat a
                // `{` immediately following a just-closed f-string field's `}` as another field open.
                if( "{".equals(t.text) && i > from ) {
                    final TokenType prevType = tokens.get(i - 1).type;
                    if(prevType == TokenType.FSTRING_START || prevType == TokenType.FSTRING_MIDDLE
                            || i - 1 == lastFStringFieldClose) {
                        lastFStringFieldClose = close;
                        i                     = close + 1;
                        continue;
                    }
                } // if
                final int contentFirst = nextSignificant(tokens, i + 1, close);
                if(contentFirst >= 0) {
                    final boolean     loose   = classifyLoose(t.text, tokens, i + 1, close);
                    final String      desired = loose ? " " : "";
                    final Replacement openGap = normalizeGap(tokens, i + 1, contentFirst, desired);
                    if(openGap != null) out.add(openGap);
                    final int         lastSig  = prevSignificant(tokens, close - 1, i);
                    final Replacement closeGap = normalizeGap(tokens, lastSig + 1, close, desired);
                    if(closeGap != null) out.add(closeGap);
                } // if
                applyBracketPadding(tokens, i + 1, close, out);
                i = close + 1;
            } // if
            else {
                ++i;
            }
        } // while
    }

    private boolean classifyLoose(
        final String      openText,
        final List<Token> tokens,
        final int         contentStart,
        final int         contentEnd
    )
    {
        final List<Token> content = tokens.subList(contentStart, contentEnd);
        if( "(".equals(openText) ) return bracketEval.isLooseParen(content);
        if( "[".equals(openText) ) return bracketEval.isLooseBracket(content);

        return bracketEval.isLooseBrace(content);
    }

    private boolean isOpenBracketText(final String text)
    {
        return "(".equals(text) || "[".equals(text) || "{".equals(text);
    }

    private boolean isCloseBracketText(final String text)
    {
        return ")".equals(text) || "]".equals(text) || "}".equals(text);
    }

    /**
     * Finds {@code openIdx}'s matching close bracket within {@code [openIdx, limit)}, tracking
     *  depth across all three bracket kinds jointly (same joint-nesting convention {@link
     *  TokenizerIndent}'s own {@code parenDepth} uses) so a mismatched-kind close never
     *  short-circuits the match. Returns -1 if unmatched within the range (should not happen for
     *  syntactically valid, single-physical-line input).
     */
    private int matchBracket(final List<Token> tokens, final int openIdx, final int limit)
    {
        final String open  = tokens.get(openIdx).text;
        final String close = "(".equals(open) ? ")" : "[".equals(open) ? "]" : "}";
              int    depth = 0;
        for(int j = openIdx; j < limit; ++j) {
            final Token t = tokens.get(j);
            if(t.type != TokenType.PUNCT) continue;
            if( isOpenBracketText(t.text) ) {
                ++depth;
            }
            else if( ")".equals(t.text) || "]".equals(t.text) || "}".equals(t.text) ) {
                --depth;
                if(depth == 0) return close.equals(t.text) ? j : -1;
            }
        } // for

        return -1;
    }

    /**
     * Scans backward from {@code from} for the nearest non-gap token, stopping strictly before
     *  {@code lowExclusive}. Returns {@code lowExclusive} itself if every token in between is a
     *  gap token (i.e. no significant token found) -- callers only invoke this when {@code
     *  nextSignificant} already confirmed at least one significant token exists in the range, so
     *  that case is unreachable in practice, but the bound keeps this helper safe standalone.
     */
    private int prevSignificant(final List<Token> tokens, final int from, final int lowExclusive)
    {
        int i = from;
        while( i > lowExclusive && isGapToken( tokens.get(i) ) ) i--;

        return i;
    }

    /**
     * Returns a {@link Replacement} collapsing the gap {@code [from, to)} to exactly {@code
     *  desired} text ({@code ""} for tight, {@code " "} for loose), or {@code null} if the gap
     *  already renders as {@code desired} or the gap contains a comment/newline (conservative skip
     *  -- same posture as {@code enforceComplexityPadding}'s own comment/NEWLINE exclusion, though
     *  a comment inside a single-physical-line decorator's own delimiter gap should not occur in
     *  practice). {@code from == to} (no existing gap token at all, e.g. a tight `("x")`) is a
     *  valid zero-width insertion point, not a no-op -- unlike {@code from > to}, which cannot
     *  happen for a well-formed range and is guarded against defensively.
     */
    private Replacement normalizeGap(
        final List<Token> tokens,
        final int         from,
        final int         to,
        final String      desired
    )
    {
        if(from > to) return null;
        for(int i = from; i < to; ++i) {
            final TokenType type = tokens.get(i).type;
            if(type == TokenType.COMMENT_LINE || type == TokenType.NEWLINE) return null;
        }
        final String current = from < to ? verbatimLineText(tokens, from, to) : "";
        if( current.equals(desired) ) return null;

        return new Replacement(from, to, desired);
    }

    // ── §5: F-Strings ────────────────────────────────────────────────────────────────

    /**
     * STYLE_PYTHON3.md §5: an f-string field's `{...}` braces are tight against the expression
     *  they hold (`f"{ x + 1 }"` -> `f"{x + 1}"`) -- whitespace directly inside the braces is
     *  never part of the printed output, so it is trimmed to nothing, same delimiter-only-padding
     *  posture {@link #applyBracketPadding} uses for a decorator's own call parens (except here the
     *  desired gap is unconditionally zero-width, never a padded space -- STYLE_PYTHON3.md §5 shows
     *  no loose-brace-padding shape for a field, only tight). Everything from the field's own
     *  `!conversion`/`:format_spec` onward (opaque per §5, see {@link
     *  com.jxmake.formatter.tokenizer.TokenizerIndent#emitFStringField}) is left completely
     *  untouched, including its own internal whitespace.
     *
     *  <p>Operates directly over the full token stream (not per-{@link RawLine}, unlike §2/§3/§4)
     *  since a field's own brace/expression tokens never carry a `NEWLINE`, and a triple-quoted
     *  f-string's surrounding literal text spanning multiple physical lines is otherwise irrelevant
     *  here -- only the field's own immediate brace-adjacent gap is ever touched.
     *
     *  <p><b>Explicitly NOT covered by this slice</b> (a deliberate scope-boundary call, not a
     *  guess): re-spacing the expression's own *internal* operator/operand spacing (e.g. collapsing
     *  `f"{x  +  1}"` to `f"{x + 1}"`) is out of scope. Surveyed {@code MiscRuleCore}/{@code
     *  MiscRuleIndent}/{@code PythonBracketComplexityEvaluator} first: the only inherited
     *  token-joining primitive ({@code MiscRuleCore#renderTokens}/{@code needsSpaceBetween}/{@code
     *  isTightToken}) is a C-family declarator-spacing helper, not a general expression-spacing one
     *  -- it hardcodes `*`/`&` as tight pointer/reference sigils (would wrongly collapse Python
     *  multiplication, e.g. `a * b` -> `a* b`), `.`/`->`/`::` as tight member-access/scope operators
     *  (Python's `.` attribute access is genuinely tight, but the others don't exist in Python), and
     *  has no notion of Python-only operators (`**`, `//`, `:=`, `and`/`or`/`not`, comprehension
     *  `for`/`if`). Building a genuine general Python expression-spacing primitive from scratch as a
     *  side effect of this checkpoint would be a large scope increase beyond §5's narrow "braces are
     *  tight" ask -- deferred to whatever future general-expression-formatting work eventually lands
     *  (same posture as §2's "multi-line RHS not yet covered"/§3's "multi-physical-line import not
     *  yet covered" gaps). Every worked example in STYLE_PYTHON3.md §5 itself already has correctly
     *  spaced internal expression text (`x + 1`, `price * quantity`), so this narrower scope is
     *  sufficient to satisfy the style doc's own examples.
     *
     *  <p><b>Also NOT covered</b> (a discovered, not guessed, interaction): when an f-string
     *  containing a field appears inside a span already fully rewritten by another pass in this
     *  same {@code process()} call -- e.g. as a §2-recognized assignment's own RHS (`x = f"{ y }"`)
     *  -- that other pass's own {@link Replacement} (covering the whole assignment's `target...
     *  value` span, rendered via verbatim token join) sorts first (smaller {@code start}) and wins;
     *  {@link #render}'s "first match at this position wins, subsequent nested-inside replacements
     *  are silently never reached" behavior means this pass's own narrower, nested replacement for
     *  that specific occurrence is dropped (not corrupted -- the original untrimmed text is kept
     *  for that one occurrence, verified via the smoke test's dedicated case below). An f-string NOT
     *  nested inside another pass's own replaced span (a bare expression statement, a function-call
     *  argument, an un-recognized-shape line) is unaffected by this interaction and trims normally.
     */
    private List<Replacement> applyFStringSpacing(final List<Token> tokens)
    {
        final List<Replacement> replacements = new ArrayList<>();
              int               i            = 0;
        final int               n            = tokens.size();
        while(i < n) {
            if( tokens.get(
                i
            ).type == TokenType.FSTRING_START ) i = processFString(
                tokens, i, replacements
            );
            else i++;
        } // while

        return replacements;
    }

    /**
     * Processes one f-string span starting at {@code startIdx} (its {@code FSTRING_START}
     *  token), dispatching each `{...}` field found at this level to {@link #processField}.
     *  Returns the index right after this f-string's {@code FSTRING_END}.
     */
    private int processFString(
        final List<Token>       tokens,
        final int               startIdx,
        final List<Replacement> out
    )
    {
          int i = startIdx + 1;
    final int n = tokens.size();
        while(i < n) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.FSTRING_END) return i + 1;
            if( t.type == TokenType.PUNCT && "{".equals(t.text) ) i = processField(tokens, i, out);
            else                                                  i++;
        }

        return i;
    }

    /**
     * Processes one `{...}` field, {@code openIdx} pointing at its opening `{`. Tracks a local
     *  `(`/`[`/`{` depth (mirroring {@link
     *  com.jxmake.formatter.tokenizer.TokenizerIndent#emitFStringField}'s own depth counter) so a
     *  nested bracket's own `}` isn't mistaken for this field's closing brace; a nested f-string
     *  found inside the expression (e.g. `f"{f'{a}'}"`) is skipped over atomically via a recursive
     *  {@link #processFString} call -- its own fields get their own independent trim through that
     *  call, and its internal brackets never affect this field's own depth count. {@code exprEnd}
     *  is the index of the first depth-0 `!conversion` OP token, {@code FSTRING_FORMAT_SPEC} token,
     *  or (if neither is present) this field's own closing `}` -- whichever comes first. Returns
     *  the index right after the field's closing `}`.
     */
    private int processField(
        final List<Token>       tokens,
        final int               openIdx,
        final List<Replacement> out
    )
    {
        int i       = openIdx + 1;
        int depth   = 0;
        int exprEnd = -1;
        while(true) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.FSTRING_START) {
                i = processFString(tokens, i, out);
                continue;
            }
            if( depth == 0 && t.type == TokenType.PUNCT && "}".equals(t.text) ) {
                final boolean directClose = exprEnd < 0;
                if(directClose) exprEnd = i;
                addBraceTrim(tokens, openIdx, exprEnd, directClose, out);
                return i + 1;
            }
            if( depth == 0 && exprEnd < 0 && t.type == TokenType.OP && isFStringConversion(
                t.text
            ) ) exprEnd = i;
            else if(depth == 0 && exprEnd < 0 && t.type == TokenType.FSTRING_FORMAT_SPEC) exprEnd = i;
            else if( t.type == TokenType.PUNCT && isOpenBracketText(t.text) ) depth++;
            else if( t.type == TokenType.PUNCT && isCloseBracketText(t.text) ) depth--;
            ++i;
        } // while
    }

    /**
     * A `!r`/`!s`/`!a` conversion OP token, per {@link
     *  com.jxmake.formatter.tokenizer.TokenizerIndent#emitFStringField}'s own emission rule (only
     *  ever a 2-character `!`-prefixed OP token).
     */
    private boolean isFStringConversion(final String text)
    {
        return text.length() == 2 && text.charAt(0) == '!';
    }

    /**
     * Trims the gap directly inside a field's opening `{` to zero-width unconditionally (the
     *  expression's own leading whitespace is never significant, STYLE_PYTHON3.md §5). The gap
     *  right before {@code exprEnd} is only trimmed when {@code trimClose} is true -- i.e. when
     *  {@code exprEnd} is itself the field's own closing `}` (no `!conversion`/`:format_spec`
     *  present). When a conversion or format spec IS present, STYLE_PYTHON3.md §5's own worked
     *  example (`f"{value !r}"`, listed as "never touched") keeps any whitespace immediately
     *  before the opaque tail exactly as written -- only the opaque tail's own text ({@code !r}/
     *  {@code :spec}), not the boundary gap leading into it, is what "never touched" refers to in
     *  the brace-tightness sense used here, so this pass does not touch that gap at all. A field
     *  with no significant expression token at all (e.g. a malformed empty `{}`) is left alone --
     *  defensive only, not valid Python.
     */
    private void addBraceTrim(
        final List<Token>       tokens,
        final int               openIdx,
        final int               exprEnd,
        final boolean           trimClose,
        final List<Replacement> out
    )
    {
        final int firstSig = nextSignificant(tokens, openIdx + 1, exprEnd);
        if(firstSig < 0) return;
        final int lastSig = prevSignificant(tokens, exprEnd - 1, openIdx);
        // Self-documenting `{expr=}` debug field: a bare trailing `=` (the tokenizer only ever
        // emits a lone 1-char `=` OP token for this -- `==`/`!=`/`<=`/`>=`/`:=`/`+=` etc. are all
        // their own distinct multi-char OP tokens per TokenizerIndent#MULTI_CHAR_OPS, so text-
        // equality with "=" alone can't be confused with a comparison/augmented-assignment/walrus
        // operator) as the LAST significant token of the expression means Python's runtime must
        // reproduce `expr`'s exact original source text -- including its own surrounding
        // whitespace -- verbatim in the debug output. Skip this pass's own gap-trimming entirely
        // for such a field rather than only skipping part of it.
        final boolean debugSpecifier = lastSig >= 0 && tokens.get(
            lastSig
        ).type == TokenType.OP && "=".equals(
            tokens.get(lastSig).text
        );
        if(!debugSpecifier) {
            // Trimming the gap after `{` to zero-width is unsafe when the very next significant
            // character is itself a literal `{` (e.g. a nested dict/set-comprehension field,
            // `f"{ {a for a in (1, 2, 3)}}"`) -- an outer field-open `{` immediately followed by a
            // literal `{` with nothing between them renders as `{{`, which Python's f-string
            // grammar parses as an ESCAPED literal `{` rather than two brace-opens, silently
            // deleting the nested expression from the program's semantics. Normalize to exactly one
            // space instead of zero in that shape so the two braces can never fuse.
            final boolean     nextIsOpenBrace = tokens.get(
                firstSig
            ).type == TokenType.PUNCT && "{".equals(
                tokens.get(firstSig).text
            );
            final Replacement openGap         = normalizeGap(
                tokens, openIdx + 1, firstSig, nextIsOpenBrace ? " " : ""
            );
            if(openGap != null) out.add(openGap);
        } // if
        if(!trimClose || debugSpecifier) return;
        final Replacement closeGap = normalizeGap(tokens, lastSig + 1, exprEnd, "");
        if(closeGap != null) out.add(closeGap);
    }

    // ── §6: Function Signature Wrapping (alignment-only slice) ─────────────────────────

    /**
     * STYLE_PYTHON3.md §6's inline-vs-one-per-line *decision* has no home in this codebase yet --
     *  same documented gap as §4's own call-argument-overflow wrapping (no general line-length-
     *  triggered breaking mechanism exists anywhere in the {@code *Indent}/{@code *Curly} family;
     *  the C-family's {@code enforceCallLineBreaking} is Curly-only). This pass therefore never
     *  decides to break or join a signature -- it only column-aligns the {@code :}/{@code =} of a
     *  {@code def} signature's parameter list that is <em>already</em> written one-parameter-per-
     *  line in the source, taking that human-authored line-breaking as given (the same posture §2's
     *  assignment alignment takes toward an already-single-line assignment candidate: normalize
     *  spacing given the existing structure, never decide when to (re)break a line). An inline
     *  (already-one-line) signature is untouched by construction -- it is never
     *  {@code multiPhysicalLine}, so it never reaches {@link #trySignatureGroup} at all.
     */
    private List<Replacement> applySignatureAlignment(
        final List<Token>   tokens,
        final List<RawLine> rawLines
    )
    {
        final List<Replacement> replacements = new ArrayList<>();
        for(final RawLine line : rawLines) {
            if(!line.multiPhysicalLine) continue;
            int kwIdx = nextSignificant(tokens, line.contentStart, line.end);
            if( kwIdx < 0 || tokens.get(kwIdx).type != TokenType.KEYWORD ) continue;
            if( isKeyword( tokens.get(kwIdx), "async" ) ) {
                kwIdx = nextSignificant(tokens, kwIdx + 1, line.end);
                if( kwIdx < 0 || tokens.get(kwIdx).type != TokenType.KEYWORD ) continue;
            }
            if( !isKeyword( tokens.get(kwIdx), "def" ) ) continue;
            final int nameIdx = nextSignificant(tokens, kwIdx + 1, line.end);
            if( nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER ) continue;
            final int openIdx = nextSignificant(tokens, nameIdx + 1, line.end);
            if( openIdx < 0 || tokens.get(
                openIdx
            ).type != TokenType.PUNCT || !"(".equals(
                tokens.get(openIdx).text
            ) ) continue;
            final int closeIdx = matchBracket(tokens, openIdx, line.end);
            if(closeIdx < 0) continue;
            final List<Replacement> group = trySignatureGroup(tokens, openIdx, closeIdx);
            if(group != null) replacements.addAll(group);
        } // for

        return replacements;
    }

    /**
     * Attempts to classify {@code (openIdx, closeIdx)}'s interior as an already one-parameter-per-
     *  line signature: the opening `(` has nothing but a {@code NEWLINE} after it on its own line,
     *  the closing `)` has nothing but its own leading indentation before it on its own line, and
     *  every line in between is exactly one parameter (optionally comment-free, single top-level
     *  {@code :}/{@code =}, optional trailing comma). Returns {@code null} (leave completely
     *  untouched) the moment any of that isn't true -- an inline first parameter
     *  (`def f(x,\n    y,\n)`), a multi-parameter line, a per-parameter trailing comment, or a
     *  parameter itself spanning more than one physical line (a multi-line default value/nested
     *  bracket continuation) are all treated as this slice's own documented gaps, same "STOP and
     *  leave untouched rather than guess" precedent §2-§5 already established -- not a hard
     *  STATE_COMMON.md ambiguity requiring a user answer, since STYLE_PYTHON3.md §6 itself only ever
     *  describes the already-one-per-line shape being aligned here.
     */
    private List<Replacement> trySignatureGroup(
        final List<Token> tokens,
        final int         openIdx,
        final int         closeIdx
    )
    {
        // NEWLINE only ends a segment at local bracket-depth 0 -- a parameter whose own type hint or
        // default itself contains a nested bracket spanning multiple physical lines (e.g.
        // `attempts: list[\n    tuple[...]\n]`) must never be split into multiple bogus segments at
        // its own interior NEWLINEs; each such NEWLINE is still inside that one parameter's own
        // unclosed bracket, so it's skipped here and the whole multi-line param collapses back into
        // one segment, which classifySignatureParam then rejects for containing embedded NEWLINEs
        // (via its own single-line assumption), correctly leaving the whole signature untouched per
        // this method's documented "parameter spanning more than one physical line" gap -- instead of
        // silently misclassifying each of that param's own continuation lines as separate params.
        final List<int[]> segments   = new ArrayList<>(); // [segStart, segEnd) per top-level-NEWLINE-delimited segment
              int         segStart   = openIdx + 1;
              int         localDepth = 0;
        for(int i = openIdx + 1; i < closeIdx; ++i) {
            final Token t = tokens.get(i);
            if( t.type == TokenType.PUNCT && isOpenBracketText(t.text) ) {
                ++localDepth;
            }
            else if( t.type == TokenType.PUNCT && isCloseBracketText(t.text) ) {
                --localDepth;
            }
            else if(t.type == TokenType.NEWLINE && localDepth == 0) {
                segments.add( new int[] { segStart, i } );
                segStart = i + 1;
            }
        } // for
        segments.add( new int[] { segStart, closeIdx } );
        if( segments.size() < 3 ) return null; // Need >=1 param line plus the blank pre-`(`/pre-`)` framing lines
        final int firstSig = nextSignificant( tokens, segments.get(0)[0], segments.get(0)[1] );
        if(firstSig >= 0) return null; // An inline first parameter shares `(`'s own line -- unsupported shape
        final int[] lastSeg    = segments.get( segments.size() - 1 );
        final int   lastSegSig = nextSignificant( tokens, lastSeg[0], lastSeg[1] );
        if(lastSegSig >= 0) return null; // `)` doesn't stand alone on its own line -- unsupported shape
        final List<PyParam> params = new ArrayList<>();
        final List<int[]>   spans  = new ArrayList<>(); // [contentFirst, contentEndExclusive] per param
        for( int s = 1; s < segments.size() - 1; ++s ) {
            final int[]   seg = segments.get(s);
            final PyParam p   = classifySignatureParam( tokens, seg[0], seg[1] );
            if(p == null) return null;
            params.add(p);
            final int contentFirst = nextSignificant( tokens, seg[0], seg[1] );
            final int contentEnd   = trimEndIdx( tokens, contentFirst, seg[1] );
            spans.add( new int[] { contentFirst, contentEnd } );
        } // for
        final List<String>      rendered = miscRule.renderPySignatureGroup(params);
        final List<Replacement> out      = new ArrayList<>();
        for( int i = 0; i < params.size(); ++i ) out.add(
            new Replacement( spans.get(i)[0], spans.get(i)[1], rendered.get(i) )
        );

        return out;
    }

    /**
     * Classifies one already-isolated parameter line ({@code [segStart, segEnd)}, a single
     *  {@code NEWLINE}-delimited segment strictly inside a signature's `(`/`)`) into a {@link
     *  PyParam}: {@code name[: type][= default][,]}. The top-level {@code :}/{@code =} search
     *  tracks this segment's own local bracket depth (starting fresh at 0, since the segment is
     *  known to hold exactly one parameter) so a nested-bracket type hint like
     *  {@code List[Dict[str, int]]} never has its own internal {@code :}/{@code =} mistaken for the
     *  parameter's own annotation/default separator -- the same depth-tracking shape {@link
     *  #classifyLoose}/{@link #matchBracket} already use elsewhere in this class, just applied to
     *  {@code :}/{@code =} search instead of bracket matching. Returns {@code null} (segment
     *  rejected, whole signature left untouched by the caller) if the segment contains a comment or
     *  has no name token at all.
     */
    private PyParam classifySignatureParam(
        final List<Token> tokens,
        final int         segStart,
        final int         segEnd
    )
    {
        for(int k = segStart; k < segEnd; ++k) {
            if( tokens.get(k).type == TokenType.COMMENT_LINE ) return null;
            // A segment containing its own embedded NEWLINE means the caller's bracket-depth-aware
            // segmentation had to fold multiple physical lines into one segment because they all
            // belonged to one still-open bracket -- i.e. this parameter's own type hint or default
            // spans more than one physical line. Reject it (whole signature left untouched), per this
            // class's documented gap, rather than misclassifying multi-line content as one flattened
            // param.
            if( tokens.get(k).type == TokenType.NEWLINE ) return null;
        } // for
        final int nameStart = nextSignificant(tokens, segStart, segEnd);
        if(nameStart < 0) return null;
        // A genuine parameter always starts with its own name (an identifier), or one of the
        // positional/keyword-only-marker/star-unpacking tokens (`*`, `**`, `/`). Anything else --
        // e.g. `|` -- means this "segment" isn't actually its own parameter at all: it's really the
        // continuation of the PREVIOUS segment's own type hint, wrapped onto its own physical line
        // with no enclosing bracket to fold it back into one segment (unlike the nested-bracket case
        // already handled above via the embedded-NEWLINE check). Reject it so the whole signature is
        // left untouched, per this class's own documented multi-physical-line-type-hint gap, instead
        // of misclassifying the continuation as a bogus parameter of its own.
        final Token   startTok        = tokens.get(nameStart);
        final boolean validParamStart = startTok.type == TokenType.IDENTIFIER || ( startTok.type == TokenType.OP && ( "*".equals(
            startTok.text
        ) || "**".equals(
            startTok.text
        ) || "/".equals(
            startTok.text
        ) ) );
        if(!validParamStart) return null;
        int lastSig = -1;
        for(int k = segEnd - 1; k >= segStart; --k) {
            if( !isGapToken( tokens.get(k) ) ) {
                lastSig = k;
                break;
            }
        }
        final boolean trailingComma = tokens.get(
            lastSig
        ).type == TokenType.PUNCT && ",".equals(
            tokens.get(lastSig).text
        );
        final int     contentEnd    = trailingComma ? lastSig : lastSig + 1;
        if(contentEnd <= nameStart) return null;
        int depth    = 0;
        int colonIdx = -1;
        int eqIdx    = -1;
        for(int k = nameStart; k < contentEnd; ++k) {
            final Token t = tokens.get(k);
                 if( t.type == TokenType.PUNCT && isOpenBracketText(t.text) ) depth++;
            else if( t.type == TokenType.PUNCT && isCloseBracketText(t.text) ) depth--;
            else if( depth == 0 && colonIdx < 0 && t.type == TokenType.PUNCT && ":".equals(
                t.text
            ) ) colonIdx = k;
            else if( depth == 0 && eqIdx < 0 && t.type == TokenType.OP && "=".equals(
                t.text
            ) ) eqIdx = k;
        } // for
        final int nameEnd        = colonIdx >= 0 ? colonIdx : (eqIdx >= 0 ? eqIdx : contentEnd);
        final int nameEndTrimmed = trimEndIdx(tokens, nameStart, nameEnd);
        if(nameEndTrimmed <= nameStart) return null;
        final List<Token> nameTokens    = tokens.subList(nameStart, nameEndTrimmed);
              List<Token> typeTokens    = new ArrayList<>();
              List<Token> defaultTokens = new ArrayList<>();
        if(colonIdx >= 0) {
            final int typeEnd   = eqIdx >= 0 ? eqIdx : contentEnd;
            final int typeStart = nextSignificant(tokens, colonIdx + 1, typeEnd);
            if(typeStart >= 0 && typeStart < typeEnd) typeTokens = tokens.subList(
                typeStart, trimEndIdx(tokens, typeStart, typeEnd)
            );
        } // if
        if(eqIdx >= 0) {
            final int defStart = nextSignificant(tokens, eqIdx + 1, contentEnd);
            if(defStart >= 0 && defStart < contentEnd) defaultTokens = tokens.subList(
                defStart, trimEndIdx(tokens, defStart, contentEnd)
            );
        }

        return new PyParam(nameTokens, typeTokens, defaultTokens, trailingComma);
    }

    /**
     * Scans backward from {@code end - 1} for the nearest non-gap token strictly at/after {@code
     *  start}, returning the index right after it (i.e. the exclusive end of the trimmed range), or
     *  {@code start} itself if no significant token is found in {@code [start, end)}.
     */
    private int trimEndIdx(final List<Token> tokens, final int start, final int end)
    {
        for(int k = end - 1; k >= start; --k) {
            if( !isGapToken( tokens.get(k) ) ) return k + 1;
        }

        return start;
    }

    // ── §7: Structural Pattern Matching (match/case) ────────────────────────────────────

    /**
     * One recognized {@code case pattern:} header on a single physical line: {@code
     *  [patternStart, patternEnd)} is the pattern's own trimmed token span (including any {@code if}
     *  guard clause -- the guard is part of the case's own header, not the body), {@code colonIdx}
     *  is the header-terminating `:` (found at bracket depth 0 relative to the pattern, so a mapping
     *  pattern's own `{"key": value}` colon -- inside `{}` -- is never mistaken for it), and {@code
     *  compact} is true when at least one significant, non-comment token follows the `:` on the same
     *  physical line (STYLE_PYTHON3.md §7/§8: the body stays same-line vs. drops to an indented
     *  block is read as-already-written, never decided here, same posture as §6's signature-grouping
     *  toward an already-broken-out parameter list).
     */
    private static final class CaseLine {

        final int     headerStart;
        final int     patternStart;
        final int     patternEnd;
        final int     colonIdx;
        final boolean compact;
        /**
         * True when this line is block-form as originally written (compact == false) but qualifies
         *  for §8's single-statement-body join -- see {@link #tryQualifyJoinBody}. Used so §7's
         *  all-or-nothing alignment decision is made against the shape the line WILL have after §8's
         *  join runs in this same pass, instead of the shape it currently has -- fixes the §7/§8
         *  join-then-align ordering non-idempotency (round1 leaves an unaligned block-form group
         *  alone, §8 joins it, and a naive round2 would then see already-compact input and align it,
         *  producing output that differs from round1's).
         */
        final boolean virtualJoin;
        final int     bodyContentStart;
        final int     bodyContentEnd;
        /**
         * The line's own physical end (only meaningful/used for a {@code compact} member's own
         *  post-alignment length check in {@link #flushCaseGroup} -- a {@code virtualJoin} member
         *  uses {@code bodyContentEnd} for that same purpose instead)
         */
        final int lineEnd;

        CaseLine(
            final int     headerStart,
            final int     patternStart,
            final int     patternEnd,
            final int     colonIdx,
            final boolean compact,
            final boolean virtualJoin,
            final int     bodyContentStart,
            final int     bodyContentEnd,
            final int     lineEnd
        )
        {
            this.headerStart      = headerStart;
            this.patternStart     = patternStart;
            this.patternEnd       = patternEnd;
            this.colonIdx         = colonIdx;
            this.compact          = compact;
            this.virtualJoin      = virtualJoin;
            this.bodyContentStart = bodyContentStart;
            this.bodyContentEnd   = bodyContentEnd;
            this.lineEnd          = lineEnd;
        }

    } // class CaseLine

    /**
     * Headers (by {@code RawLine.start}) whose §8 join was already performed -- with colon-alignment
     *  padding baked in -- by {@link #flushCaseGroup} this pass; {@link #applySingleStatementBody}
     *  consults this to avoid emitting a second, overlapping, unpadded join replacement for the same
     *  header. Cleared and repopulated at the start of every {@link #applyCaseColonAlignment} call.
     */
    private final Set<Integer> caseJoinAlignedHeaders = new HashSet<>();

    /**
     * Classifies each {@code rawLines} entry as a §7 {@code case} header or not, groups
     *  contiguous same-depth {@code case} lines (a blank line, a comment-only line, a depth change,
     *  or any non-{@code case} statement all break the group -- same boundary convention §2/§3 use),
     *  and -- **all-or-nothing** per STYLE_PYTHON3.md §7 -- aligns the `:` column across a group only
     *  when every member in it is already in compact one-line form; a group containing even one
     *  block-body {@code case} gets no alignment at all, not even for its own compact members.
     */
    private List<Replacement> applyCaseColonAlignment(
        final List<Token>   tokens,
        final List<RawLine> rawLines
    )
    {
        caseJoinAlignedHeaders.clear();
        final List<Replacement> replacements = new ArrayList<>();
              List<CaseLine>    group        = new ArrayList<>();
              int               groupDepth   = -1;
        for( int i = 0; i < rawLines.size(); ++i ) {
            final RawLine  line = rawLines.get(i);
            final CaseLine c    = classifyCaseLine(tokens, rawLines, i);
            if( c != null && ( group.isEmpty() || line.depth == groupDepth ) ) {
                group.add(c);
                groupDepth = line.depth;
                // A virtualJoin case's own body line (the next RawLine, one depth deeper) is already
                // accounted for by this CaseLine -- skip it here so it doesn't classify as null and
                // spuriously break the group, which would otherwise defeat contiguous-group alignment
                // for every block-form case (each would land in its own singleton group)
                if(c.virtualJoin) i++;
                continue;
            } // if
            flushCaseGroup(tokens, group, replacements);
            group = new ArrayList<>();
            if(c != null) {
                group.add(c);
                groupDepth = line.depth;
                if(c.virtualJoin) i++;
            }
            else {
                groupDepth = -1;
            }
        } // for
        flushCaseGroup(tokens, group, replacements);

        return replacements;
    }

    /**
     * Classifies one line as a §7 {@code case} header: {@code case} is tokenized as a plain {@code
     *  IDENTIFIER} (a context-sensitive soft keyword, per {@code TokenizerIndent}'s own {@code
     *  KEYWORDS_PYTHON} exclusion), so this checks its literal text rather than {@code
     *  TokenType.KEYWORD}. Rejects (returns {@code null}) a multi-physical-line {@code case} header
     *  (a wrapped pattern spanning a bracket/backslash continuation) -- same "documented gap, not a
     *  guess" precedent as every prior §2-§6 slice -- and any line with no top-level `:` at all.
     */
    private CaseLine classifyCaseLine(
        final List<Token>   tokens,
        final List<RawLine> rawLines,
        final int           lineIdx
    )
    {
        final RawLine line = rawLines.get(lineIdx);
        // Multi-physical-line `case` patterns retain §7's established all-or-nothing
        // colon-alignment behavior; §8's extended join support applies to the ordinary
        // if/elif/else/while/for headers handled below.
        if(line.multiPhysicalLine) return null;
        final int caseIdx = nextSignificant(tokens, line.contentStart, line.end);
        if( caseIdx < 0 || tokens.get(
            caseIdx
        ).type != TokenType.IDENTIFIER || !"case".equals(
            tokens.get(caseIdx).text
        ) ) return null;
        final int patternStart = nextSignificant(tokens, caseIdx + 1, line.end);
        if(patternStart < 0) return null;
        int depth    = 0;
        int colonIdx = -1;
        for(int k = patternStart; k < line.end; ++k) {
            final Token t = tokens.get(k);
            if( t.type == TokenType.PUNCT && isOpenBracketText(t.text) ) {
                ++depth;
            }
            else if( t.type == TokenType.PUNCT && isCloseBracketText(t.text) ) {
                --depth;
            }
            else if( depth == 0 && t.type == TokenType.PUNCT && ":".equals(t.text) ) {
                colonIdx = k;
                break;
            }
        } // for
        if(colonIdx < 0) return null;
        final int patternEnd = trimEndIdx(tokens, patternStart, colonIdx);
        if(patternEnd <= patternStart) return null;
        boolean compact                  = false;
        boolean headerHasTrailingComment = false;
        for(int k = colonIdx + 1; k < line.end; ++k) {
            final Token t = tokens.get(k);
            if(t.type == TokenType.COMMENT_LINE) {
                headerHasTrailingComment = true;
            }
            else if( !isGapToken(t) ) {
                compact = true;
                break;
            }
        } // for
        boolean virtualJoin      = false;
        int     bodyContentStart = -1;
        int     bodyContentEnd   = -1;
        // A block-form header carrying its own trailing comment must never virtual-join --
        // headerPrefix below stops at patternEnd (before the colon/comment), so joining would
        // silently drop that comment (real content loss, not just reformatting; same fix as
        // classifySingleStatementHeaderColon's own header-trailing-comment bail)
        if(!compact && !headerHasTrailingComment) {
            final int[] body = tryQualifyJoinBody(tokens, rawLines, lineIdx, line);
            if(body != null) {
                final String headerPrefix = verbatimLineText(tokens, line.start, patternEnd);
                final String bodyText     = verbatimLineText( tokens, body[0], body[1] );
                final String joined       = headerPrefix + ": " + bodyText;
                if( physicalLineLength(joined) <= lineLength ) {
                    virtualJoin      = true;
                    bodyContentStart = body[0];
                    bodyContentEnd   = body[1];
                }
            } // if
        } // if

        return new CaseLine(
            line.start, patternStart, patternEnd, colonIdx, compact, virtualJoin,
            bodyContentStart, bodyContentEnd, line.end
        );
    }

    /**
     * Returns {@code [bodyContentStart, bodyContentEnd)} for the single-statement body that would
     *  qualify for §8's join at header index {@code headerIdx}, or {@code null} if no such join
     *  qualifies -- mirrors {@link #applySingleStatementBody}'s own per-header qualification checks
     *  exactly (structural checks only; the joined-line length budget is each caller's own concern,
     *  since §7's caller needs to add its own alignment padding to that length before checking it)
     */
    private int[] tryQualifyJoinBody(
        final List<Token>   tokens,
        final List<RawLine> rawLines,
        final int           headerIdx,
        final RawLine       header
    )
    {
        if( headerIdx + 1 >= rawLines.size() ) return null;
        final RawLine body = rawLines.get(headerIdx + 1);
        if(body.depth != header.depth + 1) return null;
        final int bodyContentStart = nextSignificant(tokens, body.contentStart, body.end);
        if(bodyContentStart < 0) return null; // Blank line -- not a real statement
        if( tokens.get(
            bodyContentStart
        ).type == TokenType.COMMENT_LINE || tokens.get(
            bodyContentStart
        ).type == TokenType.COMMENT_BLOCK ) return null; // Comment-only line -- not a real statement
        if( headerIdx + 2 < rawLines.size() && rawLines.get(
            headerIdx + 2
        ).depth == body.depth ) return null; // A sibling line (second statement, or trailing blank/comment) remains
        final int bodyContentEnd = trimEndIdx(tokens, bodyContentStart, body.end);
        if( containsSemicolon(tokens, bodyContentStart, bodyContentEnd) ) return null;
        if( bodyOpensNewBlock(tokens, bodyContentStart, bodyContentEnd) ) return null;

        return new int[] {bodyContentStart, bodyContentEnd};
    }

    /**
     * Emits one {@link Replacement} per compact group member, padding the gap between its own
     *  trimmed pattern text and its `:` to the group's widest pattern width -- mirrors {@link
     *  #normalizeGap}'s "gap-only, no whole-line rebuild" shape (used for §4/§5's bracket/brace
     *  padding), just with a per-group computed width instead of a fixed tight/loose text. Does
     *  nothing at all (no replacements) when the group is empty or contains any block-body member,
     *  per §7's all-or-nothing rule.
     */
    private void flushCaseGroup(
        final List<Token>       tokens,
        final List<CaseLine>    group,
        final List<Replacement> replacements
    )
    {
        if( group.isEmpty() ) return;
        for(final CaseLine c : group) {
            // All-or-nothing: a case that is neither already-compact nor §8-join-eligible (a
            // genuine block-body member -- e.g. a multi-statement body, an overflowing join, or a
            // body that itself opens a nested block) abandons alignment for the whole group.
            if(!c.compact && !c.virtualJoin) return;
        }
        int maxLen = 0;
        for(final CaseLine c : group) maxLen = Math.max(
            maxLen, verbatimLineText(tokens, c.patternStart, c.patternEnd).length()
        );
        // Verify every member's own padded line still fits before committing -- padding can push a
        // line that fit unpadded past the limit once aligned, for a virtualJoin member (padding
        // combined with the newly-joined body content) exactly as much as for an already-compact
        // one (padding alone, widening an existing one-line `case X: stmt`). Checking only
        // virtualJoin members here let a group whose alignment gets abandoned this round (because
        // one virtualJoin member's padded+joined line would overflow) still have every member
        // individually joined by §8 afterwards -- on the NEXT round, with every member now already
        // compact, this same group would pass this check without the equivalent length guard,
        // silently applying padding this round abandoned, i.e. non-idempotent (cpython dogfood,
        // `Lib/turtle.py`'s `match param.kind` block: a `case _:` member's own short pattern needs
        // ~31 columns of padding to match a `VAR_POSITIONAL`-length sibling, which alone pushes its
        // line over the budget -- round1 correctly abandons the group's alignment (leaving §8's own
        // per-header join unaligned), but a naive round2 saw only already-compact members and
        // aligned them anyway, reproducing this exact bug on round2's own output).
        for(final CaseLine c : group) {
            final int    len     = verbatimLineText(tokens, c.patternStart, c.patternEnd).length();
            final String padding = padRightSpaces(maxLen - len);
            final String joined;
            if(c.virtualJoin) {
                final String headerPrefix = verbatimLineText(tokens, c.headerStart, c.patternEnd);
                final String bodyText     = verbatimLineText(
                    tokens, c.bodyContentStart, c.bodyContentEnd
                );
                joined = headerPrefix + padding + ": " + bodyText;
            } // if
            else {
                final String headerPrefix = verbatimLineText(tokens, c.headerStart, c.patternEnd);
                final String rest         = verbatimLineText(tokens, c.colonIdx, c.lineEnd);
                joined = headerPrefix + padding + rest;
            }
            if( physicalLineLength(
                joined
            ) > lineLength ) return; // Padding pushed this member over the line-length budget -- abandon the group
        } // for
        for(final CaseLine c : group) {
            final int    len     = verbatimLineText(tokens, c.patternStart, c.patternEnd).length();
            final String desired = padRightSpaces(maxLen - len);
            if(c.virtualJoin) {
                final String headerPrefix = verbatimLineText(tokens, c.headerStart, c.patternEnd);
                final String bodyText     = verbatimLineText(
                    tokens, c.bodyContentStart, c.bodyContentEnd
                );
                final String joined       = headerPrefix + desired + ": " + bodyText;
                replacements.add( new Replacement(c.headerStart, c.bodyContentEnd, joined) );
                caseJoinAlignedHeaders.add(c.headerStart);
            } // if
            else {
                final Replacement gap = normalizeGap(tokens, c.patternEnd, c.colonIdx, desired);
                if(gap != null) replacements.add(gap);
            }
        } // for
    }

    private String padRightSpaces(final int count)
    {
        final StringBuilder sb = new StringBuilder();
        for(int i = 0; i < count; ++i) sb.append(' ');

        return sb.toString();
    }

    // ── §8: Single-Statement Bodies ─────────────────────────────────────────────────────

    /**
     * Keywords whose already-block-form header ({@code header:} on its own line, body on the next
     *  indented line) is a §8 join candidate: `if`/`elif`/`else`/`while`/`for`. {@code case} is
     *  handled separately (reuses {@link #classifyCaseLine}, since `case` is a context-sensitive
     *  soft keyword tokenized as a plain {@code IDENTIFIER}, not a member of this set) --
     *  `def`/`class`/`try`/`except`/`finally`/`with` are deliberately never members, per
     *  STYLE_PYTHON3.md §8's explicit "never applies" list.
     */
    private static final Set<String> SINGLE_STMT_HEADER_KEYWORDS = new HashSet<>( Arrays.asList(
        "if", "elif", "else", "while", "for"
    ) );

    /**
     * STYLE_PYTHON3.md §8: joins a block already written as `header:` followed by exactly one
     *  indented simple-statement line back onto the header's own line (`header: statement`) when
     *  the joined line fits within {@code line-length}. An already-compact line over that limit
     *  expands into the ordinary indented-block form. Neither direction creates or extends a
     *  semicolon chain.
     *
     *  <p>A header qualifies only when: it is a single physical line (not {@code
     *  multiPhysicalLine} header/body preserves its internal layout), its first significant token is one of {@link
     *  #SINGLE_STMT_HEADER_KEYWORDS} or the {@code case} soft keyword (via {@link
     *  #classifyCaseLine}), and nothing but an optional trailing comment follows its own
     *  header-terminating `:` on the same physical line (i.e. it is genuinely block-form, not
     *  already compact). The immediately following {@link RawLine} must exist, sit exactly one
     *  depth deeper, not be blank/comment-only, and not
     *  itself open a further nested block (see {@link #bodyOpensNewBlock} -- a nested `if`/`for`/
     *  `while`/`with`/`match`/`def`/`class`/etc. always keeps its own indented block, never
     *  qualifies as the "simple statement" this pass joins). The line after the body (if any) must
     *  sit at a shallower depth than the body -- a sibling line still at the body's own depth means
     *  the block held more than one statement (or a trailing blank/comment line), and the whole
     *  join is skipped, consistent with every prior §2/§3 slice's own conservative "leave the group
     *  boundary alone" posture. A body trailing comment is retained verbatim; a body containing
     *  a semicolon is left untouched so this pass never produces a `;` chain.
     *
     *  <p><b>Ambiguity resolved conservatively, not guessed:</b> a body statement containing a
     *  `lambda` anywhere (`x = lambda: 1`) has its own top-level `:` that does not open a block, but
     *  distinguishing that from a genuine nested-compound-statement `:` reliably would need
     *  lambda-parameter-list-aware depth tracking this pass does not build -- {@link
     *  #bodyOpensNewBlock} conservatively treats any body containing a `lambda` keyword as "opens a
     *  new block" (i.e. never joined), sidestepping the ambiguity by leaving that narrow shape's
     *  block form untouched rather than risking a wrong join. The same conservative `lambda` check
     *  also guards the header's own condition-scanning colon search ({@link
     *  #classifySingleStmtHeader}), for the same reason.
     */
    private List<Replacement> applySingleStatementBody(
        final List<Token>   tokens,
        final List<RawLine> rawLines
    )
    {
        final List<Replacement> replacements = new ArrayList<>();
        for( int i = 0; i < rawLines.size(); ++i ) {
            final RawLine header = rawLines.get(i);
            // A `case` header already joined-with-alignment-padding by §7's flushCaseGroup this same
            // pass (see caseJoinAlignedHeaders' javadoc) must not also get a second, unpadded,
            // overlapping join here
            if( caseJoinAlignedHeaders.contains(header.start) ) continue;
            final int colonIdx = classifySingleStatementHeaderColon(tokens, rawLines, i);
            if(colonIdx >= 0) {
                final int[] body = tryQualifyJoinBody(tokens, rawLines, i, header);
                if(body == null) continue;
                final int    bodyContentStart = body[0];
                final int    bodyContentEnd   = body[1];
                final String headerText       = verbatimLineText(tokens, header.start, colonIdx + 1);
                final String bodyText         = verbatimLineText(
                    tokens, bodyContentStart, bodyContentEnd
                );
                final String joined           = headerText + " " + bodyText;
                if( physicalLineLength(joined) > lineLength ) continue;
                replacements.add( new Replacement(header.start, bodyContentEnd, joined) );
                continue;
            }

            final int compactColon = classifyCompactSingleStatementHeaderColon(tokens, rawLines, i);
            if(compactColon < 0) continue;
            final String compactText = verbatimLineText(tokens, header.start, header.end);
            if( physicalLineLength(compactText) <= lineLength ) continue;
            final int bodyStart = nextSignificant(tokens, compactColon + 1, header.end);
            if(bodyStart < 0) continue;
            final int bodyEnd = trimEndIdx(tokens, bodyStart, header.end);
            if( containsSemicolon(tokens, bodyStart, bodyEnd) ) continue;
            final String headerText = verbatimLineText(tokens, header.start, compactColon + 1);
            final String bodyText   = verbatimLineText(tokens, bodyStart, bodyEnd);
            replacements.add( new Replacement(
                header.start, bodyEnd, headerText + "\n" + leadingIndent(tokens, header)
                        + singleStatementIndentUnit() + bodyText
            ) );
        } // for

        return replacements;
    }

    /**
     * Length of {@code joined}'s longest physical line. Multi-physical-line headers and bodies
     * retain their internal layout when joined.
     */
    private int physicalLineLength(final String joined)
    {
        int max = 0;
        int len = 0;
        for(int i = 0; i < joined.length(); ++i) {
            if(joined.charAt(i) == '\n') {
                max = Math.max(max, len);
                len = 0;
            }
            else {
                ++len;
            }
        }

        return Math.max(max, len);
    }

    /** True iff a candidate body contains a semicolon. */
    private boolean containsSemicolon(final List<Token> tokens, final int from, final int to)
    {
        for(int i = from; i < to; ++i) {
            final Token t = tokens.get(i);
            if(t.type == TokenType.PUNCT && ";".equals(t.text)) return true;
        }

        return false;
    }

    private String leadingIndent(final List<Token> tokens, final RawLine line)
    {
        return verbatimLineText(tokens, line.start, line.contentStart);
    }

    private String singleStatementIndentUnit()
    {
        final StringBuilder out = new StringBuilder();
        for(int i = 0; i < indentWidth; ++i) out.append(' ');

        return out.toString();
    }

    /** True iff any token in {@code [from, to)} is a comment. */
    private boolean containsComment(final List<Token> tokens, final int from, final int to)
    {
        for(int i = from; i < to; ++i) {
            final TokenType ty = tokens.get(i).type;
            if(ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) return true;
        }

        return false;
    }

    /** Returns a qualifying compact header's body colon, for overflow expansion only. */
    private int classifyCompactSingleStatementHeaderColon(
        final List<Token>   tokens,
        final List<RawLine> rawLines,
        final int           lineIdx
    )
    {
        final RawLine line = rawLines.get(lineIdx);
        if(line.multiPhysicalLine) return -1;
        final int kwIdx = nextSignificant(tokens, line.contentStart, line.end);
        if(kwIdx < 0) return -1;
        final Token kw = tokens.get(kwIdx);
        if(kw.type == TokenType.IDENTIFIER && "case".equals(kw.text)) {
            final CaseLine c = classifyCaseLine(tokens, rawLines, lineIdx);
            return c != null && c.compact ? c.colonIdx : -1;
        }
        if( kw.type != TokenType.KEYWORD || !SINGLE_STMT_HEADER_KEYWORDS.contains(kw.text) ) return -1;
        int depth = 0;
        for(int k = kwIdx + 1; k < line.end; ++k) {
            final Token t = tokens.get(k);
            if(t.type == TokenType.KEYWORD && "lambda".equals(t.text)) return -1;
            if(t.type == TokenType.PUNCT && isOpenBracketText(t.text)) {
                ++depth;
            }
            else if(t.type == TokenType.PUNCT && isCloseBracketText(t.text)) {
                --depth;
            }
            else if(depth == 0 && t.type == TokenType.PUNCT && ":".equals(t.text)) {
                return nextSignificant(tokens, k + 1, line.end) >= 0 ? k : -1;
            }
        }

        return -1;
    }

    /**
     * Classifies {@code line} as a §8 block-form header, returning the index of its own
     *  header-terminating `:`, or {@code -1} if it doesn't qualify (already compact, not one of the
     *  qualifying keywords, multi-physical-line, or no depth-0 `:` found). Delegates to {@link
     *  #classifyCaseLine} for the {@code case} soft keyword (reusing §7's own header-colon/compact
     *  detection rather than re-deriving it); handles `if`/`elif`/`else`/`while`/`for` directly.
     */
    private int classifySingleStatementHeaderColon(
        final List<Token>   tokens,
        final List<RawLine> rawLines,
        final int           lineIdx
    )
    {
        final RawLine line = rawLines.get(lineIdx);
        final int kwIdx = nextSignificant(tokens, line.contentStart, line.end);
        if(kwIdx < 0) return -1;
        final Token kw = tokens.get(kwIdx);
        if( kw.type == TokenType.IDENTIFIER && "case".equals(kw.text) ) {
            final CaseLine c = classifyCaseLine(tokens, rawLines, lineIdx);
            // Only route through applySingleStatementBody's own generic join when
            // classifyCaseLine already confirmed a safe virtualJoin (which itself bails on a
            // header trailing comment) -- otherwise applySingleStatementBody's headerText would
            // stop at colonIdx + 1 and silently drop that comment.
            return c != null && !c.compact && c.virtualJoin ? c.colonIdx : -1;
        } // if
        if( kw.type != TokenType.KEYWORD || !SINGLE_STMT_HEADER_KEYWORDS.contains(
            kw.text
        ) ) return -1;
        int depth    = 0;
        int colonIdx = -1;
        for(int k = kwIdx + 1; k < line.end; ++k) {
            final Token t = tokens.get(k);
            if( t.type == TokenType.KEYWORD && "lambda".equals(
                t.text
            ) ) return -1; // Ambiguous condition colon vs lambda colon -- conservative bail
            if( t.type == TokenType.PUNCT && isOpenBracketText(t.text) ) {
                ++depth;
            }
            else if( t.type == TokenType.PUNCT && isCloseBracketText(t.text) ) {
                --depth;
            }
            else if( depth == 0 && t.type == TokenType.PUNCT && ":".equals(t.text) ) {
                colonIdx = k;
                break;
            }
        } // for
        if(colonIdx < 0) return -1;
        for(int k = colonIdx + 1; k < line.end; ++k) {
            final Token t = tokens.get(k);
            if(t.type == TokenType.COMMENT_LINE) {
                // A header trailing comment would otherwise be silently dropped by
                // applySingleStatementBody's headerText (which only takes tokens through
                // colonIdx + 1) -- conservatively bail rather than lose it, mirroring the
                // existing containsComment-based skip already applied to body statements
                return -1;
            } // if
            if( !isGapToken(
                t
            ) ) return -1; // Already compact (something besides a trailing comment follows `:`)
        } // for

        return colonIdx;
    }

    /**
     * True iff {@code [contentStart, contentEnd)} (a candidate body statement's own trimmed token
     *  span) itself opens a new nested block: a depth-0 `:` (a nested `if`/`for`/`while`/`with`/
     *  `match`/`def`/`class`/etc. header) or a `lambda` keyword anywhere in the span (conservative
     *  bail on the lambda-colon ambiguity -- see {@link #applySingleStatementBody}'s javadoc).
     *  `:=` is never at risk of being mistaken for either, since {@link
     *  com.jxmake.formatter.tokenizer.TokenizerIndent#emitWalrus} already merges it into a single
     *  pre-tokenized `OP`.
     */
    private boolean bodyOpensNewBlock(
        final List<Token> tokens,
        final int         contentStart,
        final int         contentEnd
    )
    {
        int depth = 0;
        for(int k = contentStart; k < contentEnd; ++k) {
            final Token t = tokens.get(k);
            if( t.type == TokenType.KEYWORD && "lambda".equals(t.text) ) return true;
                 if( t.type == TokenType.PUNCT && isOpenBracketText(t.text) )        depth++;
            else if( t.type == TokenType.PUNCT && isCloseBracketText(t.text) )       depth--;
            else if( depth == 0 && t.type == TokenType.PUNCT && ":".equals(t.text) ) return true;
        } // for

        return false;
    }

    // ── §9: Control Flow Blank Lines ─────────────────────────────────────────────────────

    /**
     * Tracks one active indentation frame while scanning {@code rawLines} linearly: {@code
     *  isFunctionBody} is true iff this frame was opened by a {@code def}/{@code async def} header
     *  (as opposed to {@code if}/{@code for}/{@code while}/{@code with}/{@code try}/{@code class}/
     *  {@code match}, all of which push a non-function frame); {@code sawContent} becomes true once
     *  at least one real statement has been processed directly inside this frame (mirrors {@code
     *  MiscRuleCurly.FuncFrame}'s own field of the same name and purpose for the C-family).
     */
    private static final class ControlFlowFrame {

        boolean isFunctionBody;
        boolean sawContent;

    } // class ControlFlowFrame

    /**
     * STYLE_PYTHON3.md §9: two independent blank-line-insertion rules, both direct ports of an
     *  already-implemented C-family precedent (§9.1 mirrors {@code
     *  MiscRuleCurly#insertBlankLineBeforeReturn}'s STYLE.md §9; §9.2 mirrors
     *  AI_PREAMBLE_FULL.md §12's default as literally stated in STYLE_PYTHON3.md §9.2's own text --
     *  the C-family's own {@code BlockStructureRule#placeElseOnOwnLine}, cited as the nominal C-family
     *  reference, turned out on inspection to only ever <em>preserve</em> an existing blank line
     *  before {@code else}, never actively insert one from a last-statement-content check; §9.2 is
     *  therefore implemented directly from STYLE_PYTHON3.md's own unambiguous text rather than by
     *  porting a mechanism that doesn't actually exist in the C-family code, since the style doc's
     *  wording ("add a blank line ... only when ...") leaves no ambiguity about the desired active
     *  behavior -- not a STATE_COMMON.md-blocking ambiguity, just a documented discrepancy between
     *  the task's framing and the actual C-family implementation). Both rules only ever ADD a
     *  missing blank line, exactly mirroring {@code insertBlankLineBeforeReturn}'s own
     *  "already-blank gap is left untouched" posture -- never removes one that's already present,
     *  even an extraneous one beyond a single blank line (no worked example in STYLE_PYTHON3.md §9
     *  shows a 2+-blank-line gap being collapsed, so this pass never collapses one either).
     *
     *  <p><b>§9.1</b>: a blank line is added directly before a {@code return} statement when it is
     *  the first significant token of its own {@link RawLine} (this alone, by construction, already
     *  excludes a §8 compact one-line body's own inline {@code return} -- e.g. {@code if x: return
     *  y}'s leading token is {@code if}, never classified here as a return statement at all, so the
     *  compact-form carve-out needs no special-case code) AND the innermost enclosing {@link
     *  ControlFlowFrame} is a function body ({@code isFunctionBody}) that has already seen at least
     *  one statement before this one ({@code sawContent} -- mirrors the C-family reference's own
     *  {@code top.sawContent} gate, and, like that reference, does not separately re-verify this is
     *  the function body's textually <em>final</em> statement; STYLE_PYTHON3.md's own prose describes
     *  the common case, but the actual C-family implementation this ports never adds that extra
     *  lookahead check either, so neither does this port). A {@code return} nested inside a further
     *  block (its own frame's {@code isFunctionBody} is false) never qualifies, regardless of what's
     *  on top of the frame stack above it -- matches the C-family reference's own "only the top
     *  frame is ever consulted" behavior exactly.
     *
     *  <p><b>§9.2</b>: a blank line is added directly before {@code elif}/{@code else} (any {@code
     *  else}, not just an {@code if}-{@code else} -- a Python {@code for}/{@code while}/{@code try}
     *  {@code else} is grammatically the same keyword and STYLE_PYTHON3.md §9.2's own text draws no
     *  distinction, so none is added here either) when the nearest preceding non-blank, non-comment
     *  {@link RawLine} (found via {@link #previousContentLine}, which walks past intervening blank/
     *  comment lines regardless of their depth) has {@code return}/{@code break}/{@code continue} --
     *  never {@code raise}, matching the C-family list's own existing omission exactly, not a Python-
     *  specific extension -- as its own first significant token. Walking past comment/blank lines
     *  (rather than requiring the immediately preceding line to be exactly the exiting statement)
     *  means this correctly finds the deepest last leaf statement of the preceding block without any
     *  depth bookkeeping of its own: Python's strict block nesting guarantees the last non-blank/
     *  non-comment line textually before the {@code elif}/{@code else} is always that block's own
     *  deepest last leaf statement, however many levels of nested compound statements it sits inside
     *  -- confirmed by construction, not guessed. Matching this exit-statement check against only the
     *  <em>first</em> significant token of that {@link RawLine} means (same "by construction, not a
     *  special case" reasoning as §9.1's own compact-form exclusion) a §8-compact preceding block
     *  whose own sole statement happens to be {@code return}/{@code break}/{@code continue} (e.g. an
     *  already-compact {@code if x: return y} as the immediately preceding block) is never recognized
     *  as ending in an exit here, since that {@link RawLine}'s own leading token is {@code if}, not
     *  {@code return} -- a real, deliberate consequence of reusing the same by-construction exclusion
     *  strategy, not independently re-verified via its own dedicated smoke case this slice (documented
     *  here rather than guessed silently).
     *
     *  <p><b>Fixed 2026-08-11</b> (were previously documented gaps here, see STATE_PYTHON3.md for the
     *  before/after): a {@code def} header written as a multi-physical-line (wrapped) parameter list
     *  IS now recognized by {@link #isDefHeaderLine} -- only its leading token was ever inspected, so
     *  no multi-line-aware handling was actually needed, the earlier conservative bail was stricter
     *  than the check required. A semicolon-chained statement (e.g. {@code x = 1; return y}) IS now
     *  recognized by both halves: §9.1 via {@link #lineHasTopLevelReturnSegment} (any top-level
     *  segment, not just the line's first), §9.2 via {@link #isUnconditionalExitLine}'s {@link
     *  #lastSemicolonSegmentStart} (the line's textually-LAST segment, matching "last statement of the
     *  preceding block"). A §8-compact preceding block (e.g. {@code if x: return y} as the block
     *  immediately before an {@code elif}/{@code else}) IS now recognized by §9.2 via {@link
     *  #isUnconditionalExitLine}'s {@link #classifySingleStatementHeaderColon}-family delegation to
     *  {@link #classifyCompactSingleStatementHeaderColon}, which narrows the search to the body span
     *  past the header colon rather than the header keyword itself.
     *
     *  <p><b>Still explicitly NOT covered</b> (documented scope boundary, not a guess): {@code
     *  try}/{@code except}/{@code finally} blank-line placement is entirely out of scope --
     *  STYLE_PYTHON3.md §9.2's own text names only {@code elif}/{@code else}, so extending this rule
     *  to {@code except}/{@code finally} would be inventing a new rule the style doc itself doesn't
     *  specify (what "the preceding block's last statement" even means for a {@code try} body whose
     *  normal-exit path never reaches {@code except} at all is a genuine design question, not a
     *  mechanical extension of the existing elif/else recognizer) -- left as a follow-up requiring an
     *  explicit style-doc decision first, not attempted here. A {@code return}/{@code elif}/{@code
     *  else} whose immediately preceding {@link RawLine} is a comment-only line (no blank line of its
     *  own) is conservatively left untouched -- same "no worked example to guess a relocation from"
     *  posture the C-family reference itself uses for Java/C++ (as opposed to Kotlin's own separately-
     *  carved-out comment-relocation behavior, which has no Python analog here).
     */
    private List<Replacement> applyControlFlowBlankLines(
        final List<Token>   tokens,
        final List<RawLine> rawLines
    )
    {
        final List<Replacement>       replacements    = new ArrayList<>();
        final Deque<ControlFlowFrame> stack           = new ArrayDeque<>();
              RawLine                 lastContentLine = null;

        for( int i = 0; i < rawLines.size(); ++i ) {
            final RawLine line = rawLines.get(i);
            final int     sig  = nextSignificant(tokens, line.contentStart, line.end);
            if(sig < 0) continue; // Blank line -- doesn't affect frames or content tracking
            final Token first = tokens.get(sig);
            if(first.type == TokenType.COMMENT_LINE || first.type == TokenType.COMMENT_BLOCK) continue; // Comment-only line -- doesn't affect frames or content tracking

            while( stack.size() > line.depth ) stack.pop();
            while( stack.size() < line.depth ) {
                final ControlFlowFrame f = new ControlFlowFrame();
                f.isFunctionBody = stack.size() == line.depth - 1 && lastContentLine != null
                                 && isDefHeaderLine(tokens, lastContentLine);
                stack.push(f);
            }
            final ControlFlowFrame top = stack.peek();

            if( top != null && top.isFunctionBody && top.sawContent
                    && lineHasTopLevelReturnSegment(tokens, line.contentStart, line.end) ) {
                final Replacement r = insertBlankLineBefore(tokens, rawLines, i, line);
                if(r != null) replacements.add(r);
            }

            if( first.type == TokenType.KEYWORD && ( "elif".equals(
                first.text
            ) || "else".equals(
                first.text
            ) ) ) {
                final int prevIdx = previousContentLineIndex(tokens, rawLines, i);
                if( prevIdx >= 0 && isUnconditionalExitLine(tokens, rawLines, prevIdx) ) {
                    final Replacement r = insertBlankLineBefore(tokens, rawLines, i, line);
                    if(r != null) replacements.add(r);
                }
            } // if

            if(top != null) top.sawContent = true;
            lastContentLine = line;
        } // for

        return replacements;
    }

    /**
     * True iff {@code line}'s first significant token is {@code def} (optionally preceded by {@code
     *  async}) -- i.e. it opens a function-body frame. Deliberately also recognizes a {@code
     *  multiPhysicalLine} header (a wrapped parameter list): only the header's own leading token is
     *  ever inspected, which is unaffected by how many physical lines the parameter list itself
     *  spans, so no additional handling is needed for that case (fixed 2026-08-11, see {@link
     *  #applyControlFlowBlankLines}'s javadoc).
     */
    private boolean isDefHeaderLine(final List<Token> tokens, final RawLine line)
    {
        int idx = nextSignificant(tokens, line.contentStart, line.end);
        if(idx < 0) return false;
        Token t = tokens.get(idx);
        if( t.type == TokenType.KEYWORD && "async".equals(t.text) ) {
            idx = nextSignificant(tokens, idx + 1, line.end);
            if(idx < 0) return false;
            t = tokens.get(idx);
        }

        return t.type == TokenType.KEYWORD && "def".equals(t.text);
    }

    /**
     * Walks backward from {@code idx - 1}, skipping blank and comment-only lines, and returns the
     *  index of the nearest genuine-content {@link RawLine}, or {@code -1} if none exists before
     *  {@code idx}. Returns an index (rather than the {@link RawLine} itself, as the prior version of
     *  this method did) so callers such as {@link #isUnconditionalExitLine} can also consult {@code
     *  rawLines}' own §8-compact-header classification for that same line, which itself needs the
     *  line's index, not just its content.
     */
    private int previousContentLineIndex(
        final List<Token>   tokens,
        final List<RawLine> rawLines,
        final int           idx
    )
    {
        for(int k = idx - 1; k >= 0; --k) {
            final RawLine candidate = rawLines.get(k);
            final int     sig       = nextSignificant(
                tokens, candidate.contentStart, candidate.end
            );
            if(sig < 0) continue;
            final TokenType ty = tokens.get(sig).type;
            if(ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) continue;
            return k;
        } // for

        return -1;
    }

    /**
     * True iff {@code rawLines.get(lineIdx)}'s effective LAST statement is exactly {@code return}/
     *  {@code break}/{@code continue} -- deliberately NOT {@code raise}, matching the C-family
     *  reference list's own existing omission (STYLE_PYTHON3.md §9.2). Two shapes beyond a plain
     *  single-statement line are recognized (fixed 2026-08-11, see {@link
     *  #applyControlFlowBlankLines}'s javadoc): (1) a §8-compact header (e.g. {@code if x: return y})
     *  -- delegates to {@link #classifyCompactSingleStatementHeaderColon} to find the body's own
     *  start just past the header colon, since that's this line's real last (and only) statement, not
     *  the header keyword; (2) a semicolon-chained line (e.g. {@code x = 1; return y}) -- {@link
     *  #lastSemicolonSegmentStart} finds the leading token of the LAST top-level {@code ;}-delimited
     *  sub-statement, which is this line's true textually-last statement regardless of how many
     *  segments precede it. Both shapes compose (a compact header whose body itself is semicolon-
     *  chained is handled by narrowing to the body span first, then finding that span's own last
     *  semicolon segment).
     */
    private boolean isUnconditionalExitLine(
        final List<Token>   tokens,
        final List<RawLine> rawLines,
        final int           lineIdx
    )
    {
        final RawLine line  = rawLines.get(lineIdx);
              int      start = line.contentStart;
        final int      colonIdx = classifyCompactSingleStatementHeaderColon(tokens, rawLines, lineIdx);
        if(colonIdx >= 0) start = colonIdx + 1;
        final int sig = lastSemicolonSegmentStart(tokens, start, line.end);
        if(sig < 0) return false;
        final Token t = tokens.get(sig);

        return t.type == TokenType.KEYWORD
                && ( "return".equals(
                    t.text
                ) || "break".equals(
                    t.text
                ) || "continue".equals(
                    t.text
                ) );
    }

    /**
     * Returns the index of the leading significant token of the LAST top-level ({@code ;}-delimited,
     *  bracket-depth-0) sub-statement within {@code [start, end)}, or {@code -1} if that span holds no
     *  significant token at all. When {@code [start, end)} contains no top-level {@code ;}, this is
     *  simply the span's own first significant token -- same result as a plain {@code
     *  nextSignificant(tokens, start, end)} call, so a line with no semicolon chaining is unaffected.
     */
    private int lastSemicolonSegmentStart(final List<Token> tokens, final int start, final int end)
    {
        int depth   = 0;
        int lastSemi = start - 1;
        for(int k = start; k < end; ++k) {
            final Token t = tokens.get(k);
                 if( t.type == TokenType.PUNCT && isOpenBracketText(t.text) )  ++depth;
            else if( t.type == TokenType.PUNCT && isCloseBracketText(t.text) ) --depth;
            else if( depth == 0 && t.type == TokenType.PUNCT && ";".equals(t.text) ) lastSemi = k;
        } // for

        return nextSignificant(tokens, lastSemi + 1, end);
    }

    /**
     * True iff any top-level ({@code ;}-delimited, bracket-depth-0) sub-statement within {@code
     *  [start, end)} leads with the {@code return} keyword -- covers both a plain {@code return}-first
     *  line (the pre-existing §9.1 shape) and a semicolon-chained line where {@code return} is a LATER
     *  sub-statement (e.g. {@code x = 1; return y}) (fixed 2026-08-11, see {@link
     *  #applyControlFlowBlankLines}'s javadoc). The latter shape still inserts the blank line before
     *  the whole physical line, not immediately before the {@code return} sub-statement itself --
     *  this pass only ever inserts a line break between two existing {@link RawLine}s, never splits
     *  one physical line into two, so a mid-line separation is not achievable at this granularity;
     *  separating the whole line from what precedes it is the closest conservative approximation.
     */
    private boolean lineHasTopLevelReturnSegment(
        final List<Token> tokens,
        final int         start,
        final int         end
    )
    {
        int depth    = 0;
        int segStart = start;
        for(int k = start; k <= end; ++k) {
            final boolean atEnd = k == end;
            final Token   t     = atEnd ? null : tokens.get(k);
            if( !atEnd && t.type == TokenType.PUNCT && isOpenBracketText(t.text) ) {
                ++depth;
                continue;
            }
            if( !atEnd && t.type == TokenType.PUNCT && isCloseBracketText(t.text) ) {
                --depth;
                continue;
            }
            if( atEnd || ( depth == 0 && t.type == TokenType.PUNCT && ";".equals(t.text) ) ) {
                final int sig = nextSignificant(tokens, segStart, k);
                if( sig >= 0 && tokens.get(sig).type == TokenType.KEYWORD
                        && "return".equals(tokens.get(sig).text) ) return true;
                segStart = k + 1;
            } // if
        } // for

        return false;
    }

    /**
     * Returns a zero-width {@link Replacement} inserting one blank line (a bare {@code "\n"})
     *  immediately before {@code line}'s own leading indentation, or {@code null} if a blank line is
     *  already present there (idempotent -- matches the C-family reference's own "already-blank gap
     *  left untouched" posture) or if the immediately preceding {@link RawLine} is a comment-only
     *  line (conservative skip, see {@link #applyControlFlowBlankLines}'s javadoc). {@code idx == 0}
     *  (no preceding line at all) also returns {@code null} -- nothing to separate a blank line from.
     */
    private Replacement insertBlankLineBefore(
        final List<Token>   tokens,
        final List<RawLine> rawLines,
        final int           idx,
        final RawLine       line
    )
    {
        if(idx == 0) return null;
        final RawLine prevRaw = rawLines.get(idx - 1);
        final int     prevSig = nextSignificant(tokens, prevRaw.contentStart, prevRaw.end);
        if(prevSig < 0) return null; // Already blank
        final TokenType prevTy = tokens.get(prevSig).type;
        if(prevTy == TokenType.COMMENT_LINE || prevTy == TokenType.COMMENT_BLOCK) return null; // Comment directly adjacent -- conservative skip
        return new Replacement(line.start, line.start, "\n");
    }

    /**
     * Reassembles {@code tokens}' source text verbatim. Every token kind's {@code text} is its
     *  exact original source span, EXCEPT the synthesized {@code INDENT}/{@code DEDENT} markers
     *  (see {@link TokenizerIndent#synthesizeIndentation}), which carry no source text of their
     *  own (their {@code text} field instead holds the new indent width, for a later rule pass's
     *  use) and so must be skipped here rather than appended.
     *
     *  <p>Two independently-computed passes' replacements can legitimately overlap in token-index
     *  range (e.g. {@link #applySingleStatementBody}'s header+body span and {@link
     *  #applyAssignmentAlignment}'s own trivial single-member-group span for that same body
     *  statement) -- per this class's established "wider, earlier-sorted replacement wins" posture
     *  (see {@link #applySingleStatementBody}'s javadoc), any later replacement whose {@code start}
     *  has already been passed by {@code i} must be discarded, not left for a future exact-match
     *  check that can never succeed once {@code i} has moved beyond it -- leaving it in place would
     *  permanently stall the {@code r} cursor and silently drop every subsequent replacement in the
     *  file, not just the one genuinely-overlapping entry.
     */
    private String render(final List<Token> tokens, final List<Replacement> replacements)
    {
        final StringBuilder out = new StringBuilder();
        final int           n   = tokens.size();
              int           i   = 0;
              int           r   = 0;
        while(i < n) {
            while( r < replacements.size() && replacements.get(r).start < i ) r++;
            if( r < replacements.size() && replacements.get(r).start == i ) {
                out.append( replacements.get(r).text );
                i = replacements.get(r).end;
                ++r;
                continue;
            }
            final Token t = tokens.get(i);
            if(t.type != TokenType.INDENT && t.type != TokenType.DEDENT) out.append(t.text);
            ++i;
        } // while

        return out.toString();
    }

} // class ScopePipelineIndent
