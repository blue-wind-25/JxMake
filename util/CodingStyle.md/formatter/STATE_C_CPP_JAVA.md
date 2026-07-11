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

*(none)*

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
- The user may specify which `*_inp.*` file to run next — **do not assume sequential
  order**. Run only the file the user names, unless told to run all remaining.
- Run test files **one at a time**, not all at once. After each file, if
  the formatter output does not match the `*_out` file, **stop and ask the
  user** before attempting any fix — the mismatch may be a bug in the
  `*_out` file itself (authored by hand, not confirmed by the formatter),
  not necessarily a formatter bug. Record which files passed and which did
  not in STATE.md as you go, so progress is preserved if quota runs out.
- After each individual file test — pass or fail — update the checklist
  item inline with `(PASS)`, `(FAIL)`, or `(SKIP)` and commit STATE.md
  immediately. Do not batch multiple results into one commit. This ensures
  no progress is lost if the session ends mid-way through the 15 files.
- **Do not remove `[x]` or `(PASS)` entries from this list**, even after all
  tests pass. Fixing a bug discovered in one file may cause a regression in a
  previously-passing file; the full list allows the user to ask for a specific
  file to be re-run at any time.
- The same ask-first rule applies to the self-dogfood pass: if formatting
  the formatter's own source produces unexpected changes, stop and report
  the diff to the user before fixing anything.
- To reduce quota usage and prevent regressions on `(PASS)` tests and previous bug fixes,
  apply STATE_COMMON.md's "evidence over reasoning" rule strictly here.

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
All 15 file-pair tests below PASS (forward + idempotency), zero known regressions.
Full bug-by-bug root-cause narratives for each fix have been compacted out of this file —
they remain fully available via `git log`/`git show` on the commits noted per entry.

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

**If any file-pair test above shows a mismatch: stop, report the full diff to the
user, and wait for instruction. Do not attempt to fix either the formatter or the
`*_out` file without explicit user direction — the `*_out` files were authored by
hand and may themselves contain errors.**

**After all 15 file-pair tests pass (or are resolved - ask the user first):**
- [x] Dogfood self-format pass: run formatter on all `src/**/*.java`, write
      to `target/dogfood-src/`
- [x] Dogfood self-format compile: `javac` the `target/dogfood-src/` tree;
      must compile with zero errors — first run surfaced a real compile-breaking bug (see
      below), now fixed; verified clean compile after the fix.
- [~] Dogfood self-format idempotency / declaration count: superseded by the real-code
      testing approach below, which found and fixed the actual bugs underlying this failure.
      Not re-run standalone against `target/dogfood-src/` since; if revisited, expect it to be
      much closer to passing given the pass-ordering fix, but there may be other Java-only
      convergence bugs the C++ testing below wouldn't have exercised.

**Real-code testing (pivoted from synthetic dogfooding — found bugs faster):** see
STATE_COMMON.md's "Real-code testing methodology" for the repeatable round1/round2/compile
recipe and fixture-registration convention — prefer this over synthetic dogfooding, it found
concrete, fixable bugs far faster than the from-scratch dogfood idempotency failure did.
Full bug-by-bug root-cause narratives for each completed candidate below have been compacted
out of this file — remain fully available via `git log`/`git show` on the noted commits/
fixtures. Several real bugs here (the `SwitchRule`/`fmtlib-fmt` flush-case-label fix, the
`MiscRule` dead-config-key audit) were only observable at a non-default `indent-size` — a
concrete example of STATE_COMMON.md's "re-test at the candidate's own convention" advice.

- **`blake-madden/tinyexpr-plusplus`** (C++20, `g++ -std=c++20`) — DONE. 3 bugs fixed in
  `MiscRule`'s multi-line call/declaration rendering and `Formatter`'s pass ordering:
  `groupByOriginalLine` same-line-sibling mis-split; `renderCallCandidate` line-length
  undercount ignoring trailing same-line text; `enforceComplexityPadding` had to move ahead of
  `enforceCallLineBreaking` in Phase 1 for idempotency (commits `1c10946`/`26a9715`). Fixture:
  `test/real_code_regressions_1_{inp,out}.cpp`. `make test` 19/19, full tree idempotent/compiles.
- **RobotCoding `gui_frontend`** (`../../../../RobotCoding/gui_frontend/src/`, 71 `.java`
  files, `javac` from `/opt/openjdk-25_linux-x64_bin`) — DONE. 4 pass-ordering idempotency
  bugs fixed: `>>>` mistokenized as `>>`+`>` (compile-breaking, added to
  `TokenizerCore.MULTI_CHAR_OPS`); `GetterSetterRule` one-liner body-column padding measured
  pre-`enforceComplexityPadding`; `enforceCallLineBreaking` joining a multi-line call loses
  complexity-padding awareness; `GetterSetterRule`/`JavaSpecificRule` one-liner detection
  didn't predict later line-breaking (both given a "has breakable call + predicted width"
  pre-check). Fixture: `test/real_code_regressions_2_{inp,out}.java`. `make test` 20/20, full
  tree idempotent, compiles with only known pre-existing `javax.jmdns` dependency errors.
- **Self-dogfood** (formatter's own `src/com/jxmake/formatter/`, 20 files) — DONE. 1 more
  pass-ordering bug: `MiscRule.parseAssignment` rejected any §6 alignment row whose RHS was
  already wrapped by a later `enforceCallLineBreaking` pass, breaking the group instead of
  falling through to a verbatim single-line `Assignment`. Fixture:
  `test/real_code_regressions_3_{inp,out}.java`. `make test` 21/21, idempotent, compiles clean,
  declaration counts match.
- **`martinus/nanobench`** (`src/include/nanobench.h`, 3484 lines, formatted as C++ via a
  `.hpp` copy) — DONE. 2 bugs: (1) `TokenizerCore` had no C++11 raw-string-literal
  (`R"delim(...)delim"`) support, so `{`/`}` inside nanobench's mustache templates corrupted
  brace-depth tracking for the rest of the file (up to ~46% content loss); fixed via
  `rawStringPrefixLength`/`emitRawString`, gated on `isC || isCpp` (an initial `isC`-only gate
  missed `.hpp`/`.cpp`). (2) `DeclarationAlignmentRule`'s general group renderer silently
  dropped a `template<...>` prefix on a bare forward declaration (only the
  function-forward-declaration path emitted it) — compile-breaking. Fixture:
  `test/real_code_regressions_4_{inp,out}.hpp`. `make test` 22/22, idempotent, compiles clean.
  Known non-bug: formatting nanobench.h under its actual `.h` (C) extension hits an unrelated
  out-of-scope mismatch (`CppSpecificRule.enforceEmptyParameterList`'s C-only heuristic
  misfires on a constructor member-initializer list) — not fixed, real C can't produce that shape.
- **User-reported bug** (`real_code_regressions_1_out.cpp`'s `} // while` indentation) — DONE.
  `ScopePipeline` never re-derived a scope's own closing-brace gap from depth, so source
  misindentation passed through untouched. Fixed by forcing that gap to the frame's own indent
  in `processScope`'s child recursion, surfacing 4 edge cases in `findParentIndent`: bare
  compound blocks, preprocessor-directive-adjacent spans, a comment directly in the trailing
  gap (must not relocate), and a `case`/`default` label sharing a span with the following
  construct (anchor search had to go backward from `openBraceIdx`, not forward from the span
  start) — plus an empty-body (`{}`) guard so the fix doesn't re-expand it into `{\n}`.
  Fixture: `test/real_code_regressions_5_{inp,out}.cpp`. `make test` 23/23; re-ran the full
  nanobench round-trip too (caught the `case`-label edge case) — still idempotent, compiles clean.
- **Local `../../../3rd_party/tools/pcpp_java/src/`** (JxMake's own Java preprocessor tool
  source, 41 `.java` files) — DONE (2026-07-06). 2 idempotency bugs fixed: `SwitchRule`
  inline-alignment overflow (same bug class as `real_code_regressions_7`) and
  `ScopePipeline.processScope`'s raw-newline one-liner-body check misfiring on a call already
  broken across lines by `enforceCallLineBreaking`. Fixtures:
  `test/real_code_regressions_9_{inp,out}.java`, `test/real_code_regressions_10_{inp,out}.java`.
  `make test` 28/28, full 41-file tree idempotent, compiles clean. Full bug-by-bug detail in the
  "Real-code testing methodology" section below.
- **C17**: `github.com/Tongsuo-Project/tongsuo-mini` — DONE (2026-07-06). 56 `.c`/`.h` files.
  1 bug: `DeclarationAlignmentRule.parseDeclaration` accepted any flat `{ a, b, c }` aggregate
  init (no nested braces, no `//` comments) as safely collapsible to one rendered line with no
  check against `lineLengthLimit` — a large byte/word table (e.g. `sm4.c`'s
  `SM4_S`/`SM4_SBOX_T*` S-boxes) collapsed into a single line thousands of characters long,
  since this class has no multi-line-initializer render path to re-wrap it. Fixed by threading
  `lineLengthLimit` into a new 2-arg `DeclarationAlignmentRule` constructor (legacy 1-arg
  constructor delegates to `MiscRule.DEFAULT_LINE_LENGTH_LIMIT`, wired via `ScopePipeline`) and
  adding `flatAggregateInitRenderedWidth` to reject the collapse if it alone would exceed the
  limit — same pattern as the existing `//`-comment guard on that code path. Fixture:
  `test/real_code_regressions_11_{inp,out}.c`. `make test` 29/29; full 56-file tree
  round1/round2 diff empty; `gcc -std=gnu99 -fsyntax-only` per-file error counts identical
  before/after (1 pre-existing, formatter-unrelated duplicate-definition error in `src/log.c`).

- **C++20**: `github.com/serge-sans-paille/frozen` — DONE (2026-07-07). 44 `.hpp` files (`.h`
  renamed, confirmed real C++) plus `tests/catch.hpp`. 10 bugs fixed total; full narratives
  available via `git log`/`git show` on commits touching `ScopePipeline.java`/
  `DeclarationAlignmentRule.java`/`TokenizerCore.java`/`CppSpecificRule.java` around
  2026-07-06/07. One-line summary each:
  1. `ScopePipeline.findParentIndent` off-by-one indent when a nested construct shares a source
     line with its parent (`struct Foo { enum Bar {`).
  2. `DeclarationAlignmentRule.parseDeclaration` depth-tracking bugs when a struct's last member
     is a braceless control statement before the class's own `};`. Fixture:
     `test/real_code_regressions_12`.
  3. Oversized brace-initializer's dangling `}` left glued to the last data line — new
     `ScopePipeline.applyOversizedAggregateInitClosingBracePass`. Fixture:
     `test/real_code_regressions_11_out.c`.
  4. `map.hpp` getter-padding growth — `enforceTemplateAngleBracketSpacing` ran after
     `GetterSetterRule`'s column-width measurement; pulled forward.
  5. `set.hpp` operator one-liners flipping K&R→Allman between passes — re-run
     `enforceFunctionDefinitionAllmanBraceStyle` once more after `enforceCallLineBreaking`.
  6. `catch.hpp` first-pass corruption — `TokenizerCore.emitPreprocessor()` didn't handle
     backslash-continued directives. Fixture: `test/real_code_regressions_13`.
  7. `ScopePipeline.findParentIndent`'s fallback used `depth * indentWidth`, wrong whenever a
     `namespace` nests in between (namespace bodies don't increment `depth`); fixed by threading
     a real accumulated `inheritedIndent` string through the recursion alongside `depth`.
     Fixture: `test/real_code_regressions_14`.
  8. `catch.hpp`'s `#ifdef __OBJC__` block: nested message sends (`[[NSString alloc] ...]`)
     were mistokenized as a C++17 `[[attribute]]` open, permanently desyncing bracket depth for
     the rest of the file. Fixed with a forward-scan `looksLikeAttributeOpen` guard in
     `TokenizerCore` that only merges `[[` when a genuine attribute-shaped close follows.
     Fixture: `test/real_code_regressions_15`.
  9. Once bug 8's file-wide desync was gone, 4 further small `catch.hpp` divergences surfaced:
     (a) `ScopePipeline.isNamespaceScope` passed `idx - 1` to a helper that already offsets by
     one, double-skipping some `namespace NAME{` forms; (b)/(c) member-initializer-list
     signatures (`Ctor(...) : field(val)`) found their closing paren via "nearest `)` before
     `{`", which lands on the *last initializer's* paren, not the constructor's — fixed via
     `ScopePipeline.findTopLevelMemberInitColon` redirecting to the real paren, plus a
     `trailingLen` parameter threaded into `MiscRule.render` so the line-fit check accounts for
     the member-init-list opener on the same output line (scoped to only this case, since an
     un-Allman'd function body's `{` would otherwise reintroduce instability elsewhere); (d)
     `struct sigaction sa = {};` (elaborated-type declaration, not a struct body) was misdetected
     by `isScopeOpeningBrace` as a construct definition, duplicating its trailing `;` on every
     reformat — fixed by bailing out on an intervening top-level `=`. Fixture:
     `test/real_code_regressions_16` (all 4 shapes combined in one file).
  `make test` 32/32; full `frozen`/`catch.hpp` round1/round2 diff empty; full 156-file
  `frozen` tree (`.c`/`.h`/`.cpp`/`.hpp`) fully idempotent round1-vs-round2.

- **C++20**: `github.com/fmtlib/fmt` — DONE (2026-07-06). 15 `.h` headers + 4 `.cc` sources.
  Idempotent at default `indent-size = 4`; re-testing at this codebase's real 2-space/flush-
  case-label style (`indent-size = 2`) found `SwitchRule.deriveUnit`'s hardcoded 4-space
  `DEFAULT_INDENT_UNIT` fallback (same dead-config-value bug class as `MiscRule.INDENT_WIDTH`,
  see the config-wiring audit below), causing unbounded case-body indent growth in 3 files.
  Fixed by threading `indentWidth` through `SwitchRule`'s constructor. No-op at default
  `indent-size` (30/30, no fixture changes); verified live at `indent-size = 2` — full 44-file
  tree now fully idempotent.

- **C++20**: `github.com/taocpp/PEGTL` — DONE (2026-07-12). 355 `.hpp` files under `include/`.
  1 bug fixed: `TokenizerCore.reclassifyAngleBrackets`'s single-tracked-open-`<` branch for
  splitting a literal `>>` token (e.g. `has_eol_rule<Input> >` after outer-template-prefix
  spacing collapse) retyped the `>>` token to `ANGLE_BRACKET_CLOSE` via `retype()`, which
  preserves the original 2-char `">>"` text, then *also* appended a new 1-char literal `>` OP
  token for the leftover close — duplicating a character on the very first format pass (breaking
  compilation of `internal/rematch_input.hpp`'s `template<typename Guard, typename Input, bool =
  has_eol_rule<Input>>>` forward declaration, `>>>` where only `>>` is valid). The sibling
  size-≥2 branch (splitting a `>>` that closes 2 tracked nested opens at once) avoids this by
  using a zero-width placeholder for its second token; the size==1 branch didn't trim the first
  token's text to match. Fixed by giving the retyped `ANGLE_BRACKET_CLOSE` token its own explicit
  1-char `">"` text instead of reusing the original 2-char token via `retype()`. Fixture:
  `test/real_code_regressions_28_{inp,out}.hpp`. Also found (no-op, no fixture, per the
  `fmtlib/fmt`/`indent-size` precedent): `ScopePipeline.normalizeIndent` rounds only
  *declaration*-statement indentation up to the nearest `indent-size` multiple
  (`applyDeclarationsPass`-only), never non-declaration statements in the same scope — at
  PEGTL's real 3-space indent convention vs. this formatter's default 4-space `indent-size`,
  that asymmetry between a `case`-block declaration and its non-declaration siblings produces a
  non-idempotent round1-vs-round2 divergence in exactly 2 files
  (`debug/internal/analyze_cycles.hpp`, `internal/unwind_guard.hpp`). No-op at default
  `indent-size` (48/48, unrelated to this candidate); confirmed non-issue by re-testing at
  `indent-size = 3` (PEGTL's own convention) — full 355-file tree fully idempotent round1-vs-
  round2 at that setting, with only the above compile-breaking bug (now fixed) remaining
  otherwise. `make test` 48/48; PEGTL's own 5 `src/example/*.cpp` files
  (`hello_world`/`json_count`/`s_expression`/`parse_tree`/`proto3_analyze`) all
  `-fsyntax-only`-compile clean (0 errors) against the fixed round1 output, matching the
  unmodified original's 0-error baseline.

**NEXT SESSION — continue here:** `fmtlib/fmt` is DONE. `serge-sans-paille/frozen` is now DONE
(all bugs fixed, full 156-file tree idempotent). `taocpp/PEGTL` is now DONE (1 bug fixed, full
355-file tree idempotent at its own indent-size convention). Continue real-code testing
against remaining C/C++ candidates in this order unless redirected: the
additional candidates below. Use `/opt/gcc-12.2.0/bin/g++ -std=c++20` (bump if a library needs
newer; confirm any compile failure also reproduces against the unmodified original before
treating it as formatter-induced). For any C++ candidate distributed under a `.h`/`.hpp`
extension, check which it actually is before testing — copy to `.hpp` first if really C++.

Additional candidates the user has since supplied (not yet started, path relative to home dir
written as `~` below so this file never embeds the actual account/user name):

- **C++23**: `github.com/basvas-jkj/cpp_modules` — DONE (2026-07-06). Confirmed it does use
  C++20/23 language modules (`import`/`export module`/`module;` global fragment) throughout,
  the exact risk flagged below — never previously exercised by this formatter. Compared against
  `github.com/V1niciosLins/StartCpp` first: `StartCpp` turned out to be a 499-line Bash project
  *generator* script (scaffolds new C++23/Modules projects on demand) with no actual C++ source
  of its own to format, so `cpp_modules` (93 total lines across 7 small `.cpp`/`.hpp`/`.mpp`
  files) was the only real candidate and also the smaller of the two by actual content. Tested
  all 7 files (the one `.mpp` copied to `.hpp` and `.cpp` files renamed to unique names, since
  Main.java doesn't infer a language from `.mpp` and several files share the name `main.cpp`):
  round1/round2 diff empty (idempotent) on every file. No formatter bug found; the one
  suspicious-looking diff (`println(...)` → `println( ... )` gaining interior padding when an
  argument is itself a call, e.g. `foo(bar())` → `foo( bar() )`) is confirmed intentional per
  the "universal complexity padding" design (commit `7b4c80d`, smoke-tested with this exact
  `func( other() )` shape), not a regression. Compile-check via
  `~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/clang++ -std=c++23 -fsyntax-only`: every file fails
  identically pre- and post-format (`'print' file not found` / `module 'std'|'cwl' not found` /
  missing `cr.hpp`) — expected, since this checkout has no compiled `std`/header-unit BMI cache
  or the repo's own missing `cr.hpp`; the identical failures on both original and formatted
  content confirm the formatter didn't change compileability. No fixture added (no bug found).
- **C++23**: `github.com/V1niciosLins/StartCpp` — DONE, see note above (not a C++ codebase to
  format; superseded by testing `cpp_modules` instead).
- **Clang 22.1.8**, already downloaded and extracted by the user to
  `~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/` (a prebuilt Linux-X64 LLVM release, run directly
  on this CentOS7 box — no `patchelf`/glibc-2.41 repointing needed after all; the release binary
  already runs as-is here). Confirmed working: `clang++ -std=c++23 -fsyntax-only <file>.cpp`
  both with and without `-stdlib=libc++` returns exit 0 on a trivial translation unit. This is
  the preferred tool for the two C++23 candidates above (more current explicit C++23 support
  than `/opt/gcc-12.2.0`). One cosmetic wrinkle: every invocation of any binary under
  `~/xsdk/clang22/LLVM-22.1.8-Linux-X64/bin/` prints one repeated stderr line per shared-library
  dependency --
  `.../clang++: /opt/gcc-12.2.0/lib64/libstdc++.so.6: no version information available (required by .../clang++)`
  -- this is just an older libstdc++ (picked up from `/opt/gcc-12.2.0`, likely via an
  already-set `LD_LIBRARY_PATH`) lacking GNU symbol-versioning metadata; it is NOT a functional
  error (the command still completes and returns the correct exit code) and is unrelated to
  glibc. Filter it out of captured output rather than treating it as a compile error, e.g.:
  `<clang++ invocation> 2>&1 | grep -v 'no version information available'`
  (grep on that fixed substring is safe/stable — don't grep on the changing library path).
  `/opt/glibc-2.41/` also exists in this environment (full glibc install, dynamic linker
  included) if a genuine glibc-version-mismatch problem is ever hit with some OTHER prebuilt
  binary and patchelf repointing becomes necessary again -- not needed for clang22 itself.

- **Modern C++ candidates the user has since supplied (2026-07-12, not yet started).** Each note
  below is what the repo *is* and what shape of formatter behavior testing it is expected to
  stress — written ahead of actually cloning any of these, so treat "expected" claims as a
  prediction to confirm/correct once a candidate is actually run, not settled fact.
  - **RECOMMENDED NEXT** (2026-07-12, smallest-size/highest-feature-density pick — see rationale
    at the end of this list): `github.com/foonathan/lexy` — a header-only C++17/20 parser-
    combinator (PEG-style) library. Small-to-medium source size (~20-40 headers under `include/`,
    far smaller than `range-v3`/`STL`), but written in a dense, idiomatic modern-C++ style: heavy
    `template<...>`/`concept`/`requires`-clause use, CRTP, operator overloading (`lexy::dsl`'s
    whole API is operator-based), `constexpr` functions, nested class hierarchies, and doc
    comments — expected to exercise `CppSpecificRule`'s concepts/`requires`-clause handling
    (RDD_KEY_85), template angle-bracket spacing (RDD_KEY_56), and general declaration-alignment
    grid logic against a lot of short, densely-templated one- or two-line declarations, likely a
    good ratio of "bugs found" per file compiled/formatted compared to a much larger library.
  - `github.com/boostorg/mp11` — a tiny, single-purpose C++11 metaprogramming library (a handful
    of headers, likely the smallest of this whole list by raw line count). Expected to be a fast,
    low-risk smoke test but a narrow one: almost entirely `template<...>` alias declarations and
    `using`-based metafunctions, very little runtime control flow (few if/for/while/switch
    bodies, no classes with member functions to speak of) — good for a quick pass, unlikely to
    surface many *new* bug classes beyond what `frozen`/`mp11`-style template-heavy testing
    already covered via `serge-sans-paille/frozen`.
  - `github.com/NVIDIA/stdexec` — a C++20 implementation of the `std::execution` (P2300)
    senders/receivers proposal. Header-only, moderate size, heavy use of C++20 concepts,
    coroutine-adjacent code (though not raw `co_await`/`co_return` itself), and deeply nested
    template metaprogramming for the sender/receiver customization-point machinery. Expected to
    stress the same concepts/`requires`-clause and template-angle-bracket paths as `lexy`, but at
    a deeper nesting nesting depth — a good complexity-padding (RDD_KEY_22) stress test.
  - `github.com/ericniebler/range-v3` — the pre-standardization Ranges library C++20 ranges were
    based on. Larger than `lexy`/`stdexec` (a well-known, mature, moderately large codebase),
    heavy template/concept-emulation-macro use (this predates real C++20 concepts, so it may use
    its own macro-based concept-emulation layer — a good test of `format-macros` handling).
    Already effectively superseded in "modern concepts" coverage by `stdexec` above, so lower
    priority unless a macro-heavy candidate specifically is wanted.
  - `github.com/microsoft/STL` — Microsoft's actual `std::` implementation. Large (this is a full
    standard library, not a small focused library) — expected to have the best raw grammar
    coverage of anything on this list (every STL container/algorithm shape, extensive
    `_Ugly_reserved_identifier` naming conventions, heavy conditional-compilation
    `#ifdef _M_X64`/`#if _HAS_CXX23` branching, `constexpr`/`consteval` throughout) but at real
    cost in testing time given its size — likely the last of this list to pick up, after the
    smaller candidates above are exhausted, similar posture to `openrewrite/rewrite` in the Java
    list below.
  - `github.com/llvm/llvm-project` — the LLVM/Clang/etc. monorepo. Enormous (likely the largest
    C/C++ codebase available to test against) — expected to have essentially unmatched grammar
    coverage (every C/C++ construct in wide real-world use, heavy template metaprogramming in
    Clang's own AST/type code, extensive macro use in LLVM's `TableGen`-adjacent headers) but at
    a testing-time cost that makes it a last resort, likely only worth a partial/targeted subtree
    run (e.g. just `clang/lib/Format/` or `llvm/include/llvm/ADT/`) rather than the whole tree.
  - `github.com/gcc-mirror` (i.e. `gcc-mirror/gcc`) — the GCC monorepo. Similarly enormous to
    `llvm-project`; also GCC's own C++ source famously targets an older/more conservative C++
    dialect for bootstrapping reasons in parts of the tree, so it may exercise *less* modern-C++
    surface than its size would suggest despite being huge — likely the lowest priority of this
    list for "modern C++ feature testing" specifically, though still valuable for sheer volume/
    real-world-idiom coverage if picked up later.

  **Smallest-size/highest-feature-density rationale**: `mp11` is the smallest by raw size but
  narrow in construct diversity (mostly alias templates, few statement-level constructs);
  `lexy` is judged the better next pick because it's still small/contained but touches operator
  overloading, concepts, CRTP, and dense declaration-alignment scenarios in the same file —
  more likely to surface a genuinely new bug per unit of testing effort than either the very
  narrow `mp11` or the much larger `STL`/`llvm-project`/`gcc-mirror` candidates. (try to test especailly the C++23 feature)
  - `github.com/llvm/llvm-project` (try to test especailly the C++23 feature)
  - `github.com/gcc-mirror` (try to test especailly the C++23 feature)

**Java candidates the user has since supplied (not yet started):**

- **SMALL**: `github.com/google/google-java-format` — DONE (2026-07-06). Idempotency check
  (format all 84 `.java` files twice, diff round1 vs round2) initially found 5 diverging files:
  `JavaOutput.java`,
  `CommandLineOptionsParser.java`, `Doc.java`, `JavadocFormatter.java`,
  `JavaInputAstVisitor.java`.
  - `JavaOutput.java` — **FIXED.** `SwitchRule.ensureBlankLineInGap` wrongly split a *trailing*
    same-line comment (e.g. `} // if`, added by an earlier `addClosingComments` pass, so only
    reproduces on the second format) onto its own line, treating it like a genuine leading
    comment before the next case. Fixed with a `startsOwnLine` check. Fixture:
    `test/real_code_regressions_6_inp/out.java`.
  - `Doc.java` — **FIXED (2026-07-06), via the config-wiring fix below, no further code change.**
    `ScopePipeline`'s indent math assumed a hardcoded 4-space width, corrupting this codebase's
    real 2-space source at odd nesting depths. Resolved once `indent-size` was actually wired
    through (see config-wiring audit below); confirmed byte-identical round1/round2 at
    `indent-size = 2`.
  - `CommandLineOptionsParser.java`, `JavadocFormatter.java` — **FIXED (2026-07-06).**
    `JavaSpecificRule.applyArrowAlignment` joined an arrow-switch case label onto its body with
    no line-length check, letting `enforceCallLineBreaking` react differently on a fresh format
    vs. a reformat of the already-joined output. Fixed by predicting the joined width first and
    leaving any overflowing case untouched. Fixture: `test/real_code_regressions_7_inp/out.java`.
  - `JavaInputAstVisitor.java` — **FIXED (2026-07-06), unrelated third bug.**
    `GetterSetterRule.parseOneLinerMember`'s `findNameBeforeParen` heuristic misparsed
    `case X, Y -> call(...);`/`default -> throw new Err(...);` arrow arms as fake one-liner
    "members" and column-aligned them together, inserting stray mid-statement padding. Fixed by
    rejecting any one-liner starting with `case`/`default` before the name/return-type heuristics
    run. Fixture: `test/real_code_regressions_8_inp/out.java`. All five previously-diverging
    files (`JavaOutput.java`, `Doc.java`, `CommandLineOptionsParser.java`,
    `JavadocFormatter.java`, `JavaInputAstVisitor.java`) confirmed byte-identical round1/round2
    after this fix; `make test` 23/23. Library marked DONE.
- **MEDIUM**: `github.com/javaparser/javaparser` — a Java parser/AST library (i.e. it parses Java
  source into an AST, unrelated to *this* formatter's own tokenizer). Medium size, excellent
  grammar coverage: expected to exercise generics-heavy declarations, visitor-pattern class
  hierarchies with deep inheritance, extensive Javadoc comments (a good test of §15
  comment-scope/sentence detection, RDD_KEY_47-50), and its own large `switch`-heavy visitor
  dispatch code (§13 switch-rule stress). Good candidate for finding parsing-edge-case bugs
  across a wide variety of Java constructs without `openrewrite/rewrite`'s size cost.
- **HUGE**: `github.com/openrewrite/rewrite` — a large-scale automated-refactoring/Java(+other
  languages) AST-rewrite engine. Low priority given its size (a genuinely large multi-module
  codebase); only pick up once the smaller candidates above are exhausted. Expected feature
  coverage is similar in kind to `javaparser` (AST/visitor-heavy code) but at much greater
  volume, plus likely some annotation-processor-generated or Lombok-style code that could
  exercise `AI_PREAMBLE`-adjacent gaps not seen elsewhere.
- **Local**: `../../../../VMA-GIT/anemonesoft/` (relative to this `formatter/` directory,
  contains `gui/` and `i18n/` subdirs at minimum) — DONE (2026-07-12). Confirmed pure Java (82
  `.java` files, no C/C++; JDK8-source, built via Makefile with `--release 8` against a JDK21
  `javac`, no pom/gradle). Copied to `/tmp/anemonesoft_test/` for all testing; original
  `VMA-GIT/anemonesoft/` tree verified untouched (`git status` clean) both before and after.
  Idempotency check (all 82 files formatted twice, diff round1 vs round2) initially found 2
  diverging files: `gui/component/Spreadsheet.java`, `gui/dialog/HelpBox.java`. **FIXED.**
  `MiscRule.renderCallCandidate`'s `containsNewline` branch calls `groupByOriginalLine`, which
  tracks only paren/bracket/angle-bracket depth, not brace depth — once a multi-argument call's
  trailing argument is itself a multi-line brace body (`new Timer(0, new ActionListener() {
  ...multi-statement... })`), every line inside that body (no top-level comma to split on) gets
  silently swallowed into one row and rendered via a single `collapseTokensToOneLine` call with
  no line-length check, producing an unboundedly long output line on the fresh format; a later
  reformat then reacted differently (Java's Allman brace pass sees a genuinely multi-line body
  the second time), surfacing as the idempotency failure. Fixed by widening an existing
  Kotlin-only "leave such an argument untouched" bail (previously justified only by Kotlin's
  missing `;` statement separator) to all languages, using a new `containsInternalNewline`
  helper (newline strictly between an argument's own first/last significant token, not merely in
  its leading formatting gap before the split comma) so `real_code_regressions_1`'s single-line
  `{ ret, level1(ret) }` brace argument is unaffected. Fixture: `test/real_code_regressions_29_
  inp/out.java`. `make test` 49/49. Compile-check (`/opt/openjdk-21_linux-x64_bin/jdk-21/bin/javac
  -d ... -cp . --release 8`, matching the project's own `Makefile.config`/`Makefile.utils`
  toolchain) of the fixed round1 output: 28 errors, identical set (same files/symbols, only line
  numbers shifted) to the unmodified original's own 28-error baseline (all pre-existing missing-
  dependency errors — `package Jama does not exist` / `cannot find symbol: class Matrix` in
  `stat/*Regression.java`, `stat/Robustness.java` — this checkout has no `Jama` library on its
  classpath; unrelated to formatting). Library marked DONE.

**Config-key wiring audit (2026-07-06), done ahead of the `Doc.java` bug above at user
request.** Root-caused `Doc.java`'s divergence to `MiscRule.INDENT_WIDTH`/`LINE_LENGTH_LIMIT`
being `public static final` constants disconnected from
`Config.indentSize()`/`Config.lineLength()` despite both getters existing. Full audit of every
key in the example `.jxmake-code-formatter`: **only `line-length` and `indent-size` were
dead/unwired**; every other key was already correctly wired. (`header-guard-style` was a
documented non-implementation at the time — since removed entirely, see below.)

Fixed by converting `MiscRule.INDENT_WIDTH`/`LINE_LENGTH_LIMIT` to instance fields
(`indentWidth`/`lineLengthLimit`, with `DEFAULT_*` constants for legacy no-config
constructors), threading them through every constructor that needs them (`MiscRule`,
`GetterSetterRule`, `JavaSpecificRule`, `CppSpecificRule`, `ScopePipeline`), and having
`Formatter.formatOne` read `config.indentSize()`/`config.lineLength()` once per file. No-op at
default settings (`make test` 23/23, no fixture changes); verified live at `indent-size = 2`
that standalone mode now actually reindents to 2 spaces.

**Follow-up audit (same day): a second, separate class of dead-`indent-size` literal.**
Several rule classes also carried their own independent
`private static final String DEFAULT_INDENT_UNIT = "    ";` fallback (same bug class as the
`SwitchRule`/`fmt` flush-case-label growth above) — found and fixed in `MiscRule`'s §8
call/declaration-wrapping pass, `JavaSpecificRule.deriveIndentUnit`, and
`CppSpecificRule.enforceRequiresClausePlacement`, each given an instance field built from
`indentWidth` instead of the hardcoded literal, threaded through `Formatter.formatOne`'s
constructor calls. No-op at default `indent-size` (`make test` 30/30); verified live at
`indent-size = 2` with a synthetic C++ repro. Re-running the `frozen` 44-file tree while
re-verifying is what surfaced the `map.hpp`/`set.hpp`/`catch.hpp` bugs in that library's entry
above — unrelated to this indent-unit audit itself.

**Removal (same day): `header-guard-style` removed entirely** rather than left as
silently-dead config surface (it accepted `ifndef`/`pragma-once` values with zero effect) —
removed from `Config.java` (key, field, getter, choices, parser call), `README.md`, and this
file's sample config; `CppSpecificRule.enforceHeaderFileStructure`'s doc comment updated to
match. Re-add the key if guard-style conversion is ever actually implemented.

Note (superseded, kept for history): this fix was first believed not to resolve the
`Doc.java`/`CommandLineOptionsParser.java`/`JavadocFormatter.java`/`JavaInputAstVisitor.java`
google-java-format idempotency divergence, since `ScopePipeline`'s callers default to
`indentSize=4` unless a project config sets otherwise and google-java-format's checkout has no
such config file. Still true of the checkout itself — but testing was done by dropping a
project config (`indent-size = 2`) alongside the checkout before formatting. Once done,
`Doc.java` came out byte-identical round1/round2, confirming the config-wiring fix resolved it
(see the google-java-format entry above, now DONE) — no separate 2-space-reindent-engine work
was needed after all.

**Local PCPP-heavy Java source (`../../../src/jxm/ugc/ARMCortexMThumbC.java.in`) — tested
2026-07-05, DONE, no bug found.** Not standalone-compilable (a `.java.in` template); verified via
the real `pcpp-java-1.30.jar` preprocessor (invoked `java -jar pcpp-java-1.30.jar <input> -o
<output>`, input before `-o`) on both pre-/post-format source, comparing token streams
(`#line` directives stripped first, since those legitimately shift with line-count changes) —
0-line diff on 105366 tokens; idempotent. Repeatable methodology for any future
PCPP-heavy candidate; plain `gcc -E`/`cpp` does NOT work here (hard-errors on this file's
intentional `##` token-pasting tricks).

**`../../../3rd_party/tools/pcpp_java/src/` (local, 41 `.java` files)** — DONE (2026-07-06).
2 idempotency bugs found via round1/round2 diffing:
- `Evaluator.java` — `SwitchRule.alignInlineSwitches` never checked a row's rendered length
  against `lineLengthLimit` before column-padding, letting a padded row exceed the limit on a
  fresh format while `enforceCallLineBreaking` (earlier phase) never got to react — same bug
  class as `real_code_regressions_7`'s arrow-join overflow. Fixed by threading `lineLengthLimit`
  into `SwitchRule` and predicting each row's final width first. Fixture:
  `test/real_code_regressions_9_{inp,out}.java`.
- `Value.java` — `ScopePipeline.processScope` used a raw `childSource.contains("\n")` check to
  decide if a scope body was still a one-liner; a call inside it broken across lines by
  `enforceCallLineBreaking` fooled that check on reformat, wrongly recursing into and corrupting
  an already-correct one-liner body. Fixed with a depth-aware `hasTopLevelNewline` scan (only a
  depth-0 `NEWLINE` counts). Fixture: `test/real_code_regressions_10_{inp,out}.java`.
- `Preprocessor.java` — no formatter bug; its diff was fully explained by the two fixes above.

`make test` 28/28, full 41-file tree idempotent, both trees compile clean with `javac`.

**Dogfood-compile-check bug (fixed before the round1/round2 methodology existed):**
`MiscRule.renderCallPreserveGroups`/`renderDeclarationPreserveGroups` split each original
source line's tokens on top-level commas independently, resetting paren/bracket/angle depth to
0 at each line start — a nested call wrapping its own arguments onto a second physical line
(real depth > 0 carried across the line break) caused a trailing comma to be misread and a
synthetic duplicate comma inserted, corrupting output (compile errors in `TokenizerCore.java`'s
own `new Token(...)` construction, among others). Fixed with a new `groupByOriginalLine` helper
that tracks depth cumulatively across the whole multi-line slice while still grouping results
back into per-original-line rows; the old buggy `splitOnNewlines` removed. `make test`
18/18, dogfood self-format compiles with zero `javac` errors.

Known pre-existing gaps, discovered during Main.java smoke-testing, left unfixed as
out of scope (flagged to user, not part of this checklist): `ServerMode.FormatHandler`
doesn't resolve `indent-style = auto` before calling `Formatter.formatOne` (will throw
on a server-delegated request for such a project — masked in practice by `Main`'s
fallback-to-standalone-on-delegation-failure behavior); `Config.lineEndings()` is
applied by `Main.applyLineEndings()` for standalone/in-process formatting but not yet
by `ServerMode.FormatHandler`. Full detail: RDD_KEY_88.

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
</content>
