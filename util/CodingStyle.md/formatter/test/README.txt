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

  real_code_regressions_17_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's RobotTcpSession.kt: a
                                            compile-breaking bug, not just an idempotency mismatch.
                                            enforceCallLineBreaking's Option 2
                                            (renderCallPreserveGroups) groups a multi-line call's
                                            arguments by original source line, not by argument --
                                            when one sibling argument is itself a multi-line brace
                                            body (a trailing lambda, `Thread({ ... }, "name")`),
                                            every line inside that body became its own row and got
                                            collapsed, and since Kotlin has no `;` to separate
                                            statements the way C/C++/Java do, this silently merged
                                            separate statements onto one line with no separator
                                            between them, producing invalid Kotlin. Fixed by bailing
                                            (leaving the call untouched) when, for Kotlin only, a
                                            top-level argument contains both a newline and a `{` --
                                            C/C++/Java brace-bodied multi-line arguments (e.g. an
                                            initializer list) are unaffected since `;` still
                                            disambiguates statement boundaries there.

  real_code_regressions_18_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's PlayMusicBlock.kt: another
                                            idempotency bug, this time in
                                            KotlinDeclarationAlignmentRule.spansMultipleLines (§6
                                            declaration alignment). A braceless `if(cond) expr else
                                            expr` initializer short enough to stay on one line
                                            groups and column-aligns normally with an adjacent
                                            `val` sibling on a fresh format -- but once its own
                                            nested call gets wrapped across lines by
                                            MiscRule.enforceCallLineBreaking (statement too long to
                                            fit), a *second* pass saw those wrapping newlines and
                                            wrongly treated the initializer as a genuine multi-line
                                            block expression, bailing it out of its alignment group
                                            -- shrinking the sibling `val`'s own column padding on
                                            every successive pass. Fixed with paren/brace-depth-
                                            aware newline tracking (mirroring
                                            ScopePipeline.hasTopLevelNewline's own "ignore newlines
                                            inside a call's parens" idiom): a newline strictly
                                            inside a call's parens with no enclosing brace no
                                            longer counts as "multi-line", but a newline inside an
                                            actual `{`...`}` block/lambda body still does (needed
                                            to avoid reintroducing the RDD_KEY_134 compile-breaking
                                            bug for `{`-bodied trailing-lambda arguments).

  real_code_regressions_19_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's MainActivity.kt: a closing-brace
                                            indentation drift bug in ScopePipeline.processScope. A
                                            trailing lambda argument's `{` can sit on a continuation
                                            line of a multi-line fluent chain
                                            (`.setPositiveButton("Ok") {`), deeper than the chain's
                                            own first line (`AlertDialog.Builder(this)`).
                                            processScope derived the lambda body's indent, and its
                                            closing `}`'s alignment, from the whole statement's
                                            first line (via findParentIndent, needed elsewhere for
                                            e.g. `case 1:` labels) instead of the brace's own
                                            physical line -- under-indenting the body by one level
                                            and misplacing a nested if/else block's closing braces
                                            to match, even on a fresh format. Fixed (Kotlin-only,
                                            via a new ScopePipeline.braceLineIndent helper) by using
                                            the brace's own physical-line indent, when deeper than
                                            the statement-start indent, for the child body's
                                            inherited indent and its closing-brace placement.

  real_code_regressions_20_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's ToolbarActions.kt/
                                            MainViewModel.kt (surfaced once RDD_KEY_136 stopped
                                            masking it): a val whose initializer is a parenthesized
                                            if/else expression (`(if (cond) a else b)`), immediately
                                            followed by another statement in the same scope, was
                                            fused onto that following statement's line with no
                                            separator at all -- invalid Kotlin.
                                            BlockStructureRule.collapseSingleExpressionBlocks has no
                                            notion of expression- vs statement-position `if`/`else`;
                                            it fired on the expression-position `if` (which has no
                                            braced body, since it's a value expression) and treated
                                            everything up to and past the wrapping `)` as if it were
                                            a braceless statement body, swallowing the newline
                                            before the next statement. Fixed (Kotlin-only) by
                                            tracking a running unmatched-paren depth and refusing to
                                            treat `if`/bare `else` as a collapsible statement while
                                            inside one -- a statement-position `if`/`else` is never
                                            itself nested inside a paren this same pass didn't open
                                            and fully consume via its own condition matching.

  real_code_regressions_21_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's BlockCanvasView.kt: a `val`
                                            declaration whose initializer contains a logical-AND
                                            expression lost the space before `&&`, rendering it
                                            flush against the preceding token (e.g. `a > 1&& b`).
                                            Root cause: DeclarationAlignmentRule.isTightToken's
                                            `Token.isRepOp(t, '*') || Token.isRepOp(t, '&')` check
                                            -- meant for C/C++'s repeated pointer/reference
                                            declarator sigils (`**`, `&&` as an rvalue-reference
                                            type) -- matches ANY token consisting solely of `&`
                                            characters, including Kotlin's `&&` logical-AND
                                            operator, which Kotlin has no unary/repeated `*`/`&`
                                            construct to be confused with. Fixed by gating both
                                            checks to non-Kotlin languages, mirroring the identical
                                            gate MiscRule.isTightToken already had for the same
                                            reason.

  real_code_regressions_22_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's BlockPalette.kt: a run of
                                            adjacent §9 expression-bodied one-liner functions, one
                                            of which has a body long enough that a later phase
                                            (MiscRule.enforceCallLineBreaking) wraps its call
                                            across multiple lines once column-padding is added. On
                                            a fresh format the run's column width was computed
                                            from every member's original (still short) text,
                                            including the long one's -- only for that later
                                            wrapping phase to break it afterward, leaving the
                                            group's padding stale. Reformatting that already-
                                            wrapped output then correctly excluded the now-multi-
                                            line member via `hasNewlineBetween`, splitting the run
                                            into different subgroups with different (narrower)
                                            column widths on the second pass -- an idempotency
                                            flap. This exact bug class was already fixed for the
                                            C/C++/Java base class
                                            (GetterSetterRule.parseOneLinerMember's own length
                                            pre-check) but never ported to the Kotlin sibling
                                            method. Fixed by adding the same `hasBreakableCall` +
                                            estimated-width pre-check to
                                            KotlinGetterSetterRule.parseKotlinOneLinerMember,
                                            excluding a too-long member from the group on the very
                                            first pass too, so the decision stays stable across
                                            repeated formats.

  real_code_regressions_23_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's BlockPalette.kt: an `override fun
                                            draw(...)` method body inside an anonymous `object :
                                            Block() { ... }` whose single call is short enough that
                                            the whole `{ ... }` body still fits on one physical line
                                            pre-formatting, but is long enough (at this method's real
                                            indentation depth) that a later phase
                                            (MiscRule.enforceCallLineBreaking) wraps the call across
                                            multiple lines anyway. On a fresh format,
                                            KotlinSpecificRule.isSingleLineBody saw the body still on
                                            one physical line (pre-wrap) and kept `{` K&R inline;
                                            only the later call-wrapping phase split it internally,
                                            leaving the K&R brace stale. Reformatting that already-
                                            wrapped output then correctly saw a genuinely multi-line
                                            body and moved `{` to Allman -- an idempotency flap
                                            identical in shape to the one already fixed on
                                            JavaSpecificRule.isSingleLineBody (never previously
                                            ported to this Kotlin sibling method) and to
                                            KotlinGetterSetterRule.parseKotlinOneLinerMember
                                            (RDD_KEY_139). Fixed by adding the same
                                            `hasBreakableCall` + estimated-width pre-check to
                                            KotlinSpecificRule.isSingleLineBody, this time also
                                            correcting the estimated-width formula (ported from
                                            JavaSpecificRule) to include indentation and inter-token
                                            spacing, which the original formula omitted -- a
                                            shortfall too small to matter at JavaSpecificRule's
                                            shallower nesting depths, but large enough at this
                                            Kotlin method's deeper (anonymous-object-nested)
                                            indentation to produce a false "fits" verdict without
                                            the fix.

  real_code_regressions_24_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's ConnectTypeDialog.kt and
                                            WifiApDialog.kt (`.show().also { ... }`):
                                            ScopePipeline.applySignaturePass's Kotlin `:
                                            ReturnType` tail handling (STYLE_KOTLIN.md §9) uses
                                            findLastTopLevelCloseParen to locate the parameter
                                            list's own closing paren when it isn't the token
                                            immediately before the body's `{` -- but that helper
                                            only scanned for the LAST depth-0 `)` in range, with no
                                            check that a genuine `:` actually follows it before the
                                            brace. A fluent chain of the shape `x.foo().bar { ... }`
                                            -- where the FIRST call has empty parens and the SECOND
                                            call uses Kotlin's bare/parenless trailing-lambda call
                                            syntax -- has exactly this token shape (`)` ...
                                            IDENTIFIER `{`) with no `:` at all, so `foo()`'s `)` was
                                            wrongly accepted as if it were a signature's parameter
                                            list and `bar` as if it were a return-type tail, causing
                                            `.bar` to be silently deleted from the rendered output
                                            on the very first format pass (not merely an idempotency
                                            flap -- first-pass wrong/uncompilable output). Fixed by
                                            requiring a top-level `:` immediately after the found
                                            `)` (via nextSignificantIndex) before accepting the
                                            Kotlin return-type-tail branch in applySignaturePass;
                                            otherwise bail (continue) rather than misdetect.

  real_code_regressions_25_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's BlockCanvasView.kt (`class
                                            BlockCanvasView @JvmOverloads constructor(`) and
                                            ToolbarActions.kt (a second adjacent `@Volatile private
                                            var` declaration): KotlinSpecificRule.
                                            enforceLabeledJumpSpacing's state machine, meant to
                                            detect Kotlin's `label@` loop-label declaration syntax
                                            (STYLE_KOTLIN.md §11), had no way to tell a genuine
                                            label apart from an unrelated `@Annotation` sitting
                                            right after some other identifier (a class name, or an
                                            enum constant ending the previous statement) --
                                            silently corrupting `@JvmOverloads`/`@Volatile` into
                                            `@ JvmOverloads`/`@ Volatile`, a parse error on the very
                                            first format pass. Fixed by adding a lookahead,
                                            isLoopLabelTarget, requiring the token after `@` to
                                            actually be `for`/`while`/`do` or `{` (a labeled lambda
                                            literal) -- the only constructs a Kotlin label can
                                            legally prefix -- before treating an `IDENTIFIER @`
                                            sequence as a label declaration, both for the state
                                            transition and for the tightBeforeAt spacing decision
                                            itself.

  real_code_regressions_26_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's Optimizer.kt: a `when`
                                            expression's `else -> { ... }` branch with a
                                            multi-statement block body was flattened onto a single
                                            line with no `;` separators between statements -- a
                                            parse error on the very first format pass. Root cause:
                                            BlockStructureRule.collapseSingleExpressionBlocks's
                                            bare-`else` handling (meant for a real `if`/`else`
                                            chain's braceless single-statement body, STYLE.md §10)
                                            matched any KEYWORD "else" not immediately followed by
                                            "if" or "{" -- which also matches a `when` arm's `else`
                                            label, since its body is introduced by `->`, not a
                                            brace. Fixed by also checking whether the token after
                                            `else` is `->`, and bailing out of the
                                            braceless-collapse path in that case.

  real_code_regressions_27_inp/out.kt    -- Found via Kotlin dogfood-testing against RobotCoding
                                            gui_frontend_android's ProgramBuilder.kt: two separate,
                                            unrelated bugs co-occurring in the same statement. (1)
                                            DeclarationAlignmentRule.needsSpaceBetween had no case
                                            for Kotlin's `!is`/`!in` negated type-check/containment
                                            operators (a single tight lexical unit with no space),
                                            so the generic KEYWORD-gets-a-leading-space default
                                            corrupted `!is`/`!in` into `! is`/`! in`. (2)
                                            MiscRule.enforceCallLineBreaking's renderCallCandidate
                                            used parseSignature (a C/C++/Java-style "type name"
                                            declaration parser) to distinguish a forward
                                            declaration's parameter list from a plain call's
                                            argument list -- but Kotlin has no such shape at all, so
                                            parseParam's generic heuristic misparsed the call
                                            argument `it.func.funcName` (no top-level comma) as a
                                            `Type name` pair, inserting a spurious space once the
                                            call needed line-wrapping and corrupting it into
                                            `it.func. funcName`. Fixed by forcing Kotlin's
                                            render-path selection through the untyped call-argument
                                            path via a separate `sigForRender` variable, while
                                            keeping the original `sig`-driven zero-param bail-out
                                            intact (an initial attempt that gated `sig` itself to
                                            null broke fixture 22's zero-param declaration).


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
