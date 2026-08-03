/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

/**
 * Landing spot for the indent-based language family's (Python3) statement-level rules -- see
 * `STATE_PYTHON3.md`. Currently implements STYLE_PYTHON3.md §2 (Assignment Alignment) and §3
 * (Import Ordering).
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

    /**
     * One recognized `identifier (op) value` assignment candidate line, restricted to a single
     *  bare IDENTIFIER target (same restriction {@link MiscRuleCore#parseAssignment} applies to
     *  the C-family) and to a single physical/logical line -- a multi-line right-hand side (see
     *  STYLE_PYTHON3.md §2's two continuation examples) is explicitly NOT covered by this slice
     *  and never produced by {@link com.jxmake.formatter.ScopePipelineIndent}'s classifier.
     */
    public static final class PyAssignment {

        public final Token       target;
        public final Token       operator;
        public final List<Token> valueTokens;

        public PyAssignment(final Token target, final Token operator, final List<Token> valueTokens)
        {
            this.target      = target;
            this.operator    = operator;
            this.valueTokens = valueTokens;
        }

    } // class PyAssignment

    /**
     * One recognized §6 function-signature parameter, restricted (by {@link
     *  com.jxmake.formatter.ScopePipelineIndent}'s own classifier) to a parameter already written on
     *  its own physical line inside an already one-parameter-per-line {@code def} signature --
     *  this class never itself decides to break/join lines, mirroring {@link PyAssignment}'s own
     *  "given, not decided" posture for §2. {@code typeTokens}/{@code defaultTokens} are empty
     *  (never null) when the parameter has no type hint / no default value, so a bare {@code name}
     *  or {@code name=default} parameter still participates in the group per STYLE_PYTHON3.md §6's
     *  own "padded as if its `:` column were empty" text.
     */
    public static final class PyParam {

        public final List<Token> nameTokens;
        public final List<Token> typeTokens;
        public final List<Token> defaultTokens;
        public final boolean     trailingComma;

        public PyParam(
            final List<Token> nameTokens,
            final List<Token> typeTokens,
            final List<Token> defaultTokens,
            final boolean     trailingComma
        )
        {
            this.nameTokens    = nameTokens;
            this.typeTokens    = typeTokens;
            this.defaultTokens = defaultTokens;
            this.trailingComma = trailingComma;
        }

    } // class PyParam

    /**
     * Public wrapper around the inherited {@link MiscRuleCore#ASSIGNMENT_OPS} -- that field is
     *  `protected`, so callers outside the {@code rules} package (e.g. {@link
     *  com.jxmake.formatter.ScopePipelineIndent}'s line classifier) cannot reference it directly.
     */
    public static boolean isAssignmentOp(final String opText)
    {
        return ASSIGNMENT_OPS.contains(opText);
    }

    /**
     * One recognized §3 import-statement candidate: `import <moduleName>` ({@code IMPORT}, empty
     *  {@code names}) or `from <moduleName> import <names>` ({@code FROM}, or {@code FUTURE} when
     *  {@code moduleName} is exactly {@code "__future__"}). {@code moduleName} is the dotted-name
     *  text verbatim (including any leading `.`/`..` relative-import dots -- ASCII `.` (0x2E)
     *  already sorts before any letter, so no special-casing is needed per STYLE_PYTHON3.md §3.1's
     *  own note). {@code names} holds each imported name's own text in source order (ignoring any
     *  `as alias` -- STYLE_PYTHON3.md §3.1 point 3 sorts by "the first imported name", not its
     *  alias), or a single {@code "*"} for a star-import. Implements {@link Comparable} directly as
     *  §3's full sort key: {@code kind} (FUTURE < IMPORT < FROM, per §3.1 point 1 plus §3.3's
     *  future-import-to-group-top promotion), then {@code moduleName} alphabetically (§3.1 point
     *  2), then {@code names} lexicographically element-by-element (§3.1 point 3).
     */
    public static final class PyImport implements Comparable<PyImport> {

        public enum Kind {

            FUTURE,
            IMPORT,
            FROM

        } // enum Kind

        public final Kind         kind;
        public final String       moduleName;
        public final List<String> names;
        // Only meaningful for FROM: the token-index span of the name list itself (`n1, n2 as a2`,
        // no surrounding parens/keywords), and each name's own verbatim rendered unit (`n` or
        // `n as alias`) in source order, parallel to `names`. Lets the caller rebuild just this
        // span in alphabetized order (STYLE_PYTHON3.md §3.1 point 3's within-clause implication --
        // the worked example's canonical `from os import path, sep` is itself name-sorted) without
        // touching the rest of the line (indentation, `from`/module/`import`, trailing comment).
        public final int          nameListStart;
        public final int          nameListEnd;
        public final List<String> nameUnitTexts;

        public PyImport(final Kind kind, final String moduleName, final List<String> names)
        {
            this(kind, moduleName, names, -1, -1, null);
        }

        public PyImport(
            final Kind         kind,
            final String       moduleName,
            final List<String> names,
            final int          nameListStart,
            final int          nameListEnd,
            final List<String> nameUnitTexts
        )
        {
            this.kind          = kind;
            this.moduleName    = moduleName;
            this.names         = names;
            this.nameListStart = nameListStart;
            this.nameListEnd   = nameListEnd;
            this.nameUnitTexts = nameUnitTexts;
        }

        @Override
        public int compareTo(final PyImport other)
        {
            int c = kind.compareTo(other.kind);
            if(c != 0) return c;
            c = moduleName.compareTo(other.moduleName);
            if(c != 0) return c;
            // §3.1 point 3 sorts by "the first imported name" -- that means the first name
            // *after* within-clause alphabetization (§3.1's own worked example's `from os import
            // path, sep` is itself name-sorted), not the as-parsed source order `names` holds.
            // Comparing on a sorted copy here (rather than `names` itself, which stays in source
            // order for `sortedNameUnits`'s own separate within-clause-rebuild use) means a group
            // with multiple `from X import ...` statements for the same `X` sorts correctly on
            // the very first pass, instead of only after a round-trip re-parses the
            // now-alphabetized rendered text (cpython dogfood, `Lib/random.py`'s four `from math
            // import ...` statements).
            final List<String> sortedNames = new ArrayList<>(names);
            Collections.sort(sortedNames);
            final List<String> otherSortedNames = new ArrayList<>(other.names);
            Collections.sort(otherSortedNames);
            final int n = Math.min( sortedNames.size(), otherSortedNames.size() );
            for(int i = 0; i < n; ++i) {
                c = sortedNames.get(i).compareTo( otherSortedNames.get(i) );
                if(c != 0) return c;
            }

            return Integer.compare( sortedNames.size(), otherSortedNames.size() );
        }

    } // class PyImport

    public MiscRuleIndent(
        final Lang    lang,
        final boolean normalizeCommentStartCase,
        final boolean normalizeCommentEndPeriod,
        final boolean commentNormalizationClassifier,
        final int     indentWidth,
        final int     lineLengthLimit
    )
    {
        super(lang, normalizeCommentStartCase, normalizeCommentEndPeriod, commentNormalizationClassifier,
                indentWidth, lineLengthLimit);
    }

    /**
     * Renders one alignment group's replacement text for each member, in order -- `name (op)=
     *  value`, padded so every `=` in the group lands in the same column, no trailing `;`, no
     *  comment-column alignment (STYLE_PYTHON3.md §2 does not call for aligning trailing comments,
     *  unlike STYLE.md §6's C-family {@code render}, which uses {@link
     *  com.jxmake.formatter.grid.ColumnGrid} for that -- omitted here since nothing in the spec
     *  requires it and no worked example shows it).
     */
    public List<String> renderPyGroup(final List<PyAssignment> group)
    {
        int maxNameLen   = 0;
        int maxPrefixLen = 0;
        for(final PyAssignment a : group) {
            maxNameLen   = Math.max( maxNameLen, a.target.text.length() );
            maxPrefixLen = Math.max( maxPrefixLen, assignOpPrefix(a.operator).length() );
        }
        // +1 unconditionally -- even a bare `=` (empty prefix) still needs its own leading space
        // before the `=` itself, same precedent as MiscRuleCore#render (RDD_KEY reasoning there:
        // maxPrefixLen=2 from `>>=` still renders a 3-wide gap, i.e. naturalMax+1, not naturalMax).
        ++maxPrefixLen;
        final List<String> out = new ArrayList<>();
        for(final PyAssignment a : group) {
            final String lhs = padRight(
                a.target.text, maxNameLen
            ) + padLeft(
                assignOpPrefix(a.operator), maxPrefixLen
            ) + "=";
            out.add( lhs + " " + joinVerbatim(a.valueTokens) );
        } // for

        return out;
    }

    /**
     * Renders one broken-out §6 signature group's replacement text for each parameter, in order.
     *  Alignment target is the {@code :} column (name padded to the group's widest name, same "pad
     *  to widest, one gap before the marker" shape {@link #renderPyGroup} uses for {@code =}) and,
     *  independently, the {@code =} column for whichever parameters have BOTH a type hint and a
     *  default -- {@code maxTypeLenForDefault} is computed only over that subset, so a parameter's
     *  own type text is never padded unless its own default needs the padding to align its `=`; a
     *  typed-but-defaultless parameter (e.g. {@code y : "List[int]"} in STYLE_PYTHON3.md §6's own
     *  worked example) renders its type text unpadded, avoiding trailing whitespace before its
     *  comma. A parameter with no type hint at all skips the `:`-column segment entirely (STYLE_
     *  PYTHON3.md §6: "padded as if its `:` column were empty" -- read literally as "omitted", not
     *  as a fake blank-padded `:` -- so its own `=`, if present, is not forced to align with typed
     *  rows' `=` column; this is the documented, expected shape of a partial row in this grid, same
     *  posture as any other sparse alignment grid elsewhere in this codebase).
     */
    public List<String> renderPySignatureGroup(final List<PyParam> group)
    {
        int maxNameLen = 0;
        for(final PyParam p : group) maxNameLen = Math.max(
            maxNameLen, joinVerbatim(p.nameTokens).length()
        );
        int maxTypeLenForDefault = 0;
        for(final PyParam p : group) {
            if( !p.typeTokens.isEmpty() && !p.defaultTokens.isEmpty() ) maxTypeLenForDefault = Math.max(
                maxTypeLenForDefault, joinVerbatim(p.typeTokens).length()
            );
        }
        final List<String> out = new ArrayList<>();
        for(final PyParam p : group) {
            final StringBuilder sb = new StringBuilder();
            sb.append( padRight( joinVerbatim(p.nameTokens), maxNameLen ) );
            if( !p.typeTokens.isEmpty() ) {
                final String typeText = joinVerbatim(p.typeTokens);
                sb.append(
                    " : "
                ).append(
                    p.defaultTokens.isEmpty() ? typeText : padRight(typeText, maxTypeLenForDefault)
                );
            } // if
            if( !p.defaultTokens.isEmpty() ) sb.append(
                " = "
            ).append(
                joinVerbatim(p.defaultTokens)
            );
            if(p.trailingComma) sb.append(",");
            out.add( sb.toString() );
        } // for

        return out;
    }

    /**
     * Python analog of {@link MiscRuleCore#convertIndentation} -- converts every line's leading
     *  indentation run to the requested `spaces`/`tabs` style, per STYLE.md §1 / STATE_PYTHON3.md's
     *  "Open Questions" entry this resolves. Deliberately NOT a per-line raw-width-ratio rescale
     *  like the C-family version: since Python's indentation is itself the only block-structure
     *  signal (no braces to independently re-derive depth from), rewriting purely off *measured*
     *  width per line risks changing block membership if a line's own width is irregular relative
     *  to {@link #indentWidth} (a naive `width / indentWidth` guess could round two genuinely
     *  different depths to the same target width, silently merging blocks).
     *
     * <p>Instead this rebuilds each real (non-blank, non-comment-only) statement line's indentation
     *  from the depth already established by {@link
     *  com.jxmake.formatter.tokenizer.TokenizerIndent#synthesizeIndentation}'s own INDENT/DEDENT
     *  synthesis -- the same mechanism Python's own grammar uses to decide block membership from
     *  the source being formatted, already proven internally consistent by the fact that the file
     *  tokenized at all. A running `depth` counter is incremented/decremented by each INDENT/DEDENT
     *  marker actually encountered; the target text is always exactly `depth * indentWidth` (or
     *  `depth` tabs), so two statement lines can never collapse to the same rendered width unless
     *  they were already at the same depth. Blank/comment-only lines are deliberately left
     *  untouched (never rewritten) -- they carry no INDENT/DEDENT of their own, so their "depth" is
     *  ambiguous (a comment is free to sit at whatever column the author chose, independent of the
     *  depth carried over from the preceding real statement; found via real-code testing against
     *  this job's own `test/py_comments_inp.py` fixture, a `match`/`case`-adjacent comment
     *  deliberately dedented to visually group with a following, shallower block). This granularity
     *  question -- whole-file uniform target vs. per-block drift-tracking -- was resolved as
     *  "neither, in the C-family sense": real-code evidence (`psf/black`/`django/django`/
     *  `python/cpython` checkouts, 2026-08-04) found zero in-code (non-string-literal) indentation
     *  drift in any of the three -- the only tab-indented lines found anywhere (3 files in
     *  `psf/black`) were entirely inside already-opaque triple-quoted docstrings (§10,
     *  RDD_KEY_186), never touched by this or any other pass. Depth-based rewriting sidesteps the
     *  need to choose a block-boundary granularity at all: it operates per physical line using
     *  structure Python's own grammar already resolved, not a heuristic re-guess of it.
     *
     * <p>A line with no leading {@code WHITESPACE} token (column-0 statements) is left alone --
     *  there is nothing to rewrite. Multi-physical-line statements (bracket/backslash
     *  continuation): only the first physical line's own leading indentation is touched; interior
     *  continuation lines' whitespace is not itself indentation (Python's grammar assigns it no
     *  block-structure meaning inside an open bracket/continuation) and is left untouched, same
     *  "documented gap, not a guess" posture used throughout this job. A frozen {@code WHITESPACE}
     *  token (inside an already-opaque span) is never rewritten.
     */
    public String convertIndentation(final List<Token> tokens, final String indentStyle)
    {
        if( !"spaces".equals(
            indentStyle
        ) && !"tabs".equals(
            indentStyle
        ) ) throw new IllegalArgumentException(
            "convertIndentation only handles spaces|tabs, got: " + indentStyle
        );
        final StringBuilder out         = new StringBuilder();
        final int            n           = tokens.size();
              int            i           = 0;
              int            depth       = 0;
              boolean        atLineStart = true;

        while(i < n) {
            if(atLineStart) {
                int wsIdx = -1;
                if( tokens.get(i).type == TokenType.WHITESPACE ) {
                    wsIdx = i;
                    ++i;
                }
                // Blank/comment-only lines never get INDENT/DEDENT synthesized (TokenizerIndent
                // #synthesizeIndentation's own "blankOrComment" check) precisely because they carry
                // no grammatical depth of their own -- a comment is free to sit at whatever column
                // the author chose (e.g. dedented early to visually group with a following, shallower
                // block), independent of the depth carried over from the preceding real statement.
                // Rewriting such a line to the carried-over `depth`'s canonical WIDTH would silently
                // override that authorial choice (found via real-code testing: `test/py_comments_inp.py`'s
                // `match`/`case`-adjacent comments). So depth-based reconstruction below only ever
                // applies to a real (non-blank, non-comment-only) statement line -- exactly the same
                // lines TokenizerIndent itself treats as depth-affecting. A blank/comment-only line's
                // own WIDTH is still safe to re-express in the target style, though (it changes no
                // meaning at all, unlike its depth) -- done via the width-preserving {@link
                // #renderIndent} (same primitive {@link MiscRuleCore#convertIndentation} uses),
                // keeping the whole file visually consistent in the target style.
                final boolean blankOrComment = i >= n || tokens.get(i).type == TokenType.NEWLINE
                        || tokens.get(i).type == TokenType.COMMENT_LINE;
                while( i < n && ( tokens.get(i).type == TokenType.INDENT
                        || tokens.get(i).type == TokenType.DEDENT ) ) {
                    if(tokens.get(i).type == TokenType.INDENT) ++depth;
                    else                                       --depth;
                    ++i;
                }
                if(wsIdx < 0) {
                    // nothing to rewrite
                }
                else if( tokens.get(wsIdx).frozen ) out.append( tokens.get(wsIdx).text );
                else if(blankOrComment) out.append( renderIndent( tokens.get(wsIdx).text, indentStyle ) );
                else out.append( indentText(depth, indentStyle) );
                atLineStart = false;
                continue;
            } // if
            final Token t = tokens.get(i);
            // A trailing INDENT/DEDENT run synthesized at EOF (closing every still-open block once
            // the source ends) lands here rather than under the `atLineStart` branch above, since
            // the preceding real content line never ended in a NEWLINE (no trailing newline at EOF)
            // or ended one that's still `insideBrackets`. Its own `text` field is a literal width
            // number for informational use (see TokenizerIndent#synthesizeIndentation's javadoc),
            // never source text to render -- appending it unconditionally here corrupted output
            // (found via real-code testing: `psf/black`'s `tests/data/cases/comments3.py`/
            // `annotations.py`, whose last line has no trailing newline, appended a stray digit).
            if(t.type != TokenType.INDENT && t.type != TokenType.DEDENT) out.append(t.text);
            if(t.type == TokenType.NEWLINE) {
                // Mirrors TokenizerIndent#synthesizeIndentation's own insideBrackets/backslash
                // check -- an interior continuation line of a multi-physical-line statement
                // (bracket or backslash continuation) never starts a new logical line, so its own
                // leading whitespace is not indentation at all (see this method's own javadoc's
                // "documented gap" note) and must not be treated as line-start here either; doing
                // so would erase an earlier pass's own cosmetic continuation-indent rendering
                // (found via real-code testing: `real_code_regressions_79/116/138_inp.py`'s
                // multi-line signatures/union-type wraps).
                final boolean insideBrackets = t.parenDepth > 0;
                final boolean backslash      = i > 0 && tokens.get(i - 1).type == TokenType.OP
                        && "\\".equals( tokens.get(i - 1).text );
                atLineStart = !insideBrackets && !backslash;
            } // if
            ++i;
        } // while

        return out.toString();
    }

} // class MiscRuleIndent
