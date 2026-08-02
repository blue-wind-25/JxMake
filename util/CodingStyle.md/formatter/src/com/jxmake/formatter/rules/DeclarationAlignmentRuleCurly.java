/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the Apache License, Version 2.0.
 * See the LICENSE file in the formatter root directory for the full Apache License, Version 2.0 text.
 */

package com.jxmake.formatter.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jxmake.formatter.Lang;
import com.jxmake.formatter.grid.ColumnGrid;
import com.jxmake.formatter.grid.CppModifierPriority;
import com.jxmake.formatter.grid.JavaModifierPriority;
import com.jxmake.formatter.grid.ModifierPriority;
import com.jxmake.formatter.tokenizer.TokenizerCore.Token;
import com.jxmake.formatter.tokenizer.TokenizerCore.TokenType;

import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isGapToken;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isOp;
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isPunct;

public class DeclarationAlignmentRuleCurly extends DeclarationAlignmentRuleCore {

    private static final Set<String> COMPOUND_ASSIGN_OPS = setOf(
        "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>="
    );

    private static final Set<String> TYPE_KEYWORDS_C = setOf(
        "void",
        "char",
        "short",
        "int",
        "long",
        "float",
        "double",
        "signed",
        "unsigned",
        "struct",
        "enum",
        "union",
        "bool",
        "_Bool"
    );

    private static final Set<String> TYPE_KEYWORDS_CPP = union(
        TYPE_KEYWORDS_C, setOf("bool", "wchar_t", "char16_t", "char32_t", "auto", "class")
    );

    private static final Set<String> TYPE_KEYWORDS_JAVA = setOf(
        "boolean", "byte", "char", "double", "float", "int", "long", "short", "void", "var"
    );

    /**
     * Statement keywords that can never legitimately begin a real type -- used to reject a
     *  cast-and-parenthesized-expression statement (e.g. {@code return (T)(cond ? a : b);}) from
     *  being misparsed as the function-pointer declarator shape ({@code Type (*name)(params);}).
     *  Same rationale/precedent as {@code GetterSetterRuleCurly.STATEMENT_KEYWORDS}, but scoped
     *  narrowly to the one call site that actually needs it here -- see its own guard comment.
     */
    private static final Set<String> STATEMENT_LEADING_KEYWORDS = new HashSet<>( Arrays.asList(
        "if",
        "else",
        "while",
        "for",
        "do",
        "switch",
        "try",
        "catch",
        "finally",
        "throw",
        "return",
        "synchronized"
    ) );

    private final ModifierPriority modifierPriority;
    private final Set<String>      typeKeywords;

    public DeclarationAlignmentRuleCurly(final Lang lang)
    {
        this(lang, MiscRuleCurly.DEFAULT_LINE_LENGTH_LIMIT);
    }

    public DeclarationAlignmentRuleCurly(final Lang lang, final int lineLengthLimit)
    {
        super(lang, lineLengthLimit);
        if(lang.isJava) {
            this.modifierPriority = new JavaModifierPriority();
            this.typeKeywords     = TYPE_KEYWORDS_JAVA;
        }
        else {
            this.modifierPriority = new CppModifierPriority();
            this.typeKeywords     = lang.isCpp ? TYPE_KEYWORDS_CPP : TYPE_KEYWORDS_C;
        }
    }

    private static Set<String> union(final Set<String> a, final Set<String> b)
    {
        final Set<String> result = new HashSet<>(a);
        result.addAll(b);

        return result;
    }

    /** One parsed declaration statement */
    public static final class Declaration {

        public final List<Token> modifiers;
        public final List<Token> typeTokens;
        public final Token       name;
        public final List<Token> sizeTokens;
        public final List<Token> initTokens;
        public final List<Token> bitfieldWidth;   // Empty if not a bitfield
        public final Token       trailingComment; // Nullable
        public final boolean     blankLineBefore;
        public final List<Token> templatePrefix;  // Empty unless preceded by `template<...>` (C++)

        Declaration(
            final List<Token> modifiers,
            final List<Token> typeTokens,
            final Token       name,
            final List<Token> sizeTokens,
            final List<Token> initTokens,
            final List<Token> bitfieldWidth,
            final Token       trailingComment,
            final boolean     blankLineBefore
        )
        {
            this( modifiers, typeTokens, name, sizeTokens, initTokens, bitfieldWidth,
                    trailingComment, blankLineBefore, Collections.<Token>emptyList() );
        }

        Declaration(
            final List<Token> modifiers,
            final List<Token> typeTokens,
            final Token       name,
            final List<Token> sizeTokens,
            final List<Token> initTokens,
            final List<Token> bitfieldWidth,
            final Token       trailingComment,
            final boolean     blankLineBefore,
            final List<Token> templatePrefix
        )
        {
            this.modifiers       = modifiers;
            this.typeTokens      = typeTokens;
            this.name            = name;
            this.sizeTokens      = sizeTokens;
            this.initTokens      = initTokens;
            this.bitfieldWidth   = bitfieldWidth;
            this.trailingComment = trailingComment;
            this.blankLineBefore = blankLineBefore;
            this.templatePrefix  = templatePrefix;
        }

    } // class Declaration

    /**
     * Splits one scope's direct-content tokens (already extracted by the caller --
     * e.g. one class/struct body at a fixed braceDepth, with no deeper-nested
     * tokens included) into groups of consecutive declaration statements. A blank
     * line, or any statement not recognized as a variable/field declaration, breaks
     * the current group (STYLE.md §5).
     */
    public List<List<Declaration>> groupDeclarations(final List<Token> scopeTokens)
    {
        final List<List<Token>> statements = splitStatements(scopeTokens);

        final List<List<Declaration>> groups  = new ArrayList<>();
              List<Declaration>       current = new ArrayList<>();

        for(final List<Token> stmt : statements) {
            final boolean     blankBefore = hasBlankLineBefore(stmt);
            final Declaration decl        = parseDeclaration(stmt, blankBefore);

            if(decl == null) {
                if( !current.isEmpty() ) {
                    groups.add(current);
                    current = new ArrayList<>();
                }
                continue;
            } // if

            // A standalone comment line between two declarations (e.g. `/* separator */` on its
            // own line, not a trailing same-line comment -- those are already pulled into the
            // previous statement by `pullTrailingSameLine`) must also break the group, just like
            // a blank line: `render(group)` fully regenerates the group's whole span, so a
            // comment surviving only in the raw leading gap of a mid-group declaration would be
            // silently discarded otherwise.
            final boolean isEnumConstantList = lang.isJava&& isJavaEnumConstantListShape(stmt);
            final boolean breakBefore        = blankBefore || hasCommentBefore(
                stmt
            ) || isMemberFunctionForwardDecl(
                decl
            ) || isEnumConstantList;
            if( breakBefore && !current.isEmpty() ) {
                groups.add(current);
                current = new ArrayList<>();
            }
            current.add(decl);

            // STYLE.md §8's free-function forward-declaration column alignment ("Alignment of
            // Return Types in Forward Declarations") applies to free functions only -- Java has
            // no free functions (governed by §14 instead), and in C++ a trailing `const`
            // qualifier (`V get(...) const;`) only ever appears on a member function, never a
            // free one. Isolate such a declaration into its own singleton group so it renders
            // solo (whitespace-normalized, not grid-padded against neighboring declarations).
            //
            // A Java enum constant list (`BEGIN_TOKEN("beginToken"), END_TOKEN("endToken");`)
            // gets the same singleton-group isolation for a different reason: it parses with the
            // same top-level shape as a comma-separated C-style multi-declarator statement
            // (`Type name(args), name2(args2);`), but with no leading type -- each comma segment
            // starts directly with the constant's own NAME token. Left ungrouped-off, it could
            // merge into the alignment group of an unrelated adjacent field (e.g. the enum's own
            // trailing `final String propertyKey;`), grid-padding a bogus wide column into that
            // field only on a fresh parse -- a reformat of already-separated output no longer
            // merges the groups, so the padding vanishes on the next pass (idempotency bug).
            // Isolating it here (rather than rejecting it from `parseDeclaration` outright) keeps
            // its own existing one-line-when-it-fits collapsing/rendering behavior intact.
            if( isMemberFunctionForwardDecl(decl) || isEnumConstantList ) {
                groups.add(current);
                current = new ArrayList<>();
            }
        } // for

        if( !current.isEmpty() ) groups.add(current);

        return groups;
    }

    /**
     * True if {@code decl} is a function forward declaration with a trailing `const`
     *  qualifier -- the shape only a C++ member function can have (see comment at the
     *  {@code isMemberFunctionForwardDecl} call site in {@link #groupDeclarations})
     */
    private boolean isMemberFunctionForwardDecl(final Declaration decl)
    {
        if( decl.sizeTokens.isEmpty() || !isPunct( decl.sizeTokens.get(0), "(" ) ) return false;
        final Token last = decl.sizeTokens.get( decl.sizeTokens.size() - 1 );

        return last.type == TokenType.KEYWORD && "const".equals(last.text);
    }

    // ── Static reorder safety ───────────────────────────────────────────────────
    /**
     * Moves `static` declarations to the front of the group (STYLE.md §5),
     * except where a preceding non-static is a size/value dependency of a
     * static (its name appears in that static's size or init tokens) -- the
     * whole run of not-yet-placed non-statics accumulated since the last
     * flush is kept immediately before such a static, preserving their
     * relative order, rather than attempting a finer-grained reorder whose
     * safety would be unclear.
     */
    public List<Declaration> reorderStatics(final List<Declaration> group)
    {
        final List<Declaration> output  = new ArrayList<>();
        final List<Declaration> pending = new ArrayList<>();

        for(final Declaration d : group) {
            if( isStatic(d) ) {
                if( dependsOnAny(d, pending) ) {
                    output.addAll(pending);
                    pending.clear();
                }
                output.add(d);
            } // if
            else {
                pending.add(d);
            }
        } // for
        output.addAll(pending);

        return output;
    }

    private boolean isStatic(final Declaration d)
    {
        for(final Token m : d.modifiers) {
            if( "static".equals(m.text) ) return true;
        }

        return false;
    }

    private boolean dependsOnAny(final Declaration d, final List<Declaration> candidates)
    {
        for(final Declaration c : candidates) {
            if( referencesName(
                d.sizeTokens, c.name.text
            ) || referencesName(
                d.initTokens, c.name.text
            ) ) return true;
        } // for

        return false;
    }

    private boolean referencesName(final List<Token> tokens, final String name)
    {
        for(final Token t : tokens) {
            if( t.type == TokenType.IDENTIFIER && name.equals(t.text) ) return true;
        }

        return false;
    }

    // ── Column grid rendering ───────────────────────────────────────────────────
    /**
     * Renders one declaration group into aligned source lines (STYLE.md §5,
     * STYLE_C_CPP.md §4): fixed modifier columns (only those actually used
     * anywhere in the group), the type (with C/C++ pointer attached), an
     * optional post-pointer `const` column, the name+size+`;` (or, for C/C++
     * bitfields, the name+`:`+width+`;` per STYLE_C_CPP.md §6), and an optional
     * trailing comment column. Columns unused by the whole group are omitted
     * rather than rendered as dead padding. Statics are reordered first
     * (see `reorderStatics`).
     */
    public List<String> render(final List<Declaration> originalGroup)
    {
        // C/C++ declarations must not be reordered -- changing order can alter semantics
        final List<Declaration> group = lang.isJava ? reorderStatics(originalGroup) : originalGroup;

        // Function forward declarations use a simpler 2-column layout (no modifier columns)
        boolean allAreFuncDecls = ! group.isEmpty();
        for(final Declaration d : group) {
            if( d.sizeTokens.isEmpty() || !isPunct( d.sizeTokens.get(0), "(" ) ) {
                allAreFuncDecls = false;
                break;
            }
        }
        if(allAreFuncDecls) return renderFunctionForwardGroup(group);

        final int       modifierColumns   = modifierPriority.columnCount();
        final boolean[] modifierActive    = new boolean[modifierColumns];
              boolean   postConstActive   = false;
              int       bitfieldNameWidth = 0;

        final List<TypeSplit> splits = new ArrayList<>( group.size() );
        for(final Declaration d : group) {
            if( !d.bitfieldWidth.isEmpty() ) bitfieldNameWidth = Math.max(
                bitfieldNameWidth, d.name.text.length()
            );
            for(final Token m : d.modifiers) {
                final int rank = modifierPriority.priorityOf(m.text);
                if(rank >= 0) modifierActive[rank] = true;
            }
            final TypeSplit split = lang.isJava ? null : splitCppType(d.typeTokens);
            if( split != null && !split.postConst.isEmpty() ) postConstActive = true;
            splits.add(split);
        } // for d

        boolean isStructuredBinding = false;
        if(lang.isCpp) {
            for(final Declaration d : group) {
                if( "[".equals(d.name.text) ) isStructuredBinding = true;
            }
        }

        int maxInitNameWidth = 0;
        for(final Declaration d : group) {
            if( !d.initTokens.isEmpty() ) {
                if( isStructuredBinding && !"[".equals(
                    d.name.text
                ) ) d.name.text = ' ' + d.name.text;
                maxInitNameWidth = Math.max(
                    maxInitNameWidth,
                    d.name.text.length() + renderTokens(d.sizeTokens).length()
                );
            } // if
        } // for

        final ColumnGrid grid = new ColumnGrid();
        for( int idx = 0; idx < group.size(); ++idx ) {
            final Declaration  d     = group.get(idx);
            final List<String> cells = new ArrayList<>();

            final String[] modCells = new String[modifierColumns];
            Arrays.fill(modCells, "");
            for(final Token m : d.modifiers) {
                final int rank = modifierPriority.priorityOf(m.text);
                if(rank >= 0) modCells[rank] = m.text;
            }
            for(int r = 0; r < modifierColumns; ++r) {
                if( modifierActive[r] ) cells.add( modCells[r] );
            }

            if(lang.isJava) {
                cells.add( renderTokens(d.typeTokens) );
            }
            else {
                final TypeSplit split = splits.get(idx);
                cells.add(split.typeAndStar);
                if(postConstActive) cells.add(split.postConst);
            }

            cells.add( renderNameCell(d, bitfieldNameWidth, maxInitNameWidth) );

            if(d.trailingComment != null) cells.add(d.trailingComment.text);

            grid.addRow( cells.toArray( new String[0] ) );
        } // for idx

        final List<String>   lines = new ArrayList<>();
        final List<String[]> rows  = grid.flush();
        for( int idx = 0; idx < rows.size(); ++idx ) {
            final Declaration d = group.get(idx);
            if( !d.templatePrefix.isEmpty() ) lines.add( renderTemplatePrefix(d.templatePrefix) );
            lines.add( String.join( " ", rows.get(idx) ) );
        }

        return lines;
    }

    /**
     * Renders the name+size+`;` (or, for a bitfield, name+`:`+width+`;`) cell.
     * Per STYLE_C_CPP.md §6, the bitfield name is padded only against other
     * bitfield names in the same group (`bitfieldNameWidth`), not against
     * unrelated declarations' names+sizes in the same group -- the trailing
     * comment column then falls out of ColumnGrid's normal per-column
     * max-width padding once this cell's content is fixed.
     * `maxInitNameWidth` pads the name+size portion when any sibling declaration
     * in the group has an initializer, aligning all `=` signs.
     */
    private String renderNameCell(
        final Declaration d,
        final int         bitfieldNameWidth,
        final int         maxInitNameWidth
    )
    {
        final StringBuilder sb = new StringBuilder();
        if( !d.bitfieldWidth.isEmpty() ) {
            sb.append(d.name.text);
            for( int pad = d.name.text.length(); pad < bitfieldNameWidth; ++pad ) sb.append(' ');
            sb.append(" : ").append( renderTokens(d.bitfieldWidth) );
            sb.append(";");
            return sb.toString();
        } // if
        final String nameAndSize = d.name.text + renderTokens(d.sizeTokens);
        if( !d.initTokens.isEmpty() ) {
            sb.append(nameAndSize);
            for( int pad = nameAndSize.length(); pad < maxInitNameWidth; ++pad ) sb.append(' ');
            sb.append(" = ").append( renderInitTokens(d.initTokens) );
        }
        else {
            sb.append(nameAndSize);
        }
        sb.append(";");

        return sb.toString();
    }

    /**
     * Renders a leading `template<...>` clause with no space around the outer angle brackets
     *  (STYLE_C_CPP.md: template parameter lists are always tight, unlike template argument
     *  usages such as `std::unique_ptr< T >`).
     */
    private String renderTemplatePrefix(final List<Token> templatePrefix)
    {
        final List<Token> params = templatePrefix.subList( 2, templatePrefix.size() - 1 );

        return "template<" + renderTokens(params) + ">";
    }

    private List<String> renderFunctionForwardGroup(final List<Declaration> group)
    {
        final ColumnGrid grid = new ColumnGrid();
        for(final Declaration d : group) {
            final List<Token> allTypeTokens = new ArrayList<>(d.modifiers);
            allTypeTokens.addAll(d.typeTokens);
            final String typeStr = renderTokens(allTypeTokens);
                  String nameStr = d.name.text + renderTokens(d.sizeTokens);
            if( !d.initTokens.isEmpty() ) nameStr += " = " + renderTokens(d.initTokens);
            nameStr += ";";
            grid.addRow( new String[] { typeStr, nameStr } );
        } // for
        final List<String>   lines = new ArrayList<>();
        final List<String[]> rows  = grid.flush();
        for( int idx = 0; idx < rows.size(); ++idx ) {
            final Declaration d = group.get(idx);
            if( !d.templatePrefix.isEmpty() ) lines.add( renderTemplatePrefix(d.templatePrefix) );
            String line = String.join( " ", rows.get(idx) );
            if(d.trailingComment != null) line += " " + d.trailingComment.text;
            lines.add(line);
        } // for

        return lines;
    }

    /** Splits C/C++ type tokens into the base-type(+pointer) cell and an optional post-pointer `const` cell */
    private static final class TypeSplit {

        final String typeAndStar;
        final String postConst;

        TypeSplit(final String typeAndStar, final String postConst)
        {
            this.typeAndStar = typeAndStar;
            this.postConst   = postConst;
        }

    } // class TypeSplit

    private TypeSplit splitCppType(final List<Token> typeTokens)
    {
        return new TypeSplit( renderTokens(typeTokens), "" );
    }

    /**
     * Joins tokens into canonical spaced text: `*`/`&`/`::`/generics/`[`/`]`/`,`
     * attach tightly per STYLE_C_CPP.md §4 conventions (e.g. `uint8_t*`,
     * `std::vector<int>`, `buffer[64]`); everything else gets a single space.
     */





    /**
     * True if {@code initTokens} represents a function-declaration specifier (`= 0`, `= delete`,
     *  `= default`) rather than a true variable initializer
     */
    private static boolean isFuncDeclSpecifier(final List<Token> initTokens)
    {
        if( initTokens.size() != 1 ) return false;
        final Token t = initTokens.get(0);

        return "0".equals(t.text) || "delete".equals(t.text) || "default".equals(t.text);
    }

    // ── Statement splitting ─────────────────────────────────────────────────────



    /**
     * True iff a comment or preprocessor-directive token appears in {@code stmt}'s leading gap,
     *  before the first significant (code) token -- by construction (see
     *  {@link #pullTrailingSameLine}) this can only be a standalone comment/directive on its own
     *  line, never a same-line trailing comment left over from the previous statement. A leading
     *  `#ifdef`/`#elif`/`#else`/`#endif`/etc. directive must break the group the same way a
     *  standalone comment does: {@code render(group)} only re-emits each {@code Declaration}'s own
     *  fields, so a directive's raw text embedded mid-group (not caught by
     *  {@code applyDeclarationsPass}'s leading-gap capture, which only covers the group's very
     *  first statement) would otherwise be silently dropped.
     */


    // ── Declaration parsing ──────────────────────────────────────────────────────
    /**
     * True iff `initTokens` is a single top-level `{ ... }` brace-initializer with no
     * nested `{` and no `;` inside -- i.e. a flat aggregate init like `{ a, b, c }`
     * that can be rendered verbatim on one line and safely column-aligned.
     */
    /** Last non-gap token index in {@code [from, to)}, or -1 if none */

    private boolean isFlatAggregateInit(final List<Token> initTokens)
    {
        if( !isPunct( initTokens.get(0), "{" ) ) return false;
        int depth = 0;
        for( int k = 0; k < initTokens.size(); ++k ) {
            final Token t = initTokens.get(k);
            if( isPunct(t, "{") ) {
                ++depth;
                if(depth > 1) return false;
            }
            else if( isPunct(t, "}") ) {
                --depth;
                if( depth == 0 && k != initTokens.size() - 1 ) return false;
            }
            else if( isPunct(t, ";") ) {
                return false;
            }
        } // for

        return depth == 0;
    }

    /**
     * True iff {@code initTokens} contains any `{`...`}` pair (at any nesting depth, matched by a
     *  simple depth counter -- exact partner matching isn't needed, only "some brace pair's
     *  interior held a NEWLINE token in the original source") whose interior originally spanned
     *  more than one physical source line. Used by {@link #parseDeclaration} to bail out of
     *  column-aligning a declaration whose initializer embeds a multi-line lambda/anonymous-class
     *  body mid-expression (e.g. a `.map(x -> { ...multi-statement... })` call in a stream chain)
     *  -- this class's {@link DeclarationAlignmentRuleCore#renderInitTokens} has no multi-line
     *  render path and would otherwise flatten such a body onto one, arbitrarily long physical
     *  line that no later pass can safely re-wrap. See the call site's own comment for the
     *  real-code-testing history (`jenkinsci/jenkins`'s `PluginManager.doPluginsSearch`).
     */
    private boolean containsMultilineBraceBody(final List<Token> initTokens)
    {
        // Stack of "has this open brace level seen a NEWLINE yet" flags, one per currently-open
        // `{`. A plain List used as a stack (push/pop at the end) -- simpler to reason about here
        // than java.util.Deque's reversed push/pop-order semantics.
        final List<Boolean> openLevels = new ArrayList<>();
        for(final Token t : initTokens) {
            if( isPunct(t, "{") ) {
                openLevels.add(Boolean.FALSE);
            }
            else if( isPunct(t, "}") ) {
                if( !openLevels.isEmpty() && openLevels.remove(
                    openLevels.size() - 1
                ) ) return true;
            }
            else if( t.type == TokenType.NEWLINE && !openLevels.isEmpty() ) {
                for( int k = 0; k < openLevels.size(); ++k ) openLevels.set(k, Boolean.TRUE);
            }
        } // for

        return false;
    }

    /**
     * Estimates the rendered width of a flat `{ a, b, c }` aggregate init on its own, as
     *  `render(group)` would emit it: `{`/`}` padded with one space, elements comma-separated
     *  with one space after each comma. Does not include the declaration's own type/name/`=`
     *  prefix -- callers only need this as a lower-bound check against `lineLengthLimit`.
     */
    private int flatAggregateInitRenderedWidth(final List<Token> rawInit)
    {
        int width = 0;
        for( int k = 0; k < rawInit.size(); ++k ) {
            final Token t = rawInit.get(k);
            width += t.text.length();
                 if( isPunct(t, "{") || isPunct(t, ",") ) width++; // One space after `{` or `,`
            else if( k < rawInit.size() - 1 && isPunct(
                rawInit.get(k + 1), "}"
            ) ) width++; // One space before the closing `}`
        } // for

        return width;
    }

    /**
     * Finds {@code openTok}/{@code closeTok} (by identity -- they come from {@code stmt} itself,
     *  just filtered through {@code significantOnly}) within {@code stmt} and returns the raw
     *  inclusive slice between them, comments and all. Returns {@code null} if either token
     *  can't be located (should not happen given the identity precondition, but the caller has a
     *  safe fallback).
     */
    /**
     * Same open/close-token-identity lookup as {@link #rawSliceBetween}, but returns the
     *  completely unfiltered inclusive slice (WHITESPACE/NEWLINE tokens included) -- needed by
     *  {@link #containsMultilineBraceBody}'s caller, which must see real NEWLINE tokens to detect
     *  a brace body that originally spanned more than one physical source line
     */
    private List<Token> rawSliceBetweenUnfiltered(
        final List<Token> stmt,
        final Token       openTok,
        final Token       closeTok
    )
    {
        int openIdx = - 1;
        for( int k = 0; k < stmt.size(); ++k ) {
            if( stmt.get(k) == openTok ) {
                openIdx = k;
                break;
            }
        }
        if(openIdx < 0) return null;
        int closeIdx = - 1;
        for( int k = stmt.size() - 1; k > openIdx; --k ) {
            if( stmt.get(k) == closeTok ) {
                closeIdx = k;
                break;
            }
        }
        if(closeIdx < 0) return null;

        return new ArrayList<>( stmt.subList(openIdx, closeIdx + 1) );
    }

    private List<Token> rawSliceBetween(
        final List<Token> stmt,
        final Token       openTok,
        final Token       closeTok
    )
    {
        int openIdx = - 1;
        for( int k = 0; k < stmt.size(); ++k ) {
            if( stmt.get(k) == openTok ) {
                openIdx = k;
                break;
            }
        }
        if(openIdx < 0) return null;
        int closeIdx = - 1;
        for( int k = stmt.size() - 1; k > openIdx; --k ) {
            if( stmt.get(k) == closeTok ) {
                closeIdx = k;
                break;
            }
        }
        if(closeIdx < 0) return null;
        final List<Token> raw = new ArrayList<>();
        for( final Token t : stmt.subList(openIdx, closeIdx + 1) ) {
            if(t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) raw.add(t);
        }

        return raw;
    }

    /**
     * True iff {@code stmt} (a whole raw statement, trailing {@code ;} included) is literally
     *  one or more comma-separated {@code IDENT} or {@code IDENT ( ... )} segments followed by
     *  {@code ;} and nothing else -- the shape of a Java enum constant list. See the call site in
     *  {@link #groupDeclarations} for why this must be isolated into its own singleton group.
     */
    private boolean isJavaEnumConstantListShape(final List<Token> stmt)
    {
        final List<Token> sig = significantOnly(stmt);
        if( sig.isEmpty() || !isPunct( sig.get( sig.size() - 1 ), ";" ) ) return false;
        final List<Token> body = sig.subList( 0, sig.size() - 1 );
        if( body.isEmpty() ) return false;
        int j = 0;
        while( j < body.size() ) {
            if( body.get(j).type != TokenType.IDENTIFIER ) return false;
            ++j;
            if( j < body.size() && isPunct( body.get(j), "(" ) ) {
                int depth = 1;
                ++j;
                while( j < body.size() && depth > 0 ) {
                         if( isPunct( body.get(j), "(" ) ) depth++;
                    else if( isPunct( body.get(j), ")" ) ) depth--;
                    ++j;
                }
                if(depth != 0) return false;
            } // if
            if( j == body.size() ) return true;
            if( !isPunct( body.get(j), "," ) ) return false;
            ++j;
        } // while

        return true;
    }

    private Declaration parseDeclaration(final List<Token> stmt, final boolean blankBefore)
    {
        final Token       trailingComment = findTrailingComment(stmt);
        final List<Token> sig             = significantOnly(stmt);

        if( sig.isEmpty() || !isPunct( sig.get( sig.size() - 1 ), ";" ) ) return null;
        final List<Token> body = sig.subList( 0, sig.size() - 1 );
        if( body.isEmpty() ) return null;
        // A `case ...` / `default ...` switch label is never a declaration -- reject it here
        // rather than letting the generic type/name heuristics below misparse its label tokens
        // (e.g. Java's `case CLASS, INTERFACE -> ...;`) as a bogus type/name split, which then
        // grid-aligns garbage padding into an unrelated sibling case's body (see STATE.md).
        if( body.get(
            0
        ).type == TokenType.KEYWORD && ( "case".equals(
            body.get(0).text
        ) || "default".equals(
            body.get(0).text
        ) ) ) return null;
        // A collapsed-to-one-line control statement (`if (...) stmt;`, `while (...) stmt;`,
        // `for (...) stmt;`, `switch (...) { ... }`, `do stmt while (...);`) is never a
        // declaration -- reject it here, same reasoning as the `case`/`default` guard above.
        // Without this, `BlockStructureRule.collapseSingleExpressionBlocks` braceless-collapsing
        // an `if`/`while`/`for` body down to one physical line (STYLE.md §10/§11) makes that
        // whole line look like a plausible `Type name = init;` declarator to the generic
        // heuristics below (e.g. `if (__d == _OnCompletion) co_await ...(...);` misparses as
        // type/name around the first top-level `=`-less split), which then wrongly joins it
        // into the preceding real declaration's alignment group and pads a huge, bogus column
        // width into that sibling. This only reproduces on a *second* formatting pass (the
        // collapse doesn't exist yet on the first pass), which is how it slipped through
        // idempotency testing on `exec/on_coro_disposition.hpp` in stdexec real-code-testing
        // (see STATE.md).
        if( body.get(
            0
        ).type == TokenType.KEYWORD && ( "if".equals(
            body.get(0).text
        ) || "while".equals(
            body.get(0).text
        ) || "for".equals(
            body.get(0).text
        ) || "switch".equals(
            body.get(0).text
        ) || "do".equals(
            body.get(0).text
        ) || "else".equals(
            body.get(0).text
        ) ) ) return null;
        // Same shape as the guard just above, but for a bare macro-invocation-as-statement
        // (e.g. STL's `_TRY_IO_BEGIN`/`_TRY_BEGIN`/`_BEGIN_LOCK` -- an IDENTIFIER used exactly
        // like an opening brace/statement, no trailing `;`, sitting on its own physical line)
        // immediately followed by a collapsed-to-one-line control statement. `splitStatements`
        // has no terminator to split on after the bare macro identifier, so it and the
        // following `if (...) stmt;` end up as one merged "statement" here -- `body.get(0)` is
        // then the macro IDENTIFIER, not the `if` keyword itself, so the check just above never
        // fires. Left unguarded, the combined token run misparses as a bogus `Type name = init;`
        // declarator (the macro identifier plus the condition become "type", the assignment's
        // LHS becomes "name") and gets rendered back as one physical line by `render(group)`,
        // gluing the macro and the following statement together -- reproduces on a *second*
        // formatting pass only (round1's source still has the `if`'s body braced, which the
        // guard above already rejects; only once `collapseSingleExpressionBlocks` has already
        // flattened it to a semicolon-terminated one-liner does this merged-statement shape
        // arise), found via `microsoft/STL` real-code-testing (`istream.hpp`/`stacktrace.hpp`/
        // `xlocale.hpp`). Scan the rest of `body` (depth-tracked, same as the bitfield `:` scan
        // below, so an `if`/`while`/etc. legitimately nested inside a lambda/brace-init
        // initializer at depth > 0 is not falsely rejected) for one of these keywords at the
        // top level and bail the same way if found.
        int cfDepth = 0;
        for( int cfI = 1; cfI < body.size(); ++cfI ) {
            final Token cfT = body.get(cfI);
                 if( isPunct(cfT, "(") || isPunct(cfT, "[") || isPunct(cfT, "{") ) cfDepth++;
            else if( isPunct(cfT, ")") || isPunct(cfT, "]") || isPunct(cfT, "}") ) cfDepth--;
            else if( cfDepth == 0 && cfT.type == TokenType.KEYWORD && ( "if".equals(
                cfT.text
            ) || "while".equals(
                cfT.text
            ) || "for".equals(
                cfT.text
            ) || "switch".equals(
                cfT.text
            ) || "do".equals(
                cfT.text
            ) || "else".equals(
                cfT.text
            ) ) ) return null;
        } // for

        int         i                    = 0;
        List<Token> templatePrefix       = Collections.emptyList();
        int         afterTemplateKeyword = i + 1;
        while( afterTemplateKeyword < body.size() && isGapToken(
            body.get(afterTemplateKeyword)
        ) ) afterTemplateKeyword++;
        if( lang.isCpp && i < body.size() && body.get(i).type == TokenType.KEYWORD
                && "template".equals( body.get(i).text ) && afterTemplateKeyword < body.size()
                && isOp( body.get(afterTemplateKeyword), "<" ) ) {
            int depth = 0;
            int j     = afterTemplateKeyword;
            for( ; j < body.size(); ++j ) {
                if( isOp( body.get(j), "<" ) ) {
                    ++depth;
                }
                else if( isOp( body.get(j), ">" ) ) {
                    --depth;
                    if(depth == 0) break;
                }
            } // for
            if( j < body.size() ) {
                templatePrefix = body.subList(i, j + 1);
                i              = j + 1;
            }
        } // if

        final List<Token> modifiers = new ArrayList<>();
        while( i < body.size() && body.get(i).type == TokenType.KEYWORD
                && modifierPriority.isModifier( body.get(i).text ) ) {
            modifiers.add( body.get(i) );
            ++i;
        }
        if( i >= body.size() ) return null;

        if(lang.isCpp) {
            final Declaration binding = parseStructuredBinding(
                stmt, modifiers, body, i, trailingComment, blankBefore
            );
            if(binding != null) return binding;
        }

        // Depth-aware: a bitfield's `:` is always at the declarator's own top level
        // (`int x : 3;`). A `:` nested inside `(`/`[`/`{` -- e.g. a range-based `for( auto v :
        // values )` that ended up embedded in this statement because an earlier misparse
        // treated a whole function/class body as a declarator's brace-initializer -- must never
        // be mistaken for one; scanning without depth-tracking previously misrouted such
        // statements into parseBitfield, which has no multi-line render path and collapsed the
        // entire embedded body onto one line (see the `frozen`/`catch.hpp` real-code-testing
        // notes in STATE.md).
        // A bitfield's `:` always precedes any initializer `=` (even C++20's `int x : 3 = 5;`
        // has the bitfield colon first), so the scan must stop at the first depth-0 `=` --
        // otherwise a ternary `:` inside an initializer expression (`int x = cond ? a : b;`)
        // gets misdetected as a bitfield-width colon and the whole declaration is misrouted
        // into parseBitfield, corrupting its rendering.
        int colonIdx       = - 1;
        int colonScanDepth = 0;
        for( int j = i; j < body.size(); ++j ) {
            final Token bt = body.get(j);
            if( isPunct(bt, "(") || isPunct(bt, "[") || isPunct(bt, "{") ) {
                ++colonScanDepth;
            }
            else if( isPunct(bt, ")") || isPunct(bt, "]") || isPunct(bt, "}") ) {
                --colonScanDepth;
            }
            else if( colonScanDepth == 0 && isOp(bt, "=") ) {
                break;
            }
            else if( colonScanDepth == 0 && isOp(bt, ":") ) {
                colonIdx = j;
                break;
            }
        } // for
        if(colonIdx >= 0) return parseBitfield(
            modifiers, body, i, colonIdx, trailingComment, blankBefore
        );

        int eqIdx = - 1;
        for( int j = i; j < body.size(); ++j ) {
            if( isOp( body.get(j), "=" ) ) {
                eqIdx = j;
                break;
            }
        }
        List<Token> initTokens;
        int         end;
        if(eqIdx >= 0) {
            initTokens = new ArrayList<>( body.subList( eqIdx + 1, body.size() ) );
            end        = eqIdx;
        }
        else {
            initTokens = new ArrayList<>();
            end        = body.size();
            // C++11 direct-list-init has no `=` at all (`Type name{args};`), and this branch
            // deliberately leaves `initTokens` empty for it -- the flat case (`int x{};`,
            // `Vec2 v{3, 4}`) is already rendered correctly by the generic declarator path
            // below, and a genuine named-construct body (`enum class Foo { A, B };`) is also
            // already handled correctly downstream, so this must not disturb either. But a
            // NON-flat trailing `{...}` here (nested braces, e.g. a function/method body that
            // got this far misparsed as if it were a declarator, such as
            // `struct S { int bar() {...} };` briefly parsed as "type=struct, name=S,
            // init={...}") can't be safely rendered by that generic path either -- it would
            // collapse the whole nested body onto one line and corrupt the trailing `;` into
            // `};;` (see STATE.md real-code-testing notes on `frozen`'s `catch.hpp`). Reject
            // that shape outright, leaving the statement untouched, same as the `eqIdx >= 0`
            // branch's own non-flat rejection just below.
            final int lastSig = lastSignificantIdx(body, i, end);
            if( lastSig >= 0 && isPunct( body.get(lastSig), "}" ) ) {
                int braceDepth = 0;
                int openIdx    = - 1;
                for(int k = lastSig; k >= i; --k) {
                    final Token bt = body.get(k);
                    if( isPunct(bt, "}") ) {
                        ++braceDepth;
                    }
                    else if( isPunct(bt, "{") ) {
                        --braceDepth;
                        if(braceDepth == 0) {
                            openIdx = k;
                            break;
                        }
                    }
                } // for
                if( openIdx >= 0 && !isFlatAggregateInit(
                    body.subList(openIdx, lastSig + 1)
                ) ) return null;
            } // if
        }
        // Reject if the initializer ends with `}` and isn't a flat aggregate init (e.g. a
        // lambda body, class/struct body, or nested-brace init) -- those can't safely be
        // column-aligned. A flat `{ a, b, c }` with no nested `{` or `;` inside is safe.
        if( !initTokens.isEmpty() && isPunct(
            initTokens.get( initTokens.size() - 1 ), "}"
        ) && !isFlatAggregateInit(
            initTokens
        ) ) return null;
        // A `{ ... }` aggregate init with a `//` line comment on one of its elements (e.g.
        // `{ 1, // One\n  2, // Two\n  /* Three */ 3 }`) can never be safely collapsed to one
        // line -- same reasoning as a signature parameter's `//` comment -- and this class has
        // no multi-line-initializer render path (every `Declaration` renders as exactly one
        // line). Bail out entirely so the statement is left byte-for-byte untouched, preserving
        // whatever multi-line layout (and comments) the original source already had, rather than
        // corrupting it via a collapse this rule cannot correctly perform.
        if( !initTokens.isEmpty() && isPunct( initTokens.get( initTokens.size() - 1 ), "}" ) ) {
            final List<Token> rawInit = rawSliceBetween(
                stmt, initTokens.get(0), initTokens.get( initTokens.size() - 1 )
            );
            if(rawInit != null) {
                for(final Token t : rawInit) {
                    if(t.type == TokenType.COMMENT_LINE) return null;
                }
                // A flat aggregate init that was already spread across multiple source lines
                // (e.g. a large byte/word table) can collapse to a single rendered line far past
                // `lineLengthLimit` -- this class has no multi-line-initializer render path, so
                // such a collapse can't be re-wrapped afterward. Bail out and leave the statement
                // untouched rather than emit an over-length line, same reasoning as the
                // `//`-comment guard above.
                if( flatAggregateInitRenderedWidth(rawInit) > lineLengthLimit ) return null;
            } // if
        } // if

        // A non-trailing-brace initializer (e.g. a `.stream()...map(x -> { ... }).collect(...)`
        // call chain, unlike the trailing-`}`-aggregate-init shapes rejected just above) can still
        // embed a multi-line, multi-statement lambda/anonymous-class body mid-expression. This
        // class's `renderInitTokens` flattens every gap to a single space with no line-length
        // check and no multi-line-render path at all (see that method's own javadoc) -- found via
        // `jenkinsci/jenkins` real-code testing (`PluginManager.doPluginsSearch`'s `sitePlugins =
        // site.getAvailables().stream().filter(...).map(plugin -> { <30-line multi-statement body>
        // }).collect(...)`): flattening produced a single ~1992-character physical line that no
        // later pass (`MiscRuleCurly.enforceCallLineBreaking` included -- it only ever wraps a
        // call's own top-level, comma-separated arguments, and this lambda is a single
        // non-splittable argument) can ever re-wrap back under `lineLengthLimit`. Bail out and
        // leave the statement completely untouched whenever any brace pair inside `initTokens`
        // (at any nesting depth, not just a trailing aggregate init) originally spanned more than
        // one physical source line -- same "can't safely collapse, don't try" posture as the two
        // aggregate-init bails immediately above. `initTokens` itself is filtered through
        // `significantOnly` (via `sig`/`body` above) and so never contains a NEWLINE token no
        // matter how many original physical lines it spanned -- `rawSliceBetween` re-locates the
        // same span within the original, gap-preserving `stmt` list so the newline-based check
        // below has real newlines to look for.
        if( !initTokens.isEmpty() ) {
            // `rawSliceBetween` (used by the aggregate-init bails just above) itself strips
            // WHITESPACE/NEWLINE tokens too -- it only preserves comments relative to
            // `significantOnly`'s own filtering, which is no help here since this check needs the
            // actual NEWLINE tokens. Locate the same span by identity directly in `stmt` (the
            // original, completely unfiltered token list) instead.
            final List<Token> rawInitForBraceCheck = rawSliceBetweenUnfiltered(
                stmt, initTokens.get(0), initTokens.get( initTokens.size() - 1 )
            );
            if( rawInitForBraceCheck != null
                    && containsMultilineBraceBody(rawInitForBraceCheck) ) return null;
        } // if

        // Direct function-pointer declaration (`void (*fp)(int) = NULL;`): shaped as
        // Type (*name)(params), i.e. two adjacent parenthesized groups rather than a single
        // trailing identifier. Detected independently of whether an initializer follows --
        // the `(params)` group is always part of the type/name here, unlike the generic
        // trailing-`(...)`-strip below which only fires for forward declarations (no init,
        // or a func-decl specifier like `= 0`/`= delete`). Folding `(*name)` into the name
        // cell lets `name.text + sizeTokens` render back as `(*fp)(int)`, so it aligns with
        // plain variables in the same group exactly like STATE.md's gap example requires.
        if( end > i && isPunct( body.get(end - 1), ")" ) ) {
            int depth         = 0;
            int paramsOpenIdx = - 1;
            for(int k = end - 1; k >= i; --k) {
                final Token t = body.get(k);
                if( isPunct(t, ")") ) {
                    ++depth;
                }
                else if( isPunct(t, "(") ) {
                    --depth;
                    if(depth == 0) {
                        paramsOpenIdx = k;
                        break;
                    }
                }
            } // for
            if( paramsOpenIdx > i && isPunct( body.get(paramsOpenIdx - 1), ")" ) ) {
                int depth2      = 0;
                int nameOpenIdx = - 1;
                for(int k = paramsOpenIdx - 1; k >= i; --k) {
                    final Token t = body.get(k);
                    if( isPunct(t, ")") ) {
                        ++depth2;
                    }
                    else if( isPunct(t, "(") ) {
                        --depth2;
                        if(depth2 == 0) {
                            nameOpenIdx = k;
                            break;
                        }
                    }
                } // for
                if(nameOpenIdx > i) {
                    final List<Token> inner         = body.subList(
                        nameOpenIdx + 1, paramsOpenIdx - 1
                    );
                          boolean     isFuncPtrName = ! inner.isEmpty()&& inner.get(
                              inner.size() - 1
                          ).type == TokenType.IDENTIFIER;
                    for( int k = 0; isFuncPtrName && k < inner.size() - 1; ++k ) {
                        // A run of `*` may be tokenized as one merged rep-op (e.g. `**`) rather
                        // than separate `*` tokens (see Token.isRepOp) -- accept either shape.
                        if( !Token.isRepOp( inner.get(k), '*' ) ) isFuncPtrName = false;
                    }
                    // A leading statement keyword (`return`, `throw`, `synchronized`, ...) can
                    // never legitimately begin a real type -- without this guard, a cast-and-
                    // parenthesized-expression statement like `return (T)(cond ? a : b);` or
                    // `return (J2) withExpression(...);` is misread as the function-pointer
                    // shape above: "return" as the "type", "(T)"/"(J2)" as the "(*name)" group,
                    // the trailing parenthesized expression as its "(params)". Found via
                    // `openrewrite/rewrite`'s `rewrite-kotlin/.../K.java`
                    // (`ExpressionStatement.withType`, Cluster 5): the misparsed "declaration"
                    // then merges into an adjacent real declaration's alignment group, padding
                    // "return" out to that group's type-column width on round1 -- an idempotency
                    // bug, since round2 (reformatting round1's own padded output) recomputes a
                    // narrower/different group and collapses the padding back down.
                    if( isFuncPtrName && i < nameOpenIdx && body.get(
                        i
                    ).type == TokenType.KEYWORD && STATEMENT_LEADING_KEYWORDS.contains(
                        body.get(i).text
                    ) ) isFuncPtrName = false;
                    if(isFuncPtrName) {
                        final Token       nameToken         = inner.get( inner.size() - 1 );
                        final List<Token> funcPtrTypeTokens = new ArrayList<>( body.subList(
                            i, nameOpenIdx
                        ) );
                        if( !funcPtrTypeTokens.isEmpty() ) {
                            final List<Token> rawParams         = rawSliceBetween(
                                stmt, body.get(paramsOpenIdx), body.get(end - 1)
                            );
                            final List<Token> funcPtrSizeTokens = new ArrayList<>( rawParams != null ? rawParams : body.subList(
                                paramsOpenIdx, end
                            ) );
                            // This "two adjacent parenthesized groups" shape also matches a C++
                            // concept-emulation macro call whose own args happen to look like a
                            // function-pointer's `(name)(params)` (e.g. range-v3's
                            // `CPP_broken_friend_ret(Rng)(requires ...)`), not a real function
                            // pointer -- rawParams there is a raw (comment-carrying) slice of a
                            // multi-line `//`-commented requires-clause, and this class only has a
                            // one-rendered-line render path for it (renderTokens/joinVerbatim
                            // downstream would flatten every interior `//` comment onto that one
                            // line, silently swallowing the rest of the declaration into it -- a
                            // `/* ... */` block comment is self-terminating and safe, so only
                            // COMMENT_LINE disqualifies). Checked (and, on failure, left completely
                            // untouched -- including nameToken.text below, which must stay unmutated
                            // -- falling through to the generic path, whose own checks such as the
                            // `->` type-token rejection are what actually catch this shape) before
                            // any mutation happens, so a disqualified statement is never partially
                            // rewritten.
                            boolean hasLineComment = false;
                            for(final Token t : funcPtrSizeTokens) {
                                if(t.type == TokenType.COMMENT_LINE) {
                                    hasLineComment = true;
                                    break;
                                }
                            }
                            if(!hasLineComment) {
                                final StringBuilder wrapped = new StringBuilder("(");
                                for( int k = 0; k < inner.size() - 1; ++k ) wrapped.append(
                                    inner.get(k).text
                                );
                                wrapped.append(nameToken.text).append(')');
                                nameToken.text = wrapped.toString();
                                return new Declaration(
                                    modifiers, funcPtrTypeTokens, nameToken, funcPtrSizeTokens,
                                    initTokens, new ArrayList<Token>(), trailingComment, blankBefore, templatePrefix
                                );
                            } // if
                        } // if
                    } // if
                } // if
            } // if
        } // if

        final List<Token> sizeTokens = new ArrayList<>();
              int         sizeEnd    = end;
        // A trailing `const` qualifier on a member-function forward declaration (`V get(...)
        // const;`) sits after the closing `)`, ahead of the parameter-list strip below -- peel
        // it off first so that strip still finds `)` as the last token, then re-append it to
        // `sizeTokens` afterward so it renders back after the parameter list.
        Token trailingConstQualifier = null;
        if( sizeEnd > i && body.get(sizeEnd - 1).type == TokenType.KEYWORD
                && "const".equals( body.get(sizeEnd - 1).text )
                && sizeEnd - 1 > i && isPunct( body.get(sizeEnd - 2), ")" ) ) {
            trailingConstQualifier = body.get(sizeEnd - 1);
            --sizeEnd;
        }
        while( sizeEnd > i && isPunct( body.get(sizeEnd - 1), "]" ) ) {
            int depth   = 0;
            int openIdx = - 1;
            for(int k = sizeEnd - 1; k >= i; --k) {
                final Token t = body.get(k);
                if( isPunct(t, "]") ) {
                    ++depth;
                }
                else if( isPunct(t, "[") ) {
                    --depth;
                    if(depth == 0) {
                        openIdx = k;
                        break;
                    }
                }
            } // for
            if(openIdx < 0) break; // Unbalanced -- bail, don't touch this statement
            sizeTokens.addAll( 0, body.subList(openIdx, sizeEnd) );
            sizeEnd = openIdx;
        } // while

        // Strip a trailing function-parameter list (...) to handle forward declarations.
        // Also fires for `= 0` / `= delete` / `= default` suffixes (pure-virtual / deleted /
        // defaulted), which are function specifiers, not true variable initializers.
        if( sizeEnd > i && isPunct( body.get(sizeEnd - 1), ")" )
                && ( initTokens.isEmpty() || isFuncDeclSpecifier(initTokens) ) ) {
            int depth2       = 0;
            int parenOpenIdx = - 1;
            for(int k = sizeEnd - 1; k >= i; --k) {
                final Token t = body.get(k);
                if( isPunct(t, ")") ) {
                    ++depth2;
                }
                else if( isPunct(t, "(") ) {
                    --depth2;
                    if(depth2 == 0) {
                        parenOpenIdx = k;
                        break;
                    }
                }
            } // for
            if(parenOpenIdx > i) {
                // Use the original (comment-carrying) token slice for the parameter list, not
                // the comment-stripped `body` -- a lone function-prototype declaration is fully
                // regenerated from `Declaration` fields, so any interior parameter comment (e.g.
                // `const K& /* Key */ k`) is only preserved if it's captured here.
                final List<Token> rawParams = rawSliceBetween(
                    stmt, body.get(parenOpenIdx), body.get(sizeEnd - 1)
                );
                sizeTokens.addAll(
                    0, rawParams != null ? rawParams : body.subList(parenOpenIdx, sizeEnd)
                );
                sizeEnd = parenOpenIdx;
            } // if
        } // if
        if(trailingConstQualifier != null) sizeTokens.add(trailingConstQualifier);

        if(sizeEnd <= i) return null;
        final Token name = body.get(sizeEnd - 1);
        if(name.type != TokenType.IDENTIFIER) return null;
        final List<Token> typeTokens = new ArrayList<>( body.subList(i, sizeEnd - 1) );
        if( typeTokens.isEmpty() ) return null;
        // Reject member-access expressions and qualified-name accesses (ptr->field, obj.field,
        // Type::member) in type position -- the last type token being `::` means the "name" is
        // actually the RHS of a scope-resolution expression, not a variable being declared. Also
        // reject a compound-assignment operator (e.g. `tmp += b;` tokenizes `+=` as one OP token,
        // which the eqIdx scan above only looks for a bare `=` and misses entirely -- leaving it
        // stranded in typeTokens, misparsing the whole statement as declaring type "tmp +=" name
        // "b" instead of recognizing it as not a declaration at all).
        for(final Token t : typeTokens) {
            if( isOp(t, "->") || isOp(t, ".") || COMPOUND_ASSIGN_OPS.contains(t.text) ) return null;
        }
        if( isOp( typeTokens.get( typeTokens.size() - 1 ), "::" ) ) return null;
        final Token firstType = typeTokens.get(0);
        if(firstType.type == TokenType.KEYWORD) {
            if( !typeKeywords.contains(
                firstType.text
            ) ) return null; // E.g. return/throw/assert/break -- not a declaration
        }
        else if(firstType.type != TokenType.IDENTIFIER) {
            return null; // E.g. a bare `++`/`--`/`!` prefix -- not a type, not a declaration
        }

        // sizeTokens/initTokens get flattened verbatim by renderTokens (space-joined, on one
        // rendered line) -- a `//` line comment landing inside either (e.g. a multi-line C++
        // trailing-return-type/requires-clause declaration with its own per-line ASCII-banner
        // `//` comments, range-v3's `CPP_broken_friend_ret(Rng)(requires ...) = delete;` shape)
        // would otherwise get silently swallowed into that one joined line, corrupting everything
        // after it on the same rendered line into commented-out text. A `/* ... */` block comment
        // is self-terminating and safe to flatten this way, so only COMMENT_LINE bails here.
        // trailingComment itself (already pulled out separately, rendered on its own) is exempt.
        for(final Token t : sizeTokens) {
            if(t.type == TokenType.COMMENT_LINE && t != trailingComment) return null;
        }
        for(final Token t : initTokens) {
            if(t.type == TokenType.COMMENT_LINE && t != trailingComment) return null;
        }

        return new Declaration(
            modifiers, typeTokens, name, sizeTokens, initTokens,
            new ArrayList<Token>(), trailingComment, blankBefore, templatePrefix
        );
    }

    /**
     * Parses the `auto [a, b, ...] = expr;` shape of a C++17 structured binding
     * (STYLE_CPP20.md §1). Legal type prefixes before the bracket list are only
     * `auto`/`const`/`volatile`/`&`/`&&` -- never a real identifier -- so if an
     * IDENTIFIER token or a top-level `=` appears before the first top-level `[`,
     * this isn't a structured binding and the caller falls through to the normal
     * name/size parsing path. Returns null (not just "no match" but also any
     * malformed/unbalanced shape) so the caller always has a safe fallback.
     */
    private Declaration parseStructuredBinding(
        final List<Token> stmt,
        final List<Token> modifiers,
        final List<Token> body,
        final int         typeStart,
        final Token       trailingComment,
        final boolean     blankBefore
    )
    {
        int bracketStart = - 1;
        for( int j = typeStart; j < body.size(); ++j ) {
            final Token t = body.get(j);
            if( isPunct(t, "[") ) {
                bracketStart = j;
                break;
            }
            if( isOp(t, "=") || t.type == TokenType.IDENTIFIER ) return null;
        } // for
        if(bracketStart < 0 || bracketStart <= typeStart) return null;

        int depth      = 0;
        int bracketEnd = - 1;
        for( int j = bracketStart; j < body.size(); ++j ) {
            final Token t = body.get(j);
            if( isPunct(t, "[") ) {
                ++depth;
            }
            else if( isPunct(t, "]") ) {
                --depth;
                if(depth == 0) {
                    bracketEnd = j;
                    break;
                }
            }
        } // for
        if(bracketEnd < 0) return null; // Unbalanced -- bail, don't touch this statement

        final List<Token> typeTokens = new ArrayList<>( body.subList(typeStart, bracketStart) );
        final Token       firstType  = typeTokens.get(0);
        if( firstType.type != TokenType.KEYWORD || !typeKeywords.contains(
            firstType.text
        ) ) return null;

        int eqIdx = - 1;
        for( int j = bracketEnd + 1; j < body.size(); ++j ) {
            if( isOp( body.get(j), "=" ) ) {
                eqIdx = j;
                break;
            }
        }
        if(eqIdx < 0) return null; // Structured bindings are always initialized
        final List<Token> initTokens = new ArrayList<>( body.subList( eqIdx + 1, body.size() ) );
        if( initTokens.isEmpty() ) return null;

        // `name` must stay a real token from `body` (identity-anchored back to the original
        // statement by ScopePipeline's splice-back map) -- so the opening `[` itself is the
        // name cell's first token, and the rest of the bracket (interior + closing `]`) rides
        // along as `sizeTokens`, exactly like a real array-size suffix would. renderNameCell
        // concatenates the two unchanged, producing the atomic "[a, b, c]" cell.
        final Token       bindingName  = body.get(bracketStart);
        final Token       bracketClose = body.get(bracketEnd);
        final List<Token> rawBracket   = rawSliceBetween(stmt, bindingName, bracketClose);
        final List<Token> bracketRest = rawBracket != null
                ? new ArrayList<>( rawBracket.subList( 1, rawBracket.size() ) )
                : new ArrayList<>( body.subList(bracketStart + 1, bracketEnd + 1) );

        return new Declaration(
            modifiers, typeTokens, bindingName, bracketRest,
            initTokens, new ArrayList<Token>(), trailingComment, blankBefore
        );
    }

    /** Parses the `Type name : width` shape of a C/C++ bitfield (STYLE_C_CPP.md §6). */
    private Declaration parseBitfield(
        final List<Token> modifiers,
        final List<Token> body,
        final int         typeStart,
        final int         colonIdx,
        final Token       trailingComment,
        final boolean     blankBefore
    )
    {
        if(colonIdx <= typeStart) return null;
        final Token name = body.get(colonIdx - 1);
        if(name.type != TokenType.IDENTIFIER) return null;
        final List<Token> typeTokens = new ArrayList<>( body.subList(typeStart, colonIdx - 1) );
        if( typeTokens.isEmpty() ) return null;
        final Token firstType = typeTokens.get(0);
        if(firstType.type == TokenType.KEYWORD) {
            if( !typeKeywords.contains(firstType.text) ) return null;
        }
        else if(firstType.type != TokenType.IDENTIFIER) {
            return null;
        }
        final List<Token> widthTokens = new ArrayList<>( body.subList(
            colonIdx + 1, body.size()
        ) );
        if( widthTokens.isEmpty() ) return null;
        // A real bitfield width is a simple integer expression -- never contains `{`.
        // Reject `enum Foo : Base { ... }` and `class Foo : Base { ... }` which reach
        // parseBitfield via the `:` in their inheritance/base-type specifier.
        for(final Token wt : widthTokens) {
            if( isPunct(wt, "{") ) return null;
        }

        return new Declaration(
            modifiers, typeTokens, name, new ArrayList<Token>(),
            new ArrayList<Token>(), widthTokens, trailingComment, blankBefore
        );
    }



} // class DeclarationAlignmentRuleCurly
