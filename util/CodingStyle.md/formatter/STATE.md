# STATE.md — Formatter Implementation Tracker

---

## Instructions for Claude CLI

**Read this section first, every session, before doing anything else.**

### Session start
1. Read this entire file to understand current state
2. Check the **File Status** table to find the current file (`IN PROGRESS` first,
   then the first `NOT STARTED`). All files are `COMPLETE`; resume work is in the
   dogfood checkpoint — see **Checklist — Step 1.5** below.
3. Resume from the first unchecked item in **Checklist — Step 1.5**.
4. If anything in this file is ambiguous, stop and ask before writing any code

**Do NOT read `FORMATTER_DISCUSSION.md` or `README.md`** unless the user explicitly
asks. All decisions relevant to implementation are recorded in the
**Resolved Design Decisions** index below (full text in `STATE_rdd_log.md` —
**do not read that file in full**, look up one key at a time via `grep -Fm1`).
`FORMATTER_DISCUSSION.md` is design history and future planning only — large, and
contains nothing the implementer needs beyond what is already indexed here.

**ONLY** read the Java source file you are currently implementing or directly modifying.
Do NOT read other source files unless a specific checklist item or ambiguity requires it.

### During implementation
- Implement one checklist section at a time
- After completing a section (or when the cumulative diff across all changed files
  exceeds ~50 lines, whichever comes first), do a checkpoint commit:
  1. Update STATE.md — check off completed items, update File Status table
  2. `git add util/CodingStyle.md/formatter/` (the entire formatter directory)
  3. `git reset util/CodingStyle.md/formatter/target/` (exclude build output)
  4. `git commit -m "<message>"` — short descriptive message, no strict format required,
     trailer ending with `Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>`
- Small related items within a section may be grouped into one commit if they
  are trivially connected — use judgment based on line count (~50 lines threshold)
- Never let implemented files and STATE.md drift out of sync — STATE.md must
  always reflect the true current state at every commit

### When hitting an ambiguity or open question
1. **Stop coding immediately** — do not guess or proceed past the ambiguity
2. Update STATE.md: add the question to **Open Questions**, mark the blocked
   checklist item with `[~]` and a note
3. Commit STATE.md only:
   ```
   git add util/CodingStyle.md/formatter/STATE.md
   git commit -m "$(cat <<'EOF'
   jxmake-code-formatter: block on <question summary>

   Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
   EOF
   )"
   ```
4. Ask the user and wait for an answer before continuing
5. Once resolved: append the full decision as a new row to `STATE_rdd_log.md`
   (next `RDD_KEY_n` number), add the key + topic to the **Resolved Design
   Decisions** index in this file, remove from **Open Questions**, unblock
   the checklist item, then continue

### When a file reaches COMPLETE
1. Mark it `COMPLETE` in the File Status table
2. Replace the **Current File** checklist with the checklist for the next file
3. Commit STATE.md together with the completed source file

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
      Main.java                 ← CLI entry point
      Config.java
      ServerMode.java
      Formatter.java            ← shared per-file pipeline (Config.resolve + ScopePipeline.process +
                                   whole-file enforceX passes, in order) called by both Main.java and
                                   ServerMode.java -- see "Formatter.java orchestration architecture"
                                   in Resolved Design Decisions
      IndentationDetector.java  ← whole-project dominant-indent-style walker (for `indent-style = auto`)
      ScopePipeline.java        ← recursive scope/signature discovery + group-render-splice engine
                                   for DeclarationAlignmentRule/GetterSetterRule/MiscRule's grouping
                                   rules (STYLE.md §5/§6/§8/§14) -- see "Main.java orchestration
                                   architecture" in Resolved Design Decisions
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
  target/
```

---

## Implementation Status

| Step | Scope | Status |
|---|---|---|
| Core formatter | `STYLE.md` / `STYLE_C_CPP.md` / `STYLE_JAVA.md` (Tier 1 + Tier 2) | COMPLETE |
| Newer-language constructs | `STYLE_JAVA17.md` / `STYLE_CPP20.md` | COMPLETE |
| Call/declaration line-breaking | `STYLE.md` §8 extension, `MiscRule.enforceCallLineBreaking` | COMPLETE |
| `renderTokens` paren-spacing fix | `DeclarationAlignmentRule` + `MiscRule` | COMPLETE |
| Dogfood checkpoint | `Main.java` + dogfood verification | IN PROGRESS |
| AI integration | local on-device AI for Tier-3 judgment calls | NOT FEASIBLE (deferred — see `STATE_NEXT_AI.md`) |

---

## Resolved Design Decisions

Full decision text lives in `STATE_rdd_log.md` — **do not read that file in full**.
To look up a specific decision during implementation:
```
grep -Fm1 'RDD_KEY_n' util/CodingStyle.md/formatter/STATE_rdd_log.md
```

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
| RDD_KEY_83 | `JavaModifierPriority` column order for `abstract`/`sealed`/`non-sealed`/`final`/`volatile` |
| RDD_KEY_84 | `record` named-construct detection through component list / `implements` clause / compact constructor |
| RDD_KEY_85 | C++ concepts/`requires` clause implementation in `CppSpecificRule.java` |
| RDD_KEY_86 | `MiscRule.java` call/declaration line-breaking architecture -- option 2 must bypass `parseSignature`, option 1 reuses it + new `renderDropped` |
| RDD_KEY_87 | `MiscRule.enforceCallLineBreaking` implementation scope decisions (nesting, comment bail-out, call-vs-declaration classification, new preserve-groups grid) + `collapseTokensToOneLine` bugfix |
| RDD_KEY_88 | `Main.java` implementation (Step 1.5) -- CLI parsing, config resolution, indent-style temp-cache, server auto-connect/delegate, `--server`/`--stop`, output modes, exit codes |

---

## Open Questions

*(none)*

---

## File Status

| File | Status |
|---|---|
| `Config.java` | COMPLETE |
| `ServerMode.java` | COMPLETE (see RDD_KEY_80: `ProcessHandle` via reflection for Java 8 target) |
| `Formatter.java` | COMPLETE (see RDD_KEY_72, RDD_KEY_74) |
| `IndentationDetector.java` | COMPLETE (see RDD_KEY_79) |
| `ScopePipeline.java` | COMPLETE (reopened during smoke-testing: C++ access-specifier label span bug — see RDD_KEY_78) |
| `TokenizerCore.java` | COMPLETE |
| `ColumnGrid.java` | COMPLETE |
| `ModifierPriority.java` | COMPLETE |
| `CppModifierPriority.java` | COMPLETE |
| `JavaModifierPriority.java` | COMPLETE (see RDD_KEY_83: sealed/non-sealed column order) |
| `ComplexityPaddingEvaluator.java` | COMPLETE |
| `DeclarationAlignmentRule.java` | COMPLETE (see RDD_KEY_68: `splitStatements` depth-awareness; RDD_KEY_76: rejects `++j;`/`--j;` as fake declaration) |
| `BlockStructureRule.java` | COMPLETE |
| `SwitchRule.java` | COMPLETE |
| `GetterSetterRule.java` | COMPLETE |
| `MiscRule.java` | COMPLETE (see RDD_KEY_43: `indent-style=auto` deferred to `IndentationDetector`; RDD_KEY_62: §3.1 padding added; RDD_KEY_77: closing-comment label detection for idempotency) |
| `CppSpecificRule.java` | COMPLETE (see RDD_KEY_58: §11 dropped from scope; RDD_KEY_75: §14 one-liner adjacency heuristic; RDD_KEY_81: Allman render-loop infinite loop fix; RDD_KEY_85: concepts/requires) |
| `JavaSpecificRule.java` | COMPLETE (see RDD_KEY_75: §14 one-liner adjacency heuristic; RDD_KEY_81: Allman render-loop infinite loop fix; RDD_KEY_83: sealed/permits clause) |
| `JavaModifierPriority.java` (sealed/non-sealed) | COMPLETE (see RDD_KEY_83; `TokenizerCore.java` and `JavaSpecificRule.java` also touched) |
| `JavaSpecificRule.java` (record) | COMPLETE (see RDD_KEY_84; `TokenizerCore.java` and `BlockStructureRule.java` also touched) |
| `JavaSpecificRule.java` (switch expressions) | COMPLETE (new `enforceSwitchExpressionArrowAlignment`, wired into `Formatter.java`) |
| `TokenizerCore.java` (text blocks) | COMPLETE (opaque `STRING` token spanning whole block) |
| `DeclarationAlignmentRule.java` (`var`) | COMPLETE (`"var"` added to `TYPE_KEYWORDS_JAVA`) |
| `JavaSpecificRule.java` (pattern matching) | COMPLETE (confirmed true no-op) |
| `CppModifierPriority.java` (consteval/constinit) | COMPLETE (see RDD_KEY_85 context; `consteval`/`constinit` added to `KEYWORDS_CPP`) |
| `DeclarationAlignmentRule.java` (structured bindings) | COMPLETE (new `parseStructuredBinding` helper) |
| `CppSpecificRule.java` (concepts/requires) | COMPLETE (see RDD_KEY_85; `TokenizerCore.java` and `BlockStructureRule.java` also touched) |
| `AI_PREAMBLE_FULL.md` / `AI_PREAMBLE_AESTHETIC.md` | COMPLETE (verified clean; no stale `AI_PREAMBLE.md` to delete) |
| `STYLE.md` (call line-breaking forms added to §8) | COMPLETE (commit b222345; verified matches RDD_KEY_86/87) |
| `MiscRule.java` (enforceCallLineBreaking) | COMPLETE (see RDD_KEY_86, RDD_KEY_87) |
| `Main.java` | COMPLETE (see RDD_KEY_88) |
| `DeclarationAlignmentRule.java` + `MiscRule.java` (`renderTokens` paren-spacing fix) | COMPLETE (see RDD_KEY_86, RDD_KEY_87) |
| `README.md` | COMPLETE (Phase 1+2 update done; ai-assist section removed) |
| `FORMATTER_DISCUSSION.md` (Step 2 NOT FEASIBLE decision) | NOT STARTED |
| `Config.java` (ai-assist keys) | NOT FEASIBLE (Step 2 deferred — see `STATE_NEXT_AI.md`) |
| `AiDecisionClient.java` | NOT FEASIBLE |
| `AI_DECISION_PROMPT.md` | NOT FEASIBLE |
| `MiscRule.java` (Tier-3 AI hooks) | NOT FEASIBLE |

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

# ── C/C++ ─────────────────────────────────────────────────────────────────────
include-sort               = off             # off | on
header-guard-rename        = off             # off | on
header-guard-style         = preserve        # preserve | ifndef | pragma-once

# ── Java ──────────────────────────────────────────────────────────────────────
java-import-order          = java, com, org, other, local, static
java-import-sort           = on
java-import-depth          = 2
java-import-blank-lines    = 1
```

---

## Fixed Constants (non-configurable)

These must appear as `private static final` in their owning class, never as raw literals.

| Constant | Value | Owner class |
|---|---|---|
| `MIN_DIVIDER_SLASHES` | `60` | `CppSpecificRule` |
| `HEADER_ZONE_BLANK_LINES` | `2` | `CppSpecificRule` |
| `INCLUDE_GROUP_BLANK_LINES` | `1` | `CppSpecificRule` |
| `EXTERN_C_LABEL` | `"extern \"C\""` | `CppSpecificRule` |
| `APP_NAME` | `"jxmake-code-formatter"` | `Config` |
| `LOCKFILE_NAME` | `"server.lock"` | `ServerMode` |
| `CONFIG_DIR` | `".config/jxmake-code-formatter"` | `Config` |
| `CONFIG_FILE` | `"config"` | `Config` |
| `DEFAULT_PORT` | `17173` | `ServerMode` |
| `MANIFEST_FILE` | `"MANIFEST.MF"` | _(build only, Makefile)_ |

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
accept `final` there). This applies to all `.java` files under `src/` in this
project, including ones already marked COMPLETE — when editing an existing file
for any reason, bring touched declarations into compliance opportunistically;
a dedicated pass is not required unless asked. Loop counters and other variables
that are genuinely reassigned (e.g. a `for` loop's `i`, an accumulator) must NOT
be marked `final` — let `javac` be the check: if marking something `final` fails
to compile, it was actually being reassigned, so leave it without `final`.

---

## Checklist — Step 1.5 (Dogfood checkpoint, in progress)

Runs after the `renderTokens` paren-spacing fix (Step 1.4), since that fix must be
in place before test output is meaningful.

**Critical rules for the file-pair tests:**

- The user may specify which `*_inp.*` file to run next — **do not assume sequential
  order**. Run only the file the user names, unless told to run all remaining.
- Run test files **one at a time**. After each file, if the formatter output does not
  match the `*_out` file, **stop and report the full diff to the user** and wait for
  instruction before attempting any fix. The mismatch may be a bug in the `*_out` file
  itself (authored by hand), not necessarily a formatter bug.
- **Do NOT run `make test`** unless the user explicitly asks. Run individual file pairs
  manually via the formatter directly.
- After each individual file test — pass or fail — update the checklist item inline with
  `(PASS)`, `(FAIL)`, or `(SKIP — <reason>)` and commit STATE.md immediately. Do not
  batch multiple results into one commit.
- **Do not remove `[x]` or `(PASS)` entries from this list**, even after all tests pass.
  Fixing a bug discovered in one file may cause a regression in a previously-passing
  file; the full list allows the user to ask for a specific file to be re-run at any time.
- The same ask-first rule applies to the self-dogfood pass.

`Main.java` standalone-mode cache note: `IndentationDetector` results are cached at
`/tmp/jxmake-code-formatter-indent-<sha256-of-boundary-dir>.cache`, content = detected
style + `\n` + boundary dir `lastModified` epoch ms; invalidated automatically on an
mtime mismatch (RDD_KEY_88).

- [x] CLI arg parsing (`--server`, `--stop`, `--standalone`, `--diff`, `--check`,
      `--out DIR`, `--port N`, file paths); unknown flags / bad usage → exit 2 (RDD_KEY_88)
- [x] Config resolution per file via `Config.resolve()` (RDD_KEY_88)
- [x] `IndentationDetector` temp-file cache for standalone mode (RDD_KEY_88)
- [x] Server auto-connect/delegate via HTTP POST `/format`, with fallback to standalone
      on delegation failure (RDD_KEY_88)
- [x] `--server` mode via `ServerMode.start()` (RDD_KEY_88)
- [x] `--stop` mode via `ServerMode.stop()` (RDD_KEY_88)
- [x] Four output modes: in-place (default), `--diff`, `--check`, `--out DIR` (RDD_KEY_88)
- [x] Exit codes: 0 = success/no changes, 1 = would-change (`--check`) or formatting
      error, 2 = usage error (RDD_KEY_88)
- [x] `README.md` update for Phase 1 + Phase 2 (added `auto` to `indent-style`
      comment; all other Phase 1+2 items already present)
- [ ] File-pair test: `h_core_inp.h` → diff vs `h_core_out.h`
- [x] File-pair test: `c_core_inp.c` → diff vs `c_core_out.c`
- [ ] File-pair test: `hpp_core_inp.hpp` → diff vs `hpp_core_out.hpp`
- [ ] File-pair test: `cpp_core_inp.cpp` → diff vs `cpp_core_out.cpp`
- [ ] File-pair test: `java_core_inp.java` → diff vs `java_core_out.java`
- [ ] File-pair test: `cpp_modern_inp.cpp` → diff vs `cpp_modern_out.cpp`
- [ ] File-pair test: `java_modern_inp.java` → diff vs `java_modern_out.java`
- [ ] File-pair test: `combined_inp.h` → diff vs `combined_out.h`
- [ ] File-pair test: `combined_inp.c` → diff vs `combined_out.c`
- [ ] File-pair test: `combined_inp.hpp` → diff vs `combined_out.hpp`
- [ ] File-pair test: `combined_inp.cpp` → diff vs `combined_out.cpp`
- [ ] File-pair test: `combined_inp.java` → diff vs `combined_out.java`
- [ ] File-pair test: `c_comments_inp.c` → diff vs `c_comments_out.c`
- [ ] File-pair test: `cpp_comments_inp.cpp` → diff vs `cpp_comments_out.cpp`
- [ ] File-pair test: `java_comments_inp.java` → diff vs `java_comments_out.java`

**If any file-pair test shows a mismatch: stop, report the full diff to the user,
and wait for instruction. Do not attempt to fix either the formatter or the `*_out`
file without explicit user direction — the `*_out` files were authored by hand and
may themselves contain errors.**

**After all 15 file-pair tests pass (or are resolved):**
- [ ] Idempotency pass: for each `test/*_out.*` file, run formatter with
      `--out <tmpdir>` and diff against original; all must be empty
- [ ] Dogfood self-format pass: run formatter on all `src/**/*.java`, write
      to `target/dogfood-src/`
- [ ] Dogfood self-format compile: `javac` the `target/dogfood-src/` tree;
      must compile with zero errors
- [ ] Dogfood self-format idempotency: run formatter on `target/dogfood-src/`
      again; must produce no changes
- [ ] Dogfood self-format declaration count: `grep -c "class\|interface\|enum"`
      on original `src/` must equal count on `target/dogfood-src/`

Known pre-existing gaps, discovered during Main.java smoke-testing, left unfixed as
out of scope: `ServerMode.FormatHandler` doesn't resolve `indent-style = auto` before
calling `Formatter.formatOne` (will throw on a server-delegated request for such a
project); `Config.lineEndings()` not applied by `ServerMode.FormatHandler`. Full
detail: RDD_KEY_88.

**Step 2 — AI integration: NOT FEASIBLE (deferred) — see `STATE_NEXT_AI.md`.**

---

## Known Gaps — Not Scheduled

Low-priority issues that do not corrupt output and have no immediate fix planned.

**`* const` cosmetic gap in mixed declaration groups (`DeclarationAlignmentRule`)**
The current separate-postConst-column layout produces a visual gap between `*`
and `const` when shorter types share a group with longer ones:

```c
char*      const c;    // ← gap (current)
char* const      c;    // ← correct per §8
```

Fix (low regression risk): in `splitCppType`, always return `postConst = ""`
and include the full token sequence in `typeAndStar`. No correctness impact
in the current state. East-const (`char const*`) is intentionally not normalized
to west-const.

**`typedef`, `using`, and direct function-pointer declarations not aligned**
`typedef` and `using` are not in `typeKeywords`, and direct function-pointer
declarations (`void (*fp)(int)`) have `)` as their last token rather than an
IDENTIFIER, so `parseDeclaration` returns null for all of these. They pass
through unchanged — no corruption — but a `typedef`/`using`/func-ptr line in
the middle of a plain variable group breaks the group at that point.
Acceptable as preserve-as-is behaviour.

---

## End Goal

- [ ] Dogfood checkpoint complete (see Checklist — Step 1.5)
- [ ] `test/` directory with 15 `*_inp`/`*_out` file pairs committed and confirmed
      correct against the formatter's actual output
