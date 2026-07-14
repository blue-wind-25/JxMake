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

---

## Open Questions

- **range-v3 real-code-testing item 20, bug (a):** idempotency divergence found in
  `utility/any.hpp`, `iterator/common_iterator.hpp`, and (newly confirmed in a follow-up
  full-tree round1/round2 verification pass) `meta.hpp` — none of these reproduce in any
  minimal repro attempted (only the full real files trigger it, suggesting a cumulative/
  stateful cross-declaration-group interaction). Two symptoms seen: (1) a nested
  template-argument angle bracket (`meta::if_c<std::is_reference<T>() || copyable<T>, T>`)
  fails to converge tight/loose spacing between round1 and round2; (2) a closing-brace-plus-
  trailing-comment line (e.g. `}; // namespace ranges`, `}; // struct partition_`,
  `}; // namespace detail`) renders at a different indentation level between round1 and
  round2. Root-cause lead for (1): `TokenizerCore.reclassifyAngleBrackets`/
  `isGenericSafeToken`'s forward-scanning stack calls `invalidateAll(openStack)` (poisoning
  all currently-open angle-bracket-stack entries) whenever a non-"generic-safe" token is seen
  while the stack is non-empty; `||`/`&&`/`!` are not in `isGenericSafeToken`'s OP whitelist,
  so a boolean trait expression like `std::is_reference<T>() || copyable<T>` inside a
  `meta::if_c<...>` non-type template argument poisons the outer `<...>` pair. Tried: adding
  `||`/`&&`/`!` to `isGenericSafeToken`'s OP case — rebuilt and ran `make test` (70/70 forward
  + 70/70 idempotency, no regressions to existing fixtures), but this did NOT fix the
  divergence — it only changed which round showed tight vs. loose spacing, round1 still != round2.
  Reverted (`git checkout -- src/com/jxmake/formatter/tokenizer/TokenizerCore.java`) rather than
  leave an unproven, only-partially-effective change in a correctness-sensitive tokenizer
  heuristic. Symptom (2) (the brace/comment indentation divergence) has not yet been
  root-caused at all. Per the ambiguity-handling protocol, stopping here rather than guessing
  further — needs a fresh investigation session (likely tracing how angle-bracket
  classification and brace/scope-depth tracking each depend on retokenized-text state that
  itself differs between a first format and a reformat-of-already-formatted-output, which is
  the general shape both symptoms share).

  **Untested alternative hypothesis (raised in a later session, not yet checked):** the
  formatter tracks brace/paren/angle-bracket depth from raw token text and does not run a real
  preprocessor — `PREPROCESSOR` is its own skipped token type, branches are never selected. A
  file with `#if`/`#ifdef`/`#else` blocks whose branches are NOT independently brace-balanced
  (only balanced once a preprocessor picks one branch) would look genuinely inconsistent to a
  linear depth-tracker scanning through both branches — which could produce exactly this
  "cumulative, doesn't reproduce in a minimal repro" signature, since the confusion only
  compounds once enough real `#if` blocks accumulate before the failing construct. This would
  reframe the issue as a documented limitation ("assumes textually balanced braces per file,
  does not preprocess"), not a tokenizer/rule defect. Cheap to check before further
  instrumentation: inspect whether `any.hpp`/`common_iterator.hpp`/`meta.hpp` contain
  `#if`-family blocks with asymmetric brace nesting across branches, and whether normalizing
  those away (e.g. picking one branch, or running through a real preprocessor first) makes the
  divergence disappear.

  **Separate, unconfirmed observation to verify independently — do not conflate with the above:**
  some already-passing test fixtures reportedly fail syntax-check under `clang` in C++23 mode
  while passing under `gcc 12` in C++20 mode. This may be a real language-version mismatch in
  the fixture's source (version-gated syntax, e.g. concepts/reflection) unrelated to the
  formatter, or may indicate the formatter's output triggers stricter-parser-only diagnostics.
  Needs its own investigation; not yet linked to bug (a) above.

---

## Config Keys and Defaults

Configurable values with their in-class defaults. All overridable via config file or CLI.

```properties
# ── Structural constants ──────────────────────────────────────────────────────
line-length                      = 100
indent-size                      = 4
indent-style                     = spaces      # spaces | tabs | auto
server-port                      = 17173

# ── Behavior ──────────────────────────────────────────────────────────────────
line-endings                     = lf          # lf | crlf | preserve
normalize-comment-start-case     = on          # on | off
normalize-comment-end-period     = on          # on | off
comment-normalization-classifier = off         # off | on
closing-comment-min-lines        = 5
format-macros                    = off         # off | on

# ── C/C++ ─────────────────────────────────────────────────────────────────────
header-guard-rename              = off         # off | on (warn only by default)

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order                = java, com, org, other, local, static
java-import-sort                 = on
java-import-depth                = 2
java-import-blank-lines          = 1

# ── Kotlin ────────────────────────────────────────────────────────────────────
kotlin-import-order              = kotlin, java, android, com, org, other, local
kotlin-import-sort               = on
kotlin-import-depth              = 2
kotlin-import-blank-lines        = 1
```

For every added, deleted, or modified configuration item, synchronize it with the implementation of *In‑file Config Support* (the `JXM_CFMT_CFG` directive).

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
  (named-construct/attribute detection, C++ modifier/specifier recognition, `::`-qualified name
  spacing, constructor Allman-brace close-paren resolution, getter/setter param column padding,
  trailing-return-type detection, if/else-if chain collapsing).
- [x] File-pair test: `java_core_inp.java` → diff vs `java_core_out.java` (PASS) — 8 bugs fixed
  (header-spacing modifier-keyword skip, `this`/`super` as assignment LHS, stale closing-comment
  replacement, `throws`-clause Allman conversion, inline-switch fallthrough/`break` padding,
  `catch`/`finally` own-line placement, `@Annotation` skip before signature parsing).
- [x] File-pair test: `cpp_modern_inp.cpp` → diff vs `cpp_modern_out.cpp` (PASS) — 11 bugs fixed
  (comment no-capitalize set, named-construct one-liner pre-expansion, `T::version;` misparse,
  `template<...>`/`requires`-clause signature handling, one-liner bodies always K&R,
  Java-only static reordering, operator-overload function detection, coroutine promise_type
  grouping, brace-initializer/structured-binding spacing, tight cast-template angle brackets,
  `namespace a::b::c` closing-comment name chain) plus a critical idempotency regression fix
  (declaration-group column padding double-counted as indentation on a second pass).
- [x] File-pair test: `java_modern_inp.java` → diff vs `java_modern_out.java` (PASS) — 5 bugs
  fixed (empty named-construct bodies no longer expanded, `.`/`=`-bearing one-liner calls
  rejected from getter/setter grouping, multi-statement one-liner bodies left unsplit, RDD_KEY_75
  adjacency heuristic removed so one-liners always stay K&R, blank line before a named-construct's
  leading comment).
- [x] File-pair test: `combined_inp.h` → diff vs `combined_out.h` (PASS) — 3 bugs fixed
  (`format-macros` value-column alignment, `extern "C"` closing-comment fixture correction,
  enum-with-alias closing comment + `#if`-guard depth-tracking + blank-line placement).
  Committed as `efeb6df`.
- [x] File-pair test: `combined_inp.c` → diff vs `combined_out.c` (PASS) — 4 bugs fixed
  (struct member group indentation strip safety, flat brace-aggregate initializers accepted into
  declaration groups + C-style cast rejoining, parameter inline block comments preserved in
  signatures, switch closing-comment idempotency ordering).
- [x] File-pair test: `combined_inp.hpp` → diff vs `combined_out.hpp` (PASS) — 1 bug fixed, 1 not
  feasible (mid-word-dot vs. sentence period disambiguation needs semantic understanding — logged
  as Tier-3 AI-assist candidate in `STATE_NEXT_AI.md`, fixture hand-edited instead), 1 more fixed
  (constructor/destructor/operator-overload exclusion from one-liner grouping, `template<...>`
  prefix recognized as a valid declaration prefix with its own tight-angle-bracket render path).
- [x] File-pair test: `combined_inp.cpp` → diff vs `combined_out.cpp` (PASS) — 4 bugs fixed
  (destructor `~` marker vs. return type, out-of-line class-template member one-liner tight
  angle brackets + column layout, structured-binding `auto [...]` misparsed as assignment,
  trailing comment duplication on group-gap trim).
- [x] File-pair test: `combined_inp.java` → diff vs `combined_out.java` (PASS) — 3 bugs fixed
  (multi-line `//` prose paragraph period-stripping evaluated as a whole via
  `computeLineCommentGroups`, enum constant-list-terminating `;` detached onto its own line as a
  new feature, blank line before final `return` in a `throws`-clause method).
- [x] File-pair test: `c_comments_inp.c` → diff vs `c_comments_out.c` (PASS) — 6 bugs fixed
  (mid-param `//` comment reattachment + forced multi-line rendering, fixing a brace-depth
  desync that silently truncated the rest of the file; compound-assignment statements no longer
  misparsed as declarations; one-param-per-line double-space padding; standalone comment between
  two same-group declarations no longer silently dropped via new `hasCommentBefore` group-break
  guard; last-param trailing-comment column alignment; `#define ... // comment` trailing-comment
  capitalization via `PREPROCESSOR`-token branch in `enforceCommentStyle`). One fixture-only
  correction (line-count blank-line theory didn't hold for `Trio`; `c_comments_out.c` hand-edited).
- [x] File-pair test: `cpp_comments_inp.cpp` → diff vs `cpp_comments_out.cpp` (PASS) — 5 bugs
  fixed (member-function forward-declaration param/trailing comments preserved + isolated from
  free-function column alignment, stale-closing-comment/capitalization guards narrowed to the
  `// end `-prefixed and own-line-brace shapes only, namespace-body non-indentation + tight
  function-type template arguments + keyword-before-`(` spacing, leading `requires`-clause
  gating a `template<...>` header pull into a collapsed signature, blank line after a member with
  a trailing same-line comment, structured-binding bracket-interior comments + template-argument
  brace-init spacing).
- [x] File-pair test: `java_comments_inp.java` → diff vs `java_comments_out.java` (PASS) — 4 bugs
  fixed (multi-line signature param leading/trailing comment reattachment + column exclusion,
  per-language `COMMENT_NO_CAPITALIZE_C`/`_CPP`/`_JAVA` keyword sets, switch-case comment
  blank-line preservation via `caseSpansMultipleLines` + sub-gap restriction, flat-aggregate
  initializers with per-element comments left byte-for-byte untouched rather than collapsed).

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

**Finished dogfood / real-code testing**
(1) `blake-madden/tinyexpr-plusplus` (C++20) — 3 bugs in `MiscRule`'s multi-line
    call/declaration rendering + `Formatter` pass ordering (commits `1c10946`/`26a9715`).
    Verified with (2). Config: default. Fixture: `real_code_regressions_1`.
(2) RobotCoding `gui_frontend` (71 `.java` files) — 4 pass-ordering idempotency bugs
    (`>>>` mistokenization, getter/setter + call-line-breaking column-padding ordering).
    Verified with (4). Config: default. Fixture: `real_code_regressions_2`.
(3) Self-dogfood (formatter's own `src/`, 20 files) — 1 pass-ordering bug in
    `MiscRule.parseAssignment` (RHS already wrapped by a later call-line-breaking pass).
    Verified with (4). Config: default. Fixture: `real_code_regressions_3`.
(4) `martinus/nanobench` (`nanobench.h`, 3484 lines, tested as `.hpp`) — 2 bugs: missing
    C++11 raw-string-literal tokenizer support (brace-depth corruption), dropped
    `template<...>` prefix on a bare forward declaration. Verified with (2). Config: default.
    Fixture: `real_code_regressions_4`. Known non-bug: testing the same file under its real
    `.h` (C) extension hits an unrelated, real-C-can't-produce-this-shape mismatch in
    `CppSpecificRule.enforceEmptyParameterList` — not fixed, out of scope.
(5) User-reported bug (`real_code_regressions_1_out.cpp`'s `} // while` indentation) —
    `ScopePipeline` never re-derived a scope's own closing-brace gap from depth; fixed in
    `processScope`'s child recursion, surfacing 4 `findParentIndent` edge cases. Verified
    with (2) (re-ran full `nanobench` round-trip too). Config: default.
    Fixture: `real_code_regressions_5`.
(6) Local `pcpp_java` tool source (41 `.java` files) — 2 idempotency bugs: `SwitchRule`
    inline-alignment overflow (no `lineLengthLimit` check), `ScopePipeline.processScope`
    one-liner-body raw-newline check misfiring on an already-broken call. Verified with (4).
    Config: default. Fixtures: `real_code_regressions_9`, `real_code_regressions_10`.
(7) C17 `Tongsuo-Project/tongsuo-mini` (56 `.c`/`.h` files) — 1 bug: flat aggregate-init
    collapse in `DeclarationAlignmentRule` had no line-length check, producing an
    unboundedly long rendered line for large byte tables; fixed by threading
    `lineLengthLimit` through a new constructor. Verified with (1) (identical pre-existing
    error count in `src/log.c`, unrelated to formatting). Config: default.
    Fixture: `real_code_regressions_11`.
(8) C++20 `serge-sans-paille/frozen` (44 `.hpp` + `tests/catch.hpp`) — 10 bugs across
    `ScopePipeline`/`DeclarationAlignmentRule`/`TokenizerCore`/`CppSpecificRule` (parent-indent
    off-by-one, struct depth-tracking, oversized-brace-init dangling `}`, getter-padding
    pass-ordering growth, K&R/Allman flapping one-liners, `catch.hpp` backslash-continued
    preprocessor corruption + 4 follow-on divergences, namespace-unaware indent fallback,
    Objective-C-message-send/C++17-attribute mistokenization). Verified via idempotency only
    (full 156-file tree round1-vs-round2 clean); no compiler used. Config: default.
    Fixtures: `real_code_regressions_12` through `_16`.
(9) C++20 `fmtlib/fmt` (15 `.h` + 4 `.cc`) — idempotent at default `indent-size`; re-testing
    at the codebase's real 2-space/flush-case-label convention found `SwitchRule.deriveUnit`'s
    hardcoded 4-space fallback (same class as the `MiscRule.INDENT_WIDTH` dead-config bug, see
    "Other findings" below), causing unbounded case-body indent growth in 3 files; fixed by
    threading `indentWidth` through `SwitchRule`. No-op at default `indent-size` (no fixture);
    verified live at `indent-size = 2`.
(10) C++20 `taocpp/PEGTL` (355 `.hpp` under `include/`) — 1 bug:
     `TokenizerCore.reclassifyAngleBrackets`'s `>>`-split branch duplicated a character via
     `retype()` reusing the original 2-char text; fixed by giving the retyped token its own
     explicit `">"` text. Verified with (2) (PEGTL's 5 example programs, 0 errors, matching
     baseline). Config: default. Fixture: `real_code_regressions_28`. Also found a no-op:
     `ScopePipeline.normalizeIndent` only rounds declaration-statement indentation, never
     non-declaration statements — a real divergence at PEGTL's actual 3-space convention but
     invisible at the formatter's default 4-space; confirmed non-issue by re-testing at
     `indent-size = 3` (full tree idempotent there too). No fixture (no-op at default).
(11) C++17/20 `foonathan/lexy` (121 `.hpp` under `include/`) — no bug found; idempotent at
     default `indent-size = 4` (matches lexy's own convention). Verified with (2) on all 9
     `examples/` files (0 errors, matching baseline); `tests/` doctest suite skipped
     (external `doctest` header not vendored). Despite predicting relevance to the
     RDD_KEY_85/RDD_KEY_56-adjacent (concepts/`requires`, tight template angle brackets)
     construct family, none of those surfaced a new defect here — already covered by
     `frozen`/PEGTL/stdexec. Config: default. No fixture.
(12) C++20 `NVIDIA/stdexec` (192 `.hpp`/`.cpp` under `include/`) — 4 bugs across three
     sessions: (1) idempotency — a C++20 requires-expression compound-requirement's inner `}`
     (followed by `->`) misidentified as a scope-closing brace by
     `ScopePipeline.splitTopLevelSpans`; (2) compile-breaking — a depth-0
     `PREPROCESSOR`/`MACRO_DEF` token didn't force-close the current statement in
     `DeclarationAlignmentRule.splitStatements`, silently dropping `#if`/`#endif` guards;
     (3) compile-breaking — `BlockStructureRule.tryCollapse`'s `renderInline` absorbed
     everything after a `//` comment between call arguments into the comment, needs a
     `containsLineComment` guard; (4) idempotency — `DeclarationAlignmentRule.parseDeclaration`
     misparsed an already-collapsed one-liner `if`/`while`/`for`/`switch`/`do`/`else` as a
     bogus declaration on a second pass. Verified with (2) (`LD_LIBRARY_PATH` needed; ~10
     pre-existing TBB/PSTL errors expected/unrelated). Config: default. Fixtures:
     `real_code_regressions_34` (bugs 1+2), `_35` (bug 3), `_36` (bug 4).
(13) C++11 `boostorg/mp11` (34 self-contained `.hpp`, 5483 lines) — no bug found; idempotent
     at default `indent-size = 4` (matches mp11's own convention). Verified with (2)
     (`LD_LIBRARY_PATH` needed) on every header standalone (repo's own `test/*.cpp` needs
     unvendored boost deps, skipped). Config: default. No fixture.
(14) C++23 `basvas-jkj/cpp_modules` (7 `.cpp`/`.hpp`/`.mpp` files, confirmed real
     C++20/23 language-modules usage) — no formatter bug found; idempotent on every file. One
     suspicious-looking diff (`foo(bar())` → `foo( bar() )`) confirmed intentional per the
     "universal complexity padding" design (commit `7b4c80d`), not a regression. Compile-check
     via (3): identical pre-/post-format failures on every file (missing `std`/header-unit BMI
     cache, missing `cr.hpp` — checkout-environment issues, not formatter-induced). Config:
     default. No fixture. (Compared first against `V1niciosLins/StartCpp`, which turned out to
     be a project-generator script with no real C++ source of its own to format — not a
     formatter candidate, superseded by testing `cpp_modules` instead.)
(15) `google/google-java-format` (SMALL, 84 `.java` files) — idempotency check found 5
     diverging files, resolved by 3 distinct bug fixes: `SwitchRule.ensureBlankLineInGap`
     wrongly splitting a trailing same-line comment onto its own line (fixed via
     `startsOwnLine` check; fixture `real_code_regressions_6`); `Doc.java`'s divergence
     resolved entirely by the config-key wiring audit (see "Other findings" below, no
     additional code change, confirmed byte-identical at `indent-size = 2`);
     `JavaSpecificRule.applyArrowAlignment` joining an arrow-switch case onto its body with no
     line-length check (fixed by predicting joined width first; fixture
     `real_code_regressions_7`); `GetterSetterRule.parseOneLinerMember`'s `findNameBeforeParen`
     misparsing `case X, Y -> call(...);`/`default -> throw ...;` arrow arms as fake one-liner
     members (fixed by rejecting `case`/`default`-leading one-liners; fixture
     `real_code_regressions_8`). Verified with (4). Config: default (Doc.java re-verified at
     indent-size = 2).
(16) MEDIUM `javaparser/javaparser` — see "Not started" below (queued, not started).
(17) HUGE `openrewrite/rewrite` — see "Not started" below (queued, not started).
(18) Local `VMA-GIT/anemonesoft/` (`gui/`, `i18n/`, 82 `.java` files, JDK8-source/JDK21-javac)
     — copied to `/tmp/anemonesoft_test/` for testing (original tree verified untouched
     before/after). Idempotency check found 2 diverging files, 1 bug:
     `MiscRule.renderCallCandidate`'s `containsNewline` branch used `groupByOriginalLine`
     (paren/bracket depth only, not brace depth), swallowing a multi-line brace-bodied trailing
     argument into one unboundedly-long rendered line; fixed by widening an existing
     Kotlin-only "leave such an argument untouched" bail to all languages via a new
     `containsInternalNewline` helper. Verified with (4) (28 pre-existing missing-`Jama`-
     dependency errors, identical before/after). Config: default.
     Fixture: `real_code_regressions_29`.
(19) Local PCPP-heavy `../../../src/jxm/ugc/ARMCortexMThumbC.java.in` (not
     standalone-compilable, a `.java.in` template) — no bug found; verified with (5), 0-line
     token-stream diff on 105366 tokens. Config: default.
(20) C++20 `ericniebler/range-v3` (311 `.hpp` under `include/range/v3/`) — 2 compile-breaking
     bugs from the concept-emulation-macro convention (`#define template(...) ...` /
     `CPP_ret`/`CPP_member`, `detail/prologue.hpp`): (1)
     `ScopePipeline.extendOverLeadingRequiresAndTemplate` pulled a `template(...)`-spelled macro
     invocation onto a declarator's line whenever a `requires` line sat above it (no check the
     `template` keyword was followed by `<`), and separately still glued a `//`-terminated
     `requires` line onto the following declarator even after that was fixed — both silently
     commented out the declarator; fixed by gating the `template` pull on `<` and refusing to
     pull a `requires` line whose own last token is a `//` comment (a `/* ... */` block comment
     stays safe to pull). (2) `CppSpecificRule.enforceEmptyParameterList` rewrote
     `CPP_ret(void)(...)` (a macro call, not a declarator) to `CPP_ret()(...)`, deleting the
     macro's real `void` argument; fixed by refusing the `(void)` -> `()` rewrite when the
     matching `)` is immediately followed by another `(`. Verified with (2)
     (`view/iota.hpp`/`detail/variant.hpp` before/after error counts now match baseline) and full
     round1/round2 idempotency over the whole 311-file tree. Config: default. Fixture:
     `real_code_regressions_50`. Follow-up session resolved bug (b) below with a narrow,
     verified guard; bug (a) remains open and is now tracked in **Open Questions** (also found
     to additionally affect `meta.hpp`, not just `any.hpp`/`common_iterator.hpp`). (a) OPEN —
     see Open Questions. (b) RESOLVED — `view/view.hpp` / `action/action.hpp` compile-breaking
     bug: a declaration ending in `;` (not a function body) whose original source spans multiple
     lines each deliberately ending in its own `//` ASCII-banner comment (e.g. a `friend ...
     operator|(...) -> CPP_broken_friend_ret(...)( requires ...) = delete;` deleted-overload
     declaration) was getting collapsed onto one rendered line. Root cause:
     `DeclarationAlignmentRule.parseDeclaration`'s function-pointer-detection branch (the
     `Type (*name)(params)` shape) misfired on this concept-emulation-macro call shape
     (`CPP_broken_friend_ret(Rng)(requires ...)` is syntactically identical to a func-ptr's
     `(name)(params)`), and that branch's `rawSliceBetween`-based token capture preserves raw
     `COMMENT_LINE` tokens verbatim, which later got flattened onto one rendered line by
     `renderTokens`/`joinVerbatim`, silently swallowing the `requires`/`= delete;` tail into the
     first `//` comment. Fixed with a narrow guard local to that one branch only (not the blanket
     "bail if any interior `//` comment" guard tried previously, which broke ~12 unrelated
     `make test` fixtures by also catching ordinary multi-line declarations with legitimate
     per-line trailing comments): scan `funcPtrSizeTokens` for a `COMMENT_LINE` token *before*
     mutating `nameToken.text`, and if found, skip the func-ptr branch entirely (falling through,
     with the statement's tokens left completely untouched, to the generic parsing path, whose
     own `->` type-token rejection check correctly returns `null` for this shape). A second,
     narrower defense-in-depth guard was also added on the generic path's own
     `sizeTokens`/`initTokens` in case some other shape ever reaches it with a raw-sliced
     comment-carrying token list. Verified with (2) (minimal repro, byte-for-byte correct and
     idempotent) and `make test` (70/70 forward + 70/70 idempotency, zero regressions), and with
     a full round1/round2 idempotency pass over the whole 318-file tree — `view.hpp`/`action.hpp`
     do not appear in the round1-vs-round2 diff, confirming zero regressions tree-wide. Config:
     default. Fixture: `real_code_regressions_51`.
(21) C++20 `boost-ext/ut` (44 `.hpp`/`.cpp` under `include/`, `example/`, `test/ft/`, `test/ut/`;
     the single-header `include/boost/ut.hpp` is the priority file) — 1 idempotency bug: a
     deduction-guide statement (`test(...) -> test<Test, TArg>;`, itself re-broken across lines
     by `enforceCallLineBreaking`) directly followed by an unrelated `struct suite { ... };`
     caused `CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`'s
     `findCloseParenBeforeTrailingReturnType` backward scan to cross the `;` statement boundary
     between them and misidentify the deduction guide's own close-paren as `struct suite`'s
     "function close paren", Allman-converting the struct's brace onto its own line with a bogus
     indent derived from deep inside the unrelated prior statement; a later K&R re-collapse pass
     on the next format pass joined it back, producing a stable round1-vs-round2 diff. Fixed by
     making both `findCloseParenBeforeTrailingReturnType` and
     `findCloseParenBeforeRequiresClause`'s backward scans stop (return -1) at a depth-0 `;`, not
     just `{`/`}`. Verified with a minimal standalone repro, `make test` (72/72 forward + 72/72
     idempotency, zero regressions), and a full round1/round2 idempotency pass over the whole
     44-file tree (clean). Compile-checked all 32 `example/*.cpp` files and `test/ut/ut.cpp` with
     (2) (`g++ -std=c++20 -fsyntax-only`, 0 errors, matching baseline) and with (3) (`clang++
     -std=c++23 -fsyntax-only`, 1 pre-existing environment error per file — missing libc++
     `<version>` header, identical before/after formatting, not formatter-induced). Config:
     default. Fixture: `real_code_regressions_52`.

**Not started dogfood / real-code testing**
(1) `github.com/microsoft/proxy` — Microsoft's reference implementation of the Proxy library
    (WG21 P0957, polymorphism without inheritance/virtual dispatch); heavy C++20/23 template
    metaprogramming, deliberately pushes newest-standard facilities. Would verify with (2)/(3)
    (clang++ preferred — may need very recent toolchain support). (NOT STARTED)
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
(5) MEDIUM `github.com/javaparser/javaparser` — Java parser/AST library; expected to
    exercise generics-heavy declarations, deep visitor-pattern hierarchies, extensive Javadoc
    (§15 comment-scope, RDD_KEY_47-50), large switch-heavy dispatch code (§13). Would verify
    with (4). (NOT STARTED)
(6) HUGE `github.com/openrewrite/rewrite` — large multi-module AST-rewrite engine; low
    priority given size, pick up once smaller candidates are exhausted. Likely some
    annotation-processor-generated/Lombok-style code (`AI_PREAMBLE`-adjacent gaps). Would
    verify with (4). (NOT STARTED)

Priority order for the C/C++ queue above unless the user redirects: `microsoft/proxy` →
`STL` → `llvm-project` → `gcc-mirror` (`mp11`/`lexy`/`stdexec`/`range-v3`/`boost-ext/ut`
already DONE, see "Finished" above — `mp11` was smallest/narrowest, `lexy` next for touching
operator overloading/concepts/CRTP/dense declaration-alignment in one small tree, `stdexec` for
concepts/`requires`/deep metaprogramming, `range-v3` for its own distinct
`template(...)`/`CPP_ret`-style concept-emulation-macro convention). For any C/C++ candidate
distributed under a `.h`/`.hpp` extension, confirm which language it actually is before testing —
copy to `.hpp` first if really C++.

**In progress dogfood / real-code testing details**

*(none currently — all previously in-progress candidates (`stdexec`, `mp11`) reached DONE
with no open gaps.)*

When a test completes, remove/compact its entry from "Not started" (or its "In progress"
detail block here) and add it to "Finished dogfood / real-code testing" above — and to
"Tools/compiler used" too, if it introduces a genuinely new tool not already listed there.

**Other findings outside the candidate list**

**Config-key wiring audit (2026-07-06)**, done ahead of the `Doc.java` bug above at user
request. Root-caused `Doc.java`'s divergence to `MiscRule.INDENT_WIDTH`/`LINE_LENGTH_LIMIT`
being `public static final` constants disconnected from `Config.indentSize()`/
`Config.lineLength()`. Full audit of the example `.jxmake-code-formatter`: only
`line-length`/`indent-size` were dead/unwired; every other key was already correctly wired.
Fixed by converting those constants to instance fields, threaded through every constructor
that needs them (`MiscRule`, `GetterSetterRule`, `JavaSpecificRule`, `CppSpecificRule`,
`ScopePipeline`), with `Formatter.formatOne` reading `config.indentSize()`/`config.lineLength()`
once per file. No-op at default settings; verified live at `indent-size = 2`.

**Follow-up (same day):** several rule classes also carried their own independent hardcoded
`DEFAULT_INDENT_UNIT = "    "` fallback (same bug class) — found and fixed in `MiscRule`'s §8
call/declaration-wrapping pass, `JavaSpecificRule.deriveIndentUnit`, and
`CppSpecificRule.enforceRequiresClausePlacement`. No-op at default `indent-size`; re-running
the `frozen` tree while re-verifying this is what surfaced the `map.hpp`/`set.hpp`/`catch.hpp`
bugs in that entry above (unrelated to this audit itself).

**Removal (same day):** `header-guard-style` removed entirely (was silently-dead config
surface — accepted `ifndef`/`pragma-once` with zero effect) from `Config.java`, `README.md`,
and this file's sample config. Re-add if guard-style conversion is ever actually implemented.

**Dogfood-compile-check bug** (fixed before the round1/round2 real-code methodology existed —
this is what the Step A "Dogfood self-format compile" checklist item above refers to):
`MiscRule.renderCallPreserveGroups`/`renderDeclarationPreserveGroups` split each source line's
tokens on top-level commas independently, resetting paren/bracket/angle depth to 0 at each line
start — a nested call whose own arguments wrap onto a second physical line (real depth > 0
carried across the break) caused a misread trailing comma and a duplicate comma insertion,
corrupting output (including the formatter's own `TokenizerCore.java`). Fixed with a new
`groupByOriginalLine` helper that tracks depth cumulatively across the whole multi-line slice;
the old buggy `splitOnNewlines` removed.

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
`Token.frozen` + `TokenizerCore.markFrozenSpans` (scans `//% JXM_CFMT_DIS`/`ENA` and block-
comment equivalents); every rule class given a frozen-span guard (skip transformation, still
counted for brace/depth/line bookkeeping); `--format-off` CLI flag (starts the whole file
frozen). Design direction (RDD_KEY_90): in-memory token masking, not fragment/tmp-dir
splitting, since disabled regions aren't guaranteed to align to block boundaries. Two bugs
found and fixed while adding the `format_toggle_inp/out.java` fixture: (1) frozen-span checks
used a group's/member's leading gap (which can contain a previous statement's own trailing
marker) instead of its first real token, falsely freezing the next unfrozen item; (2)
`ScopePipeline.processScope`'s child-scope recursion re-tokenizes a substring from scratch,
losing frozen-entry state from outside that substring — fixed by threading an explicit
`startFrozen` parameter through `processScope`/`tokenize`. `README.md` documents the marker
syntax and flag. `make test` 16/16 PASS.

### B — New config entries: `normalize-comment-start-case`/`normalize-comment-end-period` (DONE)
Two new `Config.java` keys (default `on`/`on`), following the existing `format-macros`
pattern; `MiscRule` gates both at the two shared comment-normalization methods every comment
call site already funnels through. `make test` 15/15 PASS.

### C — Don't damage C-preprocessor macros embedded in Java source (DONE)
`TokenizerCore.isPreprocessorLanguage()` now returns `true` unconditionally (was
`!"java".equals(language)`), so `#`-directive lines in `.java` lex as opaque
`PREPROCESSOR`/`MACRO_DEF` tokens like C/C++, with no further per-rule Java-specific handling
needed. Surfaced two pre-existing bugs (not introduced by this change, confirmed via
`git stash` against unmodified C++), both now fixed — see "Known Gaps — Fixed" above.
`make test` 15/15 PASS.

### D — Multi-file smoke test + benchmarking (DONE)
New `bench` Makefile target times all-at-once vs. one-by-one formatting in both
`--standalone` and client-server mode (the all-at-once passes double as the multi-file smoke
test). Server started/stopped around the client-server timing only; a real client request
polls until ready before the timer starts, excluding startup/lockfile-race time.

### E — Code cleanups (DONE)
1. New `Lang` class computes `isC`/`isCpp`/`isJava` once per file in `Formatter.formatOne`
   and is threaded into every rule class's constructor, replacing scattered
   `"c"/"cpp"/"java".equals(language)` checks.
2. Five null-safe static helpers (`isPunct`/`isOp`/`isKeyword`/`isComment`/`isGapToken`)
   centralized on `TokenizerCore.Token`; every rule class's duplicate private copy deleted in
   favor of a `static import`. Two semantically-different rule-local helpers deliberately kept
   separate: `MiscRule.isCommentOrNewline`, `ScopePipeline.isWhitespaceOrNewline`.

### F — Add more tests (DONE)
`test/c_cpp_decl_gaps_inp/out.c` added, covering the three `DeclarationAlignmentRule` fixes
under "Known Gaps — Fixed" (`* const` column gap, `typedef` grouping, function-pointer
declarations). `make test` 17/17 PASS. This was the final F sweep after Task A landed; each
of A/B/C/D/E's own fixtures were added as each task landed, not batched here.

### G — Verify `AI_PREAMBLE_FULL.md`'s `### Edge Case` sections against actual JAR behavior (DONE)
Both of `../AI_PREAMBLE_FULL.md`'s `### Edge Case` sections (renamed from `### Unresolved`)
turned out to already be enforced by the JAR, not genuine gaps requiring manual AI
judgment — confirmed with real `--standalone` runs, not static analysis, per
STATE_COMMON.md's "evidence over reasoning" rule:
1. **`else`/`else if` closing comments** — `BlockStructureRule.classifyBrace` returns
   `Frame.excluded(braceIdx)` for both the `else`/`if` brace immediately after an `else`
   keyword and a bare `else` brace, so neither ever reaches the closing-comment-adding path
   (only `IF`/`FOR`/`WHILE`/`SWITCH` frame kinds do). Verified on a fresh
   `if`/`else if`/`else` fixture (6-line bodies each, over `closing-comment-min-lines`): only
   the leading `if`'s `}` got `// if`; both `else if`'s and `else`'s `}` got no comment.
2. **`type* const` in a mixed declaration group** — `DeclarationAlignmentRule.splitCppType`
   renders the whole type including any trailing `* const` as a single cell, so the existing
   widest-cell-pads-all column logic already pads `uint8_t* const cptr;` in a group with
   `uint8_t value;`/`uint8_t* ptr;`/`uint16_t count;` exactly as `AI_PREAMBLE_FULL.md`'s own
   worked example shows. Verified byte-for-byte against that example via `--standalone`.

Both sections removed from `AI_PREAMBLE_FULL.md` as redundant with already-COMPLETE JAR
behavior — nothing left there for an AI reader to act on manually.

### H — Comment-grammar classifier accuracy upgrade (DONE)
Formerly tracked in its own `STATE_COMMENT_GRAMMAR.md` (deleted once complete — see
`RDD_LOG.md`'s `RDD_KEY_94`–`RDD_KEY_98` for the full design history). Adds an optional
classifier-backed decision path for the `normalize-comment-start-case`/
`normalize-comment-end-period` keys, behind a new `comment-normalization-classifier` config key
(default `off`, zero behavior change when off). New `com.jxmake.formatter.classifier` package:
`CommentFeatureExtractor`/`CommentFeatureVector` (pure feature extraction), `NonLatinScriptGate`
(RDD_KEY_95: any non-Latin codepoint disables the classifier for that comment), `KeywordAmbiguityGate`
(RDD_KEY_96: per-language keyword lists + two-stage ambiguity check for a keyword-leading comment
word, e.g. "static" as an English adjective vs. the C keyword), `CommentClassifier`/
`CommentClassifierWeights` (score/threshold decision, `YES`/`NO`/`ABSTAIN`; `ABSTAIN` behaves
exactly like `off` for that one comment — never guess). Weights (RDD_KEY_97: frontier-model-assisted,
not corpus-trained, for v1) generated from 40 labeled per-language examples under `cwg/` via
`cwg/derive_weights.py` (L2-regularized logistic regression, reproducible/reusable for future
re-derivation — see `cwg/README.md` and `cwg/weights.md`). Four features feed the
keyword-ambiguity path's scoring (`nextCharIsOpenParen`, `nextTokenIsArrow`, `containsSemicolon`,
`containsUrlOrFilenameOrNumber`); `nextTokenIsArrow` was added after the initial weight pass to
close a real miss on a Kotlin `when`-branch shape (`is Foo -> handle(foo)`) — see
`cwg/weights.md`'s "Adding a feature" for the worked example and the recipe for adding another.
`make test` 70/70 PASS unchanged (default `off`); classifier `on` verified via a 4-language `/tmp`
smoke test and 39/40 against the labeled example set (one documented accepted tradeoff remains,
not a defect — see `cwg/weights.md`).

**TODO:** the 40-example `cwg/` set is synthetic (written by the assisting AI, not pulled from
real code), and covers only the four features that exist today. Growing it — especially with
real comments pulled from this codebase or the `test/` fixtures, per the "is it easy to add more
examples" discussion — would firm up the weight magnitudes and might surface new feature-set
gaps the same way `nextTokenIsArrow` did. This is independent of the C/C++/Java formatter's own
test suite (`make test`'s 70 fixtures) — the classifier defaults to `off` and ships no runtime
AI dependency, so this TODO is about `cwg/`'s example quality only, not formatter correctness.

### I — C/C++/Java braceless else-if chain collapse + alignment (DONE)
User request: extend Kotlin's braceless if/else-if/else collapse + column alignment
(RDD_KEY_124/127/128) to C/C++/Java, but only when **every** branch in the chain already
qualifies for single-statement collapse — a mixed or fully-braced chain must be left
untouched byte-for-byte, never partially collapsed (RDD_KEY_129). New whole-chain scan
`BlockStructureRule.chainAllBranchesCollapsible`, backed by `findChainStart`/
`prevChainBranchIf` (only ever hops backward through an explicit `else` token, never a bare
`}` alone — an early version that did so incorrectly absorbed an unrelated standalone `if`
statement preceding a real chain into the chain-start search) and comment-aware
`skipWhitespaceOnly`/`skipWhitespaceOnlyBackward` (a comment between branches blocks the
whole chain, is never silently skipped past). `isPartOfElseChain` restructured: Kotlin's
per-branch-suppression behavior is unchanged; C/C++/Java now suppresses only when the whole
chain fails to qualify. A new bare-terminal-`else{...}` collapse path was added (previously
unreachable — the main loop only ever dispatched on `if`/`while`/`for`, which have a `(...)`
condition to anchor on). `alignBracelessElseIfChain` moved from `KotlinSpecificRule.java`
into the shared `BlockStructureRule.java` and now runs for all languages from
`Formatter.java`'s Phase 4, after every paren-tightening/spacing pass has settled.

Two follow-up defects found and fixed immediately after, both in the same feature
(RDD_KEY_130): (1) an idempotency bug in `alignBracelessElseIfChain`'s render loop — it
stripped only one leading space when re-extracting an already-padded `else` line's body,
so alignment spacing grew on every successive formatting pass; fixed by stripping all
leading spaces before re-padding. (2) a K&R `} else` (same physical line as the closing
brace) caused an entire chain to collapse onto one giant line instead of the user's actual
intent — Allman-style, one branch per line, column-aligned, matching Kotlin's own shape;
fixed by `mostRecentLineIndent`/`appendChainNewlineBeforeElse`, which force a `\n` + the
chain's original indent before a following `else` instead of letting K&R same-line
whitespace pass through unchanged.

`make test` full suite green (forward + idempotency) after both RDD_KEY_129 and
RDD_KEY_130. Fixtures updated: `test/c_combined_out.c`, `test/cpp_modern_out.cpp`,
`test/java_combined_out.java`, `test/java_core_out.java`, `test/java_modern_out.java`,
`test/real_code_regressions_15_out.hpp`.
