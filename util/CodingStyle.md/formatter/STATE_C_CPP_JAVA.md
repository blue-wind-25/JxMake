# STATE_C_CPP_JAVA.md — C/C++/Java Formatter Implementation Tracker

Read `STATE_COMMON.md` first — shared commit/ambiguity/testing conventions this file
assumes. `STATE_KOTLIN.md` is a separate job's file, not required reading here.

---

## Project Layout

```
util/CodingStyle.md/formatter/
  STATE_C_CPP_JAVA.md        ← this file
  RDD_LOG.md           ← full Resolved Design Decisions text (do not read in full)
  STATE_NEXT_AI.md           ← deferred AI-assist design and NOT FEASIBLE rationale
  README.md
  Makefile
  LICENSE
  src/
    com/jxmake/formatter/
      Main.java
      Config.java
      ServerMode.java
      Formatter.java
      IndentationDetector.java
      ScopePipeline.java
      tokenizer/
        TokenizerCore.java
      grid/
        ColumnGrid.java
        ModifierPriority.java
        CppModifierPriority.java
        JavaModifierPriority.java
      evaluator/
        ComplexityPaddingEvaluator.java
      rules/
        DeclarationAlignmentRule.java
        BlockStructureRule.java
        SwitchRule.java
        GetterSetterRule.java
        MiscRule.java
        CppSpecificRule.java
        JavaSpecificRule.java
```

---

## Resolved Design Decisions

Lookup convention in `STATE_COMMON.md`. Index below (topic only, full text in `RDD_LOG.md`):

| Key | Topic |
|---|---|
| RDD_KEY_1 | Tokenizer |
| RDD_KEY_2 | Rule engine |
| RDD_KEY_3 | Shared grid |
| RDD_KEY_4 | Modifier priority |
| RDD_KEY_5 | Constants |
| RDD_KEY_6 | Java parsing |
| RDD_KEY_7 | AI dependency |
| RDD_KEY_8 | JAR target |
| RDD_KEY_9 | Server mode |
| RDD_KEY_10 | Server idempotency |
| RDD_KEY_11 | Port |
| RDD_KEY_12 | Path separator |
| RDD_KEY_13 | Lockfile location |
| RDD_KEY_14 | Line endings |
| RDD_KEY_15 | Config precedence |
| RDD_KEY_16 | `.jxmake-code-formatter` inheritance |
| RDD_KEY_17 | Multi-module Java imports |
| RDD_KEY_18 | Windows support |
| RDD_KEY_19 | Output modes |
| RDD_KEY_20 | Build |
| RDD_KEY_21 | `ColumnGrid` flush API |
| RDD_KEY_22 | §3.1 complexity padding algorithm |
| RDD_KEY_23 | Declaration-statement detection |
| RDD_KEY_24 | Column grid rendering |
| RDD_KEY_25 | Static reorder vs. STYLE.md §5's worked example |
| RDD_KEY_26 | §10 Single-expression block eligibility |
| RDD_KEY_27 | §11 K&R brace style detection |
| RDD_KEY_28 | §11 lambda bodies also use K&R |
| RDD_KEY_29 | §12 else/else-if placement |
| RDD_KEY_30 | C/C++ bitfield column (`STYLE_C_CPP.md` §6) |
| RDD_KEY_31 | §7 closing comments — key variable on nesting |
| RDD_KEY_32 | §7 closing comments — engine structure |
| RDD_KEY_33 | §7 closing comments — named-construct blank lines |
| RDD_KEY_34 | §13 non-inline case brace wrapping |
| RDD_KEY_35 | §13 nested switch processing order |
| RDD_KEY_36 | §13 inline switch row classification |
| RDD_KEY_37 | §13 fallthrough marking |
| RDD_KEY_38 | §14 getter/setter rendering |
| RDD_KEY_39 | §14 getter/setter group detection |
| RDD_KEY_40 | §3.2 keyword spacing |
| RDD_KEY_41 | §3.3 initializer brace spacing |
| RDD_KEY_42 | §4 pre-increment rewrite |
| RDD_KEY_43 | §1 indentation scope |
| RDD_KEY_44 | §6 grouping and rendering |
| RDD_KEY_45 | §8 signature scope and rendering |
| RDD_KEY_46 | §9 function-body detection and return scoping |
| RDD_KEY_47 | §15 comment scope and sentence detection |
| RDD_KEY_48 | §15 partial-implementation split |
| RDD_KEY_49 | §15 multi-line block comment banner reformatting |
| RDD_KEY_50 | §15 separator alignment |
| RDD_KEY_51 | §6 multi-line right-hand sides |
| RDD_KEY_52 | §1 empty parameter list (`CppSpecificRule.java`) |
| RDD_KEY_53 | §2 one-liner scope (`CppSpecificRule.java`) |
| RDD_KEY_54 | §9 section dividers are non-actionable |
| RDD_KEY_55 | §4 pointer/const spacing already satisfied |
| RDD_KEY_56 | §3 template angle-bracket spacing (`CppSpecificRule.java`) |
| RDD_KEY_57 | §10 header file structure (`CppSpecificRule.java`) |
| RDD_KEY_58 | §11 dropped from `CppSpecificRule.java` scope |
| RDD_KEY_59 | `JavaSpecificRule.java` scoping |
| RDD_KEY_60 | §2 Allman-conversion vs. getter/setter one-liner groups -- left unguarded |
| RDD_KEY_61 | §3.1 condition-interior padding -- wiring decision |
| RDD_KEY_62 | §3.1 condition-interior padding -- implementation |
| RDD_KEY_63 | §2 method-definition Allman conversion (`JavaSpecificRule.java`) |
| RDD_KEY_64 | §4 array-declaration syntax parenthetical -- non-actionable |
| RDD_KEY_65 | §7 import group order/count contradiction |
| RDD_KEY_66 | `Main.java` orchestration architecture |
| RDD_KEY_67 | STYLE.md §5/§6 scope -- anywhere in code, recursively |
| RDD_KEY_68 | `DeclarationAlignmentRule.splitStatements` depth-awareness fix |
| RDD_KEY_69 | §7 import ordering implementation (`JavaSpecificRule.java`) |
| RDD_KEY_70 | `Config.java` file format |
| RDD_KEY_71 | `Config.java` resolution scope |
| RDD_KEY_72 | `Formatter.java` orchestration architecture |
| RDD_KEY_73 | `ServerMode.java` wire protocol |
| RDD_KEY_74 | `Formatter.java` whole-file pass order |
| RDD_KEY_75 | Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient |
| RDD_KEY_76 | `DeclarationAlignmentRule` misparses a bare `++j;`/`--j;` statement as a fake field declaration |
| RDD_KEY_77 | `MiscRule.enforceCommentStyle` relied on pipeline ordering (not detection) to skip closing-comment labels, breaking idempotency |
| RDD_KEY_78 | `ScopePipeline.splitTopLevelSpans` never closed a span at a C++ access-specifier label, merging it into the following member |
| RDD_KEY_79 | `IndentationDetector.java` design (`indent-style = auto`) |
| RDD_KEY_80 | `ServerMode.java` idempotency check on a Java 8 build target -- `ProcessHandle` via reflection |
| RDD_KEY_81 | Allman-brace render-loop infinite loop when `)`/`{` are already adjacent (`CppSpecificRule.java`/`JavaSpecificRule.java`) |
| RDD_KEY_82 | Phase ordering reversed -- `Main.java`/`README.md`/dogfood test deferred until after Phase 2 |
| RDD_KEY_83 | `JavaModifierPriority` column order for `abstract`/`sealed`/`non-sealed`/`final`/`volatile` -- declaration-kind-specific orderings merged into one map |
| RDD_KEY_84 | `record` named-construct detection through component list / `implements` clause / compact constructor |
| RDD_KEY_85 | C++ concepts/`requires` clause implementation in `CppSpecificRule.java` |
| RDD_KEY_86 | `MiscRule.java` call/declaration line-breaking architecture -- option 2 must bypass `parseSignature`, option 1 reuses it + new `renderDropped` |
| RDD_KEY_87 | `MiscRule.enforceCallLineBreaking` implementation scope decisions (nesting, comment bail-out, call-vs-declaration classification, new preserve-groups grid) + `collapseTokensToOneLine` bugfix |
| RDD_KEY_88 | `Main.java` implementation (Step 1.5) -- CLI parsing, config resolution, indent-style temp-cache, server auto-connect/delegate, `--server`/`--stop`, output modes, exit codes |
| RDD_KEY_89 | `combined_inp.java` -- §15 consecutive-`//`-comment grouping, enum constant-list `;` separation, `throws`-clause function-body detection |
| RDD_KEY_90 | Task A (`JXM_CFMT_DIS`/`ENA`) -- rejected split-file-into-tmp-dirs approach in favor of in-memory token masking |
| RDD_KEY_167 | `JXM_CFMT_CFG` top-of-file placement semantics -- own separate comment required, "before first non-comment/non-blank token" not literal line 1 |
| RDD_KEY_168 | `in_file_config_*.hpp` fixture -- `header-guard-rename` untestable via this harness (guard name derives from invocation path, `_inp`/`_out` always differ); swapped for `format-macros=off`, which also proves override of the `test` target's own `FORMAT_MACROS=on` env var |
| RDD_KEY_169 | range-v3 item 20 bug (a) RESOLVED -- `BlockStructureRule.enforceKAndRBraceStyle` glued a named construct's `{` onto a preceding bare `#endif` line, which a later retokenize then swallowed whole into the `#endif` PREPROCESSOR token, permanently dropping that brace from every downstream scope-depth/frame-stack pass and desyncing both the closing-comment indentation and (as a downstream side effect, not a separate bug) angle-bracket classification; fixed by skipping the K&R glue when the preceding real token is a PREPROCESSOR directive |
| RDD_KEY_170 | microsoft/proxy dogfood: 3 bugs in `CppSpecificRule.enforceRequiresClausePlacement` -- (a)/(b) baseIndent/fit-check derived from the trailing `requires` clause's unstable-across-passes closing-paren line instead of the parameter list's own opening-paren line (with chained-specifier unwinding for `noexcept(...)`); (c) a preprocessor directive inside the clause's own constraint expression got spliced mid-line, producing invalid C++ -- fixed by leaving any clause containing a `PREPROCESSOR` token untouched |

---

## Open Questions

- **range-v3 real-code-testing item 20, bug (a): RESOLVED.** Idempotency divergence in
  `utility/any.hpp`, `iterator/common_iterator.hpp`, `meta.hpp` (closing-brace-comment
  indentation + nested angle-bracket spacing not converging round1-vs-round2). Root cause:
  `BlockStructureRule.enforceKAndRBraceStyle` glued a named construct's `{` onto a preceding
  bare `#endif` line, which a later retokenize swallowed whole into the `#endif` PREPROCESSOR
  token, desyncing brace-depth tracking downstream. Fixed by skipping the K&R glue when the
  preceding real token is a PREPROCESSOR directive. Verified: minimal repros, all 3 real files
  byte-identical round1/round2, full 311-file tree round1/round2 clean, `make test` 75/75 both
  ways, `g++ -fsyntax-only` matching baseline error count. Full root-cause narrative and the
  refuted/untested alternative hypotheses explored along the way: `RDD_KEY_169` in `RDD_LOG.md`.

- **Separate, unconfirmed observation — still OPEN, do not conflate with the above:** some
  already-passing test fixtures reportedly fail syntax-check under `clang` in C++23 mode while
  passing under `gcc 12` in C++20 mode. May be a real language-version mismatch in the fixture's
  source, or may indicate the formatter's output triggers stricter-parser-only diagnostics.
  Needs its own investigation; not yet linked to bug (a) above.

---

## Config Keys and Defaults

See `STATE_COMMON.md` → **Config Keys and Defaults** (moved there since the
table is shared across all four supported languages, not C/C++/Java-specific).

---

## Java File Header

Every `.java` source file must begin with this copyright block, before the `package` declaration:

```java
/*
 * Copyright (C) 2022-2026 Aloysius Indrayanto
 *
 * This file is part of the JxMake build system and is distributed under the MIT License.
 * See the LICENSE file in the formatter root directory for the full MIT license text.
 */
```

## Java Coding Convention — `final` Locals and Parameters

Mark every local variable and method/constructor parameter `final` whenever it is
never reassigned after its initial assignment (i.e., whenever the compiler would
accept `final` there). Applies to all `.java` files under `src/`.

---

## Checklist — Phase 3

**Step 1 — Deterministic extensions (complete):**

**Step 1.5 — Dogfood checkpoint (in progress):**

**Critical rules for this step:**
- The user may name a specific `*_inp.*` file to run next — do not assume sequential
  order; run only the named file unless told to run all remaining.
- Run test files one at a time. After each file (including the self-dogfood pass, i.e.
  formatting the formatter's own source), if output doesn't match `*_out` (or produces
  unexpected changes), **stop and ask the user** before attempting any fix — the mismatch
  may be a hand-authored error in the `*_out`/expectation, not necessarily a formatter bug.
- After each file test — pass or fail — update the checklist item inline with `(PASS)`,
  `(FAIL)`, or `(SKIP)` and commit immediately (no batching), so no progress is lost.
- Never remove `[x]`/`(PASS)` entries, even once all tests pass — a later fix could
  regress a previously-passing file, and the user may ask to re-run any entry at any time.
- Apply STATE_COMMON.md's "evidence over reasoning" rule strictly here, to limit quota
  usage and avoid regressing `(PASS)` tests/prior fixes.

`Main.java` standalone-mode cache note: `IndentationDetector` results are cached at
`/tmp/jxmake-code-formatter-indent-<sha256-of-boundary-dir>.cache`, content = detected style + `\n`
+ boundary dir `lastModified` epoch ms; invalidated automatically on an mtime mismatch
(RDD_KEY_88).

- [x] CLI arg parsing (`--server`, `--stop`, `--standalone`, `--diff`, `--check`,
      `--out DIR`, `--port N`, file paths); unknown flags / bad usage → exit 2 (RDD_KEY_88)
- [x] `--lang c|cpp|java` (2026-07-06): explicit language override for files whose
      extension `inferLanguage` can't recognize; one flag per invocation (no per-file
      override), validated against exactly `c`/`cpp`/`java` (exit 2 otherwise), threaded
      through `processFile` ahead of the extension-based `inferLanguage` fallback;
      `--server`/`--stop` reject `--lang`. The `/format` HTTP endpoint already accepted an
      optional `lang` query param taking priority over its own path-extension guess
      (`Main.delegateToServer` already sent it), so no protocol change was needed — only
      server-side validation added (`ServerMode.FormatHandler` now 400s on an unrecognized
      `lang`). `README.md` updated. `make test` 25/25, no regressions.
- [x] Four output modes: in-place (default), `--diff` (self-written unified diff,
      single hunk with clamped context), `--check`, `--out DIR` (RDD_KEY_88)
- [x] Exit codes: 0 = success/no changes, 1 = would-change (`--check`) or formatting
      error, 2 = usage error (RDD_KEY_88)
- [x] `README.md` update for Phase 1 + Phase 2 (added `auto` to `indent-style`
      comment; all other Phase 1+2 items already present)

All file-pair tests below PASS (forward + idempotency), zero known regressions. Full
bug-by-bug root-cause narratives have been compacted out of this file — available via
`git log`/`git show` on the commits noted per entry.

- [x] File-pair test: `h_core_inp.h` → diff vs `h_core_out.h` (PASS)
- [x] File-pair test: `c_core_inp.c` → diff vs `c_core_out.c` (PASS)
- [x] File-pair test: `hpp_core_inp.hpp` → diff vs `hpp_core_out.hpp` (PASS)
- [x] File-pair test: `cpp_core_inp.cpp` → diff vs `cpp_core_out.cpp` (PASS) — 6 bugs fixed
  (named-construct/attribute/modifier detection, `::`-qualified spacing, constructor Allman
  close-paren, getter/setter padding, trailing-return-type, if/else-if chain collapsing).
- [x] File-pair test: `java_core_inp.java` → diff vs `java_core_out.java` (PASS) — 8 bugs fixed
  (header-spacing, `this`/`super` as LHS, stale closing-comment, `throws` Allman conversion,
  inline-switch padding, `catch`/`finally` placement, `@Annotation` skip).
- [x] File-pair test: `cpp_modern_inp.cpp` → diff vs `cpp_modern_out.cpp` (PASS) — 11 bugs fixed
  (comment no-capitalize set, named-construct one-liners, `template`/`requires` signatures,
  operator-overload detection, coroutine promise_type grouping, brace-init/structured-binding
  spacing, tight cast-template brackets, namespace closing-comment chain) + an idempotency fix
  (column padding double-counted as indentation on a second pass).
- [x] File-pair test: `java_modern_inp.java` → diff vs `java_modern_out.java` (PASS) — 5 bugs
  fixed (empty named-construct bodies, one-liner-call getter/setter rejection, multi-statement
  one-liners left unsplit, RDD_KEY_75 adjacency heuristic removed, blank line before leading
  comment).
- [x] File-pair test: `combined_inp.h` → diff vs `combined_out.h` (PASS) — 3 bugs fixed
  (`format-macros` alignment, `extern "C"` fixture correction, enum-alias closing comment +
  `#if`-guard depth). Committed as `efeb6df`.
- [x] File-pair test: `combined_inp.c` → diff vs `combined_out.c` (PASS) — 4 bugs fixed (struct
  member indentation strip safety, flat brace-aggregate initializers + C-style cast rejoining,
  parameter inline comments preserved, switch closing-comment idempotency).
- [x] File-pair test: `combined_inp.hpp` → diff vs `combined_out.hpp` (PASS) — 2 bugs fixed
  (constructor/destructor/operator-overload one-liner exclusion, `template<...>` prefix
  recognition); 1 not feasible (mid-word-dot vs. sentence-period needs semantic understanding —
  Tier-3 AI-assist candidate in `STATE_NEXT_AI.md`, fixture hand-edited instead).
- [x] File-pair test: `combined_inp.cpp` → diff vs `combined_out.cpp` (PASS) — 4 bugs fixed
  (destructor `~` vs. return type, class-template member tight brackets + layout,
  structured-binding misparse, trailing comment duplication on group-gap trim).
- [x] File-pair test: `combined_inp.java` → diff vs `combined_out.java` (PASS) — 3 bugs fixed
  (multi-line `//` prose period-stripping, enum constant-list `;` detached, blank line before
  final `return` in a `throws`-clause method).
- [x] File-pair test: `c_comments_inp.c` → diff vs `c_comments_out.c` (PASS) — 6 bugs fixed
  (mid-param `//` comment reattachment fixing a brace-depth desync, compound-assignment
  misparse, one-param-per-line padding, `hasCommentBefore` group-break guard, last-param
  comment alignment, `#define` trailing-comment capitalization). One fixture-only correction.
- [x] File-pair test: `cpp_comments_inp.cpp` → diff vs `cpp_comments_out.cpp` (PASS) — 5 bugs
  fixed (forward-declaration comment isolation, stale-closing-comment guard narrowing,
  namespace-body/template-argument/keyword spacing, `requires`-clause signature pull, blank
  line after trailing-comment member, structured-binding/template-argument comment spacing).
- [x] File-pair test: `java_comments_inp.java` → diff vs `java_comments_out.java` (PASS) — 4 bugs
  fixed (multi-line param comment reattachment, per-language no-capitalize keyword sets,
  switch-case comment blank-line preservation, flat-aggregate per-element comments untouched).

**If any file-pair test above shows a mismatch: stop, report the full diff to the user, and
wait for instruction. Do not fix either the formatter or the `*_out` file without explicit
user direction — `*_out` files are hand-authored and may themselves contain errors.**

**After all file-pair tests above pass (or are resolved — ask the user first):**
- [x] Dogfood self-format pass: run formatter on all `src/**/*.java`, write
      to `target/dogfood-src/`
- [x] Dogfood self-format compile: `javac` the `target/dogfood-src/` tree;
      must compile with zero errors — first run surfaced a real compile-breaking bug (see
      "Other findings outside the candidate list" below), now fixed; verified clean compile
      after the fix.
- [~] Dogfood self-format idempotency / declaration count: superseded by the real-code
      testing approach below, which found and fixed the actual bugs underlying this failure.
      Not re-run standalone against `target/dogfood-src/` since; if revisited, expect it to be
      much closer to passing given the pass-ordering fix, but there may be other Java-only
      convergence bugs the C++ testing below wouldn't have exercised.

**Real-code testing (pivoted from synthetic dogfooding — found bugs faster):** see
STATE_COMMON.md's "Real-code testing methodology" for the repeatable round1/round2/compile
recipe and fixture-registration convention. Full bug-by-bug root-cause narratives for
completed candidates have been compacted out of this file into the "Finished" list below —
still available via `git log`/`git show` on the noted commits/fixtures.

**Tools/compiler used**
(1) `gcc -std=gnu99 -fsyntax-only <file>.c` (used for `tongsuo-mini`)
(2) `g++ -std=c++20 -fsyntax-only <file>` — usually `/opt/gcc-12.2.0/bin/g++`; PEGTL,
    stdexec, and mp11 additionally need `LD_LIBRARY_PATH=/opt/isl-0.16.1/lib` with this
    toolchain
(3) `clang++ -std=c++23 -fsyntax-only <file>.cpp` (with/without `-stdlib=libc++`) at
    `~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++` — pipe stderr through
    `grep -v 'no version information available'` to filter a harmless libstdc++
    symbol-versioning warning (not a compile error); `/opt/glibc-2.41/` is available if a
    genuine glibc-mismatch/patchelf issue is ever hit with some other prebuilt binary
(4) `javac` — installs used so far: `/opt/openjdk-25_linux-x64_bin`,
    `/opt/openjdk-21_linux-x64_bin/jdk-21/bin/javac -d ... -cp . --release 8` (matches a
    project's own JDK8-source/JDK21-`javac` Makefile convention)
(5) `pcpp-java-1.30.jar` (JxMake's own C-preprocessor-for-Java tool) —
    `java -jar pcpp-java-1.30.jar <input> -o <output>`; compare token streams before/after
    format with `#line` directives stripped first (they legitimately shift with line-count
    changes); plain `gcc -E`/`cpp` does NOT work as a substitute (hard-errors on real `##`
    token-pasting tricks)
(6) `java_sc` — AST-based syntax-only checker. Used when a full javac is not wanted/needed
    (dependency problem) — catches parse errors only, weaker confidence than (4) (no semantic/type
    checking). Build/run commands:
```bash
JDK=/opt/openjdk-21_linux-x64_bin/jdk-21
cd ~/Projects/JxMake/0_excluded_directory/personal/SyntaxChecker
"$JDK/bin/javac" java_sc.java
"$JDK/bin/java" java_sc <file.kt>
```

**Finished dogfood / real-code testing** (one line each; full narratives via `git log`/`git show`
on the noted commits/fixtures)
(1) `blake-madden/tinyexpr-plusplus` (C++20) — 3 bugs, `MiscRule` multi-line call/decl
    rendering + pass ordering. Verified (2). Fixture: `real_code_regressions_1`.
(2) RobotCoding `gui_frontend` (71 `.java`) — 4 pass-ordering idempotency bugs (`>>>`
    mistokenization, getter/setter padding order). Verified (4). Fixture: `real_code_regressions_2`.
(3) Self-dogfood (formatter's own `src/`, 20 files) — 1 pass-ordering bug in
    `MiscRule.parseAssignment`. Verified (4). Fixture: `real_code_regressions_3`.
(4) `martinus/nanobench` (`nanobench.h` as `.hpp`) — 2 bugs: raw-string-literal tokenizer gap,
    dropped `template<...>` on forward decl. Verified (2). Fixture: `real_code_regressions_4`.
(5) User-reported `} // while` indentation bug — `ScopePipeline` closing-brace gap not
    re-derived from depth. Verified (2). Fixture: `real_code_regressions_5`.
(6) Local `pcpp_java` tool (41 `.java`) — 2 idempotency bugs: `SwitchRule` inline-alignment
    overflow, one-liner raw-newline misfire. Verified (4). Fixtures: `_9`, `_10`.
(7) C17 `Tongsuo-Project/tongsuo-mini` (56 files) — 1 bug: unbounded flat aggregate-init line
    length, no `lineLengthLimit` check. Verified (1). Fixture: `real_code_regressions_11`.
(8) C++20 `serge-sans-paille/frozen` (44 `.hpp` + `catch.hpp`) — 10 bugs across
    `ScopePipeline`/`DeclarationAlignmentRule`/`TokenizerCore`/`CppSpecificRule` (parent-indent,
    struct depth, brace-init, getter padding, K&R/Allman flapping, backslash-continued
    preprocessor corruption, namespace indent fallback, ObjC/attribute mistokenization). Verified
    via idempotency (156-file tree clean). Fixtures: `_12`–`_16`.
(9) C++20 `fmtlib/fmt` — idempotent at default; at real 2-space convention found
    `SwitchRule.deriveUnit`'s hardcoded 4-space fallback. No-op at default, no fixture.
(10) C++20 `taocpp/PEGTL` (355 `.hpp`) — 1 bug: `reclassifyAngleBrackets`'s `>>`-split
     duplicated a char via `retype()`. Verified (2). Fixture: `real_code_regressions_28`. Also a
     no-op found (`normalizeIndent` non-declaration rounding gap, invisible at default indent).
(11) C++17/20 `foonathan/lexy` (121 `.hpp`) — no bug found, idempotent at default. Verified (2)
     on all 9 examples.
(12) C++20 `NVIDIA/stdexec` (192 files) — 4 bugs across 3 sessions: requires-expression `}`
     misidentified as scope-close; `#if`/`#endif` guard dropped by `splitStatements`;
     `tryCollapse` absorbing text past a `//` comment; already-collapsed one-liner misparsed as
     declaration. Verified (2). Fixtures: `_34`, `_35`, `_36`.
(13) C++11 `boostorg/mp11` (34 `.hpp`) — no bug found, idempotent at default. Verified (2).
(14) C++23 `basvas-jkj/cpp_modules` (7 files) — no bug found, idempotent. Verified (3)
     (pre-existing environment failures only, identical before/after).
(15) `google/google-java-format` (84 `.java`) — 3 bugs: `ensureBlankLineInGap` splitting a
     trailing comment (fixture `_6`); `Doc.java` divergence resolved by the config-key wiring
     audit below (no code change); `applyArrowAlignment` joining arrow-case with no
     line-length check (fixture `_7`); `findNameBeforeParen` misparsing `case`/`default` arrow
     arms as one-liner members (fixture `_8`). Verified (4).
(16) MEDIUM `javaparser/javaparser` — see "In progress dogfood / real-code testing details"
     below: 4 bugs fixed/committed (all 4 originally-tracked bugs resolved); full-tree
     re-verification found 26 more previously-uncatalogued idempotency diffs, not yet
     root-caused -- still IN PROGRESS, not moved to Finished.
(17) HUGE `openrewrite/rewrite` — see "Not started" below (queued, not started).
(18) Local `VMA-GIT/anemonesoft/` (82 `.java`) — 1 bug: `renderCallCandidate` swallowed a
     multi-line brace-bodied trailing argument (brace depth not tracked). Verified (4).
     Fixture: `real_code_regressions_29`.
(19) Local `ARMCortexMThumbC.java.in` (PCPP template) — no bug found; verified (5), 0-line
     token-stream diff.
(20) C++20 `ericniebler/range-v3` (311 `.hpp`) — 2 compile-breaking bugs from its
     concept-emulation-macro convention: `template(...)` macro wrongly pulled onto a
     declarator line (fixed by gating on `<`); `CPP_ret(void)(...)` mis-rewritten to
     `CPP_ret()(...)`, deleting the macro's real argument. Verified (2) + full-tree idempotency.
     Fixture: `real_code_regressions_50`. Follow-up bug (b): a multi-line `//`-banner-commented
     deletion declaration got collapsed by the function-pointer-detection branch misfiring on
     the macro-call shape; fixed with a narrow `COMMENT_LINE`-scan guard local to that branch.
     Verified (2) + `make test` 70/70 + full 318-file tree idempotency. Fixture: `_51`. Bug (a)
     (the range-v3-item-20 idempotency bug) tracked and resolved separately — see Open Questions
     / `RDD_KEY_169`.
(21) C++20 `boost-ext/ut` (44 files) — 1 idempotency bug: a deduction-guide statement's
     close-paren misidentified by `findCloseParenBeforeTrailingReturnType`'s backward scan as
     an unrelated following struct's "function close paren" (scan didn't stop at a depth-0 `;`).
     Verified with minimal repro, `make test` 72/72, full-tree idempotency, and (2)/(3) compile
     checks matching baseline. Fixture: `real_code_regressions_52`.
(22) C++20/23 `microsoft/proxy` (28 `.h`/`.cpp`, `include/proxy/{,v4/}proxy{,_macros,_fmt}.h`
     + `tests/`/`benchmarks/`/`tools/`) — 3 bugs in `CppSpecificRule.enforceRequiresClausePlacement`
     (trailing `requires` clause after a wrapped multi-line parameter list): (a)/(b) unstable
     baseIndent/fit-check derived from the closing-paren's own physical line (varies across
     passes and across chained `noexcept(...)` specifiers) instead of the parameter list's real
     opening-paren line; (c) a preprocessor directive inside the clause's own constraint
     expression got spliced mid-line by `collapseToOneLine`, producing invalid C++ (compile-
     breaking, not just cosmetic) — fixed by leaving any clause containing a `PREPROCESSOR` token
     untouched. Verified with `clang++ -std=c++23 -stdlib=libc++ -fsyntax-only` (0-error baseline
     unchanged on `include/proxy/{,v4/}proxy.h` before/after), full round1/round2 idempotency over
     the whole tree, and `make test` 77/77. Fixture: `real_code_regressions_53`. RDD_KEY_170.

**Not started dogfood / real-code testing**
(2) `github.com/microsoft/STL` — Microsoft's `std::` implementation; large, best raw grammar
    coverage on the list but high testing-time cost; planned as one of the last picked up.
    Would verify with (2)/(3) (or newer, bump toolchain version if needed). (NOT STARTED)
(3) `github.com/llvm/llvm-project` — LLVM/Clang monorepo; enormous, likely only a
    partial/targeted subtree run is practical (e.g. `clang/lib/Format/` or
    `llvm/include/llvm/ADT/`). Try to exercise C++23 features specifically. Would verify with
    (2)/(3). (NOT STARTED)
(4) `github.com/gcc-mirror/gcc` — GCC monorepo; similarly enormous, and GCC's own source may
    target an older/conservative C++ dialect in parts (bootstrapping), so may exercise less
    modern-C++ surface than its size suggests — lowest priority of the four for
    modern-feature testing specifically. Try to exercise C++23 features specifically. Would
    verify with (2)/(3). (NOT STARTED)
(6) HUGE `github.com/openrewrite/rewrite` — large multi-module AST-rewrite engine; low
    priority given size, pick up once smaller candidates are exhausted. Likely some
    annotation-processor-generated/Lombok-style code (`AI_PREAMBLE`-adjacent gaps). Would
    verify with (4). (NOT STARTED)

Priority order for the C/C++ queue above unless the user redirects:
`STL` → `llvm-project` → `gcc-mirror` (`mp11`/`lexy`/`stdexec`/`range-v3`/`boost-ext/ut`/
`microsoft/proxy` already DONE, see "Finished" above — `mp11` was smallest/narrowest, `lexy` next for touching
operator overloading/concepts/CRTP/dense declaration-alignment in one small tree, `stdexec` for
concepts/`requires`/deep metaprogramming, `range-v3` for its own distinct
`template(...)`/`CPP_ret`-style concept-emulation-macro convention). For any C/C++ candidate
distributed under a `.h`/`.hpp` extension, confirm which language it actually is before testing —
copy to `.hpp` first if really C++.

**In progress dogfood / real-code testing details**

(15b) MEDIUM `javaparser/javaparser` (1997 `.java` files across 7 modules) — IN PROGRESS,
resumed session found 2 more bugs beyond what's fixed so far and left 3-4 open (see below).
Cloned to a scratch checkout under `/tmp/claude-1000/.../scratchpad/javaparser`. Round1/round2
methodology used full in-place-copy formatting (NOT `--out DIR`, which flattens all files to
basenames in one flat directory and silently collides same-named files across modules — 1997
files became 1558 unique basenames; confirmed via file-count drop, not itself a formatter bug,
just a testing-harness gotcha worth remembering for future large multi-module candidates).

Fixed and committed (`4bcd56d`): (a) `GetterSetterRule.parseOneLinerMember` misparsed a
braceless `if (cond) throw new X(...)`/`if (cond) return ...` statement as a one-liner
getter/setter-style member (same misparse class as the existing `case`/`default` guard --
`findNameBeforeParen` landed on the exception/call's own name, treating the `if (...) ... throw
new` prefix as a bogus "return type"), grid-aligning garbage padding that grew unboundedly
across passes. Fixed by rejecting any "return type" span containing a control-flow/statement
keyword. (b) `MiscRule.stripSoleTrailingPeriod`/`stripSoleTrailingPeriodAcrossLines` stripped a
comment's sole trailing `.` but left the whitespace that had separated it from the preceding
word, a stray trailing space only caught by an unrelated trim on the next pass -- fixed to trim
trailing whitespace in the same step. Fixture: `real_code_regressions_54_{inp,out}.java`. `make
test` 78/78.

Fixed and committed (bug 4 of the 4 originally-open items, this session): `TypeExtractor.java`
(both the `javaparser-symbol-solver-core` and `javaparser-symbol-solver-testing` copies) — a
blank line just before a block-closing `}` present in round1 was dropped in round2.
Root-caused to `ScopePipeline.processScope`'s trailing-gap force-reindent: it always collapsed
a span's trailing gap to exactly one newline once `effectiveSpanIndent` was text-derivable, and
`findParentIndent` only returns a text-derivable indent for a statement anchored on a bare
`else`/`catch`/`finally` once that keyword sits on its own physical line (Allman) — it returns
`null` (skip force-reindent, gap left untouched) while `else` still shares a line with the
preceding `}` (K&R). A fresh format sees K&R and skips the force-reindent (blank line survives
by luck); every reformat after the first sees the now-Allman `else` (real indent, force-reindent
fires, blank line dropped) — an idempotency bug. Fixed by counting the trailing whitespace run's
own newline count (new `trailingRunNewlineCount` helper) and replaying that many newlines in the
force-reindent instead of always forcing exactly one. Verified: minimal repro, both real
`TypeExtractor.java` copies round1/round2 byte-identical, full `make test` 79/79. Fixture:
`real_code_regressions_55_{inp,out}.java`.

Fixed and committed (bug 2 of the 4 originally-open items, this session): `CsmAttribute.java`'s
`getTokenType` — an already-one-line `if (GeneratedJavaParserConstants.tokenImage[i].equals(
expectedImage)) return i;` inside a `switch`-`case` block stayed one line on round1 but got
wrapped across 3 lines by `enforceCallLineBreaking` on round2. Root-caused to pass ordering in
`Formatter.formatOne`'s Phase 1: `MiscRule.enforceCallLineBreaking`'s "does this call fit in
`lineLengthLimit`" measurement ran BEFORE `SwitchRule.formatNonInlineSwitches` reindents
switch-case bodies one level deeper. On a fresh format, the call's line measured "fits" against
the pre-reindent (shallower) column and was left alone; `formatNonInlineSwitches` then
reindented the surrounding `case` body one level deeper, pushing the *rendered* line past the
limit with no further re-check. On a reformat, the input already has the deeper indent baked in
from the first run, so the same fits-check correctly measures it as too long and wraps it —
round1 != round2. This is the same pass-ordering bug class already documented at length in
`Formatter.java`'s own comments around `enforceComplexityPadding`/`enforceCallLineBreaking`
(the tinyexpr-plusplus-discovered case). Fixed by re-running `enforceCallLineBreaking` again
immediately after `formatNonInlineSwitches` (idempotent: a no-op on anything already correctly
fitting or wrapped), so the fits-vs-wraps decision is re-derived against the now-final column.
Verified: minimal repro (fails idempotency without the fix, passes with it), real
`CsmAttribute.java` round1/round2 byte-identical, both `TypeExtractor.java` copies still
round1/round2 byte-identical (no regression on bug 4's fix), full `make test` 80/80. Fixture:
`real_code_regressions_56_{inp,out}.java`.

**Both previously-OPEN bugs now FIXED and committed this session** (fixtures
`real_code_regressions_57`/`_58`):

- `Java1_0Validator.java`/`Java5Validator.java`: a space between a braceless `if(...)`'s closing
  paren and its single-statement body (`reporter.report(...)`) was present after round1 but
  missing after round2. Root-caused to `DeclarationAlignmentRule.isCStyleCastClose`: its guard
  against misreading a call/subscript's `)` as a C-style cast close only excluded a preceding
  IDENTIFIER/`)`/`]`, not control-flow keywords -- `if`/`while`/`for`/`switch`/`catch`/`do`/`else`
  are KEYWORD tokens and slipped past, so `if(node instanceof RecordPatternExpr)` (an
  IDENTIFIER/KEYWORD-only condition) got misclassified as a cast `(Type)`, suppressing the space
  before the following statement. Only surfaced when the whole braceless-if-plus-call construct
  was itself rendered as a declaration's initializer via `renderInitTokens` (e.g. a lambda
  argument passed to a field initializer's constructor call) -- this is why a minimal repro
  without the surrounding field-declaration context didn't reproduce it in the prior session.
  Fixed by adding a `CONTROL_FLOW_KEYWORDS` exclusion set. Verified: minimal repro (`test/
  real_code_regressions_57_{inp,out}.java`), both real `Java1_0Validator.java`/`Java5Validator.java`
  round1/round2 byte-identical, `make test` 82/82.
- `JavaParserJsonSerializer.java`: an enum constant list's indentation/alignment column differed
  between round1 (10-space struct, `final String` left-padded to a wide column) and round2
  (12-space, unpadded). Two compounding root causes: (a) `DeclarationAlignmentRule.groupDeclarations`
  had no Java-enum-constant-list exclusion, so a constant list like `BEGIN_TOKEN("beginToken"),
  END_TOKEN("endToken");` (same top-level shape as a C-style multi-declarator statement, just with
  no leading type) could merge into the same alignment group as an unrelated adjacent field (the
  enum's own trailing `final String propertyKey;`), grid-padding a bogus wide column into that
  field only on a fresh parse -- a reformat of the already-separated output no longer merged the
  groups, so the padding vanished (idempotency bug). (b) `JavaSpecificRule
  .findEnumConstantListTerminators` derived its re-emitted indent from the first constant-list
  member's own *current* line indent (`lineIndentAt`) rather than an absolute depth-based
  recompute, so each pass's drift compounded onto the next (8 -> 10 -> 12 -> ...). Fixed by (a) a
  new `isJavaEnumConstantListShape` helper isolating a Java enum-constant-list statement into its
  own singleton group in `groupDeclarations` (same isolation pattern already used for
  `isMemberFunctionForwardDecl`, preserving the constant list's own existing one-line-when-it-fits
  collapsing behavior, unlike an earlier attempt that outright rejected it from `parseDeclaration`
  and broke that collapsing -- caught by `make test` regressions on `test/java_core_inp.java`/
  `test/java_combined_inp.java`, reverted in favor of this singleton-group approach), and (b)
  deriving `findEnumConstantListTerminators`'s indent from the enum body's own stable `{`-line
  indent plus one indent unit instead of the first member's own line indent. Verified: minimal
  repro (`test/real_code_regressions_58_{inp,out}.java`), real `JavaParserJsonSerializer.java`
  round1/round2 byte-identical, `make test` 82/82.

**Full-tree re-verification run this session (after both fixes): NOT clean.** Re-ran round1/round2
over the whole 1997-file tree (in-place-copy methodology, same as before) -- both previously-open
bugs' own files (`Java1_0Validator.java`, `Java5Validator.java`, `JavaParserJsonSerializer.java`)
are now byte-identical round1 vs round2 (confirmed via direct diff), but the full-tree diff still
shows **26 other files differing**, none of which are the 3 files above -- these are additional,
previously-uncatalogued idempotency bugs beyond the original 4 (the original "42 files differed"
count was never fully individually catalogued, only sampled). Notable pattern: several of the 26
are literal duplicate copies of the same source files vendored twice under
`javaparser-symbol-solver-testing/src/test/test_sourcecode/{javaparser_new_src,javaparser_src/
proper_source}/.../{MethodDeclaration,TypeDeclaration,CloneVisitor,ModifierVisitorAdapter}.java`
etc. -- since the two copies differ from each other in small ways (different javaparser vintage),
this doesn't necessarily mean 26 independent bugs, quite possibly fewer distinct root causes
hitting a shared code shape across duplicated files. Full file list captured in this session's
scratch dir (`fulltree_r1`/`fulltree_r2` under the scratchpad) but not preserved long-term; a
future session should re-run the round1/round2 diff fresh rather than rely on this note. Since the
full-tree diff is not clean, per the task's own criterion this candidate **stays "In progress",
not "Finished"** -- `javac` compile-check was NOT run (gated on a clean idempotency diff first,
per the standard real-code-testing methodology order). Next session should either (a) treat this
as good enough given the two originally-tracked bugs are resolved and move to Finished with a
"known residual idempotency gaps, tracked separately" caveat if the user agrees that's acceptable,
or (b) continue root-causing the 26 remaining files (sample a few representative ones first, e.g.
one from each vendored-copy pair, since they likely collapse to a handful of distinct bugs) before
re-attempting the full clean-tree + `javac` verification and moving to Finished.

*(`stdexec`, `mp11` reached DONE with no open gaps.)*

When a test completes, remove/compact its entry from "Not started" (or its "In progress"
detail block here) and add it to "Finished dogfood / real-code testing" above — and to
"Tools/compiler used" too, if it introduces a genuinely new tool not already listed there.

**Other findings outside the candidate list**

**Config-key wiring audit (2026-07-06)** — root-caused `Doc.java`'s divergence to
`MiscRule.INDENT_WIDTH`/`LINE_LENGTH_LIMIT` being dead `static final` constants disconnected
from `Config`; only `line-length`/`indent-size` were unwired, every other key already correct.
Fixed by converting to instance fields threaded through the relevant rule constructors. No-op
at default settings; verified live at `indent-size = 2`.

**Follow-up (same day):** several rule classes also carried their own hardcoded
`DEFAULT_INDENT_UNIT = "    "` fallback (same bug class) — fixed in `MiscRule`, `JavaSpecificRule`,
`CppSpecificRule`. No-op at default `indent-size`.

**Removal (same day):** `header-guard-style` removed entirely (silently-dead config surface)
from `Config.java`, `README.md`, and this file's sample config.

**Dogfood-compile-check bug** (predates the round1/round2 methodology; referenced by Step A's
"Dogfood self-format compile" item above): `MiscRule`'s call/declaration preserve-group
renderers reset paren/bracket/angle depth to 0 at each physical line start, corrupting
multi-line nested calls (including the formatter's own `TokenizerCore.java`). Fixed with a
`groupByOriginalLine` helper tracking depth cumulatively across the whole slice.

**Known pre-existing gaps** (discovered during `Main.java` smoke-testing, left unfixed as
out of scope, flagged to user): `ServerMode.FormatHandler` doesn't resolve
`indent-style = auto` before calling `Formatter.formatOne` (masked in practice by `Main`'s
fallback-to-standalone-on-delegation-failure behavior); `Config.lineEndings()` is applied by
`Main.applyLineEndings()` for standalone/in-process formatting but not yet by
`ServerMode.FormatHandler`. Full detail: RDD_KEY_88.

**Step 2 — AI integration: NOT FEASIBLE (deferred) — see `STATE_NEXT_AI.md`.**

---

## Known Gaps — Fixed

Previously-recorded low-priority gaps, now resolved. One-line summaries only — full
before/after detail available via `git log`/`git show`.

- **`* const` cosmetic gap in mixed declaration groups** (`DeclarationAlignmentRule`) — FIXED.
  `splitCppType` now always returns `postConst = ""`, folding the whole type+star+const text
  into one uniformly-padded column. East-const (`char const*`) intentionally not normalized.
- **`typedef` declarations not aligned** — FIXED. `typedef` added as a rank-0
  `CppModifierPriority` keyword so it parses through the normal declaration path.
- **Direct function-pointer declarations not aligned** — FIXED. `parseDeclaration` now detects
  `Type (*name)(params)` directly and folds `(*name)` into the name cell (including
  multi-star `(**cb)`, via `Token.isRepOp`).
- **`#ifdef`/`#elif`/`#else`/`#endif` interleaved with declarations dropped every branch but
  the first** — FIXED, affects C/C++ (not Java-specific despite being found while
  implementing Task C below). `hasCommentBefore`'s group-break guard didn't check for
  `PREPROCESSOR`/`MACRO_DEF` tokens, so a directive mid-group never forced a group boundary
  and got silently discarded by `render(group)`. Fixed by adding those token types to the same
  guard.
- **`using` alias declarations not aligned — NOT SCHEDULED (design decision)**. Inverted
  grammar (`using Foo = Type;`) doesn't fit the existing `typeTokens`/`name` model; passes
  through unchanged (no corruption), not a bug. If picked up later: align at `=`, needs its
  own parsing branch and column layout keyed on `=` position.
- **Preprocessor directive glued onto a following Java method definition** — FIXED,
  genuinely Java-specific (C++'s `applySignaturePass` branch incidentally routes around it via
  a separate line-rescan). `leadStart`/`sigLeadStart` landed directly on a leading
  `PREPROCESSOR`/`MACRO_DEF` token instead of skipping past it, dropping the directive's text
  from the preserved leading gap. Fixed by walking forward past any leading directive run
  before computing the signature's real first token. Fixture:
  `test/java_preprocessor_method_inp/out.java`.

---

## TODO — All Tasks DONE

Implementation order used: C, B, D, E, A (F folded into each as it landed, plus a final F
sweep after A) — smallest/lowest-risk first, the large cross-cutting frozen-span feature (A)
last. One-line summaries below; full detail via `git log`/`git show`.

### A — Enable/disable formatting via markers/CLI flag (DONE)
`Token.frozen` + `TokenizerCore.markFrozenSpans` (scans `//% JXM_CFMT_DIS`/`ENA` and block
equivalents, in-memory token masking not fragment/tmp-dir splitting — RDD_KEY_90); every rule
class given a frozen-span guard; `--format-off` CLI flag. Two bugs fixed while adding the
`format_toggle_inp/out.java` fixture (leading-gap vs. first-real-token frozen check;
child-scope re-tokenize losing frozen state, fixed via a `startFrozen` param). `README.md`
documents the marker syntax. `make test` 16/16 PASS.

### B — New config entries: `normalize-comment-start-case`/`normalize-comment-end-period` (DONE)
Two new `Config.java` keys (default `on`/`on`), following the `format-macros` pattern.
`make test` 15/15 PASS.

### C — Don't damage C-preprocessor macros embedded in Java source (DONE)
`TokenizerCore.isPreprocessorLanguage()` now returns `true` unconditionally, so `#`-directives
in `.java` lex as opaque tokens like C/C++. Surfaced two pre-existing bugs, fixed — see "Known
Gaps — Fixed" above. `make test` 15/15 PASS.

### D — Multi-file smoke test + benchmarking (DONE)
New `bench` Makefile target times all-at-once vs. one-by-one formatting in both standalone and
client-server mode.

### E — Code cleanups (DONE)
New `Lang` class centralizes `isC`/`isCpp`/`isJava` per file (replacing scattered string
checks); five null-safe token helpers centralized on `TokenizerCore.Token`, duplicate copies
removed from rule classes.

### F — Add more tests (DONE)
`test/c_cpp_decl_gaps_inp/out.c` added, covering the three `DeclarationAlignmentRule` fixes
under "Known Gaps — Fixed". `make test` 17/17 PASS.

### G — Verify `AI_PREAMBLE_FULL.md`'s `### Edge Case` sections against actual JAR behavior (DONE)
Both edge cases (`else`/`else if` closing comments; `type* const` in a mixed declaration group)
confirmed already correctly handled by the JAR via live `--standalone` runs, not genuine gaps.
Both sections removed from `AI_PREAMBLE_FULL.md` as redundant.

### H — Comment-grammar classifier accuracy upgrade (DONE)
Formerly its own `STATE_COMMENT_GRAMMAR.md` (deleted once complete — full design history in
`RDD_LOG.md`'s `RDD_KEY_94`–`RDD_KEY_98`). Adds an optional classifier-backed decision path for
the comment-normalization keys behind a new `comment-normalization-classifier` config key
(default `off`, zero behavior change when off). New `com.jxmake.formatter.classifier` package
(feature extraction, non-Latin-script gate, keyword-ambiguity gate, weighted
`YES`/`NO`/`ABSTAIN` classifier — never guesses). Weights derived from 40 labeled examples under
`cwg/` (`cwg/derive_weights.py`, see `cwg/README.md`/`cwg/weights.md`). `make test` 70/70 PASS
unchanged (default `off`); classifier `on` verified via smoke test, 39/40 on the labeled set.

**TODO (still open):** the 40-example `cwg/` set is synthetic; growing it with real comments
would firm up weight magnitudes and might surface new feature-set gaps. Independent of the
formatter's own `make test` suite — classifier defaults to `off`, ships no runtime AI
dependency; this is about `cwg/`'s example quality only.

### I — C/C++/Java braceless else-if chain collapse + alignment (DONE)
Extended Kotlin's braceless if/else-if/else collapse + column alignment (RDD_KEY_124/127/128)
to C/C++/Java, only when every branch in the chain qualifies (RDD_KEY_129) — a mixed/braced
chain is left untouched. New `BlockStructureRule.chainAllBranchesCollapsible` +
`alignBracelessElseIfChain` (moved from `KotlinSpecificRule.java` into the shared rule class,
runs for all languages from `Formatter.java` Phase 4). Two follow-up defects fixed immediately
after (RDD_KEY_130): an idempotency bug in the render loop (leading-space stripping grew
alignment spacing each pass) and a K&R `} else` collapsing the whole chain onto one line
instead of Allman-style column alignment.

`make test` full suite green after RDD_KEY_129 and RDD_KEY_130. Fixtures updated:
`test/c_combined_out.c`, `test/cpp_modern_out.cpp`, `test/java_combined_out.java`,
`test/java_core_out.java`, `test/java_modern_out.java`,
`test/real_code_regressions_15_out.hpp`.
