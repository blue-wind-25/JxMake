Formatter Test Suite
====================

This directory contains the permanent dogfood/regression test suite for the
code-formatter code formatter. Tests are mechanically verifiable: no manual inspection
required once the expected output files are authored.


File Naming Convention
----------------------

    <name>_inp.<ext>   -- Input file (intentionally misformatted or mixed-style)
    <name>_out.<ext>   -- Expected output (correctly formatted per the style guide)

Both files are committed permanently. Add more pairs over time as new edge cases
or regressions are discovered.


Test Files
----------

Java:
  java_core_inp/out.java     -- Core Java 8-compatible constructs: declaration
                                alignment, modifier ordering, getter/setter groups,
                                closing comments, K&R/Allman braces, import sorting,
                                switch, lambdas, anonymous classes.

  java_modern_inp/out.java   -- Java 17+ constructs: records, sealed/non-sealed
                                classes, switch expressions (arrow form + block body),
                                text blocks, var, pattern-matching instanceof.

  java_combined_inp/out.java -- All of the above in one realistic file: sealed class
                                with nested record, enum, inner classes; switch
                                expressions; pattern matching; text blocks; var;
                                getter/setter groups; import ordering.

  java_comments_inp/out.java -- Uncommon comment placements: // and /* */ between
                                annotations and declarations, inside method signatures,
                                inside if/for/switch headers, between else and brace,
                                trailing on array initializers, multi-line block
                                comments inside methods.

  java_format_toggle_inp/out.java -- JXM_CFMT_DIS/JXM_CFMT_ENA formatting-toggle markers
                                (both the `//%` line-comment and `/*% */` block-comment
                                forms), each wrapping a deliberately misformatted method
                                or field that must survive byte-for-byte untouched, with
                                normally-formatted declarations immediately before,
                                between, and after each frozen region.

  java_preprocessor_method_inp/out.java -- Regression coverage for the "preprocessor
                                directive glued onto a following method definition"
                                bug (STATE.md Known Gaps): a `#endif` directly before
                                a method inside a class body, with and without blank
                                lines and a `throws` clause, must not be joined onto
                                the method's own modifier line.

C:
  c_core_inp/out.c           -- C11 constructs: declaration alignment, bitfields,
                                pointer placement, struct/enum/typedef, function
                                Allman braces, control-flow K&R, pre-increment,
                                static reordering, assignment alignment.

  c_combined_inp/out.c       -- All C constructs together in one realistic file:
                                macros, enums with closing comments, structs, forward
                                declarations, global state alignment, public API,
                                internal functions, inline comments.

  c_comments_inp/out.c       -- Uncommon comment placements in C: inside struct,
                                between params, inside if/for headers, between else
                                and brace, divider normalization, comments on macros.

  c_cpp_decl_gaps_inp/out.c  -- Regression coverage for three DeclarationAlignmentRule
                                fixes (STATE.md "Known Gaps -- Fixed"): the `* const`
                                column gap in mixed pointer-star groups, `typedef`
                                joining and aligning with a surrounding plain-variable
                                group, and direct function-pointer declarations
                                (including multi-star `(**cb)`) joining a group.

C++:
  cpp_core_inp/out.cpp       -- C++11/14 constructs: class with access specifiers,
                                template class, lambdas, auto return type, initializer
                                list constructors, getter/setter groups, extern "C".

  cpp_modern_inp/out.cpp     -- C++17/20/23 constructs: structured bindings,
                                init-statement if/switch, concepts/requires,
                                consteval/constinit, operator<=>, coroutines
                                (co_yield/co_return).

  cpp_combined_inp/out.cpp   -- All C++ constructs together: concepts, enum class,
                                template class with nested Config struct, structured
                                bindings, init-statement if/switch, consteval/constinit,
                                operator<=>, lambda with auto return, extern "C",
                                trailing comments on declarations.

  cpp_comments_inp/out.cpp   -- Uncommon comment placements in C++: inside template
                                parameter lists, inside concept requires expressions,
                                between class specifier and base, inside function
                                params, inside structured bindings, inside requires
                                clauses.
Headers:
  h_core_inp/out.h           -- C header with #ifndef guard: header zone spacing,
                                include ordering (angle vs quote), struct alignment,
                                pointer declarations, #ifdef __cplusplus extern "C".

  h_combined_inp/out.h       -- Combined C header: guard zones, macros alignment,
                                named enum/struct with closing comments, full API
                                declaration group, extern "C".

  hpp_core_inp/out.hpp       -- C++ header with #pragma once: pragma once zone
                                spacing, concepts, enum class, structs with
                                operator<=>, abstract class interface, concrete
                                derived classes.

  hpp_combined_inp/out.hpp   -- Combined C++ header: pragma once zones, concepts,
                                template base class, concrete subclass, factory
                                declaration, extern "C" block.

Real-code regressions:
  real_code_regressions_1_inp/out.cpp -- Distilled from bugs found testing against
                                real, compiling third-party C++ (tinyexpr-plusplus):
                                same-line-sibling call-argument mis-split, call
                                "does it fit" length undercount, and a
                                enforceComplexityPadding/enforceCallLineBreaking
                                pass-ordering idempotency bug.

  real_code_regressions_2_inp/out.java -- Distilled from bugs found testing against
                                real, compiling third-party Java (RobotCoding's
                                gui_frontend): `>>>` unsigned-right-shift mis-tokenized
                                as `>>`+`>`; GetterSetterRule body-column padding
                                computed against pre-padding text, stale after Phase 1
                                shrinks it; enforceCallLineBreaking joining a
                                multi-line call losing complexity-padding awareness;
                                and GetterSetterRule grouping / one-liner
                                Allman-brace-avoidance not predicting that an
                                over-long one-liner body would later be broken
                                across lines by enforceCallLineBreaking, causing
                                the grouping/brace-style decision (and thus output)
                                to differ between a fresh format and a reformat of
                                already-formatted output.

  real_code_regressions_3_inp/out.java -- Distilled from a bug found dogfood-testing
                                the formatter against its own src/ tree: MiscRule's
                                consecutive-assignment `=`-alignment rule rejected any
                                assignment whose RHS had already been wrapped across
                                lines by a later pass (enforceCallLineBreaking),
                                splitting the alignment run into smaller subgroups
                                (and shrinking their padding) on a second format of
                                already-formatted output, rather than continuing to
                                treat it as one aligned group as on a fresh format.

  real_code_regressions_4_inp/out.hpp -- Distilled from bugs found testing against
                                real, compiling third-party C++ (martinus/nanobench,
                                a single-header library): (1) the tokenizer had no
                                support for C++11 raw string literals
                                (`R"DELIM(...)DELIM"`) at all, so `{`/`}` characters
                                inside one (e.g. nanobench's mustache HTML/JSON
                                templates) were lexed as ordinary punctuation,
                                corrupting brace-depth tracking for the rest of the
                                file and silently truncating up to ~46% of real-world
                                files; (2) even after adding raw-string support, it
                                was gated on the tokenizer's C-only `isC` flag
                                instead of C-or-C++, so `.hpp`/`.cpp` files were
                                unaffected; (3) DeclarationAlignmentRule's general
                                (non-function-forward-declaration) group renderer
                                silently dropped a leading `template<...>` prefix
                                captured on a bare forward declaration (`template
                                <typename T>\nstruct Foo;`) -- only the
                                function-forward-declaration renderer emitted it.

  real_code_regressions_5_inp/out.cpp -- Distilled from a user-reported bug (a `while`
                                loop's own closing `}` staying indented to match its
                                body instead of the frame that opened it) and the
                                edge cases found fixing it: a scope's closing-brace
                                gap is now force-reindented to the frame's own
                                indent, unless (a) a `case`/`default` label shares a
                                span with the construct that follows it (the
                                anchor-finding walk must skip past the label, not
                                land on it), (b) a comment sits directly in the
                                trailing gap (e.g. between a block and a following
                                `else`) -- left untouched, (c) it's a bare compound
                                block (`{ ... }` with no preceding keyword) -- the
                                `{` itself is the anchor, or (d) the body is
                                empty/whitespace-only (`{}`) -- must not be expanded
                                into `{\n}`.

  real_code_regressions_6_inp/out.java -- Found via real-code idempotency testing on
                                google-java-format (github.com/google/google-java-format).
                                A case body ending in a trailing same-line closing
                                comment left by an earlier format pass (e.g. `} // if`
                                before the next `case`/`default` label or the switch's
                                own closing brace) was wrongly treated by
                                SwitchRule.ensureBlankLineInGap as a *leading* comment
                                glued to what follows (the doc'd exception meant for
                                e.g. `// comment before case\ncase 1:`), forcing a blank
                                line to be inserted directly before it and splitting it
                                onto its own orphaned line (`}\n\n // if`) -- reproduced
                                only on the *second* format of already-formatted output.
                                Fixed by only treating a comment as that "leading
                                comment" exception when it starts its own new line
                                (checked via the new startsOwnLine helper), not when
                                it's a trailing comment on the same line as preceding
                                content.

  real_code_regressions_7_inp/out.java -- Found via real-code idempotency testing on
                                google-java-format. JavaSpecificRule.applyArrowAlignment
                                unconditionally joined an arrow-form `case X -> body;`
                                label onto the same line as its body, with no check on
                                whether the resulting single line would exceed
                                lineLengthLimit. A fresh format could produce an
                                over-length joined line that enforceCallLineBreaking
                                (an earlier pipeline phase) never got to react to;
                                reformatting that already-joined output then let
                                enforceCallLineBreaking break it back apart -- not
                                idempotent. Fixed by predicting the joined line's width
                                before committing to the join, leaving any case whose
                                join would overflow byte-for-byte untouched instead.

  real_code_regressions_8_inp/out.java -- Found via real-code idempotency testing on
                                google-java-format. GetterSetterRule.parseOneLinerMember
                                (the getter/setter one-liner column-alignment pass)
                                treated every one-physical-line top-level statement in a
                                scope as a candidate member, so an arrow-form
                                `case`/`default` switch arm got misparsed as a bogus
                                getter/setter: e.g. `case CLASS, INTERFACE ->
                                visitClassDeclaration(tree);` read as return-type `case
                                CLASS , INTERFACE ->` + name `visitClassDeclaration`,
                                and `default -> throw new AssertionError(x);` read as
                                return-type `default -> throw new` + name
                                `AssertionError`. Grouping these fake members together
                                and column-aligning the "return type" cell to the wider
                                sibling's width injected garbage padding into the
                                shorter case's body. Fixed by rejecting any one-liner
                                whose first significant token is the `case`/`default`
                                keyword before the name/return-type heuristics run.

  real_code_regressions_9_inp/out.java -- Found via real-code idempotency testing on
                                pcpp_java (a Java preprocessor tool). SwitchRule's
                                inline-switch column alignment never checked a row's
                                rendered length against lineLengthLimit before writing
                                it: padding a short label (e.g. `default`) out to match
                                a much wider sibling label's column could push that
                                row past the limit even though the switch's original,
                                unpadded text fit. A fresh format produced a stable-
                                looking over-length aligned line that
                                MiscRule.enforceCallLineBreaking (an earlier pipeline
                                phase) never got to react to; reformatting that output
                                let enforceCallLineBreaking break the now-over-length
                                line apart, after which the alignment pass no longer
                                recognized the row shape and left it un-aligned -- not
                                idempotent. Fixed by predicting every row's final
                                rendered length before committing to any padding,
                                leaving the whole switch's cases byte-for-byte
                                untouched if even one row would overflow.

  real_code_regressions_10_inp/out.java -- Found via real-code idempotency testing on
                                pcpp_java. ScopePipeline.processScope decided whether a
                                non-named scope body (e.g. an `if` one-liner body kept
                                on its original single physical line) was still a
                                single-statement "one-liner" via a raw
                                `childSource.contains("\n")` check. On a fresh format
                                that's correct -- a one-liner body has no embedded
                                newline at all -- but MiscRule.enforceCallLineBreaking
                                can break an over-length call inside that same one-liner
                                body across multiple physical lines while leaving it one
                                logical statement; reformatting that already-broken
                                output then made the raw newline check see newlines
                                that are strictly inside the call's own parens and
                                wrongly treat the body as a real multi-statement block,
                                recursing into it and column-splitting/reindenting its
                                statements -- corrupting output that was already
                                correctly formatted. Fixed by replacing the raw
                                substring check with a paren/bracket-depth-aware scan
                                (`hasTopLevelNewline`) that only counts a newline at
                                depth 0 as evidence of a real multi-statement body.

  real_code_regressions_11_inp/out.c -- Found via real-code idempotency/round-trip
                                testing on tongsuo-mini (a C17 crypto/TLS codebase).
                                DeclarationAlignmentRule.parseDeclaration accepted any
                                flat `{ a, b, c }` aggregate init (no nested braces, no
                                `//` comments) as safely collapsible to one rendered
                                line, with no check against lineLengthLimit -- so a
                                large byte/word table (e.g. an S-box) that was
                                originally spread across many source lines collapsed
                                into a single line thousands of characters long, since
                                this class has no multi-line-initializer render path to
                                re-wrap it afterward. Fixed by estimating the flat
                                aggregate init's own rendered width and rejecting the
                                collapse (leaving the statement untouched) if it alone
                                would exceed lineLengthLimit -- same reasoning as the
                                existing `//`-comment guard on the same code path.

  real_code_regressions_12_inp/out.hpp -- Found via real-code idempotency testing
                                on serge-sans-paille/frozen (specifically its bundled
                                tests/catch.hpp). A struct containing a virtual
                                destructor, a long-signature pure-virtual method, and a
                                template method-with-body got silently corrupted on the
                                FIRST format pass (not just non-idempotent): all members
                                merged into one garbled blob, the destructor's `~` gained
                                a stray trailing space (`~ ClassName()`), and the
                                struct's closing `};` accumulated an extra semicolon on
                                every repeated pass (`};` -> `};;` -> `};;;` ...). Root
                                cause: two separate depth-tracking bugs in
                                DeclarationAlignmentRule.parseDeclaration, both triggered
                                only when the struct's LAST member is/contains a
                                braceless-body control statement (e.g. a bodyless
                                `for(...) stmt;`) immediately followed by the class's own
                                `};`: (1) the top-level statement-colon scan that detects
                                bitfields (`int x : 3;`) was not depth-aware, so a `:`
                                inside a nested range-based `for( auto v : values )`
                                was mistaken for a bitfield colon at the OUTER
                                struct-body scope, misrouting the entire multi-member
                                struct into parseBitfield, which has no multi-line
                                render path; (2) separately, the no-`=` direct-list-init
                                branch (`Type name{args};`, e.g. `int x{};`) left
                                initTokens empty and never checked whether a trailing
                                `{...}` was flat before falling through to the generic
                                declarator render, so a NON-flat trailing brace body
                                (nested braces, e.g. a function/method body) could still
                                slip through uncollapsed-checked in other call shapes.
                                Fixed by (1) making the colon scan track `(`/`[`/`{`
                                depth so only a genuine top-level `:` triggers the
                                bitfield path, and (2) adding an explicit
                                non-flat-trailing-`{...}` rejection to the no-`=` branch,
                                mirroring the existing `eqIdx >= 0` branch's own
                                rejection, while leaving the already-correct flat cases
                                (enum/direct-list-init) untouched.


How Tests Are Run
-----------------

Use the Makefile target from the formatter root directory:

    make test

This runs the formatter on every *_inp file, writes output to a temp directory,
and diffs against the corresponding *_out file. Any mismatch is a test failure.
After the forward pass, each *_out file is also formatted and diffed against
itself (idempotency check) -- the formatter must produce no changes on
already-correctly-formatted input.

See the Makefile for the exact commands and how to specify a custom JAR path.


Adding New Tests
----------------

1. Create <name>_inp.<ext> with the construct or edge case you want to test.
2. Run the formatter on it to produce the initial output.
3. Review the output for correctness against the style guide.
4. If correct, save it as <name>_out.<ext>.
5. If not correct, fix the formatter, then repeat.
6. Commit both files.

The *_out files are the ground truth. If a formatter fix intentionally changes
the output for a rule, update the corresponding *_out file in the same commit.


Dogfood Test (Self-Formatting)
------------------------------

In addition to the above file-pair tests, the Makefile's `test` target also runs
the formatter against its own Java source tree under src/. Expected outcomes:

  1. The formatted source compiles clean with javac (no errors).
  2. Running the formatter a second time on the formatted source produces no
     changes (idempotency).
  3. The class/interface/enum declaration count in the formatted source matches
     the original (no declarations were deleted or duplicated).

No bytecode comparison is performed -- line-number tables in .class files change
whenever formatting adds or removes blank lines, making bytecode diffs unreliable.
