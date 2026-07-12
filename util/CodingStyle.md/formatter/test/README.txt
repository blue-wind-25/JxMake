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
  java_core_inp/out.java                 -- Core Java 8-compatible constructs: declaration
                                            alignment, modifier ordering, getter/setter groups,
                                            closing comments, K&R/Allman braces, import sorting,
                                            switch, lambdas, anonymous classes.

  java_modern_inp/out.java               -- Java 17+ constructs: records, sealed/non-sealed classes,
                                            switch expressions (arrow form + block body), text
                                            blocks, var, pattern-matching instanceof.

  java_combined_inp/out.java             -- All of the above in one realistic file: sealed class
                                            with nested record, enum, inner classes; switch
                                            expressions; pattern matching; text blocks; var;
                                            getter/setter groups; import ordering.

  java_comments_inp/out.java             -- Uncommon comment placements: // and /* */ between
                                            annotations and declarations, inside method signatures,
                                            inside if/for/switch headers, between else and brace,
                                            trailing on array initializers, multi-line block
                                            comments inside methods.

  java_format_toggle_inp/out.java        -- JXM_CFMT_DIS/JXM_CFMT_ENA formatting-toggle markers
                                            (both the `//%` line-comment and `/*% */` block-comment
                                            forms), each wrapping a deliberately misformatted method
                                            or field that must survive byte-for-byte untouched, with
                                            normally-formatted declarations immediately before,
                                            between, and after each frozen region.

  java_preprocessor_method_inp/out.java  -- Regression coverage for Java source using C-preprocessor
                                            directives, including the "preprocessor directive glued
                                            onto a following method definition" bug (STATE.md Known
                                            Gaps): a `#endif` directly before a method inside a
                                            class body, with and without blank lines and a `throws`
                                            clause, must not be joined onto the method's own
                                            modifier line. Also covers the PCPP-preprocessed Java
                                            pattern used in `src/jxm/ugc/ARMCortexMThumbC.java.in`
                                            (a `.java.in` file run through a C-macro preprocessor
                                            before compilation, per README.md's "C-preprocessor
                                            directives in Java source" note): a `#define`-style
                                            function-like macro precedes a class and is invoked with
                                            loosely-spaced call arguments (`__GEN_CXI_NPR_NPR__(
                                            clrex, ... )`). Confirms the `#define` line itself
                                            passes through untouched (recognized/skipped like any
                                            other preprocessor directive) while the macro invocation
                                            lines still get normal call-padding tightening (`(clrex,
                                            ...)`) and are idempotent.

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
  c_core_inp/out.c                       -- C11 constructs: declaration alignment, bitfields,
                                            pointer placement, struct/enum/typedef, function Allman
                                            braces, control-flow K&R, pre-increment, static
                                            reordering, assignment alignment.

  c_combined_inp/out.c                   -- All C constructs together in one realistic file: macros,
                                            enums with closing comments, structs, forward
                                            declarations, global state alignment, public API,
                                            internal functions, inline comments.

  c_comments_inp/out.c                   -- Uncommon comment placements in C: inside struct, between
                                            params, inside if/for headers, between else and brace,
                                            divider normalization, comments on macros.

  c_cpp_decl_gaps_inp/out.c              -- Regression coverage for three DeclarationAlignmentRule
                                            fixes (STATE.md "Known Gaps -- Fixed"): the `* const`
                                            column gap in mixed pointer-star groups, `typedef`
                                            joining and aligning with a surrounding plain-variable
                                            group, and direct function-pointer declarations
                                            (including multi-star `(**cb)`) joining a group.

C++:
  cpp_core_inp/out.cpp                   -- C++11/14 constructs: class with access specifiers,
                                            template class, lambdas, auto return type, initializer
                                            list constructors, getter/setter groups, extern "C".

  cpp_modern_inp/out.cpp                 -- C++17/20/23 constructs: structured bindings,
                                            init-statement if/switch, concepts/requires,
                                            consteval/constinit, operator<=>, coroutines
                                            (co_yield/co_return).

  cpp_combined_inp/out.cpp               -- All C++ constructs together: concepts, enum class,
                                            template class with nested Config struct, structured
                                            bindings, init-statement if/switch, consteval/constinit,
                                            operator<=>, lambda with auto return, extern "C",
                                            trailing comments on declarations.

  cpp_comments_inp/out.cpp               -- Uncommon comment placements in C++: inside template
                                            parameter lists, inside concept requires expressions,
                                            between class specifier and base, inside function
                                            params, inside structured bindings, inside requires
                                            clauses.

C/C++ Headers:
  h_core_inp/out.h                       -- C header with #ifndef guard: header zone spacing,
                                            include ordering (angle vs quote), struct alignment,
                                            pointer declarations, #ifdef __cplusplus extern "C".

  h_combined_inp/out.h                   -- Combined C header: guard zones, macros alignment, named
                                            enum/struct with closing comments, full API declaration
                                            group, extern "C".

  hpp_core_inp/out.hpp                   -- C++ header with #pragma once: pragma once zone spacing,
                                            concepts, enum class, structs with operator<=>, abstract
                                            class interface, concrete derived classes.

  hpp_combined_inp/out.hpp               -- Combined C++ header: pragma once zones, concepts,
                                            template base class, concrete subclass, factory
                                            declaration, extern "C" block.

Real-code regressions:
  real_code_regressions_1_inp/out.cpp    -- Distilled from tinyexpr-plusplus: same-line-sibling
                                            call-argument mis-split, an undercounted call "does it
                                            fit" length check, and an
                                            enforceComplexityPadding/enforceCallLineBreaking
                                            pass-ordering idempotency bug.

  real_code_regressions_2_inp/out.java   -- Distilled from RobotCoding's gui_frontend: `>>>`
                                            mis-tokenized as `>>`+`>`; GetterSetterRule padding
                                            computed against stale pre-padding text;
                                            enforceCallLineBreaking losing complexity-padding
                                            awareness when joining a multi-line call; and a
                                            getter/setter grouping decision that didn't predict a
                                            later line-break, so a fresh format and a reformat
                                            produced different output.

  real_code_regressions_3_inp/out.java   -- Distilled from dogfooding the formatter's own src/ tree:
                                            MiscRule's consecutive-assignment alignment rejected an
                                            RHS already wrapped by a later pass, splitting/shrinking
                                            the alignment group on a second format instead of
                                            treating it as one group like a fresh format does.

  real_code_regressions_4_inp/out.hpp    -- Distilled from martinus/nanobench: no tokenizer support
                                            for C++11 raw string literals at all (corrupting
                                            brace-depth tracking and truncating up to ~46% of real
                                            files); raw-string support gated on a C-only flag
                                            instead of C-or-C++; and DeclarationAlignmentRule
                                            dropping a leading `template<...>` prefix on bare
                                            forward declarations.

  real_code_regressions_5_inp/out.cpp    -- User-reported: a `while` loop's own closing `}` stayed
                                            indented to its body instead of its frame. Fixed by
                                            force-reindenting a scope's closing brace to the frame's
                                            indent, with carve-outs for case/default-label spans, a
                                            comment in the trailing gap, bare compound blocks, and
                                            empty bodies.

  real_code_regressions_6_inp/out.java   -- Found via idempotency testing on google-java-format: a
                                            trailing same-line closing comment (`} // if`) before a
                                            case/default label was wrongly treated as a *leading*
                                            comment, forcing a spurious blank line and orphaning it.
                                            Fixed by only applying that exception to comments that
                                            start their own line.

  real_code_regressions_7_inp/out.java   -- Found via idempotency testing on google-java-format:
                                            arrow-form `case X -> body;` joins didn't check the
                                            resulting line length, so a fresh format could produce
                                            an over-length line that a reformat then broke apart.
                                            Fixed by predicting the joined width before committing
                                            to the join.

  real_code_regressions_8_inp/out.java   -- Found via idempotency testing on google-java-format: the
                                            getter/setter one-liner pass misparsed arrow-form
                                            `case`/`default` switch arms as accessor members,
                                            injecting garbage column padding. Fixed by rejecting any
                                            one-liner starting with `case`/`default`.

  real_code_regressions_9_inp/out.java   -- Found via idempotency testing on pcpp_java: switch
                                            inline-alignment padded a short label to match a wider
                                            sibling without checking the row's final length,
                                            producing an unstable over-length line. Fixed by
                                            predicting every row's rendered length before padding.

  real_code_regressions_10_inp/out.java  -- Found via idempotency testing on pcpp_java:
                                            one-liner-body detection used a raw newline check that
                                            could be fooled by a call already broken across lines by
                                            an earlier pass, wrongly recursing into and corrupting
                                            an already-correct one-liner body. Fixed with a
                                            paren/bracket-depth-aware scan.

  real_code_regressions_11_inp/out.c     -- Found via idempotency/round-trip testing on
                                            tongsuo-mini: a flat aggregate initializer (e.g. a large
                                            S-box table) collapsed to one line with no length check,
                                            producing lines thousands of characters long. Fixed by
                                            rejecting the collapse when it would exceed the line
                                            limit, plus normalizing the closing `}` of oversized
                                            multi-line initializers onto its own line.

  real_code_regressions_12_inp/out.hpp   -- Found via idempotency testing on
                                            serge-sans-paille/frozen's catch.hpp: a struct with a
                                            virtual destructor, a long pure-virtual signature, and a
                                            template method-with-body was corrupted on the first
                                            pass -- members merged, a stray space on `~ClassName()`,
                                            and `};` accumulating extra semicolons each pass. Two
                                            depth-tracking bugs in
                                            DeclarationAlignmentRule.parseDeclaration, both fixed.

  real_code_regressions_13_inp/out.hpp   -- Found via idempotency testing on the same catch.hpp: a
                                            nested `for` loop came out of the first pass with
                                            corrupted indentation. Root cause: the
                                            preprocessor-directive tokenizer had no
                                            backslash-line-continuation handling, desyncing the
                                            brace-depth counter for the rest of the file. Fixed by
                                            adding the same continuation handling its sibling
                                            already had.

  real_code_regressions_14_inp/out.hpp   -- Minimal repro for a general indent fix: a construct
                                            sharing its opening line with a parent `{`, nested
                                            inside a namespace, came out under-indented by one level
                                            because the indent fallback used a depth counter that
                                            doesn't increment for namespace bodies. Fixed by
                                            threading a real accumulated-indent string through the
                                            recursion instead of guessing from depth.

  real_code_regressions_15_inp/out.hpp   -- Minimal repro for a tokenizer fix: catch.hpp's
                                            Objective-C interop block has genuine `[[NSString alloc]
                                            init]` message sends, which the tokenizer used to merge
                                            into a C++17-attribute-style `[[...]]` token regardless
                                            of context, desyncing bracket-depth for the rest of the
                                            file. Fixed by only merging `[[` when a forward scan
                                            confirms a genuine attribute-shaped close.

  real_code_regressions_16_inp/out.hpp   -- Covers 4 unrelated idempotency bugs surfaced once
                                            fixture 15's fix stopped masking them, all in
                                            ScopePipeline/TokenizerCore: an off-by-one in the
                                            namespace-detection scan; a constructor's
                                            member-initializer list being mistaken for its own
                                            signature body (corrupting spacing, and missing a
                                            trailing-length check); and an elaborated-type
                                            declaration with an empty initializer (`struct sigaction
                                            sa = { };`) misdetected as a struct body, appending an
                                            extra `;` on every pass.

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

  real_code_regressions_28_inp/out.hpp   -- C++, real-code test against taocpp/PEGTL
                                            (rematch_input.hpp): reclassifyAngleBrackets'
                                            single-open-`<` branch retyped a literal `>>` token
                                            via `retype()` (preserving its 2-char text) while
                                            also appending a new 1-char `>` token, duplicating a
                                            character on the first format pass and breaking a
                                            `template<...>` forward declaration. Fixed by giving
                                            the retyped token its own explicit 1-char text.

  real_code_regressions_29_inp/out.java  -- Java, real-code test against local `anemonesoft`
                                            candidate (HelpBox.java/Spreadsheet.java):
                                            renderCallCandidate's groupByOriginalLine tracks only
                                            paren/bracket depth, not brace depth, so a call's
                                            multi-line brace-body trailing argument got silently
                                            swallowed into one unbounded output line, flapping to
                                            an idempotency failure once a later pass reacted to
                                            the now-multi-line body. Fixed by widening an existing
                                            Kotlin-only "leave such an argument untouched" bail to
                                            all languages via a new containsInternalNewline check.

  real_code_regressions_30_inp/out.kt    -- Kotlin, real-code test against `square/okio`: three
                                            co-occurring bugs. (1) renderTokens had no unary-vs-
                                            binary `-`/`+` notion, corrupting `val x = -1` into
                                            `= - 1`; fixed with an isUnaryMinusOperand lookback.
                                            (2) applySignaturePass's `: ReturnType` tail detection
                                            merged a headerless declaration with an unrelated
                                            later one across a blank line; fixed with a
                                            hasTopLevelBlankLine guard. (3) braceless if/while/for
                                            collapse rendered a stale, untightened keyword-paren
                                            space, causing enforceCallLineBreaking to over-wrap a
                                            line that fits at its true final width; fixed by
                                            tightening the space at collapse time.

  real_code_regressions_31_inp/out.kt    -- Kotlin, two compile-breaking bugs found via `kotlinc`
                                            against `square/okio` (not caught by idempotency
                                            diffing). (1) MULTI_CHAR_OPS was missing `===`/`!==`,
                                            so `!==` lexed as separate tokens and got re-spaced
                                            into invalid `!= =`; fixed by adding both operators.
                                            (2) the braceless-collapse dispatch treated a
                                            do-while's trailing `while (cond)` as a loop-starting
                                            `while`, fusing the next statement onto the same line;
                                            fixed with an isDoWhileTailKeyword lookback.

  real_code_regressions_32_inp/out.kt    -- Kotlin, real-code test against `square/kotlinpoet`: a
                                            nested `when { ... }` used as a `when` branch's body
                                            flapped its closing `}`'s indentation round1-vs-round2,
                                            since Kotlin's braceLineIndent anchors on the brace's
                                            pre-merge physical line at Phase 0 but
                                            formatWhenExpressions' Phase 4 arrow-alignment pass
                                            later merges the branch label onto that same line.
                                            Fixed with a findMergingWhenBranchLineStart lookahead
                                            that anchors on the eventual post-merge line up front.

  real_code_regressions_33_inp/out.kt    -- Kotlin, real-code test against `square/kotlinpoet`: a
                                            first-pass compile-breaking bug found via `kotlinc` --
                                            an expression-bodied function whose body is itself a
                                            trailing-lambda call (`fun addTypes(...): T = apply {
                                            ... } as T`) had `apply`'s own unrelated `{` wrongly
                                            Allman-converted as the function's own body brace.
                                            Root cause: findSignatureCloseParenBeforeBrace's
                                            backward scan for `: ReturnType` had no bail-out on an
                                            intervening depth-0 `=`. Fixed with that bail-out.

  real_code_regressions_34_inp/out.hpp   -- C++, real-code test against `NVIDIA/stdexec`:
                                            combines two bugs. (1) A requires-expression
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

  real_code_regressions_35_inp/out.hpp   -- C++, real-code test against `NVIDIA/stdexec`
                                            (continuing the candidate above): a compile-breaking
                                            bug ("Bug 3") in `__counting_scopes.hpp` -- tryCollapse's
                                            renderInline flattened a multi-line `if` condition
                                            containing a `//` comment between call arguments,
                                            silently absorbing every following token (including
                                            the closing `}`) into the comment and producing a
                                            50-error unmatched-brace cascade. Fixed with a
                                            containsLineComment guard that refuses the collapse
                                            when the condition carries a line comment.

  real_code_regressions_36_inp/out.cpp   -- C++, real-code test against `NVIDIA/stdexec`
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
                                            `Kotlin/kotlinx.coroutines`: a KDoc's own nested
                                            `/* ... */` snippet closed the outer `/**` doc-comment
                                            early, mis-lexing and silently truncating the rest of
                                            the file (`Guidance.kt`, ~330 lines dropped). Fixed by
                                            tracking block-comment nesting depth, Kotlin-only.

  real_code_regressions_39_inp/out.kt    -- Kotlin, real-code test against
                                            `Kotlin/kotlinx.coroutines`: `this@Label` got a stray
                                            space inserted before `@` (`this @Label`, a real syntax
                                            error) since `enforceLabeledJumpSpacing`'s state
                                            machine didn't recognize `this` before `@`. Fixed with a
                                            new state pair tightening `this@Label`.

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
                                            `Kotlin/kotlinx.coroutines`: `ThreadSafeHeap.kt`'s (and
                                            `AbstractSharedFlow.kt`/`ConcurrentLinkedList.kt`/
                                            `ExceptionsConstructor.kt`'s) top-level class closing
                                            brace/comment drifted deeper once a wrapped multi-line
                                            generic `where` clause put the class's own `{` on a
                                            deeply-indented continuation line -- `ScopePipeline`'s
                                            `effectiveSpanIndent` chose `braceIndent` (that
                                            continuation line's indent) over `spanIndent` (the
                                            class header's own indent) whenever it was deeper,
                                            correct for an unnamed trailing-lambda body
                                            (RDD_KEY_136) but wrong for a NAMED construct
                                            (class/fun/object), which must always indent relative
                                            to its own header. Fixed by gating `braceIndent` off
                                            entirely for named scopes.

  real_code_regressions_43_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `Kotlin/kotlinx.coroutines`: `JobSupport.kt`'s
                                            `makeCompletingOnce`, a wrapped multi-argument
                                            `throw IllegalStateException(...)` call used as a
                                            keyword-less `when` branch's body (`label ->` on one
                                            line, body starting the next) had its continuation
                                            lines/closing `)` one level deeper on round1 than
                                            round2. `MiscRule.enforceCallLineBreaking` (Phase 1)
                                            computed the call's base indent from its own physical
                                            line before `KotlinSpecificRule.formatWhenExpressions`
                                            (Phase 4) unconditionally merges the branch label and
                                            body onto one line -- stale by one level the moment
                                            that merge happens, correct only once reformatted from
                                            already-merged input. Fixed with a new
                                            `MiscRule.effectiveCallBaseIndent`: when a call
                                            candidate's own line is immediately preceded by a
                                            top-level `->` (Kotlin only), use that arrow line's
                                            indent instead of the candidate's own.

  real_code_regressions_44_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `Kotlin/kotlinx.coroutines`: `BufferedChannel.kt`'s
                                            `onUndeliveredElementReceiveCancellationConstructor`, a
                                            bare, unnamed nested-lambda-chain closing `}` drifted
                                            from col 4 to col 8 on round2.
                                            `ScopePipeline.findParentIndent`'s backward
                                            statement-boundary scan is bounded by the current
                                            span's own `span.start`, which `splitTopLevelSpans`
                                            (splitting on top-level BRACE spans) can position mid-
                                            statement: a braceless `if (...) { ... } else expr`
                                            statement's braced `if`-branch is its own span, so the
                                            braceless `else expr` becomes dangling leading text at
                                            the very start of the NEXT real span rather than a
                                            genuine new statement. A later declaration's own
                                            backward brace-search (bounded by that span.start) then
                                            landed on this dangling `else` as its anchor -- wrongly
                                            returning an unrelated, earlier statement's own line
                                            indent once a later phase's re-alignment happened to put
                                            that `else` alone on its own physical line (only on
                                            round2, not round1). Fixed by detecting an anchor that
                                            is itself a dangling `else`/`catch`/`finally` and
                                            skipping forward past its one-line body to the next real
                                            statement before deriving the indent.

  real_code_regressions_45_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `Kotlin/kotlinx.coroutines`: `ChannelFlow.kt`'s
                                            `UndispatchedContextCollector`'s `countOrElement`/
                                            `emitRef` `val` pair. `KotlinDeclarationAlignmentRule
                                            .renderAlignedGroup` padded `countOrElement`'s
                                            (typeless) row out to match `emitRef`'s type-column
                                            width, widening `emitRef`'s own line enough that a
                                            later pass wrapped its brace-bodied lambda
                                            initializer; re-parsing that now-multi-line
                                            initializer on the next pass correctly bailed `emitRef`
                                            out of its alignment group (by design --
                                            `spansMultipleLines` never guesses past a genuine
                                            multi-line block), collapsing `countOrElement`'s
                                            padding to a single space -- an idempotency flap.
                                            Fixed by plumbing `indentWidth`/`lineLengthLimit`
                                            awareness into `renderAlignedGroup`: a row whose
                                            group-aligned width would overflow the budget is
                                            excluded from the shared column grid (rendered solo
                                            instead) whenever its own initializer is brace-bodied
                                            (a lambda/object/block -- the only shape
                                            `spansMultipleLines`'s brace-depth check can ever bail
                                            on); a plain call/expression initializer is never
                                            excluded, since any future wrap of it lands strictly
                                            inside parens with no enclosing brace, which
                                            `spansMultipleLines`'s existing paren-depth carve-out
                                            (RDD_KEY_135) already keeps groupable/idempotent
                                            forever regardless of length (see
                                            `real_code_regressions_18`, unaffected by this fix).

  real_code_regressions_46_inp/out.kt    -- Kotlin, real-code idempotency test against
                                            `square/kotlinpoet`'s Shape 1 idempotency-gap group
                                            (`CodeWriter.kt`/`LambdaTypeName.kt`/
                                            `MemberSpecHolder.kt`/`ParameterizedTypeName.kt`/
                                            `TypeVariableName.kt`/`WildcardTypeName.kt`), two
                                            distinct bugs both in `MiscRule.enforceCallLineBreaking`.
                                            Bug A (`MemberSpecHolder.kt`'s
                                            `addProperties`/`addFunctions`): a multi-line,
                                            column-broken function signature with a trailing
                                            `= apply { ... }` expression body wraps its own params
                                            on a fresh format but collapses to one line on a
                                            reformat. Root cause: `lineEndIndex`'s "does the whole
                                            line fit" measurement stops at the first NEWLINE token,
                                            which on a reformat sits earlier than on a fresh format
                                            whenever an unrelated NESTED call inside the tail's own
                                            untouched trailing text (e.g. `apply { x.map(\n  y\n) }`)
                                            happens to already be wrapped from the previous round --
                                            undercounting the true rendered width and flipping the
                                            fits-or-not verdict. Fixed with a new depth-aware
                                            `effectiveLineEndIndex` that skips over a NEWLINE still
                                            inside an unclosed `(`/`[`/`{` instead of stopping there.
                                            Bug B (the other 5 files, e.g. `LambdaTypeName.kt`'s
                                            `copy(...)`): a genuine Kotlin function signature with
                                            an explicit `: ReturnType {` block-body tail (no `=`) had
                                            its already-correctly-rendered, column-padded,
                                            trailing-comma-preserved multi-line param list (from
                                            `ScopePipeline`/`KotlinSignatureRule`) silently
                                            re-wrapped by this same pass as if it were a plain
                                            untyped call, discarding the padding and trailing comma
                                            (this is RDD_KEY_149's originally-deferred bug, now
                                            root-caused): the "is this really a call, not a true
                                            signature" exemption only recognized an immediate `{`
                                            right after the closing `)`, missing the
                                            `: ReturnType {` shape entirely. Fixed with a new
                                            `isKotlinReturnTypeThenBlockBody` lookahead extending
                                            the exemption to that shape (deliberately NOT extended to
                                            a `: ReturnType = expr` tail, which still needs Bug A's
                                            fix to account for its own untouched trailing text).

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
