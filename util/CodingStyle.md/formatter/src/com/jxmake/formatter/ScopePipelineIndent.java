/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final PythonBracketComplexityEvaluator bracketEval = new PythonBracketComplexityEvaluator();
    private final int lineLength;

    public ScopePipelineIndent(final Lang lang, final int indentWidth) {
        this(lang, indentWidth, Config.DEFAULT_LINE_LENGTH);
    }

    public ScopePipelineIndent(final Lang lang, final int indentWidth, final int lineLength) {
        super(indentWidth);
        this.lang = lang;
        this.miscRule = new MiscRuleIndent(lang, false, false, false, indentWidth, 0);
        this.lineLength = lineLength;
    }

    @Override
    public String process(final String source) {
        final List<Token> tokens = new TokenizerIndent(lang).tokenize(source);
        final List<RawLine> rawLines = splitRawLines(tokens);
        final List<Replacement> replacements = new ArrayList<>();
        replacements.addAll(applyAssignmentAlignment(tokens, rawLines));
        replacements.addAll(applyImportSort(tokens, rawLines));
        replacements.addAll(applyDecoratorSpacing(tokens, rawLines));
        replacements.addAll(applyFStringSpacing(tokens));
        replacements.addAll(applySignatureAlignment(tokens, rawLines));
        replacements.addAll(applyCaseColonAlignment(tokens, rawLines));
        replacements.addAll(applySingleStatementBody(tokens, rawLines));
        replacements.addAll(applyControlFlowBlankLines(tokens, rawLines));
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

    // ── §4: Decorators ───────────────────────────────────────────────────────────────

    /** STYLE_PYTHON3.md §4: a decorator line's own `@` binds tight to whatever follows it (any
     *  whitespace between them is removed), and every `(`/`[`/`{` pair anywhere in the decorator's
     *  own expression (the call's argument list, plus any bracket nested inside it, e.g. a
     *  `methods=[...]` kwarg) gets its immediate delimiter gap normalized per §1's tight/loose
     *  test ({@link PythonBracketComplexityEvaluator#isLooseParen}/{@code isLooseBracket}/
     *  {@code isLooseBrace}) -- one space just inside the opener/closer when loose, none when
     *  tight, same delimiter-only-padding convention {@code MiscRuleCore#enforceComplexityPadding}
     *  uses for the C-family (comma/operator spacing inside the content is deliberately left
     *  untouched, not this pass's concern, same division of responsibility as that method).
     *  Multi-physical-line decorators (a wrapped call spanning a bracket continuation) are left
     *  completely untouched -- same "documented gap, not a guess" precedent as §2/§3, and
     *  consistent with STATE_PYTHON3.md's note that call-overflow line-wrapping has no existing
     *  mechanism anywhere in this codebase to build on yet. */
    private List<Replacement> applyDecoratorSpacing(final List<Token> tokens, final List<RawLine> rawLines) {
        final List<Replacement> replacements = new ArrayList<>();
        for (final RawLine line : rawLines) {
            if (line.multiPhysicalLine) {
                continue;
            }
            final int atIdx = nextSignificant(tokens, line.contentStart, line.end);
            if (atIdx < 0 || tokens.get(atIdx).type != TokenType.OP || !"@".equals(tokens.get(atIdx).text)) {
                continue;
            }
            final int nameIdx = nextSignificant(tokens, atIdx + 1, line.end);
            if (nameIdx < 0) {
                continue;
            }
            final Replacement tight = normalizeGap(tokens, atIdx + 1, nameIdx, "");
            if (tight != null) {
                replacements.add(tight);
            }
            applyBracketPadding(tokens, nameIdx, line.end, replacements);
        }
        return replacements;
    }

    /** Recursively normalizes the immediate delimiter gap of every `(`/`[`/`{` pair found in
     *  {@code [from, to)} (a decorator's own expression range) -- applies at every nesting level,
     *  not just the outermost call, mirroring {@code enforceComplexityPadding}'s uniform-depth
     *  posture. */
    private void applyBracketPadding(final List<Token> tokens, final int from, final int to,
            final List<Replacement> out) {
        int i = from;
        while (i < to) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.PUNCT && isOpenBracketText(t.text)) {
                final int close = matchBracket(tokens, i, to);
                if (close < 0) {
                    i++;
                    continue;
                }
                final int contentFirst = nextSignificant(tokens, i + 1, close);
                if (contentFirst >= 0) {
                    final boolean loose = classifyLoose(t.text, tokens, i + 1, close);
                    final String desired = loose ? " " : "";
                    final Replacement openGap = normalizeGap(tokens, i + 1, contentFirst, desired);
                    if (openGap != null) {
                        out.add(openGap);
                    }
                    final int lastSig = prevSignificant(tokens, close - 1, i);
                    final Replacement closeGap = normalizeGap(tokens, lastSig + 1, close, desired);
                    if (closeGap != null) {
                        out.add(closeGap);
                    }
                }
                applyBracketPadding(tokens, i + 1, close, out);
                i = close + 1;
            } else {
                i++;
            }
        }
    }

    private boolean classifyLoose(final String openText, final List<Token> tokens, final int contentStart,
            final int contentEnd) {
        final List<Token> content = tokens.subList(contentStart, contentEnd);
        if ("(".equals(openText)) {
            return bracketEval.isLooseParen(content);
        }
        if ("[".equals(openText)) {
            return bracketEval.isLooseBracket(content);
        }
        return bracketEval.isLooseBrace(content);
    }

    private boolean isOpenBracketText(final String text) {
        return "(".equals(text) || "[".equals(text) || "{".equals(text);
    }

    private boolean isCloseBracketText(final String text) {
        return ")".equals(text) || "]".equals(text) || "}".equals(text);
    }

    /** Finds {@code openIdx}'s matching close bracket within {@code [openIdx, limit)}, tracking
     *  depth across all three bracket kinds jointly (same joint-nesting convention {@link
     *  TokenizerIndent}'s own {@code parenDepth} uses) so a mismatched-kind close never
     *  short-circuits the match. Returns -1 if unmatched within the range (should not happen for
     *  syntactically valid, single-physical-line input). */
    private int matchBracket(final List<Token> tokens, final int openIdx, final int limit) {
        final String open = tokens.get(openIdx).text;
        final String close = "(".equals(open) ? ")" : "[".equals(open) ? "]" : "}";
        int depth = 0;
        for (int j = openIdx; j < limit; j++) {
            final Token t = tokens.get(j);
            if (t.type != TokenType.PUNCT) {
                continue;
            }
            if (isOpenBracketText(t.text)) {
                depth++;
            } else if (")".equals(t.text) || "]".equals(t.text) || "}".equals(t.text)) {
                depth--;
                if (depth == 0) {
                    return close.equals(t.text) ? j : -1;
                }
            }
        }
        return -1;
    }

    /** Scans backward from {@code from} for the nearest non-gap token, stopping strictly before
     *  {@code lowExclusive}. Returns {@code lowExclusive} itself if every token in between is a
     *  gap token (i.e. no significant token found) -- callers only invoke this when {@code
     *  nextSignificant} already confirmed at least one significant token exists in the range, so
     *  that case is unreachable in practice, but the bound keeps this helper safe standalone. */
    private int prevSignificant(final List<Token> tokens, final int from, final int lowExclusive) {
        int i = from;
        while (i > lowExclusive && isGapToken(tokens.get(i))) {
            i--;
        }
        return i;
    }

    /** Returns a {@link Replacement} collapsing the gap {@code [from, to)} to exactly {@code
     *  desired} text ({@code ""} for tight, {@code " "} for loose), or {@code null} if the gap
     *  already renders as {@code desired} or the gap contains a comment/newline (conservative skip
     *  -- same posture as {@code enforceComplexityPadding}'s own comment/NEWLINE exclusion, though
     *  a comment inside a single-physical-line decorator's own delimiter gap should not occur in
     *  practice). {@code from == to} (no existing gap token at all, e.g. a tight `("x")`) is a
     *  valid zero-width insertion point, not a no-op -- unlike {@code from > to}, which cannot
     *  happen for a well-formed range and is guarded against defensively. */
    private Replacement normalizeGap(final List<Token> tokens, final int from, final int to, final String desired) {
        if (from > to) {
            return null;
        }
        for (int i = from; i < to; i++) {
            final TokenType type = tokens.get(i).type;
            if (type == TokenType.COMMENT_LINE || type == TokenType.NEWLINE) {
                return null;
            }
        }
        final String current = from < to ? verbatimLineText(tokens, from, to) : "";
        if (current.equals(desired)) {
            return null;
        }
        return new Replacement(from, to, desired);
    }

    // ── §5: F-Strings ────────────────────────────────────────────────────────────────

    /** STYLE_PYTHON3.md §5: an f-string field's `{...}` braces are tight against the expression
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
     *  argument, an un-recognized-shape line) is unaffected by this interaction and trims normally. */
    private List<Replacement> applyFStringSpacing(final List<Token> tokens) {
        final List<Replacement> replacements = new ArrayList<>();
        int i = 0;
        final int n = tokens.size();
        while (i < n) {
            if (tokens.get(i).type == TokenType.FSTRING_START) {
                i = processFString(tokens, i, replacements);
            } else {
                i++;
            }
        }
        return replacements;
    }

    /** Processes one f-string span starting at {@code startIdx} (its {@code FSTRING_START}
     *  token), dispatching each `{...}` field found at this level to {@link #processField}.
     *  Returns the index right after this f-string's {@code FSTRING_END}. */
    private int processFString(final List<Token> tokens, final int startIdx, final List<Replacement> out) {
        int i = startIdx + 1;
        final int n = tokens.size();
        while (i < n) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.FSTRING_END) {
                return i + 1;
            }
            if (t.type == TokenType.PUNCT && "{".equals(t.text)) {
                i = processField(tokens, i, out);
            } else {
                i++;
            }
        }
        return i;
    }

    /** Processes one `{...}` field, {@code openIdx} pointing at its opening `{`. Tracks a local
     *  `(`/`[`/`{` depth (mirroring {@link
     *  com.jxmake.formatter.tokenizer.TokenizerIndent#emitFStringField}'s own depth counter) so a
     *  nested bracket's own `}` isn't mistaken for this field's closing brace; a nested f-string
     *  found inside the expression (e.g. `f"{f'{a}'}"`) is skipped over atomically via a recursive
     *  {@link #processFString} call -- its own fields get their own independent trim through that
     *  call, and its internal brackets never affect this field's own depth count. {@code exprEnd}
     *  is the index of the first depth-0 `!conversion` OP token, {@code FSTRING_FORMAT_SPEC} token,
     *  or (if neither is present) this field's own closing `}` -- whichever comes first. Returns
     *  the index right after the field's closing `}`. */
    private int processField(final List<Token> tokens, final int openIdx, final List<Replacement> out) {
        int i = openIdx + 1;
        int depth = 0;
        int exprEnd = -1;
        while (true) {
            final Token t = tokens.get(i);
            if (t.type == TokenType.FSTRING_START) {
                i = processFString(tokens, i, out);
                continue;
            }
            if (depth == 0 && t.type == TokenType.PUNCT && "}".equals(t.text)) {
                final boolean directClose = exprEnd < 0;
                if (directClose) {
                    exprEnd = i;
                }
                addBraceTrim(tokens, openIdx, exprEnd, directClose, out);
                return i + 1;
            }
            if (depth == 0 && exprEnd < 0 && t.type == TokenType.OP && isFStringConversion(t.text)) {
                exprEnd = i;
            } else if (depth == 0 && exprEnd < 0 && t.type == TokenType.FSTRING_FORMAT_SPEC) {
                exprEnd = i;
            } else if (t.type == TokenType.PUNCT && isOpenBracketText(t.text)) {
                depth++;
            } else if (t.type == TokenType.PUNCT && isCloseBracketText(t.text)) {
                depth--;
            }
            i++;
        }
    }

    /** A `!r`/`!s`/`!a` conversion OP token, per {@link
     *  com.jxmake.formatter.tokenizer.TokenizerIndent#emitFStringField}'s own emission rule (only
     *  ever a 2-character `!`-prefixed OP token). */
    private boolean isFStringConversion(final String text) {
        return text.length() == 2 && text.charAt(0) == '!';
    }

    /** Trims the gap directly inside a field's opening `{` to zero-width unconditionally (the
     *  expression's own leading whitespace is never significant, STYLE_PYTHON3.md §5). The gap
     *  right before {@code exprEnd} is only trimmed when {@code trimClose} is true -- i.e. when
     *  {@code exprEnd} is itself the field's own closing `}` (no `!conversion`/`:format_spec`
     *  present). When a conversion or format spec IS present, STYLE_PYTHON3.md §5's own worked
     *  example (`f"{value !r}"`, listed as "never touched") keeps any whitespace immediately
     *  before the opaque tail exactly as written -- only the opaque tail's own text ({@code !r}/
     *  {@code :spec}), not the boundary gap leading into it, is what "never touched" refers to in
     *  the brace-tightness sense used here, so this pass does not touch that gap at all. A field
     *  with no significant expression token at all (e.g. a malformed empty `{}`) is left alone --
     *  defensive only, not valid Python. */
    private void addBraceTrim(final List<Token> tokens, final int openIdx, final int exprEnd,
            final boolean trimClose, final List<Replacement> out) {
        final int firstSig = nextSignificant(tokens, openIdx + 1, exprEnd);
        if (firstSig < 0) {
            return;
        }
        final Replacement openGap = normalizeGap(tokens, openIdx + 1, firstSig, "");
        if (openGap != null) {
            out.add(openGap);
        }
        if (!trimClose) {
            return;
        }
        final int lastSig = prevSignificant(tokens, exprEnd - 1, openIdx);
        final Replacement closeGap = normalizeGap(tokens, lastSig + 1, exprEnd, "");
        if (closeGap != null) {
            out.add(closeGap);
        }
    }

    // ── §6: Function Signature Wrapping (alignment-only slice) ─────────────────────────

    /** STYLE_PYTHON3.md §6's inline-vs-one-per-line *decision* has no home in this codebase yet --
     *  same documented gap as §4's own call-argument-overflow wrapping (no general line-length-
     *  triggered breaking mechanism exists anywhere in the {@code *Indent}/{@code *Curly} family;
     *  the C-family's {@code enforceCallLineBreaking} is Curly-only). This pass therefore never
     *  decides to break or join a signature -- it only column-aligns the {@code :}/{@code =} of a
     *  {@code def} signature's parameter list that is <em>already</em> written one-parameter-per-
     *  line in the source, taking that human-authored line-breaking as given (the same posture §2's
     *  assignment alignment takes toward an already-single-line assignment candidate: normalize
     *  spacing given the existing structure, never decide when to (re)break a line). An inline
     *  (already-one-line) signature is untouched by construction -- it is never
     *  {@code multiPhysicalLine}, so it never reaches {@link #trySignatureGroup} at all. */
    private List<Replacement> applySignatureAlignment(final List<Token> tokens, final List<RawLine> rawLines) {
        final List<Replacement> replacements = new ArrayList<>();
        for (final RawLine line : rawLines) {
            if (!line.multiPhysicalLine) {
                continue;
            }
            int kwIdx = nextSignificant(tokens, line.contentStart, line.end);
            if (kwIdx < 0 || tokens.get(kwIdx).type != TokenType.KEYWORD) {
                continue;
            }
            if (isKeyword(tokens.get(kwIdx), "async")) {
                kwIdx = nextSignificant(tokens, kwIdx + 1, line.end);
                if (kwIdx < 0 || tokens.get(kwIdx).type != TokenType.KEYWORD) {
                    continue;
                }
            }
            if (!isKeyword(tokens.get(kwIdx), "def")) {
                continue;
            }
            final int nameIdx = nextSignificant(tokens, kwIdx + 1, line.end);
            if (nameIdx < 0 || tokens.get(nameIdx).type != TokenType.IDENTIFIER) {
                continue;
            }
            final int openIdx = nextSignificant(tokens, nameIdx + 1, line.end);
            if (openIdx < 0 || tokens.get(openIdx).type != TokenType.PUNCT || !"(".equals(tokens.get(openIdx).text)) {
                continue;
            }
            final int closeIdx = matchBracket(tokens, openIdx, line.end);
            if (closeIdx < 0) {
                continue;
            }
            final List<Replacement> group = trySignatureGroup(tokens, openIdx, closeIdx);
            if (group != null) {
                replacements.addAll(group);
            }
        }
        return replacements;
    }

    /** Attempts to classify {@code (openIdx, closeIdx)}'s interior as an already one-parameter-per-
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
     *  describes the already-one-per-line shape being aligned here. */
    private List<Replacement> trySignatureGroup(final List<Token> tokens, final int openIdx, final int closeIdx) {
        final List<int[]> segments = new ArrayList<>(); // [segStart, segEnd) per NEWLINE-delimited segment
        int segStart = openIdx + 1;
        for (int i = openIdx + 1; i < closeIdx; i++) {
            if (tokens.get(i).type == TokenType.NEWLINE) {
                segments.add(new int[] { segStart, i });
                segStart = i + 1;
            }
        }
        segments.add(new int[] { segStart, closeIdx });
        if (segments.size() < 3) {
            return null; // need >=1 param line plus the blank pre-`(`/pre-`)` framing lines
        }
        final int firstSig = nextSignificant(tokens, segments.get(0)[0], segments.get(0)[1]);
        if (firstSig >= 0) {
            return null; // an inline first parameter shares `(`'s own line -- unsupported shape
        }
        final int[] lastSeg = segments.get(segments.size() - 1);
        final int lastSegSig = nextSignificant(tokens, lastSeg[0], lastSeg[1]);
        if (lastSegSig >= 0) {
            return null; // `)` doesn't stand alone on its own line -- unsupported shape
        }
        final List<PyParam> params = new ArrayList<>();
        final List<int[]> spans = new ArrayList<>(); // [contentFirst, contentEndExclusive] per param
        for (int s = 1; s < segments.size() - 1; s++) {
            final int[] seg = segments.get(s);
            final PyParam p = classifySignatureParam(tokens, seg[0], seg[1]);
            if (p == null) {
                return null;
            }
            params.add(p);
            final int contentFirst = nextSignificant(tokens, seg[0], seg[1]);
            final int contentEnd = trimEndIdx(tokens, contentFirst, seg[1]);
            spans.add(new int[] { contentFirst, contentEnd });
        }
        final List<String> rendered = miscRule.renderPySignatureGroup(params);
        final List<Replacement> out = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            out.add(new Replacement(spans.get(i)[0], spans.get(i)[1], rendered.get(i)));
        }
        return out;
    }

    /** Classifies one already-isolated parameter line ({@code [segStart, segEnd)}, a single
     *  {@code NEWLINE}-delimited segment strictly inside a signature's `(`/`)`) into a {@link
     *  PyParam}: {@code name[: type][= default][,]}. The top-level {@code :}/{@code =} search
     *  tracks this segment's own local bracket depth (starting fresh at 0, since the segment is
     *  known to hold exactly one parameter) so a nested-bracket type hint like
     *  {@code List[Dict[str, int]]} never has its own internal {@code :}/{@code =} mistaken for the
     *  parameter's own annotation/default separator -- the same depth-tracking shape {@link
     *  #classifyLoose}/{@link #matchBracket} already use elsewhere in this class, just applied to
     *  {@code :}/{@code =} search instead of bracket matching. Returns {@code null} (segment
     *  rejected, whole signature left untouched by the caller) if the segment contains a comment or
     *  has no name token at all. */
    private PyParam classifySignatureParam(final List<Token> tokens, final int segStart, final int segEnd) {
        for (int k = segStart; k < segEnd; k++) {
            if (tokens.get(k).type == TokenType.COMMENT_LINE) {
                return null;
            }
        }
        final int nameStart = nextSignificant(tokens, segStart, segEnd);
        if (nameStart < 0) {
            return null;
        }
        int lastSig = -1;
        for (int k = segEnd - 1; k >= segStart; k--) {
            if (!isGapToken(tokens.get(k))) {
                lastSig = k;
                break;
            }
        }
        final boolean trailingComma = tokens.get(lastSig).type == TokenType.PUNCT && ",".equals(tokens.get(lastSig).text);
        final int contentEnd = trailingComma ? lastSig : lastSig + 1;
        if (contentEnd <= nameStart) {
            return null;
        }
        int depth = 0;
        int colonIdx = -1;
        int eqIdx = -1;
        for (int k = nameStart; k < contentEnd; k++) {
            final Token t = tokens.get(k);
            if (t.type == TokenType.PUNCT && isOpenBracketText(t.text)) {
                depth++;
            } else if (t.type == TokenType.PUNCT && isCloseBracketText(t.text)) {
                depth--;
            } else if (depth == 0 && colonIdx < 0 && t.type == TokenType.PUNCT && ":".equals(t.text)) {
                colonIdx = k;
            } else if (depth == 0 && eqIdx < 0 && t.type == TokenType.OP && "=".equals(t.text)) {
                eqIdx = k;
            }
        }
        final int nameEnd = colonIdx >= 0 ? colonIdx : (eqIdx >= 0 ? eqIdx : contentEnd);
        final int nameEndTrimmed = trimEndIdx(tokens, nameStart, nameEnd);
        if (nameEndTrimmed <= nameStart) {
            return null;
        }
        final List<Token> nameTokens = tokens.subList(nameStart, nameEndTrimmed);
        List<Token> typeTokens = new ArrayList<>();
        List<Token> defaultTokens = new ArrayList<>();
        if (colonIdx >= 0) {
            final int typeEnd = eqIdx >= 0 ? eqIdx : contentEnd;
            final int typeStart = nextSignificant(tokens, colonIdx + 1, typeEnd);
            if (typeStart >= 0 && typeStart < typeEnd) {
                typeTokens = tokens.subList(typeStart, trimEndIdx(tokens, typeStart, typeEnd));
            }
        }
        if (eqIdx >= 0) {
            final int defStart = nextSignificant(tokens, eqIdx + 1, contentEnd);
            if (defStart >= 0 && defStart < contentEnd) {
                defaultTokens = tokens.subList(defStart, trimEndIdx(tokens, defStart, contentEnd));
            }
        }
        return new PyParam(nameTokens, typeTokens, defaultTokens, trailingComma);
    }

    /** Scans backward from {@code end - 1} for the nearest non-gap token strictly at/after {@code
     *  start}, returning the index right after it (i.e. the exclusive end of the trimmed range), or
     *  {@code start} itself if no significant token is found in {@code [start, end)}. */
    private int trimEndIdx(final List<Token> tokens, final int start, final int end) {
        for (int k = end - 1; k >= start; k--) {
            if (!isGapToken(tokens.get(k))) {
                return k + 1;
            }
        }
        return start;
    }

    // ── §7: Structural Pattern Matching (match/case) ────────────────────────────────────

    /** One recognized {@code case pattern:} header on a single physical line: {@code
     *  [patternStart, patternEnd)} is the pattern's own trimmed token span (including any {@code if}
     *  guard clause -- the guard is part of the case's own header, not the body), {@code colonIdx}
     *  is the header-terminating `:` (found at bracket depth 0 relative to the pattern, so a mapping
     *  pattern's own `{"key": value}` colon -- inside `{}` -- is never mistaken for it), and {@code
     *  compact} is true when at least one significant, non-comment token follows the `:` on the same
     *  physical line (STYLE_PYTHON3.md §7/§8: the body stays same-line vs. drops to an indented
     *  block is read as-already-written, never decided here, same posture as §6's signature-grouping
     *  toward an already-broken-out parameter list). */
    private static final class CaseLine {
        final int patternStart;
        final int patternEnd;
        final int colonIdx;
        final boolean compact;

        CaseLine(final int patternStart, final int patternEnd, final int colonIdx, final boolean compact) {
            this.patternStart = patternStart;
            this.patternEnd = patternEnd;
            this.colonIdx = colonIdx;
            this.compact = compact;
        }
    }

    /** Classifies each {@code rawLines} entry as a §7 {@code case} header or not, groups
     *  contiguous same-depth {@code case} lines (a blank line, a comment-only line, a depth change,
     *  or any non-{@code case} statement all break the group -- same boundary convention §2/§3 use),
     *  and -- **all-or-nothing** per STYLE_PYTHON3.md §7 -- aligns the `:` column across a group only
     *  when every member in it is already in compact one-line form; a group containing even one
     *  block-body {@code case} gets no alignment at all, not even for its own compact members. */
    private List<Replacement> applyCaseColonAlignment(final List<Token> tokens, final List<RawLine> rawLines) {
        final List<Replacement> replacements = new ArrayList<>();
        List<CaseLine> group = new ArrayList<>();
        int groupDepth = -1;
        for (final RawLine line : rawLines) {
            final CaseLine c = classifyCaseLine(tokens, line);
            if (c != null && (group.isEmpty() || line.depth == groupDepth)) {
                group.add(c);
                groupDepth = line.depth;
                continue;
            }
            flushCaseGroup(tokens, group, replacements);
            group = new ArrayList<>();
            if (c != null) {
                group.add(c);
                groupDepth = line.depth;
            } else {
                groupDepth = -1;
            }
        }
        flushCaseGroup(tokens, group, replacements);
        return replacements;
    }

    /** Classifies one line as a §7 {@code case} header: {@code case} is tokenized as a plain {@code
     *  IDENTIFIER} (a context-sensitive soft keyword, per {@code TokenizerIndent}'s own {@code
     *  KEYWORDS_PYTHON} exclusion), so this checks its literal text rather than {@code
     *  TokenType.KEYWORD}. Rejects (returns {@code null}) a multi-physical-line {@code case} header
     *  (a wrapped pattern spanning a bracket/backslash continuation) -- same "documented gap, not a
     *  guess" precedent as every prior §2-§6 slice -- and any line with no top-level `:` at all. */
    private CaseLine classifyCaseLine(final List<Token> tokens, final RawLine line) {
        if (line.multiPhysicalLine) {
            return null;
        }
        final int caseIdx = nextSignificant(tokens, line.contentStart, line.end);
        if (caseIdx < 0 || tokens.get(caseIdx).type != TokenType.IDENTIFIER || !"case".equals(tokens.get(caseIdx).text)) {
            return null;
        }
        final int patternStart = nextSignificant(tokens, caseIdx + 1, line.end);
        if (patternStart < 0) {
            return null;
        }
        int depth = 0;
        int colonIdx = -1;
        for (int k = patternStart; k < line.end; k++) {
            final Token t = tokens.get(k);
            if (t.type == TokenType.PUNCT && isOpenBracketText(t.text)) {
                depth++;
            } else if (t.type == TokenType.PUNCT && isCloseBracketText(t.text)) {
                depth--;
            } else if (depth == 0 && t.type == TokenType.PUNCT && ":".equals(t.text)) {
                colonIdx = k;
                break;
            }
        }
        if (colonIdx < 0) {
            return null;
        }
        final int patternEnd = trimEndIdx(tokens, patternStart, colonIdx);
        if (patternEnd <= patternStart) {
            return null;
        }
        boolean compact = false;
        for (int k = colonIdx + 1; k < line.end; k++) {
            final Token t = tokens.get(k);
            if (!isGapToken(t) && t.type != TokenType.COMMENT_LINE) {
                compact = true;
                break;
            }
        }
        return new CaseLine(patternStart, patternEnd, colonIdx, compact);
    }

    /** Emits one {@link Replacement} per compact group member, padding the gap between its own
     *  trimmed pattern text and its `:` to the group's widest pattern width -- mirrors {@link
     *  #normalizeGap}'s "gap-only, no whole-line rebuild" shape (used for §4/§5's bracket/brace
     *  padding), just with a per-group computed width instead of a fixed tight/loose text. Does
     *  nothing at all (no replacements) when the group is empty or contains any block-body member,
     *  per §7's all-or-nothing rule. */
    private void flushCaseGroup(final List<Token> tokens, final List<CaseLine> group,
            final List<Replacement> replacements) {
        if (group.isEmpty()) {
            return;
        }
        for (final CaseLine c : group) {
            if (!c.compact) {
                return; // all-or-nothing: one block-body case abandons alignment for the whole group
            }
        }
        int maxLen = 0;
        for (final CaseLine c : group) {
            maxLen = Math.max(maxLen, verbatimLineText(tokens, c.patternStart, c.patternEnd).length());
        }
        for (final CaseLine c : group) {
            final int len = verbatimLineText(tokens, c.patternStart, c.patternEnd).length();
            final String desired = padRightSpaces(maxLen - len);
            final Replacement gap = normalizeGap(tokens, c.patternEnd, c.colonIdx, desired);
            if (gap != null) {
                replacements.add(gap);
            }
        }
    }

    private String padRightSpaces(final int count) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    // ── §8: Single-Statement Bodies ─────────────────────────────────────────────────────

    /** Keywords whose already-block-form header ({@code header:} on its own line, body on the next
     *  indented line) is a §8 join candidate: `if`/`elif`/`else`/`while`/`for`. {@code case} is
     *  handled separately (reuses {@link #classifyCaseLine}, since `case` is a context-sensitive
     *  soft keyword tokenized as a plain {@code IDENTIFIER}, not a member of this set) --
     *  `def`/`class`/`try`/`except`/`finally`/`with` are deliberately never members, per
     *  STYLE_PYTHON3.md §8's explicit "never applies" list. */
    private static final Set<String> SINGLE_STMT_HEADER_KEYWORDS =
            new HashSet<>(Arrays.asList("if", "elif", "else", "while", "for"));

    /** STYLE_PYTHON3.md §8: joins a block already written as `header:` followed by exactly one
     *  indented simple-statement line back onto the header's own line (`header: statement`) when
     *  the joined line fits within {@code line-length}; otherwise (or when any qualification below
     *  fails) the block form is left completely untouched -- this pass only ever joins, it never
     *  itself decides to expand an already-compact one-line form back into a block (STYLE_PYTHON3.md
     *  §8 names no such rule, and every already-compact worked example in the style doc is confirmed
     *  left alone by this pass's own "compactAlready" check below).
     *
     *  <p>A header qualifies only when: it is a single physical line (not {@code
     *  multiPhysicalLine}), its first significant token is one of {@link
     *  #SINGLE_STMT_HEADER_KEYWORDS} or the {@code case} soft keyword (via {@link
     *  #classifyCaseLine}), and nothing but an optional trailing comment follows its own
     *  header-terminating `:` on the same physical line (i.e. it is genuinely block-form, not
     *  already compact). The immediately following {@link RawLine} must exist, sit exactly one
     *  depth deeper, not itself be {@code multiPhysicalLine}, not be blank/comment-only, and not
     *  itself open a further nested block (see {@link #bodyOpensNewBlock} -- a nested `if`/`for`/
     *  `while`/`with`/`match`/`def`/`class`/etc. always keeps its own indented block, never
     *  qualifies as the "simple statement" this pass joins). The line after the body (if any) must
     *  sit at a shallower depth than the body -- a sibling line still at the body's own depth means
     *  the block held more than one statement (or a trailing blank/comment line), and the whole
     *  join is skipped, consistent with every prior §2/§3 slice's own conservative "leave the group
     *  boundary alone" posture. A body line carrying its own trailing comment is also skipped
     *  (conservative -- STYLE_PYTHON3.md §8 shows no worked example either way for this shape).
     *
     *  <p><b>Ambiguity resolved conservatively, not guessed:</b> a body statement containing a
     *  `lambda` anywhere (`x = lambda: 1`) has its own top-level `:` that does not open a block, but
     *  distinguishing that from a genuine nested-compound-statement `:` reliably would need
     *  lambda-parameter-list-aware depth tracking this pass does not build -- {@link
     *  #bodyOpensNewBlock} conservatively treats any body containing a `lambda` keyword as "opens a
     *  new block" (i.e. never joined), sidestepping the ambiguity by leaving that narrow shape's
     *  block form untouched rather than risking a wrong join. The same conservative `lambda` check
     *  also guards the header's own condition-scanning colon search ({@link
     *  #classifySingleStmtHeader}), for the same reason. */
    private List<Replacement> applySingleStatementBody(final List<Token> tokens, final List<RawLine> rawLines) {
        final List<Replacement> replacements = new ArrayList<>();
        for (int i = 0; i < rawLines.size(); i++) {
            final RawLine header = rawLines.get(i);
            final int colonIdx = classifySingleStatementHeaderColon(tokens, header);
            if (colonIdx < 0) {
                continue;
            }
            if (i + 1 >= rawLines.size()) {
                continue;
            }
            final RawLine body = rawLines.get(i + 1);
            if (body.multiPhysicalLine || body.depth != header.depth + 1) {
                continue;
            }
            final int bodyContentStart = nextSignificant(tokens, body.contentStart, body.end);
            if (bodyContentStart < 0) {
                continue; // blank line -- not a real statement
            }
            if (tokens.get(bodyContentStart).type == TokenType.COMMENT_LINE
                    || tokens.get(bodyContentStart).type == TokenType.COMMENT_BLOCK) {
                continue; // comment-only line -- not a real statement
            }
            if (i + 2 < rawLines.size() && rawLines.get(i + 2).depth == body.depth) {
                continue; // a sibling line (second statement, or trailing blank/comment) remains
            }
            final int bodyContentEnd = trimEndIdx(tokens, bodyContentStart, body.end);
            if (containsComment(tokens, bodyContentEnd, body.end)) {
                continue; // body carries its own trailing comment -- conservative skip
            }
            if (bodyOpensNewBlock(tokens, bodyContentStart, bodyContentEnd)) {
                continue;
            }
            final String headerText = verbatimLineText(tokens, header.start, colonIdx + 1);
            final String bodyText = verbatimLineText(tokens, bodyContentStart, bodyContentEnd);
            final String joined = headerText + " " + bodyText;
            if (physicalLineLength(joined) > lineLength) {
                continue; // overflow -- leave the indented block form untouched
            }
            replacements.add(new Replacement(header.start, bodyContentEnd, joined));
        }
        return replacements;
    }

    /** Length of {@code joined}'s longest physical line -- {@code joined} is always a single
     *  physical line by construction here (header + one simple statement), so this is just its own
     *  length, but named for clarity against STYLE.md §2's line-length limit. */
    private int physicalLineLength(final String joined) {
        return joined.length();
    }

    /** True iff any token in {@code [from, to)} is a comment. Used to conservatively skip a body
     *  statement carrying its own trailing same-line comment (see {@link
     *  #applySingleStatementBody}'s javadoc). */
    private boolean containsComment(final List<Token> tokens, final int from, final int to) {
        for (int i = from; i < to; i++) {
            final TokenType ty = tokens.get(i).type;
            if (ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) {
                return true;
            }
        }
        return false;
    }

    /** Classifies {@code line} as a §8 block-form header, returning the index of its own
     *  header-terminating `:`, or {@code -1} if it doesn't qualify (already compact, not one of the
     *  qualifying keywords, multi-physical-line, or no depth-0 `:` found). Delegates to {@link
     *  #classifyCaseLine} for the {@code case} soft keyword (reusing §7's own header-colon/compact
     *  detection rather than re-deriving it); handles `if`/`elif`/`else`/`while`/`for` directly. */
    private int classifySingleStatementHeaderColon(final List<Token> tokens, final RawLine line) {
        if (line.multiPhysicalLine) {
            return -1;
        }
        final int kwIdx = nextSignificant(tokens, line.contentStart, line.end);
        if (kwIdx < 0) {
            return -1;
        }
        final Token kw = tokens.get(kwIdx);
        if (kw.type == TokenType.IDENTIFIER && "case".equals(kw.text)) {
            final CaseLine c = classifyCaseLine(tokens, line);
            return c != null && !c.compact ? c.colonIdx : -1;
        }
        if (kw.type != TokenType.KEYWORD || !SINGLE_STMT_HEADER_KEYWORDS.contains(kw.text)) {
            return -1;
        }
        int depth = 0;
        int colonIdx = -1;
        for (int k = kwIdx + 1; k < line.end; k++) {
            final Token t = tokens.get(k);
            if (t.type == TokenType.KEYWORD && "lambda".equals(t.text)) {
                return -1; // ambiguous condition colon vs lambda colon -- conservative bail
            }
            if (t.type == TokenType.PUNCT && isOpenBracketText(t.text)) {
                depth++;
            } else if (t.type == TokenType.PUNCT && isCloseBracketText(t.text)) {
                depth--;
            } else if (depth == 0 && t.type == TokenType.PUNCT && ":".equals(t.text)) {
                colonIdx = k;
                break;
            }
        }
        if (colonIdx < 0) {
            return -1;
        }
        for (int k = colonIdx + 1; k < line.end; k++) {
            final Token t = tokens.get(k);
            if (!isGapToken(t) && t.type != TokenType.COMMENT_LINE) {
                return -1; // already compact (something besides a trailing comment follows `:`)
            }
        }
        return colonIdx;
    }

    /** True iff {@code [contentStart, contentEnd)} (a candidate body statement's own trimmed token
     *  span) itself opens a new nested block: a depth-0 `:` (a nested `if`/`for`/`while`/`with`/
     *  `match`/`def`/`class`/etc. header) or a `lambda` keyword anywhere in the span (conservative
     *  bail on the lambda-colon ambiguity -- see {@link #applySingleStatementBody}'s javadoc).
     *  `:=` is never at risk of being mistaken for either, since {@link
     *  com.jxmake.formatter.tokenizer.TokenizerIndent#emitWalrus} already merges it into a single
     *  pre-tokenized `OP`. */
    private boolean bodyOpensNewBlock(final List<Token> tokens, final int contentStart, final int contentEnd) {
        int depth = 0;
        for (int k = contentStart; k < contentEnd; k++) {
            final Token t = tokens.get(k);
            if (t.type == TokenType.KEYWORD && "lambda".equals(t.text)) {
                return true;
            }
            if (t.type == TokenType.PUNCT && isOpenBracketText(t.text)) {
                depth++;
            } else if (t.type == TokenType.PUNCT && isCloseBracketText(t.text)) {
                depth--;
            } else if (depth == 0 && t.type == TokenType.PUNCT && ":".equals(t.text)) {
                return true;
            }
        }
        return false;
    }

    // ── §9: Control Flow Blank Lines ─────────────────────────────────────────────────────

    /** Tracks one active indentation frame while scanning {@code rawLines} linearly: {@code
     *  isFunctionBody} is true iff this frame was opened by a {@code def}/{@code async def} header
     *  (as opposed to {@code if}/{@code for}/{@code while}/{@code with}/{@code try}/{@code class}/
     *  {@code match}, all of which push a non-function frame); {@code sawContent} becomes true once
     *  at least one real statement has been processed directly inside this frame (mirrors {@code
     *  MiscRuleCurly.FuncFrame}'s own field of the same name and purpose for the C-family). */
    private static final class ControlFlowFrame {
        boolean isFunctionBody;
        boolean sawContent;
    }

    /** STYLE_PYTHON3.md §9: two independent blank-line-insertion rules, both direct ports of an
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
     *  <p><b>Explicitly NOT covered by this slice</b> (documented scope boundaries, not guesses,
     *  matching every prior §2-§8 slice's own precedent): a {@code def} header itself written as a
     *  multi-physical-line (wrapped) parameter list is never recognized as a function-body-opening
     *  header by {@link #isDefHeaderLine} (conservatively returns false for any {@code
     *  multiPhysicalLine} header), silently disabling §9.1 for such a function's own {@code return}
     *  statements -- same posture as §6's own already-documented multi-physical-line-signature gaps.
     *  A semicolon-chained statement (e.g. {@code x = 1; return y} on one physical line) is never
     *  recognized as a return/exit statement by either half of this rule, since only a {@link
     *  RawLine}'s own <em>first</em> significant token is ever inspected, not a semicolon-
     *  delimited sub-statement -- no STYLE_PYTHON3.md worked example exercises semicolon-chaining
     *  anywhere in this job, so this is consistent with every other section's own silence on that
     *  shape. A {@code return}/{@code elif}/{@code else} whose immediately preceding {@link RawLine}
     *  is a comment-only line (no blank line of its own) is conservatively left untouched -- same
     *  "no worked example to guess a relocation from" posture the C-family reference itself uses for
     *  Java/C++ (as opposed to Kotlin's own separately-carved-out comment-relocation behavior, which
     *  has no Python analog here). {@code try}/{@code except}/{@code finally} blank-line placement is
     *  entirely out of scope -- STYLE_PYTHON3.md §9.2's own text names only {@code elif}/{@code else}. */
    private List<Replacement> applyControlFlowBlankLines(final List<Token> tokens, final List<RawLine> rawLines) {
        final List<Replacement> replacements = new ArrayList<>();
        final Deque<ControlFlowFrame> stack = new ArrayDeque<>();
        RawLine lastContentLine = null;

        for (int i = 0; i < rawLines.size(); i++) {
            final RawLine line = rawLines.get(i);
            final int sig = nextSignificant(tokens, line.contentStart, line.end);
            if (sig < 0) {
                continue; // blank line -- doesn't affect frames or content tracking
            }
            final Token first = tokens.get(sig);
            if (first.type == TokenType.COMMENT_LINE || first.type == TokenType.COMMENT_BLOCK) {
                continue; // comment-only line -- doesn't affect frames or content tracking
            }

            while (stack.size() > line.depth) {
                stack.pop();
            }
            while (stack.size() < line.depth) {
                final ControlFlowFrame f = new ControlFlowFrame();
                f.isFunctionBody = stack.size() == line.depth - 1 && lastContentLine != null
                        && isDefHeaderLine(tokens, lastContentLine);
                stack.push(f);
            }
            final ControlFlowFrame top = stack.peek();

            if (top != null && top.isFunctionBody && top.sawContent
                    && first.type == TokenType.KEYWORD && "return".equals(first.text)) {
                final Replacement r = insertBlankLineBefore(tokens, rawLines, i, line);
                if (r != null) {
                    replacements.add(r);
                }
            }

            if (first.type == TokenType.KEYWORD && ("elif".equals(first.text) || "else".equals(first.text))) {
                final RawLine prevStmt = previousContentLine(tokens, rawLines, i);
                if (prevStmt != null && isUnconditionalExitLine(tokens, prevStmt)) {
                    final Replacement r = insertBlankLineBefore(tokens, rawLines, i, line);
                    if (r != null) {
                        replacements.add(r);
                    }
                }
            }

            if (top != null) {
                top.sawContent = true;
            }
            lastContentLine = line;
        }
        return replacements;
    }

    /** True iff {@code line}'s first significant token is {@code def} (optionally preceded by {@code
     *  async}) -- i.e. it opens a function-body frame. A {@code multiPhysicalLine} header (a wrapped
     *  parameter list) conservatively returns false -- documented gap, see {@link
     *  #applyControlFlowBlankLines}'s javadoc. */
    private boolean isDefHeaderLine(final List<Token> tokens, final RawLine line) {
        if (line.multiPhysicalLine) {
            return false;
        }
        int idx = nextSignificant(tokens, line.contentStart, line.end);
        if (idx < 0) {
            return false;
        }
        Token t = tokens.get(idx);
        if (t.type == TokenType.KEYWORD && "async".equals(t.text)) {
            idx = nextSignificant(tokens, idx + 1, line.end);
            if (idx < 0) {
                return false;
            }
            t = tokens.get(idx);
        }
        return t.type == TokenType.KEYWORD && "def".equals(t.text);
    }

    /** Walks backward from {@code idx - 1}, skipping blank and comment-only lines, and returns the
     *  nearest genuine-content {@link RawLine}, or {@code null} if none exists before {@code idx}. */
    private RawLine previousContentLine(final List<Token> tokens, final List<RawLine> rawLines, final int idx) {
        for (int k = idx - 1; k >= 0; k--) {
            final RawLine candidate = rawLines.get(k);
            final int sig = nextSignificant(tokens, candidate.contentStart, candidate.end);
            if (sig < 0) {
                continue;
            }
            final TokenType ty = tokens.get(sig).type;
            if (ty == TokenType.COMMENT_LINE || ty == TokenType.COMMENT_BLOCK) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    /** True iff {@code line}'s first significant token is exactly {@code return}/{@code break}/
     *  {@code continue} -- deliberately NOT {@code raise}, matching the C-family reference list's own
     *  existing omission (STYLE_PYTHON3.md §9.2). */
    private boolean isUnconditionalExitLine(final List<Token> tokens, final RawLine line) {
        final int sig = nextSignificant(tokens, line.contentStart, line.end);
        if (sig < 0) {
            return false;
        }
        final Token t = tokens.get(sig);
        return t.type == TokenType.KEYWORD
                && ("return".equals(t.text) || "break".equals(t.text) || "continue".equals(t.text));
    }

    /** Returns a zero-width {@link Replacement} inserting one blank line (a bare {@code "\n"})
     *  immediately before {@code line}'s own leading indentation, or {@code null} if a blank line is
     *  already present there (idempotent -- matches the C-family reference's own "already-blank gap
     *  left untouched" posture) or if the immediately preceding {@link RawLine} is a comment-only
     *  line (conservative skip, see {@link #applyControlFlowBlankLines}'s javadoc). {@code idx == 0}
     *  (no preceding line at all) also returns {@code null} -- nothing to separate a blank line from. */
    private Replacement insertBlankLineBefore(final List<Token> tokens, final List<RawLine> rawLines,
            final int idx, final RawLine line) {
        if (idx == 0) {
            return null;
        }
        final RawLine prevRaw = rawLines.get(idx - 1);
        final int prevSig = nextSignificant(tokens, prevRaw.contentStart, prevRaw.end);
        if (prevSig < 0) {
            return null; // already blank
        }
        final TokenType prevTy = tokens.get(prevSig).type;
        if (prevTy == TokenType.COMMENT_LINE || prevTy == TokenType.COMMENT_BLOCK) {
            return null; // comment directly adjacent -- conservative skip
        }
        return new Replacement(line.start, line.start, "\n");
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
