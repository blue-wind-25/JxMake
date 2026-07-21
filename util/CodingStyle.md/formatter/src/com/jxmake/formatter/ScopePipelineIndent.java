/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

import com.jxmake.formatter.rules.MiscRuleIndent;
import com.jxmake.formatter.rules.MiscRuleIndent.PyAssignment;
import com.jxmake.formatter.rules.MiscRuleIndent.PyImport;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;
import com.jxmake.formatter.tokenizer.TokenizerIndent;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isKeyword;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        final List<RawLine> rawLines = splitRawLines(tokens);
        final List<Replacement> replacements = new ArrayList<>();
        replacements.addAll(applyAssignmentAlignment(tokens, rawLines));
        replacements.addAll(applyImportSort(tokens, rawLines));
        replacements.sort(Comparator.comparingInt(r -> r.start));
        return render(tokens, replacements);
    }

    /** One logical line: {@code [start, end)} is its full token range including its own leading
     *  {@code INDENT}/{@code DEDENT} markers (if any) and its terminating {@code NEWLINE} (absent
     *  only for a file's last line); {@code contentStart} is the first token after those leading
     *  markers/whitespace. {@code depth} is the indentation depth this line's own statement lives
     *  at, after applying its own leading markers. A line whose range spans more than one {@code
     *  NEWLINE} token (bracket/backslash continuation) is a multi-physical-line statement -- every
     *  rule pass in this class treats those as unrecognized (out of scope for this slice, same
     *  "documented gap, not a guess" precedent used throughout this job) rather than guessing at
     *  their shape. */
    private static final class RawLine {
        final int start;
        final int end;
        final int contentStart;
        final int depth;
        final boolean multiPhysicalLine;

        RawLine(final int start, final int end, final int contentStart, final int depth,
                final boolean multiPhysicalLine) {
            this.start = start;
            this.end = end;
            this.contentStart = contentStart;
            this.depth = depth;
            this.multiPhysicalLine = multiPhysicalLine;
        }
    }

    private List<RawLine> splitRawLines(final List<Token> tokens) {
        final List<RawLine> lines = new ArrayList<>();
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
            boolean multi = false;
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
                    multi = true;
                    continue;
                }
                j++;
            }
            lines.add(new RawLine(i, j, contentStart, depth, multi));
            i = j;
        }
        return lines;
    }

    private int nextSignificant(final List<Token> tokens, final int from, final int to) {
        int i = from;
        while (i < to && isGapToken(tokens.get(i))) {
            i++;
        }
        return i < to ? i : -1;
    }

    // ── §2: Assignment Alignment ─────────────────────────────────────────────────────

    /** Classifies each {@code rawLines} entry as a §2 assignment candidate or not, then groups
     *  consecutive same-depth candidates (a blank line, a comment-only line, a depth change, or a
     *  non-candidate statement all break the group -- STYLE_PYTHON3.md §2: "a blank line or a
     *  comment breaks the group") and returns one {@link Replacement} per grouped assignment,
     *  replacing only its own `target...value` span -- the line's own indentation, surrounding
     *  blank lines/comments, and any trailing same-line comment are left untouched. */
    private List<Replacement> applyAssignmentAlignment(final List<Token> tokens, final List<RawLine> rawLines) {
        final List<Replacement> replacements = new ArrayList<>();
        List<PyAssignment> group = new ArrayList<>();
        List<int[]> groupSpans = new ArrayList<>(); // [assignStart, assignEnd] per group member
        int groupDepth = -1;
        for (final RawLine line : rawLines) {
            final PyAssignment a = line.multiPhysicalLine ? null : classifyAssignment(tokens, line);
            if (a != null && (group.isEmpty() || line.depth == groupDepth)) {
                group.add(a);
                groupSpans.add(new int[] { indexOf(tokens, a.target), lastIndexOfValue(tokens, a) });
                groupDepth = line.depth;
                continue;
            }
            flushAssignmentGroup(group, groupSpans, replacements);
            group = new ArrayList<>();
            groupSpans = new ArrayList<>();
            if (a != null) {
                group.add(a);
                groupSpans.add(new int[] { indexOf(tokens, a.target), lastIndexOfValue(tokens, a) });
                groupDepth = line.depth;
            } else {
                groupDepth = -1;
            }
        }
        flushAssignmentGroup(group, groupSpans, replacements);
        return replacements;
    }

    private int indexOf(final List<Token> tokens, final Token target) {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i) == target) {
                return i;
            }
        }
        return -1;
    }

    private int lastIndexOfValue(final List<Token> tokens, final PyAssignment a) {
        final Token last = a.valueTokens.get(a.valueTokens.size() - 1);
        for (int i = tokens.size() - 1; i >= 0; i--) {
            if (tokens.get(i) == last) {
                return i + 1;
            }
        }
        return -1;
    }

    private void flushAssignmentGroup(final List<PyAssignment> group, final List<int[]> spans,
            final List<Replacement> replacements) {
        if (group.isEmpty()) {
            return;
        }
        final List<String> rendered = miscRule.renderPyGroup(group);
        for (int i = 0; i < group.size(); i++) {
            replacements.add(new Replacement(spans.get(i)[0], spans.get(i)[1], rendered.get(i)));
        }
    }

    /** Classifies one line's content range as a §2 assignment candidate. Rejects unless: the first
     *  significant token is a bare {@code IDENTIFIER}, the next significant token is an {@code OP}
     *  in {@link com.jxmake.formatter.rules.MiscRuleCore#ASSIGNMENT_OPS}, and at least one
     *  significant, non-comment value token follows. */
    private PyAssignment classifyAssignment(final List<Token> tokens, final RawLine line) {
        final int targetIdx = nextSignificant(tokens, line.contentStart, line.end);
        if (targetIdx < 0 || tokens.get(targetIdx).type != TokenType.IDENTIFIER) {
            return null;
        }
        final int opIdx = nextSignificant(tokens, targetIdx + 1, line.end);
        if (opIdx < 0 || tokens.get(opIdx).type != TokenType.OP
                || !MiscRuleIndent.isAssignmentOp(tokens.get(opIdx).text)) {
            return null;
        }
        final int valueFrom = nextSignificant(tokens, opIdx + 1, line.end);
        if (valueFrom < 0) {
            return null;
        }
        int lastValueIdx = -1;
        for (int k = line.end - 1; k >= valueFrom; k--) {
            final Token t = tokens.get(k);
            if (t.type == TokenType.IDENTIFIER || t.type == TokenType.KEYWORD || t.type == TokenType.NUMBER
                    || t.type == TokenType.STRING || t.type == TokenType.CHAR || t.type == TokenType.OP
                    || t.type == TokenType.PUNCT) {
                lastValueIdx = k;
                break;
            }
        }
        if (lastValueIdx < 0) {
            return null;
        }
        return new PyAssignment(tokens.get(targetIdx), tokens.get(opIdx),
                tokens.subList(valueFrom, lastValueIdx + 1));
    }

    // ── §3: Import Ordering ──────────────────────────────────────────────────────────

    /** Classifies each {@code rawLines} entry as a §3 import candidate or not, groups consecutive
     *  candidates at the same depth (a blank line, a comment-only line, a depth change, or a
     *  non-import statement all break the group -- STYLE_PYTHON3.md §3.2's "any non-import
     *  statement breaks the group" plus this slice's own conservative extension to blank/comment
     *  lines, since the style doc is silent on how a blank line or an attached comment should
     *  move when statements are physically reordered around it; treating them as group boundaries
     *  sidesteps that ambiguity entirely rather than guessing), stable-sorts each group per §3.1/
     *  §3.3, and returns one {@link Replacement} per group covering the whole group's line range,
     *  replacing it with the same lines' own verbatim text (indentation, content, trailing
     *  same-line comment) in the new order. */
    private List<Replacement> applyImportSort(final List<Token> tokens, final List<RawLine> rawLines) {
        final List<Replacement> replacements = new ArrayList<>();
        List<RawLine> group = new ArrayList<>();
        List<PyImport> groupImports = new ArrayList<>();
        int groupDepth = -1;
        for (final RawLine line : rawLines) {
            final PyImport imp = line.multiPhysicalLine ? null : classifyImport(tokens, line);
            if (imp != null && (group.isEmpty() || line.depth == groupDepth)) {
                group.add(line);
                groupImports.add(imp);
                groupDepth = line.depth;
                continue;
            }
            flushImportGroup(tokens, group, groupImports, replacements);
            group = new ArrayList<>();
            groupImports = new ArrayList<>();
            if (imp != null) {
                group.add(line);
                groupImports.add(imp);
                groupDepth = line.depth;
            } else {
                groupDepth = -1;
            }
        }
        flushImportGroup(tokens, group, groupImports, replacements);
        return replacements;
    }

    private void flushImportGroup(final List<Token> tokens, final List<RawLine> group,
            final List<PyImport> imports, final List<Replacement> replacements) {
        if (group.isEmpty()) {
            return;
        }
        boolean anyChange = false;
        final List<String> lineTexts = new ArrayList<>();
        for (int i = 0; i < group.size(); i++) {
            final RawLine line = group.get(i);
            final PyImport imp = imports.get(i);
            final List<String> sortedUnits = sortedNameUnits(imp);
            if (sortedUnits == null) {
                lineTexts.add(verbatimLineText(tokens, line.start, line.end));
                continue;
            }
            anyChange = true;
            final StringBuilder text = new StringBuilder();
            text.append(verbatimLineText(tokens, line.start, imp.nameListStart));
            for (int u = 0; u < sortedUnits.size(); u++) {
                if (u > 0) {
                    text.append(", ");
                }
                text.append(sortedUnits.get(u));
            }
            text.append(verbatimLineText(tokens, imp.nameListEnd, line.end));
            lineTexts.add(text.toString());
        }
        final List<Integer> order = new ArrayList<>();
        for (int i = 0; i < imports.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparing((Integer idx) -> imports.get(idx)));
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i) != i) {
                anyChange = true;
                break;
            }
        }
        if (!anyChange) {
            return;
        }
        final StringBuilder text = new StringBuilder();
        for (final int idx : order) {
            text.append(lineTexts.get(idx));
        }
        replacements.add(new Replacement(group.get(0).start, group.get(group.size() - 1).end, text.toString()));
    }

    /** Returns {@code imp}'s {@code nameUnitTexts} reordered to match its own sorted {@code names},
     *  or {@code null} if {@code imp} has no name list ({@code Kind.IMPORT}) or its names are
     *  already in sorted order (nothing to rebuild). */
    private List<String> sortedNameUnits(final PyImport imp) {
        if (imp.nameListStart < 0 || imp.names.size() < 2) {
            return null;
        }
        final List<Integer> order = new ArrayList<>();
        for (int i = 0; i < imp.names.size(); i++) {
            order.add(i);
        }
        order.sort(Comparator.comparing((Integer idx) -> imp.names.get(idx)));
        boolean alreadySorted = true;
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i) != i) {
                alreadySorted = false;
                break;
            }
        }
        if (alreadySorted) {
            return null;
        }
        final List<String> out = new ArrayList<>();
        for (final int idx : order) {
            out.add(imp.nameUnitTexts.get(idx));
        }
        return out;
    }

    private String verbatimLineText(final List<Token> tokens, final int start, final int end) {
        final StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            final Token t = tokens.get(i);
            if (t.type != TokenType.INDENT && t.type != TokenType.DEDENT) {
                sb.append(t.text);
            }
        }
        return sb.toString();
    }

    /** Classifies one line as a §3 import candidate: `import dotted[.dotted...][ as alias][, ...]`
     *  or `from [.[.[...]]][dotted] import (name[ as alias][, ...] | *)`. Rejects (returns null)
     *  a parenthesized `from X import (...)` multi-line-capable form entirely -- even a single-
     *  physical-line parenthesized list -- since distinguishing that reliably from this slice's
     *  simpler comma-list parsing isn't worth the risk; deferred alongside true multi-physical-line
     *  continuations, same as §2. */
    private PyImport classifyImport(final List<Token> tokens, final RawLine line) {
        final int kwIdx = nextSignificant(tokens, line.contentStart, line.end);
        if (kwIdx < 0 || tokens.get(kwIdx).type != TokenType.KEYWORD) {
            return null;
        }
        final Token kw = tokens.get(kwIdx);
        if (isKeyword(kw, "import")) {
            final int nameIdx = nextSignificant(tokens, kwIdx + 1, line.end);
            final String moduleName = readDottedName(tokens, nameIdx, line.end);
            if (moduleName == null || moduleName.isEmpty()) {
                return null;
            }
            int after = advancePastDottedName(tokens, nameIdx, line.end);
            if (after >= 0 && after < line.end && isKeyword(tokens.get(after), "as")) {
                after = nextSignificant(tokens, after + 1, line.end);
                if (after < 0 || tokens.get(after).type != TokenType.IDENTIFIER) {
                    return null;
                }
                after = nextSignificant(tokens, after + 1, line.end);
            }
            if (after >= 0 && after < line.end && tokens.get(after).type == TokenType.PUNCT
                    && ",".equals(tokens.get(after).text)) {
                return null; // multi-module `import a, b` on one line -- deferred, see method javadoc
            }
            return new PyImport(PyImport.Kind.IMPORT, moduleName, new ArrayList<>());
        }
        if (isKeyword(kw, "from")) {
            int p = nextSignificant(tokens, kwIdx + 1, line.end);
            final StringBuilder module = new StringBuilder();
            while (p >= 0 && p < line.end && (tokens.get(p).type == TokenType.OP && ".".equals(tokens.get(p).text))) {
                module.append('.');
                p = nextSignificant(tokens, p + 1, line.end);
            }
            if (p >= 0 && p < line.end && tokens.get(p).type == TokenType.IDENTIFIER) {
                final String rest = readDottedName(tokens, p, line.end);
                if (rest == null) {
                    return null;
                }
                module.append(rest);
                p = advancePastDottedName(tokens, p, line.end);
            }
            if (module.length() == 0) {
                return null;
            }
            final int importKwIdx = nextSignificant(tokens, p, line.end);
            if (importKwIdx < 0 || !isKeyword(tokens.get(importKwIdx), "import")) {
                return null;
            }
            int q = nextSignificant(tokens, importKwIdx + 1, line.end);
            if (q < 0) {
                return null;
            }
            if (tokens.get(q).type == TokenType.PUNCT && "(".equals(tokens.get(q).text)) {
                return null; // parenthesized import list -- deferred, see method javadoc
            }
            if (tokens.get(q).type == TokenType.OP && "*".equals(tokens.get(q).text)) {
                final List<String> names = new ArrayList<>();
                names.add("*");
                return new PyImport(PyImport.Kind.FROM, module.toString(), names);
            }
            final List<String> names = new ArrayList<>();
            final List<String> unitTexts = new ArrayList<>();
            final int nameListStart = q;
            int lastUnitEnd = q;
            while (q >= 0 && q < line.end && tokens.get(q).type == TokenType.IDENTIFIER) {
                final int unitStart = q;
                names.add(tokens.get(q).text);
                int unitEnd = q + 1; // index right after the name (or alias) itself, no trailing gap
                int after = nextSignificant(tokens, q + 1, line.end);
                if (after >= 0 && after < line.end && isKeyword(tokens.get(after), "as")) {
                    after = nextSignificant(tokens, after + 1, line.end);
                    if (after < 0 || tokens.get(after).type != TokenType.IDENTIFIER) {
                        return null;
                    }
                    unitEnd = after + 1;
                    after = nextSignificant(tokens, after + 1, line.end);
                }
                lastUnitEnd = unitEnd;
                unitTexts.add(verbatimLineText(tokens, unitStart, unitEnd));
                if (after >= 0 && after < line.end && tokens.get(after).type == TokenType.PUNCT
                        && ",".equals(tokens.get(after).text)) {
                    q = nextSignificant(tokens, after + 1, line.end);
                    continue;
                }
                q = after;
                break;
            }
            if (names.isEmpty()) {
                return null;
            }
            final boolean isFuture = "__future__".equals(module.toString());
            return new PyImport(isFuture ? PyImport.Kind.FUTURE : PyImport.Kind.FROM, module.toString(), names,
                    nameListStart, lastUnitEnd, unitTexts);
        }
        return null;
    }

    /** Reads a dotted/comma-list-leading name run (`a.b.c`) starting at {@code from}, stopping at
     *  the first token that isn't an {@code IDENTIFIER}/{@code .} continuation -- used both for a
     *  plain `import a.b.c` module name and for a `from`-clause's non-relative module tail. Returns
     *  null if {@code from} isn't an {@code IDENTIFIER}. */
    private String readDottedName(final List<Token> tokens, final int from, final int to) {
        if (from < 0 || from >= to || tokens.get(from).type != TokenType.IDENTIFIER) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        int i = from;
        sb.append(tokens.get(i).text);
        i = nextSignificant(tokens, i + 1, to);
        while (i >= 0 && i < to && tokens.get(i).type == TokenType.OP && ".".equals(tokens.get(i).text)) {
            final int nameIdx = nextSignificant(tokens, i + 1, to);
            if (nameIdx < 0 || nameIdx >= to || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
                break;
            }
            sb.append('.').append(tokens.get(nameIdx).text);
            i = nextSignificant(tokens, nameIdx + 1, to);
        }
        return sb.toString();
    }

    private int advancePastDottedName(final List<Token> tokens, final int from, final int to) {
        int i = from;
        int next = nextSignificant(tokens, i + 1, to);
        while (next >= 0 && next < to && tokens.get(next).type == TokenType.OP && ".".equals(tokens.get(next).text)) {
            final int nameIdx = nextSignificant(tokens, next + 1, to);
            if (nameIdx < 0 || nameIdx >= to || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
                return next;
            }
            i = nameIdx;
            next = nextSignificant(tokens, nameIdx + 1, to);
        }
        return next;
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
