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

Java:
  java_core_inp/out.java                  -- Core Java 8-compatible constructs: declaration
                                             alignment, modifier ordering, getter/setter groups,
                                             closing comments, K&R/Allman braces, import sorting,
                                             switch, lambdas, anonymous classes.

  java_modern_inp/out.java                -- Java 17+ constructs: records, sealed/non-sealed
                                             classes, switch expressions (arrow form + block body),
                                             text blocks, var, pattern-matching instanceof.

  java_combined_inp/out.java              -- All of the above in one realistic file: sealed class
                                             with nested record, enum, inner classes; switch
                                             expressions; pattern matching; text blocks; var;
                                             getter/setter groups; import ordering.

  java_comments_inp/out.java              -- Uncommon comment placements: // and /* */ between
                                             annotations and declarations, inside method signatures,
                                             inside if/for/switch headers, between else and brace,
                                             trailing on array initializers, multi-line block
                                             comments inside methods.

  java_format_toggle_inp/out.java         -- JXM_CFMT_DIS/JXM_CFMT_ENA formatting-toggle markers
                                             (both the `//%` line-comment and `/*% */` block-comment
                                             forms), each wrapping a deliberately misformatted
                                             method or field that must survive byte-for-byte
                                             untouched, with normally-formatted declarations
                                             immediately before, between, and after each frozen
                                             region.

  java_preprocessor_method_inp/out.java   -- Regression coverage for Java source using
                                             C-preprocessor directives, including the "preprocessor
                                             directive glued onto a following method definition" bug
                                             (STATE.md Known Gaps): a `#endif` directly before a
                                             method inside a class body, with and without blank
                                             lines and a `throws` clause, must not be joined onto
                                             the method's own modifier line. Also covers the
                                             PCPP-preprocessed Java pattern used in
                                             `src/jxm/ugc/ARMCortexMThumbC.java.in` (a `.java.in`
                                             file run through a C-macro preprocessor before
                                             compilation, per README.md's "C-preprocessor directives
                                             in Java source" note): a `#define`-style function-like
                                             macro precedes a class and is invoked with
                                             loosely-spaced call arguments (`__GEN_CXI_NPR_NPR__(
                                             clrex, ... )`). Confirms the `#define` line itself
                                             passes through untouched (recognized/skipped like any
                                             other preprocessor directive) while the macro
                                             invocation lines still get normal call-padding
                                             tightening (`(clrex, ...)`) and are idempotent.

Kotlin:
  kt_combined_inp/out.kt                 -- Kotlin STYLE_KOTLIN.md + STYLE_KOTLIN2.md end-to-end
                                            coverage: enum class with members, sealed classes, data
                                            classes, type aliases, generics/variance, where clauses,
                                            infix/extension functions, null-safety operators, when
                                            expressions, property accessors, destructuring
                                            declarations, labeled jumps, and ranges, all in one
                                            realistic file. See STATE_KOTLIN.md Step 4.

  kt_comments_inp/out.kt                 -- Uncommon comment placements in Kotlin, plus
                                            JXM_CFMT_DIS/JXM_CFMT_ENA formatting-toggle markers. See
                                            STATE_KOTLIN.md Step 4.

C:
  c_core_inp/out.c                        -- C11 constructs: declaration alignment, bitfields,
                                             pointer placement, struct/enum/typedef, function Allman
                                             braces, control-flow K&R, pre-increment, static
                                             reordering, assignment alignment.

  c_combined_inp/out.c                    -- All C constructs together in one realistic file:
                                             macros, enums with closing comments, structs, forward
                                             declarations, global state alignment, public API,
                                             internal functions, inline comments.

  c_comments_inp/out.c                    -- Uncommon comment placements in C: inside struct,
                                             between params, inside if/for headers, between else and
                                             brace, divider normalization, comments on macros.

  c_cpp_decl_gaps_inp/out.c               -- Regression coverage for three DeclarationAlignmentRule
                                             fixes (STATE.md "Known Gaps -- Fixed"): the `* const`
                                             column gap in mixed pointer-star groups, `typedef`
                                             joining and aligning with a surrounding plain-variable
                                             group, and direct function-pointer declarations
                                             (including multi-star `(**cb)`) joining a group.

C++:
  cpp_core_inp/out.cpp                    -- C++11/14 constructs: class with access specifiers,
                                             template class, lambdas, auto return type, initializer
                                             list constructors, getter/setter groups, extern "C".

  cpp_modern_inp/out.cpp                  -- C++17/20/23 constructs: structured bindings,
                                             init-statement if/switch, concepts/requires,
                                             consteval/constinit, operator<=>, coroutines
                                             (co_yield/co_return).

  cpp_combined_inp/out.cpp                -- All C++ constructs together: concepts, enum class,
                                             template class with nested Config struct, structured
                                             bindings, init-statement if/switch,
                                             consteval/constinit, operator<=>, lambda with auto
                                             return, extern "C", trailing comments on declarations.

  cpp_comments_inp/out.cpp                -- Uncommon comment placements in C++: inside template
                                             parameter lists, inside concept requires expressions,
                                             between class specifier and base, inside function
                                             params, inside structured bindings, inside requires
                                             clauses.

C/C++ Headers:
  h_core_inp/out.h                        -- C header with #ifndef guard: header zone spacing,
                                             include ordering (angle vs quote), struct alignment,
                                             pointer declarations, #ifdef __cplusplus extern "C".

  h_combined_inp/out.h                    -- Combined C header: guard zones, macros alignment, named
                                             enum/struct with closing comments, full API declaration
                                             group, extern "C".

  hpp_core_inp/out.hpp                    -- C++ header with #pragma once: pragma once zone spacing,
                                             concepts, enum class, structs with operator<=>,
                                             abstract class interface, concrete derived classes.

  hpp_combined_inp/out.hpp                -- Combined C++ header: pragma once zones, concepts,
                                             template base class, concrete subclass, factory
                                             declaration, extern "C" block.

In-file config directive:
  in_file_config_inp/out.hpp              -- Top-of-file JXM_CFMT_CFG directive (STATE_COMMON.md
                                             "In-file Config Support", RDD_KEY_167/168): sets every
                                             per-file-applicable Config Keys and Defaults key (all
                                             except server-port). Proves indent-size=2 (1-space raw
                                             source indentation rounded up to 2, not left at the
                                             source's already-4-space-multiple width) and
                                             format-macros=off (macro value columns stay unaligned
                                             despite `make test`'s own FORMAT_MACROS=on env var --
                                             proof the directive outranks env vars too).
                                             header-guard-rename intentionally left off this fixture
                                             (see RDD_KEY_168 -- untestable via the _inp/_out diff
                                             convention, since the guard name derives from the
                                             invocation path and _inp/_out always differ).

  in_file_config_inp/out.java             -- Same directive coverage as the .hpp fixture, plus
                                             java-import-order reversed from its default (java, com,
                                             org, other, local, static) to (static, local, other,
                                             org, com, java); one import per bucket proves the full
                                             reversed order is honored.

  in_file_config_inp/out.kt              -- Same directive coverage again, plus kotlin-import-order
                                            reversed from its default (kotlin, java, android, com,
                                            org, other, local) to (local, other, org, com, android,
                                            java, kotlin); one import per bucket proves the full
                                            reversed order.

  in_file_config_error_inp/out.hpp        -- Proves the hard-error path (two JXM_CFMT_CFG directives
                                             in one file must be rejected, never silently resolved).
                                             Deliberately not run by `make test` (commented out of
                                             the Makefile's INP_FILES) -- a hard-erroring input has
                                             no formatted result to diff against, and would always
                                             show as a spurious FAIL. See the file itself for how to
                                             exercise it manually.

JSON/JSON5:
  json_core_inp/out.json                  -- Plain RFC 8259 JSON: colon-alignment groups, tight
                                             atoms-only arrays, loose arrays containing objects,
                                             empty object/array.

  json5_core_inp/out.json5                -- JSON5-only additions: unquoted keys, single-quoted
                                             strings, hex/negative numbers, `//` and `/* */`
                                             comments and a blank line each breaking an alignment
                                             group, a backslash-newline multi-line string preserved
                                             opaque (§1.3), a trailing comment before the closing
                                             brace.

  json5_comments_inp/out.json5            -- A comment breaking then re-merging a colon-alignment
                                             group, a multi-line `/* */` comment reindented to its
                                             new structural depth, a comment inside an array, a `key
                                             /* comment */ : value` mid-comment excluded from
                                             alignment, and comment-start-case normalization on
                                             leading/trailing/mid comments.

CSS:
  css_combined_inp/out.css                -- Property/value colon-alignment groups broken by a
                                             comment then re-merging, a custom property (`--gap`)
                                             joining an ordinary group, `@media`/`@supports`/
                                             `@font-face`/`@keyframes` at-rules as headers starting
                                             their own independent nested group, and native CSS
                                             nesting (`&:hover`, `& .icon`) recursing the same way.

  css_comments_inp/out.css                -- A multi-line `/* */` comment breaking a group (only its
                                             first sentence gets comment-start-case normalization),
                                             a `JXM_CFMT_DIS`/`ENA` marker pair freezing a
                                             declaration's original spacing/indentation
                                             byte-for-byte, a trailing comment before a block's
                                             closing brace, a comment between a selector and its
                                             `{`, a comment between a property and its `:` (`prop /*
                                             ... */ : value`), and a comment as the sole content
                                             before declarations inside a native-nesting `&:hover`
                                             block.

YAML/TOML:
  yaml_core_inp/out.yaml                  -- Mapping colon-alignment group, a flow mapping short
                                             enough to stay flow, a flow mapping converted to block
                                             on `line-length` overflow (including its own nested
                                             array converted the same way), sequence items one level
                                             deeper than their parent key, a sequence of mappings, a
                                             block scalar (`|`), an anchor/alias pair, an explicit
                                             tag, and a multi-document stream (`---`/`...`). Sets
                                             `indent-size=2` via an in-file `#% JXM_CFMT_CFG`
                                             directive to exercise YAML's own community indent
                                             convention.

  yaml_comments_inp/out.yaml              -- A `#` comment breaking a colon-alignment group, a
                                             comment sitting between two sequence items, a `#%
                                             JXM_CFMT_DIS`/`ENA` marker pair freezing a
                                             malformed-spacing line verbatim, a trailing comment,
                                             and comment-start-case normalization.

  toml_core_inp/out.toml                  -- `=`-alignment group at the top level and within
                                             `[package]`/ `[[bin]]` tables, no added indentation for
                                             keys under a table header, a tight array of atoms vs. a
                                             loose array containing nested arrays, an
                                             always-single-line inline table, and a
                                             preserved-as-written dotted key.

  toml_comments_inp/out.toml              -- A `#` comment breaking an `=`-alignment group, a `#%
                                             JXM_CFMT_DIS`/`ENA` marker pair freezing a
                                             malformed-spacing line verbatim, a trailing comment,
                                             and comment-start-case normalization.

XML:
  xml_combined_inp/out.xml                -- `<?xml?>` PI plus a second `<?xml-stylesheet?>` PI and
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

  xml_comments_inp/out.xml                -- A standalone leading `<!-- -->` comment reindented and
                                             case-normalized, now also exercised two levels deep
                                             inside a nested block; an inline trailing comment
                                             gaining a leading space before `<!--`; a multi-line
                                             `<!-- -->` comment whose opening line reindents and
                                             capitalizes while its interior/closing line is folded
                                             onto the same line as the closing `-->`; a `<!--%
                                             JXM_CFMT_DIS -->`/`ENA` marker pair freezing a
                                             malformed-spacing tag verbatim; and a trailing comment
                                             right before the closing tag.

C++26:
  cpp26_core_inp/out.cpp                  -- Pack indexing (`T...[N]` tight vs. going loose when the
                                             index contains a call or a nested bracket), `=
                                             delete("reason")` vs. bare `= delete;`, placeholder `_`
                                             in structured bindings and if-init, and contract
                                             clauses (`pre`/`post`/`contract_assert`) staying inline
                                             when the signature fits vs. one-per-line when it
                                             doesn't.

  cpp26_comments_inp/out.cpp              -- Uncommon comment placement around the above: leading
                                             comment before pack indexing, comment between
                                             `template<>` and its `using`, comments forcing an
                                             `if`-init to stay a braced block instead of collapsing
                                             to inline, per-clause leading/trailing contract
                                             comments, and `/* */` block comments between contract
                                             clauses.

  cpp26_reflection_inp/out.cpp            -- Reflection (`^^`, `[:`/`:]` splicing): `^^` binding
                                             tight to an initializer, a `return` expression, and a
                                             parenthesized sub-expression; a four-member `constexpr
                                             auto` `=`-alignment group; `[:refl:]` staying tight vs.
                                             `[: computeRefl(x) :]` going loose because it contains
                                             a call; a standalone splice reused as an operand; a
                                             second alignment group after a blank line; and an `if`
                                             going loose then collapsing to inline. Promoted ahead
                                             of its original promotion gate (external- corpus
                                             cross-check for STYLE_CPP26.md §5 still pending) to
                                             seed the initial tokenizer test for `^^`/`[:`/`:]`; see
                                             STATE_CPP26.md.

JS/TS:
  js_combined_inp/out.js                  -- Import grouping/sorting, inline vs. own-line decorator
                                             placement, a private class field, static vs. instance
                                             getter/setter one-liner alignment groups,
                                             destructuring/spread/template literals/optional
                                             chaining/nullish coalescing, both arrow forms, an
                                             eight-member `const` `=`-alignment group, mandatory
                                             blank line before `return`, and closing comments on the
                                             class and an Allman-brace method but not a short
                                             generator.

  js_comments_inp/out.js                  -- Leading/trailing comments surviving import resort, a
                                             comment forcing a destructuring pattern multi-line (and
                                             out of any `=`-alignment group), and comments around a
                                             generator method's `yield`s.

  js_getter_setter_asi_inp/out.js         -- A semicolon-less class field (`#cache = new Map()`,
                                             legal under JS's ASI) sitting directly above a `static
                                             get`/`static set` one-liner pair: GetterSetterRule's
                                             member-splitting used to require an explicit `;`/`}`
                                             boundary (JS/TS semicolon insertion runs in a later
                                             phase), so the unterminated field swallowed the
                                             following `static get` member into its own span and
                                             desynced blank-line-boundary detection for every member
                                             after it -- leaving the static get/set pair's empty
                                             parens unpadded to match its sibling's width while a
                                             plain get/set pair below it padded correctly. Now fixed
                                             with an ASI-aware depth-0-NEWLINE statement boundary
                                             (JS/TS only).

  js_import_ordering_comments_inp/out.js  -- §15 import-ordering comment handling (RDD_KEY_197): a
                                             trailing same-line comment on an import (`import a from
                                             "alpha"; // keep with a`) travels with its own import
                                             through reordering instead of blocking the pass; a
                                             standalone comment on its own line between two imports
                                             now segments the import list (imports before/after it
                                             are grouped/sorted independently, never reordered
                                             across each other) with the comment preserved verbatim
                                             in place, instead of bailing the whole pass.

  js_nested_template_literal_inp/out.js   -- §4 nested template-literal interpolation (`` `outer
                                             ${`inner ${x+1}`}` ``): the inner template literal's
                                             own `${...}` interpolation now gets its expression
                                             spacing normalized too (`x + 1`), not just the
                                             outermost `${...}` span -- recursive reformatting of
                                             any nesting depth via
                                             enforceTemplateLiteralInterpolationSpacing.

  ts_combined_inp/out.ts                  -- Tight union/intersection `=`-alignment, both
                                             break-before/break-after long-union continuation
                                             styles, generics with a default type parameter,
                                             `interface`/`type`-alias `:` alignment, both enum
                                             forms, the full six-slot class-field modifier order, a
                                             mixed-modifier-length alignment group, and the two-step
                                             decorator-overflow cascade.

  ts_comments_inp/out.ts                  -- A trailing comment surviving union-continuation
                                             realignment, a comment inside a generic type-parameter
                                             list staying tight, comments breaking `interface`/enum
                                             alignment groups, and a trailing comment on an
                                             overflow-wrapped decorator staying attached to its
                                             closing `)`.

  ts_decl_grid_ext_inp/out.ts             -- Declaration-alignment-grid extensions
                                             (RDD_KEY_182/183): an object-destructuring-pattern LHS
                                             joins the same const/let `=`-alignment group as a plain
                                             identifier declarator, and two consecutive `type X =
                                             ...` aliases form their own `=`-aligned group.

HTML5:
  html_combined_inp/out.html              -- Void element normalization (`<img>`/`<input>`/ `<br>`
                                             lose self-closing `/`, contrasted with `<link>`), bare
                                             boolean attributes, a tag whose combined attribute
                                             width overflows and wraps one per line, an embedded
                                             `<style>` block dispatched to CSS formatting, an
                                             embedded `<script>` block dispatched to JS formatting,
                                             ordinary nesting, and `<pre>` content preserved
                                             byte-for-byte.

  html_comments_inp/out.html              -- Stacked leading `<!-- -->` comments, an inline trailing
                                             comment, opaque CDATA in a non-script tag, a `data:`
                                             URI attribute overflowing by length (not count), a
                                             comment as sole content inside a spliced `<style>`
                                             block, the CDATA-wrapped `<script>` idiom dispatched to
                                             JS formatting and re-wrapped, and a `<script
                                             type="application/json">` block staying fully opaque.

Python3:
  py_combined_inp/out.py                 -- Bracket-complexity categories, assignment
                                            alignment (augmented assignment, both
                                            continuation-break styles), import
                                            ordering/grouping including `__future__`
                                            promotion, decorators, f-strings, function
                                            signature wrapping with type hints, structural
                                            pattern matching, single-statement compound
                                            bodies, control-flow blank lines, `async`/`await`,
                                            and a `@property`/`@x.setter` pair.

  py_comments_inp/out.py                 -- Uncommon `#` comment placement: a comment
                                            breaking an assignment-alignment group, trailing
                                            comments not breaking a comprehension-assignment
                                            group, a comment forcing a signature to wrap, a
                                            byte-for-byte-preserved docstring, a comment
                                            between two `case` blocks, and a comment breaking
                                            a compact `case`-line alignment group.

Real-code regressions:
  real_code_regressions_1_inp/out.cpp     -- Distilled from tinyexpr-plusplus: same-line-sibling
                                             call-argument mis-split, an undercounted call "does it
                                             fit" length check, and an
                                             enforceComplexityPadding/enforceCallLineBreaking
                                             pass-ordering idempotency bug.

  real_code_regressions_2_inp/out.java    -- Distilled from RobotCoding's gui_frontend: `>>>`
                                             mis-tokenized as `>>`+`>`; GetterSetterRule padding
                                             computed against stale pre-padding text;
                                             enforceCallLineBreaking losing complexity-padding
                                             awareness when joining a multi-line call; and a
                                             getter/setter grouping decision that didn't predict a
                                             later line-break, so a fresh format and a reformat
                                             produced different output.

  real_code_regressions_3_inp/out.java    -- Distilled from dogfooding the formatter's own src/
                                             tree: MiscRule's consecutive-assignment alignment
                                             rejected an RHS already wrapped by a later pass,
                                             splitting/shrinking the alignment group on a second
                                             format instead of treating it as one group like a fresh
                                             format does.

  real_code_regressions_4_inp/out.hpp     -- Distilled from martinus/nanobench: no tokenizer support
                                             for C++11 raw string literals at all (corrupting
                                             brace-depth tracking and truncating up to ~46% of real
                                             files); raw-string support gated on a C-only flag
                                             instead of C-or-C++; and DeclarationAlignmentRule
                                             dropping a leading `template<...>` prefix on bare
                                             forward declarations.

  real_code_regressions_5_inp/out.cpp     -- User-reported: a `while` loop's own closing `}` stayed
                                             indented to its body instead of its frame. Fixed by
                                             force-reindenting a scope's closing brace to the
                                             frame's indent, with carve-outs for case/default-label
                                             spans, a comment in the trailing gap, bare compound
                                             blocks, and empty bodies.

  real_code_regressions_6_inp/out.java    -- Found via idempotency testing on google-java-format: a
                                             trailing same-line closing comment (`} // if`) before a
                                             case/default label was wrongly treated as a *leading*
                                             comment, forcing a spurious blank line and orphaning
                                             it. Fixed by only applying that exception to comments
                                             that start their own line.

  real_code_regressions_7_inp/out.java    -- Found via idempotency testing on google-java-format:
                                             arrow-form `case X -> body;` joins didn't check the
                                             resulting line length, so a fresh format could produce
                                             an over-length line that a reformat then broke apart.
                                             Fixed by predicting the joined width before committing
                                             to the join.

  real_code_regressions_8_inp/out.java    -- Found via idempotency testing on google-java-format:
                                             the getter/setter one-liner pass misparsed arrow-form
                                             `case`/`default` switch arms as accessor members,
                                             injecting garbage column padding. Fixed by rejecting
                                             any one-liner starting with `case`/`default`.

  real_code_regressions_9_inp/out.java    -- Found via idempotency testing on pcpp_java: switch
                                             inline-alignment padded a short label to match a wider
                                             sibling without checking the row's final length,
                                             producing an unstable over-length line. Fixed by
                                             predicting every row's rendered length before padding.

  real_code_regressions_10_inp/out.java   -- Found via idempotency testing on pcpp_java:
                                             one-liner-body detection used a raw newline check that
                                             could be fooled by a call already broken across lines
                                             by an earlier pass, wrongly recursing into and
                                             corrupting an already-correct one-liner body. Fixed
                                             with a paren/bracket-depth-aware scan.

  real_code_regressions_11_inp/out.c      -- Found via idempotency/round-trip testing on
                                             tongsuo-mini: a flat aggregate initializer (e.g. a
                                             large S-box table) collapsed to one line with no length
                                             check, producing lines thousands of characters long.
                                             Fixed by rejecting the collapse when it would exceed
                                             the line limit, plus normalizing the closing `}` of
                                             oversized multi-line initializers onto its own line.

  real_code_regressions_12_inp/out.hpp    -- Found via idempotency testing on
                                             serge-sans-paille/frozen's catch.hpp: a struct with a
                                             virtual destructor, a long pure-virtual signature, and
                                             a template method-with-body was corrupted on the first
                                             pass -- members merged, a stray space on
                                             `~ClassName()`, and `};` accumulating extra semicolons
                                             each pass. Two depth-tracking bugs in
                                             DeclarationAlignmentRule.parseDeclaration, both fixed.

  real_code_regressions_13_inp/out.hpp    -- Found via idempotency testing on the same catch.hpp: a
                                             nested `for` loop came out of the first pass with
                                             corrupted indentation. Root cause: the
                                             preprocessor-directive tokenizer had no
                                             backslash-line-continuation handling, desyncing the
                                             brace-depth counter for the rest of the file. Fixed by
                                             adding the same continuation handling its sibling
                                             already had.

  real_code_regressions_14_inp/out.hpp    -- Minimal repro for a general indent fix: a construct
                                             sharing its opening line with a parent `{`, nested
                                             inside a namespace, came out under-indented by one
                                             level because the indent fallback used a depth counter
                                             that doesn't increment for namespace bodies. Fixed by
                                             threading a real accumulated-indent string through the
                                             recursion instead of guessing from depth.

  real_code_regressions_15_inp/out.hpp    -- Minimal repro for a tokenizer fix: catch.hpp's
                                             Objective-C interop block has genuine `[[NSString
                                             alloc] init]` message sends, which the tokenizer used
                                             to merge into a C++17-attribute-style `[[...]]` token
                                             regardless of context, desyncing bracket-depth for the
                                             rest of the file. Fixed by only merging `[[` when a
                                             forward scan confirms a genuine attribute-shaped close.

  real_code_regressions_16_inp/out.hpp    -- Covers 4 unrelated idempotency bugs surfaced once
                                             fixture 15's fix stopped masking them, all in
                                             ScopePipeline/TokenizerCore: an off-by-one in the
                                             namespace-detection scan; a constructor's
                                             member-initializer list being mistaken for its own
                                             signature body (corrupting spacing, and missing a
                                             trailing-length check); and an elaborated-type
                                             declaration with an empty initializer (`struct
                                             sigaction sa = { };`) misdetected as a struct body,
                                             appending an extra `;` on every pass.

  real_code_regressions_17_inp/out.kt    -- Kotlin dogfood find (RobotTcpSession.kt):
                                            enforceCallLineBreaking's per-argument-line grouping
                                            (Option 2) collapsed a multi-line lambda-body sibling
                                            argument onto one line, merging statements with no `;`
                                            separator -- invalid Kotlin. Fixed by bailing (Kotlin-
                                            only) when a top-level argument mixes a newline and `{`.

  real_code_regressions_18_inp/out.kt    -- Kotlin idempotency (PlayMusicBlock.kt):
                                            KotlinDeclarationAlignmentRule.spansMultipleLines
                                            treated a braceless if/else initializer as multi-line
                                            once a nested call got wrapped by a later pass,
                                            shrinking the sibling val's alignment on each reformat.
                                            Fixed with paren/brace-depth-aware newline tracking so
                                            only real `{`...`}` bodies count as multi-line.

  real_code_regressions_19_inp/out.kt    -- Kotlin indent drift (MainActivity.kt):
                                            ScopePipeline.processScope derived a trailing-lambda
                                            body's indent/closing-brace from the statement's first
                                            line instead of the `{`'s own (deeper) physical line,
                                            under-indenting the body on a fresh format. Fixed via a
                                            new braceLineIndent helper (Kotlin-only).

  real_code_regressions_20_inp/out.kt    -- Kotlin compile-break
                                            (ToolbarActions.kt/MainViewModel.kt):
                                            collapseSingleExpressionBlocks has no expression- vs
                                            statement-position `if` distinction, so it swallowed the
                                            newline after a parenthesized `(if (cond) a else b)`
                                            initializer, fusing it with the next statement. Fixed by
                                            tracking unmatched-paren depth and refusing to collapse
                                            `if`/`else` while inside one.

  real_code_regressions_21_inp/out.kt    -- Kotlin spacing bug (BlockCanvasView.kt): isTightToken's
                                            `&`-repeat check (for C/C++ `&&` rvalue-ref sigils) also
                                            matched Kotlin's `&&` logical-AND, dropping the space
                                            before it. Fixed by gating the check to non-Kotlin
                                            languages, mirroring MiscRule's existing gate.

  real_code_regressions_22_inp/out.kt    -- Kotlin idempotency (BlockPalette.kt): a one-liner-
                                            function group's column width was computed from pre-wrap
                                            text, but a later pass wrapped a too-long member's call,
                                            leaving stale padding on reformat. Fixed by porting the
                                            C/Java `hasBreakableCall` + estimated-width pre-check to
                                            KotlinGetterSetterRule.parseKotlinOneLinerMember.

  real_code_regressions_23_inp/out.kt    -- Kotlin idempotency (BlockPalette.kt):
                                            KotlinSpecificRule.isSingleLineBody kept K&R `{` for a
                                            body that was pre-wrap one-line but got split by a later
                                            call-wrapping pass, flipping to Allman on reformat.
                                            Fixed by porting the same hasBreakableCall + estimated-
                                            width pre-check, with a corrected width formula that now
                                            accounts for indentation and spacing.

  real_code_regressions_24_inp/out.kt    -- Kotlin compile-break
                                            (ConnectTypeDialog.kt/WifiApDialog.kt):
                                            findLastTopLevelCloseParen accepted any last depth-0 `)`
                                            as a signature's param list even with no `:` following,
                                            so `x.foo().bar { ... }` misdetected `bar` as a return-
                                            type tail and silently deleted `.bar`. Fixed by
                                            requiring a top-level `:` immediately after the `)`
                                            before accepting the Kotlin return-type-tail branch.

  real_code_regressions_25_inp/out.kt    -- Kotlin compile-break
                                            (BlockCanvasView.kt/ToolbarActions.kt):
                                            enforceLabeledJumpSpacing's label-detection state
                                            machine couldn't tell a genuine `label@` from an
                                            unrelated `@Annotation`, corrupting
                                            `@JvmOverloads`/`@Volatile` into `@ JvmOverloads`/`@
                                            Volatile`. Fixed with an isLoopLabelTarget lookahead
                                            requiring `for`/`while`/`do`/`{` after `@`.

  real_code_regressions_26_inp/out.kt    -- Kotlin compile-break (Optimizer.kt):
                                            collapseSingleExpressionBlocks's bare-`else` handling
                                            also matched a `when` arm's `else ->` label (no brace,
                                            since its body follows `->`), flattening a multi-
                                            statement block onto one line with no `;` separators.
                                            Fixed by checking for a following `->` and bailing out
                                            of the braceless-collapse path.

  real_code_regressions_27_inp/out.kt    -- Kotlin, two co-occurring bugs (ProgramBuilder.kt): (1)
                                            needsSpaceBetween had no case for `!is`/`!in`,
                                            corrupting them into `! is`/`! in`; (2)
                                            enforceCallLineBreaking's renderCallCandidate used the
                                            C/Java-style parseSignature on a call argument
                                            (`it.func.funcName`), misparsing it as `Type name` and
                                            inserting a spurious space once wrapped. Fixed by adding
                                            `!is`/`!in` as tight tokens and routing Kotlin calls
                                            through a separate untyped sigForRender path.

  real_code_regressions_28_inp/out.hpp    -- C++, real-code test against taocpp/PEGTL
                                             (rematch_input.hpp): reclassifyAngleBrackets'
                                             single-open-`<` branch retyped a literal `>>` token via
                                             `retype()` (preserving its 2-char text) while also
                                             appending a new 1-char `>` token, duplicating a
                                             character on the first format pass and breaking a
                                             `template<...>` forward declaration. Fixed by giving
                                             the retyped token its own explicit 1-char text.

  real_code_regressions_29_inp/out.java   -- Java, real-code test against local `anemonesoft`
                                             candidate (HelpBox.java/Spreadsheet.java):
                                             renderCallCandidate's groupByOriginalLine tracks only
                                             paren/bracket depth, not brace depth, so a call's
                                             multi-line brace-body trailing argument got silently
                                             swallowed into one unbounded output line, flapping to
                                             an idempotency failure once a later pass reacted to the
                                             now-multi-line body. Fixed by widening an existing
                                             Kotlin-only "leave such an argument untouched" bail to
                                             all languages via a new containsInternalNewline check.

  real_code_regressions_30_inp/out.kt    -- Kotlin, real-code test against `square/okio`: three
                                            co-occurring bugs. (1) renderTokens had no unary-vs-
                                            binary `-`/`+` notion, corrupting `val x = -1` into `= -
                                            1`; fixed with an isUnaryMinusOperand lookback. (2)
                                            applySignaturePass's `: ReturnType` tail detection
                                            merged a headerless declaration with an unrelated later
                                            one across a blank line; fixed with a
                                            hasTopLevelBlankLine guard. (3) braceless if/while/for
                                            collapse rendered a stale, untightened keyword-paren
                                            space, causing enforceCallLineBreaking to over-wrap a
                                            line that fits at its true final width; fixed by
                                            tightening the space at collapse time.

  real_code_regressions_31_inp/out.kt    -- Kotlin, two compile-breaking bugs found via `kotlinc`
                                            against `square/okio` (not caught by idempotency
                                            diffing). (1) MULTI_CHAR_OPS was missing `===`/`!==`, so
                                            `!==` lexed as separate tokens and got re-spaced into
                                            invalid `!= =`; fixed by adding both operators. (2) the
                                            braceless-collapse dispatch treated a do-while's
                                            trailing `while (cond)` as a loop-starting `while`,
                                            fusing the next statement onto the same line; fixed with
                                            an isDoWhileTailKeyword lookback.

  real_code_regressions_32_inp/out.kt    -- Kotlin, real-code test against `square/kotlinpoet`: a
                                            nested `when { ... }` used as a `when` branch's body
                                            flapped its closing `}`'s indentation round1-vs-round2,
                                            since Kotlin's braceLineIndent anchors on the brace's
                                            pre-merge physical line at Phase 0 but
                                            formatWhenExpressions' Phase 4 arrow-alignment pass
                                            later merges the branch label onto that same line. Fixed
                                            with a findMergingWhenBranchLineStart lookahead that
                                            anchors on the eventual post-merge line up front.

  real_code_regressions_33_inp/out.kt    -- Kotlin, real-code test against `square/kotlinpoet`: a
                                            first-pass compile-breaking bug found via `kotlinc` --
                                            an expression-bodied function whose body is itself a
                                            trailing-lambda call (`fun addTypes(...): T = apply {
                                            ... } as T`) had `apply`'s own unrelated `{` wrongly
                                            Allman-converted as the function's own body brace. Root
                                            cause: findSignatureCloseParenBeforeBrace's backward
                                            scan for `: ReturnType` had no bail-out on an
                                            intervening depth-0 `=`. Fixed with that bail-out.

  real_code_regressions_34_inp/out.hpp    -- C++, real-code test against `NVIDIA/stdexec`: combines
                                             two bugs. (1) A requires-expression
                                             compound-requirement's inner `}` (followed by `->`, not
                                             `;`) was misidentified by splitTopLevelSpans as a
                                             scope-closing brace, corrupting indentation
                                             non-idempotently; fixed by also checking for a
                                             following `->`. (2) Compile-breaking: semicolon-less
                                             macro-invocation statements before a `#if`/`#endif`
                                             guard caused splitStatements to never close the current
                                             statement, silently dropping the `#if` and cascading
                                             150+ downstream errors; fixed with a depth-0 check that
                                             always closes the statement at a preprocessor token.

  real_code_regressions_35_inp/out.hpp    -- C++, real-code test against `NVIDIA/stdexec`
                                             (continuing the candidate above): a compile-breaking
                                             bug ("Bug 3") in `__counting_scopes.hpp` --
                                             tryCollapse's renderInline flattened a multi-line `if`
                                             condition containing a `//` comment between call
                                             arguments, silently absorbing every following token
                                             (including the closing `}`) into the comment and
                                             producing a 50-error unmatched-brace cascade. Fixed
                                             with a containsLineComment guard that refuses the
                                             collapse when the condition carries a line comment.

  real_code_regressions_36_inp/out.cpp    -- C++, real-code test against `NVIDIA/stdexec`
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
                                             diffing this was the only remaining divergence, marking
                                             the stdexec candidate DONE.

  real_code_regressions_37_inp/out.kt    -- Kotlin, real-code test against
                                            `Kotlin/kotlinx.coroutines`: an expression-bodied
                                            function's unconsumed `{`-led lambda tail made
                                            `renderWithTail` bake a trailing space onto `= `
                                            regardless, growing the gap by one space per reformat.
                                            Fixed by omitting the space when `exprTokens` is empty.

  real_code_regressions_38_inp/out.kt    -- Kotlin, real-code test against
                                            `Kotlin/kotlinx.coroutines`: a KDoc's own nested `/* ...
                                            */` snippet closed the outer `/**` doc-comment early,
                                            mis-lexing and silently truncating the rest of the file
                                            (`Guidance.kt`, ~330 lines dropped). Fixed by tracking
                                            block-comment nesting depth, Kotlin-only.

  real_code_regressions_39_inp/out.kt    -- Kotlin, real-code test against
                                            `Kotlin/kotlinx.coroutines`: `this@Label` got a stray
                                            space inserted before `@` (`this @Label`, a real syntax
                                            error) since `enforceLabeledJumpSpacing`'s state machine
                                            didn't recognize `this` before `@`. Fixed with a new
                                            state pair tightening `this@Label`.

  real_code_regressions_40_inp/out.kt    -- Kotlin, real-code test against
                                            `Kotlin/kotlinx.coroutines`: `LimitedDispatcher.kt`'s
                                            collapsible `while (true) { when (...) { ... } }` body
                                            owned a nested multi-line `synchronized(...) { ... }`
                                            block; `tryCollapse`'s brace-depth-unaware flattening
                                            fused its statements onto one line with no separators --
                                            a real syntax error. Fixed with a
                                            `containsMultilineNestedBrace` bail in
                                            `isKotlinSingleStatementBody`.

  real_code_regressions_41_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `Kotlin/kotlinx.coroutines`: `SystemProps.kt`'s chained
                                            `catch` span kept its stale pre-merge indent once
                                            `KotlinSignatureRule` merged the `try` signature onto
                                            one line, disagreeing with the `try` span's re-derived
                                            indent round1 vs round2. Fixed by having a chained
                                            `catch`/`finally` span inherit its preceding span's
                                            resolved indent.

  real_code_regressions_42_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `Kotlin/kotlinx.coroutines`: a class with a wrapped
                                            multi-line generic `where` clause had its closing
                                            brace/comment indent drift deeper, since
                                            `effectiveSpanIndent` preferred the deeper
                                            continuation-line `braceIndent` over the header's own
                                            `spanIndent` (correct for unnamed lambda bodies,
                                            RDD_KEY_136, but wrong for named class/fun/object
                                            scopes). Fixed by gating `braceIndent` off for named
                                            scopes.

  real_code_regressions_43_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `Kotlin/kotlinx.coroutines`: a wrapped multi-argument
                                            call used as a keyword-less `when` branch body had its
                                            continuation lines one level deeper on round1 than
                                            round2, since `enforceCallLineBreaking` computed the
                                            base indent before the branch label/body got merged onto
                                            one line by a later phase. Fixed with
                                            `effectiveCallBaseIndent`, which uses the preceding `->`
                                            line's indent when present.

  real_code_regressions_44_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `Kotlin/kotlinx.coroutines`: a nested-lambda-chain's
                                            closing `}` drifted from col 4 to col 8 on round2.
                                            `findParentIndent`'s backward scan could anchor on a
                                            dangling braceless `else expr` (left as leading text at
                                            the start of the next span by `splitTopLevelSpans`),
                                            returning a wrong, unrelated line's indent. Fixed by
                                            skipping forward past a dangling `else`/`catch`/
                                            `finally` anchor to the next real statement.

  real_code_regressions_45_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `Kotlin/kotlinx.coroutines`: a `val` alignment group
                                            padded a typeless row to match a sibling's type-column
                                            width, widening that sibling's line enough to trigger a
                                            lambda-initializer wrap on the next pass, which then
                                            correctly bailed it out of the group -- an idempotency
                                            flap. Fixed by making `renderAlignedGroup` budget-aware:
                                            a row is excluded from the shared column grid up front
                                            when its own brace-bodied initializer would overflow the
                                            line-length budget once padded.

  real_code_regressions_46_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `square/kotlinpoet`'s Shape 1 idempotency-gap group (6
                                            files), two bugs in `enforceCallLineBreaking`. Bug A: a
                                            wrapped signature with a trailing `= apply { ... }` body
                                            re-collapsed on reformat because `lineEndIndex`'s width
                                            check stopped at the first NEWLINE, undercounting width
                                            when the tail's own nested call was already wrapped from
                                            a previous round; fixed with a depth-aware
                                            `effectiveLineEndIndex` that skips NEWLINEs still inside
                                            an unclosed bracket. Bug B (RDD_KEY_149, now
                                            root-caused): a signature with an explicit `: ReturnType
                                            {` block body got its correctly wrapped, padded param
                                            list re-wrapped as a plain call, discarding
                                            padding/trailing comma, since the "is this a call"
                                            exemption only recognized `{` right after `)`. Fixed
                                            with an `isKotlinReturnTypeThenBlockBody` lookahead.

  real_code_regressions_47_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `square/kotlinpoet`'s Shape 2 (`AbstractTypesTest.kt`):
                                            a multi-line generic `where` clause gained one extra
                                            indent level every round, since
                                            `enforceWhereClausePlacement` derived the base indent
                                            from `where`'s own (already-wrapped) physical line
                                            instead of the true signature line. Fixed with a
                                            `signatureLineIndent` helper that scans backward to the
                                            nearest depth-0 `;`/`}`/`{`.

  real_code_regressions_48_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `square/kotlinpoet`'s Shape 3: a `when` branch's
                                            multi-line body (nested `when(subject) { ... }` or a
                                            trailing-lambda call) had its closing `}` sit 2 spaces
                                            shallower on round2, since
                                            `findMergingWhenBranchLineStart` (RDD_KEY_152) only
                                            recognized a bare `when {` as the merging shape. Fixed
                                            by generalizing the lookahead to accept a parenthesized
                                            `when` subject or a plain call-head identifier.

  real_code_regressions_49_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `square/kotlinpoet`'s Shape 4: a `val` declaration's
                                            alignment padding flapped between rounds because a
                                            preceding sibling's `Foo::class` reflection literal
                                            wrongly armed `namedConstructKeywordSeen` (which only
                                            checks for the `class` KEYWORD, not a real class
                                            declaration), corrupting a later unrelated scope's name
                                            tracking. Fixed by never arming on a `class` KEYWORD
                                            preceded by `::`. Also fixed two side bugs found while
                                            root-causing this: an `ArrayIndexOutOfBoundsException`
                                            in `signatureLineIndent` (RDD_KEY_164) for a
                                            `where`-clause statement with no preceding boundary
                                            token, and a boundary-anchoring correction matching
                                            `real_code_regressions_47`'s original fix intent.

  real_code_regressions_50_inp/out.cpp    -- C++, real-code test against `ericniebler/range-v3`'s
                                             concept-emulation-macro convention
                                             (`template(...)`/`CPP_ret`/`CPP_member`, see
                                             `detail/prologue.hpp`): two compile-breaking bugs. (1)
                                             `extendOverLeadingRequiresAndTemplate` pulled a
                                             preceding `template(...)`-macro invocation onto a
                                             declarator's line without checking for a following `<`,
                                             gluing a `requires`-line's trailing `//` comment onto
                                             the declarator and commenting it out; fixed by
                                             requiring `<` before the pull and refusing to pull a
                                             `requires` line ending in a `//` comment. (2)
                                             `enforceEmptyParameterList`'s `IDENTIFIER(void)` ->
                                             `IDENTIFIER()` rewrite fired on the macro invocation
                                             `CPP_ret(void)(requires ...)`, deleting an argument the
                                             macro needs; fixed by never rewriting `(void)` when the
                                             matching `)` is immediately followed by another `(`.
                                             Verified with `g++ -std=c++20 -fsyntax-only` and full
                                             round1/round2 idempotency over range-v3's 311-file
                                             tree.

  real_code_regressions_51_inp/out.cpp    -- C++, follow-up to `_50`: another range-v3
                                             compile-breaking bug in `view/view.hpp`/
                                             `action/action.hpp`. A `;`-terminated declaration whose
                                             source spans multiple `//`-commented lines (a
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

  real_code_regressions_52_inp/out.cpp    -- C++, boost-ext/ut idempotency bug: a C++20
                                             deduction-guide statement immediately followed by an
                                             unrelated `struct suite { ... };` caused
                                             `enforceFunctionDefinitionAllmanBraceStyle`'s backward
                                             close-paren scan to cross the `;` boundary and
                                             misidentify the deduction guide's close-paren as the
                                             struct's own, Allman-converting its brace with a bogus
                                             indent that a later K&R re-collapse then joined back --
                                             a stable round1-vs-round2 diff. Fixed by making both
                                             backward close-paren scans stop at a depth-0 `;`, not
                                             just `{`/`}`. Verified with a minimal repro and full
                                             `make test` (71/71, no regressions).

  real_code_regressions_53_inp/out.cpp    -- C++, microsoft/proxy: 3 bugs in
                                             `CppSpecificRule.enforceRequiresClausePlacement`. (a)
                                             baseIndent/fit-check anchored on the trailing
                                             `requires` clause's preceding `)`, unstable across
                                             passes when that `)` sits on a continuation-alignment
                                             or dedented line; fixed by deriving from the parameter
                                             list's own opening paren instead, unwinding any chained
                                             trailing specifier (e.g. `noexcept(...)`). (b)/(c) a
                                             preprocessor directive inside the clause's constraint
                                             expression got spliced mid-line by `collapseToOneLine`,
                                             producing invalid C++; fixed by leaving any clause
                                             containing a `PREPROCESSOR` token untouched. Verified
                                             with `clang++ -std=c++23 -fsyntax-only` and full
                                             round1/round2 idempotency over `microsoft/proxy`.

  real_code_regressions_54_inp/out.java   -- Java, javaparser/javaparser real-code testing: 2 bugs.
                                             (a) `GetterSetterRule.parseOneLinerMember` misparsed a
                                             braceless `if (cond) throw new X(...)`/ `if (cond)
                                             return ...` as a one-liner getter/setter, grid-aligning
                                             bogus padding that grew unboundedly across passes;
                                             fixed by rejecting any candidate whose "return type"
                                             span contains a control-flow keyword. (b)
                                             `MiscRule.stripSoleTrailingPeriod`/
                                             `stripSoleTrailingPeriodAcrossLines` stripped a
                                             comment's sole trailing `.` but left the preceding
                                             whitespace, a stray-space idempotency bug; fixed by
                                             trimming trailing whitespace in both methods. Verified
                                             with minimal repros and full `make test`.

  real_code_regressions_55_inp/out.java   -- Java, javaparser/javaparser real-code testing
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

  real_code_regressions_56_inp/out.java   -- Java, javaparser/javaparser real-code testing
                                             (continued): `Formatter.formatOne`'s Phase 1 ran
                                             `MiscRule.enforceCallLineBreaking`'s fit measurement
                                             BEFORE `SwitchRule.formatNonInlineSwitches` reindents
                                             switch-case bodies one level deeper, so a boundary call
                                             measured "fits" against the pre-reindent column and
                                             stayed unwrapped -- stable only on a second pass. Found
                                             in `CsmAttribute.java`'s `getTokenType`. Fixed by
                                             re-running `enforceCallLineBreaking` again right after
                                             `formatNonInlineSwitches` (idempotent no-op otherwise).
                                             Verified: minimal repro, real `CsmAttribute.java`
                                             round1/round2 byte-identical, both `TypeExtractor.java`
                                             copies still byte-identical (no regression on bug 4's
                                             fix), full `make test` 79/79.

  real_code_regressions_57_inp/out.java   -- Java, javaparser/javaparser real-code testing
                                             (continued):
                                             `DeclarationAlignmentRule.isCStyleCastClose`
                                             misclassified a braceless control-flow condition's own
                                             closing paren (`if(node instanceof RecordPatternExpr)`)
                                             as a C-style cast close, because its guard excluded
                                             IDENTIFIER/`)`/`]` before the matching `(` but not
                                             control-flow KEYWORD tokens, suppressing a required
                                             space when the construct was rendered as a
                                             declaration's initializer via `renderInitTokens`. Fixed
                                             by adding a `CONTROL_FLOW_KEYWORDS` exclusion set to
                                             `isCStyleCastClose`. Found in `Java1_0Validator.java`/
                                             `Java5Validator.java`. Verified: minimal repro, both
                                             real files round1/round2 byte-identical, full `make
                                             test`.

  real_code_regressions_58_inp/out.java   -- Java, javaparser/javaparser real-code testing
                                             (continued): a Java enum constant list
                                             (`BEGIN_TOKEN("beginToken"), END_TOKEN("endToken");`)
                                             shares its top-level shape with a comma-separated
                                             C-style multi-declarator statement, so
                                             `DeclarationAlignmentRule.parseDeclaration` could merge
                                             it into an unrelated adjacent field's alignment group,
                                             and `JavaSpecificRule.findEnumConstantListTerminators`
                                             derived its re-emitted indent from the first member's
                                             own current (possibly drifted) line indent instead of
                                             an absolute recompute, compounding drift each pass.
                                             Fixed by (a) isolating a Java enum-constant-list
                                             statement into its own singleton group in
                                             `DeclarationAlignmentRule.groupDeclarations` (new
                                             `isJavaEnumConstantListShape` helper), and (b) deriving
                                             `findEnumConstantListTerminators`'s indent from the
                                             enum body's own stable `{`-line indent plus one indent
                                             unit. Found in `JavaParserJsonSerializer.java`.
                                             Verified: minimal repro, real file round1/round2
                                             byte-identical, full `make test`.

  real_code_regressions_59_inp/out.kt    -- Kotlin, arrow-kt/arrow real-code testing: a generic
                                            bound's `:` (e.g. `<A : Comparable<A>>`) wasn't
                                            recognized as generic-safe by
                                            `TokenizerCore.isGenericSafeToken`, invalidating the
                                            angle-bracket tracking stack so a second bound's `>>`
                                            stayed unsplit, corrupting both bounds' spacing. Fixed
                                            by adding a Kotlin-gated `:` case to
                                            `isGenericSafeToken`'s `OP` branch. Found in
                                            `arrow-core`'s `Pair.kt` (`compareTo` extension).
                                            Verified: minimal repro, full `make test`.

  real_code_regressions_60_inp/out.kt    -- Kotlin, arrow-kt/arrow real-code testing (found via
                                            `kotlin_sc` compile-checking round1's output, not
                                            round1/round2 diffing): `BlockStructureRule
                                            .isKotlinSingleStatementBody` let a braced `if` body
                                            whose sole statement was a `val`/`var` declaration
                                            collapse to braceless form (`if (x) val y = ...`), which
                                            is illegal Kotlin. Fixed by disqualifying a body whose
                                            first token is `val`/`var` from collapse, same as
                                            `COMPOUND_BODY_KEYWORDS` does for nested compound
                                            bodies. Found in `RaiseAccumulate.kt`'s `addErrors`.
                                            Verified: minimal repro, full `make test`.

  real_code_regressions_61_inp/out.kt    -- Kotlin, arrow-kt/arrow real-code testing (also found via
                                            `kotlin_sc`): `MiscRule.needsSpaceBetween` had no
                                            tight-after case for a Kotlin annotation's `@` when it
                                            shares its source line with the function signature
                                            (rendered through `MiscRule.renderTokens`'s shared join
                                            point, used by `KotlinSignatureRule`); the default
                                            space-insert fallback produced invalid `@ RaiseDSL`.
                                            Fixed by adding a Kotlin-gated tight-after case for `@`.
                                            Kotlin's other `@`-uses (`return@label`, `label@`,
                                            `this@Label`) go through a separate rule
                                            (`KotlinSpecificRule .enforceLabeledJumpSpacing`) and
                                            are unaffected. Found in `RaiseAccumulateContext.kt`'s
                                            `mapOrAccumulate`. Verified: minimal repro, full `make
                                            test`.

  real_code_regressions_62_inp/out.kt    -- Kotlin, arrow-kt/arrow real-code testing: two
                                            idempotency bugs deferred by RDD_KEY_173. (A)
                                            RDD_KEY_174 --
                                            `KotlinSignatureRule.parseKotlinSignature`'s first
                                            `IDENTIFIER (` scan mistook a leading `context(raise:
                                            Raise<Error>)` clause's paren for the real parameter
                                            list when both shared one line, bailing instead of
                                            continuing the scan (`RaiseContext.kt`'s
                                            `ensureNotNull`). (B) RDD_KEY_175 -- `Formatter.java`
                                            ran `formatWhenExpressions` after `addClosingComments`
                                            had already counted `closing-comment-min-lines` against
                                            the enclosing `for` loop, dropping its `// for` comment
                                            on a fresh format; fixed by reordering the passes
                                            (`Iterable.kt`'s `separateEither`). Verified: minimal
                                            repro + both real files round1/round2 byte-identical +
                                            full `make test`.

  real_code_regressions_63_inp/out.kt    -- Kotlin, arrow-kt/arrow real-code testing: RDD_KEY_176 --
                                            `BlockStructureRule.collapseBracelessBody`'s
                                            bare-`else`/ braceless-`if` body scan never checked
                                            whether the body was a single statement once it could
                                            itself own a multi-line `{...}` block (e.g. a
                                            trailing-lambda call); `renderInline` fused the block's
                                            internal statements with no `;` separator, a genuine
                                            compile error. Fixed by reusing
                                            `containsMultilineNestedBrace` as a bail-out guard.
                                            Found in `Either.kt`'s `zipOrAccumulate`. Verified:
                                            `kotlin_sc` on `Either.kt` (18 errors -> 0), full `make
                                            test`.

  real_code_regressions_64_inp/out.kt    -- Kotlin, arrow-kt/arrow real-code testing: RDD_KEY_177,
                                            closing item of the investigation. Pure idempotency flap
                                            in `Comparison.kt`'s `sort2`:
                                            `collapseSingleExpressionBlocks`'s
                                            `isKotlinExpressionIf` exemption only covered a
                                            parenthesized expression-position `if`, not an
                                            unparenthesized depth-0 if-expression used as an entire
                                            expression-bodied function's whole body, so a fresh
                                            format and a reformat of already-wrapped output
                                            converged to two different stable states.

  real_code_regressions_65_inp/out.java   -- Java, local `src/jxm` real-code testing: two
                                             idempotency bugs combined in one fixture (RDD_KEY_171,
                                             RDD_KEY_172). (1)
                                             `TokenizerCore.reclassifyAngleBrackets` had no case for
                                             a literal `>>>` token (triple-nested generics); round2
                                             re-lexed round1's tight `>>>` as one token and
                                             mis-spaced the generics. Fixed by adding an explicit
                                             `>>>` case generalizing the existing `>>` split to 3
                                             nesting levels. (2)
                                             `JavaSpecificRule.isSingleLineBody`'s fits-under-limit
                                             prediction omitted leading indentation and any trailing
                                             same-line `//` comment, causing a K&R-vs-Allman
                                             flip-flop across rounds. Fixed by including both in the
                                             measurement, whitespace-collapsed like
                                             `collapseToOneLine`.

  real_code_regressions_66_inp/out.java   -- Java, local `src/jxm` real-code testing: RDD_KEY_178,
                                             two bugs in STYLE.md §8's multi-line parameter-list
                                             renderer (`MiscRule.render` and its near-duplicate
                                             multi-line- declaration renderer) around a standalone
                                             `//` banner comment used as a section divider between
                                             parameter groups (`SWDFlashLoader.Specifier`'s
                                             constructor, `STM32QSPI.newQSPICmd`). (1) A leading
                                             `//` line comment was inlined onto the same output line
                                             as the following parameter's type+name, swallowing that
                                             declaration (and cascading to the next) into the
                                             comment -- compile- breaking. Fixed by emitting it on
                                             its own line. (2) The shared type/name column width was
                                             computed only over params with no leading comment, so
                                             an excluded param's `typeText` could be as long as the
                                             column width, making `padRight` a no-op and merging
                                             type+name with zero space on the next pass. Fixed by
                                             never padding to less than `typeText.length() + 1`.
                                             Verified idempotent against both real files plus this
                                             fixture.

  real_code_regressions_67_inp/out.hpp    -- RDD_KEY_169: a named construct (struct/namespace) whose
                                             base-clause is guarded by #if/#endif, with the body `{`
                                             immediately following the bare #endif line. Proves
                                             enforceKAndRBraceStyle no longer glues the `{` onto the
                                             #endif line (which a later retokenize would swallow
                                             into the PREPROCESSOR token, desyncing brace/frame
                                             tracking and producing wrong closing-comment
                                             labels/indentation -- originally found via
                                             ericniebler/range-v3 item 20 bug (a), see
                                             STATE_C_CPP_JAVA.md Open Questions).

  real_code_regressions_68_inp/out.json   -- JSON, microsoft/vscode real-code testing:
                                             non-idempotent empty-container rendering.
                                             JsonSpecificRule.parseContainer kept a dangling
                                             placeholder Item for a comment-less blank line before
                                             closing `}`/`]`, so round1 emitted loose `{\n}` but
                                             round2 (finding no blank line to preserve) collapsed it
                                             to `{}`. Fixed by only keeping the placeholder when a
                                             real leading comment exists; a comment-less blank line
                                             before the closer is now dropped during parsing. No
                                             copyright-header block on this fixture -- plain `.json`
                                             has no comment syntax to carry it.

  real_code_regressions_69_inp/out.css    -- CSS, twbs/bootstrap real-code testing
                                             (content-preservation check, not syntax-check -- still
                                             parses as valid CSS): normalize-comment-start-case
                                             unconditionally capitalized case-sensitive rtlcss
                                             directive comments (e.g. `/* rtl:begin:ignore */` ->
                                             `/* Rtl:begin:ignore */`), silently breaking rtlcss's
                                             directive parsing. Fixed via new
                                             `FormatterSimpleBraced.isSingleTokenDirective`
                                             exemption: a single-line comment whose whole trimmed
                                             body is one whitespace-free token containing `:` or `-`
                                             is left alone; ordinary prose is still capitalized as
                                             before.

  real_code_regressions_70_inp/out.toml   -- TOML, rust-lang/cargo real-code testing (first TOML
                                             dogfood run): two forward-pass crash bugs. (1) A
                                             multi-line array's interior per-element trailing `#`
                                             comment was treated as extending to the end of the
                                             joined logical line, swallowing the array's closing `]`
                                             as comment text ("unterminated array"). Fixed by
                                             stripping each continuation line's own trailing comment
                                             before joining, not just the final result's. (2)
                                             Multi-line basic/literal strings
                                             (`"""..."""`/`'''...'''`) were entirely unsupported,
                                             crashing ("expected 'key = value' line") on a `key =
                                             """` block. Fixed by detecting an unterminated
                                             multi-line-string opener before the bracket-balance
                                             check and consuming raw lines verbatim to the matching
                                             closing delimiter, same opaque treatment as JSON5's
                                             multi-line strings.

  real_code_regressions_71_inp/out.yaml   -- YAML, kubernetes/kubernetes real-code testing (first
                                             YAML dogfood run): six combined bugs. (1) A
                                             sequence-of-mapping's first key rejected a same-indent
                                             nested sequence child (common "- apiGroups:\n    -
                                             \"*\"" manifest style); fixed by mirroring the same
                                             rule plain mapping keys already had. (2)/(3) Quoted and
                                             plain scalars wrapping across physical lines (common in
                                             CRD/API description fields) crashed the line-based
                                             parser; fixed by detecting an unterminated quote /
                                             deeper continuation line and capturing it as an opaque
                                             multi-line scalar, for both plain keys and
                                             sequence-of-mapping first keys. (4) A dangling
                                             trailing-comment item (null key) inside a
                                             sequence-of-mapping's children threw an NPE in the
                                             colon-alignment padding helper; fixed by excluding
                                             dangling items from the padding key list. (5)
                                             Idempotency-only: multi-line scalar/block-scalar
                                             continuations stored their indentation as an ABSOLUTE
                                             value rather than a delta relative to their own key, so
                                             a shift in the key's rendered column (from
                                             colon-alignment padding etc.) broke idempotency on a
                                             second pass; fixed by storing/re-anchoring a RELATIVE
                                             delta instead. (6) A `|`/`>` block scalar as a plain
                                             (non-keyed) sequence item's own value (e.g.
                                             "command:\n- |\n  script text") was silently truncated
                                             to an empty string -- found via the
                                             content-preservation check, since the truncated output
                                             was still syntactically valid YAML. Fixed by adding the
                                             same block-scalar (and multi-line-quoted-scalar)
                                             detection to that sequence-item parser branch.

  real_code_regressions_72_inp/out.yaml   -- YAML, docker/compose real-code testing: a data-loss bug
                                             found via the content-preservation check (corrupted
                                             output still syntactically valid YAML). A blank line
                                             immediately after a keyed line with no inline value
                                             (e.g. "services:" then a blank line then its nested
                                             mapping) caused the whole nested block to be silently
                                             dropped -- every "does this key have a child block"
                                             detection site used a plain `peek()`, so a blank next
                                             line was treated as "no child". Fixed by adding a
                                             `peekNonBlank()` helper (looks past blank lines without
                                             consuming them) and using it at all four detection
                                             sites instead of `peek()`.

  real_code_regressions_73_inp/out.yaml   -- YAML, ansible/ansible real-code testing: three combined
                                             bugs, all found via the content-preservation check
                                             (every corrupted output stayed syntactically valid
                                             YAML). (1) A plain (non-keyed) sequence item's own
                                             unquoted scalar value wrapping across physical lines
                                             (e.g. a changelog fragment) had no continuation
                                             handling at all, silently dropping every line past the
                                             first; fixed by adding the same multi-line-plain-scalar
                                             capture used for keyed/seqOfMapping-first-key values,
                                             with the continuation's baseline column at the scalar's
                                             own start. (2) A comment dedented below its enclosing
                                             block's indent (a real "# FIXME: ..." note at column 0
                                             between deeper-indented sibling keys) made `parseBlock`
                                             break out of every enclosing block in turn without
                                             consuming it, permanently orphaning it and dropping
                                             everything that followed at every level; fixed by
                                             looking past the comment (and any more like it) to the
                                             next real content line and attaching the comment to
                                             whichever block that line's own indent belongs to. (3)
                                             A bare top-level plain scalar document (e.g. an
                                             `$ANSIBLE_VAULT;...` header followed by opaque unquoted
                                             hex data with no "key:"/"- " shape) only kept its first
                                             line, dropping the rest; fixed by emitting the
                                             remaining raw lines verbatim once this bare-scalar-
                                             document shape is detected.

  real_code_regressions_74_inp/out.svg    -- XML, w3c/svgwg real-code testing: `.svg` files were
                                             never mapped to the "xml" language in `Lang.infer`, so
                                             every `.svg` in the corpus failed with "could not infer
                                             language from file extension" -- found via the forward
                                             pass itself erroring, before any syntax-check/content-
                                             preservation could even run. Fixed by adding `.svg`
                                             alongside `.xml` in `Lang.infer`'s extension check.

  real_code_regressions_75_inp/out.yaml   -- YAML, actions/starter-workflows real-code testing
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

  real_code_regressions_76_inp/out.hpp    -- C++26, simdjson/experimental_json_builder real-code
                                             testing: `enforceAttributeAndSpliceBracketPadding`'s
                                             loose `[: expr :]` padding ran in Phase 4, after
                                             `enforceCallLineBreaking` had already measured/decided
                                             not to wrap a line right at the length limit -- a fresh
                                             format saw the pre-padding width and stayed one line,
                                             while reformatting that already-padded output saw the
                                             now-over-limit width and wrapped, a non-idempotent
                                             round1/round2 mismatch. Found via idempotency diffing.
                                             Fixed by pulling
                                             `enforceAttributeAndSpliceBracketPadding` forward to
                                             run right before `enforceCallLineBreaking`, same fix
                                             shape already used for `enforceComplexityPadding`.

  real_code_regressions_77_inp/out.js     -- JS, expressjs/express real-code testing: two combined
                                             bugs. (1) ASI (§2 semicolon insertion): a leading-
                                             continuation-operator/comma line (method chaining on
                                             its own line, or a comma-first multi-declarator list)
                                             was wrongly treated as ending the previous statement --
                                             `needsSemicolonAfter` only checked the previous line's
                                             own trailing token, never the next line's leading
                                             token, so a bogus `;` landed mid-chain/mid-declarator-
                                             list. Found via `node --check` on round1 output. Fixed
                                             by adding a leading-operator/comma lookahead alongside
                                             the existing trailing-operator check. (2) The tokenizer
                                             had no JS/TS regex-literal recognition at all -- a bare
                                             `/` was always treated as the division operator, so a
                                             regex containing a `"` inside a bracketed character
                                             class got its `"` mistaken for a string literal,
                                             corrupting brace/paren tracking for the rest of the
                                             statement. Found via `node --check` on round1 output.
                                             Fixed by adding `TokenizerCurly.emitRegexLiteral`/`isRe
                                             gexLiteralAllowedHere` (regex-vs-division
                                             disambiguation based on the preceding significant
                                             token), emitting the whole literal as one opaque
                                             `STRING` token.

  real_code_regressions_78_inp/out.py     -- Python3, pallets/flask real-code testing (first Python3
                                             dogfood run): a non-idempotency bug found via `diff -r
                                             round1 round2` (a formatter under-application bug, not
                                             scoping corruption -- round1's tree was already
                                             semantically correct). `ScopePipelineIndent.render`'s
                                             replacement-merge loop advanced its replacement-list
                                             cursor `r` only on an exact `start == i` match;
                                             whenever two independently-computed passes legitimately
                                             produced overlapping token-range replacements, the now-
                                             stale entry permanently stalled `r`, silently dropping
                                             every later replacement in the file, not just the
                                             genuinely-overlapping one. Fixed by having `render`
                                             skip past (not get stuck on) any replacement whose
                                             `start` has already been passed by `i`.

  real_code_regressions_79_inp/out.py     -- Python3, pallets/flask real-code testing (same run as
                                             fixture 78): two more bugs found via the same final
                                             idempotency re-check after fixture 78's fix. (1) §6
                                             signature alignment: `trySignatureGroup` split a
                                             signature's interior into per-parameter segments purely
                                             on raw `NEWLINE` tokens, not bracket-depth-aware -- a
                                             parameter whose type hint spans multiple physical lines
                                             via a still-open nested bracket got its continuation
                                             lines misclassified as separate bogus parameters
                                             instead of triggering the documented "parameter spans
                                             multiple physical lines -- leave whole signature
                                             untouched" gap, corrupting the signature with growing
                                             trailing whitespace each idempotency round (non-
                                             convergent). Fixed by only splitting segments at a
                                             `NEWLINE` when local bracket depth is 0, and rejecting
                                             any segment that still contains an embedded `NEWLINE`
                                             in `classifySignatureParam`. (2) §9.2 blank-line-
                                             before-`elif`/`else` and §8's single-statement join
                                             could produce a replacement starting at the same token
                                             index, and tie-breaking let §8's statement-join swallow
                                             §9.2's zero-width blank-line insertion. Fixed by
                                             sorting zero-width entries first on ties.

  real_code_regressions_80_inp/out.py     -- Python3, pallets/click real-code testing. §4 decorator
                                             bracket-padding (`applyBracketPadding`) recursively pads
                                             every `(`/`[`/`{` pair in a decorator's own expression,
                                             but couldn't distinguish an f-string field's own `{`/`}`
                                             (emitted as plain PUNCT by `TokenizerIndent
                                             #emitFStringField`, same token shape as a dict/set
                                             literal brace) from an actual dict/set literal -- it
                                             padded an f-string field nested inside a decorator's
                                             lambda default argument as if it were a non-empty (thus
                                             always-loose per §1.5) brace pair, producing `f"{
                                             ctx.info_name }"` on the forward pass. §5's own f-string
                                             spacing pass then unconditionally trimmed that same gap
                                             back on the *next* formatting round, so the bug only
                                             surfaced as non-idempotency, not a single-pass mismatch.
                                             Fixed by skipping a `{`/`}` pair entirely in
                                             `applyBracketPadding` whenever the `{` is immediately
                                             preceded by an `FSTRING_START`/`FSTRING_MIDDLE` token --
                                             that adjacency only ever occurs for an f-string field's
                                             own delimiters, never a dict/set literal's.

  real_code_regressions_81_inp/out.ts     -- JS/TS, nestjs/nest real-code testing. A multi-arg call
                                             whose every argument is a bare dotted member-access
                                             expression with no top-level comma of its own (`options.
                                             provideInjectionTokensFrom`, `options.inject`) could be
                                             misparsed by `MiscRuleCurly.renderCallCandidate`'s
                                             `parseSignature` call as a real C/C++/Java-style
                                             "type name" forward-declaration parameter list (`type`
                                             `options.`, `name` `provideInjectionTokensFrom`) --
                                             the exact same misparse already known and guarded for
                                             Kotlin only (RDD_KEY -- see `sigForRender`'s doc comment
                                             in `MiscRuleCurly`), but the guard was never extended to
                                             JS/TS. The typed dropped/one-per-line render path then
                                             inserted a column-separator space between the bogus
                                             "type" and "name", corrupting the expression into
                                             `options. provideInjectionTokensFrom` -- compounding on
                                             every re-format pass. Fixed by forcing `sigForRender` to
                                             `null` for JS/TS as well as Kotlin, since neither
                                             language has a prototype-only forward-declaration shape
                                             either (a JS/TS function declaration always has an
                                             immediate `{` body, already exempted earlier in the same
                                             method).

  real_code_regressions_82_inp/out.ts     -- JS/TS, nestjs/nest real-code testing. Content
                                             duplication: `JsTsSpecificRule.
                                             enforceClassFieldAlignmentGrid`'s single linear
                                             `cursor` sweep over every brace classified `CLASS`
                                             assumed every selected class span was disjoint, but an
                                             anonymous `return class extends Base { ... };`
                                             expression nested inside an outer class's method (seen
                                             in `Module.createModuleReferenceType`) is a real,
                                             legitimate nesting. The outer class's own
                                             `rewriteClassFieldGroups` call already copies the
                                             entire inner class span through byte-for-byte (as an
                                             ordinary unrecognized member), so re-processing the
                                             inner span again as its own top-level `classOpens`
                                             entry both duplicated its content and walked `cursor`
                                             backward to the inner class's own (earlier) `closeIdx`
                                             -- causing the method's final raw-copy loop to re-emit
                                             everything from there to the true end of file a second
                                             time. Fixed by filtering `classOpens` to keep only the
                                             outermost class brace at each nesting level (a nested
                                             class's own fields don't get the alignment-grid
                                             treatment -- same conservative "only rewrite what's
                                             fully understood" posture as the rest of this method).

  real_code_regressions_83_inp/out.yaml   -- YAML, prometheus/prometheus real-code testing.
                                             Combines four distinct data-loss bugs found in
                                             `YamlSpecificRule`, all sharing the theme of a dash/key
                                             line whose "value" is absent/comment-only/anchor-only/an
                                             unbalanced multi-line flow opener, with real content on
                                             following more-indented lines: (1) `parseKeyItem`'s early
                                             return for a flow-looking value didn't check whether the
                                             flow was actually closed on the same physical line,
                                             truncating everything after an unbalanced multi-line
                                             `[...]` opener; (2) `parseSeqItem`'s `seqOfMapping`
                                             first-key handling had the same gap for
                                             `- source_labels:\n    [...]`, and separately dropped
                                             everything after a `- # comment`-only dash line whose
                                             real mapping keys start on subsequent lines; (3) a dash
                                             line holding only an anchor tag (`- &highalert`) followed
                                             by a nested mapping at an indent equal to (not just
                                             greater than) the dash's own key column lost its child
                                             block entirely; (4) `renderFlowValue` rendered an empty
                                             flow map/seq (`{}`/`[]`) as a block conversion whenever
                                             its line didn't `fits()`, but `renderFlowBlock`'s loop
                                             has nothing to iterate for zero entries, so the value
                                             silently vanished -- happened whenever a long key
                                             elsewhere in the same colon-alignment group pushed an
                                             unrelated `{}`/`[]` line past the width limit (seen in
                                             `web/ui/pnpm-lock.yaml`'s dependency snapshot maps, most
                                             visibly when the dropped key was the file's very last
                                             line).

  real_code_regressions_84_inp/out.ts     -- JS/TS, nestjs/nest real-code testing.
                                             Comment-continuation-indent drift /
                                             arbitrary-deep-indent corruption on an object-shaped
                                             `type X = { ... } & Y;` intersection alias.
                                             `JsTsSpecificRule.enforceUnionTypeContinuationIndent`
                                             (STYLE_JS_TS.md 11.1's union/intersection
                                             continuation-line re-alignment) detects a `type NAME =
                                             ...;` RHS as eligible whenever it contains a depth-0
                                             `|`/`&` and spans multiple physical lines, then
                                             re-indents *every* NEWLINE from the RHS's start through
                                             the terminating `;` to the RHS's own column -- with no
                                             bracket-depth tracking of its own. For a plain
                                             multi-line union list (`A |\n B |\n C`) every NEWLINE
                                             genuinely is a top-level break, so this was harmless,
                                             but an intersection whose left operand is a multi-line
                                             object-type literal (`{ ... } & Y`) has NEWLINEs nested
                                             many bracket-levels deep that belong to the object
                                             body's own (already-correct) indentation -- those got
                                             force-reindented to the alias's RHS column too, blowing
                                             every member out to an arbitrarily deep column matching
                                             the `type NAME = ` prefix's length. A leading `/** ...
                                             */` JSDoc comment on one member is a single token, so
                                             only the NEWLINE immediately before it (its own line)
                                             was corrupted this way on a first pass -- its interior
                                             continuation lines, untouched by this pass, drifted
                                             further out of sync (matching the deep column) only
                                             once a second re-format pass reindented the token
                                             *before* it again, compounding the mismatch. Fixed by
                                             tracking bracket depth in the re-indent loop itself and
                                             only re-indenting a NEWLINE found at the union/
                                             intersection's own top level (depth 0), leaving any
                                             NEWLINE nested inside a bracketed sub-shape (object-type
                                             literal, tuple, generic argument list, ...) completely
                                             untouched.

  real_code_regressions_85_inp/out.ts     -- JS/TS, nestjs/nest real-code testing.
                                             `join(...)` call-wrap/collapse non-idempotency:
                                             `MiscRuleCurly.renderCallCandidate`'s multi-line-source
                                             (`containsNewline(paramsSlice)`) branch always
                                             preserved the call's original per-line argument
                                             grouping (Option 2) unconditionally, with no fits-check
                                             of its own -- unlike the sibling single-line branch a
                                             few lines below, which correctly collapses (Option 0)
                                             whenever the call already fits under the line-length
                                             limit. A call an author (or a previous format pass) had
                                             wrapped across multiple lines therefore stayed wrapped
                                             forever even once it easily fit back onto one line,
                                             while the exact same call written fresh on one line
                                             collapsed correctly -- the same logical call could
                                             settle into two different stable shapes depending
                                             purely on incidental prior formatting, and one sitting
                                             right at the boundary (`integration/repl/e2e/
                                             repl-process.spec.ts`'s `localPackageResolver = join(
                                             workspaceRoot, '...')`, exactly 100 characters
                                             collapsed) flipped between a preserved multi-line form
                                             and a freshly-collapsed one-line form across repeated
                                             passes -- non-idempotent. Fixed by adding the same
                                             fits-check to this branch (JS/TS-only, `sigForRender ==
                                             null && (lang.isJs || lang.isTs)` -- widening to
                                             C/C++/Java regressed `real_code_regressions_1`, since a
                                             plain call's `sigForRender` is `null` there for the
                                             ordinary "not a real signature" reason too, not a
                                             JS/TS-specific misparse signal): measures the actual
                                             tight single-line candidate text (not the existing
                                             sibling branch's loose whitespace-collapsing
                                             `collapseToOneLine` helper, which turns the newline that
                                             originally followed the call's own `(` into a phantom
                                             single space and overestimates length by up to 2
                                             characters, wrongly disqualifying a call that truly
                                             fits) and collapses to one line whenever it fits,
                                             dropping any dangling trailing empty argument group
                                             (`splitTopLevelCommas`, unlike `groupByOriginalLine`,
                                             doesn't drop a trailing comma's empty tail itself) so a
                                             trailing-comma multi-line source doesn't gain a spurious
                                             trailing `, ` before `)` once collapsed. (Also required
                                             updating `real_code_regressions_81_out.ts`'s own
                                             expected output -- its `getInjectionProviders(...)` call
                                             now correctly collapses to one line too, since it fits;
                                             the old expected shape had itself been an artifact of
                                             this same bug.)

  real_code_regressions_86_inp/out.yaml   -- YAML, home-assistant/core real-code testing.
                                             Nested-sequence data loss: a sequence item whose own
                                             value is itself another sequence, written in the
                                             compact single-line form `- - a\n  - b` (e.g.
                                             `supported_features:\n  - - X\n    - Y`, common in this
                                             repo's `services.yaml` mutually-exclusive-feature-group
                                             pairs). `parseSeqItem` never recognized this shape --
                                             the inner `- ` was captured as a literal scalar value
                                             (`"- X"`), leaving the sibling nested-seq item on the
                                             next physical line completely unconsumed in the line
                                             stream. That orphaned line's indent then didn't match
                                             the enclosing block's own `blockIndent`, so
                                             `parseBlock`'s loop broke out of the entire enclosing
                                             block early -- silently dropping the rest of the nested
                                             sequence AND every sibling item/key that followed it, at
                                             every level (in the real files, everything from the
                                             second feature-group entry onward, including unrelated
                                             later top-level keys like `fields`/`stop_cover`). Fixed
                                             by detecting the `code.equals("-") ||
                                             code.startsWith("- ")` shape up front in `parseSeqItem`
                                             and parsing it via a new `parseInlineNestedSeq` helper:
                                             the first nested item's value comes from the inline
                                             dash-line text itself (recursing one more level for a
                                             further-nested `- - - x`), and any sibling nested items
                                             on subsequent physical lines (at the same column as the
                                             inline dash) are parsed via the ordinary `parseBlock`.
                                             Rendered via the existing generic `item.children`
                                             sequence-render path, which non-lossily expands the
                                             compact `- -` form into a bare dash followed by the
                                             nested items one level deeper -- always valid YAML, no
                                             data loss, same "prefer an unambiguous expanded form"
                                             precedent as this formatter's flow-to-block conversion
                                             elsewhere. Found via `yaml_content_diff.py` content-
                                             preservation checking (not syntax-check -- the corrupted
                                             output stayed syntactically valid YAML), consistent with
                                             every other YAML dogfood session's bug history.

  real_code_regressions_87_inp/out.ts     -- JS/TS, vuejs/core real-code testing.
                                             Leading multi-line block comment reindent
                                             non-idempotency: `JsTsSpecificRule`'s class-field
                                             alignment grid (`flushClassFieldGroup`), enum-member
                                             formatting (`rewriteEnumBody`), and interface/type-alias
                                             member alignment (`enforceInterfaceTypeAliasMemberColon
                                             Alignment`) all re-emitted a member's captured leading
                                             `/** ... */` comment verbatim, including whatever
                                             absolute indentation its continuation lines happened to
                                             carry at the *original* source depth -- never reindented
                                             to match the member's own (possibly different, e.g. this
                                             formatter's 4-space default vs. a 2-space source
                                             convention) re-rendered indent depth. A first pass left
                                             the comment's continuation lines visually misaligned
                                             under the field/member they precede; a second pass then
                                             caught up (a general block-comment reindent pass
                                             elsewhere normalizes it), producing a stable-but-wrong
                                             round1 and a different round2. Fixed by adding
                                             `reindentLeadingComment` (strips each continuation line's
                                             existing leading whitespace and reconstructs
                                             `indentPrefix + " " + strippedLine`) and calling it at
                                             all three sites instead of emitting the captured comment
                                             text raw.

  real_code_regressions_88_inp/out.ts     -- JS/TS, vuejs/core real-code testing.
                                             `TokenizerCurly.GENERIC_SAFE_KEYWORDS` was missing the
                                             TS primitive type keywords `symbol`/`bigint` (an earlier
                                             fix added `string`/`number`/`boolean`/etc. but missed
                                             these two), and `isGenericSafeToken`'s OP case had no
                                             entry for `|` -- both meant a union type written directly
                                             inside a generic argument list (`Record<string | symbol,
                                             Function | number>`) invalidated the enclosing `<...>`
                                             tracking, leaving the closing `>` a plain OP token instead
                                             of ANGLE_BRACKET_CLOSE. That defeated
                                             `JsTsSpecificRule.enforceSemicolonInsertion`'s
                                             CONTINUATION_OPS check (a trailing `>` OP looks like an
                                             unfinished comparison), silently dropping the statement's
                                             semicolon; `JsTsDeclarationAlignmentRule.parseTypeAlias`'s
                                             depth-scan (tracking only `(`/`[`/`{`, not real angle-
                                             bracket depth) then ran past the missing `;` into
                                             unrelated following statements (a function declaration,
                                             then a const), corrupting them with alignment-grid column
                                             padding matching the type alias's own keyword+name+`=`
                                             width. Fixed by adding `symbol`/`bigint` to
                                             `GENERIC_SAFE_KEYWORDS` and `|` (TS-only) to
                                             `isGenericSafeToken`'s OP case.

  real_code_regressions_89_inp/out.ts     -- JS/TS, vuejs/core real-code testing
                                             (componentOptions.ts), two further distinct bugs found
                                             chasing the same file's remaining non-idempotency after
                                             the _88 fix above:
                                             (1) `JsTsDeclarationAlignmentRule.parseTypeAlias`'s
                                             generic-parameter-list skip loop (only reached when the
                                             clause's own content, e.g. a type-parameter default
                                             `<T = X>`, invalidates the tokenizer's `<`/`>`
                                             reclassification) advanced past the clause's tokens
                                             without ever capturing them for re-rendering, silently
                                             deleting the whole `<...>` clause from the output
                                             (`type MergedHook<T = () => void> = T | T[]` lost its
                                             `<T = () => void>` entirely). Fixed by capturing the
                                             clause's token range before the skip loop and rendering
                                             it back (tight against the outer `<`/`>`, which stay
                                             literal OP tokens here) into the `Row`'s name text.
                                             (2) `TokenizerCurly`'s dispatch loop had an unconditional
                                             `c == ']' && peek(1) == ']'` branch (routing to
                                             `emitOperator()`, alongside the already-language-gated
                                             `[[`/`[:` branches for C++11 attributes/C++26 splice
                                             brackets) that fired for *any* two adjacent `]`
                                             characters regardless of language -- not just the C++
                                             attribute close it was meant for. A TS mapped type whose
                                             `[K in ...]` source is itself an indexed-access type
                                             (`{ [K in T[number]]?: unknown }`) produces exactly that
                                             shape with no C++ meaning at all; even after gating
                                             `MULTI_CHAR_OPS`'s own `"]]"` entry to C++ only (a
                                             preceding, insufficient fix attempt), reaching
                                             `emitOperator()` at all here still emitted a single `]`
                                             as an OP token instead of going through
                                             `emitCloseBracket()`'s ordinary PUNCT path, which defeats
                                             every `isPunct(t, "]")` check the same way one merged
                                             `"]]"` token would have. This desynced
                                             `JsTsSpecificRule.enforceSemicolonInsertion`'s `[`/`]`
                                             depth counter, which never recovered for the rest of the
                                             file, permanently disabling semicolon insertion from that
                                             point on. Fixed by also gating the dispatch-loop branch to
                                             `lang.isCpp`. Fixture's expected output still carries the
                                             pre-existing, separately-tracked general-reindentation gap
                                             (STATE_COMMON.md's "General scope-depth reindentation"
                                             section) for the mapped type's own body -- its `}` and
                                             `[K in ...]` bracket spacing are not house-style-correct,
                                             but the round1 output is stable/idempotent, which is all
                                             this regression test asserts.

  real_code_regressions_90_inp/out.ts     -- JS/TS, vuejs/core real-code testing (ref.test-d.ts /
                                             watch.test-d.ts): `JsTsSpecificRule.classifyBraces`'s
                                             `isValue` prev-token list (deciding whether a `{` is a
                                             value-shaped brace, e.g. object literal/type, vs. a
                                             statement-body brace) had no entry for TS's union/
                                             intersection continuation operators `|`/`&`. An inline
                                             object type directly following one of them in a union
                                             type alias (`type Steps = { step: '1' } | { step: '2'
                                             }`) fell through to the method's own documented
                                             "defaults to not a value" fallback, misclassifying the
                                             second object type's `{` as a statement-body brace and
                                             resetting `enforceSemicolonInsertion`'s depth counter to
                                             0 at a point that isn't a real statement boundary --
                                             corrupting every subsequent line's indentation for the
                                             rest of the enclosing scope (not just a semicolon
                                             defect). Fixed by adding `lang.isTs && (isOp(prev, "|")
                                             || isOp(prev, "&"))` to the `isValue` check.

  real_code_regressions_91_inp/out.xsl    -- XML, apache/ant real-code testing: `Lang.infer` never
                                             mapped the `.xsl` extension to `xml` at all (same gap
                                             shape as `real_code_regressions_74`'s `.svg` fix), so
                                             every `.xsl` file in the candidate failed with "could not
                                             infer language from file extension" instead of being
                                             formatted. Fixed by adding `.xsd`/`.xsl` alongside `.xml`/
                                             `.svg` in `Lang.infer`.

  real_code_regressions_92_inp/out.xsd    -- XML, apache/ant real-code testing: same `Lang.infer` gap
                                             as real_code_regressions_91, for the `.xsd` extension.

  real_code_regressions_95_inp/out.java   -- Java, local `src/com`/`src/org` vendored third-party
                                             library dogfood testing: two idempotency bugs from the
                                             same "raw source indent measured before it's converted
                                             to the target indent-style" root cause, both only
                                             observable against tab-indented source (this codebase's
                                             own default `indent-style = spaces` config never
                                             triggers them from spaces-indented input). (1)
                                             `MiscRule.enforceCommentStyle` reindented a multi-line
                                             block comment's continuation lines to the comment's own
                                             *raw*, not-yet-converted leading indent (still a literal
                                             tab), baking a tab into the continuation lines' own text
                                             -- `convertIndentation` (always last in the pipeline)
                                             never revisits that text since by then it is embedded
                                             inside the comment's own token rather than a separate
                                             leading `WHITESPACE` token, so the opening `/**` line
                                             converted to spaces but the continuation lines stayed
                                             tab-indented on a fresh format, self-correcting only on
                                             a second pass once the opening line's own indent was
                                             already spaces. Fixed by normalizing the indent through
                                             `MiscRuleCore.renderIndent` before use (new `indentStyle`
                                             parameter on `enforceCommentStyle`). (2)
                                             `MiscRule.enforceCallLineBreaking`'s whole-line/candidate
                                             fits-checks measured a tab-indented line's leading indent
                                             via `String.length()` (a tab counts as 1 char), so a line
                                             whose true width only exceeds `lineLengthLimit` once its
                                             tab is expanded to `indentWidth` stayed wrongly collapsed
                                             on a fresh format, wrapping only on a second pass once
                                             the leading indent was already spaces. Fixed by a new
                                             `MiscRuleCore.expandedIndentWidth` helper (tab-expanding
                                             width computation, same formula as `renderIndent`'s)
                                             used in place of raw `.length()` at both fits-check
                                             sites. Both found via real-code testing (STATE_C_CPP_
                                             JAVA.md dogfood item (7): `com.j256.simplemagic`,
                                             `org.itadaki.bzip2`, `org.kamranzafar.jtar`).

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
