/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * Landing spot for the indent-based language family's (Python3) statement-level rules -- see
 * `STATE_PYTHON3.md`. Currently implements STYLE_PYTHON3.md §2 (Assignment Alignment) only.
 *
 * <p>Deliberately does not reuse {@link MiscRuleCore#groupAssignments}/{@code render} verbatim --
 * those are `;`/`{}`-shaped (C-family statement termination) and always append a literal `;`.
 * Python has no statement terminator to split on (NEWLINE/INDENT/DEDENT-shaped instead, see
 * {@link com.jxmake.formatter.ScopePipelineIndent#applyAssignmentAlignment}), and its rendered
 * value is never `;`-suffixed. The padding/column primitives ({@link #padRight}, {@link #padLeft},
 * {@link #assignOpPrefix}, {@link #joinVerbatim}, {@link #ASSIGNMENT_OPS}) are still inherited
 * from {@link MiscRuleCore} and reused as-is -- those are statement-shape-agnostic.
 */
public final class MiscRuleIndent extends MiscRuleCore {

    /** One recognized `identifier (op) value` assignment candidate line, restricted to a single
     *  bare IDENTIFIER target (same restriction {@link MiscRuleCore#parseAssignment} applies to
     *  the C-family) and to a single physical/logical line -- a multi-line right-hand side (see
     *  STYLE_PYTHON3.md §2's two continuation examples) is explicitly NOT covered by this slice
     *  and never produced by {@link com.jxmake.formatter.ScopePipelineIndent}'s classifier. */
    public static final class PyAssignment {
        public final Token target;
        public final Token operator;
        public final List<Token> valueTokens;

        public PyAssignment(final Token target, final Token operator, final List<Token> valueTokens) {
            this.target = target;
            this.operator = operator;
            this.valueTokens = valueTokens;
        }
    }

    /** Public wrapper around the inherited {@link MiscRuleCore#ASSIGNMENT_OPS} -- that field is
     *  `protected`, so callers outside the {@code rules} package (e.g. {@link
     *  com.jxmake.formatter.ScopePipelineIndent}'s line classifier) cannot reference it directly. */
    public static boolean isAssignmentOp(final String opText) {
        return ASSIGNMENT_OPS.contains(opText);
    }

    public MiscRuleIndent(final Lang lang, final boolean normalizeCommentStartCase,
            final boolean normalizeCommentEndPeriod, final boolean commentNormalizationClassifier,
            final int indentWidth, final int lineLengthLimit) {
        super(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, commentNormalizationClassifier,
                indentWidth, lineLengthLimit);
    }

    /** Renders one alignment group's replacement text for each member, in order -- `name (op)=
     *  value`, padded so every `=` in the group lands in the same column, no trailing `;`, no
     *  comment-column alignment (STYLE_PYTHON3.md §2 does not call for aligning trailing comments,
     *  unlike STYLE.md §6's C-family {@code render}, which uses {@link
     *  com.jxmake.formatter.grid.ColumnGrid} for that -- omitted here since nothing in the spec
     *  requires it and no worked example shows it). */
    public List<String> renderPyGroup(final List<PyAssignment> group) {
        int maxNameLen = 0;
        int maxPrefixLen = 0;
        for (final PyAssignment a : group) {
            maxNameLen = Math.max(maxNameLen, a.target.text.length());
            maxPrefixLen = Math.max(maxPrefixLen, assignOpPrefix(a.operator).length());
        }
        // +1 unconditionally -- even a bare `=` (empty prefix) still needs its own leading space
        // before the `=` itself, same precedent as MiscRuleCore#render (RDD_KEY reasoning there:
        // maxPrefixLen=2 from `>>=` still renders a 3-wide gap, i.e. naturalMax+1, not naturalMax).
        maxPrefixLen++;
        final List<String> out = new ArrayList<>();
        for (final PyAssignment a : group) {
            final String lhs = padRight(a.target.text, maxNameLen)
                    + padLeft(assignOpPrefix(a.operator), maxPrefixLen) + "=";
            out.add(lhs + " " + joinVerbatim(a.valueTokens));
        }
        return out;
    }
}
