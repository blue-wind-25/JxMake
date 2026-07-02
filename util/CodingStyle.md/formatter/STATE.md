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
  - Bug 1 FIXED: tokenizer `namedConstructKeywordSeen` flag lets named-construct detection
    skip attribute-specifiers like `alignas(16)` between keyword and name;
    `BlockStructureRule.findConstructKeywordIndex` scans past them for the closing comment
    label; `classifyNamed` record-path guarded to Java-only.
  - Bug 2 FIXED: `virtual`/`inline`/`explicit` added to `CppModifierPriority` (were
    silently rejected by `typeKeywords` check); `= 0`/`= delete`/`= default` suffixes
    now recognised as func-decl specifiers so function-parameter stripping fires and
    the extra spaces are collapsed.
  - Bug 3 FIXED: `MiscRule.render(Signature)` unconditionally appended a space between the
    rendered lead tokens and the function name; when `leadTokens` ends with `::` (qualified
    name like `Processor::setGain`) the space landed after `::` instead of before `(`.
    Fix: check `needsSpaceBetween(lastLeadToken, sig.name)` and suppress the space when
    the last lead token is `::`, `.`, or `->`.
  - Bug 4 FIXED: `CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` used
    `prevSignificantIndex` before `{` to find the close-paren, which returned the `)` of
    the last initializer-list entry (e.g. `active_(false)`) rather than the constructor's
    own `)`. The Allman `{` was then indented to the initializer-list line (4 spaces) instead
    of the constructor's own line (0 spaces). Fix: new `resolveToFunctionCloseParen` helper
    scans backward past balanced parens looking for a depth-0 `:` (initializer-list colon);
    if found, the `)` just before it is the function's true close-paren, whose line's indent
    is used for the Allman `{`. Idempotency check moved to use the immediate preceding token
    (the last initializer `)`) rather than the now-distant function `)`.
  - Bug 4b FIXED: two pre-existing rendering issues (not caused by Bug 3/4 commits)
    - `(int   ch  )` rendered as `(int ch    )`: `GetterSetterRule.render` (definitions path,
      `isDef = true`) builds the `callGrid` with each member's entire param string as a single
      cell (`cellText(tokens, m.paramsFrom, m.paramsTo)` verbatim, e.g. `"int ch"`). The grid
      pads it to the max params-column width (here 10, from `"float gain"`), pushing all surplus
      spaces to the right (`"int ch    "`). Fix: for each param position, extract the type tokens
      and name token separately, compute `maxTypeWidth` and `maxNameWidth` across the group, and
      pad type and name individually before joining with a single space — so `"int"` is padded to
      5 (= `"float"`) and `"ch"` is padded to 4 (= `"gain"`), giving `"int   ch  "`.
    - `float* ch = buf.data + i * buf.frames` rendered as `i* buf.frames` (space before `*`
      lost): `MiscRule.renderTokens` treats `*` as a tight token via `isTightToken`, suppressing
      the preceding space regardless of whether `*` is a pointer declarator or a binary multiply.
      Some formatter pass calls `renderTokens` on a token range that includes `i * buf.frames`,
      causing the space drop. Root cause: `isTightToken(*)` is context-blind; fix requires either
      distinguishing pointer-`*` from multiply-`*` by context (preceding token is an IDENTIFIER
      or `)` → binary; preceding token is a type keyword/identifier with no intervening name →
      pointer), or passing the raw source whitespace through for binary operators.
  - Bug 5 FIXED: trailing-return-type function not detected as function definition
  - Bug 6 FIXED: `if`/`else`/`else if` chains collapsed to one-liner
- [x] File-pair test: `java_core_inp.java` → diff vs `java_core_out.java` (PASS)
  - Bug A FIXED: `public   class CoreExample` spaces not normalized — `enforceNamedConstructHeaderSpacing`
    `headerStart` now extends backward past modifier keywords (`public`, `abstract`, etc.) so
    the collapse range includes them, not just the `class`/`interface`/`enum` keyword itself.
  - Bug B FIXED: `this.count = count;` not aligned — `MiscRule.parseAssignment` required the
    first LHS token to be `IDENTIFIER`; `this`/`super` are `KEYWORD`, so were rejected. Fix:
    accept both `IDENTIFIER` and `KEYWORD` as the first LHS token.
  - Bug C FIXED: wrong `// end CoreExample` closing comments not replaced/removed —
    `addClosingComments` now replaces an existing `COMMENT_LINE` after `}` with the correct
    label (for named constructs), or removes it (if it looks like a wrong closing comment and
    no comment is wanted there). New helpers: `findExistingLineComment`, `isLikelyClosingComment`,
    `normalizeWhitespaceBefore`, `clearWhitespaceBefore`.
  - Bug D FIXED: method with `throws` clause (`void process(...) throws IOException {`) not
    converted to Allman brace style, and extra spaces between tokens not normalized. Two fixes:
    (1) `ScopePipeline.applySignaturePass` extended to detect and normalize `throws` clauses —
    scans backward through the exception list to find the true `)`, renders the signature
    normalized, then appends a normalized `throws ExceptionType` suffix to the replacement;
    (2) `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle` extended with
    `findCloseParenBeforeThrows` helper — detects the throws-clause pattern and applies the
    same `gapToBrace` Allman conversion as for bare-paren method definitions.
  - Bug E FIXED: inline switch — fall-through case (`case 3`) got only 1 space before `:`
    instead of 2 (misaligned with other labels). Root cause: `SwitchRule.applyInlineAlignment`
    added a 1-cell row `[label]` for fall-through cases; ColumnGrid's ragged-row rule never
    pads the last cell in a row, so the 1-cell row's label was unpadded. Fix: add an empty
    sentinel second cell `[label, ""]` so the label is in a non-last position and gets padded.
  - Bug F FIXED: inline switch — `break`-only case (`case 5`) was rendered as
    `case 5  :                       break;` (break after the content-column padding). Root
    cause: `classify` returned `hasContent=false, hasBreak=true`, which caused the terminator
    cell to be `"break;"` placed AFTER the content-column padding. Fix: treat `break` as plain
    content (`hasContent=true, plain="break", hasBreak=false`) so it lands in the content
    column, with `;` as the terminator column. Output: `case 5  : break                 ;`.
  - Bug G FIXED: `catch` and `finally` joined behind `}` (e.g. `} catch (...)`) instead of
    on their own line. Root cause: no pass existed to separate them (only `placeElseOnOwnLine`
    existed for `else`). Fix: added `BlockStructureRule.placeCatchFinallyOnOwnLine` (same
    algorithm as `placeElseOnOwnLine`) and wired it in `Formatter.java` right after
    `placeElseOnOwnLine`. Also added `"catch"` to `MiscRule.TIGHT_PAREN_KEYWORDS` so
    `catch (...)` is tightened to `catch(...)` matching the style guide.
  - Bug H FIXED: `@Override` annotation absorbed into method signature → rendered as
    `@ Override public void run()` on one line. Root cause: `ScopePipeline.applySignaturePass`
    treated `@` (OP token) as the first lead token, so `MiscRule.render` joined it with
    `Override` and the method modifiers with spaces. Fix: `skipAnnotations` helper in
    `ScopePipeline` scans past `@Identifier` / `@Identifier(args)` blocks before calling
    `parseSignature`, so annotations remain verbatim in `leadingGap` on their own line.
- [x] File-pair test: `cpp_modern_inp.cpp` → diff vs `cpp_modern_out.cpp` (PASS)
  - Bug 1 FIXED: `MiscRule.capitalizeFirstLetter` now extracts the first word and skips
    capitalization when it matches any C/C++/Java keyword in new `COMMENT_NO_CAPITALIZE` set.
  - Bug 2 FIXED: `ScopePipeline.processScope` pre-expands named-construct one-liner bodies
    (`struct Foo { int a; int b; };`) to multi-line before recursing, using `findParentIndent`
    to compute the correct member indent; `normalizeLeadingGap` no longer adds `\n` for
    inline (no-newline) gaps — which previously broke setter/getter one-liner bodies.
    `ScopePipeline.normalizeIndent` still normalizes non-multiple-of-4 indent widths (struct
    Triple 2→4 spaces).
  - Bug 3 FIXED: `DeclarationAlignmentRule.parseDeclaration` now rejects statements where
    `typeTokens` ends with `::` — `T::version;` was misread as declaration (type=`T::`,
    name=`version`) and rendered with a column-grid space between them.
  - Bug 4 FIXED (two parts):
    - (4a) `ScopePipeline.applySignaturePass`: for C/C++, `sigLeadStart` now starts from the
      first significant token on the same physical line as the function name (by scanning
      backward from the name for the last NEWLINE within the span), rather than from `leadStart`
      (the span's very first significant token). This prevents a `template<...>` header on a
      prior line from being pulled into `parseSignature`, where it was collapsed with the return
      type and name onto one line (and padded as a nested angle-bracket pair).
    - (4b) `CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`: new
      `findCloseParenBeforeRequiresClause` helper scans backward past the requires-clause
      expression to find the function's own `)`, enabling Allman-brace conversion for
      `f() requires Clause {`. `enforceRequiresClausePlacement` no longer swallows the
      whitespace/newline before `{`/`;`: the replaced span now ends just past the last non-gap
      clause token, so an Allman `\n{` placed by the earlier pass is preserved verbatim.
  - Bug 5 FIXED: `CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` no longer
    Allman-converts a function whose `{ ... }` body sits on a single physical line — such
    one-liners are always kept K&R (the `OneLinerCandidate` adjacency-grouping logic and all
    related dead code removed).
  - Bug 6 FIXED: `DeclarationAlignmentRule.render` was calling `reorderStatics` for all
    languages including C/C++. Reordering C/C++ declarations can alter semantics (initialization
    order, `constinit` runtime-init guarantees). Fix: only call `reorderStatics` for Java.
    `c_core_out.c` "Mixed static and non-static" section updated to reflect the preserved
    original order.
  - Bug 7 FIXED: `CppSpecificRule.isCandidateSignatureName` only accepted IDENTIFIER tokens
    before `(`, so `operator<=>` (and other operator overloads) were never treated as function
    definition candidates and their multi-line bodies were not Allman-converted. Fix: when the
    token before `(` is an OP token, check that the token before it is the `operator` keyword.
  - Bug 8 FIXED (`GetterSetterRule` — promise_type group, `GetterSetterRule.java`):
    Two fixes applied:
    - Empty-body guard: `bodyFrom < bodyTo &&` added before `isSingleStatementBody` call so
      methods with `{}` empty bodies (e.g. `return_void`, `unhandled_exception`) are accepted.
    - Multi-statement body guard REMOVED: the `!isSingleStatementBody(...)` check was removed
      entirely. `yield_value`'s body `{ value = v; return {}; }` has 2 semicolons and was
      previously rejected, splitting the 6-method group into two (3+2). Without this check,
      `hasNewlineBetween` already ensures the member is on one line. All 6 promise_type methods
      now form one group correctly.
    - `OUTLIER_RATIO` changed from 2 to 3 (earlier fix, see prior session notes).
    - The promise_type section no longer appears in the diff.
  - Bug 9 FIXED (two parts):
    - (9a) Extra space before `{` in brace-initializer, plus a double-semicolon `;;` on
      structured-binding declarations (`auto [x, y, z] = Triple{...};;`). Root cause 1:
      `DeclarationAlignmentRule.needsSpaceBetween` inserted a space before `{` whenever the
      previous token was an identifier; fixed by returning `false` when `cur` is `{` and `prev`
      is an `IDENTIFIER`. Root cause 2 (the `;;`): `ScopePipeline.splitTopLevelSpans` closed a
      `Span` on every depth-0 `}`, without the brace-initializer-vs-scope-body disambiguation
      that `DeclarationAlignmentRule.splitStatements` already had — so a brace-initializer's `}`
      (e.g. `Pair{1, 2}` in `auto [a, b] = Pair{1, 2};`) ended the span early, and the `;` that
      belongs to the declaration was treated as its own trailing/second span, which the splice
      logic then also terminated with `;`. Fix: added `isScopeOpeningBrace` — only consulted
      when a depth-0 `}` is immediately followed by `;` (the genuinely ambiguous case; function/
      control-flow/lambda bodies are never followed by `;`) — which scans every token between
      the span start and the `{` for a named-construct keyword (`class`, `struct`, `enum`,
      `namespace`, `concept`, `interface`, `record`, via new `isNamedConstructStartKeyword`,
      ported from `BlockStructureRule`). Scanning the whole range (not just the token
      immediately before `{`) is required so an intervening base-class list (`: public Base`)
      or `extern "C"` doesn't defeat the match. Verified via `make test` with zero regression
      across `h_core`, `c_core`, `hpp_core`, `cpp_core`, `java_core` after two earlier, more
      fragile attempts (narrow immediately-preceding-token checks) broke those five tests.
    - (9b) Missing space after `,` in nested brace-initializer lists not parsed as a
      `Declaration` (e.g. `std::vector<Pair> pairs = {{1,2},{3,4}};` — rejected by
      `DeclarationAlignmentRule.parseDeclaration`'s deliberate guard against `}`-ending
      initializers). Fixed in `MiscRule.enforceInitializerBraceSpacing`: added comma-spacing
      logic (`beforeComma`/`afterComma`, gated on any active initializer frame via `initStack`)
      alongside the existing brace-padding logic. Brace padding itself (STYLE.md §3.3's
      "outermost pair only" rule for nested initializers) required a second stack,
      `outermostStack`, parallel to `initStack` — an initial attempt gated padding on
      `initStack.size() == 1`, but that undercounts whenever the initializer is nested inside an
      enclosing scope brace (e.g. a function body), which also occupies a stack slot; a frame is
      "outermost" only if it was opened directly by `=` (tracked per-frame in `outermostStack`),
      not by raw stack depth. Verified via `make test`: `pairs` line now renders as
      `{ {1, 2}, {3, 4} };` matching `cpp_modern_out.cpp`, zero regressions.
  - Bug 10 FIXED: `static_cast<char*>(...)`/`reinterpret_cast<int*>(...)` rendered with spaced
    angle brackets (`static_cast < char* > (...)`) instead of tight. Root cause:
    `TokenizerCore.reclassifyAngleBrackets` only armed the `<` disambiguation stack when the
    token before `<` was an `IDENTIFIER`; `static_cast`/`dynamic_cast`/`reinterpret_cast`/
    `const_cast` are tokenized as `KEYWORD`, so the `<` after them was never reclassified to
    `ANGLE_BRACKET_OPEN`/`_CLOSE` and was instead treated as a comparison operator elsewhere,
    which spaces it. Fix: new `CAST_KEYWORDS` set + `isCastKeyword` helper; the arming check now
    accepts `IDENTIFIER` or a cast keyword before `<`.
  - Bug 11 FIXED: `namespace alpha::beta::gamma { ... }` closing comment dropped `beta`/`gamma`,
    rendering `// namespace alpha` instead of `// namespace alpha beta gamma`. Root cause:
    `TokenizerCore`'s `pendingNamedConstructName` armed on the first `IDENTIFIER` after
    `namespace` (`alpha`) but never extended across the following `::beta::gamma` segments. Fix:
    tokenizer now appends `::segment` for each further `IDENTIFIER` immediately preceded by `::`
    while `pendingNamedConstructName` is armed, giving `"alpha::beta::gamma"`.
    `BlockStructureRule.classifyNamed` special-cases a `name` containing `:` — looks up only the
    first segment via `findConstructNameIndex` to confirm the `namespace` keyword, then renders
    the closing-comment label as `"namespace " + name.replace("::", " ")` (STYLE.md-preferred
    space separator, matching `cpp_modern_out.cpp`).
  - `cpp_modern_inp.cpp` now PASSES in full (forward + idempotency).
  - CRITICAL regression FIXED: idempotency pass on `c_core_out.c`'s "Mixed static and non-static"
    section (Bug 6's own worked example) was re-indenting on a second format pass —
    `static int beta;` / `       int alpha;` gained 8 extra leading spaces each time the already-
    formatted output was re-run through the formatter. Root cause: `ScopePipeline.
    applyDeclarationsPass` derived a declaration group's continuation-line indent by scanning the
    raw whitespace immediately preceding the group's first token — but when the group's first
    declaration has no modifiers while a sibling does (`alpha`/`gamma` have none, `beta`/`delta`
    are `static`), `DeclarationAlignmentRule.render`'s `ColumnGrid` already left-pads that first
    row's own rendered line with blank space matching the widest modifier column (e.g. `"       "`
    matching `"static "`). On a fresh format that padding doesn't exist yet in the raw source, so
    scanning finds 0 extra spaces; but on a *second* pass, that already-rendered padding IS the
    literal text preceding the token, indistinguishable from real code indentation by character
    inspection alone — so it got treated as (malformed, non-multiple-of-4) indentation, rounded up
    to 8, and then prepended a second time in front of `render()`'s own freshly-recomputed padding,
    doubling it. Fix: compute `lines = declarationRule.render(group)` first, measure the leading-
    space count already present in `lines.get(0)`, and strip up to that many trailing spaces off
    the raw leading gap before deriving `rawIndent`/`indent` from what remains (new
    `leadingSpaceCount`/`stripTrailingSpaces` helpers in `ScopePipeline`) — on a first-time format
    there's nothing to strip (no-op); on a re-format it exactly cancels out the self-generated
    padding, restoring idempotency. Verified via `make test`: `c_core_out.c` idempotency now
    passes, zero regressions elsewhere.
- [x] File-pair test: `java_modern_inp.java` → diff vs `java_modern_out.java` (PASS)
  - Bug 1 FIXED: an empty named-construct body (`record Num(int value) implements Expr {}`,
    likewise empty `class`/`interface`/`enum`, and C/C++ `struct`/`class`/`enum`/`enum class`)
    was being expanded to multi-line with a closing comment even though it has no content.
    Root cause 1: `BlockStructureRule.insertNamedConstructBlankLines` unconditionally inserted a
    blank line after every named-construct `{` and before its matching `}`, with no check for an
    empty body. Root cause 2: `BlockStructureRule.decideComment`'s `NAMED` case always returned
    `f.label`, unconditionally adding a closing comment. Fix: new `isEmptyBraceBody` helper (true
    iff the `{` is immediately followed, ignoring gap tokens, by its own matching `}`); consulted
    in both places to keep an empty body collapsed and comment-free.
  - Bug 2 FIXED: consecutive plain statements in a non-class scope whose last token before `;` is
    `identifier(...)` (e.g. `var trimmed = item.trim();` / `result.add(trimmed);` inside a
    `for`-loop body) were misidentified as one-liner method declarations and column-aligned as a
    getter/setter group, corrupting them (`item. trim()`, `result.             add(trimmed)`).
    Root cause: `GetterSetterRule.parseOneLinerMember` computed a "return type" span
    (`returnTypeFrom`..`nameFrom`) without checking that the span actually looks like a type --
    for `item.trim()` this span was `"item."` (a member-access receiver, not a type) and for
    `var trimmed = item.trim()` it was `"var trimmed = item."` (an entire assignment). Fix:
    reject the candidate when the return-type span contains a `.` or `=` token -- neither is
    ever valid in an actual return-type/qualified-name-before-`::` position.
  - Bug 3 FIXED: a one-liner constructor/method body containing 2+ statements on its single
    source line (e.g. `public Rectangle(double width, double height) { this.width = width;
    this.height = height; }`) got its statements split onto separate, wrongly column-aligned
    lines. Root cause: `ScopePipeline.processScope` recurses into every child `{...}` scope and
    always re-runs the full §5/§6 declaration/assignment grouping passes on it, even when the
    child is a non-named one-liner body that was deliberately left un-pre-expanded (still a
    single physical line) specifically so later one-liner-aware passes could handle it -- those
    grouping passes assume a real multi-line body and (mis)treat the 2 statements as a multi-line
    alignment group. Fix: when recursing into a non-named child scope whose source has no `\n`,
    skip the recursive `processScope` call entirely and splice the child source back unchanged.
  - Bug 4 FIXED: a standalone one-liner method (single-statement body, not textually adjacent to
    another one-liner) was Allman-broken (`{` moved to its own line) instead of staying K&R, e.g.
    `public double distance() { return Math.sqrt(x * x + y * y); }`. Root cause: RDD_KEY_75's
    "adjacency heuristic" in `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle` only kept
    a one-liner K&R when grouped with a neighboring one-liner, Allman-breaking every ungrouped
    one-liner -- contradicted by evidence in `java_modern_out.java`/`combined_out.java`, where
    isolated one-liners (`distance()`, `hasError()`, `isActive()`, each the only one-liner in its
    enclosing scope) stay K&R. Fix: removed the grouped/ungrouped split entirely -- every
    one-liner candidate now stays K&R unconditionally; deleted the now-dead
    `findPrevSiblingBoundary`/`breaksOneLinerRun` helpers this required.
  - Bug 5 FIXED: no blank line was inserted between a named-construct's `{` and a leading
    same-line-comment-led member (e.g. `record NamedPoint(...) {` immediately followed by
    `// compact constructor` with no blank line first). Root cause:
    `BlockStructureRule.ensureBlankLine` treated *any* comment anywhere in the gap as reason to
    render the gap unchanged (blank-line insertion fully blocked), conflating two different
    shapes: a genuine trailing same-line comment glued to the previous token (correctly
    ambiguous to relocate) vs. a comment that already starts on its own new line (safe to push
    a blank line ahead of, same as any other token). Fix: only block insertion when there is no
    `NEWLINE` token before the first comment in the gap (glued case); otherwise insert the blank
    line ahead of the leading comment and leave the comment and everything after it untouched.
  - Also fixed as a side effect of Bug 4 (no separate root cause): the `permits` clause was
    already correctly line-wrapping once the Allman-brace pass stopped corrupting `distance()`'s
    surrounding structure earlier in the pipeline; no dedicated fix was needed once Bugs 1-5
    above were resolved -- confirmed via `make test`, `permits` wrapping matches
    `java_modern_out.java` with zero remaining diff.
  - All 5 bugs verified via `make test`: `java_modern_inp.java`/`java_modern_out.java` now PASS
    (forward + idempotency), zero regressions across the other 6 file-pairs.
- [~] File-pair test: `combined_inp.h` → diff vs `combined_out.h` -- 2 of 3 bugs remain
  - Bug 1 FIXED: `#define` value columns in a contiguous run of scalar macros (no blank line,
    comment, function-like macro, or valueless macro breaking the run) weren't realigned to a
    common column when `format-macros = on` (env var `JXMAKE_CODE_FORMATTER_FORMAT_MACROS=on`,
    already exported by the `test` Makefile target) -- no code path existed at all for this;
    `DeclarationAlignmentRule` never touches `#define` (requires a trailing `;`, which macros
    never have). New `CppSpecificRule.alignMacroDefinitions`, wired into `Formatter.java` Phase 4
    behind `config.isFormatMacros()`: groups consecutive scalar `#define NAME VALUE` lines into
    runs, pads each name to the run's longest name + 1 space, leaves the value (and any trailing
    same-line comment) untouched. Verified via `make test`: `combined_inp.h`'s macro-column diff
    is gone, zero regressions across the other file-pairs.
  - Bug 3 (`extern "C"` closing-comment) RESOLVED as a `combined_out.h` fixture fix (user-
    confirmed, no code change): a bare `extern "C" { ... }` (unconditional, e.g. `cpp_core`)
    gets a closing comment, but one wrapped in `#ifdef __cplusplus ... #endif` (as in
    `combined_inp.h` and `h_core_inp.h`, both of which agree) does not -- `combined_out.h`'s
    `// extern "C"` was a fixture error, now removed to match established precedent.
  - Bug 2 (enum closing-comment) PARTIALLY FIXED: `BlockStructureRule.commentInsertionIndex`
    only recognized `}` or `};` as the insertion point, so C's `typedef enum/struct NAME { ... }
    ALIAS;` shape (alias identifier between `}` and `;`) never got a closing comment -- fixed by
    skipping an optional single IDENTIFIER before the `;`. Verified via `make test` on
    `c_core_inp.c`'s `Point`/`Color` (`test/c_core_out.c` already carried the expected `// struct
    Point` / `// enum Color` from prior uncommitted work; now produced correctly), zero
    regressions.
  - Bug 2 still open for `combined_inp.h`'s `EngineState` specifically: found (not yet fixed) a
    second, broader bug in `TokenizerCore` -- `emitOpenBrace`/`emitCloseBrace`/
    `emitOpenBracket`/`emitCloseBracket` all gate `braceDepth`/`parenDepth` tracking *and*
    named-construct detection behind `preprocessorDepth == 0`, so being inside **any**
    `#if`/`#ifdef`/`#ifndef` region -- including an ordinary whole-file `#ifndef GUARD` header
    guard -- freezes depth tracking and disables naming for everything inside it. This is why
    `EngineState` (guarded) never gets `NAMED` classification, while the same shape works when
    the file uses `#pragma once` instead (`hpp_core`, `cpp_core`). Asked the user how to fix this
    (remove the guard entirely vs. special-case the header-guard shape vs. leave unfixed) --
    no response yet; **not fixed**, flagged here as blocking the rest of this bug.
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
Update `README.md` after implementing this.

---

## TODO — Not Scheduled

### Add support to enable/disable formatting

Via comments inside the code:

//% JXM_CFMT_DIS
/*% JXM_CFMT_DIS */

//% JXM_CFMT_ENA
/*% JXM_CFMT_ENA */

Via command line options to start with `JXM_CFMT_DIS` add option:
--format-off

Update `README.md` after implementing this.

### Add new configuration entries:

```properties
# ── Behavior ──────────────────────────────────────────────────────────────────
normalize-comment-start-case = on              # on | off
normalize-comment-end-period = on              # on | off
```

Update `README.md` after implementing this.

### Cleanups
1. These comparison:
     "c".equals()
     "cpp".equals()
     "java".equals()
   are scattered all over the place in the code, please refactor the, so they
   are only compared once for every file being processed.
2. Checkings such as:
     isOp(...)
     isPunct(...)
     isKeyword(...)
     isComment(...)
     etc.
   are scattered all over the place in the code, please refactor the, so they
   are centralized in the `TokenizerCore.Token` class.

### Extra

1. Smoke test support multiple-file formatting at once, both in `--standalone` and
   client-server mode
2. Add `bench` target in Makefile for benchmarking (calculate the total time):
   - Formatting the 15 files above one by one in `--standalone` mode
   - Formatting the 15 files above at once in `--standalone` mode
   - Formatting the 15 files above one by one in client-server mode
   - Formatting the 15 files above at once in client-server ` mode

Start the server before benchmarking the client-server mode and then stop the server
after the benchmarking is done. Do not include the server start and stop time in
the benchmark.
