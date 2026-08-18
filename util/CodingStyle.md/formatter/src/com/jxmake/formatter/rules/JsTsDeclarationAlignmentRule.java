/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.List;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

/**
 * STYLE_JS_TS.md §11 declaration-alignment-grid support for `let`/`const`/`var` declarations.
 * Extends {@link DeclarationAlignmentRuleCurly} to reuse its language-agnostic statement-
 * splitting/grouping-break infrastructure ({@code splitStatements}, {@code hasBlankLineBefore},
 * {@code hasCommentBefore}, {@code significantOnly}, {@code renderTokens}, {@code
 * findTrailingComment}, RDD_KEY_103's precedent), but not its {@code Declaration} parsing/
 * rendering, which is hard-baked to C/Java's {@code [modifiers] Type name [= init]} token order
 * -- JS/TS's `let x: Type = value` is name-before-type, the same reversed grammar Kotlin's
 * {@code val x: Type = value} already solved (RDD_KEY_103/104), so this class mirrors {@link
 * KotlinDeclarationAlignmentRule}'s structural shape rather than extending the base class's own
 * parser (the base's {@code parseDeclaration} is {@code private} and its index arithmetic
 * unconditionally takes the *last* token before size/params as the name -- no seam to inject a
 * reversed-grammar branch, same finding Kotlin's own class doc already recorded).
 *
 * <p>Unlike Kotlin, JS/TS statements are always `;`-terminated (STYLE_JS_TS.md §2 -- ASI is
 * never relied upon), so this class reuses the base class's own {@code splitStatements} directly
 * instead of Kotlin's newline-based {@code splitKotlinStatements}. JS/TS also has no modifier
 * table for local `let`/`const`/`var` declarations (unlike Kotlin's `KotlinModifierPriority` or
 * TS's own separate class-field modifier table, §11.2, already handled by {@code
 * JsTsSpecificRule.reorderClassFieldModifiers} as a flat pass, not this class), so the rendered
 * grid here has no modifier columns at all -- just keyword, name, optional `:` type (TS only),
 * optional `=` init, optional trailing comment.
 *
 * <p><b>Deliberately out of scope this checkpoint (see STATE_JS_TS.md for the full note):</b>
 * destructuring-pattern LHS (`const { a, b } = obj;`, `const [x, y] = arr;`, RDD_KEY_182) and
 * multi-declarator statements (`let a = 1, b = 2;`) both bail out of {@link #parseDeclaration}
 * (return null) rather than being parsed -- they are left completely untouched by this class, the
 * same as before this checkpoint, so the pre-existing Checkpoint 5/6 destructuring-space/bogus-
 * semicolon fixes (which operate independently, via {@code MiscRuleCore.needsSpaceBetween}/
 * {@code JsTsSpecificRule.classifyBraces}, never through this class) are not at risk of
 * regression. `type X = ...` alias groups (RDD_KEY_183) are a separate future extension too --
 * `type` is not recognized as a declaration keyword here at all.
 */
public class JsTsDeclarationAlignmentRule extends DeclarationAlignmentRuleCurly {

    public JsTsDeclarationAlignmentRule(final Lang lang, final int lineLengthLimit)
    {
        super(lang, lineLengthLimit);
    }

    /** One parsed `let`/`const`/`var`/`type` declaration */
    public static final class Row {

        public final Token keyword; // let/const/var/type itself
        public final Token name;    // Nameless-pattern rows (destructuring) still carry the LHS's
                                  // First token here, purely as a group-breaking/anchor reference
                                  // -- rendering uses `nameText` instead, see below
        /**
         * Rendered LHS text for this row's name column. For a plain identifier declarator this
         *  is just {@code name.text}; for a destructuring-pattern LHS (RDD_KEY_182) this is the
         *  whole `{...}`/`[...]` pattern rendered as one unit via {@code renderTokens} (never
         *  re-split token by token -- the pattern's own internal spacing is already correct from
         *  earlier bracket-padding/comma-spacing passes, this method only decides the `=` column
         *  alignment).
         */
        public final String      nameText;
        public final List<Token> typeTokens; // TS only; always empty for plain JS
        public final List<Token> initTokens; // Empty if there's no initializer (or, for a
                                              // `type X = ...` row, the alias's type expression)
        public final Token trailingComment; // Nullable
        public final Token lastAnchor;      // Splice-back end (inclusive)
        /**
         * True for a `type X = ...;` alias row (RDD_KEY_183) -- such rows only group with other
         *  type-alias rows, never with a plain `let`/`const`/`var` declarator, see {@link
         *  #groupAlignableDeclarations}.
         */
        public final boolean isTypeAlias;

        Row(
            final Token       keyword,
            final Token       name,
            final String      nameText,
            final List<Token> typeTokens,
            final List<Token> initTokens,
            final Token       trailingComment,
            final Token       lastAnchor,
            final boolean     isTypeAlias
        )
        {
            this.keyword         = keyword;
            this.name            = name;
            this.nameText        = nameText;
            this.typeTokens      = typeTokens;
            this.initTokens      = initTokens;
            this.trailingComment = trailingComment;
            this.lastAnchor      = lastAnchor;
            this.isTypeAlias     = isTypeAlias;
        }

    } // class Row

    private boolean isDeclKeyword(final Token t)
    {
        return t.type == TokenType.KEYWORD
                && ( "let".equals(t.text) || "const".equals(t.text) || "var".equals(t.text) );
    }

    /**
     * `type` is only a declaration-alignment keyword here when immediately followed by an
     *  identifier and then `=` (a type-alias statement, RDD_KEY_183) -- `type` is not a reserved
     *  word in JS/TS, so this must not fire on an identifier merely named `type`
     */
    private boolean isTypeAliasKeyword(final List<Token> sig)
    {
        if( sig.isEmpty() || !"type".equals(
            sig.get(0).text
        ) || ( sig.get(
            0
        ).type != TokenType.IDENTIFIER && sig.get(
            0
        ).type != TokenType.KEYWORD ) ) return false;
        if( sig.size() < 3 || sig.get(1).type != TokenType.IDENTIFIER ) return false;
        // Skip an optional generic parameter list (`type Foo<T> = ...`) before requiring `=`.
        int i = 2;
        if( i < sig.size() && isOp( sig.get(i), "<" ) ) {
            int depth = 0;
            while( i < sig.size() ) {
                if( isOp( sig.get(i), "<" ) ) {
                    ++depth;
                }
                else if( isOp( sig.get(i), ">" ) ) {
                    --depth;
                    ++i;
                    if(depth == 0) break;
                    continue;
                }
                ++i;
            } // while
        } // if

        return i < sig.size() && isOp( sig.get(i), "=" );
    }

    /**
     * Parses one statement's tokens as {@code let|const|var name [: type] [= init] ;}, a
     * destructuring-pattern declarator (RDD_KEY_182), or a {@code type X = ...;} alias
     * (RDD_KEY_183). Returns null if it doesn't match any of those shapes -- any other statement
     * (multi-declarator, a function call, control-flow, a class/function declaration, etc.)
     * breaks the group, same conservative "don't guess past an unrecognized shape" posture as
     * {@code KotlinDeclarationAlignmentRule.parseKotlinDeclaration}.
     */
    private Row parseDeclaration(final List<Token> stmt)
    {
        final List<Token> sig = significantOnly(stmt);
        if( sig.isEmpty() ) return null;
        if( isTypeAliasKeyword(sig) ) return parseTypeAlias(stmt, sig);
        if( !isDeclKeyword( sig.get(0) ) ) return null;
        final Token keyword = sig.get(0);
              int   i       = 1;

        if( i < sig.size() && ( isPunct(
            sig.get(i), "{"
        ) || isPunct(
            sig.get(i), "["
        ) ) ) return parseDestructuringDeclaration(
            stmt, sig, keyword, i
        );

        // Only a plain single identifier declarator is handled here
        if( i >= sig.size() || sig.get(i).type != TokenType.IDENTIFIER ) return null;
        final Token name = sig.get(i);
        ++i;

        List<Token> typeTokens = new ArrayList<>();
        if( lang.isTs && i < sig.size() && isOp( sig.get(i), ":" ) ) {
            ++i;
            final int typeStart = i;
                  int depth     = 0;
            while( i < sig.size() ) {
                final Token t = sig.get(i);
                     if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) depth++;
                else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) depth--;
                else if( depth == 0 && ( isOp(
                    t, "="
                ) || isPunct(
                    t, ";"
                ) || isPunct(
                    t, ","
                ) ) ) break;
                ++i;
            } // while
            if( i < sig.size() && isPunct(
                sig.get(i), ","
            ) ) return null; // Multi-declarator with a type -- not this checkpoint's scope
            typeTokens = sig.subList(typeStart, i);
        } // if

        List<Token> initTokens = new ArrayList<>();
        if( i < sig.size() && isOp( sig.get(i), "=" ) ) {
            final Token eqToken = sig.get(i);
            ++i;
            final int initStart = i;
                  int depth     = 0;
            while( i < sig.size() ) {
                final Token t = sig.get(i);
                     if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) depth++;
                else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) depth--;
                else if( depth == 0 && ( isPunct(t, ";") || isPunct(t, ",") ) ) break;
                ++i;
            } // while
            if( i < sig.size() && isPunct(
                sig.get(i), ","
            ) ) return null; // Multi-declarator -- not this checkpoint's scope
            initTokens = sig.subList(initStart, i);
            if( !initTokens.isEmpty() && spansMultipleLines(stmt, eqToken) ) return null;
            if( hasCommentAfter(stmt, eqToken) ) return null;
        } // if

        if( i >= sig.size() || !isPunct(
            sig.get(i), ";"
        ) ) return null; // Not the plain `;`-terminated shape this parser understands
        final Token semi = sig.get(i);
        ++i;
        if( i != sig.size() ) return null; // Trailing tokens this parser doesn't understand -- never guess

        final Token trailingComment = findTrailingComment(stmt);
        final Token lastAnchor      = trailingComment != null ? trailingComment : semi;

        return new Row(
            keyword, name, name.text, typeTokens, initTokens, trailingComment, lastAnchor, false
        );
    }

    /**
     * Parses {@code let|const|var {pattern} [: type] = init;} / {@code let|const|var [pattern]
     * [: type] = init;} -- a destructuring-pattern declarator (RDD_KEY_182). {@code patternStart}
     * indexes the opening `{`/`[` in {@code sig}. The whole bracketed pattern span is captured
     * verbatim and rendered as one unit via {@code renderTokens} for the name column -- never
     * re-split token by token, its own internal spacing already comes from the earlier
     * bracket-padding/comma-spacing passes (§3). A destructuring LHS always requires an
     * initializer (`const {a}` with no `= ...` isn't valid JS), so unlike the plain-identifier
     * path this method requires `=` rather than treating it as optional.
     */
    private Row parseDestructuringDeclaration(
        final List<Token> stmt,
        final List<Token> sig,
        final Token       keyword,
        final int         patternStart
    )
    {
        final String open  = sig.get(patternStart).text;
        final String close = "{".equals(open) ? "}" : "]";
              int    i     = patternStart;
              int    depth = 0;
        while( i < sig.size() ) {
            final Token t = sig.get(i);
            if( isPunct(t, "{") || isPunct(t, "[") ) {
                ++depth;
            }
            else if( isPunct(t, "}") || isPunct(t, "]") ) {
                --depth;
                if(depth == 0) {
                    ++i;
                    break;
                }
            }
            ++i;
        } // while
        if(depth != 0) return null; // Unbalanced -- never guess
        final List<Token> patternTokens = sig.subList(patternStart, i);
        if( patternTokens.isEmpty() || !close.equals(
            patternTokens.get( patternTokens.size() - 1 ).text
        ) ) return null;
        if( hasCommentWithin(
            stmt, patternTokens.get(0), patternTokens.get( patternTokens.size() - 1 )
        ) ) return null; // SignificantOnly() already stripped comments out of patternTokens -- // check the raw stmt so a comment embedded inside the destructuring // pattern (e.g. `{ id, // note\n name }`) isn't silently dropped by // renderTokens' one-line collapse
        final Token anchorName = patternTokens.get(0);

        List<Token> typeTokens = new ArrayList<>();
        if( lang.isTs && i < sig.size() && isOp( sig.get(i), ":" ) ) {
            ++i;
            final int typeStart = i;
                  int d         = 0;
            while( i < sig.size() ) {
                final Token t = sig.get(i);
                     if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) d++;
                else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) d--;
                else if( d == 0 && ( isOp(t, "=") || isPunct(t, ";") || isPunct(t, ",") ) ) break;
                ++i;
            } // while
            if( i < sig.size() && isPunct(
                sig.get(i), ","
            ) ) return null; // Multi-declarator with a type -- not this checkpoint's scope
            typeTokens = sig.subList(typeStart, i);
        } // if

        if( i >= sig.size() || !isOp(
            sig.get(i), "="
        ) ) return null; // A destructuring declarator with no initializer isn't valid JS/TS
        final Token eqToken = sig.get(i);
        ++i;
        final int initStart = i;
              int d         = 0;
        while( i < sig.size() ) {
            final Token t = sig.get(i);
                 if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) d++;
            else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) d--;
            else if( d == 0 && ( isPunct(t, ";") || isPunct(t, ",") ) ) break;
            ++i;
        } // while
        if( i < sig.size() && isPunct(
            sig.get(i), ","
        ) ) return null; // Multi-declarator -- not this checkpoint's scope
        final List<Token> initTokens = sig.subList(initStart, i);
        if( initTokens.isEmpty() || spansMultipleLines(
            stmt, eqToken
        ) || hasCommentAfter(
            stmt, eqToken
        ) ) return null;

        if( i >= sig.size() || !isPunct( sig.get(i), ";" ) ) return null;
        final Token semi = sig.get(i);
        ++i;
        if( i != sig.size() ) return null;

        final Token trailingComment = findTrailingComment(stmt);
        final Token lastAnchor      = trailingComment != null ? trailingComment : semi;

        return new Row(
            keyword, anchorName, renderTokens(patternTokens), typeTokens, initTokens,
            trailingComment, lastAnchor, false
        );
    }

    /**
     * Parses {@code type Name[<T>] = TypeExpr;} (RDD_KEY_183). {@code sig.get(0)} is the `type`
     * pseudo-keyword (an ordinary IDENTIFIER token, not a real reserved word -- see {@link
     * #isTypeAliasKeyword}), rendered via its own token so the name column lines up with the
     * `let`/`const`/`var` grid's own keyword column width. Reuses the same "no multi-line/comment
     * initializer" bailouts as the plain-declarator path -- this class has no multi-line render
     * path for any row.
     */
    private Row parseTypeAlias(final List<Token> stmt, final List<Token> sig)
    {
        final Token keyword = sig.get(0);
        final Token name    = sig.get(1);
              int   i       = 2;
        // A generic type-parameter list with no default value inside (`type Foo<T> = ...`) has
        // already had its `<`/`>` reclassified to ANGLE_BRACKET_OPEN/CLOSE by the tokenizer's own
        // generic-disambiguation pass -- that shape isn't handled by this OP-literal `<`/`>` scan
        // at all (falls straight through to the `=` check below and bails, leaving the whole
        // statement untouched/unaligned, which is safe but simply not this method's concern). Only
        // a generic clause containing something that *invalidates* reclassification (most commonly
        // a type-parameter default, `<T = X>` -- `=` isn't a generic-safe OP) leaves `<`/`>` as
        // literal OP tokens, which is what this scan exists to skip past.
        final int genericStart = i;
        if( i < sig.size() && isOp( sig.get(i), "<" ) ) {
            int depth = 0;
            while( i < sig.size() ) {
                if( isOp( sig.get(i), "<" ) ) {
                    ++depth;
                }
                else if( isOp( sig.get(i), ">" ) ) {
                    --depth;
                    ++i;
                    if(depth == 0) break;
                    continue;
                }
                ++i;
            } // while
        } // if
        // The generic clause's own tokens (if any were skipped above) must be preserved in the
        // rendered name column verbatim -- previously discarded entirely, silently dropping the
        // whole `<...>` type-parameter list from the output (vuejs/core dogfood,
        // `type MergedHook<T = () => void> = T | T[]`). Rendered tight against the outer `<`/`>`
        // (matching every other generic-clause rendering in this codebase) -- `renderTokens`
        // doesn't know these particular `<`/`>` are a generic bracket pair (they're still literal
        // OP tokens here, not ANGLE_BRACKET_OPEN/CLOSE, precisely because something inside
        // invalidated the tokenizer's own reclassification), so it would otherwise space them
        // like an ordinary comparison operator (`< T = ... >`).
        final String genericClauseText = i > genericStart ? "<" + renderTokens(
            sig.subList(genericStart + 1, i - 1)
        ) + ">" : "";
        if( i >= sig.size() || !isOp( sig.get(i), "=" ) ) return null;
        final Token eqToken = sig.get(i);
        ++i;
        final int initStart = i;
              int depth     = 0;
        while( i < sig.size() ) {
            final Token t = sig.get(i);
                 if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) depth++;
            else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) depth--;
            else if( depth == 0 && isPunct(t, ";") ) break;
            ++i;
        } // while
        final List<Token> initTokens = sig.subList(initStart, i);
        if( initTokens.isEmpty() || spansMultipleLines(
            stmt, eqToken
        ) || hasCommentAfter(
            stmt, eqToken
        ) ) return null;
        // A brace-bodied object-shaped alias (`type Point = { x: number; y: number };`) already
        // has its own dedicated multi-line member-alignment pass (§14,
        // `enforceInterfaceTypeAliasMemberColonAlignment`) -- never claim it here, this class only
        // ever renders one physical line per row
        if( isPunct( initTokens.get( initTokens.size() - 1 ), "}" ) ) return null;

        if( i >= sig.size() || !isPunct( sig.get(i), ";" ) ) return null;
        final Token semi = sig.get(i);
        ++i;
        if( i != sig.size() ) return null;

        final Token trailingComment = findTrailingComment(stmt);
        final Token lastAnchor      = trailingComment != null ? trailingComment : semi;

        return new Row(
            keyword, name, name.text + genericClauseText, new ArrayList<>(), initTokens,
            trailingComment, lastAnchor, true
        );
    }

    /**
     * Scans the raw (comment-bearing) {@code stmt} for any comment token between {@code from}
     *  and {@code to} inclusive (matched by identity, since both come from a {@code
     *  significantOnly}-filtered sublist of the same token objects)
     */
    private boolean hasCommentWithin(final List<Token> stmt, final Token from, final Token to)
    {
        boolean inRange = false;
        for(final Token t : stmt) {
            if(t == from) inRange = true;
            if( inRange && (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) ) return true;
            if(t == to) break;
        }

        return false;
    }

    /**
     * Same "embedded comment inside the initializer would be silently dropped" bailout as
     *  {@code KotlinDeclarationAlignmentRule.hasCommentAfter} -- a trailing end-of-line comment
     *  (after {@code stmt}'s own last significant token) is excluded, since that one is already
     *  carried separately via {@link #findTrailingComment}.
     */
    private boolean hasCommentAfter(final List<Token> stmt, final Token afterToken)
    {
        int lastSigIdx = -1;
        for( int k = 0; k < stmt.size(); ++k ) {
            if( !isGapToken( stmt.get(k) ) ) lastSigIdx = k;
        }
        boolean seen = false;
        for( int k = 0; k < stmt.size(); ++k ) {
            final Token t = stmt.get(k);
            if( seen && k < lastSigIdx && (t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) ) return true;
            if(t == afterToken) seen = true;
        }

        return false;
    }

    /**
     * Splits one scope's direct-content tokens into groups of consecutive `let`/`const`/`var`
     * declaration statements -- same grouping-break rule as the base class's {@code
     * groupDeclarations} (STYLE.md §5)/Kotlin's {@code groupAlignableDeclarations}: a blank line,
     * a standalone leading comment, or any statement that doesn't parse as a plain declaration
     * (per this class's deliberately narrow {@link #parseDeclaration}) breaks the current group.
     */
    public List<List<Row>> groupAlignableDeclarations(final List<Token> scopeTokens)
    {
        final List<List<Token>> statements = splitStatements(scopeTokens);
        final List<List<Row>>   groups     = new ArrayList<>();
              List<Row>         current    = new ArrayList<>();

        for(final List<Token> stmt : statements) {
            final Row row = parseDeclaration(stmt);
            if(row == null) {
                if( !current.isEmpty() ) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            } // if
            // RDD_KEY_183: a `type X = ...` alias row only ever groups with other type-alias
            // rows, never mixed with a plain let/const/var declarator (or vice versa).
            final boolean kindMismatch = !current.isEmpty() && current.get(
                0
            ).isTypeAlias != row.isTypeAlias;
            final boolean breakBefore  = hasBlankLineBefore(
                stmt
            ) || hasCommentBefore(
                stmt
            ) || kindMismatch;
            if( breakBefore && !current.isEmpty() ) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(row);
        } // for
        if( !current.isEmpty() ) groups.add(current);

        return groups;
    }

    /**
     * Renders one group of consecutive {@link Row}s into a STYLE_JS_TS.md §11 column grid:
     * {@code let|const|var name [: type] = init;}, each its own {@link ColumnGrid} column so
     * both the `:` and `=` columns align "for free" via the grid's own per-column max-width
     * padding -- same shape as {@code KotlinDeclarationAlignmentRule.renderAlignedGroupRaw}. A
     * column (`:` type, `=` init, trailing comment) is only emitted at all if some row in the
     * group actually uses it, same "only emit active columns" precedent used throughout this
     * codebase's grid renderers.
     *
     * <p><b>Single-declarator groups (`group.size() == 1`) never get a real alignment grid</b> --
     * STYLE.md §5's "a lone variable with no group neighbors...align trivially with itself, do
     * not leave it awkwardly padded" rule. {@link ColumnGrid} always joins adjacent cells with a
     * single space, so putting the `:` type annotation in its own cell (as multi-row groups need,
     * to line the `:` column up across rows) would leave a stray space before the colon for a
     * single row (`x : number` instead of plain TS's `x: number`) even though there is nothing to
     * align against. Fixed by merging the name and `: type` text into one cell whenever
     * {@code group.size() == 1}, so the join space lands after the identifier as usual instead of
     * before the colon; a real (`size() > 1`) group keeps the separate-cell/grid-padding behavior,
     * where a leading space before `:` is the deliberately-documented alignment look
     * (STYLE_JS_TS.md §11.2's `DEFAULT : string` example).
     */
    public List<String> renderAlignedGroup(final List<Row> group)
    {
        boolean anyType = false;
        boolean anyInit = false;
        for(final Row r : group) {
            anyType = anyType || !r.typeTokens.isEmpty();
            anyInit = anyInit || !r.initTokens.isEmpty();
        }

        final boolean singleRow = group.size() == 1;

        final ColumnGrid grid = new ColumnGrid();
        for(final Row r : group) {
            final List<String> cells    = new ArrayList<>();
            final String       typeText = r.typeTokens.isEmpty() ? "" : ": " + renderTokens(
                r.typeTokens
            );
            cells.add(r.keyword.text);
            if(singleRow) {
                cells.add(r.nameText + typeText);
            }
            else {
                cells.add(r.nameText);
                if(anyType) cells.add(typeText);
            }
            if(anyInit) cells.add(
                r.initTokens.isEmpty() ? "" : "= " + renderTokens(r.initTokens)
            );
            // Attach the statement-terminating `;` directly to this row's own last non-empty
            // cell (name, or type/init if present) -- never as its own separately-joined cell,
            // which would leave a stray space before it (`1 ;` instead of `1;`) once ColumnGrid
            // joins cells with a single space
            int lastNonEmpty = cells.size() - 1;
            while( lastNonEmpty > 0 && cells.get(lastNonEmpty).isEmpty() ) lastNonEmpty--;
            cells.set( lastNonEmpty, cells.get(lastNonEmpty) + ";" );
            if(r.trailingComment != null) cells.add(r.trailingComment.text);
            grid.addRow( cells.toArray( new String[0] ) );
        } // for

        final List<String> lines = new ArrayList<>();
        for( final String[] row : grid.flush() ) lines.add(
            trimTrailingSpaces( String.join(" ", row) )
        );

        return lines;
    }

} // class JsTsDeclarationAlignmentRule
