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
  kt_combined_inp/out.kt                  -- Kotlin STYLE_KOTLIN.md + STYLE_KOTLIN2.md end-to-end
                                             coverage: enum class with members, sealed classes, data
                                             classes, type aliases, generics/variance, where
                                             clauses, infix/extension functions, null-safety
                                             operators, when expressions, property accessors,
                                             destructuring declarations, labeled jumps, and ranges,
                                             all in one realistic file. See STATE_KOTLIN.md Step 4.

  kt_comments_inp/out.kt                  -- Uncommon comment placements in Kotlin, plus
                                             JXM_CFMT_DIS/JXM_CFMT_ENA formatting-toggle markers.
                                             See STATE_KOTLIN.md Step 4.

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

  in_file_config_inp/out.kt               -- Same directive coverage again, plus kotlin-import-order
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
  py_combined_inp/out.py                  -- Bracket-complexity categories, assignment alignment
                                             (augmented assignment, both continuation-break styles),
                                             import ordering/grouping including `__future__`
                                             promotion, decorators, f-strings, function signature
                                             wrapping with type hints, structural pattern matching,
                                             single-statement compound bodies, control-flow blank
                                             lines, `async`/`await`, and a `@property`/`@x.setter`
                                             pair.

  py_comments_inp/out.py                  -- Uncommon `#` comment placement: a comment breaking an
                                             assignment-alignment group, trailing comments not
                                             breaking a comprehension-assignment group, a comment
                                             forcing a signature to wrap, a byte-for-byte-preserved
                                             docstring, a comment between two `case` blocks, and a
                                             comment breaking a compact `case`-line alignment group.

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

  real_code_regressions_17_inp/out.kt     -- Kotlin dogfood find (RobotTcpSession.kt):
                                             enforceCallLineBreaking's per-argument-line grouping
                                             (Option 2) collapsed a multi-line lambda-body sibling
                                             argument onto one line, merging statements with no `;`
                                             separator -- invalid Kotlin. Fixed by bailing (Kotlin-
                                             only) when a top-level argument mixes a newline and
                                             `{`.

  real_code_regressions_18_inp/out.kt     -- Kotlin idempotency (PlayMusicBlock.kt):
                                             KotlinDeclarationAlignmentRule.spansMultipleLines
                                             treated a braceless if/else initializer as multi-line
                                             once a nested call got wrapped by a later pass,
                                             shrinking the sibling val's alignment on each reformat.
                                             Fixed with paren/brace-depth-aware newline tracking so
                                             only real `{`...`}` bodies count as multi-line.

  real_code_regressions_19_inp/out.kt     -- Kotlin indent drift (MainActivity.kt):
                                             ScopePipeline.processScope derived a trailing-lambda
                                             body's indent/closing-brace from the statement's first
                                             line instead of the `{`'s own (deeper) physical line,
                                             under-indenting the body on a fresh format. Fixed via a
                                             new braceLineIndent helper (Kotlin-only).

  real_code_regressions_20_inp/out.kt     -- Kotlin compile-break
                                             (ToolbarActions.kt/MainViewModel.kt):
                                             collapseSingleExpressionBlocks has no expression- vs
                                             statement-position `if` distinction, so it swallowed
                                             the newline after a parenthesized `(if (cond) a else
                                             b)` initializer, fusing it with the next statement.
                                             Fixed by tracking unmatched-paren depth and refusing to
                                             collapse `if`/`else` while inside one.

  real_code_regressions_21_inp/out.kt     -- Kotlin spacing bug (BlockCanvasView.kt): isTightToken's
                                             `&`-repeat check (for C/C++ `&&` rvalue-ref sigils)
                                             also matched Kotlin's `&&` logical-AND, dropping the
                                             space before it. Fixed by gating the check to
                                             non-Kotlin languages, mirroring MiscRule's existing
                                             gate.

  real_code_regressions_22_inp/out.kt     -- Kotlin idempotency (BlockPalette.kt): a one-liner-
                                             function group's column width was computed from
                                             pre-wrap text, but a later pass wrapped a too-long
                                             member's call, leaving stale padding on reformat. Fixed
                                             by porting the C/Java `hasBreakableCall` +
                                             estimated-width pre-check to
                                             KotlinGetterSetterRule.parseKotlinOneLinerMember.

  real_code_regressions_23_inp/out.kt     -- Kotlin idempotency (BlockPalette.kt):
                                             KotlinSpecificRule.isSingleLineBody kept K&R `{` for a
                                             body that was pre-wrap one-line but got split by a
                                             later call-wrapping pass, flipping to Allman on
                                             reformat. Fixed by porting the same hasBreakableCall +
                                             estimated- width pre-check, with a corrected width
                                             formula that now accounts for indentation and spacing.

  real_code_regressions_24_inp/out.kt     -- Kotlin compile-break
                                             (ConnectTypeDialog.kt/WifiApDialog.kt):
                                             findLastTopLevelCloseParen accepted any last depth-0
                                             `)` as a signature's param list even with no `:`
                                             following, so `x.foo().bar { ... }` misdetected `bar`
                                             as a return- type tail and silently deleted `.bar`.
                                             Fixed by requiring a top-level `:` immediately after
                                             the `)` before accepting the Kotlin return-type-tail
                                             branch.

  real_code_regressions_25_inp/out.kt     -- Kotlin compile-break
                                             (BlockCanvasView.kt/ToolbarActions.kt):
                                             enforceLabeledJumpSpacing's label-detection state
                                             machine couldn't tell a genuine `label@` from an
                                             unrelated `@Annotation`, corrupting
                                             `@JvmOverloads`/`@Volatile` into `@ JvmOverloads`/`@
                                             Volatile`. Fixed with an isLoopLabelTarget lookahead
                                             requiring `for`/`while`/`do`/`{` after `@`.

  real_code_regressions_26_inp/out.kt     -- Kotlin compile-break (Optimizer.kt):
                                             collapseSingleExpressionBlocks's bare-`else` handling
                                             also matched a `when` arm's `else ->` label (no brace,
                                             since its body follows `->`), flattening a multi-
                                             statement block onto one line with no `;` separators.
                                             Fixed by checking for a following `->` and bailing out
                                             of the braceless-collapse path.

  real_code_regressions_27_inp/out.kt     -- Kotlin, two co-occurring bugs (ProgramBuilder.kt): (1)
                                             needsSpaceBetween had no case for `!is`/`!in`,
                                             corrupting them into `! is`/`! in`; (2)
                                             enforceCallLineBreaking's renderCallCandidate used the
                                             C/Java-style parseSignature on a call argument
                                             (`it.func.funcName`), misparsing it as `Type name` and
                                             inserting a spurious space once wrapped. Fixed by
                                             adding `!is`/`!in` as tight tokens and routing Kotlin
                                             calls through a separate untyped sigForRender path.

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

  real_code_regressions_30_inp/out.kt     -- Kotlin, real-code test against `square/okio`: three
                                             co-occurring bugs. (1) renderTokens had no unary-vs-
                                             binary `-`/`+` notion, corrupting `val x = -1` into `=
                                             - 1`; fixed with an isUnaryMinusOperand lookback. (2)
                                             applySignaturePass's `: ReturnType` tail detection
                                             merged a headerless declaration with an unrelated later
                                             one across a blank line; fixed with a
                                             hasTopLevelBlankLine guard. (3) braceless if/while/for
                                             collapse rendered a stale, untightened keyword-paren
                                             space, causing enforceCallLineBreaking to over-wrap a
                                             line that fits at its true final width; fixed by
                                             tightening the space at collapse time.

  real_code_regressions_31_inp/out.kt     -- Kotlin, two compile-breaking bugs found via `kotlinc`
                                             against `square/okio` (not caught by idempotency
                                             diffing). (1) MULTI_CHAR_OPS was missing `===`/`!==`,
                                             so `!==` lexed as separate tokens and got re-spaced
                                             into invalid `!= =`; fixed by adding both operators.
                                             (2) the braceless-collapse dispatch treated a
                                             do-while's trailing `while (cond)` as a loop-starting
                                             `while`, fusing the next statement onto the same line;
                                             fixed with an isDoWhileTailKeyword lookback.

  real_code_regressions_32_inp/out.kt     -- Kotlin, real-code test against `square/kotlinpoet`: a
                                             nested `when { ... }` used as a `when` branch's body
                                             flapped its closing `}`'s indentation round1-vs-round2,
                                             since Kotlin's braceLineIndent anchors on the brace's
                                             pre-merge physical line at Phase 0 but
                                             formatWhenExpressions' Phase 4 arrow-alignment pass
                                             later merges the branch label onto that same line.
                                             Fixed with a findMergingWhenBranchLineStart lookahead
                                             that anchors on the eventual post-merge line up front.

  real_code_regressions_33_inp/out.kt     -- Kotlin, real-code test against `square/kotlinpoet`: a
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

  real_code_regressions_37_inp/out.kt     -- Kotlin, real-code test against
                                             `Kotlin/kotlinx.coroutines`: an expression-bodied
                                             function's unconsumed `{`-led lambda tail made
                                             `renderWithTail` bake a trailing space onto `= `
                                             regardless, growing the gap by one space per reformat.
                                             Fixed by omitting the space when `exprTokens` is empty.

  real_code_regressions_38_inp/out.kt     -- Kotlin, real-code test against
                                             `Kotlin/kotlinx.coroutines`: a KDoc's own nested `/*
                                             ... */` snippet closed the outer `/**` doc-comment
                                             early, mis-lexing and silently truncating the rest of
                                             the file (`Guidance.kt`, ~330 lines dropped). Fixed by
                                             tracking block-comment nesting depth, Kotlin-only.

  real_code_regressions_39_inp/out.kt     -- Kotlin, real-code test against
                                             `Kotlin/kotlinx.coroutines`: `this@Label` got a stray
                                             space inserted before `@` (`this @Label`, a real syntax
                                             error) since `enforceLabeledJumpSpacing`'s state
                                             machine didn't recognize `this` before `@`. Fixed with
                                             a new state pair tightening `this@Label`.

  real_code_regressions_40_inp/out.kt     -- Kotlin, real-code test against
                                             `Kotlin/kotlinx.coroutines`: `LimitedDispatcher.kt`'s
                                             collapsible `while (true) { when (...) { ... } }` body
                                             owned a nested multi-line `synchronized(...) { ... }`
                                             block; `tryCollapse`'s brace-depth-unaware flattening
                                             fused its statements onto one line with no separators
                                             -- a real syntax error. Fixed with a
                                             `containsMultilineNestedBrace` bail in
                                             `isKotlinSingleStatementBody`.

  real_code_regressions_41_inp/out.kt     -- Kotlin, real-code idempotency test against
                                             `Kotlin/kotlinx.coroutines`: `SystemProps.kt`'s chained
                                             `catch` span kept its stale pre-merge indent once
                                             `KotlinSignatureRule` merged the `try` signature onto
                                             one line, disagreeing with the `try` span's re-derived
                                             indent round1 vs round2. Fixed by having a chained
                                             `catch`/`finally` span inherit its preceding span's
                                             resolved indent.

  real_code_regressions_42_inp/out.kt     -- Kotlin, real-code idempotency test against
                                             `Kotlin/kotlinx.coroutines`: a class with a wrapped
                                             multi-line generic `where` clause had its closing
                                             brace/comment indent drift deeper, since
                                             `effectiveSpanIndent` preferred the deeper
                                             continuation-line `braceIndent` over the header's own
                                             `spanIndent` (correct for unnamed lambda bodies,
                                             RDD_KEY_136, but wrong for named class/fun/object
                                             scopes). Fixed by gating `braceIndent` off for named
                                             scopes.

  real_code_regressions_43_inp/out.kt     -- Kotlin, real-code idempotency test against
                                             `Kotlin/kotlinx.coroutines`: a wrapped multi-argument
                                             call used as a keyword-less `when` branch body had its
                                             continuation lines one level deeper on round1 than
                                             round2, since `enforceCallLineBreaking` computed the
                                             base indent before the branch label/body got merged
                                             onto one line by a later phase. Fixed with
                                             `effectiveCallBaseIndent`, which uses the preceding
                                             `->` line's indent when present.

  real_code_regressions_44_inp/out.kt     -- Kotlin, real-code idempotency test against
                                             `Kotlin/kotlinx.coroutines`: a nested-lambda-chain's
                                             closing `}` drifted from col 4 to col 8 on round2.
                                             `findParentIndent`'s backward scan could anchor on a
                                             dangling braceless `else expr` (left as leading text at
                                             the start of the next span by `splitTopLevelSpans`),
                                             returning a wrong, unrelated line's indent. Fixed by
                                             skipping forward past a dangling `else`/`catch`/
                                             `finally` anchor to the next real statement.

  real_code_regressions_45_inp/out.kt     -- Kotlin, real-code idempotency test against
                                             `Kotlin/kotlinx.coroutines`: a `val` alignment group
                                             padded a typeless row to match a sibling's type-column
                                             width, widening that sibling's line enough to trigger a
                                             lambda-initializer wrap on the next pass, which then
                                             correctly bailed it out of the group -- an idempotency
                                             flap. Fixed by making `renderAlignedGroup`
                                             budget-aware: a row is excluded from the shared column
                                             grid up front when its own brace-bodied initializer
                                             would overflow the line-length budget once padded.

  real_code_regressions_46_inp/out.kt     -- Kotlin, real-code idempotency test against
                                             `square/kotlinpoet`'s Shape 1 idempotency-gap group (6
                                             files), two bugs in `enforceCallLineBreaking`. Bug A: a
                                             wrapped signature with a trailing `= apply { ... }`
                                             body re-collapsed on reformat because `lineEndIndex`'s
                                             width check stopped at the first NEWLINE, undercounting
                                             width when the tail's own nested call was already
                                             wrapped from a previous round; fixed with a depth-aware
                                             `effectiveLineEndIndex` that skips NEWLINEs still
                                             inside an unclosed bracket. Bug B (RDD_KEY_149, now
                                             root-caused): a signature with an explicit `:
                                             ReturnType {` block body got its correctly wrapped,
                                             padded param list re-wrapped as a plain call,
                                             discarding padding/trailing comma, since the "is this a
                                             call" exemption only recognized `{` right after `)`.
                                             Fixed with an `isKotlinReturnTypeThenBlockBody`
                                             lookahead.

  real_code_regressions_47_inp/out.kt     -- Kotlin, real-code idempotency test against
                                             `square/kotlinpoet`'s Shape 2 (`AbstractTypesTest.kt`):
                                             a multi-line generic `where` clause gained one extra
                                             indent level every round, since
                                             `enforceWhereClausePlacement` derived the base indent
                                             from `where`'s own (already-wrapped) physical line
                                             instead of the true signature line. Fixed with a
                                             `signatureLineIndent` helper that scans backward to the
                                             nearest depth-0 `;`/`}`/`{`.

  real_code_regressions_48_inp/out.kt     -- Kotlin, real-code idempotency test against
                                             `square/kotlinpoet`'s Shape 3: a `when` branch's
                                             multi-line body (nested `when(subject) { ... }` or a
                                             trailing-lambda call) had its closing `}` sit 2 spaces
                                             shallower on round2, since
                                             `findMergingWhenBranchLineStart` (RDD_KEY_152) only
                                             recognized a bare `when {` as the merging shape. Fixed
                                             by generalizing the lookahead to accept a parenthesized
                                             `when` subject or a plain call-head identifier.

  real_code_regressions_49_inp/out.kt     -- Kotlin, real-code idempotency test against
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

  real_code_regressions_59_inp/out.kt     -- Kotlin, arrow-kt/arrow real-code testing: a generic
                                             bound's `:` (e.g. `<A : Comparable<A>>`) wasn't
                                             recognized as generic-safe by
                                             `TokenizerCore.isGenericSafeToken`, invalidating the
                                             angle-bracket tracking stack so a second bound's `>>`
                                             stayed unsplit, corrupting both bounds' spacing. Fixed
                                             by adding a Kotlin-gated `:` case to
                                             `isGenericSafeToken`'s `OP` branch. Found in
                                             `arrow-core`'s `Pair.kt` (`compareTo` extension).
                                             Verified: minimal repro, full `make test`.

  real_code_regressions_60_inp/out.kt     -- Kotlin, arrow-kt/arrow real-code testing (found via
                                             `kotlin_syntax_check` compile-checking round1's output, not
                                             round1/round2 diffing): `BlockStructureRule
                                             .isKotlinSingleStatementBody` let a braced `if` body
                                             whose sole statement was a `val`/`var` declaration
                                             collapse to braceless form (`if (x) val y = ...`),
                                             which is illegal Kotlin. Fixed by disqualifying a body
                                             whose first token is `val`/`var` from collapse, same as
                                             `COMPOUND_BODY_KEYWORDS` does for nested compound
                                             bodies. Found in `RaiseAccumulate.kt`'s `addErrors`.
                                             Verified: minimal repro, full `make test`.

  real_code_regressions_61_inp/out.kt     -- Kotlin, arrow-kt/arrow real-code testing (also found
                                             via `kotlin_syntax_check`): `MiscRule.needsSpaceBetween` had no
                                             tight-after case for a Kotlin annotation's `@` when it
                                             shares its source line with the function signature
                                             (rendered through `MiscRule.renderTokens`'s shared join
                                             point, used by `KotlinSignatureRule`); the default
                                             space-insert fallback produced invalid `@ RaiseDSL`.
                                             Fixed by adding a Kotlin-gated tight-after case for
                                             `@`. Kotlin's other `@`-uses (`return@label`, `label@`,
                                             `this@Label`) go through a separate rule
                                             (`KotlinSpecificRule .enforceLabeledJumpSpacing`) and
                                             are unaffected. Found in `RaiseAccumulateContext.kt`'s
                                             `mapOrAccumulate`. Verified: minimal repro, full `make
                                             test`.

  real_code_regressions_62_inp/out.kt     -- Kotlin, arrow-kt/arrow real-code testing: two
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

  real_code_regressions_63_inp/out.kt     -- Kotlin, arrow-kt/arrow real-code testing: RDD_KEY_176
                                             -- `BlockStructureRule.collapseBracelessBody`'s
                                             bare-`else`/ braceless-`if` body scan never checked
                                             whether the body was a single statement once it could
                                             itself own a multi-line `{...}` block (e.g. a
                                             trailing-lambda call); `renderInline` fused the block's
                                             internal statements with no `;` separator, a genuine
                                             compile error. Fixed by reusing
                                             `containsMultilineNestedBrace` as a bail-out guard.
                                             Found in `Either.kt`'s `zipOrAccumulate`. Verified:
                                             `kotlin_syntax_check` on `Either.kt` (18 errors -> 0), full `make
                                             test`.

  real_code_regressions_64_inp/out.kt     -- Kotlin, arrow-kt/arrow real-code testing: RDD_KEY_177,
                                             closing item of the investigation. Pure idempotency
                                             flap in `Comparison.kt`'s `sort2`:
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
                                             fixture 78): two more idempotency bugs. (1) §6
                                             signature alignment: `trySignatureGroup` split params
                                             on raw NEWLINE tokens without checking bracket depth,
                                             misclassifying a multi-line type-hint's continuation
                                             lines as bogus params and corrupting the signature with
                                             growing trailing whitespace each round; fixed by only
                                             splitting at depth-0 NEWLINEs in
                                             `classifySignatureParam`. (2) §9.2
                                             blank-line-before-`elif`/`else` and §8's statement-join
                                             could target the same token index, letting the join
                                             swallow the blank-line insertion; fixed by sorting
                                             zero-width entries first on ties.

  real_code_regressions_80_inp/out.py     -- Python3, pallets/click real-code testing: §4 decorator
                                             bracket-padding (`applyBracketPadding`) couldn't
                                             distinguish an f-string field's `{`/`}` from a dict/set
                                             literal, padding it like a non-empty brace pair and
                                             producing `f"{ ctx.info_name }"`; §5's f-string spacing
                                             pass then trimmed it back next round, so it only
                                             surfaced as non-idempotency. Fixed by skipping any
                                             `{`/`}` immediately preceded by
                                             `FSTRING_START`/`FSTRING_MIDDLE`.

  real_code_regressions_81_inp/out.ts     -- JS/TS, nestjs/nest real-code testing: a multi-arg call
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

  real_code_regressions_82_inp/out.ts     -- JS/TS, nestjs/nest real-code testing: content
                                             duplication.
                                             `JsTsSpecificRule.enforceClassFieldAlignmentGrid`'s
                                             linear `cursor` sweep assumed every selected class span
                                             was disjoint, but an anonymous `return class extends
                                             Base {...}` nested inside an outer class's method is
                                             legitimate nesting; re-processing the inner span as its
                                             own top-level entry duplicated content and walked
                                             `cursor` backward, causing the final raw-copy loop to
                                             re-emit everything to EOF a second time. Fixed by
                                             filtering `classOpens` to only the outermost class
                                             brace at each nesting level.

  real_code_regressions_83_inp/out.yaml   -- YAML, prometheus/prometheus real-code testing: four
                                             combined data-loss bugs in `YamlSpecificRule`, all a
                                             dash/key line whose "value" is
                                             absent/comment-only/anchor-only/an unbalanced
                                             multi-line flow opener with real content on
                                             more-indented following lines. (1) `parseKeyItem`'s
                                             flow-value early return didn't check the flow closed on
                                             the same line, truncating text after an unbalanced
                                             `[...]`. (2) `parseSeqItem`'s `seqOfMapping` first-key
                                             handling had the same gap, and also dropped everything
                                             after a comment-only dash line. (3) An anchor-only dash
                                             line (`- &highalert`) followed by a nested mapping at
                                             an equal (not just greater) indent lost its child
                                             block. (4) `renderFlowValue` rendered an empty
                                             `{}`/`[]` as a block conversion whenever the line
                                             didn't fit, but `renderFlowBlock` has nothing to
                                             iterate for zero entries, silently dropping the value.

  real_code_regressions_84_inp/out.ts     -- JS/TS, nestjs/nest real-code testing:
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

  real_code_regressions_85_inp/out.ts     -- JS/TS, nestjs/nest real-code testing: `join(...)`
                                             call-wrap/collapse non-idempotency.
                                             `MiscRuleCurly.renderCallCandidate`'s multi-line-source
                                             branch always preserved the original per-line argument
                                             grouping with no fits-check, unlike the sibling
                                             single-line branch -- a call wrapped across lines
                                             stayed wrapped forever even once it fit on one line,
                                             while the same call written fresh on one line collapsed
                                             correctly, so the same call could settle into two
                                             different stable shapes. Fixed by adding the same
                                             fits-check (JS/TS-only, to avoid regressing fixture 1's
                                             C/C++/Java case), measuring the tight single-line
                                             candidate rather than the loose `collapseToOneLine`
                                             helper (which overestimates length). Also updated
                                             fixture 81's expected output, whose old shape was
                                             itself an artifact of this bug.

  real_code_regressions_86_inp/out.yaml   -- YAML, home-assistant/core real-code testing:
                                             nested-sequence data loss. `parseSeqItem` never
                                             recognized the compact single-line nested-seq form `- -
                                             a\n  - b`; the inner `- ` was captured as a literal
                                             scalar, leaving the sibling nested item unconsumed,
                                             whose mismatched indent then made `parseBlock` break
                                             out of the entire enclosing block early -- silently
                                             dropping the rest of the sequence and every sibling
                                             item/key that followed, at every level. Fixed by
                                             detecting the `-`/`- ` shape up front via a new
                                             `parseInlineNestedSeq` helper and rendering non-lossily
                                             via the existing `item.children` path. Found via
                                             content-preservation checking, not syntax-check.

  real_code_regressions_87_inp/out.ts     -- JS/TS, vuejs/core real-code testing: leading multi-line
                                             block comment reindent non-idempotency.
                                             `JsTsSpecificRule`'s class-field alignment grid,
                                             enum-member formatting, and interface/type-alias member
                                             alignment all re-emitted a member's leading `/** ...
                                             */` comment verbatim at its original source indent,
                                             never reindented to the member's own re-rendered depth
                                             -- misaligned on the first pass, self-corrected by an
                                             unrelated general reindent pass on the second,
                                             producing round1 != round2. Fixed by adding
                                             `reindentLeadingComment` at all three sites.

  real_code_regressions_88_inp/out.ts     -- JS/TS, vuejs/core real-code testing:
                                             `TokenizerCurly.GENERIC_SAFE_KEYWORDS` was missing TS's
                                             `symbol`/`bigint`, and `isGenericSafeToken`'s OP case
                                             had no `|` entry -- a union type inside a generic
                                             argument list (`Record<string | symbol, Function |
                                             number>`) invalidated the enclosing `<...>` tracking,
                                             leaving `>` a plain OP token. That defeated
                                             `enforceSemicolonInsertion`'s continuation check
                                             (dropping the statement's `;`) and desynced
                                             `JsTsDeclarationAlignmentRule.parseTypeAlias`'s
                                             depth-scan, corrupting unrelated following statements
                                             with bogus alignment padding. Fixed by adding
                                             `symbol`/`bigint` and `|` (TS-only).

  real_code_regressions_89_inp/out.ts     -- JS/TS, vuejs/core real-code testing
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
                                             `enforceSemicolonInsertion`'s bracket-depth counter for
                                             the rest of the file. Fixed by gating the branch to
                                             `lang.isCpp`. Output still carries the known
                                             general-reindentation gap for the mapped type's own
                                             body.

  real_code_regressions_90_inp/out.ts     -- JS/TS, vuejs/core real-code testing
                                             (ref.test-d.ts/watch.test-d.ts):
                                             `JsTsSpecificRule.classifyBraces`'s `isValue`
                                             prev-token list had no entry for the union/intersection
                                             operators `|`/`&` -- an inline object type following
                                             one in a union alias fell through to "not a value",
                                             misclassifying its `{` as a statement-body brace and
                                             resetting `enforceSemicolonInsertion`'s depth counter
                                             mid-expression, corrupting every subsequent line's
                                             indentation for the rest of the scope. Fixed by adding
                                             `lang.isTs && (isOp(prev, "|") || isOp(prev, "&"))` to
                                             the check.

  real_code_regressions_91_inp/out.xsl    -- XML, apache/ant real-code testing: `Lang.infer` never
                                             mapped `.xsl` to `xml` (same gap shape as fixture 74's
                                             `.svg`), so every `.xsl` file failed with "could not
                                             infer language from file extension". Fixed by adding
                                             `.xsd`/`.xsl` alongside `.xml`/`.svg`.

  real_code_regressions_92_inp/out.xsd    -- XML, apache/ant real-code testing: same `Lang.infer`
                                             gap as fixture 91, for `.xsd`.

  real_code_regressions_93_inp/out.ts     -- JS/TS, vuejs/core real-code testing: two
                                             non-idempotency bugs from
                                             `enforceCallLineBreaking`/`enforceComplexityPadding`
                                             pass-ordering. (1) `MiscRuleCurly.collapseToOneLine`'s
                                             fits-check flattened every whitespace run to one space,
                                             including same-line declaration-alignment padding,
                                             undercounting a padded declaration's true width and
                                             wrongly collapsing it; fixed by only collapsing runs
                                             that actually contain a NEWLINE. (2) `FormatterCurly`'s
                                             final `enforceCallLineBreaking` pass had no
                                             `enforceComplexityPadding` re-run after it, so a call
                                             wrapped by an earlier inflated fits-check and later
                                             re-collapsed lost its loose `( x )` nested-bracket
                                             padding; fixed by adding one more
                                             `enforceComplexityPadding` call after the final
                                             line-breaking pass.

  real_code_regressions_94_inp/out.js     -- JS/TS, vuejs/core real-code testing:
                                             `BlockStructureRule.alignBracelessElseIfChain` runs
                                             last, after every `enforceCallLineBreaking` fits-check,
                                             so its own column padding of a braceless if/else chain
                                             could push an already-fits-checked consequent past the
                                             line limit with no re-check (widespread across the
                                             corpus). Fixed by refusing to pad a branch past the
                                             line limit when its un-padded width already fit,
                                             leaving it at natural width instead; an
                                             already-over-limit branch is still padded as before.
                                             Required adding a `lineLengthLimit` parameter to
                                             `BlockStructureRule`.

  real_code_regressions_95_inp/out.java   -- Java, local vendored third-party library dogfood
                                             testing: two idempotency bugs sharing one root cause --
                                             raw source indent measured before conversion to the
                                             target indent-style -- only observable against
                                             tab-indented source. (1) `MiscRule.enforceCommentStyle`
                                             reindented a block comment's continuation lines to the
                                             comment's raw (still-tab) leading indent, baking a tab
                                             into text that `convertIndentation` never revisits
                                             since it's now inside the comment token; self-corrected
                                             only on a second pass. Fixed by normalizing through
                                             `MiscRuleCore.renderIndent` first. (2)
                                             `MiscRule.enforceCallLineBreaking`'s fits-checks
                                             measured a tab-indented line's leading indent via
                                             `String.length()` (tab = 1 char), wrongly
                                             under-measuring width and leaving an over-limit line
                                             collapsed until a second pass. Fixed by a new
                                             `MiscRuleCore.expandedIndentWidth` helper used at both
                                             fits-check sites.

  real_code_regressions_96_inp/out.ts     -- JS/TS, vuejs/core real-code testing:
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

  real_code_regressions_97_inp/out.ts     -- JS/TS, vuejs/core real-code testing:
                                             `JsTsSpecificRule.enforceSemicolonInsertion`'s
                                             depth-tracking loop counted only `(`/`[`/`{` against
                                             `)`/`]`/`}`, with no case for a generic clause's
                                             `ANGLE_BRACKET_OPEN`/`_CLOSE` -- a multi-line generic
                                             clause (`function mergeProps<\n  T,\n  U\n>(...)`) left
                                             depth at 0 across its own NEWLINEs, wrongly inserting
                                             spurious `;` inside `<...>`. Fixed by adding the
                                             angle-bracket cases to the depth-tracking loop.

  real_code_regressions_98_inp/out.ts     -- JS/TS, vuejs/core real-code testing (found via the
                                             `tsc` typecheck pass):
                                             `JsTsSpecificRule.classifyBraces` had no case for an
                                             `export { ... }` brace header (only `import`), so a
                                             single-specifier one-liner fell through to "statement
                                             body" classification and got a bogus `;` inserted
                                             before its own closing `}` -- a real parse error. Fixed
                                             by adding an `isExportBraceHeader` case mirroring the
                                             import one, with `needsSemicolon` computed per-brace
                                             (false when followed by `from '...'`, true for a plain
                                             named export with no `from` clause).

  real_code_regressions_99_inp/out.ts     -- JS/TS, vuejs/core real-code testing (found via the same
                                             `tsc` pass as fixture 98):
                                             `TokenizerCurly.isGenericSafeToken`'s OP case
                                             recognized `:` as generic-safe for Kotlin only -- a TS
                                             conditional type inside a generic argument list
                                             (`Readonly<A extends B ? C : D>`) hit its own `:` and
                                             invalidated the enclosing `<...>` tracking, defeating
                                             fixture 97's depth tracking and wrongly inserting a `;`
                                             before the clause's closing `>`. Fixed by adding
                                             `lang.isTs && ":".equals(t.text)` alongside the
                                             Kotlin-only case.

  real_code_regressions_100_inp/out.ts    -- JS/TS, vuejs/core real-code testing (found via the same
                                             `tsc` pass): `enforceSemicolonInsertion`'s
                                             NEWLINE-boundary continuation checks had no case for a
                                             class/interface header wrapping its own
                                             `extends`/`implements` clause onto its own line -- the
                                             declaration name's trailing NEWLINE was wrongly treated
                                             as a statement boundary, splitting the header with a
                                             bogus `;`. Fixed by adding a lookahead: a next-line
                                             leading `extends`/`implements` keyword also means the
                                             statement isn't finished, same as the existing
                                             `{`/`|`/`&`/`,` cases.

  real_code_regressions_101_inp/out.ts    -- JS/TS, vuejs/core real-code testing (found via the same
                                             `tsc` pass): `enforceArrowFunctionParameterParens`
                                             wrapped any bare identifier before `=>` in parens with
                                             no check for a TS return-type annotation ending in a
                                             type predicate or bare type name (`(node: Node): node
                                             is Function => {...}`), wrapping the return type's tail
                                             as if it were a parameter -- a real TS parse error.
                                             Fixed by checking the token before the candidate
                                             identifier: a preceding `:` or `is` means it's a return
                                             type, left unwrapped.

  real_code_regressions_102_inp/out.ts    -- JS/TS, vuejs/core real-code testing (found via the same
                                             `tsc` pass): `GENERIC_SAFE_KEYWORDS` was missing TS's
                                             `true`/`false` boolean-literal-type keywords -- a
                                             boolean type argument inside a multi-line generic
                                             clause invalidated `reclassifyAngleBrackets`'s
                                             open-stack tracking, defeating
                                             `enforceSemicolonInsertion`'s depth tracking and
                                             inserting a bogus `;` before the closing `>`. Fixed by
                                             adding `true`/`false` to `GENERIC_SAFE_KEYWORDS`. A
                                             related fix landed in the same investigation: the
                                             existing "any `{`/`}`/`;` clears the whole open stack"
                                             rule was also firing on a legitimate nested object-type
                                             argument inside an already-tracked generic clause;
                                             fixed with a `nestedBraceDepth` counter that skips the
                                             clear-all for balanced `{...}` while the open stack is
                                             non-empty.

  real_code_regressions_103_inp/out.html  -- HTML5, WordPress/wordpress-develop real-code testing:
                                             `renderElement`'s multi-child block-closing render path
                                             never emitted `n.trailingComment`, unlike the other
                                             three render branches -- a same-line trailing comment
                                             right after a block element's closing tag was silently
                                             dropped whenever that element had element children (not
                                             just a lone text node), real data loss. Fixed by
                                             routing the multi-child closing-tag line through
                                             `appendWithTrailing` too.

  real_code_regressions_104_inp/out.html  -- HTML5, alexandersandberg/html5-elements-tester
                                             real-code testing (RDD_KEY_198): `<ruby>` uses HTML5's
                                             optional/implied-end-tag rule -- its
                                             `<rb>`/`<rt>`/`<rp>`/`<rtc>` children never carry an
                                             explicit closing tag -- and `parseElement` had no
                                             notion of this, throwing `XmlParseException`. Fixed by
                                             adding an extensible `OPAQUE_IMPLIED_END_TAG_ELEMENTS`
                                             set (currently just `ruby`) that scans the whole
                                             element as one verbatim opaque span, reusing the
                                             existing `<script>`/`<style>`/`<pre>` pattern.

  real_code_regressions_105_inp/out.ts    -- JS/TS, vuejs/core real-code testing (final batch from
                                             the full 514-file dogfood tsc pass, consolidated into
                                             one fixture): six independent bugs. (1)
                                             `reclassifyAngleBrackets`'s `nestedBraceDepth` guard
                                             (added for fixture 102) only covered the nested `{`/`}`
                                             delimiters, not tokens inside them, still letting an
                                             interior keyword/`;` wipe the outer `<...>` tracking;
                                             fixed by extending both checks to skip while
                                             `nestedBraceDepth > 0`. (2) A mapped-type object as a
                                             generic type argument needed `ANGLE_BRACKET_OPEN` added
                                             to `classifyBraces`'s `isValue` whitelist. (3) A
                                             ternary nested inside a parenthesized grouping
                                             expression had its `:` misclassified as a return-type
                                             colon; fixed with a new `isGroupingExpressionParen`
                                             helper. (4) `key is keyof typeof val => ...` wrongly
                                             wrapped `val` in parens because the arrow-param
                                             bail-out only recognized `is`, not `typeof`/`keyof`.
                                             (5) A trailing type-annotation `:` wrapping to the next
                                             line got a bogus `;` because `needsSemicolonAfter`'s
                                             `isPunct(t, ":")` guard never matches (`:` tokenizes as
                                             OP); fixed by adding `":"` to `CONTINUATION_OPS`
                                             instead. (6) A standalone TS function-type parameter
                                             list got padded like an arbitrary grouping paren; fixed
                                             with a `lang.isTs`-gated exception when the matching
                                             `)` is followed by `=>`. Also added `=>`/`...` to
                                             `isGenericSafeToken`'s TS-safe OP list (`...` gated to
                                             TS only, to avoid regressing C++ variadic-template
                                             spacing in fixture 53).

  real_code_regressions_106_inp/out.html  -- HTML5, unquoted attribute values (RDD_KEY_199). Found
                                             via the same `alexandersandberg/html5-elements-tester`
                                             dogfood spot-check as fixture 104:
                                             `XmlSpecificRule.parseAttr` required a quoted value and
                                             threw on `<select ... size=5>`, even though unquoted
                                             values are valid per the HTML5 spec grammar. Fixed by
                                             accepting an unquoted value on the `lang.isHtml5`
                                             branch only (plain XML still requires quotes) and
                                             preserving it unquoted on output, consistent with this
                                             codebase's "preserve as written" posture elsewhere
                                             (JSON5/TOML quote style). Fixture isolates a `<select
                                             size=5>`/unquoted `<option value=...>` block plus an
                                             `<input>` mixing unquoted values and a bare boolean
                                             attribute.

  real_code_regressions_107_inp/out.ts    -- JS/TS, vuejs/core real-code testing (found on the final
                                             full-corpus tsc rerun, after fixture 105 landed):
                                             `typeof` was missing from `GENERIC_SAFE_KEYWORDS` -- a
                                             `typeof` type-query operand inside a generic argument
                                             list (`Record<(typeof identityMethods)[number], any>`,
                                             `ReturnType<typeof createServer>`) invalidated the
                                             `<...>` open-stack tracking, same class of bug as the
                                             `keyof`/`is`/`infer` gap fixed for fixture 101. In the
                                             multi-line case this produced a bogus `;` before the
                                             closing `>`; in the single-line case, losing the
                                             tracking left `>` a plain OP token, defeating
                                             statement-boundary detection entirely and merging the
                                             following statement onto the same line. Fixed by adding
                                             `typeof` to `GENERIC_SAFE_KEYWORDS`.

  real_code_regressions_108_inp/out.html  -- HTML5, `<option>` implied-closing-trigger support
                                             (RDD_KEY_200). Covers an explicitly-closed `<option>`
                                             (regression guard on the pre-existing common case) plus
                                             a `<datalist>` with bare `<option value="...">` tags
                                             relying on HTML5's implied-end-tag rule (closed by a
                                             sibling `<option>`/`<optgroup>` start, or by the
                                             parent's own closing tag), the same shape that blocked
                                             the `alexandersandberg/html5-elements-tester` dogfood
                                             run.

  real_code_regressions_109_inp/out.html  -- HTML5, `web-platform-tests/wpt` dogfood: four bugs --
                                             (1) `<head>` added to `IMPLIED_CLOSE_TRIGGERS` (closes on
                                             sibling `<body>` start); (2) bare `<image>` rewritten to
                                             `<img>` outside real SVG foreign content only (`svgDepth`
                                             counter); (3) EOF now implicitly closes any still-open
                                             element instead of throwing (HTML5 "stopped parsing" step);
                                             (4) `<xmp>` recognized as a raw-text element like `<pre>`.

  real_code_regressions_110_inp/out.html  -- HTML5, follow-up hardening after user review of _109:
                                             (1) the hardcoded `image` check generalized into a
                                             `TAG_NAME_REWRITES` map; (2) a mismatched/orphaned closing
                                             tag with no corresponding open element no longer crashes
                                             even at document root (tolerant-close fallback broadened,
                                             plus a new top-level `parseNodes(stopAtCloseTag=false)`
                                             fallback) -- discarded and parsing continues instead of
                                             throwing.

  real_code_regressions_111_inp/out.html  -- HTML5, follow-up hardening (user request): raw-text
                                             elements (`<script>`/`<style>`/`<pre>`/`<xmp>`) whose
                                             literal closing tag never appears at all used to crash in
                                             `finishRawElement`/`finishRawTextElement` on real EOF; both
                                             now capture verbatim through EOF instead, same tolerance
                                             principle as _109/_110. Last crash site from the "HTML5
                                             deep tree-construction edge cases" Open Question in
                                             `STATE_DATA_FORMATS.md`.

  real_code_regressions_112_inp/out.html  -- HTML5, standalone follow-up (user request, 2026-07-25):
                                             SVG tag-name case-folding, split out of the tree-
                                             construction Open Question as its own lookup-table fix.
                                             New `XmlSpecificRule.SVG_TAG_NAME_CASE_FIXUP` map (spec's
                                             "Adjust SVG tag names" table, e.g. `lineargradient` ->
                                             `linearGradient`), gated `svgDepth > 0` (opposite of
                                             `TAG_NAME_REWRITES`'s `== 0`). Fixture proves both the
                                             SVG-nested rewrite and the same tag name left untouched as
                                             plain HTML. Surfaced a latent closing-tag bug: once
                                             `tagName` is case-rewritten, the literal-case `closeTok`
                                             no longer matched the source's original-case closing tag;
                                             fixed via new case-insensitive `startsWithCloseTagIgnoreCase`
                                             (HTML5-only). MathML's `definitionurl` -> `definitionURL`
                                             attribute-only fixup intentionally left open -- no MathML-
                                             depth tracking exists yet; see `STATE_DATA_FORMATS.md`.

  real_code_regressions_113_inp/out.java  -- Java, jenkinsci/jenkins real-code dogfood: 2 bugs fixed.
                                             (a) `JavaSpecificRule.findArrowCases`'s brace-depth-0 scan
                                             never skipped past a case's own found arrow, so multi-value
                                             labels like `case null, default ->` got re-matched and
                                             duplicated worse each pass; fixed by advancing the scan
                                             index past the found arrow. (b) `MiscRuleCore
                                             .needsSpaceBetween` only special-cased Kotlin's `@` as
                                             tight against the next identifier, so Java annotations
                                             rendered `@ NonNull`; extended to `lang.isJava`. A third
                                             bug (`alignCommentSeparators` false-positiving on ordinary
                                             prose) was NOT fixed -- re-opens the user-resolved
                                             RDD_KEY_50 design decision rather than being a plain
                                             implementation bug; see `STATE_C_CPP_JAVA.md`'s
                                             jenkinsci/jenkins dogfood entry (accepted as a permanent
                                             known limitation, also noted in `README.md`).

  real_code_regressions_114_inp/out.py    -- Python3, psf/black real-code dogfood: crash fix.
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
                                             gone, not a rendering change). See `STATE_PYTHON3.md`'s
                                             `psf/black` dogfood entry.

  real_code_regressions_115_inp/out.py    -- Python3, psf/black real-code dogfood: §7/§8
                                             join-then-align ordering non-idempotency fix. A
                                             block-form `match`/`case` group is correctly skipped
                                             by §7's colon alignment on the forward pass (not yet
                                             compact); §8 then joins each case's single-statement
                                             body onto its header line later in the same pass. A
                                             second pass previously saw the now-compact `case`
                                             lines for the first time and applied colon-column
                                             padding never present in the first pass's output.
                                             Fixed in `ScopePipelineIndent`: §7's `classifyCaseLine`
                                             now predicts (new `tryQualifyJoinBody`, shared with
                                             `applySingleStatementBody`) whether a block-form case
                                             will qualify for §8's join, treating it as effectively
                                             compact for grouping/alignment so `flushCaseGroup`
                                             bakes correct padding in immediately;
                                             `applySingleStatementBody` skips any header §7 already
                                             joined (`caseJoinAlignedHeaders`) to avoid a duplicate
                                             join. See `STATE_PYTHON3.md`'s `psf/black` dogfood entry.

  real_code_regressions_116_inp/out.py    -- Python3, psf/black real-code dogfood: §6 multi-
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
                                             `classifySignatureParam` now rejects (returns null) any
                                             segment whose first token isn't a valid parameter start
                                             (identifier, or `*`/`**`/`/`) -- a leading `|` means
                                             it's a continuation, not a parameter. Identity-pass
                                             fixture (converges to a true no-op instead of growing).
                                             See `STATE_PYTHON3.md`'s `psf/black` dogfood entry.

  real_code_regressions_117_inp/out.py    -- Python3, psf/black real-code dogfood: two §5
                                             `addBraceTrim` content-corruption fixes, combined
                                             (both live in the same method). (a) A field
                                             immediately followed by a nested `{` (e.g.
                                             `f"{ {a for a in (1, 2, 3)}}"`) had its open-gap trim
                                             collapse the field's `{` and the nested `{` into a
                                             literal `{{`, which Python's f-string grammar parses
                                             as an ESCAPED brace rather than two field-opens --
                                             silently deleting the whole comprehension expression
                                             (confirmed via `ast.dump`: the `FormattedValue` node
                                             vanished). Fixed: `addBraceTrim` normalizes the open
                                             gap to one space (not zero) whenever the next
                                             significant token is itself a literal `{`. (b) A
                                             self-documenting `{expr=}` debug field (e.g.
                                             `f'{  longer_name   =  :  .3f }'`) had its leading gap
                                             trimmed even though Python's runtime must reproduce
                                             `expr`'s exact original whitespace verbatim for a
                                             `=`-suffixed field -- a real behavior change, not
                                             cosmetic. Fixed: `addBraceTrim` now detects a bare
                                             trailing `=` (a lone 1-char OP token; all
                                             comparison/augmented-assignment/walrus operators
                                             tokenize as distinct multi-char OPs, so no risk of
                                             confusion) as the expression's last significant token
                                             and skips gap-trimming entirely for that field. Both
                                             verified via `python_content_diff.py` (structurally
                                             identical) and idempotency; identity-pass fixture.
                                             See `STATE_PYTHON3.md`'s `psf/black` dogfood entry.

  real_code_regressions_118_inp/out.hpp   -- C++, microsoft/STL real-code dogfood: `Main.
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
                                             tree: fixed 99 of 110 idempotency-diffing files in that
                                             candidate. See `STATE_C_CPP_JAVA.md`.

  real_code_regressions_119_inp/out.hpp   -- C++, microsoft/STL real-code dogfood: two duplicated
                                             `collapseToOneLine`/`flushCollapseGap` implementations
                                             (`MiscRuleCurly.java`, `CppSpecificRule.java`) joined a
                                             multi-line run back onto one line by unconditionally
                                             inserting a space wherever the original had a newline,
                                             with no tight-join awareness -- so a wrapped
                                             member-access/`->` expression broken right at the
                                             `.`/`->` (e.g. a constructor's member-initializer-list
                                             argument, `other.\n    _Outer`) came back corrupted as
                                             `other. _Outer` once re-collapsed. Sibling
                                             `collapseTokensToOneLine` already had this guard for
                                             JS/TS's `.`/`?.` (an earlier nestjs/nest fix) but it
                                             was never mirrored here. Fixed: both now track the
                                             previous/next significant token around each
                                             whitespace/newline run and suppress the forced space
                                             when either side is `.`/`->`. Verified against the real
                                             microsoft/STL tree (`ranges.hpp`'s wrapped
                                             constructor-initializer-list arguments). A related,
                                             deeper bug in the same area (a long constructor
                                             signature's parameter-wrap logic misapplied to its
                                             following member-initializer-list entry) was found and
                                             later fixed separately -- see fixture 121.

  real_code_regressions_120_inp/out.hpp   -- C++, microsoft/STL real-code dogfood: a bare
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
                                             never fired, misparsing the run as a bogus
                                             `Type name = init;` declarator. Fixed by widening the
                                             guard to a depth-tracked scan of the whole merged
                                             statement for a top-level `if`/`while`/`for`/`switch`/
                                             `do`/`else` keyword. Verified against the real
                                             microsoft/STL tree (`istream.hpp`/`stacktrace.hpp`/
                                             `xlocale.hpp`, all idempotent after the fix). See
                                             `STATE_C_CPP_JAVA.md`.

  real_code_regressions_121_inp/out.hpp   -- C++, microsoft/STL real-code dogfood: a wrapped
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
                                             `parseParam` mis-slices the single argument as a
                                             `Type name` declarator pair (last token is a bare
                                             IDENTIFIER) -- `sigForRender` then routes it through the
                                             declaration-style column-split renderer instead of the
                                             tight-join-`.`/`->`-aware plain-call renderer,
                                             inserting a space after the `.`. Fixed: `parseParam`
                                             now rejects (returns null) any param whose parsed
                                             `typeTokens` run ends in a `.`/`->` tight-join operator
                                             (never a real C++ type), falling back to plain-call
                                             rendering. Verified against the real microsoft/STL tree
                                             (`mutex.hpp`/`shared_mutex.hpp`, idempotent and
                                             corruption-free after the fix). See `STATE_C_CPP_JAVA.md`.

  real_code_regressions_122_inp/out.hpp   -- C++, microsoft/STL real-code dogfood: a
                                             declaration-alignment group whose first member has a
                                             same-line leading comment (STL's own
                                             `/* [[no_unique_address]] */ _Vw _Range;` followed by
                                             un-commented siblings in the same group) got that
                                             comment silently duplicated onto every sibling line one
                                             round later, with the group's column-padding width also
                                             changing between rounds -- root cause of the
                                             previously-open "declaration-alignment column-padding
                                             non-idempotency" gap (`ranges.hpp`'s `_Range` field,
                                             `algorithm.hpp`, `filesystem.hpp`). Root cause:
                                             `ScopePipelineCore.trailingIndent` returns the text
                                             after a leading gap's last `\n` as the line's
                                             indentation with no check that it's pure whitespace --
                                             when the gap ends in a same-line leading comment before
                                             the first declaration, that text got swept into
                                             `indent`, which `applyDeclarationsPass`/
                                             `applyAssignmentsPass`/
                                             `applyOversizedAggregateInitClosingBracePass` all use
                                             as the per-line join separator, duplicating the comment
                                             onto every sibling line. Fixed by truncating
                                             `trailingIndent`'s result at the first non-space/
                                             non-tab character. Verified against the real
                                             microsoft/STL tree (`ranges.hpp`, all 4 affected
                                             `_Range` occurrences now idempotent and
                                             comment-duplication-free). **Note:** a second, distinct
                                             shape of the same gap (`filesystem.hpp`'s
                                             `recursive_directory_iterator` assignment-alignment
                                             group) was found in the same session -- see fixture 124
                                             for that fix.

  real_code_regressions_123_inp/out.hpp   -- C++, `alignCommentSeparators` false-positive fix
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
                                             into a separator-alignment run: a fragment must have at
                                             most 4 whitespace-separated words, be at most 24
                                             characters, and contain no whole word (case-
                                             insensitively, single-letter words exempted) from a
                                             small common-English-stopword list; failing either
                                             check breaks the run like any non-qualifying line. This
                                             fixture also includes a genuine 2-line separator-
                                             alignment pair (`// Count : 1` / `// GrandTotal : 22`)
                                             to prove the fix doesn't regress real §15 alignment --
                                             still padded (`Count      : 1` / `GrandTotal : 22`).
                                             See `STATE_C_CPP_JAVA.md`.

  real_code_regressions_124_inp/out.hpp   -- C++, `filesystem.hpp` `recursive_directory_iterator`
                                             assignment-alignment column-padding non-idempotency
                                             (the second, distinct shape of fixture 122's gap): a
                                             class with a zero-arg default ctor, a long copy-ctor
                                             declaration whose too-long parameter list is later
                                             wrapped across multiple physical lines by
                                             `enforceCallLineBreaking` (RDD_KEY_86), a move-ctor,
                                             and a destructor -- all `= default;` one-liners. On a
                                             fresh format the long copy-ctor is still one raw line
                                             and wrongly joins the group, its full width setting the
                                             `=` column; on reformat the now-wrapped copy-ctor no
                                             longer parses as a one-liner and is excluded, shrinking
                                             the column -- non-idempotent. Root cause:
                                             `GetterSetterRuleCurly.parseOneLinerMember`'s existing
                                             breakable-width pre-check was gated only on
                                             `isDefinition`, leaving non-definition (plain
                                             declaration/pure-specifier) members with a breakable,
                                             non-empty parameter list unchecked. Fixed by adding a
                                             `hasBreakableParams` check
                                             (`!isDefinition && paramsFrom < paramsTo`) alongside
                                             the existing `hasBreakableCall` check. See
                                             `STATE_C_CPP_JAVA.md`.

  real_code_regressions_125_inp/out.html  -- HTML5, `apache/ant` `manual/` dogfood, 2 bugs: (1) a
                                             `<p>` with no explicit `</p>` before a following
                                             block-level sibling (`<h3>`) swallowed the rest of the
                                             document as its children until an unrelated downstream
                                             closing tag, producing a spurious duplicate `</p>` at
                                             EOF -- fixed by registering `p` in
                                             `XmlSpecificRule.IMPLIED_CLOSE_TRIGGERS` per the HTML5
                                             spec's "close a p element" trigger-tag list
                                             (RDD_KEY_204). (2) a same-line trailing comment after a
                                             `<td>`'s sole text child (e.g. `<td>text<!-- c --></td>`)
                                             was silently dropped -- attached correctly as the text
                                             node's `trailingComment`, but two render paths
                                             (`renderNode`'s `TEXT` case, and `renderElement`'s
                                             "sole content child" fast path reading `onlyChild.raw`
                                             directly) never consulted it; both fixed (RDD_KEY_205).
                                             See `STATE_DATA_FORMATS.md`.

  real_code_regressions_126_inp/out.java  -- Java, `apache/ant` `src/` dogfood: a braced
                                             single-statement `if` body that is itself a local
                                             variable declaration (`final boolean ignored =
                                             f.setWritable(true);`) was collapsed to a braceless
                                             `if`, which javac rejects (a declaration is not a legal
                                             braceless if/while/for body) -- fixed by refusing
                                             collapse in `BlockStructureRule.isSingleStatementBody`
                                             whenever the body's first token is `final`/`const`.
                                             See `STATE_C_CPP_JAVA.md`.

  real_code_regressions_127_inp/out.py    -- Python3, `django/django` real-code dogfood: a §8
                                             single-statement-body `match`/`case` header with its own
                                             trailing comment (e.g. `case Sequence():  # str and
                                             bytes were already handled.`) qualified for joining with
                                             its body line, but the join's `headerText` only spanned
                                             up to the header's `:`, silently deleting the comment --
                                             real content loss (surfaced via `diff -rq round1 round2`
                                             since round2 no longer had the comment to drop). Fixed
                                             in `ScopePipelineIndent`:
                                             `classifySingleStatementHeaderColon` and
                                             `classifyCaseLine` both now bail from the join whenever a
                                             trailing comment follows the header's `:`, mirroring the
                                             existing skip for a body statement's own trailing
                                             comment. See `STATE_PYTHON3.md`'s `django/django` entry.

  real_code_regressions_128_inp/out.java  -- Java, `openrewrite/rewrite` dogfood, 2 bugs sharing one
                                             root-cause shape (a fits-in-`line-length` prediction made
                                             before a later width-growing pass ran, so it agreed with
                                             reality on a fresh format but not on a reformat of
                                             already-formatted output): (a)
                                             `JavaSpecificRule.isSingleLineBody`'s overflow prediction
                                             measured a tab-indented one-liner's leading indent via
                                             raw `String.length()` instead of expanded width, so a
                                             one-liner whose true width only exceeds `line-length`
                                             once tabs expand wrongly predicted "fits" and stayed K&R
                                             on round1, then flipped to Allman on round2 once
                                             `enforceCallLineBreaking` had already wrapped it -- fixed
                                             with a local `expandedIndentWidth` helper (same formula
                                             as `MiscRuleCore`'s). (b)
                                             `enforceInitializerBraceSpacing`'s Phase-4 `{ x }`
                                             padding ran after `enforceCallLineBreaking` had already
                                             decided not to wrap, so an annotation argument just under
                                             `line-length` grew past it once padded, wrapping only on
                                             round2 -- fixed by pulling a second
                                             `enforceInitializerBraceSpacing` call forward to run
                                             right before `enforceCallLineBreaking` (same pull-forward
                                             pattern as `enforceComplexityPadding`/
                                             `enforceAttributeAndSpliceBracketPadding`), leaving the
                                             original Phase 4 call in place too. See
                                             `STATE_C_CPP_JAVA.md`'s `openrewrite/rewrite` entry.

  real_code_regressions_129_inp/out.java  -- Java, `openrewrite/rewrite` dogfood
                                             (`rewrite-benchmarks`'s `MethodMatcherBenchmark.java` and
                                             7 siblings): a `.map(name -> { ... if/else-if chain ... })`
                                             lambda's branches render on their own lines on a fresh
                                             format but fully fuse onto one line on a reformat -- a
                                             genuine non-fixed-point flap, distinct from _128's
                                             fits-prediction-before-a-later-pass shape despite looking
                                             similar. Root cause:
                                             `BlockStructureRule.collapseSingleExpressionBlocks`'s
                                             per-branch newline before a chain's next `else`
                                             (`appendChainNewlineBeforeElse`) was only ever inserted as
                                             a side effect of collapsing a *braced* if/else-if body; an
                                             already-braceless body (as fed on round2) left no brace to
                                             re-collapse, so no newline was re-inserted, and
                                             `ScopePipelineCurly`'s declaration/assignment-RHS pass
                                             (always joins an initializer back onto one line) then
                                             fused the whole chain. Fixed by adding a C/C++/Java
                                             sibling of the existing Kotlin "already-braceless
                                             multi-line body" branch: `matchControlBlock` now copies a
                                             braceless if/else-if body through verbatim (new
                                             `findBracelessStatementEnd` helper) and still invokes
                                             `appendChainNewlineBeforeElse` afterward. See
                                             `STATE_C_CPP_JAVA.md`'s `openrewrite/rewrite` entry.

  real_code_regressions_130_inp/out.java  -- Java, `openrewrite/rewrite` dogfood
                                             (`rewrite-core`'s `AdaptiveRadixTreeTest.java`, cluster 3):
                                             a `for(...; ++i)` header's pre-increment stays tight
                                             (`++i`) on a fresh format but gains a stray space (`++ i`)
                                             on a reformat, once the enclosing lambda body has been
                                             collapsed onto one line and its `for`-header re-rendered
                                             through the shared tight-attachment join point. Root cause:
                                             neither `MiscRuleCore.needsSpaceBetween` nor its documented
                                             duplicate `DeclarationAlignmentRuleCore.needsSpaceBetween`
                                             had a case for a prefix `++`/`--` immediately followed by
                                             an identifier -- `MiscRuleCurly.enforcePreIncrement`'s own
                                             swap-render path already produces the tight join on a fresh
                                             format, but once the text is already in prefix form,
                                             `collectForIncrementSpans` no longer detects it as a swap
                                             candidate (its identifier-first shape no longer matches),
                                             so a later general re-render of the collapsed one-line
                                             lambda falls through to the generic space-by-default rule.
                                             Fixed by adding a tight-join case to both methods. See
                                             `STATE_C_CPP_JAVA.md`'s `openrewrite/rewrite` entry.

  real_code_regressions_131_inp/out.java  -- Java, `openrewrite/rewrite` dogfood
                                             (`rewrite-java-{8,11,17,21,25}`'s
                                             `ReloadableJava*ParserVisitor.java`, cluster 4): a
                                             trailing `//` comment's column, in an assignment-
                                             alignment group that also contains a multi-line-call
                                             right-hand side spanning more than STYLE.md §6's
                                             supported single-newline shape, drifts by a few spaces
                                             between a fresh format and a reformat. Root cause:
                                             `MiscRuleCore.parseAssignment`'s verbatim fallback (for
                                             a value with more than one embedded newline, or a single
                                             newline `classifyMultiLineBreak` doesn't recognize)
                                             returns an ordinary non-`multiLine` `Assignment` whose
                                             `valueTokens` still contains the embedded `NEWLINE`
                                             tokens -- `MiscRuleCore.render` then fed that row's
                                             `joinVerbatim` text straight into `ColumnGrid`, whose
                                             plain `String.length()` column-width computation counted
                                             every character across the whole wrapped call, not just
                                             its first line, corrupting the whole group's
                                             comment/value column width -- and non-idempotently,
                                             since that verbatim text's own length can shift slightly
                                             between passes. Fixed by adding
                                             `valueSpansMultipleLines` and excluding any such row from
                                             the grid the same way `a.multiLine` rows already are,
                                             rendering it directly instead. See
                                             `STATE_C_CPP_JAVA.md`'s `openrewrite/rewrite` entry.

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
