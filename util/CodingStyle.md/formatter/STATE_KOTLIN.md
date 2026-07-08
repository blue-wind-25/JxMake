# STATE_KOTLIN.md — Kotlin JAR Implementation Tracker

**This file is self-contained. Do not assume `STATE.md` has been read in this
session.** If you have not read `STATE.md`, that is fine — every convention this
file depends on is restated below. This file is routed to from `CLAUDE.md`'s
job table (Kotlin JAR support → this file), and, since Kotlin implementation
work has now started, also from a redirect at the top of `STATE.md` itself
(see "Handoff Note" below for the history of that link).

---

## Purpose

Tracks implementation of Kotlin support in the deterministic JAR formatter
(`util/CodingStyle.md/formatter/`), per `STYLE_KOTLIN.md` / `STYLE_KOTLIN2.md`.
Kotlin currently has **no** JAR support — `AI_PREAMBLE_FULL.md`'s full-file AI
pass is the only existing workflow for Kotlin files (see `README.txt`). This
file tracks the work to close that gap.

---

## Hard Constraint — Shared Classes

The formatter's tokenizer and several rule classes are **shared across all
languages** (C, C++, Java, and now Kotlin) — they are not per-language files:

```
tokenizer/TokenizerCore.java
grid/ColumnGrid.java
grid/ModifierPriority.java
evaluator/ComplexityPaddingEvaluator.java
rules/DeclarationAlignmentRule.java
rules/BlockStructureRule.java
rules/SwitchRule.java
rules/GetterSetterRule.java
rules/MiscRule.java
ScopePipeline.java
Formatter.java
```

**Any change to one of these files for Kotlin's benefit must not change
behavior for C/C++/Java.** Before and after every such change, re-run the
formatter's full existing test suite (`make test` — all C/C++/Java fixtures
under `test/`) and confirm zero regressions. This is the same discipline
`STATE.md` already applies to its own commits; it is restated here because a
session working from this file alone must not skip it for lack of having read
`STATE.md`.

Kotlin-only work belongs in new files (see Project Layout below), added
alongside the existing per-language files (`JavaSpecificRule.java`,
`CppSpecificRule.java`) rather than folded into them.

**Before modifying a shared class, grep first — do not read `STATE.md` in
full.** Run `grep -Fm1 'ClassName' STATE_rdd_log.md` (substitute the class or
method you're about to touch) to surface any existing `RDD_KEY_n` decisions
that already explain its shape — e.g. why `TokenizerCore`'s multi-char
operator table is structured the way it is (RDD_KEY_69), or why a rule class
re-derives named-construct-ness from raw tokens instead of trusting one flag
(RDD_KEY_84/85). This is almost always sufficient. Only read `STATE.md`'s
Project Layout section specifically (never its Checklist or full history) if
the grep hits don't explain what you're looking at.

---

### During implementation
- Implement one checklist section at a time
- After completing a section (or when the cumulative diff across all changed files
  exceeds ~50 lines, whichever comes first), do a checkpoint commit:
  1. Update STATE_KOTLIN.md — check off completed items and update the active checklist.
  2. `git add util/CodingStyle.md/formatter/` (the entire formatter directory)
  3. `git reset util/CodingStyle.md/formatter/target/` (exclude build output)
  4. `git commit -m "<message>"` — short descriptive message, no strict format required,
     trailer ending with `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`
- Small related items within a section may be grouped into one commit if they
  are trivially connected — use judgment based on line count (~50 lines threshold)
- Never let implemented files and STATE_KOTLIN.md drift out of sync — STATE_KOTLIN.md must
  always reflect the true current state at every commit
- Never modify the files `util/CodingStyle.md/formatter/test/*_inp.*` unless they contain
  syntax errors (they are the test input files).
- Never modify the files `util/CodingStyle.md/formatter/test/*_out.*` unless explicitly
  asked (they are the reference output files that show the expected results).
- Ignore `XL.txt`, that is the user tracker file.
- Use `/tmp` for temporary smoke-test and mini-test files.
- NEVER perform filesystem-wide find; search first in `/tmp/claude-1000` or the project root.
  If still not found, ask me.
- Do not use static analysis as the primary method of bug diagnosis or regression checking.
  Prefer evidence over reasoning (using debug prints). Keep static analysis minimal—only
  enough to identify where to insert debug prints.

## Commit Workflow

Same discipline as `STATE.md`'s own (restated, not cross-referenced, per the
self-contained requirement above):

- Implement one checklist section at a time.
- Checkpoint commit after each section or when the cumulative diff exceeds
  ~50 lines, whichever comes first: update this file's checklist, then
  `git add`/commit the formatter directory (excluding `target/`).
- Trailer: `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`.
- **On any ambiguity:** stop, add the question to Open Questions below, mark
  the checklist item `[~]`, commit this file only, and wait for an answer.
  Once resolved: append the full decision to `STATE_rdd_log.md` (next
  `RDD_KEY_n`, continuing the shared sequence — do not restart numbering for
  Kotlin), add the key + topic to this file's own Resolved Design Decisions
  index below, then continue.
- **On any shared-class change:** re-run the full existing C/C++/Java test
  suite before committing, per the Hard Constraint above. Record the
  before/after test count in the commit message.

---

## Project Layout (new files only)

```
util/CodingStyle.md/formatter/
  src/
    com/jxmake/formatter/
      grid/
        KotlinModifierPriority.java
      rules/
        KotlinSpecificRule.java
  test/
    kt_combined_inp.kt / kt_combined_out.kt
    kt_comments_inp.kt / kt_comments_out.kt
```

Existing shared files listed under Hard Constraint above are modified
in-place, additively, when Kotlin needs a shared capability they don't yet
have (e.g. a new operator token) — they are not duplicated per-language.

---

## Resolved Design Decisions

Full text of each decision lives in `STATE_rdd_log.md` (shared with
`STATE.md` — continue its existing `RDD_KEY_n` numbering, do not restart).
Look up one key at a time via `grep -Fm1 'RDD_KEY_n' STATE_rdd_log.md`
(no `-A`, its lines are long).

| Key | Topic |
|---|---|
| RDD_KEY_91 | `STATE_KOTLIN.md` — self-contained tracker, not linked from `STATE.md` yet |
| RDD_KEY_92 | Shared-tokenizer approach — extend `TokenizerCore.java` in place, no separate Kotlin tokenizer |
| RDD_KEY_93 | Checklist ordering — tokenizer support first, then a `JavaSpecificRule`-style scoping pass, before any `KotlinSpecificRule.java` code |
| RDD_KEY_99 | Kotlin headless named-construct classification (`companion object {}`, anonymous `object [: Super] {}`, `init {}`) — §3.1/§3.4; also fixed a related tokenizer bug (`:` wrongly arming the supertype name as the construct name) |
| RDD_KEY_100 | Kotlin `when` no-space-before-`(` — §3.2; added `"when"` to `MiscRule.TIGHT_PAREN_KEYWORDS`, a pure no-op for C/C++/Java |
| RDD_KEY_101 | Kotlin `when` expression arrow alignment/closing comment/blank lines — §4; new `KotlinSpecificRule.formatWhenExpressions`, not a `JavaSpecificRule`/`BlockStructureRule` extension (keyword-less branches, non-all-or-nothing block-body alignment, forced blank lines) |
| RDD_KEY_102 | Kotlin null-safety operator spacing (`?.`/`!!` tight, `?:` spaced) — §5; new `KotlinSpecificRule.enforceNullSafetyOperatorSpacing`, a flat whole-file pass since no shared class does general expression-level operator re-spacing |
| RDD_KEY_103 | Kotlin variable/property declaration alignment — §6; new `KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule` (visibility-loosen-then-extend, superseding the earlier "independent parser" resolution), own statement splitter/parser/renderer for the name-before-type parts |
| RDD_KEY_104 | Kotlin constructor/function parameter list line-breaking and column alignment — §7/§7.1; new `KotlinSignatureRule extends MiscRule` (same visibility-loosen-then-extend pattern as RDD_KEY_103, user-directed), own `KotlinParam`/`KotlinSignature` model, parser, and `ColumnGrid`-based renderer for the name-before-type parts; also covers §7.2 (trailing comma preservation) |
| RDD_KEY_105 | Kotlin labeled jump / label declaration spacing (`return@label`, `label@`) — §11; new `KotlinSpecificRule.enforceLabeledJumpSpacing`, a flat whole-file pass with a small state machine, same shape as RDD_KEY_102 |
| RDD_KEY_106 | Kotlin generic `where` clause line-breaking and bound alignment — §14; new `KotlinSpecificRule.enforceWhereClausePlacement`, structurally mirrors `CppSpecificRule.enforceRequiresClausePlacement` (per-language file precedent, not a shared-class extension); new `KotlinSpecificRule(Lang, int, int)` indent-width-aware constructor |
| RDD_KEY_107 | Kotlin destructuring declarations — §12; new `DestructuringDecl`/`groupDestructuringDeclarations`/`parseDestructuringDeclaration`/`renderDestructuringGroup` in `KotlinDeclarationAlignmentRule.java` (not a new file — reuses that class's existing §6/RDD_KEY_103 infrastructure); single pre-rendered `lhsText` cell, no per-component type grid, since §12 has no type annotations to anchor one; own group stream, never merged with §6's |
| RDD_KEY_108 | Kotlin annotation use-site target `:` spacing — §16; new `KotlinSpecificRule.enforceAnnotationUseSiteTargetSpacing`, small state machine over a flat whole-file pass (same shape as §11/RDD_KEY_105); new `USE_SITE_TARGETS` set matched by token text (not `TokenType.KEYWORD`) since `delegate` is a soft keyword, not tokenizer-lexed; `@`-to-target spacing deliberately left unenforced (no textual backing, no codebase precedent for reformatting plain annotation spacing) |
| RDD_KEY_109 | Kotlin lambda-with-receiver nesting exemption + arrow spacing — §17/§17.1; **shared-class change** — `ComplexityPaddingEvaluator.isLoose` extended to skip a `.`-preceded/`->`-followed `(...)` span (a lambda-with-receiver's own invocation parens) rather than counting it as nesting, pure no-op for C/C++/Java (confirmed via harness, `make test` 32/32 before/after); new Kotlin-only `KotlinSpecificRule.enforceArrowSpacing` + `collectWhenBranchArrowIndices`, a flat whole-file single-space arrow pass that explicitly excludes `when`-branch arrows (owned by §4's column alignment) |
| RDD_KEY_110 | Kotlin `for` loops and ranges — §10; `in`/`until`/`downTo`/`step` reclassified (b)→(a), already inert w.r.t. `ComplexityPaddingEvaluator.isLoose` with zero code changes (verified via harness, not a keyword-set addition as originally guessed); new `KotlinSpecificRule.enforceRangeOperatorSpacing`, a simpler one-sided sibling of §5/RDD_KEY_102's state machine tightening `..`/`..<` on both sides (no spaced variant, unlike `?:`) |
| RDD_KEY_111 | Kotlin `enum class` with members blank-line "emphasis" around the mandatory `;` — §2; `insertNamedConstructBlankLines` (shared) confirmed to only handle the body-open/close blank lines, not the `;`-separator emphasis — new `KotlinSpecificRule.separateEnumConstantListTerminator` (+ helpers), structurally mirroring `JavaSpecificRule.separateEnumConstantListTerminator` (per-language precedent, same as §14/RDD_KEY_106) rather than reusing/relocating it, since that class isn't shared and its own helper names aren't present in any shared class either |
| RDD_KEY_112 | Kotlin expression-bodied functions — §9; new `KotlinSignatureRule.FunctionTail`/`parseFunctionTail`/`renderWithTail`, a three-tier inline/params-broken/wrap-`=` fallback delegating to §7's existing `render` for the middle tier — plus a **shared-class fix**, `MiscRule.isTightToken`'s `*`/`&` tight-token treatment gated off for Kotlin (was collapsing ordinary multiplication spacing, `x* x`, surfaced by this work's own harness reproducing the style doc's `x * x + y * y` worked example) |
| RDD_KEY_113 | Kotlin generic variance (`in`/`out`) — §13; **shared-class fix** — `TokenizerCore.GENERIC_SAFE_KEYWORDS` extended with `"in"`/`"out"` so `reclassifyAngleBrackets` recognizes `Box<out T>`/`Comparable<in T>` as generic `<`/`>` pairs rather than comparisons; pure no-op for C/C++/Java (neither keyword exists in their keyword sets); tokenizer-level fix, no rendering pass needed |
| RDD_KEY_114 | Kotlin function/secondary-constructor body Allman-brace conversion — §3/§3.3; new `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` (+ `isFunctionOrConstructorCloseParen`/`findSignatureCloseParenBeforeBrace`/`isAngleOpen`/`isAngleClose`/`skipAngleBracketsBackward`), structurally mirroring `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle`/`CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` but with a much more conservative candidate signal (backward-scan must land on `fun`/`constructor`, since Kotlin has no `new` keyword to rule out trailing-lambda calls the way Java/C++ rule out ordinary calls) — also handles `: ReturnType` sitting between `)` and `{`, and tolerates the tokenizer's non-reclassified plain-`OP` `<T>` after `fun` (both discovered only via harness) |
| RDD_KEY_115 | Kotlin semicolon stripping — §1; fixed a real bug in the pre-existing `stripOptionalSemicolons` (committed earlier, `b0e778f`, predating this session's RDD-log convention) — it only protected the enum-with-members mandatory `;`, silently stripping a deliberate same-line multi-statement `;` too (would have merged two statements into one invalid line); rewritten around a single positive-evidence `isTrailingSemicolon` rule (only strip a `;` that's the last significant thing on its physical line), reusing §2's `findEnumConstantListTerminators` for the enum exclusion; also fixed a stray-trailing-space gap the old version had |
| RDD_KEY_116 | Kotlin string template tokenizer risk — §19; **shared-class fix** — `TokenizerCore.emitString`'s naive scan-to-next-`"` misread a nested string inside a `${...}` interpolation (`"${foo("x")}"`) as three tokens instead of one, a genuine correctness risk (a later spacing pass could insert whitespace inside the literal's actual text); fixed with a Kotlin-only `skipKotlinString`/`skipKotlinInterpolationBlock`/`skipKotlinChar` path (depth-tracks `${...}`'s own `{`/`}` nesting, recurses for nested strings/chars, arbitrarily deep), gated behind `lang.isKotlin`, non-Kotlin scan untouched; surfaced triple-quoted raw strings as a related, explicitly out-of-scope gap (new row 19.1, not fixed — undocumented in either style doc) |
| RDD_KEY_117 | Kotlin triple-quoted raw string tokenizer support — row 19.1, **shared-class fix**; badly broken before this (`"""hello "world" end"""` mis-lexed as five tokens including a bare `IDENTIFIER`; multi-line raw strings leaked a spurious `NEWLINE` token into the content); fixed with Kotlin-only `isKotlinRawStringOpener`/`emitKotlinRawString`/`skipKotlinRawString` — no backslash-escape processing (literal `\` by design), greedy termination at the first `"""` (matches real Kotlin compiler semantics); `${...}` interpolation still recognized via `skipKotlinInterpolationBlock`, extended to also recognize a nested raw string inside an interpolation expression; non-Kotlin paths (Java text block, C++ raw string, plain C string) confirmed untouched |
| RDD_KEY_118 | Kotlin import-ordering implementation — §24 spec now implemented; new `KotlinSpecificRule.enforceKotlinImportOrdering` (+ `ParsedKotlinImport`/`parseKotlinImportStatement`/`appendRange`/`joinVerbatim`/`isPathOp`/`findLocalPackagePrefix`/`classifyKotlinImportGroup`/`matchesPrefix`), mirroring `JavaSpecificRule.enforceImportOrdering` but with no `static` bucket (priority local > kotlin > java/javax > org > com > other) and an import statement ending on optional `;` or NEWLINE/EOF rather than a required `;`; new `kotlin-import-order`/`-sort`/`-depth`/`-blank-lines` keys added to `Config.java` mirroring `java-import-*` exactly; verified via a standalone 10-case harness, not yet wired into `Formatter.formatOne` |

---

## Open Questions

- **First-statement double-indentation bug (found during Step 4, while
  fixing punch-list item 2/RDD_KEY_120).** A Kotlin `val`/`var` declaration
  that is the very first statement inside a function or class body gets
  double-indented (8 spaces instead of 4 at default `indent-size`), e.g.
  `fun sumAll(...) { var total = 0 ... }` renders `var total`'s line at 8
  spaces. Confirmed via bisection (`git checkout HEAD~1` /
  `git stash`) to predate both RDD_KEY_119 and RDD_KEY_120 entirely — a
  pre-existing bug, not a regression from this session's other fixes.
  Reproduces with a minimal two-line function body, no `for` loop or class
  involved. Root cause not yet fully isolated — traced as far as
  `ScopePipeline.addKotlinDeclReplacement`'s leading-gap-vs-fresh-render
  splice logic (shared with the `applyDeclarationsPass`/`processScope`
  family), which computes a `gapStart`/`rawLeadingGapFull`/`freshPad`
  interaction meant to avoid double-counting indentation between the raw
  source gap and `KotlinDeclarationAlignmentRule.renderPropertyGroup`'s own
  rendered line — but `renderPropertyGroup`'s output has no leading
  whitespace of its own (confirmed by reading it), so the doubling must come
  from `ScopePipeline`'s recursive per-scope child-fragment extraction
  (`processScope`) re-adding an indent level that the splice logic also
  adds; not yet confirmed with a debug-print harness. Out of scope for
  RDD_KEY_120 (a `BlockStructureRule.tryCollapse` fix, unrelated file/pass) —
  flagged here rather than guessed at.
- **Reversed declaration grammar (§6/§7, found during Step 1).**
  `DeclarationAlignmentRule.Declaration` (and `MiscRule`'s parameter/signature
  model) assume C/Java's `[modifiers] Type name [= init]` token order.
  Kotlin's actual grammar is `[modifiers] val/var name : Type [= init]` —
  name comes first, type is optional and trails after `:`. This affects both
  variable/property declarations (§6) and function parameter lists (§7),
  which the style doc expects to align into the same kind of column grid C/Java
  declarations do (name column, `:`/type column, `=` column).
  Two ways forward:
  1. Extend `DeclarationAlignmentRule`'s shared `Declaration` model to support
     a name-before-type grammar mode — touches an already-COMPLETE shared
     class's *behavior*, not just additive keyword recognition, so per the
     Hard Constraint this needs to stop and ask before doing it.
  2. Give `KotlinSpecificRule.java` its own independent declaration/parameter
     parser and renderer, reusing only lower-level shared primitives
     (`ColumnGrid`, `ModifierPriority`) rather than `Declaration` itself —
     no shared-class behavior change, more duplicated logic.
  **Resolved for both §6 (RDD_KEY_103) and §7 (RDD_KEY_104):** originally the
  user chose option 2 for §6 (independent parser in `KotlinSpecificRule.java`),
  but reconsidered before implementing either and picked a third approach for
  both sections instead — loosen the relevant shared class's visibility on its
  C/C++/Java-agnostic private helpers (additive, behavior-neutral), then
  extend it (`KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule`
  for §6; `KotlinSignatureRule extends MiscRule` for §7), each with its own
  name-before-type model/parser/`ColumnGrid` renderer. See the Step 3
  checklist and `RDD_KEY_103`/`RDD_KEY_104` for full implementation detail.
- **String template tokenizing (§19, found during Step 1).** Not yet verified
  whether `TokenizerCore.emitString()` correctly closes a Kotlin string when a
  `${...}` interpolation contains its own nested `"..."` (e.g.
  `"${foo("x")}"`). `emitString()` was written for C/Java strings, which never
  nest quotes. This is a tokenizer-correctness risk, not a style question —
  should be resolved with a debug-print/dump harness against real nested-quote
  input (per this file's evidence-over-reasoning rule) before trusting any
  Kotlin fixture that uses string templates. Not yet investigated in depth.
- **§8/§9 one-liner getter/setter grouping never actually fires for Kotlin
  (found post-Step-3.5, while auditing `AI_PREAMBLE_AESTHETIC.md`/
  `AI_PREAMBLE_FULL.md` for Kotlin accuracy).** §8's/§9's scoping-table rows
  (RDD_KEY_112 and the §8 row above it) assert Kotlin one-liner
  accessors/expression-bodied functions "participate in the same §14/STYLE.md
  getter/setter-style aligned group" as a free consequence of the shared
  `GetterSetterRule` — but this was never actually harness-verified for the
  *grouping* behavior itself, only for the "preserve as written" /
  no-wrong-collapse behavior of a single standalone one-liner. Live-tested
  just now (standalone JAR, `.jxmake-code-formatter` config, scratch `.kt`
  fixture) and confirmed **broken**: three adjacent `fun getX(): Int = 1`
  /`getY`/`getZ` one-liners get zero column alignment, where the equivalent
  Java (`int getX() { return 1; }` etc.) correctly aligns. Reproduced with and
  without an explicit `public` modifier — no difference. Root cause (read,
  not yet fixed): `GetterSetterRule.groupOneLiners`'s `isClassScope` gate is
  `lang.isJava || hasAccessSpecifier(...)` — `hasAccessSpecifier` looks for
  C++-style `public:`/`private:` labels, which don't exist in Kotlin, so
  `isClassScope` is always `false` for Kotlin. More fundamentally,
  `parseOneLinerMember`'s modifier-consuming loop is gated `if (lang.isJava)`
  only, and the rest of that method assumes C/Java's
  `[modifiers] ReturnType name(...)` token order to find the member's name —
  the exact same reversed-grammar problem already resolved for §6/§7 above
  (RDD_KEY_103/104), just not yet extended to this shared class. Not fixed in
  this session (documentation-only session) — same "loosen shared-class
  visibility, then extend with a Kotlin-aware subclass/method" pattern as
  RDD_KEY_103/104 is the likely fix shape, but needs its own stop-and-think
  before touching `GetterSetterRule`'s behavior per the Hard Constraint. Until
  fixed, `AI_PREAMBLE_AESTHETIC.md`'s Rule 2 ("JAR aligns standard-prefix
  getter/setter groups automatically") is **not true for Kotlin** — flagged
  there with a caveat rather than silently relying on it.

---

## Checklist

### Step 0 — Tokenizer Support (shared file, additive only)

**Critical rule for this step:** `TokenizerCore.java` is shared with C/C++/Java.
Every addition here must be additive (new keyword/operator recognition) and
must not change how any existing C/C++/Java token is lexed. Re-run the full
existing test suite after this step, before moving to Step 1.

- [x] Survey `STYLE_KOTLIN.md`/`STYLE_KOTLIN2.md` for every token not already
      lexed correctly by `TokenizerCore.java`. Added to `MULTI_CHAR_OPS`: `?.`,
      `?:`, `!!`, `..<`, `..` (longest-prefix-first: `..<` before `..`, same
      requirement as the existing `...`/`->*` ordering). `->` already existed
      and is reused as-is for Kotlin's lambda/function-type/`when` arrow — no
      new token needed. `@` in labeled jumps (`return@label`, `outer@`) needs
      no new operator entry either: it already falls through to `emitOperator`'s
      single-char fallback as its own `OP` token, which is sufficient (the
      surrounding spacing rule is a Step 3 `KotlinSpecificRule` concern, not a
      tokenizer one).
      **Found and fixed a real bug in the process (not just additive):**
      `emitNumber()` unconditionally consumed every `.` character, so
      `1..10` lexed as one bogus `NUMBER` token `"1..10"` (and `1..<10` as
      `NUMBER "1.."` + `OP "<"` + `NUMBER "10"`) instead of `NUMBER "1"` + new
      range `OP`. Fixed by stopping number consumption when a `.` is followed
      by another `.` — a decimal point is never followed by a second `.` in
      any of C/C++/Java/Kotlin, so this is safe for all four languages.
      Verified via direct `TokenizerCore` dump (all Kotlin operators lex to
      the expected token stream) and `make test` (25/25 C/C++/Java fixtures
      unaffected, including the fixture with the most numeric-literal density).
- [x] Add a Kotlin keyword set (`KEYWORDS_KOTLIN`), parallel to
      `KEYWORDS_JAVA`/`KEYWORDS_CPP` — includes all hard keywords plus the
      modifier/soft keywords listed in the checklist's original "at minimum"
      set (unconditionally reserved, same simplification already made for
      Java's `var`/`record`, both contextual in real Java but listed
      unconditionally in `KEYWORDS_JAVA`).
- [x] Add Kotlin named-construct detection (`NAMED_CONSTRUCT_KOTLIN` =
      `class`, `object`, `interface`, `enum`, `init`). Deliberately did **not**
      special-case `companion object`, `enum class`, or verify
      `computeConstructName()`'s lookback window for each shape yet — that
      cross-check against actual formatter behavior is Step 1's job (it
      re-examines every named-construct shape against the already-COMPLETE
      shared rule classes); adding it here would be guessing ahead of an
      actual failing case, which this step's own instructions warn against.
- [x] Re-run full existing C/C++/Java test suite. **25/25 pass, zero
      regressions** (24 pre-existing + the unrelated `real_code_regressions_13`
      fixture added the same session, before this Kotlin work started).
- [x] **Follow-up (surfaced during Step 1's §13 cross-check):** added
      `"in"`/`"out"` to `GENERIC_SAFE_KEYWORDS` so `reclassifyAngleBrackets`
      correctly recognizes declaration-site variance (`Box<out T>`,
      `Comparable<in T>`) as a generic `<`/`>` pair rather than a comparison.
      Pure no-op for C/C++/Java (neither keyword exists in their keyword
      sets). `make test` 32/32 before and after. RDD_KEY_113.
- [x] **Follow-up (surfaced during Step 1's §19 cross-check):** added a
      Kotlin-only interpolation-aware string scan (`skipKotlinString` /
      `skipKotlinInterpolationBlock` / `skipKotlinChar`) inside
      `emitString()`, gated behind `lang.isKotlin` — the shared naive
      scan-to-next-`"` misread a nested string inside a `${...}`
      interpolation (`"${foo("x")}"`) as three tokens instead of one,
      confirmed via harness before writing the fix. Depth-tracks `${...}`'s
      own `{`/`}` nesting (so a lambda literal inside the interpolation
      doesn't break early either) and recurses for any nested string/char
      literal, arbitrarily deep. Non-Kotlin scan left byte-for-byte as the
      original. `make test` 32/32 before and after. RDD_KEY_116.
- [x] **Follow-up (row 19.1, investigated on explicit request):** added
      Kotlin-only raw-string support (`isKotlinRawStringOpener` /
      `emitKotlinRawString` / `skipKotlinRawString`), checked in the main
      dispatch before the plain-`"` and C/C++ raw-string-prefix branches.
      Confirmed via harness the naive path was badly broken: a `"""..."""`
      raw string mis-lexed into multiple STRING/IDENTIFIER tokens, and a
      multi-line one leaked a spurious `NEWLINE` token into its own content.
      No backslash-escape processing (literal `\` by design); terminates
      greedily at the first `"""` encountered, matching the real Kotlin
      compiler. `skipKotlinInterpolationBlock` (RDD_KEY_116) extended to
      recognize a nested raw string inside an interpolation expression too.
      Non-Kotlin paths (Java text block, C++ raw string, plain C string)
      confirmed untouched. `make test` 32/32 before and after. RDD_KEY_117.

### Step 1 — Scoping Pass (mirrors `JavaSpecificRule.java`'s own scoping, RDD_KEY_59)

- [x] Cross-check every section of `STYLE_KOTLIN.md` and `STYLE_KOTLIN2.md`
      against the already-COMPLETE shared rule classes (`DeclarationAlignmentRule`,
      `BlockStructureRule`, `SwitchRule`, `GetterSetterRule`, `MiscRule`) to
      determine, per section: (a) already satisfied as-is by shared logic once
      Step 0's tokenizer work lands, (b) satisfied by a small additive
      extension to a shared class, or (c) needs a new method in
      `KotlinSpecificRule.java`. Table below.
- [x] Flag anything found during scoping that would require changing
      already-COMPLETE shared-class *behavior* (not just adding to it) — see
      **Open Questions** below: `DeclarationAlignmentRule`'s `Declaration`
      model assumes C/Java's `[modifiers] Type name [= init]` token order,
      which is structurally reversed from Kotlin's `[modifiers] val/var name : Type
      [= init]`. Stopped here rather than guessing a direction.

**Scoping table** (section numbers match `STYLE_KOTLIN.md`; `K2.N` = `STYLE_KOTLIN2.md` §N):

| § | Topic | Outcome | Notes |
|---|---|---|---|
| 1 | Semicolons (strip optional `;`) | (c), **done** | No shared class strips statement-terminating `;` for any language today (C/Java require it) — Kotlin-only `KotlinSpecificRule.stripOptionalSemicolons` pass. An earlier-session version of this method (`b0e778f`) only protected the enum-with-members mandatory `;` and stripped every other `;` unconditionally, silently mis-handling the deliberate-same-line-multi-statement case — rewritten around a single positive-evidence rule (`isTrailingSemicolon`: only strip a `;` that's the last significant thing on its line), which naturally keeps a same-line multi-statement `;` untouched with no special-casing, plus reuses §2's `findEnumConstantListTerminators` for the enum-mandatory-`;` exclusion. RDD_KEY_115. |
| 2 | `enum class` with members | (a)/(c), **done** | The `"enum class " + name` closing-comment label already falls out of `BlockStructureRule.classifyNamed`'s existing "keyword before `class` is `enum`" check (originally written for C++) — works for free once `enum`/`class` are both Kotlin keywords (Step 0, done). The body-open/close blank lines are already produced for free by the shared `insertNamedConstructBlankLines` — verified via harness, zero changes. The blank-line emphasis around the entry-list-terminating `;` itself is a separate pass, not covered by that method; implemented as new `KotlinSpecificRule.separateEnumConstantListTerminator` (+ helpers), mirroring `JavaSpecificRule.separateEnumConstantListTerminator`. See RDD_KEY_111. |
| 3 | Brace style (Allman fn bodies / K&R everything else) | (a) for K&R-enforcement direction, **verified**; (c) for Allman-conversion direction, **done** | `BlockStructureRule.qualifiesForKAndR`'s `PAREN_KR_KEYWORDS`/`BARE_KR_KEYWORDS` sets already cover Kotlin's exact same control-flow keyword vocabulary (`if/while/for/switch/catch`, `else/do/try/finally`) — confirmed via harness, K&R already gets correctly enforced onto a same-line-with-`)`-before-K&R-construct brace. The *other* direction — converting a function body's brace from K&R to Allman — needed a new Kotlin-only method: `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`, mirroring `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle`/`CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` but with a much more conservative candidate signal, since Kotlin's trailing-lambda call syntax (`someCall(args) { ... }`) is token-shape-identical to a function definition's body brace and Kotlin has no `new` keyword to rule ordinary calls out the way Java/C++ do — requires a backward scan from the candidate name (through an optional extension-receiver chain and/or `<T>` clause) to land exactly on `fun`, or the token before `(` to be `constructor` itself; anything else bails, same posture as an ordinary call being left untouched. Also handles a `: ReturnType` sitting between `)` and `{`, and a one-liner body staying K&R (RDD_KEY_75/RDD_KEY_89 exception, same as Java/C++). RDD_KEY_114. |
| 3.1 | Class/Object/Companion Object bodies | (b), **done** | Named `class Foo {`/`object Foo {` already worked. Headless gap (anonymous `companion object {}`, anonymous `object : Interface {}`, `init {}` never arming `pendingNamedConstructName`) fixed via `RDD_KEY_99`: additive `BlockStructureRule.classifyKotlinHeadlessNamed`, gated by new `Lang.isKotlin`, parallel to the existing `isAnonymousClassBrace` precedent. Also fixed a related tokenizer bug found during verification (see RDD_KEY_99): `:` was wrongly arming a following supertype identifier as the construct's own name. |
| 3.2 | `catch`/`for`/`while`/`when` no space before `(` | (b), **done** | Added `"when"` to `MiscRule.TIGHT_PAREN_KEYWORDS` — RDD_KEY_100. Pure no-op for C/C++/Java (no `when` keyword/token in any of their keyword sets). |
| 3.3 | Secondary constructors (Allman body) | (c), **done** | Covered by the same §3 method (`KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`) in one pass, as planned: a secondary constructor is recognized by the token immediately before `(` being the `constructor` keyword itself. Verified via harness: `class Foo { constructor(x: Int) { ... } }` correctly converts the constructor body to Allman. RDD_KEY_114. |
| 3.4 | `init` blocks | (b), **done** | Same headless-named-construct fix as §3.1 — `init {}` now returns `"init"` from `classifyKotlinHeadlessNamed`, grouped in the same `RDD_KEY_99` commit. |
| 4 | `when` expression (arrow alignment, closing comment, blank lines) | (c) | `SwitchRule.java` turned out to be colon-form-statement-only (STYLE.md §13), unrelated; the real arrow-form logic is `JavaSpecificRule.enforceSwitchExpressionArrowAlignment`, but its `case`/`default`-keyword label scan and all-or-nothing block-body bailout both don't fit Kotlin's keyword-less, non-all-or-nothing `when` — implemented as new `KotlinSpecificRule.formatWhenExpressions` instead. RDD_KEY_101. Body-squishing non-idempotency bug fixed under RDD_KEY_121 (root cause was `KotlinDeclarationAlignmentRule`, not this method). |
| 5 | Null-safety operators (`?.`/`!!` tight, `?:` spaced) | (c), **done** | New `KotlinSpecificRule.enforceNullSafetyOperatorSpacing` — a single flat whole-file whitespace-collapsing pass, not scoped to any one construct, since no shared class does general expression-level operator re-spacing today. RDD_KEY_102. |
| 6 | Variable/property declaration alignment | (c), **done** | New `KotlinDeclarationAlignmentRule extends DeclarationAlignmentRule` (user-directed: loosen shared-class visibility, then extend, rather than an independent parser in `KotlinSpecificRule.java`). Reuses `splitStatements`/`hasBlankLineBefore`/`hasCommentBefore`/`significantOnly`/`renderTokens`/`findTrailingComment` (raised private → protected, no behavior change) plus `ColumnGrid`/`KotlinModifierPriority`; writes its own `KotlinDecl` model, `splitKotlinStatements` (newline-terminated statement splitting — Kotlin has no `;`), `parseKotlinDeclaration`, and `renderPropertyGroup` (per-column `ColumnGrid`, not `Declaration`/`render()`). RDD_KEY_103. |
| 7 | Constructor/function parameter lists | (c), **done** | Same reversed-grammar issue as §6, in `MiscRule.Param`/`Signature` instead of `DeclarationAlignmentRule.Declaration`. Fixed as `RDD_KEY_104` — new `KotlinSignatureRule extends MiscRule`, same visibility-loosen-then-extend pattern as §6. |
| 7.1 | Named/default arguments (`=` spacing/alignment) | (c), **done for declarations** | Folded into §7's `KotlinSignatureRule.parseKotlinParam`/`render` — a default value is just one more optional trailing part of a single parameter's grammar, so no separate method was needed. **Not covered:** the call-site named-argument shape (`foo(x = 1, y = 2)`) shown in STYLE_KOTLIN.md §7.1's own worked example is a function *call*, not a declaration — no type column, different token shape (`name = value` only) — genuinely out of `KotlinSignature`'s scope as parsed here; would need its own small parser/renderer analogous to `MiscRule`'s `renderCallOnePerLine`/`renderCallPreserveGroups` family if picked up later. |
| 7.2 | Trailing comma (preserved as-is) | (a), **verified** | No existing pass adds or strips a trailing comma in any parameter/argument list for any language — trivially satisfied by doing nothing. Confirmed via harness: `KotlinSignature.trailingComma` round-trips correctly through `KotlinSignatureRule.render` for `fun foo(x: Int,)`. |
| 8 | Property accessors (`get`/`set`, preserve expression/block form) | (a), **verified for single-standalone case only — grouping is BROKEN, see Open Questions** | "Preserve as-is" is satisfied by not writing code that touches it. One risk checked: `BlockStructureRule.collapseSingleExpressionBlocks`'s `SINGLE_EXPR_KEYWORDS` is `{if, while, for}` only — an accessor's `set(v) { field = v }` block body is never a match, so it won't get wrongly collapsed to bare-statement form. Confirmed via harness for both a block-bodied `set(v) { field = v }` (left completely untouched, including its own Allman `{`, since §3's Allman-conversion gap applies here too but the *block form itself* isn't collapsed) and an expression-bodied `get() = computeY()` (untouched). **Correction (post-Step-3.5):** the claim that this "participates in the same §14/STYLE.md getter/setter-style aligned group" was never actually harness-verified — live-tested and found broken, `GetterSetterRule.groupOneLiners` never groups Kotlin one-liners at all. See Open Questions. |
| 9 | Expression-bodied functions | (a)/(c), **done for standalone case — grouping is BROKEN, see Open Questions** | "Preserve as-is" part is free (same reasoning as §8). The "wrap `= expr` onto its own line if signature-breaking alone isn't enough" part implemented as new `KotlinSignatureRule.FunctionTail`/`parseFunctionTail`/`renderWithTail`, a three-tier fallback delegating to §7's existing `render` for the middle tier. Also fixed a **shared-class bug** this work surfaced: `MiscRule.isTightToken` was collapsing Kotlin multiplication spacing (`x* x`), gated off for Kotlin. See RDD_KEY_112. **Correction (post-Step-3.5):** same grouping claim/gap as §8 above — never harness-verified, confirmed broken. See Open Questions. |
| 10 | `for` loops and ranges | (a)/(c), **done** | Tight/loose paren-padding itself is already generic (`ComplexityPaddingEvaluator`, STYLE.md §3.1) — `in`/`until`/`downTo`/`step` turned out to already be inert w.r.t. its nested-bracket detection with zero code changes (`in` is `TokenType.KEYWORD`, the other three are plain `TokenType.IDENTIFIER`, confirmed via harness — reclassified (b)→(a)). The `..`/`..<` range operator's own *tight* spacing needed new code, same kind of gap as §5 — new `KotlinSpecificRule.enforceRangeOperatorSpacing`. RDD_KEY_110. |
| 11 | Labeled jumps (`@label` spacing) | (c), **done** | New `KotlinSpecificRule.enforceLabeledJumpSpacing` — a small left-to-right state machine over a flat whole-file token pass (same shape as §5/RDD_KEY_102), telling a jump's `@label` (tight both sides) apart from a declaration's `label@` (tight before, spaced after) apart from an unrelated annotation `@Foo` (untouched). RDD_KEY_105. |
| 12 | Destructuring declarations | (c), **done** | LHS is a parenthesized name list (`(a, b) = pair`), not `MiscRule.Assignment`'s assumed single `target` token — implemented directly in `KotlinDeclarationAlignmentRule.java` (reuses its existing §6 infrastructure) as new `DestructuringDecl`/`groupDestructuringDeclarations`/`parseDestructuringDeclaration`/`renderDestructuringGroup`, a separate group stream from §6's own. Comma spacing is normalized for free as a side effect of rebuilding `lhsText` from the parsed component list, not a passive default. RDD_KEY_107. |
| 13 | Generics variance (`in`/`out`) | (b), **done** | `TokenizerCore.GENERIC_SAFE_KEYWORDS` extended with `"in"`/`"out"` — confirmed via harness these were previously misread as comparison `OP` tokens, now correctly `ANGLE_BRACKET_OPEN`/`_CLOSE` for `Box<out T>`/`Comparable<in T>`/`Pair<in T, out U>`; plain comparisons unaffected. Pure no-op for C/C++/Java (neither keyword exists in their keyword sets). Tokenizer-level fix, no rendering/spacing pass needed beyond correct classification. RDD_KEY_113. |
| 14 | Generic `where` clause | (c), **done** | Structural analog exists in `CppSpecificRule.java`'s trailing-`requires`-clause handling, but that's a per-language file, not shared — implemented as new `KotlinSpecificRule.enforceWhereClausePlacement`, using the C++ method as a reference pattern per this row's own note. RDD_KEY_106. |
| 15 | Infix functions (modifier slot; call-site spacing) | (a), **verified** | Modifier slot itself is Step 2 (`KotlinModifierPriority`) scope, not Step 1. Call-site word-operator spacing (`3 times "abc"`) is ordinary expression spacing, already left alone by every shared class (same reasoning as §5's baseline, no active interference to worry about). Confirmed via harness. |
| 16 | Annotation use-site targets (`@field:` tight `:`) | (c), **done** | No existing annotation-colon handling (Java annotations have no use-site-target shape) — new `KotlinSpecificRule.enforceAnnotationUseSiteTargetSpacing`, a flat whole-file state-machine pass. RDD_KEY_108. |
| 17 | Lambda-with-receiver / function types (exempt from nesting detector) | (b), **done** | `ComplexityPaddingEvaluator.isLoose` (shared) extended to skip a `.`-preceded/`->`-followed `(...)` span rather than counting it as nesting — pure no-op for C/C++/Java, `make test` 32/32 before/after. Known Gap (function type nested as a parameter of another) deliberately left unhandled, per the style doc's own text. RDD_KEY_109. |
| 17.1 | Lambda parameter arrow spacing | (c), **done** | New `KotlinSpecificRule.enforceArrowSpacing` — flat whole-file single-space-both-sides pass over every `->`, covering the function-type arrow (§17) and lambda-parameter arrow (§17.1) together as "one consistent arrow-spacing rule." Excludes `when`-branch arrows via `collectWhenBranchArrowIndices` (owned by §4's alignment instead). RDD_KEY_109. |
| 18 | `vararg` | (a), **verified** | Modifier-slot handling is Step 2 scope; no general spacing concern beyond that. Confirmed via harness that a `vararg` param itself is inert to every brace-style pass tried. |
| 19 | String templates (preserve `"$x"`/`"${x}"` exactly) | (c) — **tokenizer-level, done** | Investigated and confirmed the flagged risk was real: `TokenizerCore.emitString()`'s naive scan-to-next-`"` misreads `"${foo("x")}"` as three tokens instead of one correctly-bounded `STRING` token, since a nested string inside a `${...}` interpolation terminates the scan early — confirmed via a token-dump harness before writing any fix. This is a genuine correctness risk (not cosmetic): with the string's own boundary lost, a later spacing pass could insert whitespace *inside* the literal's actual text. Fixed with a Kotlin-only path (`skipKotlinString`/`skipKotlinInterpolationBlock`/`skipKotlinChar`, gated behind `lang.isKotlin`, non-Kotlin scan left byte-for-byte as the original) that depth-tracks `${...}`'s own `{`/`}` nesting (so a lambda literal inside the interpolation, `"${list.map { it * 2 }}"`, doesn't break early either) and recurses back into itself for any nested string/char literal encountered along the way, arbitrarily deep. Bare `$x` needed no special handling — it introduces no nesting risk. RDD_KEY_116. Triple-quoted raw strings (`"""..."""`) are a related but explicitly out-of-scope risk surfaced during this investigation — see new row 19.1 below; neither style doc mentions them and they have entirely different lexical rules, so left as a flagged, not-yet-investigated gap rather than folded into this fix. |
| 19.1 | Triple-quoted raw strings (`"""..."""`) | (c) — **tokenizer-level, done** | Investigated on explicit request. Confirmed via harness this was badly broken, not merely unhandled: `"""hello "world" end"""` mis-lexed as five tokens (`""` / `"hello "` / a bare `IDENTIFIER world` / `" end"` / `""`), and a multi-line raw string mis-lexed a spurious `NEWLINE` token into the middle of what should be one opaque string. Fixed with `isKotlinRawStringOpener`/`emitKotlinRawString`/`skipKotlinRawString` (Kotlin-only, checked before the plain-`"` and C/C++ raw-string branches): no backslash-escape processing (`\` is literal, by design), and greedy termination at the first `"""` encountered (matches the real Kotlin compiler — four trailing quotes closes at the first three, leaving one stray `"` token, which is correct). `${...}` interpolation still recognized via the existing `skipKotlinInterpolationBlock` (RDD_KEY_116), extended with a `"""`-lookahead so a nested raw string inside an interpolation expression is also recognized rather than misread. Verified via a 14-case harness (embedded quote runs, multi-line, literal backslash, plain/nested/doubly-nested interpolation, the 4-trailing-quotes edge case, unterminated input, plus Java text block / C++ raw string / plain C string sanity checks all confirmed untouched). `make test` 32/32 before and after. RDD_KEY_117. |
| 20 | Sealed classes/interfaces | (a), **verified** | Normal `class`/`object` K&R rules apply unchanged, no special layout. Confirmed via harness: `sealed class Result { ... }` gets the same K&R brace + closing comment (`} // class Result`) as a plain `class`. |
| 21 | Type aliases | (a), **verified** | Single-line `=`-spaced statement, no new behavior. Confirmed via harness: `typealias Handler = (Int) -> Unit` passes through every brace-style pass untouched. |
| 22 | Extension functions | (a), **verified** | `fun` behaves like any other modifier/keyword token for spacing purposes. Confirmed via harness: `KotlinSignatureRule.parseKotlinSignature` correctly parses `fun String.reverseWords()`, placing `fun String .` in `leadTokens` and `reverseWords` as the signature name — the receiver-type-before-name shape needs no special-casing beyond what §7's existing name-detection (IDENTIFIER immediately before the first depth-0 `(`) already does. |
| 23 | Known Gaps | (a), excluded | Explicitly out of scope, same posture as STYLE_JAVA.md's own excluded "unresolved" section (RDD_KEY_59). |
| K2.1 | Guard conditions in `when` | (a), **verified** | Extends §4's arrow-alignment logic as-is per the style doc — no new rule. Confirmed by direct harness test (not committed): `KotlinSpecificRule.formatWhenExpressions` (§4/RDD_KEY_101) already handles a guarded branch (`is String if x.isNotEmpty() -> foo()`) correctly with zero code changes, since the guard's `if <condition>` is just ordinary label text scanned up to the first top-level `->` — `->` alignment, forced blank lines, and the closing comment all work identically whether or not a branch carries a guard. Output matched STYLE_KOTLIN2.md §1's own worked example byte-for-byte, including a second harness case with multiple guarded branches confirming the arrow column stays aligned across guarded and unguarded branches together. |
| K2.2 | `data object` | (a), **verified** | Formatted exactly like `object` — a *named* `data object Singleton { ... }` isn't even a headless-object shape (§3.1's fix was for anonymous/headless cases), it's an ordinary named construct with an extra leading modifier, already handled by the existing named-construct path. Confirmed via harness: `data object Singleton { val x = 1 }` gets the same blank lines + closing comment (`} // Singleton`) as any other named `object`. |
| K2.3 | Other 2.0/2.1 features | (a), excluded | Explicitly "no new formatting rules" in the style doc itself. |

### Step 2 — `KotlinModifierPriority.java`

- [x] Column order for Kotlin's modifier set (`public/private/protected/
      internal`, `open/final/abstract/sealed`, `override`, `const`,
      `lateinit`, `val`/`var` sharing one slot per STYLE_KOTLIN.md §6) —
      confirm no cross-declaration-kind conflict analogous to the one resolved
      for Java in RDD_KEY_83 before assuming a single flat map suffices.
      **No such conflict found**: unlike Java's `abstract`/`volatile` case
      (where a single rank for `abstract` forced an unwanted rank shift for
      `volatile` on fields), none of Kotlin's modifiers here need a *different*
      relative order depending on which declaration kind they appear on —
      `const` (properties only), `lateinit` (var properties only), `override`
      (members only), and `open`/`final`/`abstract`/`sealed` (mutually
      exclusive modality, one or none per declaration) never fight over
      column order across kinds. Implemented as
      `grid/KotlinModifierPriority.java`: columns 0 (visibility) / 1 (modality:
      `open`/`final`/`abstract`/`sealed`, shared) / 2 (`override`) / 3
      (`const`) / 4 (`lateinit`) / 5 (`val`/`var`, shared). Compiles clean
      standalone; not yet wired into any rule class (that's Step 3's job, once
      `KotlinSpecificRule.java` exists to use it).

### Step 3 — `KotlinSpecificRule.java`

- [ ] Implement each section flagged "(c)" in Step 1's scoping table, one
      section at a time, each as its own checkpoint commit.
Full implementation/verification narratives for every checked item below have been
compacted out of this file — each is still fully recorded, in the same level of
detail, in its `RDD_KEY_n` entry in `STATE_rdd_log.md` (`grep -Fm1 'RDD_KEY_n'`).
- [x] **§1 Semicolons.** `KotlinSpecificRule.stripOptionalSemicolons` strips every
      optional statement-terminating `;`, keeping only an armed `enum class` body's
      entries/members separator when member declarations actually follow it. New
      file, no shared-class change. `make test` 25/25.
- [x] **§3.1/§3.4 Class/Object/Companion Object/`init` bodies.** `RDD_KEY_99` —
      shared-class extension (`Lang.isKotlin`,
      `BlockStructureRule.classifyKotlinHeadlessNamed`), plus a related tokenizer
      fix (`:` wrongly arming a supertype identifier as the construct's own name
      for anonymous `object : Super {}`). `make test` 25/25.
- [x] **§3.2 `when` no space before `(`.** `RDD_KEY_100` — added `"when"` to
      `MiscRule.TIGHT_PAREN_KEYWORDS`, a one-line shared-class change (pure no-op
      for C/C++/Java, no such keyword in their keyword sets). `make test` 25/25.
- [x] **§4 `when` expression (arrow alignment, closing comment, blank lines).**
      Was marked NOT idempotent — see Step 4's known-bugs punch list item 3
      (body-squishing on re-format). Root cause found and fixed under
      `RDD_KEY_121`: not an issue in `formatWhenExpressions` itself or an
      "interaction with an earlier generic blank-line-collapsing pass" as
      originally guessed, but `KotlinDeclarationAlignmentRule.
      parseKotlinDeclaration` stripping a multi-line `when` initializer's
      internal newlines before `formatWhenExpressions` ever ran. Now wired
      into `Formatter.formatOne` (Phase 4) and confirmed clean against
      `test/kt_combined_inp.kt`.
      `RDD_KEY_101` — new `KotlinSpecificRule.formatWhenExpressions`, not a
      `SwitchRule`/`JavaSpecificRule` extension (keyword-less branch labels,
      non-all-or-nothing block-body alignment, and forced rather than merely
      preserved blank lines all differ from the Java arrow-switch precedent).
      `make test` 32/32.
- [x] **§5 Null-safety operators (`?.`/`!!` tight, `?:` spaced).** `RDD_KEY_102` —
      new `KotlinSpecificRule.enforceNullSafetyOperatorSpacing`, a flat whole-file
      pass (no shared class does general expression-level operator re-spacing).
      `make test` 25/25.
- [x] **§6 Variable/property declaration alignment.** `RDD_KEY_103` — raised six
      `DeclarationAlignmentRule` helpers `private`→`protected` (additive, no
      behavior change), then `KotlinDeclarationAlignmentRule extends
      DeclarationAlignmentRule` with its own `KotlinDecl` model,
      `splitKotlinStatements` (newline-terminated, not `;`-terminated),
      `parseKotlinDeclaration`, and `ColumnGrid`-based `renderPropertyGroup`.
      User-directed: loosen shared-class visibility, then extend, rather than an
      independent parser. `make test` 25/25.
- [x] **§7/§7.1 Constructor/function parameter lists, named/default arguments.**
      `RDD_KEY_104` — same visibility-loosen-then-extend pattern as §6, six
      `MiscRule` helpers promoted, new `KotlinSignatureRule extends MiscRule` with
      its own `KotlinParam`/`KotlinSignature` model and `ColumnGrid`-based broken
      form; also covers §7.2 trailing-comma preservation. **Not covered**:
      call-site named arguments (`foo(x = 1, y = 2)`), a structurally different
      type-less shape. `make test` 32/32.
- [x] **§11 Labeled jumps (`@label` spacing).** `RDD_KEY_105` — new, fully
      self-contained `KotlinSpecificRule.enforceLabeledJumpSpacing` (no shared-class
      change needed at all), a flat whole-file pass with a small `JumpState`
      machine distinguishing a jump's `@label` (tight both sides) from a label
      declaration's `label@` (tight before, one space after) from an unrelated
      `@Annotation`. `make test` 32/32.
- [x] **§14 Generic `where` clause.** `RDD_KEY_106` — new, fully self-contained
      `KotlinSpecificRule.enforceWhereClausePlacement`, structurally mirroring
      `CppSpecificRule.enforceRequiresClausePlacement` (fits-inline vs. wraps to
      its own indented line, based on `lineLengthLimit`), but always breaking
      every bound one-per-line at the top-level comma (never within a bound)
      once wrapped, column-aligned under the first bound's start column, per
      STYLE_KOTLIN.md §14's own worked examples. Added a new
      `KotlinSpecificRule(Lang, int lineLengthLimit, int indentWidth)`
      constructor (this class's first method needing indent width, not just
      line length). No shared-class change. `make test` 32/32.
- [x] **§12 Destructuring declarations.** `RDD_KEY_107` — new `DestructuringDecl`
      model, `groupDestructuringDeclarations`/`parseDestructuringDeclaration`/
      `renderDestructuringGroup` added directly to `KotlinDeclarationAlignmentRule.java`
      (reuses that class's §6 infrastructure rather than a new file). Single
      pre-rendered `lhsText` cell (no per-component type grid, per §12's own
      "no type annotations to anchor a column grid" text) rendered via a
      two-column `ColumnGrid`, same shape as `MiscRule.Assignment`'s render.
      Own group stream, never merged with §6's property-declaration groups.
      No shared-class change. `make test` 32/32.
- [x] **§16 Annotation use-site targets.** `RDD_KEY_108` — new
      `KotlinSpecificRule.enforceAnnotationUseSiteTargetSpacing`, a flat
      whole-file state-machine pass (`@` → target → `:` → name) matching
      target keywords by token text (not `TokenType.KEYWORD`, since
      `delegate` isn't tokenizer-lexed as one). Tightens only the `:` on
      both sides; `@`-to-target spacing left unenforced (no textual
      backing, no codebase precedent for reformatting plain annotation
      spacing). No shared-class change. `make test` 32/32.
- [x] **§17/§17.1 Lambda-with-receiver / function-type nesting exemption +
      arrow spacing.** `RDD_KEY_109` — **shared-class change**:
      `ComplexityPaddingEvaluator.isLoose` extended to skip a `.`-preceded/
      `->`-followed `(...)` span (a lambda-with-receiver's own invocation
      parens) rather than counting it as nesting, so an enclosing
      parenthesized type annotation stays tight (`(StringBuilder.() -> Unit)`)
      instead of incorrectly loose-padding. Pure no-op for C/C++/Java
      (neither has this token shape); `make test` 32/32 before and after.
      Known Gap (function type nested as a parameter of another) deliberately
      left unhandled per the style doc. Also new Kotlin-only
      `KotlinSpecificRule.enforceArrowSpacing` + `collectWhenBranchArrowIndices`
      — flat whole-file single-space arrow pass covering both constructs,
      explicitly excluding `when`-branch arrows (owned by §4's column
      alignment) by index. `make test` 32/32.
- [x] **§10 `for` loops and ranges.** `RDD_KEY_110` — `in`/`until`/`downTo`/
      `step` reclassified (b)→(a): already inert w.r.t.
      `ComplexityPaddingEvaluator.isLoose` with zero code changes (`in` is
      `TokenType.KEYWORD`, the rest are plain `TokenType.IDENTIFIER`,
      confirmed via harness). New `KotlinSpecificRule.enforceRangeOperatorSpacing`
      tightens `..`/`..<` on both sides (simpler one-sided sibling of §5's
      state machine, no spaced variant to handle). No shared-class change.
      `make test` 32/32.
- [x] **§2 `enum class` with members.** `RDD_KEY_111` — closing-comment
      label and body-open/close blank lines already free (Step 0 +
      `insertNamedConstructBlankLines`, verified via harness, zero changes).
      New `KotlinSpecificRule.separateEnumConstantListTerminator` (+
      `findEnumConstantListTerminators`/`isEnumBodyBrace`/`prevSignificantIndex`)
      adds the blank-line emphasis around the mandatory entry-list-terminating
      `;`, mirroring `JavaSpecificRule.separateEnumConstantListTerminator`
      (per-language precedent, not a shared-class reuse — same reasoning as
      §14/RDD_KEY_106). Reuses this file's own existing `lineIndent` helper.
      Verified via harness: reproduces the style doc's worked example
      byte-for-byte; no-trailing-members, trailing-`;`-with-no-members-after,
      and trailing-comma-preservation cases all correctly left untouched. No
      shared-class change. `make test` 32/32.
- [x] **§9 Expression-bodied functions.** `RDD_KEY_112` — "preserve as-is"
      part free (same reasoning as §8). New `KotlinSignatureRule.FunctionTail`
      (parses `: ReturnType`/`= expr` after a signature's `)`) +
      `parseFunctionTail` + `renderWithTail`, a three-tier fallback: fits
      fully inline as written; else break params first (delegates straight
      to §7's existing `render`, unchanged) and append the tail if that now
      fits; else, only if expression-bodied, wrap `= expr` onto its own line
      indented one level (mirrors §7.1's named-argument `=`-wrap). An explicit
      return type with no `=` and still too long after breaking params is
      left as the combined line — nothing documented left to wrap for that
      shape. **Shared-class change**: `MiscRule.isTightToken`'s `*`/`&`
      tight-token treatment (a C/C++ pointer/reference-declarator convention)
      gated off for Kotlin via `!lang.isKotlin` — unconditionally applied, it
      was collapsing ordinary Kotlin multiplication spacing (`x * x` →
      `x* x`) in any expression rendered through the shared `renderTokens`,
      caught via a harness reproducing the style doc's own `x * x + y * y`
      worked example byte-for-byte. `make test` 32/32 before and after.
- [x] **§3/§3.3 Function/secondary-constructor body Allman-brace
      conversion.** `RDD_KEY_114` — new
      `KotlinSpecificRule.enforceFunctionDefinitionAllmanBraceStyle` (+
      `isFunctionOrConstructorCloseParen`/`findSignatureCloseParenBeforeBrace`/
      `isAngleOpen`/`isAngleClose`/`skipAngleBracketsBackward`), mirroring
      `JavaSpecificRule.enforceMethodDefinitionAllmanBraceStyle`/
      `CppSpecificRule.enforceFunctionDefinitionAllmanBraceStyle`'s overall
      shape but with a far more conservative candidate signal: Kotlin's
      trailing-lambda call syntax (`someCall(args) { ... }`) is token-shape-
      identical to a function definition's body brace, and Kotlin has no
      `new` keyword to rule ordinary calls out the way Java/C++ do, so a
      candidate is only accepted if a backward scan from the name (through
      an optional extension-receiver chain and/or `<T>` clause) lands
      exactly on `fun`, or the token before `(` is `constructor` itself —
      anything else bails, same "give up rather than guess" posture as
      `KotlinSignatureRule.parseKotlinSignature`. Also handles a
      `: ReturnType` sitting between `)` and `{`, and tolerates the
      tokenizer's non-reclassified plain-`OP` `<T>` right after `fun`
      (`fun <T> ...` doesn't get `ANGLE_BRACKET_OPEN`/`_CLOSE` the way
      `List<T>` does) — both discovered only via harness, not anticipated
      up front. One-liner bodies stay K&R (RDD_KEY_75/RDD_KEY_89 exception).
      Verified via an 11-case harness: plain function, secondary
      constructor, extension function, generic function, generic extension
      function (all K&R→Allman); already-Allman (idempotent); one-liner
      (stays K&R); trailing-lambda call (untouched, enclosing real function
      still converts); enum-entry anonymous body (untouched); control-flow
      block (untouched); plain call with no body (untouched). No shared-
      class change. `make test` 32/32.
- [x] **§1 Semicolon stripping.** `RDD_KEY_115` — re-examined the
      pre-existing `stripOptionalSemicolons` (committed earlier, `b0e778f`,
      before this session's own RDD-log/scoping-table-marker convention
      existed, hence the row still read plain "(c)" with no "**done**")
      rather than assuming it was already correct, and found a real bug: it
      only ever protected the enum-with-members mandatory `;` (§2) and
      stripped every other `;` unconditionally — including a deliberate
      same-line multi-statement `;` (`val a = 1; val b = 2`), which would
      have silently merged the two statements into one invalid line, not
      just a style nit. Rewrote around a single positive-evidence rule,
      `isTrailingSemicolon`: only strip a `;` that is the last significant
      thing on its physical line (next non-gap token, skipping whitespace/
      comments, either starts a new line or none remain) — this naturally
      leaves the multi-statement-same-line case untouched with no special-
      casing needed. Reuses §2/RDD_KEY_111's `findEnumConstantListTerminators`
      directly for the enum-mandatory-`;` exclusion rather than re-deriving
      a separate enum/class/brace-tracking state machine. Also fixed a
      stray-trailing-space gap the old version had (`foo() ;` → `foo() `
      instead of `foo()`) by dropping any whitespace immediately preceding a
      stripped `;` too. Verified via an 8-case harness: plain flat
      declarations (stripped); space-padded `;` (no stray trailing space);
      multi-statement-same-line (now correctly kept — the bug this re-check
      caught); trailing line comment after `;` (still stripped); enum with
      members after its mandatory `;` (kept); enum with a trailing `;` but
      no members after (stripped, optional); no semicolons at all
      (untouched); `;` at literal end-of-file (stripped). No shared-class
      change. `make test` 32/32.
- [x] **§19 String templates — tokenizer-level fix.** `RDD_KEY_116` —
      **shared-class change.** Confirmed the flagged risk was real: a nested
      string inside a `${...}` interpolation (`"${foo("x")}"`) terminated
      `TokenizerCore.emitString()`'s naive scan-to-next-`"` early, splitting
      the literal into three tokens instead of one. Fixed with a Kotlin-only
      `skipKotlinString`/`skipKotlinInterpolationBlock`/`skipKotlinChar`
      path, gated behind `lang.isKotlin`, that depth-tracks `${...}`'s own
      `{`/`}` nesting (so a lambda literal inside the interpolation doesn't
      break early either) and recurses for any nested string/char literal,
      arbitrarily deep. Non-Kotlin scan is byte-for-byte the original.
      Verified via an 11-case harness (bare `$x`; braced `${x}`; the
      original failing nested-string case; a lambda literal inside
      interpolation; two adjacent interpolation blocks; a doubly-nested
      string-inside-interpolation-inside-string; an unterminated string;
      plain string with no interpolation; escaped `\$`; a char literal with
      `\"` immediately before an interpolation containing its own char
      literal with a `"`; and a plain C string through the non-Kotlin path
      as a sanity check) — all round-tripped byte-for-byte and tokenized as
      expected. `make test` 32/32 before and after. Surfaces triple-quoted
      raw strings as a separate, undocumented, out-of-scope gap (row 19.1).

### Step 3.5 — Configuration Property Wiring

**Correction (this session):** the framing below ("no pipeline path exists
for the language at all", "Main.java wiring currently deferred") was stale —
found to be **already fully wired** when re-checked against the actual code,
contradicting the unchecked items that follow. `Main.java:389`'s
`inferLanguage` already returns `"kotlin"` for `.kt`/`.kts` (auto-detection;
`--lang` only restricts the *explicit-override* flag to c/cpp/java, which
doesn't block auto-detection), and `Formatter.java` already constructs
`KotlinSpecificRule`/`KotlinSignatureRule`/`KotlinDeclarationAlignmentRule`
with `Config`'s `lineLengthLimit`/`indentWidth` and runs every Kotlin rule
through the same pipeline as Java/C++ (lines 52-53, 97-104, 172-180,
188-190). Likely stale from an earlier session snapshot that predates this
wiring landing. Every item below was re-verified live (standalone JAR run
against scratch `.kt` fixtures with a `.jxmake-code-formatter` config file,
`key=value` format — not YAML), not just re-read from code.

- [x] `line-length` / `indent-size` / `indent-style`: confirmed wired and
      working. `indent-size=2` correctly sizes newly-generated indentation
      (e.g. wrapped parameter lines); `line-length=30` correctly triggers
      Kotlin parameter-list line-breaking (`KotlinSignatureRule`). Neither
      retroactively re-flows pre-existing indentation levels in the input —
      confirmed this is the same behavior as Java/C++, not a Kotlin gap.
- [x] `closing-comment-min-lines`: confirmed Kotlin's named-construct closing
      comments (`} // class Foo`) respect this — `closing-comment-min-lines=1`
      correctly added a closing comment to a short `class Foo { ... }` via
      the shared `BlockStructureRule.addClosingComments`/`classifyNamed`
      path, byte-for-byte parallel to the same test against an equivalent
      `.java` file.
- [x] `format-macros`: confirmed a permanent no-op for Kotlin, not just
      "not wired yet" — its only consumer is `CppSpecificRule`'s `#define`
      column-alignment logic (`format-macros = on`), only ever constructed
      and called from `Formatter.java`'s `isCOrCpp` branch. Kotlin has no
      preprocessor and never will, so there is no future wiring step where
      this property becomes meaningful for `.kt` files — this is categorically
      different from the other items below (which are blocked only pending
      the deferred pipeline wiring). `TokenizerCore.isPreprocessorLanguage()`
      unconditionally returns `true` for every language (its own comment notes
      Java source sometimes carries PCPP-style directives), so a stray `#` at
      line-start in a Kotlin file (e.g. a Kotlin script's `#!/usr/bin/env
      kotlin` shebang) still lexes as an opaque `PREPROCESSOR` token rather
      than erroring — harmless and unrelated to `format-macros` itself, noted
      here only because it was checked as part of confirming this item.
- [x] `line-endings`: confirmed language-agnostic — `Main.java`'s
      `applyLineEndings` runs after `Formatter.formatOne` regardless of
      language. `line-endings=crlf` correctly produced `\r\n` line endings
      on a `.kt` file.
- [x] `normalize-comment-start-case` / `normalize-comment-end-period`:
      confirmed via `on`/`off` config values (the actual accepted format —
      `true`/`false` is rejected with a warning and falls back to default,
      for every language, not a Kotlin-specific issue). Start-case
      capitalization applies to Kotlin `//` comments identically to Java.
      End-period normalization added no period in either the Kotlin or an
      equivalent Java test (`// comment without period` stayed period-less
      in both) — confirmed this is existing cross-language behavior (line
      comments apparently aren't in scope for that pass), not a Kotlin gap,
      so nothing further to fix here.
- [x] **Spec written** — STYLE_KOTLIN.md §24 "Import Ordering" now documents
      the Kotlin `kotlin-import-order`/`kotlin-import-sort`/`kotlin-import-depth`/
      `kotlin-import-blank-lines` properties, derived directly from
      STYLE_JAVA.md §7. One deliberate difference from the Java spec: no
      `static` group, since Kotlin has no `import static` keyword — a
      companion-object-member or top-level-function import uses the exact
      same `import a.b.c` syntax as any other import, so "this is a static
      import" isn't lexically detectable the way Java's `import static` is. A
      leading `kotlin` group (for `kotlin.*` stdlib imports) takes its place.
      Local-import detection (read the local prefix from the file's own
      `package` declaration, depth-configurable) is identical to Java's
      mechanism. Also documents that aliased imports (`import foo.Bar as
      Baz`) and wildcards sort/group by their original qualified name, not
      the alias. Spec only — no code written yet (see next item).
- [x] **Implementation** — added `kotlin-import-order`, `kotlin-import-sort`,
      `kotlin-import-depth`, `kotlin-import-blank-lines` to `Config.java`'s
      known-keys list, fields, getters, and `fromRawMap` parsing (mirroring
      the existing `java-import-*` keys exactly; default group order
      `kotlin, java, com, org, other, local`, matching §24's documented
      default). Implemented `enforceKotlinImportOrdering` in
      `KotlinSpecificRule.java`, mirroring `JavaSpecificRule.
      enforceImportOrdering`'s structure with two Kotlin-specific
      adaptations: no `static` bucket (classification priority local >
      kotlin > java/javax > org > com > other), and an import statement's
      end is an optional `;` or NEWLINE/EOF rather than a required `;`.
      Added `ParsedKotlinImport`, `parseKotlinImportStatement` (recognizes
      an optional `as Alias` suffix — `as` lexes as `TokenType.KEYWORD` for
      Kotlin, confirmed via `TokenizerCore.KEYWORDS_KOTLIN`), plus new
      `appendRange`/`joinVerbatim`/`isPathOp`/`findLocalPackagePrefix`/
      `classifyKotlinImportGroup`/`matchesPrefix` helpers (per-language
      mirroring, not shared-class reuse). Reused the file's existing
      `hasCommentBetween`/`anyFrozen` helpers verbatim. `groupOrder`
      permutation validation throws `IllegalArgumentException`, same
      config-validation posture as Java. Verified via a standalone 10-case
      scratch harness (default grouping/sorting, wildcard imports, aliased
      imports, optional-`;` tolerance, custom group order/blank-lines,
      `sortAlphabetically = false`, zero-imports no-op, comment-blocks-pass,
      no-`package`-declaration, invalid-groupOrder-throws) — all 10 passed.
      `make test` 32/32 before and after (Kotlin-only change, no
      shared-class touch). Not yet wired into `Formatter.formatOne`, same as
      every other unwired Kotlin rule class so far. RDD_KEY_118.
- [x] JXM_CFMT_DIS/JXM_CFMT_ENA marker-comment disabling and `--format-off`:
      confirmed with a live `.kt` fixture — a `//% JXM_CFMT_DIS` /
      `//% JXM_CFMT_ENA` pair correctly froze only the enclosed
      declaration (left its ugly spacing untouched) while formatting the
      declarations before/after normally; `--format-off` correctly froze an
      entire `.kt` file end-to-end. Language-generic implementation, no
      Kotlin-specific gap. `README.md`'s "Disabling formatting for part or
      all of a file" section still needs updating to mention Kotlin (tracked
      below, unchanged).
- [x] Update `README.md` for the new `kotlin-import-*` keys — added the
      `kotlin-import-*` config block (mirroring `java-import-*`), a "Kotlin
      import groups" subsection (no `static` bucket, `kotlin` group instead,
      alias/wildcard sort-by-original-name note), `.kt`/`.kts` extension
      detection in the Usage section, a top-of-file note on Kotlin support
      existing but being newer/less dogfooded than C/C++/Java, and
      `STYLE_KOTLIN.md`/`STYLE_KOTLIN2.md`/`STATE_KOTLIN.md` links in the
      Style Guide Reference section.
- [x] Update `README.txt` for the Kotlin support — corrected the top-level
      "JAR does not yet implement Kotlin support" note (now stale) to reflect
      that JAR support exists but is newer/less dogfooded than C/C++/Java;
      corrected the "no JAR support yet" comment on the Kotlin full-file-pass
      Python example to describe it as a fallback for JAR gaps instead.

### Step 4 — Test Fixtures

**IN PROGRESS**

**The test fixtures are handwritten and may have syntax error.
Confirm with the user as needed.***

`test/kt_combined_inp.kt` and `test/kt_combined_out.kt`
capture STYLE_KOTLIN.md + STYLE_KOTLIN2.md end-to-end coverage.

`test/kt_comments_inp.kt` and `test/kt_comments_out.kt`
capture uncommon comment locations + JXM_CFMT_DIS/ENA).

**The `test/kt_*_inp.kt` files are the input files.**
**The `test/kt_*_output.kt` files are the reference output files.**

Run the formatter to an input file and output the result in `/tmp`.

Perform `diff` between the output file in `/tmp` and the reference output file.
Use the result to fix the formatter.

Also perform idempotency test.

- [ ] `test/kt_combined_inp.kt` / `kt_combined_out.kt` — **in progress, see
      punch list below.**
- [ ] `test/kt_comments_inp.kt` / `kt_comments_inp.kt`
- [ ] After every fixture addition or shared-class change: full existing
      C/C++/Java suite + new Kotlin fixtures, zero regressions.

**Step 4 known-bugs punch list** (against `test/kt_combined_inp.kt` /
`kt_combined_out.kt`; re-verify each against a fresh `diff`, this list is a
working map, not a spec):

1. [x] `enum class Status(val code: Int) { ... }` missing blank line before
   closing brace + missing `} // enum class Status` closing comment. **Fixed
   — RDD_KEY_119.** Root cause was **not** enum-specific or a
   closing-comment-min-lines threshold issue — it affects any Kotlin
   `class`/`enum class` with a primary constructor parameter list;
   `TokenizerCore`'s clear-on-outermost-`(` branch (meant to stop a member
   function's `{` from picking up a surrounding class name) was wrongly also
   clearing the just-armed class/enum-class name before its own body `{`
   consumed it, since a primary constructor's `(` has the identical
   keyword-IDENTIFIER-`(` shape. Gated off for Kotlin. `make test` 32/32.
2. [x] `for(n in numbers) { total += n }` does not collapse to
   `for(n in numbers) total += n`. **Fixed — RDD_KEY_120.** Root cause:
   `BlockStructureRule.tryCollapse` required exactly one top-level `;` in the
   body, unconditionally — Kotlin has no mandatory `;`, so a Kotlin
   single-statement body always had `semiCount == 0` and was rejected
   outright, a total blind spot rather than an edge case. Added a
   Kotlin-only newline-boundary-based single-statement check
   (`isKotlinSingleStatementBody`), parallel to the existing `;`-based path.
   `make test` 32/32. **Surfaced but not fixed (out of scope for this bug):**
   a pre-existing, fully independent `ScopePipeline`/
   `KotlinDeclarationAlignmentRule` splice bug that double-indents a
   `val`/`var` declaration when it's the first statement inside a Kotlin
   function/class body — see Open Questions.
3. [x] `when(status) { ... }` block badly mangled (branches squished onto one
   line, wrong indentation). **Fixed — RDD_KEY_121.** Root cause was upstream
   of `formatWhenExpressions`/RDD_KEY_101 entirely — confirmed via
   `JXM_DEBUG`-gated debug prints in `Formatter.formatOne` that the squishing
   was already present right after Phase 0's `ScopePipeline.process` call.
   Traced to `KotlinDeclarationAlignmentRule.parseKotlinDeclaration`: it
   builds a `val`/`var` declaration's `initTokens` from `significantOnly(stmt)`,
   which strips all `NEWLINE` tokens, so a multi-line block-expression
   initializer (`when(...) { ... }`) loses its internal line structure before
   `renderPropertyGroup`/`renderKotlinTokens` joins it onto one flat line —
   correct for a normal one-line init, silently wrong for any multi-line one.
   Fixed with a new `spansMultipleLines` check right after the declaration's
   `=` token: if the raw (unfiltered) statement has a `NEWLINE` anywhere after
   `=`, `parseKotlinDeclaration` now returns `null` (same "don't guess past an
   unrecognized shape" bailout as its other cases) so the declaration is left
   out of the alignment group and rendered verbatim, preserving line structure
   for later phases (`formatWhenExpressions`) to correctly re-flow. `diff`
   against `kt_combined_out.kt` for this block is now clean. `make test`
   32/32. (The "closing comment capitalized 'When status'" symptom from the
   original bug report did not reproduce in the isolated repro or the real
   fixture — closing comment renders correctly as lowercase "when status" in
   both; likely was a stale/secondary observation from before RDD_KEY_119 was
   fixed, not a separate bug.)
4. [x] `fun test(): int` not capitalized to `fun test(): Int` — **not a
   formatter bug.** Investigated and confirmed the formatter has no
   type-name-case-mangling pass at all (an isolated `fun test(): int { return
   0 }` repro round-trips `int` unchanged); neither STYLE_KOTLIN.md nor
   STYLE_KOTLIN2.md documents any such rule. This was a typo in the fixture
   itself, fixed directly by the user in `test/kt_combined_inp.kt`
   (`int` -> `Int`). No formatter code change needed or made.
5. Lines 88-90 of `kt_combined_inp.kt` (the `val result`/`val result` +
   run-together-statements shape) — **reported as an open fixture-ambiguity
   item, not a formatter bug; not touched**, per instruction not to guess at
   `kt_combined_inp.kt`'s exact intended fix.

Use this standard copyright header when adding a new test fixture file:
```
/*
 * Copyright (C) 2024 Example Corp.
 * SPDX-License-Identifier: MIT
 */
```

### Step 5 — Dogfood / Real-Code Testing

- [ ] Once Steps 0–4 are complete, apply the same real-code-testing
      methodology `STATE.md` used for C/C++/Java (clone a real, compiling
      Kotlin project → format → idempotency check round1 vs round2 → compile
      with `kotlinc`) — deferred until the core checklist above is done, not
      started speculatively.

      Candidate **RobotCoding `gui_frontend_android`**
      (`/home/aloysius/Projects/RobotCoding/gui_frontend_android/app/src/main/java/*.kt`,
      not actually reachable via the `../../../../` relative path originally
      written here — that project lives outside the `JxMake` tree entirely,
      under `~/Projects/RobotCoding/`, a sibling of `~/Projects/JxMake/`) -
      NOT STARTED

**Standalone `K2JVMCompiler` classpath — rejected, do not use.** The recipe
that used to live here (a bare `kotlin-compiler-embeddable` +
`kotlin-stdlib` classpath) cannot syntax-check this candidate: every file
under `gui_frontend_android/app/src/main/java/*.kt` imports `android.*` /
AndroidX APIs, which only exist in the Android SDK jars pulled in by the
project's own Gradle build — a bare compiler classpath with just the Kotlin
stdlib on it has no way to resolve those symbols, so it would fail on
essentially every real file in this project, not just report genuine syntax
errors.

**Use instead:** the project's own Gradle wrapper, via its own env script —
but run it against a **copy** of the project under `/tmp`, not the original
checkout in `~/Projects/RobotCoding/`. The dogfood workflow here is
format-then-compile, which writes formatted `.kt` files back to disk; doing
that against the real, in-use RobotCoding working tree risks clobbering
uncommitted work there, so copy the whole project (or at least
`gui_frontend_android/` — it needs its sibling Gradle config, so copy the
full checkout to be safe) into a scratch directory under `/tmp` first, run
the format + Gradle compile there, and never touch the original in place.
`gui_frontend_android/env.sh` sets `ANDROID_HOME` and puts Gradle 8.9 and
JDK 21 on `PATH` — source it (or replicate just its `export` lines; it also
`cd`s and `exec bash`s into an interactive shell, which isn't wanted for a
scripted run) from within the `/tmp` copy, then run the copy's own
`./gradlew` with a compile-only task, e.g.:

```bash
cp -r /home/aloysius/Projects/RobotCoding /tmp/robotcoding-kotlin-dogfood
cd /tmp/robotcoding-kotlin-dogfood/gui_frontend_android
export ANDROID_HOME=~/android_devel
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/gradle-8.9/bin
export PATH=/opt/openjdk-21_linux-x64_bin/jdk-21/bin:$PATH
./gradlew compileDebugKotlin
```

(`env.sh` ends by `exec bash` into an interactive session — for a
non-interactive/scripted run, source only its `export`/`cd` lines instead of
running the whole script.) This gives a real syntax+type check against the
actual Android SDK/AndroidX dependency graph the source expects, which the
rejected standalone recipe could not.
