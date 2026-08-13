Formatter Test Suite
====================

This directory contains the permanent dogfood/regression test suite for the code-formatter code
formatter. Tests are mechanically verifiable: no manual inspection required once the expected output
files are authored.


File Naming Convention
----------------------

    <name>_inp.<ext> -- Input file (intentionally misformatted or mixed-style)
    <name>_out.<ext> -- Expected output (correctly formatted per the style guide)

Both files are committed permanently. Add more pairs over time as new edge cases or regressions are
discovered.


Test Files
----------

In-file config directive:
  in_file_config_inp/out.hpp                         -- Top-of-file JXM_CFMT_CFG directive (STATE_COMMON.md
                                                        "In-file Config Support", RDD_KEY_167/168): sets every
                                                        per-file-applicable Config Keys and Defaults key (all
                                                        except server-port). Proves indent-size=2 (1-space raw
                                                        source indentation rounded up to 2, not left at the
                                                        source's already-4-space-multiple width) and
                                                        format-macros=off (macro value columns stay unaligned
                                                        despite `make test`'s own FORMAT_MACROS=on env var --
                                                        proof the directive outranks env vars too).
                                                        header-guard-rename intentionally left off this
                                                        fixture (see RDD_KEY_168 -- untestable via the
                                                        _inp/_out diff convention, since the guard name
                                                        derives from the invocation path and _inp/_out always
                                                        differ).

  in_file_config_inp/out.java                        -- Same directive coverage as the .hpp fixture, plus
                                                        java-import-order reversed from its default (java,
                                                        com, org, other, local, static) to (static, local,
                                                        other, org, com, java); one import per bucket proves
                                                        the full reversed order is honored.

  in_file_config_inp/out.kt                          -- Same directive coverage again, plus
                                                        kotlin-import-order reversed from its default (kotlin,
                                                        java, android, com, org, other, local) to (local,
                                                        other, org, com, android, java, kotlin); one import
                                                        per bucket proves the full reversed order.

  in_file_config_lang_inp/out.h                      -- Proves the `--lang` pseudo-key directive form (`/*%
                                                        JXM_CFMT_CFG --lang=cpp */`): a `.h` file, which
                                                        `Lang.infer` defaults to `"c"`, is forced onto the
                                                        `cpp` pipeline instead -- shown by the
                                                        empty-parameter-list rendering flipping from C's
                                                        `bar(void)` to C++'s `bar()`.

  in_file_config_error_inp/out.hpp                   -- Proves the hard-error path (two JXM_CFMT_CFG
                                                        directives in one file must be rejected, never
                                                        silently resolved). Deliberately not run by `make
                                                        test` (commented out of the Makefile's INP_FILES) -- a
                                                        hard-erroring input has no formatted result to diff
                                                        against, and would always show as a spurious FAIL. See
                                                        the file itself for how to exercise it manually.

Java:
  java_core_inp/out.java                             -- Core Java 8-compatible constructs: declaration
                                                        alignment, modifier ordering, getter/setter groups,
                                                        closing comments, K&R/Allman braces, import sorting,
                                                        switch, lambdas, anonymous classes.

  java_modern_inp/out.java                           -- Java 17+ constructs: records, sealed/non-sealed
                                                        classes, switch expressions (arrow form + block body),
                                                        text blocks, var, pattern-matching instanceof.

  java_combined_inp/out.java                         -- All of the above in one realistic file: sealed class
                                                        with nested record, enum, inner classes; switch
                                                        expressions; pattern matching; text blocks; var;
                                                        getter/setter groups; import ordering.

  java_comments_inp/out.java                         -- Uncommon comment placements: // and /* */ between
                                                        annotations and declarations, inside method
                                                        signatures, inside if/for/switch headers, between else
                                                        and brace, trailing on array initializers, multi-line
                                                        block comments inside methods.

  java_format_toggle_inp/out.java                    -- JXM_CFMT_DIS/JXM_CFMT_ENA formatting-toggle markers
                                                        (both the `//%` line-comment and `/*% */`
                                                        block-comment forms), each wrapping a deliberately
                                                        misformatted method or field that must survive
                                                        byte-for-byte untouched, with normally-formatted
                                                        declarations immediately before, between, and after
                                                        each frozen region.

  java_preprocessor_method_inp/out.java              -- Regression coverage for Java source using
                                                        C-preprocessor directives, including the "preprocessor
                                                        directive glued onto a following method definition"
                                                        bug (STATE.md Known Gaps): a `#endif` directly before
                                                        a method inside a class body, with and without blank
                                                        lines and a `throws` clause, must not be joined onto
                                                        the method's own modifier line.

Kotlin:
  kt_combined_inp/out.kt                             -- Kotlin STYLE_KOTLIN.md + STYLE_KOTLIN2.md end-to-end
                                                        coverage: enum class with members, sealed classes,
                                                        data classes, type aliases, generics/variance, where
                                                        clauses, infix/extension functions, null-safety
                                                        operators, when expressions, property accessors,
                                                        destructuring declarations, labeled jumps, and ranges,
                                                        all in one realistic file. See STATE_KOTLIN.md Step 4.

  kt_comments_inp/out.kt                             -- Uncommon comment placements in Kotlin, plus
                                                        JXM_CFMT_DIS/JXM_CFMT_ENA formatting-toggle markers.
                                                        See STATE_KOTLIN.md Step 4.

C:
  c_core_inp/out.c                                   -- C11 constructs: declaration alignment, bitfields,
                                                        pointer placement, struct/enum/typedef, function
                                                        Allman braces, control-flow K&R, pre-increment, static
                                                        reordering, assignment alignment.

  c_combined_inp/out.c                               -- All C constructs together in one realistic file:
                                                        macros, enums with closing comments, structs, forward
                                                        declarations, global state alignment, public API,
                                                        internal functions, inline comments.

  c_comments_inp/out.c                               -- Uncommon comment placements in C: inside struct,
                                                        between params, inside if/for headers, between else
                                                        and brace, divider normalization, comments on macros.

  c_cpp_decl_gaps_inp/out.c                          -- Regression coverage for three DeclarationAlignmentRule
                                                        fixes (STATE.md "Known Gaps -- Fixed"): the `* const`
                                                        column gap in mixed pointer-star groups, `typedef`
                                                        joining and aligning with a surrounding plain-variable
                                                        group, and direct function-pointer declarations
                                                        (including multi-star `(**cb)`) joining a group.

C++:
  cpp_core_inp/out.cpp                               -- C++11/14 constructs: class with access specifiers,
                                                        template class, lambdas, auto return type, initializer
                                                        list constructors, getter/setter groups, extern "C".

  cpp_modern_inp/out.cpp                             -- C++17/20/23 constructs: structured bindings,
                                                        init-statement if/switch, concepts/requires,
                                                        consteval/constinit, operator<=>, coroutines
                                                        (co_yield/co_return).

  cpp_combined_inp/out.cpp                           -- All C++ constructs together: concepts, enum class,
                                                        template class with nested Config struct, structured
                                                        bindings, init-statement if/switch,
                                                        consteval/constinit, operator<=>, lambda with auto
                                                        return, extern "C", trailing comments on declarations.

  cpp_comments_inp/out.cpp                           -- Uncommon comment placements in C++: inside template
                                                        parameter lists, inside concept requires expressions,
                                                        between class specifier and base, inside function
                                                        params, inside structured bindings, inside requires
                                                        clauses.

  cpp_using_alias_inp/out.cpp                        -- `using` alias declarations (C++11+) column-aligned on
                                                        `=`: adjacent plain aliases of differing name length,
                                                        a `template<typename T> using Vec = ...` singleton
                                                        group left as its own group, and a function-local
                                                        `using Local = double;` unaffected by the file-scope
                                                        group.

C/C++ Headers:
  h_core_inp/out.h                                   -- C header with #ifndef guard: header zone spacing,
                                                        include ordering (angle vs quote), struct alignment,
                                                        pointer declarations, #ifdef __cplusplus extern "C".

  h_combined_inp/out.h                               -- Combined C header: guard zones, macros alignment,
                                                        named enum/struct with closing comments, full API
                                                        declaration group, extern "C".

  hpp_core_inp/out.hpp                               -- C++ header with #pragma once: pragma once zone
                                                        spacing, concepts, enum class, structs with
                                                        operator<=>, abstract class interface, concrete
                                                        derived classes.

  hpp_combined_inp/out.hpp                           -- Combined C++ header: pragma once zones, concepts,
                                                        template base class, concrete subclass, factory
                                                        declaration, extern "C" block.

JSON/JSON5:
  json_core_inp/out.json                             -- Plain RFC 8259 JSON: colon-alignment groups, tight
                                                        atoms-only arrays, loose arrays containing objects,
                                                        empty object/array.

  json5_core_inp/out.json5                           -- JSON5-only additions: unquoted keys, single-quoted
                                                        strings, hex/negative numbers, `//` and `/* */`
                                                        comments and a blank line each breaking an alignment
                                                        group, a backslash-newline multi-line string preserved
                                                        opaque (§1.3), a trailing comment before the closing
                                                        brace.

  json5_comments_inp/out.json5                       -- A comment breaking then re-merging a colon-alignment
                                                        group, a multi-line `/* */` comment reindented to its
                                                        new structural depth, a comment inside an array, a
                                                        `key /* comment */ : value` mid-comment excluded from
                                                        alignment, and comment-start-case normalization on
                                                        leading/trailing/mid comments. Two consecutive
                                                        standalone `//` leading comments with no blank line
                                                        between them chain-group like curly's `//`: only the
                                                        first is capitalized.

  json5_comment_banner_inp/out.json5                 -- A multi-line `/* */` comment already in the
                                                        conventional ` * `-per-line continuation-marker banner
                                                        shape gets curly-style single-unit treatment
                                                        (capitalize only the first content line).

CSS:
  css_combined_inp/out.css                           -- Property/value colon-alignment groups broken by a
                                                        comment then re-merging, a custom property (`--gap`)
                                                        joining an ordinary group, `@media`/`@supports`/
                                                        `@font-face`/`@keyframes` at-rules as headers starting
                                                        their own independent nested group, and native CSS
                                                        nesting (`&:hover`, `& .icon`) recursing the same way.

  css_comments_inp/out.css                           -- A multi-line `/* */` comment breaking a group (only
                                                        its first sentence gets comment-start-case
                                                        normalization), a `JXM_CFMT_DIS`/`ENA` marker pair
                                                        freezing a declaration's original spacing/indentation
                                                        byte-for-byte, a trailing comment before a block's
                                                        closing brace, a comment between a selector and its
                                                        `{`, a comment between a property and its `:` (`prop
                                                        /* ... */ : value`), and a comment as the sole content
                                                        before declarations inside a native-nesting `&:hover`
                                                        block.

YAML/TOML:
  yaml_core_inp/out.yaml                             -- Mapping colon-alignment group, a flow mapping short
                                                        enough to stay flow, a flow mapping converted to block
                                                        on `line-length` overflow (including its own nested
                                                        array converted the same way), sequence items one
                                                        level deeper than their parent key, a sequence of
                                                        mappings, a block scalar (`|`), an anchor/alias pair,
                                                        an explicit tag, and a multi-document stream
                                                        (`---`/`...`). Sets `indent-size=2` via an in-file `#%
                                                        JXM_CFMT_CFG` directive to exercise YAML's own
                                                        community indent convention.

  yaml_comments_inp/out.yaml                         -- A `#` comment breaking a colon-alignment group, a
                                                        comment sitting between two sequence items, a `#%
                                                        JXM_CFMT_DIS`/`ENA` marker pair freezing a
                                                        malformed-spacing line verbatim, a trailing comment,
                                                        and comment-start-case normalization.

  yaml_comment_chain_inp/out.yaml                    -- Two consecutive standalone `#` leading comments with
                                                        no blank line between them chain-group like curly's
                                                        `//`: only the first is capitalized.

  toml_core_inp/out.toml                             -- `=`-alignment group at the top level and within
                                                        `[package]`/ `[[bin]]` tables, no added indentation
                                                        for keys under a table header, a tight array of atoms
                                                        vs. a loose array containing nested arrays, an
                                                        always-single-line inline table, and a
                                                        preserved-as-written dotted key.

  toml_comments_inp/out.toml                         -- A `#` comment breaking an `=`-alignment group, a `#%
                                                        JXM_CFMT_DIS`/`ENA` marker pair freezing a
                                                        malformed-spacing line verbatim, a trailing comment,
                                                        and comment-start-case normalization.

  toml_comment_chain_inp/out.toml                    -- Two consecutive standalone `#` leading comments with
                                                        no blank line between them chain-group like curly's
                                                        `//`: only the first is capitalized.

XML:
  xml_combined_inp/out.xml                           -- `<?xml?>` PI plus a second `<?xml-stylesheet?>` PI and
                                                        `<!DOCTYPE>` all preserved opaque/verbatim including
                                                        irregular internal spacing; a multi-attribute opening
                                                        tag overflowing and wrapping one attribute per line,
                                                        with `xmlns`/`xmlns:xsi` order preserved; a short
                                                        attribute list staying on one line right next to a
                                                        longer one that wraps; an entity reference in ordinary
                                                        text left untouched; self-closing tags (never wrapped
                                                        regardless of length -- no wrap support for
                                                        self-closing tags yet, a known gap); and
                                                        `<notes>`/`<script>`/ `<style>` CDATA content all
                                                        staying fully opaque -- `<script>`/`<style>` splicing
                                                        to JS/CSS is an HTML5-only addition (§4.2), not
                                                        implemented for plain XML.

  xml_comments_inp/out.xml                           -- A standalone leading `<!-- -->` comment reindented and
                                                        case-normalized, now also exercised two levels deep
                                                        inside a nested block; an inline trailing comment
                                                        gaining a leading space before `<!--`; a multi-line
                                                        `<!-- -->` comment (raw interior contains a newline)
                                                        preserved byte-for-byte verbatim, including its own
                                                        interior indentation and `-->` on its own line
                                                        (RDD_KEY_232); a `<!--% JXM_CFMT_DIS -->`/`ENA` marker
                                                        pair freezing a malformed-spacing tag verbatim; and a
                                                        trailing comment right before the closing tag.

  xml_indent_auto_tabs_inp/out.xml                   -- `indent-style = auto` (via `<!--% JXM_CFMT_CFG -->`)
                                                        on a tab-indented input, reusing
                                                        `IndentationDetector.detectFromContent` to detect and
                                                        preserve tabs.

  xml_indent_auto_spaces_inp/out.xml                 -- same `indent-style = auto` directive on a
                                                        space-indented input, detecting and normalizing to the
                                                        configured space width.

  xml_indent_auto_fallback_inp/out.xml               -- same directive on a single-line/minified input with no
                                                        indentation hint at all, falling back to
                                                        `Config.DEFAULT_INDENT_STYLE` (spaces) exactly like
                                                        `IndentationDetector.detectFromContent`'s own
                                                        no-indented-line fallback.

  xml_mixed_content_inp/out.xml                      -- mixed text+element content (a text node and an element
                                                        node interleaved) stays inline on one line, preserved
                                                        exactly as written, incl. nested mixed content (`<i>`
                                                        containing its own `<em>`); a second paragraph long
                                                        enough that its reconstructed single line exceeds
                                                        `line-length` stays on that one (long) line rather
                                                        than being wrapped/reflowed; a text-only element and a
                                                        child-element-only list confirm both pre-existing
                                                        shapes are unaffected.

C++26:
  cpp26_core_inp/out.cpp                             -- Pack indexing (`T...[N]` tight vs. going loose when
                                                        the index contains a call or a nested bracket), `=
                                                        delete("reason")` vs. bare `= delete;`, placeholder
                                                        `_` in structured bindings and if-init, and contract
                                                        clauses (`pre`/`post`/`contract_assert`) staying
                                                        inline when the signature fits vs. one-per-line when
                                                        it doesn't.

  cpp26_comments_inp/out.cpp                         -- Uncommon comment placement around the above: leading
                                                        comment before pack indexing, comment between
                                                        `template<>` and its `using`, comments forcing an
                                                        `if`-init to stay a braced block instead of collapsing
                                                        to inline, per-clause leading/trailing contract
                                                        comments, and `/* */` block comments between contract
                                                        clauses.

  cpp26_reflection_inp/out.cpp                       -- Reflection (`^^`, `[:`/`:]` splicing): `^^` binding
                                                        tight to an initializer, a `return` expression, and a
                                                        parenthesized sub-expression; a four-member `constexpr
                                                        auto` `=`-alignment group; `[:refl:]` staying tight
                                                        vs. `[: computeRefl(x) :]` going loose because it
                                                        contains a call; a standalone splice reused as an
                                                        operand; a second alignment group after a blank line;
                                                        and an `if` going loose then collapsing to inline.
                                                        Promoted ahead of its original promotion gate
                                                        (external- corpus cross-check for STYLE_CPP26.md §5
                                                        still pending) to seed the initial tokenizer test for
                                                        `^^`/`[:`/`:]`; see STATE_CPP26.md.

  cpp26_nested_call_wrap_inp/out.cpp                 -- `stephenberry/glaze` dogfood regression (RDD_KEY_285):
                                                        an aggregate init whose only inner newline comes from
                                                        a nested call's own line-wrap (`glz::generic( 2.0\n)`)
                                                        must not be mistaken for a genuinely oversized
                                                        aggregate init -- round1/round2 idempotency check.

JS/TS:
  js_combined_inp/out.js                             -- Import grouping/sorting, inline vs. own-line decorator
                                                        placement, a private class field, static vs. instance
                                                        getter/setter one-liner alignment groups,
                                                        destructuring/spread/template literals/optional
                                                        chaining/nullish coalescing, both arrow forms, an
                                                        eight-member `const` `=`-alignment group, mandatory
                                                        blank line before `return`, and closing comments on
                                                        the class and an Allman-brace method but not a short
                                                        generator.

  js_comments_inp/out.js                             -- Leading/trailing comments surviving import resort, a
                                                        comment forcing a destructuring pattern multi-line
                                                        (and out of any `=`-alignment group), and comments
                                                        around a generator method's `yield`s.

  js_getter_setter_asi_inp/out.js                    -- A semicolon-less class field (`#cache = new Map()`,
                                                        legal under JS's ASI) sitting directly above a `static
                                                        get`/`static set` one-liner pair: GetterSetterRule's
                                                        member-splitting used to require an explicit `;`/`}`
                                                        boundary (JS/TS semicolon insertion runs in a later
                                                        phase), so the unterminated field swallowed the
                                                        following `static get` member into its own span and
                                                        desynced blank-line-boundary detection for every
                                                        member after it -- leaving the static get/set pair's
                                                        empty parens unpadded to match its sibling's width
                                                        while a plain get/set pair below it padded correctly.
                                                        Now fixed with an ASI-aware depth-0-NEWLINE statement
                                                        boundary (JS/TS only).

  js_import_ordering_comments_inp/out.js             -- §15 import-ordering comment handling (RDD_KEY_197): a
                                                        trailing same-line comment on an import (`import a
                                                        from "alpha"; // keep with a`) travels with its own
                                                        import through reordering instead of blocking the
                                                        pass; a standalone comment on its own line between two
                                                        imports now segments the import list (imports
                                                        before/after it are grouped/sorted independently,
                                                        never reordered across each other) with the comment
                                                        preserved verbatim in place, instead of bailing the
                                                        whole pass.

  js_nested_template_literal_inp/out.js              -- §4 nested template-literal interpolation (`` `outer
                                                        ${`inner ${x+1}`}` ``): the inner template literal's
                                                        own `${...}` interpolation now gets its expression
                                                        spacing normalized too (`x + 1`), not just the
                                                        outermost `${...}` span -- recursive reformatting of
                                                        any nesting depth via
                                                        enforceTemplateLiteralInterpolationSpacing.

  ts_combined_inp/out.ts                             -- Tight union/intersection `=`-alignment, both
                                                        break-before/break-after long-union continuation
                                                        styles, generics with a default type parameter,
                                                        `interface`/`type`-alias `:` alignment, both enum
                                                        forms, the full six-slot class-field modifier order, a
                                                        mixed-modifier-length alignment group, and the
                                                        two-step decorator-overflow cascade.

  ts_comments_inp/out.ts                             -- A trailing comment surviving union-continuation
                                                        realignment, a comment inside a generic type-parameter
                                                        list staying tight, comments breaking `interface`/enum
                                                        alignment groups, and a trailing comment on an
                                                        overflow-wrapped decorator staying attached to its
                                                        closing `)`.

  ts_decl_grid_ext_inp/out.ts                        -- Declaration-alignment-grid extensions
                                                        (RDD_KEY_182/183): an object-destructuring-pattern LHS
                                                        joins the same const/let `=`-alignment group as a
                                                        plain identifier declarator, and two consecutive `type
                                                        X = ...` aliases form their own `=`-aligned group.

JSX/TSX:
  jsx_tsx_return_context_inp/out.tsx                 -- JSX/TSX boundary-finding pre-pass
                                                        (XL.txt TIER 3, STATE_JS_TS.md), Increment 1:
                                                        `.tsx`-only, "after `return`" expression-start
                                                        context. A JSX tree (nested elements, an attribute, and
                                                        a `{...}` expression hole) round-trips byte-for-byte as
                                                        one opaque `JSX_SPAN` token while the surrounding
                                                        function/if statements still get real brace/indent
                                                        formatting applied around it; a self-closing `<br />`
                                                        return is also covered; an ordinary `if (x < 1)`
                                                        comparison (not after `return`) is confirmed untouched
                                                        by the pre-pass.

  jsx_tsx_arrow_ternary_context_inp/out.tsx          -- JSX/TSX boundary-finding pre-pass, Increment 2:
                                                        "after `=>`" (arrow-function body start) and "after
                                                        `?`"/"after `:`" (both branches of a ternary
                                                        conditional expression). Covers a bare-arrow-body JSX
                                                        return, a ternary whose both branches are simple JSX
                                                        elements, and a ternary whose truthy branch is a nested
                                                        JSX tree (with an attribute and a `{...}` hole) and
                                                        whose falsy branch is a self-closing `<br />`; an
                                                        ordinary `if (x < 1)` comparison is confirmed untouched.

  jsx_tsx_combined_sanity_inp/out.tsx                -- Real-shape sanity check combining all 3 contexts
                                                        landed so far (return, arrow-body, both ternary
                                                        branches) in one small component, including a ternary
                                                        nested inside a `{...}` expression hole inside a
                                                        `return`-context JSX tree, to catch context-interaction
                                                        bugs the isolated fixtures might miss.

  jsx_tsx_call_array_context_inp/out.tsx             -- JSX/TSX boundary-finding pre-pass, Increment 3:
                                                        call-argument-start and array-literal-element-start.
                                                        Covers a call with two JSX arguments (first
                                                        immediately after `(`, second after a top-level `,`)
                                                        and an array literal of two JSX elements (first
                                                        immediately after `[`, second after a top-level `,`);
                                                        an ordinary `if (x < 1)` comparison is confirmed
                                                        untouched.

  jsx_tsx_assign_logical_context_inp/out.tsx         -- JSX/TSX boundary-finding pre-pass, Increment 4:
                                                        assignment-RHS (incl. compound assignment, e.g.
                                                        `+=`) and logical/nullish-RHS (`&&`, `||`, `??`).
                                                        Covers a plain `=` assignment, a `+=` compound
                                                        assignment, and each of `&&`/`||`/`??` as the
                                                        operator immediately preceding a JSX open; an
                                                        ordinary `if (x < 1)` comparison is confirmed
                                                        untouched.

  jsx_tsx_assign_logical_sanity_inp/out.tsx          -- Real-shape sanity check combining Increment 4's
                                                        two new contexts (assignment-RHS, logical/nullish
                                                        `??`-RHS) with previously-landed contexts (plain
                                                        assignment `=`, `&&`-RHS, `return`-context,
                                                        call-argument-start, array-element-start, ternary
                                                        both branches, arrow-body) in one small component,
                                                        to catch context-interaction bugs the isolated
                                                        fixture might miss.

  jsx_tsx_grouping_paren_context_inp/out.tsx         -- JSX/TSX boundary-finding pre-pass, Increment 5:
                                                        grouping-paren-start (a `(` NOT preceded by an
                                                        IDENTIFIER/`)`/`]`, distinguishing it from a
                                                        call-open `(` already covered by Increment 3).
                                                        Covers a bare `const a = (<span>...</span>);` and a
                                                        `(<div ...>{a}</div>)` with a `{}` hole; an ordinary
                                                        `if (x < 1)` comparison is confirmed untouched (also
                                                        proving the fallback safety net: `if`'s `(` is itself
                                                        a non-call-open grouping-shaped paren, so this
                                                        context now fires on it too, but `findJsxSpanEnd`
                                                        correctly returns -1 since `x < 1` isn't real JSX).

  jsx_tsx_hole_spread_context_inp/out.tsx            -- JSX/TSX boundary-finding pre-pass, Increment 6:
                                                        bare `{`-hole-start (design list item 9, a JSX
                                                        element that is the sole content of a `{...}`
                                                        hole with nothing else preceding it) and spread
                                                        (design list item 11, `...items` immediately
                                                        before a JSX call-argument/array-element).
                                                        Covers a nested-hole return (`<div>{<span>...`),
                                                        a spread call argument, and a spread array
                                                        element; an ordinary `if (x < 1)` comparison is
                                                        confirmed untouched.

  jsx_tsx_template_hole_context_inp/out.tsx          -- JSX/TSX boundary-finding pre-pass, template-literal
                                                        `${}` hole support (design list item 10,
                                                        STATE_JS_TS.md's 2026-08-13 scoping-session
                                                        sub-contexts 0-1): a bare JSX element as a hole's
                                                        sole content (`` `text ${<Foo/>} more` ``), a plain
                                                        non-JSX interpolation (`` `sum ${a+b} end` ``,
                                                        confirming the pre-existing spacing-normalization
                                                        feature still fires via the new token-based path),
                                                        and a ternary mixing a real comparison with JSX
                                                        branches (`` `val ${x < 1 ? <A/> : <B/>} end` ``).
                                                        Only `.jsx`/`.tsx` files tokenize a template
                                                        literal's holes this way; plain `.js`/`.ts` files
                                                        keep the original single-opaque-STRING-token path
                                                        unchanged (sub-context 3).

  jsx_tsx_template_hole_nested_inp/out.tsx           -- Template-literal `${}` hole support, sub-context 2:
                                                        a template literal nested inside another hole, both
                                                        plain (`` `a ${ `b ${x+1}` } d` ``) and JSX-bearing
                                                        (`` `a ${ `b ${<X/>}` }` ``), plus an `if (x < 1)`
                                                        safety-net case. Confirms round-tripping is clean
                                                        and idempotent -- an earlier implementation folded
                                                        each hole into a synthetic token correctly but left
                                                        a nested literal's own STRING segments as separate
                                                        list entries, which `renderTokens` then spaced apart
                                                        as if they were unrelated value expressions,
                                                        corrupting (and, on a second pass, further growing)
                                                        the nested literal's raw text; fixed by folding a
                                                        nested literal's entire segment chain into one
                                                        synthetic STRING token before rendering.

  jsx_tsx_wrap_detect_context_inp/out.tsx            -- Step 2 ("context 11") Increment 1's original
                                                        detect-and-measure-only fixture: a `<VeryLongComponentName
                                                        .../>` opening tag whose attribute list exceeds
                                                        `line-length`, and a short `<Small a={1} />` tag that
                                                        doesn't. Its expected output was updated when Increment 2
                                                        landed real self-closing-tag wrapping (previously
                                                        byte-identical to the unwrapped input) -- the wide tag now
                                                        wraps one-attribute-per-line, the narrow tag is unchanged.

  jsx_tsx_self_closing_wrap_inp/out.tsx              -- Step 2 ("context 11") Increments 2-3: the actual
                                                        wrap-decision function, now covering both self-closing
                                                        JSX_SPANs (Increment 2) and children-bearing ones
                                                        (Increment 3). Five cases: a single over-width attribute on
                                                        a self-closing tag wraps onto its own line with a `/>` on
                                                        its own closing line; a zero-attribute over-width
                                                        self-closing tag is left on one line (nothing to wrap); an
                                                        over-width tag WITH children but whose OPENING TAG alone
                                                        fits under `line-length` is left on one line (width is
                                                        measured over the opening tag only, never the children,
                                                        matching Increment 1's own `JsxWrapDiagnostics`
                                                        approximation); an over-width opening tag WITH a short
                                                        single-expression child wraps its attributes with a bare
                                                        `>` (not `/>`) on its own closing line, with `{child}...`
                                                        spliced back on immediately after, byte-for-byte unchanged;
                                                        and an over-width opening tag with real multi-line JSX
                                                        children (nested elements, deliberately irregular internal
                                                        whitespace, an embedded `.map()` expression) wraps its own
                                                        attributes while every byte from the opening tag's `>`
                                                        onward -- children plus closing tag -- comes through
                                                        provably byte-identical, the explicit gating assertion
                                                        sub-context 6 requires before children-bearing wrap could
                                                        land (verified via `--diff` showing zero hunks touching
                                                        those lines, and round-trip idempotency).

  jsx_tsx_attr_kinds_wrap_inp/out.tsx                -- Step 2 ("context 11") Increment 4: proves the wrap
                                                        logic needs no real JSX-grammar understanding, only
                                                        balance-tracking, across the attribute kinds not yet
                                                        exercised by Increments 2-3 (which only used plain
                                                        `name={expr}`). Four cases, all self-closing so only the
                                                        attribute-kind handling is under test: a spread attribute
                                                        (`{...somePropsObjectThatIsQuiteLong}`) wraps as one
                                                        segment, its own `{`/`}` intact; a bare boolean attribute
                                                        (`disabledBecauseOfSomeReason`, no `=`) wraps as a plain
                                                        identifier with nothing else on its line; an
                                                        expression-valued attribute whose value itself contains
                                                        nested `()`/`.` (`onClick={handlers.click.bind(this,
                                                        item.id)}`) wraps as one segment without the inner parens
                                                        confusing the brace-only balance tracking; and a mixed
                                                        tag combining all three kinds plus a plain attribute in
                                                        one tag wraps each onto its own line in source order.

  jsx_tsx_fragment_shorthand_inp/out.tsx             -- Step 2 ("context 11") Increment 5 (real-corpus
                                                        validation): regression fixture for a real
                                                        content-corruption bug found dogfooding
                                                        reactstrap/reactstrap's DropdownToggle.js.
                                                        `parseJsxTag` required a tag-name IDENTIFIER
                                                        unconditionally, so bare fragment shorthand (`<>`/
                                                        `</>`, no tag name) was never recognized as JSX at
                                                        all -- its `{...}` expression content fell through to
                                                        ordinary JS statement-level formatting, which wrongly
                                                        inserted a semicolon inside the hole
                                                        (`{returnFunction(...)}}` -> `{returnFunction(...);}`),
                                                        an actual behavior change. Fixed by giving fragments an
                                                        empty-string tagName sentinel so the existing
                                                        open/close tag-identity check pairs them correctly with
                                                        no other logic changes. Three cases: a bare-expression
                                                        fragment child (the exact corrupted shape); a
                                                        multi-child fragment; a fragment nested inside a normal
                                                        element's children. All three round-trip byte-identical
                                                        (fragments have no attributes, so wrap logic never
                                                        engages -- this fixture is purely about detection, not
                                                        wrapping).

  jsx_in_plain_js_inp/out.js                         -- STATE_JS_TS.md's 2026-08-13 implementation section
                                                        (recommendation 1): plain `.js` now gets the same JSX
                                                        boundary-finding pre-pass as `.jsx`/`.tsx`
                                                        (Lang.isJsxSyntaxPath widened unconditionally to
                                                        `.js`/`.mjs`/`.cjs`). Same real-shape content as
                                                        jsx_tsx_combined_sanity, confirming JSX inside a plain
                                                        `.js` file is preserved rather than corrupted.

  ts_jsx_default_off_inp/out.ts                      -- Recommendation 2: `.ts` stays gated off by default --
                                                        a legacy angle-bracket cast (`<string>x`) is left as
                                                        ordinary TS syntax, not misdetected as a JSX open tag.

  ts_jsx_optin_inp/out.ts                            -- Recommendation 3: the new `jsx-in-ts` Config key (set
                                                        via a `JXM_CFMT_CFG` in-file directive here) lets a
                                                        `.ts` file opt into the JSX pre-pass -- same
                                                        real-shape JSX content as jsx_in_plain_js, now
                                                        preserved on `.ts` once opted in.

  jsx_mismatched_tag_inp/out.jsx                     -- Recommendation 4: TokenizerCurly.findJsxSpanEnd/
  js_mismatched_tag_inp/out.js                          parseJsxTag now track tag-name identity (not just
                                                        nesting depth) via a Deque<String> stack, so
                                                        `<a>text</b>` bails out (-1) instead of balancing as
                                                        if valid JSX. Verified in both `.jsx` (pre-existing
                                                        gate) and the newly-widened `.js` context -- output
                                                        is plain, sane, non-corrupted formatting rather than
                                                        a thrown exception or silently-accepted mismatch.

HTML5:
  html_combined_inp/out.html                         -- Void element normalization (`<img>`/`<input>`/ `<br>`
                                                        lose self-closing `/`, contrasted with `<link>`), bare
                                                        boolean attributes, a tag whose combined attribute
                                                        width overflows and wraps one per line, an embedded
                                                        `<style>` block dispatched to CSS formatting, an
                                                        embedded `<script>` block dispatched to JS formatting,
                                                        ordinary nesting, and `<pre>` content preserved
                                                        byte-for-byte.

  html_comments_inp/out.html                         -- Stacked leading `<!-- -->` comments, an inline
                                                        trailing comment, opaque CDATA in a non-script tag, a
                                                        `data:` URI attribute overflowing by length (not
                                                        count), a comment as sole content inside a spliced
                                                        `<style>` block, the CDATA-wrapped `<style>` idiom
                                                        dispatched to CSS formatting and re-wrapped, the
                                                        CDATA-wrapped `<script>` idiom dispatched to JS
                                                        formatting and re-wrapped, and a `<script
                                                        type="application/json">` block staying fully opaque.

  html_multiline_comment_verbatim_inp/out.html       -- A `<!-- -->` comment whose raw interior contains a
                                                        newline (copyright-block style, indented interior
                                                        lines, `-->` on its own line) preserved byte-for-byte;
                                                        a sibling `<p>` immediately before AND after the
                                                        multi-line comment reindented normally; a single-line
                                                        comment nearby still gets normal trim/capitalization,
                                                        proving the newline-detection gate discriminates
                                                        correctly (RDD_KEY_232).

  html_multiline_comment_banner_inp/out.html         -- A multi-line `<!-- -->` comment whose continuation
                                                        lines already follow the conventional ` * `-per-line
                                                        banner shape (curly's `/* */` equivalent) gets the
                                                        same treatment curly gives it instead of
                                                        freeze-verbatim: first content line capitalized, sole
                                                        trailing period stripped, reindented to the comment's
                                                        own depth.

  html_mathml_case_fixup_inp/out.html                -- MathML content inside HTML5 `<math>` element with
                                                        `definitionurl` attribute/tag -- proves mathmlDepth
                                                        tracking and MathML case-fixup (STATE_DATA_FORMATS.md)
                                                        correctly adjusts `definitionurl` to `definitionURL`.


  html_tc_gap_level0_body_unchanged_inp/out.html     -- Same no-explicit-`<body>` shape as above, but at the
                                                        default `html5-tc-gap-level=0` (unset, no in-file
                                                        override) -- proves the level-1 fabricated- node path
                                                        stays fully inert unless explicitly opted into, i.e.
                                                        current behavior is unchanged by default.

  html_tc_gap_level1_body_insertion_inp/out.html     -- STATE_HTML5_TCG.md tc gap job, level 1
                                                        (`html5-tc-gap-level=1` via in-file config,
                                                        RDD_KEY_230): a document with no explicit `<body>`
                                                        start tag anywhere gets one synthesized around the
                                                        first non-head content and every sibling after it
                                                        (multiple head-adjacent content nodes -- an `<h1>`, a
                                                        `<p>`, and an `<h2>` -- all land inside the single
                                                        synthesized `<body>`, proving the `bodyInserted` guard
                                                        fires at most once per document).

  html_tc_gap_level1_no_head_inp/out.html            -- STATE_HTML5_TCG.md tc gap job, level 1 fix
                                                        (`html5-tc-gap-level=1` via in-file config): a
                                                        document with no explicit `<head>` element at all --
                                                        `<meta>`, `<title>`, and `<script>` siblings appear
                                                        directly under `<html>` before an `<h1>`/`<p>` --
                                                        proves the `headInsertionModeClosed` tracked
                                                        transition (not the old sibling heuristic) keeps the
                                                        head-eligible siblings out of the synthesized
                                                        `<body>`, while the real content (`<h1>`, `<p>`) still
                                                        gets wrapped.

  html_tc_gap_level1_foster_unchanged_inp/out.html   -- Same table-with-stray-content shape as the level-2
                                                        fixture below, but at `html5-tc-gap-level=1` via
                                                        in-file config -- proves foster-parenting stays fully
                                                        inert one level below its own gate (`>= 2`), i.e. a
                                                        `<table>` with stray text/`<div>` content directly
                                                        inside it (outside any `<tr>`/`<td>`/ `<caption>`)
                                                        still formats in place, unchanged, at level 1.

  html_tc_gap_level2_foster_parenting_inp/out.html   -- STATE_HTML5_TCG.md tc gap job, level 2
                                                        (`html5-tc-gap-level=2` via in-file config,
                                                        RDD_KEY_230): a `<table>` with stray text and a stray
                                                        `<div>` directly inside it (outside any
                                                        `<tr>`/`<td>`/`<caption>`), alongside a real
                                                        `<tr><td>` row -- proves both the stray text and the
                                                        stray `<div>` (with its own content intact) get
                                                        relocated to just before the `<table>`
                                                        (`FosterBuffer`/`fosterBufferStack`), while the real
                                                        `<tr>`/`<td>` row stays nested inside the table
                                                        unchanged.

  html_tc_gap_level2_form_unchanged_inp/out.html     -- Same nested-`<form>`-in-`<template>`-in-`<form>` shape
                                                        as the level-3 fixture below, but at
                                                        `html5-tc-gap-level=2` via in-file config -- proves
                                                        misnested-`<form>` reconstruction stays fully inert
                                                        one level below its own gate (`>= 3`), i.e. the direct
                                                        second `<form id="second-direct">` sibling still
                                                        formats in place, unchanged, at level 2.

  html_tc_gap_level3_form_template_inp/out.html      -- STATE_HTML5_TCG.md tc gap job, level 3
                                                        (`html5-tc-gap-level=3` via in-file config,
                                                        RDD_KEY_230): an outer `<form id="outer">` contains a
                                                        `<template>` with its own nested `<form id="inner">`
                                                        plus a direct second sibling `<form
                                                        id="second-direct">` -- proves
                                                        `currentFormElementPointer` correctly distinguishes
                                                        the two cases: the `<template>`-scoped inner form is
                                                        preserved (a `<template>` boundary gets its own fresh
                                                        form-pointer scope), while the direct second `<form>`
                                                        sibling (same scope as the still-active outer form) is
                                                        suppressed -- its wrapping element dropped, only its
                                                        own `<p>` content spliced into the outer form's
                                                        children.

  html_tc_gap_level3_adoption_unchanged_inp/out.html -- Same misnested-`<b>`/`<i>` shape as the level-4
                                                        fixture below (`<b>one<i>two</b>three</i>`), but at
                                                        `html5-tc-gap-level=3` via in-file config -- proves
                                                        adoption agency reconstruction stays fully inert one
                                                        level below its own gate (`>= 4`), i.e. `three`
                                                        remains plain text after `</b>` closes, not wrapped in
                                                        a reconstructed `<i>`.

  html_tc_gap_level4_adoption_agency_inp/out.html    -- STATE_HTML5_TCG.md tc gap job, level 4
                                                        (`html5-tc-gap-level=4` via in-file config,
                                                        RDD_KEY_230): the classic adoption-agency misnesting
                                                        `<b>one<i>two</b>three</i>` -- `</b>` closes while
                                                        `<i>` is still open, implicitly closing `<i>` early;
                                                        once `</b>` genuinely closes the outer `<b>`, a
                                                        reconstructed `<i>` clone wraps the remaining `three`
                                                        as `<b>`'s own next sibling, matching the spec's
                                                        adoption agency recovery for this single-level case.
                                                        See `reconstructFormattingElement`'s own javadoc in
                                                        `XmlSpecificRule.java` for the documented subset of
                                                        the spec algorithm implemented vs. skipped (only the
                                                        single most-recently-orphaned formatting element is
                                                        tracked, not a full list-of-active-formatting-elements
                                                        + furthest-block + bookmark algorithm).

Python3:
  py_combined_inp/out.py                             -- Bracket-complexity categories, assignment alignment
                                                        (augmented assignment, both continuation-break
                                                        styles), import ordering/grouping including
                                                        `__future__` promotion, decorators, f-strings,
                                                        function signature wrapping with type hints,
                                                        structural pattern matching, single-statement compound
                                                        bodies, control-flow blank lines, `async`/`await`, and
                                                        a `@property`/`@x.setter` pair.

  py_comments_inp/out.py                             -- Uncommon `#` comment placement: a comment breaking an
                                                        assignment-alignment group, trailing comments not
                                                        breaking a comprehension-assignment group, a comment
                                                        forcing a signature to wrap, a byte-for-byte-preserved
                                                        docstring, a comment between two `case` blocks, and a
                                                        comment breaking a compact `case`-line alignment
                                                        group.

  py_comments_normalization_inp/out.py               -- RDD_KEY_268: `normalize-comment-start-case`/
                                                        `normalize-comment-end-period` for python3's `#`
                                                        comments (previously not wired up at all). Uses `#%
                                                        JXM_CFMT_CFG comment-normalization-classifier=off` so
                                                        the deterministic no-capitalize-word-list path is
                                                        exercised instead of the default classifier/GRU path.
                                                        Covers: a 3-line standalone chain (only the first
                                                        line's start is capitalized; the sole trailing period,
                                                        found only on the last line, is stripped only because
                                                        it is the sole `.` across the whole chain); `noqa`/
                                                        `type` leading directive words staying lowercase via
                                                        the new python-specific no-capitalize word list; a
                                                        trailing (non-standalone) comment normalizing as its
                                                        own singleton group; and an ordinary standalone
                                                        single-comment capitalization.

  py_import_blank_lines_inp/out.py                   -- RDD_KEY_247: `python-import-blank-lines` (default 1)
                                                        collapses a 2-blank-line gap between two adjacent
                                                        same-depth import groups down to 1, while a
                                                        2-blank-line gap NOT between two import groups (before
                                                        a following `def`) is left untouched -- proves the new
                                                        blank-line normalization pass is scoped to
                                                        import-group boundaries only. Each group is also
                                                        sorted (`python-import-sort`, default on) alongside
                                                        the blank-line normalization.

  py_import_multiline_inp/out.py                     -- RDD_KEY_277: `classifyImport` extended to sort/group
                                                        the three multi-physical-line import shapes it
                                                        previously left untouched -- a single-line
                                                        multi-module `import sys, os`; a parenthesized `from x
                                                        import (b, a,)` spanning several physical lines; and a
                                                        backslash-continued `import m, \` / `l, n`. All three
                                                        sort/group alongside ordinary single-module imports in
                                                        the same contiguous block. A per-name trailing comment
                                                        inside a parenthesized list (`from y import (z,  #
                                                        comment\n y,)`) is preserved verbatim positionally:
                                                        the group still sorts/moves as a whole but that
                                                        clause's own within-clause name order is left
                                                        untouched (comment presence disables only the internal
                                                        resort, per the safety guard described in
                                                        STATE_PYTHON3.md §3).

  py_single_statement_body_ext_inp/out.py            -- Python3 §8 extensions: joins a multi-physical-line
                                                        header and a multi-physical-line body without
                                                        flattening their internal layout; retains a body's
                                                        trailing comment; expands an over-limit compact body
                                                        to an indented block; and leaves a semicolon chain
                                                        untouched (never creates or extends one).

  py_control_flow_blank_line_gaps_inp/out.py         -- Python3 §9 gap fixes: blank line before a `return` in
                                                        a `def` whose own header spans multiple physical lines
                                                        (a wrapped parameter list); blank line before a
                                                        semicolon-chained statement whose LAST sub- statement
                                                        is `return` (§9.1) or whose preceding line's last
                                                        sub-statement is `return`/`break`/`continue` (§9.2);
                                                        blank line before `elif`/`else` when the immediately
                                                        preceding block is a §8-compact one-line header (`if n
                                                        > 0: return 1`) ending in an unconditional exit.

  py_decorator_overflow_inp/out.py                   -- Python3 §4 overflow: an over-`line-length` decorator
                                                        call's top-level argument list wraps one-per-line with
                                                        a trailing comma, closing `)` back at the `@` line's
                                                        indent (the general call-argument-overflow wrapping
                                                        mechanism §4 previously lacked). Covers: a plain
                                                        multi-arg overflow; an already-fitting call left
                                                        untouched; a bare `@dataclass`; a zero-arg
                                                        `@register()` call; the known-risk interaction with a
                                                        nested f-string field adjacent to another field with
                                                        no literal text between them

  py_signature_wrap_inp/out.py                       -- Python3 §6 overflow: the inline-vs-one-per-line
                                                        decision for a `def` signature (STYLE.md §8 as
                                                        directly referenced by §6), previously missing -- an
                                                        over- `line-length` inline signature wraps to
                                                        one-parameter- per-line with `:`/`=` column alignment
                                                        baked in (rendered the same way as an
                                                        already-broken-out signature's own alignment slice),
                                                        closing `)` back at the `def` line's indent, no
                                                        trailing comma after the last parameter, `->
                                                        ReturnType:` staying fixed on the closing `)`'s own
                                                        line. Covers: an already-fitting signature left
                                                        untouched; a plain overflow wrap; a
                                                        type-hint-plus-default parameter needing depth-tracked
                                                        comma splitting past a nested `Dict[str, int] = {}`
                                                        default; a return-type annotation case; and an
                                                        already- one-per-line signature
                                                        (adjacent-pass-ordering check -- picked up by the
                                                        pre-existing §6 alignment pass, not this new wrap
                                                        pass, since the two passes are mutually exclusive by
                                                        construction). (`f'Struct331_{signedness}{n}_...'`,
                                                        mirroring the §4/§5 idempotency bug already fixed for
                                                        this exact adjacency shape); a lambda-default argument
                                                        containing its own f-string field; and a trailing
                                                        same-line comment after the call, which disqualifies
                                                        the whole line from wrapping (documented gap, same
                                                        "comment disqualifies the candidate" posture as the
                                                        C-family's `enforceCallLineBreaking`).

Makefile/Bash/PowerShell:
  makefile_combined_inp/out.mk                       -- STYLE_TOOLING.md §1 combined: assignment-alignment
                                                        group (`=`/`:=`/`+=`), backslash continuation-line
                                                        alignment under the value column, target `:` spacing
                                                        (one space after `:`, single spaces between prereqs),
                                                        `ifdef`/`else`/`endif` body indentation, a leading-tab
                                                        recipe line left byte-identical, and a `#` comment
                                                        breaking an assignment-alignment group.

  bash_combined_inp/out.sh                           -- STYLE_TOOLING.md §2 combined: `if`/`then` same-line
                                                        merge, pipe spacing on lone `|`, function brace
                                                        placement with body indent, `case` arm/`;;`
                                                        formatting, arithmetic operator spacing inside
                                                        `$((...))`, plus safety cases proving pipes/arith in
                                                        strings and `#` comments, a heredoc body, and `$(...)`
                                                        command-substitution content are all left
                                                        byte-identical.

  powershell_combined_inp/out.ps1                    -- STYLE_TOOLING.md §3 combined: naive brace-depth
                                                        indent, operator spacing + `=` alignment, pipeline
                                                        split with right-aligned `|`, multi-line hashtable
                                                        entry alignment (single-line `@{...}` not expanded),
                                                        `switch` keyword-paren spacing + arm `{` alignment,
                                                        `{`/`}` spacing including single-line scriptblocks,
                                                        plus safety cases proving pipes in strings/`#`
                                                        comments and a here-string body are left
                                                        byte-identical.

General Scope-Depth Reindentation:
  curly_general_scope_reindent_inp/out.hpp           -- Proves `curly-general-scope-reindent=on` is accepted
                                                        as an in-file config key (JXM_CFMT_CFG) and produces a
                                                        correctly formatted result, not an error -- see
                                                        STATE_CURLY_GDR.md.

  curly_gdr_multipass_inp/out.java                   -- One-true-brace-style joined `} else if (...) {` / `}
                                                        else {` chain with multi-statement bodies, with BOTH
                                                        curly-general-scope-reindent=on AND
                                                        curly-general-scope-reindent-multipass=on via in-file
                                                        config -- isolates the confirmed RDD_KEY_229 root
                                                        cause (a single GDR pre-pass measures depth against
                                                        the joined line as it exists BEFORE brace-placement
                                                        splits it into separate Allman-style lines, so a
                                                        single GDR pass alone is non-idempotent on this exact
                                                        shape) and proves the 4-stage GDR/pipeline/GDR/
                                                        pipeline multipass sequence (RDD_KEY_233/RDD_KEY_234)
                                                        resolves it -- see STATE_CURLY_GDR.md's real-code
                                                        validation entries for the same fix confirmed at scale
                                                        against angular/angular, javaparser-core(-
                                                        generators), tool/JSONEncoderLite.java, and
                                                        serge-sans-paille/frozen.

  curly_gdr_js_regex_inp/out.ts                      -- JS/TS regex literal containing bracket-family
                                                        characters (`/[{]/`, `/[(]/`) with
                                                        curly-general-scope-reindent=on via in-file config --
                                                        proves GdrTokenizer (STATE_CURLY_GDR.md) correctly
                                                        tokenizes JS/TS regex literals as string-like units
                                                        (STRING) rather than plain TEXT, preventing regex
                                                        interior brackets from miscounting structural depth.

  java_flush_left_inp/out.java                       -- Every line of the input is flushed to column 0 (no
                                                        leading indentation at all), with
                                                        curly-general-scope-reindent=on via in-file config --
                                                        proves the GDR pre-pass (STATE_CURLY_GDR.md) reindents
                                                        correctly from a fully unindented starting point,
                                                        which the base pipeline's own relative-delta
                                                        reindentation cannot do on its own. Also covers the
                                                        PCPP-preprocessed Java pattern used in
                                                        `src/jxm/ugc/ARMCortexMThumbC.java.in` (a `.java.in`
                                                        file run through a C-macro preprocessor before
                                                        compilation, per README.md's "C-preprocessor
                                                        directives in Java source" note): a `#define`-style
                                                        function-like macro precedes a class and is invoked
                                                        with loosely-spaced call arguments
                                                        (`__GEN_CXI_NPR_NPR__( clrex, ... )`). Confirms the
                                                        `#define` line itself passes through untouched
                                                        (recognized/skipped like any other preprocessor
                                                        directive) while the macro invocation lines still get
                                                        normal call-padding tightening (`(clrex, ...)`) and
                                                        are idempotent.

  html_js_flush_left_inp/out.html                    -- Entire document flushed to column 0, multiple tags on
                                                        a single line (`<head>...</head>`, stacked
                                                        `<span>`/`<li>` siblings), and an embedded `<script>`
                                                        block whose JS body is also flushed to column 0, with
                                                        curly-general-scope-reindent=on via in-file config
                                                        (`<!--% JXM_CFMT_CFG ... -->`) -- proves the GDR
                                                        pre-pass reaches embedded JS through the HTML5
                                                        `<script>` dispatch
                                                        (`XmlSpecificRule.renderScriptOrStyle`) and correctly
                                                        reindents it from a fully unindented starting point,
                                                        while the surrounding HTML element nesting (not itself
                                                        GDR's concern) is handled by the base HTML5 pipeline
                                                        as usual. Also incidentally covers the `%`-prefixed
                                                        marker-comment convention: the directive comment
                                                        itself must render byte-for-byte unchanged, not gain a
                                                        corrupting inner space.

Multi-Sentence Comment Capitalization:
  multi_sentence_comment_inp/out.java                -- Proves `normalize-comment-start-case-multiline=on`
                                                        (curly family, via in-file config) capitalizes
                                                        sentence 2+ of a `//` comment group, not just sentence
                                                        1 -- see STATE_COMMON.md's "Multi-sentence comment
                                                        capitalization" section.

  multi_sentence_comment_inp/out.sh                  -- Same proof for the tooling `#`-comment family
                                                        (Makefile/Bash/PowerShell), via `#%` in-file config.

Real-code regressions:
  real_code_regressions_1_inp/out.cpp                -- Distilled from tinyexpr-plusplus: same-line-sibling
                                                        call-argument mis-split, an undercounted call "does it
                                                        fit" length check, and an
                                                        enforceComplexityPadding/enforceCallLineBreaking
                                                        pass-ordering idempotency bug.

  real_code_regressions_2_inp/out.java               -- Distilled from RobotCoding's gui_frontend: `>>>`
                                                        mis-tokenized as `>>`+`>`; GetterSetterRule padding
                                                        computed against stale pre-padding text;
                                                        enforceCallLineBreaking losing complexity-padding
                                                        awareness when joining a multi-line call; and a
                                                        getter/setter grouping decision that didn't predict a
                                                        later line-break, so a fresh format and a reformat
                                                        produced different output.

  real_code_regressions_3_inp/out.java               -- Distilled from dogfooding the formatter's own src/
                                                        tree: MiscRule's consecutive-assignment alignment
                                                        rejected an RHS already wrapped by a later pass,
                                                        splitting/shrinking the alignment group on a second
                                                        format instead of treating it as one group like a
                                                        fresh format does.

  real_code_regressions_4_inp/out.hpp                -- Distilled from martinus/nanobench: no tokenizer
                                                        support for C++11 raw string literals at all
                                                        (corrupting brace-depth tracking and truncating up to
                                                        ~46% of real files); raw-string support gated on a
                                                        C-only flag instead of C-or-C++; and
                                                        DeclarationAlignmentRule dropping a leading
                                                        `template<...>` prefix on bare forward declarations.

  real_code_regressions_5_inp/out.cpp                -- User-reported: a `while` loop's own closing `}` stayed
                                                        indented to its body instead of its frame. Fixed by
                                                        force-reindenting a scope's closing brace to the
                                                        frame's indent, with carve-outs for case/default-label
                                                        spans, a comment in the trailing gap, bare compound
                                                        blocks, and empty bodies.

  real_code_regressions_6_inp/out.java               -- Found via idempotency testing on google-java-format: a
                                                        trailing same-line closing comment (`} // if`) before
                                                        a case/default label was wrongly treated as a
                                                        *leading* comment, forcing a spurious blank line and
                                                        orphaning it. Fixed by only applying that exception to
                                                        comments that start their own line.

  real_code_regressions_7_inp/out.java               -- Found via idempotency testing on google-java-format:
                                                        arrow-form `case X -> body;` joins didn't check the
                                                        resulting line length, so a fresh format could produce
                                                        an over-length line that a reformat then broke apart.
                                                        Fixed by predicting the joined width before committing
                                                        to the join.

  real_code_regressions_8_inp/out.java               -- Found via idempotency testing on google-java-format:
                                                        the getter/setter one-liner pass misparsed arrow-form
                                                        `case`/`default` switch arms as accessor members,
                                                        injecting garbage column padding. Fixed by rejecting
                                                        any one-liner starting with `case`/`default`.

  real_code_regressions_9_inp/out.java               -- Found via idempotency testing on pcpp_java: switch
                                                        inline-alignment padded a short label to match a wider
                                                        sibling without checking the row's final length,
                                                        producing an unstable over-length line. Fixed by
                                                        predicting every row's rendered length before padding.

  real_code_regressions_10_inp/out.java              -- Found via idempotency testing on pcpp_java:
                                                        one-liner-body detection used a raw newline check that
                                                        could be fooled by a call already broken across lines
                                                        by an earlier pass, wrongly recursing into and
                                                        corrupting an already-correct one-liner body. Fixed
                                                        with a paren/bracket-depth-aware scan.

  real_code_regressions_11_inp/out.c                 -- Found via idempotency/round-trip testing on
                                                        tongsuo-mini: a flat aggregate initializer (e.g. a
                                                        large S-box table) collapsed to one line with no
                                                        length check, producing lines thousands of characters
                                                        long. Fixed by rejecting the collapse when it would
                                                        exceed the line limit, plus normalizing the closing
                                                        `}` of oversized multi-line initializers onto its own
                                                        line.

  real_code_regressions_12_inp/out.hpp               -- Found via idempotency testing on
                                                        serge-sans-paille/frozen's catch.hpp: a struct with a
                                                        virtual destructor, a long pure-virtual signature, and
                                                        a template method-with-body was corrupted on the first
                                                        pass -- members merged, a stray space on
                                                        `~ClassName()`, and `};` accumulating extra semicolons
                                                        each pass. Two depth-tracking bugs in
                                                        DeclarationAlignmentRule.parseDeclaration, both fixed.

  real_code_regressions_13_inp/out.hpp               -- Found via idempotency testing on the same catch.hpp: a
                                                        nested `for` loop came out of the first pass with
                                                        corrupted indentation. Root cause: the
                                                        preprocessor-directive tokenizer had no
                                                        backslash-line-continuation handling, desyncing the
                                                        brace-depth counter for the rest of the file. Fixed by
                                                        adding the same continuation handling its sibling
                                                        already had.

  real_code_regressions_14_inp/out.hpp               -- Minimal repro for a general indent fix: a construct
                                                        sharing its opening line with a parent `{`, nested
                                                        inside a namespace, came out under-indented by one
                                                        level because the indent fallback used a depth counter
                                                        that doesn't increment for namespace bodies. Fixed by
                                                        threading a real accumulated-indent string through the
                                                        recursion instead of guessing from depth.

  real_code_regressions_15_inp/out.hpp               -- Minimal repro for a tokenizer fix: catch.hpp's
                                                        Objective-C interop block has genuine `[[NSString
                                                        alloc] init]` message sends, which the tokenizer used
                                                        to merge into a C++17-attribute-style `[[...]]` token
                                                        regardless of context, desyncing bracket-depth for the
                                                        rest of the file. Fixed by only merging `[[` when a
                                                        forward scan confirms a genuine attribute-shaped
                                                        close.

  real_code_regressions_16_inp/out.hpp               -- Covers 4 unrelated idempotency bugs surfaced once
                                                        fixture 15's fix stopped masking them, all in
                                                        ScopePipeline/TokenizerCore: an off-by-one in the
                                                        namespace-detection scan; a constructor's
                                                        member-initializer list being mistaken for its own
                                                        signature body (corrupting spacing, and missing a
                                                        trailing-length check); and an elaborated-type
                                                        declaration with an empty initializer (`struct
                                                        sigaction sa = { };`) misdetected as a struct body,
                                                        appending an extra `;` on every pass.

  real_code_regressions_17_inp/out.kt                -- Kotlin dogfood find (RobotTcpSession.kt):
                                                        enforceCallLineBreaking's per-argument-line grouping
                                                        (Option 2) collapsed a multi-line lambda-body sibling
                                                        argument onto one line, merging statements with no `;`
                                                        separator -- invalid Kotlin. Fixed by bailing (Kotlin-
                                                        only) when a top-level argument mixes a newline and
                                                        `{`.

  real_code_regressions_18_inp/out.kt                -- Kotlin idempotency (PlayMusicBlock.kt):
                                                        KotlinDeclarationAlignmentRule.spansMultipleLines
                                                        treated a braceless if/else initializer as multi-line
                                                        once a nested call got wrapped by a later pass,
                                                        shrinking the sibling val's alignment on each
                                                        reformat. Fixed with paren/brace-depth-aware newline
                                                        tracking so only real `{`...`}` bodies count as
                                                        multi-line.

  real_code_regressions_19_inp/out.kt                -- Kotlin indent drift (MainActivity.kt):
                                                        ScopePipeline.processScope derived a trailing-lambda
                                                        body's indent/closing-brace from the statement's first
                                                        line instead of the `{`'s own (deeper) physical line,
                                                        under-indenting the body on a fresh format. Fixed via
                                                        a new braceLineIndent helper (Kotlin-only).

  real_code_regressions_20_inp/out.kt                -- Kotlin compile-break
                                                        (ToolbarActions.kt/MainViewModel.kt):
                                                        collapseSingleExpressionBlocks has no expression- vs
                                                        statement-position `if` distinction, so it swallowed
                                                        the newline after a parenthesized `(if (cond) a else
                                                        b)` initializer, fusing it with the next statement.
                                                        Fixed by tracking unmatched-paren depth and refusing
                                                        to collapse `if`/`else` while inside one.

  real_code_regressions_21_inp/out.kt                -- Kotlin spacing bug (BlockCanvasView.kt):
                                                        isTightToken's `&`-repeat check (for C/C++ `&&`
                                                        rvalue-ref sigils) also matched Kotlin's `&&`
                                                        logical-AND, dropping the space before it. Fixed by
                                                        gating the check to non-Kotlin languages, mirroring
                                                        MiscRule's existing gate.

  real_code_regressions_22_inp/out.kt                -- Kotlin idempotency (BlockPalette.kt): a one-liner-
                                                        function group's column width was computed from
                                                        pre-wrap text, but a later pass wrapped a too-long
                                                        member's call, leaving stale padding on reformat.
                                                        Fixed by porting the C/Java `hasBreakableCall` +
                                                        estimated-width pre-check to
                                                        KotlinGetterSetterRule.parseKotlinOneLinerMember.

  real_code_regressions_23_inp/out.kt                -- Kotlin idempotency (BlockPalette.kt):
                                                        KotlinSpecificRule.isSingleLineBody kept K&R `{` for a
                                                        body that was pre-wrap one-line but got split by a
                                                        later call-wrapping pass, flipping to Allman on
                                                        reformat. Fixed by porting the same hasBreakableCall +
                                                        estimated- width pre-check, with a corrected width
                                                        formula that now accounts for indentation and spacing.

  real_code_regressions_24_inp/out.kt                -- Kotlin compile-break
                                                        (ConnectTypeDialog.kt/WifiApDialog.kt):
                                                        findLastTopLevelCloseParen accepted any last depth-0
                                                        `)` as a signature's param list even with no `:`
                                                        following, so `x.foo().bar { ... }` misdetected `bar`
                                                        as a return- type tail and silently deleted `.bar`.
                                                        Fixed by requiring a top-level `:` immediately after
                                                        the `)` before accepting the Kotlin return-type-tail
                                                        branch.

  real_code_regressions_25_inp/out.kt                -- Kotlin compile-break
                                                        (BlockCanvasView.kt/ToolbarActions.kt):
                                                        enforceLabeledJumpSpacing's label-detection state
                                                        machine couldn't tell a genuine `label@` from an
                                                        unrelated `@Annotation`, corrupting
                                                        `@JvmOverloads`/`@Volatile` into `@ JvmOverloads`/`@
                                                        Volatile`. Fixed with an isLoopLabelTarget lookahead
                                                        requiring `for`/`while`/`do`/`{` after `@`.

  real_code_regressions_26_inp/out.kt                -- Kotlin compile-break (Optimizer.kt):
                                                        collapseSingleExpressionBlocks's bare-`else` handling
                                                        also matched a `when` arm's `else ->` label (no brace,
                                                        since its body follows `->`), flattening a multi-
                                                        statement block onto one line with no `;` separators.
                                                        Fixed by checking for a following `->` and bailing out
                                                        of the braceless-collapse path.

  real_code_regressions_27_inp/out.kt                -- Kotlin, two co-occurring bugs (ProgramBuilder.kt): (1)
                                                        needsSpaceBetween had no case for `!is`/`!in`,
                                                        corrupting them into `! is`/`! in`; (2)
                                                        enforceCallLineBreaking's renderCallCandidate used the
                                                        C/Java-style parseSignature on a call argument
                                                        (`it.func.funcName`), misparsing it as `Type name` and
                                                        inserting a spurious space once wrapped. Fixed by
                                                        adding `!is`/`!in` as tight tokens and routing Kotlin
                                                        calls through a separate untyped sigForRender path.

  real_code_regressions_28_inp/out.hpp               -- C++, real-code test against taocpp/PEGTL
                                                        (rematch_input.hpp): reclassifyAngleBrackets'
                                                        single-open-`<` branch retyped a literal `>>` token
                                                        via `retype()` (preserving its 2-char text) while also
                                                        appending a new 1-char `>` token, duplicating a
                                                        character on the first format pass and breaking a
                                                        `template<...>` forward declaration. Fixed by giving
                                                        the retyped token its own explicit 1-char text.

  real_code_regressions_29_inp/out.java              -- Java, real-code test against local `anemonesoft`
                                                        candidate (HelpBox.java/Spreadsheet.java):
                                                        renderCallCandidate's groupByOriginalLine tracks only
                                                        paren/bracket depth, not brace depth, so a call's
                                                        multi-line brace-body trailing argument got silently
                                                        swallowed into one unbounded output line, flapping to
                                                        an idempotency failure once a later pass reacted to
                                                        the now-multi-line body. Fixed by widening an existing
                                                        Kotlin-only "leave such an argument untouched" bail to
                                                        all languages via a new containsInternalNewline check.

  real_code_regressions_30_inp/out.kt                -- Kotlin, real-code test against `square/okio`: three
                                                        co-occurring bugs. (1) renderTokens had no unary-vs-
                                                        binary `-`/`+` notion, corrupting `val x = -1` into `=
                                                        - 1`; fixed with an isUnaryMinusOperand lookback. (2)
                                                        applySignaturePass's `: ReturnType` tail detection
                                                        merged a headerless declaration with an unrelated
                                                        later one across a blank line; fixed with a
                                                        hasTopLevelBlankLine guard. (3) braceless if/while/for
                                                        collapse rendered a stale, untightened keyword-paren
                                                        space, causing enforceCallLineBreaking to over-wrap a
                                                        line that fits at its true final width; fixed by
                                                        tightening the space at collapse time.

  real_code_regressions_31_inp/out.kt                -- Kotlin, two compile-breaking bugs found via `kotlinc`
                                                        against `square/okio` (not caught by idempotency
                                                        diffing). (1) MULTI_CHAR_OPS was missing `===`/`!==`,
                                                        so `!==` lexed as separate tokens and got re-spaced
                                                        into invalid `!= =`; fixed by adding both operators.
                                                        (2) the braceless-collapse dispatch treated a
                                                        do-while's trailing `while (cond)` as a loop-starting
                                                        `while`, fusing the next statement onto the same line;
                                                        fixed with an isDoWhileTailKeyword lookback.

  real_code_regressions_32_inp/out.kt                -- Kotlin, real-code test against `square/kotlinpoet`: a
                                                        nested `when { ... }` used as a `when` branch's body
                                                        flapped its closing `}`'s indentation
                                                        round1-vs-round2, since Kotlin's braceLineIndent
                                                        anchors on the brace's pre-merge physical line at
                                                        Phase 0 but formatWhenExpressions' Phase 4
                                                        arrow-alignment pass later merges the branch label
                                                        onto that same line. Fixed with a
                                                        findMergingWhenBranchLineStart lookahead that anchors
                                                        on the eventual post-merge line up front.

  real_code_regressions_33_inp/out.kt                -- Kotlin, real-code test against `square/kotlinpoet`: a
                                                        first-pass compile-breaking bug found via `kotlinc` --
                                                        an expression-bodied function whose body is itself a
                                                        trailing-lambda call (`fun addTypes(...): T = apply {
                                                        ... } as T`) had `apply`'s own unrelated `{` wrongly
                                                        Allman-converted as the function's own body brace.
                                                        Root cause: findSignatureCloseParenBeforeBrace's
                                                        backward scan for `: ReturnType` had no bail-out on an
                                                        intervening depth-0 `=`. Fixed with that bail-out.

  real_code_regressions_34_inp/out.hpp               -- C++, real-code test against `NVIDIA/stdexec`: combines
                                                        two bugs. (1) A requires-expression
                                                        compound-requirement's inner `}` (followed by `->`,
                                                        not `;`) was misidentified by splitTopLevelSpans as a
                                                        scope-closing brace, corrupting indentation
                                                        non-idempotently; fixed by also checking for a
                                                        following `->`. (2) Compile-breaking: semicolon-less
                                                        macro-invocation statements before a `#if`/`#endif`
                                                        guard caused splitStatements to never close the
                                                        current statement, silently dropping the `#if` and
                                                        cascading 150+ downstream errors; fixed with a depth-0
                                                        check that always closes the statement at a
                                                        preprocessor token.

  real_code_regressions_35_inp/out.hpp               -- C++, real-code test against `NVIDIA/stdexec`
                                                        (continuing the candidate above): a compile-breaking
                                                        bug ("Bug 3") in `__counting_scopes.hpp` --
                                                        tryCollapse's renderInline flattened a multi-line `if`
                                                        condition containing a `//` comment between call
                                                        arguments, silently absorbing every following token
                                                        (including the closing `}`) into the comment and
                                                        producing a 50-error unmatched-brace cascade. Fixed
                                                        with a containsLineComment guard that refuses the
                                                        collapse when the condition carries a line comment.

  real_code_regressions_36_inp/out.cpp               -- C++, real-code test against `NVIDIA/stdexec`
                                                        (continuing the candidate above): the last remaining
                                                        idempotency flap -- parseDeclaration had no guard
                                                        rejecting an already-collapsed one-liner `if`/`while`/
                                                        `for`/`switch`/`do`/`else` statement (produced by
                                                        STYLE.md §10/§11's collapse) as a candidate
                                                        declaration, so on a second pass it misparsed one as a
                                                        bogus `Declaration` and padded a neighboring real
                                                        declaration's column. Fixed by rejecting those six
                                                        leading keywords, mirroring the existing `case`/
                                                        `default` guard; confirmed via full-tree round1/round2
                                                        diffing this was the only remaining divergence,
                                                        marking the stdexec candidate DONE.

  real_code_regressions_37_inp/out.kt                -- Kotlin, real-code test against
                                                        `Kotlin/kotlinx.coroutines`: an expression-bodied
                                                        function's unconsumed `{`-led lambda tail made
                                                        `renderWithTail` bake a trailing space onto `= `
                                                        regardless, growing the gap by one space per reformat.
                                                        Fixed by omitting the space when `exprTokens` is
                                                        empty.

  real_code_regressions_38_inp/out.kt                -- Kotlin, real-code test against
                                                        `Kotlin/kotlinx.coroutines`: a KDoc's own nested `/*
                                                        ... */` snippet closed the outer `/**` doc-comment
                                                        early, mis-lexing and silently truncating the rest of
                                                        the file (`Guidance.kt`, ~330 lines dropped). Fixed by
                                                        tracking block-comment nesting depth, Kotlin-only.

  real_code_regressions_39_inp/out.kt                -- Kotlin, real-code test against
                                                        `Kotlin/kotlinx.coroutines`: `this@Label` got a stray
                                                        space inserted before `@` (`this @Label`, a real
                                                        syntax error) since `enforceLabeledJumpSpacing`'s
                                                        state machine didn't recognize `this` before `@`.
                                                        Fixed with a new state pair tightening `this@Label`.

  real_code_regressions_40_inp/out.kt                -- Kotlin, real-code test against
                                                        `Kotlin/kotlinx.coroutines`: `LimitedDispatcher.kt`'s
                                                        collapsible `while (true) { when (...) { ... } }` body
                                                        owned a nested multi-line `synchronized(...) { ... }`
                                                        block; `tryCollapse`'s brace-depth-unaware flattening
                                                        fused its statements onto one line with no separators
                                                        -- a real syntax error. Fixed with a
                                                        `containsMultilineNestedBrace` bail in
                                                        `isKotlinSingleStatementBody`.

  real_code_regressions_41_inp/out.kt                -- Kotlin, real-code idempotency test against
                                                        `Kotlin/kotlinx.coroutines`: `SystemProps.kt`'s
                                                        chained `catch` span kept its stale pre-merge indent
                                                        once `KotlinSignatureRule` merged the `try` signature
                                                        onto one line, disagreeing with the `try` span's
                                                        re-derived indent round1 vs round2. Fixed by having a
                                                        chained `catch`/`finally` span inherit its preceding
                                                        span's resolved indent.

  real_code_regressions_42_inp/out.kt                -- Kotlin, real-code idempotency test against
                                                        `Kotlin/kotlinx.coroutines`: a class with a wrapped
                                                        multi-line generic `where` clause had its closing
                                                        brace/comment indent drift deeper, since
                                                        `effectiveSpanIndent` preferred the deeper
                                                        continuation-line `braceIndent` over the header's own
                                                        `spanIndent` (correct for unnamed lambda bodies,
                                                        RDD_KEY_136, but wrong for named class/fun/object
                                                        scopes). Fixed by gating `braceIndent` off for named
                                                        scopes.

  real_code_regressions_43_inp/out.kt                -- Kotlin, real-code idempotency test against
                                                        `Kotlin/kotlinx.coroutines`: a wrapped multi-argument
                                                        call used as a keyword-less `when` branch body had its
                                                        continuation lines one level deeper on round1 than
                                                        round2, since `enforceCallLineBreaking` computed the
                                                        base indent before the branch label/body got merged
                                                        onto one line by a later phase. Fixed with
                                                        `effectiveCallBaseIndent`, which uses the preceding
                                                        `->` line's indent when present.

  real_code_regressions_44_inp/out.kt                -- Kotlin, real-code idempotency test against
                                                        `Kotlin/kotlinx.coroutines`: a nested-lambda-chain's
                                                        closing `}` drifted from col 4 to col 8 on round2.
                                                        `findParentIndent`'s backward scan could anchor on a
                                                        dangling braceless `else expr` (left as leading text
                                                        at the start of the next span by
                                                        `splitTopLevelSpans`), returning a wrong, unrelated
                                                        line's indent. Fixed by skipping forward past a
                                                        dangling `else`/`catch`/ `finally` anchor to the next
                                                        real statement.

  real_code_regressions_45_inp/out.kt                -- Kotlin, real-code idempotency test against
                                                        `Kotlin/kotlinx.coroutines`: a `val` alignment group
                                                        padded a typeless row to match a sibling's type-column
                                                        width, widening that sibling's line enough to trigger
                                                        a lambda-initializer wrap on the next pass, which then
                                                        correctly bailed it out of the group -- an idempotency
                                                        flap. Fixed by making `renderAlignedGroup`
                                                        budget-aware: a row is excluded from the shared column
                                                        grid up front when its own brace-bodied initializer
                                                        would overflow the line-length budget once padded.

  real_code_regressions_46_inp/out.kt                -- Kotlin, real-code idempotency test against
                                                        `square/kotlinpoet`'s Shape 1 idempotency-gap group (6
                                                        files), two bugs in `enforceCallLineBreaking`. Bug A:
                                                        a wrapped signature with a trailing `= apply { ... }`
                                                        body re-collapsed on reformat because `lineEndIndex`'s
                                                        width check stopped at the first NEWLINE,
                                                        undercounting width when the tail's own nested call
                                                        was already wrapped from a previous round; fixed with
                                                        a depth-aware `effectiveLineEndIndex` that skips
                                                        NEWLINEs still inside an unclosed bracket. Bug B
                                                        (RDD_KEY_149, now root-caused): a signature with an
                                                        explicit `: ReturnType {` block body got its correctly
                                                        wrapped, padded param list re-wrapped as a plain call,
                                                        discarding padding/trailing comma, since the "is this
                                                        a call" exemption only recognized `{` right after `)`.
                                                        Fixed with an `isKotlinReturnTypeThenBlockBody`
                                                        lookahead.

  real_code_regressions_47_inp/out.kt                -- Kotlin, real-code idempotency test against
                                                        `square/kotlinpoet`'s Shape 2
                                                        (`AbstractTypesTest.kt`): a multi-line generic `where`
                                                        clause gained one extra indent level every round,
                                                        since `enforceWhereClausePlacement` derived the base
                                                        indent from `where`'s own (already-wrapped) physical
                                                        line instead of the true signature line. Fixed with a
                                                        `signatureLineIndent` helper that scans backward to
                                                        the nearest depth-0 `;`/`}`/`{`.

  real_code_regressions_48_inp/out.kt                -- Kotlin, real-code idempotency test against
                                                        `square/kotlinpoet`'s Shape 3: a `when` branch's
                                                        multi-line body (nested `when(subject) { ... }` or a
                                                        trailing-lambda call) had its closing `}` sit 2 spaces
                                                        shallower on round2, since
                                                        `findMergingWhenBranchLineStart` (RDD_KEY_152) only
                                                        recognized a bare `when {` as the merging shape. Fixed
                                                        by generalizing the lookahead to accept a
                                                        parenthesized `when` subject or a plain call-head
                                                        identifier.

  real_code_regressions_49_inp/out.kt                -- Kotlin, real-code idempotency test against
                                                        `square/kotlinpoet`'s Shape 4: a `val` declaration's
                                                        alignment padding flapped between rounds because a
                                                        preceding sibling's `Foo::class` reflection literal
                                                        wrongly armed `namedConstructKeywordSeen` (which only
                                                        checks for the `class` KEYWORD, not a real class
                                                        declaration), corrupting a later unrelated scope's
                                                        name tracking. Fixed by never arming on a `class`
                                                        KEYWORD preceded by `::`. Also fixed two side bugs
                                                        found while root-causing this: an
                                                        `ArrayIndexOutOfBoundsException` in
                                                        `signatureLineIndent` (RDD_KEY_164) for a
                                                        `where`-clause statement with no preceding boundary
                                                        token, and a boundary-anchoring correction matching
                                                        `real_code_regressions_47`'s original fix intent.

  real_code_regressions_50_inp/out.cpp               -- C++, real-code test against `ericniebler/range-v3`'s
                                                        concept-emulation-macro convention
                                                        (`template(...)`/`CPP_ret`/`CPP_member`, see
                                                        `detail/prologue.hpp`): two compile-breaking bugs. (1)
                                                        `extendOverLeadingRequiresAndTemplate` pulled a
                                                        preceding `template(...)`-macro invocation onto a
                                                        declarator's line without checking for a following
                                                        `<`, gluing a `requires`-line's trailing `//` comment
                                                        onto the declarator and commenting it out; fixed by
                                                        requiring `<` before the pull and refusing to pull a
                                                        `requires` line ending in a `//` comment. (2)
                                                        `enforceEmptyParameterList`'s `IDENTIFIER(void)` ->
                                                        `IDENTIFIER()` rewrite fired on the macro invocation
                                                        `CPP_ret(void)(requires ...)`, deleting an argument
                                                        the macro needs; fixed by never rewriting `(void)`
                                                        when the matching `)` is immediately followed by
                                                        another `(`. Verified with `g++ -std=c++20
                                                        -fsyntax-only` and full round1/round2 idempotency over
                                                        range-v3's 311-file tree.

  real_code_regressions_51_inp/out.cpp               -- C++, follow-up to `_50`: another range-v3
                                                        compile-breaking bug in `view/view.hpp`/
                                                        `action/action.hpp`. A `;`-terminated declaration
                                                        whose source spans multiple `//`-commented lines (a
                                                        `CPP_broken_friend_ret(Rng)(requires ...) = delete;`
                                                        deleted-overload) got collapsed onto one line,
                                                        swallowing the tail into the first comment, because
                                                        `parseDeclaration`'s function-pointer-detection branch
                                                        misfired on the macro call (syntactically identical to
                                                        `(name)(params)`) and its raw token capture preserved
                                                        `//` comments that later got flattened. Fixed with a
                                                        narrow guard: skip that branch when a `COMMENT_LINE`
                                                        token is present, falling through to the generic path.
                                                        Verified with `g++ -std=c++20 -fsyntax-only` and full
                                                        round1/round2 idempotency over range-v3's 318-file
                                                        tree.

  real_code_regressions_52_inp/out.cpp               -- C++, boost-ext/ut idempotency bug: a C++20
                                                        deduction-guide statement immediately followed by an
                                                        unrelated `struct suite { ... };` caused
                                                        `enforceFunctionDefinitionAllmanBraceStyle`'s backward
                                                        close-paren scan to cross the `;` boundary and
                                                        misidentify the deduction guide's close-paren as the
                                                        struct's own, Allman-converting its brace with a bogus
                                                        indent that a later K&R re-collapse then joined back
                                                        -- a stable round1-vs-round2 diff. Fixed by making
                                                        both backward close-paren scans stop at a depth-0 `;`,
                                                        not just `{`/`}`. Verified with a minimal repro and
                                                        full `make test` (71/71, no regressions).

  real_code_regressions_53_inp/out.cpp               -- C++, microsoft/proxy: 3 bugs in
                                                        `CppSpecificRule.enforceRequiresClausePlacement`. (a)
                                                        baseIndent/fit-check anchored on the trailing
                                                        `requires` clause's preceding `)`, unstable across
                                                        passes when that `)` sits on a continuation-alignment
                                                        or dedented line; fixed by deriving from the parameter
                                                        list's own opening paren instead, unwinding any
                                                        chained trailing specifier (e.g. `noexcept(...)`).
                                                        (b)/(c) a preprocessor directive inside the clause's
                                                        constraint expression got spliced mid-line by
                                                        `collapseToOneLine`, producing invalid C++; fixed by
                                                        leaving any clause containing a `PREPROCESSOR` token
                                                        untouched. Verified with `clang++ -std=c++23
                                                        -fsyntax-only` and full round1/round2 idempotency over
                                                        `microsoft/proxy`.

  real_code_regressions_54_inp/out.java              -- Java, javaparser/javaparser real-code testing: 2 bugs.
                                                        (a) `GetterSetterRule.parseOneLinerMember` misparsed a
                                                        braceless `if (cond) throw new X(...)`/ `if (cond)
                                                        return ...` as a one-liner getter/setter,
                                                        grid-aligning bogus padding that grew unboundedly
                                                        across passes; fixed by rejecting any candidate whose
                                                        "return type" span contains a control-flow keyword.
                                                        (b) `MiscRule.stripSoleTrailingPeriod`/
                                                        `stripSoleTrailingPeriodAcrossLines` stripped a
                                                        comment's sole trailing `.` but left the preceding
                                                        whitespace, a stray-space idempotency bug; fixed by
                                                        trimming trailing whitespace in both methods. Verified
                                                        with minimal repros and full `make test`.

  real_code_regressions_55_inp/out.java              -- Java, javaparser/javaparser real-code testing
                                                        (continued): `ScopePipeline.processScope`'s
                                                        force-reindent of a span's trailing gap collapsed a
                                                        deliberate blank source line once `findParentIndent`
                                                        started returning a real indent for a chained
                                                        `else`/`catch`/`finally` that moved to its own line
                                                        (Allman) -- an idempotency bug (round1 K&R vs. round2
                                                        Allman), found in `TypeExtractor.java`. Fixed by
                                                        counting the trailing whitespace run's own newline
                                                        count (`trailingRunNewlineCount`) and replaying that
                                                        many newlines instead of always forcing one. Verified:
                                                        minimal repro, both real `TypeExtractor.java` copies
                                                        round1/round2 byte-identical, full `make test` 78/78.

  real_code_regressions_56_inp/out.java              -- Java, javaparser/javaparser real-code testing
                                                        (continued): `Formatter.formatOne`'s Phase 1 ran
                                                        `MiscRule.enforceCallLineBreaking`'s fit measurement
                                                        BEFORE `SwitchRule.formatNonInlineSwitches` reindents
                                                        switch-case bodies one level deeper, so a boundary
                                                        call measured "fits" against the pre-reindent column
                                                        and stayed unwrapped -- stable only on a second pass.
                                                        Found in `CsmAttribute.java`'s `getTokenType`. Fixed
                                                        by re-running `enforceCallLineBreaking` again right
                                                        after `formatNonInlineSwitches` (idempotent no-op
                                                        otherwise). Verified: minimal repro, real
                                                        `CsmAttribute.java` round1/round2 byte-identical, both
                                                        `TypeExtractor.java` copies still byte-identical (no
                                                        regression on bug 4's fix), full `make test` 79/79.

  real_code_regressions_57_inp/out.java              -- Java, javaparser/javaparser real-code testing
                                                        (continued):
                                                        `DeclarationAlignmentRule.isCStyleCastClose`
                                                        misclassified a braceless control-flow condition's own
                                                        closing paren (`if(node instanceof
                                                        RecordPatternExpr)`) as a C-style cast close, because
                                                        its guard excluded IDENTIFIER/`)`/`]` before the
                                                        matching `(` but not control-flow KEYWORD tokens,
                                                        suppressing a required space when the construct was
                                                        rendered as a declaration's initializer via
                                                        `renderInitTokens`. Fixed by adding a
                                                        `CONTROL_FLOW_KEYWORDS` exclusion set to
                                                        `isCStyleCastClose`. Found in `Java1_0Validator.java`/
                                                        `Java5Validator.java`. Verified: minimal repro, both
                                                        real files round1/round2 byte-identical, full `make
                                                        test`.

  real_code_regressions_58_inp/out.java              -- Java, javaparser/javaparser real-code testing
                                                        (continued): a Java enum constant list
                                                        (`BEGIN_TOKEN("beginToken"), END_TOKEN("endToken");`)
                                                        shares its top-level shape with a comma-separated
                                                        C-style multi-declarator statement, so
                                                        `DeclarationAlignmentRule.parseDeclaration` could
                                                        merge it into an unrelated adjacent field's alignment
                                                        group, and
                                                        `JavaSpecificRule.findEnumConstantListTerminators`
                                                        derived its re-emitted indent from the first member's
                                                        own current (possibly drifted) line indent instead of
                                                        an absolute recompute, compounding drift each pass.
                                                        Fixed by (a) isolating a Java enum-constant-list
                                                        statement into its own singleton group in
                                                        `DeclarationAlignmentRule.groupDeclarations` (new
                                                        `isJavaEnumConstantListShape` helper), and (b)
                                                        deriving `findEnumConstantListTerminators`'s indent
                                                        from the enum body's own stable `{`-line indent plus
                                                        one indent unit. Found in
                                                        `JavaParserJsonSerializer.java`. Verified: minimal
                                                        repro, real file round1/round2 byte-identical, full
                                                        `make test`.

  real_code_regressions_59_inp/out.kt                -- Kotlin, arrow-kt/arrow real-code testing: a generic
                                                        bound's `:` (e.g. `<A : Comparable<A>>`) wasn't
                                                        recognized as generic-safe by
                                                        `TokenizerCore.isGenericSafeToken`, invalidating the
                                                        angle-bracket tracking stack so a second bound's `>>`
                                                        stayed unsplit, corrupting both bounds' spacing. Fixed
                                                        by adding a Kotlin-gated `:` case to
                                                        `isGenericSafeToken`'s `OP` branch. Found in
                                                        `arrow-core`'s `Pair.kt` (`compareTo` extension).
                                                        Verified: minimal repro, full `make test`.

  real_code_regressions_60_inp/out.kt                -- Kotlin, arrow-kt/arrow real-code testing (found via
                                                        `kotlin_syntax_check` compile-checking round1's
                                                        output, not round1/round2 diffing):
                                                        `BlockStructureRule .isKotlinSingleStatementBody` let
                                                        a braced `if` body whose sole statement was a
                                                        `val`/`var` declaration collapse to braceless form
                                                        (`if (x) val y = ...`), which is illegal Kotlin. Fixed
                                                        by disqualifying a body whose first token is
                                                        `val`/`var` from collapse, same as
                                                        `COMPOUND_BODY_KEYWORDS` does for nested compound
                                                        bodies. Found in `RaiseAccumulate.kt`'s `addErrors`.
                                                        Verified: minimal repro, full `make test`.

  real_code_regressions_61_inp/out.kt                -- Kotlin, arrow-kt/arrow real-code testing (also found
                                                        via `kotlin_syntax_check`):
                                                        `MiscRule.needsSpaceBetween` had no tight-after case
                                                        for a Kotlin annotation's `@` when it shares its
                                                        source line with the function signature (rendered
                                                        through `MiscRule.renderTokens`'s shared join point,
                                                        used by `KotlinSignatureRule`); the default
                                                        space-insert fallback produced invalid `@ RaiseDSL`.
                                                        Fixed by adding a Kotlin-gated tight-after case for
                                                        `@`. Kotlin's other `@`-uses (`return@label`,
                                                        `label@`, `this@Label`) go through a separate rule
                                                        (`KotlinSpecificRule .enforceLabeledJumpSpacing`) and
                                                        are unaffected. Found in `RaiseAccumulateContext.kt`'s
                                                        `mapOrAccumulate`. Verified: minimal repro, full `make
                                                        test`.

  real_code_regressions_62_inp/out.kt                -- Kotlin, arrow-kt/arrow real-code testing: two
                                                        idempotency bugs deferred by RDD_KEY_173. (A)
                                                        RDD_KEY_174 --
                                                        `KotlinSignatureRule.parseKotlinSignature`'s first
                                                        `IDENTIFIER (` scan mistook a leading `context(raise:
                                                        Raise<Error>)` clause's paren for the real parameter
                                                        list when both shared one line, bailing instead of
                                                        continuing the scan (`RaiseContext.kt`'s
                                                        `ensureNotNull`). (B) RDD_KEY_175 -- `Formatter.java`
                                                        ran `formatWhenExpressions` after `addClosingComments`
                                                        had already counted `closing-comment-min-lines`
                                                        against the enclosing `for` loop, dropping its `//
                                                        for` comment on a fresh format; fixed by reordering
                                                        the passes (`Iterable.kt`'s `separateEither`).
                                                        Verified: minimal repro + both real files
                                                        round1/round2 byte-identical + full `make test`.

  real_code_regressions_63_inp/out.kt                -- Kotlin, arrow-kt/arrow real-code testing: RDD_KEY_176
                                                        -- `BlockStructureRule.collapseBracelessBody`'s
                                                        bare-`else`/ braceless-`if` body scan never checked
                                                        whether the body was a single statement once it could
                                                        itself own a multi-line `{...}` block (e.g. a
                                                        trailing-lambda call); `renderInline` fused the
                                                        block's internal statements with no `;` separator, a
                                                        genuine compile error. Fixed by reusing
                                                        `containsMultilineNestedBrace` as a bail-out guard.
                                                        Found in `Either.kt`'s `zipOrAccumulate`. Verified:
                                                        `kotlin_syntax_check` on `Either.kt` (18 errors -> 0),
                                                        full `make test`.

  real_code_regressions_64_inp/out.kt                -- Kotlin, arrow-kt/arrow real-code testing: RDD_KEY_177,
                                                        closing item of the investigation. Pure idempotency
                                                        flap in `Comparison.kt`'s `sort2`:
                                                        `collapseSingleExpressionBlocks`'s
                                                        `isKotlinExpressionIf` exemption only covered a
                                                        parenthesized expression-position `if`, not an
                                                        unparenthesized depth-0 if-expression used as an
                                                        entire expression-bodied function's whole body, so a
                                                        fresh format and a reformat of already-wrapped output
                                                        converged to two different stable states.

  real_code_regressions_65_inp/out.java              -- Java, local `src/jxm` real-code testing: two
                                                        idempotency bugs combined in one fixture (RDD_KEY_171,
                                                        RDD_KEY_172). (1)
                                                        `TokenizerCore.reclassifyAngleBrackets` had no case
                                                        for a literal `>>>` token (triple-nested generics);
                                                        round2 re-lexed round1's tight `>>>` as one token and
                                                        mis-spaced the generics. Fixed by adding an explicit
                                                        `>>>` case generalizing the existing `>>` split to 3
                                                        nesting levels. (2)
                                                        `JavaSpecificRule.isSingleLineBody`'s fits-under-limit
                                                        prediction omitted leading indentation and any
                                                        trailing same-line `//` comment, causing a
                                                        K&R-vs-Allman flip-flop across rounds. Fixed by
                                                        including both in the measurement,
                                                        whitespace-collapsed like `collapseToOneLine`.

  real_code_regressions_66_inp/out.java              -- Java, local `src/jxm` real-code testing: RDD_KEY_178,
                                                        two bugs in STYLE.md §8's multi-line parameter-list
                                                        renderer (`MiscRule.render` and its near-duplicate
                                                        multi-line- declaration renderer) around a standalone
                                                        `//` banner comment used as a section divider between
                                                        parameter groups (`SWDFlashLoader.Specifier`'s
                                                        constructor, `STM32QSPI.newQSPICmd`). (1) A leading
                                                        `//` line comment was inlined onto the same output
                                                        line as the following parameter's type+name,
                                                        swallowing that declaration (and cascading to the
                                                        next) into the comment -- compile- breaking. Fixed by
                                                        emitting it on its own line. (2) The shared type/name
                                                        column width was computed only over params with no
                                                        leading comment, so an excluded param's `typeText`
                                                        could be as long as the column width, making
                                                        `padRight` a no-op and merging type+name with zero
                                                        space on the next pass. Fixed by never padding to less
                                                        than `typeText.length() + 1`. Verified idempotent
                                                        against both real files plus this fixture.

  real_code_regressions_67_inp/out.hpp               -- RDD_KEY_169: a named construct (struct/namespace)
                                                        whose base-clause is guarded by #if/#endif, with the
                                                        body `{` immediately following the bare #endif line.
                                                        Proves enforceKAndRBraceStyle no longer glues the `{`
                                                        onto the #endif line (which a later retokenize would
                                                        swallow into the PREPROCESSOR token, desyncing
                                                        brace/frame tracking and producing wrong
                                                        closing-comment labels/indentation -- originally found
                                                        via ericniebler/range-v3 item 20 bug (a), see
                                                        STATE_C_CPP_JAVA.md Open Questions).

  real_code_regressions_68_inp/out.json              -- JSON, microsoft/vscode real-code testing:
                                                        non-idempotent empty-container rendering.
                                                        JsonSpecificRule.parseContainer kept a dangling
                                                        placeholder Item for a comment-less blank line before
                                                        closing `}`/`]`, so round1 emitted loose `{\n}` but
                                                        round2 (finding no blank line to preserve) collapsed
                                                        it to `{}`. Fixed by only keeping the placeholder when
                                                        a real leading comment exists; a comment-less blank
                                                        line before the closer is now dropped during parsing.
                                                        No copyright-header block on this fixture -- plain
                                                        `.json` has no comment syntax to carry it.

  real_code_regressions_69_inp/out.css               -- CSS, twbs/bootstrap real-code testing
                                                        (content-preservation check, not syntax-check -- still
                                                        parses as valid CSS): normalize-comment-start-case
                                                        unconditionally capitalized case-sensitive rtlcss
                                                        directive comments (e.g. `/* rtl:begin:ignore */` ->
                                                        `/* Rtl:begin:ignore */`), silently breaking rtlcss's
                                                        directive parsing. Fixed via new
                                                        `FormatterSimpleBraced.isSingleTokenDirective`
                                                        exemption: a single-line comment whose whole trimmed
                                                        body is one whitespace-free token containing `:` or
                                                        `-` is left alone; ordinary prose is still capitalized
                                                        as before.

  real_code_regressions_70_inp/out.toml              -- TOML, rust-lang/cargo real-code testing (first TOML
                                                        dogfood run): two forward-pass crash bugs. (1) A
                                                        multi-line array's interior per-element trailing `#`
                                                        comment was treated as extending to the end of the
                                                        joined logical line, swallowing the array's closing
                                                        `]` as comment text ("unterminated array"). Fixed by
                                                        stripping each continuation line's own trailing
                                                        comment before joining, not just the final result's.
                                                        (2) Multi-line basic/literal strings
                                                        (`"""..."""`/`'''...'''`) were entirely unsupported,
                                                        crashing ("expected 'key = value' line") on a `key =
                                                        """` block. Fixed by detecting an unterminated
                                                        multi-line-string opener before the bracket-balance
                                                        check and consuming raw lines verbatim to the matching
                                                        closing delimiter, same opaque treatment as JSON5's
                                                        multi-line strings.

  real_code_regressions_71_inp/out.yaml              -- YAML, kubernetes/kubernetes real-code testing (first
                                                        YAML dogfood run): six combined bugs. (1) A
                                                        sequence-of-mapping's first key rejected a same-indent
                                                        nested sequence child (common "- apiGroups:\n    -
                                                        \"*\"" manifest style); fixed by mirroring the same
                                                        rule plain mapping keys already had. (2)/(3) Quoted
                                                        and plain scalars wrapping across physical lines
                                                        (common in CRD/API description fields) crashed the
                                                        line-based parser; fixed by detecting an unterminated
                                                        quote / deeper continuation line and capturing it as
                                                        an opaque multi-line scalar, for both plain keys and
                                                        sequence-of-mapping first keys. (4) A dangling
                                                        trailing-comment item (null key) inside a
                                                        sequence-of-mapping's children threw an NPE in the
                                                        colon-alignment padding helper; fixed by excluding
                                                        dangling items from the padding key list. (5)
                                                        Idempotency-only: multi-line scalar/block-scalar
                                                        continuations stored their indentation as an ABSOLUTE
                                                        value rather than a delta relative to their own key,
                                                        so a shift in the key's rendered column (from
                                                        colon-alignment padding etc.) broke idempotency on a
                                                        second pass; fixed by storing/re-anchoring a RELATIVE
                                                        delta instead. (6) A `|`/`>` block scalar as a plain
                                                        (non-keyed) sequence item's own value (e.g.
                                                        "command:\n- |\n  script text") was silently truncated
                                                        to an empty string -- found via the
                                                        content-preservation check, since the truncated output
                                                        was still syntactically valid YAML. Fixed by adding
                                                        the same block-scalar (and multi-line-quoted-scalar)
                                                        detection to that sequence-item parser branch.

  real_code_regressions_72_inp/out.yaml              -- YAML, docker/compose real-code testing: a data-loss
                                                        bug found via the content-preservation check
                                                        (corrupted output still syntactically valid YAML). A
                                                        blank line immediately after a keyed line with no
                                                        inline value (e.g. "services:" then a blank line then
                                                        its nested mapping) caused the whole nested block to
                                                        be silently dropped -- every "does this key have a
                                                        child block" detection site used a plain `peek()`, so
                                                        a blank next line was treated as "no child". Fixed by
                                                        adding a `peekNonBlank()` helper (looks past blank
                                                        lines without consuming them) and using it at all four
                                                        detection sites instead of `peek()`.

  real_code_regressions_73_inp/out.yaml              -- YAML, ansible/ansible real-code testing: three
                                                        combined bugs, all found via the content-preservation
                                                        check (every corrupted output stayed syntactically
                                                        valid YAML). (1) A plain (non-keyed) sequence item's
                                                        own unquoted scalar value wrapping across physical
                                                        lines (e.g. a changelog fragment) had no continuation
                                                        handling at all, silently dropping every line past the
                                                        first; fixed by adding the same
                                                        multi-line-plain-scalar capture used for
                                                        keyed/seqOfMapping-first-key values, with the
                                                        continuation's baseline column at the scalar's own
                                                        start. (2) A comment dedented below its enclosing
                                                        block's indent (a real "# FIXME: ..." note at column 0
                                                        between deeper-indented sibling keys) made
                                                        `parseBlock` break out of every enclosing block in
                                                        turn without consuming it, permanently orphaning it
                                                        and dropping everything that followed at every level;
                                                        fixed by looking past the comment (and any more like
                                                        it) to the next real content line and attaching the
                                                        comment to whichever block that line's own indent
                                                        belongs to. (3) A bare top-level plain scalar document
                                                        (e.g. an `$ANSIBLE_VAULT;...` header followed by
                                                        opaque unquoted hex data with no "key:"/"- " shape)
                                                        only kept its first line, dropping the rest; fixed by
                                                        emitting the remaining raw lines verbatim once this
                                                        bare-scalar- document shape is detected.

  real_code_regressions_74_inp/out.svg               -- XML, w3c/svgwg real-code testing: `.svg` files were
                                                        never mapped to the "xml" language in `Lang.infer`, so
                                                        every `.svg` in the corpus failed with "could not
                                                        infer language from file extension" -- found via the
                                                        forward pass itself erroring, before any
                                                        syntax-check/content- preservation could even run.
                                                        Fixed by adding `.svg` alongside `.xml` in
                                                        `Lang.infer`'s extension check.

  real_code_regressions_75_inp/out.yaml              -- YAML, actions/starter-workflows real-code testing
                                                        (fourth and final planned YAML test-fixture repo): a
                                                        sequence item whose dash is followed by more than one
                                                        space before the key (`-   name: foo`) caused the next
                                                        sibling key (`uses:`) to be misidentified as a nested
                                                        child of the first key instead of a sibling, producing
                                                        invalid YAML ("bad indentation") -- `parseSeqItem`
                                                        computed the sibling-key column as a hardcoded dash-
                                                        plus-one-space offset instead of the actual column the
                                                        key started at. Found via syntax-checking round1
                                                        output. Fixed by computing the actual first-key column
                                                        from the dash line's real leading whitespace and using
                                                        it consistently for the sibling/nested-child decision
                                                        and multi-line scalar continuation anchoring.

  real_code_regressions_76_inp/out.hpp               -- C++26, simdjson/experimental_json_builder real-code
                                                        testing: `enforceAttributeAndSpliceBracketPadding`'s
                                                        loose `[: expr :]` padding ran in Phase 4, after
                                                        `enforceCallLineBreaking` had already measured/decided
                                                        not to wrap a line right at the length limit -- a
                                                        fresh format saw the pre-padding width and stayed one
                                                        line, while reformatting that already-padded output
                                                        saw the now-over-limit width and wrapped, a
                                                        non-idempotent round1/round2 mismatch. Found via
                                                        idempotency diffing. Fixed by pulling
                                                        `enforceAttributeAndSpliceBracketPadding` forward to
                                                        run right before `enforceCallLineBreaking`, same fix
                                                        shape already used for `enforceComplexityPadding`.

  real_code_regressions_77_inp/out.js                -- JS, expressjs/express real-code testing: two combined
                                                        bugs. (1) ASI (§2 semicolon insertion): a leading-
                                                        continuation-operator/comma line (method chaining on
                                                        its own line, or a comma-first multi-declarator list)
                                                        was wrongly treated as ending the previous statement
                                                        -- `needsSemicolonAfter` only checked the previous
                                                        line's own trailing token, never the next line's
                                                        leading token, so a bogus `;` landed
                                                        mid-chain/mid-declarator- list. Found via `node
                                                        --check` on round1 output. Fixed by adding a
                                                        leading-operator/comma lookahead alongside the
                                                        existing trailing-operator check. (2) The tokenizer
                                                        had no JS/TS regex-literal recognition at all -- a
                                                        bare `/` was always treated as the division operator,
                                                        so a regex containing a `"` inside a bracketed
                                                        character class got its `"` mistaken for a string
                                                        literal, corrupting brace/paren tracking for the rest
                                                        of the statement. Found via `node --check` on round1
                                                        output. Fixed by adding
                                                        `TokenizerCurly.emitRegexLiteral`/`isRe
                                                        gexLiteralAllowedHere` (regex-vs-division
                                                        disambiguation based on the preceding significant
                                                        token), emitting the whole literal as one opaque
                                                        `STRING` token.

  real_code_regressions_78_inp/out.py                -- Python3, pallets/flask real-code testing (first
                                                        Python3 dogfood run): a non-idempotency bug found via
                                                        `diff -r round1 round2` (a formatter under-application
                                                        bug, not scoping corruption -- round1's tree was
                                                        already semantically correct).
                                                        `ScopePipelineIndent.render`'s replacement-merge loop
                                                        advanced its replacement-list cursor `r` only on an
                                                        exact `start == i` match; whenever two
                                                        independently-computed passes legitimately produced
                                                        overlapping token-range replacements, the now- stale
                                                        entry permanently stalled `r`, silently dropping every
                                                        later replacement in the file, not just the
                                                        genuinely-overlapping one. Fixed by having `render`
                                                        skip past (not get stuck on) any replacement whose
                                                        `start` has already been passed by `i`.

  real_code_regressions_79_inp/out.py                -- Python3, pallets/flask real-code testing (same run as
                                                        fixture 78): two more idempotency bugs. (1) §6
                                                        signature alignment: `trySignatureGroup` split params
                                                        on raw NEWLINE tokens without checking bracket depth,
                                                        misclassifying a multi-line type-hint's continuation
                                                        lines as bogus params and corrupting the signature
                                                        with growing trailing whitespace each round; fixed by
                                                        only splitting at depth-0 NEWLINEs in
                                                        `classifySignatureParam`. (2) §9.2
                                                        blank-line-before-`elif`/`else` and §8's
                                                        statement-join could target the same token index,
                                                        letting the join swallow the blank-line insertion;
                                                        fixed by sorting zero-width entries first on ties.

  real_code_regressions_80_inp/out.py                -- Python3, pallets/click real-code testing: §4 decorator
                                                        bracket-padding (`applyBracketPadding`) couldn't
                                                        distinguish an f-string field's `{`/`}` from a
                                                        dict/set literal, padding it like a non-empty brace
                                                        pair and producing `f"{ ctx.info_name }"`; §5's
                                                        f-string spacing pass then trimmed it back next round,
                                                        so it only surfaced as non-idempotency. Fixed by
                                                        skipping any `{`/`}` immediately preceded by
                                                        `FSTRING_START`/`FSTRING_MIDDLE`.

  real_code_regressions_81_inp/out.ts                -- JS/TS, nestjs/nest real-code testing: a multi-arg call
                                                        whose args are all bare dotted member-access
                                                        expressions (`options.provideInjectionTokensFrom`,
                                                        `options.inject`) was misparsed by
                                                        `MiscRuleCurly.renderCallCandidate`'s `parseSignature`
                                                        as a C/C++/Java forward-declaration parameter list --
                                                        the same misparse already guarded for Kotlin but not
                                                        JS/TS -- inserting a column-separator space and
                                                        corrupting `options. provideInjectionTokensFrom`,
                                                        compounding each pass. Fixed by forcing `sigForRender`
                                                        to null for JS/TS too.

  real_code_regressions_82_inp/out.ts                -- JS/TS, nestjs/nest real-code testing: content
                                                        duplication.
                                                        `JsTsSpecificRule.enforceClassFieldAlignmentGrid`'s
                                                        linear `cursor` sweep assumed every selected class
                                                        span was disjoint, but an anonymous `return class
                                                        extends Base {...}` nested inside an outer class's
                                                        method is legitimate nesting; re-processing the inner
                                                        span as its own top-level entry duplicated content and
                                                        walked `cursor` backward, causing the final raw-copy
                                                        loop to re-emit everything to EOF a second time. Fixed
                                                        by filtering `classOpens` to only the outermost class
                                                        brace at each nesting level.

  real_code_regressions_83_inp/out.yaml              -- YAML, prometheus/prometheus real-code testing: four
                                                        combined data-loss bugs in `YamlSpecificRule`, all a
                                                        dash/key line whose "value" is
                                                        absent/comment-only/anchor-only/an unbalanced
                                                        multi-line flow opener with real content on
                                                        more-indented following lines. (1) `parseKeyItem`'s
                                                        flow-value early return didn't check the flow closed
                                                        on the same line, truncating text after an unbalanced
                                                        `[...]`. (2) `parseSeqItem`'s `seqOfMapping` first-key
                                                        handling had the same gap, and also dropped everything
                                                        after a comment-only dash line. (3) An anchor-only
                                                        dash line (`- &highalert`) followed by a nested
                                                        mapping at an equal (not just greater) indent lost its
                                                        child block. (4) `renderFlowValue` rendered an empty
                                                        `{}`/`[]` as a block conversion whenever the line
                                                        didn't fit, but `renderFlowBlock` has nothing to
                                                        iterate for zero entries, silently dropping the value.

  real_code_regressions_84_inp/out.ts                -- JS/TS, nestjs/nest real-code testing:
                                                        comment-continuation-indent drift on an object-shaped
                                                        `type X = {...} & Y;` intersection alias.
                                                        `JsTsSpecificRule.enforceUnionTypeContinuationIndent`
                                                        re-indents every NEWLINE from the RHS through the
                                                        terminating `;` to the RHS's column with no
                                                        bracket-depth tracking; for a plain multi-line union
                                                        this is harmless, but an intersection whose left
                                                        operand is a multi-line object-type literal has
                                                        NEWLINEs nested many levels deep that got
                                                        force-reindented too, blowing members out to an
                                                        arbitrarily deep column. Fixed by tracking bracket
                                                        depth and only re-indenting depth-0 NEWLINEs.

  real_code_regressions_85_inp/out.ts                -- JS/TS, nestjs/nest real-code testing: `join(...)`
                                                        call-wrap/collapse non-idempotency.
                                                        `MiscRuleCurly.renderCallCandidate`'s
                                                        multi-line-source branch always preserved the original
                                                        per-line argument grouping with no fits-check, unlike
                                                        the sibling single-line branch -- a call wrapped
                                                        across lines stayed wrapped forever even once it fit
                                                        on one line, while the same call written fresh on one
                                                        line collapsed correctly, so the same call could
                                                        settle into two different stable shapes. Fixed by
                                                        adding the same fits-check (JS/TS-only, to avoid
                                                        regressing fixture 1's C/C++/Java case), measuring the
                                                        tight single-line candidate rather than the loose
                                                        `collapseToOneLine` helper (which overestimates
                                                        length). Also updated fixture 81's expected output,
                                                        whose old shape was itself an artifact of this bug.

  real_code_regressions_86_inp/out.yaml              -- YAML, home-assistant/core real-code testing:
                                                        nested-sequence data loss. `parseSeqItem` never
                                                        recognized the compact single-line nested-seq form `-
                                                        - a\n  - b`; the inner `- ` was captured as a literal
                                                        scalar, leaving the sibling nested item unconsumed,
                                                        whose mismatched indent then made `parseBlock` break
                                                        out of the entire enclosing block early -- silently
                                                        dropping the rest of the sequence and every sibling
                                                        item/key that followed, at every level. Fixed by
                                                        detecting the `-`/`- ` shape up front via a new
                                                        `parseInlineNestedSeq` helper and rendering
                                                        non-lossily via the existing `item.children` path.
                                                        Found via content-preservation checking, not
                                                        syntax-check.

  real_code_regressions_87_inp/out.ts                -- JS/TS, vuejs/core real-code testing: leading
                                                        multi-line block comment reindent non-idempotency.
                                                        `JsTsSpecificRule`'s class-field alignment grid,
                                                        enum-member formatting, and interface/type-alias
                                                        member alignment all re-emitted a member's leading
                                                        `/** ... */` comment verbatim at its original source
                                                        indent, never reindented to the member's own
                                                        re-rendered depth -- misaligned on the first pass,
                                                        self-corrected by an unrelated general reindent pass
                                                        on the second, producing round1 != round2. Fixed by
                                                        adding `reindentLeadingComment` at all three sites.

  real_code_regressions_88_inp/out.ts                -- JS/TS, vuejs/core real-code testing:
                                                        `TokenizerCurly.GENERIC_SAFE_KEYWORDS` was missing
                                                        TS's `symbol`/`bigint`, and `isGenericSafeToken`'s OP
                                                        case had no `|` entry -- a union type inside a generic
                                                        argument list (`Record<string | symbol, Function |
                                                        number>`) invalidated the enclosing `<...>` tracking,
                                                        leaving `>` a plain OP token. That defeated
                                                        `enforceSemicolonInsertion`'s continuation check
                                                        (dropping the statement's `;`) and desynced
                                                        `JsTsDeclarationAlignmentRule.parseTypeAlias`'s
                                                        depth-scan, corrupting unrelated following statements
                                                        with bogus alignment padding. Fixed by adding
                                                        `symbol`/`bigint` and `|` (TS-only).

  real_code_regressions_89_inp/out.ts                -- JS/TS, vuejs/core real-code testing
                                                        (componentOptions.ts), two further bugs chasing the
                                                        same file's non-idempotency after fixture 88. (1)
                                                        `JsTsDeclarationAlignmentRule.parseTypeAlias`'s
                                                        generic-parameter-list skip loop advanced past a
                                                        type-parameter default clause (`<T = X>`) without
                                                        capturing it, silently deleting it from the output;
                                                        fixed by capturing and re-rendering the range. (2)
                                                        `TokenizerCurly`'s dispatch loop had an unconditional
                                                        `]]` branch meant for C++ attributes/splice brackets
                                                        that also fired for a TS indexed-access type inside a
                                                        mapped type (`{ [K in T[number]]?: unknown }`),
                                                        emitting a bare OP token instead of going through
                                                        `emitCloseBracket()`'s PUNCT path -- desyncing
                                                        `enforceSemicolonInsertion`'s bracket-depth counter
                                                        for the rest of the file. Fixed by gating the branch
                                                        to `lang.isCpp`. Output still carries the known
                                                        general-reindentation gap for the mapped type's own
                                                        body.

  real_code_regressions_90_inp/out.ts                -- JS/TS, vuejs/core real-code testing
                                                        (ref.test-d.ts/watch.test-d.ts):
                                                        `JsTsSpecificRule.classifyBraces`'s `isValue`
                                                        prev-token list had no entry for the
                                                        union/intersection operators `|`/`&` -- an inline
                                                        object type following one in a union alias fell
                                                        through to "not a value", misclassifying its `{` as a
                                                        statement-body brace and resetting
                                                        `enforceSemicolonInsertion`'s depth counter
                                                        mid-expression, corrupting every subsequent line's
                                                        indentation for the rest of the scope. Fixed by adding
                                                        `lang.isTs && (isOp(prev, "|") || isOp(prev, "&"))` to
                                                        the check.

  real_code_regressions_91_inp/out.xsl               -- XML, apache/ant real-code testing: `Lang.infer` never
                                                        mapped `.xsl` to `xml` (same gap shape as fixture 74's
                                                        `.svg`), so every `.xsl` file failed with "could not
                                                        infer language from file extension". Fixed by adding
                                                        `.xsd`/`.xsl` alongside `.xml`/`.svg`.

  real_code_regressions_92_inp/out.xsd               -- XML, apache/ant real-code testing: same `Lang.infer`
                                                        gap as fixture 91, for `.xsd`.

  real_code_regressions_93_inp/out.ts                -- JS/TS, vuejs/core real-code testing: two
                                                        non-idempotency bugs from
                                                        `enforceCallLineBreaking`/`enforceComplexityPadding`
                                                        pass-ordering. (1) `MiscRuleCurly.collapseToOneLine`'s
                                                        fits-check flattened every whitespace run to one
                                                        space, including same-line declaration-alignment
                                                        padding, undercounting a padded declaration's true
                                                        width and wrongly collapsing it; fixed by only
                                                        collapsing runs that actually contain a NEWLINE. (2)
                                                        `FormatterCurly`'s final `enforceCallLineBreaking`
                                                        pass had no `enforceComplexityPadding` re-run after
                                                        it, so a call wrapped by an earlier inflated
                                                        fits-check and later re-collapsed lost its loose `( x
                                                        )` nested-bracket padding; fixed by adding one more
                                                        `enforceComplexityPadding` call after the final
                                                        line-breaking pass.

  real_code_regressions_94_inp/out.js                -- JS/TS, vuejs/core real-code testing:
                                                        `BlockStructureRule.alignBracelessElseIfChain` runs
                                                        last, after every `enforceCallLineBreaking`
                                                        fits-check, so its own column padding of a braceless
                                                        if/else chain could push an already-fits-checked
                                                        consequent past the line limit with no re-check
                                                        (widespread across the corpus). Fixed by refusing to
                                                        pad a branch past the line limit when its un-padded
                                                        width already fit, leaving it at natural width
                                                        instead; an already-over-limit branch is still padded
                                                        as before. Required adding a `lineLengthLimit`
                                                        parameter to `BlockStructureRule`.

  real_code_regressions_95_inp/out.java              -- Java, local vendored third-party library dogfood
                                                        testing: two idempotency bugs sharing one root cause
                                                        -- raw source indent measured before conversion to the
                                                        target indent-style -- only observable against
                                                        tab-indented source. (1)
                                                        `MiscRule.enforceCommentStyle` reindented a block
                                                        comment's continuation lines to the comment's raw
                                                        (still-tab) leading indent, baking a tab into text
                                                        that `convertIndentation` never revisits since it's
                                                        now inside the comment token; self-corrected only on a
                                                        second pass. Fixed by normalizing through
                                                        `MiscRuleCore.renderIndent` first. (2)
                                                        `MiscRule.enforceCallLineBreaking`'s fits-checks
                                                        measured a tab-indented line's leading indent via
                                                        `String.length()` (tab = 1 char), wrongly
                                                        under-measuring width and leaving an over-limit line
                                                        collapsed until a second pass. Fixed by a new
                                                        `MiscRuleCore.expandedIndentWidth` helper used at both
                                                        fits-check sites.

  real_code_regressions_96_inp/out.ts                -- JS/TS, vuejs/core real-code testing:
                                                        `BlockStructureRule.collapseSingleExpressionBlocks`
                                                        flattened a braced single-statement `if` body
                                                        containing an object literal onto one line, discarding
                                                        the literal's own multi-line layout; a later
                                                        `enforceCallLineBreaking` re-wrap of a call nested
                                                        inside it then had no signal the literal's closing `}`
                                                        should get its own line, producing a genuine
                                                        non-fixed-point flap between two stable shapes. Fixed
                                                        by refusing to collapse (new `containsBrace` check)
                                                        whenever the single-statement body itself contains a
                                                        `{`/`}` pair.

  real_code_regressions_97_inp/out.ts                -- JS/TS, vuejs/core real-code testing:
                                                        `JsTsSpecificRule.enforceSemicolonInsertion`'s
                                                        depth-tracking loop counted only `(`/`[`/`{` against
                                                        `)`/`]`/`}`, with no case for a generic clause's
                                                        `ANGLE_BRACKET_OPEN`/`_CLOSE` -- a multi-line generic
                                                        clause (`function mergeProps<\n  T,\n U\n>(...)`) left
                                                        depth at 0 across its own NEWLINEs, wrongly inserting
                                                        spurious `;` inside `<...>`. Fixed by adding the
                                                        angle-bracket cases to the depth-tracking loop.

  real_code_regressions_98_inp/out.ts                -- JS/TS, vuejs/core real-code testing (found via the
                                                        `tsc` typecheck pass):
                                                        `JsTsSpecificRule.classifyBraces` had no case for an
                                                        `export { ... }` brace header (only `import`), so a
                                                        single-specifier one-liner fell through to "statement
                                                        body" classification and got a bogus `;` inserted
                                                        before its own closing `}` -- a real parse error.
                                                        Fixed by adding an `isExportBraceHeader` case
                                                        mirroring the import one, with `needsSemicolon`
                                                        computed per-brace (false when followed by `from
                                                        '...'`, true for a plain named export with no `from`
                                                        clause).

  real_code_regressions_99_inp/out.ts                -- JS/TS, vuejs/core real-code testing (found via the
                                                        same `tsc` pass as fixture 98):
                                                        `TokenizerCurly.isGenericSafeToken`'s OP case
                                                        recognized `:` as generic-safe for Kotlin only -- a TS
                                                        conditional type inside a generic argument list
                                                        (`Readonly<A extends B ? C : D>`) hit its own `:` and
                                                        invalidated the enclosing `<...>` tracking, defeating
                                                        fixture 97's depth tracking and wrongly inserting a
                                                        `;` before the clause's closing `>`. Fixed by adding
                                                        `lang.isTs && ":".equals(t.text)` alongside the
                                                        Kotlin-only case.

  real_code_regressions_100_inp/out.ts               -- JS/TS, vuejs/core real-code testing (found via the
                                                        same `tsc` pass): `enforceSemicolonInsertion`'s
                                                        NEWLINE-boundary continuation checks had no case for a
                                                        class/interface header wrapping its own
                                                        `extends`/`implements` clause onto its own line -- the
                                                        declaration name's trailing NEWLINE was wrongly
                                                        treated as a statement boundary, splitting the header
                                                        with a bogus `;`. Fixed by adding a lookahead: a
                                                        next-line leading `extends`/`implements` keyword also
                                                        means the statement isn't finished, same as the
                                                        existing `{`/`|`/`&`/`,` cases.

  real_code_regressions_101_inp/out.ts               -- JS/TS, vuejs/core real-code testing (found via the
                                                        same `tsc` pass):
                                                        `enforceArrowFunctionParameterParens` wrapped any bare
                                                        identifier before `=>` in parens with no check for a
                                                        TS return-type annotation ending in a type predicate
                                                        or bare type name (`(node: Node): node is Function =>
                                                        {...}`), wrapping the return type's tail as if it were
                                                        a parameter -- a real TS parse error. Fixed by
                                                        checking the token before the candidate identifier: a
                                                        preceding `:` or `is` means it's a return type, left
                                                        unwrapped.

  real_code_regressions_102_inp/out.ts               -- JS/TS, vuejs/core real-code testing (found via the
                                                        same `tsc` pass): `GENERIC_SAFE_KEYWORDS` was missing
                                                        TS's `true`/`false` boolean-literal-type keywords -- a
                                                        boolean type argument inside a multi-line generic
                                                        clause invalidated `reclassifyAngleBrackets`'s
                                                        open-stack tracking, defeating
                                                        `enforceSemicolonInsertion`'s depth tracking and
                                                        inserting a bogus `;` before the closing `>`. Fixed by
                                                        adding `true`/`false` to `GENERIC_SAFE_KEYWORDS`. A
                                                        related fix landed in the same investigation: the
                                                        existing "any `{`/`}`/`;` clears the whole open stack"
                                                        rule was also firing on a legitimate nested
                                                        object-type argument inside an already-tracked generic
                                                        clause; fixed with a `nestedBraceDepth` counter that
                                                        skips the clear-all for balanced `{...}` while the
                                                        open stack is non-empty.

  real_code_regressions_103_inp/out.html             -- HTML5, WordPress/wordpress-develop real-code testing:
                                                        `renderElement`'s multi-child block-closing render
                                                        path never emitted `n.trailingComment`, unlike the
                                                        other three render branches -- a same-line trailing
                                                        comment right after a block element's closing tag was
                                                        silently dropped whenever that element had element
                                                        children (not just a lone text node), real data loss.
                                                        Fixed by routing the multi-child closing-tag line
                                                        through `appendWithTrailing` too.

  real_code_regressions_104_inp/out.html             -- HTML5, alexandersandberg/html5-elements-tester
                                                        real-code testing (RDD_KEY_198): `<ruby>` uses HTML5's
                                                        optional/implied-end-tag rule -- its
                                                        `<rb>`/`<rt>`/`<rp>`/`<rtc>` children never carry an
                                                        explicit closing tag -- and `parseElement` had no
                                                        notion of this, throwing `XmlParseException`. Fixed by
                                                        adding an extensible `OPAQUE_IMPLIED_END_TAG_ELEMENTS`
                                                        set (currently just `ruby`) that scans the whole
                                                        element as one verbatim opaque span, reusing the
                                                        existing `<script>`/`<style>`/`<pre>` pattern.

  real_code_regressions_105_inp/out.ts               -- JS/TS, vuejs/core real-code testing (final batch from
                                                        the full 514-file dogfood tsc pass, consolidated into
                                                        one fixture): six independent bugs. (1)
                                                        `reclassifyAngleBrackets`'s `nestedBraceDepth` guard
                                                        (added for fixture 102) only covered the nested
                                                        `{`/`}` delimiters, not tokens inside them, still
                                                        letting an interior keyword/`;` wipe the outer `<...>`
                                                        tracking; fixed by extending both checks to skip while
                                                        `nestedBraceDepth > 0`. (2) A mapped-type object as a
                                                        generic type argument needed `ANGLE_BRACKET_OPEN`
                                                        added to `classifyBraces`'s `isValue` whitelist. (3) A
                                                        ternary nested inside a parenthesized grouping
                                                        expression had its `:` misclassified as a return-type
                                                        colon; fixed with a new `isGroupingExpressionParen`
                                                        helper. (4) `key is keyof typeof val => ...` wrongly
                                                        wrapped `val` in parens because the arrow-param
                                                        bail-out only recognized `is`, not `typeof`/`keyof`.
                                                        (5) A trailing type-annotation `:` wrapping to the
                                                        next line got a bogus `;` because
                                                        `needsSemicolonAfter`'s `isPunct(t, ":")` guard never
                                                        matches (`:` tokenizes as OP); fixed by adding `":"`
                                                        to `CONTINUATION_OPS` instead. (6) A standalone TS
                                                        function-type parameter list got padded like an
                                                        arbitrary grouping paren; fixed with a
                                                        `lang.isTs`-gated exception when the matching `)` is
                                                        followed by `=>`. Also added `=>`/`...` to
                                                        `isGenericSafeToken`'s TS-safe OP list (`...` gated to
                                                        TS only, to avoid regressing C++ variadic-template
                                                        spacing in fixture 53).

  real_code_regressions_106_inp/out.html             -- HTML5, unquoted attribute values (RDD_KEY_199). Found
                                                        via the same `alexandersandberg/html5-elements-tester`
                                                        dogfood spot-check as fixture 104:
                                                        `XmlSpecificRule.parseAttr` required a quoted value
                                                        and threw on `<select ... size=5>`, even though
                                                        unquoted values are valid per the HTML5 spec grammar.
                                                        Fixed by accepting an unquoted value on the
                                                        `lang.isHtml5` branch only (plain XML still requires
                                                        quotes) and preserving it unquoted on output,
                                                        consistent with this codebase's "preserve as written"
                                                        posture elsewhere (JSON5/TOML quote style). Fixture
                                                        isolates a `<select size=5>`/unquoted `<option
                                                        value=...>` block plus an `<input>` mixing unquoted
                                                        values and a bare boolean attribute.

  real_code_regressions_107_inp/out.ts               -- JS/TS, vuejs/core real-code testing (found on the
                                                        final full-corpus tsc rerun, after fixture 105
                                                        landed): `typeof` was missing from
                                                        `GENERIC_SAFE_KEYWORDS` -- a `typeof` type-query
                                                        operand inside a generic argument list
                                                        (`Record<(typeof identityMethods)[number], any>`,
                                                        `ReturnType<typeof createServer>`) invalidated the
                                                        `<...>` open-stack tracking, same class of bug as the
                                                        `keyof`/`is`/`infer` gap fixed for fixture 101. In the
                                                        multi-line case this produced a bogus `;` before the
                                                        closing `>`; in the single-line case, losing the
                                                        tracking left `>` a plain OP token, defeating
                                                        statement-boundary detection entirely and merging the
                                                        following statement onto the same line. Fixed by
                                                        adding `typeof` to `GENERIC_SAFE_KEYWORDS`.

  real_code_regressions_108_inp/out.html             -- HTML5, `<option>` implied-closing-trigger support
                                                        (RDD_KEY_200). Covers an explicitly-closed `<option>`
                                                        (regression guard on the pre-existing common case)
                                                        plus a `<datalist>` with bare `<option value="...">`
                                                        tags relying on HTML5's implied-end-tag rule (closed
                                                        by a sibling `<option>`/`<optgroup>` start, or by the
                                                        parent's own closing tag), the same shape that blocked
                                                        the `alexandersandberg/html5-elements-tester` dogfood
                                                        run.

  real_code_regressions_109_inp/out.html             -- HTML5, `web-platform-tests/wpt` dogfood: four bugs --
                                                        (1) `<head>` added to `IMPLIED_CLOSE_TRIGGERS` (closes
                                                        on sibling `<body>` start); (2) bare `<image>`
                                                        rewritten to `<img>` outside real SVG foreign content
                                                        only (`svgDepth` counter); (3) EOF now implicitly
                                                        closes any still-open element instead of throwing
                                                        (HTML5 "stopped parsing" step); (4) `<xmp>` recognized
                                                        as a raw-text element like `<pre>`.

  real_code_regressions_110_inp/out.html             -- HTML5, follow-up hardening after user review of _109:
                                                        (1) the hardcoded `image` check generalized into a
                                                        `TAG_NAME_REWRITES` map; (2) a mismatched/orphaned
                                                        closing tag with no corresponding open element no
                                                        longer crashes even at document root (tolerant-close
                                                        fallback broadened, plus a new top-level
                                                        `parseNodes(stopAtCloseTag=false)` fallback) --
                                                        discarded and parsing continues instead of throwing.

  real_code_regressions_111_inp/out.html             -- HTML5, follow-up hardening (user request): raw-text
                                                        elements (`<script>`/`<style>`/`<pre>`/`<xmp>`) whose
                                                        literal closing tag never appears at all used to crash
                                                        in `finishRawElement`/`finishRawTextElement` on real
                                                        EOF; both now capture verbatim through EOF instead,
                                                        same tolerance principle as _109/_110. Last crash site
                                                        from the "HTML5 deep tree-construction edge cases"
                                                        Open Question in `STATE_DATA_FORMATS.md`.

  real_code_regressions_112_inp/out.html             -- HTML5, standalone follow-up (user request,
                                                        2026-07-25): SVG tag-name case-folding, split out of
                                                        the tree- construction Open Question as its own
                                                        lookup-table fix. New
                                                        `XmlSpecificRule.SVG_TAG_NAME_CASE_FIXUP` map (spec's
                                                        "Adjust SVG tag names" table, e.g. `lineargradient` ->
                                                        `linearGradient`), gated `svgDepth > 0` (opposite of
                                                        `TAG_NAME_REWRITES`'s `== 0`). Fixture proves both the
                                                        SVG-nested rewrite and the same tag name left
                                                        untouched as plain HTML. Surfaced a latent closing-tag
                                                        bug: once `tagName` is case-rewritten, the
                                                        literal-case `closeTok` no longer matched the source's
                                                        original-case closing tag; fixed via new
                                                        case-insensitive `startsWithCloseTagIgnoreCase`
                                                        (HTML5-only). MathML's `definitionurl` ->
                                                        `definitionURL` attribute-only fixup intentionally
                                                        left open -- no MathML- depth tracking exists yet; see
                                                        `STATE_DATA_FORMATS.md`.

  real_code_regressions_113_inp/out.java             -- Java, jenkinsci/jenkins real-code dogfood: 2 bugs
                                                        fixed. (a) `JavaSpecificRule.findArrowCases`'s
                                                        brace-depth-0 scan never skipped past a case's own
                                                        found arrow, so multi-value labels like `case null,
                                                        default ->` got re-matched and duplicated worse each
                                                        pass; fixed by advancing the scan index past the found
                                                        arrow. (b) `MiscRuleCore .needsSpaceBetween` only
                                                        special-cased Kotlin's `@` as tight against the next
                                                        identifier, so Java annotations rendered `@ NonNull`;
                                                        extended to `lang.isJava`. A third bug
                                                        (`alignCommentSeparators` false-positiving on ordinary
                                                        prose) was NOT fixed -- re-opens the user-resolved
                                                        RDD_KEY_50 design decision rather than being a plain
                                                        implementation bug; see `STATE_C_CPP_JAVA.md`'s
                                                        jenkinsci/jenkins dogfood entry (accepted as a
                                                        permanent known limitation, also noted in
                                                        `README.md`).

  real_code_regressions_114_inp/out.py               -- Python3, psf/black real-code dogfood: crash fix.
                                                        `TokenizerIndent.emitFString`'s backslash-escape
                                                        handling always skipped 2 chars (backslash + next)
                                                        even when next was `{`/`}`, breaking a following
                                                        `{{`/`}}` doubled-brace escape apart -- `f"{1}\{{"`
                                                        left a lone, never-closed `{` field open, which
                                                        `ScopePipelineIndent.processField` then walked past
                                                        the token list's end on (IndexOutOfBoundsException).
                                                        Confirmed against real CPython semantics (`f"\{y}"`
                                                        opens a real field; `f"{1}\{{"` evaluates to `'1\{'`,
                                                        no dangling field): the backslash must consume only
                                                        itself before `{`/`}`. Fixed by skipping just 1 char
                                                        in that case so the brace is re-evaluated fresh next
                                                        iteration. Identity-pass fixture (proves the crash is
                                                        gone, not a rendering change). See
                                                        `STATE_PYTHON3.md`'s `psf/black` dogfood entry.

  real_code_regressions_115_inp/out.py               -- Python3, psf/black real-code dogfood: §7/§8
                                                        join-then-align ordering non-idempotency fix. A
                                                        block-form `match`/`case` group is correctly skipped
                                                        by §7's colon alignment on the forward pass (not yet
                                                        compact); §8 then joins each case's single-statement
                                                        body onto its header line later in the same pass. A
                                                        second pass previously saw the now-compact `case`
                                                        lines for the first time and applied colon-column
                                                        padding never present in the first pass's output.
                                                        Fixed in `ScopePipelineIndent`: §7's
                                                        `classifyCaseLine` now predicts (new
                                                        `tryQualifyJoinBody`, shared with
                                                        `applySingleStatementBody`) whether a block-form case
                                                        will qualify for §8's join, treating it as effectively
                                                        compact for grouping/alignment so `flushCaseGroup`
                                                        bakes correct padding in immediately;
                                                        `applySingleStatementBody` skips any header §7 already
                                                        joined (`caseJoinAlignedHeaders`) to avoid a duplicate
                                                        join. See `STATE_PYTHON3.md`'s `psf/black` dogfood
                                                        entry.

  real_code_regressions_116_inp/out.py               -- Python3, psf/black real-code dogfood: §6 multi-
                                                        physical-line type-hint gap fix. A `def` parameter
                                                        whose type hint spans lines via a `|`-union with no
                                                        enclosing bracket (`x: Type1\n| Type2,`) was
                                                        misclassified: `trySignatureGroup`'s NEWLINE-delimited
                                                        segmentation only folds a multi-line param back into
                                                        one segment when a nested bracket stays open, so each
                                                        `| TypeN` continuation became its own bogus parameter,
                                                        and `classifySignatureParam` padded a nonexistent
                                                        `:`/`=` column with whitespace that grew unbounded
                                                        every round instead of leaving the whole signature
                                                        untouched (this method's documented gap). Fixed:
                                                        `classifySignatureParam` now rejects (returns null)
                                                        any segment whose first token isn't a valid parameter
                                                        start (identifier, or `*`/`**`/`/`) -- a leading `|`
                                                        means it's a continuation, not a parameter.
                                                        Identity-pass fixture (converges to a true no-op
                                                        instead of growing). See `STATE_PYTHON3.md`'s
                                                        `psf/black` dogfood entry.

  real_code_regressions_117_inp/out.py               -- Python3, psf/black real-code dogfood: two §5
                                                        `addBraceTrim` content-corruption fixes, combined
                                                        (both live in the same method). (a) A field
                                                        immediately followed by a nested `{` (e.g. `f"{ {a for
                                                        a in (1, 2, 3)}}"`) had its open-gap trim collapse the
                                                        field's `{` and the nested `{` into a literal `{{`,
                                                        which Python's f-string grammar parses as an ESCAPED
                                                        brace rather than two field-opens -- silently deleting
                                                        the whole comprehension expression (confirmed via
                                                        `ast.dump`: the `FormattedValue` node vanished).
                                                        Fixed: `addBraceTrim` normalizes the open gap to one
                                                        space (not zero) whenever the next significant token
                                                        is itself a literal `{`. (b) A self-documenting
                                                        `{expr=}` debug field (e.g. `f'{ longer_name   =  :
                                                        .3f }'`) had its leading gap trimmed even though
                                                        Python's runtime must reproduce `expr`'s exact
                                                        original whitespace verbatim for a `=`-suffixed field
                                                        -- a real behavior change, not cosmetic. Fixed:
                                                        `addBraceTrim` now detects a bare trailing `=` (a lone
                                                        1-char OP token; all
                                                        comparison/augmented-assignment/walrus operators
                                                        tokenize as distinct multi-char OPs, so no risk of
                                                        confusion) as the expression's last significant token
                                                        and skips gap-trimming entirely for that field. Both
                                                        verified via `python_content_diff.py` (structurally
                                                        identical) and idempotency; identity-pass fixture. See
                                                        `STATE_PYTHON3.md`'s `psf/black` dogfood entry.

  real_code_regressions_118_inp/out.hpp              -- C++, microsoft/STL real-code dogfood: `Main.
                                                        applyLineEndings` idempotency fix. The tokenizer
                                                        preserves a CRLF-original file's own `\r` verbatim
                                                        inside any WHITESPACE token a pass doesn't rewrite, so
                                                        `line-endings = lf` (default)'s "already lf" fast path
                                                        left stray `\r` in untouched lines while rewritten
                                                        lines came out `\r`-free -- a mixed result that then
                                                        differed again on a second pass (round1 != round2).
                                                        Fixed: `applyLineEndings` now always normalizes to a
                                                        clean LF-only baseline before applying the requested
                                                        target ending. Verified against the real microsoft/STL
                                                        tree: fixed 99 of 110 idempotency-diffing files in
                                                        that candidate. See `STATE_C_CPP_JAVA.md`.

  real_code_regressions_119_inp/out.hpp              -- C++, microsoft/STL real-code dogfood: two duplicated
                                                        `collapseToOneLine`/`flushCollapseGap` implementations
                                                        (`MiscRuleCurly.java`, `CppSpecificRule.java`) joined
                                                        a multi-line run back onto one line by unconditionally
                                                        inserting a space wherever the original had a newline,
                                                        with no tight-join awareness -- so a wrapped
                                                        member-access/`->` expression broken right at the
                                                        `.`/`->` (e.g. a constructor's member-initializer-list
                                                        argument, `other.\n _Outer`) came back corrupted as
                                                        `other. _Outer` once re-collapsed. Sibling
                                                        `collapseTokensToOneLine` already had this guard for
                                                        JS/TS's `.`/`?.` (an earlier nestjs/nest fix) but it
                                                        was never mirrored here. Fixed: both now track the
                                                        previous/next significant token around each
                                                        whitespace/newline run and suppress the forced space
                                                        when either side is `.`/`->`. Verified against the
                                                        real microsoft/STL tree (`ranges.hpp`'s wrapped
                                                        constructor-initializer-list arguments). A related,
                                                        deeper bug in the same area (a long constructor
                                                        signature's parameter-wrap logic misapplied to its
                                                        following member-initializer-list entry) was found and
                                                        later fixed separately -- see fixture 121.

  real_code_regressions_120_inp/out.hpp              -- C++, microsoft/STL real-code dogfood: a bare
                                                        macro-invocation-as-statement (STL's own
                                                        `_TRY_IO_BEGIN`/`_TRY_BEGIN`/`_BEGIN_LOCK`, no
                                                        trailing `;`, own physical line) immediately followed
                                                        by an `if (...) { ... }` that
                                                        `collapseSingleExpressionBlocks` flattens to a
                                                        one-liner gets glued onto the macro's own line one
                                                        round later. Root cause: `splitStatements` merges the
                                                        macro identifier and the following `if (...) stmt;`
                                                        into one "statement" (no terminator after the bare
                                                        macro), so `DeclarationAlignmentRuleCurly
                                                        .parseDeclaration`'s collapsed-control-statement guard
                                                        -- which only checked the merged statement's first
                                                        token -- saw the macro IDENTIFIER instead of `if` and
                                                        never fired, misparsing the run as a bogus `Type name
                                                        = init;` declarator. Fixed by widening the guard to a
                                                        depth-tracked scan of the whole merged statement for a
                                                        top-level `if`/`while`/`for`/`switch`/ `do`/`else`
                                                        keyword. Verified against the real microsoft/STL tree
                                                        (`istream.hpp`/`stacktrace.hpp`/ `xlocale.hpp`, all
                                                        idempotent after the fix). See `STATE_C_CPP_JAVA.md`.

  real_code_regressions_121_inp/out.hpp              -- C++, microsoft/STL real-code dogfood: a wrapped
                                                        constructor signature (STL's own
                                                        `unique_lock(unique_lock&& _Other) noexcept :
                                                        _Pmtx(_Other._Pmtx), _Owns(_Other._Owns) {}`) whose
                                                        own parameter-wrap logic got misapplied to the
                                                        immediately-following member-initializer-list entry,
                                                        corrupting `_Other._Pmtx` into `_Other. _Pmtx` -- a
                                                        forward-pass bug, wrong on the very first format. Root
                                                        cause: `MiscRuleCurly.enforceCallLineBreaking` treats
                                                        `_Pmtx(_Other._Pmtx)` as an "IDENTIFIER (" call
                                                        candidate and hands it to `parseSignature`, whose
                                                        `parseParam` mis-slices the single argument as a `Type
                                                        name` declarator pair (last token is a bare
                                                        IDENTIFIER) -- `sigForRender` then routes it through
                                                        the declaration-style column-split renderer instead of
                                                        the tight-join-`.`/`->`-aware plain-call renderer,
                                                        inserting a space after the `.`. Fixed: `parseParam`
                                                        now rejects (returns null) any param whose parsed
                                                        `typeTokens` run ends in a `.`/`->` tight-join
                                                        operator (never a real C++ type), falling back to
                                                        plain-call rendering. Verified against the real
                                                        microsoft/STL tree (`mutex.hpp`/`shared_mutex.hpp`,
                                                        idempotent and corruption-free after the fix). See
                                                        `STATE_C_CPP_JAVA.md`.

  real_code_regressions_122_inp/out.hpp              -- C++, microsoft/STL real-code dogfood: a
                                                        declaration-alignment group whose first member has a
                                                        same-line leading comment (STL's own `/*
                                                        [[no_unique_address]] */ _Vw _Range;` followed by
                                                        un-commented siblings in the same group) got that
                                                        comment silently duplicated onto every sibling line
                                                        one round later, with the group's column-padding width
                                                        also changing between rounds -- root cause of the
                                                        previously-open "declaration-alignment column-padding
                                                        non-idempotency" gap (`ranges.hpp`'s `_Range` field,
                                                        `algorithm.hpp`, `filesystem.hpp`). Root cause:
                                                        `ScopePipelineCore.trailingIndent` returns the text
                                                        after a leading gap's last `\n` as the line's
                                                        indentation with no check that it's pure whitespace --
                                                        when the gap ends in a same-line leading comment
                                                        before the first declaration, that text got swept into
                                                        `indent`, which `applyDeclarationsPass`/
                                                        `applyAssignmentsPass`/
                                                        `applyOversizedAggregateInitClosingBracePass` all use
                                                        as the per-line join separator, duplicating the
                                                        comment onto every sibling line. Fixed by truncating
                                                        `trailingIndent`'s result at the first non-space/
                                                        non-tab character. Verified against the real
                                                        microsoft/STL tree (`ranges.hpp`, all 4 affected
                                                        `_Range` occurrences now idempotent and
                                                        comment-duplication-free). **Note:** a second,
                                                        distinct shape of the same gap (`filesystem.hpp`'s
                                                        `recursive_directory_iterator` assignment-alignment
                                                        group) was found in the same session -- see fixture
                                                        124 for that fix.

  real_code_regressions_123_inp/out.hpp              -- C++, `alignCommentSeparators` false-positive fix
                                                        (RDD_KEY_50/RDD_KEY_201 follow-up, jenkinsci/jenkins
                                                        `IdStrategy.java`-style repro): two adjacent trailing
                                                        `//` comments that each merely happen to contain one
                                                        incidental punctuation character flanked by spaces
                                                        (`// The @ can be used in local-part if quoted
                                                        correctly` / `// => the last @ is the one used to
                                                        separate the domain and local-part`) were wrongly
                                                        treated as a genuine STYLE.md §15 separator-alignment
                                                        pair and padded, corrupting the comment and going
                                                        non-idempotent. Fixed by `MiscRuleCore.looksCodeLike`,
                                                        a structural code-likeness check applied to each
                                                        candidate line's parsed label/rest before it's allowed
                                                        into a separator-alignment run: a fragment must have
                                                        at most 4 whitespace-separated words, be at most 24
                                                        characters, and contain no whole word (case-
                                                        insensitively, single-letter words exempted) from a
                                                        small common-English-stopword list; failing either
                                                        check breaks the run like any non-qualifying line.
                                                        This fixture also includes a genuine 2-line separator-
                                                        alignment pair (`// Count : 1` / `// GrandTotal : 22`)
                                                        to prove the fix doesn't regress real §15 alignment --
                                                        still padded (`Count      : 1` / `GrandTotal : 22`).
                                                        See `STATE_C_CPP_JAVA.md`.

  real_code_regressions_124_inp/out.hpp              -- C++, `filesystem.hpp` `recursive_directory_iterator`
                                                        assignment-alignment column-padding non-idempotency
                                                        (the second, distinct shape of fixture 122's gap): a
                                                        class with a zero-arg default ctor, a long copy-ctor
                                                        declaration whose too-long parameter list is later
                                                        wrapped across multiple physical lines by
                                                        `enforceCallLineBreaking` (RDD_KEY_86), a move-ctor,
                                                        and a destructor -- all `= default;` one-liners. On a
                                                        fresh format the long copy-ctor is still one raw line
                                                        and wrongly joins the group, its full width setting
                                                        the `=` column; on reformat the now-wrapped copy-ctor
                                                        no longer parses as a one-liner and is excluded,
                                                        shrinking the column -- non-idempotent. Root cause:
                                                        `GetterSetterRuleCurly.parseOneLinerMember`'s existing
                                                        breakable-width pre-check was gated only on
                                                        `isDefinition`, leaving non-definition (plain
                                                        declaration/pure-specifier) members with a breakable,
                                                        non-empty parameter list unchecked. Fixed by adding a
                                                        `hasBreakableParams` check (`!isDefinition &&
                                                        paramsFrom < paramsTo`) alongside the existing
                                                        `hasBreakableCall` check. See `STATE_C_CPP_JAVA.md`.

  real_code_regressions_125_inp/out.html             -- HTML5, `apache/ant` `manual/` dogfood, 2 bugs: (1) a
                                                        `<p>` with no explicit `</p>` before a following
                                                        block-level sibling (`<h3>`) swallowed the rest of the
                                                        document as its children until an unrelated downstream
                                                        closing tag, producing a spurious duplicate `</p>` at
                                                        EOF -- fixed by registering `p` in
                                                        `XmlSpecificRule.IMPLIED_CLOSE_TRIGGERS` per the HTML5
                                                        spec's "close a p element" trigger-tag list
                                                        (RDD_KEY_204). (2) a same-line trailing comment after
                                                        a `<td>`'s sole text child (e.g. `<td>text<!-- c
                                                        --></td>`) was silently dropped -- attached correctly
                                                        as the text node's `trailingComment`, but two render
                                                        paths (`renderNode`'s `TEXT` case, and
                                                        `renderElement`'s "sole content child" fast path
                                                        reading `onlyChild.raw` directly) never consulted it;
                                                        both fixed (RDD_KEY_205). See `STATE_DATA_FORMATS.md`.

  real_code_regressions_126_inp/out.java             -- Java, `apache/ant` `src/` dogfood: a braced
                                                        single-statement `if` body that is itself a local
                                                        variable declaration (`final boolean ignored =
                                                        f.setWritable(true);`) was collapsed to a braceless
                                                        `if`, which javac rejects (a declaration is not a
                                                        legal braceless if/while/for body) -- fixed by
                                                        refusing collapse in
                                                        `BlockStructureRule.isSingleStatementBody` whenever
                                                        the body's first token is `final`/`const`. See
                                                        `STATE_C_CPP_JAVA.md`.

  real_code_regressions_127_inp/out.py               -- Python3, `django/django` real-code dogfood: a §8
                                                        single-statement-body `match`/`case` header with its
                                                        own trailing comment (e.g. `case Sequence():  # str
                                                        and bytes were already handled.`) qualified for
                                                        joining with its body line, but the join's
                                                        `headerText` only spanned up to the header's `:`,
                                                        silently deleting the comment -- real content loss
                                                        (surfaced via `diff -rq round1 round2` since round2 no
                                                        longer had the comment to drop). Fixed in
                                                        `ScopePipelineIndent`:
                                                        `classifySingleStatementHeaderColon` and
                                                        `classifyCaseLine` both now bail from the join
                                                        whenever a trailing comment follows the header's `:`,
                                                        mirroring the existing skip for a body statement's own
                                                        trailing comment. See `STATE_PYTHON3.md`'s
                                                        `django/django` entry.

  real_code_regressions_128_inp/out.java             -- Java, `openrewrite/rewrite` dogfood, 2 bugs sharing
                                                        one root-cause shape (fits-prediction made before a
                                                        later width-growing pass, correct fresh but wrong on
                                                        reformat): (a) `JavaSpecificRule.isSingleLineBody`
                                                        measured a tab-indented one-liner's indent via raw
                                                        `String.length()` instead of expanded width, wrongly
                                                        predicting "fits" and staying K&R on round1, flipping
                                                        to Allman on round2 after `enforceCallLineBreaking`
                                                        wrapped it -- fixed with local `expandedIndentWidth`
                                                        helper (same formula as `MiscRuleCore`'s). (b)
                                                        `enforceInitializerBraceSpacing`'s Phase-4 `{ x }`
                                                        padding ran after `enforceCallLineBreaking` had
                                                        already decided not to wrap, so a near-limit
                                                        annotation argument grew past `line-length` once
                                                        padded, wrapping only on round2 -- fixed by pulling a
                                                        second `enforceInitializerBraceSpacing` call forward
                                                        to run right before `enforceCallLineBreaking` (same
                                                        pull-forward pattern as `enforceComplexityPadding`/
                                                        `enforceAttributeAndSpliceBracketPadding`), original
                                                        Phase 4 call left in place too. See
                                                        `STATE_C_CPP_JAVA.md`'s `openrewrite/rewrite` entry.

  real_code_regressions_129_inp/out.java             -- Java, `openrewrite/rewrite` dogfood
                                                        (`rewrite-benchmarks/MethodMatcherBenchmark.java` + 7
                                                        siblings): a `.map(name -> { if/else-if chain })`
                                                        lambda's branches render on own lines fresh but fully
                                                        fuse on reformat -- genuine non-fixed-point flap,
                                                        distinct from _128's shape. Root cause:
                                                        `BlockStructureRule.collapseSingleExpressionBlocks`'s
                                                        per-branch newline before a chain's next `else`
                                                        (`appendChainNewlineBeforeElse`) was only inserted as
                                                        a side effect of collapsing a *braced* if/else-if
                                                        body; an already-braceless body (round2 input) left no
                                                        brace to re-collapse, so no newline re-inserted, and
                                                        `ScopePipelineCurly`'s decl/assignment-RHS pass fused
                                                        the whole chain. Fixed by adding a C/C++/Java sibling
                                                        of the existing Kotlin "already-braceless multi-line
                                                        body" branch: `matchControlBlock` now copies a
                                                        braceless if/else-if body through verbatim (new
                                                        `findBracelessStatementEnd` helper) and still invokes
                                                        `appendChainNewlineBeforeElse` after. See
                                                        `STATE_C_CPP_JAVA.md`'s `openrewrite/rewrite` entry.

  real_code_regressions_130_inp/out.java             -- Java, `openrewrite/rewrite` dogfood
                                                        (`rewrite-core/AdaptiveRadixTreeTest.java`, cluster
                                                        3): a `for(...; ++i)` header's pre-increment stays
                                                        tight (`++i`) fresh but gains a stray space (`++ i`)
                                                        on reformat, once the enclosing lambda body collapses
                                                        onto one line and the `for`-header re-renders through
                                                        the shared tight-attachment join point. Root cause:
                                                        neither `MiscRuleCore. needsSpaceBetween` nor its
                                                        duplicate
                                                        `DeclarationAlignmentRuleCore.needsSpaceBetween` had a
                                                        case for prefix `++`/`--` immediately followed by an
                                                        identifier -- `MiscRuleCurly.enforcePreIncrement`'s
                                                        swap-render path produces the tight join fresh, but
                                                        once text is already prefix form,
                                                        `collectForIncrementSpans` no longer detects it as a
                                                        swap candidate, so the collapsed one-line lambda's
                                                        later re-render falls through to generic
                                                        space-by-default. Fixed by adding a tight-join case to
                                                        both methods. See `STATE_C_CPP_JAVA.md`'s
                                                        `openrewrite/rewrite` entry.

  real_code_regressions_131_inp/out.java             -- Java, `openrewrite/rewrite` dogfood
                                                        (`rewrite-java-{8,11,17,21,25}`'s
                                                        `ReloadableJava*ParserVisitor.java`, cluster 4): a
                                                        trailing `//` comment's column, in an
                                                        assignment-alignment group whose RHS spans more than
                                                        STYLE.md §6's supported single-newline shape, drifts
                                                        between fresh format and reformat. Root cause:
                                                        `MiscRuleCore.parseAssignment`'s verbatim fallback
                                                        (value with >1 embedded newline, or a single newline
                                                        `classifyMultiLineBreak` doesn't recognize) returns an
                                                        ordinary non-`multiLine` `Assignment` whose
                                                        `valueTokens` still holds embedded `NEWLINE`s --
                                                        `MiscRuleCore.render` fed that row's `joinVerbatim`
                                                        text straight into `ColumnGrid`, whose
                                                        `String.length()` column-width computation counted the
                                                        whole wrapped call, not just its first line,
                                                        corrupting the group's column width non-idempotently
                                                        (verbatim text length can shift between passes). Fixed
                                                        by adding `valueSpansMultipleLines` and excluding such
                                                        rows from the grid the same way `a.multiLine` rows
                                                        already are, rendering directly instead. See
                                                        `STATE_C_CPP_JAVA.md`'s `openrewrite/rewrite` entry.

  real_code_regressions_132_inp/out.java             -- Java, `openrewrite/rewrite` dogfood
                                                        (`rewrite-python/.../Autodetect.java`'s
                                                        `visitCollectionLiteral`, cluster 6): a still-K&R `}
                                                        else if (...) {` block's closing `}` drifted
                                                        indentation between fresh format and reformat. Root
                                                        cause: `ScopePipelineCurly.findParentIndent` returned
                                                        null for an `else`/`catch`/`finally` keyword still
                                                        sharing its physical line with the preceding block's
                                                        closing `}` (pre- `placeElseOnOwnLine`), so
                                                        `processScope`'s trailing-gap force-reindent was
                                                        skipped fresh, leaving the closing brace untouched --
                                                        but on reformat the keyword sits on its own Allman
                                                        line, so `findParentIndent` returns a real indent and
                                                        the force-reindent fires non-idempotently. Fixed by
                                                        deriving the indent from the preceding `}`'s own
                                                        physical line (via `braceLineIndent`) whenever an
                                                        else/catch/finally keyword directly follows a `}` --
                                                        that `}` is always at the nesting depth the keyword
                                                        belongs at once on its own line, so both passes agree.
                                                        See `STATE_C_CPP_JAVA.md`'s `openrewrite/rewrite`
                                                        entry.

  real_code_regressions_133_inp/out.py               -- Python, `python/cpython` dogfood
                                                        (`Lib/test/test_fstring.py`,
                                                        `test_format_specifier_expressions`-shaped cases): a
                                                        nested replacement field inside an f-string format
                                                        spec whose own expression is a quoted string
                                                        containing `{`/`}` (e.g. `f'{2:{"{"}>10}'`) crashed
                                                        with an `IndexOutOfBoundsException`. Root cause:
                                                        `TokenizerIndent.emitFStringFormatSpec`'s brace-depth
                                                        counter scanned raw characters without skipping
                                                        quoted-string content, so the literal `{`/`}` inside
                                                        the nested field's string expression miscounted depth
                                                        -- the nested field's real closing `}` only
                                                        decremented the phantom depth, and the scan for the
                                                        format spec's true closing `}` ran past the field end,
                                                        producing a single `FSTRING_FORMAT_SPEC` token
                                                        spanning to EOF with no `FSTRING_END` --
                                                        `ScopePipelineIndent.processField` then walked off the
                                                        end of the token list. Fixed by adding
                                                        `skipNestedStringLiteral` (skips quoted-string
                                                        content, escapes/triple-quotes honored, mirroring
                                                        `emitSimpleString`/`emitTripleQuotedString`) whenever
                                                        a quote is seen at `depth > 0` inside the brace
                                                        counter. `make test`: 182/182 forward + 182/182
                                                        idempotency. See `STATE_PYTHON3.md`'s `python/cpython`
                                                        entry.

  real_code_regressions_134_inp/out.ts               -- TS, `angular/angular` dogfood
                                                        (`override_rename_ts_plugin.ts`/`template_target.ts`/
                                                        `checker.ts`, critical cluster 1): an arrow function's
                                                        dotted/qualified return type or type predicate
                                                        (`ts.server.PluginModule =>`, `node is tss.Node =>`,
                                                        `diag is ts.Diagnostic =>`) had its last segment
                                                        wrapped in a spurious paren pair, producing invalid TS
                                                        (`ts.server.(PluginModule) =>`). Root cause:
                                                        `JsTsSpecificRule.enforceArrowFunctionParameterParens`'s
                                                        bail-out (added for a single-segment `vuejs/core`
                                                        case) checked only the token immediately preceding the
                                                        arrow-parameter identifier for
                                                        `:`/`is`/`typeof`/`keyof`, missing a multi-segment
                                                        dotted chain where that predecessor is a `.` instead.
                                                        Fixed by walking backward over any number of
                                                        `IDENTIFIER '.'` pairs first, so the check lands on
                                                        what precedes the WHOLE chain's first segment. Bare
                                                        single-param arrows (`n => n + 1`) still correctly
                                                        wrapped. `make test`: 183/183 forward + 183/183
                                                        idempotency. See `STATE_JS_TS.md`'s `angular/angular`
                                                        entry.

  real_code_regressions_135_inp/out.ts               -- TS, `angular/angular` dogfood (`testability.ts:243`,
                                                        critical cluster 2): a legacy angle-bracket cast
                                                        (`<WaitCallback>{...}`) had a bogus `;` injected right
                                                        before the object literal's own closing `}`
                                                        (`updateCb: updateCb;});`). Root cause: the cast's
                                                        `<Type>` is never reclassified to
                                                        ANGLE_BRACKET_OPEN/CLOSE by
                                                        `TokenizerCurly.reclassifyAngleBrackets` (tracks only
                                                        a `<` preceded by an IDENTIFIER, i.e. a generic clause
                                                        -- a cast's `<` instead follows an expression-start
                                                        token like `(`), so the object literal after the cast
                                                        fell through `JsTsSpecificRule.classifyBraces`'s
                                                        default-false case, misclassified as a statement-body
                                                        brace with depth reset to 0 mid-expression --
                                                        triggering `enforceSemicolonInsertion` to insert a
                                                        bogus `;`. Fixed by adding `isLegacyCastBrace`,
                                                        recognizing a `{` immediately preceded by a plain OP
                                                        `>` whose matching `<` sits before a (optionally
                                                        dotted) type name following a value-starting token,
                                                        treated as a value/pattern brace among
                                                        `classifyBraces`'s `isValue` disjuncts. `make test`:
                                                        184/184 forward + 184/184 idempotency. See
                                                        `STATE_JS_TS.md`'s `angular/angular` entry.

  real_code_regressions_136_inp/out.ts               -- TS, `angular/angular` dogfood
                                                        (`private/testing/src/utils.ts:102-105`, critical
                                                        cluster 3): a multi-line generic return-type clause
                                                        (`Promise<\n  (typeof import('...'))['default'] |
                                                        null\n>`) got a bogus `;` inserted at the end of the
                                                        line containing the `typeof import(...)` type-query
                                                        clause. Root cause: TS's dynamic-import type-query
                                                        operand (`import(...)` as a type operand) is a KEYWORD
                                                        token not in `TokenizerCurly.GENERIC_SAFE_KEYWORDS` --
                                                        invalidating the whole enclosing `<...>` tracking
                                                        before the matching `>`, leaving it a plain OP token
                                                        and defeating
                                                        `JsTsSpecificRule.enforceSemicolonInsertion`'s depth
                                                        tracking (same gap class as the earlier `keyof`/`is`/
                                                        `infer`/`typeof` fixes in this set). Fixed by adding
                                                        `"import"` to `GENERIC_SAFE_KEYWORDS`. `make test`:
                                                        185/185 forward + 185/185 idempotency. See
                                                        `STATE_JS_TS.md`'s `angular/angular` entry.

  real_code_regressions_137_inp/out.py               -- Python, `python/cpython` dogfood
                                                        (`Lib/random.py:53-56`, idempotency cluster 2): four
                                                        `from math import ...` statements for the same module
                                                        didn't fully alphabetize inter-statement order fresh
                                                        (within-clause order was already correct); a second
                                                        format self-corrected it. Root cause:
                                                        `MiscRuleIndent.PyImport.compareTo` compared its
                                                        `names` list element-by-element in as-parsed
                                                        (pre-within-clause-sort) order -- only after a
                                                        round-trip did `names` happen to match the sorted form
                                                        needed. Fixed by sorting a copy of each side's `names`
                                                        before comparing, matching §3.1 point 3's "sort by
                                                        first imported name" read as "first name after
                                                        within-clause alphabetization." `make test`: 186/186
                                                        forward + 186/186 idempotency. See
                                                        `STATE_PYTHON3.md`'s `python/cpython` entry.

  real_code_regressions_138_inp/out.py               -- Python, `python/cpython` dogfood (`Lib/turtle.py`'s
                                                        `match param.kind` block, idempotency cluster 3): a
                                                        run of block-form `case` members (each individually
                                                        §8-joined to one line) had `:`-column alignment
                                                        abandoned round1 (one member, `case _:`, needs padding
                                                        to match a longer sibling that would overflow
                                                        `line-length` if padded+joined, so §7 correctly
                                                        abandons alignment, leaving each member
                                                        joined-but-unaligned by §8), but round2 saw the
                                                        now-compact members and aligned them anyway. Root
                                                        cause: `flushCaseGroup`'s pre-commit length-budget
                                                        check only covered `virtualJoin` members, not
                                                        already-compact ones, so the over-length padding
                                                        round1 correctly rejected got applied round2. Fixed by
                                                        extending the check to cover every group member
                                                        uniformly. `make test`: 187/187 forward + 187/187
                                                        idempotency. See `STATE_PYTHON3.md`'s `python/cpython`
                                                        entry.

  real_code_regressions_139_inp/out.py               -- Python, `python/cpython` dogfood
                                                        (`Lib/test/test_ctypes/test_generated_structs.py`
                                                        lines 278/284, idempotency cluster 4):
                                                        `@register(f'Struct331_{signedness}{n}',
                                                        set_name=True)` -- two adjacent f-string fields with
                                                        no literal text between them -- got its second field's
                                                        `{`/`}` loose-padded to `{ n }` round1 as an ordinary
                                                        dict/set literal, since `applyBracketPadding`'s
                                                        f-string-field guard only recognized a field-open `{`
                                                        immediately preceded by FSTRING_START/FSTRING_MIDDLE,
                                                        and CPython's own FSTRING_MIDDLE emission (mirrored
                                                        here) skips that token when literal text between two
                                                        fields is empty, leaving the second field's `{`
                                                        preceded by the first field's closing `}` (plain
                                                        PUNCT) instead. Round2's `applyFStringSpacing` then
                                                        trimmed the field back to `{n}` but left the outer
                                                        decorator-call paren padding untouched --
                                                        non-idempotent. Same bug class as already-fixed
                                                        `pallets/click` case (fixture
                                                        `real_code_regressions_80`), triggered by field
                                                        adjacency rather than nesting depth. Fixed by tracking
                                                        the previous f-string field's close position in
                                                        `applyBracketPadding`'s loop and treating a `{`
                                                        immediately following it as another field open. `make
                                                        test`: 188/188 forward + 188/188 idempotency. See
                                                        `STATE_PYTHON3.md`'s `python/cpython` entry.

  real_code_regressions_140_inp/out.ts               -- TS, `angular/angular` dogfood
                                                        (`packages/router/src/create_router_state.ts:27`,
                                                        idempotency cluster 4, root cause #1 of 2):
                                                        `enforceCallLineBreaking`'s single-line-collapse
                                                        candidate (`renderCallDropped`/`renderCallOnePerLine`)
                                                        measured a call's width via `splitTopLevelCommas`,
                                                        which -- unlike sibling `groupByOriginalLine` -- does
                                                        not drop a dangling trailing empty group from a
                                                        trailing comma before `)`. A multi-line call with a
                                                        trailing comma on its last argument measured one
                                                        comma+space wider fresh than on reformat (which never
                                                        emits a trailing comma), flipping the fits-check at
                                                        the boundary -- non-idempotent. Fixed by adding the
                                                        same dangling-trailing-empty-group drop already used
                                                        elsewhere to both `renderCallDropped` and
                                                        `renderCallOnePerLine`. See `STATE_JS_TS.md`'s cluster
                                                        4 entry.

  real_code_regressions_141_inp/out.ts               -- TS, `angular/angular` dogfood
                                                        (`packages/core/src/render3/node_selector_matcher.ts:155`,
                                                        idempotency cluster 4, root cause #2 of 2):
                                                        `enforceCallLineBreaking`'s fits-check for a call
                                                        embedded in an `if (...)` condition measured the line
                                                        including the keyword-to-paren gap (`if (`) fresh,
                                                        since `enforceKeywordSpacing` (collapses to `if(`)
                                                        originally ran only in Phase 4, after this fits-check.
                                                        Reformat of already-Phase-4-processed output saw the
                                                        gap already collapsed, one character narrower --
                                                        enough to flip a candidate sitting exactly at the
                                                        line-length boundary. Fixed by pulling
                                                        `enforceKeywordSpacing` forward to run immediately
                                                        before the first `enforceCallLineBreaking` call in
                                                        `FormatterCurly.formatOne`, same "measurement must see
                                                        final width" pattern as `enforceComplexityPadding`/
                                                        `enforceAttributeAndSpliceBracketPadding`/
                                                        `enforceInitializerBraceSpacing`; original Phase 4
                                                        call left in place too. Applies to all curly-brace
                                                        languages (C/C++/Java/Kotlin/JS/TS) since
                                                        `enforceKeywordSpacing` is shared -- full `make test`
                                                        re-run confirmed no regressions. `make test`: 190/190
                                                        forward + 190/190 idempotency. See `STATE_JS_TS.md`'s
                                                        cluster 4 entry.

  real_code_regressions_142_inp/out.ts               -- TS, `angular/angular` dogfood
                                                        (`packages/common/upgrade/src/location_shim.ts:461`,
                                                        idempotency cluster 4, root cause #4 of 4): the JS/TS
                                                        tight-candidate fits-check in
                                                        `enforceCallLineBreaking` counted a trailing same-line
                                                        comment's width toward the collapse candidate's length
                                                        fresh (call+comment on one source line), but not on
                                                        reformat (comment moved to its own line after `)`) --
                                                        whether the comment counted depended on prior wrap
                                                        state, not actual final width. Fixed by adding
                                                        `appendRangeCollapsingTrailingCommentGap` next to
                                                        `appendRange` in `MiscRuleCurly.java`: a whitespace
                                                        run immediately before a trailing line comment
                                                        collapses to a single space for measurement only
                                                        (never rendered) -- that gap is comment-column
                                                        alignment padding, not real structural width. Verified
                                                        against the real `location_shim.ts` file and a minimal
                                                        repro; `make test`: 191/191 forward + 191/191
                                                        idempotency. A separate, not-yet-fixed root cause (#3,
                                                        braceless-else bodies never re-validated after
                                                        brace-collapse) remains open -- an attempted fix was
                                                        tried and reverted after breaking 5 existing
                                                        fixtures/dogfood cases. See `STATE_JS_TS.md`'s cluster
                                                        4 entry for both.

  real_code_regressions_143_inp/out.ts               -- TS, `microsoft/TypeScript` dogfood cluster 1: `||=`/
                                                        `&&=` were missing from
                                                        `TokenizerCurly.MULTI_CHAR_OPS`, silently splitting
                                                        into two tokens (`||` + `=`). Fixed by adding both
                                                        entries. `make test`: 191/191 forward + 191/191
                                                        idempotency. See `STATE_JS_TS.md`'s dogfood section,
                                                        cluster 1.

  real_code_regressions_144_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C1: an
                                                        own-line comment immediately preceding a constructor
                                                        parameter got fused onto the comment's line, silently
                                                        swallowing the parameter (valid-but-wrong Kotlin, no
                                                        parse error). Fixed in `KotlinSignatureRule.
                                                        parseKotlinSignature`. `make test`: 192/192 forward +
                                                        192/192 idempotency. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster C1.

  real_code_regressions_145_inp/out.ts               -- JS/TS, `microsoft/TypeScript` dogfood category 1
                                                        cluster #2: a union-type return-type/type-predicate
                                                        before `=>` had its last bare-identifier segment
                                                        wrapped in a spurious paren pair, a real parse error
                                                        -- same function as the already-fixed dotted-chain fix
                                                        (fixture 134) but the walk-back didn't cover a leading
                                                        `|`. Fixed in `enforceArrowFunctionParameterParens`.
                                                        `make test`: 193/193 forward + 193/193 idempotency.
                                                        See `STATE_JS_TS.md`'s Dogfood: microsoft/TypeScript
                                                        section, category 1 cluster #2.

  real_code_regressions_146_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C3 (largest
                                                        crash cluster, ~70+ files): a named-argument lambda
                                                        body with multiple statements got fused onto one
                                                        physical line with no separators --
                                                        `splitTopLevelCommas` doesn't track brace depth, so a
                                                        lambda's own param-list commas were misread as
                                                        call-argument separators. Fixed via new
                                                        brace-depth-aware `splitTopLevelCommasBraceAware` in
                                                        `MiscRuleCurly`. `make test`: 195/195 forward +
                                                        195/195 idempotency. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster C3.

  real_code_regressions_147_inp/out.ts               -- JS/TS, `microsoft/TypeScript` dogfood category 1
                                                        cluster #4: a plain double-quoted string literal
                                                        continued across CRLF-terminated lines via a trailing
                                                        `\` got corrupted -- `TokenizerCurly.emitString`'s
                                                        2-char backslash-escape skip only consumed the `\r`
                                                        half of a `\r\n` pair. Fixed by special-casing `\` +
                                                        `\r\n` as a 3-char skip. Deliberately contains real
                                                        CRLF bytes -- `.gitattributes` marks the `_inp.ts`
                                                        file `-text` so git doesn't normalize it. `make test`:
                                                        196/196 forward + 196/196 idempotency. See
                                                        `STATE_JS_TS.md`'s Dogfood: microsoft/TypeScript
                                                        section, category 1 cluster #4.

  real_code_regressions_148_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C5: a `when
                                                        (subject) { ... }` whose subject got line-wrapped by
                                                        an earlier structural pass before
                                                        `formatWhenExpressions` ran, so the raw
                                                        (newline-containing) subject text leaked into the `//
                                                        when <subject>` closing comment, corrupting everything
                                                        after it. Fixed by collapsing whitespace runs in the
                                                        captured subject to a single space in
                                                        `KotlinSpecificRule.formatWhenExpressions`. `make
                                                        test`: 197/197 forward + 197/197 idempotency. See
                                                        `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin section,
                                                        cluster C5 (also closed cluster C4 as a miscategorized
                                                        instance of this same bug). **2026-08-11 update**:
                                                        this fixture's own checked-in expected output had, by
                                                        coincidence, already baked in a second, distinct bug
                                                        in the same closing-comment mechanism -- the collapsed
                                                        subject text kept a spurious space directly inside a
                                                        wrapped call's own parens (`min( left, 2 )` instead of
                                                        `min(left, 2)`) whenever `enforceCallLineBreaking`'s
                                                        wrap placed a NEWLINE immediately after `(` or before
                                                        `)`. Fixed by stripping whitespace adjacent to a paren
                                                        after the general collapse; `_out.kt` corrected to
                                                        `min(left, 2)`/`min(right, 3)`. `make test`: 278/278
                                                        forward + 278/278 idempotency. See `STATE_KOTLIN.md`'s
                                                        "when-closing-comment paren/comma spacing" entry.

  real_code_regressions_149_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C2: an
                                                        annotation at expression position (`val lambda =
                                                        @JsNoLifting { ... }`) got a spurious space after `@`
                                                        -- `DeclarationAlignmentRuleCore.needsSpaceBetween` (a
                                                        separate duplicate of `MiscRuleCore`'s
                                                        tight-attachment rules) lacked the `@`-tight case.
                                                        Fixed by adding the same rule there. `make test`:
                                                        198/198 forward + 198/198 idempotency. See
                                                        `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin section,
                                                        cluster C2.

  real_code_regressions_150_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6a: a
                                                        typed `by`-delegate declaration has no `=` token, so
                                                        `KotlinDeclarationAlignmentRule.parseKotlinDeclaration`'s
                                                        type-scan loop (which only stopped on `=`) swept the
                                                        entire delegate expression -- including a
                                                        multi-statement trailing lambda -- into `typeTokens`,
                                                        which then got flattened onto one line with no
                                                        separators. Fixed by bailing out (verbatim render) on
                                                        a top-level `by` keyword, same posture as the existing
                                                        get/set bailout. `make test`: 198/198 forward +
                                                        198/198 idempotency (also fixed a related latent
                                                        spacing bug in `real_code_regressions_30_out.kt`). See
                                                        `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin section,
                                                        cluster C6a.

  real_code_regressions_151_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6c: a
                                                        nullable-type callable reference (`Array<*>?::get`)
                                                        was mis-split by `TokenizerCurly`'s greedy `"?:"`
                                                        elvis-operator match into elvis `?:` + stray `:`.
                                                        Fixed by bailing out of the `"?:"` match when the
                                                        source also has a third `:` (`?::`), letting `?` and
                                                        `::` tokenize separately. `make test`: 199/199 forward
                                                        + 199/199 idempotency. See `STATE_KOTLIN.md`'s
                                                        Dogfood: JetBrains/kotlin section, cluster C6c.

  real_code_regressions_152_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6j: a
                                                        square-bracket destructuring lambda param list (`{ [x,
                                                        y] -> ... }`) lost its space after `{` because
                                                        `isTightToken` treats `[` as always-tight (an
                                                        indexing-shape rule). Fixed by a narrow `{`
                                                        -directly-before-`[` carve-out in both
                                                        `MiscRuleCore.needsSpaceBetween` and
                                                        `DeclarationAlignmentRuleCore.needsSpaceBetween`
                                                        (Kotlin has no bracket array-literal syntax, so this
                                                        join is unambiguous). `make test`: 200/200 forward +
                                                        200/200 idempotency. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster C6j.

  real_code_regressions_153_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6d: an
                                                        annotation directly ahead of a function-type literal
                                                        (`@Composable (Params) -> Type`, as a parameter type
                                                        or property type) lost its required space -- the
                                                        general call-tight rule (`IDENTIFIER` immediately
                                                        before `(` is always tight, correct for
                                                        `@Composable(x)`'s own annotation-argument-list shape)
                                                        fired first and wrongly tightened this case too, since
                                                        both shapes are `IDENTIFIER` then `(` at the same
                                                        join. Fixed by a new lookback+lookahead carve-out (is
                                                        `prev` itself an annotation name immediately preceded
                                                        by `@`? is its `(...)`'s matching `)` followed by
                                                        `->`, i.e. actually a function type?) added ahead of
                                                        the general rule in
                                                        `DeclarationAlignmentRuleCore.needsSpaceBetween` and
                                                        `MiscRuleCore.needsSpaceBetween` (both gained a
                                                        `List<Token> tokens, int curIdx` overload for the
                                                        lookahead) and in `KotlinDeclarationAlignmentRule`'s
                                                        own `renderTokens` override (updated to pass the new
                                                        overload through). `make test`: 201/201 forward +
                                                        201/201 idempotency. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster C6d.

  real_code_regressions_154_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6g: a
                                                        backtick-quoted identifier containing a literal
                                                        `(`/`)` (the common JetBrains test-name idiom, e.g. ``
                                                        fun `parses correctly (no debug info)`() ``) wasn't
                                                        recognized as an opaque span at all -- unlike JS/TS's
                                                        backtick-delimited template literal, no `c == '`'`
                                                        branch matched for Kotlin in `TokenizerCurly`'s
                                                        dispatch loop, so the backtick itself fell through to
                                                        `emitOperator()` and the identifier's interior was
                                                        re-tokenized character-by-character, with any embedded
                                                        `(`/`)` emitted as real bracket tokens that corrupted
                                                        downstream paren-depth tracking. Fixed by a new
                                                        `TokenizerCurly.emitKotlinBacktickIdentifier`, gated
                                                        on `lang.isKotlin`, mirroring `emitTemplateLiteral`'s
                                                        existing opaque-span treatment: the whole backtick
                                                        span (byte-for-byte) becomes a single `IDENTIFIER`
                                                        token, no interior re-tokenization. `make test`:
                                                        203/203 forward + 203/203 idempotency. See
                                                        `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin section,
                                                        cluster C6g.

  real_code_regressions_155_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6i: a
                                                        headerless one-liner interface member (`fun clear():
                                                        Unit`) directly followed by an unrelated named
                                                        construct's own brace got its `)`/`:` wrongly matched
                                                        as that brace's `: ReturnType` tail, fusing the two
                                                        statements onto one line. Fixed with a
                                                        `hasTopLevelNewline` bail in
                                                        `ScopePipelineCurly.applySignaturePass`. `make test`:
                                                        203/203 forward + 203/203 idempotency. RDD_KEY_206.
                                                        See `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin
                                                        section, cluster C6i.

  real_code_regressions_156_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6f: two
                                                        distinct multi-line-collapse passes that flattened an
                                                        embedded `//` line comment onto the same physical line
                                                        as the code that followed it, silently swallowing that
                                                        code into the comment's text. (1)
                                                        `KotlinSignatureRule.parseKotlinParam`'s
                                                        column-aligned grid rendering of a function
                                                        parameter's default value flattened the value's tokens
                                                        (including any embedded `//` comment) via the
                                                        comment-unaware `renderTokens` helper -- fixed by a
                                                        new `containsLineComment` bail in `parseKotlinParam`,
                                                        returning null (leave the whole signature untouched)
                                                        whenever a param's default-value slice contains one.
                                                        (2) `BlockStructureRule.tryCollapseBraceless`'s
                                                        sibling-but-distinct condition-flattening path
                                                        (`renderInline`) had no comment guard at all, unlike
                                                        its braced-body sibling `tryCollapse` (which already
                                                        guarded its own condition render) -- a comment nested
                                                        arbitrarily deep inside the condition (e.g. inside a
                                                        trailing-lambda argument of a call within the
                                                        condition) reached this method's
                                                        `renderInline(tokens.subList(kwIndex,
                                                        closeParenIndex+1))` call unguarded. Fixed by adding
                                                        the exact same `containsLineComment` bail
                                                        `tryCollapse` already had, to `tryCollapseBraceless`
                                                        too. Both fixes verified against the real
                                                        `JetBrains/kotlin` corpus (`ResolutionTesting.kt` and
                                                        `KClassMembers.kt` respectively) via
                                                        `kotlin_syntax_check`; a 70-file cross-cluster sample
                                                        went from 22/70 to 40/70 syntax-clean after both fixes
                                                        (remaining failures are other, unrelated C6-series
                                                        clusters plus one further not-yet-fixed C6f sub-shape
                                                        -- see `STATE_KOTLIN.md`'s C6f row). `make test`:
                                                        204/204 forward + 204/204 idempotency. See
                                                        `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin section,
                                                        cluster C6f.

  real_code_regressions_157_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6f, third
                                                        and final shape: an expression-bodied function whose
                                                        `=` is followed by a run of standalone `//` comment
                                                        lines before the real expression body
                                                        (`AbstractNativeBlackBoxTest.kt`'s
                                                        `buildJUnitDynamicNodes`). `KotlinSignatureRule.
                                                        renderWithTail` renders the tail's expression tokens
                                                        via the comment-unaware `renderTokens` helper (same
                                                        mechanism as this cluster's already-fixed shape (1)),
                                                        fusing every leading comment line plus the first line
                                                        of the real expression onto one physical line. Fixed
                                                        by a new `containsLineComment` bail in
                                                        `KotlinSignatureRule.parseFunctionTail`, returning
                                                        null whenever the expression-body slice contains a
                                                        `COMMENT_LINE` -- `ScopePipelineCurly` already treats
                                                        a null tail like a null `KotlinSignature` and leaves
                                                        the whole span untouched, no caller change needed
                                                        beyond adding the null check. Verified against the
                                                        real `AbstractNativeBlackBoxTest.kt` via
                                                        `kotlin_syntax_check` (clean). `make test`: 205/205
                                                        forward + 205/205 idempotency (no count change --
                                                        corruption fix to already-covered functionality, not a
                                                        new formatting rule). See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster C6f.

  real_code_regressions_158_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6b: Kotlin
                                                        2.4's multi-dollar string interpolation prefix
                                                        (`$$"..."`, `$$$"""..."""`) got a spurious space
                                                        before the string it prefixes. Fixed with an
                                                        `isDollarRun` carve-out in
                                                        `MiscRuleCore.needsSpaceBetween` and
                                                        `DeclarationAlignmentRuleCore.needsSpaceBetween`, same
                                                        fix shape as C2's `@`-tight carve-out. `make test`:
                                                        206/206 forward + 206/206 idempotency. RDD_KEY_207.
                                                        See `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin
                                                        section, cluster C6b.

  real_code_regressions_159_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6e: a
                                                        multi-statement trailing-lambda body (`.all { ... }`/
                                                        `.any { ... }`) used as a boolean sub-expression
                                                        inside an `if(...)` condition got fused with no
                                                        separator when the `if` collapsed to single-statement
                                                        form -- same family as C3, but the condition (not just
                                                        the body) was unguarded. Fixed by reusing
                                                        `containsMultilineNestedBrace` as a bail guard on the
                                                        condition slice in
                                                        `BlockStructureRule.tryCollapse`/`tryCollapseBraceless`.
                                                        `make test`: 207/207 forward + 207/207 idempotency.
                                                        RDD_KEY_208. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster C6e.

  real_code_regressions_160_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6k, Shape
                                                        C6k-3: the `!is`/`!in` negated-operator carve-out
                                                        (RDD_KEY_144(A)) existed only in
                                                        `DeclarationAlignmentRuleCore.needsSpaceBetween`, not
                                                        its documented duplicate
                                                        `MiscRuleCore.needsSpaceBetween` -- a parameter
                                                        default value renders through the latter, corrupting
                                                        `!is` into `! is`. Fixed by adding the same carve-out
                                                        there. `make test`: 208/208 forward + 208/208
                                                        idempotency. RDD_KEY_209. See `STATE_KOTLIN.md`'s
                                                        Dogfood: JetBrains/kotlin section, cluster C6k.

  real_code_regressions_161_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6k, Shape
                                                        C6k-4: `KotlinSpecificRule.
                                                        enforceNullSafetyOperatorSpacing`'s `!!` tightness
                                                        applied unconditionally on the right, stripping a
                                                        required space before an ordinary keyword/operator
                                                        continuation (`port!! in range` -> `port!!in ...`, a
                                                        parse error) instead of only before a postfix chain
                                                        (`x!!.foo()`). Fixed with a new
                                                        `isPostfixNullOpContinuation` check narrowing `!!`'s
                                                        right-side tightness to `.`/`[`/`(`/`?.`/`!!` only.
                                                        `make test`: 209/209 forward + 209/209 idempotency.
                                                        RDD_KEY_210. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster C6k.

  real_code_regressions_162_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6k, Shape
                                                        C6k-5: the C6d fix (space before an annotated
                                                        function-type's parens) didn't fire for a *nullable*
                                                        function type (`@Composable( () -> Unit )?`), since
                                                        its outer paren's matching `)` is followed by `?`, not
                                                        `->`, and `isAnnotationFunctionTypeParen`'s lookahead
                                                        only recognized `->`. Fixed by also accepting `?`
                                                        there, in both
                                                        `DeclarationAlignmentRuleCore`/`MiscRuleCore`
                                                        duplicate copies. `make test`: 210/210 forward +
                                                        210/210 idempotency. RDD_KEY_211. See
                                                        `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin section,
                                                        cluster C6k.

  real_code_regressions_163_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6k, Shape
                                                        C6k-1 (multi-statement fusion), two independent root
                                                        causes in one fixture. (a)
                                                        `BlockStructureRule.isSingleStatementBody` routed
                                                        Kotlin through `isKotlinSingleStatementBody` only when
                                                        `semiCount != 1`, wrongly collapsing a body with two
                                                        newline-delimited statements plus one `;`-terminated
                                                        statement; fixed by always routing Kotlin through that
                                                        helper and teaching it to treat a depth-0 `;` as a
                                                        boundary too. (b) `KotlinSignatureRule.
                                                        parseKotlinSignature` discarded NEWLINE tokens up
                                                        front, letting `renderWithTail` flatten a
                                                        multi-statement trailing-lambda default value onto one
                                                        line; fixed with a
                                                        `containsMultilineNestedBrace(sigTokens)` bail. `make
                                                        test`: 211/211 forward + 211/211 idempotency.
                                                        RDD_KEY_212. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster C6k.

  real_code_regressions_164_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood cluster C6k, Shape
                                                        C6k-2: a Kotlin raw (triple-quoted) string ending in
                                                        its own literal `"` right before the closing `"""`
                                                        (e.g. `"""...s3""""".trimMargin()`, a run of 4+
                                                        contiguous quotes) had
                                                        `TokenizerCurly.skipKotlinRawString` (RDD_KEY_117)
                                                        terminate greedily at the *first* `"""` in the run
                                                        instead of the last, leaving stray `"` token(s) that a
                                                        later spacing pass corrupted with an inserted space.
                                                        Fixed by extending `skipKotlinRawString` through the
                                                        entire contiguous quote run. `make test`: 212/212 ->
                                                        213/213 forward + idempotency, zero regressions.
                                                        RDD_KEY_213. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster C6k.

  real_code_regressions_165_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood idempotency cluster
                                                        D2a (round1-vs-round2 closing-brace indent drift): a
                                                        trailing-lambda call chained directly onto the closing
                                                        `}` of an immediately preceding braced span
                                                        (`addFunction { ... }.apply { ... }`) had
                                                        `ScopePipelineCurly.processScope`'s
                                                        `effectiveSpanIndent` re-derive the `.apply {` span's
                                                        indent from its own volatile physical text, which can
                                                        legitimately reflow between rounds. Fixed with a new
                                                        `isChainedFluentCall` check (generalizes the existing
                                                        `isChainedCatchFinally`/RDD_KEY_158 fix shape from
                                                        `catch`/`finally` to any `.`/`?.` fluent-chain
                                                        continuation), making such a span inherit the
                                                        preceding span's own stable resolved indent. Resolved
                                                        328/334 of the corpus-wide known idempotency-flap
                                                        list. `make test`: 213/213 -> 214/214 forward +
                                                        idempotency, zero regressions. RDD_KEY_214. See
                                                        `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin section,
                                                        cluster D2a.

  real_code_regressions_166_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood idempotency, D2a
                                                        residual: `KotlinSpecificRule.signatureLineIndent`
                                                        anchored a nested `where`-clause's indent on a
                                                        volatile boundary-token line when 3+ levels of wrapped
                                                        `where` clauses nested; and `isNamedScope` never
                                                        covered `fun`, so `where`-bearing functions hit the
                                                        same volatile-indent gap in `effectiveSpanIndent`.
                                                        Fixed by anchoring to the true statement's own stable
                                                        header-line indent, and adding
                                                        `headerHasTopLevelWhereClause` to force `spanIndent`
                                                        for `where`-bearing spans. `make test`: 214/214 ->
                                                        215/215. RDD_KEY_215. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster D2a.

  real_code_regressions_167_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood idempotency, D2a
                                                        residual: `isChainedFluentCall` only recognized
                                                        `.`/`?.`-joined trailing-lambda chains, not a
                                                        boolean-operator-joined one (`} || foo.any { ... }`),
                                                        so the indent fell back to a volatile physical-line
                                                        read. Fixed via new `isChainedBooleanOp`, inheriting
                                                        `prevEffectiveSpanIndent` like the `.`/`?.` case.
                                                        `make test`: 215/215 -> 216/216. RDD_KEY_216. See
                                                        `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin section,
                                                        cluster D2a.

  real_code_regressions_168_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood idempotency,
                                                        cluster D4 (`) }` vs `)}` spacing flap):
                                                        `BlockStructureRule.collapseBracelessBody` excluded
                                                        the enclosing `}` from its rendered body but left the
                                                        preceding WHITESPACE token in `contents`, which
                                                        `renderInline` then silently dropped (no trailing-
                                                        whitespace emission). Fixed by re-appending a single
                                                        trailing space when the token before the enclosing `}`
                                                        is WHITESPACE/NEWLINE. Also corrected
                                                        `real_code_regressions_33_out.kt` line 18 (`)} as T`
                                                        -> `) } as T`), which had encoded this same bug as
                                                        "correct". `make test`: 216/216, zero regressions.
                                                        RDD_KEY_218. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster D4.

  real_code_regressions_169_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood idempotency,
                                                        cluster D1 (column-alignment padding flap): two
                                                        group-width recompute instability bugs (RDD_KEY_139/
                                                        140/162 family). `KotlinDeclarationAlignmentRule.
                                                        renderAlignedGroup` and the analogous
                                                        `ScopePipelineCurly.applyGetterSetterPass` one-liner
                                                        grouping both rendered surviving rows as one flat
                                                        shared-width grid even when an excluded row sat in the
                                                        middle, so round1/round2 disagreed on grouping once
                                                        that row hard-broke. Fixed by splitting surviving rows
                                                        into maximal contiguous runs rendered independently
                                                        (new Kotlin-gated `renderKotlinFilteredRuns` for the
                                                        getter/setter case). RDD_KEY_219. Partial fix: a third
                                                        sub-shape (group-padding-induced overflow) found but
                                                        left unfixed here -- see `_170` below. See
                                                        `STATE_KOTLIN.md`'s Dogfood: JetBrains/kotlin section,
                                                        cluster D1.

  real_code_regressions_170_inp/out.kt               -- Kotlin, `JetBrains/kotlin` dogfood idempotency,
                                                        cluster D1's third sub-shape (left unfixed by
                                                        RDD_KEY_219): `KotlinGetterSetterRule`'s one-liner
                                                        grouping could push a member over `lineLengthLimit`
                                                        via shared-column padding alone (its own solo width
                                                        fit), silently triggering a later
                                                        `enforceCallLineBreaking` wrap. Fixed by porting
                                                        RDD_KEY_162's budget-exclusion loop into a new
                                                        depth-aware `render` override (gated on
                                                        `hasBreakableCall`), threaded through
                                                        `applyGetterSetterPass`/`renderKotlinFilteredRuns`.
                                                        Closes D1 fully (all three sub-shapes fixed).
                                                        RDD_KEY_220. See `STATE_KOTLIN.md`'s Dogfood:
                                                        JetBrains/kotlin section, cluster D1.

  real_code_regressions_171_inp/out.java             -- Java, `openrewrite/rewrite` dogfood
                                                        (`rewrite-kotlin/.../K.java`'s
                                                        `ExpressionStatement.withType`, cluster 5): a
                                                        cast-and-parenthesized-expression `return` statement
                                                        (`return (T)(cond ? a : b);`) got misparsed by
                                                        `DeclarationAlignmentRuleCurly.parseDeclaration`'s
                                                        function-pointer-declarator detection as `Type
                                                        (*name)(params);` (reading "return" as the type, "(T)"
                                                        as the "(*name)" group) -- the leading keyword was
                                                        never excluded there, unlike `GetterSetterRuleCurly`'s
                                                        own `STATEMENT_KEYWORDS` guard for the same misparse
                                                        class. The bogus "declaration" merged into the
                                                        preceding real declaration's alignment group, padding
                                                        "return" to that group's type-column width --
                                                        non-idempotent, since reformatting the padded output
                                                        recomputed a different group and collapsed the padding
                                                        back down. Fixed via a new
                                                        `STATEMENT_LEADING_KEYWORDS` guard at the
                                                        function-pointer-detection call site, rejecting the
                                                        shape whenever its "type" token is `if`/`else`/
                                                        `while`/`for`/`do`/`switch`/`try`/`catch`/`finally`/
                                                        `throw`/`return`/`synchronized`. `make test`: 219/219
                                                        forward + idempotency. Closes Cluster 5, the last of
                                                        the 6 `openrewrite/rewrite` idempotency clusters --
                                                        see `STATE_C_CPP_JAVA.md`'s `openrewrite/rewrite`
                                                        entry.

  real_code_regressions_172_inp/out.ts               -- TypeScript, `angular/angular` dogfood cluster 4, root
                                                        cause #3 (call-wrap/collapse vs. alignment-padding
                                                        fits-check ordering). `checkAttrs`/`checkFlag` cover
                                                        the "rescuable" (breakable call exists, collapse
                                                        proceeds, later wrapped) and "unrescuable" (no
                                                        breakable call, collapse refused, stays braced)
                                                        branches of new
                                                        `BlockStructureRule.refuseUnrescuableCollapse`, which
                                                        reuses the `hasBreakableCall` heuristic already used
                                                        elsewhere (`JavaSpecificRule.isSingleLineBody` etc.)
                                                        and is wired into all three collapse call sites,
                                                        including one found only via this fixture's own
                                                        construction. `make test`: 220/220 forward +
                                                        idempotency. Full narrative (insertion points,
                                                        scan-scope deviation, residual heuristic limitation)
                                                        in `STATE_JS_TS.md`'s root-cause-#3 write-up.

  real_code_regressions_173_inp/out.html             -- HTML5, `apache/ant` `manual/running.html` dogfood
                                                        (STATE_DATA_FORMATS.md's Open Questions item 2): an
                                                        orphan `</p>` (bare `<body>` text, no wrapping `<p>`)
                                                        used to cascade a tolerant-close up through `<body>`
                                                        and `<html>` without consuming the stray tag, dumping
                                                        the rest of the document outside `</html>`.
                                                        `XmlSpecificRule`'s new `openTagStack` now
                                                        distinguishes "matches nothing open" (discard in
                                                        place) from "matches an ancestor" (legitimate
                                                        cascade-close, unchanged).

  real_code_regressions_174_inp/out.html             -- HTML5, regression guard for 173: confirms the
                                                        pre-existing mismatched-tag cascade-to-ancestor
                                                        behavior (WPT's `charset/after-bogus.html` idiom: an
                                                        unclosed `<bogus>` tolerant-closed only when its
                                                        enclosing `<div>`'s close is reached) still works
                                                        after `openTagStack` was added.

  real_code_regressions_175_inp/out.html             -- HTML5, `apache/ant` `manual/` dogfood
                                                        (Tasks/antlr.html, Tasks/attrib.html): commented-out
                                                        markup fragments (`<!--tr>...</tr-->`,
                                                        `<!--p>...</p-->`) starting with a lowercase
                                                        tag-name-like token immediately followed by `>` were
                                                        corrupted by `normalize-comment-start-case`
                                                        capitalizing to `Tr>`/`P>`. `XmlSpecificRule`'s new
                                                        `isMarkupFragmentDirective` skips capitalization when
                                                        the comment's leading lowercase run, immediately
                                                        followed by `>`, matches a real tag name in the new
                                                        `MARKUP_FRAGMENT_TAG_NAMES` set -- narrower than
                                                        `isSingleWordDirective`'s "whole comment is one word"
                                                        case. An unrelated lowercase-starting prose comment
                                                        (`attributes inherited from MatchingTask`, same
                                                        corpus) correctly still falls through to ordinary
                                                        capitalization.

  real_code_regressions_176_inp/out.java             -- Java, `jenkinsci/jenkins` real-code repro
                                                        (`hudson/PluginManager.java`'s `doPluginsSearch`): a
                                                        declaration initialized by a stream chain with
                                                        multiple multi-statement, brace-bodied lambda stages
                                                        (`.filter(x -> {...}).map(x -> {...})`) had its ENTIRE
                                                        initializer unconditionally flattened onto one
                                                        physical line (no line-length check) by
                                                        `DeclarationAlignmentRuleCore.renderInitTokens`, which
                                                        runs before `MiscRuleCurly.enforceCallLineBreaking` --
                                                        by then the lambda bodies' original multi-line
                                                        structure was already destroyed and un-rewrappable,
                                                        producing a ~1992-char line that stayed over the limit
                                                        and reformatted differently each round
                                                        (non-idempotent). Fixed with a pre-flight bail-out in
                                                        `DeclarationAlignmentRuleCurly.parseDeclaration`: if
                                                        any brace pair in the initializer originally spanned
                                                        more than one physical source line, leave the
                                                        statement untouched. See `RDD_KEY_225`.

  real_code_regressions_177_inp/out.ts               -- JS/TS, single-declarator colon-spacing bug: `const x:
                                                        number = 1;` rendered as `const x : number = 1;`
                                                        (stray space before `:`) whenever the declaration had
                                                        no alignment-group neighbors, because
                                                        `JsTsDeclarationAlignmentRule.renderAlignedGroup`
                                                        always put `: type` in its own `ColumnGrid` cell, and
                                                        `ColumnGrid` always joins adjacent cells with a space,
                                                        even for a one-row group. Fixed by merging the name
                                                        and `: type` into one cell when `group.size() == 1`; a
                                                        real (`size() > 1`) group keeps its separate-cell
                                                        padding and documented space before `:`
                                                        (STYLE_JS_TS.md §11.2's `DEFAULT : string` example).

  real_code_regressions_178_inp/out.py               -- Python3, indent-style conversion (Python analog of
                                                        `MiscRuleCore#convertIndentation`, new
                                                        `MiscRuleIndent#convertIndentation`): tab-indented
                                                        `match`/`case` and `if`/comment source (modeled on
                                                        `test/py_comments_inp.py`'s own comments, confirmed
                                                        absent as real drift in
                                                        `psf/black`/`django/django`/`python/cpython` -- only 3
                                                        tab-indented files found anywhere, all inside opaque
                                                        docstrings) converted to the default `indent-style =
                                                        spaces` target. Exercises: statement lines rewritten
                                                        from the tokenizer's own INDENT/DEDENT depth (never a
                                                        raw width guess); a `case`-adjacent comment dedented
                                                        to group with a shallower `case` (comments/blank lines
                                                        are never depth-rewritten, only width-converted via
                                                        `MiscRuleCore#renderIndent`); and the file's own final
                                                        line (no trailing newline), exercising the EOF
                                                        DEDENT-run fix -- a synthesized end-of-file DEDENT
                                                        token's `text` field is a literal width number for
                                                        internal use, never source text, and was found
                                                        corrupting output as a stray trailing digit during the
                                                        `psf/black` idempotency check on
                                                        `tests/data/cases/comments3.py`/`annotations.py`). See
                                                        STATE_PYTHON3.md's Resolved Design Decisions.

  real_code_regressions_179_inp/out.ts               -- Distilled from microsoft/TypeScript's
                                                        `commandLineParser.ts` shape: call-wrap/decl-alignment
                                                        vs. `ScopePipelineCurly.applyOversizedAggregateInit
                                                        ClosingBracePass`'s stale-newline-check ordering
                                                        idempotency bug (RDD_KEY_245/246/248). Fixed via a
                                                        narrower JS/TS-only re-run of the closing-brace +
                                                        decl-alignment passes (`ScopePipelineCurly.
                                                        reapplyClosingBraceAndDeclarationsPass`) right after
                                                        the first `enforceCallLineBreaking` call, skipping the
                                                        trailing-gap force-reindent step on that re-run (see
                                                        STATE_JS_TS.md's Open Questions -- that step alone
                                                        caused a real forward-pass regression on
                                                        `real_code_regressions_100.ts`).

  real_code_regressions_180_inp/out.ts               -- Distilled minimal repro (`formatOffset` braceless
                                                        if/else with a padded `else`) for the
                                                        rejoin-fits-check-vs-`alignBracelessElseIfChain`
                                                        pass-ordering idempotency bug (RDD_KEY_250). Fixed via
                                                        a narrow re-run of `enforceCallLineBreaking` (twice,
                                                        for multi-candidate convergence) +
                                                        `enforceComplexityPadding` right after
                                                        `alignBracelessElseIfChain`, same fix shape as
                                                        RDD_KEY_248's
                                                        `reapplyClosingBraceAndDeclarationsPass`.

  real_code_regressions_181_inp/out.java             -- Minimized from `javaparser/javaparser`'s
                                                        `ASTParser.java` (JavaCC-generated): a switch nested
                                                        inside another switch's case body (RDD_KEY_251) -- the
                                                        "non-idempotent switch-case re-indent" gap's
                                                        nested-switch failure mode. Fixed by having
                                                        `SwitchRule.applyNonInlineCaseIndent` derive each
                                                        case-body line's indent from its own brace-nesting
                                                        depth (`applyDepthDerivedBodyIndent`) instead of a
                                                        relative delta, and treating a nested switch's token
                                                        span as opaque to the outer switch's scan, instead of
                                                        letting two independent recomputations disagree.

  real_code_regressions_182_inp/out.ps1              -- Minimized from `PowerShell/PSScriptAnalyzer`
                                                        (`build.psm1`, `AvoidOneChar.tests.ps1`,
                                                        `RuleDocumentation.tests.ps1`): two idempotency/
                                                        correctness bugs from dogfooding. (1)
                                                        `applySwitchArmAlignment`'s `parseArm` misclassified
                                                        an unsplit pipeline's trailing scriptblock (`... |
                                                        Where-Object {...}`) as a whole-line switch-arm
                                                        pattern, so padding differed depending on whether the
                                                        pipeline was already split -- fixed by rejecting any
                                                        line with a depth-0 `|` before the `{` as an arm
                                                        candidate, and moving `format()`'s
                                                        `applyPipelineSplit` call ahead of both alignment
                                                        passes so they always see an already-split shape. (2)
                                                        `applyOperatorSpacing` treated bare `/` as division
                                                        even in bareword command-argument position, corrupting
                                                        Unix-style paths/URLs (`$profileDir/*`,
                                                        `$dir/README.md`) into wrongly-split arguments --
                                                        fixed by dropping bare `/` from the binary-operator
                                                        set (the unambiguous `/=` case is unaffected); zero
                                                        genuine-division instances found in the 24k-line
                                                        dogfood corpus.

  real_code_regressions_183_inp/out.js               -- Minimized from `lodash/lodash`'s `initCloneByTag`
                                                        typed-array fallthrough case: a `SwitchRule`
                                                        non-inline vs. inline-alignment/call-wrap ordering
                                                        gap. `FormatterCurly.format`'s first
                                                        `formatNonInlineSwitches` call decided whether a
                                                        switch needed STYLE.md #13's blank-line-around-
                                                        multiline-case-body treatment *before*
                                                        `alignInlineSwitches`'s case-grid collapse and the
                                                        later `enforceCallLineBreaking` passes could wrap an
                                                        over-width grid-aligned case's trailing call across
                                                        multiple lines -- so a case body that only becomes
                                                        multi-line as a *result* of that later wrap was
                                                        invisible on a fresh format but visible on a reformat
                                                        (round1 != round2). Fixed by re-running
                                                        `formatNonInlineSwitches` again once every
                                                        multi-line-inducing pass has settled (near the end of
                                                        Phase 4). Shared `SwitchRule`/`FormatterCurly` code --
                                                        also proves the fix for C/C++/Java's `SwitchRule`
                                                        path, not JS/TS-specific.

  real_code_regressions_184_inp/out.ts               -- Minimized from `angular/angular` cluster 4's
                                                        `shared.ts`/`directive_outputs.ts` (RDD_KEY_269): a
                                                        braceless bare `if`/`else` whose `else` an earlier
                                                        indent pass re-indents deeper than its paired `if` on
                                                        round 2, defeating
                                                        `BlockStructureRule.alignBracelessElseIfChain`'s
                                                        chain-recovery (it only tolerated the `if` being
                                                        padded wider, not the `else` being re-indented
                                                        deeper). Fixed by also stripping a bare `else`'s
                                                        excess indentation back to its paired `if`. Shared
                                                        `BlockStructureRule`/`KotlinSpecificRule` code path.

  real_code_regressions_185_inp/out.ts               -- Minimized from `microsoft/TypeScript`'s
                                                        `harness/collectionsImpl.ts` (`Metadata.set`,
                                                        RDD_KEY_270): a subscript-assignment statement whose
                                                        `[...]` gets wrapped by `enforceCallLineBreaking`,
                                                        followed by a short assignment that
                                                        `applyAssignmentsPass` grouped/padded against the
                                                        first statement's pre-wrap width -- disagreed once the
                                                        subscript assignment's own wrap state changed. Fixed
                                                        by extending the existing
                                                        `ScopePipelineCurly.reapplyClosingBraceAndDeclarationsPass`
                                                        re-run (RDD_KEY_248) with a third pass,
                                                        `applyAssignmentsPass`, so it also sees the
                                                        post-call-wrap shape.

  real_code_regressions_186_inp/out.ts               -- Minimized from `angular/angular` (cluster 4 residual
                                                        group #3, RDD_KEY_271): two JS/TS idempotency bugs.
                                                        (a) `tryParseClassField` bailed on an embedded NEWLINE
                                                        in an already-wrapped initializer; fixed by collapsing
                                                        it back to single-line text before parsing. (b)
                                                        decorator-argument union-type spacing ran in a Phase 4
                                                        pass *after* `enforceDecoratorOverflowCascade`'s
                                                        inline-fit check, so a fresh format measured narrower
                                                        than a reformat; fixed by pulling
                                                        `enforceUnionIntersectionSpacing`/
                                                        `enforceTypeColonSpacing` forward before it.

  real_code_regressions_187_inp/out.java             -- Minimized from `openrewrite/rewrite` (item 17
                                                        full-tree re-verification): `isSingleStatementBody`'s
                                                        declaration guard only caught `final`/`const`
                                                        declarations, so an unqualified `int x = ...;` body
                                                        got collapsed into an illegal braceless `if` (javac:
                                                        "variable declaration not allowed here"). Fixed by
                                                        adding a `PRIMITIVE_TYPE_KEYWORDS` guard alongside the
                                                        existing `final`/`const` check in
                                                        `BlockStructureRule.isSingleStatementBody`.

  real_code_regressions_188_inp/out.sh               -- Minimized from `ohmyzsh/ohmyzsh` (Bash dogfood): two
                                                        round1/round2 bugs in `BashSpecificRule`. (1)
                                                        `CASE_ARM`'s first-match regex mistook an escaped `)`
                                                        in a pattern like `\(\))` for the terminator; fixed
                                                        via a backslash-aware `matchCaseArm` char scan. (2)
                                                        `runPassA`'s tokenizer had no root-context backslash
                                                        handling, so `\'` in a case pattern opened a real
                                                        string frame that stayed open across lines, corrupting
                                                        indentation; fixed by adding a `c == '\\'` branch that
                                                        consumes the escaped char before any quote check.

  real_code_regressions_189_inp/out.sh               -- Minimized from `ohmyzsh/ohmyzsh` (Bash dogfood, found
                                                        after 188's fixes): `emitCaseBody` didn't recognize a
                                                        combined `esac ;;` line closing both a nested `case`
                                                        and its enclosing arm, corrupting indentation from
                                                        that point. Fixed by splitting into
                                                        `emitCaseBody`/recursive `emitCaseBodyInner`, which
                                                        recurses on nested `CASE_START` and accepts `esac`,
                                                        `esac ;;`, or `esac;;` as the nested terminator.

  real_code_regressions_190_inp/out.sh               -- Minimized from `ohmyzsh/ohmyzsh` (Bash dogfood): a
                                                        real syntax-corruption bug (not just idempotency) in
                                                        `BashSpecificRule.pipeSpacing` (§2.2) -- the lone-`|`
                                                        detector excluded `||`/`|&` but not the noclobber
                                                        redirect `>|`, splitting it into `> |` (a `bash -n`
                                                        syntax error). Fixed by also excluding `|` immediately
                                                        preceded by `>`.

  real_code_regressions_191_inp/out.ps1              -- Minimized from `PowerShell/PowerShell` (PowerShell
                                                        dogfood, run manually by the user): `KEYWORD_PAREN`'s
                                                        lookbehind excluded only word chars, so the method
                                                        call `.ForEach(` was misdetected as the `foreach`
                                                        keyword and gained a spurious space before `(` on
                                                        round2. Fixed by adding `.` to the exclusion set.

  real_code_regressions_192_inp/out.ps1              -- Minimized from `microsoft/azure-pipelines-tasks`
                                                        (`Tasks/Common/VstsAzureHelpers_/Utility.ps1`): `if($x
                                                        -eq $null)` was spaced on round2 but not round1 --
                                                        non-idempotent. Root cause: `runPassA`'s `kind[]`
                                                        array was indexed against the original `content`
                                                        string while consumers read it against
                                                        `passA.transformed`, which diverges in length once a
                                                        standalone `#` comment's placeholder is substituted,
                                                        so `applyKeywordParenSpacing` read the wrong slot.
                                                        Fixed by building `kind` from `RunBuffer`'s own output
                                                        in lockstep (`kindResult()`) plus a companion
                                                        `ChainCollector.resolveKind` that splices `kind` at
                                                        the same placeholder offsets `resolve()` uses.

  real_code_regressions_193_inp/out.java             -- Minimal repro of the Java assignment-alignment
                                                        trailing-comment padding vs. `enforceCallLineBreaking`
                                                        ordering bug (see STATE_C_CPP_JAVA.md's Open
                                                        Questions): a run of `s = applyX(s); // comment`
                                                        assignments forms an `applyAssignmentsPass` alignment
                                                        group whose comment column is padded to the widest
                                                        sibling's pre-wrap width; one sibling then gets
                                                        wrapped by `enforceCallLineBreaking` (which runs after
                                                        `applyAssignmentsPass`), leaving the rest stale until
                                                        a second pass -- non-idempotent. Fixed via a Java-only
                                                        re-run,
                                                        `ScopePipelineCurly.reapplyAssignmentsPassOnly`, that
                                                        re-derives just `applyAssignmentsPass`'s padding
                                                        against the post-wrap shape.

  real_code_regressions_194_inp/out.ts               -- Minimal repro of a syntax-corruption bug found
                                                        dogfooding `microsoft/TypeScript`'s
                                                        `compiler/watchPublic.ts`: `new Map([[undefined,
                                                        undefined]])` got a stray `;` inserted inside the
                                                        parens. Root cause: `TokenizerCurly`'s C++11
                                                        `[[attribute]]`-open detection was missing the `&&
                                                        lang.isCpp` guard its sibling branches (`]]` close,
                                                        `[:`) both have, so a TS nested `[[` array-open
                                                        matched the C++ heuristic (tokenized as OP) while its
                                                        `]]` close fell through to the ordinary PUNCT path --
                                                        the asymmetric OP/PUNCT pair undercounted bracket
                                                        depth in `enforceCallLineBreaking`'s
                                                        `matchParenForward` scan, which then read the argument
                                                        slice one token too far and rendered a spurious
                                                        statement terminator. Fixed by adding the missing `&&
                                                        lang.isCpp` guard.

  real_code_regressions_195_inp/out.ts               -- Minimal repro (deliberately CRLF-encoded, see
                                                        `.gitattributes`) of an idempotency bug found in the
                                                        2026-08-09 `microsoft/TypeScript` dogfood
                                                        reconfirmation: a braceless `if`/`else if` chain's
                                                        single-statement-body alignment padding
                                                        (`BlockStructureRule.alignBracelessElseIfChain`) went
                                                        stale across a second format pass on CRLF-original
                                                        source, e.g. `compiler/moduleNameResolver.ts`. Root
                                                        cause: this method's per-line split (on `\n`, not
                                                        `\r\n`) retains a trailing `\r` on every line while
                                                        CRLF/LF normalization only happens once at the very
                                                        end (`Main.applyLineEndings`), so the extra character
                                                        skewed the length-based `lineLengthLimit` guard
                                                        differently on round1 (still-CRLF) vs. round2
                                                        (already-LF). Fixed by stripping any trailing `\r`
                                                        from each split line up front, before measurement.
                                                        (RDD_KEY_273.)

  real_code_regressions_196_inp/out.ts               -- Minimal repro of a second idempotency bug found in the
                                                        same 2026-08-09 `microsoft/TypeScript` dogfood
                                                        reconfirmation: a class-field alignment group
                                                        (`JsTsSpecificRule.enforceClassFieldAlignmentGrid`),
                                                        e.g. `server/editorServices.ts`'s `readonly
                                                        throttledOperations`, split apart on round2 though
                                                        round1 kept it joined. Root cause: a field's leading
                                                        same-line comment (`/** @internal */`) gets rendered
                                                        on its own line by `flushClassFieldGroup`, adding a
                                                        NEWLINE not in the original text; `blankLineBetween`'s
                                                        old logic (blank if total NEWLINEs across the gap >=
                                                        2) then miscounted that forced line, on round2, as a
                                                        genuine blank line, splitting the group. Fixed by
                                                        requiring the two NEWLINE tokens be back-to-back (only
                                                        WHITESPACE allowed between, never a COMMENT).

  real_code_regressions_197_inp/out.ts               -- Minimal repro (lines 1050-1150 of the real
                                                        `services/codefixes/
                                                        fixMissingTypeAnnotationOnExports.ts`) of a third
                                                        idempotency bug in the same 2026-08-09
                                                        `microsoft/TypeScript` dogfood reconfirmation: a
                                                        closing `}` non-idempotently gained a stale `// if`
                                                        trailing-annotation comment on round2. Confirmed via
                                                        A/B bisection to be fixed solely by RDD_KEY_273's CRLF
                                                        trailing-`\r` strip in
                                                        `BlockStructureRule.alignBracelessElseIfChain` (see
                                                        fixture 195) -- reverting only that fix reproduces
                                                        this bug too, so no separate code change was needed;
                                                        this fixture just locks in that fix's coverage of the
                                                        second symptom.

  real_code_regressions_198_inp/out.ts               -- Minimal repro (distilled from `compiler/types.ts`'s
                                                        `JSDocAugmentsTag` interface) of a fourth idempotency
                                                        bug in the same 2026-08-09 `microsoft/TypeScript`
                                                        dogfood reconfirmation: an interface member's
                                                        intersection-type field with a trailing inline
                                                        object-type literal (`readonly class:
                                                        ExpressionWithTypeArguments & { readonly expression:
                                                        ...; };`) got its nested `{...}` body corrupted
                                                        between round1 and round2. Root cause:
                                                        `JsTsSpecificRule. classBraceKind` walks backward from
                                                        a brace looking for a `class`/`interface` KEYWORD

  real_code_regressions_199_inp/out.kt               -- Minimal repro of Kotlin multi‑line call/list‑literal
                                                        trailing‑comma drop (STYLE_KOTLIN.md §7.2). A trailing
                                                        comma before `)` on a multi‑line call/list‑literal was
                                                        dropped in the "preserve original line groups" path
                                                        (`MiscRuleCurly.renderCallPreserveGroups`) because
                                                        last‑comma suppression unconditionally withheld the
                                                        final comma, which is correct for C/C++/Java but wrong
                                                        for Kotlin. The issue was resolved with a
                                                        `hasTrailingComma` check gated on `lang.isKotlin`,
                                                        consulted before `groupByOriginalLine` drops the
                                                        dangling empty group, and wired into
                                                        `renderCallPreserveGroups`, `renderCallDropped`, and
                                                        `renderCallOnePerLine`. This covers a call with a
                                                        trailing comma that is preserved, a call without one
                                                        that is not added, and a `listOf(...)` list‑literal
                                                        with a trailing comma that follows the same path. A
                                                        token misclassification occurred where a field named
                                                        `class` was misread as a class‑declaration keyword,
                                                        feeding its nested brace into
                                                        `enforceClassFieldAlignmentGrid`. The correction was
                                                        `isFieldNameKeywordUsage`, which checks whether the
                                                        token after the keyword is `:` or `?` (field usage)
                                                        rather than an identifier (declared name), and skips
                                                        such cases.

  real_code_regressions_200_inp/out.js               -- JS/TS §8 getter/setter-group regression: a plain
                                                        block-bodied method with no return-type token
                                                        (`isValid() { ... }`) must join adjacent `get`/`set`
                                                        siblings. Its name-column padding is normalized from
                                                        the siblings' base indentation before rendering, so
                                                        the aligned group remains idempotent.

  real_code_regressions_201_inp/out.py               -- Python3 §6 alignment idempotency bug (found via
                                                        `psf/black` dogfood, `tests/data/cases/function.py`'s
                                                        `**kwargs` final parameter): in an already-broken-out
                                                        signature, a last parameter with neither a type hint
                                                        nor a default leaves nothing after its right-padded
                                                        name column, so `trySignatureGroup`'s padding trailing
                                                        whitespace survived as literal source text that each
                                                        round's re-padding stacked on top of again, growing by
                                                        another `maxNameLen`-derived amount every round. Fixed
                                                        by extending each parameter's replacement span through
                                                        its own trailing NEWLINE token instead of stopping at
                                                        the last significant token, so leftover whitespace is
                                                        fully swallowed rather than surviving alongside it.

  real_code_regressions_202_inp/out.py               -- Python3 §8/§7 join-threshold non-convergence bug
                                                        (found via `click`/`flask`/`django` dogfood;
                                                        RDD_KEY_287): the `if`/`for` single-statement-body
                                                        join (`applySingleStatementBody`) and the equivalent
                                                        §7 `case` virtual-join (`classifyCaseLine`)
                                                        fits-checked only header+body text, ignoring a
                                                        trailing comment that survives the join untouched --
                                                        so round1 could emit an over-length joined line, which
                                                        round2 then reversed via the separate compact-overflow
                                                        check (which does measure the whole physical line).
                                                        Fixed by measuring `joined + trailingSuffix` at all
                                                        three affected call sites in
                                                        `ScopePipelineIndent.java`:
                                                        `applySingleStatementBody`'s join,
                                                        `classifyCaseLine`'s virtualJoin, and
                                                        `flushCaseGroup`'s post-alignment length guard.

  real_code_regressions_203_inp/out.java             -- catch/finally placement bug (found via `tools/*`
                                                        self-format, `tools/gru/FilterAbstain.java`):
                                                        `BlockStructureRule.indentBefore` only found a `}`'s
                                                        indent when it was first on its own line, so a
                                                        same-line collapsed `try { ... } catch (...) { ... }`
                                                        got its moved-out `catch` placed flush-left. Fixed to
                                                        walk back to the line's actual start regardless of
                                                        what's first on it; also fixes `placeElseOnOwnLine`,
                                                        which shares the helper.
                                                        `test/real_code_regressions_168_out.kt` (a
                                                        pre-existing fixture that had baked in the buggy
                                                        flush-left `catch` as its expected output) updated to
                                                        the corrected indented form.

How Tests Are Run
-----------------

Use the Makefile target from the formatter root directory:

    make test

This runs the formatter on every *_inp file, writes output to a temp directory, and diffs against
the corresponding *_out file. Any mismatch is a test failure. After the forward pass, each *_out
file is also formatted and diffed against itself (idempotency check) -- the formatter must produce
no changes on already-correctly-formatted input.

See the Makefile for the exact commands and how to specify a custom JAR path.


Adding New Tests
----------------

1. Create <name>_inp.<ext> with the construct or edge case you want to test.
2. Run the formatter on it to produce the initial output.
3. Review the output for correctness against the style guide.
4. If correct, save it as <name>_out.<ext>.
5. If not correct, fix the formatter, then repeat.
6. Commit both files.

The *_out files are the ground truth. If a formatter fix intentionally changes the output for
a rule, update the corresponding *_out file in the same commit.


Dogfood Test (Self-Formatting)
------------------------------

In addition to the above file-pair tests, the Makefile's `test` target also runs the formatter
against its own Java source tree under src/. Expected outcomes:

  1. The formatted source compiles clean with javac (no errors).
  2. Running the formatter a second time on the formatted source produces no changes (idempotency).
  3. The class/interface/enum declaration count in the formatted source matches the original (no
     declarations were deleted or duplicated).

No bytecode comparison is performed -- line-number tables in .class files change whenever formatting
adds or removes blank lines, making bytecode diffs unreliable.
