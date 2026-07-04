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

  java_comments_inp/out.java -- Uncommon comment placements: // and /* */ between
                                annotations and declarations, inside method signatures,
                                inside if/for/switch headers, between else and brace,
                                trailing on array initializers, multi-line block
                                comments inside methods.

  format_toggle_inp/out.java -- JXM_CFMT_DIS/JXM_CFMT_ENA formatting-toggle markers
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

  combined_inp/out.java      -- All of the above in one realistic file: sealed class
                                with nested record, enum, inner classes; switch
                                expressions; pattern matching; text blocks; var;
                                getter/setter groups; import ordering.

C:
  c_core_inp/out.c           -- C11 constructs: declaration alignment, bitfields,
                                pointer placement, struct/enum/typedef, function
                                Allman braces, control-flow K&R, pre-increment,
                                static reordering, assignment alignment.

  c_comments_inp/out.c       -- Uncommon comment placements in C: inside struct,
                                between params, inside if/for headers, between else
                                and brace, divider normalization, comments on macros.

  c_cpp_decl_gaps_inp/out.c  -- Regression coverage for three DeclarationAlignmentRule
                                fixes (STATE.md "Known Gaps -- Fixed"): the `* const`
                                column gap in mixed pointer-star groups, `typedef`
                                joining and aligning with a surrounding plain-variable
                                group, and direct function-pointer declarations
                                (including multi-star `(**cb)`) joining a group.

  combined_inp/out.c         -- All C constructs together in one realistic file:
                                macros, enums with closing comments, structs, forward
                                declarations, global state alignment, public API,
                                internal functions, inline comments.

C++:
  cpp_core_inp/out.cpp       -- C++11/14 constructs: class with access specifiers,
                                template class, lambdas, auto return type, initializer
                                list constructors, getter/setter groups, extern "C".

  cpp_modern_inp/out.cpp     -- C++17/20/23 constructs: structured bindings,
                                init-statement if/switch, concepts/requires,
                                consteval/constinit, operator<=>, coroutines
                                (co_yield/co_return).

  cpp_comments_inp/out.cpp   -- Uncommon comment placements in C++: inside template
                                parameter lists, inside concept requires expressions,
                                between class specifier and base, inside function
                                params, inside structured bindings, inside requires
                                clauses.

  combined_inp/out.cpp       -- All C++ constructs together: concepts, enum class,
                                template class with nested Config struct, structured
                                bindings, init-statement if/switch, consteval/constinit,
                                operator<=>, lambda with auto return, extern "C",
                                trailing comments on declarations.

Headers:
  h_core_inp/out.h           -- C header with #ifndef guard: header zone spacing,
                                include ordering (angle vs quote), struct alignment,
                                pointer declarations, #ifdef __cplusplus extern "C".

  hpp_core_inp/out.hpp       -- C++ header with #pragma once: pragma once zone
                                spacing, concepts, enum class, structs with
                                operator<=>, abstract class interface, concrete
                                derived classes.

  combined_inp/out.h         -- Combined C header: guard zones, macros alignment,
                                named enum/struct with closing comments, full API
                                declaration group, extern "C".

  combined_inp/out.hpp       -- Combined C++ header: pragma once zones, concepts,
                                template base class, concrete subclass, factory
                                declaration, extern "C" block.


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
