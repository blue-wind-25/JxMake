# STATE.md — Formatter Implementation Tracker

---

**Do NOT read `FORMATTER_DISCUSSION.md` or `README.md`** unless the user explicitly
asks. All decisions relevant to implementation are recorded in the
**Resolved Design Decisions** index below (full text in `STATE_rdd_log.md` —
**do not read that file in full**, look up one key at a time via `grep -Fm1`).
`FORMATTER_DISCUSSION.md` is design history and future planning only — large, and
contains nothing the implementer needs beyond what is already indexed here.

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
  FORMATTER_DISCUSSION.md
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

**Real-code testing (pivoted from synthetic dogfooding — found bugs faster):**
Rather than continuing to chase the broad dogfood idempotency failure blind, tested the
formatter against a real, compiling third-party C++ codebase:
`https://github.com/blake-madden/tinyexpr-plusplus` (single-header-ish expression parser,
~2900-line `.cpp` + ~1200-line `.h`, requires C++20 for `std::set::contains()` /
`std::ranges::copy` — confirmed by trying `-std=c++17` against the *unmodified* original
source and seeing it also fail, ruling out formatter-induced C++20-isms).
Toolchain used: `/opt/gcc-12.2.0/bin/g++ -std=c++20` (this machine's newer GCC install; the
ARM toolchain at `/opt/arm-gnu-toolchain-14.2.rel1-x86_64-arm-none-eabi` was offered but not
needed here since tinyexpr-plusplus is host-only). Methodology (repeatable for future
libraries): clone with `--depth 1` into a scratch dir → format once (round1) → format the
round1 output again (round2) → `diff round1 round2` (must be empty for idempotency) →
compile round1 with g++ (must succeed with zero errors, same as the original unmodified
source). Other candidates suggested but not yet tried: `martinus/nanobench`,
`serge-sans-paille/frozen`, `fmtlib/fmt`, `taocpp/PEGTL`.

Found and fixed 3 bugs this way, all in `MiscRule`'s multi-line call/declaration rendering
and the `Formatter` pipeline's pass ordering:
1. **`groupByOriginalLine` mis-split of same-line siblings** — a *trailing* newline (before a
   lone closing-paren line) was misread as a new argument's *leading* newline, wrongly
   splitting arguments that originally shared one source line (e.g.
   `static_cast<uint16_t>(val1), static_cast<int>(val2)`) onto separate output lines. Fixed
   with `pendingNewRow`/`currentHasSignificant` state tracking in `groupByOriginalLine` (only
   a newline before a part's first significant token counts as starting a new row).
2. **Length-measurement undercount** — `renderCallCandidate`'s "does it fit" check measured
   only up to the candidate's own closing paren, ignoring trailing same-line text (e.g. an
   outer `static_cast<T>( ... ) );` suffix), undercounting the true line length. Fixed by
   adding `lineEndIndex` (symmetric to the existing `lineStartIndex`) and measuring the full
   physical line.
3. **Pass-ordering idempotency bug (the deep root cause)** — `enforceCallLineBreaking`'s
   "fits within `LINE_LENGTH_LIMIT`" check ran before `enforceComplexityPadding` added loose
   `( x )` spacing, so a borderline-length line could measure as "fits" on a fresh format,
   then grow past the limit once padding was added with no re-check — getting broken only on
   a second format pass (confirmed by measuring: a 99-char source line becomes 103 chars after
   Phase 4 padding, crossing the 100-char limit). A first fix moved `enforceCallLineBreaking`
   itself to run after `enforceComplexityPadding` (commit `1c10946`) — this fixed the
   `rotr`/`rotl` divergence but broke a *different* invariant: `BlockStructureRule
   .addClosingComments`'s line-count threshold (`closingCommentMinLines`, STYLE.md §7) needs
   to see any line-count-*expanding* pass's effect before it decides whether to add a closing
   comment (the same reasoning already applied to `switchRule.alignInlineSwitches` running
   before `addClosingComments` for switch bodies) — pushing call-breaking past
   `addClosingComments` meant a while-loop body whose call got broken across lines only
   crossed the closing-comment threshold on a second pass
   (`te_parser::list`'s while loop). **Final fix** (commit `26a9715`): instead pull
   `enforceComplexityPadding` forward into Phase 1, immediately before
   `enforceCallLineBreaking` (which stays in its original position, ahead of Phase 3) — this
   satisfies both constraints at once.

Verified after the final fix: `make test` 18/18 → 19/19 PASS (added a permanent regression
fixture, `test/real_code_regressions_1_{inp,out}.cpp` (renamed from `real_code_regressions_`
without the `_1` suffix in a later session, once a second-language fixture existed),
distilling all 3 bug shapes), and the
full tinyexpr-plusplus `.cpp`/`.h` pair is now byte-for-byte idempotent (`diff round1 round2`
empty) and compiles clean with `g++ -std=c++20`.

**Lesson for future test-writing sessions:** real, compiling third-party code found 3
concrete, fixable bugs quickly; the earlier from-scratch dogfood idempotency failure (nearly
every self-format file changing on a second pass) was broad and hard to triage by comparison.
Prefer real-code testing over synthetic dogfooding when hunting for formatter bugs.

**Java candidate — DONE (RobotCoding gui_frontend, `../../../../RobotCoding/gui_frontend/src/`,
71 `.java` files, compiled with `/opt/openjdk-25_linux-x64_bin/jdk-25/bin/javac`):** found and
fixed 4 bugs, all pass-ordering idempotency issues (round1 = fresh format, round2 = format of
round1's own output, must be byte-identical):

1. **`>>>` (unsigned right shift) tokenized as `>>` + `>`** — compile-breaking
   (`jcom/winmd/WinMDReader.java`). Fixed by adding `>>>`/`>>>=` to
   `TokenizerCore.MULTI_CHAR_OPS` (longest-prefix-first still respected).
2. **`GetterSetterRule` body-column padding measured pre-padding** (`blocks/Block.java`) —
   a one-liner body's width was measured before `enforceComplexityPadding` had shrunk its
   interior spacing (e.g. `Math.max( 0, lvl )` → `Math.max(0, lvl)`), so a sibling's trailing
   padding went stale by the amount stripped, stable only on a second pass. Fixed by adding a
   pre-pass `enforceComplexityPadding` call in `Formatter.formatOne` before Phase 0's
   `ScopePipeline.process()`.
3. **`enforceCallLineBreaking` joining a multi-line call loses complexity-padding awareness**
   (`gui/BlockDialogs.java`) — a call whose args originally spanned multiple lines has each
   side's spacing gap-blocked from the tight/loose rewrite (a NEWLINE in the gap suppresses
   it); `enforceCallLineBreaking` can later collapse that call onto one line with a plain
   space join, which looks like "loose" padding regardless of whether the args actually
   contain a nested `(`/`[`, stable only on a second pass once there's no NEWLINE left to
   block the rewrite. Fixed by re-running `enforceComplexityPadding` immediately after
   `enforceCallLineBreaking` in `Formatter.formatOne` (both calls documented in-code).
4. **`GetterSetterRule` grouping / `JavaSpecificRule` one-liner Allman-brace-avoidance don't
   predict later line-breaking** (`gui/BlockCanvas.java`, `actionEdit`/`actionCut`/
   `actionCopy`/`actionDelete`) — both `GetterSetterRule.parseOneLinerMember` (rejects a
   candidate via `hasNewlineBetween`) and `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle`
   (via `isSingleLineBody`) decide "is this a one-liner" purely from whether the body is
   *currently* on one physical line — true on a fresh format (original source), false on a
   reformat of output where `enforceCallLineBreaking` already broke an over-long body across
   lines, so the grouping/brace-style decision (and thus padding/output) differed between
   passes. Fixed in both rule classes by adding a same-verdict-both-times pre-check: if the
   one-liner body contains a "breakable call" (an identifier's `(args)` with non-empty args --
   the exact shape `enforceCallLineBreaking` may later break; a bare `return x;`/`x = y;` body
   never qualifies) *and* the body+signature's predicted rendered width exceeds
   `MiscRule.LINE_LENGTH_LIMIT`, treat it as not a one-liner from the very first pass (in
   `GetterSetterRule`, `nestDepth` is now threaded through `groupOneLiners`/
   `parseOneLinerMember` from `ScopePipeline.processScope`'s own `depth`, same precedent as
   `applySignaturePass`, to estimate indentation column via `MiscRule.INDENT_WIDTH`). The
   check is deliberately narrow (only fires for candidates with an actual breakable call) --
   an early version without the "has breakable call" guard wrongly excluded legitimate,
   intentionally-long column-aligned one-liners with no call at all (e.g. C++ out-of-line
   `template<...>` class-template member definitions in `combined_out.cpp`, which
   `enforceCallLineBreaking` never touches regardless of length), regressing that fixture.
   `JavaSpecificRule`'s own copy of the "has breakable call" helper initially had an
   off-by-one bug (`nextSignificantIndex(tokens, from)` scans starting at `from+1`, not
   `from`, unlike `GetterSetterRule`'s differently-named `nextSignificant(tokens, from, to)`
   which is inclusive of `from`) that silently made it always return false; caught via a
   debug-print smoke test on an isolated repro before it reached `make test`.

Verified: `make test` 20/20 PASS (added permanent fixture
`test/real_code_regressions_2_{inp,out}.java` covering all 4 bug shapes, registered in the
Makefile's `INP_FILES` and `test/README.txt`), the full 71-file RobotCoding tree is
byte-for-byte idempotent (`diff -rq round1 round2` empty), and `javac`-compiles with zero
formatter-induced errors (22 remaining errors are the known pre-existing `javax.jmdns`
missing-dependency issue in `toolbar/WifiStaDialog.java`, confirmed identical against the
unmodified original source).

**Self-dogfood (formatter's own `src/com/jxmake/formatter/` tree, 20 files) — DONE:** found
and fixed one more bug of the same pass-ordering idempotency class:

5. **`MiscRule.parseAssignment` rejected any consecutive-assignment-alignment (STYLE.md §6)
   row whose RHS had already been wrapped across lines by a later pass**
   (`MiscRule.enforceCallLineBreaking`) — `parseAssignment` only recognizes STYLE.md §6's own
   "multi-line right-hand side" shape (exactly one newline, split cleanly before/after an
   operator via `classifyMultiLineBreak`); any other multi-line shape (in particular a call
   whose args got wrapped onto their own line between an opening and closing paren, i.e. 2+
   newlines, or 1 newline that isn't an operator break) fell through to `return null`, which
   `groupAssignments` treats as a hard group boundary. On a fresh format, an over-long
   assignment's RHS is still one physical line when this pass runs (it hasn't been wrapped by
   `enforceCallLineBreaking` yet, which runs later), so the whole run of assignments forms one
   group with uniform `=` padding; reformatting that same output found the already-wrapped
   rows unrecognized, splitting the run into smaller subgroups with different (usually
   smaller) padding — non-idempotent. Fixed by falling through to a verbatim single-line
   `Assignment` (embedding the existing newlines/indentation as-is via `joinVerbatim`, which
   just concatenates original token text) instead of returning null whenever the multi-line
   shape isn't classifiable as STYLE.md §6's clean operator-break case — the row still
   contributes its `lhsText` to the group's width computation and keeps its neighbors grouped,
   but its own already-wrapped RHS is left untouched rather than being re-derived.

Verified: `make test` 21/21 PASS (added `test/real_code_regressions_3_{inp,out}.java`), and
the full 20-file formatter `src/` tree is byte-for-byte idempotent (`diff -rq round1 round2`
empty), `javac`-compiles with 0 errors (matching the original source), and declaration counts
match (16 top-level/nested types, same names, in both original and round1 — a naive grep
overcounts due to javadoc/line-wrap noise containing the words "class"/"interface").

**`martinus/nanobench` (single-header `src/include/nanobench.h`, 3484 lines) — DONE:** found
and fixed two real bugs, one of them a severe silent-content-loss class never seen before this
session (previous bugs were all idempotency-only divergences, never actual data loss):

6. **`TokenizerCore` had no support for C++11 raw string literals (`R"delim(...)delim"`) at
   all.** nanobench stores its mustache HTML/JSON/CSV report templates as raw strings full of
   `{{...}}` placeholders and literal `{`/`}` characters. Without raw-string recognition, the
   tokenizer lexed a raw string's content as ordinary source: the `{`/`}` characters inside were
   seen by every scope-splitting/brace-depth-tracking pass as real punctuation, corrupting
   nesting depth for the rest of the file. Diagnosed via `head -n N`-based bisection (narrowed
   the trigger to a ~4-line window), then confirmed directly: a fully-balanced, self-contained
   1895-line prefix of the file, isolated and formatted on its own, still lost its final 2
   closing braces and (in the full/unbounded-growth case before the narrower fix below) up to
   ~46% of total output, producing code that doesn't even compile (`unterminated #if`,
   `unterminated #ifndef`, unclosed namespaces) — from a source file that itself compiles
   cleanly with 0 errors. Fixed by adding `TokenizerCore.rawStringPrefixLength`/`emitRawString`:
   recognizes the optional encoding prefix (`u8R`/`uR`/`UR`/`LR`/`R`) + `"` + a ≤16-char
   delimiter with no whitespace/paren/backslash + `(`, then lexes through to the matching
   `)delim"` as one opaque STRING token (same "one token, own text, never re-examined"
   precedent as `emitBlockComment`/`emitTextBlock`) — content inside (including any
   `{`/`}`/`"`) is never exposed to any other rule.
   - **Follow-up bug in the same fix**: the new dispatch branch was gated on `isC` (this
     tokenizer's C-only flag) instead of C-or-C++, so `.hpp`/`.cpp` files (anything not
     literally `.c`/`.h`) were completely unaffected by the fix — raw strings in genuine C++
     files (like nanobench tested honestly under a `.hpp` extension, since it's really C++
     despite upstream naming it `.h`) still corrupted. Added an `isCpp` field to
     `TokenizerCore` (mirroring `Lang.isCpp`, populated from the constructor same as `isC`/
     `isJava` already were) and changed the raw-string dispatch condition to `isC || isCpp`.
7. **`DeclarationAlignmentRule`'s general (non-function-forward-declaration) group renderer
   silently dropped a leading `template<...>` prefix** captured on a bare forward declaration
   (`template <typename T>\nstruct PerfCountSet;`, no body). `parseDeclaration` already extracts
   and stores this as `Declaration.templatePrefix` for exactly this purpose, but `render()` has
   two separate group-rendering code paths gated on `allAreFuncDecls` (true only when every
   declaration's `sizeTokens` starts with `(`, i.e. looks like `name(...)`): only
   `renderFunctionForwardGroup` (the `true` branch) ever emitted `templatePrefix`; the general
   grid-render path (the `false` branch, which is what a bare `struct Foo;`/`class Bar;`
   forward declaration with no parens actually goes through) built its `lines` list directly
   from `grid.flush()` with no `templatePrefix` check at all. Result: `template <typename T>`
   vanished from in front of `struct PerfCountSet;`, while the immediately-following *full*
   definition (`template <typename T>\nstruct PerfCountSet { ... };`, which has a body and so
   is handled entirely by `ScopePipeline`, never reaching this rule) kept its own template line
   fine — `struct PerfCountSet` ends up forward-declared as a plain (non-template) type, then
   later redefined as a template of the same name: `error: 'struct ... PerfCountSet' is not a
   template` at every use site. (A smaller, likely-related contributing factor also fixed in
   the same pass: the `template` keyword's own child-clause detection in `parseDeclaration`
   required `body.get(i + 1)` to be the `<` token with zero gap tokens in between —
   `significantOnly(stmt)` already strips whitespace/newlines before this check runs, so this
   turned out not to be reachable in practice, but the defensive gap-skip was added anyway
   since it costs nothing and matches the equivalent skip already used for `requires`/
   `template` pulling elsewhere in `MiscRule`.) Fixed by adding the same `templatePrefix`-check
   in the general path's line-building loop that `renderFunctionForwardGroup` already had.

Verified: `make test` 22/22 PASS (added `test/real_code_regressions_4_{inp,out}.hpp`, covering
both the raw-string-with-braces shape and the template-forward-declaration shape in one
fixture), the full nanobench header (formatted honestly as C++ via a `.hpp` copy, since its
real content is C++ despite the upstream `.h` name) is byte-for-byte idempotent (`diff` round1
vs round2 empty) and round1 compiles clean with `g++ -std=c++20 -fsyntax-only` under
`-DANKERL_NANOBENCH_IMPLEMENT` (matching the unmodified original, which also compiles with 0
errors under the same flags).

**Known, deliberately out-of-scope observation (not a bug):** formatting nanobench.h under its
*actual* `.h` extension (this formatter's own convention: `.h` = C, `.hpp` = C++, matching
`h_core_inp.h`/`hpp_core_inp.hpp` in `test/`) hits a different, unrelated failure — `CppSpecificRule
.enforceEmptyParameterList`'s C-only "add explicit `(void)`" heuristic misfires on a constructor
member-initializer-list's trailing `, mHas() { ... }` (an empty-parens call immediately
followed by `{`, structurally identical under C rules to an empty-param C function
*definition*), rewriting it to the ill-formed `mHas(void)`. This can only happen because
nanobench is genuinely C++ mislabeled with a `.h` extension (real C has no member-initializer
lists at all, so this shape is unreachable for genuine C input) — treated as a convention
mismatch in the test file, not a formatter defect, and not fixed this session.

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
- **C++23**: `github.com/basvas-jkj/cpp_modules` — check first whether this actually uses C++20/23
  *language modules* (`import`/`export module`) before committing to it as a candidate; that
  syntax is a real risk both for the formatter (never exercised against `import`/`module`
  keyword contexts) and for whichever toolchain compiles it.
- **C++23**: `github.com/V1niciosLins/StartCpp` — smaller/likely learner-style repo; treat as a
  quick filler candidate, lower priority, not expected to surface new bugs.
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

For each new bug found: minimal isolated repro first, fix, verify against the full source
round-trip, `make test`, then a permanent fixture under `test/real_code_regressions_*` (a new
`_4`/`_5` suffixed pair if an existing one gets too large) — same pattern as this session.
Update this section as each candidate completes.

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

Previously-recorded low-priority gaps, now resolved. Kept here (not deleted)
so the history of what was wrong and how it was fixed isn't lost.

**`* const` cosmetic gap in mixed declaration groups (`DeclarationAlignmentRule`) — FIXED**
The separate-postConst-column layout used to produce a visual gap between `*`
and `const` when shorter types shared a group with longer ones:

```c
char**         c;
double**       c;
char*    const c; // ← gap (was)
char* const    c; // ← correct per §8
```

Fix: `splitCppType` now always returns `postConst = ""` and includes the full
token sequence (including any trailing `const`) in `typeAndStar`, so the whole
type+star+const text is one column padded uniformly. East-const
(`char const*`) is intentionally not normalized to west-const.

**`typedef` declarations not aligned — FIXED**
`typedef` is now a modifier-column keyword in `CppModifierPriority` (rank 0,
ahead of `static`/`constexpr`/etc. since C/C++ grammar requires it first).
`typedef int Foo;` parses through the normal declaration path and joins
surrounding plain-variable groups instead of breaking them.

**Direct function-pointer declarations not aligned — FIXED**
`void (*fp)(int);` used to make `parseDeclaration` return null (last token is
`)`, not an IDENTIFIER), breaking the surrounding group. `parseDeclaration`
now detects the `Type (*name)(params)` shape directly (independent of whether
an initializer follows) and folds `(*name)` into the name cell, so it renders
and aligns like any other declaration in the group:

```c
int   count      = 0;
void  (*cb)(int) = NULL;
float ratio      = 1.0f;
```

Multi-star names (`(**cb)`) are also handled — the tokenizer emits a run of
`*` as one merged rep-op token (`Token.isRepOp`), not separate `*` tokens, so
the detection checks `isRepOp(t, '*')` rather than a literal `"*"` op match.

**`#ifdef`/`#elif`/`#else`/`#endif` interleaved with declarations dropped every
branch but the first — FIXED**
Discovered while implementing `### C` (Java preprocessor pass-through), but
confirmed to affect C/C++ too — not Java-specific. `#ifdef`/`#elif`/`#else`
directives sitting between sibling declarations in the same group were
silently dropped (only the leading `#ifdef` before the group's first
declaration and the trailing `#endif` after its last survived), e.g.:

```cpp
#ifdef FEATURE_X
    int featureFlag = 1;
#elif defined(FEATURE_Y)
    int featureFlag = 2;
#else
    int featureFlag = 0;
#endif
```
collapsed to just the three declarations under one `#ifdef ... #endif` with
the `#elif`/`#else` lines gone entirely. Root cause:
`DeclarationAlignmentRule.splitStatements`/`ScopePipeline.splitTopLevelSpans`
never treat a `PREPROCESSOR`/`MACRO_DEF` token as its own statement boundary
(only `;`/`}`/access-specifier `:` are), so a directive line's tokens end up
folded into the *following* statement's token list -- and `groupDeclarations`'
`hasCommentBefore` group-break guard (added earlier for standalone comments,
see the `c_comments_inp.c` Bug 1 entry above) only checked for
`COMMENT_LINE`/`COMMENT_BLOCK`, not `PREPROCESSOR`/`MACRO_DEF`, so a directive
mid-group never forced a group boundary. Since `render(group)` only re-emits
each `Declaration`'s own parsed fields (type/modifiers/name/init) with no
field carrying interleaved raw text, any directive line embedded *inside* a
group (not just before the group's very first statement, which
`applyDeclarationsPass`'s leading-gap capture already preserves) was silently
discarded. Fixed by adding `PREPROCESSOR`/`MACRO_DEF` to `hasCommentBefore`'s
check, forcing a group break at any leading directive exactly like a
standalone comment already does. Verified via `make test` (zero regressions,
15/15 file-pairs) plus manual smoke tests of the `#elif`/`#else` shape above
in both `.cpp` and `.java`, confirmed idempotent.

**`using` alias declarations not aligned — NOT SCHEDULED (design decision)**
`using Foo = Type;` is inverted (name, then `=`, then type) versus every other
declaration this rule handles (type, then name), so it can't reuse the
existing `typeTokens`/`name` model. Still breaks group boundaries; passes
through unchanged (no corruption). If picked up later, align at `=` per the
user's own suggested layout:

```cpp
using whatever1           = ...;
using long_long_long_name = ...;
```

This needs its own parsing branch (recognize `using IDENTIFIER = ...;`) and a
column layout keyed on the `=` position rather than the name+size position —
scope it as a small standalone task rather than folding into the function-
pointer/typedef fix above.

**Preprocessor directive glued onto a following Java method definition — FIXED**
A `#endif` (or any preprocessor line) sitting directly before a method
definition inside a class body used to get glued onto the same output line as
the method's modifiers, e.g. `#endif public void run(void)`, regardless of
blank lines separating them in the source. Re-confirmed genuinely
**Java-specific** via a minimal repro built both before and after the fix
(`git show HEAD:...` swapped in temporarily): the identical shape in C++
already formats correctly, because `applySignaturePass`'s C/C++ branch for
computing `sigLeadStart` separately scans forward for the last `NEWLINE`
before the function name and restarts from that line, incidentally routing
around the bug -- the Java branch (`skipAnnotations`) has no equivalent
line-rescan and uses `leadStart` as-is. Root cause: `leadStart` is found via
`nextSignificantIndex`, which (correctly, for its other callers) treats
`PREPROCESSOR`/`MACRO_DEF` as significant, not a gap token -- so when a
directive line is the first token of a method's leading span, `leadStart`
landed on the directive itself instead of skipping past it. `sigLeadStart`
then also pointed at the directive, so `leadingGap` (computed as
`joinText(span.start, sigLeadStart)`) excluded the directive's own text,
silently dropping it from the preserved leading gap and leaving it glued to
the re-rendered signature's first line with no separating text at all. Fixed
by adding a loop right after computing `leadStart` that walks forward past
any run of leading `PREPROCESSOR`/`MACRO_DEF` tokens (each still its own line)
to find the real first token of the signature, conceptually the same fix
shape as `BlockStructureRule.skipGuardForward`/`skipGuardBackward` already
walking past guard directives for named-construct blank lines (a different
code path, not reused directly, since this one operates on `leadStart` inside
`applySignaturePass` rather than a brace boundary). New test fixture
`test/java_preprocessor_method_inp/out.java` covers a `#endif` directly before
a method (with and without a blank line, and with a `throws` clause), added
to `INP_FILES` and `test/README.txt`. Verified via `make test`: 18/18 PASS
(forward + idempotency), zero regressions; manually confirmed the identical
C++ shape formats correctly both before and after the fix.

---

## TODO — (DONE, partial)

**Implementation order: C, B, D, E, A -- with F threaded through each step,
plus a final F sweep after A.**
C (Java preprocessor pass-through) is first and smallest -- the
`PREPROCESSOR`/`MACRO_DEF` token machinery already exists and works for
C/C++, it's just gated off for Java by one method (`isPreprocessorLanguage`),
so it's a small, low-regression-risk change. B (new config entries) is next
-- `Config.java` has a mechanical, copy-paste-able pattern already (see
`format-macros` as precedent) and the behavior it gates (`MiscRule` comment
title-casing/end-period) has a handful of call sites to guard. D (multi-file
smoke test + benchmarking) and E (code cleanups) come after the two easy
features -- they're hygiene/testing work with no new user-facing capability,
so there's no reason to front-load them ahead of cheap real wins. A
(enable/disable formatting via markers/CLI flag) is last and by far the
largest: it's a cross-cutting change touching nearly every rule file across
the ~25-pass pipeline in `Formatter.formatOne` (`BlockStructureRule`,
`SwitchRule`, `MiscRule`, `CppSpecificRule`/`JavaSpecificRule`,
`ScopePipeline`, `DeclarationAlignmentRule`), none of which currently has any
concept of a frozen/passthrough span. Treat it as its own design-and-plan
session, not a quick pass folded in with the rest.
F ("add more tests") is not an independent slot in this order -- A/B/C's own
completion criteria already say to add tests for what they implement as they
go, so add each one's tests immediately after it lands rather than batching
them at the end. Do one final F pass after A to catch anything left over.

### A — Add support to enable/disable formatting (DONE)

Infrastructure landed: `Token.frozen` field + `TokenizerCore.markFrozenSpans` (scans
`//% JXM_CFMT_DIS`/`ENA` and block-comment equivalents, toggles frozen state, marker
itself always frozen); `Formatter.formatOne` overload takes `formatOff` and wraps
every re-tokenize call through a local `tokenizer` `Function` that calls
`markFrozenSpans`; `ScopePipeline` given the same `formatOff`-aware `tokenize()`
wrapper (its own overloaded constructor, default `false` for the old 4-arg call
sites); `Main.java` parses `--format-off`, threads it through
`processFile`/`format`/`formatStandalone`/`delegateToServer`; `ServerMode.FormatHandler`
reads a `format-off=true` query param. `make test` 15/15 PASS (forward + idempotency),
zero regressions -- this step only adds the frozen-tagging plumbing, no rule yet
actually checks `t.frozen`, so behavior is unchanged pending the per-rule guard work
below.

Per-rule frozen guards landed so far (each following `make test` 15/15 PASS
forward + idempotency, zero regressions): `MiscRule`, `BlockStructureRule`,
`SwitchRule`, `ScopePipeline`'s own splice-back passes, and now
`JavaSpecificRule`/`CppSpecificRule` -- every public method in both files
(`enforceMethodDefinitionAllmanBraceStyle`, `separateEnumConstantListTerminator`,
`enforceImportOrdering`, `enforcePermitsClauseLineBreaking`,
`enforceSwitchExpressionArrowAlignment` in `JavaSpecificRule`;
`enforceEmptyParameterList`, `enforceFunctionDefinitionAllmanBraceStyle`,
`enforceTemplateAngleBracketSpacing`, `enforceRequiresClausePlacement`,
`enforceHeaderFileStructure`, `alignMacroDefinitions` in `CppSpecificRule`) now
either skip a frozen candidate span/token (leaving it untouched) or -- for the
few whole-file passes (`enforceImportOrdering`, `enforceHeaderFileStructure`) --
bail the entire pass unchanged if any token in the relevant zone is frozen.
`CppSpecificRule.enforceHeaderFileStructure` is largely whole-file/zone-boundary
work with no per-content-span rewrite (the body itself is already passed through
verbatim); guarded anyway by bailing the whole pass if any token in the detected
header-zone span (copyright through the guard/`#endif`) is frozen, covering the
narrow case of a frozen region overlapping the guard itself.

`DeclarationAlignmentRule` and `GetterSetterRule` are only ever invoked through
`ScopePipeline`'s splice-back passes (`applyDeclarationsPass`/
`applyGetterSetterPass`), which already skip a frozen span at the splice choke
point (commit `0d36924`) -- no separate per-rule guards needed in those two
classes themselves.

New test fixture `test/format_toggle_inp/out.java` added (both marker forms,
mid-class-body, misformatted frozen content with normally-formatted
declarations immediately before/between/after each region), registered in the
Makefile's `INP_FILES` and `test/README.txt`. Writing and idempotency-checking
this fixture surfaced two real bugs in the frozen-guard plumbing, both fixed:

1. `applyDeclarationsPass`/`applyAssignmentsPass`/`applyGetterSetterPass`'s
   frozen-span checks used each group's/member's full `Span`/`memberFrom`,
   whose *leading gap* can contain a previous statement's own trailing
   `JXM_CFMT_ENA`/`DIS` marker (the marker token itself is always stamped
   frozen by design, so its re-tokenizes survive). That falsely marked the
   *next*, wholly-unfrozen declaration/assignment/member as frozen and skipped
   it. Fixed by checking from each group's own first real token (`firstIdx`/
   `sigIdx`) instead of the span's/member's raw start.
2. `ScopePipeline.processScope`'s recursion into a child scope (`{...}` body)
   re-tokenizes the extracted substring from scratch, independently re-running
   `markFrozenSpans` on just that text -- so if a scope's own `{` was already
   frozen on entry (the `JXM_CFMT_DIS` marker that caused it lives *outside*
   the substring, e.g. before the method's own signature, or via
   `--format-off`), the substring's own re-tokenize had no way to know that,
   and defaulted to "not frozen" -- silently reformatting content the outer
   guards had already promised to leave untouched. Fixed by threading the
   scope's own frozen-entry state as an explicit `startFrozen` parameter
   through `processScope`/`tokenize` (rather than the previous fixed
   `formatOff` field), seeded from `current.get(span.openBraceIdx).frozen` at
   each recursion point -- this also correctly handles a `JXM_CFMT_ENA` marker
   appearing *inside* a scope that itself started frozen (e.g.
   `--format-off` + a mid-class resume marker), which an earlier, more naive
   "skip recursion entirely if the open brace is frozen" attempt did not.

Verified via `make test` (16/16 PASS, forward + idempotency) plus manual
`--diff`/`--format-off` smoke tests covering: whole-file `--format-off` with
no resume marker (no diff at all), `--format-off` with a `JXM_CFMT_ENA` resume
marker mid-class (content before frozen, after reformatted), and a
partial in-body freeze (content outside the marker pair reformats normally,
inside stays byte-for-byte untouched).

`README.md` documents the marker syntax (`//% JXM_CFMT_DIS`/`ENA` and
`/*% JXM_CFMT_DIS */`/`/*% JXM_CFMT_ENA */`) and the `--format-off` CLI flag
under "Disabling formatting for part or all of a file". Task A is complete:
plumbing, all rule guards, test fixture, and docs all landed and verified.

//% JXM_CFMT_ENA
/*% JXM_CFMT_ENA */

Via command line option `--format-off`: formatting starts disabled for the whole file, as
if `JXM_CFMT_DIS` were present at the top -- the user must insert an explicit
`JXM_CFMT_ENA` marker in the source to turn formatting back on from that point onward.

**Design direction (RDD_KEY_90):** implement via in-memory token masking, not by
splitting the file into fragments/tmp dirs and formatting them independently --
rejected because disabled regions aren't guaranteed to align to block boundaries
and several rules need whole-file/whole-scope context. Tokenize the whole file once;
tag the token range between each marker pair as frozen; every rule treats frozen
tokens as opaque pass-through (skipped for transformation, but still counted for
brace/scope-depth/line-number bookkeeping).

Perform smoke-testing after implementing this and then `make test` to ensure there is no
regression.

Update `README.md` after the tests passed and then add the tests as one of the
new tests candidate in `## TODO — Not Scheduled` : `### F — Add more tests`.

### B — Add new configuration entries (DONE)

```properties
# ── Behavior ──────────────────────────────────────────────────────────────────
normalize-comment-start-case = on              # on | off
normalize-comment-end-period = on              # on | off
```

And implement that to enable/disable comments title-casing and end-period handling.

Implemented: two new `Config.java` keys following the existing `format-macros` pattern
(`ALL_KEYS`, private field default `true`, getter, `parseBoolean` line in `fromRawMap`).
`MiscRule` now takes both flags in its constructor and gates them at the single shared
definitions of `capitalizeFirstLetter` (start-case) and `stripSoleTrailingPeriod`/
`stripSoleTrailingPeriodAcrossLines` (end-period) -- every comment-handling call site
(line comments, block comments, multi-line banner comments, preprocessor trailing
comments) already funnels through these two methods, so no per-call-site wiring was
needed. `ScopePipeline`'s constructor also threads both flags down to its own internal
`MiscRule` instance for consistency, though that instance's comment paths aren't
currently exercised (used only for §6 assignment grouping there).

Perform smoke-testing after implementing this and then `make test` to ensure there is no
regression.

Verified via `make test` (15/15 file-pairs, zero regressions, defaults are `on`/`on` so
existing fixtures are unaffected) plus a manual smoke test toggling both keys `off` via a
`.jxmake-code-formatter` file, confirming lowercase/no-period comments are left untouched.

Update `README.md` after implementing this.

### C — Don't damage C-preprocessor macros embedded in Java source (DONE, partial)

Some Java source files use a C-macro preprocessor (e.g. PCPP-style) as a poor man's
template mechanism -- `#define`/`#ifdef`/etc. lines mixed into otherwise-normal Java code
before a separate preprocessing step runs. The Java formatter currently has no awareness
of this and could corrupt such lines (they don't look like valid Java constructs).
Investigate and, if needed, add detection/pass-through handling so these preprocessor
lines are left untouched when formatting `.java` files.

Implemented: `TokenizerCore.isPreprocessorLanguage()` now returns `true` unconditionally
(previously `!"java".equals(language)`), so `#`-directive lines in `.java` files are lexed
as opaque `PREPROCESSOR`/`MACRO_DEF` tokens, same as C/C++ -- every existing rule already
passes them through untouched with no per-rule Java-specific handling needed. Verified via
`make test`: all 15 file-pairs still PASS (forward + idempotency), zero regressions.

Smoke-testing surfaced two pre-existing bugs in the shared declaration-grouping/splice
pipeline, **not introduced by this change** (confirmed identical in unmodified C++/plain-C
files of the same shape via `git stash`): (1) `#ifdef`/`#elif`/`#else`/`#endif` interleaved
with declarations inside a scope dropped every branch but the first (affects C/C++ too --
**now FIXED**, see "Known Gaps — Fixed" below); (2) a preprocessor directive immediately
before a Java method definition gets glued onto the same line (Java-specific, C++
unaffected -- **now FIXED**, see "Known Gaps — Fixed" below).

Perform smoke-testing after implementing this and then `make test` to ensure there is no
regression.

Update `README.md` after the tests passed and then add the tests as one of the
new tests candidate in `## TODO — Not Scheduled` : `### F — Add more tests`.

### D — Extra (DONE)

1. Smoke test the support multiple-file formatting at once, both in `--standalone` and
   client-server mode
2. Add `bench` target in Makefile for benchmarking (calculate the total time):
   - Formatting the 15 files above one by one in `--standalone` mode
   - Formatting the 15 files above at once in `--standalone` mode
   - Formatting the 15 files above one by one in client-server mode
   - Formatting the 15 files above at once in client-server mode

Start the server before benchmarking the client-server mode and then stop the server
after the benchmarking is done. Do not include the server start and stop time in
the benchmark.

**Implemented:** new `bench` Makefile target covers both items at once — its
"all-at-once" standalone/client-server passes each invoke the JAR with all 15
`*_inp.*` files as separate CLI args in one process, which is the multi-file smoke
test item 1 asked for, so no separate test was written. Timing uses
`date +%s%N` deltas around each of the four passes; the client-server passes start
the server backgrounded (`&`, since `ServerMode.start` blocks in the foreground by
design — non-daemon `HttpServer` listener threads keep the JVM alive until
`/shutdown`, RDD_KEY_9) and poll with a real client request (formatting
`h_core_inp.h`) until it succeeds or 50 retries (5s) elapse, before starting the
timer — this excludes both server startup and the lockfile/port-bind race from the
measured time. The probe file is the first entry of the same `$(INP_FILES)` list
used for the timed passes (`set -- $$files; probe="$$1"`), not a hardcoded filename.
Server is stopped via `--stop` after the last pass. Verified: `make
bench` runs end-to-end with no hang and no leftover `java`/lockfile processes;
`make test` still 15/15 PASS (forward + idempotency) afterward.

Makefile note: keep recipe line continuations (`\`) aligned to a common column,
consistent with the existing `test` target — this project's Makefile uses tab
size 8.

### E — Code cleanups (DONE)
1. These comparison: (DONE)
     "c".equals()
     "cpp".equals()
     "java".equals()
   are scattered all over the place in the code, please refactor the, so they
   are only compared once for every file being processed -- precompute
   `isC`/`isCpp`/`isJava` (or an equivalent boolean/enum) once per file in
   `Formatter.formatOne` and thread it down instead of re-doing the string
   comparison in every rule method.

   Implemented: new top-level `Lang` class (`isC`/`isCpp`/`isJava` computed once from the raw
   `language` string in its constructor). `Formatter.formatOne` constructs exactly one `Lang`
   instance per file and passes it into every rule class's constructor (`BlockStructureRule`,
   `SwitchRule`, `MiscRule`, `CppSpecificRule`, `JavaSpecificRule`, `GetterSetterRule`,
   `DeclarationAlignmentRule`, `ScopePipeline`, `TokenizerCore`) instead of the raw `language`
   string; each rule stores the `Lang` (or, where only one flag was ever read, e.g.
   `GetterSetterRule`/`TokenizerCore`, just the derived booleans) and reads `lang.isC`/
   `lang.isCpp`/`lang.isJava` at every call site that used to re-run `"x".equals(language)`.
   The only remaining `"java".equals(...)`/`"cpp".equals(...)`/`"c".equals(...)` call sites in
   `src/` are `Lang`'s own constructor and one unrelated string check in
   `JavaSpecificRule.java:668` (`"java".equals(first)`/`"javax".equals(first)`, an import-prefix
   comparison, not a language check). Verified via `make test`: 15/15 file-pairs still PASS
   (forward + idempotency), zero regressions.

2. Checkings such as: (DONE)
     isOp(...)
     isPunct(...)
     isKeyword(...)
     isComment(...)
     isGapToken(...)
     etc.
   are scattered all over the place in the code, please refactor the, so they
   are centralized in the `TokenizerCore.Token` class or other class.

   Implemented: five null-safe static methods added to `TokenizerCore.Token`
   (`isPunct`/`isOp`/`isKeyword`/`isComment`/`isGapToken`), alongside the pre-existing
   `isRepOp` static/instance pair. Every rule class's byte-for-byte-duplicate private
   helper of the same name was deleted; call sites are unchanged in syntax (same
   `isPunct(t, "x")`/`isOp(t, "x")`/etc. shape) because each file adds a `static import`
   of exactly the methods it uses instead of redefining them locally
   (`ScopePipeline`, `GetterSetterRule`, `DeclarationAlignmentRule`, `CppSpecificRule`,
   `SwitchRule`, `JavaSpecificRule`, `BlockStructureRule`, `MiscRule`). `SwitchRule` also
   had its own differently-named `isGap` wrapper (identical body to `isGapToken`) --
   renamed all its call sites to `isGapToken` and dropped the wrapper. Three
   `this::isComment` method references in `BlockStructureRule` (passed to
   `Stream.anyMatch`/`noneMatch`) were switched to `Token::isComment`, since a static
   method reached only via static import can't be referenced through `this::`. Two
   rule-local helpers with different semantics from the five centralized checks were
   deliberately left in place, not touched: `MiscRule.isCommentOrNewline` (comment OR
   newline, no whitespace -- not the same predicate as `isGapToken`) and
   `ScopePipeline.isWhitespaceOrNewline` (whitespace/newline only, no comments --
   also distinct). Verified via `make test`: 15/15 file-pairs still PASS (forward +
   idempotency), zero regressions.

### F — Add more tests — DONE

Added `test/c_cpp_decl_gaps_inp/out.c` covering the three `DeclarationAlignmentRule`
fixes recorded under `## Known Gaps — Fixed`: the `* const` column gap in a mixed
`char**`/`double**`/`char* const` group, `typedef unsigned char byte;` joining and
aligning with a following plain-variable group (`int`/`double`), and direct
function-pointer declarations (`void (*cb)(int) = NULL;`, including multi-star
`void (**cbcb)(int) = NULL;`) joining a plain-variable group with the `=` aligned
across the whole group. Registered in the Makefile's `INP_FILES` and
`test/README.txt`. Verified via `make test`: 17/17 PASS (forward + idempotency),
zero regressions.

This was the final F pass done after Task A per the "F is not an independent
batching slot" note above -- Task A's own tests (`format_toggle_inp/out.java`)
were already added when Task A landed; this pass covers everything else left
over from prior sections.
