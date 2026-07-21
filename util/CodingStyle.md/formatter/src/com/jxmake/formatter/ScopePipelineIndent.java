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
    private final PythonBracketComplexityEvaluator bracketEval = new PythonBracketComplexityEvaluator();

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
        replacements.addAll(applyDecoratorSpacing(tokens, rawLines));
        replacements.addAll(applyFStringSpacing(tokens));
        replacements.addAll(applySignatureAlignment(tokens, rawLines));
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
