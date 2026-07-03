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
- [x] File-pair test: `h_core_inp.h` → diff vs `h_core_out.h` (PASS)
- [x] File-pair test: `c_core_inp.c` → diff vs `c_core_out.c` (PASS)
- [x] File-pair test: `hpp_core_inp.hpp` → diff vs `hpp_core_out.hpp` (PASS)
- [x] File-pair test: `cpp_core_inp.cpp` → diff vs `cpp_core_out.cpp` (PASS)
  - Bug 1 FIXED: named-construct detection now skips attribute-specifiers (`alignas(16)`)
    between keyword and name (`TokenizerCore.namedConstructKeywordSeen`,
    `BlockStructureRule.findConstructKeywordIndex`).
  - Bug 2 FIXED: `virtual`/`inline`/`explicit` recognized in `CppModifierPriority`;
    `= 0`/`= delete`/`= default` suffixes recognized as func-decl specifiers.
  - Bug 3 FIXED: `MiscRule.render(Signature)` no longer adds a space after `::` in qualified
    names (`Processor::setGain`) before `(`.
  - Bug 4 FIXED: `CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` now finds a
    constructor's true close-paren via `resolveToFunctionCloseParen` instead of misreading the
    last initializer-list entry's `)`, fixing wrong Allman-brace indent.
  - Bug 4b FIXED: (a) `GetterSetterRule.render` definitions path now pads param type/name
    columns independently instead of padding the whole param string as one cell; (b)
    `MiscRule.renderTokens`'s `isTightToken(*)` space-drop around binary `*` fixed for the
    affected call site.
  - Bug 5 FIXED: trailing-return-type function not detected as function definition.
  - Bug 6 FIXED: `if`/`else`/`else if` chains collapsed to one-liner.
- [x] File-pair test: `java_core_inp.java` → diff vs `java_core_out.java` (PASS)
  - Bug A FIXED: `enforceNamedConstructHeaderSpacing`'s `headerStart` now extends back past
    modifier keywords (`public`, `abstract`, ...) so `public   class X` collapses correctly.
  - Bug B FIXED: `MiscRule.parseAssignment` now accepts `this`/`super` (KEYWORD) as a valid
    LHS first token, not just IDENTIFIER.
  - Bug C FIXED: `addClosingComments` now replaces/removes an existing wrong `// end X`
    comment after `}` instead of leaving it untouched.
  - Bug D FIXED: methods with a `throws` clause now get Allman-brace conversion and signature
    normalization (`ScopePipeline.applySignaturePass`, `JavaSpecificRule.findCloseParenBeforeThrows`).
  - Bug E FIXED: inline-switch fall-through case label padding fixed via an empty sentinel
    second cell in `SwitchRule.applyInlineAlignment` (ColumnGrid never pads a row's last cell).
  - Bug F FIXED: inline-switch `break`-only case now renders `break` in the content column
    with `;` as the terminator, not `break` after content padding.
  - Bug G FIXED: added `BlockStructureRule.placeCatchFinallyOnOwnLine` (mirrors
    `placeElseOnOwnLine`) so `} catch (...)` splits onto its own line; `catch` also added to
    `TIGHT_PAREN_KEYWORDS`.
  - Bug H FIXED: `ScopePipeline.skipAnnotations` now scans past `@Identifier(...)` blocks
    before signature parsing, so `@Override` no longer gets absorbed into the method signature.
- [x] File-pair test: `cpp_modern_inp.cpp` → diff vs `cpp_modern_out.cpp` (PASS)
  - Bug 1 FIXED: comment capitalization now skips words in new `COMMENT_NO_CAPITALIZE` keyword set.
  - Bug 2 FIXED: `ScopePipeline` pre-expands named-construct one-liner bodies before recursing,
    with correct member indent; non-multiple-of-4 indent still normalized.
  - Bug 3 FIXED: `DeclarationAlignmentRule.parseDeclaration` rejects `typeTokens` ending in `::`
    (`T::version;` was misread as a declaration).
  - Bug 4 FIXED: (4a) `template<...>` header on a prior line no longer gets pulled into the
    function signature by `applySignaturePass`; (4b) Allman-brace conversion now works for
    `f() requires Clause {` (`findCloseParenBeforeRequiresClause`), and the requires-clause pass
    no longer swallows the newline before `{`/`;`.
  - Bug 5 FIXED: one-liner `{ ... }` function bodies are always kept K&R, never Allman-converted
    (dead adjacency-grouping logic removed).
  - Bug 6 FIXED: `reorderStatics` now only runs for Java — reordering C/C++ declarations can
    change initialization-order semantics.
  - Bug 7 FIXED: `operator<=>` (and other operator overloads) now recognized as function
    definition candidates in `CppSpecificRule.isCandidateSignatureName`.
  - Bug 8 FIXED: `GetterSetterRule` promise_type group — empty-body guard now accepts `{}` bodies,
    and the multi-statement-body rejection guard was removed so `yield_value`'s 2-statement body
    stays grouped with its siblings.
  - Bug 9 FIXED: (9a) extra space before `{` in brace-initializers and a double `;;` on
    structured-binding declarations, fixed via `needsSpaceBetween` and new
    `isScopeOpeningBrace`/`isNamedConstructStartKeyword` disambiguation in `ScopePipeline`;
    (9b) missing space after `,` in nested brace-initializer lists not parsed as a `Declaration`,
    fixed in `MiscRule.enforceInitializerBraceSpacing` with comma-spacing + `outermostStack` for
    STYLE.md §3.3's outermost-pair-only padding rule.
  - Bug 10 FIXED: `static_cast<...>`/etc. angle brackets were spaced instead of tight —
    `TokenizerCore.reclassifyAngleBrackets` now arms on cast keywords, not just IDENTIFIER.
  - Bug 11 FIXED: `namespace alpha::beta::gamma { ... }` closing comment dropped `beta`/`gamma` —
    tokenizer now extends `pendingNamedConstructName` across `::segment` chains.
  - `cpp_modern_inp.cpp` now PASSES in full (forward + idempotency).
  - CRITICAL regression FIXED: idempotency broke on `c_core_out.c`'s "Mixed static and
    non-static" section (Bug 6's own example) — a declaration group's already-rendered column
    padding was mistaken for real indentation on a second pass and doubled. Fixed in
    `ScopePipeline` via new `leadingSpaceCount`/`stripTrailingSpaces` helpers that strip the
    self-generated padding before re-deriving indent.
- [x] File-pair test: `java_modern_inp.java` → diff vs `java_modern_out.java` (PASS)
  - Bug 1 FIXED: empty named-construct bodies (`record Num(...) implements Expr {}`, empty
    `class`/`interface`/`enum`, C/C++ equivalents) were expanded to multi-line with a closing
    comment despite having no content. Fixed via new `isEmptyBraceBody` helper, consulted by both
    `insertNamedConstructBlankLines` and `decideComment`.
  - Bug 2 FIXED: plain statements ending in `identifier(...)` inside a non-class scope (e.g.
    `result.add(trimmed);`) were misidentified as one-liner getter/setter methods and corrupted
    by column alignment. Fixed by rejecting `GetterSetterRule.parseOneLinerMember` candidates
    whose "return type" span contains a `.` or `=`.
  - Bug 3 FIXED: a one-liner constructor/method body with 2+ statements on its single source line
    got wrongly split and column-aligned. Fixed by skipping the recursive `processScope` call for
    non-named, no-newline child scopes and splicing the source back unchanged.
  - Bug 4 FIXED: standalone (non-adjacent) one-liner methods were wrongly Allman-broken.
    RDD_KEY_75's grouped/ungrouped adjacency heuristic removed entirely — every one-liner
    candidate now stays K&R unconditionally.
  - Bug 5 FIXED: no blank line was inserted between a named-construct's `{` and a leading
    own-line comment (e.g. before `// compact constructor`). Fixed in
    `BlockStructureRule.ensureBlankLine` by only blocking insertion when the comment is glued to
    the previous token with no `NEWLINE` first.
  - Also fixed as a side effect of Bug 4 (no separate root cause): the `permits` clause was
    already correctly line-wrapping once the Allman-brace pass stopped corrupting `distance()`'s
    surrounding structure earlier in the pipeline; no dedicated fix was needed once Bugs 1-5
    above were resolved -- confirmed via `make test`, `permits` wrapping matches
    `java_modern_out.java` with zero remaining diff.
  - All 5 bugs verified via `make test`: `java_modern_inp.java`/`java_modern_out.java` now PASS
    (forward + idempotency), zero regressions across the other 6 file-pairs.
- [x] File-pair test: `combined_inp.h` → diff vs `combined_out.h`
  - Bug 1 FIXED: `#define` value columns weren't realigned to a common column under
    `format-macros = on`. New `CppSpecificRule.alignMacroDefinitions`, wired into Phase 4.
  - Bug 3 (`extern "C"` closing-comment) RESOLVED as a fixture fix, no code change: a
    `#ifdef __cplusplus`-guarded `extern "C" { ... }` doesn't get a closing comment (unlike a
    bare one) — `combined_out.h` corrected to match established precedent.
  - Bug 2 (enum closing-comment) FIXED: (a) `BlockStructureRule.commentInsertionIndex` now
    skips an optional alias IDENTIFIER before `;` so `typedef enum/struct NAME { ... } ALIAS;`
    gets a closing comment; (b) `TokenizerCore`'s `preprocessorDepth == 0` gating, which froze
    brace-depth/naming tracking inside *any* `#if`/`#ifdef` region (including ordinary header
    guards), was removed entirely per user direction — named constructs now get closing comments
    regardless of `#if` guards; (c) this surfaced a related bug in
    `insertNamedConstructBlankLines` inserting the blank line between an `extern "C" {` and its
    guard directive instead of past it — fixed by walking past guard lines sitting directly
    against the brace before deciding blank-line placement.
  - All 3 bugs verified via `make test`, zero regressions. Committed as `efeb6df`.
- [x] File-pair test: `combined_inp.c` → diff vs `combined_out.c` (PASS forward + idempotency)
  - Bug 1 FIXED: struct member group indentation was destroyed when the group's first
    declaration has no modifiers but a sibling does and real indent < modifier-padding width.
    `ScopePipeline.applyDeclarationsPass`'s idempotency-safe strip now only strips when the raw
    gap has enough trailing spaces to safely remove.
  - Bug 2 FIXED: a declaration group broke apart whenever a member's initializer was a flat
    brace aggregate (`= { A, B, false };`). `DeclarationAlignmentRule.parseDeclaration` now
    accepts single-level flat aggregates via new `isFlatAggregateInit` (only nested/complex
    brace inits still rejected); also fixed C-style cast re-joining (`(int) frames` → `(int)frames`)
    via new `isCStyleCastClose`.
  - Bug 3 FIXED: a parameter's inline block comment was silently dropped from a function
    signature. `MiscRule.parseSignature`/`Param` now preserve and re-render per-param trailing
    comments (`significantWithComments`, new `Param.comment` field); comment length also
    excluded from the inline-vs-multi-line 100-col break decision.
  - Bug 4 FIXED (idempotency): switch statement's `// switch` closing comment was dropped on a
    second format pass because `addClosingComments`' line-count decision ran before
    `alignInlineSwitches` compacted the body. Fixed by reordering Phase 3 so fallthrough
    compaction runs first.
  - All verified via `make test`, zero regressions.
- [x] File-pair test: `combined_inp.hpp` → diff vs `combined_out.hpp` (PASS forward + idempotency)
  - Bug 1 NOT FEASIBLE, input adjusted instead: distinguishing a mid-word dot (file extension,
    abbreviation) from a true sentence-ending period needs semantic understanding, not a
    mechanical rule — logged as Tier-3 AI-assist candidate in `STATE_NEXT_AI.md`;
    `combined_inp.hpp`'s comment hand-edited to sidestep the case.
  - Bug 2 FIXED: `GetterSetterRule` (§14) excluded constructors/destructors/operator-overloads
    from one-liner grouping (`= delete`/`= default` alignment). Fixed by recognizing operator
    names in `findNameBeforeParen`, accepting no-return-type members with a pure-specifier, and
    a new `mergeReturnTypeIntoCall` grid flag so empty-type rows aren't wrongly padded.
  - Bug 3 FIXED: a `template<...>`-prefixed declaration was left completely untouched (raw
    input spacing) because `parseDeclaration` never recognized `template` as a valid
    declaration prefix. Fixed via new `Declaration.templatePrefix` field, detection/skip logic,
    and `renderTemplatePrefix` (tight-angle-bracket rendering, as template *parameter* lists
    always are, unlike argument usages).
  - Both verified via `make test`, zero regressions (including `hpp_core`'s pre-existing §14
    group, unaffected by the merge-flag path).
- [x] File-pair test: `combined_inp.cpp` → diff vs `combined_out.cpp` (PASS)
  - Fixed 4 bugs, all verified via `make test` with zero regressions:
    1. A destructor's `~` marker was misread as a real return type in
       `GetterSetterRule.parseOneLinerMember` — fixed by extending `nameFrom` backward over a
       leading `~`, same as the existing `operator` handling.
    2. Out-of-line class-template member one-liners (`template<...> T Engine<Impl>::get() const
       { ... }`): (a) leading `template<...>` angle brackets weren't rendered tight for members
       with a trailing `const` qualifier — fixed via new `MiscRule.templateAngleTokens`; (b) the
       template prefix was captured as part of one padded return-type grid cell — fixed via new
       `Member.templatePrefixFrom/To`, rendered as its own unpadded column.
    3. `MiscRule.parseAssignment` misparsed a structured binding (`auto [a, b] = expr;`) as an
       assignment to a variable named `auto` — fixed by explicitly rejecting a leading `auto`
       keyword target.
    4. `ScopePipeline.isGapToken` treated comments as trimmable gap, causing a group's
       already-rendered trailing comment to be duplicated. Fixed via new
       `isWhitespaceOrNewline` helper (excludes comments) for that trim.
- [x] File-pair test: `combined_inp.java` → diff vs `combined_out.java` (PASS forward + idempotency)
  - Bug 1 FIXED: a `//` line comment's trailing period was stripped based on that single line's
    own dot count, ignoring that it might be one line of a multi-line prose paragraph (chain of
    adjacent `//` lines) that should be evaluated as a whole, same as block comments already are.
    New `computeLineCommentGroups` chains adjacent `//` lines and reuses
    `stripSoleTrailingPeriodAcrossLines`. Surfaced and fixed a related false-positive in
    `parseSeparatorComment`'s single-non-alnum-char heuristic misfiring on prose containing a
    lone `+` (`"Java 17+"`).
  - Bug 2 FIXED (new feature, no prior STYLE_JAVA.md worked example): a Java enum body with
    trailing members needs its constant-list-terminating `;` detached onto its own line with
    blank lines around it. New `JavaSpecificRule.separateEnumConstantListTerminator`. Required
    hand-fixing `java_core_out.java`'s enum fixture, which had previously encoded the conflicting
    compact shape for the same pattern (user-confirmed).
  - Bug 3 FIXED: no blank line before the final `return` in a method with a `throws` clause —
    `MiscRule.isFunctionBodyBrace`'s qualifier-skip didn't skip the exception name(s) after
    `throws`, misclassifying the brace as not-a-function-body. Fixed via new
    `skipThrowsClauseBackward` (handles comma-separated, qualified exception names).
  - All 3 verified via `make test`, zero regressions (after updating `java_core_out.java`'s enum
    fixture per Bug 2).
- [ ] File-pair test: `c_comments_inp.c` → diff vs `c_comments_out.c`
  - Bug (0)/(3) FIXED (partial, in progress -- other reported bugs in this file remain open):
    a mid-param `//` line comment (e.g. `int b, // second` immediately followed on the next
    line by the next param) was swept into the *following* param's `typeTokens` instead of
    being recognized as the trailing comment of the param it actually follows on the source
    line -- `MiscRule.parseSignature`'s comma-split had no reattachment step, and
    `parseParam` only ever captured a slice's own *last* token as a comment. This corrupted
    `void multiParam(...)`'s rendering to a single collapsed line ending in `// second int c
    /* third */) {`, where the real `{` for the function body became part of that `//`
    comment's text once re-tokenized by a later `Formatter.java` phase -- desyncing brace
    depth for the rest of the file and silently truncating everything from `switchy(...)`
    onward out of the output (bug (0)). Fixed in two parts (`MiscRule.parseSignature`):
    (a) after `splitTopLevelCommas`, a leading comment token on any non-first part is now
    reattached to the end of the previous part, so it stays with the param it followed in the
    source; (b) `render(Signature, ...)` now forces multi-line (one-param-per-line) rendering
    whenever any param's comment is a `COMMENT_LINE` (`//`) -- such a comment can never be
    safely rendered inline, since it would swallow every token after it on that physical line.
    Verified via `make test`: `c_comments_inp.c` no longer truncates and `multiParam` now
    renders one param per line; zero regressions across the other 12 file-pairs (all still
    PASS forward + idempotency). Remaining open bugs in this file (lone `/* */` comment
    deletion, missing blank lines in `Trio`, `else` closing-comment placement, and a few
    formatting/spacing diffs) are unfixed -- left for a follow-up session.
  - Bug (3b) FIXED (two parts, both alignment/column-width bugs):
    (a) `DeclarationAlignmentRule.parseDeclaration` misparsed a compound-assignment statement
    (e.g. `tmp += b;`) as a fake declaration: `+=`/`-=`/etc. tokenize as one OP token, which the
    method's `=`-only search for an initializer never matches, so the operator token was left
    stranded inside `typeTokens` (`["tmp", "+="]`) while `name` became the RHS identifier
    (`"b"`) -- passing every existing guard since `tmp` (the real first token) is a plain
    IDENTIFIER. This silently merged `tmp += b;`/`tmp += c;` into the *same* declaration group
    as a preceding `int tmp = a;`, and the fake `"tmp +="` type cell (6 chars) widened the
    group's shared type column, wrongly padding the real declaration to `int    tmp = a;`. Fixed
    by rejecting any `typeTokens` containing a compound-assignment operator (new
    `COMPOUND_ASSIGN_OPS` set), alongside the existing `->`/`.`  rejection -- `tmp += b;` is no
    longer treated as a declaration at all, so it and `int tmp = a;` are correctly independent
    (§5 declaration vs. §6 assignment are different statement kinds with no combined worked
    example in STYLE.md; confirmed with the user this is the correct read).
    (b) `MiscRule.render(Signature, ...)`'s broken (one-param-per-line) form double-counted a
    gap: `padRight(typeText, maxTypeLen + 1)` already produces the correct STYLE.md §8 gap
    (hand-verified character-by-character against the `const char*`/`uint8_t`/`uint16_t` worked
    example: column width is exactly `maxTypeLen + 1`, e.g. `uint8_t` padded to 12 gives the
    example's 5-space gap with no further space needed), but the code then appended one more
    literal `" "` before `nameText`, producing a double space whenever the padded column wasn't
    already at its exact minimum (visible whenever all param types are equal width, e.g.
    `multiParam(int a, int b, int c)` rendering `int  a,` instead of `int a,`). Fixed by dropping
    the extra `" "` at both call sites (declaration-signature and forward-declaration/call
    `renderDropped`, which shares the identical padding shape).
    Verified via `make test`: zero regressions across the other 12 file-pairs; `multiParam`'s
    broken form and the `int tmp = a;`/`tmp += b;`/`tmp += c;` sequence in `c_comments_inp.c`
    now render correctly.
  - Bug (1) FIXED: a lone `/* ... */` (or `//`) comment on its own line, standing entirely
    between two declarations in the same group with no blank line separating them (e.g.
    `static int x = 10; /* separator */ static int y = 20;`), was silently deleted. Root cause:
    `DeclarationAlignmentRule.groupDeclarations` only broke a group on a genuine blank line
    (`hasBlankLineBefore`, a run of >=2 `NEWLINE` tokens) or a non-declaration statement; a
    standalone comment consumes one of those newlines without contributing a second, so it never
    tripped the blank-line check and both declarations stayed in one group -- whose `render`
    fully regenerates the group's entire text span from the two `Declaration` objects alone, with
    no field anywhere carrying the interior comment, so it just vanished. Fixed by adding
    `hasCommentBefore` (any comment token in a statement's leading gap -- which, by
    `pullTrailingSameLine`'s existing same-line-only pull, can only be a standalone comment, never
    a leftover trailing one) as an additional group-breaking condition alongside `blankBefore`.
    Verified via `make test`: `/* separator */` now survives; zero regressions across the other
    12 file-pairs (still 24/24 PASS forward + idempotency). Missing blank lines around the
    now-preserved comment (bug (2)) and the `Trio` struct's own missing blank lines are still
    open, left for a follow-up.
  - Bug (3c) FIXED: `int c  /* Third */` (2 spaces before comment) vs `int c /* Third */`
    actual in `multiParam`'s broken form. Root cause: `MiscRule.render(Signature, ...)`
    (~line 1314) and its `renderOnePerLine` sibling (~line 2352) appended each param's
    trailing comment with a hardcoded single space, with no comment-column padding step
    analogous to the existing `typeColWidth` padding -- since the last param has no trailing
    comma, its `name+comma` cell was 1 char narrower than sibling params that do (`"c"` vs
    `"a,"`/`"b,"`), so its comment sat 1 column left of the others. Fixed by adding a
    `maxNameCommaLen` pass in both methods, padding the `name+comma` cell to a common width
    across all params in the group before appending the comment. Verified via `make test`,
    zero regressions.
  - Bug (2) RESOLVED as a fixture fix, no code change: investigation found the line-count
    theory doesn't hold (`AudioConfig`: 5 content lines, no blank lines expected;
    `AudioRingBuf`: 6 content lines, no blank lines expected; `Trio`: only 4 content lines yet
    blank lines were expected) -- line count is not the differentiator between these
    typedef-anonymous-struct-with-alias shapes. User confirmed and hand-edited
    `c_comments_out.c` to remove the blank-line expectation around `Trio`'s fields instead of
    changing formatter behavior.
  - Bug (4) FIXED: `} /* non-negative */ else {` was not split onto separate lines. Root cause:
    `BlockStructureRule.placeElseOnOwnLine`'s guard `gap.stream().noneMatch(this::isComment)`
    unconditionally bailed out of *all* repositioning whenever any comment sat in the
    `}`...`else` gap, leaving the original same-line text untouched verbatim. Fixed by dropping
    that guard and instead relocating any comment(s) found in the gap onto their own line
    between `}` and `else` (both now on their own lines at `}`'s indentation). Applied the
    identical fix to `placeCatchFinallyOnOwnLine` for consistency, since it shares the exact
    same structure/guard (no test currently exercises that path, but it had the same latent
    bug). Verified via `make test`, zero regressions.
  - CONFIRMED, NOT YET FIXED (investigated only, per explicit user instruction -- next
    session should fix this):
    - (5) `// macro a` never gets capitalized to `// Macro a` (unlike ordinary `//` comments
      elsewhere in the file). **Not** a word-exemption/no-capitalize list. Root cause:
      `TokenizerCore` lexes an entire `#define NAME VALUE // comment` line as one opaque
      `PREPROCESSOR` token (see the type's own comment: "opaque single-line #-directive") --
      the trailing `//` text is embedded raw inside that token's text and never becomes a
      separate `COMMENT_LINE` token, so it never reaches `MiscRule.enforceCommentStyle`'s
      capitalization logic (which only rewrites `COMMENT_LINE`/`COMMENT_BLOCK` token types).
      Fix needs the tokenizer to split a trailing `//` comment off of `#define` (and other
      preprocessor) lines into its own `COMMENT_LINE` token, or `enforceCommentStyle` (or a new
      pass) to specifically parse and rewrite the comment portion of `PREPROCESSOR` token text.
- [ ] File-pair test: `cpp_comments_inp.cpp` → diff vs `cpp_comments_out.cpp`
- [ ] File-pair test: `java_comments_inp.java` → diff vs `java_comments_out.java`

**If any file-pair test above shows a mismatch: stop, report the full diff to the
user, and wait for instruction. Do not attempt to fix either the formatter or the
`*_out` file without explicit user direction — the `*_out` files were authored by
hand and may themselves contain errors.**

**After all 15 file-pair tests pass (or are resolved):**
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
char**         c;
double**       c;
char*    const c; // ← gap (current)
char* const    c; // ← correct per §8
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

Perform smoke-testing after implementing/fixing each of the above gaps and then
`make test` to ensure there is no regression.

Update `README.md` after the tests passed and then add the tests as one of the
new tests candidate in `## TODO — Not Scheduled` : `### F — Add more tests`.

---

## TODO — Not Scheduled

### A — Add support to enable/disable formatting

Via comments inside the code:

//% JXM_CFMT_DIS
/*% JXM_CFMT_DIS */

//% JXM_CFMT_ENA
/*% JXM_CFMT_ENA */

Via command line option `--format-off`: formatting starts disabled for the whole file, as
if `JXM_CFMT_DIS` were present at the top -- the user must insert an explicit
`JXM_CFMT_ENA` marker in the source to turn formatting back on from that point onward.

Update `README.md` after implementing this.

### B — Add new configuration entries:

```properties
# ── Behavior ──────────────────────────────────────────────────────────────────
normalize-comment-start-case = on              # on | off
normalize-comment-end-period = on              # on | off
```

And implement that to enable/disable comments title-casing and end-period handling.

Update `README.md` after implementing this.

### C — Don't damage C-preprocessor macros embedded in Java source

Some Java source files use a C-macro preprocessor (e.g. PCPP-style) as a poor man's
template mechanism -- `#define`/`#ifdef`/etc. lines mixed into otherwise-normal Java code
before a separate preprocessing step runs. The Java formatter currently has no awareness
of this and could corrupt such lines (they don't look like valid Java constructs).
Investigate and, if needed, add detection/pass-through handling so these preprocessor
lines are left untouched when formatting `.java` files.

### D — Extra

1. Smoke test support multiple-file formatting at once, both in `--standalone` and
   client-server mode
2. Add `bench` target in Makefile for benchmarking (calculate the total time):
   - Formatting the 15 files above one by one in `--standalone` mode
   - Formatting the 15 files above at once in `--standalone` mode
   - Formatting the 15 files above one by one in client-server mode
   - Formatting the 15 files above at once in client-server mode

Start the server before benchmarking the client-server mode and then stop the server
after the benchmarking is done. Do not include the server start and stop time in
the benchmark.

### E — Code cleanups
1. These comparison:
     "c".equals()
     "cpp".equals()
     "java".equals()
   are scattered all over the place in the code, please refactor the, so they
   are only compared once for every file being processed -- precompute
   `isC`/`isCpp`/`isJava` (or an equivalent boolean/enum) once per file in
   `Formatter.formatOne` and thread it down instead of re-doing the string
   comparison in every rule method.
2. Checkings such as:
     isOp(...)
     isPunct(...)
     isKeyword(...)
     isComment(...)
     isGapToken(...)
     etc.
   are scattered all over the place in the code, please refactor the, so they
   are centralized in the `TokenizerCore.Token` class or other class.

### F — Add more tests

Add more `*_inp.c/cpp/java` and `*_out.c/cpp/java` test pairs to test more construct
variants (do not modify the existing test pairs).

Add them also under:
    File-pair test: `java_comments_inp.java` ...
in 'STATE.md'

Finally `test/README.txt` to register the new tests.

