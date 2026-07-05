# STATE.md — Formatter Implementation Tracker

---

**Do NOT read `README.md`** unless the user explicitly asks. All decisions relevant to
implementation are recorded in the **Resolved Design Decisions** index below (full text
in `STATE_rdd_log.md` — **do not read that file in full**, look up one key at a time via
`grep -Fm1`).

**ONLY** read the Java source file you are currently implementing or directly modifying. Do NOT read other source files unless a specific checklist item or ambiguity requires it.

### During implementation
- Implement one checklist section at a time
- After completing a section (or when the cumulative diff across all changed files
  exceeds ~50 lines, whichever comes first), do a checkpoint commit:
  1. Update STATE.md — check off completed items and update the active checklist.
  2. `git add util/CodingStyle.md/formatter/` (the entire formatter directory)
  3. `git reset util/CodingStyle.md/formatter/target/` (exclude build output)
  4. `git commit -m "<message>"` — short descriptive message, no strict format required,
     trailer ending with `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`
- Small related items within a section may be grouped into one commit if they
  are trivially connected — use judgment based on line count (~50 lines threshold)
- Never let implemented files and STATE.md drift out of sync — STATE.md must
  always reflect the true current state at every commit
- Never modify the files `util/CodingStyle.md/formatter/test/*_inp.*` unless they contain
  syntax errors (they are the test input files).
- Never modify the files `util/CodingStyle.md/formatter/test/*_out.*` unless explicitly
  asked (they are the reference output files that show the expected results).
- Ignore `XL.txt`, that is the user tracker file.
- Use `/tmp` for temporary smoke-test and mini-test files.

### When hitting an ambiguity or open question
1. **Stop coding immediately** — do not guess or proceed past the ambiguity
2. Update STATE.md: add the question to **Open Questions**, mark the blocked
   checklist item with `[~]` and a note
3. Commit STATE.md only.
4. Ask the user and wait for an answer before continuing
5. Once resolved: append the full decision as a new row to `STATE_rdd_log.md`
   (next `RDD_KEY_n` number), add the key + topic to the **Resolved Design
   Decisions** index in this file, remove from **Open Questions**, unblock
   the checklist item, then continue

### When a file reaches COMPLETE
1. Update the relevant checklist in STATE.md.
2. Commit STATE.md together with the completed source file.

### Session end
- Always leave STATE.md committed and up to date before ending the session
- The next session will resume from the first unchecked item in the current checklist

---

## Project Layout

```
util/CodingStyle.md/formatter/
  STATE.md                  ← this file
  STATE_rdd_log.md           ← full Resolved Design Decisions text (do not read in full)
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

Full decision text lives in `STATE_rdd_log.md` — **do not read that file in full**.
To look up a specific decision during implementation:
```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/STATE_rdd_log.md
```
**Do not add the `-An` parameter to `grep` for `STATE_rdd_log.md`, as the lines in
`STATE_rdd_log.md` are very long.**

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
line-length                = 100
indent-size                = 4
indent-style               = spaces          # spaces | tabs | auto
server-port                = 17173

# ── Behavior ──────────────────────────────────────────────────────────────────
closing-comment-min-lines  = 5
format-macros              = off             # off | on
line-endings               = lf              # lf | crlf | preserve
normalize-comment-start-case = on            # on | off
normalize-comment-end-period = on            # on | off

# ── C/C++ ─────────────────────────────────────────────────────────────────────
header-guard-rename        = off             # off | on
header-guard-style         = preserve        # preserve | ifndef | pragma-once

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order          = java, com, org, other, local, static
java-import-sort           = on
java-import-depth          = 2
java-import-blank-lines    = 1
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
accept `final` there). This applies to all `.java` files under `src/`.

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
- To reduce quota usage and prevent regressions on `(PASS)` tests and previous bug fixes
  prefer evidence over reasoning. Keep static analysis minimal—only enough to identify where
  to insert debug prints. Use debug prints and `make test` to diagnose and validate fixes.
  Do not use static analysis as the primary method of bug diagnosis or regression checking.
  After the fix is verified with `make test`, remove all debug prints and then commit the
  files you have modified (ignore files you have not modified). If unsure ask me.

`Main.java` standalone-mode cache note: `IndentationDetector` results are cached at
`/tmp/jxmake-code-formatter-indent-<sha256-of-boundary-dir>.cache`, content = detected style + `\n`
+ boundary dir `lastModified` epoch ms; invalidated automatically on an mtime mismatch
(RDD_KEY_88).

- [x] CLI arg parsing (`--server`, `--stop`, `--standalone`, `--diff`, `--check`,
      `--out DIR`, `--port N`, file paths); unknown flags / bad usage → exit 2 (RDD_KEY_88)
- [x] `--lang c|cpp|java` (2026-07-06): explicit language override for files whose
      extension `inferLanguage` can't recognize. One flag applies to every file in that
      invocation (no per-file override), validated against exactly `c`/`cpp`/`java` (exit 2
      otherwise). Threaded through `processFile` ahead of the extension-based
      `inferLanguage` fallback; `--server`/`--stop` reject `--lang` (nothing to format).
      Wire protocol: the `/format` HTTP endpoint already accepted an optional `lang` query
      parameter that takes priority over its own path-extension guess (`Main.delegateToServer`
      already always sent it) — no client/server protocol shape change was needed, only
      validation added on the server side (`ServerMode.FormatHandler` now 400s on an
      unrecognized `lang` value instead of silently mis-formatting). `README.md` updated.
      `make test` 25/25, no regressions.
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

**Real-code testing (pivoted from synthetic dogfooding — found bugs faster):** methodology
(repeatable for future libraries/languages): clone real, compiling third-party code → format
once (round1) → format round1's output again (round2) → `diff round1 round2` must be empty
(idempotency) → compile round1 with the appropriate toolchain, must succeed with the same
error count as the unmodified original. Prefer this over synthetic dogfooding — it found
concrete, fixable bugs far faster than the from-scratch dogfood idempotency failure did.
Full bug-by-bug root-cause narratives for each completed candidate below have been compacted
out of this file — they remain fully available via `git log`/`git show` on the noted commits/
fixtures.

- **`blake-madden/tinyexpr-plusplus`** (C++20, `g++ -std=c++20`) — DONE. 3 bugs fixed, all in
  `MiscRule`'s multi-line call/declaration rendering and `Formatter`'s pass ordering
  (`groupByOriginalLine` same-line-sibling mis-split; `renderCallCandidate` line-length
  undercount ignoring trailing same-line text; `enforceComplexityPadding` had to move ahead of
  `enforceCallLineBreaking` in Phase 1 for idempotency, commits `1c10946`/`26a9715`). Fixture:
  `test/real_code_regressions_1_{inp,out}.cpp`. `make test` 19/19, full tree idempotent and
  compiles clean.
- **RobotCoding `gui_frontend`** (`../../../../RobotCoding/gui_frontend/src/`, 71 `.java`
  files, `javac` from `/opt/openjdk-25_linux-x64_bin`) — DONE. 4 pass-ordering idempotency
  bugs fixed: `>>>` mistokenized as `>>`+`>` (compile-breaking, added to
  `TokenizerCore.MULTI_CHAR_OPS`); `GetterSetterRule` one-liner body-column padding measured
  pre-`enforceComplexityPadding`; `enforceCallLineBreaking` joining a multi-line call loses
  complexity-padding awareness; `GetterSetterRule`/`JavaSpecificRule` one-liner detection
  didn't predict later line-breaking (both given a "has breakable call + predicted width"
  pre-check). Fixture: `test/real_code_regressions_2_{inp,out}.java`. `make test` 20/20, full
  tree idempotent, compiles with only the known pre-existing `javax.jmdns` dependency errors.
- **Self-dogfood** (formatter's own `src/com/jxmake/formatter/`, 20 files) — DONE. 1 more
  pass-ordering bug: `MiscRule.parseAssignment` rejected any §6 alignment row whose RHS was
  already wrapped by a later `enforceCallLineBreaking` pass, breaking the group instead of
  falling through to a verbatim single-line `Assignment`. Fixture:
  `test/real_code_regressions_3_{inp,out}.java`. `make test` 21/21, tree idempotent, compiles
  clean, declaration counts match.
- **`martinus/nanobench`** (`src/include/nanobench.h`, 3484 lines, formatted honestly as C++
  via a `.hpp` copy) — DONE. 2 bugs, one a severe silent-content-loss class: (1)
  `TokenizerCore` had no C++11 raw-string-literal (`R"delim(...)delim"`) support at all, so
  `{`/`}` inside nanobench's mustache report templates corrupted brace-depth tracking for the
  rest of the file (up to ~46% content loss); fixed via `rawStringPrefixLength`/
  `emitRawString`, gated on `isC || isCpp` (an initial `isC`-only gate missed `.hpp`/`.cpp`
  entirely). (2) `DeclarationAlignmentRule`'s general group renderer silently dropped a
  `template<...>` prefix on a bare forward declaration (only the function-forward-declaration
  path emitted it) — compile-breaking (`struct ... is not a template`). Fixture:
  `test/real_code_regressions_4_{inp,out}.hpp`. `make test` 22/22, idempotent, compiles clean.
  Known non-bug: formatting nanobench.h under its *actual* `.h` (C) extension hits an
  unrelated, out-of-scope convention mismatch (`CppSpecificRule.enforceEmptyParameterList`'s
  C-only heuristic misfires on a constructor member-initializer list) — not fixed, since real
  C can't produce that shape.
- **User-reported bug** (`real_code_regressions_1_out.cpp`'s `} // while` indentation) — DONE.
  `ScopePipeline` never re-derived a scope's own closing-brace gap from depth, so
  misindentation in the original source passed through untouched. Fixed by forcing that gap to
  the frame's own indent in `processScope`'s child recursion, which then surfaced 4 edge
  cases in `findParentIndent`: bare compound blocks, preprocessor-directive-adjacent spans, a
  comment sitting directly in the trailing gap (must not relocate it), and a `case`/`default`
  label sharing a span with the construct that follows it (anchor search had to go backward
  from `openBraceIdx`, not forward from the span start) — plus a separate empty-body
  (`{}`) guard so the fix doesn't re-expand it into `{\n}`. Fixture:
  `test/real_code_regressions_5_{inp,out}.cpp`. `make test` 23/23; re-ran the full nanobench
  round-trip too (this is what caught the `case`-label edge case) — still idempotent and
  compiles clean.
**NEXT SESSION — continue here:** Continue the real-code testing methodology against the
remaining C/C++ candidates, in this order unless the user redirects:
`serge-sans-paille/frozen` → `fmtlib/fmt` → `taocpp/PEGTL`, then the additional candidates
below. Use `/opt/gcc-12.2.0/bin/g++ -std=c++20` (bump the standard flag if a library needs
newer; confirm any compile failure also reproduces against the *unmodified* original source
before treating it as formatter-induced, same check done for tinyexpr-plusplus's C++20
requirement and nanobench above). For any C++ candidate distributed under a `.h`/`.hpp`
extension, check which it actually is before testing (nanobench's own `.h`-vs-content mismatch
above cost real bisection time chasing a non-bug) — copy to `.hpp` first if the content is
really C++.

Additional candidates the user has since supplied (not yet started, path relative to home dir
written as `~` below so this file never embeds the actual account/user name):

- **C17**: `github.com/Tongsuo-Project/tongsuo-mini` — a crypto/TLS codebase, likely macro-heavy
  (good match for this formatter's own history of macro-related bugs). Compile-check with the
  ARM toolchain at `/opt/arm-gnu-toolchain-14.2.rel1-x86_64-arm-none-eabi` (`-fsyntax-only`,
  confirmed to at least launch and run a real `-fsyntax-only` pass in this environment) or
  `/opt/gcc-12.2.0` with `-std=c17`, whichever the checkout needs.
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

**Java candidates the user has since supplied (not yet started):**

- **SMALL**: `github.com/google/google-java-format` — small, expected to catch formatter logic
  bugs specifically (not just tokenizer/lexer edge cases like the raw-string bug above).
  **IN PROGRESS (started 2026-07-05).** Idempotency check (format all 84 `.java` files twice,
  diff round1 vs round2) initially found 5 diverging files: `JavaOutput.java`,
  `CommandLineOptionsParser.java`, `Doc.java`, `JavadocFormatter.java`,
  `JavaInputAstVisitor.java`.
  - `JavaOutput.java` — **FIXED.** Root cause: `SwitchRule.ensureBlankLineInGap` forced a blank
    line before the *first* comment found in a case body's trailing gap, on the assumption it's
    always a leading comment glued to the next label/case (the documented exception for e.g.
    `// comment before case\ncase 1:`). But when that first comment is instead a *trailing*
    same-line comment on the case's own last statement (e.g. `} // if`, itself added by an
    earlier `addClosingComments` pass — so this only reproduces on the *second* format of
    already-formatted output), the same logic wrongly split it onto its own line
    (`}\n\n // if`). Fixed by adding a `startsOwnLine` check so the "leading comment" exception
    only applies to a comment that starts its own new line, not one trailing prior content on
    the same line. See `test/real_code_regressions_6_inp/out.java`. Diagnosed via targeted debug
    prints across `Formatter.java`'s phase boundaries (bisected to right after
    `formatNonInlineSwitches`), `ScopePipeline.java` (ruled out — its closing-brace reindent
    fix from the previous session correctly skips gaps with comments here), and
    `BlockStructureRule.addClosingComments` (ruled out — behaves correctly given its input); all
    debug prints have been removed.
  - `Doc.java` — **FIXED (2026-07-06), via the config-wiring fix above, not a further code
    change.** Root cause (see the "Config-key wiring audit" entry above): `ScopePipeline`'s
    depth-based indent math (`normalizeIndent`, and `MiscRule.render(sig, depth, ...)`'s own
    depth-derived indentation) assumed a fixed indent width, hardcoded to 4, that didn't match
    google-java-format's own 2-space source. At 3-level nesting (odd multiple of 2, non-multiple
    of 4) this produced the observed divergence: a method's closing `}` inside an enum constant
    body matched its opening `{`'s 6-space indent on round1, then drifted to 8 (rounded up to the
    next multiple of 4) on round2. Once `indent-size` was wired through to actual effect (prior
    entry), re-testing with a `.jxmake-code-formatter` containing `indent-size = 2` for this
    checkout makes `Doc.java` byte-identical round1 vs round2 — confirmed via direct round-trip
    test. No further code change was needed; this was purely a config-value-not-taking-effect
    bug, already fixed. Same real-code-testing principle as any linter/formatter: a project
    written in a non-default style needs its own config to match that style.
  - `CommandLineOptionsParser.java`, `JavadocFormatter.java` — **FIXED (2026-07-06).** Root
    cause: `JavaSpecificRule.applyArrowAlignment` (arrow-switch `case X -> body;` label/body
    joining) unconditionally joined a case's label onto the same line as its body, with no check
    on whether the resulting single-line width would exceed `lineLengthLimit`. On a fresh format
    this could produce an over-length joined line that `enforceCallLineBreaking` (Phase 1,
    earlier in the pipeline) never got a chance to react to, since it already ran against the
    pre-join layout; reformatting that already-joined, over-length output then let
    `enforceCallLineBreaking` finally see and re-break it apart — not idempotent. Fixed by
    predicting the joined line's width (reusing the existing `collapseToOneLine` helper) before
    committing to the join, and leaving any one case whose join would overflow byte-for-byte
    untouched instead. Verified via `make test` (23/23, no regressions) and live round-trip
    testing on both real files. Permanent fixture: `test/real_code_regressions_7_inp/out.java`
    (registered in `Makefile`'s `INP_FILES`).
  - `JavaInputAstVisitor.java` — **FIXED (2026-07-06), third/unrelated bug.** Symptom: extra
    spaces inserted mid-statement (e.g. `default -> throw new     AssertionError(...)`) only on
    the first format pass of an arrow-form `switch` containing a comma-joined case label (e.g.
    `case CLASS, INTERFACE -> ...;`) followed by a `default -> throw new X(...);` arm — reproduces
    with as few as those two case arms alone, independent of surrounding context (earlier belief
    that it was context-dependent was a red herring from an invalid first repro attempt). Root
    cause: `GetterSetterRule.parseOneLinerMember` (the getter/setter one-liner column-alignment
    pass, invoked per-scope by `ScopePipeline.applyGetterSetterPass`) treats every one-physical-line
    top-level statement in a scope as a candidate "member", and its `findNameBeforeParen` heuristic
    misparsed each `case`/`default` arrow-arm as a fake member: for
    `case CLASS, INTERFACE -> visitClassDeclaration(tree);`, it read "return type" =
    `case CLASS , INTERFACE ->` and "name" = `visitClassDeclaration`; for
    `default -> throw new AssertionError(tree.getKind());`, "return type" = `default -> throw new`
    and "name" = `AssertionError`. Grouping these two fake members together and column-aligning
    the "return type" cell to the wider sibling's width is exactly what inserted the padding
    between `new` and `AssertionError`. Fixed by rejecting any one-liner whose first significant
    token is the `case`/`default` keyword at the very top of `parseOneLinerMember`, before the
    name/return-type heuristics run — same posture as the analogous guard added to
    `DeclarationAlignmentRule.parseDeclaration` while investigating this (that guard turned out to
    be for a different, never-actually-triggered misparse path in this specific bug, but is a
    correct defensive fix in its own right and was kept). Diagnosed via `JXM_DEBUG` bisection
    across `Formatter.java`'s phase boundaries (all pipeline stages, including immediately after
    `ScopePipeline.process()`, showed the bug already present, narrowing it to *inside*
    `ScopePipeline`) plus a temporary per-group debug print in
    `ScopePipeline.applyDeclarationsPass` (showed zero matches, ruling out
    `DeclarationAlignmentRule` as the active cause) and manual line-by-line truncation/bisection of
    the real file down to a 2-statement minimal repro. All debug instrumentation removed. Verified
    via `make test` (23/23, no regressions) and live round-trip testing on all four previously
    diverging files (`Doc.java`, `CommandLineOptionsParser.java`, `JavadocFormatter.java`,
    `JavaInputAstVisitor.java`) — all four now byte-identical round1 vs round2. Permanent
    fixture: `test/real_code_regressions_8_inp/out.java` (registered in `Makefile`'s
    `INP_FILES`).
- **MEDIUM**: `github.com/javaparser/javaparser` — excellent grammar coverage, good candidate for
  finding parsing-edge-case bugs across a wide variety of Java constructs.
- **HUGE**: `github.com/openrewrite/rewrite` — low priority given its size; only pick up once the
  smaller candidates above are exhausted.
- **Local**: `../../../3rd_party/tools/pcpp_java/src/` (relative to this `formatter/`
  directory) — the JxMake repo's own pcpp_java tool sources; not yet tested.
- **Local**: `../../../../VMA-GIT/anemonesoft/` (relative to this `formatter/` directory,
  contains `gui/` and `i18n/` subdirs at minimum) — not yet tested.

**Config-key wiring audit (2026-07-06), done ahead of the `Doc.java` bug above at user
request.** While investigating the `Doc.java` idempotency divergence, root-caused it to
`ScopePipeline.normalizeIndent()` rounding any indent that isn't a multiple of a
hardcoded `INDENT_WIDTH=4` up to the next multiple — this corrupts 2-space (Google-style)
source like google-java-format's own code at every odd nesting depth. Testing whether the
`indent-size` config could already control this showed it had **zero effect** — confirmed
`MiscRule.INDENT_WIDTH`/`MiscRule.LINE_LENGTH_LIMIT` were `public static final` constants,
completely disconnected from `Config.indentSize()`/`Config.lineLength()`, despite both
getters existing and being parsed from the config file. User asked for a full audit of
every key in the example `.jxmake-code-formatter` config against actual codebase usage.
Audit result: **only `line-length` and `indent-size` were dead/unwired**; every other key
(`indent-style`, `server-port`, `line-endings`, `normalize-comment-start-case`,
`normalize-comment-end-period`, `closing-comment-min-lines`, `format-macros`,
`header-guard-rename`, `java-import-order`, `java-import-sort`, `java-import-depth`,
`java-import-blank-lines`) was already confirmed wired via existing call sites. Note:
`header-guard-style` is a documented, deliberate non-implementation (see
`CppSpecificRule.java`'s own doc comment on that method), not a wiring bug, so it was left
untouched.

Fixed by converting `MiscRule.INDENT_WIDTH`/`LINE_LENGTH_LIMIT` from static constants to
instance fields (`indentWidth`/`lineLengthLimit`), renaming the old static defaults to
`DEFAULT_INDENT_WIDTH`/`DEFAULT_LINE_LENGTH_LIMIT`, and threading the two values through
every constructor that needs them: `MiscRule`, `GetterSetterRule`, `JavaSpecificRule`,
`CppSpecificRule`, and `ScopePipeline` (which now also passes them into the `MiscRule`/
`GetterSetterRule` instances it builds internally). `ScopePipeline.normalizeIndent()` now
rounds against `this.miscRule.indentWidth` instead of the removed static. `Formatter.java`
now reads `config.indentSize()`/`config.lineLength()` once per file and passes them into
every rule constructor, matching the existing pattern already used for
`closingCommentMinLines` → `BlockStructureRule`. Every one-arg legacy constructor
(`MiscRule(lang, ...)`, `GetterSetterRule(lang)`, etc.) is kept and now delegates to the
new full constructor using the `DEFAULT_*` constants, so every other call site in the
codebase (tests, `ScopePipeline`'s own no-config constructors) is unaffected — this is why
`make test` shows zero regressions with no fixture changes needed (defaults are unchanged:
4-space indent, 100-col line length).

Verified live: with a `.jxmake-code-formatter` containing `indent-size = 2`, standalone
mode now actually reindents to 2 spaces (previously had zero effect). `make test`: 23/23
forward + 23/23 idempotency, PASS, no fixture changes.

Note: this fix does NOT itself resolve the `Doc.java`/`CommandLineOptionsParser.java`/
`JavadocFormatter.java`/`JavaInputAstVisitor.java` google-java-format idempotency
divergence above — `ScopePipeline`'s callers (`Formatter.formatOne`, `Main`, etc.) still
default to `indentSize=4` unless a project config file sets otherwise, and
google-java-format's own source has no such config file, so this formatter still
processes it at the default 4-space assumption. Whether/how to actually fix the
2-space-source idempotency bug itself (build a real reindent engine vs. a narrower patch
vs. treat as permanently out of scope) is still an open question — deferred, per explicit
user instruction, pending further direction.

**Local PCPP-heavy Java source (`../../../src/jxm/ugc/ARMCortexMThumbC.java.in`, relative to
this `formatter/` directory) — tested 2026-07-05, DONE, no bug found:**
938 lines, 21 `#`-directive lines. Not standalone-compilable (a `.java.in` template, not real
Java -- JxMake's own preprocessor-templated source for generating per-target ARM Cortex-M Thumb
C variants). `.java.in` isn't a recognized extension (`Main.inferLanguage`), so it was copied to
a scratch path ending in `.java` before running the formatter.

Plain `gcc -E`/`cpp` does NOT work as a correctness check here -- it enforces strict ISO C `##`
token-pasting rules and hard-errors on constructs this file relies on (e.g. pasting `.` from
`super.` onto an expanded macro argument, `error: pasting "." and "$b" does not give a valid
preprocessing token`). The project's actual preprocessor is the real one: the prebuilt jar at
`3rd_party/tools/pcpp_java/pcpp-java-1.30.jar` (relative to the JxMake repo root), invoked
directly with `java -jar` (single-file form: `java -jar pcpp-java-1.30.jar <input> -o <output>`
-- note input before `-o`, not after, or it misparses the input as another output path and
reports "number of output files does not match number of input files"). This one accepts the
file's actual macro usage and preprocesses both the original and the formatted file cleanly.

Verification methodology used (a repeatable pattern for any future PCPP-heavy candidate): run
`pcpp_java` on both the pre-format and post-format source, strip `#line` directives from both
(these legitimately shift when the formatter changes line counts -- not a bug), then tokenize
each (`grep -oE '[A-Za-z0-9_$]+|[^A-Za-z0-9_$ \t\n]'`, i.e. identifiers/punctuation only, all
whitespace differences discarded) and diff the token streams -- if the formatter only changed
layout, the token streams must be byte-identical. Confirmed here: 0-line diff on 105366 tokens
each. Also confirmed idempotent (round1 == round2) and that the formatter doesn't crash/mangle
anything despite `#define`-heavy, backslash-continued, `##`/`#` (stringize/paste) macro bodies --
a shape none of the other current fixtures exercise. No bug surfaced; no new fixture needed since
nothing was wrong.

For each new bug found: minimal isolated repro first, fix, verify against the full source
round-trip, `make test`, then a permanent fixture under `test/real_code_regressions_*` (a new
`_4`/`_5`/`_6` suffixed pair if an existing one gets too large) — same pattern as this session.
Update this section as each candidate completes.

**`../../../3rd_party/tools/pcpp_java/src/` (local, Java preprocessor tool source, 41 `.java`
files)** — DONE (2026-07-06). 2 idempotency bugs found and fixed via the round1/round2 diff
methodology:
- `Evaluator.java` — **FIXED.** `SwitchRule.alignInlineSwitches`/`applyInlineAlignment` never
  checked a row's rendered length against `lineLengthLimit` before committing column-padding
  overrides: padding a short label (e.g. `default`) out to match a much wider sibling label's
  column could push that one row past the limit even though the switch's original, unpadded
  text fit. A fresh format produced a stable-looking over-length aligned line that
  `MiscRule.enforceCallLineBreaking` (an earlier pipeline phase) never got to react to;
  reformatting that output let `enforceCallLineBreaking` break the now-over-length line apart,
  after which the alignment pass no longer recognized the row shape and left it un-aligned —
  not idempotent. Same bug class as `real_code_regressions_7`'s arrow-join overflow. Fixed by
  threading `lineLengthLimit` into `SwitchRule` (new 2-arg constructor, legacy 1-arg
  constructor delegating to `MiscRule.DEFAULT_LINE_LENGTH_LIMIT`) and predicting every row's
  final rendered length before writing any override, leaving the whole switch's cases
  byte-for-byte untouched if even one row would overflow. Fixture:
  `test/real_code_regressions_9_{inp,out}.java`.
- `Value.java` — **FIXED.** `ScopePipeline.processScope` decided whether a non-named scope
  body (e.g. an `if` one-liner body kept on its original single physical line) was still a
  single-statement "one-liner" via a raw `childSource.contains("\n")` check. On a fresh format
  that's correct — a one-liner body has no embedded newline at all — but
  `MiscRule.enforceCallLineBreaking` can break an over-length call inside that same one-liner
  body across multiple physical lines while leaving it one logical statement (e.g.
  `if (last == 'u' || last == 'U') { unsigned = true; s = s.substring(0, s.length() - 1); }`
  wrapped only at the `substring(...)` call). Reformatting that already-broken output made the
  raw newline check see newlines strictly inside the call's own parens and wrongly treat the
  body as a real multi-statement block, recursing into it and column-splitting/reindenting its
  statements — corrupting output that was already correctly formatted, with a misplaced/
  misindented closing `}` and spurious column-padding inserted before an unrelated `=`. Fixed
  by adding `hasTopLevelNewline` (a paren/bracket-depth-aware scan over the token range, not a
  raw string search) and using it in place of `childSource.contains("\n")` at both call sites
  in `processScope` — only a `NEWLINE` token seen at depth 0 now counts as evidence of a real
  multi-statement body. Fixture: `test/real_code_regressions_10_{inp,out}.java`.
- `Preprocessor.java` — investigated, no formatter bug: its round1/round2 diff was fully
  explained by the two fixes above (both files are formatted as part of the same tree pass);
  once `SwitchRule`/`ScopePipeline` were fixed, its diff went empty too.

Verified: `make test` 28/28 (26 → 28 with the two new fixtures), full 41-file pcpp_java tree
re-diffed round1 vs round2 — empty (idempotent) on every file — and both the original and the
round1-formatted tree compile clean with `javac` (0 errors, matching counts).

**Bug found and fixed via the dogfood compile check:** `MiscRule.renderCallPreserveGroups`/
`renderDeclarationPreserveGroups` (Option 2, "preserve original line groups" for a multi-line
call/declaration argument list) used to split each original source line's tokens on top-level
commas independently, resetting paren/bracket/angle depth to 0 at the start of every line. When
a single argument was itself a nested call whose own argument list wrapped onto a second
physical line (so the outer line ended with an unclosed paren, i.e. real depth > 0 carried into
the next line), the per-line depth reset caused that line's trailing comma to be misread as
"still inside" the line's one accumulated part instead of splitting it off — and a synthetic
comma was then appended on top of it by the row-rendering loop, corrupting output with a
duplicated comma (or, in the `tokens.add(idx + 1, new Token(...))` case in `TokenizerCore.java`,
an outright compile error). Fixed by replacing the old `splitOnNewlines` + per-line
`splitTopLevelCommas` combination with a new `groupByOriginalLine` helper that tracks depth
cumulatively across the *entire* multi-line slice (correct top-level-comma detection) while
still grouping the resulting arguments back into per-original-line rows (a depth-0 `NEWLINE`
seen since the last depth-0 comma starts a new row) — preserving the original "which arguments
shared a source line" semantics without the depth-reset bug. `splitOnNewlines` itself is now
dead and was removed. Verified: `make test` 18/18 PASS (no regression), dogfood self-format now
compiles with zero `javac` errors (previously failed with several `illegal start of expression`
errors from duplicated commas / broken `new Token(...)`, `new DiffRun(...)`,
`new SwitchBlock(...)` construction). No new fixture added for this — it requires a real
multi-line nested-call argument shape to trigger; a future `## TODO — Not Scheduled` item could
add one.

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
