# STATE.md — Formatter Implementation Tracker

---

## Instructions for Claude CLI

**Read this section first, every session, before doing anything else.**

### Session start
1. Read this entire file to understand current state
2. Check the **File Status** table to find the current file (`IN PROGRESS` first,
   then the first `NOT STARTED`)
3. Check the **Current File** checklist for unchecked items — that is where to resume
4. If anything in this file is ambiguous, stop and ask before writing any code

**Do NOT read `FORMATTER_DISCUSSION.md` or `README.md`** unless the user explicitly
asks. All decisions relevant to implementation are recorded in the
**Resolved Design Decisions** index below (full text in `STATE_rdd_log.md`).
`FORMATTER_DISCUSSION.md` is design history and future planning only — large, and
contains nothing the implementer needs beyond what is already indexed here.

> ⛔ **PHASE-2 GATE — DO NOT READ:**
> `STYLE_JAVA17.md`, `STYLE_CPP20.md`, `STATE_NEXT.md`, and `STATE_rdd_log.md` (in full).
> These are off-limits until the End Goal dogfood-test milestone is checked off.
> `STATE_rdd_log.md` may only be accessed via `grep -Fm1 'RDD_KEY_n'` for a specific key.
> Violation of this gate wastes context and risks importing out-of-scope constraints.

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

### `.gitignore` — add these lines if not already present
```
# style-fmt build output
target/
*.jar
```

### When hitting an ambiguity or open question
1. **Stop coding immediately** — do not guess or proceed past the ambiguity
2. Update STATE.md: add the question to **Open Questions**, mark the blocked
   checklist item with `[~]` and a note
3. Commit STATE.md only:
   ```
   git add util/CodingStyle.md/formatter/STATE.md
   git commit -m "$(cat <<'EOF'
style-fmt: block on <question summary>

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
- The next session will resume from the first unchecked item in the Current File checklist

---

## Project Layout

```
util/CodingStyle.md/formatter/
  STATE.md                  ← this file
  README.md
  FORMATTER_DISCUSSION.md
  Makefile
  LICENSE
  src/
    com/jxmake/formatter/
      Main.java
      Config.java
      ServerMode.java
      IndentationDetector.java  ← whole-project dominant-indent-style walker (for `indent-style = keep`)
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
| RDD_KEY_16 | `.style-fmt` inheritance |
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

---

## Open Questions

- [ ] Rule engine grouping — confirm `MiscRule` does not grow too large; split into
      `WhitespaceRule` + `BraceStyleRule` if needed during implementation
- [ ] `reformat_chunks.py` — keep as-is (AI-based, for long files) alongside the
      new JAR, or deprecate once JAR handles long files natively?

---

## File Status

| File | Status |
|---|---|
| `Main.java` | NOT STARTED |
| `Config.java` | NOT STARTED |
| `ServerMode.java` | NOT STARTED |
| `IndentationDetector.java` | NOT STARTED |
| `ScopePipeline.java` | NOT STARTED |
| `TokenizerCore.java` | COMPLETE |
| `ColumnGrid.java` | COMPLETE |
| `ModifierPriority.java` | COMPLETE |
| `CppModifierPriority.java` | COMPLETE |
| `JavaModifierPriority.java` | COMPLETE |
| `ComplexityPaddingEvaluator.java` | COMPLETE |
| `DeclarationAlignmentRule.java` | COMPLETE (`splitStatements` made depth-aware -- see Resolved Design Decisions: "`DeclarationAlignmentRule.splitStatements` depth-awareness fix") |
| `BlockStructureRule.java` | COMPLETE |
| `SwitchRule.java` | COMPLETE |
| `GetterSetterRule.java` | COMPLETE |
| `MiscRule.java` | COMPLETE (§1 `indent-style=keep` cross-file integration deferred to `IndentationDetector.java` -- see Resolved Design Decisions: "§1 indentation scope"; §3.1 condition-interior padding added -- see Resolved Design Decisions: "§3.1 condition-interior padding -- implementation") |
| `CppSpecificRule.java` | COMPLETE (§11 "Include Ordering" dropped from scope -- no such section exists in STYLE_C_CPP.md; see Resolved Design Decisions: "§11 dropped from `CppSpecificRule.java` scope") |
| `JavaSpecificRule.java` | COMPLETE |
| `README.md` (defer until just before Dogfood) | NOT STARTED |

---

## Current File: `ScopePipeline.java` — NOT STARTED (scoping in progress)

> While scoping `Main.java`'s checklist, found that `Main.java`'s real prerequisite is a new file,
> `ScopePipeline.java` (see `Project Layout`/`File Status`, inserted before `Main.java`). Several
> grouping rule classes (`DeclarationAlignmentRule.groupDeclarations`, `GetterSetterRule.groupOneLiners`,
> `MiscRule.groupAssignments`, `MiscRule.parseSignature`) explicitly document that the **caller**
> must find scope/signature boundaries in the whole-file token stream and splice rendered group
> output back in -- no code anywhere does this yet. `ScopePipeline.java` is that caller.
> `Main.java`'s own checklist is deferred until `ScopePipeline.java`'s checklist is written and
> implemented -- `Main.java`'s job will then be much thinner (CLI args, file discovery, config
> loading, one call into `ScopePipeline` per file for the grouping rules, then the remaining simple
> whole-file `enforceX` passes called directly, then output-mode handling).
>
> **Resolved so far** (see Resolved Design Decisions for full detail on each):
> - Boundary-finding + splice-back orchestration lives in this new dedicated class, not
>   `Main.java`/`Config.java` -- "`Main.java` orchestration architecture".
> - STYLE.md §5/§6 (declaration/assignment alignment) apply **anywhere in code, recursively** --
>   not just class/struct bodies, but function/method bodies and every nested block too --
>   "STYLE.md §5/§6 scope -- anywhere in code, recursively".
> - Fixed a latent bug this surfaced: `DeclarationAlignmentRule.splitStatements` was not
>   depth-aware and would have corrupt-split on a scope containing a nested `{ }` block. Ported
>   `MiscRule.splitAssignmentStatements`'s depth-tracking algorithm into it directly (small,
>   mechanical, already-verified-by-smoke-test fix to an already-COMPLETE file, not a new design
>   question) -- "`DeclarationAlignmentRule.splitStatements` depth-awareness fix".
>
> **Resolved (pre-session Q&A — all open items closed, checklist ready to write):**
>
> **Boundary contracts (three distinct granularities):**
> - `groupDeclarations` / `groupAssignments` (§5/§6): direct-content-only slice — nested `{ }`
>   block appears as one opaque consumed statement in the slice; inner tokens not included.
> - `groupOneLiners` (§14): full type-body range including nested method-body tokens — confirmed
>   by reading `GetterSetterRule.splitMembers`, which tracks brace depth and consumes nested bodies.
> - `parseSignature` (§8): signature span only — first modifier/return-type token through the
>   closing `)` of the parameter list; not brace-delimited at all.
>
> **§8 signature finder — definitions only:**
> STYLE.md §8's worked examples show only function definitions; no prototype example exists
> anywhere in the spec. Confirmed: `ScopePipeline`'s signature-finder targets only definitions
> (`)` directly followed by `{`, skipping a possible C++ trailing qualifier or Java `throws`
> clause). Bare prototypes (`void foo();`) are left untouched — same deliberate gap as
> `CppSpecificRule.java` §1. No `AskUserQuestion` needed.
>
> **Recursive walk order — outer-first:**
> `ScopePipeline` visits scopes outer-first then recurses. This is safe because each scope
> receives its own extracted direct-content slice — inner splices never affect outer token
> indices. (Inner-first is only needed when passes share one flat token list with overlapping
> spans, as in §13's nested-switch fix — not the case here.)
>
> **Splice-back and internal pipeline:**
> For each scope: extract slice → group → render → splice rendered output back into the slice
> → re-tokenize the slice → recurse into nested scopes on the fresh token list. Chained via
> re-tokenizing between passes, same precedent as §11/§12/§13/§15 throughout this codebase.
>
> **Hard pipeline-ordering constraints (unchanged, carry forward to checklist):**
> - `GetterSetterRule` must run before any Allman-conversion pass (both languages) --
>   RDD_KEY_60.
> - Several `MiscRule`/`BlockStructureRule`/`SwitchRule` passes have ordering requirements
>   relative to each other -- see each section's own RDD entry.
>
> Resume by writing the actual checklist items for this file — all design questions are now
> resolved, no `AskUserQuestion` needed before implementation begins.

---

## Config Keys and Defaults

Configurable values with their in-class defaults. All overridable via config file or CLI.

```properties
# ── Structural constants ──────────────────────────────────────────────────────
line-length                = 100
indent-size                = 4
indent-style               = spaces          # spaces | tabs | keep
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
| `APP_NAME` | `"style-fmt"` | `Config` |
| `LOCKFILE_NAME` | `"server.lock"` | `ServerMode` |
| `CONFIG_DIR` | `".config/style-fmt"` | `Config` |
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

## End Goal
- [ ] Dogfood test — run formatter on its own `src/` tree, verify style compliance and that `make` still succeeds after

Once the above is checked off, the formatter's core (Tier 1 + Tier 2, STYLE.md /
STYLE_C_CPP.md / STYLE_JAVA.md) is considered complete. Phase 2 — Java 17+ and
C++20+ construct support — begins at that point, tracked separately in
`STATE_NEXT.md` (which also covers trimming `AI_PREAMBLE.md` down to its
post-JAR Tier-3-only scope). Do not open or read `STATE_NEXT.md`,
`STYLE_JAVA17.md`, or `STYLE_CPP20.md` before this milestone is checked off.
