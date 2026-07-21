/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.MiscRuleIndent;
import com.jxmake.formatter.rules.MiscRuleIndent.PyAssignment;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;
import com.jxmake.formatter.tokenizer.TokenizerIndent;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;

import java.util.ArrayList;
import java.util.List;

/**
 * Indentation-block-family scope pipeline (Python3 -- see STATE_PYTHON3.md). Tokenizes via
 * {@link TokenizerIndent}, applies STYLE_PYTHON3.md §2's assignment-alignment pass ({@link
 * #applyAssignmentAlignment}), and renders the (possibly-replaced) token stream back to source
 * text -- every token kind's original text passes through verbatim except a grouped assignment's
 * own `target...value` span (replaced with its padded/aligned rendering) and the synthesized
 * zero-text {@code INDENT}/{@code DEDENT} markers (always skipped). Further §1-9 rule passes land
 * as later calls inside {@link #process}, mirroring {@link ScopePipelineCurly}'s own multi-pass
 * shape.
 */
public final class ScopePipelineIndent extends ScopePipelineCore {

    private final Lang lang;
    private final MiscRuleIndent miscRule;

    public ScopePipelineIndent(final Lang lang, final int indentWidth) {
        super(indentWidth);
        this.lang = lang;
        this.miscRule = new MiscRuleIndent(lang, false, false, false, indentWidth, 0);
    }

    @Override
    public String process(final String source) {
        final List<Token> tokens = new TokenizerIndent(lang).tokenize(source);
        final List<Replacement> replacements = applyAssignmentAlignment(tokens);
        return render(tokens, replacements);
    }

    /** One logical line (STYLE_PYTHON3.md §2 unit of work): {@code [start, end)} is its full token
     *  range including its own leading {@code INDENT}/{@code DEDENT} markers (if any) and its
     *  terminating {@code NEWLINE} (absent only for a file's last line). {@code depth} is the
     *  indentation depth this line's own statement lives at, after applying its leading markers.
     *  {@code assignment} is non-null only for a recognized single-physical-line `identifier (op)=
     *  value` candidate (a multi-line right-hand side -- i.e. a line range spanning more than one
     *  {@code NEWLINE} token via bracket/backslash continuation -- is deliberately never populated
     *  here; STYLE_PYTHON3.md §2's continuation examples are an explicitly deferred gap, same
     *  "documented gap, not a guess" precedent used elsewhere in this job). */
    private static final class Line {
        final int start;
        final int end;
        final int depth;
        final PyAssignment assignment;
        final int assignStart; // target token index, meaningful only when assignment != null
        final int assignEnd;   // exclusive, index right after the last value token

        Line(final int start, final int end, final int depth, final PyAssignment assignment,
                final int assignStart, final int assignEnd) {
            this.start = start;
            this.end = end;
            this.depth = depth;
            this.assignment = assignment;
            this.assignStart = assignStart;
            this.assignEnd = assignEnd;
        }
    }

    /** Splits {@code tokens} into logical lines (see {@link Line}), tracking indentation depth via
     *  the synthesized {@code INDENT}/{@code DEDENT} markers and classifying each as a §2 assignment
     *  candidate or not, then groups consecutive same-depth candidates (a blank line, a comment-only
     *  line, a depth change, or a non-candidate statement all break the group -- STYLE_PYTHON3.md §2:
     *  "a blank line or a comment breaks the group") and returns one {@link Replacement} per grouped
     *  assignment, replacing only its own `target...value` span -- the line's own indentation,
     *  surrounding blank lines/comments, and any trailing same-line comment are left untouched. */
    private List<Replacement> applyAssignmentAlignment(final List<Token> tokens) {
        final List<Line> lines = splitLogicalLines(tokens);
        final List<Replacement> replacements = new ArrayList<>();
        List<Line> group = new ArrayList<>();
        int groupDepth = -1;
        for (final Line line : lines) {
            if (line.assignment != null && (group.isEmpty() || line.depth == groupDepth)) {
                group.add(line);
                groupDepth = line.depth;
                continue;
            }
            flushGroup(group, replacements);
            group = new ArrayList<>();
            if (line.assignment != null) {
                group.add(line);
                groupDepth = line.depth;
            } else {
                groupDepth = -1;
            }
        }
        flushGroup(group, replacements);
        return replacements;
    }

    private void flushGroup(final List<Line> group, final List<Replacement> replacements) {
        if (group.isEmpty()) {
            return;
        }
        final List<PyAssignment> assignments = new ArrayList<>();
        for (final Line line : group) {
            assignments.add(line.assignment);
        }
        final List<String> rendered = miscRule.renderPyGroup(assignments);
        for (int i = 0; i < group.size(); i++) {
            final Line line = group.get(i);
            replacements.add(new Replacement(line.assignStart, line.assignEnd, rendered.get(i)));
        }
    }

    private List<Line> splitLogicalLines(final List<Token> tokens) {
        final List<Line> lines = new ArrayList<>();
        final int n = tokens.size();
        int i = 0;
        int depth = 0;
        while (i < n) {
            int p = i;
            if (p < n && tokens.get(p).type == TokenType.WHITESPACE) {
                p++;
            }
            while (p < n && tokens.get(p).type == TokenType.INDENT) {
                depth++;
                p++;
            }
            while (p < n && tokens.get(p).type == TokenType.DEDENT) {
                depth--;
                p++;
            }
            final int contentStart = p;
            int j = p;
            while (j < n) {
                final Token t = tokens.get(j);
                if (t.type == TokenType.NEWLINE) {
                    final boolean insideBrackets = t.parenDepth > 0;
                    final boolean backslash = j > 0 && tokens.get(j - 1).type == TokenType.OP
                            && "\\".equals(tokens.get(j - 1).text);
                    j++;
                    if (!insideBrackets && !backslash) {
                        break;
                    }
                    continue;
                }
                j++;
            }
            lines.add(classify(tokens, i, j, contentStart, depth));
            i = j;
        }
        return lines;
    }

    /** Classifies one line's content range {@code [contentStart, end)} as a §2 assignment candidate
     *  or not. Rejects (returns a non-assignment {@link Line}) unless: the first significant token
     *  is a bare {@code IDENTIFIER}, the next significant token is an {@code OP} in {@link
     *  com.jxmake.formatter.rules.MiscRuleCore#ASSIGNMENT_OPS}, the range contains no internal
     *  {@code NEWLINE} (i.e. is not a bracket/backslash continuation -- multi-line RHS is out of
     *  scope for this slice), and at least one significant, non-comment value token follows. */
    private Line classify(final List<Token> tokens, final int start, final int end,
            final int contentStart, final int depth) {
        for (int k = contentStart; k < end - 1; k++) {
            if (tokens.get(k).type == TokenType.NEWLINE) {
                return new Line(start, end, depth, null, -1, -1);
            }
        }
        final int targetIdx = nextSignificant(tokens, contentStart, end);
        if (targetIdx < 0 || tokens.get(targetIdx).type != TokenType.IDENTIFIER) {
            return new Line(start, end, depth, null, -1, -1);
        }
        final int opIdx = nextSignificant(tokens, targetIdx + 1, end);
        if (opIdx < 0 || tokens.get(opIdx).type != TokenType.OP
                || !MiscRuleIndent.isAssignmentOp(tokens.get(opIdx).text)) {
            return new Line(start, end, depth, null, -1, -1);
        }
        final int valueFrom = nextSignificant(tokens, opIdx + 1, end);
        if (valueFrom < 0) {
            return new Line(start, end, depth, null, -1, -1);
        }
        int lastValueIdx = -1;
        for (int k = end - 1; k >= valueFrom; k--) {
            final Token t = tokens.get(k);
            if (t.type == TokenType.IDENTIFIER || t.type == TokenType.KEYWORD || t.type == TokenType.NUMBER
                    || t.type == TokenType.STRING || t.type == TokenType.CHAR || t.type == TokenType.OP
                    || t.type == TokenType.PUNCT) {
                lastValueIdx = k;
                break;
            }
        }
        if (lastValueIdx < 0) {
            return new Line(start, end, depth, null, -1, -1);
        }
        final PyAssignment assignment = new PyAssignment(tokens.get(targetIdx), tokens.get(opIdx),
                tokens.subList(valueFrom, lastValueIdx + 1));
        return new Line(start, end, depth, assignment, targetIdx, lastValueIdx + 1);
    }

    private int nextSignificant(final List<Token> tokens, final int from, final int to) {
        int i = from;
        while (i < to && isGapToken(tokens.get(i))) {
            i++;
        }
        return i < to ? i : -1;
    }

    /** Reassembles {@code tokens}' source text verbatim. Every token kind's {@code text} is its
     *  exact original source span, EXCEPT the synthesized {@code INDENT}/{@code DEDENT} markers
     *  (see {@link TokenizerIndent#synthesizeIndentation}), which carry no source text of their
     *  own (their {@code text} field instead holds the new indent width, for a later rule pass's
     *  use) and so must be skipped here rather than appended. */
    private String render(final List<Token> tokens, final List<Replacement> replacements) {
        final StringBuilder out = new StringBuilder();
        final int n = tokens.size();
        int i = 0;
        int r = 0;
        while (i < n) {
            if (r < replacements.size() && replacements.get(r).start == i) {
                out.append(replacements.get(r).text);
                i = replacements.get(r).end;
                r++;
                continue;
            }
            final Token t = tokens.get(i);
            if (t.type != TokenType.INDENT && t.type != TokenType.DEDENT) {
                out.append(t.text);
            }
            i++;
        }
        return out.toString();
    }
}
