/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is distributed under the Apache License, Version 2.0.
 * See the LICENSE_APACHEv2.txt file in the project root directory for the full license text.
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
import static com.jxmake.formatter.tokenizer.TokenizerCore.Token.isRepOp;

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
     * cast-and-parenthesized-expression statement (e.g. {@code return (T)(cond ? a : b);}) from
     * being misparsed as the function-pointer declarator shape ({@code Type (*name)(params);}).
     * Same rationale/precedent as {@code GetterSetterRuleCurly.STATEMENT_KEYWORDS}, but scoped
     * narrowly to the one call site that actually needs it here -- see its own guard comment.
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

    /**
     * `line-split-by-operator-priority` follow-up (RDD_KEY_351) -- guards a narrow broadening of
     * {@link #spansMultipleLines}'s depth check (see that method's own javadoc). Default false
     * (flag-off pipeline byte-for-byte unchanged); set post-construction from
     * {@code ScopePipelineCurly.setLineSplitByOperatorPriority}, same "avoid threading a new
     * constructor param" precedent as {@code MiscRuleCore.lineSplitByOperatorPriority}
     * (RDD_KEY_350). Consulted by the JS/TS and Kotlin declaration-alignment subclasses, which are
     * the only callers of {@code spansMultipleLines} -- harmless on this base class and the C/C++/
     * Java subclass path, which never calls that method.
     */
    protected boolean lineSplitByOperatorPriority = false;

    public void setLineSplitByOperatorPriority(final boolean value)
    {
        this.lineSplitByOperatorPriority = value;
    }

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
        // True for a C++11 alias declaration (`using Name = Type;`, RDD_KEY_283) -- inverted
        // grammar vs. every other Declaration shape (name precedes the aliased type, which this
        // rule stores in `initTokens` reusing the existing "thing after `=`" field rather than
        // adding a brand-new one). Never true for any other Declaration shape.
        public final boolean isUsingAlias;
        // Nullable. Set only for a using-alias Declaration whose aliased type contains a `...`
        // pack-expansion/pack-indexing operator token (C++26 STYLE_CPP26.md §1, e.g.
        // `T...[N]`) -- for that shape `renderUsingAliasGroup` emits this verbatim original
        // text instead of calling `renderInitTokens` on `initTokens`, since the generic
        // token-by-token re-render has no `...`-adjacent-spacing awareness (that's owned by a
        // separate whole-file pass, `CppSpecificRule.enforcePackIndexingSpacing`, which runs
        // after declaration alignment and fixes up whatever spacing this text carries anyway --
        // see RDD_KEY_283/RDD_KEY_284). Null for every other Declaration, using-alias or not.
        public final String aliasRawTypeText;
        // Nullable. Same idea as {@link #aliasRawTypeText}, but for `templatePrefix` -- set only
        // when the `template<...>` prefix itself contains a `...` operator token
        // (`template<typename... T> using Nth = ...;`). `renderUsingAliasGroup` emits this
        // verbatim instead of calling `renderTemplatePrefix`, which would otherwise regenerate
        // the `typename...` tight-join spacing generically with no dedicated fix-up pass to
        // correct it afterward if it got it wrong (RDD_KEY_284).
        public final String templatePrefixRawText;

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
            this(modifiers, typeTokens, name, sizeTokens, initTokens, bitfieldWidth,
                    trailingComment, blankLineBefore, templatePrefix, false);
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
            final List<Token> templatePrefix,
            final boolean     isUsingAlias
        )
        {
            this(modifiers, typeTokens, name, sizeTokens, initTokens, bitfieldWidth,
                    trailingComment, blankLineBefore, templatePrefix, isUsingAlias, null);
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
            final List<Token> templatePrefix,
            final boolean     isUsingAlias,
            final String      aliasRawTypeText
        )
        {
            this(modifiers, typeTokens, name, sizeTokens, initTokens, bitfieldWidth,
                    trailingComment, blankLineBefore, templatePrefix, isUsingAlias,
                    aliasRawTypeText, null);
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
            final List<Token> templatePrefix,
            final boolean     isUsingAlias,
            final String      aliasRawTypeText,
            final String      templatePrefixRawText
        )
        {
            this.modifiers             = modifiers;
            this.typeTokens            = typeTokens;
            this.name                  = name;
            this.sizeTokens            = sizeTokens;
            this.initTokens            = initTokens;
            this.bitfieldWidth         = bitfieldWidth;
            this.trailingComment       = trailingComment;
            this.blankLineBefore       = blankLineBefore;
            this.templatePrefix        = templatePrefix;
            this.isUsingAlias          = isUsingAlias;
            this.aliasRawTypeText      = aliasRawTypeText;
            this.templatePrefixRawText = templatePrefixRawText;
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
            // silently discarded otherwise
            final boolean isEnumConstantList = lang.isJava && isJavaEnumConstantListShape(stmt);
            // A `using`-alias declaration (RDD_KEY_283) has its own dedicated render layout
            // (`renderUsingAliasGroup`, keyed on the inverted name/`=`/type grammar) that can
            // never share a group with an ordinary `Type name = init;` declaration's grid --
            // force a group break whenever the shape switches either way
            final boolean usingAliasShapeChange = !current.isEmpty() && current.get(
                current.size() - 1
            ).isUsingAlias != decl.isUsingAlias;
            final boolean breakBefore           = blankBefore || hasCommentBefore(
                stmt
            ) || isMemberFunctionForwardDecl(
                decl
            ) || isEnumConstantList || usingAliasShapeChange;
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
     * qualifier -- the shape only a C++ member function can have (see comment at the
     * {@code isMemberFunctionForwardDecl} call site in {@link #groupDeclarations})
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
     * safety would be unclear
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

        // `using Name = Type;` alias declarations (RDD_KEY_283) use their own dedicated 3-column
        // layout (`using` / name / `= Type;`) -- `groupDeclarations` never mixes this shape with
        // an ordinary declaration in the same group, so "all" and "none" are the only cases
        if( !group.isEmpty() && group.get(0).isUsingAlias ) return renderUsingAliasGroup(group);

        // Function forward declarations use a simpler 2-column layout (no modifier columns)
        boolean allAreFuncDecls = !group.isEmpty();
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
     * (STYLE_C_CPP.md: template parameter lists are always tight, unlike template argument
     * usages such as `std::unique_ptr< T >`).
     */
    private String renderTemplatePrefix(final List<Token> templatePrefix)
    {
        final List<Token> params = templatePrefix.subList( 2, templatePrefix.size() - 1 );

        return "template<" + renderTokens(params) + ">";
    }

    /**
     * Renders a group of consecutive `using Name = Type;` alias declarations (RDD_KEY_283),
     *  column-aligned on the alias name and the `=` sign -- e.g.:
     * <pre>
     * using Foo   = int;
     * using Barrr = std::vector&lt;int&gt;;
     * </pre>
     * 3 grid columns: the literal `using` keyword, the alias name (padded to align every
     *  sibling's `=`), and `= Type;` (with an optional trailing comment appended after, same
     *  convention as {@link #renderFunctionForwardGroup}). Modifiers are empty for this shape
     *  (`using` cannot be preceded by a storage-class specifier), so unlike {@link #render}'s
     *  general path, no modifier-column handling is needed here.
     */
    private List<String> renderUsingAliasGroup(final List<Declaration> group)
    {
        final ColumnGrid grid = new ColumnGrid();
        for(final Declaration d : group) {
            // A pack-indexed aliased type (`aliasRawTypeText != null`) is rendered verbatim from
            // its original text rather than through `renderInitTokens` -- see the comment at its
            // computation in `parseUsingAlias` (RDD_KEY_284): the whole-file
            // `enforcePackIndexingSpacing` pass that runs after declaration alignment fixes up
            // its `...[`-adjacent spacing regardless of which text this emits.
            final String renderedType = d.aliasRawTypeText != null ? d.aliasRawTypeText : renderInitTokens(
                d.initTokens
            );
            grid.addRow( new String[] { "using", d.name.text, "= " + renderedType + ";" } );
        } // for
        final List<String>   lines = new ArrayList<>();
        final List<String[]> rows  = grid.flush();
        for( int idx = 0; idx < rows.size(); ++idx ) {
            final Declaration d = group.get(idx);
            if( !d.templatePrefix.isEmpty() ) {
                // See the field comment on `templatePrefixRawText` (RDD_KEY_284): a
                // pack-expansion `...` inside the prefix itself gets rendered verbatim instead
                // of through `renderTemplatePrefix`'s generic regeneration.
                lines.add( d.templatePrefixRawText != null
                        ? d.templatePrefixRawText : renderTemplatePrefix(d.templatePrefix) );
            } // if
            String line = String.join( " ", rows.get(idx) );
            // Two spaces before a trailing same-line `//` comment (STYLE.md convention, e.g.
            // `int[]   x  = { 1, 2, 3 };            // single-level -- pad`), not one
            if(d.trailingComment != null) line += "  " + d.trailingComment.text;
            lines.add(line);
        } // for

        return lines;
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
     * `std::vector<int>`, `buffer[64]`); everything else gets a single space
     */





    /**
     * Depth-aware scan of {@code body} from {@code from}, looking for a bracket-depth-0 {@code
     * targetOp}. If a depth-0 {@code stopOp} (may be {@code null} to disable) is hit first, the
     * scan stops early and returns -1 -- {@code targetOp} is only ever reported when it precedes
     * any {@code stopOp} occurrence. Shared shape behind both the bitfield-colon scan (target `:`,
     * stop `=` -- a bitfield's colon always precedes any initializer `=`) and the initializer-`=`
     * scan (target `=`, no stop) in the caller just below.
     */
    private static int findTopLevelOp(
        final List<Token> body,
        final int         from,
        final String      targetOp,
        final String      stopOp
    )
    {
        int depth = 0;
        for( int j = from; j < body.size(); ++j ) {
            final Token t = body.get(j);
                 if( isPunct(t, "(") || isPunct(t, "[") || isPunct(t, "{") ) ++depth;
            else if( isPunct(t, ")") || isPunct(t, "]") || isPunct(t, "}") ) --depth;
            else if( depth == 0 && stopOp != null && isOp(t, stopOp) ) return -1;
            else if( depth == 0 && isOp(t, targetOp) ) return j;
        } // for

        return -1;
    }

    /**
     * True if {@code initTokens} represents a function-declaration specifier (`= 0`, `= delete`,
     * `= default`) rather than a true variable initializer
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
     * before the first significant (code) token -- by construction (see
     * {@link #pullTrailingSameLine}) this can only be a standalone comment/directive on its own
     * line, never a same-line trailing comment left over from the previous statement. A leading
     * `#ifdef`/`#elif`/`#else`/`#endif`/etc. directive must break the group the same way a
     * standalone comment does: {@code render(group)} only re-emits each {@code Declaration}'s own
     * fields, so a directive's raw text embedded mid-group (not caught by
     * {@code applyDeclarationsPass}'s leading-gap capture, which only covers the group's very
     * first statement) would otherwise be silently dropped.
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
     * simple depth counter -- exact partner matching isn't needed, only "some brace pair's
     * interior held a NEWLINE token in the original source") whose interior originally spanned
     * more than one physical source line AND contains a top-level `;` inside that same brace
     * pair (a real multi-statement body -- a lambda/anonymous-class block, never a flat
     * aggregate-init element list, which has no statement terminator at all). Used by
     * {@link #parseDeclaration} to bail out of column-aligning a declaration whose initializer
     * embeds a multi-line lambda/anonymous-class body mid-expression (e.g. a `.map(x -> {
     * ...multi-statement... })` call in a stream chain) -- this class's
     * {@link DeclarationAlignmentRuleCore#renderInitTokens} has no multi-line render path and
     * would otherwise flatten such a body onto one, arbitrarily long physical line that no later
     * pass can safely re-wrap. See the call site's own comment for the real-code-testing history
     * (`jenkinsci/jenkins`'s `PluginManager.doPluginsSearch`).
     *
     *  <p>The `;`-inside requirement (RDD_KEY, openrewrite/rewrite `Pep508RequirementTest.java`
     *  idempotency fix) narrows the original (2026-08-05) any-multi-line-brace bail: a top-level
     *  call argument that is itself a flat aggregate init with no embedded statement (e.g. `new
     *  String[] { a, b }` as one element of a longer `Arrays.asList(new String[] {...}, new
     *  String[] {...}, ...)` argument list) IS safely re-wrappable later by
     *  {@code MiscRuleCurly.enforceCallLineBreaking} (a top-level, comma-separated call-argument
     *  shape, exactly what that pass wraps) -- unlike the genuinely dangerous lambda/anonymous-
     *  class-body shape this bail exists for, which has no such later re-wrap path. Without this
     *  narrowing, a declaration whose initializer's own `new String[] {...}` arguments get wrapped
     *  one-per-line by `enforceCallLineBreaking` on round1 (this method still saw the pre-wrap,
     *  still-flat-per-element source and did not bail) then bails on round2 (this method now sees
     *  round1's own multi-line output and DOES bail), dropping that declaration out of its
     *  alignment group entirely on round2 and collapsing the group's surviving members' padding --
     *  a non-idempotency bug distinct from, but same family as, the (already-fixed) Cluster 5
     *  `parseDeclaration` function-pointer-declarator misdetection.
     */
    private boolean containsMultilineBraceBody(final List<Token> initTokens)
    {
        // Stack of "has this open brace level seen a NEWLINE yet"/"has this open brace level seen
        // a top-level `;` yet" flag pairs, one per currently-open `{`. A plain List used as a
        // stack (push/pop at the end) -- simpler to reason about here than java.util.Deque's
        // reversed push/pop-order semantics.
        final List<Boolean> sawNewline = new ArrayList<>();
        final List<Boolean> sawSemi    = new ArrayList<>();
        for(final Token t : initTokens) {
            if( isPunct(t, "{") ) {
                sawNewline.add(Boolean.FALSE);
                sawSemi.add(Boolean.FALSE);
            }
            else if( isPunct(t, "}") ) {
                if( !sawNewline.isEmpty() ) {
                    final boolean levelSawNewline = sawNewline.remove( sawNewline.size() - 1 );
                    final boolean levelSawSemi    = sawSemi.remove( sawSemi.size() - 1 );
                    if(levelSawNewline && levelSawSemi) return true;
                } // if
            }
            else if( t.type == TokenType.NEWLINE && !sawNewline.isEmpty() ) {
                for( int k = 0; k < sawNewline.size(); ++k ) sawNewline.set(k, Boolean.TRUE);
            }
            else if( isPunct(t, ";") && !sawSemi.isEmpty() ) {
                sawSemi.set( sawSemi.size() - 1, Boolean.TRUE );
            }
        } // for

        return false;
    }

    /**
     * Estimates the rendered width of a flat `{ a, b, c }` aggregate init on its own, as
     * `render(group)` would emit it: `{`/`}` padded with one space, elements comma-separated
     * with one space after each comma. Does not include the declaration's own type/name/`=`
     * prefix -- callers only need this as a lower-bound check against `lineLengthLimit`.
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
     * just filtered through {@code significantOnly}) within {@code stmt} and returns the raw
     * inclusive slice between them, comments and all. Returns {@code null} if either token
     * can't be located (should not happen given the identity precondition, but the caller has a
     * safe fallback).
     */
    /**
     * Same open/close-token-identity lookup as {@link #rawSliceBetween}, but returns the
     * completely unfiltered inclusive slice (WHITESPACE/NEWLINE tokens included) -- needed by
     * {@link #containsMultilineBraceBody}'s caller, which must see real NEWLINE tokens to detect
     * a brace body that originally spanned more than one physical source line
     */
    private List<Token> rawSliceBetweenUnfiltered(
        final List<Token> stmt,
        final Token       openTok,
        final Token       closeTok
    )
    {
        final int[] range = findIdentityRange(stmt, openTok, closeTok);
        if(range == null) return null;

        return new ArrayList<>( stmt.subList( range[0], range[1] + 1 ) );
    }

    private List<Token> rawSliceBetween(
        final List<Token> stmt,
        final Token       openTok,
        final Token       closeTok
    )
    {
        final int[] range = findIdentityRange(stmt, openTok, closeTok);
        if(range == null) return null;
        final List<Token> raw = new ArrayList<>();
        for( final Token t : stmt.subList( range[0], range[1] + 1 ) ) {
            if(t.type != TokenType.WHITESPACE && t.type != TokenType.NEWLINE) raw.add(t);
        }

        return raw;
    }

    /**
     * Locates {@code openTok}/{@code closeTok} within {@code stmt} by identity (forward scan for
     * the open token, then backward scan from the end for the close token) and returns
     * {@code {openIdx, closeIdx}}, or {@code null} if either can't be found
     */
    private int[] findIdentityRange(
        final List<Token> stmt,
        final Token       openTok,
        final Token       closeTok
    )
    {
        int openIdx = -1;
        for( int k = 0; k < stmt.size(); ++k ) {
            if( stmt.get(k) == openTok ) {
                openIdx = k;
                break;
            }
        }
        if(openIdx < 0) return null;
        int closeIdx = -1;
        for( int k = stmt.size() - 1; k > openIdx; --k ) {
            if( stmt.get(k) == closeTok ) {
                closeIdx = k;
                break;
            }
        }
        if(closeIdx < 0) return null;

        return new int[] {openIdx, closeIdx};
    }

    /**
     * Scans backward from {@code closeIdx} down to {@code lowerBound}, tracking nesting depth
     * over {@code open}/{@code close} punctuation, and returns the index of the matching open
     * token (or -1 if unbalanced within that range)
     */
    private int matchBracketBackward(
        final List<Token> body,
        final int         closeIdx,
        final int         lowerBound,
        final String      open,
        final String      close
    )
    {
        int depth = 0;
        for(int k = closeIdx; k >= lowerBound; --k) {
            final Token t = body.get(k);
            if( isPunct(t, close) ) {
                ++depth;
            }
            else if( isPunct(t, open) ) {
                --depth;
                if(depth == 0) return k;
            }
        } // for

        return -1;
    }

    /**
     * True iff {@code stmt} (a whole raw statement, trailing {@code ;} included) is literally
     * one or more comma-separated {@code IDENT} or {@code IDENT ( ... )} segments followed by
     * {@code ;} and nothing else -- the shape of a Java enum constant list. See the call site in
     * {@link #groupDeclarations} for why this must be isolated into its own singleton group.
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
            final Declaration usingAlias = parseUsingAlias(
                stmt, modifiers, body, i, trailingComment, blankBefore, templatePrefix
            );
            if(usingAlias != null) return usingAlias;
        }

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
        final int colonIdx = findTopLevelOp(body, i, ":", "=");
        if(colonIdx >= 0) return parseBitfield(
            modifiers, body, i, colonIdx, trailingComment, blankBefore
        );

        // RDD_KEY_321 -- depth-aware, same reasoning/shape as the colonIdx scan just above: a
        // non-depth-aware scan can lock onto a `=` nested inside a call-argument's brace body
        // (e.g. a Java anonymous-class-as-call-argument's own `int x = 1;` field/local --
        // RDD_KEY_318/319's corruption) well before any genuine top-level `=` (or with none at
        // all, as in a plain call statement like `run(new Runnable() { public void run() { int x
        // = 1; ... } });`) -- misreading everything up to that nested `=` as this statement's
        // "type" (dangling unmatched `{` tokens included) and everything after it as its
        // "initializer" (dangling unmatched `}` tokens included), bypassing every brace-balance
        // safety check below since each assumes a brace pair stays fully on one side of `eqIdx`
        // or the other.
        final int         eqIdx = findTopLevelOp(body, i, "=", null);
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
                int openIdx    = -1;
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
            final int paramsOpenIdx = matchBracketBackward(body, end - 1, i, "(", ")");
            if( paramsOpenIdx > i && isPunct( body.get(paramsOpenIdx - 1), ")" ) ) {
                final int nameOpenIdx = matchBracketBackward(body, paramsOpenIdx - 1, i, "(", ")");
                if(nameOpenIdx > i) {
                    final List<Token> inner         = body.subList(
                        nameOpenIdx + 1, paramsOpenIdx - 1
                    );
                          boolean     isFuncPtrName = !inner.isEmpty() && inner.get(
                              inner.size() - 1
                          ).type == TokenType.IDENTIFIER;
                    for( int k = 0; isFuncPtrName && k < inner.size() - 1; ++k ) {
                        // A run of `*` may be tokenized as one merged rep-op (e.g. `**`) rather
                        // than separate `*` tokens (see Token.isRepOp) -- accept either shape
                        if( !isRepOp( inner.get(k), '*' ) ) isFuncPtrName = false;
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
            final int openIdx = matchBracketBackward(body, sizeEnd - 1, i, "[", "]");
            if(openIdx < 0) break; // Unbalanced -- bail, don't touch this statement
            sizeTokens.addAll( 0, body.subList(openIdx, sizeEnd) );
            sizeEnd = openIdx;
        } // while

        // Strip a trailing function-parameter list (...) to handle forward declarations.
        // Also fires for `= 0` / `= delete` / `= default` suffixes (pure-virtual / deleted /
        // defaulted), which are function specifiers, not true variable initializers.
        if( sizeEnd > i && isPunct( body.get(sizeEnd - 1), ")" )
                && ( initTokens.isEmpty() || isFuncDeclSpecifier(initTokens) ) ) {
            final int parenOpenIdx = matchBracketBackward(body, sizeEnd - 1, i, "(", ")");
            if(parenOpenIdx > i) {
                // Use the original (comment-carrying) token slice for the parameter list, not
                // the comment-stripped `body` -- a lone function-prototype declaration is fully
                // regenerated from `Declaration` fields, so any interior parameter comment (e.g.
                // `const K& /* Key */ k`) is only preserved if it's captured here
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
     * Parses the C++11 alias-declaration shape `using Name = Type;` (STYLE_CPP.md, RDD_KEY_283)
     * -- inverted grammar vs. every other Declaration shape (`Type name = init;`): the alias
     * NAME sits right after the `using` keyword, before the `=`, and the aliased TYPE sits on
     * the right of the `=`. Modeled on top of the existing `Declaration` fields rather than
     * adding a new type: `name` is the alias name, `initTokens` holds the aliased type's
     * tokens (reusing the existing "thing after `=`" field), `typeTokens`/`sizeTokens` stay
     * empty, and `isUsingAlias` flags the shape for `groupDeclarations`'/`render`'s dedicated
     * handling. Returns null (falls through to the normal parsing path -- never mutates
     * anything before returning a real `Declaration`) for anything not matching this exact
     * shape, including a template alias's own `template<...> using Name = Type;` (the
     * `template<...>` prefix was already peeled off into `templatePrefix` by the caller before
     * `i` is passed in here, so this method only ever sees the `using ...` remainder).
     */
    private Declaration parseUsingAlias(
        final List<Token> stmt,
        final List<Token> modifiers,
        final List<Token> body,
        final int         start,
        final Token       trailingComment,
        final boolean     blankBefore,
        final List<Token> templatePrefix
    )
    {
        if( start >= body.size() || body.get(start).type != TokenType.KEYWORD
                || !"using".equals( body.get(start).text ) ) return null;

        // A standalone comment sitting between a `template<...>` prefix and the `using` keyword
        // itself (e.g. `template<typename T>\n// note\nusing Name = ...;`) has nowhere to live
        // in the `Declaration` model -- `body` is already comment-filtered by the time it gets
        // here, so this method never even sees it, and `render`'s full-span regeneration would
        // silently drop it if the statement were accepted. Detect it via the raw (unfiltered)
        // slice and bail, leaving the whole statement untouched -- same posture as every other
        // "can't safely represent this" bail in this method.
        if( !templatePrefix.isEmpty() ) {
            final List<Token> rawGap = rawSliceBetweenUnfiltered(
                stmt, templatePrefix.get( templatePrefix.size() - 1 ), body.get(start)
            );
            if(rawGap != null) {
                for(final Token t : rawGap) {
                    if(t.type == TokenType.COMMENT_LINE || t.type == TokenType.COMMENT_BLOCK) return null;
                }
            }
        } // if

        final int nameIdx = start + 1;
        if( nameIdx >= body.size() || body.get(nameIdx).type != TokenType.IDENTIFIER ) return null;
        final int eqIdx = nameIdx + 1;
        if( eqIdx >= body.size() || !isOp( body.get(eqIdx), "=" ) ) return null;
        if( eqIdx + 1 >= body.size() ) return null; // No aliased type -- malformed, leave untouched

        // A plain namespace-alias/using-declaration (`using std::vector;`, `namespace X = Y;`)
        // has no `=` right after the name and never reaches this branch at all (the `eqIdx` check
        // above already requires it); a `using` directive/declaration with no `=` falls through
        // to the generic path below, which rejects it too (KEYWORD "using" is never in
        // `typeKeywords`), leaving it untouched -- same "no corruption" behavior as before this
        // feature landed
        final List<Token> aliasType = new ArrayList<>( body.subList( eqIdx + 1, body.size() ) );
        // Multi-line/comment-carrying aliased types (e.g. a banner-commented long template
        // instantiation) can't be safely collapsed onto one rendered line -- same reasoning as
        // the generic path's own COMMENT_LINE / multiline-brace-body bails. Leave untouched.
        for(final Token t : aliasType) {
            if(t.type == TokenType.COMMENT_LINE) return null;
        }
        final List<Token> rawAliasType = rawSliceBetweenUnfiltered(
            stmt, aliasType.get(0), aliasType.get( aliasType.size() - 1 )
        );
        if( rawAliasType != null && containsMultilineBraceBody(rawAliasType) ) return null;

        // A variadic-pack `...` operator token in the `template<...>` prefix itself
        // (`template<typename... T> using Nth = T...[N];`) needs the same treatment as the
        // aliased type below: there is no separate whole-file pass that fixes up `typename...`'s
        // tight-join spacing, so `renderTemplatePrefix`'s generic whitespace-collapsing
        // regeneration has no safety net if it ever gets that spacing wrong -- but rendering the
        // prefix as its own exact original raw text sidesteps the question entirely (nothing is
        // regenerated, so there's nothing to get wrong). Only bail (leave the whole statement
        // untouched) if the raw slice genuinely can't be located.
        String templatePrefixRawText = null;
        for(final Token t : templatePrefix) {
            if( isOp(t, "...") ) {
                if( templatePrefix.isEmpty() ) return null;
                final List<Token> rawTemplatePrefix = rawSliceBetweenUnfiltered(
                    stmt, templatePrefix.get(0), templatePrefix.get( templatePrefix.size() - 1 )
                );
                if(rawTemplatePrefix == null) return null;
                final StringBuilder tpsb = new StringBuilder();
                for(final Token rt : rawTemplatePrefix) tpsb.append(rt.text);
                templatePrefixRawText = tpsb.toString();
                break;
            } // if
        } // for

        // A `...` operator token in the ALIASED TYPE itself (`T...[N]`, STYLE_CPP26.md §1 pack
        // indexing) is different: `CppSpecificRule.enforcePackIndexingSpacing` runs as a whole-
        // file pass strictly AFTER declaration alignment (`FormatterCurly`'s Phase 4, well after
        // `scopePipeline.process` in Phase ~1), re-tokenizing whatever text alignment already
        // produced and fixing up `...[`-adjacent spacing generically -- it doesn't care which
        // pass emitted the line. So instead of bailing here, render the aliased type as the
        // exact original raw text (`rawAliasType`, already computed above for the multiline
        // check) rather than through `renderInitTokens`'s generic token-by-token regeneration,
        // which has no `...`-adjacency awareness and would risk collapsing/misjudging that
        // spacing itself. Whatever spacing this raw text carries, Phase 4 corrects it afterward,
        // so alignment and correct pack-indexing spacing both end up satisfied (RDD_KEY_284).
        String aliasRawTypeText = null;
        for(final Token t : aliasType) {
            if( isOp(t, "...") ) {
                if(rawAliasType == null) return null; // No raw slice available -- leave untouched
                final StringBuilder sb = new StringBuilder();
                for(final Token rt : rawAliasType) sb.append(rt.text);
                aliasRawTypeText = sb.toString();
                break;
            } // if
        } // for

        // `typeTokens` holds just the `using` keyword itself (never rendered -- see
        // `renderUsingAliasGroup`, which always emits the literal text "using" instead) purely so
        // `ScopePipelineCurly.applyDeclarationsPass`'s splice-back anchor lookup
        // (`first.typeTokens.get(0)`, used whenever `modifiers` is empty, which it always is for
        // this shape) has a real, identity-matching token from the original list to find.
        return new Declaration(
            modifiers, Collections.singletonList( body.get(start) ), body.get(nameIdx),
            new ArrayList<Token>(), aliasType, new ArrayList<Token>(), trailingComment, blankBefore,
            templatePrefix, true, aliasRawTypeText, templatePrefixRawText
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
        int bracketStart = -1;
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
        int bracketEnd = -1;
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

        int eqIdx = -1;
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
        final List<Token> bracketRest  = rawBracket != null ? new ArrayList<>( rawBracket.subList(
            1, rawBracket.size()
        ) ) : new ArrayList<>( body.subList(
            bracketStart + 1, bracketEnd + 1
        ) );

        return new Declaration(
            modifiers, typeTokens, bindingName, bracketRest,
            initTokens, new ArrayList<Token>(), trailingComment, blankBefore
        );
    }

    /** Parses the `Type name : width` shape of a C/C++ bitfield (STYLE_C_CPP.md §6) */
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

    /**
     * True iff {@code stmt} (the declaration's raw, unfiltered token span) contains a
     * {@code NEWLINE} token, anywhere after {@code afterToken} (found by identity, since
     * {@code stmt} and the caller's filtered {@code sig}/{@code initTokens} share the same
     * {@code Token} objects), that is either (a) inside a `{`...`}` brace body (brace depth > 0
     * relative to {@code afterToken}) -- a genuine multi-line block/lambda body
     * (`Thread({ ...multi-line... }, "name")`, `if(...) { ... } else { ... }`) that must never
     * be flattened onto one line by this group's rendering -- or (b) at paren/bracket depth 0
     * *and* brace depth 0 -- a top-level newline with nothing to explain it away. A newline that
     * is neither -- i.e. sitting strictly inside a call's parens with no enclosing brace, depth
     * tracking mirroring {@code ScopePipeline.hasTopLevelNewline}'s own "ignore newlines inside
     * a call's parens" idiom -- is treated as single-line for grouping purposes: it only means a
     * brace-free initializer's own nested call argument list was wrapped across lines by a
     * previous run of {@code MiscRule.enforceCallLineBreaking} (e.g. `if(cond)
     * full.substring(\n    0, N\n) + "..."  else full`, braceless, one logical expression).
     * Without the paren-depth carve-out, such an initializer bailed out of its alignment group
     * only on a *second* format pass (intact on a fresh input, since the wrapping newlines don't
     * exist yet when this check first runs there) -- an idempotency bug where a sibling
     * declaration's own column padding silently shrank between passes. Without the brace-depth
     * override on top of it, a `{`-bodied trailing-lambda argument's *own* internal
     * statement-separating newlines would also be swallowed as "inside a call's parens" --
     * caught only via `make test` regressing the RDD_KEY_134 fixture (`real_code_regressions_17`)
     * after adding the paren-depth carve-out alone, not reasoned out in advance; a single-line
     * lambda (`x?.let { it + 1 } ?: 0`, `kt_combined_out.kt`) has no newline inside its braces at
     * all and so is correctly unaffected by either carve-out. Shared by every curly-family
     * declaration-alignment subclass that needs this bailout (originally Kotlin-only, then
     * duplicated verbatim into the JS/TS sibling -- pulled up here).
     *
     * <p>{@code line-split-by-operator-priority} follow-up (RDD_KEY_351): the paren-depth
     * carve-out's "safe to flatten, a later call-wrap pass will re-derive it" assumption assumed
     * the only thing that could ever put a newline strictly inside a call's parens was {@code
     * MiscRule.enforceCallLineBreaking} wrapping that same call's own top-level comma-separated
     * arguments -- a shape that pass genuinely can re-wrap on this same pass. {@code
     * enforceOperatorLineBreaking} breaks that assumption: its tier-2 ternary/tier-1 scan can find
     * and split an operator nested arbitrarily deep inside an array literal's spread argument
     * (`[a, ...( cond ? b : c ), d]`) at whatever depth is "shallowest available" (see D9's
     * `findBinaryOpSplits` note), landing its own newline at paren/bracket depth > 0 with no
     * enclosing call argument list any pass re-wraps -- so on a reformat, this method's carve-out
     * still says "safe to flatten" and this class's generic {@code renderTokens}-based rendering
     * (which does not reapply {@code enforceOperatorLineBreaking}'s own ternary/operator spacing
     * conventions) both drops the split AND leaves stray tight spacing behind, with nothing left
     * to re-split it (the collapsed line no longer contains the top-level `if`/`while`/`switch`/
     * `for`/return/assignment-RHS shape {@code enforceOperatorLineBreaking} itself scans for, or
     * simply now fits under the line-length limit once flattened). Found via `angular/angular`'s
     * `create_application.ts` (`RDD_KEY_349`'s real-code TS dogfood already flagged this file as a
     * recurrence of this job's "declaration/condition collapse-on-round2" Known Out-of-Scope
     * Finding, not chased there). Fixed narrowly, flag-gated: when {@code lineSplitByOperatorPriority}
     * is on, ALSO treat a newline at paren/bracket depth > 0 (brace depth 0) as spanning multiple
     * lines, closing the blind spot the carve-out leaves; the flag-off pipeline (including every
     * existing fixture relying on the original carve-out) is byte-for-byte unchanged.
     */
    protected boolean spansMultipleLines(final List<Token> stmt, final Token afterToken)
    {
        boolean seen       = false;
        int     parenDepth = 0;
        int     braceDepth = 0;
        for(final Token t : stmt) {
            if(seen) {
                     if( isPunct(t, "(") || isPunct(t, "[") ) parenDepth++;
                else if( isPunct(t, ")") || isPunct(t, "]") ) parenDepth--;
                else if( isPunct(t, "{") ) braceDepth++;
                else if( isPunct(t, "}") ) braceDepth--;
                else if( t.type == TokenType.NEWLINE && ( braceDepth > 0 || (parenDepth == 0 && braceDepth == 0)
                        || (lineSplitByOperatorPriority && parenDepth > 0 && braceDepth == 0) ) ) return true;
            } // if
            if(t == afterToken) seen = true;
        } // for

        return false;
    }

    /**
     * True iff {@code stmt} (the declaration's raw, unfiltered token span) contains a
     * {@code COMMENT_LINE}/{@code COMMENT_BLOCK} token anywhere strictly between {@code
     * afterToken} (found by identity, same approach as {@link #spansMultipleLines}) and {@code
     * stmt}'s own last significant token -- i.e. an *embedded* comment inside the initializer
     * expression that a comment-free {@code significantOnly} rebuild would otherwise silently
     * drop. A trailing end-of-line comment (after the last significant token) is deliberately
     * excluded -- that one is already carried separately via {@code findTrailingComment} and
     * rendered back, so flagging it here would needlessly break grouping for the common case of
     * a plain trailing-comment declaration. Shared by every curly-family declaration-alignment
     * subclass that needs this bailout (originally Kotlin-only, then duplicated verbatim into
     * the JS/TS sibling -- pulled up here, same history as {@link #spansMultipleLines}).
     */
    protected boolean hasCommentAfter(final List<Token> stmt, final Token afterToken)
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

    protected String trimTrailingSpaces(final String s)
    {
        int end = s.length();
        while( end > 0 && s.charAt(end - 1) == ' ' ) end--;

        return s.substring(0, end);
    }

} // class DeclarationAlignmentRuleCurly
