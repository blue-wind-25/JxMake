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
                                            (rematch_input.hpp): TokenizerCore.reclassifyAngleBrackets'
                                            single-open-`<` branch for splitting a literal `>>` token
                                            retyped the token to ANGLE_BRACKET_CLOSE via `retype()`,
                                            which preserves the original 2-char ">>" text, then ALSO
                                            appended a new 1-char literal `>` OP token -- duplicating
                                            a character on the very first format pass (e.g.
                                            `has_eol_rule<Input> >` collapsed to `has_eol_rule<Input>>>`,
                                            3 close characters instead of 2), breaking compilation of
                                            an otherwise-untouched `template<...>` forward-declaration
                                            prefix. Fixed by giving the retyped ANGLE_BRACKET_CLOSE
                                            token its own explicit 1-char ">" text instead of reusing
                                            the original 2-char token via `retype()`.

  real_code_regressions_29_inp/out.java  -- Java, real-code test against local `anemonesoft`
                                            candidate (HelpBox.java/Spreadsheet.java):
                                            MiscRule.renderCallCandidate's containsNewline branch
                                            calls groupByOriginalLine, which tracks only paren/
                                            bracket/angle-bracket depth, not brace depth -- once a
                                            multi-argument call's trailing argument is itself a
                                            multi-line brace body (e.g. `new Timer(0, new
                                            ActionListener() { ...multi-statement... })`), every
                                            line inside that body (having no top-level comma to
                                            split on) gets silently swallowed into a single row and
                                            rendered via one `collapseTokensToOneLine` call with no
                                            line-length check, producing an unboundedly long output
                                            line whose later re-formatting (Java's Allman brace pass
                                            reacting to the now-multi-line body) made the bug visible
                                            as an idempotency failure. Fixed by widening an existing
                                            Kotlin-only "leave such an argument untouched" bail to
                                            all languages, using a new `containsInternalNewline`
                                            check (newline strictly between an argument's own first
                                            and last significant token, not merely in its leading
                                            formatting gap) so `real_code_regressions_1`'s
                                            single-line `{ ret, level1(ret) }` brace argument is
                                            unaffected.

  real_code_regressions_30_inp/out.kt    -- Kotlin, real-code test against `square/okio`: three
                                            co-occurring, unrelated bugs. (1)
                                            KotlinDeclarationAlignmentRule.renderTokens had no
                                            notion of unary vs. binary `-`/`+`, unconditionally
                                            inserting a space between a leading unary minus and its
                                            operand (`val x = -1` -> `= - 1`); fixed with a new
                                            isUnaryMinusOperand lookback. (2)
                                            ScopePipeline.applySignaturePass's Kotlin `:
                                            ReturnType` tail detection matched a top-level `)` with
                                            no check that the span to the next `{` stays within one
                                            statement, so a headerless multiplatform declaration
                                            followed by a blank line and an unrelated later
                                            declaration got merged into one bogus signature+tail;
                                            fixed with a new hasTopLevelBlankLine guard. (3)
                                            BlockStructureRule's braceless if/while/for collapse
                                            (both tryCollapse and tryCollapseBraceless) rendered
                                            the condition prefix with the original source's
                                            keyword-to-paren space still present, one character
                                            wider than the final tightened form a later
                                            MiscRule.TIGHT_PAREN_KEYWORDS pass produces --
                                            enforceCallLineBreaking's length check measured the
                                            stale, wider text and over-wrapped a line that fits
                                            exactly at the line-length limit in its true final
                                            width, an idempotency bug; fixed by tightening the
                                            keyword-to-paren space at collapse time in both
                                            methods.

  real_code_regressions_31_inp/out.kt    -- Kotlin, two more compile-breaking bugs found via
                                            `kotlinc` compile-checking against `square/okio` (not
                                            caught by round1-vs-round2 diffing, since both are
                                            broken consistently from the first pass). (1)
                                            TokenizerCore.MULTI_CHAR_OPS was entirely missing
                                            Kotlin's `===`/`!==` referential equality operators --
                                            `next !== this` lexed as separate `!=`/`=` tokens,
                                            later re-spaced into the invalid `!= =`; fixed by
                                            adding `===`/`!==` ahead of their 2-char prefixes. (2)
                                            BlockStructureRule's braceless-collapse dispatch
                                            treated a do-while's trailing `while (cond)` (no `{`
                                            after its `)`) the same as a genuine loop-starting
                                            `while`, fusing the following unrelated statement onto
                                            the same line with no separator; fixed with a new
                                            isDoWhileTailKeyword lookback that bails the collapse
                                            for that shape.

  real_code_regressions_32_inp/out.kt    -- Kotlin, real-code test against `square/kotlinpoet`: a
                                            nested `when { ... }` used as a `when` branch's own
                                            body (`is Class<*> ->` followed by `when { ... }` on
                                            the next line in the original source) round1-vs-round2
                                            flapped its closing `}`'s indentation between two
                                            different columns. Root cause: ScopePipeline's Kotlin
                                            braceLineIndent anchors a scope's indent on the
                                            physical line its opening `{` sits on at the moment
                                            ScopePipeline runs (Phase 0) -- but
                                            KotlinSpecificRule.formatWhenExpressions' later
                                            arrow-alignment pass (Phase 4) merges the branch's
                                            label line and the nested `when {` onto one physical
                                            line whenever it fits (always true here, since the
                                            body's own first line is just `{`). A fresh format
                                            computes the indent against the pre-merge line; a
                                            reformat of already-merged output computes it against
                                            the post-merge line -- disagreeing. Fixed with a new
                                            findMergingWhenBranchLineStart lookahead in
                                            ScopePipeline.braceLineIndent that detects this exact
                                            shape (a lone `when {` line immediately preceded by a
                                            line ending in a top-level `->`) and anchors on the
                                            eventual post-merge line up front, Kotlin-gated.

  real_code_regressions_33_inp/out.kt    -- Kotlin, real-code test against `square/kotlinpoet`:
                                            a real, first-pass, compile-breaking bug found via
                                            `kotlinc` syntax-checking (not merely idempotency
                                            diffing) -- an expression-bodied function whose body is
                                            itself a trailing-lambda call (`fun addTypes(...): T =
                                            apply { ... } as T`) had `apply`'s own unrelated `{`
                                            wrongly Allman-converted as if it were the function's
                                            own body brace, splitting `apply` from `{ ... }` across
                                            lines with no valid Kotlin grammar joining them. Root
                                            cause: `KotlinSpecificRule.findSignatureCloseParenBeforeBrace`'s
                                            backward scan for a `: ReturnType` clause before a
                                            candidate body `{` had no bail-out on an intervening
                                            depth-0 `=` -- it kept scanning past `apply`/`=` and
                                            found the function's own real `: T` further back,
                                            wrongly treating the unrelated nested `{` as this
                                            function's own body brace. Fixed with a new bail-out
                                            when a depth-0 `=` is encountered before any `:`.

  real_code_regressions_34_inp/out.hpp   -- C++, real-code test against `NVIDIA/stdexec`:
                                            combines two distinct bugs found while formatting
                                            `include/`. (1) A C++20 requires-expression
                                            compound-requirement (`requires { { expr } ->
                                            Concept; }`) had its inner `}` -- immediately followed
                                            by `->`, never by `;` -- misidentified by
                                            `ScopePipeline.splitTopLevelSpans` as a genuine
                                            scope-closing brace, since the existing
                                            `isScopeOpeningBrace` disambiguation only guarded the
                                            `;`-followed case; this mis-recursed into the
                                            compound-requirement as if it were its own child
                                            scope, corrupting indentation non-idempotently
                                            (round1 != round2). Fixed by also checking for an
                                            immediately-following `->` and never splitting a span
                                            there. (2) A real, first-pass, compile-breaking bug
                                            found via `g++ -fsyntax-only`: two semicolon-less
                                            macro-invocation "statements" (no trailing `;`)
                                            immediately preceding a `#if ... #endif` pair caused
                                            `DeclarationAlignmentRule.splitStatements` to never
                                            close the current statement before reaching the `#if`,
                                            folding the directive into the *middle* of the next
                                            real statement's token list -- invisible to
                                            `parseDeclaration`'s field-based reconstruction, which
                                            silently drops any token it doesn't recognize as part
                                            of a declaration's shape. The `#if` vanished on
                                            output while its paired `#endif` (now leading the next
                                            statement) survived, producing an `#endif without #if`
                                            error and 150+ cascading downstream errors in any file
                                            that includes the corrupted header. Fixed by adding a
                                            depth-0 check in `splitStatements` that always closes
                                            out any accumulated statement and starts a fresh one
                                            when a `PREPROCESSOR`/`MACRO_DEF` token is reached,
                                            consistent with `hasCommentBefore`'s existing
                                            leading-directive handling.

  real_code_regressions_35_inp/out.hpp   -- C++, real-code test against `NVIDIA/stdexec`
                                            (continuing the candidate above): a real,
                                            first-pass, compile-breaking bug ("Bug 3" in
                                            STATE_C_CPP_JAVA.md's stdexec entry) found via
                                            `g++ -fsyntax-only` in `__detail/__counting_scopes.hpp`'s
                                            `__base_scope::try_join`. `BlockStructureRule.
                                            tryCollapse` (the STYLE.md §10 single-statement
                                            `if(cond) body;` collapse) builds its collapsed
                                            condition text via `renderInline`, which flattens
                                            every whitespace/newline gap to a single space with
                                            no awareness that a `//` line comment consumes the
                                            rest of its original physical line -- so once an
                                            `if`'s multi-line condition containing a trailing `//`
                                            comment between arguments (here, two `compare_
                                            exchange_weak(...)` calls) got flattened, every token
                                            that followed the comment in the source (the
                                            remaining call arguments, the closing `)`, `return
                                            true;`, and the enclosing `}`) was silently absorbed
                                            into that one comment and vanished from the output,
                                            producing a 50-error `expected '}' at end of input`
                                            cascade. Fixed by adding a `containsLineComment` check
                                            in `tryCollapse` that refuses the collapse (leaves the
                                            original braced, multi-line `if` untouched) whenever
                                            its condition span carries a `COMMENT_LINE` token;
                                            block comments (`/* ... */`, which don't extend to
                                            end-of-line) remain safe to flatten as before.

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
