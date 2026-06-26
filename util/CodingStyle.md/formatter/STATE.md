# STATE.md — Formatter Implementation Tracker

---

## Instructions for Claude CLI

**Read this section first, every session, before doing anything else.**

### Session start
1. Read this entire file to understand current state
2. Check the **File Status** tables to find the current file (`IN PROGRESS` first,
   then the first `NOT STARTED`). All files are `COMPLETE`; resume work is in the
   dogfood checkpoint — see **Checklist — Phase 3, Step 1.5** below.
3. Resume from the first unchecked item in **Checklist — Phase 3, Step 1.5**.
   **Step 2 — AI integration is NOT FEASIBLE** (deferred — see `STATE_NEXT_AI.md`).
4. If anything in this file is ambiguous, stop and ask before writing any code

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
1. Mark it `COMPLETE` in the relevant File Status table
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
| RDD_KEY_83 | `JavaModifierPriority` column order for `abstract`/`sealed`/`non-sealed`/`final`/`volatile` -- declaration-kind-specific orderings merged into one map |
| RDD_KEY_84 | `record` named-construct detection through component list / `implements` clause / compact constructor |
| RDD_KEY_85 | C++ concepts/`requires` clause implementation in `CppSpecificRule.java` |
| RDD_KEY_86 | `MiscRule.java` call/declaration line-breaking architecture -- option 2 must bypass `parseSignature`, option 1 reuses it + new `renderDropped` |
| RDD_KEY_87 | `MiscRule.enforceCallLineBreaking` implementation scope decisions (nesting, comment bail-out, call-vs-declaration classification, new preserve-groups grid) + `collapseTokensToOneLine` bugfix |
| RDD_KEY_88 | `Main.java` implementation (Step 1.5) -- CLI parsing, config resolution, indent-style temp-cache, server auto-connect/delegate, `--server`/`--stop`, output modes, exit codes |

---

## Open Questions

*(none)*

---

## File Status — Phase 1

| File | Status |
|---|---|
| `Config.java` | COMPLETE |
| `ServerMode.java` | COMPLETE |
| `Formatter.java` | COMPLETE |
| `IndentationDetector.java` | COMPLETE |
| `ScopePipeline.java` | COMPLETE (reopened during `Formatter.java` smoke-testing to fix a C++ access-specifier-label span bug -- see Resolved Design Decisions: "`ScopePipeline.splitTopLevelSpans` never closed a span at a C++ access-specifier label, merging it into the following member") |
| `TokenizerCore.java` | COMPLETE |
| `ColumnGrid.java` | COMPLETE |
| `ModifierPriority.java` | COMPLETE |
| `CppModifierPriority.java` | COMPLETE |
| `JavaModifierPriority.java` | COMPLETE |
| `ComplexityPaddingEvaluator.java` | COMPLETE |
| `DeclarationAlignmentRule.java` | COMPLETE (`splitStatements` made depth-aware -- see Resolved Design Decisions: "`DeclarationAlignmentRule.splitStatements` depth-awareness fix"; reopened during `Formatter.java` smoke-testing to reject a non-keyword, non-identifier type lead -- see Resolved Design Decisions: "`DeclarationAlignmentRule` misparses a bare `++j;`/`--j;` statement as a fake field declaration") |
| `BlockStructureRule.java` | COMPLETE |
| `SwitchRule.java` | COMPLETE |
| `GetterSetterRule.java` | COMPLETE |
| `MiscRule.java` | COMPLETE (§1 `indent-style=auto` cross-file integration deferred to `IndentationDetector.java` -- see Resolved Design Decisions: "§1 indentation scope"; §3.1 condition-interior padding added -- see Resolved Design Decisions: "§3.1 condition-interior padding -- implementation"; reopened during `Formatter.java` smoke-testing to add structural detection for closing-comment labels -- see Resolved Design Decisions: "`MiscRule.enforceCommentStyle` relied on pipeline ordering (not detection) to skip closing-comment labels, breaking idempotency") |
| `CppSpecificRule.java` | COMPLETE (§11 "Include Ordering" dropped from scope -- no such section exists in STYLE_C_CPP.md; see Resolved Design Decisions: "§11 dropped from `CppSpecificRule.java` scope"; reopened during `Formatter.java` smoke-testing to add the §14 one-liner adjacency heuristic -- see Resolved Design Decisions: "Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient"; reopened during `ServerMode.java` smoke-testing to fix an infinite loop when `)`/`{` are already adjacent -- see Resolved Design Decisions: "Allman-brace render-loop infinite loop when `)`/`{` are already adjacent") |
| `JavaSpecificRule.java` | COMPLETE (reopened during `Formatter.java` smoke-testing to add the §14 one-liner adjacency heuristic -- see Resolved Design Decisions: "Supersedes RDD_KEY_60 -- Allman pass actually destroys §14 grouping, ordering alone insufficient"; reopened during `ServerMode.java` smoke-testing to fix an infinite loop when `)`/`{` are already adjacent -- see Resolved Design Decisions: "Allman-brace render-loop infinite loop when `)`/`{` are already adjacent") |

---

## File Status — Phase 2

| File | Status |
|---|---|
| `JavaModifierPriority.java` (sealed/non-sealed addition) | COMPLETE (see RDD_KEY_83; `TokenizerCore.java` and `JavaSpecificRule.java` also touched -- new keywords, new `enforcePermitsClauseLineBreaking` pass) |
| `JavaSpecificRule.java` (record) | COMPLETE (see RDD_KEY_84; `TokenizerCore.java` and `BlockStructureRule.java` also touched) |
| `JavaSpecificRule.java` (switch expressions) | COMPLETE (new `enforceSwitchExpressionArrowAlignment`, wired into `Formatter.java`; no RDD needed -- STYLE_JAVA17.md §7 already pre-resolved the only design question, the block-body all-or-nothing bail-out) |
| `TokenizerCore.java` (text blocks) | COMPLETE (new `isTextBlockOpener`/`emitTextBlock`, opaque `STRING` token spanning the whole block, mirrors `emitBlockComment`'s internal-newline pattern; no RDD needed) |
| `DeclarationAlignmentRule.java` (`var`) | COMPLETE (added `"var"` to `TYPE_KEYWORDS_JAVA`; confirmed-not-no-op) |
| `JavaSpecificRule.java` (pattern matching) | COMPLETE (confirmed true no-op, zero code changes) |
| `CppModifierPriority.java` (consteval/constinit addition) | COMPLETE (new shared-rank column for `constexpr`/`consteval`/`constinit` between `static` and `volatile`/`const`; `consteval`/`constinit` also added to `KEYWORDS_CPP` -- `constexpr` alone was already present) |
| `DeclarationAlignmentRule.java` (structured bindings) | COMPLETE (new `parseStructuredBinding` helper in `parseDeclaration`, cpp only; landed here rather than in a new `CppSpecificRule.java` method since `render`'s existing machinery already covered the atomic `[a, b, c]` name-cell additively) |
| `CppSpecificRule.java` (concepts/requires) | COMPLETE (see RDD_KEY_85; `TokenizerCore.java` and `BlockStructureRule.java` also touched -- new keywords, `pendingConceptName`, `isConceptRequiresExpressionBody`. Also fixed in the same pass: `<=>` tokenization and `co_await`/`co_return`/`co_yield` keywords for the `<=>`/coroutines/init-statement checklist item below -- both pre-existing TokenizerCore gaps, no RDD needed) |
| `AI_PREAMBLE_FULL.md` / `AI_PREAMBLE_AESTHETIC.md` presence + `README.txt` references | COMPLETE -- verified clean, no stale `AI_PREAMBLE.md` to delete |

### Checklist — Phase 2 (C++17/20/23, all complete)

- [x] `consteval` / `constinit`
- [x] Structured bindings
- [x] Concepts / `requires` clauses (RDD_KEY_85)
- [x] `<=>`, coroutines, init-statement `if`/`switch` -- also fixed two missing-keyword
      tokenizer gaps surfaced by verification (`<=>` in `MULTI_CHAR_OPS`,
      `co_await`/`co_return`/`co_yield` in `KEYWORDS_CPP`). Surfaced but left unfixed as
      out-of-scope: a pre-existing, unrelated bug where `auto x = regularFunc();` renders
      as `auto x = regularFunc ( );` -- reproduces on the pristine pre-Phase-2 build, a
      future Tier-1 `DeclarationAlignmentRule` pass should pick it up.

---

## File Status — Phase 3

| File | Status |
|---|---|
| `STYLE.md` (add call line-breaking forms to §8) | COMPLETE (commit b222345, predates this checklist pass -- verified matches RDD_EXT_4/5/6/7) |
| `MiscRule.java` (option 1 dropped form + option 2 preserve-groups+align, for both calls and declarations) | COMPLETE (new `enforceCallLineBreaking` pass + helpers, wired into `Formatter.formatOne`; verified via 13-scenario smoke test; see RDD_KEY_86/87) |
| `Main.java` | COMPLETE (see Checklist — Phase 3, Step 1.5; RDD_KEY_88) |
| `DeclarationAlignmentRule.java` + `MiscRule.java` (`renderTokens` paren-spacing fix) | COMPLETE (see Checklist — Phase 3, Step 1.4) |
| `Config.java` (ai-assist, ai-endpoint, ai-model, ai-retry-interval keys) | NOT FEASIBLE (Step 2 deferred — see Checklist — Phase 3, Step 2) |
| `AiDecisionClient.java` (OpenAI-compatible `/v1/chat/completions` caller) | NOT FEASIBLE |
| `AI_DECISION_PROMPT.md` (prompt template — separate from AI_PREAMBLE.md) | NOT FEASIBLE |
| `MiscRule.java` (Tier-3 AI decision hooks) | NOT FEASIBLE |
| `README.md` (phase 1+2 dogfood update; ai-assist section) | COMPLETE (ai-assist section removed; phase 1+2 update done) |
| `FORMATTER_DISCUSSION.md` (add Step 2 NOT FEASIBLE decision to Key Decisions table) | NOT STARTED |

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

## Checklist — Phase 3

**Step 1 — Deterministic extensions (complete):**

- [x] `STYLE.md` §8 updated with the four call-line-breaking candidate forms and
      comment-handling rules (commit b222345; verified matches RDD_EXT_4–7, with one
      deliberate simplification: multi-line source always preserves grouping (option 2)
      rather than offering a 0/2/3 AI choice, consistent with Step 2 being NOT FEASIBLE)
- [x] `MiscRule.enforceCallLineBreaking` — option 1 (dropped) + option 2
      (preserve-groups+align), for both calls and declarations (RDD_KEY_86, RDD_KEY_87)
- [x] `parseSignature` comment-handling verified safe for option 1 — strips comments via
      `significantOnly()` rather than bailing; option 2 bypasses `parseSignature` entirely
      (RDD_KEY_86)
- [x] Options 0 (inline) and 3 (one-per-line) verified working for function *calls*
      (not just signatures) inside the same `enforceCallLineBreaking` pass (RDD_KEY_87)

No-AI fallback rule (ai-assist off or endpoint unavailable): see RDD_EXT_8.

**Step 1.4 — Complexity padding: universal `()` / `[]` + `renderTokens` bracket fix**

Two coupled bugs that must land in the same commit.

**Background — STYLE.md §3.1 is universal, not just control-flow:**
The style guide states the rule applies to all `()` and `[]`, not only
`if`/`while`/`for`/`switch` conditions. The current implementation has two
gaps that violate this:

**Bug A — `enforceConditionComplexityPadding` scope too narrow (`MiscRule.java`):**
Currently only pads/tightens `(` immediately following a control-flow keyword.
All other `()` and `[]` are left untouched, producing wrong output:

```
// WRONG (current):
memset(buf, 0, frames * sizeof(float));   // outer ( contains ( → should be loose
items_.push_back(std::move(item));         // ( contains ( → should be loose
process(new ArrayList<String>());          // ( contains ( → should be loose
cfg_(std::move(cfg));                      // ( contains ( → should be loose
func();                                    // empty → tight ✓ (already correct)
a[10];                                     // no ( or [ in content → tight ✓

// CORRECT (after fix):
memset( buf, 0, frames * sizeof(float) );
items_.push_back( std::move(item) );
process( new ArrayList<String>() );
cfg_( std::move(cfg) );
func();
a[10];
// Also correct after fix:
func( other() );                           // ( contains ( → loose
func( (a + b) * c );                       // ( contains ( → loose
a[ b[i] ];                                 // [ contains [ → loose
( A[ b[n] + 1 ] );                         // outer ( contains [ → loose,
                                           // middle [ contains [ → loose,
                                           // inner [n] empty → tight
```

Fix for Bug A: rename `enforceConditionComplexityPadding` to
`enforceComplexityPadding` and remove the `TIGHT_PAREN_KEYWORDS` gate so
it scans every `(` and `[` in the token stream. Add one exclusion: skip
any `(` where the immediately preceding significant token is an IDENTIFIER
**and** the matching `)` is followed (skipping whitespace/comments) by
`{`, `->`, or any of `const`/`override`/`noexcept`/`throws`/`final` —
this identifies function/method definition parameter lists, which must not
be padded regardless of their content (e.g. `process(float[] buffer, int n)`
must stay tight even though `[]` is inside). The `isLoose` evaluator in
`ComplexityPaddingEvaluator` is already correct and **unchanged**.
Also update the call site in `Formatter.java` to use the new method name.

**If anything in this fix is unclear, stop and ask the user with a concrete
example of the input token sequence and expected output before proceeding.**

**Bug B — `renderTokens` bracket spacing (`DeclarationAlignmentRule.java` and
`MiscRule.java`):**
`renderTokens` is designed for type-token lists (modifier + type + name) but
is also called on `initTokens` and `sizeTokens` which can contain call
expressions. Its `isTightToken`/`needsSpaceBetween` helpers do not treat `(`
and `)` (or `]`) as tight, producing spurious spaces:

```
// WRONG (current) — renderTokens called on initTokens:
auto x    = arr.size ( );      // ) not tight → space before )
int  n    = arr.size ( );      // same
// CORRECT (after fix):
auto x    = arr.size();
int  n    = arr.size();
```

Fix for Bug B: in `needsSpaceBetween` in **both** `DeclarationAlignmentRule`
and `MiscRule`, add:
1. No space before `)` or `]` — always tight as `cur`.
2. No space before `(` when `prev` is IDENTIFIER — call site join.
3. `isPunct(prev, "(")` and `isPunct(prev, "[")` added to the existing
   prev-is-tight guard (alongside `ANGLE_BRACKET_OPEN`/`::`/`[`) — no space
   immediately after an open bracket.

After this fix, `renderTokens` is bracket-neutral: it neither adds nor
removes spacing around `()` and `[]`. Bug A's fix (Phase 4) then applies
the correct loose/tight padding to the whole-file token stream, including
whatever Phase 0 (`ScopePipeline` / `DeclarationAlignmentRule`) re-rendered.
The two fixes work together: Bug B makes Phase 0 stop producing wrong spacing;
Bug A makes Phase 4 apply correct spacing everywhere.

**If anything in this fix is unclear, stop and ask the user with a concrete
example of the input token sequence and expected output before proceeding.**

- [x] Bug A: rename and extend `enforceConditionComplexityPadding` →
      `enforceComplexityPadding` in `MiscRule.java`; add the function-
      definition-signature exclusion; update `Formatter.java` call site
- [x] Bug B: fix `needsSpaceBetween` in `DeclarationAlignmentRule.java`
- [x] Bug B: fix `needsSpaceBetween` in `MiscRule.java` (identical change)
      Also added `isOp(t, ".")` to `isTightToken` and `isOp(prev, ".")` to the
      prev guard in both files -- `.` is treated identically to `::` (tight on
      both sides), required by the `arr.size()` smoke test case.
- [x] Smoke-test: verify these cases produce correct output after both fixes:
      - `int n = arr.size();` → tight (empty args, declaration init) ✓
      - `auto x = func( other() );` — content of `func(...)` has `(` → loose;
        content of `other(...)` is empty → tight ✓
      - `memset( buf, 0, n * sizeof(float) );` — standalone call ✓
      - `process(float[] buffer, int n) { }` — method sig followed by `{`,
        stays tight ✓
      - `a[ b[i] ]` inside condition — `[` contains `[` → loose ✓
      - `a[10]` — tight ✓
- [x] Commit `MiscRule.java`, `DeclarationAlignmentRule.java`, `Formatter.java`,
      and `STATE.md` together

**Step 1.5 — Dogfood checkpoint (in progress):**

Runs after Step 1.4 rather than before it, since the paren/bracket spacing
fix must be in place before test output is meaningful.

**Critical rules for this step:**
- The user may specify which `*_inp.*` file to run next — **do not assume sequential
  order**. Run only the file the user names, unless told to run all remaining.
- Run test files **one at a time**, not all at once. After each file, if
  the formatter output does not match the `*_out` file, **stop and ask the
  user** before attempting any fix — the mismatch may be a bug in the
  `*_out` file itself (authored by hand, not confirmed by the formatter),
  not necessarily a formatter bug. Record which files passed and which did
  not in STATE.md as you go, so progress is preserved if quota runs out.
- **Do NOT run `make test`** unless the user explicitly asks. Run individual
  file pairs manually via the formatter directly.
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

`Main.java` standalone-mode cache note: `IndentationDetector` results are cached at
`/tmp/jxmake-code-formatter-indent-<sha256-of-boundary-dir>.cache`, content = detected style + `\n`
+ boundary dir `lastModified` epoch ms; invalidated automatically on an mtime mismatch
(RDD_KEY_88).

- [x] CLI arg parsing (`--server`, `--stop`, `--standalone`, `--diff`, `--check`,
      `--out DIR`, `--port N`, file paths); unknown flags / bad usage → exit 2 (RDD_KEY_88)
- [x] Config resolution per file via `Config.resolve()` (RDD_KEY_88)
- [x] `IndentationDetector` temp-file cache for standalone mode — see note above
      (RDD_KEY_88)
- [x] Server auto-connect/delegate via HTTP POST `/format`, with fallback to standalone
      formatting on delegation failure (RDD_KEY_88)
- [x] `--server` mode via `ServerMode.start()` (RDD_KEY_88)
- [x] `--stop` mode via `ServerMode.stop()` (RDD_KEY_88)
- [x] Four output modes: in-place (default), `--diff` (self-written unified diff,
      single hunk with clamped context), `--check`, `--out DIR` (RDD_KEY_88)
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

**If any file-pair test above shows a mismatch: stop, report the full diff to the
user, and wait for instruction. Do not attempt to fix either the formatter or the
`*_out` file without explicit user direction — the `*_out` files were authored by
hand and may themselves contain errors.**

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
out of scope (flagged to user, not part of this checklist): `ServerMode.FormatHandler`
doesn't resolve `indent-style = auto` before calling `Formatter.formatOne` (will throw
on a server-delegated request for such a project — masked in practice by `Main`'s
fallback-to-standalone-on-delegation-failure behavior); `Config.lineEndings()` is
applied by `Main.applyLineEndings()` for standalone/in-process formatting but not yet
by `ServerMode.FormatHandler`. Full detail: RDD_KEY_88.

**Step 2 — AI integration: NOT FEASIBLE (deferred) — see `STATE_NEXT_AI.md`.**

---

## Known Gaps — Not Scheduled

Low-priority issues that do not corrupt output and have no immediate fix
planned. Recorded here so they are not rediscovered in future sessions.

**`* const` cosmetic gap in mixed declaration groups (`DeclarationAlignmentRule`)**
The current separate-postConst-column layout produces a visual gap between `*`
and `const` when shorter types share a group with longer ones:

```c
char*      const c;    // ← gap (current)
char* const      c;    // ← correct per §8
```

Fix (low regression risk): in `splitCppType`, always return `postConst = ""`
and include the full token sequence in `typeAndStar`. No correctness impact
in the current state — all variants align and render without corruption.
East-const (`char const*`) is intentionally not normalized to west-const.

**`typedef`, `using`, and direct function-pointer declarations not aligned**
`typedef` and `using` are not in `typeKeywords`, and direct function-pointer
declarations (`void (*fp)(int)`) have `)` as their last token rather than an
IDENTIFIER, so `parseDeclaration` returns null for all of these. They pass
through unchanged — no corruption — but a `typedef`/`using`/func-ptr line
in the middle of a plain variable group breaks the group at that point, so
the surrounding variables end up in separate alignment groups:

```c
int    count = 0;
void (*cb)(int) = NULL;   // ← breaks group; count and ratio in separate groups
float  ratio = 1.0f;
```

No fix planned — aligning these into a variable column grid would be
semantically odd. Acceptable as preserve-as-is behaviour.

---

## End Goal

- [ ] Dogfood checkpoint complete (see Checklist — Phase 3, Step 1.5)
- [ ] `test/` directory with 15 `*_inp`/`*_out` file pairs + `README.txt` committed
      and confirmed correct against the formatter's actual output
