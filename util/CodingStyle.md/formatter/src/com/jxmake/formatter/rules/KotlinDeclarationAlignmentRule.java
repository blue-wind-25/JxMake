/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */

package com.jxmake.formatter.rules;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.grid.KotlinModifierPriority;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * STYLE_KOTLIN.md §6: variable/property declaration column alignment. Extends
 * {@link DeclarationAlignmentRule} to reuse its language-agnostic statement-splitting/
 * grouping-break infrastructure ({@code splitStatements}, {@code hasBlankLineBefore},
 * {@code hasCommentBefore}, {@code significantOnly}, {@code renderTokens},
 * {@code findTrailingComment} -- each raised private -> protected in the base class for this
 * purpose, RDD_KEY_103), but not its {@code Declaration} parsing/rendering, which is hard-baked
 * to C/Java's {@code [modifiers] Type name [= init]} token order. Kotlin's
 * {@code [modifiers] val/var name [: type] [= init]} grammar is fundamentally reversed (name
 * before type), so §6 gets its own declaration model, parser, and grid-rendering method here.
 */
public class KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule {

    private final KotlinModifierPriority modifierPriority = new KotlinModifierPriority();
    // RDD_KEY_162: needed so renderAlignedGroup can estimate a row's true rendered column (this
    // class has no scope-depth of its own -- ScopePipeline passes down depth * indentWidth as
    // its best estimate, same "nestDepth * indentWidth" approximation GetterSetterRule's own
    // width pre-check (RDD_KEY_139) already uses). 0 when constructed via the legacy
    // one/two-arg constructors, which disables the new budget check entirely (see
    // renderAlignedGroup's own doc comment).
    private final int indentWidth;

    public KotlinDeclarationAlignmentRule(final Lang lang) {
        super(lang);
        this.indentWidth = 0;
    }

    public KotlinDeclarationAlignmentRule(final Lang lang, final int lineLengthLimit) {
        super(lang, lineLengthLimit);
        this.indentWidth = 0;
    }

    /** RDD_KEY_162: indent-width-aware constructor, used by {@code ScopePipeline} so {@link
     *  #renderAlignedGroup(List, int)} can estimate a group member's true rendered column and
     *  refuse to pad another member wide enough to push it past {@code lineLengthLimit}. */
    public KotlinDeclarationAlignmentRule(final Lang lang, final int indentWidth, final int lineLengthLimit) {
        super(lang, lineLengthLimit);
        this.indentWidth = indentWidth;
    }

    /** One parsed `val`/`var` property or local-variable declaration. */
    public static final class KotlinDecl {
        public final List<Token> modifiers; // includes the val/var keyword itself (shared slot 5)
        public final Token name;
        public final List<Token> typeTokens; // empty if the type is omitted (inferred)
        public final List<Token> initTokens; // empty if there's no initializer
        public final Token trailingComment; // nullable

        KotlinDecl(final List<Token> modifiers, final Token name, final List<Token> typeTokens,
                final List<Token> initTokens, final Token trailingComment) {
            this.modifiers = modifiers;
            this.name = name;
            this.typeTokens = typeTokens;
            this.initTokens = initTokens;
            this.trailingComment = trailingComment;
        }
    }

    /**
     * Splits one scope's direct-content tokens into groups of consecutive `val`/`var`
     * declaration statements -- same grouping-break rule as the base class's
     * {@code groupDeclarations} (STYLE.md §5): a blank line, a standalone leading comment, or
     * any statement that doesn't parse as a `val`/`var` declaration breaks the current group.
     * @deprecated superseded by {@link #groupAlignableDeclarations} (RDD_KEY_107's "own group
     *     stream, never merged" design was revisited and reversed -- see RDD_LOG.md's
     *     RDD_KEY_107 entry) -- kept only because {@link #parseKotlinDeclaration} is still used
     *     standalone by the merged pass; no longer called from {@code ScopePipeline} directly.
     */
    @Deprecated
    public List<List<KotlinDecl>> groupPropertyDeclarations(final List<Token> scopeTokens) {
        final List<List<Token>> statements = splitKotlinStatements(scopeTokens);
        final List<List<KotlinDecl>> groups = new ArrayList<>();
        List<KotlinDecl> current = new ArrayList<>();

        for (final List<Token> stmt : statements) {
            final KotlinDecl decl = parseKotlinDeclaration(stmt);
            if (decl == null) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            final boolean breakBefore = hasBlankLineBefore(stmt) || hasCommentBefore(stmt);
            if (breakBefore && !current.isEmpty()) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(decl);
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    /**
     * The base class's {@code splitStatements} only splits on a top-level `;` or a `}` that
     * closes a brace-initializer -- correct for C/C++/Java, where every statement is
     * `;`-terminated, but Kotlin properties are conventionally newline-terminated with no `;`
     * at all, so reusing it verbatim would merge every property in a scope into one giant
     * "statement". This splits at every top-level (depth 0, outside any `(`/`[`/`{`) NEWLINE
     * that follows at least one significant token since the last split, in addition to `;` --
     * one declaration per line, matching every STYLE_KOTLIN.md §6 worked example. A declaration
     * whose initializer genuinely spans multiple physical lines is not the focus of any
     * worked example; it will end up split mid-expression and {@link #parseKotlinDeclaration}
     * will simply fail to parse it (leftover unconsumed tokens), so that entry is conservatively
     * left out of any alignment group rather than mis-aligned -- never guess past an
     * unrecognized shape, same posture as the rest of this class.
     */
    private List<List<Token>> splitKotlinStatements(final List<Token> scopeTokens) {
        final List<List<Token>> statements = new ArrayList<>();
        List<Token> current = new ArrayList<>();
        int depth = 0;
        boolean sawSignificant = false;

        for (final Token t : scopeTokens) {
            if (isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{")) {
                depth++;
                current.add(t);
                sawSignificant = true;
                continue;
            }
            if (isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}")) {
                depth--;
                current.add(t);
                sawSignificant = true;
                continue;
            }
            if (depth == 0 && isPunct(t, ";")) {
                current.add(t);
                statements.add(current);
                current = new ArrayList<>();
                sawSignificant = false;
                continue;
            }
            if (depth == 0 && t.type == TokenType.NEWLINE && sawSignificant) {
                statements.add(current);
                current = new ArrayList<>();
                current.add(t); // this newline becomes the next statement's leading gap
                sawSignificant = false;
                continue;
            }
            current.add(t);
            if (!isGapToken(t)) {
                sawSignificant = true;
            }
        }
        if (!current.isEmpty()) {
            statements.add(current);
        }
        return statements;
    }

    /** Parses one statement's tokens as `[modifiers] val|var name [: type] [= init]`, or
     *  returns null if it doesn't match this shape -- any other statement (a function call,
     *  control-flow, a function/class declaration, an annotation-prefixed property, etc.)
     *  breaks the group, same conservative "don't guess past an unrecognized shape" posture
     *  used throughout this codebase, rather than the base class's much broader C/Java grammar. */
    private KotlinDecl parseKotlinDeclaration(final List<Token> stmt) {
        final List<Token> sig = significantOnly(stmt);
        if (sig.isEmpty()) {
            return null;
        }

        int i = 0;
        final List<Token> modifiers = new ArrayList<>();
        while (i < sig.size() && isPlainModifier(sig.get(i))) {
            modifiers.add(sig.get(i));
            i++;
        }
        if (i >= sig.size() || !isValOrVar(sig.get(i))) {
            return null;
        }
        modifiers.add(sig.get(i)); // val/var itself -- KotlinModifierPriority's shared slot 5
        i++;

        if (i >= sig.size() || sig.get(i).type != TokenType.IDENTIFIER) {
            return null;
        }
        final Token name = sig.get(i);
        i++;

        List<Token> typeTokens = new ArrayList<>();
        if (i < sig.size() && isOp(sig.get(i), ":")) {
            i++;
            final int typeStart = i;
            while (i < sig.size() && !isOp(sig.get(i), "=")) {
                // A bare `get`/`set` keyword can never legally appear inside a type expression --
                // its presence here means this "declaration" is actually a KotlinGetterSetterRule
                // §8 merged one-liner (`val x : Int get() = 1`, RDD_KEY_133) being re-parsed on a
                // second/idempotency pass. Bail out (never guess) rather than let it get silently
                // swallowed into typeTokens -- that would corrupt this statement's own rendering
                // AND cross-contaminate any sibling declaration's column width if grouped together
                // (found via harness: an adjacent unrelated `var z : Int = 0` got its own type
                // column padded out to match a wrongly-widened `Int get()` sibling).
                if (sig.get(i).type == TokenType.KEYWORD
                        && ("get".equals(sig.get(i).text) || "set".equals(sig.get(i).text))) {
                    return null;
                }
                i++;
            }
            typeTokens = sig.subList(typeStart, i);
        }

        List<Token> initTokens = new ArrayList<>();
        if (i < sig.size() && isOp(sig.get(i), "=")) {
            final Token eqToken = sig.get(i);
            i++;
            initTokens = sig.subList(i, sig.size());
            i = sig.size();
            if (!initTokens.isEmpty() && spansMultipleLines(stmt, eqToken)) {
                // The initializer contains a source-level NEWLINE after `=` -- a multi-line
                // block-expression initializer (`when(...) { ... }`, `if(...) { ... } else { ... }`,
                // a multi-line lambda, etc.), not the short single-line init every §6 worked
                // example shows. `initTokens` above is built from `sig` (significant-only, so its
                // own NEWLINE tokens were already stripped) and gets rendered by
                // `renderPropertyGroup`/`renderKotlinTokens` as one flat joined-with-spaces line --
                // correct for a real one-line init, but silently collapsing a multi-line block
                // expression onto one line here runs *before* (and corrupts the input for) later
                // passes that assume the block's own internal line structure is still intact, e.g.
                // `KotlinSpecificRule.formatWhenExpressions`'s per-branch newline-based body
                // detection. Bail out (leave this declaration out of the group entirely, rendered
                // verbatim) rather than guess how to re-flow an arbitrary block expression --same
                // "never guess past an unrecognized shape" posture as this method's other bailouts.
                return null;
            }
            if (hasCommentAfter(stmt, eqToken)) {
                // An embedded comment inside the initializer expression (e.g. `x?.let { it + 1 }
                // /* nullable */ ?: 0`) would otherwise be silently dropped: initTokens above is
                // built from `sig` (significant-only, so comments are already stripped out of it)
                // and rendering never consults the raw, unfiltered stmt again. Bail out rather
                // than lose the comment -- same "never guess past an unrecognized shape" posture
                // as the multi-line-initializer bailout just above.
                return null;
            }
        }

        if (i != sig.size()) {
            return null; // trailing tokens this parser doesn't understand -- never guess
        }
        return new KotlinDecl(modifiers, name, typeTokens, initTokens, findTrailingComment(stmt));
    }

    /** True iff {@code stmt} (the declaration's raw, unfiltered token span) contains a
     *  {@code COMMENT_LINE}/{@code COMMENT_BLOCK} token anywhere strictly between {@code
     *  afterToken} (found by identity, same approach as {@link #spansMultipleLines}) and {@code
     *  stmt}'s own last significant token -- i.e. an *embedded* comment inside the initializer
     *  expression that a comment-free {@code significantOnly} rebuild would otherwise silently
     *  drop. A trailing end-of-line comment (after the last significant token) is deliberately
     *  excluded -- that one is already carried separately via {@link #findTrailingComment} and
     *  rendered back, so flagging it here would needlessly break grouping for the common case of
     *  a plain `val x = 1 // comment` declaration. */
    private boolean hasCommentAfter(final List<Token> stmt, final Token afterToken) {
        int lastSigIdx = -1;
        for (int k = 0; k < stmt.size(); k++) {
            if (!isGapToken(stmt.get(k))) {
                lastSigIdx = k;
            }
        }
        boolean seen = false;
        for (int k = 0; k < stmt.size(); k++) {
            final Token t = stmt.get(k);
            if (seen && k < lastSigIdx
                    && (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK)) {
                return true;
            }
            if (t == afterToken) {
                seen = true;
            }
        }
        return false;
    }

    /** True iff {@code stmt} (the declaration's raw, unfiltered token span) contains a
     *  {@code NEWLINE} token, anywhere after {@code afterToken} (found by identity, since
     *  {@code stmt} and the caller's filtered {@code sig}/{@code initTokens} share the same
     *  {@code Token} objects), that is either (a) inside a `{`...`}` brace body (brace depth > 0
     *  relative to {@code afterToken}) -- a genuine multi-line block/lambda body
     *  (`Thread({ ...multi-line... }, "name")`, `if(...) { ... } else { ... }`) that must never
     *  be flattened onto one line by this group's rendering -- or (b) at paren/bracket depth 0
     *  *and* brace depth 0 -- a top-level newline with nothing to explain it away. A newline that
     *  is neither -- i.e. sitting strictly inside a call's parens with no enclosing brace, depth
     *  tracking mirroring {@code ScopePipeline.hasTopLevelNewline}'s own "ignore newlines inside
     *  a call's parens" idiom -- is treated as single-line for grouping purposes: it only means a
     *  brace-free initializer's own nested call argument list was wrapped across lines by a
     *  previous run of {@code MiscRule.enforceCallLineBreaking} (e.g. `if(cond)
     *  full.substring(\n    0, N\n) + "..."  else full`, braceless, one logical expression).
     *  Without the paren-depth carve-out, such an initializer bailed out of its alignment group
     *  only on a *second* format pass (intact on a fresh input, since the wrapping newlines don't
     *  exist yet when this check first runs there) -- an idempotency bug where a sibling
     *  declaration's own column padding silently shrank between passes. Without the brace-depth
     *  override on top of it, a `{`-bodied trailing-lambda argument's *own* internal
     *  statement-separating newlines would also be swallowed as "inside a call's parens" --
     *  caught only via `make test` regressing the RDD_KEY_134 fixture (`real_code_regressions_17`)
     *  after adding the paren-depth carve-out alone, not reasoned out in advance; a single-line
     *  lambda (`x?.let { it + 1 } ?: 0`, `kt_combined_out.kt`) has no newline inside its braces at
     *  all and so is correctly unaffected by either carve-out. */
    private boolean spansMultipleLines(final List<Token> stmt, final Token afterToken) {
        boolean seen = false;
        int parenDepth = 0;
        int braceDepth = 0;
        for (final Token t : stmt) {
            if (seen) {
                if (isPunct(t, "(") || isPunct(t, "[")) {
                    parenDepth++;
                } else if (isPunct(t, ")") || isPunct(t, "]")) {
                    parenDepth--;
                } else if (isPunct(t, "{")) {
                    braceDepth++;
                } else if (isPunct(t, "}")) {
                    braceDepth--;
                } else if (t.type == TokenType.NEWLINE && (braceDepth > 0 || (parenDepth == 0 && braceDepth == 0))) {
                    return true;
                }
            }
            if (t == afterToken) {
                seen = true;
            }
        }
        return false;
    }

    private boolean isPlainModifier(final Token t) {
        return modifierPriority.isModifier(t.text) && !isValOrVar(t);
    }

    private boolean isValOrVar(final Token t) {
        return "val".equals(t.text) || "var".equals(t.text);
    }

    /**
     * Renders one group of consecutive {@link KotlinDecl}s into a STYLE_KOTLIN.md §6 column
     * grid: {@code [visibility] [modality/override/const/lateinit] [val|var] name [: type]
     * [= init]}, each its own {@link ColumnGrid} column so alignment (including the `:` column
     * lining up detached from the variable name) falls out of the grid automatically -- unlike
     * the base class's C/C++ {@code render()}, which must manually pre-pad a combined name+init
     * cell because bitfield/structured-binding shapes there force name and init to share one
     * cell; Kotlin's simpler grammar doesn't need that. A modifier column, or the type/init
     * column, is only emitted at all if some declaration in the group actually uses it -- same
     * "only emit active columns" precedent as the base class's {@code modifierActive} array.
     */
    public List<String> renderPropertyGroup(final List<KotlinDecl> group) {
        final int modifierColumns = modifierPriority.columnCount();
        final boolean[] modifierActive = new boolean[modifierColumns];
        boolean anyType = false;
        boolean anyInit = false;
        for (final KotlinDecl d : group) {
            for (final Token m : d.modifiers) {
                final int rank = modifierPriority.priorityOf(m.text);
                if (rank >= 0) {
                    modifierActive[rank] = true;
                }
            }
            anyType = anyType || !d.typeTokens.isEmpty();
            anyInit = anyInit || !d.initTokens.isEmpty();
        }

        final ColumnGrid grid = new ColumnGrid();
        for (final KotlinDecl d : group) {
            final List<String> cells = new ArrayList<>();
            final String[] modCells = new String[modifierColumns];
            Arrays.fill(modCells, "");
            for (final Token m : d.modifiers) {
                final int rank = modifierPriority.priorityOf(m.text);
                if (rank >= 0) {
                    modCells[rank] = m.text;
                }
            }
            for (int r = 0; r < modifierColumns; r++) {
                if (modifierActive[r]) {
                    cells.add(modCells[r]);
                }
            }
            cells.add(d.name.text);
            if (anyType) {
                cells.add(d.typeTokens.isEmpty() ? "" : ": " + renderKotlinTokens(d.typeTokens));
            }
            if (anyInit) {
                cells.add(d.initTokens.isEmpty() ? "" : "= " + renderKotlinTokens(d.initTokens));
            }
            if (d.trailingComment != null) {
                cells.add(d.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }

        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(trimTrailingSpaces(String.join(" ", row)));
        }
        return lines;
    }

    /**
     * {@code renderTokens} is inherited from the C/C++/Java base class, whose tight-token
     * table (`isTightToken`) has no notion of Kotlin's `?.`/`!!` null-safety operators
     * (STYLE_KOTLIN.md §5, {@code KotlinSpecificRule.enforceNullSafetyOperatorSpacing}) --
     * that pass operates on the whole-file token stream separately and isn't wired into this
     * grid-cell renderer, so `renderTokens` leaves its generic default (a single space) before
     * `?.`/`!!`. This strips exactly that one leftover space so the two passes agree.
     */
    private String renderKotlinTokens(final List<Token> tokens) {
        return renderTokens(tokens)
                .replaceAll("\\s+(?=(\\?\\.|!!))", "")
                .replaceAll("(?<=(\\?\\.|!!))\\s+", "");
    }

    /**
     * Overrides the base class's {@code renderTokens} to fix a real bug found via real-code
     * testing against `square/okio`: the base implementation's {@code needsSpaceBetween} is a
     * strictly pairwise (prev, cur) check with no notion of unary-vs-binary `-`/`+` -- it
     * unconditionally inserted a space between a leading unary minus/plus and its operand
     * (`val prefixIndex = -1` rendered as `= - 1`), since it has no way to see the token before
     * `prev` to tell unary from binary. Only Kotlin's declaration-initializer rendering path
     * (via {@link #renderKotlinTokens}) hits this from-scratch token-list re-render; C/C++/Java's
     * own initializer rendering goes through {@code renderInitTokens} instead, which is
     * untouched. Overriding here (rather than editing the shared base method) keeps this fix
     * fully Kotlin-scoped with zero risk to C/C++/Java behavior.
     */
    @Override
    protected String renderTokens(final List<Token> tokens) {
        final StringBuilder sb = new StringBuilder();
        Token prev = null;
        for (int i = 0; i < tokens.size(); i++) {
            final Token t = tokens.get(i);
            if (prev != null && !isUnaryMinusOperand(tokens, i) && needsSpaceBetween(prev, t)) {
                sb.append(' ');
            }
            sb.append(t.text);
            prev = t;
        }
        return sb.toString();
    }

    /** True iff {@code tokens.get(index)} is the operand immediately following a unary
     *  `-`/`+` at {@code tokens.get(index - 1)} -- i.e. that `-`/`+` is not itself preceded by
     *  another operand (identifier/number/closing `)`/`]`), which would make it binary instead. */
    private boolean isUnaryMinusOperand(final List<Token> tokens, final int index) {
        if (index == 0) {
            return false;
        }
        final Token prevTok = tokens.get(index - 1);
        if (!(isOp(prevTok, "-") || isOp(prevTok, "+"))) {
            return false;
        }
        if (index - 2 < 0) {
            return true; // nothing before the sign -- must be unary
        }
        final Token beforeSign = tokens.get(index - 2);
        return !(beforeSign.type == TokenType.IDENTIFIER || beforeSign.type == TokenType.NUMBER
                || isPunct(beforeSign, ")") || isPunct(beforeSign, "]"));
    }

    private String trimTrailingSpaces(final String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == ' ') {
            end--;
        }
        return s.substring(0, end);
    }

    // ── §12 Destructuring declarations ──────────────────────────────────────────
    /** One component of a destructuring declaration's parenthesized name list (`a` or `id:
     *  Long`); {@code typeTokens} is empty if the component has no explicit type (the common
     *  case -- STYLE_KOTLIN.md §12's own examples never type-annotate a component). The
     *  unnamed placeholder `_` needs no special handling here -- it tokenizes as a plain
     *  IDENTIFIER, per STYLE_KOTLIN.md §12's own "just another identifier for spacing
     *  purposes" framing. */
    public static final class Component {
        public final Token name;
        public final List<Token> typeTokens;

        Component(final Token name, final List<Token> typeTokens) {
            this.name = name;
            this.typeTokens = typeTokens;
        }
    }

    /** One parsed `val`/`var (a, b, ...) = value` destructuring declaration. Unlike {@link
     *  KotlinDecl}, there is no per-component type column to grid-align -- STYLE_KOTLIN.md §12
     *  is explicit that "a destructuring list has no type annotations to anchor a column grid"
     *  -- so this carries a single pre-rendered {@code lhsText} (`(a, b)`, comma-normalized)
     *  rather than a per-component token list, mirroring {@code MiscRule.Assignment}'s own
     *  single-{@code lhsText}-cell shape for the same reason. */
    public static final class DestructuringDecl {
        public final String lhsText; // e.g. "val (a, b)" -- modifiers + val/var + normalized list
        // modifiers (including val/var itself, RDD_KEY_103's shared slot 5) and nameText (just the
        // "(a, b)" component list, no modifiers prefix) exposed separately -- alongside the
        // pre-joined lhsText above -- so RDD_KEY_107's merged alignment pass (see
        // groupAlignableDeclarations/Row below) can grid-align this row's val/var column and name
        // column against an adjacent plain KotlinDecl's own val/var and name columns, the same way
        // C++ structured-bindings (`auto [b, c] = ...`) align against a plain `int a = ...` in the
        // same group.
        public final List<Token> modifiers;
        public final String nameText; // e.g. "(a, b)" -- no modifiers prefix, unlike lhsText
        public final List<Token> initTokens;
        public final Token trailingComment;
        // The statement's own first token (first plain modifier, or the val/var keyword itself if
        // there is none) -- exposed so a caller (ScopePipeline) can locate this declaration's own
        // token-index span in the original stream for splice-back, since lhsText is already a
        // pre-rendered string disconnected from token indices.
        public final Token startToken;

        DestructuringDecl(final String lhsText, final List<Token> modifiers, final String nameText,
                final List<Token> initTokens, final Token trailingComment, final Token startToken) {
            this.lhsText = lhsText;
            this.modifiers = modifiers;
            this.nameText = nameText;
            this.initTokens = initTokens;
            this.trailingComment = trailingComment;
            this.startToken = startToken;
        }
    }

    /**
     * Splits one scope's direct-content tokens into groups of consecutive destructuring
     * declaration statements -- same grouping-break rule as {@link #groupPropertyDeclarations}
     * (STYLE_KOTLIN.md §12: "same as §6, unless there's an outlier breaking the group"): a
     * blank line, a standalone leading comment, or any statement that doesn't parse as a
     * destructuring declaration breaks the current group.
     * @deprecated superseded by {@link #groupAlignableDeclarations} (RDD_KEY_107 revision) --
     *     kept only because {@link #parseDestructuringDeclaration} is still used standalone by
     *     the merged pass; no longer called from {@code ScopePipeline} directly.
     */
    @Deprecated
    public List<List<DestructuringDecl>> groupDestructuringDeclarations(final List<Token> scopeTokens) {
        final List<List<Token>> statements = splitKotlinStatements(scopeTokens);
        final List<List<DestructuringDecl>> groups = new ArrayList<>();
        List<DestructuringDecl> current = new ArrayList<>();

        for (final List<Token> stmt : statements) {
            final DestructuringDecl decl = parseDestructuringDeclaration(stmt);
            if (decl == null) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            final boolean breakBefore = hasBlankLineBefore(stmt) || hasCommentBefore(stmt);
            if (breakBefore && !current.isEmpty()) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(decl);
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    /**
     * Parses one statement's tokens as {@code [modifiers] val|var ( comp (, comp)* ) = value},
     * or returns null if it doesn't match this shape -- in particular, a plain single-name
     * declaration (§6's own shape) correctly falls through to null here (no `(` right after
     * `val`/`var`), so it never gets absorbed into a destructuring group by mistake. Modifiers
     * are preserved in source order rather than reordered by {@code KotlinModifierPriority} --
     * unlike {@link #renderPropertyGroup}'s multi-column grid, this renders the whole LHS as one
     * cell (see {@link DestructuringDecl}), and no STYLE_KOTLIN.md §12 worked example shows a
     * modifier-bearing destructuring declaration to justify guessing at a canonical order for
     * one.
     */
    private DestructuringDecl parseDestructuringDeclaration(final List<Token> stmt) {
        final List<Token> sig = significantOnly(stmt);
        if (sig.isEmpty()) {
            return null;
        }

        int i = 0;
        final List<Token> modifiers = new ArrayList<>();
        while (i < sig.size() && isPlainModifier(sig.get(i))) {
            modifiers.add(sig.get(i));
            i++;
        }
        if (i >= sig.size() || !isValOrVar(sig.get(i))) {
            return null;
        }
        modifiers.add(sig.get(i));
        i++;

        if (i >= sig.size() || !isPunct(sig.get(i), "(")) {
            return null;
        }
        i++;

        final List<Component> components = new ArrayList<>();
        while (true) {
            if (i >= sig.size() || sig.get(i).type != TokenType.IDENTIFIER) {
                return null;
            }
            final Token compName = sig.get(i);
            i++;
            List<Token> typeTokens = new ArrayList<>();
            if (i < sig.size() && isOp(sig.get(i), ":")) {
                i++;
                final int typeStart = i;
                while (i < sig.size() && !isPunct(sig.get(i), ",") && !isPunct(sig.get(i), ")")) {
                    i++;
                }
                typeTokens = sig.subList(typeStart, i);
            }
            components.add(new Component(compName, typeTokens));

            if (i >= sig.size()) {
                return null;
            }
            if (isPunct(sig.get(i), ",")) {
                i++;
                continue;
            }
            if (isPunct(sig.get(i), ")")) {
                i++;
                break;
            }
            return null;
        }
        if (components.size() < 2) {
            return null; // a single-component `(a)` has no STYLE_KOTLIN.md §12 worked example
        }

        if (i >= sig.size() || !isOp(sig.get(i), "=")) {
            return null;
        }
        final Token eqToken = sig.get(i);
        i++;
        final List<Token> initTokens = sig.subList(i, sig.size());
        if (initTokens.isEmpty()) {
            return null;
        }
        // Same "never guess/never drop content" bailouts as parseKotlinDeclaration's own §6
        // multi-line-initializer and embedded-comment checks -- initTokens here is likewise built
        // from `sig` (comments and source-level NEWLINEs already stripped), so a multi-line call
        // (e.g. `Pair(\n 1, // first\n 2 // second\n)`) would otherwise get silently squished onto
        // one line, and any embedded comment inside it silently dropped.
        if (spansMultipleLines(stmt, eqToken) || hasCommentAfter(stmt, eqToken)) {
            return null;
        }

        final StringBuilder name = new StringBuilder("(");
        for (int c = 0; c < components.size(); c++) {
            if (c > 0) {
                name.append(", ");
            }
            final Component comp = components.get(c);
            name.append(comp.name.text);
            if (!comp.typeTokens.isEmpty()) {
                name.append(": ").append(renderTokens(comp.typeTokens));
            }
        }
        name.append(')');
        final String nameText = name.toString();
        final String lhs = renderTokens(modifiers) + " " + nameText;

        return new DestructuringDecl(lhs, modifiers, nameText, initTokens, findTrailingComment(stmt),
                modifiers.get(0));
    }

    /**
     * Renders one group of consecutive {@link DestructuringDecl}s as a two-column
     * {@link ColumnGrid} (LHS, {@code "= " + value}, optional trailing comment) -- same shape as
     * {@code MiscRule.render(List<Assignment>)}'s own two-column pad, and for the same reason:
     * STYLE_KOTLIN.md §12 is explicit that a destructuring list has no type annotations to anchor
     * a finer-grained grid, so the whole LHS aligns as a single cell. The initializer's own
     * token text (including internal spacing) is reproduced verbatim via {@link #renderTokens},
     * same "don't reformat the RHS" posture as every other declaration/assignment renderer in
     * this codebase.
     * @deprecated superseded by {@link #renderAlignedGroup} (RDD_KEY_107 revision) -- kept only
     *     as a standalone fallback renderer, no longer called from {@code ScopePipeline} directly.
     */
    @Deprecated
    public List<String> renderDestructuringGroup(final List<DestructuringDecl> group) {
        final ColumnGrid grid = new ColumnGrid();
        for (final DestructuringDecl d : group) {
            final List<String> cells = new ArrayList<>();
            cells.add(d.lhsText);
            cells.add("= " + renderKotlinTokens(d.initTokens));
            if (d.trailingComment != null) {
                cells.add(d.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }
        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(trimTrailingSpaces(String.join(" ", row)));
        }
        return lines;
    }

    // ── RDD_KEY_107 revision: merged §6/§12 alignment ───────────────────────────────
    /**
     * RDD_KEY_107 originally kept a plain {@code val}/{@code var} declaration (§6) and a
     * destructuring declaration (§12) in two entirely separate group streams, reasoning that a
     * destructuring row's `(a, b)` LHS and a plain declaration's bare name "have no shared column
     * shape to align together." Revisited on explicit user request: C++'s structured bindings
     * already set the opposite precedent in this same codebase --
     * {@code auto [b, c] = somePair;} aligns in the very same group as a preceding
     * {@code int a = xxx;} (its `[b, c]` is just one more name-column cell, no special-casing
     * needed there because the base {@code Declaration} model already treats a bracketed
     * structured-binding list as an ordinary name token sequence). This revision does the Kotlin
     * equivalent: one {@link Row} model that carries either a plain identifier or a pre-rendered
     * `(a, b)` list as its {@code nameCell}, so both shapes share the same {@link ColumnGrid}
     * columns (modifiers, name, optional `:` type, optional `=` init) -- superseding the "own
     * group stream, never merged" half of RDD_KEY_107 (see RDD_LOG.md's RDD_KEY_107 entry
     * for the full revision note). A destructuring row's name column is always its whole
     * `(a, b)` text as one cell (STYLE_KOTLIN.md §12 still has no per-component type grid to
     * anchor a finer split), and its type column is always blank (destructuring declarations
     * have no type-annotation slot of their own to show there).
     */
    public static final class Row {
        public final List<Token> modifiers; // includes val/var itself, shared slot 5
        public final String nameCell; // plain identifier text, or "(a, b)" for a destructuring row
        public final List<Token> typeTokens; // always empty for a destructuring row
        public final List<Token> initTokens;
        public final Token trailingComment;
        public final Token firstAnchor; // modifiers.get(0) -- splice-back start
        public final Token lastAnchor; // splice-back end (inclusive)

        Row(final List<Token> modifiers, final String nameCell, final List<Token> typeTokens,
                final List<Token> initTokens, final Token trailingComment, final Token firstAnchor,
                final Token lastAnchor) {
            this.modifiers = modifiers;
            this.nameCell = nameCell;
            this.typeTokens = typeTokens;
            this.initTokens = initTokens;
            this.trailingComment = trailingComment;
            this.firstAnchor = firstAnchor;
            this.lastAnchor = lastAnchor;
        }
    }

    /** Wraps a parsed {@link KotlinDecl} as a {@link Row} for the merged §6/§12 pass -- same
     *  last-anchor fallback chain (trailing comment, else last init token, else last type token,
     *  else the name itself) {@code ScopePipeline}'s pre-revision two-pass code used to compute
     *  inline for the §6 side. */
    private Row toRow(final KotlinDecl d) {
        final Token lastAnchor = d.trailingComment != null ? d.trailingComment
                : !d.initTokens.isEmpty() ? d.initTokens.get(d.initTokens.size() - 1)
                : !d.typeTokens.isEmpty() ? d.typeTokens.get(d.typeTokens.size() - 1)
                : d.name;
        return new Row(d.modifiers, d.name.text, d.typeTokens, d.initTokens, d.trailingComment,
                d.modifiers.get(0), lastAnchor);
    }

    /** Wraps a parsed {@link DestructuringDecl} as a {@link Row} -- always has a non-empty
     *  {@code initTokens} ({@link #parseDestructuringDeclaration} rejects a destructuring
     *  declaration with no initializer, so no name-token fallback is ever needed here, unlike
     *  {@link #toRow(KotlinDecl)} above). */
    private Row toRow(final DestructuringDecl d) {
        final Token lastAnchor = d.trailingComment != null ? d.trailingComment
                : d.initTokens.get(d.initTokens.size() - 1);
        return new Row(d.modifiers, d.nameText, java.util.Collections.emptyList(), d.initTokens,
                d.trailingComment, d.startToken, lastAnchor);
    }

    /**
     * Splits one scope's direct-content tokens into groups of consecutive `val`/`var`
     * declarations, mixing plain (§6) and destructuring (§12) statements freely in the same
     * group -- the merged replacement for the pre-revision {@link #groupPropertyDeclarations}/
     * {@link #groupDestructuringDeclarations} two-stream split (RDD_KEY_107 revision). Each
     * statement is tried first as a plain declaration, then as a destructuring declaration; the
     * first one that matches wins (a plain declaration's grammar starts with an identifier right
     * after `val`/`var`, a destructuring declaration's with `(`, so the two shapes are mutually
     * exclusive and try-order doesn't matter). Same grouping-break rule as both predecessors: a
     * blank line, a standalone leading comment, or any statement that parses as neither shape
     * breaks the current group.
     */
    public List<List<Row>> groupAlignableDeclarations(final List<Token> scopeTokens) {
        final List<List<Token>> statements = splitKotlinStatements(scopeTokens);
        final List<List<Row>> groups = new ArrayList<>();
        List<Row> current = new ArrayList<>();

        for (final List<Token> stmt : statements) {
            final KotlinDecl plain = parseKotlinDeclaration(stmt);
            Row row = null;
            if (plain != null) {
                row = toRow(plain);
            } else {
                final DestructuringDecl destructuring = parseDestructuringDeclaration(stmt);
                if (destructuring != null) {
                    row = toRow(destructuring);
                }
            }
            if (row == null) {
                if (!current.isEmpty()) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            }
            final boolean breakBefore = hasBlankLineBefore(stmt) || hasCommentBefore(stmt);
            if (breakBefore && !current.isEmpty()) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(row);
        }
        if (!current.isEmpty()) {
            groups.add(current);
        }
        return groups;
    }

    /**
     * Renders one merged group of {@link Row}s into a single STYLE_KOTLIN.md §6/§12 column grid
     * -- same modifier-column model as {@link #renderPropertyGroup}, but {@code nameCell} is
     * already a fully-rendered string (a plain identifier for a §6 row, a whole `(a, b)` list for
     * a §12 row) rather than a {@code Token}, so both shapes share one name column for free. A
     * §12 row's type column is always blank (it has none), same "only emit active columns"
     * precedent as {@link #renderPropertyGroup} -- the type column itself is only emitted at all
     * if some row in the group actually has one.
     */
    public List<String> renderAlignedGroup(final List<Row> group) {
        return renderAlignedGroup(group, 0);
    }

    /**
     * RDD_KEY_162: width-budget-aware entry point, used by {@code ScopePipeline} (which knows
     * the group's own scope depth, unlike this class). {@code indent} is the estimated column
     * this group's lines render at ({@code depth * indentWidth}, the same "nestDepth *
     * indentWidth" approximation {@code GetterSetterRule}'s own width pre-check (RDD_KEY_139)
     * already uses -- an estimate, not the exact final indent {@code ScopePipeline
     * .addKotlinDeclReplacement} later derives from the raw leading gap, but close enough to
     * budget against since Kotlin declarations are never re-indented relative to their own
     * scope).
     *
     * <p>Root cause fixed here (RDD_KEY_161's open question, `ChannelFlow.kt`): naively padding
     * every row in a group out to a shared column width can widen a DIFFERENT row's line enough
     * to push it past {@code lineLengthLimit}, triggering a later line-wrap pass on that row;
     * re-parsing the resulting multi-line initializer on the next pass then correctly (by
     * {@link #parseKotlinDeclaration}'s existing design) bails that row out of alignment
     * entirely, collapsing padding for the group's OTHER rows -- an idempotency flap. Fixed with
     * a fixed-point loop: a row is pulled out of the shared column grid (rendered solo instead,
     * single-space, same as today's post-flap steady state but now stable/idempotent from the
     * very first pass) whenever its group-aligned rendered width overflows the budget AND its
     * initializer is brace-bodied ({@link #hasBraceBodiedInit} -- a lambda/object/block, the
     * exact shape {@link #spansMultipleLines}'s brace-depth check bails on). A row whose
     * initializer contains no brace at all (a plain call/expression, e.g.
     * `test/real_code_regressions_18`'s `display = if (...) full.substring(...) ... `) is never
     * excluded no matter how long it renders -- any future wrap of such an initializer lands
     * strictly inside parens with no enclosing brace, which {@link #spansMultipleLines}'s own
     * paren-depth carve-out (RDD_KEY_135) already keeps groupable/idempotent forever, so
     * excluding it would only needlessly discard a sibling's legitimate column alignment. This is
     * option (a) of the two considered, matching this codebase's established per-member-exclusion
     * pattern ({@code GetterSetterRule}/RDD_KEY_139) rather than inventing a new
     * column-width-capping shape. Excluding a row can shrink the remaining group's own widest
     * column, so the check re-runs against the smaller set until nothing more needs excluding
     * (bounded by group size). A zero-{@code indentWidth} construction (the legacy one/two-arg
     * constructors) disables this check entirely -- {@code indent} is meaningless without a real
     * {@code indentWidth}, and every existing caller of those constructors predates this
     * budget-awareness.
     */
    public List<String> renderAlignedGroup(final List<Row> group, final int indent) {
        if (group.size() <= 1 || indentWidth <= 0) {
            return renderAlignedGroupRaw(group);
        }

        final boolean[] excluded = new boolean[group.size()];
        while (true) {
            final List<Integer> safeIndices = new ArrayList<>();
            for (int i = 0; i < group.size(); i++) {
                if (!excluded[i]) {
                    safeIndices.add(i);
                }
            }
            if (safeIndices.size() <= 1) {
                break; // nothing left to budget-check against
            }
            final List<Row> safeRows = new ArrayList<>();
            for (final int idx : safeIndices) {
                safeRows.add(group.get(idx));
            }
            final List<String> safeLines = renderAlignedGroupRaw(safeRows);

            int overflowAt = -1;
            for (int k = 0; k < safeRows.size(); k++) {
                final Row r = safeRows.get(k);
                if (!hasBraceBodiedInit(r.initTokens)) {
                    continue;
                }
                if (indent + safeLines.get(k).length() > lineLengthLimit) {
                    overflowAt = safeIndices.get(k);
                    break;
                }
            }
            if (overflowAt < 0) {
                break; // fixed point reached -- no remaining row overflows its budget
            }
            excluded[overflowAt] = true;
        }

        final List<String> result = new ArrayList<>(java.util.Collections.nCopies(group.size(), null));
        final List<Integer> safeIndices = new ArrayList<>();
        for (int i = 0; i < group.size(); i++) {
            if (!excluded[i]) {
                safeIndices.add(i);
            }
        }
        final List<Row> safeRows = new ArrayList<>();
        for (final int idx : safeIndices) {
            safeRows.add(group.get(idx));
        }
        final List<String> safeLines = renderAlignedGroupRaw(safeRows);
        for (int k = 0; k < safeIndices.size(); k++) {
            result.set(safeIndices.get(k), safeLines.get(k));
        }
        for (int i = 0; i < group.size(); i++) {
            if (excluded[i]) {
                result.set(i, renderAlignedGroupRaw(java.util.Collections.singletonList(group.get(i))).get(0));
            }
        }
        return result;
    }

    /** True iff {@code initTokens} contains a literal {@code {} -- a lambda/object/block
     *  expression -- anywhere. This, not a generic "has a breakable call" check, is the correct
     *  gate for the width-budget exclusion above: {@link #spansMultipleLines} only ever bails a
     *  re-parsed declaration out of its alignment group over a newline found at brace-depth > 0
     *  (or a genuine top-level one); a later phase wrapping a call's arguments that sit strictly
     *  inside PARENS with no enclosing brace (e.g. `full.substring(0, N)`,
     *  `test/real_code_regressions_18`'s shape, or `threadContextElements(emitContext)` above)
     *  never lands inside a brace, so {@code spansMultipleLines} correctly does NOT bail on it --
     *  that row stays groupable/idempotent forever regardless of how long it is or how much
     *  alignment padding it carries, so it must never be excluded here. A row whose initializer
     *  is itself brace-bodied (a trailing lambda, `ChannelFlow.kt`'s
     *  `emitRef = { downstream.emit(it) }`), by contrast, WILL have any future line-break land
     *  inside that brace, guaranteeing a bail (and the resulting group-wide padding collapse) the
     *  moment it overflows -- so once its own group-aligned width overflows the budget, excluding
     *  it pre-emptively (rendering solo from the very first pass) is what actually restores
     *  idempotency, regardless of whether it was already going to wrap on its own merits (e.g. a
     *  long trailing comment) independent of any alignment padding. */
    private boolean hasBraceBodiedInit(final List<Token> initTokens) {
        for (final Token t : initTokens) {
            if (isPunct(t, "{")) {
                return true;
            }
        }
        return false;
    }

    private List<String> renderAlignedGroupRaw(final List<Row> group) {
        final int modifierColumns = modifierPriority.columnCount();
        final boolean[] modifierActive = new boolean[modifierColumns];
        boolean anyType = false;
        boolean anyInit = false;
        for (final Row r : group) {
            for (final Token m : r.modifiers) {
                final int rank = modifierPriority.priorityOf(m.text);
                if (rank >= 0) {
                    modifierActive[rank] = true;
                }
            }
            anyType = anyType || !r.typeTokens.isEmpty();
            anyInit = anyInit || !r.initTokens.isEmpty();
        }

        final ColumnGrid grid = new ColumnGrid();
        for (final Row r : group) {
            final List<String> cells = new ArrayList<>();
            final String[] modCells = new String[modifierColumns];
            Arrays.fill(modCells, "");
            for (final Token m : r.modifiers) {
                final int rank = modifierPriority.priorityOf(m.text);
                if (rank >= 0) {
                    modCells[rank] = m.text;
                }
            }
            for (int c = 0; c < modifierColumns; c++) {
                if (modifierActive[c]) {
                    cells.add(modCells[c]);
                }
            }
            cells.add(r.nameCell);
            if (anyType) {
                cells.add(r.typeTokens.isEmpty() ? "" : ": " + renderKotlinTokens(r.typeTokens));
            }
            if (anyInit) {
                cells.add(r.initTokens.isEmpty() ? "" : "= " + renderKotlinTokens(r.initTokens));
            }
            if (r.trailingComment != null) {
                cells.add(r.trailingComment.text);
            }
            grid.addRow(cells.toArray(new String[0]));
        }

        final List<String> lines = new ArrayList<>();
        for (final String[] row : grid.flush()) {
            lines.add(trimTrailingSpaces(String.join(" ", row)));
        }
        return lines;
    }

}
